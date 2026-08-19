package com.example.core.model

/**
 * Quota and Wi-Fi data consumption model skeleton.
 *
 * TODO: Populate from NetworkStatsManager / Remote sync in implementation phases.
 */
data class QuotaInfo(
    val usedGigabytes: Float = 0f,
    val totalGigabytes: Float = 0f,
    val remainingGigabytes: Float = 0f,
    val usagePercentage: Int = 0,
    val isConnectedToHomeWifi: Boolean = false,
    val dailyAverageGb: Float = 0f
)
