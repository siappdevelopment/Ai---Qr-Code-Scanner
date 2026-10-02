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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

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
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

import java.text.SimpleDateFormat;
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
    private static final String LAUNCHER_APP_BACK_PREFS = "launcher_app_back_ad";
    private static final String LAUNCHER_APP_AD_TYPE_GOOGLE_INTER = "google_inter";
    private static final String LAUNCHER_APP_AD_TYPE_GOOGLE_APP_OPEN = "google_app_open";
    private static final String LAUNCHER_APP_AD_TYPE_GOOGLE_NATIVE = "google_native";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_INTER = "quiz_inter";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_APP_OPEN = "quiz_app_open";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_NATIVE = "quiz_native";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_BROWSER = "quiz_browser";

    private static int rightSwipeInterstitialCount = 1;

    private static boolean adsSdkStarted;
    private static String adPriority = "";
    private static boolean googleAdFailedShowQuiz;
    private static String nativeAdLabelColor = "";
    private static String nativeAdButtonColor = "";
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
    private static boolean afterDefaultAdShow;
    private static String afterDefaultAdType = "inter";
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
    private static boolean launcherAppNativeListAdShow;
    private static int launcherAppNativeListAdShowPerDay;
    private static String launcherAppNativeListId = "";
    private static boolean launcherAppClickAdShow;
    private static int launcherAppCount;
    private static final ArrayList<String> launcherAppAdSequence = new ArrayList<>();
    private static int launcherAppAdSequenceIndex;
    private static String launcherAppInterstitialId = "";
    private static boolean launcherAppBackClickAdShow;
    private static int launcherAppBackCount;
    private static final ArrayList<String> launcherAppBackAdSequence = new ArrayList<>();
    private static int launcherAppBackAdSequenceIndex;
    private static String launcherAppBackInterstitialId = "";
    private static boolean launcherGoogleAdFailedShowQuiz;
    private static int launcherAppClickCount = 1;
    private static boolean launcherBackConfigRestored;

    private static final AtomicInteger interstitialLoadToken = new AtomicInteger();
    private static final AtomicInteger appOpenLoadToken = new AtomicInteger();
    private static final AtomicInteger afterDefaultPreloadToken = new AtomicInteger();
    private static boolean afterDefaultAdPreloadInProgress;
    @Nullable
    private static InterstitialAd preloadedAfterDefaultInterstitial;
    @Nullable
    private static AppOpenAd preloadedAfterDefaultAppOpen;

    private AdPlacement() {
    }

    public static void initializeIfConfigured(@Nullable Context context) {
        if (context == null || adsSdkStarted || !hasAdMobAppId(context)) {
            return;
        }
        try {
            MobileAds.initialize(context.getApplicationContext(), initializationStatus -> {
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

    public static boolean canRequestAds(@Nullable Context context) {
        return adsSdkStarted && hasAdMobAppId(context);
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

    public static boolean getLauncherAppQuizIconShow() {
        return RemoteConfigValues.getLauncherAppQuizIconShow();
    }

    public static int getLauncherAppQuizIconCount() {
        return RemoteConfigValues.getLauncherAppQuizIconCount();
    }

    public static boolean getLauncherAppNativeAdShow() {
        return RemoteConfigValues.getLauncherAppNativeAdShow();
    }

    public static int getLauncherAppNativeAdShowPerDay() {
        return RemoteConfigValues.getLauncherAppNativeAdShowPerDay();
    }

    public static String getLauncherAppNativeId() {
        return RemoteConfigValues.getLauncherAppNativeId();
    }

    public static boolean canShowLauncherAppNativeAd(Context context) {
        int countPerDay = getLauncherAppNativeAdShowPerDay();
        if (countPerDay <= 0) {
            return true;
        }
        long intervalMillis = (24L * 60L * 60L * 1000L) / countPerDay;
        long lastShowTime = context.getSharedPreferences("launcher_app_native_ad_preferences", Context.MODE_PRIVATE).getLong("last_show_time", 0);
        return System.currentTimeMillis() - lastShowTime >= intervalMillis;
    }

    public static void setLauncherAppNativeLastShowTime(Context context, long time) {
        context.getSharedPreferences("launcher_app_native_ad_preferences", Context.MODE_PRIVATE).edit().putLong("last_show_time", time).apply();
    }

    public static boolean getDefaultAppPopupShow() {
        return RemoteConfigValues.getDefaultAppPopupShow();
    }

    public static int getDefaultAppPopupCount() {
        return RemoteConfigValues.getDefaultAppPopupCount();
    }

    public static boolean canShowDefaultAppPopup(Context context) {
        int countPerDay = getDefaultAppPopupCount();
        if (countPerDay <= 0) {
            return true;
        }
        long intervalMillis = (24L * 60L * 60L * 1000L) / countPerDay;
        long lastShowTime = getDefaultAppPopupLastShowTime(context);
        return System.currentTimeMillis() - lastShowTime >= intervalMillis;
    }

    public static long getDefaultAppPopupLastShowTime(Context context) {
        return context.getSharedPreferences("default_app_popup_preferences", Context.MODE_PRIVATE).getLong("last_show_time", 0);
    }

    public static void setDefaultAppPopupLastShowTime(Context context, long time) {
        context.getSharedPreferences("default_app_popup_preferences", Context.MODE_PRIVATE).edit().putLong("last_show_time", time).apply();
    }

    public static boolean canShowAppOpenAd(Context context) {
        int countPerDay = RemoteConfigValues.getAppOpenShowPerDay();
        if (countPerDay <= 0) {
            return true;
        }
        long intervalMillis = (24L * 60L * 60L * 1000L) / countPerDay;
        long lastShowTime = getAppOpenLastShowTime(context);
        return System.currentTimeMillis() - lastShowTime >= intervalMillis;
    }

    public static long getAppOpenLastShowTime(Context context) {
        return context.getSharedPreferences("app_open_ad_preferences", Context.MODE_PRIVATE).getLong("last_show_time", 0);
    }

    public static void setAppOpenLastShowTime(Context context, long time) {
        context.getSharedPreferences("app_open_ad_preferences", Context.MODE_PRIVATE).edit().putLong("last_show_time", time).apply();
    }

    public static void openQuizGameUrl(Activity activity, String url) {
        QuizAds.openLink(activity, url);
    }

    public static void loadRightSwipeInterstitialAd(Activity activity, @Nullable OnInterstitialAdListener onFinished) {
        if (!RemoteConfigValues.getRightSwipeInterstitialAdShow()) {
            notifyComplete(onFinished);
            return;
        }
        int threshold = RemoteConfigValues.getRightSwipeInterstitial();
        if (threshold > 0) {
            if (threshold == rightSwipeInterstitialCount) {
                rightSwipeInterstitialCount = 1;
            } else {
                rightSwipeInterstitialCount++;
                notifyComplete(onFinished);
                return;
            }
        }
        loadInterstitialAdInternal(activity, RemoteConfigValues.getInterAdsId(), onFinished, true);
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

    public static String getNativeAdButtonColor() {
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
        return otherInterstitialId;
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

    public static boolean getAfterDefaultAdShow() {
        return afterDefaultAdShow;
    }

    public static void setAfterDefaultAdShow(boolean value) {
        afterDefaultAdShow = value;
    }

    public static void setAfterDefaultAdType(String value) {
        afterDefaultAdType = value == null || value.isEmpty() ? "inter" : value;
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

    public static boolean getLauncherAppNativeListAdShow() {
        return launcherAppNativeListAdShow;
    }

    public static void setLauncherAppNativeListAdShow(boolean value) {
        launcherAppNativeListAdShow = value;
    }

    public static int getLauncherAppNativeListAdShowPerDay() {
        return launcherAppNativeListAdShowPerDay;
    }

    public static void setLauncherAppNativeListAdShowPerDay(int value) {
        launcherAppNativeListAdShowPerDay = value;
    }

    public static String getLauncherAppNativeListId() {
        return launcherAppNativeListId;
    }

    public static void setLauncherAppNativeListId(String value) {
        launcherAppNativeListId = value == null ? "" : value;
    }

    public static boolean getLauncherAppClickAdShow() {
        return launcherAppClickAdShow;
    }

    public static void setLauncherAppClickAdShow(boolean value) {
        launcherAppClickAdShow = value;
    }

    public static int getLauncherAppCount() {
        return launcherAppCount;
    }

    public static void setLauncherAppCount(int value) {
        launcherAppCount = value;
    }

    public static String getLauncherAppAdType() {
        return launcherAppAdSequence.isEmpty() ? "" : launcherAppAdSequence.get(launcherAppAdSequenceIndex % launcherAppAdSequence.size());
    }

    public static void setLauncherAppAdType(String value) {
        ArrayList<String> types = new ArrayList<>();
        if (value != null && !value.trim().isEmpty()) {
            types.add(value.trim());
        }
        setLauncherAppAdSequence(types);
    }

    public static void setLauncherAppAdSequence(@Nullable List<String> types) {
        launcherAppAdSequence.clear();
        launcherAppAdSequenceIndex = 0;
        if (types == null) {
            return;
        }
        for (String type : types) {
            if (type != null && !type.trim().isEmpty() && !normalizeLauncherAppAdType(type).isEmpty()) {
                launcherAppAdSequence.add(type.trim());
            }
        }
    }

    public static String getLauncherAppInterstitialId() {
        return launcherAppInterstitialId;
    }

    public static void setLauncherAppInterstitialId(String value) {
        launcherAppInterstitialId = value == null ? "" : value;
    }

    public static boolean getLauncherAppBackClickAdShow() {
        return launcherAppBackClickAdShow;
    }

    public static void setLauncherAppBackClickAdShow(boolean value) {
        launcherAppBackClickAdShow = value;
    }

    public static int getLauncherAppBackCount() {
        return launcherAppBackCount;
    }

    public static void setLauncherAppBackCount(int value) {
        launcherAppBackCount = value;
    }

    public static String getLauncherAppBackAdType() {
        return launcherAppBackAdSequence.isEmpty() ? "" : launcherAppBackAdSequence.get(launcherAppBackAdSequenceIndex % launcherAppBackAdSequence.size());
    }

    public static void setLauncherAppBackAdType(String value) {
        ArrayList<String> types = new ArrayList<>();
        if (value != null && !value.trim().isEmpty()) {
            types.add(value.trim());
        }
        setLauncherAppBackAdSequence(types);
    }

    public static void setLauncherAppBackAdSequence(@Nullable List<String> types) {
        launcherAppBackAdSequence.clear();
        launcherAppBackAdSequenceIndex = 0;
        if (types == null) {
            return;
        }
        for (String type : types) {
            if (type != null && !type.trim().isEmpty() && !normalizeLauncherAppAdType(type).isEmpty()) {
                launcherAppBackAdSequence.add(type.trim());
            }
        }
    }

    public static void setLauncherGoogleAdFailedShowQuiz(boolean value) {
        launcherGoogleAdFailedShowQuiz = value;
    }

    public static String getLauncherAppBackInterstitialId() {
        return launcherAppBackInterstitialId;
    }

    public static void setLauncherAppBackInterstitialId(String value) {
        launcherAppBackInterstitialId = value == null ? "" : value;
    }

    public static void persistLauncherBackConfig(@Nullable Context context) {
        if (context == null) {
            return;
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < launcherAppBackAdSequence.size(); i++) {
            if (i > 0) {
                joined.append('|');
            }
            joined.append(launcherAppBackAdSequence.get(i));
        }
        context.getApplicationContext().getSharedPreferences(LAUNCHER_APP_BACK_PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("backConfigSaved", true)
                .putBoolean("backClickAdShow", launcherAppBackClickAdShow)
                .putInt("backCount", launcherAppBackCount)
                .putString("backSequence", joined.toString())
                .putString("backInterstitialId", launcherAppBackInterstitialId == null ? "" : launcherAppBackInterstitialId)
                .putBoolean("backQuizOnFail", launcherGoogleAdFailedShowQuiz)
                .apply();
        launcherBackConfigRestored = true;
    }

    private static void restoreLauncherBackConfig(@Nullable Context context) {
        if (context == null || launcherBackConfigRestored || !launcherAppBackAdSequence.isEmpty()) {
            launcherBackConfigRestored = true;
            return;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(LAUNCHER_APP_BACK_PREFS, Context.MODE_PRIVATE);
        launcherBackConfigRestored = true;
        if (!prefs.getBoolean("backConfigSaved", false)) {
            return;
        }
        launcherAppBackClickAdShow = prefs.getBoolean("backClickAdShow", false);
        launcherAppBackCount = prefs.getInt("backCount", 0);
        launcherAppBackInterstitialId = prefs.getString("backInterstitialId", "");
        launcherGoogleAdFailedShowQuiz = prefs.getBoolean("backQuizOnFail", false);
        String raw = prefs.getString("backSequence", "");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        for (String part : raw.split("\\|")) {
            if (part != null && !part.trim().isEmpty()) {
                launcherAppBackAdSequence.add(part.trim());
            }
        }
    }

    public static void applyAfterDefaultAdConfig(org.json.JSONObject permissionDefaultScreen) {
        boolean show = permissionDefaultScreen.optBoolean("After_Default_Ad_Show", permissionDefaultScreen.optBoolean("After_default_Ad_Show", false));
        String type;
        if (permissionDefaultScreen.has("After_Default_Ad_Type")) {
            type = permissionDefaultScreen.optString("After_Default_Ad_Type", "inter");
        } else {
            type = permissionDefaultScreen.optString("After_default_Ad_Type", "inter");
        }
        setAfterDefaultAdShow(show);
        setAfterDefaultAdType(type);
    }

    public static void showSlot(Activity activity, boolean showFlag, String adType, String bannerId, String nativeId, String nativeSize, RelativeLayout rlAdView, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd, boolean adaptiveBanner) {
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
            loadNativeAd(activity, nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, nativeSize);
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
                adView.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
                slBannerShimmer.setVisibility(View.GONE);
                llBannerAd.setVisibility(View.VISIBLE);
                rlBannerAdView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
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
        loadAdaptiveBannerAd(activity, bannerId, rlBannerAdView, slBannerShimmer, llBannerAd, false);
    }

    public static void loadAdaptiveBannerAd(Activity activity, String bannerId, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd, boolean googleOnly) {
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
        adView.setAdSize(getAdaptiveAdSize(activity));
        adView.setAdUnitId(unitId);
        llBannerAd.addView(adView);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                adView.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
                if (slBannerShimmer != null) {
                    slBannerShimmer.setVisibility(View.GONE);
                }
                llBannerAd.setVisibility(View.VISIBLE);
                rlBannerAdView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                adView.destroy();
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
        if (activity != null) {
            initializeIfConfigured(activity);
        }
        String unitId = nativeId == null ? "" : nativeId.trim();
        if (!googleOnly && shouldUseQuizPriority()) {
            if (!QuizAds.showNative(activity, rlNativeAdView, slNativeShimmer, flNativeAd, type)) {
                hideNativeContainer(rlNativeAdView, slNativeShimmer, flNativeAd);
                if (onAdFailed != null) {
                    onAdFailed.run();
                }
            } else if (onAdLoaded != null) {
                onAdLoaded.accept(null);
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
        if (slNativeShimmer != null) {
            slNativeShimmer.setVisibility(View.VISIBLE);
            slNativeShimmer.startShimmer();
        }
        AdLoader adLoader = new AdLoader.Builder(activity, unitId).forNativeAd(nativeAd -> {
            if (activity.isFinishing() || activity.isDestroyed() || flNativeAd == null) {
                nativeAd.destroy();
                return;
            }
            nativeAd.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
            NativeAdView adView = (NativeAdView) LayoutInflater.from(flNativeAd.getContext()).inflate(resolveGoogleNativeLayout(type), flNativeAd, false);
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
            if (onAdLoaded != null) {
                onAdLoaded.accept(nativeAd);
            }
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                if (!googleOnly && getGoogleAdFailedShowQuiz() && QuizAds.showNative(activity, rlNativeAdView, slNativeShimmer, flNativeAd, type)) {
                    if (onAdLoaded != null) {
                        onAdLoaded.accept(null);
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
            nativeAd.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
            if (onLoaded != null) {
                onLoaded.accept(nativeAd);
            }
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                if (onFailed != null) {
                    onFailed.run();
                }
            }
        }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void requestSmallBannerAd(Activity activity, String unitId, @Nullable java.util.function.Consumer<AdView> onLoaded, @Nullable Runnable onFailed) {
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
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(id);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                adView.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
                if (onLoaded != null) {
                    onLoaded.accept(adView);
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                adView.destroy();
                if (onFailed != null) {
                    onFailed.run();
                }
            }
        });
        adView.loadAd(new AdRequest.Builder().build());
    }

    public static void showLargeNative(Activity activity, FrameLayout container, NativeAd nativeAd) {
        if (activity == null || activity.isFinishing() || container == null || nativeAd == null) {
            return;
        }
        NativeAdView adView = (NativeAdView) LayoutInflater.from(container.getContext()).inflate(R.layout.native_large_ad_layout, container, false);
        populateNativeAdView(nativeAd, adView, "large");
        container.removeAllViews();
        container.addView(GestureSafeNativeAdView.wrap(adView));
        container.setVisibility(View.VISIBLE);
    }

    public static void loadLanguageInterstitialAd(Activity activity, OnInterstitialAdListener listener) {
        if (!getLanguageInterstitialAdShow()) {
            notifyComplete(listener);
            return;
        }
        loadInterstitialAdInternal(activity, getOtherInterstitialId(), listener, true);
    }

    public static void loadIntroInterstitialAd(Activity activity, OnInterstitialAdListener listener) {
        if (!getIntroInterstitialAdShow()) {
            notifyComplete(listener);
            return;
        }
        loadInterstitialAdInternal(activity, getOtherInterstitialId(), listener, true);
    }

    public static void preloadAfterDefaultAd(Context context) {
        if (context == null || !getAfterDefaultAdShow() || shouldUseQuizPriority() || !canRequestAds(context) || !isNetworkAvailable(context)) {
            return;
        }
        if (isAfterDefaultAppOpenType()) {
            if (preloadedAfterDefaultAppOpen != null || afterDefaultAdPreloadInProgress || getAppOpenId().isEmpty()) {
                return;
            }
            afterDefaultAdPreloadInProgress = true;
            int token = afterDefaultPreloadToken.get();
            AppOpenAd.load(context, getAppOpenId(), new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    afterDefaultAdPreloadInProgress = false;
                    if (token == afterDefaultPreloadToken.get()) {
                        preloadedAfterDefaultAppOpen = ad;
                    }
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    afterDefaultAdPreloadInProgress = false;
                }
            });
            return;
        }
        if (preloadedAfterDefaultInterstitial != null || afterDefaultAdPreloadInProgress || getOtherInterstitialId().isEmpty()) {
            return;
        }
        afterDefaultAdPreloadInProgress = true;
        int token = afterDefaultPreloadToken.get();
        InterstitialAd.load(context, getOtherInterstitialId(), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                afterDefaultAdPreloadInProgress = false;
                if (token == afterDefaultPreloadToken.get()) {
                    preloadedAfterDefaultInterstitial = ad;
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                afterDefaultAdPreloadInProgress = false;
            }
        });
    }

    public static void loadAfterDefaultAd(Activity activity, OnInterstitialAdListener listener) {
        if (activity == null || activity.isFinishing() || !getAfterDefaultAdShow() || shouldUseQuizPriority()) {
            notifyComplete(listener);
            return;
        }
        if (isAfterDefaultAppOpenType()) {
            if (tryShowPreloadedAfterDefaultAppOpen(activity, listener)) {
                return;
            }
            clearAfterDefaultPreloadCache();
            loadAppOpenAdInternal(activity, getAppOpenId(), listener);
            return;
        }
        if (getOtherInterstitialId().isEmpty()) {
            notifyComplete(listener);
            return;
        }
        if (tryShowPreloadedAfterDefaultInterstitial(activity, listener)) {
            return;
        }
        clearAfterDefaultPreloadCache();
        loadInterstitialAdInternal(activity, getOtherInterstitialId(), listener, false);
    }

    public static void handleLauncherAppClickAd(@Nullable Activity activity, @Nullable Runnable openSelectedApp) {
        if (openSelectedApp == null) {
            return;
        }
        if (activity == null || activity.isFinishing() || !getLauncherAppClickAdShow() || getLauncherAppCount() <= 0) {
            openSelectedApp.run();
            return;
        }
        if (getLauncherAppCount() == launcherAppClickCount) {
            launcherAppClickCount = 1;
            executeLauncherAppAd(activity, openSelectedApp, nextLauncherAppAdType(false), getLauncherAppInterstitialId());
            return;
        }
        launcherAppClickCount++;
        openSelectedApp.run();
    }

    public static void markLauncherExternalAppLaunched(@Nullable Context context, @Nullable String packageName) {
        if (context == null) {
            return;
        }
        String launched = packageName == null ? "" : packageName.trim();
        context.getApplicationContext().getSharedPreferences(LAUNCHER_APP_BACK_PREFS, Context.MODE_PRIVATE).edit().putBoolean("waitingForLauncherAppReturn", true).putString("lastLaunchedPackage", launched).apply();
    }

    public static void handleLauncherAppReturnAd(@Nullable Activity activity) {
        if (!(activity instanceof LauncherHomeActivity) || activity.isFinishing()) {
            return;
        }
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(LAUNCHER_APP_BACK_PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("waitingForLauncherAppReturn", false)) {
            return;
        }
        String lastLaunchedPackage = prefs.getString("lastLaunchedPackage", "");
        prefs.edit().putBoolean("waitingForLauncherAppReturn", false).apply();
        if (lastLaunchedPackage == null || lastLaunchedPackage.trim().isEmpty()) {
            return;
        }
        restoreLauncherBackConfig(activity);
        if (!getLauncherAppBackClickAdShow() || getLauncherAppBackCount() <= 0) {
            return;
        }
        showJoinedLauncherBackAd(activity, () -> {
        });
    }

    /** Same counter, ad sequence, and interstitial id as the launcher back flow. */
    public static void showJoinedLauncherBackAd(@Nullable Activity activity, @Nullable Runnable after) {
        Runnable done = after == null ? () -> {
        } : after;
        if (activity == null || activity.isFinishing()) {
            done.run();
            return;
        }
        restoreLauncherBackConfig(activity);
        if (getLauncherAppBackCount() <= 0) {
            done.run();
            return;
        }
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(LAUNCHER_APP_BACK_PREFS, Context.MODE_PRIVATE);
        int returnCount = prefs.getInt("launcherAppReturnCount", 0) + 1;
        boolean showReturnAd = returnCount >= getLauncherAppBackCount();
        if (showReturnAd) {
            returnCount = 0;
        }
        prefs.edit().putInt("launcherAppReturnCount", returnCount).apply();
        if (!showReturnAd) {
            done.run();
            return;
        }
        String adType = nextLauncherAppAdType(true);
        if (adType.isEmpty()) {
            done.run();
            return;
        }
        executeLauncherAppAd(activity, done, adType, getLauncherAppBackInterstitialId());
    }

    public static boolean canShowLauncherAppNativeListAd(Context context) {
        int countPerDay = getLauncherAppNativeListAdShowPerDay();
        if (countPerDay <= 0) {
            return true;
        }
        long intervalMillis = (24L * 60L * 60L * 1000L) / countPerDay;
        long lastShowTime = context.getSharedPreferences("launcher_app_native_list_ad_preferences", Context.MODE_PRIVATE).getLong("last_show_time", 0);
        return System.currentTimeMillis() - lastShowTime >= intervalMillis;
    }

    public static void setLauncherAppNativeListLastShowTime(Context context, long time) {
        context.getSharedPreferences("launcher_app_native_list_ad_preferences", Context.MODE_PRIVATE).edit().putLong("last_show_time", time).apply();
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

    private static String nextLauncherAppAdType(boolean back) {
        ArrayList<String> sequence = back ? launcherAppBackAdSequence : launcherAppAdSequence;
        if (sequence.isEmpty()) {
            return "";
        }
        int index = back ? launcherAppBackAdSequenceIndex : launcherAppAdSequenceIndex;
        String type = normalizeLauncherAppAdType(sequence.get(index % sequence.size()));
        int next = (index + 1) % sequence.size();
        if (back) {
            launcherAppBackAdSequenceIndex = next;
        } else {
            launcherAppAdSequenceIndex = next;
        }
        return type;
    }

    private static void executeLauncherAppAd(Activity activity, Runnable continueAction, String adType, @Nullable String interstitialId) {
        if (LAUNCHER_APP_AD_TYPE_GOOGLE_INTER.equals(adType)) {
            loadInterstitialAdInternal(activity, interstitialId, () -> continueAction.run(), true, false, launcherGoogleAdFailedShowQuiz);
            return;
        }
        if (LAUNCHER_APP_AD_TYPE_GOOGLE_APP_OPEN.equals(adType)) {
            loadAppOpenAdInternal(activity, getAppOpenId(), () -> continueAction.run(), false, launcherGoogleAdFailedShowQuiz);
            return;
        }
        if (LAUNCHER_APP_AD_TYPE_GOOGLE_NATIVE.equals(adType)) {
            showLauncherNativeFull(activity, continueAction, launcherGoogleAdFailedShowQuiz);
            return;
        }
        if (LAUNCHER_APP_AD_TYPE_QUIZ_INTER.equals(adType)) {
            if (!QuizAds.showInterstitial(activity, continueAction)) {
                continueAction.run();
            }
            return;
        }
        if (LAUNCHER_APP_AD_TYPE_QUIZ_APP_OPEN.equals(adType)) {
            if (!QuizAds.showAppOpen(activity, continueAction)) {
                continueAction.run();
            }
            return;
        }
        if (LAUNCHER_APP_AD_TYPE_QUIZ_NATIVE.equals(adType)) {
            if (!QuizAds.showNativeFull(activity, continueAction)) {
                continueAction.run();
            }
            return;
        }
        if (LAUNCHER_APP_AD_TYPE_QUIZ_BROWSER.equals(adType)) {
            QuizAds.openBrowserThenContinue(activity, continueAction);
            return;
        }
        continueAction.run();
    }

    private static void showLauncherNativeFull(Activity activity, Runnable continueAction, boolean quizOnFail) {
        String nativeId = getLauncherAppNativeId();
        if (!canLoad(activity, nativeId)) {
            continueAction.run();
            return;
        }
        AtomicBoolean completed = new AtomicBoolean(false);
        OnInterstitialAdListener listener = () -> continueAction.run();
        Dialog dialog = showInterstitialLoadingDialog(activity);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeout = () -> {
            dismissInterstitialLoadingDialog(dialog);
            notifyComplete(listener, completed);
        };
        handler.postDelayed(timeout, INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS);
        AdLoader loader = new AdLoader.Builder(activity, nativeId).forNativeAd(nativeAd -> {
            handler.removeCallbacks(timeout);
            dismissInterstitialLoadingDialog(dialog);
            if (activity.isFinishing()) {
                nativeAd.destroy();
                notifyComplete(listener, completed);
                return;
            }
            Dialog full = new Dialog(activity);
            full.requestWindowFeature(Window.FEATURE_NO_TITLE);
            NativeAdView adView = (NativeAdView) LayoutInflater.from(AdTheme.forLauncher(activity)).inflate(R.layout.native_full_ad_layout, null);
            populateNativeAdView(nativeAd, adView, "full");
            View close = adView.findViewById(R.id.ivClose);
            if (close != null) {
                close.setOnClickListener(v -> full.dismiss());
            }
            full.setContentView(adView);
            full.setOnDismissListener(d -> {
                nativeAd.destroy();
                notifyComplete(listener, completed);
            });
            try {
                full.show();
            } catch (Exception e) {
                nativeAd.destroy();
                notifyComplete(listener, completed);
            }
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                handler.removeCallbacks(timeout);
                dismissInterstitialLoadingDialog(dialog);
                if (quizOnFail && QuizAds.showNativeFull(activity, () -> notifyComplete(listener, completed))) {
                    return;
                }
                notifyComplete(listener, completed);
            }
        }).build();
        loader.loadAd(new AdRequest.Builder().build());
    }

    private static String normalizeLauncherAppAdType(@Nullable String adType) {
        if (adType == null) {
            return "";
        }
        String value = adType.trim();
        if ("Google_Inter".equalsIgnoreCase(value) || "google_inter".equalsIgnoreCase(value) || "inter".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_GOOGLE_INTER;
        }
        if ("Google_App_Open".equalsIgnoreCase(value) || "google_app_open".equalsIgnoreCase(value) || "app_open".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_GOOGLE_APP_OPEN;
        }
        if ("Google_Native".equalsIgnoreCase(value) || "google_native".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_GOOGLE_NATIVE;
        }
        if ("Quiz_Inter".equalsIgnoreCase(value) || "quiz_inter".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_INTER;
        }
        if ("Quiz_App_Open".equalsIgnoreCase(value) || "quiz_app_open".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_APP_OPEN;
        }
        if ("Quiz_Native".equalsIgnoreCase(value) || "quiz_native".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_NATIVE;
        }
        if ("Quiz_Browser".equalsIgnoreCase(value) || "quiz_browser".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_BROWSER;
        }
        return "";
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
            notifyComplete(listener, completed);
        };
        if (loadingDialog != null) {
            handler.postDelayed(timeout, INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS);
        }
        InterstitialAd.load(activity, interstitialId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
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
                        notifyComplete(listener, completed);
                    }
                });
                ad.show(activity);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
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
        loadAppOpenAdInternal(activity, appOpenUnitId, listener, true, getGoogleAdFailedShowQuiz());
    }

    private static void loadAppOpenAdInternal(Activity activity, String appOpenUnitId, @Nullable OnInterstitialAdListener listener, boolean honorQuizPriority, boolean quizOnFail) {
        int token = appOpenLoadToken.incrementAndGet();
        AtomicBoolean completed = new AtomicBoolean(false);
        if (honorQuizPriority && shouldUseQuizPriority()) {
            if (!QuizAds.showAppOpen(activity, () -> notifyComplete(listener, completed))) {
                notifyComplete(listener, completed);
            }
            return;
        }
        if (!canLoad(activity, appOpenUnitId)) {
            notifyComplete(listener, completed);
            return;
        }
        AppOpenAd.load(activity, appOpenUnitId, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                if (token != appOpenLoadToken.get() || activity.isFinishing()) {
                    notifyComplete(listener, completed);
                    return;
                }
                ad.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        notifyComplete(listener, completed);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        notifyComplete(listener, completed);
                    }
                });
                ad.show(activity);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                if (token != appOpenLoadToken.get()) {
                    return;
                }
                if (quizOnFail && QuizAds.showAppOpen(activity, () -> notifyComplete(listener, completed))) {
                    return;
                }
                notifyComplete(listener, completed);
            }
        });
    }

    private static boolean tryShowPreloadedAfterDefaultInterstitial(Activity activity, @Nullable OnInterstitialAdListener listener) {
        if (preloadedAfterDefaultInterstitial == null || shouldUseQuizPriority()) {
            return false;
        }
        InterstitialAd ad = preloadedAfterDefaultInterstitial;
        preloadedAfterDefaultInterstitial = null;
        AtomicBoolean completed = new AtomicBoolean(false);
        ad.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                notifyComplete(listener, completed);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                notifyComplete(listener, completed);
            }
        });
        ad.show(activity);
        return true;
    }

    private static boolean tryShowPreloadedAfterDefaultAppOpen(Activity activity, @Nullable OnInterstitialAdListener listener) {
        if (preloadedAfterDefaultAppOpen == null || shouldUseQuizPriority()) {
            return false;
        }
        AppOpenAd ad = preloadedAfterDefaultAppOpen;
        preloadedAfterDefaultAppOpen = null;
        AtomicBoolean completed = new AtomicBoolean(false);
        ad.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                notifyComplete(listener, completed);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                notifyComplete(listener, completed);
            }
        });
        ad.show(activity);
        return true;
    }

    private static void clearAfterDefaultPreloadCache() {
        afterDefaultPreloadToken.incrementAndGet();
        afterDefaultAdPreloadInProgress = false;
        preloadedAfterDefaultInterstitial = null;
        preloadedAfterDefaultAppOpen = null;
    }

    private static boolean isAfterDefaultAppOpenType() {
        String type = afterDefaultAdType == null ? "" : afterDefaultAdType;
        String normalized = type.trim().toLowerCase(Locale.US).replace("_", "").replace("-", "").replace(" ", "");
        return "appopen".equals(normalized);
    }

    public static void populateNativeAdView(NativeAd nativeAd, NativeAdView adView, String type) {
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
            applyBackgroundColor(button, nativeAdButtonColor, R.color.primary);
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
        return getAdSize(activity);
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
    private static Dialog showInterstitialLoadingDialog(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        try {
            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_loading_ads);
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

    private static void dismissInterstitialLoadingDialog(@Nullable Dialog dialog) {
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

    private static boolean callEndIpLookupStarted;
    public static final String IP_COUNTRY_NAME_KEY = "ip_country_name";

    public static void ensureClEndConfig(@Nullable Context context) {
        RemoteConfigValues.ensureLoaded(context);
    }

    public static boolean getClEndScreenShow() {
        return RemoteConfigValues.getClEndScreenShow();
    }

    public static boolean getClEndAdShow() {
        return RemoteConfigValues.getClEndAdShow();
    }

    @NonNull
    public static String getClEndAdType() {
        return RemoteConfigValues.getClEndAdType();
    }

    @NonNull
    public static String getClEndBannerId() {
        return RemoteConfigValues.getClEndBannerId();
    }

    @NonNull
    public static String getClEndNativeId() {
        return RemoteConfigValues.getClEndNativeId();
    }

    public static boolean getClEndBackAdShow() {
        return RemoteConfigValues.getClEndBackAdShow();
    }

    @NonNull
    public static String getClEndBackAdType() {
        return RemoteConfigValues.getClEndBackAdType();
    }

    public static int getClEndBackAdShowAfterDay() {
        return RemoteConfigValues.getClEndBackAdShowAfterDay();
    }

    public static int getClEndBackAdShowPerDay() {
        return RemoteConfigValues.getClEndBackAdShowPerDay();
    }

    @NonNull
    public static String getClEndBackAdInterstitialId() {
        return RemoteConfigValues.getClEndBackAdInterstitialId();
    }

    @NonNull
    public static String getClEndBackAdNativeId() {
        return RemoteConfigValues.getClEndBackAdNativeId();
    }

    @NonNull
    public static List<String[]> getClEndBackAdSequence() {
        return RemoteConfigValues.getClEndBackAdSequence();
    }

    public static boolean getClEndBackAdCountryIP() {
        return RemoteConfigValues.getClEndBackAdCountryIP();
    }

    @NonNull
    public static ArrayList<String> getClEndBackAdShowCountryList() {
        return RemoteConfigValues.getClEndBackAdShowCountryList();
    }

    public static boolean getNotificationCloseButtonShow() {
        return RemoteConfigValues.getNotificationCloseButtonShow();
    }

    public static boolean getNotificationBackAdShow() {
        return RemoteConfigValues.getNotificationBackAdShow();
    }

    public static boolean getAllAllowPermissionShowNotification() {
        return RemoteConfigValues.getAllAllowPermissionShowNotification();
    }

    public static int getNotificationInstallDays() {
        return RemoteConfigValues.getNotificationInstallDays();
    }

    public static int getNotificationCallInstallDays() {
        return RemoteConfigValues.getNotificationCallInstallDays();
    }

    public static int getNotificationCallOverlayInstallDays() {
        return RemoteConfigValues.getNotificationCallOverlayInstallDays();
    }

    @NonNull
    public static ArrayList<String> getNotificationCountryList() {
        return RemoteConfigValues.getNotificationCountryList();
    }

    @NonNull
    public static ArrayList<String> getNotificationCallCountryList() {
        return RemoteConfigValues.getNotificationCallCountryList();
    }

    @NonNull
    public static ArrayList<String> getNotificationCallOverlayCountryList() {
        return RemoteConfigValues.getNotificationCallOverlayCountryList();
    }

    public static void requestCallEndIpCountryIfNeeded(@Nullable Context context) {
        if (context == null) {
            return;
        }
        ensureClEndConfig(context);
        if (!getClEndBackAdCountryIP() || callEndIpLookupStarted || !getIpCountryName(context).isEmpty()) {
            return;
        }
        callEndIpLookupStarted = true;
        Context appContext = context.getApplicationContext();
        IPAddressHelper.getCountryName(new IPAddressHelper.IPCallback() {
            @Override
            public void onResponse(String countryName) {
                if (countryName != null && !countryName.isEmpty()) {
                    setIpCountryName(appContext, countryName);
                } else {
                    callEndIpLookupStarted = false;
                }
            }

            @Override
            public void onFailure(Exception e) {
                callEndIpLookupStarted = false;
            }
        });
    }

    public static boolean canShowClEndBackAdOnPress(Context context, boolean notificationFlow, boolean fullAdVisible) {
        if (context == null || !isNetworkAvailable(context) || !isClEndBackAdIntervalElapsed(context)) {
            return false;
        }
        if (notificationFlow) {
            if (!fullAdVisible) {
                return false;
            }
            return getNotificationBackAdShow() && meetsNotificationInstallDayRequirements(context);
        }
        if (!getClEndBackAdShow()) {
            return false;
        }
        int dayCount = getClEndBackAdShowAfterDay();
        if (dayCount <= 0) {
            return false;
        }
        if (getDaysSinceInstall(context) < dayCount) {
            return false;
        }
        return isCountryAllowedForCallEnd(context);
    }

    private static boolean isClEndBackAdIntervalElapsed(Context context) {
        int activeShowCount = getClEndBackAdShowPerDay();
        if (activeShowCount <= 0) {
            return false;
        }
        long intervalMillis = (24L * 60L * 60L * 1000L) / activeShowCount;
        return (System.currentTimeMillis() - getClEndLastShowTime(context)) >= intervalMillis;
    }

    public static boolean meetsNotificationInstallDayRequirements(Context context) {
        if (context == null) {
            return false;
        }
        boolean notification = isNotificationGranted(context);
        boolean call = isCallStateGranted(context);
        boolean overlay = isOverlayGranted(context);
        long installDays = getDaysSinceInstall(context);
        String currentCountry = getDeviceCountry(context);
        if (notification && call && overlay) {
            if (!getAllAllowPermissionShowNotification()) {
                return false;
            }
            return checkDaysAndCountry(installDays, getNotificationCallOverlayInstallDays(), currentCountry, getNotificationCallOverlayCountryList(), true);
        } else if (notification && call) {
            return checkDaysAndCountry(installDays, getNotificationCallInstallDays(), currentCountry, getNotificationCallCountryList(), true);
        } else if (notification) {
            return checkDaysAndCountry(installDays, getNotificationInstallDays(), currentCountry, getNotificationCountryList(), true);
        }
        return false;
    }

    public static long getDaysSinceInstall(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("appInstallDate", Context.MODE_PRIVATE);
        String installDate = sharedPreferences.getString("install_date", "");
        if (installDate.isEmpty()) {
            return 1;
        }
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date1 = simpleDateFormat.parse(installDate);
            Date date2 = new Date();
            if (date1 == null) {
                return 1;
            }
            long diff = date2.getTime() - date1.getTime();
            return (diff / (24 * 60 * 60 * 1000)) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    public static boolean isCountryAllowedForCallEnd(Context context) {
        List<String> allowedCountries = getClEndBackAdShowCountryList();
        if (allowedCountries == null || allowedCountries.isEmpty()) {
            return true;
        }
        String currentCountry = getDeviceCountry(context);
        for (String country : allowedCountries) {
            if (country.equalsIgnoreCase(currentCountry)) {
                return true;
            }
        }
        return false;
    }

    public static long getClEndLastShowTime(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cl_end_ad_preferences", Context.MODE_PRIVATE);
        return sharedPreferences.getLong("last_show_time", 0);
    }

    public static void setClEndLastShowTime(Context context, long time) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cl_end_ad_preferences", Context.MODE_PRIVATE);
        sharedPreferences.edit().putLong("last_show_time", time).apply();
    }

    public static String getDeviceCountry(Context context) {
        if (getClEndBackAdCountryIP() && !getIpCountryName(context).isEmpty()) {
            return getIpCountryName(context).toUpperCase();
        }
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String countryCode = telephonyManager != null ? telephonyManager.getNetworkCountryIso() : "";
            if (countryCode == null || countryCode.isEmpty()) {
                countryCode = telephonyManager != null ? telephonyManager.getSimCountryIso() : "";
            }
            if (countryCode == null || countryCode.isEmpty()) {
                countryCode = context.getResources().getConfiguration().locale.getCountry();
            }
            if (!countryCode.isEmpty()) {
                Locale locale = new Locale("", countryCode);
                return locale.getDisplayCountry(Locale.ENGLISH).toUpperCase();
            }
            return "";
        } catch (Exception e1) {
            try {
                Locale locale = context.getResources().getConfiguration().locale;
                return locale.getDisplayCountry(Locale.ENGLISH).toUpperCase();
            } catch (Exception e2) {
                return "";
            }
        }
    }

    public static String getIpCountryName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("ipCountryName", Context.MODE_PRIVATE);
        return sharedPreferences.getString(IP_COUNTRY_NAME_KEY, "");
    }

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

    public static boolean isCallStateGranted(Context context) {
        if (context == null) {
            return false;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isOverlayGranted(Context context) {
        return AppUtils.hasOverlayPermission(context);
    }

    public static boolean isCallEndPerformanceAllowed(Context context, boolean isFcmTrigger) {
        if (!getClEndAdShow()) {
            return false;
        }
        boolean notification = isNotificationGranted(context);
        boolean call = isCallStateGranted(context);
        boolean overlay = isOverlayGranted(context);
        long installDays = getDaysSinceInstall(context);
        String currentCountry = getDeviceCountry(context);
        if (notification && call && overlay) {
            if (isFcmTrigger && !getAllAllowPermissionShowNotification()) {
                return false;
            }
            return checkDaysAndCountry(installDays, getNotificationCallOverlayInstallDays(), currentCountry, getNotificationCallOverlayCountryList(), isFcmTrigger);
        } else if (notification && call) {
            return checkDaysAndCountry(installDays, getNotificationCallInstallDays(), currentCountry, getNotificationCallCountryList(), isFcmTrigger);
        } else if (notification) {
            if (!isFcmTrigger) {
                return false;
            }
            return checkDaysAndCountry(installDays, getNotificationInstallDays(), currentCountry, getNotificationCountryList(), isFcmTrigger);
        }
        return false;
    }

    public static boolean isNotificationFullAdFlow(@Nullable Intent intent, @Nullable String callType) {
        if (intent == null) {
            return false;
        }
        return intent.getBooleanExtra("is_from_fcm", false) || "Notification".equalsIgnoreCase(callType) || "call_end".equalsIgnoreCase(callType);
    }

    private static boolean checkDaysAndCountry(long currentDays, int requiredDays, String currentCountry, List<String> allowedCountries, boolean isFcmTrigger) {
        if (!isFcmTrigger) {
            return true;
        }
        if (requiredDays > 0 && currentDays < requiredDays) {
            return false;
        }
        if (allowedCountries == null || allowedCountries.isEmpty()) {
            return true;
        }
        for (String country : allowedCountries) {
            if (country != null && country.equalsIgnoreCase(currentCountry)) {
                return true;
            }
        }
        return false;
    }
}
