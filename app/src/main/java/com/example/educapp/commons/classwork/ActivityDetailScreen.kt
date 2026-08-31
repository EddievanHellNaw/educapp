package com.example.educapp.commons.classwork

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.getValue
import com.example.educapp.commons.classwork.ClassworkViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ActivityDetailScreen(
    activityId: String,
    groupId: String,
    viewModel: ClassworkViewModel = koinViewModel(
        parameters = { parametersOf(groupId) } // So Koin can inject the correct groupId
    )
) {
    val allActivities by viewModel.activities.collectAsState()
    // Find the activity in your local state
    val activity = allActivities.find { it.id == activityId }

    if (activity == null) {
        // Show a loading or not found message
        Text("Activity not found.")
    } else {
        // Show the activity details
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = activity.title, style = MaterialTheme.typography.headlineSmall)
            Text(text = "Type: ${activity.type}")
            Text(text = "Description: ${activity.description}")
            activity.dueDate?.let {
                Text("Due: ${it.toDate()}") // or a nicer date format
            }

            // If you want to show materials, do so:
            if (activity.materials.isEmpty()) {
                Text("No materials attached.")
            } else {
                activity.materials.forEach { material ->
                    Text("Material: ${material.type}, ${material.description}")
                    // Possibly wrap in clickable or show a link
                }
            }
        }
    }
}
