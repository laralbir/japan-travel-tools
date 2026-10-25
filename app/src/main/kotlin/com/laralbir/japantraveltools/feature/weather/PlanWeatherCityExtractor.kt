package com.laralbir.japantraveltools.feature.weather

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import java.util.Locale

object PlanWeatherCityExtractor {

    data class CityDefinition(
        val cityItem: WeatherCityItem,
        val aliases: List<String>
    )

    val KNOWN_CITIES: List<CityDefinition> = listOf(
        CityDefinition(
            WeatherCityItem("TOKYO", "Tokio", "Tokyo", 35.6762, 139.6503, isDefault = true),
            listOf("tokyo", "tokio", "東京", "tyo", "hnd", "nrt", "haneda", "narita", "shinjuku", "shibuya", "asakusa", "ginza", "akihabara", "ueno", "roppongi", "odaiba", "tsukiji", "harajuku")
        ),
        CityDefinition(
            WeatherCityItem("KYOTO", "Kioto", "Kyoto", 35.0116, 135.7681, isDefault = true),
            listOf("kyoto", "kioto", "京都", "kyo", "k11", "gion", "arashiyama", "fushimi", "kiyomizu", "kinkaku", "higashiyama", "pontocho")
        ),
        CityDefinition(
            WeatherCityItem("OSAKA", "Osaka", "Osaka", 34.6937, 135.5023, isDefault = true),
            listOf("osaka", "大阪", "osa", "kix", "itm", "dotonbori", "umeda", "namba", "shin-osaka", "shinosaka", "kuromon")
        ),
        CityDefinition(
            WeatherCityItem("HAKONE_FUJI", "Hakone / Monte Fuji", "Hakone / Mt. Fuji", 35.2324, 139.1069, isDefault = true),
            listOf("hakone", "fuji", "monte fuji", "mt. fuji", "kawaguchiko", "odawara", "箱根", "富士", "fujikawaguchiko", "ashi")
        ),
        CityDefinition(
            WeatherCityItem("NARA", "Nara", "Nara", 34.6851, 135.8048, isDefault = false),
            listOf("nara", "奈良", "todai-ji", "todaiji", "kasuga")
        ),
        CityDefinition(
            WeatherCityItem("HIROSHIMA", "Hiroshima", "Hiroshima", 34.3853, 132.4553, isDefault = false),
            listOf("hiroshima", "広島", "paz", "peace memorial")
        ),
        CityDefinition(
            WeatherCityItem("MIYAJIMA", "Miyajima / Itsukushima", "Miyajima / Itsukushima", 34.2964, 132.3197, isDefault = false),
            listOf("miyajima", "itsukushima", "宮島", "厳島", "torii flotante")
        ),
        CityDefinition(
            WeatherCityItem("KAMAKURA", "Kamakura", "Kamakura", 35.3192, 139.5467, isDefault = false),
            listOf("kamakura", "鎌倉", "daibutsu", "hase-dera", "hasedera")
        ),
        CityDefinition(
            WeatherCityItem("NIKKO", "Nikko", "Nikko", 36.7581, 139.5986, isDefault = false),
            listOf("nikko", "nikkō", "日光", "toshogu", "chuzenji")
        ),
        CityDefinition(
            WeatherCityItem("TAKAYAMA", "Takayama", "Takayama", 36.1460, 137.2522, isDefault = false),
            listOf("takayama", "shirakawa", "高山", "shirakawa-go", "shirakawago", "hida")
        ),
        CityDefinition(
            WeatherCityItem("KANAZAWA", "Kanazawa", "Kanazawa", 36.5613, 136.6562, isDefault = false),
            listOf("kanazawa", "金沢", "kenrokuen", "higashi chaya")
        ),
        CityDefinition(
            WeatherCityItem("SAPPORO", "Sapporo (Hokkaido)", "Sapporo (Hokkaido)", 43.0618, 141.3545, isDefault = false),
            listOf("sapporo", "hokkaido", "札幌", "cts", "otaru")
        ),
        CityDefinition(
            WeatherCityItem("FUKUOKA", "Fukuoka (Hakata)", "Fukuoka (Hakata)", 33.5902, 130.4017, isDefault = false),
            listOf("fukuoka", "hakata", "福岡", "博多", "fuk")
        ),
        CityDefinition(
            WeatherCityItem("NAGOYA", "Nagoya", "Nagoya", 35.1815, 136.9066, isDefault = false),
            listOf("nagoya", "名古屋", "ngo")
        ),
        CityDefinition(
            WeatherCityItem("YOKOHAMA", "Yokohama", "Yokohama", 35.4437, 139.6380, isDefault = false),
            listOf("yokohama", "横浜", "minato mirai")
        ),
        CityDefinition(
            WeatherCityItem("KOBE", "Kobe", "Kobe", 34.6901, 135.1955, isDefault = false),
            listOf("kobe", "神戸", "ukb")
        ),
        CityDefinition(
            WeatherCityItem("OKINAWA", "Okinawa (Naha)", "Okinawa (Naha)", 26.2124, 127.6809, isDefault = false),
            listOf("okinawa", "naha", "沖縄", "那覇", "oka")
        ),
        CityDefinition(
            WeatherCityItem("SENDAI", "Sendai", "Sendai", 38.2682, 140.8694, isDefault = false),
            listOf("sendai", "仙台", "sdj")
        ),
        CityDefinition(
            WeatherCityItem("MADRID", "Madrid", "Madrid", 40.4168, -3.7038, isDefault = false),
            listOf("madrid", "barajas", "mad")
        ),
        CityDefinition(
            WeatherCityItem("DOHA", "Doha", "Doha", 25.2854, 51.5310, isDefault = false),
            listOf("doha", "hamad", "doh")
        ),
        CityDefinition(
            WeatherCityItem("BARCELONA", "Barcelona", "Barcelona", 41.3851, 2.1734, isDefault = false),
            listOf("barcelona", "el prat", "bcn")
        ),
        CityDefinition(
            WeatherCityItem("NAGANO", "Nagano", "Nagano", 36.6513, 138.1810, isDefault = false),
            listOf("nagano", "長野", "jigokudani")
        ),
        CityDefinition(
            WeatherCityItem("MATSUMOTO", "Matsumoto", "Matsumoto", 36.2380, 137.9720, isDefault = false),
            listOf("matsumoto", "松本")
        ),
        CityDefinition(
            WeatherCityItem("HIMEJI", "Himeji", "Himeji", 34.8394, 134.6939, isDefault = false),
            listOf("himeji", "姫路")
        )
    )

