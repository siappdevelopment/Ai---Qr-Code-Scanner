---
name: app-ads-logic-all-in-one
description: The complete ads, priority and Remote Config logic of this launcher + QR app in one skill - every ad type (banner, adaptive banner, native small/medium/large/intro/full, interstitial, app open, Quiz self-promo versions), Google vs Quiz priority and failure fallback, per-day / install-day / country gates, every ad slot and layout for Splash, Language, Permission, Default Home, Intro, the Launcher (Home bottom ad, drawer native, app-click and return ads, right-swipe ad, Default Home popup and Set-as-Default card), the QR app screens (Main, Scan result, Create, Form, Preview, History, Settings, Other), Charging in/out screens, App Installed/Uninstalled screens, the Call End screen (ClEndActivity, back ad, notification full-screen native), the "make this app your default launcher" notification reminder, and every Remote Config key with default and example JSON. Use whenever the user asks about ads, ad priority, Remote Config keys, an ad that does not show or shows too often, adding a new screen with ads, porting the whole ads system to another app, or any of these screens.
---

# App ads logic - all in one

Reference app: QR Code & Scanner (launcher + QR app), Java/Kotlin package `com.qrcode.scanner` (applicationId `com.qrcode.scanner.spapp`). This file is the map and the rules; the detail lives in `references/` (read the one you need, they cite `file:line`) and the real code is copied in `assets/`. Onboarding order/flow (Splash -> Language -> ... -> Home) is in the sibling skill `onboarding-flow-all-in-one`; this skill covers the ads on those screens too.

## Read this first - which reference answers what

| Question | Read |
|---|---|
| What is key X, its type, default, effect? Full JSON? | `references/remote-config-schema.md` (+ `assets/snippets/remote_config.full.json`) |
| How do ad types, Quiz priority, gates, interstitial/native/app-open engines, layouts work? | `references/ads-engine.md` |
| Launcher ads, drawer, tap/return ads, right swipe, Default Home popup/card, QR-app screen ads | `references/launcher-and-app-screens.md` |
| Charging in/out, App installed/uninstalled | `references/event-screens.md` |
| Call End screen, notification full-screen ad, default-launcher reminder | `references/callend-and-reminder.md` |
| Splash/Language/Permission/Default/Intro ads and order | section "Onboarding ads" below + `onboarding-flow-all-in-one` |

## Architecture in 10 lines

1. **Config**: one Firebase Remote Config parameter `QRScanner_<versionCode><RC_KEY_SUFFIX>` holds a JSON. `RemoteConfigHelper` parses it into `ScreenFlowConfig`, `AdPlacement` (onboarding/launcher ids + flags) and `RemoteConfigValues` (everything else). Call-end/event/right-swipe values are also stored in prefs so a cold broadcast can read them.
2. **Engine**: `AdPlacement` (3000 lines) is the single place that loads and shows Google ads and the Quiz fallback; `ScreenInterAds` handles click/back/bottom-nav interstitials with counters; `ScreenLoadAd`/`ScreenNativeAds` and the Compose wrappers (`ScreenWithAd`, `ScreenBackAd`, `BigNativeAd`, `HomeBottomAdSlot`) place per-screen ads; `ProcessAppOpen` shows the resume app-open ad; `EventBottomAds`/`CallEndBackAd` serve the event and call-end screens; `QuizAds` renders the self-promo ads.
3. **Every slot is the same shape**: a flag (`*_Ad_Show`), a type (`*_Ad_Type`), a banner id, a native id, optional interstitial/app-open ids. Missing block = everything off.
4. **Quiz** = built-in self-promo ads (icons/titles/media from `QuizAdsDesign`, links from `AppProxyStructure`, filtered by city/state/country). It replaces Google ads when `Ad_Priority = QUIZ`, or backs them up when `Google_Ad_Failed_Show_Quiz` is true.
5. **Completion callbacks are sacred**: every full-screen ad path calls its `next` runnable exactly once (disabled, offline, no id, load fail, show fail, timeout, already shown). A screen that waits for an ad must never hang.

## Ad types

