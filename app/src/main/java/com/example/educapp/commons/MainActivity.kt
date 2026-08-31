package com.example.educapp.commons

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.educapp.commons.annotation.AnnotationMainScreen
import com.example.educapp.commons.annotation.BookAnnotationScreen
import com.example.educapp.commons.auth.WelcomeScreen
import com.example.educapp.commons.calendar.EventCreationScreen
import com.example.educapp.commons.calendar.EventDetailsScreen
import com.example.educapp.commons.student.StudentMainScreen
import com.example.educapp.commons.teacher.TeacherMainScreen
import com.example.educapp.commons.assistant.AssistantScreen
import com.example.educapp.commons.classwork.ActivityDetailScreen
import com.example.educapp.commons.classwork.ClassworkPartialScreen
import com.example.educapp.commons.classwork.PartialDetailScreen
import com.example.educapp.commons.teacher.attendance.AttendanceScreen
import com.example.educapp.commons.teacher.attendance.AttendanceViewModel
import com.example.educapp.commons.teacher.attendance.CheckScreen
import com.example.educapp.commons.teacher.attendance.TakeAttendanceDetailsScreen
import com.example.educapp.commons.teacher.attendance.TakeAttendanceScreen
import com.example.educapp.commons.teacher.calendar.CalendarMainScreen
import com.example.educapp.commons.teacher.calendar.EventRepository
import com.example.educapp.commons.teacher.grading.CheckGradesScreen
import com.example.educapp.commons.teacher.grading.GradesMainScreen
import com.example.educapp.commons.teacher.grading.GradesPartialScreen
import com.example.educapp.commons.teacher.grading.GradesViewModel
import com.example.educapp.commons.teacher.grading.TakeGradesScreen
import com.example.educapp.commons.teacher.groups.GroupDashboardScreen
import com.example.educapp.commons.teacher.groups.GroupsMainScreen
import com.example.educapp.commons.teacher.settings.SettingsMainScreen
import com.example.educapp.commons.teacher.settings.SettingsRepository
import com.example.educapp.commons.teacher.settings.SettingsViewModel
import com.example.educapp.commons.ui.AppTheme
import com.example.educapp.commons.ui.MyAppTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.net.URLDecoder

