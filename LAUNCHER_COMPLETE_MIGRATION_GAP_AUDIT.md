# Launcher Complete Migration Gap Audit

Investigation only. No source or target code was changed.

This pass compared `LAUNCHER_SOURCE_MIGRATION.md` with the source tree under `QRCodeScanner/app/src/main` (manifest, Java, resources, Gradle, ProGuard) and with the current target tree.

# 1. Final Scope

Reproduce the source app in the target app.

The only allowed substitutions:

1. Source QR / barcode scanner / QR creator product UI and its product code stay out. The existing Jetpack Compose QR app remains the product.
2. The source right-swipe page (`MainContainerFragment`) is not used. That destination opens the existing Compose QR app (`MainActivity`).

Every other source behavior must be functionally equivalent, including features earlier phases skipped.

`NOT APPLICABLE` is used only where the class is source QR-product UI or QR-product logic. Launcher, splash, onboarding, ads outside that product, call-end, quiz, referrer, notifications, widgets, VPN, overlay, and phone-state are in scope.

# 2. Source Feature Inventory

## In scope

Application init, splash, VPN, screen flow, language, collection, permission, default home, launcher home, drawer, launcher settings, sub page, quiz, ads, Firebase, Remote Config, analytics, install referrer, call-end, FCM, reminders, widgets, theme, locale helper, app-proxy IP check, ProGuard rules that protect those features.

## Not the QR product, found in source code beyond the earlier summary

