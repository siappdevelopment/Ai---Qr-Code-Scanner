---
name: drawer-native-ads
description: Build, modify, debug or port the medium native ad inside the launcher app drawer (LauncherAppsBottomSheet) - the in-list ad slot after the 8th app, the floating (sticky) ad overlay, per-day interval, the cached "show the last ad again" behaviour, Quiz-priority path, shimmer, touch forwarding, list clipping so apps never hide behind the ad, and the Remote Config keys LauncherApp_Native_List_*. Use when porting the drawer ad to another launcher, changing its size/position/interval, or fixing a drawer where the list is hidden behind the ad, the ad does not show after the interval, or scrolling is blocked over the ad.
---

# App drawer native ad

Reference implementation: QR Code & Scanner, package `com.qrcode.scanner`, `launcher/dialogs/LauncherAppsBottomSheet.java` (the drawer), `launcher/adapters/LauncherAppsAdapter.java` (the list). Source is copied under `assets/`.

The drawer is a bottom sheet with a search bar and a 4-column grid (`rvApps`). One **medium Google native ad** is shown in it.

## How the ad is built (two layers)

| Layer | What | Where |
|---|---|---|
| **Slot (spacer)** | A full-width list item (`adapter_launcher_native_spacer.xml`, height `launcher_native_list_ad_height`) inserted at index 8 of the display items (`LIST_NATIVE_AFTER_ITEMS = 8`, only when the list has at least 8 items). It is empty or shows a shimmer; it only reserves room. | `LauncherAppsAdapter.rebuildDisplayItems`, `setListNativePlaceholderEnabled`, `setListNativeShimmerVisible` |
| **Overlay (the real ad)** | `rlNativeListAdView` > `flNativeListAd`, a sibling of `rvApps` inside the same FrameLayout (`bottom_sheet_launcher_apps.xml`), `layout_gravity=bottom`. It is moved with `translationY` so it sits exactly on the slot, and sticks to the bottom when the slot is off screen. | `updateStickyNativeListAdPosition()` |

Why an overlay: the native ad view must live outside the RecyclerView so it is not recycled, and so it keeps its loaded state while the list scrolls.

## Position rules (`updateStickyNativeListAdPositionCore`)

- `stickyNaturalTop = parentHeight - adHeight - margin` (the bottom-docked spot).
- Slot found and on screen: `translationY = min(0, slotTop - stickyNaturalTop)`, so the ad rides on the slot while it is above the bottom spot.
- Slot scrolled past the top: `translationY = 0`, the ad stays docked at the bottom.
- Slot not laid out yet: the ad is parked off screen (`parkNativeListAdOffScreen`) and the update is retried (never flashes at the bottom).
- Short list (no slot): `placeNativeAdBelowVisibleApps` puts it just under the last visible app, so a short search result does not leave a gap between apps and ad. The rest of the screen below it stays empty on purpose.

## Apps must never hide behind the ad (`syncStickyListInset`)

Only while the ad **floats at the bottom** (`translationY == 0`): the list's drawing area is clipped above the ad with `rvApps.setClipBounds(...)`, and the list gets bottom padding equal to the ad's height so the last rows can still scroll above it. While the ad sits **in its slot**, there is no clip and no extra padding, so the rows after the ad stay visible and no blank space appears under it.

Window-insets handling (`applyRecyclerImePaddingIfNeeded`, `clearExpandedWindowInsets`) also writes the list's bottom padding (keyboard height). It uses `max(keyboard, nativeListAdReservePx)` and `lastImeBottomPx`, so it never wipes the ad's reserve. Do not set the list padding anywhere else.

## When the ad shows (`shouldShowNativeListAd`)

1. Fragment added.
2. Quiz priority (`AdPlacement.shouldUseQuizPriority()`): only needs `LauncherApp_Native_List_Ad_Show`; a Quiz ad fills the slot (sync).
3. Otherwise all of: `LauncherApp_Native_List_Ad_Show`, interval passed (`canShowLauncherAppNativeListAd`: gap = 24 h / `LauncherApp_Native_List_Ad_Show_Per_Day`, `<= 0` = always), network available, a unit id, and `canRequestAds` (consent).

The last-show time (`launcher_app_native_list_ad_preferences.last_show_time`) is written when a **new** ad finishes loading, not when a cached one is shown again.

