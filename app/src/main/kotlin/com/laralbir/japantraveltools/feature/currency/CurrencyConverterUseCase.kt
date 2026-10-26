package com.laralbir.japantraveltools.feature.currency

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
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

object ExchangeRateJsonCodec {
    fun exportToJson(snapshot: ExchangeRateSnapshot): String {
        val ratesEntries = snapshot.rates.entries.joinToString(",") { (k, v) ->
            "\"$k\":$v"
        }
        return """
            {
              "baseCurrency": "${snapshot.baseCurrency}",
              "rateDate": "${snapshot.rateDate}",
              "rates": {$ratesEntries},
              "isSeedData": ${snapshot.isSeedData},
              "lastSyncedEpochMillis": ${snapshot.lastSyncedEpochMillis}
            }
        """.trimIndent()
    }

    fun importFromJson(json: String): ExchangeRateSnapshot {
        if (json.isBlank()) return SeedExchangeRates.defaultSnapshot()
        return try {
            val baseRegex = """"baseCurrency"\s*:\s*"([A-Z]{3})"""".toRegex()
            val dateRegex = """"rateDate"\s*:\s*"([^"]+)"""".toRegex()
            val ratesBlockRegex = """"rates"\s*:\s*\{([^}]+)\}""".toRegex()
            val isSeedRegex = """"isSeedData"\s*:\s*(true|false)""".toRegex()
            val syncedRegex = """"lastSyncedEpochMillis"\s*:\s*(\d+)""".toRegex()
            val rateEntryRegex = """"([A-Z]{3})"\s*:\s*([0-9.]+)""".toRegex()

            val base = baseRegex.find(json)?.groupValues?.get(1) ?: return SeedExchangeRates.defaultSnapshot()
            val date = dateRegex.find(json)?.groupValues?.get(1) ?: return SeedExchangeRates.defaultSnapshot()
            val isSeed = isSeedRegex.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: false
            val synced = syncedRegex.find(json)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

            val ratesContent = ratesBlockRegex.find(json)?.groupValues?.get(1) ?: ""
            val parsedRates = mutableMapOf<String, Double>()
            rateEntryRegex.findAll(ratesContent).forEach { match ->
                val code = match.groupValues[1]
                val value = match.groupValues[2].toDoubleOrNull()
                if (value != null) {
                    parsedRates[code] = value
                }
            }

            if (parsedRates.isEmpty()) return SeedExchangeRates.defaultSnapshot()

            ExchangeRateSnapshot(
                baseCurrency = base,
                rateDate = date,
                rates = parsedRates,
                isSeedData = isSeed,
                lastSyncedEpochMillis = synced
            )
        } catch (_: Exception) {
            SeedExchangeRates.defaultSnapshot()
        }
    }
}

object SharedCurrencyRateStore {
    private var appContext: Context? = null
    private const val RATES_FILE_NAME = "saved_currency_rates.json"

    private val _rateState = MutableStateFlow(SeedExchangeRates.defaultSnapshot())
    val rateState: StateFlow<ExchangeRateSnapshot> = _rateState.asStateFlow()

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            loadFromDisk()
        }
    }

    fun loadFromDisk() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, RATES_FILE_NAME)
            if (file.exists() && file.length() > 0) {
                val json = file.readText()
                val snapshot = ExchangeRateJsonCodec.importFromJson(json)
                _rateState.value = snapshot
            }
        } catch (_: Exception) {}
    }

    fun saveToDisk() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, RATES_FILE_NAME)
            file.writeText(ExchangeRateJsonCodec.exportToJson(_rateState.value))
        } catch (_: Exception) {}
    }

    fun updateSnapshot(fresh: ExchangeRateSnapshot) {
        _rateState.value = fresh
        saveToDisk()
    }

    suspend fun syncRatesOnline(): Result<ExchangeRateSnapshot> {
        val result = FrankfurterJsonParser.fetchFromFrankfurterApi()
        result.onSuccess { fresh ->
            updateSnapshot(fresh)
        }
        return result
    }
}


