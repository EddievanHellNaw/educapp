package com.example.educapp.commons.teacher.grading

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.educapp.commons.teacher.attendance.AttendanceGroup
import com.example.educapp.commons.teacher.attendance.AttendanceRecord
import com.example.educapp.commons.teacher.attendance.AttendanceStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

data class StudentGrade(
    val studentName: String,
    val groupId: String = "",
    val partial: Int = 0,
    val noFaltas: Int = 0,
    val oralGrV: Int = 0,        // Subgrade for Gr.V (0-3)
    val oralDM: Int = 0,         // Subgrade for DM (0-3)
    val oralPron: Int = 0,       // Subgrade for Pron (0-3)
    val oralIntCom: Int = 0,
    val oral: Int = 0,
    val written: Int = 0,
    val portfolio: Int = 0
    ){
    // Compute final grade on the fly
    val finalGrade: Int
        get() = oral + written + portfolio
    // Secondary no-argument constructor for Firestore deserialization
    constructor() : this(
        studentName = "",
        groupId= "",
        partial = 0,
        noFaltas = 0,
        oralGrV = 0,
        oralDM = 0,
        oralPron = 0,
        oralIntCom = 0,
        oral = 0,
        written = 0,
        portfolio = 0
    )
}

private val sumToOralGrade = mapOf(
    0 to 0, 1 to 0,
    2 to 3, 3 to 6, 4 to 9, 5 to 12, 6 to 15, 7 to 17,
    8 to 20, 9 to 22, 10 to 25, 11 to 28, 12 to 30
)

fun computeOralGrade(gv: Int, dm: Int, pron: Int, intCom: Int): Int {
    val sum = gv + dm + pron + intCom
    return sumToOralGrade[sum] ?: 0
}


