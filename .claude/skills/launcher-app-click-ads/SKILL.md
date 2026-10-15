---
name: launcher-app-click-ads
description: Build, modify, debug or port the launcher "app click ad" (LauncherApp_Click_Ad_Show / LauncherApp_Count / LauncherApp_Ad_Type) and the "return ad" (LauncherApp_Back_Click_Ad_Show / LauncherApp_Back_Count / LauncherApp_Back_Ad_Type) of the launcher home screen and app drawer - an ad shown every Nth tap on an app icon before the app opens, and an ad shown every Nth time the user comes back to the launcher from an external app. Covers the counters, the round-robin ad-type sequence (Google Inter / App Open / Native, Quiz Inter / App Open / Native / Browser), the Quiz fallback when Google fails, how "returned from an external app" is detected and persisted (SharedPreferences, restored after process death), the Remote Config keys, and where the hooks go in the adapter, home fragment and launcher activity. Use when porting these ads to another launcher app, changing their frequency / types, or fixing an ad that never shows, shows on every tap, or shows after a cold start.
---

# Launcher app click ad + return (back) ad

Reference implementation: QR Code & Scanner, package `com.qrcode.scanner`, `launcher/`. Source is copied under `assets/`. Everything lives in `AdPlacement` plus four hook points.

Two independent ads, same ad machinery:

| | **Click ad** | **Return / back ad** |
|---|---|---|
| Moment | User taps an app icon (drawer, dock icons: dialer, SMS, browser, camera). The ad runs **first**, the app opens when the ad closes (or fails). | User comes **back** to the launcher home after using an external app (Back / Home gesture / recents). |
| Master switch | `LauncherApp_Click_Ad_Show` | `LauncherApp_Back_Click_Ad_Show` |
| Frequency | `LauncherApp_Count` = every Nth tap (`<= 0` = off) | `LauncherApp_Back_Count` = every Nth return (`<= 0` = off) |
| Ad types | `LauncherApp_Ad_Type` | `LauncherApp_Back_Ad_Type` |
| Interstitial id | `LauncherApp_Interstitial_Id` | `LauncherApp_Back_Interstitial_Id` |
| Native id (both) | `LauncherApp_Native_Id` | same |

(`LauncherApp_Native_List_*` is the drawer list ad: see `drawer-native-ads`. `LauncherApp_Native_Ad_*` / `Quiz_Icon` keys are other features.)

## Click ad: `AdPlacement.handleLauncherAppClickAd(activity, openSelectedApp)`

```
open = null                      -> nothing
activity null/finishing, switch off, Count <= 0
                                  -> open.run() at once
launcherAppClickCount == Count    -> reset counter to 1, run the ad, open.run() when the ad is done
else                              -> launcherAppClickCount++, open.run()
```

- The counter `launcherAppClickCount` is a static field starting at **1**: with `LauncherApp_Count = 3` the 3rd, 6th, 9th ... tap shows the ad. It is in memory only (reset on process start).
- The ad type is the next item of the `LauncherApp_Ad_Type` list (`nextLauncherAppAdType(false)`, round robin with `launcherAppAdSequenceIndex`). A single string is accepted too (`readAdTypeSequence`). Unknown names are dropped when the list is set; an empty list = no ad, app just opens.
- Hook points:
  - `LauncherAppsAdapter` app item click: `AdPlacement.handleLauncherAppClickAd((Activity) context, () -> launchApp(packageName))` (drawer / grid).
  - `LauncherHomeFragment.runWithLauncherClickAd(action)` wraps the four dock icons (`launchDialerApp`, `launchSmsApp`, `launchBrowserApp`, `launchCameraApp`).
  - Do **not** put the ad inside `launchApp` itself, or the ad would run twice for one tap.

## Return ad: `markLauncherExternalAppLaunched` -> `handleLauncherAppReturnAd`

```
app really launched (startActivity ok)
  AdPlacement.markLauncherExternalAppLaunched(context, packageName)
     prefs "launcher_app_back_ad": waitingForLauncherAppReturn = true, lastLaunchedPackage = pkg   (persisted)

LauncherHomeActivity.onCreate, savedInstanceState == null   (fresh launch, e.g. removed from recents)
  AdPlacement.clearLauncherAppReturnPending(this)    <- a stale mark must not show an ad after a cold start

LauncherHomeActivity.onResume   (after the "just became default" hand-over check)
  AdPlacement.handleLauncherAppReturnAd(this)
     not LauncherHomeActivity / finishing                -> stop
     waitingForLauncherAppReturn false                   -> stop
     clear the flag; lastLaunchedPackage empty           -> stop
     restoreLauncherBackConfig (config saved in prefs)   -> back switch off or Count <= 0 -> stop
     showJoinedLauncherBackAd(activity, no-op)
```

`showJoinedLauncherBackAd`: restores the saved config; `returnCount = prefs.launcherAppReturnCount + 1`; if `returnCount >= LauncherApp_Back_Count` -> reset to 0 and show the ad, else just store the count. Ad type = next item of the back list; `executeLauncherAppAd(activity, done, type, backInterstitialId)`. The return counter is **persisted** (survives restarts), unlike the click counter.

Why the config is persisted (`persistLauncherBackConfig`, called right after Remote Config is applied, `restoreLauncherBackConfig` on use): when the user returns, the process may have been killed and Remote Config may not be fetched yet when `onResume` runs; the saved switch / count / sequence / id keep the ad working. `launcherBackConfigRestored` makes the restore happen once per process, and only if no sequence is set yet.

