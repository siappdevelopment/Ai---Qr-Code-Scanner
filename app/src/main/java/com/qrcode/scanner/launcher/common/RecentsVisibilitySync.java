package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Recents rule: this app IS the default Home launcher -> every task of the app is hidden from the system Recents;
 * it is NOT the default Home -> the app shows in Recents. The manifest flag is static but the default Home role can
 * change at run time, so the state is applied to all tasks each time an activity resumes / pauses.
 */
public final class RecentsVisibilitySync implements Application.ActivityLifecycleCallbacks {

    private static void sync(@NonNull Activity activity) {
        try {
            boolean exclude = AppUtils.isDefaultHomeApp(activity);
            ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager == null) {
                return;
            }
            for (ActivityManager.AppTask task : manager.getAppTasks()) {
                // Default Home: every task of the app is hidden. Not default: only the main QR app task (MainActivity and the
                // onboarding screens started from it) shows in Recents; the guide cards, the Recent screen and the launcher
                // Home task stay hidden as their manifests say (they must never become extra cards in the system Recents).
                task.setExcludeFromRecents(exclude || !isMainTask(task));
            }
        } catch (Throwable ignored) {
            // Never let Recents handling break navigation.
        }
    }

    private static boolean isMainTask(@NonNull ActivityManager.AppTask task) {
        try {
            ActivityManager.RecentTaskInfo info = task.getTaskInfo();
            android.content.ComponentName base = info == null || info.baseIntent == null ? null : info.baseIntent.getComponent();
            return base != null && "com.qrcode.scanner.MainActivity".equals(base.getClassName());
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        sync(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        sync(activity);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
