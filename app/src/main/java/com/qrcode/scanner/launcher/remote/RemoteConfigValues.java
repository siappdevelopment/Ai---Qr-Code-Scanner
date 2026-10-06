package com.qrcode.scanner.launcher.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qrcode.scanner.launcher.common.AdPlacement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Source Remote Config values that are not already applied to the migrated ad slots.
 * App-proxy lookup starts after these values are stored and does not block startup.
 */
public final class RemoteConfigValues {
    private static final String CL_END_CONFIG_PREFS = "cl_end_config_preferences";

    private static boolean appOpenAdShow;
    private static boolean appOpenDialogShow;
    private static int appOpenShowPerDay;
    private static boolean interAdsShow;
    private static int interAdsClick;
    private static String interAdsClickType = "load";
    private static boolean interAdsOnBack;
    private static int interAdsBackClick;
    private static String interAdsBackClickType = "load";
    private static String interAdsId = "";
    private static String interBackAdsId = "";
    private static boolean bottomNavInterAdsShow;
    private static String bottomNavInterAdsType = "load";
    private static String bottomNavInterAdsId = "";
    private static int bottomNavInterClick;
    private static boolean rightSwipeInterstitialAdShow;
    private static int rightSwipeInterstitial;
    private static String rightSwipeAdsType ;
    private static boolean clEndConfigLoaded;
    private static boolean clEndScreenShow;
    private static boolean defaultAppPopupShow;
    private static int defaultAppPopupCount;

    private static boolean mainAdShow;
    private static boolean mainBigTopAdShow;
    private static String mainAdType = "banner";
    private static String mainBannerId = "";
    private static String mainNativeId = "";
//    private static boolean mainAdAutoRefresh;
    private static int mainAdAutoSecond;
    private static boolean mainBottomAdAutoRefresh;
    private static int mainBottomAdAutoSecond;

//    private static boolean createFragmentNativeAdShow;
//    private static String createFragmentNativeId = "";
//    private static int createFragmentNativeSecond;

    private static boolean settingsFragmentNativeAdShow;
    private static String settingsFragmentNativeId = "";
    private static int settingsFragmentNativeSecond;

    private static boolean launcherAppNativeAdShow;
    private static int launcherAppNativeAdShowPerDay;
    private static String launcherAppNativeId = "";
    private static boolean launcherAppQuizIconShow;
    private static int launcherAppQuizIconCount;

    private static boolean eventUninstallShow;
    private static boolean eventChargeInShow;
    private static boolean eventChargeOutShow;
    private static boolean eventBottomAdsShow;
    private static String eventBottomAdsType = "load";
    private static String eventBottomAdsView = "native";
    private static String eventNativeId = "";
    private static String eventBannerId = "";
    private static int eventScreenShowSeconds = 6;
    private static boolean eventBackAdsShow;

    private static boolean clEndAdShow;
    private static String clEndAdType = "banner";
    private static String clEndBannerId = "";
    private static String clEndNativeId = "";
    private static boolean clEndBackAdShow;
    private static String clEndBackAdType = "inter";
    private static ArrayList<String[]> clEndBackAdSequence = new ArrayList<>();
    private static int clEndBackAdShowAfterDay;
    private static int clEndBackAdShowPerDay;
    private static String clEndBackAdInterstitialId = "";
    private static String clEndBackAdNativeId = "";
    private static boolean clEndBackAdCountryIP;
    private static ArrayList<String> clEndBackAdShowCountryList = new ArrayList<>();
    private static int notificationInstallDays;
    private static int notificationCallInstallDays;
    private static int notificationCallOverlayInstallDays;
    private static boolean allAllowPermissionShowNotification;
    private static boolean notificationBackAdShow;
    private static boolean notificationCloseButtonShow;
    private static ArrayList<String> notificationCountryList = new ArrayList<>();
    private static ArrayList<String> notificationCallCountryList = new ArrayList<>();
    private static ArrayList<String> notificationCallOverlayCountryList = new ArrayList<>();

    private static final List<String> quizAppIconList = new ArrayList<>();
    private static final List<String> quizNativeMediaList = new ArrayList<>();
    private static final List<String> quizInterstitialMediaList = new ArrayList<>();
    private static final List<String> quizAppOpenMediaList = new ArrayList<>();
    private static final List<String> quizBannerTitleList = new ArrayList<>();
    private static final List<String> quizBannerDescriptionList = new ArrayList<>();
    private static final List<String> quizNativeTitleList = new ArrayList<>();
    private static final List<String> quizNativeDescriptionList = new ArrayList<>();
    private static final List<String> quizInterstitialTitleList = new ArrayList<>();
    private static final List<String> quizInterstitialDescriptionList = new ArrayList<>();
    private static final List<String> quizInterstitialRateList = new ArrayList<>();
    private static final List<String> quizLinkList = new ArrayList<>();
    private static final List<String> quizLinkExcludeList = new ArrayList<>();
    private static final List<String> activeQuizLinkList = new ArrayList<>();
    private static String quizButtonText = "";

    private static boolean appProxyCheckIp;
    private static String appProxyIpCheckerUrl = "";
    private static final List<String> appProxyListCity = new ArrayList<>();
    private static final List<String> appProxyListState = new ArrayList<>();
    private static final List<String> appProxyListCountry = new ArrayList<>();

    private RemoteConfigValues() {
    }

