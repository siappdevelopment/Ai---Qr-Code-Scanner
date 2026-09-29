# Phase 4 — Install Referrer and remaining Remote Config

Phase 4 completes the source Install Referrer reads and stores the Remote Config values that Phase 1 did not apply. Quiz, app-proxy lookup, call-end, right-swipe ads, and process app-open are still not started. The five-screen flow, permission flow, and language/night-mode behavior were not changed.

## Source files inspected

- `activities/SplashActivity.java` install referrer and install date
- `activities/LauncherHomeActivity.java` `fetchInstallReferrerIfNeeded`
- `common/AdPlacement.java` `referrer_preferences` / `SetReferrerUrl`, `getDaysSinceInstall`, `applyQuizAdsConfig`, `applyAppProxyConfig`, `saveClEndConfig`
- `helpers/RemoteConfigHelper.java` fetch, `paid_user` converters, and `applyAdConfig`
- `res/xml/default_config.xml`

No other `FirebaseRemoteConfig` usage was found. No referrer or Remote Config analytics event was found.

## Phase 1 audit

Already present and left in place:

- `InstallReferrerStartup` connects on a background executor, stores `response.getInstallReferrer()` in `referrer_preferences` / `SetReferrerUrl`, ends the connection only after `OK`, and ignores disconnect and `RemoteException`.
- The activity-private `getReferrer()` boolean skip is copied. Splash never writes that boolean.
- Splash does not wait for the referrer before Remote Config or navigation.
- `appInstallDate` / `install_date` is written only when the key is absent.
- `paid_user` vs `normal_user` uses the source converters and the four marketing strings.
- Splash, language, permission, intro, other, and launcher-app ad keys that the current screens already use were already applied.
- Fetch uses `minimumFetchIntervalInSeconds(0)` and `fetchAndActivate`. There is no source fetch timeout.
- A second fetch keeps its callback instead of dropping it, so splash is not stranded behind the application fetch.
- Empty, missing, or failed config restores the local five-screen list and still completes navigation.

Phase 1 did not start the same referrer read from launcher home. Source does.

## Install Referrer behavior

`LauncherHomeActivity` now calls the existing `InstallReferrerStartup.start` before its Remote Config fetch, inside the source try/catch. It does not add a second client.

Stored value:

- File `referrer_preferences`
- Key `SetReferrerUrl`
- Value is the Play Install Referrer string, or left unchanged when the response is not `OK`

Unavailable, empty, disconnected, and thrown cases do not block splash, screen flow, launcher home, or the QR app.

No additional referrer preference was found in the source. Install date stays independent: splash writes `yyyy-MM-dd` once, and this phase does not overwrite it. Source `getDaysSinceInstall` reads that date for later notification rules. That notification feature was not started.

The referrer client is still built with the application context. Source builds it with the activity. The stored string and preference file are the same.

## Converter logic

Unchanged from Phase 1, matching source:

- Left side of the first `=` equals `marketing_converter1` (`gclid`)
- Text before the first `&` equals `utm_source=apps.facebook.com` or `utm_source=apps.instagram.com`
- Right side of the first `=` equals `utm_source=marketing`

A match selects `paid_user`. Anything else, including an empty referrer, selects `normal_user`.

Source starts the referrer and then fetches Remote Config without waiting. This phase does not add a wait. The first fetch can still apply `normal_user` if the referrer returns later. A later fetch uses the stored URL.

## Remote Config key audit

Top-level Firebase defaults, already present:

- `Show_Screen_Flow`
- `activityChange` (target class `com.qrcode.scanner.launcher.activities.LauncherHomeActivity`)
- Version payload key `QRScanner_<versionCode>`

Already applied before this phase:

- `Privacy_Policy`, `Redirect_Home_Launcher`, `Ad_Priority`, `Google_Ad_Failed_Show_Quiz`, `Native_Ad_Label_Color`, `Native_Ad_Button_Color`, `App_Open_Id`
- `Splash_*`, `After_Splash_*`
- `Language_*`, `PermissionDefault_*`, `After_Default_Ad_Show`, `After_Default_Ad_Type` and the `After_default_*` aliases
- `Intro_Screen_Count`, `Intro_*`
- `Other_Ad_Show`, `Other_Ad_Type`, `Other_Banner_Id`, `Other_Native_Id`, `Other_Interstitial_Id`
- `LauncherApp_Native_List_*`, `LauncherApp_Click_Ad_Show`, `LauncherApp_Count`, `LauncherApp_Ad_Type`, `LauncherApp_Interstitial_Id`, `LauncherApp_Back_*`

Newly stored, with source defaults, without starting the features:

- `App_Open_Ad_Show` false, `App_Open_Dialog_Show` false, `App_Open_Show_Per_Day` 0
- `Interstitial_Click` 0
- `Right_Swipe_Interstitial_Ad_Show` false, `Right_Swipe_Interstitial` 0
- `Cl_End_Screen_Show` false
- `Default_App_Popup_Show` false, `Default_App_Popup_Count` 0
- `Other_Interstitial_Ad_Show` false
- `Main_Ad_Show` false, `Main_Ad_Type` `banner`, `Main_Banner_Id` empty, `Main_Native_Id` empty, `Main_Ad_Auto_Refresh` false, `Main_Ad_Auto_Second` 0
- `CreateFragment_Native_Ad_Show` false, `CreateFragment_Native_Id` empty, `CreateFragment_Native_Second` 0
- `General_Ad_Show` false, `General_Ad_Type` `banner`, `General_Banner_Id` empty, `General_Native_Id` empty, `General_Interstitial_Ad_Show` false, `General_Interstitial_Id` empty
- `Social_Ad_Show` false, `Social_Ad_Type` `banner`, `Social_Banner_Id` empty, `Social_Native_Id` empty, `Social_Interstitial_Ad_Show` false, `Social_Interstitial_Id` empty
- `Barcode_Ad_Show` false, `Barcode_Ad_Type` `banner`, `Barcode_Banner_Id` empty, `Barcode_Native_Id` empty, `Barcode_Interstitial_Ad_Show` false, `Barcode_Interstitial_Id` empty
- `SettingsFragment_Native_Ad_Show` false, `SettingsFragment_Native_Id` empty, `SettingsFragment_Native_Second` 0
- `LauncherApp_Native_Ad_Show` false, `LauncherApp_Native_Ad_Show_Per_Day` 0, `LauncherApp_Native_Id` empty
- `LauncherApp_Quiz_Icon_Show` false, `LauncherApp_Quiz_Icon_Count` 0
- `ClEnd_Ad_Show` false, `ClEnd_Ad_Type` `banner`, `ClEnd_Banner_Id` empty, `ClEnd_Native_Id` empty
- `ClEnd_Back_Ad_Show` false, `ClEnd_Back_Ad_Type` `inter`, `ClEnd_Back_Ad_Show_After_Day` 0, `ClEnd_Back_Ad_Show_Per_Day` 0, `ClEnd_Back_Ad_Interstitial_Id` empty, `ClEnd_Back_Ad_Country_IP` false, `ClEnd_Back_Ad_Show_Country` empty list
- `Notification_Install_Days` 0, `Notification_Call_Install_Days` 0, `Notification_Call_Overlay_Install_Days` 0
- `All_Allow_Permission_Show_Notification` false, `Notification_Back_Ad_Show` false, `Notification_Close_Button_Show` false
- `Notification_Country`, `Notification_Call_Country`, `Notification_Call_Overlay_Country` empty lists, lowercased
- `QuizAdsDesign` lists: `Quiz_App_Icon`, `Quiz_Native_Media`, `Quiz_Interstitial_Media`, `Quiz_App_Open_Media`, `Quiz_Banner_Title`, `Quiz_Banner_Description`, `Quiz_Native_Title`, `Quiz_Native_Description`, `Quiz_Interstitial_Title`, `Quiz_Interstitial_Description`, `Quiz_Interstitial_Rate`, `Quiz_Button_Text`
- `AppProxyStructure`: `App_Proxy_Check_Ip` false, `App_Proxy_Ip_Checker_Url` empty, `Quiz_Link_List`, `Quiz_Link_List_Exclude`, `App_Proxy_List_City`, `App_Proxy_List_State`, `App_Proxy_List_Country`

Quiz list keys replace the stored list only when the parsed list is non-empty, matching source. A `QuizAdsDesign` `Ad_Priority` or `Google_Ad_Failed_Show_Quiz` overrides the root value only when that key is present.

Call-end values are written to `cl_end_config_preferences` with the source preference names. Call-end is not loaded or shown.

The app-proxy IP request is not made.

## Fetch, cache, and fallback

- Interval remains 0 seconds.
- `fetchAndActivate` remains the fetch call.
- Firebase missing, empty payload, empty array, and malformed JSON still restore the cached or default screen list and let navigation continue.
- Callbacks queued during an in-flight fetch are still delivered. Source dropped the second callback. That queue stays, because removing it can leave splash waiting.

## Paid-user interaction

`paid_user` selects the Remote Config profile before ad fields are applied. It does not add a separate ad-suppression flag beyond the values inside that profile. No new ad placement was added.

## Analytics

None added.

## Build result

`:app:assembleDebug` succeeded.

`BUILD SUCCESSFUL in 9s`.

## Unit test result

`:app:testDebugUnitTest` succeeded. No failures and no errors.

## Device verification status

Not run. Live Install Referrer and live Remote Config were not verified on a device.

## Intentionally pending

- Quiz UI and quiz ads
- App-proxy IP lookup
- Right-swipe interstitial and process app-open
- Call-end, notifications, and `PhoneCallStateService`
- Default Home popup
- Launcher sub page and weather
- FCM, reminders, boot receiver, and widgets
- Crashlytics, Facebook mediation, and ProGuard
- QR product changes
