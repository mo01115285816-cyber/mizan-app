package com.example.core.common

import android.Manifest
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.receiver.MizanDeviceAdminReceiver

data class MizanPermissionsState(
    val hasUsageAccess: Boolean = false,
    val hasLocationOrWifi: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val isVpnPrepared: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val hasNotification: Boolean = false,
    val hasBootPermission: Boolean = true
) {
    val allEssentialGranted: Boolean
        get() = hasUsageAccess && hasLocationOrWifi

    val grantedCount: Int
        get() = listOf(
            hasUsageAccess,
            hasLocationOrWifi,
            hasOverlayPermission,
            isVpnPrepared,
            isIgnoringBatteryOptimizations,
            isDeviceAdminActive,
            hasNotification,
            hasBootPermission
        ).count { it }

    val totalCount: Int = 8
}

object PermissionHelper {

    fun checkAllPermissions(context: Context): MizanPermissionsState {
        return MizanPermissionsState(
            hasUsageAccess = hasUsageStatsPermission(context),
            hasLocationOrWifi = hasLocationOrWifiPermission(context),
            hasOverlayPermission = hasOverlayPermission(context),
            isVpnPrepared = isVpnPrepared(context),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context),
            isDeviceAdminActive = isDeviceAdminActive(context),
            hasNotification = hasNotificationPermission(context),
            hasBootPermission = hasBootPermission(context)
        )
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasBootPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_BOOT_COMPLETED
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun createOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    fun hasNearbyWifiPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationOrWifiPermission(context: Context): Boolean {
        return hasLocationPermission(context) &&
            hasNearbyWifiPermission(context) &&
            isLocationServicesEnabled(context)
    }

    fun missingLocationOrWifiPermissions(context: Context): Array<String> {
        val missing = buildList {
            if (!hasLocationPermission(context)) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasNearbyWifiPermission(context)) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        return missing.distinct().toTypedArray()
    }

    fun isLocationServicesEnabled(context: Context): Boolean {
        val locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) ||
                    locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun permissionHealthSummary(context: Context): String {
        val state = checkAllPermissions(context)
        val location = if (isLocationServicesEnabled(context)) "LOCATION_ON" else "LOCATION_OFF"
        val wifi = if (hasNearbyWifiPermission(context)) "NEARBY_WIFI_GRANTED" else "NEARBY_WIFI_MISSING"
        val fine = if (hasLocationPermission(context)) "LOCATION_GRANTED" else "LOCATION_MISSING"
        val usage = if (state.hasUsageAccess) "USAGE_GRANTED" else "USAGE_MISSING"
        val vpn = if (state.isVpnPrepared) "VPN_PREPARED" else "VPN_NOT_PREPARED"
        val battery = if (state.isIgnoringBatteryOptimizations) "BATTERY_EXEMPT" else "BATTERY_OPTIMIZED"
        val notification = if (state.hasNotification) "NOTIFICATION_GRANTED" else "NOTIFICATION_MISSING"
        return listOf(location, wifi, fine, usage, vpn, battery, notification).joinToString(";")
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isVpnPrepared(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        val adminComponent = ComponentName(context, MizanDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun createUsageAccessIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createDeviceAdminIntent(context: Context): Intent {
        val adminComponent = ComponentName(context, MizanDeviceAdminReceiver::class.java)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "يطلب ميزان صلاحية مسؤول الجهاز لحماية ضبط الباقة وضمان استمرار خدمة المراقبة."
            )
        }
    }

    fun openDeviceAdminSettingsDirectly(context: Context) {
        try {
            val adminComponent = ComponentName(context, MizanDeviceAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "يطلب ميزان صلاحية مسؤول الجهاز لحماية ضبط الباقة وضمان استمرار خدمة المراقبة."
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    fun createBatteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
