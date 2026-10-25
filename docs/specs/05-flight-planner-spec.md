# SPEC-05: Herramienta de Planificador de Transporte (Vuelos, Trenes/Shinkansen, Metros y Autobuses)

- **ID**: `SPEC-05`
- **Estado**: `Aprobado para Análisis/Implementación`
- **Versión Objetivo**: `0.0.1-beta.X` -> `0.0.1`

---

## 1. Objetivo

Proporcionar al viajero una herramienta integral, unificada y **100% offline** para planificar, visualizar y encadenar todos sus desplazamientos antes y durante su viaje a Japón:
1. **Planificador de Vuelos (`FLIGHT`)**: Ida, vuelta y vuelos internos con control de **apertura y límite de facturación (`check-in`), hora de embarque (`boarding`), salida, escalas (`layovers`) y llegada** entre husos horarios.
2. **Planificador de Trenes y Shinkansen (`TRAIN_SHINKANSEN` / `LIMITED_EXPRESS` / `LOCAL_TRAIN`)**: Trayectos ferroviarios por Japón con línea/nombre del tren (ej. *Nozomi*, *Hikari*, *Haruka*, *Narita Express N'EX*, *Skyliner*), andén (`Platform / Track`), coche (`Car`), asiento (`Seat` — ventana Monte Fuji / pasillo / reserva de maletas grandes), transbordos (`Transfers`) y si está cubierto por **JR Pass / Pase Regional** o tarjeta IC (*Suica / Pasmo / Icoca*).
3. **Planificador de Metro y Suburbano (`SUBWAY_METRO`)**: Desplazamientos urbanos (ej. *Tokyo Metro Ginza Line*, *Toei Oedo Line*, *Osaka Metro Midosuji*) con color oficial de línea, código de estación (ej. `G-09` -> `G-16`), **coche recomendado para el transbordo/salida** y **Número de Salida de Estación (`Exit`, ej. `Salida A4` o `Hachiko Exit`)**, crítico en las macro-estaciones japonesas.
4. **Planificador de Autobuses (`HIGHWAY_BUS` / `LOCAL_BUS` / `AIRPORT_LIMOUSINE`)**: Autobuses interurbanos (ej. *Highway Bus a Kawaguchiko / Shirakawa-go*), autobuses urbanos de Kioto o *Airport Limousine Bus*, con terminal/parada (`Bus Stop / Bay`), número de línea, hora de embarque, compañía y si requiere reserva previa o pago al subir/bajar.

Permite además **encadenar tramos multimodales** en un mismo trayecto (por ejemplo: *Vuelo aterrizaje en Narita T1 ➔ Tren Narita Express N'EX a Tokyo Station ➔ Metro Marunouchi Line hasta el hotel*), calculando automáticamente el tiempo de cada escala/transbordo.

---

## 2. Requisitos Funcionales (RF)

### RF-05.1: Gestión de Trayectos Multimodales y Tramos (`TransportJourney & TransportSegment`)
1. Un trayecto (`TransportJourney`, ej. *"Llegada a Japón: Madrid ➔ Doha ➔ Tokio Narita ➔ Hotel Shinjuku"* o *"Excursión Tokio ➔ Kioto ➔ Arashiyama"*) se compone de **uno o varios tramos consecutivos (`TransportSegment`)** de cualquier medio de transporte:
   - ✈️ **Vuelo (`FLIGHT`)**
   - 🚅 **Shinkansen / Tren Expreso (`TRAIN_SHINKANSEN` / `TRAIN_EXPRESS`)**
   - 🚃 **Tren Local / JR (`TRAIN_LOCAL`)**
   - 🚇 **Metro (`SUBWAY_METRO`)**
   - 🚌 **Autobús (`BUS_HIGHWAY` / `BUS_LOCAL` / `BUS_AIRPORT`)**
2. **Atributos específicos para Vuelos (`FLIGHT`)**:
   - Aerolínea y Número de Vuelo (`IB6801`, `QR806`, `NH211`), Localizador (`PNR`).
   - Aeropuerto de Salida y Llegada (Código IATA `MAD`, `HND`, `NRT`, `KIX`, ciudad, **Zona Horaria `ZoneId`**, **Terminal**).
   - **Mostrador de Facturación**, **Puerta de Embarque (`Gate`)**, **Grupo**, **Asiento (`Seat`)** y **Cinta de Equipaje**.
3. **Atributos específicos para Trenes y Shinkansen (`TRAIN_*`)**:
   - Operador y Línea/Servicio (ej. `JR Central · Tokaido Shinkansen Nozomi #21`, `JR Narita Express #18`).
   - Estación de Salida y Llegada (nombre en español/inglés y japonés para paneles luminosos, ej. `東京 (Tokyo)` ➔ `京都 (Kyoto)`).
   - **Andén de salida y llegada (`Track / Platform`, ej. `Andén 16`)**.
   - **Tipo de Asiento**: *Reservado (`Reserved Seat - 指定席`)*, *No Reservado (`Non-Reserved - 自由席`)* o *Green Car*, indicando **Número de Coche (`Car #7`)**, **Asiento (`Seat 12E`)** y si incluye **Reserva de Equipaje Sobredimensionado (`Oversized Baggage Area`)**.
   - **Método de Pago / Pase**: Cubierto por `JR Pass`, `Regional Pass`, `Billete Individual` o `IC Card (Suica/Pasmo/Icoca)` + coste en `JPY`.
4. **Atributos específicos para Metro (`SUBWAY_METRO`)**:
   - Línea de Metro y Color identificativo (ej. `Tokyo Metro Ginza Line (Naranja)`).
   - Estación de origen y destino con su **código alfanumérico oficial** (ej. `Asakusa G19` ➔ `Shibuya G01`) y sentido/cabecera de la línea (`Bound for Shibuya`).
   - **Andén (`Platform`)**, **Vagón recomendado (`Best Car for Transfer/Exit`, ej. `Vagón 4`)** y **Salida de Estación exacta (`Station Exit`, ej. `Salida 8 / Exit B2`)** para no perderse dentro de estaciones como Shinjuku, Shibuya o Tokyo.
5. **Atributos específicos para Autobuses (`BUS_*`)**:
   - Compañía y Línea/Número de autobús (ej. `Keio Highway Bus`, `Kyoto City Bus #206`, `Airport Limousine Bus`).
   - Estación/Parada de subida y bajada con **Dársena / Parada (`Bus Terminal / Bay #4`)**.
   - Código de reserva (si es *Highway Bus*), asiento asignado y modalidad de acceso (subir por puerta trasera / pagar con Suica o efectivo exacto al bajar).

### RF-05.2: Línea Temporal por Tramo (Facturación / Acceso -> Embarque -> Salida -> Escala/Transbordo -> Llegada)
1. **Para tramos de Vuelo (`FLIGHT`)**:
   - **Apertura de Check-in Online** (`-24h` / `-48h`).
   - **Hora de Facturación en mostrador** y **Hora límite de entrega de equipaje (`Bag Drop Deadline`)**.
   - **Hora de Embarque (`Boarding Time`)** y **Cierre de Puerta**.
   - **Hora de Salida (`Departure`)** en el huso horario de origen y **Hora de Llegada (`Arrival`)** en el huso horario de destino (`+1`/`+2` días), calculando la **duración real en el aire** con `java.time.ZonedDateTime`.
2. **Para tramos de Tren, Metro y Autobús (`TRAIN_*`, `SUBWAY_METRO`, `BUS_*`)**:
   - **Hora recomendada en Andén / Parada (`Platform / Bus Bay Arrival`)**.
   - **Hora exacta de Salida (`Departure Time`)** (puntualidad japonesa al minuto en `Asia/Tokyo`).
   - **Número de paradas intermedias** (opcional) y **Hora exacta de Llegada (`Arrival Time`)** con cálculo automático de duración del trayecto.

### RF-05.3: Asistente de Escalas Aéreas y Transbordos de Tren/Metro/Bus (`Layover & Transfer Calculator`)
1. Entre cualesquiera dos tramos consecutivos (sean dos vuelos, un vuelo conectado con el tren del aeropuerto, o un transbordo entre Shinkansen, Metro y Autobús), la herramienta inserta automáticamente una **Tarjeta de Escala / Transbordo (`Layover & Transfer Card`)** que muestra:
   - **Tiempo total de conexión** (desde la llegada del tramo anterior hasta la salida del siguiente tramo).
   - **Tiempo efectivo hasta el siguiente Embarque** (en vuelos) o **margen de cambio de andén/dársena** (en trenes, metros y autobuses).
   - **Instrucciones visuales de cambio**:
     - En vuelos: ej. `Terminal 1 ➔ Terminal 3`.
     - En tren/metro/bus: ej. `Andén 14 (Shinkansen) ➔ Transfer Gate JR ➔ Andén 5 (Yamanote Line)` o `Salida Este ➔ Dársena Bus #2`.
   - **Alertas de conexión ajustada (`Akabeni` Warning)**:
     - En escalas aéreas: alerta si el margen hasta el embarque es `< 60 min` (o `< 90 min` con cambio de terminal/inmigración).
     - En transbordos de tren/metro/autobús: alerta si el margen de cambio de andén en grandes estaciones es `< 7 min` (o `< 15 min` entre Shinkansen y líneas convencionales/metro).

### RF-05.4: Integración con Documentos (`SPEC-03`), Planificador (`SPEC-04`), Exportación/Importación JSON y Calendario
1. **Vínculo directo con Billetes, QR y Reservas (`SPEC-03`)**:
   - Cada tramo permite adjuntar o enlazar su **Tarjeta de Embarque**, **Billete de Shinkansen / QR de SmartEX**, **Reserva de Highway Bus** o captura de ruta guardada en el Repositorio de Documentos para abrirla con un toque sin conexión.
2. **Integración con el Planificador de Viaje (`SPEC-04`) y Google Maps**:
   - Los trayectos de vuelo, tren, metro y autobús se integran en la línea temporal del día (`SPEC-04`) y permiten abrir la estación/aeropuerto/parada o lanzar la ruta de transporte público en **Google Maps**.
3. **Exportación e Importación en JSON Legible (`SPEC-04 RF-04.6`)**:
   - Todos los tramos de **Vuelo, Shinkansen/Tren, Metro y Autobús** forman parte del paquete JSON legible de exportación/importación (`transportSegments`), permitiendo prepararlos desde una app externa o compartirlos entre dispositivos.
4. **Recordatorios Locales Offline y Exportación a Calendario**:
   - Alarmas offline configurables (apertura de check-in, hora de facturación, embarque, o aviso de bajada en la próxima estación de tren/autobús).
   - Exportación al Calendario del sistema / Google Calendar respetando la zona horaria de cada tramo.

### RF-05.5: Sistema de Advertencias Visuales y Notificaciones de Facturación, Embarque y Salida
1. **Umbrales Temporales de Detección de Eventos**:
   - **Facturación / Check-in**:
     - *Aproximándose (`CHECK_IN_APPROACHING`)*: cuando faltan $\le 120$ minutos para la hora de facturación (`checkInTime`).
     - *Abierta / En curso (`CHECK_IN_OPEN`)*: desde la hora de facturación hasta el inicio del embarque.
   - **Embarque / Boarding**:
     - *Aproximándose (`BOARDING_APPROACHING`)*: cuando faltan $\le 45$ minutos para la hora de embarque (`boardingTime`).
     - *¡Embarque en curso! (`BOARDING_NOW`)*: cuando faltan $\le 15$ minutos para el embarque o durante el periodo previo a la salida.
   - **Salida Inminente / Cierre de Puertas (`DEPARTURE_IMMINENT`)**:
     - Cuando faltan $\le 20$ minutos para la hora de salida programada (`departureTimeIso`).
2. **Advertencias Visuales en la Interfaz (UI)**:
   - **Banner de Alerta Superior (`TransportAlertBanner`)**:
     - Presentado de forma prominente en la parte superior de la pantalla de transporte cuando hay alertas activas.
     - Muestra icono identificativo (`✈️`, `🚄`, `⚠️`, `🚨`), título de la advertencia, minutos restantes, terminal/andén, mostrador/puerta y asiento.
     - Botón de descarte temporal y enlace rápido para ver detalles o billete.
   - **Resaltado en la Tarjeta del Tramo**:
     - Borde de advertencia (`Akabeni` / ámbar) e insignia destacada (`[⚠️ Facturación en 45m]` / `[🚨 ¡Embarque en curso!]`).
   - **Campos de Facturación y Embarque en el Editor**:
     - Formulario `TransportSegmentEditorModalSheet` con inputs opcionales para `Hora de Facturación (HH:mm)` y `Hora de Embarque (HH:mm)`.
   - **Botón de Prueba y Simulación Inmediata**:
     - Control accesible para disparar alertas de prueba en cualquier momento y validar tanto la advertencia visual como la notificación en el dispositivo físico.
3. **Notificaciones del Sistema Android (100% Offline)**:
   - **Canal de Notificaciones**: `id = "transport_alerts"`, `name = "Avisos de Transporte y Vuelos"`, con importancia alta (`IMPORTANCE_HIGH`), sonido y vibración.
   - **Permisos**: Declaración y solicitud en tiempo de ejecución de `android.permission.POST_NOTIFICATIONS` para Android 13+ (API 33+).
   - **Notificación Enriquecida**: Con título claro, indicación de puerta/mostrador/andén y apertura directa de la app al pulsar.

### RF-05.6: Creación y Edición de Tramos con Formulario Limpio (`Empty State on Create`)
1. **Inputs Limpios en Alta de Transporte**:
   - Al pulsar el botón `➕ Añadir Transporte`, el formulario modal (`TransportSegmentEditorModalSheet`) debe abrirse con todos los campos de texto e inputs de edición completamente limpios y vacíos (`""`).
   - Queda prohibida la precarga involuntaria de valores de ejemplo (como `"JR Shinkansen"`, `"Nozomi #21"`, `"Tokyo Station"`, `"Kyoto Station"`, `"09:00"`, `"11:15"`, `"Andén 16"`, `"Vagón 7 · Asiento 12E"`, etc.), evitando que el usuario deba borrar manualmente caracteres previos.
2. **Fidelidad en Modo Edición**:
   - Al abrir un tramo existente mediante `✏️ Editar`, el formulario cargará de forma fidedigna los datos específicos de dicho tramo, aislando el estado de edición mediante clave única (`initialSegment.id`) para prevenir fugas de estado en Compose.

---

## 3. Criterios de Aceptación (Gherkin / BDD)

### Escenario 1: Cálculo exacto de duración de vuelo y escala entre zonas horarias distintas
- **Dado** que el usuario registra un vuelo de ida con 2 tramos:
  - *Tramo 1 (Vuelo)*: `MAD (Europe/Madrid)` salida `10 de nov 15:30` -> `DOH (Asia/Qatar)` llegada `10 de nov 23:45`
  - *Tramo 2 (Vuelo)*: `DOH (Asia/Qatar)` embarque `11 de nov 01:15`, salida `11 de nov 02:15` -> `NRT (Asia/Tokyo)` llegada `11 de nov 17:55`
- **Cuando** visualiza el itinerario en modo offline
- **Entonces** la app calcula la duración real del Tramo 1 (`6h 15m`), muestra una **Escala en Doha de `2h 30m` totales (`1h 30m` hasta el embarque del Tramo 2)**, y calcula la duración del Tramo 2 (`9h 40m`) indicando llegada `+1 día` en hora de Japón.

### Escenario 2: Trayecto multimodal (Vuelo + Tren Expreso + Metro + Autobús) con transbordos
- **Dado** que tras aterrizar en `Narita T1` a las `17:55`, el usuario encadena:
  - *Tramo 3 (Tren)*: `Narita Express N'EX #46` salida `19:15` (Andén 1, Coche 6, Asiento 11A) -> `Tokyo Station` llegada `20:14`
  - *Tramo 4 (Metro)*: `Tokyo Metro Marunouchi Line (M)` salida `20:26` (Andén 1, Vagón 3) -> `Shinjuku-gyoemmae (M10)` llegada `20:42` (**Salida 2**)
- **Cuando** consulta su ruta en la app sin conexión a internet
- **Entonces** visualiza los tiempos de enlace en el aeropuerto (`1h 20m` para inmigración y canje de billete), el transbordo en `Tokyo Station` (`12m` del andén N'EX al metro Marunouchi) y la indicación destacada de tomar la **Salida 2** al llegar a destino.

### Escenario 3: Consulta rápida de facturación, andén, asiento y billete QR en modo avión
- **Dado** que el usuario ha vinculado el PDF/QR de su tarjeta de embarque o billete de Shinkansen/Highway Bus al tramo correspondiente
- **Cuando** pulsa *"Ver Billete / QR"* desde la tarjeta del tramo en Modo Avión
- **Entonces** el documento se abre instantáneamente a pantalla completa desde el almacenamiento interno privado.

### Escenario 4: Advertencia visual y notificación del sistema cuando se acerca la hora de facturar
- **Dado** que el usuario tiene un vuelo registrado con hora de facturación en mostrador a las `12:30`
- **Cuando** la hora actual entra en los `120 minutos` previos a la facturación (o se abre el mostrador)
- **Entonces** la app muestra un banner visual superior de advertencia (`CHECK_IN_APPROACHING` / `CHECK_IN_OPEN`), resalta la tarjeta del vuelo y despacha una notificación al sistema Android indicando el mostrador y terminal.

### Escenario 5: Advertencia visual urgente y notificación de aviso al aproximarse el embarque
- **Dado** que el usuario tiene un vuelo con hora de embarque a las `14:40` en la Puerta `S22`
- **Cuando** faltan `45 minutos` o menos para el embarque (o se encuentra en curso a falta de $\le 15$ minutos)
- **Entonces** la app despliega la advertencia visual urgente (`BOARDING_APPROACHING` / `BOARDING_NOW`) en rojo bermellón `Akabeni`, resalta la puerta `S22` y lanza una notificación de alta prioridad con sonido/vibración.

### Escenario 6: Apertura del modal de nuevo transporte con inputs limpios sin texto precargado
- **Dado** que el usuario pulsa «➕ Añadir Transporte» en la pantalla de transportes
- **Cuando** se despliega el formulario modal de alta (`TransportSegmentEditorModalSheet`)
- **Entonces** todos los campos de texto e inputs (compañía/operador, número/tren/vuelo, origen, destino, salida, llegada, facturación, embarque, andén/terminal y asiento) se muestran completamente limpios y vacíos (`""`), permitiendo la entrada directa de datos sin requerir el borrado manual de ejemplos precargados.
