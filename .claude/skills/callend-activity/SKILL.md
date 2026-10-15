---
name: callend-activity
description: Build, modify, debug or port the Call End screen (ClEndActivity) of this launcher app - the screen that opens right after a phone call ends (Incoming / Outgoing / Missed) with the call time, duration, redial button and four tabs (Content, Message, Reminder, More), how it is launched from the PHONE_STATE broadcast (overlay trick for background-start limits, default Home app shortcut, locked-phone full-screen notification), the Firebase push path (FCM data type call_end or any notification while the phone is locked, with a preloaded full-screen native ad), the English-only rule, and all its ads - the bottom banner / native, the back-press ad (CallEndBackAd inter / app open / native with alternate sequence, per-day, install-day and country gating) and the notification full-screen native with its close button. Use when porting the Call End screen to another app, changing its ads or Remote Config keys, or fixing a screen that does not open, opens twice, shows over a locked phone wrongly, or shows no ad.
---

# Call End screen (`ClEndActivity`)

Reference implementation: QR Code & Scanner, package `com.qrcode.scanner`, `launcher/`. Source is copied under `assets/`. The permissions that make it work in the background (overlay, default Home) are in the `overlay-permission-flow`, `default-home-role-flow` and `default-setting-home-screen` skills.

## Launch flow

```
Phone call ends (PHONE_STATE -> IDLE)
  PhoneCallStateService (BroadcastReceiver) works out Incoming / Outgoing / Missed Call, start / end time
    -> openNewActivity: skip when Cl_End_Screen_Show is false
    -> CallEndLaunchHelper.openAfterCallEnded -> buildCallEndIntent (extras below) -> openCallEndIntent
         phone UNLOCKED : CallEndViewManager.showCallEnd(context, intent)
                            default Home app          -> startActivity directly (allowed from background)
                            otherwise (overlay perm.) -> add a tiny TYPE_APPLICATION_OVERLAY window, 250 ms later
                                                         startActivity, then remove the window
         phone LOCKED   : CallEndPendingLaunch.save(...) + CallEndFullNotificationHelper.notifyCallEndStyle
                          -> full-screen-intent notification "See call Information" (id 1001);
                          the Activity opens when the user taps it / the system shows the full-screen intent

Firebase push (MyFirebaseMessagingService.handleIntent)
  ignore: no POST_NOTIFICATIONS (Android 13+), app in foreground (ProcessAppOpen), phone NOT locked,
          AdPlacement.isCallEndPerformanceAllowed(this, true) false
  ADSNativeFullDisplay.preloadNativeAd(ClEnd_Native_Id)   <- the screen only opens once the ad is LOADED
    -> CallEndLaunchHelper.tryOpenFromFcmData  (data "type" = "call_end": number, start/end time, call_type, duration)
       else openGenericForFirebaseNotification (title/body of any notification; CallType = "Notification")
       -> phone locked -> full-screen notification (CallEndFullNotificationHelper.notifyGenericFullscreen)
```

Intent extras (`CallEndLaunchHelper.buildCallEndIntent`): `mobile_number`, `StartTime`, `EndTime`, `CallType`, `formattedDuration`, `is_from_fcm`, `timeType` ("08:15 PM  Incoming"), `duration`, `EXTRA_SKIP_OVERLAY_PROMPT`, `EXTRA_ALLOW_SHOW_ON_LOCKSCREEN=false`, flags `NEW_TASK | CLEAR_TOP | SINGLE_TOP`. The locked-phone case adds `EXTRA_LOCKED_CALL_END_NOTIFICATION`.

`CallEndReceiver` is an empty `CallScreeningService` (permission `BIND_SCREENING_SERVICE`); it only makes the app a registered call app so the phone-state receiver keeps being delivered. Optional when porting.

## The screen (`ClEndActivity`)

