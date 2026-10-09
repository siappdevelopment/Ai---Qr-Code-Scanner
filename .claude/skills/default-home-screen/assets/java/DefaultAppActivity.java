package com.calendar.eventplanner.tasktracker.ADS.launcher.activity;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.calendar.eventplanner.tasktracker.ADS.AppAnalyticsEvents;
import com.calendar.eventplanner.tasktracker.ADS.PermissionFirebaseEvents;
import com.calendar.eventplanner.tasktracker.ADS.advertisement.ADSAppManage;
import com.calendar.eventplanner.tasktracker.ADS.advertisement.ADSBannerSmall;
import com.calendar.eventplanner.tasktracker.ADS.advertisement.ADSMainClass;
import com.calendar.eventplanner.tasktracker.ADS.advertisement.ADSNativeDisplay;
import com.calendar.eventplanner.tasktracker.R;
import com.calendar.eventplanner.tasktracker.ADS.launcher.common.AdPlacement;
import com.calendar.eventplanner.tasktracker.ADS.launcher.common.Utils;
import com.calendar.eventplanner.tasktracker.ADS.launcher.utils.DefaultPermissionButtonAd;
import com.calendar.eventplanner.tasktracker.ADS.launcher.utils.LocaleAwareAppCompatActivity;
import com.calendar.eventplanner.tasktracker.ADS.navigation.StartupFlowManager;
import com.calendar.eventplanner.tasktracker.ADS.navigation.StartupFlowStep;

public class DefaultAppActivity extends LocaleAwareAppCompatActivity {
    private AppCompatTextView tvTitle, tvDescription, tvSetAsDefault;
    private ShimmerFrameLayout btnSetDefault, slBannerShimmer, slNativeShimmer;
    TextView tv_privacy_policy;
    private boolean navigatedAway;
    private boolean waitingForDefaultResult;
    private boolean homeRoleRequestInFlight;
    private boolean isShowingButtonAd;

