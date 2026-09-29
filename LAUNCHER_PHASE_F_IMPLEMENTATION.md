# Launcher Phase F Implementation

AdMob SDK and the source ad placements that already have screens in this app. The Source Launcher project was not modified. Java stayed Java. Compose QR screens were not changed.

Debug build: `:app:assembleDebug` succeeded.  
Unit tests: `:app:testDebugUnitTest` succeeded.

Device verification was not performed. Live ads were not loaded.

Live AdMob configuration is unavailable because the target `google-services.json` and a target AdMob app id are missing.

---

## Status

| Item | Status |
| --- | --- |
| AdMob SDK integrated | Yes. `play-services-ads` 24.7.0 |
| Ad configuration available | No. No `APPLICATION_ID` meta-data and no unit ids in the project |
| Remote Config available | Code path from Phase E. Live fetch still needs `google-services.json` |
| Ad loading implemented | Yes. Loaders run only when the app id and a remote unit id are both present |
| Live ads verified | No |

---

## 1. AdMob SDK

`com.google.android.gms:play-services-ads` `24.7.0`, the source version.

Not added: Facebook mediation, Audience Network, Firebase Analytics, Google Analytics.

## 2. Target AdMob configuration

Not available.

There is no AdMob app id, no ad unit id, and no `com.google.android.gms.ads.APPLICATION_ID` meta-data in this project. Source ids were not copied into the manifest, resources, or Java.

`AdPlacement.initializeIfConfigured` reads that meta-data. If it is missing, `MobileAds.initialize` is not called. Creating an `AdView` without an app id crashes, so every load checks `canRequestAds` first and then hides the slot or continues navigation.

## 3. google-services.json

Still absent for `com.qrcode.scanner`. The Google Services plugin stays conditional, as in Phase E. Remote Config cannot fetch until that file exists. Ad flags therefore stay at their Java defaults: show flags `false`, unit ids `""`.

## 4. Application initialization

`QrScannerApplication.onCreate` still starts Remote Config, then calls `AdPlacement.initializeIfConfigured`. That call returns immediately when no app id is present. It does not do network work on the main thread. DataStore and AppCompat locale handling were not changed.

## 5. Source components inspected

| Source | Target | Decision | Reason |
| --- | --- | --- | --- |
| `AdPlacement` Google banner, adaptive banner, native, interstitial, app-open loaders | `launcher/common/AdPlacement.java` | Adapt | Used by onboarding, launcher settings, and launcher app ads |
| `MobileAds.initialize` in `MyApplication` | `QrScannerApplication` | Adapt | Only when `APPLICATION_ID` exists |
| Remote Config ad keys in `RemoteConfigHelper.applyAdConfig` | `RemoteConfigHelper.applyAdFields` | Adapt | Same key names. Defaults stay off |
| Language, Permission, Intro, Default onboarding ads | Those Phase E activities | Adapt | Source placements on screens that exist here |
| Launcher settings `Other_*` ad | `LauncherSettingsActivity` | Adapt | Existing XML container |
| Drawer native list ad | `LauncherAppsBottomSheet` | Adapt | Existing container. Quiz priority does not show a quiz card |
| App click and return ads | Adapter, home dock, `LauncherHomeActivity.onResume` | Adapt | Source counters. Quiz types continue without a quiz screen |
| Quiz layouts and `Google_Ad_Failed_Show_Quiz` UI | Not copied | Do not migrate | Quiz is out of scope. Failure hides the slot or continues |
| `logAdRevenue` / `ad_impression` | Not copied | Do not migrate | Analytics is out of scope |
| Splash and after-splash ads | Not called | Do not migrate | Splash was not migrated |
| `MainContainerFragment` right-swipe interstitial | Not called | Do not migrate | QR pager was not migrated |
| Main, Create, General, Social, Barcode, Settings fragment ads | Not called | Do not migrate | Those are the source QR pager, not Compose screens |
| `SubContainerFragment` native ad | Not called | Do not migrate | That fragment was not migrated |
| Call-end ads | Not copied | Do not migrate | Call-end was not migrated |
| Process-lifecycle app-open show | Not copied | Do not migrate | Source `shouldSkipAppOpenAd` already prevents that show |
| Rewarded ads | Not found in source | Do not migrate | Not found in source |
| Facebook mediation | Not added | Do not migrate | Not required for the Google loaders |
| `maybeShowDefaultHomePopup` | Not restored | Do not migrate | Phase D. It is a remote popup, not an ad view |
| Source AdMob app id and unit ids | Not copied | Do not migrate | They belong to the source app |

