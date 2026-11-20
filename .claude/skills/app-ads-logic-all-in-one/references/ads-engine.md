# Ads Engine Reference (shared engine, package com.qrcode.scanner)

Source of truth, read in full. Paths are relative to `app/src/main/java/com/qrcode/scanner/` unless noted.
`AP` = `launcher/common/AdPlacement.java` (3070 lines, all static, no instances). `RCV` = `launcher/remote/RemoteConfigValues.java`. `RCH` = `launcher/remote/RemoteConfigHelper.java`.
Anything not read directly is marked **unverified**.

Files in the engine:

| File | Role |
|---|---|
| launcher/common/AdPlacement.java | Core: SDK init, gating, banner/native/inter/app-open loaders, quiz fallback, preload caches, sequenced ads, call-end/event gating helpers |
| launcher/common/QuizAds.java | Self-promo ("Quiz") ads: banner, native (5 sizes), interstitial, app-open, full native, browser |
| launcher/common/ScreenLoadAd.java | Per-screen bottom banner/native slot (config from `ScreenAdConfig`) |
| launcher/common/ScreenInterAds.java | Per-screen click/back/bottom-nav interstitial counters + preload |
| launcher/common/ScreenNativeAds.java | "Big native" cards for HOME / SETTINGS / CREATE_HUB |
| launcher/common/HomeBottomAd.java | Small ad under Home bottom bar (not in the brief but required by `HomeBottomAdSlot`) |
| launcher/common/ProcessAppOpen.java | Resume App Open ad (process lifecycle) |
| launcher/common/ADSNativeFullDisplay.java | Static preload of one full-screen native (FCM / call end) |
| launcher/common/GestureSafeNativeAdView.java | Wrapper around `NativeAdView` that ignores bottom-edge up-swipes |
| launcher/common/AdTheme.java | Light/dark context forcing for ad inflation |
| launcher/common/AnalyticsEvents.java | Event names + `ad(...)` helper |
| launcher/helpers/AppProxyLookup.java | IP geo check that picks which quiz link list is active |
| launcher/helpers/LauncherQuizGames.java | Hard-coded list of 40 quiz games (`QuizGameItem`); **not used by QuizAds** (verified: QuizAds only reads RemoteConfig lists) |
| ui/components/{ScreenWithAd,ScreenBackAd,BigNativeAd,HomeBottomAdSlot}.kt | Compose wrappers |

---------------------------------------------------------------------

## 1. Ad types, loaders, `nativeSize` strings, layouts

### 1.1 Type -> loader matrix (Google path; Quiz path in 1.4)

| Ad type | Loader (AP unless noted) | Notes |
|---|---|---|
| Small banner (`AdSize.BANNER`) | `requestSmallBannerAd` :1239 -> `requestBannerAd` :1248 (adaptiveWidthDp = 0). Returns the `AdView` through callback; caller owns placement. Used by `HomeBottomAd.beginLoad` | no quiz fallback inside; caller decides |
| "Normal" banner into containers | `loadBannerAd` :977 | Actually sizes with `getAdSize` :2568 = **anchored adaptive**, full screen width |
| Anchored adaptive banner | `loadAdaptiveBannerAd` :1016-1034 (overloads: `googleOnly`, `largeInline=false`) | `getAdaptiveAdSize` :2584 |
| Inline adaptive ("large") banner | same method with `largeInline=true` | `AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize` |
| `requestAdaptiveBannerAd` :1244 | NB: javadoc says anchored but code calls `getCurrentOrientationInlineAdaptiveBannerAdSize` (**inline**). Width 0 -> screen width |
| Native in-slot (small/medium/large/intro/full) | `loadNativeAd` :1084-1190 (6 overloads) | inflates layout by `resolveGoogleNativeLayout` :2523 |
| Native "request only" (no view) | `requestLargeNativeAd` :1209 (name is historic; used for any size incl. small in `HomeBottomAd`) | returns raw `NativeAd` via `Consumer`; caller later calls `showLargeNative` :1289 (always `native_large_ad_layout`) or `bindLoadedNative` :1196 (re-show cached ad by type) |
| Native full-screen dialog (back/click sequence ad) | `showNativeFullAd` :2026 + `presentNativeFullAd` :2072 | layout `native_full_ad_layout`, theme `R.style.Theme_NativeFullAd`, close button `ivClose` |
| Native full preloaded (FCM / notification) | `ADSNativeFullDisplay.preloadNativeAd` | static `AdmobNativeAd`; consumed by `ClEndActivity` with `notification_full_ad` layout |
| Interstitial | `loadInterstitialAdInternal` :2246/:2250 (private); public wrappers below; `ScreenInterAds.loadGoogle` has its own loader | |
| App Open | `loadAppOpenAdInternal` :2325/2329/2333; `ProcessAppOpen.fetchAd` for resume | |
| Quiz banner | `QuizAds.showBanner` | `qz_banner_ad` |
| Quiz native small/medium/large/intro/full-in-slot | `QuizAds.showNative` (+`nativeLayout` :383) | |
| Quiz interstitial | `QuizAds.showInterstitial` | `qz_interstitial_ad`, theme `Theme_QuizInterstitial` |
| Quiz app open | `QuizAds.showAppOpen` | `qz_app_open_ad` |
| Quiz full native (dialog) | `QuizAds.showNativeFull` | `qz_native_full_ad`, theme `Theme_NativeFullAd` |
| Quiz browser | `QuizAds.openBrowserThenContinue` | opens link, continues when the host activity resumes |

Public interstitial/app-open helpers in AP (all end with a `notifyComplete` call):

| Method | Line | Behaviour |
|---|---|---|
| `loadAfterSplashInterstitialAd(activity,id,l)` | 541 | `loadInterstitialAdInternal(..., showLoadingDialog=false)` |
| `loadAfterSplashAppOpenAd` | 545 | `loadAppOpenAdInternal` (honors quiz priority, no dialog) |
| `loadLanguageInterstitialAd` | 1306 | gate `Language_Interstitial_Ad_Show`; preload cache if `inter_ads_click_type`=="preload"; else load with dialog |
| `loadIntroInterstitialAd` | 1320 | same, gate `Intro_Interstitial_Ad_Show` |
| `preloadOnboardingInterstitialAd(ctx, forIntro)` | 1341 | warms `preloadedOnboardingInterstitial` |
| `preloadAfterDefaultAd` / `loadAfterDefaultAd` | 1408 / 1460 | after-default (Set-as-Default) ad, inter or app-open |
| `loadRightSwipeInterstitialAd` / `prepareRightSwipePreload` | 323 / 311 | right-swipe inter (own counter + preload cache) |
| `handleLauncherAppClickAd` | 1484 | launcher icon tap ad every Nth tap |
| `handleLauncherAppReturnAd` / `showJoinedLauncherBackAd` | 1520 / 1542 | return-to-launcher ad |
| `showEventBackAd` / `preloadEventBackAd` | 1576 / 1672 | Charging / Install-Uninstall back ad |

