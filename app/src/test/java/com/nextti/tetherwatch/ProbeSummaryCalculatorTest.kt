package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbeSummaryCalculatorTest {
    @Test
    fun summarizesLatencyLossJitterAndFailures() {
        val results = listOf(
            ProbeResult("cloudflare", true, 40, null),
            ProbeResult("cloudflare", true, 80, null),
            ProbeResult("cloudflare", false, null, "timeout"),
            ProbeResult("cloudflare", true, 50, null),
            ProbeResult("cloudflare", false, null, "timeout")
        )

        val summary = ProbeSummaryCalculator.summarize(results)

        assertEquals(1, summary.consecutiveFailures)
        assertEquals(40L, summary.minLatencyMs)
        assertEquals(80L, summary.maxLatencyMs)
        assertEquals(56L, summary.averageLatencyMs)
        assertEquals(40, summary.packetLossPercent)
        assertEquals(35L, summary.jitterMs)
    }
}
