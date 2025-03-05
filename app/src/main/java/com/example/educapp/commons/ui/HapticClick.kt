package com.example.educapp.commons.ui

import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp


fun Modifier.hapticClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current

    clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        indication = null, // or keep your default ripple if you like
        interactionSource = remember {MutableInteractionSource()}
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    // Provide a custom shape and color if you want
    shape: Shape = RoundedCornerShape(16.dp),
    // Optional: Control default/pressed elevation
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 6.dp,    // was 2.dp by default
        pressedElevation = 12.dp,   // was 4.dp by default
        hoveredElevation = 8.dp,
        focusedElevation = 8.dp,
        disabledElevation = 0.dp
    ),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ),
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            ambientColor = Color.Black,
            spotColor = Color.Black
        )
    ) {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            elevation = elevation,
            colors = colors,
            contentPadding = contentPadding,
        ) {
            content()
        }
    }

}