## Cached ad: show the last ad again (`showCachedNativeListAd`)

The interval only decides when a **new** ad is requested. Between requests the drawer would otherwise show no ad at all. So the last loaded Google native ad is kept in a `static cachedNativeListAd` (survives closing the drawer). When the drawer opens and no new ad is due, `showCachedNativeListAd()` re-binds it into the new fragment's container via `AdPlacement.bindLoadedNative(...)` and reveals it. When the interval has passed, a new ad loads and replaces (destroys) the cached one. A failed load keeps the old one. A cached ad older than 55 minutes is dropped (Google native ads expire after about an hour). Quiz ads are not cached. `destroyLoadedNativeListAd()` never destroys the cached ad.

## Other behaviour

- **Shimmer:** the slot shows `shimmer_medium_ad` while loading (`setListNativeShimmerVisible(true)`); the overlay stays hidden until the ad is bound.
- **Touch forwarding:** `ScrollForwardingRelativeLayout` forwards vertical drags that start on the ad to `rvApps` (`setScrollTarget`), so the list can still be scrolled with a finger on the ad.
- **Ad type:** `NATIVE_LIST_AD_TYPE = "medium"` -> `native_medium_ad_layout.xml`; `AdPlacement.loadNativeAd(activity, id, rl, shimmer, fl, "medium", onLoaded, onFailed)`.
- **Drawer destroy:** `onDestroyView` stops shimmer, bumps `nativeListAdRequestToken` (so a late result is ignored) and destroys only a non-cached loaded ad.

## Remote Config

All inside `Screen.LauncherAppScreen` (parsed in `RemoteConfigHelper`):

| Key | Meaning |
|---|---|
| `LauncherApp_Native_List_Ad_Show` | master switch, default false |
| `LauncherApp_Native_List_Ad_Show_Per_Day` | new ad at most this many times a day (gap = 24 h / value); `<= 0` = no limit |
| `LauncherApp_Native_List_Id` | native ad unit id |

(Do not confuse with `LauncherApp_Native_Ad_Show` / `_Id`, a different native used by the launcher-app back ad.)

## Files in `assets/`

| Asset | Goes to |
|---|---|
| `java/LauncherAppsAdapter.java` | `launcher/adapters/` (slot item, shimmer toggle, `findNativePlaceholderPosition`) |
| `java/ScrollForwardingRelativeLayout.java` | `launcher/common/` |
| `layout/bottom_sheet_launcher_apps.xml` | the drawer layout (search, `rvApps`, `rlNativeListAdView`, shimmer, `flNativeListAd`) |
| `layout/adapter_launcher_native_spacer.xml`, `shimmer_medium_ad.xml`, `native_medium_ad_layout.xml` | `res/layout/` |
| `snippets/dimens_launcher.xml` | `res/values/`: `launcher_native_list_ad_height`, `launcher_native_list_margin`, `launcher_native_list_spacer_height` |
| `snippets/LauncherAppsBottomSheet.ads.snippet.java` | the ad part of the sheet: fields, `showAd`, `shouldShowNativeListAd`, cache, `maybeLoadNativeListAd`, reveal / hide, padding + clip, sticky position |
| `snippets/AdPlacement.drawerAd.snippet.java` | interval check, `bindLoadedNative`, getters/setters |
| `snippets/RemoteConfigHelper.drawerAd.snippet.java`, `remote_config.json` | key parsing + example |

## Porting checklist

1. Copy the layouts, dimens, adapter and `ScrollForwardingRelativeLayout`; wire `rvApps`, `rlNativeListAdView`, `slNativeListShimmer`, `flNativeListAd` in the sheet's `onCreateView` (set `setScrollTarget(rvApps)`).
2. Merge the sheet snippet; call `showAd()` after the list is ready; call `updateStickyNativeListAdPosition()` from the list's scroll / layout callbacks and after the adapter changes.
3. Port from `AdPlacement`: `loadNativeAd` (with the onLoaded / onFailed overload), `populateNativeAdView`, `resolveGoogleNativeLayout`, `bindLoadedNative`, `canRequestAds`, `isNetworkAvailable`, `shouldUseQuizPriority`.
4. Test: first open (shimmer, then ad), close and reopen inside the interval (the same ad shows), reopen after the interval (new ad), scroll with the slot above / on screen / below, search with few results, keyboard open, Quiz priority on.
