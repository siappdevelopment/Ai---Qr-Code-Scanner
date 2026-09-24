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

    private fun extractWifiSsid(raw: String): String? {
        return extractWifiParam(raw, "S")?.takeIf { it.isNotBlank() }
    }

    private fun extractWifiParam(raw: String, key: String): String? {
        // WIFI:T:WPA;S:Network;P:pass;;
        val body = raw.removePrefix("WIFI:").removePrefix("wifi:")
        val parts = body.split(';')
        val prefix = "$key:"
        return parts.firstOrNull { it.startsWith(prefix, ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}
