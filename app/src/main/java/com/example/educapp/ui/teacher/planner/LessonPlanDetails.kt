package com.example.educapp.ui.teacher.planner

import android.util.Log
import android.webkit.WebView
import org.apache.commons.text.StringEscapeUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import org.jsoup.Jsoup
import java.net.URLEncoder

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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ){
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(it.title, style = MaterialTheme.typography.headlineMedium)
                        Text("Level: ${it.level}", style = MaterialTheme.typography.bodyMedium)
                        Text("Topic: ${it.topic}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ){
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                                Log.d("LessonPlanDetailsScreen", "Content before loading: ${it.content}")

                                val doc = Jsoup.parse(StringEscapeUtils.unescapeJava(it.content))
                                Log.d("LessonPlanDetailsScreen", "doc content: $doc")
                                val cleanedContent = doc.html()
                                Log.d("LessonPlanDetailsScreen", "cleanedContent content: $cleanedContent")
                                val dataUri = "data:text/html;charset=utf-8" + URLEncoder.encode(cleanedContent, "utf-8")
                                loadUrl(dataUri)
                                quillEditor = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}