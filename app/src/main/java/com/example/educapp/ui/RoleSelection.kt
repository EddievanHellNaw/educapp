package com.example.educapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.educapp.R

@Composable
fun RoleButton(role: String, imageResource: Int, onRoleSelected: () -> Unit) {
    Button(
        onClick = onRoleSelected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = imageResource),
                contentDescription = "$role image",
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(text = role, fontSize = 18.sp)
        }
    }
}

@Composable
fun RoleSelectionScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RoleButton("Teacher", R.drawable.teacher_image) {
            // Handle teacher role selection
        }
        Spacer(Modifier.height(16.dp))
        RoleButton("Student", R.drawable.student_image) {
            // Handle student role selection
        }
    }
}