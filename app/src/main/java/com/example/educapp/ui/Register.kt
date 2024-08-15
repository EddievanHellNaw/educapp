package com.example.educapp.ui

import android.content.Context
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.launch

class RegistrationViewModel(private val context: Context) : ViewModel() {
    private val auth = Firebase.auth
    var role by mutableStateOf<UserRole?>(null)

    fun registerUser(name: String, email: String, password: String, role: UserRole, onResult: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update user profile with name
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user?.updateProfile(profileUpdates)
                    viewModelScope.launch {
                        UserPreferencesRepository.saveRole(context, role)
                    }
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
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

@Composable
fun RegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel = viewModel<RegistrationViewModel>(factory = RegistrationViewModelFactory(context))
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showRoleSelection by remember { mutableStateOf(false) }

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
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
        Button(onClick = {
            if (validateInput(name, email, password, confirmPassword)) {
                showRoleSelection = true
            } else {
                // Handle validation error
            }
        }) {
            Text("Next")
        }

        if (showRoleSelection) {
            RoleSelectionScreen(viewModel)

            // Show the Register button only if a role is selected
            if (viewModel.role != null) {
                Button(onClick = {
                    viewModel.registerUser(name, email, password, viewModel.role!!) { success ->
                        if (success) {
                            // Navigate to the appropriate main screen based on the selected role
                            when (viewModel.role) {
                                UserRole.TEACHER -> navController.navigate("teacher_main")
                                UserRole.STUDENT -> navController.navigate("student_main")
                                else -> {} // Should not happen, but handle for safety
                            }
                        } else {
                            // Handle registration error
                        }
                    }
                }) {
                    Text("Register")
                }
            }
        }
    }
}

@Composable
fun RoleButton(role: UserRole, imageResId: Int, onRoleSelect: () -> Unit) {
    Button(onClick = onRoleSelect) {
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
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RoleButton(UserRole.TEACHER, R.drawable.teacher_image) { viewModel.role = UserRole.TEACHER }
        Spacer(modifier = Modifier.height(16.dp))
        RoleButton(UserRole.STUDENT, R.drawable.student_image) { viewModel.role = UserRole.STUDENT }
    }
}
