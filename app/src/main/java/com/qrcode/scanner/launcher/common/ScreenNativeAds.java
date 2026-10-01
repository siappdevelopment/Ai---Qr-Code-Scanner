package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.nativead.NativeAd;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

import java.lang.ref.WeakReference;

/**
 * Big native ads for Home and Settings, using the same card, shimmer, and ad
 * slot as the right-swipe page. A new request runs only when that screen is
 * opened again after its refresh seconds have passed.
 */
public final class ScreenNativeAds {
    public enum Slot {
        HOME,
        SETTINGS
    }

    private static final SlotState HOME_STATE = new SlotState();
    private static final SlotState SETTINGS_STATE = new SlotState();

    private ScreenNativeAds() {
    }

    public static boolean isEnabled(Slot slot) {
        if (slot == Slot.HOME) {
            return RemoteConfigValues.getMainBigTopAdShow() && !unitId(slot).isEmpty();
        }
        return RemoteConfigValues.getSettingsFragmentNativeAdShow() && !unitId(slot).isEmpty();
    }

    public static void attach(Activity activity, FrameLayout host, Slot slot) {
        if (activity == null || host == null || slot == null) {
            return;
        }
        SlotState state = state(slot);
        state.activity = new WeakReference<>(activity);
        state.host = new WeakReference<>(host);
        if (!isEnabled(slot)) {
            host.removeAllViews();
            host.setVisibility(View.GONE);
            return;
        }
        bindChrome(activity, host, state);
        if (!HomeBottomAd.isQrPageOpen(activity)) {
            hideSlot(state);
            return;
        }
        showOrLoad(activity, slot);
    }

    public static void onPageVisible() {
        Activity activity = null;
        for (Slot slot : Slot.values()) {
            SlotState state = state(slot);
            if (currentHost(state) == null) {
                continue;
            }
            if (activity == null) {
                activity = state.activity == null ? null : state.activity.get();
            }
            if (activity == null || activity.isFinishing()) {
                return;
            }
            showOrLoad(activity, slot);
        }
    }

    public static void onPageHidden() {
        for (Slot slot : Slot.values()) {
            SlotState state = state(slot);
            if (currentHost(state) != null) {
                hideSlot(state);
            }
        }
    }

    private static void showOrLoad(Activity activity, Slot slot) {
        SlotState state = state(slot);
        if (remainingWait(slot) > 0L && state.ad != null) {
            showCachedAd(activity, state);
            return;
        }
        if (remainingWait(slot) > 0L && state.quizVisible) {
            if (!showQuiz(activity, state)) {
                hideSlot(state);
            }
            return;
        }
        if (state.loading) {
            showShimmer(state);
            return;
        }
        beginLoad(activity, slot);
    }

    public static void detach(FrameLayout host, Slot slot) {
        if (slot != null) {
            SlotState state = state(slot);
            FrameLayout current = currentHost(state);
            if (current == host) {
                state.host = null;
                state.container = null;
                state.shimmer = null;
                state.content = null;
            }
        }
        if (host != null) {
            host.removeAllViews();
        }
    }

    private static void beginLoad(Activity activity, Slot slot) {
        SlotState state = state(slot);
        if (state.loading || activity == null || !isEnabled(slot)) {
            return;
        }
        state.loading = true;
        showShimmer(state);
        AdPlacement.requestLargeNativeAd(activity, unitId(slot), nativeAd -> onLoaded(slot, nativeAd), () -> onFailed(slot));
    }

    private static void onLoaded(Slot slot, @Nullable NativeAd nativeAd) {
        SlotState state = state(slot);
        state.loading = false;
        state.lastCycleAt = System.currentTimeMillis();
        if (nativeAd == null) {
            hideSlot(state);
            return;
        }
        NativeAd previous = state.ad;
        state.ad = nativeAd;
        state.quizVisible = false;
        Activity activity = currentActivity(state);
        if (activity != null && currentHost(state) != null) {
            showCachedAd(activity, state);
        }
        if (previous != null && previous != nativeAd) {
            previous.destroy();
        }
    }

    private static void onFailed(Slot slot) {
        SlotState state = state(slot);
        state.loading = false;
        state.lastCycleAt = System.currentTimeMillis();
        Activity activity = currentActivity(state);
        if (activity != null && currentHost(state) != null && AdPlacement.getGoogleAdFailedShowQuiz() && showQuiz(activity, state)) {
            state.quizVisible = true;
            return;
        }
        state.quizVisible = false;
        if (state.ad != null && activity != null && currentHost(state) != null) {
            showCachedAd(activity, state);
            return;
        }
        hideSlot(state);
    }

