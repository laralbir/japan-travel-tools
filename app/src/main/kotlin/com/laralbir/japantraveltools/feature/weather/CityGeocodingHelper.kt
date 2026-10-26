package com.laralbir.japantraveltools.feature.weather

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

data class GeocodedCity(
    val nameEs: String,
    val nameEn: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val source: String = "Catalog"
)

object CityGeocodingHelper {

    private data class CatalogEntry(
        val nameEs: String,
        val nameEn: String,
        val country: String,
        val latitude: Double,
        val longitude: Double,
        val aliases: List<String> = emptyList()
    ) {
        fun toGeocodedCity(source: String = "Catalog") = GeocodedCity(
            nameEs = nameEs,
            nameEn = nameEn,
            country = country,
            latitude = latitude,
            longitude = longitude,
            source = source
        )
    }

    private val CATALOG: List<CatalogEntry> = listOf(
        // Japan - Major Destinations & Prefectural Capitals
        CatalogEntry("Tokio", "Tokyo", "Japón", 35.6762, 139.6503, listOf("tokio", "tokyo", "東京", "shinjuku", "shibuya", "asakusa", "ginza", "akihabara", "ueno")),
        CatalogEntry("Kioto", "Kyoto", "Japón", 35.0116, 135.7681, listOf("kioto", "kyoto", "京都", "gion", "arashiyama", "fushimi")),
        CatalogEntry("Osaka", "Osaka", "Japón", 34.6937, 135.5023, listOf("osaka", "大阪", "dotonbori", "umeda", "namba")),
        CatalogEntry("Hakone / Monte Fuji", "Hakone / Mt. Fuji", "Japón", 35.2324, 139.1069, listOf("hakone", "fuji", "monte fuji", "mt fuji", "kawaguchiko", "odawara", "箱根", "富士")),
        CatalogEntry("Nara", "Nara", "Japón", 34.6851, 135.8048, listOf("nara", "奈良", "todaiji")),
        CatalogEntry("Hiroshima", "Hiroshima", "Japón", 34.3853, 132.4553, listOf("hiroshima", "広島")),
        CatalogEntry("Miyajima / Itsukushima", "Miyajima / Itsukushima", "Japón", 34.2964, 132.3197, listOf("miyajima", "itsukushima", "宮島", "厳島")),
        CatalogEntry("Kamakura", "Kamakura", "Japón", 35.3192, 139.5467, listOf("kamakura", "鎌倉", "daibutsu")),
        CatalogEntry("Nikko", "Nikko", "Japón", 36.7581, 139.5986, listOf("nikko", "nikko", "日光", "toshogu")),
        CatalogEntry("Takayama", "Takayama", "Japón", 36.1460, 137.2522, listOf("takayama", "高山", "hida")),
        CatalogEntry("Shirakawa-go", "Shirakawa-go", "Japón", 36.2562, 136.9061, listOf("shirakawa", "shirakawago", "shirakawa-go", "白川郷")),
        CatalogEntry("Kanazawa", "Kanazawa", "Japón", 36.5613, 136.6562, listOf("kanazawa", "金沢", "kenrokuen")),
        CatalogEntry("Sapporo (Hokkaido)", "Sapporo (Hokkaido)", "Japón", 43.0618, 141.3545, listOf("sapporo", "札幌", "hokkaido")),
        CatalogEntry("Otaru", "Otaru", "Japón", 43.1907, 140.9947, listOf("otaru", "小樽")),
        CatalogEntry("Hakodate", "Hakodate", "Japón", 41.7687, 140.7288, listOf("hakodate", "函館")),
        CatalogEntry("Asahikawa", "Asahikawa", "Japón", 43.7706, 142.3649, listOf("asahikawa", "旭川")),
        CatalogEntry("Fukuoka (Hakata)", "Fukuoka (Hakata)", "Japón", 33.5902, 130.4017, listOf("fukuoka", "hakata", "福岡", "博多")),
        CatalogEntry("Nagoya", "Nagoya", "Japón", 35.1815, 136.9066, listOf("nagoya", "名古屋")),
        CatalogEntry("Yokohama", "Yokohama", "Japón", 35.4437, 139.6380, listOf("yokohama", "横浜")),
        CatalogEntry("Kobe", "Kobe", "Japón", 34.6901, 135.1955, listOf("kobe", "神戸")),
        CatalogEntry("Himeji", "Himeji", "Japón", 34.8394, 134.6939, listOf("himeji", "姫路")),
        CatalogEntry("Okayama", "Okayama", "Japón", 34.6551, 133.9195, listOf("okayama", "岡山")),
        CatalogEntry("Kurashiki", "Kurashiki", "Japón", 34.5850, 133.7722, listOf("kurashiki", "倉敷")),
        CatalogEntry("Naoshima", "Naoshima", "Japón", 34.4592, 133.9961, listOf("naoshima", "直島")),
        CatalogEntry("Takamatsu", "Takamatsu", "Japón", 34.3428, 134.0466, listOf("takamatsu", "高松")),
        CatalogEntry("Matsuyama", "Matsuyama", "Japón", 33.8392, 132.7656, listOf("matsuyama", "松山")),
        CatalogEntry("Kochi", "Kochi", "Japón", 33.5597, 133.5311, listOf("kochi", "高知")),
        CatalogEntry("Tokushima", "Tokushima", "Japón", 34.0704, 134.5548, listOf("tokushima", "徳島")),
        CatalogEntry("Nagano", "Nagano", "Japón", 36.6513, 138.1810, listOf("nagano", "長野")),
        CatalogEntry("Matsumoto", "Matsumoto", "Japón", 36.2380, 137.9720, listOf("matsumoto", "松本")),
        CatalogEntry("Karuizawa", "Karuizawa", "Japón", 36.3486, 138.6353, listOf("karuizawa", "軽井沢")),
        CatalogEntry("Niigata", "Niigata", "Japón", 37.9162, 139.0364, listOf("niigata", "新潟")),
        CatalogEntry("Toyama", "Toyama", "Japón", 36.6959, 137.2137, listOf("toyama", "富山")),
        CatalogEntry("Shizuoka", "Shizuoka", "Japón", 34.9756, 138.3828, listOf("shizuoka", "静岡")),
        CatalogEntry("Hamamatsu", "Hamamatsu", "Japón", 34.7108, 137.7261, listOf("hamamatsu", "浜松")),
        CatalogEntry("Sendai", "Sendai", "Japón", 38.2682, 140.8694, listOf("sendai", "仙台")),
        CatalogEntry("Morioka", "Morioka", "Japón", 39.7036, 141.1527, listOf("morioka", "盛岡")),
        CatalogEntry("Aomori", "Aomori", "Japón", 40.8244, 140.7400, listOf("aomori", "青森")),
        CatalogEntry("Akita", "Akita", "Japón", 39.7200, 140.1026, listOf("akita", "秋田")),
        CatalogEntry("Yamagata", "Yamagata", "Japón", 38.2554, 140.3396, listOf("yamagata", "山形")),
        CatalogEntry("Fukushima", "Fukushima", "Japón", 37.7608, 140.4748, listOf("fukushima", "福島")),
        CatalogEntry("Matsue", "Matsue", "Japón", 35.4723, 133.0505, listOf("matsue", "松江")),
        CatalogEntry("Tottori", "Tottori", "Japón", 35.5011, 134.2351, listOf("tottori", "鳥取")),
        CatalogEntry("Okinawa (Naha)", "Okinawa (Naha)", "Japón", 26.2124, 127.6809, listOf("okinawa", "naha", "沖縄", "那覇")),
        CatalogEntry("Ishigaki", "Ishigaki", "Japón", 24.3448, 124.1572, listOf("ishigaki", "石垣")),
        CatalogEntry("Miyakojima", "Miyakojima", "Japón", 24.8055, 125.2811, listOf("miyakojima", "宮古島")),
        CatalogEntry("Nagasaki", "Nagasaki", "Japón", 32.7503, 129.8777, listOf("nagasaki", "長崎")),
        CatalogEntry("Kumamoto", "Kumamoto", "Japón", 32.8031, 130.7079, listOf("kumamoto", "熊本")),
        CatalogEntry("Beppu / Onsen", "Beppu / Onsen", "Japón", 33.2846, 131.4912, listOf("beppu", "別府")),
        CatalogEntry("Yufuin", "Yufuin", "Japón", 33.2642, 131.3556, listOf("yufuin", "由布院")),
        CatalogEntry("Kagoshima", "Kagoshima", "Japón", 31.5966, 130.5571, listOf("kagoshima", "鹿児島")),
        CatalogEntry("Miyazaki", "Miyazaki", "Japón", 31.9077, 131.4202, listOf("miyazaki", "宮崎")),

        // International Gateway Hubs & Spanish Origins
        CatalogEntry("Madrid", "Madrid", "España", 40.4168, -3.7038, listOf("madrid", "barajas", "mad")),
        CatalogEntry("Barcelona", "Barcelona", "España", 41.3851, 2.1734, listOf("barcelona", "el prat", "bcn")),
        CatalogEntry("Valencia", "Valencia", "España", 39.4699, -0.3763, listOf("valencia", "vlc")),
        CatalogEntry("Sevilla", "Seville", "España", 37.3891, -5.9845, listOf("sevilla", "seville", "svq")),
        CatalogEntry("Málaga", "Malaga", "España", 36.7213, -4.4214, listOf("malaga", "málaga", "agp")),
        CatalogEntry("Bilbao", "Bilbao", "España", 43.2630, -2.9350, listOf("bilbao", "bio")),
        CatalogEntry("Doha", "Doha", "Catar", 25.2854, 51.5310, listOf("doha", "hamad", "doh")),
        CatalogEntry("Dubái", "Dubai", "Emiratos Árabes", 25.2048, 55.2708, listOf("dubai", "dubái", "dxb")),
        CatalogEntry("Abu Dabi", "Abu Dhabi", "Emiratos Árabes", 24.4539, 54.3773, listOf("abu dhabi", "abu dabi", "auh")),
        CatalogEntry("Londres", "London", "Reino Unido", 51.5074, -0.1278, listOf("londres", "london", "lhr", "lgw")),
        CatalogEntry("París", "Paris", "Francia", 48.8566, 2.3522, listOf("paris", "parís", "cdg")),
        CatalogEntry("Roma", "Rome", "Italia", 41.9028, 12.4964, listOf("roma", "rome", "fco")),
        CatalogEntry("Ámsterdam", "Amsterdam", "Países Bajos", 52.3676, 4.9041, listOf("amsterdam", "ámsterdam", "ams")),
        CatalogEntry("Fráncfort", "Frankfurt", "Alemania", 50.1109, 8.6821, listOf("frankfurt", "fráncfort", "fra")),
        CatalogEntry("Múnich", "Munich", "Alemania", 48.1351, 11.5820, listOf("munich", "múnich", "muc")),
        CatalogEntry("Zúrich", "Zurich", "Suiza", 47.3769, 8.5417, listOf("zurich", "zúrich", "zrh")),
        CatalogEntry("Nueva York", "New York", "Estados Unidos", 40.7128, -74.0060, listOf("new york", "nueva york", "jfk", "ewr")),
        CatalogEntry("Los Ángeles", "Los Angeles", "Estados Unidos", 34.0522, -118.2437, listOf("los angeles", "los ángeles", "lax")),
        CatalogEntry("San Francisco", "San Francisco", "Estados Unidos", 37.7749, -122.4194, listOf("san francisco", "sfo")),
        CatalogEntry("Seúl", "Seoul", "Corea del Sur", 37.5665, 126.9780, listOf("seoul", "seúl", "icn", "gmp")),
        CatalogEntry("Busan", "Busan", "Corea del Sur", 35.1796, 129.0756, listOf("busan", "pus")),
        CatalogEntry("Taipéi", "Taipei", "Taiwán", 25.0330, 121.5654, listOf("taipei", "taipéi", "tpe")),
        CatalogEntry("Bangkok", "Bangkok", "Tailandia", 13.7563, 100.5018, listOf("bangkok", "bkk")),
        CatalogEntry("Singapur", "Singapore", "Singapur", 1.3521, 103.8198, listOf("singapore", "singapur", "sin")),
        CatalogEntry("Hong Kong", "Hong Kong", "Hong Kong", 22.3193, 114.1694, listOf("hong kong", "hkg")),
        CatalogEntry("Sídney", "Sydney", "Australia", -33.8688, 151.2093, listOf("sydney", "sídney", "syd"))
    )

