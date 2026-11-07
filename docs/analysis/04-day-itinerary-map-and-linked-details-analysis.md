# Análisis Técnico 04: Mapa Integrado de Visitas del Día, Ubicación Actual y Ficha de Detalle

## 1. Visión y Arquitectura

El objetivo es ofrecer una experiencia visual e interactiva integrada en el **Planificador de Viaje**, permitiendo al viajero visualizar en un mapa todos los puntos programados para un día (`ItineraryStopItem`), su posición GPS actual en tiempo real (`UserLocation`), el trazado de la ruta con distancias en kilómetros, y desplegar al tocar cualquier punto una ficha completa con sus **guías de viaje** y **billetes digitales con QR** asociados.

```
                      [SharedTravelPlanStore]
                                 │
                         (ItineraryStopItem)
                                 │
     ┌───────────────────────────┼───────────────────────────┐
     ▼                           ▼                           ▼
[GeoProjectionUtils]     [UserLocationHelper]     [ItineraryLinkedResourcesResolver]
(Mercator / BoundingBox)    (GPS / Coarse)          (Guías + Billetes + QR)
     │                           │                           │
     └───────────────────────────┼───────────────────────────┘
                                 ▼
                      [DayItineraryMapView]
                   (Compose Canvas Interactivo)
                                 │
                          (Tocar Marcador)
                                 ▼
                     [StopDetailMapModalSheet]
               (Ficha Completa + Guía + Billete QR)
```

---

## 2. Modelos y Utilidades Geoespaciales (`GeoProjectionUtils`)

### Contrato y Funciones Clave
```kotlin
data class GeoPoint(val latitude: Double, val longitude: Double)

data class GeoBoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
    val centerLat: Double,
    val centerLng: Double
)

object GeoProjectionUtils {
    // Proyección Web Mercator normalizada [0.0 .. 1.0]
    fun projectToMercatorNormalized(lat: Double, lng: Double): Pair<Double, Double>
    
    // Cálculo de caja delimitadora para auto-ajuste de cámara (fit bounds)
    fun calculateBoundingBox(points: List<GeoPoint>, paddingRatio: Double = 0.2): GeoBoundingBox
    
    // Distancia geodésica entre dos puntos (Fórmula de Haversine)
    fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
    
    // Distancia total del recorrido diario
    fun calculateRouteTotalDistanceKm(stops: List<ItineraryStopItem>): Double
}
```

### 2.1 Selector Configurable de Capas Cartográficas (`MapTileLayerType`)
```kotlin
enum class MapTileLayerType {
    GOOGLE_STREETS,
    GOOGLE_HYBRID,
    OPENSTREETMAP;

    fun label(isEs: Boolean): String
    fun shortLabel(isEs: Boolean): String
    fun resolveTileUrl(isEs: Boolean): String
}
```
- **`GOOGLE_STREETS`** *(Por defecto)*: `https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}&hl={es|en}` (presenta topónimos de Japón en español/romaji junto al japonés).
- **`GOOGLE_HYBRID`**: `https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}&hl={es|en}` (vista satelital con nombres de calles y estaciones superpuestos).
- **`OPENSTREETMAP`**: `https://tile.openstreetmap.org/{z}/{x}/{y}.png` (cartografía comunitaria detallada en senderos de templos y parques).

### 2.2 Etiquetas Compactas Multilínea, Anticolisión por Proximidad y Ajustes de Layout (`DayItineraryMapView` & `MainScreen`)
- **Etiquetas Compactas (Máx. 2 Líneas)**:
  - Las etiquetas flotantes junto a cada marcador en Leaflet emplean CSS compacto (`max-width`, `-webkit-line-clamp: 2`, `overflow: hidden`, `text-overflow: ellipsis`) para evitar carteles alargados de una sola línea que invadan el mapa.
