package com.example.feature.home.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanTypography
import com.example.core.designsystem.fluidPressEffect

/**
 * Modern Profile & Account Bottom Sheet for Mizan.
 * Opened when tapping the top profile avatar.
 * Displays Google user photo, account details, household role, and safe logout options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MizanProfileBottomSheet(
    userName: String,
    userEmail: String,
    userPhotoUrl: String,
    householdId: String,
    deviceModel: String,
    onDismissRequest: () -> Unit,
    onSignOutClick: () -> Unit,
    onSyncNowClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MizanColors.WarmWhite,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User Avatar Header
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MizanColors.SoftMint)
                        .border(2.dp, MizanColors.Lime, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (userPhotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = userPhotoUrl,
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MizanColors.Charcoal,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (userName.isNotBlank()) userName else "عضو العائلة",
                    style = MizanTypography.Headline,
                    color = MizanColors.Charcoal
                )

                if (userEmail.isNotBlank()) {
                    Text(
                        text = userEmail,
                        style = MizanTypography.Caption,
                        color = MizanColors.MutedGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Account Info Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MizanColors.Paper)
                        .border(1.dp, MizanColors.Line, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ProfileInfoRow(
                        icon = Icons.Outlined.Home,
                        title = "المنزل المرتبط",
                        value = if (householdId.isNotBlank()) "منزل مفعل (#${householdId.take(8)})" else "المنزل الافتراضي"
                    )
                    HorizontalDivider(color = MizanColors.Line.copy(alpha = 0.6f))
                    ProfileInfoRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = "الجهاز الحالي",
                        value = deviceModel.ifBlank { "هاتف أندرويد" }
                    )
                    HorizontalDivider(color = MizanColors.Line.copy(alpha = 0.6f))
                    ProfileInfoRow(
                        icon = Icons.Outlined.Shield,
                        title = "نظام الحماية والكوتا",
                        value = "نشط ويعمل بنجاح"
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sync Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MizanColors.Lime)
                            .fluidPressEffect(onClick = {
                                onSyncNowClick()
                                onDismissRequest()
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = MizanColors.Charcoal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مزامنة البيانات",
                                style = MizanTypography.Button,
                                color = MizanColors.Charcoal
                            )
                        }
                    }

                    // Logout Button with Safe Confirm Dialog
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MizanColors.ErrorSoft)
                            .border(1.dp, Color(0xFFF0B8B0), RoundedCornerShape(16.dp))
                            .fluidPressEffect(onClick = {
                                showSignOutConfirmDialog = true
                            })
                            .testTag("profile_sheet_sign_out_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Logout,
                                contentDescription = null,
                                tint = Color(0xFF9E2A1E),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تسجيل الخروج",
                                style = MizanTypography.Button,
                                color = Color(0xFF9E2A1E)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSignOutConfirmDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirmDialog = false },
                title = {
                    Text(
                        text = "تسجيل الخروج من الحساب؟",
                        style = MizanTypography.Title,
                        color = MizanColors.Charcoal
                    )
                },
                text = {
                    Text(
                        text = "سيتم إلغاء ربط هذا الجهاز بمنزلك وإيقاف تتبع الحصص حتى تعاود تسجيل الدخول مرة أخرى.",
                        style = MizanTypography.Body,
                        color = MizanColors.Charcoal
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSignOutConfirmDialog = false
                            onDismissRequest()
                            onSignOutClick()
                        }
                    ) {
                        Text(
                            text = "نعم، تسجيل الخروج",
                            style = MizanTypography.Button,
                            color = Color(0xFFD32F2F)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutConfirmDialog = false }) {
                        Text(
                            text = "إلغاء",
                            style = MizanTypography.Button,
                            color = MizanColors.Charcoal
                        )
                    }
                },
                containerColor = MizanColors.WarmWhite,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MizanColors.Charcoal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MizanTypography.BodyMedium,
                color = MizanColors.Charcoal
            )
        }
        Text(
            text = value,
            style = MizanTypography.Caption,
            color = MizanColors.MutedGray,
            fontWeight = FontWeight.SemiBold
        )
    }
}
