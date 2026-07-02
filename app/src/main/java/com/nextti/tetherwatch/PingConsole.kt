package com.nextti.tetherwatch

import kotlin.math.roundToLong

class PingConsoleBuffer(private val maxLines: Int = PingConsoleLineOptions.MAX_COUNT) {
    private val lines = ArrayDeque<String>()

    fun add(line: String) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return
        lines.addLast(trimmed)
        while (lines.size > maxLines) lines.removeFirst()
    }

    fun clear() {
        lines.clear()
    }

    fun lines(): List<String> = lines.toList()
}

data class ParsedPingOutput(
    val result: ProbeResult,
    val consoleLine: String
)

object PingCommandOutputParser {
    private val timeRegex = Regex("""time[=<]([0-9.]+)\s*ms""")
    private val transientTokenRegex = Regex("""\s+(icmp_seq|ttl)=\S+""")

    fun parse(target: String, exitCode: Int, output: String): ParsedPingOutput {
        val lines = output
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        val successLine = lines.firstOrNull { it.contains("bytes from") && it.contains("time") }
        val latencyMs = successLine
            ?.let { timeRegex.find(it)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.roundToLong() }

        if (exitCode == 0 && latencyMs != null) {
            return ParsedPingOutput(
                result = ProbeResult(target, true, latencyMs, null),
                consoleLine = successLine.removeTransientTokens()
            )
        }

        val errorLine = lines.firstOrNull { it.startsWith("ping:", ignoreCase = true) }
            ?: lines.firstOrNull { it.contains("packet loss", ignoreCase = true) }
            ?: lines.lastOrNull()
            ?: "ping: no response from $target"
        return ParsedPingOutput(
            result = ProbeResult(target, false, null, errorLine),
            consoleLine = errorLine
        )
    }

    private fun String.removeTransientTokens(): String =
        replace(transientTokenRegex, "").replace(":  ", ": ").trim()
}

enum class PingConsoleLineTone {
    NORMAL,
    SLOW,
    ERROR
}

object PingConsoleLineClassifier {
    private val timeRegex = Regex("""time[=<]([0-9.]+)\s*ms""")
    private val errorPatterns = listOf(
        "unreachable",
        "timeout",
        "packet loss",
        "no response",
        "unknown host",
        "name or service not known",
        "network is unreachable",
        "failed"
    )

    fun tone(line: String, slowResponseThresholdMs: Int): PingConsoleLineTone {
        val normalizedLine = line.trim()
        val lowerLine = normalizedLine.lowercase()
        val latencyMs = timeRegex.find(normalizedLine)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()

        if (latencyMs != null && lowerLine.contains("bytes from")) {
            return if (latencyMs >= PingSlowResponseOptions.normalize(slowResponseThresholdMs)) {
                PingConsoleLineTone.SLOW
            } else {
                PingConsoleLineTone.NORMAL
            }
        }
        return if (errorPatterns.any { lowerLine.contains(it) }) {
            PingConsoleLineTone.ERROR
        } else {
            PingConsoleLineTone.NORMAL
        }
    }
}
