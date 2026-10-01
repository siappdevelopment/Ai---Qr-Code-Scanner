package com.qrcode.scanner.launcher.remote;

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.AdPlacement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Source Remote Config fetch.
 * A second call during an active fetch keeps its callback instead of dropping it.
 * paid_user is selected only when the stored install referrer matches the source markers.
 */
public final class RemoteConfigHelper {
    private static final AtomicBoolean IS_FETCHING = new AtomicBoolean(false);
    private static final CopyOnWriteArrayList<FetchCallback> PENDING_CALLBACKS = new CopyOnWriteArrayList<>();

    public interface FetchCallback {
        void onComplete(boolean success);
    }

    private RemoteConfigHelper() {
    }

    public static void fetchRemoteConfig(@Nullable Context context, @Nullable FetchCallback onComplete) {
        if (context == null) {
            return;
        }
        synchronized (RemoteConfigHelper.class) {
            if (onComplete != null) {
                PENDING_CALLBACKS.add(onComplete);
            }
            if (IS_FETCHING.get()) {
                return;
            }
            IS_FETCHING.set(true);
        }
        Context appContext = context.getApplicationContext();
        if (!ensureFirebase(appContext)) {
            ScreenFlowConfig.ensureShowScreenFlow(appContext);
            dispatchPending(false);
            return;
        }

        int version = getAppVersion(appContext);
        String key = "QRScanner_" + version;
        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        firebaseRemoteConfig.setConfigSettingsAsync(settings);
        firebaseRemoteConfig.setDefaultsAsync(R.xml.default_config);
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            boolean fetchSucceeded = false;
            if (task.isSuccessful()) {
                try {
                    String remoteJson = firebaseRemoteConfig.getString(key);
                    if (remoteJson == null || remoteJson.trim().isEmpty()) {
                        ScreenFlowConfig.restoreShowScreenFlow(appContext);
                        ScreenFlowConfig.ensureShowScreenFlow(appContext);
                        dispatchPending(true);
                        return;
                    }
                    String trimmedJson = remoteJson.trim();
                    JSONObject jsonObject;
                    if (trimmedJson.startsWith("[")) {
                        JSONArray jsonArray = new JSONArray(trimmedJson);
                        if (jsonArray.length() == 0) {
                            ScreenFlowConfig.restoreShowScreenFlow(appContext);
                            ScreenFlowConfig.ensureShowScreenFlow(appContext);
                            dispatchPending(true);
                            return;
                        }
                        jsonObject = jsonArray.getJSONObject(0);
                    } else {
                        jsonObject = new JSONObject(trimmedJson);
                    }
                    jsonObject = resolveUserConfig(jsonObject, referrerProfile(appContext));
                    applyScreenConfig(appContext, jsonObject, firebaseRemoteConfig.getString("Show_Screen_Flow"));
                    fetchSucceeded = true;
                } catch (Exception e) {
                    ScreenFlowConfig.restoreShowScreenFlow(appContext);
                    ScreenFlowConfig.ensureShowScreenFlow(appContext);
                }
            } else {
                ScreenFlowConfig.restoreShowScreenFlow(appContext);
                ScreenFlowConfig.ensureShowScreenFlow(appContext);
            }
            dispatchPending(fetchSucceeded);
        });
    }

    private static String referrerProfile(Context context) {
        String referrerUrl = AdPlacement.getReferrerUrl(context);
        String marketingUrl = manageMarketingConverter(referrerUrl);
        String facebookUser = facebookMarketingConverter(referrerUrl);
        String newString = stringMarketingConverter(referrerUrl);
        if (newString.equals(context.getString(R.string.marketing_converter1))
                || facebookUser.equals(context.getString(R.string.utm_source_apps_facebook_com))
                || facebookUser.equals(context.getString(R.string.utm_source_apps_instagram_com))
                || marketingUrl.equals(context.getString(R.string.utm_source_marketing))) {
            return "paid_user";
        }
        return "normal_user";
    }

    private static String stringMarketingConverter(String str) {
        if (str == null) {
            return "";
        }
        String[] arr = str.split("=", 2);
        return arr.length > 0 ? arr[0] : "";
    }

    private static String manageMarketingConverter(String str) {
        if (str == null) {
            return "";
        }
        String[] arr = str.split("=", 2);
        if (arr.length > 1) {
            return arr[1];
        }
        return "";
    }

    private static String facebookMarketingConverter(String input) {
        if (input == null) {
            return "";
        }
        int index = input.indexOf('&');
        if (index != -1) {
            return input.substring(0, index);
        }
        return "";
    }

    private static boolean ensureFirebase(Context context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                return FirebaseApp.initializeApp(context) != null;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void dispatchPending(boolean success) {
        List<FetchCallback> callbacks;
        synchronized (RemoteConfigHelper.class) {
            IS_FETCHING.set(false);
            callbacks = new ArrayList<>(PENDING_CALLBACKS);
            PENDING_CALLBACKS.clear();
        }
        for (FetchCallback callback : callbacks) {
            if (callback != null) {
                callback.onComplete(success);
            }
        }
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    private static JSONObject resolveUserConfig(JSONObject root, String profileKey) throws Exception {
        if (root == null) {
            throw new Exception("Remote Config root JSON is null");
        }
        if (!root.has(profileKey)) {
            return root;
        }
        Object profileValue = root.get(profileKey);
        if (profileValue instanceof JSONObject) {
            return (JSONObject) profileValue;
        }
        if (profileValue instanceof String) {
            String nestedKey = ((String) profileValue).trim();
            if (nestedKey.isEmpty()) {
                throw new Exception("Remote Config profile key '" + profileKey + "' is empty");
            }
            if (nestedKey.startsWith("{")) {
                return new JSONObject(nestedKey);
            }
            return root.getJSONObject(nestedKey);
        }
        throw new Exception("Unsupported Remote Config profile type for '" + profileKey + "': " + profileValue);
    }

    private static void applyScreenConfig(Context context, JSONObject jsonObject, String topLevelFlowJson) {
        ScreenFlowConfig.applyShowScreenFlowFromConfig(jsonObject, topLevelFlowJson);
        ScreenFlowConfig.cacheShowScreenFlow(context);
        ScreenFlowConfig.setPrivacyPolicy(jsonObject.optString("Privacy_Policy", ""));
        ScreenFlowConfig.setRedirectHomeLauncher(jsonObject.optBoolean("Redirect_Home_Launcher", false));
        JSONObject screen = jsonObject.optJSONObject("Screen");
        if (screen == null) {
            screen = new JSONObject();
        }
        JSONObject introScreen = screen.optJSONObject("IntroScreen");
        if (introScreen == null) {
            introScreen = new JSONObject();
        }
        ScreenFlowConfig.setIntroScreenCount(introScreen.optInt("Intro_Screen_Count", 0));
        applyAdFields(jsonObject, screen);
        RemoteConfigValues.apply(context, jsonObject, screen);
    }

    private static void applyAdFields(JSONObject jsonObject, JSONObject screen) {
        AdPlacement.setAdPriority(jsonObject.optString("Ad_Priority", ""));
        AdPlacement.setGoogleAdFailedShowQuiz(jsonObject.optBoolean("Google_Ad_Failed_Show_Quiz", false));
        AdPlacement.setNativeAdLabelColor(jsonObject.optString("Native_Ad_Label_Color", ""));
        AdPlacement.setNativeAdButtonColor(jsonObject.optString("Native_Ad_Button_Color", ""));
        AdPlacement.setAppOpenId(jsonObject.optString("App_Open_Id", ""));
        JSONObject splashScreen = screen.optJSONObject("SplashScreen");
        if (splashScreen == null) {
            splashScreen = new JSONObject();
        }
        AdPlacement.setSplashDuration(splashScreen.optInt("Splash_Duration", 0));
        AdPlacement.setSplashAdShow(splashScreen.optBoolean("Splash_Ad_Show", false));
        AdPlacement.setSplashAdType(splashScreen.optString("Splash_Ad_Type", "banner"));
        AdPlacement.setSplashBannerId(splashScreen.optString("Splash_Banner_Id", ""));
        AdPlacement.setSplashNativeId(splashScreen.optString("Splash_Native_Id", ""));
        AdPlacement.setAfterSplashAdShow(splashScreen.optBoolean("After_Splash_Ad_Show", false));
        AdPlacement.setAfterSplashAdType(splashScreen.optString("After_Splash_Ad_Type", "inter"));
        AdPlacement.setAfterSplashInterstitialId(splashScreen.optString("After_Splash_Interstitial_Id", ""));
        JSONObject languageScreen = screen.optJSONObject("LanguageScreen");
        if (languageScreen == null) {
            languageScreen = new JSONObject();
        }
        AdPlacement.setLanguageAdShow(languageScreen.optBoolean("Language_Ad_Show", false));
        AdPlacement.setLanguageAdType(languageScreen.optString("Language_Ad_Type", "native"));
        AdPlacement.setLanguageBannerId(languageScreen.optString("Language_Banner_Id", ""));
        AdPlacement.setLanguageNativeId(languageScreen.optString("Language_Native_Id", ""));
        AdPlacement.setLanguageInterstitialAdShow(languageScreen.optBoolean("Language_Interstitial_Ad_Show", false));
        JSONObject permissionDefaultScreen = screen.optJSONObject("PermissionDefaultScreen");
        if (permissionDefaultScreen == null) {
            permissionDefaultScreen = new JSONObject();
        }
        AdPlacement.setPermissionDefaultAdShow(permissionDefaultScreen.optBoolean("PermissionDefault_Ad_Show", false));
        AdPlacement.setPermissionDefaultAdType(permissionDefaultScreen.optString("PermissionDefault_Ad_Type", "banner"));
        AdPlacement.setPermissionDefaultBannerId(permissionDefaultScreen.optString("PermissionDefault_Banner_Id", ""));
        AdPlacement.setPermissionDefaultNativeId(permissionDefaultScreen.optString("PermissionDefault_Native_Id", ""));
        AdPlacement.applyAfterDefaultAdConfig(permissionDefaultScreen);
        JSONObject intro = screen.optJSONObject("IntroScreen");
        if (intro == null) {
            intro = new JSONObject();
        }
        AdPlacement.setIntroAdShow(intro.optBoolean("Intro_Ad_Show", false));
        AdPlacement.setIntroAdType(intro.optString("Intro_Ad_Type", "banner"));
        AdPlacement.setIntroBannerId(intro.optString("Intro_Banner_Id", ""));
        AdPlacement.setIntroNativeId(intro.optString("Intro_Native_Id", ""));
        AdPlacement.setIntroInterstitialAdShow(intro.optBoolean("Intro_Interstitial_Ad_Show", false));
        JSONObject launcherAppScreen = screen.optJSONObject("LauncherAppScreen");
        if (launcherAppScreen == null) {
            launcherAppScreen = new JSONObject();
        }
        AdPlacement.setLauncherAppNativeListAdShow(launcherAppScreen.optBoolean("LauncherApp_Native_List_Ad_Show", false));
        AdPlacement.setLauncherAppNativeListAdShowPerDay(launcherAppScreen.optInt("LauncherApp_Native_List_Ad_Show_Per_Day", 0));
        AdPlacement.setLauncherAppNativeListId(launcherAppScreen.optString("LauncherApp_Native_List_Id", ""));
        AdPlacement.setLauncherAppClickAdShow(launcherAppScreen.optBoolean("LauncherApp_Click_Ad_Show", false));
        AdPlacement.setLauncherAppCount(launcherAppScreen.optInt("LauncherApp_Count", 0));
        AdPlacement.setLauncherAppAdType(launcherAppScreen.optString("LauncherApp_Ad_Type", ""));
        AdPlacement.setLauncherAppInterstitialId(launcherAppScreen.optString("LauncherApp_Interstitial_Id", ""));
        AdPlacement.setLauncherAppBackClickAdShow(launcherAppScreen.optBoolean("LauncherApp_Back_Click_Ad_Show", false));
        AdPlacement.setLauncherAppBackCount(launcherAppScreen.optInt("LauncherApp_Back_Count", 0));
        AdPlacement.setLauncherAppBackAdType(launcherAppScreen.optString("LauncherApp_Back_Ad_Type", ""));
        AdPlacement.setLauncherAppBackInterstitialId(launcherAppScreen.optString("LauncherApp_Back_Interstitial_Id", ""));
    }
}
