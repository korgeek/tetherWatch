package com.nextti.tetherwatch

import android.content.Context
import android.net.ConnectivityManager
import android.net.TetheringInterface
import android.net.TetheringManager
import android.os.Build
import androidx.core.content.ContextCompat

const val TETHERING_TYPE_WIFI = 0
const val TETHERING_TYPE_OTHER = -1

data class RawTetheringInterface(
    val type: Int,
    val name: String
)

object TetheringDashboardBuilder {
    fun build(
        serviceAvailable: Boolean,
        interfaces: List<RawTetheringInterface>,
        upstreamInterface: String?,
        lastError: String? = null
    ): TetheringDashboard {
        if (!serviceAvailable) {
            return TetheringDashboard(
                state = TetheringState.UNAVAILABLE,
                lastError = lastError,
                clientVisibility = TetheringClientVisibility.UNKNOWN
            )
        }

        if (interfaces.isEmpty()) {
            return TetheringDashboard(
                state = TetheringState.OFF,
                upstreamInterface = upstreamInterface,
                lastError = lastError,
                clientVisibility = TetheringClientVisibility.UNKNOWN
            )
        }

        return TetheringDashboard(
            state = TetheringState.ACTIVE,
            types = interfaces.map { it.type.toTetheringType() }.toSet(),
            downstreamInterfaces = interfaces.map { it.name }.filter { it.isNotBlank() }.distinct(),
            upstreamInterface = upstreamInterface,
            lastError = lastError,
            clientVisibility = TetheringClientVisibility.RESTRICTED
        )
    }

    fun restricted(error: String): TetheringDashboard {
        return TetheringDashboard(
            state = TetheringState.RESTRICTED,
            lastError = error,
            clientVisibility = TetheringClientVisibility.RESTRICTED
        )
    }

    private fun Int.toTetheringType(): TetheringType = when (this) {
        TETHERING_TYPE_WIFI -> TetheringType.WIFI
        else -> TetheringType.OTHER
    }
}

class TetheringMonitor(
    private val context: Context,
    private val connectivity: ConnectivityManager,
    private val onChanged: () -> Unit
) {
    @Volatile
    private var latest: TetheringDashboard = TetheringDashboard()
    private var manager: TetheringManager? = null
    private var callback: TetheringManager.TetheringEventCallback? = null

    init {
        register()
    }

    fun snapshot(): TetheringDashboard = latest

    fun close() {
        val activeManager = manager ?: return
        val activeCallback = callback ?: return
        runCatching {
            activeManager.unregisterTetheringEventCallback(activeCallback)
        }
        callback = null
    }

    private fun register() {
        if (Build.VERSION.SDK_INT < 30) {
            latest = TetheringDashboardBuilder.build(false, emptyList(), null)
            return
        }

        val tetheringManager = context.getSystemService(TetheringManager::class.java)
        if (tetheringManager == null) {
            latest = TetheringDashboardBuilder.build(false, emptyList(), null)
            return
        }

        manager = tetheringManager
        val eventCallback = object : TetheringManager.TetheringEventCallback {
            override fun onTetheredInterfacesChanged(interfaces: MutableSet<TetheringInterface>) {
                latest = TetheringDashboardBuilder.build(
                    serviceAvailable = true,
                    interfaces = interfaces.map { it.toRawTetheringInterface() },
                    upstreamInterface = activeUpstreamInterface()
                )
                onChanged()
            }
        }
        callback = eventCallback

        try {
            tetheringManager.registerTetheringEventCallback(
                ContextCompat.getMainExecutor(context),
                eventCallback
            )
            latest = TetheringDashboardBuilder.build(true, emptyList(), activeUpstreamInterface())
        } catch (error: SecurityException) {
            latest = TetheringDashboardBuilder.restricted(error.javaClass.simpleName)
        } catch (error: RuntimeException) {
            latest = TetheringDashboardBuilder.restricted(error.javaClass.simpleName)
        }
    }

    private fun TetheringInterface.toRawTetheringInterface(): RawTetheringInterface {
        val rawType = if (type == TetheringManager.TETHERING_WIFI) {
            TETHERING_TYPE_WIFI
        } else {
            TETHERING_TYPE_OTHER
        }
        return RawTetheringInterface(rawType, getInterface())
    }

    private fun activeUpstreamInterface(): String? {
        val network = connectivity.activeNetwork ?: return null
        return connectivity.getLinkProperties(network)?.interfaceName
    }
}
