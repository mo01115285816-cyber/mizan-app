package com.example.feature.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanTypography
import com.example.core.designsystem.fluidPressEffect
import com.example.core.model.AppState

@Composable
fun SetupScreen(
    appState: AppState,
    onGoogleSignInClick: () -> Unit,
    onManualInviteSubmit: (String) -> Unit,
    onRetryClick: () -> Unit,
    onSignOutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MizanColors.Paper)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Bar: Clean Brand Title & optional Sign Out
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("mizan_brand_logo")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mizan_logo_final),
                        contentDescription = "Mizan Logo",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "MIZAN",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 2.sp,
                            color = MizanColors.Charcoal
                        )
                    )
                }

                if (appState is AppState.WaitingForInvite) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MizanColors.WarmWhite)
                            .border(1.dp, MizanColors.Line, RoundedCornerShape(16.dp))
                            .fluidPressEffect(onClick = onSignOutClick)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "خروج",
                            style = MizanTypography.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Middle Scrollable Interactive Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (appState) {
                    is AppState.SignedOut -> {
                        SignedOutContent(onGoogleSignInClick = onGoogleSignInClick)
                    }
                    is AppState.SigningIn -> {
                        LoadingStateContent(
                            title = "جاري تسجيل الدخول",
                            subtitle = "يتم التحقق من حساب Google وتهيئة الجلسة..."
                        )
                    }
                    is AppState.SessionRestoring -> {
                        LoadingStateContent(
                            title = "جاري استعادة الجلسة",
                            subtitle = "التحقق من بيانات العضوية والجهاز..."
                        )
                    }
                    is AppState.WaitingForInvite -> {
                        WaitingForInviteContent(onSubmitInvite = onManualInviteSubmit)
                    }
                    is AppState.JoiningHousehold -> {
                        LoadingStateContent(
                            title = "جاري الانضمام إلى المنزل",
                            subtitle = "يتم تفعيل الدعوة وربط العضوية..."
                        )
                    }
                    is AppState.DeviceLinking -> {
                        LoadingStateContent(
                            title = "جاري ربط الهاتف",
                            subtitle = "تسجيل معرف الجهاز وتهيئة سياسات الاستهلاك..."
                        )
                    }
                    is AppState.AuthError -> {
                        ErrorStateContent(
                            title = "تعذر تسجيل الدخول",
                            errorMessage = appState.message,
                            buttonText = "إعادة المحاولة",
                            onAction = onRetryClick
                        )
                    }
                    is AppState.InviteError -> {
                        ErrorStateContent(
                            title = "خطأ في رابط الدعوة",
                            errorMessage = appState.message,
                            buttonText = "محاولة رابط آخر",
                            onAction = onRetryClick
                        )
                    }
                    is AppState.NetworkError -> {
                        ErrorStateContent(
                            title = "خطأ في الاتصال",
                            errorMessage = appState.message,
                            buttonText = "إعادة المحاولة",
                            onAction = onRetryClick
                        )
                    }
                    else -> {}
                }
            }
        }

        // 3. Bottom-most Footer Notice (At the very bottom of the page)
        Spacer(modifier = Modifier.height(8.dp))

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("setup_footer_note"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color(0xFF4D6F18),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "نظام MIZAN لحماية وتوزيع حصص الإنترنت العائلي",
                    style = MizanTypography.Caption,
                    color = MizanColors.MutedGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SignedOutContent(
    onGoogleSignInClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Text(
            text = "أهلاً بك في MIZAN",
            style = MizanTypography.DisplayArabic,
            color = MizanColors.Charcoal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "تطبيق متابعة الحصة العائلية وإدارة استهلاك الهاتف",
            style = MizanTypography.Body,
            color = MizanColors.MutedGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        MizanSetupIllustration(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup_hero_illustration")
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Google Sign-In Pill Button with tactile fluid press effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MizanColors.Charcoal)
                .fluidPressEffect(onClick = onGoogleSignInClick)
                .testTag("google_sign_in_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleGLogo(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "تسجيل الدخول بحساب Google",
                    style = MizanTypography.Button,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun WaitingForInviteContent(
    onSubmitInvite: (String) -> Unit
) {
    var inviteInput by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MizanColors.Lime),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MarkEmailRead,
                contentDescription = null,
                tint = MizanColors.Charcoal,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "في انتظار الانضمام للمنزل",
            style = MizanTypography.Headline,
            color = MizanColors.Charcoal,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "تم تسجيل الدخول بنجاح! اضغط على رابط الدعوة المرسل من رب الأسرة للانضمام تلقائياً، أو ألصق رمز الدعوة بالأسفل.",
            style = MizanTypography.Body,
            color = MizanColors.MutedGray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Invite link / Token input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MizanColors.WarmWhite)
                .border(1.2.dp, MizanColors.Line, RoundedCornerShape(28.dp))
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MizanColors.Charcoal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (inviteInput.isEmpty()) {
                            Text(
                                text = "ألصق رابط أو رمز الدعوة هنا",
                                style = MizanTypography.BodyMedium,
                                color = MizanColors.MutedGray
                            )
                        }
                        BasicTextField(
                            value = inviteInput,
                            onValueChange = { inviteInput = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = MizanColors.Charcoal
                            ),
                            cursorBrush = SolidColor(MizanColors.Charcoal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(if (inviteInput.isNotBlank()) MizanColors.Charcoal else Color(0xFFCCCCCC))
                .fluidPressEffect(
                    onClick = {
                        if (inviteInput.isNotBlank()) {
                            val token = inviteInput.trim().substringAfterLast("/")
                            onSubmitInvite(token)
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "انضمام للمنزل",
                style = MizanTypography.Button,
                color = Color.White
            )
        }
    }
}

@Composable
private fun LoadingStateContent(
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 40.dp)
    ) {
        CircularProgressIndicator(
            color = MizanColors.Charcoal,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MizanTypography.Headline,
            color = MizanColors.Charcoal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MizanTypography.Body,
            color = MizanColors.MutedGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorStateContent(
    title: String,
    errorMessage: String,
    buttonText: String,
    onAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MizanColors.ErrorSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MizanTypography.Headline,
            color = MizanColors.Charcoal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MizanTypography.BodyMedium,
            color = Color(0xFFE53935),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(MizanColors.Charcoal)
                .fluidPressEffect(onClick = onAction),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                style = MizanTypography.Button,
                color = Color.White
            )
        }
    }
}

@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
    Text(
        text = "G",
        style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = MizanColors.Lime
        ),
        modifier = modifier
    )
}

@Composable
private fun MizanLogoDots(modifier: Modifier = Modifier) {
    val dotColor = MizanColors.Lime
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(dotColor)
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(dotColor)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(dotColor)
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(dotColor)
            )
        }
    }
}

