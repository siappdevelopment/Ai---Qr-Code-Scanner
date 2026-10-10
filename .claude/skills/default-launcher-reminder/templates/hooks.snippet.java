// ---- 1. MyApplication.onCreate -> ProcessLifecycleOwner observer ----
@Override public void onStart(@NonNull LifecycleOwner owner) {
    isAppInForeground = true;
    DefaultLauncherReminder.cancelIfDefault(MyApplication.this);     // became default via Settings
    DefaultLauncherReminder.dismissNotification(MyApplication.this); // opening app clears stale reminder
}
// MyApplication must expose:  public static boolean isAppInForeground()  (set false in onStop)

// ---- 2. SplashActivity: in the post-delay navigation, BEFORE the normal startup flow ----
if (DefaultLauncherReminder.handleSplashEntry(this)) return;
StartupFlowManager.startStartupFlow(this);

// ---- 3. DefaultAppActivity (the "set as default" screen) ----
public static final String EXTRA_FROM_REMINDER = "extra_from_default_launcher_reminder";
boolean reminderEntry = getIntent().getBooleanExtra(EXTRA_FROM_REMINDER, false);
// After the user sets / cancels the default dialog:
if (reminderEntry) {
    DefaultLauncherReminder.cancelIfDefault(this);
    openLauncherHome();   // CLEAR_TASK, no onboarding/permission steps
    return;
}
