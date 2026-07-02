package com.nextti.tetherwatch

import java.util.Locale
import kotlin.math.max

object TrafficRateFormatter {
    fun notificationText(rxBytesPerSecond: Double, txBytesPerSecond: Double): String {
        return "Down ${rxBytesPerSecond.mbps()} · Up ${txBytesPerSecond.mbps()}"
    }

    private fun Double.mbps(): String {
        val bitsPerSecond = max(0.0, this) * 8.0
        return String.format(Locale.US, "%.2f Mbps", bitsPerSecond / 1_000_000.0)
    }
}
