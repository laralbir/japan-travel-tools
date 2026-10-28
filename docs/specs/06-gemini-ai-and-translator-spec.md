# SPEC-06: Herramienta de Asistente IA con Gemini y Acceso Directo a Traducción Simultánea

- **ID**: `SPEC-06`
- **Estado**: `Aprobado para Análisis/Implementación`
- **Versión Objetivo**: `0.0.1-beta.X` -> `0.0.1`

---

## 1. Objetivo

Dotar a **Japan Travel Tools** de capacidades de Inteligencia Artificial mediante **Google Gemini** (combinando **Gemini Nano On-Device / AI Core** en los **Google Pixel 10 Pro XL y Pixel 9 Pro XL** y **Gemini API Cloud** con caché local de respuestas) junto con un **Acceso Directo de Traducción Simultánea (`ES/EN <-> Japonés 日本語`)** por voz, texto y cámara que funcione incluso **100% sin conexión a internet** mediante modelos descargables en el dispositivo (**ML Kit Translation + Speech-to-Text + Text-to-Speech + OCR Japonés** e integración directa con **Google Translate / Pixel Live Translate**).

---

## 2. Requisitos Funcionales (RF)

### RF-06.1: Acceso Directo Global a Traducción Simultánea (`Quick Live Translate`)
1. **Botón de Acción Rápida siempre accesible (Home / Barra Superior / Quick Tile)**:
   - Un botón destacado de **Traducción Simultánea** accesible con un solo toque desde el Home o desde cualquier herramienta (útil al estar frente a un camarero, en taquilla de tren o en recepción del hotel).
