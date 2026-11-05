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

### RF-PKPASS.5: Renderizado y Visualización Offline del Código QR / Aztec / Barcode
- Al parsear el pase, se extraen `barcodeMessage`, `barcodeFormat` (`PKBarcodeFormatQR`, `PKBarcodeFormatAztec`, `PKBarcodeFormatPDF417`, `PKBarcodeFormatCode128`) y `barcodeAltText`.
- La aplicación genera y renderiza un `Bitmap` de alta nitidez 100% offline sin conexión a internet ni servicios externos.
- En las tarjetas de tramos de transporte y en la lista de billetes/documentos, se muestra un botón interactivo: `📱 Ver Código QR / Billete`.
- Al pulsar, se despliega un diálogo modal en pantalla completa/destacado mostrando:
  - Cabecera con operador, número de servicio (ej. "AVE 05181", "LH 1807"), ruta, coche y asiento.
  - Imagen del código QR o código de barras sobre fondo blanco de alto contraste para facilitar la lectura por tornos y lectores ópticos.
  - Texto alternativo legible (localizador / número de billete).

### RF-PKPASS.6: Ordenación Cronológica Automática por Fecha del Evento
- Al importar o modificar un billete `.pkpass`, el nuevo tramo no se añade ciegamente al final, sino que se posiciona de forma estrictamente cronológica comparando `departureTimeIso` (`YYYY-MM-DDTHH:mm`).
- El sistema recalcula y reasigna los valores de `order = 1..N` para todos los tramos de transporte del viaje, garantizando que el viajero visualice siempre su cronología en orden temporal real.

### RF-PKPASS.7: Detección Inteligente de Duplicados (Sustituir o Duplicar)
- Antes de insertar el billete, el sistema comprueba si ya existe un tramo coincidente en el plan activo:
  - Coincidencia exacta por localizador/reserva (`bookingCode == existing.bookingCode`).
  - O coincidencia por servicio y horario (`lineOrNumber == existing.lineOrNumber` y `departureTimeIso == existing.departureTimeIso`).
- Si se detecta una coincidencia previa, la app detiene la inserción directa y muestra un diálogo de resolución al usuario:
  - **Sustituir**: Actualiza el tramo existente con los nuevos datos (coche, asiento, QR actualizado).
  - **Duplicar / Mantener ambos**: Inserta el nuevo billete como un tramo adicional independiente.
  - **Cancelar**: Descarta la importación sin alterar el estado actual.

### RF-PKPASS.8: Vinculación Bidireccional entre Transporte y Billetes/Documentos
- El tramo de transporte conserva una referencia `linkedDocumentId` hacia el billete en la Bóveda de Documentos.
- El documento en la Bóveda conserva una referencia `linkedTransportSegmentId` hacia el tramo de transporte.
- Desde la tarjeta de transporte se puede saltar al documento y viceversa.
