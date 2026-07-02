package com.nextti.tetherwatch

import java.util.Locale

data class SimSlotSnapshot(
    val slotIndex: Int,
    val subscriptionId: Int?,
    val carrierName: String,
    val displayName: String,
    val countryIso: String?,
    val activeData: Boolean,
    val permissionDenied: Boolean
) {
    companion object {
        fun permissionDenied(): SimSlotSnapshot = SimSlotSnapshot(
            slotIndex = -1,
            subscriptionId = null,
            carrierName = "Permission denied. Data was not collected.",
            displayName = "SIM",
            countryIso = null,
            activeData = false,
            permissionDenied = true
        )
    }
}

data class RawSimSubscription(
    val slotIndex: Int,
    val subscriptionId: Int,
    val carrierName: String,
    val displayName: String,
    val countryIso: String?
)

data class NetworkCountrySnapshot(
    val countryIso: String? = null,
    val countryLabel: String = "Unknown",
    val confidence: NetworkCountryConfidence = NetworkCountryConfidence.UNKNOWN
)

enum class NetworkCountryConfidence {
    NETWORK_MCC,
    SIM_COUNTRY,
    UNKNOWN
}

data class DnsProfile(
    val address: String,
    val resolverType: DnsResolverType,
    val providerLabel: String,
    val countryLabel: String,
    val confidence: DnsProfileConfidence
)

enum class DnsResolverType {
    ISP,
    PRIVATE,
    PUBLIC_CLOUDFLARE,
    PUBLIC_GOOGLE,
    PUBLIC_QUAD9,
    UNKNOWN
}

enum class DnsProfileConfidence {
    HIGH,
    MEDIUM,
    LOW
}

object NetworkCountryResolver {
    fun resolve(networkCountryIso: String?, simCountryIso: String?): NetworkCountrySnapshot {
        val networkIso = networkCountryIso.normalizedIso()
        if (networkIso != null) {
            return NetworkCountrySnapshot(
                countryIso = networkIso,
                countryLabel = networkIso.countryDisplayName(),
                confidence = NetworkCountryConfidence.NETWORK_MCC
            )
        }

        val simIso = simCountryIso.normalizedIso()
        if (simIso != null) {
            return NetworkCountrySnapshot(
                countryIso = simIso,
                countryLabel = simIso.countryDisplayName(),
                confidence = NetworkCountryConfidence.SIM_COUNTRY
            )
        }

        return NetworkCountrySnapshot()
    }
}

private fun String?.normalizedIso(): String? {
    val value = this?.trim().orEmpty()
    return value.takeIf { it.length == 2 }?.uppercase(Locale.US)
}

fun String.countryDisplayName(): String {
    val countryIso = uppercase(Locale.US)
    return Locale.Builder()
        .setRegion(countryIso)
        .build()
        .getDisplayCountry(Locale.US)
        .ifBlank { countryIso }
}
