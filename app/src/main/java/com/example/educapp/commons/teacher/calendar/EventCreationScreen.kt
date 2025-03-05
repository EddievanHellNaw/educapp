package com.example.educapp.commons.calendar


import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.commons.teacher.calendar.Event
import com.example.educapp.commons.teacher.calendar.EventRepository
import com.example.educapp.commons.teacher.calendar.scheduleNotification
import com.example.educapp.commons.ui.HapticButton
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

@Composable
fun EventCreationScreen(
    navController: NavHostController,
    teacherUsername: String,
    initialDate: LocalDate,
    eventRepository: EventRepository
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf(LocalTime.of(12, 0)) } // default to noon
    var showTimePicker by remember { mutableStateOf(false) }

    // Reminders toggles:
    var remindOneDayBefore by remember { mutableStateOf(false) }
    var remindOneWeekBefore by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Surface (Modifier.fillMaxSize()){
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Create an Event for $initialDate",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") })
            Spacer(modifier = Modifier.height(8.dp))

            // Row to show and pick the event time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Time: ${eventTime.toString()}")
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { showTimePicker = true }) {
                    Text("Pick Time")
                }
            }
            if (showTimePicker) {
                val timePickerDialog = TimePickerDialog(
                    context,
                    { _, hour: Int, minute: Int ->
                        eventTime = LocalTime.of(hour, minute)
                        showTimePicker = false
                    },
                    eventTime.hour,
                    eventTime.minute,
                    true
                )
                LaunchedEffect(Unit) {
                    timePickerDialog.show()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Reminder toggles
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Remind one day before")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = remindOneDayBefore, onCheckedChange = { remindOneDayBefore = it })
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Remind one week before")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = remindOneWeekBefore,
                    onCheckedChange = { remindOneWeekBefore = it })
            }
            Spacer(modifier = Modifier.height(16.dp))

            HapticButton(onClick = {
                coroutineScope.launch {
                    // Combine date and time into a LocalDateTime, then convert to Timestamp
                    val eventDateTime = initialDate.atTime(eventTime)
                    val eventInstant = eventDateTime.atZone(ZoneId.systemDefault()).toInstant()
                    val eventTimestamp = Timestamp(Date.from(eventInstant))

                    // Create the event (reminder booleans stored in the event if you choose)
                    val newEvent = Event(
                        title = title,
                        description = description,
                        dateTime = eventTimestamp,
                        // For simplicity, we store reminder info only as booleans
                        remind = remindOneDayBefore || remindOneWeekBefore
                    )
                    // Save event to Firestore under /users/{teacherUsername}/events
                    eventRepository.addEvent(teacherUsername, newEvent)

                    // Schedule notifications if toggles are enabled:
                    if (remindOneDayBefore) {
                        // Calculate one day before event
                        val oneDayBeforeInstant = eventInstant.minusSeconds(86400)
                        val oneDayBeforeTimestamp = Timestamp(Date.from(oneDayBeforeInstant))
                        // Schedule notification for one day before (pass a copy of the event with the appropriate reminderTime)
                        scheduleNotification(
                            context,
                            newEvent.copy(reminderTime = oneDayBeforeTimestamp)
                        )
                    }
                    if (remindOneWeekBefore) {
                        // Calculate one week before event
                        val oneWeekBeforeInstant = eventInstant.minusSeconds(604800)
                        val oneWeekBeforeTimestamp = Timestamp(Date.from(oneWeekBeforeInstant))
                        scheduleNotification(
                            context,
                            newEvent.copy(reminderTime = oneWeekBeforeTimestamp)
                        )
                    }
                    navController.popBackStack()
                }
            }) {
                Text("Save Event")
            }
        }
    }
}
