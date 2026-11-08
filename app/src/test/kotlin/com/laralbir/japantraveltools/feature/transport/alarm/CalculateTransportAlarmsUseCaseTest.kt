package com.laralbir.japantraveltools.feature.transport.alarm

import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import com.laralbir.japantraveltools.feature.transport.alarm.domain.model.TransportAlarmType
import com.laralbir.japantraveltools.feature.transport.alarm.domain.usecase.CalculateTransportAlarmsUseCase
import com.laralbir.japantraveltools.feature.transport.alarm.platform.TransportAlarmPendingIntentHelper
import com.laralbir.japantraveltools.feature.transport.domain.TransportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class CalculateTransportAlarmsUseCaseTest {

    private val useCase = CalculateTransportAlarmsUseCase()

    @Test
    fun testFlightTokyo30HoursInAdvanceGeneratesAllFourAlarmsWithExactEpochs() {
        val departureIso = "2026-11-20T12:00:00"
        val departureZone = "Europe/Berlin"
        val depZdt = ZonedDateTime.of(LocalDateTime.parse(departureIso), ZoneId.of(departureZone))
        val depEpochMillis = depZdt.toInstant().toEpochMilli()

        // Current time is 30 hours before departure
        val currentTimeMillis = depEpochMillis - (30 * 3600 * 1000L)

        val flightSegment = TransportSegmentItem(
            id = "flight-hnd-1",
            order = 1,
            type = TransportMode.FLIGHT,
            operatorName = "All Nippon Airways",
            lineOrNumber = "NH204",
            departureCode = "FRA",
            departureName = "Frankfurt Airport",
            departureZoneId = departureZone,
            departureTerminalOrPlatform = "Terminal 1 · Gate B24",
            departureTimeIso = departureIso,
            arrivalCode = "HND",
            arrivalName = "Tokyo Haneda",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "Terminal 3",
            arrivalTimeIso = "2026-11-21T07:00:00",
            seatOrCarInfo = "Seat 15K",
            barcodeMessage = "M1ALVAREZ/CARLOS ENH204 324Y015K"
        )

        val alarms = useCase(flightSegment, currentTimeMillis)

        assertEquals(4, alarms.size)

        // 1. Check-in 24h
        val checkIn24h = alarms.first { it.alarmType == TransportAlarmType.FLIGHT_CHECK_IN_24H }
        val expected24h = depEpochMillis - (24 * 3600 * 1000L)
        assertEquals(expected24h, checkIn24h.triggerTimeEpochMillis)
        assertEquals(TransportType.FLIGHT, checkIn24h.transportType)
        assertEquals("flight-hnd-1", checkIn24h.segmentId)
        assertEquals("Terminal 1 · Gate B24", checkIn24h.platform)
        assertEquals("Seat 15K", checkIn24h.carAndSeat)
        assertEquals("M1ALVAREZ/CARLOS ENH204 324Y015K", checkIn24h.qrCodePayload)

        // 2. Check-in 2h
        val checkIn2h = alarms.first { it.alarmType == TransportAlarmType.FLIGHT_CHECK_IN_2H }
        val expected2h = depEpochMillis - (2 * 3600 * 1000L)
        assertEquals(expected2h, checkIn2h.triggerTimeEpochMillis)

        // 3. Departure 60m
        val dep60m = alarms.first { it.alarmType == TransportAlarmType.DEPARTURE_60M }
        val expected60m = depEpochMillis - (60 * 60 * 1000L)
        assertEquals(expected60m, dep60m.triggerTimeEpochMillis)

        // 4. Departure 15m
        val dep15m = alarms.first { it.alarmType == TransportAlarmType.DEPARTURE_15M }
        val expected15m = depEpochMillis - (15 * 60 * 1000L)
        assertEquals(expected15m, dep15m.triggerTimeEpochMillis)
    }

    @Test
    fun testFlightDepartingInUnderOneHourDiscardsPastAlarmsAndOnlyReturns15m() {
        val departureIso = "2026-11-20T12:00:00"
        val departureZone = "Europe/Berlin"
        val depZdt = ZonedDateTime.of(LocalDateTime.parse(departureIso), ZoneId.of(departureZone))
        val depEpochMillis = depZdt.toInstant().toEpochMilli()

        // Current time is 45 minutes before departure
        val currentTimeMillis = depEpochMillis - (45 * 60 * 1000L)

        val flightSegment = TransportSegmentItem(
            id = "flight-hnd-2",
            order = 1,
            type = TransportMode.FLIGHT,
            operatorName = "All Nippon Airways",
            lineOrNumber = "NH204",
            departureCode = "FRA",
            departureName = "Frankfurt Airport",
            departureZoneId = departureZone,
            departureTerminalOrPlatform = "Gate B24",
            departureTimeIso = departureIso,
            arrivalCode = "HND",
            arrivalName = "Tokyo Haneda",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "T3",
            arrivalTimeIso = "2026-11-21T07:00:00",
            seatOrCarInfo = "Seat 15K"
        )

        val alarms = useCase(flightSegment, currentTimeMillis)

        // 24h, 2h, and 60m are in the past relative to (dep - 45m). Only 15m remains.
        assertEquals(1, alarms.size)
        assertEquals(TransportAlarmType.DEPARTURE_15M, alarms.first().alarmType)
        assertEquals(depEpochMillis - (15 * 60 * 1000L), alarms.first().triggerTimeEpochMillis)
    }

    @Test
    fun testShinkansenTokyoToKyotoGeneratesDeparture60m15mAndArrival5mAlarms() {
        val depIso = "2026-11-14T09:00"
        val arrIso = "2026-11-14T11:15"
        val zone = "Asia/Tokyo"
        val depZdt = ZonedDateTime.of(LocalDateTime.parse(depIso), ZoneId.of(zone))
        val arrZdt = ZonedDateTime.of(LocalDateTime.parse(arrIso), ZoneId.of(zone))
        val depEpochMillis = depZdt.toInstant().toEpochMilli()
        val arrEpochMillis = arrZdt.toInstant().toEpochMilli()

        // Current time is 07:00 on the departure day (2 hours before departure)
        val currentTimeMillis = depZdt.minusHours(2).toInstant().toEpochMilli()

        val shinkansen = TransportSegmentItem(
            id = "train1",
            order = 1,
            type = TransportMode.TRAIN_SHINKANSEN,
            operatorName = "JR Central",
            lineOrNumber = "Nozomi #21 (のぞみ)",
            departureCode = "TYO",
            departureName = "Tokyo Station (東京駅)",
            departureZoneId = zone,
            departureTerminalOrPlatform = "Track 16",
            departureTimeIso = depIso,
            arrivalCode = "KYO",
            arrivalName = "Kyoto Station (京都駅)",
            arrivalZoneId = zone,
            arrivalTerminalOrPlatform = "Track 13",
            arrivalTimeIso = arrIso,
            seatOrCarInfo = "Car 7 · Seat 12E (Fuji View)",
            barcodeMessage = "SMARTEX-QR-TYO-KYO"
        )

        val alarms = useCase(shinkansen, currentTimeMillis)

        assertEquals(3, alarms.size)

        // 1. Departure 60m (08:00 Tokyo time)
        val dep60m = alarms.first { it.alarmType == TransportAlarmType.DEPARTURE_60M }
        assertEquals(depEpochMillis - (60 * 60 * 1000L), dep60m.triggerTimeEpochMillis)
        assertEquals("Track 16", dep60m.platform)

        // 2. Departure 15m (08:45 Tokyo time)
        val dep15m = alarms.first { it.alarmType == TransportAlarmType.DEPARTURE_15M }
        assertEquals(depEpochMillis - (15 * 60 * 1000L), dep15m.triggerTimeEpochMillis)
        assertEquals("Track 16", dep15m.platform)

        // 3. Arrival 5m before arrival (11:10 Tokyo time)
        val arr5m = alarms.first { it.alarmType == TransportAlarmType.ARRIVAL_APPROACHING_5M }
        assertEquals(arrEpochMillis - (5 * 60 * 1000L), arr5m.triggerTimeEpochMillis)
        assertEquals("Track 13", arr5m.platform)
        assertEquals("Car 7 · Seat 12E (Fuji View)", arr5m.carAndSeat)
        assertEquals("SMARTEX-QR-TYO-KYO", arr5m.qrCodePayload)
    }

    @Test
    fun testCurrentTimeAfterArrivalGeneratesNoAlarms() {
        val depIso = "2026-11-14T09:00"
        val arrIso = "2026-11-14T11:15"
        val zone = "Asia/Tokyo"
        val arrZdt = ZonedDateTime.of(LocalDateTime.parse(arrIso), ZoneId.of(zone))

        // Current time is 11:30 (15 minutes after arrival)
        val currentTimeMillis = arrZdt.plusMinutes(15).toInstant().toEpochMilli()

        val shinkansen = TransportSegmentItem(
            id = "train1",
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

        val alarms = useCase(shinkansen, currentTimeMillis)

        assertTrue(alarms.isEmpty())
    }

    @Test
    fun testMissingOrInvalidArrivalTimeOmitsArrivalAlarm() {
        val depIso = "2026-11-14T09:00"
        val zone = "Asia/Tokyo"
        val depZdt = ZonedDateTime.of(LocalDateTime.parse(depIso), ZoneId.of(zone))
        val currentTimeMillis = depZdt.minusHours(2).toInstant().toEpochMilli()

        val bus = TransportSegmentItem(
            id = "bus1",
            order = 1,
            type = TransportMode.BUS,
            operatorName = "Fuji Kyuko Bus",
            lineOrNumber = "Highway Bus",
            departureCode = "SHI",
            departureName = "Shinjuku Expressway Bus Terminal",
            departureZoneId = zone,
            departureTerminalOrPlatform = "Bay 4",
            departureTimeIso = depIso,
            arrivalCode = "FUJ",
            arrivalName = "Kawaguchiko",
            arrivalZoneId = zone,
            arrivalTerminalOrPlatform = "",
            arrivalTimeIso = "", // empty arrival time
            seatOrCarInfo = "Seat 4B"
        )

        val alarms = useCase(bus, currentTimeMillis)

        // Only departure 60m and 15m should be returned, no arrival alarm
        assertEquals(2, alarms.size)
        assertTrue(alarms.any { it.alarmType == TransportAlarmType.DEPARTURE_60M })
        assertTrue(alarms.any { it.alarmType == TransportAlarmType.DEPARTURE_15M })
        assertTrue(alarms.none { it.alarmType == TransportAlarmType.ARRIVAL_APPROACHING_5M })
    }

    @Test
    fun testSubwayMetroGeneratesDepartureAndArrivalAlarms() {
        val depIso = "2026-11-15T14:00"
        val arrIso = "2026-11-15T14:35"
        val zone = "Asia/Tokyo"
        val depZdt = ZonedDateTime.of(LocalDateTime.parse(depIso), ZoneId.of(zone))
        val arrZdt = ZonedDateTime.of(LocalDateTime.parse(arrIso), ZoneId.of(zone))
        val currentTimeMillis = depZdt.minusHours(2).toInstant().toEpochMilli()

        val metro = TransportSegmentItem(
            id = "metro1",
            order = 1,
            type = TransportMode.SUBWAY_METRO,
            operatorName = "Tokyo Metro",
            lineOrNumber = "Ginza Line",
            departureCode = "G19",
            departureName = "Asakusa",
            departureZoneId = zone,
            departureTerminalOrPlatform = "Platform 1",
            departureTimeIso = depIso,
            arrivalCode = "G01",
            arrivalName = "Shibuya",
            arrivalZoneId = zone,
            arrivalTerminalOrPlatform = "Platform 2",
            arrivalTimeIso = arrIso,
            seatOrCarInfo = "Car 3"
        )

        val alarms = useCase(metro, currentTimeMillis)

        assertEquals(3, alarms.size)
        assertTrue(alarms.any { it.alarmType == TransportAlarmType.DEPARTURE_60M })
        assertTrue(alarms.any { it.alarmType == TransportAlarmType.DEPARTURE_15M })
        assertTrue(alarms.any { it.alarmType == TransportAlarmType.ARRIVAL_APPROACHING_5M })
    }

    @Test
    fun testDeterministicRequestCodeGeneration() {
        val segmentId = "segment-shinkansen-tokyo-kyoto"
        val type1 = TransportAlarmType.DEPARTURE_60M
        val type2 = TransportAlarmType.DEPARTURE_15M

        val code1A = TransportAlarmPendingIntentHelper.generateRequestCode(segmentId, type1)
        val code1B = TransportAlarmPendingIntentHelper.generateRequestCode(segmentId, type1)
        val code2 = TransportAlarmPendingIntentHelper.generateRequestCode(segmentId, type2)
        val codeDiffSegment = TransportAlarmPendingIntentHelper.generateRequestCode("other-seg", type1)

        // Deterministic: repeated call gives identical result
        assertEquals(code1A, code1B)

        // Different alarm types on same segment yield different codes
        assertNotEquals(code1A, code2)

        // Different segments yield different codes
        assertNotEquals(code1A, codeDiffSegment)

        // Non-negative integer (valid Android RequestCode)
        assertTrue(code1A >= 0)
        assertTrue(code2 >= 0)
        assertTrue(codeDiffSegment >= 0)

        // Exact match with formula: (segmentId.hashCode() xor (alarmType.ordinal * 31)) and 0x7FFFFFFF
        val expected = (segmentId.hashCode() xor (type1.ordinal * 31)) and 0x7FFFFFFF
        assertEquals(expected, code1A)
    }
}
