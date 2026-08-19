package com.example.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.MizanColors

/**
 * 2dp stroke line icon for the recurring Mizan motif: Home + Router / Wi-Fi signal.
 */
@Composable
fun MizanHomeWifiIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = MizanColors.Charcoal,
    strokeWidth: Dp = 2.dp,
    testTag: String = "mizan_home_wifi_icon"
) {
    Box(
        modifier = modifier
            .size(size)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val w = this.size.width
            val h = this.size.height

            // 1. House Roof & Frame
            val housePath = Path().apply {
                moveTo(w * 0.15f, h * 0.45f)
                lineTo(w * 0.50f, h * 0.18f)
                lineTo(w * 0.85f, h * 0.45f)
                lineTo(w * 0.85f, h * 0.82f)
                lineTo(w * 0.15f, h * 0.82f)
                close()
            }

            drawPath(
                path = housePath,
                color = tint,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 2. Wi-Fi Arc 1 (Outer)
            drawArc(
                color = tint,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.32f, h * 0.42f),
                size = Size(w * 0.36f, h * 0.36f),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 3. Wi-Fi Arc 2 (Inner)
            drawArc(
                color = tint,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.40f, h * 0.52f),
                size = Size(w * 0.20f, h * 0.20f),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 4. Wi-Fi Dot
            drawCircle(
                color = tint,
                radius = strokePx * 1.1f,
                center = Offset(w * 0.50f, h * 0.70f)
            )
        }
    }
}
