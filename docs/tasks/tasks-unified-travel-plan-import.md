# Tareas: Importación Unificada de Plan, Guías y Billetes en un Solo JSON

- **ID**: `TASKS-UNIFIED-PLAN-IMPORT`
- **Referencia**: `docs/analysis/10-unified-travel-plan-guides-and-documents-analysis.md`
- **Ciclo TDD**: Red ➔ Green ➔ Refactor

---

## 1. Tareas de Testing (RED)
- [x] `T-10.1`: Añadir test en `TravelPlanJsonPortabilityTest` para verificar que `SharedTravelPlanStore.importJson()` importa guías y documentos en `SharedDocumentsAndGuidesStore` si están presentes en el JSON.
- [x] `T-10.2`: Añadir test en `JsonContentDetectorTest` para verificar detección de guías y documentos embebidos en `TRAVEL_PLAN`.
- [x] `T-10.3`: Añadir test en `JsonContentDetectorTest` para verificar detección de `FULL_BACKUP` con `backupId` y `travelPlans`.

---

## 2. Tareas de Implementación (GREEN)
- [x] `T-10.4`: Modificar `SharedTravelPlanStore.importJson()` en `TravelPlanJsonPortability.kt` para importar guías y documentos embebidos.
- [x] `T-10.5`: Actualizar `JsonContentDetector.kt` para detectar backups de `GoogleDriveSyncHelper` y contar guías/documentos en planes de viaje.
- [x] `T-10.6`: Actualizar `JsonDataPortabilityCard.kt` para reflejar guías y documentos en el feedback de importación.

---

## 3. Tareas de Datos y Verificación (REFACTOR & ASSETS)
- [x] `T-10.7`: Generar `/Users/carlos/Documents/Japon/saved_travel_guides.json` con las 4 guías detalladas solicitadas (Sellos Goshuin/Eki Stamp, Tránsito y Vuelos, Suica/Transporte, Gastronomía).
- [x] `T-10.8`: Actualizar `/Users/carlos/Documents/Japon/japon-tierra-samurais-2027.json` integrando `"guides"` y `"documents"`.
- [x] `T-10.9`: Generar `/Users/carlos/Documents/Japon/japon-2027-backup-completo.json`.
- [x] `T-10.10`: Ejecutar `./gradlew testDebugUnitTest` y verificar 100% verde.
