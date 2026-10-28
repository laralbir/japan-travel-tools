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
- [x] **TASK-109 (TDD)**: Modales de confirmación de seguridad para acciones de edición y eliminación (`SPEC-00 RF-00.13`): Componente `AppConfirmationDialog`, prevención de borrados y sobrescrituras accidentales en transportes, planes, paradas, ciudades, documentos y guías con confirmaciones destructivas (`isDestructive`) y reversibles.
- [x] **TASK-110 (TDD)**: Selectores interactivos de fechas (`SPEC-00 RF-00.14 & SPEC-04 RF-04.7`): Implementar `DateSelectorHelper` y componente `@Composable AppDatePickerField` con `DatePickerDialog` de Material 3, sustituyendo campos de texto manuales `YYYY-MM-DD` en la creación y edición de planes de viaje, transportes e itinerarios.
- [x] **TASK-111 (TDD)**: Cobertura universal de confirmación de borrado y sistema de Double Check para eliminaciones sensibles (`SPEC-00 RF-00.13`): `buildSensitiveDeleteConfirmation`, interruptor obligatorio de confirmación para borrado de planes completos, días enteros de visitas, fotos de Pasaporte/DNI y vaciado de itinerario en `MainScreen.kt`, deshabilitando el botón destructivo hasta la activación explícita.
- [x] **TASK-112 (TDD)**: Selectores interactivos de hora y controles numéricos con steppers (`SPEC-00 RF-00.11`, `SPEC-04 RF-04.8/04.9`, `SPEC-05 RF-05.7`): Implementar `TimeSelectorHelper`, componente `@Composable AppTimePickerField`, `NumericInputHelper`, componente `@Composable AppNumberStepperField` y teclados numéricos directos (`KeyboardType.Number` / `KeyboardType.Decimal`), reemplazando cajas de texto manuales de horas e inputs numéricos en paradas de itinerario, planes de viaje, tramos de transporte, conversor de divisas y coordenadas meteorológicas.
- [x] **TASK-113 (TDD)**: Selección de Cuenta de Google del sistema y sincronización total en Google Drive (`SPEC-00 RF-00.11`): Implementar `GoogleAccountSelectorHelper` (selector nativo de cuentas de Android `AccountManager.newChooseAccountIntent` sin escritura manual), `GoogleDriveSyncHelper`, modelo `JapanTravelCloudSyncBundle` con serialización y fusión sin pérdidas de todos los planes de viaje, paradas, tramos de transporte, guías, documentos y ciudades meteorológicas, e integración de tarjeta de cuenta y botones de sincronización con Google Drive en `SettingsPreferencesScreen`.
- [x] **TASK-114 (TDD)**: Diagnóstico y verificación de conexión Gemini Cloud, Selector de Modelos y Alertas Visibles de Error (`SPEC-06 RF-06.2 / RF-06.3`): Implementar `testApiKeyConnection` en `GeminiApiKeyHelper` (prueba real con feedback claro de 200 OK, 403 Permiso Denegado / clave no válida, 400, 429 cuota y red), botón `⚡ Probar Conexión` en `GeminiApiKeySetupCard`, selector de modelos oficiales (`gemini-2.0-flash`, `gemini-2.0-flash-lite`, `gemini-1.5-pro`, `gemini-1.5-flash`, `gemini-2.5-flash`), persistencia en `UserPreferencesRepository` (`geminiModelId`), parseo robusto JSON con `org.json.JSONObject` y exposición de `cloudErrorDetail` con aviso destacado en `AiAssistantScreen` cuando Gemini falla en lugar de realizar un fallback offline silencioso.
- [x] **TASK-115 (TDD)**: Selector directo de modelo PRO vs Flash en el Asistente IA (`SPEC-06 RF-06.3`): Integrar `isProModel` en `GeminiApiKeyHelper`, constantes `PRO_DEFAULT_MODEL_ID` (`gemini-1.5-pro`) y `FLASH_DEFAULT_MODEL_ID` (`gemini-2.0-flash`), catálogo ampliado con `gemini-2.5-pro`, componente visual conmutador `[ ⚡ Flash (Rápido) ]` vs `[ 🧠 PRO (Razonamiento) ]` y selector de modelo Pro en la cabecera de `AiAssistantScreen`, permitiendo al viajero alternar entre velocidad de respuesta y razonamiento profundo en 1 toque.

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
- [x] **TASK-406 (TDD)**: Visor a pantalla completa y zoom táctil para Pasaporte y DNI (`SPEC-03 RF-03.1.3`): Escribir tests unitarios e implementar `ZoomPanState` con cálculo de escala `1.0x` a `5.0x`, traslación acotada, alternancia por doble toque y restablecimiento; implementar `FullScreenZoomableImageViewerDialog` e integrar apertura al tocar las fotos en `IdDocumentPhotoSection`.
- [x] **TASK-407 (TDD)**: Documentos de Identidad Personalizados y Clarificación del Bloqueo Biométrico (`SPEC-03 RF-03.2` & `RF-03.3`): Escribir tests unitarios e implementar modelo `CustomIdDocumentItem`, serialización/deserialización en `DocumentsAndGuidesJsonCodec`, persistencia reactiva en `SharedDocumentsAndGuidesStore` (`upsertCustomIdDoc`, `deleteCustomIdDoc`, `custom_id_docs.json`), tarjeta dedicada independiente para el interruptor de `🔒 Bloqueo Biométrico / PIN` en `PassportAndCustomsScreen`, componente `CustomIdDocumentItemCard` con visor inmersivo a pantalla completa (`pinch-to-zoom` 1x-5x), cambio de foto, edición y borrado seguro con confirmación double check, y modal sheet `CustomIdDocumentEditorModalSheet` con sugerencias rápidas (vacunas, recetas médicas para aduanas, carnet internacional, seguros, visados).

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
- [x] **TASK-509 (TDD)**: Asistente IA de Viaje interactivo y funcional (`SPEC-06 RF-06.2`): Estado reactivo de carga (`isLoading` con `CircularProgressIndicator` en el botón "Consultar IA"), arquitectura híbrida (Gemini REST API en la nube con API Key configurable + motor experto on-device offline con más de 20 categorías de viaje a Japón), insignia de procedencia y hora, acciones de copiado y guardado directo en Bóveda de Guías.
- [x] **TASK-510 (TDD)**: Gestión dinámica de ciudades y destinos meteorológicos (`SPEC-07 RF-07.4`): Modelo `WeatherCityItem`, CRUD completo (`addCity`, `updateCity`, `deleteCity`) en `SharedWeatherForecastStore`, catálogo de presets con 14 destinos populares de Japón, persistencia offline en `saved_weather_cities.json`, y modal sheet `CityWeatherEditorModalSheet`.

