package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity;
import com.qrcode.scanner.launcher.adapters.LauncherPagerAdapter;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

import java.lang.ref.WeakReference;

/**
 * Small ad under the Home bottom bar. A new request starts only after the
 * current ad is shown, and only when bottom auto-refresh allows it.
 */
public final class HomeBottomAd {
    private static final String TAG = "HomeBottomAd";
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final State STATE = new State();

    private HomeBottomAd() {
    }

    public static boolean isEnabled() {
        if (!RemoteConfigValues.getMainAdShow()) {
            return false;
        }
        if (AdPlacement.shouldUseQuizPriority()) {
            return true;
        }
        return !unitId().isEmpty();
    }

    public static void attach(Activity activity, FrameLayout host) {
        if (activity == null || host == null) {
            return;
        }
        STATE.activity = new WeakReference<>(activity);
        STATE.host = new WeakReference<>(host);
        log("attach type=" + adType() + " refresh=" + RemoteConfigValues.getMainBottomAdAutoRefresh() + " seconds=" + refreshSeconds() + " priority=" + (AdPlacement.shouldUseQuizPriority() ? "QUIZ" : "GOOGLE") + " quizFail=" + AdPlacement.getGoogleAdFailedShowQuiz());
        if (!isEnabled()) {
            log("hidden Main_Ad_Show=" + RemoteConfigValues.getMainAdShow());
            host.removeAllViews();
            host.setVisibility(View.GONE);
            return;
        }
        bindChrome(activity, host);
        if (!isQrPageOpen(activity)) {
            log("qr page is not open, request waits until swipe");
            hideSlot();
            return;
        }
        present();
    }

    public static void detach(FrameLayout host) {
        FrameLayout current = currentHost();
        if (current == host) {
            log("leave home");
            STATE.resumed = false;
            cancelRefresh();
            STATE.host = null;
            clearChrome();
        }
        if (host != null) {
            host.removeAllViews();
        }
    }

    public static void onHostResume() {
        Activity activity = currentActivity();
        if (!isQrPageOpen(activity)) {
            log("resume ignored, qr page is not open");
            return;
        }
        STATE.resumed = true;
        if (currentHost() == null || !isEnabled()) {
            return;
        }
        log("qr page visible");
        present();
    }

    public static void onPageVisible() {
        STATE.resumed = true;
        log("swiped to qr page");
        if (currentHost() != null && isEnabled() && swipeReturnRefreshDue()) {
            if (STATE.loading) {
                log("right swipe back, request still in flight");
                showShimmer();
            } else {
                log("auto refresh false, right swipe after " + refreshSeconds() + "s, load new ad");
                beginLoad();
            }
        } else if (currentHost() != null && isEnabled()) {
            present();
        }
        ScreenNativeAds.onPageVisible();
    }

    public static void onPageHidden() {
        log("left qr page");
        onHostPause();
        ScreenNativeAds.onPageHidden();
    }

    static boolean isQrPageOpen(@Nullable Activity activity) {
        if (!(activity instanceof LauncherHomeActivity)) {
            return true;
        }
        return ((LauncherHomeActivity) activity).getLauncherCurrentItem() == LauncherPagerAdapter.PAGE_RIGHT;
    }

    public static void onHostPause() {
        if (!STATE.resumed && currentHost() == null) {
            return;
        }
        STATE.resumed = false;
        cancelRefresh();
        log("home hidden, refresh timer stopped");
    }

    private static void present() {
        if (STATE.loading) {
            log("request already in flight, shimmer stays");
            showShimmer();
            return;
        }
        if (!refreshDue()) {
            log("keep current ad, refresh not due");
            if (hasDisplayable()) {
                showCurrent();
            } else {
                hideSlot();
            }
            scheduleRefresh();
            return;
        }
        beginLoad();
    }

