package com.example.animations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressBounce(
    enabled: Boolean = true,
    scaleOnPressed: Float = 0.88f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Out-of-phase visco-elastic squish and stretch
    val targetScaleX = if (isPressed && enabled) 1.15f else 1.0f
    val targetScaleY = if (isPressed && enabled) 0.85f else 1.0f
    val targetRotation = if (isPressed && enabled) -4.0f else 0.0f

    // Highly responsive organic underdamped springs simulating high surface-tension jelly
    val scaleX by animateFloatAsState(
        targetValue = targetScaleX,
        animationSpec = spring(
            dampingRatio = 0.42f, // Underdamped wiggle
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press_liquid_x"
    )

    val scaleY by animateFloatAsState(
        targetValue = targetScaleY,
        animationSpec = spring(
            dampingRatio = 0.38f, // Out-of-phase oscillation for squishy feel
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_liquid_y"
    )

    val rotationZ by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "press_liquid_rot"
    )

    return this
        .graphicsLayer {
            this.scaleX = scaleX
            this.scaleY = scaleY
            this.rotationZ = rotationZ
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null, // Disable default gray box ripple; allow custom visual layouts
            onLongClick = onLongClick,
            onClick = onClick
        )
}

@Composable
fun hoverScaleAnimation(
    targetState: Boolean,
    speedMultiplier: Float = 1.0f
): Float {
    val duration = (250 / speedMultiplier).toInt()
    val scale by animateFloatAsState(
        targetValue = if (targetState) 1.0f else 0.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale_animation"
    )
    return scale
}
