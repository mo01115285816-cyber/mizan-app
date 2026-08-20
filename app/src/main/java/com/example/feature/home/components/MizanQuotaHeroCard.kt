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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import java.util.Locale

@Composable
fun MizanQuotaHeroCard(
    usedGb: Float,
    quotaGb: Float,
    remainingGb: Float,
    percentage: Int,
    connectionStatus: String,
    onConnectionPillClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFEAF7CD)
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(backgroundColor)
            .padding(horizontal = 18.dp, vertical = 20.dp)
            .testTag("mizan_quota_hero_card")
    ) {
        val compact = maxWidth < 380.dp
        val ringSize = if (compact) 116.dp else 132.dp
        val numberSize = if (compact) 32.sp else 38.sp

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (compact) {
                MizanHeroUsageRing(
                    percentage = percentage,
                    size = ringSize,
                    strokeWidth = 12.dp
                )
                Spacer(modifier = Modifier.height(18.dp))
                QuotaStats(
                    usedGb = usedGb,
                    quotaGb = quotaGb,
                    remainingGb = remainingGb,
                    numberSize = numberSize,
                    compact = true
                )
            } else {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MizanHeroUsageRing(
                            percentage = percentage,
                            size = ringSize,
                            strokeWidth = 14.dp
                        )
                        QuotaStats(
                            usedGb = usedGb,
                            quotaGb = quotaGb,
                            remainingGb = remainingGb,
                            numberSize = numberSize,
                            compact = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            ConnectionStatusPill(
                connectionStatus = connectionStatus,
                onClick = onConnectionPillClick
            )
        }
    }
}

@Composable
private fun QuotaStats(
    usedGb: Float,
    quotaGb: Float,
    remainingGb: Float,
    numberSize: androidx.compose.ui.unit.TextUnit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val valueColor = Color(0xFF151515)
    val remainingColor = Color(0xFF3F7E16)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "GB",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 17.sp else 19.sp,
                        color = valueColor
                    ),
                    modifier = Modifier.padding(bottom = 3.dp, end = 5.dp),
                    maxLines = 1
                )
                Text(
                    text = formatGb(usedGb),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = numberSize,
                        color = valueColor
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Text(
            text = "من أصل ${formatGb(quotaGb)} جيجابايت",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = if (compact) 14.sp else 15.sp,
                color = Color(0xFF6F7368),
                textAlign = TextAlign.End
            ),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            softWrap = false
        )

        Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {
            drawLine(
                color = Color(0xFFCDDFAD),
                start = Offset.Zero.copy(y = size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))
        Text(
            text = "متبقي",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 14.sp else 15.sp,
                color = valueColor,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "GB",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 16.sp else 17.sp,
                        color = remainingColor
                    ),
                    modifier = Modifier.padding(bottom = 2.dp, end = 5.dp),
                    maxLines = 1
                )
                Text(
                    text = formatGb(remainingGb),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = if (compact) 24.sp else 27.sp,
                        color = remainingColor
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusPill(
    connectionStatus: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF151515))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag("mizan_connection_status_pill"),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = connectionStatus,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(5.dp))
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

private fun formatGb(value: Float): String =
    String.format(Locale.US, "%.1f", value.coerceAtLeast(0f))

@Composable
private fun MizanHeroUsageRing(
    percentage: Int,
    size: Dp,
    strokeWidth: Dp,
    modifier: Modifier = Modifier
) {
    val animatedPercent by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
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
            drawArc(
                color = Color(0xFFDBEFB7),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
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

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = percentage.coerceIn(0, 100).toString(),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 31.sp,
                        color = Color(0xFF151515)
                    ),
                    maxLines = 1
                )
                Text(
                    text = "%",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF151515)
                    ),
                    modifier = Modifier.padding(bottom = 3.dp, start = 1.dp),
                    maxLines = 1
                )
            }
        }
    }
}
