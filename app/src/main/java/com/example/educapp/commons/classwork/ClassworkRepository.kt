package com.example.educapp.commons.classwork

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

interface ClassworkRepository {
    // Partial Operations
    suspend fun getClassworkPartials(groupId: String): List<ClassworkPartial>
    fun getPartialsFlow(groupId: String): Flow<List<ClassworkPartial>>
    suspend fun createClassworkPartial(partial: ClassworkPartial): ClassworkPartial
    suspend fun updatePartialStatus(partialId: String, status: ClassworkStatus)

    // Activity Operations
    suspend fun getActivitiesForGroup(groupId: String): List<ClassworkActivity>
    suspend fun getActivitiesForPartial(partialId: String): List<ClassworkActivity>
    suspend fun updateActivityStatus(activityId: String, status: ClassworkStatus)
    suspend fun saveActivities(partialId: String, activities: List<ClassworkActivity>)

    // Material Operations
    suspend fun getMaterialsForGroup(groupId: String): List<ClassworkMaterial>
    suspend fun getMaterialsForActivity(activityId: String): List<ClassworkMaterial>
    suspend fun saveMaterials(activityId: String, materials: List<ClassworkMaterial>)
}

class FirebaseClassworkRepository(
    private val firestore: FirebaseFirestore
) : ClassworkRepository {

    private fun ClassworkActivity.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "partialId" to partialId,
        "title" to title,
        "description" to description,
        "type" to type.name,
        "status" to status.name,
        "dueDate" to (dueDate ?: FieldValue.delete()),
        "aiContext" to aiGenerationContext,
        "createdDate" to Timestamp.now()
    ).filterValues { it != FieldValue.delete() }

    private fun ClassworkPartial.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "groupId" to groupId,
        "partialNumber" to partialNumber,
        "title" to title,
        "status" to status.name,
        "createdDate" to createdDate,
        "lastModified" to lastModified
    )

    private fun ClassworkMaterial.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "activityId" to activityId,
        "type" to type.name as Any,
        "content" to content as Any,
        "description" to description as Any,
        "createdDate" to Timestamp.now() as Any
    )

    // Partial Operations
    override fun getPartialsFlow(groupId: String): Flow<List<ClassworkPartial>> = callbackFlow {
        val query = firestore.collection("classwork_partials")
            .whereEqualTo("groupId", groupId)
            .orderBy("partialNumber")

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firebase", "Partial snapshot error for groupId: $groupId", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            Log.d("Firebase", "Snapshot for groupId $groupId received with ${snapshot?.documents?.size} documents")
            val partials = snapshot?.documents?.mapNotNull { doc ->
                Log.d("Firebase", "Document id: ${doc.id}, data: ${doc.data}")
                try {
                    // Use Firestore's built-in conversion; override id field
                    doc.toObject(ClassworkPartial::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("Firebase", "Error parsing partial ${doc.id}", e)
                    null
                }
            } ?: emptyList()
            Log.d("Firebase", "Returning partials: ${partials.map { it.id to it.groupId }}")
            trySend(partials)
        }
        awaitClose { listener.remove() }
    }



    // Update existing getClassworkPartials to use the same query structure
    override suspend fun getClassworkPartials(groupId: String): List<ClassworkPartial> {
        return try {
            firestore.collection("classwork_partials")
                .whereEqualTo("groupId", groupId)
                .orderBy("partialNumber")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        doc.toObject(ClassworkPartial::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing partial document ${doc.id}")
                        null
                    }
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get classwork partials")
            emptyList()
        }
    }


    override suspend fun createClassworkPartial(partial: ClassworkPartial): ClassworkPartial {
        Log.d("Firebase", "Creating partial in Firestore: $partial")
        return try {
            // Use the same ID for document and partial object
            val docRef = firestore.collection("classwork_partials").document(partial.id)
            docRef.set(partial.toFirestoreMap()).await()
            Log.d("Firebase", "Firestore: Partial created with id: ${partial.id} and groupId: ${partial.groupId}")
            partial
        } catch (e: Exception) {
            Log.e("Firebase", "Firestore: Failed to create partial with id: ${partial.id}", e)
            throw e
        }
    }

    override suspend fun updatePartialStatus(partialId: String, status: ClassworkStatus) {
        try {
            firestore.collection("classwork_partials")
                .document(partialId)
                .update("status", status.name)
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to update partial status")
            throw e // Or handle error as needed
        }
    }

    // Activity Operations
    override suspend fun getActivitiesForGroup(groupId: String): List<ClassworkActivity> {
        return try {
            val partialIds = getClassworkPartials(groupId).map { it.id }
            partialIds.chunked(10).flatMap { batch ->
                firestore.collection("classwork_activities")
                    .whereIn("partialId", batch)
                    .get()
                    .await()
                    .toObjects(ClassworkActivity::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getActivitiesForPartial(partialId: String): List<ClassworkActivity> {
        return try {
            firestore.collection("classwork_activities")
                .whereEqualTo("partialId", partialId)
                .orderBy("createdDate")
                .get()
                .await()
                .toObjects(ClassworkActivity::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun updateActivityStatus(activityId: String, status: ClassworkStatus) {
        firestore.collection("classwork_activities")
            .document(activityId)
            .update("status", status.name)
            .await()
    }

    override suspend fun saveActivities(partialId: String, activities: List<ClassworkActivity>) {
        val batch = firestore.batch()

        activities.forEach { activity ->
            // Use the provided activity.id (already generated in the view model)
            val activityRef = firestore.collection("classwork_activities").document(activity.id)

            // Ensure the partialId matches the saved partial's id
            val newActivity = activity.copy(partialId = partialId)
            batch.set(activityRef, newActivity.toFirestoreMap())

            // Save materials using the IDs from the view model
            newActivity.materials.forEach { material ->
                val materialRef = firestore.collection("classwork_materials").document(material.id)
                batch.set(materialRef, material.toFirestoreMap())
            }
        }

        try {
            batch.commit().await()
        } catch (e: Exception) {
            Timber.e(e, "Batch commit failed")
            throw e
        }
    }

    // Material Operations
    override suspend fun getMaterialsForGroup(groupId: String): List<ClassworkMaterial> {
        return try {
            val activityIds = getActivitiesForGroup(groupId).map { it.id }
            activityIds.chunked(10).flatMap { batch ->
                firestore.collection("classwork_materials")
                    .whereIn("activityId", batch)
                    .get()
                    .await()
                    .toObjects(ClassworkMaterial::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMaterialsForActivity(activityId: String): List<ClassworkMaterial> {
        return try {
            firestore.collection("classwork_materials")
                .whereEqualTo("activityId", activityId)
                .get()
                .await()
                .toObjects(ClassworkMaterial::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveMaterials(activityId: String, materials: List<ClassworkMaterial>) {
        val batch = firestore.batch()

        materials.forEach { material ->
            val materialRef = firestore.collection("classwork_materials").document()
            val newMaterial = material.copy(
                id = materialRef.id,
                activityId = activityId
            )
            batch.set(materialRef, newMaterial.toFirestoreMap())
        }

        try {
            batch.commit().await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save materials for activity $activityId")
            throw e
        }
    }
}