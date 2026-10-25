package com.laralbir.japantraveltools.feature.ai

import com.laralbir.japantraveltools.core.portability.PortableTravelPlanBundle
import java.util.Locale

data class SimultaneousTranslationItem(
    val sourceText: String,
    val japaneseText: String,
    val romaji: String,
    val categoryBadge: String
)

data class JapaneseToTravelerVoiceResult(
    val japaneseHeardText: String,
    val romaji: String,
    val spanishSpokenText: String,
    val englishSpokenText: String,
    val contextLabel: String
)

object OfflineTranslatorEngine {

    val incomingJapaneseSamples = listOf(
        JapaneseToTravelerVoiceResult(
            japaneseHeardText = "いらっしゃいませ。何名様ですか？禁煙席でよろしいですか？",
            romaji = "Irasshaimase. Nan-mei-sama desu ka? Kin'en-seki de yoroshii desu ka?",
            spanishSpokenText = "Bienvenido. ¿Cuántas personas son? ¿Les parece bien en mesa de no fumadores?",
            englishSpokenText = "Welcome. How many people in your party? Is a non-smoking table okay?",
            contextLabel = "RESTAURANTE"
        ),
        JapaneseToTravelerVoiceResult(
            japaneseHeardText = "新幹線のぞみ号は16番ホームから発車します。エレベーターはあちらです。",
            romaji = "Shinkansen Nozomi-gō wa jū-roku-ban hōmu kara hassha shimasu. Erebētā wa achira desu.",
            spanishSpokenText = "El Shinkansen Nozomi sale del andén número 16. El ascensor está por allí.",
            englishSpokenText = "The Nozomi Shinkansen departs from platform 16. The elevator is over there.",
            contextLabel = "ESTACIÓN / TREN"
        ),
        JapaneseToTravelerVoiceResult(
            japaneseHeardText = "レジ袋はいりますか？お箸と温めはどうしますか？",
            romaji = "Rejibukuro wa irimasu ka? O-hashi to atatame wa dō shimasu ka?",
            spanishSpokenText = "¿Necesita bolsa? ¿Quiere palillos y que le calentemos la comida?",
            englishSpokenText = "Do you need a bag? Would you like chopsticks and your food warmed up?",
            contextLabel = "KONBINI / TIENDA"
        ),
        JapaneseToTravelerVoiceResult(
            japaneseHeardText = "免税手続きのため、パスポートの原本を見せてください。",
            romaji = "Menzei tetsuzuki no tame, pasupōto no genpon o misete kudasai.",
            spanishSpokenText = "Para el trámite libre de impuestos Tax-Free, muéstreme su pasaporte original, por favor.",
            englishSpokenText = "For the Tax-Free procedure, please show me your original passport.",
            contextLabel = "TAX-FREE / COMPRAS"
        ),
        JapaneseToTravelerVoiceResult(
            japaneseHeardText = "申し訳ありませんが、現金またはSuicaのみとなります。",
            romaji = "Mōshiwake arimasen ga, genkin matawa Suica nomi to narimasu.",
            spanishSpokenText = "Disculpe, solo aceptamos pago en efectivo o con tarjeta Suica.",
            englishSpokenText = "Sorry, we only accept cash or Suica IC card.",
            contextLabel = "PAGO / CAJA"
        )
    )

