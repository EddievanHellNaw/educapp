package com.example.educapp.commons.teacher.grading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.commons.ui.GradientCard
import com.example.educapp.commons.ui.HapticButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesPartialScreen(
    navController: NavHostController,
    groupId: String
) {
    val groupName = "English 5 1pm"
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Partial to grade") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Example partial boxes (1,2,3)
            LazyColumn {
                items(3) { partialIndex ->
                    PartialCard(
                        partial = partialIndex + 1,
                        groupName = groupName,
                        onCheckClick = {
                            // Navigate to the CheckAttendanceScreen
                            navController.navigate("teacher/check_grades/$groupId/${partialIndex + 1}")
                        },
                        onTakeClick = {
                            // Navigate to the TakeGradesScreen
                            navController.navigate("teacher/take_grades/$groupId/${partialIndex + 1}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PartialCard(
    partial: Int,
    groupName: String,
    onCheckClick: () -> Unit,
    onTakeClick: () -> Unit
) {
    // Example color for the right side of the gradient (adjust as needed)
    val buttonColor = MaterialTheme.colorScheme.primary

    // GradientCard is your custom composable that applies a gradient background
    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        // Outer Box with an additional gradient if desired
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            buttonColor
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Layout: partial text & group name on left,
            // two pill-shaped buttons on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left column: partial text & group name
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Partial $partial",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Right column: two buttons stacked vertically
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Button 1: Check Attendance
                    HapticButton(
                        onClick = onCheckClick,
                        modifier = Modifier.fillMaxWidth(fraction = 0.8f), // pill width
                    ) {
                        Text(
                            text = "Check Grades",
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 2: Take Grades
                    HapticButton(
                        onClick = onTakeClick,
                        modifier = Modifier.fillMaxWidth(fraction = 0.8f),
                    ) {
                        Text(
                            text = "Mark Grades",
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
