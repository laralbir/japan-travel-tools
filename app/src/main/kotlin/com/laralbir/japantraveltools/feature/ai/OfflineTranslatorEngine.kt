package com.laralbir.japantraveltools.feature.ai

import java.util.Locale

data class SimultaneousTranslationItem(
    val sourceText: String,
    val japaneseText: String,
    val romaji: String,
    val categoryBadge: String
)

data class TravelerQuickPhrase(
    val sourceEs: String,
    val sourceEn: String,
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

data class CameraOcrLine(
    val japaneseText: String,
    val romaji: String,
    val translatedText: String
)

data class CameraOcrResult(
    val rawText: String,
    val lines: List<CameraOcrLine>,
    val fullTranslation: String,
    val hasJapanese: Boolean
)

data class CameraSignSample(
    val label: String,
    val sampleJapaneseOcr: String
)

object OfflineTranslatorEngine {

    fun isJapaneseText(text: String): Boolean {
        return text.any { char ->
            val code = char.code
            (code in 0x3040..0x309F) || // Hiragana
            (code in 0x30A0..0x30FF) || // Katakana
            (code in 0x4E00..0x9FFF) || // CJK Unified Ideographs (Kanji)
            (code in 0x31F0..0x31FF) || // Katakana Phonetic Extensions
            (code in 0xFF65..0xFF9F)    // Halfwidth Katakana
        }
    }

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

    val sampleJapaneseSigns = listOf(
        CameraSignSample(
            label = "🍜 Menú Ramen",
            sampleJapaneseOcr = "特製ラーメン 1000円\n焼き餃子 450円\n生ビール 550円"
        ),
        CameraSignSample(
            label = "🚅 Shinkansen",
            sampleJapaneseOcr = "新幹線 16番線\n自由席 1号車〜3号車"
        ),
        CameraSignSample(
            label = "🚭 Cartel Local",
            sampleJapaneseOcr = "禁煙席\n本日満席\nお会計 現金のみ"
        ),
        CameraSignSample(
            label = "🚻 Aseos y Salida",
            sampleJapaneseOcr = "お手洗い\n非常口 Exit"
        )
    )

    fun translateOcrJapaneseText(rawOcrText: String, isEs: Boolean = true): CameraOcrResult {
        val clean = rawOcrText.trim()
        val rawLines = clean.lines().map { it.trim() }.filter { it.isNotBlank() }
        val hasJp = rawLines.any { isJapaneseText(it) }

        if (!hasJp || rawLines.isEmpty()) {
            return CameraOcrResult(
                rawText = rawOcrText,
                lines = emptyList(),
                fullTranslation = if (isEs) {
                    "No se detectó texto en japonés en la imagen. Intenta enfocar más cerca del cartel o menú."
                } else {
                    "No clear Japanese text detected in image. Please focus closer to the sign or menu."
                },
                hasJapanese = false
            )
        }

        val lines = rawLines.map { line ->
            translateSingleOcrLine(line, isEs)
        }

        val summary = lines.joinToString(separator = "\n") { it.translatedText }
        return CameraOcrResult(
            rawText = rawOcrText,
            lines = lines,
            fullTranslation = summary,
            hasJapanese = true
        )
    }

    private fun translateSingleOcrLine(line: String, isEs: Boolean): CameraOcrLine {
        val yenRegex = """([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)\s*円""".toRegex()
        val yenMatch = yenRegex.find(line)
        val yenAmount = yenMatch?.groupValues?.get(1)

        val (translatedText, romaji) = when {
            line.contains("ラーメン") -> {
                val priceInfo = if (yenAmount != null) " (~$yenAmount yenes)" else ""
                val dish = if (line.contains("特製")) "Ramen especial" else "Ramen"
                Pair("$dish$priceInfo", "Tokusei rāmen")
            }
            line.contains("餃子") -> {
                val priceInfo = if (yenAmount != null) " (~$yenAmount yenes)" else ""
                Pair("Empanadillas Gyoza$priceInfo", "Yaki-gyōza")
            }
            line.contains("ビール") -> {
                val priceInfo = if (yenAmount != null) " (~$yenAmount yenes)" else ""
                Pair("Cerveza de barril$priceInfo", "Nama bīru")
            }
            line.contains("新幹線") -> {
                val track = """([0-9]+)\s*番""".toRegex().find(line)?.groupValues?.get(1)
                val trackText = if (track != null) " andén $track" else ""
                Pair("Tren Shinkansen$trackText", "Shinkansen")
            }
            line.contains("禁煙") -> Pair("Zona de no fumadores (prohibido fumar)", "Kin'en-seki")
            line.contains("喫煙") -> Pair("Área de fumadores", "Kitsuen-jo")
            line.contains("満席") || line.contains("本日満席") -> Pair("Local completo (sin mesas disponibles)", "Honjitsu manseki")
            line.contains("お手洗い") || line.contains("トイレ") || line.contains("化粧室") -> Pair("Aseos / Baño", "O-tearai / Toire")
            line.contains("非常口") || line.contains("避難所") -> Pair("Salida de emergencia", "Hijōguchi")
            line.contains("出口") -> Pair("Salida", "Deguchi")
            line.contains("現金のみ") || line.contains("現金だけ") -> Pair("Solo pago en efectivo", "Genkin nomi")
            line.contains("免税") -> Pair("Trámite Tax-Free libre de impuestos", "Menzei")
            line.contains("自由席") -> Pair("Vagones de asientos libres (no reservados)", "Jiyū-seki")
            line.contains("指定席") -> Pair("Vagones de asientos reservados", "Shitei-seki")
            line.contains("うどん") -> Pair("Fideos Udon" + (if (yenAmount != null) " (~$yenAmount yenes)" else ""), "Udon")
            line.contains("そば") -> Pair("Fideos Soba" + (if (yenAmount != null) " (~$yenAmount yenes)" else ""), "Soba")
            line.contains("寿司") || line.contains("すし") -> Pair("Sushi" + (if (yenAmount != null) " (~$yenAmount yenes)" else ""), "Sushi")
            line.contains("カレー") -> Pair("Curry japonés" + (if (yenAmount != null) " (~$yenAmount yenes)" else ""), "Karē")
            line.contains("水") -> Pair("Agua fresca", "O-mizu")
            line.contains("お茶") -> Pair("Té verde", "O-cha")
            yenAmount != null -> Pair("Importe: $yenAmount yenes", "$yenAmount-en")
            else -> {
                val speechResult = translateFromJapaneseSpeech(line)
                Pair(speechResult.spanishSpokenText, speechResult.romaji)
            }
        }

        return CameraOcrLine(
            japaneseText = line,
            romaji = romaji,
            translatedText = translatedText
        )
    }

    fun translateFromJapaneseSpeech(japaneseInput: String): JapaneseToTravelerVoiceResult {
        val clean = japaneseInput.trim()
        if (clean.isBlank()) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = "",
                romaji = "",
                spanishSpokenText = "Por favor, hable en japonés cuando pulse el micrófono.",
                englishSpokenText = "Please speak in Japanese when pressing the microphone.",
                contextLabel = "MICRÓFONO VACÍO"
            )
        }

        // 1. Preguntas directas de "¿Cuánto cuesta?" (いくらですか)
        if (clean.contains("いくら")) {
            val isKore = clean.contains("これ") || clean.contains("此れ")
            val esText = if (isKore) "¿Cuánto cuesta esto?" else "¿Cuánto cuesta?"
            val enText = if (isKore) "How much is this?" else "How much is it?"
            val romaji = if (isKore) "Kore wa ikura desu ka?" else "Ikura desu ka?"
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = romaji,
                spanishSpokenText = esText,
                englishSpokenText = enText,
                contextLabel = "PRECIO / PREGUNTA"
            )
        }

        // 2. Preguntas de "¿Dónde está...?" (どこですか)
        if (clean.contains("どこ")) {
            val (esSubject, enSubject, romajiSubject) = when {
                clean.contains("トイレ") || clean.contains("お手洗い") || clean.contains("化粧室") ->
                    Triple("¿Dónde está el baño?", "Where is the restroom?", "Toire wa doko desu ka?")
                clean.contains("新幹線") ->
                    Triple("¿Dónde está el andén del Shinkansen?", "Where is the Shinkansen platform?", "Shinkansen no hōmu wa doko desu ka?")
                clean.contains("駅") ->
                    Triple("¿Dónde está la estación?", "Where is the station?", "Eki wa doko desu ka?")
                clean.contains("ホーム") || clean.contains("番線") ->
                    Triple("¿Dónde está el andén?", "Where is the platform?", "Hōmu wa doko desu ka?")
                clean.contains("エレベーター") ->
                    Triple("¿Dónde está el ascensor?", "Where is the elevator?", "Erebētā wa doko desu ka?")
                clean.contains("出口") ->
                    Triple("¿Dónde está la salida?", "Where is the exit?", "Deguchi wa doko desu ka?")
                clean.contains("切符") ->
                    Triple("¿Dónde se compran los billetes?", "Where to buy tickets?", "Kippu wa doko desu ka?")
                clean.contains("ロッカー") ->
                    Triple("¿Dónde están las consignas?", "Where are the coin lockers?", "Rokkā wa doko desu ka?")
                else ->
                    Triple("¿Dónde está?", "Where is it?", "Doko desu ka?")
            }
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = romajiSubject,
                spanishSpokenText = esSubject,
                englishSpokenText = enSubject,
                contextLabel = "PREGUNTA DE UBICACIÓN"
            )
        }

        // 3. Precios y cantidades en Yenes (ej. 1500円, 2000 円, etc.)
        val yenRegex = """([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)\s*円""".toRegex()
        val yenMatch = yenRegex.find(clean)
        if (yenMatch != null) {
            val amount = yenMatch.groupValues[1]
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "$amount-en desu.",
                spanishSpokenText = "$amount yenes.",
                englishSpokenText = "$amount yen.",
                contextLabel = "PRECIO / YENES"
            )
        }

        // 4. Direcciones hacia baños / aseos (indicaciones o ubicaciones)
        if (clean.contains("トイレ") || clean.contains("お手洗い") || clean.contains("化粧室")) {
            val direction = when {
                clean.contains("右") -> "El baño está a mano derecha."
                clean.contains("左") -> "El baño está a mano izquierda."
                clean.contains("まっすぐ") || clean.contains("直進") -> "El baño está todo recto."
                clean.contains("あちら") || clean.contains("あそこ") || clean.contains("向こう") -> "El baño está por allí."
                clean.contains("2階") -> "El baño está en la segunda planta."
                clean.contains("地下") -> "El baño está en la planta sótano."
                else -> "Baño / Aseos."
            }
            val dirEn = when {
                clean.contains("右") -> "The restroom is on the right."
                clean.contains("左") -> "The restroom is on the left."
                clean.contains("まっすぐ") || clean.contains("直進") -> "The restroom is straight ahead."
                clean.contains("あちら") || clean.contains("あそこ") || clean.contains("向こう") -> "The restroom is over there."
                clean.contains("2階") -> "The restroom is on the second floor."
                clean.contains("地下") -> "The restroom is in the basement."
                else -> "Restroom / Toilet."
            }
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Toire wa achira desu.",
                spanishSpokenText = direction,
                englishSpokenText = dirEn,
                contextLabel = "DIRECCIÓN / BAÑO"
            )
        }

        // 5. Saludos, cortesía y agradecimientos
        if (clean.contains("ありがとう") || clean.contains("有難う") || clean.contains("どうもありがとう")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Arigatō gozaimasu.",
                spanishSpokenText = "Muchas gracias.",
                englishSpokenText = "Thank you very much.",
                contextLabel = "AGRADECIMIENTO"
            )
        }
        if (clean.contains("こんにちは") || clean.contains("こんにちわ")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Konnichiwa.",
                spanishSpokenText = "Hola, buenas tardes.",
                englishSpokenText = "Hello, good day.",
                contextLabel = "SALUDO"
            )
        }
        if (clean.contains("おはよう")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Ohayō gozaimasu.",
                spanishSpokenText = "Buenos días.",
                englishSpokenText = "Good morning.",
                contextLabel = "SALUDO"
            )
        }
        if (clean.contains("こんばんは")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Konbanwa.",
                spanishSpokenText = "Buenas noches.",
                englishSpokenText = "Good evening.",
                contextLabel = "SALUDO"
            )
        }
        if (clean.contains("さようなら") || clean.contains("またね") || clean.contains("じゃあね")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Sayōnara.",
                spanishSpokenText = "Adiós, hasta la próxima.",
                englishSpokenText = "Goodbye, see you.",
                contextLabel = "DESPEDIDA"
            )
        }
        if (clean.contains("いただきます")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Itadakimasu.",
                spanishSpokenText = "Que aproveche la comida.",
                englishSpokenText = "Enjoy your meal.",
                contextLabel = "COMIDA"
            )
        }
        if (clean.contains("ごちそうさま")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Gochisōsama deshita.",
                spanishSpokenText = "Muchas gracias por la comida, estuvo delicioso.",
                englishSpokenText = "Thank you for the delicious meal.",
                contextLabel = "COMIDA"
            )
        }
        if (clean.contains("すみません") || clean.contains("すいません")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Sumimasen.",
                spanishSpokenText = "Disculpe / Perdón.",
                englishSpokenText = "Excuse me / Pardon.",
                contextLabel = "DISCULPA / ATENCIÓN"
            )
        }
        if (clean.contains("ごめんなさい") || clean.contains("ごめん")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Gomennasai.",
                spanishSpokenText = "Lo siento, perdón por las molestias.",
                englishSpokenText = "I am sorry for the inconvenience.",
                contextLabel = "DISCULPA"
            )
        }

        // 6. Confirmaciones, estados y respuestas cotidianas
        if (clean.contains("大丈夫") || clean.contains("だいじょうぶ")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Daijōbu desu.",
                spanishSpokenText = "Está bien, no hay problema.",
                englishSpokenText = "It is okay, no problem at all.",
                contextLabel = "CONFIRMACIÓN"
            )
        }
        if (clean.contains("わかりました") || clean.contains("かしこまりました") || clean.contains("承知")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Wakarimashita.",
                spanishSpokenText = "Entendido / De acuerdo.",
                englishSpokenText = "Understood / Certainly.",
                contextLabel = "ENTENDIDO"
            )
        }
        if (clean.contains("わかりません") || clean.contains("分かりません") || clean.contains("知らない")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Wakarimasen.",
                spanishSpokenText = "Lo siento, no lo sé o no lo entiendo.",
                englishSpokenText = "Sorry, I don't know or don't understand.",
                contextLabel = "NO SABE"
            )
        }
        if (clean.contains("お待ち") || clean.contains("待って")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Shōshō omachi kudasai.",
                spanishSpokenText = "Por favor, espere un momento.",
                englishSpokenText = "Please wait a moment.",
                contextLabel = "ESPERA"
            )
        }
        if (clean.contains("こちらへどうぞ") || clean.contains("どうぞ")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Kochira e dōzo.",
                spanishSpokenText = "Por aquí por favor, adelante.",
                englishSpokenText = "This way please, go ahead.",
                contextLabel = "PASO / ADELANTE"
            )
        }
        if (clean.contains("満席") || clean.contains("いっぱい") || clean.contains("満員")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Tadaima manseki desu.",
                spanishSpokenText = "Completo / Sin mesas disponibles.",
                englishSpokenText = "Fully booked / No tables available.",
                contextLabel = "LOCAL LLENO"
            )
        }
        if (clean.contains("ラストオーダー") || clean.contains("ラスト")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Rasuto ōdā desu.",
                spanishSpokenText = "Último pedido (Last Order).",
                englishSpokenText = "Last call for orders.",
                contextLabel = "ÚLTIMO PEDIDO"
            )
        }
        if (clean.contains("水") || clean.contains("お冷")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "O-mizu desu.",
                spanishSpokenText = "Agua.",
                englishSpokenText = "Water.",
                contextLabel = "AGUA / BEBIDA"
            )
        }
        if (clean.contains("お茶")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "O-cha desu.",
                spanishSpokenText = "Té.",
                englishSpokenText = "Tea.",
                contextLabel = "TÉ / BEBIDA"
            )
        }
        if (clean.contains("ビール") || clean.contains("生ビール")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Bīru desu.",
                spanishSpokenText = "Cerveza.",
                englishSpokenText = "Beer.",
                contextLabel = "CERVEZA"
            )
        }

        // 7. Pagos y caja (efectivo, tarjeta, Suica)
        if (clean.contains("現金のみ") || clean.contains("現金だけ")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Genkin nomi desu.",
                spanishSpokenText = "Solo efectivo.",
                englishSpokenText = "Cash only, please.",
                contextLabel = "PAGO / EFECTIVO"
            )
        }
        if (clean.contains("カード") && (clean.contains("使えません") || clean.contains("ダメ") || clean.contains("不可") || clean.contains("無理"))) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Kādo wa tsukaemasen.",
                spanishSpokenText = "No se aceptan tarjetas de crédito.",
                englishSpokenText = "Credit cards are not accepted.",
                contextLabel = "SIN TARJETA"
            )
        }
        if (clean.contains("カード") && (clean.contains("使えます") || clean.contains("OK") || clean.contains("大丈夫"))) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Kādo wa tsukaemasu.",
                spanishSpokenText = "Se aceptan tarjetas de crédito.",
                englishSpokenText = "Credit cards are accepted.",
                contextLabel = "PAGO / TARJETA"
            )
        }
        if (clean.contains("Suica") && (clean.contains("使えません") || clean.contains("ダメ") || clean.contains("不可"))) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Suica wa tsukaemasen.",
                spanishSpokenText = "No se puede pagar con Suica.",
                englishSpokenText = "Suica card is not accepted.",
                contextLabel = "SIN SUICA"
            )
        }
        if (clean.contains("Suica") || clean.contains("Pasmo") || clean.contains("ICカード")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Suica / IC kādo desu.",
                spanishSpokenText = "Tarjeta Suica / Tarjeta IC.",
                englishSpokenText = "Suica / IC card.",
                contextLabel = "PAGO / SUICA"
            )
        }
        if (clean.contains("現金")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Genkin desu.",
                spanishSpokenText = "Efectivo.",
                englishSpokenText = "Cash.",
                contextLabel = "PAGO / EFECTIVO"
            )
        }
        if (clean.contains("カード")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Kādo desu.",
                spanishSpokenText = "Tarjeta de crédito.",
                englishSpokenText = "Credit card.",
                contextLabel = "PAGO / TARJETA"
            )
        }
        if (clean.contains("会計") || clean.contains("お会計")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "O-kaikei desu.",
                spanishSpokenText = "La cuenta / Pago.",
                englishSpokenText = "The bill / Payment.",
                contextLabel = "PAGO / CUENTA"
            )
        }

        // 8. Konbini & tienda
        if (clean.contains("売り切れ") || clean.contains("完売")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Urikire desu.",
                spanishSpokenText = "Agotado.",
                englishSpokenText = "Sold out.",
                contextLabel = "AGOTADO"
            )
        }
        if (clean.contains("袋") || clean.contains("レジ袋")) {
            val isQuestion = clean.contains("いります") || clean.contains("必要") || clean.contains("？") || clean.contains("か")
            val esText = if (isQuestion) "¿Necesita bolsa de compra?" else "Bolsa de compra."
            val enText = if (isQuestion) "Do you need a shopping bag?" else "Shopping bag."
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Rejibukuro desu.",
                spanishSpokenText = esText,
                englishSpokenText = enText,
                contextLabel = "TIENDA / BOLSA"
            )
        }
        if (clean.contains("温め")) {
            val isQuestion = clean.contains("ますか") || clean.contains("？") || clean.contains("か")
            val esText = if (isQuestion) "¿Desea calentarlo en microondas?" else "Calentar en microondas."
            val enText = if (isQuestion) "Would you like it heated?" else "Microwave heating."
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Atatame desu.",
                spanishSpokenText = esText,
                englishSpokenText = enText,
                contextLabel = "KONBINI / MICROONDAS"
            )
        }
        if (clean.contains("箸") || clean.contains("お箸")) {
            val isQuestion = clean.contains("いります") || clean.contains("？") || clean.contains("か")
            val esText = if (isQuestion) "¿Desea palillos?" else "Palillos."
            val enText = if (isQuestion) "Do you need chopsticks?" else "Chopsticks."
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "O-hashi desu.",
                spanishSpokenText = esText,
                englishSpokenText = enText,
                contextLabel = "TIENDA / PALILLOS"
            )
        }

        // 9. Tax-Free & Pasaporte
        if (clean.contains("パスポート")) {
            val isRequest = clean.contains("見せて") || clean.contains("お願いします") || clean.contains("提示")
            val esText = if (isRequest) "Por favor, muestre su pasaporte." else "Pasaporte."
            val enText = if (isRequest) "Please show your passport." else "Passport."
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Pasupōto desu.",
                spanishSpokenText = esText,
                englishSpokenText = enText,
                contextLabel = "TAX-FREE / PASAPORTE"
            )
        }
        if (clean.contains("免税")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Menzei desu.",
                spanishSpokenText = "Libre de impuestos (Tax-Free).",
                englishSpokenText = "Tax-Free.",
                contextLabel = "TAX-FREE"
            )
        }

        // 10. Restaurante y comensales
        if (clean.contains("何名")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Nan-mei-sama desu ka?",
                spanishSpokenText = "¿Cuántas personas son?",
                englishSpokenText = "How many people in your party?",
                contextLabel = "RESTAURANTE / PERSONAS"
            )
        }
        if (clean.contains("いらっしゃいませ") || clean.contains("いらっしゃい")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Irasshaimase.",
                spanishSpokenText = "Bienvenido / Pase adelante.",
                englishSpokenText = "Welcome, please come in.",
                contextLabel = "RESTAURANTE / BIENVENIDA"
            )
        }
        if (clean.contains("禁煙")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Kin'en-seki desu.",
                spanishSpokenText = "Mesa / Zona de no fumadores.",
                englishSpokenText = "Non-smoking table / area.",
                contextLabel = "NO FUMADORES"
            )
        }
        if (clean.contains("喫煙")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Kitsuen-seki desu.",
                spanishSpokenText = "Mesa / Zona de fumadores.",
                englishSpokenText = "Smoking table / area.",
                contextLabel = "FUMADORES"
            )
        }
        if (clean.contains("予約")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Yoyaku desu.",
                spanishSpokenText = "Reserva.",
                englishSpokenText = "Reservation.",
                contextLabel = "RESERVA"
            )
        }

        // 11. Estaciones, trenes y Shinkansen
        if (clean.contains("ホーム") || clean.contains("番線") || clean.contains("新幹線") || clean.contains("電車") || clean.contains("切符")) {
            val platformNum = """([0-9]+)\s*番""".toRegex().find(clean)?.groupValues?.get(1)
            val trainText = if (platformNum != null) {
                "Andén número $platformNum."
            } else if (clean.contains("新幹線")) {
                "Tren Shinkansen."
            } else if (clean.contains("切符")) {
                "Billete de tren."
            } else {
                "Andén / Tren."
            }
            val trainTextEn = if (platformNum != null) {
                "Platform number $platformNum."
            } else if (clean.contains("新幹線")) {
                "Shinkansen bullet train."
            } else if (clean.contains("切符")) {
                "Train ticket."
            } else {
                "Platform / Train."
            }
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Hōmu kara hassha shimasu.",
                spanishSpokenText = trainText,
                englishSpokenText = trainTextEn,
                contextLabel = "ESTACIÓN / TREN"
            )
        }

        // 12. Hoteles & Check-in
        if (clean.contains("チェックイン")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Chekkuin desu.",
                spanishSpokenText = "Check-in / Registro.",
                englishSpokenText = "Check-in / Registration.",
                contextLabel = "HOTEL / RECEPCIÓN"
            )
        }
        if (clean.contains("チェックアウト")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Chekkuauto desu.",
                spanishSpokenText = "Check-out / Salida del hotel.",
                englishSpokenText = "Check-out.",
                contextLabel = "HOTEL / SALIDA"
            )
        }
        if (clean.contains("部屋")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "O-heya desu.",
                spanishSpokenText = "Habitación.",
                englishSpokenText = "Room.",
                contextLabel = "HOTEL / HABITACIÓN"
            )
        }
        if (clean.contains("朝食")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Chōshoku desu.",
                spanishSpokenText = "Desayuno.",
                englishSpokenText = "Breakfast.",
                contextLabel = "HOTEL / DESAYUNO"
            )
        }
        if (clean.contains("荷物")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "O-nimotsu desu.",
                spanishSpokenText = "Equipaje / Maletas.",
                englishSpokenText = "Luggage / Baggage.",
                contextLabel = "EQUIPAJE"
            )
        }

        // 13. Direcciones generales
        if (clean.contains("右") || clean.contains("左") || clean.contains("まっすぐ") || clean.contains("あちら")) {
            val dir = when {
                clean.contains("右") -> "A la derecha."
                clean.contains("左") -> "A la izquierda."
                clean.contains("まっすぐ") -> "Todo recto hacia adelante."
                else -> "Hacia allí enfrente."
            }
            val dirEn = when {
                clean.contains("右") -> "On the right."
                clean.contains("左") -> "On the left."
                clean.contains("まっすぐ") -> "Straight ahead."
                else -> "Over there."
            }
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Achira desu.",
                spanishSpokenText = dir,
                englishSpokenText = dirEn,
                contextLabel = "DIRECCIÓN"
            )
        }

        // 14. Reglas, calzado y fotografía
        if (clean.contains("靴") || clean.contains("脱い") || clean.contains("土足")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Kutsu o nuide kudasai.",
                spanishSpokenText = "Por favor, quítese los zapatos antes de entrar.",
                englishSpokenText = "Please take off your shoes before entering.",
                contextLabel = "COSTUMBRE / ZAPATOS"
            )
        }
        if (clean.contains("撮影禁止") || clean.contains("写真")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Satsuei kinshi desu.",
                spanishSpokenText = "Está prohibido hacer fotos o vídeos en este lugar.",
                englishSpokenText = "Photography is prohibited in this area.",
                contextLabel = "PROHIBICIÓN / FOTOS"
            )
        }

        // 15. Fallback léxico fiel directo (NUNCA inventar respuestas de dependiente ni mensajes meta)
        val recognizedWordsEs = mutableListOf<String>()
        val recognizedWordsEn = mutableListOf<String>()
        val vocabulary = listOf(
            Triple("駅", "Estación", "Station"),
            Triple("ホテル", "Hotel", "Hotel"),
            Triple("空港", "Aeropuerto", "Airport"),
            Triple("バス", "Autobús", "Bus"),
            Triple("タクシー", "Taxi", "Taxi"),
            Triple("地下鉄", "Metro", "Subway"),
            Triple("電車", "Tren", "Train"),
            Triple("切符", "Billete", "Ticket"),
            Triple("時間", "Horario", "Schedule"),
            Triple("雨", "Lluvia", "Rain"),
            Triple("大雨", "Lluvia intensa", "Heavy rain"),
            Triple("薬", "Farmacia / Medicamento", "Medicine"),
            Triple("病院", "Hospital", "Hospital"),
            Triple("交番", "Puesto de policía", "Police box"),
            Triple("出口", "Salida", "Exit"),
            Triple("入口", "Entrada", "Entrance"),
            Triple("今日", "Hoy", "Today"),
            Triple("明日", "Mañana", "Tomorrow"),
            Triple("朝", "Mañana", "Morning"),
            Triple("夜", "Noche", "Night"),
            Triple("可能", "Posibilidad", "Possibility"),
            Triple("ラーメン", "Ramen", "Ramen"),
            Triple("うどん", "Udon", "Udon"),
            Triple("そば", "Soba", "Soba"),
            Triple("寿司", "Sushi", "Sushi"),
            Triple("水", "Agua", "Water")
        )

        for ((kanji, esTerm, enTerm) in vocabulary) {
            if (clean.contains(kanji) && !recognizedWordsEs.contains(esTerm)) {
                recognizedWordsEs.add(esTerm)
                recognizedWordsEn.add(enTerm)
            }
        }

        val spanishSummary = if (recognizedWordsEs.isNotEmpty()) {
            recognizedWordsEs.joinToString(" · ")
        } else {
            "Traducción directa no disponible offline."
        }

        val englishSummary = if (recognizedWordsEn.isNotEmpty()) {
            recognizedWordsEn.joinToString(" · ")
        } else {
            "Direct translation not available offline."
        }

        return JapaneseToTravelerVoiceResult(
            japaneseHeardText = clean,
            romaji = "Nihongo: $clean",
            spanishSpokenText = spanishSummary,
            englishSpokenText = englishSummary,
            contextLabel = "TRADUCCIÓN JAPONÉS"
        )
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
            japaneseText = "トイレはどこですか？",
            romaji = "Toire wa doko desu ka?",
            categoryBadge = "BAÑO / ASEO"
        ),
        SimultaneousTranslationItem(
            sourceText = "¿Cuánto cuesta esto, por favor?",
            japaneseText = "これはいくらですか？",
            romaji = "Kore wa ikura desu ka?",
            categoryBadge = "PRECIO / COMPRAS"
        ),
        SimultaneousTranslationItem(
            sourceText = "Tengo alergia alimentaria",
            japaneseText = "アレルギーがあります。",
            romaji = "Arerugī ga arimasu.",
            categoryBadge = "ALERGIA / SALUD"
        ),
        SimultaneousTranslationItem(
            sourceText = "¿Tienen menú en inglés?",
            japaneseText = "英語のメニューはありますか？",
            romaji = "Eigo no menyū wa arimasu ka?",
            categoryBadge = "RESTAURANTE"
        ),
        SimultaneousTranslationItem(
            sourceText = "La cuenta, por favor",
            japaneseText = "お会計をお願いします。",
            romaji = "O-kaikei o onegaishimasu.",
            categoryBadge = "PAGO / CUENTA"
        ),
        SimultaneousTranslationItem(
            sourceText = "¿Hacen compras libres de impuestos (Tax-Free)?",
            japaneseText = "免税はできますか？",
            romaji = "Menzei wa dekimasu ka?",
            categoryBadge = "TAX-FREE"
        ),
        SimultaneousTranslationItem(
            sourceText = "A esta dirección de hotel, por favor",
            japaneseText = "このホテルの住所までお願いします。",
            romaji = "Kono hoteru no jūsho made onegaishimasu.",
            categoryBadge = "TAXI / HOTEL"
        )
    )

    val travelerQuickPhrases = listOf(
        // 1. CORTESÍA Y SALUDOS
        TravelerQuickPhrase(
            sourceEs = "Hola / Buenos días",
            sourceEn = "Hello / Good day",
            japaneseText = "こんにちは。",
            romaji = "Konnichiwa.",
            categoryBadge = "CORTESÍA"
        ),
        TravelerQuickPhrase(
            sourceEs = "Disculpe / Perdón",
            sourceEn = "Excuse me",
            japaneseText = "すみません。",
            romaji = "Sumimasen.",
            categoryBadge = "CORTESÍA"
        ),
        TravelerQuickPhrase(
            sourceEs = "Muchas gracias",
            sourceEn = "Thank you very much",
            japaneseText = "どうもありがとうございます。",
            romaji = "Dōmo arigatō gozaimasu.",
            categoryBadge = "CORTESÍA"
        ),
        TravelerQuickPhrase(
            sourceEs = "De nada",
            sourceEn = "You are welcome",
            japaneseText = "どういたしまして。",
            romaji = "Dōitashimashite.",
            categoryBadge = "CORTESÍA"
        ),
        TravelerQuickPhrase(
            sourceEs = "Sí",
            sourceEn = "Yes",
            japaneseText = "はい。",
            romaji = "Hai.",
            categoryBadge = "CORTESÍA"
        ),
        TravelerQuickPhrase(
            sourceEs = "No",
            sourceEn = "No",
            japaneseText = "いいえ。",
            romaji = "Iie.",
            categoryBadge = "CORTESÍA"
        ),
        TravelerQuickPhrase(
            sourceEs = "Adiós / Hasta luego",
            sourceEn = "Goodbye",
            japaneseText = "さようなら。",
            romaji = "Sayōnara.",
            categoryBadge = "CORTESÍA"
        ),

        // 2. RESTAURANTE Y COMIDA
        TravelerQuickPhrase(
            sourceEs = "¿Tienen menú en inglés?",
            sourceEn = "Do you have an English menu?",
            japaneseText = "英語のメニューはありますか？",
            romaji = "Eigo no menyū wa arimasu ka?",
            categoryBadge = "RESTAURANTE"
        ),
        TravelerQuickPhrase(
            sourceEs = "La cuenta, por favor",
            sourceEn = "The check, please",
            japaneseText = "お会計をお願いします。",
            romaji = "O-kaikei o onegaishimasu.",
            categoryBadge = "RESTAURANTE"
        ),
        TravelerQuickPhrase(
            sourceEs = "Agua, por favor",
            sourceEn = "Water, please",
            japaneseText = "お水をお願いします。",
            romaji = "O-mizu o onegaishimasu.",
            categoryBadge = "RESTAURANTE"
        ),
        TravelerQuickPhrase(
            sourceEs = "Una cerveza, por favor",
            sourceEn = "One beer, please",
            japaneseText = "ビールを1つお願いします。",
            romaji = "Bīru o hitotsu onegaishimasu.",
            categoryBadge = "RESTAURANTE"
        ),
        TravelerQuickPhrase(
            sourceEs = "Dos cervezas, por favor",
            sourceEn = "Two beers, please",
            japaneseText = "ビールを2つお願いします。",
            romaji = "Bīru o futatsu onegaishimasu.",
            categoryBadge = "RESTAURANTE"
        ),
        TravelerQuickPhrase(
            sourceEs = "Está muy delicioso",
            sourceEn = "It is very delicious",
            japaneseText = "とても美味しいです。",
            romaji = "Totemo oishii desu.",
            categoryBadge = "RESTAURANTE"
        ),
        TravelerQuickPhrase(
            sourceEs = "Buen provecho (antes de comer)",
            sourceEn = "Bon appetit (before eating)",
            japaneseText = "いただきます。",
            romaji = "Itadakimasu.",
            categoryBadge = "RESTAURANTE"
        ),
        TravelerQuickPhrase(
            sourceEs = "Gracias por la comida (al terminar)",
            sourceEn = "Thank you for the meal (after eating)",
            japaneseText = "ごちそうさまでした。",
            romaji = "Gochisōsama deshita.",
            categoryBadge = "RESTAURANTE"
        ),

        // 3. TRANSPORTE Y DIRECCIONES
        TravelerQuickPhrase(
            sourceEs = "¿Dónde está el baño?",
            sourceEn = "Where is the restroom?",
            japaneseText = "トイレはどこですか？",
            romaji = "Toire wa doko desu ka?",
            categoryBadge = "TRANSPORTE"
        ),
        TravelerQuickPhrase(
            sourceEs = "¿Dónde está la estación?",
            sourceEn = "Where is the station?",
            japaneseText = "駅はどこですか？",
            romaji = "Eki wa doko desu ka?",
            categoryBadge = "TRANSPORTE"
        ),
        TravelerQuickPhrase(
            sourceEs = "¿Dónde está el andén del Shinkansen?",
            sourceEn = "Where is the Shinkansen platform?",
            japaneseText = "新幹線のホームはどこですか？",
            romaji = "Shinkansen no hōmu wa doko desu ka?",
            categoryBadge = "TRANSPORTE"
        ),
        TravelerQuickPhrase(
            sourceEs = "A esta dirección de hotel, por favor",
            sourceEn = "To this hotel address, please",
            japaneseText = "このホテルの住所までお願いします。",
            romaji = "Kono hoteru no jūsho made onegaishimasu.",
            categoryBadge = "HOTEL / TAXI"
        ),

        // 4. COMPRAS Y PAGOS
        TravelerQuickPhrase(
            sourceEs = "¿Cuánto cuesta esto?",
            sourceEn = "How much is this?",
            japaneseText = "これはいくらですか？",
            romaji = "Kore wa ikura desu ka?",
            categoryBadge = "COMPRAS"
        ),
        TravelerQuickPhrase(
            sourceEs = "¿Aceptan tarjeta de crédito?",
            sourceEn = "Do you accept credit card?",
            japaneseText = "カードは使えますか？",
            romaji = "Kādo wa tsukaemasu ka?",
            categoryBadge = "PAGOS"
        ),
        TravelerQuickPhrase(
            sourceEs = "¿Puedo pagar con Suica / Pasmo?",
            sourceEn = "Can I pay with Suica / Pasmo?",
            japaneseText = "Suicaは使えますか？",
            romaji = "Suica wa tsukaemasu ka?",
            categoryBadge = "PAGOS"
        ),
        TravelerQuickPhrase(
            sourceEs = "¿Aceptan efectivo?",
            sourceEn = "Do you accept cash?",
            japaneseText = "現金は使えますか？",
            romaji = "Genkin wa tsukaemasu ka?",
            categoryBadge = "PAGOS"
        ),
        TravelerQuickPhrase(
            sourceEs = "¿Hacen compras libres de impuestos (Tax-Free)?",
            sourceEn = "Do you do Tax-Free shopping?",
            japaneseText = "免税はできますか？",
            romaji = "Menzei wa dekimasu ka?",
            categoryBadge = "TAX-FREE"
        ),
        TravelerQuickPhrase(
            sourceEs = "No necesito bolsa, gracias",
            sourceEn = "No bag needed, thank you",
            japaneseText = "袋は大丈夫です。",
            romaji = "Fukuro wa daijōbu desu.",
            categoryBadge = "COMPRAS"
        ),

        // 5. SALUD Y EMERGENCIAS
        TravelerQuickPhrase(
            sourceEs = "Tengo alergia alimentaria",
            sourceEn = "I have a food allergy",
            japaneseText = "アレルギーがあります。",
            romaji = "Arerugī ga arimasu.",
            categoryBadge = "SALUD"
        ),
        TravelerQuickPhrase(
            sourceEs = "Por favor, ayúdeme",
            sourceEn = "Please help me",
            japaneseText = "助けてください。",
            romaji = "Tasukete kudasai.",
            categoryBadge = "EMERGENCIA"
        ),
        TravelerQuickPhrase(
            sourceEs = "No hablo japonés",
            sourceEn = "I do not speak Japanese",
            japaneseText = "日本語が話せません。",
            romaji = "Nihongo ga hanasemasen.",
            categoryBadge = "CORTESÍA"
        )
    )

    fun translateToJapanese(input: String): SimultaneousTranslationItem {
        val raw = input.trim()
        if (raw.isBlank()) {
            return SimultaneousTranslationItem(
                sourceText = "",
                japaneseText = "",
                romaji = "",
                categoryBadge = "TEXTO VACÍO"
            )
        }
        val normalized = raw.lowercase(Locale.ROOT)
        val hasDisculpe = normalized.contains("disculpe") || normalized.contains("perdón") || normalized.contains("perdon") || normalized.contains("excuse") || normalized.contains("sorry")
        val prefix = if (hasDisculpe && normalized != "disculpe" && normalized != "perdón" && normalized != "perdon" && normalized != "excuse me" && normalized != "sorry") "すみません、" else ""
        val prefixRomaji = if (hasDisculpe && normalized != "disculpe" && normalized != "perdón" && normalized != "perdon" && normalized != "excuse me" && normalized != "sorry") "Sumimasen, " else ""

        return when {
            // Disculpa pura
            normalized == "disculpe" || normalized == "disculpe por favor" || normalized == "perdón" || normalized == "perdon" || normalized == "excuse me" || normalized == "sorry" ->
                SimultaneousTranslationItem(raw, "すみません。", "Sumimasen.", "DISCULPA")

            // 1. Saludos y cortesía (fieles y exactos, sin añadir elogios inventados)
            normalized.contains("buenos días") || normalized.contains("buenos dias") || normalized.contains("good morning") ->
                SimultaneousTranslationItem(raw, "おはようございます。", "Ohayō gozaimasu.", "SALUDO")
            normalized.contains("buenas noches") || normalized.contains("good evening") || normalized.contains("good night") ->
                SimultaneousTranslationItem(raw, "こんばんは。", "Konbanwa.", "SALUDO")
            normalized.contains("hola") || normalized.contains("hello") || normalized == "hi" || normalized.startsWith("hi ") || normalized.contains(" hi ") || normalized.endsWith(" hi") ->
                SimultaneousTranslationItem(raw, "こんにちは。", "Konnichiwa.", "SALUDO")
            normalized.contains("adiós") || normalized.contains("adios") || normalized.contains("hasta luego") || normalized.contains("goodbye") || normalized.contains("bye") || normalized.contains("chao") ->
                SimultaneousTranslationItem(raw, "さようなら。", "Sayōnara.", "DESPEDIDA")
            normalized.contains("muchas gracias") || normalized.contains("thank you very much") ->
                SimultaneousTranslationItem(raw, "どうもありがとうございます。", "Dōmo arigatō gozaimasu.", "AGRADECIMIENTO")
            normalized.contains("gracias") || normalized.contains("thank you") || normalized.contains("thanks") ->
                SimultaneousTranslationItem(raw, "ありがとうございます。", "Arigatō gozaimasu.", "AGRADECIMIENTO")
            normalized.contains("de nada") || normalized.contains("welcome") ->
                SimultaneousTranslationItem(raw, "どういたしまして。", "Dōitashimashite.", "CORTESÍA")
            normalized.contains("que aproveche") || normalized.contains("buen provecho") || normalized.contains("itadakimasu") ->
                SimultaneousTranslationItem(raw, "いただきます。", "Itadakimasu.", "COMIDA")
            normalized.contains("gracias por la comida") ->
                SimultaneousTranslationItem(raw, "ごちそうさまでした。", "Gochisōsama deshita.", "COMIDA")
            normalized.contains("está delicioso") || normalized.contains("esta delicioso") || normalized.contains("muy rico") || normalized.contains("delicious") ->
                SimultaneousTranslationItem(raw, "とても美味しいです。", "Totemo oishii desu.", "ELOGIO COMIDA")
            normalized.contains("de acuerdo") || normalized.contains("entendido") || normalized.contains("vale") || normalized.contains("understood") ->
                SimultaneousTranslationItem(raw, "わかりました。", "Wakarimashita.", "CONFIRMACIÓN")
            normalized.contains("está bien") || normalized.contains("esta bien") || normalized.contains("no hay problema") || normalized.contains("no se preocupe") || normalized.contains("no problem") ->
                SimultaneousTranslationItem(raw, "大丈夫です。", "Daijōbu desu.", "CONFIRMACIÓN")
            normalized == "sí" || normalized == "si" || normalized == "yes" ->
                SimultaneousTranslationItem(raw, "はい。", "Hai.", "RESPUESTA")
            normalized == "no" ->
                SimultaneousTranslationItem(raw, "いいえ。", "Iie.", "RESPUESTA")

            // 2. Restaurante, comida y pedidos directos (exactamente lo pedido)
            normalized.contains("cuenta") || normalized.contains("bill") || normalized.contains("check please") || normalized.contains("the check") ->
                SimultaneousTranslationItem(raw, "お会計をお願いします。", "O-kaikei o onegaishimasu.", "PAGO / CUENTA")
            normalized.contains("tarjeta") || normalized.contains("card") || normalized.contains("credit") ->
                SimultaneousTranslationItem(raw, "${prefix}カードは使えますか？", "${prefixRomaji}Kādo wa tsukaemasu ka?", "PAGO / TARJETA")
            normalized.contains("suica") || normalized.contains("pasmo") ->
                SimultaneousTranslationItem(raw, "${prefix}Suicaは使えますか？", "${prefixRomaji}Suica wa tsukaemasu ka?", "PAGO / SUICA")
            normalized.contains("efectivo") || normalized.contains("cash") ->
                SimultaneousTranslationItem(raw, "${prefix}現金は使えますか？", "${prefixRomaji}Genkin wa tsukaemasu ka?", "PAGO / EFECTIVO")
            normalized.contains("agua") || normalized.contains("water") -> {
                val (jp, rmj) = when {
                    normalized.contains("dos") || normalized.contains("2") || normalized.contains("two") ->
                        "お水を2つお願いします。" to "O-mizu o futatsu onegaishimasu."
                    normalized.contains("tres") || normalized.contains("3") || normalized.contains("three") ->
                        "お水を3つお願いします。" to "O-mizu o mittsu onegaishimasu."
                    normalized.contains("un ") || normalized.contains("uno") || normalized.contains("una ") || normalized.contains("1") || normalized.contains("one") ->
                        "お水を1つお願いします。" to "O-mizu o hitotsu onegaishimasu."
                    else ->
                        "お水をお願いします。" to "O-mizu o onegaishimasu."
                }
                SimultaneousTranslationItem(raw, "${prefix}${jp}", "${prefixRomaji}${rmj}", "BEBIDA / AGUA")
            }
            normalized.contains("cerveza") || normalized.contains("beer") -> {
                val (jp, rmj) = when {
                    normalized.contains("dos") || normalized.contains("2") || normalized.contains("two") ->
                        "ビールを2つお願いします。" to "Bīru o futatsu onegaishimasu."
                    normalized.contains("tres") || normalized.contains("3") || normalized.contains("three") ->
                        "ビールを3つお願いします。" to "Bīru o mittsu onegaishimasu."
                    normalized.contains("una") || normalized.contains("1") || normalized.contains("one") || normalized.contains("un ") ->
                        "ビールを1つお願いします。" to "Bīru o hitotsu onegaishimasu."
                    else ->
                        "ビールをお願いします。" to "Bīru o onegaishimasu."
                }
                SimultaneousTranslationItem(raw, "${prefix}${jp}", "${prefixRomaji}${rmj}", "BEBIDA")
            }
            normalized.contains("té") || normalized.contains(" té ") || normalized.startsWith("te ") || normalized.contains(" te ") || normalized.endsWith(" te") || normalized.contains("tea") ->
                SimultaneousTranslationItem(raw, "${prefix}お茶をお願いします。", "${prefixRomaji}O-cha o onegaishimasu.", "BEBIDA")
            normalized.contains("menú en inglés") || normalized.contains("menu en ingles") || normalized.contains("carta en inglés") || normalized.contains("carta en ingles") || normalized.contains("english menu") ->
                SimultaneousTranslationItem(raw, "${prefix}英語のメニューはありますか？", "${prefixRomaji}Eigo no menyū wa arimasu ka?", "RESTAURANTE")
            normalized.contains("menú") || normalized.contains("menu") || normalized.contains("carta") ->
                SimultaneousTranslationItem(raw, "${prefix}メニューをお願いします。", "${prefixRomaji}Menyū o onegaishimasu.", "RESTAURANTE")
            normalized.contains("recomend") || normalized.contains("sugiere") ->
                SimultaneousTranslationItem(raw, "${prefix}おすすめは何ですか？", "${prefixRomaji}Osusume wa nan desu ka?", "RESTAURANTE")
            normalized.contains("esto por favor") || normalized.contains("este plato") || normalized.contains("this please") ->
                SimultaneousTranslationItem(raw, "これをお願いします。", "Kore o onegaishimasu.", "PEDIDO")
            normalized.contains("dos personas") || normalized.contains("2 personas") || normalized.contains("somos dos") || normalized.contains("para dos") ->
                SimultaneousTranslationItem(raw, "2名です。", "Ni-mei desu.", "RESERVA / MESA")
            normalized.contains("una persona") || normalized.contains("1 persona") || normalized.contains("para uno") ->
                SimultaneousTranslationItem(raw, "1名です。", "Ichi-mei desu.", "RESERVA / MESA")
            normalized.contains("tres personas") || normalized.contains("3 personas") ->
                SimultaneousTranslationItem(raw, "3名です。", "San-mei desu.", "RESERVA / MESA")
            normalized.contains("cuatro personas") || normalized.contains("4 personas") ->
                SimultaneousTranslationItem(raw, "4名です。", "Yon-mei desu.", "RESERVA / MESA")
            normalized.contains("reserva") || normalized.contains("reservation") ->
                SimultaneousTranslationItem(raw, "予約があります。", "Yoyaku ga arimasu.", "RESERVA")

            // 3. Dietas y alergias estrictas (exactamente la dicha, sin asumir marisco ni carne)
            normalized.contains("vegetariano") || normalized.contains("vegetarian") ->
                SimultaneousTranslationItem(raw, "ベジタリアンです。", "Bejitarian desu.", "DIETA")
            normalized.contains("vegano") || normalized.contains("vegan") ->
                SimultaneousTranslationItem(raw, "ヴィーガンです。", "Vīgan desu.", "DIETA")
            normalized.contains("sin carne") || normalized.contains("no meat") ->
                SimultaneousTranslationItem(raw, "肉なしでお願いします。", "Niku nashi de onegaishimasu.", "DIETA")
            normalized.contains("sin pescado") || normalized.contains("no fish") ->
                SimultaneousTranslationItem(raw, "魚なしでお願いします。", "Sakana nashi de onegaishimasu.", "DIETA")
            normalized.contains("sin picante") || normalized.contains("no spicy") ->
                SimultaneousTranslationItem(raw, "辛くしないでください。", "Karaku shinaide kudasai.", "COMIDA")
            normalized.contains("marisco") || normalized.contains("shellfish") ->
                SimultaneousTranslationItem(raw, "甲殻類のアレルギーがあります。", "Kōkakurui no arerugī ga arimasu.", "ALERGIA")
            normalized.contains("cacahuete") || normalized.contains("frutos secos") || normalized.contains("peanut") || normalized.contains("nuts") ->
                SimultaneousTranslationItem(raw, "ピーナッツのアレルギーがあります。", "Pīnattsu no arerugī ga arimasu.", "ALERGIA")
            normalized.contains("gluten") || normalized.contains("celiac") || normalized.contains("celíac") ->
                SimultaneousTranslationItem(raw, "グルテンのアレルギーがあります。", "Guruten no arerugī ga arimasu.", "ALERGIA")
            normalized.contains("leche") || normalized.contains("lácteo") || normalized.contains("lacteo") || normalized.contains("dairy") || normalized.contains("milk") ->
                SimultaneousTranslationItem(raw, "乳製品のアレルギーがあります。", "Nyūseihin no arerugī ga arimasu.", "ALERGIA")
            normalized.contains("huevo") || normalized.contains("egg") ->
                SimultaneousTranslationItem(raw, "卵のアレルギーがあります。", "Tamago no arerugī ga arimasu.", "ALERGIA")
            normalized.contains("alergia") || normalized.contains("allergy") ->
                SimultaneousTranslationItem(raw, "アレルギーがあります。", "Arerugī ga arimasu.", "ALERGIA")

            // 4. Preguntas de "¿Dónde está...?" (sintéticas y limpias)
            normalized.contains("baño") || normalized.contains("aseo") || normalized.contains("servicio") || normalized.contains("toilet") || normalized.contains("restroom") || normalized.contains("bathroom") ->
                SimultaneousTranslationItem(raw, "${prefix}トイレはどこですか？", "${prefixRomaji}Toire wa doko desu ka?", "BAÑO / ASEO")
            normalized.contains("shinkansen") ->
                SimultaneousTranslationItem(raw, "${prefix}新幹線のホームはどこですか？", "${prefixRomaji}Shinkansen no hōmu wa doko desu ka?", "TREN / SHINKANSEN")
            normalized.contains("andén") || normalized.contains("anden") || normalized.contains("vía") || normalized.contains("via") || normalized.contains("platform") ->
                SimultaneousTranslationItem(raw, "${prefix}ホームはどこですか？", "${prefixRomaji}Hōmu wa doko desu ka?", "TREN / ANDÉN")
            normalized.contains("estación") || normalized.contains("estacion") || normalized.contains("station") ->
                SimultaneousTranslationItem(raw, "${prefix}駅はどこですか？", "${prefixRomaji}Eki wa doko desu ka?", "ESTACIÓN")
            normalized.contains("metro") || normalized.contains("subway") ->
                SimultaneousTranslationItem(raw, "${prefix}地下鉄はどこですか？", "${prefixRomaji}Chikatetsu wa doko desu ka?", "METRO")
            normalized.contains("autobús") || normalized.contains("autobus") || normalized.contains("bus") ->
                SimultaneousTranslationItem(raw, "${prefix}バス停はどこですか？", "${prefixRomaji}Basutei wa doko desu ka?", "AUTOBÚS")
            normalized.contains("ascensor") || normalized.contains("elevator") || normalized.contains("lift") ->
                SimultaneousTranslationItem(raw, "${prefix}エレベーターはどこですか？", "${prefixRomaji}Erebētā wa doko desu ka?", "ASCENSOR")
            normalized.contains("salida") || normalized.contains("exit") ->
                SimultaneousTranslationItem(raw, "${prefix}出口はどこですか？", "${prefixRomaji}Deguchi wa doko desu ka?", "SALIDA")
            normalized.contains("taquilla") || normalized.contains("consigna") || normalized.contains("maleta") || normalized.contains("equipaje") || normalized.contains("locker") || normalized.contains("luggage") ->
                SimultaneousTranslationItem(raw, "${prefix}コインロッカーはどこですか？", "${prefixRomaji}Koin rokkā wa doko desu ka?", "EQUIPAJE / CONSIGNA")
            normalized.contains("farmacia") || normalized.contains("pharmacy") ->
                SimultaneousTranslationItem(raw, "${prefix}薬局はどこですか？", "${prefixRomaji}Yakkyoku wa doko desu ka?", "FARMACIA")
            normalized.contains("hospital") ->
                SimultaneousTranslationItem(raw, "${prefix}病院はどこですか？", "${prefixRomaji}Byōin wa doko desu ka?", "HOSPITAL")
            normalized.contains("policía") || normalized.contains("policia") || normalized.contains("police") || normalized.contains("koban") ->
                SimultaneousTranslationItem(raw, "${prefix}交番はどこですか？", "${prefixRomaji}Kōban wa doko desu ka?", "POLICÍA")
            normalized.contains("cajero") || normalized.contains("atm") ->
                SimultaneousTranslationItem(raw, "${prefix}ATMはどこですか？", "${prefixRomaji}Ē-tī-emu wa doko desu ka?", "CAJERO")
            normalized.contains("wifi") || normalized.contains("wi-fi") || normalized.contains("internet") ->
                SimultaneousTranslationItem(raw, "${prefix}Wi-Fiはありますか？", "${prefixRomaji}Waifai wa arimasu ka?", "INTERNET")
            normalized.contains("konbini") || normalized.contains("tienda") || normalized.contains("convenience") ->
                SimultaneousTranslationItem(raw, "${prefix}コンビニはどこですか？", "${prefixRomaji}Konbini wa doko desu ka?", "TIENDA")

            // 5. Precios y compras
            normalized.contains("cuánto cuesta") || normalized.contains("cuanto cuesta") || normalized.contains("cuánto es") || normalized.contains("cuanto es") || normalized.contains("precio") || normalized.contains("how much") || normalized.contains("price") -> {
                val hasThis = normalized.contains("esto") || normalized.contains("este") || normalized.contains("this")
                val jp = if (hasThis) "これはいくらですか？" else "いくらですか？"
                val rmj = if (hasThis) "Kore wa ikura desu ka?" else "Ikura desu ka?"
                SimultaneousTranslationItem(raw, "${prefix}$jp", "${prefixRomaji}$rmj", "PRECIO / COMPRAS")
            }
            normalized.contains("tax") || normalized.contains("impuesto") || normalized.contains("free") ->
                SimultaneousTranslationItem(raw, "免税はできますか？", "Menzei wa dekimasu ka?", "TAX-FREE")
            normalized.contains("pasaporte") || normalized.contains("passport") ->
                SimultaneousTranslationItem(raw, "パスポートです。", "Pasupōto desu.", "IDENTIFICACIÓN")
            normalized.contains("bolsa") || normalized.contains("bag") ->
                SimultaneousTranslationItem(raw, "レジ袋をお願いします。", "Rejibukuro o onegaishimasu.", "TIENDA")
            normalized.contains("recibo") || normalized.contains("ticket") || normalized.contains("receipt") ->
                SimultaneousTranslationItem(raw, "レシートをお願いします。", "Reshīto o onegaishimasu.", "TIENDA")
            normalized.contains("probar") || normalized.contains("try on") ->
                SimultaneousTranslationItem(raw, "試着できますか？", "Shichaku dekimasu ka?", "TIENDA")

            // 6. Transporte, taxi y direcciones
            normalized.contains("taxi") || normalized.contains("hotel") || normalized.contains("dirección") || normalized.contains("direccion") || normalized.contains("address") || normalized.contains("lléveme") || normalized.contains("lleveme") ->
                SimultaneousTranslationItem(raw, "このホテルの住所までお願いします。", "Kono hoteru no jūsho made onegaishimasu.", "TAXI / HOTEL")
            normalized.contains("billete") || normalized.contains("ticket de tren") || normalized.contains("train ticket") ->
                SimultaneousTranslationItem(raw, "切符をお願いします。", "Kippu o onegaishimasu.", "BILLETE")
            normalized.contains("aeropuerto") || normalized.contains("airport") ->
                SimultaneousTranslationItem(raw, "空港までお願いします。", "Kūkō made onegaishimasu.", "TRANSPORTE / AEROPUERTO")

            // 7. Ayuda y emergencias
            normalized.contains("ayuda") || normalized.contains("help") ->
                SimultaneousTranslationItem(raw, "助けてください。", "Tasukete kudasai.", "AYUDA")
            normalized.contains("mal") || normalized.contains("enfermo") || normalized.contains("sick") ->
                SimultaneousTranslationItem(raw, "気分が悪いです。", "Kibun ga warui desu.", "SALUD")
            normalized.contains("perdido") || normalized.contains("perdí") || normalized.contains("perdi") || normalized.contains("lost") ->
                SimultaneousTranslationItem(raw, "無くしました。", "Nakushimashita.", "OBJETO PERDIDO")

            // 8. Interacción y lenguaje
            normalized.contains("foto") || normalized.contains("photo") ->
                SimultaneousTranslationItem(raw, "写真を撮ってもいいですか？", "Shashin o totte mo ii desu ka?", "FOTOGRAFÍA")
            normalized.contains("inglés") || normalized.contains("ingles") || normalized.contains("english") ->
                SimultaneousTranslationItem(raw, "英語を話せますか？", "Eigo o hanasemasu ka?", "IDIOMA")
            normalized.contains("despacio") || normalized.contains("slowly") ->
                SimultaneousTranslationItem(raw, "ゆっくり話してください。", "Yukkuri hanashite kudasai.", "COMUNICACIÓN")
            normalized.contains("otra vez") || normalized.contains("repetir") || normalized.contains("again") ->
                SimultaneousTranslationItem(raw, "もう一度お願いします。", "Mō ichido onegaishimasu.", "COMUNICACIÓN")

            // 9. Fallback conciso: NUNCA añadir más palabras ni mensajes sobre apps de traducción
            normalized.contains("dónde") || normalized.contains("donde") || normalized.contains("where") ->
                SimultaneousTranslationItem(raw, "${prefix}どこですか？", "${prefixRomaji}Doko desu ka?", "PREGUNTA")
            normalized.contains("por favor") || normalized.contains("favor") || normalized.contains("please") ->
                SimultaneousTranslationItem(raw, "お願いします。", "Onegaishimasu.", "PETICIÓN")
            else ->
                SimultaneousTranslationItem(raw, "${prefix}お願いします。", "${prefixRomaji}Onegaishimasu.", "PETICIÓN")
        }
    }
}

object OnlineTranslatorClient {

    fun translate(text: String, from: String, to: String): String? {
        val clean = text.trim()
        if (clean.isBlank()) return null
        return try {
            val encoded = java.net.URLEncoder.encode(clean, "UTF-8")
            val url = java.net.URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=$from&tl=$to&dt=t&q=$encoded")
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val pattern = """^\[\[\["(.*?)"\s*,\s*"""".toRegex()
                val match = pattern.find(json)
                match?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\n", "\n")?.replace("\\\\", "\\")
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
