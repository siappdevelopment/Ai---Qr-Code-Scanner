# Launcher side + QR app (Compose) screens: every ad and the default-app behaviour

Verified against the code (not against the per-feature skills). Anything not read in code is marked `unverified`.
Base package `com.qrcode.scanner`. Abbreviations: `AP` = `launcher/common/AdPlacement.java`, `RCV` = `launcher/remote/RemoteConfigValues.java`,
`RCH` = `launcher/remote/RemoteConfigHelper.java`, `LHA` = `launcher/activities/LauncherHomeActivity.java`,
`LHF` = `launcher/fragments/LauncherHomeFragment.java`, `Sheet` = `launcher/dialogs/LauncherAppsBottomSheet.java`,
`Adapter` = `launcher/adapters/LauncherAppsAdapter.java`, `DHP` = `launcher/helpers/DefaultHomePromptHelper.java`.

---------------------------------------------------------------------------------------------------

## 0. Big picture

| Piece | What it is |
|---|---|
| `LauncherHomeActivity` (HOME category, singleTask) | `ViewPager2` with 3 pages (`LauncherPagerAdapter.java:17-19`): `PAGE_RIGHT=0` = QR app (Compose `ScanPulseNavHost` inside `LauncherQrPageFragment.kt:60`), `PAGE_HOME=1` = wallpaper Home (`LauncherHomeFragment`), `PAGE_SUB=2` = left page (`SubContainerFragment`: weather, recommended apps, big native). "Right swipe" = finger swipe that reveals page 0. |
| `MainActivity` (LAUNCHER) | Same Compose `ScanPulseNavHost` standalone (used when the app is NOT the default Home and Redirect is off). No pager, no launcher ads, no Set-as-Default card. |
| Config source | One JSON per user profile (`paid_user`/`normal_user`) read from Firebase RC key `QRScanner_<versionCode><RC_KEY_SUFFIX>` (`RCH.java:68`). Applied in memory by `RCH.applyScreenConfig` (`:264-282`) and `RCV.apply` (`:149-285`). Re-fetched at every `LauncherHomeActivity.onCreate` and in `onResume` when last fetch is older than 12 h (`RCH.java:33,145`). Nothing is persisted except the Right Swipe trio, the back-ad config (see 1.5) and the ClEnd/event config (`RCV.ensureLoaded :287-326`). |
| Ad priority | `Ad_Priority == "QUIZ"` (`AP.shouldUseQuizPriority :209`) makes banners/natives/screen interstitials show Quiz ads instead of Google. It does NOT change the launcher click/return ad sequence (see 1.4). |

JSON paths: `root.<Key>` = top level of the profile object; `Screen.<Obj>.<Key>` = under `Screen` (`RCH.java:269`).
Default = value used when the key is missing.

---------------------------------------------------------------------------------------------------

## 1. Launcher side: every ad slot

### 1.1 Slot overview

| # | Slot | Where | Code entry | Trigger |
|---|---|---|---|---|
| A | Home bottom ad (small banner/native under the QR bottom bar) | QR page (pager page 0) and MainActivity | `HomeBottomAd.java`, `HomeBottomAdSlot.kt`, `ScanPulseNavHost.kt:247` | QR page visible |
| B | Drawer in-list native + floating sticky native | App drawer `LauncherAppsBottomSheet` | `Sheet:1093-1244`, `Adapter:424-444` | Drawer opened |
| C | Left-page large native | `SubContainerFragment` (PAGE_SUB) | `SubContainerFragment.java:184-322` | Page resumed |
| D | App-icon click ad (every Nth tap) | Home dock, drawer, folder dialog, left-page recommended apps | `AP.handleLauncherAppClickAd :1484` | Tap on an app icon |
| E | Return-from-external-app ad (every Nth return) | `LauncherHomeActivity.onResume` | `AP.handleLauncherAppReturnAd :1520`, `showJoinedLauncherBackAd :1542` | Back on launcher after launching an app |
| F | Right-swipe interstitial | Home -> QR page | `LHA.java:592-609`, `AP.loadRightSwipeInterstitialAd :323` | Pager settles on page 0 coming from Home |
| G | Quiz icons in the drawer | Drawer grid | `Adapter.rebuildDisplayItems :419-444` | Drawer, search empty |
| H | App Open on resume | Process level | `ProcessAppOpen.java` | App returns to foreground |
| I | Big native cards (Home tab / Settings tab / Create hub) | QR screens | `ScreenNativeAds.java` | See section 2 |

### 1.2 A. Home bottom ad (`HomeBottomAd`)

Not on the wallpaper Home page. It sits under the bottom navigation bar of the QR page (Home / Scan / Settings tabs), in the launcher QR page and in MainActivity.

Condition chain:
1. `HomeBottomAdSlot` composable returns nothing unless `HomeBottomAd.isEnabled()` (`HomeBottomAdSlot.kt:21`; `HomeBottomAd.java:38-46`): `Main_Ad_Show == true` AND (`Ad_Priority==QUIZ` OR the id for the chosen type is non-empty). Id = `Main_Banner_Id` if `Main_Ad_Type` is `banner` (case-insens.), otherwise `Main_Native_Id` (`:465-468`).
2. `attach` (`:48`): if the host activity is the launcher and the pager is not on page 0, the slot is hidden and the request waits (`isQrPageOpen :121-126`, `:62-66`). In MainActivity `isQrPageOpen` is always true.
3. `present()` (`:137`): request in flight -> shimmer; `refreshDue()` false -> keep the cached ad (or hide if none); else `beginLoad()`.
4. `beginLoad` (`:156`): Quiz priority -> `QuizAds.showBanner/showNative("small")` (no Google call). Banner -> `AP.requestSmallBannerAd` (AdSize.BANNER). Native -> `AP.requestLargeNativeAd` rendered with `native_small_ad_layout`.
5. `onFailed` (`:223`): `Google_Ad_Failed_Show_Quiz` (global `AP.getGoogleAdFailedShowQuiz`) -> Quiz; else keep the previous Google ad if there is one; else hide slot.

Refresh rules (`refreshDue :417`, `scheduleRefresh :381`, `swipeReturnRefreshDue :432`):
- First show: `lastShownAt <= 0` -> always load.
- `Main_Bottom_Ad_Auto_Refresh == true` and `Main_Bottom_Ad_Auto_Second > 0`: timer reloads every N s while the QR page is resumed; the timer stops on page hidden / host pause (`onHostPause :128`).
- `Main_Bottom_Ad_Auto_Refresh == false`: no timer. A new ad is requested only when the user leaves the QR page and swipes back (`onPageVisible :98`, called from `LHA.java:576`) AND at least `Main_Bottom_Ad_Auto_Second` s passed since the last shown ad. Seconds `<= 0` with refresh off = never reload after the first.

