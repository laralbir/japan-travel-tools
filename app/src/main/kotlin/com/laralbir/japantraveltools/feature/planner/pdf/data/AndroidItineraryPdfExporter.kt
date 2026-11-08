package com.laralbir.japantraveltools.feature.planner.pdf.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.laralbir.japantraveltools.core.datetime.TravelDateTimeFormatter
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.map.GeoProjectionUtils
import com.laralbir.japantraveltools.feature.planner.pdf.domain.PdfDocumentLayoutCalculator
import com.laralbir.japantraveltools.feature.planner.pdf.domain.PdfMapLayout
import com.laralbir.japantraveltools.feature.planner.pdf.domain.PdfMapProjectionCalculator
import com.laralbir.japantraveltools.feature.planner.pdf.domain.repository.ItineraryPdfExporter
import com.laralbir.japantraveltools.feature.planner.pdf.model.PdfExportOptions
import com.laralbir.japantraveltools.feature.planner.pdf.model.PdfExportResult
import com.laralbir.japantraveltools.feature.planner.pdf.model.PdfExportScope
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate

/**
 * 100% offline native PDF exporter using android.graphics.pdf.PdfDocument, Canvas and Paint.
 * Follows traditional Japanese aesthetic (Akabeni Torii red, Ai-iro indigo and Washi light tones)
 * and guarantees high-contrast, prominent CJK Japanese glyph readability for paper printing.
 */
