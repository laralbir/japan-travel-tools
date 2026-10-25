package com.laralbir.japantraveltools.feature.weather

import android.content.Context
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
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

data class WeatherCityItem(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val latitude: Double,
    val longitude: Double,
    val isDefault: Boolean = false
)

object WeatherCityCatalog {
    fun defaultCities(): List<WeatherCityItem> = listOf(
        WeatherCityItem("TOKYO", "Tokio", "Tokyo", 35.6762, 139.6503, isDefault = true),
        WeatherCityItem("KYOTO", "Kioto", "Kyoto", 35.0116, 135.7681, isDefault = true),
        WeatherCityItem("OSAKA", "Osaka", "Osaka", 34.6937, 135.5023, isDefault = true),
        WeatherCityItem("HAKONE_FUJI", "Hakone / Monte Fuji", "Hakone / Mt. Fuji", 35.2324, 139.1069, isDefault = true),
        WeatherCityItem("NARA", "Nara", "Nara", 34.6851, 135.8048, isDefault = false),
        WeatherCityItem("HIROSHIMA", "Hiroshima", "Hiroshima", 34.3853, 132.4553, isDefault = false)
    )

    fun destinationPresets(): List<WeatherCityItem> = listOf(
        WeatherCityItem("TOKYO", "Tokio", "Tokyo", 35.6762, 139.6503),
        WeatherCityItem("KYOTO", "Kioto", "Kyoto", 35.0116, 135.7681),
        WeatherCityItem("OSAKA", "Osaka", "Osaka", 34.6937, 135.5023),
        WeatherCityItem("HAKONE_FUJI", "Hakone / Monte Fuji", "Hakone / Mt. Fuji", 35.2324, 139.1069),
        WeatherCityItem("NARA", "Nara", "Nara", 34.6851, 135.8048),
        WeatherCityItem("HIROSHIMA", "Hiroshima", "Hiroshima", 34.3853, 132.4553),
        WeatherCityItem("MIYAJIMA", "Miyajima / Itsukushima", "Miyajima / Itsukushima", 34.2964, 132.3197),
        WeatherCityItem("KAMAKURA", "Kamakura", "Kamakura", 35.3192, 139.5467),
        WeatherCityItem("NIKKO", "Nikko", "Nikko", 36.7581, 139.5986),
        WeatherCityItem("TAKAYAMA", "Takayama", "Takayama", 36.1460, 137.2522),
        WeatherCityItem("KANAZAWA", "Kanazawa", "Kanazawa", 36.5613, 136.6562),
        WeatherCityItem("SAPPORO", "Sapporo (Hokkaido)", "Sapporo (Hokkaido)", 43.0618, 141.3545),
        WeatherCityItem("FUKUOKA", "Fukuoka (Hakata)", "Fukuoka (Hakata)", 33.5902, 130.4017),
        WeatherCityItem("NAGOYA", "Nagoya", "Nagoya", 35.1815, 136.9066),
        WeatherCityItem("YOKOHAMA", "Yokohama", "Yokohama", 35.4437, 139.6380),
        WeatherCityItem("KOBE", "Kobe", "Kobe", 34.6901, 135.1955),
        WeatherCityItem("OKINAWA", "Okinawa (Naha)", "Okinawa (Naha)", 26.2124, 127.6809),
        WeatherCityItem("SENDAI", "Sendai", "Sendai", 38.2682, 140.8694)
    )
}

