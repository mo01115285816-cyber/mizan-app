package com.example.data.datasource

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import com.example.core.model.AppUsageItem
import com.example.core.model.UsageSnapshot
import com.example.feature.home.DayUsage
import java.util.Calendar
import java.util.Locale

class AndroidNetworkStatsDataSource(
    private val context: Context
) {
    private val networkStatsManager: NetworkStatsManager? =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    private val packageManager: PackageManager = context.packageManager

    /**
     * Checks if the app has PACKAGE_USAGE_STATS permission via AppOpsManager.
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Queries total Wi-Fi byte statistics (Upload & Download) for the current billing cycle (e.g. last 30 days).
     */
    fun queryTotalWifiUsage(startTime: Long, endTime: Long): Pair<Long, Long> {
        if (!hasUsageStatsPermission() || networkStatsManager == null) {
            return Pair(0L, 0L)
        }

        return try {
            val bucket = networkStatsManager.querySummaryForDevice(
                NetworkCapabilities.TRANSPORT_WIFI,
                null,
                startTime,
                endTime
            )
            val rxBytes = bucket.rxBytes
            val txBytes = bucket.txBytes
            Pair(rxBytes, txBytes)
        } catch (_: SecurityException) {
            Pair(0L, 0L)
        } catch (_: Exception) {
            Pair(0L, 0L)
        }
    }

    /**
     * Queries per-app Wi-Fi consumption and returns a sorted list of top apps.
     */
    fun queryTopAppsUsage(
        startTime: Long,
        endTime: Long,
        networkType: Int = NetworkCapabilities.TRANSPORT_WIFI,
        limit: Int = 20
    ): List<AppUsageItem> {
        if (!hasUsageStatsPermission() || networkStatsManager == null) {
            return emptyList()
        }

        val uidUsageMap = mutableMapOf<Int, Long>()

        val transportTypes = when (networkType) {
            NetworkCapabilities.TRANSPORT_WIFI -> listOf(NetworkCapabilities.TRANSPORT_WIFI)
            NetworkCapabilities.TRANSPORT_CELLULAR -> listOf(NetworkCapabilities.TRANSPORT_CELLULAR)
            else -> listOf(NetworkCapabilities.TRANSPORT_WIFI)
        }

        for (transport in transportTypes) {
            try {
                val stats: NetworkStats = networkStatsManager.querySummary(
                    transport,
                    null,
                    startTime,
                    endTime
                )
                val bucket = NetworkStats.Bucket()
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    val uid = bucket.uid
                    val bytes = bucket.rxBytes + bucket.txBytes
                    if (bytes > 0 && uid > 0) {
                        val current = uidUsageMap[uid] ?: 0L
                        uidUsageMap[uid] = current + bytes
                    }
                }
                stats.close()
            } catch (_: Exception) {}
        }

        // Map UIDs to human-readable App names
        val result = mutableListOf<AppUsageItem>()
        for ((uid, bytes) in uidUsageMap) {
            val packages = packageManager.getPackagesForUid(uid) ?: continue
            val primaryPackage = packages.firstOrNull() ?: continue

            val appName = try {
                val appInfo = packageManager.getApplicationInfo(primaryPackage, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                primaryPackage
            }

            val consumedGb = UsageSnapshot.bytesToGb(bytes)
            if (bytes > 0) {
                result.add(
                    AppUsageItem(
                        id = primaryPackage,
                        appName = appName,
                        packageName = primaryPackage,
                        consumedGb = consumedGb
                    )
                )
            }
        }

        return result.sortedByDescending { it.consumedGb }.take(limit)
    }

    /**
     * Queries 7-day usage trend for the last 7 days.
     */
    fun query7DayTrend(startFrom: Long? = null): List<DayUsage> {
        if (!hasUsageStatsPermission() || networkStatsManager == null) {
            return getDefaultTrend()
        }

        val trend = mutableListOf<DayUsage>()
        val calendar = Calendar.getInstance()

        // Set to end of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)

        for (i in 0 until 7) {
            val endOfDay = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val effectiveStart = if (startFrom != null) {
                maxOf(startOfDay, startFrom)
            } else {
                startOfDay
            }

            val dayLabel = getArabicDayLabel(calendar.get(Calendar.DAY_OF_WEEK))
            val (rx, tx) = if (effectiveStart < endOfDay) {
                queryTotalWifiUsage(effectiveStart, endOfDay)
            } else {
                Pair(0L, 0L)
            }
            val dayGb = UsageSnapshot.bytesToGb(rx + tx)

            trend.add(0, DayUsage(dayLabel = dayLabel, valueGb = dayGb))

            // Move to previous day
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
        }

        return trend
    }

    private fun getArabicDayLabel(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SATURDAY -> "سبت"
            Calendar.FRIDAY -> "جمعة"
            Calendar.THURSDAY -> "خميس"
            Calendar.WEDNESDAY -> "الأربعاء"
            Calendar.TUESDAY -> "الثلاثاء"
            Calendar.MONDAY -> "الاثنين"
            Calendar.SUNDAY -> "أحد"
            else -> "يوم"
        }
    }

    private fun getDefaultTrend(): List<DayUsage> {
        return listOf(
            DayUsage("سبت", 0f),
            DayUsage("جمعة", 0f),
            DayUsage("خميس", 0f),
            DayUsage("الأربعاء", 0f),
            DayUsage("الثلاثاء", 0f),
            DayUsage("الاثنين", 0f),
            DayUsage("أحد", 0f)
        )
    }
}
