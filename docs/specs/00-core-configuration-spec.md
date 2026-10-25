# SPEC-00: Configuración Base, Internacionalización, Tema y Arquitectura Offline-First

- **ID**: `SPEC-00`
- **Estado**: `Aprobado para Análisis/Implementación`
- **Versión Objetivo**: `0.0.1-beta.1`

---

## 1. Objetivo

Establecer los cimientos funcionales y de experiencia de usuario de **Japan Travel Tools**, asegurando soporte multiidioma (Español e Inglés con fallback a Inglés), tema visual adaptable (Claro, Oscuro y Sistema), persistencia total de preferencias y funcionamiento **100% offline-first**.

---

## 2. Requisitos Funcionales (RF)

### RF-00.1: Internacionalización (Multiidioma ES / EN)
1. La aplicación soportará dos idiomas completos en toda su interfaz:
   - **Inglés (`en`)**: Idioma base por defecto (`res/values/strings.xml`).
   - **Español (`es`)**: Traducción completa (`res/values-es/strings.xml`).
2. **Comportamiento por defecto**:
   - En el primer inicio, la aplicación utilizará el **idioma del sistema operativo** del dispositivo.
   - Si el idioma del sistema es Español (`es-*`), se mostrará en Español.
   - Si el idioma del sistema es Inglés (`en-*`) o **cualquier otro idioma no soportado** (ej. Francés, Alemán, Japonés), el idioma por defecto será **Inglés (`en`)**.
3. **Selección manual y persistencia**:
   - Desde la pantalla de Ajustes (`Settings`), el usuario podrá elegir entre:
     - `Sistema (Por defecto)`
     - `Español`
     - `English`
   - El cambio debe aplicarse de inmediato sin perder el estado de navegación y persistirse para futuros reinicios.

### RF-00.2: Tema Visual, Estética inspirada en `kotlinconf-app` y Paleta de Colores de Japón (`Nippon Dentōshoku`)
1. La aplicación soportará tres modos de tema persistidos reactivamente en `DataStore`:
   - `Sistema (Por defecto)`: Sigue la configuración de modo oscuro del sistema Android.
   - `Claro (Light — Washi Paper)`: Fuerza la paleta clara inspirada en el papel tradicional japonés *Washi*.
   - `Oscuro (Dark — Sumi Ink)`: Fuerza la paleta oscura de alto contraste inspirada en la tinta *Sumi* y la noche de Tokio.
