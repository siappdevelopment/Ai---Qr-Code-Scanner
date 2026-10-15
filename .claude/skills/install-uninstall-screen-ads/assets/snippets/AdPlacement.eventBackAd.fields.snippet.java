// AdPlacement - fields
    private static final String EVENT_SCREEN_AD_PREFS = "event_screen_ad_preferences";
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(EVENT_SCREEN_AD_PREFS, Context.MODE_PRIVATE);
    private static final class EventBackCache {
        String type = "";
        String unitId = "";
        boolean loading;
        int token;
        long loadedAt;
        @Nullable
        InterstitialAd inter;
        @Nullable
        AppOpenAd appOpen;
        @Nullable
        NativeAd nativeAd;

        boolean ready() {
            return inter != null || appOpen != null || nativeAd != null;
        }

        void clear() {
            token++;
            loading = false;
            type = "";
            unitId = "";
            loadedAt = 0L;
            inter = null;
            appOpen = null;
            if (nativeAd != null) {
                nativeAd.destroy();
                nativeAd = null;
            }
        }
    }
    private static final EventBackCache EVENT_BACK_CHARGING = new EventBackCache();
    private static final EventBackCache EVENT_BACK_INSTALL_UNINSTALL = new EventBackCache();
    private static EventBackCache eventBackCacheFor(@Nullable String screenKey) {
        return RemoteConfigValues.EVENT_SCREEN_CHARGING.equals(screenKey) ? EVENT_BACK_CHARGING : EVENT_BACK_INSTALL_UNINSTALL;
    }
