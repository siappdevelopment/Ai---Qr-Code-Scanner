package com.qrcode.scanner.launcher.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.ProcessAppOpen;
import com.qrcode.scanner.launcher.common.WelcomeBackConfig;
import com.qrcode.scanner.spapp.R;

/**
 * "Welcome back" screen with a Continue button (counts down first) and a medium native ad at the bottom. Shown on return
 * from the background instead of the App Open ad (see {@link WelcomeBackConfig}).
 */
public class WelcomeBackActivity extends AppCompatActivity {
    private static final String TAG = "WelcomeBack";

    private NativeAd nativeAd;
    private CountDownTimer timer;
    private boolean destroyed;
    private InterstitialAd interAd;
    private AppCompatTextView button;

    /** Opens the Welcome screen above the current screen (needs consent / the ad SDK to be allowed to request ads). */
    public static void show(@NonNull Activity activity) {
        if (!AdPlacement.canRequestAds(activity)) {
            Log.d(TAG, "not shown: ads cannot be requested (consent / SDK / country lists)");
            return;
        }
        activity.startActivity(new Intent(activity, WelcomeBackActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_back);
        applySystemBars();
        button = findViewById(R.id.tvWelcomeContinue);
        button.setOnClickListener(v -> {
            if (button.isEnabled()) {
                onContinue();
            }
        });
        // Back only works once Continue is active (before that it is swallowed), then it does the same as Continue: the
        // interstitial when it is ready, then close.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (button.isEnabled()) {
                    onContinue();
                }
            }
        });
        startCountdown();
        loadAd();
        preloadInter();
    }

    /** Status and navigation bar in the page colour: white page = white bars (dark icons), dark page = dark bars (light icons). */
    private void applySystemBars() {
        int color = androidx.core.content.ContextCompat.getColor(this, R.color.surface_primary);
        boolean night = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        android.view.Window window = getWindow();
        window.setStatusBarColor(color);
        window.setNavigationBarColor(color);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.setNavigationBarDividerColor(color);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        androidx.core.view.WindowInsetsControllerCompat controller =
                androidx.core.view.WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(!night);
        controller.setAppearanceLightNavigationBars(!night);
    }

    // ---------------------------------------------------------------- Continue + its interstitial

    /** Interstitial for the Continue button: loaded while the countdown runs (only when enabled). */
    private void preloadInter() {
        final Context app = getApplicationContext();
        if (!WelcomeBackConfig.interEnabled(app) || !AdPlacement.canRequestAds(app) || !AdPlacement.isNetworkAvailable(app)) {
            Log.d(TAG, "Continue inter not requested: enabled=" + WelcomeBackConfig.interEnabled(app)
                    + " (welcome_back_inter_show + welcome_back_inter_id) canRequestAds=" + AdPlacement.canRequestAds(app)
                    + " online=" + AdPlacement.isNetworkAvailable(app));
            return;
        }
        Log.d(TAG, "Continue inter requested: " + WelcomeBackConfig.interId(app));
        InterstitialAd.load(app, WelcomeBackConfig.interId(app), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                ad.setOnPaidEventListener(value -> AdPlacement.logAdRevenue(app, value));
                Log.d(TAG, "Continue inter loaded");
                if (!destroyed) {
                    interAd = ad;
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                Log.w(TAG, "Continue inter failed to load: code=" + error.getCode() + " " + error.getMessage()
                        + " (is welcome_back_inter_id an INTERSTITIAL ad unit?)");
                interAd = null; // nothing to show, Continue just closes the screen
            }
        });
    }

    /** Continue and Back: show the interstitial if it is ready, otherwise just close. No waiting dialog. */
    private void onContinue() {
        InterstitialAd ad = interAd;
        if (ad == null || destroyed) {
            Log.d(TAG, "Continue / Back: no inter ready, closing");
            closeWelcome();
            return;
        }
        interAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                closeWelcome();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                closeWelcome();
            }
        });
        // The foreground App Open ad must not stack on this interstitial.
        ProcessAppOpen.blockFor(8_000L);
        try {
            ad.show(this);
        } catch (Exception e) {
            closeWelcome();
        }
    }

    private void closeWelcome() {
        finish();
        overridePendingTransition(0, 0);
    }

    /** "Continue in 2" -> "Continue in 1" -> "Continue" (active). */
    private void startCountdown() {
        int sec = WelcomeBackConfig.continueDelaySec(this);
        if (sec <= 0) {
            enableContinue();
            return;
        }
        button.setEnabled(false);
        button.setAlpha(0.5f);
        button.setText(getString(R.string.welcome_back_continue_in, sec));
        timer = new CountDownTimer(sec * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int left = (int) ((millisUntilFinished + 999) / 1000);
                button.setText(getString(R.string.welcome_back_continue_in, left));
            }

            @Override
            public void onFinish() {
                enableContinue();
            }
        }.start();
    }

    private void enableContinue() {
        button.setEnabled(true);
        button.setAlpha(1f);
        button.setText(R.string.welcome_back_continue);
    }

    // ---------------------------------------------------------------- native ad

    /**
     * Medium native (Google only). Skipped (area hidden) unless the Welcome screen is enabled, the id is given and ads may
     * be requested. A failed load hides the whole ad area (no empty gap).
     */
    private void loadAd() {
        RelativeLayout area = findViewById(R.id.rlWelcomeAd);
        ShimmerFrameLayout shimmer = findViewById(R.id.shimmerWelcomeAd);
        FrameLayout host = findViewById(R.id.flWelcomeAd);
        String id = WelcomeBackConfig.nativeId(this);
        if (!WelcomeBackConfig.isEnabled(this) || id.isEmpty() || !AdPlacement.canRequestAds(this) || !AdPlacement.isNetworkAvailable(this)) {
            area.setVisibility(View.GONE);
            return;
        }
        area.setVisibility(View.VISIBLE);
        shimmer.setVisibility(View.VISIBLE);
        shimmer.startShimmer();
        host.setVisibility(View.GONE);
        // googleOnly = true: a Google failure never turns into a Quiz ad here. Landing on the ad's page and coming back
        // returns to this (skip-listed) screen, so no second Welcome / App Open can start.
        AdPlacement.loadNativeAd(this, id, area, shimmer, host, "medium", ad -> nativeAd = ad, () -> {
            if (!destroyed) {
                shimmer.stopShimmer();
                area.setVisibility(View.GONE);
            }
        }, true);
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (timer != null) {
            timer.cancel();
        }
        interAd = null;
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
        super.onDestroy();
    }
}
