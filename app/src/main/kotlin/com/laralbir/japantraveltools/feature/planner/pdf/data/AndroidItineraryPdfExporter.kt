package com.laralbir.japantraveltools.feature.planner.pdf.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.laralbir.japantraveltools.core.datetime.TravelDateTimeFormatter
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.pdf.domain.PdfDocumentLayoutCalculator
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
        val paginatedStops = PdfDocumentLayoutCalculator.paginateStops(
            stops = stops,
            includeJapaneseNames = options.includeJapaneseNames
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
        var estimatedPages = 1 // Cover page

        for (dayIdx in 1..totalDays) {
            val dayStops = stopsByDay[dayIdx] ?: emptyList()
            val pages = PdfDocumentLayoutCalculator.paginateStops(
                stops = dayStops,
                includeJapaneseNames = options.includeJapaneseNames
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
