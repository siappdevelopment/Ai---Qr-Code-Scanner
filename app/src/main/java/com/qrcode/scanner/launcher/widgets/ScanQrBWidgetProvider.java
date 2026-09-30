package com.qrcode.scanner.launcher.widgets;

import androidx.annotation.NonNull;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.WidgetNavigation;

public class ScanQrBWidgetProvider extends BaseAppWidgetProvider {
    @Override
    protected int getLayoutResId() {
        return R.layout.widget_scan_qr_b;
    }

    @NonNull
    @Override
    protected String getTargetScreen() {
        return WidgetNavigation.TARGET_SCAN;
    }
}
