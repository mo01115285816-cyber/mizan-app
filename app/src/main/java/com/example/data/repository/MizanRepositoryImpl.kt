package com.example.data.repository

import android.content.Context
import android.net.NetworkCapabilities
import android.provider.Settings
import com.example.core.common.NetworkInfoProvider
import com.example.core.common.Resource
import com.example.core.model.AppUsageItem
import com.example.core.model.DeviceProfile
import com.example.core.model.QuotaInfo
import com.example.core.model.QuotaPolicy
import com.example.core.model.UsageSnapshot
import com.example.data.datasource.AndroidNetworkStatsDataSource
import com.example.data.local.DevicePreferencesDataSource
import com.example.data.remote.SupabaseAuthRepository
import com.example.data.remote.SupabaseDeviceDataSource
import com.example.feature.home.DayUsage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max

class MizanRepositoryImpl(
    private val context: Context,
    private val preferencesDataSource: DevicePreferencesDataSource = DevicePreferencesDataSource(context),
    private val networkStatsDataSource: AndroidNetworkStatsDataSource = AndroidNetworkStatsDataSource(context),
    val authRepository: SupabaseAuthRepository = SupabaseAuthRepository(context),
    private val supabaseDataSource: SupabaseDeviceDataSource = SupabaseDeviceDataSource(authRepository)
) : QuotaRepository, UsageStatsRepository, DeviceProfileRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Start listening to realtime updates once device is registered
        scope.launch {
            preferencesDataSource.deviceProfileFlow.collect { profile ->
                if (profile != null && profile.deviceKey.isNotBlank()) {
                    supabaseDataSource.startRealtimeSubscription(profile.deviceKey, profile.householdId)
                }
            }
        }

        // Apply realtime quota and Target SSID updates as soon as the dashboard changes them.
        scope.launch {
            supabaseDataSource.realtimePolicyUpdates.collect { updatedPolicy ->
                preferencesDataSource.updateQuotaLimit(updatedPolicy.monthlyLimitGb)
                preferencesDataSource.setBlockedStatus(updatedPolicy.isBlocked)
            }
        }
        scope.launch {
            supabaseDataSource.realtimeTargetSsidUpdates.collect { targetSsid ->
                preferencesDataSource.setTargetSsid(targetSsid)
                preferencesDataSource.setHomeBssid("")
            }
        }
    }

    override fun getDeviceProfile(): Flow<DeviceProfile?> {
        return preferencesDataSource.deviceProfileFlow
    }

    override suspend fun saveDeviceProfile(profile: DeviceProfile): Boolean {
        // The device must be accepted by Supabase before it is marked linked locally.
        val remoteSuccess = supabaseDataSource.upsertDevice(profile)
        if (!remoteSuccess) return false

        preferencesDataSource.saveDeviceProfile(profile)
        val targetSsid = supabaseDataSource.fetchTargetSsid(profile.householdId)
        if (targetSsid != null) preferencesDataSource.setTargetSsid(targetSsid)

        // Fairness baseline: capture only Wi-Fi counters at the exact moment
        // the device is linked. Pre-activation traffic never enters Mizan quota.
        val now = System.currentTimeMillis()
        val currentMonth = monthKey(now)
        val existingBaseline = preferencesDataSource.getWifiBaseline()
        if (!existingBaseline.initialized || existingBaseline.monthKey != currentMonth) {
            if (networkStatsDataSource.hasUsageStatsPermission()) {
                val (rx, tx) = networkStatsDataSource.queryTotalWifiUsage(monthStart(now), now)
                preferencesDataSource.setWifiBaseline(rx, tx, now, currentMonth)
            } else {
                // The first valid read after Usage Access is granted becomes baseline.
                preferencesDataSource.resetWifiUsageBaseline()
            }
        }

        // Quota policy belongs to the household administrator. Fetch it after linking
        // so registration is not blocked by a second sequential network request.
        scope.launch {
            val remotePolicy = supabaseDataSource.fetchQuotaPolicy(profile.deviceKey)
            if (remotePolicy != null) {
                preferencesDataSource.updateQuotaLimit(remotePolicy.monthlyLimitGb)
                preferencesDataSource.setBlockedStatus(remotePolicy.isBlocked)
            }
        }

        return true
    }

    override suspend fun clearProfile() {
        authRepository.logout()
        preferencesDataSource.clear()
        supabaseDataSource.stopRealtimeSubscription()
    }

    override suspend fun syncWithRemote(): Boolean {
        val profile = getDeviceProfileSync() ?: return false
        val quotaPolicy = supabaseDataSource.fetchQuotaPolicy(profile.deviceKey)
        if (quotaPolicy != null) {
            preferencesDataSource.updateQuotaLimit(quotaPolicy.monthlyLimitGb)
            preferencesDataSource.setBlockedStatus(quotaPolicy.isBlocked)
        }
        return supabaseDataSource.upsertDevice(profile)
    }

    override fun getQuotaPolicy(): Flow<QuotaPolicy?> = flow {
        val profile = getDeviceProfileSync()
        if (profile != null) {
            val remotePolicy = supabaseDataSource.fetchQuotaPolicy(profile.deviceKey)
            emit(remotePolicy ?: QuotaPolicy(
                deviceId = profile.deviceKey,
                monthlyLimitGb = profile.quotaLimitGb,
                homeSsid = profile.homeSsid,
                isBlocked = profile.isBlocked
            ))
        } else {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun updateQuotaLimit(quotaGb: Float) {
        preferencesDataSource.updateQuotaLimit(quotaGb)
    }

    override suspend fun setBlockedStatus(isBlocked: Boolean) {
        preferencesDataSource.setBlockedStatus(isBlocked)
    }

    override fun getQuotaInfo(): Flow<Resource<QuotaInfo>> = flow {
        emit(Resource.Loading)
        try {
            val hasPermission = networkStatsDataSource.hasUsageStatsPermission()
            val networkDetails = NetworkInfoProvider.getConnectedNetworkDetails(context)
            val profile = getDeviceProfileSync()
            val totalGb = profile?.quotaLimitGb ?: 0f
            val homeSsid = profile?.homeSsid ?: ""
            val targetSsid = preferencesDataSource.targetSsidFlow.first().ifBlank { homeSsid }
            val homeBssid = preferencesDataSource.homeBssidFlow.first()
            val baseline = preferencesDataSource.getWifiBaseline()

            val isHome = networkDetails.isWifi && (
                (homeBssid.isNotBlank() && networkDetails.bssid.equals(homeBssid, ignoreCase = true)) ||
                (targetSsid.isNotBlank() && networkDetails.ssid.equals(targetSsid, ignoreCase = true))
            )

            // Auto-lock router BSSID on first confirmed home connection
            if (isHome && homeBssid.isBlank() && networkDetails.bssid.isNotBlank()) {
                preferencesDataSource.setHomeBssid(networkDetails.bssid)
            }

            // The quota cycle is anchored to the Wi-Fi baseline, not the
            // historical beginning-of-month counter.
            val calendar = Calendar.getInstance()
            var usedGb: Float
            if (hasPermission) {
                // Record snapshot & compute delta
                val snapshot = recordUsageSnapshot()
                val accumulatedBytes = getAccumulatedHomeBytesSync()
                usedGb = if (accumulatedBytes > 0L) {
                    UsageSnapshot.bytesToGb(accumulatedBytes)
                } else {
                    snapshot?.consumedGb ?: 0f
                }
            } else {
                usedGb = 0f
            }

            preferencesDataSource.updateCurrentUsage(usedGb)

            val remainingGb = max(0f, totalGb - usedGb)
            val usagePercent = if (totalGb > 0f) {
                ((usedGb / totalGb) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            val now = System.currentTimeMillis()
            val activeStart = if (baseline.initialized && baseline.monthKey == monthKey(now)) {
                baseline.timestamp.coerceAtMost(now)
            } else {
                now
            }
            val activeDays = (((now - activeStart).coerceAtLeast(0L)) / 86_400_000L)
                .toInt()
                .plus(1)
                .coerceAtLeast(1)
            val dailyAvg = if (usedGb > 0f) {
                (Math.round((usedGb / activeDays.toFloat()) * 10000.0) / 10000.0).toFloat()
            } else {
                0f
            }

            val info = QuotaInfo(
                usedGigabytes = (Math.round(usedGb * 10000.0) / 10000.0).toFloat(),
                totalGigabytes = totalGb,
                remainingGigabytes = (Math.round(remainingGb * 10000.0) / 10000.0).toFloat(),
                usagePercentage = usagePercent,
                isConnectedToHomeWifi = isHome,
                dailyAverageGb = dailyAvg
            )
            emit(Resource.Success(info))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "فشل في قراءة بيانات الاستهلاك"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getTopConsumingApps(): Flow<Resource<List<AppUsageItem>>> = flow {
        emit(Resource.Loading)
        try {
            val hasPermission = networkStatsDataSource.hasUsageStatsPermission()
            if (!hasPermission) {
                emit(Resource.Success(emptyList()))
                return@flow
            }

            val baseline = preferencesDataSource.getWifiBaseline()
            if (!baseline.initialized) {
                emit(Resource.Success(emptyList()))
                return@flow
            }

            val endTime = System.currentTimeMillis()
            val startTime = baseline.timestamp.coerceAtMost(endTime)
            val apps = networkStatsDataSource.queryTopAppsUsage(
                startTime = startTime,
                endTime = endTime,
                networkType = NetworkCapabilities.TRANSPORT_WIFI,
                limit = 8
            )
            emit(Resource.Success(apps))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "فشل في قراءة إحصاءات التطبيقات"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getDailyUsageTrend(): Flow<List<DayUsage>> = flow {
        val baseline = preferencesDataSource.getWifiBaseline()
        val trend = networkStatsDataSource.query7DayTrend(
            startFrom = if (baseline.initialized) baseline.timestamp else Long.MAX_VALUE
        )
        emit(trend)
    }.flowOn(Dispatchers.IO)

    override suspend fun recordUsageSnapshot(): UsageSnapshot? {
        val hasPermission = networkStatsDataSource.hasUsageStatsPermission()
        if (!hasPermission) return null

        val networkDetails = NetworkInfoProvider.getConnectedNetworkDetails(context)
        val profile = getDeviceProfileSync()
        val homeSsid = profile?.homeSsid ?: ""
        val targetSsid = preferencesDataSource.targetSsidFlow.first().ifBlank { homeSsid }
        val homeBssid = preferencesDataSource.homeBssidFlow.first()

        val isHome = networkDetails.isWifi && (
            (homeBssid.isNotBlank() && networkDetails.bssid.equals(homeBssid, ignoreCase = true)) ||
            (targetSsid.isNotBlank() && networkDetails.ssid.equals(targetSsid, ignoreCase = true))
        )

        // Auto-lock router BSSID on first confirmed target-network connection
        if (isHome && homeBssid.isBlank() && networkDetails.bssid.isNotBlank()) {
            preferencesDataSource.setHomeBssid(networkDetails.bssid)
        }

        if (!isHome) {
            return UsageSnapshot(
                timestamp = System.currentTimeMillis(),
                uploadBytes = 0L,
                downloadBytes = 0L,
                totalBytes = 0L,
                consumedGb = UsageSnapshot.bytesToGb(getAccumulatedHomeBytesSync()),
                ssid = networkDetails.ssid,
                isHomeWifi = false,
                appSnapshots = emptyList()
            )
        }

        val endTime = System.currentTimeMillis()
        val currentMonth = monthKey(endTime)
        val monthStart = monthStart(endTime)
        val (rx, tx) = networkStatsDataSource.queryTotalWifiUsage(monthStart, endTime)
        val currentTotalWifi = rx + tx
        val baseline = preferencesDataSource.getWifiBaseline()

        // A missing or stale baseline is initialized now and contributes zero usage.
        if (!baseline.initialized || baseline.monthKey != currentMonth) {
            preferencesDataSource.setWifiBaseline(rx, tx, endTime, currentMonth)
            val baselineSnapshot = UsageSnapshot(
                timestamp = endTime,
                uploadBytes = 0L,
                downloadBytes = 0L,
                totalBytes = 0L,
                consumedGb = 0f,
                ssid = networkDetails.ssid,
                gatewayIp = networkDetails.gateway,
                wifiBand = networkDetails.frequencyGhz,
                securityType = networkDetails.securityType,
                signalPercent = networkDetails.signalPercentage,
                linkSpeedMbps = networkDetails.linkSpeedMbps,
                trackingStartedAt = endTime,
                baselineRxBytes = rx,
                baselineTxBytes = tx,
                isHomeWifi = isHome,
                appSnapshots = emptyList()
            )
            if (profile != null && profile.deviceKey.isNotBlank()) {
                supabaseDataSource.syncUsageSnapshot(profile.deviceKey, baselineSnapshot)
            }
            return baselineSnapshot
        }

        val lastKnownTotal = preferencesDataSource.getLastKnownTotalWifiBytes()
        val delta = (currentTotalWifi - lastKnownTotal).coerceAtLeast(0L)
        if (isHome && delta > 0L) {
            preferencesDataSource.addAccumulatedHomeBytes(delta)
        }
        preferencesDataSource.updateLastKnownTotalWifiBytes(currentTotalWifi)

        val topApps = networkStatsDataSource.queryTopAppsUsage(
            startTime = baseline.timestamp.coerceAtLeast(monthStart),
            endTime = endTime,
            networkType = NetworkCapabilities.TRANSPORT_WIFI,
            limit = 5
        )
        val accumulatedBytes = getAccumulatedHomeBytesSync()
        val cumulativeSinceActivation =
            (rx - baseline.rxBytes).coerceAtLeast(0L) +
                (tx - baseline.txBytes).coerceAtLeast(0L)
        val consumedGb = UsageSnapshot.bytesToGb(accumulatedBytes)

        val snapshot = UsageSnapshot(
            timestamp = endTime,
            uploadBytes = (tx - baseline.txBytes).coerceAtLeast(0L),
            downloadBytes = (rx - baseline.rxBytes).coerceAtLeast(0L),
            totalBytes = cumulativeSinceActivation,
            consumedGb = consumedGb,
            ssid = networkDetails.ssid,
            gatewayIp = networkDetails.gateway,
            wifiBand = networkDetails.frequencyGhz,
            securityType = networkDetails.securityType,
            signalPercent = networkDetails.signalPercentage,
            linkSpeedMbps = networkDetails.linkSpeedMbps,
            trackingStartedAt = baseline.timestamp,
            baselineRxBytes = baseline.rxBytes,
            baselineTxBytes = baseline.txBytes,
            isHomeWifi = isHome,
            appSnapshots = topApps
        )

        // Sync with Supabase if device is registered
        if (profile != null && profile.deviceKey.isNotBlank()) {
            supabaseDataSource.syncUsageSnapshot(profile.deviceKey, snapshot)
        }

        return snapshot
    }

    private suspend fun getAccumulatedHomeBytesSync(): Long {
        return preferencesDataSource.accumulatedBytesFlow.first()
    }

    private fun monthStart(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun monthKey(timeMillis: Long): String {
        return Calendar.getInstance().apply { timeInMillis = timeMillis }
            .let { "${it.get(Calendar.YEAR)}-${it.get(Calendar.MONTH) + 1}" }
    }

    private suspend fun getDeviceProfileSync(): DeviceProfile? {
        return preferencesDataSource.deviceProfileFlow.first()
    }

    companion object {
        fun getAndroidId(context: Context): String {
            return try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    ?: "android_${System.currentTimeMillis()}"
            } catch (_: Exception) {
                "android_${System.currentTimeMillis()}"
            }
        }
    }
}
