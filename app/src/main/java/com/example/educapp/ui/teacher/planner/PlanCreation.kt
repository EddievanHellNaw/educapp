package com.example.educapp.ui.teacher.planner

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPlanScreen(navController: NavController, viewModel: PlannerViewModel) {
    var title by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    // Use AndroidView to embed Quill editor
    var quillEditor by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Lesson Plan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go Back")
                    }
                },
                actions = {
                    Button(onClick = { showConfirmationDialog = true }) {
                        Text("Save Lesson")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = level,
                onValueChange = { level = it },
                label = { Text("Level") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Topic") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            // Quill Editor
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        loadDataWithBaseURL(
                            null,
                            "<html><head><style>body{font-size:16px;}</style></head><body><div id='editor'></div></body></html>",
                            "text/html",
                            "UTF-8",
                            null
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                val js = "var quill = new Quill('#editor', { modules: { toolbar: true }, theme: 'snow' });"
                                evaluateJavascript(js) {
                                    quillEditor = this@apply
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )

            if (showConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = { Text("Confirm Save") },
                    text = { Text("Are you sure you want to save this lesson plan?") },
                    confirmButton = {
                        Button(onClick = {
                            val content = quillEditor?.let {
                                it.evaluateJavascript(
                                    "(function() { return quill.root.innerHTML; })();",
                                    null
                                )
                            } ?: ""
                            val lessonPlan = LessonPlan(
                                title = title,
                                level = level,
                                topic = topic,
                                description = description,
                                content = content.toString()
                            )
                            viewModel.saveLessonPlan(lessonPlan)
                            showConfirmationDialog = false
                            navController.popBackStack()
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showConfirmationDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}