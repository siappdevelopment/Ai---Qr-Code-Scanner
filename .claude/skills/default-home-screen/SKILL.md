---
name: default-home-screen
description: Build, modify, debug, or port the "Set as Default Home app" step (DefaultHome) of the startup flow, including when it shows, how it behaves placed before vs after the Permission step, the ROLE_HOME request, LauncherHomeActivity resume, the Set-as-Default button ad (AppOpen/Inter), and its Remote Config, layout, drawables, manifest and strings. Use when asked to move DefaultHome before or after Permission, change when the screen is skipped, fix flow bugs after the user picks this app as Home, or change its ads.
---

# Default Home screen (`StartupFlowStep.DEFAULT_HOME`)

Asks the user to make this app the default Home (launcher) app. It is one step of the flow owned by `StartupFlowManager` (see skill `startup-flow` for the whole sequence). Its position in the Remote Config array decides whether it runs **before** or **after** the Permission step.

Package root: `app/src/main/java/com/calendar/eventplanner/tasktracker/ADS/`

## When does the screen show

The step is evaluated by `StartupFlowManager.shouldSkipStep(DEFAULT_HOME)`. It **shows** only when all are true:

| Condition | Source |
|---|---|
| `DefaultHome` (or an alias) is in `show_intro_screen_flow` (or the flow array is empty → default order contains it) | Remote Config |
| `default_launcher_show == true` (root key, default true) | `ADSMainClass.getDefaultLauncherShow()` |
| App is not already default Home and not "chosen" during setup | `!Utils.isDefaultHomeAppOrChosen()` = `isRoleHomeHeld` / `isPreferredHomePackage` or `launcherChosenDuringSetup` |
| The flow reaches its index (earlier steps are completed or skipped) | `advanceFlow` |

If `default_launcher_show` is false, `DefaultAppActivity.onCreate` also bypasses itself (completes the step or goes to `homeDestinationClass()`), and the Home destination becomes `HomeActivity` instead of `LauncherHomeActivity`.

It is **not** shown again after the user set the app as Home (`isDefaultHomeAppOrChosen`), and it has no "shown once" flag: a user who cancelled sees it only on the next startup flow run, which only happens on a fresh `startStartupFlow` (each app start through Splash while the step is not skipped).

## Position: before vs after Permission

Order lives only in Remote Config: `"show_intro_screen_flow": [...]`.

```
After Permission (default):  Language, OnBoarding, Permission, PolicyScreen, DefaultHome, Home
Before Permission:           Language, OnBoarding, DefaultHome, Permission, PolicyScreen, Home
```

No code change is needed to move it, only the array. Behaviour differences, all from existing code:

| Topic | DefaultHome **after** Permission | DefaultHome **before** Permission |
|---|---|---|
| Overlay permission | `PermissionActivity` runs the overlay step if not granted, and shows the overlay card | If the user already became default Home: `checkAndRequestOverlayPermission()` skips the overlay settings flow and `cardOverlay` is `GONE` (both check `Utils.isDefaultHomeApp`). The overlay step is effectively replaced by the launcher role |
| Task / resume | Home intent usually arrives near the end, little left to resume | System sends `ACTION_MAIN + CATEGORY_HOME` to `LauncherHomeActivity` (singleTask, `clearTaskOnLaunch`) and the old task is cleared. `handleLauncherHomeStartupIntent` resumes the saved index: it shows the button ad then `completeStep(DEFAULT_HOME)` → Permission, PolicyScreen continue normally |
| `PERMISSION` skip rule | `isDefaultDialer || PERMISSION_SCREEN_SHOWN` unchanged | unchanged (default Home is not the dialer role) |
| Cancel / back | Cancel → button ad → `completeStep(DEFAULT_HOME)` → Home | Cancel → button ad → `completeStep(DEFAULT_HOME)` → Permission |
| Ad stacking | Permission has only a bottom ad; button ad runs last before Home | Button ad runs before Permission; avoid also showing an inter on the screen before it (Language/Onboarding inter) back to back |
| Risk | none special | Flow must be `active` when the HOME intent arrives. Anything that calls `clearFlowState` earlier (e.g. back exit) makes the resume path fall back to opening Home |

