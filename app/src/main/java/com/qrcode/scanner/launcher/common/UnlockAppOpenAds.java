package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.qrcode.scanner.launcher.activities.LauncherHomeActivity;
import com.qrcode.scanner.launcher.adapters.LauncherPagerAdapter;

import java.lang.ref.WeakReference;

/**
 * The App Open ad after the phone is unlocked, on the launcher Home.
 * <p>
 * It runs only while this app is the device Home app. Locking the phone (SCREEN_OFF) arms it and, when every gate
 * passes, loads the ad ahead (only while the internet is on). The unlock (USER_PRESENT, or the keyguard going away on
 * phones that never send it) then shows it as soon as the launcher Home is in front. The QR page is left to
 * {@link ProcessAppOpen}, so one unlock never shows two App Open ads.
 * <p>
 * Remote Config (unlock_screen block, profile root as fallback): Unlock_App_Open_Show, Unlock_App_Open_Id,
 * Unlock_App_Open_Per_Day, Unlock_App_Open_Ads_type, Unlock_App_Open_Ad_Sequence, Unlock_App_Open_Interstitial_Id,
 * Unlock_App_Open_Native_Id, Unlock_App_Open_Failed_Show_Quiz. Where it may show is only the common
 * Ads_Show_countryList / Ads_show_List_Exclude rule.
 */
public final class UnlockAppOpenAds {
    private static final String TAG = "UnlockAppOpen";
    private static final String PREFS = "unlock_app_open_preferences";
    private static final String KEY_LAST_SHOW = "last_show_time";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long UNLOCK_WINDOW_MS = 6_000L;
    private static final long KEYGUARD_POLL_MS = 300L;
    private static final long KEYGUARD_WATCH_MS = 60_000L;
    private static final long STALE_RUNNING_MS = 60_000L;

