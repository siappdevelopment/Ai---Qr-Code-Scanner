package com.qrcode.scanner.ui.screens.scan

import android.content.Context
import android.content.Intent

object ScanIntents {
    const val EXTRA_RAW_VALUE = "extra_scan_raw_value"
    const val EXTRA_BARCODE_FORMAT = "extra_scan_barcode_format"
    const val EXTRA_BARCODE_FORMAT_NAME = "extra_scan_barcode_format_name"

    fun openScanner(context: Context): Intent =
        Intent(context, ScannerActivity::class.java)

    fun openScanResult(
        context: Context,
        rawValue: String,
        format: Int,
        formatName: String
    ): Intent =
        Intent(context, ScanResultActivity::class.java).apply {
            putExtra(EXTRA_RAW_VALUE, rawValue)
            putExtra(EXTRA_BARCODE_FORMAT, format)
            putExtra(EXTRA_BARCODE_FORMAT_NAME, formatName)
        }
}
