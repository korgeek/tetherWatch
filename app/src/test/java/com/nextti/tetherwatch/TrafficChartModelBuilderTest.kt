package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals(listOf("60s", "now"), model.xAxisLabels.map { it.label })
        assertEquals(listOf(0.0, 0.5, 1.0), model.downSeries.y)
        assertEquals(listOf(0.0, 0.1, 0.2), model.upSeries.y)
    }

    @Test
    fun keepsAStableSixtySecondTimelineInsteadOfOnlyTheLastTwelveSamples() {
        val samples = (1..60).map { second ->
            TrafficSamplePoint(
                timestampMs = second * 1_000L,
                rxBps = 125_000.0,
                txBps = 12_500.0,
                rxBytes = 125_000,
                txBytes = 12_500
            )
        }

        val model = TrafficChartModelBuilder.build(samples)

        assertEquals(60, model.downSeries.x.size)
        assertEquals(1.0, model.downSeries.x.first(), 0.001)
        assertEquals(60.0, model.downSeries.x.last(), 0.001)
        assertEquals(listOf("60s", "now"), model.xAxisLabels.map { it.label })
    }

    @Test
    fun usesMinimumDisplayScaleSoTinyTrafficDoesNotFillTheChart() {
        val samples = listOf(
            TrafficSamplePoint(1_000L, rxBps = 1_000.0, txBps = 500.0, rxBytes = 1_000, txBytes = 500),
            TrafficSamplePoint(2_000L, rxBps = 1_200.0, txBps = 400.0, rxBytes = 1_200, txBytes = 400)
        )

        val model = TrafficChartModelBuilder.build(samples)

        assertEquals(0.25, model.yAxisLabels.first().value, 0.001)
        assertEquals("0.25", model.yAxisLabels.first().label)
    }

    @Test
    fun summarizesActiveSecondsSoIdleTrafficIsReadable() {
        val activeSamples = (1..10).map { second ->
            TrafficSamplePoint(
                timestampMs = second * 1_000L,
                rxBps = 10_000.0,
                txBps = 1_000.0,
                rxBytes = 10_000,
                txBytes = 1_000
            )
        }
        val idleSamples = (11..60).map { second ->
            TrafficSamplePoint(
                timestampMs = second * 1_000L,
                rxBps = 0.0,
                txBps = 0.0,
                rxBytes = 0,
                txBytes = 0
            )
        }

        val model = TrafficChartModelBuilder.build(activeSamples + idleSamples)

        assertEquals("active 10s / 60s", model.activityLabel)
    }

    @Test
    fun keepsMbpsAxisLabelsForSmallTrafficRates() {
        val samples = listOf(
            TrafficSamplePoint(1_000L, rxBps = 0.0, txBps = 0.0),
            TrafficSamplePoint(2_000L, rxBps = 12_500.0, txBps = 1_250.0)
        )

        val model = TrafficChartModelBuilder.build(samples)

        assertEquals("Mbps", model.unitLabel)
        assertEquals(listOf("0.25", "0.13", "0.00"), model.yAxisLabels.map { it.label })
        assertEquals(listOf(0.0, 0.1), model.downSeries.y)
        assertEquals(listOf(0.0, 0.01), model.upSeries.y)
    }

    @Test
    fun buildsCumulativeTrafficFractionsForSustainedUsageShape() {
        val samples = listOf(
            TrafficSamplePoint(1_000L, rxBps = 0.0, txBps = 0.0, rxBytes = 100, txBytes = 0),
            TrafficSamplePoint(2_000L, rxBps = 0.0, txBps = 0.0, rxBytes = 100, txBytes = 50),
            TrafficSamplePoint(3_000L, rxBps = 0.0, txBps = 0.0, rxBytes = 200, txBytes = 50)
        )

        val model = TrafficChartModelBuilder.build(samples)

        assertEquals(listOf(0.25, 0.5, 1.0), model.cumulativeDownSeries.y)
        assertEquals(listOf(0.0, 0.5, 1.0), model.cumulativeUpSeries.y)
    }

    @Test
    fun smoothsFlowSoOneSampleSpikeIsLessDominantThanRawRate() {
        val samples = listOf(
            TrafficSamplePoint(1_000L, rxBps = 125_000.0, txBps = 12_500.0),
            TrafficSamplePoint(2_000L, rxBps = 1_250_000.0, txBps = 25_000.0),
            TrafficSamplePoint(3_000L, rxBps = 125_000.0, txBps = 12_500.0)
        )

        val model = TrafficChartModelBuilder.build(samples)

        assertEquals(10.0, model.downSeries.y[1], 0.001)
        assertTrue(model.smoothedDownSeries.y[1] < model.downSeries.y[1])
        assertTrue(model.smoothedDownSeries.y[1] > model.downSeries.y[0])
    }

    @Test
    fun marksDropWhenTrafficFallsSharplyAfterSustainedFlow() {
        val samples = listOf(
            TrafficSamplePoint(1_000L, rxBps = 625_000.0, txBps = 125_000.0),
            TrafficSamplePoint(2_000L, rxBps = 650_000.0, txBps = 120_000.0),
            TrafficSamplePoint(3_000L, rxBps = 600_000.0, txBps = 110_000.0),
            TrafficSamplePoint(4_000L, rxBps = 20_000.0, txBps = 5_000.0)
        )

        val model = TrafficChartModelBuilder.build(samples)

        assertEquals(1, model.dropMarkers.size)
        assertEquals(4.0, model.dropMarkers.first().x, 0.001)
        assertEquals(1, model.dropCount)
        assertTrue(model.flowP10P50Ratio < 0.5)
    }

    @Test
    fun doesNotMarkDropsForIdleOnlyTraffic() {
        val samples = listOf(
            TrafficSamplePoint(1_000L, rxBps = 0.0, txBps = 0.0),
            TrafficSamplePoint(2_000L, rxBps = 100.0, txBps = 100.0),
            TrafficSamplePoint(3_000L, rxBps = 0.0, txBps = 0.0),
            TrafficSamplePoint(4_000L, rxBps = 200.0, txBps = 0.0)
        )

        val model = TrafficChartModelBuilder.build(samples)

        assertEquals(emptyList<TrafficDropMarker>(), model.dropMarkers)
        assertEquals(0, model.dropCount)
        assertEquals(1.0, model.flowP10P50Ratio, 0.001)
    }
}
