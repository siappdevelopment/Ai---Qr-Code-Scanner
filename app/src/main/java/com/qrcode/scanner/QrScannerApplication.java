package com.qrcode.scanner;

import android.app.Application;
import android.util.Log;

import com.qrcode.scanner.data.settings.SettingsRepositoryKt;
import com.qrcode.scanner.launcher.activities.EventPromptActivity;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.EventPromptWatch;
import com.qrcode.scanner.launcher.remote.RemoteConfigHelper;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

/**
 * Process entry for the QR app and the migrated launcher shell.
 * Remote Config starts here and does not block startup. A splash callback joins this fetch instead of being dropped.
 * Saved language and night mode are restored here. Process app-open follows the source lifecycle, which clears the ad and skips showing it.
 * AdMob initializes only when an app id is present. Analytics is best-effort and is not initialized here.
 * Saved call-end config is loaded here so a cold phone-state broadcast can read it.
 * Light stays light and Dark stays dark for the launcher, the QR app, and onboarding.
 * The phone's system theme is not used.
 */
public class QrScannerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("EventPrompt", "application started");
        AppUtils.restoreSavedLanguage(this);
        SettingsRepositoryKt.applyStoredAppNightMode(this);
        RemoteConfigValues.ensureLoaded(this);
        Log.d("EventPrompt", "startup config uninstall=" + RemoteConfigValues.isEventScreenEnabled(EventPromptActivity.KIND_UNINSTALL)
                + " install=" + RemoteConfigValues.isEventScreenEnabled(EventPromptActivity.KIND_INSTALL)
                + " chargeIn=" + RemoteConfigValues.isEventScreenEnabled(EventPromptActivity.KIND_CHARGE_IN)
                + " chargeOut=" + RemoteConfigValues.isEventScreenEnabled(EventPromptActivity.KIND_CHARGE_OUT));
        EventPromptWatch.register(this);
        AdPlacement.initializeIfConfigured(this);
        new com.qrcode.scanner.launcher.common.ProcessAppOpen(this).register();
        RemoteConfigHelper.fetchRemoteConfig(this, null);
        RemoteConfigHelper.watchNetwork(this);
    }
}
