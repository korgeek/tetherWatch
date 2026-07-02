package com.nextti.tetherwatch

data class ProbeResult(
    val targetId: String,
    val success: Boolean,
    val latencyMs: Long?,
    val errorType: String?
)

data class ProbeSummary(
    val averageLatencyMs: Long?,
    val minLatencyMs: Long?,
    val maxLatencyMs: Long?,
    val packetLossPercent: Int,
    val jitterMs: Long?,
    val consecutiveFailures: Int
)

object ProbeSummaryCalculator {
    fun summarize(results: List<ProbeResult>): ProbeSummary {
        val latencies = results.mapNotNull { it.latencyMs }
        val failures = results.count { !it.success }
        val consecutiveFailures = results.asReversed().takeWhile { !it.success }.size
        val jitter = latencies.zipWithNext { a, b -> kotlin.math.abs(b - a) }

        return ProbeSummary(
            averageLatencyMs = latencies.takeIf { it.isNotEmpty() }?.average()?.toLong(),
            minLatencyMs = latencies.minOrNull(),
            maxLatencyMs = latencies.maxOrNull(),
            packetLossPercent = if (results.isEmpty()) 100 else failures * 100 / results.size,
            jitterMs = jitter.takeIf { it.isNotEmpty() }?.average()?.toLong(),
            consecutiveFailures = consecutiveFailures
        )
    }
}
