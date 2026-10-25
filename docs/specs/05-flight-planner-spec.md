# SPEC-05: Herramienta de Planificador de Vuelos (Facturación, Embarque, Salida, Escalas y Llegada)

- **ID**: `SPEC-05`
- **Estado**: `Aprobado para Análisis/Implementación`
- **Versión Objetivo**: `0.0.1-beta.X` -> `0.0.1`

---

## 1. Objetivo

Proporcionar al viajero una herramienta especializada y **100% offline** para planificar, visualizar y gestionar sus **vuelos de ida, vuelta y trayectos internos** hacia/desde Japón (incluyendo vuelos con una o múltiples escalas), controlando con precisión cada hito crítico en su zona horaria real:
- **Apertura y límite de Facturación (`Check-in` online y mostrador de equipaje)**.
- **Hora de Embarque (`Boarding time`) y cierre de puerta**.
- **Hora de Salida (`Departure`)** en el huso horario del aeropuerto de origen.
- **Escalas (`Layovers / Stopovers`)**: duración exacta del tránsito, cambio de terminal/puerta y tiempo disponible entre aterrizaje y siguiente embarque.
- **Hora de Llegada (`Arrival`)** en el huso horario del aeropuerto de destino (con indicador `+1 día` / `+2 días` por cruce de husos horarios hacia Japón).

---

## 2. Requisitos Funcionales (RF)

### RF-05.1: Gestión de Viajes Aéreos y Tramos (`Flight Journey & Segments`)
1. Un vuelo completo (`FlightJourney`, ej. *"Ida a Japón: Madrid -> Doha -> Tokio Haneda"* o *"Vuelta: Osaka Kansai -> Estambul -> Madrid"*) se compone de **uno o varios tramos consecutivos (`FlightSegment`)**.
2. En cada tramo (`FlightSegment`) el usuario puede registrar y consultar:
   - **Aerolínea y Número de Vuelo** (ej. `Iberia IB6801`, `Qatar Airways QR806`, `ANA NH211`).
   - **Código de Reserva (`PNR` / Localizador)** copiable con un toque.
   - **Aeropuerto de Salida** (Código IATA ej. `MAD`, nombre de ciudad, **Zona Horaria `ZoneId`**, **Terminal** y **Mostrador de Facturación**).
   - **Aeropuerto de Llegada** (Código IATA ej. `HND` / `NRT` / `KIX`, nombre de ciudad, **Zona Horaria `ZoneId`**, **Terminal** y **Cinta de Equipaje**).
   - **Puerta de Embarque (`Gate`)**, **Grupo/Zona de Embarque** y **Asiento (`Seat`, ej. `24A`)** + Franquicia de equipaje (mano / facturado).

### RF-05.2: Línea Temporal del Vuelo (Facturación -> Embarque -> Salida -> Escala -> Llegada)
1. Por cada tramo aéreo se gestionan y calculan las siguientes horas clave:
   - **Apertura de Check-in Online** (configurable, por defecto `-24h` o `-48h` antes de la salida).
   - **Hora recomendada en Aeropuerto / Inicio de Facturación en mostrador** (ej. `-3h` en vuelos internacionales) y **Hora límite de Facturación / Cierre de entrega de equipaje (`Bag Drop Deadline`)**.
   - **Hora de Embarque (`Boarding Time`)** y **Cierre de Puerta (`Gate Closure`)**.
   - **Hora de Salida Programada (`Departure Time`)** expresada en la zona horaria del aeropuerto de salida (con equivalencia simultánea a la hora del usuario/Japón).
   - **Hora de Llegada Programada (`Arrival Time`)** expresada en la zona horaria del aeropuerto de destino (calculando automáticamente el indicador `+1 día` o `-1 día`).
2. **Duración Real de Vuelo Calculada con Zonas Horarias (`java.time.ZonedDateTime`)**:
   - Dado que volar entre Europa/América y Japón implica hasta `+7h`/`+14h` de diferencia horaria, la app calculará automáticamente la **duración real de vuelo en el aire** convirtiendo el `ZonedDateTime` de salida (en zona horaria de origen) y el `ZonedDateTime` de llegada (en zona horaria de destino, ej. `Asia/Tokyo`).

