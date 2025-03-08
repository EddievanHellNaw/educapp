package com.example.educapp.commons

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.educapp.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.educapp.commons.ui.FrostedGlassTextField
import com.example.educapp.commons.ui.HapticButton
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import javax.inject.Inject

class RegistrationViewModel @Inject constructor(private val context: Context) : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    var role by mutableStateOf<UserRole?>(null)


    sealed class RegistrationError(
        override val message: String
    ) : Exception(message) {
        object EmailVerificationFailed : RegistrationError("Failed to send verification email")
        object InvalidEmail : RegistrationError("Invalid email format")
        object EmailExists : RegistrationError("Email already registered")
        object WeakPassword : RegistrationError("Password must be at least 8 characters")
        object PasswordMismatch : RegistrationError("Passwords don't match")
        object InvalidName : RegistrationError("Invalid username format")
        object NetworkError : RegistrationError("Network error - check your connection")
        object UnknownError : RegistrationError("Registration failed - please try again")
    }

    var isLoading by mutableStateOf(false)

    fun registerUser(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        onResult: (RegistrationError?) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            try {
                // Create Firebase auth user
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: throw RegistrationError.UnknownError

                // Update user profile with display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()

                // Prepare Firestore data with conditional teacherId
                val userData = hashMapOf(
                    "role" to role.name,
                    "username" to name.trim(),
                    "email" to email.lowercase()
                ).apply {
                    if (role == UserRole.TEACHER) {
                        put("teacherId", user.uid)
                    }
                }

                // Save to Firestore with rollback protection
                try {
                    db.collection("users").document(user.uid).set(userData).await()
                } catch (e: Exception) {
                    user.delete().await()  // Rollback auth creation
                    throw e  // Re-throw to outer catch
                }

                // Send verification email
                try {
                    user.sendEmailVerification().await()
                    UserPreferencesRepository.saveRole(context, role)
                    onResult(null)  // Success
                } catch (e: Exception) {
                    throw RegistrationError.EmailVerificationFailed
                }

            } catch (e: Exception) {
                val error = when (e) {
                    is FirebaseAuthUserCollisionException -> RegistrationError.EmailExists
                    is FirebaseAuthWeakPasswordException -> RegistrationError.WeakPassword
                    is FirebaseNetworkException -> RegistrationError.NetworkError
                    is RegistrationError -> e  // Preserve our custom errors
                    else -> when (e.message) {
                        "The email address is badly formatted." -> RegistrationError.InvalidEmail
                        else -> RegistrationError.UnknownError
                    }
                }
                onResult(error)
            } finally {
                isLoading = false
            }
        }
    }

    fun loginUser(usernameEmail: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val email = if (usernameEmail.contains("@")) {
                    usernameEmail
                } else {
                    val userDoc = db.collection("users").whereEqualTo("username", usernameEmail).get().await().firstOrNull()
                    userDoc?.getString("email")
                }

                if (email != null) {
                    auth.signInWithEmailAndPassword(email, password).await()
                    val user = Firebase.auth.currentUser
                    if (user?.isEmailVerified == true) {
                        // Email is verified
                        onResult(true)
                    } else {
                        // Email is not verified, show a message or block login
                        Log.w("RegistrationViewModel", "User tried to log in with unverified email")
                        onResult(false)
                    }
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
                Log.e("RegistrationViewModel", "Login failed: ${e.message}") // Log the error message
            }
        }
    }

    fun getUserRole(onRoleRetrieved: (UserRole?) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val roleName = document.getString("role")
                        val role = UserRole.valueOf(roleName!!)
                        onRoleRetrieved(role)
                    } else {
                        onRoleRetrieved(null)
                    }
                }
                .addOnFailureListener { e ->
                    onRoleRetrieved(null)
                }
        } else {
            onRoleRetrieved(null)
        }
    }

    fun validateInput(
    name: String,
    email: String,
    password: String,
    confirmPassword: String
    ): RegistrationError? {
        return when {
            name.isBlank() -> RegistrationError.InvalidName
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> RegistrationError.InvalidEmail
            password != confirmPassword -> RegistrationError.PasswordMismatch
            password.length < 8 -> RegistrationError.WeakPassword
            else -> null
        }
    }
}


