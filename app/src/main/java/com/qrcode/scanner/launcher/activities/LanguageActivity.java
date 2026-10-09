package com.qrcode.scanner.launcher.activities;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.adapters.LanguageAdapter;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.ScreenFlowNavigation;
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;
import com.qrcode.scanner.ui.navigation.ThemeNavigation;

import java.util.ArrayList;

public class LanguageActivity extends AppCompatActivity {
    public static final String EXTRA_FROM_APP_SETTINGS = "extra_from_app_settings";

    private AppCompatTextView tvTitle;
    private LanguageAdapter languageAdapter;
    private int selectedIndex;
    private boolean languageFlowInProgress;
    private final ArrayList<Integer> arrayListIcon = new ArrayList<>();
    private final ArrayList<String> arrayListName = new ArrayList<>();
    private final ArrayList<String> arrayListSubName = new ArrayList<>();
    private final ArrayList<String> arrayListCode = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyNavigationBarColor();
        setContentView(R.layout.activity_language);
        hideNavigationBar();
        findIDs();
        AdPlacement.preloadOnboardingInterstitialAd(this, false);
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
    }

    private void applyNavigationBarColor() {
        boolean isNight = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int color = ContextCompat.getColor(this, isNight ? R.color.surface_primary_dark : R.color.surface_primary_light);
        getWindow().setNavigationBarColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightNavigationBars(!isNight);
    }

    private void findIDs() {
        tvTitle = findViewById(R.id.tvTitle);
        AppCompatImageView ivBack = findViewById(R.id.ivBack);
        AppCompatImageView ivDone = findViewById(R.id.ivDone);
        if (isOpenedFromSettings()) {
            ivBack.setVisibility(View.VISIBLE);
            ivBack.setOnClickListener(view -> {
                setResult(RESULT_CANCELED);
                finish();
            });
        }
        RecyclerView rvLanguage = findViewById(R.id.rvLanguage);
        RelativeLayout rlAdView = findViewById(R.id.rlAdView);
        RelativeLayout rlBannerAdView = findViewById(R.id.rlBannerAdView);
        ShimmerFrameLayout slBannerShimmer = findViewById(R.id.slBannerShimmer);
        LinearLayout llBannerAd = findViewById(R.id.llBannerAd);
        RelativeLayout rlNativeAdView = findViewById(R.id.rlNativeAdView);
        ShimmerFrameLayout slNativeShimmer = findViewById(R.id.slNativeShimmer);
        FrameLayout flNativeAd = findViewById(R.id.flNativeAd);

        showAd(rlAdView, rlBannerAdView, slBannerShimmer, llBannerAd, rlNativeAdView, slNativeShimmer, flNativeAd);
        setupLanguages(rvLanguage);
        ivDone.setOnClickListener(view -> applySelectedLanguageAndContinue());
    }

    private void showAd(RelativeLayout rlAdView, RelativeLayout rlBannerAdView, ShimmerFrameLayout slBannerShimmer, LinearLayout llBannerAd, RelativeLayout rlNativeAdView, ShimmerFrameLayout slNativeShimmer, FrameLayout flNativeAd) {
        if (!AdPlacement.isNetworkAvailable(this) || !AdPlacement.getLanguageAdShow()) {
            rlAdView.setVisibility(View.GONE);
            return;
        }
        rlAdView.setVisibility(View.VISIBLE);
        // Quiz priority always shows the big Quiz native here, even when the Language ad type is banner.
        if ("banner".equalsIgnoreCase(AdPlacement.getLanguageAdType()) && !AdPlacement.shouldUseQuizPriority()) {
            rlBannerAdView.setVisibility(View.VISIBLE);
            rlNativeAdView.setVisibility(View.GONE);
            // Google banner failed and Quiz is the fallback: show the big Quiz native, not the small Quiz banner.
            AdPlacement.loadAdaptiveBannerAd(this, AdPlacement.getLanguageBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd, false, true, () -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                rlBannerAdView.setVisibility(View.GONE);
                rlNativeAdView.setVisibility(View.VISIBLE);
                if (!com.qrcode.scanner.launcher.common.QuizAds.showNative(this, rlNativeAdView, slNativeShimmer, flNativeAd, "large")) {
                    rlAdView.setVisibility(View.GONE);
                }
            });
        } else {
            rlBannerAdView.setVisibility(View.GONE);
            rlNativeAdView.setVisibility(View.VISIBLE);
            AdPlacement.loadNativeAd(this, AdPlacement.getLanguageNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "large", null, null, false, AdPlacement.onboardingNativeColor(this));
        }
    }

    private void setupLanguages(RecyclerView rvLanguage) {
        arrayListIcon.clear();
        arrayListName.clear();
        arrayListSubName.clear();
        arrayListCode.clear();

        arrayListIcon.add(R.drawable.ic_english);
        arrayListIcon.add(R.drawable.ic_hindi);
        arrayListIcon.add(R.drawable.ic_russian);
        arrayListIcon.add(R.drawable.ic_italian);
        arrayListIcon.add(R.drawable.ic_french);
        arrayListIcon.add(R.drawable.ic_spanish);
        arrayListIcon.add(R.drawable.ic_japanese);
        arrayListIcon.add(R.drawable.ic_korean);
        arrayListIcon.add(R.drawable.ic_german);
        arrayListIcon.add(R.drawable.ic_chinese);
        arrayListIcon.add(R.drawable.ic_thai);
        arrayListIcon.add(R.drawable.ic_greek);
        arrayListIcon.add(R.drawable.ic_portuguese_portugal);
        arrayListIcon.add(R.drawable.ic_portuguese_brazil);
        arrayListIcon.add(R.drawable.ic_dutch);
        arrayListIcon.add(R.drawable.ic_filipino);
        arrayListIcon.add(R.drawable.ic_turkish);
        arrayListIcon.add(R.drawable.ic_indonesian);
        arrayListIcon.add(R.drawable.ic_afrikaans);

        arrayListName.add("English");
        arrayListName.add("Hindi");
        arrayListName.add("Russian");
        arrayListName.add("Italian");
        arrayListName.add("French");
        arrayListName.add("Spanish");
        arrayListName.add("Japanese");
        arrayListName.add("Korean");
        arrayListName.add("German");
        arrayListName.add("Chinese");
        arrayListName.add("Thai");
        arrayListName.add("Greek");
        arrayListName.add("Portuguese (Portugal)");
        arrayListName.add("Portuguese (Brazil)");
        arrayListName.add("Dutch");
        arrayListName.add("Filipino");
        arrayListName.add("Turkish");
        arrayListName.add("Indonesian");
        arrayListName.add("Afrikaans");

        arrayListSubName.add("English");
        arrayListSubName.add("हिंदी");
        arrayListSubName.add("Русский");
        arrayListSubName.add("Italiano");
        arrayListSubName.add("Français");
        arrayListSubName.add("Española");
        arrayListSubName.add("日本語");
        arrayListSubName.add("한국인");
        arrayListSubName.add("Deutsch");
        arrayListSubName.add("中国人");
        arrayListSubName.add("แบบไทย");
        arrayListSubName.add("ελληνικά");
        arrayListSubName.add("Português");
        arrayListSubName.add("Português");
        arrayListSubName.add("Nederlands");
        arrayListSubName.add("Filipino");
        arrayListSubName.add("Türkçe");
        arrayListSubName.add("Bahasa Indonesia");
        arrayListSubName.add("Afrikaans");

        arrayListCode.add("en");
        arrayListCode.add("hi");
        arrayListCode.add("ru");
        arrayListCode.add("it");
        arrayListCode.add("fr");
        arrayListCode.add("es");
        arrayListCode.add("ja");
        arrayListCode.add("ko");
        arrayListCode.add("de");
        arrayListCode.add("zh");
        arrayListCode.add("th");
        arrayListCode.add("el");
        arrayListCode.add("pt");
        arrayListCode.add("pt-BR");
        arrayListCode.add("nl");
        arrayListCode.add("fil");
        arrayListCode.add("tr");
        arrayListCode.add("id");
        arrayListCode.add("af");

        selectedIndex = getAppliedLanguageIndex();
//        updateTitleForSelection(selectedIndex);

        languageAdapter = new LanguageAdapter(this, arrayListIcon, arrayListName, arrayListSubName, arrayListCode);
        rvLanguage.setLayoutManager(new LinearLayoutManager(this));
        rvLanguage.setAdapter(languageAdapter);
        languageAdapter.setSelectedPosition(selectedIndex);
        languageAdapter.setOnLanguageClickListener(position -> {
            selectedIndex = position;
//            updateTitleForSelection(position);
            languageAdapter.setSelectedPosition(position);
        });
    }

    private int getAppliedLanguageIndex() {
        String appliedCode = AppUtils.getLanguage(this);
        for (int i = 0; i < arrayListCode.size(); i++) {
            if (arrayListCode.get(i).equals(appliedCode)) {
                return i;
            }
        }
        return 0;
    }

