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
   - **Escucha y Traducción Simultánea Bidireccional**:
     - Reconocimiento de voz continuo (`SpeechRecognizer`) que transcribe y traduce en tiempo real lo que dice el usuario al japonés y lo que responde el interlocutor japonés al español/inglés.
3. **Traducción Visual con Cámara (Menús, Carteles y Ingredientes — OCR Offline)**:
   - Permite apuntar con la cámara o elegir una foto para extraer texto japonés (**Kanji / Hiragana / Katakana**) mediante **Google ML Kit Text Recognition v2 (Japanese)** offline y traducirlo al instante (con análisis multimodal enriquecido con **Gemini** cuando hay conexión).
4. **Lanzador Directo a Google Translate / Intérprete Nativo de Pixel**:
   - Incluye también un botón de salto rápido (`Deep Link / Intent`) hacia el **Modo Conversación / Cámara de Google Translate** o **Live Translate de Pixel** por si el usuario desea abrir la app externa del sistema.
5. **Tarjetas de Frases Esenciales y Alergias 100% Offline**:
   - Tarjetas pre-cargadas de emergencia/supervivencia (alergias alimentarias, pedir la cuenta, ¿dónde está el baño/andén?, *Tax-Free*) con texto japonés a gran tamaño y audio offline.

### RF-06.2: Asistente de Viaje con IA (Google Gemini: On-Device + Cloud con Caché Offline)
1. **Arquitectura Híbrida de Gemini (Optimizada para Pixel 10/9 Pro XL y Tablet TCL)**:
   - **Modo Gemini Cloud (`Google GenAI SDK` — ej. `gemini-2.5-flash`)**:
     - Permite configurar la API Key de Gemini en Ajustes (guardada de forma segura en `EncryptedSharedPreferences` / DataStore) para consultas complejas, análisis de fotos de menús japoneses, carteles de estaciones o sugerencias de itinerario.
   - **Modo Gemini Nano On-Device (`AICore` en Pixel 10 Pro XL y Pixel 9 Pro XL)**:
     - Aprovecha el procesador Tensor G4 / G5 de los **Pixel 9 Pro XL y Pixel 10 Pro XL** para ejecutar inferencia local sin conexión cuando esté disponible en el dispositivo.
   - **Persistencia y Caché Offline de Conversaciones e Insights de IA**:
     - Todas las respuestas, explicaciones culturales, traducciones de menús y rutas generadas por Gemini se guardan automáticamente en la base de datos local (`Room`) para poder consultarlas después en el metro o avión sin conexión, e incluso **exportarlas con un toque como una nueva Guía de Viaje (`SPEC-03`) o como paradas del Planificador (`SPEC-04`)**.
2. **Funciones Contextuales de Gemini en la App**:
   - **Asistente de Menús y Productos Japoneses (Visión)**: Haz una foto a la carta de un *Izakaya* en japonés o a un producto de farmacia/supermercado y Gemini te explica cada plato, ingredientes, precios convertidos a `EUR` (usando la tasa cacheada de `SPEC-02`) y cómo pedirlo.
   - **Optimizador Inteligente de Itinerario y Transporte**: Le puedes pedir a Gemini *"Organízame una tarde lluviosa en Kioto cerca de Gion"* y guardar sus paradas propuestas directamente en el **Planificador de Viaje (`SPEC-04`)**.
   - **Consultor de Etiqueta, Trenes y Costumbres en Japón**: Respuestas rápidas contextualizadas para el viajero.

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

### Escenario 3: Generar recomendaciones con Gemini y enviarlas al Planificador o Guías
- **Dado** que el usuario consulta a Gemini qué ver en 4 horas en Uji
- **Cuando** recibe la respuesta estructurada y pulsa *"Guardar como Guía de Viaje"* o *"Añadir al Planificador"*
- **Entonces** la información se guarda en la base de datos local de Guías (`SPEC-03`) o de Horarios (`SPEC-04`) disponible 100% offline.
