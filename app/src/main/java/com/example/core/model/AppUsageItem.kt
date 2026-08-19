package com.example.core.model

/**
 * Represents consumption data per application.
 *
 * TODO: Populate via system stats in future network integration phase.
 */
data class AppUsageItem(
    val id: String,
    val appName: String,
    val packageName: String,
    val consumedGb: Float,
    val iconResId: Int? = null
)
