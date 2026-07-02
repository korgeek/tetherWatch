package com.nextti.tetherwatch

data class DashboardVisualModel(
    val hero: HeroVisualModel,
    val tiles: List<DashboardTileVisualModel>,
    val alerts: List<DashboardAlertVisualModel>
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
    val meterFraction: Float? = null
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
                    label = "SIM",
                    value = simTileValue(activeSim),
                    detail = activeSim?.carrierName ?: "No active SIM",
                    icon = DashboardTileIcon.SIM,
                    tone = if (activeSim?.permissionDenied == true) DashboardTileTone.WARN else DashboardTileTone.NEUTRAL
                ),
                DashboardTileVisualModel(
                    label = "Network",
                    value = country.countryLabel,
                    detail = state.network.transports.firstOrNull() ?: "Offline",
                    icon = DashboardTileIcon.NETWORK,
                    tone = if (state.network.isValidated) DashboardTileTone.GOOD else DashboardTileTone.BAD
                ),
                DashboardTileVisualModel(
                    label = "DNS",
                    value = state.network.dnsProfiles.firstOrNull()?.providerLabel ?: "No DNS",
                    detail = "${state.network.dnsProfiles.size} resolver${if (state.network.dnsProfiles.size == 1) "" else "s"}",
                    icon = DashboardTileIcon.DNS,
                    tone = if (state.network.dnsProfiles.isEmpty()) DashboardTileTone.WARN else DashboardTileTone.NEUTRAL
                ),
                DashboardTileVisualModel(
                    label = "Traffic",
                    value = state.traffic.downloadMbps.metricNumber(),
                    detail = "Down Mbps",
                    icon = DashboardTileIcon.TRAFFIC,
                    tone = if (state.traffic.stability == TrafficStability.DROPPING) DashboardTileTone.WARN else DashboardTileTone.GOOD
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
                    value = tetherValue(state.tethering.value),
                    detail = state.tethering.value,
                    icon = DashboardTileIcon.TETHER,
                    tone = if (state.tethering.value.contains("unavailable", ignoreCase = true)) {
                        DashboardTileTone.WARN
                    } else {
                        DashboardTileTone.NEUTRAL
                    }
                )
            ),
            alerts = buildList {
                if (permissionsDenied) add(DashboardAlertVisualModel("Permission denied. Data was not collected.", DashboardTileTone.WARN))
                if (!state.network.isValidated) add(DashboardAlertVisualModel("Internet validation failed.", DashboardTileTone.BAD))
                if (loss > 0) add(DashboardAlertVisualModel("Probe loss $loss%.", DashboardTileTone.WARN))
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

    private fun simTileValue(sim: SimSlotSnapshot?): String {
        return when {
            sim == null -> "No SIM"
            sim.permissionDenied -> "Denied"
            sim.slotIndex >= 0 -> "SIM ${sim.slotIndex + 1}"
            else -> sim.displayName
        }
    }

    private fun tetherValue(value: String): String {
        return when {
            value.contains("unavailable", ignoreCase = true) -> "Off"
            value.contains("available", ignoreCase = true) -> "Ready"
            else -> "Check"
        }
    }
}

fun String.metricNumber(): String = trim().substringBefore(" ").ifBlank { this }
