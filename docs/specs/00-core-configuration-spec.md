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
