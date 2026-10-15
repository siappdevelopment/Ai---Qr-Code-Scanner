    public static boolean canShowLauncherAppNativeListAd(Context context) {
        int countPerDay = getLauncherAppNativeListAdShowPerDay();
        if (countPerDay <= 0) {
            return true;
        }
        long intervalMillis = (24L * 60L * 60L * 1000L) / countPerDay;
        long lastShowTime = context.getSharedPreferences("launcher_app_native_list_ad_preferences", Context.MODE_PRIVATE).getLong("last_show_time", 0);
        return System.currentTimeMillis() - lastShowTime >= intervalMillis;
    }

    public static void setLauncherAppNativeListLastShowTime(Context context, long time) {
        context.getSharedPreferences("launcher_app_native_list_ad_preferences", Context.MODE_PRIVATE).edit().putLong("last_show_time", time).apply();
    }

    /**
     * Shows an already loaded Google native ad again in a new container (the app drawer re-shows its last ad when it is
     * opened again before a new one is due). The ad must not have been destroyed.
     */
    public static void bindLoadedNative(Activity activity, FrameLayout flNativeAd, NativeAd nativeAd, String type) {
        if (activity == null || activity.isFinishing() || flNativeAd == null || nativeAd == null) {
            return;
        }
        Context adContext = AdTheme.forAds(activity, false);
        NativeAdView adView = (NativeAdView) LayoutInflater.from(adContext).inflate(resolveGoogleNativeLayout(type), flNativeAd, false);
        populateNativeAdView(nativeAd, adView, type);
        flNativeAd.removeAllViews();
        flNativeAd.addView(GestureSafeNativeAdView.wrap(adView));
        flNativeAd.setVisibility(View.VISIBLE);
        paintOnboardingNative(flNativeAd, onboardingNativeColor(activity, false));
    }

    public static boolean getLauncherAppNativeListAdShow() {
        return launcherAppNativeListAdShow;
    }

    public static void setLauncherAppNativeListAdShow(boolean value) {
        launcherAppNativeListAdShow = value;
    }

    public static int getLauncherAppNativeListAdShowPerDay() {
        return launcherAppNativeListAdShowPerDay;
    }

    public static void setLauncherAppNativeListAdShowPerDay(int value) {
        launcherAppNativeListAdShowPerDay = value;
    }

    public static String getLauncherAppNativeListId() {
        return launcherAppNativeListId;
    }

    public static void setLauncherAppNativeListId(String value) {
        launcherAppNativeListId = value == null ? "" : value;
    }
