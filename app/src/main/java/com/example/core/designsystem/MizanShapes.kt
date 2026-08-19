package com.example.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Mizan Shapes specifications based on DESIGN.md
 *
 * 28dp radius for hero panels
 * 24dp for major cards
 * 18dp for text fields and buttons
 * Full circle / 50% for pill status chips
 */
object MizanShapes {
    val HeroPanel = RoundedCornerShape(32.dp)
    val MajorCard = RoundedCornerShape(24.dp)
    val InputAndButton = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(50)
}
