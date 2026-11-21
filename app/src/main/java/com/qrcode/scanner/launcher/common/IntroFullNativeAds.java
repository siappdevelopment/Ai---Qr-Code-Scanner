package com.qrcode.scanner.launcher.common;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.nativead.NativeAd;

/**
 * Loader of the two full-screen native ads of the intro pager: slot 1 sits between Intro1 and Intro2, slot 2 between
 * Intro2 and Intro3 (Intro_Native1_* / Intro_Native2_* in the IntroScreen block).
 * <ul>
 *   <li>Both are preloaded once per intro session when the intro opens.</li>
 *   <li>The pager inserts an ad page only when its ad is already LOADED; a missing or failed ad never adds a blank or
 *       loading page, and a failed slot is never retried.</li>
 *   <li>{@link #release()} runs when the intro is finished.</li>
 * </ul>
 */
public final class IntroFullNativeAds {
    private static final int SLOTS = 2;
    private static final NativeAd[] LOADED = new NativeAd[SLOTS + 1];
    private static final boolean[] REQUESTED = new boolean[SLOTS + 1];
    private static int session;
    @Nullable
    private static Runnable onLoadedListener;

    private IntroFullNativeAds() {
    }

    /** Called (main thread) whenever a slot finishes loading, so the pager can insert its ad page. Pass null to clear. */
    public static void setOnLoadedListener(@Nullable Runnable listener) {
        onLoadedListener = listener;
    }

    /** Starts the loads that are enabled and not requested yet in this intro session. Safe to call many times. */
    public static void preloadAll(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }
        for (int slot = 1; slot <= SLOTS; slot++) {
            preload(activity, slot);
        }
    }

    private static void preload(@NonNull Activity activity, int slot) {
        if (REQUESTED[slot] || !IntroConfig.isNativeSlotEnabled(activity, slot)) {
            return;
        }
        REQUESTED[slot] = true;
        final int mySession = session;
        AdPlacement.preloadFullNativeAd(activity, IntroConfig.getNativeId(activity, slot), nativeAd -> {
            if (mySession != session || LOADED[slot] != null) {
                nativeAd.destroy();
                return;
            }
            LOADED[slot] = nativeAd;
            Runnable listener = onLoadedListener;
            if (listener != null) {
                listener.run();
            }
        }, () -> {
            // A failed slot stays requested: it is never retried in this session.
        });
    }

    /** The loaded ad of {@code slot} (not removed), or null. */
    @Nullable
    public static NativeAd peek(int slot) {
        return slot >= 1 && slot <= SLOTS ? LOADED[slot] : null;
    }

    /** Hands the loaded ad of {@code slot} to the caller (the pager page that now owns and destroys it). */
    @Nullable
    public static NativeAd take(int slot) {
        if (slot < 1 || slot > SLOTS) {
            return null;
        }
        NativeAd ad = LOADED[slot];
        LOADED[slot] = null;
        return ad;
    }

    /** Destroys unused ads and ends the session (late callbacks of this session are discarded). */
    public static void release() {
        session++;
        onLoadedListener = null;
        for (int slot = 1; slot <= SLOTS; slot++) {
            if (LOADED[slot] != null) {
                LOADED[slot].destroy();
                LOADED[slot] = null;
            }
            REQUESTED[slot] = false;
        }
    }
}
