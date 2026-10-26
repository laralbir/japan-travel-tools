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
        val prefix = if (hasDisculpe) "すみません、" else ""
        val prefixRomaji = if (hasDisculpe) "Sumimasen, " else ""

        return when {
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
            normalized.contains("cuenta") || normalized.contains("bill") || normalized.contains("check please") ->
                SimultaneousTranslationItem(raw, "お会計をお願いします。", "O-kaikei o onegaishimasu.", "PAGO / CUENTA")
            normalized.contains("tarjeta") || normalized.contains("card") || normalized.contains("credit") ->
                SimultaneousTranslationItem(raw, "カードは使えますか？", "Kādo wa tsukaemasu ka?", "PAGO / TARJETA")
            normalized.contains("suica") || normalized.contains("pasmo") ->
                SimultaneousTranslationItem(raw, "Suicaは使えますか？", "Suica wa tsukaemasu ka?", "PAGO / SUICA")
            normalized.contains("efectivo") || normalized.contains("cash") ->
                SimultaneousTranslationItem(raw, "現金は使えますか？", "Genkin wa tsukaemasu ka?", "PAGO / EFECTIVO")
            normalized.contains("agua") || normalized.contains("water") -> {
                val qty = when {
                    normalized.contains("dos") || normalized.contains("2") -> "お水を2つ"
                    normalized.contains("tres") || normalized.contains("3") -> "お水を3つ"
                    else -> "お水"
                }
                val qtyRomaji = when {
                    normalized.contains("dos") || normalized.contains("2") -> "O-mizu o futatsu"
                    normalized.contains("tres") || normalized.contains("3") -> "O-mizu o mittsu"
                    else -> "O-mizu"
                }
                SimultaneousTranslationItem(raw, "${prefix}${qty}をお願いします。", "${prefixRomaji}${qtyRomaji} o onegaishimasu.", "BEBIDA / AGUA")
            }
            normalized.contains("cerveza") || normalized.contains("beer") ->
                SimultaneousTranslationItem(raw, "${prefix}ビールをお願いします。", "${prefixRomaji}Bīru o onegaishimasu.", "BEBIDA")
            normalized.contains("té") || normalized.contains(" té ") || normalized.startsWith("te ") || normalized.contains(" te ") || normalized.endsWith(" te") || normalized.contains("tea") ->
                SimultaneousTranslationItem(raw, "${prefix}お茶をお願いします。", "${prefixRomaji}O-cha o onegaishimasu.", "BEBIDA")
            normalized.contains("menú en inglés") || normalized.contains("menu en ingles") || normalized.contains("carta en inglés") || normalized.contains("english menu") ->
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

            // 5. Precios y compras
            normalized.contains("cuánto cuesta") || normalized.contains("cuanto cuesta") || normalized.contains("cuánto es") || normalized.contains("cuanto es") || normalized.contains("precio") || normalized.contains("how much") || normalized.contains("price") ->
                SimultaneousTranslationItem(raw, "${prefix}いくらですか？", "${prefixRomaji}Ikura desu ka?", "PRECIO / COMPRAS")
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
                SimultaneousTranslationItem(raw, "この住所までお願いします。", "Kono jūsho made onegaishimasu.", "TAXI / HOTEL")
            normalized.contains("billete") || normalized.contains("ticket de tren") ->
                SimultaneousTranslationItem(raw, "切符をお願いします。", "Kippu o onegaishimasu.", "BILLETE")

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

enum class AssistantSource {
    GEMINI_CLOUD,
    OFFLINE_EXPERT
}

data class AiTravelAssistantResult(
    val answer: String,
    val source: AssistantSource,
    val timestampFormatted: String,
    val cloudErrorDetail: String? = null
)

object GeminiAssistantEngine {

    /**
     * Executes an AI query via Gemini Cloud API if an API key is available and connectivity exists,
     * otherwise falls back seamlessly to the comprehensive offline expert engine, providing
     * transparent error diagnostics if the cloud call failed.
     */
    suspend fun queryAssistant(
        prompt: String,
        apiKey: String?,
        eurToJpyRate: Double,
        currentPlan: PortableTravelPlanBundle? = null,
        modelId: String? = null,
        isEs: Boolean = true
    ): AiTravelAssistantResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cleanPrompt = prompt.trim()
        val nowFormatted = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        var cloudError: String? = null
        if (!apiKey.isNullOrBlank()) {
            val (cloudAnswer, err) = queryGeminiCloudApi(cleanPrompt, apiKey, eurToJpyRate, currentPlan, modelId, isEs)
            if (cloudAnswer != null && cloudAnswer.isNotBlank()) {
                return@withContext AiTravelAssistantResult(
                    answer = cloudAnswer,
                    source = AssistantSource.GEMINI_CLOUD,
                    timestampFormatted = nowFormatted
                )
            }
            cloudError = err
        }

        val offlineAnswer = generateSmartTravelAnswer(cleanPrompt, eurToJpyRate, currentPlan)
        AiTravelAssistantResult(
            answer = offlineAnswer,
            source = AssistantSource.OFFLINE_EXPERT,
            timestampFormatted = nowFormatted,
            cloudErrorDetail = cloudError
        )
    }

    private fun queryGeminiCloudApi(
        prompt: String,
        apiKey: String,
        eurToJpyRate: Double,
        currentPlan: PortableTravelPlanBundle?,
        modelId: String? = null,
        isEs: Boolean = true
    ): Pair<String?, String?> {
        val resolvedModel = if (modelId.isNullOrBlank()) GeminiApiKeyHelper.DEFAULT_MODEL_ID else modelId.trim()
        val prefix = apiKey.trim().take(6)
        return try {
            val endpointUrl = GeminiApiKeyHelper.buildGenerateContentUrl(apiKey, resolvedModel)
            val url = java.net.URL(endpointUrl)
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 12000
                readTimeout = 30000
                doOutput = true
            }

            val safeRate = if (eurToJpyRate > 0.0) eurToJpyRate else 164.80
            val stopsCount = currentPlan?.itineraryStops?.size ?: 0
            val systemContext = "Eres el Asistente IA de Viaje para Japón en la aplicación Japan Travel Tools. Responde con tono amable, experto y muy estructurado con viñetas y emojis. Incluye caracteres japoneses (Kanji/Kana) y fonética Romaji cuando menciones lugares, trenes o platos. Convierte precios aproximados en JPY a EUR según la tasa 1 EUR = ${String.format(Locale.US, "%.2f", safeRate)} JPY. El usuario tiene un plan de viaje con $stopsCount paradas. Responde en español."

            val escapedPrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
            val escapedSystem = systemContext.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")

            val jsonPayload = """{"system_instruction":{"parts":[{"text":"$escapedSystem"}]},"contents":[{"parts":[{"text":"$escapedPrompt"}]}],"generationConfig":{"temperature":0.4,"maxOutputTokens":1024}}"""

            connection.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val text = try {
                    val root = org.json.JSONObject(responseText)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    parts?.optJSONObject(0)?.optString("text")?.takeIf { it.isNotBlank() }
                } catch (_: Exception) {
                    null
                } ?: run {
                    val textPattern = """"text"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex()
                    val match = textPattern.find(responseText)
                    match?.groupValues?.get(1)?.replace("\\n", "\n")?.replace("\\\"", "\"")?.replace("\\\\", "\\")
                }
                text to null
            } else {
                val errorBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                val errorMsg = GeminiApiKeyHelper.evaluateHttpError(responseCode, errorBody, prefix, isEs, resolvedModel)
                null to errorMsg
            }
        } catch (e: java.net.SocketTimeoutException) {
            val msg = if (isEs) {
                "⏱️ Tiempo de espera agotado (Timeout): El modelo tardó más de 30 segundos en responder. Se recomienda seleccionar «Gemini Flash-Lite», que responde en menos de 1 segundo."
            } else {
                "⏱️ Request Timeout: Model took more than 30 seconds to respond. Select «Gemini Flash-Lite» for sub-second responses."
            }
            null to msg
        } catch (e: Exception) {
            val msg = if (isEs) {
                "📶 Error de conexión con Gemini Cloud (${e.localizedMessage ?: "timeout"})."
            } else {
                "📶 Gemini Cloud connection error (${e.localizedMessage ?: "timeout"})."
            }
            null to msg
        }
    }

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

            lower.contains("tax") || lower.contains("tax-free") || lower.contains("don quijote") || lower.contains("impuesto") || lower.contains("devolución") -> {
                val minTaxEur = String.format(Locale.US, "%.2f EUR", 5000.0 / safeRate)
                """
                    🛍️ Compras Tax-Free en Japón (Don Quijote, Bic Camera, Uniqlo):
                    • Requisito de compra: Gasto superior a ¥5,000 (~$minTaxEur) antes de impuestos en un mismo día y establecimiento.
                    • Pasaporte Físico Obligatorio: Es imprescindible presentar el pasaporte original en la caja con el sello de turista (no sirven fotos ni fotocopias).
                    • Productos consumibles (cosmética, snacks, medicinas): Se entregan en una bolsa precintada que NO se debe abrir hasta salir de Japón.
                    • Ahorro directo: Te descuentan el 10% del IVA japonés en el momento de pagar, sin esperas ni sellos en aduanas.
                """.trimIndent()
            }

            lower.contains("narita") || lower.contains("haneda") || lower.contains("aeropuerto") || lower.contains("kix") || lower.contains("skyliner") || lower.contains("haruka") -> {
                val nexEur = String.format(Locale.US, "%.2f EUR", 3070.0 / safeRate)
                val skyEur = String.format(Locale.US, "%.2f EUR", 2570.0 / safeRate)
                """
                    ✈️ Traslados desde los Aeropuertos:
                    • Aeropuerto de Narita (NRT) ➔ Tokio:
                      1. JR Narita Express (N'EX): 55 min directo a Tokyo Station / Shinjuku (~¥3,070 / ~$nexEur).
                      2. Keisei Skyliner: 36 min directo a Ueno / Nippori (~¥2,570 / ~$skyEur).
                    • Aeropuerto de Haneda (HND) ➔ Tokio:
                      1. Tokyo Monorail: 13 min a Hamamatsucho Station (conecta con JR Yamanote Line).
                      2. Línea Keikyu Airport: 11 min a Shinagawa Station.
                    • Aeropuerto de Kansai (KIX) ➔ Kioto / Osaka:
                      1. Tren JR Haruka: 75 min directo a Kioto Station / 45 min a Shin-Osaka.
                      2. Nankai Rapi:t: 34 min directo a Namba (Osaka).
                """.trimIndent()
            }

            lower.contains("esim") || lower.contains("sim") || lower.contains("internet") || lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("datos") -> {
                """
                    📶 Conexión a Internet en Japón (eSIM vs Pocket Wi-Fi):
                    • eSIM (Opción más recomendada): Muy cómoda para móviles compatibles (Pixel, iPhone, Galaxy). Se instala antes del vuelo mediante código QR (proveedores como Airalo, Ubigi o Holafly). Mantienes tu WhatsApp habitual.
                    • Pocket Wi-Fi (Router portátil): Ideal si viajas en pareja o familia para conectar varios dispositivos a la vez. Se recoge en el mostrador del aeropuerto (Narita/Haneda/KIX) y se devuelve en un buzón postal con el sobre prepagado incluido.
                    • Tarjeta SIM física: Si tu teléfono no soporta eSIM, puedes comprar una SIM de datos en las máquinas automáticas o mostradores de llegada del aeropuerto.
                """.trimIndent()
            }

            lower.contains("propina") || lower.contains("tip") || lower.contains("modales") || lower.contains("educación") || lower.contains("normas") || lower.contains("costumbre") -> {
                """
                    🚫 Propinas y Modales en Japón:
                    • Cero Propinas: En Japón NUNCA se deja propina. Dejar dinero extra en la mesa o barra es considerado de mala educación o una confusión; el camarero correrá detrás de ti para devolverte el cambio.
                    • No comer caminando: Se come o bebe de pie junto a la tienda o máquina expendedora donde compraste, no andando por la calle.
                    • Silencio en transporte público: Pon el teléfono en modo silencio ("Manner Mode"). Está mal visto hablar por teléfono o con voz alta en trenes y metros.
                    • Pago en caja: Deposita el dinero o la tarjeta en la pequeña bandeja (Torii) del mostrador, no directamente en la mano del cajero.
                """.trimIndent()
            }

            lower.contains("onsen") || lower.contains("tatuaje") || lower.contains("baño termal") || lower.contains("termas") || lower.contains("kashikiri") -> {
                """
                    ♨️ Normas en los Onsen (Baños Termales) y Tatuajes:
                    • Desnudez total: Se entra completamente desnudo, sin bañador. Hombres y mujeres se bañan en zonas separadas.
                    • Ducha previa obligatoria: Hay que lavarse y enjabonarse a conciencia en las banquetas individuales con ducha antes de meterse al agua termal.
                    • Toalla pequeña: No debe tocar nunca el agua de la poza termal; déjala doblada sobre tu cabeza o en el borde.
                    • Política de Tatuajes: Muchos onsen tradicionales prohíben la entrada a personas con tatuajes. Busca baños etiquetados como "Tattoo-friendly", cúbrelos con parches adhesivos color piel o reserva un baño privado familiar (Kashikiri Onsen).
                """.trimIndent()
            }

            lower.contains("jr pass") || lower.contains("japan rail pass") || lower.contains("merece la pena") || lower.contains("rentable") -> {
                """
                    🚅 ¿Merece la pena el JR Pass?:
                    • Tras la subida de precios de octubre de 2023 (+65%), el JR Pass nacional general casi NUNCA es rentable para un itinerario estándar (Tokio - Kioto - Osaka).
                    • Alternativa económica: Es mucho más barato comprar los billetes sueltos de Shinkansen (Tokio ➔ Kioto son unos ¥14,170 en SmartEX o taquilla) y usar la tarjeta Suica/Pasmo para los metros y trenes urbanos.
                    • Pases Regionales: Si vas a moverte mucho por Kansai, Hiroshima o los Alpes japoneses, pases como el JR Kansai-Hiroshima Area Pass sí siguen saliendo muy a cuenta.
                """.trimIndent()
            }

            lower.contains("enchufe") || lower.contains("voltaje") || lower.contains("clavija") || lower.contains("adaptador") || lower.contains("electricidad") -> {
                """
                    🔌 Enchufes y Electricidad en Japón:
                    • Tipo de Clavija: Clavijas de tipo A (dos patillas planas paralelas, idéntico al estándar de EE.UU.). Necesitarás un adaptador universal sencillo.
                    • Voltaje: En Japón el voltaje es de 100 V a 50/60 Hz.
                    • Compatibilidad: Los cargadores de móviles, portátiles, tablets y cámaras son bivoltaje (100-240 V) y funcionan perfectamente solo con el adaptador físico.
                    • Precaución: Aparatos de alta potencia sin bivoltaje (secadores o planchas de pelo europeos de 220-240 V) no calentarán adecuadamente; es mejor usar los del hotel.
                """.trimIndent()
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

            lower.contains("hakone") || lower.contains("fuji") || lower.contains("kawaguchiko") -> {
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
                    • Maletas en Shinkansen: Si la suma de alto+ancho+profundidad supera los 160 cm, es obligatorio reservar asiento especial en la última fila con espacio para maleta sin recargo.
                """.trimIndent()
            }

            lower.contains("tax") || lower.contains("tax-free") || lower.contains("don quijote") || lower.contains("impuesto") || lower.contains("devolución") -> {
                val minTaxEur = String.format(Locale.US, "%.2f EUR", 5000.0 / safeRate)
                """
                    🛍️ Compras Tax-Free en Japón (Don Quijote, Bic Camera, Uniqlo):
                    • Requisito de compra: Gasto superior a ¥5,000 (~$minTaxEur) antes de impuestos en un mismo día y establecimiento.
                    • Pasaporte Físico Obligatorio: Es imprescindible presentar el pasaporte original en la caja con el sello de turista (no sirven fotos ni fotocopias).
                    • Productos consumibles (cosmética, snacks, medicinas): Se entregan en una bolsa precintada que NO se debe abrir hasta salir de Japón.
                    • Ahorro directo: Te descuentan el 10% del IVA japonés en el momento de pagar, sin esperas ni sellos en aduanas.
                """.trimIndent()
            }

            lower.contains("esim") || lower.contains("sim") || lower.contains("internet") || lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("datos") -> {
                """
                    📶 Conexión a Internet en Japón (eSIM vs Pocket Wi-Fi):
                    • eSIM (Opción más recomendada): Muy cómoda para móviles compatibles (Pixel, iPhone, Galaxy). Se instala antes del vuelo mediante código QR (proveedores como Airalo, Ubigi o Holafly). Mantienes tu WhatsApp habitual.
                    • Pocket Wi-Fi (Router portátil): Ideal si viajas en pareja o familia para conectar varios dispositivos a la vez. Se recoge en el mostrador del aeropuerto (Narita/Haneda/KIX) y se devuelve en un buzón postal con el sobre prepagado incluido.
                    • Tarjeta SIM física: Si tu teléfono no soporta eSIM, puedes comprar una SIM de datos en las máquinas automáticas o mostradores de llegada del aeropuerto.
                """.trimIndent()
            }

            lower.contains("propina") || lower.contains("tip") || lower.contains("modales") || lower.contains("educación") || lower.contains("normas") || lower.contains("costumbre") -> {
                """
                    🚫 Propinas y Modales en Japón:
                    • Cero Propinas: En Japón NUNCA se deja propina. Dejar dinero extra en la mesa o barra es considerado de mala educación o una confusión; el camarero correrá detrás de ti para devolverte el cambio.
                    • No comer caminando: Se come o bebe de pie junto a la tienda o máquina expendedora donde compraste, no andando por la calle.
                    • Silencio en transporte público: Pon el teléfono en modo silencio ("Manner Mode"). Está mal visto hablar por teléfono o con voz alta en trenes y metros.
                    • Pago en caja: Deposita el dinero o la tarjeta en la pequeña bandeja (Torii) del mostrador, no directamente en la mano del cajero.
                """.trimIndent()
            }

            lower.contains("onsen") || lower.contains("tatuaje") || lower.contains("baño termal") || lower.contains("termas") || lower.contains("kashikiri") -> {
                """
                    ♨️ Normas en los Onsen (Baños Termales) y Tatuajes:
                    • Desnudez total: Se entra completamente desnudo, sin bañador. Hombres y mujeres se bañan en zonas separadas.
                    • Ducha previa obligatoria: Hay que lavarse y enjabonarse a conciencia en las banquetas individuales con ducha antes de meterse al agua termal.
                    • Toalla pequeña: No debe tocar nunca el agua de la poza termal; déjala doblada sobre tu cabeza o en el borde.
                    • Política de Tatuajes: Muchos onsen tradicionales prohíben la entrada a personas con tatuajes. Busca baños etiquetados como "Tattoo-friendly", cúbrelos con parches adhesivos color piel o reserva un baño privado familiar (Kashikiri Onsen).
                """.trimIndent()
            }

            lower.contains("jr pass") || lower.contains("japan rail pass") || lower.contains("merece la pena") || lower.contains("rentable") -> {
                """
                    🚅 ¿Merece la pena el JR Pass?:
                    • Tras la subida de precios de octubre de 2023 (+65%), el JR Pass nacional general casi NUNCA es rentable para un itinerario estándar (Tokio - Kioto - Osaka).
                    • Alternativa económica: Es mucho más barato comprar los billetes sueltos de Shinkansen (Tokio ➔ Kioto son unos ¥14,170 en SmartEX o taquilla) y usar la tarjeta Suica/Pasmo para los metros y trenes urbanos.
                    • Pases Regionales: Si vas a moverte mucho por Kansai, Hiroshima o los Alpes japoneses, pases como el JR Kansai-Hiroshima Area Pass sí siguen saliendo muy a cuenta.
                """.trimIndent()
            }

            lower.contains("enchufe") || lower.contains("voltaje") || lower.contains("clavija") || lower.contains("adaptador") || lower.contains("electricidad") -> {
                """
                    🔌 Enchufes y Electricidad en Japón:
                    • Tipo de Clavija: Clavijas de tipo A (dos patillas planas paralelas, idéntico al estándar de EE.UU.). Necesitarás un adaptador universal sencillo.
                    • Voltaje: En Japón el voltaje es de 100 V a 50/60 Hz.
                    • Compatibilidad: Los cargadores de móviles, portátiles, tablets y cámaras son bivoltaje (100-240 V) y funcionan perfectamente solo con el adaptador físico.
                    • Precaución: Aparatos de alta potencia sin bivoltaje (secadores o planchas de pelo europeos de 220-240 V) no calentarán adecuadamente; es mejor usar los del hotel.
                """.trimIndent()
            }

            lower.contains("narita") || lower.contains("haneda") || lower.contains("aeropuerto") || lower.contains("kix") || lower.contains("skyliner") || lower.contains("haruka") -> {
                val nexEur = String.format(Locale.US, "%.2f EUR", 3070.0 / safeRate)
                val skyEur = String.format(Locale.US, "%.2f EUR", 2570.0 / safeRate)
                """
                    ✈️ Traslados desde los Aeropuertos:
                    • Aeropuerto de Narita (NRT) ➔ Tokio:
                      1. JR Narita Express (N'EX): 55 min directo a Tokyo Station / Shinjuku (~¥3,070 / ~$nexEur).
                      2. Keisei Skyliner: 36 min directo a Ueno / Nippori (~¥2,570 / ~$skyEur).
                    • Aeropuerto de Haneda (HND) ➔ Tokio:
                      1. Tokyo Monorail: 13 min a Hamamatsucho Station (conecta con JR Yamanote Line).
                      2. Línea Keikyu Airport: 11 min a Shinagawa Station.
                    • Aeropuerto de Kansai (KIX) ➔ Kioto / Osaka:
                      1. Tren JR Haruka: 75 min directo a Kioto Station / 45 min a Shin-Osaka.
                      2. Nankai Rapi:t: 34 min directo a Namba (Osaka).
                """.trimIndent()
            }

            lower.contains("tarjeta") || lower.contains("crédito") || lower.contains("débito") || lower.contains("revolut") || lower.contains("pagar") -> {
                """
                    💳 Tarjetas y Pagos en Japón:
                    • Tarjetas internacionales: Visa y Mastercard (como Revolut, N26 o tarjetas bancarias con chip) se aceptan en hoteles, grandes almacenes, tiendas konbini (7-Eleven, Lawson) y cadenas de restaurantes.
                    • Necesidad de efectivo: Templos, santuarios, puestos callejeros de comida, máquinas de recarga de Suica y pequeños bares ramen solo aceptan dinero en metálico (efectivo en yenes).
                    • Recomendación: Lleva siempre entre ¥10,000 y ¥20,000 en la cartera y saca yenes en los cajeros Seven Bank (7-Eleven) eligiendo cobro en JPY.
                """.trimIndent()
            }

            lower.contains("ropa") || lower.contains("tiempo") || lower.contains("clima") || lower.contains("otoño") || lower.contains("primavera") || lower.contains("frío") -> {
                """
                    🧥 Qué Ropa Llevar y Clima por Estación:
                    • Otoño (Octubre - Noviembre): Clima templado y agradable (14°C a 20°C). Viste por capas: camisetas o camisas de manga larga, jersey fino y una chaqueta para la noche. Perfecto para ver el follaje otoñal (Momiji).
                    • Primavera (Marzo - Mayo): Fresco por la mañana (10°C) y suave al mediodía (18°C a 22°C). Lleva cazadora ligera y calzado muy cómodo para caminar por los templos y cerezos en flor (Sakura).
                    • Calzado: Imprescindible calzado fácil de poner y quitar (slip-on o sin cordones complicados), ya que te descalzarás a menudo en templos, ryokans y restaurantes tradicionales.
                """.trimIndent()
            }

            lower.contains("akihabara") || lower.contains("anime") || lower.contains("manga") || lower.contains("figura") || lower.contains("souvenir") -> {
                """
                    🎮 Akihabara, Anime y Souvenirs en Tokio:
                    • Figuras y Coleccionismo: Tiendas clave como Radio Kaikan (justo en la salida Electric Town), Mandarake Complex (8 plantas de vintage), AmiAmi y Kotobukiya.
                    • Gashapon y Recreativas: Gashapon Department Store en Ikebukuro o Akihabara con miles de máquinas de cápsulas, y salones de recreativas Taito Station y GiGO.
                    • Electrónica y Gadgets: Yodobashi Camera Multimedia Akiba (9 plantas de tecnología, fotografía y relojes con Tax-Free).
                    • Manga y Merchandising: Animate Ikebukuro (la tienda de anime más grande del mundo) o Nakano Broadway para coleccionismo retro.
                """.trimIndent()
            }

            lower.contains("emergencia") || lower.contains("terremoto") || lower.contains("hospital") || lower.contains("110") || lower.contains("119") || lower.contains("seguro") -> {
                """
                    🆘 Teléfonos de Emergencia y Terremotos:
                    • 110: Policía (asistencia de seguridad o pérdida de objetos).
                    • 119: Ambulancia y Bomberos (urgencias médicas).
                    • Línea de Asistencia al Turista JNTO (24/7 en español/inglés): 050-3816-2720.
                    • Terremotos: Japón cuenta con la arquitectura antisísmica más avanzada del mundo. Si suena la alarma J-Alert en tu teléfono, mantén la calma, protégete la cabeza bajo una mesa firme, aléjate de cristales y espera las indicaciones del personal.
                """.trimIndent()
            }

            else -> {
                val stopsCount = currentPlan?.itineraryStops?.size ?: 0
                val daysCount = currentPlan?.itineraryStops?.map { it.dayNumber }?.distinct()?.size ?: 0
                val segmentsCount = currentPlan?.transportSegments?.size ?: 0
                buildString {
                    appendLine("✨ Consulta de Viaje: «$cleanPrompt»")
                    appendLine("• Consejos destacados para tu duda:")
                    appendLine("  - Para transportes urbanos, recuerda recargar tu tarjeta IC (Suica/Pasmo) en efectivo.")
                    appendLine("  - En compras superiores a ¥5,000 en tiendas con distintivo, solicita la exención Tax-Free con tu pasaporte original.")
                    appendLine("  - Conversión de referencia: 1 EUR = ${String.format(Locale.US, "%.2f", safeRate)} JPY (¥1,000 ≈ ${String.format(Locale.US, "%.2f EUR", 1000.0 / safeRate)}).")
                    if (stopsCount > 0 || segmentsCount > 0) {
                        appendLine("• Tu itinerario actual tiene $stopsCount paradas organizadas en $daysCount jornada(s).")
                    }
                    append("• Pregúntame sobre cualquier ciudad (Tokio, Kioto, Osaka, Nara), transportes (Shinkansen, metro), comida, etiqueta o normas Onsen.")
                }
            }
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