    private static void bindChrome(Activity activity, FrameLayout host, SlotState state) {
        host.setVisibility(View.VISIBLE);
        host.removeAllViews();
        View root = LayoutInflater.from(activity).inflate(R.layout.view_big_native_ad, host, false);
        host.addView(root, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        state.container = root.findViewById(R.id.rlNativeAdView);
        state.shimmer = root.findViewById(R.id.slNativeShimmer);
        state.content = root.findViewById(R.id.flNativeAd);
    }

    private static void showShimmer(SlotState state) {
        RelativeLayout container = state.container;
        if (container == null) {
            return;
        }
        FrameLayout host = currentHost(state);
        if (host != null) {
            host.setVisibility(View.VISIBLE);
        }
        container.setVisibility(View.VISIBLE);
        if (state.content != null) {
            state.content.removeAllViews();
            state.content.setVisibility(View.GONE);
        }
        if (state.shimmer != null) {
            state.shimmer.setVisibility(View.VISIBLE);
            state.shimmer.startShimmer();
        }
    }

    private static void showCachedAd(Activity activity, SlotState state) {
        if (state.content == null || state.ad == null) {
            return;
        }
        FrameLayout host = currentHost(state);
        if (host != null) {
            host.setVisibility(View.VISIBLE);
        }
        if (state.container != null) {
            state.container.setVisibility(View.VISIBLE);
        }
        hideShimmer(state);
        state.content.setVisibility(View.VISIBLE);
        AdPlacement.showLargeNative(activity, state.content, state.ad);
    }

    private static boolean showQuiz(Activity activity, SlotState state) {
        if (activity == null || state.container == null || state.content == null) {
            return false;
        }
        boolean shown = QuizAds.showNative(activity, state.container, state.shimmer, state.content, "large");
        if (!shown) {
            return false;
        }
        FrameLayout host = currentHost(state);
        if (host != null) {
            host.setVisibility(View.VISIBLE);
        }
        return true;
    }

    private static void hideShimmer(SlotState state) {
        if (state.shimmer == null) {
            return;
        }
        state.shimmer.stopShimmer();
        state.shimmer.setVisibility(View.GONE);
    }

    private static void hideSlot(SlotState state) {
        hideShimmer(state);
        if (state.content != null) {
            state.content.removeAllViews();
            state.content.setVisibility(View.GONE);
        }
        if (state.container != null) {
            state.container.setVisibility(View.GONE);
        }
        FrameLayout host = currentHost(state);
        if (host != null) {
            host.setVisibility(View.GONE);
        }
    }

    private static long remainingWait(Slot slot) {
        SlotState state = state(slot);
        if (state.lastCycleAt <= 0L) {
            return 0L;
        }
        int seconds = refreshSeconds(slot);
        if (seconds <= 0) {
            return Long.MAX_VALUE;
        }
        long elapsed = System.currentTimeMillis() - state.lastCycleAt;
        long window = seconds * 1000L;
        return elapsed >= window ? 0L : window - elapsed;
    }

    private static String unitId(Slot slot) {
        String id = slot == Slot.HOME ? RemoteConfigValues.getMainNativeId() : RemoteConfigValues.getSettingsFragmentNativeId();
        return id == null ? "" : id.trim();
    }

    private static int refreshSeconds(Slot slot) {
        int seconds = slot == Slot.HOME ? RemoteConfigValues.getMainAdAutoSecond() : RemoteConfigValues.getSettingsFragmentNativeSecond();
        return Math.max(seconds, 0);
    }

    private static SlotState state(Slot slot) {
        return slot == Slot.HOME ? HOME_STATE : SETTINGS_STATE;
    }

    @Nullable
    private static Activity currentActivity(SlotState state) {
        return state.activity == null ? null : state.activity.get();
    }

    @Nullable
    private static FrameLayout currentHost(SlotState state) {
        return state.host == null ? null : state.host.get();
    }

    private static final class SlotState {
        @Nullable NativeAd ad;
        long lastCycleAt;
        boolean loading;
        boolean quizVisible;
        @Nullable WeakReference<Activity> activity;
        @Nullable WeakReference<FrameLayout> host;
        @Nullable RelativeLayout container;
        @Nullable ShimmerFrameLayout shimmer;
        @Nullable FrameLayout content;
    }
}
