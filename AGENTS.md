# AGENTS.md — Guía Operativa para Agentes de IA

Este repositorio está diseñado y estructurado para ser desarrollado por **Agentes de Inteligencia Artificial** en colaboración con desarrolladores humanos, siguiendo estrictamente **Spec-Driven Development (SDD)** y **Test-Driven Development (TDD)** sobre **Android (Kotlin + Jetpack Compose)**.

---

## 1. Flujo de Trabajo Obligatorio (Ciclo SDD + TDD)

Ningún agente debe escribir código de producción sin haber completado las fases previas del ciclo:

1. **Especificación (`docs/specs/`)**:
   - Definir o actualizar los requisitos funcionales, casos de uso, criterios de aceptación, reglas offline y diseño de datos en `/docs/specs/`.
2. **Análisis (`docs/analysis/`)**:
   - Diseñar la arquitectura técnica (Clean Architecture + MVVM/MVI), contratos de interfaces/repositorios, esquemas de base de datos (Room/DataStore) y estrategia de testing en `/docs/analysis/`.
3. **Tareas (`docs/tasks/`)**:
   - Desglosar el análisis en tareas atómicas, secuenciales y verificables en `/docs/tasks/`, indicando qué tests unitarios/instrumentados se crearán en cada paso.
4. **Implementación (TDD estricto: Red -> Green -> Refactor)**:
   - **RED**: Escribir primero el test que falla (Unit Test en `src/test` o UI/Integration Test en `src/androidTest`).
   - **GREEN**: Escribir el código mínimo necesario en Kotlin para que el test pase.
   - **REFACTOR**: Optimizar y limpiar el código manteniendo todos los tests en verde.

---

## 2. Reglas de Gitflow y Commits

### Ramas (Gitflow)
- `main`: Rama de producción/releases. **Nunca hacer commit directo en `main`**. Solo recibe merges desde `release/*` o `hotfix/*`.
- `develop`: Rama principal de integración de desarrollo.
- `feature/<nombre-corto>`: Ramas para nuevas funcionalidades o especificaciones (nacen de `develop` y vuelven a `develop`).
- `release/<version>`: Preparación de una nueva versión hacia `main` y `develop`.
- `hotfix/<nombre-corto>`: Correcciones urgentes sobre `main`.

### Conventional Commits (v1.0.0)
Todos los mensajes de commit deben seguir el estándar [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/):

```text
<tipo>(<alcance opcional>): <descripción corta en minúsculas>

[cuerpo opcional]

[pie opcional: BREAKING CHANGE o referencias a issues]
```

**Tipos permitidos:**
- `feat`: Nueva funcionalidad o herramienta.
- `fix`: Corrección de errores.
- `docs`: Cambios exclusivos en documentación (`/docs`, `README.md`, `CHANGELOG.md`, `AGENTS.md`).
- `test`: Adición o corrección de tests (TDD).
- `refactor`: Cambio de código que no corrige errores ni añade funcionalidades.
- `chore`: Configuración de build, Gradle, dependencias o CI/CD.
- `style`: Formato de código, linting.

### Formato de Archivos y Regla POSIX EOF Newline
- **Salto de línea final obligatorio (POSIX EOF newline)**: Todo archivo de código fuente (`.kt`, `.kts`), recursos (`.xml`), scripts (`.sh`, `.py`), configuración (`.properties`, `.conf`, `.toml`) y documentación (`.md`, `.json`) **debe finalizar obligatoriamente con un salto de línea estándar (`\n`)**.
- Queda prohibido dejar archivos sin salto de línea final (`No newline at end of file`) para evitar diffs espurios de formato en Git al ser abiertos o guardados por editores de código e IDEs.
- El proyecto estandariza esta regla mediante `.editorconfig` en la raíz con `insert_final_newline = true`, `end_of_line = lf` y `trim_trailing_whitespace = true`.

---

## 3. Versionado Semántico, Regla Beta y Releases (GitHub + APK)

- **Versión inicial**: `0.0.1` (con sufijo `-beta.X` en las releases de Git mientras las herramientas estén en desarrollo).
- **Regla de Madurez Beta y Prohibición de Etiquetas `BETA` en UI**:
  > **IMPORTANTE**: Hasta que una herramienta (`tool`) no esté implementada al 100%, sea completamente funcional (incluyendo modo offline y tests al 100%) y haya sido aprobada por el usuario, cualquier versión publicada en Git/GitHub llevará el sufijo `-beta.X` (ej. `0.0.1-beta.4`). Sin embargo, **NUNCA se debe mostrar la etiqueta o insignia `BETA` en ningún sitio de la interfaz de usuario (UI)**, ya que no aporta valor al viajero y ocupa espacio valioso en la pantalla.
- **Publicación en `main`**:
  - Cada vez que se publique una versión en `main`, se debe:
    1. Actualizar `CHANGELOG.md` y `README.md`.
    2. Crear el tag anotado de Git correspondiente (ej. `v0.0.1-beta.1` o `v0.0.1`).
    3. Compilar un **APK funcional** (`./gradlew assembleDebug` o `assembleRelease` firmado).
    4. Publicar una **GitHub Release** (vía `gh release create` o GitHub Actions) adjuntando el archivo `.apk` compilado y las notas del `CHANGELOG.md`.

