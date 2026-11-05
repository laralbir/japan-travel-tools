# SPEC-04: Herramienta de Planificador de Viaje (Itinerario, Fotos, Google Maps, Calendario y Horarios)

- **ID**: `SPEC-04`
- **Estado**: `Aprobado para Análisis/Implementación`
- **Versión Objetivo**: `0.0.1-beta.X` -> `0.0.1`

---

## 1. Objetivo

Proporcionar una herramienta integral y **100% funcional offline** para planificar el viaje a Japón día a día y por tramos horarios, integrando:
1. **Calendario y Línea Temporal de Horarios (`Timeline`)** propia dentro de la app + sincronización/exportación opcional al Calendario del sistema Android / Google Calendar.
2. **Galería de Fotos asociada a cada parada/actividad** (tanto fotos de referencia/inspiración previas como fotos tomadas durante el viaje, almacenadas offline).
3. **Integración dual con Google Maps**:
   - **Punto 1 (Offline + Deep Links / Intents)**: Almacenamiento local de coordenadas (`lat, lng`), dirección en inglés/japonés, enlaces de Google Maps y apertura directa de navegación/rutas en la app de Google Maps.
   - **Punto 2 (Mapa Interactivo Embebido)**: Vista de mapa integrada en la pantalla del planificador (`Google Maps SDK for Android` / Compose Maps con caché de teselas y fallback offline de marcadores y rutas del día).
4. **Vinculación directa** con documentos del repositorio (ej. reserva del hotel o entrada del museo) y guías de viaje de [`SPEC-03`](03-documents-and-id-vault-spec.md).

---

## 2. Requisitos Funcionales (RF)

### RF-04.1: Gestión de Viajes, Calendario y Días del Itinerario
1. **Configuración del Viaje**:
   - Nombre del viaje (ej. *"Japón Otoño 2026"*), fecha de inicio (`startDate`) y fecha de fin (`endDate`).
   - Generación automática de los días del viaje (`Día 1`, `Día 2`... con su fecha correspondiente y ciudad principal asignada, ej. *Día 1 - Tokio*, *Día 5 - Kioto*).
2. **Vista de Calendario Mensual / Semanal**:
   - Selector de calendario visual donde cada día muestra un resumen de la ciudad base y el número de actividades programadas.
3. **Línea Temporal de Horarios (`Schedule / Timeline`)**:
   - Dentro de cada día, las actividades se ordenan cronológicamente por **Hora de inicio (`startTime`)** y **Hora de fin (`endTime`)** en huso horario de Japón (`Asia/Tokyo`, JST).
   - Detección automática de **solapamientos horarios** (aviso visual si dos eventos se pisan) y cálculo de tiempos libres entre paradas.
   - Categorías de parada: ⛩️ *Lugar/Templo*, 🍜 *Restaurante/Comida*, 🚄 *Transporte/Tren*, 🏨 *Check-in/Hotel*, 🛍️ *Compras*, 🎟️ *Actividad/Ocio*.
4. **Creación, Edición y Eliminación Directa de Planes y Paradas (CRUD Completo)**:
   - **Añadir Parada (`➕ Nueva Parada`)**: Formulario modal para añadir una nueva parada en cualquier día indicando Día, Hora inicio, Hora fin, Título, Nombre en japonés y Notas.
   - **Editar Parada (`✏️ Editar`)**: Botón directo en cada tarjeta del itinerario (y en cada tramo de transporte) que abre el editor con los datos actuales para modificar horario, título, notas o día.
   - **Eliminar Parada (`🗑️ Eliminar`)**: Botón directo en cada tarjeta para borrar esa parada o tramo de transporte al instante.
   - **Eliminar Día Completo o Vaciar Plan (`🗑️ Eliminar Día` / `🗑️ Vaciar Plan`)**: Acción directa para borrar todas las paradas de un día concreto o vaciar por completo el plan de viaje.

### RF-04.2: Integración con Fotos por Actividad y Día
1. En cada actividad o parada del itinerario, el usuario puede adjuntar **múltiples fotografías** (desde cámara o selector de galería):
   - **Fotos de referencia previas**: Capturas de la fachada del local, plato que se quiere pedir, mapa de salida de estación o código QR.
   - **Fotos del recuerdo**: Fotos tomadas al visitar el lugar.
2. Todas las fotos se copian y comprimen de forma óptima en el almacenamiento privado interno (`Context.filesDir/planner_photos/`) para cargarse instantáneamente sin conexión a internet.
3. Vista de miniaturas en la tarjeta del horario y visor a pantalla completa con zoom al tocarlas.

