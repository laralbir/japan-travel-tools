# Changelog

Todos los cambios notables en este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/spec/v2.0.0.html) y [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/).

## [Unreleased]

## [1.1.0] - 2026-10-27

### Portabilidad JSON Unificada, Zonas Horarias ISO UTC, Sincronización en Segundo Plano y Bóveda Biométrica — Japan Travel Tools 1.1.0 🇯🇵🚀

#### 📦 Portabilidad JSON en Ajustes (Local / Google Drive SAF) y Detección Automática Inteligente:
- **Tarjeta Centralizada en Ajustes (`JsonDataPortabilityCard`)**: Acceso directo y unificado desde `SettingsPreferencesScreen` para importar y exportar datos mediante Storage Access Framework (SAF) nativo de Android, permitiendo leer y guardar archivos `.json` en almacenamiento local (Descargas, Documentos) o en Google Drive.
- **Portabilidad de Guías de Viaje (`TravelGuideArticle`) y Reservas/Billetes (`TravelDocumentItem`)**: Exportación e importación completa en formato JSON legible con opciones de reemplazo total o fusión inteligente sin sobrescribir registros existentes (`SharedDocumentsAndGuidesStore.importGuides`, `SharedDocumentsAndGuidesStore.importDocuments`).
- **Detector de Contenido JSON Inteligente (`JsonContentDetector`)**: Inspección y tipado automático de cualquier archivo JSON seleccionado (`TRAVEL_PLAN`, `TRAVEL_GUIDES`, `TRAVEL_DOCUMENTS`, `FULL_BACKUP`) con presentación de resumen de elementos encontrados y confirmación guiada antes de importar.

#### 🕒 Formateo Regional de Fechas, Horas con Zona Horaria Local y Persistencia en ISO UTC (`TravelDateTimeFormatter`):
- **Fecha Completa en Formato Regional del Sistema**: Visualización localizada de la fecha completa con `FormatStyle.FULL` (`Locale.getDefault()`, ej. *"Lunes, 15 de marzo de 2027"* / *"Monday, March 15, 2027"*) en las tarjetas de itinerario y transporte.
- **Zona Horaria Local Explícita con Desfase UTC**: Indicación clara de la hora local de cada ciudad o parada junto con su huso y desfase UTC (ej. Múnich = `UTC+1 · CET`, Osaka/Tokio = `UTC+9 · JST`) tanto en `ItineraryStopCard` como en `TransportSegmentCard`.
- **Persistencia Canónica en ISO UTC**: Cálculo y almacenamiento estándar en formato ISO UTC (`Instant.toString()`, ej. `2027-03-15T11:25:00Z`) para actividades e hitos de transporte (`startTimeUtcIso`, `endTimeUtcIso`, `departureTimeUtcIso`, `arrivalTimeUtcIso`) con compatibilidad retroactiva total en `TravelPlanJsonCodec`.
- **Selector y Detección de Zona Horaria en Editor**: Incorporación de chips rápidos en `ItineraryStopEditorModalSheet` para asignar zona horaria (Japón `UTC+9`, Múnich `UTC+1`, Madrid `UTC+1`) e inferencia automática mediante coordenadas o nombre de lugar (`inferZoneIdFromCoordinatesOrLocation`).

#### 🎴 Rediseño Ergonómico y Compacto de Botones en Tarjetas:
- **Rediseño No Invasivo en `ItineraryStopCard`**: Sustitución de dos bloques masivos apilados (`AppButtonRow` de 120dp) por una fila única, limpia y estilizada de 34dp de altura con botones proporcionados para `🗺️ Mapa`, `📅 Cal.`, `✏️ Editar` y `🗑️`, reduciendo la invasividad visual en más del 70%.
- **Rediseño Compacto en `TransportSegmentCard`**: Sustitución del botón masivo por una fila compacta de acción (`✏️ Editar`, `🗑️ Eliminar`).

