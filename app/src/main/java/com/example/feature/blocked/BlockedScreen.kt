package com.example.feature.blocked

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * MIZAN Quota Exhausted / Blocked Screen (شاشة انتهاء الحصة وإيقاف الإنترنت).
 *
 * Requirements matching:
 * 1. Charcoal full-screen background (#121212 / #141414).
 * 2. Pale-lime rounded square card container holding the transparent house image asset (R.drawable.mizan_setup_house), Wi-Fi lines, and lock icon.
 * 3. Exact Arabic Typography:
 *    - Title: "اكتملت حصتك الشهرية"
 *    - Subtitle: "تم إيقاف الإنترنت مؤقتاً"
 *    - Status Badge: "بانتظار تمديد الحصة" (Rounded pale-lime pill with hourglass icon)
 *    - Bottom Notice: "سيعود الاتصال عند تحديث الحد المسموح" (Replaced the button with this direct calm notice).
 * 4. RTL support and state decision separated from the visual composable.
 */
@Composable
fun BlockedScreen(
    state: BlockedUiState = BlockedUiState(),
    onBackToHome: () -> Unit = {},
    onBackToSetup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color(0xFF141414) // Deep charcoal canvas
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Center Content Block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Pale Lime Rounded Square Card with the new custom uploaded illustration
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(38.dp))
                            .background(Color(0xFFE5F5BE))
                            .testTag("blocked_illustration_card"),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.mizan_blocked_illustration),
                            contentDescription = "رسم توضيحي لانتهاء الحصة",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(26.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(34.dp))

                    // 2. Title: اكتملت حصتك الشهرية
                    Text(
                        text = state.title,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.3).sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("blocked_title_text")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Subtitle: تم إيقاف الإنترنت مؤقتاً
                    Text(
                        text = state.subtitle,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            color = Color(0xFFB5BAAA),
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("blocked_subtitle_text")
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 4. Status Badge Pill: بانتظار تمديد الحصة
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFE5F5BE))
                            .padding(horizontal = 24.dp, vertical = 11.dp)
                            .testTag("blocked_status_pill"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HourglassEmpty,
                                contentDescription = null,
                                tint = Color(0xFF141414),
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.statusBadgeText,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF141414)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 5. Notice Text replacing button: سيعود الاتصال عند تحديث الحد المسموح
                Text(
                    text = state.returnNoticeText,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF8A9080),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("blocked_return_notice_text")
                )
            }
        }
    }
}

/**
 * Route Composable for Blocked / Quota Exhausted Screen.
 */
@Composable
fun BlockedRoute(
    state: BlockedUiState = BlockedUiState(),
    modifier: Modifier = Modifier
) {
    BlockedScreen(
        state = state,
        modifier = modifier
    )
}

// -------------------------------------------------------------------
// RTL Arabic Compose Previews
// -------------------------------------------------------------------

@Preview(
    name = "Blocked Screen - RTL Arabic Quota Exhausted",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun BlockedScreenRtlPreview() {
    BlockedScreen(
        state = BlockedUiState()
    )
}
