package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.qrcode.scanner.app.R;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("all")
public class CallEndBackAd {
    private static final String TAG = "CL_END_BACK_AD";
    private static final String TYPE_INTER = "inter";
    private static final String TYPE_APP_OPEN = "appopen";
    private static final String TYPE_NATIVE = "native";
    private static final String TYPE_ALTERNATE = "alternate";
    private static final String SEQUENCE_PREFS = "cl_end_ad_preferences";
    private static final String SEQUENCE_CURSOR = "back_ad_sequence_cursor";
    private static final long READY_WAIT_MS = 8000L;
    private static final long READY_POLL_MS = 250L;

    public static int adsClickEvent = 0;
    public static int adsBackClick = 0;
    public static Boolean isAdsShowEnable = false;
    public static int fullScreenAdsPosition = 0;
    public static boolean fullScreenAdsFailed = false;
    public static OnCompeteAds onCompleteAdCallBack;
    public static boolean isAdsEnabled = true;
    private static InterstitialAd fullScreenAds;
    private static AppOpenAd appOpenAd;
    private static NativeAd nativeAd;
    private static boolean interstitialLoading;
    private static boolean appOpenLoading;
    private static boolean nativeLoading;
    public static boolean isAdShowing = true;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void fullScreenAdShow(Activity context, OnCompeteAds onFinishAd, boolean... doShowAds) {
        onCompleteAdCallBack = onFinishAd;
        dropUnconfiguredAds();
        String showType = resolveShowType(context);
        Log.d(TAG, "show dueType=" + showType
                + " interReady=" + (fullScreenAds != null)
                + " appOpenReady=" + (appOpenAd != null)
                + " nativeReady=" + (nativeAd != null)
                + " cursor=" + (context != null ? readSequenceCursor(context) : -1));
        if (showType.isEmpty()) {
            failShow(context);
            return;
        }
        if (isAdReady(showType)) {
            showByType(context, showType);
            return;
        }
        waitAndShow(context, showType);
    }

    private static void showByType(Activity context, String showType) {
        if (TYPE_APP_OPEN.equals(showType)) {
            admobAppOpenAd(context);
        } else if (TYPE_NATIVE.equals(showType)) {
            admobNativeFullAd(context);
        } else if (TYPE_INTER.equals(showType)) {
            admobFullScreenAd(context);
        } else {
            failShow(context);
        }
    }

