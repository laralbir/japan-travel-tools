package com.laralbir.japantraveltools.feature.currency

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class ExchangeRateSnapshot(
    val baseCurrency: String,
    val rateDate: String,
    val rates: Map<String, Double>,
    val isSeedData: Boolean,
    val lastSyncedEpochMillis: Long
)

object SeedExchangeRates {
    val supportedCurrencies = listOf("EUR", "JPY", "USD", "GBP", "CHF", "MXN", "CAD", "AUD")

    /**
     * Built-in offline seed rates (ECB reference baseline) so the app works 100% offline
     * even on first boot in Airplane Mode.
     */
    fun defaultSnapshot(): ExchangeRateSnapshot = ExchangeRateSnapshot(
        baseCurrency = "EUR",
        rateDate = "2026-10-25 (Offline Seed)",
        rates = mapOf(
            "EUR" to 1.0,
            "JPY" to 164.80,
            "USD" to 1.084,
            "GBP" to 0.835,
            "CHF" to 0.938,
            "MXN" to 21.40,
            "CAD" to 1.502,
            "AUD" to 1.642
        ),
        isSeedData = true,
        lastSyncedEpochMillis = 1729860000000L
    )
}

object ConvertCurrencyUseCase {
    val japanQuickAmountsJpy = listOf(100, 500, 1000, 5000, 10000)

    fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        snapshot: ExchangeRateSnapshot,
        customDirectRate: Double? = null
    ): Double {
        if (fromCurrency == toCurrency) return amount
        if (customDirectRate != null && customDirectRate > 0.0) {
            return amount * customDirectRate
        }
        val fromRateInEur = if (fromCurrency == snapshot.baseCurrency) {
            1.0
        } else {
            snapshot.rates[fromCurrency] ?: 1.0
        }
        val toRateInEur = if (toCurrency == snapshot.baseCurrency) {
            1.0
        } else {
            snapshot.rates[toCurrency] ?: 1.0
        }
        val amountInEur = amount / fromRateInEur
        return amountInEur * toRateInEur
    }

    fun formatAmount(amount: Double, currencyCode: String): String {
        return if (currencyCode == "JPY") {
            String.format(Locale.US, "%,.0f", amount)
        } else {
            String.format(Locale.US, "%,.2f", amount)
        }
    }
}

object FrankfurterJsonParser {
    /**
     * Lightweight, dependency-free parser for Frankfurter API (`https://api.frankfurter.dev/v1/latest?base=EUR`)
     * that works both in JVM Unit Tests and on Android without org.json stubs issues.
     */
    fun parseLatestRates(json: String, syncedEpochMillis: Long): ExchangeRateSnapshot {
        val baseRegex = """"base"\s*:\s*"([A-Z]{3})"""".toRegex()
        val dateRegex = """"date"\s*:\s*"([^"]+)"""".toRegex()
        val ratesBlockRegex = """"rates"\s*:\s*\{([^}]+)\}""".toRegex()
        val rateEntryRegex = """"([A-Z]{3})"\s*:\s*([0-9.]+)""".toRegex()

        val base = baseRegex.find(json)?.groupValues?.get(1) ?: "EUR"
        val date = dateRegex.find(json)?.groupValues?.get(1) ?: "2026-10-25"
        val ratesContent = ratesBlockRegex.find(json)?.groupValues?.get(1) ?: ""

        val parsedRates = mutableMapOf(base to 1.0)
        rateEntryRegex.findAll(ratesContent).forEach { match ->
            val code = match.groupValues[1]
            val value = match.groupValues[2].toDoubleOrNull()
            if (value != null) {
                parsedRates[code] = value
            }
        }

        return ExchangeRateSnapshot(
            baseCurrency = base,
            rateDate = date,
            rates = parsedRates,
            isSeedData = false,
            lastSyncedEpochMillis = syncedEpochMillis
        )
    }

    suspend fun fetchFromFrankfurterApi(): Result<ExchangeRateSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://api.frankfurter.dev/v1/latest?base=EUR")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException("HTTP $responseCode from Frankfurter API")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parseLatestRates(body, System.currentTimeMillis())
            } finally {
                connection.disconnect()
            }
        }
    }
}
