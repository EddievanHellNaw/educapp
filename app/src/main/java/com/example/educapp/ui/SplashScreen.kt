package com.example.educapp.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    var animationFinished by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        delay(1000)
        animationFinished = true
        delay(500)
        // Trigger navigation here based on user interaction or conditions
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EduLogo(
                modifier = Modifier
                    .size(120.dp)
                    .alpha(
                        animateFloatAsState(
                            targetValue = if (animationFinished) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ).value
                    )
                    .scale(
                        animateFloatAsState(
                            targetValue = if (animationFinished) 1f else 0.5f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ).value
                    )
            )

            // Buttons for Sign In and Login
            if (animationFinished) {
                Column {
                    Button(onClick = { navController.navigate("signin") }) {
                        Text("Sign In")
                    }
                    Button(onClick = { navController.navigate("login") }) {
                        Text("Login")
                    }
                }
            }
        }
    }
}