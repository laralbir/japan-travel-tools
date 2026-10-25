# SPEC-03: Bóveda de Pasaporte/DNI, Repositorio de Documentos y Guías de Viaje

- **ID**: `SPEC-03`
- **Estado**: `Aprobado para Análisis/Implementación`
- **Versión Objetivo**: `0.0.1-beta.X` -> `0.0.1`

---

## 1. Objetivo

Permitir al viajero llevar siempre a mano y **100% offline**:
1. **Identificación Rápida (Pasaporte y DNI)**: Fotografías de alta legibilidad de su Pasaporte y Documento Nacional de Identidad listas para mostrar rápidamente en hoteles, compras *Tax-Free* o controles, con opción de bloqueo biométrico/PIN.
2. **Repositorio de Documentos de Viaje**: Un archivo organizado de billetes de vuelo, reservas de hotel, pases de tren (JR Pass / Shinkansen), seguros médicos, códigos QR y entradas en formato PDF o imagen.
3. **Gestor y Creador de Guías de Viaje (Travel Guides)**: Capacidad de agregar guías de viaje tanto importando archivos propios (PDF, Markdown, imágenes/mapas turísticos) como creando y editando guías personalizadas enriquecidas con texto, secciones y fotos dentro de la propia aplicación (100% offline).

---

## 2. Requisitos Funcionales (RF)

### RF-03.1: Tarjetas Rápidas de Pasaporte y DNI (ID Vault)
1. Sección destacada con acceso directo a:
   - **Pasaporte** (página principal de datos + visado/sello QR de Visit Japan Web opcional).
   - **DNI / Documento de Identidad** (Anverso y Reverso).
2. Captura e importación:
   - Permite tomar una foto directamente con la cámara o importar una imagen desde el selector del sistema (`Photo Picker`).
3. Visor a pantalla completa:
   - Soporte de zoom pellizcando (`pinch-to-zoom`), rotación y ajuste de brillo máximo temporal para facilitar la lectura de códigos o datos ante el personal de hoteles/tiendas.
4. Notas rápidas asociadas (opcional):
   - Campos opcionales para anotar el número de pasaporte, fecha de caducidad y número de DNI para copiar al portapapeles con un toque al rellenar formularios.

### RF-03.2: Privacidad y Bloqueo Biométrico Opcional
1. **Almacenamiento Interno Privado**:
   - Todas las imágenes de Pasaporte/DNI, los documentos importados y las guías de viaje se copian al directorio interno privado de la app (`Context.filesDir/vault/`, `Context.filesDir/documents/` y `Context.filesDir/guides/`).
   - **Nunca** se exponen en la galería pública de fotos del teléfono ni son accesibles por otras aplicaciones.
   - Si el usuario borra el archivo original de su galería de descargas, la copia dentro de **Japan Travel Tools** permanece intacta y disponible offline.
2. **Bloqueo Biométrico / PIN Opcional**:
   - El usuario puede activar en Ajustes o en la propia pantalla de Pasaporte/DNI la opción *"Proteger documentos de identidad con biometría/PIN"*.
   - Cuando está activo, antes de mostrar o ampliar el Pasaporte o DNI se solicita autenticación mediante `BiometricPrompt` (huella, reconocimiento facial o PIN/patrón del dispositivo).

### RF-03.3: Repositorio de Documentos de Viaje (Vuelos, Hoteles, Otros)
1. El usuario puede añadir, organizar, visualizar y eliminar documentos de su viaje en formato **PDF** (`application/pdf`) o **Imagen** (`image/jpeg`, `image/png`, `image/webp`).
2. **Categorías predefinidas**:
   - ✈️ **Vuelos** (billetes, tarjetas de embarque, itinerarios).
   - 🏨 **Hoteles y Alojamiento** (reservas de Booking/Agoda, direcciones en japonés para el taxi).
   - 🚄 **Transporte** (JR Pass, reservas Shinkansen, autobuses).
   - 🛡️ **Seguros y Salud** (póliza de seguro de viaje, certificados).
   - 🎟️ **Entradas y Reservas** (parques, museos, restaurantes, eSIM, Visit Japan Web).
   - 📖 **Guías de Viaje** (guías PDF, folletos, mapas de barrios o rutas).
   - 📁 **Otros**.
3. **Metadatos y Búsqueda Offline**:
   - Cada documento tendrá: Título personalizado, Categoría, Fecha asociada opcional (ej. fecha del vuelo o check-in), Notas rápidas y marca de Favorito/Destacado (`Pinned`).
   - Buscador por texto y filtro por categoría instantáneo.
4. **Visor Integrado Offline**:
   - Las imágenes se abren en el visor interno con zoom.
   - Los archivos PDF se renderizan dentro de la propia app mediante `android.graphics.pdf.PdfRenderer` sin requerir aplicaciones externas ni conexión a internet.

### RF-03.4: Agregar y Crear Guías de Viaje (Importadas + Nativas Offline)
1. **Importación de Guías Externas**:
   - Permite agregar archivos de guías de viaje en formato **PDF**, **Markdown (`.md`)** o **Imágenes/Mapas** (ej. mapas de metro de Tokio, guías de Kioto en PDF, dossieres de agencias).
   - Organización por **Región / Ciudad** (ej. *Tokio*, *Kioto*, *Osaka*, *Nara*, *Hiroshima*, *Hokkaido*, *General*) y etiquetas (`Gastronomía`, `Templos`, `Compras`, `Transporte`, `Etiqueta`).
   - Marcador de página (`bookmark`) persistente en guías PDF largas para retomar la lectura exactamente donde se dejó.
2. **Creación y Edición de Guías Propias en la App**:
   - El usuario puede crear guías personalizadas desde cero dentro de la app con:
     - Título, Ciudad/Región y Etiquetas.
     - Contenido enriquecido (formato Markdown/bloques de texto, listas de recomendaciones, consejos prácticos).
     - **Galería de fotos adjuntas offline** (capturas de pantalla, fotos de platos, mapas o lugares de referencia).
     - **Vínculo opcional con ubicaciones o actividades del Planificador de Viaje (`SPEC-04`)** para saltar de la guía al mapa o al itinerario.

---

## 3. Criterios de Aceptación (Gherkin / BDD)

### Escenario 1: Guardar y mostrar Pasaporte en modo offline con bloqueo biométrico
- **Dado** que el usuario ha guardado la foto de su Pasaporte y ha activado el bloqueo biométrico
- **Cuando** está en un comercio en Japón sin conexión a internet y pulsa "Ver Pasaporte"
- **Entonces** la app solicita su huella/PIN local y, tras autenticar, muestra la foto del pasaporte a pantalla completa con zoom habilitado.

### Escenario 2: Importar y abrir un PDF de reserva de hotel sin internet
- **Dado** que el usuario importa el PDF de confirmación de su hotel en Tokio en la categoría `Hoteles`
- **Cuando** el usuario borra el PDF de su carpeta `Downloads` del teléfono y activa el Modo Avión
- **Entonces** el PDF sigue abriéndose perfectamente dentro del visor integrado de la aplicación porque se persistió en el almacenamiento privado interno.

### Escenario 3: Agregar y consultar una Guía de Viaje (importada o creada) sin conexión
- **Dado** que el usuario ha importado una guía en PDF de Kioto (marcando la página 14) y ha creado una guía propia de *"Dónde comer Ramen en Shinjuku"* con 3 fotos adjuntas
- **Cuando** consulta la sección de Guías de Viaje en el tren sin cobertura
- **Entonces** puede abrir el PDF directamente en la página 14 y visualizar su guía personalizada de Shinjuku con todo el texto y las fotos almacenadas localmente.
