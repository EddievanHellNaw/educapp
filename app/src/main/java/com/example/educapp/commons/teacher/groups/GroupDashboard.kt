package com.example.educapp.commons.teacher.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.commons.teacher.attendance.AttendanceGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.educapp.commons.classwork.ClassworkMainScreen
import com.example.educapp.commons.classwork.ClassworkPartialScreen
import com.example.educapp.commons.teacher.attendance.AttendanceViewModel
import com.example.educapp.commons.teacher.attendance.TakeAttendanceScreen
import com.example.educapp.commons.teacher.grading.GradesPartialScreen


@Composable
fun GroupDashboardScreen(
    navController: NavHostController,
    group: AttendanceGroup,
    attendanceViewModel: AttendanceViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Attendance", "Grades", "Classwork")

    Column(modifier = Modifier.fillMaxSize()) {
        // Group header (you can add more details as needed)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = group.schedule,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        // Tabs for different group features
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        // Switch the content based on the selected tab
        when (selectedTab) {
            0 -> TakeAttendanceScreen(
                viewModel = attendanceViewModel,
                groupId = group.id,
                navController = navController
            )
            1 -> GradesPartialScreen(
                navController = navController,
                groupId = group.id
            )
            2 -> ClassworkPartialScreen( // Replace ClassworkMainScreen
                navController = navController,
                groupId = group.id
            )
        }
    }
}
