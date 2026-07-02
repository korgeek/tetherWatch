package com.nextti.tetherwatch

import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

interface EndpointClock {
    fun nowMs(): Long
}

object SystemEndpointClock : EndpointClock {
    override fun nowMs(): Long = System.currentTimeMillis()
}

interface EndpointConnection {
    val statusCode: Int
    val location: String?
    fun bodyText(): String
    fun close()
}

interface EndpointConnectionFactory {
    fun open(url: URL, method: String, timeoutMs: Int): EndpointConnection
}

object HttpUrlEndpointConnectionFactory : EndpointConnectionFactory {
    override fun open(url: URL, method: String, timeoutMs: Int): EndpointConnection {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = false
            useCaches = false
        }
        return object : EndpointConnection {
            override val statusCode: Int
                get() = connection.responseCode
            override val location: String?
                get() = connection.getHeaderField("Location")
            override fun bodyText(): String {
                val stream = runCatching { connection.inputStream }.getOrElse { connection.errorStream }
                return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            override fun close() {
                connection.disconnect()
            }
        }
    }
}

class EndpointHttpChecker(
    private val clock: EndpointClock = SystemEndpointClock,
    private val connectionFactory: EndpointConnectionFactory = HttpUrlEndpointConnectionFactory
) {
    fun check(target: EndpointTargetSetting, settings: EndpointSettings): EndpointCheckResult {
        val timeoutMs = EndpointTimeoutOptions.normalize(settings.timeoutMs)
        val redirectLimit = EndpointRedirectOptions.normalize(settings.redirectLimit)
        return runCatching {
            val url = URL(target.url)
            if (target.url.isStatusApiEndpoint()) {
                request(
                    url = url,
                    method = "GET",
                    timeoutMs = timeoutMs,
                    redirectLimit = redirectLimit,
                    usedFallback = false,
                    parseStatusIndicator = true
                )
            } else {
                val head = request(
                    url = url,
                    method = "HEAD",
                    timeoutMs = timeoutMs,
                    redirectLimit = redirectLimit,
                    usedFallback = false
                )
                if (head.statusCode != null && head.statusCode.isReachabilityNormalHeadStatus()) {
                    head
                } else if (head.errorType != null && head.errorType != EndpointErrorType.HTTP_ERROR) {
                    head
                } else {
                    request(
                        url = url,
                        method = "GET",
                        timeoutMs = timeoutMs,
                        redirectLimit = redirectLimit,
                        usedFallback = true
                    )
                }
            }
        }.getOrElse { error ->
            EndpointCheckResult(
                url = target.url,
                method = "HEAD",
                statusCode = null,
                latencyMs = timeoutMs.toLong(),
                errorType = error.toEndpointErrorType(),
                usedFallback = false
            )
        }
    }

    private fun request(
        url: URL,
        method: String,
        timeoutMs: Int,
        redirectLimit: Int,
        usedFallback: Boolean,
        parseStatusIndicator: Boolean = false
    ): EndpointCheckResult {
        var currentUrl = url
        val startedAt = clock.nowMs()
        repeat(redirectLimit + 1) { redirectCount ->
            val connection = connectionFactory.open(currentUrl, method, timeoutMs)
            try {
                val status = connection.statusCode
                if (status.isRedirectStatus()) {
                    val location = connection.location
                    if (redirectCount >= redirectLimit || location.isNullOrBlank()) {
                        return EndpointCheckResult(
                            url = url.toString(),
                            method = method,
                            statusCode = null,
                            latencyMs = elapsedSince(startedAt),
                            errorType = EndpointErrorType.REDIRECT_LIMIT,
                            usedFallback = usedFallback
                        )
                    }
                    currentUrl = URL(currentUrl, location)
                } else {
                    val statusPage = if (parseStatusIndicator) {
                        StatusPageIndicatorParser.parse(connection.bodyText())
                    } else {
                        StatusPageStatus(null, null)
                    }
                    return EndpointCheckResult(
                        url = url.toString(),
                        method = method,
                        statusCode = status,
                        latencyMs = elapsedSince(startedAt),
                        errorType = null,
                        usedFallback = usedFallback,
                        statusIndicator = statusPage.indicator,
                        statusDescription = statusPage.description
                    )
                }
            } finally {
                connection.close()
            }
        }
        return EndpointCheckResult(
            url = url.toString(),
            method = method,
            statusCode = null,
            latencyMs = elapsedSince(startedAt),
            errorType = EndpointErrorType.REDIRECT_LIMIT,
            usedFallback = usedFallback
        )
    }

    private fun elapsedSince(startedAt: Long): Long {
        return (clock.nowMs() - startedAt).coerceAtLeast(0L)
    }

    private fun Int.isRedirectStatus(): Boolean = this in listOf(301, 302, 303, 307, 308)

    private fun Int.isReachabilityNormalHeadStatus(): Boolean = this in 200..399 || this == 401 || this == 403

    private fun String.isStatusApiEndpoint(): Boolean {
        return endsWith("/status.json", ignoreCase = true)
    }

    private fun Throwable.toEndpointErrorType(): EndpointErrorType {
        return when (this) {
            is SSLException -> EndpointErrorType.TLS_ERROR
            is UnknownHostException -> EndpointErrorType.DNS_ERROR
            is SocketTimeoutException -> EndpointErrorType.TIMEOUT
            is ConnectException -> {
                if (message?.contains("refused", ignoreCase = true) == true) {
                    EndpointErrorType.CONNECTION_REFUSED
                } else {
                    EndpointErrorType.NETWORK_ERROR
                }
            }
            else -> EndpointErrorType.NETWORK_ERROR
        }
    }
}

data class StatusPageStatus(
    val indicator: String?,
    val description: String?
)

object StatusPageIndicatorParser {
    private val statusObjectRegex = Regex(""""status"\s*:\s*\{([^}]*)\}""", RegexOption.DOT_MATCHES_ALL)
    private val fieldRegex = Regex(""""([^"]+)"\s*:\s*"([^"]*)"""")

    fun parse(body: String): StatusPageStatus {
        return StatusPageStatus(
            indicator = fieldValue(body, "indicator"),
            description = fieldValue(body, "description")
        )
    }

    private fun fieldValue(body: String, fieldName: String): String? {
        val statusObject = statusObjectRegex.find(body)?.groupValues?.getOrNull(1) ?: return null
        return fieldRegex.findAll(statusObject)
            .firstOrNull { it.groupValues.getOrNull(1) == fieldName }
            ?.groupValues
            ?.getOrNull(2)
            ?.takeIf { it.isNotBlank() }
    }
}
