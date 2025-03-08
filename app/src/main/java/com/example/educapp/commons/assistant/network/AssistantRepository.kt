package com.example.educapp.commons.assistant.network

import com.example.educapp.commons.assistant.ApiClient
import com.example.educapp.commons.classwork.ClassworkActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface AssistantRepository {
    suspend fun generateActivities(
        prompt: String,
        partialNumber: Int,
        groupId: String
    ): String

    fun streamChatMessages(
        apiKey: String,
        chatRequest: ChatCompletionRequest
    ): Flow<ChatCompletionResult>

    sealed class AssistantExceptions(message: String, cause: Throwable? = null) :
        Exception(message, cause) {
        class ActivityGenerationException(message: String, cause: Throwable?) :
            AssistantExceptions(message, cause)
        class ChatStreamException(message: String, cause: Throwable?) :
            AssistantExceptions(message, cause)
    }
}

class AssistantRepositoryImpl(
    private val authRepo: AuthRepository,
) : AssistantRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    override suspend fun generateActivities(
        prompt: String,
        partialNumber: Int,
        groupId: String
    ): String {
        val authToken = "Bearer ${authRepo.getFirebaseToken()}"
        val request = GenerationRequest(
            model = "deepseek-educational",
            messages = listOf(
                Message(
                    role = "user",
                    content = """Generate activities for:
                        |Group: $groupId
                        |Partial: $partialNumber
                        |Instructions: $prompt
                        |Format: ${ClassworkActivity.serializer().descriptor}
                        """.trimMargin()
                )
            ),
            temperature = 0.6
        )

        val response = ApiClient.cloudflareService.generateContent(authToken, request)

        return try {
            json.decodeFromString<List<ClassworkActivity>>(
                response.choices.first().message.content
            ).toString()
        } catch (e: Exception) {
            throw ActivityGenerationException("Failed to parse activities", e)  // Fixed exception throw
        }
    }

    override fun streamChatMessages(
        apiKey: String,
        chatRequest: ChatCompletionRequest
    ): Flow<ChatCompletionResult> = flow<ChatCompletionResult> {

        val reasoningBuilder = StringBuilder()
        val contentBuilder = StringBuilder()

        try {
            val response = ApiClient.cloudflareService.streamChatCompletion(apiKey, chatRequest)

            if (!response.isSuccessful) {
                throw when (response.code()) {
                    401 -> IOException("Unauthorized - check auth token")
                    429 -> IOException("Rate limit exceeded")
                    else -> IOException("HTTP ${response.code()}")
                }
            }

            response.body()?.source()?.use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue

                    if (line.startsWith("data: ")) {
                        val jsonPart = line.removePrefix("data: ").trim()
                        if (jsonPart == "[DONE]") break

                        val chunk = json.decodeFromString<StreamingChunk>(jsonPart)
                        chunk.choices?.firstOrNull()?.delta?.let { delta ->
                            var shouldEmit = false

                            delta.reasoning_content?.let {
                                reasoningBuilder.append(it)
                                shouldEmit = true
                            }

                            delta.content?.let {
                                contentBuilder.append(it)
                                shouldEmit = true
                            }

                            if (shouldEmit) {
                                emit(  // This now clearly matches <ChatCompletionResult>
                                    ChatCompletionResult(
                                        reasoningContent = reasoningBuilder.toString(),
                                        content = contentBuilder.toString()
                                    )
                                )
                            }
                        }
                    }
                }
            } ?: throw IOException("Empty response body")

        } catch (e: Exception) {
            when (e) {
                is IOException -> throw ChatStreamException("Network error: ${e.message}", e)
                else -> throw ChatStreamException("Unexpected error: ${e.message}", e)
            }
        }
    }.flowOn(Dispatchers.IO)

    @Serializable
    data class GenerationRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double = 0.7,
        val response_format: ResponseFormat = ResponseFormat(type = "text")
    )

    @Serializable
    data class ResponseFormat(val type: String)

    @Serializable
    data class Message(
        val role: String,
        val content: String
    )

    class ActivityGenerationException(message: String, cause: Throwable?) :
        Exception(message, cause)

    class ChatStreamException(message: String, cause: Throwable?) : Exception(message, cause)
}

