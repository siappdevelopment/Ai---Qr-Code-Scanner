package com.qrcode.scanner.launcher.widgets;

import androidx.annotation.NonNull;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.WidgetNavigation;

public class CreateQrAWidgetProvider extends BaseAppWidgetProvider {
    @Override
    protected int getLayoutResId() {
        return R.layout.widget_create_qr_a;
    }

    @NonNull
    @Override
    protected String getTargetScreen() {
        return WidgetNavigation.TARGET_CREATE;
    }
}
