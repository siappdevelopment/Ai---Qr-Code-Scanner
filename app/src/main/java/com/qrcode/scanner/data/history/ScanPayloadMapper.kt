package com.qrcode.scanner.data.history

import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * Shared helpers for classifying and labeling scan payloads for History / Result UI.
 */
object ScanPayloadMapper {

    const val TYPE_WEBSITE = "Website"
    const val TYPE_WIFI = "Wi-Fi"
    const val TYPE_PLAIN_TEXT = "Plain Text"
    const val TYPE_QR_CODE = "QR Code"
    const val TYPE_BARCODE = "Barcode"

    fun detectType(rawValue: String, format: Int, formatName: String): String {
        val trimmed = rawValue.trim()
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) return TYPE_WIFI
        if (looksLikeUrl(trimmed)) return TYPE_WEBSITE

        // Structured QR payloads already produced by Create — keep existing type constants.
        val upper = trimmed.uppercase()
        when {
            upper.startsWith("BEGIN:VCARD") -> return TYPE_QR_CODE
            upper.startsWith("BEGIN:VCALENDAR") || upper.startsWith("BEGIN:VEVENT") ->
                return TYPE_QR_CODE
            trimmed.startsWith("mailto:", ignoreCase = true) -> return TYPE_QR_CODE
            trimmed.startsWith("tel:", ignoreCase = true) -> return TYPE_QR_CODE
            trimmed.startsWith("SMSTO:", ignoreCase = true) -> return TYPE_QR_CODE
            trimmed.startsWith("sms:", ignoreCase = true) -> return TYPE_QR_CODE
            trimmed.startsWith("geo:", ignoreCase = true) -> return TYPE_QR_CODE
        }

        return when (format) {
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX -> TYPE_QR_CODE
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_PDF417 -> TYPE_BARCODE
            else -> when {
                formatName.contains("QR", ignoreCase = true) -> TYPE_QR_CODE
                formatName.contains("EAN", ignoreCase = true) ||
                    formatName.contains("UPC", ignoreCase = true) ||
                    formatName.contains("CODE_", ignoreCase = true) -> TYPE_BARCODE
                else -> TYPE_PLAIN_TEXT
            }
        }
    }

    fun titleFor(entity: HistoryEntity): String = titleFor(entity.rawValue, entity.detectedType)

    fun titleFor(rawValue: String, detectedType: String): String {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return "(empty)"
        when (detectedType) {
            TYPE_WIFI -> {
                extractWifiSsid(trimmed)?.let { return it }
            }
            TYPE_WEBSITE -> {
                return try {
                    val uri = Uri.parse(normalizeUrl(trimmed))
                    uri.host?.removePrefix("www.")?.ifBlank { null }
                        ?: trimmed.take(48)
                } catch (_: Exception) {
                    trimmed.take(48)
                }
            }
        }
        return if (trimmed.length > 48) trimmed.take(45) + "…" else trimmed
    }

    fun subtitleFor(entity: HistoryEntity): String = subtitleFor(
        rawValue = entity.rawValue,
        detectedType = entity.detectedType,
        formatName = entity.barcodeFormatName
    )

    fun subtitleFor(rawValue: String, detectedType: String, formatName: String): String {
        val trimmed = rawValue.trim()
        return when (detectedType) {
            TYPE_WIFI -> {
                val security = extractWifiParam(trimmed, "T") ?: "Open"
                val ssid = extractWifiSsid(trimmed)
                if (ssid != null) "$security · SSID: $ssid" else security
            }
            TYPE_WEBSITE -> trimmed
            TYPE_BARCODE -> "$formatName · Barcode"
            else -> trimmed
        }
    }

    fun badgeLabel(entity: HistoryEntity): String {
        return when {
            entity.source == HistoryEntity.SOURCE_CREATED -> "Created"
            entity.detectedType == TYPE_WIFI -> "Wi-Fi"
            entity.detectedType == TYPE_WEBSITE -> "Website"
            entity.detectedType == TYPE_BARCODE -> "Barcode"
            else -> "Scanned"
        }
    }

    fun looksLikeUrl(value: String): Boolean {
        val v = value.trim()
        if (v.startsWith("http://", ignoreCase = true) ||
            v.startsWith("https://", ignoreCase = true)
        ) return true
        if (v.startsWith("www.", ignoreCase = true) && v.contains('.')) return true
        return false
    }

    fun normalizeUrl(value: String): String {
        val v = value.trim()
        return if (v.startsWith("http://", ignoreCase = true) ||
            v.startsWith("https://", ignoreCase = true)
        ) v else "https://$v"
    }

    /**
     * Phase 12.5: eligible for Auto-Open URLs when classified as Website and the
     * normalized destination is http(s) only (www. → https:// via [normalizeUrl]).
     */
    fun isEligibleForAutoOpen(rawValue: String, detectedType: String): Boolean {
        if (detectedType != TYPE_WEBSITE) return false
        if (!looksLikeUrl(rawValue)) return false
        val normalized = normalizeUrl(rawValue)
        return normalized.startsWith("http://", ignoreCase = true) ||
            normalized.startsWith("https://", ignoreCase = true)
    }

    fun extractWifiSsid(raw: String): String? {
        return extractWifiParam(raw, "S")?.takeIf { it.isNotBlank() }
    }

    fun extractWifiPassword(raw: String): String? {
        return extractWifiParam(raw, "P")
    }

    fun extractWifiParam(raw: String, key: String): String? {
        val fields = parseWifiFields(raw)
        return fields[key.uppercase()]
    }

    /**
     * Parses `WIFI:T:…;S:…;P:…;H:…;;` splitting only on unescaped `;`,
     * then unescaping `\`, `;`, `,`, `"`.
     */
    fun parseWifiFields(raw: String): Map<String, String> {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("WIFI:", ignoreCase = true)) return emptyMap()
        val body = trimmed.substring(5) // after "WIFI:"
        val segments = splitUnescaped(body, ';')
        val out = linkedMapOf<String, String>()
        for (segment in segments) {
            if (segment.isEmpty()) continue
            val colon = segment.indexOf(':')
            if (colon <= 0) continue
            val k = segment.substring(0, colon).uppercase()
            val v = unescapeWifi(segment.substring(colon + 1))
            out[k] = v
        }
        return out
    }

    /** Split [input] on [delimiter] that are not preceded by an odd number of backslashes. */
    fun splitUnescaped(input: String, delimiter: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\\' && i + 1 < input.length) {
                current.append(c)
                current.append(input[i + 1])
                i += 2
                continue
            }
            if (c == delimiter) {
                parts.add(current.toString())
                current.clear()
                i++
                continue
            }
            current.append(c)
            i++
        }
        parts.add(current.toString())
        return parts
    }

    fun unescapeWifi(value: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                out.append(value[i + 1])
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }
}
