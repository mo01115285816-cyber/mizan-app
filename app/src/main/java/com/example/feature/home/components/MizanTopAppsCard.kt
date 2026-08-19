package com.example.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
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
import com.example.core.model.AppUsageItem

import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.runtime.remember
import android.graphics.drawable.Drawable

/**
 * Top consuming applications ranking card.
 * Shows native vector glyphs, dynamic progress bars, and real consumed GB amounts.
 */
@Composable
fun MizanTopAppsCard(
    apps: List<AppUsageItem>,
    modifier: Modifier = Modifier
) {
    val maxUsage = (apps.maxOfOrNull { it.consumedGb } ?: 1.0f).coerceAtLeast(0.1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .border(1.2.dp, Color(0xFFE8EAE0), RoundedCornerShape(26.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .testTag("mizan_top_apps_card")
    ) {
        if (apps.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "لا توجد تفاصيل استهلاك مسجلة حتى الآن",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF7A7A7A),
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "سيتم إدراج التطبيقات تلقائياً عند استخدام شبكة Wi-Fi وتوفر إذن الوصول",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = Color(0xFFA0A0A0),
                        textAlign = TextAlign.Center
                    )
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                apps.forEach { app ->
                    AppUsageRowItem(
                        app = app,
                        progress = (app.consumedGb / maxUsage).coerceIn(0.05f, 1.0f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUsageRowItem(
    app: AppUsageItem,
    progress: Float,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .testTag("app_row_${app.appName.lowercase()}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon Image
            AppIconImage(
                packageName = app.packageName,
                appName = app.appName,
                modifier = Modifier.size(42.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Center: App Name + Lime Progress Bar
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = app.appName,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF151515)
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFF2F4EC))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFC0EE2B))
                    )
                }
            }

            // Right: Consumed GB
            Row(verticalAlignment = Alignment.Bottom) {
                val formattedGb = String.format(java.util.Locale.US, "%.3f", app.consumedGb)

                Text(
                    text = formattedGb,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF151515)
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "GB",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF151515)
                    )
                )
            }
        }
    }
}

/**
 * Loads actual app icons from the system using the package name.
 */
@Composable
private fun AppIconImage(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    val icon = remember(packageName) {
        try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    if (icon != null) {
        AsyncImage(
            model = icon,
            contentDescription = appName,
            modifier = modifier.clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE8EAE0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName.take(1).uppercase(),
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF151515))
            )
        }
    }
}
