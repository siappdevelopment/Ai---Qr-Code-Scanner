package com.qrcode.scanner

import com.qrcode.scanner.data.settings.SettingsPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionFeedbackDefaultsTest {
    @Test
    fun scannerUsesSameDefaultsAsSettingsDataStore() {
        val prefs = SettingsPreferences()
        assertTrue(
            "Scanner must default vibrate ON when DataStore has no value",
            prefs.vibrateOnDetection
        )
        assertFalse(
            "Scanner must default beep OFF when DataStore has no value",
            prefs.beepOnDetection
        )
    }
}