//    private void updateTitleForSelection(int index) {
//        tvTitle.setText(AppUtils.getStringForLanguage(this, arrayListCode.get(index), R.string.choose_your_language));
//    }

    private void applySelectedLanguageAndContinue() {
        if (languageFlowInProgress) {
            return;
        }
        languageFlowInProgress = true;
        String selectedCode = arrayListCode.get(selectedIndex);
        String appliedCode = AppUtils.getLanguage(this);
        boolean languageChanged = !selectedCode.equals(appliedCode);
        // Apply before the ad so the ad can never block the change; the host screen recreates on Settings.
        if (languageChanged) {
            if (isOpenedFromSettings()) {
                ThemeNavigation.INSTANCE.markReopenSettings();
            }
            AppUtils.applyLanguage(this, selectedCode);
        }
        if (AdPlacement.getLanguageInterstitialAdShow()) {
            AdPlacement.loadLanguageInterstitialAd(this, () -> moveToNextScreen(languageChanged));
        } else {
            moveToNextScreen(languageChanged);
        }
    }

    private void moveToNextScreen(boolean languageChanged) {
        if (isOpenedFromSettings()) {
            setResult(languageChanged ? RESULT_OK : RESULT_CANCELED);
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        ScreenFlowNavigation.continueAfter(this, ScreenFlowConfig.SCREEN_LANGUAGE);
    }

    private boolean isOpenedFromSettings() {
        if (getIntent().getBooleanExtra(EXTRA_FROM_APP_SETTINGS, false)) {
            return true;
        }
        return AppUtils.getLanguageFlowCompleted(this) && !getIntent().getBooleanExtra(ScreenFlowNavigation.EXTRA_LANGUAGE_FLOW_STARTING, false);
    }
}
