// RemoteConfigValues - fields + parsing (the Charging half is the same code with the other key)
    public static final String EVENT_SCREEN_CHARGING = "charging";
    public static final String EVENT_SCREEN_INSTALL_UNINSTALL = "install_uninstall";
    private static final String EVENT_SCREEN_AD_PREFS = "event_screen_ad_preferences";

    public static final class EventScreenConfig {
        public boolean chargeInShow;
        public boolean chargeOutShow;
        public boolean installShow;
        public boolean uninstallShow;
        public boolean bottomAdShow;
        public String adLoadType = "load";
        public String bottomAdType = "native";
        public String nativeId = "";
        public String bannerId = "";
        public int buttonShowSec = 6;
        public boolean backAdShow;
        public ArrayList<String[]> backAdSequence = new ArrayList<>();
        public String interId = "";
        public String fullNativeId = "";
        public int backAdDayCount;
        public int backAdTotalShowCount;
        public ArrayList<String> backAdCountryList = new ArrayList<>();
    }

    private static final EventScreenConfig chargingScreenConfig = new EventScreenConfig();
    private static final EventScreenConfig installUninstallScreenConfig = new EventScreenConfig();

        JSONObject eventScreen = screenObject;
        if (!eventScreen.has("charging_screen") && root.has("charging_screen")) {
            eventScreen = root;
        }
        applyEventScreen(context, chargingScreenConfig, child(eventScreen, "charging_screen"), "charging_screen", EVENT_SCREEN_CHARGING);
        applyEventScreen(context, installUninstallScreenConfig, child(eventScreen, "install_uninstall_screen"), "install_uninstall_screen", EVENT_SCREEN_INSTALL_UNINSTALL);

    public static boolean isEventScreenEnabled(@Nullable String kind) {
        if ("charge_in".equals(kind)) {
            return chargingScreenConfig.chargeInShow;
        }
        if ("charge_out".equals(kind)) {
            return chargingScreenConfig.chargeOutShow;
        }
        if ("install".equals(kind)) {
            return installUninstallScreenConfig.installShow;
        }
        return installUninstallScreenConfig.uninstallShow;
    }
    public static String eventScreenKeyForKind(@Nullable String kind) {
        if ("charge_in".equals(kind) || "charge_out".equals(kind)) {
            return EVENT_SCREEN_CHARGING;
        }
        return EVENT_SCREEN_INSTALL_UNINSTALL;
    }
    public static EventScreenConfig getEventScreenConfig(@Nullable String screenKey) {
        return EVENT_SCREEN_CHARGING.equals(screenKey) ? chargingScreenConfig : installUninstallScreenConfig;
    }
    private static void saveEventScreenConfig(@NonNull SharedPreferences.Editor editor, @NonNull EventScreenConfig config, @NonNull String prefix) {
        editor.putBoolean(prefix + "ChargeInShow", config.chargeInShow);
        editor.putBoolean(prefix + "ChargeOutShow", config.chargeOutShow);
        editor.putBoolean(prefix + "InstallShow", config.installShow);
        editor.putBoolean(prefix + "UninstallShow", config.uninstallShow);
        editor.putBoolean(prefix + "BottomAdShow", config.bottomAdShow);
        editor.putString(prefix + "AdLoadType", config.adLoadType == null ? "load" : config.adLoadType);
        editor.putString(prefix + "BottomAdType", config.bottomAdType == null ? "native" : config.bottomAdType);
        editor.putString(prefix + "NativeId", config.nativeId == null ? "" : config.nativeId);
        editor.putString(prefix + "BannerId", config.bannerId == null ? "" : config.bannerId);
        editor.putInt(prefix + "ButtonShowSec", config.buttonShowSec);
        editor.putBoolean(prefix + "BackAdShow", config.backAdShow);
        editor.putString(prefix + "BackAdSequence", formatBackAdSequence(config.backAdSequence));
        editor.putString(prefix + "InterId", config.interId == null ? "" : config.interId);
        editor.putString(prefix + "FullNativeId", config.fullNativeId == null ? "" : config.fullNativeId);
        editor.putInt(prefix + "BackAdDayCount", config.backAdDayCount);
        editor.putInt(prefix + "BackAdTotalShowCount", config.backAdTotalShowCount);
        editor.putString(prefix + "BackAdCountryList", formatCountryList(config.backAdCountryList));
    }
    private static void restoreEventScreenConfig(@NonNull SharedPreferences preferences, @NonNull EventScreenConfig config, @NonNull String prefix) {
        config.chargeInShow = preferences.getBoolean(prefix + "ChargeInShow", false);
        config.chargeOutShow = preferences.getBoolean(prefix + "ChargeOutShow", false);
        config.installShow = preferences.getBoolean(prefix + "InstallShow", false);
        config.uninstallShow = preferences.getBoolean(prefix + "UninstallShow", false);
        config.bottomAdShow = preferences.getBoolean(prefix + "BottomAdShow", false);
        config.adLoadType = preferences.getString(prefix + "AdLoadType", "load");
        config.bottomAdType = preferences.getString(prefix + "BottomAdType", "native");
        config.nativeId = preferences.getString(prefix + "NativeId", "");
        config.bannerId = preferences.getString(prefix + "BannerId", "");
        config.buttonShowSec = preferences.getInt(prefix + "ButtonShowSec", 6);
        config.backAdShow = preferences.getBoolean(prefix + "BackAdShow", false);
        config.backAdSequence = parseStoredEventBackAdSequence(preferences.getString(prefix + "BackAdSequence", ""));
        config.interId = preferences.getString(prefix + "InterId", "");
        config.fullNativeId = preferences.getString(prefix + "FullNativeId", "");
        config.backAdDayCount = preferences.getInt(prefix + "BackAdDayCount", 0);
        config.backAdTotalShowCount = preferences.getInt(prefix + "BackAdTotalShowCount", 0);
        config.backAdCountryList = parseStoredCountryList(preferences.getString(prefix + "BackAdCountryList", ""));
    }
    private static void applyEventScreen(@Nullable Context context, @NonNull EventScreenConfig config,
                                         @NonNull JSONObject json, @NonNull String prefix, @NonNull String screenKey) {
        config.chargeInShow = json.optBoolean("charge_in_screen_show", false);
        config.chargeOutShow = json.optBoolean("charge_out_screen_show", false);
        config.installShow = json.optBoolean("install_screen_show", false);
        config.uninstallShow = json.optBoolean("uninstall_screen_show", false);
        config.bottomAdShow = json.optBoolean(prefix + "_bottom_ad_show", false);
        config.adLoadType = json.optString(prefix + "_ad_load_type", "load").trim();
        config.bottomAdType = json.optString(prefix + "_bottom_ad_type", "native").trim();
        config.nativeId = json.optString(prefix + "_native_id", "");
        config.bannerId = json.optString(prefix + "_banner_id", "");
        config.buttonShowSec = json.optInt(prefix + "_button_show_sec", 6);
        config.backAdShow = json.optBoolean(prefix + "_back_ad_show", false);
        String previousSequence = formatBackAdSequence(config.backAdSequence);
        config.backAdSequence = parseEventBackAdSequence(json.optJSONArray(prefix + "_back_ad_sequence"));
        config.interId = json.optString(prefix + "_inter_id", "");
        config.fullNativeId = json.optString(prefix + "_full_native_id", "");
        config.backAdDayCount = json.optInt(prefix + "_back_ad_day_count", 0);
        config.backAdTotalShowCount = json.optInt(prefix + "_back_ad_total_show_count", 0);
        config.backAdCountryList = parseCountryArray(json.optJSONArray(prefix + "_back_ads_show_country"));
        if (context != null && !previousSequence.equals(formatBackAdSequence(config.backAdSequence))) {
            context.getApplicationContext()
                    .getSharedPreferences(EVENT_SCREEN_AD_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(screenKey + "_back_ad_sequence_cursor", 0L)
                    .apply();
        }
    }
    private static ArrayList<String[]> parseEventBackAdSequence(@Nullable JSONArray sequence) {
        ArrayList<String[]> steps = new ArrayList<>();
        if (sequence == null) {
            return steps;
        }
        for (int i = 0; i < sequence.length(); i++) {
            JSONArray pair = sequence.optJSONArray(i);
            if (pair != null) {
                if (pair.length() < 2) {
                    continue;
                }
                String type = pair.optString(0, "").trim();
                int count = parsePositiveCount(pair.opt(1));
                if (!type.isEmpty() && count > 0) {
                    steps.add(new String[]{type, Integer.toString(count)});
                }
                continue;
            }
            Object item = sequence.opt(i);
            if (!(item instanceof String)) {
                continue;
            }
            String[] parts = ((String) item).split(",", 2);
            String type = parts[0].trim();
            int count = parts.length >= 2 ? parsePositiveCount(parts[1]) : 1;
            if (!type.isEmpty() && count > 0) {
                steps.add(new String[]{type, Integer.toString(count)});
            }
        }
        return steps;
    }