@Composable
fun RegistrationScreen(navController: NavController) {
    val viewModel: RegistrationViewModel = koinViewModel()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var registrationError by remember { mutableStateOf<RegistrationViewModel.RegistrationError?>(null) }
    var registrationSuccess by remember { ( mutableStateOf(false) ) }
    val snackbarHostState = remember { SnackbarHostState() }
    var registrationState by remember { mutableStateOf(RegistrationState.Input) }



    LaunchedEffect(registrationError) {
        registrationError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error.message,
                duration = SnackbarDuration.Short
            )
            registrationError = null // Reset error after showing
        }
    }

    Scaffold (snackbarHost = { SnackbarHost(snackbarHostState) })
    { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(visible = registrationState == RegistrationState.Input) {
                Column {
                    FrostedGlassTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Name"
                    )
                    FrostedGlassTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email"
                    )
                    FrostedGlassTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        visualTransformation = PasswordVisualTransformation()
                    )
                    FrostedGlassTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm Password",
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }

            AnimatedVisibility(visible = registrationState == RegistrationState.RoleSelection) {
                RoleSelectionScreen(
                    viewModel = viewModel,
                    onRoleSelected = {
                        Log.d("Registration", "Role selected, moving to registering state")
                        registrationState = RegistrationState.Registering
                    }
                )
            }

            AnimatedVisibility(visible = registrationState == RegistrationState.Registering) {
                HapticButton(
                    onClick = {
                        viewModel.registerUser(
                            name, email, password, viewModel.role!!
                        ) { error ->
                            if (error == null) {
                                navController.navigate("verify_email")
                            } else {
                                registrationError = error
                            }
                        }
                    }, modifier = Modifier
                        .width(200.dp)
                        .height(48.dp),
                    enabled = viewModel.role != null && !viewModel.isLoading
                ) {
                    Text(if (viewModel.isLoading) "Creating Account..." else "Complete Registration")
                }
            }
            val keyboardController = LocalSoftwareKeyboardController.current
            if (registrationState == RegistrationState.Input) {
                HapticButton(onClick = {
                    keyboardController?.hide()
                    val error = viewModel.validateInput(name, email, password, confirmPassword)
                    Log.d("Registration", "Validation error: $error")
                    if (error != null) {
                        Log.d("Registration", "Showing error: ${error.message}")
                        registrationError = error
                    } else {
                        Log.d("Registration", "Moving to role selection")
                        registrationState = RegistrationState.RoleSelection
                    }
                }, modifier = Modifier
                    .width(200.dp)
                    .height(48.dp)) {
                    Text("Next")
                }
            }
            LaunchedEffect(registrationSuccess) {
                if (registrationSuccess) {
                    navController.navigate("verify_email")
                    registrationSuccess = false
                }
            }

            if (registrationSuccess) {
            navController.navigate("verify_email")
            registrationSuccess = false
        }
            // Handle registration success/failure and navigation
            LaunchedEffect(registrationError, registrationSuccess) {
                if (registrationSuccess) {
                    when (viewModel.role) {
                        UserRole.TEACHER -> navController.navigate("teacher_main")
                        UserRole.STUDENT -> navController.navigate("student_main")
                        null -> snackbarHostState.showSnackbar("Role not selected")
                    }
                }
                registrationError?.let { error ->
                    snackbarHostState.showSnackbar(
                        message = error.message.toString(), // Now using your custom property
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    registrationError = null
                }
            }
        }
    }
}

private enum class RegistrationState {
    Input,
    RoleSelection,
    Registering
}
@Composable
fun RoleButton(role: UserRole, imageResId: Int, onRoleSelect: () -> Unit) {
    HapticButton(onClick = {
        Log.d("RoleSelection", "Role HapticButton clicked: ${role.name}")
        onRoleSelect()})
    {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = "Role Image",
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(role.name)
        }
    }
}

@Composable
fun RoleSelectionScreen(
    viewModel: RegistrationViewModel,
    onRoleSelected: () -> Unit
    ) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Choose Your Role", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))

        RoleButton(UserRole.TEACHER, R.drawable.teacher_image) {
            viewModel.role = UserRole.TEACHER
        }
        Spacer(modifier = Modifier.height(16.dp))
        RoleButton(UserRole.STUDENT, R.drawable.student_image) {
            viewModel.role = UserRole.STUDENT
        }
        Spacer(modifier = Modifier.height(32.dp))

        // Add confirmation button
        HapticButton(
            onClick = onRoleSelected,
            modifier = Modifier
                .width(200.dp)
                .height(48.dp),
            enabled = viewModel.role != null
        ) {
            Text("Confirm Role")
        }
    }
}

@Composable
fun EmailVerificationScreen(navController: NavController) {
    val viewModel: RegistrationViewModel = koinViewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    var checking by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var resendVerificationEvent by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        // Check verification status on first load
        checkVerification(context, navController)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Please verify your email address", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            HapticButton(
                onClick = { checking = true },
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
                enabled = !checking
            ) {
                Text(if (checking) "Checking..." else "I've verified my email")
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = {
                    Firebase.auth.currentUser?.sendEmailVerification()
                    resendVerificationEvent = true
                }
            ) {
                Text("Resend verification email")
            }

            LaunchedEffect(resendVerificationEvent) {
                if (resendVerificationEvent) {
                    snackbarHostState.showSnackbar("Verification email resent!")
                    resendVerificationEvent = false
                }
            }
        }
    }

    LaunchedEffect(checking) {
        if (checking) {
            checkVerification(context, navController)
            checking = false
        }
    }
}

private suspend fun checkVerification(
    context: Context,  // Pass context from composable
    navController: NavController
) {
    val user = Firebase.auth.currentUser
    user?.reload()

    if (user?.isEmailVerified == true) {
        // Get role from preferences using passed context
        val role = UserPreferencesRepository.getUnverifiedRole(context)

        // Navigate on the main thread
        withContext(Dispatchers.Main) {
            when (role) {
                UserRole.TEACHER -> navController.navigate("teacher/main") {
                    popUpTo("verify_email") { inclusive = true }
                }
                UserRole.STUDENT -> navController.navigate("student_main") {
                    popUpTo("verify_email") { inclusive = true }
                }
                null -> navController.popBackStack()
            }
        }
    }
}