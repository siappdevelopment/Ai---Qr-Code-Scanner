package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared interstitial for the new screens. Back is wired now. Click uses the
 * same rules and is ready for the places that will be added later.
 */
public final class ScreenInterAds {
    private static final long LOADING_TIMEOUT_MS = 10_000L;
    private static final long QUIZ_SHIMMER_MS = 700L;

    private static final int KIND_CLICK = 0;
    private static final int KIND_BACK = 1;
    private static final int KIND_BOTTOM = 2;

    private static int backCount;
    private static int clickCount;
    private static int bottomCount;
    private static int backPreloadToken;
    private static int clickPreloadToken;
    private static int bottomPreloadToken;
    @Nullable
    private static InterstitialAd preloadedBackAd;
    @Nullable
    private static InterstitialAd preloadedClickAd;
    @Nullable
    private static InterstitialAd preloadedBottomAd;
    private static boolean backPreloadInFlight;
    private static boolean clickPreloadInFlight;
    private static boolean bottomPreloadInFlight;

    private ScreenInterAds() {
    }

    public static void onConfigApplied(@Nullable Context context) {
        backCount = 0;
        clickCount = 0;
        bottomCount = 0;
        preparePreload(context, KIND_BACK);
        preparePreload(context, KIND_CLICK);
        preparePreload(context, KIND_BOTTOM);
    }

    public static void onBack(@Nullable Activity activity, @Nullable String screenKey, @Nullable Runnable leave) {
        handle(activity, screenKey, true, leave);
    }

    public static void onClick(@Nullable Activity activity, @Nullable String screenKey, @Nullable Runnable after) {
        handle(activity, screenKey, false, after);
    }

    private static void handle(@Nullable Activity activity, @Nullable String screenKey, boolean back, @Nullable Runnable next) {
        if (!countsToward(screenKey, back)) {
            run(next);
            return;
        }
        int every = back ? RemoteConfigValues.getInterAdsBackClick() : RemoteConfigValues.getInterAdsClick();
        if (every <= 0) {
            run(next);
            return;
        }
        int count = back ? ++backCount : ++clickCount;
        if (count < every) {
            run(next);
            return;
        }
        if (back) {
            backCount = 0;
        } else {
            clickCount = 0;
        }
        String type = back ? RemoteConfigValues.getInterAdsBackClickType() : RemoteConfigValues.getInterAdsClickType();
        present(activity, back ? KIND_BACK : KIND_CLICK, isPreload(type), next);
    }

    public static void onBottomNav(@Nullable Activity activity, @Nullable Runnable next) {
        if (!RemoteConfigValues.getBottomNavInterAdsShow()) {
            run(next);
            return;
        }
        int every = RemoteConfigValues.getBottomNavInterClick();
        if (every <= 0) {
            run(next);
            return;
        }
        if (++bottomCount < every) {
            run(next);
            return;
        }
        bottomCount = 0;
        present(activity, KIND_BOTTOM, isPreload(RemoteConfigValues.getBottomNavInterAdsType()), next);
    }

    private static void preparePreload(@Nullable Context context, int kind) {
        if (shouldPreload(kind)) {
            preload(context, kind);
        } else {
            clearPreloaded(kind);
        }
    }

    private static boolean countsToward(@Nullable String screenKey, boolean back) {
        boolean master = back ? RemoteConfigValues.getInterAdsOnBack() : RemoteConfigValues.getInterAdsShow();
        if (!master) {
            return false;
        }
        RemoteConfigValues.ScreenAdConfig config = RemoteConfigValues.getScreenAd(screenKey);
        return config != null && (back ? config.onBackInterShow : config.onClickInterShow);
    }

    private static void present(@Nullable Activity activity, int kind, boolean preload, @Nullable Runnable next) {
        if (activity == null || activity.isFinishing()) {
            run(next);
            return;
        }
        if (AdPlacement.shouldUseQuizPriority()) {
            showQuiz(activity, !preload, next);
            return;
        }
        InterstitialAd ready = cachedAd(kind);
        if (preload && ready != null) {
            setCachedAd(kind, null);
            showGoogle(activity, ready, true, kind, next);
            return;
        }
        loadGoogle(activity, kind, true, preload, next);
    }

