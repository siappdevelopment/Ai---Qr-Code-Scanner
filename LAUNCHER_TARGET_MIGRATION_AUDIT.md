# Launcher Target Migration Audit

Investigation only. This file documents the current Jetpack target and how the existing Java/XML Source Launcher can later be integrated into it.

No application code, XML, Gradle, Manifest, UI, or dependencies were changed.

Source of truth for the working launcher (unchanged, not modified by this audit):

- Project: `E:\Srushti\My Project\14. QR Code\QRCodeScanner (1)\QRCodeScanner`
- Package / applicationId: `com.qrscanner.barcodescanner`
- Prior source audit: `LAUNCHER_SOURCE_MIGRATION.md` in that project
- UI: XML + Android Views. Language: Java. Compose: not present in the source.

Target of this audit:

- Project: `QR Code & Scanner` (this workspace)
- Package / applicationId: `com.qrcode.scanner`
- UI: Jetpack Compose. Language: Kotlin. Java sources: not present.

Integration approach this audit recommends, and does not implement:

Keep the Source Launcher classes as Java. Host their XML layouts as Activities beside the existing Compose UI. Start those Activities with `Intent` from Kotlin. Do not convert the launcher to Compose in the migration. Do not replace the Compose scanner, create, history, or settings product with the source QR pager.

---

# 1. Current project architecture

| Item | Actual value |
| --- | --- |
| Namespace | `com.qrcode.scanner` (`app/build.gradle.kts`) |
| applicationId | `com.qrcode.scanner` |
| Root project name | `QR Code & Scanner` (`settings.gradle.kts`) |
| Modules | One module: `:app`. No library modules |
| Kotlin | All app, unit-test, and androidTest sources under `app/src` are Kotlin |
| Java | Not found in this target |
| minSdk | 24 |
| targetSdk | 36 |
| compileSdk | 37 (`release(37)`, minorApiLevel 0) |
| versionCode / versionName | 1 / `1.0` |
| Java bytecode | 11 |
| AGP | 9.1.1 |
| Kotlin | 2.2.10 |
| Compose | `buildFeatures.compose = true`, plugin `org.jetbrains.kotlin.plugin.compose` |
| Compose BOM | `2024.09.00` |
| Navigation | `androidx.navigation:navigation-compose` 2.9.0 |
| Application subclass | Not found in this target. Manifest `<application>` has no `android:name` |

## Compose setup

- `MainActivity` extends `AppCompatActivity` (chosen so `AppCompatDelegate` per-app locales recreate the activity).
- `onCreate` calls `enableEdgeToEdge()` and `setContent { QRCodeScannerTheme { ScanPulseNavHost() } }`.
- Theme is Compose `QRCodeScannerTheme` in `ui/theme/Theme.kt`, driven by DataStore `app_theme`.
- XML theme `Theme.QRCodeScanner` in `res/values/themes.xml` is `Theme.AppCompat.Light.NoActionBar`. It is the window theme for every activity. It is not the Electric Cobalt palette. The palette lives in Compose `Color.kt`.

## Activities

All of these are Kotlin. Secondary flows are separate Activities with Compose `setContent`, not NavHost destinations.

| Class | Manifest exported | Role |
| --- | --- | --- |
| `com.qrcode.scanner.MainActivity` | true | Only activity with `MAIN` + `CATEGORY_LAUNCHER`. Hosts the NavHost |
| `ui.screens.create.CreateActivity` | false | Create hub |
| `ui.screens.create.CommonQrFormActivity` | false | QR form |
| `ui.screens.create.WifiQrActivity` | false | Wi-Fi QR |
| `ui.screens.create.QrPreviewActivity` | false | Preview / export |
| `ui.screens.create.QrCustomizationActivity` | false | Style |
| `ui.screens.scan.ScannerActivity` | false | Camera scanner, portrait |
| `ui.screens.scan.ContinuousBatchResultActivity` | false | Batch results |
| `ui.screens.scan.ScanResultActivity` | false | Single result |
| `ui.screens.scan.GalleryCropActivity` | false | Gallery crop |
| `ui.screens.scan.DetectionErrorActivity` | false | Detection error |
| `ui.screens.history.FavoritesActivity` | false | Favorites |
| `ui.screens.history.HistoryActivity` | false | History list |
| `ui.screens.history.HistoryDetailActivity` | false | History detail |

## Navigation

File: `ui/navigation/ScanPulseNavHost.kt`  
Routes: `ui/navigation/AppDestination.kt`

`NavHost` `startDestination` is `splash`.

After splash, navigation enters nested graph `main_graph`. That graph’s `startDestination` is `scan`, not `home`.

Routes actually registered:

| Route | UI |
| --- | --- |
| `splash` | `SplashScreen` |
| `home` (inside `main_graph`) | `HomeScreen` |
| `scan` (inside `main_graph`) | `ScanScreen` |
| `settings` (inside `main_graph`) | `SettingsScreen` |
| `camera_permission` | `PlaceholderScreen("Camera Permission")` |
| `qr_customization` | `PlaceholderScreen` |
| `qr_preview_export` | `PlaceholderScreen` |
| `language` | `LanguageScreen` |
| `about` | `AboutScreen` |

`AppDestination` also declares `create`, `history`, `gallery_crop`, `detection_error`, and `scan_result`. Those routes are not registered in `ScanPulseNavHost`. Create, history, gallery crop, detection error, and scan result open Activities through `CreateQrIntents`, `HistoryIntents`, and `ScanIntents`.

Bottom bar (`ScanPulseBottomBar`) is shown for `home`, `scan`, and `settings` only.

