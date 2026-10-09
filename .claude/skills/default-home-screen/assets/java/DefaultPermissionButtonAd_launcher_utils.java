package com.calendar.eventplanner.tasktracker.ADS.launcher.utils;

import com.calendar.eventplanner.tasktracker.ADS.AppAnalyticsEvents;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.calendar.eventplanner.tasktracker.ADS.advertisement.ADSAppManage;
import com.calendar.eventplanner.tasktracker.ADS.advertisement.ADSMainClass;
import com.calendar.eventplanner.tasktracker.ADS.advertisement.ADSUtilitis;
import com.calendar.eventplanner.tasktracker.ADS.advertisement.AdPlacement;

/**
 * AppOpen / Interstitial shown after Set as Default.
 * Always preload; never show a loading dialog.
 * When Ad_Priority=QUIZ, shows Quiz ads instead of Google.
 */
@SuppressWarnings("all")
public class DefaultPermissionButtonAd {

    private static OnCompeteAds onCompleteAdCallBack;
    private static InterstitialAd interstitialAd;
    private static AppOpenAd appOpenAd;
    private static boolean isLoading;
    private static boolean pendingShow;
    private static Activity pendingShowActivity;
    private static boolean shownForCurrentSetup;

    public static boolean isReady() {
        if (AdPlacement.shouldUseQuizPriority()) {
            return true;
        }
        return isAppOpenType() ? appOpenAd != null : interstitialAd != null;
    }

    /**
     * Whether the Set-as-Default button ad should load/show.
     * AppOpen: default_permission_button_ads_show.
     * Inter: also requires other_screen.inter_ads_show.
     */
    public static boolean shouldShow() {
        if (ADSMainClass.getAds_Free() || !ADSMainClass.getDefaultPermissionButtonAdsShow()) {
            return false;
        }
        return isAppOpenType() || ADSMainClass.getInterAdsShow();
    }

    public static void resetShownFlag() {
        shownForCurrentSetup = false;
    }

    public static void preload(Activity context) {
        if (context == null || !shouldShow()) {
            return;
        }
        // Quiz priority does not need Google preload.
        if (AdPlacement.shouldUseQuizPriority()) {
            return;
        }
        if (isReady() || isLoading) {
            return;
        }

        Context appContext = context.getApplicationContext();
        if (isAppOpenType()) {
            loadAppOpen(appContext);
        } else {
            loadInterstitial(appContext);
        }
    }

