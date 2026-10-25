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

### RF-07.4: Gestión Dinámica de Ciudades y Destinos (CRUD Completo y Presets)
1. **Añadir Nuevas Ciudades (`➕ Añadir Ciudad` / `CityWeatherEditorModalSheet`)**:
   - El usuario puede añadir cualquier ciudad o destino de Japón a la previsión meteorológica.
   - **Catálogo de Presets Populares**: Selector rápido con un solo toque que auto-rellena las coordenadas exactas de destinos frecuentes en Japón:
     - *Nara* (34.6851, 135.8048)
     - *Hiroshima* (34.3853, 132.4553)
     - *Miyajima / Itsukushima* (34.2964, 132.3197)
     - *Kamakura* (35.3192, 139.5467)
     - *Nikko* (36.7581, 139.5986)
     - *Takayama* (36.1460, 137.2522)
     - *Kanazawa* (36.5613, 136.6562)
     - *Sapporo* (43.0618, 141.3545)
     - *Fukuoka* (33.5902, 130.4017)
     - *Nagoya* (35.1815, 136.9066)
     - *Yokohama* (35.4437, 139.6380)
     - *Kobe* (34.6901, 135.1955)
     - *Okinawa / Naha* (26.2124, 127.6809)
     - *Sendai* (38.2682, 140.8694)
   - **Coordenadas Personalizadas**: Campos de texto para nombre en español, inglés, latitud y longitud personalizadas con validación numérica estricta.
2. **Editar Ciudades Existentes (`✏️ Editar Ciudad`)**:
   - Modificar el nombre de la ciudad o ajustar sus coordenadas geográficas en cualquier momento.
3. **Eliminar Ciudades (`🗑️ Eliminar Ciudad`)**:
   - Eliminar una ciudad que ya no forme parte de la ruta del viajero, suprimiendo sus previsiones asociadas y seleccionando automáticamente la siguiente ciudad activa. Se previene la eliminación si solo queda una ciudad en la lista.
4. **Persistencia Local Automática Offline (`Context.filesDir/saved_weather_cities.json`)**:
   - Todas las ciudades añadidas, modificadas o eliminadas persisten indefinidamente en el almacenamiento interno privado del dispositivo sin perderse al reiniciar la app o apagar el teléfono.

### RF-07.5: Obtención y Sincronización Dinámica de Ciudades desde Transportes e Itinerarios de Viaje
1. **Extracción Automática de Destinos**:
   - El sistema analiza de manera continua o bajo demanda los destinos y orígenes presentes en el plan de viaje activo:
     - **Tramos de Transporte (`TransportSegmentItem`)**: extrae las ciudades y aeropuertos de origen (`departureName`, `departureCode`) y destino (`arrivalName`, `arrivalCode`).
     - **Paradas del Itinerario (`ItineraryStopItem`)**: extrae las ciudades y regiones a partir del título (`title`), nombre en japonés (`placeNameJapanese`), notas y coordenadas geográficas (`latitude`, `longitude`).
2. **Resolución Inteligente de Coordenadas y Presets**:
   - Las ubicaciones extraídas se resuelven contra el catálogo geográfico de Japón y destinos internacionales frecuentes (Tokio, Kioto, Osaka, Hakone/Fuji, Nara, Hiroshima, Miyajima, Kamakura, Nikko, Takayama, Kanazawa, Sapporo, Fukuoka, Nagoya, Yokohama, Kobe, Okinawa, Sendai, Madrid, Doha, etc.).
   - Si una parada del itinerario tiene coordenadas específicas no coincidentes con los presets, se incorpora dinámicamente como ubicación personalizada con sus coordenadas reales.
3. **Convivencia con Acciones Manuales de CRUD**:
   - La lista sincronizada respeta las acciones manuales del usuario:
     - **Añadir manualmente (`➕ Ciudad`)**: el usuario puede seguir añadiendo cualquier ciudad adicional.
     - **Editar (`✏️ Editar`)**: el usuario puede personalizar el nombre y coordenadas de cualquier ciudad activa.
     - **Eliminar (`🗑️ Eliminar`)**: el usuario puede suprimir cualquier ciudad (con modal de confirmación de seguridad). Las ciudades expresamente eliminadas por el usuario no se reincorporan automáticamente de forma indeseada a menos que el usuario pulse explícitamente el botón de sincronización total (`🔄 Sincronizar Itinerario`).

### RF-07.6: Búsqueda Predictiva de Ciudades y Geocodificación Automática de Coordenadas de Google Maps
1. **Sugerencias Predictivas en Tiempo Real al Escribir**:
   - En el modal de añadir o editar ciudad (`CityWeatherEditorModalSheet`), a medida que el usuario escribe en el campo del nombre de la ciudad (a partir de 2 caracteres), se muestra de forma reactiva un listado predictivo con los destinos coincidentes.
   - Búsqueda insensible a mayúsculas, minúsculas y acentos sobre un catálogo amplio preinstalado de Japón (más de 50 destinos) y hubs internacionales clave.
2. **Autocompletado Inmediato de Datos y Coordenadas**:
   - Al seleccionar cualquier sugerencia del listado predictivo:
     - Se auto-rellena el nombre en español (`nameEs`) y en inglés (`nameEn`).
     - Se auto-rellenan instantáneamente la **Latitud** y **Longitud** exactas.
     - Se oculta la lista desplegable y se muestra confirmación visual de coordenadas obtenidas.
3. **Geocodificación Automática con Google Maps / Android Geocoder**:
   - Si el usuario introduce cualquier ciudad personalizada no catalogada, dispone del botón de acción `"📍 Obtener coordenadas de Google Maps"` (y resolución asistida al finalizar la escritura).
   - El sistema consulta el proveedor geográfico de Google Maps en Android (`android.location.Geocoder`) con fallback a la API de geocodificación de Open-Meteo para resolver la latitud y longitud exactas sin que el usuario tenga que buscarlas ni introducirlas a mano.
   - En caso de operar en modo offline sin conexión, el catálogo local resuelve de forma inmediata decenas de ciudades japonesas e internacionales sin requerir red.

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

### Escenario 6: Detección y sincronización automática de ciudades desde el itinerario y transportes
- **Dado** que el plan de viaje del usuario contiene vuelos desde Madrid/Doha a Tokio, Shinkansen de Tokio a Kioto y paradas en Nara, Osaka, Hakone y Miyajima
- **Cuando** el usuario entra en la pestaña de Tiempo
- **Entonces** la lista de ciudades del tiempo incluye automáticamente todas las ciudades de su ruta (Tokio, Kioto, Osaka, Hakone, Nara, Miyajima, Madrid, Doha) con sus previsiones meteorológicas calculadas
- **Y** el usuario puede editar sus nombres/coordenadas o eliminar cualquiera de ellas con confirmación de seguridad sin que se reincorporen indeseadamente.

### Escenario 7: Sugerencias predictivas y obtención automática de coordenadas de Google Maps
- **Dado** que el usuario abre el modal para añadir una ciudad en la previsión del tiempo
- **Cuando** teclea "Kobe" o "Kawa" en el nombre de la ciudad
- **Entonces** aparece un listado predictivo reactivo con las ciudades y regiones coincidentes
- **Y** al pulsar sobre una de ellas, se rellenan automáticamente los nombres (ES/EN) y las coordenadas de latitud y longitud
- **Y** si escribe una ciudad arbitraria y pulsa "Obtener coordenadas de Google Maps", el servicio de geocodificación resuelve y rellena automáticamente su latitud y longitud con éxito.
