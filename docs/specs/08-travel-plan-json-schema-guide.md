# Guía de Especificación JSON del Plan de Viaje (`japan-travel-plan.v1`)

Esta guía define la estructura estricta del archivo JSON que la aplicación **Japan Travel Tools** es capaz de exportar e importar sin conexión (`SPEC-04 RF-04.6` y `SPEC-05 RF-05.4`).

Cualquier modelo de Inteligencia Artificial (Gemini, ChatGPT, Claude) o herramienta externa puede utilizar este formato para generar itinerarios de viaje por Japón, trayectos de Shinkansen, vuelos y entradas que el usuario puede importar directamente a su teléfono.

---

## 1. Prompt Recomendado para Solicitar a una IA la Creación del Plan

Puedes copiar y pegar este prompt en cualquier IA (Gemini, ChatGPT, Claude) junto con tus preferencias de viaje:

```text
Actúa como un experto en viajes a Japón y genera un plan de viaje completo en formato JSON compatible con "japan-travel-plan.v1" para la aplicación Japan Travel Tools.

Requisitos estrictos para la salida:
1. Devuelve ÚNICAMENTE el bloque JSON válido, sin explicaciones ni texto adicional antes o después.
2. "schemaVersion" debe ser 1.
3. En "itineraryStops", cada parada debe incluir su día correlativo (dayNumber: 1, 2, 3...), fecha ISO (dateIso: "YYYY-MM-DD"), horarios sin solapamientos en formato "HH:mm" (startTime y endTime en hora de Japón), título en español/inglés, nombre real en kanji/japonés (placeNameJapanese), coordenadas geográficas (latitude, longitude) y categoría ("SIGHTSEEING", "FOOD", "ACTIVITY", "HOTEL", "SHOPPING", "TRANSPORT").
4. En "transportSegments", incluye vuelos, trenes Shinkansen (ej. Nozomi/Hikari), líneas JR o metros, con horas de salida/llegada en formato ISO "YYYY-MM-DDTHH:mm", códigos de estación ("TYO", "KYO", "OSA", etc.), andén y notas de vagón/asiento.

Mi solicitud de viaje es:
[Escribe aquí tus ciudades, fechas y lugares deseados, ej: 4 días entre Tokio y Kioto del 11 al 14 de noviembre de 2026]
```

---

## 2. Esquema Técnico de Campos

El objeto raíz contiene:
- `schemaVersion` (entero, obligatorio): `1`
- `tripTitle` (cadena, obligatorio): Título del viaje (ej. `"Japón Otoño 2026"`).
- `itineraryStops` (array de paradas):
  - `id` (cadena, obligatorio): Identificador único (ej. `"stop-tokyo-1"`).
  - `dayNumber` (entero, obligatorio): Día del viaje (`1`, `2`, `3`...).
  - `dateIso` (cadena, obligatorio): Fecha en formato `"YYYY-MM-DD"` (ej. `"2026-11-11"`).
  - `startTime` (cadena, obligatorio): Hora de inicio en formato `"HH:mm"` (ej. `"09:00"`).
  - `endTime` (cadena, obligatorio): Hora de finalización en formato `"HH:mm"` (ej. `"10:30"`).
  - `timeZoneId` (cadena, opcional, por defecto `"Asia/Tokyo"`): Identificador de zona horaria IANA del lugar (ej. `"Europe/Madrid"`, `"Europe/Berlin"`, `"Asia/Tokyo"`).
  - `startTimeUtcIso` (cadena ISO UTC, opcional): Momento exacto de inicio en UTC con formato `"YYYY-MM-DDTHH:mm:ssZ"` (ej. `"2027-03-15T08:00:00Z"`).
  - `endTimeUtcIso` (cadena ISO UTC, opcional): Momento exacto de finalización en UTC con formato `"YYYY-MM-DDTHH:mm:ssZ"`.
  - `title` (cadena, obligatorio): Nombre del lugar o actividad.
  - `category` (cadena, obligatorio): Una de las siguientes categorías válidas:
    - `"SIGHTSEEING"` (Templos, miradores, parques, monumentos)
    - `"FOOD"` (Restaurantes, ramen, mercados gastronómicos)
    - `"ACTIVITY"` (Ceremonia del té, onsen, museos, entradas)
    - `"HOTEL"` (Check-in, check-out de alojamiento)
    - `"SHOPPING"` (Compras, souvenirs, electrónica, Tax-Free)
    - `"TRANSPORT"` (Traslados locales)
  - `placeNameJapanese` (cadena): Nombre en japonés o kanji para taxistas y Google Maps (ej. `"浅草寺"`).
  - `latitude` (decimal): Coordenada latitud (ej. `35.7147`).
  - `longitude` (decimal): Coordenada longitud (ej. `139.7966`).
  - `notes` (cadena): Consejos, salidas de metro recomendadas o información de reserva.
