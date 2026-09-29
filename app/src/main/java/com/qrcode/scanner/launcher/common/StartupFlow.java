package com.qrcode.scanner.launcher.common;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.qrcode.scanner.R;
import com.qrcode.scanner.launcher.helpers.VPNHelper;
import com.qrcode.scanner.launcher.remote.RemoteConfigHelper;

import java.lang.ref.WeakReference;

/**
 * Source splash sequence behind the existing Compose splash.
 * VPN stays up until Recheck finds the VPN gone. Ad failure, an empty unit id, or a disabled placement continues.
 */
public final class StartupFlow {
    private static final int PERMISSION_REQUEST_CODE = 101;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static WeakReference<Activity> boundActivity = new WeakReference<>(null);
    private static WeakReference<View> adRoot = new WeakReference<>(null);
    private static Runnable onFinished;
    private static Dialog dialogVPN;
    private static boolean splashFlowStarted;
    private static boolean navigationDelivered;
    private static int generation;

    private StartupFlow() {
    }

    public static boolean shouldShowSplash(Intent intent) {
        if (intent == null) {
            return false;
        }
        if (intent.getBooleanExtra(ScreenFlowNavigation.EXTRA_SKIP_STARTUP_SPLASH, false)) {
            return false;
        }
        if (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            return true;
        }
        if (intent.hasExtra(WidgetNavigation.EXTRA_TARGET_SCREEN)) {
            return true;
        }
        return intent.getBooleanExtra(com.qrcode.scanner.launcher.helpers.ReminderAlarmHelper.EXTRA_FROM_REMINDER_NOTIFICATION, false);
    }