Keys: `Screen.MainScreen.Main_Ad_Show` (false), `Main_Ad_Type` ("banner"), `Main_Banner_Id` (""), `Main_Native_Id` (""), `Main_Bottom_Ad_Auto_Refresh` (false), `Main_Bottom_Ad_Auto_Second` (0). Parsed at `RCV.java:162-170`.

### 1.3 B. Drawer natives (in-list slot + floating sticky overlay)

One ad, two visual states. The grid gets an invisible placeholder row; the real ad lives in an overlay (`rlNativeListAdView`) that is translated to the placeholder, or floats at the bottom when the placeholder is below the fold or scrolled past.

Placeholder row (`Adapter.java:441-443`): inserted at display index 8 (`LIST_NATIVE_AFTER_ITEMS = 8`) when `listNativePlaceholderEnabled` and the display list has >= 8 items. Display items include Quiz icons, so with Quiz icons on, "8" counts them. Full row span 4 (`getSpanSize :191`). Layout `adapter_launcher_native_spacer.xml` (card + shimmer, height `launcher_native_list_ad_height`).

Eligibility (`Sheet.shouldShowNativeListAd :1093-1108`):
- Quiz priority: only `LauncherApp_Native_List_Ad_Show`.
- Otherwise ALL of: `LauncherApp_Native_List_Ad_Show`; interval OK (`AP.canShowLauncherAppNativeListAd :1910`); network; non-empty `LauncherApp_Native_List_Id`; `canRequestAds`.
- Interval: `perDay <= 0` -> no limit (always true); else `now - last_show_time >= 86_400_000 / perDay`. Stored in SharedPreferences `launcher_app_native_list_ad_preferences` / `last_show_time`, written ONLY when the ad actually became visible (`Sheet:1223`).

Flow (`maybeLoadNativeListAd :1146`, called from `setupClickListeners -> showAd` on every drawer open `:663` and after enrichment `:1090`):
1. Not eligible: if a cached Google ad exists (`cachedNativeListAd`, static, < 55 min old, not Quiz priority, Show flag true) it is re-bound without a new request and without updating `last_show_time` (`showCachedNativeListAd :1123`). Else hide container and disable the placeholder.
2. Eligible: placeholder + shimmer on, `AP.loadNativeAd(..., "medium", callback)` (`:1192`). Quiz priority is synchronous (`AP.loadNativeAd :1111`, calls back with null). Google failure + `Google_Ad_Failed_Show_Quiz` -> Quiz native (`AP.java:1172`).
3. Success: ad cached statically (`:1207-1213`), `last_show_time = now` (`:1223`), overlay revealed after being parked off-screen (`revealNativeListAdAfterBind :1247`).
4. Failure with nothing to show: container hidden, placeholder removed (`:1227-1231`, `:1239-1241`).

Position logic (`updateStickyNativeListAdPositionCore :1363-1414`, `placeNativeAdBelowVisibleApps :1417`): overlay `translationY = min(0, placeholderTop - stickyNaturalTop)`; placeholder not laid out below fold -> parked off-screen; scrolled past -> `translationY 0` (floating at bottom). When floating (`syncStickyListInset :1312`) the list gets bottom padding = overlap and a clip rect so rows never hide behind the ad (`applyStickyListInset :1333`). Touch drags on the ad scroll the list (`ScrollForwardingRelativeLayout`). A short search list (no placeholder) puts the ad under the last visible row.

Keys (`RCH.java:375-377`): `Screen.LauncherAppScreen.LauncherApp_Native_List_Ad_Show` (false), `LauncherApp_Native_List_Ad_Show_Per_Day` (0), `LauncherApp_Native_List_Id` ("").

### 1.4 D. App-icon click ad (every Nth tap)

Hook: `AP.handleLauncherAppClickAd(activity, openSelectedApp)` (`AP.java:1484-1499`).

```
if openSelectedApp == null                                  -> return
if activity null/finishing OR !LauncherApp_Click_Ad_Show OR LauncherApp_Count <= 0  -> open app
if LauncherApp_Count == launcherAppClickCount               -> clickCount = 1; run ad of nextLauncherAppAdType(false); app opens in the ad's continue callback
else                                                          clickCount++; open app
```
- `launcherAppClickCount` is a static in-memory int starting at 1 (`AP:155`): with N the ad appears on tap N, 2N, ... (N=1 -> every tap). NOT persisted, NOT reset when Remote Config is re-applied, reset by process death.
- Call sites that count: dock icons 1-4 (`LHF:292-295` via `runWithLauncherClickAd :300`), drawer app tap (`Adapter:157-173`, only when `context instanceof Activity`), folder dialog apps (`LHF:679` uses the same adapter), left-page recommended apps (same adapter). Do NOT count: Google search box (`LHF:258-263`), voice search (`:265-270`), Quiz icon taps, app-info/uninstall popup, QR shortcut buttons (Scan/Create/History/Settings use `navigateViaRightSwipeFlow`, no click ad), widgets.
- Ad-type round robin (`nextLauncherAppAdType :1960`): list `launcherAppAdSequence` filled from `LauncherApp_Ad_Type` (JSON array, or a single string) by `RCH.readAdTypeSequence :393-409`; entries are normalised by `normalizeLauncherAppAdType :2215`. Accepted names (case-insens.): `Google_Inter|inter`, `Google_App_Open|app_open|appopen`, `Google_Native|fullnative|full_native`, `Quiz_Inter`, `Quiz_App_Open`, `Quiz_Native`, `Quiz_Browser`. Unknown names are dropped. Index = in-memory, advances by 1 per ad, wraps; reset to 0 whenever the RC is applied (`setLauncherAppAdSequence :797`). Click and return have separate sequences/indices.
- Empty sequence: counter is consumed, `adType==""` falls to `continueAction.run()` -> app opens, no ad (`:2023`).
- Execution (`executeSequencedAd :1988-2024`) with `quizOnFail = launcherGoogleAdFailedShowQuiz || globalGoogleAdFailedShowQuiz` (`:1976`):
  - `google_inter`: `loadInterstitialAdInternal(id=LauncherApp_Interstitial_Id, loadingDialog=true, honorQuizPriority=false, quizOnFail)` (`:2250`). Loading dialog, 10 s timeout (`:86`). Failure/timeout/show-failure -> Quiz interstitial only if `quizOnFail`; else the app opens.
  - `google_app_open`: `loadAppOpenAdInternal(getAppOpenId(), ..., honorQuizPriority=false, quizOnFail, dialog=true)` (`:2333`). Uses `App_Open_Id`. It does NOT check `App_Open_Ad_Show` / `App_Open_Show_Per_Day`.
  - `google_native`: full-screen native dialog (`showNativeFullAd :2026`) with unit id `LauncherApp_Native_Id` (the same key as the left-page native, section 1.6). 10 s timeout; failure -> `QuizAds.showNativeFull` if `quizOnFail`.
  - `quiz_inter / quiz_app_open / quiz_native`: Quiz full-screen; if the Quiz can't show (`QuizAds.show*` returns false, e.g. no synced items) the app opens.
  - `quiz_browser`: `QuizAds.openBrowserThenContinue` opens `RemoteConfigValues.pickQuizLink()` in the browser and continues when this activity resumes (`QuizAds.java:315-369`); empty link -> continue at once.
