package com.qrcode.scanner.launcher.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.ProcessAppOpen;
import com.qrcode.scanner.launcher.common.ScreenFlowNavigation;
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;

/**
 * "DefultSettingHome" step of Show_Screen_Flow. Has no UI of its own: it opens the system Home
 * (launcher) settings directly, shows the Default Home guide dialog-card on top of it, and moves to
 * the next flow screen when the user comes back.
 */
public class DefaultSettingHomeActivity extends ComponentActivity {
    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                stopHomeWatch();
                continueFlow();
            });
    private boolean guidePending;
    private final Handler watchHandler = new Handler(Looper.getMainLooper());
    private String homePackageAtStart;
    /** True once the flow moved on, so the background path and the Settings result never both advance it. */
    private boolean flowContinued;
    private static final long HOME_WATCH_INTERVAL_MS = 500L;
    private static final long BACKGROUND_START_CHECK_MS = 2000L;
    private boolean setupFlagSet;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            return;
        }
        if (AppUtils.isDefaultHomeApp(this) || !openHomeSettings()) {
            continueFlow();
        }
    }

    private boolean openHomeSettings() {
        Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
        if (intent.resolveActivity(getPackageManager()) == null) {
            intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            if (intent.resolveActivity(getPackageManager()) == null) {
                return false;
            }
        }
        // Same setup flags DefaultActivity uses, so LauncherHomeActivity (opened by the system once this
        // app becomes the default Home) hands back to the screen flow instead of staying on the launcher.
        AppUtils.setCompletingDefaultAppSetup(this, true);
        AppUtils.setDefaultSetupStep(this, ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME);
        AppUtils.setAwaitingDefaultRoleResult(this, true);
        setupFlagSet = true;
        try {
            settingsLauncher.launch(intent);
        } catch (Exception e) {
            clearSetupFlags();
            return false;
        }
        guidePending = true;
        startHomeWatch();
        return true;
    }

    private void clearSetupFlags() {
        setupFlagSet = false;
        AppUtils.setAwaitingDefaultRoleResult(this, false);
        AppUtils.setCompletingDefaultAppSetup(this, false);
    }

    /**
     * Runs when Settings is taking over the screen. Starting the guide here (not together with the
     * Settings launch) puts it on top of Settings. Own task, so a flow change (CLEAR_TASK) or
     * Settings' task cannot remove or hide it.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (guidePending) {
            guidePending = false;
            startActivity(new Intent(this, DefaultHomeGuideActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    private void continueFlow() {
        if (flowContinued) {
            return;
        }
        if (isFinishing()) {
            return;
        }
        if (setupFlagSet) {
            // LauncherHomeActivity clears the flag when it has already continued the flow; do not repeat it.
            if (!AppUtils.isCompletingDefaultAppSetup(this)) {
                finish();
                return;
            }
            if (AppUtils.isDefaultHomeApp(this) && !AppUtils.tryClaimAfterDefaultFlow(this)) {
                finish();
                return;
            }
            clearSetupFlags();
        }
        flowContinued = true;
        stopHomeWatch();
        ScreenFlowNavigation.continueAfter(this, ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME);
    }

    /**
     * The user may pick another launcher in the system list; the system then brings that launcher's Home forward
     * and this app is left behind it. While Settings is open, watch the default Home: when it changes to another
     * app, move the flow on from here (Android may block a background start; see {@link #moveOnFromBackground()}).
     */
    private void startHomeWatch() {
        stopHomeWatch();
        homePackageAtStart = currentHomePackage();
        watchHandler.postDelayed(homeWatchRunnable, HOME_WATCH_INTERVAL_MS);
    }

    private void stopHomeWatch() {
        watchHandler.removeCallbacks(homeWatchRunnable);
    }

    private final Runnable homeWatchRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || flowContinued) {
                return;
            }
            String now = currentHomePackage();
            if (now != null && !now.equals(homePackageAtStart) && !now.equals(getPackageName())) {
                moveOnFromBackground();
                return;
            }
            watchHandler.postDelayed(this, HOME_WATCH_INTERVAL_MS);
        }
    };

    /**
     * Starts the next flow screen while another launcher is in front. Android only allows this in some cases
     * (for example with the overlay permission). If this app is still not in front shortly after, the attempt is
     * undone so the normal path (the Settings result, when the user returns) still moves the flow on, once.
     */
    private void moveOnFromBackground() {
        continueFlow();
        watchHandler.postDelayed(() -> {
            if (!isFinishing() && !ProcessAppOpen.isAppInForeground()) {
                flowContinued = false;
            }
        }, BACKGROUND_START_CHECK_MS);
    }

    @Nullable
    private String currentHomePackage() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        if (info == null || info.activityInfo == null) {
            return null;
        }
        String packageName = info.activityInfo.packageName;
        // "android" is the system chooser (no default Home chosen yet), not a launcher.
        return "android".equals(packageName) ? null : packageName;
    }

    @Override
    protected void onDestroy() {
        stopHomeWatch();
        super.onDestroy();
    }
}
