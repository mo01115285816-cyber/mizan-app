package com.example.feature.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.home.HomeTab

/**
 * MIZAN Floating Island Pill Navigation Bar.
 * Tailored specifically to the 3 actual screens of the application:
 * 1. Home (الرئيسية)
 * 2. Usage (الاستهلاك)
 * 3. Account (الحساب)
 */
@Composable
fun MizanHomeBottomBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val barBgColor = Color(0xFF151515)
    val barBorderColor = Color(0xFF2D3028)
    val activePillColor = Color(0xFFC8F24A)
    val activeContentColor = Color(0xFF151515)
    val inactiveIconColor = Color(0xFFA0A39B)

    val tabs = listOf(
        HomeTabItem(HomeTab.Home, "الرئيسية", Icons.Outlined.Home),
        HomeTabItem(HomeTab.Usage, "الاستهلاك", Icons.Outlined.Analytics),
        HomeTabItem(HomeTab.Account, "الحساب", Icons.Outlined.Person)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(36.dp),
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(36.dp))
            .background(barBgColor)
            .border(1.dp, barBorderColor, RoundedCornerShape(36.dp))
            .testTag("mizan_floating_bottom_bar"),
        color = barBgColor,
        shape = RoundedCornerShape(36.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { item ->
                    val isSelected = selectedTab == item.tab

                    val bgAnimatedColor by animateColorAsState(
                        targetValue = if (isSelected) activePillColor else Color.Transparent,
                        animationSpec = tween(durationMillis = 120),
                        label = "tab_bg_${item.tab.name}"
                    )

                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(bgAnimatedColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onTabSelected(item.tab)
                                    }
                                }
                            )
                            .animateContentSize(
                                animationSpec = tween(durationMillis = 120)
                            )
                            .padding(
                                horizontal = if (isSelected) 18.dp else 14.dp,
                                vertical = 6.dp
                            )
                            .testTag("bottom_nav_${item.tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) activeContentColor else inactiveIconColor,
                                modifier = Modifier.size(20.dp)
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.label,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.sp,
                                        color = activeContentColor
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class HomeTabItem(
    val tab: HomeTab,
    val label: String,
    val icon: ImageVector
)
