package com.qrcode.scanner;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.ThemeUtils;
import com.qrcode.scanner.launcher.remote.RemoteConfigHelper;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

/**
 * Process entry for the QR app and the migrated launcher shell.
 * Remote Config starts here and does not block startup. A splash callback joins this fetch instead of being dropped.
 * Saved language and night mode are restored here. Process app-open follows the source lifecycle, which clears the ad and skips showing it.
 * AdMob initializes only when an app id is present. Analytics is best-effort and is not initialized here.
 * Saved call-end config is loaded here so a cold phone-state broadcast can read it.
 * Compose QR screens keep their own Settings theme. Launcher night mode uses the source APP_PREF theme.
 */
public class QrScannerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppUtils.restoreSavedLanguage(this);
        applySavedTheme();
        RemoteConfigValues.ensureLoaded(this);
        AdPlacement.initializeIfConfigured(this);
        new com.qrcode.scanner.launcher.common.ProcessAppOpen(this).register();
        RemoteConfigHelper.fetchRemoteConfig(this, null);
        RemoteConfigHelper.watchNetwork(this);
    }

    private void applySavedTheme() {
        int nightMode;
        switch (ThemeUtils.getTheme(this)) {
            case ThemeUtils.THEME_LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case ThemeUtils.THEME_DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
