package com.laralbir.japantraveltools.feature.planner.map

import com.laralbir.japantraveltools.feature.documents.DocumentCategory
import com.laralbir.japantraveltools.feature.documents.TravelDocumentItem
import com.laralbir.japantraveltools.feature.documents.TravelGuideArticle
import com.laralbir.japantraveltools.feature.planner.ItineraryStopItem
import com.laralbir.japantraveltools.feature.planner.StopCategory
import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryLinkedResourcesResolverTest {

    @Test
    fun `resolveRelatedGuides returns matching guides by city or tag or title keyword`() {
        val stopKyoto = ItineraryStopItem(
            id = "stop-kyoto-1",
            dayNumber = 4,
            dateIso = "2026-11-14",
            startTime = "09:00",
            endTime = "11:30",
            title = "Fushimi Inari Taisha",
            category = StopCategory.SIGHTSEEING,
            placeNameJapanese = "伏見稲荷大社",
            latitude = 34.9671,
            longitude = 135.7727,
            notes = "Torii gates en Kioto"
        )

        val guideKyoto = TravelGuideArticle(
            id = "guide-kyoto",
            title = "Guía esencial de Kioto y templos",
            cityRegion = "Kioto",
            tags = listOf("templos", "fushimi", "sintoísmo"),
            markdownSummary = "Información sobre Kioto"
        )
        val guideTokyo = TravelGuideArticle(
            id = "guide-tokyo",
            title = "Guía de Shinjuku y Shibuya",
            cityRegion = "Tokio",
            tags = listOf("compras", "rascacielos"),
            markdownSummary = "Información sobre Tokio"
        )

        val resolved = ItineraryLinkedResourcesResolver.resolveRelatedGuides(
            stop = stopKyoto,
            allGuides = listOf(guideKyoto, guideTokyo)
        )

        assertEquals(1, resolved.size)
        assertEquals("guide-kyoto", resolved.first().id)
    }

    @Test
    fun `resolveRelatedTicketsAndTransport returns transport segments and documents for matching date or city`() {
        val stopMarch14 = ItineraryStopItem(
            id = "stop-ave",
            dayNumber = 1,
            dateIso = "2027-03-14",
            startTime = "18:00",
            endTime = "21:00",
            title = "Llegada en tren a Valencia",
            category = StopCategory.TRANSPORT,
            placeNameJapanese = "",
            latitude = 39.4699,
            longitude = -0.3763,
            notes = "AVE Valencia Joaquín Sorolla"
        )

        val segMarch14 = TransportSegmentItem(
            id = "seg-ave-14",
            order = 1,
            type = TransportMode.TRAIN_SHINKANSEN,
            operatorName = "Renfe",
            lineOrNumber = "AVE 05181",
            departureCode = "MAD",
            departureName = "Madrid",
            departureZoneId = "Europe/Madrid",
            departureTerminalOrPlatform = "Vía 1",
            departureTimeIso = "2027-03-14T18:32",
            arrivalCode = "VLC",
            arrivalName = "Valencia",
            arrivalZoneId = "Europe/Madrid",
            arrivalTerminalOrPlatform = "Vía 1",
            arrivalTimeIso = "2027-03-14T20:38",
            seatOrCarInfo = "Coche 7 · Asiento 1B",
            barcodeMessage = "AZTEC_RENFE_CODE"
        )

        val segMarch20 = TransportSegmentItem(
            id = "seg-later",
            order = 2,
            type = TransportMode.TRAIN_SHINKANSEN,
            operatorName = "JR Central",
            lineOrNumber = "Nozomi",
            departureCode = "TYO",
            departureName = "Tokyo",
            departureZoneId = "Asia/Tokyo",
            departureTerminalOrPlatform = "Track 14",
            departureTimeIso = "2027-03-20T08:00",
            arrivalCode = "KYO",
            arrivalName = "Kyoto",
            arrivalZoneId = "Asia/Tokyo",
            arrivalTerminalOrPlatform = "Track 1",
            arrivalTimeIso = "2027-03-20T10:15",
            seatOrCarInfo = "Car 4"
        )

        val docAve = TravelDocumentItem(
            id = "doc-ave-14",
            title = "Billete AVE Madrid ➔ Valencia",
            category = DocumentCategory.TRANSPORT,
            referenceOrNotes = "Salida: 2027-03-14 18:32",
            locatorCode = "WEHI1SHC",
            barcodeMessage = "AZTEC_RENFE_CODE"
        )

        val docOther = TravelDocumentItem(
            id = "doc-other",
            title = "Entrada Museo Tokio",
            category = DocumentCategory.TICKET,
            referenceOrNotes = "Fecha: 2027-03-25"
        )

        val result = ItineraryLinkedResourcesResolver.resolveRelatedTicketsAndTransport(
            stop = stopMarch14,
            allDocuments = listOf(docAve, docOther),
            allSegments = listOf(segMarch14, segMarch20)
        )

        assertEquals(1, result.relatedSegments.size)
        assertEquals("seg-ave-14", result.relatedSegments.first().id)

        assertEquals(1, result.relatedDocuments.size)
        assertEquals("doc-ave-14", result.relatedDocuments.first().id)
        assertTrue(result.hasAnyBarcode)
    }
}