### RF-04.3: Integración Dual con Google Maps (SDK Embebido + Deep Links / Offline)
1. **Datos de Ubicación Persistidos Offline (Punto 1)**:
   - Cada actividad permite guardar:
     - Nombre del lugar (en español/inglés y nombre/dirección en japonés para mostrar a taxistas o buscar rápido).
     - Coordenadas geográficas (`latitude`, `longitude`).
     - URL compartida de Google Maps (`https://maps.app.goo.gl/...` o `https://www.google.com/maps/...`), con extractor automático de coordenadas y nombre cuando sea posible.
   - **Acciones rápidas hacia Google Maps App**:
     - Botón **"Abrir en Google Maps"** (`geo:lat,lng?q=...` o URL directa) para ver la ficha del lugar.
     - Botón **"Cómo llegar (Ruta)"** (`google.navigation:q=...` o ruta en transporte público) desde la ubicación actual o desde la parada anterior del itinerario.
2. **Mapa Interactivo Embebido en el Planificador (Punto 2)**:
   - La pantalla del día incluye un **Mapa Interactivo embebido (`Maps Compose`)** que muestra:
     - Marcadores numerados (`1`, `2`, `3`...) para cada parada del día según su orden horario (sin líneas conectoras para mantener la claridad visual).
     - Al pulsar un marcador del mapa, se resalta la tarjeta del horario correspondiente (con su hora, notas y fotos).
   - **Resiliencia Offline del Mapa**:
     - Aprovecha el cacheo automático de teselas del SDK y, si el dispositivo está completamente sin red en una zona no cacheada, mantiene funcional un modo de esquema vectorial/coordenadas offline y permite lanzar la app nativa de Google Maps (donde el usuario puede tener descargados los mapas offline de Tokio/Kioto).

### RF-04.4: Sincronización / Exportación con Calendario del Sistema y Google Calendar
1. El calendario interno de la app funciona **100% offline** sin depender de cuentas externas.
2. Adicionalmente, el usuario dispone de integración opcional con el calendario del dispositivo (`Android CalendarContract` / Intent de Google Calendar):
   - **Exportar evento individual o viaje completo** al Calendario del sistema / Google Calendar (fijando explícitamente la zona horaria del evento en `Asia/Tokyo` para que las horas no se desconfiguren al crearlo desde el país de origen).
   - Opción de configurar recordatorios/notificaciones locales antes de una reserva importante (ej. 30 min antes de salida de Shinkansen o reserva de restaurante).

### RF-04.5: Vinculación con Documentos y Guías (`SPEC-03`)
1. Una actividad del planificador puede enlazar uno o varios documentos del repositorio (ej. vincular la actividad *"Check-in Hotel Shinjuku 15:00"* con el PDF de la reserva del hotel, o vincular *"Visita a Fushimi Inari 08:30"* con la Guía de Kioto).
2. Desde la propia tarjeta del horario se puede abrir el PDF/imagen o guía vinculada con un solo toque.

### RF-04.6: Exportación e Importación de Planes de Viaje y Transporte en Formato Legible (`JSON` Abierto)
1. El usuario podrá **exportar e importar** sus planes completos de viaje en un **formato JSON legible por humanos (`pretty-printed`, UTF-8)** con un esquema documentado (`schemaVersion: 1`):
   - Incluye **Itinerarios diarios, calendarios y horarios (`ItineraryStopItem`)** con sus coordenadas, nombres en japonés y notas.
   - Incluye **Planes de Transporte Multimodal (`TransportSegmentItem` de `SPEC-05`: vuelos, trenes Shinkansen, metros y autobuses)**.
   - Incluye **Guías de Viaje personalizadas (`TravelGuideArticle` de `SPEC-03`)**.
2. **Casos de uso de Preparación Externa**:
   - Permite preparar el viaje cómodamente desde un ordenador, editor de código, hoja de cálculo, otra aplicación externa o pedirle a un LLM externo que genere el archivo `.json` siguiendo el esquema, e importarlo en la app con un toque (mediante archivo `.json` o portapapeles).
   - Permite elegir entre **Fusionar (`Merge`)** con el plan actual o **Reemplazar (`Replace`)** el plan existente, validando previamente la estructura JSON sin corromper datos locales.

