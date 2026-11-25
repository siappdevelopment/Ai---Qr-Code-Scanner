package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * "Welcome back" screen shown when the user comes back to the app from the background, INSTEAD of the App Open ad (the
 * App Open ad is used when this is off). Values are saved so they also work on a cold start.
 * <p>
 * Firebase ({@code Screen.welcome_back_screen}):
 * <pre>
 * "welcome_back_show": true,              // true = Welcome screen, false = normal App Open ad
 * "welcome_back_continue_delay_sec": 2,   // seconds before the Continue button becomes active
 * "welcome_back_native_show": true,       // false = no ad, then the Welcome screen is not used at all
 * "welcome_back_native_id": "ca-app-pub-...", // medium native at the bottom
 * "welcome_back_inter_show": true,        // interstitial when Continue is tapped
 * "welcome_back_inter_id": "ca-app-pub-..."
 * </pre>
 * Profile root: the existing {@code App_Open_Ad_Show} is the master switch for "show something when the app returns from
 * the background" (Welcome screen AND App Open ad); {@code Coming_Soon} (default false) true = nothing shows.
 */
public final class WelcomeBackConfig {
    private static final String PREFS = "welcome_back_config_preferences";
    private static final String K_SHOW = "welcome_back_show";
    private static final String K_DELAY = "welcome_back_continue_delay_sec";
    private static final String K_NATIVE_ID = "welcome_back_native_id";
    private static final String K_NATIVE_SHOW = "welcome_back_native_show";
    private static final String K_INTER_SHOW = "welcome_back_inter_show";
    private static final String K_INTER_ID = "welcome_back_inter_id";
    private static final String K_BG_SHOW = "app_open_show_in_background";
    private static final String K_COMING_SOON = "coming_soon";

    @Nullable
    private static SharedPreferences prefs;

    private WelcomeBackConfig() {
    }

    /** Remote Config: {@code block} = Screen.welcome_back_screen (null = Welcome screen off), {@code root} = profile root. */
    public static void apply(@NonNull Context context, @Nullable JSONObject block, @Nullable JSONObject root) {
        SharedPreferences.Editor editor = prefs(context).edit();
        if (block == null) {
            // A missing block means the Welcome screen is off (the App Open ad is used).
            editor.putBoolean(K_SHOW, false);
        } else {
            editor.putBoolean(K_SHOW, block.optBoolean(K_SHOW, false));
            editor.putInt(K_DELAY, block.optInt(K_DELAY, 2));
            editor.putString(K_NATIVE_ID, block.optString(K_NATIVE_ID, "").trim());
            editor.putBoolean(K_NATIVE_SHOW, block.optBoolean(K_NATIVE_SHOW, true));
            editor.putBoolean(K_INTER_SHOW, block.optBoolean(K_INTER_SHOW, false));
            editor.putString(K_INTER_ID, block.optString(K_INTER_ID, "").trim());
        }
        JSONObject top = root == null ? new JSONObject() : root;
        // The existing App_Open_Ad_Show is the master switch (saved here too, so it works on a cold start).
        editor.putBoolean(K_BG_SHOW, top.optBoolean("App_Open_Ad_Show", false));
        editor.putBoolean(K_COMING_SOON, top.optBoolean("Coming_Soon", false));
        editor.apply();
    }

    /**
     * The Welcome screen replaces the App Open ad: switched on, its native ad enabled and an ad id given. With Ad_Priority
     * QUIZ no Google ad is requested, so a Welcome screen made to carry a Google native is not used then.
     */
    public static boolean isEnabled(@NonNull Context context) {
        SharedPreferences p = prefs(context);
        return p.getBoolean(K_SHOW, false)
                && p.getBoolean(K_NATIVE_SHOW, true)
                && !nativeId(context).isEmpty()
                && !AdPlacement.shouldUseQuizPriority();
    }

    /** Master switch for the Welcome screen and the App Open ad on return from the background: {@code App_Open_Ad_Show}. */
    public static boolean backgroundShow(@NonNull Context context) {
        return prefs(context).getBoolean(K_BG_SHOW, false);
    }

    /** Maintenance mode: nothing shows. */
    public static boolean comingSoon(@NonNull Context context) {
        return prefs(context).getBoolean(K_COMING_SOON, false);
    }

    public static int continueDelaySec(@NonNull Context context) {
        return Math.max(0, prefs(context).getInt(K_DELAY, 2));
    }

    @NonNull
    public static String nativeId(@NonNull Context context) {
        String id = prefs(context).getString(K_NATIVE_ID, "");
        return id == null ? "" : id.trim();
    }

    @NonNull
    public static String interId(@NonNull Context context) {
        String id = prefs(context).getString(K_INTER_ID, "");
        return id == null ? "" : id.trim();
    }

    /** Interstitial on Continue: only requested when switched on and an id is given. */
    public static boolean interEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(K_INTER_SHOW, false) && !interId(context).isEmpty();
    }

    @NonNull
    public static String debugState(@NonNull Context context) {
        SharedPreferences p = prefs(context);
        return "show=" + p.getBoolean(K_SHOW, false) + " nativeShow=" + p.getBoolean(K_NATIVE_SHOW, true)
                + " nativeId=" + !nativeId(context).isEmpty() + " enabled=" + isEnabled(context)
                + " bgShow=" + backgroundShow(context) + " comingSoon=" + comingSoon(context);
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        SharedPreferences p = prefs;
        if (p == null) {
            p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            prefs = p;
        }
        return p;
    }
}