## 6. Placements migrated

| Placement | Screen | Type | Trigger | Remote keys | Frequency | Preload | If unavailable |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Language | `LanguageActivity` | Banner or native `large` | `onCreate` | `Language_Ad_Show`, `Language_Ad_Type` (`native` if missing), `Language_Banner_Id`, `Language_Native_Id` | None | No | Hide the slot |
| Language done | `LanguageActivity` | Interstitial | Done | `Language_Interstitial_Ad_Show`, `Other_Interstitial_Id` | None. Not `Interstitial_Click` | No. Loading dialog, 10 second timeout | Continue the screen list |
| Permission | `PermissionActivity` | Banner or native `small` | `onCreate` | `PermissionDefault_Ad_Show`, `PermissionDefault_Ad_Type` (`banner` if missing), banner and native ids | None | No | Hide the slot |
| Default Home screen | `DefaultActivity` | Same PermissionDefault slot | `onCreate` | Same `PermissionDefault_*` | None | No | Hide the slot. Role request is unchanged |
| After Default Home | `DefaultActivity` after the Phase D helper finishes | Interstitial, or app open when the type normalizes to `appopen` | After success or cancel, before `continueAfter` | `After_Default_Ad_Show` / `After_default_Ad_Show`, `After_Default_Ad_Type` / `After_default_Ad_Type` (`inter` if missing), `Other_Interstitial_Id` or `App_Open_Id` | None | `preloadAfterDefaultAd` on `onCreate` | Continue the screen list. Does not finish `LauncherHomeActivity` and does not request `ROLE_HOME` |
| Intro pages | `Intro1Activity`, `Intro2Activity`, `Intro3Activity` | Banner or native `intro` | `onCreate` | `Intro_Ad_Show`, `Intro_Ad_Type` (`banner` if missing), banner and native ids | None | No | Hide the slot |
| Last intro Next | `IntroNavigation` | Interstitial | Last visible page | `Intro_Interstitial_Ad_Show`, `Other_Interstitial_Id` | None | No. Loading dialog, 10 second timeout | `continueAfter("Intro")` |
| Launcher settings | `LauncherSettingsActivity` | Banner or native `medium` | `onCreate` | `Other_Ad_Show`, `Other_Ad_Type` (`native` if missing), `Other_Banner_Id`, `Other_Native_Id` | None | No | Hide the slot |
| App drawer list | `LauncherAppsBottomSheet` | Native `medium` | Drawer `showAd` | `LauncherApp_Native_List_Ad_Show`, `LauncherApp_Native_List_Id`, `LauncherApp_Native_List_Ad_Show_Per_Day` | Per day ≤ 0 means no cap. Otherwise `24 hours / count` | No | Hide the slot. No quiz item |
| App open from drawer or home dock | `LauncherAppsAdapter`, home dialer/SMS/browser/camera | `LauncherApp_Ad_Type` | Click, before the app opens | `LauncherApp_Click_Ad_Show`, `LauncherApp_Count`, `LauncherApp_Interstitial_Id`, `App_Open_Id` for app-open type, `Other_Native_Id` for native type | In-memory count starts at 1. Ad when count equals `LauncherApp_Count`, then resets to 1. Count ≤ 0 disables | No. 10 second timeout on interstitial and native | Open the app |
| Return to launcher | `LauncherHomeActivity.onResume` | `LauncherApp_Back_Ad_Type`, else click type | Resume after a marked external launch | `LauncherApp_Back_Click_Ad_Show`, `LauncherApp_Back_Count`, `LauncherApp_Back_Interstitial_Id` | Prefs `launcher_app_back_ad` / `launcherAppReturnCount` | No | Stay on the launcher. The activity is not finished |

