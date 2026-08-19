package com.example.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanShapes
import com.example.core.designsystem.MizanSpacing
import com.example.core.designsystem.MizanTypography

/**
 * Primary action button for Mizan.
 * Solid Charcoal background with WarmWhite text/accent and 18dp rounded corners.
 */
@Composable
fun MizanPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String = "mizan_primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = MizanShapes.InputAndButton,
        colors = ButtonDefaults.buttonColors(
            containerColor = MizanColors.Charcoal,
            contentColor = MizanColors.WarmWhite,
            disabledContainerColor = MizanColors.Line,
            disabledContentColor = MizanColors.MutedGray
        ),
        contentPadding = PaddingValues(
            horizontal = MizanSpacing.ScreenHorizontalPadding,
            vertical = 14.dp
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .testTag(testTag)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MizanColors.WarmWhite,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(MizanSpacing.LabelToControlSpacing))
                }
                Text(
                    text = text,
                    style = MizanTypography.Button,
                    color = if (enabled) MizanColors.WarmWhite else MizanColors.MutedGray
                )
            }
        }
    }
}

/**
 * Outline action button for Mizan.
 * Transparent surface with Line/Charcoal border and Charcoal text.
 */
@Composable
fun MizanOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = MizanColors.Line,
    contentColor: Color = MizanColors.Charcoal,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String = "mizan_outline_button"
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MizanShapes.InputAndButton,
        border = BorderStroke(1.5.dp, if (enabled) borderColor else MizanColors.Line),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = MizanColors.MutedGray
        ),
        contentPadding = PaddingValues(
            horizontal = MizanSpacing.ScreenHorizontalPadding,
            vertical = 14.dp
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(MizanSpacing.LabelToControlSpacing))
            }
            Text(
                text = text,
                style = MizanTypography.Button,
                color = if (enabled) contentColor else MizanColors.MutedGray
            )
        }
    }
}
