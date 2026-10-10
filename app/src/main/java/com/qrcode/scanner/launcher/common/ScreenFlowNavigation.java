package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.qrcode.scanner.MainActivity;
import com.qrcode.scanner.launcher.activities.CollectionActivity;
import com.qrcode.scanner.launcher.activities.DefaultActivity;
import com.qrcode.scanner.launcher.activities.DefaultSettingHomeActivity;
import com.qrcode.scanner.launcher.activities.Intro1Activity;
import com.qrcode.scanner.launcher.activities.LanguageActivity;
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity;
import com.qrcode.scanner.launcher.activities.PermissionActivity;
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;

import java.util.List;

public final class ScreenFlowNavigation {
    public static final String EXTRA_SKIP_STARTUP_SPLASH = "skip_startup_splash";
    public static final String EXTRA_LANGUAGE_FLOW_STARTING = "extra_language_flow_starting";
    private static final long FINAL_NAVIGATION_DEBOUNCE_MS = 1000L;
    private static long lastFinalNavigationElapsedMs;

    private ScreenFlowNavigation() {
    }

    public static void openFirstScreen(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        ensureFlow(activity);
        if (AppUtils.hasCompletedOnboarding(activity)) {
            openMain(activity);
            return;
        }
        openFromIndex(activity, 0);
    }

    public static void continueAfter(Activity activity, String completedScreen) {
        if (activity == null) {
            return;
        }
        Context appContext = activity.getApplicationContext();
        ensureFlow(appContext);
        List<String> flow = ScreenFlowConfig.getShowScreenFlow();
        int completedIndex = indexOfScreen(flow, completedScreen);
        // Clear only screens the flow has not passed yet; a late/duplicate callback of an earlier
        // screen must never send the user back through screens that are already done.
        int passed = AppUtils.getFlowProgress(appContext);
        markScreenCompleted(appContext, completedScreen);
        clearCompletionsAfter(appContext, flow, completedIndex < 0 ? completedIndex : Math.max(completedIndex, passed - 1));
        if (completedIndex >= 0) {
            AppUtils.advanceFlowProgress(appContext, completedIndex + 1);
        }

        int startIndex = completedIndex >= 0 ? completedIndex + 1 : 0;
        if (activity.isFinishing() || activity.isDestroyed()) {
            openFromIndex(appContext, startIndex);
            return;
        }
        openFromIndex(activity, startIndex);
    }

