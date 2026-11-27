package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.nativead.NativeAd;
import com.qrcode.scanner.spapp.R;

/**
 * Back on the main screen: a bottom dialog with a "Tap To exit" button and a medium native ad.
 * Back or a tap outside closes the dialog and keeps the user in the app. With the dialog switched off in Remote Config
 * ({@code ExitDialog}) Back closes the app at once.
 * <p>
 * The native ad uses the app's common native loader, so the usual rules apply: location lists, network, Quiz priority,
 * the Quiz fallback after a Google failure, revenue and analytics logging. It is requested each time the dialog opens
 * and destroyed when the dialog closes.
 */
public final class ExitDialog {
    @Nullable
    private static Dialog showing;

    private ExitDialog() {
    }

    /** The Back press that would leave the app. */
    public static void onExitRequested(@NonNull Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!ExitDialogConfig.isEnabled(activity)) {
            activity.finishAffinity();
            return;
        }
        show(activity);
    }

    /** Closes the dialog when its screen goes away (no leaked window, the ad is destroyed). */
    public static void dismiss(@NonNull Activity activity) {
        Dialog dialog = showing;
        if (dialog != null && dialog.getOwnerActivity() == activity) {
            dialog.dismiss();
            showing = null;
        }
    }

    private static void show(@NonNull Activity activity) {
        if (showing != null && showing.isShowing()) {
            return;
        }
        Dialog dialog = new Dialog(activity);
        dialog.setOwnerActivity(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_exit);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.findViewById(R.id.tvExitButton).setOnClickListener(view -> {
            dialog.dismiss();
            activity.finishAffinity();
        });

        final NativeAd[] ad = new NativeAd[1];
        final boolean[] closed = new boolean[1];
        dialog.setOnDismissListener(d -> {
            closed[0] = true;
            if (ad[0] != null) {
                ad[0].destroy();
                ad[0] = null;
            }
            if (showing == d) {
                showing = null;
            }
        });
        loadAd(activity, dialog, ad, closed);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.getAttributes().windowAnimations = R.style.ExitDialogAnimation;
        }
        showing = dialog;
        if (window == null) {
            dialog.show();
            return;
        }
        // The main screen keeps the navigation bar hidden (swipe to show): the dialog must not bring it back. Not
        // focusable while it opens, so the bar does not flash before it is hidden.
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show();
        WindowInsetsControllerCompat insets = WindowCompat.getInsetsController(window, window.getDecorView());
        insets.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        insets.hide(WindowInsetsCompat.Type.navigationBars());
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    private static void loadAd(Activity activity, Dialog dialog, NativeAd[] ad, boolean[] closed) {
        RelativeLayout area = dialog.findViewById(R.id.rlExitAd);
        ShimmerFrameLayout shimmer = dialog.findViewById(R.id.shimmerExitAd);
        FrameLayout host = dialog.findViewById(R.id.flExitAd);
        String id = ExitDialogConfig.nativeId(activity);
        if (id.isEmpty()) {
            area.setVisibility(View.GONE);
            return;
        }
        area.setVisibility(View.VISIBLE);
        AdPlacement.loadNativeAd(activity, id, area, shimmer, host, "medium", loaded -> {
            if (loaded == null) {
                return; // Quiz ad: nothing to destroy
            }
            if (closed[0]) {
                loaded.destroy();
            } else {
                ad[0] = loaded;
            }
        }, () -> {
            shimmer.stopShimmer();
            area.setVisibility(View.GONE);
        });
    }
}
