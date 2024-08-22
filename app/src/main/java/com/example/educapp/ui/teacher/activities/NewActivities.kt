package com.example.educapp.ui.teacher.activities

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ActivitiesScreen(navController: NavController) {
    CreateNewActivityBox(navController)
}

@Composable
fun CreateNewActivityBox(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.25f)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .clickable { navigateToActivityCreation(navController) }
            .indication(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple()
            ), // Add ripple effect
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create New Activity",
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Create New Activity", color = Color.Gray)
        }
    }
}

private fun navigateToActivityCreation(navController: NavController) {
    navController.navigate("teacher/newActivity")
}