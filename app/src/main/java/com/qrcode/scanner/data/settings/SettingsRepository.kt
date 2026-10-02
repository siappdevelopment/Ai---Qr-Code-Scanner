package com.qrcode.scanner.data.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.qrcode.scanner.launcher.common.ThemeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private const val SETTINGS_DATASTORE_NAME = "scanpulse_settings"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME
)

class SettingsRepository(
    private val appContext: Context,
    private val dataStore: DataStore<Preferences>
) {
    val preferences: Flow<SettingsPreferences> = dataStore.data.map { prefs ->
        SettingsPreferences(
            vibrateOnDetection = prefs[SettingsKeys.VIBRATE_ON_DETECTION]
                ?: SettingsPreferences.DEFAULT_VIBRATE_ON_DETECTION,
            beepOnDetection = prefs[SettingsKeys.BEEP_ON_DETECTION]
                ?: SettingsPreferences.DEFAULT_BEEP_ON_DETECTION,
            autoOpenUrls = prefs[SettingsKeys.AUTO_OPEN_URLS]
                ?: SettingsPreferences.DEFAULT_AUTO_OPEN_URLS,
            continuousBatchScan = prefs[SettingsKeys.CONTINUOUS_BATCH_SCAN]
                ?: SettingsPreferences.DEFAULT_CONTINUOUS_BATCH_SCAN,
            defaultQrEcc = QrDefaultEcc.fromStored(prefs[SettingsKeys.DEFAULT_QR_ECC]),
            defaultQrOutputFormat = QrDefaultOutputFormat.fromStored(
                prefs[SettingsKeys.DEFAULT_QR_OUTPUT_FORMAT]
            ),
            appTheme = AppThemeMode.fromStored(prefs[SettingsKeys.APP_THEME])
        )
    }

    suspend fun setVibrateOnDetection(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.VIBRATE_ON_DETECTION] = enabled
        }
    }

    suspend fun setBeepOnDetection(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.BEEP_ON_DETECTION] = enabled
        }
    }

    suspend fun setAutoOpenUrls(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.AUTO_OPEN_URLS] = enabled
        }
    }

    suspend fun setContinuousBatchScan(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.CONTINUOUS_BATCH_SCAN] = enabled
        }
    }

    suspend fun setDefaultQrEcc(ecc: QrDefaultEcc) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.DEFAULT_QR_ECC] = ecc.storageValue
        }
    }

    suspend fun setDefaultQrOutputFormat(format: QrDefaultOutputFormat) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.DEFAULT_QR_OUTPUT_FORMAT] = format.storageValue
        }
    }

    suspend fun setAppTheme(theme: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.APP_THEME] = theme.storageValue
        }
        applyAppNightMode(appContext, theme)
    }
}

/** Night mode for every screen: Light or Dark from Settings. Never the phone theme. */
fun readAppNightMode(context: Context): Int {
    val stored = runBlocking {
        context.applicationContext.settingsDataStore.data.first()[SettingsKeys.APP_THEME]
    }
    return nightModeFor(AppThemeMode.fromStored(stored))
}

/** Applies the saved Light/Dark choice to Compose, launcher, and onboarding. */
fun applyStoredAppNightMode(context: Context) {
    applyAppNightMode(context, AppThemeMode.fromStored(runBlocking {
        context.applicationContext.settingsDataStore.data.first()[SettingsKeys.APP_THEME]
    }))
}

private fun nightModeFor(theme: AppThemeMode): Int {
    return if (theme == AppThemeMode.DARK) {
        AppCompatDelegate.MODE_NIGHT_YES
    } else {
        AppCompatDelegate.MODE_NIGHT_NO
    }
}

private fun applyAppNightMode(context: Context, theme: AppThemeMode) {
    AppCompatDelegate.setDefaultNightMode(nightModeFor(theme))
    ThemeUtils.setTheme(
        context.applicationContext,
        if (theme == AppThemeMode.DARK) ThemeUtils.THEME_DARK else ThemeUtils.THEME_LIGHT
    )
}

object SettingsRepositoryProvider {
    @Volatile
    private var instance: SettingsRepository? = null

    fun get(context: Context): SettingsRepository {
        return instance ?: synchronized(this) {
            instance ?: SettingsRepository(
                context.applicationContext,
                context.applicationContext.settingsDataStore
            ).also { instance = it }
        }
    }
}