---

## Hito 7: Herramienta 5 — Planificador de Vuelos, Trenes, Metros y Autobuses (`SPEC-05`)

- [x] **TASK-601 (TDD)**: Escribir tests unitarios para `TransportPlannerUseCase` (cálculo exacto de duración de vuelo entre zonas horarias distintas `Europe/Madrid` -> `Asia/Qatar` -> `Asia/Tokyo`, tiempos de facturación/embarque, escalas aéreas y transbordos entre Shinkansen, Metro y Autobús con alertas de conexión ajustada).
- [x] **TASK-602 (TDD)**: Implementar modelo multimodal (`FLIGHT`, `TRAIN_SHINKANSEN`, `SUBWAY_METRO`, `BUS`) con terminales, andenes, vagón, asiento, salida de estación (`Exit`) y dársena de autobús.
- [x] **TASK-603 (TDD)**: Implementar `TransportMultiModalScreen` con tarjetas de tramo y tarjetas automáticas de escala/transbordo.
- [x] **TASK-604 (TDD)**: Integrar los planes de vuelos, Shinkansen, metro y autobús en el paquete JSON exportable/importable (`PortableTravelPlanBundle`) y en el almacén reactivo `SharedTravelPlanStore`.
- [x] **TASK-605 (TDD)**: Sistema de Advertencias Visuales y Notificaciones de Facturación, Embarque y Salida (`SPEC-05 RF-05.5`): Evaluador de dominio `TransportAlertEvaluator` con umbrales temporales precisos (`CHECK_IN_APPROACHING`, `CHECK_IN_OPEN`, `BOARDING_APPROACHING`, `BOARDING_NOW`, `DEPARTURE_IMMINENT`), `SharedTransportAlertStore`, canal de notificaciones y despachador Android `TransportAlertNotificationHelper`, banner visual `TransportAlertBanner`, campos de facturación/embarque en editor y botón de prueba/simulación interactiva.
- [x] **TASK-606 (TDD)**: Inicialización limpia de tramos al añadir transporte (`SPEC-05 RF-05.6`): `TransportPlannerUseCase.createEmptySegment` con inputs y campos de texto completamente limpios (`""`), sin precarga residual de valores de ejemplo (Nozomi/Shinkansen/horas predeterminadas), aislamiento reactivo de estado por ID y fidelidad en edición.

---

## Hito 8: Herramienta 6 — Asistente IA con Gemini y Acceso Directo a Traducción Simultánea (`SPEC-06`)

