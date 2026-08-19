package com.example.feature.home

import com.example.core.common.NetworkDetails
import com.example.core.model.AppUsageItem

/**
 * Data point for the 7-day usage chart.
 */
data class DayUsage(
    val dayLabel: String,
    val valueGb: Float
)

/**
 * UI State for the Mizan Home / Usage dashboard screen.
 */
data class HomeUiState(
    val userName: String = "",
    val userEmail: String = "",
    val userPhotoUrl: String = "",
    val householdId: String = "",
    val deviceModel: String = "",
    val usedGb: Float = 0f,
    val quotaGb: Float = 133.3f,
    val remainingGb: Float = 133.3f,
    val percentage: Int = 0,
    val networkSsid: String = "شبكة Wi-Fi",
    val connectionStatus: String = "جاري الفحص...",
    val networkDetails: NetworkDetails = NetworkDetails(
        ssid = "شبكة Wi-Fi",
        isConnected = false,
        isWifi = true,
        signalPercentage = 0,
        signalDbm = 0,
        linkSpeedMbps = 0,
        frequencyGhz = "تلقائي",
        ipAddress = "غير متوفر",
        gateway = "غير متوفر",
        securityType = "WPA2/WPA3",
        isMetered = false,
        statusText = "جاري الاتصال"
    ),
    val showNetworkDetailsSheet: Boolean = false,
    val showPermissionsSheet: Boolean = false,
    val showProfileSheet: Boolean = false,
    val hasUnreadNotifications: Boolean = true,
    val isVpnConsentGranted: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val permissionsState: com.example.core.common.MizanPermissionsState = com.example.core.common.MizanPermissionsState(),
    val dailyAverageGb: Float = 0f,
    val dailyTrend: List<DayUsage> = emptyList(),
    val appUsage: List<AppUsageItem> = emptyList(),
    val selectedTab: HomeTab = HomeTab.Home
)

/**
 * The 3 core tabs of MIZAN.
 */
enum class HomeTab {
    Account,
    Usage,
    Home
}

