package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Remote Config of the exit dialog. Values are saved so they also work on a cold start.
 * <p>
 * Firebase (profile root, next to "Screen"):
 * <pre>
 * "ExitDialog": true,                    // true = dialog on Back, false / missing = Back closes the app at once
 * "exit_native_id": "ca-app-pub-..."     // medium native inside the dialog; empty = dialog without an ad
 * </pre>
 */
public final class ExitDialogConfig {
    private static final String PREFS = "exit_dialog_config_preferences";
    private static final String KEY_SHOW = "ExitDialog";
    private static final String KEY_NATIVE_ID = "exit_native_id";

    private ExitDialogConfig() {
    }

    /** {@code root} = profile root of the Remote Config JSON. A missing key means off / no ad. */
    public static void apply(@NonNull Context context, @Nullable JSONObject root) {
        JSONObject top = root == null ? new JSONObject() : root;
        prefs(context).edit()
                .putBoolean(KEY_SHOW, top.optBoolean(KEY_SHOW, false))
                .putString(KEY_NATIVE_ID, top.optString(KEY_NATIVE_ID, "").trim())
                .apply();
    }

    public static boolean isEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_SHOW, false);
    }

    @NonNull
    public static String nativeId(@NonNull Context context) {
        String id = prefs(context).getString(KEY_NATIVE_ID, "");
        return id == null ? "" : id.trim();
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
