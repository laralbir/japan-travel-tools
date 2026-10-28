# Japan Travel Tools 🇯🇵✈️

Aplicación Android **offline-first** con herramientas esenciales para viajar a Japón, desarrollada en **Kotlin** y **Jetpack Compose** siguiendo metodologías **SDD (Spec-Driven Development)** y **TDD (Test-Driven Development)** para agentes de Inteligencia Artificial.

> **Estado actual del proyecto**: `v1.1.0` (Versión Oficial Estable de Producción — Suite integral 100% offline-first para viajeros a Japón con Portabilidad JSON Centralizada en Ajustes vía Storage Access Framework SAF con detector inteligente de contenido, Formateo Regional de Fechas completas y Horas con husos locales y almacenamiento ISO UTC `TravelDateTimeFormatter`, Rediseño Ergonómico No Invasivo de Botones en Tarjetas de Itinerario y Transporte, Bóveda de Documentos e Identidad con Bloqueo Biométrico Estricto Obligatorio por Diseño sin Toggle, Auto-bloqueo de 120s y en segundo plano, Sincronización Automática Periódica en Segundo Plano para Previsión Meteorológica (6h) y Conversor de Divisas (24h) con WorkManager, Asistente de IA con Renderizado Nativo de Markdown, Entrada Multilínea Ergonómica y Cancelación Reactiva de Consultas, Gemini Flash-Lite ultrarrápido (<1s) como modelo recomendado predeterminado libre de timeouts y cuotas, modelos verificados en tiempo real contra Google AI Studio `gemini-flash-lite-latest`, `gemini-3.5-flash`, `gemini-flash-latest` y `gemini-pro-latest`, diagnóstico específico de cuota en nivel gratuito para modelos PRO (Error 429), timeouts ampliados a 30s, soporte para claves con prefijo `AQ.`, Traducción Visual y OCR de Carteles con apertura de cámara con `FileProvider` y acceso directo a Google Lens / Google Traductor, Compilación Completa y Firma para Google Play Store con generación dual de Android App Bundle firmado `app-release.aab` y APK firmado `app-release.apk`, Selector Directo de Modelo PRO vs Flash en la cabecera del Asistente IA, Diagnóstico Activo de Conexión de Gemini Cloud en Ajustes, Sincronización Total con Google Drive `GoogleDriveSyncHelper`, Selectores Interactivos de Hora `AppTimePickerField` y Controles Numéricos con Steppers `AppNumberStepperField`, Bóveda de Documentos con fotos y zoom `pinch-to-zoom`, Modales de Confirmación Universal, Búsqueda Predictiva de Ciudades y Geocodificación Automática de Coordenadas, Traductor de Voz Fiel y Literal Bidireccional `Japonés <-> Español/Inglés`, Selectores de Fecha Interactivos Nativos `AppDatePickerField` con `DatePickerDialog` de Material 3, Notificaciones Push para Facturación y Embarque de Vuelos y Trenes, Asistente IA híbrido Gemini Cloud + Motor Experto 100% offline y persistencia local en `Context.filesDir`).

---

## Características Principales

