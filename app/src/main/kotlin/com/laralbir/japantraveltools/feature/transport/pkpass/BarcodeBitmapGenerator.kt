package com.laralbir.japantraveltools.feature.transport.pkpass

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.util.EnumMap

data class BarcodeMatrix(
    val width: Int,
    val height: Int,
    private val bits: BooleanArray
) {
    fun get(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        return bits[y * width + x]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BarcodeMatrix
        if (width != other.width) return false
        if (height != other.height) return false
        if (!bits.contentEquals(other.bits)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + bits.contentHashCode()
        return result
    }
}

object BarcodeBitmapGenerator {

    fun mapAppleFormatToZxing(format: String?): BarcodeFormat {
        return when (format?.trim()) {
            "PKBarcodeFormatAztec" -> BarcodeFormat.AZTEC
            "PKBarcodeFormatPDF417" -> BarcodeFormat.PDF_417
            "PKBarcodeFormatCode128" -> BarcodeFormat.CODE_128
            "PKBarcodeFormatQR" -> BarcodeFormat.QR_CODE
            else -> BarcodeFormat.QR_CODE
        }
    }

    fun generateMatrix(
        message: String?,
        format: String?,
        width: Int = 300,
        height: Int = 300
    ): BarcodeMatrix? {
        if (message.isNullOrBlank()) return null
        val zxingFormat = mapAppleFormatToZxing(format)
        val targetWidth = width.coerceAtLeast(100)
        val targetHeight = height.coerceAtLeast(100)

        return runCatching {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                if (zxingFormat == BarcodeFormat.QR_CODE) {
                    put(EncodeHintType.MARGIN, 1)
                }
            }
            val bitMatrix = MultiFormatWriter().encode(message, zxingFormat, targetWidth, targetHeight, hints)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val array = BooleanArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    array[y * w + x] = bitMatrix.get(x, y)
                }
            }
            BarcodeMatrix(w, h, array)
        }.getOrNull()
    }
}
