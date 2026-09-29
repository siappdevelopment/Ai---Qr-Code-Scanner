# Phase G — Analytics / TrackOnce

Live Firebase Analytics is not available. The target still has no `google-services.json`, so Firebase is not initialized and events cannot be delivered. The code path is integrated and fails closed: a missing or failing Analytics call does not crash, block startup, change onboarding, change the Home role request, or change AdMob loading.

## 1. Firebase Analytics dependency

`com.google.firebase:firebase-analytics` from the existing Firebase BOM `34.12.0`.

No Crashlytics, Performance, Messaging, or separate Google Analytics SDK was added.

## 2. Firebase configuration status

Unavailable.

`app/google-services.json` is not in the target project. The Google Services plugin stays conditional and is not applied. Source Firebase project identifiers were not copied.

## 3. Analytics initialization

Source `MyApplication` does not call Firebase Analytics. Initialization is the automatic Firebase setup that the Google Services plugin provides.

`QrScannerApplication` still only calls `AdPlacement.initializeIfConfigured` and `RemoteConfigHelper.fetchRemoteConfig`. There is no synchronous Analytics call in `onCreate()`.

## 4. TrackOnce implementation

Adapted `com.qrcode.scanner.launcher.common.TrackOnce`.

It delegates to `AppUtils.trackScreenOnce`, matching the source class.

## 5. TrackOnce persistence

SharedPreferences file `analytics_events`, private mode. Each event name is a boolean key. Default is false. The key is set true only after `logEvent` returns.

The file is separate from `screen_flow` and from launcher ad preferences. The source file name is unchanged because this app’s application id is different and the target did not already use that name.

Source `trackScreen` catches an exception and rethrows `RuntimeException`. The target swallows the exception and does not write the one-time flag, so a later trigger can try again. That avoids a crash and is not a network retry loop.

## 6. Exact source event names

| Event | Source call | Once |
| --- | --- | --- |
| `DEFAULT_HOME_APP_SET` | `TrackOnce.trackScreenOnce` | Yes |
| `DEFAULT_HOME_APP_CANCEL` | `TrackOnce.trackScreenOnce` | Yes |
| `READ_PHONE_STATE_GRANTED` | `AppUtils.trackPhoneStatePermissionGrantedOnce` | Yes |
| `CAMERA_GRANTED` | `AppUtils.trackCameraPermissionGrantedOnce` | Yes |
| `OVERLAY_PERMISSION_GRANTED` | `AppUtils.trackOverlayPermissionGrantedOnce` | Yes |
| `POST_NOTIFICATIONS_GRANTED` | Splash `TrackOnce.trackScreenOnce` | Yes |
| `FULL_NATIVE_LOAD` | `AppUtils.trackScreen` | No |
| `FULL_NATIVE_FAILED` | `AppUtils.trackScreen` | No |
| `CL_END_APP_OPEN_LOAD` | `AppUtils.trackScreen` | No |
| `CL_END_APP_OPEN_FAILED` | `AppUtils.trackScreen` | No |
| `CL_END_INTER_LOAD` | `AppUtils.trackScreen` | No |
| `CL_END_INTER_FAILED` | `AppUtils.trackScreen` | No |
| `ad_impression` | `AdPlacement.logAdRevenue` | No |

No other `logEvent` calls exist in the source.

## 7. Exact event triggers

`DEFAULT_HOME_APP_SET` is logged from `DefaultHomePromptHelper.completeSuccess` only when the host is active and `AppUtils.isDefaultHomeApp` is true. Launching the role request is not success.

`DEFAULT_HOME_APP_CANCEL` is logged from `completeCancelled` only when a selection attempt had started and the host is still active.

Both the in-launcher prompt and the onboarding `DefaultActivity` use this helper, so both surfaces use these triggers. Source `DefaultActivity` logged the same names on its own role path. That second path was not reintroduced.

## 8. Event parameters

One-time screen events use one parameter: `value` as a long, always `1`.

`ad_impression` uses:

- `ad_platform` = `Google Ad Manager`
- `currency` = `AdValue.getCurrencyCode()`
- `value` = micros divided by 1,000,000 as a double

## 9. Default Home events

`DEFAULT_HOME_APP_SET` and `DEFAULT_HOME_APP_CANCEL` only.

Phase D role behavior is unchanged. `AppUtils.createDefaultHomeRoleRequestIntent` is still the only role-intent creator. Logging does not show an ad and does not wait.

## 10. Onboarding events

