package com.example.educapp.ui.teacher.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckScreen(
    viewModel: AttendanceViewModel,
    groupId: String,
    currentPartial: Int
) {
    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var expandedStudent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key1 = groupId) {
        viewModel.getAttendanceRecordsForGroup(groupId, currentPartial).collect { records ->
            attendanceRecords = records
        }
    }

    Column {
        Text(
            text = "Total Attendance for Partial $currentPartial",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        LazyColumn {
            items(attendanceRecords) { record ->
                StudentItem(
                    student = record.student,
                    onClick = {
                        expandedStudent = if (expandedStudent == record.student) null else record.student
                    }
                )
                if (expandedStudent == record.student) {
                    DetailedAttendanceView(
                        records = attendanceRecords.filter {
                            it.student == record.student && it.partial == currentPartial
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StudentItem(student: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        Text(text = student, modifier = Modifier.padding(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerView(selectedDate: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    onDateChange(LocalDate.ofInstant(
                        Instant.ofEpochMilli(datePickerState.selectedDateMillis!!),
                        ZoneId.systemDefault()
                    ))
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
            DatePicker(state = datePickerState)
        }
    }

    Button(onClick = { showDatePicker = true }) {
        Text("Select Date: $selectedDate")
    }
}

@Composable
fun EditAttendanceView(
    record: AttendanceRecord,
    onDismiss: () -> Unit,
    onSave: (AttendanceRecord) -> Unit
) {
    var editedRecord by remember { mutableStateOf(record) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Edit Attendance for ${record.student}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        StudentItem(record.student, editedRecord.status) { status ->
            if (status != null) {
                editedRecord = editedRecord.copy(status = status)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(onClick = { onSave(editedRecord) }) {
                Text("Save")
            }
        }
    }
}

@Composable
fun AttendanceSummary(
    attendanceRecords: List<AttendanceRecord>,
    onEditClick: (AttendanceRecord) -> Unit
) {
    var selectedStudent by remember { mutableStateOf<String?>(null) }
    var expandedStudent by remember { mutableStateOf<String?>(null) }
    val attendanceSummary = attendanceRecords.groupBy { it.student }
        .mapValues { (_, records) ->
            records.groupingBy { it.status }.eachCount()
        }

    LazyColumn {
        items(attendanceSummary.entries.toList()) { (student, summary) ->
            val absentCount = summary[AttendanceStatus.ABSENT] ?: 0
            val textColor = if (absentCount >= 6) Color.Red else Color.Black
            val boxColor = if (absentCount >= 6) Color.Red.copy(alpha = 0.1f) else Color.Transparent

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .background(boxColor)
                    .clickable {
                        selectedStudent = if (selectedStudent == student) null else student
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Student: $student", color = textColor)

                    Box {
                        IconButton(onClick = { expandedStudent = student }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = expandedStudent == student,
                            onDismissRequest = { expandedStudent = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    val recordToEdit = attendanceRecords.find { it.student == student }
                                    if (recordToEdit != null) {
                                        onEditClick(recordToEdit)
                                    }
                                    expandedStudent = null
                                }
                            )
                        }
                    }
                }

                summary.forEach { (status, count) ->
                    Text(text = "- $status: $count", color = textColor)
                }

                if (selectedStudent == student) {
                    val studentRecords = attendanceRecords.filter { it.student == student }
                    DetailedAttendanceView(studentRecords)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DetailedAttendanceView(records: List<AttendanceRecord>) {
    val filteredRecords = records
        .groupBy { it.date }
        .flatMap { entry ->
            entry.value.sortedByDescending { it.timestamp.toDate() }.take(1)
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        filteredRecords.forEach { record ->
            val textColor = if (record.status == AttendanceStatus.ABSENT) Color.Red else Color.Black
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = record.date.toString(), color = textColor)
                Text(text = record.status.toString(), color = textColor)
            }
        }
    }
}