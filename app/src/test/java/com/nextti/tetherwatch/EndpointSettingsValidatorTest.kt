package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EndpointSettingsValidatorTest {
    @Test
    fun acceptsHttpsStatusJsonIntervalsTimeoutAndRedirectLimit() {
        val result = EndpointSettingsValidator.validate(
            targetInputs = listOf(EndpointTargetInput("https://status.example.com/api/v2/status.json", "30")),
            timeoutMs = 4_000,
            redirectLimit = 3
        )

        assertNull(result.errorMessage)
        assertEquals(
            EndpointSettings(
                targetSettings = listOf(EndpointTargetSetting("https://status.example.com/api/v2/status.json", 30)),
                timeoutMs = 4_000,
                redirectLimit = 0
            ),
            result.settings
        )
    }

    @Test
    fun rejectsNonHttpEndpointUrls() {
        val result = EndpointSettingsValidator.validate(
            targetInputs = listOf(EndpointTargetInput("ftp://example.com", "30")),
            timeoutMs = 4_000,
            redirectLimit = 3
        )

        assertEquals("Endpoint must start with http:// or https://.", result.errorMessage)
        assertNull(result.settings)
    }

    @Test
    fun rejectsNonStatusJsonUrls() {
        val result = EndpointSettingsValidator.validate(
            targetInputs = listOf(EndpointTargetInput("https://example.com/health", "30")),
            timeoutMs = 4_000,
            redirectLimit = 3
        )

        assertEquals("Status URL must end with /status.json.", result.errorMessage)
        assertNull(result.settings)
    }

    @Test
    fun rejectsDecimalIntervalsAndNormalizesSharedLimits() {
        assertEquals(
            "Interval must be an integer from 1 to 255.",
            EndpointSettingsValidator.validate(listOf(EndpointTargetInput("https://example.com/status.json", "1.5")), 4_000, 3).errorMessage
        )
        val result = EndpointSettingsValidator.validate(listOf(EndpointTargetInput("https://example.com/status.json", "5")), 20_000, 30)
        assertNull(result.errorMessage)
        assertEquals(5_000, result.settings?.timeoutMs)
        assertEquals(0, result.settings?.redirectLimit)
    }
}
