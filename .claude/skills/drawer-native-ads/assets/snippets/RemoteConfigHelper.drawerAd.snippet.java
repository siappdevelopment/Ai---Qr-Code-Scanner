        if (launcherAppScreen == null) {
            launcherAppScreen = new JSONObject();
        }
        AdPlacement.setLauncherAppNativeListAdShow(launcherAppScreen.optBoolean("LauncherApp_Native_List_Ad_Show", false));
        AdPlacement.setLauncherAppNativeListAdShowPerDay(launcherAppScreen.optInt("LauncherApp_Native_List_Ad_Show_Per_Day", 0));
        AdPlacement.setLauncherAppNativeListId(launcherAppScreen.optString("LauncherApp_Native_List_Id", ""));
        AdPlacement.setLauncherAppClickAdShow(launcherAppScreen.optBoolean("LauncherApp_Click_Ad_Show", false));
