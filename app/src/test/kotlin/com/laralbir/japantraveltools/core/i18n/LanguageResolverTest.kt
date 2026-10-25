package com.laralbir.japantraveltools.core.i18n

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LanguageResolverTest {

    @Test
    fun `when user selects SYSTEM and system locale is Spanish, returns SPANISH`() {
        val resolved = LanguageResolver.resolveEffectiveLanguage(
            selectedPreference = AppLanguagePreference.SYSTEM,
            systemLocale = Locale.forLanguageTag("es-ES")
        )
        assertEquals(EffectiveLanguage.SPANISH, resolved)
    }

    @Test
    fun `when user selects SYSTEM and system locale is Mexican Spanish, returns SPANISH`() {
        val resolved = LanguageResolver.resolveEffectiveLanguage(
            selectedPreference = AppLanguagePreference.SYSTEM,
            systemLocale = Locale.forLanguageTag("es-MX")
        )
        assertEquals(EffectiveLanguage.SPANISH, resolved)
    }

    @Test
    fun `when user selects SYSTEM and system locale is English, returns ENGLISH`() {
        val resolved = LanguageResolver.resolveEffectiveLanguage(
            selectedPreference = AppLanguagePreference.SYSTEM,
            systemLocale = Locale.US
        )
        assertEquals(EffectiveLanguage.ENGLISH, resolved)
    }

    @Test
    fun `when user selects SYSTEM and system locale is unsupported French or Japanese, falls back to ENGLISH`() {
        val frenchResolved = LanguageResolver.resolveEffectiveLanguage(
            selectedPreference = AppLanguagePreference.SYSTEM,
            systemLocale = Locale.FRANCE
        )
        val japaneseResolved = LanguageResolver.resolveEffectiveLanguage(
            selectedPreference = AppLanguagePreference.SYSTEM,
            systemLocale = Locale.JAPAN
        )
        assertEquals(EffectiveLanguage.ENGLISH, frenchResolved)
        assertEquals(EffectiveLanguage.ENGLISH, japaneseResolved)
    }

    @Test
    fun `when user explicitly selects SPANISH or ENGLISH, overrides system locale`() {
        val forcedSpanish = LanguageResolver.resolveEffectiveLanguage(
            selectedPreference = AppLanguagePreference.SPANISH,
            systemLocale = Locale.US
        )
        val forcedEnglish = LanguageResolver.resolveEffectiveLanguage(
            selectedPreference = AppLanguagePreference.ENGLISH,
            systemLocale = Locale.forLanguageTag("es-ES")
        )
        assertEquals(EffectiveLanguage.SPANISH, forcedSpanish)
        assertEquals(EffectiveLanguage.ENGLISH, forcedEnglish)
    }
}

