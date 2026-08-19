package com.example.feature.permissions

import android.app.Activity
import android.content.Context
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.common.MizanPermissionsState
import com.example.core.common.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsHubBottomSheet(
    permissionsState: MizanPermissionsState,
    onDismiss: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestLocationOrWifi: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestVpn: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF9FAF5),
        dragHandle = null,
        modifier = modifier.testTag("permissions_hub_sheet")
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Handle Bar
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFDCDFD3))
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Title
                Text(
                    text = "منظومة الأمان وصلاحيات ميزان",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF151515)
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = "مفعل ${permissionsState.grantedCount} من ${permissionsState.totalCount} صلاحيات لنظام حماية واحتساب غير قابل للتحايل",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = Color(0xFF6F7368),
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Usage Stats Permission
                PermissionCardItem(
                    title = "إحصاءات استهلاك التطبيقات (Usage Stats)",
                    subtitle = "جلب استهلاك كل تطبيق بدقة البايت من نظام أندرويد لمنع التلاعب.",
                    icon = Icons.Outlined.DataUsage,
                    isGranted = permissionsState.hasUsageAccess,
                    onAction = onRequestUsageAccess,
                    testTag = "perm_item_usage"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Wi-Fi SSID / BSSID Location Permission
                PermissionCardItem(
                    title = "التعرف على اسم ومعرف راوتر المنزل (BSSID)",
                    subtitle = "مطابقة عنوان الماك (MAC Address) لراوتر البيت حتى لا يتم احتساب شبكات خارجية.",
                    icon = Icons.Outlined.Wifi,
                    isGranted = permissionsState.hasLocationOrWifi,
                    onAction = onRequestLocationOrWifi,
                    testTag = "perm_item_wifi"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. System Alert Window (Overlay)
                PermissionCardItem(
                    title = "الظهور فوق التطبيقات (System Overlay)",
                    subtitle = "إظهار بطاقة تنبيه عائمة فورية تخبر المستخدم بانتهاء الحصة دون تعطيل استخدام الهاتف.",
                    icon = Icons.Outlined.Security,
                    isGranted = permissionsState.hasOverlayPermission,
                    onAction = onRequestOverlay,
                    testTag = "perm_item_overlay"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Local VPN Firewall
                PermissionCardItem(
                    title = "جدار الحظر التلقائي (Local VPN Firewall)",
                    subtitle = "عزل وقطع الإنترنت فور انتهاء الحصة دون إيقاف باقي تطبيقات الهاتف.",
                    icon = Icons.Outlined.VpnKey,
                    isGranted = permissionsState.isVpnPrepared,
                    onAction = onRequestVpn,
                    testTag = "perm_item_vpn"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Battery Optimization Exemption
                PermissionCardItem(
                    title = "استثناء توفير الطاقة (Unrestricted Battery)",
                    subtitle = "منع نظام أندرويد من قتل خدمة المراقبة عند قفل الشاشة أو في الخلفية.",
                    icon = Icons.Outlined.BatteryChargingFull,
                    isGranted = permissionsState.isIgnoringBatteryOptimizations,
                    onAction = onRequestBatteryOptimization,
                    testTag = "perm_item_battery"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Device Admin Permission
                PermissionCardItem(
                    title = "مسؤول الجهاز وحماية الحذف (Device Admin)",
                    subtitle = "حماية التطبيق من الإلغاء المفاجئ أو إيقاف الخدمة قسرياً.",
                    icon = Icons.Outlined.Lock,
                    isGranted = permissionsState.isDeviceAdminActive,
                    onAction = onRequestDeviceAdmin,
                    testTag = "perm_item_admin"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Foreground Notification Permission
                PermissionCardItem(
                    title = "إشعارات المراقبة الحية والإنذار (Notifications)",
                    subtitle = "إرسال تنبيهات اقتراب النفاد وعرض شريط الرصد الدائم.",
                    icon = Icons.Outlined.Notifications,
                    isGranted = permissionsState.hasNotification,
                    onAction = onRequestNotification,
                    testTag = "perm_item_notification"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 8. Boot Survival Auto-Start
                PermissionCardItem(
                    title = "التشغيل التلقائي عند الإقلاع (Boot Auto-Start)",
                    subtitle = "استئناف الحماية والرصد تلقائياً وفوراً عند إعادة تشغيل الهاتف.",
                    icon = Icons.Outlined.Security,
                    isGranted = permissionsState.hasBootPermission,
                    onAction = {},
                    testTag = "perm_item_boot"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Close / Done Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF151515))
                        .clickable(onClick = onDismiss)
                        .testTag("perm_sheet_done_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (permissionsState.allEssentialGranted) "تم ومتابعة" else "إغلاق",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PermissionCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    onAction: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(
                1.2.dp,
                if (isGranted) Color(0xFFC0EE2B) else Color(0xFFE8EAE0),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) Color(0xFFEFFCD5) else Color(0xFFF2F4EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF2E6B00) else Color(0xFF151515),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF151515)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = Color(0xFF7A7E72),
                        lineHeight = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Status or Action Button
            if (isGranted) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFC0EE2B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "مفعل",
                        tint = Color(0xFF151515),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF151515))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "تفعيل",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
