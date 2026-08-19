package com.example.core.model

/**
 * Represents the persistent client device profile linked to Mizan with Google Auth & Household.
 */
data class DeviceProfile(
    val deviceKey: String,
    val userId: String,
    val householdId: String,
    val deviceModel: String,
    val manufacturer: String,
    val osVersion: String,
    val homeSsid: String = "Mizan-Home-5G",
    val quotaLimitGb: Float = 133.3f,
    val currentUsageGb: Float = 0f,
    val isBlocked: Boolean = false,
    val isVpnEnforcementEnabled: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val isActive: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
