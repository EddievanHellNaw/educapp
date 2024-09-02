package com.example.educapp.ui.teacher.attendance

import android.util.Log
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.educapp.R
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
    val isLoading by viewModel.isLoading.collectAsState()
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    LaunchedEffect(key1 = groupId) {
        viewModel.getAttendanceRecordsForGroup(groupId, currentPartial).collect { records ->
            attendanceRecords = records
        }
    }

    Column {
        Text(
            text = if (showEditDialog) "" else "Total Attendance for Partial $currentPartial",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (attendanceRecords.isEmpty()) {
            Text(
                "No attendance records found for this group and partial.",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            if (!showEditDialog) { // Only show AttendanceSummary if not editing
                AttendanceSummary(attendanceRecords, { record ->
                    recordToEdit = record
                    showEditDialog = true
                }, currentPartial)
                Log.d("AttendanceRecords", "AttendanceRecords: $attendanceRecords")
            }
        }
    }

    if (showEditDialog && recordToEdit != null) {
        EditAttendanceView(
            record = recordToEdit!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedRecord ->
                viewModel.updateAttendanceRecord(updatedRecord)
                showEditDialog = false
                snackbarMessage = "Attendance updated successfully!"
                showSnackbar = true
            },
            showSnackbar = { message ->
                snackbarMessage = message
                showSnackbar = true
            }
        )
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
    onSave: (AttendanceRecord) -> Unit,
    showSnackbar: (String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(record.status) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val statusColor by animateColorAsState(
        targetValue = when (selectedStatus) {
            AttendanceStatus.PRESENT -> Color.Green
            AttendanceStatus.LATE -> Color.Yellow
            AttendanceStatus.ABSENT -> Color.Red
            else -> Color.LightGray
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Editing attendance for ${record.student} on ${record.date}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Visual confirmation
        if (selectedStatus != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(64.dp)
                    .background(statusColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedStatus.toString(),
                    color = Color.Black, // Use a contrasting color for text
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AttendanceOption(
                AttendanceStatus.PRESENT,
                Color.Green,
                painterResource(id = R.drawable.present_icon),
                {
                    selectedStatus = AttendanceStatus.PRESENT
                }
            )
            AttendanceOption(
                AttendanceStatus.LATE,
                Color.Yellow,
                painterResource(id = R.drawable.late_icon),
                {
                    selectedStatus = AttendanceStatus.LATE
                }
            )
            AttendanceOption(
                AttendanceStatus.ABSENT,
                Color.Red,
                painterResource(id = R.drawable.absent_icon),
                {
                    selectedStatus = AttendanceStatus.ABSENT
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedStatus != null) {
            Button(
                onClick = { showConfirmationDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }

        if (showConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                title = { Text("Confirm Attendance") },
                text = { Text("Are you sure you want to save the attendance?") },
                confirmButton = {
                    Button(onClick = {
                        if (selectedStatus != null) {
                            onSave(record.copy(status = selectedStatus))
                            showSnackbar("Attendance updated successfully!")
                        }
                        showConfirmationDialog = false
                    }) {
                        Text("Confirm")
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
fun AttendanceSummary(
    attendanceRecords: List<AttendanceRecord>,
    onEditClick: (AttendanceRecord) -> Unit,
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
            val presentCount = summary[AttendanceStatus.PRESENT] ?: 0
            val lateCount = summary[AttendanceStatus.LATE] ?: 0
            val textColor = if (absentCount >= 6) Color.Red else Color.Black

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Student: $student", color = textColor)
                        Text(
                            text = "A: $absentCount, P: $presentCount, L: $lateCount",
                            color = textColor
                        )
                    }

                    if (expanded) {
                        val studentRecords = attendanceRecords.filter { it.student == student }
                        DetailedAttendanceView(studentRecords, currentPartial, onEditClick)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedAttendanceView(
    records: List<AttendanceRecord>,
    currentPartial: Int,
    onEditClick: (AttendanceRecord) -> Unit
) {
    val filteredRecords = records.filter { it.partial == currentPartial }

    Column {
        filteredRecords.forEach { record ->
            val textColor = if (record.status == AttendanceStatus.ABSENT) Color.Red else Color.Black
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = record.date.toString(), color = textColor)
                    Text(text = record.status.toString(), color = textColor)
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            onEditClick(record)
                            expanded = false
                        },
                            text = { Text("Edit") }
                        )
                    }
                }
            }
        }
    }
}