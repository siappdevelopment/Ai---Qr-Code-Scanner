package com.qrcode.scanner

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.qrcode.scanner.ui.screens.create.BarcodeSymbology
import com.qrcode.scanner.ui.screens.create.QrCategoryType
import com.qrcode.scanner.ui.screens.create.QrPayloadBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BarcodeFormValidationTest {

    @Test
    fun ean13_generatesCheckDigitFrom12() {
        val built = build(QrCategoryType.EAN_13, "590123412345")
        assertEquals("5901234123457", built)
        encode(built, BarcodeFormat.EAN_13)
    }

    @Test
    fun ean13_acceptsMatching13AndRejectsMismatch() {
        assertNull(validate(QrCategoryType.EAN_13, "5901234123457"))
        assertEquals("5901234123457", build(QrCategoryType.EAN_13, "5901234123457"))
        assertEquals(
            "Check digit does not match.",
            validate(QrCategoryType.EAN_13, "5901234123458")
        )
        assertRejected(QrCategoryType.EAN_13, "5901234123458", "Check digit does not match.")
        assertEquals(
            "Enter 12 digits, or 13 digits including the check digit.",
            validate(QrCategoryType.EAN_13, "59012341234567")
        )
        assertEquals("Digits only.", validate(QrCategoryType.EAN_13, "59012341234A"))
    }

    @Test
    fun ean8_generatesAndValidatesCheckDigit() {
        val data = "5512345"
        val full = data + BarcodeSymbology.checkDigit(data)
        assertEquals(full, build(QrCategoryType.EAN_8, data))
        assertEquals(full, build(QrCategoryType.EAN_8, full))
        val bad = full.dropLast(1) + ((full.last().digitToInt() + 1) % 10)
        assertEquals("Check digit does not match.", validate(QrCategoryType.EAN_8, bad))
        encode(full, BarcodeFormat.EAN_8)
    }

    @Test
    fun upcA_generatesAndValidatesCheckDigit_andRejects13() {
        val data = "03600029145"
        val full = data + BarcodeSymbology.checkDigit(data)
        assertEquals(12, full.length)
        assertEquals(full, build(QrCategoryType.UPC_A, data))
        assertEquals(full, build(QrCategoryType.UPC_A, full))
        val bad = full.dropLast(1) + ((full.last().digitToInt() + 1) % 10)
        assertEquals("Check digit does not match.", validate(QrCategoryType.UPC_A, bad))
        assertEquals(
            "Enter 11 digits, or 12 digits including the check digit.",
            validate(QrCategoryType.UPC_A, "0$full")
        )
        encode(full, BarcodeFormat.UPC_A)
    }

    @Test
    fun upcE_expandsValidPattern_andRejectsBadCheckOrNumberSystem() {
        val seven = "0123456"
        assertNull(validate(QrCategoryType.UPC_E, seven))
        val full = build(QrCategoryType.UPC_E, seven)
        assertEquals(8, full.length)
        assertTrue(full.startsWith(seven))
        assertEquals(full, build(QrCategoryType.UPC_E, full))
        encode(full, BarcodeFormat.UPC_E)

        val bad = full.dropLast(1) + ((full.last().digitToInt() + 1) % 10)
        assertEquals("Check digit does not match.", validate(QrCategoryType.UPC_E, bad))
        assertRejected(QrCategoryType.UPC_E, bad, "Check digit does not match.")
        assertEquals("Invalid UPC-E value.", validate(QrCategoryType.UPC_E, "2123456"))
        assertEquals(
            "Enter 7 digits, or 8 digits including the check digit.",
            validate(QrCategoryType.UPC_E, "123456")
        )
        assertEquals("Digits only.", validate(QrCategoryType.UPC_E, "012345A"))
    }

    @Test
    fun code93_allowsAscii_andLimitsExpandedLength() {
        assertNull(validate(QrCategoryType.CODE_93, "Hello-93!"))
        encode("Hello-93!", BarcodeFormat.CODE_93)
        assertEquals(
            "Code 93 only supports characters up to ASCII 127.",
            validate(QrCategoryType.CODE_93, "café")
        )
        assertNull(validate(QrCategoryType.CODE_93, "A".repeat(80)))
        assertEquals(
            "This value is too long for Code 93.",
            validate(QrCategoryType.CODE_93, "A".repeat(81))
        )
        assertNull(validate(QrCategoryType.CODE_93, "a".repeat(40)))
        encode("a".repeat(40), BarcodeFormat.CODE_93)
        assertEquals(
            "This value is too long for Code 93.",
            validate(QrCategoryType.CODE_93, "a".repeat(41))
        )
    }

    @Test
    fun code39_allowsExtendedAscii_rejectsStar_andLimitsExpandedLength() {
        assertNull(validate(QrCategoryType.CODE_39, "HELLO-39"))
        assertNull(validate(QrCategoryType.CODE_39, "hello!"))
        assertEquals("hello!", build(QrCategoryType.CODE_39, "hello!"))
        encode("hello!", BarcodeFormat.CODE_39)
        assertEquals(
            "Code 39 cannot encode this character.",
            validate(QrCategoryType.CODE_39, "ABC*")
        )
        assertEquals(
            "Code 39 cannot encode this character.",
            validate(QrCategoryType.CODE_39, "café")
        )
        assertNull(validate(QrCategoryType.CODE_39, "A".repeat(80)))
        assertEquals(
            "This value is too long for Code 39.",
            validate(QrCategoryType.CODE_39, "A".repeat(81))
        )
        assertNull(validate(QrCategoryType.CODE_39, "a".repeat(40)))
        assertEquals(
            "This value is too long for Code 39.",
            validate(QrCategoryType.CODE_39, "a".repeat(41))
        )
    }

    @Test
    fun codabar_guards() {
        assertEquals("A1234A", build(QrCategoryType.CODABAR, "1234"))
        assertEquals("A1234B", build(QrCategoryType.CODABAR, "A1234B"))
        assertEquals("T12N", build(QrCategoryType.CODABAR, "t12n"))
        assertEquals("*12E", build(QrCategoryType.CODABAR, "*12E"))
        encode("A1234B", BarcodeFormat.CODABAR)
        encode("T12N", BarcodeFormat.CODABAR)
        assertEquals(
            "Start and stop characters must both be present.",
            validate(QrCategoryType.CODABAR, "A1234")
        )
        assertEquals(
            "Start and stop characters must both be present.",
            validate(QrCategoryType.CODABAR, "A1234T")
        )
        assertEquals(
            "Codabar only allows digits and - $ : / . +.",
            validate(QrCategoryType.CODABAR, "12 34")
        )
        assertEquals(
            "Codabar only allows digits and - $ : / . +.",
            validate(QrCategoryType.CODABAR, "A12BA")
        )
    }

    @Test
    fun itf_requiresEvenDigits_upTo80() {
        assertEquals("1234", build(QrCategoryType.ITF, "1234"))
        encode("1234", BarcodeFormat.ITF)
        assertEquals("Enter an even number of digits.", validate(QrCategoryType.ITF, "123"))
        assertEquals("ITF needs at least 2 digits.", validate(QrCategoryType.ITF, "1"))
        assertEquals("Digits only.", validate(QrCategoryType.ITF, "12A4"))
        assertNull(validate(QrCategoryType.ITF, "1".repeat(80)))
        assertEquals(
            "ITF cannot be longer than 80 digits.",
            validate(QrCategoryType.ITF, "1".repeat(82))
        )
    }

    @Test
    fun code128_rejectsAboveAscii127_andHasNoFakeMaximum() {
        assertNull(validate(QrCategoryType.CODE_128, "Hello 128"))
        encode("Hello 128", BarcodeFormat.CODE_128)
        assertEquals(
            "Code 128 only supports characters up to ASCII 127.",
            validate(QrCategoryType.CODE_128, "café")
        )
        assertNull(validate(QrCategoryType.CODE_128, " "))
        assertEquals("Enter a Code 128 value.", validate(QrCategoryType.CODE_128, ""))
        val longValue = "A".repeat(120)
        assertNull(validate(QrCategoryType.CODE_128, longValue))
        encode(longValue, BarcodeFormat.CODE_128)
    }

    @Test
    fun twoDimensional_acceptsShortText_andReportsCapacity() {
        assertNull(validate(QrCategoryType.DATA_MATRIX, "Scan"))
        assertNull(validate(QrCategoryType.PDF_417, "Scan"))
        assertNull(validate(QrCategoryType.AZTEC, "Scan"))
        encode("Scan", BarcodeFormat.DATA_MATRIX)
        encode("Scan", BarcodeFormat.PDF_417)
        encode("Scan", BarcodeFormat.AZTEC)
        val huge = "A".repeat(4000)
        assertEquals(
            "This value is too large for a Data Matrix symbol.",
            validate(QrCategoryType.DATA_MATRIX, huge)
        )
        assertEquals(
            "This value is too large for a PDF417 symbol.",
            validate(QrCategoryType.PDF_417, huge)
        )
        assertEquals(
            "This value is too large for an Aztec symbol.",
            validate(QrCategoryType.AZTEC, huge)
        )
    }

    private fun validate(type: QrCategoryType, value: String): String? =
        QrPayloadBuilder.validate(type, QrPayloadBuilder.FormInput(primary = value))

    private fun build(type: QrCategoryType, value: String): String =
        QrPayloadBuilder.build(type, QrPayloadBuilder.FormInput(primary = value)).payload

    private fun assertRejected(type: QrCategoryType, value: String, message: String) {
        try {
            build(type, value)
            fail("Expected rejection")
        } catch (error: IllegalArgumentException) {
            assertEquals(message, error.message)
        }
    }

    private fun encode(value: String, format: BarcodeFormat) {
        val matrix = MultiFormatWriter().encode(value, format, 200, 80)
        assertTrue(matrix.width > 0)
    }
}