- **Ocultación Automática Anticolisión en `zoomend` / Distancia en Píxeles**:
  - En cada evento de renderizado o cambio de zoom (`zoomend`), se proyectan las coordenadas de las paradas a coordenadas de contenedor (`map.latLngToContainerPoint(latLng)`).
  - Si la distancia en píxeles en pantalla respecto a una etiqueta visible previa es inferior al umbral mínimo anticolisión (y la parada no es la seleccionada actualmente), la etiqueta secundaria se oculta automáticamente (`.label-hidden`), reapareciendo de forma fluida al ampliar el zoom.
- **Conmutador Manual de Etiquetas (`🏷️`)**:
  - Control flotante en la botonera del mapa (`showLabels: Boolean`) sincronizado con JS (`setLabelsVisible(visible)`) que permite al usuario ocultar o mostrar todas las etiquetas a voluntad.
- **Aprovechamiento Vertical de `Scaffold` y Botonera Horizontal en Meteorología**:
  - Uso de `Modifier.consumeWindowInsets(innerPadding)` y reducción de padding inferior redundante en `MainScreen.kt` / `TripPlannerTabScreen` para eliminar márgenes muertos sobre la barra inferior.
  - Disposición horizontal simétrica (`Row` con `Modifier.weight(1f)`) para los botones `➕ Ciudades del Plan` y `➕ Añadir Ciudad` en `WeatherToolScreen`.

---

## 3. Motor de Vinculación Inteligente (`ItineraryLinkedResourcesResolver`)

Relaciona automáticamente cada parada del itinerario con el acervo documental de la aplicación:

### Algoritmo de Vinculación de Guías (`resolveRelatedGuides`)
1. **Coincidencia por Ciudad/Región**: Si `guide.cityRegion` coincide o está contenida en `stop.placeNameJapanese`, `stop.title` o `stop.notes` (ej. "Kioto", "Tokio", "Osaka", "Nara", "Hakone").
2. **Coincidencia por Etiquetas**: Si alguna etiqueta `guide.tags` (ej. "Templo", "Comida", "Fushimi", "Asakusa") aparece en los metadatos de la parada.
3. **Coincidencia por Términos Relevantes**: Búsqueda semántica simple entre títulos.

### Algoritmo de Vinculación de Billetes y Documentos (`resolveRelatedTicketsAndTransport`)
1. **Billetes de Transporte (`TransportSegmentItem`)**: Si la fecha del tramo (`departureTimeIso.substringBefore('T')`) coincide con la fecha de la parada (`stop.dateIso`), o si las estaciones de salida o llegada coinciden con la ciudad de la parada.
2. **Documentos de la Bóveda (`TravelDocumentItem`)**: Documentos clasificados como `TICKET`, `TRANSPORT`, `FLIGHT` u `HOTEL` cuyas notas o título incluyan la fecha o la ciudad de la visita.
3. **Acceso Inmediato a Códigos QR**: Si el billete o tramo dispone de `barcodeMessage`, se ofrece el botón directo `📱 Ver QR` para desplegar el pase digital con brillo al 100%.

---

## 4. Localización del Usuario (`UserLocationHelper`)

- Consulta reactiva de `android.location.LocationManager` con fallback de `GPS_PROVIDER` a `NETWORK_PROVIDER` y `PASSIVE_PROVIDER`.
- Comprobación segura de permisos en tiempo de ejecución (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`).
- Marcador visual distintivo:
  - Círculo azul intenso (`Color(0xFF1976D2)`) con halo exterior translúcido y punto central blanco.
  - Etiqueta flotante *"📍 Tú estás aquí"* / *"📍 You are here"*.
- Botón *"🎯 Centrar en mi ubicación"* para recentrar suavemente el mapa.

---

## 5. Estrategia de Testing (TDD)

1. **`GeoProjectionUtilsTest`**:
   - Validación matemática de proyección Mercator (valores en rango 0..1).
   - Cálculo de distancia Haversine entre Tokio y Kioto (~370-380 km).
   - Cálculo de `calculateBoundingBox` asegurando que todos los puntos quedan incluidos con el margen correspondiente.
2. **`ItineraryLinkedResourcesResolverTest`**:
   - Validación de vinculación de Guía de Kioto a parada de Fushimi Inari.
   - Validación de vinculación de billete de Shinkansen por fecha y ciudad.