- [x] **TASK-701 (TDD)**: Escribir tests unitarios e implementar `OfflineTranslatorEngine` para traducir frases de viaje entre `ES/EN <-> Japonés` con pronunciación en Romaji.
- [x] **TASK-702 (TDD)**: Escribir tests unitarios e implementar `GeminiAssistantEngine` con conversión automática de precios en yenes (`¥`) de cartas de restaurantes a `EUR`.
- [x] **TASK-703 (TDD)**: Implementar el **Acceso Directo Global a Traducción Simultánea** en la cabecera y la pantalla `GeminiAndTranslatorScreen` con tarjeta japonesa giratoria **180°** para mostrador/mesa.
- [x] **TASK-704 (TDD)**: Implementar `AiPlanBuilderEngine` para permitir a la IA Gemini crear nuevos planes de viaje y mejorar/optimizar los planes existentes en la app (resolución de conflictos horarios + adición de nuevas jornadas y tramos de Shinkansen/transporte).
- [x] **TASK-705 (TDD)**: Ampliar `OfflineTranslatorEngine` con diccionario exhaustivo bidireccional, enrutamiento inteligente de idioma (`isJapaneseText`) y regla estricta de no-fuga de caracteres japoneses a la locución TTS en español/inglés.
- [x] **TASK-706 (TDD)**: Refactorizar `translateToJapanese` en `OfflineTranslatorEngine` garantizando traducciones directas, fieles y concisas sin palabras adicionales inventadas (cero boilerplate).
- [x] **TASK-707 (TDD)**: Implementar la Traducción Visual con Cámara (OCR con ML Kit Japanese Text Recognition + Procesamiento en `OfflineTranslatorEngine.translateOcrJapaneseText` + Interfaz de Cámara/Galería en `VoiceTranslatorScreen`).
- [x] **TASK-708 (TDD)**: Traducción Fiel de Voz Bidireccional (`SPEC-06 RF-06.1.2.6`): Refactorizar `translateFromJapaneseSpeech` y `translateToJapanese` para realizar traducción literal y fiel (sin respuestas inventadas de dependiente ni mensajes meta de chatbot), integrar traductor híbrido en tiempo real (Google Translate / Gemini online + diccionario exhaustivo offline) y actualizar la interfaz para mostrar el texto japonés capturado, Romaji y traducción en español antes de locutar.
- [x] **TASK-709 (TDD)**: Método Amigable para Obtener y Configurar la Clave de API de Gemini (`SPEC-06 RF-06.3`): Escribir tests unitarios e implementar `GeminiApiKeyHelper` (enlace oficial a Google AI Studio, validación de formato `AIzaSy...`, enmascaramiento y tutorial bilingüe en 4 pasos), e implementar componente `GeminiApiKeySetupCard` con botón directo a Google AI Studio, guía interactiva desplegable, pegado automático con 1 toque desde el portapapeles, selector de visibilidad (ojo) y recordatorio de tranquilidad offline, integrándolo en `GeminiAndTranslatorScreen` y en `SettingsScreen`.
- [x] **TASK-710 (TDD)**: Desacoplamiento de la Configuración de la API Key hacia Ajustes (`SPEC-06 RF-06.3`): Trasladar de forma exclusiva la configuración completa de la API Key de Gemini a la pantalla de Ajustes (`SettingsPreferencesScreen`), dejando la pantalla del Asistente IA (`AiAssistantScreen`) limpia y enfocada con enlace de acceso directo (*"⚙️ Configurar en Ajustes"*) hacia la sección de configuración.

---

## Hito 8B: Herramienta 7 — Previsión Meteorológica con Open-Meteo para Planificación de Usuario e IA (`SPEC-07`)

- [x] **TASK-751 (TDD)**: Escribir tests unitarios para `OpenMeteoWeatherUseCase` (parseo de JSON de `api.open-meteo.com/v1/forecast`, clasificación de códigos WMO `0..99`, cálculo de idoneidad `OUTDOOR_OPTIMAL` vs `INDOOR_RECOMMENDED` e índice de visibilidad del Monte Fuji).
- [x] **TASK-752 (TDD)**: Implementar semillas offline (`SeedJapanWeatherCatalog`) para Tokio, Kioto, Osaka y Hakone/Monte Fuji y cliente HTTP con fallback 100% offline.
- [x] **TASK-753 (TDD)**: Integrar la previsión meteorológica de Open-Meteo en `AiPlanBuilderEngine.optimizePlanWithWeather` y `GeminiAssistantEngine` para que la IA adapte el itinerario según lluvia/sol.
- [x] **TASK-754 (TDD)**: Integrar tarjetas y controles interactivos de **Tiempo Open-Meteo** tanto en el Dashboard como en el Planificador de Viaje por días y en el Optimizador IA de `MainScreen.kt`.
- [x] **TASK-755 (TDD)**: Extracción y Sincronización Automática de Ciudades del Tiempo desde Transportes y Planes de Viaje (`SPEC-07 RF-07.5`): Implementar `PlanWeatherCityExtractor` para resolver destinos de `TransportSegmentItem` y `ItineraryStopItem`, integrar `syncCitiesFromPlan` en `SharedWeatherForecastStore` con memoria de exclusión de eliminaciones manuales, botón de sincronización y persistencia offline.
- [x] **TASK-756 (TDD)**: Búsqueda Predictiva de Ciudades y Geocodificación Automática de Coordenadas de Google Maps (`SPEC-07 RF-07.6`): Escribir tests unitarios e implementar `CityGeocodingHelper` con catálogo ampliado offline (>50 destinos), resolución híbrida con Google Maps (`android.location.Geocoder`) y Open-Meteo Geocoding, e integración en `CityWeatherEditorModalSheet` con lista predictiva reactiva mientras el usuario escribe, autocompletado de coordenadas y botón de geocodificación automática.

---

## Hito 9: Cierre de Beta, Aprobación del Usuario y Release Estable `v1.0.0`

- [x] **TASK-801**: Ejecutar batería completa de tests unitarios e instrumentados y validar uso 100% en Modo Avión.
- [x] **TASK-802**: Revisión y aprobación del usuario para pase a producción y publicación de la versión 1.0.0 estable.
- [x] **TASK-803**: Merge a `main` vía Gitflow (`release/1.0.0`), generación de tag `v1.0.0`, compilación de APK final y publicación en **GitHub Releases**.

## Hito 10: Hotfix y Robustez `v1.0.1` (Modelos Canónicos IA sin 404, Cámara OCR y Firma Play Store)

