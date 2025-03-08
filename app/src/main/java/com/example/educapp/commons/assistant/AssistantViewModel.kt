package com.example.myapp.teacher.assistant

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educapp.commons.UserRole
import com.example.educapp.commons.assistant.DeepSeekMessage
import com.example.educapp.commons.assistant.network.AssistantRepository
import com.example.educapp.commons.assistant.network.AuthRepository
import com.example.educapp.commons.assistant.network.ChatCompletionRequest
import com.example.educapp.commons.classwork.ClassworkActivity
import com.example.educapp.commons.classwork.ClassworkPartial
import com.example.educapp.commons.classwork.ClassworkViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import timber.log.Timber

data class ChatMessage(
    val text: String,
    val sender: String,
    val reasoning: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class AssistantViewModel(
    private val userRole: UserRole,  // Use enum instead of String
    val groupId: String,
    private val repository: AssistantRepository,
    private val classworkViewModel: ClassworkViewModel,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedModel = MutableStateFlow("deepseek-reasoner")
    val selectedModel: StateFlow<String> get() = _selectedModel

    fun selectModel(model: String) {
        _selectedModel.value = model
    }

    // UI messages for display (chain-of-thought + final text)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    // Whether the model is currently streaming
    private val _isResponding = MutableStateFlow(false)
    val isResponding: StateFlow<Boolean> = _isResponding

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

    private val _generatedActivities = MutableStateFlow<List<ClassworkActivity>>(emptyList())
    val generatedActivities: StateFlow<List<ClassworkActivity>> = _generatedActivities

    fun sendMessage(userText: String) {
        if (userRole != UserRole.TEACHER) return

        viewModelScope.launch {
            _isResponding.value = true
            try {
                val partial = classworkViewModel.partials.value.lastOrNull()
                    ?: ClassworkPartial(groupId = groupId).also {
                        classworkViewModel.createNewPartial(it)
                    }

                // Use proper JSON formatting request
                val response = repository.generateActivities(
                    prompt = "Generate activities in JSON format for: $userText",
                    partialNumber = partial.partialNumber,
                    groupId = groupId
                )

                // Directly use parsed activities
                _generatedActivities.value = Json.decodeFromString(response)

                // Update chat
                _messages.value += ChatMessage(
                    text = "Generated ${_generatedActivities.value.size} activities",
                    sender = "assistant",
                    reasoning = "Activity generation complete"
                )

            } catch (e: Exception) {
                _messages.value += ChatMessage(
                    text = "Failed to generate activities: ${e.message}",
                    sender = "assistant"
                )
            } finally {
                _isResponding.value = false
            }
        }
    }

    private fun processAiResponse(response: String, partialId: String) {
        try {
            val activities = Json.decodeFromString<List<ClassworkActivity>>(response)
            _generatedActivities.value = activities.map {
                it.copy(partialId = partialId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse AI response")
            // Add fallback parsing if needed
        }
    }

    fun confirmActivities() {
        viewModelScope.launch {
            try {
                val activities = _generatedActivities.value
                classworkViewModel.processAiResponse(
                    partialId = _generatedActivities.value.first().partialId,
                    aiResponse = Json.encodeToString(
                        ListSerializer(ClassworkActivity.serializer()), // Explicit serializer
                        activities // Value to serialize
                    )                )
                _generatedActivities.value = emptyList()
            } catch (e: Exception) {
                Timber.e(e, "Failed to save activities")
            }
        }
    }

    fun clearGeneratedActivities() {
        _generatedActivities.value = emptyList()
    }

    private var currentSystemPrompt: String = ""

    private fun updateSystemPrompt(role: String) {
        val newPrompt = systemPromptForRole(role)
        if (currentSystemPrompt != newPrompt) {
            conversationHistory.removeAll { it.role == "system" }
            conversationHistory.add(0, DeepSeekMessage("system", newPrompt))
            currentSystemPrompt = newPrompt
        }
    }
    /**
     * Multi-round conversation entry point:
     * The user enters a new message -> we do streaming -> we parse chain-of-thought + final text
     */
    fun sendMessageStream(userRole: String, userText: String) {
        Log.d("AssistantViewModel", "Active user is: $userRole")

        // Ensure system prompt is present
        if (conversationHistory.isEmpty() || conversationHistory[0].role != "system") {
            conversationHistory.add(
                0,
                DeepSeekMessage("system", systemPromptForRole(userRole))
            )
        }

        // 1) Add user message to UI
        val userMsg = ChatMessage(text = userText, sender = "user")
        _messages.value = _messages.value + userMsg

        // 2) Add user message to conversation history
        conversationHistory.add(DeepSeekMessage("user", userText))

        // 3) Create a placeholder for the assistant reply
        val placeholderIndex = _messages.value.size
        val assistantPlaceholder = ChatMessage(
            text = "",
            sender = "assistant",
            reasoning = ""
        )
        _messages.value = _messages.value + assistantPlaceholder

        // 4) Build the streaming request
        val chatRequest = ChatCompletionRequest(
            model = _selectedModel.value,
            messages = conversationHistory,
            stream = true,
            temperature = 0.6
        )

        // 5) Launch streaming
        viewModelScope.launch {
            _isResponding.value = true
            _isThinking.value = true // Track reasoning state

            try {
                val authToken = "Bearer ${authRepository.getFirebaseToken()}"
                var finalContent = ""

                repository.streamChatMessages(authToken, chatRequest).collect { result ->
                    // Update both reasoning and content simultaneously
                    _messages.value = _messages.value.mapIndexed { i, msg ->
                        if (i == placeholderIndex) {
                            finalContent = result.content
                            msg.copy(
                                reasoning = result.reasoningContent,
                                text = result.content
                            )
                        } else msg
                    }

                    // Update thinking state based on reasoning updates
                    _isThinking.value = result.reasoningContent.isNotEmpty()
                }

                // After stream completes
                conversationHistory.add(DeepSeekMessage("assistant", finalContent))

                // Auto-detect JSON responses for teachers
                if (userRole == "teacher") {
                    handlePotentialActivityResponse(finalContent, placeholderIndex)
                }

            }catch (e: Exception) {
                val errorMsg = when (e) {
                    is AssistantRepository.AssistantExceptions.ChatStreamException -> {
                        when {
                            e.message?.contains("Rate limit") != null -> "Slow down! Too many requests"
                            e.message?.contains("Unauthorized") != null -> "Session expired - please re-login"
                            else -> "Connection error"
                        }
                    }
                    is AssistantRepository.AssistantExceptions.ActivityGenerationException ->
                        "Activity generation failed: ${e.message}"
                    else -> "Error: ${e.localizedMessage}"
                }

                // Update message display
                _messages.value = _messages.value.mapIndexed { i, msg ->
                    if (i == placeholderIndex) msg.copy(text = errorMsg) else msg
                }
            } finally {
                _isResponding.value = false
                _isThinking.value = false
            }
        }
    }

    private fun handlePotentialActivityResponse(response: String, placeholderIndex: Int) {
        viewModelScope.launch {
            try {
                val activities = Json.decodeFromString<List<ClassworkActivity>>(response)
                if (activities.isNotEmpty()) {
                    _generatedActivities.value = activities
                    _messages.value = _messages.value.mapIndexed { i, msg ->
                        if (i == placeholderIndex) msg.copy(
                            text = "${msg.text}\n\n✅ Found ${activities.size} activities"
                        ) else msg
                    }
                }
            } catch (e: Exception) {
                Timber.d("No valid activities found in response")
            }
        }
    }

    fun handleSuccessfulGeneration(response: String, classworkViewModel: ClassworkViewModel) {
        val activities = classworkViewModel.parseAiResponse(response)
        if (activities.isNotEmpty()) {
            // Create a temporary partial if needed
            val tempPartial = ClassworkPartial(
                groupId = classworkViewModel.groupId,
                title = "AI Generated Partial",
                partialNumber = classworkViewModel.partials.value.size + 1
            )

            classworkViewModel.setCurrentPartial(tempPartial)
            classworkViewModel.processAiResponse(tempPartial.id, response)
        }
    }

    /**
     * Provide a custom system prompt (optional) based on user role
     */
    private fun systemPromptForRole(userRole: String): String {
        return when (userRole.lowercase().trim()) {
            "teacher" -> """
                You are an educational assistant who specializes in helping teachers design lesson plans and dynamic activities for ESL groups. Your approach is friendly, clear, and practical, always aiming to make learning both engaging and efficient. You excel at creating fun, interactive activities and breaking down grammar concepts into simple, digestible parts.

                Key points to keep in mind:
                
                Audience: Your students are college-aged (over 17), primarily Mexican, and have English proficiency levels ranging from A1 to B1 (CEFR).
                Style: Keep your responses concise, relatable, and focused on what’s immediately useful. Use a conversational tone while maintaining clarity.
                Resources: Provide links to quality resources when relevant, but avoid overwhelming teachers with overly time-consuming materials unless they ask for it.
                Flexibility: Wait for specific instructions or details before suggesting activities or lesson plans, and tailor your ideas to the teacher’s needs.
                Creativity: If asked for more creative or in-depth materials, don’t hesitate to suggest inventive ways to use existing resources without extensive prep time.

            """.trimIndent()
            "student" -> """
                You are an educational assistant dedicated to helping students learn English. Your responses are tailored to the student's level, using clear and simple language up to a B2 CEFR level. You always communicate in English, but if a student indicates they're a beginner, you may include simple phrases in their native language to clarify vocabulary and key concepts.

                Key guidelines:
                
                Adaptability: Adjust your explanations and examples to fit the student's English level.
                Corrections: Gently correct any errors in grammar, spelling, or vocabulary in the student’s questions while answering them.
                Warmth and Support: Always maintain a kind, encouraging, and patient tone.
                Initial Assessment: Ask for the student's level if it's not already provided.
                Practice Resources: When requested, include links to relevant websites for practicing specific skills.

            """.trimIndent()
            else -> "You are an educational assistant."
        }
    }
}
