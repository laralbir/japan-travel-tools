package com.laralbir.japantraveltools.feature.weather

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.planner.TripPlannerUseCase
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportPlannerUseCase
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanWeatherCityExtractorTest {

    @Test
    fun `extractCitiesFromPlan detects cities from transport departures and arrivals`() {
        val segments = listOf(
            TransportSegmentItem(
                id = "seg-1",
                order = 1,
                type = TransportMode.FLIGHT,
                operatorName = "Iberia",
                lineOrNumber = "IB6801",
                departureCode = "MAD",
                departureName = "Madrid Barajas",
                departureZoneId = "Europe/Madrid",
                departureTerminalOrPlatform = "T4S",
                departureTimeIso = "2026-11-10T12:00",
                arrivalCode = "NRT",
                arrivalName = "Tokyo Narita",
                arrivalZoneId = "Asia/Tokyo",
                arrivalTerminalOrPlatform = "T1",
                arrivalTimeIso = "2026-11-11T10:00",
                seatOrCarInfo = ""
            ),
            TransportSegmentItem(
                id = "seg-2",
                order = 2,
                type = TransportMode.TRAIN_SHINKANSEN,
                operatorName = "JR Central",
                lineOrNumber = "Nozomi #21",
                departureCode = "TYO",
                departureName = "Tokyo Station (東京駅)",
                departureZoneId = "Asia/Tokyo",
                departureTerminalOrPlatform = "Track 16",
                departureTimeIso = "2026-11-12T09:00",
                arrivalCode = "KYO",
                arrivalName = "Kyoto Station (京都駅)",
                arrivalZoneId = "Asia/Tokyo",
                arrivalTerminalOrPlatform = "Track 13",
                arrivalTimeIso = "2026-11-12T11:15",
                seatOrCarInfo = ""
            )
        )

        val cities = PlanWeatherCityExtractor.extractCitiesFromPlan(
            stops = emptyList(),
            segments = segments
        )

        assertTrue(cities.any { it.id == "MADRID" })
        assertTrue(cities.any { it.id == "TOKYO" })
        assertTrue(cities.any { it.id == "KYOTO" })
    }

    @Test
    fun `extractCitiesFromPlan detects cities from itinerary stop titles placeNameJapanese and notes`() {
        val stops = listOf(
            ItineraryStopItem(
                id = "stop-1",
                dayNumber = 1,
                dateIso = "2026-11-11",
                startTime = "10:00",
                endTime = "12:00",
                title = "Paseo por Asakusa y Senso-ji",
                category = StopCategory.SIGHTSEEING,
                placeNameJapanese = "東京",
                latitude = 35.7148,
                longitude = 139.7967,
                notes = "Tokio histórico"
            ),
            ItineraryStopItem(
                id = "stop-2",
                dayNumber = 4,
                dateIso = "2026-11-14",
                startTime = "09:00",
                endTime = "14:00",
                title = "Excursión a Hakone y Monte Fuji",
                category = StopCategory.ACTIVITY,
                placeNameJapanese = "箱根",
                latitude = 35.2324,
                longitude = 139.1069,
                notes = "Vistas del Monte Fuji"
            ),
            ItineraryStopItem(
                id = "stop-3",
                dayNumber = 7,
                dateIso = "2026-11-17",
                startTime = "11:00",
                endTime = "15:00",
                title = "Parque de Nara y Todai-ji",
                category = StopCategory.SIGHTSEEING,
                placeNameJapanese = "奈良",
                latitude = 34.6851,
                longitude = 135.8048,
                notes = "Ciervos sagrados"
            )
        )

        val cities = PlanWeatherCityExtractor.extractCitiesFromPlan(
            stops = stops,
            segments = emptyList()
        )

        assertTrue(cities.any { it.id == "TOKYO" })
        assertTrue(cities.any { it.id == "HAKONE_FUJI" })
        assertTrue(cities.any { it.id == "NARA" })
    }

    @Test
    fun `extractCitiesFromPlan works with default sample plan and extracts comprehensive destination list`() {
        val defaultStops = TripPlannerUseCase.defaultSampleStops()
        val defaultSegments = TransportPlannerUseCase.defaultSampleSegments()

        val extracted = PlanWeatherCityExtractor.extractCitiesFromPlan(defaultStops, defaultSegments)

        val cityIds = extracted.map { it.id }.toSet()
        assertTrue(cityIds.contains("TOKYO"))
        assertTrue(cityIds.contains("KYOTO"))
        assertTrue(cityIds.contains("MADRID"))
        assertTrue(cityIds.contains("DOHA"))
    }

    @Test
    fun `syncCitiesFromPlan integrates extracted cities and respects manual deletions`() {
        // Reset state
        SharedWeatherForecastStore.syncCitiesFromPlan(
            stops = TripPlannerUseCase.defaultSampleStops(),
            segments = TransportPlannerUseCase.defaultSampleSegments(),
            forceResetDeleted = true
        )

        val initialCities = SharedWeatherForecastStore.citiesState.value
        assertTrue(initialCities.any { it.id == "NARA" })

        // User deletes NARA
        SharedWeatherForecastStore.deleteCity("NARA")
        assertTrue(SharedWeatherForecastStore.citiesState.value.none { it.id == "NARA" })

        // Subsequent sync must NOT resurrect deleted NARA
        SharedWeatherForecastStore.syncCitiesFromPlan(
            stops = TripPlannerUseCase.defaultSampleStops(),
            segments = TransportPlannerUseCase.defaultSampleSegments(),
            forceResetDeleted = false
        )
        assertTrue(SharedWeatherForecastStore.citiesState.value.none { it.id == "NARA" })

        // User manually re-adds NARA
        val naraPreset = WeatherCityCatalog.destinationPresets().first { it.id == "NARA" }
        SharedWeatherForecastStore.addCity(naraPreset)
        assertTrue(SharedWeatherForecastStore.citiesState.value.any { it.id == "NARA" })
    }
}
