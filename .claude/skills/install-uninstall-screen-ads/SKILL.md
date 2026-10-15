---
name: install-uninstall-screen-ads
description: Build, modify, debug or port the ads flow of the App Installed / App Uninstalled screen (InstallUninstallScreenActivity on top of EventPromptActivity; the Charging screen uses the same engine) - the bottom ad (native or banner, Load vs PreLoad, EventBottomAds cache), the back ad shown when the user presses Done / Back (Google Inter, App Open or full-screen Native in a counted sequence, install-day gate, per-day interval, country list, preloaded copy so there is no loading dialog), the "Done" button delay, what must be true before the screen opens at all (internet, no call, not a held-back broadcast, overlay or default Home), and the Remote Config keys install_uninstall_screen_*. Ships the classes, layout, drawables, colors, fonts and strings. Use when porting the install / uninstall (or charging) prompt screens and their ads to another app, changing when or which ad shows, adding a new event screen, or fixing a screen that opens late, never shows an ad, shows the back ad every time, or closes by itself.
---

# Install / Uninstall screen - ads flow

Reference implementation: QR Code & Scanner, package `com.qrcode.scanner`, `launcher/`. Sources + resources are in `assets/`. The Charging screen is the same engine with the key `charging_screen`; only the Install / Uninstall half is described here. The permissions side (overlay, default Home) is in `overlay-permission-flow`; the sibling Call End screen is `callend-activity`.

## 1. What has to happen before any ad (screen launch gate)

```
PACKAGE_ADDED / PACKAGE_REMOVED
  EventPromptWatch (runtime receiver, registered in Application.onCreate)   <- manifest EventPromptReceiver is the backup
    ignore: EXTRA_REPLACING (app update), own package, isHeldBackDelivery() (first 3.5 s of the process / of the app coming forward:
            Android delivers old broadcasts when a frozen app wakes up)
  EventPromptLauncher.open(context, "install" | "uninstall")
    1. RemoteConfig: install_screen_show / uninstall_screen_show must be true
    2. hasInternet(): AdPlacement.isNetworkAvailable, else any validated INTERNET network (a sleeping app on Xiaomi reports no active network)
    3. not while a call is active / 5 s after any phone-state change / Call End screen visible (ClEndActivity.isVisible)
    4. same kind not twice within 1.5 s
    5. showEvent: needs the app in foreground, or the overlay permission, or default Home -> CallEndViewManager.showCallEnd (overlay trick)
       -> on failure a full-screen-intent notification (ids 9101-9104)
  InstallUninstallScreenActivity (extra event_kind, event_launched_at)
    onCreate: no internet -> finish(); stale launch (> 6 s old) -> leave without ad
```

If the screen does not appear, one of these gates failed; none of them shows an ad. Keep step 2 as is: using only `getActiveNetwork()` made the screen silently skip on phones that had put the app to sleep.

## 2. Bottom ad (`EventPromptActivity.loadBottomAd`, `EventBottomAds`)

| Key (prefix `install_uninstall_screen_`) | Meaning |
|---|---|
| `bottom_ad_show` | master switch for the bottom ad |
| `ad_load_type` | `Load`: request when the screen opens (shimmer first). `PreLoad`: also fetched ahead - `EventBottomAds.prepare` runs after the Mobile Ads SDK initialises and again after each screen, the screen takes the ready ad (`takeNative / takeBanner`) and shows it at once; falls back to loading if none is ready. Anything else = no bottom ad. |
| `bottom_ad_type` | `banner` -> inline adaptive banner (`banner_id`); anything else -> large native (`native_id`), card painted with `adCardColor` |
| `native_id`, `banner_id` | unit ids |
| `button_show_sec` | seconds of the hold progress bar before the **Done** button appears (default 6, `< 1` = 6) |

Rules: Google ads only (no Quiz priority / fallback on this screen); the cache is per screen (`CHARGING`, `INSTALL_UNINSTALL`); an ad is destroyed with the view before the window closes (`releaseAds`) or the native web view leaks; `onNewIntent` (a second event while the screen is open) restarts the hold bar and reloads the ad (`reloadAdsOnNewIntent`).

## 3. Back ad (`AdPlacement.showEventBackAd`)

The user can leave only when `canExit` (after the hold bar): Done button and Back both call `exitScreen()` -> `releaseAds()` -> `AdPlacement.showEventBackAd(activity, screenKey, this::finish)`. HOME / Recents / screen off close the screen **without** a back ad (`leaveWithoutAd`).

`showEventBackAd` shows an ad only if all hold (otherwise `finish` runs at once):

