package com.qrcode.scanner.launcher.helpers;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qrcode.scanner.MainActivity;
import com.qrcode.scanner.launcher.models.ReminderModel;
import com.qrcode.scanner.launcher.receivers.ReminderReceiver;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class ReminderAlarmHelper {
    public static final String PREFS_NAME = "reminder_preferences";
    public static final String KEY_REMINDER_LIST = "reminder_list";
    public static final String ACTION_REMINDER_ALARM = "com.phonecleaner.virusclean.ACTION_REMINDER_ALARM";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_REQUEST_CODE = "requestCode";
    public static final String EXTRA_COLOR = "color";
    public static final String EXTRA_FROM_REMINDER_NOTIFICATION = "fromReminderNotification";
    public static final String REMINDER_NOTIFICATION_TAG = "app_reminder";
    public static final String REMINDER_NOTIFICATION_CHANNEL_ID = "reminder_alerts_persistent";

    private ReminderAlarmHelper() {
    }

    public static PendingIntent buildReminderPendingIntent(Context context, ReminderModel reminder) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMINDER_ALARM);
        intent.putExtra(EXTRA_TITLE, reminder.getTitle());
        intent.putExtra(EXTRA_REQUEST_CODE, reminder.getRequestCode());
        intent.putExtra(EXTRA_COLOR, reminder.getColor());
        return PendingIntent.getBroadcast(context, reminder.getRequestCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleReminder(Context context, ReminderModel reminder) {
        if (context == null || reminder == null) {
            return;
        }

        long triggerAtMillis = reminder.getTime();
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = buildReminderPendingIntent(context, reminder);
        scheduleAlarm(alarmManager, triggerAtMillis, pendingIntent);
    }

    public static void cancelReminder(Context context, ReminderModel reminder) {
        if (context == null || reminder == null) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = buildReminderPendingIntent(context, reminder);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        pendingIntent.cancel();
    }

    public static void rescheduleAll(Context context) {
        if (context == null) {
            return;
        }

        List<ReminderModel> reminders = loadReminderList(context);
        if (reminders.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (ReminderModel reminder : reminders) {
            if (reminder != null && reminder.getTime() > now) {
                scheduleReminder(context, reminder);
            }
        }
    }

    public static ArrayList<ReminderModel> loadReminderList(Context context) {
        ArrayList<ReminderModel> reminders = new ArrayList<>();
        if (context == null) {
            return reminders;
        }

        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = preferences.getString(KEY_REMINDER_LIST, "");
        if (json.isEmpty()) {
            return reminders;
        }

        Type type = new TypeToken<ArrayList<ReminderModel>>() {
        }.getType();
        ArrayList<ReminderModel> savedList = new Gson().fromJson(json, type);
        if (savedList != null) {
            reminders.addAll(savedList);
        }
        return reminders;
    }

    public static void saveReminderList(Context context, List<ReminderModel> reminders) {
        if (context == null) {
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_REMINDER_LIST, new Gson().toJson(reminders)).apply();
    }

    public static void removeReminderByRequestCode(Context context, int requestCode) {
        if (context == null || requestCode <= 0) {
            return;
        }

        ArrayList<ReminderModel> reminders = loadReminderList(context);
        if (reminders.isEmpty()) {
            return;
        }

        boolean removed = false;
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).getRequestCode() == requestCode) {
                reminders.remove(i);
                removed = true;
                break;
            }
        }

        if (removed) {
            saveReminderList(context, reminders);
        }
    }

    public static void cancelReminderNotification(Context context, int requestCode) {
        if (context == null || requestCode < 0) {
            return;
        }
        NotificationManagerCompat.from(context).cancel(REMINDER_NOTIFICATION_TAG, requestCode);
    }

    public static Intent buildReminderLaunchIntent(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_FROM_REMINDER_NOTIFICATION, true);
        intent.putExtra(EXTRA_REQUEST_CODE, requestCode);
        return intent;
    }

    public static void handleReminderLaunchIntent(Context context, Intent intent) {
        if (context == null || intent == null || !intent.getBooleanExtra(EXTRA_FROM_REMINDER_NOTIFICATION, false)) {
            return;
        }

        int requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, -1);
        if (requestCode >= 0) {
            cancelReminderNotification(context, requestCode);
        }
        intent.removeExtra(EXTRA_FROM_REMINDER_NOTIFICATION);
        intent.removeExtra(EXTRA_REQUEST_CODE);
    }

    @SuppressLint("ScheduleExactAlarm")
    private static void scheduleAlarm(AlarmManager alarmManager, long triggerAtMillis, PendingIntent operation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            scheduleInexactWhileIdle(alarmManager, triggerAtMillis, operation);
            return;
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
        } catch (SecurityException ignored) {
            scheduleInexactWhileIdle(alarmManager, triggerAtMillis, operation);
        }
    }

    private static void scheduleInexactWhileIdle(AlarmManager alarmManager, long triggerAtMillis, PendingIntent operation) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
    }
}
