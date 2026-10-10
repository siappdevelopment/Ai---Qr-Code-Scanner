package com.qrcode.scanner.launcher.helpers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.TrackOnce;

/**
 * In-launcher set-as-default control and the source automatic popup.
 * One RoleManager request path. The popup does not continue onboarding.
 */
public final class DefaultHomePromptHelper {
    public static final long POLL_INTERVAL_MS = 500L;

    @Nullable
    private final Fragment fragment;
    @Nullable
    private final ComponentActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout llDefault;
    private ActivityResultLauncher<Intent> roleLauncher;
    private boolean waitingForDefaultHome;
    private boolean defaultHomeSelectionStarted;
    private boolean defaultHomePopupLaunchInProgress;
    private boolean launcherRegistered;
    @Nullable
    private RoleFlowListener roleFlowListener;

    public interface RoleFlowListener {
        void onRoleFlowFinished();
    }

    public DefaultHomePromptHelper(@NonNull Fragment fragment) {
        this.fragment = fragment;
        this.activity = null;
    }

    public DefaultHomePromptHelper(@NonNull ComponentActivity activity) {
        this.fragment = null;
        this.activity = activity;
    }

    public void setRoleFlowListener(@Nullable RoleFlowListener listener) {
        this.roleFlowListener = listener;
    }

    public void registerRoleLauncher() {
        if (launcherRegistered || (fragment == null && activity == null)) {
            return;
        }
        launcherRegistered = true;
        ActivityResultContracts.StartActivityForResult contract = new ActivityResultContracts.StartActivityForResult();
        if (fragment != null) {
            roleLauncher = fragment.registerForActivityResult(contract, result -> onRoleResult());
        } else {
            roleLauncher = activity.registerForActivityResult(contract, result -> onRoleResult());
        }
    }

    public void bind(@Nullable LinearLayout llDefault) {
        this.llDefault = llDefault;
        updateVisibility();
    }

    public void updateVisibility() {
        if (llDefault == null || !isActive()) {
            return;
        }
        llDefault.setVisibility(AppUtils.isDefaultHomeApp(getContext()) ? GONE : VISIBLE);
    }

    /**
     * True when maybeShowDefaultHomePopup() would show the Default Home permission screen right now.
     * Used by the Right Swipe flow to skip its ad in that case.
     */
    public boolean canShowDefaultHomePopup() {
        if (defaultHomePopupLaunchInProgress || waitingForDefaultHome || !isActive()) {
            return false;
        }
        Activity hostActivity = getHostActivity();
        if (hostActivity == null || hostActivity.isFinishing()) {
            return false;
        }
        if (!AdPlacement.getDefaultAppPopupShow()) {
            return false;
        }
        if (AppUtils.isDefaultHomeApp(getContext())) {
            return false;
        }
        if (!AdPlacement.canShowDefaultAppPopup(getContext())) {
            return false;
        }
        return AppUtils.createDefaultHomeRoleRequestIntent(getContext()) != null;
    }

    public void maybeShowDefaultHomePopup() {
        if (!canShowDefaultHomePopup()) {
            return;
        }
        if (roleLauncher == null) {
            registerRoleLauncher();
        }
        Intent roleIntent = AppUtils.createDefaultHomeRoleRequestIntent(getContext());
        if (roleIntent == null || roleLauncher == null) {
            return;
        }
        AdPlacement.setDefaultAppPopupLastShowTime(getContext(), System.currentTimeMillis());
        defaultHomePopupLaunchInProgress = true;
        waitingForDefaultHome = true;
        defaultHomeSelectionStarted = true;
        roleLauncher.launch(roleIntent);
    }

    public void handleSetAsDefaultClick() {
        if (!isActive() || waitingForDefaultHome || defaultHomePopupLaunchInProgress) {
            return;
        }
        Activity hostActivity = getHostActivity();
        if (hostActivity == null) {
            return;
        }
        if (AppUtils.isDefaultHomeApp(getContext())) {
            completeSuccess();
            return;
        }
        waitingForDefaultHome = true;
        defaultHomeSelectionStarted = true;
        Intent roleIntent = AppUtils.createDefaultHomeRoleRequestIntent(getContext());
        if (roleIntent != null && roleLauncher != null) {
            roleLauncher.launch(roleIntent);
            return;
        }
        if (!AppUtils.openDefaultHomeChooser(hostActivity)) {
            completeCancelled();
            return;
        }
        startPolling();
    }

    public void onResume() {
        if (waitingForDefaultHome && isActive() && AppUtils.isDefaultHomeApp(getContext())) {
            completeSuccess();
        } else {
            updateVisibility();
        }
    }

    public void release() {
        waitingForDefaultHome = false;
        defaultHomeSelectionStarted = false;
        defaultHomePopupLaunchInProgress = false;
        stopPolling();
        llDefault = null;
    }

    private void onRoleResult() {
        defaultHomePopupLaunchInProgress = false;
        if (!waitingForDefaultHome) {
            updateVisibility();
            return;
        }
        if (!isActive()) {
            return;
        }
        if (AppUtils.isDefaultHomeApp(getContext())) {
            completeSuccess();
        } else {
            completeCancelled();
        }
    }

    private void completeSuccess() {
        waitingForDefaultHome = false;
        defaultHomeSelectionStarted = false;
        stopPolling();
        updateVisibility();
        if (isActive() && AppUtils.isDefaultHomeApp(getContext())) {
            AppUtils.setDefaultHomeScreenCompleted(getContext(), true);
            TrackOnce.trackScreenOnce(getContext(), "DEFAULT_HOME_APP_SET");
        }
        notifyRoleFlowFinished();
    }

    private void completeCancelled() {
        waitingForDefaultHome = false;
        stopPolling();
        updateVisibility();
        if (defaultHomeSelectionStarted && isActive()) {
            TrackOnce.trackScreenOnce(getContext(), "DEFAULT_HOME_APP_CANCEL");
            defaultHomeSelectionStarted = false;
        }
        notifyRoleFlowFinished();
    }

    private void notifyRoleFlowFinished() {
        if (roleFlowListener == null || !isActive()) {
            return;
        }
        RoleFlowListener listener = roleFlowListener;
        roleFlowListener = null;
        listener.onRoleFlowFinished();
    }

    private void startPolling() {
        handler.removeCallbacks(pollRunnable);
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void stopPolling() {
        handler.removeCallbacks(pollRunnable);
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isActive() || !waitingForDefaultHome) {
                return;
            }
            Activity hostActivity = getHostActivity();
            if (hostActivity == null || hostActivity.isFinishing()) {
                return;
            }
            if (AppUtils.isDefaultHomeApp(getContext())) {
                completeSuccess();
                return;
            }
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    private boolean isActive() {
        if (fragment != null) {
            return fragment.isAdded();
        }
        if (activity != null) {
            return !activity.isFinishing() && !activity.isDestroyed();
        }
        return false;
    }

    @Nullable
    private Activity getHostActivity() {
        if (fragment != null) {
            return fragment.getActivity();
        }
        return activity;
    }

    @Nullable
    private Context getContext() {
        if (fragment != null) {
            return fragment.getContext();
        }
        return activity;
    }
}