## ViewModels and repositories

ViewModel classes: Not found in this target.

| Type | File | Used for |
| --- | --- | --- |
| `HistoryRepository` | `data/history/HistoryRepository.kt` | Room scan/create history. Home recent scans read this |
| `SettingsRepository` | `data/settings/SettingsRepository.kt` | DataStore file `scanpulse_settings` |
| `HistoryDatabaseProvider` / `AppDatabase` | `data/history/` | Room |

Settings keys in `SettingsKeys`: `vibrate_on_detection`, `beep_on_detection`, `auto_open_urls`, `continuous_batch_scan`, `default_qr_ecc`, `default_qr_output_format`, `app_theme`.

There is no repository for onboarding, default home, ads, or remote config.

---

# 2. Current startup flow

```
Process start
  (no Application.onCreate)
        ↓
MainActivity  MAIN + CATEGORY_LAUNCHER
        ↓
ScanPulseNavHost
        ↓
SplashScreen
  fixed delay 1_400 ms
  onFinished()
        ↓
main_graph  (splash removed from the back stack)
        ↓
Scan tab
```

## Splash

File: `ui/screens/splash/SplashScreen.kt`

- Compose only. No XML splash layout.
- `LaunchedEffect` waits 1400 ms, then calls `onFinished`.
- The delay is a local constant. It is not Remote Config `Splash_Duration`.
- Copy on screen: “ScanPulse”, “Instant QR & Barcode Intelligence”, “ENGINE READY”, “Ready to Scan”, “100%”, “V2.4.0”.
- No ads, no Firebase, no permission request, no VPN check, no install referrer.
- `ScanPulseNavHost` then navigates to `main_graph` and pops `splash` inclusive.

## Onboarding

Not found in this target.

No intro activities, no language/collection/permission/default-home first-run list, no `intro_completed` flag, no `Show_Screen_Flow`.

## First-launch logic

Not found in this target.

Every cold start shows the 1400 ms splash and then the Scan tab. DataStore settings use product defaults when keys are absent. Those defaults do not choose the next screen.

## Home / MainActivity

`MainActivity` is the task root and the icon entry. It is not a device HOME activity.

After splash, the visible tab is Scan. Home is a tab the user opens from the bottom bar.

`HomeScreen` (`ui/screens/home/HomeScreen.kt`) is the QR dashboard (Stitch “Home Dashboard (White Theme)”). It shows a scanner hero, quick tools, and recent Room history. It does not list installed apps, does not draw a wallpaper home, and does not request `ROLE_HOME`.

---

# 3. Current Launcher / Default Home implementation

Not found in this target.

| Source behavior | Target |
| --- | --- |
| `RoleManager` / `ROLE_HOME` | Not found |
| `isDefaultHomeApp` / `resolveActivity(CATEGORY_HOME)` | Not found |
| `createRequestRoleIntent` | Not found |
| HOME chooser for API 24–28 | Not found |
| Activity with `CATEGORY_HOME` + `CATEGORY_DEFAULT` | Not found |
| `singleTask`, empty `taskAffinity`, `excludeFromRecents`, `stateNotNeeded`, `clearTaskOnLaunch` on a home activity | Not found |
| App drawer, `queryIntentActivities(MAIN/LAUNCHER)`, icon cache, sort prefs | Not found |
| Package add/remove receiver | Not found |
| `<queries>` for `MAIN` + `LAUNCHER` | Not found |
| Launcher settings activity | Not found |

The word “launcher” in this target means the app icon (`CATEGORY_LAUNCHER` on `MainActivity`, mipmap `ic_launcher`) and Compose `rememberLauncherForActivityResult` for camera, gallery, and activity results. Those are not a default-home launcher.

Manifest services: one disabled `androidx.appcompat.app.AppLocalesMetadataHolderService` with `autoStoreLocales=true`. It stores per-app locales. It is not a launcher service.

Manifest receivers: Not found.

---

# 4. Current Firebase / Remote Config

Not found in this target.

| Item | Target |
| --- | --- |
| `google-services.json` | Not found |
| Google services Gradle plugin | Not found |
| `firebase-config`, `firebase-analytics`, `firebase-crashlytics`, `firebase-messaging` | Not found |
| `FirebaseApp`, `FirebaseRemoteConfig` | Not found |
| `res/xml/default_config.xml` | Not found |
| Remote keys `QRScanner_<versionCode>`, `Show_Screen_Flow`, `Redirect_Home_Launcher`, `Screen.*` | Not found |
| Onboarding remotely controlled | No. There is no onboarding |

Splash duration, next screen, and ads are not remote.

---

# 5. Current Ads

Not found in this target.

| Item | Target |
| --- | --- |
| `play-services-ads` | Not found |
| AdMob `APPLICATION_ID` meta-data | Not found |
| Banner, native, interstitial, app-open, rewarded | Not found |
| Facebook mediation / Audience Network / shimmer | Not found |
| Splash ad, after-splash ad, intro ad, default-home ad, launcher click/back ad | Not found |
| Remote-config ad keys | Not found. This target has no remote config |

No ad unit ids or AdMob app ids exist in this target to redact.

---

# 6. Current Analytics

Not found in this target.

No Firebase Analytics, Crashlytics calls, or other event logger was found in app Kotlin.

Events the source logs around splash, onboarding, and default home have no target equivalent:

