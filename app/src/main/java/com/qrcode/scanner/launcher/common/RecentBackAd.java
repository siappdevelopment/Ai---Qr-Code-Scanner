package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;

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

/**
 * Full-screen back ad of the Recent screen (close button / Back). One cached ad per Google type; only the type the
 * sequence points to is preloaded. When nothing is cached at close, the ad is requested behind the loading dialog (5 s max).
 * <p>
 * Quiz rules: a Quiz ad shows only for an explicit {@code quiz_*} step of the sequence. A Google ad that fails to load /
 * show, and Ad_Priority = QUIZ, never turn into a Quiz ad here.
 * <p>
 * Visit tokens: one "visit" is one opening of the Recent screen. An answer that arrives after its visit ended is thrown
 * away, so an ad can never pop up on its own at the next opening.
 */
public final class RecentBackAd {
    private static final long LOAD_TIMEOUT_MS = 5_000L;
    private static final long LOAD_POLL_MS = 150L;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private static InterstitialAd interstitial;
    private static AppOpenAd appOpen;
    private static NativeAd nativeAd;
    private static int visit;
    private static int interLoadingVisit = -1;
    private static int appOpenLoadingVisit = -1;
    private static int nativeLoadingVisit = -1;
    private static int failCount;

    public interface OnCompeteAds {
        void onCompeteAds(boolean shown);
    }

    private RecentBackAd() {
    }

    /** The Recent screen opened: nothing from an older visit is kept. */
    public static void beginVisit() {
        visit++;
        clearCache();
    }

    /** The Recent screen is gone: unused ads are dropped and requests still in flight are ignored when they answer. */
    public static void endVisit() {
        visit++;
        clearCache();
    }

    private static void clearCache() {
        interstitial = null;
        appOpen = null;
        dropNative();
    }

    private static void dropNative() {
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
    }

    /** Google ads of this screen: online, ads allowed, and not in Quiz priority (then nothing shows, no Quiz either). */
    private static boolean googleAllowed(Context context) {
        return AdPlacement.isNetworkAvailable(context) && AdPlacement.canRequestAds(context) && !AdPlacement.shouldUseQuizPriority();
    }

    /** Recent_Back_Ad_Load_Type = PreLoad: loads only the type the sequence points to (a Quiz step preloads nothing). */
    public static void loadAd(Activity activity) {
        Context context = activity.getApplicationContext();
        if (!AdPlacement.isRecentBackAdPreload()) {
            clearCache();
            return;
        }
        String step = AdPlacement.getRecentBackAdResolvedType(context);
        if (!AdPlacement.BACK_AD_INTER.equals(step)) {
            interstitial = null;
        }
        if (!AdPlacement.BACK_AD_APP_OPEN.equals(step)) {
            appOpen = null;
        }
        if (!AdPlacement.BACK_AD_NATIVE.equals(step)) {
            dropNative();
        }
        if (AdPlacement.isRecentQuizStep(step) || !googleAllowed(context)) {
            return;
        }
        loadStep(context, step);
    }

    private static void loadStep(Context context, String step) {
        if (AdPlacement.BACK_AD_INTER.equals(step)) {
            loadInterstitial(context);
        } else if (AdPlacement.BACK_AD_APP_OPEN.equals(step)) {
            loadAppOpen(context);
        } else if (AdPlacement.BACK_AD_NATIVE.equals(step)) {
            loadNative(context);
        }
    }

    private static boolean isCached(String step) {
        if (AdPlacement.BACK_AD_INTER.equals(step)) {
            return interstitial != null;
        }
        if (AdPlacement.BACK_AD_APP_OPEN.equals(step)) {
            return appOpen != null;
        }
        return nativeAd != null;
    }

