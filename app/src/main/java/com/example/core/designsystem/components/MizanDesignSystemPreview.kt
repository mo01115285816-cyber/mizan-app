package com.example.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanSpacing
import com.example.core.designsystem.MizanTheme
import com.example.core.designsystem.MizanTypography

/**
 * Design system catalog preview showcasing all reusable components
 * with Light theme and Arabic RTL layout.
 */
@Composable
fun MizanDesignSystemCatalog(
    modifier: Modifier = Modifier
) {
    var textValue by remember { mutableStateOf("") }
    var selectedNavId by remember { mutableStateOf("home") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MizanColors.Paper)
            .verticalScroll(rememberScrollState())
            .padding(MizanSpacing.ScreenHorizontalPadding)
    ) {
        // Motif Icon & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MizanHomeWifiIcon(size = 40.dp)
            Text(
                text = "نظام تصميم ميزان (MIZAN)",
                style = MizanTypography.Headline,
                color = MizanColors.Charcoal
            )
        }

        Spacer(modifier = Modifier.height(MizanSpacing.SectionSpacing))

        // Status Pills
        MizanSectionTitle(title = "شرائح الحالة (Status Pills)")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MizanStatusPill(text = "متصل بالمنزل", style = MizanPillStyle.SoftMint)
            MizanStatusPill(text = "الرئيسية", style = MizanPillStyle.Lime)
            MizanStatusPill(text = "تم الإيقاف", style = MizanPillStyle.Error)
        }

        Spacer(modifier = Modifier.height(MizanSpacing.SectionSpacing))

        // Hero Card & Usage Ring
        MizanSectionTitle(title = "البطاقة الرئيسية وحلقة الاستهلاك")
        Spacer(modifier = Modifier.height(8.dp))
        MizanHeroCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                MizanUsageRing(percentage = 0.64f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "64%",
                            style = MizanTypography.NumericDisplay,
                            color = MizanColors.Charcoal
                        )
                        Text(
                            text = "85.3 GB",
                            style = MizanTypography.Title,
                            color = MizanColors.Charcoal
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "من أصل 133.3 GB",
                    style = MizanTypography.Body,
                    color = MizanColors.MutedGray
                )
            }
        }

        Spacer(modifier = Modifier.height(MizanSpacing.SectionSpacing))

        // Text Fields
        MizanSectionTitle(title = "حقول الإدخال (Inputs)")
        Spacer(modifier = Modifier.height(8.dp))
        MizanTextField(
            value = textValue,
            onValueChange = { textValue = it },
            label = "اسمك",
            placeholder = "مثال: مؤمن"
        )

        Spacer(modifier = Modifier.height(MizanSpacing.SectionSpacing))

        // Buttons
        MizanSectionTitle(title = "الأزرار (Buttons)")
        Spacer(modifier = Modifier.height(8.dp))
        MizanPrimaryButton(
            text = "حفظ وتفعيل",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        MizanOutlineButton(
            text = "عرض التفاصيل",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(MizanSpacing.SectionSpacing))

        // Usage Rows
        MizanSectionTitle(title = "قائمة الاستهلاك (Usage Rows)")
        Spacer(modifier = Modifier.height(8.dp))
        MizanUsageRow(appName = "تيك توك", consumedAmount = "30 GB", rank = 1)
        MizanUsageRow(appName = "يوتيوب", consumedAmount = "15 GB", rank = 2)
        MizanUsageRow(appName = "فيسبوك", consumedAmount = "5 GB", rank = 3, showDivider = false)

        Spacer(modifier = Modifier.height(MizanSpacing.SectionSpacing))

        // Bottom Navigation Sample
        MizanSectionTitle(title = "شريط التنقل (Bottom Navigation)")
        Spacer(modifier = Modifier.height(8.dp))
        MizanBottomNavigation(
            items = listOf(
                MizanNavItem("home", "الرئيسية") {},
                MizanNavItem("usage", "الاستهلاك") {},
                MizanNavItem("account", "الحساب") {}
            ),
            selectedItemId = selectedNavId,
            onItemSelected = { selectedNavId = it }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun MizanDesignSystemCatalogPreview() {
    MizanTheme(forceRtl = true) {
        MizanDesignSystemCatalog()
    }
}
