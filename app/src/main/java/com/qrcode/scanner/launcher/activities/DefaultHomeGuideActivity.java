package com.qrcode.scanner.launcher.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.AppUtils;

/**
 * Guide card shown as a translucent Activity on top of the system "Default home app" screen.
 * It is a normal Activity window, so it needs no overlay permission. Any touch closes it, and it
 * closes once this app is the default Home.
 */
public class DefaultHomeGuideActivity extends AppCompatActivity {
    private static final String EXTRA_RESHOWN = "extra_reshown";

    private ValueAnimator handClickAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("Milind", "onCreate: " );
        setContentView(R.layout.activity_overlay_permission_default_home);
        ((ImageView) findViewById(R.id.ivGuideIcon)).setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));
        ((TextView) findViewById(R.id.tvGuideName)).setText(getApplicationInfo().loadLabel(getPackageManager()));

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.CENTER;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.dimAmount = 0.2f;
        window.setAttributes(params);
        setFinishOnTouchOutside(true);

        findViewById(R.id.overlay_root).setOnClickListener(view -> finish());
        startHandClickAnimation();
    }

    /**
     * Settings opens its Default home app list a moment after this guide starts and covers it. When
     * that happens, show the guide again once, so the second copy sits on top of the system list.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (!isFinishing() && !getIntent().getBooleanExtra(EXTRA_RESHOWN, false)) {
            Log.e("Milind", "guide covered by system screen, showing again on top");
            startActivity(new Intent(this, DefaultHomeGuideActivity.class)
                    .putExtra(EXTRA_RESHOWN, true)
                    // MULTIPLE_TASK: without it Android only brings this same task to front (START_TASK_TO_FRONT).
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppUtils.isDefaultHomeApp(this)) {
            finish();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        finish();
        return super.dispatchTouchEvent(motionEvent);
    }

    /** Looping "tap" on the radio: hand rises while the ripple and the radio dot grow in. */
    private void startHandClickAnimation() {
        final View hand = findViewById(R.id.homeGuideHand);
        final View ripple = findViewById(R.id.homeGuideRipple);
        final View dot = findViewById(R.id.homeGuideRadioDot);
        final float rise = 30f * getResources().getDisplayMetrics().density;
        handClickAnimator = ValueAnimator.ofFloat(0f, 1f);
        handClickAnimator.setDuration(900L);
        handClickAnimator.setRepeatCount(ValueAnimator.INFINITE);
        handClickAnimator.setInterpolator(new DecelerateInterpolator());
        handClickAnimator.addUpdateListener(animation -> {
            float p = (float) animation.getAnimatedValue();
            hand.setTranslationY(rise * (1f - p));
            ripple.setAlpha(p);
            ripple.setScaleX(0.55f + 0.45f * p);
            ripple.setScaleY(0.55f + 0.45f * p);
            dot.setAlpha(p);
            dot.setScaleX(0.3f + 0.7f * p);
            dot.setScaleY(0.3f + 0.7f * p);
        });
        handClickAnimator.start();
    }

    @Override
    protected void onDestroy() {
        Log.e("Milind", "onDestroy: " );
        if (handClickAnimator != null) {
            handClickAnimator.cancel();
        }
        super.onDestroy();
    }
}
