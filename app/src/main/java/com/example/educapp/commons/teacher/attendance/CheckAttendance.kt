package com.example.educapp.commons.teacher.attendance

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.R
import com.example.educapp.commons.ui.FrostedBox
import com.example.educapp.commons.ui.GradientCard
import com.example.educapp.commons.ui.HapticButton
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
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
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
            AttendanceStatus.PRESENT -> Color(0xFF388E3C)
            AttendanceStatus.LATE -> Color(0xFFFBC02D)
            AttendanceStatus.ABSENT -> Color(0xFFD32F2F)
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
            style = MaterialTheme.typography.headlineMedium,
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
                    color = MaterialTheme.colorScheme.onPrimary, // Use a contrasting color for text
                    style = MaterialTheme.typography.bodyLarge
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
                Color(0xFF388E3C),
                painterResource(id = R.drawable.present_icon),
                {
                    selectedStatus = AttendanceStatus.PRESENT
                }
            )
            AttendanceOption(
                AttendanceStatus.LATE,
                Color(0xFFFBC02D),
                painterResource(id = R.drawable.late_icon),
                {
                    selectedStatus = AttendanceStatus.LATE
                }
            )
            AttendanceOption(
                AttendanceStatus.ABSENT,
                Color(0xFFD32F2F),
                painterResource(id = R.drawable.absent_icon),
                {
                    selectedStatus = AttendanceStatus.ABSENT
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedStatus != null) {
            HapticButton (
                onClick = { showConfirmationDialog = true },
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
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

        Spacer(modifier = Modifier.padding(8.dp))

        HapticButton (
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
                .width(200.dp)
                .height(48.dp),
        ) {
            Text("Cancel")
        }

        if (showConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                title = { Text("Confirm Attendance",style = MaterialTheme.typography.headlineSmall) },
                text = { Text("Are you sure you want to save the attendance?",style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    HapticButton (onClick = {
                        if (selectedStatus != null) {
                            onSave(record.copy(status = selectedStatus))
                            showSnackbar("Attendance updated successfully!")
                        }
                        showConfirmationDialog = false
                    }, modifier = Modifier
                        .width(200.dp)
                        .height(48.dp)) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    HapticButton (onClick = { showConfirmationDialog = false },
                        modifier = Modifier
                        .width(200.dp)
                        .height(48.dp)) {
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
    val sortedSummary = attendanceSummary
        .entries
        .sortedBy { it.key }

    LazyColumn {
        items(sortedSummary) { (student, summary) ->
            var expanded by remember { mutableStateOf(false) }
            val absentCount = summary[AttendanceStatus.ABSENT] ?: 0
            val presentCount = summary[AttendanceStatus.PRESENT] ?: 0
            val lateCount = summary[AttendanceStatus.LATE] ?: 0

            // Determine gradient color based on absence count
            val gradientColor = when {
                absentCount > 6 -> MaterialTheme.colorScheme.error
                absentCount >= 4 -> MaterialTheme.colorScheme.onError
                else -> MaterialTheme.colorScheme.primary
            }

            GradientCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                gradientBrush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        gradientColor
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Student name
                    Text(
                        text = student,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Attendance counts in FrostedBox
                    FrostedBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth())
                        {
                            AttendanceIconCount(
                                icon = R.drawable.present_icon,
                                count = presentCount,
                                color = Color(0xFF388E3C)
                            )
                            AttendanceIconCount(
                                icon = R.drawable.late_icon,
                                count = lateCount,
                                color = Color(0xFFFBC02D)
                            )
                            AttendanceIconCount(
                                icon = R.drawable.absent_icon,
                                count = absentCount,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }

                    ExpandButton(
                        expanded = expanded,
                        onClick = { expanded = !expanded }
                    )

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
    val sortedRecords = filteredRecords.sortedByDescending { it.date }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        sortedRecords.forEach { record ->
            val statusColor = when (record.status) {
                AttendanceStatus.PRESENT -> Color(0xFF388E3C)
                AttendanceStatus.LATE -> Color(0xFFFBC02D)
                AttendanceStatus.ABSENT -> Color(0xFFD32F2F)
                null -> MaterialTheme.colorScheme.onSurface
            }

            GradientCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.status?.name ?: "UNKNOWN",
                            style = MaterialTheme.typography.headlineSmall,
                            color = statusColor.copy(alpha = 0.8f)
                        )
                        Text(
                            text = record.date.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )

                    }

                    IconButton(
                        onClick = { onEditClick(record) },
                        modifier = Modifier.size(50.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Edit attendance",
                            modifier = Modifier.size(50.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandButton(expanded: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    FrostedBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        IconButton(
            onClick =  onClick,
            modifier = Modifier
                .border(1.dp, Color.Gray, CircleShape)  // Adds an elegant outline
                .background(Color.Transparent)  // Keeps it sleek
                .size(20.dp)  // Adjust size

        ) {
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = "Expand",
                tint = Color.White,
                modifier = Modifier.rotate(rotation)
            )
        }

    }
}

@Composable
fun AttendanceIconCount(
    icon: Int,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
