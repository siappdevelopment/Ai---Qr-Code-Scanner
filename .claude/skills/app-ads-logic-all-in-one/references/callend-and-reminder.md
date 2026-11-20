# Call End screen (ClEndActivity) and "Make this app your default launcher" reminder

Verified against the code of package `com.qrcode.scanner` (Java root:
`app/src/main/java/com/qrcode/scanner/launcher/`, abbreviated `L/` below). Where the old skills
(`callend-activity`, `callend-ads`, `default-launcher-reminder`, `default-home-role-flow`) disagree with the code,
the code wins. Anything not proven by reading code is marked **unverified**. Nothing here was run on a device.

---------------------------------------------------------------------------------------------------

## PART A. CALL END SCREEN

### A1. Files and roles

| File | Role |
|---|---|
| `L/activities/ClEndActivity.java` | The screen (header, 4 tabs, bottom ad, notification full-screen ad, back-press ad) |
| `L/services/PhoneCallStateService.java` | `BroadcastReceiver` (despite the name) for PHONE_STATE; decides Incoming / Outgoing / Missed |
| `L/common/CallEndLaunchHelper.java` | Builds the intent, picks "start now" vs "locked: full-screen notification", FCM entry points |
| `L/common/CallEndViewManager.java` | Overlay trick (1x1 overlay window, then `startActivity` after 250 ms) |
| `L/common/CallEndFullNotificationHelper.java` | Full-screen-intent notification for a locked phone |
| `L/common/CallEndPendingLaunch.java` | Prefs `call_end_pending_launch` + notification id 1001 |
| `L/common/ADSNativeFullDisplay.java` | Static holder `AdmobNativeAd` for the preloaded full-screen native (FCM path) |
| `L/common/CallEndBackAd.java` | Back-press ad engine (inter / app open / native, alternate sequence) |
| `L/services/MyFirebaseMessagingService.java` | FCM entry: preload full native, then open Call End |
| `L/common/AdPlacement.java` 2726-3070 | Config getters, gates, country/IP/install-day helpers |
| `L/remote/RemoteConfigValues.java` | ClEnd keys (149-285), persistence (`saveClEndConfig` 779, `ensureLoaded` 287) |
| `L/common/IPAddressHelper.java` | IP country lookup |
| `L/receivers/CallEndReceiver.java` | NOT a call-end receiver, see A12 |
| `L/adapters/ClEndPagerAdapter.java` | ViewPager2 adapter: 0 Content, 1 Message, 2 Reminder, 3 More |
| `L/fragments/ClContentFragment, ClMessageFragment, ClReminderFragment, ClMoreOptionFragment` | Tab pages |
| `L/adapters/ClReminderAdapter.java` | Reminder list rows (`adapter_cl_reminder.xml`) |
| Layouts | `activity_cl_end.xml`, `item_tab_icon_cl_end.xml`, `adapter_cl_reminder.xml`, `notification_full_ad.xml`, `native_full_ad_layout.xml`, `call_end_view.xml` (overlay) |

Manifest (`AndroidManifest.xml`):
- `ClEndActivity` 267-277: `exported="true"`, `launchMode="singleTask"`, `taskAffinity=""`, `excludeFromRecents`,
  portrait, `Theme.ClEnd` (themes.xml:102, parent `Theme.LauncherSettings`), `adjustPan`,
  `showOnLockScreen="false"`, `turnScreenOn="false"`.
- `PhoneCallStateService` 279-295: exported receiver, priority 998, actions `PHONE_STATE`, `NEW_OUTGOING_CALL`,
  `ACTION_POWER_CONNECTED/DISCONNECTED`, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `QUICKBOOT_POWERON`, autostart extras.
  Boot / package-replaced / autostart actions fall through `onReceive` and do nothing (line 46 returns).
