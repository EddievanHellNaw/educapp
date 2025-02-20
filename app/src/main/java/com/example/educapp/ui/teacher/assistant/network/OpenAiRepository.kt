package com.example.educapp.ui.teacher.assistant.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.google.gson.Gson
import java.io.IOException
import com.example.educapp.ui.teacher.assistant.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

class OpenAiRepository {

    fun streamChatMessages(
        apiKey: String,
        chatRequest: ChatCompletionRequest
    ): Flow<String> = flow {
        // Convert your request to JSON
        val requestBodyJson = Gson().toJson(chatRequest)

        // Make the raw streaming call
        ApiClient.streamChatCompletion(apiKey, requestBodyJson).use { response ->
            if (response == null || !response.isSuccessful) {
                throw IOException("HTTP error: ${response?.code} - ${response?.message}")
            }

            // Parse chunked response line by line
            val source = response.body?.source() ?: return@use
            val partialBuilder = StringBuilder()

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue

                // OpenAI sends each chunk as "data: {...}"
                if (line.startsWith("data: ")) {
                    val jsonPart = line.removePrefix("data: ").trim()
                    if (jsonPart == "[DONE]") {
                        // Stream is complete
                        break
                    }

                    // Parse the JSON chunk
                    val chunk = Gson().fromJson(jsonPart, StreamingChunk::class.java)
                    // Each chunk has partial tokens in chunk.choices[].delta.content
                    val partialToken = chunk.choices?.firstOrNull()?.delta?.content

                    if (!partialToken.isNullOrEmpty()) {
                        // Accumulate partial tokens
                        partialBuilder.append(partialToken)
                        // Emit the entire partial text so far
                        emit(partialBuilder.toString())
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
