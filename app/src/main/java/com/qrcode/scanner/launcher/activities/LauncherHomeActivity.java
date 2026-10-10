package com.qrcode.scanner.launcher.activities;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.adapters.LauncherPagerAdapter;
import com.qrcode.scanner.launcher.dialogs.LauncherAppsBottomSheet;
import com.qrcode.scanner.launcher.fragments.LauncherHomeFragment;
import com.qrcode.scanner.launcher.fragments.LauncherQrPageFragment;
import com.qrcode.scanner.launcher.fragments.LauncherQrSystemBars;
import com.qrcode.scanner.launcher.fragments.SubContainerFragment;
import com.qrcode.scanner.launcher.helpers.LauncherAppsHelper;
import com.qrcode.scanner.launcher.helpers.LauncherSettingsHelper;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.InstallReferrerStartup;
import com.qrcode.scanner.launcher.common.WidgetNavigation;
import com.qrcode.scanner.launcher.helpers.ThemeHelper;
import com.qrcode.scanner.launcher.remote.RemoteConfigHelper;
import com.qrcode.scanner.launcher.models.LauncherAppsModel;
import com.qrcode.scanner.ui.navigation.ComposeScanRequest;
import com.qrcode.scanner.ui.screens.create.CreateActivity;
import com.qrcode.scanner.ui.screens.history.HistoryActivity;
import com.qrcode.scanner.ui.screens.scan.ScannerActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LauncherHomeActivity extends AppCompatActivity {
    private static final String PREFS_LAUNCHER_REMOTE = "launcher_remote_config";
    private static final String KEY_LAST_REMOTE_FETCH_TIME = "last_remote_fetch_time";
    private static final String KEY_RESTORE_LAUNCHER_PAGE = "restore_launcher_page";
    private static final String KEY_LAUNCHER_PAGER_VERSION = "launcher_pager_version";
    private static final int LAUNCHER_PAGER_VERSION_RIGHT_SLOT = 2;
    private static final String VIEWPAGER_FRAGMENT_TAG_PREFIX = "f";

    private ViewPager2 vpLauncher;
    private boolean navigatingToHome;
    private boolean qrPageNavigationHidden;
    private boolean launcherPagerSwipeAllowed = true;
    @Nullable
    private Bundle pendingSavedInstanceState;
    private final ExecutorService appsExecutor = Executors.newSingleThreadExecutor();

    /** Marks the home task hidden in Recent Apps without closing or reopening the screen. */
    private void hideLauncherFromRecents() {
        try {
            ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (activityManager == null) {
                return;
            }
            String mainName = "com.qrcode.scanner.MainActivity";
            for (ActivityManager.AppTask appTask : activityManager.getAppTasks()) {
                ActivityManager.RecentTaskInfo info = appTask.getTaskInfo();
                if (info != null && isHomeComponent(info.baseActivity, mainName) && !isLauncherHomeTask(info, LauncherHomeActivity.class.getName())) {
                    continue;
                }
                appTask.setExcludeFromRecents(true);
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean isLauncherHomeTask(ActivityManager.RecentTaskInfo info, String homeName) {
        if (isHomeComponent(info.baseActivity, homeName) || isHomeComponent(info.topActivity, homeName) || isHomeComponent(info.origActivity, homeName)) {
            return true;
        }
        Intent baseIntent = info.baseIntent;
        return baseIntent != null && isHomeComponent(baseIntent.getComponent(), homeName);
    }

    private static boolean isHomeComponent(@androidx.annotation.Nullable ComponentName component, String homeName) {
        return component != null && homeName.equals(component.getClassName());
    }

    public static List<LauncherAppsModel> arrayListApps = Collections.synchronizedList(new ArrayList<>());
    public static List<LauncherAppsModel> arrayListAppsSearch = Collections.synchronizedList(new ArrayList<>());
    private final LauncherSettingsHelper.ChangeListener drawerSettingsListener = this::onDrawerSettingsChanged;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LauncherSettingsHelper.registerChangeListener(drawerSettingsListener);
        hideLauncherFromRecents();
        pendingSavedInstanceState = savedInstanceState;
        if (shouldHandleAfterDefaultSetup()) {
            completeAfterDefaultSetup();
            return;
        }
        LauncherQrSystemBars.INSTANCE.showTransparentNavigationBar(this);
        if (com.qrcode.scanner.ui.navigation.ThemeNavigation.INSTANCE.isReopenSettingsPending()) {
            // Recreated by a theme change and Settings reopens: paint the header colour now, not after Compose.
            LauncherQrSystemBars.INSTANCE.applyHeaderStatusBar(this,
                    com.qrcode.scanner.data.settings.SettingsRepositoryKt.readAppNightMode(this)
                            == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        }
        setContentView(R.layout.activity_launcher_home);
        setupLauncherHome();
        WidgetNavigation.openComposeDestinationFromIntent(this);
        try {
            InstallReferrerStartup.start(this);
        } catch (Exception ignored) {
        }
        RemoteConfigHelper.fetchRemoteConfig(this, this::saveRemoteFetchTimestampIfSuccessful);
    }

    private void saveRemoteFetchTimestampIfSuccessful(boolean success) {
        if (!success) {
            return;
        }
        getSharedPreferences(PREFS_LAUNCHER_REMOTE, MODE_PRIVATE).edit().putLong(KEY_LAST_REMOTE_FETCH_TIME, System.currentTimeMillis()).apply();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (vpLauncher != null) {
            outState.putInt(KEY_LAUNCHER_PAGER_VERSION, LAUNCHER_PAGER_VERSION_RIGHT_SLOT);
            outState.putInt(KEY_RESTORE_LAUNCHER_PAGE, vpLauncher.getCurrentItem());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        } else if (vpLauncher != null) {
            qrPageNavigationHidden = false;
            LauncherQrSystemBars.INSTANCE.showTransparentNavigationBar(this);
        }
        RemoteConfigHelper.refreshIfDue(this, this::saveRemoteFetchTimestampIfSuccessful);
        LauncherAppsBottomSheet.clearSuppressBackgroundDismiss();
        returnHomeForThemeChangeIfNeeded();
        if (ComposeScanRequest.INSTANCE.isPending()) {
            openQrShell();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        WidgetNavigation.openComposeDestinationFromIntent(this);
        handleLauncherHomeIntent(intent);
    }

    @Override
    protected void onPause() {
        hideLauncherFromRecents();
        super.onPause();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        hideLauncherFromRecents();
        dismissAppsBottomSheetIfAllowed();
        dismissAppsFolderDialogOnly();
    }

    @Override
    protected void onStop() {
        hideLauncherFromRecents();
        dismissAppsBottomSheetIfAllowed();
        super.onStop();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (vpLauncher != null && vpLauncher.getCurrentItem() == LauncherPagerAdapter.PAGE_HOME) {
            LauncherHomeFragment homeFragment = findLauncherHomeFragment();
            if (homeFragment != null && homeFragment.onHostTouchEvent(ev)) {
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onDestroy() {
        LauncherSettingsHelper.unregisterChangeListener(drawerSettingsListener);
        SubContainerFragment.clearCachedNativeAd();
        appsExecutor.shutdownNow();
        super.onDestroy();
    }

    private void onDrawerSettingsChanged(int changeMask) {
        if ((changeMask & LauncherSettingsHelper.CHANGE_SERIALIZE) == 0) {
            return;
        }
        runOnUiThread(() -> {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(LauncherAppsBottomSheet.TAG);
            boolean drawerVisible = fragment instanceof LauncherAppsBottomSheet
                    && fragment.isAdded()
                    && fragment.getView() != null;
            if (drawerVisible) {
                return;
            }
            synchronized (arrayListAppsSearch) {
                LauncherAppsHelper.sortApps(this, arrayListAppsSearch);
                arrayListApps.clear();
                arrayListApps.addAll(arrayListAppsSearch);
            }
        });
    }

    public void setLauncherSwipeEnabled(boolean enabled) {
        launcherPagerSwipeAllowed = enabled;
        applyLauncherPagerInputPolicy();
    }

    private void syncQrCamera(boolean active) {
        Fragment qrPage = getSupportFragmentManager().findFragmentByTag(
                VIEWPAGER_FRAGMENT_TAG_PREFIX + LauncherPagerAdapter.PAGE_RIGHT
        );
        if (qrPage instanceof LauncherQrPageFragment) {
            ((LauncherQrPageFragment) qrPage).setCameraActive(active);
        }
    }

    private boolean isSidePage(int position) {
        return position == LauncherPagerAdapter.PAGE_RIGHT || position == LauncherPagerAdapter.PAGE_SUB;
    }

    private void showQrPageNavigation(boolean hidden) {
        if (qrPageNavigationHidden == hidden) {
            return;
        }
        qrPageNavigationHidden = hidden;
        if (hidden) {
            LauncherQrSystemBars.INSTANCE.hideNavigationBarUntilSwipe(this);
        } else {
            LauncherQrSystemBars.INSTANCE.restore(this);
        }
    }

    private void applyLauncherPagerInputPolicy() {
        if (vpLauncher == null) {
            return;
        }
        vpLauncher.setUserInputEnabled(launcherPagerSwipeAllowed);
    }

    public boolean isLauncherPagerScrolling() {
        return vpLauncher != null && vpLauncher.getScrollState() != ViewPager2.SCROLL_STATE_IDLE;
    }

    public void recoverPagerScrollStateIfNeeded() {
        if (vpLauncher == null) {
            return;
        }
        applyLauncherPagerInputPolicy();
        if (vpLauncher.getScrollState() == ViewPager2.SCROLL_STATE_DRAGGING) {
            vpLauncher.setCurrentItem(vpLauncher.getCurrentItem(), true);
        }
    }

    public int getLauncherCurrentItem() {
        return vpLauncher == null ? LauncherPagerAdapter.PAGE_HOME : vpLauncher.getCurrentItem();
    }

    public void openQrShell() {
        if (vpLauncher == null || isFinishing() || isDestroyed()) {
            return;
        }
        if (vpLauncher.getCurrentItem() != LauncherPagerAdapter.PAGE_RIGHT) {
            // The QR page keeps its last tab (e.g. Settings); the Scan icon must always land on Scan.
            ComposeScanRequest.INSTANCE.request();
            vpLauncher.setCurrentItem(LauncherPagerAdapter.PAGE_RIGHT, true);
        }
    }

    public void openScanner() {
        startActivity(new Intent(this, ScannerActivity.class));
    }

    public void openCreate() {
        startActivity(new Intent(this, CreateActivity.class));
    }

    public void openHistory() {
        startActivity(new Intent(this, HistoryActivity.class));
    }

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

    private void setupLauncherHome() {
        vpLauncher = findViewById(R.id.vpLauncher);
        setupViewPager();
        preloadInstalledApps();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (requestAppsBottomSheetAnimatedDismiss()) {
                    return;
                }
                navigateToLauncherHomeIfNeeded();
            }
        });
    }

    private void handleLauncherHomeIntent(@Nullable Intent intent) {
        dismissLauncherOverlays();
        if (returnHomeForThemeChangeIfNeeded()) {
            return;
        }
        if (intent == null) {
            return;
        }
        boolean isHomeIntent = Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_HOME);
        if (!isHomeIntent) {
            return;
        }
        if (ThemeHelper.consumeThemeChangedPendingForHome(this)) {
            returnToLauncherHome(false);
            return;
        }
        if (vpLauncher != null && vpLauncher.getCurrentItem() != LauncherPagerAdapter.PAGE_HOME) {
            returnToLauncherHome(false);
        }
    }

    private boolean returnHomeForThemeChangeIfNeeded() {
        if (!ThemeHelper.consumeLauncherReturnHomeOnResume(this)) {
            return false;
        }
        ThemeHelper.clearThemeChangedPending(this);
        returnToLauncherHome(false);
        return true;
    }

    private void dismissAppsBottomSheetIfAllowed() {
        if (!LauncherAppsBottomSheet.shouldSuppressBackgroundDismiss()) {
            requestAppsBottomSheetBackgroundDismiss();
        }
    }

    @Nullable
    private LauncherAppsBottomSheet findShowingAppsBottomSheet() {
        Fragment sheet = getSupportFragmentManager().findFragmentByTag(LauncherAppsBottomSheet.TAG);
        if (!(sheet instanceof LauncherAppsBottomSheet) || !sheet.isAdded()) {
            return null;
        }
        LauncherAppsBottomSheet appsBottomSheet = (LauncherAppsBottomSheet) sheet;
        Dialog dialog = appsBottomSheet.getDialog();
        if (dialog == null || !dialog.isShowing()) {
            return null;
        }
        return appsBottomSheet;
    }

    private boolean requestAppsBottomSheetAnimatedDismiss() {
        LauncherAppsBottomSheet appsBottomSheet = findShowingAppsBottomSheet();
        if (appsBottomSheet == null) {
            return false;
        }
        appsBottomSheet.requestAnimatedDismiss();
        return true;
    }

    private void requestAppsBottomSheetBackgroundDismiss() {
        LauncherAppsBottomSheet appsBottomSheet = findShowingAppsBottomSheet();
        if (appsBottomSheet != null) {
            appsBottomSheet.dismissForBackgroundLeave();
        }
    }

    @Nullable
    private LauncherHomeFragment findLauncherHomeFragment() {
        Fragment homeFragment = getSupportFragmentManager().findFragmentByTag(VIEWPAGER_FRAGMENT_TAG_PREFIX + LauncherPagerAdapter.PAGE_HOME);
        if (homeFragment instanceof LauncherHomeFragment) {
            return (LauncherHomeFragment) homeFragment;
        }
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof LauncherHomeFragment && fragment.isAdded()) {
                return (LauncherHomeFragment) fragment;
            }
        }
        return null;
    }

    private void dismissAppsFolderDialogOnly() {
        LauncherHomeFragment homeFragment = findLauncherHomeFragment();
        if (homeFragment != null) {
            homeFragment.dismissAppsFolderDialog();
        }
    }

    private void dismissLauncherOverlays() {
        dismissAppsBottomSheetIfAllowed();
        dismissAppsFolderDialogOnly();
    }

    private void navigateToLauncherHomeIfNeeded() {
        if (vpLauncher == null) {
            return;
        }
        if (vpLauncher.getCurrentItem() == LauncherPagerAdapter.PAGE_HOME) {
            return;
        }
        if (navigatingToHome) {
            return;
        }
        returnToLauncherHome(true);
    }

    private void returnToLauncherHome(boolean smoothScroll) {
        if (vpLauncher == null) {
            return;
        }
        if (vpLauncher.getCurrentItem() == LauncherPagerAdapter.PAGE_HOME) {
            navigatingToHome = false;
            return;
        }
        navigatingToHome = true;
        vpLauncher.setCurrentItem(LauncherPagerAdapter.PAGE_HOME, smoothScroll);
        syncQrCamera(false);
        applyLauncherPagerInputPolicy();
    }

    private void setupViewPager() {
        vpLauncher.setSaveEnabled(false);
        vpLauncher.setAdapter(new LauncherPagerAdapter(this));
        vpLauncher.setOffscreenPageLimit(1);

        int restoreLauncherPage = LauncherPagerAdapter.PAGE_HOME;
        if (pendingSavedInstanceState != null && pendingSavedInstanceState.containsKey(KEY_RESTORE_LAUNCHER_PAGE)) {
            restoreLauncherPage = pendingSavedInstanceState.getInt(KEY_RESTORE_LAUNCHER_PAGE, LauncherPagerAdapter.PAGE_HOME);
            int pagerVersion = pendingSavedInstanceState.getInt(KEY_LAUNCHER_PAGER_VERSION, 1);
            if (pagerVersion < LAUNCHER_PAGER_VERSION_RIGHT_SLOT) {
                restoreLauncherPage = restoreLauncherPage == 1
                        ? LauncherPagerAdapter.PAGE_SUB
                        : LauncherPagerAdapter.PAGE_HOME;
            }
            if (restoreLauncherPage < 0 || restoreLauncherPage >= LauncherPagerAdapter.PAGE_COUNT) {
                restoreLauncherPage = LauncherPagerAdapter.PAGE_HOME;
            }
        }
        pendingSavedInstanceState = null;
        final int initialPage = restoreLauncherPage;
        vpLauncher.setCurrentItem(initialPage, false);
        if (isSidePage(initialPage)) {
            showQrPageNavigation(true);
        }
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
    }

    private void notifyRightSwipeTutorialCompleted() {
        LauncherHomeFragment homeFragment = findLauncherHomeFragment();
        if (homeFragment != null) {
            homeFragment.onRightSwipeNavigationCompleted();
        }
    }

    private void preloadInstalledApps() {
        if (!arrayListAppsSearch.isEmpty()) {
            return;
        }
        appsExecutor.execute(() -> {
            ArrayList<LauncherAppsModel> loaded = LauncherAppsHelper.loadInstalledApps(getApplicationContext());
            LauncherAppsHelper.replaceAppLists(loaded);
        });
    }
}
