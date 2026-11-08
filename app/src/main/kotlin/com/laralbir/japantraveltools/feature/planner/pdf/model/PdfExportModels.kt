package com.laralbir.japantraveltools.feature.planner.pdf.model

import java.io.File

/**
 * Scope of PDF export: single day itinerary or full trip dossier.
 */
enum class PdfExportScope {
    SINGLE_DAY,
    FULL_TRIP
}

/**
 * User-configurable export options for generating the PDF document.
 */
data class PdfExportOptions(
    val scope: PdfExportScope,
    val targetDayIndex: Int? = null,
    val includeJapaneseNames: Boolean = true,
    val includeTransportSegments: Boolean = true,
    val includeEmergencyContacts: Boolean = true,
    val includeDayMap: Boolean = true
)

/**
 * Result of the PDF export containing reference to the generated file in cache.
 */
data class PdfExportResult(
    val file: File,
    val fileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long
) {
    val fileAbsolutePath: String get() = file.absolutePath
    val exists: Boolean get() = file.exists()
}
