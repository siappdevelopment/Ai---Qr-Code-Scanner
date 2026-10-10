package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.activities.Intro1Activity;
import com.qrcode.scanner.launcher.activities.Intro2Activity;
import com.qrcode.scanner.launcher.activities.Intro3Activity;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;

public class IntroNavigation {
    private static final int MAX_INTRO_BUTTON_SCREENS = 3;

    public static int getEffectiveIntroButtonScreenCount() {
        int count = ScreenFlowConfig.getIntroScreenCount();
        if (count <= 0) {
            return 0;
        }
        return Math.min(count, MAX_INTRO_BUTTON_SCREENS);
    }

    public static void setupIntroButtonIndicators(Context context, LinearLayout llIndicator, int activeScreen) {
        int pageCount = getEffectiveIntroButtonScreenCount();
        llIndicator.removeAllViews();
        if (pageCount <= 0) {
            llIndicator.setVisibility(View.GONE);
            return;
        }
        llIndicator.setVisibility(View.VISIBLE);
        int activeWidth = AppUtils.dpToPx(context, 18);
        int inactiveWidth = AppUtils.dpToPx(context, 6);
        int dotHeight = AppUtils.dpToPx(context, 6);
        int margin = AppUtils.dpToPx(context, 2);
        for (int i = 1; i <= pageCount; i++) {
            AppCompatImageView dot = new AppCompatImageView(context);
            boolean isActive = i == activeScreen;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(isActive ? activeWidth : inactiveWidth, dotHeight);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setAdjustViewBounds(true);
            dot.setImageResource(isActive ? R.drawable.custom_radius_100 : R.drawable.custom_circle);
            dot.setColorFilter(ContextCompat.getColor(context, isActive ? R.color.primary : R.color.text_secondary_dark));
            llIndicator.addView(dot);
        }
    }

    public static void openIntroButtonFlow(Activity activity) {
        if (getEffectiveIntroButtonScreenCount() == 0) {
            completeIntroAndOpenDefaultApp(activity);
            return;
        }
        activity.startActivity(new Intent(activity, Intro1Activity.class));
        activity.finish();
    }

    /**
     * Onboarding Back: no going back to the previous screen. It does exactly what the Next button does.
     */
    public static void bindBackAsNext(androidx.appcompat.app.AppCompatActivity activity, int currentScreen) {
        activity.getOnBackPressedDispatcher().addCallback(activity, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (activity.isFinishing()) {
                    return;
                }
                goToNextIntroButtonScreen(activity, currentScreen);
            }
        });
    }

    public static void goToNextIntroButtonScreen(Activity activity, int currentScreen) {
        if (currentScreen >= getEffectiveIntroButtonScreenCount()) {
            moveToNextScreen(activity);
            return;
        }
        Class<?> nextClass;
        switch (currentScreen + 1) {
            case 2:
                nextClass = Intro2Activity.class;
                break;
            case 3:
                nextClass = Intro3Activity.class;
                break;
            default:
                moveToNextScreen(activity);
                return;
        }
        activity.startActivity(new Intent(activity, nextClass));
        activity.finish();
    }

    private static void moveToNextScreen(Activity activity) {
        AdPlacement.loadIntroInterstitialAd(activity, () -> ScreenFlowNavigation.continueAfter(activity, ScreenFlowConfig.SCREEN_INTRO));
    }

    private static void completeIntroAndOpenDefaultApp(Activity activity) {
        ScreenFlowNavigation.continueAfter(activity, ScreenFlowConfig.SCREEN_INTRO);
    }
}
