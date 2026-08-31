package com.example.educapp.commons.classwork

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.setValue
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.educapp.commons.ui.CircularButton
import com.example.educapp.R
import com.example.educapp.commons.ui.hapticClickable


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
                    ClassworkGroupedList(
                        partials = partials,
                        activities = activities,
                        groupId = groupId,
                        navController = navController
                    )
                }
            }
        }
        // CircularButton appears in ALL states (loading, error, empty list)
        CircularButton(
            imageResId = R.drawable.activity_icon,
            isLarge = true,
            onClick = { navController.navigate("assistant/${groupId}") },
            modifier = Modifier
                .align(Alignment.BottomEnd)   // Bottom right
                .padding(16.dp)              // Some spacing from edges
        )
    }
}

@Composable
fun PartialGroupCard(
    partialNumber: Int,
    activities: List<ClassworkActivity>,
    navController: NavHostController,
    groupId: String  // New parameter
) {
    var expanded by remember { mutableStateOf(false) }

    GradientCard(
        onClick = { expanded = !expanded },
        modifier = Modifier.padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Partial $partialNumber",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                if (activities.isEmpty()) {
                    Text("No activities found for this partial.")
                } else {
                    // Make each activity clickable to navigate to detail
                    activities.forEach { activity ->
                        Text(
                            text = "• ${activity.title}",
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .hapticClickable {
                                    navController.navigate("activityDetail/$groupId/${activity.id}")
                                }
                        )
                    }
                }
            }
        }
    }
}



@Composable
private fun ClassworkGroupedList(
    partials: List<ClassworkPartial>,
    activities: List<ClassworkActivity>,
    navController: NavHostController,
    groupId: String
) {
    // Group partials by partialNumber
    val groupedPartials = partials.groupBy { it.partialNumber }
    // Sort the groups by partial number (ascending)
    val sortedGroupKeys = groupedPartials.keys.sorted()

    LazyColumn {
        items(sortedGroupKeys) { partialNumber ->
            // Get the list of partials in this group
            val groupPartials = groupedPartials[partialNumber] ?: emptyList()
            // Merge activities from all partials in this group
            val groupActivities = activities.filter { activity ->
                groupPartials.any { partial -> partial.id == activity.partialId }
            }
            PartialGroupCard(
                partialNumber = partialNumber,
                activities = groupActivities,
                navController = navController,
                groupId = groupId
            )
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