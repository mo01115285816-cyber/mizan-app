package com.example.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanShapes
import com.example.core.designsystem.MizanSpacing
import com.example.core.designsystem.MizanTypography

/**
 * Circular progress ring for Wi-Fi quota consumption.
 * Displays large percentage and bold numeric details.
 */
@Composable
fun MizanUsageRing(
    percentage: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 14.dp,
    trackColor: Color = MizanColors.SoftMint,
    progressColor: Color = MizanColors.Lime,
    centerContent: @Composable () -> Unit = {}
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "usage_ring_anim"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("mizan_usage_ring"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val diameter = this.size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(diameter, diameter)

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Progress Arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedPercentage,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        centerContent()
    }
}

/**
 * Row displaying an individual application's data consumption.
 */
@Composable
fun MizanUsageRow(
    appName: String,
    consumedAmount: String,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    icon: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    testTag: String = "mizan_usage_row"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (rank != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MizanColors.Paper),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rank.toString(),
                            style = MizanTypography.Label,
                            color = MizanColors.Charcoal
                        )
                    }
                    Spacer(modifier = Modifier.width(MizanSpacing.LabelToControlSpacing))
                }

                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(MizanSpacing.LabelToControlSpacing))
                }

                Text(
                    text = appName,
                    style = MizanTypography.Body,
                    color = MizanColors.Charcoal
                )
            }

            Text(
                text = consumedAmount,
                style = MizanTypography.Title,
                color = MizanColors.Charcoal
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = MizanColors.Line,
                thickness = 1.dp
            )
        }
    }
}
