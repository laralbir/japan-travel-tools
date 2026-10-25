# Backlog y Plan de Tareas (SDD + TDD)

Este documento define la fase de **Tareas** del flujo SDD. Todas las tareas están ordenadas secuencialmente para implementarse aplicando **TDD (Red -> Green -> Refactor)** y **Gitflow**.

> **Regla de versión Beta**: Mientras exista cualquier tarea pendiente o sin aprobación final del usuario en una herramienta, las versiones publicadas llevarán sufijo `-beta.X` (comenzando por `0.0.1-beta.1`) y las herramientas mostrarán su estado `BETA`.
>
> **Tarea Continua Transversal (`TASK-ISSUES`)**: En cada iteración de desarrollo y antes de cada release, revisar los **Issues de GitHub** (`gh issue list`), analizar si procede o no reparar cada issue reportado, **responder en el hilo del issue** con el resultado del análisis y, si procede, aplicar el ciclo SDD + TDD hasta su resolución y cierre.

---

## Hito 1: Inicialización SDD y Base del Proyecto (`v0.0.1-beta.1`)

- [x] **TASK-001**: Inicializar repositorio para agentes de IA (`AGENTS.md`, `.gitignore`, `README.md`, `CHANGELOG.md`).
- [x] **TASK-002**: Redactar especificaciones funcionales completas en `/docs/specs/` (`SPEC-00` a `SPEC-04`), análisis técnico en `/docs/analysis/` y guía de usuario en `/docs/user-guide.md`.
- [x] **TASK-003**: Configurar ramas **Gitflow** (`main` y `develop`) con el commit fundacional de especificación.
- [x] **TASK-004**: Seleccionar y especificar el **template / sistema de diseño moderno** de la app: basado en **[JetBrains KotlinConf App](https://jetbrains.github.io/kotlinconf-app/)** combinado con una **paleta de colores tradicionales de Japón (`Akabeni` rojo Torii, `Ai-iro` índigo, `Matcha`, papel `Washi` en modo claro y tinta `Sumi` en modo oscuro)**, optimizado para **Pixel 10 Pro XL**, **Pixel 9 Pro XL** y la tablet **TCL NXTPAPER 11 Plus** (`SPEC-00`).
- [x] **TASK-005**: Crear esqueleto del proyecto Android basado en el diseño de `kotlinconf-app` (Kotlin + Jetpack Compose + Design System japonés + Gradle Version Catalog + Room + DataStore + Ktor/Retrofit + AboutLibraries + Testing dependencies) en rama `feature/project-scaffolding`.
- [x] **TASK-006**: Configurar numeración de versión `0.0.1-beta.1` en `app/build.gradle.kts` y verificar compilación de APK (`./gradlew testDebugUnitTest assembleDebug`).

---

## Hito 2: Configuración Core, Idioma, Tema y Persistencia (`SPEC-00`)

- [x] **TASK-101 (TDD)**: Escribir tests unitarios para `LanguageResolver` (idioma del sistema `es` -> Español, `en` -> Inglés, cualquier otro como `fr`/`ja` -> fallback a Inglés; selección manual prevalece).
- [ ] **TASK-102 (TDD)**: Implementar `UserPreferencesRepository` con Jetpack DataStore y sus tests de persistencia (tema Claro/Oscuro/Sistema, idioma, preferencias de herramientas).
- [ ] **TASK-103 (TDD)**: Implementar recursos de cadenas `res/values/strings.xml` (EN por defecto) y `res/values-es/strings.xml` (ES), junto con `SettingsViewModel` y `SettingsScreen`.
- [ ] **TASK-104 (TDD)**: Implementar apartado **"Acerca de y Licencias Open Source"** (`AboutScreen` y `OpenSourceLicensesScreen` con plugin `AboutLibraries`), mostrando la versión exacta de la app (`0.0.1-beta.X` / `0.0.1`), créditos de datos abiertos (Frankfurter API) y todas las librerías de terceros usadas con sus licencias offline.
- [ ] **TASK-105 (TDD)**: Diseñar y generar el **Icono Oficial de la App (`Adaptive Icon` con capas `foreground`, `background` y `monochrome` para Pixel Launcher)**, configurar la **Splash Screen nativa (`androidx.core:core-splashscreen`)** en modo Claro (*Washi*) y Oscuro (*Sumi*), e integrar el set de **iconos vectoriales modernos** en el Design System.
- [ ] **TASK-106 (TDD)**: Implementar navegación adaptativa (`NavigationSuiteScaffold` + `WindowSizeClass`) con indicador visual `BETA`:
  - `NavigationBar` inferior ergonómica y Edge-to-Edge en **Pixel 10 Pro XL / Pixel 9 Pro XL**.
  - `NavigationRail` lateral y layouts multi-panel (`ListDetailPaneScaffold` / `SupportingPaneScaffold`) + alto contraste NXTPAPER en la tablet **TCL NXTPAPER 11 Plus**.
- [ ] **TASK-107 (TDD)**: Escribir tests unitarios e implementar `ConnectivityObserver` y el componente **`ConnectivityStatusPill` + `QuickConnectivitySwitcherSheet`** en el Home/Cabecera (detección en tiempo real de `Wi-Fi`, `SIM física`, `eSIM`, `Roaming` u `Offline`, y botones rápidos para abrir el panel nativo de Internet de Android y el selector de SIM/eSIM).
- [ ] **TASK-108 (TDD)**: Escribir tests unitarios e implementar el motor de **Sincronización Multi-Dispositivo con Cuenta de Google (`GoogleAccountSyncManager` + `ConflictResolutionUseCase` + `WorkManager`)** para mantener sincronizados preferencias, itinerarios, transportes, guías, documentos y fotos entre el móvil (**Pixel 10/9 Pro XL**) y la tablet (**TCL NXTPAPER 11 Plus**), con opción de *"Sincronizar archivos pesados solo por Wi-Fi"* y disponibilidad 100% offline.

---

## Hito 3: Herramienta 1 — Relojes y Ubicación (`SPEC-01`)

- [ ] **TASK-201 (TDD)**: Escribir tests unitarios para `CalculateClockComparisonUseCase` (hora local, hora en Japón `Asia/Tokyo`, hora en país de origen, cálculo exacto de diferencia horaria con DST y tramo del día).
- [ ] **TASK-202 (TDD)**: Escribir tests unitarios e implementar `OfflineTimeZoneResolver` (resolución offline de latitud/longitud a `ZoneId` sin requerir internet, con fallback al `ZoneId.systemDefault()`).
- [ ] **TASK-203 (TDD)**: Implementar `WorldClockViewModel` (actualización reactiva del reloj, solicitud opcional de permiso `ACCESS_COARSE_LOCATION` y persistencia del país de origen en `DataStore`).
- [ ] **TASK-204 (TDD)**: Implementar `WorldClockScreen` en Jetpack Compose con selector buscable de país/zona horaria de origen y tests de UI.

---

## Hito 4: Herramienta 2 — Conversor de Divisa Offline-First (`SPEC-02`)

- [ ] **TASK-301 (TDD)**: Escribir tests unitarios para `ConvertCurrencyUseCase` (conversión bidireccional `EUR <-> JPY`, redondeo para yenes sin decimales y divisas con 2 decimales, soporte de tasa manual).
- [ ] **TASK-302 (TDD)**: Implementar `CurrencyRateDao` (Room) y `SeedExchangeRates` (tasas semilla preinstaladas para garantizar funcionamiento offline desde el primer segundo) con sus tests de repositorio.
- [ ] **TASK-303 (TDD)**: Implementar cliente `FrankfurterApiService` (`https://api.frankfurter.dev/v1/latest`) y lógica de sincronización/caché en `OfflineFirstCurrencyRepository` (tests con Mock HTTP).
- [ ] **TASK-304 (TDD)**: Implementar `CurrencyConverterViewModel` y `CurrencyConverterScreen` (inputs bidireccionales, botón `Swap ⇅`, importes rápidos de Japón `¥100` a `¥10,000`, indicador de fecha de caché/modo offline y botón de actualización).

---

## Hito 5: Herramienta 3 — Pasaporte/DNI, Documentos y Guías de Viaje (`SPEC-03`)

- [ ] **TASK-401 (TDD)**: Escribir tests unitarios para `DocumentRepository`, `TravelGuideRepository` e `InternalFileStorageManager` (copia segura de imágenes, PDFs y Markdown a `Context.filesDir`, borrado limpio, marcador de página en PDFs y filtrado por categoría/ciudad).
- [ ] **TASK-402 (TDD)**: Implementar sección de **Identificación Rápida (Pasaporte y DNI)** con captura de cámara / Photo Picker, metadatos rápidos copiables y visor a pantalla completa con zoom (`pinch-to-zoom`).
- [ ] **TASK-403 (TDD)**: Integrar `BiometricPrompt` opcional para desbloquear la visualización de Pasaporte y DNI según la preferencia persistida del usuario.
- [ ] **TASK-404 (TDD)**: Implementar **Repositorio de Documentos de Viaje** (categorías: Vuelos, Hoteles, Transporte, Seguros, Entradas, Guías, Otros), buscador offline y visor PDF integrado (`PdfRenderer`).
- [ ] **TASK-405 (TDD)**: Implementar **Gestor y Creador de Guías de Viaje (`TravelGuidesScreen` y `GuideEditorScreen`)** para importar guías externas (PDF/Markdown/Imágenes) y crear/editar guías propias con notas, etiquetas por ciudad y galería de fotos offline.

---

## Hito 6: Herramienta 4 — Planificador de Viaje con Fotos, Google Maps, Calendario y Horarios (`SPEC-04`)

- [ ] **TASK-501 (TDD)**: Escribir tests unitarios para el dominio del planificador: generación de días entre fechas (`CreateTripUseCase`), ordenación cronológica de horarios en `Asia/Tokyo`, y detección de solapamientos horarios (`DetectScheduleConflictsUseCase`).
- [ ] **TASK-502 (TDD)**: Escribir tests unitarios e implementar `ParseGoogleMapsUrlUseCase` y modelos de ubicación (extracción de coordenadas `lat, lng` y nombres desde URLs/Geo URIs y construcción de Intents de ruta hacia Google Maps).
- [ ] **TASK-503 (TDD)**: Implementar `TripPlannerDao` (Room) y `TripPlannerRepository` con soporte para vincular múltiples fotos offline (`filesDir/planner_photos/`), documentos (`SPEC-03`) y guías de viaje a cada actividad del horario.
- [ ] **TASK-504 (TDD)**: Implementar vista de **Calendario y Línea Temporal de Horarios (`DayTimelineView`)** con tarjetas de actividad, galería de miniaturas de fotos y avisos de solapamiento.
- [ ] **TASK-505 (TDD)**: Implementar integración dual con **Google Maps**:
  - Vista de **Mapa Interactivo Embebido (`DayEmbeddedMapView` con Maps Compose)** mostrando marcadores numerados por orden horario y polilínea de ruta diaria.
  - Botones de acción rápida **"Abrir en Google Maps"** y **"Cómo llegar"** mediante Deep Links/Intents compatibles con mapas offline de Google Maps.
- [ ] **TASK-506 (TDD)**: Implementar `SystemCalendarExporter` para sincronizar/exportar actividades o el itinerario completo al Calendario de Android / Google Calendar con zona horaria `Asia/Tokyo`.

---

## Hito 7: Herramienta 5 — Planificador de Vuelos, Trenes, Metros y Autobuses (`SPEC-05`)

- [ ] **TASK-601 (TDD)**: Escribir tests unitarios para `CalculateTransportTimelineUseCase` y `CalculateLayoverAndTransferUseCase` (cálculo exacto de duración de vuelo entre zonas horarias distintas p. ej. `Europe/Madrid` -> `Asia/Qatar` -> `Asia/Tokyo`, tiempos de facturación/embarque, escalas aéreas, y **transbordos multimodales entre Vuelo, Shinkansen/Tren, Metro y Autobús** con alertas de conexión ajustada y cambio de día `+1`/`+2`).
- [ ] **TASK-602 (TDD)**: Implementar `TransportPlannerDao` (Room), `TransportPlannerRepository` y catálogo offline `AirportAndStationCatalog` (aeropuertos internacionales/japoneses `HND`, `NRT`, `KIX` y principales estaciones/terminales de tren, metro y autobús de Japón con zona horaria, nombres en japonés y coordenadas).
- [ ] **TASK-603 (TDD)**: Implementar `TransportPlannerViewModel` y pantallas Compose (`TransportPlannerScreen`, `FlightSegmentTimelineCard`, `TrainMetroBusSegmentCard`, `LayoverTransferConnectorCard`) con estilo `KotlinConf` + colores de Japón, mostrando andén, vagón, asiento, reserva de maletas Shinkansen, cobertura JR Pass/Suica, número de salida de estación de metro (`Exit`), dársena de autobús y enlace directo a tarjetas de embarque / billetes QR (`SPEC-03`).

---

## Hito 8: Herramienta 6 — Asistente IA con Gemini y Acceso Directo a Traducción Simultánea (`SPEC-06`)

- [ ] **TASK-701 (TDD)**: Escribir tests unitarios e implementar el motor de **Traducción Simultánea Offline (`SimultaneousTranslateUseCase` + `MlKitOfflineTranslator` + `TransliterateToRomajiUseCase`)** para traducir voz, texto e imágenes (`OCR Japonés`) entre `ES/EN <-> Japonés` con pronunciación en Romaji y tarjetas de emergencia/alergias offline.
- [ ] **TASK-702 (TDD)**: Escribir tests unitarios e implementar el **Asistente de IA con Gemini (`AskGeminiAssistantUseCase`, `AnalyzeJapaneseImageWithGeminiUseCase`)** soportando Gemini API + Gemini Nano On-Device (en Pixel 10/9 Pro XL), conversión automática de precios de cartas de restaurantes a `EUR`, persistencia offline de respuestas en Room y exportación directa a Guías (`SPEC-03`) o al Planificador (`SPEC-04`).
- [ ] **TASK-703 (TDD)**: Implementar el **Acceso Directo Global a Traducción Simultánea** en la cabecera/Home y las pantallas `SimultaneousTranslatorScreen` (modo conversación cara a cara reversible 180° en Pixel y doble panel en TCL NXTPAPER 11 Plus) y `GeminiAssistantScreen`.

---

## Hito 9: Cierre de Beta, Aprobación y Release Estable `v0.0.1`

- [ ] **TASK-801**: Ejecutar batería completa de tests unitarios e instrumentados y validar uso 100% en Modo Avión.
- [ ] **TASK-802**: Revisión y aprobación del usuario de cada herramienta para retirar la etiqueta `BETA`.
- [ ] **TASK-803**: Merge a `main` vía Gitflow (`release/0.0.1`), generación de tag `v0.0.1`, compilación de APK final y publicación en **GitHub Releases**.
