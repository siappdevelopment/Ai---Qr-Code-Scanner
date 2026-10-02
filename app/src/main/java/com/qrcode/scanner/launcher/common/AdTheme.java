package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import com.qrcode.scanner.data.settings.SettingsRepositoryKt;

/**
 * Ad layouts read light/dark colors from the configuration. Inflate them with
 * the saved app theme, not the phone theme.
 */
public final class AdTheme {
    private AdTheme() {
    }

    public static Context forApp(Context context) {
        return withNight(context, isDark(context));
    }

    public static Context forLauncher(Context context) {
        return withNight(context, isDark(context));
    }

    public static boolean launcherIsDark(Context context) {
        return isDark(context);
    }

    private static boolean isDark(Context context) {
        return SettingsRepositoryKt.readAppNightMode(context) == AppCompatDelegate.MODE_NIGHT_YES;
    }

    private static Context withNight(Context context, boolean dark) {
        Configuration config = new Configuration(context.getResources().getConfiguration());
        int night = dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
        return context.createConfigurationContext(config);
    }
}
