package com.example.educapp.ui

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.password
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.educapp.R
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.lifecycle.viewmodel.compose.viewModel

class SignInViewModel : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var signInStep by mutableStateOf(SignInStep.EnterEmail)
    var showError by mutableStateOf("")
    var deepLink by mutableStateOf<String?>(null)
    val auth = Firebase.auth
}

@Composable
fun SignInScreen(navController: NavController) {
    val viewModel = viewModel<SignInViewModel>()
    val context = LocalContext.current
    val activity = context as Activity

    LaunchedEffect(key1 = viewModel.deepLink) { // Observe deepLink in ViewModel
        viewModel.deepLink?.let { deepLink ->
            if (viewModel.auth.isSignInWithEmailLink(deepLink)) {
                val email = Uri.parse(deepLink).getQueryParameter("email") ?: ""
                viewModel.auth.signInWithEmailLink(email, deepLink)
                    .addOnCompleteListener(activity) { task ->
                        if (task.isSuccessful) {
                            viewModel.signInStep = SignInStep.SelectRole
                        } else {
                            viewModel.showError = "Error during sign-in"
                        }
                    }
                viewModel.deepLink = null // Reset deep link after handling
            }

        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            when (viewModel.signInStep) {
                SignInStep.EnterEmail -> {
                    TextField(
                        value = viewModel.email,
                        onValueChange = { viewModel.email = it },
                        label = { Text("Email") }
                    )
                    Button(onClick = {
                        // Validate email format
                        if (isValidEmail(viewModel.email)) {
                            viewModel.signInStep = SignInStep.EnterPassword
                        } else {
                            viewModel.showError = "Invalid email format"
                        }
                    }) {
                        Text("Next")
                    }
                }
                SignInStep.EnterPassword -> {
                    TextField(
                        value = viewModel.password,
                        onValueChange = { viewModel.password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    TextField(
                        value = viewModel.confirmPassword,
                        onValueChange = { viewModel.confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Button(onClick = {
                        // Validate password and confirmation
                        if (viewModel.password == viewModel.confirmPassword && isValidPassword(viewModel.password)) {
                            viewModel.signInStep = SignInStep.ConfirmEmail
                            sendConfirmationEmail(viewModel.auth, viewModel.email) {
                                viewModel.showError = it
                            }
                        } else {
                            viewModel.showError = "Passwords do not match or are invalid"
                        }
                    }) {
                        Text("Next")
                    }
                }
                SignInStep.ConfirmEmail -> {
                    Text("Check your email for a confirmation link")
                    // You might want to add a button to resend the email if needed
                }
                SignInStep.SelectRole -> {
                    RoleButton("Teacher", R.drawable.teacher_image) {
                        // Store role as "teacher" and navigate
                        // You might want to use a ViewModel or shared state to store the role
                        navController.navigate("home") // Or your next screen
                    }
                    Spacer(Modifier.height(16.dp))
                    RoleButton("Student", R.drawable.student_image) {
                        // Store role as "student" and navigate
                        navController.navigate("home") // Or your next screen
                    }
                }
            }
            if (viewModel.showError.isNotEmpty()) {
                Text("Invalid input", color = Color.Red)
            }
        }
    }
}

fun sendConfirmationEmail(auth: FirebaseAuth, email: String, showError: (String) -> Unit) {
    val actionCodeSettings = ActionCodeSettings.newBuilder()
        .setUrl("https://educapp.page.link/signin/confirm?email=$email") // Your Dynamic Link URL
        .setDynamicLinkDomain("educapp.page.link") // Your Dynamic Link domain
        .setHandleCodeInApp(true)
        .setAndroidPackageName("com.example.educapp", true, "12") // Your app details
        .build()

    auth.sendSignInLinkToEmail(email, actionCodeSettings)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Email sent successfully
                // You might want to show a success message to the user
            } else {
                val errorMessage = task.exception?.message ?: "Failed to send email"
                showError(errorMessage)
            }
            }
        }

// Add these enums outside the composable function
enum class SignInStep {
    EnterEmail,
    EnterPassword,
    ConfirmEmail,
    SelectRole
}

// Simple email validation (replace with more robust validation if needed)
fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

// Simple password validation (replace with more robust validation if needed)
fun isValidPassword(password: String): Boolean {
    return password.length >= 6
}

@Composable
fun RoleImage(imageResId: Int) {
    Image(
        painter = painterResource(id = imageResId),
        contentDescription = null, // Provide a description if needed
        modifier = Modifier.size(24.dp) // Adjust size as needed
    )
}

@Composable
fun RoleButton(role: String, imageResId: Int, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoleImage(imageResId)
            Spacer(Modifier.width(8.dp)) // Add space between image and label
            Text(role)
        }
    }
}