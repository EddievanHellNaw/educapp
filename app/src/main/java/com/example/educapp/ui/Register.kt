package com.example.educapp.ui

import android.content.Context
import android.util.Log
import androidx.activity.result.launch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.filter
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.educapp.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegistrationViewModel(private val context: Context) : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    var role by mutableStateOf<UserRole?>(null)

    fun registerUser(name: String, email: String, password: String, role: UserRole, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user
                if (user != null) {
                    val userRef = db.collection("users").document(user.uid)
                    userRef.set(hashMapOf("role" to role.name, "username" to name)).await() // Store username
                    onResult(true)
                } else {
                    onResult(false)
                }
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user?.updateProfile(profileUpdates)?.await()

                val userRef = db.collection("users").document(user!!.uid)
                userRef.set(hashMapOf("role" to role.name)).await()

                UserPreferencesRepository.saveRole(context, role)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
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
                    val authResult = auth.signInWithEmailAndPassword(email, password).await()
                    if (authResult != null) {
                        onResult(true)
                    } else {
                        onResult(false)
                    }
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    suspend fun getUserRole(): Any? {
        // 1. Try to get the role from SharedPreferences
        val savedRole = UserPreferencesRepository.getRole(context)
        if (savedRole != null) {
            return savedRole
        }

        // 2. If not found in SharedPreferences, try to get it from Firestore
        val user = auth.currentUser
        if (user != null) {
            return try {
                val document = db.collection("users").document(user.uid).get().await()
                if (document.exists()) {
                    val roleName = document.getString("role")
                    UserRole.valueOf(roleName!!) // Convert role name to UserRole enum
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        return null
    }
}

class RegistrationViewModelFactory(private val context: Context) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrationViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel = viewModel<RegistrationViewModel>(factory = RegistrationViewModelFactory(context))
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var registrationSuccess by remember { mutableStateOf<Boolean?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var registrationState by remember { mutableStateOf(RegistrationState.Input) }

    fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            return false // Check for empty fields
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false // Check for valid email format
        }
        if (password != confirmPassword) {
            return false // Check if passwords match
        }
        // Add more validation rules if needed (e.g., password length, special characters)
        return true
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(visible = registrationState == RegistrationState.Input) {
                Column {
                    TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }

            AnimatedVisibility(visible = registrationState == RegistrationState.RoleSelection) {
                Column {
                    Text("Please choose your role")
                    RoleSelectionScreen(viewModel)
                }
            }

            AnimatedVisibility(visible = registrationState == RegistrationState.Registering) {
                Button(onClick = {
                    viewModel.registerUser(name, email, password, viewModel.role!!) { success ->
                        registrationSuccess = success
                    }
                }) {
                    Text("Register")
                }
            }

            if (registrationState == RegistrationState.Input) {
                Button(onClick = {
                    if (validateInput(name, email, password, confirmPassword)) {
                        registrationState = RegistrationState.RoleSelection
                    } else {
                        // Handle validation error (e.g., show a Snackbar)
                    }
                }) {
                    Text("Next")
                }
            }

            if (registrationState == RegistrationState.RoleSelection && viewModel.role != null) {
                registrationState = RegistrationState.Registering
            }

            // Handle registration success/failure and navigation
            LaunchedEffect(key1 = registrationSuccess) {
                if (registrationSuccess == true) {
                    when (viewModel.role) {
                        UserRole.TEACHER -> navController.navigate("teacher_main")
                        UserRole.STUDENT -> navController.navigate("student_main")
                        else -> {}
                    }
                } else if (registrationSuccess == false) {
                    snackbarHostState.showSnackbar(
                        message = "Registration failed. Please try again.",
                        duration = SnackbarDuration.Short
                    )
                }
            }

            // Snackbar
            SnackbarHost(hostState = snackbarHostState)
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
    Button(onClick = {
        Log.d("RoleSelection", "Role button clicked: ${role.name}")
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
fun RoleSelectionScreen(viewModel: RegistrationViewModel) {
    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RoleButton(UserRole.TEACHER, R.drawable.teacher_image) { viewModel.role = UserRole.TEACHER }
        Spacer(modifier = Modifier.height(16.dp))
        RoleButton(UserRole.STUDENT, R.drawable.student_image) { viewModel.role = UserRole.STUDENT }
    }
}
