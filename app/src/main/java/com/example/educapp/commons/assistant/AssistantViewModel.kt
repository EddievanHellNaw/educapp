package com.example.myapp.teacher.assistant

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educapp.commons.assistant.DeepSeekMessage
import com.example.educapp.commons.assistant.network.AssistantRepository
import com.example.educapp.commons.assistant.network.ChatCompletionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val sender: String,
    val reasoning: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class AssistantViewModel(
    userRole: String,
    private val repository: AssistantRepository = AssistantRepository()
) : ViewModel() {

    // UI messages for display (chain-of-thought + final text)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    // Whether the model is currently streaming
    private val _isResponding = MutableStateFlow(false)
    val isResponding: StateFlow<Boolean> get() = _isResponding

    // Reasoning tokens incoming or not
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> get() = _isThinking

    /**
     * Internal conversation history (the official “messages” array).
     * We do NOT put chain-of-thought here, only final assistant text.
     */
    private val conversationHistory = mutableListOf<DeepSeekMessage>()

    /**
     * Example: we add a system prompt once, at the start,
     * or you can insert it every time if your design requires it.
     */
    init {
        // For example, a “teacher” system prompt by default:
        conversationHistory.add(
            DeepSeekMessage("system", systemPromptForRole(userRole))
        )
    }

    /**
     * Multi-round conversation entry point:
     * The user enters a new message -> we do streaming -> we parse chain-of-thought + final text
     */
    fun sendMessageStream(userRole: String, userText: String) {
        Log.d("AssistantViewModel", "Active user is: $userRole")
        if (conversationHistory.isEmpty() || conversationHistory[0].role != "system") {
            conversationHistory.add(
                0,
                DeepSeekMessage("system", systemPromptForRole(userRole))
            )
        }

        // 1) Add user message to UI
        val userMsg = ChatMessage(text = userText, sender = "user")
        _messages.value = _messages.value + userMsg

        // 2) Add user message to official conversation (NO chain-of-thought!)
        conversationHistory.add(DeepSeekMessage("user", userText))

        // 3) Create a placeholder for the assistant reply in UI
        val placeholderIndex = _messages.value.size
        val assistantPlaceholder = ChatMessage(
            text = "",
            sender = "assistant",
            reasoning = ""
        )
        _messages.value = _messages.value + assistantPlaceholder

        // 4) Build the streaming request, passing entire conversationHistory
        val chatRequest = ChatCompletionRequest(
            model = "deepseek-reasoner",
            messages = conversationHistory, // pass the entire history
            stream = true,
            temperature = 0.6
        )

        // 5) Launch streaming
        viewModelScope.launch {
            _isResponding.value = true
            repository.streamChatMessages(
                apiKey = "sk-91b7d73a8fb4485d8ddc6828f0029000",
                chatRequest = chatRequest
            ).collect { result ->
                // Combine partials with the old message’s data
                val updatedReasoning = result.reasoningContent
                val updatedFinalText = result.content


                // Update the placeholder with separate reasoning & final text
                _messages.value = _messages.value.mapIndexed { i, msg ->
                    if (i == placeholderIndex) {
                        msg.copy(
                            reasoning = updatedReasoning,
                            text = updatedFinalText
                        )
                    } else msg
                }
            }
            _isResponding.value = false

            // Once streaming completes, we can find the final text from the placeholder:
            val finalAssistantText = _messages.value[placeholderIndex].text
            // But it might contain “Reasoning:\n…\nAnswer:\n…”.
            // We only want the final “Answer” portion for the next round:
            val finalIndex = finalAssistantText.indexOf("Answer:\n")
            val answerOnly = if (finalIndex >= 0) {
                finalAssistantText.substring(finalIndex + "Answer:\n".length)
            } else {
                finalAssistantText // fallback if no reasoning
            }

            // 6) Add the assistant’s final text to conversationHistory,
            // so that the next round has the correct context.
            conversationHistory.add(
                DeepSeekMessage("assistant", answerOnly)
            )
        }
    }

    /**
     * Provide a custom system prompt (optional) based on user role
     */
    private fun systemPromptForRole(userRole: String): String {
        return when (userRole.lowercase().trim()) {
            "teacher" -> """
                You are an educational assistant helping a teacher create lesson plans and activities 
                for ESL groups of different levels. You are an expert on creating fun and dynamic 
                activities, and you explain grammar points in a simple yet concise manner. You try to keep your
                answers short and clear and give links to useful resources whenever relevant.
                You try to avoid might prep-time or time consuming materials for activities but if asked otherwise you 
                get really creative with material usage. 
                Take into account that students for the groups are always older than 17 years old and are college students.
                They are mexican in origin and have different skills sets when it comes to english. They are all in between A1 and B1 in CEFR levels.
                Wait for instructions or details before starting to showing activities or lesson plans.
            """.trimIndent()
            "student" -> """
                You are an educational assistant helping a student to learn English. 
                You adapt your answers to the student's level but always prefer simple language no higher than 
                a B2 CEFR level and never talk in any other language but English, except when the student tells you they are beginners, then you can use simple phrases to understand
                vocabulary and necessary concepts. 
                You correct grammar, spelling and vocabulary usage from their questions aside answering the question.
                You are always kind and warm with students and always ask their level first and whenever asked for practice
                you provide links to relevant websites to practice the skill at hand.
            """.trimIndent()
            else -> "You are an educational assistant."
        }
    }
}
