package com.example.core.model

/**
 * Snapshot of network data consumption at a point in time.
 */
data class UsageSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val consumedGb: Float = 0f,
    val ssid: String = "",
    val isHomeWifi: Boolean = false,
    val appSnapshots: List<AppUsageItem> = emptyList()
) {
    companion object {
        const val BYTES_PER_GB = 1024f * 1024f * 1024f

        fun bytesToGb(bytes: Long): Float {
            if (bytes <= 0L) return 0f
            val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            return (Math.round(gb * 10000.0) / 10000.0).toFloat()
        }

        fun bytesToExactGb(bytes: Long): Float {
            if (bytes <= 0L) return 0f
            return (bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)).toFloat()
        }
    }
}
