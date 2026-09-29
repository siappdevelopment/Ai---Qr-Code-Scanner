package com.qrcode.scanner.launcher.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import com.qrcode.scanner.launcher.activities.LauncherHomeActivity;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.models.LauncherAppsModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LauncherAppsHelper {
    private LauncherAppsHelper() {
    }

    @SuppressLint("QueryPermissionsNeeded")
    public static ArrayList<LauncherAppsModel> loadInstalledApps(Context context) {
        ArrayList<LauncherAppsModel> apps = new ArrayList<>();
        if (context == null) {
            return apps;
        }

        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
        Set<String> seenPackages = new HashSet<>();
        for (ResolveInfo info : resolveInfos) {
            if (info.activityInfo == null) {
                continue;
            }
            try {
                String packageName = info.activityInfo.packageName;
                if (packageName == null || !seenPackages.add(packageName) || AppUtils.isOwnApp(context, packageName)) {
                    continue;
                }
                String appName = AppUtils.getResolveInfoLabelEnglish(context, info);
                if (appName.isEmpty()) {
                    continue;
                }
                Drawable appIcon = packageManager.getApplicationIcon(packageName);
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                apps.add(new LauncherAppsModel(appName, packageName, appIcon, packageInfo.firstInstallTime));
            } catch (Exception ignored) {
            }
        }

        sortApps(context, apps);
        return apps;
    }

    public static void sortApps(Context context, List<LauncherAppsModel> list) {
        if (context == null || list == null || list.isEmpty()) {
            return;
        }

        String serialize = LauncherSettingsHelper.getAppSerialize(context);
        if (LauncherSettingsHelper.SERIALIZE_NAME_DESC.equals(serialize)) {
            list.sort((a, b) -> b.getAppName().compareToIgnoreCase(a.getAppName()));
        } else if (LauncherSettingsHelper.SERIALIZE_INSTALLED_DATE.equals(serialize)) {
            list.sort((a, b) -> Long.compare(b.getInstallTime(), a.getInstallTime()));
        } else {
            list.sort((a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
        }
    }

    public static void replaceAppLists(List<LauncherAppsModel> loaded) {
        List<LauncherAppsModel> snapshot = loaded == null ? new ArrayList<>() : new ArrayList<>(loaded);
        synchronized (LauncherHomeActivity.arrayListAppsSearch) {
            LauncherHomeActivity.arrayListApps.clear();
            LauncherHomeActivity.arrayListApps.addAll(snapshot);
            LauncherHomeActivity.arrayListAppsSearch.clear();
            LauncherHomeActivity.arrayListAppsSearch.addAll(snapshot);
        }
    }

    public static boolean isUninstallableApp(Context context, String packageName) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return false;
        }
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
            int flags = applicationInfo.flags;
            if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                return false;
            }
            return (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
        } catch (Exception ignored) {
            return false;
        }
    }
}
