package com.example.educapp.ui.teacher.attendance
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.util.forEach
import androidx.core.util.remove
import androidx.lifecycle.get
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE
}

data class AttendanceRecord(
    val student: String,
    val groupId: String,
    val partial: Int,
    val status: AttendanceStatus,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun CheckScreen(viewModel: AttendanceViewModel, groupId: String, currentPartial: Int) {
    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }

    val db = Firebase.firestore
    DisposableEffect(key1 = groupId) {
        val listener = db.collection("groups").document(groupId).collection("attendance")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                val records = mutableListOf<AttendanceRecord>()
                snapshot?.documents?.forEach { attendanceDocument ->
                    val student = attendanceDocument.id
                    db.collection("groups").document(groupId)
                        .collection("attendance").document(student).collection("records")
                        .get()
                        .addOnSuccessListener { recordDocuments ->
                            for (recordDocument in recordDocuments) {
                                val attendanceRecord = recordDocument.toObject(AttendanceRecord::class.java)
                                records.add(attendanceRecord)
                            }
                            attendanceRecords = records
                        }
                        .addOnFailureListener { exception ->
                            // Handle error
                        }
                }
            }

        onDispose {
            listener.remove()
        }
    }

    AttendanceSummary(attendanceRecords.filter { it.partial == currentPartial })
}

@Composable
fun AttendanceSummary(attendanceRecords: List<AttendanceRecord>) {
    Log.d("AttendanceSummary", "Received attendance records: $attendanceRecords")
    val attendanceSummary = attendanceRecords.groupBy { it.student }
        .mapValues { (_, records) ->
            records.groupingBy { it.status }.eachCount()
        }

    LazyColumn {
        items(attendanceSummary.entries.toList()) { (student, summary) ->
            Log.d("AttendanceSummary", "Student: $student, Summary: $summary")
            val absentCount = summary[AttendanceStatus.ABSENT] ?: 0
            val textColor = if (absentCount >= 6) Color.Red else Color.Black
            Column {
                Text(text = "Student: $student", color = textColor)
                summary.forEach { (status, count) ->
                    Text(text = "- $status: $count", color = textColor)
                }
            }
        }
    }
}