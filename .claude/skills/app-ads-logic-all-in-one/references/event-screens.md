# Event screens: Charging In/Out and App Installed/Uninstalled

Verified against the code (not against the older skills). Paths are relative to
`app/src/main/java/com/qrcode/scanner/launcher/` unless noted. The Gradle namespace / applicationId is
`com.qrcode.scanner.spapp` (R class: `com.qrcode.scanner.spapp.R`); Java packages stay `com.qrcode.scanner.*`.
Anything not proven by code is marked "unverified".

## 0. Map

| Piece | File | Role |
|---|---|---|
| Watch (runtime receiver) | common/EventPromptWatch.java | Registered once in `QrScannerApplication.onCreate` (QrScannerApplication.java:29). Power + package broadcasts. |
| Manifest receivers | receivers/EventPromptReceiver.java (manifest 306-314), services/PhoneCallStateService.java (manifest 279-295) | Package add/remove, and power via the PhoneCallStateService filter. |
| Gate + opener | common/EventPromptLauncher.java | `open()` 108, `showEvent()` 163. All checks, overlay trick, notification fallback. |
| Base screen | activities/EventPromptActivity.java | Hold timer, Done/Back, bottom ad, back ad call, lifecycle. |
| Charging screen | activities/ChargingScreenActivity.java | kinds charge_in / charge_out, key `charging`. |
| Install screen | activities/InstallUninstallScreenActivity.java | kinds install / uninstall, key `install_uninstall`. |
| Bottom ad cache | common/EventBottomAds.java | PreLoad cache of native/banner per screen. |
| Back ad | common/AdPlacement.java | `showEventBackAd` 1576, `preloadEventBackAd` 1672, `showPreloadedEventBackAd` 1792, `nextEventBackAdType` 1843, `isCountryAllowed` 1897, `executeSequencedAd` 1988. |
| Config | remote/RemoteConfigValues.java | `EventScreenConfig` 78-96, `apply` 192-197, `applyEventScreen` 1071, `parseEventBackAdSequence` 1102, `isEventScreenEnabled` 471, `eventScreenKeyForKind` 485, `getEventScreenConfig` 493, save 819 / restore 839. |

Two screen keys: `RemoteConfigValues.EVENT_SCREEN_CHARGING = "charging"` and
`EVENT_SCREEN_INSTALL_UNINSTALL = "install_uninstall"` (RemoteConfigValues.java:74-75). Each key owns one
`EventScreenConfig`, one `EventBottomAds.Cache` and one back-ad cache. The four kinds (`EventPromptActivity.KIND_*`,
EventPromptActivity.java:52-55) are `install`, `uninstall`, `charge_in`, `charge_out`.

Not in the code (the brief asked): there is NO app icon / app name resolution. The install and uninstall screens show
static text only. `EventPromptReceiver`/`EventPromptWatch` read the package name only to ignore this app's own package;
it is not passed to the screen (intent extras are just `event_kind` and `event_launched_at`, EventPromptLauncher.java:134-135).
No time of day is shown either; only the battery percent on Charging.

## 1. Triggers and gating

### 1.1 Who detects what

| Event | Detected by | Notes |
|---|---|---|
| Power connected / disconnected | (a) `EventPromptWatch.RECEIVER` (runtime, `ACTION_POWER_CONNECTED/DISCONNECTED`, EventPromptWatch.java:59-61); (b) manifest `PhoneCallStateService` filter with `ACTION_POWER_CONNECTED/DISCONNECTED` (manifest 286-287; handled PhoneCallStateService.java:33-40) | (b) has no held-back check. The code comment says charger broadcasts are not delivered to a manifest receiver on current Android (EventPromptWatch.java:12-14); (b) is a legacy/backup path. Delivery of (b) on Android 8+: unverified. |
| Package added / removed | (a) `EventPromptWatch` (`ACTION_PACKAGE_ADDED/REMOVED`, `addDataScheme("package")`, EventPromptWatch.java:62-65); (b) manifest `EventPromptReceiver` (manifest 306-314) | Both ignore `EXTRA_REPLACING` (updates), a null data URI and this app's own package (EventPromptWatch.java:41-47, EventPromptReceiver.java:18-24). (b) has no held-back check. |
| Phone state | `PhoneCallStateService` -> `EventPromptLauncher.onPhoneStateChanged` (PhoneCallStateService.java:53) | Not an event screen trigger; it only mutes them (see 1.3). |

Why runtime registration and not only manifest: since Android 8 (API 26) implicit broadcasts cannot be received by
manifest receivers, and the charger/package broadcasts are implicit. `QrScannerApplication.onCreate` registers the
`EventPromptWatch.RECEIVER` for as long as the process lives. On API 33+ it passes `Context.RECEIVER_EXPORTED`
(EventPromptWatch.java:66-69); older versions use plain `registerReceiver`. Consequence: when the process is dead or
frozen (swipe-away, Xiaomi/OPPO/vivo battery kill) nothing is received until the app is started again.
`PACKAGE_ADDED/REMOVED` with `data scheme=package` is on the older exemption list in practice, so the manifest
`EventPromptReceiver` may still wake a dead process on some Android versions: unverified per version.

