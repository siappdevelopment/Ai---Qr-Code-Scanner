// LauncherAppsAdapter - app icon click (drawer / grid)
                return;
            }
            LauncherAppsDisplayItem clicked = displayItems.get(adapterPosition);
            if (clicked.getType() != LauncherAppsDisplayItem.TYPE_APP || clicked.getApp() == null) {
                return;
            }
            String packageName = clicked.getApp().getPackageName();
            if (context instanceof Activity) {
                LauncherAppsBottomSheet.markSuppressBackgroundDismiss();
                AdPlacement.handleLauncherAppClickAd((Activity) context, () -> launchApp(packageName));
            } else {
                launchApp(packageName);
            }
        });
        appHolder.itemView.setOnLongClickListener(v -> {
            if (longClickListener == null) {

// LauncherAppsAdapter.launchApp - mark the external launch

    private void launchApp(String packageName) {
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                LauncherAppsBottomSheet.markSuppressBackgroundDismiss();
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                AdPlacement.markLauncherExternalAppLaunched(context, packageName);
            }
        } catch (Exception ignored) {
        }
    }

    @NonNull
    private String resolveDisplayAppName(@NonNull LauncherAppsModel model) {
