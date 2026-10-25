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

### RF-00.2: Tema Visual (Claro / Oscuro / Sistema)
1. La aplicación implementará **Material Design 3** con soporte para tres modos de tema:
   - `Sistema (Por defecto)`: Sigue la configuración de modo oscuro del sistema Android.
   - `Claro (Light)`: Fuerza paleta clara.
   - `Oscuro (Dark)`: Fuerza paleta oscura.
2. La preferencia del usuario se persistirá localmente en `DataStore` y se aplicará de forma reactiva en toda la interfaz sin parpadeos al iniciar.

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