- `MyFirebaseMessagingService` 321-327 (`MESSAGING_EVENT`).
- Permissions used: `READ_PHONE_STATE`, `POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW`, `USE_FULL_SCREEN_INTENT`,
  `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `VIBRATE`, `WAKE_LOCK`.
  Note: no `READ_CALL_LOG`, so on API 28+ the phone number in PHONE_STATE is normally empty (and is never shown anyway).

### A2. How the screen starts (4 entry paths)

#### Path 1: normal phone call (PHONE_STATE broadcast)
`PhoneCallStateService.onReceive` (:27) with `ACTION_PHONE_STATE_CHANGED` (:46). State machine (all `static`, so it
dies with the process):
- RINGING (:93): `callStartTime=now`, `isRingingCall=true`, `isShowScreenCall=false`.
- OFFHOOK (:84): `callStartTime=now`; if `!isRingingCall` -> `isOutgoingCall=true`, else `isIncomingCallMain=true`
  (+ `isIncomingCallSend=true`); `isShowScreenCall=false`.
- IDLE (:71): if `!isIncomingCallMain && isRingingCall` -> `isMissCalls=true`. Type = `Incoming` / `Missed Call` /
  `Outgoing` (:77). If `!isShowScreenCall` -> `openNewActivity(...)` and set `isShowScreenCall=true` (duplicate
  suppression: a second IDLE broadcast without a new RINGING/OFFHOOK is ignored). Otherwise only clears `outgoingNumber`.
- `openNewActivity` (:102): `AdPlacement.ensureClEndConfig` (loads persisted config in a cold process), then ONLY gate:
  `Cl_End_Screen_Show` (`getClEndScreenShow`, :104). If false -> `resetCallFlags()` and nothing opens.
  **The install-day, country and permission-tier gate (`isCallEndPerformanceAllowed`) is NOT applied to this path**,
  and `ClEnd_Ad_Show` is not checked either (the screen opens even with no ad).
- `resetCallFlags()` (:113) does NOT reset `isRingingCall` (see bug B-1).
- Duration = `end - callStartTime`, formatted `mm:ss` by `CallEndLaunchHelper.formatDuration` (:32; minutes can exceed 59).

Then `CallEndLaunchHelper.openAfterCallEnded` (:57) -> `buildCallEndIntent` (:39) -> `openCallEndIntent` (:146):
- Keyguard NOT locked -> `CallEndViewManager.showCallEnd` (:153), then `CallEndPendingLaunch.clear`.
  Any exception is swallowed (screen silently not shown).
- Keyguard locked (:158) -> adds `EXTRA_LOCKED_CALL_END_NOTIFICATION=true`, saves `CallEndPendingLaunch`, posts the
  full-screen notification (Path 3).

Intent extras (`buildCallEndIntent`): `mobile_number`, `StartTime`, `EndTime`, `CallType`, `formattedDuration`,
`is_from_fcm`, `skip_overlay_prompt=true`, `allow_show_on_lockscreen=false`, `timeType`
(`"hh:mm A"` of the END time uppercased + two spaces + call type), `duration`. Flags `NEW_TASK | CLEAR_TOP | SINGLE_TOP`.
**ClEndActivity only reads `timeType`, `duration`, `CallType` and `is_from_fcm`.** `mobile_number`, `StartTime`,
`EndTime`, `skip_overlay_prompt`, `allow_show_on_lockscreen`, `locked_call_end_notification` are written but never read.

#### Path 2: overlay trick / default Home shortcut (`CallEndViewManager.showCallEnd`, :18)
- If `AppUtils.isDefaultHomeApp(context)` -> plain `context.startActivity(intent)` (default Home may start from background).
- Otherwise inflates `call_end_view` into a `WindowManager` overlay (`TYPE_APPLICATION_OVERLAY` on O+,
  FLAG_NOT_FOCUSABLE | LAYOUT_IN_SCREEN | SHOW_WHEN_LOCKED | TURN_SCREEN_ON | KEEP_SCREEN_ON), and after 250 ms calls
  `startActivity(intent)` then removes the overlay. The visible overlay gives the process a foreground-ish state so
  Android 10+ allows the background activity start. **Needs the "Display over other apps" permission**; without it
  `addView` throws, `openCallEndIntent` swallows it and the screen never appears.

#### Path 3: locked phone -> full-screen-intent notification
`CallEndFullNotificationHelper.notifyCallEndStyle` (:50): channel `call_end_full_screen` (IMPORTANCE_HIGH, public,
lights + vibration), id `CallEndPendingLaunch.NOTIFICATION_ID = 1001`, title "See call Information" (hard-coded
English), `CATEGORY_CALL`, `PRIORITY_MAX`, content intent + `setFullScreenIntent(pi, canUseFullScreenIntent())`
(API 34 `canUseFullScreenIntent` check, :24). PendingIntent flags `UPDATE_CURRENT | IMMUTABLE`, request codes
`id*10+3` / `id*10+2`. `ClEndActivity.onCreate` -> `setupFullAdContainer` (:157) and `onResume` (:204) cancel notification 1001;
`onResume` also clears `CallEndPendingLaunch` prefs once the keyguard is gone.
`CallEndPendingLaunch` stores number/start/end/type/duration, but **nothing in the code reads it back** (write-only;
no relaunch after unlock - unverified that this is intended).
Whether the activity actually draws over the keyguard is **unverified**: the manifest uses `showOnLockScreen="false"`
and the activity never calls `setShowWhenLocked`; the lock-screen display relies on the system full-screen-intent behaviour.

#### Path 4: Firebase push while the phone is locked (preloaded full-screen native)
`MyFirebaseMessagingService.handleIntent` (:29) overrides the raw intent handler (`onMessageReceived` is empty):
1. `POST_NOTIFICATIONS` must be granted (API 33+), else return (:30).
2. Builds a `RemoteMessage`. For ANY FCM message (data or notification payload): return if
   `ProcessAppOpen.isAppInForeground()` (:42); return if the keyguard is NOT locked (:47) (so unlocked phones show nothing);
   `ensureClEndConfig`; return unless `AdPlacement.isCallEndPerformanceAllowed(this, true)` (:52).
3. `ADSNativeFullDisplay.preloadNativeAd(this, getClEndNativeId(), cb)` (:60). Only when the native LOADS
   (`onAdLoaded`) does `launchCallEnd` run; `onAdFailed` does nothing (no notification at all, and the default FCM
   notification is also suppressed because `super.handleIntent` is not called for messages that have extras).
4. `launchCallEnd` (:76): `CallEndLaunchHelper.tryOpenFromFcmData(data)` if `data.type == "call_end"` (case-insens.),
   else `openGenericForFirebaseNotification(title, body)` (notification payload), else generic with app name.
   - `tryOpenFromFcmData` (:64): if `ClEnd_Ad_Show` is false returns `true` (message swallowed, nothing shown).
     Reads `mobile_number`/`number`, `start_time`, `end_time` (epoch ms), `call_type` (default "Incoming"),
     `formatted_duration` (else computed). `is_from_fcm=true`. Because step 2 required a locked keyguard, in practice it
     always takes the locked branch (:97): save pending + `notifyCallEndStyle`. The `!locked` branch (:90) is unreachable
     from this service (keyguard race only).
   - `openGenericForFirebaseNotification` (:104): call type string `"Notification"`, duration `00:00`; locked branch posts
     `notifyGenericFullscreen` (channel `fcm_fullscreen_high` "Push notifications", the push title/body, default sound,
     small icon `ic_launcher`).
5. Tap / full-screen launch of the notification starts `ClEndActivity` with `is_from_fcm=true` ->
   `AdPlacement.isNotificationFullAdFlow` (AdPlacement:3046: `is_from_fcm` OR callType `Notification`/`call_end`) true ->
   `tryShowNotificationFullAd` (ClEndActivity:169).

Example data push (custom payload, no `notification` block so Android hands it to `handleIntent`):
```json
{ "to": "<token>", "data": { "type": "call_end", "mobile_number": "+91999999999",
  "start_time": "1760500000000", "end_time": "1760500060000", "call_type": "Missed Call",
  "formatted_duration": "01:00" } }
