package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;
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
 * Event bottom ads with load type {@code PreLoad} are fetched before the screen opens.
 * {@code Load} still requests the ad when the screen is shown. Each event screen
 * (charging, install/uninstall) keeps its own cache because ids and view types differ.
 */
public final class EventBottomAds {
    private static final class Cache {
        int token;
        @Nullable
        NativeAd nativeAd;
        @Nullable
        AdView bannerAd;
        boolean nativeLoading;
        boolean bannerLoading;
    }

    private static final Cache CHARGING = new Cache();
    private static final Cache INSTALL_UNINSTALL = new Cache();

    private EventBottomAds() {
    }

    @NonNull
    private static Cache cacheFor(@Nullable String screenKey) {
        return RemoteConfigValues.EVENT_SCREEN_CHARGING.equals(screenKey) ? CHARGING : INSTALL_UNINSTALL;
    }

    public static void prepare(@Nullable Context context) {
        prepare(context, RemoteConfigValues.EVENT_SCREEN_CHARGING);
        prepare(context, RemoteConfigValues.EVENT_SCREEN_INSTALL_UNINSTALL);
    }

    public static void prepare(@Nullable Context context, @NonNull String screenKey) {
        if (context == null) {
            return;
        }
        Context app = context.getApplicationContext();
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> prepare(app, screenKey));
            return;
        }
        RemoteConfigValues.ensureLoaded(app);
        AdPlacement.preloadEventBackAd(app, screenKey);
        Cache cache = cacheFor(screenKey);
        boolean wants = wantsPreload(screenKey);
        Log.d("EventPromptAd", "prepare " + screenKey
                + " wantsPreload=" + wants
                + " nativeAd=" + (cache.nativeAd != null)
                + " nativeLoading=" + cache.nativeLoading);
        if (!wants) {
            clear(screenKey);
            return;
        }
        if (AdPlacement.shouldUseQuizPriority()) {
            clear(screenKey);
            return;
        }
        if (useNative(screenKey)) {
            destroyBanner(cache);
            preloadNative(app, screenKey, cache);
        } else {
            destroyNative(cache);
            preloadBanner(app, screenKey, cache);
        }
    }

    public static boolean useNative(@Nullable String screenKey) {
        return !"banner".equalsIgnoreCase(RemoteConfigValues.getEventScreenConfig(screenKey).bottomAdType);
    }

    public static boolean isPreloadInFlight(@Nullable String screenKey) {
        Cache cache = cacheFor(screenKey);
        return cache.nativeAd == null && cache.nativeLoading;
    }

    @Nullable
    public static NativeAd takeNative(@Nullable String screenKey) {
        Cache cache = cacheFor(screenKey);
        NativeAd ready = cache.nativeAd;
        cache.nativeAd = null;
        return ready;
    }

    @Nullable
    public static AdView takeBanner(@Nullable String screenKey) {
        Cache cache = cacheFor(screenKey);
        AdView ready = cache.bannerAd;
        cache.bannerAd = null;
        return ready;
    }

    public static void clear(@Nullable String screenKey) {
        Cache cache = cacheFor(screenKey);
        cache.token++;
        cache.nativeLoading = false;
        cache.bannerLoading = false;
        destroyNative(cache);
        destroyBanner(cache);
    }

    private static boolean wantsPreload(@Nullable String screenKey) {
        RemoteConfigValues.EventScreenConfig config = RemoteConfigValues.getEventScreenConfig(screenKey);
        return config.bottomAdShow && "preload".equalsIgnoreCase(config.adLoadType == null ? "" : config.adLoadType.trim());
    }

    private static void preloadNative(Context app, String screenKey, Cache cache) {
        if (cache.nativeAd != null || cache.nativeLoading) {
            return;
        }
        String unitId = RemoteConfigValues.getEventScreenConfig(screenKey).nativeId.trim();
        AdPlacement.initializeIfConfigured(app);
        if (unitId.isEmpty() || !AdPlacement.canRequestAds(app) || !AdPlacement.isNetworkAvailable(app)) {
            Log.d("EventPromptAd", "preloadNative skip " + screenKey
                    + " unitId.empty=" + unitId.isEmpty()
                    + " canRequestAds=" + AdPlacement.canRequestAds(app)
                    + " network=" + AdPlacement.isNetworkAvailable(app));
            return;
        }
        int request = cache.token;
        cache.nativeLoading = true;
        Log.d("EventPrompt", "preload native start " + screenKey);
        new AdLoader.Builder(app, unitId).forNativeAd(loaded -> {
            if (request != cache.token || !wantsPreload(screenKey) || !useNative(screenKey)) {
                loaded.destroy();
                cache.nativeLoading = false;
                return;
            }
            cache.nativeLoading = false;
            if (cache.nativeAd != null) {
                cache.nativeAd.destroy();
            }
            loaded.setOnPaidEventListener(value -> AdPlacement.logAdRevenue(app, value));
            cache.nativeAd = loaded;
            Log.d("EventPrompt", "preload native ready " + screenKey);
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                if (request == cache.token) {
                    cache.nativeLoading = false;
                    Log.d("EventPrompt", "preload native failed " + screenKey);
                }
            }
        }).build().loadAd(new AdRequest.Builder().build());
    }

    private static void preloadBanner(Context app, String screenKey, Cache cache) {
        if (cache.bannerAd != null || cache.bannerLoading) {
            return;
        }
        String unitId = RemoteConfigValues.getEventScreenConfig(screenKey).bannerId.trim();
        AdPlacement.initializeIfConfigured(app);
        if (unitId.isEmpty() || !AdPlacement.canRequestAds(app) || !AdPlacement.isNetworkAvailable(app)) {
            return;
        }
        int request = cache.token;
        cache.bannerLoading = true;
        DisplayMetrics metrics = app.getResources().getDisplayMetrics();
        int widthPx = metrics.widthPixels - Math.round(32f * metrics.density);
        int widthDp = Math.max(1, (int) Math.floor(widthPx / metrics.density));
        AdView adView = new AdView(app);
        adView.setAdSize(AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(app, widthDp));
        adView.setAdUnitId(unitId);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if (request != cache.token || !wantsPreload(screenKey) || useNative(screenKey)) {
                    adView.destroy();
                    cache.bannerLoading = false;
                    return;
                }
                cache.bannerLoading = false;
                adView.setOnPaidEventListener(value -> AdPlacement.logAdRevenue(app, value));
                cache.bannerAd = adView;
                Log.d("EventPrompt", "preload banner ready " + screenKey);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                if (request == cache.token) {
                    cache.bannerLoading = false;
                    adView.destroy();
                    Log.d("EventPrompt", "preload banner failed " + screenKey);
                }
            }
        });
        Log.d("EventPrompt", "preload banner start " + screenKey);
        adView.loadAd(new AdRequest.Builder().build());
    }

    private static void destroyNative(Cache cache) {
        if (cache.nativeAd != null) {
            cache.nativeAd.destroy();
            cache.nativeAd = null;
        }
    }

    private static void destroyBanner(Cache cache) {
        if (cache.bannerAd != null) {
            cache.bannerAd.destroy();
            cache.bannerAd = null;
        }
    }
}
