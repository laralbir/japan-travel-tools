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
- [x] **TASK-007**: Especificar e implementar sistema de despliegue **ADB Debug en dispositivos autorizados** (`authorized-devices.local.conf` en `.gitignore`, plantilla `authorized-devices.example.conf`, script `scripts/deploy-authorized-devices.sh` y tarea `./gradlew installAuthorizedDebug`).

---

## Hito 2: Configuración Core, Idioma, Tema y Persistencia (`SPEC-00`)

- [x] **TASK-101 (TDD)**: Escribir tests unitarios para `LanguageResolver` (idioma del sistema `es` -> Español, `en` -> Inglés, cualquier otro como `fr`/`ja` -> fallback a Inglés; selección manual prevalece).
- [x] **TASK-102 (TDD)**: Implementar `UserPreferencesRepository` y sus tests de persistencia (tema Claro/Oscuro/Sistema, idioma, preferencias de herramientas).
- [x] **TASK-103 (TDD)**: Implementar recursos de cadenas `res/values/strings.xml` (EN por defecto) y `res/values-es/strings.xml` (ES), junto con `SettingsScreen`.
- [x] **TASK-104 (TDD)**: Implementar apartado **"Acerca de y Licencias Open Source"** (`OpenSourceLibrariesCatalog`), mostrando la versión exacta de la app (`0.0.1-beta.1`), créditos de datos abiertos (Frankfurter API) y todas las librerías de terceros usadas con sus licencias offline.
- [x] **TASK-105 (TDD)**: Diseñar y generar el **Icono Oficial de la App (`Adaptive Icon` con Torii + Sol Naciente `Akabeni` + fondo `Ai-iro`)**, configurar el tema nativo de arranque en modo Claro (*Washi*) y Oscuro (*Sumi*), e integrar el sistema de diseño `KotlinConf`.
- [x] **TASK-106 (TDD)**: Implementar navegación adaptativa (`NavigationBar` inferior en **Pixel 10/9 Pro XL** y `NavigationRail` lateral + doble panel en **TCL NXTPAPER 11 Plus**) con indicador visual `BETA`.
- [x] **TASK-107 (TDD)**: Implementar `ConnectivityStatusHelper` y el componente **`ConnectivityStatusPill` + `QuickConnectivitySwitcherContent`** en el Home/Cabecera (detección en tiempo real de `Wi-Fi`, `Pocket Wi-Fi`, `SIM física`, `eSIM`, `Roaming` u `Offline`, y botones rápidos para abrir el panel nativo de Internet de Android y el selector de SIM/eSIM).
- [x] **TASK-108 (TDD)**: Implementar configuración de **Sincronización Multi-Dispositivo con Cuenta de Google** entre móvil (**Pixel 10/9 Pro XL**) y tablet (**TCL NXTPAPER 11 Plus**) con disponibilidad offline.

---

## Hito 3: Herramienta 1 — Relojes y Ubicación (`SPEC-01`)

- [x] **TASK-201 (TDD)**: Escribir tests unitarios para `CalculateClockComparisonUseCase` (hora local, hora en Japón `Asia/Tokyo`, hora en país de origen, cálculo exacto de diferencia horaria con DST y tramo del día).
- [x] **TASK-202 (TDD)**: Escribir tests unitarios e implementar `OfflineTimeZoneResolver` (resolución offline de latitud/longitud a `ZoneId` sin requerir internet, con fallback al `ZoneId.systemDefault()`).
- [x] **TASK-203 (TDD)**: Implementar actualización reactiva del reloj y persistencia del país de origen en `UserPreferencesRepository`.
- [x] **TASK-204 (TDD)**: Implementar vista de Relojes en Jetpack Compose con selector de país/zona horaria de origen.

---

## Hito 4: Herramienta 2 — Conversor de Divisa Offline-First (`SPEC-02`)

