package com.example.educapp.ui.teacher.planner

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import androidx.constraintlayout.compose.ChainStyle


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun NewPlanScreen(navController: NavController, viewModel: PlannerViewModel) {

    var title by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isContentLoaded by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
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
        ConstraintLayout(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val (titleRef, levelRef, topicRef, descriptionRef, editorRef) = createRefs()
            createVerticalChain(titleRef, levelRef, topicRef, descriptionRef, editorRef, chainStyle = ChainStyle.Packed)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(titleRef) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            )
            OutlinedTextField(
                value = level,
                onValueChange = { level = it },
                label = { Text("Level") },
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(levelRef) {
                        top.linkTo(titleRef.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            )
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Topic") },
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(topicRef) {
                        top.linkTo(levelRef.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(descriptionRef) {
                        top.linkTo(topicRef.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            )

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                view.postDelayed({
                                    val js = "var quill = new Quill('#editor', { modules: { toolbar: true }, theme: 'snow' });"
                                    view.evaluateJavascript(js) { result ->
                                        quillEditor = this@apply
                                    }
                                }, 500)
                            }
                        }
                        loadDataWithBaseURL(
                            null,
                            """
                            <html>
                            <head>
                                <style>
                                body{font-size:16px;}
                                #editor-container{height: 100%}
                                </style> 
                                <link href="https://cdn.quilljs.com/1.3.6/quill.snow.css" rel="stylesheet">
                                <script src="https://cdn.quilljs.com/1.3.6/quill.js"></script>
                            </head>
                            <body>
                            <div id = "editor-container">
                                <div id='editor'></div>
                            </div>
                            </body>
                            </html>
                            """.trimIndent(),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                modifier = Modifier
                    .constrainAs(editorRef) {
                        top.linkTo(descriptionRef.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
            )

            if (showConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = { Text("Confirm Save") },
                    text = { Text("Are you sure you want to save this lesson plan?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                quillEditor?.evaluateJavascript(
                                    "(function() { return quill.root.innerHTML; })();"
                                ) { value ->
                                    content = value
                                    isContentLoaded = true
                                    val lessonPlan = LessonPlan(
                                        title = title,
                                        level = level,
                                        topic = topic,
                                        description = description,
                                        content = content
                                    )
                                    Log.d("EditorScreen", "Lesson plan content before saving: ${lessonPlan.content}")
                                    viewModel.saveLessonPlan(lessonPlan)
                                    showConfirmationDialog = false
                                    navController.popBackStack()
                                }
                            },
                            enabled = isContentLoaded// Disable button until content is loaded
                        ) {
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