- `ClContentFragment`, `ClMoreOptionFragment`, `ClMessageFragment`, `ClReminderFragment`
- `CallEndViewManager`, `CallEndPendingLaunch`, `CallEndLaunchHelper`, `CallEndFullNotificationHelper`, `CallEndBackAd`
- `ADSNativeFullDisplay` (FCM and call-end preload)
- `PhoneCallStateService` manifest actions beyond `PHONE_STATE`: `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `ACTION_POWER_CONNECTED`, `ACTION_POWER_DISCONNECTED`, `NEW_OUTGOING_CALL`, `QUICKBOOT_POWERON`, and the two `com.example.autostart` actions. The Java handles outgoing-call number and phone state only. The other actions return without work.
- `MyFirebaseMessagingService.handleIntent`: if notification permission is missing on API 33+, return. If the app is foreground or the keyguard is unlocked, return. Otherwise require `AdPlacement.isCallEndPerformanceAllowed`, preload `Cl_End` native, then `CallEndLaunchHelper`. `onMessageReceived` is empty.
- `BootReceiver` only calls `ReminderAlarmHelper.rescheduleAll`
- `LocaleHelper` listeners around `AppUtils.restoreSavedLanguage`
- `IPAddressHelper` for `AppProxyStructure` inside `AdPlacement`, and also from `MainContainerFragment`
- `WeatherForecastGenerator` plus `WeatherModel` / `WeatherHourlyModel` for the sub page. It generates a local forecast. It is not a weather network API.
- `AppWidgetsActivity`, `WidgetPinHelper`, `BaseAppWidgetProvider`, `WidgetType`, `WidgetNavigation` (`widget_navigation` / `pending_target_screen`)
- Widget tap opens `SplashActivity` with `TARGET_SCREEN` `scan` or `create`
- Source `proguard-rules.pro` keeps `ReminderModel`, Crashlytics line numbers, and Facebook mediation classes

## Proven source QR product — do not migrate

`MainActivity`, `ScanFragment`, `CreateFragment`, `HistoryFragment`, `HistoryScanFragment`, `HistoryCreateFragment`, `HistoryFavoriteFragment`, `BaseHistoryListFragment`, `SettingsFragment`, `MainPagerAdapter`, `MainUIController`, batch scan, barcode, QR preview/edit/templates, general and social create activities, `HistoryStore`, barcode/QR generators and validators, template catalog/renderer, `CreateFlowHelper`, `CreateOptionsBinder`, `CreateUIHelper`, `CountryCodeProvider`, `CountryCodeSelectDialog`, `SocialAppIntent`, `SocialQRFormatter`, `ScanContentParser`, `ScanSettingsUtils`, `WebsiteURLValidator`, `QRContentFormatter`, `CreateInputValidator`, `CreateTypeLabels`, `HistoryActionPopupHelper`, `SegmentedTabUiHelper`, `QrTemplate*`, `FAQsActivity` (opened only from `SettingsFragment`).

`MainContainerFragment` is the right-swipe host of that product. The page is not copied. Opening it is replaced by Compose `MainActivity`.

No custom deep link or app link was found in the source manifest. No rewarded-ad class was found. No `AppWidgetHost` was found. No DataStore was found.

# 3. Migration Status Matrix

| Feature | Source evidence | Current target | Status | Missing work | Planned phase |
| --- | --- | --- | --- | --- | --- |
| Compose QR product | Source scan/create/history/settings | Existing Compose screens | INTENTIONALLY REPLACED — OUR QR APP | Keep it | None |
| Source QR classes listed above | Product activities and helpers | Not copied | NOT APPLICABLE | Do not port | None |
| Right-swipe page | `MainContainerFragment` | `openQrShell()` → `MainActivity` | INTENTIONALLY REPLACED — OUR QR APP | Do not port the fragment | None |
| Right-swipe interstitial | `Right_Swipe_Interstitial_*` on that page | `navigateViaRightSwipeFlow` runs the action immediately | MISSING | Same counter and interstitial when that destination opens | Ads |
| Icon / splash entry | `SplashActivity` `MAIN`+`LAUNCHER` | Compose splash inside `MainActivity` | PARTIAL | Source splash sequence | Startup |
| Splash VPN | `VPNHelper`, `dialog_vpn_detect.xml` | Absent | MISSING | Block until recheck | Startup |
| Splash ads and duration | `Splash_*`, `After_Splash_*` | Keys not applied | MISSING | Banner/native, then interstitial or app-open | Startup |
| Splash notification permission | API 33+ request, `POST_NOTIFICATIONS_GRANTED` | Not requested | MISSING | Permission and one-time event | Startup |
| Install date | `appInstallDate` / `install_date` | Not written | MISSING | Write once | Startup |
| Widget extra on splash | `WidgetNavigation.captureTargetScreen` | Absent | MISSING | After flow, open Compose scan or create | Widgets |
| Reminder extra on splash | `ReminderAlarmHelper.handleReminderLaunchIntent` | Absent | MISSING | Open the reminder target | Reminders |
| Screen flow | Five tokens, prefs `screen_flow` | `ScreenFlowNavigation`, `ScreenFlowConfig` | COMPLETE | None for the walker | None |
| Intro pages | Intro1–3, count 0 skips | Activities and layouts | COMPLETE | None for page order | None |
| Language | Full list, prefs `language`, `LocaleHelper` | English only, AppCompat `en-US` | PARTIAL | Source languages, pref, listener | Language |
| Collection | Agree step, no ads | `CollectionActivity` | COMPLETE | None found | None |
| Permission requests | Phone, camera, overlay | Button only continues | MISSING | The three requests and grant events | Permissions |
| RoleManager / chooser | `AppUtils`, `DefaultHomePromptHelper` | Same helper path | PARTIAL | Popup and setup prefs still absent | Default Home |
| Default-home events | `DEFAULT_HOME_APP_SET` / `CANCEL` | `TrackOnce` in the helper | COMPLETE | Live delivery not proven | Verify |
| Default-home popup | `Default_App_Popup_*` | Absent | MISSING | Popup and `default_app_popup_preferences` | Default Home |
| Setup prefs | `default_app_flow` | Absent | MISSING | Completing and awaiting flags | Default Home |
| HOME activity | `LauncherHomeActivity` | Present with HOME filter and `Theme.Home` | COMPLETE | None for the declaration | None |
| Home dock, folders, search, voice | `LauncherHomeFragment` | Present | PARTIAL | Settings clicks are empty | Launcher |
| Settings shortcut | Home settings buttons | `openSettingsTodo()` is empty | MISSING | Open existing Compose Settings | Launcher |
| Drawer | Sheet, search, sort, launch, package receiver | Present | PARTIAL | Quiz rows are never inserted | Quiz |
| Launcher settings | Sort, sizes, labels | `LauncherSettingsActivity` | COMPLETE | Theme is separate | None |
| Sub page | `SubContainerFragment`, weather layouts | Absent | MISSING | Page, local weather, recommended apps, native | Sub page |
| Weather generator | `WeatherForecastGenerator` | Absent | MISSING | Local daily cache, not a new API | Sub page |
| Quiz games | `LauncherQuizGames`, `ic_qz_icon_*` | Adapter can open a URL but builds no quiz rows | MISSING | List, icons, injection | Quiz |
| Quiz ad layouts | `qz_*.xml` | Priority hides Google and does not show quiz | MISSING | Layouts and failure fallback | Quiz |
| Google banner, native, interstitial, app-open | `AdPlacement` | Wired for onboarding, settings, drawer list, click, return | PARTIAL | Splash, right-swipe, sub, call-end, process app-open | Ads |
| App proxy | `AppProxyStructure`, `IPAddressHelper` | Not applied | MISSING | Geo check used by ads | Ads |
| Process app-open | `MyApplication.fetchAd`, 4-hour expiry, skip splash/home/call-end | Not in `QrScannerApplication` | MISSING | Load path. Source skip still prevents the foreground show | Ads |
| `ad_impression` | `logAdRevenue` | Paid listeners call it | COMPLETE | Delivery not proven | Verify |
| TrackOnce | `analytics_events` | Present | PARTIAL | Grant, splash, call-end, and full-native events are not logged | Analytics |
| Remote Config | `QRScanner_<versionCode>` | Helper and `default_config.xml` | PARTIAL | Always `normal_user`. Many keys ignored. No 12-hour gate | Remote Config |
| Install referrer | `installreferrer`, `referrer_preferences` | Absent | MISSING | URL and `paid_user` match | Remote Config |
| `google-services.json` | Target file, not the source file | Present for `com.qrcode.scanner` | PARTIAL | Live fetch not run in this audit | Verify |
| Analytics SDK | `firebase-analytics` | On the classpath | PARTIAL | Delivery not proven | Verify |
| Crashlytics | SDK, plugin, ProGuard keep | Absent | MISSING | Dependency and rules. Navigation does not call it | Firebase extras |
| FCM | `MyFirebaseMessagingService` | Absent | MISSING | Service and the locked-screen call-end path | Notifications |
| Call-end | Activity, four fragments, screening service, helpers | Absent | MISSING | Full flow, including `Theme.ClEnd` | Call-end |
| Phone-state receiver | `PhoneCallStateService` | Absent | MISSING | Outgoing number and phone-state handling, plus the declared extra actions | Call-end |
| Reminders | `ReminderAlarmHelper`, `ReminderReceiver`, `ReminderModel` | Absent | MISSING | Alarms, boot reschedule, splash handoff | Reminders |
| Boot receiver | `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED` | Absent | MISSING | Reschedule reminders | Reminders |
| Widgets | Six providers, pin helper, gallery activity | Absent | MISSING | Providers. Taps must open Compose scan/create, not source activities | Widgets |
| Night mode | `ThemeUtils`, `ThemeHelper` | Not applied in `QrScannerApplication` | MISSING | Saved theme and return-home after change | Language |
| Translations | `values-*` | No locale string folders | MISSING | Source translations | Language |
| Facebook mediation | Gradle plus ProGuard keep | Absent | MISSING | Source ads dependency | Ads |
| FileProvider | `${applicationId}.fileprovider` | Already on the QR product | COMPLETE | None | None |
| Queries MAIN/LAUNCHER | App list | Present | COMPLETE | None | None |
| HTTP/dial/social queries | Source manifest | Not copied | NOT APPLICABLE | Used by source QR open/share. Compose QR keeps its own open path | None |
| ProGuard | Source keep file | Empty template, minify off | PARTIAL | Keep `ReminderModel` and mediation when those land | Later |
| Deep links | Manifest search | None in source | NOT APPLICABLE | Nothing to port | None |

# 4. Previously Excluded Features

Earlier phases left these out. They are required now unless the status says otherwise.

| Item | New status |
| --- | --- |
| Source splash UI, VPN, notification permission, splash ads, referrer, install date | MISSING or PARTIAL |
| Phone, camera, overlay requests | MISSING |
| Full language list and `LocaleHelper` | PARTIAL |
| Default-home popup and `default_app_flow` | MISSING |
| `MainContainerFragment` QR UI | INTENTIONALLY REPLACED — OUR QR APP |
| Right-swipe interstitial | MISSING |
| Sub page and generated weather | MISSING |
| Quiz games and quiz creatives | MISSING |
| Call-end classes and phone-state receiver | MISSING |
| FCM, boot, reminders | MISSING |
| Widgets and `AppWidgetsActivity` | MISSING |
| App-proxy IP check | MISSING |
| Process app-open loader | MISSING |
| Crashlytics and Facebook mediation | MISSING |
| `FAQsActivity` | NOT APPLICABLE. Opened only from source `SettingsFragment` |
| Source scan/create/history/settings | NOT APPLICABLE |

# 5. Critical Missing Functionality

1. Startup is the Compose splash plus screen-flow routing, not the source splash.
2. Permission and Default Home do not request phone, camera, or overlay.
3. Referrer never selects `paid_user`.
4. Quiz does not show games or quiz creatives.
5. No sub page, no right-swipe interstitial, no default-home popup.
6. Home settings buttons do nothing.
7. Call-end, FCM, reminders, boot, and widgets are absent.
8. Application does not restore language or night mode and does not load the process app-open ad.
9. App-proxy location checks are absent.
10. FCM’s real behavior is a locked-screen call-end launch after a native preload, not a normal notification.

# 6. Detail For Partial And Missing Features

## Startup

- Source: `SplashActivity.java`, `activity_splash.xml`, `VPNHelper.java`, `dialog_vpn_detect.xml`, `ic_app_logo.png`
- Dependency: Ads, Firebase, install referrer
- Manifest: splash `MAIN`+`LAUNCHER`, `POST_NOTIFICATIONS`
- State: `appInstallDate`, `referrer_preferences`
- Remote Config: `Screen.SplashScreen`
- Analytics: `POST_NOTIFICATIONS_GRANTED`
- Target: `MainActivity`, `SplashScreen`, `StartupNavigation`
- Depends on: Remote Config, ads, widgets, reminders
- Phase: Startup

## Permissions

- Source: `PermissionActivity.java`, `DefaultActivity.java` phone step, `activity_permission.xml`, `img_permission.png`
- Manifest: `READ_PHONE_STATE`, `CAMERA`, `SYSTEM_ALERT_WINDOW`
- Analytics: `READ_PHONE_STATE_GRANTED`, `CAMERA_GRANTED`, `OVERLAY_PERMISSION_GRANTED`
- Target: `PermissionActivity` continues without requesting
- Phase: Permissions

## Language and theme

- Source: `LanguageActivity.java`, `LanguageAdapter.java`, `LocaleHelper.java`, `ThemeUtils.java`, `ThemeHelper.java`, prefs `language` and `APP_PREF` / `pref_theme`, `values-*`
- Target: English-only onboarding. `QrScannerApplication` does not restore theme
- Phase: Language

## Default Home

- Source: `DefaultHomePromptHelper.java`, `DefaultActivity.java`, `MainContainerFragment.maybeShowDefaultHomePopup`, prefs `default_app_flow`, `default_app_popup_preferences`
- Remote Config: `Default_App_Popup_Show`, `Default_App_Popup_Count`
- Target: helper only
- Phase: Default Home

## Launcher gaps

- Source: `LauncherHomeFragment` settings clicks, `fragment_sub_container.xml`, `view_weather_card.xml`, `item_weather_hourly.xml`, `WeatherForecastGenerator.java`
- Target: empty `openSettingsTodo`, one-page pager
- Phase: Launcher, then Sub page

## Quiz and ads

- Source: `LauncherQuizGames.java`, `qz_*.xml`, `AdPlacement` quiz and app-proxy methods, `IPAddressHelper.java`, `MyApplication.fetchAd`
- Remote Config: `Ad_Priority`, `Google_Ad_Failed_Show_Quiz`, `Screen.QuizAdsDesign`, `AppProxyStructure`, `App_Open_Ad_Show`, `Right_Swipe_*`, `LauncherApp_Native_*`, `LauncherApp_Quiz_Icon_*`
- Target: Google loaders for the migrated screens. Quiz flag hides the slot
- Depends on: Remote Config
- Phase: Ads, Quiz

## Remote Config and referrer

- Source: `RemoteConfigHelper.java`, install referrer in splash and launcher home, string resources for marketing referrers
- Target: `RemoteConfigHelper` applies the screen list and a subset of ad fields
- Phase: Remote Config

## Call-end and phone state

- Source: `ClEndActivity`, `ClContentFragment`, `ClMoreOptionFragment`, `ClMessageFragment`, `ClReminderFragment`, `CallEndReceiver`, `PhoneCallStateService`, `CallEndLaunchHelper`, `CallEndViewManager`, `CallEndPendingLaunch`, `CallEndFullNotificationHelper`, `CallEndBackAd`, `notification_full_ad.xml`, `Theme.ClEnd`
- Permissions: `READ_PHONE_STATE`, `USE_FULL_SCREEN_INTENT`, `SYSTEM_ALERT_WINDOW`, `WAKE_LOCK`, `BIND_SCREENING_SERVICE`
- Remote Config: `Cl_End_Screen_Show` and the call-end block saved by `ensureClEndConfig`
- Analytics: `CL_END_APP_OPEN_LOAD`, `CL_END_APP_OPEN_FAILED`, `CL_END_INTER_LOAD`, `CL_END_INTER_FAILED`, `FULL_NATIVE_LOAD`, `FULL_NATIVE_FAILED`
- Target: none
- Phase: Call-end

## Notifications and reminders

- Source: `MyFirebaseMessagingService`, `BootReceiver`, `ReminderReceiver`, `ReminderAlarmHelper`, `ReminderModel`
- Permissions: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`
- Dependency: `firebase-messaging`
- Target: none
- Depends on: call-end native preload
- Phase: Notifications, Reminders

