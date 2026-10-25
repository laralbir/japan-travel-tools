package com.laralbir.japantraveltools.core.portability

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.planner.TripPlannerUseCase
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelPlanJsonPortabilityTest {

    @Test
    fun `exportToJson and importFromJson preserve itinerary stops and multimodal transport segments`() {
        val stops = listOf(
            ItineraryStopItem(
                id = "stop-10",
                dayNumber = 1,
                dateIso = "2026-11-11",
                startTime = "09:00",
                endTime = "11:00",
                title = "Meiji Jingu Shrine",
                category = StopCategory.SIGHTSEEING,
                placeNameJapanese = "明治神宮",
                latitude = 35.6764,
                longitude = 139.6993,
                notes = "Harajuku Exit"
            )
        )
        val segments = listOf(
            TransportSegmentItem(
                id = "seg-99",
                order = 1,
                type = TransportMode.TRAIN_SHINKANSEN,
                operatorName = "JR Central",
                lineOrNumber = "Hikari #509",
                departureCode = "TYO",
                departureName = "Tokyo Station",
                departureZoneId = "Asia/Tokyo",
                departureTerminalOrPlatform = "Track 17",
                checkInTime = "2026-11-14T08:10",
                boardingTime = "2026-11-14T08:25",
                departureTimeIso = "2026-11-14T08:33",
                arrivalCode = "KYO",
                arrivalName = "Kyoto Station",
                arrivalZoneId = "Asia/Tokyo",
                arrivalTerminalOrPlatform = "Track 14",
                arrivalTimeIso = "2026-11-14T11:10",
                seatOrCarInfo = "Car 5 Seat 4E",
                stationExitOrBelt = "Central Exit",
                passOrPaymentBadge = "JR Pass"
            )
        )

        val bundle = PortableTravelPlanBundle(
            schemaVersion = 1,
            tripTitle = "Japón Otoño 2026",
            itineraryStops = stops,
            transportSegments = segments
        )

        val jsonText = TravelPlanJsonCodec.exportToPrettyJson(bundle)
        assertTrue(jsonText.contains("\"schemaVersion\": 1"))
        assertTrue(jsonText.contains("\"Meiji Jingu Shrine\""))
        assertTrue(jsonText.contains("\"Hikari #509\""))

        val imported = TravelPlanJsonCodec.importFromJson(jsonText)
        assertEquals("Japón Otoño 2026", imported.tripTitle)
        assertEquals(1, imported.itineraryStops.size)
        assertEquals("Meiji Jingu Shrine", imported.itineraryStops.first().title)
        assertEquals("明治神宮", imported.itineraryStops.first().placeNameJapanese)
        assertEquals(1, imported.transportSegments.size)
        assertEquals("Hikari #509", imported.transportSegments.first().lineOrNumber)
        assertEquals(TransportMode.TRAIN_SHINKANSEN, imported.transportSegments.first().type)
    }

    @Test
    fun `AiPlanBuilderAndOptimizer resolves schedule conflicts and adds new AI itinerary and transport`() {
        val overlappingStops = listOf(
            ItineraryStopItem(
                id = "s1",
                dayNumber = 1,
                dateIso = "2026-11-11",
                startTime = "09:00",
                endTime = "11:00",
                title = "Senso-ji Temple",
                category = StopCategory.SIGHTSEEING,
                placeNameJapanese = "浅草寺",
                latitude = 35.7147,
                longitude = 139.7966
            ),
            ItineraryStopItem(
                id = "s2",
                dayNumber = 1,
                dateIso = "2026-11-11",
                startTime = "10:15",
                endTime = "12:00",
                title = "Tokyo Skytree",
                category = StopCategory.ACTIVITY,
                placeNameJapanese = "東京スカイツリー",
                latitude = 35.7100,
                longitude = 139.8107
            )
        )

        // Verify initial conflict exists
        val initialAnalysis = TripPlannerUseCase.sortAndDetectConflicts(overlappingStops)
        assertTrue(initialAnalysis.any { it.hasTimeConflict })

        // Let AI optimize and expand the plan (e.g., adding Day 3 Osaka + Shinkansen)
        val initialBundle = PortableTravelPlanBundle(
            schemaVersion = 1,
            tripTitle = "Japón 2026",
            itineraryStops = overlappingStops,
            transportSegments = emptyList()
        )
        val improvedBundle = AiPlanBuilderEngine.improveAndExpandPlan(
            currentPlan = initialBundle,
            userInstruction = "Resolver solapamientos horarios y crear Día 3 en Osaka con Shinkansen"
        )

        // Verify conflicts in Day 1 are resolved!
        val day1AfterAi = TripPlannerUseCase.sortAndDetectConflicts(
            improvedBundle.itineraryStops.filter { it.dayNumber == 1 }
        )
        assertFalse(day1AfterAi.any { it.hasTimeConflict })

        // Verify AI created Day 3 stops and transport segment
        assertTrue(improvedBundle.itineraryStops.any { it.dayNumber == 3 })
        assertTrue(improvedBundle.transportSegments.isNotEmpty())
    }

    @Test
    fun `AiPlanBuilderEngine respects specific city requests like Nara or Hakone and does not shift non-overlapping stops`() {
        val cleanStop = ItineraryStopItem(
            id = "s1",
            dayNumber = 1,
            dateIso = "2026-11-11",
            startTime = "09:00",
            endTime = "10:30",
            title = "Senso-ji",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "浅草寺",
            latitude = 35.7147,
            longitude = 139.7966
        )
        val cleanStop2 = ItineraryStopItem(
            id = "s2",
            dayNumber = 1,
            dateIso = "2026-11-11",
            startTime = "10:45",
            endTime = "12:00",
            title = "Asakusa Culture Center",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "浅草文化観光センター",
            latitude = 35.7126,
            longitude = 139.7963
        )
        val initial = PortableTravelPlanBundle(
            tripTitle = "Mi Viaje",
            itineraryStops = listOf(cleanStop, cleanStop2),
            transportSegments = emptyList()
        )

        val withNara = AiPlanBuilderEngine.improveAndExpandPlan(
            currentPlan = initial,
            userInstruction = "Añadir excursión a Nara y ciervos"
        )
        // Non-overlapping stop s2 must keep its exact 10:45 start time!
        val afterS2 = withNara.itineraryStops.first { it.id == "s2" }
        assertEquals("10:45", afterS2.startTime)
        // And Nara stops must be added
        assertTrue(withNara.itineraryStops.any { it.title.contains("Nara", ignoreCase = true) || it.title.contains("Todai-ji", ignoreCase = true) })
    }

    @Test
    fun `SharedTravelPlanStore supports creating editing and deleting stops days and transport segments`() {
        SharedTravelPlanStore.clearAll()
        assertTrue(SharedTravelPlanStore.planState.value.itineraryStops.isEmpty())
        assertTrue(SharedTravelPlanStore.planState.value.transportSegments.isEmpty())

        val newStop = ItineraryStopItem(
            id = "custom-stop-1",
            dayNumber = 1,
            dateIso = "2026-11-11",
            startTime = "10:00",
            endTime = "11:30",
            title = "Shibuya Sky",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "渋谷スカイ",
            latitude = 35.6580,
            longitude = 139.7016,
            notes = "Reserva 10:00"
        )
        SharedTravelPlanStore.upsertStop(newStop)
        assertEquals(1, SharedTravelPlanStore.planState.value.itineraryStops.size)
        assertEquals("Shibuya Sky", SharedTravelPlanStore.planState.value.itineraryStops.first().title)

        // Edit existing stop
        val editedStop = newStop.copy(title = "Shibuya Sky Sunset", startTime = "16:30", endTime = "18:00")
        SharedTravelPlanStore.upsertStop(editedStop)
        assertEquals(1, SharedTravelPlanStore.planState.value.itineraryStops.size)
        assertEquals("Shibuya Sky Sunset", SharedTravelPlanStore.planState.value.itineraryStops.first().title)
        assertEquals("16:30", SharedTravelPlanStore.planState.value.itineraryStops.first().startTime)

        // Delete stop
        SharedTravelPlanStore.deleteStop("custom-stop-1")
        assertTrue(SharedTravelPlanStore.planState.value.itineraryStops.isEmpty())

        // Restore defaults for other tests
        SharedTravelPlanStore.resetToDefaults()
    }
}
