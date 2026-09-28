package com.qrcode.scanner.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Supported per-app languages (Phase 12.19).
 *
 * Only languages with real product support are listed — English is the sole
 * shipped UI language today (hardcoded Compose copy + values/strings.xml).
 * Unsupported Stitch locales are intentionally omitted.
 */
enum class AppLanguage(
    /** BCP-47 tag for AppCompat; null means follow system (empty LocaleList). */
    val languageTag: String?,
    val displayName: String,
    val settingsSubtitle: String
) {
    SYSTEM(
        languageTag = null,
        displayName = "System Synced",
        settingsSubtitle = "Follow device language"
    ),
    ENGLISH_US(
        languageTag = "en-US",
        displayName = "English (US)",
        settingsSubtitle = "English (US)"
    );

    fun toLocaleList(): LocaleListCompat =
        if (languageTag.isNullOrBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }

    companion object {
        /** Product default when no explicit app locale is stored. */
        val DEFAULT: AppLanguage = ENGLISH_US

        fun fromLanguageTag(tag: String?): AppLanguage {
            if (tag.isNullOrBlank()) return SYSTEM
            val normalized = tag.replace('_', '-')
            return entries.firstOrNull { option ->
                val optionTag = option.languageTag ?: return@firstOrNull false
                normalized.equals(optionTag, ignoreCase = true) ||
                    normalized.substringBefore('-')
                        .equals(optionTag.substringBefore('-'), ignoreCase = true)
            } ?: DEFAULT
        }

        fun fromLocaleList(locales: LocaleListCompat): AppLanguage {
            if (locales.isEmpty) return SYSTEM
            return fromLanguageTag(locales.toLanguageTags().substringBefore(','))
        }

        fun current(): AppLanguage =
            fromLocaleList(AppCompatDelegate.getApplicationLocales())

        fun apply(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(language.toLocaleList())
        }
    }
}
