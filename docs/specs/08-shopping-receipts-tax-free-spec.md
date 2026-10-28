# SPEC-08: Tickets de Compra, Tax-Free y Control de Gastos del Viaje con Escaneo Inteligente (Local y Cloud IA)

- **ID**: `SPEC-08`
- **Estado**: `Aprobado para Análisis/Implementación`
- **Versión**: `0.0.1-beta.1`
- **Módulo**: `feature.taxfree`
- **Idiomas de UI**: Inglés (`en`, por defecto) y Español (`es`)
- **Dependencias**: `SPEC-00` (Core, Horas ISO UTC, Persistencia Offline-First, SAF), `SPEC-02` (Conversor Divisa EUR <-> JPY), `SPEC-06` (Gemini API y ML Kit OCR)

---

## 1. Objetivo

Dotar a **Japan Travel Tools** de un sistema integral y 100% offline-first para:
1. **Digitalización y Gestión de Tickets de Compra**: Registrar comprobantes de compras realizadas durante el viaje a Japón mediante entrada manual o captura y escaneo inteligente de fotos (desde cámara local, Google Drive o almacenamiento del dispositivo).
2. **Cálculo y Gestión de Devolución de Impuestos (Tax-Free Japonés)**:
   - Identificar automáticamente la elegibilidad de compras para el régimen *Tax-Free* en Japón (umbral de 5.000 ¥ sin impuestos en el mismo día y comercio).
   - Distinguir entre consumibles (*Consumables*, tasa reducida 8%), bienes generales (*General Goods*, tasa estándar 10%) o compras mixtas.
   - Generar informes consolidados de Tax-Free agrupados por tienda y fecha, con totales de impuestos recuperables en yenes (`¥`) y su contravalor en euros (`EUR`), seguimiento de estado ante la aduana japonesa (*Pending*, *Customs Stamped*, *Refunded*) y exportación en formatos legibles (Texto, Markdown y JSON).
3. **Control de Presupuesto y Gastos del Viaje**:
   - Acumulado global del gasto en yenes y en euros según el tipo de cambio oficial o personalizado.
   - Desglose por categorías de gasto (*Compras, Comida/Restauración, Transporte, Ocio/Entradas, Alojamiento, Otros*).
   - Balance analítico agrupado por día/fecha y por ciudad/localidad (Tokio, Kioto, Osaka, etc.).
4. **Escáner Inteligente Híbrido con IA (Local Offline + Nube Gemini)**:
   - **Local Offline**: Extracción de texto con **ML Kit Text Recognition Japanese** y análisis semántico determinista (`JapaneseReceiptOcrParser`) de tickets de cadenas habituales japonesas (*Don Quijote*, *Bic Camera*, *Yodobashi*, *Uniqlo*, *7-Eleven*, *FamilyMart*, *Lawson*).
   - **Nube / Multimodal con Gemini**: Análisis visual estructurado con **Google Gemini Vision API** utilizando la clave de API configurada en la aplicación, extrayendo comercio, fecha, desglose de artículos, precios y tasas directamente a JSON. Si el dispositivo no tiene red o no hay clave, conmuta automáticamente y sin errores al motor local.

---

## 2. Requisitos Funcionales (RF)

### RF-08.1: Modelo de Datos del Ticket de Compra (`ShoppingReceipt`)
Cada comprobante registrado en la aplicación debe contener de forma estructurada:
1. **Identificador único (`id`)**: UUID único en formato string.
2. **Fecha y hora de compra (`purchaseDate`)**:
   - Almacenado como `Instant` en formato ISO 8601 UTC (ej. `2027-03-20T05:30:00Z`).
   - Formateado y visualizado para el usuario en la zona horaria del lugar de compra (por defecto `Asia/Tokyo`, UTC+9).
