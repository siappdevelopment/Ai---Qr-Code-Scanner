package com.qrcode.scanner.launcher.activities;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.qrcode.scanner.launcher.common.AppUtils;
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
                Log.e("Milind", "Settings result code=" + result.getResultCode());
                continueFlow();
            });
    private boolean guidePending;
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
            Log.e("Milind", "openHomeSettings: "+e );
            clearSetupFlags();
            return false;
        }
        Log.e("Milind", "openHomeSettings: launched");
        guidePending = true;
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
            Log.e("Milind", "starting guide over Settings");
            startActivity(new Intent(this, DefaultHomeGuideActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    private void continueFlow() {
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
        ScreenFlowNavigation.continueAfter(this, ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME);
    }
}
