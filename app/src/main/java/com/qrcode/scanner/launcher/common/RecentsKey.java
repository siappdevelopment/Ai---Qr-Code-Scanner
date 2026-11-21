package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

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

import java.lang.ref.WeakReference;

/**
 * Detects the system Recents key (ACTION_CLOSE_SYSTEM_DIALOGS, reason "recentapps") and opens {@link RecentActivity}
 * instead of the system Recents UI. Only while this app is the default Home app and Recent_Screen_Show is on.
 * <p>
 * Open sequence (measured numbers of the recent-screen skill):
 * <ol>
 * <li>the system overview comes up at once; an activity started before ~250 ms ends UNDER it, one started at >= ~300 ms
 * ends on top;</li>
 * <li>{@link RecentOverlay} draws the same page at once (needs "Display over other apps");</li>
 * <li>wait until the front window lost focus (the overview took it, max 1.5 s) + {@link #SETTLE_MS};</li>
 * <li>start {@link RecentActivity} (NEW_TASK | CLEAR_TOP | NO_ANIMATION), then re-start it up to 3 times, 500 ms apart,
 * while it is not resumed; the overlay is removed once the activity has focus.</li>
 * </ol>
 * It works with an activity of this app in front and with ANOTHER app in front (opened from the launcher): then the
 * Recent screen opens over that app and closing it returns to the launcher Home.
 */
public final class RecentsKey {
    private static final String TAG = "RecentsKey";
    private static final long DEDUP_MS = 300L;
    private static final long SETTLE_MS = 300L;
    private static final long WAIT_FOCUS_MAX_MS = 1500L;
    private static final long WAIT_FOCUS_STEP_MS = 30L;
    /** No window of ours to watch (another app in front): the overview needs about this long to come up. */
    private static final long OTHER_APP_OVERVIEW_MS = 450L;
    private static final long REASSERT_EVERY_MS = 500L;
    private static final int MAX_REASSERT = 3;
    private static final long KEY_WHILE_OPEN_MS = 900L;
    private static final long COVERED_EARLY_MS = 1500L;
    private static final int MAX_RELAUNCH = 2;

    private static final Handler handler = new Handler(Looper.getMainLooper());
    /** The activity of this app that is in front right now; cleared when it is stopped (never a stale reference). */
    private static WeakReference<Activity> topActivity = new WeakReference<>(null);
    private static WeakReference<RecentActivity> lastRecent = new WeakReference<>(null);
    private static Intent lastRecentIntent;
    private static long recentResumedAt;
    private static int relaunchCount;
    private static int startedCount;
    private static long lastKeyAt;
    private static int openToken;
    private static boolean installed;

    private RecentsKey() {
    }

