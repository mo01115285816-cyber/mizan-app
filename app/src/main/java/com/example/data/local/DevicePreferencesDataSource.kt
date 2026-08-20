package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.DeviceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mizan_settings")

data class WifiBaseline(
    val rxBytes: Long,
    val txBytes: Long,
    val timestamp: Long,
    val monthKey: String,
    val initialized: Boolean
)

class DevicePreferencesDataSource(private val context: Context) {

    companion object {
        val KEY_DEVICE_KEY = stringPreferencesKey("device_key")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_HOUSEHOLD_ID = stringPreferencesKey("household_id")
        val KEY_IS_LINKED = booleanPreferencesKey("is_linked")
        val KEY_HOME_SSID = stringPreferencesKey("home_ssid")
        val KEY_TARGET_SSID = stringPreferencesKey("target_ssid")
        val KEY_HOME_BSSID = stringPreferencesKey("home_bssid")
        val KEY_QUOTA_LIMIT_GB = floatPreferencesKey("quota_limit_gb")
        val KEY_CURRENT_USAGE_GB = floatPreferencesKey("current_usage_gb")
        val KEY_ACCUMULATED_HOME_BYTES = longPreferencesKey("accumulated_home_bytes")
        val KEY_LAST_KNOWN_TOTAL_WIFI_BYTES = longPreferencesKey("last_known_total_wifi_bytes")
        val KEY_WIFI_BASELINE_BYTES = longPreferencesKey("wifi_baseline_bytes")
        val KEY_WIFI_BASELINE_RX_BYTES = longPreferencesKey("wifi_baseline_rx_bytes")
        val KEY_WIFI_BASELINE_TX_BYTES = longPreferencesKey("wifi_baseline_tx_bytes")
        val KEY_WIFI_BASELINE_TIME = longPreferencesKey("wifi_baseline_time")
        val KEY_WIFI_BASELINE_MONTH = stringPreferencesKey("wifi_baseline_month")
        val KEY_WIFI_BASELINE_INITIALIZED = booleanPreferencesKey("wifi_baseline_initialized")
        val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val KEY_IS_BLOCKED = booleanPreferencesKey("is_blocked")
        val KEY_REMOTE_ENFORCE_VPN_BLOCK = booleanPreferencesKey("remote_enforce_vpn_block")
        val KEY_IS_VPN_CONSENT_GRANTED = booleanPreferencesKey("is_vpn_consent_granted")
        val KEY_IS_DEVICE_ADMIN_ENABLED = booleanPreferencesKey("is_device_admin_enabled")
        val KEY_IS_ACTIVE = booleanPreferencesKey("is_active")
        val KEY_USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
        val KEY_WARNING_THRESHOLD = floatPreferencesKey("warning_threshold")
        val KEY_ONLY_HOME_WIFI_ENFORCEMENT = booleanPreferencesKey("only_home_wifi_enforcement")
        val KEY_SERVICE_HEARTBEAT_AT = longPreferencesKey("service_heartbeat_at")
        val KEY_LAST_POLICY_SYNC_AT = longPreferencesKey("last_policy_sync_at")
        val KEY_LAST_TELEMETRY_UPLOAD_AT = longPreferencesKey("last_telemetry_upload_at")
        val KEY_VPN_STATE = stringPreferencesKey("vpn_state")
        val KEY_VPN_STATE_CHANGED_AT = longPreferencesKey("vpn_state_changed_at")
        val KEY_NETWORK_STATE = stringPreferencesKey("network_state")
        val KEY_PERMISSION_HEALTH = stringPreferencesKey("permission_health")
    }

