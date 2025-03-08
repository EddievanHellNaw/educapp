package com.example.educapp.commons.classwork

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.commons.teacher.attendance.AttendanceGroup
import com.example.educapp.commons.ui.GradientCard
import com.example.educapp.commons.ui.hapticClickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import com.example.educapp.commons.teacher.attendance.AttendanceViewModel
import com.example.educapp.commons.teacher.grading.GradesViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassworkMainScreen(
    navController: NavHostController,
    viewModel: AttendanceViewModel,
    teacherId: String
) {

    // Start listening to groups for this teacher
    LaunchedEffect(teacherId) {
        viewModel.startGroupsListener(teacherId)
        Log.d("GradesMainScreen", "teacherId: $teacherId")
    }
    val groups = viewModel.groups
    Log.d("GradesMainScreen", "groups: $groups")

    Surface(color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Classwork") }) }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                LazyColumn {
                    items(groups) { group ->
                        GroupCard(
                            group = group,
                            onClick = {
                                // Navigate to partial selection screen
                                navController.navigate("teacher/grades_partial/${group.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupCard(group: AttendanceGroup, onClick: () -> Unit) {
    val selectedColor = remember { mutableStateOf(group.getColor())
    }
    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .hapticClickable{ onClick() },
        shape = RoundedCornerShape(8.dp),
        gradientBrush = Brush.horizontalGradient(
            colors = listOf(MaterialTheme.colorScheme.surface, selectedColor.value)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(text = group.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = group.schedule, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
