package com.example.educapp.commons.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.commons.teacher.calendar.Event
import com.example.educapp.commons.teacher.calendar.EventRepository
import com.example.educapp.commons.teacher.calendar.scheduleNotification
import com.example.educapp.commons.ui.FrostedGlassTextField
import com.example.educapp.commons.ui.HapticButton
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventDetailsScreen(
    navController: NavHostController,
    teacherUsername: String,
    eventId: String,
    eventRepository: EventRepository
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        var event by remember { mutableStateOf<Event?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(eventId) {
            try {
                event = eventRepository.getEventById(teacherUsername, eventId)
            } catch (e: Exception) {
                errorMessage = "Error loading event: ${e.message}"
            }
            isLoading = false
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (event == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = errorMessage.ifEmpty { "Event not found." })
            }
        } else {
            // Format the event date/time
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val eventDateTime: Date = event!!.dateTime.toDate()

            // Editable fields
            var title by remember { mutableStateOf(event!!.title) }
            var description by remember { mutableStateOf(event!!.description) }
            var remindMe by remember { mutableStateOf(event!!.remind) }
            var minutesBefore by remember { mutableStateOf("30") } // default, optionally parse from event.reminderTime

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Event Details", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FrostedGlassTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title"
                )
                Spacer(modifier = Modifier.height(8.dp))
                FrostedGlassTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Scheduled for: ${dateFormat.format(eventDateTime)}")
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Send Reminder")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = remindMe, onCheckedChange = { remindMe = it })
                }
                if (remindMe) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FrostedGlassTextField(
                        value = minutesBefore,
                        onValueChange = { minutesBefore = it },
                        label = "Minutes before event to remind"
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val context = LocalContext.current
                    HapticButton(onClick = {
                        coroutineScope.launch {
                            val updatedEvent = event!!.copy(
                                title = title,
                                description = description,
                                remind = remindMe,
                                reminderTime = if (remindMe) {
                                    val minutes = minutesBefore.toLongOrNull() ?: 30L
                                    val eventInstant = event!!.dateTime.toDate().toInstant()
                                    val newReminderInstant = eventInstant.minusSeconds(minutes * 60)
                                    Timestamp(Date.from(newReminderInstant))
                                } else null
                            )

                            coroutineScope.launch {
                                eventRepository.addOrUpdateEvent(teacherUsername, updatedEvent)
                                if (remindMe && updatedEvent.reminderTime != null) {
                                    scheduleNotification(context, updatedEvent)
                                }
                            }
                        }
                    }) {
                        Text("Edit")
                    }

                    HapticButton(
                        onClick = {
                            coroutineScope.launch {
                                eventRepository.deleteEvent(teacherUsername, event!!.id)
                                navController.popBackStack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HapticButton(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }
            }
        }
    }
}