| Type | Sizes / forms | Loader (in `AdPlacement`) |
|---|---|---|
| Banner | small banner, adaptive banner (Quiz banner fallback) | `loadBannerAd`, `loadAdaptiveBannerAd`, `requestSmallBannerAd`, `requestAdaptiveBannerAd` |
| Native | `small`, `medium`, `large`, `intro`, `full` (full screen) + shimmer per size | `loadNativeAd` (master overload), `requestLargeNativeAd`, `showLargeNative`, `ADSNativeFullDisplay` (full) |
| Interstitial | on-demand (`load`) or `preload` | `loadInterstitialAdInternal`, `ScreenInterAds`, screen-specific helpers (splash, language, intro, after-default, right swipe, event back, launcher tap/return) |
| App open | splash/after-default/tap-return/event/call-end/resume | `loadAppOpenAdInternal`, `loadAfterSplashAppOpenAd`, `ProcessAppOpen` |
| Quiz (self-promo) | quiz banner, native (small/medium/large/intro/full), interstitial, app open, browser (opens a link) | `QuizAds` (+ `qz_*.xml` layouts) |

Layouts for all of them are in `assets/res/layout` (`native_*_ad_layout`, `qz_*`, `shimmer_*_ad`, `view_screen_load_ad`, `view_big_native_ad`, `view_home_bottom_ad`, `dialog_loading_ads`, `notification_full_ad`, `layout_event_shimmer_*`, splash ad layouts). Container contract: a `RelativeLayout` slot holding `rlBannerAdView`/`slBannerShimmer`/`llBannerAd` and `rlNativeAdView`/`slNativeShimmer`/`flNativeAd`; see `ads-engine.md` section 1.4.

## Priority and fallback (the rule of thumb)

1. `Ad_Priority == "QUIZ"` (root, or `Screen.QuizAdsDesign.Ad_Priority`) -> show the Quiz version of the slot and never request Google for it (exceptions are documented per area: event-screen back ads and Call End never use Quiz unless the sequence says `Quiz_*`; the launcher tap/return sequences ignore the priority flag).
2. Otherwise Google. If Google cannot run (SDK not started = no AdMob app id in the manifest, offline, empty id, load/show failure) and `Google_Ad_Failed_Show_Quiz` is true (launcher keeps its own copy under `LauncherAppScreen`, falling back to the root value) -> Quiz; else the slot hides / the flow continues.
3. Quiz needs data: all required `Quiz_*` lists non-empty (max 3 items used). With none, Quiz ads silently do nothing.
4. There is **no consent or ads-free check** in `AdPlacement`; add one at `canRequestAds` if the new app needs it.

## Gates that decide whether an ad appears

| Gate | Where | Rule |
|---|---|---|
| Flag + id | every slot | flag true and the id for the chosen type not empty (Quiz priority skips the id check) |
| Network | `isNetworkAvailable` | offline hides ads, flows continue |
| Per-day | `*_Per_Day`, `App_Open_Show_Per_Day`, `Default_App_Popup_Count`, `back_ad_total_show_count`, `ClEnd_Back_Ad_Show_Per_Day` | minimum gap = 24 h / N since the last show; 0 usually = no limit (but 0 = never for event back ads) |
| Counter | `inter_ads_click`, `inter_ads_back_click`, `bottom_nav_inter_click`, `LauncherApp_Count`, `LauncherApp_Back_Count`, `Right_Swipe_Interstitial` | every Nth action; 0 = off (right swipe: 0 = every swipe) |
| Install day | `*_back_ad_day_count`, `ClEnd_Back_Ad_Show_After_Day`, `Notification_*_Install_Days` | allowed only after N days since `saveInstallDate` |
| Country | `*_show_country`, `Notification_*_Country` | **English country names** (not ISO codes), case-insensitive, empty = everyone; IP country when the IP flag is on |
| Sequence cursor | event back ads, ClEnd back ad, launcher tap/return | list of types (optionally with repeat counts) used in turn; cursor persisted; changing the list resets it |
| Default Home | `ProcessAppOpen`, right swipe | resume app-open only when this app is the default Home and the QR (right) page is open; Right Swipe ad is skipped when the Default Home popup is due |

