// LauncherHomeActivity.onCreate (fresh start)
        if (savedInstanceState == null) {
            com.qrcode.scanner.launcher.common.AnalyticsEvents.log(this, com.qrcode.scanner.launcher.common.AnalyticsEvents.LAUNCHER_HOME_VIEW);
        }
        LauncherSettingsHelper.registerChangeListener(drawerSettingsListener);
        hideLauncherFromRecents();
        if (savedInstanceState == null) {
            // Fresh launch (app was closed / removed from Recents): not a return from an external app.
            AdPlacement.clearLauncherAppReturnPending(this);
        }
        pendingSavedInstanceState = savedInstanceState;
        if (shouldHandleAfterDefaultSetup()) {
            completeAfterDefaultSetup();
            return;

// LauncherHomeActivity.onResume
        com.qrcode.scanner.launcher.helpers.DefaultHomePromptHelper.reportDefaultSetIfRequested(this);
        AdPlacement.prepareRightSwipePreload(this);
        hideLauncherFromRecents();
        if (shouldHandleAfterDefaultSetup()) {
            completeAfterDefaultSetup();
            return;
        }
        AdPlacement.handleLauncherAppReturnAd(this);
        if (vpLauncher != null && isSidePage(vpLauncher.getCurrentItem())) {
            qrPageNavigationHidden = true;
            LauncherQrSystemBars.INSTANCE.hideNavigationBarUntilSwipe(this);
