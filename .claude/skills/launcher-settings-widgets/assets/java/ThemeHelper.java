package com.qrcode.scanner.launcher.helpers;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeHelper {
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;

    private static final String PREFS_NAME = "theme_preferences";
    private static final String KEY_THEME = "selected_theme";
    private static final String KEY_THEME_CHANGED_PENDING = "theme_changed_pending";
    private static final String KEY_RETURN_LAUNCHER_HOME = "return_launcher_home_on_resume";

    private ThemeHelper() {
    }

    public static int getSavedTheme(Context context) {
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getInt(KEY_THEME, THEME_LIGHT);
    }

    public static void saveTheme(Context context, int theme) {
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sharedPreferences.edit().putInt(KEY_THEME, theme).apply();
    }

    public static void applyTheme(int theme) {
        if (theme == THEME_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    public static void applySavedTheme(Context context) {
        applyTheme(getSavedTheme(context));
    }

    public static void setTheme(Context context, int theme) {
        saveTheme(context, theme);
        applyTheme(theme);
        markThemeChangedPending(context);
    }

    public static void markThemeChangedPending(Context context) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_THEME_CHANGED_PENDING, true).apply();
    }

    public static void clearThemeChangedPending(Context context) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_THEME_CHANGED_PENDING, false).apply();
    }

    public static boolean consumeThemeChangedPendingForHome(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(KEY_THEME_CHANGED_PENDING, false)) {
            return false;
        }
        sharedPreferences.edit().putBoolean(KEY_THEME_CHANGED_PENDING, false).apply();
        return true;
    }

    public static void markLauncherReturnHomeOnNextResume(Context context) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_RETURN_LAUNCHER_HOME, true).apply();
    }

    public static boolean consumeLauncherReturnHomeOnResume(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(KEY_RETURN_LAUNCHER_HOME, false)) {
            return false;
        }
        sharedPreferences.edit().putBoolean(KEY_RETURN_LAUNCHER_HOME, false).apply();
        return true;
    }

    public static void clearLauncherReturnHomeOnResume(Context context) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_RETURN_LAUNCHER_HOME, false).apply();
    }
}