    private static void beginLoad() {
        Activity activity = currentActivity();
        if (STATE.loading || activity == null || !isEnabled()) {
            if (STATE.loading) {
                log("skip request until current ad shows");
            }
            return;
        }
        STATE.loading = true;
        showShimmer();
        if (AdPlacement.shouldUseQuizPriority()) {
            log("priority QUIZ, show quiz " + adType());
            STATE.loading = false;
            if (showQuiz(activity)) {
                markShown();
            } else {
                hideSlot();
            }
            return;
        }
        if (isBanner()) {
            log("request small banner id=" + unitId());
            AdPlacement.requestSmallBannerAd(activity, unitId(), adView -> onBannerLoaded(adView), HomeBottomAd::onFailed);
            return;
        }
        log("request small native id=" + unitId());
        AdPlacement.requestLargeNativeAd(activity, unitId(), HomeBottomAd::onNativeLoaded, HomeBottomAd::onFailed);
    }

    private static void onBannerLoaded(@Nullable AdView adView) {
        STATE.loading = false;
        if (adView == null) {
            onFailed();
            return;
        }
        if (STATE.banner != null && STATE.banner != adView) {
            STATE.banner.destroy();
        }
        STATE.banner = adView;
        STATE.quizVisible = false;
        log("banner shown");
        markShown();
        if (currentHost() != null) {
            showBanner();
        }
    }

    private static void onNativeLoaded(@Nullable NativeAd nativeAd) {
        STATE.loading = false;
        if (nativeAd == null) {
            onFailed();
            return;
        }
        NativeAd previous = STATE.nativeAd;
        STATE.nativeAd = nativeAd;
        STATE.quizVisible = false;
        log("native shown");
        markShown();
        Activity activity = currentActivity();
        if (activity != null && currentHost() != null) {
            showNative(activity);
        }
        if (previous != null && previous != nativeAd) {
            previous.destroy();
        }
    }

    private static void onFailed() {
        STATE.loading = false;
        Activity activity = currentActivity();
        log("google failed, quizFallback=" + AdPlacement.getGoogleAdFailedShowQuiz());
        if (activity != null && currentHost() != null && AdPlacement.getGoogleAdFailedShowQuiz() && showQuiz(activity)) {
            log("quiz shown");
            markShown();
            return;
        }
        STATE.quizVisible = false;
        if (hasGoogleAd() && activity != null && currentHost() != null) {
            log("refresh failed, keep previous ad");
            showCurrent();
            markShown();
            return;
        }
        log("shimmer gone");
        hideSlot();
        STATE.lastShownAt = System.currentTimeMillis();
        scheduleRefresh();
    }