    private fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    /**
     * Devuelve predicciones de ciudades coincidentes ordenadas por relevancia.
     */
    fun searchPredictions(query: String, maxResults: Int = 8): List<GeocodedCity> {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return emptyList()

        val normQuery = normalize(cleanQuery)

        val exactMatches = mutableListOf<CatalogEntry>()
        val prefixMatches = mutableListOf<CatalogEntry>()
        val substringMatches = mutableListOf<CatalogEntry>()

        for (entry in CATALOG) {
            val normEs = normalize(entry.nameEs)
            val normEn = normalize(entry.nameEn)
            val normAliases = entry.aliases.map { normalize(it) }

            when {
                normEs == normQuery || normEn == normQuery || normAliases.any { it == normQuery } -> {
                    exactMatches.add(entry)
                }
                normEs.startsWith(normQuery) || normEn.startsWith(normQuery) || normAliases.any { it.startsWith(normQuery) } -> {
                    prefixMatches.add(entry)
                }
                normEs.contains(normQuery) || normEn.contains(normQuery) || normAliases.any { it.contains(normQuery) } -> {
                    substringMatches.add(entry)
                }
            }
        }

        val combined = (exactMatches + prefixMatches + substringMatches)
            .distinctBy { "${it.latitude},${it.longitude}" }
            .take(maxResults)

        return combined.map { it.toGeocodedCity() }
    }

