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
   - Validar que `paginateStops` ajusta la altura disponible de la primera página cuando se incluye el mapa diario (`MAP_CARD_HEIGHT = 160f`).
2. **`PdfMapProjectionTest`**:
   - Validar cálculo de bounding box geográfico y proyección equirrectangular dentro del área de dibujo (`RectF`).
   - Validar preservación de aspecto longitudinal considerando la latitud media de Japón.
   - Validar manejo determinista de casos límite: 0 puntos, 1 punto, puntos colineales o idénticos.
3. **`ItineraryPdfExporterTest`**:
   - En test de integración / unitario con `Robolectric` o utilidades de archivo:
     - Generar el PDF en memoria/disco temporal.
     - Validar que el archivo resultante tiene cabecera `%PDF-1.`, tamaño superior a 10 KB y páginas válidas.

---

## 5. Diseño del Motor Cartográfico Vectorial Nativo en Canvas (`PdfMapRenderer`)

1. **Proyección Equirrectangular Calibrada para Japón**:
   - Dado el centroide $(\phi_0, \lambda_0)$ y la caja envolvente $(\phi_{\min}, \phi_{\max}, \lambda_{\min}, \lambda_{\max})$:
     $$\Delta x_{\text{geo}} = (\lambda - \lambda_0) \cdot \cos(\phi_0) \cdot R_{\text{earth}}$$
     $$\Delta y_{\text{geo}} = (\phi - \phi_0) \cdot R_{\text{earth}}$$
   - Escala $S = \min\left(\frac{W_{\text{plot}}}{\Delta x_{\text{span}}}, \frac{H_{\text{plot}}}{\Delta y_{\text{span}}}\right)$ para garantizar relación de aspecto 1:1 sin deformar la geografía de ciudades como Tokio o Kioto.
   - Mapeo a píxeles/puntos PostScript:
     $$X = X_{\text{center}} + \Delta x_{\text{geo}} \cdot S$$
     $$Y = Y_{\text{center}} - \Delta y_{\text{geo}} \cdot S$$
2. **Capas Gráficas Renderizadas en Secuencia**:
   - **Fondo de Mapa**: Tarjeta blanca con marco `COLOR_BORDER` y cuadrícula suave de coordenadas con meridianos y paralelos discontinuos.
   - **Trayectoria Secuencial de Ruta**: `android.graphics.Path` trazado de $P_1 \to P_2 \to \dots \to P_N$ mediante `DashPathEffect(floatArrayOf(6f, 4f), 0f)` con color `COLOR_AI_IRO` (índigo `#1E3A5F`, grosor 2 pt).
   - **Pines Marcadores**: Círculo de radio 8.5 pt con color `COLOR_AKABENI` (rojo Torii `#C93A40`), borde exterior blanco de 1.5 pt y número secuencial en blanco (1, 2, ...).
   - **Etiquetas de Parada (Callouts)**: Píldoras compactas adyacentes a cada pin con el nombre y hora de la parada, desplazadas para evitar solapamiento.
   - **Brújula / Indicador Norte**: Rosa minimalista con flecha hacia el Norte en la esquina superior derecha.
   - **Pie de Mapa / Escala**: Distancia total acumulada en km y leyenda informativa.
