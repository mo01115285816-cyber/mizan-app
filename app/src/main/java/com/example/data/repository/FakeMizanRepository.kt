package com.example.data.repository

import com.example.core.common.Resource
import com.example.core.model.AppUsageItem
import com.example.core.model.DeviceProfile
import com.example.core.model.QuotaInfo
import com.example.core.model.QuotaPolicy
import com.example.core.model.UsageSnapshot
import com.example.feature.home.DayUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake in-memory repository for UI Previews, Mock tests, and deterministic testing.
 */
class FakeMizanRepository(
    initialQuota: QuotaInfo = QuotaInfo(
        usedGigabytes = 85.3f,
        totalGigabytes = 133.3f,
        remainingGigabytes = 48.0f,
        usagePercentage = 64,
        isConnectedToHomeWifi = true,
        dailyAverageGb = 3.1f
    ),
    initialApps: List<AppUsageItem> = listOf(
        AppUsageItem("1", "TikTok", "com.zhiliaoapp.musically", 30.0f),
        AppUsageItem("2", "YouTube", "com.google.android.youtube", 15.0f),
        AppUsageItem("3", "Facebook", "com.facebook.katana", 5.0f)
    )
) : QuotaRepository, UsageStatsRepository, DeviceProfileRepository {

    private val _quotaFlow = MutableStateFlow<Resource<QuotaInfo>>(Resource.Success(initialQuota))
    private val _appsFlow = MutableStateFlow<Resource<List<AppUsageItem>>>(Resource.Success(initialApps))
    private val _profileFlow = MutableStateFlow<DeviceProfile?>(
        DeviceProfile(
            deviceKey = "mock_device_123",
            userId = "mock_user_456",
            householdId = "mock_household_789",
            deviceModel = "Pixel 8",
            manufacturer = "Google",
            osVersion = "Android 14",
            homeSsid = "Mizan-Home-5G",
            quotaLimitGb = initialQuota.totalGigabytes,
            currentUsageGb = initialQuota.usedGigabytes,
            isActive = true
        )
    )

    override fun getQuotaInfo(): Flow<Resource<QuotaInfo>> = _quotaFlow.asStateFlow()

    override fun getQuotaPolicy(): Flow<QuotaPolicy?> = flowOf(
        QuotaPolicy(
            deviceId = "mock_device_123",
            monthlyLimitGb = 133.3f,
            warningThresholdPercent = 85,
            homeSsid = "Mizan-Home-5G",
            isBlocked = false
        )
    )

    override suspend fun updateQuotaLimit(quotaGb: Float) {}

    override suspend fun setBlockedStatus(isBlocked: Boolean) {}

    override fun getTopConsumingApps(): Flow<Resource<List<AppUsageItem>>> = _appsFlow.asStateFlow()

    override fun getDailyUsageTrend(): Flow<List<DayUsage>> = flowOf(
        listOf(
            DayUsage("سبت", 1.8f),
            DayUsage("جمعة", 1.4f),
            DayUsage("خميس", 2.6f),
            DayUsage("الأربعاء", 1.9f),
            DayUsage("الثلاثاء", 2.8f),
            DayUsage("الاثنين", 3.9f),
            DayUsage("أحد", 2.3f)
        )
    )

    override suspend fun recordUsageSnapshot(): UsageSnapshot? = null

    override fun getDeviceProfile(): Flow<DeviceProfile?> = _profileFlow.asStateFlow()

    override suspend fun saveDeviceProfile(profile: DeviceProfile): Boolean {
        _profileFlow.value = profile
        return true
    }

    override suspend fun clearProfile() {
        _profileFlow.value = null
    }

    override suspend fun syncWithRemote(): Boolean = true
}
