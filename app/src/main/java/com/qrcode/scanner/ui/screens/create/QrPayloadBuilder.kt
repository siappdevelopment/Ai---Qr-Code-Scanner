package com.qrcode.scanner.ui.screens.create

import com.qrcode.scanner.data.history.ScanPayloadMapper
import com.qrcode.scanner.ui.screens.create.QrCategoryType.APP_LINK
import com.qrcode.scanner.ui.screens.create.QrCategoryType.BARCODE
import com.qrcode.scanner.ui.screens.create.QrCategoryType.CALENDAR
import com.qrcode.scanner.ui.screens.create.QrCategoryType.CONTACT
import com.qrcode.scanner.ui.screens.create.QrCategoryType.EMAIL
import com.qrcode.scanner.ui.screens.create.QrCategoryType.LOCATION
import com.qrcode.scanner.ui.screens.create.QrCategoryType.PHONE
import com.qrcode.scanner.ui.screens.create.QrCategoryType.PLAIN_TEXT
import com.qrcode.scanner.ui.screens.create.QrCategoryType.SMS
import com.qrcode.scanner.ui.screens.create.QrCategoryType.WEBSITE
import com.qrcode.scanner.ui.screens.create.QrCategoryType.WHATSAPP
import com.qrcode.scanner.ui.screens.create.QrCategoryType.WIFI
import java.util.Locale

/**
 * Builds and validates QR payloads for Create-flow categories.
 * Barcode is intentionally unsupported (must not fake a QR as a barcode).
 */
object QrPayloadBuilder {

    data class FormInput(
        val primary: String = "",
        val secondary: String = "",
        val tertiary: String = "",
        val wifiSecurity: WifiSecurity = WifiSecurity.WPA,
        val wifiHidden: Boolean = false
    )

    enum class WifiSecurity(val label: String, val wifiT: String) {
        WPA("WPA/WPA2", "WPA"),
        WPA3("WPA3", "WPA"),
        WEP("WEP", "WEP"),
        OPEN("Open", "nopass")
    }

    data class BuildResult(
        val payload: String,
        val displayTitle: String,
        val detectedType: String
    )

    fun supportsQrGeneration(type: QrCategoryType): Boolean = type != BARCODE

    fun validate(type: QrCategoryType, input: FormInput): String? {
        if (type == BARCODE) {
            return "Barcode / EAN generation is not available yet. Use a QR category instead."
        }
        return when (type) {
            WEBSITE -> {
                val url = input.primary.trim()
                when {
                    url.isEmpty() -> "Enter a website URL"
                    !looksLikeUrl(url) -> "Enter a valid URL (https://…)"
                    else -> null
                }
            }
            PLAIN_TEXT -> if (input.primary.trim().isEmpty()) "Enter text to encode" else null
            CONTACT -> if (input.primary.trim().isEmpty()) "Enter a contact name" else null
            PHONE -> {
                val phone = digitsPhone(input.primary)
                when {
                    input.primary.trim().isEmpty() -> "Enter a phone number"
                    phone.length < 7 -> "Enter a valid phone number"
                    else -> null
                }
            }
            EMAIL -> {
                val email = input.primary.trim()
                when {
                    email.isEmpty() -> "Enter an email address"
                    !email.contains('@') || !email.contains('.') -> "Enter a valid email address"
                    else -> null
                }
            }
            SMS -> {
                val phone = digitsPhone(input.primary)
                when {
                    input.primary.trim().isEmpty() -> "Enter a phone number"
                    phone.length < 7 -> "Enter a valid phone number"
                    else -> null
                }
            }
            WHATSAPP -> {
                val phone = digitsPhone(input.primary)
                when {
                    input.primary.trim().isEmpty() -> "Enter a WhatsApp number (with country code)"
                    phone.length < 8 -> "Enter a valid WhatsApp number with country code"
                    else -> null
                }
            }
            LOCATION -> {
                val coords = parseLatLng(input.primary.trim())
                when {
                    input.primary.trim().isEmpty() -> "Enter coordinates as lat,lng"
                    coords == null -> "Enter valid coordinates (e.g. 37.7749,-122.4194)"
                    else -> null
                }
            }
            CALENDAR -> if (input.primary.trim().isEmpty()) "Enter an event title" else null
            APP_LINK -> {
                val v = input.primary.trim()
                when {
                    v.isEmpty() -> "Enter an app URL or package name"
                    looksLikeUrl(v) || v.contains('.') -> null
                    else -> "Enter a store URL or package like com.example.app"
                }
            }
            WIFI -> {
                when {
                    input.primary.trim().isEmpty() -> "Enter a network name (SSID)"
                    input.wifiSecurity != WifiSecurity.OPEN &&
                        input.secondary.trim().isEmpty() -> "Enter the Wi-Fi password"
                    else -> null
                }
            }
            BARCODE -> "Barcode / EAN generation is not available yet."
        }
    }

