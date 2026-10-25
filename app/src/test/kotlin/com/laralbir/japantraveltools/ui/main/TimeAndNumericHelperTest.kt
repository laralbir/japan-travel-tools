package com.laralbir.japantraveltools.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeAndNumericHelperTest {

    // ========================================================================
    // TimeSelectorHelper Tests
    // ========================================================================

    @Test
    fun `parseHourAndMinute parses valid 24h formatted time strings`() {
        val (h1, m1) = TimeSelectorHelper.parseHourAndMinute("14:35")
        assertEquals(14, h1)
        assertEquals(35, m1)

        val (h2, m2) = TimeSelectorHelper.parseHourAndMinute("09:05")
        assertEquals(9, h2)
        assertEquals(5, m2)

        val (h3, m3) = TimeSelectorHelper.parseHourAndMinute("8:30")
        assertEquals(8, h3)
        assertEquals(30, m3)

        val (h4, m4) = TimeSelectorHelper.parseHourAndMinute("00:00")
        assertEquals(0, h4)
        assertEquals(0, m4)

        val (h5, m5) = TimeSelectorHelper.parseHourAndMinute("23:59")
        assertEquals(23, h5)
        assertEquals(59, m5)
    }

    @Test
    fun `parseHourAndMinute falls back to default on invalid or blank strings`() {
        val (hDef, mDef) = TimeSelectorHelper.parseHourAndMinute("", defaultHour = 9, defaultMinute = 0)
        assertEquals(9, hDef)
        assertEquals(0, mDef)

        val (hInv, mInv) = TimeSelectorHelper.parseHourAndMinute("invalid", defaultHour = 10, defaultMinute = 30)
        assertEquals(10, hInv)
        assertEquals(30, mInv)

        val (hOutH, mOutH) = TimeSelectorHelper.parseHourAndMinute("25:00", defaultHour = 9, defaultMinute = 0)
        assertEquals(9, hOutH)
        assertEquals(0, mOutH)

        val (hOutM, mOutM) = TimeSelectorHelper.parseHourAndMinute("12:65", defaultHour = 9, defaultMinute = 0)
        assertEquals(9, hOutM)
        assertEquals(0, mOutM)

        val (hNeg, mNeg) = TimeSelectorHelper.parseHourAndMinute("-1:20", defaultHour = 9, defaultMinute = 0)
        assertEquals(9, hNeg)
        assertEquals(0, mNeg)
    }

    @Test
    fun `formatTime formats hour and minute to standard HH mm string`() {
        assertEquals("14:35", TimeSelectorHelper.formatTime(14, 35))
        assertEquals("09:05", TimeSelectorHelper.formatTime(9, 5))
        assertEquals("00:00", TimeSelectorHelper.formatTime(0, 0))
        assertEquals("23:59", TimeSelectorHelper.formatTime(23, 59))
    }

    @Test
    fun `isValidTime validates time strings accurately`() {
        assertTrue(TimeSelectorHelper.isValidTime("14:35"))
        assertTrue(TimeSelectorHelper.isValidTime("00:00"))
        assertTrue(TimeSelectorHelper.isValidTime("23:59"))
        assertTrue(TimeSelectorHelper.isValidTime("9:05"))

        assertFalse(TimeSelectorHelper.isValidTime(""))
        assertFalse(TimeSelectorHelper.isValidTime("24:00"))
        assertFalse(TimeSelectorHelper.isValidTime("12:60"))
        assertFalse(TimeSelectorHelper.isValidTime("invalid"))
        assertFalse(TimeSelectorHelper.isValidTime("12"))
    }

    @Test
    fun `formatDisplayTime returns canonical display or empty when blank`() {
        assertEquals("", TimeSelectorHelper.formatDisplayTime("", isEs = true))
        assertEquals("", TimeSelectorHelper.formatDisplayTime("   ", isEs = false))
        assertEquals("14:35", TimeSelectorHelper.formatDisplayTime("14:35", isEs = true))
        assertEquals("09:05", TimeSelectorHelper.formatDisplayTime("9:05", isEs = false))
    }

    // ========================================================================
    // NumericInputHelper Tests
    // ========================================================================

    @Test
    fun `sanitizeInteger clamps and parses values within specified bounds`() {
        assertEquals(5, NumericInputHelper.sanitizeInteger("5", min = 1, max = 99, default = 1))
        assertEquals(1, NumericInputHelper.sanitizeInteger("0", min = 1, max = 99, default = 1))
        assertEquals(99, NumericInputHelper.sanitizeInteger("150", min = 1, max = 99, default = 1))
        assertEquals(1, NumericInputHelper.sanitizeInteger("", min = 1, max = 99, default = 1))
        assertEquals(1, NumericInputHelper.sanitizeInteger("not-a-number", min = 1, max = 99, default = 1))
        assertEquals(14, NumericInputHelper.sanitizeInteger("  14  ", min = 1, max = 99, default = 1))
    }

    @Test
    fun `increment and decrement step values respecting min and max boundaries`() {
        assertEquals(6, NumericInputHelper.increment(current = 5, step = 1, max = 10))
        assertEquals(10, NumericInputHelper.increment(current = 10, step = 1, max = 10))

        assertEquals(4, NumericInputHelper.decrement(current = 5, step = 1, min = 1))
        assertEquals(1, NumericInputHelper.decrement(current = 1, step = 1, min = 1))
    }

    @Test
    fun `filterNumeric strips non-digits cleanly`() {
        assertEquals("1234", NumericInputHelper.filterNumeric("12abc34"))
        assertEquals("5", NumericInputHelper.filterNumeric("-5"))
        assertEquals("", NumericInputHelper.filterNumeric("abc"))
        assertEquals("14", NumericInputHelper.filterNumeric("  14  "))
    }

    @Test
    fun `filterDecimal keeps only valid decimal characters and single dot`() {
        assertEquals("123.45", NumericInputHelper.filterDecimal("123.45"))
        assertEquals("-35.67", NumericInputHelper.filterDecimal("-35.67"))
        assertEquals("12.34", NumericInputHelper.filterDecimal("12..34"))
        assertEquals("12.5", NumericInputHelper.filterDecimal("abc12.5xyz"))
        assertEquals("", NumericInputHelper.filterDecimal("invalid"))
    }
}
