package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardVisualModelBuilderTest {
    @Test
    fun createsSixCompactTilesForTheFirstScreenDashboard() {
        val state = DashboardState(
            score = DashboardScore(86, DashboardHealth.EXCELLENT),
            network = NetworkStateSnapshot(
                isConnected = true,
                isValidated = true,
                isMetered = true,
                transports = listOf("5G/LTE"),
                interfaceName = "rmnet0",
                simSlots = listOf(
                    SimSlotSnapshot(0, 11, "Carrier A", "SIM 1", "KR", false, false),
                    SimSlotSnapshot(1, 22, "Carrier B", "SIM 2", "ID", true, false)
                ),
                country = NetworkCountrySnapshot("ID", "Indonesia", NetworkCountryConfidence.NETWORK_MCC),
                dnsProfiles = listOf(
                    DnsProfile("202.152.0.2", DnsResolverType.ISP, "ISP DNS", "Indonesia", DnsProfileConfidence.MEDIUM),
                    DnsProfile("10.19.19.20", DnsResolverType.PRIVATE, "Private DNS", "Private network", DnsProfileConfidence.HIGH)
                )
            ),
            traffic = TrafficDashboard(
                downloadMbps = "12.40 Mbps",
                uploadMbps = "2.10 Mbps",
                samples = listOf(
                    TrafficSamplePoint(1_000L, 100.0, 20.0),
                    TrafficSamplePoint(2_000L, 200.0, 40.0)
                )
            ),
            probe = ProbeDashboard(summary = ProbeSummary(42, 32, 60, 0, 8, 0)),
            tethering = TetheringDashboard(
                state = TetheringState.ACTIVE,
                types = setOf(TetheringType.WIFI),
                downstreamInterfaces = listOf("swlan0"),
                upstreamInterface = "rmnet15",
                clientVisibility = TetheringClientVisibility.RESTRICTED
            ),
            permissions = listOf(PermissionStatus("Phone state", "android.permission.READ_PHONE_STATE", true))
        )

        val visual = DashboardVisualModelBuilder.build(state)

        assertEquals(listOf("SIM", "Network", "DNS", "Traffic", "Probe", "Tether"), visual.tiles.map { it.label })
        assertEquals("SIM 2", visual.tiles[0].value)
        assertEquals("Indonesia", visual.tiles[1].value)
        assertEquals("ISP DNS", visual.tiles[2].value)
        assertEquals("202.152.0.2 · +1", visual.tiles[2].detail)
        assertEquals("12.40", visual.tiles[3].value)
        assertEquals(listOf(100.0, 200.0), visual.tiles[3].downSeries)
        assertEquals(listOf(20.0, 40.0), visual.tiles[3].upSeries)
        assertEquals("42 ms", visual.tiles[4].value)
        assertEquals("Active", visual.tiles[5].value)
        assertEquals("Wi-Fi / swlan0", visual.tiles[5].detail)
        assertTrue(visual.alerts.any { it.message == "Device list unavailable on this Android build." })
        assertFalse(visual.tiles.any { it.value.contains("Permission denied") })
    }

    @Test
    fun compressesDeniedPermissionsIntoAnAlertInsteadOfRepeatingLongTileText() {
        val state = DashboardState(
            network = NetworkStateSnapshot(
                simSlots = listOf(SimSlotSnapshot.permissionDenied())
            ),
            permissions = listOf(PermissionStatus("Phone state", "android.permission.READ_PHONE_STATE", false))
        )

        val visual = DashboardVisualModelBuilder.build(state)

        assertEquals("Denied", visual.tiles[0].value)
        assertTrue(visual.alerts.any { it.message == "Permission denied. Data was not collected." })
    }
}
