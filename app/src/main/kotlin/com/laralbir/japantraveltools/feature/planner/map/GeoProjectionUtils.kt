package com.laralbir.japantraveltools.feature.planner.map

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

data class GeoBoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
    val centerLat: Double,
    val centerLng: Double
)

enum class MapTileLayerType {
    GOOGLE_STREETS,
    GOOGLE_HYBRID,
    OPENSTREETMAP;

    fun resolveTileUrl(isEs: Boolean): String {
        val lang = if (isEs) "es" else "en"
        return when (this) {
            GOOGLE_STREETS -> "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}&hl=$lang"
            GOOGLE_HYBRID -> "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}&hl=$lang"
            OPENSTREETMAP -> "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        }
    }

    fun label(isEs: Boolean): String = when (this) {
        GOOGLE_STREETS -> "🗺️ Google Maps"
        GOOGLE_HYBRID -> if (isEs) "🛰️ Satélite" else "🛰️ Satellite"
        OPENSTREETMAP -> "🌍 OpenStreetMap"
    }

    fun shortLabel(isEs: Boolean): String = when (this) {
        GOOGLE_STREETS -> "🗺️ Google"
        GOOGLE_HYBRID -> if (isEs) "🛰️ Satélite" else "🛰️ Sat."
        OPENSTREETMAP -> "🌍 OSM"
    }

    fun next(): MapTileLayerType = when (this) {
        GOOGLE_STREETS -> GOOGLE_HYBRID
        GOOGLE_HYBRID -> OPENSTREETMAP
        OPENSTREETMAP -> GOOGLE_STREETS
    }
}


object GeoProjectionUtils {

    private const val EARTH_RADIUS_KM = 6371.0
    private const val MAX_LATITUDE = 85.05112878
    private const val MIN_LATITUDE = -85.05112878

    /**
     * Projects spherical coordinates (lat, lng) to normalized Web Mercator coordinates [0.0..1.0].
     * (0.0, 0.0) is the top-left (NW), (1.0, 1.0) is the bottom-right (SE).
     */
    fun projectToMercatorNormalized(latitude: Double, longitude: Double): Pair<Double, Double> {
        val clampedLat = latitude.coerceIn(MIN_LATITUDE, MAX_LATITUDE)
        val clampedLng = longitude.coerceIn(-180.0, 180.0)

        val x = (clampedLng + 180.0) / 360.0

        val latRad = Math.toRadians(clampedLat)
        val mercN = ln(tan(Math.PI / 4.0 + latRad / 2.0))
        val y = 0.5 - (mercN / (2.0 * Math.PI))

        return Pair(x.coerceIn(0.0, 1.0), y.coerceIn(0.0, 1.0))
    }

    /**
     * Calculates the great-circle distance between two points in kilometers using the Haversine formula.
     */
    fun haversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
            cos(rLat1) * cos(rLat2) *
            sin(dLon / 2.0) * sin(dLon / 2.0)

        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Computes the bounding box for a set of geographic points, adding an optional padding margin.
     */
    fun calculateBoundingBox(
        points: List<GeoPoint>,
        paddingRatio: Double = 0.1
    ): GeoBoundingBox {
        if (points.isEmpty()) {
            return GeoBoundingBox(
                minLat = 35.65,
                maxLat = 35.72,
                minLng = 139.68,
                maxLng = 139.80,
                centerLat = 35.685,
                centerLng = 139.74
            )
        }

        var minLat = points.minOf { it.latitude }
        var maxLat = points.maxOf { it.latitude }
        var minLng = points.minOf { it.longitude }
        var maxLng = points.maxOf { it.longitude }

        val spanLat = max(maxLat - minLat, 0.005)
        val spanLng = max(maxLng - minLng, 0.005)

        val padLat = spanLat * paddingRatio
        val padLng = spanLng * paddingRatio

        minLat -= padLat
        maxLat += padLat
        minLng -= padLng
        maxLng += padLng

        return GeoBoundingBox(
            minLat = minLat,
            maxLat = maxLat,
            minLng = minLng,
            maxLng = maxLng,
            centerLat = (minLat + maxLat) / 2.0,
            centerLng = (minLng + maxLng) / 2.0
        )
    }

    /**
     * Calculates the cumulative total distance of consecutive stops along an itinerary route.
     */
    fun calculateRouteTotalDistanceKm(stops: List<ItineraryStopItem>): Double {
        val validPoints = stops.filter { it.latitude != 0.0 && it.longitude != 0.0 }
        if (validPoints.size < 2) return 0.0

        var total = 0.0
        for (i in 0 until validPoints.size - 1) {
            val p1 = validPoints[i]
            val p2 = validPoints[i + 1]
            total += haversineDistanceKm(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        }
        return total
    }
}
