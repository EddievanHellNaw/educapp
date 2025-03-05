package com.example.educapp.commons.assistant.network

import com.example.educapp.commons.assistant.DeepSeekMessage

data class ChatCompletionRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val stream: Boolean = false,
    val temperature: Double? = 0.7
)

data class ChatCompletionResult(
    val reasoningContent: String,
    val content: String
)
