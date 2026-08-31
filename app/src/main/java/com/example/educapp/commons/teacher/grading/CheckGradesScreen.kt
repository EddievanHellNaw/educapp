package com.example.educapp.commons.teacher.grading

import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.commons.ui.FrostedBox
import com.example.educapp.commons.ui.GradientCard
import com.example.educapp.commons.ui.HapticButton
import java.io.File
import com.example.educapp.commons.teacher.attendance.StudentPortrait
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface

import com.example.educapp.commons.ui.hapticClickable



@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckGradesScreen(
    navController: NavHostController,
    viewModel: GradesViewModel,
    groupId: String,
    partial: Int
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val studentGrades by viewModel.studentGrades.collectAsState()
    val currentGroup by
    viewModel.currentGroup.collectAsState()

    // State to hold the selected PDF Uri.
    var pdfUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for file picker.
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        pdfUri = uri
        if (uri != null) {

            Toast.makeText(
                context,
                "Roster selected. Generating PDF...",
                Toast.LENGTH_SHORT
            ).show()


            val outputPdfFile =
                File(
                    context.cacheDir,
                    "exported_grades.pdf"
                )


            viewModel.exportGradesToPdfFromUri(
                context = context,
                pdfUri = uri,
                outputPdfFile = outputPdfFile
            )

        } else {

            Toast.makeText(
                context,
                "No PDF file selected.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(groupId, partial) {
        viewModel.loadStudentGrades(groupId, partial)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Registered Grades for Partial $partial") })
        },
        bottomBar = {
            HapticButton(onClick = {
                // Always launch the file picker on button click.
                pdfPickerLauncher.launch("application/pdf")
            }) {
                Text("Select & Export PDF")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn {
                items(
                    items = studentGrades,
                    key = {
                        it.studentName
                    }
                ) { grade ->

                    StudentGradeCard(
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

                        onUpdateOral = {
                                grv,
                                dm,
                                pron,
                                intCom ->

                            viewModel.updateOralGrade(
                                studentName =
                                grade.studentName,

                                groupId =
                                groupId,

                                partial =
                                partial,

                                grv =
                                grv,

                                dm =
                                dm,

                                pron =
                                pron,

                                intCom =
                                intCom
                            )
                        },

                        onUpdateWrittenPortfolio = {
                                written,
                                portfolio ->

                            viewModel
                                .updateWrittenPortfolio(
                                    studentName =
                                    grade.studentName,

                                    groupId =
                                    groupId,

                                    partial =
                                    partial,

                                    newWritten =
                                    written,

                                    newPortfolio =
                                    portfolio
                                )
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun GradeMetric(
    label: String,
    value: Int,
    valueColor: Color =
        MaterialTheme
            .colorScheme
            .onSurface,
    emphasized: Boolean = false
) {

    Column(
        horizontalAlignment =
        Alignment.CenterHorizontally
    ) {

        Text(
            text = label,

            style =
            MaterialTheme
                .typography
                .labelSmall,

            color =
            MaterialTheme
                .colorScheme
                .onSurface
                .copy(alpha = 0.7f)
        )


        Text(
            text =
            value.toString(),

            style =
            if (emphasized) {
                MaterialTheme
                    .typography
                    .titleMedium
            } else {
                MaterialTheme
                    .typography
                    .bodyMedium
            },

            fontWeight =
            if (emphasized) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            },

            color =
            valueColor
        )
    }
}

@Composable
fun StudentGradeCard(
    grade: StudentGrade,
    photoBase64: String?,
    groupColor: Color,
    onUpdateOral: (
        Int,
        Int,
        Int,
        Int
    ) -> Unit,
    onUpdateWrittenPortfolio:
        (Int, Int) -> Unit
) {

    var editing by remember(
        grade.studentName
    ) {
        mutableStateOf(false)
    }

    var showOralDialog by remember {
        mutableStateOf(false)
    }

    var showWrittenDialog by remember {
        mutableStateOf(false)
    }


    /*
     * -------------------------
     * ORAL EDIT DIALOG
     * -------------------------
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


    /*
     * -------------------------
     * WRITTEN EDIT DIALOG
     * -------------------------
     */
    if (showWrittenDialog) {

        WrittenPortfolioDialog(
            initialWritten =
            grade.written,

            initialPortfolio =
            grade.portfolio,

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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {

            /*
             * ============================
             * STUDENT HEADER
             * ============================
             */
            Row(
                modifier =
                Modifier.fillMaxWidth(),

                verticalAlignment =
                Alignment.CenterVertically
            ) {

                StudentPortrait(
                    student =
                    grade.studentName,

                    photoBase64 =
                    photoBase64,

                    compact = true
                )


                Spacer(
                    modifier =
                    Modifier.width(10.dp)
                )


                /*
                 * Name + main summary
                 */
                Column(
                    modifier =
                    Modifier.weight(1f)
                ) {

                    Text(
                        text =
                        grade.studentName,

                        style =
                        MaterialTheme
                            .typography
                            .bodyLarge
                    )


                    Spacer(
                        modifier =
                        Modifier.height(5.dp)
                    )


                    FrostedBox {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal =
                                    7.dp,
                                    vertical =
                                    5.dp
                                ),

                            horizontalArrangement =
                            Arrangement
                                .SpaceEvenly,

                            verticalAlignment =
                            Alignment
                                .CenterVertically
                        ) {

                            GradeMetric(
                                label = "Faltas",

                                value =
                                grade.noFaltas,

                                valueColor =
                                if (
                                    grade.noFaltas > 0
                                ) {
                                    MaterialTheme
                                        .colorScheme
                                        .error
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                                }
                            )


                            GradeMetric(
                                label = "Oral",
                                value = grade.oral
                            )


                            GradeMetric(
                                label = "Written",
                                value =
                                grade.written
                            )


                            GradeMetric(
                                label = "Portf.",
                                value =
                                grade.portfolio
                            )


                            GradeMetric(
                                label = "Final",
                                value =
                                grade.finalGrade,
                                emphasized = true
                            )
                        }
                    }
                }


                Spacer(
                    modifier =
                    Modifier.width(6.dp)
                )


                /*
                 * Edit toggle now lives on the
                 * RIGHT side of the card.
                 */
                IconButton(
                    onClick = {
                        editing = !editing
                    }
                ) {

                    Icon(
                        imageVector =
                        Icons.Filled.Edit,

                        contentDescription =
                        "Edit ${grade.studentName}",

                        tint =
                        if (editing) {
                            MaterialTheme
                                .colorScheme
                                .primary
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurface
                        }
                    )
                }
            }


            Spacer(
                modifier =
                Modifier.height(8.dp)
            )


            /*
             * ============================
             * ORAL RUBRIC
             * ============================
             */
            FrostedBox(
                modifier =
                Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),

                    horizontalArrangement =
                    Arrangement.SpaceEvenly
                ) {

                    GradeMetric(
                        label = "GrV",
                        value = grade.oralGrV
                    )

                    GradeMetric(
                        label = "DM",
                        value = grade.oralDM
                    )

                    GradeMetric(
                        label = "Pron",
                        value = grade.oralPron
                    )

                    GradeMetric(
                        label = "ICom",
                        value =
                        grade.oralIntCom
                    )
                }
            }


            /*
             * ============================
             * EDIT CONTROLS
             * ============================
             */
            if (editing) {

                Spacer(
                    modifier =
                    Modifier.height(8.dp)
                )


                FrostedBox(
                    modifier =
                    Modifier.fillMaxWidth()
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),

                        horizontalArrangement =
                        Arrangement.Center,

                        verticalAlignment =
                        Alignment.CenterVertically
                    ) {

                        GradeEditAction(
                            icon =
                            Icons.Filled.Mic,

                            description =
                            "Edit oral exam",

                            completed =
                            grade.oralCompleted,

                            onClick = {
                                showOralDialog =
                                    true
                            }
                        )


                        Spacer(
                            modifier =
                            Modifier.width(24.dp)
                        )


                        GradeEditAction(
                            icon =
                            Icons.Filled.Description,

                            description =
                            "Edit written exam",

                            completed =
                            grade.writtenCompleted,

                            onClick = {
                                showWrittenDialog =
                                    true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeEditAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    completed: Boolean,
    onClick: () -> Unit
) {

    val completedColor =
        Color(0xFF388E3C)


    Box {

        Surface(
            modifier = Modifier
                .size(50.dp)
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
                    .copy(alpha = 0.40f)
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
                        .copy(
                            alpha = 0.35f
                        )
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
                    Modifier.size(25.dp)
                )
            }
        }


        /*
         * Completion badge
         */
        if (completed) {

            Box(
                modifier = Modifier
                    .size(17.dp)
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
fun TableCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    // Each “cell” is a small Box with a border
    FrostedBox (
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        // Label on top, value below
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