object WeatherCityJsonCodec {
    fun exportCitiesJson(cities: List<WeatherCityItem>, deletedIds: Set<String> = emptySet()): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"cities\": [")
        cities.forEachIndexed { index, c ->
            val comma = if (index < cities.lastIndex) "," else ""
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escape(c.id)}\",")
            sb.appendLine("      \"nameEs\": \"${escape(c.nameEs)}\",")
            sb.appendLine("      \"nameEn\": \"${escape(c.nameEn)}\",")
            sb.appendLine("      \"latitude\": ${c.latitude},")
            sb.appendLine("      \"longitude\": ${c.longitude},")
            sb.appendLine("      \"isDefault\": ${c.isDefault}")
            sb.appendLine("    }$comma")
        }
        sb.appendLine("  ],")
        sb.append("  \"deletedIds\": [")
        sb.append(deletedIds.joinToString(",") { "\"${escape(it)}\"" })
        sb.appendLine("]")
        sb.append("}")
        return sb.toString()
    }

    fun importCitiesJson(json: String): List<WeatherCityItem> {
        val block = extractArrayBlock(json, "cities")
        if (block.isBlank()) return emptyList()
        val objects = splitJsonObjects(block)
        return objects.mapNotNull { obj ->
            val id = extractJsonString(obj, "id") ?: return@mapNotNull null
            val nameEs = extractJsonString(obj, "nameEs") ?: id
            val nameEn = extractJsonString(obj, "nameEn") ?: nameEs
            val lat = extractJsonDouble(obj, "latitude") ?: 35.0
            val lon = extractJsonDouble(obj, "longitude") ?: 135.0
            val isDef = extractJsonBoolean(obj, "isDefault") ?: false
            WeatherCityItem(id, nameEs, nameEn, lat, lon, isDef)
        }
    }

    fun importDeletedIds(json: String): Set<String> {
        val block = extractArrayBlock(json, "deletedIds")
        if (block.isBlank()) return emptySet()
        val regex = "\"([^\"]+)\"".toRegex()
        return regex.findAll(block).map { it.groupValues[1] }.toSet()
    }

    private fun escape(text: String): String =
        text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")

    private fun extractArrayBlock(json: String, arrayKey: String): String {
        val keyIndex = json.indexOf("\"$arrayKey\"")
        if (keyIndex == -1) return ""
        val startBracket = json.indexOf('[', keyIndex)
        if (startBracket == -1) return ""
        var depth = 0
        var inQuotes = false
        var isEscaped = false
        for (i in startBracket until json.length) {
            val char = json[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (char == '\\' && inQuotes) {
                isEscaped = true
                continue
            }
            if (char == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (char == '[') depth++
                else if (char == ']') {
                    depth--
                    if (depth == 0) {
                        return json.substring(startBracket + 1, i).trim()
                    }
                }
            }
        }
        return ""
    }

    private fun splitJsonObjects(arrayBlock: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var inQuotes = false
        var isEscaped = false
        var start = -1
        for (i in arrayBlock.indices) {
            val char = arrayBlock[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (char == '\\' && inQuotes) {
                isEscaped = true
                continue
            }
            if (char == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (char == '{') {
                    if (depth == 0) start = i
                    depth++
                } else if (char == '}') {
                    depth--
                    if (depth == 0 && start != -1) {
                        result.add(arrayBlock.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return result
    }

    private fun extractJsonString(obj: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        return regex.find(obj)?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\n", "\n")?.replace("\\\\", "\\")
    }

    private fun extractJsonDouble(obj: String, key: String): Double? {
        val regex = Regex("\"$key\"\\s*:\\s*([0-9.-]+)")
        return regex.find(obj)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractJsonBoolean(obj: String, key: String): Boolean? {
        val regex = Regex("\"$key\"\\s*:\\s*(true|false)")
        return regex.find(obj)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }
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
    private var appContext: Context? = null
    private const val CITIES_FILE_NAME = "saved_weather_cities.json"

    private val _citiesState = MutableStateFlow(WeatherCityCatalog.defaultCities())
    val citiesState: StateFlow<List<WeatherCityItem>> = _citiesState.asStateFlow()

    private val _forecastsState = MutableStateFlow(OpenMeteoWeatherUseCase.defaultSeedForecasts())
    val forecastsState: StateFlow<List<WeatherDayForecast>> = _forecastsState.asStateFlow()

    private val _deletedCityIds = mutableSetOf<String>()

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            loadFromDisk()
        }
    }

    fun saveToDisk() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, CITIES_FILE_NAME)
            file.writeText(WeatherCityJsonCodec.exportCitiesJson(_citiesState.value, _deletedCityIds))
        } catch (_: Exception) {}
    }

    fun loadFromDisk() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, CITIES_FILE_NAME)
            if (file.exists() && file.length() > 0) {
                val json = file.readText()
                val loaded = WeatherCityJsonCodec.importCitiesJson(json)
                if (loaded.isNotEmpty()) {
                    _citiesState.value = loaded
                }
                val deleted = WeatherCityJsonCodec.importDeletedIds(json)
                _deletedCityIds.clear()
                _deletedCityIds.addAll(deleted)
            }
        } catch (_: Exception) {}
    }

    fun syncCitiesFromPlan(
        stops: List<ItineraryStopItem>,
        segments: List<TransportSegmentItem>,
        forceResetDeleted: Boolean = false
    ) {
        if (forceResetDeleted) {
            _deletedCityIds.clear()
        }
        val extracted = PlanWeatherCityExtractor.extractCitiesFromPlan(stops, segments)
        val current = _citiesState.value.toMutableList()
        var changed = false

        for (city in extracted) {
            if (!forceResetDeleted && _deletedCityIds.contains(city.id)) {
                continue
            }
            val existingIdx = current.indexOfFirst { it.id == city.id }
            if (existingIdx < 0) {
                current.add(city)
                changed = true
                val existingForecasts = _forecastsState.value.filter { it.cityKey == city.id }
                if (existingForecasts.isEmpty()) {
                    val seed = OpenMeteoWeatherUseCase.generateSeedForCity(city.id)
                    _forecastsState.value = _forecastsState.value + seed
                }
            }
        }

        if (changed) {
            _citiesState.value = current
            saveToDisk()
        }
    }

    fun addCity(city: WeatherCityItem) {
        _deletedCityIds.remove(city.id)
        val current = _citiesState.value.toMutableList()
        val idx = current.indexOfFirst { it.id == city.id }
        if (idx >= 0) {
            current[idx] = city
        } else {
            current.add(city)
        }
        _citiesState.value = current

        // If no forecasts exist for this city, create initial seed forecasts
        val existing = _forecastsState.value.filter { it.cityKey == city.id }
        if (existing.isEmpty()) {
            val seed = OpenMeteoWeatherUseCase.generateSeedForCity(city.id)
            _forecastsState.value = _forecastsState.value + seed
        }
        saveToDisk()
    }

    fun updateCity(city: WeatherCityItem) {
        addCity(city)
    }

    fun deleteCity(cityId: String) {
        if (_citiesState.value.size <= 1) return // Keep at least one city
        _deletedCityIds.add(cityId)
        _citiesState.value = _citiesState.value.filterNot { it.id == cityId }
        _forecastsState.value = _forecastsState.value.filterNot { it.cityKey == cityId }
        saveToDisk()
    }

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
        ),
        evaluateForecastDay(
            cityKey = "NARA",
            dateIso = "2026-11-15",
            wmoCode = 1,
            tempMaxC = 17.5,
            tempMinC = 8.5,
            precipitationProbabilityMax = 15
        ),
        evaluateForecastDay(
            cityKey = "HIROSHIMA",
            dateIso = "2026-11-16",
            wmoCode = 0,
            tempMaxC = 18.0,
            tempMinC = 9.0,
            precipitationProbabilityMax = 10
        )
    )

    fun generateSeedForCity(cityKey: String): List<WeatherDayForecast> = listOf(
        evaluateForecastDay(
            cityKey = cityKey,
            dateIso = "2026-11-11",
            wmoCode = 1,
            tempMaxC = 18.5,
            tempMinC = 10.0,
            precipitationProbabilityMax = 15
        ),
        evaluateForecastDay(
            cityKey = cityKey,
            dateIso = "2026-11-12",
            wmoCode = 2,
            tempMaxC = 17.0,
            tempMinC = 9.5,
            precipitationProbabilityMax = 25
        ),
        evaluateForecastDay(
            cityKey = cityKey,
            dateIso = "2026-11-13",
            wmoCode = 0,
            tempMaxC = 19.0,
            tempMinC = 11.0,
            precipitationProbabilityMax = 10
        )
    )

    suspend fun fetchCityForecastOnline(city: JapanWeatherCity): Result<List<WeatherDayForecast>> =
        fetchCoordinatesForecastOnline(city.key, city.latitude, city.longitude)

    suspend fun fetchCoordinatesForecastOnline(
        cityKey: String,
        latitude: Double,
        longitude: Double
    ): Result<List<WeatherDayForecast>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(buildOpenMeteoUrl(latitude, longitude))
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val parsed = parseOpenMeteoDailyJson(cityKey, body)
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
