    private RelativeLayout rlNativeListAdView;
    private ShimmerFrameLayout slNativeListShimmer;
    private FrameLayout flNativeListAd;
    private boolean nativeListAdLoadInProgress;
    private int nativeListAdRequestToken;
    private boolean nativeListAdVisible;
    @Nullable
    private NativeAd loadedNativeListAd;
    /**
    // ... (more fields: flNativeListAd, nativeListAdVisible, ...) ...
    private static final String NATIVE_LIST_AD_TYPE = "medium";

    private void showAd() {
        maybeLoadNativeListAd();
    }

    private boolean shouldShowNativeListAd() {
        if (!isAdded()) {
            return false;
        }
        if (AdPlacement.shouldUseQuizPriority()) {
            return AdPlacement.getLauncherAppNativeListAdShow();
        }
        if (!AdPlacement.getLauncherAppNativeListAdShow()) {
            return false;
        }
        if (!AdPlacement.canShowLauncherAppNativeListAd(requireContext())) {
            return false;
        }
        String nativeId = AdPlacement.getLauncherAppNativeListId();
        return AdPlacement.isNetworkAvailable(requireContext()) && nativeId != null && !nativeId.isEmpty() && AdPlacement.canRequestAds(requireContext());
    }

    private boolean hasCachedNativeListAd() {
        if (cachedNativeListAd == null || AdPlacement.shouldUseQuizPriority() || !AdPlacement.getLauncherAppNativeListAdShow()) {
            return false;
        }
        if (System.currentTimeMillis() - cachedNativeListAdAt > CACHED_NATIVE_LIST_AD_MAX_AGE_MS) {
            cachedNativeListAd.destroy();
            cachedNativeListAd = null;
            return false;
        }
        return true;
    }

    /** No new ad is due: show the last one again. Returns false when there is none (or it cannot be shown). */
    private boolean showCachedNativeListAd() {
        if (!isAdded() || rlNativeListAdView == null || slNativeListShimmer == null || flNativeListAd == null || !hasCachedNativeListAd()) {
            return false;
        }
        if (!AdPlacement.canRequestAds(requireContext())) {
            return false;
        }
        if (launcherAppsAdapter != null) {
            launcherAppsAdapter.setListNativePlaceholderEnabled(true);
            launcherAppsAdapter.setListNativeShimmerVisible(false);
        }
        if (!(nativeListAdVisible && flNativeListAd.getChildCount() > 0)) {
            AdPlacement.bindLoadedNative(requireActivity(), flNativeListAd, cachedNativeListAd, NATIVE_LIST_AD_TYPE);
            loadedNativeListAd = cachedNativeListAd;
            nativeListAdVisible = true;
            slNativeListShimmer.stopShimmer();
            slNativeListShimmer.setVisibility(GONE);
            applyNativeListBottomPadding(true);
        }
        revealNativeListAdAfterBind();
        return true;
    }

    private void maybeLoadNativeListAd() {
        if (rlNativeListAdView == null || slNativeListShimmer == null || flNativeListAd == null || !isAdded()) {
            return;
        }
        if (!shouldShowNativeListAd()) {
            if (showCachedNativeListAd()) {
                return;
            }
            hideNativeListAdContainer();
            if (launcherAppsAdapter != null) {
                launcherAppsAdapter.setListNativePlaceholderEnabled(false);
            }
            return;
        }
        // Quiz is sync — wait for the grid layout so sticky math matches Google's async path.
        if (AdPlacement.shouldUseQuizPriority() && rvApps != null && (rvApps.getHeight() <= 0 || !rvApps.isLaidOut())) {
            rvApps.post(this::maybeLoadNativeListAd);
            return;
        }
        if (launcherAppsAdapter != null) {
            launcherAppsAdapter.setListNativePlaceholderEnabled(true);
        }
        if (nativeListAdLoadInProgress) {
            if (launcherAppsAdapter != null) {
                launcherAppsAdapter.setListNativeShimmerVisible(true);
            }
            return;
        }
        if (nativeListAdVisible && flNativeListAd.getChildCount() > 0) {
            if (launcherAppsAdapter != null) {
                launcherAppsAdapter.setListNativeShimmerVisible(false);
            }
            revealNativeListAdAfterBind();
            return;
        }
        final int requestToken = ++nativeListAdRequestToken;
        nativeListAdLoadInProgress = true;
        destroyLoadedNativeListAd();
        flNativeListAd.removeAllViews();
        flNativeListAd.setVisibility(GONE);
        rlNativeListAdView.setVisibility(GONE);
        slNativeListShimmer.stopShimmer();
        slNativeListShimmer.setVisibility(GONE);
        if (launcherAppsAdapter != null) {
            launcherAppsAdapter.setListNativeShimmerVisible(true);
        }
        AdPlacement.loadNativeAd(requireActivity(), AdPlacement.getLauncherAppNativeListId(), rlNativeListAdView, slNativeListShimmer, flNativeListAd, NATIVE_LIST_AD_TYPE, nativeAd -> {
            nativeListAdLoadInProgress = false;
            if (!isActiveNativeListAdRequest(requestToken)) {
                if (nativeAd != null) {
                    nativeAd.destroy();
                }
                return;
            }
            if (nativeAd != null && cachedNativeListAd != null && cachedNativeListAd != nativeAd) {
                if (loadedNativeListAd == cachedNativeListAd) {
                    loadedNativeListAd = null;
                }
                cachedNativeListAd.destroy();
                cachedNativeListAd = null;
            }
            if (nativeAd != null) {
                if (loadedNativeListAd != null && loadedNativeListAd != nativeAd) {
                    loadedNativeListAd.destroy();
                }
                loadedNativeListAd = nativeAd;
                cachedNativeListAd = nativeAd;
                cachedNativeListAdAt = System.currentTimeMillis();
            }
            if (flNativeListAd.getChildCount() > 0) {
                nativeListAdVisible = true;
                if (launcherAppsAdapter != null) {
                    launcherAppsAdapter.setListNativeShimmerVisible(false);
                }
                slNativeListShimmer.stopShimmer();
                slNativeListShimmer.setVisibility(GONE);
                flNativeListAd.setVisibility(VISIBLE);
                AdPlacement.setLauncherAppNativeListLastShowTime(requireContext(), System.currentTimeMillis());
                applyNativeListBottomPadding(true);
                revealNativeListAdAfterBind();
            } else {
                destroyLoadedNativeListAd();
                hideNativeListAdContainer();
                if (launcherAppsAdapter != null) {
                    launcherAppsAdapter.setListNativePlaceholderEnabled(false);
                }
            }
        }, () -> {
            nativeListAdLoadInProgress = false;
            if (!isActiveNativeListAdRequest(requestToken)) {
                return;
            }
            destroyLoadedNativeListAd();
            hideNativeListAdContainer();
            if (launcherAppsAdapter != null) {
                launcherAppsAdapter.setListNativePlaceholderEnabled(false);
            }
        });
    }

    /** Park → show → align to list spacer (Quiz sync must not flash at bottom over apps). */
    private void revealNativeListAdAfterBind() {
        if (rlNativeListAdView == null) {
            return;
        }
        parkNativeListAdOffScreen();
        rlNativeListAdView.setVisibility(VISIBLE);
        updateStickyNativeListAdPosition();
        rlNativeListAdView.post(this::updateStickyNativeListAdPosition);
        if (AdPlacement.shouldUseQuizPriority()) {
            rlNativeListAdView.post(() -> rlNativeListAdView.post(this::updateStickyNativeListAdPosition));
        }
    }

    private boolean isActiveNativeListAdRequest(int requestToken) {
        return isAdded() && requestToken == nativeListAdRequestToken && rlNativeListAdView != null && slNativeListShimmer != null && flNativeListAd != null;
    }

    private void hideNativeListAdContainer() {
        nativeListAdVisible = false;
        if (launcherAppsAdapter != null) {
            launcherAppsAdapter.setListNativeShimmerVisible(false);
        }
        if (slNativeListShimmer != null) {
            slNativeListShimmer.stopShimmer();
            slNativeListShimmer.setVisibility(GONE);
        }
        if (flNativeListAd != null) {
            flNativeListAd.setVisibility(GONE);
            flNativeListAd.removeAllViews();
        }
        if (rlNativeListAdView != null) {
            rlNativeListAdView.setVisibility(GONE);
            rlNativeListAdView.setTranslationY(0f);
        }
        applyNativeListBottomPadding(false);
    }

    private void destroyLoadedNativeListAd() {
        if (loadedNativeListAd != null && loadedNativeListAd == cachedNativeListAd) {
            loadedNativeListAd = null; // kept in the cache for the next time the drawer opens
            return;
        }
        if (loadedNativeListAd != null) {
            loadedNativeListAd.destroy();
            loadedNativeListAd = null;
        }
    }

    /**
     * Ad hidden: give the list back its normal bottom space and drawing area. While the ad is shown, its real position
     * decides (see {@link #syncStickyListInset}): in its list slot the rows below it stay visible; floating at the
     * bottom it must not cover any rows.
     */
    private void applyNativeListBottomPadding(boolean enabled) {
        if (rvApps == null) {
            return;
        }
        if (!enabled) {
            applyStickyListInset(false, 0, 0);
            return;
        }
        syncStickyListInset();
    }

    /** Reads where the ad is now. At its natural bottom spot (translationY 0) it floats over the list: reserve that space. */
    private void syncStickyListInset() {
        if (rvApps == null) {
            return;
        }
        if (!nativeListAdVisible || rlNativeListAdView == null || rlNativeListAdView.getVisibility() != VISIBLE || rlNativeListAdView.getHeight() <= 0
                || !(rlNativeListAdView.getParent() instanceof View)) {
            applyStickyListInset(false, 0, 0);
            return;
        }
        View parent = (View) rlNativeListAdView.getParent();
        boolean floating = Math.abs(rlNativeListAdView.getTranslationY()) < 1f;
        if (!floating) {
            applyStickyListInset(false, 0, 0);
            return;
        }
        int marginBottom = getResources().getDimensionPixelSize(R.dimen.launcher_native_list_margin);
        int adTop = parent.getHeight() - rlNativeListAdView.getHeight() - marginBottom;
        int reserve = Math.max(0, rvApps.getBottom() - adTop);
        applyStickyListInset(true, reserve, adTop - rvApps.getTop());
    }

    private void applyStickyListInset(boolean floating, int reservePx, int clipBottomPx) {
        nativeListAdReservePx = floating ? reservePx : 0;
        int padding = Math.max(nativeListAdReservePx, lastImeBottomPx);
        if (rvApps.getPaddingBottom() != padding) {
            appliedRecyclerPaddingBottom = padding;
            rvApps.setPadding(rvApps.getPaddingLeft(), rvApps.getPaddingTop(), rvApps.getPaddingRight(), padding);
        }
        rvApps.setClipToPadding(false);
        if (floating && rvApps.getWidth() > 0) {
            rvApps.setClipBounds(new android.graphics.Rect(0, 0, rvApps.getWidth(), Math.max(0, clipBottomPx)));
        } else {
            rvApps.setClipBounds(null);
        }
    }

    private void parkNativeListAdOffScreen() {
        if (rlNativeListAdView == null) {
            return;
        }
        View parent = (View) rlNativeListAdView.getParent();
        int parentH = parent != null ? parent.getHeight() : 0;
        int adH = rlNativeListAdView.getHeight();
        rlNativeListAdView.setTranslationY(parentH > 0 ? parentH : Math.max(adH, 1) + 4000);
    }

    private void updateStickyNativeListAdPosition() {
        updateStickyNativeListAdPositionCore();
        syncStickyListInset();
    }

    private void updateStickyNativeListAdPositionCore() {
        if (!nativeListAdVisible || rlNativeListAdView == null || rvApps == null || launcherAppsAdapter == null) {
            return;
        }
        if (rlNativeListAdView.getVisibility() != VISIBLE) {
            return;
        }
        View parent = (View) rlNativeListAdView.getParent();
        if (parent == null) {
            return;
        }
        if (rlNativeListAdView.getHeight() <= 0 || parent.getHeight() <= 0) {
            parkNativeListAdOffScreen();
            rlNativeListAdView.post(this::updateStickyNativeListAdPosition);
            return;
        }

        int marginBottom = getResources().getDimensionPixelSize(R.dimen.launcher_native_list_margin);
        int stickyNaturalTop = parent.getHeight() - rlNativeListAdView.getHeight() - marginBottom;
        int placeholderPos = launcherAppsAdapter.findNativePlaceholderPosition();
        if (placeholderPos == RecyclerView.NO_POSITION) {
            placeNativeAdBelowVisibleApps(parent, marginBottom, stickyNaturalTop);
            return;
        }

        RecyclerView.ViewHolder holder = rvApps.findViewHolderForAdapterPosition(placeholderPos);
        if (holder == null) {
            GridLayoutManager layoutManager = rvApps.getLayoutManager() instanceof GridLayoutManager ? (GridLayoutManager) rvApps.getLayoutManager() : null;
            if (layoutManager != null && layoutManager.findFirstVisibleItemPosition() > placeholderPos) {
                // Scrolled past the slot — stick to bottom.
                rlNativeListAdView.setTranslationY(0f);
            } else {
                // Slot not laid out yet (still below fold) — keep off-screen, never flash at bottom.
                parkNativeListAdOffScreen();
                rlNativeListAdView.post(this::updateStickyNativeListAdPosition);
            }
            return;
        }

        int[] placeholderLoc = new int[2];
        int[] parentLoc = new int[2];
        holder.itemView.getLocationInWindow(placeholderLoc);
        parent.getLocationInWindow(parentLoc);
        int placeholderTop = placeholderLoc[1] - parentLoc[1];
        int placeholderBottom = placeholderTop + holder.itemView.getHeight();

        if (placeholderBottom <= 0) {
            rlNativeListAdView.setTranslationY(0f);
            return;
        }
        rlNativeListAdView.setTranslationY(Math.min(0f, placeholderTop - stickyNaturalTop));
    }

    /** A short search list has no in-list ad slot, so the ad must sit under the apps. */
    private void placeNativeAdBelowVisibleApps(View parent, int marginBottom, int stickyNaturalTop) {
        if (rvApps.getChildCount() == 0) {
            rlNativeListAdView.setTranslationY(0f);
            return;
        }
        int[] parentLoc = new int[2];
        parent.getLocationInWindow(parentLoc);
        int contentBottom = 0;
        for (int i = 0; i < rvApps.getChildCount(); i++) {
            View child = rvApps.getChildAt(i);
            int[] childLoc = new int[2];
            child.getLocationInWindow(childLoc);
            contentBottom = Math.max(contentBottom, childLoc[1] - parentLoc[1] + child.getHeight());
        }
        int targetTop = contentBottom + marginBottom;
        rlNativeListAdView.setTranslationY(Math.min(0f, targetTop - stickyNaturalTop));
    }