class AndroidItineraryPdfExporter(
    private val context: Context
) : ItineraryPdfExporter {

    override suspend fun exportDayItinerary(
        planTitle: String,
        dayIndex: Int,
        dateLabel: String,
        stops: List<ItineraryStopItem>,
        transports: List<TransportSegmentItem>,
        options: PdfExportOptions
    ): PdfExportResult = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports/pdf").apply { mkdirs() }
        cleanOldExports(exportDir)

        val fileName = PdfDocumentLayoutCalculator.sanitizeFileName(
            rawTitle = planTitle,
            scope = PdfExportScope.SINGLE_DAY,
            dayIndex = dayIndex
        )
        val targetFile = File(exportDir, fileName)

        val relevantTransports = if (options.includeTransportSegments) transports else emptyList()

        val mapLayout = if (options.includeDayMap) {
            PdfMapProjectionCalculator.projectStopsToCanvas(
                stops = stops,
                width = PdfDocumentLayoutCalculator.PRINTABLE_WIDTH.toFloat(),
                height = PdfDocumentLayoutCalculator.MAP_CARD_HEIGHT
            )
        } else null
        val hasMap = mapLayout != null && mapLayout.hasValidCoordinates

        var firstPageAvailableHeight = PdfDocumentLayoutCalculator.USABLE_PAGE_HEIGHT
        if (relevantTransports.isNotEmpty()) {
            val transH = relevantTransports.take(2).sumOf {
                PdfDocumentLayoutCalculator.calculateTransportHeight(it).toDouble()
            }.toFloat() + (relevantTransports.take(2).size * PdfDocumentLayoutCalculator.ITEM_SPACING)
            firstPageAvailableHeight -= transH
        }
        if (hasMap) {
            firstPageAvailableHeight -= (PdfDocumentLayoutCalculator.MAP_CARD_HEIGHT + PdfDocumentLayoutCalculator.ITEM_SPACING)
        }
        firstPageAvailableHeight = firstPageAvailableHeight.coerceAtLeast(100f)

        val paginatedStops = PdfDocumentLayoutCalculator.paginateStops(
            stops = stops,
            includeJapaneseNames = options.includeJapaneseNames,
            firstPageAvailableHeight = firstPageAvailableHeight,
            subsequentPagesAvailableHeight = PdfDocumentLayoutCalculator.USABLE_PAGE_HEIGHT
        )
        val totalPages = paginatedStops.size.coerceAtLeast(1)

        val document = PdfDocument()
        try {
            for ((pageIdxZero, pagePlan) in paginatedStops.withIndex()) {
                val pageNumber = pageIdxZero + 1
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PdfDocumentLayoutCalculator.PAGE_WIDTH,
                    PdfDocumentLayoutCalculator.PAGE_HEIGHT,
                    pageNumber
                ).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                canvas.drawColor(PdfDocumentLayoutCalculator.COLOR_SURFACE_LIGHT)

                // Header
                val daySubHeader = PdfDocumentLayoutCalculator.formatDayHeader(dayIndex, dateLabel)
                drawHeader(canvas, planTitle, daySubHeader, pageNumber, totalPages)

                // Content cursor
                var cursorY = PdfDocumentLayoutCalculator.CONTENT_TOP + 10f

                // Draw transport segment on first page if available
                if (pageNumber == 1 && relevantTransports.isNotEmpty()) {
                    for (transport in relevantTransports.take(2)) {
                        val tHeight = PdfDocumentLayoutCalculator.calculateTransportHeight(transport)
                        drawTransportCard(canvas, transport, cursorY, tHeight)
                        cursorY += tHeight + PdfDocumentLayoutCalculator.ITEM_SPACING
                    }
                }

                // Draw daily route map on first page if available
                if (pageNumber == 1 && hasMap) {
                    drawDayMapCard(canvas, mapLayout, cursorY)
                    cursorY += PdfDocumentLayoutCalculator.MAP_CARD_HEIGHT + PdfDocumentLayoutCalculator.ITEM_SPACING
                }

                // Draw stops
                for (stop in pagePlan.items) {
                    val stopHeight = PdfDocumentLayoutCalculator.calculateStopHeight(
                        stop = stop,
                        includeJapaneseNames = options.includeJapaneseNames
                    )
                    drawStopCard(canvas, stop, cursorY, stopHeight, options.includeJapaneseNames)
                    cursorY += stopHeight + PdfDocumentLayoutCalculator.ITEM_SPACING
                }

                // Empty day state notice if no stops
                if (pagePlan.items.isEmpty() && relevantTransports.isEmpty()) {
                    drawEmptyStateNotice(canvas, cursorY)
                }

                // Footer
                drawFooter(canvas, pageNumber, totalPages)

                document.finishPage(page)
            }

            FileOutputStream(targetFile).use { fos ->
                document.writeTo(fos)
                fos.flush()
            }
        } catch (e: Exception) {
            throw IOException("Error al generar el documento PDF del día: ${e.message}", e)
        } finally {
            document.close()
        }

        PdfExportResult(
            file = targetFile,
            fileName = fileName,
            pageCount = totalPages,
            fileSizeBytes = targetFile.length()
        )
    }

    override suspend fun exportFullTrip(
        planTitle: String,
        startDateIso: String,
        totalDays: Int,
        stopsByDay: Map<Int, List<ItineraryStopItem>>,
        transports: List<TransportSegmentItem>,
        options: PdfExportOptions
    ): PdfExportResult = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports/pdf").apply { mkdirs() }
        cleanOldExports(exportDir)

        val fileName = PdfDocumentLayoutCalculator.sanitizeFileName(
            rawTitle = planTitle,
            scope = PdfExportScope.FULL_TRIP
        )
        val targetFile = File(exportDir, fileName)

        // Pre-calculate page count: Cover (1) + Day pages + Emergency contacts (1 if enabled)
        val dayPagePlans = mutableMapOf<Int, List<PdfDocumentLayoutCalculator.PageLayoutPlan<ItineraryStopItem>>>()
        val dayMapLayouts = mutableMapOf<Int, PdfMapLayout?>()
        var estimatedPages = 1 // Cover page

        for (dayIdx in 1..totalDays) {
            val dayStops = stopsByDay[dayIdx] ?: emptyList()
            val dayTransports = if (options.includeTransportSegments) {
                transports.filter { it.order == dayIdx || transports.size <= 3 }
            } else emptyList()

            val mapLayout = if (options.includeDayMap) {
                PdfMapProjectionCalculator.projectStopsToCanvas(
                    stops = dayStops,
                    width = PdfDocumentLayoutCalculator.PRINTABLE_WIDTH.toFloat(),
                    height = PdfDocumentLayoutCalculator.MAP_CARD_HEIGHT
                )
            } else null
            val hasMap = mapLayout != null && mapLayout.hasValidCoordinates
            if (hasMap) {
                dayMapLayouts[dayIdx] = mapLayout
            }

            var firstPageH = PdfDocumentLayoutCalculator.USABLE_PAGE_HEIGHT
            if (dayTransports.isNotEmpty()) {
                val transH = dayTransports.take(2).sumOf {
                    PdfDocumentLayoutCalculator.calculateTransportHeight(it).toDouble()
                }.toFloat() + (dayTransports.take(2).size * PdfDocumentLayoutCalculator.ITEM_SPACING)
                firstPageH -= transH
            }
            if (hasMap) {
                firstPageH -= (PdfDocumentLayoutCalculator.MAP_CARD_HEIGHT + PdfDocumentLayoutCalculator.ITEM_SPACING)
            }
            firstPageH = firstPageH.coerceAtLeast(100f)

            val pages = PdfDocumentLayoutCalculator.paginateStops(
                stops = dayStops,
                includeJapaneseNames = options.includeJapaneseNames,
                firstPageAvailableHeight = firstPageH,
                subsequentPagesAvailableHeight = PdfDocumentLayoutCalculator.USABLE_PAGE_HEIGHT
            )
            dayPagePlans[dayIdx] = pages
            estimatedPages += pages.size
        }

        if (options.includeEmergencyContacts) {
            estimatedPages += 1 // Dedicated emergency page
        }

        val totalPages = estimatedPages
        var currentPageNumber = 1

        val document = PdfDocument()
        try {
            // Page 1: Cover Page
            val coverPageInfo = PdfDocument.PageInfo.Builder(
                PdfDocumentLayoutCalculator.PAGE_WIDTH,
                PdfDocumentLayoutCalculator.PAGE_HEIGHT,
                currentPageNumber
            ).create()
            val coverPage = document.startPage(coverPageInfo)
            drawCoverPage(
                canvas = coverPage.canvas,
                planTitle = planTitle,
                startDateIso = startDateIso,
                totalDays = totalDays,
                totalStops = stopsByDay.values.sumOf { it.size },
                transportsCount = transports.size,
                pageNumber = currentPageNumber,
                totalPages = totalPages
            )
            document.finishPage(coverPage)
            currentPageNumber++

            // Day-by-day pages
            val baseDate = runCatching { LocalDate.parse(startDateIso) }.getOrNull()

            for (dayIdx in 1..totalDays) {
                val dayStops = stopsByDay[dayIdx] ?: emptyList()
                val dayDateLabel = baseDate?.plusDays((dayIdx - 1).toLong())?.let {
                    TravelDateTimeFormatter.formatFullLocalDate(it.toString())
                } ?: "Día $dayIdx"

                val pages = dayPagePlans[dayIdx] ?: emptyList()
                val dayTransports = if (options.includeTransportSegments) {
                    transports.filter { it.order == dayIdx || transports.size <= 3 }
                } else emptyList()

                for ((subIdx, pagePlan) in pages.withIndex()) {
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        PdfDocumentLayoutCalculator.PAGE_WIDTH,
                        PdfDocumentLayoutCalculator.PAGE_HEIGHT,
                        currentPageNumber
                    ).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas

                    canvas.drawColor(PdfDocumentLayoutCalculator.COLOR_SURFACE_LIGHT)
                    val dayHeader = PdfDocumentLayoutCalculator.formatDayHeader(dayIdx, dayDateLabel)
                    drawHeader(canvas, planTitle, dayHeader, currentPageNumber, totalPages)

                    var cursorY = PdfDocumentLayoutCalculator.CONTENT_TOP + 10f

                    // If first subpage of the day and transport exists, draw
                    if (subIdx == 0 && dayTransports.isNotEmpty()) {
                        for (transport in dayTransports.take(2)) {
                            val tHeight = PdfDocumentLayoutCalculator.calculateTransportHeight(transport)
                            drawTransportCard(canvas, transport, cursorY, tHeight)
                            cursorY += tHeight + PdfDocumentLayoutCalculator.ITEM_SPACING
                        }
                    }

                    // If first subpage of the day and map exists, draw
                    val currentDayMap = dayMapLayouts[dayIdx]
                    if (subIdx == 0 && currentDayMap != null && currentDayMap.hasValidCoordinates) {
                        drawDayMapCard(canvas, currentDayMap, cursorY)
                        cursorY += PdfDocumentLayoutCalculator.MAP_CARD_HEIGHT + PdfDocumentLayoutCalculator.ITEM_SPACING
                    }

                    for (stop in pagePlan.items) {
                        val stopHeight = PdfDocumentLayoutCalculator.calculateStopHeight(
                            stop = stop,
                            includeJapaneseNames = options.includeJapaneseNames
                        )
                        drawStopCard(canvas, stop, cursorY, stopHeight, options.includeJapaneseNames)
                        cursorY += stopHeight + PdfDocumentLayoutCalculator.ITEM_SPACING
                    }

                    if (pagePlan.items.isEmpty() && dayTransports.isEmpty()) {
                        drawEmptyStateNotice(canvas, cursorY)
                    }

                    drawFooter(canvas, currentPageNumber, totalPages)
                    document.finishPage(page)
                    currentPageNumber++
                }
            }

            // Emergency Contacts Page
            if (options.includeEmergencyContacts) {
                val emPageInfo = PdfDocument.PageInfo.Builder(
                    PdfDocumentLayoutCalculator.PAGE_WIDTH,
                    PdfDocumentLayoutCalculator.PAGE_HEIGHT,
                    currentPageNumber
                ).create()
                val emPage = document.startPage(emPageInfo)
                drawEmergencyPage(emPage.canvas, currentPageNumber, totalPages)
                document.finishPage(emPage)
                currentPageNumber++
            }

            FileOutputStream(targetFile).use { fos ->
                document.writeTo(fos)
                fos.flush()
            }
        } catch (e: Exception) {
            throw IOException("Error al generar el dossier completo de viaje en PDF: ${e.message}", e)
        } finally {
            document.close()
        }

        PdfExportResult(
            file = targetFile,
            fileName = fileName,
            pageCount = totalPages,
            fileSizeBytes = targetFile.length()
        )
    }

    private fun drawHeader(
        canvas: Canvas,
        title: String,
        subHeader: String,
        pageNumber: Int,
        totalPages: Int
    ) {
        val margin = PdfDocumentLayoutCalculator.MARGIN.toFloat()
        val printableWidth = PdfDocumentLayoutCalculator.PRINTABLE_WIDTH.toFloat()

        // Akabeni Torii top bar
        val barPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AKABENI
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            RectF(margin, margin, margin + printableWidth, margin + 5f),
            2.5f,
            2.5f,
            barPaint
        )

        // Trip title
        val titlePaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_PRIMARY
            textSize = 15f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(title, margin, margin + 24f, titlePaint)

        // Subheader (Day & Date)
        val subPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AI_IRO
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(subHeader, margin, margin + 40f, subPaint)

        // Light divider line
        val linePaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_BORDER
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val lineY = margin + PdfDocumentLayoutCalculator.HEADER_HEIGHT - 6f
        canvas.drawLine(margin, lineY, margin + printableWidth, lineY, linePaint)
    }

    private fun drawStopCard(
        canvas: Canvas,
        stop: ItineraryStopItem,
        topY: Float,
        cardHeight: Float,
        includeJapanese: Boolean
    ) {
        val margin = PdfDocumentLayoutCalculator.MARGIN.toFloat()
        val printableWidth = PdfDocumentLayoutCalculator.PRINTABLE_WIDTH.toFloat()
        val cardRect = RectF(margin, topY, margin + printableWidth, topY + cardHeight)

        // Card background
        val bgPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 6f, 6f, bgPaint)

        // Card border
        val borderPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_BORDER
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

        // Left accent strip (Akabeni)
        val stripPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AKABENI
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(margin, topY, margin + 4f, topY + cardHeight), 2f, 2f, stripPaint)

        // Time range badge
        val timePaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AI_IRO
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val timeStr = PdfDocumentLayoutCalculator.formatTimeRange(stop.startTime, stop.endTime)
        canvas.drawText(timeStr, margin + 14f, topY + 16f, timePaint)

        // Category badge
        val catPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_MUTED
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val catStr = "•  ${stop.category.badge} (${stop.category.labelEs})"
        canvas.drawText(catStr, margin + 105f, topY + 16f, catPaint)

        // Romanized / translated title
        var textY = topY + 32f
        val titlePaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_PRIMARY
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val titleLines = PdfDocumentLayoutCalculator.wrapText(stop.title, maxCharsPerLine = 48)
        for (line in titleLines) {
            canvas.drawText(line, margin + 14f, textY, titlePaint)
            textY += 14f
        }

        // Japanese official place name (highlighted with prominent CJK font >= 12pt for paper printing)
        if (includeJapanese && stop.placeNameJapanese.isNotBlank()) {
            val jpPaint = Paint().apply {
                color = PdfDocumentLayoutCalculator.COLOR_AI_IRO
                textSize = 13f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val jpLines = PdfDocumentLayoutCalculator.wrapText(
                PdfDocumentLayoutCalculator.preserveJapaneseText(stop.placeNameJapanese),
                maxCharsPerLine = 44
            )
            for (line in jpLines) {
                canvas.drawText(line, margin + 14f, textY + 2f, jpPaint)
                textY += 16f
            }
        }

        // Practical notes
        if (stop.notes.isNotBlank()) {
            val notesPaint = Paint().apply {
                color = PdfDocumentLayoutCalculator.COLOR_TEXT_MUTED
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }
            val noteLines = PdfDocumentLayoutCalculator.wrapText(stop.notes, maxCharsPerLine = 60)
            for (line in noteLines.take(3)) {
                canvas.drawText(line, margin + 14f, textY + 2f, notesPaint)
                textY += 11f
            }
        }

        // Linked document badge if present
        if (!stop.linkedDocumentTitle.isNullOrBlank()) {
            val linkPaint = Paint().apply {
                color = PdfDocumentLayoutCalculator.COLOR_AKABENI
                textSize = 8f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("Doc: ${stop.linkedDocumentTitle}", margin + 14f, textY + 2f, linkPaint)
        }
    }

    private fun drawTransportCard(
        canvas: Canvas,
        transport: TransportSegmentItem,
        topY: Float,
        cardHeight: Float
    ) {
        val margin = PdfDocumentLayoutCalculator.MARGIN.toFloat()
        val printableWidth = PdfDocumentLayoutCalculator.PRINTABLE_WIDTH.toFloat()
        val cardRect = RectF(margin, topY, margin + printableWidth, topY + cardHeight)

        val bgPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_BORDER
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

        // Ai-iro left accent strip
        val stripPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AI_IRO
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(margin, topY, margin + 4f, topY + cardHeight), 2f, 2f, stripPaint)

        // Mode & Operator header
        val headerPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AI_IRO
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerStr = "[${transport.type.badge}] ${transport.operatorName} · ${transport.lineOrNumber}"
        canvas.drawText(headerStr, margin + 14f, topY + 16f, headerPaint)

        // Route line
        val routePaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_PRIMARY
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val routeStr = "${transport.departureName} (${transport.departureCode}) → ${transport.arrivalName} (${transport.arrivalCode})"
        canvas.drawText(routeStr, margin + 14f, topY + 32f, routePaint)

        // Seat and details
        val detailPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_MUTED
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val detailStr = "Salida: ${transport.departureTimeIso.takeLast(5)} (${transport.departureTerminalOrPlatform}) • Asiento: ${transport.seatOrCarInfo.ifBlank { "Libre" }}"
        canvas.drawText(detailStr, margin + 14f, topY + 46f, detailPaint)
    }

    private fun drawDayMapCard(
        canvas: Canvas,
        mapLayout: PdfMapLayout,
        topY: Float
    ) {
        val margin = PdfDocumentLayoutCalculator.MARGIN.toFloat()
        val printableWidth = PdfDocumentLayoutCalculator.PRINTABLE_WIDTH.toFloat()
        val cardHeight = PdfDocumentLayoutCalculator.MAP_CARD_HEIGHT
        val cardRect = RectF(margin, topY, margin + printableWidth, topY + cardHeight)

        // Card background (clean cartographic off-white/washi tone)
        val bgPaint = Paint().apply {
            color = 0xFFFBFBF9.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 6f, 6f, bgPaint)

        // Card border
        val borderPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_BORDER
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

        // Left accent strip (Ai-iro indigo)
        val stripPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AI_IRO
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(margin, topY, margin + 4f, topY + cardHeight), 2f, 2f, stripPaint)

        // Header Title
        val titlePaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AI_IRO
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerText = "MAPA ESQUEMÁTICO DE VISITAS DEL DÍA (${mapLayout.points.size} PARADAS)"
        canvas.drawText(headerText, margin + 14f, topY + 16f, titlePaint)

        // Distance & Scale metric in header
        if (mapLayout.approxScaleLabel.isNotBlank()) {
            val distPaint = Paint().apply {
                color = PdfDocumentLayoutCalculator.COLOR_TEXT_MUTED
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText(mapLayout.approxScaleLabel, margin + 260f, topY + 16f, distPaint)
        }

        // Minimalist North Compass in top-right
        val compassCenterX = margin + printableWidth - 20f
        val compassCenterY = topY + 14f

        val compassBorder = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_BORDER
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawCircle(compassCenterX, compassCenterY, 8f, compassBorder)

        // Upward arrow
        val arrowPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AKABENI
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val arrowPath = Path().apply {
            moveTo(compassCenterX, compassCenterY - 7f)
            lineTo(compassCenterX - 3.5f, compassCenterY + 1f)
            lineTo(compassCenterX + 3.5f, compassCenterY + 1f)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)

        val nPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_PRIMARY
            textSize = 5.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("N", compassCenterX, compassCenterY + 6.5f, nPaint)

        // Subtle topographic grid lines
        val gridPaint = Paint().apply {
            color = 0xFFEBECEF.toInt()
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(3f, 4f), 0f)
            isAntiAlias = true
        }
        val gridY1 = topY + 55f
        val gridY2 = topY + 105f
        canvas.drawLine(margin + 14f, gridY1, margin + printableWidth - 14f, gridY1, gridPaint)
        canvas.drawLine(margin + 14f, gridY2, margin + printableWidth - 14f, gridY2, gridPaint)

        val gridX1 = margin + (printableWidth * 0.33f)
        val gridX2 = margin + (printableWidth * 0.66f)
        canvas.drawLine(gridX1, topY + 24f, gridX1, topY + cardHeight - 20f, gridPaint)
        canvas.drawLine(gridX2, topY + 24f, gridX2, topY + cardHeight - 20f, gridPaint)

        // Connecting route line
        if (mapLayout.points.size >= 2) {
            val routePath = Path()
            val firstPt = mapLayout.points.first()
            routePath.moveTo(margin + firstPt.x, topY + firstPt.y)
            for (i in 1 until mapLayout.points.size) {
                val pt = mapLayout.points[i]
                routePath.lineTo(margin + pt.x, topY + pt.y)
            }
            val routePaint = Paint().apply {
                color = PdfDocumentLayoutCalculator.COLOR_AI_IRO
                strokeWidth = 1.75f
                style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
                isAntiAlias = true
            }
            canvas.drawPath(routePath, routePaint)
        }

        // Draw Pins and Callout Labels
        val pinHaloPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val pinFillPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AKABENI
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val pinBorderPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_WHITE
            strokeWidth = 1.25f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val pinTextPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_WHITE
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val pillBgPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val pillBorderPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_BORDER
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val pillTextPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_PRIMARY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        for (pt in mapLayout.points) {
            val cx = margin + pt.x
            val cy = topY + pt.y

            // Pin badge
            canvas.drawCircle(cx, cy, 9f, pinHaloPaint)
            canvas.drawCircle(cx, cy, 7.5f, pinFillPaint)
            canvas.drawCircle(cx, cy, 7.5f, pinBorderPaint)
            canvas.drawText(pt.orderIndex.toString(), cx, cy + 2.8f, pinTextPaint)

            // Label callout pill
            val labelStr = GeoProjectionUtils.formatCompactStopLabel(
                startTime = pt.stop.startTime,
                title = pt.stop.title,
                maxChars = 20
            )
            val textWidth = pillTextPaint.measureText(labelStr)
            val pillWidth = textWidth + 8f
            val pillHeight = 12f

            val placeRight = cx + 10f + pillWidth <= margin + printableWidth - 8f
            val pillLeft = if (placeRight) cx + 9f else cx - 9f - pillWidth
            val pillTop = (cy - 6f).coerceIn(topY + 22f, topY + cardHeight - 28f)

            val pillRect = RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight)
            canvas.drawRoundRect(pillRect, 3f, 3f, pillBgPaint)
            canvas.drawRoundRect(pillRect, 3f, 3f, pillBorderPaint)
            canvas.drawText(labelStr, pillLeft + 4f, pillTop + 9f, pillTextPaint)
        }

        // Bottom legend bar
        val legendY = topY + cardHeight - 16f
        val legendLine = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_BORDER
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(margin + 10f, legendY, margin + printableWidth - 10f, legendY, legendLine)

        val legendTextPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_MUTED
            textSize = 7f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText(
            "🔴 Paradas numeradas en orden cronológico • ─── Trayecto secuencial entre visitas • 100% Offline",
            margin + 14f,
            legendY + 10f,
            legendTextPaint
        )
    }

    private fun drawEmptyStateNotice(canvas: Canvas, topY: Float) {
        val margin = PdfDocumentLayoutCalculator.MARGIN.toFloat()
        val textPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_MUTED
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("No hay paradas programadas para esta jornada en el planificador.", margin + 14f, topY + 30f, textPaint)
    }

    private fun drawCoverPage(
        canvas: Canvas,
        planTitle: String,
        startDateIso: String,
        totalDays: Int,
        totalStops: Int,
        transportsCount: Int,
        pageNumber: Int,
        totalPages: Int
    ) {
        canvas.drawColor(PdfDocumentLayoutCalculator.COLOR_SURFACE_LIGHT)
        val margin = PdfDocumentLayoutCalculator.MARGIN.toFloat()
        val printableWidth = PdfDocumentLayoutCalculator.PRINTABLE_WIDTH.toFloat()

        // Torii Red top block
        val bannerPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AKABENI
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(margin, margin, margin + printableWidth, margin + 140f), 8f, 8f, bannerPaint)

        // Cover Title inside banner
        val coverTitlePaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val bannerTitleLines = PdfDocumentLayoutCalculator.wrapText(planTitle, maxCharsPerLine = 32)
        var bannerY = margin + 45f
        for (line in bannerTitleLines) {
            canvas.drawText(line, margin + 20f, bannerY, coverTitlePaint)
            bannerY += 24f
        }

        val subCoverPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_SURFACE_LIGHT
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("DOSSIER OFICIAL DE ITINERARIO DE VIAJE", margin + 20f, bannerY + 6f, subCoverPaint)

        // Summary Card
        val cardRect = RectF(margin, margin + 160f, margin + printableWidth, margin + 300f)
        val cardPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, cardPaint)

        val cardBorder = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_BORDER
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, cardBorder)

        val statTitlePaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AI_IRO
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Resumen General del Viaje", margin + 20f, margin + 190f, statTitlePaint)

        val statBodyPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_PRIMARY
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("• Fecha de inicio: $startDateIso", margin + 20f, margin + 215f, statBodyPaint)
        canvas.drawText("• Duración total: $totalDays jornadas planificadas", margin + 20f, margin + 235f, statBodyPaint)
        canvas.drawText("• Total de actividades y paradas registradas: $totalStops", margin + 20f, margin + 255f, statBodyPaint)
        canvas.drawText("• Conexiones de transporte (Vuelos / Shinkansen): $transportsCount", margin + 20f, margin + 275f, statBodyPaint)

        // Offline notice
        val noticeRect = RectF(margin, margin + 320f, margin + printableWidth, margin + 400f)
        canvas.drawRoundRect(noticeRect, 8f, 8f, cardPaint)
        canvas.drawRoundRect(noticeRect, 8f, 8f, cardBorder)

        val noticeTitle = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_AKABENI
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Consejos de Supervivencia y Uso Offline", margin + 20f, margin + 345f, noticeTitle)

        val noticeText = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_MUTED
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("1. Los nombres en japonés están maquetados en tipografía ampliada para mostrarlos", margin + 20f, margin + 365f, noticeText)
        canvas.drawText("   directamente a taxistas o personal de estaciones en caso de perder cobertura.", margin + 20f, margin + 378f, noticeText)
        canvas.drawText("2. Mantén una copia digital en tu dispositivo o imprime este dossier en papel físico.", margin + 20f, margin + 391f, noticeText)

        drawFooter(canvas, pageNumber, totalPages)
    }

    private fun drawEmergencyPage(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        canvas.drawColor(PdfDocumentLayoutCalculator.COLOR_SURFACE_LIGHT)
        val margin = PdfDocumentLayoutCalculator.MARGIN.toFloat()
        val printableWidth = PdfDocumentLayoutCalculator.PRINTABLE_WIDTH.toFloat()

        drawHeader(
            canvas = canvas,
            title = "Contactos y Asistencia Consular de Emergencia",
            subHeader = "Teléfonos prioritarios y protocolos en Japón (24 horas)",
            pageNumber = pageNumber,
            totalPages = totalPages
        )

        var y = PdfDocumentLayoutCalculator.CONTENT_TOP + 10f

        val emergencyItems = listOf(
            Triple("Policía de Japón (Koban)", "110", "Denuncias, extravíos, emergencias de seguridad pública."),
            Triple("Ambulancia y Bomberos", "119", "Emergencias médicas graves o incendios (decir 'Kyūkyū' para ambulancia)."),
            Triple("Japan Travel Hotline (JNTO)", "050-3816-2720", "Atención multilingüe al turista 24/7 (inglés, español básico)."),
            Triple("Embajada de España en Tokio", "+81 3-3583-8531", "Sede: 1-3-29 Roppongi, Minato-ku, Tokyo 106-0032."),
            Triple("Emergencia Consular España 24h", "+81 90-1055-1926", "Exclusivo para accidentes graves, detenciones o catástrofes naturales.")
        )

        for (item in emergencyItems) {
            val cardRect = RectF(margin, y, margin + printableWidth, y + 54f)
            val bgPaint = Paint().apply {
                color = PdfDocumentLayoutCalculator.COLOR_WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(cardRect, 6f, 6f, bgPaint)

            val borderPaint = Paint().apply {
                color = PdfDocumentLayoutCalculator.COLOR_BORDER
                strokeWidth = 1f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

            // Red strip
            val stripPaint = Paint().apply {
                color = PdfDocumentLayoutCalculator.COLOR_AKABENI
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(margin, y, margin + 4f, y + 54f), 2f, 2f, stripPaint)

            val titlePaint = Paint().apply {
                color = PdfDocumentLayoutCalculator.COLOR_TEXT_PRIMARY
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(item.first, margin + 14f, y + 18f, titlePaint)

            val phonePaint = Paint().apply {
                color = PdfDocumentLayoutCalculator.COLOR_AKABENI
                textSize = 12f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(item.second, margin + 14f, y + 34f, phonePaint)

            val notePaint = Paint().apply {
                color = PdfDocumentLayoutCalculator.COLOR_TEXT_MUTED
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText(item.third, margin + 14f, y + 47f, notePaint)

            y += 54f + PdfDocumentLayoutCalculator.ITEM_SPACING
        }

        drawFooter(canvas, pageNumber, totalPages)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val margin = PdfDocumentLayoutCalculator.MARGIN.toFloat()
        val printableWidth = PdfDocumentLayoutCalculator.PRINTABLE_WIDTH.toFloat()
        val footerY = PdfDocumentLayoutCalculator.PAGE_HEIGHT - margin + 10f

        val linePaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_BORDER
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(margin, footerY - 14f, margin + printableWidth, footerY - 14f, linePaint)

        val footerPaint = Paint().apply {
            color = PdfDocumentLayoutCalculator.COLOR_TEXT_MUTED
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val footerText = PdfDocumentLayoutCalculator.formatPageFooter(pageNumber, totalPages)
        canvas.drawText(footerText, margin, footerY, footerPaint)
    }

    private fun cleanOldExports(exportDir: File) {
        runCatching {
            if (!exportDir.exists()) return
            val pdfFiles = exportDir.listFiles { file ->
                file.isFile && file.name.endsWith(".pdf", ignoreCase = true)
            } ?: return

            val now = System.currentTimeMillis()
            val maxAgeMillis = 24 * 60 * 60 * 1000L // 24 hours

            // Delete files older than 24 hours
            pdfFiles.forEach { file ->
                if (now - file.lastModified() > maxAgeMillis) {
                    file.delete()
                }
            }

            // Keep at most 10 recent PDF files in cache
            val remainingFiles = exportDir.listFiles { file ->
                file.isFile && file.name.endsWith(".pdf", ignoreCase = true)
            } ?: return
            if (remainingFiles.size > 10) {
                remainingFiles.sortedBy { it.lastModified() }
                    .take(remainingFiles.size - 10)
                    .forEach { it.delete() }
            }
        }
    }
}