## Widgets

- Source: `AppIconWidgetProvider`, `CreateQrA/B/CWidgetProvider`, `ScanQrA/BWidgetProvider`, `BaseAppWidgetProvider`, `AppWidgetsActivity`, `WidgetPinHelper`, `WidgetNavigation`, `xml/app_widget_*`
- Behavior to preserve: pin and update. Tap target becomes Compose scan or create, not `BatchScanActivity` or source create screens
- Phase: Widgets

# 7. Dependencies / Manifest / Permissions Gap

Present: AppCompat, Activity, Fragment, ViewPager2, Material, SDP, Lottie, Shimmer, Ads 24.7.0, Firebase BOM, Config, Analytics, conditional Google Services plugin.

Missing: `installreferrer` 2.2, `firebase-messaging` 23.4.1, `firebase-crashlytics` 18.6.1, Crashlytics plugin, `lifecycle-process`, Facebook mediation 6.21.0.0, Audience Network 6.21.0.

Manifest missing: `READ_PHONE_STATE`, `POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW`, `USE_FULL_SCREEN_INTENT`, `WAKE_LOCK`, exact alarms, `RECEIVE_BOOT_COMPLETED`, `AD_ID` if not merged by the ads SDK, telephony feature, call-screening service, phone-state receiver, FCM service, boot receiver, reminder receiver, six widget receivers, `ClEndActivity`.

