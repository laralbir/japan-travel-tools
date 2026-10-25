package com.laralbir.japantraveltools.feature.currency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConverterUseCaseTest {

    @Test
    fun `seed rates provide offline EUR to JPY conversion on first launch without internet`() {
        val seedSnapshot = SeedExchangeRates.defaultSnapshot()
        assertEquals("EUR", seedSnapshot.baseCurrency)
        assertTrue((seedSnapshot.rates["JPY"] ?: 0.0) > 100.0)

        // 10 EUR at 163.50 JPY/EUR -> 1635 JPY
        val convertedJpy = ConvertCurrencyUseCase.convert(
            amount = 10.0,
            fromCurrency = "EUR",
            toCurrency = "JPY",
            snapshot = seedSnapshot.copy(rates = seedSnapshot.rates + ("JPY" to 163.50))
        )
        assertEquals(1635.0, convertedJpy, 0.01)
    }

    @Test
    fun `bidirectional conversion JPY to EUR and custom rate override work accurately`() {
        val snapshot = SeedExchangeRates.defaultSnapshot().copy(
            rates = mapOf("EUR" to 1.0, "JPY" to 160.0, "USD" to 1.08)
        )

        // 1600 JPY -> 10.00 EUR
        val eurAmount = ConvertCurrencyUseCase.convert(
            amount = 1600.0,
            fromCurrency = "JPY",
            toCurrency = "EUR",
            snapshot = snapshot
        )
        assertEquals(10.0, eurAmount, 0.001)

        // Custom manual rate override: 1 EUR = 170 JPY -> 10 EUR = 1700 JPY
        val customConverted = ConvertCurrencyUseCase.convert(
            amount = 10.0,
            fromCurrency = "EUR",
            toCurrency = "JPY",
            snapshot = snapshot,
            customDirectRate = 170.0
        )
        assertEquals(1700.0, customConverted, 0.001)
    }

    @Test
    fun `FrankfurterJsonParser parses official ECB JSON response into ExchangeRateSnapshot`() {
        val sampleJson = """
            {
              "amount": 1.0,
              "base": "EUR",
              "date": "2026-10-25",
              "rates": {
                "AUD": 1.65,
                "CAD": 1.51,
                "CHF": 0.94,
                "GBP": 0.84,
                "JPY": 165.42,
                "MXN": 21.35,
                "USD": 1.085
              }
            }
        """.trimIndent()

        val parsed = FrankfurterJsonParser.parseLatestRates(sampleJson, syncedEpochMillis = 1730000000000L)
        assertEquals("EUR", parsed.baseCurrency)
        assertEquals("2026-10-25", parsed.rateDate)
        assertEquals(165.42, parsed.rates["JPY"] ?: 0.0, 0.001)
        assertEquals(1.085, parsed.rates["USD"] ?: 0.0, 0.001)
    }
}
