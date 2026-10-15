---
name: launcher-settings-widgets
description: Build, modify, debug or port the launcher settings and home-screen widgets of this app - LauncherSettingsActivity (app serialize / sort order, label visibility, app icon size and label size sliders with a live preview), LauncherSettingsHelper (SharedPreferences + change-listener bus that the Home page and drawer subscribe to), ThemeHelper (light / dark with return-to-Home flag), LocaleHelper (language change listener), and the widget system - WidgetType enum, AppWidgetsActivity (widget picker), WidgetPinHelper (requestPinAppWidget), BaseAppWidgetProvider and the six AppWidgetProvider classes with their widget layouts, xml provider infos, preview drawables and strings. Use when the user wants launcher settings, icon / label size options, sort options, dark mode switch for a launcher, pinnable home-screen widgets (1x1 shortcuts), or to copy these into another project.
---

# Launcher settings + widgets

Reference implementation: QR Code & Scanner, `launcher/activities`, `launcher/helpers`, `launcher/widgets`. Sources and resources are in `assets/`. The Home page and the drawer read these settings: see `launcher-home-screen` and `launcher-app-drawer`.

## Settings

`LauncherSettingsHelper` is the single source of truth (SharedPreferences) and a tiny event bus:

| Setting | Key | Range / values | Notify flag |
|---|---|---|---|
| Sort order | `appSerialize` | `a` name A-Z (default), `d` name Z-A, `i` installed date | `CHANGE_SERIALIZE` |
| Label visibility | `labelVisibility` | boolean, default true | `CHANGE_LABEL_VISIBILITY` |
| App icon size | `appIconSize` | 40-80 (default 60) dp | `CHANGE_ICON_SIZE` |
| App label size | `appLabelSize` | 10-16 (default 12) sp | `CHANGE_LABEL_SIZE` |
| Right-swipe tutorial shown | `right_swipe_tutorial_shown` | boolean | - |

Setters clamp the value, write it and call `notifyChanged(flag)`; listeners (`registerChangeListener / unregisterChangeListener`, a `CopyOnWriteArrayList`) receive a bit mask and re-bind only what changed. `CHANGE_DISPLAY` = icon | label | visibility. The Home activity, Home fragment and drawer each register one listener in `onCreate/onViewCreated` and unregister in `onDestroy`.

`LauncherSettingsActivity` (layout `activity_launcher_settings`, theme `Theme.LauncherSettings`): a preview card (4 sample icons `ivAppIcon1..4` + names `tvAppName1..4`) that shows the chosen icon / label size, a `SwitchCompat` for label visibility, two `SeekBar`s (`custom_seekbar_progress/thumb`) for the sizes, and a "Serialize" dropdown row (`llDropdown`) that opens the `dialog_serialize` popup with three options (check mark views `ivNameAscSelect` etc.) (`by_name_asc_a_to_z`, `by_name_desc_z_to_a`, `by_installed_date`). Every control writes through the helper immediately, so the Home screen behind it updates live. The reference also shows an interstitial / banner via `AdPlacement` / `ScreenInterAds` - remove for other apps.

`ThemeHelper`: `getSavedTheme / saveTheme / applyTheme(THEME_LIGHT|THEME_DARK)` (`AppCompatDelegate.setDefaultNightMode`), plus two one-shot flags used by the launcher: `markThemeChangedPending` -> `consumeThemeChangedPendingForHome` and `markLauncherReturnHomeOnNextResume` -> `consumeLauncherReturnHomeOnResume`, so after switching theme the Home activity scrolls back to the Home page instead of staying on the page that was recreated.

`LocaleHelper`: `registerLanguageChangeListener / unregisterLanguageChangeListener`, `syncResources(context)` - Home registers a listener so labels refresh when the language screen changes the locale (per-app locales through `AppCompatDelegate.setApplicationLocales`).

## Widgets

