    public static boolean getLauncherAppClickAdShow() {
        return launcherAppClickAdShow;
    }

    public static void setLauncherAppClickAdShow(boolean value) {
        launcherAppClickAdShow = value;
    }

    public static int getLauncherAppCount() {
        return launcherAppCount;
    }

    public static void setLauncherAppCount(int value) {
        launcherAppCount = value;
    }

    public static String getLauncherAppAdType() {
        return launcherAppAdSequence.isEmpty() ? "" : launcherAppAdSequence.get(launcherAppAdSequenceIndex % launcherAppAdSequence.size());
    }

    public static void setLauncherAppAdType(String value) {
        ArrayList<String> types = new ArrayList<>();
        if (value != null && !value.trim().isEmpty()) {
            types.add(value.trim());
        }
        setLauncherAppAdSequence(types);
    }

    public static void setLauncherAppAdSequence(@Nullable List<String> types) {
        launcherAppAdSequence.clear();
        launcherAppAdSequenceIndex = 0;
        if (types == null) {
            return;
        }
        for (String type : types) {
            if (type != null && !type.trim().isEmpty() && !normalizeLauncherAppAdType(type).isEmpty()) {
                launcherAppAdSequence.add(type.trim());
            }
        }
    }

    public static String getLauncherAppInterstitialId() {
        return launcherAppInterstitialId;
    }

    public static void setLauncherAppInterstitialId(String value) {
        launcherAppInterstitialId = value == null ? "" : value;
    }

    public static boolean getLauncherAppBackClickAdShow() {
        return launcherAppBackClickAdShow;
    }

    public static void setLauncherAppBackClickAdShow(boolean value) {
        launcherAppBackClickAdShow = value;
    }

    public static int getLauncherAppBackCount() {
        return launcherAppBackCount;
    }

    public static void setLauncherAppBackCount(int value) {
        launcherAppBackCount = value;
    }

    public static String getLauncherAppBackAdType() {
        return launcherAppBackAdSequence.isEmpty() ? "" : launcherAppBackAdSequence.get(launcherAppBackAdSequenceIndex % launcherAppBackAdSequence.size());
    }

    public static void setLauncherAppBackAdType(String value) {
        ArrayList<String> types = new ArrayList<>();
        if (value != null && !value.trim().isEmpty()) {
            types.add(value.trim());
        }
        setLauncherAppBackAdSequence(types);
    }

    public static void setLauncherAppBackAdSequence(@Nullable List<String> types) {
        launcherAppBackAdSequence.clear();
        launcherAppBackAdSequenceIndex = 0;
        if (types == null) {
            return;
        }
        for (String type : types) {
            if (type != null && !type.trim().isEmpty() && !normalizeLauncherAppAdType(type).isEmpty()) {
                launcherAppBackAdSequence.add(type.trim());
            }
        }
    }

    public static String getLauncherAppBackInterstitialId() {
        return launcherAppBackInterstitialId;
    }

    public static void setLauncherAppBackInterstitialId(String value) {
        launcherAppBackInterstitialId = value == null ? "" : value;
    }

    public static void persistLauncherBackConfig(@Nullable Context context) {
        if (context == null) {
            return;
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < launcherAppBackAdSequence.size(); i++) {
            if (i > 0) {
                joined.append('|');
            }
            joined.append(launcherAppBackAdSequence.get(i));
        }
        context.getApplicationContext().getSharedPreferences(LAUNCHER_APP_BACK_PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("backConfigSaved", true)
                .putBoolean("backClickAdShow", launcherAppBackClickAdShow)
                .putInt("backCount", launcherAppBackCount)
                .putString("backSequence", joined.toString())
                .putString("backInterstitialId", launcherAppBackInterstitialId == null ? "" : launcherAppBackInterstitialId)
                .putBoolean("backQuizOnFail", launcherGoogleAdFailedShowQuiz)
                .apply();
        launcherBackConfigRestored = true;
    }

    private static void restoreLauncherBackConfig(@Nullable Context context) {
        if (context == null || launcherBackConfigRestored || !launcherAppBackAdSequence.isEmpty()) {
            launcherBackConfigRestored = true;
            return;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(LAUNCHER_APP_BACK_PREFS, Context.MODE_PRIVATE);
        launcherBackConfigRestored = true;
        if (!prefs.getBoolean("backConfigSaved", false)) {
            return;
        }
        launcherAppBackClickAdShow = prefs.getBoolean("backClickAdShow", false);
        launcherAppBackCount = prefs.getInt("backCount", 0);
        launcherAppBackInterstitialId = prefs.getString("backInterstitialId", "");
        launcherGoogleAdFailedShowQuiz = prefs.getBoolean("backQuizOnFail", false);
        String raw = prefs.getString("backSequence", "");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        for (String part : raw.split("\\|")) {
            if (part != null && !part.trim().isEmpty()) {
                launcherAppBackAdSequence.add(part.trim());
            }
        }
    }

    public static void handleLauncherAppClickAd(@Nullable Activity activity, @Nullable Runnable openSelectedApp) {
        if (openSelectedApp == null) {
            return;
        }
        if (activity == null || activity.isFinishing() || !getLauncherAppClickAdShow() || getLauncherAppCount() <= 0) {
            openSelectedApp.run();
            return;
        }
        if (getLauncherAppCount() == launcherAppClickCount) {
            launcherAppClickCount = 1;
            executeLauncherAppAd(activity, openSelectedApp, nextLauncherAppAdType(false), getLauncherAppInterstitialId());
            return;
        }
        launcherAppClickCount++;
        openSelectedApp.run();
    }

    public static void markLauncherExternalAppLaunched(@Nullable Context context, @Nullable String packageName) {
        if (context == null) {
            return;
        }
        String launched = packageName == null ? "" : packageName.trim();
        context.getApplicationContext().getSharedPreferences(LAUNCHER_APP_BACK_PREFS, Context.MODE_PRIVATE).edit().putBoolean("waitingForLauncherAppReturn", true).putString("lastLaunchedPackage", launched).apply();
    }

    /**
     * The launcher was opened fresh (not resumed from an external app), e.g. after the app was closed or removed
     * from Recents. A pending "returned from an external app" mark from before that must not show the back ad.
     */
    public static void clearLauncherAppReturnPending(@Nullable Context context) {
        if (context == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(LAUNCHER_APP_BACK_PREFS, Context.MODE_PRIVATE).edit().putBoolean("waitingForLauncherAppReturn", false).apply();
    }

    public static void handleLauncherAppReturnAd(@Nullable Activity activity) {
        if (!(activity instanceof LauncherHomeActivity) || activity.isFinishing()) {
            return;
        }
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(LAUNCHER_APP_BACK_PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("waitingForLauncherAppReturn", false)) {
            return;
        }
        String lastLaunchedPackage = prefs.getString("lastLaunchedPackage", "");
        prefs.edit().putBoolean("waitingForLauncherAppReturn", false).apply();
        if (lastLaunchedPackage == null || lastLaunchedPackage.trim().isEmpty()) {
            return;
        }
        restoreLauncherBackConfig(activity);
        if (!getLauncherAppBackClickAdShow() || getLauncherAppBackCount() <= 0) {
            return;
        }
        showJoinedLauncherBackAd(activity, () -> {
        });
    }

    /** Same counter, ad sequence, and interstitial id as the launcher back flow. */
    public static void showJoinedLauncherBackAd(@Nullable Activity activity, @Nullable Runnable after) {
        Runnable done = after == null ? () -> {
        } : after;
        if (activity == null || activity.isFinishing()) {
            done.run();
            return;
        }
        restoreLauncherBackConfig(activity);
        if (getLauncherAppBackCount() <= 0) {
            done.run();
            return;
        }
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(LAUNCHER_APP_BACK_PREFS, Context.MODE_PRIVATE);
        int returnCount = prefs.getInt("launcherAppReturnCount", 0) + 1;
        boolean showReturnAd = returnCount >= getLauncherAppBackCount();
        if (showReturnAd) {
            returnCount = 0;
        }
        prefs.edit().putInt("launcherAppReturnCount", returnCount).apply();
        if (!showReturnAd) {
            done.run();
            return;
        }
        String adType = nextLauncherAppAdType(true);
        if (adType.isEmpty()) {
            done.run();
            return;
        }
        executeLauncherAppAd(activity, done, adType, getLauncherAppBackInterstitialId());
    }

    private static String nextLauncherAppAdType(boolean back) {
        ArrayList<String> sequence = back ? launcherAppBackAdSequence : launcherAppAdSequence;
        if (sequence.isEmpty()) {
            return "";
        }
        int index = back ? launcherAppBackAdSequenceIndex : launcherAppAdSequenceIndex;
        String type = normalizeLauncherAppAdType(sequence.get(index % sequence.size()));
        int next = (index + 1) % sequence.size();
        if (back) {
            launcherAppBackAdSequenceIndex = next;
        } else {
            launcherAppAdSequenceIndex = next;
        }
        return type;
    }

    private static boolean launcherQuizOnGoogleFail() {
        return launcherGoogleAdFailedShowQuiz || getGoogleAdFailedShowQuiz();
    }

    private static void executeLauncherAppAd(Activity activity, Runnable continueAction, String adType, @Nullable String interstitialId) {
        executeSequencedAd(activity, continueAction, adType, interstitialId, getLauncherAppNativeId());
    }

    private static void executeSequencedAd(Activity activity, Runnable continueAction, String adType, @Nullable String interstitialId, @Nullable String fullNativeId) {
        executeSequencedAd(activity, continueAction, adType, interstitialId, fullNativeId, launcherQuizOnGoogleFail());
    }

    private static void showNativeFullAd(Activity activity, Runnable continueAction, boolean quizOnFail, @Nullable String fullNativeId) {
        String nativeId = fullNativeId == null ? "" : fullNativeId.trim();
        AtomicBoolean completed = new AtomicBoolean(false);
        OnInterstitialAdListener listener = () -> continueAction.run();
        if (!canLoad(activity, nativeId)) {
            if (quizOnFail && QuizAds.showNativeFull(activity, () -> notifyComplete(listener, completed))) {
                return;
            }
            continueAction.run();
            return;
        }
        Dialog dialog = showInterstitialLoadingDialog(activity);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeout = () -> {
            dismissInterstitialLoadingDialog(dialog);
            if (quizOnFail && QuizAds.showNativeFull(activity, () -> notifyComplete(listener, completed))) {
                return;
            }
            notifyComplete(listener, completed);
        };
        handler.postDelayed(timeout, INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS);
        AdLoader loader = new AdLoader.Builder(activity, nativeId).forNativeAd(nativeAd -> {
            AnalyticsEvents.ad(activity, AnalyticsEvents.NATIVE_LOAD, AnalyticsEvents.FORMAT_NATIVE, nativeId);
            handler.removeCallbacks(timeout);
            dismissInterstitialLoadingDialog(dialog);
            if (activity.isFinishing()) {
                nativeAd.destroy();
                notifyComplete(listener, completed);
                return;
            }
            presentNativeFullAd(activity, nativeAd, listener, completed);
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                AnalyticsEvents.ad(activity, AnalyticsEvents.NATIVE_FAIL, AnalyticsEvents.FORMAT_NATIVE, nativeId);
                handler.removeCallbacks(timeout);
                dismissInterstitialLoadingDialog(dialog);
                if (quizOnFail && QuizAds.showNativeFull(activity, () -> notifyComplete(listener, completed))) {
                    return;
                }
                notifyComplete(listener, completed);
            }
        }).build();
        loader.loadAd(new AdRequest.Builder().build());
    }

    private static void presentNativeFullAd(Activity activity, NativeAd nativeAd, OnInterstitialAdListener listener, AtomicBoolean completed) {
        Dialog full = new Dialog(activity, R.style.Theme_NativeFullAd);
        full.setCancelable(true);
        NativeAdView adView = (NativeAdView) LayoutInflater.from(AdTheme.forLauncher(activity)).inflate(R.layout.native_full_ad_layout, null);
        populateNativeAdView(nativeAd, adView, "full");
        View close = adView.findViewById(R.id.ivClose);
        if (close != null) {
            close.bringToFront();
            close.setClickable(true);
            close.setOnClickListener(v -> {
                try {
                    full.dismiss();
                } catch (Exception ignored) {
                }
            });
        }
        full.setContentView(adView);
        Window fullWindow = full.getWindow();
        Window host = activity.getWindow();
        final int savedStatusColor = host.getStatusBarColor();
        final int savedNavColor = host.getNavigationBarColor();
        WindowInsetsControllerCompat hostController = WindowCompat.getInsetsController(host, host.getDecorView());
        final boolean savedLightStatus = hostController.isAppearanceLightStatusBars();
        final boolean savedLightNav = hostController.isAppearanceLightNavigationBars();
        final boolean savedStatusContrast = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && host.isStatusBarContrastEnforced();
        final boolean savedNavContrast = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && host.isNavigationBarContrastEnforced();
        applyNativeFullSystemBars(activity, fullWindow, adView);
        full.setOnDismissListener(d -> {
            host.setStatusBarColor(savedStatusColor);
            host.setNavigationBarColor(savedNavColor);
            hostController.setAppearanceLightStatusBars(savedLightStatus);
            hostController.setAppearanceLightNavigationBars(savedLightNav);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                host.setStatusBarContrastEnforced(savedStatusContrast);
                host.setNavigationBarContrastEnforced(savedNavContrast);
            }
            nativeAd.destroy();
            notifyComplete(listener, completed);
        });
        try {
            full.show();
            applyNativeFullSystemBars(activity, fullWindow, adView);
            if (close != null) {
                close.bringToFront();
            }
        } catch (Exception e) {
            nativeAd.destroy();
            notifyComplete(listener, completed);
        }
    }

    private static String normalizeLauncherAppAdType(@Nullable String adType) {
        if (adType == null) {
            return "";
        }
        String value = adType.trim();
        if ("Google_Inter".equalsIgnoreCase(value) || "google_inter".equalsIgnoreCase(value) || "inter".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_GOOGLE_INTER;
        }
        if ("Google_App_Open".equalsIgnoreCase(value) || "google_app_open".equalsIgnoreCase(value) || "app_open".equalsIgnoreCase(value)
                || "appopen".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_GOOGLE_APP_OPEN;
        }
        if ("Google_Native".equalsIgnoreCase(value) || "google_native".equalsIgnoreCase(value)
                || "fullnative".equalsIgnoreCase(value) || "full_native".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_GOOGLE_NATIVE;
        }
        if ("Quiz_Inter".equalsIgnoreCase(value) || "quiz_inter".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_INTER;
        }
        if ("Quiz_App_Open".equalsIgnoreCase(value) || "quiz_app_open".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_APP_OPEN;
        }
        if ("Quiz_Native".equalsIgnoreCase(value) || "quiz_native".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_NATIVE;
        }
        if ("Quiz_Browser".equalsIgnoreCase(value) || "quiz_browser".equalsIgnoreCase(value)) {
            return LAUNCHER_APP_AD_TYPE_QUIZ_BROWSER;
        }
        return "";
    }

    public static String getLauncherAppNativeId() {
        return RemoteConfigValues.getLauncherAppNativeId();
    }
