package com.example.educapp.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.educapp.R
import com.example.educapp.ui.CircularButton

@Composable
fun StudentMainScreen(navController: NavController) {
    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularButton(imageResId = R.drawable.chatbot_icon, onClick = { navController.navigate("chatbot") })
                CircularButton(imageResId = R.drawable.activity_icon, onClick = { navController.navigate("activity") })
                CircularButton(imageResId = R.drawable.default_user_icon, isLarge = true, onClick = { navController.navigate("profile") }) // Larger button
                CircularButton(imageResId = R.drawable.calendar_icon, onClick = { navController.navigate("calendar") })
                CircularButton(imageResId = R.drawable.settings_icon, onClick = { navController.navigate("settings") })            }
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

