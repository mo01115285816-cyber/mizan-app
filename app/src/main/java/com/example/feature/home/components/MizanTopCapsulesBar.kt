package com.example.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.designsystem.fluidPressEffect

/**
 * MIZAN Floating Top Capsules Bar (Floating Islands Architecture).
 *
 * Replaces the traditional rigid rectangular toolbar with two ultra-modern,
 * floating independent oval pills (Capsules):
 * 1. Brand Capsule (Left): MIZAN logo, brand identity typography, and active accent badge.
 * 2. Quick Actions Capsule (Right): Search with smooth morphing expansion, Wi-Fi status, refresh, and profile avatar.
 *
 * Color Palette:
 * - Capsule Background: #FFFDF8 (Ivory Warm White)
 * - Capsule Border: 1dp #E3E5DC
 * - Icons & Primary Text: #151515
 * - Accent / Highlight: #C8F24A (Mizan Lime)
 * - Muted / Secondary: #777A72
 */
@Composable
fun MizanTopCapsulesBar(
    brandTitle: String = "ميزان",
    brandSubtitle: String? = null,
    showSearch: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {},
    searchPlaceholder: String = "بحث...",
    onWifiClick: (() -> Unit)? = null,
    isWifiConnected: Boolean = true,
    onRefreshClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    userPhotoUrl: String = "",
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val capsuleBg = Color(0xFFFFFDF8)
    val capsuleBorder = Color(0xFFE3E5DC)
    val charcoal = Color(0xFF151515)
    val lime = Color(0xFFC8F24A)
    val mutedGray = Color(0xFF777A72)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .animateContentSize(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                )
                .testTag("floating_top_capsules_bar"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearchActive) {
                // Expanded Morphing Search Capsule (Full Width)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(23.dp),
                            ambientColor = Color.Black.copy(alpha = 0.08f),
                            spotColor = Color.Black.copy(alpha = 0.06f)
                        )
                        .clip(RoundedCornerShape(23.dp))
                        .background(capsuleBg)
                        .border(1.dp, capsuleBorder, RoundedCornerShape(23.dp)),
                    color = capsuleBg,
                    shape = RoundedCornerShape(23.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = charcoal,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = searchPlaceholder,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp,
                                        color = mutedGray
                                    )
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = charcoal
                                ),
                                cursorBrush = SolidColor(charcoal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("expanded_search_input")
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { onSearchQueryChange("") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    tint = mutedGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // Close / Back button to collapse search
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0EFEA))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSearchActiveChange(false)
                                    onSearchQueryChange("")
                                }
                                .testTag("close_search_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close Search",
                                tint = charcoal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Capsule 1: Brand & Identity Capsule (Left)
                Surface(
                    modifier = Modifier
                        .height(46.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(23.dp),
                            ambientColor = Color.Black.copy(alpha = 0.06f),
                            spotColor = Color.Black.copy(alpha = 0.05f)
                        )
                        .clip(RoundedCornerShape(23.dp))
                        .background(capsuleBg)
                        .border(1.dp, capsuleBorder, RoundedCornerShape(23.dp))
                        .testTag("top_capsule_brand"),
                    color = capsuleBg,
                    shape = RoundedCornerShape(23.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Brand Logo Icon
                        Image(
                            painter = painterResource(id = R.drawable.mizan_logo_final),
                            contentDescription = "MIZAN Logo",
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Brand Name
                        Text(
                            text = brandTitle,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = charcoal
                            )
                        )

                        if (brandSubtitle != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(mutedGray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = brandSubtitle,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = mutedGray
                                )
                            )
                        } else {
                            Spacer(modifier = Modifier.width(6.dp))
                            // Subtle Lime Active Dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(lime)
                            )
                        }
                    }
                }

                // Capsule 2: Quick Actions & Tools Capsule (Right)
                Surface(
                    modifier = Modifier
                        .height(46.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(23.dp),
                            ambientColor = Color.Black.copy(alpha = 0.06f),
                            spotColor = Color.Black.copy(alpha = 0.05f)
                        )
                        .clip(RoundedCornerShape(23.dp))
                        .background(capsuleBg)
                        .border(1.dp, capsuleBorder, RoundedCornerShape(23.dp))
                        .testTag("top_capsule_actions"),
                    color = capsuleBg,
                    shape = RoundedCornerShape(23.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Action 1: Search Button (if enabled)
                        if (showSearch) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF7F6F1))
                                    .fluidPressEffect(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSearchActiveChange(true)
                                    })
                                    .testTag("top_action_search"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "بحث",
                                    tint = charcoal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Action 2: Wi-Fi Status Button (if provided)
                        if (onWifiClick != null) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isWifiConnected) Color(0xFFE5F5BE) else Color(0xFFF7F6F1))
                                    .fluidPressEffect(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onWifiClick()
                                    })
                                    .testTag("top_action_wifi"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isWifiConnected) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                                    contentDescription = "حالة الشبكة",
                                    tint = if (isWifiConnected) Color(0xFF4D6F18) else charcoal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Action 3: Refresh Button (if provided)
                        if (onRefreshClick != null) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF7F6F1))
                                    .fluidPressEffect(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onRefreshClick()
                                    })
                                    .testTag("top_action_refresh"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "تحديث",
                                    tint = charcoal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Action 4: Google Profile Avatar Button (if provided)
                        if (onProfileClick != null) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .border(1.2.dp, lime, CircleShape)
                                    .fluidPressEffect(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onProfileClick()
                                    })
                                    .testTag("top_action_profile"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userPhotoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = userPhotoUrl,
                                        contentDescription = "Profile Avatar",
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = "Profile",
                                        tint = charcoal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
