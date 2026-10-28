# Tareas: Corrección de Solapamiento del Teclado Virtual sobre Campos de Entrada (Issue #7)

- **Issue**: [#7](https://github.com/laralbir/japan-travel-tools/issues/7)
- **Rama**: `fix/issue-7-keyboard-ime-overlap`

---

## 1. Tareas de Implementación

- [x] **TASK-701 (Análisis)**: Identificar todos los contenedores de pantallas (`Scaffold`, `Box`, `ModalBottomSheet`) donde el teclado virtual (`IME`) tapa los campos con foco al no aplicar `Modifier.imePadding()` en modo Edge-to-Edge.
- [x] **TASK-702 (Implementación)**:
  - Aplicar `Modifier.imePadding()` en el contenedor principal de `ActiveToolContent` (tanto en modo Smartphone como Tablet).
  - Aplicar `Modifier.navigationBarsPadding().imePadding()` y `verticalScroll()` en todos los `ModalBottomSheet` con formularios de edición (`ItineraryStopEditorModalSheet`, `TransportSegmentEditorModalSheet`, `DocumentEditorModalSheet`, `GuideEditorModalSheet`, `CityWeatherEditorModalSheet`, `CustomIdDocumentEditorModalSheet`, `JsonPortabilityModalSheet`).
- [x] **TASK-703 (Verificación y Build)**:
  - Ejecutar `./gradlew testDebugUnitTest`.
  - Compilar con `./gradlew assembleDebug`.
  - Verificar que todos los archivos cumplan la regla POSIX EOF newline.
- [x] **TASK-704 (Merge y Cierre)**:
  - Fusionar en `develop` y cerrar el Issue #7.
