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
import androidx.compose.foundation.layout.width
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
import com.example.educapp.commons.teacher.attendance.StudentPortrait
import com.example.educapp.commons.ui.FrostedBox
import com.example.educapp.commons.teacher.attendance.StudentPortrait
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import com.example.educapp.commons.teacher.attendance.StudentPortrait
import com.example.educapp.commons.ui.FrostedBox

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
    val currentGroup by
    viewModel.currentGroup.collectAsState()
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

                            photoBase64 =
                            currentGroup
                                ?.studentPhotos
                                ?.get(
                                    grade.studentName
                                ),

                            groupColor =
                            currentGroup
                                ?.getColor()
                                ?: MaterialTheme
                                    .colorScheme
                                    .primary,
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
    photoBase64: String?,
    groupColor: Color,
    onUpdateOral: (
        grv: Int,
        dm: Int,
        pron: Int,
        intCom: Int
    ) -> Unit,
    onUpdateWrittenPortfolio: (
        written: Int,
        portfolio: Int
    ) -> Unit
) {

    var showOralDialog by remember {
        mutableStateOf(false)
    }

    var showWrittenDialog by remember {
        mutableStateOf(false)
    }


    /*
     * -----------------------------
     * GRADING DIALOGS
     * -----------------------------
     */

    if (showOralDialog) {

        OralDialog(
            initialGrv = grade.oralGrV,
            initialDm = grade.oralDM,
            initialPron = grade.oralPron,
            initialIntCom = grade.oralIntCom,

            onDismiss = {
                showOralDialog = false
            },

            onConfirm = {
                    grv,
                    dm,
                    pron,
                    intCom ->

                onUpdateOral(
                    grv,
                    dm,
                    pron,
                    intCom
                )

                showOralDialog = false
            }
        )
    }


    if (showWrittenDialog) {

        WrittenPortfolioDialog(
            initialWritten = grade.written,
            initialPortfolio = grade.portfolio,

            onDismiss = {
                showWrittenDialog = false
            },

            onConfirm = {
                    written,
                    portfolio ->

                onUpdateWrittenPortfolio(
                    written,
                    portfolio
                )

                showWrittenDialog = false
            }
        )
    }


    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 5.dp
            ),

        gradientBrush =
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme
                    .colorScheme
                    .surface,

                groupColor
            )
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),

            verticalAlignment =
            Alignment.CenterVertically
        ) {

            /*
             * Student picture
             */
            StudentPortrait(
                student = grade.studentName,
                photoBase64 = photoBase64,
                compact = true
            )


            Spacer(
                modifier = Modifier.width(10.dp)
            )


            /*
             * Student information
             */
            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = grade.studentName,
                    style =
                    MaterialTheme.typography
                        .bodyLarge
                )


                Spacer(
                    modifier =
                    Modifier.height(5.dp)
                )


                AssessmentProgressBadge(
                    oralCompleted =
                    grade.oralCompleted,

                    writtenCompleted =
                    grade.writtenCompleted
                )


                Spacer(
                    modifier =
                    Modifier.height(5.dp)
                )


                FrostedBox {

                    Row(
                        modifier = Modifier
                            .padding(
                                horizontal = 7.dp,
                                vertical = 4.dp
                            ),

                        horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                    ) {

                        Text(
                            text =
                            "Faltas ${grade.noFaltas}",

                            style =
                            MaterialTheme.typography
                                .labelSmall,

                            color =
                            if (grade.noFaltas > 0) {
                                MaterialTheme
                                    .colorScheme.error
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                            }
                        )


                        Text(
                            text =
                            "Oral ${grade.oral}",

                            style =
                            MaterialTheme.typography
                                .labelSmall
                        )


                        Text(
                            text =
                            "Written ${grade.written}",

                            style =
                            MaterialTheme.typography
                                .labelSmall
                        )
                    }
                }
            }


            Spacer(
                modifier =
                Modifier.width(8.dp)
            )


            /*
             * Compact grading actions.
             */
            Row(
                horizontalArrangement =
                Arrangement.spacedBy(
                    6.dp
                )
            ) {

                GradeActionIcon(
                    icon =
                    Icons.Filled.Mic,

                    description =
                    "Grade oral exam",

                    completed =
                    grade.oralCompleted,

                    onClick = {
                        showOralDialog = true
                    }
                )


                GradeActionIcon(
                    icon =
                    Icons.Filled.Description,

                    description =
                    "Grade written exam",

                    completed =
                    grade.writtenCompleted,

                    onClick = {
                        showWrittenDialog = true
                    }
                )
            }
        }
    }
}

@Composable
private fun AssessmentProgressBadge(
    oralCompleted: Boolean,
    writtenCompleted: Boolean
) {

    val completed =
        listOf(
            oralCompleted,
            writtenCompleted
        ).count { it }


    val color =
        when (completed) {

            0 ->
                MaterialTheme
                    .colorScheme
                    .error

            1 ->
                Color(0xFFFBC02D)

            else ->
                Color(0xFF388E3C)
        }


    val label =
        when (completed) {

            0 ->
                "Pending · 0/2"

            1 ->
                "In progress · 1/2"

            else ->
                "Complete · 2/2"
        }


    Row(
        verticalAlignment =
        Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    color,
                    CircleShape
                )
        )


        Spacer(
            modifier =
            Modifier.width(5.dp)
        )


        Text(
            text = label,

            style =
            MaterialTheme.typography
                .labelMedium,

            color = color
        )
    }
}