    fun build(type: QrCategoryType, input: FormInput): BuildResult {
        validate(type, input)?.let { error ->
            throw IllegalArgumentException(error)
        }
        val note = input.secondary.trim().takeIf { type != WIFI && type != SMS && it.isNotEmpty() }
        return when (type) {
            WEBSITE -> {
                val url = ScanPayloadMapper.normalizeUrl(input.primary.trim())
                BuildResult(
                    payload = url,
                    displayTitle = note ?: hostTitle(url),
                    detectedType = ScanPayloadMapper.TYPE_WEBSITE
                )
            }
            PLAIN_TEXT -> {
                val text = input.primary.trim()
                BuildResult(
                    payload = text,
                    displayTitle = note ?: text.take(40),
                    detectedType = ScanPayloadMapper.TYPE_PLAIN_TEXT
                )
            }
            CONTACT -> {
                val name = input.primary.trim()
                val phone = input.tertiary.trim().takeIf { it.isNotEmpty() }
                val email = input.secondary.trim().takeIf {
                    it.isNotEmpty() && it.contains('@')
                }
                val titleNote = input.secondary.trim().takeIf {
                    it.isNotEmpty() && !it.contains('@')
                }
                val vcard = buildString {
                    append("BEGIN:VCARD\n")
                    append("VERSION:3.0\n")
                    append("FN:").append(escapeVcard(name)).append('\n')
                    if (titleNote != null) append("TITLE:").append(escapeVcard(titleNote)).append('\n')
                    if (phone != null) append("TEL:").append(escapeVcard(phone)).append('\n')
                    if (email != null) append("EMAIL:").append(escapeVcard(email)).append('\n')
                    append("END:VCARD")
                }
                BuildResult(
                    payload = vcard,
                    displayTitle = name,
                    detectedType = ScanPayloadMapper.TYPE_QR_CODE
                )
            }
            PHONE -> {
                val phone = digitsPhone(input.primary)
                BuildResult(
                    payload = "tel:$phone",
                    displayTitle = note ?: phone,
                    detectedType = ScanPayloadMapper.TYPE_QR_CODE
                )
            }
            EMAIL -> {
                val email = input.primary.trim()
                val subject = input.secondary.trim()
                val body = input.tertiary.trim()
                val payload = buildString {
                    append("mailto:").append(email)
                    val q = mutableListOf<String>()
                    if (subject.isNotEmpty()) q += "subject=${encodeQuery(subject)}"
                    if (body.isNotEmpty()) q += "body=${encodeQuery(body)}"
                    if (q.isNotEmpty()) append('?').append(q.joinToString("&"))
                }
                BuildResult(
                    payload = payload,
                    displayTitle = note.takeUnless { it == subject } ?: email,
                    detectedType = ScanPayloadMapper.TYPE_QR_CODE
                )
            }
            SMS -> {
                val phone = digitsPhone(input.primary)
                val message = input.secondary.trim()
                val payload = if (message.isEmpty()) {
                    "SMSTO:$phone"
                } else {
                    "SMSTO:$phone:$message"
                }
                BuildResult(
                    payload = payload,
                    displayTitle = phone,
                    detectedType = ScanPayloadMapper.TYPE_QR_CODE
                )
            }
            WHATSAPP -> {
                val phone = digitsPhone(input.primary)
                val text = input.secondary.trim()
                val payload = if (text.isEmpty()) {
                    "https://wa.me/$phone"
                } else {
                    "https://wa.me/$phone?text=${encodeQuery(text)}"
                }
                BuildResult(
                    payload = payload,
                    displayTitle = note ?: "WhatsApp $phone",
                    detectedType = ScanPayloadMapper.TYPE_WEBSITE
                )
            }
            LOCATION -> {
                val (lat, lng) = parseLatLng(input.primary.trim())!!
                val label = input.secondary.trim()
                val payload = if (label.isEmpty()) {
                    "geo:$lat,$lng"
                } else {
                    "geo:$lat,$lng?q=${encodeQuery(label)}"
                }
                BuildResult(
                    payload = payload,
                    displayTitle = note ?: label.ifEmpty { "$lat, $lng" },
                    detectedType = ScanPayloadMapper.TYPE_QR_CODE
                )
            }
            CALENDAR -> {
                val title = input.primary.trim()
                val whenText = input.secondary.trim()
                val stamp = whenText.ifEmpty { "20260101T090000" }
                val payload = buildString {
                    append("BEGIN:VCALENDAR\n")
                    append("VERSION:2.0\n")
                    append("BEGIN:VEVENT\n")
                    append("SUMMARY:").append(escapeIcal(title)).append('\n')
                    append("DTSTART:").append(stamp.filter { it.isLetterOrDigit() }.uppercase(Locale.US)).append('\n')
                    append("END:VEVENT\n")
                    append("END:VCALENDAR")
                }
                BuildResult(
                    payload = payload,
                    displayTitle = title,
                    detectedType = ScanPayloadMapper.TYPE_QR_CODE
                )
            }
            APP_LINK -> {
                val v = input.primary.trim()
                val payload = when {
                    looksLikeUrl(v) -> ScanPayloadMapper.normalizeUrl(v)
                    v.startsWith("market:", ignoreCase = true) -> v
                    else -> "market://details?id=$v"
                }
                BuildResult(
                    payload = payload,
                    displayTitle = note ?: v,
                    detectedType = ScanPayloadMapper.TYPE_WEBSITE
                )
            }
            WIFI -> {
                val ssid = input.primary.trim()
                val password = input.secondary.trim()
                val t = input.wifiSecurity.wifiT
                val hidden = if (input.wifiHidden) "H:true" else "H:false"
                val payload = buildString {
                    append("WIFI:T:").append(t).append(';')
                    append("S:").append(escapeWifi(ssid)).append(';')
                    if (t != "nopass") {
                        append("P:").append(escapeWifi(password)).append(';')
                    }
                    append(hidden).append(";;")
                }
                BuildResult(
                    payload = payload,
                    displayTitle = ssid,
                    detectedType = ScanPayloadMapper.TYPE_WIFI
                )
            }
            BARCODE -> error("Barcode generation unsupported")
        }
    }

    private fun looksLikeUrl(value: String): Boolean =
        ScanPayloadMapper.looksLikeUrl(value) ||
            value.contains('.') && !value.contains(' ')

    private fun digitsPhone(raw: String): String =
        raw.filter { it.isDigit() || it == '+' }

    private fun parseLatLng(raw: String): Pair<Double, Double>? {
        val parts = raw.split(',').map { it.trim() }
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return lat to lng
    }

    private fun hostTitle(url: String): String = try {
        android.net.Uri.parse(url).host?.removePrefix("www.") ?: url.take(40)
    } catch (_: Exception) {
        url.take(40)
    }

    private fun escapeVcard(value: String): String =
        value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")

    private fun escapeIcal(value: String): String =
        value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")

    private fun escapeWifi(value: String): String =
        value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\"", "\\\"")

    private fun encodeQuery(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}