### 1.2 `nativeSize` / `type` strings (case-insensitive)

`resolveGoogleNativeLayout` (:2523) and `QuizAds.nativeLayout` (:383) use identical mapping:

| string | Google layout | Quiz layout |
|---|---|---|
| `"large"` | native_large_ad_layout | qz_native_large_ad |
| `"medium"` | native_medium_ad_layout | qz_native_medium_ad |
| `"intro"` | native_intro_ad_layout | qz_native_intro_ad |
| `"full"` | native_full_ad_layout | qz_native_full_ad |
| anything else (null, `"small"`, `""`) | native_small_ad_layout | qz_native_small_ad |

`populateNativeAdView` (:2473) treats `large|medium|full` as "has media" (binds `ad_media`); `small`/`intro` bind only icon/headline/body/CTA.
`ScreenLoadAd.attach` only accepts `"small"` or else forces `"medium"` (:77).
Ad type strings for slot config: `"banner"` (case-insens.) -> banner, everything else -> native (`showSlot` :954-974, `ScreenLoadAd.isBanner`). Defaults differ per slot: language/other/splash-aware defaults are `"native"`/`"banner"` as set in the setters (AP :493-:720).

### 1.3 Layout inventory (res/layout)

| Layout | Purpose |
|---|---|
| native_small_ad_layout | Google small native (icon, headline, body, CTA, `ad_attribution`); used for small slots, `HomeBottomAd`, drawer |
| native_medium_ad_layout | Google medium native with `ad_media` |
| native_large_ad_layout | Google large native with `ad_media`; also used by `showLargeNative` (Home/Settings/Create big cards) |
| native_intro_ad_layout | Google intro (onboarding) native; no media |
| native_full_ad_layout | Full-screen Google native in a Dialog (`ivClose`, `statusBarScrim`, `ad_media`); via `presentNativeFullAd` |
| notification_full_ad | Full-screen native for FCM/call-end flow, inflated by `ClEndActivity` (:177) from `ADSNativeFullDisplay.AdmobNativeAd` |
| qz_native_small/medium/large/intro/full_ad | Quiz equivalents (ids: `ivQZAppIcon`, `ivQZAppMedia`, `tvQZLabel`, `tvQZAppTitle`, `tvQZAppDescription`, `btnQZClick`, `qzShimmerIcon`, `qzShimmer`, full has `ivClose`, `statusBarScrim`) |
| qz_banner_ad | Quiz banner (icon, label, title, description, button) |
| qz_interstitial_ad | Quiz full-screen interstitial (media, rating bar `rbQZRating`/`tvQZRate`, `tvQZUser`, close icon `ivQZClose`, `btnQZClose`, `tvQZTimer` hidden) |
| qz_app_open_ad | Quiz app-open (`llQZClose`, icon, media, `btnQZClick`) |
| qz_shimmer, qz_shimmer_app_open | Shimmer placeholders included inside quiz image holders (icon / media) |
| shimmer_small_ad / medium / large / intro | Shimmer skeletons matching the four non-full native sizes; `shimmer_small_ad` also banner skeleton |
| view_screen_load_ad | Slot used by `ScreenLoadAd`: `rlBannerAdView`>`slBannerShimmer`(include shimmer_small)+`llBannerAd`; `rlNativeAdView`>`slNativeShimmer`(include shimmer_medium)+`flNativeAd` |
| view_big_native_ad | Slot used by `ScreenNativeAds`: `rlNativeAdView`>`slNativeShimmer`(include shimmer_large)+`flNativeAd`; fill color `big_native_bg` |
| view_home_bottom_ad | Slot used by `HomeBottomAd`: same ids as view_screen_load_ad but native shimmer is `shimmer_small_ad` |
| dialog_loading_ads | Progress-bar loading dialog (horizontal indeterminate bar + text on `custom_radius_16`) shown while an inter/app-open/full-native loads, and as 1.5 s "App Open" pre-dialog |
| dialog_inter_quiz_shimmer | Shimmer dialog wrapper including `shimmer_medium_ad`. **Unused in code** (grep found no reference; ScreenInterAds uses `dialog_loading_ads` for the quiz 700 ms shimmer) |
| layout_event_shimmer_small / _large | Event screens (Charging / Install-Uninstall) bottom ad shimmers. NB naming looks swapped: `_small` is 250dp tall with a weight-1 media block, `_large` is 130dp media + row. Included at `activity_charging_screen.xml:260/274` and `activity_install_uninstall_screen.xml:260/274` (which include maps to which slot: **unverified**) |
| layout_splash_shimmer_small | Splash startup ad skeleton, included twice in `layout_splash_startup_ads.xml` (banner + native shimmer) |

### 1.4 Slot container contract (every slot layout)

Ids expected by the loaders: `rlAdView` (optional outer), `rlBannerAdView` (RelativeLayout) > `slBannerShimmer` (ShimmerFrameLayout) + `llBannerAd` (LinearLayout); `rlNativeAdView` (RelativeLayout) > `slNativeShimmer` + `flNativeAd` (FrameLayout). Google native views must be inflated into `flNativeAd` and wrapped via `GestureSafeNativeAdView.wrap`.

---------------------------------------------------------------------

## 2. Priority and Quiz

### 2.1 Config keys and static state

| Key (Remote Config JSON) | Setter (applied in) | Meaning |
|---|---|---|
| root `Ad_Priority` | `AP.setAdPriority` (RCH.applyAdFields :299 and RCV.apply :241 if inside `QuizAdsDesign`) | `shouldUseQuizPriority()` :209 = `"QUIZ".equalsIgnoreCase(adPriority)`. Any other value (incl. empty / "GOOGLE") = Google first |
| root `Google_Ad_Failed_Show_Quiz` (also inside `QuizAdsDesign`) | `AP.setGoogleAdFailedShowQuiz` | quiz fallback after a Google load/show failure (`getGoogleAdFailedShowQuiz()` :221) |
| `LauncherAppScreen.Google_Ad_Failed_Show_Quiz` (falls back to root value when absent, RCH :386) | `AP.setLauncherGoogleAdFailedShowQuiz` | launcher variant; used by sequenced launcher click/back ads through `launcherQuizOnGoogleFail()` :1976 = launcher flag OR global flag |
| `Native_Ad_Label_Color`, `Native_Ad_Button_Color_light`, `Native_Ad_Button_Color_dark` | AP setters | tint of Google AND Quiz label/button |
| `App_Open_Id` | `AP.setAppOpenId` | single app-open unit id used everywhere (splash, after default, sequences, resume) |

