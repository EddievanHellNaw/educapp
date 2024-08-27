package com.example.educapp.ui.teacher.attendance

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.collections.find
import java.util.Calendar

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
    val group = viewModel.groups.find { it.id == groupId }
    val students = group?.students ?: emptyList()
    var attendanceSaved by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { showDatePicker = true }) {
            Text("Select Date: $selectedDate")
        }
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.atStartOfDay(
                    ZoneId.systemDefault()
                ).toInstant().toEpochMilli()
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(onClick = {
                        showDatePicker = false
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = datePickerState.selectedDateMillis!!
                        selectedDate = LocalDate.of(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DAY_OF_MONTH)
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
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (attendanceRecords.size == students.size) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Button(
                    onClick = { showConfirmationDialog = true },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text("Confirm Attendance")
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(students.size) { index ->
                val student = students[index]
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
                        timestamp = com.google.firebase.Timestamp(
                            selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant()
                                .toEpochMilli() / 1000, 0
                        )
                    )
                    attendanceRecords.add(record)
                }
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
                        showConfirmationDialog = false
                        attendanceSaved = true
                        coroutineScope.launch {
                            viewModel.saveAttendance(groupId, attendanceRecords)
                            delay(Duration.ofMillis(1000))
                            Toast.makeText(context, "Attendance saved!", Toast.LENGTH_SHORT).show()
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
        if (attendanceSaved) { // Only show the button if attendance is confirmed
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Go Back")
            }
        }
    }
}

@Composable
fun StudentItem(student: String, onAttendanceRecorded: (AttendanceStatus) -> Unit) {
    var selectedStatus by remember { mutableStateOf<AttendanceStatus?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
            .background(
                color = when (selectedStatus) {
                    AttendanceStatus.PRESENT -> Color.Green
                    AttendanceStatus.LATE -> Color.Yellow
                    AttendanceStatus.ABSENT -> Color.Red
                    else -> Color.Transparent
                }
            )
    ) {
        Text(text = student, modifier = Modifier.padding(8.dp))

        if (selectedStatus == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttendanceOption(
                    AttendanceStatus.PRESENT,
                    Color.Green,
                    painterResource(id = R.drawable.present_icon),
                    "Present",
                    {
                        selectedStatus = AttendanceStatus.PRESENT
                        onAttendanceRecorded(AttendanceStatus.PRESENT)
                    }
                )
                AttendanceOption(
                    AttendanceStatus.LATE,
                    Color.Yellow,
                    painterResource(id = R.drawable.late_icon),
                    "Late",
                    {
                        selectedStatus = AttendanceStatus.LATE
                        onAttendanceRecorded(AttendanceStatus.LATE)
                    }
                )
                AttendanceOption(
                    AttendanceStatus.ABSENT,
                    Color.Red,
                    painterResource(id = R.drawable.absent_icon),
                    "Absent",
                    {
                        selectedStatus = AttendanceStatus.ABSENT
                        onAttendanceRecorded(AttendanceStatus.ABSENT)
                    }
                )
            }
        }
    }
}

@Composable
fun AttendanceOption(
    status: AttendanceStatus,
    color: Color,
    image: Painter,
    text: String,
    onClick: () -> Unit
) {
    var isClicked by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isClicked = !isClicked
            onClick()
        },
        modifier = Modifier
            .padding(4.dp)
            .size(width = 90.dp, height = 70.dp), // Increased height for image and text
        colors = ButtonDefaults.buttonColors(containerColor = if (isClicked) darkenColor(color) else color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = image,
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}


fun darkenColor(color: Color): Color {
    return color.copy(alpha = 0.8f)
}