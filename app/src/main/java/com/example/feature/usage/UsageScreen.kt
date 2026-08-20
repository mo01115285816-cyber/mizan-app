package com.example.feature.usage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanSpringSpecs
import com.example.core.designsystem.MizanTypography
import com.example.core.designsystem.fluidPressEffect
import com.example.core.model.AppUsageItem
import com.example.core.model.UsageSnapshot
import com.example.data.datasource.AndroidNetworkStatsDataSource
import com.example.data.local.DevicePreferencesDataSource
import com.example.feature.home.DayUsage
import com.example.feature.home.components.MizanTopCapsulesBar
import java.util.Calendar
import kotlin.math.max

enum class UsagePeriod(val label: String) {
    Today("اليوم"),
    Week("آخر 7 أيام"),
    Month("هذا الشهر")
}

/**
 * Full-featured Fluid Usage Analytics Screen for Mizan.
 * Built with Apple Fluid Design principles:
 * - Fluid segmented period selector with tactile haptics.
 * - Interactive 7-Day / Daily Bar Chart with instant touch inspection.
 * - Network split card: Wi-Fi vs Cellular data with dynamic fill bars.
 * - Live per-app breakdown with real Android stats and instant search.
 */
@Composable
fun UsageScreen(
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataSource = remember { AndroidNetworkStatsDataSource(context) }
    val preferencesDataSource = remember { DevicePreferencesDataSource(context) }

    var selectedPeriod by remember { mutableStateOf(UsagePeriod.Week) }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    var wifiUsageGb by remember { mutableStateOf(0f) }
    var trendList by remember { mutableStateOf<List<DayUsage>>(emptyList()) }
    var appsList by remember { mutableStateOf<List<AppUsageItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableStateOf(0) }

    suspend fun loadDataForPeriod(period: UsagePeriod) {
        isLoading = true
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        val currentMonthKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}"

        val startTime = when (period) {
            UsagePeriod.Today -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            UsagePeriod.Week -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            UsagePeriod.Month -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
        }

        val baseline = preferencesDataSource.getWifiBaseline()
        val baselineIsCurrent = baseline.initialized && baseline.monthKey == currentMonthKey
        val effectiveStart = if (baselineIsCurrent) maxOf(startTime, baseline.timestamp) else endTime
        val (wifiRx, wifiTx) = if (effectiveStart < endTime) {
            dataSource.queryTotalWifiUsage(effectiveStart, endTime)
        } else {
            Pair(0L, 0L)
        }
        wifiUsageGb = UsageSnapshot.bytesToGb(wifiRx + wifiTx)
        trendList = dataSource.query7DayTrend(
            startFrom = if (baselineIsCurrent) baseline.timestamp else endTime
        )
        appsList = if (effectiveStart < endTime) {
            dataSource.queryTopAppsUsage(
                startTime = effectiveStart,
                endTime = endTime,
                networkType = android.net.NetworkCapabilities.TRANSPORT_WIFI,
                limit = 25
            )
        } else {
            emptyList()
        }
        isLoading = false
    }

    LaunchedEffect(selectedPeriod, reloadToken) {
        loadDataForPeriod(selectedPeriod)
    }

    val filteredApps = remember(appsList, searchQuery) {
        if (searchQuery.isBlank()) appsList
        else appsList.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalUsageGb = wifiUsageGb

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MizanColors.Paper)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 92.dp)
            ) {

            // Fluid Period Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MizanColors.WarmWhite)
                    .border(1.dp, MizanColors.Line, RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                UsagePeriod.entries.forEach { period ->
                    val isSelected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MizanColors.Lime else Color.Transparent)
                            .fluidPressEffect(onClick = { selectedPeriod = period })
                            .testTag("period_tab_${period.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period.label,
                            style = MizanTypography.Button,
                            color = MizanColors.Charcoal,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Wi-Fi usage since device activation
                item {
                    NetworkSplitCard(totalGb = totalUsageGb)
                }

                // 2. Interactive Usage Chart
                item {
                    UsageChartCard(
                        trendList = trendList,
                        selectedIndex = selectedDayIndex,
                        onSelectDay = { index ->
                            selectedDayIndex = if (selectedDayIndex == index) null else index
                        }
                    )
                }

                // 3. Top Apps Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "استهلاك التطبيقات (${filteredApps.size})",
                            style = MizanTypography.Title,
                            color = MizanColors.Charcoal
                        )
                        Text(
                            text = "الأعلى استهلاكاً أولاً",
                            style = MizanTypography.Caption,
                            color = MizanColors.MutedGray
                        )
                    }
                }

                // 4. Apps List
                if (filteredApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MizanColors.WarmWhite)
                                .border(1.dp, MizanColors.Line, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد بيانات استهلاك مسجلة لهذه الفترة",
                                style = MizanTypography.BodyMedium,
                                color = MizanColors.MutedGray
                            )
                        }
                    }
                } else {
                    val maxUsage = max(filteredApps.firstOrNull()?.consumedGb ?: 1f, 0.01f)
                    items(filteredApps, key = { it.id }) { app ->
                        AppUsageRow(
                            item = app,
                            maxUsage = maxUsage
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
                }
            }

            MizanTopCapsulesBar(
                modifier = Modifier.align(Alignment.TopCenter),
                brandTitle = "الاستهلاك",
                brandSubtitle = "تحليل حي",
                showSearch = true,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                isSearchActive = showSearchBar,
                onSearchActiveChange = { showSearchBar = it },
                searchPlaceholder = "ابحث عن تطبيق...",
                onRefreshClick = {
                    reloadToken += 1
                    onRefresh()
                }
            )
        }
    }
}

