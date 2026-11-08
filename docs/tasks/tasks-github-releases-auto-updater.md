# Tareas: Sistema de Actualizaciones Automáticas vía GitHub Releases y Firma Criptográfica de Producción

- **ID**: `TASKS-GITHUB-AUTO-UPDATER`
- **Especificación**: [`docs/specs/10-github-releases-auto-updater-spec.md`](../specs/10-github-releases-auto-updater-spec.md) (`SPEC-10`)
- **Análisis Técnico**: [`docs/analysis/12-github-releases-auto-updater-analysis.md`](../analysis/12-github-releases-auto-updater-analysis.md) (`ANALYSIS-12`)
- **Ciclo TDD Obligatorio**: `RED` (Test que falla) ➔ `GREEN` (Código mínimo para pasar) ➔ `REFACTOR` (Limpieza y verificación integral)

---

## Fase 1: Firma Criptográfica Oficial de APKs de Producción y CI/CD (`RF-10.1`)

- [x] `T-12.1` **[CHORE / BUILD]**: Actualizar el bloque `signingConfigs.create("release")` en `app/build.gradle.kts` eliminando contraseñas por defecto inseguras y el fallback silencioso a `debug.keystore`, leyendo credenciales exclusivamente desde `keystore.properties` (local, ignorado por Git) o variables de entorno de CI (`SIGNING_KEYSTORE_PATH`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`), habilitando explícitamente `enableV1Signing = true`, `enableV2Signing = true` y `enableV3Signing = true`, y fallando rápido (`GradleException`) si una tarea `*Release` en CI carece de credenciales.
- [x] `T-12.2` **[CHORE / CONFIG]**: Crear la plantilla versionada `keystore.example.properties` en la raíz del proyecto documentando las propiedades requeridas y el comando `keytool` PKCS#12 (RSA 4096 bits), verificando que `.gitignore` mantiene excluidos `keystore.properties`, `*.jks` y `*.keystore`.
- [x] `T-12.3` **[CHORE / CI-CD]**: Crear el workflow `.github/workflows/release-apk.yml` disparado en tags `v*` (y `workflow_dispatch`) que ejecute `./gradlew testDebugUnitTest`, decodifique `SIGNING_KEYSTORE_BASE64` en `$RUNNER_TEMP`, compile `./gradlew assembleRelease`, valide esquemas v2 y v3 con `apksigner verify --verbose --print-certs`, genere el archivo `.apk.sha256`, extraiga las notas de `CHANGELOG.md`, publique la GitHub Release marcando automáticamente `prerelease` para sufijos `-beta.X` y elimine el keystore temporal (`if: always()`).

---

## Fase 2: Comparador SemVer 2.0.0 y Parser de GitHub Releases (`RF-10.2`)

- [x] `T-12.4` **[RED]**: Crear `app/src/test/kotlin/com/laralbir/japantraveltools/feature/updater/SemanticVersionComparatorTest.kt` con pruebas unitarias que fallen inicialmente cubriendo comparación numérica `MAJOR.MINOR.PATCH` (`1.10.0 > 1.2.9`), normalización de prefijo `v`/`V`, precedencia estricta SemVer 2.0.0 de pre-releases (`0.0.1-beta.1 < 0.0.1-beta.2 < 0.0.1-beta.10 < 0.0.1` y `1.3.0 < 1.4.0-beta.1 < 1.4.0`), ignorado de `+build` y entradas malformadas.
- [x] `T-12.5` **[GREEN]**: Implementar `SemanticVersion` y `SemanticVersionComparator` en `app/src/main/kotlin/com/laralbir/japantraveltools/feature/updater/domain/SemanticVersionComparator.kt` hasta poner `SemanticVersionComparatorTest` al 100% en verde.
- [x] `T-12.6` **[RED]**: Crear `app/src/test/kotlin/com/laralbir/japantraveltools/feature/updater/GitHubReleaseDtoParserTest.kt` validando el parseo de respuestas JSON de `/releases/latest` y `/releases`, extracción de `tag_name`, `body`, asset `.apk`, limpieza del prefijo `"sha256:"` en `asset.digest`, fallback al asset `.sha256` cuando `digest` sea `null`, parseo de líneas `<hash>  <archivo>` y filtrado de `draft == true` y `prerelease` según configuración.
- [x] `T-12.7` **[GREEN]**: Implementar `GitHubAssetDto`, `GitHubReleaseDto`, `GitHubReleaseJsonParser` y `AppUpdateInfo` en `feature/updater/data/GitHubReleaseDto.kt` y `feature/updater/domain/AppUpdateModels.kt` logrando pasar `GitHubReleaseDtoParserTest`.

---

## Fase 3: Preferencias Locales y Repositorio Offline-First con Política de Red y Cooldown 12 h (`RF-10.2`, `RF-10.3`)

- [x] `T-12.8` **[RED]**: Crear `app/src/test/kotlin/com/laralbir/japantraveltools/feature/updater/GitHubAutoUpdateRepositoryTest.kt` (usando `InMemoryKeyValueStore` y un `FakeGitHubReleasesApiClient`) verificando:
  1. Cero llamadas de red en modo automático si `autoCheckEnabled == false`, `forceOfflineMode == true`, sin conexión, `isRoaming == true`, red móvil bajo política `WIFI_ONLY` o si han pasado `< 12 horas` desde `lastUpdateCheckEpochMillis`.
  2. Soporte de `ETag` (`HTTP 304 Not Modified`) actualizando `lastUpdateCheckEpochMillis`.
  3. Omisión automática de notificaciones cuando `release.tagName == dismissedVersionTag`, reactivación automática ante una versión superior (`v1.4.1 > v1.4.0`) y bypass tanto del cooldown como de `dismissedVersionTag` al ejecutar búsqueda manual (`manual = true`).
- [x] `T-12.9` **[GREEN]**: Implementar `UpdatePreferencesDataStore` (`feature/updater/data/UpdatePreferencesDataStore.kt`), `GitHubReleasesApiClient` (`feature/updater/data/GitHubReleasesApiClient.kt`) y la lógica de comprobación en `GitHubAutoUpdateRepository` (`feature/updater/data/GitHubAutoUpdateRepository.kt`) poniendo `GitHubAutoUpdateRepositoryTest` en verde.

---

## Fase 4: Doble Verificación Criptográfica (SHA-256 + Certificado X.509) y Descarga Cancelable (`RF-10.4`)

- [x] `T-12.10` **[RED]**: Crear `app/src/test/kotlin/com/laralbir/japantraveltools/feature/updater/ApkSignatureVerifierTest.kt` verificando sobre archivos temporales:
  1. Cálculo exacto de `SHA-256` y fallo `ChecksumMismatch` al alterar un solo byte o carecer de hash esperado.
  2. Fallo `PackageNameMismatch` si el APK archivado no pertenece a `"com.laralbir.japantraveltools"`.
  3. Fallo `VersionDowngradeBlocked` si `archiveVersionCode <= installedVersionCode`.
  4. Fallo `SignatureMismatch` si el certificado X.509 del APK descargado difiere del instalado (anti-MITM / cruce `debug` vs `release`).
  5. Resultado `Verified` cuando ambas barreras coinciden y formateo de huella corta `AB:CD:...:EF`.
- [x] `T-12.11` **[GREEN]**: Implementar `PackageSignatureInspector` (con soporte API 26–27 `GET_SIGNATURES`, API 28–32 `GET_SIGNING_CERTIFICATES` y API 33+ `PackageInfoFlags`) y `ApkSignatureVerifier` en `feature/updater/security/ApkSignatureVerifier.kt` hasta pasar `ApkSignatureVerifierTest`.
- [x] `T-12.12` **[RED]**: Ampliar `GitHubAutoUpdateRepositoryTest.kt` con pruebas de descarga streaming a `cacheDir/updates/*.part`, emisión progresiva `(downloadedBytes, totalBytes)`, validación de dominios HTTPS permitidos (`github.com` / `objects.githubusercontent.com`), borrado inmediato del archivo ante cancelación o fallo de `ApkSignatureVerifier`, y autolimpieza en arranque cuando `currentVersionCode > lastKnownInstalledVersionCode`.
- [x] `T-12.13` **[GREEN]**: Completar `downloadAndVerifyUpdateApk(...)` y `cleanupStaleUpdateFiles(...)` en `GitHubAutoUpdateRepository.kt` logrando pasar todos los casos de prueba.

---

## Fase 5: Ejecutor `PackageInstaller`, Permiso `REQUEST_INSTALL_PACKAGES` y Manifiesto (`RF-10.4`)

- [x] `T-12.14` **[RED]**: Crear `app/src/test/kotlin/com/laralbir/japantraveltools/feature/updater/AppUpdateViewModelTest.kt` probando la máquina de estados `UpdateUiState`: búsqueda manual/automática, acción *"Recordar más tarde"*, acción *"Omitir esta versión"*, progreso y cancelación de descarga, transición a `AwaitingInstallPermission` cuando `canInstallPackages() == false` y reanudación automática de la instalación en `onHostResume()` tras conceder el permiso y revalidar el `SHA-256`.
- [x] `T-12.15` **[GREEN]**:
  1. Declarar `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`, `<uses-permission android:name="android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION" />` y `<receiver android:name=".feature.updater.platform.UpdateInstallStatusReceiver" android:exported="false" />` en `app/src/main/AndroidManifest.xml`.
  2. Implementar `PackageInstallerUpdateExecutor` (`SessionParams.MODE_FULL_INSTALL`, `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` en API 31+, `setRequestUpdateOwnership(true)` en API 34+, `PendingIntent` explícito) y `UpdateInstallStatusReceiver` en `feature/updater/platform/PackageInstallerUpdateExecutor.kt`.
  3. Implementar `AppUpdateViewModel` en `feature/updater/ui/AppUpdateViewModel.kt` hasta poner `AppUpdateViewModelTest` al 100% en verde.

---

## Fase 6: Interfaz Material 3 (`MarkdownText`), Ajustes e Internacionalización EN/ES (`RF-10.2`, `RF-10.3`, `RF-10.4`)

- [x] `T-12.16` **[GREEN / i18n]**: Añadir todas las cadenas localizadas del actualizador en **Inglés (`app/src/main/res/values/strings.xml`)** y **Español (`app/src/main/res/values-es/strings.xml`)**, verificando paridad total de claves y ausencia absoluta de etiquetas `BETA`.
- [x] `T-12.17` **[GREEN / UI]**: Implementar en `feature/updater/ui/AppUpdateComponents.kt`:
  1. `AppUpdateBanner`: aviso compacto no intrusivo Material 3 para la pantalla principal cuando exista actualización pendiente.
  2. `AppUpdateAvailableDialog`: hoja/diálogo adaptativo (Pixel 9/10 Pro XL y TCL NXTPAPER 11 Plus) con versión actual vs. nueva (`tnum`), tamaño en MB, fecha regional, notas del `CHANGELOG` renderizadas con **`MarkdownText`**, barra de progreso con cancelación, diálogo guiado de permiso `ACTION_MANAGE_UNKNOWN_APP_SOURCES` (con observador `ON_RESUME`) y botones **«⬇️ Descargar e instalar»**, **«Recordar más tarde»** y **«Omitir esta versión»** (sin mostrar jamás insignias `BETA`).
- [x] `T-12.18` **[GREEN / UI INTEGRATION]**: Integrar `SettingsUpdateSection` en la pantalla de Ajustes / Acerca de (`MainScreen.kt`) mostrando `versionName`, huella corta `SHA-256` del certificado instalado, fecha de última comprobación (`lastUpdateCheckEpochMillis`), selectores de modo automático/red/pre-releases y botón **«🔄 Buscar actualizaciones»**, y conectar en `MainActivity` / `MainScreen` la autolimpieza post-actualización y el disparo asíncrono no bloqueante cada 12 horas.

---

## Fase 7: Refactorización, Verificación Integral y Sincronización Documental (`REFACTOR`)

- [x] `T-12.19` **[REFACTOR / TEST]**: Ejecutar `./gradlew testDebugUnitTest` y `./gradlew assembleDebug` comprobando que el 100% de la suite unitaria y la compilación pasan sin errores ni advertencias, revisando además que todos los archivos modificados cumplen la regla POSIX EOF newline (`\n`).
- [x] `T-12.20` **[DOCS]**: Al cerrar la implementación funcional, actualizar `README.md`, `CHANGELOG.md`, `docs/index.md` y `docs/user-guide.md` documentando el proceso de firma de producción y el uso del actualizador integrado desde Ajustes.
