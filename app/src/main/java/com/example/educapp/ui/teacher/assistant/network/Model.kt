package com.example.educapp.ui.teacher.assistant.network

import com.example.educapp.ui.teacher.assistant.DeepSeekMessage

data class ChatCompletionRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val stream: Boolean = false,
    val temperature: Double? = 0.7
)

data class ChatMessage(
    val role: String,    // "system", "user", "assistant"
    val content: String
)