package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardScoreCalculatorTest {
    @Test
    fun offlineWhenNetworkIsNotValidatedAndAllProbesFail() {
        val score = DashboardScoreCalculator.calculate(
            isConnected = false,
            isValidated = false,
            averageLatencyMs = null,
            packetLossPercent = 100,
            jitterMs = null,
            trafficStability = TrafficStability.IDLE
        )

        assertEquals(0, score.value)
        assertEquals(DashboardHealth.OFFLINE, score.health)
    }

    @Test
    fun excellentWhenNetworkAndProbeAreHealthy() {
        val score = DashboardScoreCalculator.calculate(
            isConnected = true,
            isValidated = true,
            averageLatencyMs = 35,
            packetLossPercent = 0,
            jitterMs = 8,
            trafficStability = TrafficStability.STABLE
        )

        assertEquals(100, score.value)
        assertEquals(DashboardHealth.EXCELLENT, score.health)
    }

    @Test
    fun unstableWhenLatencyAndLossAreHigh() {
        val score = DashboardScoreCalculator.calculate(
            isConnected = true,
            isValidated = true,
            averageLatencyMs = 180,
            packetLossPercent = 8,
            jitterMs = 60,
            trafficStability = TrafficStability.DROPPING
        )

        assertEquals(DashboardHealth.UNSTABLE, score.health)
    }
}
