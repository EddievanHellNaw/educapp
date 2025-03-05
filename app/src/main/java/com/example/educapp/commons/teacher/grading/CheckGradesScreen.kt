package com.example.educapp.commons.teacher.grading

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.commons.ui.FrostedBox
import com.example.educapp.commons.ui.GradientCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckGradesScreen(
    navController: NavHostController,
    viewModel: GradesViewModel,
    groupId: String,
    partial: Int
) {
    // Observe student grades from the viewmodel
    val studentGrades by viewModel.studentGrades.collectAsState()

    // Load student grades for this group & partial
    LaunchedEffect(groupId, partial) {
        viewModel.loadStudentGrades(groupId, partial)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Registered Grades for Partial $partial") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn {
                items(items = studentGrades) { grade ->
                    StudentGradeCard(grade)
                }
            }
        }
    }
}

@Composable
fun StudentGradeCard(grade: StudentGrade) {

    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 1) Student Name at the top
            Text(
                text = grade.studentName,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                TableCell(
                    label = "GrV",
                    value = grade.oralGrV.toString(),
                    modifier = Modifier.weight(1f)
                )
                TableCell(
                    label = "DM",
                    value = grade.oralDM.toString(),
                    modifier = Modifier.weight(1f)
                )
                TableCell(
                    label = "Pron",
                    value = grade.oralPron.toString(),
                    modifier = Modifier.weight(1f)
                )
                TableCell(
                    label = "ICom",
                    value = grade.oralIntCom.toString(),
                    modifier = Modifier.weight(1f)
                )
                // 3) Oral
                TableCell(
                    label = "Oral",
                    value = grade.oral.toString(),
                    modifier = Modifier.weight(1f)
                )

            }
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                TableCell(
                    label = "Faltas",
                    value = grade.noFaltas.toString(),
                    modifier = Modifier.weight(1f)
                )

                // 4) Written
                TableCell(
                    label = "Written",
                    value = grade.written.toString(),
                    modifier = Modifier.weight(1f)
                )

                // 5) Portfolio
                TableCell(
                    label = "Portf.",
                    value = grade.portfolio.toString(),
                    modifier = Modifier.weight(1f)
                )

                // 6) Final
                TableCell(
                    label = "Final",
                    value = grade.finalGrade.toString(),
                    modifier = Modifier.weight(1f)
                )

            }
        }
    }
}

@Composable
fun TableCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    // Each “cell” is a small Box with a border
    FrostedBox (
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        // Label on top, value below
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

