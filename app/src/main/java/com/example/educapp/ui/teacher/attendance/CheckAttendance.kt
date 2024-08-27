package com.example.educapp.ui.teacher.attendance

import android.os.Build
import androidx.annotation.RequiresApi
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
import java.time.format.DateTimeFormatter

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE
}

data class AttendanceRecord(
    val student: String = "",
    val groupId: String = "",
    val partial: Int = 0,
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val date: LocalDate = LocalDate.now()
)




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckScreen(
    viewModel: AttendanceViewModel,
    groupId: String,
    currentPartial: Int
) {
    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var isEditing by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(key1 = groupId) {
        viewModel.getAttendanceRecordsForGroup(groupId).collect { records ->
            attendanceRecords = records
        }
    }
    Column{
        Text(text = "Total Attendance for Partial $currentPartial",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp))

    if (isEditing && selectedRecord != null) {
        EditAttendanceView(
            record = selectedRecord!!,
            onDismiss = { isEditing = false },
            onSave = { selectedDate, updatedStatus ->
                val updatedRecord = selectedRecord!!.copy(
                    status = updatedStatus,
                    date = selectedDate
                )
                viewModel.updateAttendanceRecord(updatedRecord)
                isEditing = false
            }
        )
    } else {

            AttendanceSummary(
                attendanceRecords.filter { it.partial == currentPartial},
                onEditClick = { record ->
                    selectedRecord = record
                    isEditing = true
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerView(selectedDate: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    showDatePicker = false
                    onDateChange(
                        LocalDate.ofInstant(
                            Instant.ofEpochMilli(datePickerState.selectedDateMillis!!),
                            ZoneId.of("UTC")
                        )
                    )
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
    onSave: (LocalDate, AttendanceStatus) -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedStatus by remember { mutableStateOf(record.status) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

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

        // Date picker
        DatePickerView(selectedDate) { newDate ->
            selectedDate = newDate
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Student item section (You'll need to implement this)
        StudentItem(record.student) { status ->
            selectedStatus = status
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(onClick = { onSave(selectedDate, selectedStatus) }) {
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
    Column(modifier = Modifier.fillMaxWidth()) {
        records.forEach { record ->
            val date = record.timestamp.toDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = date.toString())
                Text(text = record.status.toString())
            }
        }
    }
}