    public static void apply(@Nullable Context context, @Nullable JSONObject jsonObject, @Nullable JSONObject screen) {
        JSONObject root = jsonObject == null ? new JSONObject() : jsonObject;
        JSONObject screenObject = screen == null ? new JSONObject() : screen;

        appOpenAdShow = root.optBoolean("App_Open_Ad_Show", false);
        appOpenDialogShow = root.optBoolean("App_Open_Dialog_Show", false);
        appOpenShowPerDay = root.optInt("App_Open_Show_Per_Day", 0);
        applyInterAds(root.optJSONObject("inter_ads"));
        clEndScreenShow = root.optBoolean("Cl_End_Screen_Show", false);
        defaultAppPopupShow = root.optBoolean("Default_App_Popup_Show", false);
        defaultAppPopupCount = root.optInt("Default_App_Popup_Count", 0);

        JSONObject mainScreen = child(screenObject, "MainScreen");
        mainAdShow = mainScreen.optBoolean("Main_Ad_Show", false);
        mainBigTopAdShow = mainScreen.optBoolean("Main_Big_Top_Ad_Show", false);
        mainAdType = mainScreen.optString("Main_Ad_Type", "banner");
        mainBannerId = mainScreen.optString("Main_Banner_Id", "");
        mainNativeId = mainScreen.optString("Main_Native_Id", "");
//        mainAdAutoRefresh = mainScreen.optBoolean("Main_Ad_Auto_Refresh", false);
        mainAdAutoSecond = mainScreen.optInt("Main_Ad_Auto_Second", 0);
        mainBottomAdAutoRefresh = mainScreen.optBoolean("Main_Bottom_Ad_Auto_Refresh", false);
        mainBottomAdAutoSecond = mainScreen.optInt("Main_Bottom_Ad_Auto_Second", 0);
        bottomNavInterAdsShow = mainScreen.optBoolean("bottom_nav_inter_ads_show", false);
        bottomNavInterAdsType = mainScreen.optString("bottom_nav_inter_ads_type", "load");
        bottomNavInterAdsId = mainScreen.optString("bottom_nav_inter_ads_id", "");
        bottomNavInterClick = mainScreen.optInt("bottom_nav_inter_click", 0);
        rightSwipeInterstitialAdShow = mainScreen.optBoolean("Right_Swipe_Interstitial_Ad_Show", false);
        rightSwipeInterstitial = mainScreen.optInt("Right_Swipe_Interstitial", 0);
//        Log.d("TAG", "Right_Swipe_Ads_type : "   +mainScreen.optString("Right_Swipe_Interstitial"));
        rightSwipeAdsType = optStringIgnoreCase(mainScreen, "Right_Swipe_Ads_type", "load");

//        Log.d("TAG", "Right_Swipe_Ads_type : "   +mainScreen.optString("Right_Swipe_Ads_type"));

        applyScreenAds(screenObject);
        rememberInterFlags("MainScreen", mainScreen);

        JSONObject settingsFragmentScreen = child(screenObject, "SettingsFragmentScreen");
        settingsFragmentNativeAdShow = settingsFragmentScreen.optBoolean("SettingsFragment_Native_Ad_Show", false);
        settingsFragmentNativeId = settingsFragmentScreen.optString("SettingsFragment_Native_Id", "");
        settingsFragmentNativeSecond = settingsFragmentScreen.optInt("SettingsFragment_Native_Second", 0);
        rememberInterFlags("SettingsFragmentScreen", settingsFragmentScreen);

        JSONObject eventScreen = child(screenObject, "EventScreen");
        if (!eventScreen.has("uninstall_screen_show") && root.has("EventScreen")) {
            eventScreen = child(root, "EventScreen");
        }
        eventUninstallShow = eventScreen.optBoolean("uninstall_screen_show", false);
        eventChargeInShow = eventScreen.optBoolean("charge_in_screen_show", false);
        eventChargeOutShow = eventScreen.optBoolean("charge_out_screen_show", false);
        eventBottomAdsShow = eventScreen.optBoolean("bottom_ads_show", false);
        eventBottomAdsType = eventScreen.optString("bottom_ads_type", "load");
        eventBottomAdsView = eventScreen.optString("bottom_ads_view", "native");
        eventNativeId = eventScreen.optString("native_id", "");
        eventBannerId = eventScreen.optString("banner_id", "");
        eventScreenShowSeconds = eventScreen.optInt("screen_show_seconds", 6);
        eventBackAdsShow = eventScreen.optBoolean("back_ads_show", false);

        JSONObject launcherAppScreen = child(screenObject, "LauncherAppScreen");
        launcherAppNativeAdShow = launcherAppScreen.optBoolean("LauncherApp_Native_Ad_Show", false);
        launcherAppNativeAdShowPerDay = launcherAppScreen.optInt("LauncherApp_Native_Ad_Show_Per_Day", 0);
        launcherAppNativeId = launcherAppScreen.optString("LauncherApp_Native_Id", "");
        launcherAppQuizIconShow = launcherAppScreen.optBoolean("LauncherApp_Quiz_Icon_Show", false);
        launcherAppQuizIconCount = launcherAppScreen.optInt("LauncherApp_Quiz_Icon_Count", 0);

        JSONObject clEndScreen = child(screenObject, "ClEndScreen");
        clEndAdShow = clEndScreen.optBoolean("ClEnd_Ad_Show", false);
        clEndAdType = clEndScreen.optString("ClEnd_Ad_Type", "banner");
        clEndBannerId = clEndScreen.optString("ClEnd_Banner_Id", "");
        clEndNativeId = clEndScreen.optString("ClEnd_Native_Id", "");
        clEndBackAdShow = clEndScreen.optBoolean("ClEnd_Back_Ad_Show", false);
        clEndBackAdType = clEndScreen.optString("ClEnd_Back_Ad_Type", "inter").trim();
        clEndBackAdSequence = parseBackAdSequence(clEndScreen.optJSONArray("ClEnd_Back_Ad_Sequence"));
        clEndBackAdShowAfterDay = clEndScreen.optInt("ClEnd_Back_Ad_Show_After_Day", 0);
        clEndBackAdShowPerDay = clEndScreen.optInt("ClEnd_Back_Ad_Show_Per_Day", 0);
        clEndBackAdInterstitialId = clEndScreen.optString("ClEnd_Back_Ad_Interstitial_Id", "");
        clEndBackAdNativeId = clEndScreen.optString("ClEnd_Back_Ad_Native_Id", "");
        clEndBackAdCountryIP = clEndScreen.optBoolean("ClEnd_Back_Ad_Country_IP", false);
        clEndBackAdShowCountryList = parseCountryArray(clEndScreen.optJSONArray("ClEnd_Back_Ad_Show_Country"));
        notificationInstallDays = clEndScreen.optInt("Notification_Install_Days", 0);
        notificationCallInstallDays = clEndScreen.optInt("Notification_Call_Install_Days", 0);
        notificationCallOverlayInstallDays = clEndScreen.optInt("Notification_Call_Overlay_Install_Days", 0);
        allAllowPermissionShowNotification = clEndScreen.optBoolean("All_Allow_Permission_Show_Notification", false);
        notificationBackAdShow = clEndScreen.optBoolean("Notification_Back_Ad_Show", false);
        notificationCloseButtonShow = clEndScreen.optBoolean("Notification_Close_Button_Show", false);
        notificationCountryList = parseCountryArray(clEndScreen.optJSONArray("Notification_Country"));
        notificationCallCountryList = parseCountryArray(clEndScreen.optJSONArray("Notification_Call_Country"));
        notificationCallOverlayCountryList = parseCountryArray(clEndScreen.optJSONArray("Notification_Call_Overlay_Country"));

        JSONObject quizAdsDesign = screenObject.optJSONObject("QuizAdsDesign");
        if (quizAdsDesign != null) {
            if (quizAdsDesign.has("Ad_Priority")) {
                AdPlacement.setAdPriority(quizAdsDesign.optString("Ad_Priority", ""));
            }
            if (quizAdsDesign.has("Google_Ad_Failed_Show_Quiz")) {
                AdPlacement.setGoogleAdFailedShowQuiz(quizAdsDesign.optBoolean("Google_Ad_Failed_Show_Quiz", false));
            }
            replaceIfPresent(quizAppIconList, quizAdsDesign, "Quiz_App_Icon");
            replaceIfPresent(quizNativeMediaList, quizAdsDesign, "Quiz_Native_Media");
            replaceIfPresent(quizInterstitialMediaList, quizAdsDesign, "Quiz_Interstitial_Media");
            replaceIfPresent(quizAppOpenMediaList, quizAdsDesign, "Quiz_App_Open_Media");
            replaceIfPresent(quizBannerTitleList, quizAdsDesign, "Quiz_Banner_Title");
            replaceIfPresent(quizBannerDescriptionList, quizAdsDesign, "Quiz_Banner_Description");
            replaceIfPresent(quizNativeTitleList, quizAdsDesign, "Quiz_Native_Title");
            replaceIfPresent(quizNativeDescriptionList, quizAdsDesign, "Quiz_Native_Description");
            replaceIfPresent(quizInterstitialTitleList, quizAdsDesign, "Quiz_Interstitial_Title");
            replaceIfPresent(quizInterstitialDescriptionList, quizAdsDesign, "Quiz_Interstitial_Description");
            replaceIfPresent(quizInterstitialRateList, quizAdsDesign, "Quiz_Interstitial_Rate");
            List<String> buttonTexts = parseStringList(quizAdsDesign, "Quiz_Button_Text");
            if (!buttonTexts.isEmpty()) {
                quizButtonText = buttonTexts.get(0);
            }
        }

        JSONObject appProxyStructure = screenObject.optJSONObject("AppProxyStructure");
        if (appProxyStructure == null) {
            appProxyStructure = root.optJSONObject("AppProxyStructure");
        }
        if (appProxyStructure != null) {
            appProxyCheckIp = appProxyStructure.optBoolean("App_Proxy_Check_Ip", false);
            appProxyIpCheckerUrl = appProxyStructure.optString("App_Proxy_Ip_Checker_Url", "").trim();
            replaceIfPresent(quizLinkList, appProxyStructure, "Quiz_Link_List");
            replaceIfPresent(quizLinkExcludeList, appProxyStructure, "Quiz_Link_List_Exclude");
            replaceIfPresent(appProxyListCity, appProxyStructure, "App_Proxy_List_City");
            replaceIfPresent(appProxyListState, appProxyStructure, "App_Proxy_List_State");
            replaceIfPresent(appProxyListCountry, appProxyStructure, "App_Proxy_List_Country");
        }

        saveClEndConfig(context);
        clEndConfigLoaded = true;
        com.qrcode.scanner.launcher.common.ScreenInterAds.onConfigApplied(context);
        com.qrcode.scanner.launcher.common.AdPlacement.prepareRightSwipePreload(context);
        com.qrcode.scanner.launcher.common.EventBottomAds.prepare(context);
        com.qrcode.scanner.launcher.helpers.AppProxyLookup.refreshActiveQuizLinks();
        AdPlacement.requestCallEndIpCountryIfNeeded(context);
    }

