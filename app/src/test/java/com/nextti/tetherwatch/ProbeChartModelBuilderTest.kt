package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbeChartModelBuilderTest {
    @Test
    fun buildsSparseLatencySeriesAndFailureMarkers() {
        val points = listOf(
            ProbePoint("Cloudflare DNS", 40, true),
            ProbePoint("Google DNS", null, false),
            ProbePoint("Cloudflare DNS", 55, true),
            ProbePoint("Google DNS", 70, true),
            ProbePoint("Cloudflare DNS", null, false)
        )

        val model = ProbeChartModelBuilder.build(points)

        assertEquals(listOf(false, true, false, false, true), model.failures)
        assertEquals(listOf("Cloudflare DNS", "Google DNS"), model.series.map { it.label })
        assertEquals(listOf(0.0, 2.0), model.series[0].x)
        assertEquals(listOf(40.0, 55.0), model.series[0].y)
        assertEquals(listOf(3.0), model.series[1].x)
        assertEquals(listOf(70.0), model.series[1].y)
    }
}
