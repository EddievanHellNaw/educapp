package com.example.educapp.ui.teacher.attendance

import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.delay
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(viewModel: AttendanceViewModel, groupId: String, navController: NavHostController) {
    val group = viewModel.groups.find { it.id == groupId }
    val groupName = group?.name ?: ""

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Take Attendance for $groupName") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn {
                items(3) { partial ->
                    PartialCard(
                        partial = partial + 1,
                        groupName = groupName,
                        onAttendanceClick = {
                            navController.navigate("attendance/$groupId/${partial + 1}")
                        },
                        onCheckClick = {
                            navController.navigate("check/$groupId/${partial + 1}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PartialCard(
    partial: Int,
    groupName: String,
    onAttendanceClick: () -> Unit,
    onCheckClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onAttendanceClick() } // Assuming onAttendanceClick is the primary action
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Partial $partial", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = onCheckClick) {
                    Text("Check Attendance")
                }
            }
            Text(text = groupName, style = MaterialTheme.typography.bodyMedium)
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
    var showGoBackButton by remember { mutableStateOf(false) }
    val group = viewModel.groups.find { it.id == groupId }
    val students = group?.students ?: emptyList()
    val isLoading by viewModel.isLoading.collectAsState()
    val attendanceList = remember { mutableStateListOf<AttendanceRecord>() }
    var allStudentsHaveRecord by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("Confirm Attendance") }

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

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() }
            )
        },
        bottomBar = {
            if (allStudentsHaveRecord) {
                Button(
                    onClick = { if (buttonText == "Confirm Attendance") {
                        showConfirmationDialog = true
                        } else {
                        navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(buttonText)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            DatePickerView(selectedDate) { newDate ->
                Log.d("TakeAttendanceDetailsScreen", "Selected date: $newDate")
                selectedDate = newDate
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
                            Timber.tag("StudentItem")
                                .d("New status: $newStatus, Date: $selectedDate")
                            val index = attendanceList.indexOf(record)
                            if (index != -1 && newStatus != null) {
                                attendanceList[index] = record.copy(status = newStatus)
                                viewModel.addOrUpdateAttendanceRecord(attendanceList[index])
                            }
                            allStudentsHaveRecord = attendanceList.all { it.status != null }
                        }
                    )
                }
            }

            if (showConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = { Text("Confirm Attendance") },
                    text = { Text("Are you sure you want to save the attendance?") },
                    confirmButton = {
                        if (!showGoBackButton) {
                            Button(onClick = {
                                viewModel.saveAttendance(groupId, attendanceList)
                                showConfirmationDialog = false
                                showGoBackButton = true
                                buttonText = "Go Back"
                                showSnackbar = true
                            }) {
                                Text("Confirm")
                            }
                        } else {
                            Button(onClick = {
                                navController.popBackStack()
                                showConfirmationDialog = false
                                showGoBackButton = false
                            }) {
                                Text("Go Back")
                            }
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
                        delay(9000)
                        showSnackbar = false
                    }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
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
                            {
                                selectedStatus = AttendanceStatus.PRESENT
                                onAttendanceStatusChange(AttendanceStatus.PRESENT)
                            }
                        )
                        AttendanceOption(
                            AttendanceStatus.LATE,
                            Color.Yellow,
                            painterResource(id = R.drawable.late_icon),
                            {
                                selectedStatus = AttendanceStatus.LATE
                                onAttendanceStatusChange(AttendanceStatus.LATE)
                            }
                        )
                        AttendanceOption(
                            AttendanceStatus.ABSENT,
                            Color.Red,
                            painterResource(id = R.drawable.absent_icon),
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
}


@Composable
fun AttendanceOption(
    status: AttendanceStatus,
    color: Color,
    image: Painter,
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
        }
    }
}


fun darkenColor(color: Color): Color {
    return color.copy(alpha = 0.8f)
}