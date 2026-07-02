package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficRateFormatterTest {
    @Test
    fun formatsCurrentDownloadAndUploadMbpsForNotification() {
        val text = TrafficRateFormatter.notificationText(
            rxBytesPerSecond = 125_000.0,
            txBytesPerSecond = 25_000.0
        )

        assertEquals("Down 1.00 Mbps · Up 0.20 Mbps", text)
    }

    @Test
    fun clampsNegativeRatesToZero() {
        val text = TrafficRateFormatter.notificationText(
            rxBytesPerSecond = -1.0,
            txBytesPerSecond = -2.0
        )

        assertEquals("Down 0.00 Mbps · Up 0.00 Mbps", text)
    }
}
