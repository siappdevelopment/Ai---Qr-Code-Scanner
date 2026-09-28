package com.qrcode.scanner

import com.qrcode.scanner.data.settings.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeModeTest {
    @Test
    fun default_isLight() {
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStored(null))
        assertEquals("light", AppThemeMode.LIGHT.storageValue)
        assertEquals("Light (Electric Cobalt)", AppThemeMode.LIGHT.settingsSubtitle())
    }

    @Test
    fun fromStored_mapsKnownValues() {
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStored("light"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromStored("DARK"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromStored("system"))
    }

    @Test
    fun fromStored_fallsBackToLight_whenMissingOrInvalid() {
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStored(null))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStored(""))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStored("auto"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStored("white"))
    }

    @Test
    fun resolveDark_respectsModeAndSystem() {
        assertFalse(AppThemeMode.LIGHT.resolveDark(systemDark = true))
        assertFalse(AppThemeMode.LIGHT.resolveDark(systemDark = false))
        assertTrue(AppThemeMode.DARK.resolveDark(systemDark = false))
        assertTrue(AppThemeMode.DARK.resolveDark(systemDark = true))
        assertTrue(AppThemeMode.SYSTEM.resolveDark(systemDark = true))
        assertFalse(AppThemeMode.SYSTEM.resolveDark(systemDark = false))
    }

    @Test
    fun selectionTitles_matchProductCopy() {
        assertEquals("Light", AppThemeMode.LIGHT.selectionTitle())
        assertEquals("Dark", AppThemeMode.DARK.selectionTitle())
        assertEquals("System Default", AppThemeMode.SYSTEM.selectionTitle())
    }
}