| Source event | Where the source logs it | Target |
| --- | --- | --- |
| `POST_NOTIFICATIONS_GRANTED` | `SplashActivity` | Not found |
| `READ_PHONE_STATE_GRANTED` | `AppUtils` | Not found |
| `CAMERA_GRANTED` | `AppUtils` | Not found |
| `OVERLAY_PERMISSION_GRANTED` | `AppUtils` | Not found |
| `DEFAULT_HOME_APP_SET` | `AppUtils.navigateAfterDefaultAppSetup`, `DefaultActivity`, `DefaultHomePromptHelper` | Not found |
| `DEFAULT_HOME_APP_CANCEL` | same paths when the role is not held | Not found |
| `ad_impression` | `AdPlacement.logAdRevenue` | Not found |

`AppUtils.trackScreen` writes the event name to Firebase Analytics and stores a once-flag in SharedPreferences `analytics_events`. That class is not in this target.

---

# 7. Manifest

File: `app/src/main/AndroidManifest.xml`

## Permissions

| Permission | Present |
| --- | --- |
| `CAMERA` | Yes |
| `VIBRATE` | Yes |
| `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion` 28) | Yes |
| `INTERNET` | Not declared |
| `ACCESS_NETWORK_STATE` | Not declared |
| `POST_NOTIFICATIONS` | Not declared |
| `READ_PHONE_STATE` | Not declared |
| `SYSTEM_ALERT_WINDOW` | Not declared |
| `REQUEST_DELETE_PACKAGES` | Not declared |
| `AD_ID` | Not declared |

Features: `android.hardware.camera` and `camera.autofocus`, both `required=false`.

## Intent filters

Only `MainActivity` has an intent filter:

- `android.intent.action.MAIN`
- `android.intent.category.LAUNCHER`

`CATEGORY_HOME`, `CATEGORY_DEFAULT`, and `CATEGORY_BROWSABLE` are not declared.

## Other components

| Entry | Notes |
| --- | --- |
| Activities | The 14 activities in section 1. Only `MainActivity` is exported |
| Service | AppCompat locale metadata holder, `enabled=false`, `exported=false` |
| Receivers | Not found |
| Provider | `FileProvider`, authority `${applicationId}.fileprovider`, `@xml/file_paths` (`cache-path` `exports/`) |
| `<queries>` | Not found |
| AdMob meta-data | Not found |
| `android:name` on `<application>` | Not set |

Application attributes that do exist: `allowBackup=true`, `dataExtractionRules`, `fullBackupContent`, `icon`, `roundIcon`, `label`, `localeConfig=@xml/locales_config`, `supportsRtl`, `theme=@style/Theme.QRCodeScanner`.

---

# 8. Gradle / dependencies

Declared in `gradle/libs.versions.toml` and `app/build.gradle.kts`. Plugins on the app module: Android application, Kotlin Compose, KSP. Root `build.gradle.kts` applies those three with `apply false`.

## Already in the target (keep)

| Dependency | Version | Why it matters later |
| --- | --- | --- |
| `androidx.core:core-ktx` | 1.19.0 | Already newer than a typical XML app |
| `androidx.appcompat:appcompat` | 1.7.1 | Source uses 1.6.1. Keep 1.7.1. Java `AppCompatActivity` can use it |
| `lifecycle-runtime-ktx` / `lifecycle-runtime-compose` | 2.11.0 | Source `lifecycle-process` is 2.10.0. A later add should follow 2.11.0 |
| `activity-compose` | 1.13.0 | Compose activities. Source Java uses `activity` APIs (`OnBackPressedCallback`, Activity Result, `EdgeToEdge`) |
| Compose BOM, UI, Material3, Material icons extended | BOM 2024.09.00 | Existing product UI |
| `navigation-compose` | 2.9.0 | Existing NavHost. The Java launcher does not use it |
| CameraX | 1.4.2 | Scanner. Source CameraX is 1.4.1 and is not part of the launcher shell |
| ML Kit barcode-scanning | 17.3.0 | Scanner |
| Room runtime + ktx + compiler | 2.7.2 | History |
| `kotlinx-coroutines-android` | 1.10.2 | DataStore and history |
| ZXing core | 3.5.3 | QR encode/decode. Source also has 3.5.3 and does not need it for the launcher shell |
| `datastore-preferences` | 1.1.7 | Settings. Source launcher state is SharedPreferences, not this store |

## Possible conflicts if source libraries are added later

| Source library | Source version | Target today | Conflict |
| --- | --- | --- | --- |
| AGP | 9.1.0 | 9.1.1 | Keep the target plugin. Do not downgrade |
| `appcompat` | 1.6.1 | 1.7.1 | Keep 1.7.1 |
| `lifecycle-process` | 2.10.0 | runtime 2.11.0 | Align process artifact to 2.11.0 |
| `material` | 1.10.0 | Not a direct dependency. Compose Material3 is present | XML `Theme.Material3` from the source can pull a second Material stack next to Compose Material3. Add it only for XML launcher themes, and check resource attr clashes (`colorSurface`, `colorPrimary`) |
| CameraX / ML Kit / ZXing | Present on both sides | Already here | Do not add a second copy |
| Firebase BOM 34.12.0 | Source only | Absent | New. One `google-services.json` for this applicationId |
| `play-services-ads` 24.7.0 | Source only | Absent | New. One `APPLICATION_ID` meta-data |
| `sdp-android` 1.1.1 | Source layouts use `@dimen/_NNsdp` | Absent | Launcher XML will not inflate without it |
| Lottie 6.7.1 | Home fragment animations | Absent | Needed only if those Lottie views stay |
| Shimmer 0.5.0 | Ad placeholders | Absent | Needed only if ad shimmers stay |
| Install Referrer 2.2 | `paid_user` vs `normal_user` | Absent | Needed if that profile split stays |
| Facebook mediation 6.21.0.0 and Audience Network 6.21.0 | Optional in source | Absent | Optional later |

