package com.example.educapp.commons.teacher.attendance

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.R
import com.example.educapp.commons.ui.FrostedBox
import com.example.educapp.commons.ui.GradientCard
import com.example.educapp.commons.ui.hapticClickable
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first
import timber.log.Timber


private val PresentColor = Color(0xFF388E3C)
private val LateColor = Color(0xFFFBC02D)
private val AbsentColor = Color(0xFFD32F2F)

fun recordKey(
    record: AttendanceRecord
): String {

    return "${record.student}|" +
            "${record.partial}|" +
            "${record.date}"
}

@Composable
fun CheckScreen(
    viewModel: AttendanceViewModel,
    groupId: String,
    currentPartial: Int,
    navController: NavHostController
) {

    val coroutineScope =
        rememberCoroutineScope()

    var savingRecordKey by remember {
        mutableStateOf<String?>(null)
    }

    var savedRecordKey by remember {
        mutableStateOf<String?>(null)
    }

    var databaseMessage by remember {
        mutableStateOf<String?>(null)
    }

    var attendanceRecords by remember {
        mutableStateOf<List<AttendanceRecord>>(
            emptyList()
        )
    }

    val attendanceError by
    viewModel.attendanceError.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()

    /*
     * Find the group so we can reuse the student
     * photographs imported from the PDF.
     */
    val group = viewModel.groups
        .firstOrNull { it.id == groupId }


    LaunchedEffect(
        groupId,
        currentPartial
    ) {
        viewModel
            .getAttendanceRecordsForGroup(
                groupId,
                currentPartial
            )
            .collect { records ->

                attendanceRecords = records

                Log.d(
                    "AttendanceRecords",
                    "AttendanceRecords: $records"
                )
            }
    }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor =
        MaterialTheme.colorScheme.onBackground
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Text(
                text =
                "Attendance · Partial $currentPartial",

                style =
                MaterialTheme.typography
                    .headlineMedium,

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
            )


            when {

                isLoading -> {

                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(
                                Alignment.CenterHorizontally
                            )
                            .padding(24.dp)
                    )
                }

                attendanceError != null -> {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {

                        Text(
                            text =
                            "Could not load attendance records.",

                            style =
                            MaterialTheme.typography
                                .titleMedium,

                            color =
                            MaterialTheme.colorScheme.error
                        )

                        Spacer(
                            modifier =
                            Modifier.height(6.dp)
                        )

                        Text(
                            text =
                            attendanceError
                                ?: "Unknown database error",

                            style =
                            MaterialTheme.typography
                                .bodySmall
                        )
                    }
                }

                attendanceRecords.isEmpty() -> {

                    Text(
                        text =
                        "No attendance records found " +
                                "for this group and partial.",

                        modifier = Modifier
                            .align(
                                Alignment.CenterHorizontally
                            )
                            .padding(24.dp)
                    )
                }

                else -> {

                    AttendanceSummary(
                        attendanceRecords = attendanceRecords,
                        currentPartial = currentPartial,
                        studentPhotos =
                        group?.studentPhotos ?: emptyMap(),

                        savingRecordKey =
                        savingRecordKey,

                        savedRecordKey =
                        savedRecordKey,

                        onRecordUpdated =  { updatedRecord ->

                            coroutineScope.launch {

                                val key =
                                    recordKey(
                                        updatedRecord
                                    )

                                savingRecordKey = key

                                databaseMessage =
                                    "Saving attendance…"


                                val result =
                                    viewModel
                                        .updateAttendanceRecordConfirmed(
                                            updatedRecord
                                        )


                                result
                                    .onSuccess { confirmedRecord ->

                                        /*
                                         * updateAttendanceRecordConfirmed() has already:
                                         *
                                         * 1. written to Firestore
                                         * 2. waited for Firebase
                                         * 3. read the document again from Source.SERVER
                                         * 4. verified the returned value
                                         *
                                         * So there is no need to reload the entire group.
                                         */
                                        attendanceRecords =
                                            attendanceRecords.map { record ->

                                                if (
                                                    record.student ==
                                                    confirmedRecord.student &&
                                                    record.groupId ==
                                                    confirmedRecord.groupId &&
                                                    record.partial ==
                                                    confirmedRecord.partial &&
                                                    record.date ==
                                                    confirmedRecord.date
                                                ) {
                                                    confirmedRecord
                                                } else {
                                                    record
                                                }
                                            }

                                        savedRecordKey = key

                                        /*
                                         * Brief "Saved" indicator.
                                         */
                                        delay(1500)

                                        if (savedRecordKey == key) {
                                            savedRecordKey = null
                                        }
                                    }

                                    .onFailure { error ->

                                        /*
                                         * Nothing was changed locally,
                                         * therefore the old attendance value
                                         * remains visible.
                                         */
                                        databaseMessage =
                                            "Could not save change: " +
                                                    (
                                                            error.localizedMessage
                                                                ?: "Unknown database error"
                                                            )
                                    }


                                savingRecordKey = null
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun AttendanceSummary(
    attendanceRecords: List<AttendanceRecord>,
    currentPartial: Int,
    studentPhotos: Map<String, String>,
    savingRecordKey: String?,
    savedRecordKey: String?,
    onRecordUpdated: (AttendanceRecord) -> Unit
){

    val students =
        attendanceRecords
            .groupBy { it.student }
            .toSortedMap()


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
        PaddingValues(bottom = 24.dp)
    ) {

        items(
            items = students.entries.toList(),
            key = { it.key }
        ) { entry ->

            val student = entry.key

            val studentRecords =
                entry.value.filter {
                    it.partial == currentPartial
                }

            ReviewStudentCard(
                student = student,
                records = studentRecords,
                photoBase64 = studentPhotos[student],

                savingRecordKey =
                savingRecordKey,

                savedRecordKey =
                savedRecordKey,

                onRecordUpdated =
                onRecordUpdated
            )
        }
    }
}


@Composable
private fun ReviewStudentCard(
    student: String,
    records: List<AttendanceRecord>,
    photoBase64: String?,
    savingRecordKey: String?,
    savedRecordKey: String?,
    onRecordUpdated: (AttendanceRecord) -> Unit
) {

    var editing by remember(student) {
        mutableStateOf(false)
    }


    val presentCount =
        records.count {
            it.status ==
                    AttendanceStatus.PRESENT
        }

    val lateCount =
        records.count {
            it.status ==
                    AttendanceStatus.LATE
        }

    val absentCount =
        records.count {
            it.status ==
                    AttendanceStatus.ABSENT
        }


    /*
     * Keep your existing warning behavior:
     * increasingly red when absences rise.
     */
    val gradientColor =
        when {
            absentCount > 6 ->
                MaterialTheme.colorScheme.error

            absentCount >= 4 ->
                MaterialTheme.colorScheme.error
                    .copy(alpha = 0.75f)

            else ->
                MaterialTheme.colorScheme.primary
        }


    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 5.dp
            )
            .animateContentSize(
                animationSpec =
                tween(220)
            ),

        gradientBrush =
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                gradientColor
            )
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {

            /*
             * ----------------------------------------
             * COMPACT STUDENT HEADER
             * ----------------------------------------
             */
            Row(
                modifier =
                Modifier.fillMaxWidth(),

                verticalAlignment =
                Alignment.CenterVertically
            ) {

                StudentPortrait(
                    student = student,
                    photoBase64 = photoBase64,
                    compact = true
                )


                Spacer(
                    modifier = Modifier.width(10.dp)
                )


                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = student,

                        style =
                        MaterialTheme.typography
                            .bodyLarge,

                        color =
                        MaterialTheme.colorScheme
                            .onSurface
                    )


                    Spacer(
                        modifier =
                        Modifier.height(5.dp)
                    )


                    /*
                     * Attendance summary.
                     */
                    FrostedBox {

                        Row(
                            horizontalArrangement =
                            Arrangement.spacedBy(
                                14.dp
                            ),

                            verticalAlignment =
                            Alignment.CenterVertically,

                            modifier =
                            Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 5.dp
                            )
                        ) {

                            AttendanceIconCount(
                                icon =
                                R.drawable.present_icon,
                                count =
                                presentCount,
                                color =
                                PresentColor
                            )

                            AttendanceIconCount(
                                icon =
                                R.drawable.late_icon,
                                count =
                                lateCount,
                                color =
                                LateColor
                            )

                            AttendanceIconCount(
                                icon =
                                R.drawable.absent_icon,
                                count =
                                absentCount,
                                color =
                                AbsentColor
                            )
                        }
                    }
                }


                /*
                 * EDIT / CALENDAR BUTTON
                 */
                IconButton(
                    onClick = {
                        editing = !editing
                    },

                    modifier =
                    Modifier.size(44.dp)
                ) {

                    Icon(
                        imageVector =
                        Icons.Filled.Edit,

                        contentDescription =
                        if (editing) {
                            "Close attendance calendar"
                        } else {
                            "Edit attendance"
                        },

                        tint =
                        if (editing) {
                            MaterialTheme
                                .colorScheme
                                .primary
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurface
                        },

                        modifier =
                        Modifier.size(23.dp)
                    )
                }
            }


            /*
             * ----------------------------------------
             * EXPANDED CALENDAR
             * ----------------------------------------
             */
            if (editing) {

                Spacer(
                    modifier =
                    Modifier.height(12.dp)
                )

                AttendanceCalendar(
                    records = records,

                    savingRecordKey =
                    savingRecordKey,

                    savedRecordKey =
                    savedRecordKey,

                    onRecordUpdated =
                    onRecordUpdated
                )
            }
        }
    }
}


