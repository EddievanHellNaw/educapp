package com.example.educapp.commons.classwork

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Status Tracking
enum class ClassworkStatus {
    DRAFT, APPROVED, ARCHIVED
}

// Main Partial Data Classes


@Serializable
data class ClassworkPartial(
    val id: String = "",
    val groupId: String = "",
    val partialNumber: Int = 1,
    val title: String = "",
    val status: ClassworkStatus = ClassworkStatus.DRAFT,
    @Contextual
    val createdDate: Timestamp = Timestamp.now(),
    @Contextual
    val lastModified: Timestamp = Timestamp.now(),
)

@Serializable
data class ClassworkActivity(
    val id: String = "",
    val partialId: String = "",
    val title: String = "",
    val description: String = "",
    val type: ActivityType = ActivityType.GRAMMAR_EXERCISE,
    val status: ClassworkStatus = ClassworkStatus.DRAFT,
   @Contextual
    val dueDate: Timestamp? = null,
    val aiGenerationContext: String = "",
    val materials: List<ClassworkMaterial> = emptyList()
) {
    enum class ActivityType {
        ROLE_PLAY, GRAMMAR_EXERCISE, VOCABULARY_DRILL, READING_ASSIGNMENT
    }
}

@Serializable
data class ClassworkMaterial(
    @Serializable
    val id: String = "",
    @Serializable
    val activityId: String = "",
    @Serializable
    val type: MaterialType = MaterialType.TEXT,
    val content: String = "",
    val description: String = ""
) {
    @Serializable
    enum class MaterialType {
        PDF, IMAGE, TEXT, VIDEO, AUDIO, LINK
    }
}

@Serializable(with = LocalDateSerializer::class)
data class CustomDate(val date: LocalDate)

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}

class LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder) = LocalDateTime.parse(decoder.decodeString())
}

