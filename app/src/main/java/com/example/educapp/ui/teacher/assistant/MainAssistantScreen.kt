package com.example.educapp.ui.teacher.assistant

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.teacher.assistant.AssistantViewModel
import com.example.myapp.teacher.assistant.ChatMessage

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAssistantScreen(navController: NavController, viewModel: AssistantViewModel) {
    val messages by viewModel.messages.collectAsState()

    // Track user input
    var userInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Display messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = false
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                // Call the ViewModel to handle sending the message
                val userRole = "teacher" // or fetch from your user settings
                viewModel.sendMessageStream(userRole, userInput)
                userInput = ""
            }) {
                Text("Send")
            }
        }
    }
}

@Composable
fun TextField(value: String, onValueChange: () -> Unit, modifier: Modifier) {
    TODO("Not yet implemented")
}

@Composable
fun MessageBubble(message: ChatMessage) {
    // Align to left or right depending on sender
    val alignment = if (message.sender == "user") Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.sender == "user") Color.Blue else Color.Gray
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Text(
            text = message.text,
            color = Color.White,
            modifier = Modifier
                .background(backgroundColor, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
                .widthIn(max = 300.dp) // Just to limit bubble width
        )
    }
}


