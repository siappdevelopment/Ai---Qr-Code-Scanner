package com.qrcode.scanner.launcher.activities;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.ScreenFlowNavigation;
import com.qrcode.scanner.launcher.common.TrackOnce;
import com.qrcode.scanner.launcher.helpers.DefaultHomePromptHelper;
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;

/**
 * Onboarding Default Home step. The role request stays on DefaultHomePromptHelper.
 * Phone state is requested first. Setup flags let LauncherHome claim the same attempt only once.
 */
public class DefaultActivity extends AppCompatActivity {
    private static final String STATE_HAS_NAVIGATED = "state_has_navigated";
    private static final String STATE_PHONE_STEP_COMPLETED = "state_phone_step_completed";
    private static final String STATE_WAITING_PHONE_STATE = "state_waiting_phone_state";

    private final DefaultHomePromptHelper defaultHomePromptHelper = new DefaultHomePromptHelper(this);
    private boolean hasNavigated;
    private boolean phoneStateStepCompleted;
    private boolean waitingForPhoneStatePermission;
    private boolean initialRoleRequestPending;

    private final ActivityResultLauncher<String> phonePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        waitingForPhoneStatePermission = false;
        phoneStateStepCompleted = true;
        if (granted) {
            AppUtils.trackPhoneStatePermissionGrantedOnce(this);
        }
        postBeginDefaultHomeRoleRequest();
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            hasNavigated = savedInstanceState.getBoolean(STATE_HAS_NAVIGATED, false);
            phoneStateStepCompleted = savedInstanceState.getBoolean(STATE_PHONE_STEP_COMPLETED, false);
            waitingForPhoneStatePermission = savedInstanceState.getBoolean(STATE_WAITING_PHONE_STATE, false);
        }
        defaultHomePromptHelper.registerRoleLauncher();
        defaultHomePromptHelper.setRoleFlowListener(this::onDefaultRoleFlowFinished);
        if (AppUtils.hasCompletedOnboarding(this)) {
            ScreenFlowNavigation.openMain(this);
            finish();
            return;
        }
        if (AppUtils.isDefaultHomeScreenCompleted(this)) {
            if (!hasNavigated) {
                ScreenFlowNavigation.continueAfter(this, ScreenFlowConfig.SCREEN_DEFAULT_HOME);
            } else {
                ScreenFlowNavigation.openMain(this);
            }
            finish();
            return;
        }
        if (AppUtils.isCompletingDefaultAppSetup(this) && !AppUtils.isAwaitingDefaultRoleResult(this)) {
            AppUtils.clearDefaultAppSetupState(this);
        }
        if (savedInstanceState == null && !AppUtils.isDefaultSettingHomeScreenCompleted(this)
                && AppUtils.createDefaultHomeRoleRequestIntent(this) != null && !AppUtils.isDefaultHomeApp(this)) {
            // Show the Default Home list (with guide) first; this screen only appears if it is not granted.
            initialRoleRequestPending = true;
            defaultHomePromptHelper.setRoleFlowListener(this::onInitialRoleFlowFinished);
            postBeginDefaultHomeRoleRequest();
            return;
        }
        setupDefaultScreen();
    }

    private void setupDefaultScreen() {
        setContentView(R.layout.activity_default);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        AdPlacement.showSlot(this, AdPlacement.getPermissionDefaultAdShow(), AdPlacement.getPermissionDefaultAdType(), AdPlacement.getPermissionDefaultBannerId(), AdPlacement.getPermissionDefaultNativeId(), "small", findViewById(R.id.rlAdView), findViewById(R.id.rlBannerAdView), findViewById(R.id.slBannerShimmer), findViewById(R.id.llBannerAd), findViewById(R.id.rlNativeAdView), findViewById(R.id.slNativeShimmer), findViewById(R.id.flNativeAd), false, AdPlacement.onboardingNativeColor(this));
        AdPlacement.preloadAfterDefaultAd(this);
        AppCompatTextView btnSetAsDefault = findViewById(R.id.btnSetAsDefault);
        btnSetAsDefault.setOnClickListener(view -> handleSetAsDefaultClick());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                defaultHomePromptHelper.release();
                AppUtils.clearDefaultAppSetupState(DefaultActivity.this);
                setEnabled(false);
                finish();
            }
        });
        if (phoneStateStepCompleted && !hasNavigated && !waitingForPhoneStatePermission) {
            postBeginDefaultHomeRoleRequest();
        }
    }

    private void onInitialRoleFlowFinished() {
        if (hasNavigated || isFinishing() || isDestroyed()) {
            return;
        }
        initialRoleRequestPending = false;
        if (AppUtils.isDefaultHomeApp(this)) {
            onDefaultRoleFlowFinished();
            return;
        }
        AppUtils.setAwaitingDefaultRoleResult(this, false);
        AppUtils.setCompletingDefaultAppSetup(this, false);
        defaultHomePromptHelper.setRoleFlowListener(this::onDefaultRoleFlowFinished);
        setupDefaultScreen();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_HAS_NAVIGATED, hasNavigated);
        outState.putBoolean(STATE_PHONE_STEP_COMPLETED, phoneStateStepCompleted);
        outState.putBoolean(STATE_WAITING_PHONE_STATE, waitingForPhoneStatePermission);
    }

    @Override
    protected void onResume() {
        super.onResume();
        defaultHomePromptHelper.onResume();
        if (!hasNavigated && !initialRoleRequestPending && hasWindowFocus() && AppUtils.isCompletingDefaultAppSetup(this) && AppUtils.isAwaitingDefaultRoleResult(this)) {
            onDefaultRoleFlowFinished();
        }
    }

    @Override
    protected void onDestroy() {
        defaultHomePromptHelper.release();
        if (!hasNavigated) {
            AppUtils.clearDefaultAppSetupState(this);
        } else {
            AppUtils.releaseAfterDefaultFlowClaim();
        }
        super.onDestroy();
    }

    private void handleSetAsDefaultClick() {
        if (hasNavigated || waitingForPhoneStatePermission) {
            return;
        }
        if (AppUtils.isDefaultHomeApp(this)) {
            TrackOnce.trackScreenOnce(this, "DEFAULT_HOME_APP_SET");
            continueScreenFlow();
            return;
        }
        if (AppUtils.hasPhoneStatePermission(this)) {
            AppUtils.trackPhoneStatePermissionGrantedOnce(this);
            phoneStateStepCompleted = true;
            postBeginDefaultHomeRoleRequest();
            return;
        }
        waitingForPhoneStatePermission = true;
        phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE);
    }

    private void postBeginDefaultHomeRoleRequest() {
        android.view.View decor = getWindow() != null ? getWindow().getDecorView() : null;
        if (decor != null) {
            decor.post(this::beginDefaultHomeRoleRequest);
        } else {
            beginDefaultHomeRoleRequest();
        }
    }

    private void beginDefaultHomeRoleRequest() {
        if (hasNavigated || isFinishing()) {
            return;
        }
        if (AppUtils.isDefaultHomeApp(this)) {
            TrackOnce.trackScreenOnce(this, "DEFAULT_HOME_APP_SET");
            continueScreenFlow();
            return;
        }
        AppUtils.setCompletingDefaultAppSetup(this, true);
        AppUtils.setAwaitingDefaultRoleResult(this, true);
        defaultHomePromptHelper.handleSetAsDefaultClick();
    }

    private void onDefaultRoleFlowFinished() {
        if (hasNavigated || isFinishing() || isDestroyed()) {
            return;
        }
        if (!AppUtils.isCompletingDefaultAppSetup(this)) {
            return;
        }
        if (AppUtils.isDefaultHomeApp(this) && !hasWindowFocus()) {
            return;
        }
        if (!AppUtils.tryClaimAfterDefaultFlow(this)) {
            return;
        }
        hasNavigated = true;
        AppUtils.setAwaitingDefaultRoleResult(this, false);
        AppUtils.setCompletingDefaultAppSetup(this, false);
        continueScreenFlow();
    }

    private void continueScreenFlow() {
        if (isFinishing()) {
            return;
        }
        if (!hasNavigated) {
            hasNavigated = true;
        }
        AdPlacement.loadAfterDefaultAd(this, () -> ScreenFlowNavigation.continueAfter(this, ScreenFlowConfig.SCREEN_DEFAULT_HOME));
    }
}