@Composable
private fun AttendanceCalendar(
    records: List<AttendanceRecord>,
    savingRecordKey: String?,
    savedRecordKey: String?,
    onRecordUpdated: (AttendanceRecord) -> Unit
){

    if (records.isEmpty()) {

        Text(
            text =
            "No attendance records available.",

            style =
            MaterialTheme.typography.bodyMedium,

            modifier =
            Modifier.padding(12.dp)
        )

        return
    }


    /*
     * Months that actually contain attendance
     * records.
     */
    val months =
        remember(records.map { it.date }) {

            records
                .map {
                    YearMonth.from(it.date)
                }
                .distinct()
                .sorted()
        }


    var currentMonth by
    remember(
        records
            .firstOrNull()
            ?.student
    ) {
        mutableStateOf(
            months.last()
        )
    }


    /*
     * Make sure currentMonth remains valid when
     * the record list changes.
     */
    LaunchedEffect(months) {

        if (
            months.isNotEmpty() &&
            currentMonth !in months
        ) {
            currentMonth =
                months.last()
        }
    }


    var selectedDate by
    remember(
        records
            .firstOrNull()
            ?.student
    ) {
        mutableStateOf<LocalDate?>(
            null
        )
    }


    val recordsByDate =
        remember(records) {

            records.associateBy {
                it.date
            }
        }

    val selectedRecord =
        selectedDate?.let {
            recordsByDate[it]
        }


    FrostedBox(
        modifier =
        Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {

            /*
             * MONTH NAVIGATION
             */
            CalendarMonthHeader(
                currentMonth =
                currentMonth,

                availableMonths =
                months,

                onMonthChanged = { newMonth ->

                    currentMonth =
                        newMonth

                    selectedDate =
                        null
                }
            )


            Spacer(
                modifier =
                Modifier.height(6.dp)
            )


            /*
             * WEEKDAY LABELS
             *
             * Monday-first calendar.
             */
            Row(
                modifier =
                Modifier.fillMaxWidth()
            ) {

                listOf(
                    "M",
                    "T",
                    "W",
                    "T",
                    "F",
                    "S",
                    "S"
                ).forEach { day ->

                    Text(
                        text = day,

                        style =
                        MaterialTheme
                            .typography
                            .labelSmall,

                        color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                            .copy(
                                alpha = 0.65f
                            ),

                        modifier =
                        Modifier.weight(1f),

                        textAlign =
                        androidx.compose.ui
                            .text.style
                            .TextAlign.Center
                    )
                }
            }


            Spacer(
                modifier =
                Modifier.height(4.dp)
            )


            CalendarMonthGrid(
                month =
                currentMonth,

                records =
                recordsByDate,

                selectedDate =
                selectedRecord?.date,

                onDateClick = { record ->
                        selectedDate =
                            record.date
                }
            )


            /*
             * ----------------------------------------
             * DATE EDITOR
             * ----------------------------------------
             */
            selectedRecord?.let {
                    record ->

                Spacer(
                    modifier =
                    Modifier.height(10.dp)
                )

                AttendanceDateEditor(
                    record = record,

                    isSaving =
                    savingRecordKey ==
                            recordKey(record),

                    isSaved =
                    savedRecordKey ==
                            recordKey(record),

                    onStatusSelected = { newStatus ->

                        onRecordUpdated(
                            record.copy(
                                status = newStatus
                            )
                        )
                    }
                )
            }
        }
    }
}