    public static void ensureLoaded(@Nullable Context context) {
        if (context == null || clEndConfigLoaded) {
            return;
        }
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(CL_END_CONFIG_PREFS, Context.MODE_PRIVATE);
        if (!preferences.contains("clEndScreenShow")) {
            return;
        }
        clEndScreenShow = preferences.getBoolean("clEndScreenShow", false);
        clEndAdShow = preferences.getBoolean("clEndAdShow", false);
        clEndAdType = preferences.getString("clEndAdType", "banner");
        clEndBannerId = preferences.getString("clEndBannerId", "");
        clEndNativeId = preferences.getString("clEndNativeId", "");
        clEndBackAdShow = preferences.getBoolean("clEndBackAdShow", false);
        clEndBackAdType = preferences.getString("clEndBackAdType", "inter");
        clEndBackAdSequence = parseStoredBackAdSequence(preferences.getString("clEndBackAdSequence", ""));
        clEndBackAdShowAfterDay = preferences.getInt("clEndBackAdShowAfterDay", 0);
        clEndBackAdShowPerDay = preferences.getInt("clEndBackAdShowPerDay", 0);
        clEndBackAdInterstitialId = preferences.getString("clEndBackAdInterstitialId", "");
        clEndBackAdNativeId = preferences.getString("clEndBackAdNativeId", "");
        clEndBackAdCountryIP = preferences.getBoolean("clEndBackAdCountryIP", false);
        clEndBackAdShowCountryList = parseStoredCountryList(preferences.getString("clEndBackAdShowCountryList", ""));
        notificationInstallDays = preferences.getInt("notificationInstallDays", 0);
        notificationCallInstallDays = preferences.getInt("notificationCallInstallDays", 0);
        notificationCallOverlayInstallDays = preferences.getInt("notificationCallOverlayInstallDays", 0);
        allAllowPermissionShowNotification = preferences.getBoolean("allAllowPermissionShowNotification", false);
        notificationBackAdShow = preferences.getBoolean("notificationBackAdShow", false);
        notificationCloseButtonShow = preferences.getBoolean("notificationCloseButtonShow", false);
        notificationCountryList = parseStoredCountryList(preferences.getString("notificationCountryList", ""));
        notificationCallCountryList = parseStoredCountryList(preferences.getString("notificationCallCountryList", ""));
        notificationCallOverlayCountryList = parseStoredCountryList(preferences.getString("notificationCallOverlayCountryList", ""));
        eventUninstallShow = preferences.getBoolean("eventUninstallShow", false);
        eventChargeInShow = preferences.getBoolean("eventChargeInShow", false);
        eventChargeOutShow = preferences.getBoolean("eventChargeOutShow", false);
        eventBottomAdsShow = preferences.getBoolean("eventBottomAdsShow", false);
        eventBottomAdsType = preferences.getString("eventBottomAdsType", "load");
        eventBottomAdsView = preferences.getString("eventBottomAdsView", "native");
        eventNativeId = preferences.getString("eventNativeId", "");
        eventBannerId = preferences.getString("eventBannerId", "");
        eventScreenShowSeconds = preferences.getInt("eventScreenShowSeconds", 6);
        eventBackAdsShow = preferences.getBoolean("eventBackAdsShow", false);
        rightSwipeInterstitialAdShow = preferences.getBoolean("rightSwipeInterstitialAdShow", false);
        rightSwipeInterstitial = preferences.getInt("rightSwipeInterstitial", 0);
        rightSwipeAdsType = preferences.getString("rightSwipeAdsType", "load");
        clEndConfigLoaded = true;
        com.qrcode.scanner.launcher.common.EventBottomAds.prepare(context);
        com.qrcode.scanner.launcher.common.AdPlacement.prepareRightSwipePreload(context);
    }

