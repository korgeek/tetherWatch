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
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.util.Log
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
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val trafficTracker = TrafficBucketTracker()
    private val trafficSampleHistory = TrafficSampleHistory()
    private val probeResults = ArrayDeque<ProbeResult>()
    private val pingConsole = PingConsoleBuffer()
    private val endpointConsole = PingConsoleBuffer()
    private val pingTargetScheduler = PingTargetScheduler()
    private val endpointTargetScheduler = EndpointTargetScheduler()
    private val endpointHttpChecker = EndpointHttpChecker()
    private val settingsStore = app.getSharedPreferences("tetherwatch_ping_settings", Context.MODE_PRIVATE)
    private val appSettingsStore = app.getSharedPreferences(AppSettingsStorageKeys.STORE_NAME, Context.MODE_PRIVATE)
    private var probeSettings = loadProbeSettings()
    private var endpointSettings = loadEndpointSettings()
    private var appSettings = loadAppSettings()
    private var displayNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN
    private var displayNetworkTypeOverride = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
    private var registeredDisplaySubId: Int? = null
    private val _state = MutableStateFlow(
        DashboardState(
            probeSettings = probeSettings,
            endpointSettings = endpointSettings,
            appSettings = appSettings
        )
    )
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
    private val telephonyDisplayCallback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
            displayNetworkType = telephonyDisplayInfo.networkType
            displayNetworkTypeOverride = telephonyDisplayInfo.overrideNetworkType
            refreshNow()
        }
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
                runDueProbes()
                runDueEndpointChecks()
                delay(1_000L)
            }
        }
    }

    override fun onCleared() {
        connectivity.unregisterNetworkCallback(networkCallback)
        unregisterTelephonyDisplayCallback()
        tetheringMonitor.close()
    }

    fun refreshNow() {
        viewModelScope.launch {
            sample()
        }
    }

    fun setPingSettings(settings: ProbeSettings) {
        probeSettings = ProbeSettings(
            targetSettings = PingTargetOptions.normalize(settings.targetSettings),
            consoleFontSizeSp = PingConsoleFontOptions.normalize(settings.consoleFontSizeSp),
            consoleLineCount = PingConsoleLineOptions.normalize(settings.consoleLineCount),
            slowResponseThresholdMs = PingSlowResponseOptions.normalize(settings.slowResponseThresholdMs)
        )
        settingsStore.edit()
            .putInt(PING_INTERVAL_SECONDS_KEY, probeSettings.intervalSeconds)
            .putString(PING_TARGETS_KEY, PingTargetOptions.encodeSettingsText(probeSettings.targetSettings))
            .putInt(PING_CONSOLE_FONT_SIZE_SP_KEY, probeSettings.consoleFontSizeSp)
            .putInt(PING_CONSOLE_LINE_COUNT_KEY, probeSettings.consoleLineCount)
            .putInt(PING_SLOW_RESPONSE_THRESHOLD_MS_KEY, probeSettings.slowResponseThresholdMs)
            .apply()
        probeResults.clear()
        pingConsole.clear()
        pingTargetScheduler.clear()
        refreshNow()
    }

    fun setEndpointSettings(settings: EndpointSettings) {
        endpointSettings = EndpointSettings(
            targetSettings = EndpointTargetOptions.normalize(settings.targetSettings),
            timeoutMs = EndpointTimeoutOptions.normalize(settings.timeoutMs),
            redirectLimit = EndpointRedirectOptions.normalize(settings.redirectLimit)
        )
        settingsStore.edit()
            .putString(ENDPOINT_TARGETS_KEY, EndpointTargetOptions.encodeSettingsText(endpointSettings.targetSettings))
            .putInt(ENDPOINT_TIMEOUT_MS_KEY, endpointSettings.timeoutMs)
            .putInt(ENDPOINT_REDIRECT_LIMIT_KEY, endpointSettings.redirectLimit)
            .apply()
        endpointConsole.clear()
        endpointTargetScheduler.clear()
        refreshNow()
    }

    fun setAppSettings(settings: AppSettings) {
        appSettings = settings
        appSettingsStore.edit()
            .putBoolean(AppSettingsStorageKeys.AUTO_BACKGROUND_MONITORING_KEY, settings.autoBackgroundMonitoring)
            .putBoolean(AppSettingsStorageKeys.KEEP_SCREEN_ON_KEY, settings.keepScreenOn)
            .putString(AppSettingsStorageKeys.THEME_MODE_KEY, settings.themeMode.name)
            .putBoolean(AppSettingsStorageKeys.PING_MONITORING_ENABLED_KEY, settings.pingMonitoringEnabled)
            .putBoolean(AppSettingsStorageKeys.STATUS_MONITORING_ENABLED_KEY, settings.statusMonitoringEnabled)
            .apply()
        _state.value = _state.value.copy(appSettings = appSettings)
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
        trafficSampleHistory.add(
            TrafficSamplePoint(
                timestampMs = timestamp,
                rxBps = rxBps,
                txBps = txBps,
                rxBytes = rxDelta,
                txBytes = txDelta
            )
        )

        previousRxBytes = rxBytes
        previousTxBytes = txBytes
        previousTimestamp = timestamp

        val network = readNetworkState()
        val probe = readProbeDashboard()
        val endpoint = readEndpointDashboard()
        val recentUsage = trafficSampleHistory.usageInLast(timestamp, 60_000L)
        val traffic = TrafficDashboard(
            downloadMbps = formatMbps(rxBps),
            uploadMbps = formatMbps(txBps),
            recentDown = formatBytes(recentUsage.rxBytes),
            recentUp = formatBytes(recentUsage.txBytes),
            recentTotal = formatBytes(recentUsage.rxBytes + recentUsage.txBytes),
            sessionDown = formatBytes(rxBytes - initialRxBytes),
            sessionUp = formatBytes(txBytes - initialTxBytes),
            sessionTotal = formatBytes(rxBytes - initialRxBytes + txBytes - initialTxBytes),
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
            trafficStability = traffic.stability,
            pingMonitoringEnabled = appSettings.pingMonitoringEnabled
        )

        _state.value = DashboardState(
            score = score,
            network = network,
            traffic = traffic,
            probe = probe,
            endpoint = endpoint,
            tethering = tetheringMonitor.snapshot(),
            probeSettings = probeSettings,
            endpointSettings = endpointSettings,
            appSettings = appSettings,
            signal = readSignal(),
            permissions = readPermissions()
        )
    }

    private fun readNetworkState(): NetworkStateSnapshot {
        val simSlots = SimStateReader.read(app, hasPermission(Manifest.permission.READ_PHONE_STATE))
        val activeDataSubId = simSlots.firstOrNull { it.activeData }?.subscriptionId
        val telephony = app.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val activeTelephony = activeDataSubId?.let { telephony.createForSubscriptionId(it) } ?: telephony
        ensureTelephonyDisplayCallback(activeDataSubId)
        val networkCountryIso = runCatching { activeTelephony.networkCountryIso }.getOrNull()
        val networkOperatorName = runCatching { activeTelephony.networkOperatorName }.getOrNull()
        val networkOperatorCode = runCatching { activeTelephony.networkOperator }.getOrNull()
        val networkTypeLabel = runCatching { displayAwareNetworkTypeName(activeTelephony) }
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
        val ipAddresses = link?.linkAddresses
            ?.mapNotNull { it.address.hostAddress }
            ?.map { it.substringBefore('%') }
            ?.filter { it.isNotBlank() && !it.startsWith("127.") && it != "::1" }
            .orEmpty()
        return NetworkStateSnapshot(
            isConnected = true,
            isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            isMetered = connectivity.isActiveNetworkMetered,
            transports = listOfNotNull(
                "Wi-Fi".takeIf { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) },
                (networkTypeLabel ?: "5G/LTE").takeIf { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) },
                "Ethernet".takeIf { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) }
            ).ifEmpty { listOf("Other") },
            interfaceName = link?.interfaceName,
            ipAddresses = ipAddresses,
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

    private suspend fun runDueProbes() {
        if (!appSettings.pingMonitoringEnabled) {
            pingTargetScheduler.clear()
            sample()
            return
        }
        val now = System.currentTimeMillis()
        pingTargetScheduler.dueTargets(probeSettings.targetSettings, now).forEach { targetSetting ->
            val target = targetSetting.target
            val parsed = pingTarget(target)
            pingConsole.add(ConsoleLineTimestampFormatter.prefix(parsed.consoleLine, now))
            probeResults.addLast(parsed.result)
            while (probeResults.size > 60) probeResults.removeFirst()
            pingTargetScheduler.markPingStarted(targetSetting, now)
        }
        sample()
    }

    private suspend fun runDueEndpointChecks() {
        if (!appSettings.statusMonitoringEnabled) {
            endpointTargetScheduler.clear()
            sample()
            return
        }
        val now = System.currentTimeMillis()
        endpointTargetScheduler.dueTargets(endpointSettings.targetSettings, now).forEach { targetSetting ->
            val result = checkEndpoint(targetSetting)
            endpointConsole.add(
                ConsoleLineTimestampFormatter.prefix(
                    EndpointCheckResultFormatter.consoleLine(result),
                    now
                )
            )
            endpointTargetScheduler.markCheckStarted(targetSetting, now)
        }
        sample()
    }

    private suspend fun checkEndpoint(target: EndpointTargetSetting): EndpointCheckResult = withContext(Dispatchers.IO) {
        endpointHttpChecker.check(target, endpointSettings)
    }

    private suspend fun pingTarget(target: String): ParsedPingOutput = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(PING_COMMAND, "-c", "1", "-W", "2", target)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(3, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = if (finished) process.exitValue() else 1
            val timeoutLine = if (finished) "" else "\nping: timeout waiting for $target"
            PingCommandOutputParser.parse(target, exitCode, output + timeoutLine)
        } catch (error: Exception) {
            PingCommandOutputParser.parse(
                target = target,
                exitCode = 127,
                output = "ping: ${error.message ?: error.javaClass.simpleName}"
            )
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
            },
            consoleLines = pingConsole.lines()
        )
    }

    private fun readEndpointDashboard(): EndpointDashboard {
        return EndpointDashboard(consoleLines = endpointConsole.lines())
    }

    private fun loadProbeSettings(): ProbeSettings {
        val defaultInterval = settingsStore.getInt(PING_INTERVAL_SECONDS_KEY, ProbeIntervalOptions.DEFAULT_SECONDS)
        val targetSettings = settingsStore.getString(PING_TARGETS_KEY, null)
            ?.let { PingTargetOptions.parseSettingsText(it, defaultInterval) }
            ?: PingTargetOptions.DEFAULT_TARGET_SETTINGS
        return ProbeSettings(
            targetSettings = PingTargetOptions.normalize(targetSettings),
            consoleFontSizeSp = PingConsoleFontOptions.normalize(
                settingsStore.getInt(PING_CONSOLE_FONT_SIZE_SP_KEY, PingConsoleFontOptions.DEFAULT_SP)
            ),
            consoleLineCount = PingConsoleLineOptions.normalize(
                settingsStore.getInt(PING_CONSOLE_LINE_COUNT_KEY, PingConsoleLineOptions.DEFAULT_COUNT)
            ),
            slowResponseThresholdMs = PingSlowResponseOptions.normalize(
                settingsStore.getInt(PING_SLOW_RESPONSE_THRESHOLD_MS_KEY, PingSlowResponseOptions.DEFAULT_MS)
            )
        )
    }

    private fun loadEndpointSettings(): EndpointSettings {
        val targetSettings = settingsStore.getString(ENDPOINT_TARGETS_KEY, null)
            ?.let { EndpointTargetOptions.parseSettingsText(it) }
            ?: EndpointTargetOptions.DEFAULT_TARGET_SETTINGS
        return EndpointSettings(
            targetSettings = EndpointTargetOptions.migrateLegacyDefaultSettings(targetSettings),
            timeoutMs = EndpointTimeoutOptions.normalize(
                settingsStore.getInt(ENDPOINT_TIMEOUT_MS_KEY, EndpointTimeoutOptions.DEFAULT_MS)
            ),
            redirectLimit = EndpointRedirectOptions.normalize(
                settingsStore.getInt(ENDPOINT_REDIRECT_LIMIT_KEY, EndpointRedirectOptions.DEFAULT_LIMIT)
            )
        )
    }

    private fun loadAppSettings(): AppSettings {
        val themeMode = runCatching {
            AppThemeMode.valueOf(
                appSettingsStore.getString(
                    AppSettingsStorageKeys.THEME_MODE_KEY,
                    AppThemeMode.SYSTEM.name
                ) ?: AppThemeMode.SYSTEM.name
            )
        }.getOrDefault(AppThemeMode.SYSTEM)
        return AppSettings(
            autoBackgroundMonitoring = appSettingsStore.getBoolean(AppSettingsStorageKeys.AUTO_BACKGROUND_MONITORING_KEY, false),
            keepScreenOn = appSettingsStore.getBoolean(AppSettingsStorageKeys.KEEP_SCREEN_ON_KEY, false),
            themeMode = themeMode,
            pingMonitoringEnabled = appSettingsStore.getBoolean(AppSettingsStorageKeys.PING_MONITORING_ENABLED_KEY, true),
            statusMonitoringEnabled = appSettingsStore.getBoolean(AppSettingsStorageKeys.STATUS_MONITORING_ENABLED_KEY, true)
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
                    displayAwareNetworkTypeName(telephony)
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

    private fun displayAwareNetworkTypeName(telephony: TelephonyManager): String {
        val fallbackType = telephony.dataNetworkType
        val networkType = displayNetworkType.takeUnless { it == TelephonyManager.NETWORK_TYPE_UNKNOWN } ?: fallbackType
        return NetworkTypeDisplayName.label(networkType, displayNetworkTypeOverride)
    }

    private fun ensureTelephonyDisplayCallback(subscriptionId: Int?) {
        if (registeredDisplaySubId == subscriptionId) return
        unregisterTelephonyDisplayCallback()
        val telephony = app.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val displayTelephony = subscriptionId?.let { telephony.createForSubscriptionId(it) } ?: telephony
        runCatching {
            displayTelephony.registerTelephonyCallback(app.mainExecutor, telephonyDisplayCallback)
            registeredDisplaySubId = subscriptionId
        }.onFailure { error ->
            Log.w(TAG, "Unable to register telephony display callback.", error)
        }
    }

    private fun unregisterTelephonyDisplayCallback() {
        val telephony = app.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        runCatching {
            telephony.unregisterTelephonyCallback(telephonyDisplayCallback)
            registeredDisplaySubId = null
        }.onFailure { error ->
            Log.w(TAG, "Unable to unregister telephony display callback.", error)
        }
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

    companion object {
        private const val TAG = "TetherWatchDashboard"
        private const val PING_COMMAND = "/system/bin/ping"
        private const val PING_INTERVAL_SECONDS_KEY = "interval_seconds"
        private const val PING_TARGETS_KEY = "targets"
        private const val PING_CONSOLE_FONT_SIZE_SP_KEY = "console_font_size_sp"
        private const val PING_CONSOLE_LINE_COUNT_KEY = "console_line_count"
        private const val PING_SLOW_RESPONSE_THRESHOLD_MS_KEY = "slow_response_threshold_ms"
        private const val ENDPOINT_TARGETS_KEY = "endpoint_targets"
        private const val ENDPOINT_TIMEOUT_MS_KEY = "endpoint_timeout_ms"
        private const val ENDPOINT_REDIRECT_LIMIT_KEY = "endpoint_redirect_limit"
    }
}
