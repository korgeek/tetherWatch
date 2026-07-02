package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class EndpointSettingsTest {
    @Test
    fun defaultEndpointTargetsCheckProviderStatusApis() {
        assertEquals(
            listOf(
                EndpointTargetSetting("https://status.claude.com/api/v2/status.json", 30),
                EndpointTargetSetting("https://status.openai.com/api/v2/status.json", 30),
                EndpointTargetSetting("https://www.githubstatus.com/api/v2/status.json", 30),
                EndpointTargetSetting("https://www.cloudflarestatus.com/api/v2/status.json", 30)
            ),
            EndpointSettings().targetSettings
        )
    }

    @Test
    fun endpointTargetsAreTrimmedDeduplicatedLimitedAndEncoded() {
        val settings = listOf(
            EndpointTargetSetting(" https://example.com/health ", 10),
            EndpointTargetSetting("https://example.com/health", 20),
            EndpointTargetSetting("", 5),
            EndpointTargetSetting("https://api.example.com", 255)
        )

        assertEquals(
            listOf(
                EndpointTargetSetting("https://example.com/health", 10),
                EndpointTargetSetting("https://api.example.com", 255)
            ),
            EndpointTargetOptions.normalize(settings)
        )
        assertEquals(
            "https://example.com/health 10\nhttps://api.example.com 255",
            EndpointTargetOptions.encodeSettingsText(settings)
        )
    }

    @Test
    fun legacyEndpointDefaultsMigrateToProviderStatusApis() {
        assertEquals(
            listOf(
                EndpointTargetSetting("https://status.claude.com/api/v2/status.json", 30),
                EndpointTargetSetting("https://status.openai.com/api/v2/status.json", 30),
                EndpointTargetSetting("https://www.githubstatus.com/api/v2/status.json", 30),
                EndpointTargetSetting("https://www.cloudflarestatus.com/api/v2/status.json", 30)
            ),
            EndpointTargetOptions.migrateLegacyDefaultSettings(
                listOf(
                    EndpointTargetSetting("https://api.openai.com/v1/models", 10),
                    EndpointTargetSetting("https://api.anthropic.com/v1/models", 10),
                    EndpointTargetSetting("https://dns.google", 10)
                )
            )
        )
    }

    @Test
    fun customEndpointSettingsAreNotForcedToLlmDefaults() {
        val custom = listOf(EndpointTargetSetting("https://status.example.com", 15))

        assertEquals(custom, EndpointTargetOptions.migrateLegacyDefaultSettings(custom))
    }

    @Test
    fun previousProviderStatusDefaultsMigrateToIncludeGithubAndCloudflareStatus() {
        assertEquals(
            EndpointTargetOptions.DEFAULT_TARGET_SETTINGS,
            EndpointTargetOptions.migrateLegacyDefaultSettings(
                listOf(
                    EndpointTargetSetting("https://status.claude.com/api/v2/status.json", 30),
                    EndpointTargetSetting("https://status.openai.com/api/v2/status.json", 30),
                    EndpointTargetSetting("https://www.githubstatus.com/api/v2/status.json", 30)
                )
            )
        )
    }

    @Test
    fun customEndpointMixedWithLegacyDefaultIsPreserved() {
        val mixed = listOf(
            EndpointTargetSetting("https://api.openai.com/v1/models", 30),
            EndpointTargetSetting("https://status.example.com", 15)
        )

        assertEquals(mixed, EndpointTargetOptions.migrateLegacyDefaultSettings(mixed))
    }

    @Test
    fun statusTimeoutIsCappedAtFiveSecondsAndRedirectsAreDisabled() {
        assertEquals(1_000, EndpointTimeoutOptions.normalize(100))
        assertEquals(5_000, EndpointTimeoutOptions.normalize(5_100))
        assertEquals(5_000, EndpointTimeoutOptions.normalize(20_000))

        assertEquals(0, EndpointRedirectOptions.normalize(-1))
        assertEquals(0, EndpointRedirectOptions.normalize(3))
        assertEquals(0, EndpointRedirectOptions.normalize(30))
    }
}