    public static boolean getMainAdShow() {
        return mainAdShow;
    }

    public static String getMainAdType() {
        return mainAdType == null ? "" : mainAdType;
    }

    public static String getMainBannerId() {
        return mainBannerId == null ? "" : mainBannerId;
    }

    public static boolean getMainBottomAdAutoRefresh() {
        return mainBottomAdAutoRefresh;
    }

    public static int getMainBottomAdAutoSecond() {
        return mainBottomAdAutoSecond;
    }

    public static boolean getMainBigTopAdShow() {
        return mainBigTopAdShow;
    }

    public static String getMainNativeId() {
        return mainNativeId == null ? "" : mainNativeId;
    }

    public static int getMainAdAutoSecond() {
        return mainAdAutoSecond;
    }

    public static boolean getSettingsFragmentNativeAdShow() {
        return settingsFragmentNativeAdShow;
    }

    public static String getSettingsFragmentNativeId() {
        return settingsFragmentNativeId == null ? "" : settingsFragmentNativeId;
    }

    public static int getSettingsFragmentNativeSecond() {
        return settingsFragmentNativeSecond;
    }

    public static boolean getAppOpenAdShow() {
        return appOpenAdShow;
    }

    public static boolean getAppOpenDialogShow() {
        return appOpenDialogShow;
    }

    public static int getAppOpenShowPerDay() {
        return appOpenShowPerDay;
    }

    public static boolean getInterAdsShow() {
        return interAdsShow;
    }

    public static int getInterAdsClick() {
        return interAdsClick;
    }

    @NonNull
    public static String getInterAdsClickType() {
        return interAdsClickType == null ? "load" : interAdsClickType;
    }

    public static boolean getInterAdsOnBack() {
        return interAdsOnBack;
    }

    public static int getInterAdsBackClick() {
        return interAdsBackClick;
    }

    @NonNull
    public static String getInterAdsBackClickType() {
        return interAdsBackClickType == null ? "load" : interAdsBackClickType;
    }