```

### A3. Gate chain (what must be true, per path)

| Gate | Phone call path | FCM path | Where |
|---|---|---|---|
| `Cl_End_Screen_Show` (root) | required | not checked | PhoneCallStateService:104 |
| `ClEnd_Ad_Show` | not checked | required (performance + `tryOpenFromFcmData`:72) | AdPlacement:3022 |
| `READ_PHONE_STATE` | needed to receive broadcast | not needed | system |
| `POST_NOTIFICATIONS` | needed only for locked notification | required (handleIntent:30, tier gate) | |
| Overlay permission or default Home | required unless default Home (unlocked) | not needed (locked: full-screen notification) | CallEndViewManager |
| Install days | not applied | tier days (below) | AdPlacement:3053 `checkDaysAndCountry` |
| Country | not applied | tier country list | |
| App in foreground | n/a | blocks FCM path | MyFirebaseMessagingService:42 |
| Keyguard locked | decides notification vs direct | MUST be locked | |
| Duplicate | `isShowScreenCall` | fixed notification id 1001 replaces | |
| English only | forced in activity | forced | A4 |

`AdPlacement.isCallEndPerformanceAllowed(ctx, isFcmTrigger)` (:3021) - tiers by granted permissions
(`isOverlayGranted` counts the default Home app as granted, :3016):
1. `ClEnd_Ad_Show` false -> false.
2. notification + call-state + overlay granted -> needs `All_Allow_Permission_Show_Notification=true` (for FCM), then
   `Notification_Call_Overlay_Install_Days` and `Notification_Call_Overlay_Country`.
3. notification + call-state -> `Notification_Call_Install_Days` / `Notification_Call_Country`.
4. notification only -> only for FCM; `Notification_Install_Days` / `Notification_Country`.
5. no notification permission -> false.
`checkDaysAndCountry` (:3053): non-FCM always true; `requiredDays>0 && installDays<requiredDays` -> false; empty country list
-> true; else device country must equal (ignore case) one list entry. The `isFcmTrigger=false` branch is **never called anywhere**
(grep: only `MyFirebaseMessagingService:52` calls it, with `true`) - dead code, so the phone-call path really has no tier gate.

Install days: `getDaysSinceInstall` (:2917) reads prefs `appInstallDate`/`install_date` (`yyyy-MM-dd`); empty/invalid -> `1`;
result = whole days elapsed + 1 (install day = day 1).

Country gate - `getDeviceCountry` (:2961):
- If `ClEnd_Back_Ad_Country_IP` is true AND a cached IP country exists (prefs `ipCountryName`/`ip_country_name`) -> IP country,
  UPPERCASED English name (from ip-api `country` field, e.g. "INDIA").
- Else telephony `getNetworkCountryIso()` -> `getSimCountryIso()` -> locale country, converted to the English display name,
  UPPERCASED. All comparisons are `equalsIgnoreCase`, so config lists must contain **full English country names**
  ("India", "United States"), not ISO codes. `RemoteConfigValues.parseCountryArray` lowercases entries.
- IP lookup: `requestCallEndIpCountryIfNeeded` (:2836) runs only when `ClEnd_Back_Ad_Country_IP` is true, not already started and nothing
  cached. Called from `MainActivity.onCreate` (MainActivity.kt:30) and at the end of every `RemoteConfigValues.apply` (:284).
  `IPAddressHelper.getCountryName` -> GET `pro.ip-api.com/json` (10 s timeouts, single-thread executor, result posted to main).
  The result is cached forever (never refreshed); a failure resets the "started" flag so the next call retries.
  The ip-api key is hard-coded in `IPAddressHelper:19` (secret in source).

### A4. English-only rule
`ClEndActivity` overrides `attachBaseContext` (:84) with a context configured to `Locale.ENGLISH` and
`applyOverrideConfiguration` (:89) forcing English, so every string in the screen and its fragments is English whatever the
app language. `localizeCallType` (:113) tries to swap the English call type for `cl_end_call_incoming/outgoing/missed`, but
because resources are English already this is effectively a no-op; `Notification` is never localized. Night mode follows
`SettingsRepositoryKt.readAppNightMode` (:105).

### A5. Screen layout and call info (`activity_cl_end.xml`)
- Root `LinearLayout`, `fitsSystemWindows`, bg `surface_primary`. Two siblings: `llCallView` (normal screen) and `rlFullAd`
  (notification full-screen ad, `gone` by default).
- Header: user icon, static title string `cl_end_private_number` ("Private Number" - the real number is NOT displayed),
  `tvTimeType` = `"HH:MM AM  Incoming|Outgoing|Missed Call"` (time = call END time), `tvDuration` = `mm:ss`,
  green round `ivCall` button.
- `ivCall` (:251): `ACTION_DIAL` with no number (just opens the dialer), then `finishAfterLaunch`. So "redial" is not a real redial.
- `TabLayout tlOption` (fixed, 4 icon-only tabs, custom view `item_tab_icon_cl_end`) + `ViewPager2 vpData`
  (`saveEnabled=false`, offscreen limit 4; `onPageSelected` hides the keyboard).
- Bottom ad block `rlAdView` containing `rlBannerAdView` (shimmer `slBannerShimmer`, container `llBannerAd`) and `rlNativeAdView`
  (250sdp high, shimmer + `flNativeAd`).
- `rlFullAd` = `fullAdContainer` + top-right `ivClose` (24sdp, `gone` by default).

### A6. The four tabs
| # | Tab | Behaviour (all actions call `ClEndActivity.finishAfterLaunch` so the screen closes after launching) |
|---|---|---|
| 0 | Content (`ClContentFragment`) | Scan -> `ComposeScanRequest.openScan`; History -> `HistoryActivity`; App Widgets -> `AppWidgetsActivity` (QR-app specific) |
| 1 | Message (`ClMessageFragment`) | 3 canned replies (`cl_end_msg_busy/callback/meeting`) + custom EditText; send = `ACTION_SENDTO smsto:` with `sms_body` and NO recipient; empty custom text -> toast `cl_end_toast_enter_message`; no SMS app -> toast `cl_end_toast_no_sms` (screen not closed). Keyboard hidden in `onPause` |
| 2 | Reminder (`ClReminderFragment` + `ClReminderAdapter`) | Create reminder: text, date+time pickers, 9 colour dots, Yes/No; saved to prefs `reminder_preferences` key `reminder_list` (Gson) and scheduled by `ReminderAlarmHelper.scheduleReminder` (AlarmManager exact-while-idle, falls back to inexact when exact alarms are not allowed). List sorted newest-first, delete icon cancels the alarm. Hard-coded English toasts "Please Select Date & Time" / "Please Select a Future Date & Time". Opening the tab calls `rescheduleAll` |
| 3 | More (`ClMoreOptionFragment`) | Add to contact (`ACTION_INSERT`), Send message (`smsto:`), Send mail (`mailto:`), Calendar (Google Calendar or `CATEGORY_APP_CALENDAR`), Web (launches `com.google.android.googlequicksearchbox`; if the Google app is missing nothing happens and the screen stays) |

### A7. ADS on the Call End screen (overview)

| Ad | Where | Keys | Quiz fallback |
|---|---|---|---|
| Bottom banner or native | normal call view | `ClEnd_Ad_Show`, `ClEnd_Ad_Type`, `ClEnd_Banner_Id`, `ClEnd_Native_Id` | none (`googleOnly=true`) |
| Back-press ad (inter / app open / native) | `getBack()` | `ClEnd_Back_Ad_*`, `Notification_Back_Ad_Show`, tier keys | none (types are Google only; `quiz_native` is mapped to Google native) |
| Full-screen native | notification flow only (replaces the whole screen) | preloaded from `ClEnd_Native_Id`; `Notification_Close_Button_Show` | none |

So the "Quiz fallback" described in older skills does NOT apply to the Call End screen in this code: `ClEndActivity.showAd` calls
`loadAdaptiveBannerAd(..., googleOnly=true, largeInline=true)` (:298) and `loadNativeAd(..., "large", googleOnly=true)` (:302), and
`CallEndBackAd` never calls `QuizAds`. `Google_Ad_Failed_Show_Quiz` has no effect here.

#### A7.1 Bottom ad (`ClEndActivity.showAd`, :265)
Called only from `showCallStateView` (:187), i.e. NOT in the full-screen notification mode.
```
bannerId = trim(ClEnd_Banner_Id); nativeId = trim(ClEnd_Native_Id)
preferBanner = ClEnd_Ad_Type equalsIgnoreCase "banner"
showBanner = preferBanner ? bannerId != "" : (nativeId == "" && bannerId != "")
showNative = !showBanner && nativeId != ""
```
- No network, or `ClEnd_Ad_Show=false`, or neither id usable -> `rlAdView` gone. (On API 30+ the navigation bar is hidden
  in this case and also for the banner case, `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`; not for native.)
- Type `banner` with an empty banner id but a native id falls back to native; any other type string behaves as "native preferred".
- Banner = large inline adaptive banner (`AdPlacement.loadAdaptiveBannerAd`, :1027), native = `"large"` template in a 250sdp slot.
  Both use shimmer until loaded, hide themselves on failure. Banner/Native load analytics: `AnalyticsEvents.BANNER_LOAD/FAIL`, `NATIVE_LOAD/FAIL`.
- `AdPlacement.initializeIfConfigured` starts the Mobile Ads SDK if needed (cold start from a receiver).

#### A7.2 Notification full-screen native (`tryShowNotificationFullAd`, :169)
- Condition: `isNotificationFullAdFlow(intent, callType)` AND `ADSNativeFullDisplay.AdmobNativeAd != null`.
- Takes the preloaded ad (field nulled so it is single use), inflates `notification_full_ad` (`NativeAdView` with big media,
  icon, headline, body, Install button), `AdPlacement.populateNativeAdView(ad, view, "large")`, shows `rlFullAd` and hides `llCallView`.
- `ivClose` visible only if `Notification_Close_Button_Show` is true; click = `finish()` (no back ad on close). With the close button
  hidden the user can only leave by Back (which may show the back ad) or Home.
- If no preloaded ad (e.g. process killed or holder already consumed) -> falls back to the normal call view with the bottom ad.
- `onDestroy` destroys the native ad and clears the container.

#### A7.3 Back-press ad
`getBack()` (ClEndActivity:365) is the Back handler (`OnBackPressedCallback`, :257):
```
if (AdPlacement.canShowClEndBackAdOnPress(this, isNotificationCallEndFlow(), isFullAdVisible())) {
    CallEndBackAd.fullScreenAdShow(this, shown -> { if (shown) setClEndLastShowTime(now); postDelayed(finish, 500); });
} else finish();
```
Preload: `initialClicks` (:240) calls `CallEndBackAd.loadAd(this)` if online AND (`ClEnd_Back_Ad_Show` OR notification flow). Preloading
ignores day / country / interval gates, so ads may load that are never shown.

`canShowClEndBackAdOnPress` (AdPlacement:2863):
1. Online, and `isClEndBackAdIntervalElapsed`: `ClEnd_Back_Ad_Show_Per_Day > 0` and
   `now - last_show_time >= 24h / PerDay` (prefs `cl_end_ad_preferences` key `last_show_time`, written only when an ad was actually shown/dismissed).
   So "per day" is a minimum spacing, not a per-day counter. `PerDay=0` disables everything (even notification flow).
2. Notification flow: needs the full-screen ad visible (`fullAdVisible`), `Notification_Back_Ad_Show=true` and
   `meetsNotificationInstallDayRequirements` (same permission tiers as A3 but evaluated with isFcmTrigger=true and
   `All_Allow_Permission_Show_Notification` required for the 3-permission tier). `ClEnd_Back_Ad_Show`, `Show_After_Day` and
   `Show_Country` are NOT consulted in notification flow. If the fallback normal view is shown (no preloaded native) the back ad is skipped.
3. Normal flow: `ClEnd_Back_Ad_Show=true`, `ClEnd_Back_Ad_Show_After_Day > 0` (0 means never show) and `daysSinceInstall >= After_Day`,
   and `isCountryAllowedForCallEnd` (:2937): empty `ClEnd_Back_Ad_Show_Country` = all countries, otherwise device country
   (IP country when `ClEnd_Back_Ad_Country_IP` is on and cached) must match.

`CallEndBackAd.fullScreenAdShow` (:70) -> `dropUnconfiguredAds`, `resolveShowType`:
- `ClEnd_Back_Ad_Type` normalized (`normalizeAdType` :706): `inter` (also `interstitial`, `google_inter`, `google_interstitial`),
  `appopen` (`app_open`, `google_appopen`, `google_app_open`), `native` (`google_native`, `quiz_native`), `alternate`.
  `-` is treated as `_`, case-insensitive. Anything else resolves to "" -> `failShow` -> callback(false) -> screen just closes.
- `alternate`: sequence from `ClEnd_Back_Ad_Sequence`, e.g. `[["inter",2],["native",1]]` expands to inter, inter, native, inter, ...
  `buildSequence` (:654) keeps only steps with a valid type, count > 0 and a configured unit id. Cursor = prefs
  `cl_end_ad_preferences` / `back_ad_sequence_cursor`; current step = `cursor % total` walked over the counts. The cursor advances
  (`advanceAlternateSequence`) ONLY when an ad is really shown and dismissed (inter/app open `onAdDismissedFullScreenContent`, native on dialog dismiss).
  A failed load / failed show does not advance, so the same type is retried next time. The cursor resets to 0 when the stored
  sequence string changes on a config apply (RemoteConfigValues:221-227). Mode other than `alternate` never advances.
- Unit ids (`unitIdFor`, :602): inter = `ClEnd_Back_Ad_Interstitial_Id` (fallback root `inter_ads.inter_ads_id`, see
  `RemoteConfigValues.getClEndBackAdInterstitialId` :553); app open = root `App_Open_Id`; native = `ClEnd_Native_Id` (fallback
  `ClEnd_Back_Ad_Native_Id`). `ClEnd_Banner_Id` is never used for the back ad. A type without a unit id is treated as not configured.
- If the ad is ready (`isAdReady`) it is shown immediately; otherwise `waitAndShow` (:97): preloads, shows `dialog_loading_ads`
  (not cancelable) and polls every 250 ms up to `READY_WAIT_MS = 8000`; it stops early when the load is no longer running or failed after
  the wait started. Failure -> `failShow` (re-preloads, callback(false), the screen finishes).
- Inter / App Open: shown with `ad.show(activity)`; callbacks set when the ad loads (`onContactListner`, `onAppOpenListner`) hold
  the Activity that did the load. On dismiss: advance sequence, preload next, callback(true).
- Native: dialog `R.style.Theme_NativeFullAd`, layout `native_full_ad_layout` (has its own `ivClose` on the media, brought to front),
  system bars handled by `AdPlacement.applyNativeFullSystemBars` and restored on dismiss, cancelable with Back. Dismiss -> destroy,
  advance, preload, callback(true).
- After callback the activity finishes 500 ms later. Static `onCompleteAdCallBack` is a single slot (a second call overwrites the first).
- Load/revenue analytics: `AppUtils.trackScreen` events `CL_END_INTER_LOAD/FAILED`, `CL_END_APP_OPEN_LOAD/FAILED`, `CL_END_NATIVE_LOAD/FAILED`
  and `AdPlacement.logAdRevenue` paid listeners. Preloaded FCM native: `FULL_NATIVE_LOAD/FAILED`.

### A8. Remote Config: every key

Structure: `Cl_End_Screen_Show` is read from the config ROOT (the profile object after `resolveUserConfig`), all other keys from
`Screen.ClEndScreen` (`RemoteConfigValues.apply`, :157 and :206-237). Missing object or key = default.

| Key | Type | Default | Effect |
|---|---|---|---|
| `Cl_End_Screen_Show` (root) | bool | false | Master switch for the PHONE_STATE path. False = no screen after calls. Not used by the FCM path |
| `ClEnd_Ad_Show` | bool | false | Bottom ad on; also master gate of FCM path (`isCallEndPerformanceAllowed`, `tryOpenFromFcmData`, generic push) |
| `ClEnd_Ad_Type` | string | "banner" | `banner` prefers banner; anything else prefers native (see A7.1) |
| `ClEnd_Banner_Id` | string | "" | Bottom banner unit id |
| `ClEnd_Native_Id` | string | "" | Bottom native; FCM full-screen native preload; back-ad native unit (preferred) |
| `ClEnd_Back_Ad_Show` | bool | false | Back ad on (normal flow) and allows preload |
| `ClEnd_Back_Ad_Type` | string | "inter" | `inter` / `appopen` / `native` / `alternate` (+aliases, A7.3) |
| `ClEnd_Back_Ad_Sequence` | array of `[type,count]` (also array of `"type,count"` strings, or a string holding such JSON) | [] | Used for `alternate`; cursor in prefs |
| `ClEnd_Back_Ad_Show_After_Day` | int | 0 | Min install day (1 = install day). 0 = never (normal flow) |
| `ClEnd_Back_Ad_Show_Per_Day` | int | 0 | Spacing = 24h / value; 0 disables (also notification flow) |
| `ClEnd_Back_Ad_Interstitial_Id` | string | "" | Inter unit (fallback `inter_ads.inter_ads_id`) |
| `ClEnd_Back_Ad_Native_Id` | string | "" | Native unit fallback if `ClEnd_Native_Id` empty |
| `ClEnd_Back_Ad_Country_IP` | bool | false | Use cached IP country instead of SIM/network country, and trigger the IP lookup |
| `ClEnd_Back_Ad_Show_Country` | string[] | [] | Allowed countries for normal-flow back ad (full English names), empty = all |
| `Notification_Install_Days` | int | 0 | FCM tier "notification only": min install day |
| `Notification_Call_Install_Days` | int | 0 | FCM tier "notification + call state" |
| `Notification_Call_Overlay_Install_Days` | int | 0 | FCM tier "notification + call state + overlay/default Home" |
| `All_Allow_Permission_Show_Notification` | bool | false | Must be true for the 3-permission tier to pass (FCM and notification back ad) |
| `Notification_Back_Ad_Show` | bool | false | Allows the back ad over the notification full-screen native |
| `Notification_Close_Button_Show` | bool | false | Shows the close icon on the notification full-screen native |
| `Notification_Country` | string[] | [] | Country list for the notification-only tier (empty = all) |
| `Notification_Call_Country` | string[] | [] | Country list for the notification + call tier |
| `Notification_Call_Overlay_Country` | string[] | [] | Country list for the 3-permission tier |

Persistence (so a cold receiver / FCM process works): `saveClEndConfig` (:779) writes the values into prefs
`cl_end_config_preferences`; `ensureLoaded` (:287, called from `AdPlacement.ensureClEndConfig`) restores them once per process when
`clEndScreenShow` exists in the prefs and nothing was applied yet. Gaps (read from code, impact **unverified**):
`appOpenId` and the native label/button colours are saved but not restored by `ensureLoaded`; the `inter_ads_id` fallback is also not
restored. In a process that never fetched Remote Config, an `appopen` back ad has no unit id until config is applied again.

Example JSON (inside the active profile object):
```json
{
  "Cl_End_Screen_Show": true,
  "App_Open_Id": "ca-app-pub-xxx/appopen",
  "Screen": {
    "ClEndScreen": {
      "ClEnd_Ad_Show": true,
      "ClEnd_Ad_Type": "native",
      "ClEnd_Banner_Id": "ca-app-pub-xxx/banner",
      "ClEnd_Native_Id": "ca-app-pub-xxx/native",
      "ClEnd_Back_Ad_Show": true,
      "ClEnd_Back_Ad_Type": "alternate",
      "ClEnd_Back_Ad_Sequence": [["inter", 2], ["appopen", 1], ["native", 1]],
      "ClEnd_Back_Ad_Show_After_Day": 2,
      "ClEnd_Back_Ad_Show_Per_Day": 4,
      "ClEnd_Back_Ad_Interstitial_Id": "ca-app-pub-xxx/inter",
      "ClEnd_Back_Ad_Native_Id": "",
      "ClEnd_Back_Ad_Country_IP": true,
      "ClEnd_Back_Ad_Show_Country": ["India", "United States"],
      "Notification_Install_Days": 3,
      "Notification_Call_Install_Days": 2,
      "Notification_Call_Overlay_Install_Days": 1,
      "All_Allow_Permission_Show_Notification": true,
      "Notification_Back_Ad_Show": true,
      "Notification_Close_Button_Show": true,
      "Notification_Country": ["India"],
      "Notification_Call_Country": [],
      "Notification_Call_Overlay_Country": []
    }
  }
}
```

### A9. Other behaviours worth knowing
- `ClEndActivity.visible` (set in `onStart`/`onStop`, :216-226) is read by `EventPromptLauncher:121` so Charging / Install screens do not
  open over the Call End screen; `PhoneCallStateService:53` also tells the event screens a call is active (`onPhoneStateChanged`).
- `finishAfterLaunch` (:69) is how every tab action closes the screen.
- Launch mode `singleTask` + `CLEAR_TOP|SINGLE_TOP`: if an instance is alive, `onNewIntent` is not overridden and `setIntent` is not
  called, so the old extras/time/duration stay on screen (bug B-4).
- Notification 1001 is cancelled in `setupFullAdContainer` (:158) and `onResume` (:206).

### A10. Controls summary for porting
Copy: the 12 Java files in A1, layouts (A1 list + `dialog_loading_ads`, `shimmer_large_ad`), drawables `ic_content_cl_end`,
`ic_message_cl_end`, `ic_reminder_cl_end`, `ic_more_cl_end`, `tab_indicator_round`, theme `Theme.ClEnd`, strings `cl_end_*`, manifest entries,
Remote Config parsing (`RemoteConfigValues` ClEnd block + save/ensureLoaded + sequence cursor reset), and the AdPlacement helpers listed above.

### A11. Test matrix (Call End)

| # | Setup | Expected |
|---|---|---|
| 1 | `Cl_End_Screen_Show=false`, end a call | Nothing opens |
| 2 | Show=true, overlay granted, unlocked, outgoing call 30 s | Screen with "Outgoing", `00:30`, tabs, bottom ad if `ClEnd_Ad_Show` (see bug B-1 for the type) |
| 3 | Incoming answered / rejected-unanswered | "Incoming" / "Missed Call" (duration = ring time) |
| 4 | No overlay permission, not default Home | Nothing opens (swallowed exception) |
| 5 | Default Home, no overlay | Opens directly (no overlay flash) |
| 6 | Phone locked after call | Heads-up/full-screen notification "See call Information"; tap opens screen and cancels notification |
| 7 | Back with `ClEnd_Back_Ad_Show=true`, `After_Day=1`, `Per_Day=4`, type `inter` | Ad shown (preloaded: instant; else loading dialog up to 8 s); screen closes 500 ms after. Second back within 6 h: no ad |
| 8 | `After_Day=0` | Never a back ad (normal flow) |
| 9 | `alternate` `[["inter",2],["native",1]]` | inter, inter, native, inter, ... cursor in `back_ad_sequence_cursor`; changing the sequence resets it |
| 10 | Country list ["India"] with SIM in another country, `Country_IP=false` | No back ad. With `Country_IP=true` and IP in India (after lookup cached) | Ad |
| 11 | FCM data `type=call_end` while locked, app in background, `ClEnd_Ad_Show=true`, notification permission, tier passes, native loads | Full-screen notification; opening it shows full-screen native, close button per key |
| 12 | Same but native fails to load | Nothing appears (also no default FCM notification) |
| 13 | FCM while unlocked or app foreground | Nothing from this path |
| 14 | Notification flow, `Notification_Back_Ad_Show=true`, Back | Back ad subject to tier days/country; `Per_Day>0` required |
| 15 | Fallback normal view in notification flow (holder empty) | Bottom ad; back ad skipped |
| 16 | App language Hindi | Call End screen stays English |
| 17 | Charge cable plugged while Call End visible | Charging screen does not open over it |
| 18 | Tabs: send canned SMS, create reminder, delete reminder, reboot | SMS composer opens; reminder fires; BootReceiver reschedules |

### A12. Bugs / risks found (not fixed)
- **B-1** `PhoneCallStateService.isRingingCall` is never reset (`resetCallFlags` :113 omits it). After the first ring in a process, every
  later OFFHOOK takes the incoming branch (:86-91), so outgoing calls are labeled "Incoming" and an outgoing call that is declined may be
  "Missed Call". Fix: reset `isRingingCall=false` in `resetCallFlags`. (Read from code, not reproduced.)
- **B-2** The documented install-day / country / permission tier gate is only enforced for FCM, never for normal calls
  (`isCallEndPerformanceAllowed(…, false)` unused). The ad slot is still gated only by `ClEnd_Ad_Show`, and the back ad by its own keys.
- **B-3** The number is never displayed, "redial" opens an empty dialer, canned SMS has no recipient (`mobile_number` unused).
- **B-4** No `onNewIntent`: a second call end while the first screen is still alive reuses stale data and ads.
- **B-5** `CallEndPendingLaunch` is write-only; there is no relaunch of a missed locked notification beyond the notification itself.
- **B-6** `ClEndActivity` is `exported="true"` and trusts `is_from_fcm` / `CallType` extras: any app can open it (and trigger the
  notification-flow UI). Consider `exported="false"` (receiver start works with an explicit in-app intent). `CallEndReceiver` is an exported
  `CallScreeningService` that allows every call and does nothing else; it is unrelated to the call-end screen (misleading name).
- **B-7** Hard-coded ip-api key in `IPAddressHelper:19`; IP country is cached forever (VPN / travel changes not picked up).
- **B-8** Alternate sequence stalls on a type that keeps failing (cursor only advances on a shown ad).
- **B-9** FCM suppression: any FCM message that reaches `handleIntent` and is not handled returns without `super.handleIntent`, so no
  normal notification is ever displayed by the system tray handler for backgrounded messages while the app is in foreground/unlocked
  (by design of the current code, worth confirming with product).
- **B-10** `ensureLoaded` does not restore `appOpenId` / `inter_ads_id` fallback (see A8).
- **B-11** Preloaded back ad listeners capture the Activity that started the load (possible leak / callbacks to a finished activity) - **unverified** in practice.

---------------------------------------------------------------------------------------------------

## PART B. "MAKE THIS APP YOUR DEFAULT LAUNCHER" NOTIFICATION REMINDER

### B1. Files
| File | Role |
|---|---|
| `L/common/DefaultLauncherReminder.java` | Config store, scheduling plan, `sync`, notification post, Splash tap handling |
| `L/common/DefaultLauncherReminderWorker.java` | WorkManager `Worker` that decides to post |
| `L/remote/RemoteConfigHelper.java` 284-303 | `applyDefaultLauncherReminder` reads the 4 keys |
| `L/common/StartupNavigation.java:22` | Splash hook on tap |
| `L/activities/DefaultActivity.java` | Reminder-mode Default Home screen (`EXTRA_FROM_REMINDER`) |
| `L/common/ProcessAppOpen.java:168-169` | Clears stale notification when the app comes to foreground |
| strings.xml 616-618 | `default_launcher_reminder_channel/title/text` |
| Manifest | No entry needed (WorkManager, `DefaultActivity` 133-137 `exported=false`) |

### B2. Remote Config keys (where read from)
`applyDefaultLauncherReminder` (RemoteConfigHelper:289) uses `source = jsonObject.launcher_home_screen` if that object exists, else the
config ROOT of the active profile. Keys:
| Key | Type | Default | Meaning |
|---|---|---|---|
| `launcher_notification_push_show` | bool | false | Master switch; false cancels all reminder work |
| `launcher_notification_push_time` | int | 24 | Notifications per day: interval seconds = 86400 / value (12 -> 2 h, 24 -> 1 h, 48 -> 30 min, 96 -> 15 min, 5760 -> 15 s) |
| `launcher_notification_push_title` | string | "" | Blank -> `default_launcher_reminder_title` ("Make QR Code & Scanner your home screen") |
| `launcher_notification_push_description` | string | "" | Blank -> `default_launcher_reminder_text` |
Values go to prefs `default_launcher_reminder_prefs` (`push_show/time/title/description`) via `Config.apply` (DefaultLauncherReminder:105),
which then calls `sync`. Applied on every config apply (each fetch), never from `onResume`. Invalid time (<=0) -> 24; interval floor 5 s (only for
push_time > 17280). Example:
```json
{ "launcher_home_screen": { "launcher_notification_push_show": true, "launcher_notification_push_time": 12,
  "launcher_notification_push_title": "Make us your home screen", "launcher_notification_push_description": "Faster, smoother access." } }
