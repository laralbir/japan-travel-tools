package com.laralbir.japantraveltools.feature.transport.pkpass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeBitmapGeneratorTest {

    @Test
    fun `generateMatrix returns valid matrix for QR code format`() {
        val matrix = BarcodeBitmapGenerator.generateMatrix(
            message = "https://www.renfe.com/ticket?id=WEHI1SHC",
            format = "PKBarcodeFormatQR",
            width = 250,
            height = 250
        )

        assertNotNull(matrix)
        assertTrue(matrix!!.width >= 200)
        assertTrue(matrix.height >= 200)
        // Verify at least one pixel is black and one is white
        var hasBlack = false
        var hasWhite = false
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) hasBlack = true else hasWhite = true
            }
        }
        assertTrue(hasBlack)
        assertTrue(hasWhite)
    }

    @Test
    fun `generateMatrix returns valid matrix for Aztec format`() {
        val matrix = BarcodeBitmapGenerator.generateMatrix(
            message = "1455286897676010720518114/03/202718:32",
            format = "PKBarcodeFormatAztec",
            width = 200,
            height = 200
        )

        assertNotNull(matrix)
        assertTrue(matrix!!.width > 0)
        assertTrue(matrix.height > 0)
    }

    @Test
    fun `generateMatrix returns valid matrix for PDF417 format`() {
        val matrix = BarcodeBitmapGenerator.generateMatrix(
            message = "M1LARA/CARLOS         ELH1807 074Y014A0033 147",
            format = "PKBarcodeFormatPDF417",
            width = 300,
            height = 100
        )

        assertNotNull(matrix)
        assertTrue(matrix!!.width > 0)
        assertTrue(matrix.height > 0)
    }

    @Test
    fun `generateMatrix returns null on blank message`() {
        val matrix = BarcodeBitmapGenerator.generateMatrix(
            message = "   ",
            format = "PKBarcodeFormatQR",
            width = 200,
            height = 200
        )
        assertNull(matrix)
    }

    @Test
    fun `generateMatrix defaults to QR Code when format is null or unknown`() {
        val matrix = BarcodeBitmapGenerator.generateMatrix(
            message = "RENFE-TICKET-SAMPLE",
            format = null,
            width = 200,
            height = 200
        )
        assertNotNull(matrix)
    }
}

