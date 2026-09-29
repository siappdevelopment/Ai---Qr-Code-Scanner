# Phase H — Splash + Startup Flow

The existing Compose splash remains the only user-visible startup splash. It now uses the Phase E screen list when it finishes. No second splash activity was added. Live Firebase was not verified because `google-services.json` is still absent.

## 1. Source Splash flow

`SplashActivity.onCreate`:

1. `ReminderAlarmHelper.handleReminderLaunchIntent`
2. `setContentView(activity_splash)`
3. On API 30+, hide navigation bars
4. If `VPNHelper.isVPNActive`, show `dialog_vpn_detect` and stop. Recheck starts the flow only when VPN is off
5. `WidgetNavigation.captureTargetScreen`
6. `startSplashFlow` and `startSplashAnimation`

`startSplashFlow` runs once: `MobileAds.initialize`, `FirebaseApp.initializeApp`, install referrer on a background executor, then `RemoteConfigHelper.fetchRemoteConfig`. The fetch callback binds ad views, shows the splash banner or small native ad, writes `appInstallDate` / `install_date` once, then checks `POST_NOTIFICATIONS` on API 33+.

Grant logs `POST_NOTIFICATIONS_GRANTED` once. Grant or deny both call `goNextScreen`. If the permission is already granted or the API is below 33, `goNextScreen` runs immediately.

`goNextScreen` waits `Splash_Duration` seconds, then `showAfterSplashAd`. If `After_Splash_Ad_Show` is false, it navigates immediately. Type `inter` loads `After_Splash_Interstitial_Id`. Any other type loads `App_Open_Id`. Completion, an empty id, or failure still calls `navigateNextScreen`.

`navigateNextScreen` calls `ScreenFlowNavigation.openFirstScreen` and finishes splash.

`openFirstScreen`: if `intro_completed` is true, `openMain`. Otherwise the first incomplete screen in `Show_Screen_Flow`. If none remain, `openMain`.

`openMain`: default HOME launcher → `LauncherHomeActivity` with `ACTION_MAIN`, `CATEGORY_HOME`, `CATEGORY_DEFAULT`. Else if `Redirect_Home_Launcher` → `LauncherHomeActivity`. Else → source `MainActivity`.

The logo animation (700 ms, then title and description) does not control navigation. There is no `first_launch` key.

## 2. Target startup flow before Phase H

`MainActivity` is `MAIN` + `CATEGORY_LAUNCHER`. `onCreate` shows `ScanPulseNavHost`. The start destination is the Compose `SplashScreen`, which waits 1400 ms and then opens the scan graph. `openFirstScreen` existed and was not called. `LauncherHomeActivity` is `MAIN` + `HOME` + `DEFAULT`.

## 3. Target startup flow after Phase H

Icon launch (`ACTION_MAIN` + `CATEGORY_LAUNCHER`) still shows the Compose splash for 1400 ms.

When that splash finishes, `StartupNavigation.continueAfterVisibleSplash` runs:

- The Phase E list is ensured from cache or the local default. The splash does not wait on a network fetch.
- If `intro_completed` is false, `openFirstScreen` opens the first incomplete onboarding screen and finishes `MainActivity`.
- If onboarding is complete and this app is not the default home and `Redirect_Home_Launcher` is false, the same `MainActivity` opens the existing scan graph.
- If onboarding is complete and this app is the default home, or redirect is true, `openMain` opens `LauncherHomeActivity` and finishes `MainActivity`.

`MainActivity` opened without `CATEGORY_LAUNCHER` skips the splash and opens the scan graph. That covers the screen-flow return to the QR app and `LauncherHomeActivity.openQrShell`.

## 4. Splash architecture decision

The Compose splash stays the single visible splash. A source `SplashActivity` was not added. Giving it `MAIN` + `LAUNCHER` would replace or duplicate the icon entry. Leaving it without that filter and still showing its XML layout would stack a second splash on the Compose splash.

Source XML, the VPN dialog, widget capture, and the reminder launch helper were not copied.

## 5. Remote Config behavior

