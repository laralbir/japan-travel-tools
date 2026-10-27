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
        assertEquals(37, detected.stopCount)
        assertEquals(9, detected.transportCount)
        assertTrue(detected.summaryEs.contains("37 paradas"))
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
    fun `inspect returns unknown for invalid or empty json`() {
        val detected = JsonContentDetector.inspect("{ \"foo\": \"bar\" }")
        assertEquals(JsonContentType.UNKNOWN, detected.type)
    }
}
