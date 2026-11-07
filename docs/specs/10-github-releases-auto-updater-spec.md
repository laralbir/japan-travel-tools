# SPEC-10: Sistema de Actualizaciones Automáticas vía GitHub Releases y Firma Criptográfica de APKs de Producción

- **ID**: `SPEC-10`
- **Estado**: `Aprobado para Análisis e Implementación`
- **Repositorio Oficial de Distribución**: `laralbir/japan-travel-tools`
- **Referencias Cruzadas**:
  - [`SPEC-00`](00-core-configuration-spec.md) (Configuración Base, Idioma EN/ES, Tema Material 3, Conectividad y Arquitectura Offline-First)
  - [`SPEC-03`](03-documents-and-id-vault-spec.md) (Bóveda Privada en `Context.filesDir` que debe preservarse intacta entre actualizaciones)
  - [`ANALYSIS-12`](../analysis/12-github-releases-auto-updater-analysis.md) (Diseño Técnico Clean Architecture + MVVM y Pipeline CI/CD)
  - [`TASKS-GITHUB-AUTO-UPDATER`](../tasks/tasks-github-releases-auto-updater.md) (Desglose Atómico TDD)

---

## 1. Contexto, Motivación y Objetivo

**Japan Travel Tools** se distribuye de forma directa y autogestionada (*self-hosted*) a través de **GitHub Releases** en el repositorio oficial `laralbir/japan-travel-tools`, sin depender de tiendas intermediarias para publicar correcciones rápidas o mejoras durante la preparación y el transcurso del viaje a Japón.

