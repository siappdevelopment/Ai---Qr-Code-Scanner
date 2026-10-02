package com.qrcode.scanner.launcher.common;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import com.qrcode.scanner.launcher.activities.EventPromptActivity;

/**
 * Charger broadcasts are not delivered to a manifest receiver on current Android.
 * Register them for as long as this process is alive.
 */
public final class EventPromptWatch {
    private static boolean registered;

    private static final BroadcastReceiver RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null || intent.getAction() == null) {
                Log.d("EventPrompt", "live receiver ignored empty intent");
                return;
            }
            String action = intent.getAction();
            Log.d("EventPrompt", "live receiver action=" + action + " data=" + intent.getData());
            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                EventPromptLauncher.open(context, EventPromptActivity.KIND_CHARGE_IN);
                return;
            }
            if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                EventPromptLauncher.open(context, EventPromptActivity.KIND_CHARGE_OUT);
                return;
            }
            if (!Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                return;
            }
            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) || intent.getData() == null) {
                Log.d("EventPrompt", "live uninstall skipped: replacing or missing package");
                return;
            }
            String packageName = intent.getData().getSchemeSpecificPart();
            if (packageName == null || packageName.equals(context.getPackageName())) {
                Log.d("EventPrompt", "live uninstall skipped package=" + packageName);
                return;
            }
            Log.d("EventPrompt", "live uninstall package=" + packageName);
            EventPromptLauncher.open(context, EventPromptActivity.KIND_UNINSTALL);
        }
    };

    private EventPromptWatch() {
    }

    public static void register(Application application) {
        if (application == null || registered) {
            Log.d("EventPrompt", "live receivers already registered=" + registered);
            return;
        }
        IntentFilter power = new IntentFilter();
        power.addAction(Intent.ACTION_POWER_CONNECTED);
        power.addAction(Intent.ACTION_POWER_DISCONNECTED);
        IntentFilter packages = new IntentFilter();
        packages.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packages.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(RECEIVER, power, Context.RECEIVER_EXPORTED);
            application.registerReceiver(RECEIVER, packages, Context.RECEIVER_EXPORTED);
        } else {
            application.registerReceiver(RECEIVER, power);
            application.registerReceiver(RECEIVER, packages);
        }
        registered = true;
        Log.d("EventPrompt", "live receivers registered");
    }
}
