package com.example.data.repository

import com.example.core.common.Resource
import com.example.core.model.AppUsageItem
import com.example.core.model.DeviceProfile
import com.example.core.model.QuotaInfo
import com.example.core.model.QuotaPolicy
import com.example.core.model.UsageSnapshot
import com.example.feature.home.DayUsage
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Quota and Wi-Fi data operations.
 */
interface QuotaRepository {
    fun getQuotaInfo(): Flow<Resource<QuotaInfo>>
    fun getQuotaPolicy(): Flow<QuotaPolicy?>
    suspend fun updateQuotaLimit(quotaGb: Float)
    suspend fun setBlockedStatus(isBlocked: Boolean)
}

/**
 * Repository interface for App Usage Statistics operations.
 */
interface UsageStatsRepository {
    fun getTopConsumingApps(): Flow<Resource<List<AppUsageItem>>>
    fun getDailyUsageTrend(): Flow<List<DayUsage>>
    suspend fun recordUsageSnapshot(): UsageSnapshot?
}

/**
 * Repository interface for Device Registration & Profile operations.
 */
interface DeviceProfileRepository {
    fun getDeviceProfile(): Flow<DeviceProfile?>
    suspend fun saveDeviceProfile(profile: DeviceProfile): Boolean
    suspend fun clearProfile()
    suspend fun syncWithRemote(): Boolean
}
