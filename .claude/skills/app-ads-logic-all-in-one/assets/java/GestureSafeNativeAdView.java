package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.nativead.NativeAdView;

/**
 * NativeAdView is final, so this frame sits around it.
 * An upward swipe that starts in the bottom gesture strip does not click the ad.
 * A normal tap still opens the ad.
 */
public class GestureSafeNativeAdView extends FrameLayout {
    private final int swipeSlop;
    private float downRawY;
    private boolean watchSwipe;
    private boolean stoleSwipe;

    public GestureSafeNativeAdView(@NonNull Context context) {
        this(context, null);
    }

    public GestureSafeNativeAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureSafeNativeAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        swipeSlop = android.view.ViewConfiguration.get(context).getScaledTouchSlop() * 3;
    }

    public static View wrap(@NonNull NativeAdView adView) {
        GestureSafeNativeAdView guard = new GestureSafeNativeAdView(adView.getContext());
        ViewGroup.LayoutParams existing = adView.getLayoutParams();
        int width = existing == null ? ViewGroup.LayoutParams.MATCH_PARENT : existing.width;
        int height = existing == null ? ViewGroup.LayoutParams.WRAP_CONTENT : existing.height;
        guard.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        guard.addView(adView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return guard;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                stoleSwipe = false;
                downRawY = ev.getRawY();
                watchSwipe = sitsOnScreenBottom() && rawYInGestureZone(ev.getRawY());
                break;
            case MotionEvent.ACTION_MOVE:
                if (watchSwipe && downRawY - ev.getRawY() > swipeSlop) {
                    stoleSwipe = true;
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                watchSwipe = false;
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!stoleSwipe) {
            return super.onTouchEvent(event);
        }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            stoleSwipe = false;
            watchSwipe = false;
        }
        return true;
    }

    private boolean sitsOnScreenBottom() {
        int[] location = new int[2];
        getLocationOnScreen(location);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int gap = screenHeight - (location[1] + getHeight());
        return gap <= dp(24);
    }

    private boolean rawYInGestureZone(float rawY) {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        return rawY >= screenHeight - gestureZonePx();
    }

    private int gestureZonePx() {
        int gesture = 0;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(this);
        if (insets != null) {
            gesture = insets.getInsets(WindowInsetsCompat.Type.systemGestures()).bottom;
        }
        return Math.max(gesture, dp(48));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
