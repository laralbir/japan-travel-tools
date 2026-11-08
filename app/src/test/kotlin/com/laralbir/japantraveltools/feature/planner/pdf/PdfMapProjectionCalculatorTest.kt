package com.laralbir.japantraveltools.feature.planner.pdf

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.planner.pdf.domain.PdfMapProjectionCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfMapProjectionCalculatorTest {

    @Test
    fun testProjectStops_emptyList_returnsEmptyLayout() {
        val layout = PdfMapProjectionCalculator.projectStopsToCanvas(
            stops = emptyList(),
            width = 500f,
            height = 160f
        )
        assertFalse(layout.hasValidCoordinates)
        assertTrue(layout.points.isEmpty())
        assertEquals(0.0, layout.routeDistanceKm, 0.001)
    }

    @Test
    fun testProjectStops_stopsWithoutCoordinates_returnsEmptyLayout() {
        val stopsWithoutCoords = listOf(
            createStop("s1", "Stop 1", lat = null, lng = null),
            createStop("s2", "Stop 2", lat = null, lng = null)
        )
        val layout = PdfMapProjectionCalculator.projectStopsToCanvas(
            stops = stopsWithoutCoords,
            width = 500f,
            height = 160f
        )
        assertFalse(layout.hasValidCoordinates)
        assertTrue(layout.points.isEmpty())
    }

    @Test
    fun testProjectStops_singleStop_placesAtCenter() {
        val singleStop = listOf(
            createStop("s1", "Tokyo Tower", lat = 35.6586, lng = 139.7454)
        )
        val layout = PdfMapProjectionCalculator.projectStopsToCanvas(
            stops = singleStop,
            width = 500f,
            height = 160f
        )
        assertTrue(layout.hasValidCoordinates)
        assertEquals(1, layout.points.size)
        assertEquals(250f, layout.points[0].x, 1f)
        assertEquals(80f, layout.points[0].y, 1f)
        assertEquals(1, layout.points[0].orderIndex)
    }

    @Test
    fun testProjectStops_multipleStops_preservesGeographicOrientation() {
        // Asakusa is North-East of Shibuya
        val shibuya = createStop("s1", "Shibuya Crossing", lat = 35.6595, lng = 139.7005)
        val asakusa = createStop("s2", "Senso-ji Asakusa", lat = 35.7148, lng = 139.7967)

        val layout = PdfMapProjectionCalculator.projectStopsToCanvas(
            stops = listOf(shibuya, asakusa),
            width = 500f,
            height = 200f,
            padding = 20f
        )

        assertTrue(layout.hasValidCoordinates)
        assertEquals(2, layout.points.size)

        val pShibuya = layout.points[0]
        val pAsakusa = layout.points[1]

        // Shibuya (West) should have smaller X than Asakusa (East)
        assertTrue("Shibuya X (${pShibuya.x}) should be less than Asakusa X (${pAsakusa.x})", pShibuya.x < pAsakusa.x)

        // Shibuya (South) should have larger Y (canvas downwards) than Asakusa (North, smaller Y)
        assertTrue("Shibuya Y (${pShibuya.y}) should be greater than Asakusa Y (${pAsakusa.y})", pShibuya.y > pAsakusa.y)

        // All points must be within printable padding
        layout.points.forEach { pt ->
            assertTrue("Point X out of bounds", pt.x in 20f..480f)
            assertTrue("Point Y out of bounds", pt.y in 20f..180f)
        }

        // Distance should be greater than 8 km between Shibuya and Asakusa
        assertTrue("Distance should be around 11 km", layout.routeDistanceKm in 8.0..15.0)
    }

    @Test
    fun testProjectStops_mixedCoordinates_filtersValidStopsOnly() {
        val stops = listOf(
            createStop("s1", "Valid 1", lat = 35.0, lng = 135.0),
            createStop("s2", "No coords", lat = null, lng = null),
            createStop("s3", "Valid 2", lat = 35.1, lng = 135.1)
        )
        val layout = PdfMapProjectionCalculator.projectStopsToCanvas(stops, width = 400f, height = 200f)
        assertTrue(layout.hasValidCoordinates)
        assertEquals(2, layout.points.size)
        assertEquals(1, layout.points[0].orderIndex)
        assertEquals(3, layout.points[1].orderIndex) // Kept original 1-based order in day!
    }

    private fun createStop(id: String, title: String, lat: Double?, lng: Double?): ItineraryStopItem {
        return ItineraryStopItem(
            id = id,
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "09:00",
            endTime = "10:30",
            title = title,
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "テスト",
            latitude = lat,
            longitude = lng
        )
    }
}
