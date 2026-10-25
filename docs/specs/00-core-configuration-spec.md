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
3. **Paleta de Colores Descansada y Ergonomía Visual (Calm Japanese Slate & Washi)**:
   - **Diseño No Cargante en Modo Oscuro**: Se prohíbe el uso de bordes rojos/azules saturados alrededor de las tarjetas o fondos rojos intensos que fatiguen la vista. Las tarjetas utilizan superficies neutras suaves (`#1B1F27` sobre fondo `#111318` con borde sutil neutro `#2E3440`).
   - **Acento Principal Sereno (`Ai-iro Soft` / Índigo Pizarra)**:
     - Modo Claro: `#1E567A` | Modo Oscuro: `#8AB4F8` (Azul pizarra suave y descansado).
   - **Acento Secundario (`Matcha Sage` / Verde Salvia)**:
     - Modo Claro: `#3F6E4E` | Modo Oscuro: `#9CCFA4`.
   - **Tipografía Amplia y Legible**:
     - Cuerpo de texto principal en `15.sp`–`16.sp`, textos secundarios nunca inferiores a `13.sp`–`14.sp`, títulos de tarjeta en `18.sp`–`20.sp` y cifras clave en `26.sp`–`32.sp`.
   - **Navegación por Menú Lateral (`ModalNavigationDrawer` / `PermanentNavigationDrawer`)**:
     - Toda la navegación entre las herramientas de la aplicación se realiza mediante un **Menú Lateral Desplegable (`ModalNavigationDrawer`)** accesible desde el botón de menú `☰` en la barra superior (`TopAppBar`) o deslizando desde el borde izquierdo (y panel lateral fijo en tablet).
     - El menú lateral organiza las 12 herramientas en grupos claros (*Herramientas Rápidas*, *Planificación y Rutas*, *Voz e Inteligencia Artificial*, *Documentos y Guías*, *Sistema*), permitiendo abrir directamente cada herramienta en su propia pantalla limpia sin filas de sub-pestañas ni barras inferiores saturadas.
   - **Estandarización Estricta de Botones (Cero Descuadres)**:
     - Todos los botones de acción principales (`AppPrimaryButton`) y secundarios (`AppSecondaryButton`) comparten exactamente la misma geometría: **altura fija de `50.dp`**, radio `RoundedCornerShape(12.dp)`, tipografía `15.sp` con `maxLines = 1` y `TextOverflow.Ellipsis`, y dos únicos estilos cromáticos (`primary` y `surfaceVariant` con borde `outline`).
     - Cuando dos botones se colocan en paralelo (`AppButtonRow`), ambos utilizan `Modifier.weight(1f).height(50.dp)` garantizando alineación simétrica perfecta sin saltos de línea ni alturas desiguales.

### RF-00.3: Persistencia de Configuración
1. Toda configuración modificada por el usuario en la app o en cualquiera de sus herramientas (idioma, tema, país de origen en relojes, divisas seleccionadas, bloqueo biométrico, etc.) se guardará automáticamente en almacenamiento local persistente.
2. Al cerrar y volver a abrir la app (o reiniciar el dispositivo), se restaurará exactamente el último estado configurado.

### RF-00.4: Funcionamiento 100% Offline-First
1. La aplicación nunca bloqueará su uso ni mostrará pantallas vacías por falta de conexión a internet.
2. Cualquier dato proveniente de fuentes externas (como tasas de cambio de divisas o geocodificación de zona horaria) deberá:
   - Incluir datos semilla (`seed data`) empaquetados en la app para funcionar incluso si el primer arranque ocurre en modo avión.
   - Cachearse y persistirse en base de datos local (`Room` / `DataStore`) cada vez que se obtenga una actualización exitosa.
   - Mostrar de forma clara cuándo se actualizaron los datos por última vez y si se está operando en modo offline/cacheado.

