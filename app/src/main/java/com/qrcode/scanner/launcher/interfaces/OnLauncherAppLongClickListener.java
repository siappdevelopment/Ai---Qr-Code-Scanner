package com.qrcode.scanner.launcher.interfaces;

import android.view.View;

import androidx.annotation.NonNull;

import com.qrcode.scanner.launcher.models.LauncherAppsModel;

public interface OnLauncherAppLongClickListener {
    void onLauncherAppLongClick(@NonNull LauncherAppsModel app, int position, @NonNull View anchorView);
}
