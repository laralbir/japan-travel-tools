package com.laralbir.japantraveltools.feature.settings

data class OpenSourceLibraryNotice(
    val name: String,
    val artifactId: String,
    val version: String,
    val author: String,
    val license: String,
    val purposeEn: String,
    val purposeEs: String
)

object OpenSourceLibrariesCatalog {
    const val APP_VERSION_NAME = "1.1.0"
    const val APP_VERSION_CODE = 34

    val thirdPartyLibraries: List<OpenSourceLibraryNotice> = listOf(
        OpenSourceLibraryNotice(
            name = "Jetpack Compose & Material 3",
            artifactId = "androidx.compose:compose-bom",
            version = "2026.03.01",
            author = "Google / Android Open Source Project",
            license = "Apache License 2.0",
            purposeEn = "Declarative UI toolkit and adaptive layout system for Pixel 9/10 Pro XL and TCL NXTPAPER 11 Plus.",
            purposeEs = "Sistema de UI declarativa y diseño adaptativo para Pixel 9/10 Pro XL y TCL NXTPAPER 11 Plus."
        ),
        OpenSourceLibraryNotice(
            name = "Kotlin Standard Library & Coroutines",
            artifactId = "org.jetbrains.kotlinx:kotlinx-coroutines-android",
            version = "2.3.20 / 1.10.2",
            author = "JetBrains s.r.o.",
            license = "Apache License 2.0",
            purposeEn = "Asynchronous reactive streams (StateFlow) and structured concurrency.",
            purposeEs = "Flujos reactivos asíncronos (StateFlow) y concurrencia estructurada."
        ),
        OpenSourceLibraryNotice(
            name = "JetBrains KotlinConf App UI Blueprint",
            artifactId = "jetbrains.github.io/kotlinconf-app",
            version = "2025/2026 Design System",
            author = "JetBrains s.r.o.",
            license = "Apache License 2.0",
            purposeEn = "Inspiration for clean 1.dp bordered cards, schedule filter chips and tabular typography.",
            purposeEs = "Inspiración del sistema de tarjetas con borde 1.dp, chips de agenda y tipografía tabular."
        ),
        OpenSourceLibraryNotice(
            name = "AndroidX Navigation 3",
            artifactId = "androidx.navigation3:navigation3-runtime",
            version = "1.0.1",
            author = "Google / Android Open Source Project",
            license = "Apache License 2.0",
            purposeEn = "Type-safe navigation across all travel tools.",
            purposeEs = "Navegación tipada entre todas las herramientas de viaje."
        ),
        OpenSourceLibraryNotice(
            name = "AndroidX Lifecycle & ViewModel Compose",
            artifactId = "androidx.lifecycle:lifecycle-viewmodel-compose",
            version = "2.10.0",
            author = "Google / Android Open Source Project",
            license = "Apache License 2.0",
            purposeEn = "MVVM unidirectional state management surviving configuration changes.",
            purposeEs = "Gestión de estado MVVM unidireccional resistente a giros de pantalla."
        ),
        OpenSourceLibraryNotice(
            name = "Frankfurter Open Exchange Rates API",
            artifactId = "https://api.frankfurter.dev/v1/latest",
            version = "v1 (ECB Reference Rates)",
            author = "Frankfurter Open Source Project / European Central Bank",
            license = "MIT License (Open Source API)",
            purposeEn = "Open-source daily currency exchange rates (EUR <-> JPY) cached locally for offline use.",
            purposeEs = "Fuente open-source de tipos de cambio diarios (EUR <-> JPY) cacheados localmente para uso offline."
        ),
        OpenSourceLibraryNotice(
            name = "Open-Meteo Weather Forecast API (JMA Japan)",
            artifactId = "https://api.open-meteo.com/v1/forecast",
            version = "v1 (WMO + JMA High-Resolution)",
            author = "Open-Meteo Open Source Project / Japan Meteorological Agency",
            license = "CC BY 4.0 / GPLv3 (Open Source Weather API)",
            purposeEn = "Open-source weather forecasts for Tokyo, Kyoto, Osaka & Mt. Fuji used for user & Gemini AI itinerary planning.",
            purposeEs = "Previsión meteorológica open-source (JMA Japón) para Tokio, Kioto, Osaka y Monte Fuji usada para planificar por el usuario y la IA."
        ),
        OpenSourceLibraryNotice(
            name = "Google ML Kit Text Recognition Japanese (v2)",
            artifactId = "com.google.mlkit:text-recognition-japanese",
            version = "16.0.1",
            author = "Google LLC",
            license = "Android Software Development Kit License",
            purposeEn = "On-device optical character recognition (OCR) of Japanese Kanji, Hiragana and Katakana for camera translation.",
            purposeEs = "Reconocimiento óptico de caracteres (OCR) offline de Kanji, Hiragana y Katakana para traducir con la cámara."
        ),
        OpenSourceLibraryNotice(
            name = "JUnit 4 & Kotlinx Coroutines Test",
            artifactId = "junit:junit / kotlinx-coroutines-test",
            version = "4.13.2 / 1.10.2",
            author = "JUnit Team / JetBrains",
            license = "Eclipse Public License 1.0 / Apache 2.0",
            purposeEn = "Strict TDD unit testing framework for all domain and data use cases.",
            purposeEs = "Framework de tests unitarios para el flujo TDD de todas las herramientas."
        )
    )
}
