package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class PingTargetSchedulerTest {
    @Test
    fun allTargetsAreDueBeforeTheyHaveBeenPinged() {
        val scheduler = PingTargetScheduler()
        val targets = listOf(
            PingTargetSetting("1.1.1.1", 5),
            PingTargetSetting("8.8.8.8", 10)
        )

        assertEquals(targets, scheduler.dueTargets(targets, nowMs = 1_000L))
    }

    @Test
    fun eachTargetKeepsItsOwnInterval() {
        val scheduler = PingTargetScheduler()
        val cloudflare = PingTargetSetting("1.1.1.1", 5)
        val google = PingTargetSetting("8.8.8.8", 10)
        val targets = listOf(cloudflare, google)

        scheduler.markPingStarted(cloudflare, nowMs = 1_000L)
        scheduler.markPingStarted(google, nowMs = 1_000L)

        assertEquals(emptyList<PingTargetSetting>(), scheduler.dueTargets(targets, nowMs = 5_999L))
        assertEquals(listOf(cloudflare), scheduler.dueTargets(targets, nowMs = 6_000L))
        assertEquals(listOf(cloudflare, google), scheduler.dueTargets(targets, nowMs = 11_000L))
    }
}