---

## 4. Principios Arquitectónicos del Proyecto

1. **100% Offline-First**:
   - Toda herramienta debe funcionar sin conexión a internet.
   - Los datos externos (como tipos de cambio de **Frankfurter API**) deben persistirse localmente (Room / DataStore) junto con su marca de tiempo (`lastUpdated`), mostrando siempre el último valor cacheado cuando no haya red e incluyendo valores semilla (`seed`) por defecto en la instalación inicial.
2. **Multiidioma (i18n)**:
   - Idiomas soportados: **Inglés (`values/`, por defecto)** y **Español (`values-es/`)**.
   - Por defecto se adopta el idioma del sistema operativo (con fallback a inglés), pero el usuario puede forzar el idioma desde la configuración de la app (AppCompatDelegate Per-App Language APIs + persistencia en DataStore).
3. **Tema Visual (Light / Dark / System)**:
   - Soporte completo Material Design 3 con tema Claro, Oscuro y Predeterminado del Sistema (opción por defecto).
   - Persistido reactivamente en Jetpack DataStore.
4. **Privacidad y Seguridad Local**:
   - Las fotos de **Pasaporte y DNI** y el **Repositorio de Documentos** (billetes de vuelo, reservas de hotel, etc.) se almacenan en el almacenamiento interno privado de la app (`Context.filesDir`), nunca expuestos en la galería pública del dispositivo.
   - Soporte de bloqueo biométrico / PIN del dispositivo (`BiometricPrompt`) configurable para acceder a la sección de Identificación (Pasaporte/DNI).

---

## 5. Mantenimiento de Documentación

- **Idioma de la documentación**: Español.
- Cada cambio en requisitos, arquitectura, tareas o funcionalidades **obliga** al agente a actualizar en el mismo conjunto de cambios:
  - [`README.md`](README.md)
  - [`CHANGELOG.md`](CHANGELOG.md)
  - Documentación técnica y guía de usuario en [`/docs/`](docs/)

---

## 6. Gestión, Triaje y Respuesta de GitHub Issues

Durante todo el ciclo de desarrollo (al inicio de cada sesión, antes de planificar una nueva iteración y antes de preparar una release), el agente **debe revisar activamente el apartado de Issues de GitHub** (`gh issue list --state open` / `gh issue view <id>`):

1. **Revisión y Análisis de Procedencia (Triaje)**:
   - Analizar cada issue abierto contrastándolo con las especificaciones en `/docs/specs/`, la arquitectura offline-first y el estado actual del código.
   - Determinar **si procede o no** reparar/implementar lo indicado en el issue:
     - **Si procede (Bug válido o Mejora alineada)**:
       1. Responder en el issue (`gh issue comment <id>`) confirmando la recepción, explicando la causa raíz o el plan de acción SDD/TDD y la rama donde se abordará.
       2. Si añade o cambia comportamiento, actualizar primero Especificación -> Análisis -> Tareas.
       3. Crear la rama correspondiente (`fix/issue-<id>-<descripcion>` o `feature/issue-<id>-<descripcion>`), escribir el test que reproduce el fallo (`RED`), repararlo (`GREEN`), refactorizar (`REFACTOR`) y referenciar el issue en el commit (`fix(<scope>): ... (#<id>)` / `Closes #<id>`).
       4. Al fusionar la solución, responder en el issue indicando el commit/versión que lo resuelve y cerrar el issue (`gh issue close <id>`).
     - **Si NO procede (Duplicado, no reproducible, por diseño o fuera de alcance)**:
       1. Responder educada y detalladamente en el issue (`gh issue comment <id>`) justificando técnicamente por qué no procede o pidiendo información adicional si no es reproducible.
       2. Etiquetar/cerrar el issue según corresponda (`wontfix`, `invalid`, `duplicate`) tras dejar constancia escrita de la respuesta.

---

## 7. Despliegue y Debug por ADB en Dispositivos Autorizados (Sin Commitear)

Para probar el desarrollo en dispositivos físicos reales (ej. **Google Pixel 10 Pro XL**, **Google Pixel 9 Pro XL** y **Tablet TCL NXTPAPER 11 Plus**) mediante **ADB / Wireless Debugging**:

1. **Archivo de configuración local ignorado por Git (`authorized-devices.local.conf`)**:
   - **PROHIBIDO commitear** números de serie reales, direcciones IP de depuración inalámbrica o identificadores privados de dispositivos.
   - El archivo `authorized-devices.local.conf` está incluido en `.gitignore`.
   - En el repositorio solo se versiona la plantilla de ejemplo [`authorized-devices.example.conf`](authorized-devices.example.conf).
2. **Instalación y Lanzamiento Seguro**:
   - Utilizar la tarea de Gradle `./gradlew installAuthorizedDebug` o el script `./scripts/deploy-authorized-devices.sh`.
   - Este flujo verifica qué dispositivos están conectados por ADB (`adb devices`) e instala/ejecuta el APK de Debug **exclusivamente** en aquellos cuyo serial o `IP:puerto` esté listado en `authorized-devices.local.conf`.
