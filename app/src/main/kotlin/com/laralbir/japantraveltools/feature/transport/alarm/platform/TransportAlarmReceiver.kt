package com.laralbir.japantraveltools.feature.transport.alarm.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.laralbir.japantraveltools.MainActivity
import com.laralbir.japantraveltools.R

/**
 * BroadcastReceiver triggered by Android AlarmManager when a scheduled transport alarm fires.
 * Builds and dispatches high-priority notifications with sound, vibration, and optional QR view action.
 */
class TransportAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRANSPORT_ALARM = "com.laralbir.japantraveltools.action.TRANSPORT_ALARM"

        const val CHANNEL_ID = "japan_travel_transport_alarms"
        const val CHANNEL_NAME = "Transport Alerts"

        const val EXTRA_SEGMENT_ID = "extra_segment_id"
        const val EXTRA_ALARM_TYPE = "extra_alarm_type"
        const val EXTRA_TRIGGER_EPOCH = "extra_trigger_epoch"
        const val EXTRA_TRIGGER_UTC_ISO = "extra_trigger_utc_iso"
        const val EXTRA_TRANSPORT_TYPE = "extra_transport_type"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUMMARY = "extra_summary"
        const val EXTRA_PLATFORM = "extra_platform"
        const val EXTRA_CAR_AND_SEAT = "extra_car_and_seat"
        const val EXTRA_QR_CODE = "extra_qr_code"

        const val EXTRA_VIEW_TICKET_QR = "extra_view_ticket_qr"
        const val ACTION_VIEW_TICKET = "com.laralbir.japantraveltools.action.VIEW_TICKET"

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos exactos de facturación de vuelos, salidas de tren/metro/autobús y llegadas a destino"
                    enableVibration(true)
                    enableLights(true)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        }

        fun buildNotification(
            context: Context,
            intent: Intent
        ): NotificationCompat.Builder {
            ensureNotificationChannel(context)

            val segmentId = intent.getStringExtra(EXTRA_SEGMENT_ID) ?: ""
            val title = intent.getStringExtra(EXTRA_TITLE) ?: context.getString(R.string.app_name)
            val summary = intent.getStringExtra(EXTRA_SUMMARY) ?: ""
            val qrCodePayload = intent.getStringExtra(EXTRA_QR_CODE)

            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_SEGMENT_ID, segmentId)
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                (segmentId.hashCode() xor 0x1A),
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(summary)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)

            if (!qrCodePayload.isNullOrBlank()) {
                val ticketIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_VIEW_TICKET
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_VIEW_TICKET_QR, qrCodePayload)
                    putExtra(EXTRA_SEGMENT_ID, segmentId)
                }
                val ticketPendingIntent = PendingIntent.getActivity(
                    context,
                    (segmentId.hashCode() xor 0x2B),
                    ticketIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    R.mipmap.ic_launcher,
                    "📱 Ver Billete",
                    ticketPendingIntent
                )
            }

            return builder
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRANSPORT_ALARM) return

        val segmentId = intent.getStringExtra(EXTRA_SEGMENT_ID) ?: ""
        val alarmTypeStr = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: ""
        val notificationId = (segmentId.hashCode() xor (alarmTypeStr.hashCode() * 31)) and 0x7FFFFFFF

        val builder = buildNotification(context, intent)
        val notificationManager = NotificationManagerCompat.from(context)

        runCatching {
            notificationManager.notify(notificationId, builder.build())
        }
    }
}
