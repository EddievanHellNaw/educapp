package com.example.educapp.ui.teacher.planner

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import androidx.constraintlayout.compose.ChainStyle
import kotlinx.coroutines.launch

// Interface for the editor change callback
interface EditorChangeCallback {
    fun onEditorChange(newContent: String)
}

// Object to hold the callback
object AndroidView {
    lateinit var onEditorChange: EditorChangeCallback
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPlanScreen(navController: NavController, viewModel: PlannerViewModel, onContentChange: (String)-> Unit) {

    var title by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .constrainAs(editorRef) {
                        top.linkTo(descriptionRef.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            addJavascriptInterface(WebAppInterface(context, onContentChange), "Android")
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, url: String) {
                                    super.onPageFinished(view, url)
                                    quillEditor = view
                                    quillEditor?.let {
                                        try {
                                            it.evaluateJavascript(
                                                """
                                                    quill.on('editor-change', function() {
                                                        Android.onEditorChange(quill.root.innerHTML);
                                                    });
                                                """.trimIndent(),
                                                null
                                            )
                                            Log.d("NewPlanScreen", "WebView page finished loading")
                                        } catch (e: Exception) {
                                            Log.e("NewPlanScreen", "Error evaluating JavaScript", e)
                                        }
                                    }
                                }
                            }
                            loadDataWithBaseURL(
                                null,
                                """
                            <html>
                            <head>
                               <link href="https://cdn.jsdelivr.net/npm/quill@2.0.2/dist/quill.snow.css" rel="stylesheet"> 
                               <style>
                                    #editor > div.ql-editor {
                                      overflow-y: visible;
                                      -webkit-user-select: none;
                                      -khtml-user-select: none;
                                      -moz-user-select: none;
                                      -o-user-select: none;
                                      user-select: none;
                                    }
                               </style>
                            </head>
                            <body>
                                                                
                               <div id="editor">
                                      
                               </div>
                               <script src="https://cdn.jsdelivr.net/npm/quill@2.0.2/dist/quill.js"></script>
                                <script>
                                  const quill = new Quill('#editor', {
                                    placeholder: 'Write your lesson plan here...',
                                    theme: 'snow',
                                  });
                                </script>
                            </body>
                            </html>
                            """.trimIndent(),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                AndroidView.onEditorChange = object : EditorChangeCallback {
                    override fun onEditorChange(newContent: String) {
                        onContentChange(newContent)
                    }
                }
            }


            if (showConfirmationDialog) {
                var isLoading by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = { Text("Confirm Save") },
                    text = { Text("Are you sure you want to save this lesson plan?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    Log.d("NewPlanScreen", "Content value is: $content")
                                    quillEditor?.evaluateJavascript(
                                        "(function() { return quill.root.innerHTML; })();",

                                    ) { value ->
                                        Log.d("NewPlanScreen", "evaluateJavascript callback called")
                                        if (value != null) {
                                            content = value.removeSurrounding("\"")
                                            val lessonPlan = LessonPlan(
                                                title = title,
                                                level = level,
                                                topic = topic,
                                                description = description,
                                                content = content
                                            )
                                            Log.d("NewPlanScreen", "Lesson plan content before saving: $content")
                                            viewModel.saveLessonPlan(lessonPlan) { success ->
                                                Log.d(
                                                    "NewPlanScreen",
                                                    "Save callback called with success: $success"
                                                )
                                                isLoading = false
                                                if (success) {
                                                    showConfirmationDialog = false
                                                    navController.popBackStack()
                                                } else {
                                                    // Handle error (e.g., show a Snackbar)
                                                }
                                            }
                                        } else {
                                            isLoading = false
                                            // Handle error (e.g., show a Snackbar)
                                        }
                                    }
                                }
                                },
                            enabled = !isLoading,
                            modifier = Modifier.width(150.dp)
                        ) {
                            if(isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Text("Save")
                            }
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