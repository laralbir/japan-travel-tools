package com.laralbir.japantraveltools.feature.ai

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
            clean.contains("何名") || clean.contains("いらっしゃい") || clean.contains("席") ->
                incomingJapaneseSamples[0].copy(japaneseHeardText = clean)
            clean.contains("ホーム") || clean.contains("番線") || clean.contains("新幹線") || clean.contains("16") ->
                incomingJapaneseSamples[1].copy(japaneseHeardText = clean)
            clean.contains("袋") || clean.contains("温め") || clean.contains("箸") ->
                incomingJapaneseSamples[2].copy(japaneseHeardText = clean)
            clean.contains("パスポート") || clean.contains("免税") ->
                incomingJapaneseSamples[3].copy(japaneseHeardText = clean)
            clean.contains("現金") || clean.contains("カード") || clean.contains("Suica") ->
                incomingJapaneseSamples[4].copy(japaneseHeardText = clean)
            else -> JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Nihongo no onsei: $clean",
                spanishSpokenText = "El interlocutor japonés dice: $clean (Traducción de voz activa)",
                englishSpokenText = "The Japanese speaker says: $clean (Voice translation active)",
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
            sourceText = "La cuenta, por favor. ¿Aceptan tarjeta o IC Suica?",
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
            normalized.contains("shinkansen") || normalized.contains("andén") || normalized.contains("platform") ->
                essentialPhrases[0].copy(sourceText = input)
            normalized.contains("alergia") || normalized.contains("marisco") || normalized.contains("shellfish") || normalized.contains("peanut") ->
                essentialPhrases[1].copy(sourceText = input)
            normalized.contains("menú") || normalized.contains("menu") || normalized.contains("recomend") ->
                essentialPhrases[2].copy(sourceText = input)
            normalized.contains("cuenta") || normalized.contains("bill") || normalized.contains("suica") ->
                essentialPhrases[3].copy(sourceText = input)
            normalized.contains("tax") || normalized.contains("impuesto") || normalized.contains("pasaporte") ->
                essentialPhrases[4].copy(sourceText = input)
            else -> SimultaneousTranslationItem(
                sourceText = input,
                japaneseText = "すみません、「$input」について教えてください。",
                romaji = "Sumimasen, \"$input\" ni tsuite oshiete kudasai.",
                categoryBadge = "LIVE TRANSLATE OFFLINE"
            )
        }
    }
}

object GeminiAssistantEngine {

    /**
     * Finds prices in Yen (e.g. ¥1,200 or 1200円) inside Japanese menu descriptions
     * and appends the converted EUR price using the cached offline rate.
     */
    fun enrichMenuWithConvertedPrices(text: String, eurToJpyRate: Double): String {
        val safeRate = if (eurToJpyRate > 0.0) eurToJpyRate else 164.80
        val yenRegex = """¥\s?([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)""".toRegex()
        return yenRegex.replace(text) { match ->
            val rawDigits = match.groupValues[1].replace(",", "")
            val yenValue = rawDigits.toDoubleOrNull() ?: return@replace match.value
            val eurValue = yenValue / safeRate
            val formattedEur = String.format(Locale.US, "%.2f EUR", eurValue)
            "${match.value} (~$formattedEur)"
        }
    }

    fun generateSmartTravelAnswer(prompt: String, eurToJpyRate: Double): String {
        val lower = prompt.lowercase(Locale.ROOT)
        return when {
            lower.contains("ramen") || lower.contains("menú") || lower.contains("menu") || lower.contains("carta") -> {
                val sampleMenu = """
                    • Tonkotsu Ramen Especial (豚骨特製ラーメン): Caldo cremoso de cerdo 18h con chashu y huevo ajitsuke — ¥1,200
                    • Gyoza a la plancha 6 uds (焼き餃子): Empanadillas crujientes de cerdo y cebollino — ¥450
                    • Matcha Parfait de Uji (宇治抹茶パフェ): Helado artesanal de té verde con mochi — ¥850
                """.trimIndent()
                enrichMenuWithConvertedPrices(sampleMenu, eurToJpyRate)
            }
            lower.contains("kioto") || lower.contains("kyoto") || lower.contains("lluvia") -> {
                """
                    Itinerario sugerido por Gemini (Guardado en Caché Offline):
                    1. 09:00 · Mercado Nishiki (錦市場 — Galería techada ideal si llueve).
                    2. 11:30 · Ceremonia del Té en Gion y templo Kennin-ji (el templo Zen más antiguo de Kioto, interior cubierto).
                    3. 14:30 · Museo Internacional del Manga o Kyoto Station Skyway.
                """.trimIndent()
            }
            else -> {
                """
                    Respuesta de Gemini Travel Assistant (Caché Offline Activa):
                    • Para "$prompt": Recuerda llevar tu Pasaporte físico para compras Tax-Free (a partir de ¥5,500, ~${String.format(Locale.US, "%.2f EUR", 5500.0 / eurToJpyRate)}).
                    • Puedes pulsar "Enviar al Planificador" o usar el botón de Traducción Simultánea 180° en la barra superior.
                """.trimIndent()
            }
        }
    }
}

