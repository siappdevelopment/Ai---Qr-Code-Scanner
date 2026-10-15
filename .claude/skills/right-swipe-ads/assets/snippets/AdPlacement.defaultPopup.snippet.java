    public static boolean getDefaultAppPopupShow() {
        return RemoteConfigValues.getDefaultAppPopupShow();
    }

    public static int getDefaultAppPopupCount() {
        return RemoteConfigValues.getDefaultAppPopupCount();
    }

    public static boolean canShowDefaultAppPopup(Context context) {
        int countPerDay = getDefaultAppPopupCount();
        if (countPerDay <= 0) {
            return true;
        }
        long intervalMillis = (24L * 60L * 60L * 1000L) / countPerDay;
        long lastShowTime = getDefaultAppPopupLastShowTime(context);
        return System.currentTimeMillis() - lastShowTime >= intervalMillis;
    }

    public static long getDefaultAppPopupLastShowTime(Context context) {
        return context.getSharedPreferences("default_app_popup_preferences", Context.MODE_PRIVATE).getLong("last_show_time", 0);
    }

    public static void setDefaultAppPopupLastShowTime(Context context, long time) {
        context.getSharedPreferences("default_app_popup_preferences", Context.MODE_PRIVATE).edit().putLong("last_show_time", time).apply();
    }