    private static final UnlockAppOpenAds INSTANCE = new UnlockAppOpenAds();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Context context;
    private boolean started;
    private boolean armed;
    private boolean showing;
    private long runningSince;
    private long unlockedAtMs;
    private long screenOnAtMs;
    @Nullable
    private WeakReference<LauncherHomeActivity> home;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent == null ? null : intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case Intent.ACTION_SCREEN_OFF:
                    onLocked();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    watchKeyguard();
                    break;
                case Intent.ACTION_USER_PRESENT:
                    onUnlocked();
                    break;
                default:
                    break;
            }
        }
    };

    /** While the screen is on, waits for the keyguard to go away (not every phone sends USER_PRESENT). */
    private final Runnable keyguardPoll = new Runnable() {
        @Override
        public void run() {
            if (!armed || unlockedAtMs != 0L) {
                return;
            }
            if (!isKeyguardLocked()) {
                Log.d(TAG, "the keyguard is gone");
                onUnlocked();
            } else if (SystemClock.elapsedRealtime() - screenOnAtMs < KEYGUARD_WATCH_MS) {
                handler.postDelayed(this, KEYGUARD_POLL_MS);
            }
        }
    };

    private UnlockAppOpenAds() {
    }

    @NonNull
    public static UnlockAppOpenAds get() {
        return INSTANCE;
    }

    /** Registers for lock / unlock once; call from Application.onCreate. */
    public void start(@Nullable Application application) {
        if (started || application == null) {
            return;
        }
        started = true;
        context = application;
        SwipeAds.initUnlock(application);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        ContextCompat.registerReceiver(application, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    /** The launcher Home is in front (call from its onResume): an unlock that just happened shows the ad now. */
    public void onHomeResumed(@NonNull LauncherHomeActivity activity) {
        if (!started) {
            return;
        }
        home = new WeakReference<>(activity);
        // Some phones never send USER_PRESENT (no lock screen, face / Smart Lock, battery savers): Home coming back after
        // a lock while the keyguard is gone is the unlock as well.
        if (armed && unlockedAtMs == 0L && !isKeyguardLocked()) {
            unlockedAtMs = SystemClock.elapsedRealtime();
        }
        tryShow();
    }

    public void onHomePaused(@NonNull LauncherHomeActivity activity) {
        if (home != null && home.get() == activity) {
            home = null;
        }
    }

    // ---------------------------------------------------------------- lock / unlock

    private void watchKeyguard() {
        if (!armed) {
            return;
        }
        screenOnAtMs = SystemClock.elapsedRealtime();
        handler.removeCallbacks(keyguardPoll);
        handler.post(keyguardPoll);
    }

    private void onLocked() {
        handler.removeCallbacks(keyguardPoll);
        armed = canRun();
        unlockedAtMs = 0L;
        Log.d(TAG, "phone locked, armed=" + armed + (armed ? "" : " (" + whyNot() + ")"));
        if (armed) {
            // Loaded now, so the ad is ready the moment the phone is unlocked (only while the internet is on).
            SwipeAds.preloadUnlock(context);
        }
    }

    private void onUnlocked() {
        Log.d(TAG, "phone unlocked, armed=" + armed);
        if (!armed) {
            return;
        }
        unlockedAtMs = SystemClock.elapsedRealtime();
        tryShow();
    }

    /** The unlock counts for a few seconds: Home resumes just before or just after the broadcast. */
    private void tryShow() {
        if (!armed || unlockedAtMs == 0L) {
            return;
        }
        if (SystemClock.elapsedRealtime() - unlockedAtMs > UNLOCK_WINDOW_MS) {
            Log.d(TAG, "the Home page did not come in front in time");
            armed = false;
            return;
        }
        LauncherHomeActivity activity = home == null ? null : home.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        // The QR page has its own App Open ad (ProcessAppOpen).
        if (activity.getLauncherCurrentItem() == LauncherPagerAdapter.PAGE_RIGHT) {
            Log.d(TAG, "the QR page is in front, ProcessAppOpen handles it");
            armed = false;
            return;
        }
        armed = false;
        unlockedAtMs = 0L;
        show(activity);
    }

    // ---------------------------------------------------------------- the ad

    private void show(@NonNull Activity activity) {
        if (showing || !canRun()) {
            Log.d(TAG, "not shown (" + (showing ? "another unlock ad is showing" : whyNot()) + ")");
            return;
        }
        // The normal foreground App Open ad must not stack on this unlock ad.
        ProcessAppOpen.blockFor(8_000L);
        runningSince = SystemClock.elapsedRealtime();
        // The slot time is written before the ad shows: a failed request also uses its slot (no retry spam).
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_SHOW, System.currentTimeMillis()).apply();
        Log.d(TAG, "showing; next one after " + (DAY_MS / 1000L / Math.max(1, SwipeAds.getUnlockPerDay())) + " s");
        showing = true;
        SwipeAds.showUnlock(activity, () -> {
            showing = false;
            runningSince = 0L;
            // No request right after an ad: the next one is requested when the phone locks again and the gap is over.
        });
    }

    /** Remote Config was applied: keep the next ad loaded ahead with the new ids / type (the ad itself is armed by the next lock). */
    public void onConfigApplied() {
        if (armed && context != null) {
            SwipeAds.preloadUnlock(context);
        }
    }

    // ---------------------------------------------------------------- gates

    /**
     * Switch on, Per_Day above 0, this app is the Home app, internet on, ads allowed (consent and ad SDK), the common
     * country lists allow this place, a unit id exists (a sequence brings its own), and the gap since the last unlock
     * ad (24 h / Per_Day) is over.
     */
    private boolean canRun() {
        return whyNot() == null;
    }

    /** The first failing reason, or null when everything passes (also used for the log). */
    @Nullable
    private String whyNot() {
        if (context == null) {
            return "not started";
        }
        SwipeAds.initUnlock(context);
        if (!SwipeAds.isUnlockShow()) {
            return "Unlock_App_Open_Show is false";
        }
        int perDay = SwipeAds.getUnlockPerDay();
        if (perDay <= 0) {
            return "Unlock_App_Open_Per_Day is 0";
        }
        if (!AppUtils.isDefaultHomeApp(context)) {
            return "this app is not the default Home app";
        }
        if (!AdPlacement.isNetworkAvailable(context)) {
            return "no internet";
        }
        if (!AdPlacement.canRequestAds(context)) {
            return "ads cannot be requested (consent / ad SDK / Ads_Show_countryList / Ads_show_List_Exclude)";
        }
        if (!AdGates.adsLocationAllowed(context)) {
            return "blocked by Ads_Show_countryList / Ads_show_List_Exclude";
        }
        if (AdPlacement.shouldUseQuizPriority()) {
            return "Ad_Priority is QUIZ (no Quiz ad is substituted here)";
        }
        if (runningSince != 0L && SystemClock.elapsedRealtime() - runningSince < STALE_RUNNING_MS) {
            return "an unlock ad is running";
        }
        if (!SwipeAds.unlockHasUsableStep()) {
            return "no unit id for any step (Unlock_App_Open_Id / App_Open_Id / Interstitial / Native)";
        }
        long last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_SHOW, 0L);
        if (System.currentTimeMillis() - last < DAY_MS / perDay) {
            return "the gap since the last unlock ad (24 h / " + perDay + ") is not over";
        }
        return null;
    }

    private boolean isKeyguardLocked() {
        KeyguardManager manager = context == null ? null : (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return manager != null && manager.isKeyguardLocked();
    }
}
