package com.nextti.tetherwatch

import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkTypeDisplayNameTest {
    @Test
    fun nrNetworkTypeShowsFiveG() {
        assertEquals(
            "5G",
            NetworkTypeDisplayName.label(
                networkType = TelephonyManager.NETWORK_TYPE_NR,
                overrideNetworkType = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
            )
        )
    }

    @Test
    fun lteWithNrNsaOverrideShowsFiveGLikeStatusBar() {
        assertEquals(
            "5G",
            NetworkTypeDisplayName.label(
                networkType = TelephonyManager.NETWORK_TYPE_LTE,
                overrideNetworkType = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA
            )
        )
    }

    @Test
    fun lteWithoutOverrideStillShowsLte() {
        assertEquals(
            "LTE",
            NetworkTypeDisplayName.label(
                networkType = TelephonyManager.NETWORK_TYPE_LTE,
                overrideNetworkType = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
            )
        )
    }
}