`activity-ktx` is not declared by name. `activity-compose` brings `activity`. Confirm `EdgeToEdge` and `ActivityResultContracts` resolve for Java before adding a duplicate `activity-ktx`.

`fragment` and `viewpager2` are not direct dependencies. `LauncherHomeActivity`’s pager needs both later.

## Dependencies that may need to be added later

Do not add them in this phase.

- `com.google.gms:google-services` (source plugin 4.4.4)
- `com.google.firebase:firebase-bom` 34.12.0
- `firebase-config`, `firebase-analytics`
- `com.google.android.gms:play-services-ads` 24.7.0
- `androidx.fragment:fragment`
- `androidx.viewpager2:viewpager2`
- `com.google.android.material:material` (for source XML theme parent and bottom sheet)
- `com.intuit.sdp:sdp-android` 1.1.1
- `com.airbnb.android:lottie` 6.7.1 if the home animations stay
- `com.facebook.shimmer:shimmer` 0.5.0 if ad placeholders stay
- `com.android.installreferrer:installreferrer` 2.2 if paid/normal profiles stay
- `androidx.lifecycle:lifecycle-process` aligned to 2.11.0
- Facebook mediation and Audience Network only if mediation stays
- Crashlytics and FCM only if those source features are brought over. They are not required for splash, onboarding, or the home pager

---

# 9. Resources

## Launcher-related layouts

Not found in this target. There is no `res/layout` directory. Splash, home, intro, permission, and default-home UI are not XML.

## Compose screens that share names with the source flow

| Target file | What it actually is |
| --- | --- |
| `ui/screens/splash/SplashScreen.kt` | 1400 ms Compose splash |
| `ui/screens/home/HomeScreen.kt` | QR dashboard |
| `ui/screens/settings/LanguageScreen.kt` | Settings language. English (US) and System only |
| `ui/screens/settings/SettingsScreen.kt` | Scanner settings, theme, DataStore |
| `ui/screens/common/PlaceholderScreen.kt` | Unused-from-startup placeholders, including “Camera Permission” |

## Drawables and mipmaps present

| Path | Role |
| --- | --- |
| `res/drawable/ic_launcher_foreground.xml` | Adaptive icon |
| `res/drawable/ic_launcher_background.xml` | Adaptive icon |
| `res/mipmap-anydpi-v26/ic_launcher.xml` | Icon |
| `res/mipmap-anydpi-v26/ic_launcher_round.xml` | Round icon |

No intro images, no `ic_app_logo`, no launcher dock icons, no quiz icons.

`Font.kt` references `R.font.plus_jakarta_sans_regular`, `medium`, `semibold`, `bold`, and `extrabold`. A file search under this project did not return those font files or any `.ttf` / `.otf`. Source layouts use `@font/sans_trial_regular`, `sans_trial_medium`, and `sans_trial_semi_bold`. Those source font names are not in this target.

## Strings, colors, themes, XML

| File | Contents |
| --- | --- |
| `res/values/strings.xml` | `app_name` = “QR Code & Scanner” only |
| `res/values/colors.xml` | `cobalt_primary`, `cobalt_accent`, `page_background`, `white`, `black` |
| `res/values/themes.xml` | `Theme.QRCodeScanner` parent `Theme.AppCompat.Light.NoActionBar` |
| `res/values-night/` | Not found |
| `res/xml/locales_config.xml` | `en-US` only |
| `res/xml/file_paths.xml` | cache `exports/` |
| `res/xml/backup_rules.xml` | backup |
| `res/xml/data_extraction_rules.xml` | backup |
| `attrs.xml`, `dimens.xml`, navigation XML, menu XML | Not found |

Most visible copy is hardcoded in Compose, not in `strings.xml`.

---

# 10. Conflicts

## Splash

| | Source | Target |
| --- | --- | --- |
| Entry | `SplashActivity`, XML `activity_splash.xml`, `MAIN` + `CATEGORY_LAUNCHER` | Compose `SplashScreen` inside `MainActivity`, which holds `CATEGORY_LAUNCHER` |
| Timing | Remote `Splash_Duration` seconds, after Remote Config | Fixed 1400 ms, no network |
| Next screen | `ScreenFlowNavigation.openFirstScreen` | Always `main_graph`, whose start is the Scan tab |
| Side effects | Ads init, Firebase, install referrer, notification permission, VPN dialog | None |

Putting both splash implementations on `CATEGORY_LAUNCHER` would show two icons or skip one of the flows. The Compose splash cannot run the source timer, ads, or screen list without a bridge.

## Onboarding

Source onboarding is a remote list: Language, Collection, Permission, DefaultHome, Intro, gated by SharedPreferences `screen_flow` / `intro_completed`.

This target has no onboarding. Its `LanguageScreen` is a settings page for `en-US` and System via `AppCompatDelegate`. It does not match `LanguageActivity` (many languages, ads, `language_flow_completed`).

`PermissionActivity` requests `READ_PHONE_STATE`, `CAMERA`, and overlay. This target requests camera inside the scanner UI. A first-run phone-state and overlay step does not exist. The NavHost placeholder `camera_permission` is not the source permission screen.

## Home

