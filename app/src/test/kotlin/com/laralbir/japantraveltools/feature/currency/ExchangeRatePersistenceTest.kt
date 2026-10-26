package com.laralbir.japantraveltools.feature.currency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExchangeRatePersistenceTest {

    @Test
    fun exportAndImportJson_recoversExactSnapshot() {
        val original = ExchangeRateSnapshot(
            baseCurrency = "EUR",
            rateDate = "2026-10-26",
            rates = mapOf(
                "EUR" to 1.0,
                "JPY" to 164.95,
                "USD" to 1.082,
                "GBP" to 0.834
            ),
            isSeedData = false,
            lastSyncedEpochMillis = 1730005555000L
        )

        val json = ExchangeRateJsonCodec.exportToJson(original)
        assertTrue(json.contains("\"baseCurrency\":\"EUR\"") || json.contains("\"baseCurrency\": \"EUR\""))
        assertTrue(json.contains("164.95"))

        val restored = ExchangeRateJsonCodec.importFromJson(json)
        assertEquals("EUR", restored.baseCurrency)
        assertEquals("2026-10-26", restored.rateDate)
        assertEquals(164.95, restored.rates["JPY"] ?: 0.0, 0.001)
        assertEquals(1.082, restored.rates["USD"] ?: 0.0, 0.001)
        assertEquals(0.834, restored.rates["GBP"] ?: 0.0, 0.001)
        assertFalse(restored.isSeedData)
        assertEquals(1730005555000L, restored.lastSyncedEpochMillis)
    }

    @Test
    fun importFromJson_corruptedOrEmptyString_fallsBackToSeedSnapshot() {
        val emptySnapshot = ExchangeRateJsonCodec.importFromJson("")
        assertTrue(emptySnapshot.isSeedData)
        assertEquals("EUR", emptySnapshot.baseCurrency)
        val jpyRate: Double = emptySnapshot.rates["JPY"] ?: 0.0
        assertTrue(jpyRate > 100.0)

        val corruptedSnapshot = ExchangeRateJsonCodec.importFromJson("{ not valid json: true }")
        assertTrue(corruptedSnapshot.isSeedData)
        assertEquals("EUR", corruptedSnapshot.baseCurrency)
    }
}
