package com.example.educapp.commons.teacher.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.educapp.commons.ui.GradientDatePickerDialog
import com.example.educapp.commons.ui.HapticButton
import java.time.LocalDate


@Composable
fun CalendarMainScreen(
    navController: NavController,
    teacherId: String
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) } // NEW: controls dialog visibility

    // A Box that fills the screen with a vertical gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Gradient from a dark purple to an even darker shade
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
                .padding(16.dp), // Outer padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "Hero" title text
            Text(
                text = "Select a date to schedule an event",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Frosted glass style container for the Pick Date button
            Box(
                modifier = Modifier
                    .background(
                        // Slightly translucent surface color for a frosted effect
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                // The "Pick Date" button
                HapticButton(
                    onClick = {
                        showDatePicker = true  // Show the new Compose dialog
                    }
                ) {
                    Text("Pick Date")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show the selected date, if any
            selectedDate?.let { date ->
                Text(
                    text = "Selected Date: $date",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // If true, show our custom Compose date picker dialog
        if (showDatePicker) {
            GradientDatePickerDialog(
                onDateSelected = { date ->
                    // The user pressed "Confirm" with a selected date
                    selectedDate = date
                    showDatePicker = false
                    // Navigate to event creation screen with the selected date
                    navController.navigate("teacher/event_creation/${date}")
                },
                onDismiss = {
                    // The user dismissed or closed the dialog
                    showDatePicker = false
                }
            )
        }
    }
}
