package com.nextti.tetherwatch

data class NetworkBadgeModel(
    val label: String,
    val value: String,
    val icon: NetworkIcon,
    val tone: NetworkBadgeTone
)

enum class NetworkIcon {
    CELLULAR,
    WIFI,
    ETHERNET,
    OFFLINE,
    VALIDATION,
    METERED,
    INTERFACE,
    DNS
}

enum class NetworkBadgeTone {
    GOOD,
    WARN,
    BAD,
    NEUTRAL
}

object NetworkBadgeModelBuilder {
    fun build(network: NetworkStateSnapshot): List<NetworkBadgeModel> {
        return listOf(
            connectionBadge(network),
            validationBadge(network),
            meteredBadge(network),
            interfaceBadge(network),
            dnsBadge(network)
        )
    }

    private fun connectionBadge(network: NetworkStateSnapshot): NetworkBadgeModel {
        val transport = network.transports.firstOrNull()
        return NetworkBadgeModel(
            label = "Link",
            value = if (network.isConnected) transport ?: "Other" else "Offline",
            icon = when {
                !network.isConnected -> NetworkIcon.OFFLINE
                transport == "Wi-Fi" -> NetworkIcon.WIFI
                transport == "Ethernet" -> NetworkIcon.ETHERNET
                else -> NetworkIcon.CELLULAR
            },
            tone = if (network.isConnected) NetworkBadgeTone.GOOD else NetworkBadgeTone.BAD
        )
    }

    private fun validationBadge(network: NetworkStateSnapshot): NetworkBadgeModel {
        return NetworkBadgeModel(
            label = "Internet",
            value = if (network.isValidated) "Validated" else "No internet",
            icon = NetworkIcon.VALIDATION,
            tone = if (network.isValidated) NetworkBadgeTone.GOOD else NetworkBadgeTone.BAD
        )
    }

    private fun meteredBadge(network: NetworkStateSnapshot): NetworkBadgeModel {
        return NetworkBadgeModel(
            label = "Plan",
            value = if (network.isMetered) "Metered" else "Unmetered",
            icon = NetworkIcon.METERED,
            tone = if (network.isMetered) NetworkBadgeTone.WARN else NetworkBadgeTone.GOOD
        )
    }

    private fun interfaceBadge(network: NetworkStateSnapshot): NetworkBadgeModel {
        val interfaceName = network.interfaceName
        return NetworkBadgeModel(
            label = "Interface",
            value = interfaceName ?: "No interface",
            icon = NetworkIcon.INTERFACE,
            tone = if (interfaceName == null) NetworkBadgeTone.WARN else NetworkBadgeTone.NEUTRAL
        )
    }

    private fun dnsBadge(network: NetworkStateSnapshot): NetworkBadgeModel {
        val count = network.dnsServers.size
        return NetworkBadgeModel(
            label = "DNS",
            value = if (count == 0) "No DNS" else "$count servers",
            icon = NetworkIcon.DNS,
            tone = if (count == 0) NetworkBadgeTone.WARN else NetworkBadgeTone.NEUTRAL
        )
    }
}
