# Backlog y Plan de Tareas (SDD + TDD)

Este documento define la fase de **Tareas** del flujo SDD. Todas las tareas están ordenadas secuencialmente para implementarse aplicando **TDD (Red -> Green -> Refactor)** y **Gitflow**.

> **Regla de versión Beta**: Mientras exista cualquier tarea pendiente o sin aprobación final del usuario en una herramienta, las versiones publicadas llevarán sufijo `-beta.X` (comenzando por `0.0.1-beta.1`) y las herramientas mostrarán su estado `BETA`.

---

## Hito 1: Inicialización SDD y Base del Proyecto (`v0.0.1-beta.1`)

- [x] **TASK-001**: Inicializar repositorio para agentes de IA (`AGENTS.md`, `.gitignore`, `README.md`, `CHANGELOG.md`).
- [x] **TASK-002**: Redactar especificaciones funcionales completas en `/docs/specs/` (`SPEC-00` a `SPEC-03`), análisis técnico en `/docs/analysis/` y guía de usuario en `/docs/user-guide.md`.
- [x] **TASK-003**: Configurar ramas **Gitflow** (`main` y `develop`) con el commit fundacional de especificación.
- [ ] **TASK-004**: Crear esqueleto del proyecto Android (Kotlin + Jetpack Compose + Gradle Version Catalog + Room + DataStore + Ktor/Retrofit + Testing dependencies) en rama `feature/project-scaffolding`.
- [ ] **TASK-005**: Configurar numeración de versión `0.0.1-beta.1` en `app/build.gradle.kts` y verificar compilación de APK (`./gradlew testDebugUnitTest assembleDebug`).

---

## Hito 2: Configuración Core, Idioma, Tema y Persistencia (`SPEC-00`)

- [ ] **TASK-101 (TDD)**: Escribir tests unitarios para `LanguageResolver` (idioma del sistema `es` -> Español, `en` -> Inglés, cualquier otro como `fr`/`ja` -> fallback a Inglés; selección manual prevalece).
- [ ] **TASK-102 (TDD)**: Implementar `UserPreferencesRepository` con Jetpack DataStore y sus tests de persistencia (tema Claro/Oscuro/Sistema, idioma, preferencias de herramientas).
- [ ] **TASK-103 (TDD)**: Implementar recursos de cadenas `res/values/strings.xml` (EN por defecto) y `res/values-es/strings.xml` (ES), junto con `SettingsViewModel` y `SettingsScreen`.
- [ ] **TASK-104 (TDD)**: Implementar navegación principal (Bottom Navigation / Navigation Bar) con indicador visual `BETA` para herramientas en desarrollo.

---

## Hito 3: Herramienta 1 — Relojes y Ubicación (`SPEC-01`)

- [ ] **TASK-201 (TDD)**: Escribir tests unitarios para `CalculateClockComparisonUseCase` (hora local, hora en Japón `Asia/Tokyo`, hora en país de origen, cálculo exacto de diferencia horaria con DST y tramo del día).
- [ ] **TASK-202 (TDD)**: Escribir tests unitarios e implementar `OfflineTimeZoneResolver` (resolución offline de latitud/longitud a `ZoneId` sin requerir internet, con fallback al `ZoneId.systemDefault()`).
- [ ] **TASK-203 (TDD)**: Implementar `WorldClockViewModel` (actualización reactiva del reloj, solicitud opcional de permiso `ACCESS_COARSE_LOCATION` y persistencia del país de origen en `DataStore`).
- [ ] **TASK-204 (TDD)**: Implementar `WorldClockScreen` en Jetpack Compose con selector buscable de país/zona horaria de origen y tests de UI.

---

## Hito 4: Herramienta 2 — Conversor de Divisa Offline-First (`SPEC-02`)

- [ ] **TASK-301 (TDD)**: Escribir tests unitarios para `ConvertCurrencyUseCase` (conversión bidireccional `EUR <-> JPY`, redondeo para yenes sin decimales y divisas con 2 decimales, soporte de tasa manual).
- [ ] **TASK-302 (TDD)**: Implementar `CurrencyRateDao` (Room) y `SeedExchangeRates` (tasas semilla preinstaladas para garantizar funcionamiento offline desde el primer segundo) con sus tests de repositorio.
- [ ] **TASK-303 (TDD)**: Implementar cliente `FrankfurterApiService` (`https://api.frankfurter.dev/v1/latest`) y lógica de sincronización/caché en `OfflineFirstCurrencyRepository` (tests con Mock HTTP).
- [ ] **TASK-304 (TDD)**: Implementar `CurrencyConverterViewModel` y `CurrencyConverterScreen` (inputs bidireccionales, botón `Swap ⇅`, importes rápidos de Japón `¥100` a `¥10,000`, indicador de fecha de caché/modo offline y botón de actualización).

---

## Hito 5: Herramienta 3 — Pasaporte/DNI y Repositorio de Documentos (`SPEC-03`)

- [ ] **TASK-401 (TDD)**: Escribir tests unitarios para `DocumentRepository` e `InternalFileStorageManager` (copia segura de imágenes y PDFs a `Context.filesDir`, borrado limpio y listado por categoría).
- [ ] **TASK-402 (TDD)**: Implementar sección de **Identificación Rápida (Pasaporte y DNI)** con captura de cámara / Photo Picker, metadatos rápidos copiables y visor a pantalla completa con zoom (`pinch-to-zoom`).
- [ ] **TASK-403 (TDD)**: Integrar `BiometricPrompt` opcional para desbloquear la visualización de Pasaporte y DNI según la preferencia persistida del usuario.
- [ ] **TASK-404 (TDD)**: Implementar **Repositorio de Documentos de Viaje** (categorías: Vuelos, Hoteles, Transporte, Seguros, Entradas, Otros), buscador offline y visor PDF integrado (`PdfRenderer`).

---

## Hito 6: Cierre de Beta, Aprobación y Release Estable `v0.0.1`

- [ ] **TASK-501**: Ejecutar batería completa de tests unitarios e instrumentados y validar uso 100% en Modo Avión.
- [ ] **TASK-502**: Revisión y aprobación del usuario de cada herramienta para retirar la etiqueta `BETA`.
- [ ] **TASK-503**: Merge a `main` vía Gitflow (`release/0.0.1`), generación de tag `v0.0.1`, compilación de APK final y publicación en **GitHub Releases**.