The launch must be marked in **every** place that opens an external app: `LauncherAppsAdapter.launchApp` (after `startActivity`), `LauncherHomeFragment` helper that starts dock/shortcut intents (after `startActivity`; package from `getPackage()` / component, or the intent action when neither exists). Mark only after a successful start (inside the try), otherwise a failed launch would count as a return.

## Running the ad: `executeLauncherAppAd` -> `executeSequencedAd(activity, continueAction, type, interstitialId, nativeId, quizOnFail)`

| Type (normalized) | Action |
|---|---|
| `Google_Inter` | `loadInterstitialAdInternal(activity, interstitialId, continue, true, false, quizOnFail)` |
| `Google_App_Open` | `loadAppOpenAdInternal(activity, appOpenId, continue, ..., quizOnFail, true)` |
| `Google_Native` | `showNativeFullAd`: loading dialog, `AdLoader` for `LauncherApp_Native_Id`, then `Theme_NativeFullAd` dialog with `native_full_ad_layout` and a close button; continues on dismiss / failure / timeout |
| `Quiz_Inter` / `Quiz_App_Open` / `Quiz_Native` | `QuizAds.showInterstitial / showAppOpen / showNativeFull`, continue at once when no quiz ad is available |
| `Quiz_Browser` | `QuizAds.openBrowserThenContinue` |
| anything else | `continueAction.run()` |

`quizOnFail` = `launcherQuizOnGoogleFail()` = `LauncherApp Google_Ad_Failed_Show_Quiz` (falls back to the root `Google_Ad_Failed_Show_Quiz`): when a Google ad cannot load, show a Quiz ad instead. `normalizeLauncherAppAdType` accepts `Google_Inter | inter`, `Google_App_Open | app_open | appopen`, `Google_Native | fullnative | full_native`, `Quiz_*` (case-insensitive). Every path must end by calling `continueAction` exactly once (the native path uses an `AtomicBoolean` guard).

## Remote Config (`Screen.LauncherAppScreen`, read in `RemoteConfigHelper`)

| Key | Type | Meaning |
|---|---|---|
| `LauncherApp_Click_Ad_Show` | bool | click ad switch |
| `LauncherApp_Count` | int | every Nth tap |
| `LauncherApp_Ad_Type` | string or array | e.g. `["Google_Inter","Google_Native","Quiz_Inter"]` |
| `LauncherApp_Interstitial_Id` | string | inter unit id for the click ad |
| `LauncherApp_Native_Id` | string | native unit id for `Google_Native` (both ads) |
| `LauncherApp_Back_Click_Ad_Show` | bool | return ad switch |
| `LauncherApp_Back_Count` | int | every Nth return |
| `LauncherApp_Back_Ad_Type` | string or array | same item names |
| `LauncherApp_Back_Interstitial_Id` | string | inter unit id for the return ad |
| `Google_Ad_Failed_Show_Quiz` | bool | quiz fallback (screen or root level) |

Example: `assets/snippets/remote_config.json`.

## Files in `assets/`

| Asset | Goes to |
|---|---|
| `snippets/AdPlacement.launcherClickBack.fields.snippet.java` | fields + constants to add to `AdPlacement` |
| `snippets/AdPlacement.launcherClickBack.snippet.java` | getters / setters, persist / restore, click + return logic, sequence, executor, native full ad, normalizer (copy into `AdPlacement`) |
| `snippets/RemoteConfigHelper.launcherClickBack.snippet.java` | key parsing, `readAdTypeSequence`, `persistLauncherBackConfig` call |
| `snippets/LauncherAppsAdapter.hooks.snippet.java`, `LauncherHomeFragment.hooks.snippet.java`, `LauncherHomeActivity.hooks.snippet.java` | the exact hook lines |
| `snippets/remote_config.json` | example |
| `java/LauncherAppsAdapter.java`, `LauncherHomeFragment.java`, `LauncherHomeActivity.java` | full reference files (large; read for context only, do not copy wholesale) |

Needed from the app: `QuizAds`, `loadInterstitialAdInternal`, `loadAppOpenAdInternal`, `showInterstitialLoadingDialog`, `populateNativeAdView`, `AdTheme`, `canRequestAds` (consent), `isNetworkAvailable`, layout `native_full_ad_layout`, style `Theme_NativeFullAd`.

## Porting checklist

1. Add the fields and methods to your `AdPlacement`; add the Remote Config parsing and the `persistLauncherBackConfig(context)` call after the config is applied.
2. Click ad: wrap the icon click(s) with `handleLauncherAppClickAd`; keep the actual launch in a separate method.
3. Return ad: call `markLauncherExternalAppLaunched` after each successful external start; in the launcher Activity call `clearLauncherAppReturnPending` on a fresh create and `handleLauncherAppReturnAd(this)` in `onResume`. The Activity type check in `handleLauncherAppReturnAd` must match your launcher activity class.
4. Test: Count 1 (every tap), Count 3 (counter 1-2-3), types in order, ad closes -> app opens, no internet (app opens at once), Google fails with Quiz fallback on / off, return after Back, return after Home, kill the launcher process while the app is open (return ad still works from saved config), swipe the launcher out of recents then open it (no ad), return counter across restarts, default-Home hand-over screen (no return ad right after it).