### RF-05.3: Cálculo y Asistente de Escalas (`Layover Calculator`)
1. Cuando un viaje tiene 2 o más tramos consecutivos (ej. `Tramo 1: MAD -> DOH` y `Tramo 2: DOH -> HND`), la herramienta inserta automáticamente una **Tarjeta de Escala (`Layover Card`)** entre ambos tramos que muestra:
   - **Aeropuerto de escala** y su zona horaria local.
   - **Tiempo total de escala** (desde el aterrizaje del Tramo 1 hasta la salida del Tramo 2).
   - **Tiempo efectivo hasta el siguiente Embarque** (desde el aterrizaje del Tramo 1 hasta la **Hora de Embarque** del Tramo 2).
   - **Cambio de Terminal** (ej. `Terminal 1 ➔ Terminal 3`) y notas de conexión.
   - **Alerta visual de escala ajustada**: Si el tiempo entre la llegada y el siguiente embarque es inferior a `60 minutos` (o `90 min` en conexión internacional con cambio de terminal), se destaca con un aviso visual en color ámbar/rojo (`Akabeni`).

### RF-05.4: Integración con Documentos (`SPEC-03`), Planificador (`SPEC-04`) y Calendario
1. **Vínculo con Tarjetas de Embarque y Billetes PDF (`SPEC-03`)**:
   - Cada tramo permite adjuntar o enlazar directamente la **Tarjeta de Embarque (PDF/Imagen/QR)** o el billete electrónico guardado en el Repositorio de Documentos para abrirlo con un toque en la cola de facturación o puerta de embarque.
2. **Integración con el Planificador de Viaje (`SPEC-04`) y Google Maps**:
   - Los vuelos de llegada/salida se reflejan automáticamente en los días correspondientes del Planificador de Viaje (`Timeline`) y permiten abrir la ubicación de la terminal del aeropuerto en **Google Maps**.
3. **Recordatorios Locales Offline y Exportación a Calendario**:
   - Opción de activar **alarmas/notificaciones locales offline** para:
     - *Apertura de Check-in Online*.
     - *Hora de ir al mostrador de Facturación*.
     - *Inicio de Embarque*.
   - Exportación de los tramos al **Calendario del sistema / Google Calendar** respetando las zonas horarias de cada aeropuerto.

---

## 3. Criterios de Aceptación (Gherkin / BDD)

### Escenario 1: Cálculo exacto de duración de vuelo y escala entre zonas horarias distintas
- **Dado** que el usuario registra un vuelo de ida con 2 tramos:
  - *Tramo 1*: `MAD (Europe/Madrid)` salida `10 de nov 15:30` -> `DOH (Asia/Qatar)` llegada `10 de nov 23:45`
  - *Tramo 2*: `DOH (Asia/Qatar)` embarque `11 de nov 01:15`, salida `11 de nov 02:15` -> `HND (Asia/Tokyo)` llegada `11 de nov 17:55`
- **Cuando** visualiza el itinerario aéreo en modo offline
- **Entonces** la app calcula la duración real del Tramo 1 (`6h 15m`), muestra una **Escala en Doha de `2h 30m` totales (`1h 30m` hasta el embarque del Tramo 2)**, y calcula la duración del Tramo 2 (`9h 40m`) indicando llegada `+1 día` en hora de Japón.

### Escenario 2: Consulta rápida de facturación, puerta de embarque y tarjeta QR en el aeropuerto sin red
- **Dado** que el usuario ha configurado la hora de facturación (`12:30`), hora de embarque (`14:45`), Terminal `4S`, Puerta `S22`, Asiento `18K` y ha vinculado el PDF/imagen de su tarjeta de embarque
- **Cuando** abre la tarjeta del vuelo en el aeropuerto en Modo Avión
- **Entonces** ve el cronograma completo paso a paso (*Facturación -> Embarque -> Salida -> Llegada*) y al pulsar *"Ver Tarjeta de Embarque"* se abre instantáneamente el documento offline a pantalla completa.