    private static void bindChrome(Activity activity, FrameLayout host) {
        host.setVisibility(View.VISIBLE);
        host.removeAllViews();
        View root = LayoutInflater.from(activity).inflate(R.layout.view_home_bottom_ad, host, false);
        host.addView(root, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        STATE.bannerContainer = root.findViewById(R.id.rlBannerAdView);
        STATE.bannerShimmer = root.findViewById(R.id.slBannerShimmer);
        STATE.bannerContent = root.findViewById(R.id.llBannerAd);
        STATE.nativeContainer = root.findViewById(R.id.rlNativeAdView);
        STATE.nativeShimmer = root.findViewById(R.id.slNativeShimmer);
        STATE.nativeContent = root.findViewById(R.id.flNativeAd);
    }

    private static void showShimmer() {
        FrameLayout host = currentHost();
        if (host != null) {
            host.setVisibility(View.VISIBLE);
        }
        if (isBanner()) {
            setGone(STATE.nativeContainer);
            setVisible(STATE.bannerContainer);
            clearBannerContent();
            startShimmer(STATE.bannerShimmer);
            setGone(STATE.bannerContent);
            return;
        }
        setGone(STATE.bannerContainer);
        setVisible(STATE.nativeContainer);
        if (STATE.nativeContent != null) {
            STATE.nativeContent.removeAllViews();
            STATE.nativeContent.setVisibility(View.GONE);
        }
        startShimmer(STATE.nativeShimmer);
    }

    private static void showCurrent() {
        Activity activity = currentActivity();
        if (activity == null) {
            return;
        }
        if (STATE.quizVisible) {
            showQuiz(activity);
            return;
        }
        if (isBanner() && STATE.banner != null) {
            showBanner();
            return;
        }
        if (!isBanner() && STATE.nativeAd != null) {
            showNative(activity);
        }
    }

    private static void showBanner() {
        if (STATE.banner == null || STATE.bannerContent == null) {
            return;
        }
        FrameLayout host = currentHost();
        if (host != null) {
            host.setVisibility(View.VISIBLE);
        }
        setGone(STATE.nativeContainer);
        setVisible(STATE.bannerContainer);
        stopShimmer(STATE.bannerShimmer);
        ViewGroup parent = (ViewGroup) STATE.banner.getParent();
        if (parent != null) {
            parent.removeView(STATE.banner);
        }
        STATE.bannerContent.removeAllViews();
        STATE.bannerContent.addView(STATE.banner);
        STATE.bannerContent.setVisibility(View.VISIBLE);
    }

    private static void showNative(Activity activity) {
        if (STATE.nativeAd == null || STATE.nativeContent == null) {
            return;
        }
        FrameLayout host = currentHost();
        if (host != null) {
            host.setVisibility(View.VISIBLE);
        }
        setGone(STATE.bannerContainer);
        setVisible(STATE.nativeContainer);
        stopShimmer(STATE.nativeShimmer);
        NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.native_small_ad_layout, STATE.nativeContent, false);
        AdPlacement.populateNativeAdView(STATE.nativeAd, adView, "small");
        STATE.nativeContent.removeAllViews();
        STATE.nativeContent.addView(adView);
        STATE.nativeContent.setVisibility(View.VISIBLE);
    }

    private static boolean showQuiz(Activity activity) {
        if (isBanner()) {
            boolean shown = QuizAds.showBanner(activity, STATE.bannerContainer, STATE.bannerShimmer, STATE.bannerContent);
            if (!shown) {
                return false;
            }
            STATE.quizVisible = true;
            setGone(STATE.nativeContainer);
            FrameLayout host = currentHost();
            if (host != null) {
                host.setVisibility(View.VISIBLE);
            }
            return true;
        }
        boolean shown = QuizAds.showNative(activity, STATE.nativeContainer, STATE.nativeShimmer, STATE.nativeContent, "small");
        if (!shown) {
            return false;
        }
        STATE.quizVisible = true;
        setGone(STATE.bannerContainer);
        FrameLayout host = currentHost();
        if (host != null) {
            host.setVisibility(View.VISIBLE);
        }
        return true;
    }

    private static void hideSlot() {
        stopShimmer(STATE.bannerShimmer);
        stopShimmer(STATE.nativeShimmer);
        setGone(STATE.bannerContainer);
        setGone(STATE.nativeContainer);
        FrameLayout host = currentHost();
        if (host != null) {
            host.setVisibility(View.GONE);
        }
    }

    private static void markShown() {
        STATE.lastShownAt = System.currentTimeMillis();
        scheduleRefresh();
    }

    private static void scheduleRefresh() {
        cancelRefresh();
        if (!RemoteConfigValues.getMainBottomAdAutoRefresh() || !STATE.resumed || currentHost() == null) {
            return;
        }
        int seconds = refreshSeconds();
        if (seconds <= 0 || STATE.lastShownAt <= 0L) {
            return;
        }
        long delay = seconds * 1000L - (System.currentTimeMillis() - STATE.lastShownAt);
        if (delay <= 0L) {
            log("auto refresh due while on home");
            beginLoad();
            return;
        }
        int token = ++STATE.refreshToken;
        log("auto refresh in " + delay + "ms");
        Runnable runnable = () -> {
            if (STATE.refreshToken != token || !STATE.resumed || currentHost() == null) {
                return;
            }
            log("auto refresh tick");
            beginLoad();
        };
        STATE.refreshRunnable = runnable;
        HANDLER.postDelayed(runnable, delay);
    }

