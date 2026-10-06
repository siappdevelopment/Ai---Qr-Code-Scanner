package com.qrcode.scanner.launcher.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.IntroNavigation;

public class Intro1Activity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_1);
        hideNavigationAndAds();
        LinearLayout llIndicator = findViewById(R.id.llIndicator);
        AppCompatTextView btnNext = findViewById(R.id.btnNext);
        IntroNavigation.setupIntroButtonIndicators(this, llIndicator, 1);
        btnNext.setOnClickListener(view -> IntroNavigation.goToNextIntroButtonScreen(this, 1));
    }

    private void hideNavigationAndAds() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        AdPlacement.showSlot(this, AdPlacement.getIntroAdShow(), AdPlacement.getIntroAdType(), AdPlacement.getIntroBannerId(), AdPlacement.getIntroNativeId(), "intro", findViewById(R.id.rlAdView), findViewById(R.id.rlBannerAdView), findViewById(R.id.slBannerShimmer), findViewById(R.id.llBannerAd), findViewById(R.id.rlNativeAdView), findViewById(R.id.slNativeShimmer), findViewById(R.id.flNativeAd), false, AdPlacement.onboardingNativeColor(this));
    }
}
