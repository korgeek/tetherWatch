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
                dnsProfiles = listOf(DnsProfile("202.152.0.2", DnsResolverType.ISP, "ISP DNS", "Indonesia", DnsProfileConfidence.MEDIUM))
            ),
            traffic = TrafficDashboard(downloadMbps = "12.40 Mbps", uploadMbps = "2.10 Mbps"),
            probe = ProbeDashboard(summary = ProbeSummary(42, 32, 60, 0, 8, 0)),
            tethering = SimpleStatus("Tethering", "Tethering service available. Waiting for Galaxy callback validation."),
            permissions = listOf(PermissionStatus("Phone state", "android.permission.READ_PHONE_STATE", true))
        )

        val visual = DashboardVisualModelBuilder.build(state)

        assertEquals(listOf("SIM", "Network", "DNS", "Traffic", "Probe", "Tether"), visual.tiles.map { it.label })
        assertEquals("SIM 2", visual.tiles[0].value)
        assertEquals("Indonesia", visual.tiles[1].value)
        assertEquals("ISP DNS", visual.tiles[2].value)
        assertEquals("12.40", visual.tiles[3].value)
        assertEquals("42 ms", visual.tiles[4].value)
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
