package com.nextti.tetherwatch

import java.util.Locale

data class TrafficChartModel(
    val downSeries: TrafficRateSeries,
    val upSeries: TrafficRateSeries,
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

object TrafficChartModelBuilder {
    fun build(samples: List<TrafficSamplePoint>): TrafficChartModel {
        val latestSamples = samples.takeLast(MAX_VISIBLE_POINTS)
        val maxMbps = latestSamples
            .flatMap { listOf(it.rxBps, it.txBps) }
            .maxOrNull()
            ?.bytesPerSecondToMbps()
            ?: 0.0
        val useKbps = maxMbps < 1.0
        val unit = if (useKbps) "Kbps" else "Mbps"

        val downValues = latestSamples.map { it.rxBps.rateValue(useKbps) }
        val upValues = latestSamples.map { it.txBps.rateValue(useKbps) }
        val xValues = latestSamples.indices.map { it.toDouble() }
        val maxValue = (downValues + upValues).maxOrNull() ?: 0.0
        val axisMax = maxValue.takeIf { it > 0.0 } ?: 1.0

        return TrafficChartModel(
            downSeries = TrafficRateSeries(xValues, downValues),
            upSeries = TrafficRateSeries(xValues, upValues),
            yAxisLabels = yAxisLabels(axisMax, useKbps),
            xAxisLabels = xAxisLabels(latestSamples.size),
            unitLabel = unit
        )
    }

    private fun yAxisLabels(axisMax: Double, useKbps: Boolean): List<TrafficAxisLabel> {
        val mid = axisMax / 2.0
        return listOf(axisMax, mid, 0.0).map { value ->
            TrafficAxisLabel(value, value.formatRateLabel(useKbps))
        }
    }

    private fun xAxisLabels(sampleCount: Int): List<TrafficAxisLabel> {
        val maxX = (sampleCount - 1).coerceAtLeast(1).toDouble()
        return listOf(
            TrafficAxisLabel(0.0, "old"),
            TrafficAxisLabel(maxX, "now")
        )
    }

    private fun Double.rateValue(useKbps: Boolean): Double {
        val mbps = bytesPerSecondToMbps()
        return if (useKbps) mbps * 1_000.0 else mbps
    }

    private fun Double.bytesPerSecondToMbps(): Double = this * 8.0 / 1_000_000.0

    private fun Double.formatRateLabel(useKbps: Boolean): String {
        return if (useKbps) {
            String.format(Locale.US, "%.0f", this)
        } else {
            String.format(Locale.US, "%.2f", this)
        }
    }

    private const val MAX_VISIBLE_POINTS = 12
}