`Ad_Priority` = `QUIZ` skips the Google request. The source would show a quiz layout. Quiz was not migrated, so the slot stays hidden and navigation continues.

`Google_Ad_Failed_Show_Quiz` is read from Remote Config and stored. It does not show a quiz. A failed Google load hides the slot or continues. There is no second Google request.

## 7. Banner

`loadBannerAd` and `loadAdaptiveBannerAd`. Language uses the adaptive loader, matching the source language screen. Other banner slots use `loadBannerAd`.

Shimmer is shown while loading, hidden on success, and the whole slot is hidden on failure, missing id, missing app id, or no network. The banner does not block the launcher.

## 8. Native

Layouts copied from the source: `native_small_ad_layout.xml`, `native_medium_ad_layout.xml`, `native_large_ad_layout.xml`, `native_intro_ad_layout.xml`, `native_full_ad_layout.xml`.

Headline, body, icon, call to action, and media for large, medium, and full are bound the way the source `populateNativeAdView` does. `Native_Ad_Label_Color` and `Native_Ad_Button_Color` tint those views. Empty or malformed colors fall back to the existing primary color for the button.

The drawer list ad is destroyed in `onDestroyView`. Banner `AdView`s are destroyed when the container is cleared.

Quiz native injection into the app list was not restored.

## 9. Interstitial

Language done, last intro page, after-default (when the type is not app open), and launcher click/back types `google_inter` / `Google_Inter` / `inter`.

A loading dialog uses `dialog_loading_ads.xml`. It is dismissed on load, failure, or after 10 seconds, then the original action runs. There is no retry loop.

`Interstitial_Click` was not wired. In the source it applies only to `loadInterstitialAd`, which those screens do not use.

## 10. Rewarded

Not found in source. Not added.

## 11. App open

Used only where the source uses it for a placement that exists here:

- After Default Home when the type normalizes to `appopen`
- Launcher click/back type `google_app_open` / `Google_App_Open` / `app_open`, unit `App_Open_Id`

The process-lifecycle foreground app-open ad was not added. The source skips that show.

## 12. Remote Config ad keys

Applied only after a successful JSON parse, with the source defaults when a key is missing:

`Ad_Priority`, `Google_Ad_Failed_Show_Quiz`, `Native_Ad_Label_Color`, `Native_Ad_Button_Color`, `App_Open_Id`, `Language_Ad_Show`, `Language_Ad_Type`, `Language_Banner_Id`, `Language_Native_Id`, `Language_Interstitial_Ad_Show`, `PermissionDefault_Ad_Show`, `PermissionDefault_Ad_Type`, `PermissionDefault_Banner_Id`, `PermissionDefault_Native_Id`, `After_Default_Ad_Show`, `After_default_Ad_Show`, `After_Default_Ad_Type`, `After_default_Ad_Type`, `Intro_Ad_Show`, `Intro_Ad_Type`, `Intro_Banner_Id`, `Intro_Native_Id`, `Intro_Interstitial_Ad_Show`, `Other_Ad_Show`, `Other_Ad_Type`, `Other_Banner_Id`, `Other_Native_Id`, `Other_Interstitial_Id`, `LauncherApp_Native_List_Ad_Show`, `LauncherApp_Native_List_Ad_Show_Per_Day`, `LauncherApp_Native_List_Id`, `LauncherApp_Click_Ad_Show`, `LauncherApp_Count`, `LauncherApp_Ad_Type`, `LauncherApp_Interstitial_Id`, `LauncherApp_Back_Click_Ad_Show`, `LauncherApp_Back_Count`, `LauncherApp_Back_Ad_Type`, `LauncherApp_Back_Interstitial_Id`.

`default_config.xml` still has no ad-unit defaults. A missing or malformed ad value does not crash. Show flags default to false and ids default to empty, so the placement stays off.

Splash, main, create, quiz, and call-end keys are not applied. Phase E screen-list behavior is unchanged.