- `transportSegments` (array de tramos de transporte):
  - `id` (cadena, obligatorio): Identificador único del tramo (ej. `"seg-shinkansen-1"`).
  - `order` (entero): Orden cronológico del tramo (`1`, `2`...).
  - `type` (cadena, obligatorio):
    - `"FLIGHT"` (Vuelo internacional o doméstico)
    - `"TRAIN_SHINKANSEN"` (Tren bala Shinkansen o JR Express)
    - `"SUBWAY_METRO"` (Líneas de metro de Tokio/Kioto/Osaka)
    - `"BUS"` (Autobús interurbano o urbano)
  - `operatorName` (cadena): Operador (ej. `"JR Central"`, `"Tokyo Metro"`, `"Iberia"`).
  - `lineOrNumber` (cadena): Nombre de línea o número de vuelo/tren (ej. `"Nozomi #21"`, `"IB6800"`).
  - `departureCode` (cadena): Código de 3 letras de la estación o aeropuerto (ej. `"TYO"`, `"MAD"`).
  - `departureName` (cadena): Nombre de la estación de origen (ej. `"Tokyo Station"`).
  - `departureZoneId` (cadena): Zona horaria (ej. `"Asia/Tokyo"`).
  - `departureTerminalOrPlatform` (cadena): Andén o terminal (ej. `"Andén 16"`, `"Terminal 4S"`).
  - `departureTimeIso` (cadena ISO): Fecha y hora de salida `"YYYY-MM-DDTHH:mm"`.
  - `departureTimeUtcIso` (cadena ISO UTC, opcional): Fecha y hora exacta de salida en UTC `"YYYY-MM-DDTHH:mm:ssZ"`.
  - `arrivalCode` (cadena): Código de 3 letras de llegada (ej. `"KYO"`, `"HND"`).
  - `arrivalName` (cadena): Nombre de la estación de llegada (ej. `"Kyoto Station"`).
  - `arrivalZoneId` (cadena): Zona horaria de llegada (ej. `"Asia/Tokyo"`).
  - `arrivalTerminalOrPlatform` (cadena): Andén o terminal de llegada (ej. `"Andén 13"`).
  - `arrivalTimeIso` (cadena ISO): Fecha y hora de llegada `"YYYY-MM-DDTHH:mm"`.
  - `arrivalTimeUtcIso` (cadena ISO UTC, opcional): Fecha y hora exacta de llegada en UTC `"YYYY-MM-DDTHH:mm:ssZ"`.
  - `seatOrCarInfo` (cadena): Vagón y asiento o clase (ej. `"Vagón 7 · Asiento 12E"`).
  - `stationExitOrBelt` (cadena): Salida de estación o cinta de equipajes (ej. `"Salida Central"`).
  - `passOrPaymentBadge` (cadena): Cobertura o método de pago (ej. `"JR Pass"`, `"Suica"`, `"Billete"`).
- `guides` (array opcional de guías de viaje integradas):
  - `id` (cadena, obligatorio): Identificador único (ej. `"guide-sellos-goshuin"`).
  - `title` (cadena, obligatorio): Título de la guía o manual práctico.
  - `cityRegion` (cadena): Región o ciudad asociada (ej. `"General"`, `"Tokyo"`, `"Kyoto"`, `"Osaka"`).
  - `tags` (array de cadenas): Etiquetas temáticas (ej. `["Sellos", "Templos", "Eki Stamp"]`).
  - `markdownSummary` (cadena): Contenido en formato Markdown con instrucciones paso a paso.
  - `photoCount` (entero, opcional): Número de imágenes asociadas.
  - `isPinned` (booleano, opcional): Si la guía debe mostrarse fijada en la parte superior.
