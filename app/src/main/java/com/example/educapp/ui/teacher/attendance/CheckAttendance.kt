package com.example.educapp.ui.teacher.attendance
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.util.forEach
import androidx.core.util.remove
import androidx.lifecycle.get
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate

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

@Composable
fun CheckScreen(viewModel: AttendanceViewModel, groupId: String, currentPartial: Int) {
    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }

    LaunchedEffect(key1 = groupId) {
        viewModel.getAttendanceRecordsForGroup(groupId).collect { records ->
            attendanceRecords = records
        }
    }

    AttendanceSummary(attendanceRecords.filter { it.partial == currentPartial })
}

@Composable
fun AttendanceSummary(attendanceRecords: List<AttendanceRecord>) {
    var selectedStudent by remember { mutableStateOf<String?>(null) }
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
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = "Student: $student", color = textColor)
                    summary.forEach { (status, count) ->
                        Text(text = "- $status: $count", color = textColor)
                    }

                    // Show details if student is selected
                    if (selectedStudent == student) {
                        val studentRecords = attendanceRecords.filter { it.student == student }
                        studentRecords.forEach { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = record.date.toString())
                                Text(text = record.status.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedAttendanceView(records: List<AttendanceRecord>) {
    LazyColumn {
        items(records) { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = record.date.toString())
                Text(text = record.status.toString())
            }
        }
    }
}