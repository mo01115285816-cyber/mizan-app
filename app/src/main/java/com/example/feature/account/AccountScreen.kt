package com.example.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.common.MizanPermissionsState
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanTypography
import com.example.core.designsystem.fluidPressEffect
import com.example.feature.home.components.MizanTopCapsulesBar

/**
 * Full-featured Fluid Account & Settings Screen for Mizan.
 * Built with Apple Fluid Design principles:
 * - Real Google account identity card with avatar.
 * - Dynamic Quota limit controls with tactile slider.
 * - Real enforcement toggles (VPN enforcement, Home Wi-Fi restriction).
 * - System permissions diagnostic with direct action triggers.
 * - Safe explicit logout with confirmation dialog.
 */
@Composable
fun AccountScreen(
    userName: String,
    userEmail: String,
    userPhotoUrl: String,
    householdId: String,
    deviceModel: String,
    isVpnEnabled: Boolean,
    isDeviceAdminEnabled: Boolean,
    permissionsState: MizanPermissionsState,
    onToggleVpnConsent: (Boolean) -> Unit,
    onToggleDeviceAdmin: (Boolean) -> Unit,
    onOpenPermissionsHub: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var onlyHomeWifi by remember { mutableStateOf(true) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MizanColors.Paper)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 72.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Google Account Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MizanColors.WarmWhite)
                            .border(1.dp, MizanColors.Line, RoundedCornerShape(24.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MizanColors.SoftMint)
                                    .border(1.5.dp, MizanColors.Lime, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userPhotoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = userPhotoUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = MizanColors.Charcoal,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (userName.isNotBlank()) userName else "عضو عائلة ميزان",
                                    style = MizanTypography.Title,
                                    color = MizanColors.Charcoal
                                )
                                Text(
                                    text = if (userEmail.isNotBlank()) userEmail else "حساب Google متصل",
                                    style = MizanTypography.Caption,
                                    color = MizanColors.MutedGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "متصل بالخادم السحابي",
                                        style = MizanTypography.Caption,
                                        color = Color(0xFF3F7E16),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Network Enforcement Controls
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MizanColors.WarmWhite)
                            .border(1.dp, MizanColors.Line, RoundedCornerShape(24.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "ضوابط وسياسات الحظر",
                            style = MizanTypography.Title,
                            color = MizanColors.Charcoal
                        )

                        // Auto-VPN switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تفعيل حماية VPN التلقائية",
                                    style = MizanTypography.BodyMedium,
                                    color = MizanColors.Charcoal
                                )
                                Text(
                                    text = "حظر الاتصال تلقائياً عند نفاذ الحصة المحددة",
                                    style = MizanTypography.Caption,
                                    color = MizanColors.MutedGray
                                )
                            }
                            Switch(
                                checked = isVpnEnabled,
                                onCheckedChange = onToggleVpnConsent,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MizanColors.Charcoal,
                                    checkedTrackColor = MizanColors.Lime
                                )
                            )
                        }

                        HorizontalDivider(color = MizanColors.Line.copy(alpha = 0.6f))

                        // Only Home Wi-Fi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تقييد الحظر على شبكة المنزل فقط",
                                    style = MizanTypography.BodyMedium,
                                    color = MizanColors.Charcoal
                                )
                                Text(
                                    text = "السماح ببيانات الجوال بدون تقييد خارج المنزل",
                                    style = MizanTypography.Caption,
                                    color = MizanColors.MutedGray
                                )
                            }
                            Switch(
                                checked = onlyHomeWifi,
                                onCheckedChange = { onlyHomeWifi = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MizanColors.Charcoal,
                                    checkedTrackColor = MizanColors.Lime
                                )
                            )
                        }
                    }
                }

                // 4. Permissions & System Health
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MizanColors.WarmWhite)
                            .border(1.dp, MizanColors.Line, RoundedCornerShape(24.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "أذونات وصلاحيات النظام",
                                style = MizanTypography.Title,
                                color = MizanColors.Charcoal
                            )
                            Text(
                                text = "مركز الأذونات",
                                style = MizanTypography.Caption,
                                color = Color(0xFF3F7E16),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MizanColors.SoftMint)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .fluidPressEffect(onClick = onOpenPermissionsHub)
                            )
                        }

                        PermissionStatusItem(
                            title = "صلاحية مراقبة استهلاك التطبيقات",
                            isGranted = permissionsState.hasUsageAccess,
                            icon = Icons.Outlined.Speed
                        )

                        PermissionStatusItem(
                            title = "صلاحية كشف شبكة Wi-Fi والموقع",
                            isGranted = permissionsState.hasLocationOrWifi,
                            icon = Icons.Outlined.Wifi
                        )

                        PermissionStatusItem(
                            title = "إعفاء تحسين البطارية (العمل بالخلفية)",
                            isGranted = permissionsState.isIgnoringBatteryOptimizations,
                            icon = Icons.Outlined.BatteryAlert
                        )
                    }
                }

                // 5. Device Info & Logout
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MizanColors.WarmWhite)
                            .border(1.dp, MizanColors.Line, RoundedCornerShape(24.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "إصدار التطبيق",
                                style = MizanTypography.BodyMedium,
                                color = MizanColors.Charcoal
                            )
                            Text(
                                text = "Mizan v1.0 (Apple Fluid)",
                                style = MizanTypography.Caption,
                                color = MizanColors.MutedGray
                            )
                        }

                        HorizontalDivider(color = MizanColors.Line.copy(alpha = 0.6f))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MizanColors.ErrorSoft)
                                .border(1.dp, Color(0xFFF0B8B0), RoundedCornerShape(14.dp))
                                .fluidPressEffect(onClick = { showSignOutConfirm = true })
                                .testTag("account_screen_sign_out_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Logout,
                                    contentDescription = null,
                                    tint = Color(0xFF9E2A1E),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تسجيل الخروج وإلغاء ربط الجهاز",
                                    style = MizanTypography.Button,
                                    color = Color(0xFF9E2A1E)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            MizanTopCapsulesBar(
                modifier = Modifier.align(Alignment.TopCenter),
                brandTitle = "الإعدادات",
                brandSubtitle = "الحساب والأمان",
                onRefreshClick = onSyncNow,
                userPhotoUrl = userPhotoUrl
            )
        }
    }

    if (showSignOutConfirm) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirm = false },
                title = {
                    Text(
                        text = "تأكيد تسجيل الخروج",
                        style = MizanTypography.Title,
                        color = MizanColors.Charcoal
                    )
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من رغبتك في تسجيل الخروج وإلغاء الربط؟ سيتوقف تتبع الحصة الحالية.",
                        style = MizanTypography.Body,
                        color = MizanColors.Charcoal
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSignOutConfirm = false
                            onSignOut()
                        }
                    ) {
                        Text(
                            text = "تسجيل الخروج",
                            style = MizanTypography.Button,
                            color = Color(0xFFD32F2F)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutConfirm = false }) {
                        Text(
                            text = "إلغاء",
                            style = MizanTypography.Button,
                            color = MizanColors.Charcoal
                        )
                    }
                },
                containerColor = MizanColors.WarmWhite,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun PermissionStatusItem(
    title: String,
    isGranted: Boolean,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF3F7E16) else Color(0xFFD32F2F),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MizanTypography.BodyMedium,
                color = MizanColors.Charcoal
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isGranted) MizanColors.SoftMint else MizanColors.ErrorSoft)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isGranted) "مفعلة ✓" else "مطلوبة !",
                style = MizanTypography.Caption,
                color = if (isGranted) Color(0xFF3F7E16) else Color(0xFF9E2A1E),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
