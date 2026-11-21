package com.qrcode.scanner.launcher.common;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.nativead.NativeAd;

/**
 * Language screen ad in "refresh mode": the first native ad shows with a GRAY call-to-action button; when the user picks
 * a different language it is replaced ONCE by a second native ad whose button is BLUE.
 * <ul>
 *   <li>Two native ads are requested up front: {@code current} (shown first) and {@code next} (the spare).</li>
 *   <li>When a different language is picked and {@code next} is loaded, it replaces {@code current} (once). If it is not
 *       ready, the first ad stays. Nothing more is ever loaded after that and a failed request is never repeated.</li>
 *   <li>If {@code current} fails but {@code next} is ready, {@code next} is shown instead (automatically).</li>
 *   <li>{@link #release()} destroys every ad when the screen closes.</li>
 * </ul>
 */
public final class LanguageAdsManager {
    public static final String CTA_GRAY = "#FF9E9E9E";
    public static final String CTA_BLUE = "#FF007AF5";

    /** The screen that shows the ads. All callbacks run on the main thread. */
    public interface Host {
        /** Show {@code ad} in the Language ad area; {@code second} = the replacement ad (blue button). */
        void showAd(@NonNull NativeAd ad, boolean second);

        /** No ad could be loaded at all: hide the ad area. */
        void noAd();
    }

    private static int session;
    @Nullable
    private static Host host;
    @Nullable
    private static NativeAd displayed;
    @Nullable
    private static NativeAd spare;
    private static boolean currentFailed;
    private static boolean spareFailed;
    private static boolean refreshed;

    private LanguageAdsManager() {
    }

    /** True when the Language screen should use refresh mode (onboarding only, native type, Google ads). */
    public static boolean isRefreshEnabled(@NonNull Activity activity) {
        return AdPlacement.getLanguageAdShow()
                && AdPlacement.getLanguageSelectRefreshAds()
                && !"banner".equalsIgnoreCase(AdPlacement.getLanguageAdType())
                && !AdPlacement.shouldUseQuizPriority()
                && AdPlacement.isNetworkAvailable(activity)
                && AdPlacement.canRequestAds(activity);
    }

    /** Requests both ads. Call once when the Language screen opens. */
    public static void start(@NonNull Activity activity, @NonNull Host screen) {
        release();
        host = screen;
        final int mySession = session;
        final String unitId = AdPlacement.getLanguageNativeId();
        AdPlacement.requestLargeNativeAd(activity, unitId, ad -> onLoaded(mySession, true, ad), () -> onFailed(mySession, true));
        AdPlacement.requestLargeNativeAd(activity, unitId, ad -> onLoaded(mySession, false, ad), () -> onFailed(mySession, false));
    }

    private static void onLoaded(int mySession, boolean isCurrent, @Nullable NativeAd ad) {
        if (ad == null) {
            return;
        }
        if (mySession != session || host == null) {
            ad.destroy();
            return;
        }
        if (isCurrent) {
            if (displayed != null) {
                // The spare was already shown (the first ad arrived late): this one is not needed.
                ad.destroy();
                return;
            }
            displayed = ad;
            host.showAd(ad, false);
        } else {
            if (spare != null) {
                ad.destroy();
                return;
            }
            spare = ad;
            if (currentFailed && displayed == null) {
                // The first ad failed: the spare takes its place at once (it is then the only ad, so no refresh is left).
                displayed = spare;
                spare = null;
                refreshed = true;
                host.showAd(displayed, false);
            }
        }
    }

    private static void onFailed(int mySession, boolean isCurrent) {
        if (mySession != session || host == null) {
            return;
        }
        if (isCurrent) {
            currentFailed = true;
            if (spare != null && displayed == null) {
                displayed = spare;
                spare = null;
                refreshed = true;
                host.showAd(displayed, false);
                return;
            }
        } else {
            spareFailed = true;
        }
        if (currentFailed && spareFailed && displayed == null) {
            host.noAd();
        }
    }

    /**
     * A different language was picked: replaces the first ad once with the spare (blue button) when it is loaded.
     * If the spare is not ready the first ad simply stays.
     */
    public static void onLanguagePicked() {
        if (refreshed || host == null || displayed == null || spare == null) {
            return;
        }
        refreshed = true;
        NativeAd old = displayed;
        displayed = spare;
        spare = null;
        host.showAd(displayed, true);
        // The old ad's view was replaced by showAd; its resources can go now.
        old.destroy();
    }

    /** Destroys every ad and ends the session (late callbacks are discarded). */
    public static void release() {
        session++;
        host = null;
        if (displayed != null) {
            displayed.destroy();
            displayed = null;
        }
        if (spare != null) {
            spare.destroy();
            spare = null;
        }
        currentFailed = false;
        spareFailed = false;
        refreshed = false;
    }
}
