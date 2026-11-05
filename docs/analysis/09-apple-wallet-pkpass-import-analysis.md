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

## 3. Estrategia de Testing (TDD)
- Test unitario con ZIP en memoria que contiene `pass.json` de un billete de tren AVE (Renfe).
- Test unitario con ZIP en memoria que contiene `pass.json` de un billete de vuelo (Iberia / Lufthansa).
- Verificación de extracción de horas, fechas, coche, asiento, barcode y conversión a tramo de transporte.
