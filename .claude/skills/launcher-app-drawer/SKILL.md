---
name: launcher-app-drawer
description: Build, modify, debug or port the app drawer of this launcher - LauncherAppsBottomSheet (full-height BottomSheetDialogFragment opened by a swipe-up with interactive drag progress, search bar with keyboard handling, 4-column RecyclerView of installed apps, empty state, animated dismiss), LauncherAppsAdapter and its display items, the long-press popup (App info / Uninstall), LauncherAppsHelper (load, sort by name or install date, replace lists), LauncherAppsIconCache (rounded pre-rendered icons), package add / remove receiver, and the list layouts, drawables, colors, dimens and strings. Use when the user wants an app list / app drawer / all-apps screen in a launcher, a searchable installed-apps grid, uninstall-from-launcher, to copy the drawer into another project, or to fix drawer problems (slow first open, icons popping in, apps hidden behind the keyboard or an ad, uninstalled app still listed, back button not closing it).
---

# Launcher app drawer

Reference implementation: QR Code & Scanner, `launcher/dialogs`, `launcher/adapters`, `launcher/helpers`. Sources and resources are in `assets/`. The in-list ad is documented in the `drawer-native-ads` skill, the ad on app tap in `launcher-app-click-ads`; the Home page that opens the drawer is in `launcher-home-screen`.

## Pieces

| Class | Role |
|---|---|
|  `LauncherAppsBottomSheet` (`BottomSheetDialogFragment`) | The drawer window. `newInstance(interactiveOpen)`. Layout `bottom_sheet_launcher_apps`. |
| `LauncherAppsAdapter` | RecyclerView adapter, 4-column grid. Items are `LauncherAppsDisplayItem` (TYPE_APP, or the ad spacer slot). Applies icon size, label size and label visibility from `LauncherSettingsHelper`. Click -> launch the app; long-click -> `OnLauncherAppLongClickListener`. |
| `LauncherAppsModel` | `appName`, `packageName`, `installTime`, `Drawable icon`. |
| `LauncherAppsHelper` | `loadInstalledApps(context)` (launcher activities via `PackageManager`, excluding this app), `sortApps` (A-Z, Z-A, installed date from `LauncherSettingsHelper.getAppSerialize`), `replaceAppLists` (shared static lists used by the drawer and search), `isUninstallableApp`. |
| `LauncherAppsIconCache` | `warm(context, apps, sizePx, cornerRadiusPx)` pre-renders rounded icons off the main thread; `get(package, size)`; `createDisplayIcon`. This is why the second open is instant. |
| `LauncherAppContextPopup` | Long-press popup (`dialog_launcher_app_action`): **App info** (callback -> `AppUtils.openAppInfo`, port that helper: it opens `ACTION_APPLICATION_DETAILS_SETTINGS`) and **Uninstall** (`ACTION_DELETE`, needs `REQUEST_DELETE_PACKAGES`; hidden for system apps). |
| `ScrollForwardingRelativeLayout` | Forwards drags that start on the ad overlay to the list. |

## How the drawer behaves

