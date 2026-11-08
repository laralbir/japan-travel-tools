package com.laralbir.japantraveltools.feature.planner.optimizer

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.planner.optimizer.model.RouteOptimizationOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayRouteOptimizerTest {

    private val optimizer = DayRouteOptimizer()

    @Test
    fun `Tokyo zig-zag route is optimized by grouping northeastern stops and saving over 30 percent distance`() {
        val uenoHotel = ItineraryStopItem(
            id = "stop-ueno",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "09:00",
            endTime = "10:00",
            title = "Hotel Ueno",
            category = StopCategory.HOTEL,
            placeNameJapanese = "上野ホテル",
            latitude = 35.7126,
            longitude = 139.7798
        )
        val shibuyaCrossing = ItineraryStopItem(
            id = "stop-shibuya",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "10:30",
            endTime = "12:00",
            title = "Shibuya Crossing",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "渋谷スクランブル交差点",
            latitude = 35.6595,
            longitude = 139.7005
        )
        val asakusaSensoji = ItineraryStopItem(
            id = "stop-asakusa",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "12:30",
            endTime = "14:00",
            title = "Asakusa Senso-ji",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "浅草寺",
            latitude = 35.7148,
            longitude = 139.7967
        )
        val akihabara = ItineraryStopItem(
            id = "stop-akiba",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "14:30",
            endTime = "16:00",
            title = "Akihabara Electric Town",
            category = StopCategory.SHOPPING,
            placeNameJapanese = "秋葉原電気街",
            latitude = 35.6983,
            longitude = 139.7731
        )

        // Original absurd zig-zag: Ueno -> Shibuya -> Asakusa -> Akihabara
        val originalStops = listOf(uenoHotel, shibuyaCrossing, asakusaSensoji, akihabara)
        val options = RouteOptimizationOptions(
            keepFirstStopFixed = true,
            keepLastStopFixed = false,
            respectFixedTimeStops = true,
            defaultTransferBufferMinutes = 20
        )

        val result = optimizer.optimize(originalStops, options)

        // 1. Hotel Ueno remains the first stop
        assertEquals("stop-ueno", result.optimizedStops.first().id)
        // 2. Shibuya is moved to the end, grouping Ueno, Asakusa and Akihabara
        assertEquals("stop-shibuya", result.optimizedStops.last().id)

        // Possible optimal sequences: Ueno -> Asakusa -> Akiba -> Shibuya OR Ueno -> Akiba -> Asakusa -> Shibuya
        val optimizedIds = result.optimizedStops.map { it.id }
        assertTrue(
            optimizedIds == listOf("stop-ueno", "stop-asakusa", "stop-akiba", "stop-shibuya") ||
                optimizedIds == listOf("stop-ueno", "stop-akiba", "stop-asakusa", "stop-shibuya")
        )

        // Distance savings should be > 30% and > 5000 meters
        assertTrue("Distance saved should exceed 5000m: ${result.distanceSavedMeters}", result.distanceSavedMeters > 5000.0)
        assertTrue("Percentage saved should exceed 30%: ${result.percentageSaved}", result.percentageSaved > 30.0)
        assertTrue("Result should be marked beneficial", result.isBeneficial)
        assertTrue("Estimated minutes saved should be positive", result.estimatedMinutesSaved > 15)

        // Sequential order reassignment: 1..4
        assertEquals(listOf(1, 2, 3, 4), result.optimizedStops.map { it.order })

        // Recalculated tentative schedule:
        // Stop 1 (Ueno): 09:00 - 10:00 (duration: 60 min)
        assertEquals("09:00", result.optimizedStops[0].startTime)
        assertEquals("10:00", result.optimizedStops[0].endTime)

        // Stop 2: starts at 10:00 + 20 min buffer = 10:20 (duration: 90 min) -> ends 11:50
        assertEquals("10:20", result.optimizedStops[1].startTime)
        assertEquals("11:50", result.optimizedStops[1].endTime)

        // Stop 3: starts at 11:50 + 20 min buffer = 12:10 (duration: 90 min) -> ends 13:40
        assertEquals("12:10", result.optimizedStops[2].startTime)
        assertEquals("13:40", result.optimizedStops[2].endTime)

        // Stop 4 (Shibuya): starts at 13:40 + 20 min buffer = 14:00 (duration: 90 min) -> ends 15:30
        assertEquals("14:00", result.optimizedStops[3].startTime)
        assertEquals("15:30", result.optimizedStops[3].endTime)

        // All UTC ISO strings are properly populated
        result.optimizedStops.forEach {
            assertNotNull(it.startTimeUtcIso)
            assertNotNull(it.endTimeUtcIso)
        }
    }

    @Test
    fun `Fixed time stop such as a restaurant reservation preserves its chronological slot and time`() {
        val uenoHotel = ItineraryStopItem(
            id = "stop-1",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "09:00",
            endTime = "10:00",
            title = "Hotel Ueno",
            category = StopCategory.HOTEL,
            placeNameJapanese = "上野ホテル",
            latitude = 35.7126,
            longitude = 139.7798
        )
        val shibuyaCrossing = ItineraryStopItem(
            id = "stop-2",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "10:30",
            endTime = "12:00",
            title = "Shibuya Crossing",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "渋谷",
            latitude = 35.6595,
            longitude = 139.7005
        )
        val fixedLunchAsakusa = ItineraryStopItem(
            id = "stop-3-fixed",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "13:00",
            endTime = "14:30",
            title = "Reserva Sukiyaki Asakusa Imahan",
            category = StopCategory.FOOD,
            placeNameJapanese = "浅草今半",
            latitude = 35.7148,
            longitude = 139.7967,
            isTimeFixed = true
        )
        val akihabara = ItineraryStopItem(
            id = "stop-4",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "15:00",
            endTime = "16:30",
            title = "Akihabara",
            category = StopCategory.SHOPPING,
            placeNameJapanese = "秋葉原",
            latitude = 35.6983,
            longitude = 139.7731
        )

        val originalStops = listOf(uenoHotel, shibuyaCrossing, fixedLunchAsakusa, akihabara)
        val options = RouteOptimizationOptions(
            keepFirstStopFixed = true,
            respectFixedTimeStops = true,
            defaultTransferBufferMinutes = 20
        )

        val result = optimizer.optimize(originalStops, options)

        // Slot 2 must be the fixed lunch at Asakusa
        val thirdStop = result.optimizedStops[2]
        assertEquals("stop-3-fixed", thirdStop.id)
        assertTrue(thirdStop.isTimeFixed)
        assertEquals("13:00", thirdStop.startTime)
        assertEquals("14:30", thirdStop.endTime)
        assertEquals(3, thirdStop.order)

        // Akihabara should be moved to morning (slot 1) to be near Ueno before the 13:00 lunch in Asakusa
        assertEquals("stop-akiba", "stop-4", result.optimizedStops[1].id)
        // Shibuya should be after lunch
        assertEquals("stop-shibuya", "stop-2", result.optimizedStops[3].id)

        // Distance saved must be significant (> 3000m)
        assertTrue("Distance saved: ${result.distanceSavedMeters}", result.distanceSavedMeters > 3000.0)
    }

    @Test
    fun `Stops without coordinates are preserved at their original position without throwing errors`() {
        val uenoHotel = ItineraryStopItem(
            id = "stop-1",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "09:00",
            endTime = "10:00",
            title = "Hotel Ueno",
            category = StopCategory.HOTEL,
            placeNameJapanese = "上野",
            latitude = 35.7126,
            longitude = 139.7798
        )
        val shibuyaCrossing = ItineraryStopItem(
            id = "stop-2",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "10:30",
            endTime = "12:00",
            title = "Shibuya Crossing",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "渋谷",
            latitude = 35.6595,
            longitude = 139.7005
        )
        val stopWithoutCoords = ItineraryStopItem(
            id = "stop-no-coord",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "12:30",
            endTime = "13:00",
            title = "Comprar billetes Suica en máquina",
            category = StopCategory.TRANSPORT,
            placeNameJapanese = "券売機",
            latitude = null,
            longitude = null
        )
        val asakusa = ItineraryStopItem(
            id = "stop-4",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "13:30",
            endTime = "15:00",
            title = "Asakusa",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "浅草",
            latitude = 35.7148,
            longitude = 139.7967
        )
        val akihabara = ItineraryStopItem(
            id = "stop-5",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "15:30",
            endTime = "17:00",
            title = "Akihabara",
            category = StopCategory.SHOPPING,
            placeNameJapanese = "秋葉原",
            latitude = 35.6983,
            longitude = 139.7731
        )

        val stops = listOf(uenoHotel, shibuyaCrossing, stopWithoutCoords, asakusa, akihabara)
        val result = optimizer.optimize(stops)

        // Stop without coords must stay at index 2 (third stop)
        assertEquals("stop-no-coord", result.optimizedStops[2].id)
        assertEquals(3, result.optimizedStops[2].order)
        assertEquals("12:30", result.optimizedStops[2].startTime)
        assertEquals("13:00", result.optimizedStops[2].endTime)

        // Overall optimization should still succeed and be beneficial
        assertTrue(result.isBeneficial)
    }

    @Test
    fun `Degenerate cases with fewer than 3 stops with coordinates return unchanged stops with zero savings`() {
        val stop1 = ItineraryStopItem(
            id = "s1",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "09:00",
            endTime = "10:00",
            title = "Stop 1",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "",
            latitude = 35.71,
            longitude = 139.77
        )
        val stop2 = ItineraryStopItem(
            id = "s2",
            dayNumber = 1,
            dateIso = "2026-11-12",
            startTime = "10:30",
            endTime = "11:30",
            title = "Stop 2",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "",
            latitude = 35.65,
            longitude = 139.70
        )

        // 2 stops only
        val resultTwoStops = optimizer.optimize(listOf(stop1, stop2))
        assertEquals(0.0, resultTwoStops.distanceSavedMeters, 0.001)
        assertEquals(0.0, resultTwoStops.percentageSaved, 0.001)
        assertFalse(resultTwoStops.isBeneficial)
        assertEquals(listOf("s1", "s2"), resultTwoStops.optimizedStops.map { it.id })

        // 3 stops but one has null coordinates (only 2 valid)
        val stopNoCoords = stop1.copy(id = "s3", latitude = null, longitude = null)
        val resultWithNulls = optimizer.optimize(listOf(stop1, stopNoCoords, stop2))
        assertEquals(0.0, resultWithNulls.distanceSavedMeters, 0.001)
        assertEquals(0.0, resultWithNulls.percentageSaved, 0.001)
        assertFalse(resultWithNulls.isBeneficial)

        // Empty list
        val resultEmpty = optimizer.optimize(emptyList())
        assertEquals(0.0, resultEmpty.distanceSavedMeters, 0.001)
        assertFalse(resultEmpty.isBeneficial)
    }

    @Test
    fun `Execution time for 15 stops is well below 50 milliseconds offline`() {
        // Generate 15 points across Tokyo
        val baseLat = 35.68
        val baseLng = 139.75
        val stops = (1..15).map { i ->
            ItineraryStopItem(
                id = "p-$i",
                dayNumber = 1,
                dateIso = "2026-11-12",
                startTime = String.format("%02d:00", 7 + i % 12),
                endTime = String.format("%02d:45", 7 + i % 12),
                title = "Point $i",
                category = StopCategory.SIGHTSEEING,
                placeNameJapanese = "地点 $i",
                latitude = baseLat + (i * 0.013) % 0.09,
                longitude = baseLng + (i * 0.017) % 0.09
            )
        }

        val startTime = System.currentTimeMillis()
        val result = optimizer.optimize(stops)
        val elapsed = System.currentTimeMillis() - startTime

        assertTrue("Execution took ${elapsed}ms, should be < 50ms", elapsed < 50)
        assertEquals(15, result.optimizedStops.size)
        assertEquals((1..15).toList(), result.optimizedStops.map { it.order })
    }
}
