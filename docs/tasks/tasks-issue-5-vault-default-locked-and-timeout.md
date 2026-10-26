# Tareas: Bóveda Bloqueada por Defecto y Auto-Bloqueo de Seguridad (Issue #5)

## Contexto del Issue
- **Issue**: [#5 Por defecto muestra los documentos](https://github.com/laralbir/japan-travel-tools/issues/5)
- **Problema**:
  1. La sección de pasaporte y documentos muestra los documentos directamente por defecto sin verificación previa debido a la retención de estado en `rememberSaveable`.
  2. La tarjeta "Identificación Bloqueada" debe aparecer siempre por defecto en cada entrada si la biometría está habilitada y solo mostrar los documentos tras autenticación exitosa.
  3. Ausencia de auto-bloqueo tras un tiempo prudencial de seguridad y al enviar la app a segundo plano.
  4. El botón para bloquear debe tener el texto conciso "Bloquear" y mostrarse siempre que los documentos estén visibles.

---

## Desglose Secuencial de Tareas (SDD + TDD)

- [x] **Tarea 1: Actualización de Especificación y Análisis**
  - Actualizar `docs/specs/03-documents-and-id-vault-spec.md` con las reglas de estado bloqueado por defecto y auto-bloqueo.
  - Actualizar `docs/analysis/architecture-and-tech-stack.md` con la arquitectura efímera y el temporizador.

- [x] **Tarea 2: Test Unitario (RED) del Gestor de Seguridad de Bóveda**
  - Crear `VaultSecurityManagerTest` para validar las reglas de negocio de auto-bloqueo por temporizador (120 segundos) y estado inicial bloqueado.

- [x] **Tarea 3: Implementación (GREEN) del Gestor de Seguridad de Bóveda**
  - Implementar lógica testeable para el control de timeout de sesión y estado inicial de bloqueo.
  - Verificar tests en verde.

- [x] **Tarea 4: Refactorización en `MainScreen.kt` (`PassportAndCustomsScreen`)**
  - Cambiar `rememberSaveable` a `remember { mutableStateOf(!isLockActive) }` para garantizar que el estado nunca persista entre visitas a la pantalla.
  - Asegurar que la tarjeta "Identificación Bloqueada" se muestre de forma predeterminada siempre que `!vaultUnlocked && isLockActive`.
  - Integrar temporizador `LaunchedEffect(vaultUnlocked)` con auto-bloqueo tras 120 segundos de sesión activa.
  - Integrar listener de ciclo de vida con `DisposableEffect` / `LifecycleEventEffect` para bloquear la bóveda automáticamente al recibir `Lifecycle.Event.ON_STOP` o `ON_PAUSE`.
  - Cambiar el texto del botón de bloqueo a "Bloquear" (`🔒 Bloquear` / `🔒 Lock`) visible siempre que los documentos estén desbloqueados.

- [x] **Tarea 5: Verificación en Dispositivo Real**
  - Ejecutar suite de pruebas unitarias (`./gradlew testDebugUnitTest`).
  - Desplegar en Google Pixel 9 Pro XL mediante `./scripts/deploy-authorized-devices.sh`.
  - Verificar manualmente en el teléfono:
    1. Al abrir la app y entrar en Bóveda, se muestra la tarjeta "Identificación Bloqueada".
    2. Tras huella/PIN, se muestran los documentos y el botón "Bloquear".
    3. Al pulsar "Bloquear", se vuelve de inmediato a "Identificación Bloqueada".
    4. Al salir de la app (Home o multitarjeta) y volver, la bóveda está de nuevo bloqueada.
    5. Pasados 120 segundos sin salir, la bóveda se bloquea automáticamente.

- [ ] **Tarea 6: Actualización de Documentación, Commit y Cierre de Issue**
  - Actualizar `CHANGELOG.md` y `README.md`.
  - Merge a `develop` respetando Gitflow.
  - Comentar en Issue #5 y cerrarlo.
