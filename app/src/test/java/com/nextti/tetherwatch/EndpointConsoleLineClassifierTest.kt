package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class EndpointConsoleLineClassifierTest {
    @Test
    fun twoHundredAndThreeHundredStatusLinesAreNormalBelowThreshold() {
        assertEquals(
            ConsoleLineTone.NORMAL,
            EndpointConsoleLineClassifier.tone("HEAD https://example.com -> HTTP 204 time=82 ms")
        )
        assertEquals(
            ConsoleLineTone.NORMAL,
            EndpointConsoleLineClassifier.tone("GET https://example.com -> HTTP 302 time=90 ms")
        )
    }

    @Test
    fun authFailuresAreReachabilitySuccess() {
        assertEquals(
            ConsoleLineTone.NORMAL,
            EndpointConsoleLineClassifier.tone("GET https://api.openai.com/v1/models -> AUTH_FAILED HTTP 401 time=120 ms fallback=HEAD")
        )
        assertEquals(
            ConsoleLineTone.NORMAL,
            EndpointConsoleLineClassifier.tone("GET https://api.anthropic.com/v1/models -> AUTH_FAILED HTTP 403 time=1400 ms fallback=HEAD")
        )
    }

    @Test
    fun anyHttpResponseIsReachabilitySuccess() {
        assertEquals(
            ConsoleLineTone.NORMAL,
            EndpointConsoleLineClassifier.tone("GET https://example.com -> HTTP 500 time=40 ms")
        )
    }

    @Test
    fun statusLatencyDoesNotUsePingSlowThreshold() {
        assertEquals(
            ConsoleLineTone.NORMAL,
            EndpointConsoleLineClassifier.tone("HEAD https://example.com -> HTTP 200 time=3000 ms")
        )
    }

    @Test
    fun statusPageIndicatorsDriveLineTone() {
        assertEquals(
            ConsoleLineTone.NORMAL,
            EndpointConsoleLineClassifier.tone("01:02:03 GET claude -> none / All Systems Operational")
        )
        assertEquals(
            ConsoleLineTone.SLOW,
            EndpointConsoleLineClassifier.tone("01:02:04 GET openai -> minor / Partial System Degradation")
        )
        assertEquals(
            ConsoleLineTone.ERROR,
            EndpointConsoleLineClassifier.tone("GET github -> major / Some systems unavailable")
        )
        assertEquals(
            ConsoleLineTone.ERROR,
            EndpointConsoleLineClassifier.tone("GET acme -> critical / Major outage")
        )
    }

    @Test
    fun httpErrorsAndNetworkErrorsAreErrors() {
        listOf(
            "HEAD https://example.com -> DNS_ERROR",
            "HEAD https://example.com -> TLS_ERROR",
            "HEAD https://example.com -> TIMEOUT",
            "HEAD https://example.com -> CONNECTION_REFUSED",
            "HEAD https://example.com -> REDIRECT_LIMIT"
        ).forEach { line ->
            assertEquals(ConsoleLineTone.ERROR, EndpointConsoleLineClassifier.tone(line))
        }
    }
}
