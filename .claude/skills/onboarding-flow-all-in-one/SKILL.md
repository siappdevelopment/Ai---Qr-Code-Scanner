---
name: onboarding-flow-all-in-one
description: Build, modify, debug or port the WHOLE startup and onboarding flow of this launcher app in one go - Splash (VPN check, Remote Config fetch, splash ad, notification permission, after-splash inter / app open) then the Remote Config driven screen list Show_Screen_Flow (Language, Collection = PolicyScreen, Permission, DefaultHome, DefultSettingHome, Intro = OnBoarding x3) and finally Home (LauncherHomeActivity or the QR app). Covers screen order and aliases, completion flags and resume rules, back-button behaviour, every ad slot per screen and its Remote Config key, Firebase (Remote Config, Analytics events, FCM, Crashlytics, install referrer, paid / normal profile), manifest, layouts, test matrix and debugging. Use when porting the full Splash-to-Home flow to another app, changing the order or the screens, wiring ads or Firebase into it, or fixing a flow that skips, repeats or stalls a screen.
---

# Splash -> onboarding -> Home (all in one)

Reference implementation: QR Code & Scanner, package `com.qrcode.scanner`, source under `launcher/`. All flow classes, layouts and snippets are copied under `assets/`. The detailed per-step skills (listed at the end) go deeper on one step; this skill is the map that ties them together.

## The flow in one picture

```
Launcher icon -> MainActivity (Compose SplashScreen)           <- StartupFlow
   1. VPN check (dialog + Recheck, blocks until VPN is off)
   2. InstallReferrerStartup + RemoteConfigHelper.fetchRemoteConfig   (Firebase)
   3. splash bottom ad (native or banner)  + AdPlacement.saveInstallDate
   4. POST_NOTIFICATIONS request (Android 13+)
   5. wait Splash_Duration seconds
   6. after-splash ad (inter or app open)  -> StartupNavigation.continueAfterVisibleSplash
        |
        |- onboarding NOT finished -> ScreenFlowNavigation.openFirstScreen
        |        Show_Screen_Flow = ["Language","Collection","Permission","DefultSettingHome","Intro"]
        |        each screen calls ScreenFlowNavigation.continueAfter(this, <its name>) -> next incomplete screen
        |- onboarding finished     -> stay in QR app, or openMain (Launcher Home) when default Home / Redirect_Home_Launcher
        v
   Home = LauncherHomeActivity (default Home app / redirect)   or   MainActivity QR app (ScanPulseNavHost)
```

## Screen names (Remote Config) -> screens

`ScreenFlowConfig.normalizeScreenName` is the only place that maps names. Matching is case-insensitive, duplicates are dropped, unknown names are skipped (`isSupportedScreen`).

| Name in `Show_Screen_Flow` | Accepted aliases | Activity | Done flag (`AppUtils`) |
|---|---|---|---|
| `Language` | - | `LanguageActivity` | `getLanguageFlowCompleted` |
| `Collection` | **not** `PolicyScreen` by default - add the alias (`assets/snippets/ScreenFlowConfig.alias.snippet.java`) | `CollectionActivity` (privacy agree) | `isCollectionScreenCompleted` |
| `Permission` | - | `PermissionActivity` | `isPermissionScreenCompleted` |
| `DefaultHome` | `Default`, `DefaultApp`, `Default_Home` | `DefaultActivity` (own screen + RoleManager dialog) | `isDefaultHomeScreenCompleted` or already default |
| `DefultSettingHome` (typo on purpose) | `DefaultSettingHome` | `DefaultSettingHomeActivity` (no UI, system Home settings) | `isDefaultSettingHomeScreenCompleted` or already default |
| `Intro` | `Onboarding`, `OnBoarding`, `IntroScreen`, `IntroActivity` | `Intro1Activity` -> `Intro2Activity` -> `Intro3Activity` | `getIntroCompleted` |

The user's wanted order maps to this app as:

```json
"Show_Screen_Flow": ["Language", "Collection", "Permission", "DefultSettingHome", "Intro"]
```
(`PolicyScreen` -> `Collection`, `OnBoarding` -> `Intro`). **Gotchas:** the code reads the key **`Show_Screen_Flow`** (inside the profile JSON, or as its own top-level Remote Config parameter), not `show_intro_screen_flow`; and `PolicyScreen` is skipped silently until the alias is added. `Home` is not a list entry: when the list is finished the flow opens Home itself. Default list when Remote Config is empty or offline: `Language, Collection, Permission, DefaultHome, Intro` (also in `res/xml/default_config.xml`).

