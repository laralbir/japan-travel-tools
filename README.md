# Japan Travel Tools 🇯🇵✈️

Aplicación Android **offline-first** con la suite definitiva de herramientas esenciales para viajar a Japón, desarrollada en **Kotlin** y **Jetpack Compose** siguiendo metodologías estrictas de **Spec-Driven Development (SDD)** y **Test-Driven Development (TDD)**.

> **Estado actual del proyecto**: `v1.4.0` (Versión Oficial Estable de Producción). Publicada con APK firmado adjunto en [GitHub Releases](https://github.com/laralbir/japan-travel-tools/releases/tag/v1.4.0).

---

## 🌟 Características Principales y Arquitectura

### 🏛️ Configuración Global y Base (Core)
- **100% Offline-First**: Toda herramienta funciona sin conexión a internet. Los datos remotos (tasas de cambio del BCE vía Frankfurter API, meteorología con Open-Meteo) se cachean localmente en `Context.filesDir` con marcas de tiempo y disponen de semillas offline precargadas.
- **Multiidioma (i18n)**: Soporte completo en **Español** e **Inglés**, adoptando por defecto el idioma del sistema con fallback a inglés y selector manual per-app en Ajustes.
- **Inspiración Visual (`KotlinConf App` + Colores Tradicionales de Japón)**:
  - Sistema de diseño moderno inspirado en **[JetBrains KotlinConf App](https://jetbrains.github.io/kotlinconf-app/)**: tarjetas con bordes nítidos de `1.dp`, selectores de agenda interactivos, chips rápidos de filtrado y tipografía tabular de alta legibilidad.
  - **Paleta Tradicional Japonesa (`Nippon Dentōshoku`)**: Modo Claro inspirado en el papel *Washi* (`#FAF7F2`), Modo Oscuro OLED inspirado en la tinta *Sumi* (`#0F1115`) y acentos en rojo bermellón *Akabeni / Torii* (`#C9171E`), índigo japonés *Ai-iro* (`#165E83`), verde *Matcha* y rosa *Sakura*. Soporte para Modo Claro, Oscuro o Automático según el sistema.
- **Diseño Adaptativo para Dispositivos Objetivo**:
  - **Smartphones Pro XL (Google Pixel 10 Pro XL y Pixel 9 Pro XL)**: Diseño *Edge-to-Edge*, ergonomía a una mano con controles en la zona inferior y modo oscuro OLED de alto contraste.
  - **Tablets (TCL NXTPAPER 11 Plus)**: Interfaz adaptativa con barra de navegación lateral (`NavigationRail`), vista de doble panel en el Planificador (Horario + Mapa de Google Maps interactivo en paralelo), vista Maestro-Detalle para Guías y lectura optimizada para *Ink Paper* y *Color Paper*.
- **Indicador de Conexión en Vivo y Selector Rápido (`Wi-Fi / SIM / eSIM / Offline`)**: Píldora interactiva en la cabecera que detecta la conexión activa y abre de inmediato el panel de conectividad nativo de Android (`Settings.Panel.ACTION_INTERNET_CONNECTIVITY`).
- **Portabilidad JSON Unificada y Copia de Seguridad en Google Drive**: Exportación e importación todo en uno de planes, transportes, guías y reservas/billetes en un solo archivo `.json` legible (`japan-travel-plan.v1` o backup completo) mediante Storage Access Framework (SAF) local o en Google Drive, con detección inteligente automática de entidades (`JsonContentDetector`).
- **Actualizador OTA Integrado vía GitHub Releases y Firma Criptográfica de Producción (`SPEC-10`)**: Comprobación automática (cada 12 h respetando Modo Offline, Wi-Fi/eSIM y Roaming) y manual desde Ajustes con soporte `ETag` y comparación estricta SemVer 2.0.0, visor de novedades (`CHANGELOG`) renderizado con `MarkdownText`, descarga en caché privada con doble verificación criptográfica (**hash `SHA-256` + certificado X.509 PKCS#12 con APK Signature Scheme v2/v3**) e instalación *in-place* mediante `PackageInstaller` conservando intactos todos los datos locales (`Context.filesDir`).
- **Seguridad y Privacidad Local**: Almacenamiento seguro en directorio privado de la aplicación (`Context.filesDir`). Sin telemetría invasiva, sin trackers y con protección biométrica para documentos confidenciales.

---

## 🧰 Suite de Herramientas Integradas (9 Herramientas)

### 1. 🕒 Relojes y Ubicación (`SPEC-01`)
- Visualización simultánea de la hora local por red/GPS, hora de Japón (`Asia/Tokyo`) y hora del país de origen seleccionado.
- Cálculo automático de diferencia horaria e indicador visual del tramo del día (mañana, tarde, noche) para evitar llamadas a deshoras.

### 2. 💱 Conversor de Divisas Offline-First (`SPEC-02`)
- Conversión instantánea bidireccional entre Euro (`EUR`), Yen Japonés (`JPY`) y Dólar estadounidense (`USD`).
- Sincronización automática desatendida en segundo plano con WorkManager (cada 24 horas) con Frankfurter API (Banco Central Europeo) y persistencia reactiva en disco.
- Modo 100% offline garantizado con fecha de última actualización y tasas de respaldo semilla.

### 3. 🪪 Bóveda de Documentos e Identidad (`SPEC-03`)
- Almacenamiento seguro de Pasaporte, DNI (anverso y reverso) y certificados médicos o de vacunación internacional.
- Soporte multiformato para **imágenes y documentos PDF** con visor integrado y apertura externa.
- **Detección Binaria de Formatos por Magic Bytes (`detectFileFormatByMagicBytes`)**: Reconocimiento inequívoco de PDFs (`%PDF-`) e imágenes JPG/PNG independientemente de la extensión o MIME type retornado por el selector SAF.
- **Protección Biométrica Obligatoria (`BiometricPrompt` / PIN)**: Bloqueo estricto por diseño sin conmutadores superfluos, auto-bloqueo tras 120 segundos de inactividad o al minimizar la aplicación, y preservación de sesión biométrica ante lanzadores de cámara nativa o selectores de archivos.
- Visor a pantalla completa con zoom fluido táctil (`pinch-to-zoom` de `1.0x` a `5.0x`).

### 4. 📂 Repositorio de Documentos, Billetes y Guías de Viaje (`SPEC-03`)
- Gestión offline de billetes de vuelo, reservas de hotel, pases JR Pass y entradas a atracciones.
- **Pases Digitales Estilo Wallet Offline (`WalletTicketPassDialog`)**: Visor de alta legibilidad con código localizador QR para escaneo rápido en tornos o controles.
- Captura directa con la cámara nativa de Android (`FileProvider`) para fotografiar tickets de papel o entradas físicas.
- **Gestor y Lector Nativo de Guías de Viaje en Markdown (`FullGuideReaderDialog` & `MarkdownText`)**: Renderizado enriquecido 100% offline con encabezados `H1`–`H6`, negritas, cursivas, código inline/bloques, listas con sublistas indentadas, casillas `- [ ]`/`- [x]`, citas `>`, separadores `---`, enlaces `[texto](url)` y tablas Markdown desplazables, extractos limpios sin sintaxis cruda en tarjetas de resumen, pestaña conmutable `✍️ Editar` / `👁️ Vista previa` en el editor y ordenación dinámica (*Fijadas*, *A-Z*, *Por Ciudad*).
- Exportación e importación modular o unificada en formato JSON desde Ajustes.

### 5. 🗺️ Planificador de Viaje con Fotos, Horarios y Google Maps (`SPEC-04`)
- Itinerario interactivo por días con alternador de vista superior: **`[ 📋 Horario | 🗺️ Mapa del Día ]`** y aprovechamiento máximo del alto de pantalla sin márgenes inferiores duplicados en el `Scaffold`.
- **Mapa Integrado del Día con Selector de Capas Cartográficas (`DayItineraryMapView`)**:
  - Proyección Web Mercator con caché local, gestos fluidos de arrastre y zoom, y respaldo vectorial offline automático.
  - **Selector de Capas Cartográficas**: Conmutador interactivo entre **`🗺️ Google Maps`** (por defecto, con nombres de calles y estaciones en español/romaji + japonés), **`🛰️ Google Satélite Híbrido`** y **`🌍 OpenStreetMap`**.
  - **Marcadores Numerados, Etiquetas Compactas Anticolisión y Controles Laterales**: Visualización despejada de las paradas numeradas (`1..N`) por orden cronológico con etiquetas compactas multilínea (máx. 2 líneas) que se ocultan selectivamente de forma automática al aproximarse en pantalla (`zoomend` / distancia en píxeles) para evitar solapamientos, y columna de controles en la esquina inferior derecha con conmutador global de etiquetas (`[🏷️]`), zoom (`[+]`, `[—]`), botón `[📍]` para encuadrar todas las visitas (`fitToMarkers`) e icono vectorial GPS (*My Location*) para centrar en la ubicación en tiempo real del viajero (`📍 Tú estás aquí`).
- **Ficha Modal de Detalle Interactivo de Parada (`StopDetailMapModalSheet`)**: Al pulsar cualquier punto del mapa, se despliega una ficha con nombre en japonés en gran formato con botón de copia rápida para taxistas/locales, vinculación inteligente de Guías de Viaje (`TravelGuideArticle`) con botón para leer la guía completa, billetes digitales asociados (`TravelDocumentItem` / `TransportSegmentItem`) con visor QR ampliado y brillo al 100%, y botón para navegación paso a paso en Google Maps.
- **Formateo Regional de Fechas y Husos Locales (`TravelDateTimeFormatter`)**: Fechas completas en formato regional del sistema (`FormatStyle.FULL`) y horas en el huso local de cada destino (ej. Múnich `UTC+1`, Tokio `UTC+9`) con indicación del desfase.
- **Persistencia Canónica en ISO UTC**: Almacenamiento estándar en formato UTC (`Instant.toString()`) con compatibilidad retroactiva total en `TravelPlanJsonCodec`.
- Sincronización directa con el Calendario de Android / Google Calendar y exportación/importación en formato JSON (`japan-travel-plan.v1`).

### 6. 🚆 Planificador Multimodal de Transportes (`SPEC-05`)
- Gestión integral de trayectos: vuelos internacionales y domésticos, trenes bala **Shinkansen / JR**, líneas de metro y autobuses.
- **Vuelos**: Tiempos de facturación en mostrador, apertura y cierre de embarque, terminales, puertas, asientos, zonas horarias y duración real de vuelo.
- **Trenes y Metros**: Línea y color oficial, códigos de estación (`G19` ➔ `G01`), andenes recomendados, coche/vagón, reserva de equipaje voluminoso en Shinkansen, cobertura JR Pass / Suica y número exacto de salida de estación (`Exit`).
- **Calculador de Escalas y Transbordos (`Layovers & Transfers`)**: Cálculo automático de tiempos de conexión, alertas de conexión ajustada y vinculación directa con billetes QR.

### 7. 🤖 Asistente de Viaje IA (Gemini) y Traductor Simultáneo (`SPEC-06`)
- **Traductor Simultáneo en 3 Pestañas (`🎙️ Voz`, `✍️ Texto`, `📷 Carteles / Cámara`)**:
  - Pestaña de voz y conversación cara a cara con pantalla reversible 180°.
  - Pestaña de texto independiente con locución nativa en japonés (`ja-JP` / `Locale.JAPAN`) y catálogo temático de frases rápidas de viaje.
  - Traducción visual de carteles con ML Kit Japanese OCR offline y consulta contextual en segundo plano.
- **Selector de Región Estilo Google Fotos (Arrastre Táctil, Maximizado y Multizona)**:
  - Encuadre interactivo arrastrando esquinas y bordes con el dedo (`detectDragGestures`).
  - Previsualización maximizada a 380.dp con máscara translúcida y cuadrícula de tercios sin recortar destructivamente la foto original.
  - Modo multizona (`OcrTextZone`) para traducir bloques independientes simultáneamente.
- **Orientación EXIF y Rotación 90°**: Corrección automática de sensores de cámara y botón manual de giro.
- **Asistente de Viaje con Google Gemini**:
  - Modelo ultrarrápido **Gemini Flash-Lite** por defecto (<1s de respuesta).
  - Soporte para modelos avanzados (`gemini-3.5-flash`, `gemini-pro-latest`) con selector directo en cabecera y diagnóstico específico de cuota en nivel gratuito (Error 429).
  - Análisis multimodal de cartas de restaurantes y optimizador inteligente de planes de viaje (`AI Plan Builder`).

### 8. ⛅ Previsión Meteorológica y Planificación Climática (`SPEC-07`)
- Integración con **Open-Meteo API** (modelos globales + JMA de Japón) sin requerir API Key, con caché offline persistente para Tokio, Kioto, Osaka, Hakone y destinos del viaje, y botonera horizontal rápida (`➕ Ciudades del Plan` / `➕ Añadir Ciudad`).
- Sincronización automática periódica en segundo plano mediante WorkManager (cada 6 horas).
- Catálogo geográfico predictivo offline con más de 50 ciudades y destinos turísticos de Japón e internacionales.
- Índices de actividad (`⛩️ Ideal Exterior` vs. `🏛️ Recomendado Cubierto`), alertas de lluvia y probabilidad de visibilidad del Monte Fuji (`🗻 %`).
- **Planificador Sensible al Clima (`Weather-Aware AI Planner`)**: La IA adapta automáticamente el itinerario en días lluviosos sugiriendo galerías cubiertas (*Shotengai*), museos o templos techados.

### 9. 🧾 Tickets de Compra, Tax-Free y Control de Gastos (`SPEC-08`)
- **Digitalización Inteligente Híbrida**: Procesamiento de comprobantes desde la cámara, galería local o Google Drive (SAF), con decodificación nativa de facturas PDF mediante `PdfRenderer`.
- **Motor OCR Trilingüe (Japonés, Inglés, Español)**: Extracción determinista de fechas, importes, impuestos y tiendas de tickets japoneses (era Reiwa, Don Quijote, Bic Camera, Uniqlo, 7-Eleven), recibos duty-free en inglés y facturas en español.
- **Soporte Multidivisa (JPY, EUR, USD)**: Detección automática de moneda con diálogo interactivo ante ambigüedad (`ReceiptCurrencySelectionDialog`) y contravalor simultáneo en Yenes.
- **Reglas Fiscales Tax-Free de Japón**: Comprobación automática del umbral de 5.000 ¥ sin tasas, desglose de consumibles (8%), bienes generales (10%) y seguimiento del estado aduanero (*Pendiente*, *Sellado*, *Reembolsado*).
- **Control de Presupuesto del Viaje**: Cuadro de mando analítico de gastos acumulados en ¥ y EUR, categorización (Compras, Comida, Transporte, Ocio, etc.) y balances diarios por ciudad.
- **Generador de Informes para Aduanas**: Exportación consolidada de compras Tax-Free en formato Texto, Markdown y JSON con copia rápida al portapapeles.

---

## 🚀 Metodología de Desarrollo (SDD + TDD + Gitflow)

Este proyecto se desarrolla bajo estrictas directrices de ingeniería de software definidas en [`AGENTS.md`](AGENTS.md):

1. **Flujo SDD + TDD**:
   - **Especificación (`/docs/specs/`)**: Requisitos, modelos de datos, reglas offline y casos de uso.
   - **Análisis (`/docs/analysis/`)**: Arquitectura técnica, repositorios, esquemas y estrategia de testing.
   - **Tareas (`/docs/tasks/`)**: Desglose atómico secuencial.
   - **Ciclo TDD**: **RED** (escribir el test que falla) ➔ **GREEN** (código mínimo para superar el test) ➔ **REFACTOR** (limpieza y optimización manteniendo todos los tests en verde).
2. **Gitflow Estricto**:
   - `main`: Rama de producción y releases estables publicadas.
   - `develop`: Rama de integración continua de desarrollo.
   - `feature/*`, `release/*`, `hotfix/*`: Ramas de trabajo.
3. **Conventional Commits v1.0.0**: Historial semántico estandarizado (`feat`, `fix`, `docs`, `test`, `refactor`, `chore`).
4. **Política de Releases y Firma Criptográfica de Producción (`PKCS#12` v2/v3)**:
   - Toda release publicada en GitHub se compila con `./gradlew assembleRelease` firmada con el Keystore oficial PKCS#12 (`enableV1Signing`, `enableV2Signing` y `enableV3Signing`, sin secretos en Git mediante `keystore.properties` local ignorado o GitHub Secrets efímeros en `.github/workflows/release-apk.yml`), adjuntando el `.apk` verificado con `apksigner` y su archivo `.apk.sha256` para garantizar continuidad criptográfica perpetua y actualizaciones *in-place* sin perder datos.
5. **Prevención de Reinicio de App y Bloqueo de Orientación Vertical en Android**:
   - Configuración de `singleTask` y bloqueo de orientación vertical (`android:screenOrientation="portrait"`) en `AndroidManifest.xml` con manejo de `onNewIntent` en `MainActivity`, evitando rotaciones accidentales de la interfaz al sujetar el teléfono durante el viaje.
   - Control de `configChanges` y persistencia de pantalla activa en `UserPreferencesRepository`.
6. **Despliegue ADB en Dispositivos Autorizados (Sin Commitear)**:
   - Configuración local de dispositivos autorizados (`authorized-devices.local.conf`, ignorado en Git).
   - Tarea `./gradlew installAuthorizedDebug` y script `./scripts/deploy-authorized-devices.sh` con instalación directa y segura por ADB.

---

## 📚 Estructura de Documentación (`/docs`)

Toda la documentación técnica y funcional se encuentra detallada en español dentro del directorio [`/docs`](docs/):

- **[Índice General de Documentación](docs/index.md)**
- **Especificaciones Funcionales (SDD)**:
  - [00 - Core: Configuración, Idiomas, Tema y Arquitectura Offline-First](docs/specs/00-core-configuration-spec.md)
  - [01 - Herramienta: Relojes y Ubicación (Hora Local, Japón y Origen)](docs/specs/01-world-clock-spec.md)
  - [02 - Herramienta: Conversor de Divisas Offline-First (EUR / JPY / USD)](docs/specs/02-currency-converter-spec.md)
  - [03 - Herramienta: Pasaporte/DNI, Repositorio de Documentos y Guías de Viaje](docs/specs/03-documents-and-id-vault-spec.md)
  - [04 - Herramienta: Planificador de Viaje (Fotos, Google Maps, Calendario y Horarios)](docs/specs/04-trip-planner-spec.md)
  - [05 - Herramienta: Planificador de Transporte (Vuelos, Trenes/Shinkansen, Metros y Autobuses)](docs/specs/05-flight-planner-spec.md)
  - [06 - Herramienta: Asistente IA con Gemini y Traductor Simultáneo](docs/specs/06-gemini-ai-and-translator-spec.md)
  - [07 - Herramienta: Previsión Meteorológica con Open-Meteo y Planificación Climática](docs/specs/07-weather-open-meteo-spec.md)
  - [08 - Herramienta: Tickets de Compra, Tax-Free y Control de Gastos del Viaje](docs/specs/08-shopping-receipts-tax-free-spec.md)
  - [08B - Guía y Esquema JSON Todo en Uno (`japan-travel-plan.v1`) para IA y Portabilidad](docs/specs/08-travel-plan-json-schema-guide.md)
  - [09 - Herramienta: Importación de Billetes Apple Wallet (`.pkpass`) y Visor QR/Aztec](docs/specs/09-apple-wallet-pkpass-import-spec.md)
  - [10 - Sistema: Actualizaciones Automáticas vía GitHub Releases y Firma Criptográfica](docs/specs/10-github-releases-auto-updater-spec.md)
- **Análisis Técnico**:
  - [Arquitectura, Stack Tecnológico y Estrategia TDD](docs/analysis/architecture-and-tech-stack.md)
  - [04 - Análisis Técnico: Mapa del Día, Etiquetas Anticolisión y Detalle con Guías/Billetes](docs/analysis/04-day-itinerary-map-and-linked-details-analysis.md)
  - [08 - Análisis Técnico: Módulo Tax-Free y Gastos](docs/analysis/08-shopping-receipts-tax-free-analysis.md)
  - [09 - Análisis Técnico: Importación Apple Wallet (`.pkpass`)](docs/analysis/09-apple-wallet-pkpass-import-analysis.md)
  - [10 - Análisis Técnico: Importación Unificada de Plan, Guías y Documentos JSON](docs/analysis/10-unified-travel-plan-guides-and-documents-analysis.md)
  - [11 - Análisis Técnico: Renderizado Nativo de Markdown en Guías de Viaje](docs/analysis/11-travel-guides-markdown-rendering-analysis.md)
  - [12 - Análisis Técnico: Actualizador Automático vía GitHub Releases y Firma de Producción](docs/analysis/12-github-releases-auto-updater-analysis.md)
- **Planificación de Tareas**:
  - [Backlog y Desglose de Tareas SDD + TDD](docs/tasks/backlog.md)
  - [Tareas: Renderizado Nativo de Markdown en Guías de Viaje](docs/tasks/tasks-travel-guides-markdown-rendering.md)
  - [Tareas: Importación Unificada JSON](docs/tasks/tasks-unified-travel-plan-import.md)
  - [Tareas: Actualizador Automático vía GitHub Releases y Firma Criptográfica](docs/tasks/tasks-github-releases-auto-updater.md)
- **Guías**:
  - [Guía de Usuario Completa](docs/user-guide.md)
  - [Flujo de Trabajo: Gitflow, Conventional Commits y Releases](docs/development-workflow.md)

---

## 🎯 Estado de Hitos del Proyecto

- [x] **Hito 1 - Configuración Base y Tema Visual Moderno KotlinConf** (`SPEC-00`): Scaffold, colores tradicionales de Japón, multiidioma ES/EN y soporte adaptativo teléfono/tablet.
- [x] **Hito 2 - Relojes Duales/Triples y Conversor de Divisas con Sincronización Automática** (`SPEC-01`, `SPEC-02`): Frankfurter API, caché persistente y WorkManager periódico.
- [x] **Hito 3 - Bóveda de Identidad y Documentos con Seguridad Biométrica Obligatoria** (`SPEC-03`): Detección por Magic Bytes, soporte universal PDF/imágenes, BiometricPrompt estricto y zoom táctil.
- [x] **Hito 4 - Planificador de Viaje con Fotos, Horarios UTC y Mapas** (`SPEC-04`): Fechas regionales completas, husos locales con desfases UTC, mapas interactivos Maps Compose y enlaces a Google Calendar.
- [x] **Hito 5 - Planificador Multimodal de Vuelos, Trenes Shinkansen, Metros y Autobuses** (`SPEC-05`): Calculador de escalas/transbordos, códigos de andén y estación, salidas recomendadas y alertas.
- [x] **Hito 6 - Asistente IA con Gemini y Traductor Simultáneo Trilingüe** (`SPEC-06`): Gemini Flash-Lite ultrarrápido, selector de área táctil Google Fotos, rotación EXIF 90° y audio nativo en japonés.
- [x] **Hito 7 - Previsión Meteorológica con Open-Meteo y Planificador Sensible al Clima** (`SPEC-07`): Sincronización en segundo plano (6h), índice del Monte Fuji y adaptación ante lluvia.
- [x] **Hito 8 - Módulo de Tickets de Compra, Tax-Free y Control de Gastos** (`SPEC-08`): Escaneo híbrido con IA y OCR trilingüe local, soporte multidivisa JPY/EUR/USD, selector de origen y exportación de informes para aduanas.
- [x] **Hito 9 - Validación y Despliegue en Dispositivos Físicos Autorizados**: Verificación directa por ADB en Google Pixel y Tablet TCL NXTPAPER 11 Plus.
- [x] **Hito 10 - Publicación de Versión de Producción v1.3.0**: Publicada en GitHub Releases con APK firmado adjunto.
- [x] **Hito 11 - Publicación de Versión de Producción v1.4.0**: Actualizador OTA integrado vía GitHub Releases (`SPEC-10`), renderizado nativo de Markdown en Guías (`SPEC-03`), importación unificada JSON (`ANALYSIS-10`), etiquetas anticolisión en mapa del día y APK oficial firmado con esquemas v1/v2/v3.

---

## 📜 Historial de Cambios

Para consultar el registro pormenorizado de todas las versiones, mejoras y correcciones, revisa el archivo [`CHANGELOG.md`](CHANGELOG.md).