```
WidgetType (enum)  APP_ICON, CREATE_QR_A/B/C, SCAN_QR_A/B   = preview drawable + title + description + provider class + target screen
AppWidgetsActivity  picker list (activity_app_widgets): one card per WidgetType, "Add" -> WidgetPinHelper.requestPin(activity, type)
WidgetPinHelper     AppWidgetManager.requestPinAppWidget(ComponentName(provider), null, null); toasts widget_pin_not_supported / widget_pin_failed
BaseAppWidgetProvider  abstract getLayoutResId() + getTargetScreen(); onUpdate sets a click PendingIntent on R.id.widgetRoot ->
                       WidgetNavigation.createLaunchIntent(context, target)
*WidgetProvider     6 tiny subclasses (layout widget_*.xml, xml/app_widget_*_info.xml for size / preview / resizing)
WidgetNavigation    TARGET_SCAN / TARGET_CREATE constants, createLaunchIntent -> MainActivity (APP SPECIFIC: replace with your destination)
```

Each provider needs: a layout (`widget_app_icon`, `widget_create_qr_a/b/c`, `widget_scan_qr_a/b`, root id `widgetRoot`), an `xml/app_widget_*_info.xml` (`minWidth/minHeight 40dp`, `resizeMode=none`, `widgetCategory=home_screen`, `updatePeriodMillis=0`, `previewImage`, `initialLayout`), and a `<receiver>` with the `APPWIDGET_UPDATE` filter and the `android.appwidget.provider` meta-data (see `assets/manifest.snippet.xml`).

## Resources included (`assets/res`)

| Kind | Files |
|---|---|
| Layouts | `activity_launcher_settings`, `dialog_serialize`, `activity_app_widgets`, `widget_app_icon`, `widget_create_qr_a/b/c`, `widget_scan_qr_a/b`, `shimmer_small_ad`, `shimmer_medium_ad` |
| XML | `xml/app_widget_{app_icon,create_qr_a,create_qr_b,create_qr_c,scan_qr_a,scan_qr_b}_info.xml` |
| Drawables | `custom_circle`, `custom_radius_12/16`, `custom_seekbar_progress/thumb`, `custom_switch_thumb/track`, `ic_app_icon_size`, `ic_app_label_size`, `ic_serialize`, `ic_arrow`, `ic_back`, `ic_done`, `ic_info`, `ic_widget_app_logo`, `ic_widget_create_qr_1..3`, `ic_widget_scan_qr_1..2` (night variants in `drawable-night/`) |
| Fonts | `sans_trial_regular`, `sans_trial_medium` |
| Colors | `values/colors_launcher.xml`, `colors.xml`, `values-night/colors_launcher.xml`: `primary`, `primary_light`, `surface_primary/secondary`, `text_primary/secondary` (+ `_light` / `_dark`), `launcher_bg_*`, `ad_background`, `ad_shimmer` |
| Dimens | `values/dimens_launcher.xml` (preview sizes, row paddings) |
| Themes | `Theme.LauncherSettings` (+ night) in `values/themes.xml`, `values-night/themes*.xml` |
| Strings | filtered `values/strings.xml` + 19 translations: `launcher_settings`, `serialize`, `by_name_asc_a_to_z`, `by_name_desc_z_to_a`, `by_installed_date`, `preview`, `label_visibility`, `hide_app_name`, `app_icon_size`, `app_label_size`, `app_widgets`, `choose_a_widget_to_quickly_access_your_qr_tools_directly_from_your_home_screen`, `quick_access`, `app_icon_1x1`, `qr_scanner_generator`, `create_1_x_1`, `style_a/b/c`, `scan_qr`, `create_qr`, `widget_pin_not_supported`, `widget_pin_failed`, `app_name` |

## Porting checklist

1. Copy `assets/java/*`, `assets/res/*`, manifest snippet (2 activities + one `<receiver>` per widget).
2. Settings only: copy `LauncherSettingsHelper`, `LauncherSettingsActivity` and its layout / drawables / strings; skip the widget files.
3. Widgets only: copy `WidgetType`, `WidgetPinHelper`, `AppWidgetsActivity`, `BaseAppWidgetProvider`, the providers you need, `WidgetNavigation` (rewrite `createLaunchIntent`), layouts, `xml/` infos, preview drawables. Rename providers / strings / previews to your brand; keep the `widgetRoot` id.
4. Remove ad calls (`AdPlacement`, `ScreenInterAds`, `RemoteConfigValues`) from the two activities (see `assets/_external_imports.txt`).
5. Test: change each setting and watch Home + drawer update with the settings screen still open; dark / light switch returns to Home; language change; pin each widget on a launcher that supports pinning (Android 8+) and on one that does not (toast); tap a widget while the app is closed.