## Onboarding ads (details in `onboarding-flow-all-in-one`)

| Screen | Slot | Keys (under `Screen.*`) |
|---|---|---|
| Splash | bottom small native or banner | `SplashScreen.Splash_Ad_Show/_Type/_Banner_Id/_Native_Id`, `Splash_Duration` |
| After splash | inter or app open | `After_Splash_Ad_Show/_Type/_Interstitial_Id`, root `App_Open_Id` |
| Language | big native / adaptive banner, optional inter on Done | `LanguageScreen.Language_*`, `Language_Interstitial_Ad_Show` |
| Permission + Default Home | small native/banner | `PermissionDefaultScreen.PermissionDefault_*` |
| After default | inter or app open after Allow/Cancel | `After_Default_Ad_Show/_Type` |
| Intro 1-3 | `intro` native/banner, optional inter after last page | `IntroScreen.Intro_*` |
| Inter id for Language/Intro/After-default | one shared id | `OtherScreen.Other_Interstitial_Id` -> root `Other_Interstitial_Id` -> `inter_ads.inter_ads_id` |

`inter_ads_click_type = preload` also makes the Language/Intro/After-default interstitials load ahead.

## Launcher and QR-app ads (details in `launcher-and-app-screens.md`)

- **Launcher**: Home bottom ad (sits under the QR page bottom bar), drawer in-list + floating native (`LauncherApp_Native_List_*`, per-day), large native on the launcher side page (`LauncherApp_Native_Ad_*`), app-icon **tap ad** every Nth tap (`LauncherApp_Count`, sequence `LauncherApp_Ad_Type`), **return ad** every Nth return from an external app (`LauncherApp_Back_*`, persisted counter), **right swipe** interstitial (`Right_Swipe_*`, `load`/`preload`), Quiz icons in the drawer, resume App Open.
- **Default Home UI when the app is not Home**: no wallpaper launcher; the QR UI hosts the same pages, with the Set-as-Default card (Home + drawer) and the `Default_App_Popup_*` popup on Right Swipe (popup due -> the right-swipe ad and its counter are skipped; count <= 0 = no limit; last-shown time saved before the user answers). `Redirect_Home_Launcher` forces the launcher destination.
- **QR screens**: `MainScreen` (bottom ad with auto refresh, big top native, bottom-nav inter), `ScanResult`, `CreateHub` (+ big top native), `QrForm`, `QrCustomization`, `QrPreview`, `History`, `HistoryDetail`, `LauncherSettings`, `SettingsFragment` (native with refresh seconds), `Other`. Each = `<Prefix>_Ad_Show/_Type/_Banner_Id/_Native_Id` plus `on_back_inter_ads_show` / `on_click_inter_ads_show`; interstitials need the global `inter_ads` master flags too.

## Charging and install/uninstall screens (details in `event-screens.md`)

Triggered by power connected/disconnected and package added/removed (runtime receiver + manifest receivers) -> `EventPromptLauncher` gates: per-kind flag (`charge_in_screen_show`, `charge_out_screen_show`, `install_screen_show`, `uninstall_screen_show`), internet, no call, debounce, overlay/default-Home/foreground, else full-screen notification. Ads: bottom native/banner (`load` or `preload` through `EventBottomAds`, Google only) and a **back ad** on Done/Back from a persisted `[type,count]` sequence (Google_Inter / Google_AppOpen / Google_Native full / `Quiz_*`), gated by install-day, per-day interval, country, with a preloaded copy so no loading dialog. Done button appears after `*_button_show_sec` (6). Block keys: `charging_screen_*` / `install_uninstall_screen_*`.

## Call End screen and notification ad (details in `callend-and-reminder.md`)

Started by the phone-state broadcast (overlay/default-Home tricks, locked-phone full-screen notification) or by an FCM message on a locked phone with a preloaded full-screen native. Gate for normal calls is only `Cl_End_Screen_Show`; install-day / country / permission-tier gates (`Notification_*`) apply to the FCM path. Ads: bottom banner/native (`ClEnd_Ad_*`, Google only), back-press ad with sequence `ClEnd_Back_Ad_Sequence` (inter/appopen/native, cursor advances only when an ad is actually shown), `Show_After_Day`, `Show_Per_Day`, country (+ IP country), and the notification full-screen native with optional close button. Four tabs: Content, Message, Reminder, More.