### RF-00.5: Prohibición de Etiquetas `BETA` en la Interfaz de Usuario
1. Aunque la numeración interna de versiones pre-release en Git/GitHub utilice el sufijo `-beta.X` (`0.0.1-beta.X`) hasta la aprobación final de `0.0.1`, **queda estrictamente prohibido mostrar etiquetas, píldoras o insignias `BETA` en ningún lugar de la interfaz de la aplicación**, ya que no aportan información útil al usuario y consumen espacio en pantalla.

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

### RF-00.10: Revisión Continua, Análisis y Respuesta a GitHub Issues
1. Como parte integral del flujo SDD/TDD del proyecto, durante el desarrollo se revisará periódicamente el apartado de **GitHub Issues** del repositorio (`laralbir/japan-travel-tools`).
2. Por cada issue reportado:
   - Se realizará un **análisis técnico y funcional** para evaluar si procede o no su reparación/implementación.
   - **Siempre se responderá al issue en GitHub** dejando constancia del análisis:
     - Si **procede**: se comenta el diagnóstico y plan de resolución, se incorpora al flujo `Especificación -> Análisis -> Tareas -> TDD (Test que reproduce el bug -> Fix)`, se vincula el commit con `Closes #<id>` y se notifica al cerrar.
     - Si **no procede**: se responde explicando los motivos técnicos o de diseño por los que se descarta o se solicita aclaración al autor.

### RF-00.11: Sincronización Multi-Dispositivo con Cuenta de Google (Móvil Pixel <-> Tablet TCL NXTPAPER)
Dado que el usuario utilizará la aplicación simultáneamente en varios dispositivos (por ejemplo, planificando o leyendo guías en la **Tablet TCL NXTPAPER 11 Plus** en el hotel y consultando horarios/documentos en la calle con el **Google Pixel 10 Pro XL / Pixel 9 Pro XL**), la aplicación permitirá sincronizar todos los datos mediante la **misma Cuenta de Google** manteniendo intacto el principio **100% Offline-First**:
1. **Inicio de Sesión con Cuenta de Google (`Credential Manager API` — Sign in with Google)**:
   - Opcional (la app sigue funcionando 100% sin cuenta si el usuario no desea iniciar sesión).
   - Al vincular la misma cuenta de Google en el móvil y en la tablet, se habilita la sincronización en la nube privada del usuario (**Google Drive `appDataFolder` / Firebase Cloud Storage + Firestore**).
2. **Alcance Total de Datos Sincronizados**:
   - **Preferencias de usuario (`DataStore`)**: Idioma, tema, país de origen de relojes, divisas favoritas.
   - **Base de datos estructurada (`Room`)**:
     - Itinerarios, días y horarios del **Planificador de Viaje (`SPEC-04`)** con sus ubicaciones de Google Maps.
     - Trayectos de **Vuelos, Trenes, Metros y Autobuses (`SPEC-05`)**.
     - **Guías de Viaje (`SPEC-03`)**, notas y metadatos de documentos.
     - Historial de respuestas de **Gemini y frases guardadas (`SPEC-06`)**.
   - **Archivos físicos y fotografías (`Context.filesDir`)**:
     - Fotos del **Pasaporte y DNI** (cifradas en tránsito y almacenadas en el espacio privado de la cuenta del usuario), **PDFs de billetes/hoteles/guías** y **fotos de paradas del planificador**, descargándose automáticamente a la caché interna privada de cada dispositivo para que estén **disponibles 100% offline en todos ellos**.
3. **Motor de Sincronización Offline-First (`WorkManager` + Resolución de Conflictos `Last-Write-Wins` por Entidad)**:
   - Toda creación o edición en un dispositivo (incluso en Modo Avión) se guarda inmediatamente en local con una marca de tiempo `updatedAtMillis`, un contador de versión y un flag `pendingSync = true`.
   - En cuanto el dispositivo detecta conexión (`Wi-Fi`, `eSIM` o `SIM`, configurable para *"Sincronizar archivos pesados solo por Wi-Fi"* para no gastar datos de la eSIM en Japón), sincroniza automáticamente en segundo plano o al pulsar **"Sincronizar ahora 🔄"**.
   - En caso de edición concurrente en dos dispositivos offline, se resuelve por entidad individual mediante timestamp (`updatedAtMillis` más reciente prevalece) sin sobrescribir ni perder cambios de otras entidades.

