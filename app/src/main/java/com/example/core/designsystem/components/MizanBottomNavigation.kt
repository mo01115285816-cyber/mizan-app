package com.example.core.designsystem.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanElevation
import com.example.core.designsystem.MizanShapes
import com.example.core.designsystem.MizanSpacing
import com.example.core.designsystem.MizanTypography

data class MizanNavItem(
    val id: String,
    val title: String,
    val icon: @Composable (isSelected: Boolean) -> Unit
)

/**
 * Bottom navigation component with Lime pill selection container and WarmWhite surface.
 */
@Composable
fun MizanBottomNavigation(
    items: List<MizanNavItem>,
    selectedItemId: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "mizan_bottom_navigation"
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        color = MizanColors.WarmWhite,
        tonalElevation = MizanElevation.Card,
        shadowElevation = MizanElevation.Card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MizanSpacing.ScreenHorizontalPadding,
                    vertical = 12.dp
                ),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.id == selectedItemId
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .clip(MizanShapes.Pill)
                        .background(if (isSelected) MizanColors.Lime else MizanColors.WarmWhite)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onItemSelected(item.id) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        item.icon(isSelected)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = item.title,
                            style = MizanTypography.Label,
                            color = if (isSelected) MizanColors.Charcoal else MizanColors.MutedGray
                        )
                    }
                }
            }
        }
    }
}
