package com.qrcode.scanner.launcher.widgets;

import androidx.annotation.NonNull;

import com.qrcode.scanner.R;
import com.qrcode.scanner.launcher.common.WidgetNavigation;

public class ScanQrAWidgetProvider extends BaseAppWidgetProvider {
    @Override
    protected int getLayoutResId() {
        return R.layout.widget_scan_qr_a;
    }

    @NonNull
    @Override
    protected String getTargetScreen() {
        return WidgetNavigation.TARGET_SCAN;
    }
}
