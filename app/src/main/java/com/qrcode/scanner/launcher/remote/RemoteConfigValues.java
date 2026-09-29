package com.qrcode.scanner.launcher.remote;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qrcode.scanner.launcher.common.AdPlacement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Source Remote Config values that are not already applied to the migrated ad slots.
 * App-proxy lookup starts after these values are stored and does not block startup.
 */
public final class RemoteConfigValues {
    private static final String CL_END_CONFIG_PREFS = "cl_end_config_preferences";

    private static boolean appOpenAdShow;
    private static boolean appOpenDialogShow;
    private static int appOpenShowPerDay;
    private static int interstitialClick;
    private static boolean rightSwipeInterstitialAdShow;
    private static int rightSwipeInterstitial;
    private static boolean clEndConfigLoaded;
    private static boolean clEndScreenShow;
    private static boolean defaultAppPopupShow;
    private static int defaultAppPopupCount;
    private static boolean otherInterstitialAdShow;

    private static boolean mainAdShow;
    private static String mainAdType = "banner";
    private static String mainBannerId = "";
    private static String mainNativeId = "";
    private static boolean mainAdAutoRefresh;
    private static int mainAdAutoSecond;

    private static boolean createFragmentNativeAdShow;
    private static String createFragmentNativeId = "";
    private static int createFragmentNativeSecond;

    private static boolean generalAdShow;
    private static String generalAdType = "banner";
    private static String generalBannerId = "";
    private static String generalNativeId = "";
    private static boolean generalInterstitialAdShow;
    private static String generalInterstitialId = "";

    private static boolean socialAdShow;
    private static String socialAdType = "banner";
    private static String socialBannerId = "";
    private static String socialNativeId = "";
    private static boolean socialInterstitialAdShow;
    private static String socialInterstitialId = "";

    private static boolean barcodeAdShow;
    private static String barcodeAdType = "banner";
    private static String barcodeBannerId = "";
    private static String barcodeNativeId = "";
    private static boolean barcodeInterstitialAdShow;
    private static String barcodeInterstitialId = "";

    private static boolean settingsFragmentNativeAdShow;
    private static String settingsFragmentNativeId = "";
    private static int settingsFragmentNativeSecond;

    private static boolean launcherAppNativeAdShow;
    private static int launcherAppNativeAdShowPerDay;
    private static String launcherAppNativeId = "";
    private static boolean launcherAppQuizIconShow;
    private static int launcherAppQuizIconCount;

    private static boolean clEndAdShow;
    private static String clEndAdType = "banner";
    private static String clEndBannerId = "";
    private static String clEndNativeId = "";
    private static boolean clEndBackAdShow;
    private static String clEndBackAdType = "inter";
    private static int clEndBackAdShowAfterDay;
    private static int clEndBackAdShowPerDay;
    private static String clEndBackAdInterstitialId = "";
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
        interstitialClick = root.optInt("Interstitial_Click", 0);
        rightSwipeInterstitialAdShow = root.optBoolean("Right_Swipe_Interstitial_Ad_Show", false);
        rightSwipeInterstitial = root.optInt("Right_Swipe_Interstitial", 0);
        clEndScreenShow = root.optBoolean("Cl_End_Screen_Show", false);
        defaultAppPopupShow = root.optBoolean("Default_App_Popup_Show", false);
        defaultAppPopupCount = root.optInt("Default_App_Popup_Count", 0);

        JSONObject mainScreen = child(screenObject, "MainScreen");
        mainAdShow = mainScreen.optBoolean("Main_Ad_Show", false);
        mainAdType = mainScreen.optString("Main_Ad_Type", "banner");
        mainBannerId = mainScreen.optString("Main_Banner_Id", "");
        mainNativeId = mainScreen.optString("Main_Native_Id", "");
        mainAdAutoRefresh = mainScreen.optBoolean("Main_Ad_Auto_Refresh", false);
        mainAdAutoSecond = mainScreen.optInt("Main_Ad_Auto_Second", 0);

        JSONObject createFragmentScreen = child(screenObject, "CreateFragmentScreen");
        createFragmentNativeAdShow = createFragmentScreen.optBoolean("CreateFragment_Native_Ad_Show", false);
        createFragmentNativeId = createFragmentScreen.optString("CreateFragment_Native_Id", "");
        createFragmentNativeSecond = createFragmentScreen.optInt("CreateFragment_Native_Second", 0);

