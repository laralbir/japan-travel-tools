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
    fun `sorts travel guides by title, region or pinned first`() {
        val guides = listOf(
            TravelGuideArticle(id = "1", title = "Zoos and Parks in Ueno", cityRegion = "Tokyo", tags = emptyList(), markdownSummary = "", isPinned = false),
            TravelGuideArticle(id = "2", title = "Arashiyama Bamboo Grove", cityRegion = "Kyoto", tags = emptyList(), markdownSummary = "", isPinned = true),
            TravelGuideArticle(id = "3", title = "Dotonbori Street Food", cityRegion = "Osaka", tags = emptyList(), markdownSummary = "", isPinned = false)
        )

        val pinnedFirst = DocumentsFilterUseCase.filterGuides(guides, null, "", GuideSortOrder.PINNED_FIRST)
        assertEquals("Arashiyama Bamboo Grove", pinnedFirst.first().title)

        val titleAz = DocumentsFilterUseCase.filterGuides(guides, null, "", GuideSortOrder.TITLE_AZ)
        assertEquals("Arashiyama Bamboo Grove", titleAz[0].title)
        assertEquals("Dotonbori Street Food", titleAz[1].title)
        assertEquals("Zoos and Parks in Ueno", titleAz[2].title)

        val byRegion = DocumentsFilterUseCase.filterGuides(guides, null, "", GuideSortOrder.CITY_REGION)
        assertEquals("Kyoto", byRegion[0].cityRegion)
        assertEquals("Osaka", byRegion[1].cityRegion)
        assertEquals("Tokyo", byRegion[2].cityRegion)
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

    @Test
    fun `DocumentsAndGuidesJsonCodec correctly serializes and deserializes custom ID documents`() {
        val customDocs = listOf(
            CustomIdDocumentItem(
                id = "custom-1",
                title = "Certificado de Vacunación Fiebre Amarilla",
                subtitle = "Válido internacionalmente · Hospital Carlos III",
                fileName = "custom_doc_1.jpg"
            ),
            CustomIdDocumentItem(
                id = "custom-2",
                title = "Receta Médica - Antihistamínicos",
                subtitle = "Prescripción Dr. Gómez 2026",
                fileName = "custom_doc_2.jpg"
            )
        )

        val json = DocumentsAndGuidesJsonCodec.exportCustomIdDocsJson(customDocs)
        assertTrue(json.contains("Certificado de Vacunación Fiebre Amarilla"))
        assertTrue(json.contains("custom_doc_1.jpg"))
        assertTrue(json.contains("Receta Médica"))

        val imported = DocumentsAndGuidesJsonCodec.importCustomIdDocsJson(json)
        assertEquals(2, imported.size)
        assertEquals("custom-1", imported[0].id)
        assertEquals("Certificado de Vacunación Fiebre Amarilla", imported[0].title)
        assertEquals("Válido internacionalmente · Hospital Carlos III", imported[0].subtitle)
        assertEquals("custom_doc_1.jpg", imported[0].fileName)

        assertEquals("custom-2", imported[1].id)
        assertEquals("Receta Médica - Antihistamínicos", imported[1].title)
    }

    @Test
    fun `SharedDocumentsAndGuidesStore manages custom ID documents correctly`() {
        val initialSize = SharedDocumentsAndGuidesStore.customIdDocsState.value.size

        val newDoc = CustomIdDocumentItem(
            id = "test-doc-${System.currentTimeMillis()}",
            title = "Carnet de Conducir Internacional",
            subtitle = "Permiso DGT Ginebra 1949",
            fileName = "custom_permit.jpg"
        )

        SharedDocumentsAndGuidesStore.upsertCustomIdDoc(newDoc)
        assertTrue(SharedDocumentsAndGuidesStore.customIdDocsState.value.any { it.id == newDoc.id })

        val updated = newDoc.copy(subtitle = "Permiso actualizado 2026")
        SharedDocumentsAndGuidesStore.upsertCustomIdDoc(updated)
        val found = SharedDocumentsAndGuidesStore.customIdDocsState.value.first { it.id == newDoc.id }
        assertEquals("Permiso actualizado 2026", found.subtitle)

        SharedDocumentsAndGuidesStore.deleteCustomIdDoc(newDoc.id)
        assertFalse(SharedDocumentsAndGuidesStore.customIdDocsState.value.any { it.id == newDoc.id })
    }

    @Test
    fun `CustomIdDocumentItem identifies pdf extensions accurately`() {
        val pdfDoc = CustomIdDocumentItem(
            id = "pdf-1",
            title = "Certificado Vacunación",
            fileName = "custom_doc_vaccine.pdf"
        )
        val imageDoc = CustomIdDocumentItem(
            id = "img-1",
            title = "Pasaporte",
            fileName = "custom_doc_pass.jpg"
        )
        val pngDoc = CustomIdDocumentItem(
            id = "img-2",
            title = "Permiso",
            fileName = "custom_doc_permit.PNG"
        )

        assertTrue(pdfDoc.isPdf)
        assertFalse(imageDoc.isPdf)
        assertFalse(pngDoc.isPdf)
    }

    @Test
    fun `TravelDocumentItem supports locatorCode serialization and deserialization`() {
        val doc = TravelDocumentItem(
            id = "doc-ticket-smart-ex",
            title = "Shinkansen Nozomi 45 Tokyo - Shin-Osaka",
            category = DocumentCategory.TRANSPORT,
            referenceOrNotes = "Seat 12D Car 5 · SmartEX QR",
            locatorCode = "EX-JPN-8890-412"
        )
        val json = DocumentsAndGuidesJsonCodec.exportDocumentsJson(listOf(doc))
        assertTrue(json.contains("EX-JPN-8890-412"))

        val imported = DocumentsAndGuidesJsonCodec.importDocumentsJson(json)
        assertEquals(1, imported.size)
        assertEquals("EX-JPN-8890-412", imported[0].locatorCode)
    }

    @Test
    fun `SharedDocumentsAndGuidesStore handles PDF and image files for Passport and DNI slots`() {
        val tempDir = java.nio.file.Files.createTempDirectory("id_vault_test").toFile()
        try {
            val slotKey = "passport_data_page"

            // 1. Initial state: slot is empty
            var file = SharedDocumentsAndGuidesStore.getIdDocumentFile(tempDir, slotKey)
            org.junit.Assert.assertNull(file)

            // 2. Save a PDF document
            val pdfContent = "%PDF-1.4 sample passport PDF content".toByteArray()
            val savedPdf = SharedDocumentsAndGuidesStore.saveIdDocument(
                vaultDir = tempDir,
                sourceBytes = pdfContent,
                slotKey = slotKey,
                extension = "pdf"
            )
            org.junit.Assert.assertNotNull(savedPdf)
            org.junit.Assert.assertTrue(savedPdf.exists())
            org.junit.Assert.assertEquals("passport_data_page.pdf", savedPdf.name)
            org.junit.Assert.assertTrue(SharedDocumentsAndGuidesStore.isPdfDocumentFile(savedPdf))

            // Check resolving by slot key
            file = SharedDocumentsAndGuidesStore.getIdDocumentFile(tempDir, slotKey)
            org.junit.Assert.assertNotNull(file)
            org.junit.Assert.assertEquals("passport_data_page.pdf", file?.name)

            // 3. Replace PDF with a JPG photo
            val jpgContent = "sample image bytes".toByteArray()
            val savedJpg = SharedDocumentsAndGuidesStore.saveIdDocument(
                vaultDir = tempDir,
                sourceBytes = jpgContent,
                slotKey = slotKey,
                extension = "jpg"
            )
            org.junit.Assert.assertNotNull(savedJpg)
            org.junit.Assert.assertEquals("passport_data_page.jpg", savedJpg.name)
            org.junit.Assert.assertFalse(SharedDocumentsAndGuidesStore.isPdfDocumentFile(savedJpg))

            // Check that old PDF was cleaned up so only 1 file exists for that slot
            file = SharedDocumentsAndGuidesStore.getIdDocumentFile(tempDir, slotKey)
            org.junit.Assert.assertEquals("passport_data_page.jpg", file?.name)
            org.junit.Assert.assertFalse(java.io.File(tempDir, "passport_data_page.pdf").exists())

            // 4. Delete document
            val deleted = SharedDocumentsAndGuidesStore.deleteIdDocument(tempDir, slotKey)
            org.junit.Assert.assertTrue(deleted)
            org.junit.Assert.assertNull(SharedDocumentsAndGuidesStore.getIdDocumentFile(tempDir, slotKey))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `detectFileFormatByMagicBytes accurately detects PDF, JPEG, and PNG headers`() {
        val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x35) // %PDF-1.5
        val jpgBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10)
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val randomBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        assertEquals("pdf", SharedDocumentsAndGuidesStore.detectFileFormatByMagicBytes(pdfBytes))
        assertEquals("jpg", SharedDocumentsAndGuidesStore.detectFileFormatByMagicBytes(jpgBytes))
        assertEquals("png", SharedDocumentsAndGuidesStore.detectFileFormatByMagicBytes(pngBytes))
        org.junit.Assert.assertNull(SharedDocumentsAndGuidesStore.detectFileFormatByMagicBytes(randomBytes))
    }

    @Test
    fun `saveIdDocument saves PDF slot correctly even if originalName is empty or misleading`() {
        val tempDir = java.nio.file.Files.createTempDirectory("id_vault_magic_test").toFile()
        try {
            val slotKey = "dni_front"
            val pdfContent = "%PDF-1.7 confidential ID document bytes".toByteArray()

            // Source bytes has PDF magic bytes, but suggestedName has no extension
            val savedDoc = SharedDocumentsAndGuidesStore.saveIdDocumentBytesWithAutoDetect(
                vaultDir = tempDir,
                sourceBytes = pdfContent,
                slotKey = slotKey,
                suggestedName = "content_123456"
            )

            org.junit.Assert.assertNotNull(savedDoc)
            assertEquals("dni_front.pdf", savedDoc.name)
            assertTrue(SharedDocumentsAndGuidesStore.isPdfDocumentFile(savedDoc))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}


