package com.qrcode.scanner.ui.screens.create

/**
 * Create-flow QR categories from Stitch Create QR Category Hub
 * (79741836d19e4053a015ef8e4e2cf53f).
 *
 * Grouping from Stitch form screens:
 * - Common shell: Create Website QR Form (eeb194fd…) — URL / Text / vCard tabs
 * - Dedicated: Create Wi-Fi QR Form (cd2a1970…) — SSID / security / password / hidden
 * - Remaining hub categories: no dedicated Stitch form yet → Common shell + type
 */
enum class QrCategoryType(val displayTitle: String) {
    WEBSITE("Website URL"),
    PLAIN_TEXT("Plain Text"),
    CONTACT("Contact / vCard"),
    PHONE("Phone Call"),
    EMAIL("Email Message"),
    SMS("SMS / Message"),
    WHATSAPP("WhatsApp"),
    LOCATION("Location"),
    CALENDAR("Calendar Event"),
    APP_LINK("App Link"),
    BARCODE("Barcode / EAN"),
    WIFI("Wi-Fi Network"),
    FACEBOOK("Facebook"),
    YOUTUBE("YouTube"),
    TWITTER("Twitter"),
    TIKTOK("TikTok"),
    INSTAGRAM("Instagram"),
    PAYPAL("PayPal"),
    SNAPCHAT("Snapchat"),
    LINKEDIN("LinkedIn"),
    SPOTIFY("Spotify"),
    CODE_128("Code 128"),
    DATA_MATRIX("Data Matrix"),
    PDF_417("PDF 417"),
    AZTEC("Aztec"),
    EAN_13("EAN 13"),
    EAN_8("EAN 8"),
    UPC_E("UPC E"),
    UPC_A("UPC A"),
    CODE_93("Code 93"),
    CODE_39("Code 39"),
    CODABAR("Codabar"),
    ITF("ITF");

    val usesDedicatedWifiActivity: Boolean
        get() = this == WIFI

    companion object {
        const val EXTRA_QR_CATEGORY = "extra_qr_category"

        fun fromIntentExtra(value: String?): QrCategoryType =
            entries.firstOrNull { it.name == value } ?: WEBSITE
    }
}