Registration happens once (`registered` flag, EventPromptWatch.java:56-58). `RemoteConfigValues.ensureLoaded` is called
in `Application.onCreate` (QrScannerApplication.java:28) so the saved flags exist for a cold start.

### 1.2 Pipeline inside the watch

`EventPromptWatch.RECEIVER.onReceive` (EventPromptWatch.java:20-49):
1. null checks on context/intent/action.
2. `EventPromptLauncher.isHeldBackDelivery()` -> return (drop). True when the process started less than 3500 ms ago or
   the app came to foreground less than 3500 ms ago (`HELD_BACK_WINDOW_MS`, EventPromptLauncher.java:34 and 103-106;
   `ProcessAppOpen.msSinceForeground()` ProcessAppOpen.java:211). Reason: Android delivers events that were held while the
   app was frozen the moment the app starts/foregrounds; those are old.
3. power connected -> `open(KIND_CHARGE_IN)`, disconnected -> `open(KIND_CHARGE_OUT)`.
4. package added -> `open(KIND_INSTALL)`, removed -> `open(KIND_UNINSTALL)` (after the replacing / own package filter).

### 1.3 `EventPromptLauncher.open(context, kind)` (EventPromptLauncher.java:108-143)

Checked in this order; the first failure returns silently:
1. `context`/`kind` not null/blank.
2. `RemoteConfigValues.ensureLoaded(context)`, then `isEventScreenEnabled(kind)` (RemoteConfigValues.java:471):
   `charge_in` -> `charge_in_screen_show`, `charge_out` -> `charge_out_screen_show`, `install` -> `install_screen_show`,
   anything else -> `uninstall_screen_show`. All default false.
3. `hasInternet(context)` (EventPromptLauncher.java:76-95): `AdPlacement.isNetworkAvailable` (active network with
   WIFI/CELLULAR/ETHERNET transport) OR any network from `getAllNetworks()` with INTERNET + VALIDATED. The second path
   exists because a background-restricted app (Xiaomi etc.) gets a null `getActiveNetwork()`.
4. Call guard: skip if `callActive` (set by `onPhoneStateChanged`), or `isPhoneCallInProgress` (TelephonyManager call
   state != IDLE, or AudioManager mode RINGTONE / IN_CALL / IN_COMMUNICATION; lines 54-69), or
   `now < quietUntil` (5 s quiet window after every phone-state broadcast, `QUIET_AFTER_PHONE_STATE_MS`, line 33),
   or `ClEndActivity.isVisible()` (the Call End screen is on screen). Reason: a held-back power/install event is
   delivered before the PHONE_STATE broadcast that woke the app, so the real call state is read directly.
5. Debounce: same `kind` as the last one within 1500 ms is dropped (`lastKind`, `lastOpenAt`, lines 124-128). This is
   what de-duplicates the manifest receiver + runtime receiver both firing for one event. Different kinds are not
   debounced against each other (only the last kind is stored).
6. Builds the Intent for `ChargingScreenActivity` (charge kinds) or `InstallUninstallScreenActivity`, extras
   `event_kind` and `event_launched_at = now`, flags `NEW_TASK | CLEAR_TOP | SINGLE_TOP | REORDER_TO_FRONT`.
7. Runs `showEvent` on the main thread (posts to the main Looper if needed).

### 1.4 `showEvent` (EventPromptLauncher.java:163-199): how it starts from background

1. Return silently if `!ProcessAppOpen.isAppInForeground() && !AppUtils.hasOverlayPermission(app) && !AppUtils.isDefaultHomeApp(app)`.
   Reason (code comment): with none of those, a background start cannot show the screen and Android would park it in
   the app task, so it would pop up later when the user opens the app. So the notification fallback below is only
   reached when the app is foreground, has overlay, or is default Home.
2. `new CallEndViewManager().showCallEnd(app, intent)` (common/CallEndViewManager.java:18-47):
   - default Home app: plain `startActivity` (Home apps may start from background).
   - otherwise: adds a tiny `TYPE_APPLICATION_OVERLAY` view (layout `call_end_view`, NOT_FOCUSABLE, SHOW_WHEN_LOCKED,
     TURN_SCREEN_ON, KEEP_SCREEN_ON), then after 250 ms calls `startActivity(intent)` and removes the overlay. An app
     with a visible overlay window is allowed to start activities from background; this is the "overlay trick".
   - `addView` throws without overlay permission -> caught in `showEvent`.
3. On any exception, `app.startActivity(intent)` directly (works when the app is in foreground).
4. If that throws too, cancel any stale notification of that kind and post a full-screen-intent notification via
   `CallEndFullNotificationHelper.notifyGenericFullscreen` (title = `event_charge_in_title` / `event_charge_out_title` /
   `event_install_sub` / `event_uninstall_sub`, icon `R.mipmap.ic_launcher`). Notification ids: uninstall 9101,
   charge_in 9102, charge_out 9103, install 9104 (lines 23-26, 145-153). Needs `USE_FULL_SCREEN_INTENT` (declared,
   manifest line 43); the helper picks a high-priority full-screen intent when allowed (`canUseHighPriorityFullScreenIntent`).
   The screen cancels its own notification in `onCreate` / `onNewIntent` (EventPromptActivity.java:79, 103).

