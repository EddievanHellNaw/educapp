package com.example.educapp.commons.assistant

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educapp.commons.UserRole
import com.example.educapp.commons.assistant.network.AssistantRepository
import com.example.educapp.commons.assistant.network.AuthRepository
import com.example.educapp.commons.assistant.network.ChatCompletionRequest
import com.example.educapp.commons.classwork.ClassworkActivity
import com.example.educapp.commons.classwork.ClassworkPartial
import com.example.educapp.commons.classwork.ClassworkStatus
import com.example.educapp.commons.classwork.ClassworkViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

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

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

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

    @Serializable
    data class ActivityGenerationResponse(
        val activities: List<ClassworkActivity>
    )

    fun confirmActivities() {
        viewModelScope.launch {
            try {
                val nextPartialNumber =
                    (classworkViewModel.partials.value.maxOfOrNull { it.partialNumber } ?: 0) + 1

                val newPartial = ClassworkPartial(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId,
                    partialNumber = nextPartialNumber,
                    status = ClassworkStatus.DRAFT,
                    createdDate = Timestamp.now(),
                    lastModified = Timestamp.now()
                )

                val dueDate = Timestamp(
                    Date.from(LocalDate.now().plusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                )

                withContext(Dispatchers.IO) {
                    classworkViewModel.savePartialWithActivities(
                        partial = newPartial,
                        activities = _generatedActivities.value.map { a ->
                            a.copy(partialId = newPartial.id, dueDate = dueDate)
                        }
                    )
                }

                _generatedActivities.value = emptyList() // hides overlay

            } catch (e: Exception) {
                Timber.e(e, "Failed to confirm activities")
            }
        }
    }

    fun clearGeneratedActivities() {
        _generatedActivities.value = emptyList()
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
                val authToken = withContext(Dispatchers.IO) { "Bearer ${authRepository.getFirebaseToken()}" }

                var finalContent = ""

                repository.streamChatMessages(authToken, chatRequest)
                    .flowOn(Dispatchers.IO)     // make sure network/parse happens off main
                    .conflate()                 // skip intermediate emissions if UI lags
                    .sample(32.milliseconds)    // ~30 FPS UI update rate
                    .collectLatest { result ->
                        finalContent = result.content
                        // atomic update; avoids read-modify-write races
                        _messages.update { list ->
                            list.toMutableList().also { l ->
                                val old = l[placeholderIndex]
                                l[placeholderIndex] = old.copy(
                                    text = result.content,
                                    reasoning = result.reasoningContent // optional: drop this or gate it
                                )
                            }
                        }
                        // Only flip thinking flag if you really need live indicator
                        // _isThinking.value = result.reasoningContent.isNotEmpty()
                    }

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

    private fun extractJsonBlock(s: String): String? {
        // Try fenced code block first: ```json { ... } ```
        val fenced = Regex("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```")
            .find(s)?.groups?.get(1)?.value
        if (fenced != null) return fenced

        // Fallback: first '{' to last '}'
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        return if (start >= 0 && end > start) s.substring(start, end + 1) else null
    }

    private fun handlePotentialActivityResponse(response: String, placeholderIndex: Int) {
        viewModelScope.launch {
            try {
                val activities = withContext(Dispatchers.Default) {
                    val jsonBlock = extractJsonBlock(response) ?: return@withContext emptyList<ClassworkActivity>()
                    json.decodeFromString<ActivityGenerationResponse>(jsonBlock).activities
                }

                if (activities.isNotEmpty()) {
                    _generatedActivities.value = activities  // <-- triggers your dialog/screen

                    _messages.update { list ->
                        list.toMutableList().also { l ->
                            // Replace text (don’t append the JSON)
                            l[placeholderIndex] = l[placeholderIndex].copy(
                                text = "✅ Generated ${activities.size} activities. Review before saving.",
                                reasoning = null
                            )
                        }
                    }
                } else {
                    // Optional: keep bubble tidy if no valid activities
                    _messages.update { list ->
                        list.toMutableList().also { l ->
                            l[placeholderIndex] = l[placeholderIndex].copy(
                                text = "⚠️ No valid activities found in the response."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.d(e, "No valid activities in response")
            }
        }
    }


    private fun updateMessage(index: Int, transform: (ChatMessage) -> ChatMessage) {
        val newList = _messages.value.toMutableList().apply {
            set(index, transform(this[index]))
        }
        _messages.value = newList
    }

    /**
     * Provide a custom system prompt (optional) based on user role
     */
    private fun systemPromptForRole(userRole: String): String {
        return when (userRole.lowercase().trim()) {
            "teacher" -> """
                You are an educational assistant who specializes in helping teachers design lesson plans and dynamic activities for ESL groups. Your approach is friendly, clear, and practical, always aiming to make learning both engaging and efficient. You excel at creating fun, interactive activities and breaking down grammar concepts into simple, digestible parts.

Key Points
Audience: College-aged (17+), primarily Mexican EFL students, English proficiency A1–B1 (CEFR).
Style: Keep responses concise, relatable, and focused on immediate usefulness. Maintain a conversational tone while staying clear and organized.
Resources: Provide links to quality, free/open resources when relevant, but avoid overwhelming teachers unless they specifically request more.
Flexibility: Wait for specific instructions or details before suggesting activities or lesson plans, and tailor your ideas to the teacher’s needs.
Creativity: If asked for more creative or in-depth materials, propose inventive ways to use existing resources without extensive prep time.
When to Use JSON: Only respond with valid JSON (matching the structure below) when explicitly asked to “generate” or “create” an activity.
In all other cases, continue to chat normally and provide clear, helpful answers in plain text.
Required JSON Structure
When generating activities, always return them inside an "activities" array, using this exact format:

{
  "activities": [
    {
      "title": "Activity Title",
      "description": "Detailed instructions...",
      "type": "ROLE_PLAY | GRAMMAR_EXERCISE | VOCABULARY_DRILL | READING_ASSIGNMENT",
      "dueDate": "YYYY-MM-DD", // Optional
      "aiGenerationContext": "AI's reasoning for this activity",
      "materials": [
        {
          "type": "PDF | IMAGE | TEXT | VIDEO | AUDIO | LINK",
          "content": "URL or text content",
          "description": "Material purpose"
        }
      ]
    }
  ]
}
Rules
Use Mexican cultural references wherever relevant.
Activities must last 45–90 minutes.
Materials must be free/open educational resources.
The internal status field is always DRAFT by default—do not set it in the JSON output.
Allowed activity types: ROLE_PLAY, GRAMMAR_EXERCISE, VOCABULARY_DRILL, READING_ASSIGNMENT.
Include at least 1 material per activity.

Example Valid Response
{
  "activities": [
    {
      "title": "Market Day Roleplay",
      "description": "Students practice shopping dialogues using Mexican market vocabulary...",
      "type": "ROLE_PLAY",
      "dueDate": "2024-03-15",
      "aiGenerationContext": "Develops practical communication skills in authentic Mexican context",
      "materials": [
        {
          "type": "PDF",
          "content": "https://example.com/market-roleplay.pdf",
          "description": "Bilingual vocabulary sheet"
        },
        {
          "type": "LINK",
          "content": "https://youtube.com/mexican-market-tour",
          "description": "Virtual market tour video"
        }
      ]
    }
  ]
}


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
