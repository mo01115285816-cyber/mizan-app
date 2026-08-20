package com.example.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.DhcpInfo
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.RouteInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

data class NetworkDetails(
    val ssid: String,
    val bssid: String = "",
    val isConnected: Boolean,
    val isWifi: Boolean,
    val isCellular: Boolean = false,
    val signalPercentage: Int,
    val signalDbm: Int,
    val linkSpeedMbps: Int,
    val frequencyGhz: String,
    val ipAddress: String,
    val gateway: String,
    val securityType: String,
    val isMetered: Boolean,
    val statusText: String,
    val isValidated: Boolean = false
)

object NetworkInfoProvider {

    fun getConnectedNetworkDetails(context: Context): NetworkDetails {
        val appContext = context.applicationContext
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wifiManager =
            appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val linkProperties: LinkProperties? = connectivityManager?.getLinkProperties(activeNetwork)

        val isConnected = capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

        var rawSsid = ""
        var rawBssid = ""
        var rssi = 0
        var linkSpeed = 0
        var frequency = 0
        var securityType = ""

        if (wifiManager != null && isWifi) {
            val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && capabilities != null) {
                capabilities.transportInfo as? WifiInfo ?: wifiManager.connectionInfo
            } else {
                wifiManager.connectionInfo
            }

            if (wifiInfo != null) {
                rawSsid = wifiInfo.ssid ?: ""
                rawBssid = wifiInfo.bssid ?: ""
                rssi = wifiInfo.rssi
                linkSpeed = wifiInfo.linkSpeed
                frequency = wifiInfo.frequency
                securityType = resolveSecurityType(wifiInfo)
            }
        }

        // Clean up SSID formatting
        var cleanSsid = rawSsid.trim().removeSurrounding("\"")
        val hasUsableSsid = cleanSsid.isNotEmpty() &&
            !cleanSsid.equals("<unknown ssid>", ignoreCase = true) &&
            !cleanSsid.equals("0x", ignoreCase = true)
        if (!hasUsableSsid) {
            cleanSsid = if (isWifi) "غير متاح — تحقق من إذن الموقع وWi‑Fi" else if (isCellular) "بيانات الهاتف" else if (isConnected) "متصل بالإنترنت" else "غير متصل"
        }

        val cleanBssid = if (rawBssid.isNotBlank() && rawBssid != "02:00:00:00:00:00") rawBssid else ""

        // Calculate realistic signal percentage from RSSI
        val signalPercentage = when {
            rssi == 0 || !isWifi -> if (isConnected) 85 else 0
            rssi <= -100 -> 0
            rssi >= -50 -> 100
            else -> (2 * (rssi + 100)).coerceIn(0, 100)
        }

        // Real Frequency conversion
        val freqGhz = when {
            !isWifi -> "شبكة خلوية"
            frequency in 2400..2500 -> String.format(java.util.Locale.US, "%.3f GHz (2.4G)", frequency / 1000.0)
            frequency in 4900..5900 -> String.format(java.util.Locale.US, "%.3f GHz (5G)", frequency / 1000.0)
            frequency in 5925..7125 -> String.format(java.util.Locale.US, "%.3f GHz (Wi‑Fi 6E)", frequency / 1000.0)
            frequency > 0 -> "$frequency MHz"
            else -> "غير متاح"
        }

        // Extract Real IP and Gateway from LinkProperties / DhcpInfo
        val realIp = extractRealIp(linkProperties)
        val realGateway = extractRealGateway(linkProperties, wifiManager)

        val isMetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false

        val statusText = when {
            !isConnected -> "لا يوجد اتصال بالإنترنت"
            isWifi -> "متصل بشبكة Wi‑Fi"
            isCellular -> "متصل ببيانات الهاتف (الشريحة)"
            else -> "متصل بالإنترنت"
        }