    private static void openFromIndex(Activity activity, int startIndex) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        openFromIndex((Context) activity, startIndex);
    }

    private static void openFromIndex(Context context, int startIndex) {
        if (context == null) {
            return;
        }
        List<String> flow = ScreenFlowConfig.getShowScreenFlow();
        String next = findNextIncomplete(context, flow, startIndex);
        if (next == null) {
            // Flow finished: also close the last onboarding screen. The launcher opens in its own task
            // (taskAffinity ""), so CLEAR_TASK would leave this activity alive in the app task, and it would
            // show again when an Event/Charging screen is closed.
            if (context instanceof Activity && !(context instanceof LauncherHomeActivity)) {
                openMain((Activity) context);
            } else {
                openMain(context);
            }
            return;
        }
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                openScreen(activity, next);
                return;
            }
        }
        launchScreen(context, next);
    }

    private static void ensureFlow(Context context) {
        if (ScreenFlowConfig.getShowScreenFlow().isEmpty()) {
            ScreenFlowConfig.ensureShowScreenFlow(context);
        }
    }

    private static String findNextIncomplete(Context context, List<String> flow, int startIndex) {
        if (flow == null || flow.isEmpty()) {
            return null;
        }
        int start = Math.max(0, startIndex);
        for (int i = start; i < flow.size(); i++) {
            String screen = flow.get(i);
            if (!ScreenFlowConfig.isSupportedScreen(screen)) {
                continue;
            }
            if (!isScreenCompleted(context, screen)) {
                return screen;
            }
        }
        return null;
    }

    public static boolean isScreenCompleted(Context context, String screenName) {
        if (context == null || screenName == null) {
            return true;
        }
        if (ScreenFlowConfig.SCREEN_LANGUAGE.equalsIgnoreCase(screenName)) {
            return AppUtils.getLanguageFlowCompleted(context);
        }
        if (ScreenFlowConfig.SCREEN_COLLECTION.equalsIgnoreCase(screenName)) {
            return AppUtils.isCollectionScreenCompleted(context);
        }
        if (ScreenFlowConfig.SCREEN_PERMISSION.equalsIgnoreCase(screenName)) {
            return AppUtils.isPermissionScreenCompleted(context);
        }
        if (ScreenFlowConfig.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screenName)) {
            if (AppUtils.isDefaultHomeApp(context) && !AppUtils.isDefaultHomeScreenCompleted(context)) {
                AppUtils.setDefaultHomeScreenCompleted(context, true);
            }
            return AppUtils.isDefaultHomeScreenCompleted(context) || AppUtils.isDefaultHomeApp(context);
        }
        if (ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME.equalsIgnoreCase(screenName)) {
            return AppUtils.isDefaultHomeApp(context) || AppUtils.isDefaultSettingHomeScreenCompleted(context);
        }
        if (ScreenFlowConfig.SCREEN_INTRO.equalsIgnoreCase(screenName)) {
            return AppUtils.getIntroCompleted(context);
        }
        return true;
    }

    private static void markScreenCompleted(Context context, String screen) {
        if (context == null || screen == null) {
            return;
        }
        if (ScreenFlowConfig.SCREEN_LANGUAGE.equalsIgnoreCase(screen)) {
            AppUtils.setLanguageSelectedCommit(context, true);
            AppUtils.setLanguageFlowCompleted(context, true);
        } else if (ScreenFlowConfig.SCREEN_COLLECTION.equalsIgnoreCase(screen)) {
            AppUtils.setCollectionScreenCompleted(context, true);
        } else if (ScreenFlowConfig.SCREEN_PERMISSION.equalsIgnoreCase(screen)) {
            AppUtils.setPermissionScreenCompleted(context, true);
        } else if (ScreenFlowConfig.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screen)) {
            AppUtils.setDefaultHomeScreenCompleted(context, true);
        } else if (ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME.equalsIgnoreCase(screen)) {
            AppUtils.setDefaultSettingHomeScreenCompleted(context, true);
        } else if (ScreenFlowConfig.SCREEN_INTRO.equalsIgnoreCase(screen)) {
            AppUtils.setIntroCompleted(context, true);
        }
    }

    private static void clearCompletionsAfter(Context context, List<String> flow, int completedIndex) {
        if (context == null || flow == null || completedIndex < 0) {
            return;
        }
        for (int i = completedIndex + 1; i < flow.size(); i++) {
            String screen = flow.get(i);
            if (ScreenFlowConfig.SCREEN_LANGUAGE.equalsIgnoreCase(screen)) {
                AppUtils.setLanguageFlowCompleted(context, false);
            } else if (ScreenFlowConfig.SCREEN_COLLECTION.equalsIgnoreCase(screen)) {
                AppUtils.setCollectionScreenCompleted(context, false);
            } else if (ScreenFlowConfig.SCREEN_PERMISSION.equalsIgnoreCase(screen)) {
                AppUtils.setPermissionScreenCompleted(context, false);
            } else if (ScreenFlowConfig.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screen)) {
                AppUtils.setDefaultHomeScreenCompleted(context, false);
            } else if (ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME.equalsIgnoreCase(screen)) {
                AppUtils.setDefaultSettingHomeScreenCompleted(context, false);
            } else if (ScreenFlowConfig.SCREEN_INTRO.equalsIgnoreCase(screen)) {
                AppUtils.setIntroCompleted(context, false);
            }
        }
    }

    private static int indexOfScreen(List<String> flow, String screenName) {
        if (flow == null || screenName == null) {
            return -1;
        }
        for (int i = 0; i < flow.size(); i++) {
            if (screenName.equalsIgnoreCase(flow.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static void openScreen(Activity activity, String screenName) {
        if (activity == null || activity.isFinishing() || screenName == null) {
            return;
        }
        if (ScreenFlowConfig.SCREEN_LANGUAGE.equalsIgnoreCase(screenName)) {
            AppUtils.isLanguageFromStarting = true;
        } else if (ScreenFlowConfig.SCREEN_INTRO.equalsIgnoreCase(screenName)) {
            IntroNavigation.openIntroButtonFlow(activity);
            return;
        }
        Intent intent = buildScreenIntent(activity, screenName);
        if (intent == null) {
            openMain(activity);
            return;
        }
        if (ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME.equalsIgnoreCase(screenName)
                && !(activity instanceof LauncherHomeActivity)) {
            // Invisible step: keep the current screen alive underneath (no CLEAR_TASK, no finish) so
            // nothing blinks before the system settings page. continueAfter() clears it afterwards.
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        if (activity instanceof LauncherHomeActivity) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                activity.finish();
            }
        });
    }

    /** Drops the splash task so Recent Apps does not keep both the splash and the app. */
    private static void removeLaunchingActivity(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (activity instanceof MainActivity) {
            activity.finishAndRemoveTask();
            return;
        }
        activity.finish();
    }

    private static Intent buildScreenIntent(Context context, String screen) {
        if (ScreenFlowConfig.SCREEN_LANGUAGE.equalsIgnoreCase(screen)) {
            Intent intent = new Intent(context, LanguageActivity.class);
            intent.putExtra(EXTRA_LANGUAGE_FLOW_STARTING, true);
            return intent;
        }
        if (ScreenFlowConfig.SCREEN_COLLECTION.equalsIgnoreCase(screen)) {
            return new Intent(context, CollectionActivity.class);
        }
        if (ScreenFlowConfig.SCREEN_PERMISSION.equalsIgnoreCase(screen)) {
            return new Intent(context, PermissionActivity.class);
        }
        if (ScreenFlowConfig.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screen)) {
            return new Intent(context, DefaultActivity.class);
        }
        if (ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME.equalsIgnoreCase(screen)) {
            return new Intent(context, DefaultSettingHomeActivity.class);
        }
        if (ScreenFlowConfig.SCREEN_INTRO.equalsIgnoreCase(screen)) {
            return new Intent(context, Intro1Activity.class);
        }
        return null;
    }

    public static void openMain(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (!openFinalDestination(activity)) {
            return;
        }
        removeLaunchingActivity(activity);
    }

    public static void openMain(Context context) {
        if (context == null) {
            return;
        }
        openFinalDestination(context);
    }

    private static void launchScreen(Context context, String screenName) {
        if (context == null || screenName == null) {
            return;
        }
        if (ScreenFlowConfig.SCREEN_INTRO.equalsIgnoreCase(screenName)) {
            if (IntroNavigation.getEffectiveIntroButtonScreenCount() == 0) {
                markScreenCompleted(context, ScreenFlowConfig.SCREEN_INTRO);
                openMain(context);
                return;
            }
            Intent introIntent = new Intent(context, Intro1Activity.class);
            introIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(introIntent);
            return;
        }
        if (ScreenFlowConfig.SCREEN_LANGUAGE.equalsIgnoreCase(screenName)) {
            AppUtils.isLanguageFromStarting = true;
        }
        Intent intent = buildScreenIntent(context, screenName);
        if (intent == null) {
            openMain(context);
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    private static boolean openFinalDestination(Context context) {
        if (context == null) {
            return false;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastFinalNavigationElapsedMs < FINAL_NAVIGATION_DEBOUNCE_MS) {
            return false;
        }
        lastFinalNavigationElapsedMs = now;
        Intent intent = buildFinalDestinationIntent(context);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (AppUtils.isDefaultHomeApp(context)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }
        context.startActivity(intent);
        return true;
    }

    private static Intent buildFinalDestinationIntent(Context context) {
        if (AppUtils.isDefaultHomeApp(context)) {
            return AppUtils.buildLauncherHomeIntent(context);
        }
        if (ScreenFlowConfig.getRedirectHomeLauncher()) {
            Intent intent = AppUtils.buildLauncherHomeIntent(context);
            WidgetNavigation.attachPendingTargetScreen(context, intent);
            return intent;
        }
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_SKIP_STARTUP_SPLASH, true);
        WidgetNavigation.attachPendingTargetScreen(context, intent);
        return intent;
    }
}
