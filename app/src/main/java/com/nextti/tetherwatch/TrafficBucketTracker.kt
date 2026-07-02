package com.nextti.tetherwatch

import kotlin.math.abs
import kotlin.math.max

enum class TrafficStability {
    STABLE,
    BURSTY,
    DROPPING,
    IDLE
}

data class TrafficBucket(
    val startTimestamp: Long,
    val bucketSizeMs: Long,
    val rxBytes: Long,
    val txBytes: Long,
    val averageRxBps: Double,
    val averageTxBps: Double,
    val peakRxBps: Double,
    val peakTxBps: Double
)

class TrafficBucketTracker(
    private val bucketSizeMs: Long = 60_000L,
    private val maxBuckets: Int = 60
) {
    private val buckets = ArrayDeque<MutableBucket>()
    private val rates = ArrayDeque<Double>()

    fun addSample(timestamp: Long, rxBytes: Long, txBytes: Long, rxBps: Double, txBps: Double) {
        val bucketStart = timestamp - (timestamp % bucketSizeMs)
        val bucket = buckets.lastOrNull()?.takeIf { it.startTimestamp == bucketStart }
            ?: MutableBucket(bucketStart, bucketSizeMs).also {
                buckets.addLast(it)
                while (buckets.size > maxBuckets) buckets.removeFirst()
            }

        bucket.rxBytes += max(0L, rxBytes)
        bucket.txBytes += max(0L, txBytes)
        bucket.samples += 1
        bucket.rxBpsTotal += max(0.0, rxBps)
        bucket.txBpsTotal += max(0.0, txBps)
        bucket.peakRxBps = max(bucket.peakRxBps, rxBps)
        bucket.peakTxBps = max(bucket.peakTxBps, txBps)

        rates.addLast(max(0.0, rxBps + txBps))
        while (rates.size > 300) rates.removeFirst()
    }

    fun buckets(): List<TrafficBucket> = buckets.map { it.toBucket() }

    fun stability(): TrafficStability {
        if (rates.isEmpty() || rates.all { it < IDLE_BPS }) return TrafficStability.IDLE

        val recent = rates.takeLast(minOf(60, rates.size))
        if (recent.size < 5) return TrafficStability.STABLE

        val average = recent.average()
        val peak = recent.maxOrNull() ?: 0.0
        val zeroishCount = recent.count { it < IDLE_BPS }
        val spikeCount = recent.count { it > average * 3 && it > 50_000 }
        val meanDelta = recent.zipWithNext { a, b -> abs(b - a) }.average()

        return when {
            zeroishCount >= recent.size / 3 && peak > 50_000 -> TrafficStability.DROPPING
            spikeCount >= 3 -> TrafficStability.BURSTY
            meanDelta > average && peak > 50_000 -> TrafficStability.BURSTY
            else -> TrafficStability.STABLE
        }
    }

    private class MutableBucket(
        val startTimestamp: Long,
        val bucketSizeMs: Long
    ) {
        var rxBytes: Long = 0
        var txBytes: Long = 0
        var samples: Int = 0
        var rxBpsTotal: Double = 0.0
        var txBpsTotal: Double = 0.0
        var peakRxBps: Double = 0.0
        var peakTxBps: Double = 0.0

        fun toBucket() = TrafficBucket(
            startTimestamp = startTimestamp,
            bucketSizeMs = bucketSizeMs,
            rxBytes = rxBytes,
            txBytes = txBytes,
            averageRxBps = if (samples == 0) 0.0 else rxBpsTotal / samples,
            averageTxBps = if (samples == 0) 0.0 else txBpsTotal / samples,
            peakRxBps = peakRxBps,
            peakTxBps = peakTxBps
        )
    }

    private companion object {
        const val IDLE_BPS = 1_024.0
    }
}