        JSONObject generalScreen = child(screenObject, "GeneralScreen");
        generalAdShow = generalScreen.optBoolean("General_Ad_Show", false);
        generalAdType = generalScreen.optString("General_Ad_Type", "banner");
        generalBannerId = generalScreen.optString("General_Banner_Id", "");
        generalNativeId = generalScreen.optString("General_Native_Id", "");
        generalInterstitialAdShow = generalScreen.optBoolean("General_Interstitial_Ad_Show", false);
        generalInterstitialId = generalScreen.optString("General_Interstitial_Id", "");

        JSONObject socialScreen = child(screenObject, "SocialScreen");
        socialAdShow = socialScreen.optBoolean("Social_Ad_Show", false);
        socialAdType = socialScreen.optString("Social_Ad_Type", "banner");
        socialBannerId = socialScreen.optString("Social_Banner_Id", "");
        socialNativeId = socialScreen.optString("Social_Native_Id", "");
        socialInterstitialAdShow = socialScreen.optBoolean("Social_Interstitial_Ad_Show", false);
        socialInterstitialId = socialScreen.optString("Social_Interstitial_Id", "");

        JSONObject barcodeScreen = child(screenObject, "BarcodeScreen");
        barcodeAdShow = barcodeScreen.optBoolean("Barcode_Ad_Show", false);
        barcodeAdType = barcodeScreen.optString("Barcode_Ad_Type", "banner");
        barcodeBannerId = barcodeScreen.optString("Barcode_Banner_Id", "");
        barcodeNativeId = barcodeScreen.optString("Barcode_Native_Id", "");
        barcodeInterstitialAdShow = barcodeScreen.optBoolean("Barcode_Interstitial_Ad_Show", false);
        barcodeInterstitialId = barcodeScreen.optString("Barcode_Interstitial_Id", "");

        JSONObject settingsFragmentScreen = child(screenObject, "SettingsFragmentScreen");
        settingsFragmentNativeAdShow = settingsFragmentScreen.optBoolean("SettingsFragment_Native_Ad_Show", false);
        settingsFragmentNativeId = settingsFragmentScreen.optString("SettingsFragment_Native_Id", "");
        settingsFragmentNativeSecond = settingsFragmentScreen.optInt("SettingsFragment_Native_Second", 0);

        JSONObject otherScreen = child(screenObject, "OtherScreen");
        otherInterstitialAdShow = otherScreen.optBoolean("Other_Interstitial_Ad_Show", false);

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
        clEndBackAdType = clEndScreen.optString("ClEnd_Back_Ad_Type", "inter");
        clEndBackAdShowAfterDay = clEndScreen.optInt("ClEnd_Back_Ad_Show_After_Day", 0);
        clEndBackAdShowPerDay = clEndScreen.optInt("ClEnd_Back_Ad_Show_Per_Day", 0);
        clEndBackAdInterstitialId = clEndScreen.optString("ClEnd_Back_Ad_Interstitial_Id", "");
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
        clEndBackAdShowAfterDay = preferences.getInt("clEndBackAdShowAfterDay", 0);
        clEndBackAdShowPerDay = preferences.getInt("clEndBackAdShowPerDay", 0);
        clEndBackAdInterstitialId = preferences.getString("clEndBackAdInterstitialId", "");
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
        clEndConfigLoaded = true;
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

    public static int getInterstitialClick() {
        return interstitialClick;
    }

    public static boolean getRightSwipeInterstitialAdShow() {
        return rightSwipeInterstitialAdShow;
    }

    public static int getRightSwipeInterstitial() {
        return rightSwipeInterstitial;
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

    public static boolean getOtherInterstitialAdShow() {
        return otherInterstitialAdShow;
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
        editor.putInt("clEndBackAdShowAfterDay", clEndBackAdShowAfterDay);
        editor.putInt("clEndBackAdShowPerDay", clEndBackAdShowPerDay);
        editor.putString("clEndBackAdInterstitialId", clEndBackAdInterstitialId == null ? "" : clEndBackAdInterstitialId);
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
        editor.putString("notificationCountryList", formatCountryList(notificationCountryList));
        editor.putString("notificationCallCountryList", formatCountryList(notificationCallCountryList));
        editor.putString("notificationCallOverlayCountryList", formatCountryList(notificationCallOverlayCountryList));
        editor.apply();
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
