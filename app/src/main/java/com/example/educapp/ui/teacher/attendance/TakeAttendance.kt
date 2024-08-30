package com.example.educapp.ui.teacher.attendance

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.R
import java.time.LocalDate
import kotlin.collections.find
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.isEmpty
import kotlinx.coroutines.delay


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
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    val group = viewModel.groups.find { it.id == groupId }
    val students = group?.students ?: emptyList()

    val attendanceList = remember { mutableStateListOf<AttendanceRecord>() }
    var allStudentsHaveRecord by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = students) {
        if (attendanceList.isEmpty()) {
            students.forEach { student ->
                val existingRecord = attendanceRecords.find {
                    it.student == student && it.date == selectedDate
                }
                if (existingRecord != null) {
                    attendanceList.add(existingRecord)
                } else {
                    attendanceList.add(
                        AttendanceRecord(
                            student = student,
                            groupId = groupId,
                            partial = partial,
                            date = selectedDate,
                            status = null
                        )
                    )
                }
            }
        }
        allStudentsHaveRecord = attendanceList.all { it.status != null }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        DatePickerView(selectedDate) { newDate ->
            selectedDate = newDate
            // Update attendanceList when the date changes
            attendanceList.clear()
            students.forEach { student ->
                val existingRecord = attendanceRecords.find {
                    it.student == student && it.date == selectedDate
                }
                if (existingRecord != null) {
                    attendanceList.add(existingRecord)
                } else {
                    attendanceList.add(
                        AttendanceRecord(
                            student = student,
                            groupId = groupId,
                            partial = partial,
                            date = selectedDate
                        )
                    )
                }
            }
            allStudentsHaveRecord = attendanceList.all { it.status != null }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(attendanceList) { record ->
                StudentItem(
                    student = record.student,
                    status = record.status,
                    onAttendanceStatusChange = { newStatus ->
                        val index = attendanceList.indexOf(record)
                        if (index != -1 && newStatus != null) { // Only update if newStatus is not null
                            attendanceList[index] = record.copy(status = newStatus)
                            viewModel.addOrUpdateAttendanceRecord(attendanceList[index])
                        }
                        allStudentsHaveRecord = attendanceList.all { it.status != null }
                    }
                )
            }
        }

        if (allStudentsHaveRecord) {
            Button(
                onClick = { showConfirmationDialog = true },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            ) {
                Text("Confirm Attendance")
            }
        }

        if (showConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                title = { Text("Confirm Attendance") },
                text = { Text("Are you sure you want to save the attendance?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.saveAttendance(groupId, attendanceList)
                        showConfirmationDialog = false
                        showSnackbar = true
                        navController.popBackStack()
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

        if (showSnackbar) {
            Snackbar(
                action = {
                    TextButton(onClick = { showSnackbar = false }) {
                        Text("OK")
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) { Text("Attendance saved successfully!") }
            LaunchedEffect(key1 = showSnackbar) {
                if (showSnackbar) {
                    delay(9000) // Adjust delay as needed
                    showSnackbar = false
                }
            }
        }
    }
}

@Composable
fun StudentItem(
    student: String,
    status: AttendanceStatus?,
    onAttendanceStatusChange: (AttendanceStatus?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(status) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
    ) {
        Text(text = student, modifier = Modifier.padding(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = when (selectedStatus) {
                        AttendanceStatus.PRESENT -> Color.Green
                        AttendanceStatus.LATE -> Color.Yellow
                        AttendanceStatus.ABSENT -> Color.Red
                        else -> Color.Transparent
                    }
                )
                .padding(8.dp)
        ) {
            if (selectedStatus == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttendanceOption(
                        AttendanceStatus.PRESENT,
                        Color.Green,
                        painterResource(id = R.drawable.present_icon),
                        "Present",
                        {
                            selectedStatus = AttendanceStatus.PRESENT
                            onAttendanceStatusChange(AttendanceStatus.PRESENT)
                        }
                    )
                    AttendanceOption(
                        AttendanceStatus.LATE,
                        Color.Yellow,
                        painterResource(id = R.drawable.late_icon),
                        "Late",
                        {
                            selectedStatus = AttendanceStatus.LATE
                            onAttendanceStatusChange(AttendanceStatus.LATE)
                        }
                    )
                    AttendanceOption(
                        AttendanceStatus.ABSENT,
                        Color.Red,
                        painterResource(id = R.drawable.absent_icon),
                        "Absent",
                        {
                            selectedStatus = AttendanceStatus.ABSENT
                            onAttendanceStatusChange(AttendanceStatus.ABSENT)
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedStatus != null) {
                        Image(
                            painter = when (selectedStatus!!) {
                                AttendanceStatus.PRESENT -> painterResource(id = R.drawable.present_icon)
                                AttendanceStatus.LATE -> painterResource(id = R.drawable.late_icon)
                                AttendanceStatus.ABSENT -> painterResource(id = R.drawable.absent_icon)
                            },
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                    } else {
                        Text(text = "Pending")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedStatus) {
                            AttendanceStatus.PRESENT -> "Present"
                            AttendanceStatus.LATE -> "Late"
                            AttendanceStatus.ABSENT -> "Absent"
                            else -> ""
                        }
                    )
                }
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