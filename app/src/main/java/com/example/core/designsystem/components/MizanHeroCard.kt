package com.example.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanElevation
import com.example.core.designsystem.MizanShapes
import com.example.core.designsystem.MizanSpacing

/**
 * Large rounded Hero Card with 28dp radius based on DESIGN.md
 */
@Composable
fun MizanHeroCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MizanColors.WarmWhite,
    contentColor: Color = MizanColors.Charcoal,
    borderColor: Color? = MizanColors.Line,
    elevation: Dp = MizanElevation.Card,
    internalPadding: Dp = MizanSpacing.HeroInternalPadding,
    testTag: String = "mizan_hero_card",
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = MizanShapes.HeroPanel,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = if (borderColor != null) BorderStroke(1.dp, borderColor) else null
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(internalPadding)
        ) {
            content()
        }
    }
}
