package com.nextti.tetherwatch

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.Locale
import kotlin.math.max

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val trafficTracker = TrafficBucketTracker()
    private val trafficSampleHistory = TrafficSampleHistory()
    private val probeResults = ArrayDeque<ProbeResult>()
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var initialRxBytes = TrafficStats.getTotalRxBytes()
    private var initialTxBytes = TrafficStats.getTotalTxBytes()
    private var previousRxBytes = initialRxBytes
    private var previousTxBytes = initialTxBytes
    private var previousTimestamp = System.currentTimeMillis()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshNow()
        override fun onLost(network: Network) = refreshNow()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = refreshNow()
        override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) = refreshNow()
    }
    private val tetheringMonitor = TetheringMonitor(app, connectivity) {
        refreshNow()
    }

    init {
        connectivity.registerDefaultNetworkCallback(networkCallback)
        refreshNow()
        viewModelScope.launch {
            while (true) {
                sample()
                delay(1_000L)
            }
        }
        viewModelScope.launch {
            while (true) {
                runProbes()
                delay(5_000L)
            }
        }
    }

    override fun onCleared() {
        connectivity.unregisterNetworkCallback(networkCallback)
        tetheringMonitor.close()
    }

    fun refreshNow() {
        viewModelScope.launch {
            sample()
        }
    }

    private suspend fun sample() {
        val timestamp = System.currentTimeMillis()
        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()
        val elapsedSeconds = max(1.0, (timestamp - previousTimestamp) / 1_000.0)
        val rxDelta = max(0L, rxBytes - previousRxBytes)
        val txDelta = max(0L, txBytes - previousTxBytes)
        val rxBps = rxDelta / elapsedSeconds
        val txBps = txDelta / elapsedSeconds

        trafficTracker.addSample(timestamp, rxDelta, txDelta, rxBps, txBps)
        trafficSampleHistory.add(TrafficSamplePoint(timestamp, rxBps, txBps))

        previousRxBytes = rxBytes
        previousTxBytes = txBytes
        previousTimestamp = timestamp

        val network = readNetworkState()
        val probe = readProbeDashboard()
        val traffic = TrafficDashboard(
            downloadMbps = formatMbps(rxBps),
            uploadMbps = formatMbps(txBps),
            sessionDown = formatBytes(rxBytes - initialRxBytes),
            sessionUp = formatBytes(txBytes - initialTxBytes),
            stability = trafficTracker.stability(),
            buckets = trafficTracker.buckets().takeLast(8),
            samples = trafficSampleHistory.samples(),
            todayUsage = "Unavailable"
        )
        val score = DashboardScoreCalculator.calculate(
            isConnected = network.isConnected,
            isValidated = network.isValidated,
            averageLatencyMs = probe.summary.averageLatencyMs,
            packetLossPercent = probe.summary.packetLossPercent,
            jitterMs = probe.summary.jitterMs,
            trafficStability = traffic.stability
        )

        _state.value = DashboardState(
            score = score,
            network = network,
            traffic = traffic,
            probe = probe,
            tethering = tetheringMonitor.snapshot(),
            signal = readSignal(),
            permissions = readPermissions()
        )
    }

    private fun readNetworkState(): NetworkStateSnapshot {
        val simSlots = SimStateReader.read(app, hasPermission(Manifest.permission.READ_PHONE_STATE))
        val activeDataSubId = simSlots.firstOrNull { it.activeData }?.subscriptionId
        val telephony = app.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val activeTelephony = activeDataSubId?.let { telephony.createForSubscriptionId(it) } ?: telephony
        val networkCountryIso = runCatching { activeTelephony.networkCountryIso }.getOrNull()
        val networkOperatorName = runCatching { activeTelephony.networkOperatorName }.getOrNull()
        val networkOperatorCode = runCatching { activeTelephony.networkOperator }.getOrNull()
        val networkTypeLabel = runCatching { networkTypeName(activeTelephony.dataNetworkType) }
            .getOrNull()
            ?.takeUnless { it == "Unknown network" }
        val simCountryIso = simSlots.firstOrNull { it.activeData }?.countryIso
            ?: simSlots.firstOrNull { !it.permissionDenied }?.countryIso
        val country = NetworkCountryResolver.resolve(networkCountryIso, simCountryIso)
        val base = NetworkStateSnapshot(
            activeDataSubId = activeDataSubId,
            simSlots = simSlots,
            networkTypeLabel = networkTypeLabel,
            networkCountryIso = networkCountryIso,
            networkOperatorName = networkOperatorName,
            networkOperatorCode = networkOperatorCode,
            country = country
        )

        val network = connectivity.activeNetwork ?: return base
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return base
        val link = connectivity.getLinkProperties(network)
        val dnsServers = link?.dnsServers?.map { it.hostAddress.orEmpty() }.orEmpty()
        return NetworkStateSnapshot(
            isConnected = true,
            isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            isMetered = connectivity.isActiveNetworkMetered,
            transports = listOfNotNull(
                (networkTypeLabel ?: "5G/LTE").takeIf { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) },
                "Wi-Fi".takeIf { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) },
                "Ethernet".takeIf { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) }
            ).ifEmpty { listOf("Other") },
            interfaceName = link?.interfaceName,
            dnsServers = dnsServers,
            activeDataSubId = activeDataSubId,
            simSlots = simSlots,
            networkTypeLabel = networkTypeLabel,
            networkCountryIso = networkCountryIso,
            networkOperatorName = networkOperatorName,
            networkOperatorCode = networkOperatorCode,
            dnsProfiles = DnsProfileClassifier.classify(dnsServers, networkCountryIso),
            country = country
        )
    }

    private suspend fun runProbes() {
        val targets = listOf(
            "1.1.1.1" to "1.1.1.1",
            "8.8.8.8" to "8.8.8.8"
        )
        targets.forEach { (label, host) ->
            val result = tcpProbe(label, host, 53)
            probeResults.addLast(result)
            while (probeResults.size > 60) probeResults.removeFirst()
        }
        sample()
    }

    private suspend fun tcpProbe(label: String, host: String, port: Int): ProbeResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 1_500)
            }
            ProbeResult(label, true, System.currentTimeMillis() - started, null)
        } catch (error: Exception) {
            httpsHeadProbe(label, host)
        }
    }

    private fun httpsHeadProbe(label: String, host: String): ProbeResult {
        val started = System.currentTimeMillis()
        return try {
            val connection = (URL("https://$host").openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 1_500
                readTimeout = 1_500
            }
            connection.responseCode
            connection.disconnect()
            ProbeResult(label, true, System.currentTimeMillis() - started, null)
        } catch (error: Exception) {
            ProbeResult(label, false, null, error.javaClass.simpleName)
        }
    }

    private fun readProbeDashboard(): ProbeDashboard {
        val latestByTarget = probeResults.groupBy { it.targetId }.map { (target, results) ->
            val latest = results.last()
            ProbeTargetState(
                label = target,
                target = target,
                status = if (latest.success) "${latest.latencyMs} ms" else latest.errorType ?: "Failed"
            )
        }
        return ProbeDashboard(
            targets = latestByTarget,
            summary = ProbeSummaryCalculator.summarize(probeResults.toList()),
            history = probeResults.takeLast(16).map {
                ProbePoint(it.targetId, it.latencyMs, it.success)
            }
        )
    }

    private fun readSignal(): SimpleStatus {
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            return SimpleStatus("Signal", "Permission denied. Data was not collected.", true)
        }
        val telephony = app.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return try {
            SimpleStatus(
                title = "Signal",
                value = listOf(
                    telephony.networkOperatorName.ifBlank { "Unknown carrier" },
                    networkTypeName(telephony.dataNetworkType)
                ).joinToString(" / ")
            )
        } catch (error: SecurityException) {
            SimpleStatus("Signal", "Permission denied. Data was not collected.", true)
        }
    }

    private fun readPermissions(): List<PermissionStatus> {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= 33) add("Notifications" to Manifest.permission.POST_NOTIFICATIONS)
            add("Phone state" to Manifest.permission.READ_PHONE_STATE)
        }
        return permissions.map { (label, permission) ->
            PermissionStatus(label, permission, hasPermission(permission))
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(app, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun networkTypeName(type: Int): String = when (type) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
        else -> "Unknown network"
    }

    private fun formatMbps(bytesPerSecond: Double): String {
        return String.format(Locale.US, "%.2f Mbps", bytesPerSecond * 8 / 1_000_000)
    }

    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = max(0L, bytes).toDouble()
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit += 1
        }
        return String.format(Locale.US, "%.1f %s", value, units[unit])
    }
}
