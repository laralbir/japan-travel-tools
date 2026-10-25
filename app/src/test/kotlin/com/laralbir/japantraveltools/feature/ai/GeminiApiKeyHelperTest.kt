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

    @Test
    fun `getAvailableModels returns comprehensive list including 2_0 flash as default recommended`() {
        val models = GeminiApiKeyHelper.getAvailableModels()
        assertTrue(models.isNotEmpty())
        val defaultModel = models.first { it.isRecommended }
        assertEquals("gemini-2.0-flash", defaultModel.id)
        assertTrue(models.any { it.id == "gemini-2.0-flash-lite" })
        assertTrue(models.any { it.id == "gemini-1.5-pro" })
        assertTrue(models.any { it.id == "gemini-1.5-flash" })
    }

    @Test
    fun `buildGenerateContentUrl constructs valid API endpoint with model and key`() {
        val urlDefault = GeminiApiKeyHelper.buildGenerateContentUrl("AIzaSyTestKey123")
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyTestKey123",
            urlDefault
        )

        val urlCustom = GeminiApiKeyHelper.buildGenerateContentUrl("AIzaSyTestKey123", "gemini-1.5-pro")
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=AIzaSyTestKey123",
            urlCustom
        )
    }

    @Test
    fun `evaluateHttpError provides clear diagnostics for 403 400 404 429 with key prefix check`() {
        // 403 with non-AIzaSy prefix (e.g. AQ. prefix user pasted)
        val err403NonGoogle = GeminiApiKeyHelper.evaluateHttpError(
            httpCode = 403,
            responseBody = "PERMISSION_DENIED",
            keyPrefix = "AQ.Ab8",
            isEs = true
        )
        assertTrue(err403NonGoogle.contains("403"))
        assertTrue(err403NonGoogle.contains("AIzaSy"))
        assertTrue(err403NonGoogle.contains("AQ.Ab8"))

        // 403 with valid prefix but disabled key
        val err403ValidPrefix = GeminiApiKeyHelper.evaluateHttpError(
            httpCode = 403,
            responseBody = "PERMISSION_DENIED",
            keyPrefix = "AIzaSy",
            isEs = true
        )
        assertTrue(err403ValidPrefix.contains("403"))

        // 404
        val err404 = GeminiApiKeyHelper.evaluateHttpError(
            httpCode = 404,
            responseBody = "Not Found",
            keyPrefix = "AIzaSy",
            isEs = true
        )
        assertTrue(err404.contains("404"))

        // 429
        val err429 = GeminiApiKeyHelper.evaluateHttpError(
            httpCode = 429,
            responseBody = "Quota exceeded",
            keyPrefix = "AIzaSy",
            isEs = true
        )
        assertTrue(err429.contains("429"))
    }

    @Test
    fun `testApiKeyConnection handles success and error status codes deterministically`() {
        // Mock successful 200 response
        val successResult = GeminiApiKeyHelper.testApiKeyConnection(
            apiKey = "AIzaSyValidKey",
            modelId = "gemini-2.0-flash",
            isEs = true,
            networkClient = { 200 to """{"candidates":[{"content":{"parts":[{"text":"pong"}]}}]}""" }
        )
        assertTrue(successResult is GeminiConnectionResult.Success)
        assertEquals("gemini-2.0-flash", (successResult as GeminiConnectionResult.Success).modelUsed)

        // Mock 403 error response
        val errorResult = GeminiApiKeyHelper.testApiKeyConnection(
            apiKey = "AQ.Ab8Invalid",
            modelId = "gemini-2.0-flash",
            isEs = true,
            networkClient = { 403 to """{"error":{"code":403,"message":"Permission Denied"}}""" }
        )
        assertTrue(errorResult is GeminiConnectionResult.Error)
        val err = errorResult as GeminiConnectionResult.Error
        assertEquals(403, err.httpCode)
        assertTrue(err.isFormatIssue)
        assertTrue(err.message.contains("AQ.Ab8"))
    }
}
