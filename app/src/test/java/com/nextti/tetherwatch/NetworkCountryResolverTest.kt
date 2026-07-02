package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkCountryResolverTest {
    @Test
    fun prefersNetworkCountryOverSimCountry() {
        val country = NetworkCountryResolver.resolve(
            networkCountryIso = "id",
            simCountryIso = "kr"
        )

        assertEquals("ID", country.countryIso)
        assertEquals("Indonesia", country.countryLabel)
        assertEquals(NetworkCountryConfidence.NETWORK_MCC, country.confidence)
    }

    @Test
    fun fallsBackToSimCountryWhenNetworkCountryIsMissing() {
        val country = NetworkCountryResolver.resolve(
            networkCountryIso = "",
            simCountryIso = "kr"
        )

        assertEquals("KR", country.countryIso)
        assertEquals("South Korea", country.countryLabel)
        assertEquals(NetworkCountryConfidence.SIM_COUNTRY, country.confidence)
    }
}
