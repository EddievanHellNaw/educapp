package com.example.educapp.ui.teacher.attendance

import android.util.Log
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.get
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.collections.addAll
import kotlin.text.clear

data class AttendanceGroup(
    var id: String = "",
    var name: String = "",
    var schedule: String = "",
    var students: List<String> = emptyList(),
    val teacherId: String = ""
)

class AttendanceViewModel : ViewModel() {

    init{
        viewModelScope.launch {
            val currentTeacherId = getCurrentTeacherId()
            fetchGroups(currentTeacherId)
        }
    }


    private val _groups = mutableStateListOf<AttendanceGroup>()
    val groups: List<AttendanceGroup> = _groups

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

    private fun fetchGroups(teacherId: String) {
        viewModelScope.launch {
            val db = Firebase.firestore
            db.collection("groups")
                .whereEqualTo("teacherId", teacherId).get()
                .addOnSuccessListener { querySnapshot ->
                    _groups.clear()
                    for (document in querySnapshot) {
                        val group = document.toObject(AttendanceGroup::class.java)
                        group.id = document.id
                        _groups.add(group)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("AttendanceViewModel", "Error getting groups", e)
                }
        }
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
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupDialog(viewModel: AttendanceViewModel, teacherId: String, onDismiss: () -> Unit) {
    var groupName by remember { mutableStateOf("") }
    var groupSchedule by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }
    val students = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Group") },
        text = {
            Column {
                TextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") }
                )
                TextField(
                    value = groupSchedule,
                    onValueChange = { groupSchedule = it },
                    label = { Text("Schedule") }
                )
                Row (
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        modifier = Modifier.weight(1f),
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Student Name")},
                        placeholder = { Text("Enter one student at a time")}
                    )
                    IconButton(onClick = {
                        if (studentName.isNotBlank()) {
                            students.add(studentName)
                            studentName = ""
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Student")
                    }
                }
                LazyColumn {
                    items(students) { student ->
                        Text(student)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val group = AttendanceGroup(name = groupName, schedule = groupSchedule, students = students)
                Log.d("NewGroupDialog", "Students: $students")
                viewModel.saveGroup(group, teacherId)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}