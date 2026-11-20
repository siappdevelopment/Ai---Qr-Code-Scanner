# Remote Config schema (every key the app reads)

Source of truth: `launcher/remote/RemoteConfigHelper.java` (`applyScreenConfig`, `applyAdFields`, `applyDefaultLauncherReminder`), `launcher/remote/RemoteConfigValues.java` (`apply`, `applyEventScreen`, `applyScreenAds`, `applyInterAds`) and `launcher/remote/ScreenFlowConfig.java`. Defaults below are the value used when the key is missing.

## 0. How the config is fetched and picked

| Step | Behaviour |
|---|---|
| Parameter name | `"QRScanner_" + versionCode + BuildConfig.RC_KEY_SUFFIX` (`""` release, `"_qa"` QA). The value is one JSON string. |
| Shape | JSON object, or a JSON array whose **first element** is the object. Empty / invalid -> cached values stay (flow list falls back to cache then defaults). |
| Profile | If the root has `paid_user` or `normal_user` (chosen from the stored install referrer, see `referrerProfile`) that profile replaces the root. Its value may be an inline object, a JSON string, or the name of another root key. |
| Separate parameter | `Show_Screen_Flow` can also be its own Firebase parameter (`firebaseRemoteConfig.getString("Show_Screen_Flow")`); the nested one wins. |
| Local defaults | `res/xml/default_config.xml` -> `activityChange`, `Show_Screen_Flow`. |
| Fetch | `minimumFetchInterval` 0; at app start, on splash (joined), when the network returns (`watchNetwork`) and after 12 h (`refreshIfDue`). |
| Persistence | Ad ids/flags are held in memory (`AdPlacement`, `RemoteConfigValues`); call-end + event-screen + right-swipe values are also saved to prefs `cl_end_config_preferences` and restored by `RemoteConfigValues.ensureLoaded` (needed because a phone-state broadcast can start a cold process); launcher back-ad config -> prefs via `persistLauncherBackConfig`; flow list -> `show_screen_flow` prefs. |
| Side effects of a new config | `ScreenInterAds.onConfigApplied` (reset counters + preload), `AdPlacement.prepareRightSwipePreload`, `EventBottomAds.prepare`, `AppProxyLookup.refreshActiveQuizLinks`, `requestCallEndIpCountryIfNeeded`; a changed back-ad sequence resets its saved cursor to 0. |

The profile root is called **R** below; `Screen` is `R.Screen` (an object, `{}` if missing). Every block is optional: a missing object behaves as all keys at default (ads off).

## 1. Root keys (R)

| Key | Type / default | Used by |
|---|---|---|
| `Show_Screen_Flow` | string[] / default flow | onboarding order (see `onboarding-flow-all-in-one`) |
| `Privacy_Policy` | string / "" | Collection screen link |
| `Redirect_Home_Launcher` | bool / false | final destination = Launcher Home even if not default |
| `Ad_Priority` | string / "" | `"QUIZ"` -> Quiz (self-promo) ads replace Google ads everywhere (`shouldUseQuizPriority`) |
| `Google_Ad_Failed_Show_Quiz` | bool / false | Google ad failed or no id/offline -> show the Quiz ad instead |
| `Native_Ad_Label_Color` | string #hex / "" | parsed but no longer applied: native and Quiz ad label + button always use the app primary color (`R.color.primary`, Light/Dark) since the Recent hero redesign |
| `Native_Ad_Button_Color_light` / `Native_Ad_Button_Color_dark` | string #hex / "" | parsed but no longer applied (see above) |
| `App_Open_Id` | string / "" | app-open unit (splash after-ad, resume ad, after-default ad, call-end back ad) |
| `App_Open_Ad_Show` | bool / false | resume app-open ad master switch |
| `App_Open_Dialog_Show` | bool / false | show the 1.5 s "loading ad" dialog before the resume app-open ad |
| `App_Open_Show_Per_Day` | int / 0 | max resume app-open shows per day (`canShowAppOpenAd`) |
| `inter_ads` | object | see section 3 |
| `Cl_End_Screen_Show` | bool / false | Call End screen master switch |
| `Default_App_Popup_Show` | bool / false | "set as default Home" popup on Right Swipe |
| `Default_App_Popup_Count` | int / 0 | show the popup every Nth Right Swipe |
| `Other_Interstitial_Id` | string / "" | fallback for `Screen.OtherScreen.Other_Interstitial_Id` |
| `AppProxyStructure` | object | fallback location of section 8 (also allowed under `Screen`) |
| `charging_screen`, `install_uninstall_screen` | objects | allowed at root **only if** missing under `Screen` (section 6) |
| `launcher_home_screen` | object | default-launcher reminder keys (section 9) |
| `launcher_notification_push_show/_time/_title/_description` | at root if `launcher_home_screen` missing | same |

