package com.example.educapp.ui.teacher.planner

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlanDetailsScreen(lessonPlanId: String, viewModel: PlannerViewModel, navController: NavController) {

    val lessonPlan by snapshotFlow { viewModel.lessonPlan.value }.collectAsState(initial = null)
    var quillEditor by remember { mutableStateOf<WebView?>(null) }

    Log.d("LessonPlanDetailsScreen", "Lesson plan in composable: $lessonPlan") // Log the lesson plan value
    Log.d("LessonPlanDetailsScreen", "Lesson plan ID received: $lessonPlanId") // Log the ID

    LaunchedEffect(key1 = lessonPlanId) { // Fetch lesson plan when lessonPlanId changes
        viewModel.fetchLessonPlanById(lessonPlanId)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lesson Plan Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            lessonPlan?.let {
                Text(it.title, style = MaterialTheme.typography.headlineMedium)
                Text("Level: ${it.level}", style = MaterialTheme.typography.bodyMedium)
                Text("Topic: ${it.topic}", style = MaterialTheme.typography.bodyMedium)
                Text("Description: ${it.description}", style = MaterialTheme.typography.bodyMedium)

                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                                    val htmlContent = "<html><head><style>body{font-size:16px;}</style></head><body>${it.content}</body></html>"
                                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                                    quillEditor = this
                        }
                    },
                    modifier = Modifier.fillMaxSize() // Ensure the WebView fills the available space
                )
            }
        }
    }
}