- Because `honorQuizPriority=false`, `Ad_Priority=QUIZ` does not turn Google types into Quiz here. A Google type still goes to Google first.
- Network is not checked separately: `canLoad` (`:1956`) = activity alive + ads SDK started + network + non-empty id; otherwise the Quiz fallback or the plain app open.

Keys (`RCH.java:378-381`): `Screen.LauncherAppScreen.LauncherApp_Click_Ad_Show` (false), `LauncherApp_Count` (0), `LauncherApp_Ad_Type` ([]), `LauncherApp_Interstitial_Id` (""), `LauncherApp_Native_Id` (""), `Google_Ad_Failed_Show_Quiz` (see 1.7).

### 1.5 E. Return-from-external-app ad (every Nth return)

Detection/persistence (`SharedPreferences "launcher_app_back_ad"`, `AP:88`):
1. An external app was started: `markLauncherExternalAppLaunched` (`AP:1501`) is called after a successful `startActivity` in `Adapter.launchApp :473` and `LHF.startResolvedActivity :1180` (dock + voice search). It saves `waitingForLauncherAppReturn=true` and `lastLaunchedPackage` (package, or the intent action if no package). The Google search box and Quiz icons do not set it.
2. `LHA.onResume` (`:199`), after the default-setup short-circuit (`:195`), calls `handleLauncherAppReturnAd` (`AP:1520`): not pending -> nothing; clears the flag; empty `lastLaunchedPackage` -> nothing; `restoreLauncherBackConfig`; `LauncherApp_Back_Click_Ad_Show` false or `LauncherApp_Back_Count <= 0` -> nothing; else `showJoinedLauncherBackAd`.
3. Fresh launcher creation (`savedInstanceState == null`, `LHA:143-146`) calls `clearLauncherAppReturnPending`, so a mark from before the app was removed from Recents never shows an ad. System re-creation with saved state keeps the mark.
4. `showJoinedLauncherBackAd` (`:1542-1571`): `returnCount = prefs.launcherAppReturnCount + 1` (persisted); `>= LauncherApp_Back_Count` -> reset to 0 and show; else save and finish. Type from `nextLauncherAppAdType(true)` (separate index); empty -> nothing. Executed with `LauncherApp_Back_Interstitial_Id` through the same `executeSequencedAd` as 1.4.
5. Back config survives process death: `RCH.applyScreenConfig` calls `AP.persistLauncherBackConfig` (`RCH:280`, `AP:871`) writing `backConfigSaved`, `backClickAdShow`, `backCount`, `backSequence` (joined with `|`), `backInterstitialId`, `backQuizOnFail`; `restoreLauncherBackConfig :893` reloads them only when the in-memory sequence is empty (cold start before RC returns). The sequence index and the click-ad counter are not persisted.

Keys (`RCH.java:382-385`): `LauncherApp_Back_Click_Ad_Show` (false), `LauncherApp_Back_Count` (0), `LauncherApp_Back_Ad_Type` ([]), `LauncherApp_Back_Interstitial_Id` ("").

### 1.6 C. Left-page large native (`SubContainerFragment`)

- `showAd()` runs in `onResume` (`:549`). Eligible (`:213-221`): `LauncherApp_Native_Ad_Show` AND `canShowLauncherAppNativeAd` (`AP:249`: `perDay <= 0` -> always, else `86_400_000/perDay` since `launcher_app_native_ad_preferences/last_show_time`) AND (network OR Quiz priority).
- Already showing an ad and eligible again -> reloads a new one (`:194-203`). Not eligible -> container hidden.
- Load: `AP.loadNativeAd(LauncherApp_Native_Id, "large")` (`:290`); Quiz priority/Google-fail-Quiz handled inside `AP.loadNativeAd`. `last_show_time` written when content is displayed (`:304-307`).
- Note: `LauncherApp_Native_Id` is shared with the full-screen `Google_Native` step of the click/return sequences (`AP:1981`), but the `LauncherApp_Native_Ad_Show` flag is NOT checked by those sequences.
- Keys (`RCV.java:200-202`): `Screen.LauncherAppScreen.LauncherApp_Native_Ad_Show` (false), `LauncherApp_Native_Ad_Show_Per_Day` (0), `LauncherApp_Native_Id` ("").

### 1.7 Quiz fallback and Ad_Priority (launcher scope)

- Global `Ad_Priority` (root) and `Google_Ad_Failed_Show_Quiz` (root) set in `RCH.applyAdFields :306-308`; then `Screen.QuizAdsDesign.Ad_Priority/Google_Ad_Failed_Show_Quiz` override them if present (`RCV.java:239-246`).
- Launcher click/return ads use `quizOnFail = LauncherAppScreen.Google_Ad_Failed_Show_Quiz (or root value when absent, RCH:387-390) || global value`. Either true = fall back to Quiz on Google load fail / timeout / show fail.
- Slots that follow `Ad_Priority`: A, B, C, I (banner/native), screen interstitials (section 2). Slots that do not: D/E sequence selection (they follow the sequence only), H (Google-only).
- Quiz data comes from `Screen.QuizAdsDesign.*` lists (`RCV.java:247-261`) and links from `AppProxyStructure.Quiz_Link_List` (`:271`). `QuizAds.showInterstitial` returns false when `getQuizSyncedItemCount() <= 0`.

### 1.8 F. Right-swipe interstitial (and the Default Home popup)

Trigger chain (`LHA.java:551-612`):
1. `onPageSelected`: Home -> page 0 sets `pendingRightSwipeOpen = true` and `notifyRightSwipeTutorialCompleted()` (`:578-581`). Also hits when the Scan tile/rail on Home calls `openQrShell` (`LHF:256,286` -> `LHA.openQrShell :354`) and when `ComposeScanRequest.isPending()` at `onResume` (`:210-212`). Create / History / Settings tiles start activities directly and do not trigger it.
2. `onPageScrollStateChanged(IDLE)` with current item = page 0 (`:587-609`):
   - `popupFragment.canShowDefaultHomePopup()` true -> `maybeShowDefaultHomePopup()` and `return`: the ad is skipped, its counter is not advanced.
   - else `AP.loadRightSwipeInterstitialAd(activity, callback)`; callback (activity not finishing) -> `homeFragment.maybeShowDefaultHomePopup()`.
