# Launcher Phase E Implementation

Firebase Remote Config and the source remote screen list. The Source Launcher project was not modified. Java stayed Java. Compose QR screens, Scanner, Create, History, Room, and Compose Settings were not changed.

Debug build: `:app:assembleDebug` succeeded.  
Unit tests: `:app:testDebugUnitTest` succeeded.

Device verification was not performed. Live Remote Config was not verified, because this app has no `google-services.json`.

---

## 1. Firebase dependencies added

- `com.google.firebase:firebase-bom` `34.12.0`
- `com.google.firebase:firebase-config` (version from the BOM)
- Google Services Gradle plugin `4.4.4`, applied only when `app/google-services.json` exists

Not added: Firebase Analytics, Crashlytics, Cloud Messaging, AdMob.

## 2. Firebase configuration status

Not available.

`app/google-services.json` for application id `com.qrcode.scanner` is not in this project. The source file was not copied.

Without that file the Google Services plugin is not applied, `FirebaseApp.initializeApp` has no generated app id, and Remote Config cannot fetch. The app still starts and uses the local screen-list fallback.

Remote Config is implemented. It is not fully functional until a `google-services.json` for `com.qrcode.scanner` is added and a parameter `QRScanner_1` (this app’s `versionCode` is 1) exists in Firebase with the source JSON shape.

## 3. Application initialization

`QrScannerApplication.onCreate` still calls `super.onCreate()`, then `RemoteConfigHelper.fetchRemoteConfig`. The fetch is asynchronous. It does not initialize ads or analytics, and it does not change DataStore, Compose theme, or AppCompat language restore.

`LauncherHomeActivity` also starts a fetch after the home shell is set up, and stores `launcher_remote_config` / `last_remote_fetch_time` only when that fetch reports success. The shell is not delayed for the network.

## 4. Exact Remote Config keys used

| Key | Use |
| --- | --- |
| `QRScanner_<versionCode>` | Whole JSON document. This app reads `QRScanner_1` |
| `Show_Screen_Flow` | Inside that JSON, otherwise the top-level parameter |
| `normal_user` | Profile object. Used because install referrer is not migrated |
| `paid_user` | Present in the source. Not selected in this phase |
| `Privacy_Policy` | URL for the Collection footer |
| `Redirect_Home_Launcher` | After the list, open `LauncherHomeActivity` even when this app is not the default Home |
| `Screen.IntroScreen.Intro_Screen_Count` | How many intro pages to show, capped at 3 |

`default_config.xml` also contains `activityChange`. Source Java does not read it. Not found as a navigation input.

Ad, quiz, call-end, and notification keys from the source helper are not applied.

## 5. Remote Config defaults / fallback

`app/src/main/res/xml/default_config.xml`:

- `Show_Screen_Flow` = `["Language","Collection","Permission","DefaultHome","Intro"]`
- `activityChange` = `com.qrcode.scanner.launcher.activities.LauncherHomeActivity` (source package name replaced)

`ScreenFlowConfig.getDefaultShowScreenFlow()` is the same five names, in that order.

Fetch settings match the source: `setMinimumFetchIntervalInSeconds(0)` and `fetchAndActivate`. A custom fetch timeout was not found in the source. The Firebase SDK default timeout is used.

If Firebase is missing, the fetch fails, the JSON is empty, the array is empty, or parsing throws, the cached `show_screen_flow` preference is restored. If that cache is empty, the five-name default is used. This `ensure` step also runs immediately after those failures so the in-memory list is not left empty. The source called the same ensure later, from `ScreenFlowNavigation`.

`Intro_Screen_Count` is not in `default_config.xml`. The Java default is `0`. Count `0` does not show intro pages.

A changed cached list resets onboarding completion flags only when `intro_completed` is still false. A failed fetch does not mark onboarding complete.

## 6. Remote screen-list JSON / model

No dedicated model class was found in the source. The list is `List<String>`, parsed by `parseShowScreenFlow` and `normalizeScreenName`.

Accepted JSON for `QRScanner_<versionCode>`:

- one JSON object, or
- a JSON array whose first element is an object

Order:

1. Non-empty `Show_Screen_Flow` inside the version JSON
2. Else the top-level `Show_Screen_Flow` string
3. Else the five-name default if memory is empty

Aliases:

| Remote text | Token |
| --- | --- |
| `Language` | `Language` |
| `Collection` | `Collection` |
| `Permission` | `Permission` |
| `DefaultHome`, `Default`, `DefaultApp`, `Default_Home` | `DefaultHome` |
| `Intro`, `IntroActivity`, `IntroScreen`, `Onboarding` | `Intro` |