`QuizAdsDesign` object (RCV :239-261): `Quiz_App_Icon`, `Quiz_Native_Media`, `Quiz_Interstitial_Media`, `Quiz_App_Open_Media`, `Quiz_Banner_Title/Description`, `Quiz_Native_Title/Description`, `Quiz_Interstitial_Title/Description/Rate` (all string lists, replaced only if present), `Quiz_Button_Text` (first entry used; default text "Install").
`AppProxyStructure` object (RCV :264-281, also read from root): `App_Proxy_Check_Ip`, `App_Proxy_Ip_Checker_Url`, `Quiz_Link_List`, `Quiz_Link_List_Exclude`, `App_Proxy_List_City/State/Country`.

### 2.2 What "Quiz" means

Self-promotion ads that look like native/banner/inter/app-open ads, built entirely from Remote Config lists (not from `LauncherQuizGames`). Every ad picks `index = Random.nextInt(itemCount)` and uses the same index across lists so icon/title/description/media/rate stay synced.

- `RCV.getQuizSyncedItemCount` (:759): returns 0 if ANY of `quizAppIconList, quizBannerTitle/Description, quizNativeTitle/Description, quizInterstitialTitle/Description` is empty; otherwise `min(3, smallestListSize)` -> **at most 3 items are ever used**. 0 means every `QuizAds.show*` returns false (nothing shown).
- Click target: `RCV.pickQuizLink()` (:751) = random entry of `activeQuizLinkList`, else `quizLinkList`, else "". Opened with `ACTION_VIEW` + `FLAG_ACTIVITY_NEW_TASK` (`QuizAds.openLink` :371).
- Button text: `RCV.getQuizButtonText()` or "Install" (`applyButton` :569); text color forced white. Button/label background tinted by Native_Ad_* colors (`applyColors` :587; skipped if hex empty/invalid).
- Images: downloaded with `HttpURLConnection` (10 s timeouts) on a cached thread pool; empty/failed URL -> `R.mipmap.ic_launcher` fallback; shimmer shown until load; image tag guards against recycled views.
- Rating (interstitial only): `Quiz_Interstitial_Rate[index]`, else `FALLBACK_RATES[index]` 4.1..4.9, else 4.5; clamped 0..5. Users text from `USERS[index % 4]` ("50K+ Users" ... "1M+ Users").
- `bindTapOnly` (:652): click ignored when the touch moved more than touch slop (so swipes in ViewPager don't open links). Whole root and the button both open the link.
- Interstitial/app-open/full native are `Dialog`s (fullscreen, non-cancelable; close via icon/button), dismiss callback fires once.
- The container for banner/native is revealed by `showNative(..., revealContainer)`; `loadNativeAd` passes `false` in Quiz-priority path so sticky hosts (drawer) can position first, then it sets visible itself.

### 2.3 AppProxy gating (`AppProxyLookup.refreshActiveQuizLinks`)

Called at the end of `RCV.apply` (:283). Non-blocking.
```
active = quizLinkList                      // default, always set first
if !App_Proxy_Check_Ip or url empty: stop
IPAddressHelper.getLocationInfo(url) async (token-guarded, last call wins)
  match = city in App_Proxy_List_City OR state in ..._State OR country in ..._Country   (trim, lowercase equals; empty -> false)
  if match: active = Quiz_Link_List_Exclude (if non-empty) else Quiz_Link_List
  else/failure: active = Quiz_Link_List
```
So a matching location swaps users onto the "exclude" link list (e.g. safer/other links). It never blocks the ad itself.

### 2.4 Where priority is applied (rule: priority check first, then Google, then fallback)

| Entry point | Quiz-priority path | Google failed path |
|---|---|---|
| `showSlot` (:949) | network check skipped when QUIZ (offline still shows quiz) | via loaders |
| `loadBannerAd` | `QuizAds.showBanner`, else hide | `getGoogleAdFailedShowQuiz()` -> `QuizAds.showBanner`, else hide |
| `loadAdaptiveBannerAd` | skipped if `googleOnly`; else same | if `onGoogleFailedQuiz` runnable given: hide banner container, run it (caller shows a different quiz size); else `QuizAds.showBanner` |
| `loadNativeAd` | `QuizAds.showNative(type)`; if false hide + `onAdFailed`; if true paint, `onAdLoaded(null)` (**null NativeAd means "quiz shown"**) | on `onAdFailedToLoad`: quiz fallback if enabled and not `googleOnly`, else hide + `onAdFailed` |
| `canLoad` false (no AdMob id / SDK not started / offline / empty id) in `loadNativeAd` | **no quiz fallback** here: hides and calls `onAdFailed` (fallback only after a real Google load error) | |
| `loadInterstitialAdInternal(honorQuizPriority, quizOnFail)` | if priority QUIZ and `honorQuizPriority`: `QuizAds.showInterstitial` else notify | `canLoad` false -> quiz if `quizOnFail`; load failed / 10 s timeout / show failed -> quiz if `quizOnFail`; else `notifyComplete` |
| `loadAppOpenAdInternal` | same pattern with `QuizAds.showAppOpen` | same; also if activity finishing after load -> quiz or complete |
| `showNativeFullAd` | (sequence step, no priority check inside) | `canLoad` false / 10 s timeout / load failed -> `QuizAds.showNativeFull` if `quizOnFail` |
| `executeSequencedAd` | `Quiz_Inter|App_Open|Native|Browser` steps run Quiz directly (if show false -> continue) | Google steps use `quizOnFail` param |
| Preload helpers (right swipe, onboarding, after default, event back) | **skip preload and skip cache when QUIZ priority** | cached ad show-failure: onboarding -> quiz if flag; after-default/right-swipe -> just complete (right swipe retries `loadRightSwipeNow`) |
| `ScreenInterAds` | `present` -> `showQuiz` (see 4.) | `onAdFailedToLoad` / `onAdFailedToShowFullScreenContent` -> `QuizAds.showInterstitial` if flag; `canRequestAds` false / offline / empty id -> quiz only if flag (`showQuiz(activity,false,next)`) |
| `ScreenLoadAd` / `ScreenNativeAds` / `HomeBottomAd` | `isEnabled()` returns true even with empty ids when QUIZ priority | `onFailed` -> quiz if flag, else keep previous Google ad if any, else hide |

Event back ads (Charging / Install-Uninstall) deliberately set `quizOnFail = false` (:1808, `showEventBackAd` passes `false` to `executeSequencedAd`): Google only, no quiz fallback on show failure, but Quiz steps may be inside the sequence.

`ScreenNativeAds.beginLoad` in QUIZ priority: no network request at all; marks `lastCycleAt`, destroys cached Google ad.

### 2.5 Failure fallback summary

| Condition | Result |
|---|---|
| Google load fails | quiz if flag (else hide/continue) |
| Google show fails (inter/app-open) | quiz if flag, else continue |
| Empty unit id | banner/native: hide + failure callback (no quiz). Inter/app-open (`canLoad` false): quiz if `quizOnFail`, else continue. Screen slots: not enabled unless QUIZ priority |
| Offline | same as `canLoad` false; `showSlot` hides unless QUIZ priority |
| `canRequestAds` false (SDK not started or no AdMob meta app id) | same as `canLoad` false |
| Quiz lists unsynced (`itemCount` 0) | quiz returns false -> continue / hide |

---------------------------------------------------------------------

## 3. Initialization and gating

### 3.1 SDK
- `QrScannerApplication` (:30-31) calls `AP.initializeIfConfigured(this)` and registers `new ProcessAppOpen(this)`.
- `initializeIfConfigured` (:174): no-op if context null, already started, or `hasAdMobAppId` false. Calls `MobileAds.initialize`; on completion `EventBottomAds.prepare`. `adsSdkStarted` set true right after calling initialize (not on completion). Every loader re-calls it lazily.
- `hasAdMobAppId` (:189): manifest meta-data `com.google.android.gms.ads.APPLICATION_ID` non-blank.
- `canRequestAds` (:205) = `adsSdkStarted && hasAdMobAppId`. **There is no consent (UMP) check and no "ads-free/premium" check in AdPlacement** (verified by reading all 3070 lines). Any such gating must live elsewhere (**unverified**).
- `isNetworkAvailable` (:1940): active network has WIFI / CELLULAR / ETHERNET transport. (VPN-only not counted.)
- `canLoad(activity, unitId)` (:1956) = activity non-null and not finishing, `canRequestAds`, network, unitId non-empty. Note: no `isDestroyed` check.

### 3.2 Install-day logic
- `saveInstallDate` (:565): once, `SharedPreferences "appInstallDate"` key `install_date` = `yyyy-MM-dd` (English locale). Called from `StartupFlow` after remote config fetch (StartupFlow:162).
- `getDaysSinceInstall` (:2917): `(now - installDate) / 1day + 1` (day 1 on install day); returns **1** when date missing or parse fails. Parsed with `Locale.getDefault()` (possible locale mismatch, noted as a risk).
- Used with `*_Show_After_Day` / `backAdDayCount` style keys: ad allowed when `daysSinceInstall >= N` and `N > 0` (N <= 0 disables for ClEnd/event back ads).

### 3.3 Per-day interval counters ("N per day" -> interval = 24h / N)

Pattern: `intervalMs = 86_400_000 / countPerDay`; show if `now - lastShow >= intervalMs`; `countPerDay <= 0` means **always allowed** (except ClEnd/event back where <=0 means never).

| Feature | Check method | SharedPreferences file / key |
|---|---|---|
| App open resume | `canShowAppOpenAd` :289 (`App_Open_Show_Per_Day`) | `app_open_ad_preferences` / `last_show_time` (written in `onAdShowedFullScreenContent`) |
| Default app popup | `canShowDefaultAppPopup` :271 (`Default_App_Popup_Count`) | `default_app_popup_preferences` / `last_show_time` |
| Launcher app native (drawer?) | `canShowLauncherAppNativeAd` :249 | `launcher_app_native_ad_preferences` / `last_show_time` |
| Launcher app native LIST | `canShowLauncherAppNativeListAd` :1910 | `launcher_app_native_list_ad_preferences` / `last_show_time` |
| ClEnd back | `isClEndBackAdIntervalElapsed` :2886 (<=0 -> false) | `cl_end_ad_preferences` / `last_show_time`; cursor `back_ad_sequence_cursor` (reset in RCV when sequence changes) |
| Event back (charging/install) | `showEventBackAd` :1597 | `event_screen_ad_preferences` / `<screenKey>_back_ad_last_show_time`, `<screenKey>_back_ad_sequence_cursor` |
| Launcher return | `showJoinedLauncherBackAd` | `launcher_app_back_ad` / `launcherAppReturnCount`, `waitingForLauncherAppReturn`, `lastLaunchedPackage`, plus persisted back config (`backConfigSaved`, `backClickAdShow`, `backCount`, `backSequence` joined by `|`, `backInterstitialId`, `backQuizOnFail`) |
| Referrer / IP | `referrer_preferences` / `SetReferrerUrl`; `ipCountryName` / `ip_country_name` | |

In-memory counters (reset on process death): `rightSwipeInterstitialCount`, `launcherAppClickCount`, launcher sequence indexes, and all `ScreenInterAds` counters.

### 3.4 Country gating
`getDeviceCountry` (:2961): if `ClEnd_Back_Ad_Country_IP` and stored IP country non-empty -> that (upper-case); else telephony network ISO -> SIM ISO -> locale country, converted to English display name upper-case ("INDIA"). Lists compared with `equalsIgnoreCase` on that display name. Empty list = allowed everywhere (`isCountryAllowed` :1897, `isCountryAllowedForCallEnd` :2937). IP lookup `requestCallEndIpCountryIfNeeded` :2836 runs once at config apply.

### 3.5 Sequenced ad types
`normalizeLauncherAppAdType` (:2215) accepts (case-insens.): `Google_Inter|google_inter|inter`, `Google_App_Open|app_open|appopen`, `Google_Native|fullnative|full_native`, `Quiz_Inter`, `Quiz_App_Open`, `Quiz_Native`, `Quiz_Browser`; anything else -> "" (dropped when building the sequence).
- Launcher click/back: array of types, round-robin through `nextLauncherAppAdType(back)` (in-memory index).
- Event back: `backAdSequence` = list of `[type, count]`; type repeated `count` times in a persisted cursor (`nextEventBackAdType` :1843); steps whose Google unit id is empty are skipped; `advance=false` peeks (used by preload).
- Launcher native id for the full-native step = `RemoteConfigValues.getLauncherAppNativeId()`; event screens use `config.fullNativeId`; inter id per caller; app-open always `getAppOpenId()`.

### 3.6 ProcessAppOpen rules (resume App Open ad)

Constants: `AD_EXPIRY_MS` = 4 h (cached ad dropped if older), `APP_OPEN_DIALOG_DELAY_MS` = 1500.
```
onStart (process foreground): appInForeground=true; log analytics app_open;
   cancel/dismiss default-launcher reminder;
   if currentActivity==null or shouldSkipAppOpenAd(): return
   if App_Open_Dialog_Show && isAdAvailable(): show dialog_loading_ads for 1.5s then showAdIfAvailable
   else showAdIfAvailable
showAdIfAvailable: skip if shouldSkip or already showing
   if !App_Open_Ad_Show or !canShowAppOpenAd: clear cached ad, return
   if !isAdAvailable: fetchAd(); return        // loads for NEXT time, no show now
   set callbacks (dismiss/fail -> clear + refetch; onShowed -> setAppOpenLastShowTime)
   final re-check isDefaultHomeApp/activity alive, then show
onStop: appInForeground=false; fetchAd() (pre-warm for the return)
fetchAd: not if skip/loading/available; needs App_Open_Ad_Show, non-empty App_Open_Id, canRequestAds
isAdAvailable: ad!=null && App_Open_Ad_Show && canShowAppOpenAd && age<4h
shouldSkipAppOpenAd: !AppUtils.isDefaultHomeApp -> clear cached ad, skip.
   else !isRightSidePageOpen(currentActivity)
isRightSidePageOpen: LauncherHomeActivity at PAGE_RIGHT, OR MainActivity / any class in package com.qrcode.scanner.ui.*
```
So: only when the app is the **default Home**, only on the **right-side (QR) page**. `currentActivity` set in `onActivityStarted` unless an ad is showing. No Quiz fallback for resume app open. Per-day limit uses `App_Open_Show_Per_Day` with the shared interval formula. Static helpers `msSinceForeground()` / `isAppInForeground()` are used by other flows (**callers unverified**).

---------------------------------------------------------------------

## 4. Interstitial engine

### 4.1 ScreenInterAds (per-screen click / back / bottom-nav)

Config (RCV `applyInterAds`, root object `inter_ads`): `inter_ads_show`, `inter_ads_click` (every Nth click), `inter_ads_click_type` ("load" default | "preload"), `inter_ads_on_back`, `inter_ads_back_click`, `inter_ads_back_click_type`, `inter_ads_id`, `inter_back_ads_id`.
Bottom nav (inside `MainScreen`): `bottom_nav_inter_ads_show`, `bottom_nav_inter_ads_type`, `bottom_nav_inter_ads_id`, `bottom_nav_inter_click`.
Per screen (`SCREEN_AD_KEYS` objects + MainScreen + SettingsFragmentScreen): `on_back_inter_ads_show`, `on_click_inter_ads_show`.

```
onBack/onClick(activity, screenKey, next) -> handle(back?):
  countsToward(screenKey, back): master (inter_ads_on_back | inter_ads_show) AND
        ScreenAdConfig(screenKey).onBackInterShow|onClickInterShow   (unknown key -> false)
  every = inter_ads_back_click | inter_ads_click ; every<=0 -> next
  ++counter ; counter < every -> next          // counters are global per kind (not per screen), in memory
  counter = 0 ; present(kind, isPreload(type), next)
onBottomNav: bottom_nav_inter_ads_show, every=bottom_nav_inter_click, own counter -> present(KIND_BOTTOM)
present: activity null/finishing -> next
  QUIZ priority -> showQuiz(shimmerFirst = !preload)
  preload && cached ad -> show cached (refill after show)
  else loadGoogle(showLoadingDialog=true, refill = preload)
showQuiz(shimmerFirst): shimmer ? dialog_loading_ads for 700 ms, then QuizAds.showInterstitial : QuizAds.showInterstitial directly; if Quiz returns false -> next
loadGoogle: initializeIfConfigured; if !canRequestAds||!network||unit empty: quiz if Google_Ad_Failed_Show_Quiz else next
  loading dialog (dialog_loading_ads, non-cancelable) + 10 s timeout -> dismiss + next (no quiz on timeout)
  load fail -> quiz if flag else next ; loaded -> showGoogle
showGoogle: paid event -> logAdRevenue; dismissed -> (refill preload) + next; show failed -> quiz if flag (+ refill) else next
```
Unit ids (`unitId(kind)`): BACK = `inter_back_ads_id` else `inter_ads_id`; BOTTOM = `bottom_nav_inter_ads_id` (no fallback); CLICK = `inter_ads_id`.
Preload (`preparePreload` from `onConfigApplied`, which also resets all counters): `shouldPreload` = not QUIZ priority, unit id non-empty, plus (BACK: `inter_ads_on_back` && back type preload && `anyScreenInterFlag(true)`; BOTTOM: show && type preload; CLICK: `inter_ads_show` && click type preload && `anyScreenInterFlag(false)`). One cached ad per kind, token-guarded, in-flight flag. After a cached ad is shown it refills; on "load" type nothing is cached (`refillAfterShow=preload`). **No 55-minute expiry on these caches** (unlike event back cache) - risk of stale ad (**unverified** whether SDK invalidates).
Analytics: `interstitial_preload`, `interstitial_load`, `interstitial_fail` with `ad_format=interstitial`, `ad_unit=<id>`.

### 4.2 AdPlacement interstitial/app-open core

`loadInterstitialAdInternal(activity, id, listener, showLoadingDialog, honorQuizPriority, quizOnFail)` (:2250):
```
token = ++interstitialLoadToken (static AtomicInteger; newer call invalidates older callbacks)
completed = AtomicBoolean
honorQuizPriority && QUIZ -> QuizAds.showInterstitial(then notifyComplete) else notifyComplete; return
!canLoad -> quizOnFail? quiz : notifyComplete
loading dialog (optional) + timeout 10_000 ms: if token still current -> token++, dismiss, quiz(if quizOnFail) else notifyComplete
InterstitialAd.load: loaded & token current -> cancel timeout, dismiss dialog, set paid listener, show
   dismissed -> notifyComplete ; show failed -> quiz (if quizOnFail) else notifyComplete
   fail -> analytics, same fallback
```
The short overload (:2246) = `honorQuizPriority=true`, `quizOnFail=getGoogleAdFailedShowQuiz()`.
Timeout only armed when a loading dialog is shown. With no dialog, there is no timeout (listener fires only via callbacks).
`loadAppOpenAdInternal` (:2333) mirrors this with `appOpenLoadToken`, `AppOpenAd.load`, `QuizAds.showAppOpen`, and extra case "token changed or activity finishing when loaded".
`notifyComplete(listener, completed)` (:2719): fires the callback **exactly once** (`compareAndSet`). Every branch of every flow ends in `notifyComplete`, so the host screen always continues. A `null` listener is fine.

### 4.3 Preloaded caches in AdPlacement

| Cache | Warm | Consume | Clear/invalidate |
|---|---|---|---|
| Right swipe (`preloadedRightSwipeAd`, token, in-flight) | `prepareRightSwipePreload` (from RCV.apply); only when `Right_Swipe_Interstitial_Ad_Show` and `Right_Swipe_Ads_type`=="preload", not QUIZ | `loadRightSwipeInterstitialAd`: counter `Right_Swipe_Interstitial` (every Nth); cached ad shown (post to decor view) else `loadRightSwipeNow` (dialog, honors quiz priority) | dismissed -> re-preload; show failed -> load now |
| Onboarding (Language/Intro) `preloadedOnboardingInterstitial` | `preloadOnboardingInterstitialAd(ctx, forIntro)` when screen opens; needs `inter_ads_click_type`=="preload", not QUIZ, canRequestAds, network | `tryShowPreloadedOnboardingInterstitial` (quiz fallback on show failure if flag); else cache cleared and `loadInterstitialAdInternal(otherInterstitialId, dialog=true)` | `clearOnboardingInterPreloadCache` (token++) |
| After default (`preloadedAfterDefaultInterstitial` / `AppOpen`) | `preloadAfterDefaultAd` when `After_Default_Ad_Show`, not QUIZ; type from `After_Default_Ad_Type` normalized -> "appopen" else inter (uses `getOtherInterstitialId()`) | `loadAfterDefaultAd`: skipped entirely under QUIZ priority; else preloaded -> show, else clear and load (no dialog) | `clearAfterDefaultPreloadCache` |
| Event back (`EventBackCache` per screen: charging, install_uninstall) | `preloadEventBackAd` when `*_ad_load_type`=="preload" and gates pass; loads the type of the NEXT sequence step (peek) | `showPreloadedEventBackAd` (must match step type, <55 min old (`EVENT_BACK_AD_MAX_AGE_MS`), not QUIZ) else cleared and loaded on demand; after showing, `preloadEventBackAd` again | token++ on `clear()`; native cache destroyed |
| Full native FCM (`ADSNativeFullDisplay.AdmobNativeAd`) | `preloadNativeAd` (from FCM service) | `ClEndActivity` takes and nulls it | replaced -> previous destroyed |

`getOtherInterstitialId()` (:576): `Other_Interstitial_Id` -> fallback `inter_ads_id` (resolved in RCH :340-352: OtherScreen key, then root key, then `inter_ads`).

Other splash helpers: after-splash type/id/show are plain getters (`After_Splash_Ad_Show/Type/Interstitial_Id`); the splash flow itself lives in `StartupFlow` (not part of this engine; **unverified** beyond the call into AP).

---------------------------------------------------------------------

## 5. Native engine

### 5.1 `loadNativeAd` (AP :1104 is the master overload)

Params: `(activity, nativeId, rlNativeAdView, slNativeShimmer, flNativeAd, type, onAdLoaded, onAdFailed, googleOnly, nativeFillColor, forceLightTheme)`. Shorter overloads default: `onAdLoaded/onAdFailed=null`, `googleOnly=false`, `nativeFillColor=onboardingNativeColor(activity)`, `forceLightTheme=false`.
```
initializeIfConfigured
fill = nativeFillColor != 0 ? it : onboardingNativeColor(activity, forceLight)
QUIZ priority && !googleOnly -> Quiz native (revealContainer=false) ; on success paint fill, onAdLoaded(null), then make rlNativeAdView VISIBLE ; else hide + onAdFailed
!canLoad -> hideNativeContainer + onAdFailed
paint container + shimmer, shimmer visible+start
AdLoader.forNativeAd:
   analytics native_load ; if activity finishing/destroyed or flNativeAd null -> nativeAd.destroy(); return
   paid listener -> logAdRevenue
   inflate resolveGoogleNativeLayout(type) with AdTheme.forAds(activity, forceLight)
   populateNativeAdView ; flNativeAd.removeAllViews(); add GestureSafeNativeAdView.wrap(adView)
   stop/hide shimmer ; show flNativeAd + rlNativeAdView ; paint fill ; onAdLoaded(nativeAd)
   (note: the NativeAd is NOT destroyed by this method; callers that keep it must destroy it)
onAdFailedToLoad: analytics native_fail ; quiz fallback (if !googleOnly && flag) with paint + onAdLoaded(null) ; else hide + onAdFailed
```
`showSlot(...)` (:941) wrapper: `showFlag` false, or (no QUIZ priority and offline) -> hides `rlAdView`; else shows `rlAdView`, picks banner vs native by `adType`, default fill `onboardingNativeColor`.

### 5.2 `populateNativeAdView` (:2473)
View ids required: `ad_media` (media sizes), `ad_app_icon`, `ad_headline`, `ad_body`, `ad_call_to_action`, optional `ad_attribution`. Views must be `AppCompatImageView` / `AppCompatTextView`, otherwise they're silently skipped. CTA: `applyCallToAction` (default text "Install", single line ellipsize END, white text, no caps) applied before and after `setNativeAd`; CTA background tinted via `applyBackgroundColor(button, getNativeAdButtonColor(adView.getContext()), R.color.primary)`; attribution tint via `Native_Ad_Label_Color` (no fallback).
Button color is theme aware: `getNativeAdButtonColor(ctx)` (:459) returns `Native_Ad_Button_Color_dark` when the **ad context** is night and dark value is non-blank, else `Native_Ad_Button_Color_light`. Hex accepts with or without `#`; parse errors fall back to `R.color.primary` (button) or no change (label; fallback 0).

### 5.3 Fill / theme
- `AdTheme.forApp(ctx)` / `forLauncher` = context forced to the **saved app night mode** (`readAppNightMode(ctx) == MODE_NIGHT_YES`), not the phone theme. `forAds(ctx, forceLight)` = dark unless `forceLight`. `withForcedNight` = `createConfigurationContext` with uiMode night bit set.
- `onboardingNativeColor(ctx, forceLight)` (:930) = `R.color.onboarding_native_bg` resolved in the ad theme (returns 0 for null ctx).
- `paintNativeFill(root, fill)` / `paintOnboardingNative` (:2631): walks the view tree; any `CardView` background or `ColorDrawable` equal to one of `onboarding_native_bg`, `ad_background`, `surface_secondary` (in both light AND dark resource variants) is recolored to `fill`. Needed because layouts hard-reference those colors; also used to match custom card fills (`big_native_bg` in `ScreenNativeAds.paintBigNativeFill`).
- Full-screen native dialog: `applyNativeFullSystemBars` (:2124) sets edge-to-edge, `surface_primary` window/nav color, transparent status bar, applies bottom inset padding to first child and top inset margin to `ivClose`, `statusBarScrim` height = status bar inset only in night. It saves and restores the host window's status/nav colors and icon appearance on dismiss, and destroys the `NativeAd` in `OnDismissListener`.

### 5.4 GestureSafeNativeAdView
`NativeAdView` is final, so `wrap(adView)` puts it in a `FrameLayout`. Intercepts a gesture when: the wrapper's bottom is within 24dp of the screen bottom AND the touch starts in the bottom gesture zone (`max(systemGestures bottom inset, 48dp)`) AND finger moves up more than 3x touch slop. Then the wrapper consumes the rest of the gesture (ad is not clicked). Plain taps still click. Always use `wrap` when adding a Google native to a view.

### 5.5 Revenue and analytics
- `logAdRevenue(ctx, AdValue)` (:1924): Firebase event `FirebaseAnalytics.Event.AD_IMPRESSION` (`ad_impression`) with `ad_platform="Google Ad Manager"`, `currency`, `value = valueMicros/1e6`. Attached through `setOnPaidEventListener` on banner, native (loaders and `requestLargeNativeAd`), inter, app open (not attached on `ProcessAppOpen`, which has no paid listener: **verified, absent**). Exceptions swallowed.
- `AnalyticsEvents.ad(ctx, event, format, unit)` -> `log` adds `platform=android`, `app_version`, `ad_format`, `ad_unit`. Events: `banner_load/fail`, `native_load/fail`, `interstitial_load/fail/preload`, `app_open_ad_load/fail`; formats `banner|native|interstitial|app_open`. `app_open` logged on process start in `ProcessAppOpen.onStart`. `ADSNativeFullDisplay` uses `AppUtils.trackScreen("FULL_NATIVE_LOAD"/"FULL_NATIVE_FAILED")`. Quiz ads log nothing.

### 5.6 Native caching helpers
`requestLargeNativeAd(activity, unitId, onLoaded, onFailed)` loads with application context (no shimmer/view). `showLargeNative(activity, container, nativeAd[, forceLight])` always inflates the **large** layout and calls `populateNativeAdView(…,"large")`; `bindLoadedNative` re-shows by type (used by the app drawer; ad must not have been destroyed).

---------------------------------------------------------------------

## 6. Per-screen config and Compose wrappers

### 6.1 `RemoteConfigValues.SCREEN_AD_KEYS` (object name -> key prefix)

| Screen object | Prefix | Keys read |
|---|---|---|
| ScanResultScreen | ScanResult | `ScanResult_Ad_Show`, `_Ad_Type`, `_Banner_Id`, `_Native_Id` |
| CreateHubScreen | CreateHub | `CreateHub_Ad_Show/_Ad_Type/_Banner_Id/_Native_Id` (bottom slot) + separate `CreateHub_top_Ad_Show` (big native) + `CreateHub_Native_Id` for Big native (`getCreateHubNativeId`; **exact source of that id unverified**) |
| QrFormScreen | QrForm | `QrForm_*` |
| QrCustomizationScreen | QrCustomization | `QrCustomization_*` |
| QrPreviewScreen | QrPreview | `QrPreview_*` |
| HistoryScreen | History | `History_*` |
| HistoryDetailScreen | HistoryDetail | `HistoryDetail_*` |
| LauncherSettingsScreen | LauncherSettings | `LauncherSettings_*` |
| OtherScreen | Other | `Other_Ad_Show`, `Other_Ad_Type` (default "banner" here, "native" for the onboarding use), `Other_Banner_Id`, `Other_Native_Id` |

Pattern: `<Prefix>_Ad_Show` (bool), `<Prefix>_Ad_Type` ("banner" default | "native"), `<Prefix>_Banner_Id`, `<Prefix>_Native_Id`; plus un-prefixed `on_back_inter_ads_show` and `on_click_inter_ads_show` in the same object. `applyScreenAds` rebuilds the map on every config apply. `MainScreen` and `SettingsFragmentScreen` are added by `rememberInterFlags` with only the inter flags (show=false), so `ScreenInterAds.onClick(..., "MainScreen")` works but `ScreenLoadAd` never shows a banner for them.
Non-map screen configs: `MainScreen` (`Main_Ad_Show`, `Main_Ad_Type`, `Main_Banner_Id`, `Main_Native_Id`, `Main_Big_Top_Ad_Show`, `Main_Ad_Auto_Second`, `Main_Bottom_Ad_Auto_Refresh`, `Main_Bottom_Ad_Auto_Second`), `SettingsFragmentScreen` (`SettingsFragment_Native_Ad_Show/_Id/_Second`).

### 6.2 ScreenLoadAd (bottom slot)
- `isEnabled(key)`: config exists and `show`; QUIZ priority -> true; else the id for the chosen type (banner/native) must be non-empty.
- `attach(activity, host, key[, nativeSize])`: clears host; hides if disabled/empty id (non-quiz); inflates `view_screen_load_ad` with `AdTheme.forApp`, banner path -> `AdPlacement.loadBannerAd`; native path -> `loadNativeAd(..., size)` where size is `"small"` or `"medium"`; for small it forces wrap-content on container/shimmer and swaps shimmer to `shimmer_small_ad`. **A new request is issued every time the screen opens** (no caching).
- `detach(host)`: destroys `AdView`s in `llBannerAd`, removes children. Native ads loaded here are not destroyed explicitly (leak risk; **unverified** whether SDK handles).

### 6.3 ScreenNativeAds (big native cards)
Slots: `HOME` (`Main_Big_Top_Ad_Show`, `Main_Native_Id`, `Main_Ad_Auto_Second`), `SETTINGS` (`SettingsFragment_Native_Ad_Show`, `_Id`, `_Second`), `CREATE_HUB` (`CreateHub_top_Ad_Show`, no refresh seconds, requests on every open).
Logic: card inflated from `view_big_native_ad` (`big_native_bg` fill). Shown only when the QR page is open (`HomeBottomAd.isQrPageOpen`). `showOrLoad`: while within refresh window show cached quiz/google ad; after the window (or never loaded) `beginLoad` -> `requestLargeNativeAd`. `refreshSeconds<=0` means remaining wait = infinite after first load (load once). `onPageVisible/onPageHidden` driven by `HomeBottomAd`. Failure: quiz if flag, else keep previous ad, else hide. Previous `NativeAd` destroyed on replacement.

### 6.4 HomeBottomAd (small ad under Home bottom bar)
`Main_Ad_Show` gate, type from `Main_Ad_Type` ("banner" -> `requestSmallBannerAd`, else `requestLargeNativeAd` rendered with `native_small_ad_layout`), refresh by `Main_Bottom_Ad_Auto_Refresh` + `Main_Bottom_Ad_Auto_Second` (timer only while page resumed; auto refresh off -> reload only on right-swipe return after N seconds). One request in flight at a time, keeps previous ad on failure. Quiz priority: shows quiz banner/small native immediately.

### 6.5 Compose wrappers (ui/components)

| Composable / fun | Behaviour |
|---|---|
| `ScreenWithAd(screenKey, nativeSize="medium", bindBackAd=false, onBackLeave, keyboardAware=false, showAd=true, content)` | Column: content (weight 1) + `FirebaseScreenAd` at bottom with `navigationBarsPadding` and an 8dp gap only when the ad has height; hides navigation bar on non-Launcher/Main activities (`HideNavigationBarOnAdScreen`); `keyboardAware` keeps the ad at screen bottom under the keyboard and shrinks content only by the overlap. `bindBackAd=true` registers an `OnBackPressedCallback` -> `ScreenInterAds.onBack(activity, key){ onLeave ?: finish() }` |
| `FirebaseScreenAd(screenKey, nativeSize)` | Returns early if `!ScreenLoadAd.isEnabled`; `AndroidView` FrameLayout; `ScreenLoadAd.attach` in `factory`, `detach` in `onRelease` |
| `bindScreenBackAd(screenKey)` (ScreenBackAd.kt) | Activity extension: back dispatcher -> `ScreenInterAds.onBack(this, key) { finish() }` |
| `Context.runWithClickAd(screenKey, action)` | Finds host Activity; `ScreenInterAds.onClick(activity, key) { action() }`; no Activity -> runs action |
| `BigNativeAd(slot)` | `ScreenNativeAds.isEnabled(slot)` gate; FrameLayout with `clipChildren=false` (card shadow); attach/detach |
| `HomeBottomAdSlot()` | `HomeBottomAd.isEnabled` gate; attach/detach; lifecycle observer -> `HomeBottomAd.onHostResume/onHostPause` |

Observed usage keys (grep): `OtherScreen` (generic screens, with `bindBackAd=true`), `QrFormScreen` (keyboardAware, nativeSize from activity), `CreateHubScreen` (small, hidden while searching), `QrCustomizationScreen`/`QrPreviewScreen` (small), `HistoryScreen`, `HistoryDetailScreen`, `ScanResultScreen`, `MainScreen`/`SettingsFragmentScreen` (click ads only), bottom-nav -> `ScreenInterAds.onBottomNav` in `ScanPulseNavHost:210`, `LauncherSettingsScreen`/`OtherScreen` back via `ScreenInterAds.onBack` in launcher activities. Typical activity: `bindScreenBackAd(KEY)` in `onCreate` OR `ScreenWithAd(bindBackAd=true)` - never both for the same screen (two callbacks would double count).

---------------------------------------------------------------------

## 7. Rules every slot must follow

1. Gate with the config `show` flag first; then, when not QUIZ priority, require a non-empty unit id for the chosen type, network available and `canRequestAds` (`canLoad`). In QUIZ priority network and ids are not required.
2. Check `shouldUseQuizPriority()` BEFORE any Google request; do not request Google ads at all in that mode (preloads included).
3. Use Google-first fallback exactly as the table in 2.4: only a real Google failure (load error, show error, timeout for inter/app-open/full-native) falls back, and only if `Google_Ad_Failed_Show_Quiz` (launcher sequences: launcher flag OR global).
4. Every flow that blocks navigation must end in exactly one `notifyComplete` / `runOnce` - success, failure, timeout, quiz fail, activity finishing. Never leave the user stuck.
5. Loading dialog + 10 s timeout for any on-demand inter/app-open/full-native (`INTERSTITIAL_LOADING_DIALOG_TIMEOUT_MS`); dismiss the dialog on every exit path; token-guard callbacks (`interstitialLoadToken`, `appOpenLoadToken`, preload tokens) so stale loads are ignored.
6. Always check `activity.isFinishing()` (and `isDestroyed` where a Dialog is shown) before touching views or showing; destroy a `NativeAd` that cannot be displayed.
7. Inflate ad layouts with the saved app theme (`AdTheme.forApp/forAds`), then recolor with `paintNativeFill`.
8. Wrap Google `NativeAdView` in `GestureSafeNativeAdView.wrap` before adding to a container.
9. Set `setOnPaidEventListener` -> `logAdRevenue` on every Google ad object you create or reuse.
10. Banners: call `prepareBannerLoading` (done inside loaders), destroy `AdView`s in `llBannerAd` on hide/detach.
11. Native shimmer: visible + `startShimmer()` before the request, `stopShimmer()` + GONE on success, failure and hide.
12. Respect the per-day interval formula (24h / N), install-day gate and country list for ClEnd/event back ads, write last-show time only when an ad is actually going to show (event ads write before showing; app-open writes in `onAdShowedFullScreenContent`).
13. Preloaded ads: one per kind, cleared when the config disables the feature or QUIZ priority is on; full-screen cached ads should be dropped after ~1 h (event cache uses 55 min).
14. Under QUIZ priority sticky hosts (drawer) must receive the quiz with `revealContainer=false` and reveal after positioning.
15. Use `ScreenInterAds` for click/back/bottom-nav counters: master flag AND per-screen flag, global (not per-screen) counters, reset on config apply.
16. For back handling in Compose either `bindScreenBackAd` or `ScreenWithAd(bindBackAd=true)`, not both.

## 8. Common bugs / pitfalls (from code comments and structure)

- **Sticky hosts covering apps:** comment at AP :1112 - Quiz would cover list items if revealed synchronously; hence `revealContainer=false`.
- **Stale "returned from external app" mark:** `clearLauncherAppReturnPending` (:1513) - a fresh launch must not show the back ad. Launcher back config is persisted (`persistLauncherBackConfig`) because the process may die while the user is in the external app.
- **Config missing keys:** `getOtherInterstitialId` falls back to `inter_ads_id`; `LauncherApp Google_Ad_Failed_Show_Quiz` falls back to root; `After_Default_*` accepts two capitalizations; `Right_Swipe_Ads_type` read case-insensitively.
- **Show-time install-day default 1:** missing install date returns 1, so ads gated at day>=2 stay off; but `*_After_Day <= 0` disables (not "immediately").
- **Google full-screen ads expire after ~1 h:** `EventBackCache` drops >55 min; `ProcessAppOpen` drops >4 h (this is longer than the Google 4 h app-open validity; right on the limit).
- **Event back ads never fall back to Quiz on show failure** (comment :1808) - intentional.
- **Launcher return counter** is separate from sequence index; reset `returnCount` to 0 only when the ad is due.
- **Resume App Open** must never show after another launcher became default or off the QR page (`shouldSkipAppOpenAd` is re-evaluated live, and again right before `show`).
- **Swipes opening ads:** `bindTapOnly` for Quiz and `GestureSafeNativeAdView` for Google exist because ViewPager swipes and the system back-gesture swipe were triggering clicks.
- **Keyboard over ad:** `ScreenWithAd(keyboardAware)` uses exact overlap (ime top vs column bottom) because the plain IME inset includes the navigation bar area and left a gap (comment in `ScreenWithAd`).
- **Nav bar gap while keyboard open:** `navigationBarsPaddingUnlessKeyboard`.
- **Naming traps:** `requestLargeNativeAd` loads any size; `requestAdaptiveBannerAd` is inline not anchored; `layout_event_shimmer_small` is the taller one; `LauncherQuizGames` is not wired into QuizAds; `dialog_inter_quiz_shimmer` is unused.
- **Quiz count cap:** `min(3, size)` - only the first 3 entries of each list are ever shown.
- **Quiz needs ALL required lists non-empty** (icon, banner/native/inter titles and descriptions) or nothing shows; media lists are optional (falls back to launcher icon).
- **No consent / premium gate in the engine** - add one in `canRequestAds` if the product needs it.
- **Possible leaks/oddities (unverified impact):** `ScreenLoadAd.detach` does not destroy native ads; `canLoad` omits `isDestroyed`; `loadNativeAd` never destroys the loaded `NativeAd` when later replaced (callers hold it only through `onAdLoaded`); `ScreenInterAds` preload caches have no age limit; `getDaysSinceInstall` parses with default locale.
- **Timeout scope:** `loadGoogle` and AP timeout only guard the loading dialog case; a no-dialog load waits for the SDK callback.
