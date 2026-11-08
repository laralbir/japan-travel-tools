package com.laralbir.japantraveltools.feature.planner.pdf.domain.repository

import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.pdf.model.PdfExportOptions
import com.laralbir.japantraveltools.feature.planner.pdf.model.PdfExportResult
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem

/**
 * Contract for 100% offline generation of itinerary PDF documents.
 */
interface ItineraryPdfExporter {

    /**
     * Exports a single day itinerary into an A4 PDF document.
     */
    suspend fun exportDayItinerary(
        planTitle: String,
        dayIndex: Int,
        dateLabel: String,
        stops: List<ItineraryStopItem>,
        transports: List<TransportSegmentItem>,
        options: PdfExportOptions
    ): PdfExportResult

    /**
     * Exports the complete trip dossier into a multi-page A4 PDF document.
     */
    suspend fun exportFullTrip(
        planTitle: String,
        startDateIso: String,
        totalDays: Int,
        stopsByDay: Map<Int, List<ItineraryStopItem>>,
        transports: List<TransportSegmentItem>,
        options: PdfExportOptions
    ): PdfExportResult
}
