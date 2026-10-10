---
name: default-launcher-reminder
description: Add, modify or debug the "Make this app your default launcher" reminder notification - Firebase Remote Config flags (launcher_notification_push_show/time/title/description), WorkManager scheduling (periodic or sub-15-min chain), the worker that posts only when the app is NOT the default home app, notification tap flow (Splash -> DefaultAppActivity -> Launcher home), strings and hooks. Use when porting this feature to another project, changing its interval/text/Remote Config keys, or fixing why the reminder does not show or does not stop.
---

# Default Launcher reminder notification

Posts a periodic notification while the app is **not** the default home app. Remote Config controls on/off, interval and text. Reference implementation: Gallery app (`com.albums.photoeditor.photogallery`), package `launcher/common/`.

```
Firebase RC (launcher_home_screen) -> RemoteConfigHelper -> ADSMainClass prefs
        -> DefaultLauncherReminder.sync()  -> WorkManager (periodic OR one-time chain)
        -> DefaultLauncherReminderWorker.doWork() -> showNotification()
tap -> SplashActivity -> handleSplashEntry() -> DefaultAppActivity(reminder) -> Launcher home
```

## Templates (`templates/` next to this file)

| Template | Goes to |
|---|---|
| `DefaultLauncherReminder.java.tmpl` | `<pkg>/launcher/common/DefaultLauncherReminder.java` (scheduler, notification, tap handling) |
| `DefaultLauncherReminderWorker.java.tmpl` | same folder (the Worker) |
| `AdsPrefs.snippet.java` | prefs getters/setters into `ADSMainClass` + key constants into `GlobalParameterManage` |
| `RemoteConfigHelper.snippet.kt` | into the code that parses `launcher_home_screen` |
| `hooks.snippet.java` | `MyApplication` lifecycle observer, `SplashActivity`, `DefaultAppActivity` |
| `strings.xml.snippet` | `res/values/strings.xml` (+ translations) |
| `remote_config.json` | example Firebase Remote Config JSON |

Replace `{{PACKAGE}}` in the `.tmpl` files with the app's base package (the templates import `Advertisement.ADSMainClass`, `Advertisement.MyApplication`, `Activity.SplashActivity`, `launcher.activity.DefaultAppActivity`, `manager.StartupFlowManager`, `R`). Adjust those imports if the target project names them differently.

## Required project pieces (check first, port if missing)

- `Utils.isDefaultHomeApp(Context)`: `RoleManager.ROLE_HOME` held (API 29+), else `resolveActivity(ACTION_MAIN + CATEGORY_HOME)` package == own package.
- `StartupFlowManager.isFirstRunCompleted(Context)`: worker skips while onboarding is unfinished (that flow already leads to the Default screen).
- `MyApplication.isAppInForeground()`: worker skips while the app is visible.
- `DefaultAppActivity`: the "set as default" screen; must accept `EXTRA_FROM_REMINDER` and end on Launcher home.
- Dependency `androidx.work:work-runtime` (2.9.0 used) and `POST_NOTIFICATIONS` in the manifest. The project initializes WorkManager through `androidx.startup` (`WorkManagerInitializer` meta-data kept in the merged provider); do not remove it.
- Request the notification runtime permission (Android 13+) somewhere earlier in the app; `showNotification` silently returns false when notifications are disabled.

## Remote Config (Firebase)

All four keys live inside the `launcher_home_screen` JSON object:

| Key | Type | Meaning / default |
|---|---|---|
| `launcher_notification_push_show` | bool | master switch, default **false** (OFF unless RC enables) |
| `launcher_notification_push_time` | int | reverse-hours: interval seconds = 86400 / value. 12 -> 2h, 24 -> 1h, 48 -> 30 min. Default 24; <= 0 falls back to 24 |
| `launcher_notification_push_title` | string | blank -> built-in localized `default_launcher_reminder_title` |
| `launcher_notification_push_description` | string | blank -> built-in localized `default_launcher_reminder_text` |

Missing/invalid keys must never break the rest of the config apply (use `optXxx` + `jsonOptInt`). Title/description are stored on every apply and read at post time, so the next notification uses the newest values. See `templates/remote_config.json`.

## Behavior rules (keep when modifying)

1. **Scheduling strategy** (`plan()`): interval >= 900 s -> one unique `PeriodicWorkRequest` (`UPDATE` policy, initial delay = one interval). Below 900 s WorkManager cannot do periodic, so a unique one-time chain (`KEEP` first, `APPEND_OR_REPLACE` for the next run). Never both at once: each branch cancels the other. Floor 5 s for absurd values.
2. `sync()` is idempotent and is called **only when the config is applied**, never from `onResume`.
3. The schedule is **kept while the app is default**; the worker re-reads `isDefaultHomeApp` on every run and just posts nothing. This way removing the default later resumes reminders without opening the app. Only RC OFF cancels the work.
4. Skip reasons in the worker: already default, onboarding not finished, app in foreground, notifications disabled.
5. Fixed notification id (7001) so reminders replace, not stack. `dismissNotification` on app foreground; `cancelIfDefault` clears when the app becomes default.
6. Tap path goes through Splash (`EXTRA_FROM_REMINDER`, consumed once) so ads/init still run; only redirects to `DefaultAppActivity` if onboarding is done and app is not default.
7. WorkManager is best effort (Doze/OEM may delay runs). Do not promise exact timing.

## Integration checklist

1. Add RC keys + prefs getters/setters (`AdsPrefs.snippet.java`).
2. Parse them and call `DefaultLauncherReminder.sync(context)` (`RemoteConfigHelper.snippet.kt`).
3. Copy the two templates, fix package/imports.
4. Add the three hooks (`hooks.snippet.java`).
5. Add strings in all supported locales (the project has `tools/i18n` and `tools/translate_strings.ps1` for translations).
6. Publish the JSON in Firebase Remote Config (`templates/remote_config.json`).

## Debugging

Filter logcat by tag `DefaultLauncherNav`. Key lines: `reminder OFF -> worker cancelled`, `Default Launcher status = DEFAULT|NOT_DEFAULT`, `Calculated interval`, `Scheduling strategy = PERIODIC|ONE_TIME_CHAIN`, `Notification shown = false (reason)`.

Not showing? Check in order: RC `launcher_notification_push_show` true and fetched/activated; app really not default; onboarding finished; app in background; notification permission/channel enabled; battery optimization/Doze delaying the worker (`adb shell cmd jobscheduler run -f <pkg> <jobId>` to force a run).
