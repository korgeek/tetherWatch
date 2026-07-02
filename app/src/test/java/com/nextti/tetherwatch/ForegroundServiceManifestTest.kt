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
}
