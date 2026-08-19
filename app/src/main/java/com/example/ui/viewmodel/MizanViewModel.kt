package com.example.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.common.NetworkInfoProvider
import com.example.core.common.PermissionHelper
import com.example.core.common.Resource
import com.example.core.model.AppState
import com.example.core.model.AppUsageItem
import com.example.core.model.DeviceProfile
import com.example.core.model.QuotaInfo
import com.example.data.datasource.AndroidNetworkStatsDataSource
import com.example.data.local.DevicePreferencesDataSource
import com.example.data.repository.MizanRepositoryImpl
import com.example.feature.home.DayUsage
import com.example.feature.home.HomeTab
import com.example.feature.home.HomeUiState
import com.example.feature.setup.SetupUiState
import com.example.service.QuotaVpnService
import com.example.service.UsageTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class MizanViewModel(
    private val context: Context,
    val repository: MizanRepositoryImpl = MizanRepositoryImpl(context)
) : ViewModel() {

    private val networkStatsDataSource = AndroidNetworkStatsDataSource(context)
    private val preferences = DevicePreferencesDataSource(context)

    private val _appState = MutableStateFlow<AppState>(AppState.SessionRestoring)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private val _setupUiState = MutableStateFlow(SetupUiState())
    val setupUiState: StateFlow<SetupUiState> = _setupUiState.asStateFlow()

    private val _isVpnConsentGranted = MutableStateFlow(false)
    val isVpnConsentGranted: StateFlow<Boolean> = _isVpnConsentGranted.asStateFlow()

    private val _isDeviceAdminActive = MutableStateFlow(false)
    val isDeviceAdminActive: StateFlow<Boolean> = _isDeviceAdminActive.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    init {
        checkPermissions()
        restoreSessionAndProfile()
        observeUsageData()
        observeVpnAndAdminStates()
    }

    fun checkPermissions() {
        val permState = PermissionHelper.checkAllPermissions(context)
        _hasUsagePermission.value = permState.hasUsageAccess
        _homeUiState.update { it.copy(permissionsState = permState) }
    }

    fun restoreSessionAndProfile() {
        viewModelScope.launch {
            _appState.value = AppState.SessionRestoring
            val token = repository.authRepository.getValidAccessToken()
            val userId = repository.authRepository.getUserId()

            if (token == null || userId == null) {
                _appState.value = AppState.SignedOut
                return@launch
            }

            val savedName = repository.authRepository.getUserDisplayName() ?: "عضو العائلة"
            val savedEmail = repository.authRepository.getUserEmail() ?: ""
            val savedPhoto = repository.authRepository.getUserPhotoUrl() ?: ""

            preferences.deviceProfileFlow.collectLatest { profile ->
                if (profile == null) {
                    val householdId = repository.authRepository.fetchHouseholdMembership()
                    if (householdId != null) {
                        completeDeviceRegistration(householdId)
                    } else {
                        _appState.value = AppState.WaitingForInvite
                    }
                } else {
                    _homeUiState.update {
                        it.copy(
                            userName = savedName,
                            userEmail = savedEmail,
                            userPhotoUrl = savedPhoto,
                            householdId = profile.householdId,
                            deviceModel = profile.deviceModel,
                            quotaGb = profile.quotaLimitGb,
                            isVpnConsentGranted = profile.isVpnEnforcementEnabled,
                            isDeviceAdminActive = profile.isDeviceAdminActive
                        )
                    }
                    checkPermissionsAndEnforcement(profile)
                }
            }
        }
    }

    fun signInWithGoogle(
        idToken: String,
        rawNonce: String? = null,
        displayName: String? = null,
        photoUrl: String? = null
    ) {
        viewModelScope.launch {
            _appState.value = AppState.SigningIn
            val authResult = repository.authRepository.signInWithGoogleIdToken(
                idToken = idToken,
                rawNonce = rawNonce,
                displayName = displayName,
                photoUrl = photoUrl
            )
            if (authResult.success) {
                val resolvedName = displayName ?: repository.authRepository.getUserDisplayName() ?: "عضو العائلة"
                val resolvedEmail = repository.authRepository.getUserEmail() ?: ""
                val resolvedPhoto = photoUrl ?: repository.authRepository.getUserPhotoUrl() ?: ""
                _homeUiState.update {
                    it.copy(
                        userName = resolvedName,
                        userEmail = resolvedEmail,
                        userPhotoUrl = resolvedPhoto
                    )
                }
                val householdId = repository.authRepository.fetchHouseholdMembership()
                if (householdId != null) {
                    completeDeviceRegistration(householdId)
                } else {
                    _appState.value = AppState.WaitingForInvite
                }
            } else {
                _appState.value = AppState.AuthError(
                    authResult.errorMessage ?: "فشل تسجيل الدخول باستخدام Google أو التحقق من الجلسة"
                )
            }
        }
    }

    fun onSignInCancelled() {
        if (_appState.value is AppState.SigningIn || _appState.value is AppState.SessionRestoring) {
            _appState.value = AppState.SignedOut
        }
    }

    fun onSignInError(errorMessage: String) {
        _appState.value = AppState.AuthError(errorMessage)
    }

    fun handleDeepLinkInvite(token: String) {
        viewModelScope.launch {
            _appState.value = AppState.JoiningHousehold
            val success = repository.authRepository.acceptInvite(token)
            if (success) {
                val householdId = repository.authRepository.fetchHouseholdMembership()
                if (householdId != null) {
                    completeDeviceRegistration(householdId)
                } else {
                    _appState.value = AppState.InviteError("تم قبول الدعوة ولكن فشل العثور على المنزل")
                }
            } else {
                _appState.value = AppState.InviteError("رابط الدعوة غير صالح أو منتهي الصلاحية")
            }
        }
    }

    private suspend fun completeDeviceRegistration(householdId: String) {
        _appState.value = AppState.DeviceLinking
        val userId = repository.authRepository.getUserId() ?: return

        val deviceId = MizanRepositoryImpl.getAndroidId(context)
        val model = Build.MODEL ?: "Android Device"
        val manufacturer = Build.MANUFACTURER ?: "Unknown"
        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        val networkDetails = NetworkInfoProvider.getConnectedNetworkDetails(context)
        val detectedSsid = if (networkDetails.isWifi && networkDetails.ssid.isNotBlank()) {
            networkDetails.ssid
        } else {
            ""
        }

        val profile = DeviceProfile(
            deviceKey = deviceId,
            userId = userId,
            householdId = householdId,
            deviceModel = model,
            manufacturer = manufacturer,
            osVersion = osVersion,
            homeSsid = detectedSsid,
            quotaLimitGb = 0f,
            currentUsageGb = 0f,
            isBlocked = false,
            isActive = true
        )

        val success = repository.saveDeviceProfile(profile)
        if (success) {
            UsageTrackingService.start(context)
            refreshUsage()
        } else {
            _appState.value = AppState.NetworkError("فشل في ربط الجهاز بخوادم Mizan")
        }
    }

    private fun observeUsageData() {
        viewModelScope.launch {
            combine(
                repository.getQuotaInfo(),
                repository.getTopConsumingApps(),
                repository.getDailyUsageTrend(),
                preferences.deviceProfileFlow
            ) { quotaRes, appsRes, trend, profile ->
                if (profile == null) return@combine

                val permState = PermissionHelper.checkAllPermissions(context)
                _hasUsagePermission.value = permState.hasUsageAccess
                val networkDetails = NetworkInfoProvider.getConnectedNetworkDetails(context)

                val quota = when (quotaRes) {
                    is Resource.Success -> quotaRes.data
                    else -> QuotaInfo(
                        usedGigabytes = profile.currentUsageGb,
                        totalGigabytes = profile.quotaLimitGb,
                        remainingGigabytes = max(0f, profile.quotaLimitGb - profile.currentUsageGb),
                        usagePercentage = if (profile.quotaLimitGb > 0f) {
                            ((profile.currentUsageGb / profile.quotaLimitGb) * 100).toInt().coerceIn(0, 100)
                        } else 0
                    )
                }

                val realApps = when (appsRes) {
                    is Resource.Success -> appsRes.data
                    else -> emptyList()
                }

                _homeUiState.update { current ->
                    current.copy(
                        userName = "عضو العائلة",
                        usedGb = quota.usedGigabytes,
                        quotaGb = quota.totalGigabytes,
                        remainingGb = quota.remainingGigabytes,
                        percentage = quota.usagePercentage,
                        networkSsid = networkDetails.ssid,
                        connectionStatus = if (networkDetails.isConnected) {
                            "متصل بشبكة ${networkDetails.ssid}"
                        } else {
                            "غير متصل بالإنترنت"
                        },
                        networkDetails = networkDetails,
                        permissionsState = permState,
                        dailyAverageGb = quota.dailyAverageGb,
                        dailyTrend = if (trend.isNotEmpty()) trend else current.dailyTrend,
                        appUsage = realApps
                    )
                }

                val isExhausted = (quota.usedGigabytes >= quota.totalGigabytes && quota.totalGigabytes > 0f) || profile.isBlocked
                if (isExhausted) {
                    _appState.value = AppState.QuotaExhausted(
                        reason = if (profile.isBlocked) "تم إيقاف الاتصال من قبل إدارة الشبكة" else "اكتملت حصتك الشهرية المحددة"
                    )
                    if (profile.isVpnEnforcementEnabled && QuotaVpnService.isVpnPrepared(context)) {
                        QuotaVpnService.start(context)
                    }
                } else {
                    _appState.value = AppState.Ready(
                        quotaInfo = quota,
                        topApps = realApps,
                        dailyTrend = trend,
                        networkDetails = networkDetails
                    )
                    QuotaVpnService.stop(context)
                }
            }.collectLatest {}
        }
    }

    private fun observeVpnAndAdminStates() {
        viewModelScope.launch {
            preferences.isVpnConsentFlow.collect { _isVpnConsentGranted.value = it }
        }
    }

    fun refreshUsage() {
        checkPermissions()
        viewModelScope.launch {
            val details = NetworkInfoProvider.getConnectedNetworkDetails(context)
            val permState = PermissionHelper.checkAllPermissions(context)
            _homeUiState.update {
                it.copy(
                    networkSsid = details.ssid,
                    connectionStatus = if (details.isConnected) "متصل بشبكة ${details.ssid}" else "غير متصل",
                    networkDetails = details,
                    permissionsState = permState
                )
            }
            repository.recordUsageSnapshot()
            repository.syncWithRemote()
        }
    }

    fun onTabSelected(tab: HomeTab) {
        _homeUiState.update { it.copy(selectedTab = tab) }
    }

    fun onShowNetworkDetails(show: Boolean) {
        if (show) refreshUsage()
        _homeUiState.update { it.copy(showNetworkDetailsSheet = show) }
    }

    fun onShowPermissionsHub(show: Boolean) {
        if (show) checkPermissions()
        _homeUiState.update { it.copy(showPermissionsSheet = show) }
    }

    fun onShowProfileSheet(show: Boolean) {
        _homeUiState.update { it.copy(showProfileSheet = show) }
    }

    fun updateQuotaLimit(quotaGb: Float) {
        viewModelScope.launch {
            preferences.updateQuotaLimit(quotaGb)
            _homeUiState.update {
                val remaining = max(0f, quotaGb - it.usedGb)
                val pct = if (quotaGb > 0f) ((it.usedGb / quotaGb) * 100).toInt().coerceIn(0, 100) else 0
                it.copy(quotaGb = quotaGb, remainingGb = remaining, percentage = pct)
            }
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = PermissionHelper.createUsageAccessIntent()
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = PermissionHelper.createBatteryOptimizationIntent(context)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openOverlaySettings(context: Context) {
        try {
            val intent = PermissionHelper.createOverlayPermissionIntent(context)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun lockCurrentHomeRouter(bssid: String, ssid: String) {
        viewModelScope.launch {
            if (bssid.isNotBlank()) {
                preferences.setHomeBssid(bssid)
            }
            refreshUsage()
        }
    }

    fun setVpnConsentGranted(granted: Boolean) {
        viewModelScope.launch {
            preferences.setVpnConsent(granted)
            _isVpnConsentGranted.value = granted
            checkPermissions()
        }
    }

    fun setDeviceAdminEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setDeviceAdminEnabled(enabled)
            _isDeviceAdminActive.value = enabled
            checkPermissions()
        }
    }

    fun checkPermissionsAndEnforcement(profile: DeviceProfile) {
        if (profile.isBlocked) {
            _appState.value = AppState.QuotaExhausted("تم إيقاف الاتصال من قبل مسؤول الشبكة")
        }
    }

    fun unlinkDevice() {
        viewModelScope.launch {
            UsageTrackingService.stop(context)
            QuotaVpnService.stop(context)
            repository.clearProfile()
            _appState.value = AppState.SignedOut
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MizanViewModel(context.applicationContext) as T
        }
    }
}
