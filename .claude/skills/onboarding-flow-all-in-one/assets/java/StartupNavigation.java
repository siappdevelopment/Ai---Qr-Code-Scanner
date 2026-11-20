package com.qrcode.scanner.launcher.common;

import android.app.Activity;

import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;

/**
 * Routes after the source startup sequence finishes on the existing Compose splash.
 */
public final class StartupNavigation {
    private StartupNavigation() {
    }

    /**
     * @return true when the current activity should show the QR app. False when another activity was started.
     */
    public static boolean continueAfterVisibleSplash(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        // Tap on the "make this app your default launcher" reminder: go to the Default screen when it applies.
        if (DefaultLauncherReminder.handleSplashEntry(activity)) {
            return false;
        }
        ScreenFlowConfig.ensureShowScreenFlow(activity);
        if (!AppUtils.hasCompletedOnboarding(activity)) {
            ScreenFlowNavigation.openFirstScreen(activity);
            return false;
        }
        if (staysInQrApp(activity)) {
            return true;
        }
        ScreenFlowNavigation.openMain(activity);
        return false;
    }

    private static boolean staysInQrApp(Activity activity) {
        return !AppUtils.isDefaultHomeApp(activity) && !ScreenFlowConfig.getRedirectHomeLauncher();
    }
}