- **Open:** the Home page tracks a swipe-up. `beginInteractiveAppsSheet` shows the sheet with `interactiveOpen=true`, then `beginInteractive / updateInteractiveProgress(progress)` slide it under the finger; on release `endInteractiveSession(velocityY)` completes or cancels. A quick fling opens it directly. `runWhenInteractiveReady` queues actions until the first layout.
- **Instant content:** `showCachedAppsInstantly()` binds the last loaded list (and warmed icons) before the animation ends; `scheduleEnrichment` / `enrichContentAfterOpen` then refreshes in the background so the open animation never stutters.
- **Search:** `etSearch` + clear button; `searchApps -> filterAppsByQuery` filters the shared list (name contains, case-insensitive) in memory; `updateEmptyState` shows the empty view; the sheet stays expanded when the keyboard opens (`maintainExpandedSheetOnSearchFocus`); IME insets become bottom padding of the list.
- **Settings live:** icon size, label size, label visibility and sort order changes (`LauncherSettingsHelper` listener) re-bind the list without closing the drawer (`handleLauncherSettingsChanged`, `applySerializeSettingsChange`).
- **Packages:** `registerPackageReceiver()` listens to PACKAGE_ADDED / REMOVED / REPLACED and updates the list (`removeUninstalledApp` removes one row with animation); the Activity also keeps the shared list fresh.
- **Dismiss:** back, drag down, tapping outside, or Home button. `requestAnimatedDismiss`, `dismissForBackgroundLeave` (when the user returns from a launched app, keep or drop the drawer - `markSuppressBackgroundDismiss()` before launching an app keeps it from closing on `onPause`).
- **Launching an app:** `LauncherAppsAdapter.launchApp` uses `getLaunchIntentForPackage` + `NEW_TASK`; the click-ad wrapper in `launcher-app-click-ads` goes around it.

## Resources included (`assets/res`)

| Kind | Files |
|---|---|
| Layouts | `bottom_sheet_launcher_apps` (search bar, `rvApps`, empty view, native ad overlay), `adapter_launcher_apps` (icon + label), `adapter_launcher_native_spacer`, `dialog_launcher_app_action`, `shimmer_medium_ad` |
| Drawables | `custom_label`, `custom_radius_12`, `custom_radius_16`, `ic_launcher_app_info`, `ic_launcher_app_uninstall`, `ic_more`, `ic_search`, `ic_widget_app_logo` (+ night variants if any) |
| Fonts | `sans_trial_medium` |
| Colors | `values/colors_launcher.xml`, `colors.xml`, `values-night/colors_launcher.xml`: `launcher_bg_light/dark` (translucent sheet background), `surface_primary/secondary`, `text_primary/secondary` (light / dark pairs), `primary`, `ad_background`, `ad_shimmer` |
| Dimens | `values/dimens_launcher.xml`: drawer corner radius, item size, native list ad height / margin / spacer height (`launcher_native_list_*`) |
| Themes | `values/themes.xml`, `values-night/themes*.xml` (sheet theme + dialog theme) |
| Strings | filtered `values/strings.xml` + 19 translations: `app_name`, `set_as_default`, `launcher_default_set_now` (the drawer shows a "set as default" prompt). The layouts still contain hard-coded English literals (`Search` hint, `SEARCH FOR MORE APPS`, `App Info`, `Uninstall`): move them to strings when you localize. |

## Porting checklist

1. Copy `assets/java/*`, `assets/res/*`, the manifest snippet (`<queries>` for LAUNCHER intents and `REQUEST_DELETE_PACKAGES`; Android 11+ hides other apps without `<queries>` or `QUERY_ALL_PACKAGES`).
2. Needs from other skills: `LauncherSettingsHelper` (icon / label size, sort key, change listener) from `launcher-settings-widgets`; `AppUtils`, `AdPlacement`, `DefaultHomePromptHelper`, `LauncherQuizGames` / `QuizGameItem` are optional - delete the ad / quiz / default-prompt branches (see `assets/_external_imports.txt`).
3. Start the drawer from your Home (`LauncherAppsBottomSheet.newInstance(true).show(fragmentManager, tag)` then feed `updateInteractiveProgress`) or simply `newInstance(false)` from a button if you do not want the drag.
4. Preload: call `LauncherAppsHelper.loadInstalledApps` on a background thread when the launcher starts, `replaceAppLists`, `LauncherAppsIconCache.warm` with the icon size from settings.
5. Test: 200+ installed apps (scroll speed), search with keyboard (nothing hidden), uninstall one app from the long-press menu (row disappears), install an app while open, dark mode, change icon size in settings then reopen, back / Home / outside tap dismissal, work-profile and no-launcher-icon apps.
