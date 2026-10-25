package com.laralbir.japantraveltools.feature.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

enum class WeatherConditionKind(
    val emoji: String,
    val labelEs: String,
    val labelEn: String
) {
    CLEAR_SUNNY("☀️", "Soleado / Despejado", "Sunny / Clear"),
    PARTLY_CLOUDY("⛅", "Parcialmente Nublado", "Partly Cloudy"),
    RAIN("🌧️", "Lluvia", "Rain"),
    SNOW("❄️", "Nieve", "Snow"),
    HEAVY_RAIN_STORM("⛈️", "Lluvia Intensa / Tormenta", "Heavy Rain / Storm")
}

enum class PlanningSuitability(
    val badgeEs: String,
    val badgeEn: String
) {
    OUTDOOR_OPTIMAL("⛩️ Ideal Exterior (Templos / Parques)", "⛩️ Outdoor Optimal (Shrines / Parks)"),
    INDOOR_RECOMMENDED("🏛️ Recomendado Cubierto (Museos / Shotengai)", "🏛️ Indoor Recommended (Museums / Arcades)")
}

enum class JapanWeatherCity(
    val key: String,
    val nameEs: String,
    val nameEn: String,
    val latitude: Double,
    val longitude: Double
) {
    TOKYO("TOKYO", "Tokio", "Tokyo", 35.6762, 139.6503),
    KYOTO("KYOTO", "Kioto", "Kyoto", 35.0116, 135.7681),
    OSAKA("OSAKA", "Osaka", "Osaka", 34.6937, 135.5023),
    HAKONE_FUJI("HAKONE_FUJI", "Hakone / Monte Fuji", "Hakone / Mt. Fuji", 35.2324, 139.1069)
}

data class WeatherDayForecast(
    val cityKey: String,
    val dateIso: String,
    val wmoCode: Int,
    val tempMaxC: Double,
    val tempMinC: Double,
    val precipitationProbabilityMax: Int,
    val condition: WeatherConditionKind,
    val suitability: PlanningSuitability,
    val fujiVisibilityPercent: Int,
    val userPlanningAdviceEs: String,
    val userPlanningAdviceEn: String,
    val aiPromptContextSummary: String
)

object SharedWeatherForecastStore {
    private val _forecastsState = MutableStateFlow(OpenMeteoWeatherUseCase.defaultSeedForecasts())
    val forecastsState: StateFlow<List<WeatherDayForecast>> = _forecastsState.asStateFlow()

    fun updateCityForecasts(cityKey: String, updatedDays: List<WeatherDayForecast>) {
        if (updatedDays.isEmpty()) return
        val others = _forecastsState.value.filterNot { it.cityKey == cityKey }
        _forecastsState.value = others + updatedDays
    }

    fun forecastForDayNumber(dayNumber: Int): WeatherDayForecast {
        val targetDate = when (dayNumber) {
            1 -> "2026-11-11"
            2 -> "2026-11-12"
            else -> "2026-11-13"
        }
        val targetCity = when (dayNumber) {
            1 -> "TOKYO"
            2 -> "KYOTO"
            else -> "OSAKA"
        }
        return _forecastsState.value.firstOrNull { it.dateIso == targetDate && it.cityKey == targetCity }
            ?: _forecastsState.value.firstOrNull { it.cityKey == targetCity }
            ?: OpenMeteoWeatherUseCase.defaultSeedForecasts().first()
    }
}

object OpenMeteoWeatherUseCase {

    fun buildOpenMeteoUrl(latitude: Double, longitude: Double): String {
        return "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude" +
            "&longitude=$longitude" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=Asia%2FTokyo" +
            "&forecast_days=5"
    }

    fun classifyWmoCode(wmoCode: Int): WeatherConditionKind {
        return when (wmoCode) {
            0, 1 -> WeatherConditionKind.CLEAR_SUNNY
            2, 3, 45, 48 -> WeatherConditionKind.PARTLY_CLOUDY
            in 71..77, 85, 86 -> WeatherConditionKind.SNOW
            in 95..99, 65, 82 -> WeatherConditionKind.HEAVY_RAIN_STORM
            in 51..67, in 80..81 -> WeatherConditionKind.RAIN
            else -> WeatherConditionKind.PARTLY_CLOUDY
        }
    }

