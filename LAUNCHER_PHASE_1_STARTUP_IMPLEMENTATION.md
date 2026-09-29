# Phase 1 — Source startup migration

Phase 1 migrates the source splash startup sequence into the existing Compose splash. There is no second splash activity. `LauncherHomeActivity` remains HOME. `MainActivity` remains the QR / launcher-icon entry. Source `MainActivity` and `MainContainerFragment` were not migrated. QR Scanner, Create, History, and Settings behavior was not replaced.

## Files changed

Added:

- `app/src/main/java/com/qrcode/scanner/launcher/common/StartupFlow.java`
- `app/src/main/java/com/qrcode/scanner/launcher/common/WidgetNavigation.java`
- `app/src/main/java/com/qrcode/scanner/launcher/common/InstallReferrerStartup.java`
- `app/src/main/java/com/qrcode/scanner/launcher/helpers/VPNHelper.java`
- `app/src/main/java/com/qrcode/scanner/launcher/helpers/ReminderAlarmHelper.java` (launch handoff only)
- `app/src/main/res/layout/dialog_vpn_detect.xml`
- `app/src/main/res/layout/layout_splash_startup_ads.xml`
- `app/src/main/res/drawable/img_vpn_detect.webp` (copied from source)

Updated:

- `app/src/main/java/com/qrcode/scanner/MainActivity.kt`
- `app/src/main/java/com/qrcode/scanner/QrScannerApplication.java`
- `app/src/main/java/com/qrcode/scanner/ui/screens/splash/SplashScreen.kt`
- `app/src/main/java/com/qrcode/scanner/ui/navigation/ScanPulseNavHost.kt`
- `app/src/main/java/com/qrcode/scanner/launcher/common/StartupNavigation.java`
- `app/src/main/java/com/qrcode/scanner/launcher/common/ScreenFlowNavigation.java`
- `app/src/main/java/com/qrcode/scanner/launcher/common/AdPlacement.java`
- `app/src/main/java/com/qrcode/scanner/launcher/common/AppUtils.java`
- `app/src/main/java/com/qrcode/scanner/launcher/remote/RemoteConfigHelper.java`
- `app/src/main/java/com/qrcode/scanner/launcher/activities/LauncherHomeActivity.java`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/res/values/strings_onboarding.xml`

## Source classes and features migrated

- `SplashActivity` startup sequence, without its layout or logo animation. The animation does not decide navigation in source.
- `VPNHelper.isVPNActive` and `dialog_vpn_detect` (non-cancelable, Recheck, 16 dp dialog padding).
- Splash banner/native placement and after-splash interstitial or app-open placement.
- API 33+ `POST_NOTIFICATIONS` request code 101.
- `appInstallDate` / `install_date` (`yyyy-MM-dd`, written only when the key is absent).
- Install Referrer client started from splash. Stored in `referrer_preferences` / `SetReferrerUrl`.
- `WidgetNavigation.captureTargetScreen` and `attachPendingTargetScreen`.
- `ReminderAlarmHelper.handleReminderLaunchIntent` (cancel notification tag `app_reminder`, then strip extras).
- Splash Remote Config fields and the source `paid_user` / `normal_user` referrer check.

## Target integration points

The Compose `SplashScreen` stays visible. It no longer waits a fixed 1400 ms. `StartupFlow` runs the source sequence, then calls the existing `StartupNavigation.continueAfterVisibleSplash`.

Order, matching source:

1. Reminder handoff on `MainActivity` before the splash decision, so the reminder extra is still visible when deciding to show the splash.
2. If a VPN transport is active, show the VPN dialog and do not start the splash flow. Recheck starts the flow only when the VPN is off. Recheck does not capture a widget extra. That matches source, which captures the widget extra only on the non-VPN `onCreate` path and on `onNewIntent`.
3. Capture `TARGET_SCREEN` (`scan` or `create`) into prefs `widget_navigation` / `pending_target_screen`, then remove it from the intent.
4. Start Install Referrer on a background executor. Do not wait for it.
5. `RemoteConfigHelper.fetchRemoteConfig`. If `QrScannerApplication` already has a fetch in progress, the splash callback is queued and invoked when that fetch finishes. It is not dropped.
6. Show the splash banner, or native size `small`, when the network is available and `Splash_Ad_Show` is true.
7. Save the install date if missing.
8. On API 33+, request `POST_NOTIFICATIONS` when it is not granted. Grant and deny both continue. The request is not repeated inside the same startup sequence.
9. Wait `Splash_Duration` seconds (missing value is 0).
10. If `After_Splash_Ad_Show` is false, continue immediately. Type `inter` loads `After_Splash_Interstitial_Id` with no loading dialog. Any other type loads `App_Open_Id` as an app-open ad.
11. `continueAfterVisibleSplash` keeps the existing five-screen flow. Incomplete onboarding calls `openFirstScreen`. Completed onboarding stays in this `MainActivity` and opens the scan graph only when the app is not the default home and `Redirect_Home_Launcher` is false. Otherwise `openMain` runs.

Screen-flow returns to `MainActivity` set `skip_startup_splash` and do not show the splash again. A widget or reminder intent that is not a screen-flow return does show this splash, because source starts `SplashActivity` for those intents.

Widget routing:

- `scan` opens the existing `ScannerActivity`.
- `create` opens the existing Create hub.
- The pending value is attached on the non-default-home final intent, including redirect-to-launcher-home, matching source. Default-home does not attach it.
- When startup stays in the current QR activity, the pending value is consumed there.
- `LauncherHomeActivity` opens Scan or Create only when its own intent already carries `TARGET_SCREEN`.

`createLaunchIntent` targets `MainActivity`, not a new splash activity. Widget providers are not registered in this phase.

## Manifest changes

- Added `android.permission.POST_NOTIFICATIONS`.
- No `SplashActivity`.
- `MainActivity` remains MAIN + LAUNCHER.
- `LauncherHomeActivity` remains MAIN + HOME + DEFAULT.
- No production AdMob app id was added. The existing sample app id meta-data was left as it was.

## Dependencies added

- `com.android.installreferrer:installreferrer` 2.2

No Crashlytics, Firebase Messaging, Facebook mediation, or Gson dependency was added.

## Permissions added

- `POST_NOTIFICATIONS`

Phone, camera-from-the-permission-screen, overlay, and the other source permission requests were not added.

## Remote Config keys used

Read from `Screen.SplashScreen`, with the source defaults:

- `Splash_Duration` (int, default 0)
- `Splash_Ad_Show` (boolean, default false)
- `Splash_Ad_Type` (missing becomes `banner`)
- `Splash_Banner_Id` (default empty)
- `Splash_Native_Id` (default empty)
- `After_Splash_Ad_Show` (boolean, default false)
- `After_Splash_Ad_Type` (missing becomes `inter`)
- `After_Splash_Interstitial_Id` (default empty)

App-open after splash uses the existing `App_Open_Id` key, as source does.

`paid_user` is selected only when the stored referrer matches the source converters and these string values:

- `marketing_converter1` = `gclid`
- `utm_source_apps_facebook_com` = `utm_source=apps.facebook.com`
- `utm_source_apps_instagram_com` = `utm_source=apps.instagram.com`
- `utm_source_marketing` = `utm_source=marketing`

Otherwise `normal_user` is used. The five-screen `Show_Screen_Flow` path is unchanged.

## Analytics events added

- `POST_NOTIFICATIONS_GRANTED` through the existing `TrackOnce` / `AppUtils.trackScreenOnce` path, only when the permission result is granted. A failed log does not set the one-time flag and does not crash.

No other splash or permission events were added.

## Fallback behavior

- VPN still active on Recheck: dialog stays. Startup does not continue.
- Remote Config failure, empty payload, or missing Firebase: existing local screen-flow fallback runs, splash ad flags stay off, and navigation continues.
- Splash ad flag off, no network, empty unit id, missing AdMob app id, or load failure: the splash slot is hidden or left unused, and startup continues.
- After-splash flag off, empty id, or load/show failure: navigation continues. No second ad request is added.
- Quiz ad priority still uses the existing `AdPlacement` path, which hides the Google slot and continues. Quiz layouts were not migrated.
- Notification grant and denial both continue.
- Install date already present is not overwritten.
- Install Referrer setup failure, disconnect, or `RemoteException` does not block startup. The URL is stored only on `InstallReferrerResponse.OK`.
- The source activity-private boolean check on `Activity.getReferrer()` is copied. Splash never writes that boolean. Not found in the splash method as a write.

## Build result

`:app:assembleDebug` succeeded.

`BUILD SUCCESSFUL in 3m 38s` (29 Sep 2026).

Pre-existing notes that are not failures: `stripDebugDebugSymbols` could not strip some native libraries, and `CreateHubScreen.kt` reports the deprecated `Icons.Outlined.Chat` warning.

## Unit test result

`:app:testDebugUnitTest` succeeded. 12 suites, 67 tests, 0 failures, 0 errors, 0 skipped.

## Device verification status

Not run. No device or emulator install was performed in this phase. Live ads, live Remote Config, VPN, notification permission, and referrer were not verified on a device.

## Remaining startup limitations

- Source logo animation timings (700 / 250 / 600 / 350 / 600 ms) are not played. They do not control source navigation. The visible splash is the existing Compose splash for the source wait, which can be only as long as `Splash_Duration` plus the permission and ad callbacks. Default duration is 0.
- `QrScannerApplication` still starts Remote Config during `Application.onCreate`. The splash callback joins that fetch. Install Referrer is started later and is not awaited, so `paid_user` can be missed on that first fetch when the referrer returns after config is applied. Source also does not wait for the referrer before fetching.
- Splash does not call `MobileAds.initialize` or `FirebaseApp.initializeApp` a second time. Ads still initialize only through `AdPlacement.initializeIfConfigured` when an app id is present. Firebase still initializes through the existing Remote Config helper.
- Full reminder scheduling, storage, boot receive, and the reminder notification channel are not implemented. Only the launch cancel/strip handoff is present. A reminder notification cannot be produced by this phase.
- Widget providers, pin flow, and widget layouts are not implemented. Only capture, attach, and routing to the existing Scan or Create activities are present.
- The splash native/banner shimmer uses an empty `View`. Source `shimmer_small_ad` was not part of the earlier layout migration.
- Call-end, quiz, sub page, language list, permission-screen phone/camera/overlay requests, default-home popup, FCM, Crashlytics, Facebook mediation, launcher settings changes, right-swipe interstitial, and QR product changes were not started.
