# SPEC-14: Exportación de Itinerarios y Guías a Documento PDF Compartible 100% Offline

- **ID**: `SPEC-14`
- **Estado**: `Aprobado para Análisis e Implementación`
- **Herramienta**: Herramienta 4 — Planificador de Viaje (`SPEC-04`)
- **Referencias Cruzadas**:
  - [`SPEC-00`](00-core-configuration-spec.md) (Configuración Base, Paleta de Color Japonesa y Offline-First)
  - [`SPEC-04`](04-trip-planner-spec.md) (Planificador de Viaje, Días, Horarios y Paradas)
  - [`SPEC-05`](05-flight-planner-spec.md) (Tramos de Transporte: Shinkansen, Vuelos y Metros)
  - [`ANALYSIS-16`](../analysis/16-itinerary-pdf-export-analysis.md) (Diseño del Generador PDF Nativo con PdfDocument)

---

## 1. Contexto, Motivación y Objetivo

Al viajar en grupo, con pareja o con familiares por Japón, es muy habitual que los acompañantes deseen consultar el plan de la jornada o los horarios de los trenes sin tener necesariamente instalada la aplicación (o disponiendo de dispositivos iOS u ordenadores portátiles). Asimismo, llevar una copia digital en PDF en la memoria del teléfono o una copia impresa en papel en la mochila es una de las mejores prácticas de viaje ante imprevistos como el agotamiento repentino de batería en el dispositivo móvil.

En la versión actual de la app:
- La portabilidad se realiza mediante archivos técnicos `japan-travel-plan.v1.json` diseñados para backup e importación entre dispositivos.
- No existe un mecanismo visual y legible para generar un documento maquetado para lectura o impresión.

### Objetivo
Desarrollar un **Generador de Documentos PDF 100% Offline** utilizando la API nativa `android.graphics.pdf.PdfDocument`, capaz de exportar:
1. **La Ficha Resumen del Día**: Un documento A4 compacto con las visitas de la jornada, horarios estimados, nombres en japonés legibles para taxistas y transeúntes, consejos y previsión del tiempo.
2. **El Dossier Completo del Viaje**: Un documento multipágina exhaustivo con la portada del viaje, calendario general, todos los días de visita y todos los tramos de vuelos y Shinkansen con sus números de reserva, terminales, andenes y asientos.
3. Compartición directa e inmediata mediante el **Android Sharesheet** (`Intent.ACTION_SEND` a través de `FileProvider`) para enviar el PDF por WhatsApp, Telegram, correo electrónico, Drive o imprimirlo.

---

## 2. Requisitos Funcionales (RF)

### RF-14.1: Alcance de la Exportación PDF
1. **Exportación de la Jornada Actual (`ExportDayPdfUseCase`)**:
   - Genera un PDF de 1 página (o máximo 2 páginas si hay muchas visitas) correspondiente al día seleccionado en el Planificador.
   - Encabezado: Título del viaje, Día X / N, fecha formateada (`Miércoles, 15 de Octubre de 2026`) y ciudad principal.
   - Tabla cronológica de paradas con:
     - Franja horaria y duración estimada.
     - Título en español / inglés y **nombre oficial en japonés en tipografía destacada** (`DisplayMedium`).
     - Dirección o notas útiles (ej. *"Comprar billete en máquina verde", "Entrada reservada 10:30"*).
     - Tarjetas de transporte del día (ej. Shinkansen Tokio -> Kioto con horarios y asientos).
2. **Exportación del Plan Completo (`ExportFullTripPdfUseCase`)**:
   - Genera un dossier formal estructurado:
     - **Portada**: Título del plan, autor, fechas de inicio y fin, recuento de jornadas y ciudades visitadas.
     - **Sección de Transporte**: Tabla consolidada con todos los vuelos internacionales y trenes Shinkansen.
     - **Itinerario Día a Día**: Cada jornada maquetada con sus respectivas paradas numeradas.
     - **Contactos de Emergencia**: Sede consular de referencia y teléfono de asistencia del seguro médico (`SPEC-11`).

### RF-14.2: Motor Gráfico Nativo (`PdfDocument`) y Diseño Visual Japonés
1. **Sin Dependencias Externas Pesadas**:
   - Implementado íntegramente mediante `android.graphics.pdf.PdfDocument`, `android.graphics.Canvas` y `android.graphics.Paint`. Cero librerías pesadas como iText o Apache PDFBox, manteniendo el tamaño del APK liviano.
