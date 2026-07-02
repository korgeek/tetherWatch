package com.nextti.tetherwatch

import java.util.Locale

data class TrafficChartModel(
    val downSeries: TrafficRateSeries,
    val upSeries: TrafficRateSeries,
    val smoothedDownSeries: TrafficRateSeries,
    val smoothedUpSeries: TrafficRateSeries,
    val cumulativeDownSeries: TrafficRateSeries,
    val cumulativeUpSeries: TrafficRateSeries,
    val dropMarkers: List<TrafficDropMarker>,
    val flowP10P50Ratio: Double,
    val dropCount: Int,
    val activityLabel: String,
    val yAxisLabels: List<TrafficAxisLabel>,
    val xAxisLabels: List<TrafficAxisLabel>,
    val unitLabel: String
)

data class TrafficRateSeries(
    val x: List<Double>,
    val y: List<Double>
)

data class TrafficAxisLabel(
    val value: Double,
    val label: String
)

data class TrafficDropMarker(
    val x: Double
)

object TrafficChartModelBuilder {
    fun build(samples: List<TrafficSamplePoint>): TrafficChartModel {
        val latestTimestamp = samples.maxOfOrNull { it.timestampMs } ?: 0L
        val windowStart = (latestTimestamp - WINDOW_MS).coerceAtLeast(0L)
        val latestSamples = samples.filter { it.timestampMs in windowStart..latestTimestamp }
        val downValues = latestSamples.map { it.rxBps.bytesPerSecondToMbps() }
        val upValues = latestSamples.map { it.txBps.bytesPerSecondToMbps() }
        val smoothedDownValues = downValues.ewma()
        val smoothedUpValues = upValues.ewma()
        val xValues = latestSamples.map { sample ->
            ((sample.timestampMs - windowStart).coerceIn(0L, WINDOW_MS)).toDouble() / 1_000.0
        }
        val cumulativeDownValues = latestSamples.cumulativeFractions { it.rxBytes }
        val cumulativeUpValues = latestSamples.cumulativeFractions { it.txBytes }
        val totalValues = downValues.zip(upValues) { down, up -> down + up }
        val smoothedTotalValues = smoothedDownValues.zip(smoothedUpValues) { down, up -> down + up }
        val dropMarkers = buildDropMarkers(xValues, totalValues, smoothedTotalValues)
        val flowRatio = p10P50Ratio(totalValues)
        val maxValue = (downValues + upValues + smoothedDownValues + smoothedUpValues).maxOrNull() ?: 0.0
        val axisMax = maxOf(maxValue, MIN_AXIS_Mbps)

        return TrafficChartModel(
            downSeries = TrafficRateSeries(xValues, downValues),
            upSeries = TrafficRateSeries(xValues, upValues),
            smoothedDownSeries = TrafficRateSeries(xValues, smoothedDownValues),
            smoothedUpSeries = TrafficRateSeries(xValues, smoothedUpValues),
            cumulativeDownSeries = TrafficRateSeries(xValues, cumulativeDownValues),
            cumulativeUpSeries = TrafficRateSeries(xValues, cumulativeUpValues),
            dropMarkers = dropMarkers,
            flowP10P50Ratio = flowRatio,
            dropCount = dropMarkers.size,
            activityLabel = activityLabel(latestSamples),
            yAxisLabels = yAxisLabels(axisMax),
            xAxisLabels = xAxisLabels(),
            unitLabel = "Mbps"
        )
    }

    private fun List<TrafficSamplePoint>.cumulativeFractions(bytes: (TrafficSamplePoint) -> Long): List<Double> {
        var total = 0L
        val cumulative = map { sample ->
            total += bytes(sample).coerceAtLeast(0L)
            total
        }
        val max = cumulative.maxOrNull()?.takeIf { it > 0L } ?: return map { 0.0 }
        return cumulative.map { it.toDouble() / max.toDouble() }
    }

    private fun List<Double>.ewma(): List<Double> {
        var previous: Double? = null
        return map { value ->
            val smoothed = previous?.let { (EWMA_ALPHA * value) + ((1.0 - EWMA_ALPHA) * it) } ?: value
            previous = smoothed
            smoothed
        }
    }

    private fun buildDropMarkers(
        xValues: List<Double>,
        totalValues: List<Double>,
        smoothedTotalValues: List<Double>
    ): List<TrafficDropMarker> {
        val median = totalValues.percentile(0.50)
        if (median < ACTIVE_FLOW_Mbps) return emptyList()

        return totalValues.mapIndexedNotNull { index, value ->
            val smoothed = smoothedTotalValues.getOrElse(index) { 0.0 }
            val isSharpDip = smoothed >= ACTIVE_FLOW_Mbps &&
                value <= smoothed * DROP_FRACTION &&
                value <= median * DROP_FRACTION
            if (isSharpDip) TrafficDropMarker(xValues[index]) else null
        }
    }

    private fun p10P50Ratio(values: List<Double>): Double {
        val median = values.percentile(0.50)
        if (median < ACTIVE_FLOW_Mbps) return 1.0
        return (values.percentile(0.10) / median).coerceIn(0.0, 1.0)
    }

    private fun List<Double>.percentile(fraction: Double): Double {
        if (isEmpty()) return 0.0
        val sorted = sorted()
        val index = ((sorted.size - 1) * fraction).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    private fun yAxisLabels(axisMax: Double): List<TrafficAxisLabel> {
        val mid = axisMax / 2.0
        return listOf(axisMax, mid, 0.0).map { value ->
            TrafficAxisLabel(value, value.formatRateLabel())
        }
    }

    private fun activityLabel(samples: List<TrafficSamplePoint>): String {
        val activeSeconds = samples.count { sample ->
            sample.rxBytes + sample.txBytes >= ACTIVE_BYTES || sample.rxBps.bytesPerSecondToMbps() + sample.txBps.bytesPerSecondToMbps() >= ACTIVE_RATE_Mbps
        }
        return if (activeSeconds == 0) {
            "idle ${WINDOW_SECONDS}s"
        } else {
            "active ${activeSeconds}s / ${WINDOW_SECONDS}s"
        }
    }

    private fun xAxisLabels(): List<TrafficAxisLabel> {
        return listOf(
            TrafficAxisLabel(0.0, "60s"),
            TrafficAxisLabel(WINDOW_SECONDS.toDouble(), "now")
        )
    }

    private fun Double.bytesPerSecondToMbps(): Double = this * 8.0 / 1_000_000.0

    private fun Double.formatRateLabel(): String {
        return String.format(Locale.US, "%.2f", this)
    }

    private const val WINDOW_MS = 60_000L
    private const val WINDOW_SECONDS = 60
    private const val EWMA_ALPHA = 0.35
    private const val ACTIVE_FLOW_Mbps = 0.25
    private const val ACTIVE_RATE_Mbps = 0.01
    private const val MIN_AXIS_Mbps = 0.25
    private const val ACTIVE_BYTES = 1_024L
    private const val DROP_FRACTION = 0.35
}
