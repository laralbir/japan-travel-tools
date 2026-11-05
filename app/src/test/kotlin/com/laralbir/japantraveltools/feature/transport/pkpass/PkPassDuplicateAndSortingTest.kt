package com.laralbir.japantraveltools.feature.transport.pkpass

import com.laralbir.japantraveltools.feature.transport.TransportMode
import com.laralbir.japantraveltools.feature.transport.TransportSegmentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PkPassDuplicateAndSortingTest {

    private fun createSegment(
        id: String,
        order: Int,
        lineOrNumber: String,
        departureTimeIso: String,
        notes: String = ""
    ): TransportSegmentItem = TransportSegmentItem(
        id = id,
        order = order,
        type = TransportMode.TRAIN_SHINKANSEN,
        operatorName = "Renfe",
        lineOrNumber = lineOrNumber,
        departureCode = "MAD",
        departureName = "Madrid",
        departureZoneId = "Europe/Madrid",
        departureTerminalOrPlatform = "Vía 1",
        departureTimeIso = departureTimeIso,
        arrivalCode = "VLC",
        arrivalName = "Valencia",
        arrivalZoneId = "Europe/Madrid",
        arrivalTerminalOrPlatform = "Vía 1",
        arrivalTimeIso = "${departureTimeIso.substringBefore('T')}T22:00",
        seatOrCarInfo = "Coche 1",
        stationExitOrBelt = notes,
        passOrPaymentBadge = "Pass"
    )

    @Test
    fun `reorderChronologically sorts segments by departureTimeIso and reindexes orders 1 to N`() {
        val segMarch26 = createSegment("seg-3", 1, "AVE 05070", "2027-03-26T07:30")
        val segMarch14 = createSegment("seg-1", 2, "AVE 05181", "2027-03-14T18:32")
        val segMarch15 = createSegment("seg-2", 3, "LH 1807", "2027-03-15T07:25")

        val sorted = PkPassParser.reorderChronologically(listOf(segMarch26, segMarch14, segMarch15))

        assertEquals("seg-1", sorted[0].id)
        assertEquals(1, sorted[0].order)
        assertEquals("2027-03-14T18:32", sorted[0].departureTimeIso)

        assertEquals("seg-2", sorted[1].id)
        assertEquals(2, sorted[1].order)
        assertEquals("2027-03-15T07:25", sorted[1].departureTimeIso)

        assertEquals("seg-3", sorted[2].id)
        assertEquals(3, sorted[2].order)
        assertEquals("2027-03-26T07:30", sorted[2].departureTimeIso)
    }

    @Test
    fun `findDuplicate returns existing segment when bookingCode matches`() {
        val existing = listOf(
            createSegment("seg-1", 1, "AVE 05181", "2027-03-14T18:32", notes = "Localizador: WEHI1SHC")
        )

        val pass = ParsedPkPass(
            organizationName = "Renfe",
            description = "Billete",
            transitType = "PKTransitTypeTrain",
            originName = "València",
            destinationName = "Madrid",
            departureDateIso = "2027-03-14",
            departureTime = "18:32",
            arrivalDateIso = "2027-03-14",
            arrivalTime = "20:38",
            operatorName = "Renfe",
            serviceNumber = "AVE 05181",
            carriage = "7",
            seat = "1B",
            passengerName = "Carlos",
            bookingCode = "WEHI1SHC",
            ticketNumber = "1455286897676",
            barcodeMessage = "QR_DATA",
            barcodeFormat = "PKBarcodeFormatAztec",
            barcodeAltText = "1455286897676",
            rawJson = "{}"
        )

        val duplicate = PkPassParser.findDuplicate(pass, existing)
        assertNotNull(duplicate)
        assertEquals("seg-1", duplicate!!.id)
    }

    @Test
    fun `findDuplicate returns existing segment when serviceNumber and departureTime match`() {
        val existing = listOf(
            createSegment("seg-ave", 1, "AVE 05181", "2027-03-14T18:32")
        )

        val pass = ParsedPkPass(
            organizationName = "Renfe",
            description = "Billete",
            transitType = "PKTransitTypeTrain",
            originName = "València",
            destinationName = "Madrid",
            departureDateIso = "2027-03-14",
            departureTime = "18:32",
            arrivalDateIso = "2027-03-14",
            arrivalTime = "20:38",
            operatorName = "Renfe",
            serviceNumber = "AVE 05181",
            carriage = "7",
            seat = "1B",
            passengerName = "Carlos",
            bookingCode = null,
            ticketNumber = null,
            barcodeMessage = "QR_DATA",
            barcodeFormat = null,
            barcodeAltText = null,
            rawJson = "{}"
        )

        val duplicate = PkPassParser.findDuplicate(pass, existing)
        assertNotNull(duplicate)
        assertEquals("seg-ave", duplicate!!.id)
    }

    @Test
    fun `findDuplicate returns null when pass is completely different`() {
        val existing = listOf(
            createSegment("seg-1", 1, "AVE 05181", "2027-03-14T18:32")
        )

        val pass = ParsedPkPass(
            organizationName = "Renfe",
            description = "Billete",
            transitType = "PKTransitTypeTrain",
            originName = "Madrid",
            destinationName = "València",
            departureDateIso = "2027-03-26",
            departureTime = "07:30",
            arrivalDateIso = "2027-03-26",
            arrivalTime = "09:30",
            operatorName = "Renfe",
            serviceNumber = "AVE 05070",
            carriage = "3",
            seat = "8A",
            passengerName = "Carlos",
            bookingCode = "OTHER",
            ticketNumber = null,
            barcodeMessage = null,
            barcodeFormat = null,
            barcodeAltText = null,
            rawJson = "{}"
        )

        val duplicate = PkPassParser.findDuplicate(pass, existing)
        assertNull(duplicate)
    }
}
