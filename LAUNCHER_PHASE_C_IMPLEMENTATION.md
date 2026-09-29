# Launcher Phase C Implementation

Launcher shell only. The Source Launcher project was not modified. Existing Compose QR screens were not rewritten.

Debug build: `:app:assembleDebug` succeeded.

---

## What this phase did

The device HOME shell from the Java/XML source now lives in this app as Java, under `com.qrcode.scanner.launcher`.

`LauncherHomeActivity` is a new activity. It is the only activity with `ACTION_MAIN` + `CATEGORY_HOME` + `CATEGORY_DEFAULT`. It uses the source home layout (`activity_launcher_home.xml` + `fragment_launcher_home.xml`) and `Theme.Home` (translucent window, system wallpaper).

`MainActivity` is still the Compose QR shell and still the only `CATEGORY_LAUNCHER` activity. Splash was not migrated, so the icon entry was not moved.

Ads, Firebase, Remote Config, onboarding, RoleManager, `DefaultActivity`, `DefaultHomePromptHelper`, analytics, install referrer, call-end, FCM, widgets, and the source QR pager (`MainContainerFragment`) were not migrated.

---

## Files created

### Application

- `app/src/main/java/com/qrcode/scanner/QrScannerApplication.java`

`onCreate` only calls `super.onCreate()`. It does not initialize ads or Firebase, does not restore the source night-mode preference, and does not call `AppCompatDelegate.setApplicationLocales`. Compose theme and settings language stay on the existing DataStore / AppCompat path.

### Java launcher shell (kept as Java)

- `launcher/activities/LauncherHomeActivity.java` (adapted)
- `launcher/activities/LauncherSettingsActivity.java` (ad load removed)
- `launcher/fragments/LauncherHomeFragment.java` (shortcut targets adapted)
- `launcher/adapters/LauncherPagerAdapter.java` (home page only)
- `launcher/adapters/LauncherAppsAdapter.java` (quiz injection and ad click removed)
- `launcher/dialogs/LauncherAppsBottomSheet.java` (role request and native ad load removed)
- `launcher/dialogs/LauncherAppContextPopup.java`
- `launcher/helpers/LauncherAppsHelper.java`
- `launcher/helpers/LauncherAppsIconCache.java`
- `launcher/helpers/LauncherSettingsHelper.java`
- `launcher/models/LauncherAppsModel.java`
- `launcher/models/LauncherAppsDisplayItem.java`
- `launcher/models/QuizGameItem.java` (type used by the drawer row model; quiz icons are not injected)
- `launcher/interfaces/OnLauncherAppLongClickListener.java`
- `launcher/interfaces/MainNavigationHost.java` (copied; the HOME activity does not implement it)
- `launcher/common/AppUtils.java` (label, dp, own-package, and app-info helpers only)

### Layouts

- `res/layout/activity_launcher_home.xml`
- `res/layout/fragment_launcher_home.xml`
- `res/layout/bottom_sheet_launcher_apps.xml`
- `res/layout/adapter_launcher_apps.xml`
- `res/layout/adapter_launcher_native_spacer.xml`
- `res/layout/dialog_apps_folder.xml`
- `res/layout/dialog_launcher_app_action.xml`
- `res/layout/dialog_serialize.xml`
- `res/layout/activity_launcher_settings.xml`

The four `<include layout="@layout/shimmer_*_ad"/>` tags were replaced with an empty `View`. The source ad shimmer layouts were not copied. Ad containers stay in the XML and are not filled.

### Other resources

- `res/values/colors_launcher.xml`
- `res/values/dimens_launcher.xml`
- `res/values/strings_launcher.xml` (`app_name` was not replaced)
- `res/values/attrs_launcher.xml`
- `res/values-night/themes_launcher.xml` (`Theme.Home` and the drawer dialog theme only)
- `res/font/sans_trial_regular.ttf`, `sans_trial_medium.ttf`, `sans_trial_semi_bold.ttf`
- `assets/right_swipe_animation.json`, `assets/launcher_tap_animation.json`
- Drawables used by those layouts (see git status). Night copies: `ic_arrow.png`, `ic_back.png`, `ic_app_label_size.png`, `img_default_home.png`

