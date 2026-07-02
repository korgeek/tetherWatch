package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TetheringDashboardBuilderTest {
    @Test
    fun emptyInterfaceSetMeansTetheringIsOff() {
        val dashboard = TetheringDashboardBuilder.build(
            serviceAvailable = true,
            interfaces = emptyList(),
            upstreamInterface = null
        )

        assertEquals(TetheringState.OFF, dashboard.state)
        assertEquals(TetheringClientVisibility.UNKNOWN, dashboard.clientVisibility)
        assertTrue(dashboard.downstreamInterfaces.isEmpty())
    }

    @Test
    fun wifiInterfaceCreatesActiveRestrictedDashboard() {
        val dashboard = TetheringDashboardBuilder.build(
            serviceAvailable = true,
            interfaces = listOf(RawTetheringInterface(TETHERING_TYPE_WIFI, "swlan0")),
            upstreamInterface = "rmnet15"
        )

        assertEquals(TetheringState.ACTIVE, dashboard.state)
        assertEquals(setOf(TetheringType.WIFI), dashboard.types)
        assertEquals(listOf("swlan0"), dashboard.downstreamInterfaces)
        assertEquals("rmnet15", dashboard.upstreamInterface)
        assertEquals(TetheringClientVisibility.RESTRICTED, dashboard.clientVisibility)
    }

    @Test
    fun unavailableServiceCreatesUnavailableDashboard() {
        val dashboard = TetheringDashboardBuilder.build(
            serviceAvailable = false,
            interfaces = listOf(RawTetheringInterface(TETHERING_TYPE_WIFI, "swlan0")),
            upstreamInterface = "rmnet15"
        )

        assertEquals(TetheringState.UNAVAILABLE, dashboard.state)
        assertTrue(dashboard.types.isEmpty())
        assertTrue(dashboard.downstreamInterfaces.isEmpty())
    }
}