@Composable
private fun CalendarMonthHeader(
    currentMonth: YearMonth,
    availableMonths: List<YearMonth>,
    onMonthChanged: (YearMonth) -> Unit
) {

    val index =
        availableMonths.indexOf(
            currentMonth
        )


    Row(
        modifier =
        Modifier.fillMaxWidth(),

        verticalAlignment =
        Alignment.CenterVertically
    ) {

        IconButton(
            enabled = index > 0,

            onClick = {

                if (index > 0) {

                    onMonthChanged(
                        availableMonths[
                            index - 1
                        ]
                    )
                }
            }
        ) {

            Icon(
                imageVector =
                Icons.Filled
                    .KeyboardArrowLeft,

                contentDescription =
                "Previous month"
            )
        }


        Text(
            text =
            currentMonth
                .format(
                    DateTimeFormatter
                        .ofPattern(
                            "MMMM yyyy",
                            Locale.getDefault()
                        )
                )
                .replaceFirstChar {
                    it.uppercase()
                },

            style =
            MaterialTheme
                .typography
                .titleMedium,

            modifier =
            Modifier.weight(1f),

            textAlign =
            androidx.compose.ui
                .text.style
                .TextAlign.Center
        )


        IconButton(
            enabled =
            index >= 0 &&
                    index <
                    availableMonths.lastIndex,

            onClick = {

                if (
                    index >= 0 &&
                    index <
                    availableMonths.lastIndex
                ) {

                    onMonthChanged(
                        availableMonths[
                            index + 1
                        ]
                    )
                }
            }
        ) {

            Icon(
                imageVector =
                Icons.Filled
                    .KeyboardArrowRight,

                contentDescription =
                "Next month"
            )
        }
    }
}


