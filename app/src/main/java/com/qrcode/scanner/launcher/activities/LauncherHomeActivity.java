package com.qrcode.scanner.launcher.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.qrcode.scanner.R;
import com.qrcode.scanner.launcher.adapters.LauncherPagerAdapter;
import com.qrcode.scanner.launcher.dialogs.LauncherAppsBottomSheet;
import com.qrcode.scanner.launcher.fragments.LauncherHomeFragment;
import com.qrcode.scanner.launcher.fragments.SubContainerFragment;
import com.qrcode.scanner.launcher.helpers.LauncherAppsHelper;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.InstallReferrerStartup;
import com.qrcode.scanner.launcher.common.WidgetNavigation;
import com.qrcode.scanner.launcher.helpers.ThemeHelper;
import com.qrcode.scanner.launcher.remote.RemoteConfigHelper;
import com.qrcode.scanner.launcher.models.LauncherAppsModel;
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
    private boolean launcherPagerSwipeAllowed = true;
    @Nullable
    private Bundle pendingSavedInstanceState;
    private final ExecutorService appsExecutor = Executors.newSingleThreadExecutor();

    public static List<LauncherAppsModel> arrayListApps = Collections.synchronizedList(new ArrayList<>());
    public static List<LauncherAppsModel> arrayListAppsSearch = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pendingSavedInstanceState = savedInstanceState;
        if (shouldHandleAfterDefaultSetup()) {
            completeAfterDefaultSetup();
            return;
        }
        EdgeToEdge.enable(this);
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
        if (shouldHandleAfterDefaultSetup()) {
            completeAfterDefaultSetup();
            return;
        }
        AdPlacement.handleLauncherAppReturnAd(this);
        LauncherAppsBottomSheet.clearSuppressBackgroundDismiss();
        returnHomeForThemeChangeIfNeeded();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        WidgetNavigation.openComposeDestinationFromIntent(this);
        handleLauncherHomeIntent(intent);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        dismissAppsBottomSheetIfAllowed();
        dismissAppsFolderDialogOnly();
    }

    @Override
    protected void onStop() {
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
        SubContainerFragment.clearCachedNativeAd();
        appsExecutor.shutdownNow();
        super.onDestroy();
    }

    public void setLauncherSwipeEnabled(boolean enabled) {
        launcherPagerSwipeAllowed = enabled;
        applyLauncherPagerInputPolicy();
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
        vpLauncher.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private int lastSelectedPage = initialPage;
            private boolean pendingRightSwipeOpen;

            @Override
            public void onPageSelected(int position) {
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
                    if (pendingRightSwipeOpen && vpLauncher.getCurrentItem() == LauncherPagerAdapter.PAGE_RIGHT) {
                        pendingRightSwipeOpen = false;
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
