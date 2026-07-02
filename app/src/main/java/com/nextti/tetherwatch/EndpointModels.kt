package com.nextti.tetherwatch

import java.net.URI

data class EndpointDashboard(
    val consoleLines: List<String> = emptyList()
)

data class EndpointSettings(
    val targetSettings: List<EndpointTargetSetting> = EndpointTargetOptions.DEFAULT_TARGET_SETTINGS,
    val timeoutMs: Int = EndpointTimeoutOptions.DEFAULT_MS,
    val redirectLimit: Int = EndpointRedirectOptions.DEFAULT_LIMIT
)

data class EndpointTargetSetting(
    val url: String,
    val intervalSeconds: Int
)

object EndpointTargetOptions {
    private const val LEGACY_CLOUDFLARE_ENDPOINT = "https://1.1.1.1"
    private const val LEGACY_OPENAI_MODELS_ENDPOINT = "https://api.openai.com/v1/models"
    private const val LEGACY_ANTHROPIC_MODELS_ENDPOINT = "https://api.anthropic.com/v1/models"
    private const val LEGACY_GOOGLE_DNS_ENDPOINT = "https://dns.google"
    const val CLAUDE_STATUS_ENDPOINT = "https://status.claude.com/api/v2/status.json"
    const val OPENAI_STATUS_ENDPOINT = "https://status.openai.com/api/v2/status.json"
    const val GITHUB_STATUS_ENDPOINT = "https://www.githubstatus.com/api/v2/status.json"
    const val CLOUDFLARE_STATUS_ENDPOINT = "https://www.cloudflarestatus.com/api/v2/status.json"
    val DEFAULT_TARGET_SETTINGS = listOf(
        EndpointTargetSetting(CLAUDE_STATUS_ENDPOINT, 30),
        EndpointTargetSetting(OPENAI_STATUS_ENDPOINT, 30),
        EndpointTargetSetting(GITHUB_STATUS_ENDPOINT, 30),
        EndpointTargetSetting(CLOUDFLARE_STATUS_ENDPOINT, 30)
    )
    private const val MAX_TARGETS = 6

    fun normalize(values: List<EndpointTargetSetting>): List<EndpointTargetSetting> {
        val seen = mutableSetOf<String>()
        val normalized = values.asSequence()
            .map { EndpointTargetSetting(it.url.trim(), ProbeIntervalOptions.normalize(it.intervalSeconds)) }
            .filter { it.url.isNotEmpty() }
            .filter { seen.add(it.url) }
            .take(MAX_TARGETS)
            .toList()
        return normalized.ifEmpty { DEFAULT_TARGET_SETTINGS }
    }

    fun migrateLegacyDefaultSettings(values: List<EndpointTargetSetting>): List<EndpointTargetSetting> {
        val normalized = normalize(values)
        val urls = normalized.map { it.url }.toSet()
        val defaultStatusEndpoints = setOf(
            CLAUDE_STATUS_ENDPOINT,
            OPENAI_STATUS_ENDPOINT,
            GITHUB_STATUS_ENDPOINT,
            CLOUDFLARE_STATUS_ENDPOINT
        )
        val hasAnyStatusEndpoint = urls.any { it in defaultStatusEndpoints }
        val legacyDefaults = setOf(
            LEGACY_CLOUDFLARE_ENDPOINT,
            LEGACY_OPENAI_MODELS_ENDPOINT,
            LEGACY_ANTHROPIC_MODELS_ENDPOINT,
            LEGACY_GOOGLE_DNS_ENDPOINT
        )
        return if (urls.isNotEmpty() && (
                (!hasAnyStatusEndpoint && urls.all { it in legacyDefaults }) ||
                    (hasAnyStatusEndpoint && urls.all { it in defaultStatusEndpoints } && urls != defaultStatusEndpoints)
                )
        ) {
            DEFAULT_TARGET_SETTINGS
        } else {
            normalized
        }
    }

    fun parseSettingsText(text: String, defaultIntervalSeconds: Int = 30): List<EndpointTargetSetting> {
        val parsed = text.lineSequence()
            .flatMap { it.split(';').asSequence() }
            .map { line ->
                val parts = line.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
                val url = parts.getOrNull(0).orEmpty()
                val interval = parts.getOrNull(1)?.toIntOrNull() ?: defaultIntervalSeconds
                EndpointTargetSetting(url, interval)
            }
            .toList()
        return normalize(parsed)
    }

    fun encodeSettingsText(values: List<EndpointTargetSetting>): String {
        return normalize(values).joinToString("\n") { "${it.url} ${it.intervalSeconds}" }
    }
}

object EndpointTimeoutOptions {
    const val MIN_MS = 1_000
    const val DEFAULT_MS = 3_000
    const val MAX_MS = 5_000
    const val STEP_MS = 500

    fun normalize(value: Int): Int {
        val snapped = ((value + STEP_MS / 2) / STEP_MS) * STEP_MS
        return snapped.coerceIn(MIN_MS, MAX_MS)
    }
}

object EndpointRedirectOptions {
    const val MIN_LIMIT = 0
    const val DEFAULT_LIMIT = 0
    const val MAX_LIMIT = 0

    fun normalize(value: Int): Int = 0
}

