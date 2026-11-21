package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.qrcode.scanner.spapp.R;
import com.qrcode.scanner.launcher.activities.IntroActivity;
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;

public class IntroNavigation {
    private static final int MAX_INTRO_BUTTON_SCREENS = 3;

    /** Number of intro pages (Intro_Screen_Count, 0 = no intro, at most 3). */
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

    /** Opens the single Intro screen (a ViewPager2 with all intro pages). With no intro pages the intro is skipped. */
    public static void openIntroButtonFlow(Activity activity) {
        if (getEffectiveIntroButtonScreenCount() == 0) {
            ScreenFlowNavigation.continueAfter(activity, ScreenFlowConfig.SCREEN_INTRO);
            return;
        }
        activity.startActivity(new Intent(activity, IntroActivity.class));
        activity.finish();
    }

    /**
     * The user finished the intro: the repeat count (Intro_Show_Every_Launch) starts again from here, the unused
     * full-screen natives are released, the intro interstitial (if on) runs, and the flow moves on.
     */
    public static void finishIntro(Activity activity) {
        IntroConfig.markShown(activity);
        IntroFullNativeAds.release();
        AdPlacement.loadIntroInterstitialAd(activity, () -> ScreenFlowNavigation.continueAfter(activity, ScreenFlowConfig.SCREEN_INTRO));
    }
}
