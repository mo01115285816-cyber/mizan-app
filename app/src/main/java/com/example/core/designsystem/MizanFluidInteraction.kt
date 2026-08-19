package com.example.core.designsystem

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Apple Fluid Design System physics parameters translated for Jetpack Compose:
 * - Critically Damped (Damping = 1.0f): Smooth settle, zero overshoot for everyday UI.
 * - Low Bouncy (Damping ~ 0.8f): Reserved for momentum and release flick gestures.
 */
object MizanSpringSpecs {
    val CriticallyDampedFloat = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val MomentumSpringFloat = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val SnapSpringFloat = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
}

/**
 * Fluid Press Interaction Modifier (Apple Design Rule 1 & 4):
 * - Responds instantly on pointer-down (scale 0.965f) with zero tap latency.
 * - Settle back to 1.0f on release with critically-damped spring physics.
 * - Triggers subtle tactical haptic feedback on touch contact.
 */
fun Modifier.fluidPressEffect(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    this
        .scale(scale.value)
        .pointerInput(enabled) {
            detectTapGestures(
                onPress = {
                    performTickHaptic(view)
                    scope.launch {
                        scale.animateTo(
                            targetValue = pressedScale,
                            animationSpec = MizanSpringSpecs.CriticallyDampedFloat
                        )
                    }
                    val released = tryAwaitRelease()
                    scope.launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = MizanSpringSpecs.CriticallyDampedFloat
                        )
                    }
                    if (released && onClick != null) {
                        onClick()
                    }
                },
                onLongPress = {
                    performImpactHaptic(view)
                    onLongClick?.invoke()
                }
            )
        }
}

/**
 * Performs immediate physical haptic feedback.
 */
fun performTickHaptic(view: View) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    } catch (_: Exception) {}
}

fun performImpactHaptic(view: View) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    } catch (_: Exception) {}
}

/**
 * Translucent Glass Surface (Apple Design Rule 12):
 * - Semi-transparent background with layered depth.
 * - Subtle 1px luminous edge border capturing light.
 */
@Composable
fun MizanGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = MizanColors.WarmWhite.copy(alpha = 0.90f),
    borderColor: Color = MizanColors.Line.copy(alpha = 0.80f),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 3.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(shape)
            .border(borderWidth, borderColor, shape),
        shape = shape,
        color = backgroundColor,
        shadowElevation = elevation,
        tonalElevation = 1.dp
    ) {
        Box(content = content)
    }
}
