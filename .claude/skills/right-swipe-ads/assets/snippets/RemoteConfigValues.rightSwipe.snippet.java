    private static boolean rightSwipeInterstitialAdShow;
    private static int rightSwipeInterstitial;
    private static String rightSwipeAdsType ;
        clEndScreenShow = root.optBoolean("Cl_End_Screen_Show", false);
        defaultAppPopupShow = root.optBoolean("Default_App_Popup_Show", false);
        defaultAppPopupCount = root.optInt("Default_App_Popup_Count", 0);

        bottomNavInterAdsId = mainScreen.optString("bottom_nav_inter_ads_id", "");
        bottomNavInterClick = mainScreen.optInt("bottom_nav_inter_click", 0);
        rightSwipeInterstitialAdShow = mainScreen.optBoolean("Right_Swipe_Interstitial_Ad_Show", false);
        rightSwipeInterstitial = mainScreen.optInt("Right_Swipe_Interstitial", 0);
        rightSwipeAdsType = optStringIgnoreCase(mainScreen, "Right_Swipe_Ads_type", "load");

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

    }

    public static boolean getDefaultAppPopupShow() {
        return defaultAppPopupShow;
    }

    public static int getDefaultAppPopupCount() {
        return defaultAppPopupCount;
    }

    public static boolean getLauncherAppNativeAdShow() {
