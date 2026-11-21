package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Intro rules of the onboarding flow. Every key lives in the existing {@code Screen.IntroScreen} block
 * (saved on the phone):
 * <ul>
 *   <li>{@code Intro_Show_Every_Launch}: repeat count. 0 = first time only, 1 = every launch, N = every Nth launch
 *       (the first time ever is always shown). Decided once per app launch ({@link #prepareLaunch}); on an intro
 *       launch the Language screen is shown again together with the intro.</li>
 *   <li>{@code Intro_Native1_Show / _Id / _Auto_Sec}: full-screen native ad after intro page 1.</li>
 *   <li>{@code Intro_Native2_Show / _Id / _Auto_Sec}: full-screen native ad after intro page 2.</li>
 * </ul>
 * The order of the screens is NOT handled here: it stays in Show_Screen_Flow.
 */
public final class IntroConfig {
    private static final String PREFS = "intro_config_preferences";
    private static final String KEY_EVERY = "Intro_Show_Every_Launch";
    private static final String KEY_SHOWN_EVER = "intro_shown_ever";
    private static final String KEY_LAUNCHES_SINCE_SHOWN = "launches_since_shown";

    private static boolean evaluatedThisLaunch;
    private static boolean introLaunch;

    private IntroConfig() {
    }

    // ---------------------------------------------------------------- Remote Config

    /**
     * Reads the repeat count and the two full-screen native slots from the {@code IntroScreen} block.
     * Missing keys keep today's behaviour (first time only, no full-screen native).
     */
    public static void applyConfig(@Nullable Context context, @Nullable JSONObject introBlock) {
        if (context == null) {
            return;
        }
        JSONObject block = introBlock == null ? new JSONObject() : introBlock;
        SharedPreferences.Editor editor = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_EVERY, Math.max(0, block.optInt(KEY_EVERY, 0)));
        for (int slot = 1; slot <= 2; slot++) {
            editor.putBoolean(nativeKey(slot, "Show"), block.optBoolean(nativeKey(slot, "Show"), false));
            editor.putString(nativeKey(slot, "Id"), block.optString(nativeKey(slot, "Id"), "").trim());
            editor.putInt(nativeKey(slot, "Auto_Sec"), Math.max(0, block.optInt(nativeKey(slot, "Auto_Sec"), 0)));
        }
        editor.apply();
    }

    @NonNull
    private static String nativeKey(int slot, @NonNull String suffix) {
        return "Intro_Native" + slot + "_" + suffix;
    }

    /**
     * Full-screen native after intro page {@code slot} (1 or 2). Needs the switch on, an ad unit id, and an intro
     * page after it (slot below the number of intro pages).
     */
    public static boolean isNativeSlotEnabled(@Nullable Context context, int slot) {
        SharedPreferences prefs = prefs(context);
        return prefs != null && slot >= 1 && slot <= 2
                && slot < IntroNavigation.getEffectiveIntroButtonScreenCount()
                && prefs.getBoolean(nativeKey(slot, "Show"), false)
                && !prefs.getString(nativeKey(slot, "Id"), "").isEmpty();
    }

    @NonNull
    public static String getNativeId(@Nullable Context context, int slot) {
        SharedPreferences prefs = prefs(context);
        return prefs == null ? "" : prefs.getString(nativeKey(slot, "Id"), "");
    }

    /** Seconds after which the full-screen native closes by itself (0 = only the user closes it). */
    public static int getNativeAutoSec(@Nullable Context context, int slot) {
        SharedPreferences prefs = prefs(context);
        return prefs == null ? 0 : prefs.getInt(nativeKey(slot, "Auto_Sec"), 0);
    }

    // ---------------------------------------------------------------- repeat count (once per launch)

    /**
     * Call once at the start of every app launch, before the flow starts. Decides whether the intro is due this
     * launch. When it is due and onboarding was finished before, Language and Intro are made "not completed" so the
     * normal flow shows them again (all other steps stay completed and are skipped).
     */
    public static synchronized void prepareLaunch(@Nullable Context context) {
        if (evaluatedThisLaunch || context == null) {
            return;
        }
        evaluatedThisLaunch = true;
        SharedPreferences prefs = prefs(context);
        if (prefs == null) {
            return;
        }
        if (!prefs.getBoolean(KEY_SHOWN_EVER, false) && AppUtils.hasCompletedOnboarding(context)) {
            // Users who finished onboarding before this rule existed already saw the intro: do not repeat it once.
            prefs.edit().putBoolean(KEY_SHOWN_EVER, true).putInt(KEY_LAUNCHES_SINCE_SHOWN, 0).apply();
        }
        introLaunch = isIntroDueThisLaunch(prefs);
        if (introLaunch && AppUtils.hasCompletedOnboarding(context)) {
            AppUtils.setLanguageFlowCompleted(context, false);
            AppUtils.setIntroCompleted(context, false);
        }
    }

    private static boolean isIntroDueThisLaunch(@NonNull SharedPreferences prefs) {
        if (IntroNavigation.getEffectiveIntroButtonScreenCount() <= 0) {
            return false;
        }
        if (!prefs.getBoolean(KEY_SHOWN_EVER, false)) {
            return true;
        }
        int every = prefs.getInt(KEY_EVERY, 0);
        if (every <= 0) {
            return false;
        }
        int launches = prefs.getInt(KEY_LAUNCHES_SINCE_SHOWN, 0) + 1;
        if (launches >= every) {
            return true;
        }
        prefs.edit().putInt(KEY_LAUNCHES_SINCE_SHOWN, launches).apply();
        return false;
    }

    /** True for the whole launch once the intro was found due: Language is shown again together with it. */
    public static boolean isIntroLaunch() {
        return introLaunch;
    }

    /** Call when the user really finished the intro: the next count starts from here. */
    public static void markShown(@Nullable Context context) {
        SharedPreferences prefs = prefs(context);
        if (prefs != null) {
            prefs.edit().putBoolean(KEY_SHOWN_EVER, true).putInt(KEY_LAUNCHES_SINCE_SHOWN, 0).apply();
        }
    }

    @Nullable
    private static SharedPreferences prefs(@Nullable Context context) {
        return context == null ? null : context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
