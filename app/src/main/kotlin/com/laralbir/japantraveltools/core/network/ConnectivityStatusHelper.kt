package com.laralbir.japantraveltools.core.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings

enum class ActiveConnectionKind(
    val badgeEn: String,
    val badgeEs: String
) {
    WIFI("Wi-Fi", "Wi-Fi"),
    WIFI_METERED("Pocket Wi-Fi / Hotspot", "Pocket Wi-Fi / Hotspot"),
    CELLULAR_ESIM("eSIM Data · 5G/LTE", "Datos eSIM · 5G/LTE"),
    CELLULAR_SIM("SIM Data · 5G/LTE", "Datos SIM · 5G/LTE"),
    CELLULAR_ROAMING("Roaming Active!", "¡Roaming Activo!"),
    OFFLINE("Offline Mode (Local Cache)", "Modo Offline (Caché Local)")
}

data class ConnectivityStatusSnapshot(
    val kind: ActiveConnectionKind,
    val isOnline: Boolean,
    val isMetered: Boolean,
    val isRoaming: Boolean,
    val forceOfflineByApp: Boolean
)

object ConnectivityStatusHelper {

    fun inspectCurrentConnection(
        context: Context,
        forceOfflineByApp: Boolean
    ): ConnectivityStatusSnapshot {
        if (forceOfflineByApp) {
            return ConnectivityStatusSnapshot(
                kind = ActiveConnectionKind.OFFLINE,
                isOnline = false,
                isMetered = false,
                isRoaming = false,
                forceOfflineByApp = true
            )
        }

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return ConnectivityStatusSnapshot(
                kind = ActiveConnectionKind.OFFLINE,
                isOnline = false,
                isMetered = false,
                isRoaming = false,
                forceOfflineByApp = false
            )

        val activeNetwork = cm.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return ConnectivityStatusSnapshot(
                kind = ActiveConnectionKind.OFFLINE,
                isOnline = false,
                isMetered = false,
                isRoaming = false,
                forceOfflineByApp = false
            )
        }

        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val isRoaming = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)

        val kind = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                if (isMetered) ActiveConnectionKind.WIFI_METERED else ActiveConnectionKind.WIFI
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (isRoaming) {
                    ActiveConnectionKind.CELLULAR_ROAMING
                } else {
                    ActiveConnectionKind.CELLULAR_ESIM
                }
            }
            else -> ActiveConnectionKind.WIFI
        }

        return ConnectivityStatusSnapshot(
            kind = kind,
            isOnline = true,
            isMetered = isMetered,
            isRoaming = isRoaming,
            forceOfflineByApp = false
        )
    }

    fun openAndroidInternetPanel(context: Context) {
        runCatching {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openSimAndEsimSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
        }
    }
}