If DefaultHome is placed before Language or OnBoarding, the same resume logic restarts at the saved index, so those screens still run (code comment: "Resume remaining steps (Language / Permission / OnBoarding / PolicyScreen)").

## Flow in detail

```
advanceFlow reaches DEFAULT_HOME (not skipped)
  navigateToStep(DEFAULT_HOME):
     DefaultPermissionButtonAd.preload(activity)
     requestDefaultHomeBeforeDefaultApp(host)           [API 29+, ROLE_HOME available]
        Utils.setCompletingDefaultAppSetup(true)          (commit(), LauncherHome must see it)
        startActivity(Settings.ACTION_HOME_SETTINGS)      (real "Default home app" page)
        host onStopped → OverlayPermissionActivity(EXTRA_DEFAULT_HOME_GUIDE=true)  (guide chip over settings, auto-hides)
        host onResumed after pause:
            default Home now → host.finish()              (HOME intent goes to LauncherHomeActivity)
            else → setCompletingDefaultAppSetup(false), start DefaultAppActivity, finish host
        exception / role unavailable → return false → start DefaultAppActivity directly
  DefaultAppActivity (if shown): "Set as default" button
     tvSetAsDefault click:  SET_DEFAULT_HOME_CLICK event, ad.resetShownFlag(), ad.preload()
        already default → showButtonAd → goNextAfterDefault()
        else waitingForDefaultResult=true, setCompletingDefaultAppSetup(true), requestDefaultHomeApp():
             API 29+ RoleManager ROLE_HOME: held → ad → next; else homeRoleLauncher(createRequestRoleIntent)
             else HOME chooser intent; else ACTION_HOME_SETTINGS fallback
     result:
        Allow (isDefaultHomeApp) → applyRecentsVisibility, finish();  LauncherHomeActivity continues (below)
        Cancel                    → DEFAULT_HOME_SET_CANCEL + PermissionFirebaseEvents, button ad, continueAfterCancelAd()
     goNextAfterDefault(): navigatedAway=true, setCompletingDefaultAppSetup(false), if default → DEFAULT_HOME_SET_SUCCESS,
                           StartupFlowManager.completeStep(DEFAULT_HOME)
LauncherHomeActivity (HOME intent, flow active or completingSetup)
  onCreate: TransparentTheme set BEFORE super.onCreate → handleLauncherHomeStartupIntent():
     setLauncherChosenDuringSetup(true), success events, setCompletingDefaultAppSetup(false)
     flow not active → start homeDestinationClass() (CLEAR_TOP), finish
     on DEFAULT_HOME step / completingSetup → showDefaultButtonAdThenCompleteStep → ad → completeStep(DEFAULT_HOME)
     other step pending (and not HOME) → advanceFlow(index)
     else → return false (normal launcher UI)
  onNewIntent runs the same handler.
```

Guards to keep: `defaultButtonAdAdvanceInProgress` (no double ad / double completeStep if the HOME intent fires twice), `navigatedAway` and `isShowingButtonAd` in the activity, `completeStep`'s `currentIndex > completedIndex` check.

Back button on `DefaultAppActivity`: `StartupFlowManager.exitAppOnBackPress` (clears flow state, closes the app).
Manifest `excludeFromRecents` on `DefaultAppActivity`; `Utils.applyRecentsVisibility` hides the app from Recents once it is the default Home.

## Files