- [x] **TASK-301 (TDD)**: Escribir tests unitarios para `ConvertCurrencyUseCase` (conversión bidireccional `EUR <-> JPY`, redondeo para yenes sin decimales y divisas con 2 decimales, soporte de tasa manual).
- [x] **TASK-302 (TDD)**: Implementar `SeedExchangeRates` (tasas semilla preinstaladas para garantizar funcionamiento offline desde el primer segundo) con sus tests.
- [x] **TASK-303 (TDD)**: Implementar cliente y parser `FrankfurterJsonParser` (`https://api.frankfurter.dev/v1/latest`) y lógica de sincronización/caché offline.
- [x] **TASK-304 (TDD)**: Implementar UI del Conversor de Divisas (inputs bidireccionales, botón `Swap ⇅`, importes rápidos de Japón `¥100` a `¥10,000`, indicador de fecha de caché/modo offline y botón de actualización).

---

## Hito 5: Herramienta 3 — Pasaporte/DNI, Documentos y Guías de Viaje (`SPEC-03`)

- [x] **TASK-401 (TDD)**: Escribir tests unitarios para `DocumentsFilterUseCase` y `DefaultTravelGuidesCatalog` (filtrado por categoría/ciudad y prioridad de elementos fijados `Pinned`).
- [x] **TASK-402 (TDD)**: Implementar sección de **Identificación Rápida (Pasaporte y DNI)** en `VaultAndGuidesAdaptiveScreen`.
- [x] **TASK-403 (TDD)**: Integrar bloqueo de privacidad persistido para desbloquear la visualización de Pasaporte y DNI.
- [x] **TASK-404 (TDD)**: Implementar **Repositorio de Documentos de Viaje** (categorías: Vuelos, Hoteles, Transporte, Seguros, Entradas, Guías) y buscador offline.
- [x] **TASK-405 (TDD)**: Implementar sección de **Guías de Viaje Offline** organizadas por ciudad (*Tokio*, *Kioto*, *Osaka*) con etiquetas y contador de fotos.

---

## Hito 6: Herramienta 4 — Planificador de Viaje con Fotos, Google Maps, Calendario y Horarios (`SPEC-04`)

- [x] **TASK-501 (TDD)**: Escribir tests unitarios para `TripPlannerUseCase` (ordenación cronológica de horarios en `Asia/Tokyo` y detección de solapamientos horarios).
- [x] **TASK-502 (TDD)**: Escribir tests unitarios e implementar `extractCoordinatesFromGoogleMapsUrl` y lanzamiento de Intents de ruta hacia Google Maps.
- [x] **TASK-503 (TDD)**: Implementar catálogo de paradas con fotos offline y documentos vinculados.
- [x] **TASK-504 (TDD)**: Implementar vista de **Calendario y Línea Temporal de Horarios (`DayTimelineList` / `TimelineStopCard`)** con miniaturas de fotos y avisos de solapamiento.
- [x] **TASK-505 (TDD)**: Implementar integración dual con **Google Maps** (`EmbeddedRouteMapCard` con marcadores numerados + apertura directa en Google Maps) y vista de doble panel 50/50 en la tablet **TCL NXTPAPER 11 Plus**.
- [x] **TASK-506 (TDD)**: Implementar exportación de paradas al Calendario de Android / Google Calendar con zona horaria `Asia/Tokyo`.
- [x] **TASK-507 (TDD)**: Implementar exportación e importación de planes de viaje y transporte en formato **JSON legible (`TravelPlanJsonCodec` + `JsonPortabilityModalSheet`)** con soporte de fusión (`Merge`) y reemplazo (`Replace`) para preparar viajes desde apps externas.
- [x] **TASK-508 (TDD)**: Persistencia real offline-first en almacenamiento local (`context.filesDir`) para planes, transportes, documentos, guías y reservas + soporte completo de gestión de múltiples planes (Crear / Editar / Cambiar / Borrar planes de viaje) + adjuntos de archivos y PDFs a documentos y reservas.

---

## Hito 7: Herramienta 5 — Planificador de Vuelos, Trenes, Metros y Autobuses (`SPEC-05`)

- [x] **TASK-601 (TDD)**: Escribir tests unitarios para `TransportPlannerUseCase` (cálculo exacto de duración de vuelo entre zonas horarias distintas `Europe/Madrid` -> `Asia/Qatar` -> `Asia/Tokyo`, tiempos de facturación/embarque, escalas aéreas y transbordos entre Shinkansen, Metro y Autobús con alertas de conexión ajustada).
- [x] **TASK-602 (TDD)**: Implementar modelo multimodal (`FLIGHT`, `TRAIN_SHINKANSEN`, `SUBWAY_METRO`, `BUS`) con terminales, andenes, vagón, asiento, salida de estación (`Exit`) y dársena de autobús.
- [x] **TASK-603 (TDD)**: Implementar `TransportMultiModalScreen` con tarjetas de tramo y tarjetas automáticas de escala/transbordo.
- [x] **TASK-604 (TDD)**: Integrar los planes de vuelos, Shinkansen, metro y autobús en el paquete JSON exportable/importable (`PortableTravelPlanBundle`) y en el almacén reactivo `SharedTravelPlanStore`.

