package com.example.educapp.ui.teacher.attendance

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

data class AttendanceGroup(
    var id: String = "",
    var name: String = "",
    var schedule: String = "",
    var students: List<String> = emptyList(),
    val teacherId: String = ""
)

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE
}

data class AttendanceRecord(
    val student: String = "",
    val groupId: String = "",
    val partial: Int = 0,
    val status: AttendanceStatus? = null,
    val date: LocalDate = LocalDate.now(),
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
)
@HiltViewModel
class AttendanceViewModel @Inject constructor() : ViewModel() {

    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val _groups = mutableStateListOf<AttendanceGroup>()
    val groups: List<AttendanceGroup> = _groups
    private val _attendanceRecordsFlow = MutableSharedFlow<List<AttendanceRecord>>()
    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    private var groupsListenerRegistration: ListenerRegistration? = null

    init{
        viewModelScope.launch {
            val currentTeacherId = getCurrentTeacherId()
            startGroupsListener(currentTeacherId)
        }
    }

    fun startGroupsListener(teacherId: String) {
        viewModelScope.launch {
            val db = Firebase.firestore
            groupsListenerRegistration = db.collection("groups")
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener { querySnapshot, e ->
                    if (e != null) {
                        Log.w("AttendanceViewModel", "Error getting groups", e)
                        return@addSnapshotListener
                    }

                    _groups.clear()
                    for (document in querySnapshot!!) {
                        val group = document.toObject(AttendanceGroup::class.java)
                        group.id = document.id
                        _groups.add(group)
                    }
                }
        }
    }

    fun stopGroupsListener() {
        groupsListenerRegistration?.remove()
        groupsListenerRegistration = null
    }

    private suspend fun getCurrentTeacherId(): String {
        val auth = Firebase.auth
        val db = Firebase.firestore
        val user = auth.currentUser
        return if (user != null) {
            try {
                val userDoc = db.collection("users").document(user.uid).get().await()
                userDoc.getString("teacherId") ?: ""
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error getting teacherId", e)
                ""
            }
        } else {
            ""
        }
    }

    suspend fun getTeacherId(): String {
        return getCurrentTeacherId()
    }