| File | Role |
|---|---|
| `launcher/activity/DefaultAppActivity.java` | UI, bottom ad, role request, cancel/allow handling, `completeStep(DEFAULT_HOME)` |
| `navigation/StartupFlowManager.kt` | skip rule, `navigateToStep`, `requestDefaultHomeBeforeDefaultApp`, `handleLauncherHomeStartupIntent`, `showDefaultButtonAdThenCompleteStep`, `homeDestinationClass` |
| `launcher/LauncherHomeActivity.java` | HOME entry; transparent theme + resume handler in `onCreate` / `onNewIntent` |
| `activities/overlayPermission/OverlayPermissionActivity.java` | `EXTRA_DEFAULT_HOME_GUIDE` guide card over the system settings page |
| `advertisement/DefaultPermissionButtonAd.java` | thin wrapper (preload / show / shouldShow / resetShownFlag) |
| `launcher/utils/DefaultPermissionButtonAd.java` | real AppOpen / Inter loader + show, Quiz priority fallback |
| `launcher/common/Utils.java` | `isDefaultHomeApp`, `isRoleHomeHeld`, `isPreferredHomePackage`, `isDefaultHomeAppOrChosen`, `set/isCompletingDefaultAppSetup`, `set/isLauncherChosenDuringSetup`, `applyRecentsVisibility` |
| `activities/permissions/PermissionActivity.kt` | overlay step skipped / card hidden when `isDefaultHomeApp` |
| `activities/splash/FirebaseRemoteConfigLoader.kt`, `GlobalParameterManage.java`, `advertisement/ADSMainClass.java` | config parse, constants, storage |

Copies of the activity, both ad classes, the layouts and the drawables are in `assets/`.

## Ads

| Ad | Condition | Notes |
|---|---|---|
| Bottom ad on the screen | `ADSMainClass.getDefaultPermissionBottomAdsShow()` (default true) | type `getDefaultPermissionAdsType()` (default `banner`): `native` → `ADSNativeDisplay.loadAdmobNativeAdBig(DEFAULT_PERMISSION_SCREEN_NATIVE, flNativeSmallPlaceholder, shimmer_container_banner, "big", this)`, else `ADSBannerAdaptive.loadAdMobBanner(DEFAULT_PERMISSION_SCREEN_BANNER, …, "big")`. Sets `AdPlacement.KEEP_BIG_SHIMMER_TAG` on the shimmer. Hidden when off |
| Set-as-Default **button ad** (full-screen) | `DefaultPermissionButtonAd.shouldShow()` = not ads-free **and** `default_permission_button_ads_show` **and** (type is AppOpen **or** global `getInterAdsShow()`) | type `getDefaultPermissionButtonAdsType()` `Appopen` (default) or `Inter` (unit `INTER_FIRST_TIME`). Preloaded on entering the step (`navigateToStep`), in the activity `onCreate` / `onResume`, and on button click. Shown after Allow (from LauncherHome) and after Cancel (in the activity), `shownForCurrentSetup` allows one show per setup, `resetShownFlag()` on each button click |

Rules: the next screen opens only in the ad's finish callback, but every path (ads-free, not ready, already shown, load fail) must still call it once. `ADSAppManage.isAppOpenBlocked = true` while the ad runs so the generic app-open ad does not stack. The ad is shown on a transparent host (`TransparentTheme`) after Allow, so no wallpaper flashes.

## Firebase Remote Config

| Where | Key | Type | Default if missing |
|---|---|---|---|
| root | `show_intro_screen_flow` | array, include `"DefaultHome"` | default order (after Permission) |
| root | `default_launcher_show` (legacy `defualt_launcher_show`) | bool | true |
| `screen.default_permission_screem` | `default_permission_bottom_ads_show` | bool | true |
| | `default_permission_ads_type` | `native` / `banner` | `banner` |
| | `default_permission_native_id` / `default_permission_banner_id` | string | `""` |
| | `default_permission_button_ads_show` | bool | false |
| | `default_permission_button_ads_type` | `Appopen` / `Inter` | `Appopen` |
| `screen.other_screen` (global) | `inter_ads_show` | bool | needed only when the button ad type is `Inter` |

The whole `default_permission_screem` object is optional (`optJSONObject`), so a missing block never breaks the parse. Place DefaultHome after Permission:

```json
"show_intro_screen_flow": ["Language","OnBoarding","Permission","PolicyScreen","DefaultHome","Home"]
```
Before Permission:
```json
"show_intro_screen_flow": ["Language","OnBoarding","DefaultHome","Permission","PolicyScreen","Home"]
```
Hide the step completely: set `default_launcher_show` to `false` (also switches Home to `HomeActivity`) or remove `"DefaultHome"` from the array.

