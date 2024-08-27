package com.example.educapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.educapp.ui.auth.WelcomeScreen
import com.example.educapp.ui.student.StudentMainScreen
import com.example.educapp.ui.teacher.attendance.AttendanceScreen
import com.example.educapp.ui.teacher.TeacherMainScreen
import com.example.educapp.ui.teacher.attendance.AttendanceViewModel
import com.example.educapp.ui.teacher.attendance.CheckScreen
import com.example.educapp.ui.teacher.attendance.TakeAttendanceDetailsScreen
import com.example.educapp.ui.teacher.attendance.TakeAttendanceScreen
import com.example.educapp.ui.teacher.planner.MainPlannerScreen
import com.example.educapp.ui.teacher.planner.NewPlanScreen
import com.example.educapp.ui.teacher.planner.PlannerViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_VIEW) {
            val user = Firebase.auth.currentUser
            user?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (user.isEmailVerified) {
                        // Email is verified, navigate to the appropriate screen based on user role
                        Log.d("MainActivity", "Email verified!")
                        // You'll need to add logic here to determine the user's role and navigate accordingly
                    } else {
                        // Email is not verified, show a message
                        Log.d("MainActivity", "Email not verified")
                        // Show a Snackbar or a dialog informing the user that the email is not verified
                    }
                } else {
                    // Handle error
                    Log.d("MainActivity", "Error reloading user: ${task.exception}")
                    // Show an error message to the user
                }
            }
        }
    }

}






@Composable
fun MainScreen() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") { WelcomeScreen(navController) }
        composable("registration") { RegistrationScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("student_main") { StudentMainScreen(navController) }
        navigation(startDestination = "teacher/main", route = "teacher") {
            composable("teacher/main") { TeacherMainScreen(navController) }
            composable("teacher/attendance") {
                val viewModel = AttendanceViewModel()
                AttendanceScreen(viewModel, navController)
            }
            composable(
                route = "teacher/take_attendance/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                val viewModel: AttendanceViewModel = viewModel()
                TakeAttendanceScreen(viewModel, groupId, navController)
            }
            composable(
                route = "attendance/{groupId}/{partial}",
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("partial") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                val partial = backStackEntry.arguments?.getInt("partial") ?: 1
                val viewModel: AttendanceViewModel = viewModel()
                TakeAttendanceDetailsScreen(viewModel, groupId, partial, navController)
            }
            composable(
                route = "check/{groupId}/{partial}",
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("partial") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                val partial = backStackEntry.arguments?.getInt("partial") ?: 1
                val viewModel: AttendanceViewModel = viewModel()
                CheckScreen(viewModel, groupId, partial)
            }

            composable("teacher/planner") { val viewModel = hiltViewModel<PlannerViewModel>()
                MainPlannerScreen(navController, viewModel) }
            composable ("teacher/newPlan") { val viewModel = hiltViewModel<PlannerViewModel>()
                NewPlanScreen(navController, viewModel)}       }
    }
}