package com.laralbir.japantraveltools.feature.taxfree

import java.time.Instant
import java.util.Locale
import java.util.UUID

enum class GoodsType {
    CONSUMABLES,
    GENERAL_GOODS,
    MIXED
}

enum class TaxFreeClaimStatus {
    NOT_APPLICABLE,
    PENDING,
    CUSTOMS_STAMPED,
    REFUNDED
}

enum class ReceiptCurrency(
    val code: String,
    val symbol: String,
    val displayNameEs: String,
    val displayNameEn: String
) {
    JPY("JPY", "¥", "Yen japonés (¥ / JPY)", "Japanese Yen (¥ / JPY)"),
    EUR("EUR", "€", "Euro (€ / EUR)", "Euro (€ / EUR)"),
    USD("USD", "$", "Dólar estadounidense ($ / USD)", "US Dollar ($ / USD)");

    companion object {
        fun fromCode(code: String?): ReceiptCurrency =
            values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: JPY
    }
}

enum class ExpenseCategory(val displayNameEs: String, val displayNameEn: String, val icon: String) {
    SHOPPING("Compras y Recuerdos", "Shopping & Souvenirs", "🛍️"),
    FOOD("Comida y Bebida", "Food & Dining", "🍜"),
    TRANSPORT("Transporte", "Transport", "🚆"),
    LEISURE("Ocio y Entradas", "Leisure & Activities", "⛩️"),
    ACCOMMODATION("Alojamiento", "Accommodation", "🏨"),
    OTHER("Otros Gastos", "Other", "🏷️")
}

data class ReceiptProductItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Int = 1,
    val unitPriceYen: Double,
    val taxRatePercent: Double = 10.0,
    val subtotalWithoutTaxYen: Double = quantity * unitPriceYen,
    val taxYen: Double = Math.round(subtotalWithoutTaxYen * (taxRatePercent / 100.0)).toDouble(),
    val totalWithTaxYen: Double = subtotalWithoutTaxYen + taxYen,
    val currency: String = "JPY",
    val originalUnitPrice: Double = unitPriceYen,
    val originalSubtotalWithoutTax: Double = quantity * originalUnitPrice,
    val originalTax: Double = Math.round(originalSubtotalWithoutTax * (taxRatePercent / 100.0) * 100.0) / 100.0,
    val originalTotalWithTax: Double = originalSubtotalWithoutTax + originalTax
)

