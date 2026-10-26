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
3. Visor a Pantalla Completa con Zoom Táctil (`FullScreenZoomableImageViewerDialog`):
   - Al tocar la fotografía de Pasaporte o DNI, se abre un visor inmersivo a pantalla completa sobre fondo negro de alto contraste.
   - **Gesto de pellizco para zoom (`pinch-to-zoom`)**: amplía suavemente la imagen desde 1.0x hasta 5.0x, permitiendo inspeccionar con total nitidez detalles cruciales como el número de pasaporte, fecha de caducidad, códigos MRZ o números de identidad.
   - **Desplazamiento panorámico (`pan / drag`)**: permite arrastrar y mover la foto ampliada para recorrer los bordes y zonas de interés manteniéndola delimitada en pantalla.
   - **Doble toque rápido (`double-tap`)**: alternador rápido entre zoom ampliado (2.5x) y zoom normal (1.0x).
   - **Indicador y control de restablecimiento**: visualización en vivo del porcentaje de zoom (ej. `🔍 100%`, `🔍 250%`) y botón para restablecer inmediatamente la imagen a su escala original.
4. Notas rápidas asociadas (opcional):
   - Campos opcionales para anotar el número de pasaporte, fecha de caducidad y número de DNI para copiar al portapapeles con un toque al rellenar formularios.

### RF-03.2: Documentos de Identidad Personalizados (Custom ID Documents Vault)
1. El viajero puede añadir, editar, consultar y eliminar cualquier documento sensible adicional con fotografía en la bóveda de identidad:
   - 💉 **Certificados de Vacunación** (fiebre amarilla, tétanos, Covid, etc.).
   - 💊 **Recetas Médicas para Aduanas** (indispensables en aduanas de Japón para medicamentos prescritos y permisos *Yakkan Shoumei* / confirmación de importación).
   - 🚗 **Carnet de Conducir Internacional** (IDP según la Convención de Ginebra de 1949).
   - 🏥 **Pólizas de Seguro Médico Internacional** con tarjeta de asistencia y teléfonos 24/7.
   - 🛂 **Visados, sellos consulares y códigos QR de entrada**.
   - 📄 **Cualquier documento oficial o privado personalizado**.
2. **Sugerencias Rápidas (Chips de Presets)**:
   - El editor modal incluye chips rápidos para rellenar de inmediato el título con un solo toque.
3. **Visor Inmersivo con Zoom Táctil**:
   - Cada documento personalizado cuenta con acceso al visor a pantalla completa con `pinch-to-zoom` (1.0x-5.0x), arrastre y doble toque.
4. **Protección y Doble Comprobación de Seguridad**:
   - La eliminación de cualquier documento o fotografía personalizada requiere confirmación obligatoria con `buildSensitiveDeleteConfirmation` y doble check contra borrados involuntarios.

### RF-03.3: Privacidad y Bloqueo Biométrico Obligatorio (Real Android BiometricPrompt)
1. **Almacenamiento Interno Privado**:
   - Todas las imágenes de Pasaporte, DNI y Documentos Personalizados se copian al directorio interno privado de la app (`Context.filesDir/id_vault/`, `custom_id_docs.json`).
   - **Nunca** se exponen en la galería pública de fotos del teléfono ni son accesibles por otras aplicaciones.
   - Si el usuario borra el archivo original de su galería o descargas, la copia dentro de **Japan Travel Tools** permanece intacta y disponible offline.
2. **Bloqueo Biométrico Obligatorio por Diseño (Sin Conmutador ni Bypass)**:
   - La protección biométrica para documentos confidenciales (Pasaporte, DNI y documentos personalizados) es **estrictamente obligatoria** cuando el dispositivo tiene configurada huella dactilar, reconocimiento facial o PIN/patrón/contraseña (`BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BIOMETRIC_SUCCESS`).
   - **Eliminación del Conmutador (Toggle)**: No existe toggle para desactivar la seguridad, eliminando cualquier riesgo de desprotección involuntaria o bypass. La tarjeta de cabecera informa el estado de protección activa obligatoria.
   - Si el dispositivo carece de método biométrico o PIN configurado en los ajustes de Android, se permite el acceso directo mostrando un aviso de advertencia de seguridad recomendando configurar una clave en el sistema operativo.
3. **Estado Bloqueado Obligatorio por Defecto (Efímero en Memoria)**:
   - Cada vez que el usuario accede a la sección de Identificación, la vista nace **siempre bloqueada por defecto** (`vaultUnlocked = false`) mostrando la tarjeta `"🔒 Identificación Bloqueada"`.
   - El estado de desbloqueo es estrictamente efímero en memoria (`remember`) y **nunca** se persiste en el `savedInstanceState` (`rememberSaveable`), obligando siempre a una nueva verificación de biometría en cada entrada o reinicio.
4. **Auto-Bloqueo por Tiempo de Seguridad y Ciclo de Vida**:
   - Al desbloquearse la bóveda, se inicia un temporizador de seguridad de **120 segundos** (2 minutos); superado dicho tiempo, la bóveda se bloquea automáticamente (`vaultUnlocked = false`) ocultando los documentos.
   - Si la aplicación o la pantalla pasa a segundo plano (`Lifecycle.Event.ON_STOP` / `ON_PAUSE`), la bóveda se bloquea inmediatamente por seguridad.
5. **Botón de Bloqueo Manual ("Bloquear")**:
   - Mientras los documentos estén visibles (`vaultUnlocked = true`), se muestra un botón con el texto conciso **"Bloquear"** (`🔒 Bloquear`), que permite al usuario forzar el bloqueo inmediato en cualquier instante con un solo toque.

### RF-03.4: Repositorio de Documentos y Entradas de Viaje (Vuelos, Hoteles, Entradas y Museos)
1. El usuario puede añadir, organizar, visualizar, editar y eliminar documentos y entradas de su viaje en formato **PDF** (`application/pdf`) o **Imagen** (`image/jpeg`, `image/png`, `image/webp`).
2. **Categorías predefinidas**:
   - ✈️ **Vuelos** (billetes, tarjetas de embarque, itinerarios).
   - 🏨 **Hoteles y Alojamiento** (reservas de Booking/Agoda, direcciones en japonés para el taxi).
   - 🎟️ **Entradas y Ocio (Tickets)**: Gestión específica para museos (Museo Ghibli, teamLab Planets, Museo Nacional), parques temáticos (Universal Studios Japan USJ con pase Nintendo World, Tokyo Disneyland / DisneySea), miradores (Shibuya Sky, Tokyo Skytree) y reservas con código QR o localizador horario.
   - 🚄 **Transporte** (JR Pass, reservas Shinkansen, autobuses).
   - 🛡️ **Seguros y Salud** (póliza de seguro de viaje, certificados).
   - 📖 **Guías de Viaje** (guías PDF, folletos, mapas de barrios o rutas).
   - 📁 **Otros**.
3. **Gestión Completa (CRUD)**:
   - Posibilidad de crear, editar datos (título, categoría, localizador/notas, fecha) y eliminar cualquier documento o entrada en cualquier momento.
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