## Navigation engine (`ScreenFlowNavigation` + `ScreenFlowConfig` + `AppUtils`)

- `openFirstScreen` -> `ensureFlow` (restore the cached list, else defaults) -> if `hasCompletedOnboarding` (= `Intro` done) open Home, else `openFromIndex(0)`.
- `continueAfter(activity, doneScreen)`: marks the screen done, clears completions of later screens, `advanceFlowProgress(index+1)`, then `openFromIndex(index+1)`. The "furthest progress" (`flow_progress_index`) stops a late or duplicate callback of an earlier screen from clearing screens already passed.
- `findNextIncomplete` skips unsupported names and screens whose flag is already true (so a screen that is satisfied, e.g. already default Home, is never shown).
- `openScreen`: `NEW_TASK | CLEAR_TASK`, then finishes the caller. **Exception:** `DefultSettingHome` is started with `NO_ANIMATION`, no CLEAR_TASK and no finish, so nothing blinks before the system page.
- No next screen -> `openMain`: `buildFinalDestinationIntent` = Launcher Home when the app is default Home or `Redirect_Home_Launcher`, else `MainActivity` with `EXTRA_SKIP_STARTUP_SPLASH`. `FINAL_NAVIGATION_DEBOUNCE_MS = 1000` blocks a double open.
- Flow changed remotely: `cacheShowScreenFlow` compares the new list with the cached one; if different and onboarding is not finished, `resetOnboardingScreenCompletions` restarts the flow.
- Intro count `Intro_Screen_Count` (0..3, capped by `MAX_INTRO_BUTTON_SCREENS`). `0` means the Intro step completes at once.
- **Back button rule everywhere:** onboarding Back never goes to the previous screen - it does what the main button does (Language: Done, Collection: Agree & Continue, Permission: Allow, Default: Set as default, Intro: Next).

## Step 0 - Splash (`StartupFlow`, `SplashScreen.kt`, `MainActivity.kt`)

- `MainActivity` is the launcher activity. `StartupFlow.shouldShowSplash(intent)` is true for a MAIN/LAUNCHER start, a widget target, or a reminder notification; false when `EXTRA_SKIP_STARTUP_SPLASH` is set (return from the flow). The Compose `SplashScreen` shows logo, name and progress, and hosts the ad container `layout_splash_startup_ads` through `AndroidView`; `onAdRootReady` hands that view to `StartupFlow.begin`.
- `begin`: VPN active -> `dialog_vpn_detect` (Recheck, not cancelable); else `startSplashFlow` (runs once, guarded by `splashFlowStarted` / `generation` so rotation or a second host cannot repeat it).
- `startSplashFlow`: `InstallReferrerStartup.start`, then `RemoteConfigHelper.fetchRemoteConfig` -> callback: `showSplashAd`, `AdPlacement.saveInstallDate` (install-day ad gating), `checkNotificationPermission`.
- `showSplashAd`: hidden when offline or `Splash_Ad_Show` false; `Splash_Ad_Type` `banner` -> `loadBannerAd(Splash_Banner_Id)`, else small native (`Splash_Native_Id`).
- `checkNotificationPermission`: Android 13+ requests `POST_NOTIFICATIONS` (request code 101, result forwarded from `MainActivity.onRequestPermissionsResult`; granted is tracked once as `POST_NOTIFICATIONS_GRANTED`). Then `goNextScreen`.
- `goNextScreen`: delay `Splash_Duration` s -> `showAfterSplashAd`: `After_Splash_Ad_Show` false -> finish; `After_Splash_Ad_Type` `inter` -> `loadAfterSplashInterstitialAd(After_Splash_Interstitial_Id)`, else `loadAfterSplashAppOpenAd(App_Open_Id)`. **Every path must call `deliverFinished` once** (ad failure, empty id, disabled placement all continue). `deliverFinished` -> the `onFinished` runnable set by the NavHost -> `StartupNavigation.continueAfterVisibleSplash`.
- `continueAfterVisibleSplash`: reminder-notification entry first (`DefaultLauncherReminder.handleSplashEntry`); onboarding not done -> `openFirstScreen`; done and not default Home / not redirected -> stay in the QR app; else `openMain`.
- Analytics: `splash_view` on first composition, `main_view` in `MainActivity.onCreate`.

## Step 1 - Language (`LanguageActivity`, `LanguageAdapter`, `activity_language`)

