package com.example.educapp.ui.teacher.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.foundation.layout.Arrangement


enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE
}
data class AttendanceRecord(
    val student: String,
    val partial: Int,
    val status: AttendanceStatus,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(viewModel: AttendanceViewModel, groupId: String) {
    val group = viewModel.groups.find { it.id == groupId }
    val groupName = group?.name ?: ""
    val students = group?.students ?: emptyList()

    val attendanceRecords = remember { mutableStateListOf<AttendanceRecord>() }
    var currentPartial by remember { mutableStateOf(1) }
    val totalPartials = 3
    var showAttendance by remember { mutableStateOf(false) }
    var showCheck by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

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
        for (partial in 1..totalPartials) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .clickable {
                        currentPartial = partial
                        showAttendance = true
                    }
            ) {
                if (showAttendance && currentPartial == partial) {
                    Column {
                        Button(onClick = { showDatePicker = true }) {
                            Text("Select Date")
                        }
                        if (showDatePicker) {
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    Button(onClick = {
                                        showDatePicker = false
                                    }) {
                                        Text("OK")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = { showDatePicker = false }) {
                                        Text("Cancel")
                                    }
                                }
                            ) {
                                val datePickerState = rememberDatePickerState(
                                    initialSelectedDateMillis = selectedDate.atStartOfDay()
                                        .toInstant(ZoneOffset.UTC).toEpochMilli()
                                )
                                DatePicker(
                                    state = datePickerState,
                                    title = { Text("Select Date") },
                                    modifier = Modifier.padding(16.dp)
                                )
                                LaunchedEffect(datePickerState.selectedDateMillis) {
                                    if (datePickerState.selectedDateMillis != null) {
                                        selectedDate = LocalDate.ofEpochDay(
                                            datePickerState.selectedDateMillis!! / 86400000
                                        )
                                    }
                                }
                            }
                        }
                        students.forEach { student ->
                            StudentItem(student) { status ->
                                val date =
                                    selectedDate.atStartOfDay().toInstant(ZoneOffset.UTC)
                                        .toEpochMilli()
                                val record =
                                    AttendanceRecord(student, currentPartial, status, date)
                                attendanceRecords.add(record)
                                if (attendanceRecords.size == students.size) {
                                    showConfirmationDialog = true
                                }
                            }
                        }
                        if (showConfirmationDialog) {
                            AlertDialog(
                                onDismissRequest = { showConfirmationDialog = false },
                                title = { Text("Confirm Attendance") },
                                text = { Text("Do you want to save the attendance?") },
                                confirmButton = {
                                    Button(onClick = {
                                        showConfirmationDialog = false
                                        showAttendance = false
                                        viewModel.saveAttendance(
                                            groupId,
                                            attendanceRecords
                                        )
                                        attendanceRecords.clear()
                                    }) {
                                        Text("Save")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = {
                                        showConfirmationDialog = false
                                        attendanceRecords.clear()
                                    }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                } else if (showCheck && currentPartial == partial) {
                    // Display attendance summary for the partial
                    AttendanceSummary(attendanceRecords.filter { it.partial == currentPartial })
                } else {
                    Text(
                        text = "Partial $partial",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (showAttendance && currentPartial == partial) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Button(onClick = {
                            showAttendance = false
                        }) {
                            Text("Attendance")
                        }
                        Button(onClick = {
                            showCheck = true
                            showAttendance = false
                        }) {
                            Text("Check")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentItem(student: String, onAttendanceRecorded: (AttendanceStatus) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = student)
        Spacer(modifier = Modifier.weight(1f))
        AttendanceOptions(student, onAttendanceRecorded)
    }
}

@Composable
fun AttendanceOptions(
    student: String,
    onAttendanceRecorded: (AttendanceStatus) -> Unit
) {
    var attendanceStatus by remember { mutableStateOf<AttendanceStatus?>(null) }

    Row {
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

    // Store attendance status for student (implementation needed)
    LaunchedEffect(key1 = attendanceStatus) {
        if (attendanceStatus != null) {
            onAttendanceRecorded(attendanceStatus!!)
            attendanceStatus = null // Reset status after recording
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

@Composable
fun AttendanceSummary(attendanceRecords: List<AttendanceRecord>) {
    val attendanceSummary = attendanceRecords.groupBy { it.student }
        .mapValues { (_, records) ->
            records.groupingBy { it.status }.eachCount()
        }

    LazyColumn {
        items(attendanceSummary.entries.toList()) { (student, summary) ->
            val absentCount = summary[AttendanceStatus.ABSENT] ?: 0
            val textColor = if (absentCount >= 6) Color.Red else Color.Black
            Column {
                Text(text = "Student: $student", color = textColor)
                summary.forEach { (status, count) ->
                    Text(text = "- $status: $count", color = textColor)
                }
            }
        }
    }
}