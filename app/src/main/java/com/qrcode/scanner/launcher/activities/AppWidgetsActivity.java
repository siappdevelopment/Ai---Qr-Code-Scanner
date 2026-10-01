package com.qrcode.scanner.launcher.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.WidgetType;
import com.qrcode.scanner.launcher.helpers.WidgetPinHelper;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

public class AppWidgetsActivity extends AppCompatActivity {
    private AppCompatImageView ivBack;
    private LinearLayout llWidgetAppIcon, llWidgetCreateQRA, llWidgetCreateQRB, llWidgetCreateQRC, llWidgetScanQRA, llWidgetScanQRB;

    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer, slNativeShimmer;
    private LinearLayout llBannerAd;
    private FrameLayout flNativeAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_widgets);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        findIDs();
    }

    private void findIDs() {
        ivBack = findViewById(R.id.ivBack);
        llWidgetAppIcon = findViewById(R.id.llWidgetAppIcon);
        llWidgetCreateQRA = findViewById(R.id.llWidgetCreateQRA);
        llWidgetCreateQRB = findViewById(R.id.llWidgetCreateQRB);
        llWidgetCreateQRC = findViewById(R.id.llWidgetCreateQRC);
        llWidgetScanQRA = findViewById(R.id.llWidgetScanQRA);
        llWidgetScanQRB = findViewById(R.id.llWidgetScanQRB);

        rlAdView = findViewById(R.id.rlAdView);
        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);
        rlNativeAdView = findViewById(R.id.rlNativeAdView);
        slNativeShimmer = findViewById(R.id.slNativeShimmer);
        flNativeAd = findViewById(R.id.flNativeAd);

        initialClicks();
    }

    private void initialClicks() {
        showAd();

        ivBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                AppWidgetsActivity.this.finish();
                overridePendingTransition(0, 0);
            }
        });

        llWidgetAppIcon.setOnClickListener(v -> requestWidgetPin(WidgetType.APP_ICON));
        llWidgetCreateQRA.setOnClickListener(v -> requestWidgetPin(WidgetType.CREATE_QR_A));
        llWidgetCreateQRB.setOnClickListener(v -> requestWidgetPin(WidgetType.CREATE_QR_B));
        llWidgetCreateQRC.setOnClickListener(v -> requestWidgetPin(WidgetType.CREATE_QR_C));
        llWidgetScanQRA.setOnClickListener(v -> requestWidgetPin(WidgetType.SCAN_QR_A));
        llWidgetScanQRB.setOnClickListener(v -> requestWidgetPin(WidgetType.SCAN_QR_B));
    }

    private void showAd() {
        RemoteConfigValues.ScreenAdConfig config = RemoteConfigValues.getScreenAd("OtherScreen");
        if (config == null || !config.show) {
            rlAdView.setVisibility(View.GONE);
            return;
        }
        rlAdView.setVisibility(View.VISIBLE);
        if ("banner".equalsIgnoreCase(config.type)) {
            rlBannerAdView.setVisibility(View.VISIBLE);
            rlNativeAdView.setVisibility(View.GONE);
            AdPlacement.loadBannerAd(this, config.bannerId, rlBannerAdView, slBannerShimmer, llBannerAd);
        } else {
            rlBannerAdView.setVisibility(View.GONE);
            rlNativeAdView.setVisibility(View.VISIBLE);
            AdPlacement.loadNativeAd(this, config.nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, "medium");
        }
    }

    private void requestWidgetPin(WidgetType widgetType) {
        WidgetPinHelper.requestPin(this, widgetType);
    }
}
