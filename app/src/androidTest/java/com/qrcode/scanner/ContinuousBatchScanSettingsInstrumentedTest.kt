package com.qrcode.scanner

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.data.settings.SettingsRepositoryProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContinuousBatchScanSettingsInstrumentedTest {

    @Test
    fun continuousBatchScan_defaultFalse_andPersistsToggle() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val repo = SettingsRepositoryProvider.get(context)
            assertFalse(SettingsPreferences.DEFAULT_CONTINUOUS_BATCH_SCAN)

            repo.setContinuousBatchScan(false)
            assertFalse(repo.preferences.first().continuousBatchScan)

            repo.setContinuousBatchScan(true)
            assertTrue(repo.preferences.first().continuousBatchScan)

            repo.setContinuousBatchScan(false)
            assertFalse(repo.preferences.first().continuousBatchScan)
        }
    }
}
