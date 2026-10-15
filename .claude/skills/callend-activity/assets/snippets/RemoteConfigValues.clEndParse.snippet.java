// Inside RemoteConfigValues.apply(...) where screenObject = Screen JSON
        JSONObject clEndScreen = child(screenObject, "ClEndScreen");
        clEndAdShow = clEndScreen.optBoolean("ClEnd_Ad_Show", false);
        clEndAdType = clEndScreen.optString("ClEnd_Ad_Type", "banner");
        clEndBannerId = clEndScreen.optString("ClEnd_Banner_Id", "");
        clEndNativeId = clEndScreen.optString("ClEnd_Native_Id", "");
        clEndBackAdShow = clEndScreen.optBoolean("ClEnd_Back_Ad_Show", false);
        clEndBackAdType = clEndScreen.optString("ClEnd_Back_Ad_Type", "inter").trim();
        String previousSequence = formatBackAdSequence(clEndBackAdSequence);
        clEndBackAdSequence = parseBackAdSequenceField(clEndScreen);
        clEndBackAdShowAfterDay = clEndScreen.optInt("ClEnd_Back_Ad_Show_After_Day", 0);
        clEndBackAdShowPerDay = clEndScreen.optInt("ClEnd_Back_Ad_Show_Per_Day", 0);
        clEndBackAdInterstitialId = clEndScreen.optString("ClEnd_Back_Ad_Interstitial_Id", "");
        clEndBackAdNativeId = clEndScreen.optString("ClEnd_Back_Ad_Native_Id", "");
        clEndBackAdCountryIP = clEndScreen.optBoolean("ClEnd_Back_Ad_Country_IP", false);
        String nextSequence = formatBackAdSequence(clEndBackAdSequence);
        if (context != null && !previousSequence.equals(nextSequence)) {
            context.getApplicationContext()
                    .getSharedPreferences("cl_end_ad_preferences", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("back_ad_sequence_cursor", 0L)
                    .apply();
        }
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