    /** Shows the ad of the current step; {@code onFinishAd} (main thread, once) says whether an ad was really shown. */
    public static void fullScreenAdShow(Activity activity, OnCompeteAds onFinishAd) {
        final boolean[] finished = {false};
        final OnCompeteAds onDone = shown -> {
            if (!finished[0]) {
                finished[0] = true;
                if (onFinishAd != null) {
                    onFinishAd.onCompeteAds(shown);
                }
            }
        };
        Context context = activity.getApplicationContext();
        String step = AdPlacement.getRecentBackAdResolvedType(context);
        if (AdPlacement.isRecentQuizStep(step)) {
            // Explicit Quiz step: the only way a Quiz ad appears on this screen.
            AdPlacement.showRecentQuizStep(activity, step, shown -> onDone.onCompeteAds(shown));
            return;
        }
        if (!googleAllowed(context)) {
            onDone.onCompeteAds(false);
            return;
        }
        if (!isCached(step)) {
            // Load mode, or PreLoad whose ad did not arrive in time: request it now behind the loading dialog.
            loadThenShow(activity, step, onDone);
            return;
        }
        showStep(activity, step, onDone);
    }

    private static void showStep(Activity activity, String step, OnCompeteAds onDone) {
        if (AdPlacement.BACK_AD_INTER.equals(step)) {
            showInterstitial(activity, onDone);
        } else if (AdPlacement.BACK_AD_APP_OPEN.equals(step)) {
            showAppOpen(activity, onDone);
        } else {
            showNative(activity, onDone);
        }
    }

    private static void onLoadFailed(Context context, String step) {
        failCount++;
        AdPlacement.skipRecentBackAdStep(context, step);
    }

    private static void loadThenShow(Activity activity, String step, OnCompeteAds onDone) {
        final Context context = activity.getApplicationContext();
        final Dialog dialog = AdPlacement.showInterstitialLoadingDialog(activity);
        loadStep(context, step);
        final long start = SystemClock.elapsedRealtime();
        final int failsAtStart = failCount;
        HANDLER.post(new Runnable() {
            @Override
            public void run() {
                boolean ready = isCached(step);
                boolean timedOut = SystemClock.elapsedRealtime() - start >= LOAD_TIMEOUT_MS;
                if (ready || failCount != failsAtStart || timedOut || activity.isFinishing() || activity.isDestroyed()) {
                    AdPlacement.dismissInterstitialLoadingDialog(dialog);
                    if (ready && !activity.isFinishing() && !activity.isDestroyed()) {
                        showStep(activity, step, onDone);
                    } else {
                        // Not ready in time: a failed try (the gap starts, the sequence moves on). A late answer is
                        // thrown away with the visit, so it never shows at the next opening.
                        if (!ready && failCount == failsAtStart) {
                            AdPlacement.skipRecentBackAdStep(context, step);
                        }
                        clearCache();
                        visit++;
                        onDone.onCompeteAds(false);
                    }
                    return;
                }
                HANDLER.postDelayed(this, LOAD_POLL_MS);
            }
        });
    }

    // ------------------------------------------------------------------ interstitial

