package com.qrcode.scanner.launcher.helpers;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.qrcode.scanner.R;
import com.qrcode.scanner.launcher.common.WidgetType;

public final class WidgetPinHelper {
    private WidgetPinHelper() {
    }

    public static boolean requestPin(@NonNull Activity activity, @NonNull WidgetType widgetType) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(activity, R.string.widget_pin_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }

        AppWidgetManager appWidgetManager = activity.getSystemService(AppWidgetManager.class);
        if (appWidgetManager == null || !appWidgetManager.isRequestPinAppWidgetSupported()) {
            Toast.makeText(activity, R.string.widget_pin_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }

        ComponentName provider = new ComponentName(activity, widgetType.getProviderClass());
        boolean requested = appWidgetManager.requestPinAppWidget(provider, null, null);
        if (!requested) {
            Toast.makeText(activity, R.string.widget_pin_failed, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}
