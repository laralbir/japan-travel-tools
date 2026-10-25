package com.laralbir.japantraveltools.feature.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentsAndGuidesUseCaseTest {

    @Test
    fun `filters documents by category and search query keeping pinned items first`() {
        val docs = listOf(
            TravelDocumentItem(
                id = "1",
                title = "Hotel Gracery Shinjuku Godzilla",
                category = DocumentCategory.HOTEL,
                referenceOrNotes = "Check-in 15:00 · Kabukicho",
                isPinned = false
            ),
            TravelDocumentItem(
                id = "2",
                title = "Pasaporte Principal",
                category = DocumentCategory.PASSPORT,
                referenceOrNotes = "EXP 2031",
                isPinned = true
            ),
            TravelDocumentItem(
                id = "3",
                title = "JR Pass 14 Days Voucher",
                category = DocumentCategory.TRANSPORT,
                referenceOrNotes = "Exchange at Narita T1",
                isPinned = true
            )
        )

        val filteredAll = DocumentsFilterUseCase.filterDocuments(
            documents = docs,
            selectedCategory = null,
            searchQuery = ""
        )
        // Pinned items come first
        assertTrue(filteredAll.first().isPinned)

        val shinjukuSearch = DocumentsFilterUseCase.filterDocuments(
            documents = docs,
            selectedCategory = DocumentCategory.HOTEL,
            searchQuery = "shinjuku"
        )
        assertEquals(1, shinjukuSearch.size)
        assertEquals("Hotel Gracery Shinjuku Godzilla", shinjukuSearch.first().title)
    }

    @Test
    fun `filters travel guides by city region and tags`() {
        val guides = DefaultTravelGuidesCatalog.initialGuides()
        val kyotoGuides = DocumentsFilterUseCase.filterGuides(
            guides = guides,
            cityFilter = "Kyoto",
            searchQuery = ""
        )
        assertTrue(kyotoGuides.isNotEmpty())
        assertTrue(kyotoGuides.all { it.cityRegion.equals("Kyoto", ignoreCase = true) })
    }

    @Test
    fun `DocumentsAndGuidesJsonCodec correctly serializes and deserializes travel documents with attachments`() {
        val docs = listOf(
            TravelDocumentItem(
                id = "doc-1",
                title = "Entrada USJ Super Nintendo World",
                category = DocumentCategory.TICKET,
                referenceOrNotes = "Entrada 10:30 AM · QR Express Pass",
                fileUri = "/data/user/0/app/files/documents/doc_usj.pdf",
                isPinned = true
            ),
            TravelDocumentItem(
                id = "doc-2",
                title = "Vuelo Madrid - Tokyo Haneda",
                category = DocumentCategory.FLIGHT,
                referenceOrNotes = "IB6801 · Localizador XYZ123",
                fileUri = null,
                isPinned = false
            )
        )

        val json = DocumentsAndGuidesJsonCodec.exportDocumentsJson(docs)
        assertTrue(json.contains("Entrada USJ Super Nintendo World"))
        assertTrue(json.contains("doc_usj.pdf"))
        assertTrue(json.contains("IB6801"))

        val imported = DocumentsAndGuidesJsonCodec.importDocumentsJson(json)
        assertEquals(2, imported.size)
        assertEquals("doc-1", imported[0].id)
        assertEquals("Entrada USJ Super Nintendo World", imported[0].title)
        assertEquals(DocumentCategory.TICKET, imported[0].category)
        assertEquals("/data/user/0/app/files/documents/doc_usj.pdf", imported[0].fileUri)
        assertTrue(imported[0].isPinned)
        assertEquals("doc-2", imported[1].id)
        assertFalse(imported[1].isPinned)
    }

    @Test
    fun `DocumentsAndGuidesJsonCodec correctly serializes and deserializes travel guides`() {
        val guides = listOf(
            TravelGuideArticle(
                id = "guide-1",
                title = "Ruta de Templos Ocultos en Kioto",
                cityRegion = "Kyoto",
                tags = listOf("Templos", "Fotografía", "Silencio"),
                markdownSummary = "Visita Otagi Nenbutsu-ji en Arashiyama con cientos de estatuas sonrientes."
            )
        )

        val json = DocumentsAndGuidesJsonCodec.exportGuidesJson(guides)
        assertTrue(json.contains("Ruta de Templos Ocultos en Kioto"))
        assertTrue(json.contains("Otagi Nenbutsu-ji"))

        val imported = DocumentsAndGuidesJsonCodec.importGuidesJson(json)
        assertEquals(1, imported.size)
        assertEquals("guide-1", imported[0].id)
        assertEquals("Kyoto", imported[0].cityRegion)
        assertEquals(3, imported[0].tags.size)
        assertTrue(imported[0].markdownSummary.contains("Otagi Nenbutsu-ji"))
    }
}