- [x] **TASK-116 (TDD)**: Corrección de error 404 en llamadas a Gemini Cloud mediante alias canónicos oficiales (`gemini-flash-latest`, `gemini-pro-latest`, `gemini-2.5-flash`, `gemini-2.5-pro`, `gemini-flash-lite-latest`), migración automática reactiva de identificadores deprecados en `UserPreferencesRepository` y tests unitarios de migración y endpoints.
- [x] **TASK-117 (TDD)**: Reparación del flujo de apertura de cámara y permisos en runtime en el traductor visual de carteles (`SPEC-06`), verificación previa de `CAMERA` con launcher de permisos, captura en alta resolución mediante `FileProvider` con fallback a vista previa, y selector robusto para Google Lens (`com.google.ar.lens`), Google Traductor y cámara del sistema con soporte en `<queries>`.
- [x] **TASK-118 (DevOps)**: Pipeline completo de firma y generación de artefactos para Google Play Store (`signingConfigs.release`), creación de plantilla `keystore.properties.example`, generación dual de Android App Bundle firmado (`app-release.aab`) y APK firmado (`app-release.apk`).
- [x] **TASK-804**: Publicación de release `v1.0.1` en GitHub Releases con notas de cambio completas y adjuntos de `.aab` firmado para Google Play Store y APKs firmados.

## Hito 11: Optimización de Rendimiento IA y Modelos Verificados `v1.0.2`

- [x] **TASK-119 (TDD)**: Adopción de `Gemini Flash-Lite` (`gemini-flash-lite-latest`) como modelo recomendado predeterminado (<1s de latencia), ampliación de `readTimeout` a 30s en peticiones HTTP y 25s en pruebas de conexión, soporte nativo de claves con prefijo `AQ.`, clarificación de 429 para modelos PRO en el nivel gratuito y retirada de modelos 404 deprecados por Google.
- [x] **TASK-805**: Publicación de release `v1.0.2` en GitHub Releases con compilación completa firmada para Google Play Store (`.aab` y `.apk`).

## Hito 12: Resolución de Issues de GitHub (#1, #2, #3)

### Issue #1: Asistente de IA (Markdown, Input Multilínea, Teclado y Cancelación)
- [x] **TASK-120 (TDD)**: Parser y Renderizado de Markdown en Respuestas de IA (`Issue #1 / SPEC-06 RF-06.4`):
  - Escribir tests unitarios en `MarkdownTextParserTest.kt` validando la tokenización y estructuración de encabezados (`#`, `##`, `###`), texto en negrita (`**...**`), cursiva (`*...*`), viñetas (`* `, `- `, `• `), bloques de código inline/fenced y párrafos limpios.
  - Implementar `MarkdownTextParser.kt` y componente composable `MarkdownText.kt` con soporte de `AnnotatedString` y estilos tipográficos Material 3.
  - Integrar `MarkdownText` en la tarjeta de respuesta del Asistente IA (`MainScreen.kt`), sustituyendo el `Text` plano anterior.
- [x] **TASK-121 (TDD)**: UX de Entrada, Input Multilínea, Teclado e IME Padding (`Issue #1 / SPEC-06 RF-06.4`):
  - Modificar el campo de consulta a `singleLine = false`, `minLines = 2`, `maxLines = 5` para redacción ergonómica.
  - Añadir soporte para envío rápido desde el teclado mediante `KeyboardOptions(imeAction = ImeAction.Send)` y `KeyboardActions(onSend = { ... })`.
  - Integrar `imePadding()` y gestión de scroll para que el teclado no tape el campo de entrada.
  - Implementar cierre automático del teclado virtual (`LocalSoftwareKeyboardController.current?.hide()`) y deselección de foco al enviar la consulta.
- [x] **TASK-122 (TDD)**: Cancelación Reactiva de Consulta en Progreso (`Issue #1 / SPEC-06 RF-06.4`):
  - Controlar la corrutina de consulta mediante `activeQueryJob: Job?`.
  - Mostrar botón `❌ Cancelar consulta` visible y accesible mientras `isLoading == true`.
  - Cancelar la petición inmediatamente al hacer click, restaurando la UI y mostrando mensaje de estado informativo.

### Issue #2: Ratio de Divisas no se Persiste y Actualización Periódica
- [x] **TASK-123 (TDD)**: Serialización y Persistencia de Tasas de Cambio (`Issue #2 / SPEC-02 RF-02.3`):
  - Escribir tests unitarios en `ExchangeRatePersistenceTest.kt` validando `ExchangeRateJsonCodec.exportToJson` e `importFromJson` (preservando fecha, divisa base, mapa de tasas, isSeedData y timestamp).
  - Implementar `ExchangeRateJsonCodec` y `SharedCurrencyRateStore` con métodos `init`, `loadFromDisk`, `saveToDisk`, `updateSnapshot` y `syncRatesOnline`.
- [x] **TASK-124 (TDD)**: Tarea Periódica en Segundo Plano con WorkManager (`Issue #2 / SPEC-02 RF-02.4`):
  - Implementar `CurrencySyncWorker` (`CoroutineWorker`) y `CurrencySyncScheduler` con periodicidad de 24 horas y restricción de red conectada.
  - Escribir tests unitarios para `CurrencySyncWorker` verificando ejecución exitosa y actualización de `SharedCurrencyRateStore`.
- [x] **TASK-125 (TDD)**: Integración en la Pantalla del Conversor de Divisas (`Issue #2 / SPEC-02 RF-02.3`):
  - Conectar `CurrencyToolScreen` en `MainScreen.kt` con `SharedCurrencyRateStore.rateState.collectAsState()`.
  - Asegurar que al pulsar "🔄 Actualizar BCE", se invoque `syncRatesOnline()` y se mantenga persistido el resultado en disco.

