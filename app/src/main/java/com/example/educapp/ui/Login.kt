package com.example.educapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel = viewModel<RegistrationViewModel>(
        factory = RegistrationViewModelFactory(context)
    ) // Assuming RegistrationViewModel has loginUser and getUserRole
    var usernameEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginSuccess by remember { mutableStateOf<Boolean?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var loginState by remember { mutableStateOf(LoginState.Input) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(visible = loginState == LoginState.Input) {
                Column {
                    TextField(
                        value = usernameEmail,
                        onValueChange = { usernameEmail = it },
                        label = { Text("Username or Email") }
                    )
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }

            AnimatedVisibility(visible = loginState == LoginState.LoggingIn) {
                Button(onClick = {
                    viewModel.loginUser(usernameEmail, password) { success ->
                        loginSuccess = success
                    }
                }) {
                    Text("Login")
                }
            }

            if (loginState == LoginState.Input) {
                Button(onClick = {
                    if (usernameEmail.isNotBlank() && password.isNotBlank()) {
                        loginState = LoginState.LoggingIn
                    } else {
                        // Handle validation error (e.g., show a Snackbar)
                    }
                }) {
                    Text("Next")
                }
            }

            // Handle login success/failure and navigation
            LaunchedEffect(key1 = loginSuccess) {
                if (loginSuccess == true) {
                    when (viewModel.getUserRole()) {
                        UserRole.TEACHER -> navController.navigate("teacher_main")
                        UserRole.STUDENT -> navController.navigate("student_main")
                        else -> {}
                    }
                } else if (loginSuccess == false) {
                    snackbarHostState.showSnackbar(
                        message = "Login failed. Please try again.",
                        duration = SnackbarDuration.Short
                    )
                }
            }

            // Snackbar
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

private enum class LoginState {
    Input,
    LoggingIn
}