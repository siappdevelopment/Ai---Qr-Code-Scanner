# Phase 3 — Language, locale, and night mode

Phase 3 migrates the source language catalog, locale persistence, and night-mode restore. The five-screen flow is unchanged. Scanner, Create, History, QR generation, result screens, and QR Settings were not modified.

## Source files inspected

- `activities/LanguageActivity.java`
- `adapters/LanguageAdapter.java`
- `helpers/LocaleHelper.java`
- `common/AppUtils.java` language methods: prefs file `language`, key `language`, default `en`, `restoreSavedLanguage`, `applyLanguage`, `getStringForLanguage`
- `common/ThemeUtils.java` prefs `APP_PREF` / `pref_theme`
- `helpers/ThemeHelper.java` prefs `theme_preferences`
- `MyApplication.applySavedTheme`
- `activities/LauncherHomeActivity` theme-return handling
- `fragments/LauncherHomeFragment` language-change listener
- `fragments/SettingsFragment` theme dialog (source QR settings)
- `res/values-*` translations and language flag drawables

No language or theme analytics events were found in the source language or theme classes.

## Target files changed

- `app/src/main/java/com/qrcode/scanner/launcher/activities/LanguageActivity.java`
- `app/src/main/java/com/qrcode/scanner/launcher/adapters/LanguageAdapter.java`
- `app/src/main/java/com/qrcode/scanner/launcher/common/AppUtils.java`
- `app/src/main/java/com/qrcode/scanner/launcher/common/ThemeUtils.java`
- `app/src/main/java/com/qrcode/scanner/launcher/helpers/LocaleHelper.java`
- `app/src/main/java/com/qrcode/scanner/launcher/helpers/ThemeHelper.java`
- `app/src/main/java/com/qrcode/scanner/QrScannerApplication.java`
- `app/src/main/java/com/qrcode/scanner/launcher/activities/LauncherHomeActivity.java`
- `app/src/main/java/com/qrcode/scanner/launcher/fragments/LauncherHomeFragment.java`
- `app/src/main/res/values/themes.xml` (`Theme.LauncherSettings` parent is DayNight)
- `app/src/main/res/values-night/themes_launcher.xml`
- Language flag drawables under `app/src/main/res/drawable`
- Launcher/onboarding strings in `app/src/main/res/values-*/strings_launcher.xml`

`Theme.QRCodeScanner` was not changed. Compose `QRCodeScannerTheme` and the QR Settings theme picker were not changed.

## Language catalog

Source order, names, native subtitles, and codes:

| Name | Code |
| --- | --- |
| English | `en` |
| Hindi | `hi` |
| Russian | `ru` |
| Italian | `it` |
| French | `fr` |
| Spanish | `es` |
| Japanese | `ja` |
| Korean | `ko` |
| German | `de` |
| Chinese | `zh` |
| Thai | `th` |
| Greek | `el` |
| Portuguese (Portugal) | `pt` |
| Portuguese (Brazil) | `pt-BR` |
| Dutch | `nl` |
| Filipino | `fil` |
| Turkish | `tr` |
| Indonesian | `id` |
| Afrikaans | `af` |

Selecting a row updates the title with `choose_your_language` in that language. The saved code is selected on open. An unknown saved code selects English.

Done applies the code only when it differs from the saved code. If `Language_Interstitial_Ad_Show` is true, the existing language interstitial runs first and then continues. That placement was already present. No new ad placement was added.

If the screen is opened after the language step is already complete, and the startup extra is absent, Done finishes with `RESULT_OK` when the language changed and `RESULT_CANCELED` when it did not. During the startup flow, Done calls `ScreenFlowNavigation.continueAfter` for `Language`.

## Locale behavior

- Preference file `language`, key `language`, default `en`.
- `applyLanguage` stores the code and calls `AppCompatDelegate.setApplicationLocales`.
- `QrScannerApplication` calls `restoreSavedLanguage` on startup.
- `LocaleHelper` registers listeners, `syncResources` restores the saved language and notifies listeners, and `wrap` restores the saved language.
- `LauncherHomeFragment` refreshes when the saved code changes, matching the source detach/attach refresh.
- Launcher and onboarding string translations were copied for the 18 source locales. `app_name` was not translated, so the Compose home and About labels stay on the existing English resource.
- Screen-flow completion keys were not reset. A user who already finished Language is not sent through Language again.

## Night mode

Source startup uses `ThemeUtils`, not `ThemeHelper.applySavedTheme`.

Prefs `APP_PREF` / `pref_theme`:

- `0` system, the default: `MODE_NIGHT_FOLLOW_SYSTEM`
- `1` light: `MODE_NIGHT_NO`
- `2` dark: `MODE_NIGHT_YES`

`QrScannerApplication` applies that saved value on startup.

`Theme.Home` already had a night variant. `Theme.LauncherSettings` now uses a DayNight parent and a night color set, so onboarding and launcher settings follow the saved mode. `Theme.QRCodeScanner` stays the existing light parent used by QR activities. Compose QR theme still follows its own Settings preference, whose default is Light.

`ThemeHelper` (`theme_preferences`, default dark for that helper only) and the launcher-home return flags were migrated. Source `LauncherHomeActivity` consumes those flags. Nothing in the inspected source calls `ThemeHelper.setTheme`, so those flags stay unset unless a later screen sets them.

The source Light / Dark / System picker is `dialog_theme` inside source `SettingsFragment`. That is source QR settings. It was not added to the Compose QR Settings screen.

## Analytics events

None. The source language and theme flow does not log an analytics event.

## Screen-flow integration

Language still completes through `continueAfter` for `Language`. The next screens remain Collection, Permission, Default Home, and Intro. Permission, Default Home, and Intro behavior from Phase 2 was not changed. Splash is not restarted from the language screen.

## Build result

`:app:assembleDebug` succeeded.

`BUILD SUCCESSFUL in 21s`.

## Unit test result

`:app:testDebugUnitTest` succeeded. No failures and no errors.

## Device verification status

Not run. Language selection, locale recreation, and night mode were not exercised on a device.

## Intentionally pending

- Install Referrer changes beyond Phase 1
- Remaining Remote Config migration
- Quiz, app proxy, right-swipe interstitial, and process app-open
- Launcher sub page and weather
- Settings shortcut and the source QR theme dialog
- Default Home popup and setup preferences
- Call-end, `PhoneCallStateService`, and FCM
- Reminders, `BootReceiver`, and widgets
- Crashlytics, Facebook mediation, and ProGuard migration
- QR product changes
