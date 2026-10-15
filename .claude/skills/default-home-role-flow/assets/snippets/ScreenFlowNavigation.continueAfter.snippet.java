    public static void continueAfter(Activity activity, String completedScreen) {
        if (activity == null) {
            return;
        }
        Context appContext = activity.getApplicationContext();
        ensureFlow(appContext);
        List<String> flow = ScreenFlowConfig.getShowScreenFlow();
        int completedIndex = indexOfScreen(flow, completedScreen);
        // Clear only screens the flow has not passed yet; a late/duplicate callback of an earlier
        // screen must never send the user back through screens that are already done.
        int passed = AppUtils.getFlowProgress(appContext);
        markScreenCompleted(appContext, completedScreen);
        clearCompletionsAfter(appContext, flow, completedIndex < 0 ? completedIndex : Math.max(completedIndex, passed - 1));
        if (completedIndex >= 0) {
            AppUtils.advanceFlowProgress(appContext, completedIndex + 1);
        }

        int startIndex = completedIndex >= 0 ? completedIndex + 1 : 0;
        if (activity.isFinishing() || activity.isDestroyed()) {
            openFromIndex(appContext, startIndex);
            return;
        }
        openFromIndex(activity, startIndex);
    }

    private static void openFromIndex(Activity activity, int startIndex) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        openFromIndex((Context) activity, startIndex);
    }
