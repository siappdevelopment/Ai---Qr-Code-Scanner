package com.qrcode.scanner.launcher.common;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Right-swipe (QR page) and left-swipe (sub page) ads of the launcher pager, and the Back ad from each page.
 * <ul>
 *   <li>One in-memory counter per side is shared by the swipe and the Back: {@code *_Swipe_Interstitial} = N shows the
 *       ad on every Nth of them (0 = every time). It resets after a show and after an app restart.</li>
 *   <li>A configured sequence ({@code *_Swipe_Ad_Sequence}) replaces the single interstitial; a cursor moves on only
 *       when a step runs. Google steps without a unit id are skipped.</li>
 *   <li>Preload ({@code *_Swipe_Ads_type = preload}): the Google ad of the step the next event will use is loaded
 *       ahead and shown without a loading dialog; otherwise it is loaded on demand.</li>
 *   <li>Quiz: Quiz_Inter / Quiz_Native / Quiz_App_Open / Quiz_Browser steps, the Quiz fallback after a failed Google ad
 *       ({@code *_Swipe_Failed_Show_Quiz}) and {@code Ad_Priority = QUIZ} (Quiz first, no preload).</li>
 *   <li>Right swipe only: the Default Home popup wins (the ad is skipped), and the ad waits while a permission a system
 *       dialog can still grant is missing. The Back ads and the whole left side have neither.</li>
 * </ul>
 */
public final class SwipeAds {
    public enum Side {
        RIGHT("right", "Right_Swipe"),
        LEFT("left", "Left_Swipe"),

        /** The App Open ad after the phone is unlocked ({@link UnlockAppOpenAds}). */
        UNLOCK("unlock", "Unlock_App_Open");

        final String prefix;
        final String key;

        Side(String prefix, String key) {
            this.prefix = prefix;
            this.key = key;
        }
    }

    /** The Default Home popup of the home fragment, as the right swipe needs it. */
    public interface PopupHost {
        boolean canShowPopup();

        void showPopup();
    }

    private static final String CONFIG_PREFS = "swipe_ads_config";
    private static final String SEQUENCE_PREFS = "swipe_sequence_ad_preferences";
    private static final String PERMISSION_PREFS = "swipe_permission_state";
    private static final long INTERSTITIAL_MAX_AGE_MS = 55L * 60L * 1000L;
    private static final long APP_OPEN_MAX_AGE_MS = 4L * 60L * 60L * 1000L;

    private static final String T_GOOGLE_INTER = "google_inter";
    private static final String T_GOOGLE_APP_OPEN = "google_app_open";
    private static final String T_GOOGLE_NATIVE = "google_native";
    private static final String T_QUIZ_INTER = "quiz_inter";
    private static final String T_QUIZ_APP_OPEN = "quiz_app_open";
    private static final String T_QUIZ_NATIVE = "quiz_native";

    private static final class Step {
        final String type;
        final int count;

        Step(String type, int count) {
            this.type = type;
            this.count = count;
        }
    }

    private static final class SideConfig {
        boolean show;
        boolean preload;
        int every;
        List<Step> steps = new ArrayList<>();
        String interId = "";
        String nativeId = "";
        boolean failedShowQuiz = true;
        /** Unlock only: Unlock_App_Open_Id (blank = the global App_Open_Id). */
        String appOpenId = "";
        /** Unlock only: Unlock_App_Open_Per_Day (gap = 24 h / N, 0 = off). */
        int perDay;
    }

    /** One preloaded Google ad of a side (only one of the three is ever set). */
    private static final class Cache {
        InterstitialAd inter;
        AppOpenAd appOpen;
        NativeAd nativeAd;
        long loadedAt;
        boolean loading;
        int token;
        String format = "";
    }

    private static final SideConfig RIGHT_CONFIG = new SideConfig();
    private static final SideConfig LEFT_CONFIG = new SideConfig();
    private static final Cache RIGHT_CACHE = new Cache();
    private static final Cache LEFT_CACHE = new Cache();
    private static final SideConfig UNLOCK_CONFIG = new SideConfig();
    private static final Cache UNLOCK_CACHE = new Cache();
    @Nullable
    private static Context appContext;
    private static boolean configLoaded;
    private static int rightEvents;
    private static int leftEvents;
    private static boolean backAdRunning;
    private static long backAdStartedAt;
    private static final long BACK_GUARD_MAX_MS = 30_000L;
    @Nullable
    private static WeakReference<Activity> host;

