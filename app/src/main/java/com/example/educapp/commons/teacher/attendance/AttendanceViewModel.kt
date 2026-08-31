package com.example.educapp.commons.teacher.attendance

import java.io.FileOutputStream
import java.text.Normalizer
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.googlecode.tesseract.android.TessBaseAPI
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.flow.update
import java.io.File
import kotlin.text.Regex
import android.graphics.Matrix
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException


data class AttendanceGroup(
    var id: String = "",
    var name: String = "",
    var schedule: String = "",
    var students: List<String> = emptyList(),

    // Student name -> Base64 JPEG thumbnail
    val studentPhotos: Map<String, String> = emptyMap(),

    val teacherId: String = "",
    val color: Int = Color(0xFF6A5ACD).toArgb(),
    val order: Long = 0L
) {
    fun getColor(): Color = Color(color)
}

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE
}

data class AttendanceRecord(
    val student: String = "",
    val groupId: String = "",
    val partial: Int = 0,
    val status: AttendanceStatus? = null,
    val date: LocalDate = LocalDate.now(),
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
)

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val importedStudents: List<String> = emptyList(),
    val showImportSuccess: Boolean = false,
    val errorMessage: String? = null
)




class AttendanceViewModel (
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val _groups = mutableStateListOf<AttendanceGroup>()
    val groups: List<AttendanceGroup> = _groups
    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()
    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()
    private val _snackbarMessage = MutableStateFlow<Event<String>?>(null)
    private var groupsListenerRegistration: ListenerRegistration? = null
    private val saveAttendanceJob = Job()
    private val _attendanceError =
        MutableStateFlow<String?>(null)

    val attendanceError:
            StateFlow<String?> =
        _attendanceError.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init{
        viewModelScope.launch {
            val currentTeacherId = getCurrentTeacherId()
            startGroupsListener(currentTeacherId)
        }
    }

    private fun LocalDate.toFirestoreTimestamp(): Timestamp {
        return Timestamp(
            atStartOfDay(
                ZoneId.systemDefault()
            ).toEpochSecond(),
            0
        )
    }
    private fun attendanceLogicalKey(
        record: AttendanceRecord
    ): String {

        val normalizedStudent =
            normalizeForMatch(record.student)
                .trim()
                .lowercase()

        return buildString {
            append(record.groupId)
            append("|")
            append(record.partial)
            append("|")
            append(record.date)
            append("|")
            append(normalizedStudent)
        }
    }


    private fun attendanceDocumentId(
        record: AttendanceRecord
    ): String {

        val rawKey =
            attendanceLogicalKey(record)

        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    rawKey.toByteArray()
                )

        return digest.joinToString("") { byte ->
            "%02x".format(
                byte.toInt() and 0xff
            )
        }
    }

    private fun DocumentSnapshot.readAttendanceDate(): LocalDate? {

        /*
         * --------------------------------------------------
         * 1. NEW FORMAT: dateKey = "2026-08-27"
         * --------------------------------------------------
         *
         * Use get() instead of getString() so a malformed
         * value cannot trigger an unexpected type conversion.
         */
        val dateKey =
            get("dateKey") as? String

        if (!dateKey.isNullOrBlank()) {

            try {
                return LocalDate.parse(dateKey)
            } catch (e: Exception) {

                Timber.tag("AttendanceDatabase")
                    .w(
                        e,
                        "Invalid dateKey '$dateKey' in document $id"
                    )
            }
        }


        /*
         * --------------------------------------------------
         * 2. Inspect the raw date value FIRST.
         * --------------------------------------------------
         *
         * This is important for backwards compatibility.
         *
         * Do NOT call getTimestamp("date") before checking
         * what type the old Firestore value actually is.
         */
        val rawDate = get("date")


        /*
         * NEW FORMAT:
         * Firestore Timestamp
         */
        if (rawDate is Timestamp) {

            return rawDate
                .toDate()
                .toInstant()
                .atZone(
                    ZoneId.systemDefault()
                )
                .toLocalDate()
        }


        /*
         * Possible legacy Date representation.
         */
        if (rawDate is java.util.Date) {

            return rawDate
                .toInstant()
                .atZone(
                    ZoneId.systemDefault()
                )
                .toLocalDate()
        }


        /*
         * Possible ISO text representation.
         *
         * Example:
         * "2026-08-27"
         */
        if (rawDate is String) {

            try {

                return LocalDate.parse(
                    rawDate
                )

            } catch (e: Exception) {

                Timber.tag("AttendanceDatabase")
                    .w(
                        e,
                        "Invalid string date '$rawDate' in document $id"
                    )
            }
        }


        /*
         * --------------------------------------------------
         * 3. LEGACY FORMAT
         * --------------------------------------------------
         *
         * Previous AttendanceRecord objects may have
         * serialized LocalDate as a map.
         *
         * {
         *     year: 2026,
         *     monthValue: 8,
         *     dayOfMonth: 27
         * }
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
                    )

                } catch (e: Exception) {

                    Timber.tag("AttendanceDatabase")
                        .w(
                            e,
                            "Invalid legacy date in document $id: " +
                                    "$year-$month-$day"
                        )

                    null
                }
            }


            Timber.tag("AttendanceDatabase")
                .w(
                    "Legacy date map in document $id " +
                            "does not contain expected fields: $rawDate"
                )
        }


        Timber.tag("AttendanceDatabase")
            .w(
                "Unable to determine attendance date " +
                        "for document $id. Raw date=$rawDate"
            )

        return null
    }


    private suspend fun writeAndVerifyAttendanceRecord(
        record: AttendanceRecord
    ): AttendanceRecord {

        val status =
            record.status
                ?: throw IllegalArgumentException(
                    "Attendance status cannot be null"
                )


        /*
         * Deterministic document ID.
         *
         * The same student/group/partial/date ALWAYS
         * writes to the same Firestore document.
         */
        val documentId =
            attendanceDocumentId(record)

        val reference =
            firestore
                .collection("attendance")
                .document(documentId)


        val data =
            mapOf(
                "student" to
                        record.student,

                "groupId" to
                        record.groupId,

                "partial" to
                        record.partial,

                "status" to
                        status.name,

                "date" to
                        record.date
                            .toFirestoreTimestamp(),

                "dateKey" to
                        record.date.toString(),

                "timestamp" to
                        FieldValue.serverTimestamp()
            )


        Timber.tag("AttendanceDatabase")
            .d(
                "Writing canonical attendance doc: " +
                        "$documentId | " +
                        "${record.student} | " +
                        "${record.date} | " +
                        status.name
            )


        /*
         * Write and wait for Firebase.
         */
        reference
            .set(
                data,
                SetOptions.merge()
            )
            .await()


        /*
         * Then explicitly read FROM SERVER.
         */
        val verifiedDocument =
            reference
                .get(Source.SERVER)
                .await()


        if (!verifiedDocument.exists()) {

            throw IllegalStateException(
                "Attendance document was not found " +
                        "after Firebase acknowledged the write."
            )
        }


        val verifiedStudent =
            verifiedDocument
                .getString("student")

        val verifiedGroupId =
            verifiedDocument
                .getString("groupId")

        val verifiedPartial =
            (verifiedDocument.get("partial") as? Number)
                ?.toInt()

        val verifiedStatus =
            verifiedDocument
                .getString("status")

        val verifiedDate =
            verifiedDocument
                .readAttendanceDate()


        if (
            verifiedStudent != record.student ||
            verifiedGroupId != record.groupId ||
            verifiedPartial != record.partial ||
            verifiedStatus != status.name ||
            verifiedDate != record.date
        ) {

            throw IllegalStateException(
                "Server verification failed.\n" +
                        "Expected: " +
                        "${record.student} | " +
                        "${record.groupId} | " +
                        "P${record.partial} | " +
                        "${record.date} | " +
                        "${status.name}\n" +
                        "Received: " +
                        "$verifiedStudent | " +
                        "$verifiedGroupId | " +
                        "P$verifiedPartial | " +
                        "$verifiedDate | " +
                        "$verifiedStatus"
            )
        }


        val serverTimestamp =
            verifiedDocument
                .getTimestamp("timestamp")
                ?: Timestamp.now()


        Timber.tag("AttendanceDatabase")
            .d(
                "Canonical attendance SERVER VERIFIED: " +
                        "$documentId"
            )


        return record.copy(
            timestamp = serverTimestamp
        )
    }

    private suspend fun writeAttendanceWithRetry(
        record: AttendanceRecord,
        attempts: Int = 3
    ): AttendanceRecord {

        var lastError: Exception? = null


        repeat(attempts) { attempt ->

            try {

                Timber
                    .tag("AttendanceDatabase")
                    .d(
                        "Saving ${record.student}, " +
                                "${record.date}, " +
                                "${record.status}. " +
                                "Attempt ${attempt + 1}/$attempts"
                    )


                val verified =
                    writeAndVerifyAttendanceRecord(
                        record
                    )


                Timber
                    .tag("AttendanceDatabase")
                    .d(
                        "SERVER CONFIRMED: " +
                                "${record.student} - " +
                                "${record.date} - " +
                                "${record.status}"
                    )


                return verified


            } catch (e: Exception) {

                lastError = e


                Timber
                    .tag("AttendanceDatabase")
                    .w(
                        e,
                        "Attendance write attempt " +
                                "${attempt + 1} failed"
                    )


                if (
                    attempt <
                    attempts - 1
                ) {

                    /*
                     * 400ms
                     * 800ms
                     * then fail.
                     */
                    delay(
                        400L *
                                (attempt + 1)
                    )
                }
            }
        }


        throw lastError
            ?: IllegalStateException(
                "Attendance could not be saved."
            )
    }

    private fun normalizeForMatch(input: String): String {
        // Converts "INGLÉS" (and decomposed variants) -> "INGLES"
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "") // remove diacritics
    }

    private fun saveDebugText(context: Context, text: String, filename: String) {
        try {
            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { it.write(text.toByteArray()) }
            Log.d("PDF_DEBUG", "Saved debug file: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("PDF_DEBUG", "Failed to save debug file", e)
        }
    }

    private fun logExtractionProcess(lines: List<String>) {
        Log.d("PDF_DEBUG", "=== PDF STRUCTURE ANALYSIS ===")
        lines.forEachIndexed { i, line ->
            Log.d("PDF_DEBUG", "Line $i: $line")
            when {
                line.matches(Regex("""^\d+\s+\d{7}$""")) ->
                    Log.d("PDF_DEBUG", "--> ID LINE DETECTED")
                line == "L" ->
                    Log.d("PDF_DEBUG", "--> L LINE DETECTED")
                line.contains("Asistencia") ->
                    Log.d("PDF_DEBUG", "--> ASISTENCIA HEADER")
                line.contains("M M J V") ->
                    Log.d("PDF_DEBUG", "--> ATTENDANCE MARKERS")
            }
        }
    }

    fun startGroupsListener(teacherId: String) {
        viewModelScope.launch {
            groupsListenerRegistration = firestore.collection("groups")
                .whereEqualTo("teacherId", teacherId)
                .orderBy("order")
                .addSnapshotListener { querySnapshot, e ->
                    if (e != null) {
                        Timber.tag("AttendanceViewModel").w(e, "Error getting groups")
                        return@addSnapshotListener
                    }

                    _groups.clear()
                    for (document in querySnapshot!!) {
                        val group = document.toObject(AttendanceGroup::class.java)
                        group.id = document.id
                        _groups.add(group)
                    }
                }
        }
    }

    fun stopGroupsListener() {
        groupsListenerRegistration?.remove()
        groupsListenerRegistration = null
    }

    private fun getCurrentTeacherId(): String {
        return Firebase.auth.currentUser?.uid ?: ""
    }

    fun moveGroup(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in _groups.indices) return
        if (toIndex !in _groups.indices) return

        val item = _groups.removeAt(fromIndex)
        _groups.add(toIndex, item)
    }

    fun persistGroupsOrder(teacherId: String) {
        viewModelScope.launch {

            try {

                val db = Firebase.firestore
                val batch = db.batch()

                _groups.forEachIndexed { index, group ->

                    if (group.teacherId == teacherId) {

                        val ref = db
                            .collection("groups")
                            .document(group.id)

                        batch.update(
                            ref,
                            "order",
                            index.toLong()
                        )
                    }
                }

                batch.commit().await()

                Timber.tag("AttendanceViewModel")
                    .d("Group order saved successfully")

            } catch (e: Exception) {

                Timber.tag("AttendanceViewModel")
                    .e(e, "Error saving group order")
            }
        }
    }


    private data class ImportedStudent(
        val number: Int,
        val name: String
    )

    fun saveGroup(group: AttendanceGroup, teacherId: String) {
        viewModelScope.launch {
            val db = Firebase.firestore
            val nextOrder = (_groups.maxOfOrNull { it.order } ?: -1L) + 1L
            val data = mapOf(
                "name" to group.name,
                "schedule" to group.schedule,
                "students" to group.students,
                "studentPhotos" to group.studentPhotos,
                "color" to group.color,
                "teacherId" to teacherId,
                "order" to nextOrder
            )
            db.collection("groups").add(data)
                .addOnSuccessListener { documentReference ->
                    Timber.tag("AttendanceViewModel")
                        .d("Group added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Timber.tag("AttendanceViewModel").w(e, "Error adding group")
                }
        }
    }

    fun updateGroup(group: AttendanceGroup, currentTeacherId: String) {
        viewModelScope.launch {
            val db = Firebase.firestore
            try {
                if (group.teacherId == currentTeacherId) {
                    val querySnapshot = db.collection("groups").whereEqualTo("name", group.name).get().await()
                    if (querySnapshot.documents.isNotEmpty()) { // Check if documents exist
                        val groupRef = db.collection("groups").document(group.id)
                        groupRef.update(
                            mapOf(
                                "name" to group.name,
                                "schedule" to group.schedule,
                                "students" to group.students,
                                "color" to group.color
                            )
                        ).await()

                        // Update the group in _groups list
                        val index = _groups.indexOfFirst { it.id == group.id }
                        if (index != -1) {
                            _groups[index] = group
                        }
                    } else {
                        // Handle empty snapshot (e.g., show a message to the user)
                        Timber.tag("AttendanceViewModel")
                            .w("No group found with name: ${group.name}")
                    }
                } else {
                    Timber.tag("AttendanceViewModel")
                        .w("Current teacher ID does not match the group's teacher ID")
                }
            } catch (e: Exception) {
                Timber.tag("AttendanceViewModel").w(e, "Error updating group")
                // Handle error, e.g., show a Snackbar
            }
            refreshGroups(currentTeacherId)
        }
    }
    fun deleteGroup(group: AttendanceGroup, currentTeacherId: String) {
        viewModelScope.launch {
            val db = Firebase.firestore

            try {
                _isLoading.value = true

                // Security check: only the owner can delete the group
                if (group.teacherId != currentTeacherId) {
                    Timber.tag("AttendanceViewModel")
                        .w("Current teacher ID does not match the group's teacher ID")
                    return@launch
                }

                Timber.tag("AttendanceViewModel")
                    .d("Deleting group: ${group.name} (${group.id})")

                // 1. Find every attendance record belonging to this group
                val attendanceSnapshot = db.collection("attendance")
                    .whereEqualTo("groupId", group.id)
                    .get()
                    .await()

                /*
                 * Firestore batches have a write limit.
                 * Using chunks keeps this safe even for groups with
                 * a large attendance history.
                 */
                attendanceSnapshot.documents
                    .chunked(450)
                    .forEach { documents ->

                        val batch = db.batch()

                        documents.forEach { document ->
                            batch.delete(document.reference)
                        }

                        batch.commit().await()
                    }

                // 2. Delete the group itself
                db.collection("groups")
                    .document(group.id)
                    .delete()
                    .await()

                // 3. Remove it immediately from local state
                _groups.removeAll { it.id == group.id }

                Timber.tag("AttendanceViewModel")
                    .d(
                        "Deleted group ${group.name} and " +
                                "${attendanceSnapshot.size()} attendance records"
                    )

            } catch (e: Exception) {
                Timber.tag("AttendanceViewModel")
                    .e(e, "Error deleting group ${group.name}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun extractStudentPhotos(
        uri: Uri,
        context: Context,
        students: List<ImportedStudent>
    ): Map<String, String> {

        if (students.isEmpty()) {
            return emptyMap()
        }

        val result =
            mutableMapOf<String, String>()

        context.contentResolver
            .openFileDescriptor(uri, "r")
            ?.use { descriptor ->

                PdfRenderer(descriptor).use { renderer ->

                    /*
                     * Current PDF format:
                     *
                     * 12 students per full page.
                     *
                     * Left column:
                     * 1-6
                     *
                     * Right column:
                     * 7-12
                     *
                     * Next page:
                     * 13-24, etc.
                     */
                    val studentsPerPage = 12
                    val rowsPerColumn = 6

                    /*
                     * Coordinates measured from the new
                     * attendance-list PDF.
                     *
                     * They are expressed as percentages,
                     * rather than absolute pixels, so they
                     * continue working at different rendering
                     * resolutions.
                     */
                    val leftPhotoX = 15.17f / 612f
                    val rightPhotoX = 307f / 612f

                    val firstPhotoY = 128.46f / 792f

                    val photoWidth = 55f / 612f
                    val photoHeight = 70f / 792f

                    val rowStep = 101.21f / 792f

                    /*
                     * Render at 2x so the thumbnail has enough
                     * detail for taking attendance.
                     */
                    val renderScale = 2f

                    val studentsByPage =
                        students.groupBy { student ->

                            (student.number - 1) /
                                    studentsPerPage
                        }

                    studentsByPage.forEach {
                            (pageIndex, pageStudents) ->

                        if (
                            pageIndex !in
                            0 until renderer.pageCount
                        ) {
                            return@forEach
                        }

                        renderer
                            .openPage(pageIndex)
                            .use { page ->

                                val pageBitmap =
                                    Bitmap.createBitmap(
                                        (
                                                page.width *
                                                        renderScale
                                                ).roundToInt(),
                                        (
                                                page.height *
                                                        renderScale
                                                ).roundToInt(),
                                        Bitmap.Config.ARGB_8888
                                    )

                                val matrix =
                                    Matrix().apply {
                                        setScale(
                                            renderScale,
                                            renderScale
                                        )
                                    }

                                page.render(
                                    pageBitmap,
                                    null,
                                    matrix,
                                    PdfRenderer.Page
                                        .RENDER_MODE_FOR_DISPLAY
                                )


                                pageStudents.forEach {
                                        student ->

                                    val slot =
                                        (student.number - 1) %
                                                studentsPerPage

                                    val column =
                                        if (
                                            slot <
                                            rowsPerColumn
                                        ) {
                                            0
                                        } else {
                                            1
                                        }

                                    val row =
                                        if (column == 0) {
                                            slot
                                        } else {
                                            slot -
                                                    rowsPerColumn
                                        }

                                    val xRatio =
                                        if (column == 0) {
                                            leftPhotoX
                                        } else {
                                            rightPhotoX
                                        }

                                    val yRatio =
                                        firstPhotoY +
                                                (
                                                        row *
                                                                rowStep
                                                        )

                                    val left =
                                        (
                                                pageBitmap.width *
                                                        xRatio
                                                )
                                            .roundToInt()

                                    val top =
                                        (
                                                pageBitmap.height *
                                                        yRatio
                                                )
                                            .roundToInt()

                                    val width =
                                        (
                                                pageBitmap.width *
                                                        photoWidth
                                                )
                                            .roundToInt()

                                    val height =
                                        (
                                                pageBitmap.height *
                                                        photoHeight
                                                )
                                            .roundToInt()


                                    /*
                                     * Protect against rounding pushing
                                     * the crop outside the bitmap.
                                     */
                                    val safeWidth =
                                        width.coerceAtMost(
                                            pageBitmap.width -
                                                    left
                                        )

                                    val safeHeight =
                                        height.coerceAtMost(
                                            pageBitmap.height -
                                                    top
                                        )

                                    if (
                                        left >= 0 &&
                                        top >= 0 &&
                                        safeWidth > 0 &&
                                        safeHeight > 0
                                    ) {

                                        val crop =
                                            Bitmap.createBitmap(
                                                pageBitmap,
                                                left,
                                                top,
                                                safeWidth,
                                                safeHeight
                                            )

                                        val encoded =
                                            createPhotoThumbnail(
                                                crop
                                            )

                                        result[student.name] =
                                            encoded

                                        crop.recycle()
                                    }
                                }

                                pageBitmap.recycle()
                            }
                    }
                }
            }

        Log.d(
            IMPORT_TAG,
            "Extracted ${result.size} student photos"
        )

        return result
    }
    private fun createPhotoThumbnail(
        bitmap: Bitmap
    ): String {

        // Resize the portrait to a small, predictable size.
        val thumbnail = Bitmap.createScaledBitmap(
            bitmap,
            132,
            168,
            true
        )

        val outputStream = ByteArrayOutputStream()

        thumbnail.compress(
            Bitmap.CompressFormat.JPEG,
            78,
            outputStream
        )

        val bytes = outputStream.toByteArray()

        outputStream.close()

        // Only recycle it if Android created a new bitmap.
        if (thumbnail !== bitmap) {
            thumbnail.recycle()
        }

        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP
        )
    }
    private fun isValidAcademicText(text: String): Boolean {
        val normalized = normalizeForMatch(text)

        return normalized.contains("LISTA DE ASISTENCIA", ignoreCase = true) &&
                normalized.contains("GRUPO:", ignoreCase = true) &&
                normalized.contains("NIVEL:", ignoreCase = true) &&
                normalized.contains("HORARIO:", ignoreCase = true) &&
                Regex("""\b\d{7}\b""").containsMatchIn(normalized)
    }

    fun importPdfAndCreateGroup(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(IMPORT_TAG, "importPdfAndCreateGroup() START uri=$uri")

            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                Log.d(IMPORT_TAG, "UI -> isLoading=true")

                Log.d(IMPORT_TAG, "Step 1: Extracting text...")
                val text = extractTextFromPdf(uri, context)
                Log.d(IMPORT_TAG, "Step 1 DONE: textLength=${text.length}, blank=${text.isBlank()}")

                if (text.isBlank()) {
                    Log.w(IMPORT_TAG, "Text is blank -> aborting import")
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo leer el PDF (texto vacío).")
                    }
                    return@launch
                }

                Log.d(IMPORT_TAG, "Step 2: Validating academic text...")
                val valid = isValidAcademicText(text)
                Log.d(IMPORT_TAG, "Step 2 DONE: valid=$valid")

                if (!valid) {
                    Log.w(IMPORT_TAG, "Validation FAILED -> aborting import")
                    _uiState.update {
                        it.copy(errorMessage = "El documento no sigue el formato académico requerido")
                    }
                    return@launch
                }

                Log.d(IMPORT_TAG, "Step 3: Parsing group + students...")
                val group =
                    parseRawPdfTextToGroup(
                        context,
                        text
                    )

                Log.d(
                    IMPORT_TAG,
                    "Step 3 DONE: " +
                            "groupNull=${group == null}, " +
                            "groupName=${group?.name}, " +
                            "students=${group?.students?.size}"
                )

                group?.let { parsedGroup ->

                    /*
                     * Reuse the same parsed PDF text to retain the
                     * student numbers. Those numbers tell us where
                     * each portrait appears on the PDF pages.
                     */
                    val lines =
                        text.lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

                    val numberedStudents =
                        extractStudentEntries(lines)

                    Log.d(
                        IMPORT_TAG,
                        "Extracting photos for " +
                                "${numberedStudents.size} students"
                    )

                    val photos =
                        extractStudentPhotos(
                            uri = uri,
                            context = context,
                            students = numberedStudents
                        )

                    val groupWithPhotos =
                        parsedGroup.copy(
                            studentPhotos = photos
                        )

                    Log.d(
                        IMPORT_TAG,
                        "Photos extracted: ${photos.size}"
                    )

                    Log.d(
                        IMPORT_TAG,
                        "Step 4: Saving group to Firestore..."
                    )

                    saveGroup(
                        groupWithPhotos,
                        getCurrentTeacherId()
                    )

                    _uiState.update { state ->
                        state.copy(
                            showImportSuccess = true
                        )
                    }

                    Log.d(
                        IMPORT_TAG,
                        "UI -> showImportSuccess=true"
                    )
                }?: run {
                    Log.w(IMPORT_TAG, "Parsing returned null -> Invalid PDF format")
                    _uiState.update {
                        it.copy(errorMessage = "Invalid PDF format (parser returned null)")
                    }
                }
            } catch (e: Exception) {
                Log.e(IMPORT_TAG, "IMPORT FAILED with exception", e)
                _uiState.update {
                    it.copy(errorMessage = "Error: ${e.localizedMessage}")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                Log.d(IMPORT_TAG, "UI -> isLoading=false, import END")
            }
        }
    }

    private class OcrHelper(context: Context) {
        private val tessBaseAPI: TessBaseAPI = TessBaseAPI().apply {
            // 1. Create proper directory structure
            val trainedDataPath = File(context.getExternalFilesDir(null), "tessdata").apply {
                mkdirs()
            }.absolutePath

            // 2. Copy trained data from assets if needed
            val trainedDataFile = File("$trainedDataPath/spa.traineddata")
            if (!trainedDataFile.exists()) {
                context.assets.open("tessdata/spa.traineddata").use { input ->
                    FileOutputStream(trainedDataFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // 3. Verify initialization
            if (!init(trainedDataPath, "spa")) {
                throw IllegalStateException("Could not initialize Tesseract with Spanish data")
            }
        }

        fun extractTextFromBitmap(bitmap: Bitmap): String {
            return try {
                // 4. Add image preprocessing
                val processedBitmap = preprocessImage(bitmap)
                tessBaseAPI.setImage(processedBitmap)
                tessBaseAPI.utF8Text?.trim() ?: ""
            } finally {
                // 5. Clean up resources
                tessBaseAPI.clear()
                bitmap.recycle()
            }
        }

        private fun preprocessImage(bitmap: Bitmap): Bitmap {
            // Enhanced preprocessing for Mexican academic docs
            val matrix = android.graphics.ColorMatrix().apply {
                setSaturation(0f) // Grayscale
                set(
                    floatArrayOf(
                        1.5f, 0f, 0f, 0f, -50f, // Contrast
                        0f, 1.5f, 0f, 0f, -50f,
                        0f, 0f, 1.5f, 0f, -50f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }

            return Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).apply {
                val canvas = android.graphics.Canvas(this)
                val paint = android.graphics.Paint().apply {
                    colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
                }
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
        }



        fun shutdown() {
            tessBaseAPI.end()
        }
    }

    private fun extractTextFromImagePdf(uri: Uri, context: Context): String {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val ocrHelper = OcrHelper(context)
                val textBuilder = StringBuilder()

                try {
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val bitmap = Bitmap.createBitmap(
                                page.width,
                                page.height,
                                Bitmap.Config.ARGB_8888
                            ).apply {
                                page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                            textBuilder.appendLine(ocrHelper.extractTextFromBitmap(bitmap))
                            bitmap.recycle()
                        }
                    }
                    return textBuilder.toString()
                } finally {
                    ocrHelper.shutdown()
                }
            }
        }
        return ""
    }


    private fun extractTextFromPdf(uri: Uri, context: Context): String {
        Log.d(IMPORT_TAG, "extractTextFromPdf() START")
        return try {
            val text = parsePdfToText(uri, context)
            Log.d(IMPORT_TAG, "parsePdfToText() returned length=${text.length}")

            if (text.isNotBlank()) {
                Log.d(IMPORT_TAG, "extractTextFromPdf() using PDFBox text")
                text
            } else {
                // Only OCR if it's truly blank (likely scanned)
                Log.d("PDF_DEBUG", "PDFTextStripper returned blank text; attempting OCR fallback")
                extractTextFromImagePdf(uri, context)
            }
        } catch (e: Exception) {
            Log.e("PDF_ERROR", "PDFBox failed. Not OCRing automatically to avoid PdfRenderer failures.", e)
            // IMPORTANT: return blank so validation fails gracefully and you can show a helpful message
            ""
        } finally {
            Log.d(IMPORT_TAG, "extractTextFromPdf() END")
        }
    }

    private fun parsePdfToText(uri: Uri, context: Context): String {
        PDFBoxResourceLoader.init(context)
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper().apply {
                    setSortByPosition(true)
                    lineSeparator = "\n"
                    paragraphStart = "\n"
                }
                stripper.getText(document)
            }
        } ?: ""
    }

    private fun parseRawPdfTextToGroup(context: Context, rawText: String): AttendanceGroup? {
        Log.d("PDF_DEBUG", "=== RAW PDF TEXT ===\n$rawText")

        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        saveDebugText(context, rawText, "raw_pdf.txt")
        logExtractionProcess(lines)

        val group = extractGroupMetadata(lines)
        val students = extractStudents(lines)

        Log.d("PDF_DEBUG", "=== EXTRACTED STUDENTS ===\n${students.joinToString("\n")}")
        Log.d(IMPORT_TAG, "Parsed groupName='${group.name}', schedule='${group.schedule}', studentsFound=${students.size}")

        return if (group.name.isNotBlank() && students.isNotEmpty()) {
            group.copy(students = students)
        } else {
            null
        }
    }

    private fun extractGroupMetadata(lines: List<String>): AttendanceGroup {

        val nivelPattern = Regex(
            """NIVEL\s*:\s*(INGLES)\s*([1-5])""",
            RegexOption.IGNORE_CASE
        )

        val grupoPattern = Regex(
            """GRUPO\s*:\s*([A-Z0-9]+)""",
            RegexOption.IGNORE_CASE
        )

        // New format:
        // Horario: (LMaMiJV) 07:00-08:00
        val schedulePattern = Regex(
            """HORARIO\s*:\s*\(([^)]+)\)\s*(\d{2}:\d{2}\s*-\s*\d{2}:\d{2})""",
            RegexOption.IGNORE_CASE
        )

        var nivel = ""
        var grupo = ""
        var scheduleDays = ""
        var scheduleTime = ""

        lines.forEach { originalLine ->

            val line = normalizeForMatch(originalLine)

            nivelPattern.find(line)?.let {
                nivel = "${it.groupValues[1].uppercase()} ${it.groupValues[2]}"
            }

            grupoPattern.find(line)?.let {
                grupo = it.groupValues[1].uppercase()
            }

            schedulePattern.find(line)?.let {
                scheduleDays = it.groupValues[1].trim()
                scheduleTime = it.groupValues[2]
                    .replace(Regex("""\s*-\s*"""), "-")
                    .trim()
            }
        }

        val groupName =
            if (nivel.isNotBlank() && grupo.isNotBlank()) {
                "$nivel - $grupo"
            } else {
                ""
            }

        val schedule =
            if (scheduleDays.isNotBlank() && scheduleTime.isNotBlank()) {
                "($scheduleDays) $scheduleTime"
            } else {
                ""
            }

        Log.d(
            IMPORT_TAG,
            "META nivel='$nivel' grupo='$grupo' " +
                    "days='$scheduleDays' time='$scheduleTime'"
        )

        return AttendanceGroup(
            name = groupName,
            schedule = schedule,
            color = mapLevelToColor(nivel),
            teacherId = getCurrentTeacherId()
        )
    }



    private companion object {
        const val IMPORT_TAG = "IMPORT_FLOW"
        val STUDENT_LINE_PATTERN = Regex(
            """(\d+)\s+(\d{7})\s+([A-ZÁÉÍÓÚÜÑ][A-ZÁÉÍÓÚÜÑ\s]+?)\s+(\d+)\s+(\d{7})\s+([A-ZÁÉÍÓÚÜÑ][A-ZÁÉÍÓÚÜÑ\s]+)""",
            RegexOption.IGNORE_CASE
        )

        val SINGLE_STUDENT_PATTERN = Regex(
            """^(\d+)\s+(\d{7})\s+([A-ZÁÉÍÓÚÜÑ][A-ZÁÉÍÓÚÜÑ\s]+)""",
            RegexOption.IGNORE_CASE
        )
    }

    private fun extractStudentEntries(
        lines: List<String>
    ): List<ImportedStudent> {

        val students = mutableListOf<ImportedStudent>()

        /*
         * First strategy:
         * Lines containing two students.
         *
         * Example:
         * 1 0395319 NAME ... 7 0360888 NAME ...
         */
        lines.forEach { line ->

            STUDENT_LINE_PATTERN
                .findAll(line)
                .forEach { match ->

                    val firstNumber =
                        match.groupValues[1].toIntOrNull()

                    val firstName =
                        cleanStudentName(
                            match.groupValues[3]
                        )

                    val secondNumber =
                        match.groupValues[4].toIntOrNull()

                    val secondName =
                        cleanStudentName(
                            match.groupValues[6]
                        )

                    if (
                        firstNumber != null &&
                        firstName.length >= 5
                    ) {
                        students.add(
                            ImportedStudent(
                                number = firstNumber,
                                name = firstName
                            )
                        )
                    }

                    if (
                        secondNumber != null &&
                        secondName.length >= 5
                    ) {
                        students.add(
                            ImportedStudent(
                                number = secondNumber,
                                name = secondName
                            )
                        )
                    }
                }


            /*
             * Second strategy:
             * Normal single student line.
             */
            SINGLE_STUDENT_PATTERN
                .find(line)
                ?.let { match ->

                    val number =
                        match.groupValues[1].toIntOrNull()

                    val name =
                        cleanStudentName(
                            match.groupValues[3]
                        )

                    if (
                        number != null &&
                        name.length >= 5
                    ) {
                        students.add(
                            ImportedStudent(
                                number = number,
                                name = name
                            )
                        )
                    }
                }
        }


        /*
         * Fallback for PDFs where PDFBox separates:
         *
         * 1 0395319
         * L
         * Asistencia
         * M M J V
         * STUDENT NAME
         */

        var currentState = 0
        var pendingNumber: Int? = null

        val idPattern = Regex(
            """^(\d+)\s+\d{7}$"""
        )

        lines.forEach { line ->

            when (currentState) {

                0 -> {
                    val match = idPattern.find(line)

                    if (match != null) {
                        pendingNumber =
                            match.groupValues[1].toIntOrNull()

                        currentState = 1
                    }
                }

                1 -> {
                    // Skip L
                    currentState = 2
                }

                2 -> {
                    // Skip Asistencia
                    currentState = 3
                }

                3 -> {
                    // Skip M M J V
                    currentState = 4
                }

                4 -> {

                    if (
                        pendingNumber != null &&
                        isValidStudentName(line)
                    ) {
                        students.add(
                            ImportedStudent(
                                number = pendingNumber!!,
                                name = cleanStudentName(line)
                            )
                        )
                    }

                    pendingNumber = null
                    currentState = 0
                }
            }

            if (
                line.contains("FACULTAD DE") ||
                line.contains("Página")
            ) {
                pendingNumber = null
                currentState = 0
            }
        }

        return students
            .distinctBy { it.number }
            .sortedBy { it.number }
    }

    private fun extractStudents(
        lines: List<String>
    ): List<String> {

        return extractStudentEntries(lines)
            .map { it.name }
            .distinct()
            .sorted()
    }
    private fun isValidStudentName(line: String): Boolean {
        return line.matches(Regex("""^(?!.*(FACULTAD|COORDINADOR|Página)) 
        [A-ZÁÉÍÓÚÜÑ][A-ZÁÉÍÓÚÜÑ\s]{4,}${'$'}""", RegexOption.IGNORE_CASE)) &&
                line.count { it.isLetter() } >= 3 &&
                line.contains(Regex("[A-ZÁÉÍÓÚÜÑ]"))
    }

    private fun cleanStudentName(raw: String): String {
        return raw
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .replace(
                Regex("""[^a-zA-ZÁÉÍÓÚÜÑáéíóúüñ\s]"""),
                ""
            )
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase()
                    .replaceFirstChar {
                        when {
                            it.isLowerCase() -> it.titlecase()
                            it == 'ñ' -> "Ñ"
                            else -> it.toString()
                        }
                    }
            }
            .trim()
    }

    private fun mapLevelToColor(levelLine: String): Int {
        return when {
            "INGLES 5" in levelLine.uppercase() -> Color(0xFF5E006E).toArgb()
            "INGLES 4" in levelLine.uppercase() -> Color(0xFFF57C00).toArgb()
            "INGLES 3" in levelLine.uppercase() -> Color(0xFF008000).toArgb()
            "INGLES 2" in levelLine.uppercase() -> Color(0xFF0045F5).toArgb()
            "INGLES 1" in levelLine.uppercase() -> Color(0xFFFD0331).toArgb()
            else -> Color(0xFF6A5ACD).toArgb()
        }
    }



    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    suspend fun saveAttendanceConfirmed(
        attendanceRecords: List<AttendanceRecord>
    ): Result<List<AttendanceRecord>> {

        _isLoading.value = true

        return try {

            val verifiedRecords =
                mutableListOf<AttendanceRecord>()


            attendanceRecords.forEach {
                    record ->

                val verified =
                    writeAttendanceWithRetry(
                        record
                    )

                verifiedRecords.add(
                    verified
                )
            }


            /*
             * Every single student has now been written
             * AND verified from Firestore's server.
             */
            val mergedRecords =
                _attendanceRecords.value
                    .toMutableList()


            verifiedRecords.forEach { verified ->

                val index =
                    mergedRecords.indexOfFirst {
                        it.student == verified.student &&
                                it.groupId == verified.groupId &&
                                it.partial == verified.partial &&
                                it.date == verified.date
                    }


                if (index >= 0) {

                    mergedRecords[index] =
                        verified

                } else {

                    mergedRecords.add(
                        verified
                    )
                }
            }


            _attendanceRecords.value =
                mergedRecords


            Timber
                .tag("AttendanceDatabase")
                .d(
                    "Attendance batch SERVER CONFIRMED. " +
                            "${verifiedRecords.size} records."
                )


            Result.success(
                verifiedRecords
            )


        } catch (e: Exception) {

            Timber
                .tag("AttendanceDatabase")
                .e(
                    e,
                    "Attendance batch could not be verified"
                )


            Result.failure(e)


        } finally {

            _isLoading.value = false
        }
    }



    fun getAttendanceRecordsForGroup(
        groupId: String,
        partial: Int
    ): Flow<List<AttendanceRecord>> = flow {

        try {

            _isLoading.value = true
            _attendanceError.value = null

            Timber.tag("AttendanceDatabase")
                .d(
                    "Loading attendance from SERVER: " +
                            "group=$groupId partial=$partial"
                )

            val snapshot =
                firestore
                    .collection("attendance")
                    .whereEqualTo(
                        "groupId",
                        groupId
                    )
                    .get(Source.SERVER)
                    .await()

            Timber.tag("AttendanceDatabase")
                .d(
                    "Firestore returned " +
                            "${snapshot.size()} documents"
                )

            /*
             * Keep the Firestore DocumentSnapshot together
             * with its parsed AttendanceRecord.
             *
             * This lets us identify canonical records and
             * ignore legacy duplicates.
             */
            val parsedRecords =
                snapshot.documents
                    .mapNotNull { document ->

                        try {

                            val record =
                                document.toAttendanceRecord()

                            Pair(
                                document,
                                record
                            )

                        } catch (e: Exception) {

                            Timber.tag(
                                "AttendanceDatabase"
                            ).e(
                                e,
                                "Could not parse attendance " +
                                        "document ${document.id}. " +
                                        "Raw data=${document.data}"
                            )

                            null
                        }
                    }
                    .filter { (_, record) ->
                        record.partial == partial
                    }


            /*
             * Group together duplicate logical records:
             *
             * same group
             * same student
             * same partial
             * same date
             */
            val grouped =
                parsedRecords.groupBy {
                        (_, record) ->

                    attendanceLogicalKey(
                        record
                    )
                }


            val records =
                grouped
                    .mapNotNull {
                            (_, versions) ->

                        /*
                         * Prefer the new deterministic
                         * Firestore document.
                         */
                        val canonical =
                            versions.firstOrNull {
                                    (document, record) ->

                                document.id ==
                                        attendanceDocumentId(
                                            record
                                        )
                            }

                        if (canonical != null) {

                            canonical.second

                        } else {

                            /*
                             * If there are only legacy
                             * documents, use the newest one.
                             */
                            versions
                                .maxByOrNull {
                                        (document, _) ->

                                    document
                                        .getTimestamp(
                                            "timestamp"
                                        )
                                        ?.seconds
                                        ?: 0L
                                }
                                ?.second
                        }
                    }
                    .sortedWith(
                        compareBy<AttendanceRecord> {
                            it.student
                        }.thenBy {
                            it.date
                        }
                    )


            val duplicateCount =
                parsedRecords.size -
                        records.size

            if (duplicateCount > 0) {

                Timber.tag("AttendanceDatabase")
                    .w(
                        "Found $duplicateCount legacy " +
                                "duplicate attendance records."
                    )
            }


            /*
             * Synchronize application state with the
             * server result.
             */
            _attendanceRecords.value =
                records


            Timber.tag("AttendanceDatabase")
                .d(
                    "Final attendance records: " +
                            "${records.size}"
                )


            /*
             * This can legitimately be aborted by
             * Flow.first().
             */
            emit(records)


        } catch (e: CancellationException) {

            /*
             * CRITICAL:
             *
             * Flow.first() intentionally cancels the Flow
             * after receiving one result.
             *
             * Never turn that cancellation into another
             * emission.
             */
            throw e


        } catch (e: Exception) {

            /*
             * These are REAL Firestore/data errors.
             */
            Timber.tag("AttendanceDatabase")
                .e(
                    e,
                    "SERVER attendance read failed"
                )

            _attendanceError.value =
                e.localizedMessage
                    ?: "Unknown Firestore error"

            emit(
                emptyList()
            )


        } finally {

            _isLoading.value = false
        }
    }

    suspend fun updateAttendanceRecordConfirmed(
        updatedRecord: AttendanceRecord
    ): Result<AttendanceRecord> {

        return try {

            val verifiedRecord =
                writeAttendanceWithRetry(
                    updatedRecord
                )

            /*
             * Only update application state after
             * server verification succeeds.
             */
            _attendanceRecords.value =
                _attendanceRecords.value.map { record ->

                    if (
                        record.student ==
                        verifiedRecord.student &&
                        record.groupId ==
                        verifiedRecord.groupId &&
                        record.partial ==
                        verifiedRecord.partial &&
                        record.date ==
                        verifiedRecord.date
                    ) {
                        verifiedRecord
                    } else {
                        record
                    }
                }

            Result.success(
                verifiedRecord
            )

        } catch (e: Exception) {

            Timber
                .tag("AttendanceDatabase")
                .e(
                    e,
                    "Could not verify attendance update"
                )

            Result.failure(e)
        }
    }

    fun addOrUpdateAttendanceRecord(
        record: AttendanceRecord
    ) {
        Timber.tag("AttendanceViewModel")
            .d("Adding/updating local record: $record")

        val current =
            _attendanceRecords.value.toMutableList()

        val index =
            current.indexOfFirst {
                it.student == record.student &&
                        it.groupId == record.groupId &&
                        it.partial == record.partial &&
                        it.date == record.date
            }

        if (index >= 0) {

            current[index] =
                record.copy(
                    timestamp = Timestamp.now()
                )

        } else {

            /*
             * This was missing before.
             *
             * If the StateFlow does not already contain
             * the record, add it.
             */
            current.add(
                record.copy(
                    timestamp = Timestamp.now()
                )
            )
        }

        _attendanceRecords.value = current
    }

    fun refreshGroups(teacherId: String) {
        viewModelScope.launch {
            // Same code as fetchGroups but without _groups.clear()
            val db = Firebase.firestore
            db.collection("groups")
                .whereEqualTo("teacherId", teacherId).get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot) {
                        val group = document.toObject(AttendanceGroup::class.java)
                        group.id = document.id
                        // Update existing group or add new one
                        val index = _groups.indexOfFirst { it.id == group.id }
                        if (index != -1) {
                            _groups[index] = group
                        } else {
                            _groups.add(group)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Timber.tag("AttendanceViewModel").w(e, "Error getting groups")
                }
        }
    }

    // Helper function to convert a Firestore document to AttendanceRecord
    private fun DocumentSnapshot.toAttendanceRecord(): AttendanceRecord {

        val student =
            getString("student")
                ?: throw IllegalStateException(
                    "Attendance document $id has no student"
                )

        val groupId =
            getString("groupId")
                ?: throw IllegalStateException(
                    "Attendance document $id has no groupId"
                )

        val partial =
            (get("partial") as? Number)
                ?.toInt()
                ?: 0

        val status =
            when (getString("status")) {

                "PRESENT" ->
                    AttendanceStatus.PRESENT

                "LATE" ->
                    AttendanceStatus.LATE

                "ABSENT" ->
                    AttendanceStatus.ABSENT

                else ->
                    null
            }

        val date =
            readAttendanceDate()
                ?: throw IllegalStateException(
                    "Attendance document $id has invalid date data"
                )

        val timestamp =
            getTimestamp("timestamp")
                ?: Timestamp.now()

        return AttendanceRecord(
            student = student,
            groupId = groupId,
            partial = partial,
            status = status,
            date = date,
            timestamp = timestamp
        )
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
        stopGroupsListener()
        saveAttendanceJob.cancel()// Cancel the SupervisorJob when ViewModel is cleared
    }
}

