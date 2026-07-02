package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbeSettingsTest {
    @Test
    fun defaultProbeIntervalIsFiveSeconds() {
        assertEquals(5, ProbeSettings().intervalSeconds)
    }

    @Test
    fun exposesCompactProbeIntervalOptions() {
        assertEquals(listOf(2, 5, 10, 30), ProbeIntervalOptions.seconds)
        assertEquals("Every 10 sec", ProbeIntervalOptions.label(10))
    }

    @Test
    fun unsupportedProbeIntervalFallsBackToDefault() {
        assertEquals(5, ProbeIntervalOptions.normalize(7))
    }
}
