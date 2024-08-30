package com.example.educapp.ui.teacher.attendance

import android.util.Log
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
import androidx.navigation.NavHostController
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId



@Composable
fun CheckScreen(
    viewModel: AttendanceViewModel,
    groupId: String,
    currentPartial: Int,
    navController: NavHostController
) {
    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<AttendanceRecord?>(null) }

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

        AttendanceSummary(attendanceRecords, { studentName ->
            recordToEdit = attendanceRecords.find { it.student == studentName }
            showEditDialog = true
        }, currentPartial)
        Log.d("AttendanceRecords", "AttendanceRecords: $attendanceRecords")
    }

    if (showEditDialog && recordToEdit != null) {
        EditAttendanceView(
            record = recordToEdit!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedRecord ->
                viewModel.updateAttendanceRecord(updatedRecord)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun AttendanceSummary(
    attendanceRecords: List<AttendanceRecord>,
    onEditClick: (String) -> Unit, // Changed to accept student name
    currentPartial: Int
) {
    val attendanceSummary = attendanceRecords.groupBy { it.student }
        .mapValues { (_, records) ->
            records.groupingBy { it.status }.eachCount()
        }

    LazyColumn {
        items(attendanceSummary.entries.toList()) { (student, summary) ->
            var expanded by remember { mutableStateOf(false) }
            val absentCount = summary[AttendanceStatus.ABSENT] ?: 0
            val textColor = if (absentCount >= 6) Color.Red else Color.Black
            val boxColor = if (absentCount >= 6) Color.Red.copy(alpha = 0.1f) else Color.Transparent

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .background(boxColor)
                    .clickable { expanded = !expanded }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onEditClick(student) },
                horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Student: $student", color = textColor)
                    Text(text = "Absent: $absentCount", color = textColor)
                }

                if (expanded) {
                    val studentRecords = attendanceRecords.filter { it.student == student }
                    DetailedAttendanceView(studentRecords, currentPartial)
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
fun DetailedAttendanceView(records: List<AttendanceRecord>, currentPartial: Int) {
    val filteredRecords = records.filter { it.partial == currentPartial }

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