3. `loadRightSwipeInterstitialAd` (`AP:323-345`): `Right_Swipe_Interstitial_Ad_Show` false -> callback at once. `threshold = Right_Swipe_Interstitial`; `> 0`: static counter `rightSwipeInterstitialCount` (starts 1): `== threshold` -> reset to 1 and show; else `++` and skip (ad on swipe N, 2N...). `0` = every swipe. Preloaded ad ready and not Quiz priority -> show it (`showReadyRightSwipeAd :401`); else `loadRightSwipeNow` -> `loadInterstitialAdInternal(RCV.getInterAdsId(), loading dialog, honorQuizPriority=true, quizOnFail=global flag)`.
4. Preload (`Right_Swipe_Ads_type == "preload"`, case-insens., needs Show=true and not Quiz priority): `prepareRightSwipePreload` is called on RC apply (`RCV:281,325`) and each `LHA.onResume` (`:193`); the next ad is refilled after dismiss.

Keys: `Screen.MainScreen.Right_Swipe_Interstitial_Ad_Show` (false), `Right_Swipe_Interstitial` (0), `Right_Swipe_Ads_type` ("load", name matched ignoring case, `RCV.java:175-177`); unit id = `root.inter_ads.inter_ads_id` (`RCV.applyInterAds :927-937`).

### 1.9 G. Quiz icons in the drawer

- Injected only by the drawer adapter (`new LauncherAppsAdapter(..., true)` `Sheet:772`), only while the search box is empty (`Sheet:878 setQuizIconsVisible`), and only if `LauncherApp_Quiz_Icon_Show` (`Adapter:424`).
- `LauncherApp_Quiz_Icon_Count` = number of games, capped by 40 (`LauncherQuizGames.size()`, `MAX_QUIZ_ITEMS`). Pattern: app, quiz, app, quiz, ... for the first N apps (`:433-439`), so the Quiz icons come right after each of the first N apps. Each item shows an "Ad" label (`tvAdLabel`) and the game title/icon from `LauncherQuizGames` (40 `gamqora.com` links, `LauncherQuizGames.java:10-50`).
- Tap (`Adapter:134-144`, `openQuizGame :452`): `AP.openQuizGameUrl -> QuizAds.openLink` (ACTION_VIEW, NEW_TASK). No click counter, no external-launch mark (so no return ad), independent of `Ad_Priority`/Google fail flags. Long press does nothing.
- Keys (`RCV.java:203-204`): `Screen.LauncherAppScreen.LauncherApp_Quiz_Icon_Show` (false), `LauncherApp_Quiz_Icon_Count` (0).

### 1.10 H. App Open on resume (`ProcessAppOpen`, registered in `QrScannerApplication.java:31`)

- Fires from `ProcessLifecycleOwner.onStart` (`:163`) and preloads in `onStop` (`:182-186`).
- `shouldSkipAppOpenAd :93` skips (and clears the loaded ad) when the app is NOT the default Home app (read live), or when the top activity is not "right side": launcher on page 0, or MainActivity / `com.qrcode.scanner.ui.*` (those two branches are effectively dead because of the default-Home check).
- Needs `App_Open_Ad_Show`, `App_Open_Id`, `canShowAppOpenAd` (`AP:289`: `App_Open_Show_Per_Day <= 0` -> always; else `86_400_000/perDay` since `app_open_ad_preferences/last_show_time`, written in `onAdShowedFullScreenContent :150`). Ad expires after 4 h (`AD_EXPIRY_MS`). `App_Open_Dialog_Show` shows a 1.5 s "loading ad" dialog first (`:174,188-207`).
- Google only; `Ad_Priority=QUIZ` is not consulted (unverified for anything outside this file).
- Keys (root): `App_Open_Ad_Show` (false), `App_Open_Dialog_Show` (false), `App_Open_Show_Per_Day` (0), `App_Open_Id` ("").

### 1.11 Other launcher-area screens

| Screen | Ad | Code |
|---|---|---|
| `LauncherSettingsActivity` | Banner/native (medium) from `LauncherSettingsScreen`; back interstitial via `ScreenInterAds.onBack("LauncherSettingsScreen")` | `LauncherSettingsActivity.java:111-118,136-144` |
| `AppWidgetsActivity` | `OtherScreen` banner/medium native; back interstitial `OtherScreen` | `AppWidgetsActivity.java:73-107` |
| Wallpaper Home page (`LauncherHomeFragment`) | No ad of its own; Home shows only the Set-as-Default card (section 3) | `LHF` |

---------------------------------------------------------------------------------------------------

## 2. QR app (Compose) screens: screen keys, ads, interstitials

### 2.1 Engines

| Engine | File | Used for |
|---|---|---|
| `ScreenWithAd` + `FirebaseScreenAd` -> `ScreenLoadAd` | `ui/components/ScreenWithAd.kt:44-137`, `launcher/common/ScreenLoadAd.java` | Bottom banner/native of a screen. A NEW request on every open of the screen (no auto-refresh, no interval). |
| `BigNativeAd` -> `ScreenNativeAds` | `ui/components/BigNativeAd.kt`, `launcher/common/ScreenNativeAds.java` | Big top native card (Home tab, Settings tab, Create hub). |
| `HomeBottomAdSlot` -> `HomeBottomAd` | section 1.2 | Small ad under the bottom bar of the 3 root tabs. |
| `bindScreenBackAd` / `ScreenWithAd(bindBackAd=true)` | `ui/components/ScreenBackAd.kt`, `ScreenWithAd.kt:139-161` | Back-press interstitial. |
| `runWithClickAd` | `ScreenBackAd.kt:20-27` | Click interstitial before an action. |
| `ScreenInterAds.onBottomNav` | `ScanPulseNavHost.kt:204-211`, `ScreenInterAds.java:97-113` | Bottom-nav tab interstitial. |

Native size: `FirebaseScreenAd` default `medium`; `small` where noted. `ScreenLoadAd.isEnabled(screenKey)` (`:25-36`): `<Prefix>_Ad_Show` true AND (Quiz priority OR id for the type non-empty). `Type` default "banner"; banner -> `AP.loadBannerAd`, otherwise `AP.loadNativeAd`.

### 2.2 Per-screen key naming (`RCV.SCREEN_AD_KEYS :877-887`, `applyScreenAds :899-914`)