### Configuración Global y Base (Core)
- **Multiidioma**: Español e Inglés. Selecciona automáticamente el idioma del sistema (con **Inglés** como idioma por defecto si el sistema usa otro idioma) y permite cambiarlo manualmente dentro de la app.
- **Tema Personalizable e Inspiración Visual (`KotlinConf App` + Colores de Japón)**:
  - Basado en el moderno sistema de diseño de **[JetBrains KotlinConf App](https://jetbrains.github.io/kotlinconf-app/)** (tarjetas con bordes nítidos de `1.dp`, selectores de agenda por días/horarios, chips de filtrado rápidos y tipografía tabular de alta legibilidad).
  - **Paleta Tradicional Japonesa (`Nippon Dentōshoku`)**: Modo Claro inspirado en el papel *Washi* (`#FAF7F2`), Modo Oscuro inspirado en la tinta *Sumi* (`#0F1115` OLED) y acentos en rojo bermellón *Akabeni / Torii* (`#C9171E`), índigo japonés *Ai-iro* (`#165E83`), verde *Matcha* y rosa *Sakura*. Soporta Modo Claro, Oscuro o Automático según el sistema (por defecto).
  - **Iconografía Moderna, Adaptive Icon y Splash Screen**: Set de iconos vectoriales modernos (`Material Symbols Rounded / Lucide`), icono de aplicación adaptativo con capa monocromática para *Material You* (Pixel Launcher) y pantalla de arranque nativa (`SplashScreen API`) sincronizada con el tema claro/oscuro.
- **100% Persistente, Offline-First y Sincronizable con Cuenta de Google**: Todas las preferencias, cachés de datos externos, itinerarios, transportes, guías y documentos del viajero se persisten localmente en el dispositivo para funcionar sin conexión a internet, y pueden **sincronizarse automáticamente entre varios dispositivos (móvil Pixel + tablet TCL NXTPAPER)** iniciando sesión con la misma **Cuenta de Google** (descargando copia local completa en cada dispositivo para uso offline).
- **Indicador de Conexión en Vivo y Selector Rápido (`Wi-Fi / SIM / eSIM / Offline`)**: Píldora interactiva en el Home/Cabecera que muestra en tiempo real qué conexión se está usando (distinguiendo entre Wi-Fi/Pocket Wi-Fi, SIM física, eSIM de datos en Japón, alerta de Roaming o modo Offline) y permite cambiar entre Wi-Fi, SIM y eSIM con un toque mediante el panel flotante nativo de Android (`Settings.Panel.ACTION_INTERNET_CONNECTIVITY`) y el gestor de SIMs.
- **Optimización para Dispositivos Objetivo (Smartphones Pro XL y Tablet NXTPAPER)**:
  - **Google Pixel 10 Pro XL y Pixel 9 Pro XL (6.8" 20:9 LTPO OLED 120Hz)**: Diseño *Edge-to-Edge*, ergonomía a una mano con controles principales en la zona inferior y modo oscuro OLED de alto rendimiento.
  - **Tablet TCL NXTPAPER 11 Plus (11.5" 2.2K ratio 3:2 120Hz)**: Interfaz adaptativa con barra lateral (`NavigationRail`), vista de doble panel en el Planificador (Horario + Mapa de Google Maps lado a lado), vista Maestro-Detalle para lectura de Guías/PDFs y contraste nítido optimizado para los modos *Ink Paper* y *Color Paper*.
- **Transparencia y Open Source ("Acerca de")**: Apartado integrado que muestra la versión exacta de la aplicación (`0.0.1-beta.X` / `0.0.1`), las fuentes de datos abiertas utilizadas y el listado completo de todas las librerías open source de terceros con sus respectivas licencias accesibles offline.

### Herramientas Incluidas en la Especificación (v0.0.1)
1. **Relojes Duales (Hora Local / Japón / País de Origen) [En Especificación - Beta]**
   - Visualización simultánea de la hora actual según ubicación GPS/red, hora de Japón (`Asia/Tokyo`) y hora del país/zona horaria de origen seleccionada por el usuario.
   - Indicador de diferencia horaria y tramo del día (mañana/tarde/noche) para evitar llamadas a deshoras.
2. **Conversor de Divisa Offline-First (`EUR <-> JPY` por defecto) [En Especificación - Beta]**
   - Conversión bidireccional instantánea entre Euro (`EUR`), Yen Japonés (`JPY`) y otras divisas soportadas.
   - Descarga y actualización de tasas de cambio desde fuente open-source (**Frankfurter API** - datos del Banco Central Europeo).
   - Caché local persistente con fecha de última actualización y tasa semilla inicial para funcionar 100% offline desde el primer arranque.
3. **Bóveda de Documentos de Identidad (Pasaporte, DNI y Documentos Personalizados con Zoom y PDF) [En Especificación - Beta]**
   - Acceso rápido offline a las fotografías o PDFs de tu Pasaporte y DNI/documento de identidad (anverso/reverso) para mostrar en controles, hoteles o compras *Tax-Free*.
   - **Soporte Multi-Formato PDF e Imágenes**: Permite adjuntar y previsualizar tanto fotos como documentos PDF oficiales para pasaporte y DNI, además de documentos personalizados.
   - **Bloqueo Biométrico Obligatorio con Excepción de Ciclo de Vida**: Protección biométrica/PIN estricta sin toggles superfluos, con gestión inteligente de ciclo de vida para evitar rebloqueos al tomar fotos con la cámara o seleccionar archivos del sistema.
   - **Documentos Personalizados con Foto o PDF**: Añade certificados de vacunas, recetas médicas para aduanas japonesas (*Yakkan Shoumei*), carnet internacional de conducir, visados o pólizas con editor modal y chips de sugerencias rápidas.
   - **Visor a Pantalla Completa con Zoom Táctil**: Toca cualquier foto para abrirla en pantalla completa con fondo oscuro y zoom fluido pellizcando (`1.0x` a `5.0x`), desplazamiento panorámico y alternador por doble toque.
4. **Repositorio de Documentos y Guías de Viaje [En Especificación - Beta]**
   - Gestión y visualización offline de PDFs e imágenes de billetes de vuelo, reservas de hotel, JR Pass, seguros de viaje y entradas.
   - **Gestor y Creador de Guías de Viaje**: Permite tanto importar guías externas (PDF con marcador de página, Markdown, mapas en imagen) como crear y editar guías propias por ciudad/región con texto enriquecido y fotos adjuntas offline.
   - **Exportación e Importación JSON en Ajustes**: Exporta e importa colecciones de Guías de Viaje y Billetes/Reservas mediante archivos JSON legibles desde almacenamiento local o Google Drive.
5. **Planificador de Viaje con Fotos, Google Maps, Calendario y Horarios [En Especificación - Beta]**
   - Calendario de viaje por días y línea temporal de horarios (`Timeline` en zona horaria local del destino y persistencia ISO UTC) con detección de solapamientos.
   - **Fechas Regionales del Sistema y Zonas Horarias Locales**: Visualización de la fecha completa según la configuración regional del sistema (`FormatStyle.FULL`) y de las horas en el huso local de cada destino (ej. Múnich `UTC+1`, Osaka/Tokio `UTC+9`) indicando siempre el desfase y la ciudad.
   - **Diseño Ergonómico de Botones**: Tarjetas de itinerario con botones compactos y sutiles (`🗺️ Mapa`, `📅 Cal.`, `✏️ Editar`, `🗑️`) que reducen la altura y el ruido visual.
   - Galería de fotos offline asociada a cada parada o actividad del itinerario.
   - **Integración dual con Google Maps**: Mapa interactivo embebido (`Maps Compose`) con marcadores numerados de la ruta del día + almacenamiento offline de coordenadas/direcciones en japonés y lanzamiento directo de rutas en la app de Google Maps.
   - Sincronización/exportación opcional al Calendario del sistema / Google Calendar, vinculación directa con PDFs de reservas y guías de viaje, y **exportación/importación en formato `JSON` legible (`japan-travel-plan.v1`) desde Ajustes (Almacenamiento Local o Drive con SAF)**.
6. **Planificador de Vuelos, Trenes, Metros y Autobuses [En Especificación - Beta]**
   - Gestión de trayectos multimodales (vuelos internacionales/domésticos, trenes **Shinkansen / JR**, líneas de **Metro** y **Autobuses** interurbanos/urbanos) 100% offline e **importables/exportables en `JSON` legible desde Ajustes**.
   - **Vuelos**: Apertura de check-in online, hora de facturación en mostrador y cierre de equipaje, hora de embarque, puerta, asiento, salida, llegada (`+1`/`+2` días), visualización de zonas horarias de origen y destino con desfases UTC, y cálculo automático de duración real entre zonas horarias.
   - **Trenes, Metros y Autobuses**: Línea y color oficial, códigos de estación (`G19` ➔ `G01`), nombres en japonés para paneles, andén (`Platform`), número de coche/vagón recomendado, asiento, reserva de maletas grandes en Shinkansen, cobertura **JR Pass / Suica**, **número exacto de salida de estación (`Exit`)** y dársena de autobús (`Bus Bay`).
   - **Calculador de Escalas y Transbordos (`Layovers & Transfers`)**: Tiempo total de conexión, margen hasta el siguiente embarque o cambio de andén/terminal, alerta de conexión ajustada y acceso directo a tarjetas de embarque o billetes QR (`SPEC-03`).
7. **Asistente IA con Google Gemini y Acceso Directo a Traducción Simultánea [En Especificación - Beta]**
   - **Acceso Directo Global a Traducción Simultánea (`ES/EN <-> Japonés`)**: Modo conversación cara a cara por voz y texto (con selector en pestañas estilizadas `🎙️ Voz`, `✍️ Texto` y `📷 Carteles / Cámara`, pantalla dividida reversible 180°, pronunciación en *Romaji*, OCR de cámara para menús/carteles con ML Kit Offline y salto rápido a *Google Translate / Pixel Live Translate*).
   - **Orientación EXIF, Rotación 90° y Selección de Región de Imagen**: Corrección automática de la rotación de sensor de cámara (`ExifInterface`) para evitar fotos apaisadas o giradas, botón manual `🔄 Girar 90°` y diálogo interactivo con presets (`Toda la foto`, `Superior`, `Centro`, `Inferior`, `Personalizada`) para seleccionar qué parte de la imagen recortar y traducir.
   - **Frases de Viaje Unificadas y Deduplicadas**: Sección unificada al pie del traductor con 28 frases esenciales categorizadas (*Cortesía, Restaurante, Transporte, Compras, Emergencias*), texto bilingüe (español/inglés + kanji/kana), transcripción fonética en Romaji y botón instantáneo de locución `🔊` en japonés nativo (`ja-JP`).
   - **Asistente de Viaje con Gemini (`Gemini API` + `Gemini Nano On-Device` en Pixel 9/10 Pro XL)**: Análisis visual de cartas de restaurantes japoneses (con conversión automática de precios a `EUR`), recomendaciones contextuales y **Creador / Optimizador de Planes de Viaje (`AI Plan Builder`)** capaz de generar nuevos itinerarios y mejorar los planes existentes en la app (resolviendo conflictos horarios y añadiendo nuevas jornadas y conexiones Shinkansen/vuelos).
   - **Método Amigable para Obtener la API Key**: Botón directo de 1 toque a Google AI Studio (`aistudio.google.com/app/apikey`), tutorial interactivo en 4 pasos, botón de pegado automático desde el portapapeles y conmutador de visibilidad (`GeminiApiKeySetupCard`).
8. **Previsión Meteorológica y Planificación Climática con Open-Meteo (`SPEC-07`) [En Especificación - Beta]**
   - Integración con **Open-Meteo API** (modelos globales + **JMA — Japan Meteorological Agency**, 100% open-source y sin API Key) con semilla estacional y caché **Offline-First** para *Tokio*, *Kioto*, *Osaka* y *Hakone / Monte Fuji*.
   - **Búsqueda Predictiva y Coordenadas de Google Maps**: Sugerencias predictivas instantáneas reactivas mientras se escribe el nombre de la ciudad y obtención automática de coordenadas geográficas de Google Maps y Open-Meteo con catálogo local (>50 ciudades y destinos turísticos) 100% offline.
   - **Útil para el Usuario**: Clasificación diaria (`⛩️ Ideal Exterior` vs. `🏛️ Recomendado Cubierto`), avisos de paraguas (`☔ %`), temperaturas máx/mín y **porcentaje de visibilidad del Monte Fuji (`🗻 %`)** integrado en cada día del Planificador de Viaje.
   - **Útil para la IA (`Weather-Aware AI Planner`)**: Inyecta la previsión de Open-Meteo en `AiPlanBuilderEngine` para que la IA adapte automáticamente las paradas del itinerario en días de lluvia añadiendo alternativas cubiertas (*mercados Nishiki/Kuromon*, *galerías Shotengai*, museos).

---

## Metodología de Desarrollo (SDD + TDD + Gitflow)

Este proyecto se rige por las directrices definidas en [`AGENTS.md`](AGENTS.md):

1. **Especificación (`/docs/specs/`)** -> **Análisis (`/docs/analysis/`)** -> **Tareas (`/docs/tasks/`)** -> **Implementación (TDD)**.
2. **Gitflow**:
   - `main`: Releases estables y betas publicadas con su correspondiente APK en GitHub Releases.
   - `develop`: Rama de integración continua de desarrollo.
   - `feature/*`, `release/*`, `hotfix/*`: Ramas de trabajo.
3. **Conventional Commits v1.0.0**: Historial semántico estandarizado (`feat`, `fix`, `docs`, `test`, `refactor`, `chore`).
4. **Política de Versiones y Releases**:
   - Comenzamos en la versión `0.0.1`.
   - Toda herramienta no implementada al 100%, probada y aprobada mantiene la versión en estado **Beta** (`-beta.X`).
   - Cada publicación en `main` genera su tag y una **GitHub Release** con el archivo `.apk` funcional adjunto.
5. **Revisión y Respuesta Activa a GitHub Issues**:
   - Durante el desarrollo se revisan periódicamente los **Issues de GitHub**, analizando técnicamente si procede o no su reparación, respondiendo siempre en el propio issue y aplicando el ciclo SDD + TDD para los issues aceptados.
6. **Despliegue ADB Debug en Dispositivos Autorizados (Sin Commitear)**:
   - Permite instalar y lanzar la versión Debug mediante ADB simultáneamente en la lista local de dispositivos autorizados (`authorized-devices.local.conf`, excluido en `.gitignore`) ejecutando `./gradlew installAuthorizedDebug` con auto-descubrimiento mDNS en redes Wi-Fi (ver plantilla en [`authorized-devices.example.conf`](authorized-devices.example.conf)).

---

## Estructura de Documentación (`/docs`)

Toda la documentación técnica y funcional se encuentra en español dentro del directorio [`/docs`](docs/):

- **[Índice General de Documentación](docs/index.md)**
- **Especificaciones (SDD)**:
  - [00 - Visión General, Configuración, Idioma, Tema y Offline-First](docs/specs/00-core-configuration-spec.md)
  - [01 - Herramienta: Relojes y Ubicación (Hora Local y País Origen)](docs/specs/01-world-clock-spec.md)
  - [02 - Herramienta: Conversor de Divisa Offline-First (EUR <-> JPY)](docs/specs/02-currency-converter-spec.md)
  - [03 - Herramienta: Pasaporte/DNI, Repositorio de Documentos y Guías de Viaje](docs/specs/03-documents-and-id-vault-spec.md)
  - [04 - Herramienta: Planificador de Viaje (Fotos, Google Maps, Calendario y Horarios)](docs/specs/04-trip-planner-spec.md)
  - [05 - Herramienta: Planificador de Transporte (Vuelos, Trenes/Shinkansen, Metros y Autobuses)](docs/specs/05-flight-planner-spec.md)
  - [06 - Herramienta: Asistente IA con Gemini y Acceso Directo a Traducción Simultánea](docs/specs/06-gemini-ai-and-translator-spec.md)
  - [07 - Herramienta: Previsión Meteorológica con Open-Meteo para Planificación de Usuario e IA](docs/specs/07-weather-open-meteo-spec.md)
  - [08 - Guía y Esquema JSON para Generación con IA y Portabilidad](docs/specs/08-travel-plan-json-schema-guide.md)
- **Análisis Técnico**:
  - [Arquitectura, Stack Tecnológico y Estrategia TDD](docs/analysis/architecture-and-tech-stack.md)
- **Planificación de Tareas**:
  - [Backlog y Desglose de Tareas SDD + TDD](docs/tasks/backlog.md)
- **Guías**:
  - [Guía de Usuario Completa](docs/user-guide.md)
  - [Flujo de Trabajo: Gitflow, Conventional Commits y Releases](docs/development-workflow.md)

---

## Próximos Pasos (TODO Inmediato)
- [x] **Template moderno seleccionado (`TASK-004`)**: Basado en **[JetBrains KotlinConf App](https://jetbrains.github.io/kotlinconf-app/)** con paleta de colores tradicionales de Japón (*Washi*, *Sumi*, *Akabeni Torii*, *Ai-iro Indigo*, *Matcha*).
- [x] **Scaffolding del proyecto Android y compilación `0.0.1-beta.1` (`TASK-005`, `TASK-006`)**: Proyecto Kotlin + Jetpack Compose compilando con éxito y tests unitarios en verde.
- [x] **Suite Completa Funcional `v0.0.1-beta.1` Implementada con TDD (`Hitos 2 al 8`)**:
  - Relojes duales/triples con ubicación offline + Conversor `EUR <-> JPY` con Frankfurter API y semilla offline.
  - Planificador de viaje con fotos, mapa numerado e integración con Google Maps y Calendar.
  - Planificador multimodal de Vuelos, Trenes Shinkansen, Metros y Autobuses con calculador de escalas y transbordos.
  - Bóveda de Pasaporte y DNI + Repositorio de Documentos y Guías de Viaje offline.
  - Asistente IA con Gemini (con conversor de precios de menús en `¥` a `EUR`) y Traducción Simultánea 180°.
  - Indicador de conexión en vivo (`Wi-Fi / SIM / eSIM / Offline`) con selector rápido, Ajustes persistentes, Sincronización con Cuenta de Google y apartado Acerca de / Licencias Open Source.
- [ ] **Probar en Google Pixel 10 Pro XL vía ADB Wireless Debugging (`./gradlew installAuthorizedDebug`)** y validar feedback del usuario (`Hito 9`).

---

## Historial de Cambios

Consulta el archivo [`CHANGELOG.md`](CHANGELOG.md) para ver el detalle de cada versión.
