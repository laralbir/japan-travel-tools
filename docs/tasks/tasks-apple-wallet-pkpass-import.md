# Tareas de Implementación: Importación de Billetes Apple Wallet (.pkpass)

- [ ] **Tarea 1: Modelo y Parser TDD de `.pkpass`**
  - Crear test unitario `PkPassParserTest.kt` (fase RED) que construya un ZIP en memoria con `pass.json` de un billete de tren y de un billete de avión y valide la extracción.
  - Implementar `PkPassParser.kt` en `feature.transport.pkpass` con `parsePkPass`, `parsePassJson`, `toTransportSegment` y `toTravelDocument` (fase GREEN).
  - Optimizar el código y asegurar soporte offline puro con `ZipInputStream` y `org.json.JSONObject` (fase REFACTOR).

- [ ] **Tarea 2: Integración en la Interfaz de Usuario (UI)**
  - Añadir botón de importación de billetes `.pkpass` en `TransportSegmentsScreen`.
  - Integrar launcher SAF `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())` o `GetContent()` admitiendo `.pkpass`.
  - Desplegar feedback al usuario tras la importación con título, estaciones y asientos.

- [ ] **Tarea 3: Verificación con Tests y Despliegue en Dispositivos Físicos**
  - Ejecutar `./gradlew testDebugUnitTest` y verificar que todos los tests pasen al 100%.
  - Desplegar en la tablet TCL NXTPAPER 11 Plus y Pixel 9 Pro XL con `./scripts/deploy-authorized-devices.sh`.