3. **Comercio (`storeName`)**: Nombre de la tienda o establecimiento (ej. "Don Quijote Dotonbori", "Bic Camera Shinjuku", "Uniqlo Ginza").
4. **Localización / Ciudad (`location`)**: Localidad o ciudad japonesa donde se realizó la compra (ej. "Osaka", "Tokyo", "Kyoto").
5. **Lista de artículos comprados (`items: List<ReceiptProductItem>`)**:
   - `id`: Identificador del ítem.
   - `name`: Nombre o descripción del producto en alfabeto latino o caracteres japoneses (Kanji/Kana).
   - `quantity`: Cantidad de unidades (entero >= 1).
   - `unitPriceYen`: Precio unitario en yenes (entero o doble >= 0).
   - `taxRatePercent`: Porcentaje impositivo aplicable en Japón:
     * `8.0`%: Tasa reducida (*Keigen Zeiritsu*) para alimentación, bebidas y consumibles comestibles.
     * `10.0`%: Tasa general estándar para electrónica, ropa, accesorios y bienes generales.
   - `subtotalWithoutTaxYen`: Subtotal de la línea sin impuestos (`quantity * unitPriceYen` si el precio es base, o calculado a partir del desglose).
   - `taxYen`: Importe impositivo de la línea.
   - `totalWithTaxYen`: Total con impuestos incluidos.
6. **Totalizaciones del Comprobante**:
   - `subtotalWithoutTaxYen`: Suma total de bases imponibles sin tasas en yenes.
   - `totalTaxYen`: Suma total de las tasas impositivas calculadas en yenes.
   - `totalWithTaxYen`: Total final pagado en yenes (`subtotalWithoutTaxYen + totalTaxYen`).
7. **Clasificación de Bienes (`goodsType`)**:
   - `CONSUMABLES`: Bienes consumibles (alimentos, cosméticos, golosinas, medicamentos).
   - `GENERAL_GOODS`: Bienes duraderos (ropa, electrónica, calzado, figuras, recuerdos duraderos).
   - `MIXED`: Compra que contiene tanto consumibles como bienes generales.
8. **Elegibilidad Tax-Free (`isTaxFreeEligible`)**:
   - Booleano calculado conforme a la legislación de aduanas de Japón: compras del mismo día en el mismo establecimiento cuyo importe total sin impuestos (`subtotalWithoutTaxYen`) sea igual o superior a **5.000 ¥**.
9. **Estado de Solicitud de Tax-Free (`taxFreeStatus`)**:
   - `NOT_APPLICABLE`: Compra que no alcanza el umbral de 5.000 ¥ o no califica para exención.
   - `PENDING`: Compra elegible pendiente de presentar o registrar en aduana/tienda.
   - `CUSTOMS_STAMPED`: Compra sellada/registrada ante la aduana japonesa (*Visit Japan Web Tax-Free QR* o pasaporte).
   - `REFUNDED`: Impuesto devuelto con éxito o descontado en el momento de la compra.
10. **Categoría de Gasto (`category: ExpenseCategory`)**:
    - `SHOPPING` (Compras y recuerdos / Omiyage)
    - `FOOD` (Comida, restaurantes, konbini y cafeterías)
    - `TRANSPORT` (Billetes de tren, metro, taxis, pases)
    - `LEISURE` (Entradas a atracciones, templos, museos, ocio)
    - `ACCOMMODATION` (Hoteles, ryokan, hostales)
    - `OTHER` (Servicios, envíos, imprevistos)
11. **Foto del Comprobante Adjunta (`photoFilePath`)**:
    - Ruta interna o URI del archivo de imagen del ticket (capturado por cámara con `FileProvider`, importado desde almacenamiento local o Google Drive).
12. **Notas y Etiquetas Adicionales (`notes`)**: Texto opcional para anotaciones personales del viajero.

---

### RF-08.2: Reglas de Elegibilidad y Devolución Tax-Free de Japón
1. **Umbral Mínimo**:
   - La exención fiscal exige un gasto acumulado en el mismo establecimiento durante la misma jornada de al menos **5.000 ¥ sin impuestos** (`>= 5000 JPY`).
