package com.qrcode.scanner

import com.google.mlkit.vision.barcode.common.Barcode
import com.qrcode.scanner.data.history.ScanPayloadMapper
import com.qrcode.scanner.ui.screens.create.QrCategoryType
import com.qrcode.scanner.ui.screens.create.QrPayloadBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class QrPayloadBuilderTest {

    @Test
    fun website_normalAndWithoutScheme() {
        val withScheme = QrPayloadBuilder.build(
            QrCategoryType.WEBSITE,
            QrPayloadBuilder.FormInput(primary = "https://scanpulse.test/path")
        )
        assertEquals("https://scanpulse.test/path", withScheme.payload)

        val noScheme = QrPayloadBuilder.build(
            QrCategoryType.WEBSITE,
            QrPayloadBuilder.FormInput(primary = "scanpulse.test/path")
        )
        assertEquals("https://scanpulse.test/path", noScheme.payload)
    }

    @Test
    fun plainText_unicodeMultilineSpecial() {
        val text = "Hello 你好\nLine2 & <tag> % #"
        val built = QrPayloadBuilder.build(
            QrCategoryType.PLAIN_TEXT,
            QrPayloadBuilder.FormInput(primary = text)
        )
        assertEquals(text.trim(), built.payload)
    }

    @Test
    fun contact_fieldsIndependent() {
        val nameOnly = QrPayloadBuilder.build(
            QrCategoryType.CONTACT,
            QrPayloadBuilder.FormInput(primary = "Ada")
        )
        assertTrue(nameOnly.payload.contains("FN:Ada"))
        assertFalse(nameOnly.payload.contains("EMAIL:"))
        assertFalse(nameOnly.payload.contains("TITLE:"))
        assertFalse(nameOnly.payload.contains("TEL:"))

        val all = QrPayloadBuilder.build(
            QrCategoryType.CONTACT,
            QrPayloadBuilder.FormInput(
                primary = "Ada; Lovelace",
                secondary = "ada@example.com",
                quaternary = "Engineer, Lead",
                tertiary = "+1 555 0100"
            )
        )
        assertTrue(all.payload.contains("FN:Ada\\; Lovelace"))
        assertTrue(all.payload.contains("EMAIL:ada@example.com"))
        assertTrue(all.payload.contains("TITLE:Engineer\\, Lead"))
        assertTrue(all.payload.contains("TEL:+1 555 0100"))

        val unicode = QrPayloadBuilder.build(
            QrCategoryType.CONTACT,
            QrPayloadBuilder.FormInput(primary = "田中 太郎", secondary = "t@例.jp")
        )
        assertTrue(unicode.payload.contains("FN:田中 太郎"))
        assertTrue(unicode.payload.contains("EMAIL:t@例.jp"))
    }

    @Test
    fun wifi_wpaOpenHiddenEscapes() {
        val wpa = QrPayloadBuilder.build(
            QrCategoryType.WIFI,
            QrPayloadBuilder.FormInput(
                primary = "My;Network",
                secondary = "pa;ss:word",
                wifiSecurity = QrPayloadBuilder.WifiSecurity.WPA
            )
        )
        assertEquals("WIFI:T:WPA;S:My\\;Network;P:pa\\;ss:word;H:false;;", wpa.payload)

        val open = QrPayloadBuilder.build(
            QrCategoryType.WIFI,
            QrPayloadBuilder.FormInput(
                primary = "Guest",
                wifiSecurity = QrPayloadBuilder.WifiSecurity.OPEN
            )
        )
        assertEquals("WIFI:T:nopass;S:Guest;H:false;;", open.payload)

        val hidden = QrPayloadBuilder.build(
            QrCategoryType.WIFI,
            QrPayloadBuilder.FormInput(
                primary = "Net\"A\\B,C",
                secondary = "x",
                wifiSecurity = QrPayloadBuilder.WifiSecurity.WPA,
                wifiHidden = true
            )
        )
        assertTrue(hidden.payload.contains("S:Net\\\"A\\\\B\\,C"))
        assertTrue(hidden.payload.contains("H:true;;"))
    }

    @Test
    fun phone_plusAndFormatted() {
        val built = QrPayloadBuilder.build(
            QrCategoryType.PHONE,
            QrPayloadBuilder.FormInput(primary = "+1 (555) 123-4567")
        )
        assertEquals("tel:+15551234567", built.payload)
    }

    @Test
    fun email_bodyRequired() {
        assertEquals(
            "Enter an email body",
            QrPayloadBuilder.validate(
                QrCategoryType.EMAIL,
                QrPayloadBuilder.FormInput(primary = "a@b.com", secondary = "Hi")
            )
        )
    }

    @Test
    fun plainText_rejectsPayloadThatCannotFitInQr() {
        val huge = "a".repeat(QrPayloadBuilder.MAX_QR_PAYLOAD_BYTES + 1)
        assertEquals(
            "This field is limited to 500 characters.",
            QrPayloadBuilder.validate(
                QrCategoryType.PLAIN_TEXT,
                QrPayloadBuilder.FormInput(primary = huge)
            )
        )
        val encodedHeavy = "你".repeat(450)
        assertEquals(
            "This is too long to fit in a QR code. Shorten the text.",
            QrPayloadBuilder.validate(
                QrCategoryType.PLAIN_TEXT,
                QrPayloadBuilder.FormInput(primary = encodedHeavy)
            )
        )
    }

    @Test
    fun email_specialSubjectBody() {
        val built = QrPayloadBuilder.build(
            QrCategoryType.EMAIL,
            QrPayloadBuilder.FormInput(
                primary = "a@b.com",
                secondary = "Hi & Bye %",
                tertiary = "Body : ; ?"
            )
        )
        assertTrue(built.payload.startsWith("mailto:a@b.com?"))
        assertTrue(built.payload.contains("subject="))
        assertTrue(built.payload.contains("body="))
        assertFalse(built.payload.contains("subject=Hi & Bye"))
    }

    @Test
    fun sms_bodyEncodingAndEmpty() {
        assertEquals(
            "Enter a message",
            QrPayloadBuilder.validate(
                QrCategoryType.SMS,
                QrPayloadBuilder.FormInput(primary = "+15551234567")
            )
        )
        try {
            QrPayloadBuilder.build(
                QrCategoryType.SMS,
                QrPayloadBuilder.FormInput(primary = "+15551234567")
            )
            fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }

        val colon = QrPayloadBuilder.build(
            QrCategoryType.SMS,
            QrPayloadBuilder.FormInput(primary = "15551234567", secondary = "A:B:C")
        )
        assertEquals("sms:15551234567?body=A%3AB%3AC", colon.payload)

        val specials = QrPayloadBuilder.build(
            QrCategoryType.SMS,
            QrPayloadBuilder.FormInput(primary = "15551234567", secondary = "a;b&c%d\n你好")
        )
        assertTrue(specials.payload.startsWith("sms:15551234567?body="))
        assertTrue(specials.payload.contains("%"))
        assertFalse(specials.payload.contains("sms:15551234567:a;b"))
    }

    @Test
    fun whatsapp_digitsOnlyPath() {
        assertEquals(
            "https://wa.me/15551234567",
            QrPayloadBuilder.build(
                QrCategoryType.WHATSAPP,
                QrPayloadBuilder.FormInput(primary = "+15551234567")
            ).payload
        )
        assertEquals(
            "https://wa.me/15551234567",
            QrPayloadBuilder.build(
                QrCategoryType.WHATSAPP,
                QrPayloadBuilder.FormInput(primary = "15551234567")
            ).payload
        )
        assertEquals(
            "https://wa.me/15551234567",
            QrPayloadBuilder.build(
                QrCategoryType.WHATSAPP,
                QrPayloadBuilder.FormInput(primary = "+1 555 123 4567")
            ).payload
        )
        assertEquals(
            "https://wa.me/15551234567",
            QrPayloadBuilder.build(
                QrCategoryType.WHATSAPP,
                QrPayloadBuilder.FormInput(primary = "1-555-123-4567")
            ).payload
        )
        val withText = QrPayloadBuilder.build(
            QrCategoryType.WHATSAPP,
            QrPayloadBuilder.FormInput(primary = "+1 555 123 4567", secondary = "Hello 世界 &")
        )
        assertTrue(withText.payload.startsWith("https://wa.me/15551234567?text="))
        assertTrue(withText.payload.contains("%"))
    }

    @Test
    fun location_andAppLink() {
        val loc = QrPayloadBuilder.build(
            QrCategoryType.LOCATION,
            QrPayloadBuilder.FormInput(primary = "37.7749,-122.4194", secondary = "A&B")
        )
        assertTrue(loc.payload.startsWith("geo:37.7749,-122.4194?q="))

        val pkg = QrPayloadBuilder.build(
            QrCategoryType.APP_LINK,
            QrPayloadBuilder.FormInput(primary = "com.example.app")
        )
        assertEquals("market://details?id=com.example.app", pkg.payload)

        val url = QrPayloadBuilder.build(
            QrCategoryType.APP_LINK,
            QrPayloadBuilder.FormInput(primary = "https://play.google.com/store/apps/details?id=x")
        )
        assertTrue(url.payload.startsWith("https://play.google.com/"))
    }

    @Test
    fun calendar_validInvalidDefault() {
        val valid = QrPayloadBuilder.build(
            QrCategoryType.CALENDAR,
            QrPayloadBuilder.FormInput(primary = "Standup; Daily", secondary = "20260921T090000")
        )
        assertTrue(valid.payload.contains("DTSTART:20260921T090000"))
        assertTrue(valid.payload.contains("SUMMARY:Standup\\; Daily"))

        val dateOnly = QrPayloadBuilder.build(
            QrCategoryType.CALENDAR,
            QrPayloadBuilder.FormInput(primary = "Event", secondary = "20261006")
        )
        assertTrue(dateOnly.payload.contains("DTSTART;VALUE=DATE:20261006"))

        assertNotNull(QrPayloadBuilder.validate(
            QrCategoryType.CALENDAR,
            QrPayloadBuilder.FormInput(primary = "Event")
        ))

        assertNull(QrPayloadBuilder.normalizeCalendarStamp("not-a-date"))
        assertNull(QrPayloadBuilder.normalizeCalendarStamp("20261301T090000")) // invalid month
        assertNull(QrPayloadBuilder.normalizeCalendarStamp("20260230T090000")) // invalid day
        assertNull(QrPayloadBuilder.normalizeCalendarStamp("20260921T250000")) // invalid hour
        assertNotNull(QrPayloadBuilder.normalizeCalendarStamp("2026-09-21T09:00:00"))

        assertNotNull(QrPayloadBuilder.validate(
            QrCategoryType.CALENDAR,
            QrPayloadBuilder.FormInput(primary = "X", secondary = "garbage")
        ))

        try {
            QrPayloadBuilder.build(
                QrCategoryType.CALENDAR,
                QrPayloadBuilder.FormInput(primary = "X", secondary = "bad")
            )
            fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }

        val unicode = QrPayloadBuilder.build(
            QrCategoryType.CALENDAR,
            QrPayloadBuilder.FormInput(primary = "会議 🚀", secondary = "20261006")
        )
        assertTrue(unicode.payload.contains("SUMMARY:会議 🚀"))
    }

    @Test
    fun social_usernameAndFullLink() {
        val facebook = QrPayloadBuilder.build(
            QrCategoryType.FACEBOOK,
            QrPayloadBuilder.FormInput(primary = "@Scan.Pulse")
        )
        assertEquals("https://www.facebook.com/Scan.Pulse", facebook.payload)

        val youtube = QrPayloadBuilder.build(
            QrCategoryType.YOUTUBE,
            QrPayloadBuilder.FormInput(primary = "https://www.youtube.com/watch?v=abc")
        )
        assertEquals("https://www.youtube.com/watch?v=abc", youtube.payload)

        val tiktok = QrPayloadBuilder.build(
            QrCategoryType.TIKTOK,
            QrPayloadBuilder.FormInput(primary = "creator")
        )
        assertEquals("https://www.tiktok.com/@creator", tiktok.payload)

        assertNotNull(
            QrPayloadBuilder.validate(QrCategoryType.INSTAGRAM, QrPayloadBuilder.FormInput())
        )
    }

    @Test
    fun barcodeFormats_normalizeAndEncode() {
        val ean = QrPayloadBuilder.build(
            QrCategoryType.EAN_13,
            QrPayloadBuilder.FormInput(primary = "590123412345")
        )
        assertEquals("5901234123457", ean.payload)
        assertEquals(ScanPayloadMapper.TYPE_BARCODE, ean.detectedType)

        val itf = QrPayloadBuilder.build(
            QrCategoryType.ITF,
            QrPayloadBuilder.FormInput(primary = "1234")
        )
        assertEquals("1234", itf.payload)

        assertNotNull(
            QrPayloadBuilder.validate(
                QrCategoryType.ITF,
                QrPayloadBuilder.FormInput(primary = "123")
            )
        )
        assertNull(
            QrPayloadBuilder.validate(
                QrCategoryType.CODE_39,
                QrPayloadBuilder.FormInput(primary = "hello!")
            )
        )
        assertNotNull(
            QrPayloadBuilder.validate(
                QrCategoryType.CODE_39,
                QrPayloadBuilder.FormInput(primary = "*")
            )
        )

        val matrix = com.google.zxing.MultiFormatWriter().encode(
            "SCAN",
            com.google.zxing.BarcodeFormat.CODE_128,
            200,
            80
        )
        assertTrue(matrix.width > 0)
    }

    @Test
    fun barcode_blocked() {
        assertNotNull(
            QrPayloadBuilder.validate(QrCategoryType.BARCODE, QrPayloadBuilder.FormInput())
        )
    }
}