    @NonNull
    public static String getInterAdsId() {
        return interAdsId == null ? "" : interAdsId;
    }

    @NonNull
    public static String getInterBackAdsId() {
        return interBackAdsId == null ? "" : interBackAdsId;
    }

    public static boolean getBottomNavInterAdsShow() {
        return bottomNavInterAdsShow;
    }

    @NonNull
    public static String getBottomNavInterAdsType() {
        return bottomNavInterAdsType == null ? "load" : bottomNavInterAdsType;
    }

    @NonNull
    public static String getBottomNavInterAdsId() {
        return bottomNavInterAdsId == null ? "" : bottomNavInterAdsId;
    }

    public static int getBottomNavInterClick() {
        return bottomNavInterClick;
    }

    public static boolean anyScreenInterFlag(boolean back) {
        for (ScreenAdConfig config : screenAds.values()) {
            if (config != null && (back ? config.onBackInterShow : config.onClickInterShow)) {
                return true;
            }
        }
        return false;
    }

    public static boolean getRightSwipeInterstitialAdShow() {
        return rightSwipeInterstitialAdShow;
    }

    public static int getRightSwipeInterstitial() {
        return rightSwipeInterstitial;
    }

    @NonNull
    public static String getRightSwipeAdsType() {
        return rightSwipeAdsType == null || rightSwipeAdsType.trim().isEmpty() ? "load" : rightSwipeAdsType;
    }

    public static boolean isEventScreenEnabled(@Nullable String kind) {
        if ("charge_in".equals(kind)) {
            return eventChargeInShow;
        }
        if ("charge_out".equals(kind)) {
            return eventChargeOutShow;
        }
        return eventUninstallShow;
    }

    public static boolean getEventBottomAdsShow() {
        return eventBottomAdsShow;
    }

    @NonNull
    public static String getEventBottomAdsType() {
        return eventBottomAdsType == null || eventBottomAdsType.trim().isEmpty() ? "load" : eventBottomAdsType;
    }

    @NonNull
    public static String getEventBottomAdsView() {
        return eventBottomAdsView == null || eventBottomAdsView.trim().isEmpty() ? "native" : eventBottomAdsView;
    }

    @NonNull
    public static String getEventNativeId() {
        return eventNativeId == null ? "" : eventNativeId;
    }

    @NonNull
    public static String getEventBannerId() {
        return eventBannerId == null ? "" : eventBannerId;
    }

    public static int getEventScreenShowSeconds() {
        return eventScreenShowSeconds < 1 ? 6 : eventScreenShowSeconds;
    }

    public static boolean getEventBackAdsShow() {
        return eventBackAdsShow;
    }

    public static boolean getClEndScreenShow() {
        return clEndScreenShow;
    }

    public static boolean getClEndAdShow() {
        return clEndAdShow;
    }

    @NonNull
    public static String getClEndAdType() {
        return clEndAdType == null ? "" : clEndAdType;
    }

    @NonNull
    public static String getClEndBannerId() {
        return clEndBannerId == null ? "" : clEndBannerId;
    }

    @NonNull
    public static String getClEndNativeId() {
        return clEndNativeId == null ? "" : clEndNativeId;
    }

    public static boolean getClEndBackAdShow() {
        return clEndBackAdShow;
    }

    @NonNull
    public static String getClEndBackAdType() {
        return clEndBackAdType == null ? "" : clEndBackAdType;
    }

    @NonNull
    public static List<String[]> getClEndBackAdSequence() {
        List<String[]> copy = new ArrayList<>();
        if (clEndBackAdSequence == null) {
            return copy;
        }
        for (String[] step : clEndBackAdSequence) {
            if (step == null || step.length < 2) {
                continue;
            }
            copy.add(new String[]{step[0], step[1]});
        }
        return copy;
    }

    public static int getClEndBackAdShowAfterDay() {
        return clEndBackAdShowAfterDay;
    }

    public static int getClEndBackAdShowPerDay() {
        return clEndBackAdShowPerDay;
    }

    @NonNull
    public static String getClEndBackAdInterstitialId() {
        return clEndBackAdInterstitialId == null ? "" : clEndBackAdInterstitialId;
    }

    @NonNull
    public static String getClEndBackAdNativeId() {
        return clEndBackAdNativeId == null ? "" : clEndBackAdNativeId;
    }

    public static boolean getClEndBackAdCountryIP() {
        return clEndBackAdCountryIP;
    }

    @NonNull
    public static ArrayList<String> getClEndBackAdShowCountryList() {
        return clEndBackAdShowCountryList == null ? new ArrayList<>() : clEndBackAdShowCountryList;
    }

    public static int getNotificationInstallDays() {
        return notificationInstallDays;
    }

    public static int getNotificationCallInstallDays() {
        return notificationCallInstallDays;
    }

    public static int getNotificationCallOverlayInstallDays() {
        return notificationCallOverlayInstallDays;
    }

    public static boolean getAllAllowPermissionShowNotification() {
        return allAllowPermissionShowNotification;
    }

    public static boolean getNotificationBackAdShow() {
        return notificationBackAdShow;
    }

    public static boolean getNotificationCloseButtonShow() {
        return notificationCloseButtonShow;
    }

    @NonNull
    public static ArrayList<String> getNotificationCountryList() {
        return notificationCountryList == null ? new ArrayList<>() : notificationCountryList;
    }

    @NonNull
    public static ArrayList<String> getNotificationCallCountryList() {
        return notificationCallCountryList == null ? new ArrayList<>() : notificationCallCountryList;
    }

