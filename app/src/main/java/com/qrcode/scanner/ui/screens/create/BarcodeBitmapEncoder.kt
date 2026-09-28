package com.qrcode.scanner.ui.screens.create

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

object BarcodeBitmapEncoder {

    fun encode(content: String, type: QrCategoryType): Bitmap {
        val matrix = encodeMatrix(content, type)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            it.setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    fun toSvg(content: String, type: QrCategoryType): String {
        val matrix = encodeMatrix(content, type)
        val width = matrix.width
        val height = matrix.height
        val sb = StringBuilder()
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $width $height" shape-rendering="crispEdges">""")
        sb.append("""<rect width="100%" height="100%" fill="#FFFFFF"/>""")
        sb.append("""<g fill="#000000">""")
        for (y in 0 until height) {
            var x = 0
            while (x < width) {
                if (!matrix[x, y]) {
                    x++
                    continue
                }
                val start = x
                while (x < width && matrix[x, y]) x++
                sb.append("""<rect x="$start" y="$y" width="${x - start}" height="1"/>""")
            }
        }
        sb.append("</g></svg>")
        return sb.toString()
    }

    private fun encodeMatrix(content: String, type: QrCategoryType): com.google.zxing.common.BitMatrix {
        val format = BarcodeSymbology.formatOf(type)
            ?: throw IllegalArgumentException("Not a barcode format")
        val (width, height) = pixelSize(format)
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 2
        )
        return MultiFormatWriter().encode(content, format, width, height, hints)
    }

    private fun pixelSize(format: BarcodeFormat): Pair<Int, Int> = when (format) {
        BarcodeFormat.AZTEC, BarcodeFormat.DATA_MATRIX -> 800 to 800
        BarcodeFormat.PDF_417 -> 1200 to 480
        else -> 1100 to 360
    }
}