    /**
     * Busca una ciudad exacta o normalizada en el catálogo offline.
     */
    fun getFromCatalog(name: String): GeocodedCity? {
        val clean = name.trim()
        if (clean.isBlank()) return null
        val norm = normalize(clean)

        // Exact match check
        for (entry in CATALOG) {
            if (normalize(entry.nameEs) == norm || normalize(entry.nameEn) == norm) {
                return entry.toGeocodedCity()
            }
            if (entry.aliases.any { normalize(it) == norm }) {
                return entry.toGeocodedCity()
            }
        }

        // Substring fallback in catalog if clear
        for (entry in CATALOG) {
            if (norm.contains(normalize(entry.nameEs)) || norm.contains(normalize(entry.nameEn))) {
                return entry.toGeocodedCity()
            }
        }

        return null
    }

    /**
     * Geocodifica una ciudad automáticamente:
     * 1. Consulta el catálogo offline de alta velocidad (sin red).
     * 2. Si hay Context y Geocoder (Google Maps Android), consulta Google Maps Geocoder.
     * 3. Fallback a la API de geocodificación de Open-Meteo.
     */
    suspend fun geocodeCity(context: Context?, query: String): GeocodedCity? {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return null

        // 1. Catálogo local offline
        val local = getFromCatalog(cleanQuery)
        if (local != null) {
            return local
        }

        // 2. Android Geocoder (Google Maps Provider en Android)
        if (context != null) {
            val fromGeocoder = geocodeWithAndroidGeocoder(context, cleanQuery)
            if (fromGeocoder != null) {
                return fromGeocoder
            }
        }

        // 3. Fallback a Open-Meteo Geocoding REST API
        val fromOpenMeteo = geocodeWithOpenMeteoApi(cleanQuery)
        if (fromOpenMeteo != null) {
            return fromOpenMeteo
        }

        return null
    }