| | Source | Target |
| --- | --- | --- |
| Class | `LauncherHomeActivity` + `LauncherHomeFragment` | `HomeScreen` inside `MainActivity` |
| Job | Device home: wallpaper, dock, drawer, launch other apps | QR dashboard and recent scans |
| Manifest | `MAIN` + `HOME` + `DEFAULT`, `Theme.Home`, `singleTask`, empty task affinity | No HOME filter. Opaque Compose theme |
| Pager page 0 | Source `MainContainerFragment` (source QR UI) | Already implemented as Compose Scan / Create / History / Settings |

`HomeScreen` must stay the QR dashboard. `LauncherHomeActivity` must be a new HOME activity. The source pager page that embeds the old QR UI should not be copied over this Compose app.

Source home shortcuts (scan, create, history, settings) need to call this target’s `ScannerActivity`, `CreateActivity`, `HistoryActivity`, and settings route. They currently call the source pager.

## RoleManager / default home

This target has no role request. Adding a second HOME activity later, or requesting `ROLE_HOME` from both `DefaultActivity` and a Compose screen, would make the result ambiguous. The source ignores `resultCode` and polls `isDefaultHomeApp`. One request path should own that poll: the existing Java `DefaultActivity` and `DefaultHomePromptHelper`.

`ScreenFlowNavigation` opens the source `MainActivity` when the app is not the default home and `Redirect_Home_Launcher` is false. In this target that destination has to be `com.qrcode.scanner.MainActivity`. The source `MainActivity` must not be copied.

## Firebase / Remote Config

This target has no Firebase. The source expects:

- Its own `google-services.json` (do not copy that file into this app)
- Parameter name `QRScanner_` + versionCode (both apps are versionCode 1 today, so the name is `QRScanner_1`)
- `MyApplication.fetchAd()` after config. This target has no `Application` class, so a copied `instanceof MyApplication` check would fail

`minimumFetchIntervalInSeconds(0)` and the 12-hour launcher refetch are source behavior to preserve if `RemoteConfigHelper` is moved, not behavior to invent on the Compose splash.

## Ads

This target has no ads SDK and no `APPLICATION_ID`. The source `AdPlacement` class also serves QR-screen and call-end placements. Copying the class whole pulls those call sites. Call-end (`ClEndActivity`, call screening, `ensureClEndConfig`) is outside this target.

`shouldSkipAppOpenAd()` in the source always skips the foreground app-open show. Moving `MyApplication` keeps that behavior.

## Class and package names

| Name | Risk |
| --- | --- |
| `com.qrscanner.barcodescanner` vs `com.qrcode.scanner` | Source Java uses `com.qrscanner.barcodescanner.R`. This module’s `R` is `com.qrcode.scanner.R`. Copied Java must be repackaged under the target namespace (a subpackage is fine) |
| `MainActivity` | Both apps have one. They are different classes. Keep the target class. Do not copy the source class |
| `Theme.QRCodeScanner` | Both apps define this style. Parents differ (source Material3 DayNight vs target AppCompat Light) |
| `app_name` | Both define it. Keep the target string unless a later product decision changes the label |
| `ic_launcher_foreground` / `ic_launcher_background` | Both apps have these drawables. Keep the target icons |
| FileProvider authority | Both use `${applicationId}.fileprovider`. Application ids differ, so the authorities differ as long as `applicationId` stays `com.qrcode.scanner` |
| `LanguageActivity` vs `LanguageScreen` | Different types. A copied `LanguageActivity` does not replace settings language by itself, but two language systems (`language` SharedPreferences vs `AppCompat` locales) will diverge |
| Theme storage | Source `APP_PREF` / `pref_theme` and `ThemeUtils` vs target DataStore `app_theme` (`light` / `dark` / `system`) |

## Manifest

| Topic | Conflict |
| --- | --- |
| `CATEGORY_LAUNCHER` | Today on target `MainActivity`. Source puts it on `SplashActivity` |
| `CATEGORY_HOME` + `DEFAULT` | Absent here. Must be added on a new `LauncherHomeActivity` only |
| Two MAIN activities | Source already uses two: splash is the icon, home is the role. This target has one MAIN. The later manifest should keep a single LAUNCHER filter and a single HOME filter |
| Permissions | Camera already declared. Phone state, overlay, notifications, uninstall, and ad id are new and broader than the current scanner |
| `<queries>` MAIN/LAUNCHER | Required for the drawer on API 30+. Absent here. Without it, `queryIntentActivities` will not see other apps |
| Backup | Target already has backup rules. Source screen-flow prefs are not in those rules. A later pass should decide whether `intro_completed` is backed up |

## Other source product surface

Do not bring over: call-end service and receiver, boot receiver, FCM service, QR widget providers, source scan/create activities, source `MainContainerFragment`.

This target already implements scan, create, history, favorites, settings, theme, and per-app language in Compose.

---

# 11. Migration mapping

Classifications:

- **REUSE** — this target already has the piece the flow should keep
- **MIGRATE** — bring the source Java/XML in as Java/XML, new to this target
- **ADAPT** — bring the behavior, but change the source so it targets this app’s activities, package, `R`, and final destination
- **DO NOT MIGRATE** — leave it in the source app
- **NOT FOUND IN TARGET** — this target has no equivalent today (the action column says what to do later)

