package com.laralbir.japantraveltools.feature.planner.optimizer.model

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem

/**
 * Configuration options for the geodesic day route optimizer (SPEC-13 / ANALYSIS-15).
 */
data class RouteOptimizationOptions(
    val keepFirstStopFixed: Boolean = true,
    val keepLastStopFixed: Boolean = false,
    val respectFixedTimeStops: Boolean = true,
    val defaultTransferBufferMinutes: Int = 20
)

/**
 * Outcome and comparative metrics calculated by [com.laralbir.japantraveltools.feature.planner.optimizer.DayRouteOptimizer].
 */
data class RouteOptimizationResult(
    val dayIndex: Int,
    val originalStops: List<ItineraryStopItem>,
    val optimizedStops: List<ItineraryStopItem>,
    val originalDistanceMeters: Double,
    val optimizedDistanceMeters: Double,
    val distanceSavedMeters: Double,
    val percentageSaved: Double,
    val estimatedMinutesSaved: Int,
    val isBeneficial: Boolean = distanceSavedMeters > 200.0
)
