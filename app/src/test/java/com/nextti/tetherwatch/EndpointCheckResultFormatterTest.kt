package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class EndpointCheckResultFormatterTest {
    @Test
    fun formatsSuccessWithMethodStatusAndLatency() {
        assertEquals(
            "HEAD example.com/health -> HTTP 200 time=42 ms",
            EndpointCheckResultFormatter.consoleLine(
                EndpointCheckResult(
                    url = "https://example.com/health",
                    method = "HEAD",
                    statusCode = 200,
                    latencyMs = 42,
                    errorType = null,
                    usedFallback = false,
                    statusIndicator = null,
                    statusDescription = null
                )
            )
        )
    }

    @Test
    fun formatsStatusIndicatorAndDescriptionBeforeHttpDetails() {
        assertEquals(
            "GET openai -> none / All Systems Operational",
            EndpointCheckResultFormatter.consoleLine(
                EndpointCheckResult(
                    url = "https://status.openai.com/api/v2/status.json",
                    method = "GET",
                    statusCode = 200,
                    latencyMs = 42,
                    errorType = null,
                    usedFallback = false,
                    statusIndicator = "none",
                    statusDescription = "All Systems Operational"
                )
            )
        )
    }

    @Test
    fun derivesServiceNameFromStatusJsonDomains() {
        assertEquals(
            "GET acme -> minor / Partial outage",
            EndpointCheckResultFormatter.consoleLine(
                EndpointCheckResult(
                    url = "https://status.acme.com/api/v2/status.json",
                    method = "GET",
                    statusCode = 200,
                    latencyMs = 42,
                    errorType = null,
                    usedFallback = false,
                    statusIndicator = "minor",
                    statusDescription = "Partial outage"
                )
            )
        )
        assertEquals(
            "GET github -> none / All Systems Operational",
            EndpointCheckResultFormatter.consoleLine(
                EndpointCheckResult(
                    url = "https://www.githubstatus.com/api/v2/status.json",
                    method = "GET",
                    statusCode = 200,
                    latencyMs = 42,
                    errorType = null,
                    usedFallback = false,
                    statusIndicator = "none",
                    statusDescription = "All Systems Operational"
                )
            )
        )
    }

    @Test
    fun formatsFallbackAndNetworkErrors() {
        assertEquals(
            "GET example.com/health -> HTTP 200 time=90 ms fallback=HEAD",
            EndpointCheckResultFormatter.consoleLine(
                EndpointCheckResult(
                    url = "https://example.com/health",
                    method = "GET",
                    statusCode = 200,
                    latencyMs = 90,
                    errorType = null,
                    usedFallback = true,
                    statusIndicator = null,
                    statusDescription = null
                )
            )
        )
        assertEquals(
            "GET api.openai.com/v1/models -> AUTH_FAILED HTTP 401 time=90 ms fallback=HEAD",
            EndpointCheckResultFormatter.consoleLine(
                EndpointCheckResult(
                    url = "https://api.openai.com/v1/models",
                    method = "GET",
                    statusCode = 401,
                    latencyMs = 90,
                    errorType = null,
                    usedFallback = true,
                    statusIndicator = null,
                    statusDescription = null
                )
            )
        )
        assertEquals(
            "HEAD example.com/health -> TIMEOUT time=3000 ms",
            EndpointCheckResultFormatter.consoleLine(
                EndpointCheckResult(
                    url = "https://example.com/health",
                    method = "HEAD",
                    statusCode = null,
                    latencyMs = 3000,
                    errorType = EndpointErrorType.TIMEOUT,
                    usedFallback = false,
                    statusIndicator = null,
                    statusDescription = null
                )
            )
        )
    }
}