- 19 languages (`en hi ru it fr es ja ko de zh th el pt pt-BR nl fil tr id af`) with flag icons, native sub-names and a radio selection. Selected index = `AppUtils.getLanguage`.
- Opened from the flow with `EXTRA_LANGUAGE_FLOW_STARTING`; opened from Settings it shows a back arrow and returns `RESULT_OK` when the language changed (`isOpenedFromSettings`).
- Done / Back: apply the language first (`AppUtils.applyLanguage`, so an ad can never block it), then `Language_Interstitial_Ad_Show` ? `loadLanguageInterstitialAd` : go on. -> `continueAfter(SCREEN_LANGUAGE)`. `languageFlowInProgress` blocks double taps.
- Ad: `Language_Ad_Show` / `Language_Ad_Type` (`native` big, or `banner` adaptive with the Quiz big-native fallback) / `Language_Native_Id` / `Language_Banner_Id`. Quiz priority (`shouldUseQuizPriority`) always shows the big Quiz native. `preloadOnboardingInterstitialAd(this, false)` in `onCreate` when `inter_ads_click_type` is `Preload`.
- Analytics: `language_view`.

## Step 2 - Collection = PolicyScreen (`CollectionActivity`, `activity_collection`)

- Checkbox (`ivSelect`) + footer text with a clickable Privacy Policy span -> `AppUtils.openPrivacyPolicy` opens `Privacy_Policy` (Remote Config string; toast "Privacy Policy Not Found !" when empty). State survives rotation (`key_agreed`).
- Agree & Continue (also Back) requires the checkbox, else toast `collection_agree_toast`; then `continueAfter(SCREEN_COLLECTION)`. No ad slot on this screen.

## Step 3 - Permission (`PermissionActivity`, `OverlayPermissionGuideActivity`, `activity_permission`)

Allow (or Back) starts a chain; a denial of any step just continues:
1. `READ_PHONE_STATE` -> 2. `CAMERA` -> 3. `POST_NOTIFICATIONS` (Android 13+, only if Splash did not get it) -> 4. **Display over other apps**: `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` for the package, skipped when already granted or when the app is the default Home app. While Settings is open: the guide card (`OverlayPermissionGuideActivity.show`, started in `onPause`) sits on top, and a 400 ms poller (`overlayPermissionMonitorRunnable`) brings the user back as soon as `Settings.canDrawOverlays` is true. Step flags are saved in `onSaveInstanceState`, so a process restart resumes the chain.
- Already satisfied on entry (phone + camera + notification + overlay-or-default-home) -> `completePermissionScreenAndNavigate` at once. Done -> grant events once (`trackPhoneStatePermissionGrantedOnce` etc.) -> `continueAfter(SCREEN_PERMISSION)`.
- Ad: shares the `PermissionDefaultScreen` block (`PermissionDefault_Ad_Show/Type/Banner_Id/Native_Id`, small).
- Analytics: `permission_view`. Details: `overlay-permission-flow` skill.

## Step 4a - DefaultHome (`DefaultActivity`, `DefaultHomePromptHelper`, `activity_default`)

- Always shows its own screen first; the role request starts from **Set as default** (or Back). Order: `READ_PHONE_STATE` first, then `RoleManager` `ROLE_HOME` through `DefaultHomePromptHelper` (suppressed dialog falls back to the system Home settings).
- Shared flags (`AppUtils`): `setCompletingDefaultAppSetup`, `setDefaultSetupStep(SCREEN_DEFAULT_HOME)`, `setAwaitingDefaultRoleResult`. After the system makes this app Home it restarts `LauncherHomeActivity`, which sees `isCompletingDefaultAppSetup && isDefaultHomeApp` -> `completeAfterDefaultSetup` -> `navigateAfterDefaultAppSetup` (marks the step that started it, tracks `DEFAULT_HOME_APP_SET` / `DEFAULT_HOME_APP_CANCEL`, shows the after-default ad, `continueAfter(startedBy)`). `tryClaimAfterDefaultFlow` makes sure only one of {LauncherHome, DefaultActivity} continues the flow.
- Ads: bottom slot = `PermissionDefaultScreen` block; after Allow / Cancel `loadAfterDefaultAd` (`After_Default_Ad_Show`, `After_Default_Ad_Type` inter | app open; unit = `Other_Interstitial_Id` or `App_Open_Id`; skipped under Quiz priority), preloaded by `preloadAfterDefaultAd` in `setupDefaultScreen`.
- Also used as the entry of the "make this app your default launcher" reminder (`EXTRA_FROM_REMINDER`): Back/Allow then go straight to Home. Skills: `default-home-role-flow`, `default-home-screen`, `default-launcher-reminder`.

