package com.laralbir.japantraveltools.feature.planner.pdf

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.planner.pdf.domain.PdfDocumentLayoutCalculator
import com.laralbir.japantraveltools.feature.planner.pdf.model.PdfExportScope
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfDocumentLayoutCalculatorTest {

    @Test
    fun testPageConstantsAndColors() {
        assertEquals(595, PdfDocumentLayoutCalculator.PAGE_WIDTH)
        assertEquals(842, PdfDocumentLayoutCalculator.PAGE_HEIGHT)
        assertEquals(36, PdfDocumentLayoutCalculator.MARGIN)
        assertEquals(523, PdfDocumentLayoutCalculator.PRINTABLE_WIDTH)
        assertEquals(0xFFC93A40.toInt(), PdfDocumentLayoutCalculator.COLOR_AKABENI)
        assertEquals(0xFF1E3A5F.toInt(), PdfDocumentLayoutCalculator.COLOR_AI_IRO)
        assertEquals(0xFF1A1A1A.toInt(), PdfDocumentLayoutCalculator.COLOR_TEXT_PRIMARY)
        assertEquals(0xFFFAF7F2.toInt(), PdfDocumentLayoutCalculator.COLOR_SURFACE_LIGHT)
    }

    @Test
    fun testCalculateStopHeight_withAndWithoutJapanese() {
        val stop = ItineraryStopItem(
            id = "stop-test-1",
            dayNumber = 1,
            dateIso = "2026-11-11",
            startTime = "09:00",
            endTime = "11:00",
            title = "Senso-ji Temple & Kaminarimon",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "浅草寺 · 雷門",
            latitude = 35.7147,
            longitude = 139.7966,
            notes = "Tomar la línea Ginza salida 1 y comprar Ningyo-yaki"
        )

        val heightWithJp = PdfDocumentLayoutCalculator.calculateStopHeight(stop, includeJapaneseNames = true)
        val heightWithoutJp = PdfDocumentLayoutCalculator.calculateStopHeight(stop, includeJapaneseNames = false)

        assertTrue("Height with Japanese should be greater than without", heightWithJp > heightWithoutJp)
        assertTrue("Height should be at least 50 points", heightWithoutJp >= 50f)
    }

    @Test
    fun testCalculateTransportHeight() {
        val segment = TransportSegmentItem(
            id = "seg-1",
            order = 1,
            type = TransportMode.TRAIN_SHINKANSEN,
            operatorName = "JR Central",
            lineOrNumber = "Nozomi 23",
            departureCode = "TYO",
            departureName = "Tokyo Station",
            departureZoneId = "Asia/Tokyo",
            departureTerminalOrPlatform = "Andén 14",
            departureTimeIso = "2026-11-12T09:00",
            arrivalCode = "KYO",
            arrivalName = "Kyoto Station",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "Andén 12",
            arrivalTimeIso = "2026-11-12T11:15",
            seatOrCarInfo = "Coche 5 · Asiento 12-E"
        )

        val height = PdfDocumentLayoutCalculator.calculateTransportHeight(segment)
        assertTrue("Transport segment height should be between 50 and 120 points", height in 50f..120f)
    }

    @Test
    fun testPaginateStops_singlePage_whenFewStops() {
        val sampleStops = listOf(
            createSampleStop("1", "Stop 1", "08:00", "09:30", "浅草寺"),
            createSampleStop("2", "Stop 2", "10:00", "11:30", "東京スカイツリー"),
            createSampleStop("3", "Stop 3", "12:00", "13:30", "一蘭 新宿")
        )

        val pages = PdfDocumentLayoutCalculator.paginateStops(sampleStops, includeJapaneseNames = true)
        assertEquals("Three stops should comfortably fit on 1 page", 1, pages.size)
        assertEquals(3, pages[0].items.size)
        assertTrue("Total height should not exceed usable page height", pages[0].totalHeight <= PdfDocumentLayoutCalculator.USABLE_PAGE_HEIGHT)
    }

    @Test
    fun testPaginateStops_multiplePages_whenManyStops() {
        // Create 16 stops with notes, which cannot fit on a single A4 page
        val manyStops = (1..16).map { i ->
            createSampleStop(
                id = "$i",
                title = "Parada turística número $i con descripción extensa de actividades y recomendaciones",
                startTime = String.format("%02d:00", 7 + (i % 14)),
                endTime = String.format("%02d:30", 8 + (i % 14)),
                japanese = "観光地 $i 番目 · 日本語の名称",
                notes = "Instrucciones de transporte, trasbordo en estación JR y billetes reservados."
            )
        }

        val pages = PdfDocumentLayoutCalculator.paginateStops(manyStops, includeJapaneseNames = true)
        assertTrue("16 detailed stops should require at least 2 pages", pages.size >= 2)

        val totalItemsInPages = pages.sumOf { it.items.size }
        assertEquals("All stops must be accounted for across pages", 16, totalItemsInPages)

        pages.forEach { page ->
            assertTrue(
                "Page ${page.pageIndex} height (${page.totalHeight}) exceeded USABLE_PAGE_HEIGHT (${PdfDocumentLayoutCalculator.USABLE_PAGE_HEIGHT})",
                page.totalHeight <= PdfDocumentLayoutCalculator.USABLE_PAGE_HEIGHT
            )
        }
    }

    @Test
    fun testPaginateStops_emptyList_returnsSingleEmptyPage() {
        val pages = PdfDocumentLayoutCalculator.paginateStops(emptyList())
        assertEquals(1, pages.size)
        assertTrue(pages[0].items.isEmpty())
        assertEquals(0f, pages[0].totalHeight, 0.01f)
    }

    @Test
    fun testBilingualStopLabelFormatting() {
        val labelWithJp = PdfDocumentLayoutCalculator.formatBilingualStopLabel(
            title = "Senso-ji",
            placeNameJapanese = "浅草寺",
            includeJapanese = true
        )
        assertTrue(labelWithJp.contains("Senso-ji"))
        assertTrue(labelWithJp.contains("浅草寺"))

        val labelWithoutJp = PdfDocumentLayoutCalculator.formatBilingualStopLabel(
            title = "Senso-ji",
            placeNameJapanese = "浅草寺",
            includeJapanese = false
        )
        assertEquals("Senso-ji", labelWithoutJp)

        val labelWithBlankJp = PdfDocumentLayoutCalculator.formatBilingualStopLabel(
            title = "Senso-ji",
            placeNameJapanese = "   ",
            includeJapanese = true
        )
        assertEquals("Senso-ji", labelWithBlankJp)
    }

    @Test
    fun testPreserveJapaneseText_preservesKanjiAndKana() {
        val originalText = "伏見稲荷大社 (千本鳥居) · ひらがな カタカナ 123"
        val preserved = PdfDocumentLayoutCalculator.preserveJapaneseText(originalText)
        assertEquals(originalText, preserved)

        assertTrue(PdfDocumentLayoutCalculator.containsJapaneseGlyphs("浅草寺"))
        assertTrue(PdfDocumentLayoutCalculator.containsJapaneseGlyphs("あさくさ"))
        assertTrue(PdfDocumentLayoutCalculator.containsJapaneseGlyphs("スカイツリー"))
        assertTrue(PdfDocumentLayoutCalculator.containsJapaneseGlyphs("新宿グランベルホテル (歌舞伎町 2-14-5)"))
        assertFalse(PdfDocumentLayoutCalculator.containsJapaneseGlyphs("Tokyo Skytree Tower 123"))
    }

    @Test
    fun testTextWrapping_handlesJapaneseAndLatin() {
        val longText = "Esta es una descripción larga para verificar que el calculador de maquetación divide correctamente las líneas de texto en un documento PDF."
        val lines = PdfDocumentLayoutCalculator.wrapText(longText, maxCharsPerLine = 30)
        assertTrue("Should split into multiple lines", lines.size > 1)
        lines.forEach { line ->
            assertTrue("Line length should not exceed maxCharsPerLine by much", line.length <= 35)
        }

        val cjkText = "東京都千代田区千代田皇居東御苑歴史散策コース外国人観光客向けおすすめルート"
        val cjkLines = PdfDocumentLayoutCalculator.wrapText(cjkText, maxCharsPerLine = 15)
        assertTrue("CJK text without spaces should wrap properly", cjkLines.size >= 2)
        assertEquals(cjkText, cjkLines.joinToString(""))
    }

    @Test
    fun testFormatPageFooter() {
        val footer = PdfDocumentLayoutCalculator.formatPageFooter(pageIndex = 2, totalPages = 5)
        assertEquals("Página 2 de 5 — Japan Travel Tools", footer)
    }

    @Test
    fun testFormatDayHeader() {
        val header = PdfDocumentLayoutCalculator.formatDayHeader(dayIndex = 1, dateLabel = "Miércoles, 11 de Noviembre")
        assertEquals("Día 1 · Miércoles, 11 de Noviembre", header)
    }

    @Test
    fun testSanitizeFileName() {
        val dayFileName = PdfDocumentLayoutCalculator.sanitizeFileName("Viaje Japón 2026", PdfExportScope.SINGLE_DAY, dayIndex = 3)
        assertEquals("viaje-japon-2026-dia-3.pdf", dayFileName)

        val fullTripFileName = PdfDocumentLayoutCalculator.sanitizeFileName("Mi Luna de Miel en Tokio & Kioto", PdfExportScope.FULL_TRIP)
        assertEquals("mi-luna-de-miel-en-tokio-kioto-dossier-completo.pdf", fullTripFileName)
    }

    private fun createSampleStop(
        id: String,
        title: String,
        startTime: String,
        endTime: String,
        japanese: String,
        notes: String = ""
    ): ItineraryStopItem {
        return ItineraryStopItem(
            id = id,
            dayNumber = 1,
            dateIso = "2026-11-11",
            startTime = startTime,
            endTime = endTime,
            title = title,
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = japanese,
            latitude = 35.0,
            longitude = 139.0,
            notes = notes
        )
    }
}
