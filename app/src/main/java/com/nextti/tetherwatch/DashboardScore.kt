package com.nextti.tetherwatch

enum class DashboardHealth {
    EXCELLENT,
    GOOD,
    UNSTABLE,
    POOR,
    OFFLINE
}

data class DashboardScore(
    val value: Int,
    val health: DashboardHealth,
    val breakdown: List<ScoreBreakdown> = emptyList()
)

data class ScoreBreakdown(
    val label: String,
    val value: Int,
    val max: Int,
    val enabled: Boolean = true
)

object DashboardScoreCalculator {
    fun calculate(
        isConnected: Boolean,
        isValidated: Boolean,
        averageLatencyMs: Long?,
        packetLossPercent: Int,
        jitterMs: Long?,
        trafficStability: TrafficStability,
        pingMonitoringEnabled: Boolean = true
    ): DashboardScore {
        if (!isConnected || (pingMonitoringEnabled && !isValidated && packetLossPercent == 100)) {
            return DashboardScore(0, DashboardHealth.OFFLINE, emptyBreakdown())
        }

        val linkScore = (if (isConnected) 20 else 0) + (if (isValidated) 15 else 0)

        val latencyScore = when {
            averageLatencyMs == null -> 0
            averageLatencyMs < 50 -> 25
            averageLatencyMs < 100 -> 20
            averageLatencyMs <= 250 -> 10
            else -> 0
        }

        val lossScore = when {
            packetLossPercent < 1 -> 20
            packetLossPercent < 3 -> 16
            packetLossPercent <= 10 -> 8
            else -> 0
        }

        val jitterScore = when {
            jitterMs == null -> 0
            jitterMs < 20 -> 10
            jitterMs < 60 -> 5
            else -> 0
        }

        val flowScore = when (trafficStability) {
            TrafficStability.STABLE, TrafficStability.IDLE -> 10
            TrafficStability.BURSTY -> 5
            TrafficStability.DROPPING -> 0
        }

        val breakdown = listOf(
            ScoreBreakdown("LINK", linkScore, 35),
            ScoreBreakdown("LAT", if (pingMonitoringEnabled) latencyScore else 0, 25, enabled = pingMonitoringEnabled),
            ScoreBreakdown("LOSS", if (pingMonitoringEnabled) lossScore else 0, 20, enabled = pingMonitoringEnabled),
            ScoreBreakdown("JIT", if (pingMonitoringEnabled) jitterScore else 0, 10, enabled = pingMonitoringEnabled),
            ScoreBreakdown("FLOW", flowScore, 10)
        )
        val enabledBreakdown = breakdown.filter { it.enabled }
        val score = enabledBreakdown.sumOf { it.value }
        val maxScore = enabledBreakdown.sumOf { it.max }
        val value = if (maxScore <= 0) {
            0
        } else {
            ((score.toDouble() / maxScore.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        }
        val health = when (value) {
            0 -> DashboardHealth.OFFLINE
            in 90..100 -> DashboardHealth.EXCELLENT
            in 75..89 -> DashboardHealth.GOOD
            in 50..74 -> DashboardHealth.UNSTABLE
            else -> DashboardHealth.POOR
        }
        return DashboardScore(value, health, breakdown)
    }

    private fun emptyBreakdown(): List<ScoreBreakdown> {
        return listOf(
            ScoreBreakdown("LINK", 0, 35),
            ScoreBreakdown("LAT", 0, 25),
            ScoreBreakdown("LOSS", 0, 20),
            ScoreBreakdown("JIT", 0, 10),
            ScoreBreakdown("FLOW", 0, 10)
        )
    }
}
