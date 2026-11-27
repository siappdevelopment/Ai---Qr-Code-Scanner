package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Keeps every screen of the app in place while a full-screen ad (Interstitial / App Open) is shown or closed. Those ads
 * hide the status bar, the screen underneath then gets a top inset of 0 and its content jumps upward. This reports the
 * status bar with its normal height to the app's screens even while it is hidden, so their padding never changes. No
 * screen of the app hides the status bar on purpose. Installed once; covers XML and Compose screens.
 */
public final class StableStatusBarInsets implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        // Only this app's own screens: ad screens of the SDK handle their bars themselves.
        if (!activity.getClass().getName().startsWith("com.qrcode.scanner")) {
            return;
        }
        // Set here (not in onCreate): asking for the decor view earlier would lock the window features.
        View decor = activity.getWindow() == null ? null : activity.getWindow().peekDecorView();
        if (decor == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(decor, (view, insets) -> {
            if (insets.isVisible(WindowInsetsCompat.Type.statusBars())) {
                return ViewCompat.onApplyWindowInsets(view, insets);
            }
            Insets bar = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars());
            WindowInsetsCompat stable = new WindowInsetsCompat.Builder(insets)
                    .setInsets(WindowInsetsCompat.Type.statusBars(), bar)
                    .setVisible(WindowInsetsCompat.Type.statusBars(), true)
                    .build();
            return ViewCompat.onApplyWindowInsets(view, stable);
        });
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
