# Tareas: Reparación de Toggle Biométrico y Prevención de Bypass (Issue #4)

## Contexto del Issue
- **Issue**: [#4 Toggle de documetos se salta biometria](https://github.com/laralbir/japan-travel-tools/issues/4)
- **Problema**: 
  1. Al abrir la bóveda y autenticarse con biometría, el switch aparece apagado (`checked = false`) pese a haberse exigido autenticación.
  2. Si el usuario conmuta el switch, cancela el prompt biométrico y vuelve a conmutar, los documentos se hacen visibles sin haber superado la validación biométrica (bypass de seguridad).

---

## Desglose Secuencial de Tareas (SDD + TDD)

- [x] **Tarea 1: Actualización de Especificación y Análisis**
  - Actualizar `docs/specs/03-documents-and-id-vault-spec.md` con las reglas de valor por defecto seguro y anti-bypass.
  - Actualizar `docs/analysis/architecture-and-tech-stack.md` con la máquina de estados segura.

- [x] **Tarea 2: Test Unitario (RED) en `UserPreferencesRepositoryTest`**
  - Escribir test que verifique que el valor predeterminado de `biometricLockEnabled` es `true`.
  - Escribir test que verifique la persistencia de cambios de valor (`true` -> `false` -> `true`).

- [x] **Tarea 3: Implementación (GREEN) en `UserPreferencesRepository`**
  - Modificar valor por defecto de `biometricLockEnabled = true` en `UserPreferences`.
  - Modificar lectura por defecto de `store.getBoolean(KEY_BIOMETRIC, true)` en `DefaultUserPreferencesRepository`.
  - Asegurar que los tests unitarios pasen en verde.

- [x] **Tarea 4: Lógica Segura y Anti-Bypass en `PassportAndCustomsScreen`**
  - Inicializar `vaultUnlocked` como `!(prefs.biometricLockEnabled && biometricAvailable)`.
  - Actualizar `LaunchedEffect` para que solo solicite biometría si `isLockActive && !vaultUnlocked`.
  - Implementar callbacks seguros en `launchRealBiometricPrompt(onSuccess: () -> Unit)` para desacoplar la validación de la mutación de estado.
  - Asegurar que cambiar el switch a `false` mientras la bóveda esté bloqueada requiera autenticación previa obligatoria.
  - Asegurar que cancelar el prompt nunca desbloquee la bóveda ni altere la preferencia si no hubo éxito.
  - Añadir botón de acción rápida para re-bloquear la bóveda manualmente cuando esté desbloqueada.

- [x] **Tarea 5: Verificación de Tests y Despliegue en Dispositivo Físico**
  - Ejecutar suite de pruebas unitarias (`./gradlew testDebugUnitTest`).
  - Desplegar en Google Pixel 9 Pro XL mediante `./scripts/deploy-authorized-devices.sh`.
  - Verificar manualmente en el teléfono:
    1. Al abrir Bóveda, solicita biometría -> tras huella/PIN, documentos visibles y switch encendido.
    2. Al conmutar el switch a apagado con la bóveda bloqueada -> pide biometría -> al cancelar, el switch sigue encendido y la bóveda bloqueada.
    3. Ninguna combinación de cancelaciones o toques salta la biometría.

- [ ] **Tarea 6: Actualización de Documentación, Commit y Cierre de Issue**
  - Actualizar `CHANGELOG.md` y `README.md`.
  - Merge a `develop` respetando Gitflow.
  - Comentar en Issue #4 y cerrarlo.