Storage / constants: `GlobalParameterManage` (`DEFAULT_PERMISSION_SCREEM`, `DEFAULT_PERMISSION_*`), `ADSMainClass` (`DefaultPermissionBottomAdsShow`, `DefaultPermissionAdsType`, `DefaultPermissionButtonAdsShow`, `DefaultPermissionButtonAdsType`, `DefaultLauncherShow`, IDs `DEFAULT_PERMISSION_SCREEN_NATIVE / _BANNER`).

## AndroidManifest

```xml
<activity android:name=".ADS.launcher.activity.DefaultAppActivity"
    android:excludeFromRecents="true" android:exported="true"
    android:screenOrientation="portrait" android:windowSoftInputMode="adjustPan" />

<!-- guide card over the system settings page -->
<activity android:name=".ADS.activities.overlayPermission.OverlayPermissionActivity"
    android:allowEmbedded="true" android:configChanges="keyboardHidden" android:excludeFromRecents="true"
    android:exported="true" android:launchMode="singleInstance" android:screenOrientation="portrait"
    android:theme="@style/OverlayGuideActivity.Theme" android:windowSoftInputMode="adjustPan" />

<!-- the real launcher: the HOME category is what makes the role request possible -->
<activity android:name=".ADS.launcher.LauncherHomeActivity"
    android:clearTaskOnLaunch="true"
    android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize"
    android:excludeFromRecents="true" android:exported="true" android:launchMode="singleTask"
    android:screenOrientation="portrait" android:stateNotNeeded="true"
    android:theme="@style/Theme.Home" android:windowDisablePreview="true" android:windowSoftInputMode="adjustPan">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```
Also needed: `TransparentTheme` style, and (for the overlay guide) the overlay permission declared by the Permission step. Without the HOME intent filter `RoleManager.isRoleAvailable(ROLE_HOME)` is true but the app is not selectable.

## Layout, drawables, strings

`res/layout/activity_default_app.xml` (copy in `assets/layout/`), root vertical `LinearLayout` (white, `fitsSystemWindows`):
1. `NestedScrollView` (weight 1): `img_default` (150sdp), `tvTitle` (`plus_jakarta_sans_bold`, 18sdp, `@string/set_calendar_as_your_home_app`), `tvDescription` (regular, 12sdp, `gray_color`, maxLines 2)
2. `btnSetDefault` (`ShimmerFrameLayout`, 38sdp) wrapping `tvSetAsDefault` (`@drawable/custom_button`, white bold 15sdp, text `@string/set_as_default`)
3. Policy row: `ic_policy` + `tv_privacy_policy` (spannable "terms / privacy" links in `calendar_primary`, built from `we_don_t_collect_personal`, `terms_of_service`, `privacy_policy`)
4. Bottom ad container `banner` → CardView → `shimmer_container_banner` (`fill_in_ad_unifiled_small` include) + `flNativeSmallPlaceholder` (style `NativeAdsView`), sibling `flBannerSmallPlaceholder`

Required IDs: `tvTitle`, `tvDescription`, `btnSetDefault`, `tvSetAsDefault`, `tv_privacy_policy`, `shimmer_container_banner`, `flNativeSmallPlaceholder`, `flBannerSmallPlaceholder`.

Guide layout `activity_overlay_permission_default_home.xml` needs `ivGuideIcon` and `tvGuideName`; drawables `bg_default_home_guide_chip/icon/radio`; string `default_home_guide_message`.

Resources to ship: `img_default.png`, `ic_policy.xml`, `custom_button.xml`, `bg_default_home_guide_*.xml`, strings `set_calendar_as_your_home_app`, `set_as_default`, `set_calendar_home_launcher_as_your_default_home_app_…`, `we_don_t_collect_personal`, `terms_of_service`, `privacy_policy`, `default_home_guide_message` (all locales), colours `white`, `black`, `gray_color`, `calendar_primary`.

## Analytics