| SOURCE LAUNCHER | TARGET JETPACK | Classification |
| --- | --- | --- |
| `SplashActivity` + `activity_splash.xml` | `SplashScreen.kt` inside `MainActivity` | ADAPT. Target splash is a different screen. Source splash logic (Remote Config, ads, screen list) is not in the Compose delay |
| `ScreenFlowNavigation` | Not found | ADAPT. Final destination must be this `MainActivity` or `LauncherHomeActivity`, not the source `MainActivity` |
| `IntroNavigation` + `Intro1/2/3Activity` + intro layouts/images | Not found | MIGRATE as Java/XML activities |
| `LanguageActivity` + `LanguageAdapter` | `LanguageScreen` + `AppLanguage` (settings, en-US / System) | ADAPT if the remote list still contains `Language`. Do not delete the settings screen |
| `CollectionActivity` | Not found | MIGRATE only if `Collection` stays in `Show_Screen_Flow` |
| `PermissionActivity` | Camera permission inside the scanner. Placeholder route `camera_permission` | ADAPT. The placeholder is not the source screen |
| `DefaultActivity` + `activity_default.xml` | Not found | MIGRATE |
| `AppUtils` role methods (`isDefaultHomeApp`, `createDefaultHomeRoleRequestIntent`, `openDefaultHomeChooser`, `navigateAfterDefaultAppSetup`, `buildLauncherHomeIntent`) | Not found | MIGRATE the role methods. ADAPT the class so QR share/save helpers in the same file are not required |
| `DefaultHomePromptHelper` | Not found | ADAPT. Source popup is started from `MainContainerFragment`, which should not move |
| `LauncherHomeActivity` + `activity_launcher_home.xml` + `Theme.Home` | `HomeScreen` is the QR dashboard, not a HOME activity | MIGRATE as a new HOME activity. REUSE `HomeScreen` for the QR product |
| `LauncherHomeFragment` + `fragment_launcher_home.xml` | Not found | ADAPT. Dock shortcuts must open this app’s scan/create/history/settings |
| `LauncherPagerAdapter` | Not found | ADAPT. Page 0 must not be source `MainContainerFragment` |
| `MainContainerFragment` | Compose Scan / Home / History / Settings | DO NOT MIGRATE |
| `SubContainerFragment` + weather layouts | Not found | MIGRATE only if pager page 2 stays. Otherwise DO NOT MIGRATE |
| `LauncherAppsBottomSheet` + package receiver | Not found | MIGRATE |
| `LauncherAppsAdapter` + `LauncherAppsHelper` + `LauncherAppsIconCache` | Not found | MIGRATE |
| `LauncherAppsModel` + `LauncherAppsDisplayItem` + `OnLauncherAppLongClickListener` | Not found | MIGRATE |
| `LauncherAppContextPopup` | Not found | MIGRATE |
| `LauncherSettingsHelper` + `LauncherSettingsActivity` | `SettingsScreen` is scanner settings | MIGRATE launcher settings separately. REUSE `SettingsScreen` for the QR product |
| `LauncherQuizGames` + `ic_qz_icon_*` | Not found | DO NOT MIGRATE unless quiz icons are explicitly in scope |
| `AdPlacement` | Not found | ADAPT. Keep splash, intro, default-home, and launcher placements. Leave call-end and source QR placements behind |
| `RemoteConfigHelper` + `default_config.xml` | Not found | ADAPT. Application cast and `QRScanner_<versionCode>` must match this app |
| `MyApplication` | No Application class | ADAPT. Add one Application. Do not register two. Skip call-end init unless call-end is in scope |
| `TrackOnce` + `AppUtils.trackScreen` | Not found | MIGRATE with Firebase Analytics |
| `VPNHelper` + `dialog_vpn_detect.xml` | Not found | MIGRATE only if the splash VPN block stays |
| `ReminderAlarmHelper` on splash, `WidgetNavigation` | Not found | DO NOT MIGRATE |
| `ThemeHelper` + `ThemeUtils` (`pref_theme`) | `AppThemeMode` + DataStore `app_theme` + `QRCodeScannerTheme` | DO NOT MIGRATE over the Compose theme. Launcher `Theme.Home` is separate and should be MIGRATE for the HOME activity only |
| Source `MainActivity` | `com.qrcode.scanner.MainActivity` | DO NOT MIGRATE the source class. REUSE the target class as the QR shell |
| `google-services.json` from the source | Not found | DO NOT MIGRATE. Create a file for `com.qrcode.scanner` later |
| AdMob app id and unit ids in the source | Not found | DO NOT MIGRATE the source values. Put this app’s ids in Firebase later |
| Call-end, FCM, boot receiver, QR widgets, source scan activities | Not found, and this app already has its own scanner | DO NOT MIGRATE |
| Compose `NavHost`, bottom bar, Room history, CameraX scanner, create flow, settings DataStore | Present | REUSE. Do not rewrite them as XML |
| `CATEGORY_LAUNCHER` on target `MainActivity` | Present | ADAPT later so only one activity has the icon filter |
| `FileProvider` | Present | REUSE. Authority already follows this `applicationId` |
| `CAMERA` permission | Present | REUSE |
| Fonts `plus_jakarta_sans_*` | Referenced by `Font.kt`. Binaries were not found by file search | REUSE the Compose font setup. MIGRATE `sans_trial_*` only for XML layouts that reference them |
| Target adaptive icon | Present | REUSE. Do not overwrite with the source mipmaps |

---

# 12. Recommended migration order for this target

