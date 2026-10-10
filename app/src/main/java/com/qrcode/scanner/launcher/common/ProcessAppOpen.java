package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.qrcode.scanner.MainActivity;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity;
import com.qrcode.scanner.launcher.adapters.LauncherPagerAdapter;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

/**
 * Shows an app-open ad when the app returns to the foreground, gated by App_Open_Ad_Show,
 * App_Open_Show_Per_Day (via {@link AdPlacement#canShowAppOpenAd}) and App_Open_Id.
 */
public final class ProcessAppOpen implements DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    private static final String TAG = "ProcessAppOpen";
    private static final long AD_EXPIRY_MS = 4L * 60L * 60L * 1000L;
    private static final long APP_OPEN_DIALOG_DELAY_MS = 1500L;

    private final Application application;
    @Nullable
    private Activity currentActivity;
    @Nullable
    private AppOpenAd appOpenAd;
    private boolean isLoadingAd;
    private boolean isShowingAd;
    private long loadTime;
    private static boolean appInForeground;

    public ProcessAppOpen(@NonNull Application application) {
        this.application = application;
    }

    public void register() {
        application.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    private void fetchAd() {
        if (shouldSkipAppOpenAd() || isLoadingAd || isAdAvailable()) {
            Log.d(TAG, "fetch skipped: skip=" + shouldSkipAppOpenAd() + " loading=" + isLoadingAd + " available=" + isAdAvailable());
            return;
        }
        String unitId = AdPlacement.getAppOpenId();
        if (!AdPlacement.getAppOpenAdShow() || unitId == null || unitId.isEmpty() || !AdPlacement.canRequestAds(application)) {
            Log.d(TAG, "fetch blocked: show=" + AdPlacement.getAppOpenAdShow() + " unitId=" + unitId + " canRequest=" + AdPlacement.canRequestAds(application));
            return;
        }
        isLoadingAd = true;
        Log.d(TAG, "fetch started");
        AppOpenAd.load(application, unitId, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                appOpenAd = ad;
                isLoadingAd = false;
                loadTime = System.currentTimeMillis();
                Log.d(TAG, "fetch loaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                isLoadingAd = false;
                Log.d(TAG, "fetch failed: " + loadAdError.getMessage());
            }
        });
    }

    private boolean isAdAvailable() {
        return appOpenAd != null && AdPlacement.getAppOpenAdShow() && AdPlacement.canShowAppOpenAd(application) && (System.currentTimeMillis() - loadTime) < AD_EXPIRY_MS;
    }

    /** The resume ad belongs to the Right-Side page only; every other screen is skipped. */
    private boolean shouldSkipAppOpenAd() {
        return !isRightSidePageOpen(currentActivity);
    }

    /**
     * The Right-Side (QR app) UI: the launcher's right page, or, when the app is not the default Home,
     * the same QR UI hosted by MainActivity and its QR activities (History, Create, Scan, ...).
     */
    private static boolean isRightSidePageOpen(@Nullable Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        if (activity instanceof LauncherHomeActivity) {
            return ((LauncherHomeActivity) activity).getLauncherCurrentItem() == LauncherPagerAdapter.PAGE_RIGHT;
        }
        return activity instanceof MainActivity || activity.getClass().getName().startsWith("com.qrcode.scanner.ui.");
    }

    private void clearLoadedAd() {
        appOpenAd = null;
        isLoadingAd = false;
    }

    private void showAdIfAvailable(@NonNull Activity activity) {
        if (shouldSkipAppOpenAd() || isShowingAd) {
            Log.d(TAG, "show skipped: skip=" + shouldSkipAppOpenAd() + " showing=" + isShowingAd);
            return;
        }
        if (!AdPlacement.getAppOpenAdShow() || !AdPlacement.canShowAppOpenAd(application)) {
            Log.d(TAG, "show blocked: show=" + AdPlacement.getAppOpenAdShow() + " canShow(perDay)=" + AdPlacement.canShowAppOpenAd(application));
            clearLoadedAd();
            return;
        }
        if (!isAdAvailable()) {
            Log.d(TAG, "show: ad not ready yet, fetching");
            fetchAd();
            return;
        }
        Log.d(TAG, "showing resume app open ad");
        final AppOpenAd adToShow = appOpenAd;
        adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                clearLoadedAd();
                isShowingAd = false;
                fetchAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                clearLoadedAd();
                isShowingAd = false;
                fetchAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                AdPlacement.setAppOpenLastShowTime(application, System.currentTimeMillis());
            }
        });
        isShowingAd = true;
        adToShow.show(activity);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        appInForeground = true;
        // The user is in the app: a stale "make this your default launcher" reminder is cleared.
        DefaultLauncherReminder.cancelIfDefault(application);
        DefaultLauncherReminder.dismissNotification(application);
        Activity activity = currentActivity;
        Log.d(TAG, "process onStart activity=" + (activity == null ? "null" : activity.getClass().getSimpleName()) + " rightPage=" + isRightSidePageOpen(activity));
        if (activity == null || shouldSkipAppOpenAd()) {
            return;
        }
        if (RemoteConfigValues.getAppOpenDialogShow() && isAdAvailable()) {
            showAppOpenDialog(activity, () -> showAdIfAvailable(activity));
        } else {
            showAdIfAvailable(activity);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        appInForeground = false;
        Log.d(TAG, "process onStop activity=" + (currentActivity == null ? "null" : currentActivity.getClass().getSimpleName()) + " rightPage=" + isRightSidePageOpen(currentActivity));
        // The user left from the Right-Side page (Share, Rate Us, another app): have the ad ready for the return.
        fetchAd();
    }

    private void showAppOpenDialog(Activity activity, Runnable onDismiss) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || !isAdAvailable()) {
            onDismiss.run();
            return;
        }
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
        }
        dialog.setContentView(R.layout.dialog_loading_ads);
        dialog.setCancelable(false);
        dialog.show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing() && !activity.isFinishing() && !activity.isDestroyed()) {
                dialog.dismiss();
            }
            onDismiss.run();
        }, APP_OPEN_DIALOG_DELAY_MS);
    }

    public static boolean isAppInForeground() {
        return appInForeground;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!isShowingAd) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}
