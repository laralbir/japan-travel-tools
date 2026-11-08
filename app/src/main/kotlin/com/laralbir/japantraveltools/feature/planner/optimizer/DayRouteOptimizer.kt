package com.laralbir.japantraveltools.feature.planner.optimizer

import com.laralbir.japantraveltools.core.datetime.TravelDateTimeFormatter
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.optimizer.model.RouteOptimizationOptions
import com.laralbir.japantraveltools.feature.planner.optimizer.model.RouteOptimizationResult
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 100% Offline Geodesic Day Route Optimizer for Japan Travel Tools (SPEC-13 / ANALYSIS-15).
 *
 * Implements a hybrid TSP heuristic (Nearest Neighbor construction + constrained 2-opt local search)
 * supporting fixed starts, fixed ends, immutable time reservations, and stops without coordinates.
 */
class DayRouteOptimizer {

    /**
     * Optimizes the sequence of visits for a single day itinerary.
     */
    fun optimize(
        stops: List<ItineraryStopItem>,
        options: RouteOptimizationOptions = RouteOptimizationOptions(),
        dayIndex: Int = stops.firstOrNull()?.dayNumber ?: 1
    ): RouteOptimizationResult {
        val originalDistance = calculateTotalDistanceMeters(stops)

        // 1. Precondition: At least 3 stops with valid coordinates required to optimize
        val validCoordStops = stops.filter { hasValidCoordinates(it) }
        if (validCoordStops.size < 3) {
            return RouteOptimizationResult(
                dayIndex = dayIndex,
                originalStops = stops,
                optimizedStops = stops,
                originalDistanceMeters = originalDistance,
                optimizedDistanceMeters = originalDistance,
                distanceSavedMeters = 0.0,
                percentageSaved = 0.0,
                estimatedMinutesSaved = 0,
                isBeneficial = false
            )
        }

        val totalStopsCount = stops.size

        // 2. Determine fixed slots (cannot be reordered)
        val fixedIndices = mutableSetOf<Int>()
        if (options.keepFirstStopFixed && totalStopsCount > 0) {
            fixedIndices.add(0)
        }
        if (options.keepLastStopFixed && totalStopsCount > 0) {
            fixedIndices.add(totalStopsCount - 1)
        }

        stops.forEachIndexed { idx, stop ->
            if (options.respectFixedTimeStops && stop.isTimeFixed) {
                fixedIndices.add(idx)
            }
            if (!hasValidCoordinates(stop)) {
                fixedIndices.add(idx)
            }
        }

        val freeIndices = (0 until totalStopsCount).filter { it !in fixedIndices }
        val pool = freeIndices.map { stops[it] }

        if (freeIndices.size <= 1) {
            return RouteOptimizationResult(
                dayIndex = dayIndex,
                originalStops = stops,
                optimizedStops = stops,
                originalDistanceMeters = originalDistance,
                optimizedDistanceMeters = originalDistance,
                distanceSavedMeters = 0.0,
                percentageSaved = 0.0,
                estimatedMinutesSaved = 0,
                isBeneficial = false
            )
        }

        // 3. Phase 1: Nearest Neighbor Construction
        val constructed = stops.toMutableList()
        val unplaced = pool.toMutableList()
        var lastPoint: Pair<Double, Double>? = null

        for (slot in 0 until totalStopsCount) {
            if (slot in fixedIndices) {
                val fixedStop = stops[slot]
                constructed[slot] = fixedStop
                if (hasValidCoordinates(fixedStop)) {
                    lastPoint = Pair(fixedStop.latitude!!, fixedStop.longitude!!)
                }
            } else {
                val chosen = if (lastPoint != null) {
                    val lp = lastPoint
                    unplaced.minByOrNull { item ->
                        if (hasValidCoordinates(item)) {
                            haversineDistanceMeters(
                                lp.first,
                                lp.second,
                                item.latitude!!,
                                item.longitude!!
                            )
                        } else {
                            Double.MAX_VALUE
                        }
                    } ?: unplaced.first()
                } else {
                    unplaced.first()
                }

                constructed[slot] = chosen
                unplaced.remove(chosen)
                if (hasValidCoordinates(chosen)) {
                    lastPoint = Pair(chosen.latitude!!, chosen.longitude!!)
                }
            }
        }

        // 4. Phase 2: Constrained 2-Opt Local Search & Free Index Swaps
        var currentRoute = constructed
        var currentDistance = calculateTotalDistanceMeters(currentRoute)

        val maxIterations = 50
        var iteration = 0
        var improved = true

        while (improved && iteration < maxIterations) {
            improved = false
            iteration++

            // A. Subsegment reversals (2-opt)
            for (i in 0 until totalStopsCount - 1) {
                for (j in i + 1 until totalStopsCount) {
                    var canReverse = true
                    for (k in i..j) {
                        if (k in fixedIndices) {
                            val targetK = i + j - k
                            if (targetK != k) {
                                canReverse = false
                                break
                            }
                        }
                    }

                    if (canReverse) {
                        val candidate = currentRoute.toMutableList()
                        var left = i
                        var right = j
                        while (left < right) {
                            val temp = candidate[left]
                            candidate[left] = candidate[right]
                            candidate[right] = temp
                            left++
                            right--
                        }
                        val candidateDist = calculateTotalDistanceMeters(candidate)
                        if (candidateDist < currentDistance - 0.5) {
                            currentRoute = candidate
                            currentDistance = candidateDist
                            improved = true
                            break
                        }
                    }
                }
                if (improved) break
            }

            // B. Pairwise swaps of free indices across fixed obstacles
            if (!improved) {
                for (a in 0 until freeIndices.size - 1) {
                    val u = freeIndices[a]
                    for (b in a + 1 until freeIndices.size) {
                        val v = freeIndices[b]
                        val candidate = currentRoute.toMutableList()
                        val temp = candidate[u]
                        candidate[u] = candidate[v]
                        candidate[v] = temp

                        val candidateDist = calculateTotalDistanceMeters(candidate)
                        if (candidateDist < currentDistance - 0.5) {
                            currentRoute = candidate
                            currentDistance = candidateDist
                            improved = true
                            break
                        }
                    }
                    if (improved) break
                }
            }
        }

        // 5. Ensure we never return a route worse than original
        val chosenStops = if (currentDistance < originalDistance - 0.5) {
            currentRoute
        } else {
            stops
        }
        val finalDistance = calculateTotalDistanceMeters(chosenStops)
        val distanceSaved = (originalDistance - finalDistance).coerceAtLeast(0.0)
        val percentageSaved = if (originalDistance > 0.0) {
            (distanceSaved / originalDistance) * 100.0
        } else {
            0.0
        }

        // Conservative estimate of transit time saved: ~4 minutes per kilometer in urban Japan
        val estimatedMinutesSaved = kotlin.math.round((distanceSaved / 1000.0) * 4.0).toInt().coerceAtLeast(0)

        // 6. Recalculate sequential orders (1..N) and tentative schedules
        var clockMinutes: Int? = null

        val scheduledStops = chosenStops.mapIndexed { index, stop ->
            val duration = stop.durationMinutes
            val isFixedSlot = stop.isTimeFixed || !hasValidCoordinates(stop)

            if (isFixedSlot) {
                val originalEnd = parseMinutes(stop.endTime)
                clockMinutes = originalEnd + options.defaultTransferBufferMinutes
                stop.copy(
                    order = index + 1
                )
            } else {
                val startMin = if (index == 0 || clockMinutes == null) {
                    parseMinutes(stop.startTime)
                } else {
                    clockMinutes
                }
                val endMin = startMin + duration
                clockMinutes = endMin + options.defaultTransferBufferMinutes

                val newStart = formatHhMm(startMin)
                val newEnd = formatHhMm(endMin)
                val startUtc = TravelDateTimeFormatter.localToUtcIso(stop.dateIso, newStart, stop.timeZoneId)
                val endUtc = TravelDateTimeFormatter.localToUtcIso(stop.dateIso, newEnd, stop.timeZoneId)

                stop.copy(
                    order = index + 1,
                    startTime = newStart,
                    endTime = newEnd,
                    startTimeUtcIso = startUtc,
                    endTimeUtcIso = endUtc
                )
            }
        }

        return RouteOptimizationResult(
            dayIndex = dayIndex,
            originalStops = stops,
            optimizedStops = scheduledStops,
            originalDistanceMeters = originalDistance,
            optimizedDistanceMeters = finalDistance,
            distanceSavedMeters = distanceSaved,
            percentageSaved = percentageSaved,
            estimatedMinutesSaved = estimatedMinutesSaved,
            isBeneficial = distanceSaved > 200.0
        )
    }

    /**
     * Calculates the great-circle distance between two points in meters using the Haversine formula.
     */
    fun haversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusMeters = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
            cos(rLat1) * cos(rLat2) *
            sin(dLon / 2.0) * sin(dLon / 2.0)

        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return earthRadiusMeters * c
    }

    /**
     * Calculates cumulative total distance of consecutive stops along a route.
     */
    fun calculateTotalDistanceMeters(stops: List<ItineraryStopItem>): Double {
        val valid = stops.filter { hasValidCoordinates(it) }
        if (valid.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until valid.size - 1) {
            val s1 = valid[i]
            val s2 = valid[i + 1]
            total += haversineDistanceMeters(
                s1.latitude!!,
                s1.longitude!!,
                s2.latitude!!,
                s2.longitude!!
            )
        }
        return total
    }

    private fun hasValidCoordinates(stop: ItineraryStopItem): Boolean {
        val lat = stop.latitude ?: return false
        val lng = stop.longitude ?: return false
        return !(lat == 0.0 && lng == 0.0)
    }

    private fun parseMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun formatHhMm(totalMinutes: Int): String {
        val bounded = totalMinutes.coerceIn(0, 23 * 60 + 59)
        val h = bounded / 60
        val m = bounded % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }
}
