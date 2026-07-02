package com.nextti.tetherwatch

data class DashboardState(
    val score: DashboardScore = DashboardScore(0, DashboardHealth.OFFLINE),
    val network: NetworkStateSnapshot = NetworkStateSnapshot(),
    val traffic: TrafficDashboard = TrafficDashboard(),
    val probe: ProbeDashboard = ProbeDashboard(),
    val endpoint: EndpointDashboard = EndpointDashboard(),
    val tethering: TetheringDashboard = TetheringDashboard(),
    val probeSettings: ProbeSettings = ProbeSettings(),
    val endpointSettings: EndpointSettings = EndpointSettings(),
    val appSettings: AppSettings = AppSettings(),
    val signal: SimpleStatus = SimpleStatus("Signal", "Permission denied. Data was not collected.", true),
    val permissions: List<PermissionStatus> = emptyList()
)

data class AppSettings(
    val autoBackgroundMonitoring: Boolean = false,
    val keepScreenOn: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class NetworkStateSnapshot(
    val isConnected: Boolean = false,
    val isValidated: Boolean = false,
    val isMetered: Boolean = false,
    val transports: List<String> = emptyList(),
    val networkTypeLabel: String? = null,
    val interfaceName: String? = null,
    val ipAddresses: List<String> = emptyList(),
    val dnsServers: List<String> = emptyList(),
    val activeDataSubId: Int? = null,
    val simSlots: List<SimSlotSnapshot> = emptyList(),
    val networkCountryIso: String? = null,
    val networkOperatorName: String? = null,
    val networkOperatorCode: String? = null,
    val dnsProfiles: List<DnsProfile> = emptyList(),
    val country: NetworkCountrySnapshot = NetworkCountrySnapshot()
)

data class TrafficDashboard(
    val downloadMbps: String = "0.00 Mbps",
    val uploadMbps: String = "0.00 Mbps",
    val recentDown: String = "0.0 B",
    val recentUp: String = "0.0 B",
    val recentTotal: String = "0.0 B",
    val sessionDown: String = "0.0 B",
    val sessionUp: String = "0.0 B",
    val sessionTotal: String = "0.0 B",
    val stability: TrafficStability = TrafficStability.IDLE,
    val buckets: List<TrafficBucket> = emptyList(),
    val samples: List<TrafficSamplePoint> = emptyList(),
    val todayUsage: String = "Unavailable"
)

data class TrafficSamplePoint(
    val timestampMs: Long,
    val rxBps: Double,
    val txBps: Double,
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L
)

data class ProbeDashboard(
    val targets: List<ProbeTargetState> = emptyList(),
    val summary: ProbeSummary = ProbeSummary(null, null, null, 100, null, 0),
    val history: List<ProbePoint> = emptyList(),
    val consoleLines: List<String> = emptyList()
)

data class ProbePoint(
    val label: String,
    val latencyMs: Long?,
    val success: Boolean
)

data class ProbeTargetState(
    val label: String,
    val target: String,
    val status: String
)

data class ProbeSettings(
    val targetSettings: List<PingTargetSetting> = PingTargetOptions.DEFAULT_TARGET_SETTINGS,
    val consoleFontSizeSp: Int = PingConsoleFontOptions.DEFAULT_SP,
    val consoleLineCount: Int = PingConsoleLineOptions.DEFAULT_COUNT,
    val slowResponseThresholdMs: Int = PingSlowResponseOptions.DEFAULT_MS
) {
    val intervalSeconds: Int
        get() = targetSettings.minOfOrNull { it.intervalSeconds } ?: ProbeIntervalOptions.DEFAULT_SECONDS

    val targets: List<String>
        get() = targetSettings.map { it.target }
}

data class PingTargetSetting(
    val target: String,
    val intervalSeconds: Int
)

object ProbeIntervalOptions {
    const val DEFAULT_SECONDS = 5
    val seconds = listOf(2, 5, 10, 30)

    fun normalize(value: Int): Int = value.coerceIn(1, 255)

    fun label(value: Int): String = "Every ${normalize(value)} sec"
}

object PingConsoleFontOptions {
    const val MIN_SP = 6
    const val DEFAULT_SP = 7
    const val MAX_SP = 10

    fun normalize(value: Int): Int = value.coerceIn(MIN_SP, MAX_SP)
}

object PingConsoleLineOptions {
    const val MIN_COUNT = 6
    const val DEFAULT_COUNT = 15
    const val MAX_COUNT = 30
    const val STEP_COUNT = 3

    fun normalize(value: Int): Int {
        val snapped = MIN_COUNT + (((value - MIN_COUNT) + STEP_COUNT / 2) / STEP_COUNT) * STEP_COUNT
        return snapped.coerceIn(MIN_COUNT, MAX_COUNT)
    }
}

object PingSlowResponseOptions {
    const val MIN_MS = 100
    const val DEFAULT_MS = 1000
    const val MAX_MS = 3000
    const val STEP_MS = 100

    fun normalize(value: Int): Int {
        val snapped = ((value + STEP_MS / 2) / STEP_MS) * STEP_MS
        return snapped.coerceIn(MIN_MS, MAX_MS)
    }
}

object PingTargetOptions {
    val DEFAULT_TARGET_SETTINGS = listOf(
        PingTargetSetting("1.1.1.1", ProbeIntervalOptions.DEFAULT_SECONDS),
        PingTargetSetting("8.8.8.8", ProbeIntervalOptions.DEFAULT_SECONDS)
    )
    val DEFAULT_TARGETS = DEFAULT_TARGET_SETTINGS.map { it.target }
    private const val MAX_TARGETS = 6

    fun normalize(values: List<PingTargetSetting>): List<PingTargetSetting> {
        val seen = mutableSetOf<String>()
        val normalized = values.asSequence()
            .map { PingTargetSetting(it.target.trim(), ProbeIntervalOptions.normalize(it.intervalSeconds)) }
            .filter { it.target.isNotEmpty() }
            .filter { seen.add(it.target) }
            .take(MAX_TARGETS)
            .toList()
        return normalized.ifEmpty { DEFAULT_TARGET_SETTINGS }
    }

    fun parseSettingsText(text: String, defaultIntervalSeconds: Int = ProbeIntervalOptions.DEFAULT_SECONDS): List<PingTargetSetting> {
        val parsed = text.lineSequence()
            .flatMap { it.split(';').asSequence() }
            .map { line ->
                val parts = line.trim().split(Regex("""[\s,]+""")).filter { it.isNotBlank() }
                val target = parts.getOrNull(0).orEmpty()
                val interval = parts.getOrNull(1)?.toIntOrNull() ?: defaultIntervalSeconds
                PingTargetSetting(target, interval)
            }
            .toList()
        return normalize(parsed)
    }

    fun encodeSettingsText(values: List<PingTargetSetting>): String {
        return normalize(values).joinToString("\n") { "${it.target} ${it.intervalSeconds}" }
    }
}

data class PingTargetInput(
    val target: String,
    val intervalSecondsText: String
)

data class PingSettingsValidationResult(
    val settings: ProbeSettings?,
    val errorMessage: String?
)

object PingSettingsValidator {
    private val integerRegex = Regex("""\d+""")

    fun validate(
        targetInputs: List<PingTargetInput>,
        consoleFontSizeSp: Int,
        consoleLineCount: Int = PingConsoleLineOptions.DEFAULT_COUNT,
        slowResponseThresholdMs: Int = PingSlowResponseOptions.DEFAULT_MS
    ): PingSettingsValidationResult {
        val targets = mutableListOf<PingTargetSetting>()
        val seen = mutableSetOf<String>()
        for (input in targetInputs) {
            val target = input.target.trim()
            if (!isIpv4Address(target)) {
                return PingSettingsValidationResult(null, "IP must be an IPv4 address.")
            }
            val intervalText = input.intervalSecondsText.trim()
            if (!integerRegex.matches(intervalText)) {
                return PingSettingsValidationResult(null, "Interval must be an integer from 1 to 255.")
            }
            val interval = intervalText.toIntOrNull()
            if (interval == null || interval !in 1..255) {
                return PingSettingsValidationResult(null, "Interval must be an integer from 1 to 255.")
            }
            if (seen.add(target)) targets += PingTargetSetting(target, interval)
        }
        if (targets.isEmpty()) {
            return PingSettingsValidationResult(null, "IP must be an IPv4 address.")
        }
        return PingSettingsValidationResult(
            settings = ProbeSettings(
                targetSettings = targets,
                consoleFontSizeSp = PingConsoleFontOptions.normalize(consoleFontSizeSp),
                consoleLineCount = PingConsoleLineOptions.normalize(consoleLineCount),
                slowResponseThresholdMs = PingSlowResponseOptions.normalize(slowResponseThresholdMs)
            ),
            errorMessage = null
        )
    }

    private fun isIpv4Address(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() &&
                integerRegex.matches(part) &&
                (part.length == 1 || !part.startsWith("0")) &&
                part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }
}

data class SimpleStatus(
    val title: String,
    val value: String,
    val permissionDenied: Boolean = false
)

data class PermissionStatus(
    val label: String,
    val permission: String,
    val granted: Boolean
)

data class TetheringDashboard(
    val state: TetheringState = TetheringState.UNAVAILABLE,
    val types: Set<TetheringType> = emptySet(),
    val downstreamInterfaces: List<String> = emptyList(),
    val upstreamInterface: String? = null,
    val lastError: String? = null,
    val clientCount: Int? = null,
    val clientVisibility: TetheringClientVisibility = TetheringClientVisibility.UNKNOWN
)

enum class TetheringState {
    UNAVAILABLE,
    OFF,
    ACTIVE,
    RESTRICTED
}

enum class TetheringType {
    WIFI,
    USB,
    BLUETOOTH,
    OTHER
}

enum class TetheringClientVisibility {
    UNKNOWN,
    RESTRICTED
}
