package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbeSettingsTest {
    @Test
    fun defaultProbeIntervalIsFiveSeconds() {
        assertEquals(5, ProbeSettings().intervalSeconds)
    }

    @Test
    fun defaultPingTargetsAreCloudflareAndGoogleDns() {
        assertEquals(listOf("1.1.1.1", "8.8.8.8"), ProbeSettings().targets)
        assertEquals(
            listOf(
                PingTargetSetting("1.1.1.1", 5),
                PingTargetSetting("8.8.8.8", 5)
            ),
            ProbeSettings().targetSettings
        )
    }

    @Test
    fun defaultPingConsoleFontSizeIsReadableButCompact() {
        assertEquals(7, ProbeSettings().consoleFontSizeSp)
    }

    @Test
    fun defaultPingConsoleShowsFifteenLinesAndMarksSlowRepliesAtOneSecond() {
        assertEquals(15, ProbeSettings().consoleLineCount)
        assertEquals(1000, ProbeSettings().slowResponseThresholdMs)
    }

    @Test
    fun pingConsoleFontSizeIsClampedToSixThroughTenSp() {
        assertEquals(6, PingConsoleFontOptions.normalize(4))
        assertEquals(8, PingConsoleFontOptions.normalize(8))
        assertEquals(10, PingConsoleFontOptions.normalize(13))
    }

    @Test
    fun pingConsoleLineCountUsesSixThroughThirtyRowsInThreeLineSteps() {
        assertEquals(6, PingConsoleLineOptions.normalize(2))
        assertEquals(6, PingConsoleLineOptions.normalize(6))
        assertEquals(9, PingConsoleLineOptions.normalize(8))
        assertEquals(12, PingConsoleLineOptions.normalize(12))
        assertEquals(15, PingConsoleLineOptions.normalize(15))
        assertEquals(18, PingConsoleLineOptions.normalize(18))
        assertEquals(21, PingConsoleLineOptions.normalize(20))
        assertEquals(24, PingConsoleLineOptions.normalize(24))
        assertEquals(27, PingConsoleLineOptions.normalize(27))
        assertEquals(30, PingConsoleLineOptions.normalize(99))
    }

    @Test
    fun slowPingThresholdIsClampedToOneHundredThroughThreeThousandMs() {
        assertEquals(100, PingSlowResponseOptions.normalize(20))
        assertEquals(1000, PingSlowResponseOptions.normalize(1049))
        assertEquals(1100, PingSlowResponseOptions.normalize(1050))
        assertEquals(3000, PingSlowResponseOptions.normalize(20_000))
    }

    @Test
    fun exposesCompactProbeIntervalOptions() {
        assertEquals(listOf(2, 5, 10, 30), ProbeIntervalOptions.seconds)
        assertEquals("Every 10 sec", ProbeIntervalOptions.label(10))
    }

    @Test
    fun customProbeIntervalIsClampedToOneThroughTwoHundredFiftyFiveSeconds() {
        assertEquals(1, ProbeIntervalOptions.normalize(0))
        assertEquals(7, ProbeIntervalOptions.normalize(7))
        assertEquals(255, ProbeIntervalOptions.normalize(999))
    }

    @Test
    fun pingTargetsAreTrimmedDeduplicatedAndLimited() {
        assertEquals(
            listOf(
                PingTargetSetting("1.1.1.1", 5),
                PingTargetSetting("8.8.8.8", 10),
                PingTargetSetting("google.com", 7),
                PingTargetSetting("extra.test", 9),
                PingTargetSetting("quad9.net", 12),
                PingTargetSetting("opendns.test", 15)
            ),
            PingTargetOptions.normalize(
                listOf(
                    PingTargetSetting(" 1.1.1.1 ", 5),
                    PingTargetSetting("8.8.8.8", 10),
                    PingTargetSetting("", 20),
                    PingTargetSetting("1.1.1.1", 30),
                    PingTargetSetting("google.com", 7),
                    PingTargetSetting("extra.test", 9),
                    PingTargetSetting("quad9.net", 12),
                    PingTargetSetting("opendns.test", 15),
                    PingTargetSetting("overflow.test", 25)
                )
            )
        )
    }

    @Test
    fun blankPingTargetsFallBackToDefaults() {
        assertEquals(
            listOf(PingTargetSetting("1.1.1.1", 5), PingTargetSetting("8.8.8.8", 5)),
            PingTargetOptions.normalize(emptyList())
        )
    }

    @Test
    fun parsesPerTargetIntervalsFromSettingsText() {
        assertEquals(
            listOf(
                PingTargetSetting("1.1.1.1", 3),
                PingTargetSetting("8.8.8.8", 10),
                PingTargetSetting("google.com", 5)
            ),
            PingTargetOptions.parseSettingsText(
                """
                1.1.1.1 3
                8.8.8.8, 10
                google.com
                """.trimIndent()
            )
        )
    }

    @Test
    fun encodesPerTargetIntervalsForStorageAndEditing() {
        assertEquals(
            "1.1.1.1 3\n8.8.8.8 10",
            PingTargetOptions.encodeSettingsText(
                listOf(
                    PingTargetSetting("1.1.1.1", 3),
                    PingTargetSetting("8.8.8.8", 10)
                )
            )
        )
    }
}