    private static void showQuiz(@Nullable Activity activity, boolean shimmerFirst, @Nullable Runnable next) {
        if (activity == null || activity.isFinishing()) {
            run(next);
            return;
        }
        if (!shimmerFirst) {
            if (!QuizAds.showInterstitial(activity, next)) {
                run(next);
            }
            return;
        }
        Dialog loading = showDialog(activity, R.layout.dialog_loading_ads);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            dismiss(loading);
            if (activity.isFinishing()) {
                run(next);
                return;
            }
            if (!QuizAds.showInterstitial(activity, next)) {
                run(next);
            }
        }, QUIZ_SHIMMER_MS);
    }

    private static void loadGoogle(@Nullable Activity activity, int kind, boolean showLoadingDialog, boolean refillAfterShow, @Nullable Runnable next) {
        if (activity == null || activity.isFinishing()) {
            run(next);
            return;
        }
        AdPlacement.initializeIfConfigured(activity);
        String unitId = unitId(kind);
        if (!AdPlacement.canRequestAds(activity) || !AdPlacement.isNetworkAvailable(activity) || unitId.isEmpty()) {
            if (AdPlacement.getGoogleAdFailedShowQuiz()) {
                showQuiz(activity, false, next);
            } else {
                run(next);
            }
            return;
        }
        AtomicBoolean done = new AtomicBoolean(false);
        Dialog loading = showLoadingDialog ? showDialog(activity, R.layout.dialog_loading_ads) : null;
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeout = () -> {
            dismiss(loading);
            runOnce(done, next);
        };
        if (loading != null) {
            handler.postDelayed(timeout, LOADING_TIMEOUT_MS);
        }
        InterstitialAd.load(activity, unitId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                handler.removeCallbacks(timeout);
                dismiss(loading);
                if (activity.isFinishing() || done.get()) {
                    return;
                }
                showGoogle(activity, ad, refillAfterShow, kind, () -> runOnce(done, next));
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                handler.removeCallbacks(timeout);
                dismiss(loading);
                if (done.get()) {
                    return;
                }
                if (AdPlacement.getGoogleAdFailedShowQuiz() && QuizAds.showInterstitial(activity, () -> runOnce(done, next))) {
                    return;
                }
                runOnce(done, next);
            }
        });
    }

    private static void showGoogle(Activity activity, InterstitialAd ad, boolean refillAfterShow, int kind, @Nullable Runnable next) {
        AtomicBoolean done = new AtomicBoolean(false);
        ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(activity, adValue));
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (refillAfterShow) {
                    preload(activity, kind);
                }
                runOnce(done, next);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                if (AdPlacement.getGoogleAdFailedShowQuiz() && QuizAds.showInterstitial(activity, () -> runOnce(done, next))) {
                    if (refillAfterShow) {
                        preload(activity, kind);
                    }
                    return;
                }
                if (refillAfterShow) {
                    preload(activity, kind);
                }
                runOnce(done, next);
            }
        });
        ad.show(activity);
    }

    @NonNull
    private static String unitId(int kind) {
        String id;
        if (kind == KIND_BACK) {
            id = RemoteConfigValues.getInterBackAdsId();
            if (id == null || id.trim().isEmpty()) {
                id = RemoteConfigValues.getInterAdsId();
            }
        } else if (kind == KIND_BOTTOM) {
            id = RemoteConfigValues.getBottomNavInterAdsId();
        } else {
            id = RemoteConfigValues.getInterAdsId();
        }
        return id == null ? "" : id.trim();
    }

    private static boolean shouldPreload(int kind) {
        if (AdPlacement.shouldUseQuizPriority() || unitId(kind).isEmpty()) {
            return false;
        }
        if (kind == KIND_BACK) {
            return RemoteConfigValues.getInterAdsOnBack()
                    && isPreload(RemoteConfigValues.getInterAdsBackClickType())
                    && RemoteConfigValues.anyScreenInterFlag(true);
        }
        if (kind == KIND_BOTTOM) {
            return RemoteConfigValues.getBottomNavInterAdsShow()
                    && isPreload(RemoteConfigValues.getBottomNavInterAdsType());
        }
        return RemoteConfigValues.getInterAdsShow()
                && isPreload(RemoteConfigValues.getInterAdsClickType())
                && RemoteConfigValues.anyScreenInterFlag(false);
    }

    @Nullable
    private static InterstitialAd cachedAd(int kind) {
        if (kind == KIND_BACK) {
            return preloadedBackAd;
        }
        if (kind == KIND_BOTTOM) {
            return preloadedBottomAd;
        }
        return preloadedClickAd;
    }

    private static void setCachedAd(int kind, @Nullable InterstitialAd ad) {
        if (kind == KIND_BACK) {
            preloadedBackAd = ad;
        } else if (kind == KIND_BOTTOM) {
            preloadedBottomAd = ad;
        } else {
            preloadedClickAd = ad;
        }
    }

    private static void preload(@Nullable Context context, int kind) {
        if (context == null || !shouldPreload(kind) || cachedAd(kind) != null || inFlight(kind)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        AdPlacement.initializeIfConfigured(appContext);
        if (!AdPlacement.canRequestAds(appContext) || !AdPlacement.isNetworkAvailable(appContext)) {
            return;
        }
        int token = nextToken(kind);
        setInFlight(kind, true);
        InterstitialAd.load(appContext, unitId(kind), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                if (token != token(kind)) {
                    return;
                }
                setInFlight(kind, false);
                setCachedAd(kind, ad);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                if (token != token(kind)) {
                    return;
                }
                setInFlight(kind, false);
            }
        });
    }

    private static void clearPreloaded(int kind) {
        if (kind == KIND_BACK) {
            backPreloadToken++;
        } else if (kind == KIND_BOTTOM) {
            bottomPreloadToken++;
        } else {
            clickPreloadToken++;
        }
        setInFlight(kind, false);
        setCachedAd(kind, null);
    }

    private static boolean inFlight(int kind) {
        if (kind == KIND_BACK) {
            return backPreloadInFlight;
        }
        if (kind == KIND_BOTTOM) {
            return bottomPreloadInFlight;
        }
        return clickPreloadInFlight;
    }

    private static void setInFlight(int kind, boolean value) {
        if (kind == KIND_BACK) {
            backPreloadInFlight = value;
        } else if (kind == KIND_BOTTOM) {
            bottomPreloadInFlight = value;
        } else {
            clickPreloadInFlight = value;
        }
    }

    private static int nextToken(int kind) {
        if (kind == KIND_BACK) {
            return ++backPreloadToken;
        }
        if (kind == KIND_BOTTOM) {
            return ++bottomPreloadToken;
        }
        return ++clickPreloadToken;
    }

    private static int token(int kind) {
        if (kind == KIND_BACK) {
            return backPreloadToken;
        }
        if (kind == KIND_BOTTOM) {
            return bottomPreloadToken;
        }
        return clickPreloadToken;
    }

    private static boolean isPreload(@Nullable String type) {
        return type != null && "preload".equalsIgnoreCase(type.trim());
    }

    @Nullable
    private static Dialog showDialog(@Nullable Activity activity, int layout) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        try {
            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            dialog.setContentView(android.view.LayoutInflater.from(AdTheme.forApp(activity)).inflate(layout, null, false));
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

    private static void dismiss(@Nullable Dialog dialog) {
        if (dialog == null || !dialog.isShowing()) {
            return;
        }
        try {
            dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    private static void run(@Nullable Runnable next) {
        if (next != null) {
            next.run();
        }
    }

    private static void runOnce(AtomicBoolean done, @Nullable Runnable next) {
        if (done.compareAndSet(false, true)) {
            run(next);
        }
    }
}
