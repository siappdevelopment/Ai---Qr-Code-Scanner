package com.calendar.eventplanner.tasktracker.ADS.advertisement;

import android.app.Activity;

/**
 * Shared Set-as-Default button ad. Delegates to the launcher helper so
 * Allow / Cancel use the same preloaded instance.
 */
@SuppressWarnings("all")
public class DefaultPermissionButtonAd {

    public static void preload(Activity context) {
        com.calendar.eventplanner.tasktracker.ADS.launcher.utils.DefaultPermissionButtonAd.preload(context);
    }

    public static void resetShownFlag() {
        com.calendar.eventplanner.tasktracker.ADS.launcher.utils.DefaultPermissionButtonAd.resetShownFlag();
    }

    public static boolean isReady() {
        return com.calendar.eventplanner.tasktracker.ADS.launcher.utils.DefaultPermissionButtonAd.isReady();
    }

    public static boolean shouldShow() {
        return com.calendar.eventplanner.tasktracker.ADS.launcher.utils.DefaultPermissionButtonAd.shouldShow();
    }

    public static void show(Activity context, OnCompeteAds onFinishAd) {
        com.calendar.eventplanner.tasktracker.ADS.launcher.utils.DefaultPermissionButtonAd.show(
                context,
                b -> {
                    if (onFinishAd != null) {
                        onFinishAd.onCompeteAds(b);
                    }
                }
        );
    }

    public interface OnCompeteAds {
        void onCompeteAds(boolean b);
    }
}
