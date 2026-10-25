package com.laralbir.japantraveltools.feature.clock

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WorldClockUseCaseTest {

    @Test
    fun `OfflineTimeZoneResolver resolves coordinates inside Japan to Asia Tokyo`() {
        val zone = OfflineTimeZoneResolver.resolveZoneFromCoordinates(
            latitude = 35.6762,
            longitude = 139.6503
        )
        assertEquals(ZoneId.of("Asia/Tokyo"), zone)
    }

    @Test
    fun `OfflineTimeZoneResolver resolves Madrid coordinates to Europe Madrid`() {
        val zone = OfflineTimeZoneResolver.resolveZoneFromCoordinates(
            latitude = 40.4168,
            longitude = -3.7038
        )
        assertEquals(ZoneId.of("Europe/Madrid"), zone)
    }

    @Test
    fun `CalculateClockComparisonUseCase computes exact hour difference in winter and summer DST`() {
        // Winter (no DST in Madrid, UTC+1 vs Tokyo UTC+9 -> +8h difference)
        val winterInstant = Instant.parse("2026-11-15T00:30:00Z")
        val comparisonWinter = CalculateClockComparisonUseCase.calculate(
            instant = winterInstant,
            localZone = ZoneId.of("Asia/Tokyo"),
            originZone = ZoneId.of("Europe/Madrid")
        )
        assertEquals("09:30", comparisonWinter.japanTimeFormatted)
        assertEquals("01:30", comparisonWinter.originTimeFormatted)
        assertEquals(8, comparisonWinter.hoursDifferenceTokyoVsOrigin)
        assertEquals(DayPeriod.NIGHT, comparisonWinter.originDayPeriod)
        assertEquals(DayPeriod.MORNING, comparisonWinter.japanDayPeriod)

        // Summer (DST active in Madrid, UTC+2 vs Tokyo UTC+9 -> +7h difference)
        val summerInstant = Instant.parse("2026-07-15T12:00:00Z")
        val comparisonSummer = CalculateClockComparisonUseCase.calculate(
            instant = summerInstant,
            localZone = ZoneId.of("Asia/Tokyo"),
            originZone = ZoneId.of("Europe/Madrid")
        )
        assertEquals(7, comparisonSummer.hoursDifferenceTokyoVsOrigin)
        assertEquals("21:00", comparisonSummer.japanTimeFormatted)
        assertEquals("14:00", comparisonSummer.originTimeFormatted)
        assertEquals(DayPeriod.AFTERNOON, comparisonSummer.originDayPeriod)
        assertEquals(DayPeriod.EVENING, comparisonSummer.japanDayPeriod)
    }
}