class ScanPayloadMapperTest {

    @Test
    fun wifi_escapedSemicolonRoundTrip() {
        val payload = QrPayloadBuilder.build(
            QrCategoryType.WIFI,
            QrPayloadBuilder.FormInput(
                primary = "My;Network",
                secondary = "pa;ss:word",
                wifiSecurity = QrPayloadBuilder.WifiSecurity.WPA,
                wifiHidden = true
            )
        ).payload
        assertEquals("My;Network", ScanPayloadMapper.extractWifiSsid(payload))
        assertEquals("pa;ss:word", ScanPayloadMapper.extractWifiPassword(payload))
        assertEquals("true", ScanPayloadMapper.extractWifiParam(payload, "H"))
        assertEquals("WPA", ScanPayloadMapper.extractWifiParam(payload, "T"))
    }

    @Test
    fun wifi_quotesBackslashCommaColonOpen() {
        val payload = QrPayloadBuilder.build(
            QrCategoryType.WIFI,
            QrPayloadBuilder.FormInput(
                primary = "A\"B\\C,D",
                secondary = "p:w",
                wifiSecurity = QrPayloadBuilder.WifiSecurity.WPA
            )
        ).payload
        assertEquals("A\"B\\C,D", ScanPayloadMapper.extractWifiSsid(payload))
        assertEquals("p:w", ScanPayloadMapper.extractWifiPassword(payload))

        val open = QrPayloadBuilder.build(
            QrCategoryType.WIFI,
            QrPayloadBuilder.FormInput(
                primary = "OpenNet",
                wifiSecurity = QrPayloadBuilder.WifiSecurity.OPEN
            )
        ).payload
        assertEquals("OpenNet", ScanPayloadMapper.extractWifiSsid(open))
        assertNull(ScanPayloadMapper.extractWifiPassword(open))
        assertEquals("nopass", ScanPayloadMapper.extractWifiParam(open, "T"))
    }

