package com.example.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Mizan Light Color Scheme based strictly on DESIGN.md tokens:
 * Dominant accent relationship: Charcoal + Lime + Warm White on Paper background.
 */
private val MizanLightColorScheme = lightColorScheme(
    primary = MizanColors.Charcoal,
    onPrimary = MizanColors.WarmWhite,
    primaryContainer = MizanColors.Lime,
    onPrimaryContainer = MizanColors.Charcoal,
    secondary = MizanColors.Lime,
    onSecondary = MizanColors.Charcoal,
    secondaryContainer = MizanColors.SoftMint,
    onSecondaryContainer = MizanColors.Charcoal,
    tertiary = MizanColors.FreshGreen,
    onTertiary = MizanColors.Charcoal,
    background = MizanColors.Paper,
    onBackground = MizanColors.Charcoal,
    surface = MizanColors.WarmWhite,
    onSurface = MizanColors.Charcoal,
    surfaceVariant = MizanColors.Paper,
    onSurfaceVariant = MizanColors.MutedGray,
    outline = MizanColors.Line,
    outlineVariant = MizanColors.Line,
    error = MizanColors.Charcoal,
    errorContainer = MizanColors.ErrorSoft,
    onError = MizanColors.Charcoal,
    onErrorContainer = MizanColors.Charcoal
)

@Composable
fun MizanTheme(
    forceRtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val layoutDirection = if (forceRtl) LayoutDirection.Rtl else LocalLayoutDirection.current

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = MizanLightColorScheme,
            typography = MizanTypography.MaterialTypography,
            content = content
        )
    }
}
