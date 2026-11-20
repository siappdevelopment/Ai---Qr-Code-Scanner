package com.qrcode.scanner.launcher.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.qrcode.scanner.spapp.R;
import com.qrcode.scanner.launcher.common.AppUtils;

/**
 * Guide card shown as a translucent Activity on top of the system "Display over other apps" screen.
 * It is a normal Activity window, so it needs no overlay permission. Any touch closes it so the user can then
 * allow the permission, and it closes once the permission is granted. Same flow as {@link DefaultHomeGuideActivity}.
 */
public class OverlayPermissionGuideActivity extends AppCompatActivity {
    private static final String EXTRA_RESHOWN = "extra_reshown";

    /** Starts the guide in its own task so it sits above the system screen. */
    public static void show(Context context) {
        try {
            context.startActivity(new Intent(context, OverlayPermissionGuideActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overlay_permission);

        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.BOTTOM;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);
        setFinishOnTouchOutside(true);

        findViewById(R.id.overlay_root).setOnClickListener(view -> finish());
    }

    /**
     * Settings opens its permission screen a moment after this guide starts and covers it. When that happens,
     * show the guide again once, so the second copy sits on top of the system screen.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (!isFinishing() && !getIntent().getBooleanExtra(EXTRA_RESHOWN, false)) {
            startActivity(new Intent(this, OverlayPermissionGuideActivity.class)
                    .putExtra(EXTRA_RESHOWN, true)
                    // MULTIPLE_TASK: without it Android only brings this same task to front.
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppUtils.hasOverlayPermission(this)) {
            finish();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        finish();
        return super.dispatchTouchEvent(motionEvent);
    }
}