## Default-launcher reminder (app not default Home)

Keys `launcher_notification_push_show/_time/_title/_description` (in `launcher_home_screen` or root). `time` = times per day (interval = 86400 / time seconds; >= 15 min periodic WorkManager, below that a self-rescheduling chain). The worker posts only when this app is **not** the default Home, onboarding is finished and the app is in the background; tapping goes Splash -> `DefaultActivity(EXTRA_FROM_REMINDER)` -> role request -> launcher home (Back skips the request). It is cleared when the app comes to the foreground as default Home.

## Files in `assets/`

`java/` - AdPlacement, ScreenInterAds, ScreenNativeAds, ScreenLoadAd, QuizAds, ADSNativeFullDisplay, GestureSafeNativeAdView, AdTheme, ProcessAppOpen, EventBottomAds, EventPromptLauncher/Watch, CallEnd* helpers, DefaultLauncherReminder(+Worker), HomeBottomAd, IPAddressHelper, AnalyticsEvents, RemoteConfigValues/Helper, ScreenFlowConfig, AppProxyLookup, LauncherQuizGames, DefaultHomePopupHost, the Cl/Event/Charging/InstallUninstall activities, the receivers and services. `kotlin/` - Compose ad wrappers. `res/layout/` - every ad and event layout. `snippets/remote_config.full.json` - all keys; `snippets/manifest.ads-screens.xml.snippet` - components and permissions.
Not copied (take from the project): `AppUtils`, `WidgetNavigation`, themes/styles (`Theme.EventPrompt`, `Theme.ClEnd`), drawables, fonts, strings in ~19 locales.

## Porting / adding a screen with ads

1. Add dependencies (play-services-ads, firebase-bom + config/analytics/messaging, shimmer, work-runtime, installreferrer) and the manifest `APPLICATION_ID` meta-data; copy `assets/`, fix package and `R`.
2. Merge `manifest.ads-screens.xml.snippet`; keep the ads-related permissions.
3. Create the Remote Config parameter from `remote_config.full.json`; start with ads off, then enable per screen.
4. New QR screen: add its `<Object>Screen`/`<Prefix>` pair to `RemoteConfigValues.SCREEN_AD_KEYS`, call `ScreenLoadAd.attach` (views) or `ScreenWithAd` (Compose) for the bottom ad, `ScreenInterAds.onBack/onClick` for interstitials, and add the flags to the JSON.
5. New onboarding/launcher slot: add getters/setters in `AdPlacement`, parse in `RemoteConfigHelper.applyAdFields`, use `AdPlacement.showSlot(...)`; always route the next step through the ad's finish callback.
6. Never read `0` the same way for every key (see "Meaning of 0" in the schema reference).

## Test matrix (minimum)

Ads on/off per slot; offline; empty ids; `Ad_Priority=QUIZ`; Google fail + `Google_Ad_Failed_Show_Quiz` on/off; `load` vs `preload`; counters wrapping (N-1, N, N+1) and process restart (tap counter is memory-only, return counter persisted); per-day windows across midnight; install-day gate; English country names vs ISO codes; sequence change resets cursor; default Home vs not (resume app-open, popup, card); right swipe with popup due; charging in/out and install/uninstall from background, locked phone, during a call; call end normal vs FCM-locked; reminder after reboot and after becoming default; Android 12-14 and Xiaomi/Samsung background-start limits.

## Known gaps worth fixing when porting

No consent/ads-free gate; `ClEndActivity` is exported; Call End `isRingingCall` is never reset (outgoing may show as Incoming); `ensureLoaded` does not restore `appOpenId` for a cold broadcast; popup host in `MainActivity` effectively inactive (`markRightSwipeQrVisible` never called); `view_default_home_guide.xml` and `dialog_inter_quiz_shimmer.xml` unused; `layout_event_shimmer_*` names look swapped; event screens log no analytics of their own. Items marked "unverified" in the references were not confirmed on a device.
