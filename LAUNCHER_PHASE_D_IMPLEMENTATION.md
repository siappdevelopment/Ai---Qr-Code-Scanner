# Launcher Phase D Implementation

Default Home / RoleManager only. The Source Launcher project was not modified. Java stayed Java. Existing Compose screens, Scanner, Create, History, Room, and Settings were not changed.

Debug build: `:app:assembleDebug` succeeded.

Device verification was not performed.

---

## What this phase did

The existing set-as-default controls now use Android’s real Home role.

- Android 10 (API 29) and above: `RoleManager.ROLE_HOME` via `createRequestRoleIntent`.
- Android 7 through 9 (API 24–28): the system Home chooser (`ACTION_MAIN` + `CATEGORY_HOME`). `RoleManager` is not called on those versions.

There is one request implementation: `DefaultHomePromptHelper.handleSetAsDefaultClick`. The home page and the apps drawer both use that method. Neither path finishes `LauncherHomeActivity`.

`LauncherHomeActivity` is still the only `MAIN` + `HOME` + `DEFAULT` activity. `MainActivity` is still the only `MAIN` + `LAUNCHER` activity.

---

## Files created

- `app/src/main/java/com/qrcode/scanner/launcher/helpers/DefaultHomePromptHelper.java`

## Files modified

- `app/src/main/java/com/qrcode/scanner/launcher/common/AppUtils.java`
- `app/src/main/java/com/qrcode/scanner/launcher/fragments/LauncherHomeFragment.java`
- `app/src/main/java/com/qrcode/scanner/launcher/dialogs/LauncherAppsBottomSheet.java`

## Manifest

No manifest changes. No new permissions. `CATEGORY_HOME` stays on `LauncherHomeActivity`. `CATEGORY_LAUNCHER` stays on `MainActivity`.

---

## Source classes reused / adapted

| Source | Target | What was kept |
| --- | --- | --- |
| `helpers/DefaultHomePromptHelper.java` | `launcher/helpers/DefaultHomePromptHelper.java` | Bind, visibility, click, role result, pre-29 poll, resume re-check, release |
| `AppUtils.isDefaultHomeApp` | `launcher/common/AppUtils.java` | API 29+ `isRoleHeld(ROLE_HOME)`, otherwise `MATCH_DEFAULT_ONLY` resolve |
| `AppUtils.createDefaultHomeRoleRequestIntent` | same | Null below API 29, if the role is unavailable, or if it is already held |
| `AppUtils.openDefaultHomeChooser` | same | Home chooser for API 24–28 |
| `AppUtils.setDefaultHomeScreenCompleted` | same | Writes `screen_flow` / `default_home_completed` on a confirmed success. Nothing in this phase reads that flag |

Removed from the helper because those dependencies are not in this app and are out of scope: `AdPlacement`, `TrackOnce`, and `maybeShowDefaultHomePopup`.

`DefaultActivity` was not migrated. In the source it is the first-run onboarding screen (ads, phone-state permission, `ScreenFlowNavigation`) and a second `ROLE_HOME` request. This phase keeps a single request path on the launcher shell.

---

## RoleManager

`DefaultHomePromptHelper.registerRoleLauncher()` registers `ActivityResultContracts.StartActivityForResult` in `onCreate`, before the fragment is started.

`handleSetAsDefaultClick()`:

1. Returns immediately if a request is already in progress.
2. If `isDefaultHomeApp` is already true, hides the prompt.
3. If `createDefaultHomeRoleRequestIntent` returns an intent, launches it with the registered launcher. The result code is ignored.
4. If that intent is null (API below 29, role unavailable, or already held), opens the system Home chooser and polls every 500 ms on the main looper. The poll only calls `isDefaultHomeApp`.

No SMS sync, database work, or network work runs in the request or result path. The preference write uses `apply()`.

## Default-home detection

`AppUtils.isDefaultHomeApp`:

- API 29+ and `ROLE_HOME` available: `RoleManager.isRoleHeld(ROLE_HOME)`.
- Otherwise: `PackageManager.resolveActivity(ACTION_MAIN + CATEGORY_HOME, MATCH_DEFAULT_ONLY)` compared with this app’s package name.

`RoleManager` calls sit in methods that run only when `SDK_INT >= 29`, so API 24–28 does not execute them.

## UI

Existing layouts only. No new screens.

- Home: `llDefault` / `rlSetAsDefault` in `fragment_launcher_home.xml`.
- Drawer: `llDefault` / `btnSetNow` in `bottom_sheet_launcher_apps.xml`.

`bind()` sets `llDefault` to `GONE` when this app is the default Home app, and `VISIBLE` otherwise. `onResume` runs the same check.

## Success

The role callback, the resume check, or the pre-29 poll sees `isDefaultHomeApp == true`. The prompt is hidden, polling stops, and `default_home_completed` is stored. The launcher activity stays open.

## Cancel

The role callback or a failed chooser launch sees that this app is still not the default Home app. Polling stops, the prompt stays visible, and the launcher stays usable. A later tap can request the role again.

## API compatibility

`minSdk` is 24. `RoleManager` and `ROLE_HOME` exist from API 29. Below that, the chooser path is used. No newer-only API is called on the older path.

---

## Intentionally not migrated

- `DefaultActivity` and `activity_default.xml`
- `maybeShowDefaultHomePopup` and remote-config / daily-cap popup flags
- `TrackOnce` analytics (`DEFAULT_HOME_APP_SET`, `DEFAULT_HOME_APP_CANCEL`)
- `ScreenFlowNavigation` and `navigateAfterDefaultAppSetup`
- Source `SettingsFragment` default-home row. Compose Settings and the launcher Settings shortcut were left as they were
- Firebase, Remote Config, AdMob, onboarding, Splash, install referrer, call-end, FCM, widgets, quiz, and `MainContainerFragment`

---

## Build

`:app:assembleDebug` — BUILD SUCCESSFUL.

Device verification was not performed.