data class ShoppingReceipt(
    val id: String = UUID.randomUUID().toString(),
    val purchaseDateIso: String = Instant.now().toString(),
    val storeName: String,
    val location: String = "Tokyo",
    val items: List<ReceiptProductItem> = emptyList(),
    val subtotalWithoutTaxYen: Double = items.sumOf { it.subtotalWithoutTaxYen },
    val totalTaxYen: Double = items.sumOf { it.taxYen },
    val totalWithTaxYen: Double = items.sumOf { it.totalWithTaxYen },
    val currency: String = "JPY",
    val originalSubtotal: Double = if (currency == "JPY") subtotalWithoutTaxYen else items.sumOf { it.originalSubtotalWithoutTax },
    val originalTotalTax: Double = if (currency == "JPY") totalTaxYen else items.sumOf { it.originalTax },
    val originalTotalWithTax: Double = if (currency == "JPY") totalWithTaxYen else items.sumOf { it.originalTotalWithTax },
    val isTaxFreeEligible: Boolean = (currency == "JPY" && subtotalWithoutTaxYen >= 5000.0),
    val goodsType: GoodsType = GoodsType.GENERAL_GOODS,
    val taxFreeStatus: TaxFreeClaimStatus = if (isTaxFreeEligible) TaxFreeClaimStatus.PENDING else TaxFreeClaimStatus.NOT_APPLICABLE,
    val category: ExpenseCategory = ExpenseCategory.SHOPPING,
    val photoFilePath: String? = null,
    val notes: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class CategoryExpenseBreakdown(
    val category: ExpenseCategory,
    val totalYen: Double,
    val totalEur: Double,
    val percentageOfTotal: Double,
    val receiptCount: Int
)

data class DayExpenseBreakdown(
    val dateIso: String,
    val totalYen: Double,
    val totalEur: Double,
    val receiptCount: Int
)

data class CityExpenseBreakdown(
    val city: String,
    val totalYen: Double,
    val totalEur: Double,
    val receiptCount: Int
)

data class TravelBudgetSummary(
    val totalSpentYen: Double,
    val totalSpentEur: Double,
    val totalTaxFreeRecoverableYen: Double,
    val totalTaxFreeRecoverableEur: Double,
    val byCategory: List<CategoryExpenseBreakdown>,
    val byDay: List<DayExpenseBreakdown>,
    val byCity: List<CityExpenseBreakdown>,
    val exchangeRateUsed: Double
)

data class TaxFreeStoreGroup(
    val storeName: String,
    val dateIso: String,
    val receiptCount: Int,
    val subtotalWithoutTaxYen: Double,
    val totalTaxYen: Double,
    val totalWithTaxYen: Double,
    val goodsType: GoodsType,
    val status: TaxFreeClaimStatus,
    val receipts: List<ShoppingReceipt>
)

data class TaxFreeReport(
    val generatedAtIso: String,
    val storeGroups: List<TaxFreeStoreGroup>,
    val totalReceiptsCount: Int,
    val totalSubtotalWithoutTaxYen: Double,
    val totalRecoverableTaxYen: Double,
    val totalRecoverableTaxEur: Double,
    val statusBreakdown: Map<TaxFreeClaimStatus, Int>,
    val exchangeRateUsed: Double,
    val textReport: String,
    val markdownReport: String,
    val jsonExport: String
)

enum class ScannerSource {
    GEMINI_CLOUD_VISION,
    LOCAL_MLKIT_OCR
}

data class ReceiptScanResult(
    val receipt: ShoppingReceipt,
    val rawText: String,
    val source: ScannerSource,
    val warning: String? = null,
    val detectedCurrency: String? = null,
    val isCurrencyAmbiguous: Boolean = (detectedCurrency == null)
)

object TaxFreeCalculator {

    const val TAX_FREE_MIN_YEN: Double = 5000.0

    fun createReceipt(
        id: String = UUID.randomUUID().toString(),
        purchaseDateIso: String = Instant.now().toString(),
        storeName: String,
        location: String = "Tokyo",
        items: List<ReceiptProductItem>,
        category: ExpenseCategory = ExpenseCategory.SHOPPING,
        taxFreeStatus: TaxFreeClaimStatus? = null,
        photoFilePath: String? = null,
        notes: String = "",
        currency: String = "JPY",
        originalSubtotal: Double? = null,
        originalTax: Double? = null,
        originalTotal: Double? = null,
        eurToJpyRate: Double = 164.80,
        usdToEurRate: Double = 1.084
    ): ShoppingReceipt {
        val isJpy = currency.equals("JPY", ignoreCase = true)
        val rateToJpy = when (currency.uppercase(Locale.ROOT)) {
            "EUR" -> if (eurToJpyRate > 0) eurToJpyRate else 164.80
            "USD" -> {
                val eRate = if (eurToJpyRate > 0) eurToJpyRate else 164.80
                val uRate = if (usdToEurRate > 0) usdToEurRate else 1.084
                eRate / uRate
            }
            else -> 1.0
        }

        val origSub = originalSubtotal ?: items.sumOf { it.originalSubtotalWithoutTax }
        val origTax = originalTax ?: items.sumOf { it.originalTax }
        val origTotal = originalTotal ?: items.sumOf { it.originalTotalWithTax }

        val subtotalWithoutTax = if (isJpy) origSub else Math.round(origSub * rateToJpy).toDouble()
        val totalTax = if (isJpy) origTax else Math.round(origTax * rateToJpy).toDouble()
        val totalWithTax = if (isJpy) origTotal else (subtotalWithoutTax + totalTax)

        val isDutyFree = storeName.contains("Duty Free", ignoreCase = true) || storeName.contains("Tax Free", ignoreCase = true)
        val isEligible = (isJpy || isDutyFree) && subtotalWithoutTax >= TAX_FREE_MIN_YEN

        val has8 = items.any { it.taxRatePercent == 8.0 }
        val has10 = items.any { it.taxRatePercent == 10.0 }
        val goodsType = when {
            has8 && has10 -> GoodsType.MIXED
            has8 -> GoodsType.CONSUMABLES
            else -> GoodsType.GENERAL_GOODS
        }

        val finalStatus = taxFreeStatus ?: if (isEligible) TaxFreeClaimStatus.PENDING else TaxFreeClaimStatus.NOT_APPLICABLE

        return ShoppingReceipt(
            id = id,
            purchaseDateIso = purchaseDateIso,
            storeName = storeName,
            location = location,
            items = items,
            subtotalWithoutTaxYen = subtotalWithoutTax,
            totalTaxYen = totalTax,
            totalWithTaxYen = totalWithTax,
            currency = currency.uppercase(Locale.ROOT),
            originalSubtotal = origSub,
            originalTotalTax = origTax,
            originalTotalWithTax = origTotal,
            isTaxFreeEligible = isEligible,
            goodsType = goodsType,
            taxFreeStatus = finalStatus,
            category = category,
            photoFilePath = photoFilePath,
            notes = notes,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun calculateBudgetSummary(
        receipts: List<ShoppingReceipt>,
        eurToJpyRate: Double
    ): TravelBudgetSummary {
        val rate = if (eurToJpyRate > 0.0) eurToJpyRate else 160.0
        val totalSpentYen = receipts.sumOf { it.totalWithTaxYen }
        val totalSpentEur = totalSpentYen / rate

        val eligibleReceipts = receipts.filter {
            it.isTaxFreeEligible &&
                (it.taxFreeStatus == TaxFreeClaimStatus.PENDING || it.taxFreeStatus == TaxFreeClaimStatus.CUSTOMS_STAMPED)
        }
        val totalTaxFreeRecoverableYen = eligibleReceipts.sumOf { it.totalTaxYen }
        val totalTaxFreeRecoverableEur = totalTaxFreeRecoverableYen / rate

        // Desglose por Categoría
        val byCategory = ExpenseCategory.values().mapNotNull { cat ->
            val matching = receipts.filter { it.category == cat }
            if (matching.isEmpty()) {
                null
            } else {
                val catYen = matching.sumOf { it.totalWithTaxYen }
                val catEur = catYen / rate
                val percentage = if (totalSpentYen > 0.0) (catYen / totalSpentYen) * 100.0 else 0.0
                CategoryExpenseBreakdown(
                    category = cat,
                    totalYen = catYen,
                    totalEur = catEur,
                    percentageOfTotal = percentage,
                    receiptCount = matching.size
                )
            }
        }.sortedByDescending { it.totalYen }

        // Desglose por Día (primeros 10 caracteres del ISO date: YYYY-MM-DD)
        val byDay = receipts.groupBy { it.purchaseDateIso.take(10) }
            .map { (date, recs) ->
                val dayYen = recs.sumOf { it.totalWithTaxYen }
                DayExpenseBreakdown(
                    dateIso = date,
                    totalYen = dayYen,
                    totalEur = dayYen / rate,
                    receiptCount = recs.size
                )
            }.sortedBy { it.dateIso }

        // Desglose por Ciudad
        val byCity = receipts.groupBy { it.location.trim().ifBlank { "Tokyo" } }
            .map { (city, recs) ->
                val cityYen = recs.sumOf { it.totalWithTaxYen }
                CityExpenseBreakdown(
                    city = city,
                    totalYen = cityYen,
                    totalEur = cityYen / rate,
                    receiptCount = recs.size
                )
            }.sortedByDescending { it.totalYen }

        return TravelBudgetSummary(
            totalSpentYen = totalSpentYen,
            totalSpentEur = totalSpentEur,
            totalTaxFreeRecoverableYen = totalTaxFreeRecoverableYen,
            totalTaxFreeRecoverableEur = totalTaxFreeRecoverableEur,
            byCategory = byCategory,
            byDay = byDay,
            byCity = byCity,
            exchangeRateUsed = rate
        )
    }
}

object AiReceiptScannerEngine {

    fun parseGeminiJsonResponse(jsonString: String): ShoppingReceipt {
        return ShoppingReceiptsJsonCodec.parseGeminiJson(jsonString)
    }

    fun scanWithFallback(
        rawOcrText: String,
        geminiApiKey: String?,
        imageBytes: ByteArray?,
        eurToJpyRate: Double = 164.80,
        usdToEurRate: Double = 1.084
    ): ReceiptScanResult {
        if (!geminiApiKey.isNullOrBlank() && imageBytes != null && imageBytes.isNotEmpty()) {
            try {
                // Invocación a Gemini Vision API si hay red y credenciales.
            } catch (_: Exception) {
                // Fallback graceful
            }
        }

        val detectedCur = JapaneseReceiptOcrParser.detectCurrency(rawOcrText)
        val localReceipt = JapaneseReceiptOcrParser.parse(
            rawOcrText = rawOcrText,
            forcedCurrency = detectedCur,
            eurToJpyRate = eurToJpyRate,
            usdToEurRate = usdToEurRate
        )
        return ReceiptScanResult(
            receipt = localReceipt,
            rawText = rawOcrText,
            source = ScannerSource.LOCAL_MLKIT_OCR,
            detectedCurrency = detectedCur,
            isCurrencyAmbiguous = (detectedCur == null)
        )
    }
}

object ShoppingReceiptsJsonCodec {

    private fun escape(s: String): String =
        s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    private fun unescape(s: String): String =
        s.replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")

    fun extractString(json: String, key: String): String? {
        val pattern = Regex("""\"$key\"\s*:\s*\"((?:[^\"\\]|\\.)*)\"""")
        val match = pattern.find(json) ?: return null
        return unescape(match.groupValues[1])
    }

    fun extractDouble(json: String, key: String): Double? {
        val pattern = Regex("""\"$key\"\s*:\s*([0-9.]+)""")
        val match = pattern.find(json) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }

    fun extractInt(json: String, key: String): Int? {
        val pattern = Regex("""\"$key\"\s*:\s*([0-9]+)""")
        val match = pattern.find(json) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    fun extractBoolean(json: String, key: String): Boolean? {
        val pattern = Regex("""\"$key\"\s*:\s*(true|false)""", RegexOption.IGNORE_CASE)
        val match = pattern.find(json) ?: return null
        return match.groupValues[1].toBoolean()
    }

    fun exportReceipt(receipt: ShoppingReceipt): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"id\": \"${escape(receipt.id)}\",")
        sb.appendLine("  \"purchaseDateIso\": \"${escape(receipt.purchaseDateIso)}\",")
        sb.appendLine("  \"storeName\": \"${escape(receipt.storeName)}\",")
        sb.appendLine("  \"location\": \"${escape(receipt.location)}\",")
        sb.appendLine("  \"currency\": \"${escape(receipt.currency)}\",")
        sb.appendLine("  \"originalSubtotal\": ${receipt.originalSubtotal},")
        sb.appendLine("  \"originalTotalTax\": ${receipt.originalTotalTax},")
        sb.appendLine("  \"originalTotalWithTax\": ${receipt.originalTotalWithTax},")
        sb.appendLine("  \"subtotalWithoutTaxYen\": ${receipt.subtotalWithoutTaxYen},")
        sb.appendLine("  \"totalTaxYen\": ${receipt.totalTaxYen},")
        sb.appendLine("  \"totalWithTaxYen\": ${receipt.totalWithTaxYen},")
        sb.appendLine("  \"isTaxFreeEligible\": ${receipt.isTaxFreeEligible},")
        sb.appendLine("  \"goodsType\": \"${receipt.goodsType.name}\",")
        sb.appendLine("  \"taxFreeStatus\": \"${receipt.taxFreeStatus.name}\",")
        sb.appendLine("  \"category\": \"${receipt.category.name}\",")
        sb.appendLine("  \"photoFilePath\": ${if (receipt.photoFilePath != null) "\"${escape(receipt.photoFilePath)}\"" else "null"},")
        sb.appendLine("  \"notes\": \"${escape(receipt.notes)}\",")
        sb.appendLine("  \"updatedAtMillis\": ${receipt.updatedAtMillis},")
        sb.appendLine("  \"items\": [")
        receipt.items.forEachIndexed { idx, it ->
            val comma = if (idx < receipt.items.lastIndex) "," else ""
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escape(it.id)}\",")
            sb.appendLine("      \"name\": \"${escape(it.name)}\",")
            sb.appendLine("      \"quantity\": ${it.quantity},")
            sb.appendLine("      \"currency\": \"${escape(it.currency)}\",")
            sb.appendLine("      \"originalUnitPrice\": ${it.originalUnitPrice},")
            sb.appendLine("      \"originalSubtotalWithoutTax\": ${it.originalSubtotalWithoutTax},")
            sb.appendLine("      \"originalTax\": ${it.originalTax},")
            sb.appendLine("      \"originalTotalWithTax\": ${it.originalTotalWithTax},")
            sb.appendLine("      \"unitPriceYen\": ${it.unitPriceYen},")
            sb.appendLine("      \"taxRatePercent\": ${it.taxRatePercent},")
            sb.appendLine("      \"subtotalWithoutTaxYen\": ${it.subtotalWithoutTaxYen},")
            sb.appendLine("      \"taxYen\": ${it.taxYen},")
            sb.appendLine("      \"totalWithTaxYen\": ${it.totalWithTaxYen}")
            sb.appendLine("    }$comma")
        }
        sb.appendLine("  ]")
        sb.append("}")
        return sb.toString()
    }

    fun importReceipt(json: String): ShoppingReceipt {
        val id = extractString(json, "id") ?: UUID.randomUUID().toString()
        val purchaseDateIso = extractString(json, "purchaseDateIso") ?: Instant.now().toString()
        val storeName = extractString(json, "storeName") ?: "Store"
        val location = extractString(json, "location") ?: "Tokyo"
        val currency = extractString(json, "currency") ?: "JPY"
        val goodsTypeStr = extractString(json, "goodsType")
        val goodsType = GoodsType.values().firstOrNull { it.name.equals(goodsTypeStr, ignoreCase = true) } ?: GoodsType.GENERAL_GOODS
        val taxFreeStatusStr = extractString(json, "taxFreeStatus")
        val taxFreeStatus = TaxFreeClaimStatus.values().firstOrNull { it.name.equals(taxFreeStatusStr, ignoreCase = true) } ?: TaxFreeClaimStatus.NOT_APPLICABLE
        val categoryStr = extractString(json, "category")
        val category = ExpenseCategory.values().firstOrNull { it.name.equals(categoryStr, ignoreCase = true) } ?: ExpenseCategory.SHOPPING
        val photoFilePath = extractString(json, "photoFilePath")
        val notes = extractString(json, "notes") ?: ""
        val updatedAtMillis = extractDouble(json, "updatedAtMillis")?.toLong() ?: System.currentTimeMillis()

        val items = extractItemsList(json)
        val subtotal = extractDouble(json, "subtotalWithoutTaxYen") ?: items.sumOf { it.subtotalWithoutTaxYen }
        val tax = extractDouble(json, "totalTaxYen") ?: items.sumOf { it.taxYen }
        val total = extractDouble(json, "totalWithTaxYen") ?: items.sumOf { it.totalWithTaxYen }
        val originalSubtotal = extractDouble(json, "originalSubtotal") ?: if (currency == "JPY") subtotal else items.sumOf { it.originalSubtotalWithoutTax }
        val originalTotalTax = extractDouble(json, "originalTotalTax") ?: if (currency == "JPY") tax else items.sumOf { it.originalTax }
        val originalTotalWithTax = extractDouble(json, "originalTotalWithTax") ?: if (currency == "JPY") total else items.sumOf { it.originalTotalWithTax }
        val isTaxFreeEligible = extractBoolean(json, "isTaxFreeEligible") ?: (currency == "JPY" && subtotal >= TaxFreeCalculator.TAX_FREE_MIN_YEN)

        return ShoppingReceipt(
            id = id,
            purchaseDateIso = purchaseDateIso,
            storeName = storeName,
            location = location,
            items = items,
            subtotalWithoutTaxYen = subtotal,
            totalTaxYen = tax,
            totalWithTaxYen = total,
            currency = currency,
            originalSubtotal = originalSubtotal,
            originalTotalTax = originalTotalTax,
            originalTotalWithTax = originalTotalWithTax,
            isTaxFreeEligible = isTaxFreeEligible,
            goodsType = goodsType,
            taxFreeStatus = taxFreeStatus,
            category = category,
            photoFilePath = photoFilePath,
            notes = notes,
            updatedAtMillis = updatedAtMillis
        )
    }

    private fun extractItemsList(json: String): List<ReceiptProductItem> {
        val itemsRegex = Regex("""\"items\"\s*:\s*\[([\s\S]*?)\]\s*(?:,|\})""")
        val itemsBlock = itemsRegex.find(json)?.groupValues?.get(1) ?: return emptyList()

        val itemObjRegex = Regex("""\{([^{}]+)\}""")
        val matches = itemObjRegex.findAll(itemsBlock)
        return matches.map { m ->
            val itemJson = m.value
            val id = extractString(itemJson, "id") ?: UUID.randomUUID().toString()
            val name = extractString(itemJson, "name") ?: "Item"
            val qty = extractInt(itemJson, "quantity") ?: 1
            val unitPrice = extractDouble(itemJson, "unitPriceYen") ?: 0.0
            val taxRate = extractDouble(itemJson, "taxRatePercent") ?: 10.0
            val sub = extractDouble(itemJson, "subtotalWithoutTaxYen") ?: (qty * unitPrice)
            val tax = extractDouble(itemJson, "taxYen") ?: Math.round(sub * (taxRate / 100.0)).toDouble()
            val total = extractDouble(itemJson, "totalWithTaxYen") ?: (sub + tax)
            val cur = extractString(itemJson, "currency") ?: "JPY"
            val origUnitPrice = extractDouble(itemJson, "originalUnitPrice") ?: if (cur == "JPY") unitPrice else 0.0
            val origSub = extractDouble(itemJson, "originalSubtotalWithoutTax") ?: (qty * origUnitPrice)
            val origTax = extractDouble(itemJson, "originalTax") ?: Math.round(origSub * (taxRate / 100.0) * 100.0) / 100.0
            val origTotal = extractDouble(itemJson, "originalTotalWithTax") ?: (origSub + origTax)

            ReceiptProductItem(
                id = id,
                name = name,
                quantity = qty,
                unitPriceYen = unitPrice,
                taxRatePercent = taxRate,
                subtotalWithoutTaxYen = sub,
                taxYen = tax,
                totalWithTaxYen = total,
                currency = cur,
                originalUnitPrice = origUnitPrice,
                originalSubtotalWithoutTax = origSub,
                originalTax = origTax,
                originalTotalWithTax = origTotal
            )
        }.toList()
    }

    fun exportReceiptList(list: List<ShoppingReceipt>): String {
        val sb = StringBuilder()
        sb.appendLine("[")
        list.forEachIndexed { index, item ->
            val comma = if (index < list.lastIndex) "," else ""
            sb.appendLine(exportReceipt(item).prependIndent("  ") + comma)
        }
        sb.appendLine("]")
        return sb.toString()
    }

    fun importReceiptList(json: String): List<ShoppingReceipt> {
        val clean = json.trim()
        if (clean.isBlank()) return emptyList()

        // Buscar bloques de objetos delimitados por llaves que contengan "storeName"
        val receipts = mutableListOf<ShoppingReceipt>()
        var depth = 0
        var startIndex = -1

        for (i in clean.indices) {
            when (clean[i]) {
                '{' -> {
                    if (depth == 0) startIndex = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && startIndex >= 0) {
                        val objJson = clean.substring(startIndex, i + 1)
                        if (objJson.contains("\"storeName\"")) {
                            runCatching {
                                receipts.add(importReceipt(objJson))
                            }
                        }
                        startIndex = -1
                    }
                }
            }
        }
        return receipts
    }

    fun parseGeminiJson(jsonString: String): ShoppingReceipt {
        val clean = jsonString.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val storeName = extractString(clean, "storeName") ?: "Japanese Store"
        val dateIso = extractString(clean, "dateIso") ?: Instant.now().toString()
        val location = extractString(clean, "location") ?: "Tokyo"
        val categoryStr = extractString(clean, "category")
        val category = ExpenseCategory.values().firstOrNull { it.name.equals(categoryStr, ignoreCase = true) }
            ?: ExpenseCategory.SHOPPING

        val items = extractItemsList(clean)

        return TaxFreeCalculator.createReceipt(
            storeName = storeName,
            location = location,
            purchaseDateIso = dateIso,
            items = items,
            category = category
        )
    }
}
