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
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
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


enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE
}
data class AttendanceRecord(
    val student: String,
    val partial: Int,
    val status: AttendanceStatus
)

@Composable
fun TakeAttendanceScreen(viewModel: AttendanceViewModel, groupId: String) {
    val group = viewModel.groups.find { it.id == groupId } // Find group by ID
    val groupName = group?.name ?: "" // Get group name or empty string if not found


    val attendanceRecords = remember { mutableStateListOf<AttendanceRecord>() }
    var currentPartial by remember { mutableStateOf(1) }
    val totalPartials = 3 // Example: 3 partials
    var showStudentList by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // Add padding for better visual spacing
    ) {
        Text(
            text = "Take Attendance for $groupName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Partial boxes
        for (i in 1..3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .clickable {
                        currentPartial = i
                        showStudentList = true
                    }
            ) {
                Text(
                    text = "Partial $i",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        // Student list (displayed when showStudentList is true)
        if (showStudentList) {
            StudentList(group, currentPartial) { student, status ->
                val record = AttendanceRecord(student, currentPartial, status)
                attendanceRecords.add(record)
                if (attendanceRecords.size == group?.students?.size) {
                    showStudentList = false
                    if (currentPartial < totalPartials) {
                        currentPartial++
                    } else {
                        showSummary = true
                    }
                }
            }
        }
        if (showSummary) {
            AttendanceSummary(attendanceRecords)
        }
    }
}

@Composable
fun StudentList(group: AttendanceGroup?, currentPartial: Int, onAttendanceRecorded: (String, AttendanceStatus) -> Unit)  {
    val students = group?.students ?: emptyList()

    LazyColumn {
        items(students) { student ->
            StudentItem(student, currentPartial, onAttendanceRecorded)
        }
    }
}

@Composable
fun StudentItem(student: String, currentPartial: Int, onAttendanceRecorded: (String, AttendanceStatus) -> Unit) {
    var attendanceStatus by remember { mutableStateOf<AttendanceStatus?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = student)
        Spacer(modifier = Modifier.weight(1f))
        AttendanceOption(AttendanceStatus.PRESENT, Color.Green) {
            attendanceStatus = AttendanceStatus.PRESENT
        }
        AttendanceOption(AttendanceStatus.ABSENT, Color.Red) {
            attendanceStatus = AttendanceStatus.ABSENT
        }
        AttendanceOption(AttendanceStatus.LATE, Color.Yellow) {
            attendanceStatus = AttendanceStatus.LATE
        }
    }

    // Store attendance status for student and partial (implementation needed)
    LaunchedEffect(key1 = attendanceStatus) {
        if (attendanceStatus != null) {
            onAttendanceRecorded(student, attendanceStatus!!)
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
            Text(text = "Student: $student")
            summary.forEach { (status, count) ->
                Text(text = "- $status: $count")
            }
        }
    }
}