    fun matchCity(text: String, code: String = ""): WeatherCityItem? {
        val normText = text.lowercase(Locale.ROOT)
        val normCode = code.lowercase(Locale.ROOT).trim()
        val textWords = normText.split("[^\\p{L}\\p{Nd}]+".toRegex()).filter { it.isNotBlank() }

        // 1. First priority: Check exact airport / station code if provided
        if (normCode.isNotBlank()) {
            for (def in KNOWN_CITIES) {
                if (def.aliases.contains(normCode)) {
                    return def.cityItem
                }
            }
        }

        // 2. Second priority: Check full name / city aliases
        for (def in KNOWN_CITIES) {
            for (alias in def.aliases) {
                if (alias.length <= 3) {
                    // For short 3-letter codes or abbreviations (e.g. "mad", "doh", "tyo"), match as an isolated word only
                    if (textWords.contains(alias)) {
                        return def.cityItem
                    }
                } else {
                    // For words >= 4 chars (e.g. "madrid", "tokyo", "doha", "hamad"), substring match
                    if (normText.contains(alias)) {
                        return def.cityItem
                    }
                }
            }
        }
        return null
    }

    fun extractCitiesFromPlan(
        stops: List<ItineraryStopItem>,
        segments: List<TransportSegmentItem>
    ): List<WeatherCityItem> {
        val resultMap = LinkedHashMap<String, WeatherCityItem>()

        // 1. Process transport segments (origin and destination)
        for (seg in segments) {
            val depCity = matchCity(seg.departureName, seg.departureCode)
            if (depCity != null) {
                resultMap[depCity.id] = depCity
            }
            val arrCity = matchCity(seg.arrivalName, seg.arrivalCode)
            if (arrCity != null) {
                resultMap[arrCity.id] = arrCity
            }
        }

        // 2. Process itinerary stops
        for (stop in stops) {
            val combinedText = "${stop.title} ${stop.placeNameJapanese} ${stop.notes}"
            val matched = matchCity(combinedText)
            if (matched != null) {
                resultMap[matched.id] = matched
            } else if (stop.latitude != 0.0 && stop.longitude != 0.0 && stop.title.isNotBlank()) {
                val cleanTitle = stop.title.take(30).trim()
                val customId = "CITY_" + cleanTitle.uppercase(Locale.ROOT).replace("[^A-Z0-9]".toRegex(), "_")
                if (!resultMap.containsKey(customId)) {
                    resultMap[customId] = WeatherCityItem(
                        id = customId,
                        nameEs = cleanTitle,
                        nameEn = cleanTitle,
                        latitude = stop.latitude,
                        longitude = stop.longitude,
                        isDefault = false
                    )
                }
            }
        }

        return if (resultMap.isEmpty()) {
            WeatherCityCatalog.defaultCities()
        } else {
            resultMap.values.toList()
        }
    }
}
