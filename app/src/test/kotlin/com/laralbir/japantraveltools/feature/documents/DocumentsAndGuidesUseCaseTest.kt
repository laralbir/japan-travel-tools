package com.laralbir.japantraveltools.feature.documents

import org.junit.Assert.assertEquals
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
}

