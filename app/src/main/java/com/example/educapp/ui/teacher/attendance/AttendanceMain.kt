package com.example.educapp.ui.teacher.attendance

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.navigation.NavHostController
import kotlin.text.isNotBlank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel, navController: NavHostController) {
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var teacherId by remember { mutableStateOf("") }
    val groups = viewModel.groups

    LaunchedEffect(Unit) {
        teacherId = viewModel.getTeacherId()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Groups") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewGroupDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New Group")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn {
                items(groups) { group ->
                    GroupCard(
                        group = group,
                        onEdit = { groupToEdit ->
                            viewModel.updateGroup(groupToEdit, teacherId)
                        },
                        onDelete = { groupToDelete ->
                            viewModel.deleteGroup(groupToDelete, teacherId)
                        },
                        onTakeAttendance = { selectedGroup ->
                            navController.navigate("teacher/take_attendance/${selectedGroup.id}")
                        }
                    )
                }
            }
            if (showNewGroupDialog) {
                NewGroupDialog(viewModel, teacherId, onDismiss = { showNewGroupDialog = false })
            }
        }
    }
}

@Composable
fun GroupCard(
    group: AttendanceGroup,
    onEdit: (AttendanceGroup) -> Unit,
    onDelete: (AttendanceGroup) -> Unit,
    onTakeAttendance: (AttendanceGroup) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onTakeAttendance(group) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = group.name, style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
            }

            if (expanded) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showEditGroupDialog = true
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showDeleteConfirmationDialog = true
                            expanded = false
                        }
                    )
                }
            }
            Text(text = group.schedule, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (showEditGroupDialog) {
        EditGroupDialog(
            group = group,
            onConfirm = { updatedGroup ->
                onEdit(updatedGroup)
                showEditGroupDialog = false
            },
            onDismiss = { showEditGroupDialog = false }
        )
    }

    if (showDeleteConfirmationDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDelete(group)
                showDeleteConfirmationDialog = false
            },
            onDismiss = { showDeleteConfirmationDialog = false }
        )
    }

}

@Composable
fun EditGroupDialog(
    group: AttendanceGroup,
    onConfirm: (AttendanceGroup) -> Unit,
    onDismiss: () -> Unit
) {
    var editedName by remember { mutableStateOf(group.name) }
    var editedSchedule by remember { mutableStateOf(group.schedule) }
    val editedStudents = remember { mutableStateListOf<String>().also { it.addAll(group.students) } }
    var newStudentName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Group") },
        text = {
            Column {
                TextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Name") }
                )
                TextField(
                    value = editedSchedule,
                    onValueChange = { editedSchedule = it },
                    label = { Text("Schedule") }
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        modifier = Modifier.weight(1f),
                        value = newStudentName,
                        onValueChange = { newStudentName = it },
                        label = { Text("New Student Name") }
                    )
                    IconButton(onClick = {
                        if (newStudentName.isNotBlank()) {
                            editedStudents.add(newStudentName)
                            newStudentName = ""
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Student")
                    }
                }
                LazyColumn {
                    items(editedStudents) { student ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(student)
                            IconButton(onClick = { editedStudents.remove(student) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove Student")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                group.name = editedName // Directly modify group properties
                group.schedule = editedSchedule
                group.students = editedStudents
                onConfirm(group) // Pass the modified group object
                onDismiss()
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("This will delete all data for this group. Are you sure?") },
        confirmButton = {
            Button(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}