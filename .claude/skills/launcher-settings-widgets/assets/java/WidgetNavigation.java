package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qrcode.scanner.MainActivity;
import com.qrcode.scanner.ui.screens.create.CreateQrIntents;
import com.qrcode.scanner.ui.screens.scan.ScanIntents;

public final class WidgetNavigation {
    public static final String EXTRA_TARGET_SCREEN = "TARGET_SCREEN";
    public static final String TARGET_SCAN = "scan";
    public static final String TARGET_CREATE = "create";

    private static final String PREFS_NAME = "widget_navigation";
    private static final String KEY_PENDING_TARGET_SCREEN = "pending_target_screen";

    private WidgetNavigation() {
    }

    @NonNull
    public static Intent createLaunchIntent(@NonNull Context context, @NonNull String targetScreen) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_TARGET_SCREEN, targetScreen);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    public static void captureTargetScreen(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        String targetScreen = intent.getStringExtra(EXTRA_TARGET_SCREEN);
        if (targetScreen == null || targetScreen.isEmpty()) {
            return;
        }

        getPrefs(context).edit().putString(KEY_PENDING_TARGET_SCREEN, targetScreen).apply();
        intent.removeExtra(EXTRA_TARGET_SCREEN);
    }

    public static void attachPendingTargetScreen(@NonNull Context context, @NonNull Intent mainIntent) {
        String targetScreen = consumePendingTargetScreen(context);
        if (targetScreen != null && !targetScreen.isEmpty()) {
            mainIntent.putExtra(EXTRA_TARGET_SCREEN, targetScreen);
        }
    }

    public static void openComposeDestinationFromIntent(@NonNull Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) {
            return;
        }
        String targetScreen = intent.getStringExtra(EXTRA_TARGET_SCREEN);
        if (targetScreen == null || targetScreen.isEmpty()) {
            return;
        }
        intent.removeExtra(EXTRA_TARGET_SCREEN);
        openComposeDestination(activity, targetScreen);
    }

    public static void openPendingComposeDestination(@NonNull Activity activity) {
        openComposeDestination(activity, consumePendingTargetScreen(activity));
    }

    private static void openComposeDestination(@NonNull Activity activity, @Nullable String targetScreen) {
        if (TARGET_CREATE.equals(targetScreen)) {
            activity.startActivity(CreateQrIntents.INSTANCE.openHub(activity));
        } else if (TARGET_SCAN.equals(targetScreen)) {
            activity.startActivity(ScanIntents.INSTANCE.openScanner(activity));
        }
    }

    @Nullable
    private static String consumePendingTargetScreen(@NonNull Context context) {
        SharedPreferences prefs = getPrefs(context);
        String targetScreen = prefs.getString(KEY_PENDING_TARGET_SCREEN, null);
        if (targetScreen != null) {
            prefs.edit().remove(KEY_PENDING_TARGET_SCREEN).apply();
        }
        return targetScreen;
    }

    @NonNull
    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
