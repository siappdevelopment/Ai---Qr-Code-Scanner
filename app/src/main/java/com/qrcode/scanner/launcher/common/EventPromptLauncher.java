package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.activities.ChargingScreenActivity;
import com.qrcode.scanner.launcher.activities.EventPromptActivity;
import com.qrcode.scanner.launcher.activities.InstallUninstallScreenActivity;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

/**
 * Charge and uninstall broadcasts arrive while this app is in the background.
 * A plain startActivity is blocked there, so this follows the call-end path:
 * a short overlay, then the activity, then a full-screen notification.
 */
public final class EventPromptLauncher {
    private static final int NOTIFICATION_UNINSTALL = 9101;
    private static final int NOTIFICATION_CHARGE_IN = 9102;
    private static final int NOTIFICATION_CHARGE_OUT = 9103;
    private static final int NOTIFICATION_INSTALL = 9104;

    private static long lastOpenAt;
    private static String lastKind = "";

    private EventPromptLauncher() {
    }

    public static void open(Context context, String kind) {
        Log.d("EventPrompt", "open requested kind=" + kind);
        if (context == null || kind == null || kind.trim().isEmpty()) {
            Log.d("EventPrompt", "open skipped: missing context or kind");
            return;
        }
        RemoteConfigValues.ensureLoaded(context);
        RemoteConfigValues.EventScreenConfig config = RemoteConfigValues.getEventScreenConfig(RemoteConfigValues.eventScreenKeyForKind(kind));
        Log.d("EventPrompt", "config uninstall=" + RemoteConfigValues.isEventScreenEnabled(EventPromptActivity.KIND_UNINSTALL)
                + " install=" + RemoteConfigValues.isEventScreenEnabled(EventPromptActivity.KIND_INSTALL)
                + " chargeIn=" + RemoteConfigValues.isEventScreenEnabled(EventPromptActivity.KIND_CHARGE_IN)
                + " chargeOut=" + RemoteConfigValues.isEventScreenEnabled(EventPromptActivity.KIND_CHARGE_OUT)
                + " bottomAds=" + config.bottomAdShow
                + " loadType=" + config.adLoadType
                + " adType=" + config.bottomAdType
                + " seconds=" + config.buttonShowSec
                + " backAds=" + config.backAdShow);
        if (!RemoteConfigValues.isEventScreenEnabled(kind)) {
            Log.d("EventPrompt", "open skipped: " + kind + " flag is false");
            return;
        }
        if (!AdPlacement.isNetworkAvailable(context)) {
            Log.d("EventPrompt", "open skipped: no internet for " + kind);
            return;
        }
        long now = System.currentTimeMillis();
        if (kind.equals(lastKind) && now - lastOpenAt < 1500L) {
            Log.d("EventPrompt", "open skipped: duplicate " + kind + " within 1500ms");
            return;
        }
        lastKind = kind;
        lastOpenAt = now;
        Log.d("EventPrompt", "opening " + kind);
        Context app = context.getApplicationContext();
        Class<?> screen = RemoteConfigValues.EVENT_SCREEN_CHARGING.equals(RemoteConfigValues.eventScreenKeyForKind(kind))
                ? ChargingScreenActivity.class
                : InstallUninstallScreenActivity.class;
        Intent intent = new Intent(app, screen);
        intent.putExtra(EventPromptActivity.EXTRA_KIND, kind);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        Runnable show = () -> showEvent(app, intent, kind);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            show.run();
        } else {
            new Handler(Looper.getMainLooper()).post(show);
        }
    }

    private static int notificationIdFor(String kind) {
        return EventPromptActivity.KIND_CHARGE_IN.equals(kind)
                ? NOTIFICATION_CHARGE_IN
                : EventPromptActivity.KIND_CHARGE_OUT.equals(kind)
                ? NOTIFICATION_CHARGE_OUT
                : EventPromptActivity.KIND_INSTALL.equals(kind)
                ? NOTIFICATION_INSTALL
                : NOTIFICATION_UNINSTALL;
    }

    /** Removes the full-screen-intent notification of this kind (the screen is up, or a newer one replaces it). */
    public static void cancelNotification(Context context, String kind) {
        if (context == null || kind == null) {
            return;
        }
        androidx.core.app.NotificationManagerCompat.from(context.getApplicationContext()).cancel(notificationIdFor(kind));
        Log.d("EventPrompt", "notification cancelled kind=" + kind);
    }

    private static void showEvent(Context app, Intent intent, String kind) {
        try {
            Log.d("EventPrompt", "show overlay for " + kind);
            new CallEndViewManager().showCallEnd(app, intent);
            Log.d("EventPrompt", "overlay accepted for " + kind);
            return;
        } catch (Exception overlayError) {
            Log.d("EventPrompt", "overlay failed for " + kind + ": " + overlayError);
        }
        try {
            Log.d("EventPrompt", "startActivity for " + kind);
            app.startActivity(intent);
            Log.d("EventPrompt", "startActivity accepted for " + kind);
        } catch (Exception startError) {
            Log.d("EventPrompt", "startActivity failed for " + kind + ": " + startError + ", posting notification");
            int notificationId = notificationIdFor(kind);
            // A stale notification of this kind must not suppress the new one: remove it first, then post.
            cancelNotification(app, kind);
            String title = EventPromptActivity.KIND_CHARGE_IN.equals(kind)
                    ? app.getString(R.string.event_charge_in_title)
                    : EventPromptActivity.KIND_CHARGE_OUT.equals(kind)
                    ? app.getString(R.string.event_charge_out_title)
                    : EventPromptActivity.KIND_INSTALL.equals(kind)
                    ? app.getString(R.string.event_install_sub)
                    : app.getString(R.string.event_uninstall_sub);
            CallEndFullNotificationHelper.notifyGenericFullscreen(
                    app,
                    intent,
                    notificationId,
                    title,
                    title,
                    title,
                    R.mipmap.ic_launcher,
                    null
            );
        }
    }
}
