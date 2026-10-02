package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdView;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

/**
 * Banner or medium native for one app screen. A new request starts every time
 * that screen opens. Quiz follows Ad_Priority and Google_Ad_Failed_Show_Quiz.
 */
public final class ScreenLoadAd {
    private ScreenLoadAd() {
    }

    public static boolean isEnabled(String screenKey) {
        RemoteConfigValues.ScreenAdConfig config = RemoteConfigValues.getScreenAd(screenKey);
        if (config == null || !config.show) {
            return false;
        }
        if (AdPlacement.shouldUseQuizPriority()) {
            return true;
        }
        boolean banner = isBanner(config);
        String id = banner ? config.bannerId : config.nativeId;
        return id != null && !id.trim().isEmpty();
    }

    public static void attach(Activity activity, FrameLayout host, String screenKey) {
        if (host == null) {
            return;
        }
        host.removeAllViews();
        RemoteConfigValues.ScreenAdConfig config = RemoteConfigValues.getScreenAd(screenKey);
        if (activity == null || config == null || !config.show) {
            host.setVisibility(View.GONE);
            return;
        }
        boolean banner = isBanner(config);
        if (!AdPlacement.shouldUseQuizPriority()) {
            String id = banner ? config.bannerId : config.nativeId;
            if (id == null || id.trim().isEmpty()) {
                host.setVisibility(View.GONE);
                return;
            }
        }
        View root = LayoutInflater.from(AdTheme.forApp(activity)).inflate(R.layout.view_screen_load_ad, host, false);
        host.addView(root, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        host.setVisibility(View.VISIBLE);
        RelativeLayout bannerContainer = root.findViewById(R.id.rlBannerAdView);
        ShimmerFrameLayout bannerShimmer = root.findViewById(R.id.slBannerShimmer);
        LinearLayout bannerContent = root.findViewById(R.id.llBannerAd);
        RelativeLayout nativeContainer = root.findViewById(R.id.rlNativeAdView);
        ShimmerFrameLayout nativeShimmer = root.findViewById(R.id.slNativeShimmer);
        FrameLayout nativeContent = root.findViewById(R.id.flNativeAd);
        if (banner) {
            nativeContainer.setVisibility(View.GONE);
            bannerContainer.setVisibility(View.VISIBLE);
            AdPlacement.loadBannerAd(activity, config.bannerId, bannerContainer, bannerShimmer, bannerContent);
            return;
        }
        bannerContainer.setVisibility(View.GONE);
        nativeContainer.setVisibility(View.VISIBLE);
        AdPlacement.loadNativeAd(activity, config.nativeId, nativeContainer, nativeShimmer, nativeContent, "medium");
    }

    public static void detach(FrameLayout host) {
        if (host == null) {
            return;
        }
        destroyBannerViews(host);
        host.removeAllViews();
    }

    private static void destroyBannerViews(FrameLayout host) {
        LinearLayout bannerContent = host.findViewById(R.id.llBannerAd);
        if (bannerContent == null) {
            return;
        }
        for (int i = 0; i < bannerContent.getChildCount(); i++) {
            View child = bannerContent.getChildAt(i);
            if (child instanceof AdView) {
                ((AdView) child).destroy();
            }
        }
    }

    private static boolean isBanner(RemoteConfigValues.ScreenAdConfig config) {
        return "banner".equalsIgnoreCase(config.type == null ? "" : config.type.trim());
    }
}
