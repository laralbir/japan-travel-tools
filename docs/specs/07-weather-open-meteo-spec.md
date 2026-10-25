# SPEC-07: Herramienta de Previsión Meteorológica con Open-Meteo para Planificación de Usuario e IA

- **Estado**: En Especificación (Beta)
- **Versión**: `0.0.1-beta.3`
- **Idiomas de UI**: Inglés (`en`, por defecto) y Español (`es`)
- **Fuente de Datos Abierta**: [Open-Meteo Weather Forecast API](https://open-meteo.com/) (modelos globales + **JMA — Japan Meteorological Agency**, sin API Key requerida, licencia CC BY 4.0)

---

## 1. Objetivo

Proporcionar una herramienta de **Previsión Meteorológica y Planificación Climática en Japón** basada en **Open-Meteo API** con caché 100% **Offline-First**, diseñada para que la información del tiempo no sea un mero dato decorativo, sino **inteligencia accionable tanto para el usuario humano como para el Asistente y Optimizador de Planes con IA (Gemini)**:

1. **Para el Usuario**:
   - Consultar el tiempo previsto por ciudad japonesa (*Tokio*, *Kioto*, *Osaka*, *Nara*, *Hakone / Monte Fuji*, *Hiroshima*) y por cada día del itinerario (`Asia/Tokyo`).
   - Recibir recomendaciones prácticas directas para planificar cada jornada:
     - **Idoneidad Exterior vs. Interior** (*Ideal para templos, parques y miradores al aire libre* vs. *Recomendado plan cubierto: mercados Nishiki/Kuromon, museos, galerías comercial Shotengai*).
     - **Equipamiento del día**: aviso de paraguas/chubasquero (`☔ Probabilidad de lluvia >= 50%`), ropa térmica por capas o protección solar.
     - **Índice de Visibilidad del Monte Fuji**: útil para decidir si mantener o mover la excursión a Hakone/Kawaguchiko o elegir el asiento `E` del Shinkansen.
2. **Para la IA Integrada (Gemini AI Plan Builder & Assistant)**:
   - Inyectar automáticamente el contexto meteorológico estructurado (`WeatherDayForecast`) en el motor de IA (`AiPlanBuilderEngine` y `GeminiAssistantEngine`).
   - Permitir que la IA **cree, reordene o adapte automáticamente el itinerario del viaje según el tiempo previsto** (por ejemplo, si el Día 2 en Kioto tiene lluvia intensa prevista en Open-Meteo, la IA prioriza actividades cubiertas o añade notas de rutas por galerías techadas y transporte directo en metro en lugar de caminatas largas bajo la lluvia).

---

## 2. Requisitos Funcionales (RF)

### RF-07.1: Integración con Open-Meteo API y Modelos JMA (Japón)
1. **Consulta por Coordenadas de Ciudades y Paradas del Itinerario**:
   - Endpoint REST GET sin clave de API:
     `https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max&timezone=Asia%2FTokyo`
   - Interpretación estándar de códigos meteorológicos **WMO (`weather_code` 0..99)**:
     - `0, 1`: Despejado / Mayormente despejado (`CLEAR_SUNNY` ☀️)
     - `2, 3, 45, 48`: Parcialmente nublado / Niebla (`PARTLY_CLOUDY` ⛅)
     - `51..67, 80..82`: Llovizna / Lluvia (`RAIN` 🌧️)
     - `71..77, 85..86`: Nieve (`SNOW` ❄️)
     - `95..99`: Tormenta / Lluvia severa (`HEAVY_RAIN_STORM` ⛈️)
2. **Estrategia 100% Offline-First con Semilla Climática (`Seed Weather Snapshot`)**:
   - La app incluye datos semilla preinstalados para las principales ciudades de Japón (*Tokio*, *Kioto*, *Osaka*, *Hakone / Monte Fuji*) y persiste la última respuesta descargada de Open-Meteo junto con su marca de tiempo (`lastUpdatedIso`).
   - En modo avión o sin cobertura, el usuario y la IA siguen disponiendo de la última previsión cacheada o de la semilla climática estacional sin errores bloqueantes.

### RF-07.2: Utilidad Directa para la Planificación del Usuario
1. **Tarjetas Climáticas en el Planificador de Viaje y en el Panel Principal**:
   - Cada día del itinerario muestra su resumen meteorológico (`☀️ 19°C / 11°C · ☔ 10%` o `🌧️ 15°C / 9°C · ☔ 80%`).
   - Etiqueta automática de planificación:
     - **`OUTDOOR_OPTIMAL` (Ideal Exterior)**: Cuando la probabilidad de lluvia es `< 40%` y el código WMO es soleado o parcialmente nublado.
     - **`INDOOR_RECOMMENDED` (Plan Interior / Cubierto)**: Cuando la probabilidad de lluvia es `>= 50%` o hay código WMO de lluvia/tormenta/nieve intensa.
   - Recomendación práctica en el idioma activo (`ES`/`EN`) con consejos sobre paraguas (*konbini vinyl umbrella*), calzado impermeable, visibilidad del Monte Fuji y sugerencias de barrios cubiertos (*Shotengai*, mercados techados, museos).

### RF-07.3: Utilidad Directa para la Planificación con IA (`Weather-Aware AI Planner`)
1. **Contexto Meteorológico en `AiPlanBuilderEngine`**:
   - Al pulsar **"✨ Optimizar Plan según el Tiempo (Open-Meteo + IA)"** o **"✨ Mejorar / Ampliar Plan con Gemini IA"**, el motor cruza cada parada del itinerario (`ItineraryStopItem`) con la previsión de Open-Meteo para esa fecha y coordenadas.
   - Si detecta lluvia (`RAIN` / `HEAVY_RAIN_STORM` o probabilidad `>= 50%`) en un día del viaje:
     - Añade alertas y alternativas cubiertas en las paradas al aire libre (ej. rutas cubiertas por *Teramachi / Nishiki Market* en Kioto o galerías subterráneas *Namba Walk / Kuromon* en Osaka).
     - Ajusta los márgenes de desplazamiento y actualiza el resumen de optimización (`aiOptimizationSummary`) explicando qué ajustes climáticos se han aplicado.
   - Además, el paquete exportable/importable `PortableTravelPlanBundle` (`JSON`) conserva estas recomendaciones para poder compartirlas entre dispositivos o apps externas.

---

## 3. Criterios de Aceptación (Gherkin)

### Escenario 1: Consulta y actualización desde Open-Meteo con respaldo Offline
- **Dado** que el usuario abre la herramienta de Tiempo o el Planificador de Viaje
- **Cuando** pulsa "Actualizar Open-Meteo" teniendo conexión a internet
- **Entonces** la app descarga la previsión diaria en zona horaria `Asia/Tokyo`, interpreta los códigos WMO y actualiza las temperaturas máximas/mínimas y probabilidad de lluvia
- **Y** si el dispositivo está en Modo Avión, mantiene la previsión cacheada/semilla indicando que opera en modo offline.

### Escenario 2: Recomendación de planificación para el usuario según lluvia o sol
- **Dado** un día con previsión de Open-Meteo en Kioto con `weatherCode = 63` (lluvia moderada) y `precipitationProbabilityMax = 80%`
- **Cuando** el usuario consulta ese día en el Planificador
- **Entonces** el sistema clasifica la jornada como `INDOOR_RECOMMENDED` y muestra el aviso de llevar paraguas y priorizar recintos cubiertos (Mercado Nishiki, museos o galerías comerciales).

### Escenario 3: Optimización automática del itinerario por la IA usando datos de Open-Meteo
- **Dado** un plan de viaje activo y una previsión meteorológica de Open-Meteo con lluvia en uno de los días del itinerario
- **Cuando** el usuario solicita a la IA optimizar el plan teniendo en cuenta el tiempo (`optimizePlanWithWeather`)
- **Entonces** `AiPlanBuilderEngine` enriquece las paradas afectadas con alternativas cubiertas y consejos climáticos para esa fecha, dejando constancia en `aiOptimizationSummary`.