Unknown names stay in the list and are skipped. They do not get activities.

A separate enable/disable flag was not found. A screen is omitted by leaving it out of the list.

## 7. Screen identifiers supported

`Language`, `Collection`, `Permission`, `DefaultHome`, `Intro`.

## 8. Screen ordering behavior

The remote list order is kept. The walk starts at the first incomplete supported screen. `continueAfter` marks that screen complete, clears completion flags for every later screen in the list, then opens the next incomplete supported screen. Nothing later in the list is shown early. A screen that is absent is not shown.

## 9. ScreenFlowNavigation adaptation

`launcher/common/ScreenFlowNavigation.java` follows the source rules.

Final destination:

1. This app is the default Home app → `LauncherHomeActivity` with `ACTION_MAIN`, `CATEGORY_HOME`, `CATEGORY_DEFAULT`
2. Else if `Redirect_Home_Launcher` → `LauncherHomeActivity`
3. Else → `com.qrcode.scanner.MainActivity`

Widget extras were not attached. `WidgetNavigation` was not migrated.

`openFirstScreen` is implemented. The source caller is `SplashActivity`. Splash was not migrated, and `MainActivity` was not changed, so the app icon still opens the existing Compose QR app. Onboarding runs when `openFirstScreen` is called.

## 10. Intro implementation

Adapted: `IntroNavigation`, `Intro1Activity`, `Intro2Activity`, `Intro3Activity`, and `activity_intro_1.xml`, `activity_intro_2.xml`, `activity_intro_3.xml`.

Next moves to the next page only when `Intro_Screen_Count` allows it. The last visible page calls `continueAfter(..., "Intro")`.

Skip button: not found in source.  
“Get Started” on the last page: not found in source. Page 3 still uses Next.  
Back override: not found in source. System back finishes the page and does not set `intro_completed`.

Ad containers stay in the XML and are hidden. Ad loads were not copied. Count `0` marks Intro complete and does not open Intro1.

## 11. Language integration

`LanguageActivity` keeps `activity_language.xml` and `LanguageAdapter`.

The source list has many languages. This app ships `en-US` only (`locales_config.xml` and `AppLanguage`). Only English is shown. Done calls `AppCompatDelegate.setApplicationLocales` with `en-US` when that locale is not already applied. A second language preference file was not added. Compose Settings was not changed.

Other source language codes and flag drawables were not copied. Translations were not invented.

## 12. Collection integration

`CollectionActivity` and `activity_collection.xml`. The agree checkbox is instance state `key_agreed`. Agree & Continue calls `continueAfter(..., "Collection")`. The footer opens `Privacy_Policy` when that URL is non-empty. Otherwise it shows the source “Privacy Policy Not Found !” toast.

No QR pager and no new destinations.

## 13. Permission integration

`PermissionActivity` and `activity_permission.xml` are present.

The source requests `READ_PHONE_STATE`, then `CAMERA`, then overlay settings. Those requests were not copied. Camera permission stays on the existing scanner. SMS, contacts, storage, notification, and phone-state permissions were not added.

Allow Permission only continues the screen list. System back does not mark the step complete. A permission back override was not found in the source.

## 14. Default Home integration with Phase D

`DefaultActivity` is the onboarding step. It uses `activity_default.xml`.

The button calls the existing `DefaultHomePromptHelper`. `RoleManager.createRequestRoleIntent(ROLE_HOME)` still exists only in `AppUtils`. There is no second request implementation.

The source `DefaultActivity` phone-state request, ad preload, and its own role launcher were not copied.

After the helper finishes, success or cancel, `continueAfter(..., "DefaultHome")` runs. The source marks `default_home_completed` on cancel as well, then continues the list. That is preserved here so cancel does not reopen this step. The in-launcher set-as-default row still hides only when this app actually holds the Home role, and it does not continue onboarding.

System back on this screen finishes it and does not mark the step complete.

## 15. First-launch / onboarding state

SharedPreferences file `screen_flow` (`MODE_PRIVATE`):

| Key | Meaning |
| --- | --- |
| `language_flow_completed` | Language step done |
| `language_selected` | Written with language completion |
| `collection_completed` | Collection step done |
| `permission_completed` | Permission step done |
| `default_home_completed` | Default Home step done |
| `intro_completed` | Onboarding complete |

`hasCompletedOnboarding` is only `intro_completed`. The other flags do not skip the whole list by themselves.

