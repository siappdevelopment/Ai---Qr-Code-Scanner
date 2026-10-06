package com.qrcode.scanner.ui.screens.create

import android.content.Context
import androidx.annotation.StringRes
import com.google.zxing.BarcodeFormat
import com.google.zxing.FormatException
import com.google.zxing.MultiFormatWriter
import com.google.zxing.oned.UPCEReader
import com.qrcode.scanner.app.R

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
    fun counterText(type: QrCategoryType, length: Int, context: Context? = null): String = when (type) {
        QrCategoryType.EAN_13 -> if (length <= 12) {
            msg(context, R.string.barcode_counter_ean_13_short, "$length/12", length)
        } else {
            msg(context, R.string.barcode_counter_ean_13_long, "$length/13", length)
        }
        QrCategoryType.EAN_8 -> if (length <= 7) {
            msg(context, R.string.barcode_counter_ean_8_short, "$length/7", length)
        } else {
            msg(context, R.string.barcode_counter_ean_8_long, "$length/8", length)
        }
        QrCategoryType.UPC_E -> if (length <= 7) {
            msg(context, R.string.barcode_counter_upc_e_short, "$length/7", length)
        } else {
            msg(context, R.string.barcode_counter_upc_e_long, "$length/8", length)
        }
        QrCategoryType.UPC_A -> if (length <= 11) {
            msg(context, R.string.barcode_counter_upc_a_short, "$length/11", length)
        } else {
            msg(context, R.string.barcode_counter_upc_a_long, "$length/12", length)
        }
        QrCategoryType.ITF -> msg(
            context,
            R.string.barcode_counter_itf_digits,
            "$length digits",
            length
        )
        else -> msg(context, R.string.barcode_counter_chars, "$length chars", length)
    }

    /**
     * Input cap. Fixed retail formats use their real digit length.
     * Code 128 and Codabar have no symbol maximum, so the cap only stops a huge paste.
     * Data Matrix, PDF417, and Aztec still report capacity from the encoder.
     */
    fun maxInputLength(type: QrCategoryType): Int = when (type) {
        QrCategoryType.EAN_13 -> 13
        QrCategoryType.EAN_8 -> 8
        QrCategoryType.UPC_E -> 8
        QrCategoryType.UPC_A -> 12
        QrCategoryType.ITF, QrCategoryType.CODE_39, QrCategoryType.CODE_93 -> 80
        QrCategoryType.CODE_128, QrCategoryType.CODABAR -> 2000
        QrCategoryType.DATA_MATRIX, QrCategoryType.PDF_417, QrCategoryType.AZTEC -> 8000
        else -> 2000
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

    fun validate(type: QrCategoryType, raw: String, context: Context? = null): String? {
        if (!isBarcode(type)) return null
        if (raw.isEmpty()) return emptyMessage(type, context)
        val value = raw
        return when (type) {
            QrCategoryType.EAN_13 -> digitLengthMessage(
                value,
                12,
                13,
                msg(
                    context,
                    R.string.barcode_validation_ean_13_length,
                    "Enter 12 digits, or 13 digits including the check digit."
                ),
                context
            )
            QrCategoryType.EAN_8 -> digitLengthMessage(
                value,
                7,
                8,
                msg(
                    context,
                    R.string.barcode_validation_ean_8_length,
                    "Enter 7 digits, or 8 digits including the check digit."
                ),
                context
            )
            QrCategoryType.UPC_A -> digitsMessage(
                value,
                11,
                12,
                msg(
                    context,
                    R.string.barcode_validation_upc_a_length,
                    "Enter 11 digits, or 12 digits including the check digit."
                ),
                context
            )
            QrCategoryType.UPC_E -> validateUpcE(value, context)
            QrCategoryType.ITF -> validateItf(value, context)
            QrCategoryType.CODE_128 -> validateAscii(
                value,
                msg(
                    context,
                    R.string.barcode_validation_code_128_ascii,
                    "Code 128 only supports characters up to ASCII 127."
                )
            )
            QrCategoryType.CODE_93 -> validateCode93(value, context)
            QrCategoryType.CODE_39 -> validateCode39(value, context)
            QrCategoryType.CODABAR -> validateCodabar(value, context)
            QrCategoryType.DATA_MATRIX,
            QrCategoryType.PDF_417,
            QrCategoryType.AZTEC -> ensureFits(type, value, context)
            else -> null
        }
    }

    /** Value that will actually be encoded. Retail formats correct the check digit when needed. */
    fun normalize(type: QrCategoryType, raw: String): String {
        validate(type, raw)?.let { throw IllegalArgumentException(it) }
        val value = raw
        return when (type) {
            // Always encode from the data digits so a mistyped last digit still works.
            QrCategoryType.EAN_13 -> ensureCheckDigit(value, 12)
            QrCategoryType.EAN_8 -> ensureCheckDigit(value, 7)
            QrCategoryType.UPC_A -> withCheckDigit(value, 11)
            QrCategoryType.UPC_E -> ensureUpcE(value)
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

    private fun emptyMessage(type: QrCategoryType, context: Context?): String = when (type) {
        QrCategoryType.EAN_13 -> msg(
            context,
            R.string.barcode_validation_ean_13_length,
            "Enter 12 digits, or 13 digits including the check digit."
        )
        QrCategoryType.EAN_8 -> msg(
            context,
            R.string.barcode_validation_ean_8_length,
            "Enter 7 digits, or 8 digits including the check digit."
        )
        QrCategoryType.UPC_A -> msg(
            context,
            R.string.barcode_validation_upc_a_length,
            "Enter 11 digits, or 12 digits including the check digit."
        )
        QrCategoryType.UPC_E -> msg(
            context,
            R.string.barcode_validation_upc_e_length,
            "Enter 7 digits, or 8 digits including the check digit."
        )
        QrCategoryType.ITF -> msg(
            context,
            R.string.barcode_validation_itf_even,
            "Enter an even number of digits."
        )
        QrCategoryType.CODE_128 -> msg(
            context,
            R.string.barcode_validation_enter_code_128,
            "Enter a Code 128 value."
        )
        QrCategoryType.CODE_93 -> msg(
            context,
            R.string.barcode_validation_enter_code_93,
            "Enter a Code 93 value."
        )
        QrCategoryType.CODE_39 -> msg(
            context,
            R.string.barcode_validation_enter_code_39,
            "Enter a Code 39 value."
        )
        QrCategoryType.CODABAR -> msg(
            context,
            R.string.barcode_validation_enter_codabar,
            "Enter a Codabar value."
        )
        QrCategoryType.DATA_MATRIX -> msg(
            context,
            R.string.barcode_validation_enter_data_matrix,
            "Enter a Data Matrix value."
        )
        QrCategoryType.PDF_417 -> msg(
            context,
            R.string.barcode_validation_enter_pdf417,
            "Enter a PDF417 value."
        )
        QrCategoryType.AZTEC -> msg(
            context,
            R.string.barcode_validation_enter_aztec,
            "Enter an Aztec value."
        )
        else -> msg(
            context,
            R.string.create_validation_enter_value,
            "Enter a value to encode"
        )
    }

    private fun digitsMessage(
        value: String,
        short: Int,
        full: Int,
        lengthMessage: String,
        context: Context?
    ): String? {
        if (!value.all { it.isDigit() }) {
            return msg(context, R.string.barcode_validation_digits_only, "Digits only.")
        }
        if (value.length != short && value.length != full) return lengthMessage
        if (value.length == full && !hasValidCheckDigit(value)) {
            return msg(
                context,
                R.string.barcode_validation_check_digit_mismatch,
                "Check digit does not match."
            )
        }
        return null
    }

    /** Length and digits only. Check digit is corrected in [normalize]. */
    private fun digitLengthMessage(
        value: String,
        short: Int,
        full: Int,
        lengthMessage: String,
        context: Context?
    ): String? {
        if (!value.all { it.isDigit() }) {
            return msg(context, R.string.barcode_validation_digits_only, "Digits only.")
        }
        if (value.length != short && value.length != full) return lengthMessage
        return null
    }

    private fun validateUpcE(value: String, context: Context?): String? {
        if (!value.all { it.isDigit() }) {
            return msg(context, R.string.barcode_validation_digits_only, "Digits only.")
        }
        if (value.length != 7 && value.length != 8) {
            return msg(
                context,
                R.string.barcode_validation_upc_e_length,
                "Enter 7 digits, or 8 digits including the check digit."
            )
        }
        if (value[0] != '0' && value[0] != '1') {
            return msg(
                context,
                R.string.barcode_validation_upc_e_invalid,
                "Invalid UPC-E value."
            )
        }
        // Expansion uses the first 7 digits; check digit is corrected in [normalize].
        if (expandUpcE(value.take(7)) == null) {
            return msg(
                context,
                R.string.barcode_validation_upc_e_invalid,
                "Invalid UPC-E value."
            )
        }
        return null
    }

    private fun validateItf(value: String, context: Context?): String? {
        if (!value.all { it.isDigit() }) {
            return msg(context, R.string.barcode_validation_digits_only, "Digits only.")
        }
        if (value.length < 2) {
            return msg(context, R.string.barcode_validation_itf_min, "ITF needs at least 2 digits.")
        }
        if (value.length % 2 != 0) {
            return msg(
                context,
                R.string.barcode_validation_itf_even,
                "Enter an even number of digits."
            )
        }
        if (value.length > 80) {
            return msg(
                context,
                R.string.barcode_validation_itf_max,
                "ITF cannot be longer than 80 digits."
            )
        }
        return null
    }

    private fun validateAscii(value: String, highMessage: String): String? =
        if (value.any { it.code > 127 }) highMessage else null

    private fun validateCode93(value: String, context: Context?): String? {
        if (value.any { it.code > 127 }) {
            return msg(
                context,
                R.string.barcode_validation_code_93_ascii,
                "Code 93 only supports characters up to ASCII 127."
            )
        }
        val expanded = code93ExtendedLength(value)
        return if (expanded > 80) {
            msg(
                context,
                R.string.barcode_validation_code_93_too_long,
                "This value is too long for Code 93."
            )
        } else {
            null
        }
    }

    private fun validateCode39(value: String, context: Context?): String? {
        val badChar = msg(
            context,
            R.string.barcode_validation_code_39_unsupported,
            "Code 39 cannot encode this character."
        )
        if (value.any { it == '*' }) return badChar
        if (value.any { it.code > 127 }) return badChar
        val expanded = if (value.all { it in CODE_39_NATIVE }) {
            value.length
        } else {
            code39Extended(value)?.length ?: return badChar
        }
        return if (expanded > 80) {
            msg(
                context,
                R.string.barcode_validation_code_39_too_long,
                "This value is too long for Code 39."
            )
        } else {
            null
        }
    }

    private fun validateCodabar(value: String, context: Context?): String? {
        val charsMsg = msg(
            context,
            R.string.barcode_validation_codabar_chars,
            "Codabar only allows digits and - \$ : / . +."
        )
        if (value.any { it == ' ' }) return charsMsg
        val upper = value.uppercase()
        val guarded = codabarGuards(upper)
            ?: return msg(
                context,
                R.string.barcode_validation_codabar_guards,
                "Start and stop characters must both be present."
            )
        val data = if (guarded) upper.substring(1, upper.length - 1) else upper
        if (data.isEmpty()) {
            return msg(
                context,
                R.string.barcode_validation_enter_codabar,
                "Enter a Codabar value."
            )
        }
        if (data.any { it !in CODABAR_DATA }) return charsMsg
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

    private fun ensureCheckDigit(value: String, dataLength: Int): String {
        val data = value.take(dataLength)
        return data + checkDigit(data)
    }

    private fun hasValidCheckDigit(value: String): Boolean {
        if (value.length < 2 || value.any { !it.isDigit() }) return false
        return checkDigit(value.dropLast(1)) == value.last()
    }

    private fun ensureUpcE(value: String): String {
        val seven = value.take(7)
        val expanded = expandUpcE(seven)
            ?: throw IllegalArgumentException("Invalid UPC-E value.")
        return seven + checkDigit(expanded)
    }

    /** ZXing expansion. 7 digits expand to 11; 8 digits expand to 12 including the check digit. */
    private fun expandUpcE(value: String): String? = try {
        UPCEReader.convertUPCEtoUPCA(value)
    } catch (_: FormatException) {
        null
    }

    private fun ensureFits(type: QrCategoryType, value: String, context: Context?): String? {
        val format = formatOf(type) ?: return null
        return try {
            MultiFormatWriter().encode(value, format, 1, 1)
            null
        } catch (_: Throwable) {
            when (type) {
                QrCategoryType.DATA_MATRIX -> msg(
                    context,
                    R.string.barcode_validation_too_large_data_matrix,
                    "This value is too large for a Data Matrix symbol."
                )
                QrCategoryType.PDF_417 -> msg(
                    context,
                    R.string.barcode_validation_too_large_pdf417,
                    "This value is too large for a PDF417 symbol."
                )
                QrCategoryType.AZTEC -> msg(
                    context,
                    R.string.barcode_validation_too_large_aztec,
                    "This value is too large for an Aztec symbol."
                )
                else -> msg(
                    context,
                    R.string.barcode_validation_cannot_encode,
                    "This value cannot be encoded."
                )
            }
        }
    }

    private fun msg(context: Context?, @StringRes resId: Int, english: String, vararg formatArgs: Any): String =
        if (context != null) {
            if (formatArgs.isEmpty()) context.getString(resId) else context.getString(resId, *formatArgs)
        } else {
            english
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