2. **Modo Conversación Cara a Cara (Pantalla Dividida / Dual View)**:
   - Interfaz dividida pensada para sostener el móvil o apoyar la tablet **TCL NXTPAPER 11 Plus** en una mesa:
     - **Mitad inferior (Idioma del viajero: Español o Inglés)**: Micrófono y texto en el idioma configurado en la app.
     - **Mitad superior (Japonés `日本語`, con opción de girar 180°)**: Muestra la traducción en caracteres japoneses grandes (`Kanji/Kana`), lectura fonética (**Romaji** para que el viajero pueda pronunciarlo) y reproducción de voz clara en japonés (`TTS`).
   - **Escucha y Traducción Simultánea Bidireccional por Voz y Audio (`Audio-First`: STT + Auto-TTS)**:
     - Dado que el viajero **no sabe leer japonés**, la traducción simultánea es **100% guiada por voz y audio hablado en ambos sentidos**:
       1. **Habla el Viajero (`🎙️ Hablar en ES/EN ➔ 🔊 Suena en Japonés`)**: El usuario habla por el micrófono en Español o Inglés (`RecognizerIntent` `es-ES` / `en-US`), la app traduce la frase y **la locuta automáticamente en voz alta en Japonés (`TextToSpeech` `Locale.JAPAN`)** para que la escuche el interlocutor japonés.
          - **Regla Estricta de Concisión y Fidelidad**: La traducción al japonés debe ser fiel y concisa, traduciendo con exactitud lo expresado **sin añadir en ningún caso más palabras, preguntas secundarias, elogios no dichos o disculpas sobre apps de traducción** (ej. si pide la cuenta, no añadir preguntas de Suica/tarjeta; si dice hola o gracias, no añadir que la comida estuvo rica; si dice una sola bebida, no añadir 'dos').
       2. **Habla el Interlocutor Japonés (`🎙️ Escuchar Japonés ➔ 🔊 Suena en ES/EN`)**: El interlocutor habla en Japonés (`RecognizerIntent` `ja-JP`), la app captura la voz japonesa, transcribe el texto en japonés, traduce ese texto de manera **fiel y literal** a Español/Inglés (sin inventar respuestas conversacionales ni actuar como un bot que responde al interlocutor) y **la locuta automáticamente en voz alta en Español/Inglés (`TextToSpeech` `es-ES` / `en-US`)** para que el viajero escuche con exactitud lo que se ha dicho.
       3. **Regla Estricta de Fidelidad Literal y Prohibición de Respuestas Conversacionales Inventadas**:
          - **El traductor es un INTÉRPRETE, no un BOT conversacional**: Si el interlocutor japonés dice *"すみません"* (*Sumimasen*), la app debe traducir *"Disculpe / Perdón"*, **NUNCA inventar una respuesta de dependiente** como *"¿En qué puedo ayudarle?"*. Si dice *"1500円"*, debe traducir *"1500 yenes"*, **NUNCA** *"El total a pagar son 1500 yenes"*. Si dice *"お水"*, debe traducir *"Agua"*, **NUNCA** *"Aquí tiene agua fresca de cortesía"*. Si dice *"チェックイン"*, debe traducir *"Check-in / Registro"*, **NUNCA** *"Vamos a gestionar el check-in de su habitación..."*.
          - **Prohibición de Mensajes Meta de Fallback**: Queda terminantemente prohibido generar mensajes como *"El interlocutor ha respondido en japonés. Por favor, pídele que hable despacio..."*. La app debe traducir directamente las palabras y conceptos reconocidos o consultar el motor de traducción en tiempo real cuando haya red.
          - **Regla Estricta de No-Fuga de Texto en Locución**: El texto a locutar al viajero (`spanishSpokenText` / `englishSpokenText`) **NUNCA debe contener caracteres japoneses (Kanji, Hiragana o Katakana)** ni devolver la cadena japonesa en crudo. Todo mensaje reconocido o heurístico se traduce a español/inglés para que el motor TTS pronuncie en perfecto español/inglés.
       4. **Visualización Completa y Simétrica en Ambas Tarjetas**:
          - **Tarjeta 1 (Español ➔ Japonés)**: Muestra el texto capturado en español, la traducción en caracteres japoneses grandes (`Kanji/Kana`) y la transcripción fonética en **Romaji**, con botón para reproducir en audio japonés.
          - **Tarjeta 2 (Japonés ➔ Español)**: Muestra el texto japonés capturado (`Kanji/Kana`), su fonética en **Romaji** y la traducción fiel en español (o inglés), con botón para reproducir en audio español.
       5. **Detección Automática de Idioma Bidireccional (`Smart Language Routing`)**: Si se detectan caracteres japoneses en el micrófono del viajero, la app reconoce que se habló en japonés y locuta la traducción en español. A la inversa, si se habla en español o inglés en el micrófono del interlocutor, lo traduce y locuta en japonés.
       6. **Controles de Reproducción de Voz Directa (`🔊 Escuchar en Japonés` / `🔊 Escuchar en Español`) y Auto-Locución**: Cualquier frase seleccionada o traducida puede reproducirse por altavoz con un toque (y cuenta con interruptor de locución automática activado por defecto). En el apartado de frases habituales en japonés, la reproducción se realiza obligatoriamente en japonés (`Locale.JAPAN` / `ja-JP`) para que el viajero pueda escuchar cómo suena o mostrárselo a personas locales (#8).
3. **Traductor de Texto Dedicado (Español / Inglés ➔ Japonés — Issue #8)**:
   - **Tarjeta Independiente para Entrada de Texto**: Dispone de un `OutlinedTextField` multilínea para que el viajero pueda redactar cualquier consulta o frase libremente (ej. *"¿Dónde está el baño?"*, *"La cuenta, por favor"*, *"Una cerveza, por favor"*, etc.).
   - **Traducción Inmediata y Fonética**: Botón `🇯🇵 Traducir a Japonés` que genera al instante la traducción a caracteres japoneses (`Kanji/Kana`) junto con su fonética `Romaji` y categoría temática.
   - **Locución en Japonés por Voz (`ja-JP`)**: Botón prominente `🔊 Escuchar en Japonés` para reproducir la pronunciación exacta mediante Android TextToSpeech (`Locale.JAPAN`).
   - Las tarjetas individuales de traducción (Texto, Voz Japonés ➔ Español y Voz Español ➔ Japonés) se mantienen limpias y concisas, sin listas redundantes ni preguntas incrustadas en su interior.
4. **Traducción Visual con Cámara (Menús, Carteles e Ingredientes — OCR Offline)**:
   - **Permisos en Tiempo de Ejecución y Captura Robusta**: Manejo de permiso de cámara `CAMERA` en runtime con `rememberLauncherForActivityResult(RequestPermission)` antes de invocar la captura.
   - **Captura directa y selección de fotos**: Botones para abrir la cámara y tomar foto instantánea en alta resolución mediante `FileProvider` en caché (`ActivityResultContracts.TakePicture`) con fallback a `TakePicturePreview`, o importar fotos desde la galería/archivos (`GetContent("image/*")`).
   - **Reconocimiento de Texto Japonés (Kanji / Kana)**: Procesamiento mediante **Google ML Kit Text Recognition v2 (Japanese)** (`JapaneseTextRecognizerOptions`) sobre la imagen, extrayendo los caracteres japoneses.
   - **Traducción Inmediata y Pronunciación**: Desglose línea por línea del cartel o menú en español/inglés, con pronunciación fonética en Romaji, detección de precios en yenes (`¥`) y botón para escuchar la locución por voz (`🔊`).
   - **Carteles de Prueba Rápida**: Muestras representativas de carteles y menús comunes en Japón (*Ramen 1000円*, *Zona de no fumadores*, *Completo/Sold out*, *Andén Shinkansen*, *Aseos*, *Salida*, *Solo efectivo*) para pruebas inmediatas.
5. **Sección Unificada de Frases Habituales de Viaje en Japonés (Al Final y Sin Repeticiones)**:
   - En lugar de fragmentar preguntas en cada tarjeta, la pantalla consolida al final una **tarjeta unificada y dedicada** de frases hechas y preguntas habituales de viaje para hablar en japonés.
   - **Sin Repeticiones**: Lista única, desduplicada y clasificada temáticamente (Cortesía, Restaurante, Trenes y Transporte, Tiendas y Pagos, Hotel y Dirección, Emergencias y Salud).
   - **Representación Dual Obligatoria**: Cada frase muestra con total claridad tanto el **texto en español** (o inglés según el idioma activo) como el **texto en japonés** (`Kanji/Kana`) y su fonética en **Romaji**.
   - **Locución Instantánea en Japonés**: Cada elemento dispone de un botón directo de altavoz `🔊` para que el teléfono reproduzca de inmediato la frase en voz alta en japonés (`Locale.JAPAN`), permitiendo que el viajero la escuche para aprenderla o se la muestre/reproduzca directamente al interlocutor local.

### RF-06.2: Asistente de Viaje con IA (Google Gemini: On-Device + Cloud con Caché Offline)
1. **Arquitectura Híbrida de Gemini (Optimizada para Pixel 10/9 Pro XL y Tablet TCL)**:
   - **Modo Gemini Cloud (`Google GenAI REST API` — ej. `gemini-2.0-flash` / `gemini-1.5-flash`)**:
     - Permite configurar la API Key de Gemini en Ajustes y en el propio Asistente IA (guardada de forma segura en `EncryptedSharedPreferences` / DataStore) para consultas libres y complejas con generación en tiempo real cuando hay conexión.
   - **Modo Experto Offline en Viajes a Japón (On-Device Knowledge Engine)**:
     - Motor offline especializado con cobertura profunda de más de 20 categorías habituales (Tarjetas Suica/Pasmo/IC, Shinkansen, JR Pass y rentabilidad, reglas de maletas grandes >160cm, comida y cartas de Izakaya/Ramen/Sushi, Tax-Free y compras en Don Quijote, eSIM/Pocket Wi-Fi, cajeros 7-Eleven y efectivo, enchufes y voltaje 100V, propinas y modales, normas Onsen y tatuajes, clima y ropa por temporada, traslados desde aeropuertos Narita/Haneda/KIX, emergencias y terremotos J-Alert, rutas completas Tokio/Kioto/Osaka/Nara/Hakone/Hiroshima).
   - **Estados de Carga y Retroalimentación Visual Reactiva**:
     - Al pulsar `✨ Consultar IA` o cualquier chip de tema rápido, la interfaz debe mostrar de forma inequívoca el estado de carga (`isLoading = true`), bloqueando pulsaciones duplicadas y mostrando un spinner animado (`CircularProgressIndicator`) con el texto *"Consultando a la IA..."*.
     - Cada respuesta debe mostrar su fuente de procedencia (`✨ Gemini Cloud (Online)` o `⚡ Experto Japón (100% Offline)`) y la hora de generación para que el usuario compruebe de forma inmediata que el botón funciona y actualiza el contenido.
   - **Acciones Directas sobre la Respuesta Generada**:
     - Botón `📋 Copiar`: Copia la respuesta completa al portapapeles del sistema con confirmación visual.
     - Botón `📖 Guardar como Guía`: Guarda la respuesta como un nuevo artículo en la Bóveda de Guías (`SPEC-03`) para conservarla permanentemente offline.
     - Botón `🗓️ Aplicar al Itinerario`: Inyecta las paradas o excursión sugerida en el Planificador de Viaje (`SPEC-04`).
   - **Persistencia y Caché Offline de Conversaciones e Insights de IA**:
     - Todas las respuestas, explicaciones culturales, traducciones de menús y rutas generadas por Gemini se guardan automáticamente en la base de datos local para poder consultarlas después en el metro o avión sin conexión.
2. **Funciones Contextuales de Gemini en la App**:
   - **Asistente de Menús y Productos Japoneses (Visión)**: Haz una foto a la carta de un *Izakaya* en japonés o a un producto de farmacia/supermercado y Gemini te explica cada plato, ingredientes, precios convertidos a `EUR` (usando la tasa cacheada de `SPEC-02`) y cómo pedirlo.
   - **Creación y Mejora Directa de Planes de Viaje y Transporte en la App (`AI Plan Builder & Optimizer`)**:
     - La IA integrada (**Gemini**) tiene acceso de lectura y escritura (bajo confirmación del usuario) sobre los planes de viaje (`SPEC-04`) y planes de transporte (`SPEC-05`) almacenados en la app:
       1. **Crear Nuevos Planes desde Cero con IA**: El usuario puede pedir *"Créame un plan de día completo en Kioto (Arashiyama + Kinkaku-ji + Gion) con sus trenes/autobuses"* y Gemini genera directamente las paradas horarias (`ItineraryStopItem` con coordenadas y nombre en japonés) y los tramos de transporte (`TransportSegmentItem`), inyectándolos en el Planificador.
       2. **Analizar y Mejorar Planes Existentes (`AI Optimize Plan`)**: Gemini lee el itinerario actual de la app y:
          - Detecta y resuelve automáticamente **solapamientos horarios** o desplazamientos geográficos ineficientes.
          - Añade paradas complementarias cercanas (ej. lugares recomendados para comer cerca del templo programado a mediodía).
          - Sugiere o inserta los **tramos óptimos de Shinkansen, Metro o Autobús** (con su línea, código de estación y salida recomendada) entre dos actividades del planificador.
          - Puede generar y modificar directamente el **JSON legible del plan de viaje** para aplicar mejoras en bloque.
   - **Consultor de Etiqueta, Trenes y Costumbres en Japón**: Respuestas rápidas contextualizadas para el viajero.

### RF-06.3: Configuración Amigable de Clave de API de Gemini (`GeminiApiKeyHelper` & `GeminiApiKeySetupCard`)
1. **Acceso Directo y Asistido a Google AI Studio**:
   - Botón directo y destacado: `🌐 Obtener Clave Gratis en Google AI Studio`, que abre con un solo toque la consola oficial (`https://aistudio.google.com/app/apikey`) en el navegador del dispositivo.
2. **Tutorial Integrado en 4 Pasos Sencillos**:
   - Tarjeta desplegable con guía paso a paso para cualquier perfil de viajero:
     - **Paso 1**: Abrir Google AI Studio con el botón directo.
     - **Paso 2**: Iniciar sesión con cualquier cuenta personal de Google/Gmail (acceso 100% gratuito y sin tarjetas de crédito).
     - **Paso 3**: Crear la clave con un toque en «Create API key» y copiarla.
     - **Paso 4**: Volver a la app, pulsar «Pegar desde Portapapeles» y guardar.
3. **Pegado Rápido y Conmutador de Visibilidad**:
   - Botón `📋 Pegar`: Extrae automáticamente la clave copiada del portapapeles del sistema evitando selecciones manuales complejas en pantallas táctiles.
   - Conmutador `👁️ / 🙈`: Permite alternar la máscara de contraseña para verificar la clave o mantenerla oculta.
4. **Validación Reactiva de Formato**:
   - Verificación en vivo del prefijo estándar `AIzaSy...` con indicación visual (`✅ Formato válido` vs advertencias descriptivas).
5. **Garantía y Tranquilidad Offline**:
   - Aclaración explícita de que la clave es 100% opcional; la app funciona de manera autónoma sin internet ni registro gracias a su Motor Experto On-Device.
6. **Configuración Centralizada en Ajustes**:
   - La configuración completa de la API Key se gestiona de forma centralizada en la pantalla de **Ajustes** (`SettingsPreferencesScreen`), manteniendo la pantalla del **Asistente IA** despejada de formularios de configuración y proporcionando un acceso directo mediante el botón `⚙️ Configurar en Ajustes` que traslada al viajero a la pantalla de Ajustes.
7. **Verificación Activa y Diagnóstico de Conexión (`testApiKeyConnection`)**:
   - Botón `⚡ Probar Conexión` en la tarjeta de configuración que ejecuta un ping real contra la API de Gemini (`generateContent`).
   - Muestra diagnósticos claros en pantalla:
     - `200 OK`: Confirmación exitosa con indicación del modelo verificado.
     - `403 PERMISSION_DENIED`: Alerta explícita de clave no válida o sin permisos, recordando que las claves de Google AI Studio empiezan siempre por `AIzaSy...`.
     - `429 RESOURCE_EXHAUSTED`: Advertencia de cuota excedida temporalmente.
     - Error de Red / Offline: Advertencia de falta de conexión para verificar.
8. **Transparencia y Alertas Visibles de Error Cloud (Fin del Fallback Silencioso)**:
   - Cuando el usuario tiene una API Key configurada pero la llamada a Gemini Cloud falla, el resultado de `queryAssistant` expone `cloudErrorDetail` (código HTTP y causa).
   - En la interfaz del Asistente IA se muestra un aviso prominente informando de que Gemini Cloud falló y que se ofrece la respuesta del Motor Experto Offline como respaldo, erradicando cualquier confusión del viajero.
9. **Selector de Modelos Canónicos de Gemini y Prevención de Errores 404 (`pref_gemini_model_id`)**:
   - Selector en Ajustes para alternar entre modelos oficiales canónicos respaldados por la API de Google AI Studio:
     - `Gemini Flash (Latest)` (`gemini-flash-latest`, alias canónico oficial siempre actualizado, recomendado por defecto).
     - `Gemini Pro (Latest)` (`gemini-pro-latest`, alias canónico oficial para razonamiento profundo).
     - `Gemini 2.5 Flash` (`gemini-2.5-flash`, versión estable de Gemini 2.5).
     - `Gemini 2.5 Pro` (`gemini-2.5-pro`, última generación Pro con razonamiento avanzado).
     - `Gemini Flash-Lite` (`gemini-flash-lite-latest`, ultrarrápido de mínima latencia).
     - Opción de modelo personalizado libre.
   - **Migración Automática de Modelos Deprecados**: El repositorio `UserPreferencesRepository` detecta identificadores deprecados por Google en la API v1beta (`gemini-1.5-flash`, `gemini-2.0-flash`, `gemini-1.5-pro`) y los migra de forma transparente e inmediata a `gemini-flash-latest` o `gemini-pro-latest`, eliminando el error HTTP 404 (*Model Not Found*).
   - Parseo de respuesta con `org.json.JSONObject` nativo de Android extrayendo de forma segura `candidates[0].content.parts[0].text`.
10. **Selector Rápido de Modelo PRO vs FLASH en el Asistente IA**:
   - Selector directo y conmutador táctil integrado en la cabecera del Asistente IA (`AiAssistantScreen`):
     - `⚡ Flash (Rápido)`: Conmuta en 1 toque a `gemini-flash-latest` para respuestas instantáneas de consultas habituales.
     - `🧠 PRO (Razonamiento)`: Conmuta en 1 toque al modelo Pro (`gemini-pro-latest` / `gemini-2.5-pro`) para análisis profundos, optimización de itinerarios y resolución de conflictos de agenda.
   - Sincronización bidireccional reactiva con la persistencia en `UserPreferencesRepository`.

### RF-06.4: Experiencia de Usuario y Renderizado del Asistente IA (Markdown, Input Multilínea, Teclado y Cancelación)
1. **Renderizado de Markdown Rico en Respuestas**:
   - Las respuestas generadas por los modelos de Gemini o el Experto Offline suelen incluir formato Markdown (`## Encabezados`, `**negrita**`, `*cursiva*`, `- listas con viñetas`, `` `código o términos en romaji/japonés` ``).
   - La interfaz no debe volcar texto en crudo con símbolos Markdown sin interpretar. Debe utilizar un componente de renderizado nativo (`MarkdownText` / `AnnotatedString`) con tipografía Material 3 que destaque encabezados en negrita y mayor escala, palabras en negrita/cursiva, viñetas indentadas y términos destacados con tipografía monoespaciada sobre fondo sutil.
2. **Entrada de Texto Multilínea Ergonómica**:
   - El campo de texto de consulta a la IA debe soportar multilínea (`singleLine = false`, `minLines = 2`, `maxLines = 5`) para que el usuario pueda redactar y revisar consultas largas o itinerarios complejos sin desplazamientos horizontales incómodos.
   - Debe integrar `Modifier.imePadding()` en la jerarquía del contenedor para que el teclado virtual del sistema no tape el campo de entrada.
   - Debe incorporar opción de envío rápido mediante la acción IME del teclado (`ImeAction.Send` / `onSend`).
3. **Gestión Automática del Teclado Virtual**:
   - Al enviar una consulta (ya sea pulsando el botón `✨ Consultar IA` o la acción de envío del teclado virtual), el teclado debe cerrarse automáticamente (`LocalSoftwareKeyboardController.current?.hide()`) y liberar el foco para ofrecer una vista despejada del progreso y la respuesta.
4. **Cancelación Inmediata de Consultas en Progreso**:
   - Toda consulta en curso debe estar ligada a un `Job` de corrutina cancelable.
   - Mientras `isLoading == true`, la interfaz debe proporcionar un botón prominente `❌ Cancelar consulta`.
   - Al pulsar el botón de cancelar, la petición en curso debe abortarse de inmediato, restableciendo el estado a `isLoading = false` e indicando un mensaje de estado amigable (*"Consulta cancelada"*).

---

## 3. Criterios de Aceptación (Gherkin / BDD)

### Escenario 1: Traducción simultánea por voz y texto en Modo Avión
- **Dado** que los modelos offline de Español/Inglés y Japonés están descargados en el dispositivo y el usuario está en Modo Avión
- **Cuando** pulsa el **Acceso Directo a Traducción Simultánea** y habla en Español: *"¿Tiene este plato marisco o cacahuetes?"*
- **Entonces** la app transcribe la frase, muestra la traducción en Japonés (`この料理には魚介類やピーナッツが入っていますか？`), muestra su pronunciación en **Romaji** y la reproduce por voz en japonés sin necesitar conexión a internet.

### Escenario 2: Analizar una carta de restaurante japonés con Gemini y guardarla offline
- **Dado** que el usuario toma una foto de un menú en japonés desde el asistente de Gemini
- **Cuando** Gemini analiza la imagen
- **Entonces** devuelve los platos traducidos al idioma del usuario con sus precios convertidos a `EUR` y toda la respuesta queda persistida localmente en el historial offline.

### Escenario 3: Crear y mejorar planes de viaje y transporte directamente con Gemini
- **Dado** que el usuario tiene un plan en la app con horarios solapados o quiere añadir un nuevo día en Kioto/Osaka
- **Cuando** pulsa *"Crear / Mejorar Plan con Gemini IA"*
- **Entonces** Gemini analiza el plan actual de la app, corrige los solapamientos horarios, añade las paradas óptimas con sus coordenadas/nombres en japonés y sus tramos de tren/metro, y actualiza el Planificador de Viaje (`SPEC-04`) y de Transporte (`SPEC-05`) dejándolo persistido 100% offline.

### Escenario 4: Optimización de planes por la IA según el tiempo de Open-Meteo (`SPEC-07`)
- **Dado** que la previsión meteorológica de Open-Meteo (`SPEC-07`) indica alta probabilidad de lluvia o mal tiempo en una fecha del itinerario
- **Cuando** el usuario solicita a la IA optimizar el plan con información meteorológica
- **Entonces** la IA adapta las paradas de ese día añadiendo alternativas cubiertas (mercados techados, museos, galerías *Shotengai*) y consejos de equipamiento (paraguas, calzado impermeable, visibilidad del Monte Fuji).

### Escenario 5: Flujo de traducción literal de voz en ambos sentidos sin respuestas inventadas
- **Dado** que el usuario utiliza el traductor de voz bidireccional
- **Cuando** habla un interlocutor japonés y se captura texto en japonés (ej. *"これはいくらですか？"* o *"すみません"* o *"お水"*)
- **Entonces** la app captura el texto en japonés, lo traduce directamente al español (*"¿Cuánto cuesta esto?"*, *"Disculpe / Perdón"*, *"Agua"* sin inventar respuestas conversacionales ni mensajes meta de chatbot), muestra el texto japonés, su fonética en Romaji y la traducción fiel en español, y locuta el texto traducido al español.
- **Y cuando** el viajero habla en español (ej. *"¿Dónde está la estación?"*)
- **Entonces** la app captura el texto en español, lo traduce fielmente al japonés (`駅はどこですか？`), muestra el texto en español, el texto en japonés y el Romaji (`Eki wa doko desu ka?`), y lo locuta en voz alta en japonés.

### Escenario 6: Renderizado formateado de respuestas con Markdown
- **Dado** que la IA devuelve una respuesta con títulos `## Shinkansen`, viñetas `* Maletas grandes`, negritas `**reserva obligatoria**` o términos destacados `` `Tokaido Shinkansen` ``
- **Cuando** se muestra en la pantalla del Asistente
- **Entonces** la vista formatea los títulos con mayor peso tipográfico y tamaño, renderiza las viñetas indentadas, las negritas destacadas y los términos en código con fuente monoespaciada, sin exhibir asteriscos ni almohadillas en crudo.

### Escenario 7: Experiencia fluida de teclado e input multilínea
- **Dado** que el usuario escribe una consulta extensa en el Asistente IA
- **Cuando** escribe varias líneas y pulsa la tecla de envío del teclado virtual
- **Entonces** el campo se expande dinámicamente hasta 5 líneas sin ser tapado por el teclado (mediante padding IME), se lanza automáticamente la consulta a la IA y el teclado virtual se oculta liberando la pantalla.

### Escenario 8: Cancelación de una consulta en progreso
- **Dado** que el usuario pulsa *"Consultar IA"* y la petición entra en estado de carga (`isLoading = true`)
- **Cuando** el usuario pulsa *"Cancelar consulta"*
- **Entonces** la corrutina en curso se cancela de forma inmediata, el indicador de carga desaparece y el sistema muestra un mensaje informativo indicando que la consulta ha sido cancelada.

