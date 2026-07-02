package com.nextti.tetherwatch

import java.util.Locale

object DnsProfileClassifier {
    private val cloudflare = setOf(
        "1.1.1.1",
        "1.0.0.1",
        "2606:4700:4700::1111",
        "2606:4700:4700::1001"
    )
    private val google = setOf(
        "8.8.8.8",
        "8.8.4.4",
        "2001:4860:4860::8888",
        "2001:4860:4860::8844"
    )
    private val quad9 = setOf(
        "9.9.9.9",
        "149.112.112.112",
        "2620:fe::fe",
        "2620:fe::9"
    )

    fun classify(addresses: List<String>, networkCountryIso: String?): List<DnsProfile> {
        return addresses.map { address ->
            val normalized = normalize(address)
            when {
                normalized in cloudflare -> public(address, DnsResolverType.PUBLIC_CLOUDFLARE, "Cloudflare")
                normalized in google -> public(address, DnsResolverType.PUBLIC_GOOGLE, "Google")
                normalized in quad9 -> public(address, DnsResolverType.PUBLIC_QUAD9, "Quad9")
                isPrivateAddress(normalized) -> DnsProfile(
                    address = address,
                    resolverType = DnsResolverType.PRIVATE,
                    providerLabel = "Private DNS",
                    countryLabel = "Private network",
                    confidence = DnsProfileConfidence.HIGH
                )
                normalized.isBlank() -> DnsProfile(
                    address = address,
                    resolverType = DnsResolverType.UNKNOWN,
                    providerLabel = "Unknown DNS",
                    countryLabel = "Unknown",
                    confidence = DnsProfileConfidence.LOW
                )
                else -> DnsProfile(
                    address = address,
                    resolverType = DnsResolverType.ISP,
                    providerLabel = "ISP DNS",
                    countryLabel = networkCountryIso
                        ?.takeIf { it.trim().length == 2 }
                        ?.uppercase(Locale.US)
                        ?.countryDisplayName()
                        ?: "Network country unknown",
                    confidence = DnsProfileConfidence.MEDIUM
                )
            }
        }
    }

    private fun public(address: String, type: DnsResolverType, provider: String): DnsProfile {
        return DnsProfile(
            address = address,
            resolverType = type,
            providerLabel = provider,
            countryLabel = "Global public resolver",
            confidence = DnsProfileConfidence.HIGH
        )
    }

    private fun normalize(address: String): String {
        return address.trim().removePrefix("[").removeSuffix("]").lowercase(Locale.US)
    }

    private fun isPrivateAddress(address: String): Boolean {
        return address == "::1" ||
            address.startsWith("fe80:") ||
            address.startsWith("fc") ||
            address.startsWith("fd") ||
            address.startsWith("10.") ||
            address.startsWith("192.168.") ||
            address.startsWith("172.16.") ||
            address.startsWith("172.17.") ||
            address.startsWith("172.18.") ||
            address.startsWith("172.19.") ||
            address.startsWith("172.20.") ||
            address.startsWith("172.21.") ||
            address.startsWith("172.22.") ||
            address.startsWith("172.23.") ||
            address.startsWith("172.24.") ||
            address.startsWith("172.25.") ||
            address.startsWith("172.26.") ||
            address.startsWith("172.27.") ||
            address.startsWith("172.28.") ||
            address.startsWith("172.29.") ||
            address.startsWith("172.30.") ||
            address.startsWith("172.31.")
    }
}
