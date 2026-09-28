package com.qrcode.scanner

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.data.settings.SettingsRepositoryProvider
import com.qrcode.scanner.ui.screens.scan.ScanIntents
import com.qrcode.scanner.ui.screens.scan.ScanResultActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutoOpenUrlsInstrumentedTest {

    @Test
    fun autoOpenUrls_persistsAcrossReads() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val repo = SettingsRepositoryProvider.get(context)
            repo.setAutoOpenUrls(false)
            assertFalse(repo.preferences.first().autoOpenUrls)
            repo.setAutoOpenUrls(true)
            assertTrue(repo.preferences.first().autoOpenUrls)
            repo.setAutoOpenUrls(false)
            assertFalse(repo.preferences.first().autoOpenUrls)
            assertFalse(SettingsPreferences.DEFAULT_AUTO_OPEN_URLS)
        }
    }

    @Test
    fun scanResult_withHttps_andAutoOpenOff_doesNotCrash() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            SettingsRepositoryProvider.get(context).setAutoOpenUrls(false)
            val intent = Intent(context, ScanResultActivity::class.java).apply {
                putExtra(ScanIntents.EXTRA_RAW_VALUE, "https://scanpulse.test/auto-open-off")
                putExtra(ScanIntents.EXTRA_BARCODE_FORMAT, 256)
                putExtra(ScanIntents.EXTRA_BARCODE_FORMAT_NAME, "QR_CODE")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ActivityScenario.launch<ScanResultActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    assertFalse(activity.isFinishing)
                }
                Thread.sleep(800)
                scenario.onActivity { activity ->
                    assertFalse(activity.isFinishing)
                }
            }
        }
    }

    @Test
    fun scanResult_withMailto_andAutoOpenOn_doesNotCrash() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            SettingsRepositoryProvider.get(context).setAutoOpenUrls(true)
            val intent = Intent(context, ScanResultActivity::class.java).apply {
                putExtra(ScanIntents.EXTRA_RAW_VALUE, "mailto:test@example.com")
                putExtra(ScanIntents.EXTRA_BARCODE_FORMAT, 256)
                putExtra(ScanIntents.EXTRA_BARCODE_FORMAT_NAME, "QR_CODE")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ActivityScenario.launch<ScanResultActivity>(intent).use { scenario ->
                Thread.sleep(800)
                scenario.onActivity { activity ->
                    assertFalse(activity.isFinishing)
                }
            }
            SettingsRepositoryProvider.get(context).setAutoOpenUrls(false)
        }
    }

    @Test
    fun scanResult_withHttps_andAutoOpenOn_resultRemains() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            SettingsRepositoryProvider.get(context).setAutoOpenUrls(true)
            val intent = Intent(context, ScanResultActivity::class.java).apply {
                putExtra(ScanIntents.EXTRA_RAW_VALUE, "https://example.com/phase125-on")
                putExtra(ScanIntents.EXTRA_BARCODE_FORMAT, 256)
                putExtra(ScanIntents.EXTRA_BARCODE_FORMAT_NAME, "QR_CODE")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ActivityScenario.launch<ScanResultActivity>(intent).use { scenario ->
                Thread.sleep(1200)
                scenario.onActivity { activity ->
                    // Auto-open may launch a viewer on top; Result must remain alive.
                    assertFalse(activity.isFinishing)
                }
            }
            SettingsRepositoryProvider.get(context).setAutoOpenUrls(false)
        }
    }
}
