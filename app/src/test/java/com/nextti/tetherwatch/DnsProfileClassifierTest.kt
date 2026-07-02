package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class DnsProfileClassifierTest {
    @Test
    fun classifiesPublicPrivateAndIspResolversWithoutGeoIp() {
        val profiles = DnsProfileClassifier.classify(
            addresses = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9", "192.168.1.1", "202.152.0.2"),
            networkCountryIso = "id"
        )

        assertEquals(DnsResolverType.PUBLIC_CLOUDFLARE, profiles[0].resolverType)
        assertEquals("Cloudflare", profiles[0].providerLabel)
        assertEquals("Global public resolver", profiles[0].countryLabel)
        assertEquals(DnsResolverType.PUBLIC_GOOGLE, profiles[1].resolverType)
        assertEquals(DnsResolverType.PUBLIC_QUAD9, profiles[2].resolverType)
        assertEquals(DnsResolverType.PRIVATE, profiles[3].resolverType)
        assertEquals("Private network", profiles[3].countryLabel)
        assertEquals(DnsResolverType.ISP, profiles[4].resolverType)
        assertEquals("Indonesia", profiles[4].countryLabel)
    }
}
