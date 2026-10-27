# Tareas: Bloqueo Biométrico Obligatorio para Documentos y Retirada del Conmutador

## Contexto y Requerimiento
- **Requisito del Usuario**: El bloqueo biométrico para documentos confidenciales (Pasaporte, DNI y documentos personalizados) es **estrictamente obligatorio**; el conmutador (toggle) ya no hace falta y se retira de la interfaz.
- **Objetivo**:
  1. La verificación biométrica es obligatoria por diseño si el hardware dispone de biometría o PIN.
  2. Retirar el conmutador (Switch) de activación/desactivación de la pantalla de documentos.
  3. Mantener el estado bloqueado obligatorio por defecto (`vaultUnlocked = false` efímero).
  4. Mantener el temporizador de auto-bloqueo tras 120s y el bloqueo en segundo plano (`ON_STOP`/`ON_PAUSE`).
  5. Mantener el botón `🔒 Bloquear` visible siempre que la bóveda esté desbloqueada.

---

## Desglose Secuencial de Tareas (SDD + TDD)

- [x] **Tarea 1: Actualización de Especificación y Análisis**
  - Actualizar `docs/specs/03-documents-and-id-vault-spec.md` consagrando el bloqueo obligatorio y la eliminación del toggle.
  - Actualizar `docs/analysis/architecture-and-tech-stack.md` con la nueva máquina de estados obligatoria.

- [x] **Tarea 2: Test Unitario (RED) en `VaultSecurityPolicyTest`**
  - Adaptar `VaultSecurityPolicyTest` para validar `shouldStartUnlocked(biometricAvailable)` e `isLockMandatory(biometricAvailable)`.

- [x] **Tarea 3: Implementación (GREEN) en `VaultSecurityPolicy`**
  - Actualizar `VaultSecurityPolicy.kt` para reflejar que la obligatoriedad depende de la disponibilidad de biometría/PIN en el hardware.
  - Verificar tests en verde.

- [x] **Tarea 4: Refactorización en `MainScreen.kt` (`PassportAndCustomsScreen`)**
  - Eliminar el componente `Switch` de activación/desactivación biométrica.
  - Reemplazar por tarjeta informativa de seguridad: `🔒 Protección Biométrica Obligatoria Activa` si `biometricAvailable == true`, o aviso de advertencia si el dispositivo no tiene credenciales configuradas.
  - Mantener el botón `🔒 Bloquear` junto con el indicador de auto-bloqueo mientras los documentos estén visibles.

- [x] **Tarea 5: Verificación en Dispositivo Real**
  - Ejecutar suite de pruebas unitarias (`./gradlew testDebugUnitTest`).
  - Desplegar en Google Pixel 9 Pro XL mediante `./scripts/deploy-authorized-devices.sh`.
  - Verificar visualmente y funcionalmente en el teléfono que no hay switch, la verificación es obligatoria y el botón "Bloquear" funciona correctamente.

- [ ] **Tarea 6: Actualización de Documentación, Commit y Cierre**
  - Actualizar `CHANGELOG.md` y `README.md`.
  - Merge a `develop` respetando Gitflow.
