package com.example.educapp.ui.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.educapp.R
import kotlinx.coroutines.delay


@Composable
fun WelcomeScreen(navController: NavController) {
    var animationState by remember { mutableStateOf(AnimationState.Showing) }
    val offsetY by animateFloatAsState(
        targetValue = when (animationState) {
            AnimationState.Showing -> 0f
            AnimationState.MovingUp -> -100f
        },
        animationSpec = when (animationState) {
            AnimationState.MovingUp -> spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
            else -> tween(durationMillis = 500)
        }
    )
    val alpha by animateFloatAsState(
        targetValue = if (animationState == AnimationState.Showing) 1f else 1f,
        animationSpec = tween(durationMillis = 1000) // Gradual appearance
    )

    LaunchedEffect(key1 = animationState) {
        when (animationState) {
            AnimationState.Showing -> {
                delay(750)
                animationState = AnimationState.MovingUp
            }
            AnimationState.MovingUp -> {
                // No delay needed here
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (animationState == AnimationState.MovingUp) 16.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.thinking_cap),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(200.dp)
                .offset(y = offsetY.dp)
                .alpha(alpha) // Apply alpha for gradual appearance
        )

        Text(
            text = "Welcome to Educapp",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(16.dp)
                .offset(y = offsetY.dp)
                .alpha(alpha) // Apply alpha for gradual appearance
        )

        if (animationState == AnimationState.MovingUp) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("registration") }) {
                Text("Register")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("login") }) {
                Text("Login")
            }
        }
    }
}

private enum class AnimationState {
    Showing,
    MovingUp
}