---
name: default-home-role-flow
description: Build, modify, debug or port how this launcher app asks to become the default Home app through RoleManager - the "DefaultHome" onboarding screen (Set as default button, Back = Set as default, phone-state permission first), the shared DefaultHomePromptHelper (role request, success / cancel, suppressed-dialog fallback to the system Home settings, polling), what happens when Android restarts the launcher after Allow, the after-default ad, the Home-page "Set as Default" card and Right Swipe popup, the DEFAULT_HOME_APP_SET / DEFAULT_HOME_APP_CANCEL analytics events, and the reminder-notification entry. Use when porting this to another app, or fixing a flow that does not continue, a request that returns instantly, or a missing set / cancel event.
---

# DefaultHome role flow (RoleManager)

Reference implementation: QR Code & Scanner, package `com.qrcode.scanner`, `launcher/`. Source is copied under `assets/`. (`default-home-screen` in this folder is the reference from another project; this skill describes this app's current code.) The system-settings variant of the step is in `default-setting-home-screen`; the swipe popup timing is in `right-swipe-ads`.

## Entry points (all use `DefaultHomePromptHelper`)

| Entry | Class | Starts the request from |
|---|---|---|
| Onboarding step `DefaultHome` | `DefaultActivity` | the **Set as default** button, or Back (Back = the same button) |
| Home page card "Set as Default" (`llDefault`) | `LauncherHomeFragment` | `handleSetAsDefaultClick()` |
| Right Swipe popup | `LauncherHomeFragment` / `DefaultHomePopupHost` | `maybeShowDefaultHomePopup()` |
| Reminder notification | `DefaultActivity` with `EXTRA_FROM_REMINDER` | same button, but afterwards goes to the launcher, not the next onboarding step |

## Onboarding screen (`DefaultActivity`)

1. `onCreate` skips itself when: reminder entry and already default (clears the reminder, opens the launcher); not a reminder and onboarding already completed; the step is already completed (continues the flow). Otherwise the screen is **always shown first**; the role dialog starts only from the button.
2. **Set as default** (`handleSetAsDefaultClick`): already default -> log `DEFAULT_HOME_APP_SET` and continue. Else if `READ_PHONE_STATE` is missing -> ask it (`phonePermissionLauncher`), then continue; the result starts the role request via `postBeginDefaultHomeRoleRequest()` (posted to the decor view so the dialog opens after the permission dialog closed).
3. `beginDefaultHomeRoleRequest()`: sets the shared flags (`setCompletingDefaultAppSetup(true)`, `setDefaultSetupStep(SCREEN_DEFAULT_HOME)`, `setAwaitingDefaultRoleResult(true)`) and calls `defaultHomePromptHelper.handleSetAsDefaultClick()`.
4. The helper reports back through `RoleFlowListener.onRoleFlowFinished` -> `onDefaultRoleFlowFinished()`: guarded (`hasNavigated`, flags still set, window focus when default, `tryClaimAfterDefaultFlow()`), then `continueScreenFlow()`.
5. `continueScreenFlow()`: runs the **after-default ad** (`AdPlacement.loadAfterDefaultAd`, config `After_Default_Ad_Show` / `After_Default_Ad_Type`, preloaded by `preloadAfterDefaultAd` when the screen opens), then `ScreenFlowNavigation.continueAfter(this, SCREEN_DEFAULT_HOME)` (reminder mode: `openMain`).
6. Both outcomes (Allow and Cancel) continue the flow; the difference is only the analytics event and whether the app is now the launcher.

## The helper (`DefaultHomePromptHelper`)

- `registerRoleLauncher()` must run in `onCreate` (it registers the `ActivityResultLauncher`).
- `handleSetAsDefaultClick()`: already default -> `completeSuccess()`. Else `RoleManager.createRequestRoleIntent(ROLE_HOME)` launched through the launcher. If no role intent exists, `AppUtils.openDefaultHomeChooser` (the HOME intent) plus polling every 500 ms for `isDefaultHomeApp`.
- `onRoleResult()`: default now -> `completeSuccess()` (flag screen completed, event `DEFAULT_HOME_APP_SET`); otherwise -> `completeCancelled()` (event `DEFAULT_HOME_APP_CANCEL`). Both call `notifyRoleFlowFinished()`.
- **Suppressed dialog:** after the user declined the system dialog before, Android stops showing it and the request returns at once with no screen. A result under 800 ms from the **button** (not from the automatic popup) is treated as suppressed: `openHomeSettingsFallback()` opens `ACTION_HOME_SETTINGS` (else `ACTION_MANAGE_DEFAULT_APPS_SETTINGS`, else the HOME chooser) plus the guide card, and keeps waiting (polling). If the user comes back without choosing this app, `onResume` ends the wait (`homeSettingsOpened`), so the button works again.
- `onResume()` completes the wait when the app became default while the dialog was open.
- `release()` clears the flags and the poller (call from `onDestroy` / `onDestroyView`).

## Android restarts the launcher after Allow

When the user allows, the system makes the app the Home app and starts it as Home, so the Activity that asked can be recreated and its result callback lost. Two safety nets:

- **Onboarding:** `LauncherHomeActivity.onResume` calls `shouldHandleAfterDefaultSetup()` (flags set **and** app is default) -> `completeAfterDefaultSetup()` -> `tryClaimAfterDefaultFlow()` (one winner between the old Activity and the launcher) -> `AppUtils.navigateAfterDefaultAppSetup()`, which marks the step completed (by `startedBy`: `DefaultHome` or `DefultSettingHome`), logs `DEFAULT_HOME_APP_SET` / `CANCEL`, and continues the screen flow.
- **Popup / Home card (no flags):** `markRoleRequestPending` stores a flag when a request starts; `DefaultHomePromptHelper.reportDefaultSetIfRequested(context)` (called from `LauncherHomeActivity.onResume` and `DefaultHomePopupHost.onHostResume`) sends `DEFAULT_HOME_APP_SET` once if the app is now default. All events use `TrackOnce.trackScreenOnce`: once per install.

## Remote Config

| Key | Meaning |
|---|---|
| `Show_Screen_Flow` contains `DefaultHome` | the onboarding step (default flow: Language, Collection, Permission, DefaultHome, Intro) |
| `PermissionDefaultScreen.PermissionDefault_Ad_Show / _Ad_Type / _Banner_Id / _Native_Id` | ad slot of the Permission and DefaultHome screens |
| `PermissionDefaultScreen.After_Default_Ad_Show / After_Default_Ad_Type` | ad shown after the role result, before the next screen (`inter` or `appopen`) |
| `Default_App_Popup_Show / Default_App_Popup_Count` | the popup after a Right Swipe (see `right-swipe-ads`) |

## Files in `assets/`

| Asset | Goes to |
|---|---|
| `java/DefaultActivity.java` | `launcher/activities/` |
| `java/DefaultHomePromptHelper.java`, `DefaultHomePopupHost.java` | `launcher/helpers/` |
| `layout/activity_default.xml`, `drawable/img_default_home.png` | the DefaultHome screen |
| `snippets/AppUtils.defaultFlow.snippet.java` | flags, claim, `navigateAfterDefaultAppSetup` |
| `snippets/AppUtils.role.snippet.java` | `isDefaultHomeApp`, role intent, HOME chooser |
| `snippets/LauncherHomeActivity.afterDefault.snippet.java` | the launcher hand-over |
| `snippets/ScreenFlowNavigation.continueAfter.snippet.java` | how a step hands over to the next one |
| `snippets/AdPlacement.afterDefaultAd.snippet.java`, `remote_config.json` | after-default ad + example |

## Porting checklist

1. Copy the activity, helper, popup host and layout; register `DefaultActivity` (portrait, normal task).
2. The launcher must be a real Home app: manifest `HOME` + `DEFAULT` intent filter on the launcher activity (RoleManager only offers apps that can be Home).
3. Port the `AppUtils` flag functions and the `LauncherHomeActivity` hand-over exactly; without them the flow stops after Allow.
4. Add `READ_PHONE_STATE` if the screen asks for it first (optional for the role request itself).
5. Test: Allow, Cancel, Cancel twice then tap the Home card (settings fallback), Allow from the Right Swipe popup (the event still fires after the restart), back button on the screen, the reminder entry, a phone without RoleManager support.