## Step 4b - DefultSettingHome (`DefaultSettingHomeActivity`, `DefaultHomeGuideActivity`)

- No layout. `onCreate`: already default or no settings screen -> continue; else set the same three flags (step = `DefultSettingHome`), launch `Settings.ACTION_HOME_SETTINGS` (fallback `ACTION_MANAGE_DEFAULT_APPS_SETTINGS`), start the home watcher.
- `onPause` starts `DefaultHomeGuideActivity` (own task, bottom card with an animated hand, closes on touch or when default; re-shows itself above the Settings list). Result callback or a changed Home package (500 ms watch; background start is rolled back after 2 s if Android blocks it) -> `continueFlow()` -> `continueAfter(SCREEN_DEFAULT_SETTING_HOME)`; `flowContinued` makes it run once.
- No ad of its own (the after-default ad runs through `navigateAfterDefaultAppSetup` when the app becomes Home). Skill: `default-setting-home-screen`. Use **either** `DefaultHome` or `DefultSettingHome` (or both) in the list.

## Step 5 - Intro = OnBoarding (`Intro1/2/3Activity`, `IntroNavigation`)

- Up to 3 pages (`Intro_Screen_Count`), each with a dot indicator (`setupIntroButtonIndicators`: active dot 18 dp, inactive 6 dp) and a Next button; on the last page of a 2-3 page flow Next moves to the top right (`moveNextButtonToTopIfLast`). Next/Back -> `goToNextIntroButtonScreen` -> next page or, after the last, `loadIntroInterstitialAd` (`Intro_Interstitial_Ad_Show`) -> `continueAfter(SCREEN_INTRO)`. Finishing Intro = onboarding finished (`hasCompletedOnboarding`), then Home opens.
- Ad: `IntroScreen` block (`Intro_Ad_Show/Type/Banner_Id/Native_Id`), slot size `intro`. `preloadOnboardingInterstitialAd(this, true)` on page 1.
- Analytics: `intro_1_view`, `intro_2_view`, `intro_3_view`.

## Step 6 - Home

`openMain` -> `LauncherHomeActivity` (3-page pager: QR page, Home, sub page) when this app is default Home or `Redirect_Home_Launcher`; otherwise the Compose QR app (`ScanPulseNavHost`, bottom bar, `HomeScreen`). Analytics: `launcher_home_view` / `main_view`. After Home, the other features apply (see index).

## Firebase

| Piece | Where | Notes |
|---|---|---|
| Remote Config | `RemoteConfigHelper.fetchRemoteConfig` | Started in `QrScannerApplication.onCreate` **and** joined by Splash (callbacks are queued, not dropped). Key: `"QRScanner_" + versionCode + BuildConfig.RC_KEY_SUFFIX` (`""` release, `"_qa"` QA). `minimumFetchInterval` 0, defaults from `res/xml/default_config.xml`. Value is a JSON object (or an array whose first element is the object). Profile switch: root keys `paid_user` / `normal_user` (selected from the stored install referrer: `marketing_converter1`, `utm_source_apps_facebook_com`, `utm_source_apps_instagram_com`, `utm_source_marketing`); the value may be an inline object, a nested key name or a JSON string. |
| Offline / failure | same | On empty value, bad JSON or a failed fetch the cached list (`restoreShowScreenFlow`) or the default list is used. `watchNetwork` refetches when the network returns; `refreshIfDue` refetches after 12 h. |
| Parsed fields | `applyScreenConfig` | `Show_Screen_Flow`, `Privacy_Policy`, `Redirect_Home_Launcher`, `Screen.*` ad blocks (below), `launcher_home_screen.launcher_notification_push_*`, then `RemoteConfigValues.apply` for the rest (call end, event screens, drawer, quiz). |
| Analytics | `AnalyticsEvents.log(context, event, k, v...)` | Adds `platform` and `app_version`. Screen events: `splash_view`, `language_view`, `permission_view`, `intro_1_view` .. `intro_3_view`, `main_view`, `launcher_home_view`; ad events `banner/native/interstitial/app_open_ad` `_load` / `_fail`, `interstitial_preload`. `TrackOnce.trackScreenOnce` sends one-time events (`POST_NOTIFICATIONS_GRANTED`, `DEFAULT_HOME_APP_SET`, `DEFAULT_HOME_APP_CANCEL`); `AppUtils.track*PermissionGrantedOnce` for phone / camera / overlay. |
| Messaging | `MyFirebaseMessagingService` | `handleIntent` only acts when notifications are allowed, the app is in the background, the phone is locked and the call-end config allows it: it preloads the call-end full native ad, then opens the Call End screen. Normal notifications go to `super.handleIntent`. See `callend-activity`. |
| Crashlytics / Google services | `build.gradle.kts` | `google-services` and `crashlytics` plugins apply only if `app/google-services.json` exists. Dependencies: firebase-bom, config, analytics, messaging, crashlytics; also shimmer, installreferrer, play-services-ads. |
| Install referrer | `InstallReferrerStartup.start` (splash) | Stored referrer decides `paid_user` vs `normal_user`. |

