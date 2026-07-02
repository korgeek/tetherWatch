package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PingSettingsValidatorTest {
    @Test
    fun acceptsIpv4TargetsAndIntervalsFromOneThroughTwoHundredFiftyFive() {
        val result = PingSettingsValidator.validate(
            targetInputs = listOf(
                PingTargetInput("1.1.1.1", "1"),
                PingTargetInput("8.8.8.8", "255")
            ),
            consoleFontSizeSp = 7,
            consoleLineCount = 21,
            slowResponseThresholdMs = 900
        )

        assertNull(result.errorMessage)
        assertEquals(
            ProbeSettings(
                targetSettings = listOf(
                    PingTargetSetting("1.1.1.1", 1),
                    PingTargetSetting("8.8.8.8", 255)
                ),
                consoleFontSizeSp = 7,
                consoleLineCount = 21,
                slowResponseThresholdMs = 900
            ),
            result.settings
        )
    }

    @Test
    fun rejectsTargetsThatAreNotIpv4Addresses() {
        val result = PingSettingsValidator.validate(
            targetInputs = listOf(PingTargetInput("google.com", "5")),
            consoleFontSizeSp = 7,
            consoleLineCount = 15,
            slowResponseThresholdMs = 1000
        )

        assertEquals("IP must be an IPv4 address.", result.errorMessage)
        assertNull(result.settings)
    }

    @Test
    fun rejectsDecimalOrOutOfRangeIntervals() {
        assertEquals(
            "Interval must be an integer from 1 to 255.",
            PingSettingsValidator.validate(listOf(PingTargetInput("1.1.1.1", "1.5")), 7).errorMessage
        )
        assertEquals(
            "Interval must be an integer from 1 to 255.",
            PingSettingsValidator.validate(listOf(PingTargetInput("1.1.1.1", "0")), 7).errorMessage
        )
        assertEquals(
            "Interval must be an integer from 1 to 255.",
            PingSettingsValidator.validate(listOf(PingTargetInput("1.1.1.1", "256")), 7).errorMessage
        )
    }

    @Test
    fun normalizesConsoleLineCountAndSlowResponseThreshold() {
        val result = PingSettingsValidator.validate(
            targetInputs = listOf(PingTargetInput("1.1.1.1", "5")),
            consoleFontSizeSp = 7,
            consoleLineCount = 99,
            slowResponseThresholdMs = 20_000
        )

        assertNull(result.errorMessage)
        assertEquals(30, result.settings?.consoleLineCount)
        assertEquals(3000, result.settings?.slowResponseThresholdMs)
    }
}
