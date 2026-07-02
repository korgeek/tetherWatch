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
}
