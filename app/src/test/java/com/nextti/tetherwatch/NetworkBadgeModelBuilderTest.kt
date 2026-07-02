package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkBadgeModelBuilderTest {
    @Test
    fun summarizesHealthyCellularNetworkIntoCompactBadges() {
        val badges = NetworkBadgeModelBuilder.build(
            NetworkStateSnapshot(
                isConnected = true,
                isValidated = true,
                isMetered = true,
                transports = listOf("5G/LTE"),
                interfaceName = "rmnet0",
                dnsServers = listOf("1.1.1.1", "8.8.8.8")
            )
        )

        assertEquals(NetworkIcon.CELLULAR, badges[0].icon)
        assertEquals("5G/LTE", badges[0].value)
        assertEquals(NetworkBadgeTone.GOOD, badges[1].tone)
        assertEquals("Validated", badges[1].value)
        assertEquals(NetworkBadgeTone.WARN, badges[2].tone)
        assertEquals("Metered", badges[2].value)
        assertEquals("rmnet0", badges[3].value)
        assertEquals("2 servers", badges[4].value)
    }

    @Test
    fun summarizesOfflineNetworkWithoutPretendingDetailsExist() {
        val badges = NetworkBadgeModelBuilder.build(NetworkStateSnapshot())

        assertEquals(NetworkIcon.OFFLINE, badges[0].icon)
        assertEquals("Offline", badges[0].value)
        assertEquals(NetworkBadgeTone.BAD, badges[0].tone)
        assertEquals("No internet", badges[1].value)
        assertEquals("No interface", badges[3].value)
        assertEquals("No DNS", badges[4].value)
    }
}
