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
    val countryLabel: String,
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
    val tone: DashboardTileTone
)

enum class DashboardTileIcon {
    SIM,
    NETWORK,
    DNS,
    TRAFFIC,
    PROBE,
    TETHER
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
        val probeAverage = state.probe.summary.averageLatencyMs
        val loss = state.probe.summary.packetLossPercent
        val permissionsDenied = state.permissions.any { !it.granted } ||
            state.network.simSlots.any { it.permissionDenied }

        return DashboardVisualModel(
            header = DashboardHeaderVisualModel(
                networkTypeLabel = state.network.networkTypeLabel
                    ?: state.network.transports.firstOrNull()
                    ?: "Offline",
                simProviderLabel = simProviderLabel(activeSim),
                countryCode = country.countryIso?.uppercase()?.ifBlank { "--" }?.take(2) ?: "--",
                tetheringLabel = headerTetheringLabel(state.tethering),
                clientCountLabel = state.tethering.clientCount?.toString()
                    ?: if (state.tethering.state == TetheringState.OFF) "0" else "?",
                tetheringActive = state.tethering.state == TetheringState.ACTIVE
            ),
            hero = HeroVisualModel(
                score = state.score.value,
                healthLabel = state.score.health.name.lowercase().replaceFirstChar { it.uppercase() },
                activeSimLabel = activeSimLabel(activeSim),
                countryLabel = country.countryLabel,
                downloadValue = state.traffic.downloadMbps.metricNumber(),
                uploadValue = state.traffic.uploadMbps.metricNumber()
            ),
            tiles = listOf(
                DashboardTileVisualModel(
                    label = "DNS",
                    value = dnsValue(state.network),
                    detail = dnsDetail(state.network),
                    icon = DashboardTileIcon.DNS,
                    tone = if (state.network.dnsProfiles.isEmpty()) DashboardTileTone.WARN else DashboardTileTone.NEUTRAL
                ),
                DashboardTileVisualModel(
                    label = "Probe",
                    value = probeAverage?.let { "$it ms" } ?: "Failing",
                    detail = "Loss $loss%",
                    icon = DashboardTileIcon.PROBE,
                    tone = when {
                        loss >= 25 -> DashboardTileTone.BAD
                        loss > 0 || (probeAverage ?: 0) > 200 -> DashboardTileTone.WARN
                        else -> DashboardTileTone.GOOD
                    },
                    meterFraction = (1f - (loss / 100f)).coerceIn(0f, 1f)
                ),
                DashboardTileVisualModel(
                    label = "Tether",
                    value = tetherValue(state.tethering),
                    detail = tetherDetail(state.tethering),
                    icon = DashboardTileIcon.TETHER,
                    tone = tetherTone(state.tethering)
                )
            ),
            alerts = buildList {
                if (permissionsDenied) add(DashboardAlertVisualModel("Permission denied. Data was not collected.", DashboardTileTone.WARN))
                if (!state.network.isValidated) add(DashboardAlertVisualModel("Internet validation failed.", DashboardTileTone.BAD))
                if (loss > 0) add(DashboardAlertVisualModel("Probe loss $loss%.", DashboardTileTone.WARN))
                if (state.tethering.state == TetheringState.ACTIVE &&
                    state.tethering.clientVisibility == TetheringClientVisibility.RESTRICTED
                ) {
                    add(DashboardAlertVisualModel("Device list unavailable on this Android build.", DashboardTileTone.WARN))
                }
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
        return network.dnsProfiles.firstOrNull()?.address
            ?: network.dnsServers.firstOrNull()
            ?: "No DNS"
    }

    private fun dnsDetail(network: NetworkStateSnapshot): String {
        val addresses = network.dnsProfiles.map { it.address }.ifEmpty { network.dnsServers }
        return when {
            addresses.isEmpty() -> "No resolver"
            addresses.size == 1 -> "1 resolver"
            addresses.size == 2 -> addresses[1]
            else -> "${addresses[1]} · +${addresses.size - 2}"
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

    private fun tetherValue(tethering: TetheringDashboard): String {
        return when (tethering.state) {
            TetheringState.ACTIVE -> "Active"
            TetheringState.OFF -> "Off"
            TetheringState.RESTRICTED -> "Restricted"
            TetheringState.UNAVAILABLE -> "Unavailable"
        }
    }

    private fun tetherDetail(tethering: TetheringDashboard): String {
        return when (tethering.state) {
            TetheringState.ACTIVE -> {
                val type = tethering.types.firstOrNull()?.label() ?: "Tether"
                val downstream = tethering.downstreamInterfaces.firstOrNull()
                when {
                    downstream != null -> "$type / $downstream"
                    tethering.upstreamInterface != null -> "Upstream ${tethering.upstreamInterface}"
                    else -> "Clients: restricted"
                }
            }
            TetheringState.OFF -> tethering.upstreamInterface?.let { "Upstream $it" } ?: "No tethered interface"
            TetheringState.RESTRICTED -> "Clients: restricted"
            TetheringState.UNAVAILABLE -> "Service unavailable"
        }
    }

    private fun tetherTone(tethering: TetheringDashboard): DashboardTileTone {
        return when (tethering.state) {
            TetheringState.ACTIVE -> DashboardTileTone.GOOD
            TetheringState.OFF -> DashboardTileTone.NEUTRAL
            TetheringState.RESTRICTED -> DashboardTileTone.WARN
            TetheringState.UNAVAILABLE -> DashboardTileTone.WARN
        }
    }

    private fun TetheringType.label(): String {
        return when (this) {
            TetheringType.WIFI -> "Wi-Fi"
            TetheringType.USB -> "USB"
            TetheringType.BLUETOOTH -> "Bluetooth"
            TetheringType.OTHER -> "Tether"
        }
    }
}

fun String.metricNumber(): String = trim().substringBefore(" ").ifBlank { this }
