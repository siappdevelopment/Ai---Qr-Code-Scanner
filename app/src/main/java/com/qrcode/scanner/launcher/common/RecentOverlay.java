package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qrcode.scanner.launcher.activities.LauncherHomeActivity;
import com.qrcode.scanner.spapp.R;

/**
 * The instant overlay of the Recent screen. The system overview opens at once and covers an activity that is started
 * before ~250 ms, so the same page is drawn as an overlay window (TYPE_APPLICATION_OVERLAY, needs "Display over other
 * apps") ~100 ms after the Recents key. The real {@link com.qrcode.scanner.launcher.activities.RecentActivity} is started
 * above the overview after the settle time and the overlay is removed once the activity has focus. No ads here.
 */
public final class RecentOverlay {
    private static final String TAG = "RecentOverlay";
    private static final long SAFETY_HIDE_MS = 4_000L;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static View view;
    private static WindowManager windowManager;
    private static volatile boolean handledByUser;
    private static final Runnable SAFETY = RecentOverlay::hide;

    private static volatile int uiMode;

    private RecentOverlay() {
    }

    /** Remembers the light / dark mode of the app's screens so the overlay matches them. */
    public static void rememberUiMode(int mode) {
        uiMode = mode;
    }

    public static boolean isShowing() {
        return view != null;
    }

    /** The user already acted on the overlay (closed it / opened an app): the activity must not be started after it. */
    public static boolean handledByUser() {
        return handledByUser;
    }

    public static void markHandled() {
        handledByUser = true;
    }

    public static boolean canShow(@NonNull Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    /** Draws the page now. Returns false (and draws nothing) without the overlay permission. */
    public static boolean show(@NonNull Context context) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            HANDLER.post(() -> show(context));
            return canShow(context);
        }
        final Context app = context.getApplicationContext();
        handledByUser = false;
        if (view != null) {
            return true;
        }
        if (!canShow(app)) {
            Log.d(TAG, "no 'Display over other apps' permission: the page shows when the activity is up");
            return false;
        }
        try {
            // Same light / dark as the activity (the app theme may differ from the system one).
            android.content.res.Configuration config = new android.content.res.Configuration(app.getResources().getConfiguration());
            if (uiMode != 0) {
                config.uiMode = uiMode;
            }
            Context themed = new ContextThemeWrapper(app.createConfigurationContext(config), R.style.Theme_RecentScreen);
            final View root = LayoutInflater.from(themed).inflate(R.layout.activity_recent, null, false);
            root.setFitsSystemWindows(false);
            View ad = root.findViewById(R.id.rlAdView);
            if (ad != null) {
                ad.setVisibility(View.GONE);
            }
            // Phones place overlay windows differently: after the first layout pad only what the bars still cover.
            root.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    int[] loc = new int[2];
                    root.getLocationOnScreen(loc);
                    int realHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
                    int top = Math.max(0, barHeight("status_bar_height") - loc[1]);
                    int bottom = Math.max(0, barHeight("navigation_bar_height") - Math.max(0, realHeight - (loc[1] + root.getHeight())));
                    root.setPadding(0, top, 0, bottom);
                }
            });
            new RecentPage(root, new OverlayHost(app)).bind();

            WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.OPAQUE);
            lp.windowAnimations = 0;
            wm.addView(root, lp);
            view = root;
            windowManager = wm;
            HANDLER.removeCallbacks(SAFETY);
            HANDLER.postDelayed(SAFETY, SAFETY_HIDE_MS);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "overlay could not be shown: " + t);
            view = null;
            return false;
        }
    }

    public static void hide() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            HANDLER.post(RecentOverlay::hide);
            return;
        }
        HANDLER.removeCallbacks(SAFETY);
        View v = view;
        view = null;
        if (v == null || windowManager == null) {
            return;
        }
        try {
            windowManager.removeViewImmediate(v);
        } catch (Throwable ignored) {
        }
    }

    private static int barHeight(String name) {
        Resources r = Resources.getSystem();
        int id = r.getIdentifier(name, "dimen", "android");
        return id > 0 ? r.getDimensionPixelSize(id) : 0;
    }

    /** Any action on the overlay marks it handled, removes it and goes on without ads. */
    private static final class OverlayHost implements RecentPage.Host {
        private final Context app;

        OverlayHost(Context app) {
            this.app = app;
        }

        @Override
        public void onClose() {
            markHandled();
            hide();
            startHome();
        }

        @Override
        public void onOpenApp(@NonNull String packageName) {
            markHandled();
            hide();
            RecentPage.Launch.app(app, packageName);
        }

        @Override
        public void onOpenFeature(@NonNull Intent intent) {
            markHandled();
            hide();
            try {
                app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {
            }
        }

        private void startHome() {
            try {
                app.startActivity(new Intent(app, LauncherHomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    static View currentView() {
        return view;
    }
}