Hasta ahora, la distribución manual presentaba dos limitaciones críticas para el viajero:
1. **Riesgo de discontinuidad de firma criptográfica (`debug.keystore` efímero)**: Compilar entregas mediante `assembleDebug` o claves locales no estandarizadas genera certificados distintos según la máquina o el runner de integración continua. En Android, intentar instalar un APK firmado con un certificado distinto sobre una instalación existente falla con `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, obligando al usuario a **desinstalar la aplicación** y provocando la **pérdida irreversible de todo el almacenamiento privado (`Context.filesDir`)**: fotografías de Pasaporte/DNI, pases `.pkpass`, reservas PDF, guías Markdown, planes de viaje, recibos Tax-Free y preferencias locales, además de disparar bloqueos de seguridad en *Google Play Protect*.
2. **Ausencia de descubrimiento, verificación e instalación asistida en la propia app**: El usuario debía revisar manualmente la web de GitHub, descargar el archivo `.apk` desde un navegador externo sin comprobar su integridad criptográfica y gestionar archivos residuales en la carpeta pública de descargas.

### Objetivo
Dotar a **Japan Travel Tools** de:
1. Un **estándar inmutable de firma criptográfica de producción (`assembleRelease` con APK Signature Scheme v2 y v3)** tanto en compilaciones locales autorizadas como en GitHub Actions, garantizando continuidad criptográfica perpetua.
2. Un **sistema integrado de actualizaciones OTA (*Over-The-Air*) 100% compatible con la filosofía Offline-First** que permita comprobar nuevas versiones en **GitHub Releases**, consultar sus notas de cambio renderizadas nativamente en Markdown, descargar el `.apk` en segundo plano con progreso real, verificar doblemente su autenticidad (**hash SHA-256 + certificado de firma X.509**) e instalar la actualización *in-place* mediante la API nativa `PackageInstaller` sin perder jamás un solo dato del viajero.

---

## 2. Requisitos Funcionales (RF)

### RF-10.1: Firma Criptográfica Oficial de APKs de Release (Keystore de Producción v2/v3)

1. **Sustitución Definitiva de `assembleDebug` por `assembleRelease` Firmado en Distribución**:
   - Queda prohibido adjuntar compilaciones `debug` (`assembleDebug` firmadas con `debug.keystore`) en las **GitHub Releases** del proyecto.
   - Toda versión publicada en GitHub (tanto versiones estables `vX.Y.Z` como pre-releases `vX.Y.Z-beta.N`) se compilará exclusivamente mediante `./gradlew assembleRelease`, produciendo un APK optimizado y firmado criptográficamente con el **Keystore oficial de producción** en formato estándar **PKCS#12 (`.jks` / `.keystore`, clave RSA de 4096 bits, validez mínima de 25 años)**.
   - Cada GitHub Release adjuntará el binario firmado (`japan-travel-tools-v<version>.apk`) y su archivo de suma de verificación (`japan-travel-tools-v<version>.apk.sha256`), además del metadato `digest` (`sha256:...`) generado automáticamente por GitHub Releases.

2. **Esquemas de Firma Criptográfica Habilitados (APK Signature Scheme v2 y v3)**:
   - La configuración de firma de `release` habilitará explícitamente:
     - **APK Signature Scheme v2 (`enableV2Signing = true`)**: Firma criptográfica de bloque completo del archivo APK (estándar nativo desde Android 7.0 / `minSdk = 26`), garantizando verificación ultrarrápida e impidiendo cualquier alteración de entradas o cabeceras ZIP no cubiertas por el antiguo esquema JAR.
     - **APK Signature Scheme v3 (`enableV3Signing = true`)**: Soporte criptográfico nativo desde Android 9 (`API 28+`) que incorpora estructura de linaje de certificados (*proof-of-rotation*), permitiendo en el futuro rotar la clave criptográfica sin perder identidad si algún día fuera necesario.
     - **Compatibilidad JAR v1 (`enableV1Signing = true`)**: Mantenida únicamente como respaldo secundario para herramientas externas de auditoría de archivos, siendo v2/v3 la autoridad en tiempo de ejecución en Android 8.0+ (`minSdk = 26`).

3. **Gestión Segura de Credenciales (Cero Secretos Commiteados)**:
   - **Prohibición absoluta de secretos en Git**: Ningún archivo `.jks`, `.keystore`, `keystore.properties` ni contraseñas reales o de respaldo inseguras podrán incluirse jamás en el repositorio ni en `app/build.gradle.kts`.
   - **Entorno de Desarrollo Local**:
     - Las credenciales de firma se leen desde el archivo local `keystore.properties` en la raíz del proyecto (ignorado en `.gitignore`), con las propiedades `storeFile`, `storePassword`, `keyAlias` y `keyPassword`.
     - El repositorio incluirá únicamente la plantilla documentada `keystore.example.properties` (con salto de línea final POSIX `\n`) explicando las variables requeridas sin valores sensibles.
   - **Entorno CI/CD (GitHub Actions)**:
     - El workflow automatizado de publicación leerá las credenciales exclusivamente desde **GitHub Repository Secrets**:
       - `SIGNING_KEYSTORE_BASE64`: Contenido binario del Keystore PKCS#12 codificado en Base64.
       - `SIGNING_KEY_ALIAS`: Alias de la clave de producción.
       - `SIGNING_STORE_PASSWORD`: Contraseña del almacén PKCS#12.
       - `SIGNING_KEY_PASSWORD`: Contraseña de la clave privada.
     - El archivo `.jks` decodificado se escribirá únicamente en el directorio efímero del runner (`$RUNNER_TEMP`) durante el paso de compilación y se eliminará obligatoriamente al finalizar el job (`if: always()`).

4. **Continuidad Criptográfica (`SHA-256 Signing Certificate`), Preservación de Datos y Permisos del Sistema**:
   - **Actualizaciones *in-place* sin pérdida de datos**: Al mantener invariable el certificado de firma (`SHA-256`) y crecer monótonamente `versionCode`, Android reemplaza únicamente el código y recursos del APK conservando intactos:
     - Todos los documentos confidenciales en `Context.filesDir` (fotos de Pasaporte y DNI, pases `.pkpass`, PDFs de vuelos/hoteles, guías Markdown, recibos Tax-Free y cachés offline).
     - Todas las preferencias en `DataStore` / `SharedPreferences`, claves criptográficas del Android Keystore y permisos en tiempo de ejecución ya otorgados (Cámara, Micrófono, Ubicación, Notificaciones y Biometría).
   - **Prevención de bloqueos de instalación y reputación en Google Play Protect**:
     - Elimina de raíz los errores `INSTALL_PARSE_FAILED_NO_CERTIFICATES` (APK sin firmar) e `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (conflicto de certificados).
     - Mantiene una identidad criptográfica estable `(applicationId, signingCertificateSha256)` ante **Google Play Protect**, evitando las advertencias severas asociadas a claves de depuración o certificados cambiantes.
   - **Comportamiento preciso de `REQUEST_INSTALL_PACKAGES` y actualizaciones fluidas en Android 12+ (`API 31+`)**:
     - En Android 8.0+ (`API 26+`), el permiso especial `android.permission.REQUEST_INSTALL_PACKAGES` (*"Instalar aplicaciones desconocidas"*) se concede por aplicación fuente (`package:com.laralbir.japantraveltools`). Gracias a la continuidad de firma, el usuario solo necesita autorizar a **Japan Travel Tools** **una única vez** en toda la vida útil de la app; dicha autorización persiste en todas las actualizaciones posteriores.
     - En **Android 12 y versiones superiores (`API 31+`, `targetSdk = 36`)**, la app declarará el permiso de nivel `normal` `<uses-permission android:name="android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION" />` y configurará la sesión del instalador con `PackageInstaller.SessionParams.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)` (junto con `setRequestUpdateOwnership(true)` en Android 14+ / `API 34+`). Cuando el certificado SHA-256 del nuevo APK coincide con el instalado y la propia aplicación figura como instaladora de origen (`installerPackageName`), Android permite completar la actualización de forma directa y fluida sin diálogos redundantes. Si en la primera auto-actualización (cuando la instalación inicial provino de navegador, gestor de archivos o ADB) o por política del fabricante el sistema requiere confirmación interactiva, `PackageInstaller` emite `STATUS_PENDING_USER_ACTION` con `Intent.EXTRA_INTENT`, abriendo limpiamente la hoja de confirmación nativa de un solo toque.

