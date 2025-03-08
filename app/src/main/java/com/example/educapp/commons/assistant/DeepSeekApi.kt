package com.example.educapp.commons.assistant

import com.example.educapp.commons.assistant.network.AssistantRepository
import com.example.educapp.commons.assistant.network.ChatCompletionRequest
import com.example.educapp.commons.assistant.network.ChatCompletionResult
import com.example.educapp.commons.classwork.ClassworkActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

@Serializable
data class DeepSeekRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    @SerialName("temperature")
    val temperature: Double? = 0.7,
    @SerialName("stream")
    val stream: Boolean = false
)

@Serializable
data class DeepSeekMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@Serializable
data class DeepSeekResponse(
    @SerialName("id") val id: String,
    @SerialName("choices") val choices: List<Choice>
) {
    @Serializable
    data class Choice(
        @SerialName("message") val message: DeepSeekMessage
    )
}

@Serializable
data class StreamingChunk(
    @SerialName("id") val id: String? = null,
    @SerialName("object") val obj: String? = null,
    @SerialName("created") val created: Long? = null,
    @SerialName("model") val model: String? = null,
    @SerialName("choices") val choices: List<StreamChoice>? = null
)

@Serializable
data class StreamChoice(
    @SerialName("delta") val delta: Delta,
    @SerialName("index") val index: Int? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class Delta(
    @SerialName("role") val role: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null
)

interface DeepSeekApi {
    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: DeepSeekRequest
    ): DeepSeekResponse

    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    fun streamChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: DeepSeekRequest
    ): Response
}

class AssistantGenerativeAPI(
    private val api: DeepSeekApi,
    private val json: Json
) : AssistantRepository {

    override suspend fun generateActivities(
        prompt: String,
        partialNumber: Int,
        groupId: String
    ): String {
        val request = DeepSeekRequest(
            model = "deepseek-reasoner",
            messages = listOf(
                DeepSeekMessage(
                    role = "system",
                    content = """Generate classwork activities in JSON format with:
                        - Partial number: $partialNumber
                        - Group ID: $groupId
                        - Valid activity types: ${ClassworkActivity.ActivityType.values().joinToString { it.name }}"""
                ),
                DeepSeekMessage(
                    role = "user",
                    content = prompt
                )
            ),
            temperature = 0.6
        )

        val response = api.getChatCompletion("Bearer YOUR_API_KEY", request)
        return response.choices.first().message.content
    }

    override fun streamChatMessages(
        apiKey: String,
        chatRequest: ChatCompletionRequest
    ): Flow<ChatCompletionResult> = flow {
        val deepSeekRequest = chatRequest.toDeepSeekRequest()
        val response = api.streamChatCompletion(apiKey, deepSeekRequest)

        response.body?.source()?.let { source ->
            val reasoningBuilder = StringBuilder()
            val contentBuilder = StringBuilder()

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (line.startsWith("data: ")) {
                    val jsonPart = line.removePrefix("data: ").trim()
                    if (jsonPart == "[DONE]") break

                    val chunk = json.decodeFromString<StreamingChunk>(jsonPart)
                    chunk.choices?.forEach { choice ->
                        choice.delta.let { delta ->
                            delta.reasoningContent?.let { reasoningBuilder.append(it) }
                            delta.content?.let { contentBuilder.append(it) }
                        }
                    }

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

    private fun ChatCompletionRequest.toDeepSeekRequest(): DeepSeekRequest {
        return DeepSeekRequest(
            model = model,
            messages = messages.map {
                DeepSeekMessage(role = it.role, content = it.content)
            },
            temperature = temperature,
            stream = true
        )
    }
}