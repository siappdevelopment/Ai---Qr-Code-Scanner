package com.qrcode.scanner.launcher.remote;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.qrcode.scanner.launcher.common.AppUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Screen-list state taken from the source AdPlacement helpers.
 * Ad unit ids and ad flags are not stored.
 */
public final class ScreenFlowConfig {
    public static final String SCREEN_LANGUAGE = "Language";
    public static final String SCREEN_COLLECTION = "Collection";
    public static final String SCREEN_PERMISSION = "Permission";
    public static final String SCREEN_DEFAULT_HOME = "DefaultHome";
    public static final String SCREEN_DEFAULT_SETTING_HOME = "DefultSettingHome";
    public static final String SCREEN_INTRO = "Intro";

    private static final String PREF_SHOW_SCREEN_FLOW = "show_screen_flow";

    private static List<String> showScreenFlow = new ArrayList<>();
    private static int introScreenCount;
    private static boolean redirectHomeLauncher;
    private static String privacyPolicy = "";

    private ScreenFlowConfig() {
    }

    public static int getIntroScreenCount() {
        return introScreenCount;
    }

    public static void setIntroScreenCount(int count) {
        introScreenCount = count;
    }

    public static boolean getRedirectHomeLauncher() {
        return redirectHomeLauncher;
    }

    public static void setRedirectHomeLauncher(boolean redirect) {
        redirectHomeLauncher = redirect;
    }

    public static String getPrivacyPolicy() {
        return privacyPolicy;
    }

    public static void setPrivacyPolicy(String url) {
        privacyPolicy = url == null ? "" : url;
    }

    public static List<String> getShowScreenFlow() {
        if (showScreenFlow == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(showScreenFlow);
    }

    public static void setShowScreenFlow(List<String> flow) {
        showScreenFlow = flow == null ? new ArrayList<>() : new ArrayList<>(flow);
    }

    public static List<String> getDefaultShowScreenFlow() {
        List<String> defaults = new ArrayList<>();
        defaults.add(SCREEN_LANGUAGE);
        defaults.add(SCREEN_COLLECTION);
        defaults.add(SCREEN_PERMISSION);
        defaults.add(SCREEN_DEFAULT_HOME);
        defaults.add(SCREEN_INTRO);
        return defaults;
    }

    public static void ensureShowScreenFlow(Context context) {
        if (!getShowScreenFlow().isEmpty()) {
            return;
        }
        if (context != null) {
            restoreShowScreenFlow(context);
        }
        if (getShowScreenFlow().isEmpty()) {
            setShowScreenFlow(getDefaultShowScreenFlow());
            if (context != null) {
                cacheShowScreenFlow(context);
            }
        }
    }

    public static List<String> parseShowScreenFlow(Object rawFlow) {
        JSONArray array = toJsonArray(rawFlow);
        if (array == null) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String screen = normalizeScreenName(array.optString(i, ""));
            if (!screen.isEmpty() && !result.contains(screen)) {
                result.add(screen);
            }
        }
        return result;
    }

    public static void applyShowScreenFlowFromConfig(@Nullable JSONObject jsonObject, String topLevelFlowJson) {
        List<String> nestedFlow = new ArrayList<>();
        if (jsonObject != null && jsonObject.has("Show_Screen_Flow")) {
            nestedFlow = parseShowScreenFlow(jsonObject.opt("Show_Screen_Flow"));
        }
        List<String> topLevelFlow = parseShowScreenFlow(topLevelFlowJson);
        if (!nestedFlow.isEmpty()) {
            setShowScreenFlow(nestedFlow);
            return;
        }
        if (!topLevelFlow.isEmpty()) {
            setShowScreenFlow(topLevelFlow);
            return;
        }
        if (getShowScreenFlow().isEmpty()) {
            setShowScreenFlow(getDefaultShowScreenFlow());
        }
    }

    public static void cacheShowScreenFlow(Context context) {
        if (context == null) {
            return;
        }
        JSONArray array = new JSONArray();
        for (String screen : getShowScreenFlow()) {
            array.put(screen);
        }
        String newFlowJson = array.toString();
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_SHOW_SCREEN_FLOW, MODE_PRIVATE);
        String oldFlowJson = prefs.getString("flow", "");
        if (prefs.getBoolean("cached", false) && !oldFlowJson.isEmpty() && !oldFlowJson.equals(newFlowJson)) {
            if (!AppUtils.hasCompletedOnboarding(context)) {
                AppUtils.resetOnboardingScreenCompletions(context);
            }
        }
        prefs.edit().putBoolean("cached", true).putString("flow", newFlowJson).apply();
    }

    public static void restoreShowScreenFlow(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_SHOW_SCREEN_FLOW, MODE_PRIVATE);
        if (!prefs.getBoolean("cached", false)) {
            return;
        }
        List<String> restored = parseShowScreenFlow(prefs.getString("flow", "[]"));
        if (!restored.isEmpty()) {
            setShowScreenFlow(restored);
        }
    }

    public static boolean isSupportedScreen(String screen) {
        return SCREEN_LANGUAGE.equalsIgnoreCase(screen)
                || SCREEN_COLLECTION.equalsIgnoreCase(screen)
                || SCREEN_PERMISSION.equalsIgnoreCase(screen)
                || SCREEN_DEFAULT_HOME.equalsIgnoreCase(screen)
                || SCREEN_DEFAULT_SETTING_HOME.equalsIgnoreCase(screen)
                || SCREEN_INTRO.equalsIgnoreCase(screen);
    }

    private static JSONArray toJsonArray(Object rawFlow) {
        if (rawFlow == null) {
            return null;
        }
        try {
            if (rawFlow instanceof JSONArray) {
                return (JSONArray) rawFlow;
            }
            String value = String.valueOf(rawFlow).trim();
            if (value.startsWith("\uFEFF")) {
                value = value.substring(1).trim();
            }
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1).replace("\\\"", "\"").trim();
            }
            if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
                return null;
            }
            if (value.startsWith("[")) {
                return new JSONArray(value);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String normalizeScreenName(String screen) {
        if (screen == null) {
            return "";
        }
        String value = screen.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (value.equalsIgnoreCase(SCREEN_LANGUAGE)) {
            return SCREEN_LANGUAGE;
        }
        if (value.equalsIgnoreCase(SCREEN_COLLECTION)) {
            return SCREEN_COLLECTION;
        }
        if (value.equalsIgnoreCase(SCREEN_PERMISSION)) {
            return SCREEN_PERMISSION;
        }
        if (value.equalsIgnoreCase(SCREEN_DEFAULT_HOME) || value.equalsIgnoreCase("Default") || value.equalsIgnoreCase("DefaultApp") || value.equalsIgnoreCase("Default_Home")) {
            return SCREEN_DEFAULT_HOME;
        }
        if (value.equalsIgnoreCase(SCREEN_DEFAULT_SETTING_HOME) || value.equalsIgnoreCase("DefaultSettingHome")) {
            return SCREEN_DEFAULT_SETTING_HOME;
        }
        if (value.equalsIgnoreCase(SCREEN_INTRO) || value.equalsIgnoreCase("IntroActivity") || value.equalsIgnoreCase("IntroScreen") || value.equalsIgnoreCase("Onboarding")) {
            return SCREEN_INTRO;
        }
        return value;
    }
}
