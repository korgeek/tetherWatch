package com.nextti.tetherwatch

data class DashboardVisualModel(
    val header: DashboardHeaderVisualModel,
    val hero: HeroVisualModel,
    val tiles: List<DashboardTileVisualModel>,
    val alerts: List<DashboardAlertVisualModel>
)

data class DashboardHeaderVisualModel(
    val networkTypeLabel: String,
    val simProviderLabel: String,
    val countryCode: String,
    val tetheringLabel: String,
    val clientCountLabel: String,
    val tetheringActive: Boolean
)

data class HeroVisualModel(
    val score: Int,
    val healthLabel: String,
    val activeSimLabel: String,
    val downloadValue: String,
    val uploadValue: String
)

data class DashboardTileVisualModel(
    val label: String,
    val value: String,
    val detail: String,
    val icon: DashboardTileIcon,
    val tone: DashboardTileTone,
    val meterFraction: Float? = null,
    val downSeries: List<Double> = emptyList(),
    val upSeries: List<Double> = emptyList()
)

data class DashboardAlertVisualModel(
    val message: String,
    val tone: DashboardTileTone,
    val action: PermissionRecoveryAction? = null
)

data class PermissionRecoveryAction(
    val permission: String,
    val title: String,
    val explanation: String,
    val actionLabel: String
)

enum class DashboardTileIcon {
    SIM,
    NETWORK,
    DNS,
    TRAFFIC,
    PROBE
}

enum class DashboardTileTone {
    GOOD,
    WARN,
    BAD,
    NEUTRAL
}

object DashboardVisualModelBuilder {
    fun build(state: DashboardState): DashboardVisualModel {
        val activeSim = activeSim(state.network)
        val country = state.network.country
        val deniedPermissions = state.permissions.filterNot { it.granted }
        val phoneStateDenied = deniedPermissions.any { it.permission == PHONE_STATE_PERMISSION } ||
            state.network.simSlots.any { it.permissionDenied }

        return DashboardVisualModel(
            header = DashboardHeaderVisualModel(
                networkTypeLabel = headerNetworkTypeLabel(state.network),
                simProviderLabel = headerConnectionDetail(state.network, activeSim),
                countryCode = country.countryIso?.uppercase()?.ifBlank { "--" }?.take(2) ?: "--",
                tetheringLabel = headerTetheringLabel(state.tethering),
                clientCountLabel = state.tethering.clientCount?.toString()
                    ?: if (state.tethering.state == TetheringState.OFF) "0" else "",
                tetheringActive = state.tethering.state == TetheringState.ACTIVE
            ),
            hero = HeroVisualModel(
                score = state.score.value,
                healthLabel = state.score.health.name.lowercase().replaceFirstChar { it.uppercase() },
                activeSimLabel = heroNetworkLabel(state.network, activeSim),
                downloadValue = state.traffic.downloadMbps,
                uploadValue = state.traffic.uploadMbps
            ),
            tiles = listOf(
                DashboardTileVisualModel(
                    label = "DNS",
                    value = dnsValue(state.network),
                    detail = dnsDetail(state.network),
                    icon = DashboardTileIcon.DNS,
                    tone = if (state.network.dnsProfiles.isEmpty()) DashboardTileTone.WARN else DashboardTileTone.NEUTRAL
                )
            ),
            alerts = buildList {
                if (phoneStateDenied) {
                    add(
                        DashboardAlertVisualModel(
                            message = "Permission denied. Data was not collected.",
                            tone = DashboardTileTone.WARN,
                            action = PermissionRecoveryAction(
                                permission = PHONE_STATE_PERMISSION,
                                title = "Why TetherWatch needs phone state",
                                explanation = "Used to read SIM, carrier, and network type for local tethering status. TetherWatch does not collect phone numbers, call logs, contacts, or upload this data.",
                                actionLabel = "Allow phone state"
                            )
                        )
                    )
                }
                if (deniedPermissions.any { it.permission == NOTIFICATION_PERMISSION }) {
                    add(
                        DashboardAlertVisualModel(
                            message = "Monitoring notification disabled. Tap to review.",
                            tone = DashboardTileTone.WARN,
                            action = PermissionRecoveryAction(
                                permission = NOTIFICATION_PERMISSION,
                                title = "Why TetherWatch needs notifications",
                                explanation = "Used to keep monitoring visible while TetherWatch samples network status in the background. This is not used for marketing or remote messages.",
                                actionLabel = "Allow notifications"
                            )
                        )
                    )
                }
                if (!state.network.isValidated) add(DashboardAlertVisualModel("Internet validation failed.", DashboardTileTone.BAD))
            }
        )
    }

    private fun activeSim(network: NetworkStateSnapshot): SimSlotSnapshot? {
        return network.simSlots.firstOrNull { it.activeData } ?: network.simSlots.firstOrNull()
    }

    private fun activeSimLabel(sim: SimSlotSnapshot?): String {
        return when {
            sim == null -> "No SIM"
            sim.permissionDenied -> "SIM locked"
            sim.slotIndex >= 0 -> "SIM ${sim.slotIndex + 1}"
            else -> sim.displayName
        }
    }

    private fun heroNetworkLabel(network: NetworkStateSnapshot, sim: SimSlotSnapshot?): String {
        val address = network.ipAddresses.firstOrNull { it.count { char -> char == '.' } == 3 }
            ?: network.ipAddresses.firstOrNull()
        return address?.let { "IP $it" } ?: activeSimLabel(sim)
    }

    private fun headerNetworkTypeLabel(network: NetworkStateSnapshot): String {
        return when {
            network.transports.any { it.equals("Wi-Fi", ignoreCase = true) } -> "Wi-Fi"
            !network.networkTypeLabel.isNullOrBlank() -> network.networkTypeLabel
            else -> network.transports.firstOrNull() ?: "Offline"
        }
    }

    private fun headerConnectionDetail(network: NetworkStateSnapshot, sim: SimSlotSnapshot?): String {
        return if (network.transports.any { it.equals("Wi-Fi", ignoreCase = true) }) {
            network.interfaceName?.takeIf { it.isNotBlank() } ?: "Connected"
        } else {
            simProviderLabel(sim)
        }
    }

    private fun simProviderLabel(sim: SimSlotSnapshot?): String {
        return when {
            sim == null -> "No SIM"
            sim.permissionDenied -> "SIM locked"
            sim.slotIndex >= 0 -> listOf("SIM ${sim.slotIndex + 1}", sim.carrierName)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            else -> sim.displayName
        }
    }

    private fun dnsValue(network: NetworkStateSnapshot): String {
        return dnsAddresses(network).take(2).takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "No DNS"
    }

    private fun dnsDetail(network: NetworkStateSnapshot): String {
        return ""
    }

    private fun dnsAddresses(network: NetworkStateSnapshot): List<String> {
        val addresses = network.dnsProfiles.map { it.address }.ifEmpty { network.dnsServers }
        return addresses.sortedBy { address ->
            if (address.count { char -> char == '.' } == 3) 0 else 1
        }
    }

    private fun headerTetheringLabel(tethering: TetheringDashboard): String {
        return when (tethering.state) {
            TetheringState.ACTIVE -> "Tethering"
            TetheringState.OFF -> "Off"
            TetheringState.RESTRICTED -> "Restricted"
            TetheringState.UNAVAILABLE -> "Unavailable"
        }
    }

    private const val PHONE_STATE_PERMISSION = "android.permission.READ_PHONE_STATE"
    private const val NOTIFICATION_PERMISSION = "android.permission.POST_NOTIFICATIONS"
}

fun String.metricNumber(): String = trim().substringBefore(" ").ifBlank { this }
