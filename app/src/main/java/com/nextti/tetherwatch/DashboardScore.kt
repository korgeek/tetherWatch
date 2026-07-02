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
    val health: DashboardHealth
)

object DashboardScoreCalculator {
    fun calculate(
        isConnected: Boolean,
        isValidated: Boolean,
        averageLatencyMs: Long?,
        packetLossPercent: Int,
        jitterMs: Long?,
        trafficStability: TrafficStability
    ): DashboardScore {
        if (!isConnected || !isValidated && packetLossPercent == 100) {
            return DashboardScore(0, DashboardHealth.OFFLINE)
        }

        var score = 0
        if (isConnected) score += 20
        if (isValidated) score += 15

        score += when {
            averageLatencyMs == null -> 0
            averageLatencyMs < 50 -> 25
            averageLatencyMs < 100 -> 20
            averageLatencyMs <= 250 -> 10
            else -> 0
        }

        score += when {
            packetLossPercent < 1 -> 20
            packetLossPercent < 3 -> 16
            packetLossPercent <= 10 -> 8
            else -> 0
        }

        score += when {
            jitterMs == null -> 0
            jitterMs < 20 -> 10
            jitterMs < 60 -> 5
            else -> 0
        }

        score += when (trafficStability) {
            TrafficStability.STABLE, TrafficStability.IDLE -> 10
            TrafficStability.BURSTY -> 5
            TrafficStability.DROPPING -> 0
        }

        val value = score.coerceIn(0, 100)
        val health = when (value) {
            0 -> DashboardHealth.OFFLINE
            in 90..100 -> DashboardHealth.EXCELLENT
            in 75..89 -> DashboardHealth.GOOD
            in 50..74 -> DashboardHealth.UNSTABLE
            else -> DashboardHealth.POOR
        }
        return DashboardScore(value, health)
    }
}