### Issue #3: Predicciones Meteorológicas no se Persisten y Actualización Periódica
- [x] **TASK-126 (TDD)**: Serialización y Persistencia de Pronósticos Meteorológicos (`Issue #3 / SPEC-07 RF-07.7`):
  - Escribir tests unitarios en `WeatherForecastPersistenceTest.kt` validando `WeatherForecastJsonCodec.exportForecastsJson` e `importForecastsJson` (preservando días, temperaturas, WMO, idoneidad, fuji visibility y consejos).
  - Implementar `WeatherForecastJsonCodec` y actualizar `SharedWeatherForecastStore` para persistir `_forecastsState` en `saved_weather_forecasts.json` al guardar y cargar.
- [x] **TASK-127 (TDD)**: Tarea Periódica en Segundo Plano con WorkManager (`Issue #3 / SPEC-07 RF-07.7`):
  - Implementar `WeatherSyncWorker` (`CoroutineWorker`) y `WeatherSyncScheduler` con periodicidad de 6 horas y restricción de red conectada.
  - Registrar la inicialización de `WeatherSyncScheduler.schedule(this)` en `MainActivity.kt`.
- [x] **TASK-128 (TDD)**: Persistencia Inmediata en Actualizaciones Manuales y de Itinerario (`Issue #3 / SPEC-07 RF-07.7`):
  - Asegurar que `SharedWeatherForecastStore.updateCityForecasts` invoque inmediatamente `saveToDisk()`.

---

## Hito 12: Portabilidad JSON en Ajustes, Zonas Horarias ISO UTC, Sincronización en Segundo Plano y Bóveda Biométrica `v1.1.0`

### Bóveda de Documentos e Identidad (Seguridad Biometría y Bloqueo Estricto)
- [x] **TASK-129 (TDD)**: Seguridad Biometría y Anti-Bypass (`Issue #4 / SPEC-03 RF-03.2`):
  - Ajustar estado por defecto de `biometricLockEnabled` a `true` y prevenir bypass por cancelación de diálogo.
  - Añadir botón rápido de re-bloqueo.
- [x] **TASK-130 (TDD)**: Bloqueo Estricto por Defecto y Auto-Bloqueo (`Issue #5 / SPEC-03 RF-03.2`):
  - Implementar `VaultSecurityPolicy` con vista bloqueada por defecto sin persistencia en sesión.
  - Implementar temporizador de auto-bloqueo de 120s y bloqueo en background (`LifecycleEventObserver`).
- [x] **TASK-131 (TDD)**: Bloqueo Biométrico Obligatorio por Diseño (`SPEC-03 RF-03.2`):
  - Eliminar conmutador toggle de la UI para garantizar que no pueda desactivarse deliberada o accidentalmente.

### Portabilidad JSON en Ajustes, Zonas Horarias y Rediseño de Tarjetas
- [x] **TASK-132 (TDD)**: Formateo Regional de Fechas, Horas con Zona Horaria Local y Persistencia en ISO UTC (`SPEC-00`, `SPEC-04`, `SPEC-05`):
  - Implementar `TravelDateTimeFormatter` con formato regional `FormatStyle.FULL` y cálculo ISO UTC estándar.
  - Serialización con compatibilidad hacia atrás en `TravelPlanJsonCodec`.
- [x] **TASK-133 (TDD)**: Portabilidad JSON en Ajustes y Detector Inteligente de Contenido (`SPEC-00`, `SPEC-03`, `SPEC-04`):
  - Implementar `JsonContentDetector` para clasificar e inspeccionar planes, guías, billetes y backups completos.
  - Implementar `JsonDataPortabilityCard` integrado en Ajustes con Storage Access Framework (SAF).
- [x] **TASK-134 (UI)**: Rediseño Ergonómico de Botones en Tarjetas:
  - Rediseñar botones en `ItineraryStopCard` y `TransportSegmentCard` con altura reducida y disposición limpia.
- [x] **TASK-135**: Publicación de release `v1.1.0` en GitHub Releases con compilación completa firmada para Google Play Store (`.aab` y `.apk`) y APK debug.

---

## Hito 13: Traductor de Texto Bidireccional y Locución en Japonés (Issue #8)

### Traducción de Texto y Locución en Japonés (Issue #8)
- [x] **TASK-136 (TDD)**: Extracción y Pruebas Exhaustivas de `OfflineTranslatorEngine` (`Issue #8 / SPEC-06`):
  - Extraer `OfflineTranslatorEngine.kt` a archivo propio en `feature.ai` y crear test suite `OfflineTranslatorEngineTest`.
  - Ampliar diccionario para consultas frecuentes de viaje (restaurante, bebidas con cantidades, precios, aseos, direcciones, cortesía) en español e inglés.
- [x] **TASK-137 (TDD/UI)**: Corrección de Audio en Frases Habituales en Japonés (`Issue #8 / SPEC-06`):
  - Corregir reproducción de audio en frases habituales de prueba de la Tarjeta 1 para que hable en japonés (`Locale.JAPAN` / `ja-JP`) en vez de español.
- [x] **TASK-138 (UI)**: Sección Dedicada de Traductor de Texto (Español ➔ Japonés) (`Issue #8 / SPEC-06`):
  - Añadir tarjeta dedicada con `OutlinedTextField` multilínea, botón `🇯🇵 Traducir a Japonés`, visualización de caracteres Kanji/Kana con fonética Romaji, botón `🔊 Escuchar en Japonés` y fichas de preguntas rápidas.

