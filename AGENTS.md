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

---

## 3. Versionado Semántico, Regla Beta y Releases (GitHub + APK)

- **Versión inicial**: `0.0.1` (con sufijo `-beta.X` mientras las herramientas estén en desarrollo).
- **Regla de Madurez Beta**:
  > **IMPORTANTE**: Hasta que una herramienta (`tool`) no esté implementada al 100%, sea completamente funcional (incluyendo modo offline y tests al 100%) y haya sido aprobada por el usuario, cualquier versión publicada llevará el sufijo `-beta.X` (ej. `0.0.1-beta.1`) y la herramienta mostrará o tendrá estado Beta.
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
