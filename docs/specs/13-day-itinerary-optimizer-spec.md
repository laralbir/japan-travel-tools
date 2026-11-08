# SPEC-13: Optimizador Geodésico y Reordenación Inteligente de Visitas del Día

- **ID**: `SPEC-13`
- **Estado**: `Aprobado para Análisis e Implementación`
- **Herramienta**: Herramienta 4 — Planificador de Viaje (`SPEC-04`)
- **Referencias Cruzadas**:
  - [`SPEC-00`](00-core-configuration-spec.md) (Configuración Base, Rendimiento y Offline-First)
  - [`SPEC-04`](04-trip-planner-spec.md) (Planificador de Viaje, Horarios, Mapa del Día y Paradas)
  - [`ANALYSIS-04`](../analysis/04-day-itinerary-map-and-linked-details-analysis.md) (Cálculos Geodésicos y Proyección Mercator)
  - [`ANALYSIS-15`](../analysis/15-day-itinerary-optimizer-analysis.md) (Algoritmos Heurísticos TSP Offline y Análisis Técnico)

---

## 1. Contexto, Motivación y Objetivo

Al organizar un viaje a Japón, los viajeros suelen apuntar los templos, miradores, restaurantes y barrios que desean visitar en una misma jornada sin conocer con exactitud la geografía de megaciudades como Tokio u Osaka. Esto provoca con frecuencia **itinerarios con efecto "zig-zag"** (por ejemplo: comenzar en Asakusa al noreste, desplazarse a Shibuya al suroeste para comer, y regresar a Akihabara por la tarde), perdiendo horas valiosas en transbordos de metro y tren.

En la versión actual de la app:
- Las paradas se ordenan estrictamente según las horas introducidas manualmente (`order = 1..N`).
- Si el usuario desea optimizar su día, debe comparar mentalmente las coordenadas de cada parada o moverlas una a una manualmente.

### Objetivo
Dotar al Planificador de Viaje de un **Optimizador Geodésico 100% Offline** capaz de:
1. Analizar las coordenadas geográficas (`lat`, `lng`) de todas las paradas planificadas para un día concreto.
2. Identificar **paradas con horario fijo obligatorio** (como reservas de restaurantes o entradas con franja horaria estricta de *teamLab Planets*, *Shibuya Sky* o *Museo Ghibli*) que deben mantenerse inamovibles.
3. Calcular mediante algoritmos heurísticos offline (*Nearest Neighbor + 2-opt local search*) el recorrido geodésico más corto que conecte todas las visitas.
4. Presentar al viajero una comparativa clara en pantalla (**distancia original vs. distancia optimizada, ahorro estimado en kilómetros y tiempo**) y permitir aplicar el nuevo orden con un solo toque sin alterar sus datos hasta su aprobación explícita.

---

## 2. Requisitos Funcionales (RF)

### RF-13.1: Algoritmo de Optimización de Ruta Offline (Heurística TSP con Restricciones)
1. **Cálculo de Matriz de Distancias Geodésicas**:
   - Uso de la fórmula Haversine integrada en `GeoProjectionUtils` para calcular la distancia en metros entre cada par de paradas con coordenadas válidas.
2. **Tratamiento de Restricciones y Puntos Fijos**:
   - **Paradas Fijas / Inamovibles (`isTimeFixed = true`)**: Visitas marcadas como reserva con hora cerrada o eventos con hora estricta no alteran su posición cronológica.
   - **Punto de Inicio Fijo**: Opcionalmente, la primera parada del día (habitualmente el hotel de salida) se mantiene fija como origen.
3. **Optimización Rápida y Determinista**:
   - Para un número habitual de visitas por día ($N \in [3, 15]$), el algoritmo ejecuta una solución constructiva por vecino más cercano (*Nearest Neighbor*) seguida de optimización por intercambio de aristas (*2-opt local search*).
   - Tiempo de ejecución acotado a menos de 50 milisegundos en el hilo de cálculo, garantizando una respuesta instantánea 100% offline.

### RF-13.2: Comparativa de Ahorro y Previsualización UI
1. **Métricas de Comparativa**:
   - Distancia total actual: suma de distancias geodésicas en el orden actual ($\text{km}$).
   - Distancia optimizada: suma en el orden propuesto ($\text{km}$).
   - Reducción porcentual y ahorro absoluto en kilómetros (ej. *"Ahorro de 5.4 km (32% menos trayecto)"*).
   - Estimación conservadora de tiempo ganado en desplazamientos (asumiendo promedio de transporte urbano en Japón).
2. **Diálogo / Hoja Modal de Optimización (`DayRouteOptimizationSheet`)**:
   - Lista interactiva mostrando la secuencia sugerida con chips de categoría y distancias tramo a tramo.
   - Indicador visual claro para paradas fijas (`🔒 Hora fija reservada`).
   - Botón `🧭 Aplicar Orden Optimizado` (con confirmación reversible).
   - Botón `✕ Mantener Orden Actual` (descarta la sugerencia sin alterar nada).

### RF-13.3: Ajuste de Horarios Tentativos al Aplicar el Orden
1. **Recalculación de Horarios Sugeridos**:
   - Al aplicar la optimización, las horas de inicio y fin de las paradas flexibles se recalculan de forma coherente respetando la duración prevista de cada visita (`durationMinutes`) y añadiendo un colchón estimado de desplazamiento entre destinos consecutivos.
   - Las paradas fijadas conservan intacta su franja horaria original.
2. **Actualización Reactiva de Mapas y Listas**:
   - La nueva secuencia actualiza inmediatamente los índices `order = 1..N` en `SharedTravelPlanStore`, reflejándose en tiempo real en la lista del itinerario y en los marcadores numerados del **Mapa del Día (`DayItineraryMapView`)**.

---

## 3. Reglas de Negocio y Offline-First

1. **Inmutabilidad sin Autorización (RN-13.1)**: El optimizador jamás altera el orden de un plan guardado sin que el usuario pulse explícitamente el botón de confirmación.
2. **Tolerancia a Paradas sin Coordenadas (RN-13.2)**: Si una parada carece de coordenadas (`latitude == null`), el optimizador la mantiene en su posición horaria actual y optimiza las demás paradas alrededor de ella.
3. **Mínimo de Paradas Requeridas (RN-13.3)**: El botón de optimización solo se habilita si el día contiene al menos 3 paradas con coordenadas geográficas válidas.

---

## 4. Diseño de Datos

```kotlin
data class RouteOptimizationResult(
    val dayIndex: Int,
    val originalStops: List<ItineraryStopItem>,
    val optimizedStops: List<ItineraryStopItem>,
    val originalDistanceMeters: Double,
    val optimizedDistanceMeters: Double,
    val distanceSavedMeters: Double,
    val percentageSaved: Double,
    val estimatedMinutesSaved: Int
)

data class RouteOptimizationOptions(
    val keepFirstStopFixed: Boolean = true,
    val keepLastStopFixed: Boolean = false,
    val respectFixedTimeStops: Boolean = true,
    val defaultTransferBufferMinutes: Int = 20
)
```

---

## 5. Criterios de Aceptación (DoD)

1. Suite unitaria exhaustiva para el algoritmo de optimización (resolución de casos de prueba con paradas en Tokio y Kioto, preservación de paradas fijas y paradas sin coordenadas).
2. Tiempos de cálculo inferiores a 50 ms para 15 paradas.
3. Componente Compose bilingüe con soporte Material 3 (Claro/Oscuro).
4. Persistencia íntegra de las nuevas horas y órdenes en `SharedTravelPlanStore`.
5. Salto de línea final estándar POSIX (`\n`) en todos los archivos.
