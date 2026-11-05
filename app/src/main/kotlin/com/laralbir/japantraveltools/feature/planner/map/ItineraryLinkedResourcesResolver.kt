package com.laralbir.japantraveltools.feature.planner.map

import com.laralbir.japantraveltools.feature.documents.TravelDocumentItem
import com.laralbir.japantraveltools.feature.documents.TravelGuideArticle
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem

data class LinkedResourcesResult(
    val relatedGuides: List<TravelGuideArticle>,
    val relatedDocuments: List<TravelDocumentItem>,
    val relatedSegments: List<TransportSegmentItem>,
    val hasAnyBarcode: Boolean
)

object ItineraryLinkedResourcesResolver {

    /**
     * Resolves travel guide articles related to an itinerary stop by matching city/region,
     * tags, or keywords in title and notes.
     */
    fun resolveRelatedGuides(
        stop: ItineraryStopItem,
        allGuides: List<TravelGuideArticle>
    ): List<TravelGuideArticle> {
        val stopText = buildString {
            append(stop.title.lowercase())
            append(" ")
            append(stop.placeNameJapanese.lowercase())
            append(" ")
            append(stop.notes.lowercase())
        }

        return allGuides.filter { guide ->
            val cityRegion = guide.cityRegion.lowercase().trim()
            val matchesCity = cityRegion.isNotBlank() && (
                stopText.contains(cityRegion) ||
                    (cityRegion == "kioto" && stopText.contains("kyoto")) ||
                    (cityRegion == "kyoto" && stopText.contains("kioto")) ||
                    (cityRegion == "tokio" && stopText.contains("tokyo")) ||
                    (cityRegion == "tokyo" && stopText.contains("tokio"))
            )

            val matchesTag = guide.tags.any { tag ->
                val cleanTag = tag.lowercase().trim()
                cleanTag.length >= 3 && stopText.contains(cleanTag)
            }

            val guideTitleWords = guide.title.lowercase().split("\\s+".toRegex())
                .filter { it.length >= 4 && !it.contains("guía") }
            val matchesTitle = guideTitleWords.any { word -> stopText.contains(word) }

            matchesCity || matchesTag || matchesTitle
        }
    }

    /**
     * Resolves travel documents (tickets, reservations) and transport segments related to an itinerary stop
     * by matching date, city names, locator codes or explicit transport segment links.
     */
    fun resolveRelatedTicketsAndTransport(
        stop: ItineraryStopItem,
        allDocuments: List<TravelDocumentItem>,
        allSegments: List<TransportSegmentItem>
    ): LinkedResourcesResult {
        val stopDate = stop.dateIso.trim()
        val stopText = buildString {
            append(stop.title.lowercase())
            append(" ")
            append(stop.notes.lowercase())
        }

        // 1. Resolve matching transport segments
        val matchedSegments = allSegments.filter { seg ->
            val matchesDate = stopDate.isNotBlank() && (
                seg.departureTimeIso.startsWith(stopDate) || seg.arrivalTimeIso.startsWith(stopDate)
            )

            val depName = seg.departureName.lowercase().trim()
            val arrName = seg.arrivalName.lowercase().trim()
            val lineOrNum = seg.lineOrNumber.lowercase().trim()

            val matchesPlace = (depName.length >= 3 && stopText.contains(depName)) ||
                (arrName.length >= 3 && stopText.contains(arrName)) ||
                (lineOrNum.length >= 3 && stopText.contains(lineOrNum))

            matchesDate || matchesPlace
        }

        val matchedSegmentIds = matchedSegments.map { it.id }.toSet()

        // 2. Resolve matching documents
        val matchedDocuments = allDocuments.filter { doc ->
            // Matched by linked transport segment ID
            val isLinkedToMatchedSegment = doc.linkedTransportSegmentId != null &&
                matchedSegmentIds.contains(doc.linkedTransportSegmentId)

            // Matched by date inside referenceOrNotes
            val matchesDate = stopDate.isNotBlank() && doc.referenceOrNotes.contains(stopDate)

            // Matched by keywords in title or reference
            val docTitle = doc.title.lowercase()
            val matchesKeyword = (docTitle.length >= 4 && stopText.contains(docTitle)) ||
                stopText.split("\\s+".toRegex()).any { word ->
                    word.length >= 4 && docTitle.contains(word)
                }

            isLinkedToMatchedSegment || matchesDate || matchesKeyword
        }

        val hasAnyBarcode = matchedDocuments.any { !it.barcodeMessage.isNullOrBlank() } ||
            matchedSegments.any { !it.barcodeMessage.isNullOrBlank() }

        return LinkedResourcesResult(
            relatedGuides = emptyList(),
            relatedDocuments = matchedDocuments,
            relatedSegments = matchedSegments,
            hasAnyBarcode = hasAnyBarcode
        )
    }

    /**
     * Convenience function to resolve both guides and tickets/transport segments at once.
     */
    fun resolveAll(
        stop: ItineraryStopItem,
        allGuides: List<TravelGuideArticle>,
        allDocuments: List<TravelDocumentItem>,
        allSegments: List<TransportSegmentItem>
    ): LinkedResourcesResult {
        val guides = resolveRelatedGuides(stop, allGuides)
        val tickets = resolveRelatedTicketsAndTransport(stop, allDocuments, allSegments)
        return tickets.copy(relatedGuides = guides)
    }
}

