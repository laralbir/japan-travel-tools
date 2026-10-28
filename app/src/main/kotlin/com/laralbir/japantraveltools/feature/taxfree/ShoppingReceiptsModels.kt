package com.laralbir.japantraveltools.feature.taxfree

import java.time.Instant
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
    val totalWithTaxYen: Double = subtotalWithoutTaxYen + taxYen
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
    val isTaxFreeEligible: Boolean = subtotalWithoutTaxYen >= 5000.0,
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
    val warning: String? = null
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
        notes: String = ""
    ): ShoppingReceipt {
        val subtotalWithoutTax = items.sumOf { it.subtotalWithoutTaxYen }
        val totalTax = items.sumOf { it.taxYen }
        val totalWithTax = items.sumOf { it.totalWithTaxYen }
        val isEligible = subtotalWithoutTax >= TAX_FREE_MIN_YEN

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
        imageBytes: ByteArray?
    ): ReceiptScanResult {
        if (!geminiApiKey.isNullOrBlank() && imageBytes != null && imageBytes.isNotEmpty()) {
            try {
                // Invocación a Gemini Vision API si hay red y credenciales.
            } catch (_: Exception) {
                // Fallback graceful
            }
        }

        val localReceipt = JapaneseReceiptOcrParser.parse(rawOcrText)
        return ReceiptScanResult(
            receipt = localReceipt,
            rawText = rawOcrText,
            source = ScannerSource.LOCAL_MLKIT_OCR
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
        val isTaxFreeEligible = extractBoolean(json, "isTaxFreeEligible") ?: (subtotal >= TaxFreeCalculator.TAX_FREE_MIN_YEN)

        return ShoppingReceipt(
            id = id,
            purchaseDateIso = purchaseDateIso,
            storeName = storeName,
            location = location,
            items = items,
            subtotalWithoutTaxYen = subtotal,
            totalTaxYen = tax,
            totalWithTaxYen = total,
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
            ReceiptProductItem(
                id = id,
                name = name,
                quantity = qty,
                unitPriceYen = unitPrice,
                taxRatePercent = taxRate,
                subtotalWithoutTaxYen = sub,
                taxYen = tax,
                totalWithTaxYen = total
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
