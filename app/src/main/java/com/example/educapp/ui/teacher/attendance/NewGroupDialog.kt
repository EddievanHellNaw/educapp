package com.example.educapp.ui.teacher.attendance

import android.util.Log
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
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.places.api.model.LocalDate
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


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