package com.nextti.tetherwatch

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.URL
import javax.net.ssl.SSLException

class EndpointHttpCheckerTest {
    @Test
    fun statusApiTargetsUseGetAndParseStatusIndicatorAndDescription() {
        val checker = EndpointHttpChecker(
            clock = FakeClock(),
            connectionFactory = FakeEndpointConnectionFactory(
                FakeEndpointResponse(
                    url = "https://status.openai.com/api/v2/status.json",
                    method = "GET",
                    statusCode = 200,
                    body = """{"page":{"id":"abc"},"status":{"indicator":"none","description":"All Systems Operational"}}"""
                )
            )
        )

        val result = checker.check(
            EndpointTargetSetting("https://status.openai.com/api/v2/status.json", 30),
            EndpointSettings(timeoutMs = 3_000, redirectLimit = 3)
        )

        assertEquals("GET", result.method)
        assertEquals(200, result.statusCode)
        assertEquals("none", result.statusIndicator)
        assertEquals("All Systems Operational", result.statusDescription)
        assertEquals(false, result.usedFallback)
        assertEquals(null, result.errorType)
    }

    @Test
    fun fallsBackToGetWhenHeadReturnsMethodNotAllowed() {
        val checker = EndpointHttpChecker(
            clock = FakeClock(),
            connectionFactory = FakeEndpointConnectionFactory(
                FakeEndpointResponse("https://example.com/health", "HEAD", 405),
                FakeEndpointResponse("https://example.com/health", "GET", 200)
            )
        )

        val result = checker.check(
            EndpointTargetSetting("https://example.com/health", 30),
            EndpointSettings(timeoutMs = 3_000, redirectLimit = 3)
        )

        assertEquals("GET", result.method)
        assertEquals(200, result.statusCode)
        assertEquals(true, result.usedFallback)
        assertEquals(null, result.errorType)
    }

    @Test
    fun reportsRedirectLimitBecauseStatusRedirectsAreDisabled() {
        val checker = EndpointHttpChecker(
            clock = FakeClock(),
            connectionFactory = FakeEndpointConnectionFactory(
                FakeEndpointResponse("https://example.com/status.json", "GET", 302, "https://edge.example.com/status.json")
            )
        )

        val result = checker.check(
            EndpointTargetSetting("https://example.com/status.json", 30),
            EndpointSettings(timeoutMs = 3_000, redirectLimit = 3)
        )

        assertEquals("GET", result.method)
        assertEquals(null, result.statusCode)
        assertEquals(EndpointErrorType.REDIRECT_LIMIT, result.errorType)
    }

    @Test
    fun treatsUnauthorizedAndForbiddenAsReachabilitySuccess() {
        val checker = EndpointHttpChecker(
            clock = FakeClock(),
            connectionFactory = FakeEndpointConnectionFactory(
                FakeEndpointResponse("https://api.openai.com/v1/models", "HEAD", 401),
                FakeEndpointResponse("https://api.anthropic.com/v1/models", "HEAD", 403)
            )
        )

        val openAi = checker.check(
            EndpointTargetSetting("https://api.openai.com/v1/models", 30),
            EndpointSettings(timeoutMs = 3_000, redirectLimit = 3)
        )
        val anthropic = checker.check(
            EndpointTargetSetting("https://api.anthropic.com/v1/models", 30),
            EndpointSettings(timeoutMs = 3_000, redirectLimit = 3)
        )

        assertEquals("HEAD", openAi.method)
        assertEquals(401, openAi.statusCode)
        assertEquals(null, openAi.errorType)
        assertEquals("HEAD", anthropic.method)
        assertEquals(403, anthropic.statusCode)
        assertEquals(null, anthropic.errorType)
    }

    @Test
    fun fallsBackToGetForNonReachabilityNormalHeadStatus() {
        val checker = EndpointHttpChecker(
            clock = FakeClock(),
            connectionFactory = FakeEndpointConnectionFactory(
                FakeEndpointResponse("https://example.com/problem", "HEAD", 500),
                FakeEndpointResponse("https://example.com/problem", "GET", 500)
            )
        )

        val result = checker.check(
            EndpointTargetSetting("https://example.com/problem", 30),
            EndpointSettings(timeoutMs = 3_000, redirectLimit = 3)
        )

        assertEquals("GET", result.method)
        assertEquals(500, result.statusCode)
        assertEquals(null, result.errorType)
        assertEquals(true, result.usedFallback)
    }

    @Test
    fun reportsRedirectLimitWhenTooManyRedirects() {
        val checker = EndpointHttpChecker(
            clock = FakeClock(),
            connectionFactory = FakeEndpointConnectionFactory(
                FakeEndpointResponse("https://example.com", "HEAD", 302, "https://edge.example.com")
            )
        )

        val result = checker.check(
            EndpointTargetSetting("https://example.com", 30),
            EndpointSettings(timeoutMs = 3_000, redirectLimit = 0)
        )

        assertEquals(EndpointErrorType.REDIRECT_LIMIT, result.errorType)
        assertEquals(null, result.statusCode)
    }

    @Test
    fun mapsTlsDnsTimeoutAndRefusedErrors() {
        val cases = listOf(
            SSLException("bad cert") to EndpointErrorType.TLS_ERROR,
            java.net.UnknownHostException("missing.test") to EndpointErrorType.DNS_ERROR,
            SocketTimeoutException("timeout") to EndpointErrorType.TIMEOUT,
            java.net.ConnectException("Connection refused") to EndpointErrorType.CONNECTION_REFUSED
        )

        cases.forEach { (error, expected) ->
            val checker = EndpointHttpChecker(
                clock = FakeClock(),
                connectionFactory = ThrowingEndpointConnectionFactory(error)
            )
            val result = checker.check(
                EndpointTargetSetting("https://example.com", 30),
                EndpointSettings(timeoutMs = 3_000, redirectLimit = 3)
            )

            assertEquals(expected, result.errorType)
        }
    }
}

private class FakeClock : EndpointClock {
    private var now = 0L
    override fun nowMs(): Long {
        now += 50
        return now
    }
}

private data class FakeEndpointResponse(
    val url: String,
    val method: String,
    val statusCode: Int,
    val location: String? = null,
    val body: String = ""
)

private class FakeEndpointConnectionFactory(
    private vararg val responses: FakeEndpointResponse
) : EndpointConnectionFactory {
    override fun open(url: URL, method: String, timeoutMs: Int): EndpointConnection {
        val response = responses.firstOrNull { it.url == url.toString() && it.method == method }
            ?: error("No fake response for $method $url")
        return object : EndpointConnection {
            override val statusCode: Int = response.statusCode
            override val location: String? = response.location
            override fun bodyText(): String = response.body
            override fun close() = Unit
        }
    }
}

private class ThrowingEndpointConnectionFactory(
    private val error: Exception
) : EndpointConnectionFactory {
    override fun open(url: URL, method: String, timeoutMs: Int): EndpointConnection {
        throw error
    }
}