For every screen object `Screen.<Obj>` the keys are `<Prefix>_Ad_Show` (false), `<Prefix>_Ad_Type` ("banner"), `<Prefix>_Banner_Id` (""), `<Prefix>_Native_Id` (""), plus the interstitial flags `on_back_inter_ads_show` (false) and `on_click_inter_ads_show` (false) on the same object.

| Object (screen key used in code) | Prefix | Used by |
|---|---|---|
| `MainScreen` | `Main` (+ special keys below) | Home tab, Scan tab, bottom ad slot; only the inter flags are stored by `rememberInterFlags` (`RCV:181`) |
| `SettingsFragmentScreen` | `SettingsFragment` | Settings tab (inter flags only via `rememberInterFlags :187`) |
| `ScanResultScreen` | `ScanResult` | `ScanResultActivity` |
| `CreateHubScreen` | `CreateHub` | `CreateActivity` |
| `QrFormScreen` | `QrForm` | `CommonQrFormActivity`, `WifiQrActivity` |
| `QrCustomizationScreen` | `QrCustomization` | `QrCustomizationActivity` |
| `QrPreviewScreen` | `QrPreview` | `QrPreviewActivity` |
| `HistoryScreen` | `History` | `HistoryActivity` |
| `HistoryDetailScreen` | `HistoryDetail` | `HistoryDetailActivity` |
| `LauncherSettingsScreen` | `LauncherSettings` | `LauncherSettingsActivity` |
| `OtherScreen` | `Other` | Favorites, ContinuousBatchResult, DetectionError, GalleryCrop, About, AppWidgets |

Note the key is the object name (with `Screen`) in code (`getScreenAd("QrFormScreen")`), and the short prefix inside JSON (`QrForm_Ad_Show`).

### 2.3 Screen table

| Screen (file) | Bottom ad | Big native | Back inter | Click inter (`runWithClickAd`) |
|---|---|---|---|---|
| Home tab (`HomeScreen.kt`, route `home`) | `HomeBottomAdSlot` (Main_*) | `BigNativeAd(HOME)` `HomeScreen.kt:120` | none | `MainScreen`: Create QR, History, Favorites, View all (`ScanPulseNavHost.kt:331-349`); recent item and Scan buttons: none |
| Scan tab (`ScanScreen`) | `HomeBottomAdSlot` (bar visible) | none | none | none |
| Settings tab (`SettingsScreen.kt:121`) | `HomeBottomAdSlot` | `BigNativeAd(SETTINGS)` | none | `SettingsFragmentScreen`: Language row (`ScanPulseNavHost.kt:373-379`) |
| About (nav route, `ScanPulseNavHost.kt:396-405`) | `ScreenWithAd("OtherScreen")` medium | none | `OtherScreen` (`bindBackAd`, leave = popBackStack) | none |
| Scan result (`ScanResultActivity.kt:112,126`) | `ScanResultScreen` medium | none | `ScanResultScreen` | none |
| Create hub (`CreateActivity.kt:25-37`) | `CreateHubScreen` **small**, hidden while searching (`showAd = !searching`) | `BigNativeAd(CREATE_HUB)` hidden while searching (`CreateHubScreen.kt:163-166`) | `CreateHubScreen` | `CreateHubScreen` on category tap |
| QR form (`CommonQrFormActivity.kt:98-109`) | `QrFormScreen`, native size medium if the form has <= 2 edit rows else small, `keyboardAware` | none | `QrFormScreen` | `QrFormScreen` on "Generate" before opening preview (`:181`) |
| Wi-Fi form (`WifiQrActivity.kt:90-93,145`) | `QrFormScreen` small, keyboardAware | none | `QrFormScreen` | `QrFormScreen` before preview |
| QR customization (`QrCustomizationActivity.kt:95-118`) | `QrCustomizationScreen` small | none | `QrCustomizationScreen` | `QrCustomizationScreen` on Apply |
| QR preview (`QrPreviewActivity.kt:104,126,419,464`) | `QrPreviewScreen` small | none | `QrPreviewScreen` | `QrPreviewScreen` on Customize and on Save & Create |
| History (`HistoryActivity.kt:23-30`) | `HistoryScreen` medium (nav-bar padding added only when the ad is disabled) | none | `HistoryScreen` | none |
| History detail (`HistoryDetailActivity.kt:53-57`) | `HistoryDetailScreen` medium | none | `HistoryDetailScreen` | none |
| Favorites (`FavoritesActivity.kt:68`) | `OtherScreen` medium | none | `OtherScreen` | none |
| Continuous batch result (`ContinuousBatchResultActivity.kt:77`) | `OtherScreen` | none | `OtherScreen` | none |
| Detection error (`DetectionErrorActivity.kt:75`) | `OtherScreen` | none | `OtherScreen` | none |
| Gallery crop (`GalleryCropActivity.kt:108`) | `OtherScreen` small | none | `OtherScreen` | none |
| Launcher settings (`LauncherSettingsActivity`) | `LauncherSettingsScreen` medium via `AP.showSlot` | none | `LauncherSettingsScreen` | none |
| App widgets (`AppWidgetsActivity`) | `OtherScreen` medium | none | `OtherScreen` | none |
| Scanner viewfinder / `ScannerActivity` | none found | none | none | none |

Screens using `ScreenWithAd` without a back flag still get the back ad through `bindScreenBackAd` in `onCreate` (ScanResult, Create, Form, Customization, Preview, History, HistoryDetail). `ScreenWithAd` also hides the nav bar in non-launcher/non-Main activities (`HideNavigationBarOnAdScreen :163-182`).

### 2.4 Big native cards (`ScreenNativeAds`)

- `isEnabled(slot)` (`:38-51`): HOME = `Main_Big_Top_Ad_Show` + id `Main_Native_Id`; SETTINGS = `SettingsFragment_Native_Ad_Show` + `SettingsFragment_Native_Id`; CREATE_HUB = `CreateHubScreen.CreateHub_top_Ad_Show` (or `CreateHub_Top_Ad_Show`) + id `CreateHub_Native_Id` (the same id key as the bottom ad of that screen, `RCV.getCreateHubNativeId :378`). Quiz priority only needs the show flag.
- Hidden when the pager is not on page 0 (`attach :70`), re-shown on `onPageVisible`/hidden on `onPageHidden` (`:77-107`, called from `HomeBottomAd.onPageVisible/Hidden`).
- Refresh (`showOrLoad :109`, `remainingWait :308`, `refreshSeconds :329`): HOME seconds = `Main_Ad_Auto_Second`; SETTINGS seconds = `SettingsFragment_Native_Second`; seconds `<= 0` = load once and keep the cached ad for the whole session (re-request only if no ad was obtained). When the wait is over, a new request starts the next time the screen is opened/visible. CREATE_HUB has no seconds: each time the hub attaches (`lastCycleAt = 0`, `:66-69`) it requests a fresh ad.
- Failure: Quiz if `Google_Ad_Failed_Show_Quiz`, else keep the cached ad, else hide (`onFailed :193`).
- Layout `view_big_native_ad`, large native; fill colour `big_native_bg`.