1. Leave the source project unchanged. Leave this app’s Compose scanner, create, history, settings, Room, and CameraX behavior unchanged.
2. Decide the activity split before copying files: a new Java splash (or an adapted entry) owns `CATEGORY_LAUNCHER`; a new `LauncherHomeActivity` owns `HOME` + `DEFAULT`; existing `MainActivity` remains the QR Compose shell and loses the icon filter in that later manifest edit.
3. Add one `Application` class for this process. It is the place for `MobileAds.initialize`, language/theme restore that the XML flow needs, and the `fetchAd()` hook `RemoteConfigHelper` expects. Do not copy call-end setup unless that feature is in scope.
4. Add Gradle dependencies from section 8 only when implementation starts. Keep this module’s AGP, AppCompat, Lifecycle, CameraX, Room, and Compose versions. Align new Lifecycle artifacts to 2.11.0.
5. Copy launcher Java into this module under `com.qrcode.scanner` (subpackage), still as Java. Point `R` at this namespace. Prefix or rename resources that collide (`Theme.QRCodeScanner` must stay the target style; add `Theme.Home` beside it).
6. Copy XML layouts, drawables, strings, `Theme.Home`, `attrs` (`launcherBackgroundColor`), `default_config.xml`, and `sans_trial_*` fonts required by those layouts. Keep Compose screens on Plus Jakarta / theme colors they already use.
7. Manifest, still later: HOME activity attributes from the source (`singleTask`, empty `taskAffinity`, `excludeFromRecents`, `stateNotNeeded`, `clearTaskOnLaunch`, portrait, `Theme.Home`), `<queries>` for `MAIN`/`LAUNCHER`, AdMob meta-data for this app’s id, and the extra permissions only for the steps that stay in `Show_Screen_Flow`.
8. Retarget `ScreenFlowNavigation.buildFinalDestinationIntent` to this `MainActivity` and the new `LauncherHomeActivity`.
9. Retarget launcher dock shortcuts and any right-swipe QR entry to `ScannerActivity`, `CreateActivity`, `HistoryActivity`, and the existing settings route. Do not embed `MainContainerFragment`.
10. Wire first-run activities that remain in the default list (`Language`, `Collection`, `Permission`, `DefaultHome`, `Intro`). If a token is dropped, remove it from `getDefaultShowScreenFlow` and from `default_config.xml` in the same change. A token with no activity crashes on `startActivity`.
11. Add this app’s `google-services.json` and a Firebase parameter `QRScanner_` plus this `versionCode`, with the same JSON shape. Do not paste source secrets.
12. Enable ads only after Remote Config flags load. Empty unit ids already skip in the source loaders.
13. Skip the Compose `SplashScreen` delay when the Java splash owns cold start, so the user does not see two splashes. Keep `SplashScreen.kt` in the project until that handoff is implemented and checked.
14. Verify on API 24–28 (HOME chooser) and API 29+ (`RoleManager`). Verify a returning user (`intro_completed`) opens the final destination, and a new user walks only the screens still in the list.

---

# 13. Later file plan

Nothing in this list was created or edited except this audit.

## Files that will probably be created

Java (new files in this module, copied and repackaged, not converted):

- `SplashActivity` (or a thin Kotlin entry that immediately starts the Java splash — the source class itself should stay Java)
- `ScreenFlowNavigation`, `IntroNavigation`, `Intro1Activity`, `Intro2Activity`, `Intro3Activity`
- `LanguageActivity`, `LanguageAdapter` if Language stays in the list
- `CollectionActivity` if Collection stays
- `PermissionActivity` if Permission stays
- `DefaultActivity`
- `DefaultHomePromptHelper`
- `LauncherHomeActivity`, `LauncherHomeFragment`, `LauncherPagerAdapter`
- `LauncherAppsBottomSheet`, `LauncherAppsAdapter`, `LauncherAppsHelper`, `LauncherAppsIconCache`
- `LauncherAppsModel`, `LauncherAppsDisplayItem`, `OnLauncherAppLongClickListener`
- `LauncherAppContextPopup`, `LauncherSettingsHelper`, `LauncherSettingsActivity`
- `AdPlacement` (trimmed), `RemoteConfigHelper`, `TrackOnce`
- Role-related portion of `AppUtils`
- One `Application` class for this target
- Optional: `SubContainerFragment`, `VPNHelper`

XML / resources:

- Layouts listed in the source audit section 9 for the screens that stay
- Intro, permission, and default-home drawables that those layouts reference
- `Theme.Home` in `values/themes.xml` and `values-night/themes.xml`
- `launcherBackgroundColor` in `attrs.xml`
- `res/xml/default_config.xml`
- Launcher strings (new names, or a dedicated strings file, so `app_name` is not overwritten by accident)
- `sans_trial_*` fonts for XML
- Ad layouts only for formats that stay enabled
- `google-services.json` for this applicationId (not the source file)

Kotlin bridge, only if Java must open a Compose destination that has no Activity intent today (settings lives in the NavHost):

- A small intent extra or Activity that opens `SettingsScreen`

## Files that will probably be modified

- `app/src/main/AndroidManifest.xml` — LAUNCHER vs HOME split, queries, permissions, Application name, AdMob meta-data
- `app/build.gradle.kts` and `gradle/libs.versions.toml` — dependencies in section 8
- Root `build.gradle.kts` — Google services plugin `apply false`, if that is how this project applies plugins
- `ui/navigation/ScanPulseNavHost.kt` — stop showing the Compose splash after the Java splash owns startup; accept a start on Home or Scan when opened as the non-default destination
- `res/values/themes.xml` — add `Theme.Home` without changing `Theme.QRCodeScanner`’s parent
- `res/values/strings.xml` and `colors.xml` — add launcher strings/colors under new names where the source names collide
- Launcher fragment shortcut click listeners — point at this app’s activities (that edit is inside the migrated Java, after it exists)

## Files that should not be touched