`AppAnalyticsEvents`: `SET_DEFAULT_HOME_VIEW` (screen view), `SET_DEFAULT_HOME_CLICK`, `DEFAULT_HOME_SET_SUCCESS`, `DEFAULT_HOME_SET_CANCEL`; `PermissionFirebaseEvents.trackDefaultHomeAppSet / trackDefaultHomeAppCancel`.

## Build sequence (in order)

1. **Remote Config**: add `"DefaultHome"` to `show_intro_screen_flow` at the wanted position (before or after `"Permission"`), set `default_launcher_show` and the `default_permission_screem` block.
2. **Constants / storage**: `GlobalParameterManage`, `ADSMainClass` getters/setters, IDs; parse in `FirebaseRemoteConfigLoader` with the optional getters shown above.
3. **Resources**: strings (all locales), drawables, colours, `TransparentTheme`, both layouts.
4. **Manifest**: the three activities above.
5. **Classes**: `Utils` helpers first, then the two `DefaultPermissionButtonAd` classes, `DefaultAppActivity`, the `OverlayPermissionActivity` guide mode, then the `StartupFlowManager` cases (`shouldSkipStep`, `navigateToStep`, `requestDefaultHomeBeforeDefaultApp`, `handleLauncherHomeStartupIntent`) and `LauncherHomeActivity` hook.
6. **Cross-step check**: `PermissionActivity` overlay skip/hide uses `Utils.isDefaultHomeApp`.
7. **Test matrix** (do both placements):
   - Allow, Cancel, back, kill app on the settings page
   - already default Home (step skipped), `default_launcher_show` false
   - API < 29 (chooser path), role unavailable, `ACTION_HOME_SETTINGS` missing
   - button ad on/off, `Appopen` vs `Inter` with `inter_ads_show` off, ads-free, Quiz priority
   - before-Permission order: after Allow the app resumes at Permission (not Home), overlay card hidden; after Cancel Permission shows overlay card
   - HOME intent delivered twice, rotation, process death while waiting on the role dialog

## Debugging

- Logcat `ADS_INTER`: `DefaultPermission Allow SHOW first on …`, `already showing, skip re-entry`, `DefaultPermission Allow — ad closed → next screen`, `DefaultPermissionButtonAd.show skip | inter_ads_show=false` (button type Inter without global inter on). Tag `StartupFlow`: `ROLE_HOME request could not be started`.
- Step never appears: app already default (`isDefaultHomeAppOrChosen`, note `launcherChosenDuringSetup` stays true), `default_launcher_show=false`, name missing/misspelled in the array.
- Flow jumps to Home after Allow instead of the next step: flow not active when HOME arrived (`clearFlowState` ran), or `flow[index]` is already `HOME`.
- Blank / wallpaper flash after Allow: `LauncherHomeActivity` theme not set to `TransparentTheme` before `super.onCreate`, or `overridePendingTransition(0,0)` missing.
- Ad shows twice or flow advances twice: lost `defaultButtonAdAdvanceInProgress` / `navigatedAway` guard.
- Overlay card still visible after setting default: `isDefaultHomeApp` false because RoleManager lags (Android 12); `launcherChosenDuringSetup` is only consulted by `isDefaultHomeAppOrChosen`, not by `isDefaultHomeApp`.
- Stuck guide card on the settings page: `OverlayPermissionActivity` finishes only when `isDefaultHomeApp`, otherwise after `DEFAULT_HOME_GUIDE_AUTO_HIDE_MS` (2 s).

## Porting

1. Copy `assets/java/*`, `assets/layout/*`, `assets/drawable/*`, `img_default.png`, `ic_policy.xml` and the strings.
2. Replace `ADSMainClass`, `ADSNativeDisplay`, `ADSBannerAdaptive`, `AdPlacement`, `AppAnalyticsEvents`, `PermissionFirebaseEvents`, `LocaleAwareAppCompatActivity`, `Utils`, `StartupFlowManager` with the target project's versions.
3. The target app needs a launcher activity with the HOME intent filter and a `StartupFlowManager` containing the DefaultHome cases.
4. Pick the position in the flow array; nothing else changes.