---

## Hito 14: Adjuntos PDF en Pasaporte/DNI, Ciclo de Vida de Bóveda y Consolidación de Frases de Viaje

### Bóveda de Pasaporte y DNI (`SPEC-03`)
- [x] **TASK-408 (TDD)**: Soporte de PDF e Imagen en Pasaporte y DNI (`SPEC-03 RF-03.1`):
  - Implementar en `SharedDocumentsAndGuidesStore` los métodos `getIdDocumentFile`, `saveIdDocument` y `deleteIdDocument` para soportar extensiones `.pdf`, `.jpg`, `.jpeg`, `.png`, `.webp` en los slots `passport_data_page`, `dni_front` y `dni_back`.
  - Diseñar `IdDocumentSection` con soporte interactivo para PDFs (tarjeta de documento PDF con tamaño, apertura en visor PDF externo) e imágenes (miniatura táctil con zoom full-screen), con botones de acción para `📸 Cámara`, `🖼️ Galería`, `📄 Adjuntar PDF` y `🗑️ Eliminar`.
- [x] **TASK-409 (TDD)**: Prevención de Re-bloqueo Biométrico en Captura/Selección Externa (`SPEC-03 RF-03.3`):
  - Eliminar el bloqueo prematuro en `ON_PAUSE` del ciclo de vida en `PassportAndCustomsScreen`.
  - Implementar flag `isLaunchingExternalPicker` para evitar que la transición a la cámara del sistema o al selector de archivos SAF active el bloqueo en `ON_STOP` o relance el modal biométrico al volver a la app.

### Traductor de Voz y Texto (`SPEC-06`)
- [x] **TASK-711 (TDD)**: Consolidación y Desduplicación de Frases Habituales de Viaje en Japonés (`SPEC-06 RF-06.1`):
  - Retirar las listas de preguntas/frases embebidas en las 3 tarjetas de traducción individuales (Texto, Voz JP->ES y Voz ES->JP).
  - Implementar en `OfflineTranslatorEngine` la colección consolidada y desduplicada `travelerQuickPhrases` con categorías estructuradas.
  - Diseñar la tarjeta unificada al pie de la pantalla en `VoiceTranslatorScreen` con visualización obligatoria dual (Español y Japonés con Kanji/Kana y fonética Romaji) y botón directo de audio `🔊` en japonés (`Locale.JAPAN`).

---

## Hito 15: Traductor de Texto en Sección Independiente con Botones Estilo Pestaña

### Navegación de Traductor y Pestaña de Texto (`SPEC-06`)
- [x] **TASK-712 (TDD)**: Modelo de Modos de Navegación por Pestañas del Traductor (`SPEC-06 RF-06.1.3`):
  - Definir enum `TranslatorTabMode` (`VOICE`, `TEXT`, `CAMERA`) con identificador, icono y etiquetas localizadas en `OfflineTranslatorEngine.kt`.
  - Crear pruebas unitarias en `OfflineTranslatorEngineTest.kt` validando los modos, títulos y filtrado de contenido de cada pestaña.
- [x] **TASK-713 (UI/TDD)**: Pestaña Independiente de Traductor de Texto en `VoiceTranslatorScreen` (`SPEC-06 RF-06.1.3`):
  - Actualizar la barra superior de modos con botones de estilo pestaña (`AppOptionButton`) para `🎙️ Voz`, `✍️ Texto` y `📷 Carteles / Cámara`.
  - Desplegar la sección de Traductor de Texto exclusivamente en la pestaña `TEXT`, con tarjeta completa de traducción (entrada multilínea, botones de traducción y locución, visor de caracteres japoneses, romaji y audio).
  - Incorporar en la pestaña `TEXT` la opción de seleccionar/insertar frases rápidas de viaje en el campo de texto.
  - Mantener en la pestaña `VOICE` las tarjetas de conversación bidireccional y las frases habituales con audio al pie.
  - Mantener en la pestaña `CAMERA` el traductor OCR de carteles y menús.
- [x] **TASK-714 (TDD/UI)**: Orientación EXIF de Cámara, Rotación 90° y Selección de Área/Recorte para OCR (`SPEC-06 RF-06.1.4`):
  - Implementar decodificación con corrección de orientación EXIF (`ExifInterface`) para evitar que las fotos tomadas con la cámara nativa se giren o carguen apaisadas.
  - Implementar botón de rotación manual `🔄 Girar 90°` en la tarjeta de foto capturada.
  - Implementar selección de región de imagen (`ImageCropPreset` y `calculateCropBounds`) con presets rápidos (`Toda la foto`, `Superior`, `Centro`, `Inferior`, `Personalizada`).
  - Implementar diálogo/panel `SelectImageRegionDialog` con controles de márgenes y previsualización interactiva de la caja delimitadora para enviar únicamente el sub-mapa de bits recortado a ML Kit OCR.

---

## Hito 16: Robustez de Adjuntos PDF en Bóveda, Prevención Definitiva de Rebloqueo Biométrico y Fix Despliegue ADB Pixel 9