Permissions in the manifest used here: `SYSTEM_ALERT_WINDOW` (42), `USE_FULL_SCREEN_INTENT` (43), `POST_NOTIFICATIONS` (38),
`ACCESS_NETWORK_STATE` (39), `READ_PHONE_STATE` (41, for the call guard), `INTERNET` (37). No new permission is requested
by these screens themselves.

### 1.5 Checks inside the activity

`EventPromptActivity.onCreate` (75-96): cancel notification; if `!hasInternet` -> `finish()` (screen never builds);
`setShowWhenLocked(true)` / `setTurnScreenOn(true)` (API 27+); `setContentView`; hide navigation bar; `bindKind`;
`bindClicks`; `setupBackBlock`; `loadBottomAd`; `startHoldProgressThenShowDone`.

Stale launch guard (132-135, 112-121, 77): the first `onResume` compares `now - event_launched_at` with 6000 ms
(`STALE_LAUNCH_MS`). If the screen only became visible after more than 6 s (an activity that sat in the task, or a
notification tapped late) it calls `leaveWithoutAd("stale launch")` = `finishAndRemoveTask()`. `onNewIntent` sets
`checkFreshOnResume = !resumed`.

Manifest (173-189): both activities `exported=false`, `launchMode=singleTask`, `excludeFromRecents=true`,
`screenOrientation=portrait`, `showWhenLocked=true`, `turnScreenOn=true`, theme `@style/Theme.EventPrompt`
(themes.xml:25: AppCompat Light NoActionBar, status bar `event_header`, nav bar `event_sheet`, light status bar).

### 1.6 Content data

- Charging (ChargingScreenActivity.bindKind 36-53): `batteryPercent()` (EventPromptActivity.java:247-257) reads
  `BatteryManager.BATTERY_PROPERTY_CAPACITY`, 0 if outside 0-100. Charge in: title "Power Plugged In", success `"%d%%"`,
  green palette (header 0xFFE7F8EF, accent 0xFF16A34A), hero `ic_event_hero_charge_in`, status icon `ic_event_check`,
  badge = level%. Charge out: title "Charger Unplugged", success `"%d%% Optimal"`, orange palette (0xFFFFF4E8 / 0xFFEA580C),
  hero `ic_event_hero_charge_out`, status icon `ic_event_bolt`. Level is read once in `bindKind`, so a broadcast that
  arrives just before the battery property updates can show the previous level (unverified on-device).
- Install / Uninstall (InstallUninstallScreenActivity.bindKind 31-47): static text and palette (header 0xFFEAF1FB,
  accent `R.color.primary`), badge text `event_fast`, hero `ic_event_hero_install` / `ic_event_hero_uninstall`.
  Install: "New App Installed" / "Install done" / "App installed successfully"; Uninstall: "Uninstalled?" / "Cleanup done"
  / "App uninstalled successfully". The "new app" is not named.
- `showContent` (198-224) fills ids `tvEventTitle`, `tvEventSuccess`, `tvEventSubtitle`, `tvEventBody`, `tvOptimizeTitle`,
  `tvOptimizeBadge`, `tvOptimizeSub`, `ivEventHero`, `ivEventStatusIcon`, `ivOptimizeIcon`; `applyScreenColors` (226-245)
  tints root, header, status bar, success text, Done button, badge and sets `adCardColor = R.color.event_card`.
- Strings `event_*` (values/strings.xml:500-523) exist in values and 19 locale folders (af, de, el, es, fil, fr, hi, in, it,
  ja, ko, nl, pt, pt-rBR, ru, th, tr, zh seen in the grep), so the text is LOCALIZED, not English-only. Keys:
  `event_charge_in_*`, `event_charge_out_*`, `event_install_*`, `event_uninstall_*`, `event_optimized_speed`, `event_fast`,
  `event_optimized_sub`, `event_done`.

### 1.7 Done / Back / auto-close

- Both screens ignore Back and Done until the hold timer ends (`canExit` false): `setupBackBlock` 281-290 swallows back
  presses (`OnBackPressedCallback(true)` that only acts if `canExit`); the Done button is hidden (GONE) until then.
- Done or Back after the timer -> `exitScreen()` (339-347): sets `exiting`, `adRequest++` (cancels pending bottom-ad
  callbacks), `releaseAds()` (destroy bottom ad), then `AdPlacement.showEventBackAd(this, screenKey(), this::finish)`.
  `finish()` runs when the back ad is dismissed / fails / is skipped. Plain `finish()`, not `finishAndRemoveTask()`.
- Settings gear `ivEventSettings` and the "Optimized speed" card `llOptimizedSpeed` open `LauncherSettingsActivity`
  (bindClicks 263-279); `internalNavigation = true` prevents the leave handler from closing the screen.