### 2.5 Screen interstitials (`ScreenInterAds`)

Config (root, `RCV.applyInterAds :927-937`): `inter_ads.inter_ads_show` (false), `inter_ads_click` (0 = off), `inter_ads_click_type` ("load"), `inter_ads_on_back` (false), `inter_ads_back_click` (0 = off), `inter_ads_back_click_type` ("load"), `inter_ads_id` (""), `inter_back_ads_id` ("" -> falls back to `inter_ads_id`, `ScreenInterAds.unitId :256-270`).
Bottom nav (`Screen.MainScreen`, `RCV:171-174`): `bottom_nav_inter_ads_show` (false), `bottom_nav_inter_ads_type` ("load"), `bottom_nav_inter_ads_id` (""), `bottom_nav_inter_click` (0).

Logic (`handle :73-95`):
1. `countsToward(screenKey, back)` (`:123`): master flag (`inter_ads_on_back` for back, `inter_ads_show` for click) AND the screen's own flag (`on_back_inter_ads_show` / `on_click_inter_ads_show`). Otherwise the action just runs and nothing is counted.
2. `every <= 0` -> run. Global in-memory counters `backCount` / `clickCount` are shared by ALL screens; `++count < every` -> run; else reset to 0 and present. So the Nth qualifying action shows the ad, regardless of screen.
3. `present` (`:132`): Quiz priority -> Quiz interstitial (with a 700 ms shimmer dialog when type = "load"). Type `preload` and a cached ad exists -> show it, then refill. Else `loadGoogle` with a loading dialog and 10 s timeout (`:174-225`); load fail -> Quiz if `Google_Ad_Failed_Show_Quiz`, else just continue. Missing id / no network -> Quiz or continue.
4. Bottom nav (`onBottomNav :97`): `bottom_nav_inter_ads_show` and `bottom_nav_inter_click > 0`; counter `bottomCount`; independent of the per-screen flags. Called when a different bottom-bar tab is tapped (`ScanPulseNavHost.kt:233-241`); a tap on the tab already open does nothing and is not counted.
5. Counters reset and preloads are re-evaluated on every RC apply (`onConfigApplied :56-63`, from `RCV:280`). Preload only when master on, type = "preload", id non-empty, not Quiz priority and (for back/click) at least one screen has the flag (`shouldPreload :272-288`).

---------------------------------------------------------------------------------------------------

## 3. "App is NOT the default Home app": what the user sees

### 3.1 Which UI the user lands in

| Situation | Result | Code |
|---|---|---|
| Not default, `Redirect_Home_Launcher=false` (default) | Splash -> `MainActivity` Compose app only (`staysInQrApp`). No pager, no launcher ads, no Set-as-Default card, no Right Swipe, no App Open on resume (skipped, not default). Only section 2 ads. | `StartupNavigation.java:35-39`; `ScreenFlowNavigation.buildFinalDestinationIntent :339-352` |
| Not default, `Redirect_Home_Launcher=true` | Final destination is `LauncherHomeActivity` via an explicit HOME-category intent (NEW_TASK + CLEAR_TASK + EXCLUDE_FROM_RECENTS). Full launcher UI with the Set-as-Default card on the wallpaper Home and in the drawer, Right Swipe popup and all launcher ads. | `AppUtils.buildLauncherHomeIntent :312-319`; `ScreenFlowNavigation.java:344-349` |
| Default Home | Launcher opens directly. Card hidden. | `AppUtils.isDefaultHomeApp :345` (RoleManager `ROLE_HOME`, fallback `resolveActivity`) |

`Redirect_Home_Launcher` is a root key (`RCH.java:268`), default false, kept only in memory in `ScreenFlowConfig` (`:35,49-55`), so before the first RC fetch of a process it is false (unverified: whether Splash always waits for the fetch before reading it). `ComposeScanRequest.openScan` also honours it (`:39`): host = launcher when default or Redirect, else MainActivity.

### 3.2 Home-page "Set as Default" card (`llDefault`)

- Layout: `fragment_launcher_home.xml:310-...` (title `launcher_default_title` "Make Us Your Default", subtitle `launcher_default_subtitle`, swinging arrow `ivDefaultArrow` animated in `LHF.startDefaultArrowSwing :1397`). Drawer copy: `bottom_sheet_launcher_apps.xml:52` with a "Set Now" button (`btnSetNow`, click `Sheet:668`).
- Visibility is not Remote-Config controlled: `DHP.updateVisibility :116-121` -> `VISIBLE` whenever `!isDefaultHomeApp`, GONE when default. Called on `bind`, `onResume` (`LHF:1359`, `Sheet:1884`) and after role results.
- Tap (`DHP.handleSetAsDefaultClick :167-194`): ignored while a request is in flight; already default -> `completeSuccess`; else `roleLauncher.launch(createRequestRoleIntent(ROLE_HOME))` (`AppUtils.createDefaultHomeRoleRequestIntent :358`; null below API 29 / role held / unavailable -> falls back to `openDefaultHomeChooser`, a plain HOME intent, then polling every 500 ms until default).
- Suppressed dialog (user declined before): if the role result returns in < 800 ms after a click (`SUPPRESSED_ROLE_RESULT_MS`, `:34,230-236`), `openHomeSettingsFallback :243-264` opens `Settings.ACTION_HOME_SETTINGS` (or MANAGE_DEFAULT_APPS) and the guide card `DefaultHomeGuideActivity` (layout `activity_overlay_permission_default_home.xml`, `DefaultHomeGuideActivity.java:33`). `res/layout/view_default_home_guide.xml` is not referenced from any Java/Kotlin/XML (unused) - unverified for dynamic lookups by name. Back on resume without being default ends the wait (`onResume :196-201` -> `completeCancelled`).
- Result handling: success -> `AppUtils.setDefaultHomeScreenCompleted(true)`, `DEFAULT_HOME_APP_SET` tracked once (`TrackOnce`), card hidden; cancel -> `DEFAULT_HOME_APP_CANCEL` (only if a selection had started), card stays. If Android restarts the launcher after Allow, `LHA.onResume` calls `DHP.reportDefaultSetIfRequested` (`:192`), which sends `DEFAULT_HOME_APP_SET` once from the persisted `default_home_role_request/pending` mark (`DHP:74-92`).
- No Remote Config key limits the card; it never shows in MainActivity.

