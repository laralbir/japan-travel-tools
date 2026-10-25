package com.laralbir.japantraveltools.core.i18n

import java.util.Locale

enum class AppLanguagePreference {
    SYSTEM,
    ENGLISH,
    SPANISH
}

enum class EffectiveLanguage(val languageTag: String) {
    ENGLISH("en"),
    SPANISH("es")
}

object LanguageResolver {
    fun resolveEffectiveLanguage(
        selectedPreference: AppLanguagePreference,
        systemLocale: Locale = Locale.getDefault()
    ): EffectiveLanguage {
        return when (selectedPreference) {
            AppLanguagePreference.SPANISH -> EffectiveLanguage.SPANISH
            AppLanguagePreference.ENGLISH -> EffectiveLanguage.ENGLISH
            AppLanguagePreference.SYSTEM -> {
                if (systemLocale.language.lowercase(Locale.ROOT) == "es") {
                    EffectiveLanguage.SPANISH
                } else {
                    EffectiveLanguage.ENGLISH
                }
            }
        }
    }
}