    private static void waitAndShow(Activity context, String showType) {
        if (context == null || context.isFinishing()) {
            failShow(context);
            return;
        }
        preloadIfConfigured(context, showType);
        Dialog loading = showLoadingDialog(context);
        AtomicBoolean finished = new AtomicBoolean(false);
        long deadline = System.currentTimeMillis() + READY_WAIT_MS;
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (finished.get()) {
                    return;
                }
                if (context.isFinishing() || context.isDestroyed()) {
                    if (finished.compareAndSet(false, true)) {
                        dismissDialog(loading);
                        failShow(context);
                    }
                    return;
                }
                if (isAdReady(showType)) {
                    if (finished.compareAndSet(false, true)) {
                        dismissDialog(loading);
                        showByType(context, showType);
                    }
                    return;
                }
                if (System.currentTimeMillis() >= deadline) {
                    if (finished.compareAndSet(false, true)) {
                        dismissDialog(loading);
                        Log.d(TAG, "wait timeout for " + showType);
                        failShow(context);
                    }
                    return;
                }
                if (!isLoading(showType)) {
                    preloadIfConfigured(context, showType);
                }
                mainHandler.postDelayed(this, READY_POLL_MS);
            }
        };
        mainHandler.postDelayed(poll, READY_POLL_MS);
    }

    private static boolean isLoading(String type) {
        String normalized = normalizeAdType(type);
        if (TYPE_INTER.equals(normalized)) {
            return interstitialLoading;
        }
        if (TYPE_APP_OPEN.equals(normalized)) {
            return appOpenLoading;
        }
        if (TYPE_NATIVE.equals(normalized)) {
            return nativeLoading;
        }
        return false;
    }

    @Nullable
    private static Dialog showLoadingDialog(Activity activity) {
        try {
            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(LayoutInflater.from(AdTheme.forApp(activity)).inflate(R.layout.dialog_loading_ads, null, false));
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            }
            dialog.show();
            return dialog;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void dismissDialog(@Nullable Dialog dialog) {
        if (dialog == null) {
            return;
        }
        try {
            dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    private static void failShow(@Nullable Activity context) {
        adsShowCheckEvent(true);
        if (context != null) {
            loadAd(context);
        }
        if (onCompleteAdCallBack != null) {
            OnCompeteAds callback = onCompleteAdCallBack;
            onCompleteAdCallBack = null;
            callback.onCompeteAds(false);
        }
    }

    public static void admobAppOpenAd(Activity context) {
        if (appOpenAd != null) {
            adsShowCheckEvent(false);
            try {
                AppOpenAd ad = appOpenAd;
                appOpenAd = null;
                ad.show(context);
            } catch (Exception e) {
                appOpenAd = null;
                adsShowCheckEvent(true);
                preloadIfConfigured(context, TYPE_APP_OPEN);
                if (onCompleteAdCallBack != null) {
                    OnCompeteAds callback = onCompleteAdCallBack;
                    onCompleteAdCallBack = null;
                    callback.onCompeteAds(false);
                }
            }
        } else {
            failShow(context);
        }
    }

    public static void admobFullScreenAd(Activity context) {
        if (fullScreenAds != null) {
            adsShowCheckEvent(false);
            try {
                InterstitialAd ad = fullScreenAds;
                fullScreenAds = null;
                ad.show(context);
            } catch (Exception e) {
                fullScreenAds = null;
                adsShowCheckEvent(true);
                preloadIfConfigured(context, TYPE_INTER);
                if (onCompleteAdCallBack != null) {
                    OnCompeteAds callback = onCompleteAdCallBack;
                    onCompleteAdCallBack = null;
                    callback.onCompeteAds(false);
                }
            }
        } else {
            failShow(context);
        }
    }

    private static void admobNativeFullAd(Activity context) {
        NativeAd ad = nativeAd;
        if (ad == null || context == null) {
            failShow(context);
            return;
        }
        nativeAd = null;
        adsShowCheckEvent(false);
        AtomicBoolean completed = new AtomicBoolean(false);
        try {
            Dialog dialog = new Dialog(context, R.style.Theme_NativeFullAd);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            NativeAdView adView = (NativeAdView) LayoutInflater.from(AdTheme.forApp(context)).inflate(R.layout.native_full_ad_layout, null);
            AdPlacement.populateNativeAdView(ad, adView, "full");
            View close = adView.findViewById(R.id.ivClose);
            if (close != null) {
                close.setOnClickListener(v -> {
                    try {
                        dialog.dismiss();
                    } catch (Exception ignored) {
                    }
                });
            }
            dialog.setContentView(adView);
            dialog.setCancelable(true);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.setOnDismissListener(d -> finishNativeShow(context, ad, completed, true));
            dialog.show();
        } catch (Exception e) {
            finishNativeShow(context, ad, completed, false);
        }
    }

    private static void finishNativeShow(Activity context, NativeAd ad, AtomicBoolean completed, boolean shown) {
        if (ad == null || completed == null || !completed.compareAndSet(false, true)) {
            return;
        }
        try {
            ad.destroy();
        } catch (Exception ignored) {
        }
        adsShowCheckEvent(true);
        if (shown) {
            advanceAlternateSequence(context);
        }
        preloadIfConfigured(context, TYPE_NATIVE);
        if (onCompleteAdCallBack != null) {
            OnCompeteAds callback = onCompleteAdCallBack;
            onCompleteAdCallBack = null;
            callback.onCompeteAds(shown);
        }
    }

    public static void admobAppOpenAdLoad(Activity context) {
        if (!isTypeConfigured(TYPE_APP_OPEN) || context == null || appOpenAd != null || appOpenLoading) {
            return;
        }

        String adUnitId = unitIdFor(TYPE_APP_OPEN);
        if (adUnitId.isEmpty()) {
            return;
        }

        appOpenLoading = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(context, adUnitId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                appOpenLoading = false;
                isAdsEnabled = true;
                if (!isTypeConfigured(TYPE_APP_OPEN)) {
                    return;
                }
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));
                appOpenAd = ad;
                AppUtils.trackScreen(context, "CL_END_APP_OPEN_LOAD");
                Log.d(TAG, "appopen loaded");
                onAppOpenListner(context);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                appOpenLoading = false;
                isAdsEnabled = true;
                AppUtils.trackScreen(context, "CL_END_APP_OPEN_FAILED");
                Log.d(TAG, "appopen failed: " + loadAdError);
            }
        });
    }

    private static void onAppOpenListner(Activity context) {
        if (appOpenAd == null) {
            return;
        }
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                adsShowCheckEvent(true);
                advanceAlternateSequence(context);
                preloadIfConfigured(context, TYPE_APP_OPEN);
                if (onCompleteAdCallBack != null) {
                    OnCompeteAds callback = onCompleteAdCallBack;
                    onCompleteAdCallBack = null;
                    callback.onCompeteAds(true);
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                adsShowCheckEvent(true);
                preloadIfConfigured(context, TYPE_APP_OPEN);
                if (onCompleteAdCallBack != null) {
                    OnCompeteAds callback = onCompleteAdCallBack;
                    onCompleteAdCallBack = null;
                    callback.onCompeteAds(false);
                }
            }
        });
    }

    public static void adsShowCheckEvent(Boolean check) {
        if (check) {
            isAdShowing = false;
            isAdsShowEnable = true;
        } else {
            isAdShowing = true;
            isAdsShowEnable = false;
        }
    }

    public static void loadAd(Activity context) {
        if (context == null) {
            return;
        }
        dropUnconfiguredAds();
        Set<String> types = configuredTypes();
        Log.d(TAG, "preload types=" + types);
        if (types.contains(TYPE_INTER)) {
            admobFullScreenAdLoad(context);
        }
        if (types.contains(TYPE_APP_OPEN)) {
            admobAppOpenAdLoad(context);
        }
        if (types.contains(TYPE_NATIVE)) {
            admobNativeAdLoad(context);
        }
    }

    private static void admobFullScreenAdLoad(Activity context) {
        if (!isTypeConfigured(TYPE_INTER) || context == null || fullScreenAds != null || interstitialLoading) {
            return;
        }

        String adUnitId = unitIdFor(TYPE_INTER);
        if (adUnitId.isEmpty()) {
            return;
        }

        interstitialLoading = true;
        AdRequest request = new AdRequest.Builder().build();
        InterstitialAd.load(context, adUnitId, request, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                interstitialLoading = false;
                isAdsEnabled = true;
                if (!isTypeConfigured(TYPE_INTER)) {
                    return;
                }
                interstitialAd.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));
                CallEndBackAd.fullScreenAds = interstitialAd;
                AppUtils.trackScreen(context, "CL_END_INTER_LOAD");
                Log.d(TAG, "inter loaded");
                onContactListner(context);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                interstitialLoading = false;
                isAdsEnabled = true;
                AppUtils.trackScreen(context, "CL_END_INTER_FAILED");
                Log.d(TAG, "inter failed: " + loadAdError);
            }
        });
    }

    private static void admobNativeAdLoad(Activity context) {
        if (!isTypeConfigured(TYPE_NATIVE) || context == null || nativeAd != null || nativeLoading) {
            return;
        }

        String adUnitId = unitIdFor(TYPE_NATIVE);
        if (adUnitId.isEmpty()) {
            return;
        }

        nativeLoading = true;
        AdLoader adLoader = new AdLoader.Builder(context, adUnitId).forNativeAd(loadedAd -> {
            nativeLoading = false;
            isAdsEnabled = true;
            if (!isTypeConfigured(TYPE_NATIVE)) {
                loadedAd.destroy();
                return;
            }
            if (nativeAd != null) {
                nativeAd.destroy();
            }
            nativeAd = loadedAd;
            nativeAd.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));
            AppUtils.trackScreen(context, "CL_END_NATIVE_LOAD");
            Log.d(TAG, "native loaded");
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                nativeLoading = false;
                isAdsEnabled = true;
                AppUtils.trackScreen(context, "CL_END_NATIVE_FAILED");
                Log.d(TAG, "native failed: " + adError);
            }
        }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void onContactListner(Context context) {
        if (fullScreenAds == null) {
            return;
        }
        fullScreenAds.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                adsShowCheckEvent(true);
                advanceAlternateSequence(context);
                if (context instanceof Activity) {
                    preloadIfConfigured((Activity) context, TYPE_INTER);
                }
                if (onCompleteAdCallBack != null) {
                    OnCompeteAds callback = onCompleteAdCallBack;
                    onCompleteAdCallBack = null;
                    callback.onCompeteAds(true);
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                adsShowCheckEvent(true);
                if (context instanceof Activity) {
                    preloadIfConfigured((Activity) context, TYPE_INTER);
                }
                if (onCompleteAdCallBack != null) {
                    OnCompeteAds callback = onCompleteAdCallBack;
                    onCompleteAdCallBack = null;
                    callback.onCompeteAds(false);
                }
            }

            @Override
            public void onAdImpression() {
            }

            @Override
            public void onAdShowedFullScreenContent() {
            }
        });
    }

    private static void preloadIfConfigured(Activity context, String type) {
        if (context == null || context.isFinishing() || context.isDestroyed() || !isTypeConfigured(type)) {
            return;
        }
        if (TYPE_INTER.equals(type)) {
            admobFullScreenAdLoad(context);
        } else if (TYPE_APP_OPEN.equals(type)) {
            admobAppOpenAdLoad(context);
        } else if (TYPE_NATIVE.equals(type)) {
            admobNativeAdLoad(context);
        }
    }

    private static void dropUnconfiguredAds() {
        if (!isTypeConfigured(TYPE_INTER)) {
            fullScreenAds = null;
        }
        if (!isTypeConfigured(TYPE_APP_OPEN)) {
            appOpenAd = null;
        }
        if (!isTypeConfigured(TYPE_NATIVE) && nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
    }

    @NonNull
    private static Set<String> configuredTypes() {
        Set<String> types = new LinkedHashSet<>();
        String mode = normalizeAdType(AdPlacement.getClEndBackAdType());
        if (TYPE_INTER.equals(mode) || TYPE_APP_OPEN.equals(mode) || TYPE_NATIVE.equals(mode)) {
            if (hasUnitId(mode)) {
                types.add(mode);
            }
            return types;
        }
        if (!TYPE_ALTERNATE.equals(mode)) {
            return types;
        }
        for (String[] step : AdPlacement.getClEndBackAdSequence()) {
            if (step == null || step.length < 2 || parsePositiveCount(step[1]) <= 0) {
                continue;
            }
            String type = normalizeAdType(step[0]);
            if ((TYPE_INTER.equals(type) || TYPE_APP_OPEN.equals(type) || TYPE_NATIVE.equals(type)) && hasUnitId(type)) {
                types.add(type);
            }
        }
        return types;
    }

    private static boolean isTypeConfigured(String type) {
        String normalized = normalizeAdType(type);
        return !normalized.isEmpty() && configuredTypes().contains(normalized);
    }

    @NonNull
    private static String unitIdFor(String type) {
        String normalized = normalizeAdType(type);
        if (TYPE_INTER.equals(normalized)) {
            // ClEnd_Back_Ad_Interstitial_Id
            String id = AdPlacement.getClEndBackAdInterstitialId();
            return id == null ? "" : id.trim();
        }
        if (TYPE_APP_OPEN.equals(normalized)) {
            String id = AdPlacement.getAppOpenId();
            return id == null ? "" : id.trim();
        }
        if (TYPE_NATIVE.equals(normalized)) {
            // ClEnd_Native_Id (banner stays ClEnd_Banner_Id on screen only)
            String id = AdPlacement.getClEndNativeId();
            if (id == null || id.trim().isEmpty()) {
                id = AdPlacement.getClEndBackAdNativeId();
            }
            return id == null ? "" : id.trim();
        }
        return "";
    }

    private static boolean hasUnitId(String type) {
        return !unitIdFor(type).isEmpty();
    }

    @NonNull
    private static String resolveShowType(Context context) {
        String mode = normalizeAdType(AdPlacement.getClEndBackAdType());
        if (TYPE_INTER.equals(mode) || TYPE_APP_OPEN.equals(mode) || TYPE_NATIVE.equals(mode)) {
            return hasUnitId(mode) ? mode : "";
        }
        if (!TYPE_ALTERNATE.equals(mode)) {
            return "";
        }
        List<String> types = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        long total = buildSequence(types, counts);
        if (total <= 0L || context == null) {
            return "";
        }
        long index = readSequenceCursor(context) % total;
        long walked = 0L;
        for (int i = 0; i < types.size(); i++) {
            walked += counts.get(i);
            if (index < walked) {
                return types.get(i);
            }
        }
        return "";
    }

    private static long buildSequence(List<String> types, List<Integer> counts) {
        long total = 0L;
        for (String[] step : AdPlacement.getClEndBackAdSequence()) {
            if (step == null || step.length < 2) {
                continue;
            }
            String type = normalizeAdType(step[0]);
            int count = parsePositiveCount(step[1]);
            if ((!TYPE_INTER.equals(type) && !TYPE_APP_OPEN.equals(type) && !TYPE_NATIVE.equals(type))
                    || count <= 0
                    || !hasUnitId(type)) {
                continue;
            }
            types.add(type);
            counts.add(count);
            total += count;
        }
        return total;
    }

    private static boolean isAdReady(@Nullable String type) {
        String normalized = normalizeAdType(type);
        if (TYPE_INTER.equals(normalized)) {
            return fullScreenAds != null;
        }
        if (TYPE_APP_OPEN.equals(normalized)) {
            return appOpenAd != null;
        }
        if (TYPE_NATIVE.equals(normalized)) {
            return nativeAd != null;
        }
        return false;
    }

    private static void advanceAlternateSequence(Context context) {
        if (context == null || !TYPE_ALTERNATE.equals(normalizeAdType(AdPlacement.getClEndBackAdType()))) {
            return;
        }
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(SEQUENCE_PREFS, Context.MODE_PRIVATE);
        long cursor = preferences.getLong(SEQUENCE_CURSOR, 0L);
        if (cursor < 0L) {
            cursor = 0L;
        }
        preferences.edit().putLong(SEQUENCE_CURSOR, cursor + 1L).apply();
        Log.d(TAG, "cursor -> " + (cursor + 1L));
    }

    private static long readSequenceCursor(Context context) {
        long cursor = context.getApplicationContext().getSharedPreferences(SEQUENCE_PREFS, Context.MODE_PRIVATE).getLong(SEQUENCE_CURSOR, 0L);
        return cursor < 0L ? 0L : cursor;
    }

    @NonNull
    private static String normalizeAdType(String value) {
        if (value == null) {
            return "";
        }
        String type = value.trim().toLowerCase(Locale.US).replace('-', '_');
        if (TYPE_ALTERNATE.equals(type)) {
            return TYPE_ALTERNATE;
        }
        if (TYPE_INTER.equals(type)
                || "interstitial".equals(type)
                || "google_inter".equals(type)
                || "google_interstitial".equals(type)) {
            return TYPE_INTER;
        }
        if (TYPE_APP_OPEN.equals(type)
                || "app_open".equals(type)
                || "google_appopen".equals(type)
                || "google_app_open".equals(type)) {
            return TYPE_APP_OPEN;
        }
        if (TYPE_NATIVE.equals(type)
                || "google_native".equals(type)
                || "quiz_native".equals(type)) {
            return TYPE_NATIVE;
        }
        return "";
    }

    private static int parsePositiveCount(String value) {
        if (value == null) {
            return 0;
        }
        try {
            int count = Integer.parseInt(value.trim());
            return count > 0 ? count : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    public interface OnCompeteAds {
        void onCompeteAds(boolean b);
    }
}
