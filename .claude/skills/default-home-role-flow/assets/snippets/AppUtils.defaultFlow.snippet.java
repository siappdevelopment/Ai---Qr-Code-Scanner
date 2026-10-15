    public static void clearDefaultAppSetupState(Context context) {
        if (context == null) {
            return;
        }
        setCompletingDefaultAppSetup(context, false);
        releaseAfterDefaultFlowClaim();
    }

    public static void setCompletingDefaultAppSetup(Context context, boolean value) {
        if (context == null) {
            return;
        }
        afterDefaultFlowClaimed = false;
        SharedPreferences.Editor editor = context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).edit().putBoolean(KEY_COMPLETING_DEFAULT_APP_SETUP, value);
        if (!value) {
            editor.putBoolean(KEY_AWAITING_DEFAULT_ROLE_RESULT, false);
        }
        editor.apply();
    }

    public static synchronized void releaseAfterDefaultFlowClaim() {
        afterDefaultFlowClaimed = false;
    }

    public static void setAwaitingDefaultRoleResult(Context context, boolean value) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).edit().putBoolean(KEY_AWAITING_DEFAULT_ROLE_RESULT, value).apply();
    }

    public static boolean isAwaitingDefaultRoleResult(Context context) {
        if (context == null) {
            return false;
        }
        return context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).getBoolean(KEY_AWAITING_DEFAULT_ROLE_RESULT, false);
    }

    public static synchronized boolean tryClaimAfterDefaultFlow(Context context) {
        if (!isCompletingDefaultAppSetup(context)) {
            return false;
        }
        if (afterDefaultFlowClaimed) {
            return false;
        }
        afterDefaultFlowClaimed = true;
        return true;
    }

    public static boolean isCompletingDefaultAppSetup(Context context) {
        if (context == null) {
            return false;
        }
        return context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).getBoolean(KEY_COMPLETING_DEFAULT_APP_SETUP, false);
    }

    public static void navigateAfterDefaultAppSetup(Context context) {
        if (context == null) {
            return;
        }
        // Resume after the flow step that started the setup, not always after DefaultHome: with DefaultHome
        // first in Show_Screen_Flow, continuing after it would reset every later screen.
        final String startedBy = getDefaultSetupStep(context);
        setCompletingDefaultAppSetup(context, false);
        if (ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME.equals(startedBy)) {
            setDefaultSettingHomeScreenCompleted(context, true);
        } else {
            setDefaultHomeScreenCompleted(context, true);
        }
        if (isDefaultHomeApp(context)) {
            TrackOnce.trackScreenOnce(context, "DEFAULT_HOME_APP_SET");
        } else {
            TrackOnce.trackScreenOnce(context, "DEFAULT_HOME_APP_CANCEL");
        }
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                AdPlacement.loadAfterDefaultAd(activity, () -> ScreenFlowNavigation.continueAfter(activity, startedBy));
            }
        }
    }

    /** Which flow step (DefaultHome or DefultSettingHome) started the current default-app setup. */
    public static void setDefaultSetupStep(Context context, String step) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).edit().putString("default_setup_step", step).apply();
    }

    public static String getDefaultSetupStep(Context context) {
        String step = context == null ? null : context.getApplicationContext().getSharedPreferences(PREFS_DEFAULT_APP_FLOW, MODE_PRIVATE).getString("default_setup_step", null);
        return step == null || step.isEmpty() ? ScreenFlowConfig.SCREEN_DEFAULT_HOME : step;
    }

    public static void setDefaultHomeScreenCompleted(Context context, boolean completed) {
        if (context == null) {
            return;
        }
        screenFlowPrefs(context).edit().putBoolean(KEY_DEFAULT_HOME_COMPLETED, completed).apply();
    }

    public static boolean isDefaultHomeScreenCompleted(Context context) {
        return context != null && screenFlowPrefs(context).getBoolean(KEY_DEFAULT_HOME_COMPLETED, false);
    }

    public static void setDefaultSettingHomeScreenCompleted(Context context, boolean completed) {
        if (context == null) {
            return;
        }
        screenFlowPrefs(context).edit().putBoolean(KEY_DEFAULT_SETTING_HOME_COMPLETED, completed).apply();
    }

    public static boolean isDefaultSettingHomeScreenCompleted(Context context) {
        return context != null && screenFlowPrefs(context).getBoolean(KEY_DEFAULT_SETTING_HOME_COMPLETED, false);
    }

    public static boolean hasCompletedOnboarding(Context context) {
        return getIntroCompleted(context);
    }
