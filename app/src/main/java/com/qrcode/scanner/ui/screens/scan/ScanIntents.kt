package com.qrcode.scanner.ui.screens.scan

import android.content.Context
import android.content.Intent
import android.net.Uri

object ScanIntents {
    const val EXTRA_RAW_VALUE = "extra_scan_raw_value"
    const val EXTRA_BARCODE_FORMAT = "extra_scan_barcode_format"
    const val EXTRA_BARCODE_FORMAT_NAME = "extra_scan_barcode_format_name"
    /** When > 0, ScanResultActivity loads this row and must not insert History again. */
    const val EXTRA_HISTORY_ID = "extra_history_id"
    /**
     * When true, ScanResultActivity must not Auto-Open the browser.
     * Used for Continuous Batch detail (Batch Results remains the review point).
     */
    const val EXTRA_SKIP_AUTO_OPEN = "extra_skip_auto_open"

    const val EXTRA_IMAGE_URI = "extra_gallery_image_uri"
    const val EXTRA_SCAN_MODE = "extra_gallery_scan_mode"
    const val EXTRA_ERROR_REASON = "extra_detection_error_reason"
    const val EXTRA_AUTO_PICK = "extra_gallery_auto_pick"

    const val MODE_QR = "qr"
    const val MODE_BARCODE = "barcode"
    const val MODE_BATCH = "batch"

    const val EXTRA_BATCH_ITEMS = "extra_continuous_batch_items"

    fun openScanner(context: Context): Intent =
        Intent(context, ScannerActivity::class.java)

    fun openScanResult(
        context: Context,
        rawValue: String,
        format: Int,
        formatName: String,
        historyId: Long = -1L,
        skipAutoOpen: Boolean = false
    ): Intent =
        Intent(context, ScanResultActivity::class.java).apply {
            putExtra(EXTRA_RAW_VALUE, rawValue)
            putExtra(EXTRA_BARCODE_FORMAT, format)
            putExtra(EXTRA_BARCODE_FORMAT_NAME, formatName)
            if (historyId > 0L) {
                putExtra(EXTRA_HISTORY_ID, historyId)
            }
            if (skipAutoOpen) {
                putExtra(EXTRA_SKIP_AUTO_OPEN, true)
            }
        }

    fun openContinuousBatchResult(
        context: Context,
        items: List<ContinuousBatchItem>
    ): Intent =
        Intent(context, ContinuousBatchResultActivity::class.java).apply {
            putExtra(EXTRA_BATCH_ITEMS, ArrayList(items))
        }

    /** Opens crop screen; if [imageUri] is null, crop activity launches the system picker. */
    fun openGalleryCrop(
        context: Context,
        imageUri: Uri? = null,
        scanMode: String = MODE_BATCH
    ): Intent =
        Intent(context, GalleryCropActivity::class.java).apply {
            if (imageUri != null) {
                putExtra(EXTRA_IMAGE_URI, imageUri.toString())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                putExtra(EXTRA_AUTO_PICK, true)
            }
            putExtra(EXTRA_SCAN_MODE, scanMode)
        }

    fun openDetectionError(
        context: Context,
        imageUri: Uri? = null,
        reason: String? = null,
        scanMode: String = MODE_BATCH
    ): Intent =
        Intent(context, DetectionErrorActivity::class.java).apply {
            if (imageUri != null) {
                putExtra(EXTRA_IMAGE_URI, imageUri.toString())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (reason != null) putExtra(EXTRA_ERROR_REASON, reason)
            putExtra(EXTRA_SCAN_MODE, scanMode)
        }
}
