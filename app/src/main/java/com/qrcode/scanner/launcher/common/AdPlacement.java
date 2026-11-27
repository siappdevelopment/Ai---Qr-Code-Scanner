package com.qrcode.scanner.launcher.common;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.qrcode.scanner.spapp.R;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

import java.text.SimpleDateFormat;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Google ad loads for the migrated launcher and onboarding screens.
 * Quiz layouts, analytics revenue logs, and source unit ids are not included.
 * Loads no-op when the AdMob app id or the remote unit id is missing.
 */
public final class AdPlacement {
    public interface OnInterstitialAdListener {
        void onInterstitialAdListener();
    }

    private static final long INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS = 10_000L;
    private static final String ADMOB_APP_ID_META = "com.google.android.gms.ads.APPLICATION_ID";

    private static boolean adsSdkStarted;
    private static String adPriority = "";
    private static boolean googleAdFailedShowQuiz;
    private static String nativeAdLabelColor = "";
    private static String nativeAdButtonColor = "";
    private static String nativeAdButtonColorDark = "";
    private static String appOpenId = "";
    private static String otherInterstitialId = "";
    private static String otherBannerId = "";
    private static String otherNativeId = "";
    private static boolean otherAdShow;
    private static String otherAdType = "native";
    private static boolean languageAdShow;
    private static String languageAdType = "native";
    private static String languageBannerId = "";
    private static String languageNativeId = "";
    private static boolean languageInterstitialAdShow;
    private static boolean permissionDefaultAdShow;
    private static String permissionDefaultAdType = "banner";
    private static String permissionDefaultBannerId = "";
    private static String permissionDefaultNativeId = "";
    private static boolean introAdShow;
    private static String introAdType = "banner";
    private static String introBannerId = "";
    private static String introNativeId = "";
    private static boolean introInterstitialAdShow;
    private static int splashDuration;
    private static boolean splashAdShow;
    private static String splashAdType = "banner";
    private static String splashBannerId = "";
    private static String splashNativeId = "";
    private static boolean afterSplashAdShow;
    private static String afterSplashAdType = "inter";
    private static String afterSplashInterstitialId = "";

    private static final AtomicInteger interstitialLoadToken = new AtomicInteger();
    private static final AtomicInteger appOpenLoadToken = new AtomicInteger();
    private static final AtomicInteger onboardingInterPreloadToken = new AtomicInteger();
    private static boolean onboardingInterPreloadInProgress;
    @Nullable
    private static InterstitialAd preloadedOnboardingInterstitial;

    private AdPlacement() {
    }

    public static void initializeIfConfigured(@Nullable Context context) {
        if (context == null || adsSdkStarted || !hasAdMobAppId(context)) {
            return;
        }
        try {
            Context appContext = context.getApplicationContext();
            MobileAds.initialize(appContext, initializationStatus -> {
            });
            adsSdkStarted = true;
        } catch (Exception ignored) {
            adsSdkStarted = false;
        }
    }

