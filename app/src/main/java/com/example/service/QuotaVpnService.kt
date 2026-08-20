package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.nio.ByteBuffer

/**
 * Local VPN service for enforcing quota policy and restricting traffic when quota is exhausted.
 * Runs a dedicated packet drain loop to ensure network requests are gracefully dropped without kernel stalls.
 */
class QuotaVpnService : VpnService() {

    private val tag = "QuotaVpnService"
    private var vpnInterface: ParcelFileDescriptor? = null
    private val vpnScope = CoroutineScope(Dispatchers.IO + Job())
    private var packetDrainJob: Job? = null

    companion object {
        const val CHANNEL_ID = "mizan_vpn_channel"
        const val NOTIFICATION_ID = 1002

        const val ACTION_START_VPN = "com.example.mizan.action.START_VPN"
        const val ACTION_STOP_VPN = "com.example.mizan.action.STOP_VPN"

        fun start(context: Context) {
            val intent = Intent(context, QuotaVpnService::class.java).apply {
                action = ACTION_START_VPN
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w("QuotaVpnService", "Failed to start VPN service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, QuotaVpnService::class.java).apply {
                action = ACTION_STOP_VPN
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun isVpnPrepared(context: Context): Boolean {
            return prepare(context) == null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_VPN -> {
                stopVpn()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startVpn()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn() {
        createNotificationChannel()
        val notification = buildVpnNotification("تم إيقاف الإنترنت مؤقتاً لاكتمال الحصة الشهرية")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            stopVpnInternal()

            val builder = Builder()
                .setSession("ميزان • إيقاف الحصة")
                .addAddress("10.0.0.2", 32)
                .addAddress("fd00:6d69:7a61::2", 128)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .setBlocking(true)
                .setMtu(1500)

            // Disallow Mizan app so it can still sync with Supabase
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (_: Exception) {}
            }

            vpnInterface = builder.establish()
                ?: throw IllegalStateException("Android refused to establish the VPN interface")
            startPacketDrainLoop()
            Log.i(tag, "Quota VPN restriction established successfully.")
        } catch (e: Exception) {
            Log.e(tag, "Failed to establish VPN interface: ${e.message}")
        }
    }

    private fun startPacketDrainLoop() {
        val pfd = vpnInterface ?: return
        packetDrainJob?.cancel()
        packetDrainJob = vpnScope.launch {
            try {
                val inputStream = FileInputStream(pfd.fileDescriptor)
                val buffer = ByteBuffer.allocate(32768)
                val byteArray = ByteArray(32768)

                while (isActive && vpnInterface != null) {
                    val length = inputStream.read(byteArray)
                    if (length <= 0) break
                    // Drain and drop packets cleanly
                    buffer.clear()
                }
            } catch (_: Exception) {
                // Expected when interface closes
            }
        }
    }

    private fun stopVpnInternal() {
        packetDrainJob?.cancel()
        packetDrainJob = null
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}
    }

    private fun stopVpn() {
        stopVpnInternal()
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.i(tag, "Quota VPN stopped and normal connectivity restored.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "إيقاف الإنترنت مؤقتاً (ميزان)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعار يوضح أن الإنترنت مقطوع مؤقتاً بسبب اكتمال الحصة"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildVpnNotification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setContentTitle("ميزان • اكتملت الحصة")
        .setContentText(text)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
