package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingCommandOutputParserTest {
    @Test
    fun parsesSuccessfulAndroidPingOutput() {
        val parsed = PingCommandOutputParser.parse(
            target = "1.1.1.1",
            exitCode = 0,
            output = """
                PING 1.1.1.1 (1.1.1.1) 56(84) bytes of data.
                64 bytes from 1.1.1.1: icmp_seq=1 ttl=55 time=64.7 ms

                --- 1.1.1.1 ping statistics ---
                1 packets transmitted, 1 received, 0% packet loss, time 0ms
                rtt min/avg/max/mdev = 64.724/64.724/64.724/0.000 ms
            """.trimIndent()
        )

        assertTrue(parsed.result.success)
        assertEquals(65L, parsed.result.latencyMs)
        assertNull(parsed.result.errorType)
        assertEquals("64 bytes from 1.1.1.1: time=64.7 ms", parsed.consoleLine)
    }

    @Test
    fun keepsFailureTextAsConsoleLine() {
        val parsed = PingCommandOutputParser.parse(
            target = "bad.host",
            exitCode = 1,
            output = "ping: unknown host bad.host"
        )

        assertFalse(parsed.result.success)
        assertNull(parsed.result.latencyMs)
        assertEquals("ping: unknown host bad.host", parsed.result.errorType)
        assertEquals("ping: unknown host bad.host", parsed.consoleLine)
    }
}
