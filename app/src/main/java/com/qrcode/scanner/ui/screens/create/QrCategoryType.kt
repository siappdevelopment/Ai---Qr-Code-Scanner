package com.qrcode.scanner.ui.screens.create

import android.content.Context
import androidx.annotation.StringRes
import com.qrcode.scanner.app.R

/**
 * Create-flow QR categories from Stitch Create QR Category Hub
 * (79741836d19e4053a015ef8e4e2cf53f).
 *
 * Grouping from Stitch form screens:
 * - Common shell: Create Website QR Form (eeb194fd…) — URL / Text / vCard tabs
 * - Dedicated: Create Wi-Fi QR Form (cd2a1970…) — SSID / security / password / hidden
 * - Remaining hub categories: no dedicated Stitch form yet → Common shell + type
 */
enum class QrCategoryType(@StringRes val titleRes: Int) {
    WEBSITE(R.string.create_cat_website_title),
    PLAIN_TEXT(R.string.create_cat_plain_text_title),
    CONTACT(R.string.create_cat_contact_title),
    PHONE(R.string.create_cat_phone_title),
    EMAIL(R.string.create_cat_email_title),
    SMS(R.string.create_cat_sms_title),
    WHATSAPP(R.string.create_cat_whatsapp_title),
    LOCATION(R.string.create_cat_location_title),
    CALENDAR(R.string.create_cat_calendar_title),
    APP_LINK(R.string.create_cat_app_link_title),
    BARCODE(R.string.create_cat_barcode_title),
    WIFI(R.string.create_cat_wifi_title),
    FACEBOOK(R.string.create_cat_facebook_title),
    YOUTUBE(R.string.create_cat_youtube_title),
    TWITTER(R.string.create_cat_twitter_title),
    TIKTOK(R.string.create_cat_tiktok_title),
    INSTAGRAM(R.string.create_cat_instagram_title),
    PAYPAL(R.string.create_cat_paypal_title),
    SNAPCHAT(R.string.create_cat_snapchat_title),
    LINKEDIN(R.string.create_cat_linkedin_title),
    SPOTIFY(R.string.create_cat_spotify_title),
    CODE_128(R.string.create_cat_code_128_title),
    DATA_MATRIX(R.string.create_cat_data_matrix_title),
    PDF_417(R.string.create_cat_pdf_417_title),
    AZTEC(R.string.create_cat_aztec_title),
    EAN_13(R.string.create_cat_ean_13_title),
    EAN_8(R.string.create_cat_ean_8_title),
    UPC_E(R.string.create_cat_upc_e_title),
    UPC_A(R.string.create_cat_upc_a_title),
    CODE_93(R.string.create_cat_code_93_title),
    CODE_39(R.string.create_cat_code_39_title),
    CODABAR(R.string.create_cat_codabar_title),
    ITF(R.string.create_cat_itf_title);

    fun displayTitle(context: Context): String = context.getString(titleRes)

    val usesDedicatedWifiActivity: Boolean
        get() = this == WIFI

    companion object {
        const val EXTRA_QR_CATEGORY = "extra_qr_category"

        fun fromIntentExtra(value: String?): QrCategoryType =
            entries.firstOrNull { it.name == value } ?: WEBSITE
    }
}
