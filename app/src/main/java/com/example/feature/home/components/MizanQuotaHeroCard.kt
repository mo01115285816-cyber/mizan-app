package com.example.feature.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Large Mint / Lime Quota Hero Card.
 * Displays:
 * 1. 64% circular usage ring on the left
 * 2. 85.3 GB, total quota subtitle, dotted divider, and 48.0 GB remaining on the right
 * 3. Interactive Charcoal status pill displaying the dynamic connected network name
 */
@Composable
fun MizanQuotaHeroCard(
    usedGb: Float,
    quotaGb: Float,
    remainingGb: Float,
    percentage: Int,
    connectionStatus: String,
    onConnectionPillClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFEAF7CD) // Soothing mint-lime background
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(backgroundColor)
            .padding(horizontal = 20.dp, vertical = 22.dp)
            .testTag("mizan_quota_hero_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Top Row: Left = Ring, Right = Stats
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Circular Usage Ring
                    MizanHeroUsageRing(
                        percentage = percentage,
                        size = 145.dp,
                        strokeWidth = 14.dp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // Right: Arabic Quota Stats Block
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, end = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // 85.3 GB
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "GB",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = Color(0xFF151515)
                                ),
                                modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.3f", usedGb),
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 38.sp,
                                    color = Color(0xFF151515)
                                )
                            )
                        }

                        // من أصل 133.3 ج.ب
                        Text(
                            text = "من أصل $quotaGb ج.ب",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                color = Color(0xFF6F7368),
                                textAlign = TextAlign.End
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dotted Divider Line
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(2.dp)
                        ) {
                            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            drawLine(
                                color = Color(0xFFCDDFAD),
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = pathEffect,
                                cap = StrokeCap.Round
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // متبقي
                        Text(
                            text = "متبقي",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF151515),
                                textAlign = TextAlign.End
                            )
                        )

                        // 48.0 GB
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "GB",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color(0xFF3F7E16)
                                ),
                                modifier = Modifier.padding(bottom = 2.dp, end = 4.dp)
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.3f", remainingGb),
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 25.sp,
                                    color = Color(0xFF3F7E16)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Center Interactive Status Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF151515))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onConnectionPillClick
                    )
                    .padding(horizontal = 18.dp, vertical = 9.dp)
                    .testTag("mizan_connection_status_pill"),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Green Home / Wi-Fi icon
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.2.dp, Color(0xFFD6F355), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = null,
                                tint = Color(0xFFD6F355),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(9.dp))

                        Text(
                            text = connectionStatus,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = "عرض التفاصيل",
                            tint = Color(0xFFB0B4A8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Usage Ring with Animated Progress and bold center percentage.
 */
@Composable
private fun MizanHeroUsageRing(
    percentage: Int,
    size: Dp,
    strokeWidth: Dp,
    modifier: Modifier = Modifier
) {
    val animatedPercent by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "hero_usage_ring"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val diameter = this.size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(diameter, diameter)

            // Background soft track
            drawArc(
                color = Color(0xFFDBEFB7),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Active Green progress arc
            drawArc(
                color = Color(0xFF86CF1A),
                startAngle = -90f,
                sweepAngle = 360f * animatedPercent,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        // Percentage text inside ring
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$percentage",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 33.sp,
                    color = Color(0xFF151515)
                )
            )
            Text(
                text = "%",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color(0xFF151515)
                ),
                modifier = Modifier.padding(bottom = 3.dp, start = 1.dp)
            )
        }
    }
}