The current AdMob meta-data is Google’s published sample app id. Do not copy a source production id.

# 8. Firebase / Remote Config / Analytics Gap

The target `google-services.json` matches `com.qrcode.scanner`. This audit did not fetch Remote Config or log an event on a device.

Still absent: `paid_user`, splash keys, right-swipe keys, call-end keys, quiz design, app proxy, popup keys, app-open show keys, interstitial click, sub-page native keys, 12-hour refetch, Crashlytics, FCM, and the analytics events tied to flows that are not migrated yet.

# 9. Ads / AdMob Gap

Working code paths: language, permission/default, intro, after-default, launcher settings, drawer native list, click, return, `ad_impression`.

Absent: splash, after-splash, right-swipe, sub native, quiz creatives, app proxy, process app-open load, call-end ads, FCM native preload.

# 10. Services / Receivers / Providers / Install Referrer Gap

| Source | Target |
| --- | --- |
| Drawer package receiver | Present |
| `CallEndReceiver` | Missing |
| `PhoneCallStateService` | Missing |
| `ReminderReceiver` | Missing |
| `MyFirebaseMessagingService` | Missing |
| `BootReceiver` | Missing |
| Widget receivers | Missing |
| FileProvider | Present |
| Install Referrer | Missing |

# 11. Launcher / Right-Swipe Integration Gap

