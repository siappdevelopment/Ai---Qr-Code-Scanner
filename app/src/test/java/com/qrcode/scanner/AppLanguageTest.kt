package com.qrcode.scanner

import androidx.core.os.LocaleListCompat
import com.qrcode.scanner.ui.screens.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {
    @Test
    fun default_isEnglishUs() {
        assertEquals(AppLanguage.ENGLISH_US, AppLanguage.DEFAULT)
        assertEquals("en-US", AppLanguage.ENGLISH_US.languageTag)
        assertEquals("English (US)", AppLanguage.ENGLISH_US.settingsSubtitle)
    }

    @Test
    fun system_hasNullTag_andEmptyLocaleList() {
        assertNull(AppLanguage.SYSTEM.languageTag)
        assertTrue(AppLanguage.SYSTEM.toLocaleList().isEmpty)
        assertEquals("Follow device language", AppLanguage.SYSTEM.settingsSubtitle)
    }

    @Test
    fun fromLanguageTag_mapsEnglishVariants() {
        assertEquals(AppLanguage.ENGLISH_US, AppLanguage.fromLanguageTag("en-US"))
        assertEquals(AppLanguage.ENGLISH_US, AppLanguage.fromLanguageTag("en_US"))
        assertEquals(AppLanguage.ENGLISH_US, AppLanguage.fromLanguageTag("en"))
        assertEquals(AppLanguage.ENGLISH_US, AppLanguage.fromLanguageTag("EN-us"))
    }

    @Test
    fun fromLanguageTag_blankMeansSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("   "))
    }

    @Test
    fun fromLanguageTag_unsupportedFallsBackToEnglish() {
        // Do not pretend Spanish/French/etc. are supported.
        assertEquals(AppLanguage.ENGLISH_US, AppLanguage.fromLanguageTag("es"))
        assertEquals(AppLanguage.ENGLISH_US, AppLanguage.fromLanguageTag("fr-FR"))
        assertEquals(AppLanguage.ENGLISH_US, AppLanguage.fromLanguageTag("ja"))
        assertEquals(AppLanguage.ENGLISH_US, AppLanguage.fromLanguageTag("zh-CN"))
        assertEquals(AppLanguage.ENGLISH_US, AppLanguage.fromLanguageTag("hi"))
    }

    @Test
    fun fromLocaleList_emptyIsSystem_englishIsEnglish() {
        assertEquals(
            AppLanguage.SYSTEM,
            AppLanguage.fromLocaleList(LocaleListCompat.getEmptyLocaleList())
        )
        assertEquals(
            AppLanguage.ENGLISH_US,
            AppLanguage.fromLocaleList(LocaleListCompat.forLanguageTags("en-US"))
        )
    }

    @Test
    fun english_toLocaleList_usesEnUsTag() {
        assertEquals("en-US", AppLanguage.ENGLISH_US.toLocaleList().toLanguageTags())
    }

    @Test
    fun onlySupportedEntries_areSystemAndEnglish() {
        assertEquals(
            listOf(AppLanguage.SYSTEM, AppLanguage.ENGLISH_US),
            AppLanguage.entries.toList()
        )
    }
}
