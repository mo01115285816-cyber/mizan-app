package com.example.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.example.core.common.NetworkDetails
import com.example.core.common.NetworkInfoProvider
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanTypography
import com.example.core.designsystem.fluidPressEffect
import com.example.core.model.AppUsageItem
import com.example.feature.account.AccountScreen
import com.example.feature.home.components.MizanDailyInsightCard
import com.example.feature.home.components.MizanHomeBottomBar
import com.example.feature.home.components.MizanNetworkDetailsBottomSheet
import com.example.feature.home.components.MizanPermissionsBanner
import com.example.feature.home.components.MizanProfileBottomSheet
import com.example.feature.home.components.MizanQuotaHeroCard
import com.example.feature.home.components.MizanTopAppsCard
import com.example.feature.home.components.MizanTopCapsulesBar
import com.example.feature.permissions.PermissionsHubBottomSheet
import com.example.feature.usage.UsageScreen

/**
 * Main MIZAN Application Shell.
 * Built with Apple Fluid Design principles:
 * - Fluid Navigation between 6 tabs: Overview, Devices, Quotas, Analytics, Notifications, Settings.
 * - Top Header: Wi-Fi status button (left), Arabic greeting (center), Google Profile Avatar (right).
 * - Tapping Profile Avatar opens the non-destructive Profile Details Sheet instead of logging out!
 * - Mint/Lime Quota Hero Card with interactive direct manipulation & haptics.
 * - Floating Island Pill bottom navigation with expanding active tab and light haptic feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onTabSelected: (HomeTab) -> Unit,
    onProfileClick: () -> Unit = {},
    onDismissProfileSheet: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onWifiStatusClick: () -> Unit = {},
    onConnectionPillClick: () -> Unit = {},
    onDismissNetworkSheet: () -> Unit = {},
    onRefreshNetworkDetails: () -> Unit = {},
    onOpenPermissionsHub: () -> Unit = {},
    onDismissPermissionsHub: () -> Unit = {},
    onUpdateQuotaLimit: (Float) -> Unit = {},
    onToggleVpnConsent: (Boolean) -> Unit = {},
    onToggleDeviceAdmin: (Boolean) -> Unit = {},
    onRequestUsageAccess: () -> Unit = {},
    onRequestLocationOrWifi: () -> Unit = {},
    onRequestOverlay: () -> Unit = {},
    onRequestNotification: () -> Unit = {},
    onRequestVpn: () -> Unit = {},
    onRequestDeviceAdmin: () -> Unit = {},
    onRequestBatteryOptimization: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MizanColors.Paper,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                MizanHomeBottomBar(
                    selectedTab = state.selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "home_tab_transition"
            ) { tab ->
                when (tab) {
                    HomeTab.Home -> {
                        HomeDashboardContent(
                            state = state,
                            onWifiStatusClick = onWifiStatusClick,
                            onProfileClick = onProfileClick,
                            onOpenPermissionsHub = onOpenPermissionsHub,
                            onConnectionPillClick = onConnectionPillClick,
                            onRefresh = onRefreshNetworkDetails
                        )
                    }
                    HomeTab.Usage -> {
                        UsageScreen(
                            onRefresh = onRefreshNetworkDetails
                        )
                    }
                    HomeTab.Account -> {
                        AccountScreen(
                            userName = state.userName,
                            userEmail = state.userEmail,
                            userPhotoUrl = state.userPhotoUrl,
                            householdId = state.householdId,
                            deviceModel = state.deviceModel,
                            quotaLimitGb = state.quotaGb,
                            isVpnEnabled = state.isVpnConsentGranted,
                            isDeviceAdminEnabled = state.isDeviceAdminActive,
                            permissionsState = state.permissionsState,
                            onUpdateQuotaLimit = onUpdateQuotaLimit,
                            onToggleVpnConsent = onToggleVpnConsent,
                            onToggleDeviceAdmin = onToggleDeviceAdmin,
                            onOpenPermissionsHub = onOpenPermissionsHub,
                            onSyncNow = onRefreshNetworkDetails,
                            onSignOut = onSignOutClick
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // Profile Bottom Sheet
        // -------------------------------------------------------------
        if (state.showProfileSheet) {
            MizanProfileBottomSheet(
                userName = state.userName,
                userEmail = state.userEmail,
                userPhotoUrl = state.userPhotoUrl,
                householdId = state.householdId,
                deviceModel = state.deviceModel,
                onDismissRequest = onDismissProfileSheet,
                onSignOutClick = onSignOutClick,
                onSyncNowClick = onRefreshNetworkDetails
            )
        }

        // -------------------------------------------------------------
        // Network Details Bottom Sheet
        // -------------------------------------------------------------
        if (state.showNetworkDetailsSheet) {
            MizanNetworkDetailsBottomSheet(
                networkDetails = state.networkDetails,
                onDismissRequest = onDismissNetworkSheet,
                onRefresh = onRefreshNetworkDetails
            )
        }

        // -------------------------------------------------------------
        // Permissions Hub Bottom Sheet
        // -------------------------------------------------------------
        if (state.showPermissionsSheet) {
            PermissionsHubBottomSheet(
                permissionsState = state.permissionsState,
                onDismiss = onDismissPermissionsHub,
                onRequestUsageAccess = onRequestUsageAccess,
                onRequestLocationOrWifi = onRequestLocationOrWifi,
                onRequestOverlay = onRequestOverlay,
                onRequestNotification = onRequestNotification,
                onRequestVpn = onRequestVpn,
                onRequestDeviceAdmin = onRequestDeviceAdmin,
                onRequestBatteryOptimization = onRequestBatteryOptimization
            )
        }
    }
}

@Composable
private fun HomeDashboardContent(
    state: HomeUiState,
    onWifiStatusClick: () -> Unit,
    onProfileClick: () -> Unit,
    onOpenPermissionsHub: () -> Unit,
    onConnectionPillClick: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ---------------------------------------------------------
        // 1. Floating Top Capsules Bar (Brand Capsule + Actions Capsule)
        // ---------------------------------------------------------
        MizanTopCapsulesBar(
            brandTitle = "ميزان",
            brandSubtitle = if (state.userName.isNotBlank()) state.userName else null,
            onWifiClick = onWifiStatusClick,
            isWifiConnected = state.networkDetails.isWifi && state.networkDetails.isConnected,
            onRefreshClick = onRefresh,
            onProfileClick = onProfileClick,
            userPhotoUrl = state.userPhotoUrl
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ---------------------------------------------------------
        // Permissions Banner (Appears if any essential permission is missing)
        // ---------------------------------------------------------
        MizanPermissionsBanner(
            permissionsState = state.permissionsState,
            onClick = onOpenPermissionsHub,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---------------------------------------------------------
        // 2. Large Mint / Lime Quota Hero Card with Dynamic Network Pill
        // ---------------------------------------------------------
        MizanQuotaHeroCard(
            usedGb = state.usedGb,
            quotaGb = state.quotaGb,
            remainingGb = state.remainingGb,
            percentage = state.percentage,
            connectionStatus = state.connectionStatus,
            onConnectionPillClick = onConnectionPillClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---------------------------------------------------------
        // 3. White Daily Average Insight Card
        // ---------------------------------------------------------
        MizanDailyInsightCard(
            dailyAverageGb = state.dailyAverageGb,
            dailyTrend = state.dailyTrend,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(22.dp))

        // ---------------------------------------------------------
        // 4. Section Title
        // ---------------------------------------------------------
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = "أكثر التطبيقات استهلاكاً",
                style = MizanTypography.Title,
                color = MizanColors.Charcoal,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_top_apps_title")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ---------------------------------------------------------
        // 5. White Ranked App Usage Card
        // ---------------------------------------------------------
        MizanTopAppsCard(
            apps = state.appUsage,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Green Wi-Fi Signal Vector Icon.
 */
@Composable
private fun WifiStatusIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.2.dp.toPx()
        val cx = w * 0.50f
        val dotY = h * 0.76f

        // Bottom Dot
        drawCircle(color = color, radius = 2.2.dp.toPx(), center = Offset(cx, dotY))

        // Small Arc
        drawWifiArc(cx, dotY, w * 0.22f, color, stroke)

        // Large Arc
        drawWifiArc(cx, dotY, w * 0.40f, color, stroke)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWifiArc(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path().apply {
        val rect = Rect(center = Offset(cx, cy), radius = radius)
        arcTo(rect = rect, startAngleDegrees = 220f, sweepAngleDegrees = 100f, forceMoveTo = true)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