The remote screen list does not read Analytics. No Language, Collection, Permission, Intro, skip, next, or back event exists in the source, so none was added.

Permission grant events exist in the source but are not called here. Phase E does not request phone state, camera, or overlay on the onboarding permission screen, and this phase does not add those requests.

## 11. Launcher events

No source event was found for launcher opened, an installed app name, drawer open, settings open, or launcher return. None was added.

Default Home events from the in-launcher helper are the only launcher analytics.

## 12. Ad-related analytics

`ad_impression` is migrated through `AdPlacement.logAdRevenue`.

Paid listeners are attached where the source attaches them for ads this app already loads: banner, adaptive banner, native, interstitial, app open, and the preloaded after-default show path. Load, show, failure, and navigation behavior from Phase F is unchanged. A failure inside `logAdRevenue` is ignored.

## 13. Events intentionally NOT migrated

- `READ_PHONE_STATE_GRANTED`, `CAMERA_GRANTED`, `OVERLAY_PERMISSION_GRANTED` — their source triggers request permissions this migration does not request.
- `POST_NOTIFICATIONS_GRANTED` — Splash only. Splash is not migrated.
- `FULL_NATIVE_LOAD`, `FULL_NATIVE_FAILED` — `ADSNativeFullDisplay` only. That display was not migrated.
- `CL_END_APP_OPEN_LOAD`, `CL_END_APP_OPEN_FAILED`, `CL_END_INTER_LOAD`, `CL_END_INTER_FAILED` — call-end only. Call-end is not migrated.
- Quiz, FCM, widgets, install referrer, and the source QR pager have no migrated analytics calls.

## 14. Data minimization

Parameters are the source parameters only. No phone number, SMS, contacts, installed-app list, location, camera contents, QR payload, device id, or user id is added.

## 15. Failure and fallback

If Firebase is missing, `getInstance` throws, or `logEvent` fails:

- the exception is caught
- the one-time flag stays false when the screen event was not logged
- onboarding, Default Home, the launcher, and ads continue
- there is no dialog and no retry loop

## 16. Files created

- `app/src/main/java/com/qrcode/scanner/launcher/common/TrackOnce.java`
- `LAUNCHER_PHASE_G_IMPLEMENTATION.md`

## 17. Files modified

- `app/src/main/java/com/qrcode/scanner/launcher/common/AppUtils.java`
- `app/src/main/java/com/qrcode/scanner/launcher/helpers/DefaultHomePromptHelper.java`
- `app/src/main/java/com/qrcode/scanner/launcher/common/AdPlacement.java`
- `app/src/main/java/com/qrcode/scanner/QrScannerApplication.java` (comment only)
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`

## 18. Gradle changes

`firebase-analytics` added on the existing BOM. No new plugin.

## 19. Manifest changes

None.

## 20. Build result

`:app:assembleDebug` succeeded.

## 21. Unit-test result

`:app:testDebugUnitTest` succeeded. No new tests were added.

## 22. Device verification status

Not performed.

## 23. Live Firebase Analytics

Not verified. Analytics code is integrated. Firebase project configuration is not available. Events are implemented in code. They were not observed on a device.

## Source mapping

| Source | Event | Trigger | Target | Decision |
| --- | --- | --- | --- | --- |
| `TrackOnce` | caller’s name | one-time guard then `AppUtils.trackScreen` | `launcher/common/TrackOnce` | Adapt |
| `AppUtils.trackScreenOnce` / `trackScreen` | event name argument | prefs `analytics_events` then `logEvent` with `value=1` | `AppUtils` | Adapt |
| `DefaultHomePromptHelper` | `DEFAULT_HOME_APP_SET` / `CANCEL` | confirmed default home, or cancel after a started attempt | same helper | Adapt |
| `DefaultActivity` own track calls | same two names | source role path | helper used by target `DefaultActivity` | Adapt via helper. Do not add a second role path |
| Permission and Default phone-state helpers | grant event names | permission results | none | Do not migrate |
| `SplashActivity` | `POST_NOTIFICATIONS_GRANTED` | notification grant | none | Do not migrate |
| `ADSNativeFullDisplay` | `FULL_NATIVE_LOAD` / `FAILED` | full native load | none | Do not migrate |
| `CallEndBackAd` | `CL_END_*` | call-end ads | none | Do not migrate |
| `AdPlacement.logAdRevenue` | `ad_impression` | paid event on a loaded Google ad | `AdPlacement.logAdRevenue` | Adapt |