@SuppressLint("SetJavaScriptEnabled")
class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(this)

        // Request POST_NOTIFICATIONS permission if needed (only required on API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                val requestPermissionLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (!isGranted) {
                        // Handle the case where the user denies the permission.
                    }
                }
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent {
            val settingsRepository = SettingsRepository(applicationContext)
            val settingsViewModel = SettingsViewModel(settingsRepository)
            val currentTheme by settingsViewModel.theme.collectAsState()
            val timerDuration by settingsViewModel.timerDuration.collectAsState()
            MyAppTheme(theme = currentTheme, timerDuration = timerDuration) {
                var content by remember { mutableStateOf("") }
                navController = rememberAnimatedNavController()
                // If you still have the dark theme flag somewhere, you might need to update that too.
                MyApp(
                    navController,
                    content = content,
                    onContentChange = { newContent -> content = newContent },
                    isDarkTheme = (currentTheme == AppTheme.CinnamoSpiral), // or however you want to derive this
                    onDarkThemeChanged = { /* update if needed */ }
                )
            }
        }
        handleDeepLink(intent.data)
    }
    private fun handleDeepLink(uri: Uri?) {
        uri?.let {
            if (it.toString().contains("mode=verifyEmail")) {
                Firebase.auth.currentUser?.reload()?.addOnCompleteListener {
                    // Navigate using the stored nav controller
                    navController.navigate("verify_email") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent?.data)

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


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MyApp(
    navController: NavHostController,
    content: String,
    onContentChange: (String) -> Unit,
    isDarkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit
) {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = "welcome",
        enterTransition = { fadeIn(animationSpec = tween(800, easing = FastOutSlowInEasing)) },
        exitTransition = { fadeOut(animationSpec = tween(800, easing = FastOutSlowInEasing)) },
        popEnterTransition = { fadeIn(animationSpec = tween(800, easing = FastOutSlowInEasing)) },
        popExitTransition = { fadeOut(animationSpec = tween(800, easing = FastOutSlowInEasing)) }
    ) {
        // Auth screens
        composable("welcome") { WelcomeScreen(navController) }
        composable("registration") { RegistrationScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("verify_email") { EmailVerificationScreen(navController) }


        // Teacher navigation group
        navigation(startDestination = "teacher/main", route = "teacher") {
            composable("teacher/main") {  // Get teacherId from Firebase (or use a default value)
                val teacherUsername = Firebase.auth.currentUser?.displayName ?: "defaultUsername"
                val eventRepository = EventRepository()
                TeacherMainScreen(navController, teacherUsername, eventRepository)
                }

            composable ("teacher/groups") {
                val teacherId = Firebase.auth.currentUser?.uid ?: ""
                val viewModel: AttendanceViewModel = koinViewModel()
                GroupsMainScreen(navController, viewModel, teacherId)
            }

            composable(
                route = "teacher/group_dashboard/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                // Get the actual group from your ViewModel's groups list.
                val attendanceViewModel: AttendanceViewModel = koinViewModel()
                // Now use the instance's groups property
                val group = attendanceViewModel.groups.find { it.id == groupId }
                if (group != null) {
                    GroupDashboardScreen(
                        navController = navController,
                        group = group,
                        attendanceViewModel = attendanceViewModel
                    )
                } else {
                    // If group isn't found yet, show a loading or error state.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Group not found. Please try again.")
                    }
                }
            }

            composable("teacher/attendance") {
                val viewModel: AttendanceViewModel = koinViewModel()
                val teacherId = Firebase.auth.currentUser?.uid ?: ""
                AttendanceScreen(viewModel, navController,teacherId)
            }
            composable(
                route = "teacher/take_attendance/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                val viewModel: AttendanceViewModel = koinViewModel()
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
                val viewModel: AttendanceViewModel = koinViewModel()
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
                val viewModel: AttendanceViewModel = koinViewModel()
                CheckScreen(viewModel, groupId, partial, navController)
            }

            composable("teacher/settings") {

                val settingsViewModel: SettingsViewModel = koinViewModel()
                SettingsMainScreen(settingsViewModel)
            }

            // NEW: Calendar and Event feature routes
            composable("teacher/calendar") {
                val teacherUsername = Firebase.auth.currentUser?.displayName ?: "defaultUsername"
                CalendarMainScreen(navController, teacherUsername)
            }
            composable(
                route = "teacher/event_creation/{dateString}",
                arguments = listOf(navArgument("dateString") { type = NavType.StringType })
            ) { backStackEntry ->
                val dateString = backStackEntry.arguments?.getString("dateString") ?: ""
                // Parse the date string to LocalDate (make sure your format matches)
                val initialDate = java.time.LocalDate.parse(dateString)
                // EventCreationScreen: allow teacher to schedule an event
                val teacherUsername = Firebase.auth.currentUser?.displayName ?: "defaultUsername"
                EventCreationScreen(
                    navController = navController,
                    teacherUsername = teacherUsername,
                    initialDate = initialDate,
                    eventRepository = EventRepository()
                )
            }
            composable(
                route = "teacher/event_details/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                val teacherUsername = Firebase.auth.currentUser?.displayName ?: "defaultUsername"
                // EventDetailsScreen: show event details
                EventDetailsScreen(
                    navController = navController,
                    teacherUsername = teacherUsername,
                    eventId = eventId,
                    eventRepository = EventRepository()
                )
            }
            // 1) Grades Main Screen: list groups
            composable("teacher/grades_main") {
                // You might retrieve teacherId from Firebase here or pass it in
                val teacherId = Firebase.auth.currentUser?.uid ?: ""
                val viewModel: GradesViewModel = koinViewModel()
                GradesMainScreen(navController, viewModel, teacherId)
            }

            // 2) Grades Partial Screen
            composable(
                route = "teacher/grades_partial/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                GradesPartialScreen(navController, groupId)
            }

            // 3) Take Grades Screen
            composable(
                route = "teacher/take_grades/{groupId}/{partial}",
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("partial") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                val partial = backStackEntry.arguments?.getInt("partial") ?: 1
                val viewModel: GradesViewModel = koinViewModel()
                val settingsViewModel: SettingsViewModel = koinViewModel()
                TakeGradesScreen(navController, viewModel, groupId, partial, settingsViewModel)
            }

            composable(
                route = "teacher/check_grades/{groupId}/{partial}",
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("partial") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                val partial = backStackEntry.arguments?.getInt("partial") ?: 1

                val viewModel: GradesViewModel = koinViewModel()
                CheckGradesScreen(navController, viewModel, groupId, partial)
            }



            // In your NavGraph definition
            composable("classwork/{groupId}") { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                ClassworkPartialScreen(
                    navController = navController,
                    groupId = groupId
                )
            }
        }
        composable("partialDetail/{partialId}") { backStackEntry ->
            val partialId = backStackEntry.arguments?.getString("partialId") ?: ""
            PartialDetailScreen(partialId = partialId)
        }

        composable("activityDetail/{groupId}/{activityId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val activityId = backStackEntry.arguments?.getString("activityId") ?: ""
            ActivityDetailScreen(activityId = activityId, groupId = groupId)
        }



        composable("assistant/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            // Get userRole from your app's session/auth system
            val userRole = remember { UserRole.TEACHER } // Replace with actual source

            AssistantScreen(
                navController = navController,
                viewModel = koinViewModel(parameters = { parametersOf(userRole, groupId) }),
                groupId = groupId
            )
        }

        navigation(startDestination = "student_main", route = "student") {
            composable("student_main") {  // ← Must be top-level
                StudentMainScreen(navController)
            }
        }

        composable("annotation") {
            // AnnotationScreen is a composable implementing the PDF annotation feature.
            AnnotationMainScreen(navController)
        }
        composable(
            "annotation/{encodedOption}",
            arguments = listOf(navArgument("encodedOption") { type = NavType.StringType })
        ) { backStackEntry ->
            val decodedOption = URLDecoder.decode(
                backStackEntry.arguments?.getString("encodedOption"),
                "UTF-8"
            )
            BookAnnotationScreen(
                navController = navController,
                englishOption = decodedOption
            )
        }



    }
}