```
(Same keys also work directly at the root.) Note the Javadoc at DefaultLauncherReminder:33 says the object is `launcher_home_screen`; matches code.

### B3. Scheduling (`DefaultLauncherReminder.sync`, :173)
One strategy at a time:
- Interval >= 900 s (`PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS`): unique periodic work `default_launcher_reminder_work`
  (`ExistingPeriodicWorkPolicy.UPDATE`, initial delay = one interval, i.e. first reminder one interval after the config apply);
  cancels the chain `default_launcher_reminder_chain`.
- Interval < 900 s: unique one-time chain `default_launcher_reminder_chain` (`ExistingWorkPolicy.KEEP`, input `reminder_chain=true`, delay = interval);
  cancels the periodic work. Each chain run schedules the next one in `Worker` (`scheduleNextChainRun` :216, `APPEND_OR_REPLACE`, reading
  the CURRENT config; hands back to `sync` if the interval became >= 15 min or the switch is off).
- `push_show=false` -> `cancelWork` (both unique works).
- The schedule is NOT cancelled when the app becomes the default; the worker simply posts nothing then (so it resumes if the user removes the default).
- WorkManager survives reboot by itself; `BootReceiver` does not touch this reminder (it only calls `ReminderAlarmHelper.rescheduleAll` for the
  user's Call End reminders). WorkManager is not exact (Doze / OEM may delay).

### B4. Worker conditions (`DefaultLauncherReminderWorker.doWork`, :28)
1. `push_show` false -> `cancelWork`, success.
2. `isDefaultHomeApp` true -> skip + `onDefaultLauncherConfirmed` (dismisses a leftover notification 7001).
3. `!hasCompletedOnboarding` -> skip (onboarding already leads to the Default screen).
4. `ProcessAppOpen.isAppInForeground()` -> skip.
5. Else `showNotification` (:306): requires `areNotificationsEnabled()`; creates channel `default_launcher_reminder_channel`
   (IMPORTANCE_DEFAULT) every time; posts id 7001 (same id replaces the previous), `setOnlyAlertOnce`, `autoCancel`, BigText.
   `SecurityException` (permission revoked) swallowed.
6. If this was a chain run -> `scheduleNextChainRun`. Always returns `Result.success()`; exceptions swallowed; no logging/analytics.
Notification permission: uses the same `POST_NOTIFICATIONS` runtime permission asked in onboarding; there is no extra request here.
Dismissal: `ProcessAppOpen.onStart` (:168-169) cancels notification 7001 every time the app comes to the foreground; `cancelIfDefault` is also
called from `DefaultActivity`.

### B5. Tap flow
Notification content intent (`showNotification` :323): `MainActivity` with `ACTION_MAIN` + `CATEGORY_LAUNCHER`, extra
`extra_from_default_launcher_reminder=true`, flags `NEW_TASK | CLEAR_TASK`, PendingIntent request code 7001, `UPDATE_CURRENT | IMMUTABLE`.
1. `MainActivity` shows the Compose splash (ads/init run as normal) and at its end `StartupNavigation.continueAfterVisibleSplash`
   calls `DefaultLauncherReminder.handleSplashEntry` (:285): removes the extra (consumed), then
   - onboarding not completed -> returns false (normal first-run flow);
   - already default Home -> dismiss notification, normal path (launcher home);
   - else starts `DefaultActivity` with `EXTRA_FROM_REMINDER=true`, finishes the splash.
2. `DefaultActivity` reminder mode (`reminderEntry`): if already default -> `cancelIfDefault`, `openMain`, finish. Otherwise shows the
   Default Home screen with the permission-default bottom ad (`getPermissionDefaultAd*`) and preloads the after-default ad.
   Button "Set as default": READ_PHONE_STATE first (if missing), then the RoleManager `ROLE_HOME` request via `DefaultHomePromptHelper`
   (events `DEFAULT_HOME_APP_SET` / `DEFAULT_HOME_APP_CANCEL`, see `default-home-role-flow`). **Back** in reminder mode goes straight to
   `ScreenFlowNavigation.openMain` (no role request), unlike onboarding where Back = Set as default.
3. After Allow OR Cancel (`continueScreenFlow`, :227 reminder branch): `AdPlacement.loadAfterDefaultAd` then `cancelIfDefault` and
   `ScreenFlowNavigation.openMain` -> Launcher home. No onboarding steps. If the role was granted the system also restarts the launcher.
Analytics specific to the reminder: none (no event for post / tap).

### B6. How it differs from the Call End "Reminder" tab
| | Default-launcher reminder | Call End Reminder tab (`ReminderAlarmHelper`) |
|---|---|---|
| Source | Remote Config | User-created (text, date/time, colour) |
| Engine | WorkManager periodic / chain | `AlarmManager` exact-while-idle (inexact fallback) via `ReminderReceiver` |
| Storage | prefs `default_launcher_reminder_prefs` | prefs `reminder_preferences` JSON list |
| Channel | `default_launcher_reminder_channel` (7001) | `reminder_alerts_persistent`, tag `app_reminder` |
| Tap | Splash -> DefaultActivity -> Launcher home | `MainActivity` with `fromReminderNotification`, `CLEAR_TASK` |
| Boot | WorkManager persists | `BootReceiver` (`BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`) -> `rescheduleAll` |

### B7. Test matrix (reminder)
| # | Setup | Expected |
|---|---|---|
| 1 | `push_show=false` | No work scheduled; existing work cancelled at next config apply |
| 2 | `push_show=true`, time=24, onboarding done, not default, app in background | Notification about 1 h after config apply, then hourly (WorkManager may drift) |
| 3 | time=96/5760 (sub-15-min) | Chain of one-time works at exact interval; second apply does not duplicate (KEEP) |
| 4 | Switch from 5760 to 24 | Next chain run calls `sync` -> periodic work, chain cancelled |
| 5 | App is default Home | Nothing posted, leftover cleared; schedule continues |
| 6 | User removes default later without opening app | Next run posts the reminder |
| 7 | App in foreground | Skipped; notification cleared on app start |
| 8 | Onboarding not finished | Skipped |
| 9 | Notifications disabled | Not posted |
| 10 | Tap, not default | Splash -> DefaultActivity (ad) -> Set as default -> Allow/Cancel -> after-default ad -> Launcher home |
| 11 | Tap, already default | Splash -> launcher home |
| 12 | Tap during first-run onboarding | Normal onboarding flow |
| 13 | Blank title/description in config | Built-in localized strings |
| 14 | Reboot | WorkManager reschedules itself; verify notification still arrives (**unverified** on OEMs with aggressive battery killers) |

### B8. Common bugs / gotchas (reminder)
- Works only after a successful Remote Config apply in a process (`Config` prefs only exist then); until then show=false.
- Each config apply with changed periodic interval replaces the work (UPDATE) and moves the next run; an unchanged value does not.
- The notification channel is created with IMPORTANCE_DEFAULT once; changing importance later requires a new channel id or user settings.
- Title/description come from the activated config at post time, not at schedule time.
- Tapping while Splash is slow: the extra is consumed on first handle; a recreated Splash will not redirect again.
- `isDefaultHomeApp` is role-based on Q+; a false negative (role manager unavailable) makes the app post reminders although it is default - **unverified**.
