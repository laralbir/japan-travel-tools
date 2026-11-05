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

