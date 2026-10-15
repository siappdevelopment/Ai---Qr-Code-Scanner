---
name: launcher-home-screen
description: Build, modify, debug or port the Home launcher shell of this app - LauncherHomeActivity (the HOME-category activity with the 3-page ViewPager2: right page = app feature page, centre = Home over the wallpaper, left = sub page), LauncherHomeFragment (date / time, weather card, Google and Tools folders, 4-icon dock, swipe-up gesture that opens the app drawer, swipe-right gesture, long-press, icon size / label settings applied live), LauncherPagerAdapter, SubContainerFragment, system-bar handling, wallpaper window refresh, hiding the task from Recents, reacting to the HOME button / theme / language change / installed-app changes. Ships the classes, layouts, drawables, fonts, colors, dimens, themes and strings for the Home screen. Use whenever the user wants to turn another app into a launcher / home screen app, copy this launcher home to a new project, change the Home layout (clock, folders, dock), or fix Home-page problems such as a white or black background instead of the wallpaper, swipe gestures fighting the pager, or the Home activity opening twice.
---

# Launcher home screen (shell + Home page)

Reference implementation: QR Code & Scanner, package `com.qrcode.scanner`, `launcher/`. Everything is in `assets/` so it can be copied into another project. Companion skills: `launcher-app-drawer` (the swipe-up app list), `launcher-settings-widgets` (settings + home-screen widgets), `default-home-role-flow` / `default-home-screen` (asking Android to make the app the Home app), `right-swipe-ads`, `launcher-app-click-ads` (ads on this screen).

## Structure

```
LauncherHomeActivity   theme Theme.Home (transparent window + FLAG_SHOW_WALLPAPER), layout activity_launcher_home (one ViewPager2: vpLauncher)
 └─ LauncherPagerAdapter (FragmentStateAdapter, 3 pages)
      PAGE_RIGHT = 0  LauncherQrPageFragment.kt   Compose page of the app's own feature (QR scanner here) - REPLACE with your page
      PAGE_HOME  = 1  LauncherHomeFragment        the real Home page (layout fragment_launcher_home)
      PAGE_SUB   = 2  SubContainerFragment        empty container for a second page (layout fragment_sub_container)
LauncherQrSystemBars.kt  status / navigation bar colours for the feature page vs the transparent Home page
MainNavigationHost       small interface the feature page uses to navigate
WeatherForecastGenerator + WeatherModel + WeatherHourlyModel   offline sample weather for view_weather_card / item_weather_hourly
```

### LauncherHomeActivity responsibilities

- Manifest: `MAIN + HOME + DEFAULT` intent filter, `singleTask`, `taskAffinity=""`, `excludeFromRecents`, `clearTaskOnLaunch`, `stateNotNeeded`, portrait (see `assets/manifest.snippet.xml`). The HOME + DEFAULT filter is what lets the user choose the app as the Home app.
- `onCreate`: registers the `LauncherSettingsHelper` change listener, `hideLauncherFromRecents()` (`AppTask.setExcludeFromRecents(true)`), `setContentView`, `setupLauncherHome()` (pager, `preloadInstalledApps()` on a single-thread executor, `registerInstalledAppsReceiver()` for install / uninstall), back-press handler (closes the drawer first, otherwise returns to the Home page).
- `onNewIntent` -> `handleLauncherHomeIntent`: a HOME-button press closes drawer / folder dialogs and scrolls back to PAGE_HOME (also after a theme change).
- **Wallpaper fix** (`refreshWallpaperWindowIfNeeded`): after a recreate (language change) the translucent window can show a solid colour instead of the wallpaper; once the window has focus on the Home page it re-adds `FLAG_SHOW_WALLPAPER` and forces a layout. Keep this when porting.
- Pager input policy: swipes are disabled while the drawer is opening / scrolling (`setLauncherSwipeEnabled`, `applyLauncherPagerInputPolicy`, `recoverPagerScrollStateIfNeeded`) so a vertical drawer drag never turns into a page swipe.
- `openQrShell / openScanner / openCreate / openHistory`: go to PAGE_RIGHT or the feature page (app specific).
- `onUserLeaveHint`, `onStop`: dismiss overlays, remember page for `onSaveInstanceState` (`KEY_RESTORE_LAUNCHER_PAGE`).

### LauncherHomeFragment responsibilities

