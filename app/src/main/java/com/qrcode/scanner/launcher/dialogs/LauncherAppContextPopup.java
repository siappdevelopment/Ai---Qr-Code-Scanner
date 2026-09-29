package com.qrcode.scanner.launcher.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qrcode.scanner.R;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.helpers.LauncherAppsHelper;
import com.qrcode.scanner.launcher.models.LauncherAppsModel;

public final class LauncherAppContextPopup {
    public interface Callback {
        void onAppInfo(@NonNull LauncherAppsModel app);

        void onUninstall(@NonNull LauncherAppsModel app, int position);
    }

    private static final int SCREEN_EDGE_MARGIN_PX_FALLBACK = 24;
    private static final int ANCHOR_GAP_DP = 0;

    @Nullable
    private PopupWindow popupWindow;

    public void dismiss() {
        if (popupWindow != null) {
            PopupWindow current = popupWindow;
            popupWindow = null;
            current.dismiss();
        }
    }

    @SuppressLint("InflateParams")
    public void show(@NonNull Context context, @NonNull View anchor, @NonNull LauncherAppsModel app, int position, @NonNull Callback callback) {
        dismiss();

        anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        View popupView = LayoutInflater.from(context).inflate(R.layout.dialog_launcher_app_action, null, false);
        LinearLayout llUninstall = popupView.findViewById(R.id.llUninstall);
        boolean uninstallable = LauncherAppsHelper.isUninstallableApp(context, app.getPackageName());
        llUninstall.setVisibility(uninstallable ? View.VISIBLE : View.GONE);

        PopupWindow window = new PopupWindow(popupView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
        window.setOutsideTouchable(true);
        window.setFocusable(true);
        window.setClippingEnabled(false);
        window.setElevation(AppUtils.dpToPx(context, 8));
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setAnimationStyle(android.R.style.Animation_Dialog);
        popupWindow = window;

        popupView.findViewById(R.id.llAppInfo).setOnClickListener(v -> {
            dismiss();
            callback.onAppInfo(app);
        });

        llUninstall.setOnClickListener(v -> {
            dismiss();
            callback.onUninstall(app, position);
        });

        window.setOnDismissListener(() -> {
            if (popupWindow == window) {
                popupWindow = null;
            }
        });

        popupView.setAlpha(0f);
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight = popupView.getMeasuredHeight();
        int gapPx = AppUtils.dpToPx(context, ANCHOR_GAP_DP);

        int[] anchorLocation = new int[2];
        anchor.getLocationOnScreen(anchorLocation);
        int anchorCenterX = anchorLocation[0] + anchor.getWidth() / 2;
        int anchorTop = anchorLocation[1];

        int x = anchorCenterX - popupWidth / 2;
        int y = anchorTop - popupHeight - gapPx;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int marginPx = AppUtils.dpToPx(context, 12);
        if (marginPx <= 0) {
            marginPx = SCREEN_EDGE_MARGIN_PX_FALLBACK;
        }
        x = Math.max(marginPx, Math.min(x, screenWidth - popupWidth - marginPx));
        if (y < marginPx) {
            y = marginPx;
        }

        View rootView = anchor.getRootView();
        window.showAtLocation(rootView, Gravity.NO_GRAVITY, x, y);
        popupView.animate().alpha(1f).setDuration(120L).start();
    }
}
