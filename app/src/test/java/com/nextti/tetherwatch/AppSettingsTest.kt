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
        assertTrue(settings.pingMonitoringEnabled)
        assertTrue(settings.statusMonitoringEnabled)
    }

    @Test
    fun canEnableBackgroundKeepScreenAndProbeToggles() {
        val settings = AppSettings(
            autoBackgroundMonitoring = true,
            keepScreenOn = true,
            themeMode = AppThemeMode.DARK,
            pingMonitoringEnabled = false,
            statusMonitoringEnabled = false
        )

        assertTrue(settings.autoBackgroundMonitoring)
        assertTrue(settings.keepScreenOn)
        assertEquals(AppThemeMode.DARK, settings.themeMode)
        assertFalse(settings.pingMonitoringEnabled)
        assertFalse(settings.statusMonitoringEnabled)
    }
}
