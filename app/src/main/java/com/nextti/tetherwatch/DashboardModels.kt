package com.nextti.tetherwatch

data class DashboardState(
    val score: DashboardScore = DashboardScore(0, DashboardHealth.OFFLINE),
    val network: NetworkStateSnapshot = NetworkStateSnapshot(),
    val traffic: TrafficDashboard = TrafficDashboard(),
    val probe: ProbeDashboard = ProbeDashboard(),
    val tethering: SimpleStatus = SimpleStatus("Tethering", "Checking..."),
    val signal: SimpleStatus = SimpleStatus("Signal", "Permission denied. Data was not collected.", true),
    val permissions: List<PermissionStatus> = emptyList()
)

data class NetworkStateSnapshot(
    val isConnected: Boolean = false,
    val isValidated: Boolean = false,
    val isMetered: Boolean = false,
    val transports: List<String> = emptyList(),
    val interfaceName: String? = null,
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
    val sessionDown: String = "0.0 B",
    val sessionUp: String = "0.0 B",
    val stability: TrafficStability = TrafficStability.IDLE,
    val buckets: List<TrafficBucket> = emptyList(),
    val todayUsage: String = "Unavailable"
)

data class ProbeDashboard(
    val targets: List<ProbeTargetState> = emptyList(),
    val summary: ProbeSummary = ProbeSummary(null, null, null, 100, null, 0),
    val history: List<ProbePoint> = emptyList()
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