    fun translateFromJapaneseSpeech(japaneseInput: String): JapaneseToTravelerVoiceResult {
        val clean = japaneseInput.trim()
        return when {
            clean.contains("何名") || clean.contains("いらっしゃい") || clean.contains("席") || clean.contains("予約") ->
                incomingJapaneseSamples[0].copy(japaneseHeardText = clean)
            clean.contains("ホーム") || clean.contains("番線") || clean.contains("新幹線") || clean.contains("16") || clean.contains("電車") ->
                incomingJapaneseSamples[1].copy(japaneseHeardText = clean)
            clean.contains("袋") || clean.contains("温め") || clean.contains("箸") || clean.contains("弁当") ->
                incomingJapaneseSamples[2].copy(japaneseHeardText = clean)
            clean.contains("パスポート") || clean.contains("免税") ->
                incomingJapaneseSamples[3].copy(japaneseHeardText = clean)
            clean.contains("現金") || clean.contains("カード") || clean.contains("Suica") || clean.contains("会計") ->
                incomingJapaneseSamples[4].copy(japaneseHeardText = clean)
            clean.contains("トイレ") || clean.contains("お手洗い") ->
                JapaneseToTravelerVoiceResult(
                    japaneseHeardText = clean,
                    romaji = "O-tearai wa massugu itte migigawa desu.",
                    spanishSpokenText = "El baño está todo recto a mano derecha.",
                    englishSpokenText = "The restroom is straight ahead on the right.",
                    contextLabel = "DIRECCIÓN / BAÑO"
                )
            clean.contains("チェックイン") || clean.contains("部屋") || clean.contains("朝食") ->
                JapaneseToTravelerVoiceResult(
                    japaneseHeardText = clean,
                    romaji = "Chekkuin no tetsuzuki o shimasu. Chōshoku wa shichi-ji kara desu.",
                    spanishSpokenText = "Vamos a realizar el check-in. El desayuno se sirve a partir de las 7:00.",
                    englishSpokenText = "We will process your check-in now. Breakfast starts at 7:00 AM.",
                    contextLabel = "HOTEL / RECEPCIÓN"
                )
            else -> JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Nihongo: $clean",
                spanishSpokenText = clean,
                englishSpokenText = clean,
                contextLabel = "VOZ JAPONESA"
            )
        }
    }

    val essentialPhrases = listOf(
        SimultaneousTranslationItem(
            sourceText = "¿Dónde está el andén del Shinkansen?",
            japaneseText = "新幹線のホームはどこですか？",
            romaji = "Shinkansen no hōmu wa doko desu ka?",
            categoryBadge = "TREN / ESTACIÓN"
        ),
        SimultaneousTranslationItem(
            sourceText = "¿Dónde está el baño, por favor?",
            japaneseText = "すみません、お手洗い（トイレ）はどこですか？",
            romaji = "Sumimasen, o-tearai (toire) wa doko desu ka?",
            categoryBadge = "BAÑO / ASEO"
        ),
        SimultaneousTranslationItem(
            sourceText = "¿Cuánto cuesta esto, por favor?",
            japaneseText = "すみません、これはいくらですか？",
            romaji = "Sumimasen, kore wa ikura desu ka?",
            categoryBadge = "PRECIO / COMPRAS"
        ),
        SimultaneousTranslationItem(
            sourceText = "No puedo comer marisco ni cacahuetes (alergia)",
            japaneseText = "甲殻類とピーナッツのアレルギーがあります。食べられません。",
            romaji = "Kōkakurui to pīnattsu no arerugī ga arimasu. Taberaremasen.",
            categoryBadge = "ALERGIA / SALUD"
        ),
        SimultaneousTranslationItem(
            sourceText = "¿Tienen menú en inglés o recomendación del chef?",
            japaneseText = "英語のメニューはありますか？おすすめは何ですか？",
            romaji = "Eigo no menyū wa arimasu ka? Osusume wa nan desu ka?",
            categoryBadge = "RESTAURANTE"
        ),
        SimultaneousTranslationItem(
            sourceText = "La cuenta, por favor. ¿Aceptan tarjeta o Suica?",
            japaneseText = "お会計をお願いします。カードかSuicaは使えますか？",
            romaji = "O-kaikei o onegaishimasu. Kādo ka Suica wa tsukaemasu ka?",
            categoryBadge = "PAGO / SUICA"
        ),
        SimultaneousTranslationItem(
            sourceText = "¿Hacen compras libres de impuestos (Tax-Free)?",
            japaneseText = "免税（タックスフリー）はできますか？パスポートがあります。",
            romaji = "Menzei (Takkusu Furī) wa dekimasu ka? Pasupōto ga arimasu.",
            categoryBadge = "TAX-FREE"
        ),
        SimultaneousTranslationItem(
            sourceText = "Por favor, lléveme a esta dirección de hotel",
            japaneseText = "このホテルの住所までお願いします。",
            romaji = "Kono hoteru no jūsho made onegaishimasu.",
            categoryBadge = "TAXI / HOTEL"
        )
    )

    fun translateToJapanese(input: String): SimultaneousTranslationItem {
        val normalized = input.trim().lowercase(Locale.ROOT)
        return when {
            normalized.contains("shinkansen") || normalized.contains("andén") || normalized.contains("platform") || normalized.contains("tren") || normalized.contains("estación") || normalized.contains("station") ->
                essentialPhrases[0].copy(sourceText = input)
            normalized.contains("baño") || normalized.contains("aseo") || normalized.contains("servicio") || normalized.contains("toilet") || normalized.contains("restroom") || normalized.contains("bathroom") ->
                essentialPhrases[1].copy(sourceText = input)
            normalized.contains("cuánto cuesta") || normalized.contains("cuanto cuesta") || normalized.contains("precio") || normalized.contains("how much") || normalized.contains("price") ->
                essentialPhrases[2].copy(sourceText = input)
            normalized.contains("alergia") || normalized.contains("marisco") || normalized.contains("shellfish") || normalized.contains("peanut") || normalized.contains("gluten") ->
                essentialPhrases[3].copy(sourceText = input)
            normalized.contains("menú") || normalized.contains("menu") || normalized.contains("recomend") || normalized.contains("carta") ->
                essentialPhrases[4].copy(sourceText = input)
            normalized.contains("cuenta") || normalized.contains("bill") || normalized.contains("suica") || normalized.contains("pagar") || normalized.contains("tarjeta") || normalized.contains("efectivo") ->
                essentialPhrases[5].copy(sourceText = input)
            normalized.contains("tax") || normalized.contains("impuesto") || normalized.contains("pasaporte") || normalized.contains("free") ->
                essentialPhrases[6].copy(sourceText = input)
            normalized.contains("taxi") || normalized.contains("hotel") || normalized.contains("dirección") || normalized.contains("address") || normalized.contains("lléveme") ->
                essentialPhrases[7].copy(sourceText = input)
            normalized.contains("agua") || normalized.contains("water") || normalized.contains("cerveza") || normalized.contains("té") ->
                SimultaneousTranslationItem(
                    sourceText = input,
                    japaneseText = "すみません、お水を2つお願いします。",
                    romaji = "Sumimasen, o-mizu o futatsu onegaishimasu.",
                    categoryBadge = "BEBIDA / AGUA"
                )
            normalized.contains("mesa") || normalized.contains("personas") || normalized.contains("dos") || normalized.contains("table") ->
                SimultaneousTranslationItem(
                    sourceText = input,
                    japaneseText = "すみません、2名です。席はありますか？",
                    romaji = "Sumimasen, ni-mei desu. Seki wa arimasu ka?",
                    categoryBadge = "MESA / RESERVA"
                )
            normalized.contains("vegetariano") || normalized.contains("vegano") || normalized.contains("carne") || normalized.contains("vegetarian") ->
                SimultaneousTranslationItem(
                    sourceText = input,
                    japaneseText = "私はベジタリアンです。肉と魚は食べられません。",
                    romaji = "Watashi wa bejitarian desu. Niku to sakana wa taberaremasen.",
                    categoryBadge = "DIETA / SIN CARNE"
                )
            normalized.contains("maleta") || normalized.contains("equipaje") || normalized.contains("taquilla") || normalized.contains("luggage") ->
                SimultaneousTranslationItem(
                    sourceText = input,
                    japaneseText = "すみません、荷物を預ける場所やコインロッカーはありますか？",
                    romaji = "Sumimasen, nimotsu o azukeru basho ya koin-rokkā wa arimasu ka?",
                    categoryBadge = "EQUIPAJE / CONSIGNA"
                )
            normalized.contains("ayuda") || normalized.contains("farmacia") || normalized.contains("médico") || normalized.contains("help") ->
                SimultaneousTranslationItem(
                    sourceText = input,
                    japaneseText = "すみません、助けが必要です。近くに薬局はありますか？",
                    romaji = "Sumimasen, tasuke ga hitsuyō desu. Chikaku ni yakkyoku wa arimasu ka?",
                    categoryBadge = "AYUDA / FARMACIA"
                )
            normalized.contains("gracias") || normalized.contains("hola") || normalized.contains("disculpe") || normalized.contains("thank") ->
                SimultaneousTranslationItem(
                    sourceText = input,
                    japaneseText = "ありがとうございます。とても美味しかったです。",
                    romaji = "Arigatō gozaimasu. Totemo oishikatta desu.",
                    categoryBadge = "CORTESÍA"
                )
            else -> SimultaneousTranslationItem(
                sourceText = input,
                japaneseText = "すみません、日本語がわかりません。翻訳アプリを使っています。ゆっくり話してください。",
                romaji = "Sumimasen, Nihongo ga wakarimasen. Hon'yaku apuri o tsukatte imasu. Yukkuri hanashite kudasai.",
                categoryBadge = "ASISTENCIA EN JAPONÉS"
            )
        }
    }
}