    public static void begin(Activity activity, View root, Runnable finished) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || root == null) {
            return;
        }
        Activity previous = boundActivity.get();
        if (previous != null && previous != activity) {
            resetLockedState();
        }
        boundActivity = new WeakReference<>(activity);
        adRoot = new WeakReference<>(root);
        onFinished = finished;
        if (splashFlowStarted) {
            return;
        }
        if (VPNHelper.isVPNActive(activity)) {
            showVpnDialog(activity);
            return;
        }
        WidgetNavigation.captureTargetScreen(activity, activity.getIntent());
        startSplashFlow(activity);
    }

    public static void onRequestPermissionsResult(Activity activity, int requestCode, int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE || activity == null || activity != boundActivity.get()) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            TrackOnce.trackScreenOnce(activity, "POST_NOTIFICATIONS_GRANTED");
        }
        goNextScreen(activity);
    }

    public static void onHostDestroy(Activity activity) {
        if (activity == null || activity != boundActivity.get()) {
            return;
        }
        dismissVpnDialog();
        resetLockedState();
        boundActivity = new WeakReference<>(null);
        adRoot = new WeakReference<>(null);
        onFinished = null;
    }

    private static void showVpnDialog(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (dialogVPN != null && dialogVPN.isShowing()) {
            return;
        }
        Context themed = new ContextThemeWrapper(activity, R.style.Theme_LauncherSettings);
        dialogVPN = new Dialog(themed);
        dialogVPN.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogVPN.setContentView(R.layout.dialog_vpn_detect);
        dialogVPN.setCancelable(false);
        if (dialogVPN.getWindow() != null) {
            dialogVPN.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialogVPN.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        AppCompatTextView btnReCheck = dialogVPN.findViewById(R.id.btnReCheck);
        btnReCheck.setOnClickListener(v -> handleVpnRecheck(activity));
        AppUtils.applyDialogPadding(activity, dialogVPN);
        dialogVPN.show();
    }

    private static void handleVpnRecheck(Activity activity) {
        if (activity != boundActivity.get() || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (VPNHelper.isVPNActive(activity)) {
            return;
        }
        dismissVpnDialog();
        startSplashFlow(activity);
    }

    private static void startSplashFlow(Activity activity) {
        if (splashFlowStarted || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        splashFlowStarted = true;
        try {
            InstallReferrerStartup.start(activity);
        } catch (Exception ignored) {
        }
        int token = generation;
        RemoteConfigHelper.fetchRemoteConfig(activity, success -> {
            if (token != generation || activity != boundActivity.get() || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            showSplashAd(activity);
            AdPlacement.saveInstallDate(activity);
            checkNotificationPermission(activity);
        });
    }

    private static void showSplashAd(Activity activity) {
        View root = adRoot.get();
        if (root == null) {
            return;
        }
        RelativeLayout rlAdView = root.findViewById(R.id.rlAdView);
        RelativeLayout rlBannerAdView = root.findViewById(R.id.rlBannerAdView);
        ShimmerFrameLayout slBannerShimmer = root.findViewById(R.id.slBannerShimmer);
        LinearLayout llBannerAd = root.findViewById(R.id.llBannerAd);
        RelativeLayout rlNativeAdView = root.findViewById(R.id.rlNativeAdView);
        ShimmerFrameLayout slNativeShimmer = root.findViewById(R.id.slNativeShimmer);
        FrameLayout flNativeAd = root.findViewById(R.id.flNativeAd);
        if (rlAdView == null) {
            return;
        }
        if (!AdPlacement.isNetworkAvailable(activity) || !AdPlacement.getSplashAdShow()) {
            rlAdView.setVisibility(GONE);
            return;
        }
        rlAdView.setVisibility(VISIBLE);
        if ("banner".equalsIgnoreCase(AdPlacement.getSplashAdType())) {
            if (rlBannerAdView != null) {
                rlBannerAdView.setVisibility(VISIBLE);
            }
            if (rlNativeAdView != null) {
                rlNativeAdView.setVisibility(GONE);
            }
            AdPlacement.loadBannerAd(activity, AdPlacement.getSplashBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd);
        } else {
            if (rlBannerAdView != null) {
                rlBannerAdView.setVisibility(GONE);
            }
            if (rlNativeAdView != null) {
                rlNativeAdView.setVisibility(VISIBLE);
            }
            AdPlacement.loadNativeAd(activity, AdPlacement.getSplashNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "small");
        }
    }

    private static void checkNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            return;
        }
        goNextScreen(activity);
    }

    private static void goNextScreen(Activity activity) {
        int token = generation;
        long delay = AdPlacement.getSplashDuration() * 1000L;
        HANDLER.postDelayed(() -> {
            if (token != generation || activity != boundActivity.get() || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            showAfterSplashAd(activity);
        }, delay);
    }

    private static void showAfterSplashAd(Activity activity) {
        View root = adRoot.get();
        LinearLayout llContainAds = root == null ? null : root.findViewById(R.id.llContainAds);
        if (!AdPlacement.getAfterSplashAdShow()) {
            if (llContainAds != null) {
                llContainAds.setVisibility(GONE);
            }
            deliverFinished(generation);
            return;
        }
        if (llContainAds != null) {
            llContainAds.setVisibility(VISIBLE);
        }
        int token = generation;
        if ("inter".equalsIgnoreCase(AdPlacement.getAfterSplashAdType())) {
            AdPlacement.loadAfterSplashInterstitialAd(activity, AdPlacement.getAfterSplashInterstitialId(), () -> deliverFinished(token));
        } else {
            AdPlacement.loadAfterSplashAppOpenAd(activity, AdPlacement.getAppOpenId(), () -> deliverFinished(token));
        }
    }

    private static void deliverFinished(int token) {
        if (token != generation || navigationDelivered) {
            return;
        }
        navigationDelivered = true;
        Runnable finished = onFinished;
        if (finished != null) {
            finished.run();
        }
    }

    private static void dismissVpnDialog() {
        if (dialogVPN != null && dialogVPN.isShowing()) {
            dialogVPN.dismiss();
        }
        dialogVPN = null;
    }

    private static void resetLockedState() {
        generation++;
        splashFlowStarted = false;
        navigationDelivered = false;
    }
}
