package com.qrcode.scanner.launcher.common;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;





/**
 * The job behind {@link DefaultLauncherReminder}. Every run re-reads the real state, so a config
 * switch-off or the app becoming the default stops the reminders immediately. Runs that belong to
 * the sub-15-minute one-time chain schedule the next run themselves.
 */
public class DefaultLauncherReminderWorker extends Worker {

    private static final String TAG = DefaultLauncherReminder.TAG;

    public DefaultLauncherReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        DefaultLauncherReminder.Config.init(context);
        boolean chain = getInputData().getBoolean(DefaultLauncherReminder.KEY_CHAIN, false);
        try {
            

            // Remote Config OFF → no notification, stop (periodic and chain both cancelled).
            if (!DefaultLauncherReminder.Config.getLauncherNotificationPushShow()) {
                DefaultLauncherReminder.cancelWork(context);
                return Result.success();
            }

            // Notification condition: the real default-launcher state is read on EVERY run. While
            // the app is the default nothing is posted (and any leftover reminder is cleared), but the
            // schedule is NOT cancelled — if the user later removes the default without opening the
            // app, a following run finds NOT_DEFAULT and posts the reminder again.
            boolean isDefault = AppUtils.isDefaultHomeApp(context);

            String skipReason = null;
            if (isDefault) {
                skipReason = "already default";
                DefaultLauncherReminder.onDefaultLauncherConfirmed(context);
            } else if (!AppUtils.hasCompletedOnboarding(context)) {
                skipReason = "onboarding not finished"; // that flow already leads to the Default screen
            } else if (ProcessAppOpen.isAppInForeground()) {
                skipReason = "app in foreground";
            }
            boolean shown = skipReason == null && DefaultLauncherReminder.showNotification(context);
        } catch (Exception e) {
        }
        if (chain) {
            DefaultLauncherReminder.scheduleNextChainRun(context);
        }
        return Result.success();
    }
}
