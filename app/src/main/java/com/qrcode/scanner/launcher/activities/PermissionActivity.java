package com.qrcode.scanner.launcher.activities;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.qrcode.scanner.spapp.R;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.ScreenFlowNavigation;
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;

/**
 * Permission screen. Order is camera, then notifications (Android 13+). A denial continues to the next step.
 */
public class PermissionActivity extends AppCompatActivity {
    private AppCompatTextView btnAllowPermission;

    private static final String KEY_PERMISSION_FLOW_ACTIVE = "key_permission_flow_active";
    private static final String KEY_CAMERA_STEP_COMPLETED = "key_camera_step_completed";
    private boolean permissionFlowActive;
    private boolean cameraStepCompleted;
    private boolean hasNavigated;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            com.qrcode.scanner.launcher.common.AnalyticsEvents.log(this, com.qrcode.scanner.launcher.common.AnalyticsEvents.PERMISSION_VIEW);
        }
        setContentView(R.layout.activity_permission);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        if (savedInstanceState != null) {
            permissionFlowActive = savedInstanceState.getBoolean(KEY_PERMISSION_FLOW_ACTIVE, false);
            cameraStepCompleted = savedInstanceState.getBoolean(KEY_CAMERA_STEP_COMPLETED, false);
        }

        findIDs();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_PERMISSION_FLOW_ACTIVE, permissionFlowActive);
        outState.putBoolean(KEY_CAMERA_STEP_COMPLETED, cameraStepCompleted);
    }

    private void findIDs() {
        btnAllowPermission = findViewById(R.id.btnAllowPermission);

        RelativeLayout rlAdView = findViewById(R.id.rlAdView);
        RelativeLayout rlBannerAdView = findViewById(R.id.rlBannerAdView);
        ShimmerFrameLayout slBannerShimmer = findViewById(R.id.slBannerShimmer);
        LinearLayout llBannerAd = findViewById(R.id.llBannerAd);
        RelativeLayout rlNativeAdView = findViewById(R.id.rlNativeAdView);
        ShimmerFrameLayout slNativeShimmer = findViewById(R.id.slNativeShimmer);
        FrameLayout flNativeAd = findViewById(R.id.flNativeAd);
        AdPlacement.showSlot(this, AdPlacement.getPermissionDefaultAdShow(), AdPlacement.getPermissionDefaultAdType(), AdPlacement.getPermissionDefaultBannerId(), AdPlacement.getPermissionDefaultNativeId(), "small", rlAdView, rlBannerAdView, slBannerShimmer, llBannerAd, rlNativeAdView, slNativeShimmer, flNativeAd, false, AdPlacement.onboardingNativeColor(this));

        initialClicks();
    }

    private void initialClicks() {
        if (permissionFlowActive && !hasNavigated && cameraStepCompleted) {
            scheduleNotificationPermissionRequest();
            return;
        }

        if (!permissionFlowActive && AppUtils.hasCameraPermission(this) && isNotificationStepSatisfied()) {
            completePermissionScreenAndNavigate();
        }

        btnAllowPermission.setOnClickListener(view -> handleAllowPermissionClick());
        // Back follows the Allow flow (its own guards stop a second request); it never returns to the previous screen.
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleAllowPermissionClick();
            }
        });
    }

    /**
     * Notifications: asked here when Splash did not get them (Android 13+). Runs after Camera.
     * A denial (also a permanent one, which returns at once) just continues; nothing is marked granted.
     */
    private boolean isNotificationStepSatisfied() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || AdPlacement.isNotificationGranted(this);
    }

    private void scheduleNotificationPermissionRequest() {
        View decorView = getWindow() != null ? getWindow().getDecorView() : null;
        if (decorView != null) {
            decorView.post(this::requestNotificationPermission);
        } else {
            requestNotificationPermission();
        }
    }

    private void requestNotificationPermission() {
        if (hasNavigated || isFinishing()) {
            return;
        }
        if (isNotificationStepSatisfied()) {
            completePermissionScreenAndNavigate();
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (hasNavigated) {
            return;
        }
        completePermissionScreenAndNavigate();
    });

    private void completePermissionScreenAndNavigate() {
        if (AppUtils.hasCameraPermission(this)) {
            AppUtils.trackCameraPermissionGrantedOnce(this);
        }
        navigateToNextScreen();
    }

    private void handleAllowPermissionClick() {
        if (hasNavigated || permissionFlowActive) {
            return;
        }
        permissionFlowActive = true;
        cameraStepCompleted = false;
        requestCameraPermission();
    }

    private void requestCameraPermission() {
        if (hasNavigated || cameraStepCompleted) {
            return;
        }
        if (AppUtils.hasCameraPermission(this)) {
            cameraStepCompleted = true;
            AppUtils.trackCameraPermissionGrantedOnce(this);
            scheduleNotificationPermissionRequest();
            return;
        }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (hasNavigated) {
            return;
        }
        cameraStepCompleted = true;
        if (granted) {
            AppUtils.trackCameraPermissionGrantedOnce(this);
        }
        scheduleNotificationPermissionRequest();
    });

    private void navigateToNextScreen() {
        if (hasNavigated || isFinishing()) {
            return;
        }
        hasNavigated = true;
        permissionFlowActive = false;
        cameraStepCompleted = false;
        ScreenFlowNavigation.continueAfter(this, ScreenFlowConfig.SCREEN_PERMISSION);
    }
}
