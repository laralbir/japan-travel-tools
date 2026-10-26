# SPEC-02: Herramienta de Conversor de Divisa Offline-First (EUR <-> JPY)

- **ID**: `SPEC-02`
- **Estado**: `Aprobado para Análisis/Implementación`
- **Versión Objetivo**: `0.0.1-beta.X` -> `0.0.1`

---

## 1. Objetivo

Ofrecer un conversor de moneda rápido, bidireccional y **100% funcional offline**, configurado por defecto para convertir entre **Euro (`EUR`)** y **Yen Japonés (`JPY`)**, con capacidad de descargar y cachear tipos de cambio actualizados desde una fuente open-source gratuita (**Frankfurter API**, basada en datos oficiales del Banco Central Europeo).

---

## 2. Requisitos Funcionales (RF)

### RF-02.1: Conversión Bidireccional Instantánea (`EUR <-> JPY` por defecto)
1. Por defecto, la divisa base será **Euro (`EUR`)** y la divisa destino **Yen Japonés (`JPY`)**.
2. El usuario podrá:
   - Escribir un importe en cualquiera de las dos divisas y ver el resultado en tiempo real en la otra.
   - Pulsar un botón de intercambio rápido (`Swap ⇅`) para invertir el sentido de conversión (`JPY -> EUR` o `EUR -> JPY`).
   - Cambiar cualquiera de las dos divisas por otras soportadas (ej. `USD`, `GBP`, `MXN`, `CHF`, `AUD`, `CAD`, etc.).
3. Incluirá botones de importes rápidos habituales en Japón (ej. `¥100`, `¥500`, `¥1,000`, `¥5,000`, `¥10,000`) para consulta exprés en tiendas y restaurantes.

### RF-02.2: Fuente de Datos Open-Source (Frankfurter API)
1. La obtención de tasas de cambio en línea se realizará mediante la API pública y open-source **Frankfurter** (`https://api.frankfurter.dev/v1/latest?base=EUR`).
2. No requiere clave de API (API Key), es código abierto y publica las tasas de referencia diarias del Banco Central Europeo.

### RF-02.3: Caché Local Persistente y Modo Offline-First
1. **Datos Semilla (`Seed Rates`)**:
   - La app incluirá un conjunto de tasas semilla pre-empaquetadas (ej. tasa de referencia `EUR -> JPY`) para que el conversor funcione incluso si el usuario instala y abre la app por primera vez sin internet.
2. **Persistencia automática**:
   - Cada vez que se descarguen tasas actualizadas desde Frankfurter API, se guardarán en base de datos local (`Room` / `DataStore`) junto con:
     - Fecha de publicación de la tasa (`rateDate`).
     - Timestamp exacto de la última sincronización (`lastSyncedAt`).
3. **Estrategia de actualización y uso offline**:
   - Al abrir el conversor, la UI carga **inmediatamente** las tasas cacheadas en disco (latencia cero).
   - En segundo plano (si hay red y la caché tiene más de X horas, o cuando el usuario pulsa el botón `Actualizar 🔄`), solicita los datos nuevos a Frankfurter API y actualiza la caché y la UI.
   - Si no hay conexión a internet, muestra un aviso no intrusivo indicando: *"Modo Offline — Usando cambio guardado del [Fecha]"*.
4. **Tasa personalizada manual (Override opcional)**:
   - El usuario podrá introducir manualmente un tipo de cambio personalizado si desea usar el cambio exacto al que compró efectivo o el de su tarjeta bancaria, pudiendo volver al cambio oficial cacheado en cualquier momento.

### RF-02.4: Sincronización Periódica en Segundo Plano (`WorkManager`)
1. **Actualización Automática Desatendida**:
   - Integración de `CurrencySyncWorker` mediante **Android Jetpack WorkManager** (`PeriodicWorkRequestBuilder`) con periodicidad de 24 horas para mantener los ratios actualizados automáticamente.
   - Restricción estricta de red (`NetworkType.CONNECTED`) para evitar consumo de batería o llamadas innecesarias en Modo Avión o sin red.
   - Persistencia automática de las nuevas tasas en `SharedCurrencyRateStore` (`saved_currency_rates.json`), garantizando que al abrir la app tras días de viaje las tasas se mantengan actualizadas de forma desatendida.

---

## 3. Criterios de Aceptación (Gherkin / BDD)

### Escenario 1: Conversión por defecto en el primer arranque sin red
- **Dado** que la app acaba de instalarse y el dispositivo no tiene internet
- **Cuando** el usuario abre el Conversor de Divisas e introduce `1000` en `JPY`
- **Entonces** la app calcula el equivalente en `EUR` usando la tasa semilla preinstalada e indica que es una tasa base offline.

### Escenario 2: Descarga y persistencia de tasas actualizadas
- **Dado** que el dispositivo tiene conexión a internet
- **Cuando** el usuario pulsa "Actualizar tasas" o abre el conversor con caché expirada
- **Entonces** la app descarga las tasas desde `api.frankfurter.dev`, las persiste en el almacenamiento local, actualiza la fecha de última sincronización y recalcula el importe en pantalla.

### Escenario 3: Uso posterior sin conexión tras haber sincronizado (Persistencia entre reinicios)
- **Dado** que la app descargó previamente un cambio de `1 EUR = 163.50 JPY`
- **Cuando** el usuario activa el Modo Avión, cierra la app, la vuelve a abrir y convierte `10 EUR`
- **Entonces** la app carga inmediatamente la tasa persistida de `163.50 JPY` desde disco (sin volver a la semilla inicial) y obtiene `1,635 JPY`.

### Escenario 4: Sincronización periódica automática en segundo plano
- **Dado** que el dispositivo se conecta a una red Wi-Fi o móvil
- **Cuando** WorkManager ejecuta la tarea programada `CurrencySyncWorker`
- **Entonces** se descargan las últimas tasas del BCE, se guardan en el archivo persistente y al abrir la app la fecha de sincronización refleja el último ratio actualizado sin intervención del usuario.
