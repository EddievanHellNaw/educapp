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

@Composable
fun PartialDetailScreen(
    partialId: String,
    classworkViewModel: ClassworkViewModel = koinViewModel()
) {
    // 1. Get the partial object
    val partial = classworkViewModel.getPartialById(partialId)
    // 2. Filter the activities for this partial
    val allActivities by classworkViewModel.activities.collectAsState()
    val partialActivities = allActivities.filter {
        it.partialId == partialId
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Partial ${partial?.partialNumber ?: "?"}", style = MaterialTheme.typography.titleLarge)
        Text("Activities:", style = MaterialTheme.typography.titleMedium)
        partialActivities.forEach { activity ->
            Text("- ${activity.title}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