### RF-04.7: Selectores de Fecha Interactivos Nativos (`AppDatePickerField`)
1. **Eliminación de Cajas de Texto de Fecha**:
   - La fecha de inicio del plan de viaje (`startDateIso`) y la fecha de las paradas (`dateIso`) deben seleccionarse mediante un selector interactivo nativo de Material 3 (`DatePickerDialog`), eliminando la introducción manual de texto en formato `YYYY-MM-DD`.
2. **Formato Amigable y Localizado**:
   - Muestra la fecha seleccionada en formato amigable (ej. *"11 nov 2026"*) y el formato ISO estándar (`YYYY-MM-DD`), abriendo el calendario al pulsar el campo.

### RF-04.8: Selectores de Hora Interactivos Nativos (`AppTimePickerField`)
1. **Eliminación de Cajas de Texto Libre para Horas (`HH:mm`)**:
   - La hora de inicio (`startTimeText`) y la hora de fin (`endTimeText`) de cada parada o actividad del itinerario deben seleccionarse mediante un selector interactivo visual de Material 3 / Android TimePicker (`AppTimePickerField`).
   - El selector muestra un icono de reloj (`🕒`), abre el diálogo de selección horaria en formato 24 horas y formatea automáticamente la hora en `HH:mm` sin requerir que el usuario teclee dos puntos ni cifras en teclado alfanumérico.
2. **Soporte de Campos Opcionales y Borrado**:
   - Permite borrar o desmarcar la hora cuando sea opcional, manteniendo la coherencia visual con el resto de selectores de la aplicación.

### RF-04.9: Controles Numéricos Especializados y Steppers (`AppNumberStepperField`)
1. **Controles Incrementales / Decrementales (`Stepper`)**:
   - Para valores enteros discretos como el índice del día (`dayText`, ej. Día 1..N) o el total de días de un viaje (`daysText`, ej. 14 días), el formulario proporciona un componente especializado `AppNumberStepperField` con botones rápidos `[-]` y `[+]`.
   - Permite incrementar o decrementar el valor en 1 unidad con un solo toque y validar cotas mínimas (`min >= 1`) y máximas.
2. **Teclado Numérico Directo**:
   - El campo central permite la introducción directa del número desplegando exclusivamente el teclado numérico (`KeyboardType.Number`), filtrando cualquier carácter no numérico.

### RF-04.5: Visualización de Fechas Regionales, Zonas Horarias Locales, ISO UTC y Botones Ergonómicos
1. **Fecha Completa en Formato Regional**:
   - Cada tarjeta del itinerario (`ItineraryStopCard`) muestra la fecha completa respetando el formato regional del dispositivo (`FormatStyle.FULL`, ej. *"Lunes, 15 de marzo de 2027"* o *"Monday, March 15, 2027"*).
2. **Hora con Zona Horaria Local Explícita**:
   - Se muestra la hora de inicio y fin junto con la indicación clara del huso horario local del destino y la ciudad (ej. `10:00 – 12:25 (Múnich · UTC+1 · CET)` o `07:55 – 09:30 (Osaka · UTC+9 · JST)`).
3. **Persistencia ISO UTC**:
   - Toda hora se guarda y calcula en formato estándar ISO UTC (`Instant.toString()`, ej. `2027-03-15T11:25:00Z`).
4. **Botones Compactos y No Invasivos**:
   - Se reemplazan los bloques masivos apilados (`AppButtonRow` de 120dp) por una única fila compacta de botones (`height` 32-36dp) con acceso rápido a `🗺️ Mapa`, `📅 Calendario`, `✏️ Editar` y `🗑️ Eliminar`.

### RF-04.10: Mapa Integrado de Visitas del Día con Localización Actual y Selector de Capas
1. **Alternador de Vista en el Planificador**:
   - La pantalla del planificador permite alternar limpiamente entre la vista de **Horario (`📋 Horario`)** y la vista de **Mapa del Día (`🗺️ Mapa del Día`)**.
2. **Representación de Paradas del Día en el Mapa (Sin Líneas Conectoras)**:
   - Muestra todas las paradas del día seleccionado mediante **marcadores circulares numerados (`1`, `2`, `3`...)** según su orden cronológico de visita, sin trazar líneas rectas entre puntos para mantener la limpieza visual sobre el callejero.
   - Cada marcador luce el color temático de su categoría (Templo, Comida, Transporte, Hotel, Ocio, Compras) y etiqueta con título y horario.
