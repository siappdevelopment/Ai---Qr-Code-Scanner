    public static boolean hasOverlayPermission(Context context) {
        if (context == null) {
            return false;
        }
        try {
            return Settings.canDrawOverlays(context);
        } catch (Exception e) {
            return false;
        }
    }