    @Test
    fun detectType_structuredPayloads() {
        val fmt = Barcode.FORMAT_QR_CODE
        assertEquals(
            ScanPayloadMapper.TYPE_WIFI,
            ScanPayloadMapper.detectType("WIFI:T:WPA;S:A;P:b;H:false;;", fmt, "QR_CODE")
        )
        assertEquals(
            ScanPayloadMapper.TYPE_WEBSITE,
            ScanPayloadMapper.detectType("https://wa.me/15551234567", fmt, "QR_CODE")
        )
        assertEquals(
            ScanPayloadMapper.TYPE_QR_CODE,
            ScanPayloadMapper.detectType("BEGIN:VCARD\nVERSION:3.0\nFN:A\nEND:VCARD", fmt, "QR_CODE")
        )
        assertEquals(
            ScanPayloadMapper.TYPE_QR_CODE,
            ScanPayloadMapper.detectType("mailto:a@b.com?subject=Hi", fmt, "QR_CODE")
        )
        assertEquals(
            ScanPayloadMapper.TYPE_QR_CODE,
            ScanPayloadMapper.detectType("tel:+15551234567", fmt, "QR_CODE")
        )
        assertEquals(
            ScanPayloadMapper.TYPE_QR_CODE,
            ScanPayloadMapper.detectType("SMSTO:1555:hi", fmt, "QR_CODE")
        )
        assertEquals(
            ScanPayloadMapper.TYPE_QR_CODE,
            ScanPayloadMapper.detectType("sms:+1555?body=Hi%3A", fmt, "QR_CODE")
        )
        assertEquals(
            ScanPayloadMapper.TYPE_QR_CODE,
            ScanPayloadMapper.detectType("geo:1.0,2.0", fmt, "QR_CODE")
        )
        assertEquals(
            ScanPayloadMapper.TYPE_QR_CODE,
            ScanPayloadMapper.detectType(
                "BEGIN:VCALENDAR\nVERSION:2.0\nBEGIN:VEVENT\nSUMMARY:A\nDTSTART:20260101T090000\nEND:VEVENT\nEND:VCALENDAR",
                fmt,
                "QR_CODE"
            )
        )
    }
}
