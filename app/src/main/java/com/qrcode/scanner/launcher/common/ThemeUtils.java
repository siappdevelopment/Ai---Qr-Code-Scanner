package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeUtils {
    public static final String PREF_THEME = "pref_theme";
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private ThemeUtils() {
    }

    public static void setTheme(Context context, int theme) {
        if (context == null) {
            return;
        }
        SharedPreferences.Editor editor = context.getSharedPreferences("APP_PREF", Context.MODE_PRIVATE).edit();
        editor.putInt(PREF_THEME, theme);
        editor.apply();
    }

    public static int getTheme(Context context) {
        if (context == null) {
            return THEME_LIGHT;
        }
        int stored = context.getSharedPreferences("APP_PREF", Context.MODE_PRIVATE)
                .getInt(PREF_THEME, THEME_LIGHT);
        if (stored == THEME_SYSTEM) {
            return THEME_LIGHT;
        }
        return stored;
    }
}