    public static synchronized void install(@Nullable Application app) {
        if (installed || app == null) {
            return;
        }
        installed = true;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                startedCount++;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                topActivity = new WeakReference<>(activity);
                RecentOverlay.rememberUiMode(activity.getResources().getConfiguration().uiMode);
                if (activity instanceof RecentActivity) {
                    recentResumedAt = SystemClock.elapsedRealtime();
                    lastRecent = new WeakReference<>((RecentActivity) activity);
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                startedCount = Math.max(0, startedCount - 1);
                if (activity instanceof RecentActivity && !activity.isFinishing()) {
                    relaunchIfCovered(activity.getApplicationContext());
                }
                // A stopped activity is not in front any more (otherwise the next Recents key would be ignored).
                if (topActivity.get() == activity) {
                    topActivity = new WeakReference<>(null);
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
        ContextCompat.registerReceiver(app, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }
                String reason = intent.getStringExtra("reason");
                Log.d(TAG, "CLOSE_SYSTEM_DIALOGS received, reason=" + reason);
                if ("recentapps".equals(reason)) {
                    onRecentsKey(context.getApplicationContext());
                }
            }
        }, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS), ContextCompat.RECEIVER_EXPORTED);
        // Icons of the apps strip are ready before the first key press.
        RecentPage.warm(app);
    }

    /** Screens that must never be covered (onboarding, permission / default flows, call end, event screens, ad screens). */
    private static boolean isHandledElsewhere(@Nullable Activity activity) {
        return activity == null
                || activity instanceof ClEndActivity
                || activity instanceof EventPromptActivity
                || activity instanceof ChargingScreenActivity                || activity instanceof LanguageActivity
                || activity instanceof IntroActivity
                || activity instanceof CollectionActivity
                || activity instanceof PermissionActivity
                || activity instanceof DefaultActivity
                || activity instanceof DefaultSettingHomeActivity
                || activity instanceof DefaultHomeGuideActivity
                || activity instanceof OverlayPermissionGuideActivity
                || activity.getClass().getName().startsWith("com.google.android.gms.ads");
    }

    private static boolean isRecentVisible() {
        Activity top = topActivity.get();
        return startedCount > 0 && top instanceof RecentActivity && !top.isFinishing() && RecentActivity.isResumedNow();
    }

    /** All three must hold, otherwise the system overview is left alone. */
    private static String blockedReason(Context app) {
        AdPlacement.loadRecentConfig(app);
        if (!AppUtils.isDefaultHomeApp(app)) {
            return "this app is not the default Home app";
        }
        if (!AdPlacement.getRecentScreenShow()) {
            return "Recent_Screen_Show is false";
        }
        if (!AdGates.adsLocationAllowed(app)) {
            return "blocked by Ads_Show_countryList / Ads_show_List_Exclude";
        }
        return null;
    }

    private static void onRecentsKey(Context app) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastKeyAt < DEDUP_MS) {
            Log.d(TAG, "ignored: second key within " + DEDUP_MS + " ms");
            return;
        }
        lastKeyAt = now;
        final boolean overOtherApp = startedCount <= 0;
        final Activity front = overOtherApp ? null : topActivity.get();
        String why = blockedReason(app);
        if (why == null && front instanceof RecentActivity) {
            // The Recents key while the page is open: the overview opens over it again, so bring the page back.
            if (RecentActivity.userLeft) {
                return;
            }
            Log.d(TAG, "Recents key while the Recent screen is open: bringing it back");
            handler.postDelayed(() -> reassert(app), KEY_WHILE_OPEN_MS);
            return;
        }
        if (why == null && !overOtherApp && isHandledElsewhere(front)) {
            why = "front activity is " + (front == null ? "null" : front.getClass().getSimpleName());
        }
        if (why != null) {
            Log.d(TAG, "Recents key ignored: " + why);
            return;
        }
        Log.d(TAG, "Recents key: opening the Recent screen over " + (overOtherApp ? "another app" : front.getClass().getSimpleName()));
        final ComponentName returnTo = overOtherApp || front instanceof LauncherHomeActivity ? null : front.getComponentName();
        final int token = ++openToken;
        RecentActivity.userLeft = false;
        // 1) The page at once, as an overlay (without the permission it simply shows when the activity is up).
        RecentOverlay.show(app);
        // 2) Wait for the system overview to take the focus, then let it settle.
        waitForOverview(front, token, () -> handler.postDelayed(() -> startRecent(app, returnTo, overOtherApp, token), SETTLE_MS));
    }

    private static void waitForOverview(@Nullable Activity front, int token, Runnable then) {
        if (front == null) {
            handler.postDelayed(then, OTHER_APP_OVERVIEW_MS);
            return;
        }
        final long start = SystemClock.elapsedRealtime();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (token != openToken) {
                    return;
                }
                boolean lostFocus = !front.hasWindowFocus();
                if (lostFocus || SystemClock.elapsedRealtime() - start >= WAIT_FOCUS_MAX_MS) {
                    then.run();
                } else {
                    handler.postDelayed(this, WAIT_FOCUS_STEP_MS);
                }
            }
        });
    }

    private static void startRecent(Context app, @Nullable ComponentName returnTo, boolean overOtherApp, int token) {
        if (token != openToken) {
            return;
        }
        if (RecentOverlay.handledByUser()) {
            Log.d(TAG, "the user already used the overlay: the activity is not started");
            return;
        }
        if (isRecentVisible() || !AppUtils.isDefaultHomeApp(app) || !AdPlacement.getRecentScreenShow()) {
            RecentOverlay.hide();
            return;
        }
        Intent intent = new Intent(app, RecentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(RecentActivity.EXTRA_RETURN_TO, returnTo);
        intent.putExtra(RecentActivity.EXTRA_OVER_OTHER_APP, overOtherApp);
        lastRecentIntent = intent;
        relaunchCount = 0;
        try {
            app.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "could not start the Recent screen: " + e);
            RecentOverlay.hide();
            return;
        }
        // 3) Not resumed 500 ms later (the overview covered it): bring it back, up to 3 times.
        scheduleReassert(app, token, 1);
    }

    private static void scheduleReassert(Context app, int token, int attempt) {
        handler.postDelayed(() -> {
            if (token != openToken || RecentActivity.userLeft || RecentActivity.isResumedNow() || attempt > MAX_REASSERT) {
                return;
            }
            Log.d(TAG, "the Recent screen is not on top yet: re-start #" + attempt);
            reassert(app);
            scheduleReassert(app, token, attempt + 1);
        }, REASSERT_EVERY_MS);
    }

    private static void reassert(Context app) {
        if (lastRecentIntent == null || RecentActivity.userLeft) {
            return;
        }
        Intent intent = new Intent(lastRecentIntent);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            app.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "could not bring the Recent screen back: " + e);
        }
    }

    /** The system Recents UI may cover the screen right after it opens: bring it back (up to twice). */
    private static void relaunchIfCovered(Context app) {
        if (lastRecentIntent == null || RecentActivity.userLeft || relaunchCount >= MAX_RELAUNCH
                || SystemClock.elapsedRealtime() - recentResumedAt > COVERED_EARLY_MS) {
            return;
        }
        relaunchCount++;
        Log.d(TAG, "the Recent screen was covered by the system Recents: relaunch #" + relaunchCount);
        handler.postDelayed(() -> {
            RecentActivity recent = lastRecent.get();
            if (recent == null || recent.isFinishing() || recent.isDestroyed()) {
                return;
            }
            reassert(app);
        }, 300L);
    }
}
