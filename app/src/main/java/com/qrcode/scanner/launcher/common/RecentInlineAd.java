package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;

/**
 * Cache of the inline ad of the Recent screen for {@code Recent_Ad_Load_Type = PreLoad}. One ad (native or adaptive
 * banner, as {@code Recent_Ad_Type} says) is requested ahead; the screen takes it when it opens and shows it at once, then
 * asks for the next one. With {@code Load} nothing here is used: the screen requests its own ad when it opens.
 */
public final class RecentInlineAd {
    private static final long FAIL_COOLDOWN_MS = 5 * 60_000L;

    private static NativeAd nativeAd;
    private static AdView banner;
    private static AdLoader nativeLoader;
    private static boolean bannerLoading;
    private static long lastFailAt = -FAIL_COOLDOWN_MS;

    private RecentInlineAd() {
    }

    /** Requests the next inline ad when none is cached or on its way. Google only, same rules as the screen's own request. */
    public static void preload(@NonNull Context context) {
        Context app = context.getApplicationContext();
        if (!AdPlacement.isRecentAdPreload() || !AdPlacement.getRecentAdShow() || !allowed(app)) {
            return;
        }
        // A failed request is not repeated at once: the next one waits for the cool-down.
        if (SystemClock.elapsedRealtime() - lastFailAt < FAIL_COOLDOWN_MS) {
            return;
        }
        if ("banner".equalsIgnoreCase(AdPlacement.getRecentAdType())) {
            loadBanner(app);
        } else {
            loadNative(app);
        }
    }

    /** The cached native ad (removed from the cache), or null. The caller owns it and destroys it. */
    @Nullable
    public static NativeAd takeNative() {
        NativeAd ad = nativeAd;
        nativeAd = null;
        return ad;
    }

    /** The cached banner (removed from the cache), or null. The caller owns it and destroys it. */
    @Nullable
    public static AdView takeBanner() {
        AdView view = banner;
        banner = null;
        return view;
    }

    private static boolean allowed(Context context) {
        return AdPlacement.isNetworkAvailable(context) && AdPlacement.canRequestAds(context) && !AdPlacement.shouldUseQuizPriority();
    }

    private static void loadNative(Context context) {
        String id = AdPlacement.getRecentNativeId();
        if (nativeAd != null || (nativeLoader != null && nativeLoader.isLoading()) || id == null || id.trim().isEmpty()) {
            return;
        }
        nativeLoader = new AdLoader.Builder(context, id.trim()).forNativeAd(ad -> {
            ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));
            if (nativeAd != null) {
                nativeAd.destroy();
            }
            nativeAd = ad;
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                lastFailAt = SystemClock.elapsedRealtime();
            }
        }).build();
        nativeLoader.loadAd(new AdRequest.Builder().build());
    }

    private static void loadBanner(Context context) {
        String id = AdPlacement.getRecentBannerId();
        if (banner != null || bannerLoading || id == null || id.trim().isEmpty()) {
            return;
        }
        bannerLoading = true;
        float density = context.getResources().getDisplayMetrics().density;
        int widthDp = (int) (context.getResources().getDisplayMetrics().widthPixels / density);
        final AdView view = new AdView(context);
        view.setAdUnitId(id.trim());
        view.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp));
        view.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                bannerLoading = false;
                if (banner != null) {
                    banner.destroy();
                }
                banner = view;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                bannerLoading = false;
                lastFailAt = SystemClock.elapsedRealtime();
                view.destroy();
            }
        });
        view.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));
        view.loadAd(new AdRequest.Builder().build());
    }
}
