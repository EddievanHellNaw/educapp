package com.example.educapp.commons.teacher.grading

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.educapp.commons.teacher.attendance.AttendanceGroup
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
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate
import java.time.ZoneId
import java.text.Normalizer
import com.tom_roush.pdfbox.text.PDFTextStripper


data class StudentGrade(
    val studentName: String = "",
    val groupId: String = "",
    val partial: Int = 0,
    val noFaltas: Int = 0,

    val oralGrV: Int = 0,
    val oralDM: Int = 0,
    val oralPron: Int = 0,
    val oralIntCom: Int = 0,
    val oral: Int = 0,

    val written: Int = 0,
    val portfolio: Int = 0,

    val oralCompleted: Boolean = false,
    val writtenCompleted: Boolean = false
) {
    val finalGrade: Int
        get() = oral + written + portfolio
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

private data class PdfStudentSlot(
    val number: Int,
    val pageIndex: Int,
    val column: Int,
    val row: Int,
    val pdfName: String
)
private fun normalizeStudentNameForPdf(
    name: String
): String {

    return Normalizer
        .normalize(
            name.uppercase(),
            Normalizer.Form.NFD
        )
        .replace(
            Regex("\\p{Mn}+"),
            ""
        )
        .replace(
            Regex("[^A-Z0-9]"),
            ""
        )
}

private fun extractPdfStudentSlots(
    document: PDDocument
): Map<String, PdfStudentSlot> {

    val text =
        PDFTextStripper()
            .apply {
                sortByPosition = true
            }
            .getText(document)

    Timber.tag("ExportPDF")
        .d(
            "Extracted PDF text:\n$text"
        )


    /*
     * Matches:
     *
     * 1 0395319 CABAÑAS SANCHEZ SOFIA
     *
     * Also tolerates another student beginning
     * later on the same extracted line.
     */
    val studentPattern =
        Regex(
            """(\d{1,2})\s+(\d{7})\s+([A-ZÁÉÍÓÚÜÑ][A-ZÁÉÍÓÚÜÑ ]+?)(?=\s+\d{1,2}\s+\d{7}\b|$)""",
            setOf(
                RegexOption.IGNORE_CASE,
                RegexOption.MULTILINE
            )
        )


    val result =
        mutableMapOf<String, PdfStudentSlot>()


    studentPattern
        .findAll(text)
        .forEach { match ->

            val number =
                match.groupValues[1]
                    .toIntOrNull()
                    ?: return@forEach


            /*
             * Ignore values that could not reasonably
             * be student roster positions.
             */
            if (number !in 1..60) {
                return@forEach
            }


            val name =
                match.groupValues[3]
                    .trim()
                    .replace(
                        Regex("\\s+"),
                        " "
                    )


            val pageIndex =
                (number - 1) / 12


            val slotOnPage =
                (number - 1) % 12


            val column =
                if (slotOnPage < 6) {
                    0
                } else {
                    1
                }


            val row =
                if (column == 0) {
                    slotOnPage
                } else {
                    slotOnPage - 6
                }


            val normalizedName =
                normalizeStudentNameForPdf(
                    name
                )


            result[normalizedName] =
                PdfStudentSlot(
                    number = number,
                    pageIndex = pageIndex,
                    column = column,
                    row = row,
                    pdfName = name
                )


            Timber.tag("ExportPDF")
                .d(
                    "Student matched: " +
                            "#$number $name | " +
                            "page=${pageIndex + 1} " +
                            "column=$column row=$row"
                )
        }


    Timber.tag("ExportPDF")
        .d(
            "Total students located in PDF: " +
                    result.size
        )


    return result
}

private object GradePdfLayout {

    /*
     * The current DUI attendance PDF is a
     * US-Letter style page of approximately
     * 612 x 792 PDF points.
     */


    /*
     * Distance between the left and right
     * student blocks.
     */
    const val RIGHT_COLUMN_OFFSET =
        307.0f


    /*
     * Vertical distance between students.
     */
    const val ROW_STEP =
        101.2f


    /*
     * Y baseline of the detailed Oral fields
     * for the first student.
     */
    const val FIRST_DETAIL_Y =
        630.0f


    /*
     * Y baseline for No. faltas and the
     * Oral/Written/Portfolio/Final scores.
     */
    const val FIRST_MAIN_Y =
        598.0f


    /*
     * X positions in the LEFT student block.
     */
    const val ABSENCES_X =
        106.0f


    const val GRV_X =
        198.0f

    const val DM_X =
        223.0f

    const val PRON_X =
        249.0f

    const val ICOM_X =
        274.0f

    const val ORAL_DETAIL_TOTAL_X =
        297.0f


    const val ORAL_X =
        156.0f

    const val WRITTEN_X =
        199.0f

    const val PORTFOLIO_X =
        243.0f

    const val FINAL_X =
        286.0f
}

private fun pdfX(
    leftColumnX: Float,
    column: Int
): Float {

    return leftColumnX +
            if (column == 1) {
                GradePdfLayout.RIGHT_COLUMN_OFFSET
            } else {
                0f
            }
}


private fun detailY(
    row: Int
): Float {

    return GradePdfLayout.FIRST_DETAIL_Y -
            row * GradePdfLayout.ROW_STEP
}


private fun mainY(
    row: Int
): Float {

    return GradePdfLayout.FIRST_MAIN_Y -
            row * GradePdfLayout.ROW_STEP
}

private fun drawCenteredPdfValue(
    contentStream: PDPageContentStream,
    value: String,
    centerX: Float,
    baselineY: Float,
    fontSize: Float
) {

    if (value.isBlank()) {
        return
    }


    val font =
        PDType1Font.HELVETICA_BOLD


    val textWidth =
        font.getStringWidth(value) /
                1000f *
                fontSize


    contentStream.beginText()

    contentStream.setFont(
        font,
        fontSize
    )

    contentStream.newLineAtOffset(
        centerX -
                textWidth / 2f,

        baselineY
    )

    contentStream.showText(
        value
    )

    contentStream.endText()
}

private fun stampStudentGrade(
    contentStream: PDPageContentStream,
    grade: StudentGrade,
    slot: PdfStudentSlot
) {

    val row =
        slot.row

    val column =
        slot.column


    /*
     * ---------------------------------------
     * ABSENCES
     * ---------------------------------------
     *
     * Always write this value because it comes
     * directly from Attendance.
     */
    drawCenteredPdfValue(
        contentStream = contentStream,

        value =
        grade.noFaltas.toString(),

        centerX =
        pdfX(
            GradePdfLayout.ABSENCES_X,
            column
        ),

        baselineY =
        mainY(row),

        fontSize = 11f
    )


    /*
     * ---------------------------------------
     * ORAL
     * ---------------------------------------
     *
     * Only fill these fields if the oral
     * assessment has actually been completed.
     */
    if (grade.oralCompleted) {

        drawCenteredPdfValue(
            contentStream,
            grade.oralGrV.toString(),
            pdfX(
                GradePdfLayout.GRV_X,
                column
            ),
            detailY(row),
            11f
        )


        drawCenteredPdfValue(
            contentStream,
            grade.oralDM.toString(),
            pdfX(
                GradePdfLayout.DM_X,
                column
            ),
            detailY(row),
            11f
        )


        drawCenteredPdfValue(
            contentStream,
            grade.oralPron.toString(),
            pdfX(
                GradePdfLayout.PRON_X,
                column
            ),
            detailY(row),
            11f
        )


        drawCenteredPdfValue(
            contentStream,
            grade.oralIntCom.toString(),
            pdfX(
                GradePdfLayout.ICOM_X,
                column
            ),
            detailY(row),
            11f
        )


        /*
         * The PDF's "Total" field is out of 12,
         * not the converted Oral score out of 30.
         *
         * Example:
         *
         * 2 + 2 + 2 + 2 = 8
         *
         * which converts to Oral = 20.
         */
        val rawOralTotal =
            grade.oralGrV +
                    grade.oralDM +
                    grade.oralPron +
                    grade.oralIntCom


        drawCenteredPdfValue(
            contentStream,
            rawOralTotal.toString(),
            pdfX(
                GradePdfLayout
                    .ORAL_DETAIL_TOTAL_X,
                column
            ),
            detailY(row),
            11f
        )


        drawCenteredPdfValue(
            contentStream,
            grade.oral.toString(),
            pdfX(
                GradePdfLayout.ORAL_X,
                column
            ),
            mainY(row),
            13f
        )
    }


    /*
     * ---------------------------------------
     * WRITTEN + PORTFOLIO
     * ---------------------------------------
     */
    if (grade.writtenCompleted) {

        drawCenteredPdfValue(
            contentStream,
            grade.written.toString(),
            pdfX(
                GradePdfLayout.WRITTEN_X,
                column
            ),
            mainY(row),
            13f
        )


        drawCenteredPdfValue(
            contentStream,
            grade.portfolio.toString(),
            pdfX(
                GradePdfLayout.PORTFOLIO_X,
                column
            ),
            mainY(row),
            13f
        )
    }


    /*
     * ---------------------------------------
     * FINAL
     * ---------------------------------------
     *
     * Don't print a misleading partial final
     * grade if one assessment is still pending.
     */
    if (
        grade.oralCompleted &&
        grade.writtenCompleted
    ) {

        drawCenteredPdfValue(
            contentStream,
            grade.finalGrade.toString(),
            pdfX(
                GradePdfLayout.FINAL_X,
                column
            ),
            mainY(row),
            14f
        )
    }
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
    private val _currentGroup =
        MutableStateFlow<AttendanceGroup?>(null)

    val currentGroup:
            StateFlow<AttendanceGroup?> =
        _currentGroup.asStateFlow()

    private fun DocumentSnapshot.readAttendanceDateKey(): String? {

        /*
         * Current canonical attendance format.
         */
        val dateKey =
            get("dateKey") as? String

        if (!dateKey.isNullOrBlank()) {
            return dateKey
        }


        /*
         * Timestamp format.
         */
        val rawDate = get("date")

        if (rawDate is Timestamp) {

            return rawDate
                .toDate()
                .toInstant()
                .atZone(
                    ZoneId.systemDefault()
                )
                .toLocalDate()
                .toString()
        }


        /*
         * Legacy LocalDate map format.
         */
        if (rawDate is Map<*, *>) {

            val year =
                (rawDate["year"] as? Number)
                    ?.toInt()

            val month =
                (rawDate["monthValue"] as? Number)
                    ?.toInt()
                    ?: (rawDate["month"] as? Number)
                        ?.toInt()

            val day =
                (rawDate["dayOfMonth"] as? Number)
                    ?.toInt()
                    ?: (rawDate["day"] as? Number)
                        ?.toInt()


            if (
                year != null &&
                month != null &&
                day != null
            ) {

                return try {

                    LocalDate.of(
                        year,
                        month,
                        day
                    ).toString()

                } catch (_: Exception) {
                    null
                }
            }
        }


        if (rawDate is String) {
            return rawDate
        }


        return null
    }

    private suspend fun loadAbsenceCounts(
        groupId: String,
        partial: Int
    ): Map<String, Int> {

        /*
         * Query only by groupId.
         *
         * We filter partial locally, avoiding another
         * Firestore composite-index requirement.
         */
        val snapshot =
            firestore
                .collection("attendance")
                .whereEqualTo(
                    "groupId",
                    groupId
                )
                .get()
                .await()


        /*
         * Attendance went through a migration while we
         * were developing it, so legacy duplicate documents
         * may still exist.
         *
         * Keep only the newest document for:
         *
         * student + partial + date
         */
        val latestRecords =
            mutableMapOf<String, DocumentSnapshot>()


        snapshot.documents.forEach { document ->

            val student =
                document.getString("student")
                    ?: return@forEach

            val recordPartial =
                (document.get("partial") as? Number)
                    ?.toInt()
                    ?: return@forEach


            /*
             * Only absences belonging to the partial
             * being graded should count.
             */
            if (recordPartial != partial) {
                return@forEach
            }


            val dateKey =
                document.readAttendanceDateKey()
                    ?: return@forEach


            val logicalKey =
                "$student|$recordPartial|$dateKey"


            val existing =
                latestRecords[logicalKey]


            val newTimestamp =
                document
                    .getTimestamp("timestamp")
                    ?.seconds
                    ?: 0L


            val existingTimestamp =
                existing
                    ?.getTimestamp("timestamp")
                    ?.seconds
                    ?: Long.MIN_VALUE


            if (
                existing == null ||
                newTimestamp >= existingTimestamp
            ) {

                latestRecords[logicalKey] =
                    document
            }
        }


        /*
         * Now count only records whose final persisted
         * status is ABSENT.
         */
        val counts =
            mutableMapOf<String, Int>()


        latestRecords.values.forEach { document ->

            if (
                document.getString("status") ==
                AttendanceStatus.ABSENT.name
            ) {

                val student =
                    document.getString("student")
                        ?: return@forEach

                counts[student] =
                    (counts[student] ?: 0) + 1
            }
        }


        Timber.tag("GradesViewModel")
            .d(
                "Absence counts for group=$groupId " +
                        "partial=$partial: $counts"
            )


        return counts
    }

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

    fun loadStudentGrades(
        groupId: String,
        partial: Int
    ) {
        viewModelScope.launch {

            try {

                /*
                 * 1. Load group
                 */
                val groupDoc =
                    firestore
                        .collection("groups")
                        .document(groupId)
                        .get()
                        .await()

                val loadedGroup =
                    groupDoc.toObject(
                        AttendanceGroup::class.java
                    )

                if (loadedGroup == null) {

                    _currentGroup.value = null
                    _studentGrades.value = emptyList()

                    return@launch
                }

                loadedGroup.id = groupId
                _currentGroup.value = loadedGroup


                /*
                 * 2. Load existing grade documents
                 */
                val gradeSnapshot =
                    firestore
                        .collection("grades")
                        .whereEqualTo(
                            "groupId",
                            groupId
                        )
                        .whereEqualTo(
                            "partial",
                            partial
                        )
                        .get()
                        .await()


                val existingGrades =
                    gradeSnapshot.documents
                        .mapNotNull { document ->

                            document
                                .toObject(
                                    StudentGrade::class.java
                                )
                                ?.let { grade ->

                                    grade.studentName to grade
                                }
                        }
                        .toMap()


                /*
                 * 3. Calculate absences using attendance DB
                 */
                val absenceCounts =
                    loadAbsenceCounts(
                        groupId = groupId,
                        partial = partial
                    )


                /*
                 * 4. Merge group roster, grades and attendance
                 */
                val mergedGrades =
                    loadedGroup.students
                        .map { studentName ->

                            val existing =
                                existingGrades[studentName]


                            if (existing != null) {

                                /*
                                 * Migration support for grades created
                                 * before oralCompleted/writtenCompleted
                                 * existed.
                                 */
                                val migratedOralCompleted =
                                    existing.oralCompleted ||
                                            existing.oral > 0 ||
                                            existing.oralGrV > 0 ||
                                            existing.oralDM > 0 ||
                                            existing.oralPron > 0 ||
                                            existing.oralIntCom > 0


                                val migratedWrittenCompleted =
                                    existing.writtenCompleted ||
                                            existing.written > 0 ||
                                            existing.portfolio > 0


                                existing.copy(
                                    groupId = groupId,
                                    partial = partial,

                                    /*
                                     * Attendance remains the source
                                     * of truth for absences.
                                     */
                                    noFaltas =
                                    absenceCounts[
                                        studentName
                                    ] ?: 0,

                                    oralCompleted =
                                    migratedOralCompleted,

                                    writtenCompleted =
                                    migratedWrittenCompleted
                                )

                            } else {

                                /*
                                 * Student has no grade document yet.
                                 */
                                StudentGrade(
                                    studentName = studentName,
                                    groupId = groupId,
                                    partial = partial,

                                    noFaltas =
                                    absenceCounts[
                                        studentName
                                    ] ?: 0
                                )
                            }
                        }
                        .sortedBy {
                            it.studentName
                        }


                _studentGrades.value =
                    mergedGrades


                /*
                 * 5. Synchronize Firestore.
                 *
                 * This also permanently stores the migrated
                 * completion flags.
                 */
                mergedGrades.forEach { grade ->

                    val docId =
                        "$groupId-$partial-${grade.studentName}"


                    firestore
                        .collection("grades")
                        .document(docId)
                        .set(
                            grade,
                            SetOptions.merge()
                        )
                        .await()
                }


                Timber.tag("GradesViewModel")
                    .d(
                        "Loaded ${mergedGrades.size} grades. " +
                                "Absences=$absenceCounts"
                    )


            } catch (e: Exception) {

                Timber.tag("GradesViewModel")
                    .e(
                        e,
                        "Error loading student grades"
                    )

                _studentGrades.value =
                    emptyList()
            }
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
                            oralCompleted = true
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
                            "oralIntCom" to intCom,

                            "oralCompleted" to true
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
                            portfolio = newPortfolio,
                            writtenCompleted = true
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
                            "portfolio" to newPortfolio,

                            "writtenCompleted" to true
                        ),
                        SetOptions.merge()
                    )
                    .await()

            } catch (e: Exception) {
                Timber.tag("GradesViewModel").w(e, "Error updating written/portfolio")
            }
        }
    }

    /**
     * Exports the current student grades by overlaying them onto the imported PDF template.
     *
     * Call this function when the user taps on an "Export PDF" or "Print" button.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun exportGradesToPdfFromUri(
        context: Context,
        pdfUri: Uri,
        outputPdfFile: File
    ) {

        viewModelScope.launch(
            Dispatchers.IO
        ) {

            try {

                val grades =
                    _studentGrades.value


                if (grades.isEmpty()) {

                    withContext(
                        Dispatchers.Main
                    ) {

                        Toast.makeText(
                            context,
                            "No grades available to export.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    return@launch
                }


                PDFBoxResourceLoader.init(
                    context
                )


                val document =
                    context.contentResolver
                        .openInputStream(pdfUri)
                        ?.use { inputStream ->

                            PDDocument.load(
                                inputStream
                            )
                        }
                        ?: throw IllegalStateException(
                            "Could not open selected PDF."
                        )


                try {

                    /*
                     * ---------------------------------
                     * 1. Validate + locate students
                     * ---------------------------------
                     */
                    val studentSlots =
                        extractPdfStudentSlots(
                            document
                        )


                    if (studentSlots.isEmpty()) {

                        throw IllegalStateException(
                            "No students could be identified " +
                                    "in the selected PDF."
                        )
                    }


                    /*
                     * Make sure this really is the same
                     * roster as the currently loaded group.
                     */
                    val unmatchedStudents =
                        grades.filter { grade ->

                            normalizeStudentNameForPdf(
                                grade.studentName
                            ) !in studentSlots
                        }


                    if (
                        unmatchedStudents.isNotEmpty()
                    ) {

                        val names =
                            unmatchedStudents
                                .take(4)
                                .joinToString {
                                    it.studentName
                                }


                        throw IllegalStateException(
                            "The selected PDF does not appear " +
                                    "to match this group. " +
                                    "Students not found: $names"
                        )
                    }


                    /*
                     * ---------------------------------
                     * 2. Match grades with PDF slots
                     * ---------------------------------
                     */
                    val exportEntries =
                        grades.mapNotNull { grade ->

                            val slot =
                                studentSlots[
                                    normalizeStudentNameForPdf(
                                        grade.studentName
                                    )
                                ]


                            if (slot == null) {

                                Timber.tag(
                                    "ExportPDF"
                                ).w(
                                    "No PDF slot for " +
                                            grade.studentName
                                )

                                null

                            } else {

                                Pair(
                                    grade,
                                    slot
                                )
                            }
                        }


                    /*
                     * ---------------------------------
                     * 3. Write page by page
                     * ---------------------------------
                     */
                    exportEntries
                        .groupBy {
                            it.second.pageIndex
                        }
                        .forEach {
                                (
                                    pageIndex,
                                    pageEntries
                                ) ->


                            if (
                                pageIndex !in
                                0 until
                                document.numberOfPages
                            ) {

                                Timber.tag(
                                    "ExportPDF"
                                ).w(
                                    "PDF does not contain " +
                                            "page $pageIndex"
                                )

                                return@forEach
                            }


                            val page =
                                document.getPage(
                                    pageIndex
                                )


                            val contentStream =
                                PDPageContentStream(
                                    document,
                                    page,

                                    PDPageContentStream
                                        .AppendMode
                                        .APPEND,

                                    true,
                                    true
                                )


                            try {

                                /*
                                 * Blue grading ink similar
                                 * to your example.
                                 */
                                contentStream
                                    .setNonStrokingColor(
                                        0,
                                        105,
                                        220
                                    )


                                pageEntries
                                    .forEach {
                                            (
                                                grade,
                                                slot
                                            ) ->


                                        Timber.tag(
                                            "ExportPDF"
                                        ).d(
                                            "Writing " +
                                                    grade.studentName +
                                                    " -> page " +
                                                    (pageIndex + 1) +
                                                    ", row " +
                                                    slot.row +
                                                    ", col " +
                                                    slot.column
                                        )


                                        stampStudentGrade(
                                            contentStream =
                                            contentStream,

                                            grade =
                                            grade,

                                            slot =
                                            slot
                                        )
                                    }


                            } finally {

                                contentStream.close()
                            }
                        }


                    /*
                     * ---------------------------------
                     * 4. Save final PDF
                     * ---------------------------------
                     */
                    document.save(
                        outputPdfFile
                    )


                } finally {

                    document.close()
                }


                /*
                 * ---------------------------------
                 * 5. Copy finished file to Downloads
                 * ---------------------------------
                 *
                 * IMPORTANT:
                 *
                 * Do this AFTER document.save().
                 */
                val partial =
                    grades.firstOrNull()
                        ?.partial
                        ?: 0


                val groupName =
                    _currentGroup.value
                        ?.name
                        ?: "Group"


                val safeGroupName =
                    groupName
                        .replace(
                            Regex(
                                "[^A-Za-z0-9_-]"
                            ),
                            "_"
                        )


                val displayName =
                    "${safeGroupName}_" +
                            "Partial_${partial}_" +
                            "Grades.pdf"


                copyFileToDownloads(
                    context = context,

                    sourceFile =
                    outputPdfFile,

                    displayName =
                    displayName
                )


                withContext(
                    Dispatchers.Main
                ) {

                    Toast.makeText(
                        context,

                        "Grades exported to Downloads:\n" +
                                displayName,

                        Toast.LENGTH_LONG
                    ).show()
                }


            } catch (e: Exception) {

                Timber.tag("ExportPDF")
                    .e(
                        e,
                        "Error exporting grades"
                    )


                withContext(
                    Dispatchers.Main
                ) {

                    Toast.makeText(
                        context,

                        "Could not export PDF: " +
                                (
                                        e.localizedMessage
                                            ?: "Unknown error"
                                        ),

                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun copyFileToDownloads(
        context: Context,
        sourceFile: File,
        displayName: String =
            sourceFile.name
    ) {

        if (!sourceFile.exists()) {

            throw IllegalStateException(
                "Generated PDF does not exist: " +
                        sourceFile.absolutePath
            )
        }


        val resolver =
            context.contentResolver


        val contentValues =
            ContentValues().apply {

                put(
                    MediaStore.Downloads.DISPLAY_NAME,
                    displayName
                )

                put(
                    MediaStore.Downloads.MIME_TYPE,
                    "application/pdf"
                )

                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS
                )

                put(
                    MediaStore.Downloads.IS_PENDING,
                    1
                )
            }


        val uri =
            resolver.insert(
                MediaStore.Downloads
                    .EXTERNAL_CONTENT_URI,

                contentValues
            )
                ?: throw IllegalStateException(
                    "Android could not create " +
                            "the PDF in Downloads."
                )


        try {

            resolver
                .openOutputStream(uri)
                ?.use { outputStream ->

                    FileInputStream(
                        sourceFile
                    ).use { inputStream ->

                        inputStream.copyTo(
                            outputStream
                        )
                    }
                }
                ?: throw IllegalStateException(
                    "Could not open Downloads " +
                            "output stream."
                )


            /*
             * File is complete.
             */
            val finishedValues =
                ContentValues().apply {

                    put(
                        MediaStore.Downloads.IS_PENDING,
                        0
                    )
                }


            resolver.update(
                uri,
                finishedValues,
                null,
                null
            )


            Timber.tag("ExportPDF")
                .d(
                    "PDF successfully copied: " +
                            displayName
                )


        } catch (e: Exception) {

            /*
             * Don't leave a broken file entry behind.
             */
            resolver.delete(
                uri,
                null,
                null
            )

            throw e
        }
    }
}