@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    records:
    Map<LocalDate, AttendanceRecord>,
    selectedDate: LocalDate?,
    onDateClick:
        (AttendanceRecord) -> Unit
) {

    val firstDay =
        month.atDay(1)

    /*
     * java.time:
     * Monday = 1
     * Sunday = 7
     */
    val leadingEmptyCells =
        firstDay.dayOfWeek.value - 1

    val totalDays =
        month.lengthOfMonth()


    /*
     * Six complete weeks avoids calendar
     * height jumping between months.
     */
    val calendarCells =
        (0 until 42).map {
                cell ->

            val day =
                cell -
                        leadingEmptyCells +
                        1

            if (
                day in 1..totalDays
            ) {
                day
            } else {
                null
            }
        }


    calendarCells
        .chunked(7)
        .forEach { week ->

            Row(
                modifier =
                Modifier.fillMaxWidth()
            ) {

                week.forEach { day ->

                    if (day == null) {

                        Spacer(
                            modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                        )

                    } else {

                        val date =
                            month.atDay(day)

                        val record =
                            records[date]


                        CalendarDay(
                            day = day,
                            record = record,

                            selected =
                            selectedDate ==
                                    date,

                            modifier =
                            Modifier.weight(1f),

                            onClick = {

                                if (
                                    record != null
                                ) {
                                    onDateClick(
                                        record
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
}


@Composable
private fun CalendarDay(
    day: Int,
    record: AttendanceRecord?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    val statusColor =
        when (record?.status) {

            AttendanceStatus.PRESENT ->
                PresentColor

            AttendanceStatus.LATE ->
                LateColor

            AttendanceStatus.ABSENT ->
                AbsentColor

            null ->
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
        }


    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(
                RoundedCornerShape(8.dp)
            )
            .background(
                if (record != null) {
                    statusColor.copy(
                        alpha = 0.22f
                    )
                } else {
                    MaterialTheme
                        .colorScheme
                        .surface
                        .copy(alpha = 0.18f)
                }
            )
            .then(
                if (selected) {

                    Modifier.border(
                        width = 2.dp,
                        color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                        shape =
                        RoundedCornerShape(
                            8.dp
                        )
                    )

                } else if (
                    record != null
                ) {

                    Modifier.border(
                        width = 1.dp,
                        color = statusColor,
                        shape =
                        RoundedCornerShape(
                            8.dp
                        )
                    )

                } else {
                    Modifier
                }
            )
            .then(
                if (record != null) {

                    Modifier.hapticClickable {
                        onClick()
                    }

                } else {
                    Modifier
                }
            ),

        contentAlignment =
        Alignment.Center
    ) {

        Column(
            horizontalAlignment =
            Alignment.CenterHorizontally
        ) {

            Text(
                text = day.toString(),

                style =
                MaterialTheme
                    .typography
                    .bodyMedium,

                color =
                if (record != null) {
                    MaterialTheme
                        .colorScheme
                        .onSurface
                } else {
                    MaterialTheme
                        .colorScheme
                        .onSurface
                        .copy(
                            alpha = 0.3f
                        )
                }
            )


            /*
             * Tiny colored dot makes recorded
             * attendance easier to scan.
             */
            if (record != null) {

                Spacer(
                    modifier =
                    Modifier.height(2.dp)
                )

                Box(
                    modifier =
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            statusColor
                        )
                )
            }
        }
    }
}


@Composable
private fun AttendanceDateEditor(
    record: AttendanceRecord,
    isSaving: Boolean,
    isSaved: Boolean,
    onStatusSelected: (AttendanceStatus) -> Unit
) {
    when {

        isSaving -> {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),

                verticalAlignment =
                Alignment.CenterVertically
            ) {

                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "Saving and verifying…",
                    style =
                    MaterialTheme.typography.bodySmall
                )
            }
        }


        isSaved -> {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),

                verticalAlignment =
                Alignment.CenterVertically
            ) {

                Text(
                    text = "✓",
                    color = PresentColor,
                    style =
                    MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = "Saved",
                    color = PresentColor,
                    style =
                    MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    10.dp
                )
            )
            .background(
                MaterialTheme
                    .colorScheme
                    .surface
                    .copy(alpha = 0.45f)
            )
            .padding(10.dp)
    ) {

        Row(
            modifier =
            Modifier.fillMaxWidth(),

            verticalAlignment =
            Alignment.CenterVertically
        ) {

            Column(
                modifier =
                Modifier.weight(1f)
            ) {

                Text(
                    text =
                    record.date.format(
                        DateTimeFormatter
                            .ofPattern(
                                "EEEE, MMM d",
                                Locale.getDefault()
                            )
                    ),

                    style =
                    MaterialTheme
                        .typography
                        .titleSmall
                )


                Text(
                    text =
                    "Current: ${
                        statusLabel(
                            record.status
                        )
                    }",

                    style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                    color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
                        .copy(
                            alpha = 0.7f
                        )
                )
            }
        }


        Spacer(
            modifier =
            Modifier.height(8.dp)
        )


        Row(
            modifier =
            Modifier.fillMaxWidth(),

            horizontalArrangement =
            Arrangement.SpaceEvenly
        ) {

            StatusEditButton(
                status =
                AttendanceStatus.PRESENT,

                selected =
                record.status ==
                        AttendanceStatus.PRESENT,

                color =
                PresentColor,

                icon =
                R.drawable.present_icon,

                enabled =
                !isSaving,

                onClick = {
                    onStatusSelected(
                        AttendanceStatus.PRESENT
                    )
                }
            )


            StatusEditButton(
                status =
                AttendanceStatus.LATE,

                selected =
                record.status ==
                        AttendanceStatus.LATE,

                color =
                LateColor,

                icon =
                R.drawable.late_icon,

                enabled =
                !isSaving,

                onClick = {
                    onStatusSelected(
                        AttendanceStatus.LATE
                    )
                }
            )


            StatusEditButton(
                status =
                AttendanceStatus.ABSENT,

                selected =
                record.status ==
                        AttendanceStatus.ABSENT,

                color =
                AbsentColor,

                icon =
                R.drawable.absent_icon,

                enabled =
                !isSaving,

                onClick = {
                    onStatusSelected(
                        AttendanceStatus.ABSENT
                    )
                }
            )
        }
    }
}


@Composable
private fun StatusEditButton(
    status: AttendanceStatus,
    selected: Boolean,
    color: Color,
    icon: Int,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .size(54.dp)
            .then(
                if (enabled) {
                    Modifier.hapticClickable {
                        onClick()
                    }
                } else {
                    Modifier
                }
            ),

        shape = CircleShape,

        color =
        if (selected) {
            color.copy(alpha = 0.28f)
        } else {
            MaterialTheme
                .colorScheme
                .surface
                .copy(alpha = 0.4f)
        },

        border =
        BorderStroke(
            width =
            if (selected) {
                2.dp
            } else {
                1.dp
            },

            color =
            if (selected) {
                color
            } else {
                MaterialTheme
                    .colorScheme
                    .outline
                    .copy(alpha = 0.35f)
            }
        )
    ) {

        Box(
            modifier =
            Modifier.fillMaxSize(),

            contentAlignment =
            Alignment.Center
        ) {

            Icon(
                painter =
                painterResource(
                    id = icon
                ),

                contentDescription =
                statusLabel(status),

                tint = color,

                modifier =
                Modifier.size(26.dp)
            )
        }
    }
}


private fun statusLabel(
    status: AttendanceStatus?
): String {

    return when (status) {

        AttendanceStatus.PRESENT ->
            "Present"

        AttendanceStatus.LATE ->
            "Late"

        AttendanceStatus.ABSENT ->
            "Absent"

        null ->
            "Unknown"
    }
}


@Composable
fun AttendanceIconCount(
    icon: Int,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {

    Row(
        verticalAlignment =
        Alignment.CenterVertically,

        modifier = modifier
    ) {

        Icon(
            painter =
            painterResource(
                id = icon
            ),

            contentDescription = null,

            tint = color,

            modifier =
            Modifier.size(20.dp)
        )


        Spacer(
            modifier =
            Modifier.width(4.dp)
        )


        Text(
            text =
            count.toString(),

            style =
            MaterialTheme
                .typography
                .bodyMedium,

            color =
            MaterialTheme
                .colorScheme
                .onSurface
        )
    }
}