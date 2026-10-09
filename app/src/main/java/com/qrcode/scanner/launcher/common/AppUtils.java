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
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity;
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig;

import java.util.Locale;

/**
 * Launcher and onboarding helpers.
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
    private static final String KEY_DEFAULT_HOME_COMPLETED = "default_home_completed";
    private static final String KEY_DEFAULT_SETTING_HOME_COMPLETED = "default_setting_home_completed";
    private static final String KEY_INTRO_COMPLETED = "intro_completed";
    private static final String PREFS_DEFAULT_APP_FLOW = "default_app_flow";
    private static final String KEY_COMPLETING_DEFAULT_APP_SETUP = "completing_default_app_setup";
    private static final String KEY_AWAITING_DEFAULT_ROLE_RESULT = "awaiting_default_role_result";

    private static boolean afterDefaultFlowClaimed;

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
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
    }

    public static void applyLanguage(@NonNull Context context, @NonNull String languageCode) {
        String normalizedCode = languageCode.isEmpty() ? "en" : languageCode;
        setLanguage(context, normalizedCode);
        appLanguage = normalizedCode;
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalizedCode));
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

    public static boolean isDefaultHomeScreenCompleted(Context context) {
        return context != null && screenFlowPrefs(context).getBoolean(KEY_DEFAULT_HOME_COMPLETED, false);
    }

    public static void setDefaultHomeScreenCompleted(Context context, boolean completed) {
        if (context == null) {
            return;
        }
        screenFlowPrefs(context).edit().putBoolean(KEY_DEFAULT_HOME_COMPLETED, completed).apply();
    }

    public static boolean isDefaultSettingHomeScreenCompleted(Context context) {
        return context != null && screenFlowPrefs(context).getBoolean(KEY_DEFAULT_SETTING_HOME_COMPLETED, false);
    }

    public static void setDefaultSettingHomeScreenCompleted(Context context, boolean completed) {
        if (context == null) {
            return;
        }
        screenFlowPrefs(context).edit().putBoolean(KEY_DEFAULT_SETTING_HOME_COMPLETED, completed).apply();
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
                .putBoolean(KEY_DEFAULT_HOME_COMPLETED, false)
                .putBoolean(KEY_DEFAULT_SETTING_HOME_COMPLETED, false)
                .apply();
        setIntroCompleted(context, false);
    }

    public static void clearDefaultAppSetupState(Context context) {
        if (context == null) {
            return;
        }
        setCompletingDefaultAppSetup(context, false);
        releaseAfterDefaultFlowClaim();
    }

    public static void setCompletingDefaultAppSetup(Context context, boolean value) {
        if (context == null) {
            return;
        }
        afterDefaultFlowClaimed = false;
        SharedPreferences.Editor editor = context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).edit().putBoolean(KEY_COMPLETING_DEFAULT_APP_SETUP, value);
        if (!value) {
            editor.putBoolean(KEY_AWAITING_DEFAULT_ROLE_RESULT, false);
        }
        editor.apply();
    }

    public static synchronized void releaseAfterDefaultFlowClaim() {
        afterDefaultFlowClaimed = false;
    }

    public static void setAwaitingDefaultRoleResult(Context context, boolean value) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).edit().putBoolean(KEY_AWAITING_DEFAULT_ROLE_RESULT, value).apply();
    }

    public static boolean isAwaitingDefaultRoleResult(Context context) {
        if (context == null) {
            return false;
        }
        return context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).getBoolean(KEY_AWAITING_DEFAULT_ROLE_RESULT, false);
    }

    public static synchronized boolean tryClaimAfterDefaultFlow(Context context) {
        if (!isCompletingDefaultAppSetup(context)) {
            return false;
        }
        if (afterDefaultFlowClaimed) {
            return false;
        }
        afterDefaultFlowClaimed = true;
        return true;
    }

    public static boolean isCompletingDefaultAppSetup(Context context) {
        if (context == null) {
            return false;
        }
        return context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).getBoolean(KEY_COMPLETING_DEFAULT_APP_SETUP, false);
    }

    public static void navigateAfterDefaultAppSetup(Context context) {
        if (context == null) {
            return;
        }
        setCompletingDefaultAppSetup(context, false);
        setDefaultHomeScreenCompleted(context, true);
        if (isDefaultHomeApp(context)) {
            TrackOnce.trackScreenOnce(context, "DEFAULT_HOME_APP_SET");
        } else {
            TrackOnce.trackScreenOnce(context, "DEFAULT_HOME_APP_CANCEL");
        }
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                AdPlacement.loadAfterDefaultAd(activity, () -> ScreenFlowNavigation.continueAfter(activity, ScreenFlowConfig.SCREEN_DEFAULT_HOME));
            }
        }
    }

    public static Intent buildLauncherHomeIntent(Context context) {
        Intent intent = new Intent(context, LauncherHomeActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return intent;
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

    public static boolean isDefaultHomeApp(Context context) {
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Boolean roleHeld = roleHomeHeldOrNull(context);
            if (roleHeld != null) {
                return roleHeld;
            }
        }
        return isDefaultHomeByResolve(context);
    }

    public static Intent createDefaultHomeRoleRequestIntent(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }
        return createRoleHomeRequestIntent(context);
    }

    public static boolean openDefaultHomeChooser(Activity activity) {
        if (activity == null) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Boolean roleHomeHeldOrNull(Context context) {
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return roleManager.isRoleHeld(RoleManager.ROLE_HOME);
        }
        return null;
    }

    private static Intent createRoleHomeRequestIntent(Context context) {
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_HOME) || roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
            return null;
        }
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);
    }

    private static boolean isDefaultHomeByResolve(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return false;
        }
        return context.getPackageName().equals(resolveInfo.activityInfo.packageName);
    }

    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static boolean isOwnApp(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return false;
        }
        return packageName.equals(context.getApplicationContext().getPackageName());
    }

    public static void openAppInfo(Context context, String packageName) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open app info", Toast.LENGTH_SHORT).show();
        }
    }

    public static String getApplicationLabelEnglish(Context context, ApplicationInfo applicationInfo) {
        if (context == null || applicationInfo == null) {
            return "";
        }
        try {
            if (applicationInfo.nonLocalizedLabel != null) {
                String nonLocalized = applicationInfo.nonLocalizedLabel.toString().trim();
                if (!nonLocalized.isEmpty()) {
                    return nonLocalized;
                }
            }
            if (applicationInfo.labelRes != 0) {
                String englishLabel = loadPackageStringEnglish(context, applicationInfo.packageName, applicationInfo.labelRes);
                if (!englishLabel.isEmpty()) {
                    return englishLabel;
                }
            }
        } catch (Exception ignored) {
        }
        try {
            CharSequence label = context.getPackageManager().getApplicationLabel(applicationInfo);
            return label != null ? label.toString().trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    public static String getResolveInfoLabelEnglish(Context context, ResolveInfo resolveInfo) {
        if (context == null || resolveInfo == null) {
            return "";
        }
        try {
            if (resolveInfo.nonLocalizedLabel != null) {
                String nonLocalized = resolveInfo.nonLocalizedLabel.toString().trim();
                if (!nonLocalized.isEmpty()) {
                    return nonLocalized;
                }
            }
            if (resolveInfo.activityInfo != null) {
                ApplicationInfo applicationInfo = resolveInfo.activityInfo.applicationInfo;
                int labelRes = resolveInfo.activityInfo.labelRes;
                if (labelRes == 0 && applicationInfo != null) {
                    labelRes = applicationInfo.labelRes;
                }
                if (labelRes != 0 && applicationInfo != null && applicationInfo.packageName != null) {
                    String englishLabel = loadPackageStringEnglish(context, applicationInfo.packageName, labelRes);
                    if (!englishLabel.isEmpty()) {
                        return englishLabel;
                    }
                }
                if (applicationInfo != null) {
                    String appLabel = getApplicationLabelEnglish(context, applicationInfo);
                    if (!appLabel.isEmpty()) {
                        return appLabel;
                    }
                }
            }
            CharSequence label = resolveInfo.loadLabel(context.getPackageManager());
            return label != null ? label.toString().trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    @NonNull
    private static String loadPackageStringEnglish(@NonNull Context context, @NonNull String packageName, int labelRes) {
        if (labelRes == 0 || packageName.isEmpty()) {
            return "";
        }
        try {
            Configuration configuration = new Configuration();
            configuration.setLocale(Locale.ENGLISH);
            Context packageContext = context.createPackageContext(packageName, 0);
            Context englishContext = packageContext.createConfigurationContext(configuration);
            CharSequence label = englishContext.getResources().getText(labelRes);
            if (label == null) {
                return "";
            }
            return label.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    public static void applyDialogPadding(Activity activity, android.app.Dialog dialog) {
        if (activity == null || dialog == null || dialog.getWindow() == null) {
            return;
        }
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, activity.getResources().getDisplayMetrics());
        dialog.getWindow().getDecorView().setPadding(padding, 0, padding, 0);
    }

    public static boolean hasPhoneStatePermission(Context context) {
        if (context == null) {
            return false;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public static void trackPhoneStatePermissionGrantedOnce(Context context) {
        trackScreenOnce(context, "READ_PHONE_STATE_GRANTED");
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

    public static boolean hasOverlayPermission(Context context) {
        if (context == null) {
            return false;
        }
        try {
            return Settings.canDrawOverlays(context);
        } catch (Exception e) {
            return false;
        }
    }

    public static void trackOverlayPermissionGrantedOnce(Context context) {
        trackScreenOnce(context, "OVERLAY_PERMISSION_GRANTED");
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