### RF-00.12: Instalación y Depuración (ADB + Debug) sobre Listado Configurable de Dispositivos Autorizados (No Commiteable)
1. El proyecto debe permitir compilar, instalar y lanzar en modo **Debug** la aplicación mediante **ADB (`adb install` / `android install`)** sobre una lista configurable de dispositivos autorizados (por ejemplo, el **Pixel 10 Pro XL**, el **Pixel 9 Pro XL** y la tablet **TCL NXTPAPER 11 Plus**, tanto por cable USB como por *ADB Wireless Debugging*).
2. **Privacidad de Identificadores de Hardware (Git-Ignored)**:
   - La lista real de dispositivos autorizados (números de serie ADB e IPs locales de depuración) se almacenará en el archivo local **`authorized-devices.local.conf`**, el cual estará **estrictamente excluido del control de versiones en `.gitignore`** para que nunca se pueda commitear por error.
   - Se incluirá una plantilla versionada (`authorized-devices.example.conf`) y la tarea automatizada `./gradlew installAuthorizedDebug` (junto con `scripts/deploy-authorized-devices.sh`) que cruzará los dispositivos conectados en `adb devices` contra la lista autorizada e instalará la versión Debug únicamente en los dispositivos permitidos.

### RF-00.13: Diálogos y Modales de Confirmación de Seguridad Universal y Sistema de Double Check para Eliminaciones Sensibles
1. **Prevención Universal de Pérdidas Accidentales de Información de Viaje**:
   - Cualquier acción destructiva o de eliminación definitiva (eliminar un tramo de transporte/vuelo/tren, eliminar una parada del itinerario, eliminar un día completo, eliminar un plan de viaje, eliminar una ciudad meteorológica, eliminar fotos de pasaporte/DNI, vaciar itinerario o eliminar un documento/entrada/reserva o guía de viaje) **nunca debe ejecutarse inmediatamente sin confirmación previa**.
   - Toda acción de borrado debe desplegar un diálogo modal de confirmación (`AppConfirmationDialog`) que:
     - Identifique con claridad el tipo y nombre del elemento a eliminar.
     - Advierta que la acción no se puede deshacer.
     - Presente un botón de confirmación destructivo claramente visible (color de peligro / error `Akabeni`) y un botón de cancelación seguro.
2. **Sistema de Doble Comprobación (Double Check) para Eliminaciones Sensibles**:
   - Se clasifican como **Elementos Sensibles de Alto Riesgo**:
     - **Planes de Viaje Completos** (`deleteCurrentPlan`).
     - **Días Completos del Itinerario** (`deleteDay` con todas sus paradas asociadas).
     - **Fotografías de Identidad Oficial (Pasaporte y DNI)** (`deleteIdPhoto`).
     - **Vaciado Total del Itinerario** (`updatePlan` vacío).
   - Para estas acciones de alto impacto, el diálogo modal integra un **mecanismo interactivo de doble comprobación (Double Check)**:
     - Cabecera y marco visual de advertencia crítica (`🚨`).
     - Interruptor o verificación de seguridad obligatorio: *"🚨 DOBLE COMPROBACIÓN: Confirmo que deseo eliminar este elemento sensible de forma definitiva y entiendo que es 100% irreversible."*
     - El botón de confirmación destructivo permanece **estrictamente deshabilitado** hasta que el usuario activa deliberadamente el interruptor de doble comprobación, imposibilitando cualquier borrado accidental por toques rápidos o involuntarios.
