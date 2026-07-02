package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficChartModelBuilderTest {
    @Test
    fun buildsMbpsAxisLabelsForHeroTrafficChart() {
        val samples = listOf(
            TrafficSamplePoint(1_000L, rxBps = 0.0, txBps = 0.0),
            TrafficSamplePoint(2_000L, rxBps = 62_500.0, txBps = 12_500.0),
            TrafficSamplePoint(3_000L, rxBps = 125_000.0, txBps = 25_000.0)
        )

        val model = TrafficChartModelBuilder.build(samples)

        assertEquals("Mbps", model.unitLabel)
        assertEquals(listOf("1.00", "0.50", "0.00"), model.yAxisLabels.map { it.label })
        assertEquals(listOf("old", "now"), model.xAxisLabels.map { it.label })
        assertEquals(listOf(0.0, 0.5, 1.0), model.downSeries.y)
        assertEquals(listOf(0.0, 0.1, 0.2), model.upSeries.y)
    }

    @Test
    fun usesKbpsAxisLabelsForSmallTrafficRates() {
        val samples = listOf(
            TrafficSamplePoint(1_000L, rxBps = 0.0, txBps = 0.0),
            TrafficSamplePoint(2_000L, rxBps = 12_500.0, txBps = 1_250.0)
        )

        val model = TrafficChartModelBuilder.build(samples)

        assertEquals("Kbps", model.unitLabel)
        assertEquals(listOf("100", "50", "0"), model.yAxisLabels.map { it.label })
        assertEquals(listOf(0.0, 100.0), model.downSeries.y)
        assertEquals(listOf(0.0, 10.0), model.upSeries.y)
    }
}
