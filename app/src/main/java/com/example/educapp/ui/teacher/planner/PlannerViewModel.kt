package com.example.educapp.ui.teacher.planner

import android.util.Log
import androidx.compose.animation.core.copy
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

data class LessonPlan(
    val id: String = "", // Document ID in Firestore
    val title: String = "",
    val level: String = "",
    val topic: String = "",
    val description: String = "",
    val content: String = "" // Can be HTML or rich text
)

class PlannerViewModel : ViewModel() {
    private val _lessonPlans = MutableStateFlow<List<LessonPlan>>(emptyList())
    val lessonPlans: StateFlow<List<LessonPlan>> = _lessonPlans.asStateFlow()

    fun saveLessonPlan(lessonPlan: LessonPlan) {
        viewModelScope.launch {
            val db = Firebase.firestore
            try {
                val newLessonRef = db.collection("lessonPlans").document()
                val newLesson = lessonPlan.copy(id = newLessonRef.id)
                newLessonRef.set(newLesson).await()
                _lessonPlans.value = _lessonPlans.value + newLesson
            } catch (e: Exception) {
                Log.e("PlannerViewModel", "Error saving lesson plan", e)
                // Handle error (e.g., show a Snackbar)
            }
        }
    }

    fun getLessonPlans() {
        viewModelScope.launch {
            val db = Firebase.firestore
            try {
                db.collection("lessonPlans")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        _lessonPlans.value = querySnapshot.documents.map { document ->
                            document.toLessonPlan()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("PlannerViewModel", "Error getting lesson plans", e)
                        // Handle error
                    }
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

    private fun DocumentSnapshot.toLessonPlan(): LessonPlan {
        return LessonPlan(
            id = id,
            title = getString("title") ?: "",
            level = getString("level") ?: "",
            topic = getString("topic") ?: "",
            description = getString("description") ?: "",
            content = getString("content") ?: ""
        )
    }
}