## 13. Frequency / cooldown

- Launcher click: in-memory count, starts at 1, shows when it equals `LauncherApp_Count`, then resets to 1. Count ≤ 0 disables the ad.
- Launcher return: `launcherAppReturnCount` in prefs `launcher_app_back_ad`. Shows when the count reaches `LauncherApp_Back_Count`, then resets to 0.
- Drawer native list: if `LauncherApp_Native_List_Ad_Show_Per_Day` ≤ 0, no cap. Otherwise the gap is 24 hours divided by that count, stored in `launcher_app_native_list_ad_preferences`.

## 14. Preload

`preloadAfterDefaultAd` loads the after-default interstitial or app-open ad when the flag is on, the network is up, the app id exists, and the unit id is non-empty. `loadAfterDefaultAd` shows that preload or loads again. Empty id or failure continues onboarding.

No other placement is preloaded.

## 15. Failure / fallback

Missing app id, missing unit id, no network, SDK init skipped, load error, show error, or the 10 second dialog timeout: hide the slot or run the original navigation. No crash, no second request, no permanent shimmer. `Ad_Priority` `QUIZ` does the same, because quiz UI was not migrated.

## 16. XML containers wired

Existing `rlAdView` trees on language, permission, default, intro 1–3, and launcher settings. Existing drawer `rlNativeListAdView`. No new cards, banners, or spacing were added. Shimmer stays inside the source containers and is hidden when loading ends.

## 17. Manifest

`ACCESS_NETWORK_STATE` was added so the source network check can see a connection. `INTERNET` was already present from Phase E.

No AdMob `APPLICATION_ID` meta-data was added. Launcher and Home ownership were not changed.

## 18. Gradle

`play-services-ads` 24.7.0 in `gradle/libs.versions.toml` and `app/build.gradle.kts`.

The Google Services plugin is still applied only when `app/google-services.json` exists.

## 19. Files created

- `launcher/common/AdPlacement.java`
- `res/layout/native_small_ad_layout.xml`
- `res/layout/native_medium_ad_layout.xml`
- `res/layout/native_large_ad_layout.xml`
- `res/layout/native_intro_ad_layout.xml`
- `res/layout/native_full_ad_layout.xml`
- `res/layout/dialog_loading_ads.xml`
- `res/drawable/custom_label.xml`
- `res/drawable/ic_close.png`

## 20. Files modified

- `QrScannerApplication.java`
- `launcher/remote/RemoteConfigHelper.java`
- `launcher/common/IntroNavigation.java`
- `launcher/activities/LanguageActivity.java`
- `launcher/activities/PermissionActivity.java`
- `launcher/activities/DefaultActivity.java`
- `launcher/activities/Intro1Activity.java`
- `launcher/activities/Intro2Activity.java`
- `launcher/activities/Intro3Activity.java`
- `launcher/activities/LauncherSettingsActivity.java`
- `launcher/activities/LauncherHomeActivity.java`
- `launcher/dialogs/LauncherAppsBottomSheet.java`
- `launcher/adapters/LauncherAppsAdapter.java`
- `launcher/fragments/LauncherHomeFragment.java`
- `AndroidManifest.xml`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `res/values/strings_onboarding.xml` (`loading_ads_title`, `loading_ads_desc`)

## 21. Source ad ids not copied

No source app id and no source unit id were written into the manifest, strings, constants, Remote Config defaults, or Java.

## 22. Ads not migrated

Splash, after-splash, right-swipe, source QR pager placements, SubContainer native, call-end, rewarded, process-lifecycle app open, Facebook mediation, quiz creatives, analytics impression logs, and `maybeShowDefaultHomePopup`.

## 23. Can live ads load?

No. There is no target AdMob app id and no `google-services.json`, so unit ids cannot be fetched and `MobileAds.initialize` is not called. The app still opens and navigation still continues.

## 24. Build result

`:app:assembleDebug` — BUILD SUCCESSFUL.

## 25. Unit-test result

`:app:testDebugUnitTest` — BUILD SUCCESSFUL.

## 26. Device verification

Not performed.
