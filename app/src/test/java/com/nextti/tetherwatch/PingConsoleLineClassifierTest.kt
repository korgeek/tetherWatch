package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class PingConsoleLineClassifierTest {
    @Test
    fun successfulPingBelowThresholdIsNormal() {
        assertEquals(
            PingConsoleLineTone.NORMAL,
            PingConsoleLineClassifier.tone(
                "64 bytes from 1.1.1.1: icmp_seq=1 ttl=55 time=42.4 ms",
                slowResponseThresholdMs = 1000
            )
        )
    }

    @Test
    fun successfulPingAtOrAboveThresholdIsSlow() {
        assertEquals(
            PingConsoleLineTone.SLOW,
            PingConsoleLineClassifier.tone(
                "64 bytes from 8.8.8.8: icmp_seq=1 ttl=115 time=1000 ms",
                slowResponseThresholdMs = 1000
            )
        )
    }

    @Test
    fun unreachableTimeoutAndPacketLossLinesAreErrors() {
        listOf(
            "ping: unreachable 192.0.2.1",
            "ping: timeout waiting for 192.0.2.1",
            "1 packets transmitted, 0 received, 100% packet loss, time 0ms",
            "Destination Host Unreachable"
        ).forEach { line ->
            assertEquals(PingConsoleLineTone.ERROR, PingConsoleLineClassifier.tone(line, 1000))
        }
    }
}
