    /** Event screen back ad: ClEnd-style gating (install days, per-day count, country) with the launcher ad-type sequence. */
    public static void showEventBackAd(@Nullable Activity activity, @Nullable String screenKey, @Nullable Runnable after) {
        Runnable done = after == null ? () -> {
        } : after;
        if (activity == null || activity.isFinishing()) {
            done.run();
            return;
        }
        RemoteConfigValues.ensureLoaded(activity);
        RemoteConfigValues.EventScreenConfig config = RemoteConfigValues.getEventScreenConfig(screenKey);
        if (!config.backAdShow || !isNetworkAvailable(activity)) {
            done.run();
            return;
        }
        if (config.backAdDayCount <= 0 || getDaysSinceInstall(activity) < config.backAdDayCount) {
            done.run();
            return;
        }
        if (config.backAdTotalShowCount <= 0) {
            done.run();
            return;
        }
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(EVENT_SCREEN_AD_PREFS, Context.MODE_PRIVATE);
        long intervalMillis = (24L * 60L * 60L * 1000L) / config.backAdTotalShowCount;
        long lastShow = prefs.getLong(screenKey + "_back_ad_last_show_time", 0L);
        if (System.currentTimeMillis() - lastShow < intervalMillis) {
            done.run();
            return;
        }
        if (!isCountryAllowed(activity, config.backAdCountryList)) {
            done.run();
            return;
        }
        String adType = nextEventBackAdType(prefs, screenKey, config, true);
        if (adType.isEmpty()) {
            done.run();
            return;
        }
        prefs.edit().putLong(screenKey + "_back_ad_last_show_time", System.currentTimeMillis()).apply();
        Context app = activity.getApplicationContext();
        Runnable next = () -> {
            done.run();
            preloadEventBackAd(app, screenKey);
        };
        if (showPreloadedEventBackAd(activity, screenKey, adType, next)) {
            return;
        }
        executeSequencedAd(activity, next, adType, config.interId, config.fullNativeId, false);
    }

    /**
     * With "*_ad_load_type": "PreLoad", load the back ad for the next sequence step ahead of time
     * (Google App Open / Native / Inter), so the back press shows it without the loading dialog.
     */
    public static void preloadEventBackAd(@Nullable Context context, @Nullable String screenKey) {
        if (context == null || screenKey == null) {
            return;
        }
        Context app = context.getApplicationContext();
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> preloadEventBackAd(app, screenKey));
            return;
        }
        RemoteConfigValues.ensureLoaded(app);
        initializeIfConfigured(app);
        RemoteConfigValues.EventScreenConfig config = RemoteConfigValues.getEventScreenConfig(screenKey);
        EventBackCache cache = eventBackCacheFor(screenKey);
        String loadType = config.adLoadType == null ? "" : config.adLoadType.trim();
        if (!config.backAdShow || !"preload".equalsIgnoreCase(loadType)
                || config.backAdDayCount <= 0 || config.backAdTotalShowCount <= 0) {
            cache.clear();
            return;
        }
        if (shouldUseQuizPriority() || !canRequestAds(app) || !isNetworkAvailable(app)
                || getDaysSinceInstall(app) < config.backAdDayCount
                || !isCountryAllowed(app, config.backAdCountryList)) {
            return;
        }
        SharedPreferences prefs = app.getSharedPreferences(EVENT_SCREEN_AD_PREFS, Context.MODE_PRIVATE);
        String type = nextEventBackAdType(prefs, screenKey, config, false);
        String unitId;
        if (LAUNCHER_APP_AD_TYPE_GOOGLE_INTER.equals(type)) {
            unitId = config.interId == null ? "" : config.interId.trim();
        } else if (LAUNCHER_APP_AD_TYPE_GOOGLE_APP_OPEN.equals(type)) {
            unitId = getAppOpenId() == null ? "" : getAppOpenId().trim();
        } else if (LAUNCHER_APP_AD_TYPE_GOOGLE_NATIVE.equals(type)) {
            unitId = config.fullNativeId == null ? "" : config.fullNativeId.trim();
        } else {
            // Quiz steps (or nothing to show) need no Google preload.
            cache.clear();
            return;
        }
        if (unitId.isEmpty()) {
            cache.clear();
            return;
        }
        boolean sameStep = type.equals(cache.type) && unitId.equals(cache.unitId);
        boolean fresh = cache.ready() && System.currentTimeMillis() - cache.loadedAt < EVENT_BACK_AD_MAX_AGE_MS;
        if (sameStep && (cache.loading || fresh)) {
            return;
        }
        cache.clear();
        cache.type = type;
        cache.unitId = unitId;
        cache.loading = true;
        int token = cache.token;
        if (LAUNCHER_APP_AD_TYPE_GOOGLE_INTER.equals(type)) {
            AnalyticsEvents.ad(app, AnalyticsEvents.INTERSTITIAL_PRELOAD, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
            InterstitialAd.load(app, unitId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    AnalyticsEvents.ad(app, AnalyticsEvents.INTERSTITIAL_LOAD, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
                    if (token != cache.token) {
                        return;
                    }
                    cache.loading = false;
                    cache.inter = ad;
                    cache.loadedAt = System.currentTimeMillis();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    AnalyticsEvents.ad(app, AnalyticsEvents.INTERSTITIAL_FAIL, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
                    if (token == cache.token) {
                        cache.loading = false;
                    }
                }
            });
            return;
        }
        if (LAUNCHER_APP_AD_TYPE_GOOGLE_APP_OPEN.equals(type)) {
            AppOpenAd.load(app, unitId, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    AnalyticsEvents.ad(app, AnalyticsEvents.APP_OPEN_AD_LOAD, AnalyticsEvents.FORMAT_APP_OPEN, unitId);
                    if (token != cache.token) {
                        return;
                    }
                    cache.loading = false;
                    cache.appOpen = ad;
                    cache.loadedAt = System.currentTimeMillis();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    AnalyticsEvents.ad(app, AnalyticsEvents.APP_OPEN_AD_FAIL, AnalyticsEvents.FORMAT_APP_OPEN, unitId);
                    if (token == cache.token) {
                        cache.loading = false;
                    }
                }
            });
            return;
        }
        new AdLoader.Builder(app, unitId).forNativeAd(nativeAd -> {
            AnalyticsEvents.ad(app, AnalyticsEvents.NATIVE_LOAD, AnalyticsEvents.FORMAT_NATIVE, unitId);
            if (token != cache.token) {
                nativeAd.destroy();
                return;
            }
            cache.loading = false;
            cache.nativeAd = nativeAd;
            cache.loadedAt = System.currentTimeMillis();
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                AnalyticsEvents.ad(app, AnalyticsEvents.NATIVE_FAIL, AnalyticsEvents.FORMAT_NATIVE, unitId);
                if (token == cache.token) {
                    cache.loading = false;
                }
            }
        }).build().loadAd(new AdRequest.Builder().build());
    }

    /** Shows the preloaded ad when it matches this step; otherwise drops it so the caller loads on demand. */
    private static boolean showPreloadedEventBackAd(Activity activity, @Nullable String screenKey, String adType, Runnable next) {
        EventBackCache cache = eventBackCacheFor(screenKey);
        boolean usable = adType.equals(cache.type) && cache.ready()
                && System.currentTimeMillis() - cache.loadedAt < EVENT_BACK_AD_MAX_AGE_MS
                && !shouldUseQuizPriority();
        if (!usable) {
            cache.clear();
            return false;
        }
        InterstitialAd inter = cache.inter;
        AppOpenAd appOpen = cache.appOpen;
        NativeAd nativeAd = cache.nativeAd;
        cache.nativeAd = null;
        cache.clear();
        AtomicBoolean completed = new AtomicBoolean(false);
        OnInterstitialAdListener listener = next::run;
        boolean quizOnFail = false; // Charging / Install: Google ads only, no Quiz fallback when a Google ad fails.
        if (nativeAd != null) {
            presentNativeFullAd(activity, nativeAd, listener, completed);
            return true;
        }
        FullScreenContentCallback callback = new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                notifyComplete(listener, completed);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                boolean shown = quizOnFail && (inter != null
                        ? QuizAds.showInterstitial(activity, () -> notifyComplete(listener, completed))
                        : QuizAds.showAppOpen(activity, () -> notifyComplete(listener, completed)));
                if (!shown) {
                    notifyComplete(listener, completed);
                }
            }
        };
        if (inter != null) {
            inter.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
            inter.setFullScreenContentCallback(callback);
            inter.show(activity);
        } else {
            appOpen.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
            appOpen.setFullScreenContentCallback(callback);
            appOpen.show(activity);
        }
        return true;
    }

    /** advance=false peeks at the step the next back press will use without moving the cursor. */
    @NonNull
    private static String nextEventBackAdType(@NonNull SharedPreferences prefs, @Nullable String screenKey,
                                              @NonNull RemoteConfigValues.EventScreenConfig config, boolean advance) {
        List<String> types = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        long total = 0L;
        for (String[] step : config.backAdSequence) {
            if (step == null || step.length < 2) {
                continue;
            }
            String type = normalizeLauncherAppAdType(step[0]);
            int count;
            try {
                count = Integer.parseInt(step[1]);
            } catch (NumberFormatException e) {
                continue;
            }
            if (type.isEmpty() || count <= 0) {
                continue;
            }
            if (LAUNCHER_APP_AD_TYPE_GOOGLE_INTER.equals(type) && (config.interId == null || config.interId.trim().isEmpty())) {
                continue;
            }
            if (LAUNCHER_APP_AD_TYPE_GOOGLE_NATIVE.equals(type) && (config.fullNativeId == null || config.fullNativeId.trim().isEmpty())) {
                continue;
            }
            if (LAUNCHER_APP_AD_TYPE_GOOGLE_APP_OPEN.equals(type) && (getAppOpenId() == null || getAppOpenId().trim().isEmpty())) {
                continue;
            }
            types.add(type);
            counts.add(count);
            total += count;
        }
        if (total <= 0L) {
            return "";
        }
        String cursorKey = screenKey + "_back_ad_sequence_cursor";
        long cursor = prefs.getLong(cursorKey, 0L);
        if (cursor < 0L) {
            cursor = 0L;
        }
        if (advance) {
            prefs.edit().putLong(cursorKey, cursor + 1L).apply();
        }
        long index = cursor % total;
        long walked = 0L;
        for (int i = 0; i < types.size(); i++) {
            walked += counts.get(i);
            if (index < walked) {
                return types.get(i);
            }
        }
        return "";
    }

    private static boolean isCountryAllowed(Context context, @Nullable List<String> allowedCountries) {
        if (allowedCountries == null || allowedCountries.isEmpty()) {
            return true;
        }
        String currentCountry = getDeviceCountry(context);
        for (String country : allowedCountries) {
            if (country != null && country.equalsIgnoreCase(currentCountry)) {
                return true;
            }
        }
        return false;
    }

    public static long getDaysSinceInstall(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("appInstallDate", Context.MODE_PRIVATE);
        String installDate = sharedPreferences.getString("install_date", "");
        if (installDate.isEmpty()) {
            return 1;
        }
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date1 = simpleDateFormat.parse(installDate);
            Date date2 = new Date();
            if (date1 == null) {
                return 1;
            }
            long diff = date2.getTime() - date1.getTime();
            return (diff / (24 * 60 * 60 * 1000)) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    public static void saveInstallDate(@Nullable Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences("appInstallDate", Context.MODE_PRIVATE);
        if (!sharedPreferences.contains("install_date")) {
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(new java.util.Date());
            sharedPreferences.edit().putString("install_date", today).apply();
        }
    }

    public static void showLargeNative(Activity activity, FrameLayout container, NativeAd nativeAd) {
        showLargeNative(activity, container, nativeAd, false);
    }
