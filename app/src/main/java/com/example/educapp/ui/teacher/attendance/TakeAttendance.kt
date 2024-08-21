package com.example.educapp.ui.teacher.attendance

import android.widget.Toast
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.collections.find

@Composable
fun TakeAttendanceScreen(viewModel: AttendanceViewModel, groupId: String, navController: NavHostController) {
    val group = viewModel.groups.find { it.id == groupId }
    val groupName = group?.name ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Take Attendance for $groupName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Partial boxes
        for (partial in 1..3) {
            PartialBox(
                partial = partial,
                onAttendanceClick = {
                    navController.navigate("attendance/$groupId/$partial")
                },
                onCheckClick = {
                    navController.navigate("check/$groupId/$partial")
                }
            )
        }
    }
}

@Composable
fun PartialBox(partial: Int, onAttendanceClick: () -> Unit, onCheckClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Partial $partial")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = onAttendanceClick) {
                    Text("Attendance")
                }
                Button(onClick = onCheckClick) {
                    Text("Check")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceDetailsScreen(
    viewModel: AttendanceViewModel,
    groupId: String,
    partial: Int,
    navController: NavHostController
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val attendanceRecords = remember { mutableStateListOf<AttendanceRecord>() }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val group = viewModel.groups.find { it.id == groupId } // Get group from ViewModel
    val students = group?.students ?: emptyList()

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { showDatePicker = true }) {
            Text("Select Date: $selectedDate")
        }
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(onClick = { showDatePicker = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDay(
                    ZoneId.systemDefault()).toInstant().toEpochMilli()))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        students.forEach { student ->
            StudentItem(student) { status ->
                val existingRecord = attendanceRecords.find { it.student == student }
                if (existingRecord != null) {
                    attendanceRecords.remove(existingRecord)
                }
                val record = AttendanceRecord(
                    student = student,
                    groupId = groupId,
                    partial = partial,
                    status = status,
                    timestamp = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()
                )
                attendanceRecords.add(record)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (attendanceRecords.size == students.size) {
            Button(onClick = { showConfirmationDialog = true }) {
                Text("Confirm Attendance")
            }
        }

        if (showConfirmationDialog) {
            val coroutineScope = rememberCoroutineScope()
            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                title = { Text("Confirm Attendance") },
                text = { Text("Do you want to save the attendance?") },
                confirmButton = {
                    Button(onClick = {
                        showConfirmationDialog = false // Use rememberCoroutineScope
                        coroutineScope.launch{
                            viewModel.saveAttendance(groupId, attendanceRecords)
                            delay(Duration.ofMillis(1000))
                            Toast.makeText(context, "Attendance saved!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack() // Navigate back to the previous screen
                        }
                    }) {
                        Text("Save")
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

@Composable
fun StudentItem(student: String, onAttendanceRecorded: (AttendanceStatus) -> Unit) {
    var attendanceStatus by remember { mutableStateOf<AttendanceStatus?>(null) }

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = student)
        Spacer(modifier = Modifier.weight(1f))
        AttendanceOption(AttendanceStatus.PRESENT, Color.Green) {
            attendanceStatus = AttendanceStatus.PRESENT
        }
        AttendanceOption(AttendanceStatus.LATE, Color.Yellow) {
            attendanceStatus = AttendanceStatus.LATE
        }
        AttendanceOption(AttendanceStatus.ABSENT, Color.Red) {
            attendanceStatus = AttendanceStatus.ABSENT
        }
    }

    LaunchedEffect(key1 = attendanceStatus) {
        if (attendanceStatus != null) {
            onAttendanceRecorded(attendanceStatus!!)
            attendanceStatus = null
        }
    }
}

@Composable
fun AttendanceOption(status: AttendanceStatus, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color)
            .clickable { onClick() }
    )
}