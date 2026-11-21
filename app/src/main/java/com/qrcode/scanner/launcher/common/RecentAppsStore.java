package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Most-recently-launched apps (opened through this launcher). 30 stored, 12 shown. */
public final class RecentAppsStore {
    private static final String PREFS = "recent_apps_preferences";
    private static final String KEY = "packages";
    private static final int MAX_STORED = 30;
    private static final int MAX_SHOWN = 12;

    private RecentAppsStore() {
    }

    public static synchronized void record(@Nullable Context context, @Nullable String packageName) {
        if (context == null || packageName == null || packageName.trim().isEmpty() || packageName.equals(context.getPackageName())) {
            return;
        }
        List<String> packages = read(context);
        packages.remove(packageName);
        packages.add(0, packageName);
        while (packages.size() > MAX_STORED) {
            packages.remove(packages.size() - 1);
        }
        prefs(context).edit().putString(KEY, String.join(",", packages)).apply();
    }

    /** "Clear all" of the Recent screen. */
    public static synchronized void clear(@Nullable Context context) {
        if (context != null) {
            prefs(context).edit().remove(KEY).apply();
        }
    }

    public static synchronized List<String> recentApps(Context context) {
        List<String> result = new ArrayList<>();
        for (String packageName : read(context)) {
            if (result.size() >= MAX_SHOWN) {
                break;
            }
            if (context.getPackageManager().getLaunchIntentForPackage(packageName) != null) {
                result.add(packageName);
            }
        }
        return result;
    }

    private static List<String> read(Context context) {
        List<String> packages = new ArrayList<>();
        String stored = prefs(context).getString(KEY, "");
        if (stored != null && !stored.isEmpty()) {
            for (String packageName : stored.split(",")) {
                if (!packageName.isEmpty()) {
                    packages.add(packageName);
                }
            }
        }
        return packages;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
