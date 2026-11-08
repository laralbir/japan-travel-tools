# ANALYSIS-16: Análisis Técnico del Generador Nativo de PDF para Itinerarios y Guías

- **ID**: `ANALYSIS-16`
- **Especificación**: [`SPEC-14`](../specs/14-itinerary-pdf-export-spec.md)
- **Capa**: Platform & Domain (`android.graphics.pdf.PdfDocument`, `FileProvider`)
- **Paquete Kotlin**: `com.laralbir.japantraveltools.feature.planner.pdf`

---

## 1. Diseño de Arquitectura del Generador PDF

```
feature.planner.pdf/
├── domain/
│   ├── model/
│   │   ├── PdfExportOptions.kt        # SINGLE_DAY / FULL_TRIP, flags de inclusión
│   │   └── PdfExportResult.kt         # Uri de FileProvider, tamaño, conteo de páginas
│   ├── usecase/
│   │   ├── GenerateDayItineraryPdfUseCase.kt
│   │   └── GenerateFullTripPdfUseCase.kt
│   └── repository/
│       └── ItineraryPdfExporter.kt    # Contrato de generación de documento
├── data/
│   └── AndroidItineraryPdfExporter.kt # Motor nativo basado en PdfDocument y Canvas
└── ui/
    ├── PdfExportDialog.kt             # Diálogo de opciones y progreso de exportación
    └── PdfShareHelper.kt              # Lanza Intent.ACTION_SEND a través del Sharesheet
```

---

## 2. Renderizado con `android.graphics.pdf.PdfDocument`

1. **Dimensiones de Página Estándar A4**:
   - Anchura: `595` puntos.
   - Altura: `842` puntos.
   - Márgenes: `36` puntos (0.5 pulgadas) a cada lado.
2. **Pintura y Tipografía**:
   - Cabecera: `Paint` con color `#C93A40` (Akabeni Torii) para el bloque decorativo superior.
   - Glifos japoneses: Uso de `Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)` con soporte nativo de Android para fuentes CJK (Noto Sans CJK preinstalada en el sistema).
   - Ajuste automático de texto (*Text Wrapping*): Algoritmo de partición de líneas mediante `StaticLayout` o medición manual de anchura con `paint.measureText`.
3. **Flujo de Paginación**:
   - Se mantiene un cursor vertical $Y$.
   - Al dibujar una parada o sección, si $Y + \Delta Y > \text{PageHeight} - \text{BottomMargin}$, se cierra la página actual (`pdfDocument.finishPage(page)`), se inicia una nueva (`pdfDocument.startPage(pageInfo)`), se redibuja la cabecera reducida y se restablece el cursor.

---

## 3. Integración Segura con `FileProvider`

1. El archivo se genera en el directorio seguro de caché privada:
   ```kotlin
   val exportDir = File(context.cacheDir, "exports/pdf").apply { mkdirs() }
   val pdfFile = File(exportDir, "japan-travel-day-$dayIndex.pdf")
   ```
2. La URI compartible se resuelve con:
   ```kotlin
   val contentUri = FileProvider.getUriForFile(
       context,
       "${context.packageName}.fileprovider",
       pdfFile
   )
   ```
3. Se despacha el intent de compartición:
   ```kotlin
   val shareIntent = Intent(Intent.ACTION_SEND).apply {
       type = "application/pdf"
       putExtra(Intent.EXTRA_STREAM, contentUri)
       addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
   }
   context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_pdf_title)))
   ```

---

## 4. Estrategia de Testing (TDD)

1. **`PdfDocumentLayoutCalculatorTest`**:
   - Validar que el cálculo de paginación divide adecuadamente días con 1 a 20 paradas sin solapamiento de textos.
   - Validar formateo de fechas en español e inglés y preservación de cadenas kanji.
2. **`ItineraryPdfExporterTest`**:
   - En test de integración / unitario con `Robolectric` o utilidades de archivo:
     - Generar el PDF en memoria/disco temporal.
     - Validar que el archivo resultante tiene cabecera `%PDF-1.`, tamaño superior a 10 KB y páginas válidas.
