package com.example.educapp.commons.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.educapp.R
import com.example.educapp.commons.ui.CircularButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMainScreen(navController: NavController) {
    Scaffold(topBar = {
        // The topBar will align its content to the start by default.
        TopAppBar(
            title = {
                Text(
                    text = "Welcome!",
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
                CircularButton(imageResId = R.drawable.student_image, onClick = { navController.navigate("chatbot") })
                CircularButton(imageResId = R.drawable.grading_icon,  onClick = { navController.navigate("profile") })
                CircularButton(imageResId = R.drawable.activity_icon,isLarge = true, onClick = { navController.navigate("assistant") })
                CircularButton(imageResId = R.drawable.calendar_icon, onClick = { navController.navigate("calendar") })
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
            Text("Student Home Screen")
        }
    }
}

