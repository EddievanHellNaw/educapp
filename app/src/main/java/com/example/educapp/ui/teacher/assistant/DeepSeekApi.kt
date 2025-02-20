package com.example.educapp.ui.teacher.assistant

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class DeepSeekRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val temperature: Double? = 0.7,
    val stream: Boolean = false
)

data class DeepSeekMessage(
    val role: String,   // "system", "user", or "assistant"
    val content: String
)

data class DeepSeekResponse(
    val id: String,
    val choices: List<Choice>
) {
    data class Choice(
        val message: DeepSeekMessage
    )
}

interface DeepSeekApi {

    @POST("v1/chat/completions")  // Adjust to your actual endpoint
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: DeepSeekRequest
    ): DeepSeekResponse
}
