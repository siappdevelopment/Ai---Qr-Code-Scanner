package com.qrcode.scanner.launcher.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.qrcode.scanner.spapp.R;

@SuppressLint("InflateParams")
public class CallEndViewManager {
    public void showCallEnd(Context context, Intent intent) {
        if (AppUtils.isDefaultHomeApp(context)) {
            // The default Home app may start screens from the background, so no overlay window is needed.
            context.startActivity(intent);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        View floatView = inflater.inflate(R.layout.call_end_view, null);

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, PixelFormat.TRANSLUCENT);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(floatView, params);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                context.startActivity(intent);
            } catch (Exception startError) {
            }
            try {
                windowManager.removeView(floatView);
            } catch (Exception removeError) {
            }
        }, 250);
    }
}
