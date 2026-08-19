package com.example.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.home.DayUsage

/**
 * White Insight Card for 7-day usage trend and daily average.
 * Displays:
 * 1. "متوسطك اليومي" + "3.1 GB" on the left
 * 2. Chip "آخر 7 أيام" on the right
 * 3. 7-point smooth line chart with gradient area fill, circular nodes, dashed grid lines, and Arabic day labels
 */
@Composable
fun MizanDailyInsightCard(
    dailyAverageGb: Float,
    dailyTrend: List<DayUsage>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .border(1.2.dp, Color(0xFFE8EAE0), RoundedCornerShape(26.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag("mizan_daily_insight_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Left = Average Stats, Right = 7-Day Chip
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Average Stats
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "متوسطك اليومي",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = Color(0xFF6F7368)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.3f", dailyAverageGb),
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = Color(0xFF151515)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GB",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF151515)
                                ),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    // 7-day pill chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFEFF8DF))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "آخر 7 أيام",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color(0xFF151515)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7-Point Line Chart
            MizanUsageLineChart(
                points = dailyTrend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            )
        }
    }
}

/**
 * 7-point smooth line chart rendered on Canvas with gradient fill and Arabic day labels.
 */
@Composable
private fun MizanUsageLineChart(
    points: List<DayUsage>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val w = size.width
            val h = size.height
            val n = points.size

            if (n < 2) return@Canvas

            val stepX = w / (n - 1)
            val minVal = 0.8f
            val maxVal = 4.2f
            val range = maxVal - minVal

            val coords = points.mapIndexed { index, day ->
                val x = index * stepX
                val normalized = ((day.valueGb - minVal) / range).coerceIn(0.1f, 0.9f)
                val y = h - (normalized * h)
                Offset(x, y)
            }

            // 1. Draw subtle vertical dashed grid lines under each point
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            coords.forEach { pt ->
                drawLine(
                    color = Color(0xFFE8ECE0),
                    start = Offset(pt.x, pt.y),
                    end = Offset(pt.x, h),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            // 2. Build smooth cubic bezier path for curve & gradient fill
            val strokePath = Path()
            val fillPath = Path()

            strokePath.moveTo(coords[0].x, coords[0].y)
            fillPath.moveTo(coords[0].x, h)
            fillPath.lineTo(coords[0].x, coords[0].y)

            for (i in 0 until coords.size - 1) {
                val p0 = coords[i]
                val p1 = coords[i + 1]
                val midX = (p0.x + p1.x) / 2f
                strokePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }

            fillPath.lineTo(coords.last().x, h)
            fillPath.close()

            // Draw Area Fill Gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x4082C826),
                        Color(0x1082C826),
                        Color(0x0082C826)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // Draw Smooth Green Stroke
            drawPath(
                path = strokePath,
                color = Color(0xFF7CBD24),
                style = Stroke(
                    width = 2.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw Point Nodes
            coords.forEach { pt ->
                // Outer ring
                drawCircle(
                    color = Color(0xFFFFFFFF),
                    radius = 5.dp.toPx(),
                    center = pt
                )
                // Inner green dot
                drawCircle(
                    color = Color(0xFF7CBD24),
                    radius = 4.dp.toPx(),
                    center = pt
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Arabic Day Labels across the bottom
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { day ->
                    Text(
                        text = day.dayLabel,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = Color(0xFF8E9284),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}
