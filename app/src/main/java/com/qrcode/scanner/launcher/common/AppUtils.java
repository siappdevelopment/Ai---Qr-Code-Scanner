package com.qrcode.scanner.launcher.common;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;

import java.util.Locale;

/**
 * App and onboarding helpers.
 * Language selection uses AppCompat locales. Onboarding completion uses the screen_flow file.
 * One-time analytics use a separate analytics_events file.
 */
public final class AppUtils {
    public static String appLanguage;
    public static boolean isLanguageFromStarting;

    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_FILE_LANGUAGE = "language";
    private static final String PREF_SCREEN_FLOW = "screen_flow";
    private static final String KEY_LANGUAGE_FLOW_COMPLETED = "language_flow_completed";
    private static final String KEY_LANGUAGE_SELECTED = "language_selected";
    private static final String KEY_COLLECTION_COMPLETED = "collection_completed";
    private static final String KEY_PERMISSION_COMPLETED = "permission_completed";
    private static final String KEY_INTRO_COMPLETED = "intro_completed";

    private AppUtils() {
    }

    public static void setLanguage(Context context, String value) {
        if (context == null) {
            return;
        }
        context.getSharedPreferences(PREF_FILE_LANGUAGE, MODE_PRIVATE).edit().putString(PREF_LANGUAGE, value).apply();
    }

    public static String getLanguage(Context context) {
        if (context == null) {
            return "en";
        }
        return context.getSharedPreferences(PREF_FILE_LANGUAGE, MODE_PRIVATE).getString(PREF_LANGUAGE, "en");
    }

    public static void restoreSavedLanguage(@NonNull Context context) {
        String languageCode = getLanguage(context);
        appLanguage = languageCode;
        setAppLocales(context, languageCode);
    }

    public static void applyLanguage(@NonNull Context context, @NonNull String languageCode) {
        String normalizedCode = languageCode.isEmpty() ? "en" : languageCode;
        setLanguage(context, normalizedCode);
        appLanguage = normalizedCode;
        setAppLocales(context, normalizedCode);
    }

    /**
     * Android 13 can reject the platform per-app locale call with "Can't change activity type once set" when this
     * app has a Home-type task (it is a launcher) next to its normal tasks. The language is already saved, so
     * instead of crashing, fall back to updating this app's own resources configuration.
     */
    private static void setAppLocales(@NonNull Context context, @NonNull String languageCode) {
        try {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
        } catch (IllegalStateException e) {
            Locale locale = Locale.forLanguageTag(languageCode);
            Locale.setDefault(locale);
            Context app = context.getApplicationContext();
            Configuration configuration = new Configuration(app.getResources().getConfiguration());
            configuration.setLocale(locale);
            app.getResources().updateConfiguration(configuration, app.getResources().getDisplayMetrics());
        }
    }

