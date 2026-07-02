package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardVisualModelBuilderTest {
    @Test
    fun createsCompactHeaderAndEssentialTilesForTheFirstScreenDashboard() {
        val state = DashboardState(
            score = DashboardScore(86, DashboardHealth.EXCELLENT),
            network = NetworkStateSnapshot(
                isConnected = true,
                isValidated = true,
                isMetered = true,
                transports = listOf("5G/LTE"),
                networkTypeLabel = "5G",
                interfaceName = "rmnet0",
                ipAddresses = listOf("10.0.2.15"),
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
                clientCount = null,
                clientVisibility = TetheringClientVisibility.RESTRICTED
            ),
            permissions = listOf(PermissionStatus("Phone state", "android.permission.READ_PHONE_STATE", true))
        )

        val visual = DashboardVisualModelBuilder.build(state)

        assertEquals("5G", visual.header.networkTypeLabel)
        assertEquals("SIM 2 · Carrier B", visual.header.simProviderLabel)
        assertEquals("ID", visual.header.countryCode)
        assertEquals("Tethering", visual.header.tetheringLabel)
        assertEquals("", visual.header.clientCountLabel)
        assertTrue(visual.header.tetheringActive)
        assertEquals(listOf("DNS"), visual.tiles.map { it.label })
        assertEquals("202.152.0.2 · 10.19.19.20", visual.tiles[0].value)
        assertEquals("", visual.tiles[0].detail)
        assertEquals("IP 10.0.2.15", visual.hero.activeSimLabel)
        assertFalse(visual.alerts.any { it.message == "Device list unavailable on this Android build." })
        assertFalse(visual.tiles.any { it.value.contains("Permission denied") })
    }

    @Test
    fun heroUsesIpAddressInsteadOfSimLabelWhenAvailable() {
        val state = DashboardState(
            network = NetworkStateSnapshot(
                ipAddresses = listOf("10.0.2.15"),
                simSlots = listOf(SimSlotSnapshot(1, 22, "Carrier B", "SIM 2", "ID", true, false)),
                country = NetworkCountrySnapshot("ID", "Indonesia", NetworkCountryConfidence.NETWORK_MCC)
            )
        )

        val visual = DashboardVisualModelBuilder.build(state)

        assertEquals("IP 10.0.2.15", visual.hero.activeSimLabel)
    }

    @Test
    fun headerPrefersWifiConnectionOverMobileSimWhenWifiIsActive() {
        val state = DashboardState(
            network = NetworkStateSnapshot(
                isConnected = true,
                transports = listOf("Wi-Fi", "5G"),
                networkTypeLabel = "5G",
                interfaceName = "wlan0",
                simSlots = listOf(SimSlotSnapshot(1, 22, "Carrier B", "SIM 2", "ID", true, false)),
                country = NetworkCountrySnapshot("ID", "Indonesia", NetworkCountryConfidence.NETWORK_MCC)
            )
        )

        val visual = DashboardVisualModelBuilder.build(state)

        assertEquals("Wi-Fi", visual.header.networkTypeLabel)
        assertEquals("wlan0", visual.header.simProviderLabel)
    }

    @Test
    fun dnsTileShowsTwoIpv4ResolversInOneBoldCompactLine() {
        val visual = DashboardVisualModelBuilder.build(
            DashboardState(
                network = NetworkStateSnapshot(
                    dnsServers = listOf("2404:c0:5c1d:9500::1", "10.25.176.130", "114.5.5.5")
                )
            )
        )

        val dnsTile = visual.tiles.first { it.label == "DNS" }
        assertEquals("10.25.176.130 · 114.5.5.5", dnsTile.value)
        assertEquals("", dnsTile.detail)
    }

    @Test
    fun tetherStateStaysInHeaderButDoesNotCreateASeparateTile() {
        val visual = DashboardVisualModelBuilder.build(
            DashboardState(tethering = TetheringDashboard(state = TetheringState.ACTIVE, clientCount = 2))
        )

        assertEquals("Tethering", visual.header.tetheringLabel)
        assertEquals("2", visual.header.clientCountLabel)
        assertFalse(visual.tiles.any { it.label == "Tether" })
    }

    @Test
    fun restrictedTetherClientListDoesNotShowUnknownCountOrDeveloperWarning() {
        val visual = DashboardVisualModelBuilder.build(
            DashboardState(
                tethering = TetheringDashboard(
                    state = TetheringState.ACTIVE,
                    clientCount = null,
                    clientVisibility = TetheringClientVisibility.RESTRICTED
                )
            )
        )

        assertEquals("", visual.header.clientCountLabel)
        assertFalse(visual.alerts.any { it.message == "Device list unavailable on this Android build." })
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

        assertEquals("SIM locked", visual.header.simProviderLabel)
        assertTrue(visual.alerts.any { it.message == "Permission denied. Data was not collected." })
    }

    @Test
    fun createsSeparateRecoveryActionsForPhoneStateAndNotificationPermissions() {
        val state = DashboardState(
            network = NetworkStateSnapshot(
                simSlots = listOf(SimSlotSnapshot.permissionDenied())
            ),
            permissions = listOf(
                PermissionStatus("Notifications", "android.permission.POST_NOTIFICATIONS", false),
                PermissionStatus("Phone state", "android.permission.READ_PHONE_STATE", false)
            )
        )

        val visual = DashboardVisualModelBuilder.build(state)

        val phoneAlert = visual.alerts.first { it.action?.permission == "android.permission.READ_PHONE_STATE" }
        assertEquals("Permission denied. Data was not collected.", phoneAlert.message)
        assertEquals("Why TetherWatch needs phone state", phoneAlert.action?.title)
        assertEquals("Allow phone state", phoneAlert.action?.actionLabel)

        val notificationAlert = visual.alerts.first { it.action?.permission == "android.permission.POST_NOTIFICATIONS" }
        assertEquals("Monitoring notification disabled. Tap to review.", notificationAlert.message)
        assertEquals("Why TetherWatch needs notifications", notificationAlert.action?.title)
        assertEquals("Allow notifications", notificationAlert.action?.actionLabel)

        assertFalse(visual.alerts.any {
            it.action?.permission == "android.permission.POST_NOTIFICATIONS" &&
                it.message.contains("Data was not collected")
        })
    }

    @Test
    fun offTetheringShowsZeroClientsInTheHeader() {
        val visual = DashboardVisualModelBuilder.build(
            DashboardState(tethering = TetheringDashboard(state = TetheringState.OFF, clientCount = 0))
        )

        assertEquals("Off", visual.header.tetheringLabel)
        assertEquals("0", visual.header.clientCountLabel)
        assertFalse(visual.header.tetheringActive)
    }

    @Test
    fun pingLossDoesNotCreateDashboardAlert() {
        val visual = DashboardVisualModelBuilder.build(
            DashboardState(
                network = NetworkStateSnapshot(isConnected = true, isValidated = true),
                probe = ProbeDashboard(summary = ProbeSummary(80, 70, 90, 3, 4, 0))
            )
        )

        assertFalse(visual.alerts.any { it.message.startsWith("Ping loss") })
    }
}
