# Tareas de Implementación: Mapa Integrado de Visitas del Día y Detalle con Guías y Billetes

- [x] **Tarea 1: Utilidades Geoespaciales y Proyección Mercator (TDD)**
  - Crear test `GeoProjectionUtilsTest.kt` (RED) validando:
    - Proyección Mercator normalizada de coordenadas (lat, lng -> x, y).
    - Cálculo de distancia geodésica Haversine en km entre paradas.
    - Cálculo de caja delimitadora (`calculateBoundingBox`) con margen.
  - Implementar `GeoProjectionUtils.kt` en `feature.planner.map` (GREEN).
  - Optimizar cálculos y verificar suite de tests (REFACTOR).

- [x] **Tarea 2: Motor de Vinculación Inteligente de Guías y Billetes (TDD)**
  - Crear test `ItineraryLinkedResourcesResolverTest.kt` (RED) validando:
    - Vinculación de guías de viaje por coincidencia de ciudad, etiquetas o nombre del lugar.
    - Vinculación de billetes de transporte y documentos de reserva por fecha y ciudad.
    - Detección de billetes con código QR disponible.
  - Implementar `ItineraryLinkedResourcesResolver.kt` en `feature.planner.map` (GREEN).
  - Refactorizar y verificar 100% de tests en verde (REFACTOR).

- [x] **Tarea 3: Componente UI de Mapa Interactivo del Día (`DayItineraryMapView`)**
  - Implementar lienzo Compose `DayItineraryMapView` con:
    - Proyección Mercator y soporte táctil para arrastrar (pan) y hacer zoom (pinch-to-zoom).
    - Marcadores circulares numerados `1..N` según orden cronológico con colores de categoría.
    - Trazado de ruta / polilínea continua uniendo las visitas con indicador de distancia.
    - Marcador azul interactivo de ubicación actual del usuario (`📍 Tú estás aquí`).
    - Controles de zoom `[+]`, `[-]`, recentrado en usuario `🎯` y ajuste a la ruta `🔍`.

- [x] **Tarea 4: Ficha Modal de Detalle con Guía y Billetes QR (`StopDetailMapModalSheet`)**
  - Implementar hoja modal Compose `StopDetailMapModalSheet` que despliega:
    - Cabecera con número, título, categoría, hora inicio/fin y duración.
    - Nombre en japonés con botón de copia rápida al portapapeles.
    - Notas y observaciones.
    - Tarjeta de guía relacionada con botón directo para abrirla.
    - Tarjeta de billete digital relacionado con botón `📱 Ver QR` (brillo al 100%).
    - Botones de acción directa: `🗺️ Abrir en Google Maps` y `✏️ Editar Parada`.

- [x] **Tarea 5: Integración en el Planificador (`TripPlannerTabScreen`)**
  - Añadir alternador de vista superior `[ 📋 Horario | 🗺️ Mapa del Día ]`.
  - Integrar `DayItineraryMapView` y vincular el toque de marcador para abrir `StopDetailMapModalSheet`.
  - Declarar permiso `ACCESS_FINE_LOCATION` en `AndroidManifest.xml`.

- [x] **Tarea 6: Verificación de Tests y Despliegue en Google Pixel 10 Pro XL**
  - Ejecutar `./gradlew testDebugUnitTest` y verificar 100% verde.
  - Compilar y desplegar con `./scripts/deploy-authorized-devices.sh`.

- [x] **Tarea 7: Selector Configurable de Capas de Mapa (`MapTileLayerType` - TDD)**
  - Añadir tests unitarios en `GeoProjectionUtilsTest.kt` (RED) validando:
    - Generación de URLs de teselas localizadas (`hl=es` / `hl=en`) para Google Maps Estándar (`GOOGLE_STREETS`), Google Satélite (`GOOGLE_HYBRID`) y OpenStreetMap (`OPENSTREETMAP`).
    - Etiquetas de interfaz bilingües y rotación de capas.
  - Implementar `MapTileLayerType` y el selector visual de capas en `DayItineraryMapView.kt` con cambio en caliente mediante `setTileLayer()` en Leaflet (GREEN).
  - Desplegar y verificar en Google Pixel 9 Pro XL (REFACTOR).