    public static String getStringForLanguage(@NonNull Context context, @NonNull String languageCode, int stringResId) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(languageCode));
        return context.createConfigurationContext(configuration).getString(stringResId);
    }

    public static boolean getLanguageFlowCompleted(Context context) {
        return context != null && screenFlowPrefs(context).getBoolean(KEY_LANGUAGE_FLOW_COMPLETED, false);
    }

    public static void setLanguageFlowCompleted(Context context, boolean completed) {
        if (context == null) {
            return;
        }
        screenFlowPrefs(context).edit().putBoolean(KEY_LANGUAGE_FLOW_COMPLETED, completed).apply();
    }

    public static void setLanguageSelectedCommit(Context context, boolean selected) {
        if (context == null) {
            return;
        }
        screenFlowPrefs(context).edit().putBoolean(KEY_LANGUAGE_SELECTED, selected).apply();
    }

    public static boolean isCollectionScreenCompleted(Context context) {
        return context != null && screenFlowPrefs(context).getBoolean(KEY_COLLECTION_COMPLETED, false);
    }

    public static void setCollectionScreenCompleted(Context context, boolean completed) {
        if (context == null) {
            return;
        }
        screenFlowPrefs(context).edit().putBoolean(KEY_COLLECTION_COMPLETED, completed).apply();
    }

    public static boolean isPermissionScreenCompleted(Context context) {
        return context != null && screenFlowPrefs(context).getBoolean(KEY_PERMISSION_COMPLETED, false);
    }

    public static void setPermissionScreenCompleted(Context context, boolean completed) {
        if (context == null) {
            return;
        }
        screenFlowPrefs(context).edit().putBoolean(KEY_PERMISSION_COMPLETED, completed).apply();
    }

    public static boolean getIntroCompleted(Context context) {
        return context != null && screenFlowPrefs(context).getBoolean(KEY_INTRO_COMPLETED, false);
    }

    public static void setIntroCompleted(Context context, boolean completed) {
        if (context == null) {
            return;
        }
        screenFlowPrefs(context).edit().putBoolean(KEY_INTRO_COMPLETED, completed).apply();
    }

    public static boolean hasCompletedOnboarding(Context context) {
        return getIntroCompleted(context);
    }

    public static void resetOnboardingScreenCompletions(Context context) {
        if (context == null) {
            return;
        }
        screenFlowPrefs(context).edit()
                .putBoolean(KEY_LANGUAGE_FLOW_COMPLETED, false)
                .putBoolean(KEY_COLLECTION_COMPLETED, false)
                .putBoolean(KEY_PERMISSION_COMPLETED, false)
                .putInt("flow_progress_index", 0)
                .apply();
        setIntroCompleted(context, false);
    }

    /** Furthest flow position reached in this onboarding run. Completed screens before it are never cleared again. */
    public static int getFlowProgress(Context context) {
        return context == null ? 0 : screenFlowPrefs(context).getInt("flow_progress_index", 0);
    }

    public static void advanceFlowProgress(Context context, int position) {
        if (context == null || position <= getFlowProgress(context)) {
            return;
        }
        screenFlowPrefs(context).edit().putInt("flow_progress_index", position).apply();
    }

    public static void openPrivacyPolicy(Context context) {
        if (context == null) {
            return;
        }
        String url = ScreenFlowConfig.getPrivacyPolicy();
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(context, "Privacy Policy Not Found !", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()));
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Privacy Policy Not Found !", Toast.LENGTH_SHORT).show();
        }
    }

    private static SharedPreferences screenFlowPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_SCREEN_FLOW, MODE_PRIVATE);
    }

    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static void applyDialogPadding(Activity activity, android.app.Dialog dialog) {
        if (activity == null || dialog == null || dialog.getWindow() == null) {
            return;
        }
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, activity.getResources().getDisplayMetrics());
        dialog.getWindow().getDecorView().setPadding(padding, 0, padding, 0);
    }

    /**
     * Everything the Permission screen asks for is already allowed: camera and notifications (Android 13+). Used to skip
     * that screen when the flow runs again.
     */
    public static boolean areOnboardingPermissionsAllowed(Context context) {
        if (context == null) {
            return false;
        }
        boolean notification = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || AdPlacement.isNotificationGranted(context);
        return hasCameraPermission(context) && notification;
    }

    public static boolean hasCameraPermission(Context context) {
        if (context == null) {
            return false;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public static void trackCameraPermissionGrantedOnce(Context context) {
        trackScreenOnce(context, "CAMERA_GRANTED");
    }

    public static void trackScreenOnce(Context context, String screenName) {
        if (context == null || screenName == null || screenName.isEmpty()) {
            return;
        }
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences("analytics_events", MODE_PRIVATE);
        if (sharedPreferences.getBoolean(screenName, false)) {
            return;
        }
        if (!trackScreenEvent(context, screenName)) {
            return;
        }
        sharedPreferences.edit().putBoolean(screenName, true).apply();
    }

    public static void trackScreen(Context context, String screenName) {
        if (context == null || screenName == null || screenName.isEmpty()) {
            return;
        }
        trackScreenEvent(context, screenName);
    }

    private static boolean trackScreenEvent(Context context, String screenName) {
        try {
            Context appContext = context.getApplicationContext();
            Bundle bundle = new Bundle();
            bundle.putLong("value", 1L);
            FirebaseAnalytics.getInstance(appContext).logEvent(screenName, bundle);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isUninstallableApp(PackageManager packageManager, String packageName) {
        if (packageManager == null || packageName == null || packageName.isEmpty()) {
            return false;
        }
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
            return (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
        } catch (Exception ignored) {
            return false;
        }
    }
}
