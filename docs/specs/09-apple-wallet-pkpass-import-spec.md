# Especificación 09: Importación de Billetes y Tarjetas de Embarque Apple Wallet (.pkpass)

## 1. Visión General
Permite a los viajeros importar de forma inmediata y 100% offline sus billetes de tren (Renfe AVE, JR Shinkansen, etc.), vuelos de aerolíneas (Iberia, Lufthansa, Qatar Airways, ANA, JAL) y pases de transporte en formato estándar **Apple Wallet (.pkpass)** directamente a **Japan Travel Tools**.

Al seleccionar un archivo `.pkpass`:
1. La aplicación descomprime el archivo ZIP en memoria sin herramientas externas.
2. Parsea `pass.json` extrayendo el operador, número de servicio/vuelo, estaciones de origen y destino, fechas y horas de salida/llegada, coche, asiento, localizador de reserva y códigos de acceso.
3. Convierte automáticamente el billete en un `TransportSegmentItem` listo para el itinerario de transporte, alertando y sincronizándose con el sistema de notificaciones de embarque y salida.
4. Opcionalmente registra el documento en el **Repositorio Seguro de Billetes y Documentos** (`Context.filesDir`), preservando la privacidad del viajero.

---

## 2. Requisitos Funcionales

### RF-PKPASS.1: Descompresión y Parseo Seguro Offline de `.pkpass`
- Los archivos `.pkpass` son paquetes ZIP que contienen `pass.json`, iconos y manifiestos.
- La aplicación procesa el archivo mediante `java.util.zip.ZipInputStream` directamente desde un `InputStream` de Android (`Uri` de SAF o almacenamiento local).
- Extrae el archivo `pass.json` sin escribir archivos temporales en almacenamiento compartido.

### RF-PKPASS.2: Mapeo de `transitType` a Modos de Transporte de la App
- `PKTransitTypeTrain` ➔ `TransportMode.TRAIN_SHINKANSEN` (o tren alta velocidad).
- `PKTransitTypeAir` ➔ `TransportMode.FLIGHT`.
- `PKTransitTypeBus` ➔ `TransportMode.BUS`.
- `PKTransitTypeBoat` / genérico ➔ `TransportMode.TRAIN_SHINKANSEN` o `SUBWAY_METRO`.

### RF-PKPASS.3: Extracción de Metadatos del Viaje
- **Operador**: `organizationName` (ej. "Renfe", "Iberia", "Lufthansa").
- **Línea / Servicio**: De `auxiliaryFields` o `headerFields` (ej. "AVE 05181", "IB6801", "LH 742").
- **Origen y Destino**: De `primaryFields` (`depart` y `arrive`).
- **Fecha y Horarios**:
  - `relevantDate` (ISO 8601 con zona horaria) o combinación de `headerFields.date` + `primaryFields.depart/arrive`.
  - Construcción de `departureTimeIso` (`YYYY-MM-DDTHH:mm`) y `arrivalTimeIso`.
- **Coche y Asiento**: De `auxiliaryFields` (`carriage`, `seat`, `COCHE`, `ASIENTO`).
- **Reserva / Localizador**: De `secondaryFields` o `backFields` (`bookingNumber`, `ticketNumber`).
- **Código de Barras / QR**: De `barcode` o `barcodes` (`message`, `format`, `altText`).

### RF-PKPASS.4: Interfaz de Usuario y Experiencia del Viajero
- Botón accesible en la pantalla de **Transporte**: `🎫 Importar Billete (.pkpass)`.
- Selector de archivos mediante `ActivityResultContracts.OpenDocument()` o `GetContent()`.
- Confirmación visual de importación con los datos parseados y agregado inmediato al plan activo.
