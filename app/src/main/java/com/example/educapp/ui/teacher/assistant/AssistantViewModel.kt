package com.example.myapp.teacher.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educapp.ui.teacher.assistant.network.OpenAiRepository
import com.example.educapp.ui.teacher.assistant.network.ChatCompletionRequest
import com.example.educapp.ui.teacher.assistant.DeepSeekMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val sender: String, // "user" or "assistant"
    val timestamp: Long = System.currentTimeMillis()
)

class AssistantViewModel(
    private val repository: OpenAiRepository = OpenAiRepository() // or inject via Koin/Hilt
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    /**
     * Sends a message to the streaming endpoint, updates UI with partial responses.
     */
    fun sendMessageStream(userRole: String, messageText: String) {
        // 1) Add the user’s message to the chat
        val userMsg = ChatMessage(
            text = messageText,
            sender = "user"
        )
        _messages.value = _messages.value + userMsg

        // 2) Create a placeholder for the assistant’s partial reply
        //    We'll update this same message as tokens stream in
        val assistantPlaceholder = ChatMessage(
            text = "",  // start empty
            sender = "assistant"
        )
        val placeholderIndex = _messages.value.size // index for updating later

        _messages.value = _messages.value + assistantPlaceholder

        // 3) Build the streaming request
        val chatRequest = ChatCompletionRequest(
            model = "gpt-4o-mini", // or "gpt-3.5-turbo" / "gpt-4" if you have access
            messages = listOf(
                DeepSeekMessage("system", systemPromptForRole(userRole)),
                DeepSeekMessage("user", messageText)
            ),
            stream = true,
            temperature = 0.6
        )

        // 4) Collect partial text from the repository
        viewModelScope.launch {
            repository.streamChatMessages(
                apiKey = "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",
                chatRequest = chatRequest
            ).collect { partialText ->
                // Update the placeholder message with the latest partial
                _messages.value = _messages.value.mapIndexed { i, msg ->
                    if (i == placeholderIndex) msg.copy(text = partialText) else msg
                }
            }
        }
    }

    /**
     * Provide a custom system prompt based on the user’s role
     */
    private fun systemPromptForRole(userRole: String): String {
        return when (userRole) {
            "teacher" -> """
                You are an educational assistant helping a teacher create lesson plans and activities 
                for ESL groups of different levels. You are an expert on creating fun and dynamic 
                activities, and you explain grammar points in a simple yet concise manner.
            """.trimIndent()
            "student" -> "You are an educational assistant helping a student..."
            else -> "You are an educational assistant..."
        }
    }
}
