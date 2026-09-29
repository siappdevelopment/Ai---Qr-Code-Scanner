package com.qrcode.scanner.launcher.receivers;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.qrcode.scanner.R;
import com.qrcode.scanner.launcher.helpers.ReminderAlarmHelper;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_NAME = "Reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        String title = intent.getStringExtra(ReminderAlarmHelper.EXTRA_TITLE);
        int requestCode = intent.getIntExtra(ReminderAlarmHelper.EXTRA_REQUEST_CODE, -1);
        if (title == null || title.isEmpty() || requestCode < 0) {
            return;
        }

        Context appContext = context.getApplicationContext();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(appContext);
        ensureNotificationChannel(appContext, notificationManager);

        if (canPostNotifications(appContext)) {
            Intent openAppIntent = ReminderAlarmHelper.buildReminderLaunchIntent(appContext, requestCode);
            PendingIntent contentPendingIntent = PendingIntent.getActivity(appContext, requestCode, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, ReminderAlarmHelper.REMINDER_NOTIFICATION_CHANNEL_ID).setSmallIcon(R.drawable.img_app_icon_round).setContentTitle(appContext.getString(R.string.reminder_notification_title)).setContentText(title).setStyle(new NotificationCompat.BigTextStyle().bigText(title)).setPriority(NotificationCompat.PRIORITY_HIGH).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setOnlyAlertOnce(false).setOngoing(false).setAutoCancel(false).setWhen(System.currentTimeMillis()).setShowWhen(true).setContentIntent(contentPendingIntent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setTimeoutAfter(0L);
            }

            if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            notificationManager.notify(ReminderAlarmHelper.REMINDER_NOTIFICATION_TAG, requestCode, builder.build());
        }

        ReminderAlarmHelper.removeReminderByRequestCode(appContext, requestCode);
    }

    private static boolean canPostNotifications(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static void ensureNotificationChannel(Context context, NotificationManagerCompat notificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(ReminderAlarmHelper.REMINDER_NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.reminder_notification_channel_desc));
        channel.enableVibration(true);
        channel.enableLights(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setShowBadge(true);
        notificationManager.createNotificationChannel(channel);
    }
}
