package com.qrcode.scanner.ui.screens.create

import android.content.Context
import com.qrcode.scanner.launcher.common.AnalyticsEvents

/**
 * Firebase events of the Create flow, named as in "QR Code Scanner & Generator - Firebase Event Naming Guide".
 * Only the QR types the guide names get a type-specific view event; the other app types (Location, App link, ...)
 * are covered by the generic [AnalyticsEvents.QR_EDIT_VIEW] only.
 */
internal object CreateAnalytics {
    private fun typeViewEvent(category: QrCategoryType): String? = when (category) {
        QrCategoryType.WEBSITE -> "general_website_view"
        QrCategoryType.PLAIN_TEXT -> "general_text_view"
        QrCategoryType.CONTACT -> "general_contact_view"
        QrCategoryType.PHONE -> "general_phone_view"
        QrCategoryType.EMAIL -> "general_email_view"
        QrCategoryType.SMS -> "general_sms_view"
        QrCategoryType.CALENDAR -> "general_calendar_view"
        QrCategoryType.WIFI -> "general_wifi_view"
        QrCategoryType.FACEBOOK -> "social_facebook_view"
        QrCategoryType.INSTAGRAM -> "social_instagram_view"
        QrCategoryType.LINKEDIN -> "social_linkedin_view"
        QrCategoryType.PAYPAL -> "social_paypal_view"
        QrCategoryType.SNAPCHAT -> "social_snapchat_view"
        QrCategoryType.SPOTIFY -> "social_spotify_view"
        QrCategoryType.TIKTOK -> "social_tiktok_view"
        QrCategoryType.TWITTER -> "social_twitter_view"
        QrCategoryType.WHATSAPP -> "social_whatsapp_view"
        QrCategoryType.YOUTUBE -> "social_youtube_view"
        else -> null
    }

    /** The create form of [category] opened. */
    fun logFormView(context: Context, category: QrCategoryType) {
        AnalyticsEvents.log(context, AnalyticsEvents.QR_EDIT_VIEW, "qr_type", category.name)
        typeViewEvent(category)?.let { AnalyticsEvents.log(context, it) }
        if (category == QrCategoryType.BARCODE || BarcodeSymbology.isBarcode(category)) {
            AnalyticsEvents.log(context, AnalyticsEvents.BARCODE_VIEW, "qr_type", category.name)
        }
    }

    /** The user pressed Generate and the form input is valid: the preview opens next. */
    fun logGenerateStart(context: Context, category: QrCategoryType) {
        AnalyticsEvents.log(context, AnalyticsEvents.QR_GENERATE_START, "qr_type", category.name)
    }
}
