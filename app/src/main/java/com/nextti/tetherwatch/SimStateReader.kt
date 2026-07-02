package com.nextti.tetherwatch

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import java.util.Locale

object SimStateReader {
    fun read(context: Context, permissionGranted: Boolean): List<SimSlotSnapshot> {
        if (!permissionGranted) return buildSnapshots(false, null, emptyList())

        return try {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            val activeDataSubId = SubscriptionManager.getActiveDataSubscriptionId()
                .takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
            val subscriptions = subscriptionManager.activeSubscriptionInfoList.orEmpty().map {
                RawSimSubscription(
                    slotIndex = it.simSlotIndex,
                    subscriptionId = it.subscriptionId,
                    carrierName = it.carrierName?.toString().orEmpty().ifBlank { "Unknown carrier" },
                    displayName = it.displayName?.toString().orEmpty().ifBlank { "SIM ${it.simSlotIndex + 1}" },
                    countryIso = if (Build.VERSION.SDK_INT >= 29) it.countryIso else null
                )
            }
            buildSnapshots(true, activeDataSubId, subscriptions)
        } catch (error: SecurityException) {
            buildSnapshots(false, null, emptyList())
        } catch (error: Exception) {
            emptyList()
        }
    }

    fun buildSnapshots(
        permissionGranted: Boolean,
        activeDataSubId: Int?,
        subscriptions: List<RawSimSubscription>
    ): List<SimSlotSnapshot> {
        if (!permissionGranted) return listOf(SimSlotSnapshot.permissionDenied())

        return subscriptions
            .sortedBy { it.slotIndex }
            .map { subscription ->
                SimSlotSnapshot(
                    slotIndex = subscription.slotIndex,
                    subscriptionId = subscription.subscriptionId,
                    carrierName = subscription.carrierName.ifBlank { "Unknown carrier" },
                    displayName = subscription.displayName.ifBlank { "SIM ${subscription.slotIndex + 1}" },
                    countryIso = subscription.countryIso
                        ?.trim()
                        ?.takeIf { it.length == 2 }
                        ?.uppercase(Locale.US),
                    activeData = subscription.subscriptionId == activeDataSubId,
                    permissionDenied = false
                )
            }
    }
}
