package com.example.educapp.commons.teacher.attendance

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.commons.ui.FrostedBox
import com.example.educapp.commons.ui.FrostedGlassTextField
import com.example.educapp.commons.ui.GradientCard
import com.example.educapp.commons.ui.HapticButton
import com.example.educapp.commons.ui.hapticClickable
import kotlin.text.isNotBlank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel, navController: NavHostController, teacherId: String) {
    var showNewGroupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teacherId) {
        viewModel.startGroupsListener(teacherId)
        Log.d("AttendanceMainScreen", "teacherId: $teacherId")
    }
    val groups = viewModel.groups
    Log.d("AttendanceMainScreen", "groups: $groups")
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(
                text = "Groups",
                style = MaterialTheme.typography.headlineLarge
                ) })
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

    val selectedColor = remember { mutableStateOf(group.getColor()) }

    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .hapticClickable { onTakeAttendance(group) },
        gradientBrush = Brush.horizontalGradient(
            colors = listOf(MaterialTheme.colorScheme.surface, selectedColor.value)
        )
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
                        text = { Text(
                            text = "Edit",
                            style = MaterialTheme.typography.bodySmall) },
                        onClick = {
                            showEditGroupDialog = true
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(
                            text = "Delete",
                            style = MaterialTheme.typography.bodySmall) },
                        onClick = {
                            showDeleteConfirmationDialog = true
                            expanded = false
                        }
                    )
                }
            }
            Text(text = group.schedule,
                style = MaterialTheme.typography.bodySmall)
        }
    }
    if (showEditGroupDialog) {
        EditGroupDialog(
            group = group,
            onConfirm = { updatedGroup ->
                selectedColor.value = Color(updatedGroup.color)
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

    // 🔹 Store color as an Int instead of Color
    var selectedColor by remember { mutableStateOf(group.color) }
    var showGradientPicker by remember { mutableStateOf(false) }

    if (showGradientPicker) {
        GradientPickerDialog(
            onDismiss = { showGradientPicker = false },
            onColorSelected = { newColor ->
                selectedColor = newColor.toArgb() // 🔹 Convert Color to Int when selected
                showGradientPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(
            text = "Edit Group",
            style = MaterialTheme.typography.headlineSmall
        )},
        text = {
            Column {
                FrostedGlassTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = "Name"
                )
                FrostedGlassTextField(
                    value = editedSchedule,
                    onValueChange = { editedSchedule = it },
                    label = "Schedule"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Change the color: ", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    FrostedBox(
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(40.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface,
                                            Color(selectedColor) // 🔹 Convert Int back to Color
                                        )
                                    )
                                )
                                .hapticClickable { showGradientPicker = true }
                        )
                    }
                }

                FrostedGlassTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = newStudentName,
                    onValueChange = { newStudentName = it },
                    label = "New Student Name",
                    placeholder = { Text(
                        text = "Enter one student at a time",
                        style = MaterialTheme.typography.labelMedium
                    )},
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (newStudentName.isNotBlank()) {
                                    editedStudents.add(newStudentName)
                                    newStudentName = ""
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Student")
                        }
                    }
                )

                // 4) Show the list of existing students with a frosted style
                LazyColumn {
                    items(editedStudents) { student ->
                        // Each student row has a frosted container
                        FrostedBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = student,
                                    style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { editedStudents.remove(student) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove Student")
                                }
                            }
                        }
                    }
                }


            }
        },
        confirmButton = {
            HapticButton(onClick = {
                val updatedGroup = group.copy(
                    name = editedName,
                    schedule = editedSchedule,
                    students = editedStudents,
                    color = selectedColor // 🔹 Save as Int
                )
                onConfirm(updatedGroup)
                onDismiss()
            }) {
                Text(text ="Confirm", style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = {
            HapticButton(onClick = onDismiss) {
                Text(text = "Cancel", style = MaterialTheme.typography.bodySmall)
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
        title = { Text(text="Confirm Deletion", style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text = "This will delete all data for this group. Are you sure?", style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            HapticButton(onClick = {
                onConfirm()
                onDismiss()
            },) {
                Text(text = "Confirm", style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = {
            HapticButton(onClick = onDismiss) {
                Text(text = "Cancel", style = MaterialTheme.typography.bodySmall)
            }
        }
    )
}

@Composable
fun GradientPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val gradientColors = listOf(
        Color(0xFFFD0331), // English 1
        Color(0xFF0045F5), // English 2
        Color(0xFF008000), // English 3
        Color(0xFFF57C00), // English 4
        Color(0xFF5E006E)  // English 5
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Pick a Color", style = MaterialTheme.typography.headlineSmall) },
        confirmButton = {
            HapticButton (onClick = onDismiss) { Text(text = "Close", style = MaterialTheme.typography.bodySmall) }
        },
        text = {
            Column {
                gradientColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.surface, color)
                                )
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        }
    )
}
