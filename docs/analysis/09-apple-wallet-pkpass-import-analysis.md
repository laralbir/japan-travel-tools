# Análisis Técnico 09: Arquitectura de Importación Apple Wallet (.pkpass)

## 1. Arquitectura de Descompresión y Parseo

```
[InputStream (.pkpass)] ➔ [ZipInputStream]
                                  │
                       Busca "pass.json"
                                  ▼
                        [JSONObject Parse]
                                  │
         ┌────────────────────────┴────────────────────────┐
         ▼                                                 ▼
[ParsedPkPass Model]                             [Raw Barcode / Metadata]
         │                                                 │
         ├────────────────────────┐                        │
         ▼                        ▼                        ▼
[TransportSegmentItem]   [TravelDocumentItem]     [Visual Feedback / UI]
```

## 2. Contratos y Clases Principales

### `ParsedPkPass`
```kotlin
data class ParsedPkPass(
    val organizationName: String,
    val description: String,
    val transitType: String?,
    val originName: String,
    val destinationName: String,
    val departureDateIso: String,
    val departureTime: String,
    val arrivalDateIso: String,
    val arrivalTime: String,
    val operatorName: String,
    val serviceNumber: String,
    val carriage: String?,
    val seat: String?,
    val passengerName: String?,
    val bookingCode: String?,
    val ticketNumber: String?,
    val barcodeMessage: String?,
    val barcodeFormat: String?,
    val latitude: Double? = null,
    val longitude: Double? = null
)
```

### `PkPassParser`
1. `parsePkPass(inputStream: InputStream): Result<ParsedPkPass>`
2. `parsePassJson(jsonString: String): Result<ParsedPkPass>`
3. `toTransportSegment(pass: ParsedPkPass, order: Int): TransportSegmentItem`
4. `toTravelDocument(pass: ParsedPkPass, rawFileBytes: ByteArray? = null): TravelDocumentItem`

## 3. Generación y Renderizado de Códigos de Barras y QR

```
[ParsedPkPass.barcodeMessage & barcodeFormat]
                    │
                    ▼
       [BarcodeBitmapGenerator]
      (ZXing: QRCode / Aztec / PDF417 / Code128)
                    │
                    ▼
      [android.graphics.Bitmap]
                    │
                    ▼
       [PkPassBarcodeViewerDialog]
    (Jetpack Compose Modal con fondo blanco)
```

### Formatos soportados de Apple Wallet y mapeo ZXing:
- `PKBarcodeFormatQR` ➔ `com.google.zxing.BarcodeFormat.QR_CODE`
- `PKBarcodeFormatAztec` ➔ `com.google.zxing.BarcodeFormat.AZTEC`
- `PKBarcodeFormatPDF417` ➔ `com.google.zxing.BarcodeFormat.PDF_417`
- `PKBarcodeFormatCode128` ➔ `com.google.zxing.BarcodeFormat.CODE_128`

## 4. Detección de Duplicados y Ordenación Cronológica

### Algoritmo de Detección de Duplicados
Se considera duplicado un billete importado si respecto a la lista actual de tramos:
1. `existing.stationExitOrBelt.contains(pass.bookingCode)` o `existing.stationExitOrBelt.contains(pass.ticketNumber)` (cuando el localizador no esté vacío).
2. O bien `existing.lineOrNumber == pass.serviceNumber && existing.departureTimeIso == "${pass.departureDateIso}T${pass.departureTime}"`.

Si hay coincidencia, la UI muestra un diálogo con 3 acciones:
- **Sustituir**: Reemplaza el `TransportSegmentItem` y su `TravelDocumentItem` asociado.
- **Duplicar**: Inserta con nuevo ID único.
- **Cancelar**: No realiza modificaciones.

### Algoritmo de Ordenación Cronológica
Al agregar o sustituir un tramo:
```kotlin
val sorted = allSegments.sortedBy { it.departureTimeIso }
val reordered = sorted.mapIndexed { index, item -> item.copy(order = index + 1) }
```
Garantiza que cualquier billete insertado se coloque en su posición temporal real dentro de la cronología del viaje.

## 5. Estrategia de Testing (TDD)
- Test unitario con ZIP en memoria que contiene `pass.json` de un billete de tren AVE (Renfe).
- Test unitario con ZIP en memoria que contiene `pass.json` de un billete de vuelo (Iberia / Lufthansa).
- Test unitario de `BarcodeBitmapGeneratorTest` verificando generación no nula de Bitmaps QR, Aztec y PDF417.
- Test unitario de detección de duplicados y reordenación cronológica automática.
