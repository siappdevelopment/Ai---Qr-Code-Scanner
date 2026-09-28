package com.qrcode.scanner.ui.screens.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.media.ExifInterface
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Shared gallery image decode + ML Kit detection for Phase 9.
 * Format sets mirror [ScannerViewfinderScreen] QR / Barcode / Batch modes.
 */
object GalleryScanHelper {

    private const val TAG = "GalleryScanHelper"
    private const val MAX_DECODE_EDGE = 2048

    data class DetectedCode(
        val rawValue: String,
        val format: Int,
        val formatName: String
    )

    fun barcodeOptionsFor(mode: String): BarcodeScannerOptions {
        val formats = when (mode) {
            ScanIntents.MODE_QR -> intArrayOf(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_DATA_MATRIX
            )
            ScanIntents.MODE_BARCODE -> intArrayOf(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_PDF417
            )
            else -> intArrayOf(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_PDF417
            )
        }
        return BarcodeScannerOptions.Builder()
            .setBarcodeFormats(formats[0], *formats.copyOfRange(1, formats.size))
            .build()
    }

    fun formatName(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_ITF -> "ITF"
        else -> "UNKNOWN"
    }

    fun decodeBitmap(
        context: Context,
        uri: Uri,
        maxEdge: Int = MAX_DECODE_EDGE
    ): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            val longest = max(bounds.outWidth, bounds.outHeight)
            while (longest / sample > maxEdge) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: run {
                // Fallback for file:// URIs some resolvers won't stream
                if (uri.scheme == "file") {
                    BitmapFactory.decodeFile(uri.path, opts)
                } else {
                    null
                }
            } ?: return null

            Log.d(TAG, "decodeBitmap ok ${decoded.width}x${decoded.height} uri=$uri sample=$sample")
            applyExifOrientation(context, uri, decoded)
        } catch (e: Exception) {
            Log.w(TAG, "decodeBitmap failed for $uri", e)
            null
        }
    }

    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val orientation = context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                ExifInterface(pfd.fileDescriptor).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                if (it != bitmap) bitmap.recycle()
            }
        } catch (e: Exception) {
            Log.w(TAG, "EXIF orientation failed", e)
            bitmap
        }
    }

    fun transformBitmap(
        source: Bitmap,
        rotationDegrees: Int,
        flipHorizontal: Boolean
    ): Bitmap {
        if (rotationDegrees % 360 == 0 && !flipHorizontal) return source
        val matrix = Matrix()
        if (flipHorizontal) matrix.postScale(-1f, 1f)
        if (rotationDegrees % 360 != 0) matrix.postRotate((rotationDegrees % 360).toFloat())
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * [cropNorm] is left/top/right/bottom in 0..1 relative to the transformed image.
     */
    fun cropBitmap(
        source: Bitmap,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Bitmap {
        val l = (left.coerceIn(0f, 1f) * source.width).roundToInt().coerceIn(0, source.width - 1)
        val t = (top.coerceIn(0f, 1f) * source.height).roundToInt().coerceIn(0, source.height - 1)
        val r = (right.coerceIn(0f, 1f) * source.width).roundToInt().coerceIn(l + 1, source.width)
        val b = (bottom.coerceIn(0f, 1f) * source.height).roundToInt().coerceIn(t + 1, source.height)
        return Bitmap.createBitmap(source, l, t, r - l, b - t)
    }

    suspend fun detect(bitmap: Bitmap, mode: String): List<DetectedCode> =
        suspendCancellableCoroutine { cont ->
            Log.d(
                TAG,
                "detect start mode=$mode size=${bitmap.width}x${bitmap.height} config=${bitmap.config}"
            )
            val scanner = BarcodeScanning.getClient(barcodeOptionsFor(mode))
            cont.invokeOnCancellation {
                runCatching { scanner.close() }
            }
            // Copy to a stable ARGB_8888 software bitmap; ML Kit can miss codes on
            // recycled/aliased or unexpected configs from createBitmap crops.
            val forScan = if (
                bitmap.config == Bitmap.Config.ARGB_8888 && !bitmap.isRecycled
            ) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            } else {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            }
            val image = InputImage.fromBitmap(forScan, 0)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    Log.d(TAG, "detect success count=${barcodes.size}")
                    val results = barcodes.mapNotNull { barcode ->
                        val raw = barcode.rawValue?.takeIf { it.isNotBlank() }
                            ?: barcode.displayValue?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null
                        DetectedCode(
                            rawValue = raw,
                            format = barcode.format,
                            formatName = formatName(barcode.format)
                        )
                    }
                    if (forScan !== bitmap && !forScan.isRecycled) {
                        forScan.recycle()
                    }
                    if (cont.isActive) cont.resume(results)
                    runCatching { scanner.close() }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "ML Kit gallery detect failed", e)
                    if (forScan !== bitmap && !forScan.isRecycled) {
                        forScan.recycle()
                    }
                    if (cont.isActive) cont.resume(emptyList())
                    runCatching { scanner.close() }
                }
        }
}