- Clock: `updateDateTime()` every second while resumed (`startDateTimeUpdates / stopDateTimeUpdates`).
- Folders: Google and Tools folder cards with a 2x2 icon preview (`loadFolderApps`, `bindFolderPreview`); tap opens `dialog_apps_folder` (`dialogAppsFolder`). Package lists are in `loadFolderApps` - edit for your app set.
- Dock: four icons - dialer, SMS, browser, camera (`loadBottomApps`, `launchDialerApp / launchSmsApp / launchBrowserApp / launchCameraApp`); each resolves the user's default app and launches it with `startResolvedActivity`; Google voice search button (`launchGoogleVoiceSearch`).
- Bottom action row: scan / create / history / settings cards (`bg_home_action*`, app specific).
- Gestures (`onHostTouchEvent`, called from the activity's `dispatchTouchEvent`): swipe up opens `LauncherAppsBottomSheet` interactively (`beginInteractiveAppsSheet`, `handleAppsSwipeMove`, `handleAppsSwipeEnd`, `maybeOpenFromQuickFling`); swipe right moves the pager to PAGE_RIGHT; `setupSwipeAwareClick` makes taps ignore a drag. Thresholds live in `dimens_launcher.xml`.
- Settings applied live: `applyLauncherSizeSettings()` re-sizes icons / labels when `LauncherSettingsHelper` notifies a change (`launcher-settings-widgets`).
- Language change: `LocaleHelper` listener -> `refreshLayoutForLanguageChange()` re-reads strings.
- App-specific hooks you can delete when porting: `AdPlacement.*`, `DefaultHomePromptHelper` / default-home popup, `RemoteConfigHelper`, `InstallReferrerStartup`, `AnalyticsEvents`, right-swipe tutorial, `ComposeScanRequest`.

## Resources included (`assets/res`)

| Kind | Files |
|---|---|
| Layouts | `activity_launcher_home`, `fragment_launcher_home`, `fragment_sub_container`, `dialog_apps_folder`, `view_weather_card`, `item_weather_hourly`, `shimmer_large_ad` |
| Drawables | `bg_home_action`, `bg_home_action_icon`, `custom_button_round_right`, `custom_radius_12`, `ic_home_action_{create,history,scan,setting}`, `ic_weather_*` (5 vectors), `ic_arrow`, `ic_google`, `ic_google_voice`, `ic_create_qr_home`, `ic_settings_home`, `ic_defult_vector`, `ic_widget_app_logo`, `img_app_icon_round`; night variants in `drawable-night/` |
| Fonts | `sans_trial_regular / medium / semi_bold` |
| Colors | `values/colors_launcher.xml`, `colors.xml` (+ `values-night/colors_launcher.xml`): `launcher_bg(_light/_dark)`, `surface_primary/secondary` (light/dark pairs), `text_primary/secondary` (light/dark pairs), `primary`, `home_action_*` (card surface, stroke, icon well), `dock_*` accent colours, `ad_background`, `ad_shimmer` |
| Dimens | `values/dimens_launcher.xml` (icon sizes, folder paddings, dock sizes, swipe distances) |
| Themes | `values/themes.xml`, `values-night/themes*.xml`: `Theme.Home` (transparent, wallpaper) and the other themes of the app |
| Strings | `values/strings.xml` filtered to the 3 strings the Home code uses + the same keys in 19 `values-xx/` translations (`launcher_default_title`, `launcher_default_subtitle`, `app_name`). Most Home text is icon-only or comes from `PackageManager` labels. |

`assets/external_dependencies` is `_external_imports.txt`: the project classes the copied code still imports (AppUtils, AdPlacement, ...). Replace or stub each one when porting.

## Porting checklist

1. Copy `assets/java/*` into your package (fix `package` lines and `com.qrcode.scanner.spapp.R`), `assets/res/*` into `res/` (merge `values` files by hand if you already have the same names), the manifest snippet into `AndroidManifest.xml`.
2. Dependencies: `androidx.viewpager2`, `androidx.fragment`, `androidx.appcompat`, `com.google.android.material`; Compose only if you keep `LauncherQrPageFragment.kt` (otherwise make PAGE_RIGHT a normal fragment or drop the page and set `PAGE_COUNT`).
3. Port the helper classes from `launcher-app-drawer` and `launcher-settings-widgets` (`LauncherAppsHelper`, `LauncherAppsIconCache`, `LauncherSettingsHelper`, `LocaleHelper`, `ThemeHelper`) - Home does not compile without them.
4. Strip the ad / Remote Config / default-home lines you do not need (search the external imports list). Keep the HOME intent filter.
5. Replace the folder package lists, dock fallbacks and the bottom action row with your own.
6. Test: Home button from another app, back button on Home / on the drawer, rotation (portrait locked), language change (wallpaper still visible), dark mode, installing / uninstalling an app while Home is open, first launch with no default dialer / browser, "Always" Home selection after the system dialog.
