package com.example.core.model

/**
 * Quota and enforcement policy synchronized from remote server / Supabase.
 */
data class QuotaPolicy(
    val deviceId: String,
    val monthlyLimitGb: Float = 133.3f,
    val warningThresholdPercent: Int = 85,
    val homeSsid: String = "Mizan-Home-5G",
    val enforceVpnBlock: Boolean = false,
    val isBlocked: Boolean = false,
    val reason: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
