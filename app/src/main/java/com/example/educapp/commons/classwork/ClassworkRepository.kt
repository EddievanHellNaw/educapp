package com.example.educapp.commons.classwork

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.ZoneId

interface ClassworkRepository {
    // Partial Operations
    suspend fun getClassworkPartials(groupId: String): List<ClassworkPartial>
    suspend fun createClassworkPartial(partial: ClassworkPartial)
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
        "dueDate" to (dueDate?.let {
            Timestamp(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
        } ?: FieldValue.delete()), // Handle null dates
        "aiContext" to aiGenerationContext,
        "createdDate" to Timestamp.now() as Any // Explicit cast
    ).filterValues { it != FieldValue.delete() } // Remove deleted fields

    private fun ClassworkMaterial.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "activityId" to activityId,
        "type" to type.name as Any,
        "content" to content as Any,
        "description" to description as Any,
        "createdDate" to Timestamp.now() as Any
    )

    private fun ClassworkPartial.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "groupId" to groupId,
        "partialNumber" to partialNumber,
        "title" to title,
        "status" to status.name as Any,
        "createdDate" to Timestamp.now() as Any,
        "lastModified" to Timestamp.now() as Any
    )

    // Partial Operations
    override suspend fun getClassworkPartials(groupId: String): List<ClassworkPartial> {
        return try {
            firestore.collection("classwork_partials")
                .whereEqualTo("groupId", groupId)
                .orderBy("partialNumber")
                .get()
                .await()
                .toObjects(ClassworkPartial::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun createClassworkPartial(partial: ClassworkPartial) {
        val docRef = firestore.collection("classwork_partials").document()
        val newPartial = partial.copy(id = docRef.id)
        docRef.set(newPartial.toFirestoreMap()).await()
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
            // Save activity
            val activityRef = firestore.collection("classwork_activities").document()
            val newActivity = activity.copy(id = activityRef.id, partialId = partialId)
            batch.set(activityRef, newActivity.toFirestoreMap())

            // Save materials
            activity.materials.forEach { material ->
                val materialRef = firestore.collection("classwork_materials").document()
                val newMaterial = material.copy(
                    id = materialRef.id,
                    activityId = activityRef.id
                )
                batch.set(materialRef, newMaterial.toFirestoreMap())
            }
        }

        batch.commit().await()
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