Source page 0 → do not use.

Target: `LauncherHomeActivity.openQrShell()` → `MainActivity`.

That `MainActivity` launch has no `CATEGORY_LAUNCHER`, so the Compose splash does not show again.

Still missing on that path: the right-swipe interstitial, the default-home popup that source ran from `MainContainerFragment`, and a real settings open.

# 12. Recommended Implementation Order

1. Startup, including VPN, notification permission, splash ads, install date, and the Remote Config callback.
2. Permission requests.
3. Language list, locale helper, and night mode.
4. Install referrer and the remaining Remote Config keys.
5. Quiz, app proxy, right-swipe interstitial, and process app-open.
6. Sub page and the empty settings shortcut.
7. Default-home popup and setup prefs.
8. Call-end and phone state.
9. FCM, reminders, and boot.
10. Widgets, with taps opening the Compose QR app.
11. Crashlytics, mediation, and ProGuard keeps for the new models.
12. Device verification. Do not mark a flow passed unless it was run.

# 13. Every Source Java Class

Statuses after a direct listing of `app/src/main/java`. No menu XML and no navigation XML exist in the source. No app-link intent filter exists.

| Class | Status |
| --- | --- |
| `MyApplication` | PARTIAL. Ads init exists. Language, theme, process app-open, and call-end config do not |
| `SplashActivity` | PARTIAL. Routing only, through the Compose splash |
| `LanguageActivity`, `LanguageAdapter` | PARTIAL. English only |
| `LocaleHelper` | MISSING |
| `CollectionActivity` | COMPLETE |
| `PermissionActivity` | PARTIAL. UI only |
| `DefaultActivity`, `DefaultHomePromptHelper` | PARTIAL |
| `Intro1Activity`, `Intro2Activity`, `Intro3Activity`, `IntroNavigation` | COMPLETE |
| `ScreenFlowNavigation`, `TrackOnce` | COMPLETE |
| `RemoteConfigHelper` | PARTIAL |
| `AdPlacement` | PARTIAL |
| `OnInterstitialAdListener` | PARTIAL. The target uses its own listener on `AdPlacement` |
| `LauncherHomeActivity`, `LauncherHomeFragment`, `LauncherPagerAdapter` | PARTIAL. One page, empty settings action |
| `LauncherAppsBottomSheet`, `LauncherAppsAdapter`, `LauncherAppsHelper`, `LauncherAppsIconCache`, `LauncherAppsModel`, `LauncherAppsDisplayItem`, `OnLauncherAppLongClickListener`, `LauncherAppContextPopup` | PARTIAL. Quiz rows are not built |
| `LauncherSettingsActivity`, `LauncherSettingsHelper` | COMPLETE |
| `LauncherQuizGames`, `QuizGameItem` | MISSING |
| `SubContainerFragment`, `WeatherForecastGenerator`, `WeatherModel`, `WeatherHourlyModel` | MISSING |
| `MainContainerFragment` | INTENTIONALLY REPLACED — OUR QR APP |
| `MainNavigationHost`, `MainNavigationHosts` | PARTIAL. Home implements the host. Source `MainActivity` host is the QR shell |
| `VPNHelper` | MISSING |
| `IPAddressHelper` | MISSING for app proxy. The call from `MainContainerFragment` is not ported with that page |
| `ThemeUtils`, `ThemeHelper` | MISSING |
| `AppUtils` role, screen-flow, and analytics methods | PARTIAL. Referrer, full language, and setup prefs are absent |
| `ClEndActivity`, `ClContentFragment`, `ClMoreOptionFragment`, `ClMessageFragment`, `ClReminderFragment`, `ClEndPagerAdapter`, `ClReminderAdapter`, `OnReminderDeleteListener` | MISSING |
| `CallEndReceiver`, `CallEndViewManager`, `CallEndPendingLaunch`, `CallEndLaunchHelper`, `CallEndFullNotificationHelper`, `CallEndBackAd`, `ADSNativeFullDisplay` | MISSING |
| `PhoneCallStateService` | MISSING |
| `MyFirebaseMessagingService` | MISSING |
| `ReminderAlarmHelper`, `ReminderReceiver`, `ReminderModel`, `BootReceiver` | MISSING |
| `AppWidgetsActivity`, `WidgetPinHelper`, `WidgetNavigation`, `WidgetType`, `BaseAppWidgetProvider`, six widget providers | MISSING |
| `MainActivity`, scan/create/history/settings fragments, social and general create activities, barcode and template classes, `FAQsActivity`, `FaqAdapter`, `HistoryStore`, `BatchScanStore`, history repositories, `CountryCode*` | NOT APPLICABLE |