    @NonNull
    public static ArrayList<String> getNotificationCallOverlayCountryList() {
        return notificationCallOverlayCountryList == null ? new ArrayList<>() : notificationCallOverlayCountryList;
    }

    public static boolean getDefaultAppPopupShow() {
        return defaultAppPopupShow;
    }

    public static int getDefaultAppPopupCount() {
        return defaultAppPopupCount;
    }

    public static boolean getLauncherAppNativeAdShow() {
        return launcherAppNativeAdShow;
    }

    public static int getLauncherAppNativeAdShowPerDay() {
        return launcherAppNativeAdShowPerDay;
    }

    @NonNull
    public static String getLauncherAppNativeId() {
        return launcherAppNativeId == null ? "" : launcherAppNativeId;
    }

    public static boolean getLauncherAppQuizIconShow() {
        return launcherAppQuizIconShow;
    }

    public static int getLauncherAppQuizIconCount() {
        return launcherAppQuizIconCount;
    }

    @NonNull
    public static List<String> getQuizAppIconList() {
        return quizAppIconList;
    }

    @NonNull
    public static List<String> getQuizNativeMediaList() {
        return quizNativeMediaList;
    }

    @NonNull
    public static List<String> getQuizInterstitialMediaList() {
        return quizInterstitialMediaList;
    }

    @NonNull
    public static List<String> getQuizAppOpenMediaList() {
        return quizAppOpenMediaList;
    }

    @NonNull
    public static List<String> getQuizBannerTitleList() {
        return quizBannerTitleList;
    }

    @NonNull
    public static List<String> getQuizBannerDescriptionList() {
        return quizBannerDescriptionList;
    }

    @NonNull
    public static List<String> getQuizNativeTitleList() {
        return quizNativeTitleList;
    }

    @NonNull
    public static List<String> getQuizNativeDescriptionList() {
        return quizNativeDescriptionList;
    }

    @NonNull
    public static List<String> getQuizInterstitialTitleList() {
        return quizInterstitialTitleList;
    }

    @NonNull
    public static List<String> getQuizInterstitialDescriptionList() {
        return quizInterstitialDescriptionList;
    }

    @NonNull
    public static List<String> getQuizInterstitialRateList() {
        return quizInterstitialRateList;
    }

    @NonNull
    public static String getQuizButtonText() {
        return quizButtonText == null ? "" : quizButtonText.trim();
    }

    @NonNull
    public static List<String> getQuizLinkList() {
        return quizLinkList;
    }

    @NonNull
    public static List<String> getQuizLinkExcludeList() {
        return quizLinkExcludeList;
    }

    @NonNull
    public static List<String> getAppProxyListCity() {
        return appProxyListCity;
    }

    @NonNull
    public static List<String> getAppProxyListState() {
        return appProxyListState;
    }

    @NonNull
    public static List<String> getAppProxyListCountry() {
        return appProxyListCountry;
    }

    public static boolean isAppProxyCheckIp() {
        return appProxyCheckIp;
    }

    @NonNull
    public static String getAppProxyIpCheckerUrl() {
        return appProxyIpCheckerUrl == null ? "" : appProxyIpCheckerUrl;
    }

    public static void setActiveQuizLinkList(@NonNull List<String> sourceLinks) {
        activeQuizLinkList.clear();
        if (!sourceLinks.isEmpty()) {
            activeQuizLinkList.addAll(sourceLinks);
        }
    }

    @NonNull
    public static String pickQuizLink() {
        List<String> links = !activeQuizLinkList.isEmpty() ? activeQuizLinkList : quizLinkList;
        if (links.isEmpty()) {
            return "";
        }
        return links.get(new java.util.Random().nextInt(links.size()));
    }

    public static int getQuizSyncedItemCount() {
        int count = Integer.MAX_VALUE;
        List<List<String>> required = java.util.Arrays.asList(quizAppIconList, quizBannerTitleList, quizBannerDescriptionList, quizNativeTitleList, quizNativeDescriptionList, quizInterstitialTitleList, quizInterstitialDescriptionList);
        for (List<String> list : required) {
            if (list == null || list.isEmpty()) {
                return 0;
            }
            count = Math.min(count, list.size());
        }
        return count == Integer.MAX_VALUE ? 0 : Math.min(3, count);
    }

    @NonNull
    public static String quizItem(@Nullable List<String> list, int index) {
        if (list == null || list.isEmpty() || index < 0 || index >= list.size()) {
            return "";
        }
        return list.get(index);
    }