### 3.3 Right-swipe Default Home popup (`Default_App_Popup_*`)

The "popup" is the system RoleManager dialog, launched from the launcher after a right swipe (no custom view).

`DHP.canShowDefaultHomePopup :127-145` returns true only when ALL hold: no popup/request in progress; fragment active and host activity alive; `Default_App_Popup_Show == true` (`AP.getDefaultAppPopupShow`); not already the default Home; `AP.canShowDefaultAppPopup` (below); a role request intent can be built (API 29+, role available and not held).

`canShowDefaultAppPopup` (`AP:271-279`): `Default_App_Popup_Count <= 0` -> TRUE (no limit: every qualifying right swipe); else allowed when `now - default_app_popup_preferences/last_show_time >= 86_400_000 / Count` (so Count=4 means at most one popup every 6 h, spaced, not 4 in a burst).

`maybeShowDefaultHomePopup :147-165`: writes `last_show_time = now` BEFORE launching (a declined/cancelled popup still consumes the slot), sets in-progress flags, marks the role request pending, launches the role intent. Result -> `onRoleResult :218-240` -> success/cancel handlers as in 3.2 (the suppressed-dialog settings fallback only applies to the click path, `roleRequestFromClick`).

Interaction with the Right Swipe ad (`LHA.java:592-609`): popup due -> show popup and skip the ad entirely (ad counter untouched). Popup not due -> ad runs (if due) and the popup is re-checked in the ad's completion callback. This ensures the interstitial and the permission dialog never stack. Popup keys are root level: `Default_App_Popup_Show` (false), `Default_App_Popup_Count` (0) (`RCV.java:158-159`).

MainActivity: `DefaultHomePopupHost` (registered at `MainActivity.kt:22,26`) only shows the popup when the intent carries `EXTRA_RIGHT_SWIPE_QR_VISIBLE`, set by `markRightSwipeQrVisible`; nothing in the project calls that method, so the standalone app never shows the popup (it only runs `reportDefaultSetIfRequested`, `DefaultHomePopupHost.java:28-42`).

---------------------------------------------------------------------------------------------------

## 4. Remote Config key index (launcher + QR screens)

| Path | Default | Used by |
|---|---|---|
| `Redirect_Home_Launcher` | false | 3.1 |
| `Default_App_Popup_Show` / `Default_App_Popup_Count` | false / 0 | 3.3 |
| `App_Open_Ad_Show`, `App_Open_Dialog_Show`, `App_Open_Show_Per_Day`, `App_Open_Id` | false, false, 0, "" | 1.10 (and the `Google_App_Open` step) |
| `Ad_Priority`, `Google_Ad_Failed_Show_Quiz` (also under `Screen.QuizAdsDesign`, overrides) | "", false | 1.7 |
| `inter_ads.inter_ads_show / inter_ads_click / inter_ads_click_type / inter_ads_on_back / inter_ads_back_click / inter_ads_back_click_type / inter_ads_id / inter_back_ads_id` | false/0/"load"/false/0/"load"/""/"" | 2.5, right swipe unit id |
| `Screen.MainScreen.Main_Ad_Show / Main_Ad_Type / Main_Banner_Id / Main_Native_Id` | false/"banner"/""/"" | 1.2 |
| `Screen.MainScreen.Main_Bottom_Ad_Auto_Refresh / Main_Bottom_Ad_Auto_Second` | false / 0 | 1.2 |
| `Screen.MainScreen.Main_Big_Top_Ad_Show / Main_Ad_Auto_Second` | false / 0 | 2.4 (Home big native) |
| `Screen.MainScreen.bottom_nav_inter_ads_show / _type / _id / bottom_nav_inter_click` | false/"load"/""/0 | 2.5 |
| `Screen.MainScreen.Right_Swipe_Interstitial_Ad_Show / Right_Swipe_Interstitial / Right_Swipe_Ads_type` | false / 0 / "load" | 1.8 |
| `Screen.MainScreen.on_back_inter_ads_show / on_click_inter_ads_show` | false / false | 2.5 |
| `Screen.SettingsFragmentScreen.SettingsFragment_Native_Ad_Show / _Native_Id / _Native_Second` (+ inter flags) | false / "" / 0 | 2.4 |
| `Screen.CreateHubScreen.CreateHub_top_Ad_Show` (+ `CreateHub_Ad_*`, inter flags) | false | 2.4 |
| `Screen.<ScanResult|CreateHub|QrForm|QrCustomization|QrPreview|History|HistoryDetail|LauncherSettings|Other>Screen.<Prefix>_Ad_Show / _Ad_Type / _Banner_Id / _Native_Id` + `on_back_inter_ads_show`, `on_click_inter_ads_show` | false/"banner"/""/""/false/false | 2.2 |
| `Screen.LauncherAppScreen.LauncherApp_Native_List_Ad_Show / _Show_Per_Day / _Native_List_Id` | false / 0 / "" | 1.3 |
| `Screen.LauncherAppScreen.LauncherApp_Native_Ad_Show / _Show_Per_Day / LauncherApp_Native_Id` | false / 0 / "" | 1.6, full native step |
| `Screen.LauncherAppScreen.LauncherApp_Click_Ad_Show / LauncherApp_Count / LauncherApp_Ad_Type / LauncherApp_Interstitial_Id` | false / 0 / [] / "" | 1.4 |
| `Screen.LauncherAppScreen.LauncherApp_Back_Click_Ad_Show / LauncherApp_Back_Count / LauncherApp_Back_Ad_Type / LauncherApp_Back_Interstitial_Id` | false / 0 / [] / "" | 1.5 |
| `Screen.LauncherAppScreen.LauncherApp_Quiz_Icon_Show / LauncherApp_Quiz_Icon_Count` | false / 0 | 1.9 |
| `Screen.LauncherAppScreen.Google_Ad_Failed_Show_Quiz` | root value | 1.7 |
| `Screen.QuizAdsDesign.*`, `AppProxyStructure.*` | lists empty | Quiz content/links |

`<n> <= 0` semantics differ by key: per-day keys (`*_Per_Day`, `Default_App_Popup_Count`) mean "no limit", while count keys (`LauncherApp_Count`, `LauncherApp_Back_Count`, `inter_ads_click`, `inter_ads_back_click`, `bottom_nav_inter_click`) mean "off". `Right_Swipe_Interstitial = 0` means "every swipe".

---------------------------------------------------------------------------------------------------

## 5. Test matrix