2. **Template Visual y Sistema de Componentes (`JetBrains/kotlinconf-app` Blueprint)**:
   - La estructura visual, jerarquía tipográfica y componentes seguirán el diseño moderno de **[JetBrains KotlinConf App](https://jetbrains.github.io/kotlinconf-app/)**:
     - **Tarjetas limpias con borde sutil (`1.dp`) y radio redondeado consistente (`16.dp` - `20.dp`)** sin abuso de sombras difusas, lo que además proporciona nitidez máxima en la tablet **TCL NXTPAPER 11 Plus** y elegancia moderna en **Pixel 10 Pro XL / Pixel 9 Pro XL**.
     - **Cabeceras de fecha y filtros estilo *Agenda/Schedule* (`Pill Chips`)** con scroll horizontal suave para alternar días del itinerario, ciudades (*Tokio*, *Kioto*, *Osaka*) o categorías de documentos.
     - **Tipografía con jerarquía editorial y números tabulares (`tnum`)** para que los relojes duales, horarios del planificador y cifras del conversor de yenes estén siempre perfectamente alineados.
     - **Barra de navegación flotante/estilizada** en smartphones (`NavigationBar`) y barra lateral limpia (`NavigationRail`) en tablet.
3. **Paleta de Colores Tradicionales de Japón (`日本の伝統色` — Nippon Colors)**:
   - **Acento Principal (`Akabeni` / `Torii Vermilion`)**:
     - Modo Claro: `#C9171E` (Carmesí profundo del Sol Naciente / Santuarios Torii).
     - Modo Oscuro: `#FF5A5F` (Coral/Bermellón luminoso de alta legibilidad sobre fondo oscuro).
   - **Acento Secundario (`Ai-iro` — Índigo Japonés)**:
     - Modo Claro: `#165E83` | Modo Oscuro: `#64B5F6`.
   - **Acento Terciario (`Matcha` / `Sakura`)**:
     - Verde Matcha (`#5B8930` / `#81C784`) para estados sincronizados/offline OK y Rosa Sakura (`#F4A7B9`) para destacados/favoritos (`Pinned`).
   - **Fondos y Superficies**:
     - Modo Claro (*Washi*): Fondo principal `#FAF7F2` (blanco cálido papel Washi), tarjetas `#FFFFFF` con borde `#E6E0D6`, texto principal `#1C1B1F` (*Sumi* carbón).
     - Modo Oscuro (*Sumi / OLED*): Fondo principal `#0F1115` (negro carbón profundo para los paneles OLED de Pixel 9/10 Pro XL), tarjetas `#181B22` con borde `#2A2F3A`, texto principal `#F4F1EA`.

### RF-00.3: Persistencia de Configuración
1. Toda configuración modificada por el usuario en la app o en cualquiera de sus herramientas (idioma, tema, país de origen en relojes, divisas seleccionadas, bloqueo biométrico, etc.) se guardará automáticamente en almacenamiento local persistente.
2. Al cerrar y volver a abrir la app (o reiniciar el dispositivo), se restaurará exactamente el último estado configurado.

### RF-00.4: Funcionamiento 100% Offline-First
1. La aplicación nunca bloqueará su uso ni mostrará pantallas vacías por falta de conexión a internet.
2. Cualquier dato proveniente de fuentes externas (como tasas de cambio de divisas o geocodificación de zona horaria) deberá:
   - Incluir datos semilla (`seed data`) empaquetados en la app para funcionar incluso si el primer arranque ocurre en modo avión.
   - Cachearse y persistirse en base de datos local (`Room` / `DataStore`) cada vez que se obtenga una actualización exitosa.
   - Mostrar de forma clara cuándo se actualizaron los datos por última vez y si se está operando en modo offline/cacheado.

### RF-00.5: Indicador de Estado Beta por Herramienta
1. Mientras una herramienta no esté implementada al 100%, validada y aprobada por el usuario, mostrará una etiqueta visual discreta `BETA` y la versión de la aplicación se mantendrá bajo numeración pre-release (`0.0.1-beta.X`).

### RF-00.6: Apartado "Acerca de" (Versión de la App y Librerías Open Source de Terceros)
1. Dentro de Ajustes (`Settings` / `About`), la aplicación contará con un apartado **"Acerca de / Información de la App"** accesible 100% offline que mostrará:
   - **Nombre de la aplicación** y logotipo.
   - **Versión exacta instalada**: `versionName` (ej. `0.0.1-beta.1` o `0.0.1`), `versionCode` y estado de madurez (`BETA` o `Estable`).
   - **Fuentes de datos abiertas utilizadas**: Mención explícita a **Frankfurter API** (datos del Banco Central Europeo) para los tipos de cambio.
2. **Listado de Librerías Open Source de Terceros (`Open Source Licenses`)**:
   - Incluirá una pantalla dedicada donde se listen **todas las librerías open source de terceros** empleadas en el proyecto (ej. *Jetpack Compose*, *Room*, *DataStore*, *Kotlin Coroutines*, *Ktor / OkHttp / Retrofit*, *Kotlinx Serialization*, *Maps Compose*, *Coil*, etc.).
   - Por cada librería se mostrará:
     - Nombre de la librería y autor/organización.
     - Versión utilizada y artefacto Maven.
     - Tipo y texto completo de la licencia open source (ej. `Apache License 2.0`, `MIT License`) empaquetado localmente en el APK (mediante plugin de Gradle como **AboutLibraries** o `oss-licenses-plugin`) para que pueda consultarse **sin conexión a internet**.

### RF-00.7: Optimización Adaptativa para Dispositivos Objetivo (Pixel 10 Pro XL, Pixel 9 Pro XL y Tablet TCL NXTPAPER 11 Plus)
La interfaz de usuario, el rendimiento y la ergonomía táctil deberán estar específicamente optimizados para tres dispositivos de referencia:

1. **Smartphones de Gran Formato — Google Pixel 10 Pro XL y Google Pixel 9 Pro XL (~6.8" Super Actua LTPO OLED, 1344 x 2992 px, 20:9, 1-120 Hz)**:
   - **Diseño Edge-to-Edge (`enableEdgeToEdge`)**: Aprovechamiento completo de la pantalla de borde a borde respetando los *insets* del recorte de cámara (`displayCutout`), barra de estado y barra de gestos inferior.
   - **Ergonomía a una mano**: Controles de acción frecuente (teclado rápido de divisas, cambio de día, botones de acción flotantes y barra de navegación inferior `NavigationBar`) situados en el tercio inferior de la pantalla de 6.8" para alcanzarlos cómodamente con el pulgar.
   - **Fluidez 120 Hz y OLED Dark Mode**: Animaciones sin tirones (*Baseline Profiles* + estado inmutable en Compose) y paleta oscura de alto contraste real para paneles LTPO OLED, además de soporte opcional para colores dinámicos (*Material You / Dynamic Color* en Android 15/16).

2. **Tablet de Lectura — TCL NXTPAPER 11 Plus (11.5" 2.2K, 2200 x 1440 px, ratio 3:2, 120 Hz, pantalla mate estilo papel)**:
   - **Layouts Adaptativos Multi-Panel (`WindowSizeClass` Medium / Expanded y `NavigationSuiteScaffold`)**:
     - Sustitución automática de la barra inferior por **`NavigationRail`** lateral (o `PermanentNavigationDrawer` en horizontal) para no desperdiciar espacio vertical en formato 3:2.
     - **Planificador de Viaje (`SPEC-04`) en Doble Panel (50% / 50%)**: En la TCL NXTPAPER 11 Plus se muestra simultáneamente la **Línea Temporal de Horarios + Fotos** a la izquierda y el **Mapa Interactivo de Google Maps embebido** a la derecha, sincronizados en tiempo real sin necesidad de cambiar de pestaña.
     - **Documentos y Guías de Viaje (`SPEC-03`) en Maestro-Detalle (`ListDetailPaneScaffold`)**: Lista de documentos/guías a la izquierda y el visor de **PDF / Guía Markdown / Pasaporte** abierto a gran tamaño a la derecha, aprovechando los 11.5" en proporción 3:2 ideales para lectura de documentos y mapas.
     - **Dashboard / Relojes y Conversor en Cuadrícula (`Grid`)**: Posibilidad de ver los relojes duales y el conversor de divisas en tarjetas paralelas en una sola vista de escritorio de viaje.
   - **Modo de Alto Contraste / Tinta para Pantalla NXTPAPER**:
     - Paleta y tipografía con contraste reforzado y bordes definidos (`OutlinedCard` / trazos nítidos sin depender de sombras difusas tenues), optimizada tanto para el modo a color mate como para los modos de lectura **Ink Paper (Blanco y Negro)** y **Color Paper** propios de la tecnología TCL NXTPAPER.

### RF-00.8: Iconografía Moderna, Generación de Icono de App (Adaptive Icon) y Splash Screen Nativa
1. **Iconografía Moderna en toda la Interfaz**:
   - Se prohíbe el uso de iconos heredados antiguos o emojis planos como sustitutos de iconos de navegación/acción.
   - Se utilizará un sistema de **iconos vectoriales modernos (`VectorDrawable` / `ImageVector`)** de trazo limpio y redondeado (`2.dp` stroke, estilo **Lucide Icons / Phosphor Icons / Material Symbols Rounded** en línea con **[JetBrains KotlinConf App](https://jetbrains.github.io/kotlinconf-app/)**), con estados `Outlined` (inactivo) y `Filled/Duotone` (activo).
   - Iconos temáticos personalizados para el contexto de viaje a Japón (Torii / Templo, Yen `¥`, Shinkansen / Tren bala, Avión / Escala, Pasaporte Biométrico, Reloj Dual, Mapa de Ruta).
2. **Generación de Icono de la Aplicación (`Adaptive Launcher Icon` + `Monochrome`)**:
   - Se diseñará y generará el icono oficial de **Japan Travel Tools** cumpliendo el estándar **Android Adaptive Icons** (`res/mipmap-anydpi-v26/ic_launcher.xml` y `ic_launcher_round.xml`):
     - **Background Layer**: Fondo en textura/tono cálido *Washi* (`#FAF7F2`) o Índigo profundo *Ai-iro* (`#165E83`) con geometría sutil japonesa (*Seigaiha* / ondas).
     - **Foreground Layer**: Emblema minimalista y moderno que combina el **Sol Naciente (`Akabeni` `#C9171E`)**, la silueta de un **Torii / Monte Fuji** y un **avión/brújula de viaje** en trazos vectoriales limpios.
     - **Monochrome Layer (Android 13+ Themed Icons)**: Capa monocromática vectorial pura para que en los **Google Pixel 10 Pro XL y Pixel 9 Pro XL** el icono se adapte automáticamente a los iconos temáticos de *Material You* del escritorio de Pixel Launcher.
3. **Generación de Splash Screen Nativa (`androidx.core:core-splashscreen`)**:
   - Implementación mediante la API oficial **AndroidX SplashScreen** (Android 12+ y compatible hacia atrás):
     - Fondo adaptado automáticamente al tema del sistema (*Washi* `#FAF7F2` en modo claro / *Sumi* `#0F1115` en modo oscuro para evitar fogonazos blancos en pantallas OLED por la noche).
     - **Icono central animado (`windowSplashScreenAnimatedIcon`)** con transición fluida hacia la pantalla principal una vez cargadas las preferencias de `DataStore` (sin pantallas de carga artificiales ni bloqueos).

### RF-00.9: Indicador En Vivo de Tipo de Conexión a Internet y Selector Rápido (Wi-Fi / SIM / eSIM / Offline)
En un viaje a Japón es habitual alternar entre una **eSIM de datos japonesa** (ej. Ubigi, Airalo, Holafly), la **SIM física del país de origen**, **Pocket Wi-Fi / Wi-Fi de hoteles** y el **Modo Offline/Avión**.
1. **Píldora / Widget de Estado de Conexión en Cabecera / Home (`ConnectivityStatusPill`)**:
   - Visible de un vistazo en la barra superior / pantalla Home, actualizándose en tiempo real mediante `ConnectivityManager.NetworkCallback`, `TelephonyManager` y `SubscriptionManager`:
     - **Wi-Fi**: Muestra icono vectorial de Wi-Fi + estado de validación de internet (y si la conexión es medida `Metered`, útil cuando se conecta a un *Pocket Wi-Fi* o punto de acceso móvil).
     - **Datos Móviles — Distinción SIM Física vs. eSIM**:
       - Identifica si la línea de datos activa corresponde a una **eSIM (`isEmbedded == true`)** o a una **SIM física**, mostrando el nombre del operador activo (ej. `eSIM · NTT Docomo / SoftBank` o `SIM · Movistar`), tipo de red (`5G` / `LTE`) y un **aviso destacado en rojo `Akabeni` si detecta Roaming activo en la SIM de origen** para evitar cargos accidentales.
     - **Offline / Sin Conexión**: Muestra insignia clara `Offline Mode (Caché Local Activa)` en tono ámbar/gris para confirmar que la app opera 100% con datos locales.
2. **Cambio Rápido de Conexión (`Quick Connectivity Switcher Sheet`)**:
   - Al tocar el indicador de conexión en el Home/Cabecera, se despliega un panel rápido con acciones directas sin tener que buscar en los menús profundos de Android:
     - **Panel Flotante Nativo de Internet (`Settings.Panel.ACTION_INTERNET_CONNECTIVITY`)**: Abre el panel deslizable nativo de Android (especialmente rápido en **Pixel 10 Pro XL y Pixel 9 Pro XL**) para encender/apagar Wi-Fi, cambiar de red Wi-Fi o conmutar datos móviles al instante sin salir de la app.
     - **Selector de SIM / eSIM de Datos (`ACTION_NETWORK_OPERATOR_SETTINGS` / `ACTION_MANAGE_ALL_SIM_PROFILES_SETTINGS`)**: Acceso directo de un toque para alternar qué tarjeta (**SIM física vs. eSIM de Japón**) lleva los datos móviles activos o gestionar perfiles eSIM.
     - **Modo Solo Offline en la App (Opcional)**: Interruptor para forzar a **Japan Travel Tools** a no realizar peticiones de red aunque el teléfono tenga datos activos (modo ahorro extremo de datos de eSIM).

---

## 3. Criterios de Aceptación (Gherkin / BDD)

### Escenario 1: Idioma del sistema soportado vs no soportado
- **Dado** que el usuario abre la app por primera vez con la opción de idioma en `Sistema`
- **Cuando** el dispositivo está configurado en `es_ES`
- **Entonces** la interfaz se muestra en Español.
- **Y cuando** el dispositivo está configurado en `fr_FR` (no soportado)
- **Entonces** la interfaz hace fallback automático a Inglés (`en`).

### Escenario 2: Cambio y persistencia del tema visual
- **Dado** que el tema por defecto es `Sistema`
- **Cuando** el usuario selecciona `Oscuro` en Ajustes y reinicia la aplicación
- **Entonces** la aplicación arranca directamente en modo `Oscuro`.

### Escenario 3: Arranque en modo avión
- **Dado** que el dispositivo no tiene conexión a internet (Modo Avión activo)
- **Cuando** el usuario navega por todas las herramientas (Relojes, Conversor de Divisas, Pasaporte/DNI y Documentos)
- **Entonces** todas las herramientas funcionan sin errores utilizando datos locales o cacheados.

### Escenario 4: Consulta de versión y licencias Open Source en modo offline
- **Dado** que el usuario entra en `Ajustes > Acerca de` sin conexión a internet
- **Cuando** consulta la información de la aplicación y pulsa en *"Librerías de código abierto / Licencias Open Source"*
- **Entonces** visualiza la versión actual (`0.0.1-beta.X` / `0.0.1`) y la lista completa de librerías de terceros con sus respectivas versiones y textos de licencia cargados localmente.

### Escenario 5: Adaptación automática entre Pixel 9/10 Pro XL y Tablet TCL NXTPAPER 11 Plus
- **Dado** que el usuario abre la aplicación en un **Pixel 10 Pro XL** o **Pixel 9 Pro XL** (`WindowWidthSizeClass.Compact`)
- **Entonces** la navegación se muestra mediante `NavigationBar` inferior ergonómica con diseño Edge-to-Edge y vistas de un solo panel.
- **Y cuando** abre la misma aplicación en una tablet **TCL NXTPAPER 11 Plus** (`WindowWidthSizeClass.Medium` / `Expanded`, pantalla 11.5" 3:2)
- **Entonces** la navegación cambia automáticamente a `NavigationRail` lateral, el Planificador muestra el horario y el mapa de Google Maps lado a lado en doble panel, y las Guías/Documentos se abren en vista Maestro-Detalle con contraste nítido optimizado para pantalla NXTPAPER.

### Escenario 6: Visualización de Adaptive Icon (con soporte Material You en Pixel) y Splash Screen
- **Dado** que el usuario instala la app en un **Pixel 10 Pro XL / Pixel 9 Pro XL** con "Iconos temáticos" activados o desactivados
- **Cuando** visualiza el icono en el launcher y abre la app
- **Entonces** el icono muestra el diseño vectorial japonés (o su variante monocromática adaptada al color del Pixel), y al abrirse presenta la **Splash Screen** nativa con el color de fondo acorde al modo Claro/Oscuro antes de entrar sin parpadeos a la pantalla principal.

### Escenario 7: Detección en vivo del tipo de conexión (Wi-Fi / SIM / eSIM / Offline) y cambio rápido
- **Dado** que el usuario está en la pantalla principal (Home) de la aplicación
- **Cuando** el dispositivo está conectado mediante datos móviles de una **eSIM** (o mediante **Wi-Fi**, **SIM física** o **Sin conexión**)
- **Entonces** la píldora de estado en la cabecera muestra en tiempo real el tipo de conexión activo (ej. `eSIM · 5G`, `Wi-Fi` u `Offline`).
- **Y cuando** el usuario toca sobre dicho indicador de conexión
- **Entonces** se abre el selector rápido que le permite desplegar el panel nativo de conectividad de Android (`Settings.Panel.ACTION_INTERNET_CONNECTIVITY`) o el gestor de **SIM / eSIM** para cambiar entre Wi-Fi, SIM y eSIM en un toque.