    private final ActivityResultLauncher<Intent> homeRoleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        homeRoleRequestInFlight = false;
        waitingForDefaultResult = false;
        handleDefaultDialogResult(result.getResultCode());
    });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ADSMainClass.getDefaultLauncherShow()) {
            if (StartupFlowManager.isStartupFlowActive(this)) {
                StartupFlowManager.completeStep(this, StartupFlowStep.DEFAULT_HOME);
            } else {
                startActivity(new Intent(this, StartupFlowManager.homeDestinationClass())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                overridePendingTransition(0, 0);
                finish();
            }
            return;
        }
        setContentView(R.layout.activity_default_app);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        initViews();
        manageTextViews();
        setupBackPress();
        DefaultPermissionButtonAd.preload(this);
        if (StartupFlowManager.isStartupFlowActive(this) && Utils.isDefaultHomeApp(this)) {
            findViewById(android.R.id.content).post(this::goNextAfterDefault);
        }
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                overridePendingTransition(0, 0);
                StartupFlowManager.exitAppOnBackPress(DefaultAppActivity.this);
            }
        });
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        btnSetDefault = findViewById(R.id.btnSetDefault);
        tvSetAsDefault = findViewById(R.id.tvSetAsDefault);
        tv_privacy_policy = findViewById(R.id.tv_privacy_policy);

        clickEvents();
        loadAds();
    }

    private void loadAds() {
        if (ADSMainClass.getDefaultPermissionBottomAdsShow()) {
            // This screen only: its Quiz Banner / Quiz Native keep the big shimmer and show the big Quiz ad.
            ((ShimmerFrameLayout) findViewById(R.id.shimmer_container_banner))
                    .setTag(com.calendar.eventplanner.tasktracker.ADS.advertisement.AdPlacement.KEEP_BIG_SHIMMER_TAG);
            if (ADSMainClass.getDefaultPermissionAdsType().equals("native")) {
                ADSNativeDisplay.loadAdmobNativeAdBig(
                        ADSMainClass.getStringValue(ADSMainClass.DEFAULT_PERMISSION_SCREEN_NATIVE),
                        findViewById(R.id.flNativeSmallPlaceholder),
                        findViewById(R.id.shimmer_container_banner),
                        "big",
                        this
                );
            } else {
                com.calendar.eventplanner.tasktracker.ADS.advertisement.ADSBannerAdaptive.loadAdMobBanner(
                        ADSMainClass.getStringValue(ADSMainClass.DEFAULT_PERMISSION_SCREEN_BANNER),
                        findViewById(R.id.flBannerSmallPlaceholder),
                        findViewById(R.id.shimmer_container_banner),
                        this, "big"
                );
            }
        } else {
            findViewById(R.id.shimmer_container_banner).setVisibility(View.GONE);
            findViewById(R.id.flNativeSmallPlaceholder).setVisibility(View.GONE);
            findViewById(R.id.flBannerSmallPlaceholder).setVisibility(View.GONE);
        }
    }

    private void manageTextViews() {
        String terms = getString(R.string.terms_of_service);
        String privacy = getString(R.string.privacy_policy);
        String text = getString(R.string.we_don_t_collect_personal, terms, privacy);

        SpannableString spannable = new SpannableString(text);

        applyPolicyLinkSpan(spannable, text, terms);
        applyPolicyLinkSpan(spannable, text, privacy);

        tv_privacy_policy.setText(spannable);
        tv_privacy_policy.setMovementMethod(LinkMovementMethod.getInstance());
        tv_privacy_policy.setHighlightColor(Color.TRANSPARENT);
    }

    private void applyPolicyLinkSpan(SpannableString spannable, String fullText, String label) {
        if (label == null || label.isEmpty()) {
            return;
        }

        int start = fullText.indexOf(label);
        if (start < 0) {
            return;
        }

        int end = start + label.length();

        ClickableSpan clickable = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openPrivacyPolicyUrl();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(DefaultAppActivity.this, R.color.calendar_primary));
                ds.setUnderlineText(true);
            }
        };

        spannable.setSpan(clickable, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void openPrivacyPolicyUrl() {
        if (!ADSMainClass.getPrivacyPolicy().equals("")) {
            ADSAppManage.isAppOpenBlocked = true;
            startActivity(
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(ADSMainClass.getPrivacyPolicy())
                    )
            );
        } else {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
        }
    }

    private void clickEvents() {
//        if (!AdPlacement.getDefaultAdShow()) {
////            rlAdView.setVisibility(View.GONE);
//        } else {
////            rlAdView.setVisibility(View.VISIBLE);
//        }

        tvSetAsDefault.setOnClickListener(view -> {
            AppAnalyticsEvents.track(this, AppAnalyticsEvents.SET_DEFAULT_HOME_CLICK);
            DefaultPermissionButtonAd.resetShownFlag();
            DefaultPermissionButtonAd.preload(this);
            if (Utils.isDefaultHomeApp(this)) {
                showDefaultPermissionButtonAdThen(this::goNextAfterDefault);
            } else {
                waitingForDefaultResult = true;
                Utils.setCompletingDefaultAppSetup(this, true);
                // Same RoleManager.ROLE_HOME request dialog LauncherHomePageFragment shows.
                requestDefaultHomeApp();
            }
        });
    }

    /**
     * Allow → finish; LauncherHome (HOME) shows button ad first, then next screen.
     * Cancel → show button ad here, then continue after dismiss.
     */
    private void handleDefaultDialogResult(int resultCode) {
        if (Utils.isDefaultHomeApp(this)) {
            // Role accepted — system opens HOME. LauncherHome shows ad → next screen.
            Utils.applyRecentsVisibility(this);
            if (!isFinishing()) {
                finish();
            }
            return;
        }

        boolean isCancel = resultCode == Activity.RESULT_CANCELED || !Utils.isDefaultHomeApp(this);
        if (isCancel) {
            AppAnalyticsEvents.track(this, AppAnalyticsEvents.DEFAULT_HOME_SET_CANCEL);
            PermissionFirebaseEvents.trackDefaultHomeAppCancel(this);
            showDefaultPermissionButtonAdThen(this::continueAfterCancelAd);
        } else {
            continueAfterCancelAd();
        }
    }

    private void continueAfterCancelAd() {
        if (Utils.isCompletingDefaultAppSetup(this)) {
            goNextAfterDefault();
        } else if (!isFinishing()) {
            finish();
        }
    }

    private void showDefaultPermissionButtonAdThen(Runnable onComplete) {
        if (navigatedAway || isFinishing()) {
            return;
        }
        if (isShowingButtonAd) {
            return;
        }
        if (!DefaultPermissionButtonAd.shouldShow()) {
            onComplete.run();
            return;
        }

        isShowingButtonAd = true;
        DefaultPermissionButtonAd.show(this, b -> {
            isShowingButtonAd = false;
            if (!isFinishing() && !navigatedAway) {
                onComplete.run();
            }
        });
    }

    private void goNextAfterDefault() {
        if (navigatedAway || isFinishing()) {
            return;
        }
        navigatedAway = true;
        waitingForDefaultResult = false;
        Utils.setCompletingDefaultAppSetup(this, false);
        if (Utils.isDefaultHomeApp(this)) {
            Utils.trackScreen(this, AppAnalyticsEvents.DEFAULT_HOME_SET_SUCCESS);
            PermissionFirebaseEvents.trackDefaultHomeAppSet(this);
            Utils.applyRecentsVisibility(this);
        }
        StartupFlowManager.completeStep(DefaultAppActivity.this, StartupFlowStep.DEFAULT_HOME);
    }

    private void requestDefaultHomeApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        showDefaultPermissionButtonAdThen(this::goNextAfterDefault);
                        return;
                    }
                    Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);
                    // RoleManager returns a system-owned intent. On Android 11/12,
                    // resolveActivity() can return null because of package visibility
                    // even though this intent is valid. Falling back in that case opens
                    // the generic "Select a Home app" dialog.
                    homeRoleRequestInFlight = true;
                    homeRoleLauncher.launch(intent);
                    return;
                }
            }
            openHomeAppChooser();
        } catch (Exception e) {
            homeRoleRequestInFlight = false;
            e.printStackTrace();
            openHomeSettingsFallback();
        }
    }

    private void openHomeAppChooser() {
        try {
            Intent selector = new Intent(Intent.ACTION_MAIN);
            selector.addCategory(Intent.CATEGORY_HOME);
            if (selector.resolveActivity(getPackageManager()) != null) {
                waitingForDefaultResult = true;
                startActivity(selector);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        waitingForDefaultResult = false;
        // No chooser — treat as cancel path
        showDefaultPermissionButtonAdThen(this::continueAfterCancelAd);
    }

    private void openHomeSettingsFallback() {
        try {
            waitingForDefaultResult = true;
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception e) {
            e.printStackTrace();
            waitingForDefaultResult = false;
            showDefaultPermissionButtonAdThen(this::continueAfterCancelAd);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.applyRecentsVisibility(this);

        DefaultPermissionButtonAd.preload(this);

        if (tvTitle != null) {
                    tvSetAsDefault.setText(getString(R.string.set_as_default));
        }

        // Home chooser / settings path (no ActivityResult)
        if (waitingForDefaultResult && !homeRoleRequestInFlight && !isShowingButtonAd) {
            waitingForDefaultResult = false;
            if (Utils.isDefaultHomeApp(this)) {
                if (!isFinishing()) {
                    finish();
                }
            } else {
                showDefaultPermissionButtonAdThen(this::continueAfterCancelAd);
            }
        }
    }
}
