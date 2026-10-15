    public static void prepareRightSwipePreload(@Nullable Context context) {
        if (context instanceof Activity) {
            rightSwipeHost = new WeakReference<>((Activity) context);
        }
        Activity host = rightSwipeHost == null ? null : rightSwipeHost.get();
        if (!isRightSwipePreloadType()) {
            clearRightSwipePreload();
            return;
        }
        preloadRightSwipeAd(host);
    }

    public static void loadRightSwipeInterstitialAd(Activity activity, @Nullable OnInterstitialAdListener onFinished) {
        if (!RemoteConfigValues.getRightSwipeInterstitialAdShow()) {
            notifyComplete(onFinished);
            return;
        }
        int threshold = RemoteConfigValues.getRightSwipeInterstitial();
        if (threshold > 0) {
            if (threshold == rightSwipeInterstitialCount) {
                rightSwipeInterstitialCount = 1;
            } else {
                rightSwipeInterstitialCount++;
                notifyComplete(onFinished);
                return;
            }
        }
        if (isRightSwipePreloadType() && preloadedRightSwipeAd != null && !shouldUseQuizPriority()) {
            InterstitialAd ready = preloadedRightSwipeAd;
            preloadedRightSwipeAd = null;
            showReadyRightSwipeAd(activity, ready, onFinished);
            return;
        }
        loadRightSwipeNow(activity, onFinished);
    }

    private static boolean isRightSwipePreloadType() {
        return RemoteConfigValues.getRightSwipeInterstitialAdShow()
                && "preload".equalsIgnoreCase(RemoteConfigValues.getRightSwipeAdsType().trim());
    }

    private static void loadRightSwipeNow(Activity activity, @Nullable OnInterstitialAdListener onFinished) {
        loadInterstitialAdInternal(activity, RemoteConfigValues.getInterAdsId(), () -> {
            if (isRightSwipePreloadType()) {
                preloadRightSwipeAd(activity);
            }
            notifyComplete(onFinished);
        }, true);
    }

    private static void preloadRightSwipeAd(@Nullable Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || !isRightSwipePreloadType() || preloadedRightSwipeAd != null || rightSwipePreloadInFlight || shouldUseQuizPriority()) {
            return;
        }
        initializeIfConfigured(activity);
        String unitId = RemoteConfigValues.getInterAdsId().trim();
        if (!canLoad(activity, unitId)) {
            return;
        }
        int token = ++rightSwipePreloadToken;
        rightSwipePreloadInFlight = true;
        AnalyticsEvents.ad(activity, AnalyticsEvents.INTERSTITIAL_PRELOAD, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
        InterstitialAd.load(activity, unitId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                AnalyticsEvents.ad(activity, AnalyticsEvents.INTERSTITIAL_LOAD, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
                if (token != rightSwipePreloadToken || !isRightSwipePreloadType()) {
                    rightSwipePreloadInFlight = false;
                    return;
                }
                rightSwipePreloadInFlight = false;
                preloadedRightSwipeAd = ad;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                AnalyticsEvents.ad(activity, AnalyticsEvents.INTERSTITIAL_FAIL, AnalyticsEvents.FORMAT_INTERSTITIAL, unitId);
                if (token == rightSwipePreloadToken) {
                    rightSwipePreloadInFlight = false;
                }
            }
        });
    }

    private static void clearRightSwipePreload() {
        rightSwipePreloadToken++;
        rightSwipePreloadInFlight = false;
        preloadedRightSwipeAd = null;
    }

    private static void showReadyRightSwipeAd(Activity activity, InterstitialAd ad, @Nullable OnInterstitialAdListener listener) {
        AtomicBoolean completed = new AtomicBoolean(false);
        ad.setOnPaidEventListener(adValue -> logAdRevenue(activity, adValue));
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                preloadRightSwipeAd(activity);
                notifyComplete(listener, completed);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                loadRightSwipeNow(activity, listener);
            }
        });
        activity.getWindow().getDecorView().post(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                notifyComplete(listener, completed);
                return;
            }
            try {
                ad.show(activity);
            } catch (Exception ignored) {
                loadRightSwipeNow(activity, listener);
            }
        });
    }
