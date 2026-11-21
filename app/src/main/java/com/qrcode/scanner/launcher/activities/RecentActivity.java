package com.qrcode.scanner.launcher.activities;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.RecentBackAd;
import com.qrcode.scanner.launcher.common.RecentInlineAd;
import com.qrcode.scanner.launcher.common.RecentOverlay;
import com.qrcode.scanner.launcher.common.RecentPage;
import com.qrcode.scanner.spapp.R;

/**
 * The app's own Recent screen: it replaces Android's Recents while this app is the default Home app (opened by
 * {@link com.qrcode.scanner.launcher.common.RecentsKey}, shown at once by {@link RecentOverlay}). The content is
 * {@link RecentPage}; the inline ad is Google native / banner only ({@code Recent_Ad_*}); closing runs the back ad of
 * {@code Recent_Back_Ad_*} ({@link RecentBackAd}). No Quiz ad is shown here unless the back-ad sequence has an explicit
 * {@code quiz_*} step.
 */
public class RecentActivity extends AppCompatActivity implements RecentPage.Host {
    public static final String EXTRA_RETURN_TO = "extra_return_to";
    /** Opened over another app: closing the screen just returns to that app / launcher (no extra Home start). */
    public static final String EXTRA_OVER_OTHER_APP = "extra_over_other_app";
    private static final long INLINE_GIVE_UP_MS = 10_000L;

    /** The user closed the page or opened something from it: no automatic re-open after that. */
    public static volatile boolean userLeft;
    private static volatile boolean resumed;

    private RecentPage page;
    private RelativeLayout rlAdView;
    private RelativeLayout rlBannerAdView;
    private RelativeLayout rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer;
    private ShimmerFrameLayout slNativeShimmer;
    private LinearLayout llBannerAd;
    private FrameLayout flNativeAd;

    private Runnable pendingAdLoad;
    private NativeAd inlineNative;
    private AdView inlineBanner;
    private boolean visitStarted;
    private boolean closeAdInProgress;
    private boolean appLaunchInProgress;

