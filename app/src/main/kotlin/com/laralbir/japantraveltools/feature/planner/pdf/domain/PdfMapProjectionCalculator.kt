package com.laralbir.japantraveltools.feature.planner.pdf.domain

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.map.GeoProjectionUtils
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Projected stop coordinates mapped to Canvas pixel/point space within the PDF card bounds.
 */
data class PdfMapPoint(
    val x: Float,
    val y: Float,
    val stop: ItineraryStopItem,
    val orderIndex: Int
)

/**
 * Complete calculated layout of a daily route map ready for rendering on Android Canvas.
 */
data class PdfMapLayout(
    val width: Float,
    val height: Float,
    val points: List<PdfMapPoint>,
    val routeDistanceKm: Double,
    val centerLat: Double,
    val centerLng: Double,
    val approxScaleLabel: String,
    val hasValidCoordinates: Boolean
)

/**
 * Pure domain geometric projection engine for rendering vector itinerary maps in PDF documents.
 * 100% offline, deterministic, and free of platform UI dependencies.
 */
object PdfMapProjectionCalculator {

    const val DEFAULT_MAP_HEIGHT = 160f
    const val INNER_PADDING = 24f

    /**
     * Projects a list of daily itinerary stops onto a bounded 2D Cartesian plane (Canvas)
     * using an equirectangular projection calibrated for Japan's latitude (~35° N).
     */
    fun projectStopsToCanvas(
        stops: List<ItineraryStopItem>,
        width: Float = 523f, // Standard printable width
        height: Float = DEFAULT_MAP_HEIGHT,
        padding: Float = INNER_PADDING
    ): PdfMapLayout {
        val validStopsWithIndex = stops.mapIndexedNotNull { index, stop ->
            val lat = stop.latitude
            val lng = stop.longitude
            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                Triple(stop, index + 1, lat to lng)
            } else null
        }

        if (validStopsWithIndex.isEmpty()) {
            return PdfMapLayout(
                width = width,
                height = height,
                points = emptyList(),
                routeDistanceKm = 0.0,
                centerLat = 0.0,
                centerLng = 0.0,
                approxScaleLabel = "",
                hasValidCoordinates = false
            )
        }

        val validStops = validStopsWithIndex.map { it.first }
        val routeDistance = GeoProjectionUtils.calculateRouteTotalDistanceKm(validStops)

        if (validStopsWithIndex.size == 1) {
            val (singleStop, orderIdx, coords) = validStopsWithIndex.first()
            val centerX = width / 2f
            val centerY = height / 2f
            val point = PdfMapPoint(
                x = centerX,
                y = centerY,
                stop = singleStop,
                orderIndex = orderIdx
            )
            return PdfMapLayout(
                width = width,
                height = height,
                points = listOf(point),
                routeDistanceKm = 0.0,
                centerLat = coords.first,
                centerLng = coords.second,
                approxScaleLabel = "1 parada geolocalizada",
                hasValidCoordinates = true
            )
        }

        val lats = validStopsWithIndex.map { it.third.first }
        val lngs = validStopsWithIndex.map { it.third.second }

        val minLat = lats.minOrNull() ?: 35.0
        val maxLat = lats.maxOrNull() ?: 35.0
        val minLng = lngs.minOrNull() ?: 139.0
        val maxLng = lngs.maxOrNull() ?: 139.0

        val centerLat = (minLat + maxLat) / 2.0
        val centerLng = (minLng + maxLng) / 2.0

        val latRad = Math.toRadians(centerLat)
        val cosLat = cos(latRad).coerceAtLeast(0.1)

        val rawSpanLat = max(maxLat - minLat, 0.002)
        val rawSpanLng = max(maxLng - minLng, 0.002)

        // Add 20% geographical padding around points so pins never touch edges
        val padRatio = 0.20
        val spanLat = rawSpanLat * (1.0 + padRatio * 2.0)
        val spanLng = rawSpanLng * (1.0 + padRatio * 2.0)

        val geoSpanX = spanLng * cosLat
        val geoSpanY = spanLat

        val plotW = max(width - (padding * 2f), 20f)
        val plotH = max(height - (padding * 2f), 20f)

        val scale = min(plotW / geoSpanX, plotH / geoSpanY)

        val centerX = width / 2f
        val centerY = height / 2f

        val projectedPoints = validStopsWithIndex.map { (stop, orderIdx, coords) ->
            val relLng = (coords.second - centerLng) * cosLat
            val relLat = coords.first - centerLat

            val px = (centerX + (relLng * scale)).toFloat().coerceIn(padding, width - padding)
            val py = (centerY - (relLat * scale)).toFloat().coerceIn(padding, height - padding)

            PdfMapPoint(
                x = px,
                y = py,
                stop = stop,
                orderIndex = orderIdx
            )
        }

        val scaleLabel = if (routeDistance > 0.0) {
            String.format(Locale.US, "Distancia acumulada: %.1f km", routeDistance)
        } else {
            "${projectedPoints.size} paradas"
        }

        return PdfMapLayout(
            width = width,
            height = height,
            points = projectedPoints,
            routeDistanceKm = routeDistance,
            centerLat = centerLat,
            centerLng = centerLng,
            approxScaleLabel = scaleLabel,
            hasValidCoordinates = true
        )
    }
}
