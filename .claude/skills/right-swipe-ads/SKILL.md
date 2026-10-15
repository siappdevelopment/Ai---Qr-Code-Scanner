---
name: right-swipe-ads
description: Build, modify, debug or port the Right Swipe flow of the launcher - the interstitial ad shown when the user swipes from the launcher Home page to the QR (right) page, its Remote Config keys (Right_Swipe_Interstitial_Ad_Show / Right_Swipe_Interstitial / Right_Swipe_Ads_type, preload vs load), and the rule that the ad is skipped when the Default Home permission popup is due (Default_App_Popup_Show / Default_App_Popup_Count) so the permission screen shows instead and the ad is not stacked on it. Use when porting this to another launcher app, changing when the ad or the permission popup shows, or fixing an ad that appears together with the Default Home dialog.
---

# Right Swipe ad + Default Home popup

Reference implementation: QR Code & Scanner, package `com.qrcode.scanner`, `launcher/`. Source is copied under `assets/`.

When the user swipes from the launcher **Home page** to the **right page** (the QR app), exactly one of these happens:

```
swipe settles on the right page
 |- Default Home popup is DUE  -> show the system "set as default" dialog, NO ad
 |- otherwise                  -> Right Swipe interstitial (if configured and due)
                                  -> when the ad is done (or skipped): maybeShowDefaultHomePopup()
```

The second `maybeShowDefaultHomePopup()` call is a safety: if the popup became due while the ad ran, it is shown after the ad.

## Files in `assets/`

| Asset | Goes to | Notes |
|---|---|---|
| `snippets/LauncherHomeActivity.pagerCallback.snippet.java` | `LauncherHomeActivity` | the ViewPager2 callback: sets `pendingRightSwipeOpen` in `onPageSelected` (HOME -> RIGHT) and runs the flow in `onPageScrollStateChanged(IDLE)` |
| `snippets/AdPlacement.rightSwipe.snippet.java` | `AdPlacement` | `prepareRightSwipePreload`, `loadRightSwipeInterstitialAd`, preload / show / token handling |
| `snippets/AdPlacement.defaultPopup.snippet.java` | `AdPlacement` | `getDefaultAppPopupShow`, `canShowDefaultAppPopup`, last-show time |
| `snippets/LauncherHomeFragment.rightSwipe.snippet.java` | `LauncherHomeFragment` | `canShowDefaultHomePopup()` / `maybeShowDefaultHomePopup()` forwarders, tutorial hook |
| `snippets/RemoteConfigValues.rightSwipe.snippet.java` | `RemoteConfigValues` | fields, parsing and getters of the keys below |
| `java/DefaultHomePromptHelper.java` | `launcher/helpers/` | owns the role request, `canShowDefaultHomePopup()` (the "is it due" rule) |
| `java/DefaultHomePopupHost.java` | `launcher/helpers/` | the same popup for a plain Activity host (MainActivity) via `EXTRA_RIGHT_SWIPE_QR_VISIBLE` |
| `snippets/remote_config.json` | Firebase | example |

## Remote Config

| Key | Where | Meaning / default |
|---|---|---|
| `Right_Swipe_Interstitial_Ad_Show` | `Screen.MainScreen` | master switch of the ad, default false |
| `Right_Swipe_Interstitial` | `Screen.MainScreen` | show on every Nth swipe. `<= 0` = every swipe. The counter lives in memory (`rightSwipeInterstitialCount`), so it restarts with the process |
| `Right_Swipe_Ads_type` | `Screen.MainScreen` | `preload` = keep one interstitial loaded ahead; anything else (default `load`) = load when the swipe happens (with the loading dialog) |
| interstitial unit id | `inter_ads` (`RemoteConfigValues.getInterAdsId()`) | the unit used for both load and preload |
| `Default_App_Popup_Show` | profile root | enables the Default Home popup, default false |
| `Default_App_Popup_Count` | profile root | popups per day: minimum gap = 24 h / count. `<= 0` = no limit |

## When the Default Home popup is due (`DefaultHomePromptHelper.canShowDefaultHomePopup`)

All of these must be true, else the ad runs as normal:
1. no role request already running (`defaultHomePopupLaunchInProgress`, `waitingForDefaultHome`) and the host is alive,
2. `Default_App_Popup_Show` is true,
3. the app is **not** already the default Home app,
4. `AdPlacement.canShowDefaultAppPopup()`: the per-day gap since `default_app_popup_preferences.last_show_time` has passed,
5. `AppUtils.createDefaultHomeRoleRequestIntent()` is not null (RoleManager has ROLE_HOME and the app does not hold it).

`maybeShowDefaultHomePopup()` writes `setDefaultAppPopupLastShowTime` right before it launches the role request, so the gap starts when the dialog is shown, not when the user answers.

## Ad behaviour (`AdPlacement`)

1. `loadRightSwipeInterstitialAd(activity, onFinished)`:
   - flag off -> `onFinished` at once;
   - Nth-swipe counter (`Right_Swipe_Interstitial`) decides whether this swipe shows an ad; if not -> `onFinished`;
   - `preload` type and a preloaded ad exists (and Quiz priority is off) -> show it (`showReadyRightSwipeAd`);
   - otherwise load now (`loadRightSwipeNow` -> `loadInterstitialAdInternal`, with loading dialog).
2. `prepareRightSwipePreload(context)` runs from `LauncherHomeActivity.onResume` and after Remote Config is applied. It holds a weak reference to the host activity, and clears the cache when the type is not `preload`. A token (`rightSwipePreloadToken`) drops results of an outdated request.
3. After a shown ad is dismissed, the next one is preloaded (`preload` type). If showing fails, it falls back to `loadRightSwipeNow`.
4. Quiz priority (`shouldUseQuizPriority()`) bypasses the preloaded Google ad: the load path shows a Quiz ad instead.

## Behaviour rules (keep when modifying)

1. **No ad when the permission screen is due.** The check `popupFragment.canShowDefaultHomePopup()` runs before the ad; if true it calls `maybeShowDefaultHomePopup()` and `return`s. Never show both.
2. The flow runs only when the pager is IDLE on the right page (`pendingRightSwipeOpen` is set by a HOME -> RIGHT selection). Tapping the Scan icon on Home goes through `setCurrentItem(PAGE_RIGHT)`, which is the same HOME -> RIGHT selection, so it also gets the ad / popup. Programmatic opens that start on the right page do not.
3. `onPageSelected` also calls `notifyRightSwipeTutorialCompleted()` (hides the "swipe right" hint).
4. The popup result handling (success / cancelled, events `DEFAULT_HOME_APP_SET` / `DEFAULT_HOME_APP_CANCEL`, the Home-settings fallback when the system dialog is suppressed) is in the `default-home-role-flow` skill.

## Porting checklist

1. Copy `java/` helpers; fix package and `R` imports. Add the snippets to `AdPlacement`, `RemoteConfigValues` (+ the Remote Config apply code), `LauncherHomeFragment`, `LauncherHomeActivity`.
2. Port what these call: `AppUtils.isDefaultHomeApp`, `createDefaultHomeRoleRequestIntent`, `TrackOnce`, `loadInterstitialAdInternal`, `shouldUseQuizPriority`, `notifyComplete`, `logAdRevenue`.
3. Call `AdPlacement.prepareRightSwipePreload(this)` from the launcher's `onResume`.
4. Test: swipe with the popup due (dialog, no ad), swipe with the popup not due (ad, then nothing), `Right_Swipe_Interstitial = 2` (every second swipe), type `preload` and `load`.
