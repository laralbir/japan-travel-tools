package com.laralbir.japantraveltools.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DateSelectorHelperTest {

    @Test
    fun `parseIsoDateToUtcMillis parses valid ISO date into UTC midnight millis`() {
        val millis = DateSelectorHelper.parseIsoDateToUtcMillis("2026-11-11")
        val expected = LocalDate.of(2026, 11, 11).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(expected, millis)
    }

    @Test
    fun `formatUtcMillisToIsoDate formats UTC millis into ISO date string`() {
        val millis = LocalDate.of(2026, 11, 11).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val formatted = DateSelectorHelper.formatUtcMillisToIsoDate(millis)
        assertEquals("2026-11-11", formatted)
    }

    @Test
    fun `formatDisplayDate formats friendly readable date in Spanish and English`() {
        val displayEs = DateSelectorHelper.formatDisplayDate("2026-11-11", isEs = true)
        assertTrue(displayEs.contains("11"))
        assertTrue(displayEs.contains("nov", ignoreCase = true))
        assertTrue(displayEs.contains("2026"))

        val displayEn = DateSelectorHelper.formatDisplayDate("2026-11-11", isEs = false)
        assertTrue(displayEn.contains("11"))
        assertTrue(displayEn.contains("Nov", ignoreCase = true))
        assertTrue(displayEn.contains("2026"))
    }

    @Test
    fun `fallback handles invalid or blank date string gracefully without crashing`() {
        val millis = DateSelectorHelper.parseIsoDateToUtcMillis("invalid-date")
        assertTrue(millis > 0)

        val display = DateSelectorHelper.formatDisplayDate("", isEs = true)
        assertNotNull(display)
        assertTrue(display.isNotBlank())
    }
}
