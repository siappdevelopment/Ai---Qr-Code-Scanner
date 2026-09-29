# Phase 2 — Source permission flow

Phase 2 migrates the source `PermissionActivity` permission sequence onto the existing Java/XML permission screen. The five-screen list is unchanged. Scanner, Create, History, QR generation, result screens, and QR Settings were not modified.

## Source files used

- `activities/PermissionActivity.java`
- `common/AppUtils.java` permission checks and one-time events: `hasPhoneStatePermission`, `trackPhoneStatePermissionGrantedOnce`, `hasCameraPermission`, `trackCameraPermissionGrantedOnce`, `hasOverlayPermission`, `trackOverlayPermissionGrantedOnce`
- Existing target layout `activity_permission.xml` and `img_permission` (already present; not redesigned)
- Existing `AdPlacement.showSlot` for `PermissionDefault_*` banner or native size `small`

Source `continueAfter(..., "Permission")` maps to the existing `ScreenFlowNavigation.continueAfter(..., ScreenFlowConfig.SCREEN_PERMISSION)`.

## Target files changed

- `app/src/main/java/com/qrcode/scanner/launcher/activities/PermissionActivity.java`
- `app/src/main/java/com/qrcode/scanner/launcher/common/AppUtils.java`
- `app/src/main/AndroidManifest.xml`

## Permission flow and order

The button starts the flow once. A second tap while the flow is active does nothing.

1. `READ_PHONE_STATE`
2. `CAMERA`
3. Overlay settings: `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` for this package

A denied runtime result still advances to the next step. Opening the overlay settings screen does not count as a grant. After the settings result returns, the screen checks `Settings.canDrawOverlays` and continues either way.

If phone, camera, and overlay are already granted when the screen opens, and a flow is not already in progress, the screen completes without waiting for the button.

Process-death state is restored with the source keys: flow active, phone step completed, camera step completed, waiting for overlay settings, and paused for overlay settings. If both phone and camera steps were completed, overlay is requested again. If only the phone step was completed, camera is requested again.

On API 30+, the source navigation-bar hide remains.

## READ_PHONE_STATE

Requested with `ActivityResultContracts.RequestPermission`. If it is already granted, the request is skipped and the camera step is scheduled. A grant logs `READ_PHONE_STATE_GRANTED` once. A denial does not log that event and still continues to camera. No other phone permission is requested.

## CAMERA

Requested only after the phone step completes. If it is already granted, the request is skipped, `CAMERA_GRANTED` is logged once, and overlay is scheduled. A grant logs the same event. A denial does not log it and still continues to overlay.

`ScannerViewfinderScreen` still requests `CAMERA` itself. This screen uses `AppUtils.hasCameraPermission`, which is the same `checkSelfPermission` check. The scanner request UI was not changed. A grant here is the same permission the scanner already checks, so the scanner does not get a second, conflicting decision path.

## Overlay

`AppUtils.hasOverlayPermission` calls `Settings.canDrawOverlays` and returns false if that call throws.

The source `SDK_INT < M` branch is kept. This app’s minimum SDK is 24, so that branch does not run. On supported versions the screen uses the source API 23+ overlay check and settings intent.

If overlay is already granted, the screen continues and logs `OVERLAY_PERMISSION_GRANTED` only when the check is true.

If the settings intent cannot be launched, the screen continues without treating overlay as granted.

While the settings screen is open, `onPause` starts a 400 ms poll. If the permission becomes granted in the background, the activity is brought forward with `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_REORDER_TO_FRONT`, then the screen continues. `onResume` also checks the real permission after returning. The settings-result callback continues even when overlay is still denied, and the grant event is logged only when `canDrawOverlays` is true.

## Screen-flow integration

Completion calls `ScreenFlowNavigation.continueAfter` for `Permission`. That marks the permission step complete and opens the next incomplete screen. In the default list that is Default Home, then Intro. It does not skip those screens and does not start the launcher splash.

The existing permission ad slot is still shown from `PermissionDefault_Ad_Show`, `PermissionDefault_Ad_Type`, `PermissionDefault_Banner_Id`, and `PermissionDefault_Native_Id`.

## Manifest changes

Already present:

- `CAMERA`

Added:

- `READ_PHONE_STATE`
- `SYSTEM_ALERT_WINDOW`

Not added: call-end, FCM, reminder, widget, exact-alarm, or `USE_FULL_SCREEN_INTENT` permissions.

## Analytics events

Logged through existing `AppUtils.trackScreenOnce` (`analytics_events`, one successful log per event name):

- `READ_PHONE_STATE_GRANTED`
- `CAMERA_GRANTED`
- `OVERLAY_PERMISSION_GRANTED`

Each event is logged only when that permission is actually granted. A failed analytics log does not set the one-time flag.

## Source behavior accounted for

Compared with source `PermissionActivity`:

- Button starts phone, then camera, then overlay.
- Already-granted runtime permissions are skipped.
- All three already granted auto-continues.
- Deny still continues.
- Overlay settings failure still continues.
- Overlay grant is rechecked after return and by the 400 ms poll.
- Saved instance state restores an in-progress flow.
- Navigation-bar hide on API 30+ remains.
- Next screen is `continueAfter` for `Permission`.
- The three grant events are one-time and only after a real grant.

Source `DefaultActivity` also requests `READ_PHONE_STATE` before the Home role. That is the Default Home screen, not `PermissionActivity`. It was not changed in this phase.

## Build result

`:app:assembleDebug` succeeded.

`BUILD SUCCESSFUL in 19s`.

## Unit test result

`:app:testDebugUnitTest` succeeded. No failures and no errors.

## Device verification status

Not run. The permission dialogs, overlay settings return, and analytics events were not exercised on a device.

## Intentionally pending

- Default Home phone-state request inside source `DefaultActivity`
- Language catalog, `LocaleHelper`, and night mode
- Quiz, sub page, right-swipe interstitial, and process app-open
- Default Home popup, call-end, `PhoneCallStateService`, and FCM
- Reminders, `BootReceiver`, and widgets
- Crashlytics and Facebook mediation
- QR product changes
