# Phase 6 — Launcher Sub Page, Weather, and Settings Shortcut

Phase 6 only. Phase 7 was not started.

## Source files inspected

- `adapters/LauncherPagerAdapter.java` — pages are main (0), home (1), sub (2)
- `fragments/SubContainerFragment.java`
- `res/layout/fragment_sub_container.xml`, `view_weather_card.xml`, `item_weather_hourly.xml`, `shimmer_large_ad.xml`
- `common/WeatherForecastGenerator.java`, `models/WeatherModel.java`, `models/WeatherHourlyModel.java`
- Weather drawables `ic_weather_sunny`, `ic_weather_clear_night`, `ic_weather_cloudy`, `ic_weather_partly_cloudy`, `ic_weather_partly_cloudy_night`
- `activities/LauncherHomeActivity.java` — pager restore, back-to-home, cached native ad clear
- `fragments/LauncherHomeFragment.java` — `llSetting` and `llSettings`
- `adapters/MainPagerAdapter.java` — `PAGE_SETTINGS` fragment creation is commented out
- `fragments/SettingsFragment.java` — QR settings, including the row that opens `LauncherSettingsActivity`
- `dialogs/LauncherAppsBottomSheet.java` — drawer already opens launcher settings
- `interfaces/MainNavigationHost.java`, `helpers/MainNavigationHosts.java`
- `AdPlacement` launcher native ad show, per-day spacing, and unit id

No source analytics events were found for the sub page, weather, or the settings shortcut.

## Sub page

The source right-swipe page stays the existing Compose QR app. The sub page is the page to the other side of home.

The launcher pager is now home, then the sub page. A left swipe from home opens `SubContainerFragment`. System back, and a new HOME intent while the sub page is showing, return to launcher home. The current pager page is saved and restored. This does not reopen splash or onboarding.

The sub page shows:

- Time-of-day greeting: Good Morning, Good Afternoon, Good Evening, or Good Night. These strings are hardcoded in the source.
- A local weather card and six hourly rows
- Up to four installed recommended apps, in source package order: WhatsApp, Instagram, Google Pay, PhonePe, YouTube, then Settings. Missing packages are skipped. Sort and icon size follow the existing launcher settings.
- A large native ad when `LauncherApp_Native_Ad_Show` is on, the per-day spacing in `LauncherApp_Native_Ad_Show_Per_Day` allows it, and `LauncherApp_Native_Id` can load. Quiz priority can show the quiz native layout without a network connection. Load failure hides the slot.

## Weather

`WeatherForecastGenerator` builds the forecast on the device. There is no weather network API and no location permission.

The day high and low are seeded from the year and day of year, with a seasonal base. Hourly temperatures follow a cosine curve around 14:00 and change by at most 2 degrees between hours. Conditions are Sunny, Partly Cloudy, Cloudy, or Clear, with day and night icons. The location label is `Locale.getDefault().getDisplayCountry()`, or `Local` when that is empty. Temperatures use a degree sign and no unit conversion.

The same forecast is cached for the current day. The card first shows `--°` and `Loading weather…`. Generation runs off the main thread. A null result shows `Weather unavailable` and hides the hourly row. A failed generation does not block launcher startup or the QR app.

## Settings shortcut

The home Setting and Settings buttons open `LauncherSettingsActivity`.

That is the migrated launcher settings screen: label visibility, icon size, label size, and sort order. The app drawer already opened this same screen.

Source home buttons call `navigateToMainPage(PAGE_SETTINGS)`. `MainPagerAdapter` does not create `SettingsFragment`; that case is commented out. `SettingsFragment` is the source QR settings page. It was not migrated. Compose QR Settings was not changed and is not opened by these buttons.

## Ads

Only the sub-page native placement was added to the show path. It reads the Phase 4 values `LauncherApp_Native_Ad_Show`, `LauncherApp_Native_Ad_Show_Per_Day`, and `LauncherApp_Native_Id`. Spacing is stored in `launcher_app_native_ad_preferences`. Empty id, ads off, a failed load, or a failed quiz layout hides the slot. Settings-fragment native keys stay stored and are not shown, because that screen was not migrated.

## Language and night mode

The sub page runs inside `LauncherHomeActivity`, which already uses the Phase 3 locale and `Theme.Home` night mode. No second locale or theme system was added. QR theme preferences were not changed.

## Build and tests

- `:app:assembleDebug` — BUILD SUCCESSFUL
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL, 12 suites, 67 tests, 0 failures, 0 errors

## Device verification

Not performed. No device or emulator run was done for this phase.

## Intentionally pending (Phase 7+)

- Default Home popup and setup preferences
- Call-End and `PhoneCallStateService`
- FCM, reminders, `BootReceiver`, widgets
- Crashlytics, Facebook mediation, ProGuard migration
- Source QR Settings page and its theme dialog
- Source right-swipe second-app UI
