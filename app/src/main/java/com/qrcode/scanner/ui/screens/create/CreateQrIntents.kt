package com.qrcode.scanner.ui.screens.create

import android.content.Context
import android.content.Intent

object CreateQrIntents {
    const val EXTRA_PAYLOAD = "extra_qr_payload"
    const val EXTRA_DISPLAY_TITLE = "extra_qr_display_title"
    const val EXTRA_DETECTED_TYPE = "extra_qr_detected_type"
    const val EXTRA_ECC_LEVEL = "extra_qr_ecc_level"
    const val RESULT_SAVED_HISTORY_ID = "result_saved_history_id"

    fun openCategory(context: Context, type: QrCategoryType): Intent {
        return if (type.usesDedicatedWifiActivity) {
            Intent(context, WifiQrActivity::class.java).apply {
                putExtra(QrCategoryType.EXTRA_QR_CATEGORY, type.name)
            }
        } else {
            Intent(context, CommonQrFormActivity::class.java).apply {
                putExtra(QrCategoryType.EXTRA_QR_CATEGORY, type.name)
            }
        }
    }

    fun openPreview(
        context: Context,
        category: QrCategoryType,
        payload: String,
        displayTitle: String,
        detectedType: String,
        eccLevel: String = QrBitmapEncoder.EccLevel.H.name
    ): Intent =
        Intent(context, QrPreviewActivity::class.java).apply {
            putExtra(QrCategoryType.EXTRA_QR_CATEGORY, category.name)
            putExtra(EXTRA_PAYLOAD, payload)
            putExtra(EXTRA_DISPLAY_TITLE, displayTitle)
            putExtra(EXTRA_DETECTED_TYPE, detectedType)
            putExtra(EXTRA_ECC_LEVEL, eccLevel)
        }
}