2. **Tratamiento según tipo de bienes**:
   - **Bienes Generales (`GENERAL_GOODS`)**: No consumibles en Japón; deben ser extraídos del país en un plazo de 6 meses.
   - **Consumibles (`CONSUMABLES`)**: No deben ser consumidos dentro de Japón; deben ser embolsados en bolsas de seguridad selladas y salir del país en un plazo de 30 días.
   - **Compras Mixtas (`MIXED`)**: Si se combinan consumibles y bienes generales para alcanzar los 5.000 ¥, todos los bienes quedan sujetos a las reglas de los consumibles (bolsa precintada).
3. **Cálculo de Impuesto Recuperable**:
   - El impuesto recuperable equivale exactamente al valor de `totalTaxYen` de todas las compras elegibles con estado `PENDING` o `CUSTOMS_STAMPED`.
   - Se muestra tanto en yenes (`¥`) como en euros (`EUR`), aplicando la tasa de cambio vigente provista por `SPEC-02` (`CurrencyConverterUseCase` / Frankfurter API).

---

### RF-08.3: Generación y Exportación del Informe Tax-Free
1. **Agrupación y Desglose**:
   - El generador de informes (`TaxFreeReportGenerator`) agrupa los tickets elegibles por **tienda / establecimiento** y por **fecha**.
   - Proporciona totales acumulados:
     * Total compras brutas (con impuestos).
     * Total base imponible (sin impuestos).
     * Total impuesto recuperable en `¥` y en `EUR`.
     * Conteo de tickets por estado (`PENDING`, `CUSTOMS_STAMPED`, `REFUNDED`).
2. **Formatos de Exportación Soportados**:
   - **Texto Plano (`Text`)**: Resumen conciso para copiar rápidamente o enviar por mensajería.
   - **Markdown (`Markdown`)**: Documento estructurado con tablas, encabezados claros y desglose por tienda, apto para guardar en la Bóveda de Guías (`SPEC-03`).
   - **JSON (`JSON`)**: Esquema estructurado y portable según las directrices de `SPEC-08` / `SPEC-00`, compatible con importación y exportación mediante Storage Access Framework (SAF) y Google Drive.

---

### RF-08.4: Control y Presupuesto de Gastos del Viaje (`TravelBudgetSummary`)
1. **Métricas Globales**:
   - Gasto total del viaje acumulado en yenes (`¥`).
   - Gasto total equivalente en euros (`EUR`).
   - Total de impuestos recuperables pendientes de cobro (`¥` y `EUR`).
2. **Desglose Analítico por Categoría**:
   - Importe en `¥` y `EUR` para cada una de las 6 categorías (`SHOPPING`, `FOOD`, `TRANSPORT`, `LEISURE`, `ACCOMMODATION`, `OTHER`).
   - Porcentaje relativo de cada categoría sobre el presupuesto total gastado.
3. **Desglose por Día / Fecha**:
   - Acumulado de gasto día a día para identificar picos de consumo durante el itinerario.
4. **Desglose por Ciudad / Destino**:
   - Acumulado de gasto por ciudad (Tokio, Kioto, Osaka, etc.) facilitando el control de costes por etapa del viaje.

---

### RF-08.5: Escaneo Inteligente Híbrido (Local Offline + Cloud IA) y Fuentes de Entrada
1. **Selector de Origen del Ticket (Cámara, Local y Google Drive)**:
   - El sistema proporciona un selector explícito con opciones claras:
     * `📷 Cámara de Fotos`: Captura directa mediante la cámara del dispositivo.
     * `📁 Almacenamiento Local / Galería`: Selección de fotos (JPG, PNG, WEBP) o documentos PDF almacenados en el teléfono.
     * `☁️ Google Drive`: Acceso a tickets y facturas PDF o imágenes almacenadas en la nube a través del proveedor SAF (`Storage Access Framework`).
   - **Renderizado y Decodificación Universal de Documentos PDF (`PdfRenderer`)**: Cuando el comprobante seleccionado es un archivo PDF, el motor rasteriza automáticamente la primera página a un `Bitmap` de alta resolución para alimentar al OCR local o al modelo multimodal de IA sin dependencias de terceros.