@Composable
private fun NetworkSplitCard(
    totalGb: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MizanColors.WarmWhite)
            .border(1.dp, MizanColors.Line, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "استهلاك Wi‑Fi منذ التفعيل",
                style = MizanTypography.Title,
                color = MizanColors.Charcoal
            )
            Text(
                text = "${String.format(java.util.Locale.US, "%.2f", totalGb)} GB",
                style = MizanTypography.BodyMedium,
                fontWeight = FontWeight.Bold,
                color = MizanColors.Charcoal
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MizanColors.Line)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MizanColors.Lime)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Wifi,
                    contentDescription = null,
                    tint = MizanColors.Charcoal,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Wi‑Fi فقط",
                    style = MizanTypography.Label,
                    color = MizanColors.Charcoal
                )
            }
            Text(
                text = "بيانات الجوال غير محسوبة",
                style = MizanTypography.Label,
                color = MizanColors.MutedGray
            )
        }
    }
}

@Composable
private fun UsageChartCard(
    trendList: List<DayUsage>,
    selectedIndex: Int?,
    onSelectDay: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MizanColors.WarmWhite)
            .border(1.dp, MizanColors.Line, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "معدل الاستهلاك اليومي",
                style = MizanTypography.Title,
                color = MizanColors.Charcoal
            )
            if (selectedIndex != null && selectedIndex in trendList.indices) {
                val selectedDay = trendList[selectedIndex]
                Text(
                    text = "${selectedDay.dayLabel}: ${String.format(java.util.Locale.US, "%.2f", selectedDay.valueGb)} GB",
                    style = MizanTypography.Label,
                    color = Color(0xFF3F7E16),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Bar Chart
        val maxDayValue = max(trendList.maxOfOrNull { it.valueGb } ?: 1f, 0.1f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            trendList.forEachIndexed { index, day ->
                val isSelected = selectedIndex == index
                val barFraction = (day.valueGb / maxDayValue).coerceIn(0.08f, 1f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fluidPressEffect(onClick = { onSelectDay(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height((100 * barFraction).dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                if (isSelected) MizanColors.Lime
                                else Color(0xFFD3E7A8)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) MizanColors.Charcoal else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = day.dayLabel,
                        style = MizanTypography.Caption,
                        color = if (isSelected) MizanColors.Charcoal else MizanColors.MutedGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUsageRow(
    item: AppUsageItem,
    maxUsage: Float
) {
    val progress = (item.consumedGb / maxUsage).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MizanColors.WarmWhite)
            .border(1.dp, MizanColors.Line, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Monogram / Icon Box
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MizanColors.SoftMint)
                .border(1.dp, MizanColors.Lime, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.appName.take(1).uppercase(),
                style = MizanTypography.Title,
                color = MizanColors.Charcoal
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.appName,
                    style = MizanTypography.BodyMedium,
                    color = MizanColors.Charcoal,
                    maxLines = 1
                )
                Text(
                    text = "${String.format(java.util.Locale.US, "%.3f", item.consumedGb)} GB",
                    style = MizanTypography.BodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MizanColors.Charcoal
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Animated progress line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MizanColors.Paper)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxSize()
                        .background(MizanColors.Lime)
                )
            }
        }
    }
}