---

### RF-10.2: Comprobación Automática y Manual de Nuevas Versiones en GitHub Releases

1. **Consulta a la API Oficial de GitHub Releases**:
   - **Canal Estable (por defecto)**: Petición `GET https://api.github.com/repos/laralbir/japan-travel-tools/releases/latest` con cabeceras `Accept: application/vnd.github+json` y `X-GitHub-Api-Version: 2022-11-28`. Este endpoint devuelve exclusivamente la versión publicada más reciente marcada como no borrador (`draft == false`) y no preliminar (`prerelease == false`).
   - **Inclusión opcional de versiones preliminares (`-beta.X`)**: En Ajustes, el usuario dispondrá de un interruptor opcional (*"Incluir versiones previas de prueba"* — activado por defecto mientras `BuildConfig.VERSION_NAME` contenga `-beta.`, y desactivado por defecto en versiones estables, pero siempre editable por el usuario). Cuando esté activo, se consultará `GET https://api.github.com/repos/laralbir/japan-travel-tools/releases?per_page=10`, filtrando `draft == false` y seleccionando la versión de mayor precedencia SemVer que disponga de un asset `.apk` válido.
   - **Eficiencia de Red y Cuota (`ETag` / `HTTP 304`)**: La app almacenará la cabecera `ETag` de la última respuesta exitosa y enviará `If-None-Match` en consultas sucesivas; si GitHub responde `304 Not Modified`, se actualiza `lastUpdateCheckEpochMillis` sin consumir ancho de banda ni cuota de peticiones de la API pública.

2. **Comparación Estricta SemVer 2.0.0 (`SemanticVersionComparator`)**:
   - Las etiquetas de Git (`tag_name`, ej. `v1.4.0` o `v1.4.0-beta.2`) y `BuildConfig.VERSION_NAME` se normalizarán eliminando el prefijo inicial `v`/`V` y metadatos de compilación `+...`.
   - El comparador evaluará estrictamente según la especificación **Semantic Versioning 2.0.0**:
     1. Comparación numérica entera de `MAJOR`, después `MINOR` y después `PATCH` (`1.10.0 > 1.2.0`).
     2. **Precedencia de sufijos pre-release**: Para idéntico trío `MAJOR.MINOR.PATCH`, toda versión con sufijo pre-release tiene **menor precedencia** que la versión estable correspondiente (`1.4.0-alpha.1 < 1.4.0-beta.1 < 1.4.0-beta.2 < 1.4.0-beta.10 < 1.4.0`).
     3. Los segmentos numéricos dentro del identificador pre-release (`beta.2` frente a `beta.10`) se comparan como enteros (`2 < 10`), nunca alfabéticamente.
   - Solo se notificará actualización cuando `remoteVersion > installedVersion`.

