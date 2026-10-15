---
name: overlay-permission-flow
description: Build, modify, debug or port how the app asks for the "Display over other apps" (SYSTEM_ALERT_WINDOW) permission - the step inside the onboarding Permission screen (after Phone, Camera and Notification), what happens on Allow and on Don't Allow, the guide card (toggle animation) shown on top of the system settings page, the poller that returns the user to the app after Allow, the Home-screen "Allow display over other apps" card, which phones skip it (default Home app), and Xiaomi notes. Use when porting this permission flow to another app, changing the guide card, or fixing a flow that does not continue after Allow / Don't Allow.
---

# Overlay permission flow

Reference implementation: QR Code & Scanner, package `com.qrcode.scanner`. Source is copied under `assets/`.

Why the app wants it: the Call End screen and the Charging / Install / Uninstall screens are started from a background broadcast; Android blocks that, so the app first shows a tiny overlay window (`CallEndViewManager`) and starts the Activity while an overlay is visible. A default Home app does not need it.

## Where it is asked

Two places:

| Place | When |
|---|---|
| **Onboarding** `PermissionActivity` | After the user taps Allow: Phone state -> Camera -> Notification (Android 13+) -> **Overlay** -> next screen of `Show_Screen_Flow` |
| **Home card** `OverlayPermissionCard` (Compose, `HomeScreen.kt`) | On the QR app Home tab while the permission is missing and the app is **not** the default Home app |

## Onboarding step (`PermissionActivity.requestOverlayPermission`)

```
phone + camera steps done
  overlay already granted OR app is default Home OR SDK < 23  -> finishOverlayStepAndNavigate() (no screen)
  else: Settings.ACTION_MANAGE_OVERLAY_PERMISSION, data package:<app>
        waitingForOverlaySettings = true, overlayGuidePending = true
        overlaySettingsLauncher.launch(intent)
        onPause(): start OverlayPermissionGuideActivity (guide card over the settings page)
```

### Allow vs Don't Allow

- The settings page is a screen **for this app only** with one switch. The user turns it on (**Allow**) or leaves it off and presses back (**Don't Allow**).
- The ActivityResult callback of `overlaySettingsLauncher` fires when that page closes, **in both cases** -> `finishOverlayStepAndNavigate()` -> `completePermissionScreenAndNavigate()`: it logs `OVERLAY_PERMISSION_GRANTED` only if the permission is now granted (`AppUtils.trackOverlayPermissionGrantedOnce`), then `navigateToNextScreen()`. So a denial never blocks onboarding and is not asked again here.
- **Allow while the settings page is still open** (the user can stay on it): when `PermissionActivity` pauses it starts a 400 ms poller (`overlayPermissionMonitorRunnable`). When `hasOverlayPermission` becomes true it relaunches `PermissionActivity` with `CLEAR_TOP | SINGLE_TOP | REORDER_TO_FRONT` (brings the app back over the settings page), then continues the flow.
- Coming back by `onResume` with the permission granted also continues (`completeOverlayGrantAndContinue`).
- State survives rotation / process death through `onSaveInstanceState` (`waitingForOverlaySettings`, `pausedForOverlaySettings`).
- Back on the Permission screen behaves like the Allow button (`handleAllowPermissionClick`); it never returns to the previous onboarding screen, and a second tap is ignored while the flow runs.

## The guide card (`OverlayPermissionGuideActivity`)

- Normal translucent Activity (no overlay permission needed): theme `Theme.ClEnd`, `taskAffinity=""`, `excludeFromRecents`. Window `Gravity.BOTTOM`, `MATCH_PARENT x WRAP_CONTENT`.
- Layout `activity_overlay_permission.xml`: a white card with the app icon, the app name and a looping Lottie toggle (`raw/toggle_button.json`, needs `com.airbnb.android:lottie`). It points at the switch the user must turn on.
- **Any touch closes it** so the user can reach the switch; it closes by itself in `onResume` once the permission is granted.
- **Re-show trick:** Settings covers the guide after a moment, so in `onPause` the guide starts one copy of itself (`EXTRA_RESHOWN`, `NEW_TASK | MULTIPLE_TASK | NO_ANIMATION`) above the settings page and finishes. `OverlayPermissionGuideActivity.show(context)` is the single entry point (used by the Permission screen and the Home card).
- The card is started from `onPause` of the caller, not together with the settings launch, so it ends up on top of Settings.

## Home card (`OverlayPermissionCard`)

A Compose row (icon, title `home_overlay_title`, blue Allow button `home_overlay_allow`). Shown while `!AppUtils.hasOverlayPermission(context) && !AppUtils.isDefaultHomeApp(context)` (a default Home app can start screens from the background, so it does not need the card); the state is re-read on every `ON_RESUME` (coming back from settings), so it disappears after Allow. Allow opens `ACTION_MANAGE_OVERLAY_PERMISSION` for the app and then `OverlayPermissionGuideActivity.show(context)`.

## Files in `assets/`

| Asset | Goes to |
|---|---|
| `java/PermissionActivity.java` | `launcher/activities/`: the whole Permission screen (phone, camera, notification, overlay, poller) |
| `java/OverlayPermissionGuideActivity.java` | `launcher/activities/` |
| `layout/activity_overlay_permission.xml` | the guide card |
| `layout/activity_permission.xml` | the Permission screen (Allow button + ad slot) |
| `raw/toggle_button.json` | Lottie animation |
| `snippets/HomeScreen.OverlayPermissionCard.snippet.kt` | the Home card |
| `snippets/AppUtils.hasOverlayPermission.snippet.java` | `Settings.canDrawOverlays` wrapper |
| `snippets/manifest.activity.xml.snippet`, `strings.xml.snippet`, `remote_config.json` | manifest entry + permission, strings, flow note |

## Manifest and strings

- `<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />`
- Register `OverlayPermissionGuideActivity` (`Theme.ClEnd`, `taskAffinity=""`, `excludeFromRecents`, portrait).
- Strings `home_overlay_title`, `home_overlay_allow` (translate them), app label for the guide card.

## Notes

- The overlay step has no Remote Config key; it lives inside the `Permission` step of `Show_Screen_Flow`.
- Xiaomi (HyperOS / MIUI): even with the overlay permission and as default Home, background starts are blocked unless the separate "Display pop-up windows while running in the background" permission is on. The guide card only handles the standard overlay switch.
- `Settings.canDrawOverlays` can read false on some OEM builds right after the user allowed; the poller / `onResume` re-check handles the delay.

## Porting checklist

1. Copy the two activities, layouts, Lottie file and dependency; add the manifest permission and activity.
2. Keep the order in `PermissionActivity`: phone -> camera -> notification -> overlay, each step posting to the next from its result callback.
3. Port `AppUtils.hasOverlayPermission`, `isDefaultHomeApp`, `trackOverlayPermissionGrantedOnce`, `ScreenFlowNavigation.continueAfter`.
4. Test: Allow, Don't Allow (back), Allow while staying on the page, rotating on the settings page, a default Home app (step skipped), the Home card after Don't Allow (hidden while the app is default Home).