Reminder alarms use action `com.phonecleaner.virusclean.ACTION_REMINDER_ALARM`, channel `reminder_alerts_persistent`, and prefs `reminder_preferences` / `reminder_list` stored with Gson. Gson is also used by source history and country codes. The reminder use is in scope. The history use is not.

Call-end state files not previously named: `cl_end_config_preferences`, `cl_end_ad_preferences`, `call_end_pending_launch` (`pending`, `mobile_number`, `start_ms`, `end_ms`, `call_type`, `formatted_duration`). Country cache file `ipCountryName` belongs to the ad/IP helper.

# 14. Final Completion Checklist

- [ ] Source startup behavior, then the existing screen flow
- [ ] Language, collection, permission requests, default home, intro
- [ ] HOME launcher, drawer, settings, sub page
- [ ] Right-swipe destination is Compose QR, with the source interstitial
- [ ] Quiz, referrer, Remote Config keys, and ad fallbacks
- [ ] Call-end, phone state, FCM, reminders, boot, widgets
- [ ] TrackOnce events that exist in the source for those flows
- [ ] Compose Scanner, Create, History, and Settings unchanged

## FINAL MIGRATION RULE

Before declaring migration complete, verify:

SOURCE FUNCTIONALITY = TARGET FUNCTIONALITY

except only:

1. Source QR product → target Jetpack Compose QR product
2. Source right-swipe second-app UI → target Jetpack Compose QR product

Everything else in this audit must be implemented. A feature left out by Phase C–H is not finished.

## Investigation Status

SOURCE CODE CHANGED: NO  
TARGET CODE CHANGED: NO  
THIS FILE UPDATED: YES