3. **Confirmación en Acciones de Edición y Guardado de Cambios**:
   - Al modificar o sobrescribir datos críticos existentes en los editores modales, la interfaz debe solicitar confirmación o verificar la intención del viajero antes de persistir la sobrescritura, evitando cambios involuntarios al interactuar con el dispositivo en movimiento.

### RF-00.14: Componente Universal de Selector de Fechas Interactivo (`AppDatePickerField`)
1. **Sustitución de Cajas de Texto de Fecha por Selectores Interactivos**:
   - Queda prohibido obligar al usuario a teclear fechas manualmente como cadenas de texto (`YYYY-MM-DD`).
   - Todos los campos de fecha de la aplicación (fecha de inicio del plan de viaje, fecha de paradas del itinerario y fecha de tramos de vuelos/trenes) deben utilizar un selector interactivo nativo (`AppDatePickerField` con `DatePickerDialog` de Material 3).
2. **Ergonomía y Visualización Clara**:
   - El campo muestra un icono de calendario (`📅`), la fecha en formato localizado legible (ej. *"11 nov 2026"*) y el formato ISO estándar (`YYYY-MM-DD`).
   - Al tocar el campo o su icono, se abre el diálogo de calendario (`DatePickerDialog`) permitiendo elegir año, mes y día de forma táctil y cómoda sin teclado virtual.

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

### Escenario 8: Sincronización entre Tablet TCL NXTPAPER 11 Plus y Móvil Pixel 10/9 Pro XL con la misma cuenta de Google
- **Dado** que el usuario ha iniciado sesión con la misma cuenta de Google en su tablet **TCL NXTPAPER 11 Plus** y en su móvil **Pixel 10 Pro XL**
- **Cuando** en la tablet añade un nuevo tramo de *Shinkansen*, importa un PDF de hotel y edita el horario del *Día 3*, y ambos dispositivos tienen conexión a internet
- **Entonces** el móvil **Pixel 10 Pro XL** descarga y persiste localmente los cambios y los archivos PDF/fotos, quedando disponibles en el móvil incluso si posteriormente se activa el Modo Avión.

### Escenario 9: Prevención de borrados o modificaciones accidentales y sistema de double check para elementos sensibles
- **Dado** que el usuario pulsa en el botón `🗑️ Eliminar` sobre cualquier transporte, parada de itinerario, ciudad meteorológica, documento o guía
- **Cuando** se despliega el diálogo modal de confirmación estándar
- **Entonces** si pulsa *"Cancelar"*, la acción se aborta de inmediato y el elemento se mantiene inalterado
- **Y cuando** pulsa *"Eliminar"*, se ejecuta el borrado seguro con confirmación visual.
- **Y si la acción es sensible** (eliminar un plan de viaje completo, borrar un día entero del itinerario, quitar fotos de Pasaporte/DNI o vaciar el itinerario)
- **Entonces** el modal activa el sistema de **Double Check**, mostrando advertencia crítica (`🚨`) y requiriendo activar un interruptor obligatorio de confirmación de irreversibilidad para habilitar el botón de borrado definitivo.
- **Y de igual forma**, si modifica un elemento existente y pulsa *"Guardar"*, se solicita confirmación de actualización para evitar sobrescrituras accidentales.

### Escenario 10: Selección de fechas mediante selector interactivo de calendario
- **Dado** que el usuario abre un formulario que requiere fecha (ej. «Crear o Editar Plan de Viaje», «Añadir o Editar Transporte», o «Editar Parada de Itinerario»)
- **Cuando** pulsa sobre el campo de fecha o el icono del calendario `📅`
- **Entonces** se despliega el selector de fecha nativo de Material 3 (`DatePickerDialog`) permitiendo elegir año, mes y día con toques táctiles
- **Y cuando** pulsa *"Aceptar"*, el campo se actualiza con la fecha seleccionada sin requerir tecleo manual de texto.