class ClassworkViewModel(
    private val repository: ClassworkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {


    val groupId: String = savedStateHandle.get<String>("groupId")!!

    private val _partials = MutableStateFlow<List<ClassworkPartial>>(emptyList())
    val partials: StateFlow<List<ClassworkPartial>> = _partials

    private val _activities = MutableStateFlow<List<ClassworkActivity>>(emptyList())
    val activities: StateFlow<List<ClassworkActivity>> = _activities

    private val _materials = MutableStateFlow<List<ClassworkMaterial>>(emptyList())
    val materials: StateFlow<List<ClassworkMaterial>> = _materials

    private val _uiState = MutableStateFlow<ClassworkUiState>(ClassworkUiState.Loading)
    val uiState: StateFlow<ClassworkUiState> = _uiState
    private val _previewActivities = MutableStateFlow<List<ClassworkActivity>>(emptyList())
    val previewActivities: StateFlow<List<ClassworkActivity>> = _previewActivities

    private val _currentPartial = MutableStateFlow<ClassworkPartial?>(null)
    val currentPartial: StateFlow<ClassworkPartial?> = _currentPartial

    var currentPartialId by mutableStateOf("")
        private set

    init {
        observePartials()
        loadAllClassworkData()
        setupPartialUpdates()
    }

    fun setCurrentPartial(partial: ClassworkPartial) {
        _currentPartial.value = partial
    }

    fun clearPreview() {
        _previewActivities.value = emptyList()
        _currentPartial.value = null
    }
    // Change the Success state to not require a message
    sealed interface ClassworkUiState {
        object Loading : ClassworkUiState
        object Success : ClassworkUiState  // Removed message parameter
        data class Error(val message: String) : ClassworkUiState
    }

    private fun setupPartialUpdates() {
        viewModelScope.launch {
            repository.getPartialsFlow(groupId).collect { partialsList ->
                _partials.value = partialsList
                loadActivitiesForPartials(partialsList)
            }
        }
    }

    private fun observePartials() {
        viewModelScope.launch {
            Log.d("ViewModel", "Observing partials for groupId: $groupId")
            repository.getPartialsFlow(groupId).collect { partials ->
                _partials.value = partials
                loadActivitiesForPartials(partials)
                _uiState.value = if (partials.isEmpty()) {
                    ClassworkUiState.Success // Still show empty state
                } else {
                    ClassworkUiState.Success
                }
                Log.d("ViewModel", "Fetched partials for group $groupId: ${partials.map { it.id to it.groupId }}")
            }
        }
    }

    private fun loadActivitiesForPartials(partials: List<ClassworkPartial>) {
        viewModelScope.launch {
            val allActivities = partials.flatMap { partial ->
                repository.getActivitiesForPartial(partial.id)
            }
            _activities.value = allActivities
        }
    }

    fun loadAllClassworkData() {
        _uiState.value = ClassworkUiState.Loading
        viewModelScope.launch {
            try {
                _partials.value = repository.getClassworkPartials(groupId)
                _uiState.value = ClassworkUiState.Success
            } catch (e: Exception) {
                _uiState.value = ClassworkUiState.Error("Failed to load data: ${e.message}")
            }
        }
    }
    private fun loadActivitiesForAllPartials() {
        viewModelScope.launch {
            _partials.value.forEach { partial ->
                val activities = repository.getActivitiesForPartial(partial.id)
                _activities.value = _activities.value + activities
            }
        }
    }

    // Call this after loading partials
    fun approvePartial(partialId: String) {
        viewModelScope.launch {
            try {
                // Add this to your repository interface
                repository.updatePartialStatus(partialId, ClassworkStatus.APPROVED)
                _partials.value = _partials.value.map {
                    if (it.id == partialId) it.copy(status = ClassworkStatus.APPROVED) else it
                }
            } catch (e: Exception) {
                _uiState.value = ClassworkUiState.Error("Approval failed: ${e.message}")
            }
        }
    }
    fun getPartialById(partialId: String): ClassworkPartial? {
        return _partials.value.find { it.id == partialId }
    }

    // In ClassworkViewModel.kt
    fun approveActivity(activityId: String) {
        viewModelScope.launch {
            try {
                // 1. Update in repository
                repository.updateActivityStatus(activityId, ClassworkStatus.APPROVED)

                // 2. Update local state
                _activities.value = _activities.value.map {
                    if (it.id == activityId) it.copy(status = ClassworkStatus.APPROVED) else it
                }

                // 3. Update parent partial if needed
                updatePartialStatusIfNeeded(activityId)

            } catch (e: Exception) {
                _uiState.value = ClassworkUiState.Error("Approval failed: ${e.message}")
            }
        }
    }

    private fun updatePartialStatusIfNeeded(activityId: String) {
        val activity = _activities.value.find { it.id == activityId }
        activity?.let {
            val partialId = it.partialId
            val allApproved = _activities.value
                .filter { it.partialId == partialId }
                .all { it.status == ClassworkStatus.APPROVED }

            if (allApproved) {
                approvePartial(partialId)
            }
        }
    }

    fun savePartialWithActivities(partial: ClassworkPartial, activities: List<ClassworkActivity>) {
        viewModelScope.launch {
            try {
                // Ensure the partial has the correct groupId (using the view model's groupId)
                val partialWithGroup = partial.copy(groupId = groupId)
                Log.d("ViewModel", "Attempting to save partial: $partialWithGroup")
                val savedPartial = repository.createClassworkPartial(partialWithGroup)
                Log.d("ViewModel", "Partial successfully created with id: ${savedPartial.id} and groupId: ${savedPartial.groupId}")

                // Generate new IDs for each activity and assign them to both the activity and its materials.
                val activitiesWithIds = activities.map { activity ->
                    val newActivityId = UUID.randomUUID().toString()
                    activity.copy(
                        id = newActivityId,
                        partialId = savedPartial.id,
                        materials = activity.materials.map { material ->
                            material.copy(
                                id = UUID.randomUUID().toString(),
                                activityId = newActivityId  // Ensure material links to the new activity ID
                            )
                        }
                    )
                }

                // Save activities and materials; repository.saveActivities handles both.
                repository.saveActivities(savedPartial.id, activitiesWithIds)

                // Update local state
                _partials.value = _partials.value + savedPartial
                _activities.value = _activities.value + activitiesWithIds

            } catch (e: Exception) {
                _uiState.value = ClassworkUiState.Error("Save failed: ${e.message}")
                Log.e("ViewModel", "Failed to save partial with activities", e)
            }
        }
    }

    fun createNewPartial(partial: ClassworkPartial) {
        viewModelScope.launch {
            try {
                repository.createClassworkPartial(partial)
                loadAllClassworkData()
            } catch (e: Exception) {
                _uiState.value = ClassworkUiState.Error("Creation failed: ${e.message}")
            }
        }
    }

    // Update processAiResponse in ClassworkViewModel
    fun processAiResponse(partialId: String, aiResponse: String) {
        viewModelScope.launch {
            _uiState.value = ClassworkUiState.Loading
            try {
                val parsed = parseAiResponse(aiResponse)
                _previewActivities.value = parsed
                _uiState.value = ClassworkUiState.Success
            } catch (e: Exception) {
                _uiState.value = ClassworkUiState.Error("AI Processing failed: ${e.message}")
            }
        }
    }

    fun confirmAndSaveActivities() {
        viewModelScope.launch {
            val partialId = currentPartial.value?.id ?: return@launch
            _uiState.value = ClassworkUiState.Loading

            try {
                // Save activities and their materials in one go.
                repository.saveActivities(partialId, _previewActivities.value)

                // Removed redundant material saving loop.

                loadAllClassworkData()
                clearPreview()
                _uiState.value = ClassworkUiState.Success
            } catch (e: Exception) {
                _uiState.value = ClassworkUiState.Error("Save failed: ${e.message}")
            }
        }
    }

    private fun extractJsonFromResponse(response: String): String {
        val jsonStart = response.indexOfFirst { it == '[' }
        val jsonEnd = response.indexOfLast { it == ']' }
        return if (jsonStart != -1 && jsonEnd != -1) {
            response.substring(jsonStart..jsonEnd)
        } else {
            ""
        }
    }

    internal fun parseAiResponse(response: String): List<ClassworkActivity> {
        return try {
            Json.decodeFromString(
                deserializer = ListSerializer(ClassworkActivity.serializer()),
                string = response
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to parse AI response")
            emptyList()
        }
    }
}