package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

/**
 * Event bottom ads with type {@code preload} are fetched before the screen opens.
 * {@code load} still requests the ad when the screen is shown.
 */
public final class EventBottomAds {
    private static int token;
    @Nullable
    private static NativeAd nativeAd;
    @Nullable
    private static AdView bannerAd;
    private static boolean nativeLoading;
    private static boolean bannerLoading;

    private EventBottomAds() {
    }

    public static void prepare(@Nullable Context context) {
        if (context == null) {
            return;
        }
        Context app = context.getApplicationContext();
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> prepare(app));
            return;
        }
        RemoteConfigValues.ensureLoaded(app);
        if (!wantsPreload()) {
            clear();
            return;
        }
        if (AdPlacement.shouldUseQuizPriority()) {
            clear();
            return;
        }
        if (useNative()) {
            destroyBanner();
            preloadNative(app);
        } else {
            destroyNative();
            preloadBanner(app);
        }
    }

    public static boolean useNative() {
        return !"banner".equalsIgnoreCase(RemoteConfigValues.getEventBottomAdsView());
    }

    @Nullable
    public static NativeAd takeNative() {
        NativeAd ready = nativeAd;
        nativeAd = null;
        return ready;
    }

    @Nullable
    public static AdView takeBanner() {
        AdView ready = bannerAd;
        bannerAd = null;
        return ready;
    }

    public static void clear() {
        token++;
        nativeLoading = false;
        bannerLoading = false;
        destroyNative();
        destroyBanner();
    }

    private static boolean wantsPreload() {
        return RemoteConfigValues.getEventBottomAdsShow()
                && "preload".equalsIgnoreCase(RemoteConfigValues.getEventBottomAdsType().trim());
    }

    private static void preloadNative(Context app) {
        if (nativeAd != null || nativeLoading) {
            return;
        }
        String unitId = RemoteConfigValues.getEventNativeId().trim();
        AdPlacement.initializeIfConfigured(app);
        if (unitId.isEmpty() || !AdPlacement.canRequestAds(app) || !AdPlacement.isNetworkAvailable(app)) {
            return;
        }
        int request = token;
        nativeLoading = true;
        Log.d("EventPrompt", "preload native start");
        new AdLoader.Builder(app, unitId).forNativeAd(loaded -> {
            if (request != token || !wantsPreload() || !useNative()) {
                loaded.destroy();
                nativeLoading = false;
                return;
            }
            nativeLoading = false;
            if (nativeAd != null) {
                nativeAd.destroy();
            }
            loaded.setOnPaidEventListener(value -> AdPlacement.logAdRevenue(app, value));
            nativeAd = loaded;
            Log.d("EventPrompt", "preload native ready");
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                if (request == token) {
                    nativeLoading = false;
                    Log.d("EventPrompt", "preload native failed");
                }
            }
        }).build().loadAd(new AdRequest.Builder().build());
    }

    private static void preloadBanner(Context app) {
        if (bannerAd != null || bannerLoading) {
            return;
        }
        String unitId = RemoteConfigValues.getEventBannerId().trim();
        AdPlacement.initializeIfConfigured(app);
        if (unitId.isEmpty() || !AdPlacement.canRequestAds(app) || !AdPlacement.isNetworkAvailable(app)) {
            return;
        }
        int request = token;
        bannerLoading = true;
        DisplayMetrics metrics = app.getResources().getDisplayMetrics();
        int widthPx = metrics.widthPixels - Math.round(32f * metrics.density);
        int widthDp = Math.max(1, (int) Math.floor(widthPx / metrics.density));
        AdView adView = new AdView(app);
        adView.setAdSize(AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(app, widthDp));
        adView.setAdUnitId(unitId);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if (request != token || !wantsPreload() || useNative()) {
                    adView.destroy();
                    bannerLoading = false;
                    return;
                }
                bannerLoading = false;
                adView.setOnPaidEventListener(value -> AdPlacement.logAdRevenue(app, value));
                bannerAd = adView;
                Log.d("EventPrompt", "preload banner ready");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                if (request == token) {
                    bannerLoading = false;
                    adView.destroy();
                    Log.d("EventPrompt", "preload banner failed");
                }
            }
        });
        Log.d("EventPrompt", "preload banner start");
        adView.loadAd(new AdRequest.Builder().build());
    }

    private static void destroyNative() {
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
    }

    private static void destroyBanner() {
        if (bannerAd != null) {
            bannerAd.destroy();
            bannerAd = null;
        }
    }
}
