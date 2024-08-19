package com.example.educapp.ui

import android.util.Log
import androidx.activity.result.launch
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel = viewModel<RegistrationViewModel>(
        factory = RegistrationViewModelFactory(context)
    )
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

                    }
                }) {
                    Text("Next")
                }
            }

            // Handle login success/failure and navigation
            LaunchedEffect(key1 = loginSuccess) {
                Log.d("LoginScreen", "LaunchedEffect triggered with loginSuccess: $loginSuccess")

                if (loginSuccess == true) {
                    viewModel.getUserRole { role -> // Call getUserRole with the callback
                        Log.d("LoginScreen", "User role: $role")

                        when (role) { // Use the role received in the callback
                            UserRole.TEACHER -> {
                                Log.d("LoginScreen", "Navigating to teacher_main")
                                navController.navigate("teacher")
                            }
                            UserRole.STUDENT -> {
                                Log.d("LoginScreen", "Navigating to student_main")
                                navController.navigate("student")
                            }
                            else -> {
                                Log.d("LoginScreen", "No role found, not navigating")
                            }
                        }
                    }
                } else if (loginSuccess == false) {
                    Log.d("LoginScreen", "Login failed")
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