2. **Motor Local Offline Trilingüe (`JapaneseReceiptOcrParser` + ML Kit)**:
   - Procesamiento sin conexión de texto extraído por Google ML Kit Text Recognition con soporte completo para recibos en **Japonés, Inglés y Español**:
     * **Japonés**: Cadenas populares (Don Quijote `ドン・キホーテ`, Bic Camera `ビックカメラ`, Yodobashi `ヨドバシ`, Uniqlo `ユニクロ`, 7-Eleven `セブン-イレブン`, FamilyMart, Lawson), era Reiwa (`令和` / `R7`), `合計` (Total), `小計` (Subtotal), `内消費税` (Impuestos), `免税` / `TAX FREE`.
     * **Inglés**: Recibos duty-free o internacionales (`TAX FREE SHOP`, `DUTY FREE`, `RECEIPT`, `SUBTOTAL`, `TAX`, `VAT`, `TOTAL AMOUNT`, `GRAND TOTAL`, `ITEMS`).
     * **Español**: Comprobantes de agencias o comercios hispanohablantes (`FACTURA SIMPLIFICADA`, `TICKET DE VENTA`, `BASE IMPONIBLE`, `IMPORTE NETO`, `CUOTA IVA`, `IVA 10%`, `TOTAL FACTURA`, `TOTAL A PAGAR`).
     * Detección de fechas en formato ISO (`YYYY/MM/DD`), europeo (`DD/MM/YYYY`) y era japonesa (`R07/03/15`).
     * Clasificación automática de categorías en los 3 idiomas (`Comida/Food`, `Transporte/Transport`, `Compras/Shopping`, `Alojamiento/Accommodation`, `Ocio/Leisure`).

3. **Motor Cloud IA (`Gemini Vision Multimodal`)**:
   - Cuando existe conexión a internet y una Gemini API Key válida (`SPEC-06`), el sistema permite enviar la imagen del ticket directamente a Gemini Vision con un prompt estricto solicitando un esquema JSON validado con los campos: `storeName`, `dateIso`, `location`, `items`, `subtotalWithoutTaxYen`, `taxYen`, `totalWithTaxYen`, `category` y `goodsType`.
   - **Fallback Automático**: Si la llamada a Gemini Cloud falla por cuota (HTTP 429), falta de red o error de red, el sistema recurre inmediatamente y sin interrupción al motor OCR local offline.

4. **Diseño de Interfaz Consistente (Filas de Botones Homogéneas)**:
   - Todas las filas de botones de acción presentan una altura unificada idéntica de `48.dp` con esquinas redondeadas (`12.dp`), eliminando botones huérfanos con iconos desconocidos y ofreciendo etiquetas de texto autoexplicativas con icono descriptivo:
     * Fila 1: `➕ Ticket Manual` (Filled Primary) y `✨ Escanear con IA` (Filled Primary).
     * Fila 2: `📁 Local / ☁️ Drive` (Outlined Secondary) y `📄 Informe Aduanas` (Outlined Secondary).

### RF-08.6: Soporte Multidivisa (Yen JPY, Euro EUR y Dólar USD) con Detección Inteligente
1. **Detección Automática de Moneda en OCR**:
   - El analizador examina los símbolos y códigos monetarios en el texto extraído:
     * **Yen Japonés (`JPY`)**: Símbolos `¥`, `￥`, kanji `円`, o cadenas y términos reconocidos en japonés.
     * **Euro (`EUR`)**: Símbolo `€`, códigos `EUR`, palabras `Euro` / `Euros`.
     * **Dólar Estadounidense (`USD`)**: Símbolo `$`, códigos `USD`, `US$`, palabras `Dollar` / `Dollars`.
2. **Pregunta Interactiva al Usuario si la Divisa es Ambigua**:
   - Si el comprobante contiene cantidades numéricas genéricas sin símbolo monetario explícito o presenta símbolos conflictivos, el motor marca la divisa como no identificada (`isCurrencyAmbiguous = true`).
   - La aplicación muestra inmediatamente el diálogo emergente `ReceiptCurrencySelectionDialog`:
     * "¿Cuál es la moneda del ticket?" con opciones `💴 ¥ JPY`, `💶 € EUR` y `💵 $ USD`.
     * Al pulsar la opción elegida, se recalculan los importes y equivalencias en Yen según los tipos de cambio vigentes antes de abrir el editor.
