package com.example.educapp.commons.assistant.network

import com.example.educapp.commons.assistant.DeepSeekMessage
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val stream: Boolean = false,
    val temperature: Double? = 0.7
)
@Serializable
data class ChatCompletionResult(
    val reasoningContent: String,
    val content: String
)
