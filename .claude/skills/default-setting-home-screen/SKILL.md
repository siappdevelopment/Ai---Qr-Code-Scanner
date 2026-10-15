---
name: default-setting-home-screen
description: Build, modify, debug or port the "DefultSettingHome" step of the onboarding flow - the full-screen step that has no layout of its own and opens the system "Default home app" settings list (Settings.ACTION_HOME_SETTINGS) instead of the RoleManager dialog, shows the Default Home guide card (animated hand tapping the radio) on top of that list, moves on when the user comes back or picks another launcher, and hands over to the launcher when this app becomes the default. Includes the guide card activity, its layout and drawables, the Remote Config flow name (and aliases), the flags it shares with DefaultHome, and the Xiaomi / background-start limits. Use when porting this step to another app, changing the guide card, or fixing a flow that does not continue after the settings screen.
---

# DefultSettingHome step (system Home settings + guide card)

Reference implementation: QR Code & Scanner, package `com.qrcode.scanner`, `launcher/`. Source is copied under `assets/`.

This is one entry of the Remote Config array `Show_Screen_Flow`. The name is spelled **`DefultSettingHome`** (typo kept on purpose; `DefaultSettingHome` is accepted as an alias by `ScreenFlowConfig`). It is the alternative to the `DefaultHome` step (`default-home-role-flow` skill): `DefaultHome` shows an app screen and asks Android for the role; `DefultSettingHome` skips our screen and sends the user straight to the system list where they choose the Home app.

```
flow reaches DefultSettingHome
  -> DefaultSettingHomeActivity (translucent, no UI)
       already default / no settings screen on the phone -> continue the flow at once
       otherwise: set the shared "completing default setup" flags
                  launch Settings.ACTION_HOME_SETTINGS (ActivityResult)
                  onPause: start DefaultHomeGuideActivity (own task) -> the guide card sits on top of the list
  user taps this app            -> the system makes it default and opens the launcher
                                   -> LauncherHomeActivity.onResume: shouldHandleAfterDefaultSetup -> flow continues
  user picks ANOTHER launcher   -> watcher sees the Home package change -> flow moves on from the background (if Android allows)
  user presses back / returns   -> ActivityResult callback -> continueFlow() -> next screen
```

## Files in `assets/`

| Asset | Goes to | Notes |
|---|---|---|
| `java/DefaultSettingHomeActivity.java` | `launcher/activities/` | the step; `ComponentActivity`, theme `Theme.Translucent.NoTitleBar` |
| `java/DefaultHomeGuideActivity.java` | `launcher/activities/` | the guide card activity |
| `layout/activity_overlay_permission_default_home.xml` | `res/layout/` | the guide card (despite the name it is the **Default Home** guide) |
| `drawable/bg_dialog.xml`, `bg_home_guide_radio_*.xml` | `res/drawable/` | card background, radio dot / ripple |
| `snippets/manifest.activities.xml.snippet` | `AndroidManifest.xml` | both activities |
| `snippets/ScreenFlowConfig.names.snippet.java` | `ScreenFlowConfig` | the `DefultSettingHome` constant + alias normalisation |
| `snippets/ScreenFlowNavigation.defaultSettingHome.snippet.java` | `ScreenFlowNavigation` | skip rule, completion flag, intent for the step |
| `snippets/remote_config.json` | Firebase | example flow |

Also needed from `AppUtils` (see the `default-home-role-flow` skill snippets): `setCompletingDefaultAppSetup`, `setDefaultSetupStep`, `setAwaitingDefaultRoleResult`, `isDefaultHomeApp`, `isDefaultSettingHomeScreenCompleted` / `setDefaultSettingHomeScreenCompleted`, `tryClaimAfterDefaultFlow`, `navigateAfterDefaultAppSetup`.

## The step (`DefaultSettingHomeActivity`)

1. `onCreate` (only when `savedInstanceState == null`): if the app is already default Home, or no settings screen exists (`openHomeSettings()` false) -> `continueFlow()`.
2. `openHomeSettings()`:
   - intent = `Settings.ACTION_HOME_SETTINGS`; if nothing resolves, `ACTION_MANAGE_DEFAULT_APPS_SETTINGS`; if that fails too -> no settings screen.
   - sets the shared flags: `setCompletingDefaultAppSetup(true)`, `setDefaultSetupStep(SCREEN_DEFAULT_SETTING_HOME)`, `setAwaitingDefaultRoleResult(true)`. `LauncherHomeActivity` reads these when the system opens it as the new Home, and hands the flow back to the screen that started it (`startedBy`).
   - launches through an `ActivityResultLauncher`; `guidePending = true`; starts the home watcher.
