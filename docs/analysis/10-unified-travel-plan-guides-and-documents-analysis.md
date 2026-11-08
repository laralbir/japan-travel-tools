# Análisis Técnico 10: Importación Unificada de Plan de Viaje, Guías y Billetes en un Solo JSON

- **ID**: `ANALYSIS-10`
- **Componente**: `core.portability` & `feature.documents`
- **Fecha**: 2026-11-07
- **Estado**: Aprobado

---

## 1. Problema

Actualmente, cuando un usuario importa un archivo JSON de plan de viaje (como `japon-tierra-samurais-2027.json`), `SharedTravelPlanStore.importJson()` procesa las paradas del itinerario (`itineraryStops`) y los tramos de transporte (`transportSegments`), pero ignora o no recibe las Guías de Viaje (`guides`) ni los Billetes/Documentos (`documents`).

Esto provoca que el usuario deba realizar hasta 3 importaciones separadas (Plan, Guías y Billetes) o configurar un backup completo. La experiencia de usuario ideal es que, si un archivo JSON incluye `"guides"` o `"documents"` junto a `"itineraryStops"`, la aplicación importe automáticamente todo el contenido en un solo paso.

Adicionalmente, `JsonContentDetector` identifica copias de seguridad completas solo si contienen la clave legacy `cloudBackupVersion` o `syncData`, pero los backups exportados por `GoogleDriveSyncHelper` utilizan `backupId` y `travelPlans`, por lo que caían erróneamente en el analizador de plan simple.

---

## 2. Arquitectura de la Solución

### 2.1 Coordinación en `SharedTravelPlanStore.importJson()`
Al importar un JSON de plan de viaje:
1. Se deserializa `PortableTravelPlanBundle` mediante `TravelPlanJsonCodec.importFromJson()`.
2. Se actualiza el estado reactivo del plan (`_planState` y lista de planes).
3. Si el JSON contiene la clave `"guides"`, se invoca `DocumentsAndGuidesJsonCodec.importGuidesJson()` y se pasan a `SharedDocumentsAndGuidesStore.importGuides(guides, merge = mergeWithExisting)`.
4. Si el JSON contiene la clave `"documents"`, se invoca `DocumentsAndGuidesJsonCodec.importDocumentsJson()` y se pasan a `SharedDocumentsAndGuidesStore.importDocuments(docs, merge = mergeWithExisting)`.

### 2.2 Detección Inteligente en `JsonContentDetector`
1. **Detección de Backup Completo Robusta**:
   Se añade la condición: `trimmed.contains("\"backupId\"") && (trimmed.contains("\"travelPlans\"") || trimmed.contains("\"travelGuides\""))`.
2. **Conteo y Resumen de Contenido Combinado**:
   Al detectar `TRAVEL_PLAN`, si el JSON contiene `"guides"` o `"documents"`, se extraen sus conteos (`guideCount`, `documentCount`) y se incluyen en `summaryEs` y `summaryEn` para que el diálogo de confirmación informe con precisión:
   *"Plan de viaje con X paradas, Y transportes, Z guías y W documentos/billetes."*

### 2.3 `JsonDataPortabilityCard`
Al ejecutar la importación de `TRAVEL_PLAN`, el mensaje de estado final informa de los elementos adicionales importados:
`"✅ Plan «...» importado (X paradas, Y transportes, Z guías, W documentos)."`

---

## 3. Plan de Testing (TDD)
- **Unit Test 1**: `TravelPlanJsonPortabilityTest` -> `SharedTravelPlanStore importJson automatically imports embedded guides and documents into shared store`.
- **Unit Test 2**: `JsonContentDetectorTest` -> `inspect detects travel plan with embedded guides and documents` y valida `guideCount`, `documentCount` y el resumen.
- **Unit Test 3**: `JsonContentDetectorTest` -> `inspect detects cloud backup with backupId and travelPlans`.