- `documents` (array opcional de billetes, reservas y documentos adjuntos):
  - `id` (cadena, obligatorio): Identificador único del documento (ej. `"doc-ave-vlc-mad"`).
  - `title` (cadena, obligatorio): Nombre descriptivo (ej. `"Billete AVE Valencia ➔ Madrid"`).
  - `category` (cadena, obligatorio): `"TRANSPORT"`, `"FLIGHT"`, `"HOTEL"`, `"TICKET"`, `"INSURANCE"`, `"PASSPORT"`, `"NATIONAL_ID"`, `"GUIDE"`.
  - `referenceOrNotes` (cadena): Asiento, vagón, localizador o notas.
  - `isPinned` (booleano, opcional): Si debe fijarse al inicio del repositorio.
  - `fileUri` (cadena o null): Ruta relativa o URI interna del archivo adjunto (ej. `"documents/billete.pkpass"`).
  - `locatorCode` (cadena o null): Código PNR o localizador de reserva.
  - `barcodeMessage` (cadena o null): Payload para generar el código QR o de barras offline.
  - `barcodeFormat` (cadena o null): Formato del código (`"PKBarcodeFormatAztec"`, `"QR_CODE"`, `"CODE_128"`).
  - `barcodeAltText` (cadena o null): Texto legible alternativo bajo el código.
  - `linkedTransportSegmentId` (cadena o null): ID del tramo de transporte vinculado en el plan.

---

## 3. Ejemplo Completo Válido para Importar (Paquete Unificado Todo en Uno)

```json
{
  "schemaVersion": 1,
  "tripTitle": "Viaje a Japón 2026",
  "itineraryStops": [
    {
      "id": "stop-tokyo-1",
      "dayNumber": 1,
      "dateIso": "2026-11-11",
      "startTime": "09:00",
      "endTime": "11:00",
      "title": "Templo Senso-ji y Calle Nakamise",
      "category": "SIGHTSEEING",
      "placeNameJapanese": "浅草寺 · 仲見世通り",
      "latitude": 35.7147,
      "longitude": 139.7966,
      "notes": "Metro Ginza Line hasta Asakusa (Salida 1). Probar melonpan recién horneado."
    },
    {
      "id": "stop-tokyo-2",
      "dayNumber": 1,
      "dateIso": "2026-11-11",
      "startTime": "11:30",
      "endTime": "13:30",
      "title": "Parque Ueno y Mercado Ameyoko",
      "category": "SHOPPING",
      "placeNameJapanese": "上野恩賜公園 · アメヤ横丁",
      "latitude": 35.7118,
      "longitude": 139.7744,
      "notes": "Paseo entre puestos de comida callejera y compras Tax-Free."
    },
    {
      "id": "stop-kyoto-1",
      "dayNumber": 2,
      "dateIso": "2026-11-12",
      "startTime": "08:00",
      "endTime": "10:30",
      "title": "Santuario Fushimi Inari Taisha",
      "category": "SIGHTSEEING",
      "placeNameJapanese": "伏見稲荷大社",
      "latitude": 34.9671,
      "longitude": 135.7727,
      "notes": "Llegar temprano para subir los mil toriis rojos sin multitudes."
    }
  ],
  "transportSegments": [
    {
      "id": "seg-shinkansen-tokyo-kyoto",
      "order": 1,
      "type": "TRAIN_SHINKANSEN",
      "operatorName": "JR Central",
      "lineOrNumber": "Shinkansen Nozomi #21",
      "departureCode": "TYO",
      "departureName": "Tokyo Station",
      "departureZoneId": "Asia/Tokyo",
      "departureTerminalOrPlatform": "Andén 16",
      "departureTimeIso": "2026-11-12T06:30",
      "arrivalCode": "KYO",
      "arrivalName": "Kyoto Station",
      "arrivalZoneId": "Asia/Tokyo",
      "arrivalTerminalOrPlatform": "Andén 13",
      "arrivalTimeIso": "2026-11-12T08:45",
      "seatOrCarInfo": "Vagón 7 Asiento 12E (Ventanilla con vistas al Monte Fuji)",
      "stationExitOrBelt": "Salida Central",
      "passOrPaymentBadge": "SmartEX / JR Pass"
    }
  ],
  "guides": [
    {
      "id": "guide-sellos-goshuin",
      "title": "Guía de Sellos de Japón (Goshuin vs Eki Stamp)",
      "cityRegion": "General",
      "tags": ["Sellos", "Goshuin", "Eki Stamp"],
      "markdownSummary": "• Goshuin: Libro Goshuincho en templos/santuarios (¥300-¥1,000).\n• Eki Stamp: Sellos gratuitos en estaciones JR y miradores con tu propia libreta."
    }
  ],
  "documents": [
    {
      "id": "doc-vuelo-mad-hnd",
      "title": "Billete Madrid ➔ Tokio Haneda",
      "category": "FLIGHT",
      "referenceOrNotes": "Vuelo IB6801 · Localizador ABC123",
      "isPinned": true
    }
  ]
}
```
