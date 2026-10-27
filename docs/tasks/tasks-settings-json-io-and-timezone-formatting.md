# Tareas: Portabilidad JSON en Ajustes (Local/Drive), Formateo Regional de Fechas/Horas con Zonas Horarias (ISO UTC) y Rediseño de Botones en Tarjetas

- **Rama**: `feature/settings-json-io-timezone-card-ux`
- **Especificaciones**: `SPEC-00 (RF-00.4, RF-00.5, RF-00.6)`, `SPEC-03 (RF-03.5)`, `SPEC-04 (RF-04.5)`, `SPEC-05`
- **Análisis**: `docs/analysis/architecture-and-tech-stack.md` (Secciones 3.10 y 3.11)

---

## 1. Tareas de Implementación (TDD Estricto)

### Fase 1: Formateo Regional de Fechas, Horas y Zonas Horarias (ISO UTC)
- [ ] **TASK-601 (TDD - RED)**: Crear tests unitarios en `TravelDateTimeFormatterTest` para:
  - Formateo de fecha completa localizada (`formatFullLocalDate`) con `FormatStyle.FULL` y capitalización de primera letra.
  - Formateo de hora con zona horaria local (`formatLocalTimeWithZone`) con desfase UTC (`UTC+1`, `UTC+9`), nombre de zona (`CET`, `JST`) y nombre de ciudad.
  - Conversión bidireccional local <-> ISO UTC (`localToUtcIso`, `utcIsoToLocal`).
  - Inferencia inteligente de zona horaria basada en coordenadas geográficas y nombre de ciudad (`inferZoneId`).
- [ ] **TASK-602 (TDD - GREEN)**: Implementar `TravelDateTimeFormatter` en `core/datetime/TravelDateTimeFormatter.kt`.
- [ ] **TASK-603 (TDD - REFACTOR)**: Asegurar manejo seguro ante cadenas vacías o malformadas y rendimiento óptimo.

### Fase 2: Actualización de Modelos y Serialización JSON
- [ ] **TASK-604 (TDD - RED)**: Crear tests unitarios en `TravelPlanJsonPortabilityTest` para:
  - Serialización y deserialización de `ItineraryStopItem` con `timeZoneId`, `startTimeUtcIso`, `endTimeUtcIso`.
  - Compatibilidad total hacia atrás con archivos existentes sin estos campos (ej. `plans/japon-tierra-samurais-2027.json`).
  - Serialización y deserialización de `TransportSegmentItem` con `departureTimeUtcIso` y `arrivalTimeUtcIso`.
- [ ] **TASK-605 (TDD - GREEN)**: Actualizar `ItineraryStopItem`, `TransportSegmentItem` y `TravelPlanJsonCodec`.
- [ ] **TASK-606 (TDD - REFACTOR)**: Validar que todos los tests de serialización pasan en verde.

### Fase 3: Portabilidad JSON en Ajustes (Local Storage / Google Drive SAF)
- [ ] **TASK-607 (TDD - RED)**: Crear tests unitarios en `DocumentsAndGuidesJsonPortabilityTest` para verificar la exportación e importación de Guías (`TravelGuideArticle`) y Billetes/Reservas (`TravelDocumentItem`), además de la detección automática de tipos de JSON.
- [ ] **TASK-608 (TDD - GREEN)**: Implementar `JsonDataPortabilityCard` en Compose con selectores SAF (`CreateDocument` / `OpenDocument`) integrados para Planes, Guías, Billetes y Copia de Seguridad Completa, con detector inteligente de contenido.
- [ ] **TASK-609 (TDD - REFACTOR)**: Integrar `JsonDataPortabilityCard` en `SettingsPreferencesScreen` en `MainScreen.kt`.

### Fase 4: Rediseño de Botones en Tarjetas y Visualización de Fechas/Horas
- [ ] **TASK-610 (UI)**: Actualizar `ItineraryStopCard` en `MainScreen.kt`:
  - Mostrar la fecha completa con formato regional del sistema.
  - Mostrar las horas con la zona horaria del lugar (ej. `12:25 (Múnich · UTC+1)`).
  - Reemplazar los botones apilados por una fila compacta no invasiva (`🗺️ Mapa`, `📅 Calendario`, `✏️ Editar`, `🗑️`).
- [ ] **TASK-611 (UI)**: Actualizar `TransportSegmentCard` en `MainScreen.kt`:
  - Mostrar la fecha completa con formato regional.
  - Indicar claramente zona horaria de salida y llegada con ciudades y desfases UTC.
  - Reemplazar botones gigantes por botones compactos y limpios.
- [ ] **TASK-612 (UI)**: Actualizar el editor de paradas (`ItineraryStopEditorModalSheet`) para permitir ver o editar la zona horaria del lugar.

### Fase 5: Verificación, Compilación y Pruebas en Dispositivo
- [ ] **TASK-613 (Test)**: Ejecutar suite completa de tests (`./gradlew testDebugUnitTest`).
- [ ] **TASK-614 (Build)**: Compilar APK (`./gradlew assembleDebug`).
- [ ] **TASK-615 (Deploy)**: Desplegar en Pixel 9 Pro XL e importar `plans/japon-tierra-samurais-2027.json` para verificar visualmente en el dispositivo.
- [ ] **TASK-616 (Docs & Commits)**: Actualizar `README.md` y `CHANGELOG.md` siguiendo Conventional Commits y regla POSIX EOF newline.
