package com.laralbir.japantraveltools.core.cloud

import com.laralbir.japantraveltools.core.portability.PortableTravelPlanBundle
import com.laralbir.japantraveltools.core.preferences.AppThemeMode
import com.laralbir.japantraveltools.core.preferences.UserPreferences
import com.laralbir.japantraveltools.feature.documents.DocumentCategory
import com.laralbir.japantraveltools.feature.documents.TravelDocumentItem
import com.laralbir.japantraveltools.feature.documents.TravelGuideArticle
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportPlannerUseCase
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import com.laralbir.japantraveltools.feature.weather.WeatherCityItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveSyncHelperTest {

    private fun samplePlan(id: String = "plan-1", title: String = "Viaje Tokio 2026"): PortableTravelPlanBundle {
        val stop = ItineraryStopItem(
            id = "stop-1",
            dayNumber = 1,
            dateIso = "2026-11-11",
            startTime = "09:00",
            endTime = "11:00",
            title = "Templo Senso-ji",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "浅草寺",
            latitude = 35.7147,
            longitude = 139.7966,
            notes = "Madrugar para fotos"
        )
        val transport = TransportPlannerUseCase.defaultSampleSegments().first().copy(
            id = "seg-1",
            departureName = "Tokyo",
            arrivalName = "Kyoto"
        )
        return PortableTravelPlanBundle(
            schemaVersion = 1,
            tripTitle = title,
            itineraryStops = listOf(stop),
            transportSegments = listOf(transport),
            id = id,
            startDateIso = "2026-11-11",
            totalDays = 7
        )
    }

    private fun sampleGuide(id: String = "guide-1", title: String = "Guía de Kioto"): TravelGuideArticle {
        return TravelGuideArticle(
            id = id,
            title = title,
            cityRegion = "Kioto",
            tags = listOf("Fushimi Inari", "Kinkaku-ji"),
            markdownSummary = "Visitar Fushimi Inari a primera hora",
            photoCount = 2,
            isPinned = true
        )
    }

    private fun sampleDocument(id: String = "doc-1", title: String = "Vuelo Madrid-Tokio"): TravelDocumentItem {
        return TravelDocumentItem(
            id = id,
            title = title,
            category = DocumentCategory.FLIGHT,
            referenceOrNotes = "Localizador QR8827 · Terminal 4S",
            isPinned = true
        )
    }

    private fun sampleCity(
        id: String = "city-nara",
        nameEs: String = "Nara",
        lat: Double = 34.6851,
        lon: Double = 135.8048
    ): WeatherCityItem {
        return WeatherCityItem(
            id = id,
            nameEs = nameEs,
            nameEn = nameEs,
            latitude = lat,
            longitude = lon,
            isDefault = false
        )
    }

    @Test
    fun `buildSyncBundle creates valid unified backup bundle`() {
        val plan = samplePlan()
        val guide = sampleGuide()
        val doc = sampleDocument()
        val city = sampleCity()
        val prefs = UserPreferences(
            themeMode = AppThemeMode.DARK,
            googleAccountEmail = "carlos.travel.japan@gmail.com",
            originZoneId = "Europe/Madrid"
        )

        val bundle = GoogleDriveSyncHelper.buildSyncBundle(
            accountEmail = "carlos.travel.japan@gmail.com",
            deviceModel = "Google Pixel 10 Pro XL",
            plans = listOf(plan),
            guides = listOf(guide),
            documents = listOf(doc),
            cities = listOf(city),
            prefs = prefs,
            timestampMillis = 1729881600000L
        )

        assertEquals(1, bundle.schemaVersion)
        assertEquals("carlos.travel.japan@gmail.com", bundle.accountEmail)
        assertEquals("Google Pixel 10 Pro XL", bundle.deviceModel)
        assertEquals(1, bundle.travelPlans.size)
        assertEquals(1, bundle.travelGuides.size)
        assertEquals(1, bundle.travelDocuments.size)
        assertEquals(1, bundle.weatherCities.size)
        assertEquals("DARK", bundle.themeMode)
        assertEquals("Europe/Madrid", bundle.originZoneId)
    }

    @Test
    fun `exportCloudBackupJson and importCloudBackupJson serialize and deserialize symmetrically`() {
        val plan = samplePlan()
        val guide = sampleGuide()
        val doc = sampleDocument()
        val city = sampleCity()
        val prefs = UserPreferences(googleAccountEmail = "carlos@gmail.com")

        val originalBundle = GoogleDriveSyncHelper.buildSyncBundle(
            accountEmail = "carlos@gmail.com",
            deviceModel = "TCL NXTPAPER 11 Plus",
            plans = listOf(plan),
            guides = listOf(guide),
            documents = listOf(doc),
            cities = listOf(city),
            prefs = prefs,
            timestampMillis = 1729882000000L
        )

        val json = GoogleDriveSyncHelper.exportCloudBackupJson(originalBundle)
        assertTrue(json.contains("\"accountEmail\": \"carlos@gmail.com\""))
        assertTrue(json.contains("\"deviceModel\": \"TCL NXTPAPER 11 Plus\""))
        assertTrue(json.contains("Templo Senso-ji"))
        assertTrue(json.contains("Guía de Kioto"))
        assertTrue(json.contains("QR8827"))

        val restoredBundle = GoogleDriveSyncHelper.importCloudBackupJson(json)
        assertNotNull(restoredBundle)
        assertEquals(originalBundle.accountEmail, restoredBundle!!.accountEmail)
        assertEquals(originalBundle.deviceModel, restoredBundle.deviceModel)
        assertEquals(1, restoredBundle.travelPlans.size)
        assertEquals("Viaje Tokio 2026", restoredBundle.travelPlans.first().tripTitle)
        assertEquals(1, restoredBundle.travelGuides.size)
        assertEquals("Guía de Kioto", restoredBundle.travelGuides.first().title)
        assertEquals(1, restoredBundle.travelDocuments.size)
        assertEquals("Vuelo Madrid-Tokio", restoredBundle.travelDocuments.first().title)
        assertEquals(1, restoredBundle.weatherCities.size)
        assertEquals("Nara", restoredBundle.weatherCities.first().nameEs)
    }

    @Test
    fun `importCloudBackupJson returns null on blank or corrupted json string`() {
        assertNull(GoogleDriveSyncHelper.importCloudBackupJson(""))
        assertNull(GoogleDriveSyncHelper.importCloudBackupJson("{ corrupted json"))
        assertNull(GoogleDriveSyncHelper.importCloudBackupJson("""{"schemaVersion": "not-a-number"}"""))
    }

    @Test
    fun `mergePlans combines plans without duplicates and preserves stops`() {
        val existingPlan = samplePlan(id = "plan-1", title = "Plan Existente")
        val newIncomingPlan = samplePlan(id = "plan-2", title = "Plan Nuevo de Tablet")
        val updatedPlan1 = existingPlan.copy(tripTitle = "Plan 1 Actualizado en Drive")

        val merged = GoogleDriveSyncHelper.mergePlans(
            existingPlans = listOf(existingPlan),
            incomingPlans = listOf(updatedPlan1, newIncomingPlan)
        )

        assertEquals(2, merged.size)
        val plan1 = merged.first { it.id == "plan-1" }
        assertEquals("Plan 1 Actualizado en Drive", plan1.tripTitle)
        assertTrue(merged.any { it.id == "plan-2" })
    }

    @Test
    fun `mergeGuides unites guides by ID without loss`() {
        val g1 = sampleGuide(id = "g-1", title = "Tokio")
        val g2 = sampleGuide(id = "g-2", title = "Kioto")
        val g1Updated = sampleGuide(id = "g-1", title = "Tokio y Yokohama")

        val merged = GoogleDriveSyncHelper.mergeGuides(
            existing = listOf(g1),
            incoming = listOf(g1Updated, g2)
        )

        assertEquals(2, merged.size)
        assertEquals("Tokio y Yokohama", merged.first { it.id == "g-1" }.title)
        assertEquals("Kioto", merged.first { it.id == "g-2" }.title)
    }

    @Test
    fun `mergeDocuments unites bookings and tickets by ID`() {
        val d1 = sampleDocument(id = "d-1", title = "Vuelo")
        val d2 = sampleDocument(id = "d-2", title = "Hotel Shinjuku")

        val merged = GoogleDriveSyncHelper.mergeDocuments(
            existing = listOf(d1),
            incoming = listOf(d2)
        )

        assertEquals(2, merged.size)
        assertTrue(merged.any { it.id == "d-1" })
        assertTrue(merged.any { it.id == "d-2" })
    }

    @Test
    fun `mergeWeatherCities merges destination cities without duplicating coordinates`() {
        val c1 = sampleCity(id = "c-1", nameEs = "Nara", lat = 34.6851, lon = 135.8048)
        val c2 = sampleCity(id = "c-2", nameEs = "Kamakura", lat = 35.3192, lon = 139.5467)

        val merged = GoogleDriveSyncHelper.mergeWeatherCities(
            existing = listOf(c1),
            incoming = listOf(c1, c2)
        )

        assertEquals(2, merged.size)
        assertTrue(merged.any { it.nameEs == "Nara" })
        assertTrue(merged.any { it.nameEs == "Kamakura" })
    }

    @Test
    fun `formatSyncTime formats timestamps accurately in ES and EN`() {
        val neverEs = GoogleDriveSyncHelper.formatSyncTime(null, isEs = true)
        assertEquals("Sin sincronizar", neverEs)

        val neverEn = GoogleDriveSyncHelper.formatSyncTime(null, isEs = false)
        assertEquals("Not synced yet", neverEn)

        val formattedEs = GoogleDriveSyncHelper.formatSyncTime(1729881600000L, isEs = true)
        assertNotNull(formattedEs)
        assertTrue(formattedEs.isNotBlank())
    }

    @Test
    fun `getCloudBackupFileName produces clean and safe file name`() {
        val fileName = GoogleDriveSyncHelper.getCloudBackupFileName("carlos.travel.japan@gmail.com")
        assertEquals("japan-travel-backup-carlos.travel.japan.json", fileName)

        val fallback = GoogleDriveSyncHelper.getCloudBackupFileName("")
        assertEquals("japan-travel-backup.json", fallback)
    }

    @Test
    fun `GoogleAccountSelectorHelper validates and masks account email properly`() {
        assertTrue(GoogleAccountSelectorHelper.isValidGoogleEmail("carlos@gmail.com"))
        assertTrue(GoogleAccountSelectorHelper.isValidGoogleEmail("user.travel@googlemail.com"))
        assertFalse(GoogleAccountSelectorHelper.isValidGoogleEmail("invalid"))
        assertFalse(GoogleAccountSelectorHelper.isValidGoogleEmail(""))

        assertEquals("c***s@gmail.com", GoogleAccountSelectorHelper.maskAccountEmail("carlos@gmail.com"))
        assertEquals("u***r@domain.com", GoogleAccountSelectorHelper.maskAccountEmail("user@domain.com"))
        assertEquals("t***t@test.com", GoogleAccountSelectorHelper.maskAccountEmail("test@test.com"))
        assertEquals("a***@test.com", GoogleAccountSelectorHelper.maskAccountEmail("ab@test.com"))
        assertEquals("invalid-email", GoogleAccountSelectorHelper.maskAccountEmail("invalid-email"))
    }
}