    private static void cancelRefresh() {
        STATE.refreshToken++;
        if (STATE.refreshRunnable != null) {
            HANDLER.removeCallbacks(STATE.refreshRunnable);
            STATE.refreshRunnable = null;
        }
    }

    private static boolean refreshDue() {
        if (STATE.lastShownAt <= 0L) {
            return true;
        }
        if (!RemoteConfigValues.getMainBottomAdAutoRefresh()) {
            return false;
        }
        int seconds = refreshSeconds();
        if (seconds <= 0) {
            return false;
        }
        return System.currentTimeMillis() - STATE.lastShownAt >= seconds * 1000L;
    }

    /** Auto-refresh off: a new request only after leaving to the launcher home and swiping back. */
    private static boolean swipeReturnRefreshDue() {
        if (RemoteConfigValues.getMainBottomAdAutoRefresh() || STATE.lastShownAt <= 0L) {
            return false;
        }
        int seconds = refreshSeconds();
        if (seconds <= 0) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - STATE.lastShownAt;
        if (elapsed < seconds * 1000L) {
            log("right swipe before " + seconds + "s, keep current ad");
            return false;
        }
        return true;
    }

    private static boolean hasDisplayable() {
        return STATE.quizVisible || hasGoogleAd();
    }

    private static boolean hasGoogleAd() {
        return isBanner() ? STATE.banner != null : STATE.nativeAd != null;
    }

    private static boolean isBanner() {
        return "banner".equalsIgnoreCase(adType());
    }

    private static String adType() {
        String type = RemoteConfigValues.getMainAdType();
        return type == null ? "" : type.trim();
    }

    private static String unitId() {
        String id = isBanner() ? RemoteConfigValues.getMainBannerId() : RemoteConfigValues.getMainNativeId();
        return id == null ? "" : id.trim();
    }

    private static int refreshSeconds() {
        return Math.max(RemoteConfigValues.getMainBottomAdAutoSecond(), 0);
    }

    private static void startShimmer(@Nullable ShimmerFrameLayout shimmer) {
        if (shimmer == null) {
            return;
        }
        shimmer.setVisibility(View.VISIBLE);
        shimmer.startShimmer();
    }

    private static void stopShimmer(@Nullable ShimmerFrameLayout shimmer) {
        if (shimmer == null) {
            return;
        }
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
    }

    private static void clearBannerContent() {
        if (STATE.bannerContent == null) {
            return;
        }
        if (STATE.banner != null && STATE.banner.getParent() == STATE.bannerContent) {
            STATE.bannerContent.removeView(STATE.banner);
        }
        STATE.bannerContent.removeAllViews();
    }

    private static void setVisible(@Nullable View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private static void setGone(@Nullable View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private static void clearChrome() {
        STATE.bannerContainer = null;
        STATE.bannerShimmer = null;
        STATE.bannerContent = null;
        STATE.nativeContainer = null;
        STATE.nativeShimmer = null;
        STATE.nativeContent = null;
    }

    @Nullable
    private static Activity currentActivity() {
        Activity activity = STATE.activity == null ? null : STATE.activity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        return activity;
    }

    @Nullable
    private static FrameLayout currentHost() {
        return STATE.host == null ? null : STATE.host.get();
    }

    private static void log(String message) {
        Log.d(TAG, message);
    }

    private static final class State {
        long lastShownAt;
        boolean loading;
        boolean quizVisible;
        boolean resumed;
        int refreshToken;
        @Nullable Runnable refreshRunnable;
        @Nullable AdView banner;
        @Nullable NativeAd nativeAd;
        @Nullable WeakReference<Activity> activity;
        @Nullable WeakReference<FrameLayout> host;
        @Nullable RelativeLayout bannerContainer;
        @Nullable ShimmerFrameLayout bannerShimmer;
        @Nullable LinearLayout bannerContent;
        @Nullable RelativeLayout nativeContainer;
        @Nullable ShimmerFrameLayout nativeShimmer;
        @Nullable FrameLayout nativeContent;
    }
}
