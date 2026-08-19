package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.DevicePreferencesDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Standard Device Admin receiver for Mizan device administration management.
 * Note: A standard Device Admin receiver does not completely prevent uninstallation
 * on unmanaged consumer Android devices (which requires Device Owner mode).
 */
class MizanDeviceAdminReceiver : DeviceAdminReceiver() {

    private val tag = "MizanDeviceAdmin"

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(tag, "Device Admin enabled by user.")
        val prefs = DevicePreferencesDataSource(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            prefs.setDeviceAdminEnabled(true)
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(tag, "Device Admin disabled by user.")
        val prefs = DevicePreferencesDataSource(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            prefs.setDeviceAdminEnabled(false)
        }
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "تعطيل إدارة الجهاز قد يؤثر على حماية باقة Wi-Fi المنزلية في ميزان."
    }
}
