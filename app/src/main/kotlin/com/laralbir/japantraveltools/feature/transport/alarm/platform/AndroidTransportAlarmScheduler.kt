package com.laralbir.japantraveltools.feature.transport.alarm.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.laralbir.japantraveltools.feature.transport.alarm.domain.model.TransportAlarmSchedule
import com.laralbir.japantraveltools.feature.transport.alarm.domain.model.TransportAlarmType
import com.laralbir.japantraveltools.feature.transport.alarm.domain.repository.TransportAlarmScheduler

/**
 * Android platform implementation of [TransportAlarmScheduler] using [AlarmManager].
 * Checks runtime permissions (`SCHEDULE_EXACT_ALARM`) and schedules alarms with [setExactAndAllowWhileIdle]
 * or graceful fallback to [setAndAllowWhileIdle].
 */
class AndroidTransportAlarmScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager? = runCatching {
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    }.getOrNull(),
    initialSegmentIds: Set<String>? = null
) : TransportAlarmScheduler {

    companion object {
        const val PREFS_NAME = "transport_alarm_scheduler_prefs"
        const val KEY_SCHEDULED_SEGMENTS = "scheduled_segment_ids"

        /**
         * Builds an explicit Intent for [TransportAlarmReceiver] populated with all alarm details.
         */
        fun createAlarmIntent(context: Context, schedule: TransportAlarmSchedule): Intent {
            return Intent(context, TransportAlarmReceiver::class.java).apply {
                action = TransportAlarmReceiver.ACTION_TRANSPORT_ALARM
                putExtra(TransportAlarmReceiver.EXTRA_SEGMENT_ID, schedule.segmentId)
                putExtra(TransportAlarmReceiver.EXTRA_ALARM_TYPE, schedule.alarmType.name)
                putExtra(TransportAlarmReceiver.EXTRA_TRIGGER_EPOCH, schedule.triggerTimeEpochMillis)
                putExtra(TransportAlarmReceiver.EXTRA_TRIGGER_UTC_ISO, schedule.triggerTimeUtcIso)
                putExtra(TransportAlarmReceiver.EXTRA_TRANSPORT_TYPE, schedule.transportType.name)
                putExtra(TransportAlarmReceiver.EXTRA_TITLE, schedule.title)
                putExtra(TransportAlarmReceiver.EXTRA_SUMMARY, schedule.summary)
                putExtra(TransportAlarmReceiver.EXTRA_PLATFORM, schedule.platform)
                putExtra(TransportAlarmReceiver.EXTRA_CAR_AND_SEAT, schedule.carAndSeat)
                putExtra(TransportAlarmReceiver.EXTRA_QR_CODE, schedule.qrCodePayload)
            }
        }
    }

    private val inMemorySegmentIds = mutableSetOf<String>().apply {
        if (initialSegmentIds != null) addAll(initialSegmentIds)
    }

    override fun scheduleAlarms(schedules: List<TransportAlarmSchedule>) {
        if (schedules.isEmpty()) return

        val updatedSegmentIds = getScheduledSegmentIds().toMutableSet()
        val am = alarmManager

        val canScheduleExact = if (am != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { am.canScheduleExactAlarms() }.getOrDefault(false)
        } else {
            true
        }

        for (schedule in schedules) {
            runCatching {
                val intent = createAlarmIntent(context, schedule)
                val requestCode = TransportAlarmPendingIntentHelper.generateRequestCode(
                    schedule.segmentId,
                    schedule.alarmType
                )

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (am != null) {
                    if (canScheduleExact) {
                        try {
                            am.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                schedule.triggerTimeEpochMillis,
                                pendingIntent
                            )
                        } catch (e: SecurityException) {
                            am.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                schedule.triggerTimeEpochMillis,
                                pendingIntent
                            )
                        }
                    } else {
                        am.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            schedule.triggerTimeEpochMillis,
                            pendingIntent
                        )
                    }
                }
            }

            updatedSegmentIds.add(schedule.segmentId)
        }

        saveScheduledSegmentIds(updatedSegmentIds)
    }

    override fun cancelAlarmsForSegment(segmentId: String) {
        val am = alarmManager

        for (alarmType in TransportAlarmType.entries) {
            runCatching {
                val requestCode = TransportAlarmPendingIntentHelper.generateRequestCode(segmentId, alarmType)
                val intent = Intent(context, TransportAlarmReceiver::class.java).apply {
                    action = TransportAlarmReceiver.ACTION_TRANSPORT_ALARM
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (am != null && pendingIntent != null) {
                    am.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
        }

        val updatedSegmentIds = getScheduledSegmentIds().toMutableSet()
        if (updatedSegmentIds.remove(segmentId)) {
            saveScheduledSegmentIds(updatedSegmentIds)
        }
    }

    override fun cancelAllAlarms() {
        val scheduledSegments = getScheduledSegmentIds()
        for (segmentId in scheduledSegments) {
            cancelAlarmsForSegment(segmentId)
        }
        saveScheduledSegmentIds(emptySet())
    }

    fun getScheduledSegmentIds(): Set<String> {
        val fromPrefs = runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getStringSet(KEY_SCHEDULED_SEGMENTS, emptySet())?.toSet()
        }.getOrNull()

        return if (fromPrefs != null && fromPrefs.isNotEmpty()) {
            fromPrefs
        } else {
            inMemorySegmentIds.toSet()
        }
    }

    private fun saveScheduledSegmentIds(segmentIds: Set<String>) {
        inMemorySegmentIds.clear()
        inMemorySegmentIds.addAll(segmentIds)
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet(KEY_SCHEDULED_SEGMENTS, segmentIds).apply()
        }
    }
}