- Manifest: `singleTask`, `taskAffinity=""`, `excludeFromRecents`, portrait, theme `Theme.ClEnd` (translucent, no dim), `showOnLockScreen=false`, `turnScreenOn=false`.
- **English only**: `attachBaseContext` + `applyOverrideConfiguration` force `Locale.ENGLISH`, so the whole screen (and its fragments) ignores the app language. `localizeCallType` only swaps the trailing call type word using `cl_end_call_*` strings.
- Static `visible` (set in `onStart` / `onStop`, read with `ClEndActivity.isVisible()`): the Charging / Install screens (`EventPromptLauncher`) do not open while Call End is on screen.
- `findIDs` -> `setupFullAdContainer`: cancels the call-end notification; if the launch is a **notification flow** (`AdPlacement.isNotificationFullAdFlow`: `is_from_fcm`, or CallType `Notification` / `call_end`) and `ADSNativeFullDisplay.AdmobNativeAd` holds the preloaded ad, the screen shows only that full-screen native (`notification_full_ad.xml`, `rlFullAd`); otherwise `showCallStateView` shows the normal call info and the bottom ad.
- `onResume`: cancels the notification, clears `CallEndPendingLaunch` when the phone is unlocked.
- `ivCall` opens the dialer (`ACTION_DIAL`) and then `finishAfterLaunch`. The fragments call `ClEndActivity.finishAfterLaunch(...)` after they start another app (message, mail, calendar, web, contact), so the screen does not stay behind.
- Tabs: `ClEndPagerAdapter` -> `ClContentFragment` (scan / history / widgets shortcuts, app specific - replace), `ClMessageFragment` (quick SMS replies), `ClReminderFragment` (+ `ClReminderAdapter`, `ReminderModel`, `ReminderReceiver` alarm that fires the reminder notification), `ClMoreOptionFragment` (add contact, send message, mail, calendar, web). Tab icons `ic_*_cl_end` in `item_tab_icon_cl_end.xml`, tab background `bg_cl_end_*`. The pager is `ViewPager2` + `TabLayoutMediator`, keyboard hidden on page change.

## Ads on this screen

| Ad | Where | Gate |
|---|---|---|
| **Bottom ad** | `showAd()`: banner (`AdPlacement.loadAdaptiveBannerAd`) or large native (`loadNativeAd(..., "large")`) in `rlAdView` | `ClEnd_Ad_Show`, network, a banner / native id. `ClEnd_Ad_Type=banner` prefers banner (falls back to native when no banner id); any other value prefers native. When no ad shows, the navigation bar is hidden (R+). |
| **Back ad** | `getBack()` on back press (OnBackPressedCallback) -> `CallEndBackAd.fullScreenAdShow` -> then `finish()` after 500 ms. On show: `AdPlacement.setClEndLastShowTime(now)` | `AdPlacement.canShowClEndBackAdOnPress` (below) |
| **Notification full native** | `rlFullAd` (replaces the call view) | only the notification flow, preloaded by the FCM service; close button `ivClose` only when `Notification_Close_Button_Show` |

`CallEndBackAd.loadAd(activity)` runs in `initialClicks` (when online and back ad is on / notification flow) and preloads every configured format (inter, app open, native). `fullScreenAdShow` chooses the format (`resolveShowType`): fixed `inter` / `appopen` / `native` from `ClEnd_Back_Ad_Type`, or `alternate`: follows `ClEnd_Back_Ad_Sequence` = pairs `[format, times]` using a cursor (`cl_end_ad_preferences/back_ad_sequence_cursor`, reset when the sequence changes, advanced after each shown ad). If the chosen ad is not ready it shows a loading dialog and waits up to 8 s (`READY_WAIT_MS`), polling every 250 ms, or ends early when that load already failed; on failure it just continues to `finish()`.

### `canShowClEndBackAdOnPress(context, notificationFlow, fullAdVisible)`

1. Network on **and** the per-day interval passed: `24h / ClEnd_Back_Ad_Show_Per_Day` since `cl_end_ad_preferences/last_show_time` (per-day `<= 0` = never).
2. **Notification flow**: needs the full ad visible, `Notification_Back_Ad_Show`, and `meetsNotificationInstallDayRequirements`.
3. **Normal flow**: `ClEnd_Back_Ad_Show`, `ClEnd_Back_Ad_Show_After_Day > 0`, days since install >= that value, and the device country is allowed (`ClEnd_Back_Ad_Show_Country`, empty = all; country from the SIM / network, or from an IP lookup when `ClEnd_Back_Ad_Country_IP`).

### FCM / notification gating (`isCallEndPerformanceAllowed(context, isFcmTrigger)`)

`ClEnd_Ad_Show` must be on, then by granted permissions (`notification`, `call` = READ_PHONE_STATE, `overlay` = overlay **or** default Home):

| Granted | Needs |
|---|---|
| notification + call + overlay | `All_Allow_Permission_Show_Notification` (FCM) and `Notification_Call_Overlay_Install_Days` + `Notification_Call_Overlay_Country` |
| notification + call | `Notification_Call_Install_Days` + `Notification_Call_Country` |
| notification only | FCM only: `Notification_Install_Days` + `Notification_Country` |
| none | not allowed |

Install days = days since `appInstallDate/install_date` (+1); empty country list = all countries.

## Remote Config (`Screen.ClEndScreen`, parsed in `RemoteConfigValues.apply`)

