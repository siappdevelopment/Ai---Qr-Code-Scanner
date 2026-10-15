    public static boolean isScreenCompleted(Context context, String screenName) {
        if (context == null || screenName == null) {
            return true;
        }
        if (ScreenFlowConfig.SCREEN_LANGUAGE.equalsIgnoreCase(screenName)) {
            return AppUtils.getLanguageFlowCompleted(context);
        }
        if (ScreenFlowConfig.SCREEN_COLLECTION.equalsIgnoreCase(screenName)) {
            return AppUtils.isCollectionScreenCompleted(context);
        }
        if (ScreenFlowConfig.SCREEN_PERMISSION.equalsIgnoreCase(screenName)) {
            return AppUtils.isPermissionScreenCompleted(context);
        }
        if (ScreenFlowConfig.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screenName)) {
            if (AppUtils.isDefaultHomeApp(context) && !AppUtils.isDefaultHomeScreenCompleted(context)) {
                AppUtils.setDefaultHomeScreenCompleted(context, true);
            }
            return AppUtils.isDefaultHomeScreenCompleted(context) || AppUtils.isDefaultHomeApp(context);
        }
        if (ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME.equalsIgnoreCase(screenName)) {
            return AppUtils.isDefaultHomeApp(context) || AppUtils.isDefaultSettingHomeScreenCompleted(context);
        }
        if (ScreenFlowConfig.SCREEN_INTRO.equalsIgnoreCase(screenName)) {
            return AppUtils.getIntroCompleted(context);
        }
        return true;
    }

    private static void markScreenCompleted(Context context, String screen) {
        if (context == null || screen == null) {
            return;
        }
        if (ScreenFlowConfig.SCREEN_LANGUAGE.equalsIgnoreCase(screen)) {
            AppUtils.setLanguageSelectedCommit(context, true);
            AppUtils.setLanguageFlowCompleted(context, true);
        } else if (ScreenFlowConfig.SCREEN_COLLECTION.equalsIgnoreCase(screen)) {
            AppUtils.setCollectionScreenCompleted(context, true);
        } else if (ScreenFlowConfig.SCREEN_PERMISSION.equalsIgnoreCase(screen)) {
            AppUtils.setPermissionScreenCompleted(context, true);
        } else if (ScreenFlowConfig.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screen)) {
            AppUtils.setDefaultHomeScreenCompleted(context, true);
        } else if (ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME.equalsIgnoreCase(screen)) {
            AppUtils.setDefaultSettingHomeScreenCompleted(context, true);
        } else if (ScreenFlowConfig.SCREEN_INTRO.equalsIgnoreCase(screen)) {
            AppUtils.setIntroCompleted(context, true);
        }
    }

    private static void clearCompletionsAfter(Context context, List<String> flow, int completedIndex) {
        if (context == null || flow == null || completedIndex < 0) {
            return;
        }
        for (int i = completedIndex + 1; i < flow.size(); i++) {
            String screen = flow.get(i);
            if (ScreenFlowConfig.SCREEN_LANGUAGE.equalsIgnoreCase(screen)) {
                AppUtils.setLanguageFlowCompleted(context, false);
            } else if (ScreenFlowConfig.SCREEN_COLLECTION.equalsIgnoreCase(screen)) {
                AppUtils.setCollectionScreenCompleted(context, false);
            } else if (ScreenFlowConfig.SCREEN_PERMISSION.equalsIgnoreCase(screen)) {
                AppUtils.setPermissionScreenCompleted(context, false);
            } else if (ScreenFlowConfig.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screen)) {
                AppUtils.setDefaultHomeScreenCompleted(context, false);
            } else if (ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME.equalsIgnoreCase(screen)) {
                AppUtils.setDefaultSettingHomeScreenCompleted(context, false);
            } else if (ScreenFlowConfig.SCREEN_INTRO.equalsIgnoreCase(screen)) {
                AppUtils.setIntroCompleted(context, false);
            }
        }
    }

    public static void openScreen(Activity activity, String screenName) {
        if (activity == null || activity.isFinishing() || screenName == null) {
            return;
        }
        if (ScreenFlowConfig.SCREEN_LANGUAGE.equalsIgnoreCase(screenName)) {
            AppUtils.isLanguageFromStarting = true;
        } else if (ScreenFlowConfig.SCREEN_INTRO.equalsIgnoreCase(screenName)) {
            IntroNavigation.openIntroButtonFlow(activity);
            return;
        }
        Intent intent = buildScreenIntent(activity, screenName);
        if (intent == null) {
            openMain(activity);
            return;
        }
        if (ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME.equalsIgnoreCase(screenName)
                && !(activity instanceof LauncherHomeActivity)) {
            // Invisible step: keep the current screen alive underneath (no CLEAR_TASK, no finish) so
            // nothing blinks before the system settings page. continueAfter() clears it afterwards.
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        if (activity instanceof LauncherHomeActivity) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                activity.finish();
            }
        });
    }

    private static Intent buildScreenIntent(Context context, String screen) {
        if (ScreenFlowConfig.SCREEN_LANGUAGE.equalsIgnoreCase(screen)) {
            Intent intent = new Intent(context, LanguageActivity.class);
            intent.putExtra(EXTRA_LANGUAGE_FLOW_STARTING, true);
            return intent;
        }
        if (ScreenFlowConfig.SCREEN_COLLECTION.equalsIgnoreCase(screen)) {
            return new Intent(context, CollectionActivity.class);
        }
        if (ScreenFlowConfig.SCREEN_PERMISSION.equalsIgnoreCase(screen)) {
            return new Intent(context, PermissionActivity.class);
        }
        if (ScreenFlowConfig.SCREEN_DEFAULT_HOME.equalsIgnoreCase(screen)) {
            return new Intent(context, DefaultActivity.class);
        }
        if (ScreenFlowConfig.SCREEN_DEFAULT_SETTING_HOME.equalsIgnoreCase(screen)) {
            return new Intent(context, DefaultSettingHomeActivity.class);
        }
        if (ScreenFlowConfig.SCREEN_INTRO.equalsIgnoreCase(screen)) {
            return new Intent(context, Intro1Activity.class);
        }
        return null;
    }
