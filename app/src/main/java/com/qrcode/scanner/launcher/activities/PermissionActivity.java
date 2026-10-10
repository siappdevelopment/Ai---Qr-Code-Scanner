package com.qrcode.scanner.launcher.activities;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.ScreenFlowNavigation;
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;

/**
 * Source permission screen. Order is phone, then camera, then overlay settings.
 * A denial continues to the next step. Overlay is granted only when Settings.canDrawOverlays is true.
 */
public class PermissionActivity extends AppCompatActivity {
    private AppCompatTextView btnAllowPermission;

    private static final String KEY_PERMISSION_FLOW_ACTIVE = "key_permission_flow_active";
    private static final String KEY_PHONE_STEP_COMPLETED = "key_phone_step_completed";
    private static final String KEY_CAMERA_STEP_COMPLETED = "key_camera_step_completed";
    private static final String KEY_WAITING_OVERLAY_SETTINGS = "key_waiting_overlay_settings";
    private static final String KEY_PAUSED_FOR_OVERLAY_SETTINGS = "key_paused_for_overlay_settings";
    private static final long OVERLAY_PERMISSION_POLL_INTERVAL_MS = 400L;
    private final Handler overlayPermissionHandler = new Handler(Looper.getMainLooper());
    private boolean permissionFlowActive;
    private boolean phoneStepCompleted;
    private boolean cameraStepCompleted;
    private boolean waitingForOverlaySettings;
    private boolean pausedForOverlaySettings;
    private boolean hasNavigated;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            phoneStepCompleted = savedInstanceState.getBoolean(KEY_PHONE_STEP_COMPLETED, false);
            cameraStepCompleted = savedInstanceState.getBoolean(KEY_CAMERA_STEP_COMPLETED, false);
            waitingForOverlaySettings = savedInstanceState.getBoolean(KEY_WAITING_OVERLAY_SETTINGS, false);
            pausedForOverlaySettings = savedInstanceState.getBoolean(KEY_PAUSED_FOR_OVERLAY_SETTINGS, false);
        }

        findIDs();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (waitingForOverlaySettings) {
            pausedForOverlaySettings = true;
            startOverlayPermissionMonitor();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        stopOverlayPermissionMonitor();
        if (!waitingForOverlaySettings || !pausedForOverlaySettings || hasNavigated) {
            return;
        }
        if (AppUtils.hasOverlayPermission(this)) {
            completeOverlayGrantAndContinue();
        }
    }

    @Override
    protected void onDestroy() {
        stopOverlayPermissionMonitor();
        super.onDestroy();
    }

    private final Runnable overlayPermissionMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (hasNavigated || !waitingForOverlaySettings || isFinishing()) {
                stopOverlayPermissionMonitor();
                return;
            }
            if (AppUtils.hasOverlayPermission(PermissionActivity.this)) {
                handleOverlayPermissionGrantedFromBackground();
                return;
            }
            overlayPermissionHandler.postDelayed(this, OVERLAY_PERMISSION_POLL_INTERVAL_MS);
        }
    };

    private void startOverlayPermissionMonitor() {
        stopOverlayPermissionMonitor();
        overlayPermissionHandler.postDelayed(overlayPermissionMonitorRunnable, OVERLAY_PERMISSION_POLL_INTERVAL_MS);
    }

    private void stopOverlayPermissionMonitor() {
        overlayPermissionHandler.removeCallbacks(overlayPermissionMonitorRunnable);
    }

    private void handleOverlayPermissionGrantedFromBackground() {
        if (hasNavigated || !waitingForOverlaySettings) {
            stopOverlayPermissionMonitor();
            return;
        }
        stopOverlayPermissionMonitor();
        waitingForOverlaySettings = false;
        pausedForOverlaySettings = false;
        try {
            Intent returnIntent = new Intent(this, PermissionActivity.class);
            returnIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(returnIntent);
        } catch (Exception ignored) {
        }
        finishOverlayStepAndNavigate();
    }

    private void completeOverlayGrantAndContinue() {
        if (hasNavigated) {
            return;
        }
        waitingForOverlaySettings = false;
        pausedForOverlaySettings = false;
        stopOverlayPermissionMonitor();
        finishOverlayStepAndNavigate();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_PERMISSION_FLOW_ACTIVE, permissionFlowActive);
        outState.putBoolean(KEY_PHONE_STEP_COMPLETED, phoneStepCompleted);
        outState.putBoolean(KEY_CAMERA_STEP_COMPLETED, cameraStepCompleted);
        outState.putBoolean(KEY_WAITING_OVERLAY_SETTINGS, waitingForOverlaySettings);
        outState.putBoolean(KEY_PAUSED_FOR_OVERLAY_SETTINGS, pausedForOverlaySettings);
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
        if (permissionFlowActive && !hasNavigated) {
            if (phoneStepCompleted && cameraStepCompleted) {
                scheduleOverlayPermissionRequest();
                return;
            }
            if (phoneStepCompleted) {
                scheduleCameraPermissionRequest();
                return;
            }
        }

        if (!permissionFlowActive && areAllRuntimePermissionsGranted() && isNotificationStepSatisfied()
                && (AppUtils.hasOverlayPermission(this) || AppUtils.isDefaultHomeApp(this))) {
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

    private void scheduleCameraPermissionRequest() {
        View decorView = getWindow() != null ? getWindow().getDecorView() : null;
        if (decorView != null) {
            decorView.post(this::requestCameraPermission);
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Notifications: asked here when Splash did not get them (Android 13+). Runs between Camera and Overlay.
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
            scheduleOverlayPermissionRequest();
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (hasNavigated) {
            return;
        }
        scheduleOverlayPermissionRequest();
    });

    private void scheduleOverlayPermissionRequest() {
        View decorView = getWindow() != null ? getWindow().getDecorView() : null;
        if (decorView != null) {
            decorView.post(this::requestOverlayPermission);
        } else {
            requestOverlayPermission();
        }
    }

    private boolean areAllRuntimePermissionsGranted() {
        return AppUtils.hasPhoneStatePermission(this) && AppUtils.hasCameraPermission(this);
    }

    private void completePermissionScreenAndNavigate() {
        if (AppUtils.hasPhoneStatePermission(this)) {
            AppUtils.trackPhoneStatePermissionGrantedOnce(this);
        }
        if (AppUtils.hasCameraPermission(this)) {
            AppUtils.trackCameraPermissionGrantedOnce(this);
        }
        if (AppUtils.hasOverlayPermission(this)) {
            AppUtils.trackOverlayPermissionGrantedOnce(this);
        }
        navigateToNextScreen();
    }

    private void handleAllowPermissionClick() {
        if (hasNavigated || permissionFlowActive) {
            return;
        }
        startPermissionFlow();
    }

    private void startPermissionFlow() {
        if (hasNavigated) {
            return;
        }
        permissionFlowActive = true;
        phoneStepCompleted = false;
        cameraStepCompleted = false;
        requestPhonePermission();
    }

    private void requestPhonePermission() {
        if (hasNavigated) {
            return;
        }
        if (AppUtils.hasPhoneStatePermission(this)) {
            phoneStepCompleted = true;
            scheduleCameraPermissionRequest();
            return;
        }
        phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE);
    }

    private final ActivityResultLauncher<String> phonePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (hasNavigated) {
            return;
        }
        phoneStepCompleted = true;
        if (granted) {
            AppUtils.trackPhoneStatePermissionGrantedOnce(this);
        }
        scheduleCameraPermissionRequest();
    });

    private void requestCameraPermission() {
        if (hasNavigated || !phoneStepCompleted || cameraStepCompleted) {
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

    private void requestOverlayPermission() {
        if (hasNavigated || !phoneStepCompleted || !cameraStepCompleted) {
            return;
        }
        // A default launcher can already start screens from the background, so skip the overlay step.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || AppUtils.hasOverlayPermission(this) || AppUtils.isDefaultHomeApp(this)) {
            finishOverlayStepAndNavigate();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        waitingForOverlaySettings = true;
        try {
            overlaySettingsLauncher.launch(intent);
        } catch (Exception ignored) {
            waitingForOverlaySettings = false;
            finishOverlayStepAndNavigate();
        }
    }

    private final ActivityResultLauncher<Intent> overlaySettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (hasNavigated) {
                    return;
                }
                stopOverlayPermissionMonitor();
                waitingForOverlaySettings = false;
                pausedForOverlaySettings = false;
                finishOverlayStepAndNavigate();
            });

    private void finishOverlayStepAndNavigate() {
        if (hasNavigated) {
            return;
        }
        completePermissionScreenAndNavigate();
    }

    private void navigateToNextScreen() {
        if (hasNavigated || isFinishing()) {
            return;
        }
        hasNavigated = true;
        permissionFlowActive = false;
        phoneStepCompleted = false;
        cameraStepCompleted = false;
        waitingForOverlaySettings = false;
        pausedForOverlaySettings = false;
        stopOverlayPermissionMonitor();
        ScreenFlowNavigation.continueAfter(this, ScreenFlowConfig.SCREEN_PERMISSION);
    }
}
