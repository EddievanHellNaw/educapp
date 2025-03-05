package com.example.educapp.commons.teacher.attendance

import java.io.FileOutputStream
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

data class AttendanceGroup(
    var id: String = "",
    var name: String = "",
    var schedule: String = "",
    var students: List<String> = emptyList(),
    val teacherId: String = "",
    val color: Int = Color(0xFF6A5ACD).toArgb()
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
    val snackbarMessage: StateFlow<Event<String>?> = _snackbarMessage.asStateFlow()
    private var groupsListenerRegistration: ListenerRegistration? = null
    private val saveAttendanceJob = Job()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init{
        viewModelScope.launch {
            val currentTeacherId = getCurrentTeacherId()
            startGroupsListener(currentTeacherId)
        }
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

    fun saveGroup(group: AttendanceGroup, teacherId: String) {
        viewModelScope.launch {
            val db = Firebase.firestore
            val data = mapOf(
                "name" to group.name,
                "schedule" to group.schedule,
                "students" to group.students,
                "color" to group.color,
                "teacherId" to teacherId
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
                if(group.teacherId == currentTeacherId) {
                    val groupRef =
                        db.collection("groups").document(group.id).delete().await() // Use document ID
                    _groups.remove(group)
                }else {
                    Timber.tag("AttendanceViewModel")
                        .w("Current teacher ID does not match the group's teacher ID")
                }
            } catch (e: Exception) {
                Timber.tag("AttendanceViewModel").w(e, "Error deleting group")
                // Handle error, e.g., show a Snackbar
            }
        }
    }


    private fun isValidAcademicPdf(context: Context, uri: Uri): Boolean {
        return try {
            val text = extractTextFromPdf(uri, context)
            text.contains(Regex("""(Departamento Universitario de Inglés|FACULTAD DE)""")) &&
                    text.contains(Regex("""\b\d{7}\b""")) && // 7-digit IDs
                    text.contains(Regex("""(Calificación|Asistencia|Portafolio)"""))
        } catch (e: Exception) {
            false
        }
    }

    fun importPdfAndCreateGroup(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val text = extractTextFromPdf(uri, context)
                // Validate PDF structure first
                if (!isValidAcademicPdf(context, uri)) {
                    _uiState.update {
                        it.copy(errorMessage = "El documento no sigue el formato académico requerido")
                    }
                    return@launch
                }

                val group = parseRawPdfTextToGroup(context, text)
                group?.let {
                    saveGroup(it, getCurrentTeacherId())
                    _uiState.update {
                        it.copy(showImportSuccess = true)
                    }
                } ?: run {
                    _uiState.update {
                        it.copy(errorMessage = "Invalid PDF format")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Error: ${e.localizedMessage}")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
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
        return try {
            val text = parsePdfToText(uri, context)
            if (text.isNotBlank()) {
                text
            } else {
                Log.d("PDF_DEBUG", "Falling back to OCR")
                extractTextFromImagePdf(uri, context)
            }
        } catch (e: Exception) {
            Log.e("PDF_ERROR", "Text extraction failed", e)
            extractTextFromImagePdf(uri, context)
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

        return if (group.name.isNotBlank() && students.isNotEmpty()) {
            group.copy(students = students)
        } else {
            null
        }
    }

    private fun extractGroupMetadata(lines: List<String>): AttendanceGroup {
        val nivelPattern = Regex("""Nivel\s*:\s*(INGLES\s+[1-5])""", RegexOption.IGNORE_CASE)
        val grupoPattern = Regex("""Grupo\s*:\s*([A-Z0-9]+)""")
        val schedulePattern = Regex("""Horario\s*:\s*(\S+)\s+\((\d{2}:\d{2}\s*-\s*\d{2}:\d{2})\)""")

        var nivel = ""
        var grupo = ""
        var rawSchedule = ""
        var scheduleTime = ""

        lines.forEach { line ->
            nivelPattern.find(line)?.let {
                nivel = it.groupValues[1].uppercase()
            }
            grupoPattern.find(line)?.let {
                grupo = it.groupValues[1]
            }
            schedulePattern.find(line)?.let {
                rawSchedule = it.groupValues[1]
                scheduleTime = it.groupValues[2]
            }
        }

        return AttendanceGroup(
            name = if (nivel.isNotEmpty() && grupo.isNotEmpty()) "$nivel - $grupo" else "",
            schedule = if (rawSchedule.isNotEmpty()) "L-V ($scheduleTime)" else "",
            color = mapLevelToColor(nivel),
            teacherId = getCurrentTeacherId()
        )
    }

    private companion object {
        val STUDENT_LINE_PATTERN = Regex(
            """(\d+)\s+(\d{7})\s+([A-Z][A-Z\s]+?)\s+(\d+)\s+(\d{7})\s+([A-Z][A-Z\s]+)"""
        )

        val SINGLE_STUDENT_PATTERN = Regex(
            """^\d+\s+\d{7}\s+([A-Z][A-Z\s]+)"""
        )
    }

    private fun extractStudents(lines: List<String>): List<String> {
        val students = mutableListOf<String>()

        lines.forEach { line ->
            // Handle two students per line
            STUDENT_LINE_PATTERN.findAll(line).forEach { match ->
                students.add(cleanStudentName(match.groupValues[3]))  // First student
                students.add(cleanStudentName(match.groupValues[6]))  // Second student
            }

            // Handle single student lines
            SINGLE_STUDENT_PATTERN.find(line)?.let {
                students.add(cleanStudentName(it.groupValues[1]))
            }
        }

        var currentState = 0
        val idPattern = Regex("""^\d+\s+\d{7}$""") // Matches "1 0351636"

        lines.forEach { line ->
            when (currentState) {
                0 -> if (idPattern.matches(line)) currentState = 1
                1 -> currentState = 2 // Skip "L" line
                2 -> currentState = 3 // Skip "Asistencia" line
                3 -> {
                    currentState = 4 // Skip "M M J V" line
                }
                4 -> {
                    if (isValidStudentName(line)) {
                        students.add(cleanStudentName(line))
                    }
                    currentState = 0
                }
            }

            // Handle page breaks more precisely
            if (line.contains("FACULTAD DE") || line.contains("Página")) {
                currentState = 0
            }
        }

        return students.distinct()
            .filter { it.length >= 5 }
            .sorted()
    }


    private fun isValidStudentName(line: String): Boolean {
        return line.matches(Regex("""^(?!.*(FACULTAD|COORDINADOR|Página)) 
        [A-ZÁÉÍÓÚÜÑ][A-ZÁÉÍÓÚÜÑ\s]{4,}${'$'}""", RegexOption.IGNORE_CASE)) &&
                line.count { it.isLetter() } >= 3
    }

    private fun cleanStudentName(raw: String): String {
        return raw.replace(Regex("""\s{2,}"""), " ")
            .trim()
            .split(" ")
            .joinToString(" ") { part ->
                part.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
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

    fun saveAttendance(groupId: String, attendanceRecords: List<AttendanceRecord>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val updatedRecords = attendanceRecords.map { record ->
                    val dateTimestamp = com.google.firebase.Timestamp(
                        record.date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0
                    )
                    val query = firestore.collection("attendance")
                        .whereEqualTo("student", record.student)
                        .whereEqualTo("groupId", record.groupId)
                        .whereEqualTo("partial", record.partial)
                        .whereEqualTo("date", dateTimestamp)

                    val querySnapshot = query.get().await() // Get QuerySnapshot synchronously before the transaction
                    val existingRecord = querySnapshot.documents.firstOrNull()

                    // Perform the update or set operation within the transaction
                    firestore.runTransaction { transaction ->
                        if (existingRecord != null) {
                            transaction.update(
                                firestore.collection("attendance").document(existingRecord.id),
                                "status", record.status
                            )
                        } else {
                            transaction.set(firestore.collection("attendance").document(), record)
                        }
                    }.await() // Get the result of the transaction synchronously

                    // Fetch the updated record (or the newly created record)
                    val updatedRecordQuery = firestore.collection("attendance")
                        .whereEqualTo("student", record.student)
                        .whereEqualTo("groupId", record.groupId)
                        .whereEqualTo("partial", record.partial)
                        .whereEqualTo("date", dateTimestamp)
                    val updatedRecordSnapshot = updatedRecordQuery.get().await()
                    updatedRecordSnapshot.documents.firstOrNull()?.toObject(AttendanceRecord::class.java)
                }

                _attendanceRecords.value = updatedRecords.filterNotNull()
                _isLoading.value = false
                _snackbarMessage.value = Event("Attendance saved successfully")
            } catch (e: FirebaseFirestoreException) {
                Timber.tag("AttendanceViewModel").w(e, "Firestore error saving attendance")
                _isLoading.value = false
                _snackbarMessage.value = Event("Error saving attendance: ${e.message}")
            } catch (e: Exception) {
                Timber.tag("AttendanceViewModel").w(e, "Error saving attendance")
                _isLoading.value = false
                _snackbarMessage.value = Event("Error saving attendance")
            }
        }
    }



    fun getAttendanceRecordsForGroup(groupId: String, partial: Int): Flow<List<AttendanceRecord>> = flow {
        val db = Firebase.firestore
        try {
            _isLoading.value = true
            val attendanceRecords = db.collection("attendance")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("partial", partial)
                .get().await().documents.mapNotNull { document ->
                    try {
                        document.toAttendanceRecord()
                    } catch (e: Exception) {
                        Timber.tag("AttendanceViewModel").w(e, "Error converting document to AttendanceRecord")
                        null // Skip this document if conversion fails
                    }
                }
            emit(attendanceRecords)
            Log.d("AttendanceViewModel", "Emitting attendance records: $attendanceRecords")
        } catch (e: Exception) {
            Timber.tag("AttendanceViewModel").w(e, "Error getting attendance records")
            emit(emptyList())
        } finally {
            _isLoading.value = false
        }
    }

    fun updateAttendanceRecord(updatedRecord: AttendanceRecord) {
        Log.d("AttendanceViewModel", "Updating record: $updatedRecord")
        viewModelScope.launch {
            try {
                val db = Firebase.firestore
                val querySnapshot = db.collection("attendance")
                    .whereEqualTo("student", updatedRecord.student)
                    .whereEqualTo("groupId", updatedRecord.groupId)
                    .whereEqualTo("partial", updatedRecord.partial)
                    .whereEqualTo("date", updatedRecord.date)
                    .get()
                    .await()

                if (querySnapshot.documents.isNotEmpty()) {
                    val recordRef = db.collection("attendance").document(querySnapshot.documents.first().id)
                    recordRef.update("status", updatedRecord.status).await()

                    // Update _attendanceRecords StateFlow
                    _attendanceRecords.value = _attendanceRecords.value.map {
                        if (it.student == updatedRecord.student && it.date == updatedRecord.date) updatedRecord else it
                    }
                } else {
                    Timber.tag("AttendanceViewModel").w("No attendance record found for update")
                }
            } catch (e: Exception) {
                Timber.tag("AttendanceViewModel").w(e, "Error updating attendance record")
            }
        }
    }

    fun addOrUpdateAttendanceRecord(record: AttendanceRecord) {
        Timber.tag("AttendanceViewModel").d("Adding/Updating record: $record")
        _attendanceRecords.value = _attendanceRecords.value.map {
            if (it.student == record.student && it.groupId == record.groupId && it.partial == record.partial && it.date == record.date) {
                record.copy(timestamp = com.google.firebase.Timestamp.now()) // Update only the timestamp
            } else {
                it
            }
        }
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
        val statusString = getString("status")
        val status = when (statusString) {
            "PRESENT" -> AttendanceStatus.PRESENT
            "ABSENT" -> AttendanceStatus.ABSENT
            "LATE" -> AttendanceStatus.LATE
            else -> null // Handle the case where status is null or unknown
        }
        val dateMap = get("date") as? Map<*, *>
        val date = if (dateMap != null) {
            val year = (dateMap["year"] as? Long)?.toInt() ?: 2024
            val month = (dateMap["monthValue"] as? Long)?.toInt() ?: 1
            val day = (dateMap["dayOfMonth"] as? Long)?.toInt() ?: 1
            Log.d("toAttendanceRecord", "Extracted date: year=$year, month=$month, day=$day")
            LocalDate.of(year, month, day)
        } else {
            LocalDate.now()
        }

        return AttendanceRecord(
            student = getString("student") ?: "",
            groupId = getString("groupId") ?: "",
            partial = getLong("partial")?.toInt() ?: 0,
            status = status, // Use the status obtained from the when expression
            date = date,
            timestamp = getTimestamp("timestamp")!!
        )
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
        stopGroupsListener()
        saveAttendanceJob.cancel()// Cancel the SupervisorJob when ViewModel is cleared
    }
}

