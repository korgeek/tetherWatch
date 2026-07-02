package com.nextti.tetherwatch

import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager

object NetworkTypeDisplayName {
    fun label(
        networkType: Int,
        overrideNetworkType: Int = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
    ): String {
        return when {
            overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
                overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5G"
            networkType == TelephonyManager.NETWORK_TYPE_NR -> "5G"
            networkType == TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            networkType == TelephonyManager.NETWORK_TYPE_HSPAP ||
                networkType == TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
            networkType == TelephonyManager.NETWORK_TYPE_EDGE ||
                networkType == TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
            else -> "Unknown network"
        }
    }
}
