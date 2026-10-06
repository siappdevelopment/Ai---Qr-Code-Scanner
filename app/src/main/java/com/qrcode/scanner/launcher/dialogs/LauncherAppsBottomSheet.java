package com.qrcode.scanner.launcher.dialogs;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.gms.ads.nativead.NativeAd;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity;
import com.qrcode.scanner.launcher.activities.LauncherSettingsActivity;
import com.qrcode.scanner.launcher.adapters.LauncherAppsAdapter;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.fragments.LauncherHomeFragment;
import com.qrcode.scanner.launcher.helpers.DefaultHomePromptHelper;
import com.qrcode.scanner.launcher.helpers.LauncherAppsHelper;
import com.qrcode.scanner.launcher.helpers.LauncherAppsIconCache;
import com.qrcode.scanner.launcher.helpers.LauncherSettingsHelper;
import com.qrcode.scanner.launcher.interfaces.OnLauncherAppLongClickListener;
import com.qrcode.scanner.launcher.models.LauncherAppsModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LauncherAppsBottomSheet extends BottomSheetDialogFragment implements OnLauncherAppLongClickListener {
    private AppCompatEditText etSearch;
    private AppCompatImageView ivMore;
    private LinearLayout llDefault;
    private AppCompatTextView btnSetNow, tvNoApps, tvMoreApps;
    private RecyclerView rvApps;
    private View llNoApps;

    private RelativeLayout rlNativeListAdView;
    private ShimmerFrameLayout slNativeListShimmer;
    private FrameLayout flNativeListAd;
    private boolean nativeListAdLoadInProgress;
    private int nativeListAdRequestToken;
    private boolean nativeListAdVisible;
    @Nullable
    private NativeAd loadedNativeListAd;

    private LauncherAppsAdapter launcherAppsAdapter;
    private final LauncherAppContextPopup appContextPopup = new LauncherAppContextPopup();
    private boolean appsShown;
    private boolean enrichmentDone;
    private final DefaultHomePromptHelper defaultHomePromptHelper = new DefaultHomePromptHelper(this);
    private boolean enrichScheduled;
    private boolean receiverRegistered;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();

    public static final String TAG = "LauncherAppsBottomSheet";
    private static final String ARG_INTERACTIVE_OPEN = "interactive_open";
    private static final float SHEET_DIM_AMOUNT = 0.25f;
    private static final int SETTLE_MIN_MS = 60;
    private static final int SETTLE_MAX_MS = 140;
    private static final String NATIVE_LIST_AD_TYPE = "medium";

    private boolean interactiveOpen;
    private boolean interactiveSessionActive;
    private boolean parked;
    private float interactiveProgress;
    private boolean hasPendingEndInteractive;
    private float pendingEndVelocityY;
    private final ArrayList<Runnable> pendingReadyRunnables = new ArrayList<>();

    private BottomSheetBehavior<FrameLayout> sheetBehavior;
    private FrameLayout bottomSheetView;
    private View contentRoot;
    private View statusBarScrim;
    private int contentRootDefaultPaddingTop;

    private boolean hostStatusBarSaved;
    private int savedHostStatusBarColor;
    private boolean savedHostLightStatusBars;
    private boolean expandedStatusBarApplied;
    private boolean windowInsetsListenerAttached;
    private int appliedContentPaddingTop = -1;
    private int appliedContentPaddingBottom = -1;
    private int appliedRecyclerPaddingBottom = -1;
    private int appliedStatusBarScrimHeight = -1;
    private int appliedStatusBarScrimColor = Color.TRANSPARENT;
    private boolean animatedDismissRequested;
    private boolean dismissingImmediately;
    private boolean dismissInProgress;
    private OnBackPressedCallback backDismissCallback;
    private static volatile boolean suppressBackgroundDismiss;
    private LauncherSettingsHelper.ChangeListener launcherSettingsChangeListener;

    private boolean isFragmentReady() {
        return isAdded() && getActivity() != null;
    }

    private boolean isSafeToDismiss() {
        return isAdded() && !isDetached() && getHost() != null;
    }

    @NonNull
    private static List<LauncherAppsModel> snapshotSearchApps() {
        synchronized (LauncherHomeActivity.arrayListAppsSearch) {
            return new ArrayList<>(LauncherHomeActivity.arrayListAppsSearch);
        }
    }

    private static void syncDisplayAppsFromSearch() {
        List<LauncherAppsModel> snapshot = snapshotSearchApps();
        LauncherHomeActivity.arrayListApps.clear();
        LauncherHomeActivity.arrayListApps.addAll(snapshot);
    }

    public static void markSuppressBackgroundDismiss() {
        suppressBackgroundDismiss = true;
    }

    public static boolean shouldSuppressBackgroundDismiss() {
        return suppressBackgroundDismiss;
    }

    public static void clearSuppressBackgroundDismiss() {
        suppressBackgroundDismiss = false;
    }

    private final BottomSheetBehavior.BottomSheetCallback sheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (interactiveSessionActive) {
                return;
            }
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                applyExpandedStatusBarAppearance();
                postApplyExpandedStatusBarAppearance();
                scheduleEnrichment();
            } else if (newState == BottomSheetBehavior.STATE_DRAGGING || newState == BottomSheetBehavior.STATE_SETTLING) {
                clearExpandedStatusBarAppearance();
            } else if (newState == BottomSheetBehavior.STATE_HIDDEN && !parked && !dismissingImmediately) {
                animatedDismissRequested = false;
                clearExpandedStatusBarAppearance();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            if (interactiveSessionActive && bottomSheetView != null) {
                snapSheetToExpandedTop(bottomSheetView);
                applyInteractiveProgress(interactiveProgress);
                return;
            }
            if (slideOffset >= 0.999f) {
                applyExpandedStatusBarAppearance();
            } else if (slideOffset < 0.85f) {
                clearExpandedStatusBarAppearance();
            }
        }
    };

    private final View.OnLayoutChangeListener interactiveLayoutListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
        if (!interactiveSessionActive || bottomSheetView == null) {
            return;
        }
        snapSheetToExpandedTop(bottomSheetView);
        applyInteractiveProgress(interactiveProgress);
    };

    public static LauncherAppsBottomSheet newInstance(boolean interactiveOpen) {
        LauncherAppsBottomSheet sheet = new LauncherAppsBottomSheet();
        Bundle args = new Bundle();
        args.putBoolean(ARG_INTERACTIVE_OPEN, interactiveOpen);
        sheet.setArguments(args);
        return sheet;
    }

    public boolean isBlockingHomeSwipe() {
        return isAdded() && getDialog() != null && getDialog().isShowing() && !interactiveSessionActive && !interactiveOpen && !parked;
    }

    public boolean isInteractiveSessionActive() {
        return interactiveSessionActive;
    }

    public boolean isSheetVisible() {
        Dialog dialog = getDialog();
        return isAdded() && dialog != null && dialog.isShowing();
    }

    private boolean isInteractiveReady() {
        return bottomSheetView != null && sheetBehavior != null;
    }

    public void runWhenInteractiveReady(@NonNull Runnable action) {
        if (isInteractiveReady()) {
            action.run();
            return;
        }
        pendingReadyRunnables.add(action);
        View view = getView();
        if (view != null) {
            view.post(this::flushPendingReadyRunnables);
        } else {
            mainHandler.post(this::flushPendingReadyRunnables);
        }
    }

    private void flushPendingReadyRunnables() {
        if (!isInteractiveReady() || pendingReadyRunnables.isEmpty()) {
            return;
        }
        ArrayList<Runnable> runnables = new ArrayList<>(pendingReadyRunnables);
        pendingReadyRunnables.clear();
        for (Runnable runnable : runnables) {
            runnable.run();
        }
    }

    private void flushPendingEndInteractive() {
        if (!hasPendingEndInteractive || !isInteractiveReady()) {
            return;
        }
        hasPendingEndInteractive = false;
        endInteractiveSession(pendingEndVelocityY);
    }

    public void endInteractiveSessionWhenReady(float velocityY) {
        if (!isAdded()) {
            return;
        }
        if (!isInteractiveReady()) {
            hasPendingEndInteractive = true;
            pendingEndVelocityY = velocityY;
            return;
        }
        endInteractiveSession(velocityY);
    }

    public void beginInteractive(float progress) {
        if (!isAdded()) {
            return;
        }
        parked = false;
        interactiveOpen = true;
        interactiveSessionActive = true;
        interactiveProgress = clamp01(progress);

        if (bottomSheetView != null) {
            bottomSheetView.animate().cancel();
        }
        Dialog dialog = getDialog();
        if (dialog != null) {
            if (!dialog.isShowing()) {
                dialog.show();
            }
            dialog.setCanceledOnTouchOutside(false);
            prepareInteractiveWindow(dialog);
        }
        if (sheetBehavior != null) {
            sheetBehavior.setDraggable(false);
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        if (bottomSheetView != null) {
            snapSheetToExpandedTop(bottomSheetView);
        }
        clearExpandedStatusBarAppearance();
        applySheetSurfaceAppearance();
        applyInteractiveProgress(interactiveProgress);
    }

    public void setInitialInteractiveProgress(float progress) {
        interactiveProgress = clamp01(progress);
    }

    public void updateInteractiveProgress(float progress) {
        if (!interactiveSessionActive && !interactiveOpen && !parked) {
            return;
        }
        if (parked || !interactiveSessionActive) {
            beginInteractive(progress);
            return;
        }
        interactiveProgress = clamp01(progress);
        applyInteractiveProgress(interactiveProgress);
    }

    public void endInteractiveSession(float velocityY) {
        if (!isAdded()) {
            return;
        }
        if (!interactiveSessionActive) {
            if (!isInteractiveReady()) {
                hasPendingEndInteractive = true;
                pendingEndVelocityY = velocityY;
            }
            return;
        }
        if (!isInteractiveReady()) {
            hasPendingEndInteractive = true;
            pendingEndVelocityY = velocityY;
            return;
        }
        hasPendingEndInteractive = false;
        interactiveSessionActive = false;
        clearInteractiveWindowFlags();

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(true);
        }

        boolean expand;
        if (Math.abs(velocityY) >= 800f) {
            expand = velocityY < 0f;
        } else {
            expand = interactiveProgress >= 0.35f;
        }

        if (sheetBehavior != null) {
            sheetBehavior.setDraggable(true);
        }

        if (expand) {
            settleInteractiveExpand(velocityY);
        } else {
            parkOffScreen();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        interactiveOpen = args != null && args.getBoolean(ARG_INTERACTIVE_OPEN, false);
        if (savedInstanceState != null) {
            interactiveOpen = false;
        }
        setStyle(STYLE_NORMAL, R.style.LauncherAppsBottomSheetTheme);
        setCancelable(true);
        defaultHomePromptHelper.registerRoleLauncher();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), getTheme()) {
            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                Window window = getWindow();
                if (window != null) {
                    window.setWindowAnimations(0);
                    clearWindowDim(window);
                    if (interactiveOpen) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                    }
                }
            }
        };
        dialog.setCanceledOnTouchOutside(!interactiveOpen);
        dialog.setDismissWithAnimation(true);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_launcher_apps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contentRoot = view;
        contentRootDefaultPaddingTop = view.getPaddingTop();
        findIDs(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }
        clearWindowDim(dialog.getWindow());
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }

        bottomSheetView = bottomSheet;
        ensureStatusBarScrim();
        applySheetSurfaceAppearance();
        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheet.setLayoutParams(layoutParams);

        sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setFitToContents(false);
        sheetBehavior.setExpandedOffset(0);
        sheetBehavior.setSkipCollapsed(true);
        sheetBehavior.setHideable(true);
        sheetBehavior.removeBottomSheetCallback(sheetCallback);
        sheetBehavior.addBottomSheetCallback(sheetCallback);

        if (interactiveOpen) {
            parked = false;
            interactiveSessionActive = true;
            sheetBehavior.setDraggable(false);
            prepareInteractiveWindow(dialog);
            bottomSheet.animate().cancel();
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            snapSheetToExpandedTop(bottomSheet);
            applyInteractiveProgress(interactiveProgress);
            bottomSheet.addOnLayoutChangeListener(interactiveLayoutListener);
            flushPendingReadyRunnables();
            flushPendingEndInteractive();
        } else {
            sheetBehavior.setDraggable(true);
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            configureExpandedWindow(dialog);
            applyExpandedStatusBarAppearance();
            postApplyExpandedStatusBarAppearance();
            mainHandler.postDelayed(this::scheduleEnrichment, 280);
        }
        setupBackDismissHandler(dialog);
    }

    private void setupBackDismissHandler(@NonNull Dialog dialog) {
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }
        if (backDismissCallback != null) {
            backDismissCallback.remove();
        }
        backDismissCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requestAnimatedDismiss();
            }
        };
        ((BottomSheetDialog) dialog).getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backDismissCallback);
    }

    @Override
    public void dismiss() {
        if (!isSafeToDismiss() || dismissInProgress) {
            return;
        }
        dismissInProgress = true;
        try {
            super.dismiss();
        } catch (IllegalStateException ignored) {
        } finally {
            dismissInProgress = false;
        }
    }

    @Override
    public void dismissAllowingStateLoss() {
        if (!isSafeToDismiss() || dismissInProgress) {
            return;
        }
        dismissInProgress = true;
        try {
            super.dismissAllowingStateLoss();
        } catch (IllegalStateException ignored) {
        } finally {
            dismissInProgress = false;
        }
    }

    public void requestAnimatedDismiss() {
        if (!isAdded() || animatedDismissRequested || dismissingImmediately) {
            return;
        }
        Dialog dialog = getDialog();
        if (dialog == null || !dialog.isShowing()) {
            return;
        }

        prepareDismissUI();

        if (sheetBehavior != null && !parked && !interactiveSessionActive) {
            int state = sheetBehavior.getState();
            if (state == BottomSheetBehavior.STATE_HIDDEN || state == BottomSheetBehavior.STATE_COLLAPSED) {
                dismissAllowingStateLoss();
                return;
            }
            animatedDismissRequested = true;
            sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }

        dismissAllowingStateLoss();
    }

    public void dismissForBackgroundLeave() {
        if (!isAdded() || dismissingImmediately) {
            return;
        }
        Dialog dialog = getDialog();
        if (dialog == null || !dialog.isShowing()) {
            return;
        }

        dismissingImmediately = true;
        animatedDismissRequested = false;
        mainHandler.removeCallbacksAndMessages(null);
        prepareDismissUI();

        if (bottomSheetView != null) {
            bottomSheetView.animate().cancel();
            bottomSheetView.setTranslationY(hiddenTranslationY());
        }
        if (sheetBehavior != null) {
            sheetBehavior.removeBottomSheetCallback(sheetCallback);
        }

        clearExpandedStatusBarAppearance();
        dismissAllowingStateLoss();
    }

    private void prepareDismissUI() {
        appContextPopup.dismiss();
        hideSearchKeyboard();
    }

    private void findIDs(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        ivMore = view.findViewById(R.id.ivMore);
        llDefault = view.findViewById(R.id.llDefault);
        btnSetNow = view.findViewById(R.id.btnSetNow);
        defaultHomePromptHelper.bind(llDefault);
        rvApps = view.findViewById(R.id.rvApps);
        llNoApps = view.findViewById(R.id.llNoApps);
        tvNoApps = view.findViewById(R.id.tvNoApps);
        tvMoreApps = view.findViewById(R.id.tvMoreApps);

        rlNativeListAdView = view.findViewById(R.id.rlNativeListAdView);
        slNativeListShimmer = view.findViewById(R.id.slNativeListShimmer);
        flNativeListAd = view.findViewById(R.id.flNativeListAd);

        setupAppsList();
        setupClickListeners();
        launcherSettingsChangeListener = this::handleLauncherSettingsChanged;
        LauncherSettingsHelper.registerChangeListener(launcherSettingsChangeListener);
    }

    private void handleLauncherSettingsChanged(int changeMask) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) {
                return;
            }
            if ((changeMask & LauncherSettingsHelper.CHANGE_DISPLAY) != 0 && launcherAppsAdapter != null) {
                launcherAppsAdapter.applyDisplaySettings();
            }
            if ((changeMask & LauncherSettingsHelper.CHANGE_SERIALIZE) != 0) {
                applySerializeSettingsChange();
            }
        });
    }

    private void applySerializeSettingsChange() {
        if (launcherAppsAdapter == null || !enrichmentDone) {
            return;
        }
        bgExecutor.execute(() -> {
            Context context = getContext();
            if (context == null) {
                return;
            }
            List<LauncherAppsModel> previousOrder;
            synchronized (LauncherHomeActivity.arrayListAppsSearch) {
                previousOrder = new ArrayList<>(LauncherHomeActivity.arrayListApps);
                LauncherAppsHelper.sortApps(context, LauncherHomeActivity.arrayListAppsSearch);
            }
            mainHandler.post(() -> {
                if (!isAdded() || launcherAppsAdapter == null || etSearch == null) {
                    return;
                }
                String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
                if (!query.trim().isEmpty()) {
                    searchApps(query);
                } else {
                    syncDisplayAppsFromSearch();
                    launcherAppsAdapter.notifyOrderChanged(previousOrder);
                }
            });
        });
    }

    private void setupClickListeners() {
        showAd();

        ivMore.setOnClickListener(v -> {
            markSuppressBackgroundDismiss();
            startActivity(new Intent(requireContext(), LauncherSettingsActivity.class));
            scheduleSearchClear();
        });

        btnSetNow.setOnClickListener(v -> {
            markSuppressBackgroundDismiss();
            defaultHomePromptHelper.handleSetAsDefaultClick();
        });

        tvMoreApps.setOnClickListener(v -> {
            String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
            markSuppressBackgroundDismiss();

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + query));
                intent.setPackage("com.android.vending");
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(query) + "&c=apps"));
                startActivity(intent);
            }
            scheduleSearchClear();
        });
    }

    private void scheduleSearchClear() {
        mainHandler.postDelayed(this::searchClearView, 100);
    }

    private void setupAppsList() {
        rvApps.setItemAnimator(null);
        rvApps.setItemViewCacheSize(24);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 4);
        gridLayoutManager.setInitialPrefetchItemCount(16);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return launcherAppsAdapter != null ? launcherAppsAdapter.getSpanSize(position) : 1;
            }
        });
        rvApps.setLayoutManager(gridLayoutManager);
        rvApps.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateStickyNativeListAdPosition();
            }
        });
        warmAppIconCacheAsync();
        showCachedAppsInstantly();
        maintainExpandedSheetOnSearchFocus();

        etSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!appsShown) {
                    return;
                }
                searchApps(s.toString());
            }
        });
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showCachedAppsInstantly() {
        if (appsShown || !isAdded()) {
            return;
        }
        if (LauncherHomeActivity.arrayListAppsSearch.isEmpty()) {
            rvApps.setVisibility(INVISIBLE);
            rvApps.setAlpha(0f);
            return;
        }

        Context context = getContext();
        List<LauncherAppsModel> previousOrder = new ArrayList<>(LauncherHomeActivity.arrayListApps);
        if (context != null) {
            synchronized (LauncherHomeActivity.arrayListAppsSearch) {
                LauncherAppsHelper.sortApps(context, LauncherHomeActivity.arrayListAppsSearch);
            }
        }
        syncDisplayAppsFromSearch();

        attachAppsAdapterIfNeeded();
        if (launcherAppsAdapter != null) {
            launcherAppsAdapter.notifyOrderChanged(previousOrder);
            launcherAppsAdapter.updateSizeVisibility();
        }
        appsShown = true;
        rvApps.setAlpha(1f);
        rvApps.setVisibility(VISIBLE);
        updateEmptyState(LauncherHomeActivity.arrayListApps.size());
    }

    private void attachAppsAdapterIfNeeded() {
        if (launcherAppsAdapter != null || !isAdded()) {
            return;
        }
        launcherAppsAdapter = new LauncherAppsAdapter(requireContext(), LauncherHomeActivity.arrayListApps, this, true);
        if (shouldShowNativeListAd()) {
            launcherAppsAdapter.setListNativeShimmerVisible(true);
            launcherAppsAdapter.setListNativePlaceholderEnabled(true);
        }
        rvApps.setAdapter(launcherAppsAdapter);
        rvApps.post(this::updateStickyNativeListAdPosition);
    }

    private void maintainExpandedSheetOnSearchFocus() {
        if (etSearch == null) {
            return;
        }
        etSearch.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus || !isAdded() || interactiveSessionActive || sheetBehavior == null) {
                return;
            }
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            if (bottomSheetView != null) {
                bottomSheetView.setTranslationY(0f);
            }
            Dialog dialog = getDialog();
            if (dialog != null) {
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setDimAmount(SHEET_DIM_AMOUNT);
                }
            }
        });
    }

    private void scheduleEnrichment() {
        if (enrichmentDone || enrichScheduled || !isAdded()) {
            return;
        }
        enrichScheduled = true;
        mainHandler.post(this::enrichContentAfterOpen);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void enrichContentAfterOpen() {
        if (!isAdded() || enrichmentDone) {
            return;
        }
        enrichmentDone = true;

        final boolean needsFullReload = LauncherHomeActivity.arrayListAppsSearch.isEmpty();

        bgExecutor.execute(() -> {
            Context context = getContext();
            if (context == null) {
                return;
            }

            if (needsFullReload) {
                reloadInstalledApps(context);
            }
            warmAppIconCache(context, snapshotSearchApps());

            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }

                if (!appsShown) {
                    if (!LauncherHomeActivity.arrayListAppsSearch.isEmpty()) {
                        syncDisplayAppsFromSearch();
                    }
                    attachAppsAdapterIfNeeded();
                    appsShown = true;
                    rvApps.setVisibility(VISIBLE);
                    rvApps.animate().alpha(1f).setDuration(100).start();
                    updateEmptyState(LauncherHomeActivity.arrayListApps.size());
                } else if (launcherAppsAdapter != null && needsFullReload) {
                    launcherAppsAdapter.notifyAppsDataChanged();
                    updateEmptyState(LauncherHomeActivity.arrayListApps.size());
                }

                registerPackageReceiver();
            });
        });
    }

    private void reloadInstalledApps(Context context) {
        LauncherAppsHelper.replaceAppLists(LauncherAppsHelper.loadInstalledApps(context));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void searchApps(String query) {
        if (launcherAppsAdapter == null) {
            return;
        }

        String search = query.trim().toLowerCase();
        LauncherHomeActivity.arrayListApps.clear();

        if (search.isEmpty()) {
            LauncherHomeActivity.arrayListApps.addAll(snapshotSearchApps());
        } else {
            filterAppsByQuery(search);
        }

        launcherAppsAdapter.setQuizIconsVisible(search.isEmpty());
        launcherAppsAdapter.notifyAppsDataChanged();
        updateEmptyState(LauncherHomeActivity.arrayListApps.size());
        if (rvApps != null) {
            rvApps.scrollToPosition(0);
            rvApps.post(() -> {
                updateStickyNativeListAdPosition();
                rvApps.post(this::updateStickyNativeListAdPosition);
            });
        }
    }

    private void filterAppsByQuery(@NonNull String search) {
        for (LauncherAppsModel app : snapshotSearchApps()) {
            String appName = app.getAppName();
            if (appName != null && appName.toLowerCase().contains(search)) {
                LauncherHomeActivity.arrayListApps.add(app);
            }
        }
    }

    private boolean isSearchActive() {
        return etSearch != null && etSearch.getText() != null && !etSearch.getText().toString().trim().isEmpty();
    }

    @SuppressLint("SetTextI18n")
    private void updateEmptyState(int itemCount) {
        if (llNoApps == null || rvApps == null) {
            return;
        }

        if (!isSearchActive()) {
            llNoApps.setVisibility(GONE);
            if (appsShown) {
                rvApps.setVisibility(VISIBLE);
            }
            return;
        }

        boolean showEmptyState = itemCount == 0;
        llNoApps.setVisibility(showEmptyState ? VISIBLE : GONE);
        rvApps.setVisibility(showEmptyState ? GONE : VISIBLE);

        if (showEmptyState) {
            String query = etSearch.getText().toString().trim();
            tvNoApps.setVisibility(VISIBLE);
            tvNoApps.setText("No " + query + " Apps Found");
            tvMoreApps.setVisibility(VISIBLE);
        }
    }

    private void searchClearView() {
        if (etSearch == null) {
            return;
        }

        hideSearchKeyboard();
        etSearch.clearFocus();
        if (etSearch.getText() != null && etSearch.getText().length() > 0) {
            etSearch.setText("");
        } else {
            updateEmptyState(LauncherHomeActivity.arrayListApps.size());
        }
    }

    private void hideSearchKeyboard() {
        if (etSearch == null) {
            return;
        }
        Context context = getContext();
        if (context == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    private void registerPackageReceiver() {
        if (receiverRegistered || getContext() == null) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        requireContext().registerReceiver(appChangeReceiver, filter);
        receiverRegistered = true;
    }

    private final BroadcastReceiver appChangeReceiver = new BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isAdded() || !enrichmentDone) {
                return;
            }
            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                String action = intent.getAction();

                if (Intent.ACTION_PACKAGE_REMOVED.equals(action) && intent.getData() != null) {
                    if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        return;
                    }
                    String packageName = intent.getData().getSchemeSpecificPart();
                    removeUninstalledApp(packageName);
                } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                    bgExecutor.execute(() -> {
                        Context appContext = getContext();
                        if (appContext == null) {
                            return;
                        }
                        reloadInstalledApps(appContext);
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded() || launcherAppsAdapter == null) {
                                return;
                            }
                            launcherAppsAdapter.notifyAppsDataChanged();
                        });
                    });
                }
            });
        }
    };

    @SuppressLint("NotifyDataSetChanged")
    private void removeUninstalledApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        int displayIndex = findAppIndex(LauncherHomeActivity.arrayListApps, packageName);
        if (displayIndex >= 0) {
            LauncherHomeActivity.arrayListApps.remove(displayIndex);
            if (launcherAppsAdapter != null) {
                if (launcherAppsAdapter.injectsQuizIcons()) {
                    launcherAppsAdapter.notifyAppsDataChanged();
                } else {
                    launcherAppsAdapter.notifyItemRemoved(displayIndex);
                }
            }
        }

        synchronized (LauncherHomeActivity.arrayListAppsSearch) {
            int searchIndex = findAppIndex(LauncherHomeActivity.arrayListAppsSearch, packageName);
            if (searchIndex >= 0) {
                LauncherHomeActivity.arrayListAppsSearch.remove(searchIndex);
            }
        }

        updateEmptyState(LauncherHomeActivity.arrayListApps.size());
    }

    private static int findAppIndex(List<LauncherAppsModel> apps, String packageName) {
        if (apps == null || packageName == null) {
            return -1;
        }
        for (int i = 0; i < apps.size(); i++) {
            if (packageName.equals(apps.get(i).getPackageName())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onLauncherAppLongClick(@NonNull LauncherAppsModel app, int position, @NonNull View anchorView) {
        if (!isAdded()) {
            return;
        }
        Context context = getContext();
        if (context == null) {
            return;
        }
        appContextPopup.show(context, anchorView, app, position, new LauncherAppContextPopup.Callback() {
            @Override
            public void onAppInfo(@NonNull LauncherAppsModel selectedApp) {
                markSuppressBackgroundDismiss();
                AppUtils.openAppInfo(requireContext(), selectedApp.getPackageName());
            }

            @Override
            public void onUninstall(@NonNull LauncherAppsModel selectedApp, int adapterPosition) {
                launchUninstallFlow(selectedApp.getPackageName());
            }
        });
    }

    private void launchUninstallFlow(String packageName) {
        if (!isAdded() || packageName == null || packageName.isEmpty()) {
            return;
        }
        if (!LauncherAppsHelper.isUninstallableApp(requireContext(), packageName)) {
            return;
        }
        try {
            markSuppressBackgroundDismiss();
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

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

    private void maybeLoadNativeListAd() {
        if (rlNativeListAdView == null || slNativeListShimmer == null || flNativeListAd == null || !isAdded()) {
            return;
        }
        if (!shouldShowNativeListAd()) {
            hideNativeListAdContainer();
            if (launcherAppsAdapter != null) {
                launcherAppsAdapter.setListNativePlaceholderEnabled(false);
            }
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
            rlNativeListAdView.setVisibility(VISIBLE);
            updateStickyNativeListAdPosition();
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
            if (nativeAd != null) {
                if (loadedNativeListAd != null && loadedNativeListAd != nativeAd) {
                    loadedNativeListAd.destroy();
                }
                loadedNativeListAd = nativeAd;
            }
            if (flNativeListAd.getChildCount() > 0) {
                nativeListAdVisible = true;
                if (launcherAppsAdapter != null) {
                    launcherAppsAdapter.setListNativeShimmerVisible(false);
                }
                slNativeListShimmer.stopShimmer();
                slNativeListShimmer.setVisibility(GONE);
                flNativeListAd.setVisibility(VISIBLE);
                rlNativeListAdView.setVisibility(VISIBLE);
                AdPlacement.setLauncherAppNativeListLastShowTime(requireContext(), System.currentTimeMillis());
                applyNativeListBottomPadding(true);
                updateStickyNativeListAdPosition();
                rlNativeListAdView.post(this::updateStickyNativeListAdPosition);
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
        if (loadedNativeListAd != null) {
            loadedNativeListAd.destroy();
            loadedNativeListAd = null;
        }
    }

    private void applyNativeListBottomPadding(boolean enabled) {
        if (rvApps == null) {
            return;
        }
        int bottom = enabled ? getResources().getDimensionPixelSize(R.dimen.launcher_native_list_spacer_height) : getResources().getDimensionPixelSize(R.dimen.launcher_native_list_margin);
        rvApps.setPadding(rvApps.getPaddingLeft(), rvApps.getPaddingTop(), rvApps.getPaddingRight(), bottom);
    }

    private void updateStickyNativeListAdPosition() {
        if (!nativeListAdVisible || rlNativeListAdView == null || rvApps == null || launcherAppsAdapter == null) {
            return;
        }
        if (rlNativeListAdView.getVisibility() != VISIBLE || rlNativeListAdView.getHeight() <= 0) {
            return;
        }
        View parent = (View) rlNativeListAdView.getParent();
        if (parent == null || parent.getHeight() <= 0) {
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
                rlNativeListAdView.setTranslationY(parent.getHeight());
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

    private void warmAppIconCacheAsync() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        bgExecutor.execute(() -> warmAppIconCache(context, snapshotSearchApps()));
    }

    private void warmAppIconCache(@NonNull Context context, @NonNull List<LauncherAppsModel> apps) {
        int iconSizePx = AppUtils.dpToPx(context, LauncherSettingsHelper.getAppIconSize(context));
        int cornerRadiusPx = AppUtils.dpToPx(context, 20);
        LauncherAppsIconCache.warm(context, apps, iconSizePx, cornerRadiusPx);
    }

    private void configureExpandedWindow(@NonNull Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            window.setDimAmount(SHEET_DIM_AMOUNT);
        }
    }

    private void prepareInteractiveWindow(@NonNull Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setWindowAnimations(0);
        clearWindowDim(window);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    private void clearInteractiveWindowFlags() {
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    private void applyInteractiveProgress(float progress) {
        if (bottomSheetView == null) {
            return;
        }
        bottomSheetView.animate().cancel();
        bottomSheetView.setTranslationY(hiddenTranslationY() * (1f - clamp01(progress)));

        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(SHEET_DIM_AMOUNT * clamp01(progress));
            }
        }
    }

    private void clearWindowDim(@Nullable Window window) {
        if (window == null) {
            return;
        }
        window.setDimAmount(0f);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    private float hiddenTranslationY() {
        if (bottomSheetView != null && bottomSheetView.getHeight() > 0) {
            return bottomSheetView.getHeight();
        }
        return getResources().getDisplayMetrics().heightPixels;
    }

    private void snapSheetToExpandedTop(@NonNull View bottomSheet) {
        int top = bottomSheet.getTop();
        if (top != 0) {
            bottomSheet.offsetTopAndBottom(-top);
        }
    }

    private void settleInteractiveExpand(float velocityY) {
        if (bottomSheetView == null) {
            finishInteractiveExpand();
            return;
        }
        bottomSheetView.animate().cancel();
        bottomSheetView.animate().translationY(0f).setDuration(settleDurationMs(bottomSheetView.getTranslationY(), velocityY)).setInterpolator(new DecelerateInterpolator()).withEndAction(this::finishInteractiveExpand).start();

        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(SHEET_DIM_AMOUNT);
            }
        }
    }

    private void finishInteractiveExpand() {
        if (!isAdded()) {
            return;
        }
        if (bottomSheetView != null) {
            bottomSheetView.setTranslationY(0f);
        }
        parked = false;
        interactiveOpen = false;
        if (sheetBehavior != null) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        Dialog dialog = getDialog();
        if (dialog != null && bottomSheetView != null) {
            configureExpandedWindow(dialog);
            applySheetSurfaceAppearance();
            applyExpandedStatusBarAppearance();
            postApplyExpandedStatusBarAppearance();
        }
        scheduleEnrichment();
    }

    private void parkOffScreen() {
        if (!isAdded()) {
            return;
        }
        parked = true;
        interactiveOpen = false;
        interactiveSessionActive = false;
        interactiveProgress = 0f;

        if (bottomSheetView != null) {
            bottomSheetView.animate().cancel();
            bottomSheetView.setTranslationY(hiddenTranslationY());
        }
        if (sheetBehavior != null) {
            sheetBehavior.setDraggable(false);
        }
        Dialog dialog = getDialog();
        if (dialog != null) {
            prepareInteractiveWindow(dialog);
            clearWindowDim(dialog.getWindow());
        }
        clearExpandedStatusBarAppearance();
        dismissAllowingStateLoss();
    }

    private int settleDurationMs(float fromTy, float velocityY) {
        float distance = Math.abs(fromTy);
        float speed = Math.abs(velocityY);
        if (speed > 1f) {
            int duration = (int) (distance / speed * 1000f);
            return Math.max(SETTLE_MIN_MS, Math.min(SETTLE_MAX_MS, duration));
        }
        return SETTLE_MIN_MS;
    }

    private static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        return Math.min(value, 1f);
    }

    private void applySheetSurfaceAppearance() {
        if (!isAdded() || parked) {
            return;
        }
        Context context = getContext();
        if (context == null) {
            return;
        }
        int surfaceColor = resolveThemeSurfaceColor(context);
        if (surfaceColor == 0 || bottomSheetView == null) {
            return;
        }
        applyBottomSheetSurface(bottomSheetView, surfaceColor);
    }

    private void applyExpandedStatusBarAppearance() {
        if (!isAdded() || parked || interactiveSessionActive || expandedStatusBarApplied) {
            return;
        }
        if (bottomSheetView != null && bottomSheetView.getTranslationY() > 0f) {
            return;
        }
        Context context = getContext();
        if (context == null) {
            return;
        }
        int statusBarColor = resolveThemeStatusBarColor(context);
        int surfaceColor = resolveThemeSurfaceColor(context);
        if (statusBarColor == 0) {
            statusBarColor = surfaceColor;
        }
        if (statusBarColor == 0) {
            return;
        }
        if (surfaceColor == 0) {
            surfaceColor = statusBarColor;
        }
        boolean lightStatusBars = resolveThemeLightStatusBars(context, statusBarColor);
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        applyExpandedDialogSystemUi(window, statusBarColor, surfaceColor, lightStatusBars);
        applyBottomSheetSurface(bottomSheetView, surfaceColor);
        applyExpandedWindowInsets(statusBarColor);
        if (!hostStatusBarSaved) {
            saveHostStatusBarState();
        }
        applyHostStatusBar(statusBarColor, lightStatusBars);
        expandedStatusBarApplied = true;
    }

    private void postApplyExpandedStatusBarAppearance() {
        View anchor = bottomSheetView != null ? bottomSheetView : contentRoot;
        if (anchor == null) {
            return;
        }
        anchor.post(() -> {
            if (isFullyExpanded()) {
                applyExpandedStatusBarAppearance();
            }
        });
    }

    private void clearExpandedStatusBarAppearance() {
        clearGestureDialogSystemUI();
        clearExpandedWindowInsets();
        if (expandedStatusBarApplied) {
            restoreHostStatusBarState();
            expandedStatusBarApplied = false;
        }
    }

    private void ensureStatusBarScrim() {
        if (statusBarScrim != null || bottomSheetView == null || getContext() == null) {
            return;
        }
        statusBarScrim = new View(requireContext());
        statusBarScrim.setVisibility(GONE);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        layoutParams.gravity = Gravity.TOP;
        bottomSheetView.addView(statusBarScrim, 0, layoutParams);
    }

    private void applyExpandedDialogSystemUi(@NonNull Window window, @ColorInt int statusBarColor, @ColorInt int surfaceColor, boolean lightStatusBars) {
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(surfaceColor);
        window.setBackgroundDrawable(new ColorDrawable(surfaceColor));
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(lightStatusBars);
        controller.setAppearanceLightNavigationBars(ColorUtils.calculateLuminance(surfaceColor) > 0.5);
        updateStatusBarScrimIfNeeded(statusBarColor, resolveStatusBarHeight(window));
    }

    private void clearGestureDialogSystemUI() {
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        updateStatusBarScrimIfNeeded(Color.TRANSPARENT, 0);
    }

    private void applyExpandedWindowInsets(@ColorInt int statusBarColor) {
        if (bottomSheetView == null || contentRoot == null) {
            return;
        }
        if (!windowInsetsListenerAttached) {
            windowInsetsListenerAttached = true;
            ViewCompat.setOnApplyWindowInsetsListener(bottomSheetView, (view, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                updateStatusBarScrimIfNeeded(statusBarColor, statusBarHeight);
                applyContentInsetsIfNeeded(statusBarHeight, navigationBarHeight);
                applyRecyclerImePaddingIfNeeded(imeInsets.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(bottomSheetView);
        }
    }

    private void applyContentInsetsIfNeeded(int statusBarHeight, int navigationBarHeight) {
        if (contentRoot == null) {
            return;
        }
        int paddingTop = contentRootDefaultPaddingTop + statusBarHeight;
        if (appliedContentPaddingTop == paddingTop && appliedContentPaddingBottom == navigationBarHeight) {
            return;
        }
        appliedContentPaddingTop = paddingTop;
        appliedContentPaddingBottom = navigationBarHeight;
        contentRoot.setPadding(contentRoot.getPaddingLeft(), paddingTop, contentRoot.getPaddingRight(), navigationBarHeight);
    }

    private void applyRecyclerImePaddingIfNeeded(int imeBottom) {
        if (rvApps == null) {
            return;
        }
        int paddingBottom = Math.max(imeBottom, 0);
        if (appliedRecyclerPaddingBottom == paddingBottom) {
            return;
        }
        appliedRecyclerPaddingBottom = paddingBottom;
        rvApps.setPadding(rvApps.getPaddingLeft(), rvApps.getPaddingTop(), rvApps.getPaddingRight(), paddingBottom);
    }

    private void updateStatusBarScrimIfNeeded(@ColorInt int color, int height) {
        if (statusBarScrim == null) {
            return;
        }
        int safeHeight = Math.max(height, 0);
        if (appliedStatusBarScrimHeight == safeHeight && appliedStatusBarScrimColor == color) {
            return;
        }
        appliedStatusBarScrimHeight = safeHeight;
        appliedStatusBarScrimColor = color;
        ViewGroup.LayoutParams layoutParams = statusBarScrim.getLayoutParams();
        layoutParams.height = safeHeight;
        statusBarScrim.setLayoutParams(layoutParams);
        statusBarScrim.setBackgroundColor(color);
        statusBarScrim.setVisibility(safeHeight > 0 && Color.alpha(color) > 0 ? VISIBLE : GONE);
    }

    private void clearExpandedWindowInsets() {
        if (bottomSheetView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomSheetView, null);
        }
        windowInsetsListenerAttached = false;
        appliedContentPaddingTop = -1;
        appliedContentPaddingBottom = -1;
        appliedRecyclerPaddingBottom = -1;
        appliedStatusBarScrimHeight = -1;
        appliedStatusBarScrimColor = Color.TRANSPARENT;
        if (contentRoot != null) {
            contentRoot.setPadding(contentRoot.getPaddingLeft(), contentRootDefaultPaddingTop, contentRoot.getPaddingRight(), 0);
        }
        if (rvApps != null && rvApps.getPaddingBottom() != 0) {
            rvApps.setPadding(rvApps.getPaddingLeft(), rvApps.getPaddingTop(), rvApps.getPaddingRight(), 0);
        }
        updateStatusBarScrimIfNeeded(Color.TRANSPARENT, 0);
    }

    private static int resolveStatusBarHeight(@NonNull Window window) {
        View decorView = window.getDecorView();
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decorView);
        if (insets != null) {
            return insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
        }
        return 0;
    }

    private boolean isFullyExpanded() {
        return isAdded() && !parked && !interactiveSessionActive && !interactiveOpen && bottomSheetView != null && bottomSheetView.getTranslationY() <= 0f;
    }

    private void applyBottomSheetSurface(@NonNull View bottomSheet, @ColorInt int surfaceColor) {
        bottomSheet.setElevation(0f);
        bottomSheet.setOutlineProvider(null);
        bottomSheet.setClipToOutline(false);
        bottomSheet.setBackground(new ColorDrawable(surfaceColor));
        bottomSheet.setBackgroundTintList(null);
    }

    private void applyHostStatusBar(@ColorInt int statusBarColor, boolean lightStatusBars) {
        if (getActivity() == null) {
            return;
        }
        Window window = getActivity().getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(statusBarColor);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(lightStatusBars);
    }

    private void saveHostStatusBarState() {
        if (hostStatusBarSaved || getActivity() == null) {
            return;
        }
        Window window = getActivity().getWindow();
        savedHostStatusBarColor = window.getStatusBarColor();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        savedHostLightStatusBars = controller.isAppearanceLightStatusBars();
        hostStatusBarSaved = true;
    }

    private void restoreHostStatusBarState() {
        if (!hostStatusBarSaved || getActivity() == null) {
            return;
        }
        Window window = getActivity().getWindow();
        window.setStatusBarColor(savedHostStatusBarColor);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(savedHostLightStatusBars);
        hostStatusBarSaved = false;
    }

    @ColorInt
    private static int resolveThemeStatusBarColor(@NonNull Context context) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.statusBarColor, typedValue, true)) {
            int color = typedValue.data;
            if (Color.alpha(color) != 0) {
                return color;
            }
        }
        return resolveThemeSurfaceColor(context);
    }

    private static boolean resolveThemeLightStatusBars(@NonNull Context context, @ColorInt int statusBarColor) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.windowLightStatusBar, typedValue, true)) {
            return typedValue.data != 0;
        }
        return ColorUtils.calculateLuminance(statusBarColor) > 0.5;
    }

    @ColorInt
    private static int resolveThemeSurfaceColor(@NonNull Context context) {
        TypedValue typedValue = new TypedValue();
        if (!context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)) {
            return 0;
        }
        return typedValue.data;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        defaultHomePromptHelper.onResume();
        if (bottomSheetView != null && !parked) {
            applySheetSurfaceAppearance();
            if (isFullyExpanded()) {
                applyExpandedStatusBarAppearance();
                postApplyExpandedStatusBarAppearance();
            }
        }
        if (!enrichmentDone) {
            return;
        }
        bgExecutor.execute(() -> {
            Context context = getContext();
            if (context == null) {
                return;
            }
            List<LauncherAppsModel> sortedSnapshot;
            synchronized (LauncherHomeActivity.arrayListAppsSearch) {
                LauncherAppsHelper.sortApps(context, LauncherHomeActivity.arrayListAppsSearch);
                sortedSnapshot = new ArrayList<>(LauncherHomeActivity.arrayListAppsSearch);
            }
            warmAppIconCache(context, sortedSnapshot);
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || launcherAppsAdapter == null || etSearch == null) {
                    return;
                }
                String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
                if (!query.trim().isEmpty()) {
                    searchApps(query);
                } else {
                    syncDisplayAppsFromSearch();
                    launcherAppsAdapter.notifyAppsDataChanged();
                }
                launcherAppsAdapter.updateSizeVisibility();
            });
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        clearExpandedStatusBarAppearance();
        notifyHomeSheetDismissed();
        super.onDismiss(dialog);
    }

    private void notifyHomeSheetDismissed() {
        if (!(getActivity() instanceof LauncherHomeActivity)) {
            return;
        }
        for (Fragment fragment : getActivity().getSupportFragmentManager().getFragments()) {
            if (fragment instanceof LauncherHomeFragment) {
                ((LauncherHomeFragment) fragment).clearAppsSheetOpeningState();
                break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        defaultHomePromptHelper.release();
        dismissingImmediately = true;
        animatedDismissRequested = false;
        dismissInProgress = false;
        appContextPopup.dismiss();
        mainHandler.removeCallbacksAndMessages(null);
        clearExpandedStatusBarAppearance();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            ((BottomSheetDialog) dialog).setDismissWithAnimation(false);
        }
        if (sheetBehavior != null) {
            sheetBehavior.removeBottomSheetCallback(sheetCallback);
            sheetBehavior = null;
        }
        if (bottomSheetView != null) {
            bottomSheetView.animate().cancel();
            bottomSheetView.removeOnLayoutChangeListener(interactiveLayoutListener);
            bottomSheetView = null;
        }
        contentRoot = null;
        statusBarScrim = null;
        interactiveSessionActive = false;
        hasPendingEndInteractive = false;
        pendingReadyRunnables.clear();
        clearInteractiveWindowFlags();
        if (backDismissCallback != null) {
            backDismissCallback.remove();
            backDismissCallback = null;
        }
        animatedDismissRequested = false;
        dismissingImmediately = false;
        if (receiverRegistered) {
            try {
                requireContext().unregisterReceiver(appChangeReceiver);
            } catch (Exception ignored) {
            }
            receiverRegistered = false;
        }
        if (launcherSettingsChangeListener != null) {
            LauncherSettingsHelper.unregisterChangeListener(launcherSettingsChangeListener);
            launcherSettingsChangeListener = null;
        }
        searchClearView();
        destroyLoadedNativeListAd();
        nativeListAdLoadInProgress = false;
        nativeListAdRequestToken++;
        nativeListAdVisible = false;
        rlNativeListAdView = null;
        slNativeListShimmer = null;
        flNativeListAd = null;
        launcherAppsAdapter = null;
        appsShown = false;
        enrichmentDone = false;
        enrichScheduled = false;
        etSearch = null;
        ivMore = null;
        llDefault = null;
        btnSetNow = null;
        rvApps = null;
        llNoApps = null;
        tvNoApps = null;
        tvMoreApps = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        bgExecutor.shutdownNow();
        super.onDestroy();
    }
}