    private static void loadInterstitial(Context context) {
        String id = AdPlacement.getRecentBackAdInterstitialId();
        final int v = visit;
        if (interstitial != null || interLoadingVisit == v || id == null || id.trim().isEmpty()) {
            return;
        }
        interLoadingVisit = v;
        InterstitialAd.load(context, id.trim(), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                if (interLoadingVisit == v) {
                    interLoadingVisit = -1;
                }
                if (v != visit) {
                    return;
                }
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));
                interstitial = ad;
                AppUtils.trackScreen(context, "recent_inter_load");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                if (interLoadingVisit == v) {
                    interLoadingVisit = -1;
                }
                AppUtils.trackScreen(context, "recent_inter_fail");
                if (v == visit) {
                    onLoadFailed(context, AdPlacement.BACK_AD_INTER);
                }
            }
        });
    }

    private static void showInterstitial(Activity activity, OnCompeteAds onDone) {
        InterstitialAd ad = interstitial;
        interstitial = null;
        if (ad == null) {
            onDone.onCompeteAds(false);
            return;
        }
        final Context app = activity.getApplicationContext();
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdImpression() {
                AppUtils.trackScreen(app, "recent_inter_impression");
            }

            @Override
            public void onAdClicked() {
                AppUtils.trackScreen(app, "recent_inter_click");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                onDone.onCompeteAds(true);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                AdPlacement.skipRecentBackAdStep(app, AdPlacement.BACK_AD_INTER);
                onDone.onCompeteAds(false);
            }
        });
        try {
            ad.show(activity);
        } catch (Exception e) {
            onDone.onCompeteAds(false);
        }
    }

    // ------------------------------------------------------------------ app open

    private static void loadAppOpen(Context context) {
        String id = AdPlacement.getAppOpenId();
        final int v = visit;
        if (appOpen != null || appOpenLoadingVisit == v || id == null || id.trim().isEmpty()) {
            return;
        }
        appOpenLoadingVisit = v;
        AppOpenAd.load(context, id.trim(), new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                if (appOpenLoadingVisit == v) {
                    appOpenLoadingVisit = -1;
                }
                if (v != visit) {
                    return;
                }
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));
                appOpen = ad;
                AppUtils.trackScreen(context, "recent_app_open_load");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                if (appOpenLoadingVisit == v) {
                    appOpenLoadingVisit = -1;
                }
                AppUtils.trackScreen(context, "recent_app_open_fail");
                if (v == visit) {
                    onLoadFailed(context, AdPlacement.BACK_AD_APP_OPEN);
                }
            }
        });
    }

    private static void showAppOpen(Activity activity, OnCompeteAds onDone) {
        AppOpenAd ad = appOpen;
        appOpen = null;
        if (ad == null) {
            onDone.onCompeteAds(false);
            return;
        }
        final Context app = activity.getApplicationContext();
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdImpression() {
                AppUtils.trackScreen(app, "recent_app_open_impression");
            }

            @Override
            public void onAdClicked() {
                AppUtils.trackScreen(app, "recent_app_open_click");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                onDone.onCompeteAds(true);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                AdPlacement.skipRecentBackAdStep(app, AdPlacement.BACK_AD_APP_OPEN);
                onDone.onCompeteAds(false);
            }
        });
        try {
            ad.show(activity);
        } catch (Exception e) {
            onDone.onCompeteAds(false);
        }
    }

    // ------------------------------------------------------------------ native

    private static void loadNative(Context context) {
        String id = AdPlacement.getRecentNativeId();
        final int v = visit;
        if (nativeAd != null || nativeLoadingVisit == v || id == null || id.trim().isEmpty()) {
            return;
        }
        nativeLoadingVisit = v;
        new AdLoader.Builder(context, id.trim()).forNativeAd(ad -> {
            if (nativeLoadingVisit == v) {
                nativeLoadingVisit = -1;
            }
            if (v != visit) {
                ad.destroy();
                return;
            }
            ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));
            dropNative();
            nativeAd = ad;
            AppUtils.trackScreen(context, "recent_native_load");
        }).withAdListener(new AdListener() {
            @Override
            public void onAdImpression() {
                AppUtils.trackScreen(context, "recent_native_impression");
            }

            @Override
            public void onAdClicked() {
                AppUtils.trackScreen(context, "recent_native_click");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                if (nativeLoadingVisit == v) {
                    nativeLoadingVisit = -1;
                }
                AppUtils.trackScreen(context, "recent_native_fail");
                if (v == visit) {
                    onLoadFailed(context, AdPlacement.BACK_AD_NATIVE);
                }
            }
        }).build().loadAd(new AdRequest.Builder().build());
    }

    private static void showNative(Activity activity, OnCompeteAds onDone) {
        NativeAd ad = nativeAd;
        nativeAd = null;
        if (ad == null) {
            onDone.onCompeteAds(false);
            return;
        }
        AdPlacement.showBackNativeAd(activity, ad, AdPlacement.getRecentNativeId(), shown -> onDone.onCompeteAds(Boolean.TRUE.equals(shown)));
    }
}
