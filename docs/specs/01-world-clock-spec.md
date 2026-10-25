# SPEC-01: Herramienta de Relojes y Ubicación (Hora Local y País de Origen)

- **ID**: `SPEC-01`
- **Estado**: `Aprobado para Análisis/Implementación`
- **Versión Objetivo**: `0.0.1-beta.X` -> `0.0.1`

---

## 1. Objetivo

Proporcionar al viajero una herramienta visual, rápida y 100% offline para consultar simultáneamente:
1. La **hora local actual** (detectada mediante la ubicación del dispositivo o zona horaria actual, y siempre teniendo presente la hora de Japón `Asia/Tokyo`).
2. La **hora del país/ciudad de origen** seleccionada por el usuario (ej. `Europe/Madrid`, `America/Mexico_City`, `America/New_York`, etc.).

---

## 2. Requisitos Funcionales (RF)

### RF-01.1: Visualización de Relojes
1. La pantalla mostrará tarjetas de reloj sincronizadas en tiempo real (actualización cada segundo/minuto):
   - **Reloj de Ubicación Actual / Destino (Japón)**:
     - Obtiene la ubicación del dispositivo (`COARSE_LOCATION`) para determinar las coordenadas y resolver offline la zona horaria y nombre de región/ciudad aproximada (o usa el `TimeZone` del dispositivo como fallback inmediato si se deniega el permiso de ubicación).
     - Si el usuario aún no está físicamente en Japón, mostrará también una tarjeta rápida de referencia para **Japón (`Asia/Tokyo`, JST UTC+9)**.
   - **Reloj de País de Origen**:
     - Muestra la hora, fecha y día de la semana en el país de origen configurado por el usuario.
     - Valor por defecto inicial: `Europe/Madrid` (si el idioma es Español) o `UTC` / zona detectada inicial, totalmente configurable mediante un selector con buscador de países/zonas horarias (`java.time.ZoneId`).

### RF-01.2: Uso de Ubicación Offline-Compatible
1. La app solicitará permiso `ACCESS_COARSE_LOCATION` cuando el usuario abra o pulse "Detectar ubicación actual" en la herramienta de relojes.
2. Con la latitud/longitud obtenida del proveedor de ubicación del sistema (GPS / Red), la app determinará la zona horaria correspondiente mediante una tabla/convertidor offline integrado (sin requerir llamadas de red a APIs externas para saber el huso horario).
3. La última ubicación/zona horaria detectada se persistirá localmente.
4. Si el usuario deniega el permiso o tiene el GPS apagado, la herramienta seguirá funcionando usando la zona horaria del sistema o permitiendo selección manual sin bloquear la experiencia.

### RF-01.3: Comparador de Diferencia Horaria y Estado del Día
1. Mostrará explícitamente la diferencia de horas entre la ubicación actual/Japón y el país de origen (ej. `+7h` o `+8h` respecto a España según horario de verano/invierno DST).
2. Incluirá un indicador visual de franja horaria en el país de origen (Madrugada 🌙, Mañana 🌅, Tarde ☀️, Noche 🌆) para saber de un vistazo si es buena hora para llamar o escribir a familiares en origen.

### RF-01.4: Persistencia de Configuración de Relojes
1. El país/zona horaria de origen seleccionado, el formato de hora (12h / 24h) y cualquier reloj adicional fijado por el usuario se persistirán en `DataStore` / `Room`.

---

## 3. Criterios de Aceptación (Gherkin / BDD)

### Escenario 1: Detección de ubicación en Tokio y comparación con país de origen
- **Dado** que el usuario ha configurado `España (Europe/Madrid)` como país de origen
- **Cuando** la ubicación del dispositivo reporta coordenadas dentro de Japón (`35.6762, 139.6503`)
- **Entonces** el reloj local muestra la hora en `Asia/Tokyo (JST)`, el reloj de origen muestra la hora en `Europe/Madrid`, y se indica la diferencia horaria exacta teniendo en cuenta el cambio de hora (DST) europeo.

### Escenario 2: Funcionamiento sin permisos de ubicación y offline
- **Dado** que el usuario deniega el permiso de ubicación y está en modo avión
- **Cuando** accede a la herramienta de Relojes
- **Entonces** la app utiliza el huso horario del sistema para el reloj local, mantiene `Asia/Tokyo` y el país de origen persistido, y no produce ningún fallo.
