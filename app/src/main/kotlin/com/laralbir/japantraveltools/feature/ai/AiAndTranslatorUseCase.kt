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

        // 1. Precios y cantidades en Yenes (ej. 1500円, 2000 円, etc.)
        val yenRegex = """([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)\s*円""".toRegex()
        val yenMatch = yenRegex.find(clean)
        if (yenMatch != null) {
            val amount = yenMatch.groupValues[1]
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "$amount-en ni narimasu.",
                spanishSpokenText = "El total a pagar son $amount yenes.",
                englishSpokenText = "The total is $amount yen.",
                contextLabel = "PRECIO / YENES"
            )
        }

        // 2. Direcciones hacia baños / aseos
        if (clean.contains("トイレ") || clean.contains("お手洗い") || clean.contains("化粧室")) {
            val direction = when {
                clean.contains("右") -> "a mano derecha"
                clean.contains("左") -> "a mano izquierda"
                clean.contains("まっすぐ") || clean.contains("直進") -> "todo recto hacia adelante"
                clean.contains("あちら") || clean.contains("あそこ") || clean.contains("向こう") -> "en esa dirección, por allí"
                clean.contains("2階") -> "en la segunda planta"
                clean.contains("地下") -> "en la planta sótano"
                else -> "todo recto a mano derecha"
            }
            val dirEn = when {
                clean.contains("右") -> "on the right"
                clean.contains("左") -> "on the left"
                clean.contains("まっすぐ") || clean.contains("直進") -> "straight ahead"
                clean.contains("あちら") || clean.contains("あそこ") || clean.contains("向こう") -> "over there"
                clean.contains("2階") -> "on the second floor"
                clean.contains("地下") -> "in the basement"
                else -> "straight ahead on the right"
            }
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "O-tearai wa $direction desu.",
                spanishSpokenText = "El baño está $direction.",
                englishSpokenText = "The restroom is $dirEn.",
                contextLabel = "DIRECCIÓN / BAÑO"
            )
        }

        // 3. Saludos, cortesía y agradecimientos
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
            val extra = when {
                clean.contains("右") -> "El camino está a la derecha."
                clean.contains("左") -> "El camino está a la izquierda."
                clean.contains("まっすぐ") -> "Siga todo recto."
                clean.contains("わかりません") -> "Disculpe, no lo sé."
                else -> "Disculpe, ¿en qué puedo ayudarle?"
            }
            val extraEn = when {
                clean.contains("右") -> "The way is on the right."
                clean.contains("左") -> "The way is on the left."
                clean.contains("まっすぐ") -> "Go straight ahead."
                clean.contains("わかりません") -> "Excuse me, I don't know."
                else -> "Excuse me, how can I help you?"
            }
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Sumimasen.",
                spanishSpokenText = extra,
                englishSpokenText = extraEn,
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

        // 4. Confirmaciones, estados y respuestas cotidianas
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
                romaji = "Wakarimashita. Kashikomarimashita.",
                spanishSpokenText = "Entendido, de acuerdo, enseguida lo preparo.",
                englishSpokenText = "Understood, right away.",
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
                spanishSpokenText = "Por favor, espere un momento enseguida le atendemos.",
                englishSpokenText = "Please wait a moment, we will be right with you.",
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
                spanishSpokenText = "Lo sentimos, el local está completo en este momento.",
                englishSpokenText = "Sorry, the place is fully booked at this moment.",
                contextLabel = "LOCAL LLENO"
            )
        }
        if (clean.contains("ラストオーダー") || clean.contains("ラスト")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Rasuto ōdā desu.",
                spanishSpokenText = "Es la última ronda de pedidos de comida y bebida.",
                englishSpokenText = "It is the last call for orders.",
                contextLabel = "ÚLTIMO PEDIDO"
            )
        }
        if (clean.contains("水") || clean.contains("お冷") || clean.contains("お茶")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "O-mizu o dōzo.",
                spanishSpokenText = "Aquí tiene agua fresca de cortesía.",
                englishSpokenText = "Here is your complimentary cold water.",
                contextLabel = "AGUA / BEBIDA"
            )
        }

        // 5. Pagos y caja (efectivo, tarjeta, Suica)
        if (clean.contains("現金のみ") || clean.contains("現金だけ")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Genkin nomi desu.",
                spanishSpokenText = "Disculpe, solo aceptamos pago en efectivo.",
                englishSpokenText = "Sorry, cash only please.",
                contextLabel = "PAGO / EFECTIVO"
            )
        }
        if (clean.contains("カード") && (clean.contains("使えません") || clean.contains("ダメ") || clean.contains("不可") || clean.contains("無理"))) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Kādo wa tsukaemasen. Genkin nomi desu.",
                spanishSpokenText = "No aceptamos tarjeta de crédito, debe ser en efectivo.",
                englishSpokenText = "Credit cards are not accepted, cash only.",
                contextLabel = "SIN TARJETA"
            )
        }
        if (clean.contains("現金") || clean.contains("カード") || clean.contains("Suica") || clean.contains("会計") || clean.contains("お会計")) {
            return incomingJapaneseSamples[4].copy(japaneseHeardText = clean)
        }

        // 6. Konbini & tienda
        if (clean.contains("袋") || clean.contains("温め") || clean.contains("箸") || clean.contains("弁当")) {
            return incomingJapaneseSamples[2].copy(japaneseHeardText = clean)
        }
        if (clean.contains("売り切れ") || clean.contains("完売")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Mōshiwake arimasen, urikire desu.",
                spanishSpokenText = "Lo sentimos mucho, este artículo está agotado.",
                englishSpokenText = "We are sorry, this item is sold out.",
                contextLabel = "AGOTADO"
            )
        }

        // 7. Tax-Free & Pasaporte
        if (clean.contains("パスポート") || clean.contains("免税")) {
            return incomingJapaneseSamples[3].copy(japaneseHeardText = clean)
        }

        // 8. Restaurante y comensales
        if (clean.contains("何名") || clean.contains("いらっしゃい") || clean.contains("席") || clean.contains("予約")) {
            return incomingJapaneseSamples[0].copy(japaneseHeardText = clean)
        }

        // 9. Estaciones, trenes y Shinkansen
        if (clean.contains("ホーム") || clean.contains("番線") || clean.contains("新幹線") || clean.contains("電車") || clean.contains("切符")) {
            val platformNum = """([0-9]+)\s*番""".toRegex().find(clean)?.groupValues?.get(1)
            val trainText = if (platformNum != null) {
                "El tren sale del andén número $platformNum."
            } else {
                "El tren sale del andén indicado en las pantallas de la estación."
            }
            val trainTextEn = if (platformNum != null) {
                "The train departs from platform number $platformNum."
            } else {
                "The train departs from the platform shown on station displays."
            }
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Hōmu kara hassha shimasu.",
                spanishSpokenText = trainText,
                englishSpokenText = trainTextEn,
                contextLabel = "ESTACIÓN / TREN"
            )
        }

        // 10. Hoteles & Check-in
        if (clean.contains("チェックイン") || clean.contains("部屋") || clean.contains("朝食") || clean.contains("荷物")) {
            return incomingJapaneseSamples[0].copy(
                japaneseHeardText = clean,
                romaji = "Chekkuin no tetsuzuki o shimasu.",
                spanishSpokenText = "Vamos a gestionar el check-in de su habitación. Por favor, muestre su pasaporte.",
                englishSpokenText = "We will process your room check-in. Please show your passport.",
                contextLabel = "HOTEL / RECEPCIÓN"
            )
        }

        // 11. Direcciones generales
        if (clean.contains("右") || clean.contains("左") || clean.contains("まっすぐ") || clean.contains("あちら")) {
            val dir = when {
                clean.contains("右") -> "a la derecha"
                clean.contains("左") -> "a la izquierda"
                clean.contains("まっすぐ") -> "todo recto hacia adelante"
                else -> "hacia allí enfrente"
            }
            val dirEn = when {
                clean.contains("右") -> "on the right"
                clean.contains("左") -> "on the left"
                clean.contains("まっすぐ") -> "straight ahead"
                else -> "over there"
            }
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Achira no hōkō desu.",
                spanishSpokenText = "Debe dirigirse $dir.",
                englishSpokenText = "Please head $dirEn.",
                contextLabel = "INDICACIÓN DE RUTA"
            )
        }

        // 12. Reglas, calzado y fotografía
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
        if (clean.contains("禁煙")) {
            return JapaneseToTravelerVoiceResult(
                japaneseHeardText = clean,
                romaji = "Kin'en desu.",
                spanishSpokenText = "Área de no fumadores, prohibido fumar.",
                englishSpokenText = "Non-smoking area, smoking is prohibited.",
                contextLabel = "PROHIBIDO FUMAR"
            )
        }

        // 13. Fallback heurístico multi-término: NUNCA fugar caracteres japoneses al texto hablado
        val recognizedKeywords = mutableListOf<String>()
        val recognizedKeywordsEn = mutableListOf<String>()
        if (clean.contains("駅")) { recognizedKeywords.add("estación"); recognizedKeywordsEn.add("station") }
        if (clean.contains("ホテル")) { recognizedKeywords.add("hotel"); recognizedKeywordsEn.add("hotel") }
        if (clean.contains("空港")) { recognizedKeywords.add("aeropuerto"); recognizedKeywordsEn.add("airport") }
        if (clean.contains("バス")) { recognizedKeywords.add("autobús"); recognizedKeywordsEn.add("bus") }
        if (clean.contains("タクシー")) { recognizedKeywords.add("taxi"); recognizedKeywordsEn.add("taxi") }
        if (clean.contains("時間")) { recognizedKeywords.add("horario"); recognizedKeywordsEn.add("schedule") }
        if (clean.contains("雨")) { recognizedKeywords.add("lluvia"); recognizedKeywordsEn.add("rain") }
        if (clean.contains("薬")) { recognizedKeywords.add("medicamento/farmacia"); recognizedKeywordsEn.add("medicine/pharmacy") }

        val spanishSummary = if (recognizedKeywords.isNotEmpty()) {
            "El interlocutor ha indicado información sobre: ${recognizedKeywords.joinToString(", ")}. Por favor, pídele que hable despacio si necesitas más detalles."
        } else {
            "El interlocutor ha respondido en japonés. Por favor, pídele que hable despacio o te lo muestre en pantalla."
        }

        val englishSummary = if (recognizedKeywordsEn.isNotEmpty()) {
            "The speaker provided information regarding: ${recognizedKeywordsEn.joinToString(", ")}. Please ask them to speak slowly for more details."
        } else {
            "The speaker replied in Japanese. Please ask them to speak slowly or show the screen."
        }

        return JapaneseToTravelerVoiceResult(
            japaneseHeardText = clean,
            romaji = "Nihongo no kotae: $clean",
            spanishSpokenText = spanishSummary,
            englishSpokenText = englishSummary,
            contextLabel = "RESPUESTA JAPONESA"
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