## Ads per screen (Remote Config `Screen.*`)

| Screen | Slot | Keys |
|---|---|---|
| Splash | bottom native (small) or banner | `SplashScreen.Splash_Ad_Show`, `Splash_Ad_Type`, `Splash_Native_Id`, `Splash_Banner_Id`, `Splash_Duration` |
| After splash | full screen inter or app open | `After_Splash_Ad_Show`, `After_Splash_Ad_Type`, `After_Splash_Interstitial_Id`, root `App_Open_Id` |
| Language | big native / adaptive banner (+ optional inter on Done) | `LanguageScreen.Language_*`, `Language_Interstitial_Ad_Show` |
| Collection | none | - |
| Permission and DefaultHome | small native / banner | `PermissionDefaultScreen.PermissionDefault_*` |
| After default | inter or app open after Allow / Cancel | `After_Default_Ad_Show`, `After_Default_Ad_Type` (also read as `After_default_*`) |
| Intro 1..3 | `intro`-size native / banner (+ optional inter after the last page) | `IntroScreen.Intro_*`, `Intro_Interstitial_Ad_Show` |
| Shared inter id | Language, Intro, After default | `OtherScreen.Other_Interstitial_Id` -> else root `Other_Interstitial_Id` -> else `inter_ads.inter_ads_id` |
| Global | priority and look | `Ad_Priority`, `Google_Ad_Failed_Show_Quiz`, `Native_Ad_*_Color*`, `inter_ads_click_type` (`Preload` = load ahead) |

Rules for every slot: `AdPlacement.showSlot(...)` hides the container when offline or the flag is off; a missing id, a load failure or an off flag must still continue the flow; the next screen opens only in the ad's finish callback and exactly once; no ad on `DefultSettingHome`; use `AdPlacement.onboardingNativeColor` so native ads match the onboarding background. Post-onboarding ads (Home, drawer, app click, right swipe, call end, event screens) are in their own skills.

## Application init (`QrScannerApplication.onCreate`, order matters)

`restoreSavedLanguage` -> apply stored night mode -> `RemoteConfigValues.ensureLoaded` (cached values, so a cold phone-state broadcast works) -> `EventPromptWatch.register` -> `AdPlacement.initializeIfConfigured` (AdMob only when an app id exists) -> `ProcessAppOpen.register` -> `RemoteConfigHelper.fetchRemoteConfig(this, null)` -> `watchNetwork`.

## Files in `assets/`

| Asset | Goes to |
|---|---|
| `java/StartupFlow, StartupNavigation, ScreenFlowNavigation, IntroNavigation, AnalyticsEvents, TrackOnce, InstallReferrerStartup` | `launcher/common/` |
| `java/ScreenFlowConfig, RemoteConfigHelper` | `launcher/remote/` |
| `java/Language/Collection/Permission/Default/DefaultSettingHome/DefaultHomeGuide/OverlayPermissionGuide/Intro1-3 Activity` | `launcher/activities/` |
| `java/LanguageAdapter` | `launcher/adapters/` |
| `java/DefaultHomePromptHelper, DefaultHomePopupHost` | `launcher/helpers/` |
| `java/MyFirebaseMessagingService` | `launcher/services/` |
| `java/QrScannerApplication` | app root |
| `kotlin/MainActivity.kt`, `kotlin/SplashScreen.kt` | app root, `ui/screens/splash/` |
| `res/layout/*` | `res/layout/` (language, collection, permission, default, intro 1-3, both overlay guides, splash ads + shimmer, vpn dialog) |
| `res/xml/default_config.xml` | `res/xml/` |
| `snippets/remote_config.json` | Firebase parameter value (full example for the flow + every ad block) |
| `snippets/manifest.onboarding.xml.snippet` | `AndroidManifest.xml` |
| `snippets/ScreenFlowConfig.alias.snippet.java` | adds the `PolicyScreen` alias / `show_intro_screen_flow` key |

