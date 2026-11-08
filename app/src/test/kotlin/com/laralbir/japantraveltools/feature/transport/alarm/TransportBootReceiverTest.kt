package com.laralbir.japantraveltools.feature.transport.alarm

import com.laralbir.japantraveltools.core.portability.PortableTravelPlanBundle
import com.laralbir.japantraveltools.core.portability.SharedTravelPlanStore
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import com.laralbir.japantraveltools.feature.transport.alarm.domain.model.TransportAlarmSchedule
import com.laralbir.japantraveltools.feature.transport.alarm.domain.repository.TransportAlarmScheduler
import com.laralbir.japantraveltools.feature.transport.alarm.domain.usecase.CalculateTransportAlarmsUseCase
import com.laralbir.japantraveltools.feature.transport.alarm.platform.TransportBootReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TransportBootReceiverTest {

    private class TestScheduler : TransportAlarmScheduler {
        val scheduled = mutableListOf<TransportAlarmSchedule>()
        val cancelled = mutableListOf<String>()

        override fun scheduleAlarms(schedules: List<TransportAlarmSchedule>) {
            scheduled.addAll(schedules)
        }

        override fun cancelAlarmsForSegment(segmentId: String) {
            cancelled.add(segmentId)
        }

        override fun cancelAllAlarms() {
            scheduled.clear()
        }
    }

    @Test
    fun testRescheduleAllAlarmsFiltersPastAndSchedulesFutureAlarms() {
        val scheduler = TestScheduler()

        val futureDepIso = "2026-11-20T12:00:00"
        val zone = "Asia/Tokyo"
        val depZdt = ZonedDateTime.of(LocalDateTime.parse(futureDepIso), ZoneId.of(zone))
        val currentTimeMillis = depZdt.minusHours(5).toInstant().toEpochMilli()

        val segment = TransportSegmentItem(
            id = "seg-shinkansen-boot",
            order = 1,
            type = TransportMode.TRAIN_SHINKANSEN,
            operatorName = "JR Central",
            lineOrNumber = "Hikari 501",
            departureCode = "TYO",
            departureName = "Tokyo",
            departureZoneId = zone,
            departureTerminalOrPlatform = "Track 14",
            departureTimeIso = futureDepIso,
            arrivalCode = "OSA",
            arrivalName = "Shin-Osaka",
            arrivalZoneId = zone,
            arrivalTerminalOrPlatform = "Track 21",
            arrivalTimeIso = "2026-11-20T15:00:00",
            seatOrCarInfo = "Car 4 · Seat 10A"
        )

        // Seed store with this segment
        SharedTravelPlanStore.replaceAllPlans(
            listOf(
                PortableTravelPlanBundle(
                    id = "test-plan",
                    tripTitle = "Test Trip",
                    itineraryStops = emptyList(),
                    transportSegments = listOf(segment)
                )
            )
        )

        val scheduledCount = TransportBootReceiver.rescheduleAllAlarms(
            context = null,
            scheduler = scheduler,
            calculateAlarms = CalculateTransportAlarmsUseCase(),
            currentTimeMillis = currentTimeMillis
        )

        // 60m departure, 15m departure, and 5m arrival -> 3 alarms
        assertEquals(3, scheduledCount)
        assertEquals(3, scheduler.scheduled.size)
        assertTrue(scheduler.scheduled.all { it.segmentId == "seg-shinkansen-boot" })
    }

    @Test
    fun testRescheduleAllAlarmsWhenNoFutureAlarmsReturnsZero() {
        val scheduler = TestScheduler()

        val pastDepIso = "2026-10-01T08:00:00"
        val zone = "Asia/Tokyo"
        val currentTimeMillis = System.currentTimeMillis() // 2026-11-xx, way after pastDepIso

        val pastSegment = TransportSegmentItem(
            id = "seg-past",
            order = 1,
            type = TransportMode.BUS,
            operatorName = "City Bus",
            lineOrNumber = "Bus 1",
            departureCode = "A",
            departureName = "Station A",
            departureZoneId = zone,
            departureTerminalOrPlatform = "Bay 1",
            departureTimeIso = pastDepIso,
            arrivalCode = "B",
            arrivalName = "Station B",
            arrivalZoneId = zone,
            arrivalTerminalOrPlatform = "Bay 2",
            arrivalTimeIso = "2026-10-01T09:00:00",
            seatOrCarInfo = ""
        )

        SharedTravelPlanStore.replaceAllPlans(
            listOf(
                PortableTravelPlanBundle(
                    id = "test-past-plan",
                    tripTitle = "Past Trip",
                    itineraryStops = emptyList(),
                    transportSegments = listOf(pastSegment)
                )
            )
        )

        val scheduledCount = TransportBootReceiver.rescheduleAllAlarms(
            context = null,
            scheduler = scheduler,
            calculateAlarms = CalculateTransportAlarmsUseCase(),
            currentTimeMillis = currentTimeMillis
        )

        assertEquals(0, scheduledCount)
        assertTrue(scheduler.scheduled.isEmpty())
    }
}
