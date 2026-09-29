# Phase 5 — Quiz, App Proxy, Right-Swipe Interstitial, and Process App-Open

Phase 5 only. Phase 6 was not started.

## Source classes and resources inspected

- `common/AdPlacement.java` — quiz banner, native, interstitial, app-open, link open, `Google_Ad_Failed_Show_Quiz`, `refreshActiveQuizLinks`, `matchesAppProxyLocation`, `loadRightSwipeInterstitialAd`, `canShowAppOpenAd`
- `common/IPAddressHelper.java`
- `MyApplication.java` — process lifecycle, app-open load/show, dialog, 4-hour expiry, `shouldSkipAppOpenAd`
- `fragments/MainContainerFragment.java` — pending right-swipe interstitial
- `activities/LauncherHomeActivity.java` — pager swipe marks the pending interstitial
- `helpers/LauncherQuizGames.java`
- `adapters/LauncherAppsAdapter.java` — quiz icon injection
- Quiz layouts: `qz_banner_ad`, `qz_native_small_ad`, `qz_native_medium_ad`, `qz_native_large_ad`, `qz_native_intro_ad`, `qz_native_full_ad`, `qz_interstitial_ad`, `qz_app_open_ad`, shimmer includes
- Rating drawables and `ic_qz_icon_1` through `ic_qz_icon_40`

No source analytics events were found for Quiz, App Proxy, right-swipe, or process App Open. Call-end events such as `CL_END_APP_OPEN_LOAD` belong to Call-End and were not migrated.

## Quiz

Source quiz layouts are shown from the existing launcher ad slots. Compose QR screens were not changed.

- `Ad_Priority` of `QUIZ` shows the quiz layout instead of a Google request.
- `Google_Ad_Failed_Show_Quiz` shows the quiz layout only after a Google load failure.
- Synced item count is the shortest required title/icon list, capped at 3. An empty required list hides the quiz slot and continues.
- Banner uses `Quiz_Banner_Title` / `Quiz_Banner_Description` and `Quiz_App_Icon`.
- Native uses `Quiz_Native_Title` / `Quiz_Native_Description`, `Quiz_Native_Media`, and the matching `qz_native_*` layout.
- Interstitial uses interstitial title, description, media, and rate. Close stays disabled for 5 seconds. Fallback rates are `4.1` through `4.9`. User labels rotate through the source `50K+` / `100K+` / `500K+` / `1M+` strings.
- App-open quiz uses `Quiz_App_Open_Media` and `qz_app_open_ad`.
- `Quiz_Button_Text` replaces the button label when it is not empty.
- Clicks open a random link from the active quiz list with `ACTION_VIEW`. Source tries Custom Tabs first. Custom Tabs is not a target dependency, so the source `ACTION_VIEW` fallback is used.
- Launcher app click types `Quiz_Inter`, `Quiz_App_Open`, `Quiz_Native`, and `Quiz_Browser` follow the same layouts. Browser waits for the launcher activity to resume, then continues.
- App drawer injection uses `LauncherApp_Quiz_Icon_Show` and `LauncherApp_Quiz_Icon_Count`, up to 40 source game icons. Clicks open that game URL.

Remote Config keys are read from the Phase 4 `RemoteConfigValues` store. They were not parsed a second time.

## App Proxy

`AppProxyLookup.refreshActiveQuizLinks()` runs after the stored values are applied. It does not block launcher startup.

- `App_Proxy_Check_Ip` false: the active list is `Quiz_Link_List`.
- Check true: the active list starts as `Quiz_Link_List`. If `App_Proxy_Ip_Checker_Url` is empty, no request is made.
- Otherwise a background GET uses that URL. Connect and read timeouts are 10 seconds. JSON must have `status=success`. City, state, and country come from `city`, `regionName`, and `country`.
- A city, state, or country match (case-insensitive, any list) switches the active list to `Quiz_Link_List_Exclude` when that list is not empty. Otherwise the normal quiz list stays.
- HTTP errors, a non-success status, and other network failures keep `Quiz_Link_List`.

`IPAddressHelper` still contains the source fallback URL, including its embedded key. The quiz refresh does not call that fallback. It returns when the Remote Config URL is empty, matching `refreshActiveQuizLinks`.

## Right-swipe interstitial

The source second-app page was not migrated. The equivalent action is `LauncherHomeActivity.openQrShell()`, which opens the existing Compose `MainActivity`.

- `Right_Swipe_Interstitial_Ad_Show` false continues straight to the QR app.
- The counter starts at 1. When `Right_Swipe_Interstitial` is greater than 0, the interstitial is requested only when the counter equals that threshold, then the counter resets to 1. Other swipes increment the counter and open the QR app with no ad.
- A threshold of 0 requests an interstitial on every right-swipe into the QR app.
- The unit id is the existing `Other_Interstitial_Id`. Quiz priority shows the quiz interstitial instead.
- Flag off, empty id, missing AdMob app id, load failure, show failure, or a missing quiz list continues into `MainActivity`. The loading dialog uses the existing 10-second timeout and then continues. The launcher activity is not finished by the ad.

Scan, Create, History, and Settings shortcuts do not use this interstitial.

## Process App Open

`ProcessAppOpen` is registered from `QrScannerApplication`. It tracks the current activity and observes process start/stop.

Source `shouldSkipAppOpenAd()` clears any loaded ad and returns true. That method was copied as written. Because of it, `fetchAd`, `showAdIfAvailable`, and `onStart` return immediately. The process app-open ad does not load or show.

The surrounding source conditions are present and unreachable while that skip stays true:

- Skip splash/current activity and launcher home before show
- `App_Open_Ad_Show` and `App_Open_Id`
- `App_Open_Dialog_Show` waits 1500 ms on `dialog_loading_ads` before show
- `App_Open_Show_Per_Day`: 0 means no daily spacing; otherwise the day is divided by that count
- Loaded ads expire after 4 hours
- Show failure clears the ad and does not block the current screen

This does not add a second app-open on splash, onboarding, permission, Default Home, or QR screens, because the source skip prevents a show.

## Failure behavior

A false flag, empty id, missing AdMob configuration, load failure, show failure, or quiz priority with an empty quiz list continues navigation. No new ad unit ids were added.

## Analytics

Not found in source for Quiz, App Proxy, right-swipe, or process App Open. No new event names were added.

## Build and tests

- `:app:assembleDebug` — BUILD SUCCESSFUL
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL, 12 suites, 67 tests, 0 failures, 0 errors

## Device verification

Not performed. No device or emulator run was done for this phase.

## Intentionally pending (Phase 6+)

- Launcher sub page / weather
- Settings shortcut
- Default Home popup and setup preferences
- Call-End and `PhoneCallStateService`
- FCM, reminders, `BootReceiver`, widgets
- Crashlytics, Facebook mediation, ProGuard migration
- Source right-swipe second-app UI (replaced by the Compose QR app)
