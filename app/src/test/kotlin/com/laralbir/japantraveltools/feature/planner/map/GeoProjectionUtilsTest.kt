package com.laralbir.japantraveltools.feature.planner.map

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoProjectionUtilsTest {

    @Test
    fun `projectToMercatorNormalized returns 0_5 for prime meridian and equator`() {
        val (x, y) = GeoProjectionUtils.projectToMercatorNormalized(0.0, 0.0)
        assertEquals(0.5, x, 0.001)
        assertEquals(0.5, y, 0.001)
    }

    @Test
    fun `projectToMercatorNormalized returns valid range for Tokyo and Kyoto`() {
        val (tokyoX, tokyoY) = GeoProjectionUtils.projectToMercatorNormalized(35.6762, 139.6503)
        assertTrue(tokyoX in 0.0..1.0)
        assertTrue(tokyoY in 0.0..1.0)

        val (kyotoX, kyotoY) = GeoProjectionUtils.projectToMercatorNormalized(34.9858, 135.7588)
        assertTrue(kyotoX in 0.0..1.0)
        assertTrue(kyotoY in 0.0..1.0)

        // Kyoto is west of Tokyo, so Kyoto x < Tokyo x
        assertTrue("Kyoto should be west of Tokyo", kyotoX < tokyoX)
    }

    @Test
    fun `haversineDistanceKm calculates accurate distance between Tokyo and Kyoto`() {
        val tokyoLat = 35.6812
        val tokyoLng = 139.7671
        val kyotoLat = 34.9858
        val kyotoLng = 135.7588

        val distanceKm = GeoProjectionUtils.haversineDistanceKm(tokyoLat, tokyoLng, kyotoLat, kyotoLng)
        // Actual direct distance is ~370 km
        assertTrue("Distance between Tokyo and Kyoto should be ~370km, was $distanceKm", distanceKm in 350.0..390.0)

        val zeroDistance = GeoProjectionUtils.haversineDistanceKm(tokyoLat, tokyoLng, tokyoLat, tokyoLng)
        assertEquals(0.0, zeroDistance, 0.0001)
    }

    @Test
    fun `calculateBoundingBox frames all points with safety margin`() {
        val points = listOf(
            GeoPoint(35.7147, 139.7966), // Asakusa
            GeoPoint(35.6586, 139.7454), // Tokyo Tower
            GeoPoint(35.6580, 139.7016)  // Shibuya
        )

        val bbox = GeoProjectionUtils.calculateBoundingBox(points, paddingRatio = 0.1)

        assertTrue(bbox.minLat < 35.6580)
        assertTrue(bbox.maxLat > 35.7147)
        assertTrue(bbox.minLng < 139.7016)
        assertTrue(bbox.maxLng > 139.7966)
        assertTrue(bbox.centerLat in bbox.minLat..bbox.maxLat)
        assertTrue(bbox.centerLng in bbox.minLng..bbox.maxLng)
    }

    @Test
    fun `calculateRouteTotalDistanceKm returns sum of consecutive legs`() {
        val stop1 = ItineraryStopItem(
            id = "s1", dayNumber = 1, dateIso = "2026-11-11", startTime = "09:00", endTime = "10:00",
            title = "Stop 1", category = StopCategory.SIGHTSEEING, placeNameJapanese = "",
            latitude = 35.0, longitude = 135.0, notes = ""
        )
        val stop2 = ItineraryStopItem(
            id = "s2", dayNumber = 1, dateIso = "2026-11-11", startTime = "10:30", endTime = "11:30",
            title = "Stop 2", category = StopCategory.SIGHTSEEING, placeNameJapanese = "",
            latitude = 35.05, longitude = 135.05, notes = ""
        )
        val stop3 = ItineraryStopItem(
            id = "s3", dayNumber = 1, dateIso = "2026-11-11", startTime = "12:00", endTime = "13:00",
            title = "Stop 3", category = StopCategory.SIGHTSEEING, placeNameJapanese = "",
            latitude = 35.10, longitude = 135.10, notes = ""
        )

        val total = GeoProjectionUtils.calculateRouteTotalDistanceKm(listOf(stop1, stop2, stop3))
        val leg1 = GeoProjectionUtils.haversineDistanceKm(35.0, 135.0, 35.05, 135.05)
        val leg2 = GeoProjectionUtils.haversineDistanceKm(35.05, 135.05, 35.10, 135.10)

        assertEquals(leg1 + leg2, total, 0.001)
    }

    @Test
    fun `MapTileLayerType resolves localized tile URLs for Google Streets, Hybrid, and OSM`() {
        assertEquals(
            "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}&hl=es",
            MapTileLayerType.GOOGLE_STREETS.resolveTileUrl(isEs = true)
        )
        assertEquals(
            "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}&hl=en",
            MapTileLayerType.GOOGLE_STREETS.resolveTileUrl(isEs = false)
        )

        assertEquals(
            "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}&hl=es",
            MapTileLayerType.GOOGLE_HYBRID.resolveTileUrl(isEs = true)
        )
        assertEquals(
            "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}&hl=en",
            MapTileLayerType.GOOGLE_HYBRID.resolveTileUrl(isEs = false)
        )

        assertEquals(
            "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
            MapTileLayerType.OPENSTREETMAP.resolveTileUrl(isEs = true)
        )
        assertEquals(
            "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
            MapTileLayerType.OPENSTREETMAP.resolveTileUrl(isEs = false)
        )
    }

    @Test
    fun `MapTileLayerType returns localized labels and shortLabels`() {
        assertEquals("🗺️ Google Maps", MapTileLayerType.GOOGLE_STREETS.label(isEs = true))
        assertEquals("🗺️ Google Maps", MapTileLayerType.GOOGLE_STREETS.label(isEs = false))
        assertEquals("🗺️ Google", MapTileLayerType.GOOGLE_STREETS.shortLabel(isEs = true))
        assertEquals("🗺️ Google", MapTileLayerType.GOOGLE_STREETS.shortLabel(isEs = false))

        assertEquals("🛰️ Satélite", MapTileLayerType.GOOGLE_HYBRID.label(isEs = true))
        assertEquals("🛰️ Satellite", MapTileLayerType.GOOGLE_HYBRID.label(isEs = false))
        assertEquals("🛰️ Satélite", MapTileLayerType.GOOGLE_HYBRID.shortLabel(isEs = true))
        assertEquals("🛰️ Sat.", MapTileLayerType.GOOGLE_HYBRID.shortLabel(isEs = false))

        assertEquals("🌍 OpenStreetMap", MapTileLayerType.OPENSTREETMAP.label(isEs = true))
        assertEquals("🌍 OpenStreetMap", MapTileLayerType.OPENSTREETMAP.label(isEs = false))
        assertEquals("🌍 OSM", MapTileLayerType.OPENSTREETMAP.shortLabel(isEs = true))
        assertEquals("🌍 OSM", MapTileLayerType.OPENSTREETMAP.shortLabel(isEs = false))
    }

    @Test
    fun `MapTileLayerType next cycles through all layers in order`() {
        assertEquals(MapTileLayerType.GOOGLE_HYBRID, MapTileLayerType.GOOGLE_STREETS.next())
        assertEquals(MapTileLayerType.OPENSTREETMAP, MapTileLayerType.GOOGLE_HYBRID.next())
        assertEquals(MapTileLayerType.GOOGLE_STREETS, MapTileLayerType.OPENSTREETMAP.next())
    }

    @Test
    fun `formatCompactStopLabel strips parenthetical clutter and formats HH_mm cleanly`() {
        assertEquals(
            "07:00 Fushimi Inari-taisha",
            GeoProjectionUtils.formatCompactStopLabel(
                startTime = "07:00",
                title = "Fushimi Inari-taisha (Mil Torii)"
            )
        )
        assertEquals(
            "11:00 Tokyo Skytree Tembo Deck",
            GeoProjectionUtils.formatCompactStopLabel(
                startTime = "11:00:00",
                title = "Tokyo Skytree Tembo Deck (350m)"
            )
        )
        assertEquals(
            "Kiyomizu-dera",
            GeoProjectionUtils.formatCompactStopLabel(
                startTime = "   ",
                title = "  Kiyomizu-dera  "
            )
        )
        // If the entire title is parenthetical, preserve it rather than returning empty text
        assertEquals(
            "09:15 (Parada libre)",
            GeoProjectionUtils.formatCompactStopLabel(
                startTime = "09:15",
                title = "(Parada libre)"
            )
        )
    }

    @Test
    fun `formatCompactStopLabel truncates long titles gracefully with ellipsis within maxChars`() {
        val compact = GeoProjectionUtils.formatCompactStopLabel(
            startTime = "08:30",
            title = "Templo Kinkaku-ji y Jardín Tradicional Muromachi en el Norte de Kioto",
            maxChars = 34
        )
        assertTrue("Compact label length (${compact.length}) must be <= 34: $compact", compact.length <= 34)
        assertTrue("Compact label must start with time prefix: $compact", compact.startsWith("08:30 "))
        assertTrue("Compact label must end with ellipsis: $compact", compact.endsWith("…"))
    }

    @Test
    fun `computeVisibleLabelStopIds hides overlapping clustered labels keeping chronological priority`() {
        val points = listOf(
            "kyoto-1" to (200.0 to 300.0),
            "kyoto-2" to (235.0 to 312.0), // Collides with kyoto-1 (dx=35 < 112, dy=12 < 44)
            "kyoto-3" to (250.0 to 325.0), // Collides with kyoto-1 (dx=50 < 112, dy=25 < 44)
            "kyoto-north" to (210.0 to 240.0), // Vertically separated (dy=60 >= 44) -> visible
            "tokyo-1" to (520.0 to 180.0) // Horizontally & vertically separated -> visible
        )

        val visible = GeoProjectionUtils.computeVisibleLabelStopIds(
            points = points,
            selectedStopId = null,
            minHorizontalGapPx = 112.0,
            minVerticalGapPx = 44.0
        )

        assertEquals(setOf("kyoto-1", "kyoto-north", "tokyo-1"), visible)
    }

    @Test
    fun `computeVisibleLabelStopIds always prioritizes selectedStopId over earlier clustered stops`() {
        val points = listOf(
            "stop-1" to (200.0 to 300.0),
            "stop-2" to (225.0 to 310.0), // Selected! Should stay visible and suppress stop-1 & stop-3
            "stop-3" to (250.0 to 320.0),
            "stop-4" to (420.0 to 310.0)  // Far enough horizontally (dx=195 >= 112) -> visible
        )

        val visible = GeoProjectionUtils.computeVisibleLabelStopIds(
            points = points,
            selectedStopId = "stop-2",
            minHorizontalGapPx = 112.0,
            minVerticalGapPx = 44.0
        )

        assertEquals(setOf("stop-2", "stop-4"), visible)
        assertEquals(
            emptySet<String>(),
            GeoProjectionUtils.computeVisibleLabelStopIds(emptyList(), selectedStopId = "stop-2")
        )
    }
}