    private static void saveClEndConfig(@Nullable Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences.Editor editor = context.getApplicationContext().getSharedPreferences(CL_END_CONFIG_PREFS, Context.MODE_PRIVATE).edit();
        editor.putBoolean("clEndScreenShow", clEndScreenShow);
        editor.putBoolean("clEndAdShow", clEndAdShow);
        editor.putString("clEndAdType", clEndAdType == null ? "" : clEndAdType);
        editor.putString("clEndBannerId", clEndBannerId == null ? "" : clEndBannerId);
        editor.putString("clEndNativeId", clEndNativeId == null ? "" : clEndNativeId);
        editor.putBoolean("clEndBackAdShow", clEndBackAdShow);
        editor.putString("clEndBackAdType", clEndBackAdType == null ? "" : clEndBackAdType);
        editor.putString("clEndBackAdSequence", formatBackAdSequence(clEndBackAdSequence));
        editor.putInt("clEndBackAdShowAfterDay", clEndBackAdShowAfterDay);
        editor.putInt("clEndBackAdShowPerDay", clEndBackAdShowPerDay);
        editor.putString("clEndBackAdInterstitialId", clEndBackAdInterstitialId == null ? "" : clEndBackAdInterstitialId);
        editor.putString("clEndBackAdNativeId", clEndBackAdNativeId == null ? "" : clEndBackAdNativeId);
        editor.putBoolean("clEndBackAdCountryIP", clEndBackAdCountryIP);
        editor.putString("appOpenId", AdPlacement.getAppOpenId() == null ? "" : AdPlacement.getAppOpenId());
        editor.putString("nativeAdLabelColor", AdPlacement.getNativeAdLabelColor() == null ? "" : AdPlacement.getNativeAdLabelColor());
        editor.putString("nativeAdButtonColor", AdPlacement.getNativeAdButtonColor() == null ? "" : AdPlacement.getNativeAdButtonColor());
        editor.putString("clEndBackAdShowCountryList", formatCountryList(clEndBackAdShowCountryList));
        editor.putInt("notificationInstallDays", notificationInstallDays);
        editor.putInt("notificationCallInstallDays", notificationCallInstallDays);
        editor.putInt("notificationCallOverlayInstallDays", notificationCallOverlayInstallDays);
        editor.putBoolean("allAllowPermissionShowNotification", allAllowPermissionShowNotification);
        editor.putBoolean("notificationBackAdShow", notificationBackAdShow);
        editor.putBoolean("notificationCloseButtonShow", notificationCloseButtonShow);
        editor.putBoolean("eventUninstallShow", eventUninstallShow);
        editor.putBoolean("eventChargeInShow", eventChargeInShow);
        editor.putBoolean("eventChargeOutShow", eventChargeOutShow);
        editor.putBoolean("eventBottomAdsShow", eventBottomAdsShow);
        editor.putString("eventBottomAdsType", eventBottomAdsType == null ? "load" : eventBottomAdsType);
        editor.putString("eventBottomAdsView", eventBottomAdsView == null ? "native" : eventBottomAdsView);
        editor.putString("eventNativeId", eventNativeId == null ? "" : eventNativeId);
        editor.putString("eventBannerId", eventBannerId == null ? "" : eventBannerId);
        editor.putInt("eventScreenShowSeconds", eventScreenShowSeconds);
        editor.putBoolean("eventBackAdsShow", eventBackAdsShow);
        editor.putBoolean("rightSwipeInterstitialAdShow", rightSwipeInterstitialAdShow);
        editor.putInt("rightSwipeInterstitial", rightSwipeInterstitial);
        editor.putString("rightSwipeAdsType", rightSwipeAdsType == null ? "load" : rightSwipeAdsType);
        editor.putString("notificationCountryList", formatCountryList(notificationCountryList));
        editor.putString("notificationCallCountryList", formatCountryList(notificationCallCountryList));
        editor.putString("notificationCallOverlayCountryList", formatCountryList(notificationCallOverlayCountryList));
        editor.apply();
    }

    public static final class ScreenAdConfig {
        public final boolean show;
        public final String type;
        public final String bannerId;
        public final String nativeId;
        public final boolean onBackInterShow;
        public final boolean onClickInterShow;

        ScreenAdConfig(boolean show, String type, String bannerId, String nativeId, boolean onBackInterShow, boolean onClickInterShow) {
            this.show = show;
            this.type = type == null ? "banner" : type;
            this.bannerId = bannerId == null ? "" : bannerId;
            this.nativeId = nativeId == null ? "" : nativeId;
            this.onBackInterShow = onBackInterShow;
            this.onClickInterShow = onClickInterShow;
        }
    }

    private static final String[] SCREEN_AD_KEYS = {
            "ScanResultScreen", "ScanResult",
            "CreateHubScreen", "CreateHub",
            "QrFormScreen", "QrForm",
            "QrCustomizationScreen", "QrCustomization",
            "QrPreviewScreen", "QrPreview",
            "HistoryScreen", "History",
            "HistoryDetailScreen", "HistoryDetail",
            "LauncherSettingsScreen", "LauncherSettings",
            "OtherScreen", "Other"
    };

    private static final HashMap<String, ScreenAdConfig> screenAds = new HashMap<>();

    @Nullable
    public static ScreenAdConfig getScreenAd(@Nullable String screenKey) {
        if (screenKey == null) {
            return null;
        }
        return screenAds.get(screenKey);
    }

    private static void applyScreenAds(JSONObject screenObject) {
        screenAds.clear();
        for (int i = 0; i < SCREEN_AD_KEYS.length; i += 2) {
            String objectName = SCREEN_AD_KEYS[i];
            String prefix = SCREEN_AD_KEYS[i + 1];
            JSONObject screen = child(screenObject, objectName);
            screenAds.put(objectName, new ScreenAdConfig(
                    screen.optBoolean(prefix + "_Ad_Show", false),
                    screen.optString(prefix + "_Ad_Type", "banner"),
                    screen.optString(prefix + "_Banner_Id", ""),
                    screen.optString(prefix + "_Native_Id", ""),
                    screen.optBoolean("on_back_inter_ads_show", false),
                    screen.optBoolean("on_click_inter_ads_show", false)
            ));
        }
    }

    private static void rememberInterFlags(String key, JSONObject screen) {
        screenAds.put(key, new ScreenAdConfig(
                false,
                "banner",
                "",
                "",
                screen.optBoolean("on_back_inter_ads_show", false),
                screen.optBoolean("on_click_inter_ads_show", false)
        ));
    }