3. `onPause()`: if `guidePending`, start `DefaultHomeGuideActivity` with `FLAG_ACTIVITY_NEW_TASK`. Starting it here (not together with the Settings launch) puts it on top of Settings, and its own task cannot be removed by a flow change (`CLEAR_TASK`).
4. **Result callback** (settings closed, whatever the user did) -> `stopHomeWatch()`, `continueFlow()`.
5. `continueFlow()` (guarded by `flowContinued`): if the flags are still set, but `LauncherHomeActivity` already handled the continuation (`!isCompletingDefaultAppSetup`) -> just `finish()`; if the app is default and `tryClaimAfterDefaultFlow()` fails -> `finish()`; else clear the flags and call `ScreenFlowNavigation.continueAfter(this, SCREEN_DEFAULT_SETTING_HOME)`. That marks the step completed (`setDefaultSettingHomeScreenCompleted(true)`) and opens the next screen.
6. **Home watcher** (`startHomeWatch`): every 500 ms compares the current default Home package (`resolveActivity(ACTION_MAIN + CATEGORY_HOME)`, "android" = chooser = none) with the one at start. If it changed to another app (not this one), `moveOnFromBackground()` calls `continueFlow()` while the other launcher is in front. After 2 s, if `ProcessAppOpen.isAppInForeground()` is still false (Android blocked the background start), `flowContinued` is reset so the normal return path (the result callback) still moves on, exactly once.
7. `onDestroy` stops the watcher.

## The guide card (`DefaultHomeGuideActivity`)

- Normal translucent Activity (`Theme.ClEnd`, `taskAffinity=""`, `excludeFromRecents`), so it needs **no overlay permission**. The window is `Gravity.BOTTOM`, `MATCH_PARENT x WRAP_CONTENT`, dim 0.2.
- The layout shows the app icon and name (loaded from the app info), the text `default_home_guide_message` ("Tap to set as your default Home app") and an animated hand that presses a radio button (`homeGuideHand`, `homeGuideRipple`, `homeGuideRadioDot`, 900 ms loop, `DecelerateInterpolator`).
- **Touch anywhere closes it** (`dispatchTouchEvent` -> `finish()`, `setFinishOnTouchOutside(true)`, `overlay_root` click). It also closes in `onResume` when the app is default Home.
- **Re-show trick:** Settings opens its list a moment after the guide starts and covers it. In `onPause`, if the guide is not finishing and is not itself the re-shown copy (`EXTRA_RESHOWN`), it starts one copy of itself (`NEW_TASK | MULTIPLE_TASK | NO_ANIMATION`) and finishes, so the copy ends up above the system list. `MULTIPLE_TASK` is needed, else Android only brings the same task to the front.

## Remote Config

Only the flow array: `"Show_Screen_Flow": ["Language","Collection","Permission","DefultSettingHome","Intro"]`. `ScreenFlowNavigation` skips the step when the app is already the default Home or its completion flag is set, and `getDefaultShowScreenFlow()` does not contain it (add it explicitly). Position it after `Permission` so the overlay step (skipped for a default Home app) runs first.

## Limits to know

- On Xiaomi (HyperOS / MIUI) a start from the background is blocked unless "Display pop-up windows while running in the background" is allowed for the app, even as default Home with the overlay permission. The "move on from the background" path may therefore only complete when the user returns.
- The settings screen is not under the app's control; the guide card position is a fixed bottom card, so it can sit over a different part of the list on different phones.

## Porting checklist

1. Copy the activities, layout and drawables; add the strings `default_home_guide_message` (and the other guide strings, translated); register both activities (guide: own task, translucent).
2. Add the flow name + alias to `ScreenFlowConfig`, and the step to `ScreenFlowNavigation` (skip rule, completion flag, intent).
3. Make sure the launcher activity implements the "after default setup" hand-over (see `default-home-role-flow`), or the flow will not continue when the app becomes default.
4. Test: tap this app in the list, pick another launcher, press back without choosing, rotate while the guide is shown, a phone without a Home settings screen.
