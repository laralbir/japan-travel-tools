package com.laralbir.japantraveltools.feature.weather

import com.laralbir.japantraveltools.core.portability.AiPlanBuilderEngine
import com.laralbir.japantraveltools.core.portability.PortableTravelPlanBundle
import com.laralbir.japantraveltools.feature.planner.TripPlannerUseCase
import com.laralbir.japantraveltools.feature.transport.TransportPlannerUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoWeatherUseCaseTest {

    @Test
    fun `buildOpenMeteoUrl constructs valid Asia Tokyo forecast endpoint without API key`() {
        val url = OpenMeteoWeatherUseCase.buildOpenMeteoUrl(latitude = 35.6762, longitude = 139.6503)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?"))
        assertTrue(url.contains("latitude=35.6762"))
        assertTrue(url.contains("longitude=139.6503"))
        assertTrue(url.contains("daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"))
        assertTrue(url.contains("timezone=Asia%2FTokyo"))
    }

    @Test
    fun `classifyWmoCode and suitability evaluate sunny vs rainy days accurately`() {
        val sunny = OpenMeteoWeatherUseCase.evaluateForecastDay(
            cityKey = "TOKYO",
            dateIso = "2026-11-11",
            wmoCode = 1,
            tempMaxC = 19.4,
            tempMinC = 11.2,
            precipitationProbabilityMax = 10
        )
        assertEquals(WeatherConditionKind.CLEAR_SUNNY, sunny.condition)
        assertEquals(PlanningSuitability.OUTDOOR_OPTIMAL, sunny.suitability)
        assertTrue(sunny.fujiVisibilityPercent >= 80)

        val rainy = OpenMeteoWeatherUseCase.evaluateForecastDay(
            cityKey = "KYOTO",
            dateIso = "2026-11-12",
            wmoCode = 63,
            tempMaxC = 15.0,
            tempMinC = 9.5,
            precipitationProbabilityMax = 85
        )
        assertEquals(WeatherConditionKind.RAIN, rainy.condition)
        assertEquals(PlanningSuitability.INDOOR_RECOMMENDED, rainy.suitability)
        assertTrue(rainy.userPlanningAdviceEs.contains("paraguas", ignoreCase = true))
        assertTrue(rainy.aiPromptContextSummary.contains("INDOOR_RECOMMENDED"))
    }

    @Test
    fun `parseOpenMeteoDailyJson extracts daily arrays into WeatherDayForecast list`() {
        val sampleOpenMeteoJson = """
            {
              "latitude": 35.0,
              "longitude": 135.75,
              "timezone": "Asia/Tokyo",
              "daily": {
                "time": ["2026-11-11", "2026-11-12", "2026-11-13"],
                "weather_code": [0, 61, 2],
                "temperature_2m_max": [20.1, 14.8, 18.5],
                "temperature_2m_min": [11.5, 9.0, 10.2],
                "precipitation_probability_max": [5, 75, 20]
              }
            }
        """.trimIndent()

        val parsed = OpenMeteoWeatherUseCase.parseOpenMeteoDailyJson("KYOTO", sampleOpenMeteoJson)
        assertEquals(3, parsed.size)
        assertEquals("2026-11-11", parsed[0].dateIso)
        assertEquals(WeatherConditionKind.CLEAR_SUNNY, parsed[0].condition)
        assertEquals(PlanningSuitability.OUTDOOR_OPTIMAL, parsed[0].suitability)

        assertEquals("2026-11-12", parsed[1].dateIso)
        assertEquals(WeatherConditionKind.RAIN, parsed[1].condition)
        assertEquals(PlanningSuitability.INDOOR_RECOMMENDED, parsed[1].suitability)
        assertEquals(75, parsed[1].precipitationProbabilityMax)
    }

    @Test
    fun `AiPlanBuilderEngine optimizePlanWithWeather adapts itinerary stops using OpenMeteo forecast`() {
        val initialPlan = PortableTravelPlanBundle(
            schemaVersion = 1,
            tripTitle = "Japón Otoño 2026",
            itineraryStops = TripPlannerUseCase.defaultSampleStops(),
            transportSegments = TransportPlannerUseCase.defaultSampleSegments()
        )
        val forecasts = OpenMeteoWeatherUseCase.defaultSeedForecasts()

        val weatherOptimizedPlan = AiPlanBuilderEngine.optimizePlanWithWeather(
            currentPlan = initialPlan,
            forecasts = forecasts
        )

        assertTrue(weatherOptimizedPlan.aiOptimizationSummary.contains("Open-Meteo", ignoreCase = true))
        val day2Stops = weatherOptimizedPlan.itineraryStops.filter { it.dayNumber == 2 }
        assertTrue(day2Stops.any { it.notes.contains("Open-Meteo", ignoreCase = true) || it.notes.contains("☔") })
    }
}
