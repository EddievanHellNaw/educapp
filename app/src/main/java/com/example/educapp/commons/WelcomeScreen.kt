package com.example.educapp.commons.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.educapp.R
import com.example.educapp.commons.ui.HapticButton
import kotlinx.coroutines.delay


@Composable
fun WelcomeScreen(navController: NavController) {
    // State machine for the circle animation
    var animationState by remember { mutableStateOf(AnimationState.Showing) }

    // Animate the circle offset upwards
    val offsetY by animateFloatAsState(
        targetValue = if (animationState == AnimationState.MovingUp) -100f else 0f,
        animationSpec = when (animationState) {
            AnimationState.MovingUp -> spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
            else -> tween(durationMillis = 500)
        }
    )

    // Animate the circle alpha (you can keep it at 1f if you prefer)
    val circleAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000)
    )

    // Separate boolean to trigger the text fade-in after the circle finishes
    var textVisible by remember { mutableStateOf(false) }
    // Animate text alpha
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    // Launch side effects
    LaunchedEffect(animationState) {
        when (animationState) {
            AnimationState.Showing -> {
                // Wait a bit, then move the circle up
                delay(750)
                animationState = AnimationState.MovingUp
            }
            AnimationState.MovingUp -> {
                // Once the circle is moving, wait a bit more, then fade in text
                delay(600)
                textVisible = true
            }
        }
    }

    // Use a Surface for the background
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 1) The animated circle with the logo
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = offsetY.dp)
                    .alpha(circleAlpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.thinking_cap),
                    contentDescription = "App Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2) The welcome text, fades in after the circle animation
            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .alpha(textAlpha)
                    .clip(RoundedCornerShape(50.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Welcome to Educapp",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 3) The buttons, pinned near the bottom center
            if (animationState == AnimationState.MovingUp) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 250.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Use a shared modifier to make both buttons the same size
                    val buttonModifier = Modifier
                        .width(200.dp)
                        .height(48.dp)

                    HapticButton(
                        onClick = { navController.navigate("registration") },
                        modifier = buttonModifier
                    ) {
                        Text("Register")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HapticButton(
                        onClick = { navController.navigate("login") },
                        modifier = buttonModifier
                    ) {
                        Text("Login")
                    }
                }
            }
        }
    }
}

private enum class AnimationState {
    Showing,
    MovingUp
}
