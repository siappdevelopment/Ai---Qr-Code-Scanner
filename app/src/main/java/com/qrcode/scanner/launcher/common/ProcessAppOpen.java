package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.qrcode.scanner.MainActivity;
import com.qrcode.scanner.launcher.activities.ChargingScreenActivity;
import com.qrcode.scanner.launcher.activities.ClEndActivity;
import com.qrcode.scanner.launcher.activities.CollectionActivity;
import com.qrcode.scanner.launcher.activities.DefaultActivity;
import com.qrcode.scanner.launcher.activities.DefaultHomeGuideActivity;
import com.qrcode.scanner.launcher.activities.DefaultSettingHomeActivity;
import com.qrcode.scanner.launcher.activities.EventPromptActivity;
import com.qrcode.scanner.launcher.activities.IntroActivity;
import com.qrcode.scanner.launcher.activities.LanguageActivity;
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity;
import com.qrcode.scanner.launcher.activities.OverlayPermissionGuideActivity;
import com.qrcode.scanner.launcher.activities.PermissionActivity;
import com.qrcode.scanner.launcher.activities.RecentActivity;
import com.qrcode.scanner.launcher.activities.WelcomeBackActivity;
import com.qrcode.scanner.launcher.adapters.LauncherPagerAdapter;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;
import com.qrcode.scanner.spapp.R;

/**
 * What happens when the user brings the app back to the foreground (the "Welcome back" screen / App Open ad decision of
 * the welcome-screen skill). Exactly one of three things happens:
 * <pre>
 * app returns to foreground (an activity starts while numStarted == 1)
 *  |- any gate fails                      -> nothing
 *  |- Welcome enabled AND this app is NOT the default Home app -> Welcome back screen
 *  |- this app IS the default Home app, daily limit not reached -> App Open ad (preloaded copy, else request for next time)
 * </pre>
 * Gates, in order (the first failing one ends it): activity not in the skip list; timed block ({@link #blockFor});
 * one-shot skip ({@link #skipNextAppOpen}) and one-shot block ({@link #blockNextAppOpen}); not Coming_Soon; the startup
 * (Splash) has handed over ({@code appStartingScreenOpen}); numStarted == 1; not {@code fastStart}; online;
 * App_Open_Show_In_Background. This app is a launcher: as before, the return only counts while it is the default Home app
 * and the QR (right) page / QR screens are in front - the other launcher pages have the unlock ad and must not show a
 * Welcome screen on every Home press.
 */
