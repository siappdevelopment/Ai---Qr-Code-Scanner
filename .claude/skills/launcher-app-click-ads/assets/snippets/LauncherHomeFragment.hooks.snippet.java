// LauncherHomeFragment - click ad wrapper
        setupSwipeAwareClick(llHistory, () -> navigateViaRightSwipeFlow(this::openHistory));
        setupSwipeAwareClick(llSetting, () -> navigateViaRightSwipeFlow(this::openSettingsTodo));
        setupSwipeAwareClick(llCreateQR, () -> navigateViaRightSwipeFlow(this::openCreate));
        setupSwipeAwareClick(llSettings, () -> navigateViaRightSwipeFlow(this::openSettingsTodo));
        setupSwipeAwareClick(ivAppIcon1, () -> runWithLauncherClickAd(this::launchDialerApp));
        setupSwipeAwareClick(ivAppIcon2, () -> runWithLauncherClickAd(this::launchSmsApp));
        setupSwipeAwareClick(ivAppIcon3, () -> runWithLauncherClickAd(this::launchBrowserApp));
        setupSwipeAwareClick(ivAppIcon4, () -> runWithLauncherClickAd(this::launchCameraApp));

        attachSwipeAwareTouchListeners(llRightSwipe, llDateTime, cvGoogleFolder, cvToolsFolder, gvGoogleApps, gvToolsApps, tvDay, tvDate, tvTime, tvGoogle, tvTools, tvTopAppName1, tvTopAppName2, ivTopAppIcon1, ivTopAppIcon2);
    }

    private void runWithLauncherClickAd(@NonNull Runnable action) {
        if (!isAdded()) {
            return;
        }
        AdPlacement.handleLauncherAppClickAd(requireActivity(), action);
    }


// LauncherHomeFragment - external launch + mark
                if (info == null || info.activityInfo == null) {
                    return false;
                }
                launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            }
            startActivity(launch);
            String packageName = launch.getPackage();
            if (packageName == null && launch.getComponent() != null) {
                packageName = launch.getComponent().getPackageName();
            }
            AdPlacement.markLauncherExternalAppLaunched(requireContext(), packageName == null || packageName.trim().isEmpty() ? launch.getAction() : packageName);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    @Nullable
    private ResolveInfo preferredApp(@NonNull PackageManager packageManager, @NonNull Intent intent) {
        ResolveInfo resolved = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (isConcreteApp(resolved)) {