Screen-list cache: SharedPreferences `show_screen_flow`, keys `cached` and `flow`.

DataStore settings were not replaced.

## 16. Error / fallback behavior

| Case | Result |
| --- | --- |
| Firebase unavailable | Default five-name list. App start is not blocked |
| Fetch failure | Restore cache, else the five-name default. `intro_completed` stays false |
| Timeout | Not found as a custom value. SDK `fetchAndActivate` failure uses the same restore path |
| Invalid JSON | Restore cache, else the default list. No crash |
| Empty list | `openFirstScreen` goes to the final destination and does not mark onboarding complete |
| Unknown screen | Skipped |
| Screen left out of the list | Not shown |
| User backs out of a step | That step stays incomplete. Nothing restarts it automatically |
| User cancels Default Home | Step is marked complete, next screen opens, launcher stays usable |
| User completes Default Home | Next screen opens. The launcher is not finished by the role request |
| Process recreation | Completion flags and the cached list are in SharedPreferences. Agree state is in the collection instance bundle |

## 17. Manifest changes

- `INTERNET` added. Remote Config needs it
- Onboarding activities added, `exported=false`, portrait, `Theme.LauncherSettings`
- `MainActivity` remains the only `MAIN` + `CATEGORY_LAUNCHER` activity
- `LauncherHomeActivity` remains the only `MAIN` + `HOME` + `DEFAULT` activity

No phone, SMS, contacts, overlay, or notification permissions were added.

## 18. Resources added

Layouts: `activity_language.xml`, `activity_collection.xml`, `activity_permission.xml`, `activity_default.xml`, `activity_intro_1.xml`, `activity_intro_2.xml`, `activity_intro_3.xml`, `adapter_language.xml`.

Shimmer ad includes were replaced with empty views. Ad containers are hidden.

Drawables: intro, permission, and default images (including night copies), `ic_english`, `ic_done`, checkbox vectors, `custom_radius_100`, `custom_circle`, `custom_radius_border_12`.

Strings: `strings_onboarding.xml`.  
Defaults: `xml/default_config.xml`.

`Theme.QRCodeScanner`, Compose resources, and launcher icons were not overwritten. Onboarding activities use the existing `Theme.LauncherSettings` so launcher color attributes used by the source drawables resolve.

## 19. Files created

- `launcher/remote/RemoteConfigHelper.java`
- `launcher/remote/ScreenFlowConfig.java`
- `launcher/common/ScreenFlowNavigation.java`
- `launcher/common/IntroNavigation.java`
- `launcher/activities/LanguageActivity.java`
- `launcher/activities/CollectionActivity.java`
- `launcher/activities/PermissionActivity.java`
- `launcher/activities/DefaultActivity.java`
- `launcher/activities/Intro1Activity.java`
- `launcher/activities/Intro2Activity.java`
- `launcher/activities/Intro3Activity.java`
- `launcher/adapters/LanguageAdapter.java`
- layouts, drawables, `strings_onboarding.xml`, and `xml/default_config.xml` listed above

## 20. Files modified

- `QrScannerApplication.java`
- `launcher/activities/LauncherHomeActivity.java`
- `launcher/common/AppUtils.java`
- `launcher/helpers/DefaultHomePromptHelper.java` (activity host and one completion listener for the onboarding step)
- `app/build.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `AndroidManifest.xml`

## 21. Intentionally not migrated

- AdMob and every ad key, loader, and unit id
- Firebase Analytics and `TrackOnce`
- Splash, install referrer, FCM, call-end, widgets, quiz, `MainContainerFragment`
- Source `paid_user` selection (needs the install referrer)
- Source language list beyond English
- `READ_PHONE_STATE`, overlay, and a second camera-permission request
- Source `DefaultActivity` role-request code
- Network-callback refetch in the source launcher. The 12-hour check in that callback was not copied. `minimumFetchIntervalInSeconds` remains `0`, matching the helper
- Compose Settings

## 22. Firebase / Remote Config limitations

Live fetch cannot succeed until `google-services.json` for `com.qrcode.scanner` is added. Do not use the source app’s file.

Until then, startup uses the local five-name default, intro page count stays `0`, and `Redirect_Home_Launcher` stays false. The QR app still opens from the launcher icon.

`openFirstScreen` is not called from the Compose splash.

## 23. Build result

`:app:assembleDebug` — BUILD SUCCESSFUL.

## 24. Unit-test result

`:app:testDebugUnitTest` — BUILD SUCCESSFUL.

Device verification was not performed.
