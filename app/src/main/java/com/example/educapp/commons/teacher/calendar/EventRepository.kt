package com.example.educapp.commons.teacher.calendar

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class EventRepository {
    private val firestore: FirebaseFirestore = Firebase.firestore

    suspend fun addEvent(username: String, event: Event): String {
        val docRef = firestore.collection("users")
            .document(username)
            .collection("events")
            .add(event)
            .await()
        return docRef.id
    }

    suspend fun addOrUpdateEvent(username: String, event: Event) {
        // If event.id is non-empty, update; else add a new document
        if (event.id.isNotEmpty()) {
            firestore.collection("users")
                .document(username)
                .collection("events")
                .document(event.id)
                .set(event)
                .await()
        } else {
            addEvent(username, event)
        }
    }

    fun getUpcomingEvents(username: String) = callbackFlow<List<Event>> {
        val now = Timestamp.now()
        val query = firestore.collection("users")
            .document(username)
            .collection("events")
            .whereGreaterThanOrEqualTo("dateTime", now)
            .orderBy("dateTime", Query.Direction.ASCENDING)
        val listener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            val events = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Event::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(events)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getEventById(username: String, eventId: String): Event? {
        val doc = firestore.collection("users")
            .document(username)
            .collection("events")
            .document(eventId)
            .get()
            .await()
        return doc.toObject(Event::class.java)?.copy(id = doc.id)
    }

    suspend fun deleteEvent(username: String, eventId: String) {
        firestore.collection("users")
            .document(username)
            .collection("events")
            .document(eventId)
            .delete()
            .await()
    }
}