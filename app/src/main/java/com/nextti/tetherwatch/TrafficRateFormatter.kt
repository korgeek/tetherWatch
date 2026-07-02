package com.nextti.tetherwatch

import java.util.Locale
import kotlin.math.max

object TrafficRateFormatter {
    fun notificationText(
        rxBytesPerSecond: Double,
        txBytesPerSecond: Double,
        recentBytes: Long,
        sessionBytes: Long
    ): String {
        return "DN ${rxBytesPerSecond.mbps()} · UP ${txBytesPerSecond.mbps()} · 60s ${recentBytes.bytes()} · Session ${sessionBytes.bytes()}"
    }

    private fun Double.mbps(): String {
        val bitsPerSecond = max(0.0, this) * 8.0
        return String.format(Locale.US, "%.2f Mbps", bitsPerSecond / 1_000_000.0)
    }

    private fun Long.bytes(): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = max(0L, this).toDouble()
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit += 1
        }
        return String.format(Locale.US, "%.1f %s", value, units[unit])
    }
}
