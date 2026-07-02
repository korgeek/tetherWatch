package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficRateFormatterTest {
    @Test
    fun formatsCurrentDownloadAndUploadMbpsForNotification() {
        val text = TrafficRateFormatter.notificationText(
            rxBytesPerSecond = 125_000.0,
            txBytesPerSecond = 25_000.0,
            recentBytes = 42_000L,
            sessionBytes = 1_572_864L
        )

        assertEquals("DN 1.00 Mbps · UP 0.20 Mbps · 60s 41.0 KB · Session 1.5 MB", text)
    }

    @Test
    fun clampsNegativeRatesToZero() {
        val text = TrafficRateFormatter.notificationText(
            rxBytesPerSecond = -1.0,
            txBytesPerSecond = -2.0,
            recentBytes = -1L,
            sessionBytes = -2L
        )

        assertEquals("DN 0.00 Mbps · UP 0.00 Mbps · 60s 0.0 B · Session 0.0 B", text)
    }
}