@Composable
private fun GradeActionIcon(
    icon: ImageVector,
    description: String,
    completed: Boolean,
    onClick: () -> Unit
) {

    val completedColor =
        Color(0xFF388E3C)


    Box {

        Surface(
            modifier = Modifier
                .size(46.dp)
                .hapticClickable {
                    onClick()
                },

            shape = CircleShape,

            color =
            if (completed) {
                completedColor.copy(
                    alpha = 0.18f
                )
            } else {
                MaterialTheme
                    .colorScheme
                    .surface
                    .copy(alpha = 0.4f)
            },

            border =
            BorderStroke(
                width =
                if (completed) {
                    2.dp
                } else {
                    1.dp
                },

                color =
                if (completed) {
                    completedColor
                } else {
                    MaterialTheme
                        .colorScheme
                        .outline
                        .copy(alpha = 0.4f)
                }
            )
        ) {

            Box(
                modifier =
                Modifier.fillMaxSize(),

                contentAlignment =
                Alignment.Center
            ) {

                Icon(
                    imageVector = icon,

                    contentDescription =
                    description,

                    tint =
                    if (completed) {
                        completedColor
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurface
                    },

                    modifier =
                    Modifier.size(24.dp)
                )
            }
        }


        /*
         * Small completion check.
         */
        if (completed) {

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(
                        Alignment.TopEnd
                    )
                    .background(
                        completedColor,
                        CircleShape
                    ),

                contentAlignment =
                Alignment.Center
            ) {

                Icon(
                    imageVector =
                    Icons.Filled.Check,

                    contentDescription =
                    "Completed",

                    tint = Color.White,

                    modifier =
                    Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
fun OralDialog(
    initialGrv: Int = 0,
    initialDm: Int = 0,
    initialPron: Int = 0,
    initialIntCom: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (
        gv: Int,
        dm: Int,
        pron: Int,
        intCom: Int
    ) -> Unit
) {

    var grv by remember(initialGrv) {
        mutableStateOf(initialGrv)
    }

    var dm by remember(initialDm) {
        mutableStateOf(initialDm)
    }

    var pron by remember(initialPron) {
        mutableStateOf(initialPron)
    }

    var intCom by remember(initialIntCom) {
        mutableStateOf(initialIntCom)
    }


    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Oral Grading")
        },

        text = {

            Column {

                Text("Gr.V (0–3)")

                IntSpinner(
                    value = grv,
                    range = 0..3,
                    onValueChange = {
                        grv = it
                    }
                )


                Text("DM (0–3)")

                IntSpinner(
                    value = dm,
                    range = 0..3,
                    onValueChange = {
                        dm = it
                    }
                )


                Text("Pron (0–3)")

                IntSpinner(
                    value = pron,
                    range = 0..3,
                    onValueChange = {
                        pron = it
                    }
                )


                Text("Int Com (0–3)")

                IntSpinner(
                    value = intCom,
                    range = 0..3,
                    onValueChange = {
                        intCom = it
                    }
                )
            }
        },

        confirmButton = {

            HapticButton(
                onClick = {

                    onConfirm(
                        grv,
                        dm,
                        pron,
                        intCom
                    )
                }
            ) {

                Text("Confirm")
            }
        },

        dismissButton = {

            HapticButton(
                onClick = onDismiss
            ) {

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
    initialWritten: Int = 0,
    initialPortfolio: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (
        written: Int,
        portfolio: Int
    ) -> Unit
) {

    var writtenText by
    remember(initialWritten) {
        mutableStateOf(
            initialWritten.toString()
        )
    }


    var portfolioText by
    remember(initialPortfolio) {
        mutableStateOf(
            initialPortfolio.toString()
        )
    }


    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                "Written & Portfolio"
            )
        },

        text = {

            Column {

                FrostedGlassTextField(
                    value = writtenText,

                    onValueChange = {
                            newValue ->

                        writtenText =
                            newValue
                                .filter {
                                    it.isDigit()
                                }
                                .take(2)
                    },

                    label =
                    "Written (max 50)",

                    keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                        KeyboardType.Number
                    )
                )


                Spacer(
                    modifier =
                    Modifier.height(8.dp)
                )


                FrostedGlassTextField(
                    value = portfolioText,

                    onValueChange = {
                            newValue ->

                        portfolioText =
                            newValue
                                .filter {
                                    it.isDigit()
                                }
                                .take(2)
                    },

                    label =
                    "Portfolio (max 20)",

                    keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                        KeyboardType.Number
                    )
                )
            }
        },

        confirmButton = {

            HapticButton(
                onClick = {

                    val written =
                        (
                                writtenText
                                    .toIntOrNull()
                                    ?: 0
                                )
                            .coerceIn(
                                0,
                                50
                            )

                    val portfolio =
                        (
                                portfolioText
                                    .toIntOrNull()
                                    ?: 0
                                )
                            .coerceIn(
                                0,
                                20
                            )


                    onConfirm(
                        written,
                        portfolio
                    )
                }
            ) {

                Text("Confirm")
            }
        },

        dismissButton = {

            HapticButton(
                onClick = onDismiss
            ) {

                Text("Cancel")
            }
        }
    )
}
