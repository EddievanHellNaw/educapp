package com.example.educapp.commons.teacher.attendance

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.educapp.commons.ui.FrostedBox
import com.example.educapp.commons.ui.FrostedGlassTextField
import com.example.educapp.commons.ui.HapticButton
import com.example.educapp.commons.ui.hapticClickable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupDialog(
    viewModel: AttendanceViewModel,
    teacherId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var groupName by remember { mutableStateOf("") }
    var groupSchedule by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }
    val students = remember { mutableStateListOf<String>() }
    var selectedColor by remember { mutableStateOf(0xFF0045F5.toInt()) }
    var showGradientPicker by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.importPdfAndCreateGroup(it, context) }
        }
    )

    LaunchedEffect(uiState.showImportSuccess) {
        if (uiState.showImportSuccess) {
            onDismiss()
            Toast.makeText(context, "Group imported successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Title
                Text(
                    text = "New Group",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Scrollable Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        FrostedGlassTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = "Group Name"
                        )
                        FrostedGlassTextField(
                            value = groupSchedule,
                            onValueChange = { groupSchedule = it },
                            label = "Schedule"
                        )
                        FrostedGlassTextField(
                            value = studentName,
                            onValueChange = { studentName = it },
                            label = "Student Name",
                            placeholder = {
                                Text(
                                    text = "Enter one student at a time",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (studentName.isNotBlank()) {
                                        students.add(studentName)
                                        studentName = ""
                                    }
                                }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add Student")
                                }
                            }
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                            items(students) { student ->
                                Text(
                                    text = student,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Group Color: ", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.width(8.dp))
                            FrostedBox(
                                modifier = Modifier
                                    .size(40.dp)
                                    .hapticClickable { showGradientPicker = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.surface,
                                                    Color(selectedColor)
                                                )
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                // Buttons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HapticButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.height(50.dp).width(100.dp)
                    ) {
                        Text("Import PDF",
                            style = MaterialTheme.typography.labelMedium)
                    }

                    Row {
                        HapticButton(onClick = onDismiss,
                            modifier = Modifier.height(50.dp).width(85.dp)) {
                            Text("Cancel",
                                style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.width(8.dp))
                        HapticButton(
                            onClick = {
                                val group = AttendanceGroup(
                                    name = groupName,
                                    schedule = groupSchedule,
                                    students = students,
                                    color = selectedColor
                                )
                                viewModel.saveGroup(group, teacherId)
                                onDismiss()
                            },
                            enabled = groupName.isNotBlank() && students.isNotEmpty(),
                            modifier = Modifier.height(50.dp).width(85.dp)
                        ) {
                            Text("Save",
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    if (showGradientPicker) {
        GradientPickerDialog(
            onDismiss = { showGradientPicker = false },
            onColorSelected = { newColor ->
                selectedColor = newColor.toArgb()
                showGradientPicker = false
            }
        )
    }
}