public final class ProcessAppOpen implements DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    private static final String TAG = "ProcessAppOpen";
    private static final long AD_EXPIRY_MS = 4L * 60L * 60L * 1000L;
    private static final long APP_OPEN_DIALOG_DELAY_MS = 1500L;
    private static final long DECISION_DELAY_MS = 100L;

    // ---- shared state (the "ADSAppManage" flags of the skill) ----
    /** Splash sets this after its own App Open / skip so the very next start is not treated as a return. */
    private static volatile boolean fastStart;
    /** True once the startup (Splash) has handed over to the app; until then a return shows nothing. */
    private static volatile boolean appStartingScreenOpen;
    private static volatile long blockedUntilMs;
    private static volatile boolean skipNextAppOpen;
    private static volatile boolean appOpenBlocked;
    private static boolean appInForeground;
    private static volatile long foregroundSinceMs;
    @Nullable
    private static ProcessAppOpen instance;

    private final Application application;
    private final Handler handler = new Handler(Looper.getMainLooper());
    @Nullable
    private Activity currentActivity;
    @Nullable
    private AppOpenAd appOpenAd;
    private boolean isLoadingAd;
    private boolean isShowingAd;
    private long loadTime;
    private int numStarted;
    /** Activities created but not yet started: their first start is the app starting, never a return. */
    private final java.util.WeakHashMap<Activity, Boolean> justCreated = new java.util.WeakHashMap<>();

    public ProcessAppOpen(@NonNull Application application) {
        this.application = application;
        instance = this;
    }

    public void register() {
        application.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    // ================================================================= flags other code sets

    /** The foreground App Open ad / Welcome screen must not start for a while (e.g. before another full-screen ad). */
    public static void blockFor(long millis) {
        blockedUntilMs = SystemClock.elapsedRealtime() + millis;
    }

    /** One-shot: the next return is not an app return (the app is about to come back from a system Settings screen). */
    public static void skipNextAppOpen() {
        skipNextAppOpen = true;
    }

    /** One-shot: the next return shows nothing (set before launching a full-screen ad / permission screen). */
    public static void blockNextAppOpen() {
        appOpenBlocked = true;
    }

    /** Splash finished: later returns may show. The very next start is not a return ({@code fastStart}). */
    public static void markStartupHandedOver() {
        appStartingScreenOpen = true;
        fastStart = true;
    }

    /** A new Splash flow began: nothing shows until it hands over again. */
    public static void markStartupBegan() {
        appStartingScreenOpen = false;
    }

    /** The launcher Home is up (default Home boots without the Splash): returns may show from now on. */
    public static void markHomeReached() {
        appStartingScreenOpen = true;
    }

    /** True when a loaded, still valid App Open ad is waiting. */
    public static boolean isAppOpenAdReady() {
        return instance != null && instance.isAdAvailable();
    }

    // ================================================================= preload

    /** Keeps one App Open ad ready. Never requested while the Welcome screen replaces it. */
    private void fetchAd() {
        if (isLoadingAd || isAdAvailable()) {
            return;
        }
        if (!AppUtils.isDefaultHomeApp(application)) {
            clearLoadedAd();
            return;
        }
        String unitId = AdPlacement.getAppOpenId();
        if (!AdPlacement.getAppOpenAdShow() || !AdPlacement.canShowAppOpenAd(application) || unitId == null || unitId.isEmpty()
                || !AdPlacement.canRequestAds(application) || !AdPlacement.isNetworkAvailable(application)) {
            return;
        }
        isLoadingAd = true;
        AppOpenAd.load(application, unitId, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                AnalyticsEvents.ad(application, AnalyticsEvents.APP_OPEN_AD_LOAD, AnalyticsEvents.FORMAT_APP_OPEN, unitId);
                AppUtils.trackScreen(application, "App_AppOpen_Preload");
                ad.setOnPaidEventListener(value -> AdPlacement.logAdRevenue(application, value));
                appOpenAd = ad;
                isLoadingAd = false;
                loadTime = System.currentTimeMillis();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                AnalyticsEvents.ad(application, AnalyticsEvents.APP_OPEN_AD_FAIL, AnalyticsEvents.FORMAT_APP_OPEN, unitId);
                AppUtils.trackScreen(application, "App_AppOpen_Preload_Fail");
                isLoadingAd = false;
            }
        });
    }

    private boolean isAdAvailable() {
        return appOpenAd != null && AdPlacement.getAppOpenAdShow() && AdPlacement.canShowAppOpenAd(application)
                && (System.currentTimeMillis() - loadTime) < AD_EXPIRY_MS;
    }

    private void clearLoadedAd() {
        appOpenAd = null;
        isLoadingAd = false;
    }

    // ================================================================= show

    private void showAdIfAvailable(@NonNull Activity activity) {
        if (isShowingAd) {
            return;
        }
        if (!AdPlacement.getAppOpenAdShow() || !AdPlacement.canShowAppOpenAd(application)) {
            clearLoadedAd();
            return;
        }
        if (!isAdAvailable()) {
            // Not preloaded: no waiting dialog, the request is for the next return.
            fetchAd();
            return;
        }
        final AppOpenAd adToShow = appOpenAd;
        adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                clearLoadedAd();
                isShowingAd = false;
                fetchAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                clearLoadedAd();
                isShowingAd = false;
                fetchAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Only a really shown ad counts for the daily limit.
                AdPlacement.incrementAppOpenAdCount(application);
                AdPlacement.setAppOpenLastShowTime(application, System.currentTimeMillis());
                AppUtils.trackScreen(application, "App_AppOpen_Show");
            }
        });
        // Last check right before showing: the launcher status may have changed since the ad was loaded.
        if (!AppUtils.isDefaultHomeApp(application) || activity.isFinishing() || activity.isDestroyed()) {
            clearLoadedAd();
            return;
        }
        isShowingAd = true;
        adToShow.show(activity);
    }

    private void showAppOpenDialog(Activity activity, Runnable onDismiss) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || !isAdAvailable()) {
            onDismiss.run();
            return;
        }
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
        }
        dialog.setContentView(R.layout.dialog_loading_ads);
        dialog.setCancelable(false);
        dialog.show();
        handler.postDelayed(() -> {
            if (dialog.isShowing() && !activity.isFinishing() && !activity.isDestroyed()) {
                dialog.dismiss();
            }
            onDismiss.run();
        }, APP_OPEN_DIALOG_DELAY_MS);
    }

    // ================================================================= the return decision

    /** Screens that must never trigger (or be covered by) a Welcome screen / App Open ad. */
    private static boolean isSkipListed(@NonNull Activity activity) {
        return activity instanceof LanguageActivity
                || activity instanceof IntroActivity
                || activity instanceof CollectionActivity
                || activity instanceof PermissionActivity
                || activity instanceof DefaultActivity
                || activity instanceof DefaultSettingHomeActivity
                || activity instanceof DefaultHomeGuideActivity
                || activity instanceof OverlayPermissionGuideActivity
                || activity instanceof ClEndActivity
                || activity instanceof EventPromptActivity
                || activity instanceof ChargingScreenActivity
                || activity instanceof RecentActivity
                || activity instanceof WelcomeBackActivity
                || activity.getClass().getName().startsWith("com.google.android.gms.ads");
    }

    /**
     * The QR (right) UI: the launcher's right page, or the same QR UI hosted by MainActivity and its QR activities. The
     * other launcher pages never get the Welcome screen / App Open ad (they have the unlock ad).
     */
    private static boolean isRightSidePageOpen(@Nullable Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        if (activity instanceof LauncherHomeActivity) {
            return ((LauncherHomeActivity) activity).getLauncherCurrentItem() == LauncherPagerAdapter.PAGE_RIGHT;
        }
        return activity instanceof MainActivity || activity.getClass().getName().startsWith("com.qrcode.scanner.ui.");
    }

    private void onReturnCandidate(@NonNull Activity activity) {
        if (isSkipListed(activity)) {
            return;
        }
        if (appStartingScreenOpen) {
            // Keeps one ad ready (once the Splash is done, on every start).
            fetchAd();
        }
        String name = activity.getClass().getSimpleName();
        if (SystemClock.elapsedRealtime() < blockedUntilMs) {
            Log.d(TAG, name + ": no return, a timed block is running");
            return;
        }
        if (skipNextAppOpen) {
            skipNextAppOpen = false;
            appOpenBlocked = false;
            Log.d(TAG, name + ": no return, one-shot skip (back from a system Settings screen)");
            return;
        }
        if (appOpenBlocked) {
            appOpenBlocked = false;
            Log.d(TAG, name + ": no return, one-shot block");
            return;
        }
        if (WelcomeBackConfig.comingSoon(application)) {
            Log.d(TAG, name + ": no return, Coming_Soon is true");
            return;
        }
        if (!appStartingScreenOpen) {
            Log.d(TAG, name + ": no return, the Splash has not handed over yet");
            return;
        }
        if (numStarted != 1) {
            return; // only the first activity of a return; opening a second screen inside the app never fires it
        }
        if (fastStart) {
            fastStart = false;
            Log.d(TAG, name + ": no return, fastStart (the start right after the Splash)");
            return;
        }
        if (!AdPlacement.isNetworkAvailable(application)) {
            Log.d(TAG, name + ": no return, offline");
            return;
        }
        if (!WelcomeBackConfig.backgroundShow(application)) {
            Log.d(TAG, name + ": no return, App_Open_Ad_Show is false"); // master switch of the Welcome screen and the App Open ad
            return;
        }
        // The Welcome screen is only for the app when it is NOT the default Home app; the default Home app keeps the App
        // Open ad (QR page in front, as before).
        final boolean isDefault = AppUtils.isDefaultHomeApp(application);
        final boolean welcome = !isDefault && WelcomeBackConfig.isEnabled(application);
        Log.d(TAG, name + ": app returned. default=" + isDefault + " " + WelcomeBackConfig.debugState(application)
                + " canShowAppOpen=" + AdPlacement.canShowAppOpenAd(application));
        if (!welcome && !isDefault) {
            Log.d(TAG, name + ": no return, not the default Home app and the Welcome screen is off");
            return;
        }
        if (!welcome && !AdPlacement.canShowAppOpenAd(application)) {
            Log.d(TAG, name + ": no App Open ad, App_Open_Show_Per_Day is 0 or today's limit is reached");
            return;
        }
        handler.postDelayed(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            if (welcome) {
                // Not the default Home app: the Welcome screen replaces the App Open ad (which is only for the default Home).
                if (AppUtils.isDefaultHomeApp(application)) {
                    Log.d(TAG, "Welcome ignored: this app became the default Home app");
                    return;
                }
                Log.d(TAG, "showing the Welcome back screen");
                WelcomeBackActivity.show(activity);
                return;
            }
            // Default Home app: the App Open ad, only with the QR page in front (the other launcher pages have the unlock ad).
            if (!AppUtils.isDefaultHomeApp(application) || !isRightSidePageOpen(activity)) {
                Log.d(TAG, "return ignored: not the default Home app or the QR page is not in front");
                return;
            }
            if (isAdAvailable()) {
                if (RemoteConfigValues.getAppOpenDialogShow()) {
                    showAppOpenDialog(activity, () -> showAdIfAvailable(activity));
                } else {
                    showAdIfAvailable(activity);
                }
            } else {
                fetchAd();
            }
        }, DECISION_DELAY_MS);
    }

    // ================================================================= lifecycle

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        appInForeground = true;
        foregroundSinceMs = SystemClock.elapsedRealtime();
        AnalyticsEvents.log(application, AnalyticsEvents.APP_OPEN);
        // The user is in the app: a stale "make this your default launcher" reminder is cleared.
        DefaultLauncherReminder.cancelIfDefault(application);
        DefaultLauncherReminder.dismissNotification(application);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        appInForeground = false;
        // The user left the app: have the ad ready for the return.
        fetchAd();
    }

    /** Milliseconds since the app last came to the foreground (a very large value if it never did). */
    public static long msSinceForeground() {
        return foregroundSinceMs == 0L ? Long.MAX_VALUE : SystemClock.elapsedRealtime() - foregroundSinceMs;
    }

    public static boolean isAppInForeground() {
        return appInForeground;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        // A freshly created activity is the app STARTING (cold start, new launch, a screen opened inside the app), not a
        // return from the background: a return restarts an activity that already exists (onStart without onCreate).
        justCreated.put(activity, Boolean.TRUE);
        if (activity instanceof MainActivity && com.qrcode.scanner.launcher.common.StartupFlow.shouldShowSplash(activity.getIntent())) {
            // The app was started from its icon: the Splash runs again, nothing may show before it hands over.
            markStartupBegan();
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!isShowingAd) {
            currentActivity = activity;
        }
        numStarted++;
        if (justCreated.remove(activity) != null) {
            if (appStartingScreenOpen && !isSkipListed(activity)) {
                fetchAd();
            }
            return;
        }
        onReturnCandidate(activity);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        numStarted = Math.max(0, numStarted - 1);
        if (numStarted == 0) {
            // The app really went to the background: the next start is a genuine return, never "the start after Splash".
            fastStart = false;
            fetchAd();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}