    private SwipeAds() {
    }

    // ================================================================= config

    /** Reads the swipe keys of the MainScreen block, saves them, and refreshes the preloads. */
    public static synchronized void applyConfig(@Nullable Context context, @Nullable JSONObject mainScreen) {
        JSONObject block = mainScreen == null ? new JSONObject() : mainScreen;
        readSide(Side.RIGHT, RIGHT_CONFIG, block);
        readSide(Side.LEFT, LEFT_CONFIG, block);
        android.util.Log.i("SwipeAds", "config: right show=" + RIGHT_CONFIG.show + " every=" + RIGHT_CONFIG.every + " preload=" + RIGHT_CONFIG.preload + " seq=" + stepsToJson(RIGHT_CONFIG.steps)
                + " | left show=" + LEFT_CONFIG.show + " every=" + LEFT_CONFIG.every + " preload=" + LEFT_CONFIG.preload + " seq=" + stepsToJson(LEFT_CONFIG.steps));
        if (!configLoaded && context != null) {
            // Keep the saved Unlock config until applyUnlockConfig runs right after this.
            loadSide(context.getApplicationContext().getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE), Side.UNLOCK, UNLOCK_CONFIG);
        }
        configLoaded = true;
        if (context != null) {
            appContext = context.getApplicationContext();
            SharedPreferences.Editor editor = context.getApplicationContext().getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE).edit();
            saveSide(editor, Side.RIGHT, RIGHT_CONFIG);
            saveSide(editor, Side.LEFT, LEFT_CONFIG);
            editor.apply();
        }
    }

    /**
     * Unlock App Open keys. Each key is read from the Screen.unlock_screen block, and from the profile root when the block
     * does not have it. Missing keys mean the feature is off.
     */
    public static synchronized void applyUnlockConfig(@Nullable Context context, @Nullable JSONObject root, @Nullable JSONObject launcherAppScreen) {
        JSONObject block = launcherAppScreen == null ? new JSONObject() : launcherAppScreen;
        JSONObject top = root == null ? new JSONObject() : root;
        SideConfig config = UNLOCK_CONFIG;
        config.show = pick(block, top, "Unlock_App_Open_Show").optBoolean("Unlock_App_Open_Show", false);
        config.appOpenId = pick(block, top, "Unlock_App_Open_Id").optString("Unlock_App_Open_Id", "").trim();
        config.perDay = Math.max(0, pick(block, top, "Unlock_App_Open_Per_Day").optInt("Unlock_App_Open_Per_Day", 0));
        // preload is the default; only an explicit "load" requests the ad at unlock time.
        config.preload = !"load".equalsIgnoreCase(pick(block, top, "Unlock_App_Open_Ads_type").optString("Unlock_App_Open_Ads_type", "preload").trim());
        config.steps = unlockStepsOnly(parseSteps(pick(block, top, "Unlock_App_Open_Ad_Sequence").opt("Unlock_App_Open_Ad_Sequence")));
        config.interId = pick(block, top, "Unlock_App_Open_Interstitial_Id").optString("Unlock_App_Open_Interstitial_Id", "").trim();
        config.nativeId = pick(block, top, "Unlock_App_Open_Native_Id").optString("Unlock_App_Open_Native_Id", "").trim();
        // A failed Google ad is NEVER replaced by a Quiz ad on the unlock side (Unlock_App_Open_Failed_Show_Quiz is ignored);
        // Quiz appears only as an explicit Quiz_Browser step.
        config.failedShowQuiz = false;
        config.every = 0;
        configLoaded = true;
        if (context != null) {
            appContext = context.getApplicationContext();
            SharedPreferences.Editor editor = appContext.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE).edit();
            saveSide(editor, Side.UNLOCK, UNLOCK_CONFIG);
            editor.apply();
        }
    }

    /** Unlock sequences know only Google App Open / Inter / Native and Quiz_Browser; Quiz inter / native / app open are dropped. */
    @NonNull
    private static List<Step> unlockStepsOnly(@NonNull List<Step> steps) {
        List<Step> kept = new ArrayList<>();
        for (Step step : steps) {
            if (T_GOOGLE_INTER.equals(step.type) || T_GOOGLE_APP_OPEN.equals(step.type) || T_GOOGLE_NATIVE.equals(step.type)
                    || "quiz_browser".equals(step.type)) {
                kept.add(step);
            }
        }
        return kept;
    }

    @NonNull
    private static JSONObject pick(@NonNull JSONObject block, @NonNull JSONObject root, @NonNull String key) {
        return block.has(key) ? block : root;
    }

    private static void readSide(Side side, SideConfig config, JSONObject block) {
        config.show = block.optBoolean(side.key + "_Interstitial_Ad_Show", false);
        config.preload = "preload".equalsIgnoreCase(block.optString(side.key + "_Ads_type", "load").trim());
        config.every = Math.max(0, block.optInt(side.key + "_Interstitial", 0));
        config.steps = parseSteps(block.opt(side.key + "_Ad_Sequence"));
        config.interId = block.optString(side.key + "_Interstitial_Id", "").trim();
        config.nativeId = block.optString(side.key + "_Native_Id", "").trim();
        config.failedShowQuiz = block.optBoolean(side.key + "_Failed_Show_Quiz", true);
    }

    private static void saveSide(SharedPreferences.Editor editor, Side side, SideConfig config) {
        editor.putBoolean(side.prefix + "_show", config.show);
        editor.putBoolean(side.prefix + "_preload", config.preload);
        editor.putInt(side.prefix + "_every", config.every);
        editor.putString(side.prefix + "_sequence", stepsToJson(config.steps));
        editor.putString(side.prefix + "_inter_id", config.interId);
        editor.putString(side.prefix + "_native_id", config.nativeId);
        editor.putBoolean(side.prefix + "_failed_quiz", config.failedShowQuiz);
        editor.putString(side.prefix + "_app_open_id", config.appOpenId);
        editor.putInt(side.prefix + "_per_day", config.perDay);
    }

    private static synchronized void ensureLoaded(@Nullable Context context) {
        if (configLoaded || context == null) {
            return;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE);
        loadSide(prefs, Side.RIGHT, RIGHT_CONFIG);
        loadSide(prefs, Side.LEFT, LEFT_CONFIG);
        loadSide(prefs, Side.UNLOCK, UNLOCK_CONFIG);
        appContext = context.getApplicationContext();
        configLoaded = true;
    }

    private static void loadSide(SharedPreferences prefs, Side side, SideConfig config) {
        config.show = prefs.getBoolean(side.prefix + "_show", false);
        config.preload = prefs.getBoolean(side.prefix + "_preload", false);
        config.every = prefs.getInt(side.prefix + "_every", 0);
        config.steps = parseSteps(prefs.getString(side.prefix + "_sequence", "[]"));
        config.interId = prefs.getString(side.prefix + "_inter_id", "");
        config.nativeId = prefs.getString(side.prefix + "_native_id", "");
        config.failedShowQuiz = prefs.getBoolean(side.prefix + "_failed_quiz", true);
        config.appOpenId = prefs.getString(side.prefix + "_app_open_id", "");
        config.perDay = prefs.getInt(side.prefix + "_per_day", 0);
    }

    @NonNull
    private static List<Step> parseSteps(@Nullable Object raw) {
        List<Step> steps = new ArrayList<>();
        try {
            JSONArray array;
            if (raw instanceof JSONArray) {
                array = (JSONArray) raw;
            } else if (raw instanceof String && ((String) raw).trim().startsWith("[")) {
                array = new JSONArray((String) raw);
            } else if (raw instanceof String && !((String) raw).trim().isEmpty()) {
                array = new JSONArray().put(((String) raw).trim());
            } else {
                return steps;
            }
            for (int i = 0; i < array.length(); i++) {
                Object item = array.opt(i);
                String rawType;
                int count = 1;
                if (item instanceof JSONArray) {
                    JSONArray entry = (JSONArray) item;
                    rawType = entry.optString(0, "");
                    // A missing count means 1.
                    count = entry.length() > 1 ? entry.optInt(1, 1) : 1;
                } else if (item instanceof String) {
                    // "Quiz_Browser", "Quiz_Browser,2" or "Quiz_Browser:2"
                    String[] parts = ((String) item).trim().split("[,:]");
                    rawType = parts[0];
                    if (parts.length > 1) {
                        try {
                            count = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException ignored) {
                            count = 1;
                        }
                    }
                } else {
                    continue;
                }
                String type = AdPlacement.normalizeSequenceAdType(rawType);
                if (type.isEmpty()) {
                    android.util.Log.w("SwipeAds", "unknown ad type in sequence: " + rawType);
                } else if (count > 0) {
                    steps.add(new Step(type, count));
                }
            }
        } catch (Exception ignored) {
        }
        return steps;
    }

    @NonNull
    private static String stepsToJson(@NonNull List<Step> steps) {
        JSONArray array = new JSONArray();
        for (Step step : steps) {
            JSONArray entry = new JSONArray();
            entry.put(step.type);
            entry.put(step.count);
            array.put(entry);
        }
        return array.toString();
    }

    private static SideConfig config(Side side) {
        return side == Side.RIGHT ? RIGHT_CONFIG : side == Side.LEFT ? LEFT_CONFIG : UNLOCK_CONFIG;
    }

    private static Cache cache(Side side) {
        return side == Side.RIGHT ? RIGHT_CACHE : side == Side.LEFT ? LEFT_CACHE : UNLOCK_CACHE;
    }

    /** Single-ad unit id = the shared interstitial id; a sequence's inter step uses its own id, else the shared one. */
    @NonNull
    private static String interId(SideConfig config) {
        String id = config.interId.isEmpty() ? RemoteConfigValues.getInterAdsId() : config.interId;
        return id == null ? "" : id.trim();
    }

    // ================================================================= events

    /**
     * The pager settled on the QR page coming from Home. The Default Home popup wins (no ad, the event still counts);
     * then a missing permission holds the ad back (the event still counts); otherwise the swipe ad runs and the popup
     * is checked again when it closes.
     */
    public static void onRightSwipe(@NonNull Activity activity, @Nullable PopupHost popup) {
        remember(activity);
        ensureLoaded(activity);
        SideConfig config = RIGHT_CONFIG;
        if (popup != null && popup.canShowPopup()) {
            popup.showPopup();
            if (config.show) {
                countEvent(Side.RIGHT);
            }
            return;
        }
        if (pendingPermissions(activity)) {
            if (config.show) {
                countEvent(Side.RIGHT);
            }
            return;
        }
        showAd(activity, Side.RIGHT, () -> {
            if (popup != null && !activity.isFinishing() && !activity.isDestroyed() && popup.canShowPopup()) {
                popup.showPopup();
            }
        });
    }

    /** The pager settled on the left page coming from Home. No popup, no permission gate. */
    public static void onLeftSwipe(@NonNull Activity activity) {
        remember(activity);
        ensureLoaded(activity);
        showAd(activity, Side.LEFT, () -> {
        });
    }

    /**
     * Back from an edge page to Home: the same ad management as the swipe, without popup and permission gate.
     * {@code next} runs once, with or without an ad; a second Back while an ad is open is ignored.
     */
    public static void onBack(@NonNull Activity activity, @NonNull Side side, @NonNull Runnable next) {
        long now = android.os.SystemClock.elapsedRealtime();
        // A Back while an ad is open is ignored; the guard frees itself if an ad never reports back.
        if (backAdRunning && now - backAdStartedAt < BACK_GUARD_MAX_MS) {
            return;
        }
        remember(activity);
        ensureLoaded(activity);
        backAdRunning = true;
        backAdStartedAt = now;
        showAd(activity, side, () -> {
            backAdRunning = false;
            next.run();
        });
    }

    // ================================================================= unlock App Open (used by UnlockAppOpenAds)

    /** Loads the saved unlock config (cold start). Safe to call many times. */
    public static void initUnlock(@Nullable Context context) {
        ensureLoaded(context);
    }

    public static boolean isUnlockShow() {
        return UNLOCK_CONFIG.show;
    }

    /** Unlock_App_Open_Per_Day: the gap between two unlock ads is 24 h / N; 0 = off. */
    public static int getUnlockPerDay() {
        return UNLOCK_CONFIG.perDay;
    }

    public static boolean unlockHasSequence() {
        return !UNLOCK_CONFIG.steps.isEmpty();
    }

    /** Unit id of the single unlock App Open ad: Unlock_App_Open_Id, else App_Open_Id. */
    @NonNull
    public static String getUnlockAppOpenId() {
        return appOpenIdOf(UNLOCK_CONFIG);
    }

    /** At lock time: loads the ad the unlock will show (Unlock_App_Open_Ads_type = preload), or drops the cache. */
    public static void preloadUnlock(@Nullable Context context) {
        ensureLoaded(context);
        preloadNext(Side.UNLOCK);
    }

    public static void clearUnlock() {
        clear(Side.UNLOCK);
    }

    /**
     * Shows the unlock ad: the next step of the sequence, or the single App Open ad (preloaded when ready, otherwise
     * one request without a loading dialog; a failed Google ad falls back to Quiz when Unlock_App_Open_Failed_Show_Quiz).
     * {@code done} runs exactly once, with or without an ad.
     */
    public static void showUnlock(@NonNull Activity activity, @NonNull Runnable done) {
        ensureLoaded(activity);
        final AtomicBoolean ran = new AtomicBoolean(false);
        final Runnable complete = () -> {
            if (ran.compareAndSet(false, true)) {
                done.run();
            }
        };
        if (activity.isFinishing() || activity.isDestroyed()) {
            complete.run();
            return;
        }
        final SideConfig config = UNLOCK_CONFIG;
        // The step at the cursor (empty sequence = one Google App Open). The cursor moves on for every attempt.
        final String type = config.steps.isEmpty()
                ? (appOpenIdOf(config).isEmpty() ? null : T_GOOGLE_APP_OPEN)
                : nextType(activity, Side.UNLOCK, config, true);
        android.util.Log.i("SwipeAds", "UNLOCK step -> " + type + " (preload=" + config.preload + ")");
        if (type == null) {
            complete.run();
            return;
        }
        final String format = formatOf(type);
        if (format == null) {
            // Quiz_Browser: the in-app web view, only because the sequence names it.
            clear(Side.UNLOCK);
            QuizAds.openBrowserThenContinue(activity, complete);
            return;
        }
        if (config.preload) {
            // Only an ad that is already loaded is shown. Not loaded / late: the step ends at once (no loading dialog, no
            // shimmer, no new request); the slot is already spent and the cursor has moved on.
            if (hasFresh(UNLOCK_CACHE, format)) {
                showCached(activity, Side.UNLOCK, format, complete);
            } else {
                clear(Side.UNLOCK);
                complete.run();
            }
            return;
        }
        // Load type: the ad is requested now with its normal loading UI; no Quiz fallback.
        clear(Side.UNLOCK);
        if (!AdPlacement.isNetworkAvailable(activity)) {
            complete.run();
        } else if (T_GOOGLE_APP_OPEN.equals(type)) {
            AdPlacement.loadAppOpenNoDialog(activity, appOpenIdOf(config), false, complete);
        } else {
            AdPlacement.executeSwipeSequenceAd(activity, complete, type, interId(config), config.nativeId, false);
        }
    }

    /** True when at least one step of the unlock sequence (or the single App Open) has a usable unit id. */
    public static boolean unlockHasUsableStep() {
        if (UNLOCK_CONFIG.steps.isEmpty()) {
            return !appOpenIdOf(UNLOCK_CONFIG).isEmpty();
        }
        for (Step step : UNLOCK_CONFIG.steps) {
            if (unitIdOf(step.type, UNLOCK_CONFIG) != null) {
                return true;
            }
        }
        return false;
    }

    private static void remember(@NonNull Activity activity) {
        host = new WeakReference<>(activity);
    }

    /** Counts one swipe / Back; true when the ad is due on it. */
    private static boolean countEvent(Side side) {
        int every = config(side).every;
        if (every <= 0) {
            return true;
        }
        int count = (side == Side.RIGHT ? ++rightEvents : ++leftEvents);
        if (count < every) {
            return false;
        }
        if (side == Side.RIGHT) {
            rightEvents = 0;
        } else {
            leftEvents = 0;
        }
        return true;
    }

    // ================================================================= show

    private static void showAd(@NonNull Activity activity, @NonNull Side side, @NonNull Runnable onComplete) {
        final SideConfig config = config(side);
        final AtomicBoolean done = new AtomicBoolean(false);
        final Runnable complete = () -> {
            if (done.compareAndSet(false, true)) {
                onComplete.run();
            }
        };
        if (!config.show || activity.isFinishing() || activity.isDestroyed()) {
            complete.run();
            return;
        }
        if (!countEvent(side)) {
            complete.run();
            return;
        }
        if (!AdGates.adsLocationAllowed(activity)) {
            complete.run();
            return;
        }
        if (!config.steps.isEmpty()) {
            showSequenceStep(activity, side, complete);
            return;
        }
        // Single interstitial: preloaded when due, otherwise on demand.
        if (isPreloadType(config)) {
            Cache cache = cache(side);
            if (hasFresh(cache, "inter")) {
                InterstitialAd ad = cache.inter;
                clear(side);
                showInterstitial(activity, ad, () -> {
                    preloadNext(side);
                    complete.run();
                }, () -> loadNow(activity, side, complete));
                return;
            }
        }
        loadNow(activity, side, complete);
    }

    /** On-demand load with the loading dialog; Quiz priority and the Quiz fallback are honoured. */
    private static void loadNow(@NonNull Activity activity, @NonNull Side side, @NonNull Runnable complete) {
        SideConfig config = config(side);
        AdPlacement.loadSwipeInterstitial(activity, interId(config), config.failedShowQuiz, () -> {
            if (isPreloadType(config)) {
                preloadNext(side);
            }
            complete.run();
        });
    }

    private static void showSequenceStep(@NonNull Activity activity, @NonNull Side side, @NonNull Runnable complete) {
        SideConfig config = config(side);
        String type = nextType(activity, side, config, true);
        android.util.Log.i("SwipeAds", side + " sequence step -> " + type + " (quizPriority=" + AdPlacement.shouldUseQuizPriority() + ")");
        if (type == null) {
            complete.run();
            return;
        }
        final Runnable next = () -> {
            complete.run();
            preloadNext(side);
        };
        String format = formatOf(type);
        Cache cache = cache(side);
        if (config.preload && format != null && hasFresh(cache, format)) {
            showCached(activity, side, format, next);
            return;
        }
        clear(side);
        // Ad_Priority QUIZ: Quiz first, the Google step becomes its Quiz counterpart.
        String run = AdPlacement.shouldUseQuizPriority() ? quizCounterpart(type) : type;
        AdPlacement.executeSwipeSequenceAd(activity, next, run, interId(config), config.nativeId, config.failedShowQuiz);
    }

    private static void showCached(@NonNull Activity activity, @NonNull Side side, @NonNull String format, @NonNull Runnable next) {
        Cache cache = cache(side);
        if ("inter".equals(format)) {
            InterstitialAd ad = cache.inter;
            clear(side);
            showInterstitial(activity, ad, next, next);
        } else if ("appopen".equals(format)) {
            AppOpenAd ad = cache.appOpen;
            clear(side);
            showAppOpen(activity, ad, next);
        } else {
            NativeAd ad = cache.nativeAd;
            // Taken out of the cache first: clear() destroys the cached native, and a destroyed ad shows blank media.
            cache.nativeAd = null;
            clear(side);
            // When the dialog cannot be shown, presentFullNativeAd runs `next` itself.
            AdPlacement.presentFullNativeAd(activity, ad, next);
        }
    }

    private static void showInterstitial(@NonNull Activity activity, @NonNull InterstitialAd ad, @NonNull Runnable onDismissed, @NonNull Runnable onShowFailed) {
        final AtomicBoolean finished = new AtomicBoolean(false);
        ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (finished.compareAndSet(false, true)) {
                    onDismissed.run();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                if (finished.compareAndSet(false, true)) {
                    onShowFailed.run();
                }
            }
        });
        activity.getWindow().getDecorView().post(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                if (finished.compareAndSet(false, true)) {
                    onDismissed.run();
                }
                return;
            }
            try {
                ad.show(activity);
            } catch (Exception e) {
                if (finished.compareAndSet(false, true)) {
                    onShowFailed.run();
                }
            }
        });
    }

    private static void showAppOpen(@NonNull Activity activity, @NonNull AppOpenAd ad, @NonNull Runnable next) {
        final AtomicBoolean finished = new AtomicBoolean(false);
        ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (finished.compareAndSet(false, true)) {
                    next.run();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                if (finished.compareAndSet(false, true)) {
                    next.run();
                }
            }
        });
        activity.getWindow().getDecorView().post(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                if (finished.compareAndSet(false, true)) {
                    next.run();
                }
                return;
            }
            try {
                ad.show(activity);
            } catch (Exception e) {
                if (finished.compareAndSet(false, true)) {
                    next.run();
                }
            }
        });
    }

    // ================================================================= sequence cursor

    /** The step at the saved cursor, skipping Google steps without a unit id. The cursor restarts when the sequence changes. */
    @Nullable
    private static String nextType(@NonNull Context context, @NonNull Side side, @NonNull SideConfig config, boolean advance) {
        List<Step> steps = new ArrayList<>();
        long total = 0;
        for (Step step : config.steps) {
            if (step.count > 0 && unitIdOf(step.type, config) != null) {
                steps.add(step);
                total += step.count;
            }
        }
        if (total <= 0L) {
            return null;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(SEQUENCE_PREFS, Context.MODE_PRIVATE);
        StringBuilder signature = new StringBuilder();
        for (Step step : config.steps) {
            signature.append(step.type).append(',').append(step.count).append(';');
        }
        String cursorKey = side.prefix + "_sequence_cursor";
        String signatureKey = side.prefix + "_sequence_signature";
        long cursor = Math.max(0L, prefs.getLong(cursorKey, 0L));
        if (!signature.toString().equals(prefs.getString(signatureKey, null))) {
            cursor = 0L;
            prefs.edit().putString(signatureKey, signature.toString()).putLong(cursorKey, 0L).apply();
        }
        if (advance) {
            prefs.edit().putLong(cursorKey, cursor + 1L).apply();
        }
        long index = cursor % total;
        long walked = 0L;
        for (Step step : steps) {
            walked += step.count;
            if (index < walked) {
                return step.type;
            }
        }
        return null;
    }

    /** Unit id a step needs: "" for Quiz steps (no Google id), null when a Google step has no id. */
    @Nullable
    private static String unitIdOf(@NonNull String type, @NonNull SideConfig config) {
        String id;
        switch (type) {
            case T_GOOGLE_INTER:
                id = interId(config);
                break;
            case T_GOOGLE_NATIVE:
                id = config.nativeId;
                break;
            case T_GOOGLE_APP_OPEN:
                id = appOpenIdOf(config);
                break;
            default:
                return "";
        }
        id = id == null ? "" : id.trim();
        return id.isEmpty() ? null : id;
    }

    @Nullable
    private static String formatOf(@NonNull String type) {
        switch (type) {
            case T_GOOGLE_INTER:
                return "inter";
            case T_GOOGLE_APP_OPEN:
                return "appopen";
            case T_GOOGLE_NATIVE:
                return "native";
            default:
                return null;
        }
    }

    @NonNull
    private static String quizCounterpart(@NonNull String type) {
        switch (type) {
            case T_GOOGLE_INTER:
                return T_QUIZ_INTER;
            case T_GOOGLE_APP_OPEN:
                return T_QUIZ_APP_OPEN;
            case T_GOOGLE_NATIVE:
                return T_QUIZ_NATIVE;
            default:
                return type;
        }
    }

    // ================================================================= preload

    private static boolean isPreloadType(SideConfig config) {
        return config.show && config.preload && !AdPlacement.shouldUseQuizPriority();
    }

    /** Refreshes both sides: on resume and after every new Remote Config. Pass null to reuse the last activity. */
    public static void preparePreload(@Nullable Activity activity) {
        if (activity != null) {
            remember(activity);
        }
        ensureLoaded(activity != null ? activity : currentHost());
        preloadNext(Side.RIGHT);
        preloadNext(Side.LEFT);
    }

    @Nullable
    private static Activity currentHost() {
        Activity activity = host == null ? null : host.get();
        return activity == null || activity.isFinishing() || activity.isDestroyed() ? null : activity;
    }

    /** App Open unit id of a side: Unlock_App_Open_Id when set (unlock side), else the global App_Open_Id. */
    @NonNull
    private static String appOpenIdOf(@NonNull SideConfig config) {
        String id = config.appOpenId.isEmpty() ? AdPlacement.getAppOpenId() : config.appOpenId;
        return id == null ? "" : id.trim();
    }

    /**
     * Loads the Google ad of the step the next event of {@code side} will use (the cursor does not move), or the single
     * ad (interstitial; App Open on the unlock side). Clears the cache when nothing should be preloaded. Needs only the
     * application context, so it also works while the app is in the background (the unlock preloads at lock time).
     */
    private static void preloadNext(@NonNull Side side) {
        final Activity activity = currentHost();
        final Context context = activity != null ? activity : appContext;
        final SideConfig config = config(side);
        if (context == null || !isPreloadType(config) || !AdPlacement.isNetworkAvailable(context)
                || !AdPlacement.canRequestAds(context) || !AdGates.adsLocationAllowed(context)) {
            clear(side);
            return;
        }
        String format;
        String unitId;
        if (config.steps.isEmpty()) {
            if (side == Side.UNLOCK) {
                format = "appopen";
                unitId = appOpenIdOf(config);
            } else {
                format = "inter";
                unitId = interId(config);
            }
        } else {
            String type = nextType(context, side, config, false);
            format = type == null ? null : formatOf(type);
            unitId = type == null ? "" : unitIdOf(type, config);
        }
        if (format == null || unitId == null || unitId.isEmpty()) {
            clear(side);
            return;
        }
        Cache cache = cache(side);
        if (hasFresh(cache, format)) {
            return;
        }
        if (cache.loading && format.equals(cache.format)) {
            return;
        }
        clear(side);
        load(context, side, format, unitId);
    }

    private static boolean hasFresh(@NonNull Cache cache, @NonNull String format) {
        if (!format.equals(cache.format)) {
            return false;
        }
        boolean present = cache.inter != null || cache.appOpen != null || cache.nativeAd != null;
        long maxAge = "appopen".equals(format) ? APP_OPEN_MAX_AGE_MS : INTERSTITIAL_MAX_AGE_MS;
        return present && System.currentTimeMillis() - cache.loadedAt < maxAge;
    }

    private static void clear(@NonNull Side side) {
        Cache cache = cache(side);
        cache.token++;
        cache.loading = false;
        cache.inter = null;
        cache.appOpen = null;
        if (cache.nativeAd != null) {
            cache.nativeAd.destroy();
            cache.nativeAd = null;
        }
        cache.format = "";
    }

    private static void load(@NonNull Context context, @NonNull Side side, @NonNull String format, @NonNull String unitId) {
        final Cache cache = cache(side);
        final int token = ++cache.token;
        cache.loading = true;
        cache.format = format;
        final Context app = context.getApplicationContext();
        if ("inter".equals(format)) {
            AnalyticsEvents.ad(app, AnalyticsEvents.INTERSTITIAL_PRELOAD, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
            InterstitialAd.load(app, unitId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    AnalyticsEvents.ad(app, AnalyticsEvents.INTERSTITIAL_LOAD, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
                    if (token != cache.token) {
                        return;
                    }
                    cache.loading = false;
                    cache.inter = ad;
                    cache.loadedAt = System.currentTimeMillis();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    AnalyticsEvents.ad(app, AnalyticsEvents.INTERSTITIAL_FAIL, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
                    failed(cache, token);
                }
            });
        } else if ("appopen".equals(format)) {
            AppOpenAd.load(app, unitId, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    AnalyticsEvents.ad(app, AnalyticsEvents.APP_OPEN_AD_LOAD, AnalyticsEvents.FORMAT_APP_OPEN, unitId);
                    if (token != cache.token) {
                        return;
                    }
                    cache.loading = false;
                    cache.appOpen = ad;
                    cache.loadedAt = System.currentTimeMillis();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    AnalyticsEvents.ad(app, AnalyticsEvents.APP_OPEN_AD_FAIL, AnalyticsEvents.FORMAT_APP_OPEN, unitId);
                    failed(cache, token);
                }
            });
        } else {
            AdPlacement.preloadFullNativeAd(app, unitId, nativeAd -> {
                if (token != cache.token) {
                    nativeAd.destroy();
                    return;
                }
                cache.loading = false;
                cache.nativeAd = nativeAd;
                cache.loadedAt = System.currentTimeMillis();
            }, () -> failed(cache, token));
        }
    }

    /** A failed preload is not retried by itself; the next resume / event / new config tries again. */
    private static void failed(@NonNull Cache cache, int token) {
        if (token == cache.token) {
            cache.loading = false;
            cache.format = "";
        }
    }

    // ================================================================= permission gate (right swipe only)

    /**
     * True while a permission that a system dialog can still grant is missing (camera, notifications on Android 13+,
     * phone state). A permission that was already seen missing and whose dialog is now blocked (Android no longer shows
     * it, "don't ask again") does not count, so a refused permission can never hold the swipe ad back forever.
     * Display-over-other-apps is only grantable in Settings and is never counted.
     */
    static boolean pendingPermissions(@NonNull Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE);
        Set<String> seen = new HashSet<>(prefs.getStringSet("seen_missing", new HashSet<>()));
        boolean pending = false;
        boolean changed = false;
        for (String permission : permissionsForAds()) {
            if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
                changed |= seen.remove(permission);
                continue;
            }
            boolean blockedForGood = seen.contains(permission)
                    && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
            if (blockedForGood) {
                continue;
            }
            pending = true;
            changed |= seen.add(permission);
        }
        if (changed) {
            prefs.edit().putStringSet("seen_missing", seen).apply();
        }
        return pending;
    }

    private static List<String> permissionsForAds() {
        List<String> list = new ArrayList<>();
        list.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        list.add(Manifest.permission.READ_PHONE_STATE);
        return list;
    }
}
