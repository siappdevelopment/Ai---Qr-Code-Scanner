package com.qrcode.scanner.launcher.helpers;

import android.content.Intent;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;

/**
 * Shows the source default-home popup only after a right-swipe has opened the QR app.
 * Scanner, create, history, and settings launches do not set the extra, so they do not show it.
 */
public final class DefaultHomePopupHost {
    public static final String EXTRA_RIGHT_SWIPE_QR_VISIBLE = "extra_right_swipe_qr_visible";

    private final ComponentActivity activity;
    private final DefaultHomePromptHelper helper;

    public DefaultHomePopupHost(@NonNull ComponentActivity activity) {
        this.activity = activity;
        this.helper = new DefaultHomePromptHelper(activity);
    }

    public void register() {
        helper.registerRoleLauncher();
    }

    public void onHostResume() {
        helper.onResume();
        Intent intent = activity.getIntent();
        if (intent == null || !intent.getBooleanExtra(EXTRA_RIGHT_SWIPE_QR_VISIBLE, false)) {
            return;
        }
        intent.removeExtra(EXTRA_RIGHT_SWIPE_QR_VISIBLE);
        View decor = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
        if (decor != null) {
            decor.post(helper::maybeShowDefaultHomePopup);
        } else {
            helper.maybeShowDefaultHomePopup();
        }
    }

    public void release() {
        helper.release();
    }

    public static void markRightSwipeQrVisible(@NonNull Intent intent) {
        intent.putExtra(EXTRA_RIGHT_SWIPE_QR_VISIBLE, true);
    }
}