3. **Selector Configurable de Capas Cartográficas (`MapTileLayerType`)**:
   - El usuario puede seleccionar y alternar en tiempo real entre tres capas de mapas directamente desde los controles del visor:
     - **🗺️ Google Maps (Estándar)** *(Por defecto)*: Cartografía oficial de Google Maps (`mt1.google.com/vt/lyrs=m`) localizada automáticamente al idioma activo de la aplicación (`hl=es` o `hl=en`), mostrando nombres de ciudades, barrios y estaciones en español/romaji junto al japonés.
     - **🛰️ Google Satélite (Híbrido)**: Ortofoto satelital de alta resolución con etiquetas de calles y lugares superpuestas (`mt1.google.com/vt/lyrs=y`).
     - **🌍 OpenStreetMap (OSM)**: Cartografía detallada de senderos peatonales, recintos de templos y parques (`tile.openstreetmap.org`).
   - El cambio de capa se aplica instantáneamente en caliente sin perder el nivel de zoom ni la posición actual del mapa.
4. **Localización Actual y Controles de Navegación del Mapa**:
   - Detección reactiva de la posición GPS/red del usuario (`android.permission.ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`).
   - Marcador distintivo azul pulsante con halo de precisión y etiqueta *"📍 Tú estás aquí"* / *"📍 You are here"*.
   - Columna de controles en la esquina inferior derecha del mapa que integra:
     - Botones de zoom interactivos `[+]` y `[—]`, además de gestos táctiles fluidos de arrastre (`pan`) y pellizco (`pinch-to-zoom`).
     - Botón de chincheta `[📍]` para encuadrar y centrar todas las paradas del día (`fitToMarkers`).
     - Botón con icono vectorial GPS (*My Location*) para centrar la cámara en la ubicación en tiempo real del viajero (sustituyendo el emoji de diana `🎯`).
5. **Resiliencia Offline**:
   - El contenedor del mapa dispone de una cuadrícula vectorial milimétrica en CSS como respaldo visual inmediato sin red y cachea las teselas visitadas.
   - Se ofrece un botón directo para **"🗺️ Ruta en Maps"** como acceso externo a Google Maps nativo.

### RF-04.11: Ficha de Detalle de Parada con Guías y Billetes Relacionados
1. **Apertura de Detalle al Tocar un Marcador**:
   - Al pulsar cualquier marcador numerado en el mapa (o seleccionarlo en el carrusel inferior de paradas), se resalta el punto y se despliega una hoja modal inferior de **Detalle Completo de la Parada**.
2. **Contenido de la Ficha de Detalle**:
   - **Cabecera**: Número de parada, título, hora de inicio y fin (`HH:mm`), duración estimada y distintivo de categoría.
   - **Nombre y Dirección en Japonés (`placeNameJapanese`)**: Presentado en gran tamaño con botón de copiado rápido al portapapeles para enseñar a taxistas o transeúntes.
   - **Notas y Recomendaciones**: Texto descriptivo, consejos de visita y observaciones.
   - **Guía de Viaje Relacionada (`TravelGuideArticle`)**:
     - Vinculación inteligente automática: el sistema consulta las guías descargadas en la Bóveda de Documentos que coincidan por ciudad (ej. Kioto, Tokio, Osaka, Nara), por etiquetas (`tags`) o por términos del título.
     - Si hay coincidencia, muestra la tarjeta de la guía con botón directo **"📖 Leer Guía"**.
   - **Billetes y Pases Relacionados (`TravelDocumentItem` y `TransportSegmentItem`)**:
     - Vinculación inteligente automática: busca billetes de tren, vuelos, reservas de hotel o entradas de museos asociadas a la fecha o ciudad del día.
     - Si el billete dispone de código QR o código de barras, muestra el botón **"📱 Ver QR / Billete"** que abre directamente el código interactivo con brillo al 100%.
   - **Acciones Rápidas**:
     - **"🗺️ Cómo Llegar (Google Maps)"**: Abre la navegación exacta hacia las coordenadas `(lat, lng)` de la parada.
     - **"✏️ Editar Parada"**: Permite editar los datos de la visita directamente desde el mapa sin perder el contexto.

---

## 3. Criterios de Aceptación (Gherkin / BDD)

### Escenario 1: Crear itinerario diario con horarios, fotos y ubicación
- **Dado** que el usuario crea el viaje *"Japón 2026"* del 10 al 24 de noviembre
- **Cuando** añade en el *Día 1* la actividad *"Senso-ji Temple"* de `09:00` a `11:00`, adjunta 2 fotos desde su teléfono y guarda las coordenadas `35.7147, 139.7966` (o enlace de Google Maps)
- **Entonces** la actividad aparece ordenada en la línea temporal del Día 1 con sus miniaturas de fotos, se dibuja el marcador `#1` en el mapa embebido del día, y todo permanece accesible en Modo Avión.

