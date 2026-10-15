    private boolean shouldHandleAfterDefaultSetup() {
        return AppUtils.isCompletingDefaultAppSetup(this) && AppUtils.isDefaultHomeApp(this);
    }

    private void completeAfterDefaultSetup() {
        overridePendingTransition(0, 0);
        if (!AppUtils.tryClaimAfterDefaultFlow(this)) {
            finish();
            return;
        }
        AppUtils.setAwaitingDefaultRoleResult(this, false);
        AppUtils.navigateAfterDefaultAppSetup(this);
    }
