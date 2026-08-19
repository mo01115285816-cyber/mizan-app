package com.example.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanShapes
import com.example.core.designsystem.MizanSpacing
import com.example.core.designsystem.MizanTypography

enum class MizanPillStyle {
    Lime,
    SoftMint,
    Charcoal,
    Muted,
    Error
}

/**
 * Compact pill-shaped status indicator.
 */
@Composable
fun MizanStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    style: MizanPillStyle = MizanPillStyle.SoftMint,
    showDot: Boolean = true,
    testTag: String = "mizan_status_pill"
) {
    val (backgroundColor, textColor, dotColor, borderColor) = when (style) {
        MizanPillStyle.Lime -> Quad(MizanColors.Lime, MizanColors.Charcoal, MizanColors.Charcoal, null)
        MizanPillStyle.SoftMint -> Quad(MizanColors.SoftMint, MizanColors.Charcoal, MizanColors.FreshGreen, null)
        MizanPillStyle.Charcoal -> Quad(MizanColors.Charcoal, MizanColors.WarmWhite, MizanColors.Lime, null)
        MizanPillStyle.Muted -> Quad(MizanColors.Paper, MizanColors.MutedGray, MizanColors.MutedGray, MizanColors.Line)
        MizanPillStyle.Error -> Quad(MizanColors.ErrorSoft, MizanColors.Charcoal, Color(0xFFD32F2F), null)
    }

    val borderModifier = if (borderColor != null) {
        Modifier.border(1.dp, borderColor, MizanShapes.Pill)
    } else Modifier

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(MizanShapes.Pill)
            .background(backgroundColor)
            .then(borderModifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MizanTypography.Label,
            color = textColor
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
