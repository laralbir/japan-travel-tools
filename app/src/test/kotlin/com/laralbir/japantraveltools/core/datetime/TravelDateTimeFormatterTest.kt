package com.laralbir.japantraveltools.core.datetime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Locale

class TravelDateTimeFormatterTest {

    @Test
    fun `formatFullLocalDate formats date according to regional system locale`() {
        val dateIso = "2027-03-15"

        // Spanish locale test
        val formattedEs = TravelDateTimeFormatter.formatFullLocalDate(dateIso, Locale.forLanguageTag("es-ES"))
        assertTrue("Expected Monday in Spanish, got: $formattedEs", formattedEs.startsWith("Lunes") || formattedEs.startsWith("lunes"))
        assertTrue(formattedEs.contains("15"))
        assertTrue(formattedEs.contains("marzo"))
        assertTrue(formattedEs.contains("2027"))

        // English US locale test
        val formattedEn = TravelDateTimeFormatter.formatFullLocalDate(dateIso, Locale.US)
        assertTrue("Expected Monday in English, got: $formattedEn", formattedEn.startsWith("Monday"))
        assertTrue(formattedEn.contains("March"))
        assertTrue(formattedEn.contains("15"))
        assertTrue(formattedEn.contains("2027"))
    }

    @Test
    fun `formatLocalTimeWithZone displays localized time, offset and location name`() {
        // Munich test: 12:25 in Europe/Berlin (UTC+1 standard time in March 15)
        val munichTime = TravelDateTimeFormatter.formatLocalTimeWithZone(
            timeHhMm = "12:25",
            dateIso = "2027-03-15",
            zoneId = "Europe/Berlin",
            locationName = "Múnich",
            isEs = true
        )
        assertTrue("Should contain 12:25", munichTime.contains("12:25"))
        assertTrue("Should contain Múnich", munichTime.contains("Múnich"))
        assertTrue("Should contain UTC+1 or CET", munichTime.contains("UTC+1") || munichTime.contains("CET"))

        // Osaka test: 07:55 in Asia/Tokyo (UTC+9)
        val osakaTime = TravelDateTimeFormatter.formatLocalTimeWithZone(
            timeHhMm = "07:55",
            dateIso = "2027-03-16",
            zoneId = "Asia/Tokyo",
            locationName = "Osaka",
            isEs = true
        )
        assertTrue("Should contain 07:55", osakaTime.contains("07:55"))
        assertTrue("Should contain Osaka", osakaTime.contains("Osaka"))
        assertTrue("Should contain UTC+9 or JST", osakaTime.contains("UTC+9") || osakaTime.contains("JST"))
    }

    @Test
    fun `localToUtcIso and utcIsoToLocal convert correctly and bidirectionally`() {
        // Munich: 2027-03-15 12:25 in Europe/Berlin -> UTC is 11:25
        val utcMunich = TravelDateTimeFormatter.localToUtcIso(
            dateIso = "2027-03-15",
            timeHhMm = "12:25",
            zoneId = "Europe/Berlin"
        )
        assertEquals("2027-03-15T11:25:00Z", utcMunich)

        val (backDateMunich, backTimeMunich) = TravelDateTimeFormatter.utcIsoToLocal(utcMunich, "Europe/Berlin")
        assertEquals("2027-03-15", backDateMunich)
        assertEquals("12:25", backTimeMunich)

        // Osaka: 2027-03-16 07:55 in Asia/Tokyo (UTC+9) -> UTC is 2027-03-15 22:55
        val utcOsaka = TravelDateTimeFormatter.localToUtcIso(
            dateIso = "2027-03-16",
            timeHhMm = "07:55",
            zoneId = "Asia/Tokyo"
        )
        assertEquals("2027-03-15T22:55:00Z", utcOsaka)

        val (backDateOsaka, backTimeOsaka) = TravelDateTimeFormatter.utcIsoToLocal(utcOsaka, "Asia/Tokyo")
        assertEquals("2027-03-16", backDateOsaka)
        assertEquals("07:55", backTimeOsaka)
    }

    @Test
    fun `inferZoneIdFromCoordinatesOrLocation resolves destinations correctly`() {
        // Madrid Barajas coordinates (40.4983, -3.5676)
        val madZone = TravelDateTimeFormatter.inferZoneIdFromCoordinatesOrLocation(
            lat = 40.4983,
            lng = -3.5676,
            locationText = "Madrid-Barajas"
        )
        assertEquals("Europe/Madrid", madZone)

        // Munich Airport coordinates (48.3537, 11.775)
        val mucZone = TravelDateTimeFormatter.inferZoneIdFromCoordinatesOrLocation(
            lat = 48.3537,
            lng = 11.775,
            locationText = "Múnich"
        )
        assertEquals("Europe/Berlin", mucZone)

        // Osaka Kansai coordinates (34.4347, 135.2442)
        val kixZone = TravelDateTimeFormatter.inferZoneIdFromCoordinatesOrLocation(
            lat = 34.4347,
            lng = 135.2442,
            locationText = "Osaka Kansai"
        )
        assertEquals("Asia/Tokyo", kixZone)

        // Tokyo Senso-ji (35.7147, 139.7966)
        val tyoZone = TravelDateTimeFormatter.inferZoneIdFromCoordinatesOrLocation(
            lat = 35.7147,
            lng = 139.7966,
            locationText = "Senso-ji Asakusa"
        )
        assertEquals("Asia/Tokyo", tyoZone)
    }
}