data class EndpointTargetInput(
    val url: String,
    val intervalSecondsText: String
)

data class EndpointSettingsValidationResult(
    val settings: EndpointSettings?,
    val errorMessage: String?
)

object EndpointSettingsValidator {
    private val integerRegex = Regex("""\d+""")

    fun validate(
        targetInputs: List<EndpointTargetInput>,
        timeoutMs: Int,
        redirectLimit: Int
    ): EndpointSettingsValidationResult {
        val targets = mutableListOf<EndpointTargetSetting>()
        val seen = mutableSetOf<String>()
        for (input in targetInputs) {
            val url = input.url.trim()
            if (!isHttpEndpoint(url)) {
                return EndpointSettingsValidationResult(null, "Endpoint must start with http:// or https://.")
            }
            if (!url.endsWith("/status.json", ignoreCase = true)) {
                return EndpointSettingsValidationResult(null, "Status URL must end with /status.json.")
            }
            val intervalText = input.intervalSecondsText.trim()
            if (!integerRegex.matches(intervalText)) {
                return EndpointSettingsValidationResult(null, "Interval must be an integer from 1 to 255.")
            }
            val interval = intervalText.toIntOrNull()
            if (interval == null || interval !in 1..255) {
                return EndpointSettingsValidationResult(null, "Interval must be an integer from 1 to 255.")
            }
            if (seen.add(url)) targets += EndpointTargetSetting(url, interval)
        }
        if (targets.isEmpty()) {
            return EndpointSettingsValidationResult(null, "Endpoint must start with http:// or https://.")
        }
        return EndpointSettingsValidationResult(
            settings = EndpointSettings(
                targetSettings = EndpointTargetOptions.normalize(targets),
                timeoutMs = EndpointTimeoutOptions.normalize(timeoutMs),
                redirectLimit = EndpointRedirectOptions.normalize(redirectLimit)
            ),
            errorMessage = null
        )
    }

    private fun isHttpEndpoint(value: String): Boolean {
        return runCatching {
            val uri = URI(value)
            (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }
}

enum class EndpointErrorType {
    TLS_ERROR,
    DNS_ERROR,
    TIMEOUT,
    CONNECTION_REFUSED,
    REDIRECT_LIMIT,
    HTTP_ERROR,
    NETWORK_ERROR
}

data class EndpointCheckResult(
    val url: String,
    val method: String,
    val statusCode: Int?,
    val latencyMs: Long,
    val errorType: EndpointErrorType?,
    val usedFallback: Boolean,
    val statusIndicator: String? = null,
    val statusDescription: String? = null
)

object EndpointCheckResultFormatter {
    fun consoleLine(result: EndpointCheckResult): String {
        if (result.statusIndicator != null || result.statusDescription != null) {
            val indicator = result.statusIndicator ?: "unknown"
            val description = result.statusDescription ?: "No description"
            return "${result.method} ${result.url.compactEndpointLabel()} -> $indicator / $description"
        }
        val status = result.statusCode?.let { statusCode ->
            if (statusCode == 401 || statusCode == 403) {
                "AUTH_FAILED HTTP $statusCode"
            } else {
                "HTTP $statusCode"
            }
        } ?: result.errorType?.name ?: "NETWORK_ERROR"
        val fallback = if (result.usedFallback) " fallback=HEAD" else ""
        return "${result.method} ${result.url.compactEndpointLabel()} -> $status time=${result.latencyMs} ms$fallback"
    }

    private fun String.compactEndpointLabel(): String {
        if (endsWith("/status.json", ignoreCase = true)) {
            statusServiceName()?.let { return it }
        }
        return removePrefix("https://").removePrefix("http://")
    }

    private fun String.statusServiceName(): String? {
        val host = runCatching { URI(this).host }.getOrNull()?.removePrefix("www.") ?: return null
        val labels = host.split('.').filter { it.isNotBlank() }
        return when {
            host == "githubstatus.com" -> "github"
            labels.firstOrNull() == "status" && labels.size >= 2 -> labels[1]
            host.endsWith("statuspage.io") && labels.isNotEmpty() -> labels.first()
            labels.firstOrNull()?.endsWith("status") == true -> labels.first().removeSuffix("status")
            else -> labels.firstOrNull()
        }?.takeIf { it.isNotBlank() }
    }
}

enum class ConsoleLineTone {
    NORMAL,
    SLOW,
    ERROR
}

object EndpointConsoleLineClassifier {
    private val statusIndicatorRegex = Regex("""->\s*([A-Za-z_ -]+)\s*/""")
    private val errorPatterns = EndpointErrorType.entries
        .filter { it != EndpointErrorType.HTTP_ERROR }
        .map { it.name }

    fun tone(line: String): ConsoleLineTone {
        val normalizedLine = line.trim()
        if (errorPatterns.any { normalizedLine.contains(it, ignoreCase = true) }) return ConsoleLineTone.ERROR

        statusIndicatorRegex.find(normalizedLine)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.lowercase()
            ?.let { indicator ->
                return when (indicator) {
                    "none" -> ConsoleLineTone.NORMAL
                    "major", "critical", "error", "major_outage" -> ConsoleLineTone.ERROR
                    else -> ConsoleLineTone.SLOW
                }
            }

        return ConsoleLineTone.NORMAL
    }
}
