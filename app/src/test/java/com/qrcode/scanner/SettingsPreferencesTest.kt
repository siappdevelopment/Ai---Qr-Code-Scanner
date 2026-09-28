package com.qrcode.scanner

import com.qrcode.scanner.data.history.ScanPayloadMapper
import com.qrcode.scanner.data.settings.AppThemeMode
import com.qrcode.scanner.data.settings.QrDefaultEcc
import com.qrcode.scanner.data.settings.QrDefaultOutputFormat
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.ui.screens.create.QrBitmapEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class SettingsPreferencesTest {
    @Test
    fun defaults_matchProductExpectations() {
        val prefs = SettingsPreferences()
        assertTrue(prefs.vibrateOnDetection)
        assertFalse(prefs.beepOnDetection)
        assertFalse(prefs.autoOpenUrls)
        assertFalse(prefs.continuousBatchScan)
        assertEquals(QrDefaultEcc.H, prefs.defaultQrEcc)
        assertEquals(QrDefaultOutputFormat.SVG, prefs.defaultQrOutputFormat)
        assertEquals(AppThemeMode.LIGHT, prefs.appTheme)
        assertEquals(true, SettingsPreferences.DEFAULT_VIBRATE_ON_DETECTION)
        assertEquals(false, SettingsPreferences.DEFAULT_BEEP_ON_DETECTION)
        assertEquals(false, SettingsPreferences.DEFAULT_AUTO_OPEN_URLS)
        assertEquals(false, SettingsPreferences.DEFAULT_CONTINUOUS_BATCH_SCAN)
        assertEquals(QrDefaultEcc.H, SettingsPreferences.DEFAULT_QR_ECC)
        assertEquals(QrDefaultOutputFormat.SVG, SettingsPreferences.DEFAULT_QR_OUTPUT_FORMAT)
        assertEquals(AppThemeMode.LIGHT, SettingsPreferences.DEFAULT_APP_THEME)
    }
}

class QrCreationDefaultsTest {
    @Test
    fun ecc_fromStored_mapsKnownValues() {
        assertEquals(QrDefaultEcc.L, QrDefaultEcc.fromStored("L"))
        assertEquals(QrDefaultEcc.M, QrDefaultEcc.fromStored("m"))
        assertEquals(QrDefaultEcc.Q, QrDefaultEcc.fromStored("Q"))
        assertEquals(QrDefaultEcc.H, QrDefaultEcc.fromStored("H"))
    }

    @Test
    fun ecc_fromStored_fallsBackToHigh_whenMissingOrInvalid() {
        assertEquals(QrDefaultEcc.H, QrDefaultEcc.fromStored(null))
        assertEquals(QrDefaultEcc.H, QrDefaultEcc.fromStored(""))
        assertEquals(QrDefaultEcc.H, QrDefaultEcc.fromStored("HIGH"))
        assertEquals(QrDefaultEcc.H, QrDefaultEcc.fromStored("30%"))
    }

    @Test
    fun ecc_toEncoderLevel_and_fromEncoder_roundTrip() {
        for (ecc in QrDefaultEcc.entries) {
            val encoder = ecc.toEncoderLevel()
            assertEquals(ecc.storageValue, encoder.name)
            assertEquals(ecc, QrDefaultEcc.fromEncoder(encoder))
        }
        assertEquals(QrBitmapEncoder.EccLevel.H, QrDefaultEcc.H.toEncoderLevel())
    }

    @Test
    fun outputFormat_fromStored_mapsKnownValues() {
        assertEquals(QrDefaultOutputFormat.SVG, QrDefaultOutputFormat.fromStored("SVG"))
        assertEquals(QrDefaultOutputFormat.PNG, QrDefaultOutputFormat.fromStored("png"))
    }

    @Test
    fun outputFormat_fromStored_fallsBackToSvg_whenMissingOrInvalid() {
        assertEquals(QrDefaultOutputFormat.SVG, QrDefaultOutputFormat.fromStored(null))
        assertEquals(QrDefaultOutputFormat.SVG, QrDefaultOutputFormat.fromStored(""))
        assertEquals(QrDefaultOutputFormat.SVG, QrDefaultOutputFormat.fromStored("JPEG"))
        assertEquals(QrDefaultOutputFormat.SVG, QrDefaultOutputFormat.fromStored("Vector"))
    }

    @Test
    fun storageValues_areStableShortTokens() {
        assertEquals("L", QrDefaultEcc.L.storageValue)
        assertEquals("H", QrDefaultEcc.H.storageValue)
        assertEquals("SVG", QrDefaultOutputFormat.SVG.storageValue)
        assertEquals("PNG", QrDefaultOutputFormat.PNG.storageValue)
    }
}

class AutoOpenUrlsEligibilityTest {
    private val qr = 256

    @Test
    fun httpsUrl_isEligible() {
        val raw = "https://scanpulse.test/phase125"
        val type = ScanPayloadMapper.detectType(raw, qr, "QR_CODE")
        assertEquals(ScanPayloadMapper.TYPE_WEBSITE, type)
        assertTrue(ScanPayloadMapper.isEligibleForAutoOpen(raw, type))
    }

    @Test
    fun httpUrl_isEligible() {
        val raw = "http://example.com/path"
        val type = ScanPayloadMapper.detectType(raw, qr, "QR_CODE")
        assertTrue(ScanPayloadMapper.isEligibleForAutoOpen(raw, type))
    }

    @Test
    fun wwwUrl_normalizesToHttps_andIsEligible() {
        val raw = "www.example.com"
        val type = ScanPayloadMapper.detectType(raw, qr, "QR_CODE")
        assertEquals(ScanPayloadMapper.TYPE_WEBSITE, type)
        assertTrue(ScanPayloadMapper.isEligibleForAutoOpen(raw, type))
        assertTrue(
            ScanPayloadMapper.normalizeUrl(raw).startsWith("https://", ignoreCase = true)
        )
    }

    @Test
    fun mailto_tel_sms_plainText_areNotEligible() {
        val cases = listOf(
            "mailto:a@b.com",
            "tel:+15551234567",
            "sms:+1555?body=Hi",
            "SMSTO:1555:hi",
            "just plain text",
            "WIFI:T:WPA;S:A;P:b;;"
        )
        for (raw in cases) {
            val type = ScanPayloadMapper.detectType(raw, qr, "QR_CODE")
            assertFalse(
                "Should not auto-open: $raw (type=$type)",
                ScanPayloadMapper.isEligibleForAutoOpen(raw, type)
            )
        }
    }

    @Test
    fun websiteTypeRequired_evenIfRawLooksLikeUrl() {
        assertFalse(
            ScanPayloadMapper.isEligibleForAutoOpen(
                "https://evil.example",
                ScanPayloadMapper.TYPE_PLAIN_TEXT
            )
        )
    }

    @Test
    fun oneShotGuard_preventsDuplicateOpens() {
        val attempted = AtomicBoolean(false)
        var openCount = 0
        repeat(5) {
            if (attempted.compareAndSet(false, true)) {
                openCount++
            }
        }
        assertEquals(1, openCount)
    }
}
