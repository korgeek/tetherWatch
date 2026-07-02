package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class ConsoleLineTimestampFormatterTest {
    @Test
    fun prefixesConsoleLinesWithTwoDigitHourMinuteSecond() {
        val line = ConsoleLineTimestampFormatter.prefix(
            line = "GET openai -> none / All Systems Operational",
            timestampMs = 3_723_000L,
            timeZone = TimeZone.getTimeZone("UTC")
        )

        assertEquals("01:02:03 GET openai -> none / All Systems Operational", line)
    }
}
