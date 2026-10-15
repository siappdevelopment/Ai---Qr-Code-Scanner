// RemoteConfigHelper.apply(...) - inside the part that reads Screen.LauncherAppScreen
        AdPlacement.setLauncherAppClickAdShow(launcherAppScreen.optBoolean("LauncherApp_Click_Ad_Show", false));
        AdPlacement.setLauncherAppCount(launcherAppScreen.optInt("LauncherApp_Count", 0));
        AdPlacement.setLauncherAppAdSequence(readAdTypeSequence(launcherAppScreen, "LauncherApp_Ad_Type"));
        AdPlacement.setLauncherAppInterstitialId(launcherAppScreen.optString("LauncherApp_Interstitial_Id", ""));
        AdPlacement.setLauncherAppBackClickAdShow(launcherAppScreen.optBoolean("LauncherApp_Back_Click_Ad_Show", false));
        AdPlacement.setLauncherAppBackCount(launcherAppScreen.optInt("LauncherApp_Back_Count", 0));
        AdPlacement.setLauncherAppBackAdSequence(readAdTypeSequence(launcherAppScreen, "LauncherApp_Back_Ad_Type"));
        AdPlacement.setLauncherAppBackInterstitialId(launcherAppScreen.optString("LauncherApp_Back_Interstitial_Id", ""));

    private static ArrayList<String> readAdTypeSequence(JSONObject screen, String key) {
        ArrayList<String> types = new ArrayList<>();
        JSONArray array = screen.optJSONArray(key);
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String type = array.optString(i, "").trim();
                if (!type.isEmpty()) {
                    types.add(type);
                }
            }
            return types;
        }
        String single = screen.optString(key, "").trim();
        if (!single.isEmpty() && !single.startsWith("[")) {
            types.add(single);
        }
        return types;
    }

// after the config is applied (RemoteConfigHelper ~line 280):
AdPlacement.persistLauncherBackConfig(context);
