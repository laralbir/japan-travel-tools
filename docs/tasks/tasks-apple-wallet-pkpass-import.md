# Tareas de Implementación: Importación de Billetes Apple Wallet (.pkpass)

- [x] **Tarea 1: Modelo y Parser TDD de `.pkpass`**
  - Crear test unitario `PkPassParserTest.kt` (fase RED) que construya un ZIP en memoria con `pass.json` de un billete de tren y de un billete de avión y valide la extracción.
  - Implementar `PkPassParser.kt` en `feature.transport.pkpass` con `parsePkPass`, `parsePassJson`, `toTransportSegment` y `toTravelDocument` (fase GREEN).
  - Optimizar el código y asegurar soporte offline puro con `ZipInputStream` y `org.json.JSONObject` (fase REFACTOR).

- [x] **Tarea 2: Integración en la Interfaz de Usuario (UI)**
  - Añadir botón de importación de billetes `.pkpass` en `TransportSegmentsScreen`.
  - Integrar launcher SAF `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())` o `GetContent()` admitiendo `.pkpass`.
  - Desplegar feedback al usuario tras la importación con título, estaciones y asientos.

- [x] **Tarea 4: Renderizado de Código QR / Aztec / Barcode con ZXing (TDD)**
  - Añadir dependencia `com.google.zxing:core:3.5.3` en `libs.versions.toml` y `app/build.gradle.kts`.
  - Escribir test `BarcodeBitmapGeneratorTest.kt` validando generación de códigos QR, Aztec, PDF417 y Code128 en `Bitmap`.
  - Implementar `BarcodeBitmapGenerator.kt` en `feature.transport.pkpass`.

- [x] **Tarea 5: Ordenación Cronológica y Detección de Duplicados (TDD)**
  - Escribir test `PkPassDuplicateAndSortingTest.kt` validando detección de billetes duplicados y reordenación de `order = 1..N` por `departureTimeIso`.
  - Implementar funciones auxiliares en `SharedTravelPlanStore` y `PkPassParser`.

- [x] **Tarea 6: Componentes UI de Visualización de QR y Diálogo de Duplicados**
  - Crear diálogo modal Compose `PkPassBarcodeViewerDialog` con fondo blanco de alto contraste y brillo para escaneo.
  - Implementar `FullScreenBarcodeDialog` con brillo al 100% (`screenBrightness = 1.0f`) y maximización a pantalla completa al tocar sobre el código QR.
  - Crear diálogo modal Compose `PkPassDuplicateResolutionDialog` (Sustituir / Duplicar / Cancelar).
  - Añadir botón `📱 Ver QR` en tarjeta de tramo de transporte y tarjeta de documento.

- [x] **Tarea 7: Verificación con Tests y Despliegue en Pixel 10 Pro XL**
  - Ejecutar `./gradlew testDebugUnitTest` y verificar 100% verde.
  - Desplegar en el Pixel 10 Pro XL con `./scripts/deploy-authorized-devices.sh`.
