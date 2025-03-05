package com.example.educapp.commons.assistant.network

import com.example.educapp.commons.assistant.ApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

class AssistantRepository {

    fun streamChatMessages(
        apiKey: String,
        chatRequest: ChatCompletionRequest
    ): Flow<ChatCompletionResult> = flow {
        val requestBodyJson = Gson().toJson(chatRequest)
        ApiClient.streamChatCompletion(apiKey, requestBodyJson).use { response ->
            if (response == null || !response.isSuccessful) {
                throw IOException("HTTP error: ${response?.code} - ${response?.message}")
            }
            val source = response.body?.source() ?: return@use
            val reasoningBuilder = StringBuilder()
            val contentBuilder = StringBuilder()

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (line.startsWith("data: ")) {
                    val jsonPart = line.removePrefix("data: ").trim()
                    if (jsonPart == "[DONE]") break

                    // Parse the JSON chunk
                    val chunk = Gson().fromJson(jsonPart, StreamingChunk::class.java)
                    val delta = chunk.choices?.firstOrNull()?.delta

                    // If the delta contains reasoning content, accumulate it; otherwise, accumulate content.
                    if (!delta?.reasoning_content.isNullOrEmpty()) {
                        reasoningBuilder.append(delta.reasoning_content)
                    } else if (!delta?.content.isNullOrEmpty()) {
                        contentBuilder.append(delta.content)
                    }

                    // Emit the current accumulation as a ChatCompletionResult
                    emit(
                        ChatCompletionResult(
                            reasoningContent = reasoningBuilder.toString(),
                            content = contentBuilder.toString()
                        )
                    )
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