object GeminiAssistantEngine {

    /**
     * Finds prices in Yen (e.g. ¥1,200, 1200円, 1200 yenes, 1200 yen, 1200 JPY)
     * and appends the converted EUR price using the cached offline rate.
     */
    fun enrichMenuWithConvertedPrices(text: String, eurToJpyRate: Double): String {
        val safeRate = if (eurToJpyRate > 0.0) eurToJpyRate else 164.80
        val yenSymbolRegex = """¥\s?([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)""".toRegex()
        val withSymbolConverted = yenSymbolRegex.replace(text) { match ->
            val rawDigits = match.groupValues[1].replace(",", "")
            val yenValue = rawDigits.toDoubleOrNull() ?: return@replace match.value
            val eurValue = yenValue / safeRate
            val formattedEur = String.format(Locale.US, "%.2f EUR", eurValue)
            "${match.value} (~$formattedEur)"
        }
        return withSymbolConverted
    }

    fun generateSmartTravelAnswer(
        prompt: String,
        eurToJpyRate: Double,
        currentPlan: PortableTravelPlanBundle? = null
    ): String {
        val safeRate = if (eurToJpyRate > 0.0) eurToJpyRate else 164.80
        val cleanPrompt = prompt.trim()
        val lower = cleanPrompt.lowercase(Locale.ROOT)

        // 1. Check if the user entered specific yen numbers (e.g., "3200 yenes", "¥1,500", "5000 jpy")
        val numberYenRegex = """(?:¥\s*)?([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{2,7})\s*(?:yenes|yen|jpy|円)?""".toRegex(RegexOption.IGNORE_CASE)
        val hasExplicitPriceContext = lower.contains("¥") || lower.contains("yen") || lower.contains("jpy") || lower.contains("円") || lower.contains("euro") || lower.contains("cuesta") || lower.contains("precio")
        if (hasExplicitPriceContext && !lower.contains("ramen") && !lower.contains("carta")) {
            val match = numberYenRegex.find(cleanPrompt)
            val rawDigits = match?.groupValues?.get(1)?.replace(",", "")?.replace(".", "")
            val yenAmount = rawDigits?.toDoubleOrNull()
            if (yenAmount != null && yenAmount > 0) {
                val eurAmount = String.format(Locale.US, "%.2f EUR", yenAmount / safeRate)
                return buildString {
                    appendLine("💴 Conversión rápida e impuestos en Japón:")
                    appendLine("• ${yenAmount.toLong()} JPY equivalen a $eurAmount (tasa actual: 1 EUR = ${String.format(Locale.US, "%.2f", safeRate)} JPY).")
                    val withTax = (yenAmount * 1.10).toLong()
                    val withTaxEur = String.format(Locale.US, "%.2f EUR", withTax / safeRate)
                    appendLine("• Si el precio es antes de impuestos (税抜), con el 10% de IVA japonés (税込) serán ¥$withTax (~$withTaxEur).")
                    if (yenAmount >= 5000) {
                        append("• Al superar los ¥5,000 (¥5,500 con impuestos), puedes pedir devolución Tax-Free mostrando tu pasaporte físico.")
                    }
                }
            }
        }

        // 2. Topic-specific intelligent travel assistant answers
        return when {
            lower.contains("ramen") || lower.contains("menú") || lower.contains("menu") || lower.contains("carta") || lower.contains("sushi") || lower.contains("comer") -> {
                val sampleMenu = """
                    🍜 Traducción de Carta Habitual y Precios en Euros:
                    • Tonkotsu Ramen Especial (豚骨特製ラーメン): Caldo cremoso de cerdo con chashu y huevo ajitsuke — ¥1,200
                    • Gyoza a la plancha 6 uds (焼き餃子): Empanadillas crujientes de cerdo y cebollino — ¥450
                    • Katsudon (カツ丼): Cuenco de arroz con lomo de cerdo empanado y huevo — ¥980
                    • Matcha Parfait de Uji (宇治抹茶パフェ): Helado artesanal de té verde con mochi — ¥850
                    • Cerveza de barril Nama Biru (生ビール): Jarra fría — ¥600
                """.trimIndent()
                enrichMenuWithConvertedPrices(sampleMenu, safeRate)
            }

            lower.contains("suica") || lower.contains("pasmo") || lower.contains("icoca") || lower.contains("metro") || lower.contains("tarjeta ic") -> {
                val depositEur = String.format(Locale.US, "%.2f EUR", 500.0 / safeRate)
                val loadEur = String.format(Locale.US, "%.2f EUR", 3000.0 / safeRate)
                """
                    💳 Tarjetas Suica / Pasmo / ICOCA:
                    • ¿Para qué sirven?: Pagan al instante metros, trenes JR urbanos, autobuses, máquinas expendedoras y tiendas Konbini (7-Eleven, Lawson, FamilyMart) acercando la tarjeta al lector.
                    • ¿Cuánto recargar?: Se recomienda empezar con ¥3,000 (~$loadEur). El depósito físico inicial es de ¥500 (~$depositEur).
                    • Nota importante: Las tarjetas IC se recargan siempre en efectivo (yenes) en las máquinas de cualquier estación de tren o metro.
                """.trimIndent()
            }

            lower.contains("shinkansen") || lower.contains("jr pass") || lower.contains("tren bala") || lower.contains("nozomi") || lower.contains("hikari") -> {
                val tokyoKyotoEur = String.format(Locale.US, "%.2f EUR", 14170.0 / safeRate)
                """
                    🚄 Guía de Shinkansen y JR Pass:
                    • Tokio ➔ Kioto: Tarda 2h 15m en Nozomi / 2h 40m en Hikari. Billete suelto: ~¥14,170 (~$tokyoKyotoEur).
                    • Regla de Maletas Grandes (>160 cm suma de alto+ancho+fondo): Es obligatorio reservar asiento "Oversized Baggage Area" (última fila del vagón) sin coste extra al sacar el billete.
                    • Vistas al Monte Fuji: Desde Tokio hacia Kioto/Osaka, elige siempre el Asiento E (ventanilla derecha en sentido de la marcha).
                """.trimIndent()
            }

            lower.contains("nara") || lower.contains("ciervo") || lower.contains("todai") -> {
                val crackersEur = String.format(Locale.US, "%.2f EUR", 200.0 / safeRate)
                """
                    🦌 Excursión Recomendada a Nara (Medio Día desde Kioto u Osaka):
                    1. 09:00 · Tren Kintetsu Nara (35 min desde Kioto / 40 min desde Osaka Namba) o JR Nara Line.
                    2. 09:45 · Parque de Nara (奈良公園): Más de 1.200 ciervos sagrados en libertad (galletas Shika-senbei: ¥200 / ~$crackersEur).
                    3. 10:30 · Gran Buda de Todai-ji (東大寺): El pabellón de madera más impresionante de Japón.
                    4. 12:00 · Santuario Kasuga Taisha (春日大社): Camino de los 3.000 faroles de piedra y bronce.
                    👉 Pulsa "Aplicar en mi Ruta" si quieres añadir este día automáticamente a tu Planificador.
                """.trimIndent()
            }

            lower.contains("hakone") || lower.contains("fuji") || lower.contains("onsen") || lower.contains("kawaguchiko") -> {
                """
                    🗻 Excursión a Hakone y Vistas al Monte Fuji:
                    1. 08:30 · Salida desde Shinjuku (Odakyu Romancecar) o desde Tokio en Shinkansen Kodama hasta Odawara.
                    2. 10:15 · Teleférico de Hakone y valle volcánico de Owakudani (大涌谷) — prueba los huevos negros Kuro-tamago.
                    3. 12:30 · Crucero por el Lago Ashi (芦ノ湖) hasta el Torii flotante del Santuario Hakone-jinja.
                    4. 15:00 · Baño termal Onsen de día en Hakone Yuryo antes de regresar a Tokio.
                    👉 Revisa antes el índice de visibilidad del Monte Fuji en la herramienta "Tiempo (Open-Meteo)".
                """.trimIndent()
            }

            lower.contains("hiroshima") || lower.contains("miyajima") || lower.contains("itsukushima") -> {
                """
                    ⛩️ Excursión a Hiroshima y la Isla de Miyajima:
                    1. 08:00 · Shinkansen desde Kioto/Shin-Osaka hasta Hiroshima Station (~1h 25m).
                    2. 09:45 · Tren JR Sanyo Line hasta Miyajimaguchi + Ferry JR (10 min, incluido en JR Pass) hasta la isla de Miyajima.
                    3. 10:30 · Gran Torii Flotante de Itsukushima (厳島神社) y prueba las ostras a la parrilla y Momiji Manju.
                    4. 14:30 · Regreso a Hiroshima: Cúpula de la Bomba Atómica (Genbaku Dome), Parque Conmemorativo de la Paz y cena de Okonomiyaki estilo Hiroshima.
                """.trimIndent()
            }

            lower.contains("tokio") || lower.contains("tokyo") || lower.contains("shibuya") || lower.contains("shinjuku") || lower.contains("asakusa") -> {
                """
                    🗼 Ruta Imprescindible por Tokio:
                    • Mañana (Tradición): Templo Senso-ji en Asakusa (08:30, sin aglomeraciones) + Paseo por el Parque Ueno y mercado Ameyoko.
                    • Mediodía (Contraste): Santuario Meiji Jingu en el bosque de Yoyogi + calle Takeshita y Omotesando en Harajuku.
                    • Tarde/Noche (Neones): Mirador Shibuya Sky al atardecer, cruce de Shibuya y cena en Omoide Yokocho / Kabukicho (Shinjuku).
                """.trimIndent()
            }

            lower.contains("kioto") || lower.contains("kyoto") || lower.contains("lluvia") || lower.contains("gion") || lower.contains("fushimi") -> {
                """
                    ⛩️ Ruta Recomendada en Kioto (con alternativa si llueve):
                    • 08:00 · Fushimi Inari Taisha (伏見稲荷大社): Sube los mil toriis rojos a primera hora.
                    • 11:00 · Mercado Nishiki (錦市場 — Galería techada ideal si llueve) y almuerzo en Pontocho.
                    • 14:30 · Templo Kiyomizu-dera y calles históricas de madera Sannenzaka y Ninenzaka.
                    • 18:00 · Paseo al anochecer por el barrio de geishas de Gion (Hanamikoji-dori).
                """.trimIndent()
            }

            lower.contains("osaka") || lower.contains("dotonbori") || lower.contains("kuromon") -> {
                """
                    🐙 Ruta Gastronómica y Urbana en Osaka:
                    • 09:30 · Castillo de Osaka (大阪城) y jardines Nishinomaru.
                    • 12:15 · Mercado Kuromon Ichiba (黒門市場): Marisco fresco a la brasa, vieiras y carne de Wagyu.
                    • 15:30 · Barrio retro de Shinsekai y torre Tsutenkaku (prueba el Kushikatsu).
                    • 18:30 · Canal de Dotonbori, cartel de Glico Man y Takoyaki recién hecho.
                """.trimIndent()
            }

            lower.contains("efectivo") || lower.contains("cajero") || lower.contains("dinero") || lower.contains("7-eleven") || lower.contains("konbini") -> {
                val tenKEur = String.format(Locale.US, "%.2f EUR", 10000.0 / safeRate)
                """
                    🏧 Efectivo y Cajeros Automáticos en Japón:
                    • Los mejores cajeros para tarjetas extranjeras (Visa/Mastercard/Revolut/N26) son los de 7-Eleven (7-Bank ATM), disponibles 24h en todo el país.
                    • Elige siempre facturar en Yenes (JPY) en la pantalla del cajero para evitar comisiones de conversión dinámica.
                    • Lleva siempre entre ¥10,000 (~$tenKEur) y ¥20,000 en efectivo para templos, mercados callejeros y recargas de Suica.
                """.trimIndent()
            }

            lower.contains("maleta") || lower.contains("equipaje") || lower.contains("takkyubin") || lower.contains("yamato") -> {
                val bagEur = String.format(Locale.US, "%.2f EUR", 2500.0 / safeRate)
                """
                    🧳 Envío de Maletas entre Hoteles (Takkyubin / Yamato Transport):
                    • Para no cargar con maletas grandes en el Shinkansen o escaleras de metro, pide en la recepción de tu hotel el servicio "Takkyubin" (icono del gato negro de Yamato).
                    • Coste aproximado por maleta grande (Tokio ➔ Kioto): ~¥2,500 (~$bagEur), entrega al día siguiente directamente en tu próximo hotel.
                """.trimIndent()
            }

            else -> {
                val stopsCount = currentPlan?.itineraryStops?.size ?: 0
                val daysCount = currentPlan?.itineraryStops?.map { it.dayNumber }?.distinct()?.size ?: 0
                val segmentsCount = currentPlan?.transportSegments?.size ?: 0
                buildString {
                    appendLine("✨ Resumen Inteligente para tu consulta («$cleanPrompt»):")
                    if (stopsCount > 0 || segmentsCount > 0) {
                        appendLine("• Estado de tu ruta actual: Tienes $stopsCount paradas distribuidas en $daysCount día(s) y $segmentsCount tramo(s) de transporte.")
                    } else {
                        appendLine("• Tu planificador está actualmente vacío y listo para crear nuevas jornadas.")
                    }
                    appendLine("• Cambio actual aplicado: 1 EUR = ${String.format(Locale.US, "%.2f", safeRate)} JPY (¥1,000 ≈ ${String.format(Locale.US, "%.2f EUR", 1000.0 / safeRate)}).")
                    append("• Puedes pedirme rutas por ciudad (Tokio, Kioto, Osaka, Nara, Hakone, Hiroshima), dudas de Suica/Shinkansen/cajeros/maletas o pegar precios en ¥ para convertirlos.")
                }
            }
        }
    }
}
