package com.example.educapp.commons.teacher.calendar

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

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

    /**
     * Imported university-calendar events use a stable document ID generated
     * from sourceKey. Importing the same academic calendar twice therefore
     * updates the same Firestore document instead of creating duplicates.
     *
     * Stage 1 only prepares this method. The parser will start calling it in
     * a later stage.
     */
    suspend fun upsertImportedEvent(
        username: String,
        event: Event
    ): String {
        require(event.sourceKey.isNotBlank()) {
            "Imported events require a non-empty sourceKey."
        }

        val documentId = stableDocumentId(event.sourceKey)

        firestore.collection("users")
            .document(username)
            .collection("events")
            .document(documentId)
            .set(event.copy(id = documentId))
            .await()

        return documentId
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

    private fun stableDocumentId(sourceKey: String): String {
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(sourceKey.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }
}
