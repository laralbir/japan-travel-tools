package com.laralbir.japantraveltools.feature.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiKeyHelperTest {

    @Test
    fun `isValidApiKeyFormat validates correct Google API keys and rejects invalid ones`() {
        // Valid keys
        assertTrue(GeminiApiKeyHelper.isValidApiKeyFormat("AIzaSyD-1234567890abcdefghijklmnopqrstuv"))
        assertTrue(GeminiApiKeyHelper.isValidApiKeyFormat("AIzaSyA_BcDeFgHiJkLmNoPqRsTuVwXyZ0123456"))

        // Invalid keys
        assertFalse(GeminiApiKeyHelper.isValidApiKeyFormat(null))
        assertFalse(GeminiApiKeyHelper.isValidApiKeyFormat(""))
        assertFalse(GeminiApiKeyHelper.isValidApiKeyFormat("   "))
        assertFalse(GeminiApiKeyHelper.isValidApiKeyFormat("short-key"))
        assertFalse(GeminiApiKeyHelper.isValidApiKeyFormat("sk-proj-12345678901234567890")) // OpenAI key format
        assertFalse(GeminiApiKeyHelper.isValidApiKeyFormat("not_a_gemini_key_12345678901234567890"))
    }

    @Test
    fun `maskApiKey masks sensitive characters while preserving recognizable prefix and suffix`() {
        assertEquals("", GeminiApiKeyHelper.maskApiKey(null))
        assertEquals("", GeminiApiKeyHelper.maskApiKey(""))
        assertEquals("••••••••", GeminiApiKeyHelper.maskApiKey("short"))

        val realKey = "AIzaSyD-1234567890abcdefghijklmnopqr7B9x"
        val masked = GeminiApiKeyHelper.maskApiKey(realKey)
        assertEquals("AIzaSy...7B9x", masked)
    }

    @Test
    fun `getSetupGuideSteps returns bilingual friendly 4-step guide`() {
        val stepsEs = GeminiApiKeyHelper.getSetupGuideSteps(isEs = true)
        assertEquals(4, stepsEs.size)
        assertTrue(stepsEs[0].title.contains("Google AI Studio", ignoreCase = true))
        assertTrue(stepsEs[1].title.contains("Google", ignoreCase = true))
        assertTrue(stepsEs[2].title.contains("API Key", ignoreCase = true))
        assertTrue(stepsEs[3].title.contains("Peg", ignoreCase = true))

        val stepsEn = GeminiApiKeyHelper.getSetupGuideSteps(isEs = false)
        assertEquals(4, stepsEn.size)
        assertTrue(stepsEn[0].title.contains("Google AI Studio", ignoreCase = true))
        assertTrue(stepsEn[3].title.contains("Paste", ignoreCase = true))
    }

    @Test
    fun `AI_STUDIO_API_KEY_URL points to official Google AI Studio key creation page`() {
        assertEquals("https://aistudio.google.com/app/apikey", GeminiApiKeyHelper.AI_STUDIO_API_KEY_URL)
    }
}