### Escenario 2: Navegación con mapa embebido y apertura en Google Maps
- **Dado** que el *Día 1* tiene 4 paradas con coordenadas configuradas
- **Cuando** el usuario abre la vista de Mapa del Día 1
- **Entonces** visualiza los 4 marcadores numerados unidos en orden horario y, al pulsar *"Cómo llegar"* en la parada `#2`, se lanza directamente la ruta hacia esa ubicación en la aplicación Google Maps.

### Escenario 3: Exportación de horarios con zona horaria de Japón (`Asia/Tokyo`)
- **Dado** que el usuario está planificando su viaje desde España (`Europe/Madrid`) y programa una reserva de cena a las `19:30` (hora de Japón)
- **Cuando** pulsa *"Sincronizar / Añadir al Calendario"*
- **Entonces** el evento se registra con el huso horario `Asia/Tokyo` para que al aterrizar en Japón su calendario muestre exactamente las `19:30`.

### Escenario 4: Exportar e importar un plan completo (Itinerario + Vuelos/Trenes/Metros/Buses) en JSON legible
- **Dado** que el usuario prepara un archivo `japan-trip-plan.json` en una herramienta externa con días, horarios, coordenadas y tramos de vuelo/Shinkansen
- **Cuando** importa el JSON en la aplicación
- **Entonces** el Planificador de Viaje y el Planificador de Transporte cargan y persisten automáticamente todas las paradas y trayectos para su uso 100% offline.

### Escenario 5: Integración de la previsión meteorológica de Open-Meteo (`SPEC-07`) en cada día del itinerario
- **Dado** que existe previsión meteorológica de Open-Meteo para la fecha y ciudad del día seleccionado
- **Cuando** el usuario visualiza el día en el Planificador de Viaje
- **Entonces** ve la tarjeta climática del día (temperatura máx/mín, probabilidad de lluvia, recomendación de paraguas e idoneidad Exterior vs. Interior) y puede optimizar las paradas con IA según el clima.

### Escenario 6: Selección de fecha de inicio del viaje mediante selector visual
- **Dado** que el usuario abre el modal para crear o editar un plan de viaje
- **Cuando** pulsa sobre el selector de fecha de inicio
- **Entonces** se despliega el calendario táctil de Material 3 (`DatePickerDialog`) permitiendo seleccionar el día deseado sin teclear cadenas de texto `YYYY-MM-DD`.

### Escenario 7: Selección de horas de actividad con selector interactivo
- **Dado** que el usuario añade o edita una parada en el itinerario
- **Cuando** pulsa sobre el campo de "Inicio" o "Fin"
- **Entonces** se despliega el selector visual de horas (`AppTimePickerField`), permitiendo fijar horas y minutos con un toque táctil y formateando la hora en `HH:mm`.

### Escenario 8: Ajuste de días con control incremental (`Stepper`)
- **Dado** que el usuario edita el número de días de un plan o el día asignado a una parada
- **Cuando** pulsa sobre los botones `[-]` o `[+]`
- **Entonces** el valor se actualiza inmediatamente respetando los límites mínimos, o bien permite introducir dígitos directamente desplegando el teclado numérico.

### Escenario 9: Mapa interactivo integrado del día con ubicación actual y controles de encuadre
- **Dado** que el usuario se encuentra en el Día 1 con 4 paradas programadas
- **Cuando** pulsa la pestaña "🗺️ Mapa del Día"
- **Entonces** el mapa integrado muestra los marcadores numerados `1`, `2`, `3`, `4` en sus coordenadas exactas sin líneas conectoras, dibuja la posición GPS actual del usuario como un marcador azul interactivo y ofrece en la esquina inferior derecha los controles `[+]`, `[—]`, `[📍]` (`fitToMarkers`) y el icono vectorial GPS de localización.

### Escenario 10: Detalle completo de parada al tocar el mapa con guías y billetes vinculados
- **Dado** que el usuario visualiza el mapa del día y existen guías de Kioto y billetes de Shinkansen/Renfe para esa jornada
- **Cuando** toca el marcador `#1` en el mapa
- **Entonces** se despliega la ficha de detalle con su nombre en japonés, notas, acceso a la guía de Kioto relacionada y botón para abrir el billete digital con código QR a pantalla completa.