`QrScannerApplication` still starts `RemoteConfigHelper.fetchRemoteConfig` without blocking. Splash does not call a second fetch and does not wait for `fetchAndActivate`. Routing uses the list already applied, or the Phase E fallback from `ensureShowScreenFlow`.

## 6. Local fallback behavior

Missing Firebase, a failed fetch, empty JSON, or malformed JSON still uses the Phase E cache or `Language`, `Collection`, `Permission`, `DefaultHome`, `Intro`. Startup continues. There is no spinner and no retry loop on the splash.

## 7. Onboarding routing

The same `screen_flow` preferences and `ScreenFlowNavigation` order are reused. Splash does not write a second completion file and does not clear existing completion.

- Fresh install: `intro_completed` is false, so the first incomplete screen opens.
- Partial completion: the first incomplete screen in the current list opens.
- Completed onboarding: the list is skipped.
- Remote Config unavailable: the local fallback list is used.

## 8. Default Home routing

Splash does not call `RoleManager`. The onboarding Default Home step still uses `DefaultHomePromptHelper`. After onboarding, a confirmed default-home app opens `LauncherHomeActivity` through the existing `openMain` rules.

## 9. Ad behavior

Phase F did not migrate splash or after-splash ads. They stay excluded. No banner, native, interstitial, app-open, or quiz placement was added. Existing ad frequency and cooldown logic was not changed. `MobileAds.initialize` remains the Phase F application call.

## 10. Analytics behavior

Phase G is unchanged. `POST_NOTIFICATIONS_GRANTED` stays unmigrated, and splash does not request notification permission. No splash or onboarding analytics events were added. `TrackOnce` was not modified.

## 11. Duplicate-splash prevention

There is no `SplashActivity`. The Compose splash is shown only for the launcher-icon intent. The screen-flow handoff to `MainActivity` has no `CATEGORY_LAUNCHER`, so it does not show the splash again. When the QR app is the final destination, the same `MainActivity` continues into the scan graph instead of restarting itself.

## 12. Startup performance behavior

Splash does not synchronously wait for Firebase, Remote Config, Analytics, AdMob, a database, or the network. The existing 1400 ms Compose delay is unchanged. Source `Splash_Duration` was not applied because it gates the excluded after-splash ad.

## 13. Activities and resources created

- `app/src/main/java/com/qrcode/scanner/launcher/common/StartupNavigation.java`
- `LAUNCHER_PHASE_H_IMPLEMENTATION.md`

No layout, theme, or icon resources were added.

## 14. Files modified

- `app/src/main/java/com/qrcode/scanner/MainActivity.kt`
- `app/src/main/java/com/qrcode/scanner/ui/navigation/ScanPulseNavHost.kt`

## 15. Gradle changes

None.

## 16. Manifest changes

None. `MainActivity` remains the only `MAIN` + `CATEGORY_LAUNCHER` activity. `LauncherHomeActivity` remains `MAIN` + `HOME` + `DEFAULT`.

## 17. Build result

`:app:assembleDebug` succeeded.

## 18. Unit-test result

`:app:testDebugUnitTest` succeeded. No new tests were added.

## 19. Device verification status

Not performed.

## 20. Live Firebase / Remote Config verification status

Not verified. `app/google-services.json` is not in the project. The fallback path does not need it.

## Source behavior left out

| Source step | Decision |
| --- | --- |
| `ReminderAlarmHelper` | Not migrated. Not part of screen-flow routing |
| VPN dialog | Not migrated. Would add a second blocking UI on the existing splash |
| `WidgetNavigation` | Widgets are out of scope |
| Install referrer | Explicitly excluded |
| `saveInstallDate` | Does not choose the next screen. Not used by the migrated flow |
| `POST_NOTIFICATIONS` and `POST_NOTIFICATIONS_GRANTED` | Phase G kept this event unmigrated |
| Splash banner/native and after-splash interstitial/app-open | Phase F kept these placements unmigrated |
| Source `Splash_Duration` | Times the excluded after-splash ad |
| XML `activity_splash` | Would be a second visible splash |
