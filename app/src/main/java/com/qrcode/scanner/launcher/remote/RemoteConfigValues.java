package com.qrcode.scanner.launcher.remote;

import android.content.Context;
import android.content.SharedPreferences;

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
    private static boolean createHubTopAdShow;

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

        applyScreenAds(screenObject);
        rememberInterFlags("MainScreen", mainScreen);

        JSONObject settingsFragmentScreen = child(screenObject, "SettingsFragmentScreen");
        settingsFragmentNativeAdShow = settingsFragmentScreen.optBoolean("SettingsFragment_Native_Ad_Show", false);
        settingsFragmentNativeId = settingsFragmentScreen.optString("SettingsFragment_Native_Id", "");
        settingsFragmentNativeSecond = settingsFragmentScreen.optInt("SettingsFragment_Native_Second", 0);
        rememberInterFlags("SettingsFragmentScreen", settingsFragmentScreen);

        JSONObject createHubScreen = child(screenObject, "CreateHubScreen");
        createHubTopAdShow = createHubScreen.optBoolean("CreateHub_top_Ad_Show", createHubScreen.optBoolean("CreateHub_Top_Ad_Show", false));

        // Welcome back screen (instead of the App Open ad on return) + Coming_Soon.
        if (context != null) {
            JSONObject welcomeBlock = screenObject.optJSONObject("welcome_back_screen");
            if (welcomeBlock == null) {
                welcomeBlock = screenObject.optJSONObject("WelcomeBackScreen");
            }
            if (welcomeBlock == null) {
                // Also accepted at the profile root, next to "Screen".
                welcomeBlock = root.optJSONObject("welcome_back_screen");
            }
            com.qrcode.scanner.launcher.common.WelcomeBackConfig.apply(context, welcomeBlock, root);
            com.qrcode.scanner.launcher.common.ExitDialogConfig.apply(context, root);
        }

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

        com.qrcode.scanner.launcher.common.ScreenInterAds.onConfigApplied(context);
        com.qrcode.scanner.launcher.helpers.AppProxyLookup.refreshActiveQuizLinks();
        // Common ads location rule. A missing key means an empty list, so removing it in Firebase clears the rule.
        com.qrcode.scanner.launcher.common.AdGates.onConfigApplied(
                context,
                readLocationList(root, screenObject, "Ads_Show_countryList"),
                readLocationList(root, screenObject, "Ads_show_List_Exclude"));
    }

    /** Reads a JSON array of strings, or one string with "|" separators, from the profile root (then the screen block). */
    @NonNull
    private static List<String> readLocationList(@NonNull JSONObject root, @NonNull JSONObject screen, @NonNull String key) {
        JSONObject source = root.has(key) ? root : screen;
        List<String> items = new ArrayList<>();
        for (String item : parseStringList(source, key)) {
            String cleaned = item == null ? "" : item.trim().toLowerCase(Locale.ROOT);
            if (!cleaned.isEmpty() && !items.contains(cleaned)) {
                items.add(cleaned);
            }
        }
        return items;
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

    /** CreateHubScreen.CreateHub_top_Ad_Show: Big native at the top of the Create hub. */
    public static boolean getCreateHubTopAdShow() {
        return createHubTopAdShow;
    }

    /** The Create hub's own native id (CreateHubScreen.CreateHub_Native_Id). */
    public static String getCreateHubNativeId() {
        ScreenAdConfig config = getScreenAd("CreateHubScreen");
        return config == null || config.nativeId == null ? "" : config.nativeId;
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