    public static boolean hasAdMobAppId(@Nullable Context context) {
        if (context == null) {
            return false;
        }
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (info.metaData == null) {
                return false;
            }
            String appId = info.metaData.getString(ADMOB_APP_ID_META);
            return appId != null && !appId.trim().isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * A Google ad may be requested. When Ad_Priority is QUIZ this is always false: no Google ad request of any kind is
     * sent (Quiz ads are shown instead), so every loader that checks this gate stays silent.
     */
    public static boolean canRequestAds(@Nullable Context context) {
        return !shouldUseQuizPriority() && adsSdkStarted && hasAdMobAppId(context) && AdGates.adsLocationAllowed(context);
    }

    public static boolean shouldUseQuizPriority() {
        return "QUIZ".equalsIgnoreCase(adPriority);
    }

    public static void setAdPriority(String value) {
        adPriority = value == null ? "" : value;
    }

    public static void setGoogleAdFailedShowQuiz(boolean value) {
        googleAdFailedShowQuiz = value;
    }

    public static boolean getGoogleAdFailedShowQuiz() {
        return googleAdFailedShowQuiz;
    }

    public static boolean getAppOpenAdShow() {
        return RemoteConfigValues.getAppOpenAdShow();
    }

    /**
     * Daily limit of the App Open ad: {@code App_Open_Show_Per_Day} ads per calendar day (0 or less = none). Today's count
     * is {@code ad_count_today} of the day {@code last_ad_date}; a new day starts again from 0. Only a really shown ad
     * counts ({@link #incrementAppOpenAdCount}).
     */
    public static boolean canShowAppOpenAd(Context context) {
        int limit = RemoteConfigValues.getAppOpenShowPerDay();
        if (limit <= 0) {
            return false;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("app_open_ad_preferences", Context.MODE_PRIVATE);
        if (!todayKey().equals(prefs.getString("last_ad_date", ""))) {
            return true;
        }
        return prefs.getInt("ad_count_today", 0) < limit;
    }

    /** An App Open ad was really shown: counts for today (a new day resets the count first). */
    public static void incrementAppOpenAdCount(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("app_open_ad_preferences", Context.MODE_PRIVATE);
        String today = todayKey();
        int count = prefs.getInt("ad_count_today", 0);
        if (!today.equals(prefs.getString("last_ad_date", ""))) {
            count = 0;
        }
        prefs.edit().putString("last_ad_date", today).putInt("ad_count_today", count + 1).apply();
    }

    private static String todayKey() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
    }

    public static long getAppOpenLastShowTime(Context context) {
        return context.getSharedPreferences("app_open_ad_preferences", Context.MODE_PRIVATE).getLong("last_show_time", 0);
    }

    public static void setAppOpenLastShowTime(Context context, long time) {
        context.getSharedPreferences("app_open_ad_preferences", Context.MODE_PRIVATE).edit().putLong("last_show_time", time).apply();
    }

    public static void setNativeAdLabelColor(String value) {
        nativeAdLabelColor = value == null ? "" : value;
    }

    public static void setNativeAdButtonColor(String value) {
        nativeAdButtonColor = value == null ? "" : value;
    }

    public static String getNativeAdLabelColor() {
        return nativeAdLabelColor;
    }

    /** Light-theme button colour (Native_Ad_Button_Color_light). */
    public static String getNativeAdButtonColor() {
        return nativeAdButtonColor;
    }

    public static void setNativeAdButtonColorDark(String value) {
        nativeAdButtonColorDark = value == null ? "" : value;
    }

    /**
     * Button colour for the theme the ad is drawn in. The context is the one the ad was inflated with,
     * so a screen that forces light ads gets the light colour. Dark falls back to light when not set.
     */
    public static String getNativeAdButtonColor(@Nullable Context adContext) {
        boolean dark = adContext != null
                && (adContext.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        if (dark && nativeAdButtonColorDark != null && !nativeAdButtonColorDark.trim().isEmpty()) {
            return nativeAdButtonColorDark;
        }
        return nativeAdButtonColor;
    }

    public static String getAppOpenId() {
        return appOpenId;
    }

    public static void setAppOpenId(String value) {
        appOpenId = value == null ? "" : value;
    }

    public static int getSplashDuration() {
        return splashDuration;
    }

    public static void setSplashDuration(int value) {
        splashDuration = Math.max(0, value);
    }

    public static boolean getSplashAdShow() {
        return splashAdShow;
    }

    public static void setSplashAdShow(boolean value) {
        splashAdShow = value;
    }

    public static String getSplashAdType() {
        return splashAdType;
    }

    public static void setSplashAdType(String value) {
        splashAdType = value == null || value.isEmpty() ? "banner" : value;
    }

    public static String getSplashBannerId() {
        return splashBannerId;
    }

    public static void setSplashBannerId(String value) {
        splashBannerId = value == null ? "" : value;
    }

    public static String getSplashNativeId() {
        return splashNativeId;
    }

    public static void setSplashNativeId(String value) {
        splashNativeId = value == null ? "" : value;
    }

    public static boolean getAfterSplashAdShow() {
        return afterSplashAdShow;
    }

    public static void setAfterSplashAdShow(boolean value) {
        afterSplashAdShow = value;
    }

    public static String getAfterSplashAdType() {
        return afterSplashAdType;
    }

    public static void setAfterSplashAdType(String value) {
        afterSplashAdType = value == null || value.isEmpty() ? "inter" : value;
    }

    public static String getAfterSplashInterstitialId() {
        return afterSplashInterstitialId;
    }

    public static void setAfterSplashInterstitialId(String value) {
        afterSplashInterstitialId = value == null ? "" : value;
    }

    public static void loadAfterSplashInterstitialAd(Activity activity, String interstitialId, @Nullable OnInterstitialAdListener listener) {
        loadInterstitialAdInternal(activity, interstitialId, listener, false);
    }

    public static void loadAfterSplashAppOpenAd(Activity activity, String appOpenId, @Nullable OnInterstitialAdListener listener) {
        loadAppOpenAdInternal(activity, appOpenId, listener);
    }

    public static final String REFERRER_URL_KEY = "SetReferrerUrl";

    public static String getReferrerUrl(@Nullable Context context) {
        if (context == null) {
            return "";
        }
        return context.getApplicationContext().getSharedPreferences("referrer_preferences", Context.MODE_PRIVATE).getString(REFERRER_URL_KEY, "");
    }

    public static void setReferrerUrl(@Nullable Context context, @Nullable String value) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences("referrer_preferences", Context.MODE_PRIVATE).edit().putString(REFERRER_URL_KEY, value == null ? "" : value).apply();
    }

    public static void saveInstallDate(@Nullable Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences("appInstallDate", Context.MODE_PRIVATE);
        if (!sharedPreferences.contains("install_date")) {
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(new java.util.Date());
            sharedPreferences.edit().putString("install_date", today).apply();
        }
    }

    public static String getOtherInterstitialId() {
        String id = otherInterstitialId == null ? "" : otherInterstitialId.trim();
        if (!id.isEmpty()) {
            return id;
        }
        // Fallback when Other_Interstitial_Id is missing from Remote Config.
        return RemoteConfigValues.getInterAdsId().trim();
    }

    public static void setOtherInterstitialId(String value) {
        otherInterstitialId = value == null ? "" : value;
    }

    public static boolean getOtherAdShow() {
        return otherAdShow;
    }

    public static void setOtherAdShow(boolean value) {
        otherAdShow = value;
    }

    public static String getOtherAdType() {
        return otherAdType;
    }

    public static void setOtherAdType(String value) {
        otherAdType = value == null || value.isEmpty() ? "native" : value;
    }

    public static String getOtherBannerId() {
        return otherBannerId;
    }

    public static void setOtherBannerId(String value) {
        otherBannerId = value == null ? "" : value;
    }

    public static String getOtherNativeId() {
        return otherNativeId;
    }

    public static void setOtherNativeId(String value) {
        otherNativeId = value == null ? "" : value;
    }

    public static boolean getLanguageAdShow() {
        return languageAdShow;
    }

    public static void setLanguageAdShow(boolean value) {
        languageAdShow = value;
    }

    public static String getLanguageAdType() {
        return languageAdType;
    }

    public static void setLanguageAdType(String value) {
        languageAdType = value == null || value.isEmpty() ? "native" : value;
    }

    public static String getLanguageBannerId() {
        return languageBannerId;
    }

    public static void setLanguageBannerId(String value) {
        languageBannerId = value == null ? "" : value;
    }

    public static String getLanguageNativeId() {
        return languageNativeId;
    }

    public static void setLanguageNativeId(String value) {
        languageNativeId = value == null ? "" : value;
    }

    private static boolean languageSelectRefreshAds = true;

    /** Language_Select_Refresh_Ads: the Language ad starts with a gray button and is replaced once (blue button) when a language is picked. */
    public static boolean getLanguageSelectRefreshAds() {
        return languageSelectRefreshAds;
    }

    public static void setLanguageSelectRefreshAds(boolean value) {
        languageSelectRefreshAds = value;
    }

    public static boolean getLanguageInterstitialAdShow() {
        return languageInterstitialAdShow;
    }

    public static void setLanguageInterstitialAdShow(boolean value) {
        languageInterstitialAdShow = value;
    }

    public static boolean getPermissionDefaultAdShow() {
        return permissionDefaultAdShow;
    }

    public static void setPermissionDefaultAdShow(boolean value) {
        permissionDefaultAdShow = value;
    }

    public static String getPermissionDefaultAdType() {
        return permissionDefaultAdType;
    }

    public static void setPermissionDefaultAdType(String value) {
        permissionDefaultAdType = value == null || value.isEmpty() ? "banner" : value;
    }

    public static String getPermissionDefaultBannerId() {
        return permissionDefaultBannerId;
    }

    public static void setPermissionDefaultBannerId(String value) {
        permissionDefaultBannerId = value == null ? "" : value;
    }

    public static String getPermissionDefaultNativeId() {
        return permissionDefaultNativeId;
    }

    public static void setPermissionDefaultNativeId(String value) {
        permissionDefaultNativeId = value == null ? "" : value;
    }

    public static boolean getIntroAdShow() {
        return introAdShow;
    }

    public static void setIntroAdShow(boolean value) {
        introAdShow = value;
    }

    public static String getIntroAdType() {
        return introAdType;
    }

    public static void setIntroAdType(String value) {
        introAdType = value == null || value.isEmpty() ? "banner" : value;
    }

    public static String getIntroBannerId() {
        return introBannerId;
    }

    public static void setIntroBannerId(String value) {
        introBannerId = value == null ? "" : value;
    }

    public static String getIntroNativeId() {
        return introNativeId;
    }

    public static void setIntroNativeId(String value) {
        introNativeId = value == null ? "" : value;
    }

    public static boolean getIntroInterstitialAdShow() {
        return introInterstitialAdShow;
    }

    public static void setIntroInterstitialAdShow(boolean value) {
        introInterstitialAdShow = value;
    }

    public static int onboardingNativeColor(@Nullable Context context) {
        return onboardingNativeColor(context, false);
    }

    public static int onboardingNativeColor(@Nullable Context context, boolean forceLight) {
        if (context == null) {
            return 0;
        }
        return ContextCompat.getColor(AdTheme.forAds(context, forceLight), R.color.onboarding_native_bg);
    }

    public static void showSlot(Activity activity, boolean showFlag, String adType, String bannerId, String nativeId, String nativeSize, RelativeLayout rlAdView, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, boolean adaptiveBanner) {
        showSlot(activity, showFlag, adType, bannerId, nativeId, nativeSize, rlAdView, rlBannerAdView, slBannerShimmer, llBannerAd, rlNativeAdView, slNativeShimmer, flNativeAd, adaptiveBanner, onboardingNativeColor(activity));
    }

    public static void showSlot(Activity activity, boolean showFlag, String adType, String bannerId, String nativeId, String nativeSize, RelativeLayout rlAdView, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, boolean adaptiveBanner, int nativeFillColor) {
        if (rlAdView == null) {
            return;
        }
        if (!showFlag || (!shouldUseQuizPriority() && !isNetworkAvailable(activity))) {
            rlAdView.setVisibility(View.GONE);
            return;
        }
        rlAdView.setVisibility(View.VISIBLE);
        if ("banner".equalsIgnoreCase(adType)) {
            if (rlBannerAdView != null) {
                rlBannerAdView.setVisibility(View.VISIBLE);
            }
            if (rlNativeAdView != null) {
                rlNativeAdView.setVisibility(View.GONE);
            }
            if (adaptiveBanner) {
                loadAdaptiveBannerAd(activity, bannerId, rlBannerAdView, slBannerShimmer, llBannerAd);
            } else {
                loadBannerAd(activity, bannerId, rlBannerAdView, slBannerShimmer, llBannerAd);
            }
        } else {
            if (rlBannerAdView != null) {
                rlBannerAdView.setVisibility(View.GONE);
            }
            if (rlNativeAdView != null) {
                rlNativeAdView.setVisibility(View.VISIBLE);
            }
            loadNativeAd(activity, nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, nativeSize, null, null, false, nativeFillColor);
        }
    }

    public static void loadBannerAd(Activity activity, String bannerId, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd) {
        if (shouldUseQuizPriority()) {
            if (!QuizAds.showBanner(activity, rlBannerAdView, slBannerShimmer, llBannerAd)) {
                hideBannerContainer(rlBannerAdView, slBannerShimmer, llBannerAd);
            }
            return;
        }
        if (!canLoad(activity, bannerId)) {
            hideBannerContainer(rlBannerAdView, slBannerShimmer, llBannerAd);
            return;
        }
        prepareBannerLoading(slBannerShimmer, llBannerAd);
        AdView adView = new AdView(activity);
        adView.setAdSize(getAdSize(activity));
        adView.setAdUnitId(bannerId);
        llBannerAd.addView(adView);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                AnalyticsEvents.ad(activity, AnalyticsEvents.BANNER_LOAD, AnalyticsEvents.FORMAT_BANNER, bannerId);
                adView.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
                slBannerShimmer.setVisibility(View.GONE);
                llBannerAd.setVisibility(View.VISIBLE);
                rlBannerAdView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                AnalyticsEvents.ad(activity, AnalyticsEvents.BANNER_FAIL, AnalyticsEvents.FORMAT_BANNER, bannerId);
                adView.destroy();
                if (getGoogleAdFailedShowQuiz() && QuizAds.showBanner(activity, rlBannerAdView, slBannerShimmer, llBannerAd)) {
                    return;
                }
                hideBannerContainer(rlBannerAdView, slBannerShimmer, llBannerAd);
            }
        });
        adView.loadAd(new AdRequest.Builder().build());
    }

    public static void loadAdaptiveBannerAd(Activity activity, String bannerId, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd) {
        loadAdaptiveBannerAd(activity, bannerId, rlBannerAdView, slBannerShimmer, llBannerAd, false, false);
    }

    public static void loadAdaptiveBannerAd(Activity activity, String bannerId, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd, boolean googleOnly) {
        loadAdaptiveBannerAd(activity, bannerId, rlBannerAdView, slBannerShimmer, llBannerAd, googleOnly, false);
    }

    /**
     * @param largeInline true = inline adaptive (taller / big), false = anchored adaptive
     */
    public static void loadAdaptiveBannerAd(Activity activity, String bannerId, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd, boolean googleOnly, boolean largeInline) {
        loadAdaptiveBannerAd(activity, bannerId, rlBannerAdView, slBannerShimmer, llBannerAd, googleOnly, largeInline, null);
    }

    /**
     * @param onGoogleFailedQuiz when set, replaces the default small Quiz banner fallback after a Google load failure
     */
    public static void loadAdaptiveBannerAd(Activity activity, String bannerId, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd, boolean googleOnly, boolean largeInline, @Nullable Runnable onGoogleFailedQuiz) {
        loadAdaptiveBannerAd(activity, bannerId, rlBannerAdView, slBannerShimmer, llBannerAd, googleOnly, largeInline, onGoogleFailedQuiz, 0);
    }

    /**
     * @param inlineMaxHeightDp with largeInline: the tallest the inline adaptive banner may be (0 = the screen height)
     */
    public static void loadAdaptiveBannerAd(Activity activity, String bannerId, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd, boolean googleOnly, boolean largeInline, @Nullable Runnable onGoogleFailedQuiz, int inlineMaxHeightDp) {
        if (activity != null) {
            initializeIfConfigured(activity);
        }
        String unitId = bannerId == null ? "" : bannerId.trim();
        if (!googleOnly && shouldUseQuizPriority()) {
            if (!QuizAds.showBanner(activity, rlBannerAdView, slBannerShimmer, llBannerAd)) {
                hideBannerContainer(rlBannerAdView, slBannerShimmer, llBannerAd);
            }
            return;
        }
        if (!canLoad(activity, unitId)) {
            hideBannerContainer(rlBannerAdView, slBannerShimmer, llBannerAd);
            return;
        }
        prepareBannerLoading(slBannerShimmer, llBannerAd);
        AdView adView = new AdView(activity);
        adView.setAdSize(largeInline && inlineMaxHeightDp > 0
                ? AdSize.getInlineAdaptiveBannerAdSize(bannerWidthDp(activity), inlineMaxHeightDp)
                : getAdaptiveAdSize(activity, -1, largeInline));
        adView.setAdUnitId(unitId);
        llBannerAd.addView(adView);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                AnalyticsEvents.ad(activity, AnalyticsEvents.BANNER_LOAD, AnalyticsEvents.FORMAT_BANNER, unitId);
                adView.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
                if (slBannerShimmer != null) {
                    slBannerShimmer.setVisibility(View.GONE);
                }
                llBannerAd.setVisibility(View.VISIBLE);
                rlBannerAdView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                AnalyticsEvents.ad(activity, AnalyticsEvents.BANNER_FAIL, AnalyticsEvents.FORMAT_BANNER, unitId);
                adView.destroy();
                if (!googleOnly && getGoogleAdFailedShowQuiz() && onGoogleFailedQuiz != null) {
                    hideBannerContainer(rlBannerAdView, slBannerShimmer, llBannerAd);
                    onGoogleFailedQuiz.run();
                    return;
                }
                if (!googleOnly && getGoogleAdFailedShowQuiz() && QuizAds.showBanner(activity, rlBannerAdView, slBannerShimmer, llBannerAd)) {
                    return;
                }
                hideBannerContainer(rlBannerAdView, slBannerShimmer, llBannerAd);
            }
        });
        adView.loadAd(new AdRequest.Builder().build());
    }

    public static void loadNativeAd(Activity activity, String nativeId, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, String type) {
        loadNativeAd(activity, nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, type, null, null, false);
    }

    public static void loadNativeAd(Activity activity, String nativeId, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, String type, boolean googleOnly) {
        loadNativeAd(activity, nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, type, null, null, googleOnly);
    }

    public static void loadNativeAd(Activity activity, String nativeId, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, String type, @Nullable java.util.function.Consumer<NativeAd> onAdLoaded, @Nullable Runnable onAdFailed) {
        loadNativeAd(activity, nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, type, onAdLoaded, onAdFailed, false);
    }

    public static void loadNativeAd(Activity activity, String nativeId, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, String type, @Nullable java.util.function.Consumer<NativeAd> onAdLoaded, @Nullable Runnable onAdFailed, boolean googleOnly) {
        loadNativeAd(activity, nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, type, onAdLoaded, onAdFailed, googleOnly, onboardingNativeColor(activity), false);
    }

    public static void loadNativeAd(Activity activity, String nativeId, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, String type, @Nullable java.util.function.Consumer<NativeAd> onAdLoaded, @Nullable Runnable onAdFailed, boolean googleOnly, int nativeFillColor) {
        loadNativeAd(activity, nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, type, onAdLoaded, onAdFailed, googleOnly, nativeFillColor, false);
    }

    public static void loadNativeAd(Activity activity, String nativeId, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, String type, @Nullable java.util.function.Consumer<NativeAd> onAdLoaded, @Nullable Runnable onAdFailed, boolean googleOnly, int nativeFillColor, boolean forceLightTheme) {
        if (activity != null) {
            initializeIfConfigured(activity);
        }
        final boolean lightAds = forceLightTheme;
        final int fillColor = nativeFillColor != 0 ? nativeFillColor : onboardingNativeColor(activity, lightAds);
        String unitId = nativeId == null ? "" : nativeId.trim();
        if (!googleOnly && shouldUseQuizPriority()) {
            // Do not reveal container here — sticky hosts (app drawer) must park/position first.
            if (!QuizAds.showNative(activity, rlNativeAdView, slNativeShimmer, flNativeAd, type, lightAds, false)) {
                hideNativeContainer(rlNativeAdView, slNativeShimmer, flNativeAd);
                if (onAdFailed != null) {
                    onAdFailed.run();
                }
            } else {
                paintOnboardingNative(flNativeAd, fillColor);
                if (onAdLoaded != null) {
                    onAdLoaded.accept(null);
                }
                if (rlNativeAdView != null && rlNativeAdView.getVisibility() != View.VISIBLE) {
                    rlNativeAdView.setVisibility(View.VISIBLE);
                }
            }
            return;
        }
        if (!canLoad(activity, unitId)) {
            hideNativeContainer(rlNativeAdView, slNativeShimmer, flNativeAd);
            if (onAdFailed != null) {
                onAdFailed.run();
            }
            return;
        }
        paintOnboardingNative(rlNativeAdView, fillColor);
        if (slNativeShimmer != null) {
            slNativeShimmer.setVisibility(View.VISIBLE);
            slNativeShimmer.startShimmer();
            paintOnboardingNative(slNativeShimmer, fillColor);
        }
        AdLoader adLoader = new AdLoader.Builder(activity, unitId).forNativeAd(nativeAd -> {
            AnalyticsEvents.ad(activity, AnalyticsEvents.NATIVE_LOAD, AnalyticsEvents.FORMAT_NATIVE, unitId);
            if (activity.isFinishing() || activity.isDestroyed() || flNativeAd == null) {
                nativeAd.destroy();
                return;
            }
            nativeAd.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
            Context adContext = AdTheme.forAds(activity, lightAds);
            NativeAdView adView = (NativeAdView) LayoutInflater.from(adContext).inflate(resolveGoogleNativeLayout(type), flNativeAd, false);
            populateNativeAdView(nativeAd, adView, type);
            flNativeAd.removeAllViews();
            flNativeAd.addView(GestureSafeNativeAdView.wrap(adView));
            if (slNativeShimmer != null) {
                slNativeShimmer.stopShimmer();
                slNativeShimmer.setVisibility(View.GONE);
            }
            flNativeAd.setVisibility(View.VISIBLE);
            if (rlNativeAdView != null) {
                rlNativeAdView.setVisibility(View.VISIBLE);
            }
            paintOnboardingNative(rlNativeAdView, fillColor);
            paintOnboardingNative(slNativeShimmer, fillColor);
            paintOnboardingNative(flNativeAd, fillColor);
            if (onAdLoaded != null) {
                onAdLoaded.accept(nativeAd);
            }
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                AnalyticsEvents.ad(activity, AnalyticsEvents.NATIVE_FAIL, AnalyticsEvents.FORMAT_NATIVE, unitId);
                if (!googleOnly && getGoogleAdFailedShowQuiz()
                        && QuizAds.showNative(activity, rlNativeAdView, slNativeShimmer, flNativeAd, type, lightAds, false)) {
                    paintOnboardingNative(flNativeAd, fillColor);
                    if (onAdLoaded != null) {
                        onAdLoaded.accept(null);
                    }
                    if (rlNativeAdView != null && rlNativeAdView.getVisibility() != View.VISIBLE) {
                        rlNativeAdView.setVisibility(View.VISIBLE);
                    }
                    return;
                }
                hideNativeContainer(rlNativeAdView, slNativeShimmer, flNativeAd);
                if (onAdFailed != null) {
                    onAdFailed.run();
                }
            }
        }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    /**
     * Shows an already loaded Google native ad again in a new container (the app drawer re-shows its last ad when it is
     * opened again before a new one is due). The ad must not have been destroyed.
     */
    public static void bindLoadedNative(Activity activity, FrameLayout flNativeAd, NativeAd nativeAd, String type) {
        bindLoadedNative(activity, flNativeAd, nativeAd, type, null);
    }

    /** As above, with the call-to-action button in {@code ctaColorHex} (e.g. gray first, blue after a Language pick). */
    public static void bindLoadedNative(Activity activity, FrameLayout flNativeAd, NativeAd nativeAd, String type, @Nullable String ctaColorHex) {
        if (activity == null || activity.isFinishing() || flNativeAd == null || nativeAd == null) {
            return;
        }
        Context adContext = AdTheme.forAds(activity, false);
        NativeAdView adView = (NativeAdView) LayoutInflater.from(adContext).inflate(resolveGoogleNativeLayout(type), flNativeAd, false);
        populateNativeAdView(nativeAd, adView, type, ctaColorHex);
        flNativeAd.removeAllViews();
        flNativeAd.addView(GestureSafeNativeAdView.wrap(adView));
        flNativeAd.setVisibility(View.VISIBLE);
        paintOnboardingNative(flNativeAd, onboardingNativeColor(activity, false));
    }

    public static void requestLargeNativeAd(Activity activity, String unitId, @Nullable java.util.function.Consumer<NativeAd> onLoaded, @Nullable Runnable onFailed) {
        if (activity != null) {
            initializeIfConfigured(activity);
        }
        String id = unitId == null ? "" : unitId.trim();
        Context context = activity == null ? null : activity.getApplicationContext();
        if (context == null || !canLoad(activity, id)) {
            if (onFailed != null) {
                onFailed.run();
            }
            return;
        }
        AdLoader adLoader = new AdLoader.Builder(context, id).forNativeAd(nativeAd -> {
            AnalyticsEvents.ad(context, AnalyticsEvents.NATIVE_LOAD, AnalyticsEvents.FORMAT_NATIVE, id);
            nativeAd.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
            if (onLoaded != null) {
                onLoaded.accept(nativeAd);
            }
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
            AnalyticsEvents.ad(context, AnalyticsEvents.NATIVE_FAIL, AnalyticsEvents.FORMAT_NATIVE, id);
                if (onFailed != null) {
                    onFailed.run();
                }
            }
        }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void requestSmallBannerAd(Activity activity, String unitId, @Nullable java.util.function.Consumer<AdView> onLoaded, @Nullable Runnable onFailed) {
        requestBannerAd(activity, unitId, 0, onLoaded, onFailed);
    }

    private static void requestBannerAd(Activity activity, String unitId, int adaptiveWidthDp, @Nullable java.util.function.Consumer<AdView> onLoaded, @Nullable Runnable onFailed) {
        if (activity != null) {
            initializeIfConfigured(activity);
        }
        String id = unitId == null ? "" : unitId.trim();
        if (activity == null || !canLoad(activity, id)) {
            if (onFailed != null) {
                onFailed.run();
            }
            return;
        }
        AdView adView = new AdView(activity);
        if (adaptiveWidthDp == 0) {
            adView.setAdSize(AdSize.BANNER);
        } else {
            int width = adaptiveWidthDp > 0 ? adaptiveWidthDp : bannerWidthDp(activity);
            adView.setAdSize(AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, width));
        }
        adView.setAdUnitId(id);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                AnalyticsEvents.ad(activity, AnalyticsEvents.BANNER_LOAD, AnalyticsEvents.FORMAT_BANNER, id);
                adView.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
                if (onLoaded != null) {
                    onLoaded.accept(adView);
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                AnalyticsEvents.ad(activity, AnalyticsEvents.BANNER_FAIL, AnalyticsEvents.FORMAT_BANNER, id);
                adView.destroy();
                if (onFailed != null) {
                    onFailed.run();
                }
            }
        });
        adView.loadAd(new AdRequest.Builder().build());
    }

    public static void showLargeNative(Activity activity, FrameLayout container, NativeAd nativeAd) {
        showLargeNative(activity, container, nativeAd, false);
    }

    public static void showLargeNative(Activity activity, FrameLayout container, NativeAd nativeAd, boolean forceLightTheme) {
        if (activity == null || activity.isFinishing() || container == null || nativeAd == null) {
            return;
        }
        Context adContext = AdTheme.forAds(activity, forceLightTheme);
        NativeAdView adView = (NativeAdView) LayoutInflater.from(adContext).inflate(R.layout.native_large_ad_layout, container, false);
        populateNativeAdView(nativeAd, adView, "large");
        container.removeAllViews();
        container.addView(GestureSafeNativeAdView.wrap(adView));
        container.setVisibility(View.VISIBLE);
        paintOnboardingNative(container, onboardingNativeColor(activity, forceLightTheme));
    }

    public static void loadLanguageInterstitialAd(Activity activity, OnInterstitialAdListener listener) {
        if (!getLanguageInterstitialAdShow()) {
            notifyComplete(listener);
            return;
        }
        if (isOnboardingInterPreloadType()) {
            if (tryShowPreloadedOnboardingInterstitial(activity, listener)) {
                return;
            }
            clearOnboardingInterPreloadCache();
        }
        loadInterstitialAdInternal(activity, getOtherInterstitialId(), listener, true);
    }

    public static void loadIntroInterstitialAd(Activity activity, OnInterstitialAdListener listener) {
        if (!getIntroInterstitialAdShow()) {
            notifyComplete(listener);
            return;
        }
        if (isOnboardingInterPreloadType()) {
            if (tryShowPreloadedOnboardingInterstitial(activity, listener)) {
                return;
            }
            clearOnboardingInterPreloadCache();
        }
        loadInterstitialAdInternal(activity, getOtherInterstitialId(), listener, true);
    }

    /** Language and Intro interstitials follow inter_ads_click_type: "Preload" loads ahead, anything else loads on demand. */
    private static boolean isOnboardingInterPreloadType() {
        String type = RemoteConfigValues.getInterAdsClickType();
        return type != null && "preload".equalsIgnoreCase(type.trim());
    }

    /** Called when the Language or Intro screen opens; no-op unless that screen's interstitial is on and the type is Preload. */
    public static void preloadOnboardingInterstitialAd(Context context, boolean forIntro) {
        boolean show = forIntro ? getIntroInterstitialAdShow() : getLanguageInterstitialAdShow();
        if (context == null || !show || !isOnboardingInterPreloadType() || shouldUseQuizPriority()
                || !canRequestAds(context) || !isNetworkAvailable(context)) {
            return;
        }
        String unitId = getOtherInterstitialId();
        if (unitId == null || unitId.trim().isEmpty() || preloadedOnboardingInterstitial != null || onboardingInterPreloadInProgress) {
            return;
        }
        onboardingInterPreloadInProgress = true;
        int token = onboardingInterPreloadToken.get();
        AnalyticsEvents.ad(context, AnalyticsEvents.INTERSTITIAL_PRELOAD, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
        InterstitialAd.load(context.getApplicationContext(), unitId.trim(), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                AnalyticsEvents.ad(context, AnalyticsEvents.INTERSTITIAL_LOAD, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
                if (token != onboardingInterPreloadToken.get()) {
                    return;
                }
                onboardingInterPreloadInProgress = false;
                preloadedOnboardingInterstitial = ad;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                AnalyticsEvents.ad(context, AnalyticsEvents.INTERSTITIAL_FAIL, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
                if (token != onboardingInterPreloadToken.get()) {
                    return;
                }
                onboardingInterPreloadInProgress = false;
            }
        });
    }

    private static boolean tryShowPreloadedOnboardingInterstitial(Activity activity, @Nullable OnInterstitialAdListener listener) {
        if (activity == null || activity.isFinishing() || preloadedOnboardingInterstitial == null || shouldUseQuizPriority()) {
            return false;
        }
        InterstitialAd ad = preloadedOnboardingInterstitial;
        preloadedOnboardingInterstitial = null;
        AtomicBoolean completed = new AtomicBoolean(false);
        ad.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                notifyComplete(listener, completed);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                if (getGoogleAdFailedShowQuiz() && QuizAds.showInterstitial(activity, () -> notifyComplete(listener, completed))) {
                    return;
                }
                notifyComplete(listener, completed);
            }
        });
        ad.show(activity);
        return true;
    }

    private static void clearOnboardingInterPreloadCache() {
        onboardingInterPreloadToken.incrementAndGet();
        onboardingInterPreloadInProgress = false;
        preloadedOnboardingInterstitial = null;
    }

    public static void logAdRevenue(Context context, AdValue adValue) {
        if (context == null || adValue == null) {
            return;
        }
        try {
            double revenue = adValue.getValueMicros() / 1_000_000.0;
            String currency = adValue.getCurrencyCode();
            android.os.Bundle adRevenueParams = new android.os.Bundle();
            adRevenueParams.putString(FirebaseAnalytics.Param.AD_PLATFORM, "Google Ad Manager");
            adRevenueParams.putString(FirebaseAnalytics.Param.CURRENCY, currency);
            adRevenueParams.putDouble(FirebaseAnalytics.Param.VALUE, revenue);
            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, adRevenueParams);
        } catch (Exception ignored) {
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    private static boolean canLoad(Activity activity, String unitId) {
        return activity != null && !activity.isFinishing() && canRequestAds(activity) && isNetworkAvailable(activity) && unitId != null && !unitId.isEmpty();
    }

    /**
     * Preloads a full-screen native ad (no loading dialog). {@code onFailed} runs when ads cannot be requested here
     * (consent, location rule, no network, no id, Quiz priority) or when the load fails.
     */
    public static void preloadFullNativeAd(@Nullable Context context, @Nullable String adUnitId, @NonNull java.util.function.Consumer<NativeAd> onLoaded, @NonNull Runnable onFailed) {
        String nativeId = adUnitId == null ? "" : adUnitId.trim();
        if (context == null || nativeId.isEmpty() || shouldUseQuizPriority() || !canRequestAds(context) || !isNetworkAvailable(context)) {
            onFailed.run();
            return;
        }
        final Context app = context.getApplicationContext();
        new AdLoader.Builder(app, nativeId).forNativeAd(nativeAd -> {
            AnalyticsEvents.ad(app, AnalyticsEvents.NATIVE_LOAD, AnalyticsEvents.FORMAT_NATIVE, nativeId);
            onLoaded.accept(nativeAd);
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                AnalyticsEvents.ad(app, AnalyticsEvents.NATIVE_FAIL, AnalyticsEvents.FORMAT_NATIVE, nativeId);
                onFailed.run();
            }
        }).build().loadAd(new AdRequest.Builder().build());
    }

    private static void loadInterstitialAdInternal(Activity activity, String interstitialId, @Nullable OnInterstitialAdListener listener, boolean showLoadingDialog) {
        loadInterstitialAdInternal(activity, interstitialId, listener, showLoadingDialog, true, getGoogleAdFailedShowQuiz());
    }

    private static void loadInterstitialAdInternal(Activity activity, String interstitialId, @Nullable OnInterstitialAdListener listener, boolean showLoadingDialog, boolean honorQuizPriority, boolean quizOnFail) {
        int token = interstitialLoadToken.incrementAndGet();
        AtomicBoolean completed = new AtomicBoolean(false);
        if (honorQuizPriority && shouldUseQuizPriority()) {
            if (!QuizAds.showInterstitial(activity, () -> notifyComplete(listener, completed))) {
                notifyComplete(listener, completed);
            }
            return;
        }
        if (!canLoad(activity, interstitialId)) {
            if (quizOnFail && QuizAds.showInterstitial(activity, () -> notifyComplete(listener, completed))) {
                return;
            }
            notifyComplete(listener, completed);
            return;
        }
        Dialog loadingDialog = showLoadingDialog ? showInterstitialLoadingDialog(activity) : null;
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeout = () -> {
            if (token != interstitialLoadToken.get()) {
                return;
            }
            interstitialLoadToken.incrementAndGet();
            dismissInterstitialLoadingDialog(loadingDialog);
            if (quizOnFail && QuizAds.showInterstitial(activity, () -> notifyComplete(listener, completed))) {
                return;
            }
            notifyComplete(listener, completed);
        };
        if (loadingDialog != null) {
            handler.postDelayed(timeout, INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS);
        }
        InterstitialAd.load(activity, interstitialId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                AnalyticsEvents.ad(activity, AnalyticsEvents.INTERSTITIAL_LOAD, AnalyticsEvents.FORMAT_INTERSTITIAL, interstitialId);
                if (token != interstitialLoadToken.get()) {
                    return;
                }
                handler.removeCallbacks(timeout);
                dismissInterstitialLoadingDialog(loadingDialog);
                ad.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        notifyComplete(listener, completed);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        if (quizOnFail && QuizAds.showInterstitial(activity, () -> notifyComplete(listener, completed))) {
                            return;
                        }
                        notifyComplete(listener, completed);
                    }
                });
                ad.show(activity);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                AnalyticsEvents.ad(activity, AnalyticsEvents.INTERSTITIAL_FAIL, AnalyticsEvents.FORMAT_INTERSTITIAL, interstitialId);
                if (token != interstitialLoadToken.get()) {
                    return;
                }
                handler.removeCallbacks(timeout);
                dismissInterstitialLoadingDialog(loadingDialog);
                if (quizOnFail && QuizAds.showInterstitial(activity, () -> notifyComplete(listener, completed))) {
                    return;
                }
                notifyComplete(listener, completed);
            }
        });
    }

    private static void loadAppOpenAdInternal(Activity activity, String appOpenUnitId, @Nullable OnInterstitialAdListener listener) {
        loadAppOpenAdInternal(activity, appOpenUnitId, listener, true, getGoogleAdFailedShowQuiz(), false);
    }

    private static void loadAppOpenAdInternal(Activity activity, String appOpenUnitId, @Nullable OnInterstitialAdListener listener, boolean honorQuizPriority, boolean quizOnFail) {
        loadAppOpenAdInternal(activity, appOpenUnitId, listener, honorQuizPriority, quizOnFail, false);
    }

    private static void loadAppOpenAdInternal(Activity activity, String appOpenUnitId, @Nullable OnInterstitialAdListener listener, boolean honorQuizPriority, boolean quizOnFail, boolean showLoadingDialog) {
        int token = appOpenLoadToken.incrementAndGet();
        AtomicBoolean completed = new AtomicBoolean(false);
        if (honorQuizPriority && shouldUseQuizPriority()) {
            if (!QuizAds.showAppOpen(activity, () -> notifyComplete(listener, completed))) {
                notifyComplete(listener, completed);
            }
            return;
        }
        if (!canLoad(activity, appOpenUnitId)) {
            if (quizOnFail && QuizAds.showAppOpen(activity, () -> notifyComplete(listener, completed))) {
                return;
            }
            notifyComplete(listener, completed);
            return;
        }
        Dialog loadingDialog = showLoadingDialog ? showInterstitialLoadingDialog(activity) : null;
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeout = () -> {
            if (token != appOpenLoadToken.get()) {
                return;
            }
            appOpenLoadToken.incrementAndGet();
            dismissInterstitialLoadingDialog(loadingDialog);
            if (quizOnFail && QuizAds.showAppOpen(activity, () -> notifyComplete(listener, completed))) {
                return;
            }
            notifyComplete(listener, completed);
        };
        if (loadingDialog != null) {
            handler.postDelayed(timeout, INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS);
        }
        AppOpenAd.load(activity, appOpenUnitId, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                AnalyticsEvents.ad(activity, AnalyticsEvents.APP_OPEN_AD_LOAD, AnalyticsEvents.FORMAT_APP_OPEN, appOpenUnitId);
                if (token != appOpenLoadToken.get() || activity.isFinishing()) {
                    dismissInterstitialLoadingDialog(loadingDialog);
                    if (quizOnFail && !activity.isFinishing()
                            && QuizAds.showAppOpen(activity, () -> notifyComplete(listener, completed))) {
                        return;
                    }
                    notifyComplete(listener, completed);
                    return;
                }
                handler.removeCallbacks(timeout);
                dismissInterstitialLoadingDialog(loadingDialog);
                ad.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        notifyComplete(listener, completed);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        if (quizOnFail && QuizAds.showAppOpen(activity, () -> notifyComplete(listener, completed))) {
                            return;
                        }
                        notifyComplete(listener, completed);
                    }
                });
                ad.show(activity);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                AnalyticsEvents.ad(activity, AnalyticsEvents.APP_OPEN_AD_FAIL, AnalyticsEvents.FORMAT_APP_OPEN, appOpenUnitId);
                if (token != appOpenLoadToken.get()) {
                    return;
                }
                handler.removeCallbacks(timeout);
                dismissInterstitialLoadingDialog(loadingDialog);
                if (quizOnFail && QuizAds.showAppOpen(activity, () -> notifyComplete(listener, completed))) {
                    return;
                }
                notifyComplete(listener, completed);
            }
        });
    }

    public static void populateNativeAdView(NativeAd nativeAd, NativeAdView adView, String type) {
        populateNativeAdView(nativeAd, adView, type, null);
    }

    /** @param ctaColorHex when set, the call-to-action button uses this colour instead of Native_Ad_Button_Color. */
    public static void populateNativeAdView(NativeAd nativeAd, NativeAdView adView, String type, @Nullable String ctaColorHex) {
        boolean hasMedia = "large".equalsIgnoreCase(type) || "medium".equalsIgnoreCase(type) || "full".equalsIgnoreCase(type);
        if (hasMedia) {
            adView.setMediaView(adView.findViewById(R.id.ad_media));
        }
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        if (hasMedia && adView.getMediaView() != null) {
            MediaView mediaView = adView.getMediaView();
            mediaView.setMediaContent(nativeAd.getMediaContent());
        }
        if (nativeAd.getIcon() != null && adView.getIconView() instanceof AppCompatImageView) {
            ((AppCompatImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
        }
        if (adView.getHeadlineView() instanceof AppCompatTextView) {
            ((AppCompatTextView) Objects.requireNonNull(adView.getHeadlineView())).setText(nativeAd.getHeadline());
        }
        if (adView.getBodyView() instanceof AppCompatTextView) {
            ((AppCompatTextView) adView.getBodyView()).setText(nativeAd.getBody());
        }
        if (adView.getCallToActionView() instanceof AppCompatTextView) {
            AppCompatTextView button = (AppCompatTextView) adView.getCallToActionView();
            applyCallToAction(button, nativeAd.getCallToAction());
            applyBackgroundColor(button, ctaColorHex != null && !ctaColorHex.trim().isEmpty() ? ctaColorHex : getNativeAdButtonColor(adView.getContext()), R.color.primary);
        }
        View attribution = adView.findViewById(R.id.ad_attribution);
        applyBackgroundColor(attribution, nativeAdLabelColor, 0);
        adView.setNativeAd(nativeAd);
        if (adView.getCallToActionView() instanceof AppCompatTextView) {
            applyCallToAction((AppCompatTextView) adView.getCallToActionView(), ((AppCompatTextView) adView.getCallToActionView()).getText());
        }
    }

    private static void applyCallToAction(AppCompatTextView button, CharSequence label) {
        String text = label == null ? "" : label.toString().trim();
        if (text.isEmpty()) {
            text = "Install";
        }
        button.setSingleLine(false);
        button.setHorizontallyScrolling(false);
        button.setMaxLines(1);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setGravity(Gravity.CENTER);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
    }

    private static int resolveGoogleNativeLayout(@Nullable String type) {
        if ("large".equalsIgnoreCase(type)) {
            return R.layout.native_large_ad_layout;
        }
        if ("medium".equalsIgnoreCase(type)) {
            return R.layout.native_medium_ad_layout;
        }
        if ("intro".equalsIgnoreCase(type)) {
            return R.layout.native_intro_ad_layout;
        }
        if ("full".equalsIgnoreCase(type)) {
            return R.layout.native_full_ad_layout;
        }
        return R.layout.native_small_ad_layout;
    }

    private static void applyBackgroundColor(View view, String colorHex, int fallbackColorRes) {
        if (view == null) {
            return;
        }
        Integer color = null;
        if (colorHex != null && !colorHex.trim().isEmpty()) {
            try {
                String value = colorHex.trim();
                if (!value.startsWith("#")) {
                    value = "#" + value;
                }
                color = Color.parseColor(value);
            } catch (Exception ignored) {
                color = null;
            }
        }
        if (color == null && fallbackColorRes != 0) {
            color = ContextCompat.getColor(view.getContext(), fallbackColorRes);
        }
        if (color == null) {
            return;
        }
        if (view.getBackground() != null) {
            DrawableCompat.setTint(DrawableCompat.wrap(view.getBackground().mutate()), color);
        } else {
            view.setBackgroundColor(color);
        }
    }

    private static AdSize getAdSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        int adWidth = (int) (outMetrics.widthPixels / outMetrics.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    private static AdSize getAdaptiveAdSize(Activity activity) {
        return getAdaptiveAdSize(activity, -1, false);
    }

    private static AdSize getAdaptiveAdSize(Activity activity, int widthDp) {
        return getAdaptiveAdSize(activity, widthDp, false);
    }

    private static AdSize getAdaptiveAdSize(Activity activity, int widthDp, boolean largeInline) {
        int width = widthDp > 0 ? widthDp : bannerWidthDp(activity);
        if (largeInline) {
            return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, width);
        }
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, width);
    }

    private static int bannerWidthDp(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        return Math.max(1, (int) (outMetrics.widthPixels / outMetrics.density));
    }

    private static void prepareBannerLoading(@Nullable ShimmerFrameLayout slBannerShimmer, @Nullable LinearLayout llBannerAd) {
        if (llBannerAd != null) {
            destroyAdViews(llBannerAd);
            llBannerAd.removeAllViews();
            llBannerAd.setVisibility(View.GONE);
        }
        if (slBannerShimmer != null) {
            slBannerShimmer.setVisibility(View.VISIBLE);
            slBannerShimmer.startShimmer();
        }
    }

    private static void hideBannerContainer(@Nullable RelativeLayout rlBannerAdView, @Nullable ShimmerFrameLayout slBannerShimmer, @Nullable LinearLayout llBannerAd) {
        if (slBannerShimmer != null) {
            slBannerShimmer.stopShimmer();
            slBannerShimmer.setVisibility(View.GONE);
        }
        if (llBannerAd != null) {
            destroyAdViews(llBannerAd);
            llBannerAd.removeAllViews();
            llBannerAd.setVisibility(View.GONE);
        }
        if (rlBannerAdView != null) {
            rlBannerAdView.setVisibility(View.GONE);
        }
    }

    /** Repaint native ad chrome to the saved Light/Dark app theme. */
    public static void paintNativeFill(@Nullable View root, int fillColor) {
        paintOnboardingNative(root, fillColor);
    }

    private static void paintOnboardingNative(@Nullable View root, int fillColor) {
        if (root == null || fillColor == 0) {
            return;
        }
        Context light = AdTheme.withForcedNight(root.getContext(), false);
        Context dark = AdTheme.withForcedNight(root.getContext(), true);
        paintMatchingFill(root, ContextCompat.getColor(light, R.color.onboarding_native_bg), fillColor);
        paintMatchingFill(root, ContextCompat.getColor(dark, R.color.onboarding_native_bg), fillColor);
        paintMatchingFill(root, ContextCompat.getColor(light, R.color.ad_background), fillColor);
        paintMatchingFill(root, ContextCompat.getColor(dark, R.color.ad_background), fillColor);
        paintMatchingFill(root, ContextCompat.getColor(light, R.color.surface_secondary), fillColor);
        paintMatchingFill(root, ContextCompat.getColor(dark, R.color.surface_secondary), fillColor);
    }

    private static void paintMatchingFill(View view, int fromColor, int toColor) {
        if (view instanceof androidx.cardview.widget.CardView) {
            androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) view;
            if (card.getCardBackgroundColor().getDefaultColor() == fromColor) {
                card.setCardBackgroundColor(toColor);
            }
        } else if (view.getBackground() instanceof ColorDrawable && ((ColorDrawable) view.getBackground()).getColor() == fromColor) {
            view.setBackgroundColor(toColor);
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                paintMatchingFill(group.getChildAt(i), fromColor, toColor);
            }
        }
    }

    private static void hideNativeContainer(@Nullable RelativeLayout rlNativeAdView, @Nullable ShimmerFrameLayout slNativeShimmer, @Nullable FrameLayout flNativeAd) {
        if (slNativeShimmer != null) {
            slNativeShimmer.stopShimmer();
            slNativeShimmer.setVisibility(View.GONE);
        }
        if (flNativeAd != null) {
            flNativeAd.removeAllViews();
            flNativeAd.setVisibility(View.GONE);
        }
        if (rlNativeAdView != null) {
            rlNativeAdView.setVisibility(View.GONE);
        }
    }

    private static void destroyAdViews(LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof AdView) {
                ((AdView) child).destroy();
            }
        }
    }

    @Nullable
    public static Dialog showInterstitialLoadingDialog(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        try {
            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(LayoutInflater.from(AdTheme.forApp(activity)).inflate(R.layout.dialog_loading_ads, null, false));
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.setCancelable(false);
            dialog.show();
            return dialog;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void dismissInterstitialLoadingDialog(@Nullable Dialog dialog) {
        if (dialog == null || !dialog.isShowing()) {
            return;
        }
        try {
            dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    private static void notifyComplete(@Nullable OnInterstitialAdListener listener) {
        notifyComplete(listener, new AtomicBoolean(false));
    }

    private static void notifyComplete(@Nullable OnInterstitialAdListener listener, AtomicBoolean completed) {
        if (listener == null || !completed.compareAndSet(false, true)) {
            return;
        }
        listener.onInterstitialAdListener();
    }

    public static final String IP_COUNTRY_NAME_KEY = "ip_country_name";

    public static void setIpCountryName(Context context, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("ipCountryName", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(IP_COUNTRY_NAME_KEY, value).apply();
    }

    public static boolean isNotificationGranted(Context context) {
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

}
