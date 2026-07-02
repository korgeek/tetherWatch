package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultsFollowDeviceThemeAndAskBeforeBackgrounding() {
        val settings = AppSettings()

        assertEquals(AppThemeMode.SYSTEM, settings.themeMode)
        assertFalse(settings.autoBackgroundMonitoring)
        assertFalse(settings.keepScreenOn)
    }

    @Test
    fun canEnableBackgroundAndKeepScreenOptions() {
        val settings = AppSettings(
            autoBackgroundMonitoring = true,
            keepScreenOn = true,
            themeMode = AppThemeMode.DARK
        )

        assertTrue(settings.autoBackgroundMonitoring)
        assertTrue(settings.keepScreenOn)
        assertEquals(AppThemeMode.DARK, settings.themeMode)
    }
}
