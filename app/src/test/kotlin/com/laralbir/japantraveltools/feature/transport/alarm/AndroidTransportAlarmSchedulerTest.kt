package com.laralbir.japantraveltools.feature.transport.alarm

import android.content.ContextWrapper
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.alarm.domain.model.TransportAlarmSchedule
import com.laralbir.japantraveltools.feature.transport.alarm.domain.model.TransportAlarmType
import com.laralbir.japantraveltools.feature.transport.alarm.platform.AndroidTransportAlarmScheduler
import com.laralbir.japantraveltools.feature.transport.alarm.platform.TransportAlarmReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTransportAlarmSchedulerTest {

    private val dummyContext = ContextWrapper(null)

    @Test
    fun testSchedulerConstantsAreDefinedCorrectly() {
        assertEquals("transport_alarm_scheduler_prefs", AndroidTransportAlarmScheduler.PREFS_NAME)
        assertEquals("scheduled_segment_ids", AndroidTransportAlarmScheduler.KEY_SCHEDULED_SEGMENTS)
    }

    @Test
    fun testCreateAlarmIntentBuildsNonNullIntent() {
        val schedule = TransportAlarmSchedule(
            segmentId = "seg-flight-101",
            alarmType = TransportAlarmType.FLIGHT_CHECK_IN_24H,
            triggerTimeUtcIso = "2026-11-20T10:00:00Z",
            triggerTimeEpochMillis = 1795168800000L,
            transportType = TransportMode.FLIGHT,
            title = "✈️ Check-in Abierto: ANA NH204",
            summary = "Facturación online disponible 24h antes",
            platform = "Terminal 1 · Mostrador 42",
            carAndSeat = "Seat 22A",
            qrCodePayload = "M1ALVAREZ/CARLOS ENH204"
        )

        val intent = AndroidTransportAlarmScheduler.createAlarmIntent(dummyContext, schedule)
        assertNotNull(intent)
    }

    @Test
    fun testScheduleAlarmsTracksSegmentIds() {
        val scheduler = AndroidTransportAlarmScheduler(
            context = dummyContext,
            alarmManager = null
        )

        val schedule = TransportAlarmSchedule(
            segmentId = "seg-flight-101",
            alarmType = TransportAlarmType.DEPARTURE_60M,
            triggerTimeUtcIso = "2026-11-20T11:00:00Z",
            triggerTimeEpochMillis = 1795172400000L,
            transportType = TransportMode.FLIGHT,
            title = "✈️ Salida en 60 min: ANA NH204",
            summary = "Salida en 60 min",
            platform = "Gate 42",
            carAndSeat = "Seat 22A",
            qrCodePayload = null
        )

        // Empty list does nothing
        scheduler.scheduleAlarms(emptyList())
        assertTrue(scheduler.getScheduledSegmentIds().isEmpty())

        // Schedule one alarm
        scheduler.scheduleAlarms(listOf(schedule))
        assertTrue(scheduler.getScheduledSegmentIds().contains("seg-flight-101"))

        // Cancel alarms for segment
        scheduler.cancelAlarmsForSegment("seg-flight-101")
        assertFalse(scheduler.getScheduledSegmentIds().contains("seg-flight-101"))
    }

    @Test
    fun testCancelAllAlarmsClearsScheduledSegmentIds() {
        val scheduler = AndroidTransportAlarmScheduler(
            context = dummyContext,
            alarmManager = null,
            initialSegmentIds = setOf("seg-1", "seg-2", "seg-3")
        )

        assertEquals(3, scheduler.getScheduledSegmentIds().size)

        scheduler.cancelAllAlarms()
        assertTrue(scheduler.getScheduledSegmentIds().isEmpty())
    }
}