    val userDisplayNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_DISPLAY_NAME] ?: ""
    }

    val userEmailFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_EMAIL] ?: ""
    }

    val userPhotoUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_PHOTO_URL] ?: ""
    }

    val onlyHomeWifiFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONLY_HOME_WIFI_ENFORCEMENT] ?: true
    }

    suspend fun saveUserInfo(name: String, email: String, photoUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_DISPLAY_NAME] = name
            preferences[KEY_USER_EMAIL] = email
            preferences[KEY_USER_PHOTO_URL] = photoUrl
        }
    }

    suspend fun setOnlyHomeWifiEnforcement(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONLY_HOME_WIFI_ENFORCEMENT] = enabled
        }
    }

    val deviceProfileFlow: Flow<DeviceProfile?> = context.dataStore.data.map { preferences ->
        val isLinked = preferences[KEY_IS_LINKED] ?: false
        if (!isLinked) {
            null
        } else {
            DeviceProfile(
                deviceKey = preferences[KEY_DEVICE_KEY] ?: "",
                userId = preferences[KEY_USER_ID] ?: "",
                householdId = preferences[KEY_HOUSEHOLD_ID] ?: "",
                deviceModel = android.os.Build.MODEL ?: "Android",
                manufacturer = android.os.Build.MANUFACTURER ?: "Unknown",
                osVersion = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
                homeSsid = preferences[KEY_HOME_SSID] ?: "",
                quotaLimitGb = preferences[KEY_QUOTA_LIMIT_GB] ?: 133.3f,
                currentUsageGb = preferences[KEY_CURRENT_USAGE_GB] ?: 0f,
                isBlocked = preferences[KEY_IS_BLOCKED] ?: false,
                isVpnEnforcementEnabled = preferences[KEY_IS_VPN_CONSENT_GRANTED] ?: false,
                isDeviceAdminActive = preferences[KEY_IS_DEVICE_ADMIN_ENABLED] ?: false,
                isActive = preferences[KEY_IS_ACTIVE] ?: true,
                lastSyncTimestamp = preferences[KEY_LAST_SYNC_TIME] ?: 0L
            )
        }
    }

    val accumulatedBytesFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACCUMULATED_HOME_BYTES] ?: 0L
    }

    val homeSsidFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_HOME_SSID] ?: ""
    }

    val targetSsidFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_TARGET_SSID] ?: ""
    }

    val homeBssidFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_HOME_BSSID] ?: ""
    }

    val isBlockedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_BLOCKED] ?: false
    }

    val remoteEnforceVpnBlockFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_REMOTE_ENFORCE_VPN_BLOCK] ?: true
    }

    val isVpnConsentFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_VPN_CONSENT_GRANTED] ?: false
    }

    val serviceHeartbeatAtFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_SERVICE_HEARTBEAT_AT] ?: 0L
    }

    val lastPolicySyncAtFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_POLICY_SYNC_AT] ?: 0L
    }

    val lastTelemetryUploadAtFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_TELEMETRY_UPLOAD_AT] ?: 0L
    }

    val vpnStateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_VPN_STATE] ?: "UNKNOWN"
    }

    val networkStateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_NETWORK_STATE] ?: "UNKNOWN"
    }

    val permissionHealthFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_PERMISSION_HEALTH] ?: "UNKNOWN"
    }

    suspend fun saveDeviceProfile(profile: DeviceProfile) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LINKED] = true
            preferences[KEY_DEVICE_KEY] = profile.deviceKey
            preferences[KEY_USER_ID] = profile.userId
            preferences[KEY_HOUSEHOLD_ID] = profile.householdId
            preferences[KEY_HOME_SSID] = profile.homeSsid
            if (!preferences.contains(KEY_TARGET_SSID)) preferences[KEY_TARGET_SSID] = ""
            preferences[KEY_QUOTA_LIMIT_GB] = profile.quotaLimitGb
            preferences[KEY_CURRENT_USAGE_GB] = profile.currentUsageGb
            preferences[KEY_IS_BLOCKED] = profile.isBlocked
            preferences[KEY_IS_VPN_CONSENT_GRANTED] = profile.isVpnEnforcementEnabled
            preferences[KEY_IS_DEVICE_ADMIN_ENABLED] = profile.isDeviceAdminActive
            preferences[KEY_IS_ACTIVE] = profile.isActive
            preferences[KEY_LAST_SYNC_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun updateQuotaLimit(quotaGb: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_QUOTA_LIMIT_GB] = quotaGb
        }
    }

    suspend fun updateCurrentUsage(usageGb: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENT_USAGE_GB] = usageGb
        }
    }

    suspend fun setBlockedStatus(isBlocked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_BLOCKED] = isBlocked
        }
    }

    suspend fun setRemoteEnforceVpnBlock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMOTE_ENFORCE_VPN_BLOCK] = enabled
        }
    }

    suspend fun setTargetSsid(ssid: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TARGET_SSID] = ssid.trim()
        }
    }

    suspend fun setHomeBssid(bssid: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HOME_BSSID] = bssid
        }
    }

    suspend fun setVpnConsent(granted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_VPN_CONSENT_GRANTED] = granted
        }
    }

    suspend fun setDeviceAdminEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_DEVICE_ADMIN_ENABLED] = enabled
        }
    }

    suspend fun recordServiceHeartbeat(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SERVICE_HEARTBEAT_AT] = timestamp
        }
    }

    suspend fun recordPolicySync(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_POLICY_SYNC_AT] = timestamp
        }
    }

    suspend fun recordTelemetryUpload(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_TELEMETRY_UPLOAD_AT] = timestamp
        }
    }

    suspend fun setVpnState(state: String, timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VPN_STATE] = state
            preferences[KEY_VPN_STATE_CHANGED_AT] = timestamp
        }
    }

    suspend fun setNetworkState(state: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NETWORK_STATE] = state
        }
    }

    suspend fun setPermissionHealth(state: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PERMISSION_HEALTH] = state
        }
    }

    suspend fun addAccumulatedHomeBytes(deltaBytes: Long) {
        if (deltaBytes <= 0L) return
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_ACCUMULATED_HOME_BYTES] ?: 0L
            preferences[KEY_ACCUMULATED_HOME_BYTES] = current + deltaBytes
        }
    }

    suspend fun setAccumulatedHomeBytes(totalBytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCUMULATED_HOME_BYTES] = totalBytes
        }
    }

    suspend fun updateLastKnownTotalWifiBytes(totalBytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_KNOWN_TOTAL_WIFI_BYTES] = totalBytes
        }
    }

    suspend fun getLastKnownTotalWifiBytes(): Long {
        return context.dataStore.data.first()[KEY_LAST_KNOWN_TOTAL_WIFI_BYTES] ?: 0L
    }

    suspend fun setWifiBaseline(rxBytes: Long, txBytes: Long, timestamp: Long, monthKey: String) {
        val safeRx = rxBytes.coerceAtLeast(0L)
        val safeTx = txBytes.coerceAtLeast(0L)
        context.dataStore.edit { preferences ->
            preferences[KEY_WIFI_BASELINE_RX_BYTES] = safeRx
            preferences[KEY_WIFI_BASELINE_TX_BYTES] = safeTx
            preferences[KEY_WIFI_BASELINE_BYTES] = safeRx + safeTx
            preferences[KEY_WIFI_BASELINE_TIME] = timestamp
            preferences[KEY_WIFI_BASELINE_MONTH] = monthKey
            preferences[KEY_WIFI_BASELINE_INITIALIZED] = true
            preferences[KEY_ACCUMULATED_HOME_BYTES] = 0L
            preferences[KEY_LAST_KNOWN_TOTAL_WIFI_BYTES] = safeRx + safeTx
        }
    }

    suspend fun getWifiBaseline(): WifiBaseline {
        val preferences = context.dataStore.data.first()
        return WifiBaseline(
            rxBytes = preferences[KEY_WIFI_BASELINE_RX_BYTES] ?: 0L,
            txBytes = preferences[KEY_WIFI_BASELINE_TX_BYTES] ?: 0L,
            timestamp = preferences[KEY_WIFI_BASELINE_TIME] ?: 0L,
            monthKey = preferences[KEY_WIFI_BASELINE_MONTH] ?: "",
            initialized = preferences[KEY_WIFI_BASELINE_INITIALIZED] ?: false
        )
    }

    suspend fun isWifiBaselineInitialized(): Boolean {
        return context.dataStore.data.first()[KEY_WIFI_BASELINE_INITIALIZED] ?: false
    }

    suspend fun resetWifiUsageBaseline() {
        context.dataStore.edit { preferences ->
            preferences[KEY_WIFI_BASELINE_RX_BYTES] = 0L
            preferences[KEY_WIFI_BASELINE_TX_BYTES] = 0L
            preferences[KEY_WIFI_BASELINE_BYTES] = 0L
            preferences[KEY_WIFI_BASELINE_TIME] = 0L
            preferences[KEY_WIFI_BASELINE_MONTH] = ""
            preferences[KEY_WIFI_BASELINE_INITIALIZED] = false
            preferences[KEY_ACCUMULATED_HOME_BYTES] = 0L
            preferences[KEY_LAST_KNOWN_TOTAL_WIFI_BYTES] = 0L
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
