package com.qrcode.scanner.launcher.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.Log;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.QuizAds;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

public class EventPromptActivity extends AppCompatActivity {
    public static final String EXTRA_KIND = "event_kind";
    public static final String KIND_UNINSTALL = "uninstall";
    public static final String KIND_CHARGE_IN = "charge_in";
    public static final String KIND_CHARGE_OUT = "charge_out";

    private ProgressBar pbEventHold;
    private TextView btnEventDone;
    private ValueAnimator holdAnimator;
    private boolean canExit;
    private boolean exiting;
    private int adRequest;
    private int adCardColor = Color.WHITE;
    @Nullable
    private NativeAd loadedNativeAd;
    @Nullable
    private AdView loadedBanner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("EventPrompt", "activity created kind=" + kindFrom(getIntent()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        setContentView(R.layout.activity_event_prompt);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        hideNavigationBar();
        bindKind(kindFrom(getIntent()));
        bindClicks();
        setupBackBlock();
        loadBottomAd();
        startHoldProgressThenShowDone();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        bindKind(kindFrom(intent));
        restartHold();
        loadBottomAd();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideNavigationBar();
    }

    private void hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        WindowInsetsControllerCompat compat = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        compat.setAppearanceLightStatusBars(true);
    }

    private String kindFrom(@Nullable Intent intent) {
        String kind = intent == null ? null : intent.getStringExtra(EXTRA_KIND);
        if (KIND_CHARGE_IN.equals(kind) || KIND_CHARGE_OUT.equals(kind)) {
            return kind;
        }
        return KIND_UNINSTALL;
    }

    private void bindKind(String kind) {
        TextView title = findViewById(R.id.tvEventTitle);
        TextView success = findViewById(R.id.tvEventSuccess);
        TextView subtitle = findViewById(R.id.tvEventSubtitle);
        TextView body = findViewById(R.id.tvEventBody);
        TextView chipTitle = findViewById(R.id.tvOptimizeTitle);
        TextView chipBadge = findViewById(R.id.tvOptimizeBadge);
        TextView chipSub = findViewById(R.id.tvOptimizeSub);
        ImageView hero = findViewById(R.id.ivEventHero);
        ImageView statusIcon = findViewById(R.id.ivEventStatusIcon);
        ImageView chipIcon = findViewById(R.id.ivOptimizeIcon);
        int level = batteryPercent();
        if (KIND_CHARGE_IN.equals(kind)) {
            applyScreenColors(0xFFE7F8EF, 0xFF16A34A, 0xFFDDF6E8, 0xFF166534);
            title.setText(R.string.event_charge_in_title);
            success.setText(getString(R.string.event_charge_in_success, level));
            subtitle.setText(R.string.event_charge_in_sub);
            body.setText(R.string.event_charge_in_body);
            chipTitle.setText(R.string.event_charge_in_chip);
            chipBadge.setText(level + "%");
            chipSub.setText(R.string.event_charge_in_body);
            hero.setImageResource(R.drawable.ic_event_hero_charge_in);
            statusIcon.setImageResource(R.drawable.ic_event_check);
            chipIcon.setImageResource(R.drawable.ic_event_bolt);
            return;
        }
        if (KIND_CHARGE_OUT.equals(kind)) {
            applyScreenColors(0xFFFFF4E8, 0xFFEA580C, 0xFFFFEDD5, 0xFF9A3412);
            title.setText(R.string.event_charge_out_title);
            success.setText(getString(R.string.event_charge_out_success, level));
            subtitle.setText(R.string.event_charge_out_sub);
            body.setText(R.string.event_charge_out_body);
            chipTitle.setText(R.string.event_charge_out_chip);
            chipBadge.setText(level + "%");
            chipSub.setText(R.string.event_charge_out_body);
            hero.setImageResource(R.drawable.ic_event_hero_charge_out);
            statusIcon.setImageResource(R.drawable.ic_event_bolt);
            chipIcon.setImageResource(R.drawable.ic_event_bolt);
            return;
        }
        applyScreenColors(0xFFEAF1FB, 0xFF0063E5, 0xFFDBEAFE, 0xFF1D4ED8);
        title.setText(R.string.event_uninstall_title);
        success.setText(R.string.event_uninstall_success);
        subtitle.setText(R.string.event_uninstall_sub);
        body.setText(R.string.event_uninstall_body);
        chipTitle.setText(R.string.event_uninstall_chip);
        chipBadge.setText(R.string.event_fast);
        chipSub.setText(R.string.event_uninstall_body);
        hero.setImageResource(R.drawable.ic_event_hero_uninstall);
        statusIcon.setImageResource(R.drawable.ic_event_check);
        chipIcon.setImageResource(R.drawable.ic_event_check);
    }

