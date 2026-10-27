package com.laralbir.japantraveltools.core.portability

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.planner.TripPlannerUseCase
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @Test
    fun `SharedTravelPlanStore supports creating new travel plans, switching plans, editing metadata and deleting plans`() {
        SharedTravelPlanStore.resetToDefaults()
        val initialPlanCount = SharedTravelPlanStore.allPlansState.value.size
        assertTrue(initialPlanCount >= 1)

        // 1. Create a brand new travel plan
        val newPlan = SharedTravelPlanStore.createPlan(
            title = "Viaje a Kioto y Alpes 2027",
            startDateIso = "2027-04-10",
            daysCount = 5
        )
        assertEquals("Viaje a Kioto y Alpes 2027", newPlan.tripTitle)
        assertEquals("2027-04-10", newPlan.startDateIso)
        assertEquals(5, newPlan.totalDays)
        assertEquals(initialPlanCount + 1, SharedTravelPlanStore.allPlansState.value.size)
        assertEquals(newPlan.id, SharedTravelPlanStore.planState.value.id)

        // 2. Edit metadata of the active plan
        SharedTravelPlanStore.editCurrentPlan(
            newTitle = "Viaje a Kioto, Takayama y Shirakawago",
            newStartDateIso = "2027-04-12",
            newTotalDays = 7
        )
        assertEquals("Viaje a Kioto, Takayama y Shirakawago", SharedTravelPlanStore.planState.value.tripTitle)
        assertEquals("2027-04-12", SharedTravelPlanStore.planState.value.startDateIso)
        assertEquals(7, SharedTravelPlanStore.planState.value.totalDays)

        // 3. Switch back to first plan
        val firstPlan = SharedTravelPlanStore.allPlansState.value.last()
        SharedTravelPlanStore.switchPlan(firstPlan.id)
        assertEquals(firstPlan.id, SharedTravelPlanStore.planState.value.id)

        // 4. Switch to new plan and delete it
        SharedTravelPlanStore.switchPlan(newPlan.id)
        SharedTravelPlanStore.deleteCurrentPlan()
        assertEquals(initialPlanCount, SharedTravelPlanStore.allPlansState.value.size)
        assertFalse(SharedTravelPlanStore.allPlansState.value.any { it.id == newPlan.id })

        SharedTravelPlanStore.resetToDefaults()
    }

    @Test
    fun `TravelPlanJsonCodec serializes and deserializes multi-plan collections with active plan tracking`() {
        val plan1 = PortableTravelPlanBundle(
            id = "plan-1",
            tripTitle = "Plan Tokio 2026",
            startDateIso = "2026-11-01",
            totalDays = 7,
            itineraryStops = emptyList(),
            transportSegments = emptyList()
        )
        val plan2 = PortableTravelPlanBundle(
            id = "plan-2",
            tripTitle = "Plan Kioto 2027",
            startDateIso = "2027-03-20",
            totalDays = 5,
            itineraryStops = emptyList(),
            transportSegments = emptyList()
        )

        val json = TravelPlanJsonCodec.exportMultiPlanJson(listOf(plan1, plan2), activePlanId = "plan-2")
        assertTrue(json.contains("\"activePlanId\": \"plan-2\""))
        assertTrue(json.contains("Plan Tokio 2026"))
        assertTrue(json.contains("Plan Kioto 2027"))

        val (importedPlans, activeId) = TravelPlanJsonCodec.importMultiPlanJson(json)
        assertEquals(2, importedPlans.size)
        assertEquals("plan-2", activeId)
        assertEquals("Plan Tokio 2026", importedPlans[0].tripTitle)
        assertEquals("Plan Kioto 2027", importedPlans[1].tripTitle)
    }

    @Test
    fun `TravelPlanJsonCodec serializes and deserializes timeZoneId and UTC ISO times`() {
        val stop = ItineraryStopItem(
            id = "stop-muc-1",
            dayNumber = 1,
            dateIso = "2027-03-15",
            startTime = "10:00",
            endTime = "12:25",
            title = "Aeropuerto Múnich T2",
            category = StopCategory.TRANSPORT,
            placeNameJapanese = "ミュンヘン国際空港",
            latitude = 48.3537,
            longitude = 11.775,
            notes = "Escala hacia Osaka",
            timeZoneId = "Europe/Berlin",
            startTimeUtcIso = "2027-03-15T09:00:00Z",
            endTimeUtcIso = "2027-03-15T11:25:00Z"
        )
        val segment = TransportSegmentItem(
            id = "seg-lh742",
            order = 1,
            type = TransportMode.FLIGHT,
            operatorName = "Lufthansa",
            lineOrNumber = "LH 742",
            departureCode = "MUC",
            departureName = "Múnich",
            departureZoneId = "Europe/Berlin",
            departureTerminalOrPlatform = "T2",
            departureTimeIso = "2027-03-15T12:25",
            arrivalCode = "KIX",
            arrivalName = "Osaka Kansai",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "T1",
            arrivalTimeIso = "2027-03-16T07:55",
            seatOrCarInfo = "Airbus A350",
            departureTimeUtcIso = "2027-03-15T11:25:00Z",
            arrivalTimeUtcIso = "2027-03-15T22:55:00Z"
        )
        val bundle = PortableTravelPlanBundle(
            schemaVersion = 1,
            tripTitle = "Japón 2027",
            itineraryStops = listOf(stop),
            transportSegments = listOf(segment)
        )

        val json = TravelPlanJsonCodec.exportToPrettyJson(bundle)
        assertTrue(json.contains("\"timeZoneId\": \"Europe/Berlin\""))
        assertTrue(json.contains("\"startTimeUtcIso\": \"2027-03-15T09:00:00Z\""))
        assertTrue(json.contains("\"departureTimeUtcIso\": \"2027-03-15T11:25:00Z\""))

        val parsed = TravelPlanJsonCodec.importFromJson(json)
        val importedStop = parsed.itineraryStops.first()
        assertEquals("Europe/Berlin", importedStop.timeZoneId)
        assertEquals("2027-03-15T09:00:00Z", importedStop.startTimeUtcIso)
        assertEquals("2027-03-15T11:25:00Z", importedStop.endTimeUtcIso)

        val importedSeg = parsed.transportSegments.first()
        assertEquals("2027-03-15T11:25:00Z", importedSeg.departureTimeUtcIso)
        assertEquals("2027-03-15T22:55:00Z", importedSeg.arrivalTimeUtcIso)
    }

    @Test
    fun `TravelPlanJsonCodec imports sample plan file without breaking and infers timezones and UTC ISO`() {
        val planFile = java.io.File("../plans/japon-tierra-samurais-2027.json").takeIf { it.exists() }
            ?: java.io.File("plans/japon-tierra-samurais-2027.json")
        assertTrue("Sample plan file must exist", planFile.exists())

        val planJson = planFile.readText()
        val parsed = TravelPlanJsonCodec.importFromJson(planJson)

        assertEquals("Japón, Tierra de Samurais 2027", parsed.tripTitle)
        assertEquals(37, parsed.itineraryStops.size)
        assertEquals(9, parsed.transportSegments.size)

        // Stop 1 is Madrid Airport: should infer or contain Europe/Madrid and UTC ISO
        val madStop = parsed.itineraryStops[0]
        assertEquals("Europe/Madrid", madStop.timeZoneId)
        assertNotNull(madStop.startTimeUtcIso)
        assertTrue("Madrid UTC time should end in Z", madStop.startTimeUtcIso!!.endsWith("Z"))

        // Stop 2 is Munich Airport: should infer or contain Europe/Berlin and UTC ISO
        val mucStop = parsed.itineraryStops[1]
        assertEquals("Europe/Berlin", mucStop.timeZoneId)
        assertNotNull(mucStop.startTimeUtcIso)

        // Stop 3 is Osaka KIX: should infer or contain Asia/Tokyo
        val kixStop = parsed.itineraryStops[2]
        assertEquals("Asia/Tokyo", kixStop.timeZoneId)

        // Flight segment 1: MAD -> MUC should calculate departureTimeUtcIso and arrivalTimeUtcIso
        val seg1 = parsed.transportSegments[0]
        assertNotNull(seg1.departureTimeUtcIso)
        assertEquals("2027-03-15T06:25:00Z", seg1.departureTimeUtcIso) // 07:25 CET in Madrid is 06:25 UTC
    }
}
