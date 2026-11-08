package com.laralbir.japantraveltools.feature.transport.alarm

import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import com.laralbir.japantraveltools.feature.transport.alarm.domain.model.TransportAlarmSchedule
import com.laralbir.japantraveltools.feature.transport.alarm.domain.repository.TransportAlarmScheduler
import com.laralbir.japantraveltools.feature.transport.alarm.domain.usecase.ScheduleTransportAlarmsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleTransportAlarmsUseCaseTest {

    private class FakeTransportAlarmScheduler : TransportAlarmScheduler {
        val scheduledList = mutableListOf<TransportAlarmSchedule>()
        val cancelledSegments = mutableListOf<String>()
        var cancelledAllCount = 0

        override fun scheduleAlarms(schedules: List<TransportAlarmSchedule>) {
            scheduledList.addAll(schedules)
        }

        override fun cancelAlarmsForSegment(segmentId: String) {
            cancelledSegments.add(segmentId)
            scheduledList.removeAll { it.segmentId == segmentId }
        }

        override fun cancelAllAlarms() {
            cancelledAllCount++
            scheduledList.clear()
        }
    }

    @Test
    fun testScheduleAlarmsCancelsOldAndRegistersNewAlarms() {
        val fakeScheduler = FakeTransportAlarmScheduler()
        val useCase = ScheduleTransportAlarmsUseCase(fakeScheduler)

        val depIso = "2026-11-14T09:00"
        val arrIso = "2026-11-14T11:15"
        val zone = "Asia/Tokyo"
        val depZdt = ZonedDateTime.of(LocalDateTime.parse(depIso), ZoneId.of(zone))
        val currentTimeMillis = depZdt.minusHours(2).toInstant().toEpochMilli()

        val shinkansen = TransportSegmentItem(
            id = "train-nozomi-21",
            order = 1,
            type = TransportMode.TRAIN_SHINKANSEN,
            operatorName = "JR Central",
            lineOrNumber = "Nozomi #21",
            departureCode = "TYO",
            departureName = "Tokyo Station",
            departureZoneId = zone,
            departureTerminalOrPlatform = "Track 16",
            departureTimeIso = depIso,
            arrivalCode = "KYO",
            arrivalName = "Kyoto Station",
            arrivalZoneId = zone,
            arrivalTerminalOrPlatform = "Track 13",
            arrivalTimeIso = arrIso,
            seatOrCarInfo = "Car 7 · Seat 12E"
        )

        useCase(shinkansen, currentTimeMillis)

        // Verifies cancel was called first for segment
        assertEquals(listOf("train-nozomi-21"), fakeScheduler.cancelledSegments)
        // Verifies 3 alarms scheduled (60m, 15m, 5m arrival)
        assertEquals(3, fakeScheduler.scheduledList.size)
    }

    @Test
    fun testScheduleAlarmsWhenAllInPastOnlyCancelsWithoutScheduling() {
        val fakeScheduler = FakeTransportAlarmScheduler()
        val useCase = ScheduleTransportAlarmsUseCase(fakeScheduler)

        val depIso = "2026-11-14T09:00"
        val arrIso = "2026-11-14T11:15"
        val zone = "Asia/Tokyo"
        val arrZdt = ZonedDateTime.of(LocalDateTime.parse(arrIso), ZoneId.of(zone))
        val currentTimeMillis = arrZdt.plusHours(1).toInstant().toEpochMilli()

        val shinkansen = TransportSegmentItem(
            id = "train-nozomi-21",
            order = 1,
            type = TransportMode.TRAIN_SHINKANSEN,
            operatorName = "JR Central",
            lineOrNumber = "Nozomi #21",
            departureCode = "TYO",
            departureName = "Tokyo Station",
            departureZoneId = zone,
            departureTerminalOrPlatform = "Track 16",
            departureTimeIso = depIso,
            arrivalCode = "KYO",
            arrivalName = "Kyoto Station",
            arrivalZoneId = zone,
            arrivalTerminalOrPlatform = "Track 13",
            arrivalTimeIso = arrIso,
            seatOrCarInfo = "Car 7 · Seat 12E"
        )

        useCase(shinkansen, currentTimeMillis)

        assertEquals(listOf("train-nozomi-21"), fakeScheduler.cancelledSegments)
        assertTrue(fakeScheduler.scheduledList.isEmpty())
    }
}