- The entire source project `QRCodeScanner (1)\QRCodeScanner`
- `HomeScreen.kt` product layout (QR dashboard)
- Scanner, gallery, batch, and result Activities and their CameraX / ML Kit code
- Create / preview / customization Activities
- History Activities, `HistoryRepository`, Room entities and DAO
- `SettingsRepository`, `SettingsKeys`, `AppThemeMode`, `QRCodeScannerTheme`, `Color.kt`
- `LanguageScreen.kt` and `AppLanguage.kt` (settings language stays)
- `ScanPulseBottomBar.kt` visual design
- Target adaptive icons
- `file_paths.xml` and the existing FileProvider, unless a new share path is required
- Tests for theme, settings, batch scan, and barcode payloads, unless a later phase changes those contracts

## Dependencies potentially required later

See section 8. The minimum set for a faithful splash + screen list + HOME launcher + ads + remote config is:

- Google services plugin, Firebase BOM, `firebase-config`, `firebase-analytics`
- `play-services-ads`
- `fragment`, `viewpager2`, Material (XML)
- `sdp-android`
- `lifecycle-process` at 2.11.0
- `installreferrer` if paid/normal profiles stay
- Lottie and shimmer if those views stay

## Manifest changes potentially required later

- `android:name` on `<application>` pointing at the new Application class
- Move `MAIN` / `CATEGORY_LAUNCHER` from `MainActivity` to the splash activity
- Add `LauncherHomeActivity` exported, `MAIN` + `HOME` + `DEFAULT`, `launchMode=singleTask`, `taskAffinity=""`, `excludeFromRecents=true`, `stateNotNeeded=true`, `clearTaskOnLaunch=true`, `screenOrientation=portrait`, `theme=@style/Theme.Home`
- Declare each first-run activity that remains in the screen list, `exported=false`
- Declare `LauncherSettingsActivity`, `exported=false`
- `<queries>` intent `MAIN` + `CATEGORY_LAUNCHER`
- Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, and, only for the steps kept: `POST_NOTIFICATIONS`, `READ_PHONE_STATE`, `SYSTEM_ALERT_WINDOW`, `REQUEST_DELETE_PACKAGES`
- Meta-data `com.google.android.gms.ads.APPLICATION_ID` with this app’s id
- Do not add a second FileProvider
- Do not add call-screening, boot, FCM, or widget receivers for this migration

---

# 14. How Java/XML should sit inside this Jetpack app

Android compiles Java and Kotlin in one module. The launcher Activities can keep `setContentView` and `findViewById`. Compose `NavHost` does not display those layouts. Kotlin starts them with `Intent`.

`FLAG_ACTIVITY_CLEAR_TASK` in `ScreenFlowNavigation` clears the task. After that flag, the Compose back stack inside `MainActivity` is gone because the activity is gone. Returning users who land on `MainActivity` start `ScanPulseNavHost` from its `startDestination` again. That is why the Compose splash must be skipped or made instantaneous when `MainActivity` is opened from the screen flow.

`Theme.Home` is translucent and shows the system wallpaper. Apply it only to `LauncherHomeActivity`. The existing `Theme.QRCodeScanner` stays on `MainActivity` and the QR Activities so Compose screens stay opaque.

SharedPreferences files from the source (`screen_flow`, `default_app_flow`, `launcher_settings`, ad timing prefs) can live next to DataStore `scanpulse_settings`. They do not use the same file name. Do not move `intro_completed` into DataStore during the first integration. The Java readers expect SharedPreferences.

`RemoteConfigHelper` must see an Application that implements `fetchAd()`, because it casts to `MyApplication` today. Rename is fine if every cast is updated. A failed cast skips `fetchAd()` and the rest of config can still apply.

One `ActivityResultLauncher` for `ROLE_HOME` should stay inside `DefaultActivity` / `DefaultHomePromptHelper`. Do not also register a Compose launcher for the same request in `MainActivity`.

---

# FINAL STATUS

- Target architecture understood: YES
- Launcher integration possible: REQUIRES ADAPTATION
- Major conflicts:
  - Compose splash and `MainActivity` `CATEGORY_LAUNCHER` versus source `SplashActivity`
  - No onboarding here versus a remote five-step list gated by `intro_completed`
  - `HomeScreen` is a QR dashboard versus `LauncherHomeActivity` as device HOME
  - No `RoleManager`, HOME filter, `<queries>`, Application class, Firebase, or ads in this target
  - Package `com.qrcode.scanner` versus source `com.qrscanner.barcodescanner` and `R`
  - Shared style name `Theme.QRCodeScanner` and icon drawable names
  - Source `AdPlacement` / `MyApplication` also own call-end behavior this app does not have
  - Source pager page 0 is the old QR UI this Compose app already replaced
- Major risks:
  - Two splashes, or a screen-list token whose Activity was not copied
  - `Theme.Home` applied to Compose activities
  - Copying source `google-services.json` or AdMob ids into `com.qrcode.scanner`
  - `queryIntentActivities` returning an empty drawer without `<queries>`
  - A second `ROLE_HOME` request path
  - Material XML plus Compose Material3 resource clashes
  - `FLAG_ACTIVITY_CLEAR_TASK` re-entering the Compose splash
  - Overwriting scanner settings DataStore or Room history
- Recommended next phase: Implementation phase 1 should add the Application class, repackaged Java/XML launcher shell, HOME manifest entries, and the `ScreenFlowNavigation` destination bridge, with source call-end and source QR activities left behind. Ads and Firebase follow once that shell compiles and the single LAUNCHER and single HOME filters are in place. Do not convert the launcher to Compose in that phase.

SOURCE PROJECT CHANGED: NO  
TARGET CODE CHANGED: NO  
GRADLE CHANGED: NO  
MANIFEST CHANGED: NO
