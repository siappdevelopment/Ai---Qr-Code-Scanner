package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.media.AudioManager;
import android.telephony.TelephonyManager;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.activities.ChargingScreenActivity;
import com.qrcode.scanner.launcher.activities.ClEndActivity;
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
    /** A call is ringing or active: the event screens stay closed. */
    private static volatile boolean callActive;
    /** Short quiet time after each phone-state broadcast (see {@link #onPhoneStateChanged}). */
    private static volatile long quietUntil;
    private static final long QUIET_AFTER_PHONE_STATE_MS = 5000L;
    private static final long HELD_BACK_WINDOW_MS = 3500L;
    private static String lastKind = "";

    private EventPromptLauncher() {
    }

    /**
     * Charging / Install broadcasts that arrive while this app is frozen are held back and delivered when a phone-state
     * broadcast wakes it. Those are old events, so no event screen opens during a call, nor for a few seconds after
     * any phone-state change (this also keeps them away from the Call End screen).
     */
    public static void onPhoneStateChanged(boolean callInProgress) {
        callActive = callInProgress;
        quietUntil = System.currentTimeMillis() + QUIET_AFTER_PHONE_STATE_MS;
    }

    /**
     * The real phone state, read when the event is handled. A held-back Charging / Install broadcast is delivered
     * before the phone-state broadcast that woke the app, so the flags above are not set yet at that moment.
     */
    private static boolean isPhoneCallInProgress(Context context) {
        try {
            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephony != null && telephony.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                return true;
            }
        } catch (Exception ignored) {
        }
        try {
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int mode = audio == null ? AudioManager.MODE_NORMAL : audio.getMode();
            return mode == AudioManager.MODE_RINGTONE || mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Internet check for the event screens. {@code getActiveNetwork()} returns null for an app that the phone has put
     * to sleep in the background (Xiaomi / Android battery limits block its network), although Wi-Fi or mobile data is
     * connected, which made these screens skip with "no internet". Read the network capabilities themselves instead.
     */
    public static boolean hasInternet(Context context) {
        if (AdPlacement.isNetworkAvailable(context)) {
            return true;
        }
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }
            for (android.net.Network network : cm.getAllNetworks()) {
                android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                if (caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Charging / Install broadcasts that were held back while the app was closed or frozen are delivered the moment
     * the app starts or comes to the foreground. Those events are old, so a screen opened for them would just
     * appear on top of whatever the user opened the app for. A real plug / install does not happen in the first
     * seconds of the process or of the app coming forward, so those deliveries are ignored.
     */
    public static boolean isHeldBackDelivery() {
        long sinceProcessStart = android.os.SystemClock.elapsedRealtime() - android.os.Process.getStartElapsedRealtime();
        return sinceProcessStart < HELD_BACK_WINDOW_MS || ProcessAppOpen.msSinceForeground() < HELD_BACK_WINDOW_MS;
    }

    public static void open(Context context, String kind) {
        if (context == null || kind == null || kind.trim().isEmpty()) {
            return;
        }
        RemoteConfigValues.ensureLoaded(context);
        RemoteConfigValues.EventScreenConfig config = RemoteConfigValues.getEventScreenConfig(RemoteConfigValues.eventScreenKeyForKind(kind));
        if (!RemoteConfigValues.isEventScreenEnabled(kind)) {
            return;
        }
        if (!hasInternet(context)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (callActive || isPhoneCallInProgress(context) || now < quietUntil || ClEndActivity.isVisible()) {
            return;
        }
        if (kind.equals(lastKind) && now - lastOpenAt < 1500L) {
            return;
        }
        lastKind = kind;
        lastOpenAt = now;
        Context app = context.getApplicationContext();
        Class<?> screen = RemoteConfigValues.EVENT_SCREEN_CHARGING.equals(RemoteConfigValues.eventScreenKeyForKind(kind))
                ? ChargingScreenActivity.class
                : InstallUninstallScreenActivity.class;
        Intent intent = new Intent(app, screen);
        intent.putExtra(EventPromptActivity.EXTRA_KIND, kind);
        intent.putExtra(EventPromptActivity.EXTRA_LAUNCHED_AT, System.currentTimeMillis());
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
    }

    private static void showEvent(Context app, Intent intent, String kind) {
        // Without the overlay permission, and not as the default Home app, a background start cannot bring the screen
        // up. Android still creates it inside this app's existing task, so it would be waiting there and appear the
        // next time the app is opened. Start it only when it can really show: app open, overlay allowed, or default Home.
        if (!ProcessAppOpen.isAppInForeground() && !AppUtils.hasOverlayPermission(app) && !AppUtils.isDefaultHomeApp(app)) {
            return;
        }
        try {
            new CallEndViewManager().showCallEnd(app, intent);
            return;
        } catch (Exception overlayError) {
        }
        try {
            app.startActivity(intent);
        } catch (Exception startError) {
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