    private void applyScreenColors(int header, int accent, int badgeBg, int badgeText) {
        View root = findViewById(R.id.eventRoot);
        View headerView = findViewById(R.id.eventHeader);
        TextView success = findViewById(R.id.tvEventSuccess);
        TextView badge = findViewById(R.id.tvOptimizeBadge);
        TextView done = findViewById(R.id.btnEventDone);
        View adHost = findViewById(R.id.rlEventAd);
        root.setBackgroundColor(header);
        headerView.setBackgroundColor(header);
        getWindow().setStatusBarColor(header);
        success.setTextColor(accent);
        done.setBackgroundTintList(ColorStateList.valueOf(accent));
        if (badge.getBackground() != null) {
            badge.getBackground().mutate().setTint(badgeBg);
        }
        badge.setTextColor(badgeText);
        adCardColor = getColor(R.color.event_card);
        adHost.setBackground(null);
        adHost.setPadding(0, 0, 0, 0);
    }

    private int batteryPercent() {
        BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        if (manager == null) {
            return 0;
        }
        int level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        if (level < 0 || level > 100) {
            return 0;
        }
        return level;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void bindClicks() {
        View settings = findViewById(R.id.ivEventSettings);
        View optimize = findViewById(R.id.llOptimizedSpeed);
        View.OnClickListener openSettings = v -> startActivity(new Intent(this, LauncherSettingsActivity.class));
        settings.setOnClickListener(openSettings);
        optimize.setOnClickListener(openSettings);
        btnEventDone = findViewById(R.id.btnEventDone);
        pbEventHold = findViewById(R.id.pbEventHold);
        btnEventDone.setOnClickListener(v -> {
            if (canExit) {
                exitScreen();
            }
        });
    }

    private void setupBackBlock() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (canExit) {
                    exitScreen();
                }
            }
        });
    }

    private void restartHold() {
        if (holdAnimator != null) {
            holdAnimator.cancel();
            holdAnimator = null;
        }
        exiting = false;
        startHoldProgressThenShowDone();
    }

    private void startHoldProgressThenShowDone() {
        canExit = false;
        pbEventHold.setVisibility(View.VISIBLE);
        btnEventDone.setVisibility(View.GONE);
        pbEventHold.setMax(1000);
        pbEventHold.setProgress(0);
        long durationMs = RemoteConfigValues.getEventScreenShowSeconds() * 1000L;
        holdAnimator = ValueAnimator.ofInt(0, 1000);
        holdAnimator.setDuration(durationMs);
        holdAnimator.setInterpolator(new LinearInterpolator());
        holdAnimator.addUpdateListener(animation -> {
            if (pbEventHold != null) {
                pbEventHold.setProgress((int) animation.getAnimatedValue());
            }
        });
        holdAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showDoneButton();
            }
        });
        holdAnimator.start();
    }

    private void showDoneButton() {
        if (isFinishing()) {
            return;
        }
        if (pbEventHold != null) {
            pbEventHold.setVisibility(View.GONE);
        }
        if (btnEventDone != null) {
            btnEventDone.setVisibility(View.VISIBLE);
        }
        canExit = true;
    }

    private void exitScreen() {
        if (!canExit || exiting) {
            return;
        }
        exiting = true;
        adRequest++;
        releaseAds();
        if (RemoteConfigValues.getEventBackAdsShow()) {
            AdPlacement.showJoinedLauncherBackAd(this, this::finish);
            return;
        }
        finish();
    }

    private void loadBottomAd() {
        RemoteConfigValues.ensureLoaded(this);
        String type = RemoteConfigValues.getEventBottomAdsType().trim();
        boolean preload = "preload".equalsIgnoreCase(type);
        boolean load = "load".equalsIgnoreCase(type);
        if (!RemoteConfigValues.getEventBottomAdsShow() || (!load && !preload)) {
            hideAdPlaceholders();
            com.qrcode.scanner.launcher.common.EventBottomAds.clear();
            return;
        }
        final int request = ++adRequest;
        clearShownAd();
        boolean useNative = com.qrcode.scanner.launcher.common.EventBottomAds.useNative();
        if (preload && showPreloadedAd(request, useNative)) {
            com.qrcode.scanner.launcher.common.EventBottomAds.prepare(this);
            return;
        }
        if (useNative) {
            loadNativeAd(request);
        } else {
            loadBannerAd(request);
        }
        if (preload) {
            Log.d("EventPrompt", "preload cache miss, loading on screen");
            com.qrcode.scanner.launcher.common.EventBottomAds.prepare(this);
        }
    }

    private boolean showPreloadedAd(int request, boolean useNative) {
        if (useNative) {
            NativeAd ready = com.qrcode.scanner.launcher.common.EventBottomAds.takeNative();
            if (ready == null) {
                return false;
            }
            if (request != adRequest || isFinishing()) {
                ready.destroy();
                return true;
            }
            Log.d("EventPrompt", "preload native shown");
            showReadyNative(ready);
            return true;
        }
        AdView ready = com.qrcode.scanner.launcher.common.EventBottomAds.takeBanner();
        if (ready == null) {
            return false;
        }
        if (request != adRequest || isFinishing()) {
            ready.destroy();
            return true;
        }
        Log.d("EventPrompt", "preload banner shown");
        showReadyBanner(ready);
        return true;
    }

    private void showReadyNative(NativeAd ready) {
        RelativeLayout container = findViewById(R.id.rlEventAd);
        ShimmerFrameLayout shimmer = findViewById(R.id.slEventShimmerNative);
        ShimmerFrameLayout bannerShimmer = findViewById(R.id.slEventShimmerBanner);
        FrameLayout nativeSlot = findViewById(R.id.flEventNative);
        View bannerSlot = findViewById(R.id.llEventBanner);
        androidx.cardview.widget.CardView adCard = findViewById(R.id.eventAdCard);
        container.setVisibility(View.VISIBLE);
        container.setBackgroundColor(adCardColor);
        if (adCard != null) {
            adCard.setCardBackgroundColor(adCardColor);
            adCard.setVisibility(View.VISIBLE);
        }
        bannerShimmer.setVisibility(View.GONE);
        bannerSlot.setVisibility(View.GONE);
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
        AdPlacement.showLargeNative(this, nativeSlot, ready);
        paintNativeCard(nativeSlot);
        loadedNativeAd = ready;
    }

    private void showReadyBanner(AdView ready) {
        RelativeLayout container = findViewById(R.id.rlEventAd);
        ShimmerFrameLayout nativeShimmer = findViewById(R.id.slEventShimmerNative);
        ShimmerFrameLayout shimmer = findViewById(R.id.slEventShimmerBanner);
        LinearLayout bannerSlot = findViewById(R.id.llEventBanner);
        androidx.cardview.widget.CardView adCard = findViewById(R.id.eventAdCard);
        container.setVisibility(View.VISIBLE);
        container.setBackground(null);
        nativeShimmer.setVisibility(View.GONE);
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
        if (adCard != null) {
            adCard.setVisibility(View.GONE);
        }
        allowBannerToDrawFully(bannerSlot);
        showFullBanner(bannerSlot, ready);
        loadedBanner = ready;
    }

    private void clearShownAd() {
        releaseAds();
    }

    /** Remove the ad view before the activity window closes, or the native web view leaks. */
    private void releaseAds() {
        FrameLayout nativeSlot = findViewById(R.id.flEventNative);
        boolean destroyedView = false;
        if (nativeSlot != null) {
            for (int i = 0; i < nativeSlot.getChildCount(); i++) {
                View child = nativeSlot.getChildAt(i);
                NativeAdView nativeAdView = child instanceof NativeAdView
                        ? (NativeAdView) child
                        : (child instanceof ViewGroup && ((ViewGroup) child).getChildCount() > 0 && ((ViewGroup) child).getChildAt(0) instanceof NativeAdView)
                        ? (NativeAdView) ((ViewGroup) child).getChildAt(0)
                        : null;
                if (nativeAdView != null) {
                    nativeAdView.destroy();
                    destroyedView = true;
                }
            }
            nativeSlot.removeAllViews();
        }
        if (!destroyedView && loadedNativeAd != null) {
            loadedNativeAd.destroy();
        }
        loadedNativeAd = null;
        if (loadedBanner != null) {
            loadedBanner.destroy();
            loadedBanner = null;
        }
        LinearLayout bannerSlot = findViewById(R.id.llEventBanner);
        if (bannerSlot != null) {
            bannerSlot.removeAllViews();
        }
    }

    private void loadNativeAd(int request) {
        RelativeLayout container = findViewById(R.id.rlEventAd);
        ShimmerFrameLayout shimmer = findViewById(R.id.slEventShimmerNative);
        ShimmerFrameLayout bannerShimmer = findViewById(R.id.slEventShimmerBanner);
        FrameLayout nativeSlot = findViewById(R.id.flEventNative);
        View bannerSlot = findViewById(R.id.llEventBanner);
        container.setVisibility(View.VISIBLE);
        container.setBackgroundColor(adCardColor);
        androidx.cardview.widget.CardView adCard = findViewById(R.id.eventAdCard);
        if (adCard != null) {
            adCard.setCardBackgroundColor(adCardColor);
            adCard.setVisibility(View.VISIBLE);
        }
        bannerShimmer.setVisibility(View.GONE);
        bannerSlot.setVisibility(View.GONE);
        shimmer.setVisibility(View.VISIBLE);
        shimmer.startShimmer();
        nativeSlot.setVisibility(View.GONE);
        String nativeId = RemoteConfigValues.getEventNativeId().trim();
        AdPlacement.loadNativeAd(this, nativeId, container, shimmer, nativeSlot, "large", nativeAd -> {
            if (request != adRequest || isFinishing()) {
                if (nativeAd != null) {
                    nativeAd.destroy();
                }
                return;
            }
            if (nativeAd != null) {
                loadedNativeAd = nativeAd;
            }
            paintNativeCard(nativeSlot);
        }, () -> {
            if (request != adRequest || isFinishing()) {
                return;
            }
            if (AdPlacement.getGoogleAdFailedShowQuiz() && QuizAds.showNative(this, container, shimmer, nativeSlot, "large")) {
                paintNativeCard(nativeSlot);
                return;
            }
            hideAdPlaceholders();
        }, true);
    }

    /** The ad layout ships with a white fill, so it blends into the sheet. Repaint that fill to the Fast Charging card. */
    private void paintNativeCard(@Nullable View slot) {
        if (slot == null) {
            return;
        }
        applyAdFill(slot);
    }

    private void applyAdFill(@Nullable View view) {
        if (view == null) {
            return;
        }
        if (view.getBackground() instanceof ColorDrawable) {
            view.setBackgroundColor(adCardColor);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyAdFill(group.getChildAt(i));
            }
        }
    }

    private void loadBannerAd(int request) {
        RelativeLayout container = findViewById(R.id.rlEventAd);
        ShimmerFrameLayout nativeShimmer = findViewById(R.id.slEventShimmerNative);
        ShimmerFrameLayout shimmer = findViewById(R.id.slEventShimmerBanner);
        LinearLayout bannerSlot = findViewById(R.id.llEventBanner);
        View nativeSlot = findViewById(R.id.flEventNative);
        container.setVisibility(View.VISIBLE);
        container.setBackground(null);
        androidx.cardview.widget.CardView adCard = findViewById(R.id.eventAdCard);
        if (adCard != null) {
            adCard.setVisibility(View.VISIBLE);
            adCard.setCardBackgroundColor(adCardColor);
        }
        nativeShimmer.setVisibility(View.GONE);
        nativeSlot.setVisibility(View.GONE);
        shimmer.setVisibility(View.VISIBLE);
        shimmer.startShimmer();
        bannerSlot.setVisibility(View.GONE);
        String bannerId = RemoteConfigValues.getEventBannerId().trim();
        container.post(() -> {
            if (request != adRequest || isFinishing()) {
                return;
            }
            AdPlacement.requestAdaptiveBannerAd(this, bannerId, eventBannerWidthDp(), adView -> {
                if (request != adRequest || isFinishing()) {
                    adView.destroy();
                    return;
                }
                shimmer.stopShimmer();
                shimmer.setVisibility(View.GONE);
                nativeShimmer.setVisibility(View.GONE);
                if (adCard != null) {
                    adCard.setVisibility(View.GONE);
                }
                allowBannerToDrawFully(bannerSlot);
                showFullBanner(bannerSlot, adView);
                loadedBanner = adView;
            }, () -> {
                if (request != adRequest || isFinishing()) {
                    return;
                }
                if (AdPlacement.getGoogleAdFailedShowQuiz() && QuizAds.showBanner(this, container, shimmer, bannerSlot)) {
                    return;
                }
                hideAdPlaceholders();
            });
        });
    }

    private void showFullBanner(LinearLayout bannerSlot, com.google.android.gms.ads.AdView adView) {
        adView.setClipChildren(false);
        adView.setClipToPadding(false);
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (adView.getAdSize() != null) {
            width = adView.getAdSize().getWidthInPixels(this);
            height = adView.getAdSize().getHeightInPixels(this) + dp(8);
        }
        FrameLayout holder = new FrameLayout(this);
        holder.setClipChildren(false);
        holder.setClipToPadding(false);
        holder.setPadding(0, dp(10), 0, dp(4));
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(width, height);
        adParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
        holder.addView(adView, adParams);
        bannerSlot.removeAllViews();
        bannerSlot.addView(holder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bannerSlot.setVisibility(View.VISIBLE);
        bannerSlot.setBackground(null);
        findViewById(R.id.rlEventAd).setBackground(null);
        adView.post(() -> unclipAdChildren(adView));
    }

    private void unclipAdChildren(ViewGroup group) {
        group.setClipChildren(false);
        group.setClipToPadding(false);
        group.setClipToOutline(false);
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setClipToOutline(false);
            if (child instanceof ViewGroup) {
                unclipAdChildren((ViewGroup) child);
            }
        }
    }

    private void allowBannerToDrawFully(@Nullable View start) {
        View view = start;
        while (view != null) {
            view.setClipToOutline(false);
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                group.setClipChildren(false);
                group.setClipToPadding(false);
            }
            if (view.getId() == R.id.eventRoot) {
                break;
            }
            view = view.getParent() instanceof View ? (View) view.getParent() : null;
        }
    }

    private int eventBannerWidthDp() {
        View container = findViewById(R.id.rlEventAd);
        float density = getResources().getDisplayMetrics().density;
        int widthPx = container != null && container.getWidth() > 0
                ? container.getWidth()
                : getResources().getDisplayMetrics().widthPixels - dp(32);
        return Math.max(1, (int) Math.floor(widthPx / density));
    }

    private void hideAdPlaceholders() {
        View container = findViewById(R.id.rlEventAd);
        if (container != null) {
            container.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        adRequest++;
        if (holdAnimator != null) {
            holdAnimator.cancel();
            holdAnimator = null;
        }
        releaseAds();
        super.onDestroy();
    }
}