        return NetworkDetails(
            ssid = cleanSsid,
            bssid = cleanBssid,
            isConnected = isConnected,
            isWifi = isWifi,
            isCellular = isCellular,
            signalPercentage = signalPercentage,
            signalDbm = rssi,
                            linkSpeedMbps = linkSpeed.coerceAtLeast(0),

            frequencyGhz = freqGhz,
            ipAddress = realIp,
            gateway = realGateway,
            securityType = if (isWifi) securityType else "شبكة محمول",

            isMetered = isMetered,
            statusText = statusText,
            isValidated = isValidated
        )
    }

    fun matchesTargetNetwork(
        details: NetworkDetails,
        targetSsid: String,
        targetBssid: String,
        legacyHomeBssid: String = "",
        fallbackSsid: String = ""
    ): Boolean {
        if (!details.isWifi || !details.isConnected) return false
        val configuredBssid = targetBssid.trim().ifBlank { legacyHomeBssid.trim() }
        val configuredSsid = targetSsid.trim().ifBlank { fallbackSsid.trim() }
        val observedSsidUsable = details.ssid.isNotBlank() && !details.ssid.startsWith("غير متاح")
        if (configuredBssid.isNotBlank()) {
            return details.bssid.isNotBlank() &&
                details.bssid.equals(configuredBssid, ignoreCase = true)
        }
        return configuredSsid.isNotBlank() && observedSsidUsable &&
            details.ssid.equals(configuredSsid, ignoreCase = true)
    }

    fun classifyNetworkState(details: NetworkDetails): String {
        return when {
            !details.isConnected -> "NO_NETWORK"
            details.isCellular -> "CELLULAR"
            !details.isWifi -> "OTHER_TRANSPORT"
            !details.isValidated -> "WIFI_UNVALIDATED"
            details.ssid.startsWith("غير متاح") -> "WIFI_SSID_REDACTED"
            else -> "WIFI_CONNECTED"
        }
    }

    private fun resolveSecurityType(wifiInfo: WifiInfo): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return ""
        return try {
            val value = WifiInfo::class.java.getMethod("getCurrentSecurityType").invoke(wifiInfo) as? Int ?: return ""
            val labels = listOf(
                "SECURITY_TYPE_OPEN" to "OPEN",
                "SECURITY_TYPE_WEP" to "WEP",
                "SECURITY_TYPE_PSK" to "PSK",
                "SECURITY_TYPE_EAP" to "EAP",
                "SECURITY_TYPE_SAE" to "SAE",
                "SECURITY_TYPE_OWE" to "OWE",
                "SECURITY_TYPE_WAPI_PSK" to "WAPI-PSK",
                "SECURITY_TYPE_WAPI_CERT" to "WAPI-CERT",
                "SECURITY_TYPE_DPP" to "DPP"
            )
            labels.firstNotNullOfOrNull { (fieldName, label) ->
                runCatching {
                    if (WifiInfo::class.java.getField(fieldName).getInt(null) == value) label else null
                }.getOrNull()
            } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun isLocationServicesEnabled(context: Context): Boolean {
        val locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) ||
                    locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun extractRealIp(linkProperties: LinkProperties?): String {
        if (linkProperties != null) {
            for (linkAddress in linkProperties.linkAddresses) {
                val addr = linkAddress.address
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    val host = addr.hostAddress
                    if (!host.isNullOrBlank()) return host
                }
            }
        }

        // Fallback to active network interfaces
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isUp && !intf.isLoopback) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank()) return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return "غير متوفر"
    }

    private fun extractRealGateway(linkProperties: LinkProperties?, wifiManager: WifiManager?): String {
        if (linkProperties != null) {
            for (route in linkProperties.routes) {
                val gateway = route.gateway
                if (gateway is Inet4Address && !gateway.isLoopbackAddress && route.isDefaultRoute) {
                    val host = gateway.hostAddress
                    if (!host.isNullOrBlank()) return host
                }
            }
        }

        if (wifiManager != null) {
            try {
                val dhcp: DhcpInfo? = wifiManager.dhcpInfo
                if (dhcp != null && dhcp.gateway != 0) {
                    @Suppress("DEPRECATION")
                    return Formatter.formatIpAddress(dhcp.gateway)
                }
            } catch (_: Exception) {}
        }

        return "غير متوفر"
    }
}