    private static void applyInterAds(@Nullable JSONObject interAds) {
        JSONObject value = interAds == null ? new JSONObject() : interAds;
        interAdsShow = value.optBoolean("inter_ads_show", false);
        interAdsClick = value.optInt("inter_ads_click", 0);
        interAdsClickType = value.optString("inter_ads_click_type", "load");
        interAdsOnBack = value.optBoolean("inter_ads_on_back", false);
        interAdsBackClick = value.optInt("inter_ads_back_click", 0);
        interAdsBackClickType = value.optString("inter_ads_back_click_type", "load");
        interAdsId = value.optString("inter_ads_id", "");
        interBackAdsId = value.optString("inter_back_ads_id", "");
    }

    @NonNull
    private static String optStringIgnoreCase(JSONObject object, String key, String fallback) {
        if (object.has(key)) {
            return object.optString(key, fallback);
        }
        JSONArray names = object.names();
        if (names == null) {
            return fallback;
        }
        for (int i = 0; i < names.length(); i++) {
            String name = names.optString(i, "");
            if (key.equalsIgnoreCase(name)) {
                return object.optString(name, fallback);
            }
        }
        return fallback;
    }

    private static JSONObject child(JSONObject screen, String key) {
        JSONObject value = screen.optJSONObject(key);
        return value == null ? new JSONObject() : value;
    }

    private static ArrayList<String> parseStoredCountryList(@Nullable String countries) {
        ArrayList<String> countryList = new ArrayList<>();
        if (countries == null || countries.isEmpty()) {
            return countryList;
        }
        String[] parts = countries.split(",");
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                countryList.add(part.trim().toLowerCase());
            }
        }
        return countryList;
    }

    private static ArrayList<String[]> parseBackAdSequence(@Nullable JSONArray sequence) {
        ArrayList<String[]> steps = new ArrayList<>();
        if (sequence == null) {
            return steps;
        }
        for (int i = 0; i < sequence.length(); i++) {
            JSONArray pair = sequence.optJSONArray(i);
            if (pair == null || pair.length() < 2) {
                continue;
            }
            String type = normalizeBackAdFormat(pair.optString(0, ""));
            int count = parsePositiveCount(pair.opt(1));
            if (type.isEmpty() || count <= 0) {
                continue;
            }
            steps.add(new String[]{type, Integer.toString(count)});
        }
        return steps;
    }

    private static ArrayList<String[]> parseStoredBackAdSequence(@Nullable String stored) {
        ArrayList<String[]> steps = new ArrayList<>();
        if (stored == null || stored.trim().isEmpty()) {
            return steps;
        }
        String[] parts = stored.split(";");
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            String[] pair = part.split(",", 2);
            if (pair.length < 2) {
                continue;
            }
            String type = normalizeBackAdFormat(pair[0]);
            int count = parsePositiveCount(pair[1]);
            if (type.isEmpty() || count <= 0) {
                continue;
            }
            steps.add(new String[]{type, Integer.toString(count)});
        }
        return steps;
    }

    @NonNull
    private static String formatBackAdSequence(@Nullable ArrayList<String[]> sequence) {
        if (sequence == null || sequence.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String[] step : sequence) {
            if (step == null || step.length < 2) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(step[0]).append(',').append(step[1]);
        }
        return builder.toString();
    }

    @NonNull
    private static String normalizeBackAdFormat(@Nullable String type) {
        if (type == null) {
            return "";
        }
        String value = type.trim().toLowerCase(Locale.US);
        if ("inter".equals(value) || "appopen".equals(value) || "native".equals(value)) {
            return value;
        }
        return "";
    }

    private static int parsePositiveCount(@Nullable Object value) {
        if (value == null || value == JSONObject.NULL) {
            return 0;
        }
        try {
            int count;
            if (value instanceof Number) {
                count = ((Number) value).intValue();
            } else {
                String raw = String.valueOf(value).trim();
                if (raw.isEmpty()) {
                    return 0;
                }
                count = Integer.parseInt(raw);
            }
            return count > 0 ? count : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static ArrayList<String> parseCountryArray(@Nullable JSONArray countryArray) {
        ArrayList<String> countryList = new ArrayList<>();
        if (countryArray != null) {
            for (int i = 0; i < countryArray.length(); i++) {
                countryList.add(countryArray.optString(i, "").toLowerCase());
            }
        }
        return countryList;
    }

    private static String formatCountryList(@Nullable ArrayList<String> countryList) {
        if (countryList == null || countryList.isEmpty()) {
            return "";
        }
        StringBuilder countries = new StringBuilder();
        for (int i = 0; i < countryList.size(); i++) {
            if (i > 0) {
                countries.append(",");
            }
            countries.append(countryList.get(i));
        }
        return countries.toString();
    }

    private static void replaceIfPresent(List<String> target, JSONObject jsonObject, String key) {
        List<String> parsed = parseStringList(jsonObject, key);
        if (!parsed.isEmpty()) {
            target.clear();
            target.addAll(parsed);
        }
    }

    private static List<String> parseStringList(@Nullable JSONObject jsonObject, String key) {
        List<String> items = new ArrayList<>();
        if (jsonObject == null || !jsonObject.has(key)) {
            return items;
        }
        Object value = jsonObject.opt(key);
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                String item = array.optString(i, "").trim();
                if (!item.isEmpty()) {
                    items.add(item);
                }
            }
            return items;
        }
        String raw = jsonObject.optString(key, "");
        if (raw.trim().isEmpty()) {
            return items;
        }
        String[] parts = raw.split("\\|", -1);
        for (String part : parts) {
            items.add(part == null ? "" : part.trim());
        }
        return items;
    }
}
