# SPEC-12: Alarmas y Notificaciones de Transporte en Segundo Plano con AlarmManager

- **ID**: `SPEC-12`
- **Estado**: `Aprobado para Análisis e Implementación`
- **Herramienta**: Herramienta 5 — Planificador de Transportes Multimodal (`SPEC-05`)
- **Referencias Cruzadas**:
  - [`SPEC-00`](00-core-configuration-spec.md) (Configuración Base y Arquitectura Offline-First)
  - [`SPEC-05`](05-flight-planner-spec.md) (Planificador de Transporte: Vuelos, Shinkansen, Trenes, Metro y Autobús)
  - [`ANALYSIS-14`](../analysis/14-transport-alarms-analysis.md) (Diseño Técnico con AlarmManager y BroadcastReceiver)

---

## 1. Contexto, Motivación y Objetivo

En la versión actual de **Japan Travel Tools**, el evaluador de alertas de transporte (`TransportAlertEvaluator`) calcula en tiempo real si un tramo está en período de facturación, embarque o salida inminente cuando el usuario tiene la aplicación abierta y en primer plano.

Sin embargo, durante un viaje real por Japón:
1. El viajero suele llevar el teléfono en el bolsillo con la pantalla bloqueada mientras camina por una estación masiva (como Shinjuku, Tokio o Kioto).
2. En trayectos largos de Shinkansen (como Tokio a Kioto o Hiroshima), es común descansar o dormir, corriendo el riesgo de pasarse de la estación de destino si no recibe una alarma oportuna.
3. El viajero no dispone necesariamente de conexión de datos activa y necesita que las alertas de check-in, embarque y llegada se ejecuten con **precisión milimétrica de forma 100% offline**.

### Objetivo
Dotar al Planificador de Transporte de un **motor de alarmas nativo del sistema operativo Android (`AlarmManager`)** que programe avisos exactos en segundo plano para:
- **Apertura de Check-in** (24 h y 2 h antes de vuelos).
- **Salida inminente y embarque** (60 min y 15 min antes de la salida de trenes Shinkansen, vuelos o autobuses).
- **Alarma de proximidad de llegada a estación de destino** (5 a 10 min antes de la hora prevista de llegada).
- Todo ello sincronizado de forma reactiva ante cambios de fechas, ediciones o cancelaciones de tramos.

---

## 2. Requisitos Funcionales (RF)

### RF-12.1: Tipos de Alarmas y Umbrales Temporales
1. **Alarma de Check-in / Facturación de Vuelos**:
   - Disparo 24 horas antes de `departureTimeIso` (apertura habitual de check-in online) y 2 horas antes de la salida (cierre de facturación en mostrador).
2. **Alarma de Salida / Embarque**:
   - **Aviso 60 minutos antes**: Notificación preventiva para iniciar el desplazamiento hacia la estación o puerta de embarque.
   - **Aviso 15 minutos antes**: Alerta de alta prioridad indicando andén (*Platform*), número de tren/vuelo, vagón (*Car*) y asiento (*Seat*).
3. **Alarma de Despertador / Próxima Parada (Llegada a Destino)**:
   - Disparo configurable **5 o 10 minutos antes** de `arrivalTimeIso`.
   - Propósito: Despertar al viajero en Shinkansen o trenes nocturnos para preparar equipaje y situarse ante las puertas de salida antes del frenado en la estación destino.

### RF-12.2: Programación con Android `AlarmManager`
1. **Precisión sin Conexión (`setExactAndAllowWhileIdle` / `setAlarmClock`)**:
   - Utilización de `AlarmManager.setExactAndAllowWhileIdle` o `AlarmManager.setAlarmClock` garantizando que la alarma se active incluso si el dispositivo entra en modo *Doze* (ahorro de batería profundo).
