package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun trafficBucketsRollUpSamples() {
        val tracker = TrafficBucketTracker(bucketSizeMs = 60_000L)

        tracker.addSample(timestamp = 1_000L, rxBytes = 100, txBytes = 20, rxBps = 100.0, txBps = 20.0)
        tracker.addSample(timestamp = 2_000L, rxBytes = 300, txBytes = 40, rxBps = 300.0, txBps = 40.0)

        val bucket = tracker.buckets().single()
        assertEquals(400, bucket.rxBytes)
        assertEquals(60, bucket.txBytes)
        assertEquals(TrafficStability.IDLE, tracker.stability())
    }

    @Test
    fun trafficStabilityDetectsDrops() {
        val tracker = TrafficBucketTracker(bucketSizeMs = 60_000L)
        repeat(10) { index ->
            val bps = if (index % 2 == 0) 100_000.0 else 0.0
            tracker.addSample(timestamp = index * 1_000L, rxBytes = bps.toLong(), txBytes = 0, rxBps = bps, txBps = 0.0)
        }

        assertEquals(TrafficStability.DROPPING, tracker.stability())
    }
}