    public static void show(Activity context, OnCompeteAds onFinishAd) {
        onCompleteAdCallBack = onFinishAd;

        if (context == null || context.isFinishing() || context.isDestroyed()
                || !shouldShow()) {
            if (!isAppOpenType() && !ADSMainClass.getInterAdsShow()
                    && ADSMainClass.getDefaultPermissionButtonAdsShow()
                    && !ADSMainClass.getAds_Free()) {
                android.util.Log.d("ADS_INTER", "DefaultPermissionButtonAd.show skip"
                        + " | inter_ads_show=false | type="
                        + ADSMainClass.getDefaultPermissionButtonAdsType());
            }
            finishCallback(true);
            return;
        }

        if (shownForCurrentSetup) {
            finishCallback(true);
            return;
        }

        ADSAppManage.isAppOpenBlocked = true;
        runWhenResumed(context, () -> {
            android.util.Log.d("ADS_INTER", "DefaultPermissionButtonAd.show"
                    + " | screen=" + context.getClass().getSimpleName()
                    + " | type=" + ADSMainClass.getDefaultPermissionButtonAdsType()
                    + " | inter_ads_show=" + ADSMainClass.getInterAdsShow()
                    + " | Ad_Priority=[" + AdPlacement.getAdPriority() + "]"
                    + " | interId=[" + ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME) + "]");
            if (AdPlacement.shouldUseQuizPriority()) {
                android.util.Log.w("ADS_INTER", "DefaultPermission SHOW QUIZ | cause=Ad_Priority=QUIZ");
                showQuizAd(context);
                return;
            }
            showWhenReady(context);
        });
    }

    private static void showQuizAd(Activity context) {
        if (context == null || context.isFinishing() || context.isDestroyed()) {
            finishCallback(false);
            return;
        }
        android.util.Log.w("ADS_INTER", "DefaultPermission showQuizAd | screen="
                + context.getClass().getSimpleName());
        Runnable onQuizComplete = () -> finishCallback(true);
        boolean allowInter = ADSMainClass.getInterAdsShow();
        boolean shown;
        if (isAppOpenType()) {
            shown = AdPlacement.showQuizAppOpenAd(context, onQuizComplete)
                    || (allowInter && AdPlacement.showQuizInterstitialAd(context, onQuizComplete));
        } else if (!allowInter) {
            finishCallback(false);
            return;
        } else {
            shown = AdPlacement.showQuizInterstitialAd(context, onQuizComplete)
                    || AdPlacement.showQuizAppOpenAd(context, onQuizComplete);
        }
        if (shown) {
            shownForCurrentSetup = true;
            ADSUtilitis.trackScreen(context, AppAnalyticsEvents.NATIVE_IMPRESSION);
        } else {
            finishCallback(false);
        }
    }

    private static boolean tryShowQuizFallback(Activity context) {
        if (context == null || context.isFinishing() || context.isDestroyed()
                || !AdPlacement.getGoogleAdFailedShowQuiz()) {
            return false;
        }
        Runnable onQuizComplete = () -> finishCallback(true);
        boolean allowInter = ADSMainClass.getInterAdsShow();
        boolean shown;
        if (isAppOpenType()) {
            shown = AdPlacement.tryShowQuizAppOpenOnGoogleFail(context, onQuizComplete)
                    || (allowInter && AdPlacement.tryShowQuizInterstitialOnGoogleFail(context, onQuizComplete));
        } else if (!allowInter) {
            return false;
        } else {
            shown = AdPlacement.tryShowQuizInterstitialOnGoogleFail(context, onQuizComplete)
                    || AdPlacement.tryShowQuizAppOpenOnGoogleFail(context, onQuizComplete);
        }
        if (shown) {
            shownForCurrentSetup = true;
            ADSUtilitis.trackScreen(context, AppAnalyticsEvents.NATIVE_IMPRESSION);
        }
        return shown;
    }

    private static void runWhenResumed(Activity context, Runnable action) {
        if (context instanceof LifecycleOwner) {
            LifecycleOwner owner = (LifecycleOwner) context;
            if (owner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                action.run();
                return;
            }
            owner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onResume(@NonNull LifecycleOwner o) {
                    o.getLifecycle().removeObserver(this);
                    new Handler(Looper.getMainLooper()).post(action);
                }
            });
            return;
        }
        context.getWindow().getDecorView().post(action);
    }

    private static boolean isAppOpenType() {
        String type = ADSMainClass.getDefaultPermissionButtonAdsType();
        if (type == null || type.trim().isEmpty()) {
            return true;
        }
        String t = type.trim().toLowerCase();
        if (t.contains("inter")) {
            return false;
        }
        return t.contains("appopen") || t.contains("app_open") || t.equals("app open");
    }

    private static void showWhenReady(Activity context) {
        if (context.isFinishing() || context.isDestroyed()) {
            finishCallback(true);
            return;
        }
        if (isAppOpenType()) {
            if (appOpenAd != null) {
                presentAppOpen(context);
                return;
            }
        } else if (interstitialAd != null) {
            presentInterstitial(context);
            return;
        }

        // Ad not ready yet — wait for preload silently. Never open loading dialog.
        pendingShow = true;
        pendingShowActivity = context;
        if (!isLoading) {
            Context appContext = context.getApplicationContext();
            if (isAppOpenType()) {
                loadAppOpen(appContext);
            } else {
                loadInterstitial(appContext);
            }
        }
        // If load never completes, do not leave the click hanging forever.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (pendingShow && pendingShowActivity == context) {
                android.util.Log.w("ADS_INTER", "DefaultPermission pendingShow timeout — fail");
                failPendingShow();
            }
        }, 12_000L);
    }

    private static void loadAppOpen(Context context) {
        String appOpenId = ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID);
        if (appOpenId == null || appOpenId.trim().isEmpty()) {
            failPendingShow();
            return;
        }

        isLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        AppOpenAd.load(context, appOpenId.trim(), adRequest,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        isLoading = false;
                        ad.setOnPaidEventListener(adValue -> ADSUtilitis.logAdRevenue(context, adValue));
                        appOpenAd = ad;
                        ADSUtilitis.trackScreen(context, AppAnalyticsEvents.APP_OPEN_AD_LOAD);
                        maybePresentPending(true);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isLoading = false;
                        appOpenAd = null;
                        ADSUtilitis.trackScreen(context, AppAnalyticsEvents.APP_OPEN_AD_FAIL);
                        failPendingShow();
                    }
                });
    }

    private static void loadInterstitial(Context context) {
        if (!ADSMainClass.getInterAdsShow()) {
            failPendingShow();
            return;
        }
        String interId = ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME);
        if (interId == null || interId.trim().isEmpty()) {
            failPendingShow();
            return;
        }

        isLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, interId.trim(), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        isLoading = false;
                        ad.setOnPaidEventListener(adValue -> ADSUtilitis.logAdRevenue(context, adValue));
                        interstitialAd = ad;
                        ADSUtilitis.trackScreen(context, AppAnalyticsEvents.DEFAULT_PERMISSION_INTERSTITIAL_LOAD);
                        maybePresentPending(false);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isLoading = false;
                        interstitialAd = null;
                        ADSUtilitis.trackScreen(context, AppAnalyticsEvents.DEFAULT_PERMISSION_INTERSTITIAL_FAIL);
                        failPendingShow();
                    }
                });
    }

    private static void failPendingShow() {
        if (!pendingShow) {
            return;
        }
        Activity showActivity = pendingShowActivity;
        pendingShow = false;
        pendingShowActivity = null;
        if (showActivity != null && !showActivity.isFinishing() && !showActivity.isDestroyed()
                && tryShowQuizFallback(showActivity)) {
            return;
        }
        finishCallback(false);
    }

    private static void maybePresentPending(boolean appOpen) {
        if (!pendingShow) {
            return;
        }
        Activity showActivity = pendingShowActivity;
        pendingShow = false;
        pendingShowActivity = null;
        if (showActivity == null || showActivity.isFinishing() || showActivity.isDestroyed()) {
            if (onCompleteAdCallBack != null) {
                finishCallback(false);
            }
            return;
        }
        runWhenResumed(showActivity, () -> {
            if (showActivity.isFinishing() || showActivity.isDestroyed()) {
                if (onCompleteAdCallBack != null) {
                    finishCallback(false);
                }
                return;
            }
            if (appOpen) {
                presentAppOpen(showActivity);
            } else {
                presentInterstitial(showActivity);
            }
        });
    }

    private static void presentAppOpen(Activity context) {
        if (appOpenAd == null) {
            if (!tryShowQuizFallback(context)) {
                finishCallback(false);
            }
            return;
        }
        if (context.isFinishing() || context.isDestroyed()) {
            finishCallback(false);
            return;
        }

        AppOpenAd ad = appOpenAd;
        appOpenAd = null;
        shownForCurrentSetup = true;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                ADSUtilitis.trackScreen(context, AppAnalyticsEvents.APP_OPEN_AD_IMPRESSION);
                finishCallback(true);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                ADSUtilitis.trackScreen(context, AppAnalyticsEvents.APP_OPEN_AD_FAIL);
                shownForCurrentSetup = false;
                if (!tryShowQuizFallback(context)) {
                    finishCallback(false);
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                ADSUtilitis.trackScreen(context, AppAnalyticsEvents.APP_OPEN_AD_IMPRESSION);
            }
        });
        try {
            ad.show(context);
        } catch (Exception e) {
            shownForCurrentSetup = false;
            if (!tryShowQuizFallback(context)) {
                finishCallback(false);
            }
        }
    }

    private static void presentInterstitial(Activity context) {
        if (!ADSMainClass.getInterAdsShow()) {
            interstitialAd = null;
            finishCallback(false);
            return;
        }
        if (interstitialAd == null) {
            if (!tryShowQuizFallback(context)) {
                finishCallback(false);
            }
            return;
        }
        if (context.isFinishing() || context.isDestroyed()) {
            finishCallback(false);
            return;
        }

        InterstitialAd ad = interstitialAd;
        interstitialAd = null;
        shownForCurrentSetup = true;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                ADSUtilitis.trackScreen(context, AppAnalyticsEvents.DEFAULT_PERMISSION_INTERSTITIAL_IMPRESSION);
                finishCallback(true);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                ADSUtilitis.trackScreen(context, AppAnalyticsEvents.DEFAULT_PERMISSION_INTERSTITIAL_FAIL);
                shownForCurrentSetup = false;
                if (!tryShowQuizFallback(context)) {
                    finishCallback(false);
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                ADSUtilitis.trackScreen(context, AppAnalyticsEvents.DEFAULT_PERMISSION_INTERSTITIAL_IMPRESSION);
            }
        });
        try {
            ad.show(context);
        } catch (Exception e) {
            shownForCurrentSetup = false;
            if (!tryShowQuizFallback(context)) {
                finishCallback(false);
            }
        }
    }

    private static void finishCallback(boolean shown) {
        ADSAppManage.isAppOpenBlocked = false;
        if (onCompleteAdCallBack != null) {
            OnCompeteAds cb = onCompleteAdCallBack;
            onCompleteAdCallBack = null;
            cb.onCompeteAds(shown);
        }
    }

    public interface OnCompeteAds {
        void onCompeteAds(boolean b);
    }
}
