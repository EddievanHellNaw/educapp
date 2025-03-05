package com.example.educapp.commons.teacher.grading

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.commons.ui.GradientCard
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.educapp.commons.ui.FrostedGlassTextField
import com.example.educapp.commons.ui.HapticButton
import com.example.educapp.commons.ui.hapticClickable
import kotlinx.coroutines.delay
import com.example.educapp.R
import com.example.educapp.commons.teacher.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeGradesScreen(
    navController: NavHostController,
    viewModel: GradesViewModel,
    groupId: String,
    partial: Int,
    settingsViewModel: SettingsViewModel
) {

    // Collect the timer duration (e.g. 120) from SettingsViewModel
    val timerDurationState = settingsViewModel.timerDuration.collectAsState()
    val timerDuration = timerDurationState.value
    // Keep track of the current countdown time in local state
    var remainingTime by remember { mutableStateOf(timerDuration) }
    var isTimerRunning by remember { mutableStateOf(false) }
    // State to show the alarm dialog
    var showAlarmDialog by remember { mutableStateOf(false) }
    // MediaPlayer state to keep track of the sound
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val studentGrades by viewModel.studentGrades.collectAsState()
    Log.d("TakeGradesScreen", "studentGrades: $studentGrades")
// This effect runs whenever `isTimerRunning` changes
    val context = LocalContext.current
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            // Count down every second
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
            }
            // Once we get here, time is up
            isTimerRunning = false
            mediaPlayer = MediaPlayer.create(context, R.raw.beep_sound).apply {
                isLooping = true
                start()
            }
            showAlarmDialog = true// or your resource
            Log.d("TakeGradesScreen", "Time is up!")
        }
    }

    LaunchedEffect(groupId, partial) {
        viewModel.loadStudentGrades(groupId, partial)
        Log.d("TakeGradesScreen", "LaunchedEffect: groupId=$groupId, partial=$partial")

    }


    if (showAlarmDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss on outside touch */ },
            title = { Text("Oral Assessment Finished") },
            text = { Text("The oral assessments have finished. Please confirm to stop the alarm.") },
            confirmButton = {
                HapticButton(onClick = {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            mp.stop()
                        }
                        mp.release()
                        showAlarmDialog = false
                    }
                }) {
                    Text("Confirm")
                }
            }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grades for Partial $partial") },
                actions = {
                    // If not running, show a button to start
                    if (!isTimerRunning) {
                        HapticButton(
                            onClick = {
                                // Reset & start the timer
                                remainingTime = timerDuration * 60
                                isTimerRunning = true
                            }
                        ) {
                            Text("Start Timer")
                        }
                    } else {
                        // If running, show the remaining time
                        val minutes = remainingTime / 60
                        val seconds = remainingTime % 60
                        // Format seconds with leading zero if needed
                        val secondsText = seconds.toString().padStart(2, '0')
                        // Example: "1:59" or "0:05"
                        Text("$minutes:$secondsText")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (studentGrades.isEmpty()) {
                Log.d("TakeGradesScreen", "GroupId is: $groupId")
                Text("No grades available for this partial.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn {
                    items(studentGrades) { grade ->
                        StudentGradingCard(
                            grade = grade,
                            onUpdateOral = { grv, dm, pron, intCom ->
                                viewModel.updateOralGrade(
                                    studentName = grade.studentName,
                                    groupId = groupId,
                                    partial = partial,
                                    grv = grv,
                                    dm = dm,
                                    pron = pron,
                                    intCom = intCom
                                )
                            },
                            onUpdateWrittenPortfolio = { written, portfolio ->
                                viewModel.updateWrittenPortfolio(
                                    studentName = grade.studentName,
                                    groupId = groupId,
                                    partial = partial,
                                    newWritten = written,
                                    newPortfolio = portfolio
                                )
                            }
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun StudentGradingCard(
    grade: StudentGrade,
    onUpdateOral: (grv: Int, dm: Int, pron: Int, intCom: Int) -> Unit,
    onUpdateWrittenPortfolio: (written: Int, portfolio: Int) -> Unit
) {
    val isOralGraded = grade.oral > 0
    val isWrittenGraded = grade.written > 0 && grade.portfolio > 0

    var showOralDialog by remember { mutableStateOf(false) }
    var showWrittenDialog by remember { mutableStateOf(false) }

    // -- Dialogs --
    if (showOralDialog) {
        OralDialog(
            onDismiss = { showOralDialog = false },
            onConfirm = { grv, dm, pron, intCom ->
                onUpdateOral(grv, dm, pron, intCom)
                showOralDialog = false
            }
        )
    }
    if (showWrittenDialog) {
        WrittenPortfolioDialog(
            onDismiss = { showWrittenDialog = false },
            onConfirm = { written, portfolio ->
                onUpdateWrittenPortfolio(written, portfolio)
                showWrittenDialog = false
            }
        )
    }

    // -- The card layout --
    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Use a Row so we can have the name+label on the left and
        // the two vertical buttons on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // -- Left column: Name on top, status label below
            Column(
                modifier = Modifier.weight(2f) // Takes more space
            ) {
                // Student name
                Text(
                    text = grade.studentName,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Status label
                when {
                    isOralGraded && isWrittenGraded -> {
                        Text(
                            text = "✓ Oral & Written Graded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    isOralGraded -> {
                        Text(
                            text = "✓ Oral Graded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    else -> {
                        Text(
                            text = "Grade Pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // -- Right column: Two vertical buttons
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button for Oral
                HapticButton(
                    onClick = { showOralDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Oral")
                }
                // Button for Written + Portfolio
                HapticButton(
                    onClick = { showWrittenDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Written")
                }
            }
        }
    }
}

@Composable
fun OralDialog(
    onDismiss: () -> Unit,
    onConfirm: (gv: Int, dm: Int, pron: Int, intCom: Int) -> Unit
) {
    // local states for each aspect
    var grv by remember { mutableStateOf(0) }
    var dm by remember { mutableStateOf(0) }
    var pron by remember { mutableStateOf(0) }
    var intCom by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oral Grading") },
        text = {
            Column {
                // Four dropdowns or spinners from 0..3
                Text("Gr.V (0–3)")
                IntSpinner(value = grv, range = 0..3, onValueChange = { grv = it })

                Text("DM (0–3)")
                IntSpinner(value = dm, range = 0..3, onValueChange = { dm = it })

                Text("Pron (0–3)")
                IntSpinner(value = pron, range = 0..3, onValueChange = { pron = it })

                Text("Int Com (0–3)")
                IntSpinner(value = intCom, range = 0..3, onValueChange = { intCom = it })
            }
        },
        confirmButton = {
            HapticButton (onClick = {
                onConfirm(grv, dm, pron, intCom)
                onDismiss()
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            HapticButton (onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Simple spinner for integers
@Composable
fun IntSpinner(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    // You can implement with a DropdownMenu, or a row of RadioButtons, etc.
    // For brevity, let's just show a row of clickable Text.
    Row {
        range.forEach { num ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .hapticClickable { onValueChange(num) }
                    .background(if (value == num) Color.Gray else Color.Transparent)
            ) {
                Text("$num", modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun WrittenPortfolioDialog(
    onDismiss: () -> Unit,
    onConfirm: (written: Int, portfolio: Int) -> Unit
) {
    // Keep them as strings for the TextField,
    // but convert to Int upon confirmation
    var writtenText by remember { mutableStateOf("") }
    var portfolioText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Written & Portfolio") },
        text = {
            Column {
                // Written TextField
                FrostedGlassTextField(
                    value = writtenText,
                    onValueChange = { newValue ->
                        // Only allow digits
                        val digitsOnly = newValue.filter { it.isDigit() }
                        // Optionally limit to 2 or 3 digits
                        // if you want to avoid huge numbers
                        writtenText = digitsOnly.take(2)
                    },
                    label = "Written (max 50)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Portfolio TextField
                FrostedGlassTextField(
                    value = portfolioText,
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.filter { it.isDigit() }
                        portfolioText = digitsOnly.take(2)
                    },
                    label = "Portfolio (max 20)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            HapticButton(onClick = {
                // Convert strings to Int safely
                val w = writtenText.toIntOrNull() ?: 0
                val p = portfolioText.toIntOrNull() ?: 0

                // Enforce the maximum values
                val finalWritten = if (w > 50) 50 else w
                val finalPortfolio = if (p > 20) 20 else p

                onConfirm(finalWritten, finalPortfolio)
                onDismiss()
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            HapticButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
