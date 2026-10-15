        vpLauncher.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private int lastSelectedPage = initialPage;
            private boolean pendingRightSwipeOpen;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                boolean openingSubPage = position == LauncherPagerAdapter.PAGE_HOME && positionOffset > 0f;
                showQrPageNavigation(isSidePage(position) || openingSubPage);
                if (position == LauncherPagerAdapter.PAGE_RIGHT && positionOffset > 0f) {
                    // Swiping from the QR page to the wallpaper home: do not keep the header colour behind it.
                    LauncherQrSystemBars.INSTANCE.clearHeaderWindowBackground(LauncherHomeActivity.this);
                }
            }

            @Override
            public void onPageSelected(int position) {
                syncQrCamera(position == LauncherPagerAdapter.PAGE_RIGHT);
                if (position == LauncherPagerAdapter.PAGE_HOME) {
                    refreshWallpaperWindowIfNeeded();
                }
                if (lastSelectedPage == LauncherPagerAdapter.PAGE_RIGHT && position != LauncherPagerAdapter.PAGE_RIGHT) {
                    com.qrcode.scanner.launcher.common.HomeBottomAd.onPageHidden();
                }
                if (position == LauncherPagerAdapter.PAGE_RIGHT && lastSelectedPage != LauncherPagerAdapter.PAGE_RIGHT) {
                    showQrPageNavigation(true);
                    com.qrcode.scanner.launcher.common.HomeBottomAd.onPageVisible();
                }
                if (lastSelectedPage == LauncherPagerAdapter.PAGE_HOME && position == LauncherPagerAdapter.PAGE_RIGHT) {
                    pendingRightSwipeOpen = true;
                    notifyRightSwipeTutorialCompleted();
                }
                lastSelectedPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    navigatingToHome = false;
                    if (vpLauncher.getCurrentItem() == LauncherPagerAdapter.PAGE_RIGHT) {
                        LauncherQrSystemBars.INSTANCE.reapplyHeaderWindowBackground(LauncherHomeActivity.this);
                    }
                    if (pendingRightSwipeOpen && vpLauncher.getCurrentItem() == LauncherPagerAdapter.PAGE_RIGHT) {
                        pendingRightSwipeOpen = false;
                        LauncherHomeFragment popupFragment = findLauncherHomeFragment();
                        if (popupFragment != null && popupFragment.canShowDefaultHomePopup()) {
                            // The Default Home permission screen is due: no Right Swipe ad, show the permission screen.
                            popupFragment.maybeShowDefaultHomePopup();
                            return;
                        }
                        AdPlacement.loadRightSwipeInterstitialAd(LauncherHomeActivity.this, () -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            LauncherHomeFragment homeFragment = findLauncherHomeFragment();
                            if (homeFragment != null) {
                                homeFragment.maybeShowDefaultHomePopup();
                            }
                        });
                    }
                }
            }
        });
        applyLauncherPagerInputPolicy();
