package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimStateReaderTest {
    @Test
    fun marksTheActiveDataSimInADualSimDevice() {
        val slots = SimStateReader.buildSnapshots(
            permissionGranted = true,
            activeDataSubId = 22,
            subscriptions = listOf(
                RawSimSubscription(0, 11, "Carrier A", "SIM 1", "kr"),
                RawSimSubscription(1, 22, "Carrier B", "SIM 2", "id")
            )
        )

        assertEquals(2, slots.size)
        assertEquals(false, slots[0].activeData)
        assertEquals(true, slots[1].activeData)
        assertEquals("Carrier B", slots[1].carrierName)
        assertEquals("ID", slots[1].countryIso)
    }

    @Test
    fun returnsDeniedSnapshotWhenPhoneStatePermissionIsMissing() {
        val slots = SimStateReader.buildSnapshots(
            permissionGranted = false,
            activeDataSubId = 22,
            subscriptions = emptyList()
        )

        assertEquals(1, slots.size)
        assertTrue(slots[0].permissionDenied)
        assertEquals("Permission denied. Data was not collected.", slots[0].carrierName)
    }
}
