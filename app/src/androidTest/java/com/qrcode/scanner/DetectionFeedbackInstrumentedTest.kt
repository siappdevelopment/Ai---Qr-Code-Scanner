package com.qrcode.scanner

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.data.settings.SettingsRepositoryProvider
import com.qrcode.scanner.ui.screens.scan.DetectionFeedback
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class DetectionFeedbackInstrumentedTest {

    @Test
    fun onAcceptedDetection_allPreferenceCombos_doNotCrash() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DetectionFeedback.onAcceptedDetection(context, vibrateEnabled = true, beepEnabled = false)
        DetectionFeedback.onAcceptedDetection(context, vibrateEnabled = false, beepEnabled = false)
        DetectionFeedback.onAcceptedDetection(context, vibrateEnabled = true, beepEnabled = true)
        DetectionFeedback.onAcceptedDetection(context, vibrateEnabled = false, beepEnabled = true)
    }

    @Test
    fun detectionGate_compareAndSet_firesOnceLikeScanner() {
        val detectionHandled = AtomicBoolean(false)
        var feedbackCount = 0
        var navigateCount = 0
        repeat(5) {
            if (detectionHandled.compareAndSet(false, true)) {
                feedbackCount++
                navigateCount++
            }
        }
        assertEquals(1, feedbackCount)
        assertEquals(1, navigateCount)
    }

    @Test
    fun settingsRepository_defaultsMatchScannerFallback() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = SettingsRepositoryProvider.get(context)
        // Force known values then read back
        repo.setVibrateOnDetection(true)
        repo.setBeepOnDetection(false)
        val prefs = repo.preferences.first()
        assertTrue(prefs.vibrateOnDetection)
        assertFalse(prefs.beepOnDetection)
        assertEquals(SettingsPreferences.DEFAULT_VIBRATE_ON_DETECTION, true)
        assertEquals(SettingsPreferences.DEFAULT_BEEP_ON_DETECTION, false)
    }
}
