package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Custom Firebase Analytics events from "QR Code Scanner & Generator - Firebase Event Naming Guide".
 * Names are lowercase with underscores, as the guide requires. The events that were already in the app
 * (see {@link AppUtils#trackScreen}) are untouched; these are additional events.
 * <p>
 * Every event carries the guide's common parameters {@code platform} and {@code app_version}.
 */
public final class AnalyticsEvents {
    // Core app flow
    public static final String APP_OPEN = "app_open";
    public static final String SPLASH_VIEW = "splash_view";
    public static final String LANGUAGE_VIEW = "language_view";
    public static final String INTRO_1_VIEW = "intro_1_view";
    public static final String INTRO_2_VIEW = "intro_2_view";
    public static final String INTRO_3_VIEW = "intro_3_view";
    public static final String MAIN_VIEW = "main_view";
    public static final String PERMISSION_VIEW = "permission_view";
    public static final String LAUNCHER_HOME_VIEW = "launcher_home_view";
    public static final String LAUNCHER_SETTINGS_VIEW = "launcher_settings_view";
    public static final String SETTINGS_VIEW = "settings_view";

    // QR scanner
    public static final String QR_SCANNER_VIEW = "qr_scanner_view";
    public static final String QR_SCAN_START = "qr_scan_start";
    public static final String QR_SCAN_SUCCESS = "qr_scan_success";
    public static final String QR_SCAN_FAIL = "qr_scan_fail";
    public static final String QR_RESULT_VIEW = "qr_result_view";
    public static final String QR_RESULT_OPEN = "qr_result_open";
    public static final String QR_RESULT_COPY = "qr_result_copy";
    public static final String QR_RESULT_SHARE = "qr_result_share";

    // QR generator
    public static final String QR_EDIT_VIEW = "qr_edit_view";
    public static final String QR_GENERATE_START = "qr_generate_start";
    public static final String QR_GENERATE_SUCCESS = "qr_generate_success";
    public static final String QR_GENERATE_FAIL = "qr_generate_fail";
    public static final String QR_PREVIEW_VIEW = "qr_preview_view";
    public static final String QR_SAVE = "qr_save";
    public static final String QR_SHARE = "qr_share";
    public static final String BARCODE_VIEW = "barcode_view";

    // Ads (load / fail / preload, see the audit report for what is intentionally left to Firebase)
    public static final String BANNER_LOAD = "banner_load";
    public static final String BANNER_FAIL = "banner_fail";
    public static final String NATIVE_LOAD = "native_load";
    public static final String NATIVE_FAIL = "native_fail";
    public static final String INTERSTITIAL_LOAD = "interstitial_load";
    public static final String INTERSTITIAL_FAIL = "interstitial_fail";
    public static final String INTERSTITIAL_PRELOAD = "interstitial_preload";
    public static final String APP_OPEN_AD_LOAD = "app_open_ad_load";
    public static final String APP_OPEN_AD_FAIL = "app_open_ad_fail";

    public static final String FORMAT_BANNER = "banner";
    public static final String FORMAT_NATIVE = "native";
    public static final String FORMAT_INTERSTITIAL = "interstitial";
    public static final String FORMAT_APP_OPEN = "app_open";

    private AnalyticsEvents() {
    }

    /** Logs an event with the common parameters. {@code keyValues} are alternating parameter names and values. */
    public static void log(@Nullable Context context, String event, String... keyValues) {
        if (context == null || event == null || event.isEmpty()) {
            return;
        }
        try {
            Context app = context.getApplicationContext();
            Bundle bundle = new Bundle();
            bundle.putString("platform", "android");
            bundle.putString("app_version", appVersion(app));
            for (int i = 0; i + 1 < keyValues.length; i += 2) {
                if (keyValues[i] != null && keyValues[i + 1] != null) {
                    bundle.putString(keyValues[i], keyValues[i + 1]);
                }
            }
            FirebaseAnalytics.getInstance(app).logEvent(event, bundle);
        } catch (Exception ignored) {
        }
    }

    /** Ad events: {@code ad_format} and {@code ad_unit} are the guide's "Ads" parameters. */
    public static void ad(@Nullable Context context, String event, String adFormat, @Nullable String adUnit) {
        log(context, event, "ad_format", adFormat, "ad_unit", adUnit == null ? "" : adUnit.trim());
    }

    private static String appVersion(Context app) {
        try {
            PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (Exception e) {
            return "";
        }
    }
}
