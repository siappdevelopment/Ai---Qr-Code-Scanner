package com.qrcode.scanner.launcher.models;

import android.graphics.drawable.Drawable;

public class LauncherAppsModel {
    private final String appName;
    private final String packageName;
    private final Drawable appIcon;
    private final long installTime;

    public LauncherAppsModel(String appName, String packageName, Drawable appIcon, long installTime) {
        this.appName = appName;
        this.packageName = packageName;
        this.appIcon = appIcon;
        this.installTime = installTime;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public long getInstallTime() {
        return installTime;
    }
}
