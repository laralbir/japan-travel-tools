package com.laralbir.japantraveltools.feature.transport

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.laralbir.japantraveltools.MainActivity
import com.laralbir.japantraveltools.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZonedDateTime

object TransportAlertNotificationHelper {

    const val CHANNEL_ID = "transport_alerts"
    const val CHANNEL_NAME = "Avisos de Transporte y Vuelos"

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de facturación de vuelos, embarque y salidas de transporte"
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun dispatchAlertNotification(
        context: Context,
        alert: TransportAlert,
        isEs: Boolean = true
    ): Boolean {
        ensureChannelCreated(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                return false
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.uniqueKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isEs) alert.titleEs else alert.titleEn
        val message = if (isEs) alert.messageEs else alert.messageEn

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(
                if (alert.severity == TransportAlertSeverity.URGENT) {
                    NotificationCompat.PRIORITY_MAX
                } else {
                    NotificationCompat.PRIORITY_HIGH
                }
            )
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(alert.uniqueKey.hashCode(), builder.build())
            return true
        } catch (_: SecurityException) {
            return false
        }
    }
}

object SharedTransportAlertStore {

    private val _alerts = MutableStateFlow<List<TransportAlert>>(emptyList())
    val alertsState: StateFlow<List<TransportAlert>> = _alerts.asStateFlow()

    private val _dismissedKeys = MutableStateFlow<Set<String>>(emptySet())
    val dismissedKeysState: StateFlow<Set<String>> = _dismissedKeys.asStateFlow()

    private val notifiedKeys = mutableSetOf<String>()

    fun evaluate(
        segments: List<TransportSegmentItem>,
        context: Context?,
        isEs: Boolean = true
    ) {
        val now = ZonedDateTime.now()
        val evaluated = TransportAlertEvaluator.evaluateSegmentAlerts(
            segments = segments,
            now = now,
            dismissedAlertKeys = _dismissedKeys.value
        )
        _alerts.value = evaluated

        if (context != null) {
            for (alert in evaluated) {
                if (alert.uniqueKey !in notifiedKeys) {
                    val dispatched = TransportAlertNotificationHelper.dispatchAlertNotification(
                        context = context,
                        alert = alert,
                        isEs = isEs
                    )
                    if (dispatched) {
                        notifiedKeys.add(alert.uniqueKey)
                    }
                }
            }
        }
    }

    fun dismissAlert(uniqueKey: String) {
        _dismissedKeys.value = _dismissedKeys.value + uniqueKey
        _alerts.value = _alerts.value.filter { it.uniqueKey != uniqueKey }
    }

    fun clearDismissed() {
        _dismissedKeys.value = emptySet()
    }

    fun triggerSimulation(
        context: Context?,
        type: TransportAlertType,
        isEs: Boolean = true
    ) {
        val simulatedAlert = when (type) {
            TransportAlertType.BOARDING_NOW -> TransportAlert(
                segmentId = "sim-flight-now",
                type = TransportAlertType.BOARDING_NOW,
                severity = TransportAlertSeverity.URGENT,
                titleEs = "🚨 ¡Embarque en Curso! Qatar Airways QR806",
                titleEn = "🚨 Boarding Now! Qatar Airways QR806",
                messageEs = "Puerta C14 abierta en Terminal 1. Asiento 24K. ¡Acude a la puerta de inmediato!",
                messageEn = "Gate C14 open at Terminal 1. Seat 24K. Proceed to the gate immediately!",
                minutesUntilEvent = 5L,
                eventTimeIso = ZonedDateTime.now().plusMinutes(5).toLocalDateTime().toString(),
                targetGateOrCounter = "Terminal 1 · Puerta C14"
            )
            TransportAlertType.BOARDING_APPROACHING -> TransportAlert(
                segmentId = "sim-flight-soon",
                type = TransportAlertType.BOARDING_APPROACHING,
                severity = TransportAlertSeverity.WARNING,
                titleEs = "⚠️ Embarque próximo: Iberia IB6801",
                titleEn = "⚠️ Boarding Soon: Iberia IB6801",
                messageEs = "El embarque comienza en 35 min en Terminal 4S · Puerta S22. Asiento 18A.",
                messageEn = "Boarding starts in 35 min at Terminal 4S · Gate S22. Seat 18A.",
                minutesUntilEvent = 35L,
                eventTimeIso = ZonedDateTime.now().plusMinutes(35).toLocalDateTime().toString(),
                targetGateOrCounter = "Terminal 4S · Puerta S22"
            )
            TransportAlertType.CHECK_IN_OPEN -> TransportAlert(
                segmentId = "sim-flight-checkin",
                type = TransportAlertType.CHECK_IN_OPEN,
                severity = TransportAlertSeverity.WARNING,
                titleEs = "ℹ️ Facturación abierta: Qatar Airways QR150",
                titleEn = "ℹ️ Check-in Open: Qatar Airways QR150",
                messageEs = "Mostrador 810 abierto en Terminal 4S. Entrega tu equipaje antes del cierre.",
                messageEn = "Desk 810 open at Terminal 4S. Drop your bags before closing.",
                minutesUntilEvent = 0L,
                eventTimeIso = ZonedDateTime.now().toLocalDateTime().toString(),
                targetGateOrCounter = "Terminal 4S · Mostrador 810"
            )
            TransportAlertType.CHECK_IN_APPROACHING -> TransportAlert(
                segmentId = "sim-flight-checkin-soon",
                type = TransportAlertType.CHECK_IN_APPROACHING,
                severity = TransportAlertSeverity.WARNING,
                titleEs = "🕒 Facturación próxima: Iberia IB6801",
                titleEn = "🕒 Check-in Soon: Iberia IB6801",
                messageEs = "La facturación abre en 45 min en Mostrador 810 de Terminal 4S.",
                messageEn = "Check-in opens in 45 min at Desk 810 in Terminal 4S.",
                minutesUntilEvent = 45L,
                eventTimeIso = ZonedDateTime.now().plusMinutes(45).toLocalDateTime().toString(),
                targetGateOrCounter = "Terminal 4S · Mostrador 810"
            )
            TransportAlertType.DEPARTURE_IMMINENT -> TransportAlert(
                segmentId = "sim-flight-dep",
                type = TransportAlertType.DEPARTURE_IMMINENT,
                severity = TransportAlertSeverity.URGENT,
                titleEs = "🚨 Salida Inminente: Nozomi #21",
                titleEn = "🚨 Imminent Departure: Nozomi #21",
                messageEs = "Salida en 10 min desde Andén 16 de Tokyo Station. Vagón 7 · Asiento 12E.",
                messageEn = "Departing in 10 min from Track 16 at Tokyo Station. Car 7 · Seat 12E.",
                minutesUntilEvent = 10L,
                eventTimeIso = ZonedDateTime.now().plusMinutes(10).toLocalDateTime().toString(),
                targetGateOrCounter = "Tokyo Station · Andén 16"
            )
        }

        _alerts.value = listOf(simulatedAlert) + _alerts.value.filter { it.uniqueKey != simulatedAlert.uniqueKey }

        if (context != null) {
            TransportAlertNotificationHelper.dispatchAlertNotification(
                context = context,
                alert = simulatedAlert,
                isEs = isEs
            )
        }
    }
}

