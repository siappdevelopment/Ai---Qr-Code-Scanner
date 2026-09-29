package com.qrcode.scanner.launcher.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.qrcode.scanner.R;
import com.qrcode.scanner.launcher.common.WidgetNavigation;

public abstract class BaseAppWidgetProvider extends AppWidgetProvider {
    @LayoutRes
    protected abstract int getLayoutResId();

    @NonNull
    protected abstract String getTargetScreen();

    @Override
    public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, @NonNull int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getLayoutResId());
        Intent launchIntent = WidgetNavigation.createLaunchIntent(context, getTargetScreen());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, getRequestCode(appWidgetId), launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        remoteViews.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private int getRequestCode(int appWidgetId) {
        return (getClass().getName().hashCode() * 31) + appWidgetId;
    }
}
