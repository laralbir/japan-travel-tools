package com.laralbir.japantraveltools.feature.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTranslatorEngineTest {

    @Test
    fun `translateToJapanese translates common travel questions accurately`() {
        // ¿Dónde está el baño?
        val restroom = OfflineTranslatorEngine.translateToJapanese("¿Dónde está el baño?")
        assertTrue(restroom.japaneseText.contains("トイレはどこですか？"))
        assertEquals("Toire wa doko desu ka?", restroom.romaji)
        assertEquals("BAÑO / ASEO", restroom.categoryBadge)

        // ¿Cuánto cuesta?
        val howMuch = OfflineTranslatorEngine.translateToJapanese("¿Cuánto cuesta?")
        assertTrue(howMuch.japaneseText.contains("いくらですか？"))
        assertEquals("Ikura desu ka?", howMuch.romaji)

        // ¿Cuánto cuesta esto?
        val howMuchThis = OfflineTranslatorEngine.translateToJapanese("¿Cuánto cuesta esto?")
        assertTrue(howMuchThis.japaneseText.contains("これはいくらですか？"))
        assertEquals("Kore wa ikura desu ka?", howMuchThis.romaji)

        // La cuenta, por favor
        val bill = OfflineTranslatorEngine.translateToJapanese("La cuenta, por favor")
        assertEquals("お会計をお願いします。", bill.japaneseText)
        assertEquals("O-kaikei o onegaishimasu.", bill.romaji)

        // ¿Tienen menú en inglés?
        val menu = OfflineTranslatorEngine.translateToJapanese("¿Tienen menú en inglés?")
        assertTrue(menu.japaneseText.contains("英語のメニューはありますか？"))
        assertEquals("Eigo no menyū wa arimasu ka?", menu.romaji)

        // Una cerveza, por favor
        val oneBeer = OfflineTranslatorEngine.translateToJapanese("Una cerveza, por favor")
        assertTrue(oneBeer.japaneseText.contains("ビールを1つお願いします。"))
        assertEquals("Bīru o hitotsu onegaishimasu.", oneBeer.romaji)

        // Dos cervezas, por favor
        val twoBeers = OfflineTranslatorEngine.translateToJapanese("Dos cervezas, por favor")
        assertTrue(twoBeers.japaneseText.contains("ビールを2つお願いします。"))
        assertEquals("Bīru o futatsu onegaishimasu.", twoBeers.romaji)

        // Agua, por favor
        val water = OfflineTranslatorEngine.translateToJapanese("Agua, por favor")
        assertTrue(water.japaneseText.contains("お水をお願いします。"))
        assertEquals("O-mizu o onegaishimasu.", water.romaji)

        // Dos aguas, por favor
        val twoWaters = OfflineTranslatorEngine.translateToJapanese("Dos aguas, por favor")
        assertTrue(twoWaters.japaneseText.contains("お水を2つお願いします。"))
        assertEquals("O-mizu o futatsu onegaishimasu.", twoWaters.romaji)
    }

    @Test
    fun `translateToJapanese handles courteous disculpe prefixes and stand-alone disculpa`() {
        val pardonAlone = OfflineTranslatorEngine.translateToJapanese("Disculpe")
        assertEquals("すみません。", pardonAlone.japaneseText)
        assertEquals("Sumimasen.", pardonAlone.romaji)

        val stationWithPrefix = OfflineTranslatorEngine.translateToJapanese("Disculpe, ¿dónde está la estación?")
        assertEquals("すみません、駅はどこですか？", stationWithPrefix.japaneseText)
        assertEquals("Sumimasen, Eki wa doko desu ka?", stationWithPrefix.romaji)
    }

    @Test
    fun `translateToJapanese supports English travel queries`() {
        val englishRestroom = OfflineTranslatorEngine.translateToJapanese("Where is the restroom?")
        assertTrue(englishRestroom.japaneseText.contains("トイレはどこですか？"))

        val englishCheck = OfflineTranslatorEngine.translateToJapanese("The check please")
        assertEquals("お会計をお願いします。", englishCheck.japaneseText)

        val englishBeer = OfflineTranslatorEngine.translateToJapanese("One beer please")
        assertTrue(englishBeer.japaneseText.contains("ビールを1つお願いします。"))

        val englishCard = OfflineTranslatorEngine.translateToJapanese("Can I pay with credit card?")
        assertTrue(englishCard.japaneseText.contains("カードは使えますか？"))
    }

    @Test
    fun `translateToJapanese handles payments, tax-free, and dietary needs`() {
        val suica = OfflineTranslatorEngine.translateToJapanese("¿Puedo pagar con Suica?")
        assertTrue(suica.japaneseText.contains("Suicaは使えますか？"))

        val cash = OfflineTranslatorEngine.translateToJapanese("¿Solo efectivo?")
        assertTrue(cash.japaneseText.contains("現金は使えますか？"))

        val taxFree = OfflineTranslatorEngine.translateToJapanese("¿Tienen tax-free?")
        assertTrue(taxFree.japaneseText.contains("免税はできますか？"))

        val veg = OfflineTranslatorEngine.translateToJapanese("Soy vegetariano")
        assertEquals("ベジタリアンです。", veg.japaneseText)

        val allergy = OfflineTranslatorEngine.translateToJapanese("Tengo alergia al marisco")
        assertEquals("甲殻類のアレルギーがあります。", allergy.japaneseText)

        val hotelAddress = OfflineTranslatorEngine.translateToJapanese("A esta dirección de hotel, por favor")
        assertEquals("このホテルの住所までお願いします。", hotelAddress.japaneseText)
    }

    @Test
    fun `translateFromJapaneseSpeech translates incoming Japanese spoken queries accurately`() {
        val welcome = OfflineTranslatorEngine.translateFromJapaneseSpeech("いらっしゃいませ。何名様ですか？禁煙席でよろしいですか？")
        assertTrue(welcome.spanishSpokenText.contains("personas", ignoreCase = true))
        assertTrue(welcome.englishSpokenText.contains("party", ignoreCase = true) || welcome.englishSpokenText.contains("people", ignoreCase = true))
        assertTrue(welcome.contextLabel.startsWith("RESTAURANTE"))

        val shinkansen = OfflineTranslatorEngine.translateFromJapaneseSpeech("新幹線のぞみ号は16番ホームから発車します。エレベーターはあちらです。")
        assertTrue(shinkansen.spanishSpokenText.contains("16"))
        assertTrue(shinkansen.spanishSpokenText.contains("andén", ignoreCase = true))

        val bag = OfflineTranslatorEngine.translateFromJapaneseSpeech("レジ袋はいりますか？お箸と温めはどうしますか？")
        assertTrue(bag.spanishSpokenText.contains("bolsa", ignoreCase = true))

        val taxFree = OfflineTranslatorEngine.translateFromJapaneseSpeech("免税手続きのため、パスポートの原本を見せてください。")
        assertTrue(taxFree.spanishSpokenText.contains("pasaporte", ignoreCase = true))

        val suicaOnly = OfflineTranslatorEngine.translateFromJapaneseSpeech("申し訳ありませんが、現金またはSuicaのみとなります。")
        assertTrue(suicaOnly.spanishSpokenText.contains("Suica", ignoreCase = true) || suicaOnly.spanishSpokenText.contains("efectivo", ignoreCase = true))

        val blank = OfflineTranslatorEngine.translateFromJapaneseSpeech("   ")
        assertTrue(blank.spanishSpokenText.contains("micrófono", ignoreCase = true))
    }

    @Test
    fun `incomingJapaneseSamples and essentialPhrases have valid Japanese script for Locale JAPANESE TTS`() {
        assertTrue(OfflineTranslatorEngine.incomingJapaneseSamples.isNotEmpty())
        for (sample in OfflineTranslatorEngine.incomingJapaneseSamples) {
            assertTrue("Expected Japanese text for ${sample.contextLabel}", OfflineTranslatorEngine.isJapaneseText(sample.japaneseHeardText))
            assertTrue(sample.romaji.isNotBlank())
            assertTrue(sample.spanishSpokenText.isNotBlank())
            assertTrue(sample.englishSpokenText.isNotBlank())
        }

        assertTrue(OfflineTranslatorEngine.essentialPhrases.isNotEmpty())
        for (phrase in OfflineTranslatorEngine.essentialPhrases) {
            assertTrue("Expected Japanese text for ${phrase.sourceText}", OfflineTranslatorEngine.isJapaneseText(phrase.japaneseText))
            assertTrue(phrase.romaji.isNotBlank())
            assertTrue(phrase.categoryBadge.isNotBlank())
        }
    }

    @Test
    fun `isJapaneseText correctly distinguishes Japanese scripts from Latin alphabet`() {
        assertTrue(OfflineTranslatorEngine.isJapaneseText("こんにちは")) // Hiragana
        assertTrue(OfflineTranslatorEngine.isJapaneseText("ビール")) // Katakana
        assertTrue(OfflineTranslatorEngine.isJapaneseText("新幹線")) // Kanji
        assertTrue(OfflineTranslatorEngine.isJapaneseText("1000円")) // Number + Kanji

        assertTrue(!OfflineTranslatorEngine.isJapaneseText("¿Dónde está el baño?"))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText("Where is the station?"))
        assertTrue(!OfflineTranslatorEngine.isJapaneseText("12345"))
    }

    @Test
    fun `translateOcrJapaneseText correctly translates OCR text lines`() {
        val ocr = "新幹線 16番線\n本日満席\n生ビール 550円"
        val result = OfflineTranslatorEngine.translateOcrJapaneseText(ocr, isEs = true)
        assertTrue(result.hasJapanese)
        assertEquals(3, result.lines.size)
        assertTrue(result.lines[0].translatedText.contains("Shinkansen", ignoreCase = true))
        assertTrue(result.lines[1].translatedText.contains("completo", ignoreCase = true))
        assertTrue(result.lines[2].translatedText.contains("cerveza", ignoreCase = true))
    }

    @Test
    fun `travelerQuickPhrases is consolidated deduplicated and contains both Spanish and Japanese with audio TTS ready`() {
        val phrases = OfflineTranslatorEngine.travelerQuickPhrases
        assertTrue(phrases.isNotEmpty())

        // Sin repeticiones (desduplicado estricto)
        val uniqueJapanese = phrases.map { it.japaneseText.trim() }.toSet()
        assertEquals("No debe haber frases en japonés repetidas", phrases.size, uniqueJapanese.size)

        val uniqueSpanish = phrases.map { it.sourceEs.trim().lowercase() }.toSet()
        assertEquals("No debe haber frases en español repetidas", phrases.size, uniqueSpanish.size)

        for (phrase in phrases) {
            assertTrue("Texto en español no debe estar vacío", phrase.sourceEs.isNotBlank())
            assertTrue("Texto en inglés no debe estar vacío", phrase.sourceEn.isNotBlank())
            assertTrue("Texto en japonés no debe estar vacío", phrase.japaneseText.isNotBlank())
            assertTrue("Debe contener caracteres japoneses", OfflineTranslatorEngine.isJapaneseText(phrase.japaneseText))
            assertTrue("Romaji no debe estar vacío", phrase.romaji.isNotBlank())
            assertTrue("Categoría no debe estar vacía", phrase.categoryBadge.isNotBlank())
        }
    }

    @Test
    fun `TranslatorTabMode defines voice text and camera modes with localized labels`() {
        val modes = TranslatorTabMode.entries
        assertEquals(3, modes.size)
        assertTrue(modes.contains(TranslatorTabMode.VOICE))
        assertTrue(modes.contains(TranslatorTabMode.TEXT))
        assertTrue(modes.contains(TranslatorTabMode.CAMERA))

        for (mode in modes) {
            assertTrue(mode.id.isNotBlank())
            assertTrue(mode.iconEmoji.isNotBlank())
            assertTrue(mode.labelEs.isNotBlank())
            assertTrue(mode.labelEn.isNotBlank())
        }
    }

    @Test
    fun `ImageCropPreset and calculateCropBounds accurately compute crop coordinates`() {
        val presets = ImageCropPreset.entries
        assertTrue(presets.contains(ImageCropPreset.FULL))
        assertTrue(presets.contains(ImageCropPreset.TOP_HALF))
        assertTrue(presets.contains(ImageCropPreset.CENTER))
        assertTrue(presets.contains(ImageCropPreset.BOTTOM_HALF))
        assertTrue(presets.contains(ImageCropPreset.CUSTOM))

        // Prueba sobre imagen de 1000x2000
        val fullBounds = OfflineTranslatorEngine.calculateCropBounds(
            imageWidth = 1000,
            imageHeight = 2000,
            leftFraction = ImageCropPreset.FULL.leftFraction,
            topFraction = ImageCropPreset.FULL.topFraction,
            rightFraction = ImageCropPreset.FULL.rightFraction,
            bottomFraction = ImageCropPreset.FULL.bottomFraction
        )
        assertEquals(0, fullBounds.left)
        assertEquals(0, fullBounds.top)
        assertEquals(1000, fullBounds.width)
        assertEquals(2000, fullBounds.height)

        val topBounds = OfflineTranslatorEngine.calculateCropBounds(
            imageWidth = 1000,
            imageHeight = 2000,
            leftFraction = ImageCropPreset.TOP_HALF.leftFraction,
            topFraction = ImageCropPreset.TOP_HALF.topFraction,
            rightFraction = ImageCropPreset.TOP_HALF.rightFraction,
            bottomFraction = ImageCropPreset.TOP_HALF.bottomFraction
        )
        assertEquals(0, topBounds.left)
        assertEquals(0, topBounds.top)
        assertEquals(1000, topBounds.width)
        assertEquals(1000, topBounds.height)

        val bottomBounds = OfflineTranslatorEngine.calculateCropBounds(
            imageWidth = 1000,
            imageHeight = 2000,
            leftFraction = ImageCropPreset.BOTTOM_HALF.leftFraction,
            topFraction = ImageCropPreset.BOTTOM_HALF.topFraction,
            rightFraction = ImageCropPreset.BOTTOM_HALF.rightFraction,
            bottomFraction = ImageCropPreset.BOTTOM_HALF.bottomFraction
        )
        assertEquals(0, bottomBounds.left)
        assertEquals(1000, bottomBounds.top)
        assertEquals(1000, bottomBounds.width)
        assertEquals(1000, bottomBounds.height)

        // Resistencia a valores fuera de rango
        val clampedBounds = OfflineTranslatorEngine.calculateCropBounds(
            imageWidth = 500,
            imageHeight = 800,
            leftFraction = -0.5f,
            topFraction = -0.2f,
            rightFraction = 1.8f,
            bottomFraction = 2.0f
        )
        assertEquals(0, clampedBounds.left)
        assertEquals(0, clampedBounds.top)
        assertEquals(500, clampedBounds.width)
        assertEquals(800, clampedBounds.height)
    }

    @Test
    fun `translateOcrJapaneseText never outputs generic unavailable string for common signs and menus`() {
        val testSigns = listOf(
            "新宿駅 南口 改札口",
            "営業中 準備中 定休日",
            "立入禁止 危険 足元注意",
            "お会計はレジにて クレジットカード利用可",
            "唐揚げ定食 850円",
            "温泉 露天風呂 入浴料",
            "和牛 焼肉 盛り合わせ",
            "コインロッカー",
            "非常口 避難誘導",
            "禁煙席 喫煙所",
            "手荷物預かり所",
            "自動券売機",
            "のりば 2番線",
            "本日休業",
            "売り切れ 完売",
            "段差注意 頭上注意"
        )

        for (sign in testSigns) {
            val result = OfflineTranslatorEngine.translateOcrJapaneseText(sign, isEs = true)
            assertTrue("Debe detectar japonés en: $sign", result.hasJapanese)
            for (line in result.lines) {
                assertFalse(
                    "El texto '$sign' no debe devolver 'Traducción directa no disponible offline': ${line.translatedText}",
                    line.translatedText.contains("Traducción directa no disponible offline", ignoreCase = true)
                )
                assertTrue(
                    "Debe haber una traducción significativa para '$sign', obtuvo: ${line.translatedText}",
                    line.translatedText.isNotBlank()
                )
            }
        }
    }

    @Test
    fun `translateOcrJapaneseText performs morphological token segmentation on compound Japanese sentences`() {
        val compound = "新宿駅の南口で切符を買う"
        val result = OfflineTranslatorEngine.translateOcrJapaneseText(compound, isEs = true)
        assertTrue(result.hasJapanese)
        val translation = result.lines.first().translatedText
        assertFalse(translation.contains("Traducción directa no disponible offline"))
        assertTrue(translation.contains("Estación", ignoreCase = true) || translation.contains("Shinjuku", ignoreCase = true))
        assertTrue(translation.contains("Salida", ignoreCase = true) || translation.contains("Sur", ignoreCase = true))
        assertTrue(translation.contains("Billete", ignoreCase = true) || translation.contains("Ticket", ignoreCase = true))
    }

    @Test
    fun `calculateCropBounds handles multi-zone presets accurately`() {
        val width = 1000
        val height = 2000

        // Left half
        val leftBounds = OfflineTranslatorEngine.calculateCropBounds(
            width, height,
            ImageCropPreset.LEFT_HALF.leftFraction,
            ImageCropPreset.LEFT_HALF.topFraction,
            ImageCropPreset.LEFT_HALF.rightFraction,
            ImageCropPreset.LEFT_HALF.bottomFraction
        )
        assertEquals(0, leftBounds.left)
        assertEquals(0, leftBounds.top)
        assertEquals(500, leftBounds.width)
        assertEquals(2000, leftBounds.height)

        // Right half
        val rightBounds = OfflineTranslatorEngine.calculateCropBounds(
            width, height,
            ImageCropPreset.RIGHT_HALF.leftFraction,
            ImageCropPreset.RIGHT_HALF.topFraction,
            ImageCropPreset.RIGHT_HALF.rightFraction,
            ImageCropPreset.RIGHT_HALF.bottomFraction
        )
        assertEquals(500, rightBounds.left)
        assertEquals(0, rightBounds.top)
        assertEquals(500, rightBounds.width)
        assertEquals(2000, rightBounds.height)

        // Standard multi-zones
        val zones = OfflineTranslatorEngine.getStandardMultiZones()
        assertEquals(5, zones.size)
        val topZone = zones[0]
        val topBounds = OfflineTranslatorEngine.calculateCropBounds(
            width, height,
            topZone.leftFraction, topZone.topFraction,
            topZone.rightFraction, topZone.bottomFraction
        )
        assertEquals(0, topBounds.left)
        assertEquals(0, topBounds.top)
        assertEquals(1000, topBounds.width)
        assertEquals(700, topBounds.height)
    }
}