3. **Comprobación Automática No Bloqueante (Cooldown de 12 h y Política de Red)**:
   - El usuario podrá elegir en Ajustes el modo de comprobación:
     - **`Automática`** (por defecto): comprueba en segundo plano al abrir la app (o reanudar tras inactividad prolongada) **como máximo una vez cada 12 horas (`43.200.000 ms`)** respecto a `lastUpdateCheckEpochMillis`.
     - **`Solo manual`**: desactiva por completo cualquier comprobación automática en segundo plano.
   - **Respeto absoluto a la Política de Red de la App**:
     - **`Modo Offline` activo** (`forceOfflineMode == true` o dispositivo sin conectividad validada): **jamás** se intenta abrir conexión de red.
     - **`Solo Wi-Fi`** (opción por defecto heredada/sincronizada con la política de red de la app): las comprobaciones automáticas se ejecutan únicamente cuando la conexión activa es Wi-Fi no medido (`ActiveConnectionKind.WIFI`), protegiendo los megabytes de la eSIM de viaje japonesa, Pocket Wi-Fi medido o conexiones en Roaming.
     - **`Wi-Fi y Datos móviles`**: permite comprobaciones sobre Wi-Fi o datos móviles (`SIM` / `eSIM`), bloqueando igualmente cualquier intento si se detecta Roaming activo (`isRoaming == true`).
   - **Cero impacto en el arranque**: Toda comprobación se ejecuta en `Dispatchers.IO` después del primer fotograma de la interfaz, con timeouts estrictos (`6 s` conexión / `8 s` lectura) y captura silenciosa de errores de red en modo automático para no interrumpir nunca al viajero.

4. **Panel en Ajustes y Botón Manual «🔄 Buscar actualizaciones»**:
   - Dentro de la pantalla de **Ajustes (`Settings`)**, en la sección de Sistema / Acerca de, se mostrará:
     - **Versión instalada actual** (`versionName` y código interno de compilación `versionCode`).
     - **Huella abreviada del certificado de firma** del paquete instalado (ej. `Cert SHA-256: 4F:8A:...:C1`), verificable de un vistazo.
     - **Fecha y hora de la última comprobación** (`lastUpdateCheckEpochMillis` persistido localmente, formateado según el idioma y zona horaria del dispositivo, o *"Sin comprobaciones recientes"*).
     - **Botón de acción manual «🔄 Buscar actualizaciones»**:
       - Ignora la ventana de enfriamiento de 12 horas y omite temporalmente el filtro `dismissedVersionTag` (de modo que si el usuario omitió por error una versión, pueda recuperarla manualmente en cualquier momento).
       - Si el Modo Offline forzado está activo o no hay conexión, informa de inmediato con un aviso claro sin colgar la interfaz.
       - Si la app está al día, muestra confirmación clara (*"Estás usando la última versión disponible"*).

---

### RF-10.3: Aviso No Intrusivo y Notas de Versión en Markdown

1. **Aviso Discreto sin Bloqueos de Pantalla**:
   - Cuando una comprobación automática detecta una nueva versión no omitida, la aplicación muestra una **tarjeta/banner no intrusivo Material 3** (en la cabecera contextual de Inicio y en Ajustes) en lugar de un diálogo modal bloqueante inesperado, evitando estorbar cuando el viajero abre la app con prisa en un control de pasaportes o torno de estación.
   - Al pulsar en **«Ver novedades»** sobre dicho aviso (o tras pulsar **«🔄 Buscar actualizaciones»** manualmente), se despliega el diálogo/hoja modal Material 3 de actualización (`AppUpdateAvailableDialog`).
   - **Cumplimiento estricto de Prohibición de Etiquetas `BETA` (`AGENTS.md` §3 y `SPEC-00 RF-00.5`)**: Aunque la versión disponible tenga un número como `1.4.0-beta.2`, **nunca se mostrará ninguna insignia, etiqueta o píldora `BETA` en ningún lugar de la UI**.

2. **Ficha Comparativa y Renderizado Nativo `MarkdownText`**:
   - El diálogo/hoja de nueva versión presentará con estética limpia (*Calm Japanese Slate & Washi*):
     - **Versión actual vs. Nueva versión** con tipografía tabular `tnum` (ej. `1.3.0 ➔ 1.4.0`).
     - **Tamaño del archivo `.apk`** formateado en megabytes (`MB`) y **fecha de publicación** en formato regional del usuario.
     - **Notas de la versión (`CHANGELOG`)**: el campo `body` en Markdown devuelto por GitHub Releases se renderizará mediante el motor propio 100% offline **`MarkdownText`**, respetando encabezados, listas con viñetas, negritas, tablas, bloques de código y enlaces seguros, dentro de un contenedor desplazable con altura acotada.

