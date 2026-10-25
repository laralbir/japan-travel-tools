package com.laralbir.japantraveltools.feature.planner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlannerUseCaseTest {

    @Test
    fun `DetectScheduleConflictsUseCase sorts items chronologically and flags overlapping schedules`() {
        val items = listOf(
            ItineraryStopItem(
                id = "b",
                dayNumber = 1,
                dateIso = "2026-11-10",
                startTime = "10:30",
                endTime = "12:00",
                title = "Nakamise Shopping Street & Ramen",
                category = StopCategory.FOOD,
                placeNameJapanese = "仲見世通り",
                latitude = 35.7119,
                longitude = 139.7964
            ),
            ItineraryStopItem(
                id = "a",
                dayNumber = 1,
                dateIso = "2026-11-10",
                startTime = "09:00",
                endTime = "11:00",
                title = "Senso-ji Temple Asakusa",
                category = StopCategory.SIGHTSEEING,
                placeNameJapanese = "浅草寺",
                latitude = 35.7147,
                longitude = 139.7966
            ),
            ItineraryStopItem(
                id = "c",
                dayNumber = 1,
                dateIso = "2026-11-10",
                startTime = "13:00",
                endTime = "15:00",
                title = "Tokyo Skytree Observation Deck",
                category = StopCategory.ACTIVITY,
                placeNameJapanese = "東京スカイツリー",
                latitude = 35.7100,
                longitude = 139.8107
            )
        )

        val analyzed = TripPlannerUseCase.sortAndDetectConflicts(items)
        // Sorted chronologically: 09:00 ("a"), 10:30 ("b"), 13:00 ("c")
        assertEquals(listOf("a", "b", "c"), analyzed.map { it.item.id })
        // "a" (09:00-11:00) and "b" (10:30-12:00) overlap!
        assertTrue(analyzed[0].hasTimeConflict)
        assertTrue(analyzed[1].hasTimeConflict)
        // "c" (13:00-15:00) does not overlap
        assertEquals(false, analyzed[2].hasTimeConflict)
    }

    @Test
    fun `ParseGoogleMapsUrlUseCase extracts coordinates from standard Google Maps URLs`() {
        val url = "https://www.google.com/maps/place/Senso-ji/@35.7147651,139.7966553,17z"
        val coords = TripPlannerUseCase.extractCoordinatesFromGoogleMapsUrl(url)
        assertNotNull(coords)
        assertEquals(35.7147651, coords!!.first, 0.0001)
        assertEquals(139.7966553, coords.second, 0.0001)
    }
}