Not copied because they are big and shared: `AdPlacement` (ads engine, ~3000 lines), `RemoteConfigValues`, `AppUtils` (flow flags: `getFlowProgress`, `advanceFlowProgress`, `resetOnboardingScreenCompletions`, `*Completed` getters/setters, default-app setup flags, `isDefaultHomeApp`, `buildLauncherHomeIntent`, `openPrivacyPolicy`), `ProcessAppOpen`, `VPNHelper`, `LauncherHomeActivity`. Take them from the project when porting; the images (flags, intro art, `img_default`), strings (all locales), colors (`onboarding_native_bg`, `primary`, `text_secondary_dark`) and styles (`Theme.LauncherSettings`, `Theme.ClEnd`, `Theme.Home`) come from the project too.

## Porting checklist

1. Add dependencies (firebase-bom, config, analytics, messaging, crashlytics, play-services-ads, shimmer, installreferrer) and `google-services.json`; set `RC_KEY_SUFFIX` build fields.
2. Copy `assets/` as mapped above; change the package and the `R` import (`com.qrcode.scanner.spapp.R`).
3. Merge `manifest.onboarding.xml.snippet` (HOME intent filter on the launcher activity is what makes the Default Home role selectable).
4. Create the Remote Config parameter `QRScanner_<versionCode>` with `snippets/remote_config.json`; set real ad unit ids and `Privacy_Policy`.
5. Wire the splash: the NavHost shows `SplashScreen(onAdRootReady = { StartupFlow.begin(activity, it) { StartupNavigation.continueAfterVisibleSplash(activity) } })`, and `MainActivity` forwards `onRequestPermissionsResult` and `onDestroy` to `StartupFlow`.
6. Add the `PolicyScreen` alias if the old flow names must keep working.
7. Make `LauncherHomeActivity.onCreate` call `completeAfterDefaultSetup` when `isCompletingDefaultAppSetup && isDefaultHomeApp`.

## Test matrix

- Fresh install, online / offline (cached list, default list), VPN on then Recheck, notification allow / deny.
- Each screen: Back = main button, double tap, rotate, kill the app and reopen (resumes at the first incomplete screen).
- Permission: deny each of phone / camera / notification, grant overlay from Settings (guide card + poller), already default Home (overlay skipped).
- DefaultHome vs DefultSettingHome: Allow, Cancel, pick another launcher, already default, API < 29, settings screen missing.
- `Intro_Screen_Count` 0 / 1 / 2 / 3; every ad flag off; ad load failure and empty ids (flow must still advance once).
- Change `Show_Screen_Flow` remotely mid-onboarding (restart) and after onboarding (must not restart); `paid_user` vs `normal_user`.
- After onboarding: cold start goes straight to Home / QR app, never through the splash screens again except the splash itself.

## Debugging

- Screen never appears: name misspelled or unsupported (`PolicyScreen` without alias), its done flag is already true, or the app is already default Home (Default steps).
- Flow loops or jumps back: `flow_progress_index` reset, or the list changed remotely before onboarding ended (`resetOnboardingScreenCompletions`).
- Stuck on splash: `onFinished` never ran - check VPN dialog, Remote Config callback, `Splash_Duration`, and that an ad path forgot to call its finish callback.
- Next screen opens twice: missing `hasNavigated` / `flowContinued` / `navigationDelivered` guard, or the 1 s final-navigation debounce was bypassed.
- Home opens instead of the next step after Allow: `LauncherHomeActivity` handled the continuation but `startedBy` was wrong - check `setDefaultSetupStep`.
- Remote Config ignored: wrong parameter name (`QRScanner_<versionCode>` + suffix), invalid JSON (falls back to cache), profile key replacing the root.

## Related skills (the pieces after / beside this flow)

`default-home-role-flow`, `default-home-screen`, `default-setting-home-screen`, `overlay-permission-flow`, `default-launcher-reminder` (steps 3-4); `launcher-home-screen`, `launcher-app-drawer`, `launcher-settings-widgets` (Home); `launcher-app-click-ads`, `drawer-native-ads`, `right-swipe-ads` (Home ads); `callend-activity`, `install-uninstall-screen-ads` (event screens driven by Firebase / broadcasts).