    /**
     * Búsqueda predictiva online con Open-Meteo para ciudades no presentes en el catálogo offline.
     */
    suspend fun searchOnlinePredictions(query: String, maxResults: Int = 5): List<GeocodedCity> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.length < 2) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=$maxResults&language=es&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "JapanTravelTools/1.0.2")
            }
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                return@withContext parseOpenMeteoGeocodingResponse(response)
            }
        } catch (_: Exception) {
            // Silently fallback to offline predictions
        }
        emptyList()
    }

    private suspend fun geocodeWithAndroidGeocoder(context: Context, query: String): GeocodedCity? = withContext(Dispatchers.IO) {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results: List<Address>? = geocoder.getFromLocationName(query, 1)
                val first = results?.firstOrNull()
                if (first != null && first.hasLatitude() && first.hasLongitude()) {
                    val cityName = first.locality ?: first.subAdminArea ?: first.featureName ?: query
                    val country = first.countryName ?: ""
                    return@withContext GeocodedCity(
                        nameEs = cityName,
                        nameEn = cityName,
                        country = country,
                        latitude = first.latitude,
                        longitude = first.longitude,
                        source = "Google Maps"
                    )
                }
            }
        } catch (_: Exception) {
            // Geocoder service may throw IOException on network issues or no backend
        }
        null
    }

    private suspend fun geocodeWithOpenMeteoApi(query: String): GeocodedCity? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=es&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "JapanTravelTools/1.0.2")
            }
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = parseOpenMeteoGeocodingResponse(response)
                return@withContext parsed.firstOrNull()
            }
        } catch (_: Exception) {
            // Network failure / offline
        }
        null
    }

    /**
     * Parsea la respuesta JSON de Open-Meteo Geocoding API de forma manual y robusta.
     */
    fun parseOpenMeteoGeocodingResponse(json: String): List<GeocodedCity> {
        if (json.isBlank() || !json.contains("\"results\"")) return emptyList()

        val resultsList = mutableListOf<GeocodedCity>()
        val resultsBlock = extractArrayBlock(json, "results")
        if (resultsBlock.isBlank()) return emptyList()

        val objects = splitJsonObjects(resultsBlock)
        for (obj in objects) {
            val name = extractJsonString(obj, "name") ?: continue
            val latitude = extractJsonDouble(obj, "latitude") ?: continue
            val longitude = extractJsonDouble(obj, "longitude") ?: continue
            val country = extractJsonString(obj, "country") ?: ""
            val admin1 = extractJsonString(obj, "admin1") ?: ""

            val displayName = if (admin1.isNotBlank() && admin1 != name) "$name ($admin1)" else name

            resultsList.add(
                GeocodedCity(
                    nameEs = displayName,
                    nameEn = name,
                    country = country,
                    latitude = latitude,
                    longitude = longitude,
                    source = "Open-Meteo"
                )
            )
        }
        return resultsList
    }

    private fun extractArrayBlock(json: String, arrayName: String): String {
        val marker = "\"$arrayName\":"
        val startIdx = json.indexOf(marker)
        if (startIdx == -1) return ""
        val openBracket = json.indexOf('[', startIdx + marker.length)
        if (openBracket == -1) return ""
        var depth = 0
        var inQuotes = false
        var isEscaped = false
        for (i in openBracket until json.length) {
            val c = json[i]
            if (c == '\\' && inQuotes) {
                isEscaped = !isEscaped
                continue
            }
            if (c == '"' && !isEscaped) {
                inQuotes = !inQuotes
            }
            isEscaped = false
            if (!inQuotes) {
                if (c == '[') depth++
                if (c == ']') {
                    depth--
                    if (depth == 0) {
                        return json.substring(openBracket + 1, i)
                    }
                }
            }
        }
        return ""
    }

    private fun splitJsonObjects(arrayContent: String): List<String> {
        val list = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inQuotes = false
        var isEscaped = false
        for (i in arrayContent.indices) {
            val c = arrayContent[i]
            if (c == '\\' && inQuotes) {
                isEscaped = !isEscaped
                continue
            }
            if (c == '"' && !isEscaped) {
                inQuotes = !inQuotes
            }
            isEscaped = false
            if (!inQuotes) {
                if (c == '{') {
                    if (depth == 0) start = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && start != -1) {
                        list.add(arrayContent.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return list
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractJsonDouble(json: String, key: String): Double? {
        val pattern = "\"$key\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }
}

