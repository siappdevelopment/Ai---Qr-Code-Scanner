package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Overlay that sits above a RecyclerView (the app drawer's sticky native ad). A vertical drag that starts
 * on the ad scrolls the RecyclerView underneath instead of being swallowed by the ad. Once the drag passes
 * the touch slop the ad's children get ACTION_CANCEL, so the drag never turns into an ad click. A normal tap
 * is untouched. Works for Google and Quiz ads alike because both are children of this layout.
 */
public class ScrollForwardingRelativeLayout extends RelativeLayout {
    private final int touchSlop;
    @Nullable
    private RecyclerView scrollTarget;
    private float downX;
    private float downY;
    private boolean forwarding;

    public ScrollForwardingRelativeLayout(@NonNull Context context) {
        this(context, null);
    }

    public ScrollForwardingRelativeLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollForwardingRelativeLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setScrollTarget(@Nullable RecyclerView target) {
        scrollTarget = target;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (scrollTarget == null) {
            return false;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getRawX();
                downY = ev.getRawY();
                forwarding = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!forwarding && isVerticalDrag(ev)) {
                    startForwarding(ev);
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scrollTarget == null) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Nothing under the finger handled it (gap around the card): still follow the drag.
                downX = event.getRawX();
                downY = event.getRawY();
                forwarding = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!forwarding && isVerticalDrag(event)) {
                    startForwarding(event);
                    return true;
                }
                if (forwarding) {
                    forward(event, event.getActionMasked());
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (forwarding) {
                    forward(event, event.getActionMasked());
                    forwarding = false;
                }
                return true;
            default:
                return true;
        }
    }

    private boolean isVerticalDrag(MotionEvent ev) {
        float dy = Math.abs(ev.getRawY() - downY);
        float dx = Math.abs(ev.getRawX() - downX);
        return dy > touchSlop && dy > dx;
    }

    private void startForwarding(MotionEvent ev) {
        forwarding = true;
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        forward(ev, MotionEvent.ACTION_DOWN);
    }

    private void forward(MotionEvent source, int action) {
        RecyclerView target = scrollTarget;
        if (target == null) {
            return;
        }
        int[] location = new int[2];
        target.getLocationOnScreen(location);
        MotionEvent copy = MotionEvent.obtain(source);
        copy.setAction(action);
        copy.setLocation(source.getRawX() - location[0], source.getRawY() - location[1]);
        target.dispatchTouchEvent(copy);
        copy.recycle();
    }
}
