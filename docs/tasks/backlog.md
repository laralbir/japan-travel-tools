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