3. **Acciones Disponibles**:
   - **«⬇️ Descargar e instalar»**: Inicia de inmediato la descarga segura y el flujo de verificación/instalación (`RF-10.4`).
   - **«Recordar más tarde»**: Cierra el diálogo/banner durante la sesión actual sin marcar la versión como descartada permanentemente; volverá a ofrecerse en un ciclo posterior.
   - **«Omitir esta versión»**: Persiste `dismissedVersionTag = release.tagName` en almacenamiento local (`DataStore` / `KeyValueStore`). La app no volverá a mostrar avisos automáticos para esa etiqueta exacta, reactivándose automáticamente en cuanto se publique una versión superior en GitHub o si el usuario pulsa manualmente **«🔄 Buscar actualizaciones»**.

---

### RF-10.4: Descarga Segura, Verificación de Integridad (SHA-256 + Certificado) e Instalación con `PackageInstaller`

1. **Descarga Segura en Almacenamiento Privado Temporal con Progreso y Cancelación**:
   - El archivo `.apk` se descargará exclusivamente en el subdirectorio privado de caché de la app: `Context.cacheDir/updates/` (nunca en carpetas públicas accesibles por otras apps), empleando un archivo temporal `.part` que solo se renombra a `.apk` al concluir la descarga completa.
   - Antes de iniciar la transferencia, se comprobará que `cacheDir.usableSpace` disponga de margen suficiente (`>= 2.2 × tamaño del APK`); en caso contrario se informará al usuario sin iniciar descargas condenadas a fallar.
   - Durante la descarga se mostrará una barra de progreso Material 3 en tiempo real con:
     - Porcentaje exacto completado (`0%` a `100%`).
     - Megabytes descargados frente al total con dígitos tabulares (`ej. 11.4 MB / 19.8 MB`).
     - Botón **«Cancelar»** que detiene al instante el flujo de red y elimina el archivo `.part` residual.

2. **Doble Verificación Criptográfica Pre-Instalación (Anti-Corrupción y Anti-MITM)**:
   Antes de entregar cualquier archivo al instalador del sistema, la app ejecutará obligatoriamente dos validaciones secuenciales en local:
   1. **Verificación de Hash SHA-256 del Binario**:
      - Se calcula el hash `SHA-256` real de los bytes del `.apk` descargado.
      - Se compara contra el valor `digest` (`sha256:<64-hex>`) expuesto por la API de GitHub Releases para ese asset (o, como respaldo si no viniera informado, descargando el archivo adjunto `.sha256` de la misma release).
      - Si no existe suma SHA-256 de referencia o los 64 caracteres hexadecimales no coinciden exactamente, la validación falla.
   2. **Verificación de Identidad de Paquete y Certificado de Firma X.509 (`GET_SIGNING_CERTIFICATES`)**:
      - Se inspecciona el APK descargado mediante `PackageManager.getPackageArchiveInfo(..., PackageManager.GET_SIGNING_CERTIFICATES)` (con soporte multi-API desde `minSdk = 26` hasta `targetSdk = 36`).
      - Se comprueba que:
        - `packageName` del APK descargado es exactamente `"com.laralbir.japantraveltools"`.
        - `versionCode` del APK descargado es estrictamente mayor que el `versionCode` instalado (impidiendo ataques de *downgrade*).
        - La huella **SHA-256 del certificado de firma** del APK descargado coincide exactamente con la huella SHA-256 del certificado de firma de la aplicación actualmente instalada en el dispositivo.
   - **Política de Fallo Cerrado (*Fail-Closed*)**: Si falla cualquiera de las dos verificaciones, la aplicación **elimina inmediatamente el archivo `.apk` descargado**, aborta por completo la llamada a `PackageInstaller` y presenta un mensaje de seguridad explicativo (distinguiendo entre descarga corrupta, discrepancia de certificado —por ejemplo, si un desarrollador intenta actualizar una compilación local `debug` con un APK `release`— o posible manipulación de red).

