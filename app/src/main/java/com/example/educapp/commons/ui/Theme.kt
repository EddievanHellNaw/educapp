package com.example.educapp.commons.ui

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.content.Context
import android.widget.CalendarView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


// Dark theme color scheme
val GhostColorScheme = darkColorScheme(
    primary = DeepPurple,        // main brand color in dark theme
    onPrimary = PureWhite,
    secondary = Lilac,
    onSecondary = OffBlack,
    background = DarkGray,
    onBackground = PureWhite,
    surface = MediumGray,
    onSurface = PureWhite,
    onError = warning,
    error = error,
    surfaceVariant = Color(0xFF404040),  // Slightly lighter than surface
    surfaceTint = DeepPurple.copy(alpha = 0.08f)  // Subtle primary overlay
    // Optionally define tertiary, error, onError, etc.
)

// Light theme color scheme
val CinnamoSpiralColorScheme = lightColorScheme(
    primary = hardPink,
    onPrimary = OffBlack,
    secondary = softBlue,
    onSecondary = DarkGray,
    background = MidBlue,
    onBackground = OffBlack,
    surface = BabyBlue,
    onSurface = OffBlack,
    tertiary = softPink,
    onTertiary = DarkGray,
    error = error,
    onError = warning,
    surfaceVariant = OffBlack,  // 15% darker than BabyBlue
    surfaceTint = hardPink.copy(alpha = 0.2f)
)



val ItalianPlumberColorScheme = darkColorScheme(
    primary = MarioRed,            // MarioRed = Color(0xFFEE1C25)
    onPrimary = Color.White,
    secondary = MarioBlue,         // MarioBlue = Color(0xFF0055B3)
    onSecondary = Color.White,
    background = Color(0xFF0B0B3B),  // A deep, rich blue as background
    onBackground = Color.White,
    surface = Color(0xFF2C2C5C),     // A cooler mid-tone blue for surfaces
    onSurface = Color.White,
    error = error,                 // your existing error color
    onError = warning              // your existing warning color
)

val ItalianHunterColorScheme = darkColorScheme(
    primary = LuigiGreen,         // LuigiGreen = Color(0xFF029502)
    onPrimary = Color.White,
    secondary = LuigiNavy,        // LuigiNavy  = Color(0xFF00159B)
    onSecondary = Color.White,
    background = Color(0xFF0B0B3B), // A deep forest green for a rich dark backdrop
    onBackground = Color.White,
    surface = LuigiNavy,    // A vibrant forest green for surfaces
    onSurface = Color.White,
    error = error,
    onError = warning
)

val ValiantAutonomyColorScheme = darkColorScheme(
    primary = Color(0xFFFFD700),   // A rich gold (e.g. “goldenrod” or true gold)
    onPrimary = Color.Black,       // For legibility on gold
    secondary = Color(0xFF002060),  // A deep, regal blue
    onSecondary = Color.White,
    background = Color(0xFF002569), // Dark background
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),    // A slightly lighter dark for cards and surfaces
    onSurface = Color.White,
    error = error,                 // Use your predefined error color
    onError = warning              // And warning color for onError
)

// 1. Define your theme names as an enum:
enum class AppTheme {
    Ghost,
    CinnamoSpiral,
    ItalianPlumber,
    ItalianHunter,
    ValiantAutonomy
}


fun Context.findActivity(): ComponentActivity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

val LocalTimerDuration = staticCompositionLocalOf { 60 }

@Composable
fun MyAppTheme(
    theme: AppTheme = AppTheme.Ghost,
    timerDuration: Int, // Default theme can be changed
    content: @Composable () -> Unit
) {
    // Cache the selected color scheme using remember to avoid re-calculation on every recomposition.
    val colorScheme = remember(theme) {
        when (theme) {
            AppTheme.Ghost -> GhostColorScheme
            AppTheme.CinnamoSpiral -> CinnamoSpiralColorScheme
            AppTheme.ItalianPlumber -> ItalianPlumberColorScheme
            AppTheme.ItalianHunter -> ItalianHunterColorScheme
            AppTheme.ValiantAutonomy -> ValiantAutonomyColorScheme
        }
    }

    // Optionally animate the background color change for smoother transitions
    val animatedBackground = animateColorAsState(targetValue = colorScheme.background)

    // Update status bar color with side effects
    val view = LocalView.current
    SideEffect {
        val activity = view.context.findActivity()
        activity?.window?.statusBarColor = animatedBackground.value.toArgb()
        WindowCompat.getInsetsController(activity?.window!!, view)
            ?.isAppearanceLightStatusBars = (theme == AppTheme.CinnamoSpiral)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CustomTypography,
        shapes = AppShapes,
        content = content
    )
}


@Composable
fun GradientCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    gradientBrush: Brush = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primary
        )
    ),
    shape: Shape = MaterialTheme.shapes.medium,
    defaultElevation: Dp = 8.dp,
    pressedElevation: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentElevation by animateDpAsState(
        targetValue = if (isPressed) pressedElevation else defaultElevation,
        animationSpec = tween(200)
    )

    Card(
        modifier = modifier
            .shadow(
                elevation = currentElevation,
                shape = shape,
                ambientColor = MaterialTheme.colorScheme.outlineVariant,
                spotColor = MaterialTheme.colorScheme.outline
            )
            .then(
                if (onClick != null) {
                    Modifier.hapticClickable(
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        elevation = CardDefaults.cardElevation(0.dp) // Disable default elevation
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun FrostedGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    placeholder: @Composable () -> Unit = {},
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val animatedElevation by animateDpAsState(
        targetValue = if (isFocused) 12.dp else 6.dp,
        animationSpec = tween(100)
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = animatedElevation,
                shape = RoundedCornerShape(16.dp),
                ambientColor = MaterialTheme.colorScheme.onSurface,
                spotColor = MaterialTheme.colorScheme.surfaceTint
            )
            .shadow(
                elevation = animatedElevation,
                shape = RoundedCornerShape(16.dp),
                ambientColor = MaterialTheme.colorScheme.surfaceTint,
                spotColor = MaterialTheme.colorScheme.secondary
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            visualTransformation = visualTransformation,
            placeholder = {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    placeholder()
                }
            },
            trailingIcon = trailingIcon,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            keyboardOptions = keyboardOptions,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
    }
}

@Composable
fun GlowingOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val isFocused = remember { mutableStateOf(false) }
    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isFocused.value) 0.6f else 0.3f)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused.value = it.isFocused }
            .border(2.dp, glowColor, RoundedCornerShape(8.dp)),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun CustomCalendarView(selectedDate: LocalDate, onDateChange: (LocalDate) -> Unit) {
    AndroidView(
        factory = { context ->
            CalendarView(context).apply {
                setOnDateChangeListener { _, year, month, dayOfMonth ->
                    onDateChange(LocalDate.of(year, month + 1, dayOfMonth))
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientDatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            HapticButton (onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                }
                onDismiss()
            }) {
                Text("Confirm")
            }
        },
        colors = DatePickerDefaults.colors(containerColor = Color(0xFF3E1C96))
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun FrostedBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
    ) {
        content()
    }
}
