    private void updateRightSwipeTutorialVisibility() {
        if (!isAdded()) {
            return;
        }
        boolean tutorialShown = LauncherSettingsHelper.isRightSwipeTutorialShown(requireContext());
        if (llRightSwipe != null) {
            llRightSwipe.setVisibility(tutorialShown ? GONE : VISIBLE);
        }
        if (llDateTime != null) {
            llDateTime.setVisibility(tutorialShown ? VISIBLE : GONE);
        }
    }

    public void maybeShowDefaultHomePopup() {
        if (!isAdded()) {
            return;
        }
        defaultHomePromptHelper.maybeShowDefaultHomePopup();
    }

    /** True when the Default Home permission screen would be shown now (the Right Swipe ad is skipped then). */
    public boolean canShowDefaultHomePopup() {
        return isAdded() && defaultHomePromptHelper.canShowDefaultHomePopup();
    }

    public void onRightSwipeNavigationCompleted() {
        if (!isAdded()) {
            return;
        }
        if (LauncherSettingsHelper.isRightSwipeTutorialShown(requireContext())) {
            return;
        }
        LauncherSettingsHelper.setRightSwipeTutorialShown(requireContext(), true);
        updateRightSwipeTutorialVisibility();
    }