3. **Selector Manual en el Editor y Tarjetas de Gasto**:
   - El editor manual/modal incluye un selector de moneda (`FilterChips`) para cambiar la divisa en cualquier momento.
   - En las tarjetas del listado se muestra el importe en la moneda nativa del ticket (`22,00 €` o `$55.00`) junto con su contravalor equivalente en yenes (`≈ ¥3.630` o `≈ ¥8.250`).
   - En el cuadro de mando presupuestario global, todos los gastos se normalizan homogéneamente a ¥ y EUR.

---

## 3. Criterios de Aceptación (Escenarios Gherkin)

### Escenario 1: Añadir ticket manual y verificar cálculo de impuestos y elegibilidad Tax-Free
- **Dado** que el viajero realiza una compra en "Bic Camera Shinjuku" de 45.000 ¥ en electrónica (tasa 10%) y 6.000 ¥ en consumibles (tasa 8%)
- **Cuando** registra el ticket con ambos artículos
- **Entonces** el subtotal sin impuestos es 51.000 ¥, la tasa calculada es 4.980 ¥ (4.500 ¥ + 480 ¥) y el total con tasas es 55.980 ¥
- **Y** la compra es catalogada automáticamente como `isTaxFreeEligible = true`, con tipo `MIXED` y estado `PENDING`.

### Escenario 2: Compra que no alcanza el umbral de Tax-Free
- **Dado** que el viajero compra en "7-Eleven Shibuya" por importe de 1.200 ¥ en comida (tasa 8%)
- **Cuando** registra el ticket
- **Entonces** el subtotal sin impuestos es inferior a 5.000 ¥
- **Y** la compra es catalogada como `isTaxFreeEligible = false` y `taxFreeStatus = NOT_APPLICABLE`.

### Escenario 3: Generación de informe Tax-Free y conversión a EUR
- **Dado** que existen 3 tickets elegibles de diferentes tiendas con un impuesto recuperable conjunto de 8.200 ¥
- **Y** la tasa de cambio vigente es de 160.0 JPY por EUR
- **Cuando** el viajero solicita generar el informe de Tax-Free
- **Entonces** el informe refleja un impuesto recuperable de 8.200 ¥ (equivalente a 51.25 EUR)
- **Y** permite exportarlo en texto, Markdown y formato JSON.

### Escenario 4: Control de gastos por categorías y destinos
- **Dado** que el viajero ha registrado compras en Tokio y Kioto repartidas en `FOOD`, `SHOPPING` y `TRANSPORT`
- **Cuando** consulta el resumen presupuestario (`TravelBudgetSummary`)
- **Entonces** visualiza el total gastado en ¥ y EUR, los porcentajes por categoría y el balance desglosado por ciudad y fecha.

### Escenario 5: Escaneo de recibo japonés con fallback offline
- **Dado** que el viajero toma una foto de un recibo de "Don Quijote"
- **Cuando** no hay conexión a internet o no hay API Key de Gemini
- **Entonces** el motor local `JapaneseReceiptOcrParser` extrae el comercio, la fecha, los artículos con sus tasas del 8% y 10% y el total sin errores.

### Escenario 6: Detección automática de moneda (Euros o Dólares)
- **Dado** que el viajero escanea un ticket con importes en euros (`20,00 €`) o dólares (`$ 50.00 USD`)
- **Cuando** el OCR procesa la imagen
- **Entonces** detecta automáticamente la divisa (`EUR` o `USD`), almacena las cantidades originales y calcula el importe equivalente en yenes (`¥`).

### Escenario 7: Pregunta interactiva si la moneda es ambigua
- **Dado** que el viajero escanea un comprobante con texto e importes genéricos sin símbolo monetario (`10.00`)
- **Cuando** el motor OCR no puede inferir la moneda con certeza
- **Entonces** la aplicación muestra el diálogo "¿Cuál es la moneda del ticket?" para que el usuario elija entre JPY, EUR y USD.