## 2. `Screen.*` onboarding / launcher blocks (read in `applyAdFields`)

`SplashScreen`: `Splash_Duration` int 0 (seconds) · `Splash_Ad_Show` bool false · `Splash_Ad_Type` `banner`|`native` default `banner` · `Splash_Banner_Id` · `Splash_Native_Id` · `After_Splash_Ad_Show` bool false · `After_Splash_Ad_Type` `inter`|`appopen` default `inter` · `After_Splash_Interstitial_Id`.

`LanguageScreen`: `Language_Ad_Show` false · `Language_Ad_Type` default `native` · `Language_Banner_Id` · `Language_Native_Id` · `Language_Interstitial_Ad_Show` false (inter after Done).

`PermissionDefaultScreen` (Permission + DefaultHome screens): `PermissionDefault_Ad_Show` false · `PermissionDefault_Ad_Type` default `banner` · `PermissionDefault_Banner_Id` · `PermissionDefault_Native_Id` · `After_Default_Ad_Show` (alias `After_default_Ad_Show`) false · `After_Default_Ad_Type` (alias `After_default_Ad_Type`) default `inter`, `inter` or app open.

`IntroScreen`: `Intro_Screen_Count` int 0 (max 3) · `Intro_Ad_Show` false · `Intro_Ad_Type` default `banner` · `Intro_Banner_Id` · `Intro_Native_Id` · `Intro_Interstitial_Ad_Show` false.

`OtherScreen`: `Other_Ad_Show` false · `Other_Ad_Type` default `native` · `Other_Banner_Id` · `Other_Native_Id` · `Other_Interstitial_Id` -> else `R.Other_Interstitial_Id` -> else `R.inter_ads.inter_ads_id` (this one id serves Language, Intro and After-default interstitials). `OtherScreen` is also one of the nine per-screen ad blocks of section 4 (it uses the prefix `Other`).

`LauncherAppScreen`: 
`LauncherApp_Native_List_Ad_Show` false · `LauncherApp_Native_List_Ad_Show_Per_Day` int 0 · `LauncherApp_Native_List_Id` (in-list/floating native in the drawer) ·
`LauncherApp_Click_Ad_Show` false · `LauncherApp_Count` int 0 (every Nth app tap) · `LauncherApp_Ad_Type` string **or array** (round-robin sequence; accepted, case-insensitive: `Google_Inter`/`inter`, `Google_App_Open`/`app_open`/`appopen`, `Google_Native`/`fullnative`/`full_native`, `Quiz_Inter`, `Quiz_App_Open`, `Quiz_Native`, `Quiz_Browser`; unknown names are dropped by `normalizeLauncherAppAdType`) · `LauncherApp_Interstitial_Id` ·
`LauncherApp_Back_Click_Ad_Show` false · `LauncherApp_Back_Count` int 0 · `LauncherApp_Back_Ad_Type` string or array · `LauncherApp_Back_Interstitial_Id` ·
`Google_Ad_Failed_Show_Quiz` (optional here; falls back to the root value) ·
and in `RemoteConfigValues`: `LauncherApp_Native_Ad_Show` false · `LauncherApp_Native_Ad_Show_Per_Day` 0 · `LauncherApp_Native_Id` · `LauncherApp_Quiz_Icon_Show` false · `LauncherApp_Quiz_Icon_Count` 0 (Quiz game icons interleaved after the first N drawer apps, max 40). `LauncherApp_Native_Ad_*` drives the large native on the launcher side page (shown in `onResume`) and `LauncherApp_Native_Id` is also the unit of the full-screen `Google_Native` step of the tap/return sequences. Meaning of 0: per-day keys (`*_Per_Day`, `Default_App_Popup_Count`) = no limit; count keys (`LauncherApp_Count`, `LauncherApp_Back_Count`, `inter_ads_click`, `inter_ads_back_click`, `bottom_nav_inter_click`) = off; `Right_Swipe_Interstitial` 0 = every swipe. Exact roles: `launcher-and-app-screens.md`.

## 3. `inter_ads` (global interstitial for the QR app screens, `applyInterAds`)

