package com.laralbir.japantraveltools.feature.taxfree

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID

/**
 * Analizador OCR de comprobantes y tickets de compra con soporte trilingüe y multidivisa:
 * - Divisas detectadas: JPY (¥ / 円), EUR (€ / Euro) y USD ($ / US Dollar)
 * - Japonés (日本語): kanjis habituales (小計, 合計, 内消費税, 免税, era Reiwa, etc.)
 * - Inglés (English): términos internacionales y duty-free (SUBTOTAL, TOTAL, TAX, VAT, etc.)
 * - Español (Spanish): comprobantes en español (BASE IMPONIBLE, TOTAL A PAGAR, IVA, etc.)
 */
object JapaneseReceiptOcrParser {

    private val JST_ZONE = ZoneId.of("Asia/Tokyo")

    /**
     * Detecta la divisa del ticket a partir del texto OCR.
     * Retorna "JPY", "EUR", "USD" o null si la divisa es ambigua o no se detecta con seguridad.
     */
    fun detectCurrency(text: String): String? {
        val hasEuroSymbol = text.contains("€")
        val hasEuroWord = Regex("""\b(?:EUR|EUROS?)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
            Regex("""[0-9,.]+\s*(?:€|EUR)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
            Regex("""(?:€|EUR)\s*[0-9,.]+""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val isEuro = hasEuroSymbol || hasEuroWord

        val hasUsdWord = Regex("""\b(?:USD|US\$|DOLLARS?|DÓLARES?)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val hasDollarSymbol = text.contains("$")
        val isUsd = hasUsdWord || (hasDollarSymbol && !isEuro)

        val hasYenSymbol = text.contains("¥") || text.contains("￥") || text.contains("円")
        val hasYenWord = Regex("""\b(?:JPY|YEN)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val hasJapaneseKanjiClues = text.contains("合計") || text.contains("小計") || text.contains("内消費税") ||
            text.contains("消費税") || text.contains("税抜") || text.contains("ドン・キホーテ") ||
            text.contains("ビックカメラ") || text.contains("ヨドバシ") || text.contains("ユニクロ") ||
            text.contains("セブン-イレブン") || text.contains("ファミリーマート") || text.contains("ローソン")
        val isJpy = hasYenSymbol || hasYenWord || hasJapaneseKanjiClues

        return when {
            isEuro && !isUsd && !hasYenSymbol -> "EUR"
            isUsd && !isEuro && !hasYenSymbol -> "USD"
            isJpy && !isEuro && !isUsd -> "JPY"
            else -> null
        }
    }

    fun isCurrencyAmbiguous(text: String): Boolean = detectCurrency(text) == null

    /**
     * Parsea importes numéricos en formato internacional (12.50 / 1,250.00), europeo (12,50 / 1.250,00) o enteros (1250).
     */
    fun parsePriceString(raw: String): Double? {
        val clean = raw.trim()
            .replace("¥", "")
            .replace("￥", "")
            .replace("$", "")
            .replace("€", "")
            .replace("円", "")
            .replace("USD", "", ignoreCase = true)
            .replace("EUR", "", ignoreCase = true)
            .replace("JPY", "", ignoreCase = true)
            .trim()
        if (clean.isBlank()) return null

        // 1. Formato europeo con punto como miles y coma decimal: 1.250,50
        if (clean.matches(Regex("""\d{1,3}(?:\.\d{3})+,\d{1,2}"""))) {
            return clean.replace(".", "").replace(",", ".").toDoubleOrNull()
        }
        // 2. Coma como separador decimal simple: 20,00 o 2,50
        if (clean.matches(Regex("""\d+,\d{1,2}"""))) {
            return clean.replace(",", ".").toDoubleOrNull()
        }
        // 3. Formato estándar US/JP con coma de miles y punto decimal (o sin decimales): 1,250.50 o 5000 o 50.00
        return clean.replace(",", "").toDoubleOrNull()
    }

    fun parse(
        rawOcrText: String,
        forcedCurrency: String? = null,
        eurToJpyRate: Double = 164.80,
        usdToEurRate: Double = 1.084
    ): ShoppingReceipt {
        val detected = detectCurrency(rawOcrText)
        val effectiveCurrency = forcedCurrency?.uppercase(Locale.ROOT) ?: detected ?: "JPY"
        val isJpy = effectiveCurrency == "JPY"

        val rateToJpy = when (effectiveCurrency) {
            "EUR" -> if (eurToJpyRate > 0) eurToJpyRate else 164.80
            "USD" -> {
                val eRate = if (eurToJpyRate > 0) eurToJpyRate else 164.80
                val uRate = if (usdToEurRate > 0) usdToEurRate else 1.084
                eRate / uRate
            }
            else -> 1.0
        }

        val lines = rawOcrText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val storeName = detectStoreName(lines)
        val location = detectLocation(rawOcrText)
        val purchaseDateIso = detectPurchaseDateIso(rawOcrText)
        val category = detectCategory(storeName, rawOcrText)

        // Totales explícitos (soporta Japonés, Inglés y Español)
        val explicitSubtotal = extractAmount(
            rawOcrText,
            listOf(
                // Japonés
                "小計", "免税対象額", "免税対象", "対象額", "税抜", "税抜き", "本体価格", "本体計",
                // Inglés
                "SUBTOTAL", "SUB TOTAL", "SUB-TOTAL", "NET AMOUNT", "NET TOTAL", "TAXABLE AMOUNT", "TAX EXCLUDED", "BEFORE TAX",
                // Español
                "BASE IMPONIBLE", "BASE IMP", "IMPORTE NETO", "TOTAL NETO", "SIN IVA"
            )
        )
        val explicitTax = extractTotalTax(rawOcrText)
        val explicitTotal = extractAmount(
            rawOcrText,
            listOf(
                // Japonés
                "合計", "お買上合計", "支払合計", "税込合計", "請求額", "お買上計",
                // Inglés
                "GRAND TOTAL", "TOTAL AMOUNT", "TOTAL DUE", "AMOUNT DUE", "TOTAL PAID", "BALANCE DUE", "TOTAL",
                // Español
                "TOTAL A PAGAR", "TOTAL FACTURA", "TOTAL TICKET", "IMPORTE TOTAL", "PAGADO", "TOTAL COMPRA"
            )
        )

        // Extracción de líneas de productos
        val items = extractProductItems(lines, effectiveCurrency, rateToJpy)

        val finalItems = if (items.isNotEmpty()) {
            items
        } else {
            val basePrice = explicitSubtotal ?: if (explicitTotal != null && explicitTax != null) {
                explicitTotal - explicitTax
            } else {
                explicitTotal ?: if (isJpy) 1000.0 else 10.0
            }
            val taxAmount = explicitTax ?: Math.round(basePrice * 0.10 * 100.0) / 100.0
            val rate = if (basePrice > 0) Math.round((taxAmount / basePrice) * 100.0).toDouble() else 10.0
            val unitYen = if (isJpy) basePrice else Math.round(basePrice * rateToJpy).toDouble()

            listOf(
                ReceiptProductItem(
                    id = UUID.randomUUID().toString(),
                    name = "Compra en $storeName",
                    quantity = 1,
                    unitPriceYen = unitYen,
                    taxRatePercent = if (rate in 7.0..9.0) 8.0 else 10.0,
                    currency = effectiveCurrency,
                    originalUnitPrice = basePrice
                )
            )
        }

        val origSub = explicitSubtotal ?: finalItems.sumOf { it.originalSubtotalWithoutTax }
        val origTax = explicitTax ?: finalItems.sumOf { it.originalTax }
        val origTotal = explicitTotal ?: (origSub + origTax)

        val subtotalYen = if (isJpy) origSub else Math.round(origSub * rateToJpy).toDouble()
        val totalTaxYen = if (isJpy) origTax else Math.round(origTax * rateToJpy).toDouble()
        val totalWithTaxYen = if (isJpy) origTotal else (subtotalYen + totalTaxYen)

        val isDutyFree = storeName.contains("Duty Free", ignoreCase = true) || storeName.contains("Tax Free", ignoreCase = true)
        val isEligible = (isJpy || isDutyFree) && subtotalYen >= TaxFreeCalculator.TAX_FREE_MIN_YEN ||
            rawOcrText.contains("免税", ignoreCase = true) ||
            rawOcrText.contains("TAX FREE", ignoreCase = true) ||
            rawOcrText.contains("TAX-FREE", ignoreCase = true) ||
            rawOcrText.contains("DUTY FREE", ignoreCase = true) ||
            rawOcrText.contains("DUTY-FREE", ignoreCase = true)

        val has8 = finalItems.any { it.taxRatePercent == 8.0 } ||
            rawOcrText.contains("8%") || rawOcrText.contains("軽") ||
            rawOcrText.contains("8 %")
        val has10 = finalItems.any { it.taxRatePercent == 10.0 } ||
            rawOcrText.contains("10%") || rawOcrText.contains("10 %")
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
            subtotalWithoutTaxYen = subtotalYen,
            totalTaxYen = totalTaxYen,
            totalWithTaxYen = totalWithTaxYen,
            currency = effectiveCurrency,
            originalSubtotal = origSub,
            originalTotalTax = origTax,
            originalTotalWithTax = origTotal,
            isTaxFreeEligible = isEligible,
            goodsType = goodsType,
            taxFreeStatus = status,
            category = category,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    /**
     * Recalcula un comprobante cuando el usuario selecciona o cambia manualmente la divisa.
     */
    fun recalculateWithCurrency(
        receipt: ShoppingReceipt,
        targetCurrency: String,
        eurToJpyRate: Double = 164.80,
        usdToEurRate: Double = 1.084
    ): ShoppingReceipt {
        val cur = targetCurrency.uppercase(Locale.ROOT)
        val isJpy = cur == "JPY"
        val rateToJpy = when (cur) {
            "EUR" -> if (eurToJpyRate > 0) eurToJpyRate else 164.80
            "USD" -> {
                val eRate = if (eurToJpyRate > 0) eurToJpyRate else 164.80
                val uRate = if (usdToEurRate > 0) usdToEurRate else 1.084
                eRate / uRate
            }
            else -> 1.0
        }

        val origSub = receipt.originalSubtotal
        val origTax = receipt.originalTotalTax
        val origTotal = receipt.originalTotalWithTax

        val subtotalYen = if (isJpy) origSub else Math.round(origSub * rateToJpy).toDouble()
        val totalTaxYen = if (isJpy) origTax else Math.round(origTax * rateToJpy).toDouble()
        val totalWithTaxYen = if (isJpy) origTotal else (subtotalYen + totalTaxYen)

        val updatedItems = receipt.items.map { item ->
            val unitYen = if (isJpy) item.originalUnitPrice else Math.round(item.originalUnitPrice * rateToJpy).toDouble()
            item.copy(
                currency = cur,
                unitPriceYen = unitYen
            )
        }

        val isDutyFree = receipt.storeName.contains("Duty Free", ignoreCase = true) || receipt.storeName.contains("Tax Free", ignoreCase = true)
        val isEligible = (isJpy || isDutyFree) && subtotalYen >= TaxFreeCalculator.TAX_FREE_MIN_YEN

        return receipt.copy(
            currency = cur,
            originalSubtotal = origSub,
            originalTotalTax = origTax,
            originalTotalWithTax = origTotal,
            subtotalWithoutTaxYen = subtotalYen,
            totalTaxYen = totalTaxYen,
            totalWithTaxYen = totalWithTaxYen,
            items = updatedItems,
            isTaxFreeEligible = isEligible,
            taxFreeStatus = if (isEligible) TaxFreeClaimStatus.PENDING else TaxFreeClaimStatus.NOT_APPLICABLE,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    private fun detectStoreName(lines: List<String>): String {
        for (line in lines.take(7)) {
            val clean = line.replace("#", "").replace("*", "").trim()
            when {
                // Tiendas japonesas conocidas (Kanji / Kana / Romaji)
                clean.contains("ドン・キホーテ", ignoreCase = true) || clean.contains("Don Quijote", ignoreCase = true) -> return clean
                clean.contains("ビックカメラ", ignoreCase = true) || clean.contains("BIC CAMERA", ignoreCase = true) -> return clean
                clean.contains("ヨドバシ", ignoreCase = true) || clean.contains("YODOBASHI", ignoreCase = true) -> return clean
                clean.contains("ユニクロ", ignoreCase = true) || clean.contains("UNIQLO", ignoreCase = true) -> return clean
                clean.contains("無印良品", ignoreCase = true) || clean.contains("MUJI", ignoreCase = true) -> return clean
                clean.contains("マツモトキヨシ", ignoreCase = true) || clean.contains("Matsumoto", ignoreCase = true) -> return clean
                clean.contains("セブン-イレブン", ignoreCase = true) || clean.contains("7-Eleven", ignoreCase = true) -> return clean
                clean.contains("ファミリーマート", ignoreCase = true) || clean.contains("FamilyMart", ignoreCase = true) -> return clean
                clean.contains("ローソン", ignoreCase = true) || clean.contains("LAWSON", ignoreCase = true) -> return clean
                clean.contains("Apple", ignoreCase = true) -> return clean
                clean.contains("Starbucks", ignoreCase = true) -> return clean
                clean.contains("Duty Free", ignoreCase = true) || clean.contains("Tax Free Shop", ignoreCase = true) -> return clean
            }
        }
        // Fallback: primera línea con longitud adecuada que no sea fecha, teléfono o web
        return lines.firstOrNull { l ->
            l.length in 3..40 &&
                !l.contains("TEL", ignoreCase = true) &&
                !l.contains("http", ignoreCase = true) &&
                !l.contains("www.", ignoreCase = true) &&
                !l.matches(Regex("""[0-9/.\- :]+"""))
        } ?: (lines.firstOrNull()?.take(30) ?: "Store")
    }

    private fun detectLocation(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            text.contains("道頓堀") || text.contains("なんば") || text.contains("難波") ||
                text.contains("梅田") || text.contains("大阪") || lower.contains("osaka") ||
                lower.contains("dotonbori") || lower.contains("namba") || lower.contains("umeda") -> "Osaka"

            text.contains("京都") || text.contains("祇園") || text.contains("清水") ||
                text.contains("河原町") || lower.contains("kyoto") || lower.contains("gion") ||
                lower.contains("kiyomizu") -> "Kyoto"

            text.contains("奈良") || lower.contains("nara") -> "Nara"
            text.contains("広島") || lower.contains("hiroshima") -> "Hiroshima"
            text.contains("名古屋") || lower.contains("nagoya") -> "Nagoya"
            text.contains("福岡") || text.contains("博多") || lower.contains("fukuoka") || lower.contains("hakata") -> "Fukuoka"
            text.contains("札幌") || lower.contains("sapporo") -> "Sapporo"
            text.contains("箱根") || lower.contains("hakone") -> "Hakone"
            text.contains("横浜") || lower.contains("yokohama") -> "Yokohama"
            else -> "Tokyo"
        }
    }

    private fun detectCategory(storeName: String, text: String): ExpenseCategory {
        val lower = (storeName + " " + text).lowercase(Locale.ROOT)
        return when {
            // Comida / Bebida (Japonés, Inglés, Español)
            lower.contains("セブン-イレブン") || lower.contains("7-eleven") ||
                lower.contains("familymart") || lower.contains("ファミリーマート") ||
                lower.contains("lawson") || lower.contains("ローソン") ||
                lower.contains("おにぎり") || lower.contains("ラーメン") ||
                lower.contains("寿司") || lower.contains("カフェ") ||
                lower.contains("珈琲") || lower.contains("レストラン") ||
                lower.contains("居酒屋") || lower.contains("定食") ||
                lower.contains("restaurant") || lower.contains("cafe") ||
                lower.contains("dining") || lower.contains("coffee") ||
                lower.contains("breakfast") || lower.contains("lunch") ||
                lower.contains("dinner") || lower.contains("bakery") ||
                lower.contains("supermarket") || lower.contains("grocery") ||
                lower.contains("food") || lower.contains("comida") ||
                lower.contains("restaurante") || lower.contains("café") ||
                lower.contains("bar") || lower.contains("almuerzo") ||
                lower.contains("cena") || lower.contains("desayuno") ||
                lower.contains("tapas") || lower.contains("alimentacion") -> ExpenseCategory.FOOD

            // Transporte (Japonés, Inglés, Español)
            lower.contains("jr") || lower.contains("地下鉄") || lower.contains("メトロ") ||
                lower.contains("切符") || lower.contains("乗車券") || lower.contains("新幹線") ||
                lower.contains("subway") || lower.contains("metro") || lower.contains("train") ||
                lower.contains("flight") || lower.contains("ticket") || lower.contains("bus") ||
                lower.contains("taxi") || lower.contains("shinkansen") || lower.contains("railway") ||
                lower.contains("transporte") || lower.contains("tren") || lower.contains("vuelo") ||
                lower.contains("autobús") || lower.contains("pasaje") -> ExpenseCategory.TRANSPORT

            // Ocio y Entradas (Japonés, Inglés, Español)
            lower.contains("入場券") || lower.contains("拝観料") || lower.contains("チケット") ||
                lower.contains("museum") || lower.contains("寺") || lower.contains("神社") ||
                lower.contains("temple") || lower.contains("shrine") || lower.contains("castle") ||
                lower.contains("admission") || lower.contains("tour") || lower.contains("disney") ||
                lower.contains("universal") || lower.contains("museo") || lower.contains("templo") ||
                lower.contains("santuario") || lower.contains("castillo") || lower.contains("entrada") ||
                lower.contains("excursión") || lower.contains("parque") -> ExpenseCategory.LEISURE

            // Alojamiento (Japonés, Inglés, Español)
            lower.contains("ホテル") || lower.contains("旅館") || lower.contains("hotel") ||
                lower.contains("ryokan") || lower.contains("hostel") || lower.contains("inn") ||
                lower.contains("resort") || lower.contains("accommodation") || lower.contains("airbnb") ||
                lower.contains("alojamiento") || lower.contains("apartamento") || lower.contains("pensión") ||
                lower.contains("reserva") -> ExpenseCategory.ACCOMMODATION

            // Por defecto: Compras
            else -> ExpenseCategory.SHOPPING
        }
    }

    private fun detectPurchaseDateIso(text: String): String {
        try {
            // 1. Formato ISO o japonés: YYYY/MM/DD, YYYY-MM-DD o YYYY.MM.DD
            val regexG = Regex("""(\d{4})[/-](\d{1,2})[/-](\d{1,2})""")
            val matchG = regexG.find(text)
            if (matchG != null) {
                val y = matchG.groupValues[1].toInt()
                val m = matchG.groupValues[2].toInt()
                val d = matchG.groupValues[3].toInt()
                val localDate = LocalDate.of(y, m, d)
                val localTime = extractTime(text)
                return ZonedDateTime.of(localDate, localTime, JST_ZONE).toInstant().toString()
            }

            // 2. Formato Europeo / Español: DD/MM/YYYY o DD-MM-YYYY
            val regexEU = Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{4})""")
            val matchEU = regexEU.find(text)
            if (matchEU != null) {
                val d = matchEU.groupValues[1].toInt()
                val m = matchEU.groupValues[2].toInt()
                val y = matchEU.groupValues[3].toInt()
                if (m in 1..12 && d in 1..31) {
                    val localDate = LocalDate.of(y, m, d)
                    val localTime = extractTime(text)
                    return ZonedDateTime.of(localDate, localTime, JST_ZONE).toInstant().toString()
                }
            }

            // 3. Era Reiwa japonesa: R06.03.15, R7/03/15 o 令和7年3月15日
            val regexR = Regex("""R(\d{1,2})[./-](\d{1,2})[./-](\d{1,2})""", RegexOption.IGNORE_CASE)
            val matchR = regexR.find(text)
            if (matchR != null) {
                val rYear = matchR.groupValues[1].toInt()
                val y = 2018 + rYear
                val m = matchR.groupValues[2].toInt()
                val d = matchR.groupValues[3].toInt()
                val localTime = extractTime(text)
                return ZonedDateTime.of(LocalDate.of(y, m, d), localTime, JST_ZONE).toInstant().toString()
            }

            val regexReiwaKanji = Regex("""令和(\d{1,2})年(\d{1,2})月(\d{1,2})日""")
            val matchRK = regexReiwaKanji.find(text)
            if (matchRK != null) {
                val rYear = matchRK.groupValues[1].toInt()
                val y = 2018 + rYear
                val m = matchRK.groupValues[2].toInt()
                val d = matchRK.groupValues[3].toInt()
                val localTime = extractTime(text)
                return ZonedDateTime.of(LocalDate.of(y, m, d), localTime, JST_ZONE).toInstant().toString()
            }
        } catch (_: Exception) {
            // fallback
        }
        return Instant.now().toString()
    }

    private fun extractTime(text: String): LocalTime {
        val regexT = Regex("""(\d{1,2}):(\d{2})""")
        val matchT = regexT.find(text)
        return if (matchT != null) {
            val hh = matchT.groupValues[1].toInt().coerceIn(0, 23)
            val mm = matchT.groupValues[2].toInt().coerceIn(0, 59)
            LocalTime.of(hh, mm)
        } else {
            LocalTime.of(12, 0)
        }
    }

    private fun extractAmount(text: String, keywords: List<String>): Double? {
        for (kw in keywords) {
            val kwRegex = if (kw.firstOrNull()?.isLetter() == true) """\b$kw\b""" else Regex.escape(kw)
            val pattern = Regex(
                """(?:$kwRegex)\s*[:：]?\s*[¥￥$€]?\s*([0-9,.]+(?:\s*[¥￥$€]|(?:\s*USD|\s*EUR))?)""",
                RegexOption.IGNORE_CASE
            )
            val match = pattern.find(text)
            if (match != null) {
                val numStr = match.groupValues[1]
                val parsed = parsePriceString(numStr)
                if (parsed != null && parsed > 0) return parsed
            }
        }
        return null
    }

    private fun extractTotalTax(text: String): Double? {
        var accumulatedTax = 0.0
        var found = false

        // Buscar múltiples líneas de impuesto (Japonés, Inglés y Español)
        val taxPattern = Regex(
            """(?:\b(?:CONSUMPTION TAX|SALES TAX|TOTAL TAX|TAX AMOUNT|TAX|VAT|CUOTA IVA|TOTAL IVA|IVA|IMPUESTOS?|TASAS?)\b|内消費税(?:等)?|消費税等?|外税|税額)\s*(?:\([^)]*\)|8%|10%)?\s*[:：]?\s*[¥￥$€]?\s*([0-9,.]+(?:\s*[¥￥$€]|(?:\s*USD|\s*EUR))?)""",
            RegexOption.IGNORE_CASE
        )
        val matches = taxPattern.findAll(text)
        for (m in matches) {
            val numStr = m.groupValues[1]
            val parsed = parsePriceString(numStr)
            if (parsed != null && parsed > 0) {
                accumulatedTax += parsed
                found = true
            }
        }

        return if (found) accumulatedTax else null
    }

    private fun extractProductItems(
        lines: List<String>,
        currency: String,
        rateToJpy: Double
    ): List<ReceiptProductItem> {
        val items = mutableListOf<ReceiptProductItem>()
        val isJpy = currency == "JPY"
        val skipKeywords = listOf(
            // Japonés
            "小計", "合計", "消費税", "税", "免税", "お釣", "預り", "TEL", "レシート", "領収書", "対象額",
            // Inglés
            "SUBTOTAL", "SUB TOTAL", "TOTAL", "TAX", "VAT", "CHANGE", "CASH", "CREDIT", "THANK YOU", "RECEIPT", "BALANCE",
            // Español
            "BASE IMPONIBLE", "BASE IMP", "IVA", "IMPUESTO", "TOTAL", "SUBTOTAL", "CAMBIO", "EFECTIVO", "TARJETA", "GRACIAS", "FACTURA", "TICKET"
        )

        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT)
            if (skipKeywords.any { upper.startsWith(it) || upper.contains("TOTAL") || line.contains("合計") || line.contains("小計") }) {
                continue
            }

            // Buscar si la línea termina con un número de precio
            val priceMatch = Regex("""\s+[¥￥$€]?\s*([0-9,.]+(?:\.[0-9]{1,2}|,\d{1,2})?)\s*(?:[¥￥$€]|USD|EUR|円)?\s*$""").find(line)
            if (priceMatch != null) {
                val priceStr = priceMatch.groupValues[1]
                val price = parsePriceString(priceStr)
                if (price != null && price > 0) {
                    val rawName = line.substring(0, priceMatch.range.first).trim()
                    if (rawName.isNotBlank() && !skipKeywords.any { rawName.uppercase(Locale.ROOT).contains(it) }) {
                        val isReduced = line.contains("8%") || line.contains("*") || line.contains("軽") || line.contains("※")
                        val taxRate = if (isReduced) 8.0 else 10.0
                        val unitYen = if (isJpy) price else Math.round(price * rateToJpy).toDouble()

                        items.add(
                            ReceiptProductItem(
                                id = UUID.randomUUID().toString(),
                                name = rawName,
                                quantity = 1,
                                unitPriceYen = unitYen,
                                taxRatePercent = taxRate,
                                currency = currency,
                                originalUnitPrice = price
                            )
                        )
                    }
                }
            }
        }

        return items
    }
}
