package com.laralbir.japantraveltools.core.portability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class JsonContentDetectorTest {

    @Test
    fun `inspect detects travel plan from sample json`() {
        val planFile = File("../plans/japon-tierra-samurais-2027.json").takeIf { it.exists() }
            ?: File("plans/japon-tierra-samurais-2027.json")
        assertTrue(planFile.exists())
        val json = planFile.readText()

        val detected = JsonContentDetector.inspect(json)
        assertEquals(JsonContentType.TRAVEL_PLAN, detected.type)
        assertEquals("Japón, Tierra de Samurais 2027", detected.title)
        assertEquals(38, detected.stopCount)
        assertEquals(9, detected.transportCount)
        assertTrue(detected.summaryEs.contains("38 paradas"))
        assertTrue(detected.summaryEs.contains("9 tramos"))
    }

    @Test
    fun `inspect detects travel guides json`() {
        val guidesJson = """
            {
              "guides": [
                {
                  "id": "g1",
                  "title": "Guía de Tokio",
                  "cityRegion": "Tokyo",
                  "tags": ["Transporte"],
                  "markdownSummary": "Resumen de Tokio"
                },
                {
                  "id": "g2",
                  "title": "Guía de Kioto",
                  "cityRegion": "Kyoto",
                  "tags": ["Templos"],
                  "markdownSummary": "Resumen de Kioto"
                }
              ]
            }
        """.trimIndent()

        val detected = JsonContentDetector.inspect(guidesJson)
        assertEquals(JsonContentType.TRAVEL_GUIDES, detected.type)
        assertEquals(2, detected.guideCount)
        assertTrue(detected.summaryEs.contains("2 guías"))
    }

    @Test
    fun `inspect detects travel documents json`() {
        val docsJson = """
            {
              "documents": [
                {
                  "id": "d1",
                  "title": "Billete Madrid - Tokio",
                  "category": "FLIGHT",
                  "referenceOrNotes": "IB6801",
                  "fileUri": null,
                  "isPinned": true
                }
              ]
            }
        """.trimIndent()

        val detected = JsonContentDetector.inspect(docsJson)
        assertEquals(JsonContentType.TRAVEL_DOCUMENTS, detected.type)
        assertEquals(1, detected.documentCount)
        assertTrue(detected.summaryEs.contains("1 billetes o reservas"))
    }

    @Test
    fun `inspect detects full cloud backup bundle json`() {
        val backupJson = """
            {
              "cloudBackupVersion": 1,
              "exportTimestampMillis": 1729000000000,
              "syncData": {
                "plans": [],
                "guides": [],
                "documents": [],
                "cities": []
              }
            }
        """.trimIndent()

        val detected = JsonContentDetector.inspect(backupJson)
        assertEquals(JsonContentType.FULL_BACKUP, detected.type)
        assertTrue(detected.summaryEs.contains("Copia de seguridad completa"))
    }

    @Test
    fun `inspect detects travel plan with embedded guides and documents`() {
        val unifiedJson = """
            {
              "schemaVersion": 1,
              "tripTitle": "Japón Completo 2027",
              "itineraryStops": [
                {
                  "id": "s1",
                  "dayNumber": 1,
                  "dateIso": "2027-03-15",
                  "startTime": "09:00",
                  "endTime": "11:00",
                  "title": "Osaka Castle",
                  "category": "SIGHTSEEING",
                  "placeNameJapanese": "大阪城",
                  "latitude": 34.6873,
                  "longitude": 135.5262
                }
              ],
              "transportSegments": [
                {
                  "id": "t1",
                  "order": 1,
                  "type": "FLIGHT",
                  "operatorName": "Lufthansa",
                  "departureName": "MAD",
                  "arrivalName": "MUC",
                  "departureTimeIso": "2027-03-15T07:45",
                  "arrivalTimeIso": "2027-03-15T10:20"
                }
              ],
              "guides": [
                {
                  "id": "g1",
                  "title": "Guía de Sellos",
                  "cityRegion": "General",
                  "tags": ["Sellos"],
                  "markdownSummary": "Tips"
                }
              ],
              "documents": [
                {
                  "id": "d1",
                  "title": "Billete AVE",
                  "category": "TRANSPORT",
                  "referenceOrNotes": "Asiento 1B"
                }
              ]
            }
        """.trimIndent()

        val detected = JsonContentDetector.inspect(unifiedJson)
        assertEquals(JsonContentType.TRAVEL_PLAN, detected.type)
        assertEquals(1, detected.stopCount)
        assertEquals(1, detected.transportCount)
        assertEquals(1, detected.guideCount)
        assertEquals(1, detected.documentCount)
        assertTrue(detected.summaryEs.contains("1 guías"))
        assertTrue(detected.summaryEs.contains("1 documentos"))
    }

    @Test
    fun `inspect detects cloud backup with backupId and travelPlans`() {
        val syncBackupJson = """
            {
              "schemaVersion": 1,
              "backupId": "backup-1729000000000",
              "accountEmail": "test@example.com",
              "timestampMillis": 1729000000000,
              "deviceModel": "Pixel 10 Pro XL",
              "travelPlans": [],
              "travelGuides": [],
              "travelDocuments": [],
              "weatherCities": []
            }
        """.trimIndent()

        val detected = JsonContentDetector.inspect(syncBackupJson)
        assertEquals(JsonContentType.FULL_BACKUP, detected.type)
        assertTrue(detected.summaryEs.contains("Copia de seguridad completa"))
    }

    @Test
    fun `inspect returns unknown for invalid or empty json`() {
        val detected = JsonContentDetector.inspect("{ \"foo\": \"bar\" }")
        assertEquals(JsonContentType.UNKNOWN, detected.type)
    }
}
