package com.example.educapp.commons.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


fun Modifier.hapticClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = null
    ) {
        // Trigger the haptic feedback
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        // Then call the original click
        onClick()
    }
}


@Composable
fun HapticButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
        .wrapContentSize()  // Add this
        .width(200.dp)
        .height(48.dp),
    contentPadding: PaddingValues = PaddingValues(  // Reduced padding
        horizontal = 16.dp,
        vertical = 8.dp
    ),
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    baseColor: Color = MaterialTheme.colorScheme.surface,
    lightShadow: Color = MaterialTheme.colorScheme.secondary,
    darkShadow: Color = MaterialTheme.colorScheme.surfaceTint,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    elevation: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) elevation / 2 else elevation,
        animationSpec = tween(200)
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = animatedElevation,
                shape = shape,
                ambientColor = lightShadow,
                spotColor = darkShadow,
                clip = false
            )
            .shadow(
                elevation = animatedElevation,
                shape = shape,
                ambientColor = darkShadow,
                spotColor = lightShadow,
                clip = false
            )
            .background(baseColor, shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
    ) {
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val contentAlpha by animateFloatAsState(
                targetValue = if (enabled) 1f else 0.2f,
                animationSpec = tween(100)
            )

            CompositionLocalProvider(
                LocalContentColor provides contentColor.copy(alpha = contentAlpha)
            ) {
                content()
            }
        }
    }
}

@Composable
fun HapticFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    HapticButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(
                minWidth = 56.dp,
                minHeight = 56.dp
            ),
        shape = RoundedCornerShape(16.dp),
        baseColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        elevation = 8.dp,
        content = content
    )
}