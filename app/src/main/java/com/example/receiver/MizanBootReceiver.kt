package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.DevicePreferencesDataSource
import com.example.service.UsageTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Boot Completed Receiver for instant survival upon device reboot.
 * Restarts background quota tracking and re-engages VPN enforcement if quota was already exhausted.
 */
class MizanBootReceiver : BroadcastReceiver() {

    private val tag = "MizanBootReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(tag, "Received broadcast intent: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val preferences = DevicePreferencesDataSource(context.applicationContext)
                    val profile = preferences.deviceProfileFlow.first()

                    if (profile != null && profile.isActive) {
                        // Start the foreground monitor first. It fetches the current
                        // Supabase policy before making any quota-block decision, so a
                        // locally cached block cannot override a dashboard unblock.
                        UsageTrackingService.start(context.applicationContext)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error handling boot broadcast: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
