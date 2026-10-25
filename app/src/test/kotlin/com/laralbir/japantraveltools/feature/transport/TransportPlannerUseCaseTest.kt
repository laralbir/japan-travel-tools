package com.laralbir.japantraveltools.feature.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportPlannerUseCaseTest {

    @Test
    fun `calculates real flight duration across timezones and layover time before next boarding`() {
        val seg1 = TransportSegmentItem(
            id = "seg1",
            order = 1,
            type = TransportMode.FLIGHT,
            operatorName = "Qatar Airways",
            lineOrNumber = "QR150",
            departureCode = "MAD",
            departureName = "Madrid Barajas",
            departureZoneId = "Europe/Madrid",
            departureTerminalOrPlatform = "T4S",
            checkInTime = "2026-11-10T12:30",
            boardingTime = "2026-11-10T14:40",
            departureTimeIso = "2026-11-10T15:30",
            arrivalCode = "DOH",
            arrivalName = "Doha Hamad",
            arrivalZoneId = "Asia/Qatar",
            arrivalTerminalOrPlatform = "T1",
            arrivalTimeIso = "2026-11-10T23:45",
            seatOrCarInfo = "Gate S22 · Seat 18A"
        )

        val seg2 = TransportSegmentItem(
            id = "seg2",
            order = 2,
            type = TransportMode.FLIGHT,
            operatorName = "Qatar Airways",
            lineOrNumber = "QR806",
            departureCode = "DOH",
            departureName = "Doha Hamad",
            departureZoneId = "Asia/Qatar",
            departureTerminalOrPlatform = "T1",
            checkInTime = null,
            boardingTime = "2026-11-11T01:15",
            departureTimeIso = "2026-11-11T02:15",
            arrivalCode = "HND",
            arrivalName = "Tokyo Haneda (羽田空港)",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "T3",
            arrivalTimeIso = "2026-11-11T17:55",
            seatOrCarInfo = "Gate C14 · Seat 24K"
        )

        // Flight 1 duration: 15:30 CET (14:30 UTC) to 23:45 AST (20:45 UTC) = 6h 15m (375 mins)
        val duration1 = TransportPlannerUseCase.calculateSegmentDurationMinutes(seg1)
        assertEquals(375L, duration1)

        // Flight 2 duration: 02:15 AST (23:15 UTC prev day) to 17:55 JST (08:55 UTC) = 9h 40m (580 mins)
        val duration2 = TransportPlannerUseCase.calculateSegmentDurationMinutes(seg2)
        assertEquals(580L, duration2)

        // Layover in Doha: 23:45 to 02:15 = 150 mins total; 23:45 to 01:15 boarding = 90 mins until boarding
        val layover = TransportPlannerUseCase.calculateConnection(seg1, seg2)
        assertEquals(150L, layover.totalConnectionMinutes)
        assertEquals(90L, layover.minutesUntilNextBoardingOrDeparture)
        assertFalse(layover.isTightConnection)
    }

    @Test
    fun `detects tight transfer between Shinkansen and Metro in Tokyo Station`() {
        val shinkansen = TransportSegmentItem(
            id = "train1",
            order = 1,
            type = TransportMode.TRAIN_SHINKANSEN,
            operatorName = "JR Central",
            lineOrNumber = "Nozomi #21 (のぞみ)",
            departureCode = "TYO",
            departureName = "Tokyo Station (東京駅)",
            departureZoneId = "Asia/Tokyo",
            departureTerminalOrPlatform = "Track 16",
            departureTimeIso = "2026-11-14T09:00",
            arrivalCode = "KYO",
            arrivalName = "Kyoto Station (京都駅)",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "Track 13",
            arrivalTimeIso = "2026-11-14T11:15",
            seatOrCarInfo = "Car 7 · Seat 12E (Fuji View) · JR Pass / SmartEX"
        )

        val metro = TransportSegmentItem(
            id = "metro1",
            order = 2,
            type = TransportMode.SUBWAY_METRO,
            operatorName = "Kyoto Municipal Subway",
            lineOrNumber = "Karasuma Line (K)",
            departureCode = "K11",
            departureName = "Kyoto Station (京都)",
            departureZoneId = "Asia/Tokyo",
            departureTerminalOrPlatform = "Platform 1",
            departureTimeIso = "2026-11-14T11:21",
            arrivalCode = "K08",
            arrivalName = "Karasuma Oike (烏丸御池)",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "Exit 5",
            arrivalTimeIso = "2026-11-14T11:27",
            seatOrCarInfo = "Car 3 · Exit 5 · Suica IC"
        )

        val transfer = TransportPlannerUseCase.calculateConnection(shinkansen, metro)
        assertEquals(6L, transfer.totalConnectionMinutes)
        // 6 minutes between Shinkansen and Subway is flagged as tight (< 10 min threshold)
        assertTrue(transfer.isTightConnection)
    }

    @Test
    fun `createEmptySegment produces clean segment with completely empty text inputs`() {
        val emptySeg = TransportPlannerUseCase.createEmptySegment(id = "seg-new-123", order = 3)

        assertEquals("seg-new-123", emptySeg.id)
        assertEquals(3, emptySeg.order)
        assertEquals(TransportMode.TRAIN_SHINKANSEN, emptySeg.type)
        assertEquals("", emptySeg.operatorName)
        assertEquals("", emptySeg.lineOrNumber)
        assertEquals("", emptySeg.departureCode)
        assertEquals("", emptySeg.departureName)
        assertEquals("", emptySeg.departureTerminalOrPlatform)
        assertEquals(null, emptySeg.checkInTime)
        assertEquals(null, emptySeg.boardingTime)
        assertEquals("", emptySeg.departureTimeIso)
        assertEquals("", emptySeg.arrivalCode)
        assertEquals("", emptySeg.arrivalName)
        assertEquals("", emptySeg.arrivalTerminalOrPlatform)
        assertEquals("", emptySeg.arrivalTimeIso)
        assertEquals("", emptySeg.seatOrCarInfo)
        assertEquals("", emptySeg.stationExitOrBelt)
        assertEquals("", emptySeg.passOrPaymentBadge)
    }

    @Test
    fun `createEmptySegment supports explicit initial mode without injecting sample text`() {
        val emptyFlight = TransportPlannerUseCase.createEmptySegment(
            id = "seg-flight-1",
            order = 1,
            initialMode = TransportMode.FLIGHT
        )

        assertEquals(TransportMode.FLIGHT, emptyFlight.type)
        assertEquals("", emptyFlight.operatorName)
        assertEquals("", emptyFlight.lineOrNumber)
        assertEquals("", emptyFlight.departureName)
        assertEquals("", emptyFlight.arrivalName)
    }
}

