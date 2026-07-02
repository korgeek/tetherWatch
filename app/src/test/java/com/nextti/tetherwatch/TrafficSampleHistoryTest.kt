package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficSampleHistoryTest {
    @Test
    fun keepsOnlyRecentFortyFiveSamples() {
        val history = TrafficSampleHistory(maxSamples = 45)

        repeat(50) { index ->
            history.add(
                TrafficSamplePoint(
                    timestampMs = index * 1_000L,
                    rxBps = index.toDouble(),
                    txBps = (index * 2).toDouble()
                )
            )
        }

        val samples = history.samples()
        assertEquals(45, samples.size)
        assertEquals(5_000L, samples.first().timestampMs)
        assertEquals(49_000L, samples.last().timestampMs)
    }

    @Test
    fun sumsRecentByteDeltasForRollingUsageWindow() {
        val history = TrafficSampleHistory(maxSamples = 300)

        history.add(TrafficSamplePoint(timestampMs = 10_000L, rxBps = 0.0, txBps = 0.0, rxBytes = 100, txBytes = 10))
        history.add(TrafficSamplePoint(timestampMs = 65_000L, rxBps = 0.0, txBps = 0.0, rxBytes = 200, txBytes = 20))
        history.add(TrafficSamplePoint(timestampMs = 120_000L, rxBps = 0.0, txBps = 0.0, rxBytes = 300, txBytes = 30))

        val usage = history.usageInLast(timestampMs = 120_000L, windowMs = 60_000L)

        assertEquals(500, usage.rxBytes)
        assertEquals(50, usage.txBytes)
    }
}
