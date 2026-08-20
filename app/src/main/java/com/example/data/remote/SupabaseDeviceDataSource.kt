package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.core.model.DeviceProfile
import com.example.core.model.QuotaPolicy
import com.example.core.model.UsageSnapshot
import com.example.core.common.NetworkDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class SupabaseDeviceDataSource(
    private val authRepository: SupabaseAuthRepository? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val tag = "SupabaseDataSource"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _realtimePolicyUpdates = MutableSharedFlow<QuotaPolicy>(extraBufferCapacity = 10)
    val realtimePolicyUpdates: SharedFlow<QuotaPolicy> = _realtimePolicyUpdates.asSharedFlow()
    private val _realtimeTargetSsidUpdates = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val realtimeTargetSsidUpdates: SharedFlow<String> = _realtimeTargetSsidUpdates.asSharedFlow()

    private var activeWebSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var subscribedDeviceKey: String? = null
    private var subscribedHouseholdId: String? = null

    companion object {
        const val DEFAULT_SUPABASE_URL = "https://ecfgmznpkyekgpqsdhhr.supabase.co"
        const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVjZmdtem5wa3lla2dwcXNkaGhyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODcwMDQxNzAsImV4cCI6MjEwMjU4MDE3MH0.oEsbiq2G9iUmS6kcGGvAFKVf9fraDDB0kmukac6XQjE"

        private fun iso8601(timestamp: Long): String {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            df.timeZone = TimeZone.getTimeZone("UTC")
            return df.format(Date(timestamp))
        }

        private fun getIso8601Timestamp(): String = iso8601(System.currentTimeMillis())
    }

    val supabaseUrl: String
        get() {
            val fromConfig = try {
                val field = BuildConfig::class.java.getField("SUPABASE_URL")
                val url = field.get(null) as? String ?: ""
                url.trimEnd('/')
            } catch (_: Exception) {
                ""
            }
            return if (fromConfig.isNotBlank() && !fromConfig.contains("placeholder")) fromConfig else DEFAULT_SUPABASE_URL
        }

    val anonKey: String
        get() {
            val fromConfig = try {
                val field = BuildConfig::class.java.getField("SUPABASE_ANON_KEY")
                field.get(null) as? String ?: ""
            } catch (_: Exception) {
                ""
            }
            return if (fromConfig.isNotBlank() && !fromConfig.contains("placeholder")) fromConfig else DEFAULT_ANON_KEY
        }

    private suspend fun getAuthBearerToken(): String {
        return authRepository?.getValidAccessToken() ?: anonKey
    }

    private val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && anonKey.isNotBlank()

    /**
     * Upserts device profile into Supabase 'devices' table.
     */
    suspend fun upsertDevice(profile: DeviceProfile, includePolicyFields: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false

        try {
            val nowIso = getIso8601Timestamp()
            val json = JSONObject().apply {
                put("device_key", profile.deviceKey)
                put("user_id", profile.userId)
                put("household_id", profile.householdId)
                put("model", profile.deviceModel)
                put("manufacturer", profile.manufacturer)
                put("os_version", profile.osVersion)
                put("home_ssid", profile.homeSsid)
                if (includePolicyFields) {
                    put("quota_limit_gb", profile.quotaLimitGb)
                    put("current_usage_gb", profile.currentUsageGb)
                    put("is_blocked", profile.isBlocked)
                    put("is_vpn_enforced", profile.isVpnEnforcementEnabled)
                }
                put("is_admin_active", profile.isDeviceAdminActive)
                put("is_active", profile.isActive)
                put("last_seen_at", nowIso)
                put("updated_at", nowIso)
            }

            val bearer = getAuthBearerToken()
            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/devices?on_conflict=device_key")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $bearer")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build()

            val response: Response = client.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()
            successful
        } catch (e: Exception) {
            Log.w(tag, "Failed to upsert device to Supabase: ${e.message}")
            false
        }
    }

    /**
     * Sends observed network telemetry only. This is intentionally separate from usage
     * snapshots so an unknown Wi-Fi network can be shown to the administrator without
     * counting any bytes before Target SSID is confirmed.
     */
    suspend fun syncNetworkTelemetry(deviceKey: String, details: NetworkDetails): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || deviceKey.isBlank() || !details.isWifi) return@withContext false
        try {
            val payload = JSONObject().apply {
                put("latest_ssid", details.ssid)
                put("latest_gateway_ip", details.gateway)
                put("latest_wifi_band", details.frequencyGhz)
                put("latest_security_type", details.securityType)
                put("latest_signal_percent", details.signalPercentage)
                put("latest_link_speed_mbps", details.linkSpeedMbps)
                put("network_updated_at", getIso8601Timestamp())
                put("last_seen_at", getIso8601Timestamp())
            }
            val response = client.newCall(Request.Builder()
                .url("$supabaseUrl/rest/v1/devices?device_key=eq.$deviceKey")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer ${getAuthBearerToken()}")
                .header("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()).execute()
            response.use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync network telemetry: ${e.message}")
            false
        }
    }

    /**
     * Sends usage snapshot to Supabase 'usage_snapshots' table.
     */
    suspend fun syncUsageSnapshot(deviceKey: String, snapshot: UsageSnapshot): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || !snapshot.isHomeWifi) return@withContext false

        try {
            val json = JSONObject().apply {
                put("device_key", deviceKey)
                put("upload_bytes", snapshot.uploadBytes)
                put("download_bytes", snapshot.downloadBytes)
                put("total_bytes", snapshot.totalBytes)
                put("consumed_gb", snapshot.consumedGb)
                put("ssid", snapshot.ssid)
                put("gateway_ip", snapshot.gatewayIp)
                put("wifi_band", snapshot.wifiBand)
                put("security_type", snapshot.securityType)
                put("signal_percent", snapshot.signalPercent)
                put("link_speed_mbps", snapshot.linkSpeedMbps)
                put("tracking_started_at", if (snapshot.trackingStartedAt > 0L) iso8601(snapshot.trackingStartedAt) else JSONObject.NULL)
                put("baseline_rx_bytes", snapshot.baselineRxBytes)
                put("baseline_tx_bytes", snapshot.baselineTxBytes)
                put("timestamp", getIso8601Timestamp())
            }

            val bearer = getAuthBearerToken()
            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/usage_snapshots")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $bearer")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()
            if (!successful) return@withContext false

            val userId = authRepository?.getUserId()
            snapshot.appSnapshots.forEach { app ->
                try {
                    val appJson = JSONObject().apply {
                        put("device_key", deviceKey)
                        if (!userId.isNullOrBlank()) put("user_id", userId)
                        put("package_name", app.packageName)
                        put("app_name", app.appName)
                        put("usage_bytes", (app.consumedGb * UsageSnapshot.BYTES_PER_GB).toLong())
                        put("usage_gb", app.consumedGb)
                        put("recorded_date", iso8601(snapshot.timestamp).substring(0, 10))
                    }
                    val appRequest = Request.Builder()
                        .url("$supabaseUrl/rest/v1/app_usage_records")
                        .header("apikey", anonKey)
                        .header("Authorization", "Bearer $bearer")
                        .header("Content-Type", "application/json")
                        .post(appJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    client.newCall(appRequest).execute().close()
                } catch (appError: Exception) {
                    Log.w(tag, "Failed to send app usage row: ${appError.message}")
                }
            }

            val deviceUpdate = JSONObject().apply {
                put("current_usage_gb", snapshot.consumedGb)
                put("latest_ssid", snapshot.ssid)
                put("latest_gateway_ip", snapshot.gatewayIp)
                put("latest_wifi_band", snapshot.wifiBand)
                put("latest_security_type", snapshot.securityType)
                put("latest_signal_percent", snapshot.signalPercent)
                put("latest_link_speed_mbps", snapshot.linkSpeedMbps)
                put("tracking_enabled", snapshot.trackingStartedAt > 0L)
                if (snapshot.trackingStartedAt > 0L) put("tracking_started_at", iso8601(snapshot.trackingStartedAt))
                put("baseline_rx_bytes", snapshot.baselineRxBytes)
                put("baseline_tx_bytes", snapshot.baselineTxBytes)
                put("network_updated_at", getIso8601Timestamp())
                put("last_seen_at", getIso8601Timestamp())
            }
            val deviceRequest = Request.Builder()
                .url("$supabaseUrl/rest/v1/devices?device_key=eq.$deviceKey")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $bearer")
                .header("Content-Type", "application/json")
                .patch(deviceUpdate.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            client.newCall(deviceRequest).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w(tag, "Failed to send usage snapshot: ${e.message}")
            false
        }
    }

    /**
     * Fetches current quota policy for device from Supabase.
     */
    suspend fun fetchQuotaPolicy(deviceKey: String): QuotaPolicy? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null

        try {
            val bearer = getAuthBearerToken()
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/quota_policies?device_key=eq.$deviceKey&select=*")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $bearer")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext null
            }

            val body = response.body?.string() ?: ""
            response.close()

            val array = JSONArray(body)
            if (array.length() == 0) return@withContext null

            val item = array.getJSONObject(0)
            QuotaPolicy(
                deviceId = item.optString("device_key", deviceKey),
                monthlyLimitGb = item.optDouble("monthly_limit_gb", 0.0).toFloat(),
                warningThresholdPercent = item.optInt("warning_threshold_percent", 85),
                homeSsid = item.optString("home_ssid", ""),
                enforceVpnBlock = item.optBoolean("enforce_vpn_block", false),
                isBlocked = item.optBoolean("is_blocked", false),
                reason = item.optString("reason", null)
            )
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch quota policy: ${e.message}")
            null
        }
    }

    suspend fun fetchTargetSsid(householdId: String): String? = withContext(Dispatchers.IO) {
        if (!isConfigured || householdId.isBlank()) return@withContext null
        try {
            val bearer = getAuthBearerToken()
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/gateway_system_settings?household_id=eq.$householdId&select=target_ssid&limit=1")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $bearer")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext null
            }
            val body = response.body?.string() ?: "[]"
            response.close()
            val array = JSONArray(body)
            if (array.length() == 0) null else array.getJSONObject(0).optString("target_ssid", "").trim()
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch target SSID: ${e.message}")
            null
        }
    }

    /**
     * Subscribes to Supabase Realtime WebSocket to receive instant quota policy updates.
     */
    fun startRealtimeSubscription(deviceKey: String, householdId: String? = null) {
        subscribedDeviceKey = deviceKey
        subscribedHouseholdId = householdId
        if (!isConfigured || activeWebSocket != null) return

        try {
            val wsUrl = supabaseUrl.replace("https://", "wss://").replace("http://", "ws://") +
                    "/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"

            val request = Request.Builder().url(wsUrl).build()
            activeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(tag, "Supabase Realtime WebSocket connected")
                    scope.launch {
                        val joinConfig = JSONObject().apply {
                            put("broadcast", JSONObject().apply { put("ack", false); put("self", false) })
                            put("presence", JSONObject().apply { put("key", "") })
                            put("postgres_changes", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", "quota_policies")
                                    put("filter", "device_key=eq.$deviceKey")
                                })
                                subscribedHouseholdId?.takeIf { it.isNotBlank() }?.let { householdId ->
                                    put(JSONObject().apply {
                                        put("event", "*")
                                        put("schema", "public")
                                        put("table", "gateway_system_settings")
                                        put("filter", "household_id=eq.$householdId")
                                    })
                                }
                            })
                        }
                        val joinPayload = JSONObject().apply {
                            put("config", joinConfig)
                            put("access_token", getAuthBearerToken())
                        }
                        val joinMsg = JSONObject().apply {
                            put("topic", "realtime:public")
                            put("event", "phx_join")
                            put("payload", joinPayload)
                            put("ref", "1")
                        }
                        webSocket.send(joinMsg.toString())
                    }
                    startHeartbeat(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val event = json.optString("event")
                        if (event == "postgres_changes" || event == "broadcast") {
                            val payload = json.optJSONObject("payload")
                            val record = payload?.optJSONObject("data")?.optJSONObject("record")
                                ?: payload?.optJSONObject("record")
                            if (record != null && record.has("target_ssid")) {
                                scope.launch { _realtimeTargetSsidUpdates.emit(record.optString("target_ssid", "").trim()) }
                            }
                            if (record != null && record.has("device_key")) {
                                val updatedPolicy = QuotaPolicy(
                                    deviceId = record.optString("device_key", deviceKey),
                                    monthlyLimitGb = record.optDouble("monthly_limit_gb", 0.0).toFloat(),
                                    warningThresholdPercent = record.optInt("warning_threshold_percent", 85),
                                    homeSsid = record.optString("home_ssid", ""),
                                    enforceVpnBlock = record.optBoolean("enforce_vpn_block", false),
                                    isBlocked = record.optBoolean("is_blocked", false),
                                    reason = record.optString("reason", null)
                                )
                                scope.launch { _realtimePolicyUpdates.emit(updatedPolicy) }
                            }
                        }
                    } catch (_: Exception) {}
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(tag, "Supabase Realtime connection failed: ${t.message}. Retrying in 10s...")
                    activeWebSocket = null
                    scheduleReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    activeWebSocket = null
                    scheduleReconnect()
                }
            })
        } catch (e: Exception) {
            Log.w(tag, "Could not start Realtime socket: ${e.message}")
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(10000L)
            val devKey = subscribedDeviceKey
            if (devKey != null && activeWebSocket == null) {
                startRealtimeSubscription(devKey, subscribedHouseholdId)
            }
        }
    }

    private fun startHeartbeat(webSocket: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(25000)
                try {
                    val heartbeat = JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", "heartbeat")
                        put("payload", JSONObject())
                        put("ref", System.currentTimeMillis().toString())
                    }
                    webSocket.send(heartbeat.toString())
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    fun stopRealtimeSubscription() {
        subscribedDeviceKey = null
        subscribedHouseholdId = null
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        heartbeatJob = null
        activeWebSocket?.close(1000, "App closed")
        activeWebSocket = null
    }
}