    fun evaluateForecastDay(
        cityKey: String,
        dateIso: String,
        wmoCode: Int,
        tempMaxC: Double,
        tempMinC: Double,
        precipitationProbabilityMax: Int
    ): WeatherDayForecast {
        val condition = classifyWmoCode(wmoCode)
        val isWetOrStormy = precipitationProbabilityMax >= 50 ||
            condition == WeatherConditionKind.RAIN ||
            condition == WeatherConditionKind.HEAVY_RAIN_STORM ||
            condition == WeatherConditionKind.SNOW

        val suitability = if (isWetOrStormy) {
            PlanningSuitability.INDOOR_RECOMMENDED
        } else {
            PlanningSuitability.OUTDOOR_OPTIMAL
        }

        val fujiVisibility = when (condition) {
            WeatherConditionKind.CLEAR_SUNNY -> (95 - precipitationProbabilityMax / 2).coerceIn(70, 98)
            WeatherConditionKind.PARTLY_CLOUDY -> (60 - precipitationProbabilityMax / 2).coerceIn(35, 65)
            else -> 10
        }

        val maxRounded = tempMaxC.roundToInt()
        val minRounded = tempMinC.roundToInt()

        val adviceEs = if (suitability == PlanningSuitability.INDOOR_RECOMMENDED) {
            "☔ Lluvia prevista ($precipitationProbabilityMax%, $maxRounded°/$minRounded°C): Llevar paraguas y priorizar paradas cubiertas (Mercado Nishiki/Kuromon, museos o galerías Shotengai)."
        } else {
            "☀️ Día excelente al aire libre ($maxRounded°/$minRounded°C, lluvia $precipitationProbabilityMax%): Ideal para templos, jardines, fotos exteriores y miradores (Visibilidad Fuji ~$fujiVisibility%)."
        }

        val adviceEn = if (suitability == PlanningSuitability.INDOOR_RECOMMENDED) {
            "☔ Rain expected ($precipitationProbabilityMax%, $maxRounded°/$minRounded°C): Carry an umbrella and prioritize indoor stops (Nishiki/Kuromon markets, museums, Shotengai arcades)."
        } else {
            "☀️ Great outdoor weather ($maxRounded°/$minRounded°C, rain $precipitationProbabilityMax%): Ideal for shrines, gardens, outdoor photography & viewpoints (Fuji visibility ~$fujiVisibility%)."
        }

        val aiContext = "[Open-Meteo $cityKey $dateIso: ${condition.name}, ${maxRounded}C/${minRounded}C, rain=${precipitationProbabilityMax}%, suitability=${suitability.name}, fujiVisibility=${fujiVisibility}%]"

        return WeatherDayForecast(
            cityKey = cityKey,
            dateIso = dateIso,
            wmoCode = wmoCode,
            tempMaxC = tempMaxC,
            tempMinC = tempMinC,
            precipitationProbabilityMax = precipitationProbabilityMax,
            condition = condition,
            suitability = suitability,
            fujiVisibilityPercent = fujiVisibility,
            userPlanningAdviceEs = adviceEs,
            userPlanningAdviceEn = adviceEn,
            aiPromptContextSummary = aiContext
        )
    }

    fun parseOpenMeteoDailyJson(cityKey: String, rawJson: String): List<WeatherDayForecast> {
        val dailyBlock = rawJson.substringAfter("\"daily\"", "")
        if (dailyBlock.isBlank()) return emptyList()

        val dates = extractStringArray(dailyBlock, "time")
        val codes = extractNumberArray(dailyBlock, "weather_code").map { it.toInt() }
        val maxTemps = extractNumberArray(dailyBlock, "temperature_2m_max")
        val minTemps = extractNumberArray(dailyBlock, "temperature_2m_min")
        val rainProbs = extractNumberArray(dailyBlock, "precipitation_probability_max").map { it.toInt() }

        val count = listOf(dates.size, codes.size, maxTemps.size, minTemps.size).minOrNull() ?: 0
        if (count == 0) return emptyList()

        return (0 until count).map { idx ->
            evaluateForecastDay(
                cityKey = cityKey,
                dateIso = dates[idx],
                wmoCode = codes[idx],
                tempMaxC = maxTemps[idx],
                tempMinC = minTemps[idx],
                precipitationProbabilityMax = rainProbs.getOrElse(idx) { 15 }
            )
        }
    }

    fun defaultSeedForecasts(): List<WeatherDayForecast> = listOf(
        evaluateForecastDay(
            cityKey = "TOKYO",
            dateIso = "2026-11-11",
            wmoCode = 1,
            tempMaxC = 19.5,
            tempMinC = 11.0,
            precipitationProbabilityMax = 10
        ),
        evaluateForecastDay(
            cityKey = "KYOTO",
            dateIso = "2026-11-12",
            wmoCode = 61,
            tempMaxC = 15.2,
            tempMinC = 9.0,
            precipitationProbabilityMax = 75
        ),
        evaluateForecastDay(
            cityKey = "OSAKA",
            dateIso = "2026-11-13",
            wmoCode = 2,
            tempMaxC = 18.8,
            tempMinC = 10.5,
            precipitationProbabilityMax = 20
        ),
        evaluateForecastDay(
            cityKey = "HAKONE_FUJI",
            dateIso = "2026-11-14",
            wmoCode = 0,
            tempMaxC = 14.0,
            tempMinC = 5.5,
            precipitationProbabilityMax = 5
        )
    )

    suspend fun fetchCityForecastOnline(city: JapanWeatherCity): Result<List<WeatherDayForecast>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(buildOpenMeteoUrl(city.latitude, city.longitude))
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4500
                    readTimeout = 4500
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = parseOpenMeteoDailyJson(city.key, body)
                require(parsed.isNotEmpty()) { "Empty Open-Meteo daily forecast" }
                parsed
            }
        }

    private fun extractStringArray(block: String, key: String): List<String> {
        val afterKey = block.substringAfter("\"$key\"", "")
        val arrayContent = afterKey.substringAfter("[", "").substringBefore("]", "")
        if (arrayContent.isBlank()) return emptyList()
        return Regex("\"([^\"]+)\"").findAll(arrayContent).map { it.groupValues[1] }.toList()
    }

    private fun extractNumberArray(block: String, key: String): List<Double> {
        val afterKey = block.substringAfter("\"$key\"", "")
        val arrayContent = afterKey.substringAfter("[", "").substringBefore("]", "")
        if (arrayContent.isBlank()) return emptyList()
        return arrayContent.split(",")
            .mapNotNull { it.trim().toDoubleOrNull() }
    }
}
