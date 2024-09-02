package com.example.educapp.ui.teacher.planner

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPlannerScreen(navController: NavController, viewModel: PlannerViewModel) {
    val lessonPlans by viewModel.lessonPlans.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var lessonPlanToDelete by remember { mutableStateOf<LessonPlan?>(null) }

    LaunchedEffect(Unit) {
        viewModel.getLessonPlans()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Lesson Plans") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("teacher/newPlan") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Lesson Plan")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Confirm Delete") },
                    text = { Text("Are you sure you want to delete this lesson plan?") },
                    confirmButton = {
                        Button(onClick = {
                            lessonPlanToDelete?.let { viewModel.deleteLessonPlan(it) }
                            showDialog = false
                        }) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            LessonPlanList(lessonPlans,
                onEdit = { lessonPlan ->
                    // Handle edit action (e.g., navigate to edit screen)
                    navController.navigate("teacher/editLessonPlan/${lessonPlan.id}")
                },
                onDelete = { lessonPlan ->
                    showDialog = true
                    lessonPlanToDelete = lessonPlan
                }
                , navController
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LessonPlanList(
    lessonPlans: List<LessonPlan>,
    onEdit: (LessonPlan) -> Unit,
    onDelete: (LessonPlan) -> Unit,
    navController: NavController
) {
    LazyColumn {
        items(lessonPlans) { lessonPlan ->
            LessonPlanCard(lessonPlan, onEdit, onDelete, navController)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LessonPlanCard(
    lessonPlan: LessonPlan,
    onEdit: (LessonPlan) -> Unit,
    onDelete: (LessonPlan) -> Unit,
    navController: NavController
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { navController.navigate("teacher/lessonPlanDetails/${lessonPlan.id}") }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(lessonPlan.title, style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }
            if (expanded) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEdit(lessonPlan)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete(lessonPlan)
                            expanded = false
                        }
                    )
                }
            }

            Text("Level: ${lessonPlan.level}", style = MaterialTheme.typography.bodyMedium)
            Text("Topic: ${lessonPlan.topic}", style = MaterialTheme.typography.bodyMedium)
            Text("Description: ${lessonPlan.description}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}