package com.example.educapp.ui.teacher.planner

import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import androidx.constraintlayout.compose.ChainStyle
import com.example.educapp.R
import kotlinx.coroutines.launch
import jp.wasabeef.richeditor.RichEditor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPlanScreen(navController: NavController, viewModel: PlannerViewModel, onContentChange: (String)-> Unit) {

    var title by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }

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
            val (titleRef, levelRef, topicRef, descriptionRef, editorRef, toolbarRef) = createRefs()
            createVerticalChain(titleRef, levelRef, topicRef, descriptionRef, editorRef, toolbarRef, chainStyle = ChainStyle.Packed)

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
                    val view = LayoutInflater.from(context).inflate(R.layout.editor_layout, null)
                    val richEditor = view.findViewById<RichEditor>(R.id.rich_editor)
                    val toolbar = view.findViewById<LinearLayout>(R.id.toolbar)
                    richEditor.apply {
                        setEditorFontSize(16)
                        setEditorFontColor(android.graphics.Color.BLACK)
                        setEditorBackgroundColor(android.graphics.Color.WHITE)
                        setPadding(10, 10, 10, 10)
                        setPlaceholder("Write your lesson plan here...")
                        setOnTextChangeListener { text ->
                            content = text
                            onContentChange(text)
                        }
                    }
                    val boldButton = toolbar.findViewById<ImageButton>(R.id.action_bold)
                    boldButton.setOnClickListener {
                        richEditor.setBold()
                    }
                    view
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(editorRef) {
                        top.linkTo(descriptionRef.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            )

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
                                            // Handle error
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

