package com.nextti.tetherwatch

class EndpointTargetScheduler {
    private val nextCheckAtMs = mutableMapOf<String, Long>()

    fun dueTargets(targets: List<EndpointTargetSetting>, nowMs: Long): List<EndpointTargetSetting> {
        return targets.filter { target ->
            nowMs >= (nextCheckAtMs[target.url] ?: 0L)
        }
    }

    fun markCheckStarted(target: EndpointTargetSetting, nowMs: Long) {
        nextCheckAtMs[target.url] = nowMs + target.intervalSeconds * 1_000L
    }

    fun clear() {
        nextCheckAtMs.clear()
    }
}
