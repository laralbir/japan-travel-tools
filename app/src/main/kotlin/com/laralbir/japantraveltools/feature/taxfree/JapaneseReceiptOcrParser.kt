package com.laralbir.japantraveltools.feature.taxfree

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

object JapaneseReceiptOcrParser {

    private val JST_ZONE = ZoneId.of("Asia/Tokyo")

    fun parse(rawOcrText: String): ShoppingReceipt {
        val lines = rawOcrText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val storeName = detectStoreName(lines)
        val location = detectLocation(rawOcrText)
        val purchaseDateIso = detectPurchaseDateIso(rawOcrText)
        val category = detectCategory(storeName, rawOcrText)

        // Totales explícitos
        val explicitSubtotal = extractAmount(rawOcrText, listOf("小計", "免税対象額", "免税対象", "対象額"))
        val explicitTax = extractTotalTax(rawOcrText)
        val explicitTotal = extractAmount(rawOcrText, listOf("合計", "お買上合計", "支払合計"))

        // Extracción de líneas de productos
        val items = extractProductItems(lines)

        val finalItems = if (items.isNotEmpty()) {
            items
        } else {
            // Si no se pudieron desglosar artículos línea a línea, creamos un ítem sintético con los totales encontrados
            val basePrice = explicitSubtotal ?: if (explicitTotal != null && explicitTax != null) {
                explicitTotal - explicitTax
            } else {
                explicitTotal ?: 1000.0
            }
            val taxAmount = explicitTax ?: Math.round(basePrice * 0.10).toDouble()
            val rate = if (basePrice > 0) Math.round((taxAmount / basePrice) * 100.0).toDouble() else 10.0
            listOf(
                ReceiptProductItem(
                    id = UUID.randomUUID().toString(),
                    name = "Compra en $storeName",
                    quantity = 1,
                    unitPriceYen = basePrice,
                    taxRatePercent = if (rate in 7.0..9.0) 8.0 else 10.0
                )
            )
        }

        val subtotal = explicitSubtotal ?: finalItems.sumOf { it.subtotalWithoutTaxYen }
        val tax = explicitTax ?: finalItems.sumOf { it.taxYen }
        val total = explicitTotal ?: (subtotal + tax)

        val isEligible = subtotal >= TaxFreeCalculator.TAX_FREE_MIN_YEN ||
            rawOcrText.contains("免税", ignoreCase = true) ||
            rawOcrText.contains("TAX FREE", ignoreCase = true)

        val has8 = finalItems.any { it.taxRatePercent == 8.0 } || rawOcrText.contains("8%") || rawOcrText.contains("軽")
        val has10 = finalItems.any { it.taxRatePercent == 10.0 } || rawOcrText.contains("10%")
        val goodsType = when {
            has8 && has10 -> GoodsType.MIXED
            has8 -> GoodsType.CONSUMABLES
            else -> GoodsType.GENERAL_GOODS
        }

        val status = if (isEligible) TaxFreeClaimStatus.PENDING else TaxFreeClaimStatus.NOT_APPLICABLE

        return ShoppingReceipt(
            id = UUID.randomUUID().toString(),
            purchaseDateIso = purchaseDateIso,
            storeName = storeName,
            location = location,
            items = finalItems,
            subtotalWithoutTaxYen = subtotal,
            totalTaxYen = tax,
            totalWithTaxYen = total,
            isTaxFreeEligible = isEligible,
            goodsType = goodsType,
            taxFreeStatus = status,
            category = category,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    private fun detectStoreName(lines: List<String>): String {
        for (line in lines.take(6)) {
            when {
                line.contains("ドン・キホーテ", ignoreCase = true) || line.contains("Don Quijote", ignoreCase = true) -> return line
                line.contains("ビックカメラ", ignoreCase = true) || line.contains("BIC CAMERA", ignoreCase = true) -> return line
                line.contains("ヨドバシ", ignoreCase = true) || line.contains("YODOBASHI", ignoreCase = true) -> return line
                line.contains("ユニクロ", ignoreCase = true) || line.contains("UNIQLO", ignoreCase = true) -> return line
                line.contains("セブン-イレブン", ignoreCase = true) || line.contains("7-Eleven", ignoreCase = true) -> return line
                line.contains("ファミリーマート", ignoreCase = true) || line.contains("FamilyMart", ignoreCase = true) -> return line
                line.contains("ローソン", ignoreCase = true) || line.contains("LAWSON", ignoreCase = true) -> return line
                line.contains("マツモトキヨシ", ignoreCase = true) || line.contains("Matsumoto", ignoreCase = true) -> return line
            }
        }
        return lines.firstOrNull()?.take(30) ?: "Japanese Store"
    }

    private fun detectLocation(text: String): String {
        return when {
            text.contains("道頓堀") || text.contains("なんば") || text.contains("難波") ||
                text.contains("梅田") || text.contains("大阪") -> "Osaka"
            text.contains("京都") || text.contains("祇園") || text.contains("清水") ||
                text.contains("河原町") -> "Kyoto"
            text.contains("奈良") -> "Nara"
            text.contains("広島") -> "Hiroshima"
            text.contains("名古屋") -> "Nagoya"
            text.contains("福岡") || text.contains("博多") -> "Fukuoka"
            text.contains("札幌") -> "Sapporo"
            text.contains("箱根") -> "Hakone"
            else -> "Tokyo"
        }
    }

    private fun detectCategory(storeName: String, text: String): ExpenseCategory {
        val lower = (storeName + " " + text).lowercase()
        return when {
            lower.contains("セブン-イレブン") || lower.contains("7-eleven") ||
                lower.contains("familymart") || lower.contains("ファミリーマート") ||
                lower.contains("lawson") || lower.contains("ローソン") ||
                lower.contains("おにぎり") || lower.contains("ラーメン") ||
                lower.contains("寿司") || lower.contains("カフェ") ||
                lower.contains("珈琲") || lower.contains("レストラン") -> ExpenseCategory.FOOD

            lower.contains("jr") || lower.contains("地下鉄") || lower.contains("メトロ") ||
                lower.contains("切符") || lower.contains("乗車券") -> ExpenseCategory.TRANSPORT

            lower.contains("入場券") || lower.contains("拝観料") || lower.contains("チケット") ||
                lower.contains("museum") || lower.contains("寺") || lower.contains("神社") -> ExpenseCategory.LEISURE

            lower.contains("ホテル") || lower.contains("hotel") || lower.contains("ryokan") ||
                lower.contains("旅館") -> ExpenseCategory.ACCOMMODATION

            else -> ExpenseCategory.SHOPPING
        }
    }

    private fun detectPurchaseDateIso(text: String): String {
        try {
            // YYYY/MM/DD o YYYY-MM-DD
            val regexG = Regex("""(\d{4})[/-](\d{1,2})[/-](\d{1,2})""")
            val matchG = regexG.find(text)
            if (matchG != null) {
                val y = matchG.groupValues[1].toInt()
                val m = matchG.groupValues[2].toInt()
                val d = matchG.groupValues[3].toInt()
                val localDate = LocalDate.of(y, m, d)

                // Buscar hora HH:mm
                val regexT = Regex("""(\d{1,2}):(\d{2})""")
                val matchT = regexT.find(text)
                val localTime = if (matchT != null) {
                    val hh = matchT.groupValues[1].toInt()
                    val mm = matchT.groupValues[2].toInt()
                    LocalTime.of(hh, mm)
                } else {
                    LocalTime.of(12, 0)
                }

                val zdt = ZonedDateTime.of(localDate, localTime, JST_ZONE)
                return zdt.toInstant().toString()
            }

            // Reiwa era: R06.03.15 o R7/03/15
            val regexR = Regex("""R(\d{1,2})[./-](\d{1,2})[./-](\d{1,2})""", RegexOption.IGNORE_CASE)
            val matchR = regexR.find(text)
            if (matchR != null) {
                val rYear = matchR.groupValues[1].toInt()
                val y = 2018 + rYear
                val m = matchR.groupValues[2].toInt()
                val d = matchR.groupValues[3].toInt()
                val zdt = ZonedDateTime.of(LocalDate.of(y, m, d), LocalTime.of(12, 0), JST_ZONE)
                return zdt.toInstant().toString()
            }
        } catch (_: Exception) {
            // fallback
        }
        return Instant.now().toString()
    }

    private fun extractAmount(text: String, keywords: List<String>): Double? {
        for (kw in keywords) {
            val pattern = Regex("""$kw\s*[:：]?\s*[¥￥]?\s*([0-9,]+)""")
            val match = pattern.find(text)
            if (match != null) {
                val numStr = match.groupValues[1].replace(",", "")
                val parsed = numStr.toDoubleOrNull()
                if (parsed != null) return parsed
            }
        }
        return null
    }

    private fun extractTotalTax(text: String): Double? {
        var accumulatedTax = 0.0
        var found = false

        // Buscar múltiples líneas de impuesto (ej. 8% y 10%)
        val taxPattern = Regex("""(?:内消費税(?:等)?|消費税等?|外税)\s*(?:\([^)]*\)|8%|10%)?\s*[:：]?\s*[¥￥]?\s*([0-9,]+)""")
        val matches = taxPattern.findAll(text)
        for (m in matches) {
            val numStr = m.groupValues[1].replace(",", "")
            val parsed = numStr.toDoubleOrNull()
            if (parsed != null && parsed > 0) {
                accumulatedTax += parsed
                found = true
            }
        }

        return if (found) accumulatedTax else null
    }

    private fun extractProductItems(lines: List<String>): List<ReceiptProductItem> {
        val items = mutableListOf<ReceiptProductItem>()
        val skipKeywords = listOf("小計", "合計", "消費税", "税", "免税", "お釣", "預り", "TEL", "レシート", "領収書", "TAX")

        for (line in lines) {
            if (skipKeywords.any { line.startsWith(it) || line.contains("合計") || line.contains("小計") }) {
                continue
            }

            // Buscar si la línea termina con un número de precio
            val priceMatch = Regex("""\s+([0-9,]{3,})\s*$""").find(line)
            if (priceMatch != null) {
                val priceStr = priceMatch.groupValues[1].replace(",", "")
                val price = priceStr.toDoubleOrNull()
                if (price != null && price > 0) {
                    val rawName = line.substring(0, priceMatch.range.first).trim()
                    if (rawName.isNotBlank() && !skipKeywords.any { rawName.contains(it) }) {
                        val isReduced = line.contains("8%") || line.contains("*") || line.contains("軽") || line.contains("※")
                        val taxRate = if (isReduced) 8.0 else 10.0
                        items.add(
                            ReceiptProductItem(
                                id = UUID.randomUUID().toString(),
                                name = rawName,
                                quantity = 1,
                                unitPriceYen = price,
                                taxRatePercent = taxRate
                            )
                        )
                    }
                }
            }
        }

        return items
    }
}
