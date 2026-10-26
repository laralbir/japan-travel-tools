package com.laralbir.japantraveltools.feature.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherForecastPersistenceTest {

    @Test
    fun exportAndImportForecastsJson_recoversExactForecasts() {
        val originalList = listOf(
            WeatherDayForecast(
                cityKey = "TOKYO",
                dateIso = "2026-11-11",
                wmoCode = 1,
                tempMaxC = 19.5,
                tempMinC = 11.2,
                precipitationProbabilityMax = 10,
                condition = WeatherConditionKind.CLEAR_SUNNY,
                suitability = PlanningSuitability.OUTDOOR_OPTIMAL,
                fujiVisibilityPercent = 85,
                userPlanningAdviceEs = "Día ideal para exteriores.",
                userPlanningAdviceEn = "Ideal day for outdoors.",
                aiPromptContextSummary = "TOKYO 2026-11-11 OUTDOOR_OPTIMAL"
            ),
            WeatherDayForecast(
                cityKey = "KYOTO",
                dateIso = "2026-11-12",
                wmoCode = 61,
                tempMaxC = 14.8,
                tempMinC = 8.5,
                precipitationProbabilityMax = 80,
                condition = WeatherConditionKind.RAIN,
                suitability = PlanningSuitability.INDOOR_RECOMMENDED,
                fujiVisibilityPercent = 10,
                userPlanningAdviceEs = "Lluvia prevista, llevar paraguas.",
                userPlanningAdviceEn = "Rain expected, carry umbrella.",
                aiPromptContextSummary = "KYOTO 2026-11-12 INDOOR_RECOMMENDED"
            )
        )

        val json = WeatherForecastJsonCodec.exportForecastsJson(originalList)
        assertTrue(json.contains("\"cityKey\": \"TOKYO\"") || json.contains("\"cityKey\":\"TOKYO\""))
        assertTrue(json.contains("\"cityKey\": \"KYOTO\"") || json.contains("\"cityKey\":\"KYOTO\""))

        val restored = WeatherForecastJsonCodec.importForecastsJson(json)
        assertEquals(2, restored.size)

        val tokyo = restored[0]
        assertEquals("TOKYO", tokyo.cityKey)
        assertEquals("2026-11-11", tokyo.dateIso)
        assertEquals(1, tokyo.wmoCode)
        assertEquals(19.5, tokyo.tempMaxC, 0.01)
        assertEquals(11.2, tokyo.tempMinC, 0.01)
        assertEquals(10, tokyo.precipitationProbabilityMax)
        assertEquals(WeatherConditionKind.CLEAR_SUNNY, tokyo.condition)
        assertEquals(PlanningSuitability.OUTDOOR_OPTIMAL, tokyo.suitability)
        assertEquals(85, tokyo.fujiVisibilityPercent)
        assertEquals("Día ideal para exteriores.", tokyo.userPlanningAdviceEs)
        assertEquals("Ideal day for outdoors.", tokyo.userPlanningAdviceEn)
        assertEquals("TOKYO 2026-11-11 OUTDOOR_OPTIMAL", tokyo.aiPromptContextSummary)

        val kyoto = restored[1]
        assertEquals("KYOTO", kyoto.cityKey)
        assertEquals("2026-11-12", kyoto.dateIso)
        assertEquals(61, kyoto.wmoCode)
        assertEquals(14.8, kyoto.tempMaxC, 0.01)
        assertEquals(8.5, kyoto.tempMinC, 0.01)
        assertEquals(80, kyoto.precipitationProbabilityMax)
        assertEquals(WeatherConditionKind.RAIN, kyoto.condition)
        assertEquals(PlanningSuitability.INDOOR_RECOMMENDED, kyoto.suitability)
    }

    @Test
    fun importForecastsJson_emptyOrInvalid_returnsEmptyListWithoutCrashing() {
        val empty = WeatherForecastJsonCodec.importForecastsJson("")
        assertTrue(empty.isEmpty())

        val invalid = WeatherForecastJsonCodec.importForecastsJson("{ invalid: true }")
        assertTrue(invalid.isEmpty())
    }
}