2. **Estilo Gráfico Tradicional Japonés**:
   - Formato de página estándar **A4 (595 × 842 puntos PostScript)** a 72 dpi.
   - Colores oficiales del Design System de la app:
     - Cabeceras y barras de acento en **`Akabeni` (rojo Torii `#C93A40`)**.
     - Subtítulos y bordes en **`Ai-iro` (índigo japonés `#1E3A5F`)**.
     - Textos principales con contraste óptimo en tinta oscura `#1A1A1A`.
   - Soporte tipográfico completo para glifos japoneses (Kanji, Hiragana y Katakana) mediante tipografía de sistema (`Typeface.SANS_SERIF`).
   - Numeración de páginas en pie de página (*"Página X de Y — Japan Travel Tools"*).

### RF-14.3: Compartición Segura mediante Android Sharesheet
1. **Almacenamiento Efímero en Caché**:
   - Los archivos PDF se escriben de forma atómica en `Context.cacheDir/exports/pdf/` (`plan-viaje-japon-dia-X.pdf` o `dossier-viaje-japon-completo.pdf`).
   - Autolimpieza de PDFs antiguos para no acumular espacio en disco.
2. **Lanzamiento del Sharesheet**:
   - Exposición del archivo a través de `androidx.core.content.FileProvider` (`content://com.laralbir.japantraveltools.fileprovider/exports/pdf/...`).
   - Permiso `Intent.FLAG_GRANT_READ_URI_PERMISSION` y tipo MIME `application/pdf`.
   - Lanzador del diálogo nativo del sistema `Intent.createChooser` con opciones para WhatsApp, Telegram, Gmail, Drive o apps de visualización de PDF.

### RF-14.4: Inclusión de Mapa Vectorial de Ruta Diario Offline en PDF
1. **Representación Cartográfica Esquemática 100% Offline**:
   - Para cada jornada con paradas geolocalizadas (`latitude != null && longitude != null`), el PDF renderiza una tarjeta de mapa esquemático vectorial maquetada en el encabezado de las visitas del día.
   - **Proyección Local Acotada**: Centrado en las coordenadas del día mediante proyección equirrectangular calibrada para la latitud de Japón ($\approx 35^\circ$ N) con ratio de aspecto geodésico preservado.
   - **Trazado de Ruta y Pines Numerados**:
     - Línea discontinua con estilo japonés que une las paradas consecutivas en orden cronológico (1 $\to$ 2 $\to$ ... $\to$ N).
     - Pines circulares con fondo `Akabeni` (rojo Torii `#C93A40`), borde blanco y numeración secuencial idéntica a la lista de visitas.
     - Etiquetas flotantes legibles con nombre del lugar y hora prevista.
   - **Metadatos y Orientación**:
     - Indicador de orientación Norte (flecha / rosa de los vientos minimalista).
     - Métrica de distancia acumulada de la ruta diaria en kilómetros.
   - **Casos Límite**:
     - Si la jornada cuenta con una sola parada geolocalizada, se dibuja centrada con área de contexto.
     - Si ninguna parada del día posee coordenadas, se omite limpiamente el mapa sin generar espacios vacíos espurios.
2. **Control en Opciones de Exportación**:
   - Campo `includeDayMap: Boolean = true` en `PdfExportOptions`.
   - Selector interactivo mediante casilla de verificación en `PdfExportModalSheet`.

---

## 3. Reglas de Negocio y Offline-First

1. **Generación 100% Offline (RN-14.1)**: El renderizado del documento PDF no realiza ninguna llamada a la red ni depende de servicios en la nube.
2. **Preservación de Legibilidad en Papel (RN-14.2)**: Los textos en japonés deben tener un tamaño de fuente mínimo de 12 puntos para garantizar que sean perfectamente legibles si el documento se imprime en papel físico.
3. **Manejo Seguro de Excepciones (RN-14.3)**: Si ocurre un error de escritura en disco o memoria, la aplicación no debe cerrarse bruscamente (*crash*), sino capturar la excepción y mostrar un mensaje de error amigable en pantalla.

---

## 4. Diseño de Datos

```kotlin
data class PdfExportOptions(
    val scope: PdfExportScope,
    val targetDayIndex: Int? = null,
    val includeJapaneseNames: Boolean = true,
    val includeTransportSegments: Boolean = true,
    val includeEmergencyContacts: Boolean = true,
    val includeDayMap: Boolean = true
)

enum class PdfExportScope {
    SINGLE_DAY,
    FULL_TRIP
}

data class PdfExportResult(
    val fileUri: Uri,
    val fileAbsolutePath: String,
    val fileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long
)
```

---

## 5. Criterios de Aceptación (DoD)

1. Tests unitarios para el formateador de datos y ensamblador de páginas del PDF.
2. Comprobación de renderizado nítido de caracteres Kanji/Kana y acentos en español.
3. Apertura correcta en visualizadores PDF estándar de Android (Google PDF Viewer, Adobe Acrobat).
4. Textos de interfaz localizados en español e inglés.
5. Salto de línea final estándar POSIX (`\n`) en todos los archivos.
