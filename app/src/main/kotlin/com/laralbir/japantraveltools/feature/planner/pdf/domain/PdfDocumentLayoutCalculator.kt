package com.laralbir.japantraveltools.feature.planner.pdf.domain

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.pdf.model.PdfExportScope
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem

/**
 * Pure domain layout calculation engine for A4 itinerary PDF generation.
 * Operates without platform UI or Canvas dependencies to allow fast, deterministic JVM unit tests.
 */
object PdfDocumentLayoutCalculator {

    // Standard A4 dimensions in PostScript points (72 DPI)
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    const val MARGIN = 36

    // Traditional Japanese palette colors
    const val COLOR_AKABENI = 0xFFC93A40.toInt()      // Akabeni (Torii red)
    const val COLOR_AI_IRO = 0xFF1E3A5F.toInt()       // Ai-iro (Japanese Indigo)
    const val COLOR_TEXT_PRIMARY = 0xFF1A1A1A.toInt() // Sumi ink dark
    const val COLOR_SURFACE_LIGHT = 0xFFFAF7F2.toInt()// Washi paper tone
    const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    const val COLOR_TEXT_MUTED = 0xFF6B7280.toInt()
    const val COLOR_BORDER = 0xFFE5E7EB.toInt()
    const val COLOR_ACCENT_GOLD = 0xFFD4AF37.toInt()

