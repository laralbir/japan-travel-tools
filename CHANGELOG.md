# Changelog

Todos los cambios notables en este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/spec/v2.0.0.html) y [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/).

## [Unreleased]

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