---

## Hito 8: Herramienta 6 — Asistente IA con Gemini y Acceso Directo a Traducción Simultánea (`SPEC-06`)

- [x] **TASK-701 (TDD)**: Escribir tests unitarios e implementar `OfflineTranslatorEngine` para traducir frases de viaje entre `ES/EN <-> Japonés` con pronunciación en Romaji.
- [x] **TASK-702 (TDD)**: Escribir tests unitarios e implementar `GeminiAssistantEngine` con conversión automática de precios en yenes (`¥`) de cartas de restaurantes a `EUR`.
- [x] **TASK-703 (TDD)**: Implementar el **Acceso Directo Global a Traducción Simultánea** en la cabecera y la pantalla `GeminiAndTranslatorScreen` con tarjeta japonesa giratoria **180°** para mostrador/mesa.
- [x] **TASK-704 (TDD)**: Implementar `AiPlanBuilderEngine` para permitir a la IA Gemini crear nuevos planes de viaje y mejorar/optimizar los planes existentes en la app (resolución de conflictos horarios + adición de nuevas jornadas y tramos de Shinkansen/transporte).
- [x] **TASK-705 (TDD)**: Ampliar `OfflineTranslatorEngine` con diccionario exhaustivo bidireccional, enrutamiento inteligente de idioma (`isJapaneseText`) y regla estricta de no-fuga de caracteres japoneses a la locución TTS en español/inglés.
- [x] **TASK-706 (TDD)**: Refactorizar `translateToJapanese` en `OfflineTranslatorEngine` garantizando traducciones directas, fieles y concisas sin palabras adicionales inventadas (cero boilerplate).
- [x] **TASK-707 (TDD)**: Implementar la Traducción Visual con Cámara (OCR con ML Kit Japanese Text Recognition + Procesamiento en `OfflineTranslatorEngine.translateOcrJapaneseText` + Interfaz de Cámara/Galería en `VoiceTranslatorScreen`).

---

## Hito 8B: Herramienta 7 — Previsión Meteorológica con Open-Meteo para Planificación de Usuario e IA (`SPEC-07`)

- [x] **TASK-751 (TDD)**: Escribir tests unitarios para `OpenMeteoWeatherUseCase` (parseo de JSON de `api.open-meteo.com/v1/forecast`, clasificación de códigos WMO `0..99`, cálculo de idoneidad `OUTDOOR_OPTIMAL` vs `INDOOR_RECOMMENDED` e índice de visibilidad del Monte Fuji).
- [x] **TASK-752 (TDD)**: Implementar semillas offline (`SeedJapanWeatherCatalog`) para Tokio, Kioto, Osaka y Hakone/Monte Fuji y cliente HTTP con fallback 100% offline.
- [x] **TASK-753 (TDD)**: Integrar la previsión meteorológica de Open-Meteo en `AiPlanBuilderEngine.optimizePlanWithWeather` y `GeminiAssistantEngine` para que la IA adapte el itinerario según lluvia/sol.
- [x] **TASK-754 (TDD)**: Integrar tarjetas y controles interactivos de **Tiempo Open-Meteo** tanto en el Dashboard como en el Planificador de Viaje por días y en el Optimizador IA de `MainScreen.kt`.

---

## Hito 9: Cierre de Beta, Aprobación y Release Estable `v0.0.1`

- [ ] **TASK-801**: Ejecutar batería completa de tests unitarios e instrumentados y validar uso 100% en Modo Avión.
- [ ] **TASK-802**: Revisión y aprobación del usuario de cada herramienta para retirar la etiqueta `BETA`.
- [ ] **TASK-803**: Merge a `main` vía Gitflow (`release/0.0.1`), generación de tag `v0.0.1`, compilación de APK final y publicación en **GitHub Releases**.