### Bóveda de Identidad y PDF (`SPEC-03`)
- [x] **TASK-715 (TDD)**: Detección Binaria de Archivos por Magic Bytes y Selector SAF en `SharedDocumentsAndGuidesStore` (`SPEC-03 RF-03.3`):
  - Implementar `detectFileFormatByMagicBytes` para identificar encabezados `%PDF-`, `0xFF, 0xD8` (JPEG) y `0x89, 0x50` (PNG).
  - Usar la detección binaria en `saveIdDocument` para asignar la extensión correcta (`pdf`, `jpg`, `png`) sin depender de nombres o tipos MIME imprecisos.
  - Migrar `pdfPickerLauncher` y selectores a `ActivityResultContracts.OpenDocument()` con `arrayOf("application/pdf", "*/*")` y persistencia de permisos URI.
- [x] **TASK-716 (UI/Lifecycle)**: Sesión Temporal con `rememberSaveable` y Prevención de Rebloqueo en Bóveda (`SPEC-03 RF-03.3`):
  - Migrar el estado de sesión a `var lastUnlockTimestamp by rememberSaveable { mutableStateOf(0L) }`.
  - Evaluar `isSessionValid = !VaultSecurityPolicy.isSessionExpired(lastUnlockTimestamp, System.currentTimeMillis())`.
  - Ajustar `Lifecycle.Event.ON_STOP` y `onDispose` para no invalidar `lastUnlockTimestamp` cuando `isLaunchingExternalPicker` es verdadero.
  - Sincronizar auto-bloqueo tras 120s o al pulsar el botón `🔒 Bloquear`.
- [x] **TASK-717 (DevOps)**: Optimización del Script de Despliegue ADB para Evitar Cuelgues en Pixel 9:
  - Eliminar la llamada a `android install` con delta-install que se cuelga en Android 15.
  - Usar directamente `adb install -r -d -t` con timeout de 35 segundos para una instalación fluida y sin bloqueos.

---

## Hito 17: Traducción Robusta de Carteles (Offline + Online) y Selector Multizona Estilo Google Fotos

### Motor de Traducción de Carteles y Menús (`SPEC-06`)
- [x] **TASK-718 (TDD)**: Diccionario Offline Ampliado y Tokenizador Morfológico para Carteles en `OfflineTranslatorEngine` (`SPEC-06 RF-06.1.6`):
  - Expandir el diccionario offline a más de 150 términos y expresiones categorizadas de viaje (carteles urbanos, estaciones, salidas, andenes, advertencias, normas, platos de comida, bebidas, precios, etc.).
  - Implementar segmentación y descomposición morfológica de frases compuestas en japonés para traducir cada vocablo o kanji conocido, eliminando por completo el mensaje genérico de *"Traducción directa no disponible offline"*.
  - Crear tests unitarios en `OfflineTranslatorEngineTest.kt` validando carteles de estaciones, comida, precios, advertencias y segmentación morfológica.
- [x] **TASK-719 (Async/TDD)**: Traducción Fluida en Línea en Segundo Plano para OCR (`SPEC-06 RF-06.1.6`):
  - Conectar el resultado de OCR en `VoiceTranslatorScreen` con `OnlineTranslatorClient` para consultar traducción contextual en línea en segundo plano cuando haya conexión y no esté forzado el modo offline.
  - Mostrar la traducción online contextual en una tarjeta destacada con botón de audio locutado, complementando los desgloses offline instantáneos.
- [x] **TASK-720 (UI/TDD)**: Selector de Zonas de Foto no Destructivo Estilo Google Fotos y Soporte Multizona (`SPEC-06 RF-06.1.5`):
  - Diseñar el diálogo/visor interactivo `SelectImageRegionDialog` con encuadre no destructivo sobre la foto original (sin recortar el archivo base): imagen completa, máscara atenuada (`scrim`), cuadrícula de tercios y los 4 brackets de esquinas icónicos `┌ ┐ └ ┘`.
  - Soporte Multizona (`OcrTextZone`): selector rápido de presets individuales (`Toda la foto`, `Superior`, `Centro`, `Inferior`, `Izquierda`, `Derecha`) y modo multizona con procesamiento en bloque y agrupación de resultados traducidos por zona (`📍 Zona 1`, `📍 Zona 2`, etc.).
