package com.qrcode.scanner.launcher.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.qrcode.scanner.launcher.activities.EventPromptActivity;
import com.qrcode.scanner.launcher.common.EventPromptLauncher;

public class EventPromptReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean installed = intent != null && Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction());
        if (context == null || intent == null
                || (!installed && !Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction()))) {
            return;
        }
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) || intent.getData() == null) {
            return;
        }
        String packageName = intent.getData().getSchemeSpecificPart();
        if (packageName == null || packageName.equals(context.getPackageName())) {
            return;
        }
        EventPromptLauncher.open(context, installed ? EventPromptActivity.KIND_INSTALL : EventPromptActivity.KIND_UNINSTALL);
    }
}