#### 🔒 Bóveda de Documentos e Identidad (Bloqueo Biométrico Obligatorio sin Toggle y Auto-Bloqueo — Issues #4 y #5):
- **Bloqueo Biométrico Obligatorio por Diseño**: La verificación biométrica (huella, rostro o PIN) es estrictamente obligatoria si el hardware dispone de credenciales configuradas. Se retiró por completo el conmutador (toggle) de la interfaz para evitar riesgos de desprotección accidental o deliberada de documentos confidenciales (Pasaporte y DNI).
- **Estado Bloqueado Obligatorio por Defecto (`VaultSecurityPolicy`)**: Eliminación del uso de `rememberSaveable` para el estado de desbloqueo. Cada vez que el usuario accede a la pantalla de Identificación/Pasaporte, la vista nace obligatoriamente bloqueada mostrando la tarjeta `"Identificación Bloqueada"`, garantizando que los documentos nunca se expongan sin verificación biométrica previa (#5).
- **Auto-Bloqueo de Seguridad (Temporizador de 120s)**: Incorporación de cierre automático de sesión de visualización tras 120 segundos (2 minutos) continuos de desbloqueo para proteger la privacidad ante descuidos (#5).
- **Auto-Bloqueo en Segundo Plano (`LifecycleEventObserver`)**: Bloqueo automático e inmediato de la bóveda al pausar, minimizar la aplicación o apagar la pantalla del dispositivo (#5).
- **Protección Anti-Bypass Estricta**: Eliminación de cualquier riesgo por el cual cancelar el diálogo de autenticación pudiera saltarse el control de biometría (#4).
- **Tarjeta de Protección Activa y Botón Unificado "Bloquear"**: Estandarización de la tarjeta informativa con el botón directo `"🔒 Bloquear"` visible siempre que los documentos estén desbloqueados (#4, #5).

#### ⛅ Previsión Meteorológica (Persistencia Offline y Actualización Periódica en Segundo Plano — Issue #3):
- **Persistencia en Disco de Pronósticos (`SharedWeatherForecastStore` & `WeatherForecastJsonCodec`)**: Serialización y almacenamiento de la lista de pronósticos de 7 días (`WeatherDayForecast`) por ciudad en `saved_weather_forecasts.json`. Al reiniciar la app se cargan inmediatamente las predicciones previamente descargadas (#3).
- **Actualización Automática Desatendida (`WeatherSyncWorker` & `WorkManager`)**: Programación periódica cada 6 horas con restricciones de red conectada para actualizar los pronósticos de todas las ciudades del itinerario en segundo plano (#3).
- **Persistencia Inmediata al Actualizar**: Sincronización automática a disco en cada actualización manual o de itinerario (#3).

#### 💱 Conversor de Divisas (Persistencia Offline y Actualización Periódica en Segundo Plano — Issue #2):
- **Persistencia en Disco de Tasas (`SharedCurrencyRateStore` & `ExchangeRateJsonCodec`)**: Almacenamiento automático y reactivo de las tasas actualizadas del BCE en el almacenamiento interno privado (`saved_currency_rates.json`), garantizando que al reiniciar la app se conserven las tasas cacheadas sin revertir a datos semilla (#2).
- **Actualización Automática Desatendida (`CurrencySyncWorker` & `WorkManager`)**: Programación periódica cada 24 horas con restricciones de red conectada para sincronizar las tasas de cambio de Frankfurter API en segundo plano (#2).
- **Indicador Dinámico de Sincronización**: Visualización de la fecha real del tipo de cambio cacheado o del modo offline en la tarjeta principal del conversor (#2).

#### 🤖 Asistente de IA (Renderizado Markdown Nativo, Entrada Multilínea y Cancelación — Issue #1):
- **Renderizado Nativo de Markdown (`MarkdownText` & `MarkdownTextParser`)**: Procesamiento y visualización con estilos enriquecidos para encabezados (`#`, `##`, `###`), negritas (`**texto**`), cursivas (`*texto*`), viñetas (`•`), listas numeradas y bloques/términos en código monoespaciado, eliminando la exposición de caracteres Markdown en crudo (#1).
- **Entrada Multilínea Ergonómica**: Ampliación del campo de texto a multilínea (`minLines = 2`, `maxLines = 5`) con `imePadding()` para evitar solapamientos visuales con el teclado virtual (#1).
- **Acción IME y Cierre Automático del Teclado**: Soporte para disparar consultas directamente desde la tecla de envío del teclado (`ImeAction.Send`) y ocultamiento inmediato del teclado virtual con deselección de foco al consultar (#1).
- **Cancelación Reactiva de Consultas**: Incorporación de botón `❌ Cancelar consulta` durante la carga que cancela de forma inmediata la corrutina en progreso (#1).

#### 📐 Reglas Generales de Estilo y Formato de Código:
- **Regla POSIX EOF Newline Obligatoria (`.editorconfig`)**: Creación de la configuración estándar de proyecto `.editorconfig` (`insert_final_newline = true`, `end_of_line = lf`, `trim_trailing_whitespace = true`) e incorporación de la regla obligatoria en `AGENTS.md` para garantizar que todos los archivos finalicen con salto de línea estándar y evitar diffs espurios de formateo en Git.

## [1.0.2] - 2026-10-26

### Parche de Rendimiento IA y Modelos Verificados en Tiempo Real — Japan Travel Tools 1.0.2 🇯🇵⚡

#### ⚡ `Gemini Flash-Lite` Establecido como Modelo Recomendado Predeterminado:
- **Respuesta Instantánea sin Timeouts (<1s)**: Verificación en tiempo real contra los servidores de Google demostrando que `gemini-flash-lite-latest` responde consistentemente en **0.58s - 0.87s**, erradicando por completo los bloqueos por timeout de red.
- **Ampliación de Timeouts HTTP**: Aumento del `readTimeout` a 30s en consultas de IA y 25s en pruebas de conexión para evitar cancelaciones prematuras en conexiones móviles lentas.
- **Clarificación de Cuotas Gratuitas vs Modelos PRO (Error 429)**: Diagnóstico específico informando que Google AI Studio asigna una cuota de 0 peticiones/min a los modelos PRO en el nivel gratuito (requiere tarjeta/facturación en Google Cloud). Se incluye aviso guiado recomendando conmutar a `Gemini Flash-Lite` o `Gemini 3.5 Flash`.
- **Eliminación de Modelos 404 Obsoletos**: Retirada definitiva de `gemini-2.5-flash` y `gemini-2.5-pro` (marcados por Google como inaccesibles para nuevos usuarios) e incorporación de `gemini-3.5-flash` (5.5s, gratuito y estable).
- **Soporte de Formato Moderno de Clave `AQ.`**: Reconocimiento oficial en `isValidApiKeyFormat` y enmascaramiento para las claves de última generación generadas por Google AI Studio que inician con `AQ.`.

## [1.0.1] - 2026-10-26

### Parche Oficial de Corrección y Firma para Google Play Store — Japan Travel Tools 1.0.1 🇯🇵📱

#### 🧠 Corrección de Error 404 en Modelos Gemini (Alias Canónicos de Google):
- **Diagnóstico y Causa Raíz**: Google retiró e invalidó los endpoints de versión previos de `gemini-1.5-flash`, `gemini-2.0-flash` y `gemini-1.5-pro` en la API pública `v1beta`, devolviendo un error HTTP 404 (*Model Not Found*).
- **Adopción de Alias Canónicos Oficiales**:
  - `gemini-flash-latest`: Apunta siempre a la versión activa más reciente de Gemini Flash recomendada por Google (por defecto).
  - `gemini-pro-latest`: Apunta siempre a la versión activa más reciente de Gemini Pro para razonamiento profundo de itinerarios.
  - Inclusión de versiones fijas estables: `gemini-2.5-flash`, `gemini-2.5-pro` y `gemini-flash-lite-latest`.
- **Migración Automática de Preferencias en Dispositivos**: `UserPreferencesRepository` intercepta de manera transparente cualquier clave de modelo deprecada almacenada previamente en el terminal del usuario y la actualiza en tiempo real a su alias canónico correspondiente, eliminando cualquier intervención manual por parte del viajero.

#### 📷 Reparación Integral del Botón de Cámara en Traducción Visual (OCR de Carteles):
- **Permisos en Tiempo de Ejecución**: Integración del launcher de permisos `ActivityResultContracts.RequestPermission()` para solicitar `android.permission.CAMERA` de forma interactiva y amigable antes de abrir la cámara, evitando excepciones de seguridad silenciosas.
- **Captura en Alta Resolución vía FileProvider**: Configuración de `FileProvider` con directorio caché (`file_paths.xml`) para tomar fotografías a resolución completa (`ActivityResultContracts.TakePicture()`) con fallback a vista previa, garantizando una nitidez óptima para el reconocimiento óptico de caracteres de kanji y kana japoneses por Google ML Kit.
- **Acceso Directo a Google Lens y Google Traductor**: Renovación de la tarjeta de Realidad Aumentada con botones dedicados `🌐 Abrir Google Lens` y `🌐 Google Traductor`, con declaración de paquetes en `<queries>` (`com.google.ar.lens`, `com.google.android.apps.translate`) y fallback automático a la cámara nativa del sistema (`MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA`) o navegador.

#### 📦 Compilación Completa y Firma para Google Play Store:
- **Configuración de Firma Release (`signingConfigs.release`)**: Soporte en `app/build.gradle.kts` para firmas automáticas con `release.keystore`, plantilla `keystore.properties.example` y variables de entorno del pipeline de CI/CD.
- **Generación Dual de Artefactos de Publicación**:
  - **Android App Bundle firmado (`app-release.aab`)**: Paquete completo listo para subir a Google Play Console.
  - **APK firmado (`app-release.apk`)**: Paquete para instalación directa en dispositivos.
  - **APK Debug (`app-debug.apk`)**: Paquete para desarrollo y pruebas en dispositivos autorizados vía ADB.

## [1.0.0] - 2026-10-25

### Release Oficial Estable — Japan Travel Tools 1.0.0 🇯🇵✈️
Primera versión oficial de producción para viajeros a Japón. Suite integral 100% offline-first desarrollada con Spec-Driven Development (SDD) y Test-Driven Development (TDD) sobre Android (Kotlin + Jetpack Compose), optimizada para smartphones insignia (**Google Pixel 10 Pro XL**, **Pixel 9 Pro XL**) y tablets (**TCL NXTPAPER 11 Plus**).

#### 🗺️ Herramientas Incluidas en la Suite v1.0.0:
1. **Relojes y Ubicación Dual (`SPEC-01`)**:
   - Hora local automática por GPS, hora de Japón (`Asia/Tokyo`) y hora del país de origen (`Europe/Madrid` u otra seleccionada).
   - Cálculo en tiempo real de diferencia horaria y fases del día (mañana, tarde, noche) para comunicarse con casa sin confusiones.
2. **Conversor de Divisas Offline-First (`SPEC-02`)**:
   - Conversión bidireccional instantánea `EUR <-> JPY` con tasas oficiales del Banco Central Europeo (Frankfurter API) y semilla offline.
   - Despliegue de teclado numérico decimal especializado.
3. **Bóveda de Documentos de Identidad con Zoom Pellizcando (`SPEC-03`)**:
   - Almacenamiento seguro y privado en `Context.filesDir` de fotos de Pasaporte, DNI y documentos personalizados (recetas médicas para aduanas japonesas *Yakkan Shoumei*, certificado de vacunas, visados, seguro de viaje y carnet internacional).
   - Visor a pantalla completa con zoom táctil (`pinch-to-zoom`) de `1.0x` a `5.0x` y alternador por doble toque.
   - Soporte de Bloqueo Biométrico / PIN configurable.
4. **Repositorio de Documentos y Creador de Guías (`SPEC-03`)**:
   - Bóveda de billetes de vuelo, reservas de hotel y JR Pass.
   - Creador y gestor de guías de viaje propias por regiones con texto enriquecido y fotografías offline.
5. **Planificador de Viaje con Itinerario y Mapas (`SPEC-04`)**:
   - Línea temporal de actividades por días en huso horario japonés.
   - Selectores de fecha nativos de Material 3 (`AppDatePickerField`), selectores de hora (`AppTimePickerField`) y controles numéricos incrementales con steppers (`AppNumberStepperField`).
   - Integración con Google Maps, geolocalización de coordenadas y exportación/importación en formato `JSON` legible (`japan-travel-plan.v1`).
6. **Planificador de Transportes Multimodal (`SPEC-05`)**:
   - Gestión de vuelos (internacionales y domésticos), trenes bala **Shinkansen / JR**, líneas de metro y autobuses.
   - Cálculo de escalas aéreas y transbordos entre andenes, número exacto de salida de estación (`Exit`) y dársena de autobús (`Bus Bay`).
   - Sistema de advertencias visuales y notificaciones push para recordatorios de facturación y hora de embarque.
7. **Asistente IA Híbrido (Google Gemini Cloud + Motor Experto Offline) (`SPEC-06`)**:
   - Conmutador táctil directo en cabecera `[ ⚡ Flash (Rápido) ]` vs `[ 🧠 PRO (Razonamiento) ]`.
   - Soporte de modelos oficiales: `Gemini 2.0 Flash`, `Gemini 1.5 Pro`, `Gemini 2.5 Pro`, `Gemini 2.0 Flash Lite` y `Gemini 2.5 Flash`.
   - Botón `⚡ Probar Conexión con Gemini` en Ajustes y alertas visibles ante fallos de conexión o claves inválidas.
   - Motor Experto On-Device integrado con más de 20 temáticas de Japón 100% offline.
   - **Traductor de Voz Fiel y Literal Bidireccional (`Japonés <-> Español/Inglés`)**: Traducción literal sin alucinaciones ni invención conversacional.
8. **Previsión Meteorológica Open-Meteo (`SPEC-07`)**:
   - Previsión a 7 días sin necesidad de API Key (modelos globales + JMA).
   - Porcentaje de visibilidad del Monte Fuji e idoneidad climática (`⛩️ Ideal Exterior` vs `🏛️ Recomendado Cubierto`).
   - Búsqueda predictiva de ciudades y geocodificación automática de coordenadas con Google Maps.
   - Sincronización automática de destinos desde planes y transportes.
9. **Selector de Cuenta Google del Sistema y Sincronización en Drive (`SPEC-00`)**:
   - Selector nativo de cuentas Android (`AccountManager.newChooseAccountIntent`) sin escritura manual.
   - Respaldo y restauración del bundle completo (`JapanTravelCloudSyncBundle`) directamente en Google Drive vía Android Storage Access Framework (`CreateDocument` / `OpenDocument`).
   - Fusión inteligente multi-dispositivo sin pérdidas de datos.
10. **Seguridad y Usabilidad Universal (`SPEC-00`)**:
    - Modales de confirmación interactivos y sistema de **Double Check** con interruptor obligatorio para borrados sensibles.
    - Soporte completo para temas Claro (*Washi*), Oscuro (*Sumi* OLED) y Sistema.

#### 🧪 Calidad, SDD y TDD
- 100% de la batería de tests unitarios superada sin errores.
- Flujo Gitflow completado y release final consolidada en `main`.

## [0.0.1-beta.30] - 2026-10-25

### Added
- **Selector Directo de Modelo PRO vs Flash en el Asistente IA (`AiAssistantScreen`) (`SPEC-06 RF-06.3`)**:
  - **Conmutador Rápido de 1 Toque en Pantalla Principal**: Componente visual integrado directamente en la cabecera del Asistente IA que permite alternar instantáneamente entre `[ ⚡ Flash (Rápido) ]` y `[ 🧠 PRO (Razonamiento) ]`, eliminando la necesidad de ir a Ajustes para cambiar de modelo.
  - **Insignia de Modo Reactiva**: Badge dinámico `🧠 PRO ACTIVO` vs `⚡ FLASH ACTIVO` con colores semánticos Material Design 3.
  - **Sub-selector de Variantes PRO**: Al seleccionar PRO, se despliega una barra horizontal con chips para elegir con un toque entre **Gemini 1.5 Pro** (estándar probado para itinerarios y consultas complejas) y **Gemini 2.5 Pro** (última generación de Google con razonamiento profundo avanzado).
- **Catálogo Oficial Ampliado con `Gemini 2.5 Pro` (`GeminiApiKeyHelper`)**:
  - Incorporación de `gemini-2.5-pro` junto con `gemini-1.5-pro`.
  - Helpers de dominio `isProModel(modelId)`, `getProModels()`, `getFlashModels()`, `PRO_DEFAULT_MODEL_ID` (`gemini-1.5-pro`) y `FLASH_DEFAULT_MODEL_ID` (`gemini-2.0-flash`).
  - Distinción visual en Ajustes (`GeminiApiKeySetupCard`) con prefijos `🧠 PRO`, `⚡ Flash` y contenedores semánticos diferenciados.
- **Batería de Tests Unitarios TDD (`GeminiApiKeyHelperTest`)**:
  - Tests unitarios que verifican la detección inequívoca de modelos Pro, constantes canónicas de defaults y presencia de variantes en el catálogo.

## [0.0.1-beta.29] - 2026-10-25

### Added
- **Diagnóstico Activo de Conexión y Fin del Fallback Silencioso en Gemini Cloud (`SPEC-06 RF-06.2 / RF-06.3`)**:
  - **Botón `⚡ Probar Conexión con Gemini` en Ajustes (`GeminiApiKeySetupCard`)**:
    - Permite verificar en vivo contra la API de Google si la clave ingresada y el modelo seleccionado funcionan correctamente antes de realizar consultas.
    - Proporciona diagnósticos claros e inequívocos:
      - `✅ Conexión con Gemini exitosa (Modelo: ...)` ante respuesta HTTP 200.
      - `❌ Error 403 (Permiso Denegado)` alertando si la clave no es válida o si se ha pegado un identificador erróneo (recordando que las claves de Google AI Studio siempre inician con `AIzaSy...`).
      - `⚠️ Error 429` ante saturación de cuota y diagnósticos de conectividad offline.
  - **Tarjeta de Advertencia Visible en el Asistente IA (`AiAssistantScreen`)**:
    - Cuando el usuario tiene una clave de Gemini guardada pero la llamada cloud falla (ej. por HTTP 403 con clave incorrecta), la app **ya no oculta silenciosamente el error**.
    - Se muestra una tarjeta de alerta destacada explicando el fallo exacto de Gemini Cloud y aclarando que se proporciona la respuesta del **Motor Experto Offline** como respaldo para que el viajero nunca se quede sin información.
- **Selector de Modelos de Google Gemini Cloud (`GeminiApiKeyHelper.getAvailableModels()`)**:
  - Selector con chips interactivos en Ajustes para alternar entre los modelos oficiales de Google AI Studio:
    - **Gemini 2.0 Flash** (Recomendado, rápido y multimodal).
    - **Gemini 2.0 Flash Lite** (Máxima velocidad y menor latencia).
    - **Gemini 1.5 Pro** (Razonamiento profundo para itinerarios complejos).
    - **Gemini 1.5 Flash** (Generación previa estable).
    - **Gemini 2.5 Flash** (Última versión 2.5 de Google).
  - Persistencia de la preferencia en `UserPreferencesRepository` (`pref_gemini_model_id`) y visualización dinámica del modelo activo en la cabecera del Asistente IA.
  - Parseo robusto de respuestas JSON con `org.json.JSONObject` de Android para evitar fallos de regex en caracteres escapados.
- **Batería de Tests Unitarios TDD (`GeminiApiKeyHelperTest`)**:
  - Cobertura de catálogo de modelos, construcción dinámica de URLs para cada modelo, evaluación semántica de errores HTTP y pruebas simuladas de conexión con éxito y error.

## [0.0.1-beta.28] - 2026-10-25

### Added
- **Selector de Cuenta de Google del Sistema (`GoogleAccountSelectorHelper`) (`SPEC-00 RF-00.11`)**:
  - **Diálogo Nativo del Sistema Android**: Se erradica por completo la escritura manual del correo electrónico (`OutlinedTextField`), reemplazándolo por un selector nativo del sistema operativo mediante `AccountManager.newChooseAccountIntent(...)`.
  - **Selección de Cuenta en 1 Toque**: El usuario puede elegir directamente cualquiera de las cuentas de Google ya configuradas en el dispositivo Android (Pixel 10/9 Pro XL, TCL NXTPAPER 11 Plus, etc.) con total fluidez y seguridad.
  - **Gestión y Desconexión de Cuenta**: Posibilidad de cambiar de cuenta o desvincularla en cualquier momento desde los Ajustes, mostrando la cuenta activa con insignia visual y correo enmascarado.
- **Sincronización Total con Google Drive y Multi-Dispositivo (`GoogleDriveSyncHelper`) (`SPEC-00 RF-00.11`)**:
  - **Copia de Seguridad Integral (`JapanTravelCloudSyncBundle`)**: Exporta e importa en formato JSON portable todo el ecosistema de datos del viajero: planes de viaje, paradas de itinerarios, tramos de transporte (vuelos, trenes bala Shinkansen, metro, autobuses), guías turísticas y de supervivencia, documentos/reservas y ciudades meteorológicas configuradas.
  - **Integración con Google Drive vía Android Storage Access Framework**:
    - **Exportar a Google Drive (`CreateDocument`)**: Permite guardar el archivo de backup (`japan-travel-backup-${account}.json`) directamente en la carpeta de Google Drive del usuario o en almacenamiento local/SD, sin necesidad de configuración compleja de consolas de API o tokens externos.
    - **Restaurar desde Google Drive (`OpenDocument`)**: Permite restaurar el backup desde Google Drive en cualquier otro dispositivo con la misma cuenta de Google, sincronizando planes y guías al instante.
  - **Fusión Inteligente sin Pérdidas (`mergeSyncBundles`)**: Algoritmo de combinación que integra datos entre dispositivos sumando planes, guías, documentos y ciudades sin borrar los existentes y evitando duplicidades.
  - **Sincronización Rápida en Caché Local**: Botón de sincronización con 1 toque para actualizar el estado local y registrar la marca temporal exacta de la última sincronización.
  - **Componente Visual `GoogleDriveSyncCard` en Ajustes**: Tarjeta moderna con diseño KotlinConf que muestra la cuenta activa, contadores de elementos sincronizados (planes, guías, documentos, ciudades), botones de acción rápida e información clara de privacidad.
- **Batería de Tests Unitarios TDD (`GoogleDriveSyncHelperTest`)**:
  - 10 tests unitarios exhaustivos que comprueban la generación y serialización simétrica del bundle, exportación/importación JSON, fusión sin duplicados de planes, guías, documentos y ciudades meteorológicas, saneamiento de nombres de backup y validación/enmascaramiento seguro de cuentas de Google.

## [0.0.1-beta.27] - 2026-10-25

### Added
- **Selectores Interactivos de Hora (`AppTimePickerField`) y Controles Numéricos con Steppers (`AppNumberStepperField`) (`SPEC-00 RF-00.11`, `SPEC-04 RF-04.8/04.9`, `SPEC-05 RF-05.7`)**:
  - **Selector de Horas Nativo / Material 3 (`AppTimePickerField`)**:
    - Sustitución completa de las cajas de texto manual para horas (`HH:mm`) en todas las secciones de la aplicación por selectores táctiles visuales con diálogo Material 3 de 24 horas y reloj interactivo.
    - Integrado en **Paradas del Itinerario** (`ItineraryStopEditorModalSheet`: `Hora Inicio` y `Hora Fin`).
    - Integrado en **Tramos de Transporte** (`TransportSegmentEditorModalSheet`: `Hora Salida`, `Hora Llegada`, `Facturación (opc)` y `Embarque (opc)`).
    - Soporte de limpieza rápida (`✕` / botón "Borrar") para campos horarios opcionales.
  - **Control Incremental de Números (`AppNumberStepperField`)**:
    - Componente especializado con botones táctiles `[-]` y `[+]` para incrementar o decrementar enteros discretos de 1 en 1 sin teclear.
    - Integrado en el selector de **Día de la actividad** (`dayText`, 1..99) en paradas del itinerario.
    - Integrado en la **Duración total del viaje** (`daysText`, 1..90) en la creación y edición de planes de viaje.
    - Campo central con tipografía centrada y despliegue directo del teclado numérico (`KeyboardType.Number`) filtrando automáticamente caracteres no numéricos.
  - **Teclados Numéricos Decimales Especializados (`KeyboardOptions(keyboardType = KeyboardType.Decimal)`)**:
    - Despliegue automático del teclado numérico decimal en el conversor de divisas (`CurrencyScreen`: `inputAmountText`).
    - Despliegue automático del teclado numérico decimal en las coordenadas geográficas de ciudades (`CityWeatherEditorModalSheet`: `latText` y `lonText`).
- **Batería de Tests Unitarios TDD (`TimeAndNumericHelperTest`)**:
  - 8 tests unitarios que verifican exhaustivamente el parseo y validación de horas en 24h (`parseHourAndMinute`, `isValidTime`, `formatTime`, `formatDisplayTime`), saneamiento y delimitación de enteros (`sanitizeInteger`), operaciones de stepper (`increment`, `decrement`) y filtrado de números y decimales (`filterNumeric`, `filterDecimal`).

### Changed
- **Centralización Exclusiva de la Configuración de Gemini API Key en Ajustes (`SPEC-06 RF-06.3`)**:
  - **Pantalla del Asistente IA limpia y enfocada**: Se retira el formulario de entrada y el desplegable de configuración de la pantalla `AiAssistantScreen`, evitando sobrecargar la interfaz de chat y consultas de viaje.
  - **Acceso rápido desde el Asistente**: En la barra de estado de motor (`✨ Gemini Cloud` vs `⚡ Motor Experto Offline`), se incorpora el botón `⚙️ Configurar en Ajustes` (`onNavigateToSettings`), que traslada inmediatamente al usuario a la pantalla de **Ajustes** (`SettingsPreferencesScreen`).
  - **Ajustes como punto único de configuración**: Toda la gestión y obtención asistida de la API Key (`GeminiApiKeySetupCard` con enlace directo a Google AI Studio, tutorial en 4 pasos, pegado con 1 toque del portapapeles y validación) se gestiona de forma centralizada en la sección de Ajustes y Sincronización.

## [0.0.1-beta.25] - 2026-10-25

### Added
- **Método Amigable para Obtener y Configurar la Clave de API de Gemini (`SPEC-06 RF-06.3`)**:
  - **Botón Directo de 1 Toque a Google AI Studio**: Se incorpora el botón destacado `🌐 Obtener Clave Gratis en Google AI Studio` que abre de forma directa e inmediata la página oficial de gestión de claves de Google (`https://aistudio.google.com/app/apikey`) en el navegador del dispositivo, sin necesidad de buscar o escribir URLs complicadas.
  - **Tutorial Integrado y Desplegable en 4 Pasos**: Guía clara y accesible para cualquier viajero explicando cómo obtenerla en menos de 1 minuto, recalcando que el acceso personal a Gemini 2.0 Flash es 100% gratuito con cualquier cuenta de Gmail/Google y no requiere tarjeta de crédito.
  - **Botón de Pegado Rápido con 1 Toque (`📋 Pegar`)**: Permite trasladar la clave copiada en el navegador directamente al campo de texto con un solo toque desde el portapapeles del sistema, erradicando la fricción de mantener pulsado en el móvil.
  - **Selector de Visibilidad (`👁️ / 🙈`)**: Alterna suavemente entre la máscara de contraseña y texto visible para comprobar que la clave se ha pegado correctamente antes de guardar.
  - **Validación Reactiva de Formato**: Detección en tiempo real del prefijo estándar de Google (`AIzaSy...`) con insignias visuales de confirmación (`✅ Formato válido`).
  - **Garantía y Tranquilidad del Modo Offline**: Aclaración visual reconfortante recordando que la clave es 100% opcional; la app funciona plenamente offline mediante su Motor Experto de Viaje on-device integrado.
  - **Integración Unificada (`GeminiApiKeySetupCard`)**: Sustitución de los campos rudimentarios anteriores en la pantalla del Asistente IA (`GeminiAndTranslatorScreen`) y en la pantalla de Ajustes (`SettingsScreen`) por una experiencia visual moderna, coherente y amigable.
- **Batería de Tests Unitarios TDD (`GeminiApiKeyHelperTest`)**:
  - 4 tests unitarios que verifican la validación rigurosa de formato (`isValidApiKeyFormat`), enmascaramiento (`maskApiKey`), pasos del tutorial bilingüe (`getSetupGuideSteps`) y URL canónica de Google AI Studio.

## [0.0.1-beta.24] - 2026-10-25

### Added
- **Clarificación de la Tarjeta de Seguridad y Desacoplamiento del Interruptor de Bloqueo Biométrico (`SPEC-03 RF-03.3`)**:
  - **Eliminación de la ambigüedad**: Se reestructura la cabecera de la sección de Identificación, eliminando el interruptor flotante que aparecía pegado al título principal *"Pasaporte, DNI y Documentos"* y que provocaba confusión respecto a si apagaba o encendía la sección.
  - **Tarjeta dedicada de seguridad**: Se integra una tarjeta de configuración visualmente independiente: `🔒 Bloqueo Biométrico / PIN` con título destacado e información explicativa explícita: *"Exigir huella, rostro o PIN para acceder a las fotos de pasaporte y documentos"*, clarificando de inmediato que protege el acceso a los datos de identidad.
- **Bóveda de Documentos de Identidad Personalizados (`CustomIdDocumentItem` — `SPEC-03 RF-03.2`)**:
  - **Soporte ilimitado de documentos sensibles con foto**: Permite al viajero guardar de forma privada y 100% offline cualquier documento oficial con fotografía:
    - 💉 **Certificados de Vacunación** (fiebre amarilla, Covid, etc.).
    - 💊 **Recetas Médicas para Aduanas** (indispensables ante la estricta normativa de aduanas de Japón para medicamentos prescritos y permisos *Yakkan Shoumei* / confirmación de importación).
    - 🚗 **Carnet de Conducir Internacional** (IDP según la Convención de Ginebra de 1949).
    - 🏥 **Pólizas de Seguro Médico Internacional** con números de asistencia 24h.
    - 🛂 **Visados, sellos consulares y códigos QR de entrada**.
    - 📄 **Cualquier documento oficial o privado personalizado**.
  - **Editor Modal con Sugerencias Rápidas (`CustomIdDocumentEditorModalSheet`)**:
    - Chips horizontales interactivos con presets habituales que rellenan el título con un solo toque.
    - Campos de texto para nombre del documento y notas/referencia/caducidad (ej. *"Hospital Carlos III · Válido hasta 2028"*).
    - Selector integrado de fotos desde galería o archivos con previsualización de estado.
  - **Visor Inmersivo con Zoom Táctil (`pinch-to-zoom`)**:
    - Cada documento personalizado cuenta con acceso inmediato al visor a pantalla completa sobre fondo negro (`FullScreenZoomableImageViewerDialog`) con zoom de `1.0x` a `5.0x`, arrastre panorámico, alternador por doble toque y restablecimiento.
  - **Gestión Completa y Borrado Seguro con Double Check**:
    - Botones dedicados de cambio de foto (`🔄 Cambiar Foto`), edición de textos (`✏️ Editar`) y eliminación protegida (`🗑️ Quitar Foto` / `🗑️ Eliminar Documento`).
    - Integración obligatoria con `ConfirmationDialogBuilder.buildSensitiveDeleteConfirmation` que exige activar el interruptor de doble confirmación antes de eliminar permanentemente cualquier documento o fotografía.
  - **Persistencia Offline-First Segura**:
    - Metadatos guardados en `Context.filesDir/id_vault/custom_id_docs.json`.
    - Fotografías almacenadas en el directorio privado `Context.filesDir/id_vault/custom_doc_<timestamp>.jpg`, aisladas de la galería pública y accesibles sin internet.
- **Batería de Tests Unitarios TDD (`DocumentsAndGuidesUseCaseTest`)**:
  - Verificación de serialización y deserialización JSON sin dependencias externas (`DocumentsAndGuidesJsonCodec.exportCustomIdDocsJson` / `importCustomIdDocsJson`).
  - Verificación de operaciones reactivas de inserción, actualización de datos y borrado en `SharedDocumentsAndGuidesStore`.

## [0.0.1-beta.23] - 2026-10-25

### Added
- **Visor a Pantalla Completa y Zoom Táctil para Pasaporte y DNI (`SPEC-03 RF-03.1.3`)**:
  - **Apertura Táctil Inmersiva**:
    - Al pulsar sobre la fotografía de Pasaporte o DNI (anverso o reverso) en la Bóveda de Identificación (`IdDocumentPhotoSection`), la imagen se abre a pantalla completa con fondo negro de alto contraste para máxima legibilidad.
    - Se añade indicador visual *"🔍 Toca para pantalla completa y zoom"* en la miniatura de cada documento.
  - **Zoom Pellizcando y Desplazamiento Panorámico (`ZoomPanState`)**:
    - **Pinch-to-zoom fluido**: Permite ampliar de `1.0x` a `5.0x` para inspeccionar con máxima nitidez detalles como número de pasaporte, fecha de caducidad, códigos MRZ o números de identidad.
    - **Pan / Drag delimitado**: Permite desplazar la imagen ampliada manteniendo los márgenes dentro del contenedor de la pantalla.
    - **Alternador por doble toque**: Alterna de forma instantánea entre zoom ampliado (`2.5x`) y escala original (`1.0x`).
    - **Controles flotantes**: Indicador del porcentaje de zoom en tiempo real (ej. `🔍 100%`, `🔍 250%`), botón *"Restablecer"* y botón de cierre *"✕"*.
  - **Batería de Tests Unitarios TDD (`ZoomPanStateTest`)**:
    - 7 tests unitarios que verifican la escala inicial, acotación entre 1x y 5x, traslación panorámica dentro de límites, alternancia por doble toque y restablecimiento a estado inicial.

## [0.0.1-beta.22] - 2026-10-25

### Added
- **Modales de Confirmación Universal y Sistema de Double Check para Acciones Sensibles (`SPEC-00 RF-00.13`)**:
  - **Cobertura Universal de Borrado**:
    - Se garantiza que el 100% de las acciones de eliminación en la aplicación requieran confirmación modal obligatoria antes de proceder, erradicando cualquier borrado inmediato por toques accidentales.
    - Se añade confirmación obligatoria a las acciones que carecían de ella: **Borrado de Días Completos de Itinerario** (`deleteDay`), **Eliminación de Fotografías de Identidad Oficial** (Pasaporte y DNI en `IdVaultScreen`), y **Vaciado de Itinerario** (`updatePlan` vacío).
  - **Sistema de Doble Comprobación (Double Check)**:
    - Diseñado específicamente para acciones de alto impacto y elementos sensibles:
      - **Borrar Plan de Viaje Completo**: previene la pérdida de semanas de planificación.
      - **Borrar Día Completo del Itinerario**: previene la supresión accidental de todas las visitas y paradas del día.
      - **Quitar Fotografías de Pasaporte / DNI**: protege datos de identidad legales cruciales durante el viaje.
      - **Vaciar Itinerario**: previene el reseteo accidental de la agenda de viaje.
    - **Mecanismo Interactivo de Seguridad**:
      - Banner de alerta visual crítica con icono de emergencia (`🚨`).
      - Interruptor obligatorio de confirmación: *"🚨 DOBLE COMPROBACIÓN: Confirmo que deseo eliminar este elemento sensible de forma definitiva y entiendo que es 100% irreversible."*
      - El botón de confirmación destructivo (`🗑️ Eliminar Definitivamente`) permanece **estrictamente bloqueado y deshabilitado** hasta que el usuario activa conscientemente el interruptor.
  - **Batería de Tests Unitarios TDD (`ConfirmationDialogHelperTest`)**:
    - Nuevos tests unitarios verificando `buildSensitiveDeleteConfirmation` en español e inglés, banderas de `requiresDoubleCheck`, textos de advertencia, prompts de confirmación y ejecución fiel de `onConfirm`.

## [0.0.1-beta.21] - 2026-10-25

### Added
- **Búsqueda Predictiva de Ciudades y Geocodificación Automática de Coordenadas de Google Maps (`SPEC-07 RF-07.6`)**:
  - **Listado Predictivo Reactivo al Escribir**:
    - A medida que el usuario teclea el nombre de la ciudad en el formulario (`CityWeatherEditorModalSheet`), se despliega una lista interactiva de sugerencias coincidentes con un diseño limpio basado en Material Design 3.
    - Motor de búsqueda insensible a mayúsculas, minúsculas y acentos (`Normalizer`) que localiza destinos por nombre en español, inglés o nombres de barrios/zonas turísticas clave (ej. "Shinjuku", "Hakata", "Fuji", "Gion").
  - **Autocompletado Inmediato de Nombres y Coordenadas**:
    - Al pulsar cualquier sugerencia del listado predictivo, se auto-rellenan instantáneamente el nombre en español, en inglés, la latitud y la longitud con máxima precisión.
  - **Geocodificación Automática con Google Maps**:
    - Botón `"📍 Obtener coordenadas de Google Maps"` para resolver y rellenar automáticamente la latitud y longitud de cualquier ciudad personalizada mediante `android.location.Geocoder` en Android (servicios de Google Maps) con fallback a la API de geocodificación de Open-Meteo.
  - **Catálogo Geográfico Extendido 100% Offline**:
    - Más de 50 destinos y capitales de prefecturas japonesas preinstaladas (Tokio, Kioto, Osaka, Hakone/Fuji, Nara, Hiroshima, Miyajima, Kamakura, Nikko, Takayama, Shirakawa-go, Kanazawa, Sapporo, Otaru, Hakodate, Fukuoka, Nagoya, Yokohama, Kobe, Himeji, Okayama, Kurashiki, Naoshima, Takamatsu, Matsuyama, Nagano, Matsumoto, Karuizawa, Niigata, Shizuoka, Sendai, Okinawa, Beppu, Kagoshima, etc.) y hubs de origen/escala internacional (Madrid, Barcelona, Valencia, Sevilla, Málaga, Bilbao, Doha, Dubái, Abu Dabi, Londres, París, Roma, Ámsterdam, Fráncfort, Múnich, Seúl, etc.).
  - **Batería de Tests Unitarios TDD (`CityGeocodingHelperTest`)**:
    - 11 tests unitarios que verifican la búsqueda por prefijo, tolerancia a acentos, aliases de barrios/zonas, destinos internacionales, parseo de respuestas JSON de geocodificación y cobertura de presets.

## [0.0.1-beta.20] - 2026-10-25

### Added
- **Traductor de Voz Fiel y Literal Bidireccional (`SPEC-06 RF-06.1.2.6`)**:
  - **Eliminación Total de Respuestas Conversacionales Inventadas**:
    - Se elimina el comportamiento de chatbot en `translateFromJapaneseSpeech` que respondía al interlocutor japonés (ej. responder *"¿En qué puedo ayudarle?"* ante *"すみません"*, inventar *"Aquí tiene agua fresca de cortesía"* ante *"お水"*, o generar *"El total a pagar son..."* ante precios en yenes).
    - Ahora el motor actúa estrictamente como un **intérprete fiel y literal**: traduce lo que se ha dicho tal cual (*"Disculpe / Perdón"*, *"Agua"*, *"1500 yenes"*), erradicando las alucinaciones conversacionales.
    - Se eliminan también los mensajes meta de chatbot (*"El interlocutor ha respondido en japonés. Por favor, pídele que hable despacio..."*), sustituyéndolos por un analizador léxico y semántico de términos clave.
  - **Flujo Bidireccional Exacto y Simétrico**:
    - **1. Habla Japonés ➔ Captura texto japonés ➔ Traduce al español ➔ Habla en español**: Captura de voz japonesa (`ja-JP`), visualización del texto japonés en caracteres grandes (`Kanji/Kana`), pronunciación fonética en Romaji, traducción directa al español y locución por voz (`TextToSpeech` en `es-ES`).
    - **2. Habla Español ➔ Captura texto en español ➔ Traduce al japonés ➔ Habla en japonés**: Captura de voz en español (`es-ES`), visualización del texto en español, traducción fiel a caracteres japoneses con Romaji, locución por voz (`TextToSpeech` en `ja-JP`) y tarjeta rotable `180°` para mostrar al dependiente.
  - **Motor Híbrido en Tiempo Real (`OnlineTranslatorClient`)**:
    - Cliente de traducción rápida online en segundo plano (`Dispatchers.IO`) para traducir libremente cualquier locución o frase no catalogada cuando hay conexión a internet, manteniendo el motor offline local como base 100% offline-first.
  - **Batería de Tests Unitarios TDD (`AiAndTranslatorUseCaseTest`)**:
    - Pruebas unitarias de traducción literal estricta y verificación de que nunca se generan respuestas de dependiente ni mensajes meta de chatbot.

## [0.0.1-beta.19] - 2026-10-25

### Added
- **Selectores de Fechas Interactivos Nativos (`SPEC-00 RF-00.14` & `SPEC-04 RF-04.7`)**:
  - **Componente Universal `AppDatePickerField`**:
    - Sustituye los campos de texto libre con formato manual `YYYY-MM-DD` por un selector interactivo basado en `DatePickerDialog` de Material 3 Compose.
    - Presentación amigable y legible para el viajero en español e inglés (ej. `12 de nov de 2026 (2026-11-12)` o `Nov 12, 2026 (2026-11-12)`).
    - Campo de sólo lectura con icono de calendario (`📅`) y zona táctil completa que despliega el calendario emergente sin mostrar el teclado virtual.
  - **Conversor Seguro de Fechas `DateSelectorHelper`**:
    - Conversión bidireccional entre cadenas ISO-8601 (`YYYY-MM-DD`) y milisegundos UTC a medianoche, evitando cualquier desviación por husos horarios locales.
    - Manejo seguro de fallbacks ante entradas vacías o formatos anómalos.
  - **Integración Completa en Formularios de la Suite**:
    - **Planificador de Viaje (`PlanEditorModalSheet`)**: Selección gráfica de la fecha de inicio del viaje.
    - **Gestor de Transportes (`TransportSegmentEditorModalSheet`)**: Selección interactiva de la fecha del trayecto para vuelos, trenes Shinkansen, metro y autobuses.
    - **Itinerario de Actividades (`ItineraryStopEditorModalSheet`)**: Selección visual de la fecha de la actividad sincronizada con el número de día.
  - **Batería de Tests Unitarios TDD (`DateSelectorHelperTest`)**:
    - Pruebas exhaustivas de parsing ISO a UTC, formateo UTC a ISO, formateo display multiidioma y resiliencia ante errores.

## [0.0.1-beta.18] - 2026-10-25

### Added
- **Sincronización Automática de Ciudades del Tiempo desde Transportes e Itinerarios (`SPEC-07 RF-07.5`)**:
  - **Extractor Inteligente `PlanWeatherCityExtractor`**: Detección automática y resolución de destinos y escalas a partir de los orígenes y llegadas de vuelos/trenes/metro (`TransportSegmentItem`) y de las paradas, visitas y actividades de la agenda (`ItineraryStopItem`).
  - **Resolución Robusta de Códigos y Destinos**: Mapeo contra aeropuertos y ciudades japonesas e internacionales (Tokio, Kioto, Osaka, Hakone/Fuji, Nara, Hiroshima, Miyajima, Kamakura, Nikko, Takayama, Kanazawa, Sapporo, Fukuoka, Nagoya, Yokohama, Kobe, Okinawa, Sendai, Madrid, Doha, etc.) con aislamiento estricto de códigos IATA (`\b$code\b`) para evitar falsos positivos.
  - **Convivencia Total con Edición y Eliminación Manual**: El usuario puede seguir añadiendo ciudades manualmente (`➕ Ciudad`), modificando nombres y coordenadas (`✏️ Editar`), o suprimiendo ciudades (`🗑️ Eliminar`), con memoria de exclusión de eliminaciones (`_deletedCityIds`) y botón de sincronización manual (`🔄 Plan`).
  - **Tests Unitarios TDD**: Batería completa en `PlanWeatherCityExtractorTest` y serialización de exclusiones en `WeatherCityJsonCodec`.

## [0.0.1-beta.17] - 2026-10-25

### Added
- **Inicialización Limpia de Tramos al Añadir Transporte (`SPEC-05 RF-05.6`)**:
  - **Formulario Limpio sin Texto Residual**: Al pulsar «➕ Añadir Transporte», todos los campos de texto e inputs de edición (`TransportSegmentEditorModalSheet`) se abren completamente vacíos (`""`), eliminando la precarga de datos de ejemplo (Nozomi, Shinkansen, horas predeterminadas u orígenes/destinos precargados) para que el viajero no tenga que borrar manualmente texto previo.
  - **Aislamiento Reactivo en Compose**: Claves de estado ligadas a `initialSegment.id` en `rememberSaveable` para evitar fugas de estado entre creaciones y ediciones.
  - **Selector de Modo Dinámico y Soporte de Autobús**: Selección de modo precargada según el filtro activo en pantalla e inclusión del botón de selección para Autobús (`BUS`).
  - **Tests Unitarios TDD**: Cobertura en `TransportPlannerUseCaseTest` para `createEmptySegment`.

## [0.0.1-beta.16] - 2026-10-25

### Added
- **Modales de Confirmación de Seguridad para Acciones de Edición y Eliminación (`SPEC-00 RF-00.13`)**:
  - **Componente Universal `AppConfirmationDialog` y `ConfirmationDialogBuilder`**:
    - Diálogos modales nativos (`AlertDialog`) con soporte destructivo (`isDestructive = true`, botón en rojo de peligro) y de modificación segura.
  - **Protección contra Borrados Accidentales en Toda la Suite**:
    - **Vuelos y Transportes**: Confirmación antes de eliminar cualquier tramo desde la tarjeta o desde el editor.
    - **Planificador de Viaje**: Confirmación antes de borrar un plan de viaje completo, eliminar paradas o cargar datos demo.
    - **Ciudades Meteorológicas**: Confirmación antes de eliminar cualquier ciudad de la lista de seguimiento.
    - **Bóveda de Documentos y Reservas**: Confirmación antes de suprimir billetes, reservas de hotel o entradas de parques/museos.
    - **Guías de Viaje**: Confirmación antes de borrar artículos o notas offline.
    - **Portabilidad JSON**: Confirmación antes de vaciar el plan o reemplazarlo al importar un JSON.
  - **Protección contra Sobrescrituras Accidentales al Editar**:
    - Confirmación modal al pulsar "💾 Guardar" en registros existentes para evitar modificaciones involuntarias mientras se usa el móvil en movimiento.

## [0.0.1-beta.15] - 2026-10-25

### Added
- **Sistema de Advertencias Visuales y Notificaciones de Facturación, Embarque y Salida (`SPEC-05 RF-05.5`)**:
  - **Evaluador de Dominio 100% Offline (`TransportAlertEvaluator`)**:
    - Detección precisa de eventos según los umbrales temporales del vuelo o transporte en su huso horario local:
      - `CHECK_IN_APPROACHING` (cuando faltan $\le 120$ min para abrir el mostrador de facturación).
      - `CHECK_IN_OPEN` (mostrador de entrega de equipaje abierto).
      - `BOARDING_APPROACHING` (cuando faltan $\le 45$ min para la hora de embarque).
      - `BOARDING_NOW` (urgente: embarque en curso o $\le 15$ min para inicio de embarque con puerta y asiento).
      - `DEPARTURE_IMMINENT` (urgente: $\le 20$ min para la salida programada / cierre de puertas).
    - Adaptado tanto a vuelos como a trenes Shinkansen (avisos de llegada al andén y coche).
  - **Advertencias Visuales en Tiempo Real (`TransportAlertBanner` y Tarjetas de Tramo)**:
    - Banner interactivo destacado en la cabecera de la pantalla de transporte con colores de alerta (`Akabeni` rojo Torii para urgentes y ámbar para preventivas), cuenta atrás en minutos, información de puerta/mostrador y botón de descarte.
    - Resaltado perimetral con borde de color de alerta e insignia en la tarjeta del tramo correspondiente.
    - Visualización clara de `🧳 Facturación` y `🚪 Embarque` en cada tramo.
  - **Notificaciones del Sistema Android (Canal `transport_alerts`)**:
    - Creación automática de canal de notificación con prioridad alta (`IMPORTANCE_HIGH`), sonido y vibración.
    - Soporte completo para Android 13+ (solicitud en tiempo de ejecución del permiso `POST_NOTIFICATIONS`).
    - Despacho de notificaciones enriquecidas (`NotificationCompat.Builder`) con apertura directa de la app al pulsar.
  - **Controles de Prueba y Simulación Interactiva (1-Touch Testing)**:
    - Botones de prueba rápida en la cabecera (`🚨 Embarque`, `🧳 Facturación`, `🚄 Tren Salida`) para validar tanto el banner visual como la notificación del sistema en el dispositivo físico en cualquier momento.
  - **Edición Personalizada de Facturación y Embarque**:
    - Nuevos campos `Facturación (HH:mm)` y `Embarque (HH:mm)` en la hoja modal `TransportSegmentEditorModalSheet`.

## [0.0.1-beta.14] - 2026-10-25

### Added
- **Gestión Dinámica de Ciudades y Destinos Meteorológicos (`SPEC-07 RF-07.4`)**:
  - **Catálogo Dinámico y CRUD Completo de Ciudades**:
    - Reemplazo del enum estático por el modelo dinámico `WeatherCityItem(id, nameEs, nameEn, latitude, longitude, isDefault)`.
    - **Añadir Nuevas Ciudades**: Botón `➕ Añadir Ciudad` en el panel del tiempo que abre la hoja modal `CityWeatherEditorModalSheet`.
    - **Editar Coordenadas y Nombre**: Botón `✏️ Editar` en la tarjeta de la ciudad seleccionada para ajustar nombre en español, inglés o coordenadas GPS con validación numérica estricta (`latitude` entre -90 y 90, `longitude` entre -180 y 180).
    - **Eliminar Ciudades**: Botón `🗑️ Eliminar` para suprimir destinos que el usuario no vaya a visitar (con protección para no dejar la lista vacía).
  - **Selector Rápido de Destinos Populares de Japón (1-Touch Presets)**:
    - Carrusel horizontal con chips de acceso inmediato a 14 destinos turísticos emblemáticos de Japón con coordenadas GPS preconfiguradas:
      - *Nara* (`34.6851, 135.8048`), *Hiroshima* (`34.3853, 132.4553`), *Miyajima / Itsukushima* (`34.2965, 132.3197`), *Kamakura* (`35.3192, 139.5467`), *Nikko* (`36.7580, 139.5989`), *Takayama* (`36.1460, 137.2520`), *Kanazawa* (`36.5613, 136.6562`), *Sapporo* (`43.0618, 141.3545`), *Fukuoka* (`33.5904, 130.4017`), *Nagoya* (`35.1815, 136.9066`), *Yokohama* (`35.4437, 139.6380`), *Kobe* (`34.6901, 135.1955`), *Okinawa (Naha)* (`26.2124, 127.6809`), y *Sendai* (`38.2682, 140.8694`).
    - Al pulsar cualquiera de los presets se rellenan automáticamente los campos de nombre y coordenadas en el formulario.
  - **Previsión Online en Tiempo Real para Cualquier Destino**:
    - Integración de `OpenMeteoWeatherUseCase.fetchCoordinatesForecastOnline(cityKey, lat, lon)` que consulta `https://api.open-meteo.com/v1/forecast` para las coordenadas exactas de la ciudad elegida en tiempo real y sin necesidad de API Key.
  - **Persistencia Local Automática Offline-First**:
    - `SharedWeatherForecastStore.saveToDisk()` / `loadFromDisk()`: Serialización automática de la lista personalizada de ciudades en `context.filesDir/saved_weather_cities.json` con `WeatherCityJsonCodec`.
    - Todas las ciudades añadidas, modificadas o eliminadas persisten indefinidamente tras el cierre o reinicio de la aplicación, manteniendo además las semillas offline por defecto si no hay conexión.

## [0.0.1-beta.13] - 2026-10-25

### Fixed & Enhanced
- **Asistente IA de Viaje Interactivo y Funcional (`SPEC-06 RF-06.2`)**:
  - **Estado Reactivo de Carga y Retroalimentación Visual**:
    - El botón `✨ Consultar IA` y los chips de temas rápidos activan inmediatamente un estado de carga (`isLoading = true`) con un spinner animado (`CircularProgressIndicator`) y el texto *"Consultando..."*, bloqueando pulsaciones dobles accidentales.
    - Tarjeta de progreso visible mientras se genera la respuesta, indicando si se está conectando con Gemini Cloud o analizando el motor experto local.
    - Insignia de origen en cada respuesta (`✨ Gemini Cloud` o `⚡ Experto Japón Offline`) junto con la hora exacta de generación (`🕐 HH:mm`).
  - **Arquitectura Híbrida Real (Google Gemini 2.0 Flash REST API + Motor Experto On-Device Offline)**:
    - **Soporte Gemini Cloud**: Permite configurar opcionalmente una Google Gemini API Key (`AIzaSy...`) tanto en la propia pantalla del Asistente IA como en *Ajustes*, conectando directamente con Gemini 2.0 Flash en la nube cuando hay conexión a internet.
    - **Ampliación Profunda del Motor Experto Offline**: Cobertura detallada de más de 20 temáticas esenciales para el viajero (Compras Tax-Free en Don Quijote, conexión a internet con eSIM vs Pocket Wi-Fi, propinas y modales en restaurantes, normas en Onsen y política de tatuajes, rentabilidad del JR Pass nacional vs billetes sueltos, enchufes y voltaje 100V, traslados desde aeropuertos Narita/Haneda/KIX, equipaje grande en Shinkansen, ropa según la estación, Akihabara y anime, y teléfonos de emergencia 110/119).
  - **Acciones Rápidas sobre la Respuesta de la IA**:
    - Botón `📋 Copiar`: Copia la respuesta completa al portapapeles con confirmación visual en pantalla.
    - Botón `📖 Guardar Guía`: Exporta directamente la respuesta generada a la *Bóveda de Guías de Viaje* (`SharedDocumentsAndGuidesStore.upsertGuide`) con persistencia local permanente en disco.
    - Botón `🗓️ Aplicar`: Aplica sugerencias de itinerario directamente en el Planificador de Viaje.
    - Campo de consulta con botón de borrado rápido (`✕`) e inicialización limpia con tarjeta de bienvenida y consejos de uso.

### Added
- **Gestión Completa de Múltiples Planes de Viaje (`SPEC-04 RF-04.1`)**:
  - **Tarjeta de Plan Activo**: Ubicada en la cabecera del Planificador de Viaje, mostrando el título del viaje, fecha de inicio, duración en días y conteo total de paradas.
  - **Crear Nuevo Plan de Viaje (`PlanEditorModalSheet`)**: Botón `➕ Nuevo Plan` que permite crear itinerarios independientes (ej. *Primavera Kioto 2027*, *Ruta Tokio-Osaka*), seleccionando nombre, fecha de inicio y número de días.
  - **Editar y Renombrar Plan**: Botón `✏️ Editar Plan` para modificar el título del plan, fecha de inicio o ajustar la duración en cualquier momento.
  - **Selector y Conmutador de Planes (`PlanSwitcherModalSheet`)**: Botón `📑 Cambiar Plan` para alternar al instante entre los diferentes viajes guardados.
  - **Eliminación Segura de Planes**: Botón `🗑️ Borrar Plan` para suprimir el plan activo de forma limpia y cambiar automáticamente al siguiente plan disponible.
- **Persistencia Local Automática Offline-First en Almacenamiento Interno (`Context.filesDir`)**:
  - `SharedTravelPlanStore`: Serialización automática en `context.filesDir/saved_travel_plans.json` de todos los planes, paradas, días y tramos de transporte multimodales.
  - `SharedDocumentsAndGuidesStore`: Serialización automática en `context.filesDir/saved_documents.json` y `saved_guides.json`.
  - Cero pérdida de datos: Todas las creaciones, ediciones o borrados de planes, paradas, transportes, documentos, guías y reservas persisten de forma indefinida tras reiniciar la app o apagar el dispositivo.
- **Adjuntos de Archivos, PDFs y Fotos a Entradas y Reservas (`SPEC-03 RF-03.2`)**:
  - Selector nativo de archivos en `DocumentEditorModalSheet` para adjuntar billetes en PDF, capturas de pantalla de reservas o códigos QR (`*/*`).
  - Copia y almacenamiento seguro en `context.filesDir/documents/`.
  - Previsualización de imágenes y botón directo `📄 Abrir Documento / PDF` con `androidx.core.content.FileProvider` para abrir billetes y reservas con el visor del sistema.

## [0.0.1-beta.11] - 2026-10-25

### Added
- **Traducción Visual con Cámara de Texto Japonés y Carteles 100% Offline (`SPEC-06 RF-06.1`)**:
  - **Reconocimiento Óptico de Caracteres (OCR)**: Integrado `com.google.mlkit:text-recognition-japanese:16.0.1` para lectura on-device de Kanji, Hiragana y Katakana sin requerir conexión a internet ni cuenta en la nube.
  - **Selector de Modo en Traductor**: Pestañas en cabecera `[🎙️ Voz Simultánea]` y `[📷 Cámara / Carteles]` dentro de `VoiceTranslatorScreen`.
  - **Captura y Selección de Imágenes**: Soporte directo para tomar fotos con la cámara del dispositivo (`ActivityResultContracts.TakePicturePreview`) o cargar fotos de la galería (`ActivityResultContracts.GetContent`).
  - **Traducción Automática de Carteles y Menús (`translateOcrJapaneseText`)**:
    - Detección e interpretación fiel de platos típicos de Japón (Ramen, Gyoza, Cerveza, Udon, Soba, Sushi, Curry), reglas de locales (Zona de no fumadores, Fumadores, Local completo, Salida de emergencia, Aseos) y andenes/tipos de vagón de Shinkansen (asientos libres vs reservados).
    - Extracción de precios en Yenes (`([0-9]+)\s*円`) con conversión contextual.
    - Generación de pronunciación en Romaji y traducción en español e inglés.
  - **Sintetizador de Voz (TTS)**: Botones para escuchar la traducción completa locutada en español o escuchar el texto japonés original.
  - **Carteles de Demostración Rápida**: Chips de prueba con un solo toque (*🍜 Menú Ramen*, *🚅 Shinkansen*, *🚭 Cartel Local*, *🚻 Aseos y Salida*) para verificar el funcionamiento inmediato sin necesidad de apuntar a un cartel físico.
  - **Acceso Directo a Cámara de Google Traductor / Lens**: Botón de fallback para realidad aumentada con superposición en vivo (`googletranslate://camera`).

## [0.0.1-beta.10] - 2026-10-25

### Fixed
- **Traducción Fiel y Concisa de Español a Japonés sin Palabras Inventadas (`SPEC-06 RF-06.1`)**:
  - `OfflineTranslatorEngine`: Refactorización integral de `translateToJapanese` para que la locución en japonés reproduzca con máxima fidelidad lo pronunciado por el viajero por voz.
  - **Eliminación Total de Boilerplate y Elogios Inventados**: Se eliminaron preguntas secundarias innecesarias (ej. no añadir *¿Aceptan tarjeta o Suica?* si solo se pide la cuenta), agradecimientos ficticios sobre comida si solo se dice *Hola* o *Gracias*, cantidades asumidas (no añadir *dos aguas* si solo se dice *agua*) y disculpas automáticas sobre apps de traducción.
  - **Tests de Fidelidad de Voz**: Incorporados tests unitarios específicos en `AiAndTranslatorUseCaseTest` que verifican que la traducción a japonés nunca añade palabras no expresadas por el usuario.

## [0.0.1-beta.9] - 2026-10-25

### Fixed
- **Traducción de Voz de Japonés a Español sin Repetición en Japonés (`SPEC-06 RF-06.1`)**:
  - `OfflineTranslatorEngine`: Implementado diccionario exhaustivo de frases, cortesía, transportes, direcciones, precios en yenes y respuestas en japonés con traducción directa al español y al inglés.
  - **Regla Estricta de No-Fuga de Texto**: Garantizado que `spanishSpokenText` y `englishSpokenText` nunca devuelvan caracteres japoneses (Kanji/Kana) en crudo ni oraciones sin traducir, evitando que el sintetizador de voz (TTS) repita las palabras en japonés.
  - **Enrutamiento Inteligente de Idioma (`isJapaneseText`)**: Si el usuario habla en japonés en cualquiera de los dos micrófonos, la app detecta automáticamente los caracteres japoneses y lo locuta traducido en español (`Locale.forLanguageTag("es-ES")`). Si habla en español o inglés, lo traduce y locuta en japonés (`Locale.JAPAN`).
  - **Manejo Robusto de Locales en TTS**: Configuración resiliente del motor `TextToSpeech` con comprobación de soporte y fallback automático.

## [0.0.1-beta.8] - 2026-10-25

### Added
- **Creación, Edición y Eliminación Completa de Planes, Paradas, Días y Transporte (`SPEC-04 RF-04.1`, `SPEC-05 RF-05.1`)**:
  - `SharedTravelPlanStore`: Métodos `upsertStop`, `deleteStop`, `deleteDay`, `upsertTransportSegment`, `deleteTransportSegment` y `clearAll` con tests unitarios en `TravelPlanJsonPortabilityTest`.
  - **En el Planificador de Viaje**:
    - Botón **`➕ Añadir Parada`** y botón **`➕ Día`** en la cabecera de días.
    - Botones directos **`✏️ Editar`** y **`🗑️ Eliminar`** en cada tarjeta de parada del itinerario.
    - Botón **`🗑️ Borrar Día X`** para eliminar un día completo y editor modal (`ItineraryStopEditorModalSheet`) para modificar día, hora de inicio/fin, lugar, nombre en japonés, categoría y notas.
  - **En Transporte y Trenes**:
    - Botón **`➕ Añadir Transporte`** y botones **`✏️ Editar`** / **`🗑️ Eliminar`** en cada tarjeta de vuelo/tren (`TransportSegmentEditorModalSheet`).
- **Bóveda de Pasaporte y DNI con Fotos Reales y Biometría Obligatoria (`SPEC-03 RF-03.1`)**:
  - Selección de imágenes desde Galería, Cámara o Drive (`ActivityResultContracts.GetContent()`), copia privada en almacenamiento interno (`context.filesDir/id_vault/`), renderizado de miniaturas y opciones para sustituir o eliminar cada foto.
  - Autenticación biométrica real y obligatoria mediante `androidx.biometric.BiometricPrompt` en `FragmentActivity`, con fallback a credencial del sistema (PIN/patrón/contraseña).
- **Gestión de Entradas, Documentos y Guías de Viaje (`SPEC-03 RF-03.2`, `RF-03.3`)**:
  - Nueva categoría de **Entradas** (`TICKET`) en `SPEC-03` para parques temáticos y museos (USJ Super Nintendo World, Museo Ghibli, teamLab Planets, Shibuya Sky, Tokyo Disneyland) con soporte de código de reserva y QR.
  - Creación, edición y borrado de documentos de reserva (`DocumentEditorModalSheet`) y guías de viaje personalizadas (`GuideEditorModalSheet`).
- **Guía de Esquema JSON para Generación con IA (`SPEC-08`)**:
  - Documentación de formato estandarizado `japan-travel-plan.v1` (`docs/specs/08-travel-plan-json-schema-guide.md`) con esquema estricto, tipos y prompt de sistema listo para usar en Gemini, Claude o ChatGPT.

### Fixed
- **Legibilidad de la Barra de Notificaciones en Tema Claro (`SPEC-00`)**:
  - Configuración explícita de `isAppearanceLightStatusBars = !darkTheme` e `isAppearanceLightNavigationBars = !darkTheme` en `Theme.kt`, evitando iconos blancos sobre fondo gris claro en modo claro.
- **Traducción de Voz Directa sin Boilerplate (`SPEC-06`)**:
  - Eliminación de fórmulas repetitivas ("El interlocutor dice...") para una locución en vivo directa y fluida.
- **Filtro de Previsión Meteorológica Open-Meteo (`SPEC-07`)**:
  - Visualización exclusiva de la ciudad seleccionada por el usuario (Tokio, Kioto, Osaka, Hakone), sin saturación de tarjetas redundantes.

## [0.0.1-beta.7] - 2026-10-25

### Changed
- **Menú Lateral (`ModalNavigationDrawer`) y Estandarización Geométrica de Botones (`SPEC-00 RF-00.2`)**:
  - Navegación unificada mediante menú lateral desplegable (`☰ Menú` o deslizando desde el borde izquierdo) con las 12 herramientas organizadas en 5 secciones (*Herramientas Rápidas*, *Planificación y Rutas*, *Voz e IA*, *Documentos y Guías*, *Sistema*).
  - Estandarización estricta de todos los botones (`AppPrimaryButton`, `AppSecondaryButton`, `AppButtonRow`, `AppOptionButton`) con altura fija (`50.dp` / `44.dp`), radio uniforme (`12.dp` / `10.dp`), una sola línea de texto (`maxLines = 1`) y alineación simétrica 50%/50% sin descuadres.
- **Revisión y Corrección del Motor de IA y Traductor por Voz (`SPEC-06`)**:
  - `GeminiAssistantEngine`: Conversión automática de cualquier importe en yenes (`¥`, `yenes`, `JPY`, `円`) e IVA japonés (+10%), respuestas contextuales reales sobre *Suica*, *Shinkansen*, *Nara*, *Hakone/Fuji*, *Hiroshima/Miyajima*, *Tokio*, *Kioto*, *Osaka*, *cajeros 7-Eleven* y *envío de maletas Takkyubin*.
  - `AiPlanBuilderEngine`: Respeta los horarios existentes que no se solapen y añade rutas completas según la ciudad pedida por el usuario (*Nara*, *Hakone*, *Hiroshima*, *Osaka*).
  - `OfflineTranslatorEngine`: Ampliación del diccionario conversacional por voz (baños, precios, agua, mesa en restaurante, alergias, dieta vegetariana, equipaje, farmacia) evitando que el locutor japonés lea palabras en español.

## [0.0.1-beta.6] - 2026-10-25

### Changed
- **Rediseño Ergonómico de Paleta Oscura, Tipografía Grande y Baja Densidad Visual (`SPEC-00 RF-00.2`)**:
  - **Paleta Oscura Relajante (Soft Slate & Indigo)**: Sustitución de los bordes rojos/azules intensos en modo oscuro por superficies pizarra suaves (`#111318`, `#1A1D24`, `#232730`), bordes neutros discretos (`#2E3440`) y acentos pastel descansados (`#8AB4F8` índigo suave y `#94C9A9` verde salvia).
  - **Tipografía Ampliada (`15.sp`–`34.sp`)**: Aumento global de tamaños tipográficos (prohibido texto menor de `13.sp`), mayor interlineado y espaciado interno generoso (`18.dp`).
  - **Sub-vistas Segmentadas (1 herramienta por pantalla)**: División de las pantallas principales mediante selectores limpios (`💴 Conversor` | `🕒 Relojes` | `🌦️ Tiempo` en Inicio; `🛂 Pasaporte` | `📄 Reservas` | `📖 Guías` en Docs; `🎙️ Traductor de Voz` | `✨ Asistente IA` en Voz/IA; `⚙️ Preferencias` | `ℹ️ Acerca de` en Ajustes) para eliminar el exceso de información simultánea.
  - **Gestión de Datos Demo en el Planificador**: Añadido botón `"🗑️ Vaciar Demo"` / `"🔄 Cargar Demo"` en la hoja de opciones del Planificador para limpiar o restaurar los planes de ejemplo en cualquier momento.

## [0.0.1-beta.5] - 2026-10-25

### Added
- **Traducción Simultánea Bidireccional por Voz y Audio (`SPEC-06 RF-06.1`)**:
  - Integración de reconocimiento de voz (`RecognizerIntent` en `es-ES`/`en-US` y `ja-JP`) y síntesis de voz nativa (`TextToSpeech` en `Locale.JAPAN` y `es-ES`/`en-US`).
  - **Voz del Viajero (`🎙️ Hablar Yo ➔ 🔊 Suena Japonés`)**: El usuario habla en español/inglés y el móvil locuta automáticamente la traducción en japonés para el interlocutor.
  - **Voz del Japonés (`🎙️ Escuchar Japonés ➔ 🔊 Me habla en Español`)**: El interlocutor habla en japonés y el móvil traduce y locuta automáticamente la respuesta en español/inglés para que el viajero la escuche sin necesidad de saber leer japonés.

## [0.0.1-beta.4] - 2026-10-25

### Changed
- **Eliminación de etiquetas `BETA` de toda la interfaz (`SPEC-00 RF-00.5`)**:
  - Se eliminaron todas las píldoras/etiquetas `BETA` y de versión en cabeceras y tarjetas para maximizar el espacio útil de pantalla en los Pixel 10/9 Pro XL y Tablet TCL NXTPAPER 11 Plus.
  - Mejora de compatibilidad POSIX `sh` y detección automática `ADB_LIBUSB=0` en `scripts/deploy-authorized-devices.sh`.

## [0.0.1-beta.3] - 2026-10-25

### Added
- **Herramienta de Previsión Meteorológica con Open-Meteo para Usuario e IA (`SPEC-07`)**:
  - `OpenMeteoWeatherUseCase` y `SharedWeatherForecastStore` conectados a `https://api.open-meteo.com/v1/forecast` (modelos globales + **JMA Japón** en `Asia/Tokyo`, sin API Key) con semilla offline preinstalada para *Tokio*, *Kioto*, *Osaka* y *Hakone / Monte Fuji*.
  - Clasificación automática de códigos WMO (`0..99`), etiqueta de idoneidad diaria (`⛩️ Ideal Exterior` vs `🏛️ Recomendado Cubierto`), avisos de paraguas e **Índice de Visibilidad del Monte Fuji (`🗻 %`)** integrados en el Dashboard y en cada jornada del Planificador de Viaje.
  - `AiPlanBuilderEngine.optimizePlanWithWeather`: permite a la IA integrada adaptar las paradas del itinerario según el tiempo previsto por Open-Meteo (añadiendo alternativas cubiertas como mercados *Nishiki/Kuromon* y galerías *Shotengai* en días de lluvia).

## [0.0.1-beta.2] - 2026-10-25

### Added
- **Exportación e Importación de Planes de Viaje y Transporte en JSON Legible (`SPEC-04 RF-04.6` & `SPEC-05 RF-05.4`)**:
  - `TravelPlanJsonCodec` y `PortableTravelPlanBundle` (`schemaVersion = "japan-travel-plan.v1"`) con formato JSON indentado (pretty-printed) 100% legible por humanos para preparar itinerarios, calendarios, vuelos, trenes Shinkansen, metros y autobuses desde aplicaciones externas e importarlos mediante **Fusión (`Merge`)** o **Reemplazo (`Replace`)**.
  - `JsonPortabilityModalSheet` integrada en el Planificador de Viaje, Planificador de Transporte y pestaña de IA para exportar/compartir JSON o pegar/importar planes externos en tiempo real.
- **Creación y Mejora de Planes de Viaje con IA Gemini (`SPEC-06 RF-06.2`)**:
  - `AiPlanBuilderEngine` y `SharedTravelPlanStore` para que la IA integrada pueda crear nuevos planes de viaje y optimizar los planes existentes en la app (resolución automática de solapamientos horarios y adición de Día 3 en Osaka con paradas + conexión en Shinkansen Nozomi Kioto ➔ Shin-Osaka).

## [0.0.1-beta.1] - 2026-10-25

### Added
- Inicialización del repositorio orientado a Agentes de IA (`AGENTS.md`, `.gitignore`, `README.md`, `CHANGELOG.md`).
- Configuración de ramas **Gitflow** (`main` y `develop`).
- Documentación completa de **Fase 1: Especificación (SDD)** en `/docs/specs/` (`SPEC-00` a `SPEC-06`), **Fase 2: Análisis Técnico** y **Fase 3: Plan de Tareas (TDD)**.
- Implementación funcional completa de **`v0.0.1-beta.1`** con **TDD (100% tests unitarios en verde)** e interfaz inspirada en **JetBrains KotlinConf App** con paleta japonesa (*Washi*, *Sumi*, *Akabeni Torii*, *Ai-iro*, *Matcha*) e icono adaptativo Torii/Sol Naciente:
  - **Core (`SPEC-00`)**: Persistencia en `DefaultUserPreferencesRepository`, cambio reactivo de idioma (`ES`/`EN`/Sistema con fallback a `EN`) y tema (`Claro`/`Oscuro`/`Sistema`), píldora de conexión en vivo (`Wi-Fi`, `Pocket Wi-Fi`, `SIM`, `eSIM`, `Roaming`, `Offline`) con acceso al panel nativo de Internet/SIM de Android, sincronización multi-dispositivo con Cuenta de Google, apartado **"Acerca de"** con versión `0.0.1-beta.1` y listado de librerías Open Source de terceros, y diseño adaptativo (`NavigationBar` en **Pixel 10/9 Pro XL** y `NavigationRail` + doble panel en **TCL NXTPAPER 11 Plus**).
  - **Relojes y Ubicación (`SPEC-01`)**: `CalculateClockComparisonUseCase` y `OfflineTimeZoneResolver` con hora en Japón (`Asia/Tokyo`), hora local GPS y selector persistente de país de origen con diferencia horaria exacta (DST).
  - **Conversor de Divisas Offline-First (`SPEC-02`)**: `ConvertCurrencyUseCase`, tasas semilla offline preinstaladas (`EUR <-> JPY`), sincronización en vivo con **Frankfurter API (BCE)** y botones rápidos de billetes/monedas de Japón (`¥100` a `¥10,000`).
  - **Pasaporte/DNI, Documentos y Guías de Viaje (`SPEC-03`)**: Bóveda rápida de Pasaporte y DNI con interruptor de bloqueo biométrico/PIN, repositorio buscable de documentos por categoría y guías de viaje offline (*Tokio*, *Kioto*, *Osaka*).
  - **Planificador de Viaje (`SPEC-04`)**: Línea temporal de horarios (`JST`) con detección de solapamientos, fotos offline por parada, mapa de ruta con marcadores numerados, apertura directa en **Google Maps** y exportación a **Google Calendar / Calendario del sistema**.
  - **Planificador de Transporte Multimodal (`SPEC-05`)**: Gestión de Vuelos (facturación, embarque, puerta, salida, llegada), Shinkansen/Trenes (andén, coche, asiento, maletas grandes, JR Pass), Metro (código de estación, vagón recomendado, número de salida `Exit`) y Autobuses (dársena), con cálculo automático de escalas y transbordos entre husos horarios.
  - **Asistente IA con Gemini y Traducción Simultánea (`SPEC-06`)**: Botón de acceso directo global en cabecera, traductor cara a cara giratorio **180°** con Romaji y asistente Gemini con conversión automática de precios de cartas en `¥` a `EUR`.
- Sistema de despliegue **ADB Debug para dispositivos autorizados** (`./gradlew installAuthorizedDebug`, `scripts/deploy-authorized-devices.sh` y `authorized-devices.local.conf` ignorado en Git).