    // Printable & layout metrics
    const val PRINTABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2) // 523 points
    const val PRINTABLE_HEIGHT = PAGE_HEIGHT - (MARGIN * 2) // 770 points
    const val HEADER_HEIGHT = 70f
    const val FOOTER_HEIGHT = 30f
    const val CONTENT_TOP = MARGIN + HEADER_HEIGHT // 106f
    const val CONTENT_BOTTOM = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT // 776f
    const val USABLE_PAGE_HEIGHT = CONTENT_BOTTOM - CONTENT_TOP // 670f
    const val ITEM_SPACING = 12f

    data class PageLayoutPlan<T>(
        val pageIndex: Int,
        val items: List<T>,
        val totalHeight: Float,
        val isFirstPage: Boolean = pageIndex == 1
    )

    /**
     * Calculates the estimated vertical height of an itinerary stop card.
     */
    fun calculateStopHeight(
        stop: ItineraryStopItem,
        includeJapaneseNames: Boolean = true,
        maxTitleCharsPerLine: Int = 38,
        maxNotesCharsPerLine: Int = 44
    ): Float {
        var height = 28f // Order badge, time range and padding

        val titleLines = wrapText(stop.title, maxTitleCharsPerLine).size.coerceAtLeast(1)
        height += titleLines * 16f

        if (includeJapaneseNames && stop.placeNameJapanese.isNotBlank()) {
            val jpLines = wrapText(stop.placeNameJapanese, maxTitleCharsPerLine).size.coerceAtLeast(1)
            height += jpLines * 18f
        }

        if (stop.notes.isNotBlank()) {
            val notesLines = wrapText(stop.notes, maxNotesCharsPerLine).size.coerceAtLeast(1)
            height += notesLines * 13f + 4f
        }

        if (!stop.linkedDocumentTitle.isNullOrBlank()) {
            height += 15f
        }

        return height.coerceAtLeast(50f)
    }

    /**
     * Calculates the estimated vertical height of a transport segment card.
     */
    fun calculateTransportHeight(
        transport: TransportSegmentItem
    ): Float {
        var height = 24f // Header row (badge, operator, line)
        height += 28f   // Route row (Origin -> Destination with platforms and times)
        height += 18f   // Seat / car info row
        return height.coerceIn(50f, 120f)
    }

    /**
     * Partitions a list of itinerary stops into separate pages so they fit cleanly without vertical clipping.
     */
    fun paginateStops(
        stops: List<ItineraryStopItem>,
        includeJapaneseNames: Boolean = true,
        availableHeight: Float = USABLE_PAGE_HEIGHT,
        itemSpacing: Float = ITEM_SPACING
    ): List<PageLayoutPlan<ItineraryStopItem>> {
        if (stops.isEmpty()) {
            return listOf(PageLayoutPlan(pageIndex = 1, items = emptyList(), totalHeight = 0f, isFirstPage = true))
        }

        val pages = mutableListOf<PageLayoutPlan<ItineraryStopItem>>()
        var currentPageIndex = 1
        var currentItems = mutableListOf<ItineraryStopItem>()
        var currentHeight = 0f

        for (stop in stops) {
            val stopHeight = calculateStopHeight(stop, includeJapaneseNames)
            val addedHeight = if (currentItems.isEmpty()) stopHeight else stopHeight + itemSpacing
            if (currentHeight + addedHeight > availableHeight && currentItems.isNotEmpty()) {
                pages.add(
                    PageLayoutPlan(
                        pageIndex = currentPageIndex,
                        items = currentItems,
                        totalHeight = currentHeight,
                        isFirstPage = currentPageIndex == 1
                    )
                )
                currentPageIndex++
                currentItems = mutableListOf(stop)
                currentHeight = stopHeight
            } else {
                currentItems.add(stop)
                currentHeight += addedHeight
            }
        }

        if (currentItems.isNotEmpty()) {
            pages.add(
                PageLayoutPlan(
                    pageIndex = currentPageIndex,
                    items = currentItems,
                    totalHeight = currentHeight,
                    isFirstPage = currentPageIndex == 1
                )
            )
        }

        return pages
    }

    /**
     * Formats a bilingual stop label, optionally combining romanized title and Japanese name.
     */
    fun formatBilingualStopLabel(
        title: String,
        placeNameJapanese: String,
        includeJapanese: Boolean
    ): String {
        val cleanJp = placeNameJapanese.trim()
        return if (includeJapanese && cleanJp.isNotEmpty()) {
            "$title ($cleanJp)"
        } else {
            title
        }
    }

    /**
     * Formats start and end time into a clean range string.
     */
    fun formatTimeRange(startTime: String, endTime: String): String {
        return if (endTime.isNotBlank()) "$startTime – $endTime" else startTime
    }

    /**
     * Formats the day section header.
     */
    fun formatDayHeader(dayIndex: Int, dateLabel: String): String {
        return "Día $dayIndex · $dateLabel"
    }

    /**
     * Formats the standard footer string with current page, total pages, and app name.
     */
    fun formatPageFooter(pageIndex: Int, totalPages: Int): String {
        return "Página $pageIndex de $totalPages — Japan Travel Tools"
    }

    /**
     * Checks if a string contains any Japanese Unicode characters (Kanji, Hiragana, Katakana, CJK symbols).
     */
    fun containsJapaneseGlyphs(text: String): Boolean {
        return text.any { ch ->
            val code = ch.code
            (code in 0x3040..0x309F) || // Hiragana
            (code in 0x30A0..0x30FF) || // Katakana
            (code in 0x4E00..0x9FFF) || // CJK Unified Ideographs
            (code in 0x3400..0x4DBF) || // CJK Extension A
            (code in 0x3000..0x303F) || // CJK Symbols and Punctuation
            (code in 0xFF00..0xFFEF)    // Halfwidth and Fullwidth Forms
        }
    }

    /**
     * Preserves Japanese text while removing non-printable control characters.
     */
    fun preserveJapaneseText(text: String): String {
        return text.filter { it.code in 0x20..0x7E || it == '\n' || it == '\t' || it.code >= 0x00A0 }
    }

    /**
     * Wraps a string into lines of maximum characters, splitting on spaces or CJK glyph boundaries.
     */
    fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        if (text.isEmpty()) return emptyList()
        if (text.length <= maxCharsPerLine) return listOf(text)

        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (para in paragraphs) {
            if (para.isEmpty()) {
                lines.add("")
                continue
            }

            val hasSpaces = para.contains(" ")
            if (!hasSpaces && containsJapaneseGlyphs(para)) {
                var currentPos = 0
                while (currentPos < para.length) {
                    val endPos = (currentPos + maxCharsPerLine).coerceAtMost(para.length)
                    lines.add(para.substring(currentPos, endPos))
                    currentPos = endPos
                }
            } else {
                val words = para.split(Regex("\\s+"))
                var currentLine = StringBuilder()

                for (word in words) {
                    if (currentLine.isEmpty()) {
                        if (word.length <= maxCharsPerLine) {
                            currentLine.append(word)
                        } else {
                            var pos = 0
                            while (pos < word.length) {
                                val chunkEnd = (pos + maxCharsPerLine).coerceAtMost(word.length)
                                val chunk = word.substring(pos, chunkEnd)
                                if (pos + maxCharsPerLine >= word.length) {
                                    currentLine.append(chunk)
                                } else {
                                    lines.add(chunk)
                                }
                                pos = chunkEnd
                            }
                        }
                    } else if (currentLine.length + 1 + word.length <= maxCharsPerLine) {
                        currentLine.append(" ").append(word)
                    } else {
                        lines.add(currentLine.toString())
                        currentLine = StringBuilder()
                        if (word.length <= maxCharsPerLine) {
                            currentLine.append(word)
                        } else {
                            var pos = 0
                            while (pos < word.length) {
                                val chunkEnd = (pos + maxCharsPerLine).coerceAtMost(word.length)
                                val chunk = word.substring(pos, chunkEnd)
                                if (pos + maxCharsPerLine >= word.length) {
                                    currentLine.append(chunk)
                                } else {
                                    lines.add(chunk)
                                }
                                pos = chunkEnd
                            }
                        }
                    }
                }
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
            }
        }

        return lines
    }

    /**
     * Produces a clean, sanitized file name for the exported PDF.
     */
    fun sanitizeFileName(rawTitle: String, scope: PdfExportScope, dayIndex: Int? = null): String {
        val normalized = java.text.Normalizer.normalize(rawTitle, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        val cleanTitle = normalized.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "itinerario" }
        return when (scope) {
            PdfExportScope.SINGLE_DAY -> "$cleanTitle-dia-${dayIndex ?: 1}.pdf"
            PdfExportScope.FULL_TRIP -> "$cleanTitle-dossier-completo.pdf"
        }
    }
}
