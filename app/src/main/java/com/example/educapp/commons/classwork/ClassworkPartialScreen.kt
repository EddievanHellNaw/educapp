package com.example.educapp.commons.classwork

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.commons.teacher.attendance.AttendanceGroup
import com.example.educapp.commons.teacher.attendance.AttendanceViewModel
import com.example.educapp.commons.ui.GradientCard
import com.example.educapp.commons.ui.HapticButton
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.foundation.lazy.items
import com.example.educapp.commons.ui.CircularButton
import com.example.educapp.R


@Composable
fun ClassworkPartialScreen(
    navController: NavHostController,
    groupId: String,
    viewModel: ClassworkViewModel = koinViewModel(parameters = { parametersOf(groupId) }),
    attendanceViewModel: AttendanceViewModel = koinViewModel()
) {
    val activities by viewModel.activities.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val partials by viewModel.partials.collectAsState()
    Log.d("DEBUG", "ClassworkPartialScreen groupId = $groupId")

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is ClassworkViewModel.ClassworkUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ClassworkViewModel.ClassworkUiState.Error -> {
                val message = (uiState as ClassworkViewModel.ClassworkUiState.Error).message
                ErrorMessage(message)
            }
            is ClassworkViewModel.ClassworkUiState.Success -> {
                if (partials.isEmpty()) {
                    EmptyStateMessage()
                } else {
                    ClassworkList(
                        partials = partials,
                        activities = activities,
                        attendanceViewModel = attendanceViewModel, // Add this
                        onApprove = { viewModel.approvePartial(it.id) },
                        groupId = groupId,
                        navController = navController // Add this
                    )
                }
            }
        }

        // CircularButton appears in ALL states (loading, error, empty list)
        CircularButton(
            imageResId = R.drawable.activity_icon,
            isLarge = true,
            onClick = { navController.navigate("assistant/${groupId}") }
        )
    }
}

@Composable
private fun ClassworkList(
    partials: List<ClassworkPartial>,
    activities: List<ClassworkActivity>,
    attendanceViewModel: AttendanceViewModel, // Add this parameter
    onApprove: (ClassworkPartial) -> Unit,
    groupId: String,
    navController: NavHostController
) {
    LazyColumn {
        items(partials) { partial ->
            // Get group for this partial
            val group by attendanceViewModel.getGroupById(partial.groupId)
                .collectAsState(initial = null)

            val partialActivities = activities
                .filter { it.partialId == partial.id }
                .sortedBy { it.dueDate }

            PartialClassworkCard(
                partial = partial,
                activities = partialActivities,
                group = group,
                onApprove = { onApprove(partial) },
                onSelect = {
                    // Add navigation to partial detail
                    navController.navigate("partialDetail/${partial.id}")
                }
            )
        }
    }
}

@Composable
private fun PartialClassworkCard(
    partial: ClassworkPartial,
    activities: List<ClassworkActivity>,
    group: AttendanceGroup?,
    onApprove: () -> Unit,
    onSelect: () -> Unit
) {
    // Handle null group case
    val groupName = group?.name ?: "Unknown Group"
    val groupColor = group?.getColor() ?: MaterialTheme.colorScheme.surface

    GradientCard (
        onClick = onSelect,
        modifier = Modifier.padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Partial ${partial.partialNumber}", style = MaterialTheme.typography.titleLarge)
            Text(groupName, style = MaterialTheme.typography.bodyMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                HapticButton(onClick = onApprove) {
                    Text("Approve")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = partial.status.name,
                    color = when (partial.status) {
                        ClassworkStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
                        ClassworkStatus.APPROVED -> MaterialTheme.colorScheme.primary
                        ClassworkStatus.ARCHIVED -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyStateMessage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No classwork partials found", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}