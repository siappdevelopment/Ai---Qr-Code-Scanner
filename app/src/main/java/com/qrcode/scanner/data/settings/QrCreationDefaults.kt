package com.qrcode.scanner.data.settings

import com.qrcode.scanner.ui.screens.create.QrBitmapEncoder

/**
 * Persisted QR creation defaults (Phase 12.17).
 * Storage values are stable enum names — not UI copy.
 */
enum class QrDefaultEcc(val storageValue: String) {
    L("L"),
    M("M"),
    Q("Q"),
    H("H");

    fun toEncoderLevel(): QrBitmapEncoder.EccLevel =
        QrBitmapEncoder.EccLevel.entries.first { it.name == storageValue }

    fun settingsSubtitle(): String = when (this) {
        L -> "Low (7% ECC)"
        M -> "Medium (15% ECC)"
        Q -> "Quartile (25% ECC)"
        H -> "High (30% ECC) · Optimal redundancy"
    }

    fun selectionTitle(): String = when (this) {
        L -> "Low / L / 7%"
        M -> "Medium / M / 15%"
        Q -> "Quartile / Q / 25%"
        H -> "High / H / 30%"
    }

    companion object {
        fun fromStored(value: String?): QrDefaultEcc =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) }
                ?: H

        fun fromEncoder(level: QrBitmapEncoder.EccLevel): QrDefaultEcc =
            fromStored(level.name)
    }
}

enum class QrDefaultOutputFormat(val storageValue: String) {
    SVG("SVG"),
    PNG("PNG");

    fun settingsSubtitle(): String = when (this) {
        SVG -> "Vector SVG / Sharp 1024px"
        PNG -> "PNG / 1024px"
    }

    fun selectionTitle(): String = settingsSubtitle()

    companion object {
        fun fromStored(value: String?): QrDefaultOutputFormat =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) }
                ?: SVG
    }
}
