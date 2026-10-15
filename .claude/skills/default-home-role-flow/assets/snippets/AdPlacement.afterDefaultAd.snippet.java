    public static boolean getAfterDefaultAdShow() {
        return afterDefaultAdShow;
    }

    public static void applyAfterDefaultAdConfig(org.json.JSONObject permissionDefaultScreen) {
        boolean show = permissionDefaultScreen.optBoolean("After_Default_Ad_Show", permissionDefaultScreen.optBoolean("After_default_Ad_Show", false));
        String type;
        if (permissionDefaultScreen.has("After_Default_Ad_Type")) {
            type = permissionDefaultScreen.optString("After_Default_Ad_Type", "inter");
        } else {
            type = permissionDefaultScreen.optString("After_default_Ad_Type", "inter");
        }
        setAfterDefaultAdShow(show);
        setAfterDefaultAdType(type);
    }

    public static void preloadAfterDefaultAd(Context context) {
        if (context == null || !getAfterDefaultAdShow() || shouldUseQuizPriority() || !canRequestAds(context) || !isNetworkAvailable(context)) {
            return;
        }
        if (isAfterDefaultAppOpenType()) {
            if (preloadedAfterDefaultAppOpen != null || afterDefaultAdPreloadInProgress || getAppOpenId().isEmpty()) {
                return;
            }
            afterDefaultAdPreloadInProgress = true;
            int token = afterDefaultPreloadToken.get();
            AppOpenAd.load(context, getAppOpenId(), new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    AnalyticsEvents.ad(context, AnalyticsEvents.APP_OPEN_AD_LOAD, AnalyticsEvents.FORMAT_APP_OPEN, getAppOpenId());
                    afterDefaultAdPreloadInProgress = false;
                    if (token == afterDefaultPreloadToken.get()) {
                        preloadedAfterDefaultAppOpen = ad;
                    }
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    AnalyticsEvents.ad(context, AnalyticsEvents.APP_OPEN_AD_FAIL, AnalyticsEvents.FORMAT_APP_OPEN, getAppOpenId());
                    afterDefaultAdPreloadInProgress = false;
                }
            });
            return;
        }
        if (preloadedAfterDefaultInterstitial != null || afterDefaultAdPreloadInProgress || getOtherInterstitialId().isEmpty()) {
            return;
        }
        afterDefaultAdPreloadInProgress = true;
        int token = afterDefaultPreloadToken.get();
        AnalyticsEvents.ad(context, AnalyticsEvents.INTERSTITIAL_PRELOAD, AnalyticsEvents.FORMAT_INTERSTITIAL, getOtherInterstitialId());
        InterstitialAd.load(context, getOtherInterstitialId(), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                AnalyticsEvents.ad(context, AnalyticsEvents.INTERSTITIAL_LOAD, AnalyticsEvents.FORMAT_INTERSTITIAL, getOtherInterstitialId());
                afterDefaultAdPreloadInProgress = false;
                if (token == afterDefaultPreloadToken.get()) {
                    preloadedAfterDefaultInterstitial = ad;
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                AnalyticsEvents.ad(context, AnalyticsEvents.INTERSTITIAL_FAIL, AnalyticsEvents.FORMAT_INTERSTITIAL, getOtherInterstitialId());
                afterDefaultAdPreloadInProgress = false;
            }
        });
    }

    public static void loadAfterDefaultAd(Activity activity, OnInterstitialAdListener listener) {
        if (activity == null || activity.isFinishing() || !getAfterDefaultAdShow() || shouldUseQuizPriority()) {
            notifyComplete(listener);
            return;
        }
        if (isAfterDefaultAppOpenType()) {
            if (tryShowPreloadedAfterDefaultAppOpen(activity, listener)) {
                return;
            }
            clearAfterDefaultPreloadCache();
            loadAppOpenAdInternal(activity, getAppOpenId(), listener);
            return;
        }
        if (getOtherInterstitialId().isEmpty()) {
            notifyComplete(listener);
            return;
        }
        if (tryShowPreloadedAfterDefaultInterstitial(activity, listener)) {
            return;
        }
        clearAfterDefaultPreloadCache();
        loadInterstitialAdInternal(activity, getOtherInterstitialId(), listener, false);
    }

    private static boolean tryShowPreloadedAfterDefaultInterstitial(Activity activity, @Nullable OnInterstitialAdListener listener) {
        if (preloadedAfterDefaultInterstitial == null || shouldUseQuizPriority()) {
            return false;
        }
        InterstitialAd ad = preloadedAfterDefaultInterstitial;
        preloadedAfterDefaultInterstitial = null;
        AtomicBoolean completed = new AtomicBoolean(false);
        ad.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                notifyComplete(listener, completed);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                notifyComplete(listener, completed);
            }
        });
        ad.show(activity);
        return true;
    }

    private static boolean tryShowPreloadedAfterDefaultAppOpen(Activity activity, @Nullable OnInterstitialAdListener listener) {
        if (preloadedAfterDefaultAppOpen == null || shouldUseQuizPriority()) {
            return false;
        }
        AppOpenAd ad = preloadedAfterDefaultAppOpen;
        preloadedAfterDefaultAppOpen = null;
        AtomicBoolean completed = new AtomicBoolean(false);
        ad.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                notifyComplete(listener, completed);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                notifyComplete(listener, completed);
            }
        });
        ad.show(activity);
        return true;
    }
