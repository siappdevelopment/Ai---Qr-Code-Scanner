# Phase I — Device Verification

No device flow was executed. A Pixel 4a was attached at the start of this check, then disconnected before the debug APK could be installed. After an adb restart and a later retry, `adb devices` stayed empty. No Android Virtual Device is configured. No code was changed.

## 1. Device model

NOT VERIFIED.

`adb devices -l` once listed `08111JEC219613` as `Pixel_4a` (`sunfish`). The next commands read Android 13, API 33, and then the device was gone. The APK was not installed on it.

## 2. Android version

NOT VERIFIED as a test device. The brief reading before disconnect was Android 13, API 33.

## 3. Build tested

`:app:assembleDebug` and `:app:testDebugUnitTest` both succeeded (`BUILD SUCCESSFUL` in 24s).

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`  
Timestamp: 29 Sep 2026, 10:01 AM  
Size: 55,979,496 bytes  
`applicationId`: `com.qrcode.scanner`  
`versionName`: `1.0` (`versionCode` 1)

This APK was not installed.

## 4. Fresh install result

NOT VERIFIED.

## 5. Splash result

NOT VERIFIED.

## 6. Onboarding result

NOT VERIFIED. Fresh, partial, and complete screen-flow paths were not opened on a device.

## 7. Completed onboarding result

NOT VERIFIED.

## 8. Default Home success result

NOT VERIFIED.

## 9. Default Home cancel result

NOT VERIFIED.

## 10. LauncherHome result

NOT VERIFIED.

## 11. QR app return result

NOT VERIFIED.

## 12. Duplicate Splash result

NOT VERIFIED.

## 13. Remote Config fallback result

NOT VERIFIED on a device. `app/google-services.json` is still absent. No Firebase file was added.

## 14. Firebase / Analytics failure-safety result

NOT VERIFIED on a device. Live Analytics was not delivered and was not claimed.

## 15. Ad failure-safety result

NOT VERIFIED on a device. Live ads were not loaded and were not claimed.

## 16. QR feature regression result

NOT VERIFIED. Scanner, camera permission, scan result, Create, History, Settings, and back navigation were not exercised.

## 17. Process restart result

NOT VERIFIED.

## 18. Logcat findings

NOT VERIFIED. Logcat was not captured because the app was not launched.

## 19. Android 7–9 status

NOT VERIFIED. No API 24–28 device or emulator was available. `emulator -list-avds` returned no virtual devices.

## 20. Android 10+ status

NOT VERIFIED. The Pixel 4a disconnected before install, so API 33 behavior was not exercised.

## 21. Reproduced bugs

None. No device session reached the app, so no regression was reproduced.

## 22. Code changes

None.

## 23. Final build result

`:app:assembleDebug` succeeded.

## 24. Final unit-test result

`:app:testDebugUnitTest` succeeded. No tests were added.
