package com.laralbir.japantraveltools.feature.transport.pkpass

import com.laralbir.japantraveltools.feature.transport.TransportMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PkPassParserTest {

    private val sampleRenfePassJson = """
    {
      "formatVersion": 1,
      "passTypeIdentifier": "pass.com.renfe.viajeros",
      "serialNumber": "1455286897676",
      "relevantDate": "2027-03-14T18:32:00+01:00",
      "organizationName": "Renfe",
      "description": "Billete de tren",
      "barcode": {
        "message": "1455286897676010720518114/03/202718:32",
        "format": "PKBarcodeFormatAztec",
        "altText": "1455286897676"
      },
      "boardingPass": {
        "transitType": "PKTransitTypeTrain",
        "headerFields": [
          {
            "key": "date",
            "label": "VIAJE A: Madrid-Chamartín-Clara Campoamor",
            "value": "14/03/2027"
          }
        ],
        "primaryFields": [
          {
            "key": "depart",
            "label": "València-Joaquín Sorolla",
            "value": "18:32"
          },
          {
            "key": "arrive",
            "label": "Madrid-Chamartín-Clara Campoamor",
            "value": "20:38"
          }
        ],
        "secondaryFields": [
          {
            "key": "passenger",
            "label": "PASAJERO",
            "value": "Viajero Ejemplo"
          },
          {
            "key": "bookingNumber",
            "label": "LOCALIZADOR",
            "value": "WEHI1SHC"
          }
        ],
        "auxiliaryFields": [
          {
            "key": "service",
            "label": "TREN AVE",
            "value": "05181"
          },
          {
            "key": "carriage",
            "label": "COCHE",
            "value": "007"
          },
          {
            "key": "seat",
            "label": "ASIENTO",
            "value": "1B"
          },
          {
            "key": "comfortZone",
            "label": "ESPACIO",
            "value": "Estándar"
          }
        ],
        "backFields": [
          {
            "key": "ticketNumber",
            "label": "Número de billete",
            "value": "1455286897676"
          }
        ]
      }
    }
    """.trimIndent()

    private val sampleFlightPassJson = """
    {
      "formatVersion": 1,
      "passTypeIdentifier": "pass.com.lufthansa.boarding",
      "serialNumber": "LH-1807-MAD",
      "relevantDate": "2027-03-15T07:25:00+01:00",
      "organizationName": "Lufthansa",
      "description": "Boarding Pass",
      "boardingPass": {
        "transitType": "PKTransitTypeAir",
        "headerFields": [
          {
            "key": "gate",
            "label": "GATE",
            "value": "D62"
          }
        ],
        "primaryFields": [
          {
            "key": "origin",
            "label": "Madrid Barajas (MAD)",
            "value": "MAD"
          },
          {
            "key": "destination",
            "label": "Munich Franz Josef Strauss (MUC)",
            "value": "MUC"
          }
        ],
        "secondaryFields": [
          {
            "key": "flight",
            "label": "FLIGHT",
            "value": "LH1807"
          },
          {
            "key": "bookingRef",
            "label": "BOOKING",
            "value": "LHXYZ1"
          }
        ],
        "auxiliaryFields": [
          {
            "key": "seat",
            "label": "SEAT",
            "value": "14A"
          },
          {
            "key": "boarding",
            "label": "BOARDING",
            "value": "06:45"
          }
        ]
      }
    }
    """.trimIndent()

    @Test
    fun `parsePassJson parses Renfe train ticket correctly`() {
        val result = PkPassParser.parsePassJson(sampleRenfePassJson)
        assertTrue(result.isSuccess)
        val pass = result.getOrThrow()

        assertEquals("Renfe", pass.organizationName)
        assertEquals("PKTransitTypeTrain", pass.transitType)
        assertEquals("València-Joaquín Sorolla", pass.originName)
        assertEquals("Madrid-Chamartín-Clara Campoamor", pass.destinationName)
        assertEquals("2027-03-14", pass.departureDateIso)
        assertEquals("18:32", pass.departureTime)
        assertEquals("20:38", pass.arrivalTime)
        assertEquals("AVE 05181", pass.serviceNumber)
        assertEquals("007", pass.carriage)
        assertEquals("1B", pass.seat)
        assertEquals("Viajero Ejemplo", pass.passengerName)
        assertEquals("WEHI1SHC", pass.bookingCode)
        assertEquals("1455286897676", pass.ticketNumber)
        assertEquals("1455286897676", pass.barcodeAltText)
    }

    @Test
    fun `toTransportSegment converts Renfe pass to TransportSegmentItem`() {
        val pass = PkPassParser.parsePassJson(sampleRenfePassJson).getOrThrow()
        val segment = PkPassParser.toTransportSegment(pass, order = 1)

        assertEquals(TransportMode.TRAIN_SHINKANSEN, segment.type)
        assertEquals("Renfe", segment.operatorName)
        assertEquals("AVE 05181", segment.lineOrNumber)
        assertEquals("València-Joaquín Sorolla", segment.departureName)
        assertEquals("Madrid-Chamartín-Clara Campoamor", segment.arrivalName)
        assertEquals("2027-03-14T18:32", segment.departureTimeIso)
        assertEquals("2027-03-14T20:38", segment.arrivalTimeIso)
        assertTrue(segment.seatOrCarInfo.contains("Coche 007"))
        assertTrue(segment.seatOrCarInfo.contains("Asiento 1B"))
        assertTrue(segment.stationExitOrBelt.contains("WEHI1SHC"))
    }

    @Test
    fun `parsePassJson parses airline flight pass correctly`() {
        val result = PkPassParser.parsePassJson(sampleFlightPassJson)
        assertTrue(result.isSuccess)
        val pass = result.getOrThrow()

        assertEquals("Lufthansa", pass.organizationName)
        assertEquals("PKTransitTypeAir", pass.transitType)
        assertEquals("LH1807", pass.serviceNumber)
        assertEquals("14A", pass.seat)
        assertEquals("06:45", pass.boardingTime)

        val segment = PkPassParser.toTransportSegment(pass, order = 2)
        assertEquals(TransportMode.FLIGHT, segment.type)
        assertEquals("Lufthansa", segment.operatorName)
        assertEquals("LH1807", segment.lineOrNumber)
    }

    @Test
    fun `parsePkPass unzips and parses pass from zip stream`() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("pass.json"))
            zos.write(sampleRenfePassJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val bais = ByteArrayInputStream(baos.toByteArray())
        val result = PkPassParser.parsePkPass(bais)
        assertTrue(result.isSuccess)
        val pass = result.getOrThrow()
        assertEquals("Renfe", pass.organizationName)
        assertEquals("AVE 05181", pass.serviceNumber)
    }
}
