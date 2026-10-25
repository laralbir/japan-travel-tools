package com.laralbir.japantraveltools.feature.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TransportAlertEvaluatorTest {

    private val sampleFlight = TransportSegmentItem(
        id = "flight-101",
        order = 1,
        type = TransportMode.FLIGHT,
        operatorName = "Iberia",
        lineOrNumber = "IB6801",
        departureCode = "MAD",
        departureName = "Madrid Barajas",
        departureZoneId = "Europe/Madrid",
        departureTerminalOrPlatform = "Terminal 4S · Mostrador 810",
        checkInTime = "2026-11-10T12:30",
        boardingTime = "2026-11-10T14:40",
        departureTimeIso = "2026-11-10T15:30",
        arrivalCode = "NRT",
        arrivalName = "Tokyo Narita",
        arrivalZoneId = "Asia/Tokyo",
        arrivalTerminalOrPlatform = "Terminal 1",
        arrivalTimeIso = "2026-11-11T10:00",
        seatOrCarInfo = "Puerta S22 · Asiento 12A"
    )

    private val sampleTrain = TransportSegmentItem(
        id = "train-202",
        order = 2,
        type = TransportMode.TRAIN_SHINKANSEN,
        operatorName = "JR Central",
        lineOrNumber = "Nozomi #21",
        departureCode = "TYO",
        departureName = "Tokyo Station",
        departureZoneId = "Asia/Tokyo",
        departureTerminalOrPlatform = "Andén 16",
        checkInTime = null,
        boardingTime = "2026-11-14T08:50",
        departureTimeIso = "2026-11-14T09:00",
        arrivalCode = "KYO",
        arrivalName = "Kyoto Station",
        arrivalZoneId = "Asia/Tokyo",
        arrivalTerminalOrPlatform = "Andén 13",
        arrivalTimeIso = "2026-11-14T11:15",
        seatOrCarInfo = "Coche 7 · Asiento 12E"
    )

    @Test
    fun `detects CHECK_IN_APPROACHING when 60 minutes before checkInTime`() {
        val now = ZonedDateTime.of(
            LocalDateTime.parse("2026-11-10T11:30"),
            ZoneId.of("Europe/Madrid")
        )
        val alert = TransportAlertEvaluator.evaluateSegmentAlert(sampleFlight, now)

        assertNotNull(alert)
        assertEquals(TransportAlertType.CHECK_IN_APPROACHING, alert?.type)
        assertEquals(TransportAlertSeverity.WARNING, alert?.severity)
        assertEquals(60L, alert?.minutesUntilEvent)
        assertTrue(alert?.titleEs?.contains("Facturación") == true)
        assertTrue(alert?.messageEs?.contains("Mostrador 810") == true)
    }

    @Test
    fun `detects CHECK_IN_OPEN when time is between checkIn and boarding`() {
        val now = ZonedDateTime.of(
            LocalDateTime.parse("2026-11-10T13:00"),
            ZoneId.of("Europe/Madrid")
        )
        val alert = TransportAlertEvaluator.evaluateSegmentAlert(sampleFlight, now)

        assertNotNull(alert)
        assertEquals(TransportAlertType.CHECK_IN_OPEN, alert?.type)
        assertEquals(TransportAlertSeverity.WARNING, alert?.severity)
        assertTrue(alert?.titleEs?.contains("Facturación abierta") == true)
    }

    @Test
    fun `detects BOARDING_APPROACHING when 35 minutes before boardingTime`() {
        val now = ZonedDateTime.of(
            LocalDateTime.parse("2026-11-10T14:05"),
            ZoneId.of("Europe/Madrid")
        )
        val alert = TransportAlertEvaluator.evaluateSegmentAlert(sampleFlight, now)

        assertNotNull(alert)
        assertEquals(TransportAlertType.BOARDING_APPROACHING, alert?.type)
        assertEquals(TransportAlertSeverity.WARNING, alert?.severity)
        assertEquals(35L, alert?.minutesUntilEvent)
        assertTrue(alert?.titleEs?.contains("Embarque próximo") == true)
        assertTrue(alert?.messageEs?.contains("Puerta S22") == true)
    }

    @Test
    fun `detects BOARDING_NOW with URGENT severity when 10 minutes before boarding`() {
        val now = ZonedDateTime.of(
            LocalDateTime.parse("2026-11-10T14:30"),
            ZoneId.of("Europe/Madrid")
        )
        val alert = TransportAlertEvaluator.evaluateSegmentAlert(sampleFlight, now)

        assertNotNull(alert)
        assertEquals(TransportAlertType.BOARDING_NOW, alert?.type)
        assertEquals(TransportAlertSeverity.URGENT, alert?.severity)
        assertEquals(10L, alert?.minutesUntilEvent)
        assertTrue(alert?.titleEs?.contains("¡Embarque") == true)
    }

    @Test
    fun `detects DEPARTURE_IMMINENT when boarding time is passed and 15 mins before departure`() {
        val now = ZonedDateTime.of(
            LocalDateTime.parse("2026-11-10T15:15"),
            ZoneId.of("Europe/Madrid")
        )
        val alert = TransportAlertEvaluator.evaluateSegmentAlert(sampleFlight, now)

        assertNotNull(alert)
        assertEquals(TransportAlertType.DEPARTURE_IMMINENT, alert?.type)
        assertEquals(TransportAlertSeverity.URGENT, alert?.severity)
        assertEquals(15L, alert?.minutesUntilEvent)
    }

    @Test
    fun `returns null if flight is more than 3 hours away from checkIn`() {
        val now = ZonedDateTime.of(
            LocalDateTime.parse("2026-11-10T08:00"),
            ZoneId.of("Europe/Madrid")
        )
        val alert = TransportAlertEvaluator.evaluateSegmentAlert(sampleFlight, now)
        assertNull(alert)
    }

    @Test
    fun `detects train departure alert within 30 minutes with platform and car`() {
        val now = ZonedDateTime.of(
            LocalDateTime.parse("2026-11-14T08:40"),
            ZoneId.of("Asia/Tokyo")
        )
        val alert = TransportAlertEvaluator.evaluateSegmentAlert(sampleTrain, now)

        assertNotNull(alert)
        assertEquals(TransportAlertType.BOARDING_NOW, alert?.type)
        assertEquals(10L, alert?.minutesUntilEvent)
        assertTrue(alert?.messageEs?.contains("Andén 16") == true)
        assertTrue(alert?.messageEs?.contains("Coche 7") == true)
    }

    @Test
    fun `filters out dismissed alerts`() {
        val now = ZonedDateTime.of(
            LocalDateTime.parse("2026-11-10T14:05"),
            ZoneId.of("Europe/Madrid")
        )
        val dismissed = setOf("flight-101_${TransportAlertType.BOARDING_APPROACHING.name}")
        val alerts = TransportAlertEvaluator.evaluateSegmentAlerts(
            segments = listOf(sampleFlight),
            now = now,
            dismissedAlertKeys = dismissed
        )
        assertTrue(alerts.isEmpty())
    }
}