- Auto-close WITHOUT a back ad (`leaveWithoutAd` -> `finishAndRemoveTask()` 161-166): HOME / Recents
  (`onUserLeaveHint` 142-149), `onStop` when not finishing / not exiting / not internal navigation (152-159, covers screen
  off), and the stale-launch guard. The back-ad activity itself triggers `onUserLeaveHint`, which is why `exiting` is
  checked. There is no timer-based auto-close: the user must wait for the timer, then press Done/Back.
- Second event while open (`onNewIntent` 99-109): rebinds content, restarts the hold timer, `exiting=false`. Install
  screen reloads the bottom ad (`reloadAdsOnNewIntent()` default true); the Charging screen returns false
  (ChargingScreenActivity.java:31-33) so it keeps its ads.
- `onDestroy` (660-671): cancels animator, releases ads, then `AdPlacement.preloadEventBackAd(app, key)` so the next back
  ad is ready.

## 2. UI

Layouts `res/layout/activity_charging_screen.xml` and `activity_install_uninstall_screen.xml` are identical files
(a `diff` printed nothing); the two activities just point at one each.

Structure: root `LinearLayout#eventRoot` (fitsSystemWindows, bg `event_header` #C2E6F6, overridden per kind) ->
1. `FrameLayout#eventHeader` (min 188dp): hero `ImageView#ivEventHero` 170dp end/center, settings `ivEventSettings` 40dp
   top end (`bg_event_icon_well`, `ic_event_settings`), text column with `tvEventTitle` (28sp, semi-bold), `tvEventSuccess`
   (28sp, accent), status row (`ivEventStatusIcon` 18dp + `tvEventSubtitle` 14sp), `tvEventBody` (13sp).
2. Sheet `LinearLayout` (`bg_event_sheet`, weight 1) -> `ScrollView` containing:
   - card `llOptimizedSpeed` (`bg_event_card`): `ivOptimizeIcon`, `tvOptimizeTitle` (15sp, max 2 lines), `tvOptimizeBadge`
     (`bg_event_fast_badge`, 11sp), `tvOptimizeSub` (12sp), arrow `ic_event_arrow`.
   - ad host `RelativeLayout#rlEventAd` (visibility gone by default, margin 16/12dp) with `CardView#eventAdCard` (radius 12dp,
     white, elevation 0) containing `ShimmerFrameLayout#slEventShimmerNative` (includes `layout_event_shimmer_large`),
     `ShimmerFrameLayout#slEventShimmerBanner` (gone; includes `layout_event_shimmer_small`) and `FrameLayout#flEventNative`;
     plus `LinearLayout#llEventBanner` (outside the card, for full banners).
   - footer `FrameLayout`: `ProgressBar#pbEventHold` (14dp, max 1000, `bg_event_progress`) and `TextView#btnEventDone`
     (56dp, `bg_event_done`, text `event_done`, white 17sp, gone).
3. Fonts: `@font/sans_trial_semi_bold` and `@font/sans_trial_medium` (res/font). Colors `event_*` in values/colors.xml:8-19
   (`event_header`, `event_sheet`, `event_title`, `event_accent` = primary, `event_muted`, `event_card` #F5F5F5,
   `event_fast`, `event_fast_text`, `event_shimmer`, `event_shimmer_block`, `event_progress_track`, `event_check`).
   Drawables: `bg_event_card/done/fast_badge/icon_well/progress/sheet`, `ic_event_arrow/bolt/check/settings`,
   `ic_event_hero_charge_in/charge_out/install/uninstall`.
4. Hold timer (301-324): `RemoteConfigValues.getEventScreenConfig(key).buttonShowSec`; `<1` falls back to 6 s. A linear
   `ValueAnimator` 0..1000 drives `pbEventHold`; on end `showDoneButton()` hides the bar, shows `btnEventDone`, sets
   `canExit = true`. `restartHold()` re-runs it for a new event.
5. Shimmer: native shimmer is visible while a native ad loads (`loadNativeAd` 482-519, `startShimmer`); banner shimmer while
   a banner loads (544-589). On failure `hideAdPlaceholders()` hides `rlEventAd` entirely. On success the shimmer stops
   and goes GONE. Native ad fill is repainted to `event_card` by `paintNativeCard/applyAdFill` (522-542).
6. Window: `setDecorFitsSystemWindows(true)`, navigation bar hidden (transient by swipe, API 30+), light status bar,
   `setShowWhenLocked`, `setTurnScreenOn`.

## 3. Ads

Everything is gated by the screen's own flags, independent of the global `Ad_Priority`/Quiz settings except where noted.
Quiz is used ONLY as an explicit sequence step.

### 3.1 Bottom ad

`EventPromptActivity.loadBottomAd` (349-377), called from `onCreate` and (Install only) `onNewIntent`:
1. `RemoteConfigValues.ensureLoaded`; `AdPlacement.preloadEventBackAd(this, key)` (so the back ad starts loading as the
   screen opens).
2. Let `type = <prefix>_ad_load_type` trimmed. Show nothing (hide `rlEventAd`, `EventBottomAds.clear(key)`) unless
   `<prefix>_bottom_ad_show` is true AND type is `load` or `preload` (case-insensitive). Any other value (e.g. `none`) = no bottom ad.
3. `useNative = EventBottomAds.useNative(key)` = `<prefix>_bottom_ad_type` is NOT `banner` (so `native`, empty or any other
   string = native).
4. `preload`: tries `showPreloadedAd` (takes the cached `NativeAd`/`AdView` from `EventBottomAds`, 379-402). If found: show
   instantly and call `EventBottomAds.prepare` to refill. If not found: falls through to a normal load and also calls `prepare`.
5. `load` (or preload cache miss): `loadNativeAd(request)` uses `AdPlacement.loadNativeAd(..., "large", ..., googleOnly=true,
   fill color, forceLightTheme=true)` with `<prefix>_native_id`; `loadBannerAd(request)` uses
   `AdPlacement.requestAdaptiveBannerAd(this, <prefix>_banner_id, widthDp)` (anchored adaptive, width from `rlEventAd`).
6. A stale-request counter `adRequest` makes late callbacks destroy their ad (lines 385, 396, 502-507, 564-571).
7. Failure (`onAdFailed`): `hideAdPlaceholders()` only. NO Quiz fallback (`googleOnly=true`, comments at 516 and 585);
   Quiz priority does not apply to this ad either.

`EventBottomAds` (common/EventBottomAds.java): one cache per screen (CHARGING, INSTALL_UNINSTALL).
- `prepare(context)` runs for both screens whenever config is applied or restored (RemoteConfigValues.java:282, 324);
  `prepare(context, key)` per screen: calls `AdPlacement.preloadEventBackAd`, then if `bottom_ad_show && load_type == preload`
  preloads a native (id `native_id`, empty id or no network -> skip) or a banner (inline adaptive size
  `getCurrentOrientationInlineAdaptiveBannerAdSize`, width = screen minus 32dp), destroying the other type. Otherwise `clear`.
- `takeNative/takeBanner` hand the ad to the activity and null the slot. Token (`cache.token`) invalidates stale loads.
- A preloaded banner is inline adaptive while a `load` banner is anchored adaptive (different size APIs: expect different heights).
- Preload analytics: `native_load/native_fail`, `banner_load/banner_fail` with `ad_unit` (EventBottomAds.java:130, 145, 173, 186).
- No expiry on the bottom ad cache (the back ad cache has 55 min expiry). Unverified: behaviour of a very old cached native.

### 3.2 Back ad (shown on Done / Back)

`AdPlacement.showEventBackAd(activity, key, after)` (1576-1623). Gates in order (any failure -> run `after` = `finish()` with no ad):
1. activity not null / not finishing.
2. `<prefix>_back_ad_show` true and `isNetworkAvailable`.
3. Install-day gate: `<prefix>_back_ad_day_count > 0` AND `getDaysSinceInstall(activity) >= back_ad_day_count`
   (AdPlacement.java:2917: day 1 = install date, stored in prefs `appInstallDate/install_date` on first launch; unreadable date
   returns 1). So `back_ad_day_count` means "start showing on install day N"; 0 or less disables the back ad.
4. `<prefix>_back_ad_total_show_count > 0` (it is shows PER DAY despite the name): minimum interval is
   `24h / total_show_count`. Last time is stored per screen in prefs `event_screen_ad_preferences`, key
   `<screenKey>_back_ad_last_show_time` (screenKey = `charging` / `install_uninstall`). It is stamped BEFORE the ad is shown
   (1613), so a failed ad still consumes the slot. The two screens have independent clocks.
5. Country: `isCountryAllowed(backAdCountryList)` (1897): empty list = everyone. Otherwise the device country (from
   `getDeviceCountry`, AdPlacement.java:2961: IP country only if `ClEnd` IP flag is set and known; else TelephonyManager network
   ISO, SIM ISO, then locale; converted to the ENGLISH display name upper-cased, e.g. "INDIA") must equal a list entry,
   ignoring case. Entries are stored lower-cased (`parseCountryArray` RemoteConfigValues.java:1204). So the list must contain
   country NAMES in English ("India"), not ISO codes. Whether the app-wide ClEnd IP flag also governs this list: yes by code
   (shared `getDeviceCountry`), flag lives in the ClEnd block.
6. `nextEventBackAdType(prefs, key, config, advance=true)` picks the step type; empty -> no ad.

Sequence (`nextEventBackAdType`, 1843-1895; config `parseEventBackAdSequence` RemoteConfigValues.java:1102):
- JSON `<prefix>_back_ad_sequence` = array of `[type, count]` pairs (or strings `"Type,count"`; a bare `"Type"` = count 1).
  Count must be a positive int (number or numeric string).
- Type names (case-insensitive, `normalizeLauncherAppAdType` 2215): `Google_Inter` (also `inter`), `Google_App_Open` (`app_open`,
  `appopen`), `Google_Native` (`fullnative`, `full_native`), `Quiz_Inter`, `Quiz_App_Open`, `Quiz_Native`, `Quiz_Browser`.
  Unknown names are dropped.
- A step is also dropped at runtime if its id is blank: `Google_Inter` needs `<prefix>_inter_id`, `Google_Native` needs
  `<prefix>_full_native_id`, `Google_App_Open` needs the global app-open id (`getAppOpenId()`, the same id used for the splash
  app-open ad; there is no per-screen app-open id key).
- Cursor: persisted counter `<screenKey>_back_ad_sequence_cursor` (SharedPreferences `event_screen_ad_preferences`, long).
  `index = cursor % sum(counts)`; steps expand like `[[Google_Inter,2],[Quiz_Native,1]]` => Inter, Inter, QuizNative, Inter...
  Advanced by 1 each time a back ad is actually attempted (after gates 1-5 pass); the preload peek (`advance=false`) never moves it.
  The cursor resets to 0 whenever the saved sequence text changes (`applyEventScreen` 1084-1097).
- Execution (`executeSequencedAd(..., quizOnFail=false)` 1988-2024): `Google_Inter` -> `loadInterstitialAdInternal(honorQuizPriority=false,
  showLoadingDialog=true)`; `Google_App_Open` -> `loadAppOpenAdInternal(honorQuizPriority=false, showLoadingDialog=true)`;
  `Google_Native` -> `showNativeFullAd` (loading dialog + `presentNativeFullAd` full-screen `native_full_ad_layout` dialog with close);
  `Quiz_*` -> `QuizAds.*`; if the Quiz ad is unavailable, continue (finish). Loading dialog times out after 10 s
  (`INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS`, AdPlacement.java:86), then continues without an ad.
- Failure: `quizOnFail` is passed as false, so a failed/unavailable Google step never turns into a Quiz ad; the screen just closes.
  Quiz priority (`Ad_Priority = QUIZ`) is not honored for Google steps (honorQuizPriority=false); to show Quiz, put a `Quiz_*`
  step in the sequence. In the preloaded path Quiz priority DOES discard the preloaded Google ad (see below), so under
  Quiz priority the Google step is re-requested live (with loading dialog).

Preloaded copy (no loading dialog): `preloadEventBackAd(context, key)` (1672-1789), called from: `loadBottomAd` start, `EventBottomAds.prepare`,
after a back ad finishes (`next` in showEventBackAd 1615-1618), and `onDestroy`. It only preloads when ALL hold:
`back_ad_show`, `ad_load_type == preload`, `back_ad_day_count > 0`, `back_ad_total_show_count > 0`, not Quiz priority, `canRequestAds`,
network, install-day and country gates. It peeks the next step type; Quiz steps and blank ids clear the cache; Google Inter /
App Open / Native are loaded into `EventBackCache` (one slot per screen: type, unitId, loadedAt, token). Same step already
loading or fresh (younger than 55 min, `EVENT_BACK_AD_MAX_AGE_MS`) is not reloaded. At back time, `showPreloadedEventBackAd`
(1792-1839) uses the cache only if its type equals the step type, it is ready, fresh and not Quiz priority; otherwise it is cleared and
the ad is loaded on demand (with loading dialog). A preloaded Native is shown with `presentNativeFullAd`, Inter/AppOpen with
`show(activity)`; `onAdFailedToShowFullScreenContent` just continues (quiz fallback disabled). With `ad_load_type = load` there is NO back ad
preload: every back press loads on demand with the loading dialog. `none` or anything else disables bottom ad and back ad preloading
but the back ad still follows `back_ad_show` and loads on demand.

Analytics for the back ad loads: `interstitial_preload` (preload only), `interstitial_load/fail`, `app_open_ad_load/fail`, `native_load/fail`, all
with `ad_format`/`ad_unit` params (AnalyticsEvents.java:53-66, 92); revenue via `AdPlacement.logAdRevenue` -> Firebase `ad_impression`.

## 4. Remote Config keys

Remote Config source JSON is the app's saved config. `RemoteConfigHelper` passes `root` and `root.Screen` to
`RemoteConfigValues.apply(context, root, screen)` (RemoteConfigHelper.java:269, 281). The blocks are read from
`Screen.charging_screen` and `Screen.install_uninstall_screen`; if `Screen` has no `charging_screen` key but the root has one,
the root is used for BOTH blocks (RemoteConfigValues.java:192-197). Missing block = all defaults (everything off).
Values are persisted to SharedPreferences (prefix `eventCharging` / `eventInstallUninstall`, save 808-837) and restored by `ensureLoaded`, so a cold
process works offline. All keys are read with `optBoolean/optString/optInt` (so `"true"` strings work for booleans via org.json coercion;
exact coercion for strings: unverified).

`<p>` = `charging_screen` or `install_uninstall_screen` (the block key, used as key prefix too).

### charging_screen block (`Screen.charging_screen`)

| Key | Type | Default | Effect |
|---|---|---|---|
| `charge_in_screen_show` | bool | false | Charge-in screen allowed (`isEventScreenEnabled("charge_in")`). |
| `charge_out_screen_show` | bool | false | Charge-out screen allowed. |
| `charging_screen_bottom_ad_show` | bool | false | Master switch for the bottom ad. |
| `charging_screen_ad_load_type` | string | `load` | `Load` / `PreLoad` (case-insens.). Controls bottom ad AND back ad preload; other values disable the bottom ad. |
| `charging_screen_bottom_ad_type` | string | `native` | `banner` = banner; anything else = native. |
| `charging_screen_native_id` | string | "" | Native unit id for the bottom ad (blank = no ad). |
| `charging_screen_banner_id` | string | "" | Banner unit id. |
| `charging_screen_button_show_sec` | int | 6 | Seconds before Done/Back are enabled (<1 -> 6). |
| `charging_screen_back_ad_show` | bool | false | Master switch for the back ad. |
| `charging_screen_back_ad_sequence` | array | [] | `[[type,count],...]` (see 3.2). Empty = no back ad. |
| `charging_screen_inter_id` | string | "" | Unit id for `Google_Inter` steps. |
| `charging_screen_full_native_id` | string | "" | Unit id for `Google_Native` (full screen) steps. |
| `charging_screen_back_ad_day_count` | int | 0 | Show back ad only from install day N (day 1 = install day); 0 = never. |
| `charging_screen_back_ad_total_show_count` | int | 0 | Back ads per day (interval 24h/N); 0 = never. |
| `charging_screen_back_ads_show_country` | array of strings | [] | English country names; empty = all countries. |

### install_uninstall_screen block (`Screen.install_uninstall_screen`)

Same shape; replace the prefix with `install_uninstall_screen_`:

| Key | Type | Default |
|---|---|---|
| `install_screen_show` | bool | false (enables the Install screen) |
| `uninstall_screen_show` | bool | false (enables the Uninstall screen) |
| `install_uninstall_screen_bottom_ad_show` | bool | false |
| `install_uninstall_screen_ad_load_type` | string | `load` |
| `install_uninstall_screen_bottom_ad_type` | string | `native` |
| `install_uninstall_screen_native_id` | string | "" |
| `install_uninstall_screen_banner_id` | string | "" |
| `install_uninstall_screen_button_show_sec` | int | 6 |
| `install_uninstall_screen_back_ad_show` | bool | false |
| `install_uninstall_screen_back_ad_sequence` | array | [] |
| `install_uninstall_screen_inter_id` | string | "" |
| `install_uninstall_screen_full_native_id` | string | "" |
| `install_uninstall_screen_back_ad_day_count` | int | 0 |
| `install_uninstall_screen_back_ad_total_show_count` | int | 0 |
| `install_uninstall_screen_back_ads_show_country` | array | [] |

Note the asymmetry: the four show flags are NOT prefixed (`charge_in_screen_show`, `charge_out_screen_show`,
`install_screen_show`, `uninstall_screen_show`); everything else uses the block prefix.

Related global keys used by these ads (not in the block): app-open unit id (`getAppOpenId`), install date (local prefs),
`Ad_Priority` (only affects the preload skip and preloaded-ad usage), ClEnd country-IP flag (affects `getDeviceCountry`),
AdMob app id / consent (`canRequestAds`).

### Example

```json
{
  "Screen": {
    "charging_screen": {
      "charge_in_screen_show": true,
      "charge_out_screen_show": true,
      "charging_screen_bottom_ad_show": true,
      "charging_screen_ad_load_type": "PreLoad",
      "charging_screen_bottom_ad_type": "native",
      "charging_screen_native_id": "ca-app-pub-xxx/111",
      "charging_screen_banner_id": "ca-app-pub-xxx/112",
      "charging_screen_button_show_sec": 5,
      "charging_screen_back_ad_show": true,
      "charging_screen_back_ad_sequence": [["Google_Inter", 2], ["Google_Native", 1], ["Quiz_Native", 1]],
      "charging_screen_inter_id": "ca-app-pub-xxx/113",
      "charging_screen_full_native_id": "ca-app-pub-xxx/114",
      "charging_screen_back_ad_day_count": 2,
      "charging_screen_back_ad_total_show_count": 6,
      "charging_screen_back_ads_show_country": ["India", "United States"]
    },
    "install_uninstall_screen": {
      "install_screen_show": true,
      "uninstall_screen_show": true,
      "install_uninstall_screen_bottom_ad_show": true,
      "install_uninstall_screen_ad_load_type": "Load",
      "install_uninstall_screen_bottom_ad_type": "banner",
      "install_uninstall_screen_native_id": "ca-app-pub-xxx/121",
      "install_uninstall_screen_banner_id": "ca-app-pub-xxx/122",
      "install_uninstall_screen_button_show_sec": 6,
      "install_uninstall_screen_back_ad_show": true,
      "install_uninstall_screen_back_ad_sequence": [["Google_App_Open", 1], ["Google_Inter", 1]],
      "install_uninstall_screen_inter_id": "ca-app-pub-xxx/123",
      "install_uninstall_screen_full_native_id": "ca-app-pub-xxx/124",
      "install_uninstall_screen_back_ad_day_count": 1,
      "install_uninstall_screen_back_ad_total_show_count": 4,
      "install_uninstall_screen_back_ads_show_country": []
    }
  }
}
```

## 5. Analytics, test matrix, common bugs

### 5.1 Analytics

The event screens log no screen-level or "shown" event of their own (no `AnalyticsEvents`/`TrackOnce` call in
EventPromptActivity, EventPromptLauncher, EventPromptWatch, the two activities). Only ad events exist:
`banner_load/banner_fail`, `native_load/native_fail`, `interstitial_preload/load/fail`, `app_open_ad_load/fail` (via
`AnalyticsEvents.ad(context, event, format, unit)`), plus Firebase `ad_impression` revenue events from `logAdRevenue`.
Analytics for "which kind was shown" is unverified (none found); add one in `EventPromptLauncher.open` if needed.

### 5.2 Test matrix

| Case | Expected |
|---|---|
| App foreground, plug charger | Screen opens by direct `startActivity` (overlay add may throw without permission, caught). |
| App background, overlay granted | Overlay trick, screen opens ~250 ms later. |
| App background, default Home | Direct start. |
| App background, no overlay, not default Home | Nothing happens (showEvent returns at EventPromptLauncher.java:167). |
| Process killed / swiped (Xiaomi, OPPO, vivo, Samsung "sleeping apps") | Runtime receiver is gone: no charging screens. Install/uninstall may still arrive via the manifest `EventPromptReceiver` on some builds: unverified. Need Autostart + "No restrictions" battery. |
| Call ringing/active, or within 5 s of a phone-state change | Suppressed (plus ClEnd visible). |
| Open the app right after plugging in earlier (frozen app wakes) | Dropped by `isHeldBackDelivery` (3.5 s window). |
| Plug, unplug within 1.5 s | Second kind is not debounced against the first (different kind): both open; the second arrives via `onNewIntent`. |
| Same event delivered by manifest + runtime receivers | One screen (1500 ms same-kind debounce). |
| No internet / Wi-Fi without validation | No screen (also checked in `onCreate`). |
| Install a new app / uninstall | Screen. Updating an app (`EXTRA_REPLACING`) and this app's own install do not. |
| HOME pressed while open | Screen closes silently, no back ad. |
| Done pressed before timer | Ignored; Back also ignored. |
| Android 12 (API 31) | Background activity start blocked unless overlay/Home/full-screen-intent; the overlay trick covers it. |
| Android 13 (33) | `POST_NOTIFICATIONS` runtime permission needed for the notification fallback; receiver registered `RECEIVER_EXPORTED`. |
| Android 14 (34) | `USE_FULL_SCREEN_INTENT` is user-revocable for non-call/alarm apps; the helper checks `canUseHighPriorityFullScreenIntent` (unverified result when denied); notification may stay in the shade. |
| Xiaomi / HyperOS | "Display pop-up windows while running in the background" and "Show on lock screen" must be allowed in addition to overlay; otherwise `startActivity` is silently blocked (that case posts no notification because no exception is thrown). |
| Samsung | Put the app in "Never sleeping apps"; Android 12-14 may defer broadcasts for sleeping apps. |

Verify bottom/back ad cases: flags off -> no ad box (placeholder hidden); `PreLoad` + cached -> instant native, no shimmer; `Load` -> shimmer then ad;
back ad day gate (use a device whose `install_date` is old enough), per-day interval (clear `event_screen_ad_preferences` to reset), cursor
(inspect `<key>_back_ad_sequence_cursor`), country list names.

### 5.3 Common bugs / gotchas

- Screen never opens: check `*_show` flag is set in the right block; remember flags are read from SharedPreferences saved by a successful fetch, defaults false.
- Opens late/never in background: missing overlay permission and not default Home; or OEM pop-up permission (no exception, no notification).
- Duplicate screens: debounce only covers same kind within 1.5 s.
- Power events from `PhoneCallStateService` and `EventPromptReceiver` skip the held-back check; only the runtime receiver has it.
- Back ad never shows: `back_ad_day_count` <= 0 or greater than days since install; `total_show_count` 0; no id for the chosen step (step silently dropped); country names given as ISO codes ("IN") instead of English names; empty sequence.
- Back ad appears less often than expected: the last-show time is stamped before the ad loads, so failures/timeouts also consume the slot.
- Sequence order seems to restart: any text change of `*_back_ad_sequence` resets the cursor to 0.
- Quiz not shown on Google failure: by design (no Quiz fallback); use a `Quiz_*` step. Quiz priority is ignored for Google steps and disables the preloaded Google back ad.
- Loading dialog before the back ad: `ad_load_type` is not `preload`, or preload missed (no network at preload time, ad not ready, Quiz priority, step type changed).
- Bottom ad hides itself: failure path just hides `rlEventAd`; banner id/native id blank also hides.
- Charging bottom ad not refreshed on a second plug event while open (`reloadAdsOnNewIntent()` false), Install reloads.
- Battery % can lag by one value right at the plug instant (unverified).
- Older docs in `.claude/skills/install-uninstall-screen-ads` and `.claude/skills/event-screens` may describe app icon/name or other behavior; this file reflects the code (no icon/name, localized text).
