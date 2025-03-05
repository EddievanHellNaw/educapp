package com.example.educapp.commons.teacher

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.educapp.R
import com.example.educapp.commons.ui.CircularButton
import com.example.educapp.commons.teacher.calendar.Event
import com.example.educapp.commons.teacher.calendar.EventRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import com.example.educapp.commons.ui.GradientCard
import com.example.educapp.commons.ui.hapticClickable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherMainScreen(navController: NavController,teacherUserName: String, eventRepository: EventRepository) {


    Scaffold(
        topBar = {
            // The topBar will align its content to the start by default.
            TopAppBar(
                title = {
                    Text(
                        text = "Welcome teacher $teacherUserName",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                },
                actions = {
                    CircularButton(imageResId = R.drawable.settings_icon, onClick = { navController.navigate("teacher/settings") })
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularButton(imageResId = R.drawable.attendance_icon, onClick = { navController.navigate("teacher/attendance") })
                CircularButton(imageResId = R.drawable.grading_icon, onClick = { navController.navigate("teacher/grades_main") })
                CircularButton(imageResId = R.drawable.activity_icon, isLarge = true, onClick = { navController.navigate("assistant") })
                CircularButton(imageResId = R.drawable.calendar_icon, onClick = { navController.navigate("teacher/calendar") })
                CircularButton(imageResId = R.drawable.notes_icon, onClick = { navController.navigate("annotation") })
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            UpcomingEventsSection(teacherUserName, eventRepository, navController as NavHostController)
        }
    }
}

@Composable
fun UpcomingEventsSection(
    teacherUserName: String,
    eventRepository: EventRepository,
    navController: NavHostController
) {
    val upcomingEvents: List<Event> by eventRepository
        .getUpcomingEvents(teacherUserName)
        .collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Upcoming Events",style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        upcomingEvents.forEach { event ->
            EventCard(event) {
                // On event click, navigate to event details
                navController.navigate("teacher/event_details/${event.id}")
            }
        }
    }
}

@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .hapticClickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(text = event.title, style = MaterialTheme.typography.headlineSmall)
            Text(text = event.toLocalDate().toString(),style = MaterialTheme.typography.bodySmall)
        }
    }
}