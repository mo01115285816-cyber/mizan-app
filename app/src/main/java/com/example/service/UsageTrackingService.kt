package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.core.common.NetworkInfoProvider
import com.example.data.datasource.AndroidNetworkStatsDataSource
import com.example.data.local.DevicePreferencesDataSource
import com.example.data.repository.MizanRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UsageTrackingService : Service() {

    private val tag = "UsageTrackingService"
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var trackingLoopJob: Job? = null
    private var networkCallbackRegistered = false
    private val syncMutex = Mutex()
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = scheduleImmediateRefresh()
        override fun onLost(network: Network) = scheduleImmediateRefresh()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = scheduleImmediateRefresh()
    }

    private lateinit var repository: MizanRepositoryImpl
    private lateinit var preferences: DevicePreferencesDataSource
    private lateinit var networkStatsDataSource: AndroidNetworkStatsDataSource

    companion object {
        const val CHANNEL_ID = "mizan_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_TRACKING = "com.example.mizan.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.mizan.action.STOP_TRACKING"
        const val ACTION_REFRESH_NOW = "com.example.mizan.action.REFRESH_NOW"

        fun start(context: Context) {
            val intent = Intent(context, UsageTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w("UsageTrackingService", "Failed to start service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, UsageTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun refreshNow(context: Context) {
            val intent = Intent(context, UsageTrackingService::class.java).apply {
                action = ACTION_REFRESH_NOW
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w("UsageTrackingService", "Failed to request immediate refresh: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = DevicePreferencesDataSource(applicationContext)
        networkStatsDataSource = AndroidNetworkStatsDataSource(applicationContext)
        repository = MizanRepositoryImpl(applicationContext, preferences, networkStatsDataSource)
        createNotificationChannel()
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        if (networkCallbackRegistered) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            networkCallbackRegistered = true
        } catch (e: Exception) {
            Log.w(tag, "Could not register network callback: ${e.message}")
        }
    }

    private fun unregisterNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        if (!networkCallbackRegistered) return
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        networkCallbackRegistered = false
    }

    private fun scheduleImmediateRefresh() {
        if (!serviceJob.isActive) return
        serviceScope.launch {
            runCatching { syncMutex.withLock { checkAndSyncUsage() } }
                .onFailure { Log.w(tag, "Immediate network refresh failed: ${it.message}") }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_TRACKING -> {
                trackingLoopJob?.cancel()
                trackingLoopJob = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH_NOW -> {
                startForegroundNotification()
                serviceScope.launch { syncMutex.withLock { checkAndSyncUsage() } }
            }
            else -> {
                startForegroundNotification()
                if (trackingLoopJob == null || trackingLoopJob?.isActive == false) {
                    startPeriodicTrackingLoop()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterNetworkCallback()
        trackingLoopJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val notification = buildTrackingNotification(
            statusText = "متابعة استهلاك شبكة Wi-Fi المنزلية قيد التشغيل"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startPeriodicTrackingLoop() {
        trackingLoopJob?.cancel()
        trackingLoopJob = serviceScope.launch {
            while (isActive) {
                try {
                    syncMutex.withLock { checkAndSyncUsage() }
                } catch (e: Exception) {
                    Log.w(tag, "Error in tracking loop: ${e.message}")
                }
                delay(60_000L)
            }
        }
    }

    private suspend fun checkAndSyncUsage() {
        preferences.recordServiceHeartbeat()
        val snapshot = repository.recordUsageSnapshot()
        val networkDetails = NetworkInfoProvider.getConnectedNetworkDetails(applicationContext)
        preferences.setNetworkState(NetworkInfoProvider.classifyNetworkState(networkDetails))

        var profile = preferences.deviceProfileFlow.first()
        val savedHomeBssid = preferences.homeBssidFlow.first()
        val targetSsid = preferences.targetSsidFlow.first().trim()
        val savedHomeSsid = preferences.homeSsidFlow.first().trim()

        // Only the explicitly configured Target SSID or locked BSSID identifies home Wi-Fi.
        val isHomeNetwork = networkDetails.isWifi && (
            (savedHomeBssid.isNotBlank() && networkDetails.bssid.equals(savedHomeBssid, ignoreCase = true)) ||
            (targetSsid.isNotBlank() && networkDetails.ssid.equals(targetSsid, ignoreCase = true)) ||
            (targetSsid.isBlank() && savedHomeSsid.isNotBlank() && networkDetails.ssid.equals(savedHomeSsid, ignoreCase = true))
        )

        val consumedGb = snapshot?.consumedGb ?: 0f
        val enforceVpnBlock = preferences.remoteEnforceVpnBlockFlow.first()
        val notificationText = if (isHomeNetwork) {
            "متصل بشبكة المنزل (${networkDetails.ssid}) • الاستهلاك: ${consumedGb} جيجابايت"
        } else if (networkDetails.isCellular) {
            "متصل ببيانات الشريحة (Mobile Data) • المتابعة معلقة"
        } else {
            "خارج شبكة المنزل (${networkDetails.ssid}) • المتابعة معلقة مؤقتاً"
        }

        updateNotification(notificationText)
        val syncStartedAt = System.currentTimeMillis()
        val remoteSyncSucceeded = repository.syncWithRemote()
        val lastPolicySyncAt = preferences.lastPolicySyncAtFlow.first()
        val policyFresh = remoteSyncSucceeded && lastPolicySyncAt >= syncStartedAt
        profile = preferences.deviceProfileFlow.first()

        // Background Enforcement of Quota strictly tied to Home Router
        if (profile != null) {
            val totalGb = profile.quotaLimitGb
            val manualBlock = profile.isBlocked
            val quotaBlock = policyFresh && enforceVpnBlock && consumedGb >= totalGb && totalGb > 0f && isHomeNetwork
            val shouldBlock = manualBlock || quotaBlock

            if (shouldBlock) {
                if (profile.isVpnEnforcementEnabled && QuotaVpnService.isVpnPrepared(applicationContext)) {
                    QuotaVpnService.start(applicationContext)
                    QuotaOverlayService.show(applicationContext)
                }
            } else {
                // No manual block and no home-Wi-Fi quota exhaustion: restore connectivity.
                QuotaVpnService.stop(applicationContext)
                QuotaOverlayService.hide(applicationContext)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "متابعة استهلاك باقة ميزان",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار دائم يوضح حالة متابعة استهلاك شبكة Wi-Fi المنزلية"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildTrackingNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("ميزان • مراقبة باقة Wi-Fi")
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildTrackingNotification(statusText))
    }

}
