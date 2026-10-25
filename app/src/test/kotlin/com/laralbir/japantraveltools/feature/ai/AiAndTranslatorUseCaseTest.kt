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
}

