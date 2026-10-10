// ---- ADSMainClass (prefs-backed getters/setters). Safe defaults: OFF, 24, "" ----
private static final String LauncherNotificationPushShow = "LauncherNotificationPushShow";
private static final String LauncherNotificationPushTime = "LauncherNotificationPushTime";
private static final String LauncherNotificationPushTitle = "LauncherNotificationPushTitle";
private static final String LauncherNotificationPushDescription = "LauncherNotificationPushDescription";

public static boolean getLauncherNotificationPushShow() { return ADSPrefManage().getBoolean(LauncherNotificationPushShow, false); }
public static void setLauncherNotificationPushShow(boolean v) { ADSPrefManage().edit().putBoolean(LauncherNotificationPushShow, v).apply(); }
public static int getLauncherNotificationPushTime() { return ADSPrefManage().getInt(LauncherNotificationPushTime, 24); }
public static void setLauncherNotificationPushTime(int v) { ADSPrefManage().edit().putInt(LauncherNotificationPushTime, v).apply(); }
public static String getLauncherNotificationPushTitle() { return ADSPrefManage().getString(LauncherNotificationPushTitle, ""); }
public static void setLauncherNotificationPushTitle(String v) { ADSPrefManage().edit().putString(LauncherNotificationPushTitle, v == null ? "" : v).apply(); }
public static String getLauncherNotificationPushDescription() { return ADSPrefManage().getString(LauncherNotificationPushDescription, ""); }
public static void setLauncherNotificationPushDescription(String v) { ADSPrefManage().edit().putString(LauncherNotificationPushDescription, v == null ? "" : v).apply(); }

// ---- GlobalParameterManage (Remote Config key names) ----
public static final String LAUNCHER_NOTIFICATION_PUSH_SHOW = "launcher_notification_push_show";
public static final String LAUNCHER_NOTIFICATION_PUSH_TIME = "launcher_notification_push_time";
public static final String LAUNCHER_NOTIFICATION_PUSH_TITLE = "launcher_notification_push_title";
public static final String LAUNCHER_NOTIFICATION_PUSH_DESCRIPTION = "launcher_notification_push_description";