3. **Instalación con `android.content.pm.PackageInstaller` y Gestión Guiada de `canRequestPackageInstalls()`**:
   - **Verificación y Flujo Guiado del Permiso de Instalación**:
     - Tras superar la doble verificación criptográfica, se consulta `PackageManager.canRequestPackageInstalls()`.
     - Si devuelve `false` (primera actualización en el dispositivo), la app muestra un aviso claro y conciso explicando cómo habilitar el interruptor *"Permitir desde esta fuente"* y abre directamente la pantalla exacta del sistema con `Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))`.
     - La app conserva la referencia al APK ya descargado y verificado y escucha el evento `Lifecycle.Event.ON_RESUME`: al regresar el usuario de Ajustes del sistema con el permiso concedido, **revalida el hash SHA-256 local en milisegundos y lanza automáticamente la instalación** sin obligar al usuario a volver a descargar ni pulsar botones repetidos.
   - **Ejecución de Sesión `PackageInstaller`**:
     - Se instancia `PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)` especificando `setAppPackageName(context.packageName)`, `setSize(apkFile.length())` y, en **Android 12+ (`API 31+`)**, `setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)` (más `setRequestUpdateOwnership(true)` en Android 14+ / `API 34+`).
     - Se vuelca el contenido del `.apk` a la sesión con `session.fsync(...)` y se ejecuta `session.commit(intentSender)` hacia un `BroadcastReceiver` interno (`UpdateInstallStatusReceiver`, `android:exported="false"`).
     - Si el sistema operativo emite `PackageInstaller.STATUS_PENDING_USER_ACTION`, el receptor lanza automáticamente el `Intent` de confirmación oficial de Android (`Intent.EXTRA_INTENT`). Si se completa de forma desatendida (`STATUS_SUCCESS`), Android reinicia o actualiza el paquete limpiamente.

4. **Limpieza Automática de Artefactos Temporales Post-Actualización**:
   - En cada arranque de la aplicación, un inicializador ligero en segundo plano comprobará `BuildConfig.VERSION_CODE` frente al último `lastKnownInstalledVersionCode` guardado en preferencias:
     - Si `BuildConfig.VERSION_CODE > lastKnownInstalledVersionCode`, significa que la actualización se ha instalado correctamente: se eliminan todos los archivos `.apk` y `.part` de `Context.cacheDir/updates/`, se limpia cualquier estado de actualización pendiente y se persiste el nuevo `versionCode`.
     - Igualmente, se purgará cualquier archivo temporal huérfano en `Context.cacheDir/updates/` con antigüedad superior a 24 horas.

---

## 3. Reglas Arquitectónicas Transversales

### 3.1 Reglas Offline-First y Ahorro de Datos en Viaje
1. **Independencia total de la red al arrancar**: Ninguna pantalla ni herramienta de **Japan Travel Tools** depende del estado del comprobador de actualizaciones. En Modo Avión o con `Modo Solo Offline` activado, todas las funciones operan al 100% sin demoras ni mensajes de error.
2. **Protección de datos móviles y Roaming**:
   - Con la política por defecto (`Solo Wi-Fi`), el comprobador automático nunca realiza llamadas sobre redes móviles ni puntos de acceso marcados como medidos (`NET_CAPABILITY_NOT_METERED == false`), evitando consumir datos de eSIM o Pocket Wi-Fi sin permiso.
   - En presencia de Roaming móvil (`isRoaming == true`), toda comprobación automática queda estrictamente bloqueada; si el usuario inicia una descarga manual sobre red móvil o medida, se muestra el tamaño exacto en MB antes de transferir el binario.
3. **Persistencia local de estado**: La fecha de última comprobación (`lastUpdateCheckEpochMillis`), la versión omitida (`dismissedVersionTag`), el `ETag` HTTP y las preferencias del canal de actualización se persisten localmente para sobrevivir a reinicios y cambios de conectividad.

### 3.2 Seguridad y Privacidad Local
1. **Transporte cifrado exclusivo**: Solo se admiten URLs bajo esquema `https://` pertenecientes a los dominios oficiales de GitHub (`api.github.com`, `github.com` y `objects.githubusercontent.com` en redirecciones firmadas de assets). Cualquier redirección a esquema `http://` en texto claro es rechazada.
2. **Cero telemetría ni SDKs externos**: La comprobación utiliza directamente la API REST pública de GitHub sin SDKs propietarios, analíticas ni envío de identificadores del dispositivo o del usuario.
3. **Aislamiento del binario**: El `.apk` se aloja únicamente en `Context.cacheDir/updates/` bajo permisos privados del UID de la app, impidiendo que otras aplicaciones instaladas puedan sustituir el archivo entre la verificación criptográfica y la lectura de `PackageInstaller` (*prevención de ataques TOCTOU — Time-of-Check to Time-of-Use*).