| Key | Default | Meaning |
|---|---|---|
| `inter_ads_show` | false | master switch for **click** interstitials |
| `inter_ads_click` | 0 | show every Nth counted click (0 = never) |
| `inter_ads_click_type` | `load` | `preload` = keep one loaded ahead; anything else loads on demand. Also drives onboarding inter preload |
| `inter_ads_on_back` | false | master switch for **back** interstitials |
| `inter_ads_back_click` | 0 | every Nth counted back |
| `inter_ads_back_click_type` | `load` | `preload` / `load` |
| `inter_ads_id` | "" | click unit (also fallback for back, for the ClEnd back inter, and `OtherScreen` inter) |
| `inter_back_ads_id` | "" | back unit (falls back to `inter_ads_id`) |

A screen only counts when **both** the master flag and its own `on_click_inter_ads_show` / `on_back_inter_ads_show` (inside that screen's block) are true.

## 4. Per-screen ad blocks (`applyScreenAds`, `SCREEN_AD_KEYS`)

Object name -> key prefix: `ScanResultScreen`/`ScanResult`, `CreateHubScreen`/`CreateHub`, `QrFormScreen`/`QrForm`, `QrCustomizationScreen`/`QrCustomization`, `QrPreviewScreen`/`QrPreview`, `HistoryScreen`/`History`, `HistoryDetailScreen`/`HistoryDetail`, `LauncherSettingsScreen`/`LauncherSettings`, `OtherScreen`/`Other`.
Keys per block: `<Prefix>_Ad_Show` false · `<Prefix>_Ad_Type` `banner`|`native` default `banner` · `<Prefix>_Banner_Id` · `<Prefix>_Native_Id` · `on_back_inter_ads_show` false · `on_click_inter_ads_show` false.
Used by `ScreenLoadAd.attach` (native default size `medium`, `small` on request) and `ScreenInterAds`.

## 5. `Screen.MainScreen`, `SettingsFragmentScreen`, `CreateHubScreen`

`MainScreen`: `Main_Ad_Show` false · `Main_Big_Top_Ad_Show` false (big native at the top of the QR home) · `Main_Ad_Type` default `banner` · `Main_Banner_Id` · `Main_Native_Id` · `Main_Ad_Auto_Second` 0 · `Main_Bottom_Ad_Auto_Refresh` false · `Main_Bottom_Ad_Auto_Second` 0 (auto-refresh interval) · `bottom_nav_inter_ads_show` false · `bottom_nav_inter_ads_type` `load` · `bottom_nav_inter_ads_id` · `bottom_nav_inter_click` 0 (every Nth bottom-bar tap) · `Right_Swipe_Interstitial_Ad_Show` false · `Right_Swipe_Interstitial` int 0 (every Nth swipe) · `Right_Swipe_Ads_type` `load`|`preload` (read case-insensitively) · plus `on_back_inter_ads_show` / `on_click_inter_ads_show` (remembered for key `MainScreen`).
`SettingsFragmentScreen`: `SettingsFragment_Native_Ad_Show` false · `SettingsFragment_Native_Id` · `SettingsFragment_Native_Second` 0 (refresh seconds) · `on_*_inter_ads_show`.
`CreateHubScreen`: `CreateHub_top_Ad_Show` (alias `CreateHub_Top_Ad_Show`) false -> big native on top of the Create hub; the rest of the block is the generic per-screen block (section 4).

## 6. Event screens (`charging_screen`, `install_uninstall_screen`; under `Screen`, or under root if absent in `Screen`)

Prefix `charging_screen` / `install_uninstall_screen` (`P`). Flags: `charging_screen.charge_in_screen_show`, `charging_screen.charge_out_screen_show`, `install_uninstall_screen.install_screen_show`, `install_uninstall_screen.uninstall_screen_show` (all false). Then, per block:

| Key | Default | Meaning |
|---|---|---|
| `P_bottom_ad_show` | false | bottom ad on/off |
| `P_ad_load_type` | `load` | `preload` = ad loaded before the screen opens (`EventBottomAds`) |
| `P_bottom_ad_type` | `native` | `native` or `banner` |
| `P_native_id`, `P_banner_id` | "" | units |
| `P_button_show_sec` | 6 | seconds before the Done button appears |
| `P_back_ad_show` | false | back/Done ad on/off |
| `P_back_ad_sequence` | [] | array of `[type,count]` pairs (or `"type,count"` strings); raw types kept (Google_Inter, Google_AppOpen, Google_Native, Quiz_*) |
| `P_inter_id`, `P_full_native_id` | "" | units for the back ad (inter / full-screen native) |
| `P_back_ad_day_count` | 0 | install-day gate: ad allowed only once days-since-install >= this (0 or less than that = blocks, see event-screens.md) |
| `P_back_ad_total_show_count` | 0 | despite the name: back ads **per day**; minimum gap = 24h / N; 0 = never |
| `P_back_ads_show_country` | [] | English country **names** ("India"), compared case-insensitively; **not** ISO codes; empty = all |

Details and flow: `event-screens.md`.

## 7. `Screen.ClEndScreen` (Call End) – read in `RemoteConfigValues.apply`

`ClEnd_Ad_Show` false · `ClEnd_Ad_Type` `banner` · `ClEnd_Banner_Id` · `ClEnd_Native_Id` · `ClEnd_Back_Ad_Show` false · `ClEnd_Back_Ad_Type` `inter` · `ClEnd_Back_Ad_Sequence` (array of `[type,count]` or `"type,count"`; types normalised to `inter`/`appopen`/`native`) · `ClEnd_Back_Ad_Show_After_Day` 0 · `ClEnd_Back_Ad_Show_Per_Day` 0 · `ClEnd_Back_Ad_Interstitial_Id` (falls back to `inter_ads_id`) · `ClEnd_Back_Ad_Native_Id` (the back native actually uses `ClEnd_Native_Id` first) · `ClEnd_Back_Ad_Country_IP` false · `ClEnd_Back_Ad_Show_Country` [] · `Notification_Install_Days` 0 · `Notification_Call_Install_Days` 0 · `Notification_Call_Overlay_Install_Days` 0 · `All_Allow_Permission_Show_Notification` false · `Notification_Back_Ad_Show` false · `Notification_Close_Button_Show` false · `Notification_Country` [] · `Notification_Call_Country` [] · `Notification_Call_Overlay_Country` [] (all country lists hold full English country names, e.g. "India", compared case-insensitively - not ISO codes; the install-day, country and permission-tier gate `isCallEndPerformanceAllowed` applies only to the FCM path; `ClEnd_Back_Ad_Show_After_Day=0` means no back ad in the normal flow; `ClEnd_Back_Ad_Show_Per_Day` is a minimum gap of 24h / value). Root: `Cl_End_Screen_Show`. Details: `callend-and-reminder.md`.

## 8. Quiz / proxy blocks

`Screen.QuizAdsDesign` (lists are JSON arrays or `|`-separated strings; each list is replaced only when present): `Ad_Priority`, `Google_Ad_Failed_Show_Quiz` (override the root ones), `Quiz_App_Icon`, `Quiz_Native_Media`, `Quiz_Interstitial_Media`, `Quiz_App_Open_Media`, `Quiz_Banner_Title`, `Quiz_Banner_Description`, `Quiz_Native_Title`, `Quiz_Native_Description`, `Quiz_Interstitial_Title`, `Quiz_Interstitial_Description`, `Quiz_Interstitial_Rate`, `Quiz_Button_Text` (first item used). `getQuizSyncedItemCount` = min list length (max 3) over icon, banner title/description, native title/description, interstitial title/description; 0 disables Quiz ads.
`Screen.AppProxyStructure` (or root): `App_Proxy_Check_Ip` bool · `App_Proxy_Ip_Checker_Url` · `Quiz_Link_List` · `Quiz_Link_List_Exclude` · `App_Proxy_List_City` · `App_Proxy_List_State` · `App_Proxy_List_Country`. `AppProxyLookup` looks up the user's IP location and removes the quiz links for matching city/state/country so the Quiz ad link is not opened where it is excluded (`pickQuizLink` picks one at random).

## 9. Default-launcher reminder (`launcher_home_screen`, else root)

`launcher_notification_push_show` false · `launcher_notification_push_time` 24 (**times per day**, not hours: interval seconds = 86400 / value, so 24 -> every 1 h, 12 -> 2 h, 48 -> 30 min, 5760 -> 15 s; value <= 0 falls back to 24; interval >= 15 min uses one periodic WorkManager job, below that a self-rescheduling one-time chain; values above 17280 are floored at 5 s) · `launcher_notification_push_title` "" · `launcher_notification_push_description` "". A missing key never breaks the rest of the config (try/catch). Details: `callend-and-reminder.md`.

## 10. Minimal complete example

See `assets/snippets/remote_config.full.json` (every key above with sample values).
