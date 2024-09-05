package com.example.educapp.ui.teacher.planner

import android.util.Log
import org.jsoup.safety.Safelist
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.State
import org.jsoup.Jsoup
import timber.log.Timber

data class LessonPlan(
    val id: String = "",
    val title: String = "",
    val level: String = "",
    val topic: String = "",
    val description: String = "",
    val content: String  = ""
)

class PlannerViewModel : ViewModel() {
    private val _lessonPlans = MutableStateFlow<List<LessonPlan>>(emptyList())
    val lessonPlans: StateFlow<List<LessonPlan>> = _lessonPlans.asStateFlow()

    fun saveLessonPlan(lessonPlan: LessonPlan, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val db = Firebase.firestore
            try {
                Log.d("PlannerViewModel", "Saving lesson plan...")
                val sanitizedContent = sanitizeHtml(lessonPlan.content)
                val newLessonRef = db.collection("lessonPlans").document()
                val newLesson = lessonPlan.copy(id = newLessonRef.id, content = sanitizedContent)
                newLessonRef.set(newLesson).await()
                _lessonPlans.value = _lessonPlans.value + newLesson
                Log.d("PlannerViewModel", "Lesson plan saved successfully!")
                onComplete(true)
            } catch (e: Exception) {
                Log.e("PlannerViewModel", "Error saving lesson plan", e)
                onComplete(false)
            }
        }
    }

    private fun sanitizeHtml(content: String): String {
        return Jsoup.clean(content, Safelist.basic())
    }

    fun getLessonPlans() {
        viewModelScope.launch {
            try {
                val querySnapshot = Firebase.firestore.collection("lessonPlans").get().await()
                _lessonPlans.value = querySnapshot.documents.mapNotNull { it.toLessonPlan() }
            } catch (e: Exception) {
                Log.e("PlannerViewModel", "Error getting lesson plans", e)
                // Handle error
            }
        }
    }

    fun updateLessonPlan(lessonPlan: LessonPlan) {
        viewModelScope.launch {
            val db = Firebase.firestore
            try {
                db.collection("lessonPlans").document(lessonPlan.id)
                    .set(lessonPlan)
                    .await()
                _lessonPlans.value = _lessonPlans.value.map {
                    if (it.id == lessonPlan.id) lessonPlan else it
                }
            } catch (e: Exception) {
                Log.e("PlannerViewModel", "Error updating lesson plan", e)
                // Handle error
            }
        }
    }

    fun deleteLessonPlan(lessonPlan: LessonPlan) {
        viewModelScope.launch {
            val db = Firebase.firestore
            try {
                db.collection("lessonPlans").document(lessonPlan.id)
                    .delete()
                    .await()
                _lessonPlans.value = _lessonPlans.value - lessonPlan
            } catch (e: Exception) {
                Log.e("PlannerViewModel", "Error deleting lesson plan", e)
                // Handle error
            }
        }
    }

    fun DocumentSnapshot.toLessonPlan(): LessonPlan? {
        return try {
            this.toObject(LessonPlan::class.java)
        } catch (e: Exception) {
            Log.e("DocumentSnapshot", "Error converting to LessonPlan", e)
            null
        }
    }

    private val _lessonPlan = mutableStateOf<LessonPlan?>(null)
    val lessonPlan: State<LessonPlan?> = _lessonPlan

    fun fetchLessonPlanById(lessonPlanId: String) {
        viewModelScope.launch {
            try {
                Log.d("PlannerViewModel", "Fetching lesson plan with ID: $lessonPlanId")
                val documentSnapshot = Firebase.firestore.collection("lessonPlans").document(lessonPlanId).get().await()
                _lessonPlan.value = if (documentSnapshot.exists()) {
                    documentSnapshot.toObject(LessonPlan::class.java)
                } else {
                    null // Or handle the case where the document doesn't exist
                }
                Log.d("PlannerViewModel", "Lesson plan fetched: ${_lessonPlan.value}")
                Log.d("PlannerViewModel", "Lesson plan fetched: ${_lessonPlan.value?.content}")
            } catch (e: Exception) {
                // Handle exceptions (e.g., log the error or show an error message)
                Log.e("PlannerViewModel", "Error fetching lesson plan", e) // Using Timber for logging
                _lessonPlan.value = null // Or handle the error state in your UI
            }
        }
    }
}