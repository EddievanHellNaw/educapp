package com.example.educapp.commons.assistant.network

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class AuthRepository {
    suspend fun getFirebaseToken(): String {
        return try {
            Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                ?: throw Exception("User not authenticated")
        } catch (e: Exception) {
            throw Exception("Failed to get auth token: ${e.message}")
        }
    }
}