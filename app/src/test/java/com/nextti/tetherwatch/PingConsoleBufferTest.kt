package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class PingConsoleBufferTest {
    @Test
    fun keepsEnoughRecentLinesForTheLargestConsoleSettingByDefault() {
        val buffer = PingConsoleBuffer()

        (1..40).forEach { buffer.add("line $it") }

        assertEquals(30, buffer.lines().size)
        assertEquals("line 11", buffer.lines().first())
        assertEquals("line 40", buffer.lines().last())
    }

    @Test
    fun ignoresBlankLinesFromPingOutput() {
        val buffer = PingConsoleBuffer()

        buffer.add("")
        buffer.add("   ")
        buffer.add("64 bytes from 1.1.1.1: icmp_seq=1 ttl=55 time=64.7 ms")

        assertEquals(listOf("64 bytes from 1.1.1.1: icmp_seq=1 ttl=55 time=64.7 ms"), buffer.lines())
    }
}
