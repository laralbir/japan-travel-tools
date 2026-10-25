package com.laralbir.japantraveltools.feature.weather

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CityGeocodingHelperTest {

    @Test
    fun `searchPredictions returns empty for blank or single char query`() {
        assertTrue(CityGeocodingHelper.searchPredictions("").isEmpty())
        assertTrue(CityGeocodingHelper.searchPredictions(" ").isEmpty())
        assertTrue(CityGeocodingHelper.searchPredictions("t").isEmpty())
    }

    @Test
    fun `searchPredictions matches Japanese cities by prefix and substring`() {
        val resultsTokyo = CityGeocodingHelper.searchPredictions("Tok")
        assertTrue(resultsTokyo.any { it.nameEs.contains("Tokio", ignoreCase = true) || it.nameEn.contains("Tokyo", ignoreCase = true) })
        val tokyo = resultsTokyo.first { it.nameEn == "Tokyo" }
        assertEquals(35.6762, tokyo.latitude, 0.01)
        assertEquals(139.6503, tokyo.longitude, 0.01)

        val resultsKyoto = CityGeocodingHelper.searchPredictions("Kyo")
        assertTrue(resultsKyoto.any { it.nameEn == "Kyoto" })

        val resultsOsaka = CityGeocodingHelper.searchPredictions("Osa")
        assertTrue(resultsOsaka.any { it.nameEn == "Osaka" })
    }

    @Test
    fun `searchPredictions handles accents and alternate spellings`() {
        // "kioto" in Spanish vs "kyoto" in English
        val resultsKioto = CityGeocodingHelper.searchPredictions("kioto")
        assertTrue(resultsKioto.isNotEmpty())
        assertEquals("Kioto", resultsKioto.first().nameEs)

        // "Málaga" with accent vs "malaga" without accent
        val resultsMalaga = CityGeocodingHelper.searchPredictions("malaga")
        assertTrue(resultsMalaga.any { it.nameEs.contains("Málaga", ignoreCase = true) || it.nameEs.contains("Malaga", ignoreCase = true) })

        val resultsMalagaAccent = CityGeocodingHelper.searchPredictions("málaga")
        assertTrue(resultsMalagaAccent.isNotEmpty())
    }

    @Test
    fun `searchPredictions includes international hubs and flight origins`() {
        val resultsMadrid = CityGeocodingHelper.searchPredictions("Madrid")
        assertTrue(resultsMadrid.any { it.nameEs == "Madrid" })
        val madrid = resultsMadrid.first { it.nameEs == "Madrid" }
        assertEquals(40.4168, madrid.latitude, 0.01)
        assertEquals(-3.7038, madrid.longitude, 0.01)

        val resultsDoha = CityGeocodingHelper.searchPredictions("Doha")
        assertTrue(resultsDoha.any { it.nameEs == "Doha" })

        val resultsBarcelona = CityGeocodingHelper.searchPredictions("Barce")
        assertTrue(resultsBarcelona.any { it.nameEs == "Barcelona" })
    }

    @Test
    fun `searchPredictions matches popular area and neighborhood aliases`() {
        val fujiMatches = CityGeocodingHelper.searchPredictions("fuji")
        assertTrue(fujiMatches.any { it.nameEs.contains("Fuji") })

        val shinjukuMatches = CityGeocodingHelper.searchPredictions("shinjuku")
        assertTrue(shinjukuMatches.any { it.nameEs.contains("Tokio") })

        val hakataMatches = CityGeocodingHelper.searchPredictions("hakata")
        assertTrue(hakataMatches.any { it.nameEs.contains("Fukuoka") })
    }

    @Test
    fun `searchPredictions respects maxResults parameter`() {
        val maxResults = 3
        val results = CityGeocodingHelper.searchPredictions("a", maxResults = maxResults)
        // Note: single char "a" returns empty; with 2 chars "ka":
        val resultsKa = CityGeocodingHelper.searchPredictions("ka", maxResults = maxResults)
        assertTrue(resultsKa.size in 1..maxResults)
    }

    @Test
    fun `getFromCatalog finds exact and normalized matches`() {
        val tokyo = CityGeocodingHelper.getFromCatalog("Tokio")
        assertNotNull(tokyo)
        assertEquals(35.6762, tokyo!!.latitude, 0.01)

        val fuji = CityGeocodingHelper.getFromCatalog("Hakone / Monte Fuji")
        assertNotNull(fuji)
        assertEquals(35.2324, fuji!!.latitude, 0.01)

        val unknown = CityGeocodingHelper.getFromCatalog("CiudadInexistenteXYZ123")
        assertNull(unknown)
    }

    @Test
    fun `parseOpenMeteoGeocodingResponse extracts coordinates and location info`() {
        val sampleJson = """
            {
              "results": [
                {
                  "id": 1850147,
                  "name": "Tokyo",
                  "latitude": 35.6895,
                  "longitude": 139.69171,
                  "elevation": 40.0,
                  "feature_code": "PPLC",
                  "country_code": "JP",
                  "admin1_id": 1850144,
                  "timezone": "Asia/Tokyo",
                  "population": 8336599,
                  "country": "Japón",
                  "admin1": "Tokio"
                },
                {
                  "id": 1857910,
                  "name": "Kyoto",
                  "latitude": 35.02107,
                  "longitude": 135.75385,
                  "country_code": "JP",
                  "country": "Japón",
                  "admin1": "Kioto"
                }
              ]
            }
        """.trimIndent()

        val parsed = CityGeocodingHelper.parseOpenMeteoGeocodingResponse(sampleJson)
        assertEquals(2, parsed.size)
        assertEquals("Tokyo", parsed[0].nameEn)
        assertEquals("Japón", parsed[0].country)
        assertEquals(35.6895, parsed[0].latitude, 0.0001)
        assertEquals(139.69171, parsed[0].longitude, 0.0001)
        assertEquals("Open-Meteo", parsed[0].source)

        assertEquals("Kyoto", parsed[1].nameEn)
        assertEquals(35.02107, parsed[1].latitude, 0.0001)
    }

    @Test
    fun `parseOpenMeteoGeocodingResponse handles empty or corrupt JSON gracefully`() {
        assertTrue(CityGeocodingHelper.parseOpenMeteoGeocodingResponse("").isEmpty())
        assertTrue(CityGeocodingHelper.parseOpenMeteoGeocodingResponse("   ").isEmpty())
        assertTrue(CityGeocodingHelper.parseOpenMeteoGeocodingResponse("{\"error\":true}").isEmpty())
        assertTrue(CityGeocodingHelper.parseOpenMeteoGeocodingResponse("{invalid-json}").isEmpty())
    }

    @Test
    fun `geocodeCity resolves from catalog without calling network when matching`() = runTest {
        val city = CityGeocodingHelper.geocodeCity(context = null, query = "Kyoto")
        assertNotNull(city)
        assertEquals("Kyoto", city!!.nameEn)
        assertEquals(35.0116, city.latitude, 0.01)
        assertEquals(135.7681, city.longitude, 0.01)
        assertEquals("Catalog", city.source)
    }

    @Test
    fun `all destination presets are covered in CityGeocodingHelper catalog`() {
        val presets = WeatherCityCatalog.destinationPresets()
        for (preset in presets) {
            val found = CityGeocodingHelper.getFromCatalog(preset.nameEn)
                ?: CityGeocodingHelper.getFromCatalog(preset.nameEs)
            assertNotNull("Preset ${preset.nameEn} should be found in CityGeocodingHelper", found)
        }
    }
}
