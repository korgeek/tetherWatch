package com.nextti.tetherwatch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.max
import java.util.concurrent.TimeUnit

class MonitorForegroundService : Service() {
    private var monitorThread: Thread? = null
    @Volatile
    private var running = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == MonitoringIntents.ACTION_STOP_MONITORING) {
            return stopMonitoring(startId)
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                action = MonitoringIntents.ACTION_OPEN_MONITORING
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopMonitoring = PendingIntent.getService(
            this,
            1,
            Intent(this, MonitorForegroundService::class.java).apply {
                action = MonitoringIntents.ACTION_STOP_MONITORING
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return try {
            startForeground(NOTIFICATION_ID, buildNotification("Starting traffic monitor", openApp, stopMonitoring))
            startTrafficNotificationLoop(openApp, stopMonitoring)
            START_STICKY
        } catch (error: SecurityException) {
            Log.w(TAG, "Foreground monitoring is not allowed on this build.", error)
            stopSelf(startId)
            START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        monitorThread?.interrupt()
        monitorThread = null
        super.onDestroy()
    }

    private fun startTrafficNotificationLoop(openApp: PendingIntent, stopMonitoring: PendingIntent) {
        if (running) return
        running = true
        monitorThread = Thread {
            val manager = getSystemService(NotificationManager::class.java)
            var previousRxBytes = TrafficStats.getTotalRxBytes()
            var previousTxBytes = TrafficStats.getTotalTxBytes()
            var previousTimestamp = System.currentTimeMillis()
            val pingTargetScheduler = PingTargetScheduler()
            val endpointTargetScheduler = EndpointTargetScheduler()
            val endpointHttpChecker = EndpointHttpChecker()

            while (running) {
                try {
                    Thread.sleep(NOTIFICATION_UPDATE_INTERVAL_MS)
                    val timestamp = System.currentTimeMillis()
                    val rxBytes = TrafficStats.getTotalRxBytes()
                    val txBytes = TrafficStats.getTotalTxBytes()
                    val elapsedSeconds = max(1.0, (timestamp - previousTimestamp) / 1_000.0)
                    val rxRate = max(0L, rxBytes - previousRxBytes) / elapsedSeconds
                    val txRate = max(0L, txBytes - previousTxBytes) / elapsedSeconds

                    previousRxBytes = rxBytes
                    previousTxBytes = txBytes
                    previousTimestamp = timestamp

                    val probeSettings = loadMonitorProbeSettings()
                    pingTargetScheduler.dueTargets(probeSettings.targetSettings, timestamp).forEach { targetSetting ->
                        pingTarget(targetSetting.target)
                        pingTargetScheduler.markPingStarted(targetSetting, timestamp)
                    }
                    val endpointSettings = loadMonitorEndpointSettings()
                    endpointTargetScheduler.dueTargets(endpointSettings.targetSettings, timestamp).forEach { targetSetting ->
                        endpointHttpChecker.check(targetSetting, endpointSettings)
                        endpointTargetScheduler.markCheckStarted(targetSetting, timestamp)
                    }

                    manager.notify(
                        NOTIFICATION_ID,
                        buildNotification(
                            TrafficRateFormatter.notificationText(rxRate, txRate),
                            openApp,
                            stopMonitoring
                        )
                    )
                } catch (_: InterruptedException) {
                    running = false
                } catch (error: SecurityException) {
                    Log.w(TAG, "Unable to update monitoring notification.", error)
                    running = false
                }
            }
        }.apply {
            name = "TetherWatchTrafficNotification"
            isDaemon = true
            start()
        }
    }

    private fun loadMonitorProbeSettings(): ProbeSettings {
        val store = getSharedPreferences(PING_SETTINGS_NAME, MODE_PRIVATE)
        val defaultInterval = store.getInt(PING_INTERVAL_SECONDS_KEY, ProbeIntervalOptions.DEFAULT_SECONDS)
        val targetSettings = store.getString(PING_TARGETS_KEY, null)
            ?.let { PingTargetOptions.parseSettingsText(it, defaultInterval) }
            ?: PingTargetOptions.DEFAULT_TARGET_SETTINGS
        return ProbeSettings(targetSettings = PingTargetOptions.normalize(targetSettings))
    }

    private fun loadMonitorEndpointSettings(): EndpointSettings {
        val store = getSharedPreferences(PING_SETTINGS_NAME, MODE_PRIVATE)
        val targetSettings = store.getString(ENDPOINT_TARGETS_KEY, null)
            ?.let { EndpointTargetOptions.parseSettingsText(it) }
            ?: EndpointTargetOptions.DEFAULT_TARGET_SETTINGS
        return EndpointSettings(
            targetSettings = EndpointTargetOptions.migrateLegacyDefaultSettings(targetSettings),
            timeoutMs = EndpointTimeoutOptions.normalize(
                store.getInt(ENDPOINT_TIMEOUT_MS_KEY, EndpointTimeoutOptions.DEFAULT_MS)
            ),
            redirectLimit = EndpointRedirectOptions.normalize(
                store.getInt(ENDPOINT_REDIRECT_LIMIT_KEY, EndpointRedirectOptions.DEFAULT_LIMIT)
            )
        )
    }

    private fun pingTarget(target: String): ParsedPingOutput {
        return try {
            val process = ProcessBuilder(PING_COMMAND, "-c", "1", "-W", "2", target)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(3, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
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

    private fun stopMonitoring(startId: Int): Int {
        running = false
        monitorThread?.interrupt()
        monitorThread = null
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun buildNotification(contentText: String, openApp: PendingIntent, stopMonitoring: PendingIntent) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("TetherWatch is monitoring")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop monitoring", stopMonitoring)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TetherWatch monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "tetherwatch_monitoring"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_UPDATE_INTERVAL_MS = 2_000L
        const val PING_SETTINGS_NAME = "tetherwatch_ping_settings"
        const val PING_INTERVAL_SECONDS_KEY = "interval_seconds"
        const val PING_TARGETS_KEY = "targets"
        const val ENDPOINT_TARGETS_KEY = "endpoint_targets"
        const val ENDPOINT_TIMEOUT_MS_KEY = "endpoint_timeout_ms"
        const val ENDPOINT_REDIRECT_LIMIT_KEY = "endpoint_redirect_limit"
        const val PING_COMMAND = "/system/bin/ping"
        const val TAG = "TetherWatchMonitor"
    }
}
