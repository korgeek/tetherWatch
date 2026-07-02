package com.nextti.tetherwatch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ForegroundServiceManifestTest {
    @Test
    fun monitorServiceDoesNotUseConnectedDeviceForegroundType() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE_DATA_SYNC"))
        assertTrue(manifest.contains("android:foregroundServiceType=\"dataSync\""))
        assertFalse(manifest.contains("android:foregroundServiceType=\"connectedDevice\""))
    }

    @Test
    fun mainActivityStartsMonitorForegroundServiceOnLaunch() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()

        assertTrue(mainActivity.contains("startMonitorForegroundService()"))
        assertTrue(mainActivity.contains("ContextCompat.startForegroundService"))
        assertTrue(mainActivity.contains("MonitorForegroundService::class.java"))
    }

    @Test
    fun backPressShowsMonitoringControlsInsteadOfFinishingImmediately() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()
        val service = File("src/main/java/com/nextti/tetherwatch/MonitorForegroundService.kt").readText()

        assertTrue(mainActivity.contains("BackHandler"))
        assertTrue(mainActivity.contains("MonitoringControlsDialog"))
        assertTrue(mainActivity.contains("moveTaskToBack(true)"))
        assertTrue(mainActivity.contains("stopMonitorForegroundService()"))
        assertTrue(mainActivity.contains("finishAndRemoveTask()"))
        assertTrue(mainActivity.contains("MonitoringIntents.ACTION_STOP_MONITORING"))
        assertTrue(service.contains("MonitoringIntents.ACTION_STOP_MONITORING"))
        assertTrue(service.contains("stopForeground"))
        assertTrue(service.contains("stopSelf(startId)"))
    }

    @Test
    fun notificationTapReturnsToSingleTopMonitoringScreen() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val service = File("src/main/java/com/nextti/tetherwatch/MonitorForegroundService.kt").readText()
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()

        assertTrue(manifest.contains("android:launchMode=\"singleTop\""))
        assertTrue(service.contains("MonitoringIntents.ACTION_OPEN_MONITORING"))
        assertTrue(service.contains("Intent.FLAG_ACTIVITY_CLEAR_TOP"))
        assertTrue(service.contains("Intent.FLAG_ACTIVITY_SINGLE_TOP"))
        assertTrue(service.contains("Intent.FLAG_ACTIVITY_NEW_TASK"))
        assertTrue(mainActivity.contains("onNewIntent"))
        assertTrue(mainActivity.contains("isMonitoringOpenIntent"))
        assertFalse(mainActivity.contains("notificationOpenToken"))
        assertFalse(mainActivity.contains("mutableStateOf(intent.isMonitoringOpenIntent())"))
        assertFalse(mainActivity.contains("LaunchedEffect(notificationOpenToken)"))
    }

    @Test
    fun notificationIncludesStopMonitoringAction() {
        val service = File("src/main/java/com/nextti/tetherwatch/MonitorForegroundService.kt").readText()

        assertTrue(service.contains("PendingIntent.getService"))
        assertTrue(service.contains("ACTION_STOP_MONITORING"))
        assertTrue(service.contains(".addAction("))
        assertTrue(service.contains("\"Stop monitoring\""))
    }

    @Test
    fun foregroundServiceRunsPingWhileInBackgroundButNotificationOnlyShowsTraffic() {
        val service = File("src/main/java/com/nextti/tetherwatch/MonitorForegroundService.kt").readText()

        assertTrue(service.contains("PingTargetScheduler"))
        assertTrue(service.contains("EndpointTargetScheduler"))
        assertTrue(service.contains("loadMonitorProbeSettings"))
        assertTrue(service.contains("loadMonitorEndpointSettings"))
        assertTrue(service.contains("pingTarget("))
        assertTrue(service.contains("endpointHttpChecker.check"))
        assertTrue(service.contains("PingCommandOutputParser.parse"))
        assertTrue(service.contains("TrafficRateFormatter.notificationText"))
        assertTrue(service.contains("NotificationCompat.BigTextStyle"))
        assertFalse(service.contains("monitoringNotificationText"))
    }

    @Test
    fun monitoringControlsUseCustomModernDialog() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()

        assertTrue(mainActivity.contains("private fun MonitoringControlsDialog"))
        assertTrue(mainActivity.contains("Dialog("))
        assertTrue(mainActivity.contains("Background monitoring"))
        assertTrue(mainActivity.contains("Keep background monitoring"))
        assertTrue(mainActivity.contains("Stop and close"))
        assertTrue(mainActivity.contains("Cancel"))
        assertTrue(mainActivity.contains("RoundedCornerShape(28.dp)"))
        assertTrue(!mainActivity.contains("Choose what happens when TetherWatch leaves the screen."))
        assertTrue(!mainActivity.contains("color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)"))
        assertTrue(!mainActivity.contains("title = { Text(\"Monitoring controls\") }"))
    }
}
