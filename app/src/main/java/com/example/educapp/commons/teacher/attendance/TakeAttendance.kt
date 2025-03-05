package com.example.educapp.commons.teacher.attendance

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.R
import java.time.LocalDate
import kotlin.collections.find
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Brush
import com.example.educapp.commons.ui.GradientCard
import com.example.educapp.commons.ui.HapticButton
import com.example.educapp.commons.ui.hapticClickable
import kotlinx.coroutines.delay
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(viewModel: AttendanceViewModel, groupId: String, navController: NavHostController) {
    val group = viewModel.groups.find { it.id == groupId }
    val groupName = group?.name ?: ""

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Take Attendance for $groupName", style = MaterialTheme.typography.headlineSmall) })
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

    val buttonColor = MaterialTheme.colorScheme.primary
    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .hapticClickable { onAttendanceClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface, // left color
                            buttonColor                        // right color
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top row: partial text (left half), button (right half)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Partial text occupies left half
                    Text(
                        text = "Partial $partial",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    // HapticButton occupies right half
                    HapticButton(
                        onClick = onCheckClick,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text(text = "Review",style = MaterialTheme.typography.bodyMedium)
                    }
                }
                // Below row: group name
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
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
                HapticButton(
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
                        Text(buttonText, style = MaterialTheme.typography.bodySmall)
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
                    title = { Text("Confirm Attendance",style = MaterialTheme.typography.headlineSmall) },
                    text = { Text("Are you sure you want to save the attendance?",style = MaterialTheme.typography.bodyMedium) },
                    confirmButton = {
                        if (!showGoBackButton) {
                            HapticButton(onClick = {
                                viewModel.saveAttendance(groupId, attendanceList)
                                showConfirmationDialog = false
                                showGoBackButton = true
                                buttonText = "Go Back"
                                showSnackbar = true
                            }) {
                                Text("Confirm", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            HapticButton(onClick = {
                                navController.popBackStack()
                                showConfirmationDialog = false
                                showGoBackButton = false
                            }) {
                                Text("Go Back",style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    },
                    dismissButton = {
                        HapticButton(onClick = { showConfirmationDialog = false }) {
                            Text("Cancel",style = MaterialTheme.typography.bodySmall)
                        }
                    }
                )
            }

            if (showSnackbar) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("OK",style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) { Text("Attendance saved successfully!",style = MaterialTheme.typography.bodyMedium) }
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

    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = student,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = when (selectedStatus) {
                            AttendanceStatus.PRESENT -> Color(0xFF388E3C)
                            AttendanceStatus.LATE -> Color(0xFFFBC02D)
                            AttendanceStatus.ABSENT -> Color(0xFFD32F2F)
                            else -> Color.Transparent
                        },
                        shape = RoundedCornerShape(8.dp)
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
                            Color(0xFF388E3C),
                            painterResource(id = R.drawable.present_icon),
                            {
                                selectedStatus = AttendanceStatus.PRESENT
                                onAttendanceStatusChange(AttendanceStatus.PRESENT)
                            }
                        )
                        AttendanceOption(
                            AttendanceStatus.LATE,
                            Color(0xFFFBC02D),
                            painterResource(id = R.drawable.late_icon),
                            {
                                selectedStatus = AttendanceStatus.LATE
                                onAttendanceStatusChange(AttendanceStatus.LATE)
                            }
                        )
                        AttendanceOption(
                            AttendanceStatus.ABSENT,
                            Color(0xFFD32F2F),
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
                            Text(text = "Pending", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedStatus) {
                                AttendanceStatus.PRESENT -> "Present"
                                AttendanceStatus.LATE -> "Late"
                                AttendanceStatus.ABSENT -> "Absent"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium
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

    HapticButton(
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