    fun saveGroup(group: AttendanceGroup, teacherId: String) {
        viewModelScope.launch {
            val db = Firebase.firestore
            val data = mapOf(
                "name" to group.name,
                "schedule" to group.schedule,
                "students" to group.students,
                "teacherId" to teacherId
            )
            db.collection("groups").add(data)
                .addOnSuccessListener { documentReference ->
                    Log.d("AttendanceViewModel", "Group added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w("AttendanceViewModel", "Error adding group", e)
                }
        }
    }

    fun updateGroup(group: AttendanceGroup, currentTeacherId: String) {
        viewModelScope.launch {
            val db = Firebase.firestore
            try {
                if (group.teacherId == currentTeacherId) {
                    val querySnapshot = db.collection("groups").whereEqualTo("name", group.name).get().await()
                    if (querySnapshot.documents.isNotEmpty()) { // Check if documents exist
                        val groupRef = db.collection("groups").document(group.id)
                        groupRef.update(
                            mapOf(
                                "name" to group.name,
                                "schedule" to group.schedule,
                                "students" to group.students
                            )
                        ).await()

                        // Update the group in _groups list
                        val index = _groups.indexOfFirst { it.id == group.id }
                        if (index != -1) {
                            _groups[index] = group
                        }
                    } else {
                        // Handle empty snapshot (e.g., show a message to the user)
                        Log.w("AttendanceViewModel", "No group found with name: ${group.name}")
                    }
                } else {
                    Log.w("AttendanceViewModel", "Current teacher ID does not match the group's teacher ID")
                }
            } catch (e: Exception) {
                Log.w("AttendanceViewModel", "Error updating group", e)
                // Handle error, e.g., show a Snackbar
            }
            refreshGroups(currentTeacherId)
        }
    }
    fun deleteGroup(group: AttendanceGroup, currentTeacherId: String) {
        viewModelScope.launch {
            val db = Firebase.firestore
            try {
                if(group.teacherId == currentTeacherId) {
                    val groupRef =
                        db.collection("groups").document(group.id).delete().await() // Use document ID
                    _groups.remove(group)
                }else {
                    Log.w("AttendanceViewModel", "Current teacher ID does not match the group's teacher ID")
                }
            } catch (e: Exception) {
                Log.w("AttendanceViewModel", "Error deleting group", e)
                // Handle error, e.g., show a Snackbar
            }
        }
    }

    fun saveAttendance(groupId: String, attendanceRecords: List<AttendanceRecord>) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = Firebase.firestore
            try {
                val batches = attendanceRecords.chunked(500)
                batches.forEach { batch ->
                    if (!isActive) return@forEach
                    val batchOperation = db.batch()
                    batch.forEach { record ->
                        val query = db.collection("attendance")
                            .whereEqualTo("student", record.student)
                            .whereEqualTo("groupId", record.groupId)
                            .whereEqualTo("partial", record.partial)
                            .whereEqualTo("date", record.date)
                        val existingRecord = query.get().await().documents.firstOrNull()
                        if (existingRecord != null) {
                            batchOperation.update(
                                db.collection("attendance").document(existingRecord.id),
                                "status",
                                record.status
                            )
                        } else {
                            batchOperation.set(db.collection("attendance").document(), record)
                        }
                    }
                    batchOperation.commit().await()
                }
                // Refresh attendance records from Firestore
                val updatedRecords = db.collection("attendance")
                    .whereEqualTo("groupId", groupId)
                    .get().await().documents.map { it.toAttendanceRecord() }
                _attendanceRecords.value = updatedRecords
                Log.d("AttendanceViewModel", "Attendance saved successfully")
            } catch (e: Exception) {
                Log.w("AttendanceViewModel", "Error saving attendance", e)
                // Handle the exception
            }
        }
    }


    fun getAttendanceRecordsForGroup(groupId: String, partial: Int): Flow<List<AttendanceRecord>> = flow {
        val db = Firebase.firestore
        try {
            val attendanceRecords = db.collection("attendance")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("partial", partial) // Add filter for partial
                .get().await().documents.map { it.toAttendanceRecord() }
            emit(attendanceRecords) // Emit all records for the group and partial
        } catch (e: Exception) {
            Log.w("AttendanceViewModel", "Error getting attendance records", e)
            emit(emptyList())
        }
    }

    fun updateAttendanceRecord(updatedRecord: AttendanceRecord) {
        viewModelScope.launch {
            try {
                val db = Firebase.firestore
                val querySnapshot = db.collection("attendance")
                    .whereEqualTo("student", updatedRecord.student)
                    .whereEqualTo("groupId", updatedRecord.groupId)
                    .whereEqualTo("partial", updatedRecord.partial)
                    .whereEqualTo("date", updatedRecord.date)
                    .get()
                    .await()

                if (querySnapshot.documents.isNotEmpty()) {
                    val recordRef = db.collection("attendance").document(querySnapshot.documents.first().id)
                    recordRef.update("status", updatedRecord.status).await()

                    // Update _attendanceRecords StateFlow
                    _attendanceRecords.value = _attendanceRecords.value.map {
                        if (it.student == updatedRecord.student && it.date == updatedRecord.date) updatedRecord else it
                    }
                } else {
                    Log.w("AttendanceViewModel", "No attendance record found for update")
                }
            } catch (e: Exception) {
                Log.w("AttendanceViewModel", "Error updating attendance record", e)
            }
        }
    }

    fun addOrUpdateAttendanceRecord(record: AttendanceRecord) {
        _attendanceRecords.value = _attendanceRecords.value.map {
            if (it.student == record.student && it.groupId == record.groupId && it.partial == record.partial && it.date == record.date) {
                record.copy(timestamp = com.google.firebase.Timestamp.now()) // Update only the timestamp
            } else {
                it
            }
        }
    }

    fun refreshGroups(teacherId: String) {
        viewModelScope.launch {
            // Same code as fetchGroups but without _groups.clear()
            val db = Firebase.firestore
            db.collection("groups")
                .whereEqualTo("teacherId", teacherId).get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot) {
                        val group = document.toObject(AttendanceGroup::class.java)
                        group.id = document.id
                        // Update existing group or add new one
                        val index = _groups.indexOfFirst { it.id == group.id }
                        if (index != -1) {
                            _groups[index] = group
                        } else {
                            _groups.add(group)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("AttendanceViewModel", "Error getting groups", e)
                }
        }
    }

    // Helper function to convert a Firestore document to AttendanceRecord
    private fun DocumentSnapshot.toAttendanceRecord(): AttendanceRecord {
        val statusString = getString("status")
        val status = when (statusString) {
            "PRESENT" -> AttendanceStatus.PRESENT
            "ABSENT" -> AttendanceStatus.ABSENT
            "LATE" -> AttendanceStatus.LATE
            else -> null // Handle the case where status is null or unknown
        }
        return AttendanceRecord(
            student = getString("student") ?: "",
            groupId = getString("groupId") ?: "",
            partial = getLong("partial")?.toInt() ?: 0,
            status = status, // Use the status obtained from the when expression
            timestamp = getTimestamp("timestamp")!!
        )
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel() // Cancel the SupervisorJob when ViewModel is cleared
    }
}