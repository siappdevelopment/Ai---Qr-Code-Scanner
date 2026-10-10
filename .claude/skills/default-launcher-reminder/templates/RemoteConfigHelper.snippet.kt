// Inside the function that parses launcher_home_screen (after the other launcher_* values):
val reminderPushTime =
    jsonOptInt(launcher_home_screen, GlobalParameterManage.LAUNCHER_NOTIFICATION_PUSH_TIME, 24)
ADSMainClass.setLauncherNotificationPushShow(
    launcher_home_screen.optBoolean(GlobalParameterManage.LAUNCHER_NOTIFICATION_PUSH_SHOW, false)
)
ADSMainClass.setLauncherNotificationPushTime(reminderPushTime)
ADSMainClass.setLauncherNotificationPushTitle(
    launcher_home_screen.optString(GlobalParameterManage.LAUNCHER_NOTIFICATION_PUSH_TITLE, "")
)
ADSMainClass.setLauncherNotificationPushDescription(
    launcher_home_screen.optString(GlobalParameterManage.LAUNCHER_NOTIFICATION_PUSH_DESCRIPTION, "")
)
// Idempotent: schedules / updates / cancels the single unique reminder worker.
DefaultLauncherReminder.sync(context)
