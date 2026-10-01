package com.qrcode.scanner.launcher.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telecom.TelecomManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.helpers.LauncherSettingsHelper;

import java.util.function.IntConsumer;

public class LauncherSettingsActivity extends AppCompatActivity {
    private AppCompatImageView ivBack, ivArrow;
    private AppCompatTextView tvSelectedSerialize, tvAppIconSizePercentage, tvAppLabelSizePercentage;
    private AppCompatImageView[] appIcons;
    private AppCompatTextView[] appNames;
    private LinearLayout llDropdown;
    private SwitchCompat scLabelVisibility;
    private AppCompatSeekBar sbAppIconSize, sbAppLabelSize;

    private RelativeLayout rlAdView, rlNativeAdView, rlBannerAdView;
    private ShimmerFrameLayout slNativeShimmer, slBannerShimmer;
    private FrameLayout flNativeAd;
    private LinearLayout llBannerAd;

    private boolean suppressLabelListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher_settings);

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
        llDropdown = findViewById(R.id.llDropdown);
        tvSelectedSerialize = findViewById(R.id.tvSelectedSerialize);
        ivArrow = findViewById(R.id.ivArrow);
        appIcons = new AppCompatImageView[]{findViewById(R.id.ivAppIcon1), findViewById(R.id.ivAppIcon2), findViewById(R.id.ivAppIcon3), findViewById(R.id.ivAppIcon4)};
        appNames = new AppCompatTextView[]{findViewById(R.id.tvAppName1), findViewById(R.id.tvAppName2), findViewById(R.id.tvAppName3), findViewById(R.id.tvAppName4)};
        scLabelVisibility = findViewById(R.id.scLabelVisibility);
        sbAppIconSize = findViewById(R.id.sbAppIconSize);
        tvAppIconSizePercentage = findViewById(R.id.tvAppIconSizePercentage);
        sbAppLabelSize = findViewById(R.id.sbAppLabelSize);
        tvAppLabelSizePercentage = findViewById(R.id.tvAppLabelSizePercentage);

        rlAdView = findViewById(R.id.rlAdView);
        rlNativeAdView = findViewById(R.id.rlNativeAdView);
        slNativeShimmer = findViewById(R.id.slNativeShimmer);
        flNativeAd = findViewById(R.id.flNativeAd);
        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);

        initialClicks();
    }

    private void initialClicks() {
        showAd();
        setApp();
        refreshSettingsUI();

        ivBack.setOnClickListener(v -> {
            LauncherSettingsActivity.this.finish();
            overridePendingTransition(0, 0);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                LauncherSettingsActivity.this.finish();
                overridePendingTransition(0, 0);
            }
        });

        llDropdown.setOnClickListener(this::showSerializeMenu);

        scLabelVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressLabelListener) {
                return;
            }
            LauncherSettingsHelper.setLabelVisibility(this, isChecked);
            applyLabelVisibility(isChecked);
        });

        sbAppIconSize.setOnSeekBarChangeListener(createSeekBarListener(value -> LauncherSettingsHelper.setAppIconSize(this, value), this::applyIconSize, tvAppIconSizePercentage));

        sbAppLabelSize.setOnSeekBarChangeListener(createSeekBarListener(value -> LauncherSettingsHelper.setAppLabelSize(this, value), this::applyLabelSize, tvAppLabelSizePercentage));
    }

    private void showAd() {
        RemoteConfigValues.ScreenAdConfig config = RemoteConfigValues.getScreenAd("LauncherSettingsScreen");
        boolean show = config != null && config.show;
        String type = config == null ? "banner" : config.type;
        String bannerId = config == null ? "" : config.bannerId;
        String nativeId = config == null ? "" : config.nativeId;
        AdPlacement.showSlot(this, show, type, bannerId, nativeId, "medium", rlAdView, rlBannerAdView, slBannerShimmer, llBannerAd, rlNativeAdView, slNativeShimmer, flNativeAd, false);
    }

    private void setApp() {
        PackageManager packageManager = getPackageManager();

        TelecomManager telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
        if (telecomManager != null) {
            String dialerPackage = telecomManager.getDefaultDialerPackage();
            if (dialerPackage != null) {
                try {
                    ApplicationInfo applicationInfo = packageManager.getApplicationInfo(dialerPackage, 0);
                    appIcons[0].setImageDrawable(packageManager.getApplicationIcon(dialerPackage));
                    appNames[0].setText(AppUtils.getApplicationLabelEnglish(this, applicationInfo));
                } catch (Exception ignored) {
                }
            }
        }

        try {
            appIcons[1].setImageDrawable(packageManager.getApplicationIcon(getPackageName()));
            appNames[1].setText(R.string.app_name);
        } catch (Exception ignored) {
        }

        ResolveInfo cameraInfo = packageManager.resolveActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), PackageManager.MATCH_DEFAULT_ONLY);
        if (cameraInfo != null) {
            appIcons[2].setImageDrawable(cameraInfo.loadIcon(packageManager));
            appNames[2].setText(AppUtils.getResolveInfoLabelEnglish(this, cameraInfo));
        }

        try {
            ApplicationInfo settingsInfo = packageManager.getApplicationInfo("com.android.settings", 0);
            appIcons[3].setImageDrawable(packageManager.getApplicationIcon(settingsInfo));
            appNames[3].setText(AppUtils.getApplicationLabelEnglish(this, settingsInfo));
        } catch (Exception ignored) {
        }
    }

    private void refreshSettingsUI() {
        updateSelectedSerialize();

        boolean labelVisible = LauncherSettingsHelper.getLabelVisibility(this);
        suppressLabelListener = true;
        scLabelVisibility.setChecked(labelVisible);
        suppressLabelListener = false;
        applyLabelVisibility(labelVisible);

        configureSeekBar(sbAppIconSize, LauncherSettingsHelper.MIN_APP_ICON_SIZE, LauncherSettingsHelper.MAX_APP_ICON_SIZE, LauncherSettingsHelper.getAppIconSize(this), tvAppIconSizePercentage);
        applyIconSize(LauncherSettingsHelper.getAppIconSize(this));

        configureSeekBar(sbAppLabelSize, LauncherSettingsHelper.MIN_APP_LABEL_SIZE, LauncherSettingsHelper.MAX_APP_LABEL_SIZE, LauncherSettingsHelper.getAppLabelSize(this), tvAppLabelSizePercentage);
        applyLabelSize(LauncherSettingsHelper.getAppLabelSize(this));
    }

    private void updateSelectedSerialize() {
        tvSelectedSerialize.setText(LauncherSettingsHelper.getSerializeLabelResId(LauncherSettingsHelper.getAppSerialize(this)));
    }

    private void applyLabelVisibility(boolean isVisible) {
        int visibility = isVisible ? VISIBLE : GONE;
        for (AppCompatTextView nameView : appNames) {
            nameView.setVisibility(visibility);
        }
    }

    private void configureSeekBar(AppCompatSeekBar seekBar, int min, int max, int progress, AppCompatTextView label) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seekBar.setMin(min);
        }
        seekBar.setMax(max);
        seekBar.setProgress(progress);
        label.setText(String.valueOf(progress));
    }

    private void applyIconSize(int sizeDp) {
        int sizePx = AppUtils.dpToPx(this, sizeDp);
        for (AppCompatImageView icon : appIcons) {
            ViewGroup.LayoutParams params = icon.getLayoutParams();
            params.width = sizePx;
            params.height = sizePx;
            icon.setLayoutParams(params);
        }
    }

    private void applyLabelSize(int sizeSp) {
        for (AppCompatTextView nameView : appNames) {
            nameView.setTextSize(sizeSp);
        }
    }

    private SeekBar.OnSeekBarChangeListener createSeekBarListener(IntConsumer saveValue, IntConsumer applyValue, AppCompatTextView label) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                label.setText(String.valueOf(progress));
                applyValue.accept(progress);
                if (fromUser) {
                    saveValue.accept(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
    }

    @SuppressLint("InflateParams")
    private void showSerializeMenu(View anchor) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.dialog_serialize, null);

        PopupWindow popupWindow = new PopupWindow(popupView, anchor.getWidth(), WindowManager.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setClippingEnabled(false);
        popupWindow.setElevation(0f);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        String serialize = LauncherSettingsHelper.getAppSerialize(this);
        popupView.findViewById(R.id.ivNameAscSelect).setVisibility(LauncherSettingsHelper.SERIALIZE_NAME_ASC.equals(serialize) ? VISIBLE : GONE);
        popupView.findViewById(R.id.ivNameDescSelect).setVisibility(LauncherSettingsHelper.SERIALIZE_NAME_DESC.equals(serialize) ? VISIBLE : GONE);
        popupView.findViewById(R.id.ivInstalledDateSelect).setVisibility(LauncherSettingsHelper.SERIALIZE_INSTALLED_DATE.equals(serialize) ? VISIBLE : GONE);

        View.OnClickListener clickListener = view -> {
            int id = view.getId();
            if (id == R.id.llNameAsc) {
                LauncherSettingsHelper.setAppSerialize(this, LauncherSettingsHelper.SERIALIZE_NAME_ASC);
            } else if (id == R.id.llNameDesc) {
                LauncherSettingsHelper.setAppSerialize(this, LauncherSettingsHelper.SERIALIZE_NAME_DESC);
            } else if (id == R.id.llInstalledDate) {
                LauncherSettingsHelper.setAppSerialize(this, LauncherSettingsHelper.SERIALIZE_INSTALLED_DATE);
            } else {
                return;
            }
            popupWindow.dismiss();
            updateSelectedSerialize();
        };

        popupView.findViewById(R.id.llNameAsc).setOnClickListener(clickListener);
        popupView.findViewById(R.id.llNameDesc).setOnClickListener(clickListener);
        popupView.findViewById(R.id.llInstalledDate).setOnClickListener(clickListener);

        ivArrow.animate().rotation(270f).setDuration(180).start();
        popupWindow.setOnDismissListener(() -> ivArrow.animate().rotation(90f).setDuration(180).start());

        int yOff = (int) (6 * getResources().getDisplayMetrics().density);
        popupWindow.showAsDropDown(anchor, 0, yOff);
    }
}