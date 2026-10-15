    @NonNull
    private static ArrayList<String[]> parseBackAdSequenceField(@NonNull JSONObject clEndScreen) {
        JSONArray sequence = clEndScreen.optJSONArray("ClEnd_Back_Ad_Sequence");
        if (sequence == null) {
            String raw = clEndScreen.optString("ClEnd_Back_Ad_Sequence", "").trim();
            if (!raw.isEmpty()) {
                try {
                    sequence = new JSONArray(raw);
                } catch (Exception ignored) {
                    ArrayList<String[]> fromStored = parseStoredBackAdSequence(raw);
                    if (!fromStored.isEmpty()) {
                        return fromStored;
                    }
                }
            }
        }
        return parseBackAdSequence(sequence);
    }

    private static ArrayList<String[]> parseBackAdSequence(@Nullable JSONArray sequence) {
        ArrayList<String[]> steps = new ArrayList<>();
        if (sequence == null) {
            return steps;
        }
        for (int i = 0; i < sequence.length(); i++) {
            JSONArray pair = sequence.optJSONArray(i);
            if (pair == null) {
                Object item = sequence.opt(i);
                if (item instanceof String) {
                    String[] parts = ((String) item).split(",", 2);
                    if (parts.length >= 2) {
                        String type = normalizeBackAdFormat(parts[0]);
                        int count = parsePositiveCount(parts[1]);
                        if (!type.isEmpty() && count > 0) {
                            steps.add(new String[]{type, Integer.toString(count)});
                        }
                    }
                }
                continue;
            }
            if (pair.length() < 2) {
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

    public static boolean getAllAllowPermissionShowNotification() {
        return allAllowPermissionShowNotification;
    }

    public static boolean getNotificationBackAdShow() {
        return notificationBackAdShow;
    }

    public static boolean getNotificationCloseButtonShow() {
        return notificationCloseButtonShow;
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
        editor.putString("nativeAdButtonColorDark", AdPlacement.getNativeAdButtonColorDark() == null ? "" : AdPlacement.getNativeAdButtonColorDark());
        editor.putString("clEndBackAdShowCountryList", formatCountryList(clEndBackAdShowCountryList));
        editor.putInt("notificationInstallDays", notificationInstallDays);
        editor.putInt("notificationCallInstallDays", notificationCallInstallDays);
        editor.putInt("notificationCallOverlayInstallDays", notificationCallOverlayInstallDays);
        editor.putBoolean("allAllowPermissionShowNotification", allAllowPermissionShowNotification);
        editor.putBoolean("notificationBackAdShow", notificationBackAdShow);
        editor.putBoolean("notificationCloseButtonShow", notificationCloseButtonShow);
        saveEventScreenConfig(editor, chargingScreenConfig, "eventCharging");
        saveEventScreenConfig(editor, installUninstallScreenConfig, "eventInstallUninstall");
        editor.putBoolean("rightSwipeInterstitialAdShow", rightSwipeInterstitialAdShow);
        editor.putInt("rightSwipeInterstitial", rightSwipeInterstitial);
        editor.putString("rightSwipeAdsType", rightSwipeAdsType == null ? "load" : rightSwipeAdsType);
        editor.putString("notificationCountryList", formatCountryList(notificationCountryList));
        editor.putString("notificationCallCountryList", formatCountryList(notificationCallCountryList));
        editor.putString("notificationCallOverlayCountryList", formatCountryList(notificationCallOverlayCountryList));
        editor.apply();
    }