Root key `Cl_End_Screen_Show` switches the whole screen. Keys inside `ClEndScreen`: `ClEnd_Ad_Show`, `ClEnd_Ad_Type`, `ClEnd_Banner_Id`, `ClEnd_Native_Id`, `ClEnd_Back_Ad_Show`, `ClEnd_Back_Ad_Type`, `ClEnd_Back_Ad_Sequence`, `ClEnd_Back_Ad_Show_After_Day`, `ClEnd_Back_Ad_Show_Per_Day`, `ClEnd_Back_Ad_Interstitial_Id` (falls back to the app inter id), `ClEnd_Back_Ad_Native_Id` (the back native uses `ClEnd_Native_Id` first), `ClEnd_Back_Ad_Country_IP`, `ClEnd_Back_Ad_Show_Country`, `Notification_*` (see above), `Notification_Back_Ad_Show`, `Notification_Close_Button_Show`. The values are saved (`saveClEndConfig`) so the receiver / FCM service can read them when the app process starts without a fresh fetch (`AdPlacement.ensureClEndConfig` -> `RemoteConfigValues.ensureLoaded`). Example: `assets/snippets/remote_config.json`.

## Files in `assets/`

| Asset | Goes to |
|---|---|
| `java/ClEndActivity.java` | `launcher/activities/` |
| `java/ClEndPagerAdapter.java`, `ClReminderAdapter.java` | `launcher/adapters/` |
| `java/ClContentFragment|ClMessageFragment|ClReminderFragment|ClMoreOptionFragment.java` | `launcher/fragments/` |
| `java/ReminderModel.java` | `launcher/models/` |
| `java/ReminderReceiver.java`, `CallEndReceiver.java` | `launcher/receivers/` |
| `java/PhoneCallStateService.java`, `MyFirebaseMessagingService.java` | `launcher/services/` (the phone-state receiver also routes charger events to `EventPromptLauncher`; delete those branches if you do not port event screens) |
| `java/CallEndLaunchHelper|CallEndPendingLaunch|CallEndFullNotificationHelper|CallEndViewManager|CallEndBackAd|ADSNativeFullDisplay.java` | `launcher/common/` |
| `layout/*.xml` | `activity_cl_end` (tabs + ViewPager2 + bottom ad + full ad), `fragment_cl_*`, `adapter_cl_reminder`, `item_tab_icon_cl_end`, `call_end_view` (the 1-pixel overlay view), `notification_full_ad` |
| `drawable/*` | tab icons and backgrounds |
| `snippets/AdPlacement.callEnd.snippet.java` | config getters, gating, install days, country, last-show time, permission checks |
| `snippets/RemoteConfigValues.clEndParse.snippet.java`, `RemoteConfigValues.callEnd.snippet.java` | parsing of `ClEndScreen` + sequence parser |
| `snippets/manifest.callEnd.xml.snippet`, `strings.cl_end.xml.snippet`, `themes.ClEnd.xml.snippet`, `remote_config.json` | manifest, strings (`cl_end_*`), theme, example |

Also needed from the app (port or replace): `AdPlacement` (`loadAdaptiveBannerAd`, `loadNativeAd`, `populateNativeAdView`, `initializeIfConfigured`, `isNetworkAvailable`), `AdTheme`, `AppUtils.isDefaultHomeApp / hasOverlayPermission`, `ProcessAppOpen.isAppInForeground`, `EventPromptLauncher` (optional), `SettingsRepositoryKt.readAppNightMode` (night mode), layouts `native_full_ad_layout`, shimmer layouts, dependencies `shimmer`, `viewpager2`, `material` (TabLayout), Firebase Messaging, play-services-ads.

## Porting checklist

1. Copy classes (fix packages / `R` import), layouts, drawables; merge the manifest snippet (activity, receivers, service, permissions), strings and theme.
2. Port the `AdPlacement` call-end section and the `RemoteConfigValues` fields + parse + save/load; keep the key names so one Remote Config JSON works for both apps.
3. Replace `ClContentFragment` with your own shortcuts; keep the other tabs or remove them with their strings / layouts.
4. Ask the permissions: `READ_PHONE_STATE`, `POST_NOTIFICATIONS`, overlay (see `overlay-permission-flow`); `USE_FULL_SCREEN_INTENT` on Android 14+ is user-controlled.
5. Test: all three call types, phone unlocked with overlay permission / as default Home / with neither (nothing should open), phone locked (notification, tap -> screen), back ad with each format and the alternate sequence, screen with ads off, FCM `call_end` data message and a plain notification while locked, English UI when the app language is Gujarati / Hindi, no second Call End screen when a second broadcast arrives.
6. Known limits: MIUI / HyperOS needs "Display pop-up windows while running in the background" for background starts; Android 12+ phone-state broadcasts need `READ_PHONE_STATE` and the app must not be force-stopped.