1. `<prefix>_back_ad_show` and network.
2. `back_ad_day_count > 0` and days since install >= it (`getDaysSinceInstall`).
3. `back_ad_total_show_count > 0` and `now - last_show >= 24 h / total_show_count` (`event_screen_ad_preferences/<screen>_back_ad_last_show_time`; written when an ad is about to show).
4. Country allowed (`back_ads_show_country`, empty = all).
5. A sequence step exists: `nextEventBackAdType` walks `back_ad_sequence` = pairs `[type, times in a row]` with a persisted cursor (`<screen>_back_ad_sequence_cursor`, reset when the sequence changes); steps whose unit id is empty are skipped (`inter_id` for Google_Inter, `full_native_id` for Google_Native, the global app-open id for Google_App_Open).

Then `showPreloadedEventBackAd` shows the ad that was loaded ahead (`preloadEventBackAd`, only with `ad_load_type = PreLoad`, same gates, one ready ad per screen, dropped after 55 min and when Quiz priority is on); if none is ready, `executeSequencedAd(..., quizOnFail=false)` loads it on demand behind a loading dialog (with a timeout, then `finish`). After the screen closes, `preloadEventBackAd` loads the next step.

| Back-ad key | Meaning |
|---|---|
| `back_ad_show` | master switch |
| `back_ad_sequence` | `[["Google_Inter",2],["Google_Native",1],["Google_App_Open",1]]` |
| `inter_id`, `full_native_id` | ids for inter / full-screen native |
| `back_ad_day_count` | do not show before this install day (`<= 0` = never) |
| `back_ad_total_show_count` | shows per day (`<= 0` = never); the interval is 24 h / value |
| `back_ads_show_country` | allowed countries |

The same keys with prefix `charging_screen_` configure the Charging screen; `install_screen_show`, `uninstall_screen_show`, `charge_in_screen_show`, `charge_out_screen_show` are not prefixed. Config is parsed in `RemoteConfigValues.applyEventScreen` and saved / restored (`eventInstallUninstall...` prefs) so the background receiver has it without a fresh fetch. Example: `assets/snippets/remote_config.json`.

## Files in `assets/`

| Asset | Goes to |
|---|---|
| `java/EventPromptActivity.java`, `InstallUninstallScreenActivity.java` | `launcher/activities/` (base screen + the install / uninstall subclass: texts, colours, hero image per kind) |
| `java/EventBottomAds.java`, `EventPromptLauncher.java`, `EventPromptWatch.java` | `launcher/common/` |
| `java/EventPromptReceiver.java` | `launcher/receivers/` |
| `res/layout/activity_install_uninstall_screen.xml`, `layout_event_shimmer_large/small.xml`, `native_large_ad_layout`, `native_small_ad_layout`, `native_full_ad_layout`, `shimmer_*` | the screen + ad layouts |
| `res/drawable/bg_event_*`, `ic_event_*` | card, button, progress, hero icons |
| `res/values*/colors*.xml`, `themes*.xml`, `dimens_launcher.xml`, `strings.xml` (+ 19 translations) | colours (`event_header`, `event_sheet`, `primary`, ad colours), `Theme.EventPrompt`, strings `event_install_*`, `event_uninstall_*`, `event_fast` |
| `res/font/sans_trial_*` | fonts |
| `snippets/AdPlacement.eventBackAd.snippet.java` + `.fields.snippet.java` | back-ad engine to paste into `AdPlacement` |
| `snippets/RemoteConfigValues.eventScreen.snippet.java` | `EventScreenConfig`, parse, save / restore, `isEventScreenEnabled` |
| `snippets/manifest.eventScreens.xml.snippet`, `remote_config.json` | manifest, theme, permissions, example config |

`assets/_external_imports.txt` lists what the code still needs from the app: `AdPlacement` (`loadNativeAd`, `showLargeNative`, `requestAdaptiveBannerAd`, `executeSequencedAd`, `presentNativeFullAd`, `canRequestAds`, `initializeIfConfigured`, `logAdRevenue`), `RemoteConfigValues`, `ChargingScreenActivity` (the sibling), `ClEndActivity.isVisible()` (delete if you do not port Call End), `CallEndViewManager` / `CallEndFullNotificationHelper` (from `callend-activity`), `ProcessAppOpen`, `AppUtils`, `AnalyticsEvents`.

## Porting checklist

1. Copy classes, layouts, drawables and values; merge the manifest snippet; call `EventPromptWatch.register(this)` in `Application.onCreate`.
2. Add the back-ad engine and `EventScreenConfig` snippets to your `AdPlacement` / config class; keep the key names so the same Remote Config JSON works in both apps.
3. Ask for the overlay permission (or make the app the default Home app) - without one of them a background start is blocked and the screen cannot show.
4. Make sure `MobileAds.initialize` calls `EventBottomAds.prepare(context)` in its completion callback (that is what makes `PreLoad` work).
5. Test: install and uninstall an app with Load and PreLoad, bottom ad native and banner, Done after 6 s, back ad day gate (`day_count` higher than the install day = no ad), second Done within the interval (no ad), sequence order (inter, inter, native, app open), no internet (screen should not open), airplane mode off/on, a call in progress (screen should stay closed), HOME while the screen is up (closes without ad), two quick installs (second event reuses the screen).
