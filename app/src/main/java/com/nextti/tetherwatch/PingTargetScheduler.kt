package com.nextti.tetherwatch

class PingTargetScheduler {
    private val nextPingAtMs = mutableMapOf<String, Long>()

    fun dueTargets(targets: List<PingTargetSetting>, nowMs: Long): List<PingTargetSetting> {
        return targets.filter { target ->
            nowMs >= (nextPingAtMs[target.target] ?: 0L)
        }
    }

    fun markPingStarted(target: PingTargetSetting, nowMs: Long) {
        nextPingAtMs[target.target] = nowMs + target.intervalSeconds * 1_000L
    }

    fun clear() {
        nextPingAtMs.clear()
    }
}