    /** The page is on screen right now (resumed); the key listener re-opens it when the system overview covered it. */
    public static boolean isResumedNow() {
        return resumed;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        userLeft = false;
        if (!AdPlacement.getRecentScreenShow()) {
            // Switched off in Firebase between the key and the open: never show it.
            RecentOverlay.hide();
            closeScreen();
            return;
        }
        RecentBackAd.beginVisit();
        visitStarted = true;

        setContentView(R.layout.activity_recent);
        findAdViews();
        page = new RecentPage(findViewById(R.id.recentRoot), this);
        page.bind();
        setupInlineAd();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onClose();
            }
        });
        bringToFront();

        // First frame first: ad loads follow it, so the page itself appears instantly.
        final View decor = getWindow().getDecorView();
        decor.post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (pendingAdLoad != null) {
                pendingAdLoad.run();
                pendingAdLoad = null;
            }
            // Recent_Ad_Load_Type = PreLoad: ask for the inline ad of the next visit (the cached one was just shown).
            if (AdPlacement.isRecentAdPreload()) {
                RecentInlineAd.preload(this);
            }
            if (AdPlacement.isNetworkAvailable(this) && AdPlacement.showRecentBackAd(this)) {
                RecentBackAd.loadAd(this);
            }
        });
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (page != null) {
            page.refresh();
        }
    }

    @Override
    protected void onPause() {
        resumed = false;
        super.onPause();
    }

    // ---------------------------------------------------------------- opening reliably

    /** The overview can still end up above this page: ask for the page's task on top at once, and again a moment later. */
    private void bringToFront() {
        View root = findViewById(R.id.recentRoot);
        for (long delay : new long[]{0L, 150L, 400L}) {
            root.postDelayed(this::moveTaskToFront, delay);
        }
    }

    private void moveTaskToFront() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        try {
            ActivityManager manager = getSystemService(ActivityManager.class);
            if (manager == null) {
                return;
            }
            for (ActivityManager.AppTask task : manager.getAppTasks()) {
                if (task.getTaskInfo() != null && task.getTaskInfo().taskId == getTaskId()) {
                    task.moveToFront();
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** The overlay that showed the page at once goes away when this page really is on screen (the short delay avoids a flash). */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && RecentOverlay.isShowing()) {
            getWindow().getDecorView().postDelayed(RecentOverlay::hide, 600L);
        }
    }

    // ---------------------------------------------------------------- inline ad (Google only)

    private void findAdViews() {
        rlAdView = findViewById(R.id.rlAdView);
        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);
        rlNativeAdView = findViewById(R.id.rlNativeAdView);
        slNativeShimmer = findViewById(R.id.slNativeShimmer);
        flNativeAd = findViewById(R.id.flNativeAd);
    }

    /** Decides the inline ad. Hidden unless Recent_Ad_Show, network, ads allowed and NOT Quiz priority (never a Quiz ad). */
    private void setupInlineAd() {
        boolean banner = "banner".equalsIgnoreCase(AdPlacement.getRecentAdType());
        String id = banner ? AdPlacement.getRecentBannerId() : AdPlacement.getRecentNativeId();
        if (!AdPlacement.getRecentAdShow() || id == null || id.trim().isEmpty() || !AdPlacement.isNetworkAvailable(this)
                || !AdPlacement.canRequestAds(this) || AdPlacement.shouldUseQuizPriority()) {
            rlAdView.setVisibility(View.GONE);
            return;
        }
        rlAdView.setVisibility(View.VISIBLE);
        rlBannerAdView.setVisibility(banner ? View.VISIBLE : View.GONE);
        rlNativeAdView.setVisibility(banner ? View.GONE : View.VISIBLE);
        if (AdPlacement.isRecentAdPreload()) {
            // PreLoad: a cached ad shows at once (no shimmer, no request now); otherwise this visit loads its own ad.
            if (banner) {
                AdView cached = RecentInlineAd.takeBanner();
                if (cached != null) {
                    showCachedBanner(cached);
                    return;
                }
            } else {
                NativeAd cached = RecentInlineAd.takeNative();
                if (cached != null) {
                    showCachedNative(cached);
                    return;
                }
            }
        }
        // googleOnly = true: a Google failure never shows a Quiz ad on this screen.
        if (banner) {
            pendingAdLoad = () -> AdPlacement.loadAdaptiveBannerAd(this, id.trim(), rlBannerAdView, slBannerShimmer, llBannerAd, true);
        } else {
            pendingAdLoad = () -> AdPlacement.loadNativeAd(this, id.trim(), rlNativeAdView, slNativeShimmer, flNativeAd, "large", null, null, true);
        }
        // Give up after 10 s: no ad, no empty gap.
        rlAdView.postDelayed(() -> {
            ShimmerFrameLayout shimmer = banner ? slBannerShimmer : slNativeShimmer;
            if (!isFinishing() && shimmer != null && shimmer.getVisibility() == View.VISIBLE) {
                shimmer.stopShimmer();
                rlAdView.setVisibility(View.GONE);
            }
        }, INLINE_GIVE_UP_MS);
    }

    private void showCachedNative(NativeAd ad) {
        if (inlineNative != null) {
            inlineNative.destroy();
        }
        inlineNative = ad;
        AdPlacement.bindLoadedNative(this, flNativeAd, ad, "large");
        slNativeShimmer.stopShimmer();
        slNativeShimmer.setVisibility(View.GONE);
        flNativeAd.setVisibility(View.VISIBLE);
    }

    private void showCachedBanner(AdView view) {
        inlineBanner = view;
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        llBannerAd.removeAllViews();
        llBannerAd.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        slBannerShimmer.stopShimmer();
        slBannerShimmer.setVisibility(View.GONE);
        llBannerAd.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------------- closing and actions

    /** Close button and system Back: the back ad (when due), then back to the screen below. */
    @Override
    public void onClose() {
        if (closeAdInProgress) {
            return;
        }
        closeAdInProgress = true;
        if (!AdPlacement.showRecentBackAd(this)) {
            closeAdInProgress = false;
            closeScreen();
            return;
        }
        RecentBackAd.fullScreenAdShow(this, shown -> {
            if (shown) {
                // Stamps the per-day interval and moves the sequence on; only a really shown ad counts.
                AdPlacement.setRecentLastShowTime(getApplicationContext(), System.currentTimeMillis());
            }
            closeAdInProgress = false;
            closeScreen();
        });
    }

    @Override
    public void onOpenApp(@NonNull String packageName) {
        if (appLaunchInProgress) {
            return;
        }
        appLaunchInProgress = true;
        userLeft = true;
        // No launcher click ad here: this screen shows no Quiz ad by itself.
        if (RecentPage.Launch.app(this, packageName)) {
            finishNoAnimation();
        } else {
            appLaunchInProgress = false;
            userLeft = false;
            page.refresh();
        }
    }

    @Override
    public void onOpenFeature(@NonNull Intent intent) {
        userLeft = true;
        try {
            startActivity(intent);
            finishNoAnimation();
        } catch (Exception e) {
            userLeft = false;
        }
    }

    private void finishNoAnimation() {
        RecentOverlay.hide();
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * Brings the screen that was below back to the front before finishing. That also dismisses the system overview, which
     * stays open on some phones when the page only finishes.
     */
    private void closeScreen() {
        userLeft = true;
        RecentOverlay.hide();
        if (getIntent().getBooleanExtra(EXTRA_OVER_OTHER_APP, false)) {
            // Opened over another app: just finish (Android then shows the launcher Home).
            finishNoAnimation();
            return;
        }
        ComponentName returnTo = getIntent().getParcelableExtra(EXTRA_RETURN_TO);
        Intent intent = returnTo != null
                ? new Intent().setComponent(returnTo).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                : new Intent(this, LauncherHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(intent);
        } catch (Exception ignored) {
        }
        finishNoAnimation();
    }

    @Override
    protected void onDestroy() {
        resumed = false;
        // Unused back ads of this visit are dropped, a late answer is ignored: no ad shows on its own at the next opening.
        if (visitStarted) {
            RecentBackAd.endVisit();
        }
        if (inlineNative != null) {
            inlineNative.destroy();
            inlineNative = null;
        }
        if (inlineBanner != null) {
            inlineBanner.destroy();
            inlineBanner = null;
        }
        super.onDestroy();
    }
}
