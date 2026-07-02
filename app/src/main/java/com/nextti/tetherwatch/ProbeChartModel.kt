package com.nextti.tetherwatch

data class ProbeChartModel(
    val series: List<ProbeLatencySeries>,
    val failures: List<Boolean>
)

data class ProbeLatencySeries(
    val label: String,
    val x: List<Double>,
    val y: List<Double>
)

object ProbeChartModelBuilder {
    fun build(points: List<ProbePoint>): ProbeChartModel {
        val latencySeries = points
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

        return ProbeChartModel(
            series = latencySeries,
            failures = points.map { !it.success }
        )
    }
}

private data class IndexedProbeLatency(
    val label: String,
    val x: Double,
    val y: Double
)