- [x] **TASK-721 (UI/Touch)**: Arrastre Táctil Directo para Encuadre de Carteles y Maximización de Pantalla (`SPEC-06 RF-06.1.5`):
  - Reemplazo completo de deslizadores numéricos por gestos multitáctiles en Compose (`pointerInput` + `detectDragGestures`).
  - Redimensionamiento arrastrando con el dedo esquinas (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`) y bordes (`EDGE_TOP`, `EDGE_BOTTOM`, `EDGE_LEFT`, `EDGE_RIGHT`).
  - Desplazamiento/paneo completo del recuadro de encuadre tocando y arrastrando el interior (`MOVE_BOX`).
  - Maximización del área visual de la imagen en pantalla a 380.dp de altura y ancho completo (`usePlatformDefaultWidth = false`), permitiendo inspeccionar cómodamente los textos antes de traducir.

---

## Hito 18: Tickets de Compra, Tax-Free, Control de Gastos y Escaneo IA (Local + Cloud)

### Módulo de Comprobantes, Tax-Free y Presupuesto (`SPEC-08`)
- [x] **TASK-801 (SDD)**: Especificación Funcional de Tickets de Compra, Tax-Free y Control de Gastos:
  - Crear `docs/specs/08-shopping-receipts-tax-free-spec.md` detallando campos, reglas de 5.000 ¥, consumibles/generales/mixtos, control presupuestario y escaneo híbrido.
- [x] **TASK-802 (Analysis)**: Análisis Técnico y Arquitectura Clean Architecture:
  - Crear `docs/analysis/08-shopping-receipts-tax-free-analysis.md` detallando entidades, contratos y estrategia de testing.
- [x] **TASK-803 (TDD/Domain)**: Modelos de Dominio y Calculador de Reglas Tax-Free de Japón:
  - Definir `ReceiptProductItem`, `ShoppingReceipt`, `GoodsType`, `TaxFreeClaimStatus` y `ExpenseCategory` en `ShoppingReceiptsModels.kt`.
  - Implementar cálculo de subtotales, desglose impositivo al 8% y 10%, y umbral de 5.000 ¥ sin impuestos.
- [x] **TASK-804 (TDD/Domain)**: Analizador OCR de Recibos Japoneses (`JapaneseReceiptOcrParser`):
  - Reconocimiento de cadenas principales (*Don Quijote, Bic Camera, Uniqlo, 7-Eleven, Lawson, FamilyMart*).
  - Detección de fechas gregorianas y era Reiwa, extracción de productos, subtotales, `内消費税` e indicadores `8% / 10%`.
- [x] **TASK-805 (TDD/Domain)**: Generador de Informes Consolidados de Tax-Free (`TaxFreeReportGenerator`):
  - Agrupación por tienda y fecha, cálculo de impuesto recuperable en ¥ y EUR con Frankfurter API.
  - Exportación en formatos Texto, Markdown y JSON portable.
- [x] **TASK-806 (TDD/Domain)**: Módulo de Control de Presupuesto y Gastos del Viaje (`TravelBudgetSummary`):
  - Acumulados totales de gasto en ¥ y EUR, desgloses por categoría (`ExpenseCategory`), por día y por ciudad.
- [x] **TASK-807 (TDD/Data)**: Almacén Persistente y Gestor de Tickets (`ShoppingReceiptsStore`):
  - Persistencia de comprobantes en almacenamiento privado (`filesDir`), soporte CRUD reactivo y cálculo de balances.
- [x] **TASK-808 (TDD/AI)**: Escáner Inteligente Híbrido con Gemini Cloud Vision y Fallback a ML Kit (`AiReceiptScannerEngine`):
  - Extracción multimodal vía Gemini Vision con fallback automático a ML Kit local offline ante errores o falta de red/clave.
- [x] **TASK-809 (Test)**: Suite de Tests Unitarios Exhaustiva (`TaxFreeReceiptsTest`):
  - Verificación de cálculos fiscales, reglas de umbral, parser OCR japonés, informes Tax-Free, control de gastos y escaneo híbrido.
- [x] **TASK-810 (UI/Integration)**: Pantalla de Tickets, Tax-Free y Presupuesto (`ShoppingReceiptsScreen`):
  - Pantalla adaptativa con listado de tickets, formulario de alta manual / OCR / Cloud IA (cámara, Drive o archivo local).
  - Resumen visual de gastos (¥ y EUR) por categoría y tarjeta de saldo para reembolso Tax-Free.
  - Generador y visor del informe de solicitud de Tax-Free para aduanas con exportación a portapapeles/archivo.
  - Integración en la navegación principal y selector de herramientas (`TAX_FREE_RECEIPTS`).
- [x] **TASK-811 (Lifecycle/Android)**: Prevención de Reinicio de App al Cambiar de Aplicación en Android:
  - Configuración de `android:launchMode="singleTask"` e implementación de `onNewIntent` en `MainActivity`.
  - Configuración de `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|smallestScreenSize|uiMode"` para evitar destrucciones innecesarias de la Activity.
  - Persistencia reactiva de `lastSelectedDestination` en `UserPreferencesRepository` para restaurar la herramienta activa tras suspensiones o cierres en segundo plano.
- [x] **TASK-812 (UI/UX)**: Estandarización de Filas de Botones y Selector de Origen de Tickets (Cámara / Local / Drive):
  - Unificación visual y ergonómica de botones de acción a una altura idéntica de `48.dp` con esquinas de `12.dp` en `ShoppingReceiptsScreen`.
  - Eliminación de iconos huérfanos confusos (`🖼️`) y sustitución por etiquetas autoexplicativas (`📁 Local / ☁️ Drive`).
  - Creación de diálogo selector de fuente de escaneo (`ScanSourceSelectorDialog`) con tres opciones independientes: `📷 Cámara de Fotos`, `📁 Almacenamiento Local / Galería` y `☁️ Google Drive`.
  - Integración nativa de `android.graphics.pdf.PdfRenderer` para decodificar y rasterizar facturas o tickets PDF directamente a imagen para el OCR / IA sin dependencias externas.
- [x] **TASK-813 (TDD/Domain)**: Analizador OCR Trilingüe (Japonés, Inglés, Español):
  - Extensión de `JapaneseReceiptOcrParser` para reconocer tickets en inglés (recibos Duty-Free internacionales: `SUBTOTAL`, `TAX`, `VAT`, `TOTAL AMOUNT`) y español (`BASE IMPONIBLE`, `IVA 10%`, `TOTAL FACTURA`, `TOTAL A PAGAR`).
  - Detección de formatos de fecha ISO (`YYYY/MM/DD`), europeos (`DD/MM/YYYY`) y era japonesa (`R07/03/15`).
  - Clasificación multilingüe de categorías de gasto y tests unitarios automatizados en `TaxFreeReceiptsTest`.






