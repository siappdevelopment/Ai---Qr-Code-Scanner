package com.qrcode.scanner.launcher.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.speech.RecognizerIntent;
import android.telecom.TelecomManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity;
import com.qrcode.scanner.launcher.adapters.LauncherAppsAdapter;
import com.qrcode.scanner.launcher.adapters.LauncherPagerAdapter;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.dialogs.LauncherAppsBottomSheet;
import com.qrcode.scanner.launcher.helpers.DefaultHomePromptHelper;
import com.qrcode.scanner.launcher.helpers.LauncherAppsIconCache;
import com.qrcode.scanner.launcher.activities.LauncherSettingsActivity;
import com.qrcode.scanner.launcher.helpers.LauncherSettingsHelper;
import com.qrcode.scanner.launcher.helpers.LocaleHelper;
import com.qrcode.scanner.launcher.models.LauncherAppsModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LauncherHomeFragment extends Fragment {
    private final DefaultHomePromptHelper defaultHomePromptHelper = new DefaultHomePromptHelper(this);
    private LauncherAppsBottomSheet appsBottomSheet;
    private RelativeLayout rlQuickAction;
    private LinearLayout llGoogleSearch, llRightSwipe, llDateTime, llDefault, llGoogleFolder, llToolsFolder, llScan, llCreate, llHistory, llSetting, llCreateQR, llSettings;
    private CardView cvGoogleFolder, cvToolsFolder;
    private GridLayout gvGoogleApps, gvToolsApps;
    private AppCompatImageView ivGoogleVoiceSearch, ivTopAppIcon1, ivTopAppIcon2, ivAppIcon1, ivAppIcon2, ivAppIcon3, ivAppIcon4;
    private AppCompatTextView tvDay, tvDate, tvTime, tvGoogle, tvTools, tvTopAppName1, tvTopAppName2;

    private static final String GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final String CHROME_PACKAGE = "com.android.chrome";
    @Nullable
    private String bottomCameraPackage;
    private static final long SHORTCUT_CLICK_DEBOUNCE_MS = 800L;
    private long lastShortcutLaunchElapsedMs;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
    private boolean timeReceiverRegistered;

    private static final int FOLDER_PREVIEW_COLUMNS = 3;
    private static final int FOLDER_PREVIEW_MAX_ICONS = 9;
    private static final int FOLDER_DIALOG_COLUMNS = 3;
    private static final float FOLDER_PREVIEW_PADDING_RATIO = 4f / 50f;
    private static final String[] GOOGLE_APP_PACKAGES = {"com.android.chrome", "com.google.android.apps.docs", "com.google.android.gm", "com.google.android.googlequicksearchbox", "com.google.android.apps.maps", "com.google.android.apps.photos", "com.google.android.youtube", "com.android.vending", "com.google.android.calendar"};
    private static final String[][] TOOLS_APP_GROUPS = {{"com.google.android.calculator", "com.sec.android.app.popupcalculator", "com.miui.calculator"}, {"com.google.android.deskclock", "com.sec.android.app.clockpackage", "com.android.deskclock"}, {"com.google.android.keep", "com.miui.notes", "com.samsung.android.app.notes"}, {"com.google.android.calendar", "com.android.calendar"}, {"com.google.android.documentsui", "com.mi.android.globalFileexplorer", "com.sec.android.app.myfiles", "com.coloros.filemanager"}, {"com.android.settings"}};
    private Dialog appsFolderDialog;

    private final ArrayList<LauncherAppsModel> arrayListGoogleApps = new ArrayList<>();
    private final ArrayList<LauncherAppsModel> arrayListToolsApps = new ArrayList<>();

    private int touchSlop;
    private int minFlingVelocity;
    private int maxFlingVelocity;
    private VelocityTracker velocityTracker;
    private float touchDownX;
    private float touchDownY;
    private boolean appsSwipeResolved;
    private boolean appsSheetDragging;
    private boolean appsSheetOpening;
    private boolean appsGestureConsumed;
    private boolean upEventConsumed;
    private boolean clickBlockedForGesture;
    private float swipeAwareDownX;
    private float swipeAwareDownY;
    private static final float HORIZONTAL_DOMINANCE_RATIO = 1.25f;
    private static final float VERTICAL_DOMINANCE_RATIO = 1.35f;
    private static final int GESTURE_AXIS_NONE = 0;
    private static final int GESTURE_AXIS_HORIZONTAL = 1;
    private static final int GESTURE_AXIS_VERTICAL = 2;
    private int lockedGestureAxis = GESTURE_AXIS_NONE;
    private boolean pagerSwipeTemporarilyDisabled;

    private LauncherSettingsHelper.ChangeListener launcherSettingsChangeListener;
    private String syncedLanguageCode;
    private LocaleHelper.LanguageChangeListener languageChangeListener;

    @Nullable
    private LauncherHomeActivity getLauncherHomeActivity() {
        return getActivity() instanceof LauncherHomeActivity ? (LauncherHomeActivity) getActivity() : null;
    }

    private boolean isFragmentReady() {
        return isAdded() && getContext() != null;
    }

    private int getLauncherIconSizePx() {
        return AppUtils.dpToPx(requireContext(), LauncherSettingsHelper.getAppIconSize(requireContext()));
    }

    private int getLauncherIconCornerRadiusPx() {
        return AppUtils.dpToPx(requireContext(), 20);
    }

    private int getFolderPreviewPaddingPx(int folderSizePx) {
        return Math.max(AppUtils.dpToPx(requireContext(), 2), Math.round(folderSizePx * FOLDER_PREVIEW_PADDING_RATIO));
    }

    private boolean isVerticalUpDominant(float dx, float dy) {
        return dy < 0f && Math.abs(dy) > Math.abs(dx) * VERTICAL_DOMINANCE_RATIO;
    }

    private float getAppsSheetProgress(float rawY) {
        return (touchDownY - rawY) / getAppsSheetOpenDistance();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        defaultHomePromptHelper.registerRoleLauncher();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_launcher_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        syncedLanguageCode = AppUtils.getLanguage(requireContext());
        languageChangeListener = this::refreshLayoutForLanguageChange;
        LocaleHelper.registerLanguageChangeListener(languageChangeListener);
        launcherSettingsChangeListener = changeMask -> {
            if (!isFragmentReady()) {
                return;
            }
            if ((changeMask & LauncherSettingsHelper.CHANGE_DISPLAY) != 0) {
                requireActivity().runOnUiThread(this::applyLauncherSizeSettings);
            }
        };
        LauncherSettingsHelper.registerChangeListener(launcherSettingsChangeListener);
        ViewConfiguration configuration = ViewConfiguration.get(requireContext());
        touchSlop = configuration.getScaledTouchSlop();
        minFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        maxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        findIDs(view);
    }

    private void refreshLayoutForLanguageChange() {
        if (!isAdded()) {
            return;
        }
        String languageCode = AppUtils.getLanguage(requireContext());
        if (TextUtils.equals(languageCode, syncedLanguageCode)) {
            return;
        }
        syncedLanguageCode = languageCode;
        LocaleHelper.syncResources(requireActivity());
        getParentFragmentManager().beginTransaction().detach(this).attach(this).commitNowAllowingStateLoss();
    }

    private void findIDs(View view) {
        rlQuickAction = view.findViewById(R.id.rlQuickAction);
        llGoogleSearch = view.findViewById(R.id.llGoogleSearch);
        ivGoogleVoiceSearch = view.findViewById(R.id.ivGoogleVoiceSearch);
        llRightSwipe = view.findViewById(R.id.llRightSwipe);
        llDateTime = view.findViewById(R.id.llDateTime);
        tvDay = view.findViewById(R.id.tvDay);
        tvDate = view.findViewById(R.id.tvDate);
        tvTime = view.findViewById(R.id.tvTime);
        llDefault = view.findViewById(R.id.llDefault);
//        rlSetAsDefault = view.findViewById(R.id.rlSetAsDefault);
        llGoogleFolder = view.findViewById(R.id.llGoogleFolder);
        cvGoogleFolder = view.findViewById(R.id.cvGoogleFolder);
        gvGoogleApps = view.findViewById(R.id.gvGoogleApps);
        tvGoogle = view.findViewById(R.id.tvGoogle);
        llToolsFolder = view.findViewById(R.id.llToolsFolder);
        cvToolsFolder = view.findViewById(R.id.cvToolsFolder);
        gvToolsApps = view.findViewById(R.id.gvToolsApps);
        tvTools = view.findViewById(R.id.tvTools);
        llScan = view.findViewById(R.id.llScan);
        llCreate = view.findViewById(R.id.llCreate);
        llHistory = view.findViewById(R.id.llHistory);
        llSetting = view.findViewById(R.id.llSetting);
        llCreateQR = view.findViewById(R.id.llCreateQR);
        llSettings = view.findViewById(R.id.llSettings);
        ivTopAppIcon1 = view.findViewById(R.id.ivTopAppIcon1);
        ivTopAppIcon2 = view.findViewById(R.id.ivTopAppIcon2);
        tvTopAppName1 = view.findViewById(R.id.tvTopAppName1);
        tvTopAppName2 = view.findViewById(R.id.tvTopAppName2);
        ivAppIcon1 = view.findViewById(R.id.ivAppIcon1);
        ivAppIcon2 = view.findViewById(R.id.ivAppIcon2);
        ivAppIcon3 = view.findViewById(R.id.ivAppIcon3);
        ivAppIcon4 = view.findViewById(R.id.ivAppIcon4);
        defaultHomePromptHelper.bind(llDefault);

        updateRightSwipeTutorialVisibility();
        setupClickListeners();
        applyLauncherSizeSettings();
        loadFolderApps();
        loadBottomApps();
    }

    private void setupClickListeners() {
        setupSwipeAwareClick(rlQuickAction, () -> navigateViaRightSwipeFlow(this::openQrShell));

        setupSwipeAwareClick(llGoogleSearch, () -> {
            Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(GOOGLE_APP_PACKAGE);
            if (intent != null) {
                startActivity(intent);
            }
        });

        setupSwipeAwareClick(ivGoogleVoiceSearch, () -> {
            if (!canLaunchShortcut()) {
                return;
            }
            launchGoogleVoiceSearch();
        });

        setupSwipeAwareClick(llDefault, defaultHomePromptHelper::handleSetAsDefaultClick);

        setupSwipeAwareClick(llGoogleFolder, () -> {
            if (!arrayListGoogleApps.isEmpty()) {
                dialogAppsFolder("Google", arrayListGoogleApps);
            }
        });

        setupSwipeAwareClick(llToolsFolder, () -> {
            if (!arrayListToolsApps.isEmpty()) {
                dialogAppsFolder("Tools", arrayListToolsApps);
            }
        });

        setupSwipeAwareClick(llScan, () -> navigateViaRightSwipeFlow(this::openScanner));
        setupSwipeAwareClick(llCreate, () -> navigateViaRightSwipeFlow(this::openCreate));
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

    private void navigateViaRightSwipeFlow(@NonNull Runnable navigation) {
        onRightSwipeNavigationCompleted();
        navigation.run();
    }

    private void openQrShell() {
        LauncherHomeActivity launcherActivity = getLauncherHomeActivity();
        if (launcherActivity != null) {
            launcherActivity.openQrShell();
        }
    }

    private void openScanner() {
        LauncherHomeActivity launcherActivity = getLauncherHomeActivity();
        if (launcherActivity != null) {
            launcherActivity.openScanner();
        }
    }

    private void openCreate() {
        LauncherHomeActivity launcherActivity = getLauncherHomeActivity();
        if (launcherActivity != null) {
            launcherActivity.openCreate();
        }
    }

    private void openHistory() {
        LauncherHomeActivity launcherActivity = getLauncherHomeActivity();
        if (launcherActivity != null) {
            launcherActivity.openHistory();
        }
    }

    private void openSettingsTodo() {
        if (!isAdded()) {
            return;
        }
        startActivity(new Intent(requireContext(), LauncherSettingsActivity.class));
    }

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

    private void setupSwipeAwareClick(@Nullable View view, @NonNull Runnable action) {
        if (view == null) {
            return;
        }
        view.setClickable(true);
        attachSwipeAwareTouchListener(view);
        view.setOnClickListener(v -> {
            if (clickBlockedForGesture) {
                clickBlockedForGesture = false;
                return;
            }
            action.run();
        });
    }

    private void attachSwipeAwareTouchListener(@Nullable View view) {
        if (view == null) {
            return;
        }
        view.setOnTouchListener(this::handleSwipeAwareViewTouch);
    }

    private void attachSwipeAwareTouchListeners(View... views) {
        for (View view : views) {
            attachSwipeAwareTouchListener(view);
        }
    }

    private boolean handleSwipeAwareViewTouch(@NonNull View view, @NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                clickBlockedForGesture = false;
                swipeAwareDownX = event.getRawX();
                swipeAwareDownY = event.getRawY();
                return false;
            case MotionEvent.ACTION_MOVE:
                if (view == rlQuickAction && isSwipeRightGesture(event.getRawX(), event.getRawY())) {
                    onRightSwipeNavigationCompleted();
                }
                if (!clickBlockedForGesture && isSwipeUpGesture(event.getRawX(), event.getRawY())) {
                    clickBlockedForGesture = true;
                    upEventConsumed = true;
                    view.cancelLongPress();
                    view.setPressed(false);
                    clearClickablePressedStates();
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (clickBlockedForGesture) {
                    view.setPressed(false);
                    clickBlockedForGesture = false;
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private boolean isSwipeUpGesture(float rawX, float rawY) {
        float dx = rawX - swipeAwareDownX;
        float dy = rawY - swipeAwareDownY;
        return dy < 0f && Math.abs(dy) > touchSlop && Math.abs(dy) > Math.abs(dx) * VERTICAL_DOMINANCE_RATIO;
    }

    private boolean isSwipeRightGesture(float rawX, float rawY) {
        float dx = rawX - swipeAwareDownX;
        float dy = rawY - swipeAwareDownY;
        return dx > 0f && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * HORIZONTAL_DOMINANCE_RATIO;
    }

    private void clearClickablePressedStates() {
        View[] views = {llGoogleSearch, llRightSwipe, llDateTime, rlQuickAction, llDefault, llGoogleFolder, llToolsFolder, llScan, llCreate, llHistory, llSetting, llCreateQR, llSettings, ivGoogleVoiceSearch, ivTopAppIcon1, ivTopAppIcon2, ivAppIcon1, ivAppIcon2, ivAppIcon3, ivAppIcon4, cvGoogleFolder, cvToolsFolder, gvGoogleApps, gvToolsApps, tvDay, tvDate, tvTime, tvGoogle, tvTools, tvTopAppName1, tvTopAppName2};
        for (View view : views) {
            clearPressedState(view);
        }
    }

    private void clearPressedState(@Nullable View view) {
        if (view != null) {
            view.setPressed(false);
        }
    }

    private void applyLauncherSizeSettings() {
        if (!isFragmentReady()) {
            return;
        }

        int iconSizePx = getLauncherIconSizePx();
        int labelSizeSp = LauncherSettingsHelper.getAppLabelSize(requireContext());
        int labelVisibility = LauncherSettingsHelper.getLabelVisibility(requireContext()) ? VISIBLE : GONE;

        applyIconSize(cvGoogleFolder, iconSizePx);
        applyIconSize(cvToolsFolder, iconSizePx);
        applyFolderPreviewLayout(gvGoogleApps, iconSizePx);
        applyFolderPreviewLayout(gvToolsApps, iconSizePx);
        refreshFolderPreviews();
        applyIconSize(ivTopAppIcon1, iconSizePx);
        applyIconSize(ivTopAppIcon2, iconSizePx);
        applyIconSize(ivAppIcon1, iconSizePx);
        applyIconSize(ivAppIcon2, iconSizePx);
        applyIconSize(ivAppIcon3, iconSizePx);
        applyIconSize(ivAppIcon4, iconSizePx);

        applyLabelStyle(tvGoogle, labelSizeSp, labelVisibility);
        applyLabelStyle(tvTools, labelSizeSp, labelVisibility);
        applyLabelStyle(tvTopAppName1, labelSizeSp, labelVisibility);
        applyLabelStyle(tvTopAppName2, labelSizeSp, labelVisibility);
    }

    private void applyIconSize(@Nullable View view, int sizePx) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) {
            return;
        }
        params.width = sizePx;
        params.height = sizePx;
        view.setLayoutParams(params);
    }

    private void applyLabelStyle(@Nullable AppCompatTextView textView, int sizeSp, int visibility) {
        if (textView == null) {
            return;
        }
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        textView.setVisibility(visibility);
    }

    private void applyFolderPreviewLayout(@Nullable GridLayout gridLayout, int folderSizePx) {
        if (gridLayout == null) {
            return;
        }

        int padding = getFolderPreviewPaddingPx(folderSizePx);
        gridLayout.setPadding(padding, padding, padding, padding);

        ViewGroup.LayoutParams params = gridLayout.getLayoutParams();
        if (params instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) params;
            frameParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            frameParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            frameParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            gridLayout.setLayoutParams(frameParams);
        }
    }

    private void refreshFolderPreviews() {
        bindFolderPreview(gvGoogleApps, arrayListGoogleApps);
        bindFolderPreview(gvToolsApps, arrayListToolsApps);
    }

    private int getFolderPreviewIconSizePx(int folderSizePx) {
        int padding = getFolderPreviewPaddingPx(folderSizePx);
        int margin = AppUtils.dpToPx(requireContext(), 1);
        int availableWidth = folderSizePx - (padding * 2);
        int iconSize = (availableWidth - (FOLDER_PREVIEW_COLUMNS * 2 * margin)) / FOLDER_PREVIEW_COLUMNS;
        return Math.max(AppUtils.dpToPx(requireContext(), 6), iconSize);
    }

    private void loadFolderApps() {
        if (!isFragmentReady()) {
            return;
        }

        arrayListGoogleApps.clear();
        arrayListToolsApps.clear();

        PackageManager packageManager = requireContext().getPackageManager();
        Context appContext = requireContext().getApplicationContext();

        for (String packageName : GOOGLE_APP_PACKAGES) {
            addInstalledApp(appContext, packageManager, packageName, arrayListGoogleApps);
        }

        for (String[] group : TOOLS_APP_GROUPS) {
            for (String packageName : group) {
                if (addInstalledApp(appContext, packageManager, packageName, arrayListToolsApps)) {
                    break;
                }
            }
        }

        bindFolderPreview(gvGoogleApps, arrayListGoogleApps);
        bindFolderPreview(gvToolsApps, arrayListToolsApps);
        updateFolderVisibility();
    }

    private boolean addInstalledApp(@NonNull Context context, @NonNull PackageManager packageManager, @NonNull String packageName, @NonNull ArrayList<LauncherAppsModel> target) {
        try {
            if (packageManager.getLaunchIntentForPackage(packageName) == null) {
                return false;
            }

            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            String appName = AppUtils.getApplicationLabelEnglish(context, applicationInfo);
            Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
            long installTime = 0L;
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                installTime = packageInfo.firstInstallTime;
            } catch (PackageManager.NameNotFoundException ignored) {
            }

            target.add(new LauncherAppsModel(appName, packageName, appIcon, installTime));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void bindFolderPreview(@Nullable GridLayout gridLayout, @NonNull List<LauncherAppsModel> apps) {
        if (gridLayout == null || getContext() == null) {
            return;
        }

        gridLayout.removeAllViews();
        if (apps.isEmpty()) {
            return;
        }

        int folderSizePx = getLauncherIconSizePx();
        int iconSize = getFolderPreviewIconSizePx(folderSizePx);
        int margin = AppUtils.dpToPx(requireContext(), 1);
        int cornerRadiusPx = getLauncherIconCornerRadiusPx();
        int previewCount = Math.min(apps.size(), FOLDER_PREVIEW_MAX_ICONS);
        int rowCount = (previewCount + FOLDER_PREVIEW_COLUMNS - 1) / FOLDER_PREVIEW_COLUMNS;

        gridLayout.setColumnCount(FOLDER_PREVIEW_COLUMNS);
        gridLayout.setRowCount(rowCount);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        gridLayout.setUseDefaultMargins(false);

        for (int index = 0; index < previewCount; index++) {
            LauncherAppsModel app = apps.get(index);
            AppCompatImageView iconView = new AppCompatImageView(requireContext());
            iconView.setAdjustViewBounds(true);
            iconView.setScaleType(AppCompatImageView.ScaleType.FIT_CENTER);
            Drawable appIcon = app.getAppIcon();
            if (appIcon != null) {
                Drawable displayIcon = LauncherAppsIconCache.createDisplayIcon(requireContext(), appIcon, iconSize, cornerRadiusPx);
                iconView.setImageDrawable(displayIcon != null ? displayIcon : appIcon);
            } else {
                iconView.setImageResource(R.mipmap.ic_launcher);
            }

            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(GridLayout.spec(index / FOLDER_PREVIEW_COLUMNS, 1f), GridLayout.spec(index % FOLDER_PREVIEW_COLUMNS, 1f));
            layoutParams.width = iconSize;
            layoutParams.height = iconSize;
            layoutParams.setMargins(margin, margin, margin, margin);
            iconView.setLayoutParams(layoutParams);
            gridLayout.addView(iconView);
        }
    }

    private void updateFolderVisibility() {
        if (llGoogleFolder != null) {
            llGoogleFolder.setVisibility(arrayListGoogleApps.isEmpty() ? GONE : VISIBLE);
        }
        if (llToolsFolder != null) {
            llToolsFolder.setVisibility(arrayListToolsApps.isEmpty() ? GONE : VISIBLE);
        }
    }

    private void dialogAppsFolder(@NonNull String title, @NonNull ArrayList<LauncherAppsModel> apps) {
        if (!isFragmentReady() || apps.isEmpty()) {
            return;
        }

        dismissAppsFolderDialog();

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_apps_folder);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        AppCompatTextView tvTitle = dialog.findViewById(R.id.tvTitle);
        RecyclerView rvApps = dialog.findViewById(R.id.rvApps);
        if (tvTitle != null) {
            tvTitle.setText(title);
        }

        if (rvApps != null) {
            Context context = requireContext();
            int iconSizePx = getLauncherIconSizePx();
            int cornerRadiusPx = getLauncherIconCornerRadiusPx();
            LauncherAppsIconCache.warm(context, apps, iconSizePx, cornerRadiusPx);
            rvApps.setItemAnimator(null);
            rvApps.setLayoutManager(new GridLayoutManager(context, FOLDER_DIALOG_COLUMNS));
            rvApps.setAdapter(new LauncherAppsAdapter(context, apps, null));
        }

        dialog.setOnDismissListener(d -> appsFolderDialog = null);
        applyDialogSidePadding(dialog);
        appsFolderDialog = dialog;
        dialog.show();
    }

    public void dismissAppsFolderDialog() {
        if (appsFolderDialog == null) {
            return;
        }

        try {
            if (appsFolderDialog.isShowing()) {
                appsFolderDialog.dismiss();
            }
        } catch (Exception ignored) {
        }
        appsFolderDialog = null;
    }

    private void applyDialogSidePadding(@NonNull Dialog dialog) {
        if (dialog.getWindow() == null || getContext() == null) {
            return;
        }
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, requireContext().getResources().getDisplayMetrics());
        dialog.getWindow().getDecorView().setPadding(padding, 0, padding, 0);
    }

    private void loadBottomApps() {
        if (!isFragmentReady()) {
            return;
        }

        PackageManager packageManager = requireContext().getPackageManager();
        setBottomAppIconFromPackage(ivAppIcon1, getDefaultDialerPackageName(), packageManager);
        setBottomAppIconFromPackage(ivAppIcon2, Telephony.Sms.getDefaultSmsPackage(requireContext()), packageManager);
        setBottomAppIconFromPackage(ivAppIcon3, getBrowserPackageName(packageManager), packageManager);

        ResolveInfo cameraInfo = preferredApp(packageManager, new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        if (cameraInfo == null) {
            cameraInfo = preferredApp(packageManager, new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
        }
        bottomCameraPackage = cameraInfo != null && cameraInfo.activityInfo != null ? cameraInfo.activityInfo.packageName : null;
        if (cameraInfo != null) {
            setBottomAppIcon(ivAppIcon4, cameraInfo.loadIcon(packageManager));
        } else {
            setBottomAppIcon(ivAppIcon4, null);
        }
    }

    private void setBottomAppIconFromPackage(@Nullable AppCompatImageView iconView, @Nullable String packageName, @NonNull PackageManager packageManager) {
        try {
            if (packageName != null) {
                setBottomAppIcon(iconView, packageManager.getApplicationIcon(packageName));
            } else {
                setBottomAppIcon(iconView, null);
            }
        } catch (Exception ignored) {
            setBottomAppIcon(iconView, null);
        }
    }

    private void setBottomAppIcon(@Nullable AppCompatImageView iconView, @Nullable Drawable icon) {
        if (iconView == null) {
            return;
        }

        View slot = iconView.getParent() instanceof View ? (View) iconView.getParent() : iconView;
        if (icon == null) {
            slot.setVisibility(GONE);
            return;
        }

        slot.setVisibility(VISIBLE);
        Context context = requireContext();
        int iconSizePx = getLauncherIconSizePx();
        int cornerRadiusPx = getLauncherIconCornerRadiusPx();
        Drawable displayIcon = LauncherAppsIconCache.createDisplayIcon(context, icon, iconSizePx, cornerRadiusPx);
        iconView.setImageDrawable(displayIcon != null ? displayIcon : icon);
    }

    @Nullable
    private String getDefaultDialerPackageName() {
        Context context = getContext();
        if (context == null) {
            return null;
        }

        TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager == null) {
            return null;
        }
        return telecomManager.getDefaultDialerPackage();
    }

    @Nullable
    private String getBrowserPackageName(@NonNull PackageManager packageManager) {
        if (packageManager.getLaunchIntentForPackage(CHROME_PACKAGE) != null) {
            return CHROME_PACKAGE;
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
        browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        ResolveInfo resolveInfo = preferredApp(packageManager, browserIntent);
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        return null;
    }

    public boolean onHostTouchEvent(@NonNull MotionEvent event) {
        if (!isAdded() || !isSwipeUpEnabled()) {
            return false;
        }

        if (lockedGestureAxis == GESTURE_AXIS_HORIZONTAL || isLauncherPagerScrolling()) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                lockedGestureAxis = GESTURE_AXIS_NONE;
                appsSwipeResolved = false;
                recycleVelocityTracker();
            }
            return false;
        }

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getRawX();
                touchDownY = event.getRawY();
                appsSwipeResolved = false;
                appsSheetDragging = false;
                appsGestureConsumed = false;
                upEventConsumed = false;
                clickBlockedForGesture = false;
                lockedGestureAxis = GESTURE_AXIS_NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                handleAppsSwipeMove(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleAppsSwipeEnd(event);
                break;
            default:
                break;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            boolean consume = upEventConsumed;
            upEventConsumed = false;
            if (consume) {
                clearClickablePressedStates();
            }
            return consume;
        }
        return appsSheetDragging;
    }

    private void handleAppsSwipeMove(@NonNull MotionEvent event) {
        if (lockedGestureAxis == GESTURE_AXIS_HORIZONTAL || isLauncherPagerScrolling()) {
            return;
        }

        float dx = event.getRawX() - touchDownX;
        float dy = event.getRawY() - touchDownY;

        if (!appsSwipeResolved) {
            if (Math.abs(dx) < touchSlop && Math.abs(dy) < touchSlop) {
                return;
            }

            if (Math.abs(dx) > Math.abs(dy) * HORIZONTAL_DOMINANCE_RATIO) {
                appsSwipeResolved = true;
                lockedGestureAxis = GESTURE_AXIS_HORIZONTAL;
                return;
            }

            boolean verticalUp = isVerticalUpDominant(dx, dy);
            if (!verticalUp) {
                float totalDistance = (float) Math.hypot(dx, dy);
                if (totalDistance < touchSlop * 2f) {
                    return;
                }
                if (Math.abs(dx) >= Math.abs(dy)) {
                    appsSwipeResolved = true;
                    lockedGestureAxis = GESTURE_AXIS_HORIZONTAL;
                }
                return;
            }

            appsSwipeResolved = true;
            lockedGestureAxis = GESTURE_AXIS_VERTICAL;
            appsGestureConsumed = true;
            upEventConsumed = true;
            clickBlockedForGesture = true;
            clearClickablePressedStates();

            float progress = getAppsSheetProgress(event.getRawY());
            if (!beginInteractiveAppsSheet(progress)) {
                resetAppsGestureState();
                return;
            }
            appsSheetDragging = true;
            setPagerSwipeEnabled(false);
            requestDisallowParentIntercept(true);
        }

        if (lockedGestureAxis != GESTURE_AXIS_VERTICAL || !appsSheetDragging || appsBottomSheet == null) {
            return;
        }

        appsBottomSheet.updateInteractiveProgress(getAppsSheetProgress(event.getRawY()));
    }

    private void handleAppsSwipeEnd(@NonNull MotionEvent event) {
        float velocityY = 0f;
        if (velocityTracker != null) {
            velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
            velocityY = velocityTracker.getYVelocity();
            if (Math.abs(velocityY) < minFlingVelocity * 0.5f) {
                velocityY = 0f;
            }
        }

        boolean verticalGesture = lockedGestureAxis == GESTURE_AXIS_VERTICAL;
        if (verticalGesture && appsSheetDragging && appsBottomSheet != null) {
            appsBottomSheet.endInteractiveSessionWhenReady(velocityY);
            upEventConsumed = true;
        } else if (lockedGestureAxis != GESTURE_AXIS_HORIZONTAL && event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (maybeOpenFromQuickFling(event, velocityY)) {
                upEventConsumed = true;
            }
        } else if (verticalGesture || appsGestureConsumed || clickBlockedForGesture) {
            upEventConsumed = true;
        }

        resetAppsGestureState();
    }

    private void resetAppsGestureState() {
        recycleVelocityTracker();
        appsSheetDragging = false;
        appsSwipeResolved = false;
        appsGestureConsumed = false;
        lockedGestureAxis = GESTURE_AXIS_NONE;
        if (findAddedAppsBottomSheet() == null) {
            appsBottomSheet = null;
        }
        requestDisallowParentIntercept(false);
        ensurePagerSwipeRestored();
    }

    public void clearAppsSheetOpeningState() {
        appsSheetOpening = false;
        if (!appsSheetDragging && findAddedAppsBottomSheet() == null) {
            appsBottomSheet = null;
        }
    }

    private boolean maybeOpenFromQuickFling(@NonNull MotionEvent event, float velocityY) {
        if (findAddedAppsBottomSheet() != null) {
            return false;
        }

        float dx = event.getRawX() - touchDownX;
        float dy = event.getRawY() - touchDownY;
        if (!isVerticalUpDominant(dx, dy) || velocityY > -minFlingVelocity) {
            return false;
        }

        float openDistance = getAppsSheetOpenDistance();
        float progress = openDistance > 0f ? Math.min(0.35f, (-dy) / openDistance) : 0.2f;
        if (!beginInteractiveAppsSheet(progress)) {
            ensurePagerSwipeRestored();
            return false;
        }
        appsGestureConsumed = true;
        clickBlockedForGesture = true;
        lockedGestureAxis = GESTURE_AXIS_VERTICAL;
        setPagerSwipeEnabled(false);
        appsBottomSheet.endInteractiveSessionWhenReady(velocityY);
        return true;
    }

    private boolean beginInteractiveAppsSheet(float initialProgress) {
        if (!isAdded()) {
            return false;
        }

        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        if (fragmentManager.isStateSaved()) {
            return false;
        }

        removeStaleAppsBottomSheet(fragmentManager);

        LauncherAppsBottomSheet addedSheet = findAddedAppsBottomSheet(fragmentManager);
        if (addedSheet != null) {
            appsBottomSheet = addedSheet;
            if (addedSheet.isInteractiveSessionActive()) {
                return true;
            }
            if (addedSheet.isSheetVisible()) {
                return false;
            }
            scheduleBeginInteractive(appsBottomSheet, initialProgress);
            return true;
        }

        if (appsSheetOpening) {
            return false;
        }

        appsSheetOpening = true;
        appsBottomSheet = LauncherAppsBottomSheet.newInstance(true);
        appsBottomSheet.setInitialInteractiveProgress(initialProgress);
        try {
            appsBottomSheet.showNow(fragmentManager, LauncherAppsBottomSheet.TAG);
        } catch (IllegalStateException e) {
            appsBottomSheet = null;
            clearAppsSheetOpeningState();
            return false;
        }
        if (!appsBottomSheet.isAdded()) {
            appsBottomSheet = null;
            clearAppsSheetOpeningState();
            return false;
        }
        scheduleBeginInteractive(appsBottomSheet, initialProgress);
        return true;
    }

    private void scheduleBeginInteractive(@NonNull LauncherAppsBottomSheet sheet, float initialProgress) {
        sheet.setInitialInteractiveProgress(initialProgress);
        sheet.runWhenInteractiveReady(() -> {
            if (!sheet.isAdded()) {
                clearAppsSheetOpeningState();
                return;
            }
            sheet.beginInteractive(initialProgress);
            clearAppsSheetOpeningState();
        });
    }

    private void removeStaleAppsBottomSheet(@NonNull FragmentManager fragmentManager) {
        Fragment existing = fragmentManager.findFragmentByTag(LauncherAppsBottomSheet.TAG);
        if (existing == null || existing.isAdded()) {
            return;
        }
        try {
            fragmentManager.beginTransaction().remove(existing).commitNowAllowingStateLoss();
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private LauncherAppsBottomSheet findAddedAppsBottomSheet(@NonNull FragmentManager fragmentManager) {
        Fragment existing = fragmentManager.findFragmentByTag(LauncherAppsBottomSheet.TAG);
        if (existing instanceof LauncherAppsBottomSheet && existing.isAdded()) {
            return (LauncherAppsBottomSheet) existing;
        }
        return null;
    }

    @Nullable
    private LauncherAppsBottomSheet findAddedAppsBottomSheet() {
        if (!isAdded()) {
            return null;
        }
        return findAddedAppsBottomSheet(requireActivity().getSupportFragmentManager());
    }

    private void requestDisallowParentIntercept(boolean disallow) {
        View view = getView();
        if (view == null) {
            return;
        }
        for (ViewParent parent = view.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ViewGroup) {
                parent.requestDisallowInterceptTouchEvent(disallow);
            }
        }
    }

    private float getAppsSheetOpenDistance() {
        View view = getView();
        if (view != null && view.getHeight() > 0) {
            return view.getHeight();
        }
        return getResources().getDisplayMetrics().heightPixels;
    }

    private boolean isSwipeUpEnabled() {
        if (!isResumed() || !isHomePageActive()) {
            return false;
        }
        LauncherAppsBottomSheet sheet = findAddedAppsBottomSheet();
        if (sheet != null) {
            return !sheet.isBlockingHomeSwipe();
        }
        return true;
    }

    private void setPagerSwipeEnabled(boolean enabled) {
        pagerSwipeTemporarilyDisabled = !enabled;
        LauncherHomeActivity launcherActivity = getLauncherHomeActivity();
        if (launcherActivity != null) {
            launcherActivity.setLauncherSwipeEnabled(enabled);
        }
    }

    private boolean isLauncherPagerScrolling() {
        LauncherHomeActivity launcherActivity = getLauncherHomeActivity();
        return launcherActivity != null && launcherActivity.isLauncherPagerScrolling();
    }

    private void ensurePagerSwipeRestored() {
        if (!pagerSwipeTemporarilyDisabled) {
            return;
        }
        pagerSwipeTemporarilyDisabled = false;
        setPagerSwipeEnabled(true);
        LauncherHomeActivity launcherActivity = getLauncherHomeActivity();
        if (launcherActivity != null) {
            launcherActivity.recoverPagerScrollStateIfNeeded();
        }
    }

    private boolean isHomePageActive() {
        LauncherHomeActivity launcherActivity = getLauncherHomeActivity();
        return launcherActivity != null && launcherActivity.getLauncherCurrentItem() == LauncherPagerAdapter.PAGE_HOME;
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private boolean canLaunchShortcut() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastShortcutLaunchElapsedMs < SHORTCUT_CLICK_DEBOUNCE_MS) {
            return false;
        }
        lastShortcutLaunchElapsedMs = now;
        return true;
    }

    private void launchGoogleVoiceSearch() {
        if (!isFragmentReady()) {
            return;
        }

        PackageManager packageManager = requireContext().getPackageManager();

        Intent googleVoiceSearch = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        googleVoiceSearch.setPackage(GOOGLE_APP_PACKAGE);
        googleVoiceSearch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (startResolvedActivity(googleVoiceSearch, packageManager)) {
            return;
        }

        Intent systemVoiceCommand = new Intent(Intent.ACTION_VOICE_COMMAND);
        systemVoiceCommand.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (startResolvedActivity(systemVoiceCommand, packageManager)) {
            return;
        }

        Intent systemVoiceSearch = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        systemVoiceSearch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startResolvedActivity(systemVoiceSearch, packageManager);
    }

    private boolean startResolvedActivity(Intent intent, PackageManager packageManager) {
        if (intent == null || packageManager == null || !isFragmentReady()) {
            return false;
        }
        try {
            Intent launch = new Intent(intent);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (launch.getComponent() == null) {
                ResolveInfo info = preferredApp(packageManager, launch);
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
            return resolved;
        }
        ResolveInfo match = firstConcreteApp(packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY));
        if (match != null) {
            return match;
        }
        return firstConcreteApp(packageManager.queryIntentActivities(intent, 0));
    }

    @Nullable
    private ResolveInfo firstConcreteApp(@Nullable List<ResolveInfo> matches) {
        if (matches == null) {
            return null;
        }
        for (ResolveInfo info : matches) {
            if (isConcreteApp(info)) {
                return info;
            }
        }
        return null;
    }

    private boolean isConcreteApp(@Nullable ResolveInfo info) {
        if (info == null || info.activityInfo == null) {
            return false;
        }
        String packageName = info.activityInfo.packageName == null ? "" : info.activityInfo.packageName;
        String className = info.activityInfo.name == null ? "" : info.activityInfo.name;
        if ("android".equals(packageName)) {
            return false;
        }
        return !className.contains("ResolverActivity") && !className.contains("ChooserActivity");
    }

    private void updateDateTime() {
        if (!isAdded() || tvDay == null || tvDate == null || tvTime == null) {
            return;
        }
        Date now = Calendar.getInstance().getTime();
        String formattedTime = timeFormat.format(now).toUpperCase(Locale.ENGLISH);
        int amPmIndex = formattedTime.lastIndexOf(' ');
        if (amPmIndex > 0) {
            SpannableString spannable = new SpannableString(formattedTime);
            spannable.setSpan(new RelativeSizeSpan(0.45f), amPmIndex + 1, formattedTime.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvTime.setText(spannable);
        } else {
            tvTime.setText(formattedTime);
        }

        tvDay.setText(new SimpleDateFormat("EEEE", Locale.ENGLISH).format(now));
        tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(now));
    }

    private void startDateTimeUpdates() {
        updateDateTime();
        if (timeReceiverRegistered || getContext() == null) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        requireContext().registerReceiver(timeChangeReceiver, filter);
        timeReceiverRegistered = true;
    }

    private void stopDateTimeUpdates() {
        if (!timeReceiverRegistered || getContext() == null) {
            return;
        }
        try {
            requireContext().unregisterReceiver(timeChangeReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        timeReceiverRegistered = false;
    }

    private final BroadcastReceiver timeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDateTime();
        }
    };

    private void launchDialerApp() {
        if (!isFragmentReady()) {
            return;
        }

        try {
            PackageManager packageManager = requireContext().getPackageManager();
            String dialerPackage = getDefaultDialerPackageName();
            if (dialerPackage != null) {
                Intent intent = packageManager.getLaunchIntentForPackage(dialerPackage);
                if (intent != null) {
                    startResolvedActivity(intent, packageManager);
                    return;
                }

                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setPackage(dialerPackage);
                startResolvedActivity(dialIntent, packageManager);
            }
        } catch (Exception ignored) {
        }
    }

    private void launchSmsApp() {
        if (!isFragmentReady()) {
            return;
        }

        try {
            String smsPackage = Telephony.Sms.getDefaultSmsPackage(requireContext());
            if (smsPackage == null) {
                return;
            }

            PackageManager packageManager = requireContext().getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(smsPackage);
            startResolvedActivity(intent, packageManager);
        } catch (Exception ignored) {
        }
    }

    private void launchBrowserApp() {
        if (!isFragmentReady()) {
            return;
        }

        PackageManager packageManager = requireContext().getPackageManager();
        String browserPackage = getBrowserPackageName(packageManager);
        if (browserPackage == null) {
            return;
        }
        Intent intent = packageManager.getLaunchIntentForPackage(browserPackage);
        if (startResolvedActivity(intent, packageManager)) {
            return;
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
        browserIntent.setPackage(browserPackage);
        browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        startResolvedActivity(browserIntent, packageManager);
    }

    private void launchCameraApp() {
        if (!isFragmentReady() || bottomCameraPackage == null) {
            return;
        }

        PackageManager packageManager = requireContext().getPackageManager();
        Intent stillCamera = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        stillCamera.setPackage(bottomCameraPackage);
        if (startResolvedActivity(stillCamera, packageManager)) {
            return;
        }

        Intent launchIntent = packageManager.getLaunchIntentForPackage(bottomCameraPackage);
        startResolvedActivity(launchIntent, packageManager);
    }

    @Override
    public void onResume() {
        super.onResume();
        defaultHomePromptHelper.onResume();
        updateRightSwipeTutorialVisibility();
        startDateTimeUpdates();
        applyLauncherSizeSettings();
        loadFolderApps();
        loadBottomApps();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopDateTimeUpdates();
        dismissAppsFolderDialog();
    }

    @Override
    public void onDestroyView() {
        defaultHomePromptHelper.release();
        if (languageChangeListener != null) {
            LocaleHelper.unregisterLanguageChangeListener(languageChangeListener);
            languageChangeListener = null;
        }
        if (launcherSettingsChangeListener != null) {
            LauncherSettingsHelper.unregisterChangeListener(launcherSettingsChangeListener);
            launcherSettingsChangeListener = null;
        }
        dismissAppsFolderDialog();
        recycleVelocityTracker();
        clearAppsSheetOpeningState();
        resetAppsGestureState();
        llDefault = null;
        super.onDestroyView();
    }
}