### 3.3 Internacionalización (i18n: Inglés y Español) y Diseño Visual Material 3
1. **Soporte completo EN / ES**: Todos los textos de configuración, estados de comprobación, botones de acción, barra de progreso, diálogos de permisos y mensajes de diagnóstico de firma/red estarán externalizados y traducidos íntegramente en **Inglés (`res/values/strings.xml`, idioma base)** y **Español (`res/values-es/strings.xml`)**, respetando en tiempo real el selector de idioma de la app (`SPEC-00 RF-00.1`).
2. **Estética Material Design 3 (Light / Dark / System)**:
   - Integración plena con las paletas *Washi Paper* (Claro) y *Sumi Ink* (Oscuro), tarjetas con borde sutil de `1.dp`, radios de `16.dp`, botones simétricos de `50.dp` y números tabulares `tnum` para versiones, megabytes y porcentajes.
   - **Diseño Adaptativo**: Hoja o diálogo centrado de ancho legible máximo (`560.dp`) en la tablet **TCL NXTPAPER 11 Plus** (11.5" 3:2) y hoja ergonómica alcanzable a una mano en **Google Pixel 10 Pro XL / Pixel 9 Pro XL**.

---

## 4. Diseño de Datos y Preferencias Locales

| Clave de Preferencia (`KeyValueStore` / `DataStore`) | Tipo | Valor por Defecto | Descripción |
| :--- | :--- | :--- | :--- |
| `pref_update_auto_check_enabled` | `Boolean` | `true` | Activa/desactiva la comprobación automática cada 12 horas. |
| `pref_update_network_policy` | `String` | `"WIFI_ONLY"` | Política de red para comprobación automática: `"WIFI_ONLY"` o `"WIFI_AND_CELLULAR"`. |
| `pref_update_include_prereleases` | `Boolean` | Dinámico según canal instalado | Si es `true`, evalúa también releases con `prerelease == true` (`-beta.X`). |
| `pref_update_last_check_millis` | `Long` | `0L` | Marca temporal epoch (ms) de la última comprobación exitosa ante GitHub. |
| `pref_update_dismissed_version_tag` | `String?` | `null` | Etiqueta (`tag_name`, ej. `"v1.4.0"`) omitida explícitamente por el usuario. |
| `pref_update_last_etag` | `String?` | `null` | Cabecera `ETag` de GitHub API para peticiones condicionales `If-None-Match`. |
| `pref_update_cached_release_json` | `String?` | `null` | Última release válida detectada en JSON compacto para mostrar estado sin red. |
| `pref_update_last_installed_version_code` | `Long` | `BuildConfig.VERSION_CODE` | Permite detectar el primer arranque tras una actualización y limpiar `cacheDir/updates/`. |

---

## 5. Criterios de Aceptación (Escenarios Gherkin / BDD)

### Escenario 1: Actualización *in-place* preservando todos los datos locales gracias a continuidad de firma v2/v3
- **Dado** que el usuario tiene instalada la versión `1.3.0` (`versionCode = 36`) firmada con el Keystore oficial de producción y almacena fotos de su Pasaporte, billetes `.pkpass`, planes de viaje y guías en `Context.filesDir`
- **Cuando** se publica en GitHub Releases la versión `1.4.0` (`versionCode = 37`) compilada con `assembleRelease` y firmada con el mismo Keystore de producción (APK Signature Scheme v2 y v3) y el usuario la descarga e instala desde la app
- **Entonces** Android actualiza la aplicación *in-place* sin solicitar desinstalación ni mostrar errores de incompatibilidad de paquete
- **Y** al abrir la versión `1.4.0`, todas las fotos de Pasaporte/DNI, pases `.pkpass`, documentos, planes, guías y preferencias permanecen 100% intactos.

### Escenario 2: Respeto estricto a Modo Offline, política Solo Wi-Fi y ventana de 12 horas en segundo plano
- **Dado** que la comprobación automática de actualizaciones está activada con política `Solo Wi-Fi`
- **Cuando** el viajero abre la aplicación estando en Modo Avión, con el `Modo Solo Offline` activado en la app o conectado únicamente por datos móviles de su eSIM
- **Entonces** la aplicación arranca instantáneamente sin realizar ninguna petición HTTP a GitHub ni mostrar avisos de error.
- **Y cuando** se conecta al Wi-Fi del hotel y se cumple una comprobación exitosa, si cierra y vuelve a abrir la app 2 horas después
- **Entonces** no se realiza una nueva llamada a la API por no haberse cumplido el intervalo mínimo de 12 horas.

### Escenario 3: Comprobación manual en Ajustes y visualización de notas en Markdown (sin etiquetas `BETA`)
- **Dado** que el usuario entra en `Ajustes > Sistema / Acerca de` con conexión a internet disponible
- **Cuando** visualiza la tarjeta de versión (con `versionName`, última fecha de comprobación y huella corta SHA-256 del certificado) y pulsa **«🔄 Buscar actualizaciones»** existiendo en GitHub una versión superior (ej. `v1.4.0` o `v1.4.0-beta.1` con pre-releases habilitadas)
- **Entonces** se despliega la ficha Material 3 mostrando la comparativa de versiones, tamaño del `.apk`, fecha de publicación y las notas del `CHANGELOG` renderizadas enriquecidamente con `MarkdownText`
- **Y** en ningún punto de la interfaz aparece la etiqueta o insignia `BETA`.

### Escenario 4: Omitir una versión concreta frente a publicación de una versión ulterior
- **Dado** que aparece el aviso de actualización para la versión `v1.4.0`
- **Cuando** el usuario pulsa **«Omitir esta versión»**
- **Entonces** se guarda `dismissedVersionTag = "v1.4.0"` y las siguientes comprobaciones automáticas no vuelven a mostrar el aviso mientras `v1.4.0` siga siendo la última versión.
- **Pero cuando** posteriormente se publica en GitHub la versión `v1.4.1` (o el usuario pulsa manualmente **«🔄 Buscar actualizaciones»**)
- **Entonces** el sistema vuelve a presentar la nueva actualización disponible.

### Escenario 5: Descarga con progreso, doble verificación exitosa, concesión guiada de permiso e instalación fluida
- **Dado** que el usuario pulsa **«⬇️ Descargar e instalar»** sobre la versión `v1.4.0`
- **Cuando** se descarga el archivo `.apk` en `Context.cacheDir/updates/` mostrando porcentaje y megabytes en tiempo real
- **Entonces** al llegar al `100%`, la app verifica que el hash `SHA-256` del archivo coincide con el `digest` oficial de GitHub y que el `packageName` y certificado de firma X.509 coinciden con la app instalada.
- **Y si** `canRequestPackageInstalls()` es `false` (primera vez), la app guía al usuario a `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` y, al volver (`ON_RESUME`) con el permiso activo, reanuda automáticamente la sesión de `PackageInstaller` (`USER_ACTION_NOT_REQUIRED` en Android 12+) sin volver a descargar el APK.

### Escenario 6: Bloqueo de seguridad ante manipulación del APK o discrepancia de certificado de firma
- **Dado** que finaliza la descarga de un archivo `.apk` en `Context.cacheDir/updates/`
- **Cuando** el hash `SHA-256` calculado difiere del `digest` oficial de la release **o** el certificado X.509 interno del `.apk` no coincide con el certificado de la aplicación instalada
- **Entonces** el sistema elimina inmediatamente el archivo `.apk` de la caché privada, bloquea cualquier invocación a `PackageInstaller` y muestra una alerta clara de verificación de seguridad fallida.

### Escenario 7: Limpieza automática del APK temporal tras actualizar
- **Dado** que el archivo `update-1.4.0.apk` se utilizó en `Context.cacheDir/updates/` para actualizar la aplicación de `versionCode = 36` a `versionCode = 37`
- **Cuando** la aplicación se inicia por primera vez en la versión `versionCode = 37`
- **Entonces** el inicializador detecta el incremento de `versionCode`, borra automáticamente todos los archivos residuales en `Context.cacheDir/updates/` y actualiza `lastKnownInstalledVersionCode = 37`.
