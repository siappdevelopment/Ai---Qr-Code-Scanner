package com.qrcode.scanner.launcher.common;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.widgets.AppIconWidgetProvider;
import com.qrcode.scanner.launcher.widgets.CreateQrAWidgetProvider;
import com.qrcode.scanner.launcher.widgets.CreateQrBWidgetProvider;
import com.qrcode.scanner.launcher.widgets.CreateQrCWidgetProvider;
import com.qrcode.scanner.launcher.widgets.ScanQrAWidgetProvider;
import com.qrcode.scanner.launcher.widgets.ScanQrBWidgetProvider;

public enum WidgetType {
    APP_ICON(
            R.drawable.ic_widget_app_logo,
            R.string.app_icon_1x1,
            R.string.qr_scanner_generator,
            AppIconWidgetProvider.class,
            WidgetNavigation.TARGET_SCAN
    ),
    CREATE_QR_A(
            R.drawable.ic_widget_create_qr_1,
            R.string.create_1_x_1,
            R.string.style_a,
            CreateQrAWidgetProvider.class,
            WidgetNavigation.TARGET_CREATE
    ),
    CREATE_QR_B(
            R.drawable.ic_widget_create_qr_2,
            R.string.create_1_x_1,
            R.string.style_b,
            CreateQrBWidgetProvider.class,
            WidgetNavigation.TARGET_CREATE
    ),
    CREATE_QR_C(
            R.drawable.ic_widget_create_qr_3,
            R.string.create_1_x_1,
            R.string.style_c,
            CreateQrCWidgetProvider.class,
            WidgetNavigation.TARGET_CREATE
    ),
    SCAN_QR_A(
            R.drawable.ic_widget_scan_qr_1,
            R.string.create_1_x_1,
            R.string.style_a,
            ScanQrAWidgetProvider.class,
            WidgetNavigation.TARGET_SCAN
    ),
    SCAN_QR_B(
            R.drawable.ic_widget_scan_qr_2,
            R.string.create_1_x_1,
            R.string.style_b,
            ScanQrBWidgetProvider.class,
            WidgetNavigation.TARGET_SCAN
    );

    @DrawableRes
    private final int previewImageRes;
    @StringRes
    private final int titleRes;
    @StringRes
    private final int descriptionRes;
    @NonNull
    private final Class<?> providerClass;
    @NonNull
    private final String targetScreen;

    WidgetType(
            @DrawableRes int previewImageRes,
            @StringRes int titleRes,
            @StringRes int descriptionRes,
            @NonNull Class<?> providerClass,
            @NonNull String targetScreen
    ) {
        this.previewImageRes = previewImageRes;
        this.titleRes = titleRes;
        this.descriptionRes = descriptionRes;
        this.providerClass = providerClass;
        this.targetScreen = targetScreen;
    }

    @DrawableRes
    public int getPreviewImageRes() {
        return previewImageRes;
    }

    @StringRes
    public int getTitleRes() {
        return titleRes;
    }

    @StringRes
    public int getDescriptionRes() {
        return descriptionRes;
    }

    @NonNull
    public Class<?> getProviderClass() {
        return providerClass;
    }

    @NonNull
    public String getTargetScreen() {
        return targetScreen;
    }
}
