package com.qrcode.scanner.launcher.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.qrcode.scanner.launcher.activities.EventPromptActivity;
import com.qrcode.scanner.launcher.common.EventPromptLauncher;

public class EventPromptReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("EventPrompt", "package receiver action=" + (intent == null ? "null" : intent.getAction())
                + " data=" + (intent == null ? "null" : intent.getData())
                + " replacing=" + (intent != null && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)));
        if (context == null || intent == null || !Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
            Log.d("EventPrompt", "uninstall skipped: not PACKAGE_REMOVED");
            return;
        }
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) || intent.getData() == null) {
            Log.d("EventPrompt", "uninstall skipped: replacing or missing package");
            return;
        }
        String packageName = intent.getData().getSchemeSpecificPart();
        if (packageName == null || packageName.equals(context.getPackageName())) {
            Log.d("EventPrompt", "uninstall skipped: package=" + packageName);
            return;
        }
        Log.d("EventPrompt", "uninstall broadcast received package=" + packageName);
        EventPromptLauncher.open(context, EventPromptActivity.KIND_UNINSTALL);
    }
}