| # | Setup (RC) | Action | Expected |
|---|---|---|---|
| 1 | `LauncherApp_Click_Ad_Show=true`, `LauncherApp_Count=3`, `Ad_Type=["Google_Inter"]` | Tap 3 different drawer apps in a fresh process | Taps 1-2 open the app; tap 3 shows loading dialog then inter, app opens after dismiss |
| 2 | Same, `Ad_Type=["Google_Inter","Quiz_Browser","Google_Native"]`, Count=1 | Tap 4 apps | Types in order Inter, Browser (opens link, continues when launcher resumes), Native, Inter |
| 3 | Count=2, dock icons + drawer + folder app | Alternate sources | One shared counter |
| 4 | Count=0 or Show=false | Tap apps | Never an ad |
| 5 | `Google_Inter`, bad interstitial id, quiz-on-fail true | Tap Nth | Quiz interstitial (or app opens if Quiz unavailable); false -> app opens |
| 6 | `Ad_Priority=QUIZ`, `Ad_Type=["Google_Inter"]` | Tap Nth | Still Google first (priority does not switch the sequence) |
| 7 | `LauncherApp_Back_Click_Ad_Show=true`, `Back_Count=2` | Open app, press Home/back to launcher, twice | Ad after the 2nd return; count persisted across process death |
| 8 | Same | Launch app, swipe launcher away from Recents, reopen | No return ad (fresh create clears mark) |
| 9 | Same | Search box / Quiz icon -> return | No return ad |
| 10 | `LauncherApp_Native_List_Ad_Show=true`, per-day 24 (1 h) | Open drawer twice < 1 h apart | 2nd open re-shows the cached ad, no new request; after 1 h a new request |
| 11 | Per-day 0 | Open drawer repeatedly | New request each open |
| 12 | Native list ad, scroll list | Scroll so slot is below / above fold | Ad floats at bottom, rows never hidden; in slot it follows the placeholder |
| 13 | Search text typed in drawer | Type query | Quiz icons hidden; short list puts ad below apps |
| 14 | `LauncherApp_Quiz_Icon_Show=true`, Count=5 | Open drawer | After each of the first 5 apps one Quiz icon with "Ad" label; tap opens browser, no counter |
| 15 | `Main_Ad_Show=true`, `Main_Ad_Type=native`, refresh true, 30 s | Stay on QR page 90 s | New ad every ~30 s; leaving the page stops timer |
| 16 | Same, refresh false, 30 s | Swipe Home->QR within 30 s, then after 30 s | Within 30 s keeps ad; after 30 s loads new ad |
| 17 | `Right_Swipe_Interstitial_Ad_Show=true`, threshold 2, `Default_App_Popup_Show=false` | Swipe to QR 3 times | Ad on swipe 2 only |
| 18 | Popup Show=true, Count=0, not default, ad also on | Swipe to QR | Role dialog appears, NO interstitial; counter unchanged |
| 19 | Popup Count=2 | Swipe twice quickly, then after 12 h | 2nd swipe: ad path (popup not due); popup again after 12 h |
| 20 | Popup due, user taps Don't allow | Swipe again later | Interval counted from the first launch time; card still visible |
| 21 | Not default, tap Home card/Set Now, accept | Return | Card hidden, `DEFAULT_HOME_APP_SET` once |
| 22 | Decline once, tap card again | Instant return (< 800 ms) | System Home settings + guide activity opens |
| 23 | Redirect false, not default | Cold start | Compose app only; no card, no popup, no App Open |
| 24 | Redirect true, not default | Cold start | Launcher UI; card on Home and drawer |
| 25 | Default Home, `App_Open_Ad_Show=true`, per-day 4 | Background and return on QR page | App Open at most every 6 h; none on wallpaper Home |
| 26 | Screen inter: `inter_ads_on_back=true`, `inter_ads_back_click=2`, flags on 2 screens | Back from 3 screens | Shared counter: 2nd qualifying back shows the ad, regardless of screen |
| 27 | Bottom nav: show true, click 3 | Tap Home/Settings/Scan | Ad on 3rd tab switch; same-tab tap not counted |
| 28 | Screen ad `QrForm_Ad_Show=true`, native | Open form with keyboard | Ad stays at bottom, content shrinks only by overlap |
| 29 | `CreateHub_top_Ad_Show=true` | Open hub, search, clear search | Big native fresh per open; hidden while searching |
| 30 | `SettingsFragment_Native_Second=0` | Open Settings repeatedly | One request, cached card reused |

## 6. Common bugs and gotchas

1. Click counter is in memory (starts at 1, never reset by RC apply); return counter is persisted. Expect "ad on first tap after restart" confusion only if Count=1.
2. Empty/misspelled `LauncherApp_Ad_Type` entries are silently dropped; an all-invalid list yields no ad but still consumes the counter.
3. Google search box and Quiz icons are not counted and do not arm the return ad; QR shortcut tiles never run click ads.
4. `LauncherApp_Native_Id` is shared by the left-page large native and the full-screen `Google_Native` step; an empty id breaks both (native step falls back to Quiz or no ad).
5. `Default_App_Popup_Count <= 0` means unlimited popups per right swipe, not "off". Use `Default_App_Popup_Show=false` to disable. The slot time is saved before the user answers.
6. When the popup is due, the Right Swipe ad (and its counter) is skipped; a product expecting both on the same swipe will see only the popup.
7. `DefaultHomePopupHost` in MainActivity is effectively dead (`markRightSwipeQrVisible` has no caller); `view_default_home_guide.xml` is unused (guide uses `activity_overlay_permission_default_home.xml`).
8. `Redirect_Home_Launcher` is not persisted; a cold start before RC loads behaves as false (`unverified` how Splash orders this).
9. `Ad_Priority=QUIZ` does not change launcher click/return sequences nor App Open; it does change banners/natives/screen interstitials.
10. Drawer native: placeholder index 8 counts Quiz icons; `last_show_time` is written only when the ad is displayed, and a re-shown cached ad does not extend the interval; the cache is static (<= 55 min) and survives drawer closes.
11. Home bottom ad with refresh off needs a leave-and-return swipe; with `Main_Bottom_Ad_Auto_Second=0` it never reloads.
12. Screen ads: `Type` default is "banner" in `ScreenLoadAd`, whereas the legacy `AP.otherAdType` default is "native"; `OtherScreen` Compose screens use the former.
13. Screen interstitial counters are global across all screens and reset on every RC apply (including the 12 h refresh in `onResume`), which can delay a due ad.
14. `ViewPager2.setCurrentItem(PAGE_RIGHT, false)` from `onResume` (pending Scan request, e.g. History/Call End) should still hit the IDLE callback and the popup/ad path (`unverified` on-device).
15. `showOrLoad` for big natives with seconds 0 never refreshes after the first successful ad; after a failed first load it retries on each visibility.