2. **Compatibilidad con Permisos de Android 12+ y 14+ (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`)**:
   - Verificación de `alarmManager.canScheduleExactAlarms()` en tiempo de ejecución.
   - Si el permiso no está otorgado o el usuario lo deniega, degradación elegante a `setAndAllowWhileIdle` no inexacto y botón de acceso a Ajustes del sistema para autorizar alarmas exactas.

### RF-12.3: Despacho de Notificaciones Locales y Canal de Notificación
1. **Canal de Notificaciones de Alta Prioridad**:
   - Canal dedicado: `japan_travel_transport_alarms` con `NotificationManager.IMPORTANCE_HIGH`.
   - Sonido del sistema, vibración y visualización en pantalla bloqueada (*Heads-up Notification*).
2. **Contenido Enriquecido de la Notificación**:
   - Título: Código o nombre del servicio (ej. `🚄 Shinkansen Nozomi 23`, `✈️ Vuelo IB6800`).
   - Texto: Resumen conciso con andén, vagón, asiento y tiempo restante (ej. *"Salida en 15 min desde Andén 14 • Vagón 5 Asiento 12-A"* o *"Llegada a Kioto en 5 min. ¡Prepara tu equipaje!"*).
   - Botón de acción directa en la notificación: `📱 Ver Billete / QR` para desplegar el billete en pantalla completa y brillo máximo (`SPEC-09`).

### RF-12.4: Ciclo de Vida, Reprogramación y Cancelación Automática
1. **Cancelación al Eliminar**: Al borrar un tramo de transporte de `SharedTravelPlanStore`, todas sus alarmas programadas en `AlarmManager` deben cancelarse de inmediato (`alarmManager.cancel(pendingIntent)`).
2. **Reprogramación al Editar o Desplazar Fechas**: Al editar las horas o al cambiar la fecha de inicio del viaje (desplazamiento dinámico introducido en `v1.4.2`), las alarmas antiguas deben cancelarse y registrarse con los nuevos instantes UTC.
3. **Restauración tras Reinicio del Dispositivo (`BOOT_COMPLETED`)**:
   - `BroadcastReceiver` registrado para `Intent.ACTION_BOOT_COMPLETED` que reprograma automáticamente en el sistema las alarmas futuras vigentes a partir de los planes persistidos en `SharedTravelPlanStore`.

---

## 3. Reglas de Negocio y Offline-First

1. **Cero Conexión Externa (RN-12.1)**: La programación, cálculo horario y notificación se ejecutan íntegramente de forma local en el dispositivo.
2. **No Alarma para el Pasado (RN-12.2)**: Nunca programar alarmas para instantes que ya hayan transcurrido respecto al reloj del sistema (`triggerAtMillis <= System.currentTimeMillis()`).
3. **Respeto a Preferencias de Usuario (RN-12.3)**: El usuario puede desactivar individualmente las alarmas de llegada o las alarmas de 60 min desde la pantalla de Ajustes o desde el editor del tramo.

---

## 4. Diseño de Datos

```kotlin
data class TransportAlarmSchedule(
    val segmentId: String,
    val alarmType: TransportAlarmType,
    val triggerTimeUtcEpochMillis: Long,
    val transportType: TransportType,
    val title: String,
    val summary: String,
    val platform: String,
    val carAndSeat: String,
    val qrCodePayload: String?
)

enum class TransportAlarmType {
    FLIGHT_CHECK_IN_24H,
    FLIGHT_CHECK_IN_2H,
    DEPARTURE_60M,
    DEPARTURE_15M,
    ARRIVAL_APPROACHING_5M
}
```

---

## 5. Criterios de Aceptación (DoD)

1. Tests unitarios (TDD) para el calculador de tiempos de alarma a partir de `TransportSegmentItem` con horarios UTC y zonas horarias internacionales (`Europe/Madrid`, `Asia/Tokyo`).
2. Tests unitarios para el programador y cancelador de intents pendientes.
3. Verificación de permisos y comportamiento en Android 12, 13, 14 y 15.
4. Notificaciones localizadas en español e inglés.
5. Salto de línea final estándar POSIX (`\n`) en todos los archivos.
