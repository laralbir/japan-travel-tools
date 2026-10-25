package com.laralbir.japantraveltools.feature.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAndTranslatorUseCaseTest {

    @Test
    fun `OfflinePhraseTranslator translates common Spanish and English travel phrases to Japanese with Romaji`() {
        val resultEs = OfflineTranslatorEngine.translateToJapanese("¿Dónde está el andén del Shinkansen?")
        assertTrue(resultEs.japaneseText.contains("新幹線"))
        assertTrue(resultEs.romaji.isNotBlank())

        val resultEn = OfflineTranslatorEngine.translateToJapanese("No shellfish or peanuts please")
        assertTrue(resultEn.japaneseText.isNotBlank())
        assertTrue(resultEn.romaji.isNotBlank())
    }

    @Test
    fun `GeminiMenuPriceConverter converts detected Yen prices in Japanese menu text to EUR using cached rate`() {
        val menuText = "Tonkotsu Ramen Special ¥1,200 / Gyoza (6 pcs) ¥450 / Matcha Parfait ¥800"
        val enriched = GeminiAssistantEngine.enrichMenuWithConvertedPrices(
            text = menuText,
            eurToJpyRate = 160.0
        )
        // 1200 JPY / 160 = 7.50 EUR
        assertTrue(enriched.contains("7.50 EUR") || enriched.contains("7,50 EUR"))
    }

    @Test
    fun `translateFromJapaneseSpeech translates spoken Japanese into Spanish and English audio text`() {
        val reply = OfflineTranslatorEngine.translateFromJapaneseSpeech("いらっしゃいませ。何名様ですか？")
        assertTrue(reply.spanishSpokenText.contains("personas", ignoreCase = true))
        assertTrue(reply.englishSpokenText.contains("people", ignoreCase = true) || reply.englishSpokenText.contains("many", ignoreCase = true))

        val platformReply = OfflineTranslatorEngine.translateFromJapaneseSpeech("新幹線のホームは16番線です")
        assertTrue(platformReply.spanishSpokenText.contains("andén", ignoreCase = true) || platformReply.spanishSpokenText.contains("16"))
    }

    @Test
    fun `isJapaneseText correctly identifies Japanese scripts versus Latin scripts`() {
        assertTrue(OfflineTranslatorEngine.isJapaneseText("こんにちは"))
        assertTrue(OfflineTranslatorEngine.isJapaneseText("新幹線"))
        assertTrue(OfflineTranslatorEngine.isJapaneseText("ラーメン"))
        assertTrue(OfflineTranslatorEngine.isJapaneseText("1500円"))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText("Hola, ¿dónde está el baño?"))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText("Where is the train station?"))
    }

    @Test
    fun `translateFromJapaneseSpeech translates common Japanese travel phrases directly into Spanish without repeating Japanese`() {
        val konnichiwa = OfflineTranslatorEngine.translateFromJapaneseSpeech("こんにちは")
        assertTrue(konnichiwa.spanishSpokenText.contains("hola", ignoreCase = true) || konnichiwa.spanishSpokenText.contains("buenos", ignoreCase = true))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(konnichiwa.spanishSpokenText))

        val arigato = OfflineTranslatorEngine.translateFromJapaneseSpeech("ありがとうございます")
        assertTrue(arigato.spanishSpokenText.contains("gracias", ignoreCase = true))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(arigato.spanishSpokenText))

        val sumimasen = OfflineTranslatorEngine.translateFromJapaneseSpeech("すみません")
        assertTrue(sumimasen.spanishSpokenText.contains("disculpe", ignoreCase = true) || sumimasen.spanishSpokenText.contains("perdón", ignoreCase = true))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(sumimasen.spanishSpokenText))

        val daijoubu = OfflineTranslatorEngine.translateFromJapaneseSpeech("大丈夫です")
        assertTrue(daijoubu.spanishSpokenText.contains("bien", ignoreCase = true) || daijoubu.spanishSpokenText.contains("problema", ignoreCase = true))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(daijoubu.spanishSpokenText))

        val wakarimashita = OfflineTranslatorEngine.translateFromJapaneseSpeech("わかりました")
        assertTrue(wakarimashita.spanishSpokenText.contains("entendido", ignoreCase = true) || wakarimashita.spanishSpokenText.contains("acuerdo", ignoreCase = true))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(wakarimashita.spanishSpokenText))

        val genkin = OfflineTranslatorEngine.translateFromJapaneseSpeech("現金のみです")
        assertTrue(genkin.spanishSpokenText.contains("efectivo", ignoreCase = true))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(genkin.spanishSpokenText))

        val yenPrice = OfflineTranslatorEngine.translateFromJapaneseSpeech("1500円になります")
        assertTrue(yenPrice.spanishSpokenText.contains("1500") || yenPrice.spanishSpokenText.contains("yenes", ignoreCase = true))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(yenPrice.spanishSpokenText))

        val cardUnavailable = OfflineTranslatorEngine.translateFromJapaneseSpeech("カードは使えません")
        assertTrue(cardUnavailable.spanishSpokenText.contains("tarjeta", ignoreCase = true))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(cardUnavailable.spanishSpokenText))

        val waitMoment = OfflineTranslatorEngine.translateFromJapaneseSpeech("少々お待ちください")
        assertTrue(waitMoment.spanishSpokenText.contains("esper", ignoreCase = true) || waitMoment.spanishSpokenText.contains("momento", ignoreCase = true))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(waitMoment.spanishSpokenText))

        val restroom = OfflineTranslatorEngine.translateFromJapaneseSpeech("トイレはまっすぐ行って右です")
        assertTrue(restroom.spanishSpokenText.contains("baño", ignoreCase = true))
        assertTrue(restroom.spanishSpokenText.contains("derecha", ignoreCase = true) || restroom.spanishSpokenText.contains("recto", ignoreCase = true))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(restroom.spanishSpokenText))
    }

    @Test
    fun `translateFromJapaneseSpeech fallback strictly never leaks Japanese characters into Spanish or English audio`() {
        val unknown = OfflineTranslatorEngine.translateFromJapaneseSpeech("明日は大雨になる可能性があります")
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(unknown.spanishSpokenText))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText(unknown.englishSpokenText))
        assertTrue(unknown.spanishSpokenText.isNotBlank())
        assertTrue(unknown.englishSpokenText.isNotBlank())
    }

    @Test
    fun `translateFromJapaneseSpeech faithfully translates literal meaning and never invents conversational replies or bot messages`() {
        val sumimasen = OfflineTranslatorEngine.translateFromJapaneseSpeech("すみません")
        assertEquals("Disculpe / Perdón.", sumimasen.spanishSpokenText)
        assertTrue(!sumimasen.spanishSpokenText.contains("ayudar"))

        val water = OfflineTranslatorEngine.translateFromJapaneseSpeech("お水")
        assertEquals("Agua.", water.spanishSpokenText)
        assertTrue(!water.spanishSpokenText.contains("cortesía"))

        val checkIn = OfflineTranslatorEngine.translateFromJapaneseSpeech("チェックイン")
        assertEquals("Check-in / Registro.", checkIn.spanishSpokenText)
        assertTrue(!checkIn.spanishSpokenText.contains("habitación"))

        val wakarimashita = OfflineTranslatorEngine.translateFromJapaneseSpeech("わかりました")
        assertEquals("Entendido / De acuerdo.", wakarimashita.spanishSpokenText)
        assertTrue(!wakarimashita.spanishSpokenText.contains("preparo"))

        val howMuch = OfflineTranslatorEngine.translateFromJapaneseSpeech("これはいくらですか？")
        assertTrue(howMuch.spanishSpokenText.contains("cuánto cuesta", ignoreCase = true) || howMuch.spanishSpokenText.contains("cuánto es", ignoreCase = true))
        assertTrue(!howMuch.spanishSpokenText.contains("interlocutor"))

        val whereRestroom = OfflineTranslatorEngine.translateFromJapaneseSpeech("トイレはどこですか？")
        assertTrue(whereRestroom.spanishSpokenText.contains("dónde está el baño", ignoreCase = true))
        assertTrue(!whereRestroom.spanishSpokenText.contains("interlocutor"))

        val yenPrice = OfflineTranslatorEngine.translateFromJapaneseSpeech("1500円")
        assertTrue(yenPrice.spanishSpokenText.contains("1500 yenes") || yenPrice.spanishSpokenText.contains("1.500 yenes"))
        assertTrue(!yenPrice.spanishSpokenText.contains("total a pagar"))
    }

    @Test
    fun `OnlineTranslatorClient translates Japanese to Spanish and Spanish to Japanese`() {
        val es = OnlineTranslatorClient.translate("これはいくらですか？", "ja", "es")
        if (es != null) {
            assertTrue(es.contains("cuánto", ignoreCase = true) || es.contains("cuanto", ignoreCase = true))
        }

        val ja = OnlineTranslatorClient.translate("¿Dónde está el baño?", "es", "ja")
        if (ja != null) {
            assertTrue(ja.contains("トイレ") || ja.contains("お手洗い"))
        }
    }

    @Test
    fun `translateToJapanese never adds extra unsaid words, questions, compliments or boilerplate`() {
        val hola = OfflineTranslatorEngine.translateToJapanese("Hola")
        assertTrue(hola.japaneseText.contains("こんにちは"))
        assertTrue(!hola.japaneseText.contains("美味しかったです"))
        assertTrue(!hola.japaneseText.contains("ありがとうございます"))

        val gracias = OfflineTranslatorEngine.translateToJapanese("Gracias")
        assertTrue(gracias.japaneseText.contains("ありがとう"))
        assertTrue(!gracias.japaneseText.contains("美味しかったです"))

        val bill = OfflineTranslatorEngine.translateToJapanese("La cuenta por favor")
        assertTrue(bill.japaneseText.contains("お会計") || bill.japaneseText.contains("会計"))
        assertTrue(!bill.japaneseText.contains("Suica"))
        assertTrue(!bill.japaneseText.contains("カード"))

        val water = OfflineTranslatorEngine.translateToJapanese("Agua por favor")
        assertTrue(water.japaneseText.contains("お水") || water.japaneseText.contains("水"))
        assertTrue(!water.japaneseText.contains("2つ"))
        assertTrue(!water.japaneseText.contains("二つ"))

        val veg = OfflineTranslatorEngine.translateToJapanese("Soy vegetariano")
        assertTrue(veg.japaneseText.contains("ベジタリアン"))
        assertTrue(!veg.japaneseText.contains("肉と魚は食べられません"))

        val taxFree = OfflineTranslatorEngine.translateToJapanese("¿Hacen tax free?")
        assertTrue(taxFree.japaneseText.contains("免税"))
        assertTrue(!taxFree.japaneseText.contains("パスポートがあります"))

        val custom = OfflineTranslatorEngine.translateToJapanese("Buenas noches")
        assertTrue(!custom.japaneseText.contains("翻訳アプリを使っています"))
    }

    @Test
    fun `translateOcrJapaneseText translates multi-line Japanese restaurant menu with prices and dish names`() {
        val menuOcr = """
            特製ラーメン 1000円
            焼き餃子 450円
            生ビール 550円
        """.trimIndent()

        val result = OfflineTranslatorEngine.translateOcrJapaneseText(menuOcr, isEs = true)
        assertTrue(result.hasJapanese)
        assertEquals(3, result.lines.size)
        assertTrue(result.lines[0].translatedText.contains("ramen", ignoreCase = true))
        assertTrue(result.lines[0].translatedText.contains("1000") || result.lines[0].translatedText.contains("1.000"))
        assertTrue(result.lines[1].translatedText.contains("gyoza", ignoreCase = true) || result.lines[1].translatedText.contains("empanadilla", ignoreCase = true))
        assertTrue(result.lines[2].translatedText.contains("cerveza", ignoreCase = true))
    }

    @Test
    fun `translateOcrJapaneseText translates Japanese signs for station, smoking rules, and restrooms`() {
        val signOcr = """
            新幹線 16番線
            禁煙席
            本日満席
            お手洗い
        """.trimIndent()

        val result = OfflineTranslatorEngine.translateOcrJapaneseText(signOcr, isEs = true)
        assertTrue(result.hasJapanese)
        assertEquals(4, result.lines.size)
        assertTrue(result.lines[0].translatedText.contains("shinkansen", ignoreCase = true) || result.lines[0].translatedText.contains("andén", ignoreCase = true))
        assertTrue(result.lines[1].translatedText.contains("fumador", ignoreCase = true))
        assertTrue(result.lines[2].translatedText.contains("completo", ignoreCase = true) || result.lines[2].translatedText.contains("lleno", ignoreCase = true))
        assertTrue(result.lines[3].translatedText.contains("baño", ignoreCase = true) || result.lines[3].translatedText.contains("aseo", ignoreCase = true))
    }

    @Test
    fun `translateOcrJapaneseText handles text with no Japanese gracefully`() {
        val noJapanese = "Entrance Only Please Keep Left"
        val result = OfflineTranslatorEngine.translateOcrJapaneseText(noJapanese, isEs = true)
        assertTrue(!result.hasJapanese)
        assertTrue(result.fullTranslation.contains("japonés", ignoreCase = true) || result.fullTranslation.contains("detectó", ignoreCase = true))
    }

    @Test
    fun `OfflineTranslatorEngine translates everyday questions without leaking raw Spanish into Japanese TTS`() {
        val restroom = OfflineTranslatorEngine.translateToJapanese("¿Dónde está el baño por favor?")
        assertTrue(restroom.japaneseText.contains("トイレ"))
        assertTrue(!restroom.japaneseText.contains("baño", ignoreCase = true))

        val price = OfflineTranslatorEngine.translateToJapanese("¿Cuánto cuesta esto?")
        assertTrue(price.japaneseText.contains("いくら"))

        val water = OfflineTranslatorEngine.translateToJapanese("Agua para dos personas por favor")
        assertTrue(water.japaneseText.contains("水") || water.japaneseText.contains("2名") || water.japaneseText.contains("二人"))
    }

    @Test
    fun `GeminiAssistantEngine answers specific topics accurately and converts inline yen amounts`() {
        val suicaAnswer = GeminiAssistantEngine.generateSmartTravelAnswer("¿Cómo funciona la Suica y el tren?", 160.0)
        assertTrue(suicaAnswer.contains("Suica", ignoreCase = true))
        assertTrue(suicaAnswer.contains("IC", ignoreCase = true))

        val naraAnswer = GeminiAssistantEngine.generateSmartTravelAnswer("¿Qué ver en Nara en medio día?", 160.0)
        assertTrue(naraAnswer.contains("Todai-ji", ignoreCase = true) || naraAnswer.contains("ciervos", ignoreCase = true))

        val priceQuery = GeminiAssistantEngine.generateSmartTravelAnswer("¿Cuánto son 3200 yenes en euros?", 160.0)
        assertTrue(priceQuery.contains("20.00 EUR") || priceQuery.contains("20,00 EUR"))
    }

    @Test
    fun `GeminiAssistantEngine answers diverse everyday travel queries with rich offline domain knowledge`() {
        val taxFree = GeminiAssistantEngine.generateSmartTravelAnswer("¿Cómo funciona el tax free en Don Quijote?", 160.0)
        assertTrue(taxFree.contains("Tax-Free", ignoreCase = true))
        assertTrue(taxFree.contains("pasaporte", ignoreCase = true))

        val internet = GeminiAssistantEngine.generateSmartTravelAnswer("¿Cómo tener internet con eSIM o Pocket Wifi en Japón?", 160.0)
        assertTrue(internet.contains("eSIM", ignoreCase = true))
        assertTrue(internet.contains("Pocket Wi-Fi", ignoreCase = true) || internet.contains("wifi", ignoreCase = true))

        val tipping = GeminiAssistantEngine.generateSmartTravelAnswer("¿Hay que dejar propina en restaurantes de Japón?", 160.0)
        assertTrue(tipping.contains("propina", ignoreCase = true))

        val onsen = GeminiAssistantEngine.generateSmartTravelAnswer("¿Puedo entrar a un onsen si tengo tatuajes?", 160.0)
        assertTrue(onsen.contains("onsen", ignoreCase = true))
        assertTrue(onsen.contains("tatuaje", ignoreCase = true))

        val jrPass = GeminiAssistantEngine.generateSmartTravelAnswer("¿Merece la pena el JR Pass?", 160.0)
        assertTrue(jrPass.contains("JR Pass", ignoreCase = true))

        val plugs = GeminiAssistantEngine.generateSmartTravelAnswer("¿Qué enchufes y voltaje se usan en Japón?", 160.0)
        assertTrue(plugs.contains("100V", ignoreCase = true) || plugs.contains("tipo A", ignoreCase = true))

        val airport = GeminiAssistantEngine.generateSmartTravelAnswer("¿Cómo ir del aeropuerto de Narita a Tokio centro?", 160.0)
        assertTrue(airport.contains("Narita Express", ignoreCase = true) || airport.contains("Skyliner", ignoreCase = true))
    }

    @Test
    fun `queryAssistant returns AiTravelAssistantResult with OFFLINE_EXPERT when no api key is configured`() = kotlinx.coroutines.test.runTest {
        val result = GeminiAssistantEngine.queryAssistant(
            prompt = "¿Cómo funciona la tarjeta Suica?",
            apiKey = null,
            eurToJpyRate = 160.0
        )
        assertEquals(AssistantSource.OFFLINE_EXPERT, result.source)
        assertTrue(result.answer.contains("Suica", ignoreCase = true))
        assertTrue(result.timestampFormatted.isNotBlank())
    }
}