class GradesViewModel(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _groups = mutableStateListOf<AttendanceGroup>()
    val groups: List<AttendanceGroup> = _groups

    private var groupsListenerRegistration: ListenerRegistration? = null

    private val _studentGrades = MutableStateFlow<List<StudentGrade>>(emptyList())
    val studentGrades: StateFlow<List<StudentGrade>> = _studentGrades.asStateFlow()

    fun startGroupsListener(teacherId: String) {
        viewModelScope.launch {
            groupsListenerRegistration = firestore.collection("groups")
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener { querySnapshot, e ->
                    if (e != null) {
                        Timber.tag("GradesViewModel").w(e, "Error getting groups")
                        return@addSnapshotListener
                    }
                    _groups.clear()
                    for (document in querySnapshot!!) {
                        val group = document.toObject(AttendanceGroup::class.java)
                        group.id = document.id
                        _groups.add(group)
                    }
                }
            Log.d("GradesViewModel", "teacherId: $teacherId")
        }
    }

    fun stopGroupsListener() {
        groupsListenerRegistration?.remove()
        groupsListenerRegistration = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
        stopGroupsListener()
    }

    fun loadStudentGrades(groupId: String, partial: Int) {
        viewModelScope.launch {
            try {
                // 1. Fetch all existing grade documents for this group and partial.
                val gradeQuerySnapshot = firestore.collection("grades")
                    .whereEqualTo("groupId", groupId)
                    .whereEqualTo("partial", partial)
                    .get()
                    .await()
                Timber.d("Retrieved ${gradeQuerySnapshot.size()} grade documents")

                // Map existing documents to a map keyed by studentName.
                val existingGrades = gradeQuerySnapshot.documents.mapNotNull { document ->
                    document.toObject(StudentGrade::class.java)?.let { grade ->
                        grade.studentName to grade
                    }
                }.toMap()

                // 2. Load the group document to get the full list of students.
                val groupDoc = firestore.collection("groups").document(groupId).get().await()
                val group = groupDoc.toObject(AttendanceGroup::class.java)
                if (group != null) {
                    // 3. Generate default StudentGrade objects for all students.
                    val defaultGrades = group.students.map { studentName ->
                        StudentGrade(
                            studentName = studentName,
                            partial = partial,
                            noFaltas = 0,
                            oralGrV = 0,
                            oralDM = 0,
                            oralPron = 0,
                            oralIntCom = 0,
                            oral = 0,
                            written = 0,
                            portfolio = 0
                        )
                    }.associateBy { it.studentName }

                    // 4. Merge: override defaults with any existing values.
                    val mergedGrades = group.students.map { studentName ->
                        existingGrades[studentName] ?: defaultGrades[studentName]!!
                    }.sortedBy { it.studentName }
                    _studentGrades.value = mergedGrades

                    // Optionally: Save missing default documents to Firestore.
                    mergedGrades.forEach { grade ->
                        val docId = "$groupId-$partial-${grade.studentName}"
                        firestore.collection("grades").document(docId).set(grade).await()
                    }
                } else {
                    _studentGrades.value = emptyList()
                }
            } catch (e: Exception) {
                Timber.tag("GradesViewModel").w(e, "Error loading student grades")
                _studentGrades.value = emptyList()
            }
        }
    }

    fun computeStudentGradesForGroup(group: AttendanceGroup, records: List<AttendanceRecord>, partial: Int): List<StudentGrade> {
        return group.students.map { studentName ->
            val absenceCount = records.count { it.student == studentName && it.status == AttendanceStatus.ABSENT }
            Log.d("computeStudentGrades", "computeStudentGrades: Absence count for $studentName: $absenceCount")
            StudentGrade(
                studentName = studentName,
                partial = partial, // Set the partial value here
                noFaltas = absenceCount,
                oralGrV = 0,
                oralDM = 0,
                oralPron = 0,
                oralIntCom = 0,
                oral = 0,
                written = 0,
                portfolio = 0
            )
        }
    }


    fun updateOralGrade(
    studentName: String,
    groupId: String,
    partial: Int,
    grv: Int,
    dm: Int,
    pron: Int,
    intCom: Int
    ) {
        viewModelScope.launch {
            try {
                // Compute the final oral grade using the helper
                val finalOral = computeOralGrade(grv, dm, pron, intCom)

                // Update the local state (assuming _studentGrades is a MutableStateFlow<List<StudentGrade>>)
                _studentGrades.value = _studentGrades.value.map { grade ->
                    if (grade.studentName == studentName) {
                        grade.copy(
                            oral = finalOral,
                            oralGrV = grv,
                            oralDM = dm,
                            oralPron = pron,
                            oralIntCom = intCom,
                        )
                    } else {
                        grade
                    }
                }

                // Define a document ID scheme. For example, combining groupId, partial and studentName:
                val docId = "$groupId-$partial-$studentName"

                // Update or merge the grade document in Firestore
                firestore.collection("grades").document(docId)
                    .set(
                        mapOf(
                            "studentName" to studentName,
                            "groupId" to groupId,
                            "partial" to partial,
                            "oral" to finalOral,
                            "oralGrV" to grv,
                            "oralDM" to dm,
                            "oralPron" to pron,
                            "oralIntCom" to intCom
                        ),
                        SetOptions.merge()
                    )
                    .await()
            } catch (e: Exception) {
                Timber.tag("GradesViewModel").w(e, "Error updating oral grade")
            }
        }
    }

    fun updateWrittenPortfolio(
        studentName: String,
        groupId: String,
        partial: Int,
        newWritten: Int,
        newPortfolio: Int
    ) {
        viewModelScope.launch {
            try {
                // Update the local state first
                _studentGrades.value = _studentGrades.value.map { grade ->
                    if (grade.studentName == studentName) {
                        grade.copy(
                            written = newWritten,
                            portfolio = newPortfolio
                            // finalGrade is computed on the fly (oral + written + portfolio)
                        )
                    } else {
                        grade
                    }
                }

                // Define the docId for Firestore
                val docId = "$groupId-$partial-$studentName"

                // Merge the new fields into the existing document
                firestore.collection("grades")
                    .document(docId)
                    .set(
                        mapOf(
                            "studentName" to studentName,
                            "groupId" to groupId,
                            "partial" to partial,
                            "written" to newWritten,
                            "portfolio" to newPortfolio
                        ),
                        SetOptions.merge()
                    )
                    .await()

            } catch (e: Exception) {
                Timber.tag("GradesViewModel").w(e, "Error updating written/portfolio")
            }
        }
    }

}