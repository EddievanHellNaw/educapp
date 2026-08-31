package com.example.educapp.commons.teacher.calendar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.educapp.commons.ui.GradientDatePickerDialog
import com.example.educapp.commons.ui.HapticButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

@Composable
fun CalendarMainScreen(
    navController: NavController,
    teacherId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val importer = remember { AcademicCalendarImporter() }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedCalendarUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var previewDraft by remember {
        mutableStateOf<CalendarImportDraft?>(null)
    }

    var acceptedDraft by remember {
        mutableStateOf<CalendarImportDraft?>(null)
    }

    var importError by remember {
        mutableStateOf<String?>(null)
    }

    var isPreparingImport by remember {
        mutableStateOf(false)
    }

    val calendarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            isPreparingImport = true
            importError = null
            acceptedDraft = null

            try {
                previewDraft = importer.prepare(
                    context = context,
                    uri = uri
                )
            } catch (e: Exception) {
                importError = e.localizedMessage
                    ?: "Unable to open the selected academic calendar."
            } finally {
                isPreparingImport = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Top
        ) {

            Text(
                text = "Calendar",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )


            Spacer(
                modifier = Modifier.height(6.dp)
            )


            Text(
                text = "Create an event manually or import the university academic calendar.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )


            Spacer(
                modifier = Modifier.height(24.dp)
            )


            /*
             * ----------------------------------------
             * MANUAL EVENT
             * ----------------------------------------
             */
            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(16.dp),

                colors = CardDefaults.cardColors(
                    containerColor =
                    MaterialTheme.colorScheme
                        .surface
                        .copy(alpha = 0.25f)
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        text = "Manual Event",
                        style =
                        MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )


                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )


                    Text(
                        text = "Choose a date and create an event manually.",
                        style =
                        MaterialTheme.typography.bodySmall,
                        color =
                        Color.White.copy(alpha = 0.75f)
                    )


                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )


                    HapticButton(
                        onClick = {
                            showDatePicker = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Pick Date"
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            /*
             * ----------------------------------------
             * UNIVERSITY CALENDAR IMPORT
             * ----------------------------------------
             */
            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(16.dp),

                colors = CardDefaults.cardColors(
                    containerColor =
                    MaterialTheme.colorScheme
                        .surface
                        .copy(alpha = 0.25f)
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        text = "University Academic Calendar",
                        style =
                        MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )


                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )


                    Text(
                        text = "Import the calendar image or PDF provided by DUI.",
                        style =
                        MaterialTheme.typography.bodySmall,
                        color =
                        Color.White.copy(alpha = 0.75f)
                    )


                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )


                    HapticButton(
                        onClick = {

                            calendarPicker.launch(
                                arrayOf(
                                    "image/*",
                                    "application/pdf"
                                )
                            )
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {

                        Text(
                            text = "Import University Calendar"
                        )
                    }
                }
            }


            /*
             * Temporary Stage 1 debug feedback.
             *
             * We'll replace this with the actual preview
             * during the next step.
             */
            selectedCalendarUri?.let {

                Spacer(
                    modifier =
                    Modifier.height(16.dp)
                )


                Text(
                    text =
                    "Calendar selected ✓",

                    style =
                    MaterialTheme.typography.bodyMedium,

                    color =
                    Color.White
                )
            }
        }

        if (showDatePicker) {
            GradientDatePickerDialog(
                onDateSelected = { date ->
                    selectedDate = date
                    showDatePicker = false
                    navController.navigate("teacher/event_creation/${date}")
                },
                onDismiss = {
                    showDatePicker = false
                }
            )
        }

        previewDraft?.let { draft ->
            CalendarImportPreviewDialog(
                draft = draft,
                onUseCalendar = {
                    acceptedDraft = draft
                    previewDraft = null
                },
                onDismiss = {
                    previewDraft = null
                }
            )
        }
    }
}

@Composable
private fun CalendarActionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}
