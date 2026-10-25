# Changelog

Todos los cambios notables en este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/spec/v2.0.0.html) y [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/).

## [Unreleased]

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
