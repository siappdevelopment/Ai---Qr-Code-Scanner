package com.qrcode.scanner.ui.screens.create

import com.google.zxing.BarcodeFormat
import com.google.zxing.FormatException
import com.google.zxing.MultiFormatWriter
import com.google.zxing.oned.UPCEReader

/**
 * Input rules for Create barcode formats, aligned with ZXing core 3.5.3 writers.
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

    fun isNumericInput(type: QrCategoryType): Boolean = when (type) {
        QrCategoryType.EAN_13,
        QrCategoryType.EAN_8,
        QrCategoryType.UPC_E,
        QrCategoryType.UPC_A,
        QrCategoryType.ITF -> true
        else -> false
    }

    /** Typed length label. Fixed formats include the accepted full length. No invented maximums. */
    fun counterText(type: QrCategoryType, length: Int): String = when (type) {
        QrCategoryType.EAN_13 -> if (length <= 12) "$length/12" else "$length/13"
        QrCategoryType.EAN_8 -> if (length <= 7) "$length/7" else "$length/8"
        QrCategoryType.UPC_E -> if (length <= 7) "$length/7" else "$length/8"
        QrCategoryType.UPC_A -> if (length <= 11) "$length/11" else "$length/12"
        QrCategoryType.ITF -> "$length digits"
        else -> "$length chars"
    }

    fun filterInput(type: QrCategoryType, raw: String): String = when (type) {
        QrCategoryType.EAN_13,
        QrCategoryType.EAN_8,
        QrCategoryType.UPC_E,
        QrCategoryType.UPC_A,
        QrCategoryType.ITF -> raw.filter { it.isDigit() }
        QrCategoryType.CODE_39 -> raw.filter { it != '*' }
        QrCategoryType.CODABAR -> raw.filter { it != ' ' }
        else -> raw
    }

    fun validate(type: QrCategoryType, raw: String): String? {
        if (!isBarcode(type)) return null
        if (raw.isEmpty()) return emptyMessage(type)
        val value = raw
        return when (type) {
            QrCategoryType.EAN_13 -> digitsMessage(
                value,
                12,
                13,
                "Enter 12 digits, or 13 digits including the check digit."
            )
            QrCategoryType.EAN_8 -> digitsMessage(
                value,
                7,
                8,
                "Enter 7 digits, or 8 digits including the check digit."
            )
            QrCategoryType.UPC_A -> digitsMessage(
                value,
                11,
                12,
                "Enter 11 digits, or 12 digits including the check digit."
            )
            QrCategoryType.UPC_E -> validateUpcE(value)
            QrCategoryType.ITF -> validateItf(value)
            QrCategoryType.CODE_128 -> validateAscii(
                value,
                "Code 128 only supports characters up to ASCII 127."
            )
            QrCategoryType.CODE_93 -> validateCode93(value)
            QrCategoryType.CODE_39 -> validateCode39(value)
            QrCategoryType.CODABAR -> validateCodabar(value)
            QrCategoryType.DATA_MATRIX,
            QrCategoryType.PDF_417,
            QrCategoryType.AZTEC -> ensureFits(type, value)
            else -> null
        }
    }

    /** Value that will actually be encoded. Never replaces a rejected check digit. */
    fun normalize(type: QrCategoryType, raw: String): String {
        validate(type, raw)?.let { throw IllegalArgumentException(it) }
        val value = raw
        return when (type) {
            QrCategoryType.EAN_13 -> withCheckDigit(value, 12)
            QrCategoryType.EAN_8 -> withCheckDigit(value, 7)
            QrCategoryType.UPC_A -> withCheckDigit(value, 11)
            QrCategoryType.UPC_E -> if (value.length == 7) value + upcECheckDigit(value) else value
            QrCategoryType.CODABAR -> normalizeCodabar(value)
            else -> value
        }
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

    private fun emptyMessage(type: QrCategoryType): String = when (type) {
        QrCategoryType.EAN_13 -> "Enter 12 digits, or 13 digits including the check digit."
        QrCategoryType.EAN_8 -> "Enter 7 digits, or 8 digits including the check digit."
        QrCategoryType.UPC_A -> "Enter 11 digits, or 12 digits including the check digit."
        QrCategoryType.UPC_E -> "Enter 7 digits, or 8 digits including the check digit."
        QrCategoryType.ITF -> "Enter an even number of digits."
        QrCategoryType.CODE_128 -> "Enter a Code 128 value."
        QrCategoryType.CODE_93 -> "Enter a Code 93 value."
        QrCategoryType.CODE_39 -> "Enter a Code 39 value."
        QrCategoryType.CODABAR -> "Enter a Codabar value."
        QrCategoryType.DATA_MATRIX -> "Enter a Data Matrix value."
        QrCategoryType.PDF_417 -> "Enter a PDF417 value."
        QrCategoryType.AZTEC -> "Enter an Aztec value."
        else -> "Enter a value to encode"
    }

    private fun digitsMessage(value: String, short: Int, full: Int, lengthMessage: String): String? {
        if (!value.all { it.isDigit() }) return "Digits only."
        if (value.length != short && value.length != full) return lengthMessage
        if (value.length == full && !hasValidCheckDigit(value)) return "Check digit does not match."
        return null
    }

    private fun validateUpcE(value: String): String? {
        if (!value.all { it.isDigit() }) return "Digits only."
        if (value.length != 7 && value.length != 8) {
            return "Enter 7 digits, or 8 digits including the check digit."
        }
        if (value[0] != '0' && value[0] != '1') return "Invalid UPC-E value."
        val expanded = expandUpcE(value) ?: return "Invalid UPC-E value."
        if (value.length == 8 && !hasValidCheckDigit(expanded)) {
            return "Check digit does not match."
        }
        return null
    }

    private fun validateItf(value: String): String? {
        if (!value.all { it.isDigit() }) return "Digits only."
        if (value.length < 2) return "ITF needs at least 2 digits."
        if (value.length % 2 != 0) return "Enter an even number of digits."
        if (value.length > 80) return "ITF cannot be longer than 80 digits."
        return null
    }

    private fun validateAscii(value: String, highMessage: String): String? =
        if (value.any { it.code > 127 }) highMessage else null

    private fun validateCode93(value: String): String? {
        if (value.any { it.code > 127 }) {
            return "Code 93 only supports characters up to ASCII 127."
        }
        val expanded = code93ExtendedLength(value)
        return if (expanded > 80) "This value is too long for Code 93." else null
    }

    private fun validateCode39(value: String): String? {
        if (value.any { it == '*' }) return "Code 39 cannot encode this character."
        if (value.any { it.code > 127 }) return "Code 39 cannot encode this character."
        val expanded = if (value.all { it in CODE_39_NATIVE }) {
            value.length
        } else {
            code39Extended(value)?.length ?: return "Code 39 cannot encode this character."
        }
        return if (expanded > 80) "This value is too long for Code 39." else null
    }

    private fun validateCodabar(value: String): String? {
        if (value.any { it == ' ' }) {
            return "Codabar only allows digits and - $ : / . +."
        }
        val upper = value.uppercase()
        val guarded = codabarGuards(upper) ?: return "Start and stop characters must both be present."
        val data = if (guarded) upper.substring(1, upper.length - 1) else upper
        if (data.isEmpty()) return "Enter a Codabar value."
        if (data.any { it !in CODABAR_DATA }) {
            return "Codabar only allows digits and - $ : / . +."
        }
        return null
    }

    private fun normalizeCodabar(value: String): String {
        val upper = value.uppercase()
        val guarded = codabarGuards(upper) == true
        return if (guarded) upper else "A${upper}A"
    }

    /** @return true when both ends are guards, false when neither is, null when only one end is. */
    private fun codabarGuards(upper: String): Boolean? {
        if (upper.isEmpty()) return false
        val first = upper.first()
        val last = upper.last()
        val startNormal = first in CODABAR_NORMAL_GUARDS
        val endNormal = last in CODABAR_NORMAL_GUARDS
        val startAlt = first in CODABAR_ALT_GUARDS
        val endAlt = last in CODABAR_ALT_GUARDS
        return when {
            startNormal && endNormal -> true
            startAlt && endAlt -> true
            !startNormal && !startAlt && !endNormal && !endAlt -> false
            else -> null
        }
    }

    private fun withCheckDigit(value: String, dataLength: Int): String =
        if (value.length == dataLength) value + checkDigit(value) else value

    private fun hasValidCheckDigit(value: String): Boolean {
        if (value.length < 2 || value.any { !it.isDigit() }) return false
        return checkDigit(value.dropLast(1)) == value.last()
    }

    private fun upcECheckDigit(sevenDigits: String): Char {
        val expanded = expandUpcE(sevenDigits)
            ?: throw IllegalArgumentException("Invalid UPC-E value.")
        return checkDigit(expanded)
    }

    /** ZXing expansion. 7 digits expand to 11; 8 digits expand to 12 including the check digit. */
    private fun expandUpcE(value: String): String? = try {
        UPCEReader.convertUPCEtoUPCA(value)
    } catch (_: FormatException) {
        null
    }

    private fun ensureFits(type: QrCategoryType, value: String): String? {
        val format = formatOf(type) ?: return null
        return try {
            MultiFormatWriter().encode(value, format, 1, 1)
            null
        } catch (_: Throwable) {
            when (type) {
                QrCategoryType.DATA_MATRIX -> "This value is too large for a Data Matrix symbol."
                QrCategoryType.PDF_417 -> "This value is too large for a PDF417 symbol."
                QrCategoryType.AZTEC -> "This value is too large for an Aztec symbol."
                else -> "This value cannot be encoded."
            }
        }
    }

    /** Matches ZXing 3.5.3 Code93Writer.convertToExtended length, not a second encoder. */
    private fun code93ExtendedLength(contents: String): Int {
        var length = 0
        for (character in contents) {
            length += when {
                character == 0.toChar() -> 2
                character <= 26.toChar() -> 2
                character <= 31.toChar() -> 2
                character == ' ' || character == '$' || character == '%' || character == '+' -> 1
                character <= ',' -> 2
                character <= '9' -> 1
                character == ':' -> 2
                character <= '?' -> 2
                character == '@' -> 2
                character <= 'Z' -> 1
                character <= '_' -> 2
                character == '`' -> 2
                character <= 'z' -> 2
                character <= 127.toChar() -> 2
                else -> 2
            }
        }
        return length
    }

    /** Matches ZXing 3.5.3 Code39Writer.tryToConvertToExtendedMode. */
    private fun code39Extended(contents: String): String? {
        val extended = StringBuilder()
        for (character in contents) {
            when (character) {
                '\u0000' -> extended.append("%U")
                ' ', '-', '.' -> extended.append(character)
                '@' -> extended.append("%V")
                '`' -> extended.append("%W")
                else -> {
                    if (character <= 26.toChar()) {
                        extended.append('$')
                        extended.append('A' + (character.code - 1))
                    } else if (character < ' ') {
                        extended.append('%')
                        extended.append('A' + (character.code - 27))
                    } else if (character <= ',' || character == '/' || character == ':') {
                        extended.append('/')
                        extended.append('A' + (character.code - 33))
                    } else if (character <= '9') {
                        extended.append(character)
                    } else if (character <= '?') {
                        extended.append('%')
                        extended.append('F' + (character.code - 59))
                    } else if (character <= 'Z') {
                        extended.append(character)
                    } else if (character <= '_') {
                        extended.append('%')
                        extended.append('K' + (character.code - 91))
                    } else if (character <= 'z') {
                        extended.append('+')
                        extended.append('A' + (character.code - 97))
                    } else if (character <= 127.toChar()) {
                        extended.append('%')
                        extended.append('P' + (character.code - 123))
                    } else {
                        return null
                    }
                }
            }
        }
        return extended.toString()
    }

    private const val CODE_39_NATIVE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%"
    private const val CODABAR_DATA = "0123456789-$:/.+"
    private const val CODABAR_NORMAL_GUARDS = "ABCD"
    private const val CODABAR_ALT_GUARDS = "TN*E"
}
