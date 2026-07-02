package com.nextti.tetherwatch

import kotlin.math.roundToLong

data class ProbeChartModel(
    val series: List<ProbeLatencySeries>,
    val yAxisLabels: List<ProbeAxisLabel>,
    val xAxisLabels: List<ProbeAxisLabel>
)

data class ProbeLatencySeries(
    val label: String,
    val x: List<Double>,
    val y: List<Double>
)

data class ProbeAxisLabel(
    val value: Double,
    val label: String
)

object ProbeChartModelBuilder {
    fun build(points: List<ProbePoint>): ProbeChartModel {
        val latestPoints = points.takeLast(MAX_VISIBLE_POINTS)
        val latencySeries = latestPoints
            .mapIndexedNotNull { index, point ->
                val latency = point.latencyMs
                if (point.success && latency != null) {
                    IndexedProbeLatency(point.label, index.toDouble(), latency.toDouble())
                } else {
                    null
                }
            }
            .groupBy { it.label }
            .map { (label, values) ->
                ProbeLatencySeries(
                    label = label,
                    x = values.map { it.x },
                    y = values.map { it.y }
                )
            }

        val latencies = latencySeries.flatMap { it.y }
        return ProbeChartModel(
            series = latencySeries,
            yAxisLabels = yAxisLabels(latencies),
            xAxisLabels = xAxisLabels(latestPoints.size)
        )
    }

    private fun yAxisLabels(latencies: List<Double>): List<ProbeAxisLabel> {
        if (latencies.isEmpty()) return emptyList()
        val min = latencies.min()
        val max = latencies.max()
        val mid = min + ((max - min) / 2.0)
        return listOf(
            ProbeAxisLabel(max, "${max.roundMs()} ms"),
            ProbeAxisLabel(mid, "${mid.roundMs()} ms"),
            ProbeAxisLabel(min, "${min.roundMs()} ms")
        ).distinctBy { it.label }
    }

    private fun xAxisLabels(pointCount: Int): List<ProbeAxisLabel> {
        val maxX = (pointCount - 1).coerceAtLeast(1).toDouble()
        return listOf(
            ProbeAxisLabel(0.0, "old"),
            ProbeAxisLabel(maxX, "now")
        )
    }

    private fun Double.roundMs(): Long = roundToLong()

    private const val MAX_VISIBLE_POINTS = 8
}

private data class IndexedProbeLatency(
    val label: String,
    val x: Double,
    val y: Double
)
