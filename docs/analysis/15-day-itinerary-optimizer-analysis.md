# ANALYSIS-15: Análisis Técnico del Optimizador Geodésico de Rutas del Día

- **ID**: `ANALYSIS-15`
- **Especificación**: [`SPEC-13`](../specs/13-day-itinerary-optimizer-spec.md)
- **Capa**: Domain & UI (`GeoProjectionUtils`, Algoritmo TSP Heurístico)
- **Paquete Kotlin**: `com.laralbir.japantraveltools.feature.planner.optimizer`

---

## 1. Algoritmo de Optimización Offline

Para resolver el problema de ordenación espacial sin conexión a internet ni consumo excesivo de CPU:

1. **Matriz de Distancias Simétrica $D_{i,j}$**:
   - Para las $N$ paradas del día, se calcula la distancia Haversine $D_{i,j} = \text{distanceMeters}(P_i, P_j)$.
2. **Heurística Híbrida Constructiva + Mejora Local**:
   - **Fase 1 (Vecino más Cercano / Nearest Neighbor)**:
     - Comenzando en el punto de inicio (parada 1 o hotel), se selecciona iterativamente la parada no visitada más cercana que respete las restricciones temporales de paradas fijas.
   - **Fase 2 (2-Opt Local Search)**:
     - Para cada par de aristas no consecutivas $(i, i+1)$ y $(j, j+1)$, si intercambiar las conexiones reduce la distancia total sin violar restricciones de paradas fijadas, se aplica la inversión del subsegmento.
     - El bucle converge en $O(N^2)$ en menos de 10-20 iteraciones, tardando típicamente $<5\text{ ms}$ para $N \le 15$.

---

## 2. Modelado de Restricciones

```kotlin
data class RouteNode(
    val stop: ItineraryStopItem,
    val isFixed: Boolean,
    val fixedSlotIndex: Int?
)

class DayRouteOptimizer {
    fun optimize(
        stops: List<ItineraryStopItem>,
        options: RouteOptimizationOptions
    ): RouteOptimizationResult {
        // Validación: Al menos 3 paradas con coordenadas válidas
        // Extracción de nodos con coordenadas
        // Aplicación de algoritmo Nearest Neighbor + 2-opt condicionado
        // Recalculación de horas tentativas preservando duraciones
        // Devolución de métricas y lista optimizada
    }
}
```

---

## 3. Estrategia de Testing (TDD)

1. **`DayRouteOptimizerTest`**:
   - **Caso Tokio Zig-Zag**: 4 paradas (Asakusa -> Shibuya -> Akihabara -> Ueno). Verificar que el orden optimizado agrupa las del noreste (Asakusa, Ueno, Akihabara) y traslada Shibuya al final (o viceversa), reduciendo la distancia en $>30\%$.
   - **Caso Kioto con Parada Fija**: Reserva de comida en Gion a las 13:00 con `isTimeFixed = true`. Verificar que la parada de Gion no se mueve de su orden temporal correspondiente al mediodía.
   - **Caso con Paradas sin Coordenadas**: Validar que paradas sin lat/lng se preservan en su posición original sin provocar excepciones.
   - **Caso Degenerado**: Menos de 3 paradas devuelve el mismo orden con 0 metros de ahorro.