Existing `Theme.QRCodeScanner`, `ic_launcher_foreground`, and `ic_launcher_background` were not overwritten.

---

## Files modified

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/themes.xml` (added `Theme.Home`, `Theme.LauncherSettings`, and drawer styles; `Theme.QRCodeScanner` is still `Theme.AppCompat.Light.NoActionBar`)
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

No Compose scanner, create, history, Room, settings, or theme Kotlin files were edited.

---

## Manifest

| Item | Result |
| --- | --- |
| `android:name` | `.QrScannerApplication` |
| `CATEGORY_LAUNCHER` | Still only on `MainActivity` |
| `LauncherHomeActivity` | `exported=true`, `launchMode=singleTask`, `taskAffinity=""`, `excludeFromRecents=true`, `stateNotNeeded=true`, `clearTaskOnLaunch=true`, portrait, `@style/Theme.Home` |
| HOME filter | `MAIN` + `HOME` + `DEFAULT` on `LauncherHomeActivity` only |
| `LauncherSettingsActivity` | `exported=false`, portrait, `@style/Theme.LauncherSettings` |
| `<queries>` | `ACTION_MAIN` + `CATEGORY_LAUNCHER` |
| `REQUEST_DELETE_PACKAGES` | Added for the drawer uninstall action |
| `CAMERA` | Unchanged |

---

## Dependencies added

Required for the XML shell to compile and inflate. Ads and Firebase were not added.

| Library | Version |
| --- | --- |
| `com.google.android.material:material` | 1.12.0 |
| `androidx.fragment:fragment` | 1.8.6 |
| `androidx.viewpager2:viewpager2` | 1.1.0 |
| `com.intuit.sdp:sdp-android` | 1.1.1 |
| `com.airbnb.android:lottie` | 6.6.2 |
| `com.facebook.shimmer:shimmer` | 0.5.0 |

Shimmer is on the classpath because the copied layouts still contain `ShimmerFrameLayout`. No ad is requested.

---

## Behavior adapted for this app

- The pager has one page: `LauncherHomeFragment`. Source page 0 (`MainContainerFragment`) and page 2 (`SubContainerFragment`) are not in this build.
- Right-swipe / quick action opens this app’s `MainActivity` (Compose QR shell).
- Scan opens `ScannerActivity`.
- Create and Create QR open `CreateActivity`.
- History opens `HistoryActivity`.
- Settings buttons do nothing. Settings is still a Compose route inside `MainActivity`, not an Activity.
- Set-as-default controls stay in the source layouts and do not call `RoleManager`.
- App drawer, search, launch, long-press app info / uninstall, folders, and launcher settings (sort, label, icon size) are the source Java behavior.
- Installed-app query uses `PackageManager.queryIntentActivities` for `MAIN` / `LAUNCHER`.

---

## Known TODOs for the next phase

- RoleManager / `DefaultActivity` / `DefaultHomePromptHelper`, including hiding the set-as-default row when this app is already the home app.
- Settings bridge: open the existing Compose Settings screen from the launcher shortcuts without redesigning Settings.
- Firebase, Remote Config, and the remote screen list (Language, Collection, Permission, Default Home, Intro).
- AdMob. Ad containers are present and empty. Do not copy source ad unit ids.
- Splash as `CATEGORY_LAUNCHER`, if that handoff is still wanted. This phase left the icon on `MainActivity` so there is still one launcher entry.
- Source pager side pages (QR container and sub page), if they are still required after the Compose shell is the QR destination.
- Quiz icons (`LauncherQuizGames`) were not migrated.

---

## Confirmation

- Source Launcher project modified: NO
- Java converted to Kotlin: NO
- Existing Compose QR functionality intentionally changed: NO
- Firebase / Remote Config / AdMob / onboarding / RoleManager implemented: NO
- `:app:assembleDebug`: SUCCESS
