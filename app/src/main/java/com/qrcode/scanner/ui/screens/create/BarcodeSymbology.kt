package com.qrcode.scanner.ui.screens.create

import com.google.zxing.BarcodeFormat

/**
 * Rules and canonical payloads for the Create barcode formats.
 * Does not draw bitmaps, so unit tests can call it on the JVM.
 */
object BarcodeSymbology {

    fun formatOf(type: QrCategoryType): BarcodeFormat? = when (type) {
        QrCategoryType.CODE_128 -> BarcodeFormat.CODE_128
        QrCategoryType.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
        QrCategoryType.PDF_417 -> BarcodeFormat.PDF_417
        QrCategoryType.AZTEC -> BarcodeFormat.AZTEC
        QrCategoryType.EAN_13 -> BarcodeFormat.EAN_13
        QrCategoryType.EAN_8 -> BarcodeFormat.EAN_8
        QrCategoryType.UPC_E -> BarcodeFormat.UPC_E
        QrCategoryType.UPC_A -> BarcodeFormat.UPC_A
        QrCategoryType.CODE_93 -> BarcodeFormat.CODE_93
        QrCategoryType.CODE_39 -> BarcodeFormat.CODE_39
        QrCategoryType.CODABAR -> BarcodeFormat.CODABAR
        QrCategoryType.ITF -> BarcodeFormat.ITF
        else -> null
    }

    fun isBarcode(type: QrCategoryType): Boolean = formatOf(type) != null

    fun validate(type: QrCategoryType, raw: String): String? {
        if (!isBarcode(type)) return null
        val value = raw.trim()
        if (value.isEmpty()) return "Enter a value to encode"
        return when (type) {
            QrCategoryType.EAN_13 -> digitsMessage(value, 12, 13, "EAN 13")
            QrCategoryType.EAN_8 -> digitsMessage(value, 7, 8, "EAN 8")
            QrCategoryType.UPC_A -> digitsMessage(value, 11, 12, "UPC A")
            QrCategoryType.UPC_E -> digitsMessage(value, 7, 8, "UPC E")
            QrCategoryType.ITF -> {
                if (!value.all { it.isDigit() }) "ITF uses digits only"
                else if (value.length % 2 != 0) "ITF needs an even number of digits"
                else null
            }
            QrCategoryType.CODE_39 -> charsetMessage(value.uppercase(), CODE_39_CHARS, "Code 39")
            QrCategoryType.CODE_93 -> charsetMessage(value.uppercase(), CODE_39_CHARS, "Code 93")
            QrCategoryType.CODABAR -> {
                val body = value.uppercase()
                if (body.any { it !in CODABAR_CHARS }) {
                    "Codabar uses digits and - $ : / . +"
                } else {
                    null
                }
            }
            else -> null
        }
    }

    /** Value that will actually be encoded, including a checksum when the format requires one. */
    fun normalize(type: QrCategoryType, raw: String): String {
        validate(type, raw)?.let { throw IllegalArgumentException(it) }
        val value = raw.trim()
        return when (type) {
            QrCategoryType.EAN_13 -> withCheckDigit(value, 12)
            QrCategoryType.EAN_8 -> withCheckDigit(value, 7)
            QrCategoryType.UPC_A -> withCheckDigit(value, 11)
            QrCategoryType.UPC_E -> value.filter { it.isDigit() }
            QrCategoryType.CODE_39, QrCategoryType.CODE_93 -> value.uppercase()
            QrCategoryType.CODABAR -> {
                val body = value.uppercase()
                val started = body.first() in "ABCD"
                val ended = body.last() in "ABCD"
                buildString {
                    if (!started) append('A')
                    append(body)
                    if (!ended) append('A')
                }
            }
            else -> value
        }
    }

    private fun digitsMessage(value: String, short: Int, full: Int, name: String): String? {
        if (!value.all { it.isDigit() }) return "$name uses digits only"
        if (value.length != short && value.length != full) {
            return "Enter $short digits, or $full including the check digit"
        }
        if (value.length == full && !hasValidCheckDigit(value)) {
            return "Check digit does not match"
        }
        return null
    }

    private fun charsetMessage(value: String, allowed: String, name: String): String? =
        if (value.all { it in allowed }) null else "$name uses letters, digits, and - . \$ / + % space"

    private fun withCheckDigit(value: String, dataLength: Int): String {
        val digits = value.filter { it.isDigit() }
        return if (digits.length == dataLength) digits + checkDigit(digits) else digits
    }

    private fun hasValidCheckDigit(value: String): Boolean {
        if (value.length < 2) return false
        val data = value.dropLast(1)
        return checkDigit(data) == value.last()
    }

    /** GS1 check digit. [data] is the digits without the check digit. */
    fun checkDigit(data: String): Char {
        var sum = 0
        data.reversed().forEachIndexed { index, ch ->
            val n = ch.digitToInt()
            sum += if (index % 2 == 0) n * 3 else n
        }
        return ('0' + ((10 - (sum % 10)) % 10))
    }

    private const val CODE_39_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%"
    private const val CODABAR_CHARS = "0123456789-$:/.+ABCD"
}
