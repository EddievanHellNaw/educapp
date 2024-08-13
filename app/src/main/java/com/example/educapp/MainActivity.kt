package com.example.educapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen()

        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
        MainScreen()

}

@Composable
fun MainScreen() {
    var showLoginForm by remember { mutableStateOf(false) }
    var animationFinished by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        delay(1000)
        animationFinished = true
        delay(500)
        showLoginForm = true
    }

    Scaffold { paddingValues -> // Add Scaffold for structure
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Apply padding from Scaffold
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EduLogo(
                modifier = Modifier
                    .size(120.dp)
                    .alpha(animateFloatAsState(
                        targetValue = if (animationFinished) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ).value)
                    .scale(animateFloatAsState(
                        targetValue = if (animationFinished) 1f else 0.5f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ).value)
            )

            if (showLoginForm) {
                LoginForm(modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun EduLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.thinking_cap),
        contentDescription = "Educapp Logo",
        modifier = modifier
    )
}

@Composable
fun LoginForm(modifier: Modifier = Modifier) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            /*TODO*/
        }) {
            Text("Login")
        }
    }
}
