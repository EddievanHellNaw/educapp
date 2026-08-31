package com.example.educapp.commons.teacher.attendance

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.educapp.R
import com.example.educapp.commons.ui.GradientCard
import com.example.educapp.commons.ui.HapticButton
import com.example.educapp.commons.ui.hapticClickable
import kotlinx.coroutines.delay
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.collections.find
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(viewModel: AttendanceViewModel, groupId: String, navController: NavHostController) {
    val group = viewModel.groups.find { it.id == groupId }
    val groupName = group?.name ?: ""

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Take Attendance for $groupName", style = MaterialTheme.typography.headlineSmall) })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn {
                items(3) { partial ->
                    PartialCard(
                        partial = partial + 1,
                        groupName = groupName,
                        onAttendanceClick = {
                            navController.navigate("attendance/$groupId/${partial + 1}")
                        },
                        onCheckClick = {
                            navController.navigate("check/$groupId/${partial + 1}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PartialCard(
    partial: Int,
    groupName: String,
    onAttendanceClick: () -> Unit,
    onCheckClick: () -> Unit
) {

    val buttonColor = MaterialTheme.colorScheme.primary
    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .hapticClickable { onAttendanceClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface, // left color
                            buttonColor                        // right color
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top row: partial text (left half), button (right half)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Partial text occupies left half
                    Text(
                        text = "Partial $partial",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )

                    // HapticButton occupies right half
                    HapticButton(
                        onClick = onCheckClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Review",style = MaterialTheme.typography.bodyMedium)
                    }
                }
                // Below row: group name
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceDetailsScreen(
    viewModel: AttendanceViewModel,
    groupId: String,
    partial: Int,
    navController: NavHostController
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showGoBackButton by remember { mutableStateOf(false) }
    val snackbarHostState =
        remember {
            SnackbarHostState()
        }
    val group = viewModel.groups.find { it.id == groupId }
    val students = group?.students ?: emptyList()
    val isLoading by viewModel.isLoading.collectAsState()
    val attendanceList = remember { mutableStateListOf<AttendanceRecord>() }
    val allStudentsHaveRecord =
        attendanceList.isNotEmpty() &&
                attendanceList.all {
                    it.status != null
                }
    var buttonText by remember { mutableStateOf("Confirm Attendance") }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var initialRecordsLoaded by remember(
        groupId,
        partial
    ) {
        mutableStateOf(false)
    }

    LaunchedEffect(
        groupId,
        partial
    ) {
        initialRecordsLoaded = false

        /*
         * Wait until the initial Firestore read has
         * completely finished before allowing attendance
         * to be edited.
         *
         * This prevents a delayed server response from
         * overwriting attendance that the teacher has
         * already marked locally.
         */
        viewModel
            .getAttendanceRecordsForGroup(
                groupId,
                partial
            )
            .first()

        initialRecordsLoaded = true
    }

    LaunchedEffect(
        students,
        selectedDate,
        initialRecordsLoaded
    ) {

        /*
         * Do not build the editable attendance draft until
         * the initial server read has completed.
         */
        if (!initialRecordsLoaded) {
            return@LaunchedEffect
        }

        attendanceList.clear()

        students.forEach { student ->

            val existingRecord =
                attendanceRecords.find {

                    it.student == student &&
                            it.groupId == groupId &&
                            it.partial == partial &&
                            it.date == selectedDate
                }

            attendanceList.add(
                existingRecord
                    ?: AttendanceRecord(
                        student = student,
                        groupId = groupId,
                        partial = partial,
                        date = selectedDate,
                        status = null
                    )
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
        bottomBar = {
            if (allStudentsHaveRecord) {
                HapticButton(
                    onClick = { if (buttonText == "Confirm Attendance") {
                        showConfirmationDialog = true
                        } else {
                        navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .width(200.dp)
                        .height(48.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(buttonText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Spacer(modifier = Modifier.padding(8.dp))
            HapticButton(
                onClick = { showDatePickerDialog = true },
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp)
            ) {
                Text(
                    text="Select Date: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (showDatePickerDialog) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                )

                DatePickerDialog(
                    onDismissRequest = { showDatePickerDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDatePickerDialog = false
                                datePickerState.selectedDateMillis?.let { millis ->
                                    selectedDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()

                                    // Update attendance list
                                    attendanceList.clear()
                                    students.forEach { student ->
                                        val existingRecord =
                                            attendanceRecords.find {

                                                it.student == student &&
                                                        it.groupId == groupId &&
                                                        it.partial == partial &&
                                                        it.date == selectedDate
                                            }
                                        if (existingRecord != null) {
                                            attendanceList.add(existingRecord)
                                        } else {
                                            attendanceList.add(
                                                AttendanceRecord(
                                                    student = student,
                                                    groupId = groupId,
                                                    partial = partial,
                                                    date = selectedDate
                                                )
                                            )
                                        }
                                    }

                                }
                            }
                        ) {
                            Text("OK")
                        }
                    }
                ) {
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors() // Add default colors
                    )
                }
            }

            Spacer(modifier = Modifier.padding(8.dp))

            if (!initialRecordsLoaded) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    items(
                        items = attendanceList,
                        key = { record ->
                            "${record.student}-${record.date}-${record.partial}"
                        }
                    ) { record ->

                        StudentItem(
                            student = record.student,

                            photoBase64 = group
                                ?.studentPhotos
                                ?.get(record.student),

                            status = record.status,

                            onAttendanceStatusChange = { newStatus ->

                                Timber.tag("StudentItem")
                                    .d(
                                        "New status: $newStatus, " +
                                                "Date: $selectedDate"
                                    )

                                val index =
                                    attendanceList.indexOf(record)

                                if (
                                    index != -1 &&
                                    newStatus != null
                                ) {

                                    attendanceList[index] =
                                        record.copy(
                                            status = newStatus
                                        )

                                    viewModel.addOrUpdateAttendanceRecord(
                                        attendanceList[index]
                                    )
                                }
                            }
                        )
                    }
                }
            }

            if (showConfirmationDialog) {

                AlertDialog(
                    onDismissRequest = {
                        if (!isLoading) {
                            showConfirmationDialog = false
                        }
                    },

                    title = {
                        Text(
                            text = "Confirm Attendance",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },

                    text = {
                        Text(
                            text = "Are you sure you want to save the attendance?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },

                    confirmButton = {

                        HapticButton(
                            onClick = {

                                coroutineScope.launch {

                                    /*
                                     * Close the confirmation dialog.
                                     *
                                     * The main bottom button will show
                                     * the loading state while Firebase
                                     * saves and verifies everything.
                                     */
                                    showConfirmationDialog = false

                                    val result =
                                        viewModel.saveAttendanceConfirmed(
                                            attendanceList.toList()
                                        )


                                    if (result.isSuccess) {

                                        val confirmedRecords =
                                            result.getOrThrow()

                                        Timber.tag("TakeAttendance")
                                            .d(
                                                "Attendance saved and verified. " +
                                                        "${confirmedRecords.size} records."
                                            )

                                        showGoBackButton = true
                                        buttonText = "Go Back"


                                        /*
                                         * This is now connected to Scaffold's
                                         * actual SnackbarHost.
                                         */
                                        snackbarHostState.showSnackbar(
                                            message =
                                            "Attendance saved and verified ✓"
                                        )

                                    } else {

                                        val error =
                                            result.exceptionOrNull()

                                        Timber.tag("TakeAttendance")
                                            .e(
                                                error,
                                                "Attendance could not be saved"
                                            )

                                        showGoBackButton = false
                                        buttonText = "Confirm Attendance"


                                        snackbarHostState.showSnackbar(
                                            message =
                                            "Save failed: ${
                                                error?.localizedMessage
                                                    ?: "Unknown database error"
                                            }"
                                        )
                                    }


                                }
                            },

                            enabled = !isLoading,

                            modifier = Modifier
                                .width(200.dp)
                                .height(48.dp)
                        ) {

                            if (isLoading) {

                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )

                            } else {

                                Text(
                                    text = "Confirm",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },

                    dismissButton = {

                        HapticButton(
                            onClick = {
                                showConfirmationDialog = false
                            },

                            enabled = !isLoading,

                            modifier = Modifier
                                .width(200.dp)
                                .height(48.dp)
                        ) {

                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )
            }

        }
    }
}


@Composable
fun StudentItem(
    student: String,
    photoBase64: String? = null,
    status: AttendanceStatus?,
    onAttendanceStatusChange: (AttendanceStatus?) -> Unit
) {
    val presentColor = Color(0xFF388E3C)
    val lateColor = Color(0xFFFBC02D)
    val absentColor = Color(0xFFD32F2F)

    val shape = RoundedCornerShape(12.dp)

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    /*
     * How far the user must swipe before the
     * attendance status changes.
     */
    val swipeThreshold = with(density) {
        85.dp.toPx()
    }

    /*
     * Prevent the card from being dragged
     * completely off the screen.
     */
    val maximumSwipe = with(density) {
        145.dp.toPx()
    }

    /*
     * Local display state.
     *
     * remember(student) prevents state belonging to
     * one student from moving to another LazyColumn row.
     */


    var dragOffset by remember(student) {
        mutableStateOf(0f)
    }

    fun markAttendance(
        newStatus: AttendanceStatus
    ) {

        onAttendanceStatusChange(
            newStatus
        )

        haptic.performHapticFeedback(
            HapticFeedbackType.TextHandleMove
        )
    }

    val statusColor =
        when (status) {
            AttendanceStatus.PRESENT -> presentColor
            AttendanceStatus.LATE -> lateColor
            AttendanceStatus.ABSENT -> absentColor
            null -> MaterialTheme.colorScheme.primary
        }

    /*
     * Background revealed underneath the card
     * while swiping.
     */
    val swipeBackgroundColor =
        when {
            dragOffset > 0f ->
                presentColor

            dragOffset < 0f ->
                absentColor

            else ->
                Color.Transparent
        }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical =
                if (status == null) {
                    6.dp
                } else {
                    3.dp
                }
            )
            .clip(shape)
            .background(
                swipeBackgroundColor
            )
    ) {

        /*
         * Labels underneath the moving card.
         */
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 18.dp),

            verticalAlignment =
            Alignment.CenterVertically,

            horizontalArrangement =
            Arrangement.SpaceBetween
        ) {

            Text(
                text =
                if (dragOffset > 15f) {
                    "PRESENT"
                } else {
                    ""
                },
                color = Color.White,
                style =
                MaterialTheme.typography.labelLarge
            )

            Text(
                text =
                if (dragOffset < -15f) {
                    "ABSENT"
                } else {
                    ""
                },
                color = Color.White,
                style =
                MaterialTheme.typography.labelLarge
            )
        }

        /*
         * Actual student card.
         */
        Card(
            modifier = Modifier
                .fillMaxWidth()

                /*
                 * Physically move the card while
                 * the finger moves.
                 */
                .offset {
                    IntOffset(
                        x = dragOffset.roundToInt(),
                        y = 0
                    )
                }

                /*
                 * Collapse smoothly after a status
                 * has been selected.
                 */
                .animateContentSize(
                    animationSpec =
                    tween(durationMillis = 180)
                )

                /*
                 * Horizontal swipe recognizer.
                 *
                 * LazyColumn vertical scrolling still works
                 * because this detector waits specifically
                 * for a horizontal gesture.
                 */
                .pointerInput(student) {

                    detectHorizontalDragGestures(

                        onDragEnd = {

                            when {

                                dragOffset >=
                                        swipeThreshold -> {

                                    markAttendance(
                                        AttendanceStatus.PRESENT
                                    )
                                }

                                dragOffset <=
                                        -swipeThreshold -> {

                                    markAttendance(
                                        AttendanceStatus.ABSENT
                                    )
                                }
                            }

                            /*
                             * Return card to its normal
                             * horizontal position.
                             */
                            dragOffset = 0f
                        },

                        onDragCancel = {
                            dragOffset = 0f
                        },

                        onHorizontalDrag = {
                                change,
                                dragAmount ->

                            change.consume()

                            dragOffset =
                                (
                                        dragOffset +
                                                dragAmount
                                        ).coerceIn(
                                        -maximumSwipe,
                                        maximumSwipe
                                    )
                        }
                    )
                },

            shape = shape,

            colors = CardDefaults.cardColors(
                containerColor =
                Color.Transparent
            ),

            elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                if (
                    status == null
                ) {
                    8.dp
                } else {
                    4.dp
                }
            )
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme
                                    .colorScheme
                                    .surface,

                                statusColor.copy(
                                    alpha =
                                    if (
                                        status ==
                                        null
                                    ) {
                                        0.65f
                                    } else {
                                        0.45f
                                    }
                                )
                            )
                        )
                    )
            ) {

                /*
                 * Pending cards are larger.
                 *
                 * Once attendance is selected,
                 * switch to the compact card.
                 */
                if (status == null) {

                    PendingStudentContent(
                        student = student,
                        photoBase64 = photoBase64,

                        onLate = {
                            markAttendance(
                                AttendanceStatus.LATE
                            )
                        }
                    )

                } else {

                    CompactStudentContent(
                        student = student,
                        photoBase64 = photoBase64,
                        status = status!!,

                        onLate = {
                            markAttendance(
                                AttendanceStatus.LATE
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingStudentContent(
    student: String,
    photoBase64: String?,
    onLate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),

        verticalAlignment =
        Alignment.CenterVertically
    ) {

        StudentPortrait(
            student = student,
            photoBase64 = photoBase64,
            compact = false
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = student,
                style =
                MaterialTheme.typography.bodyLarge,
                color =
                MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = "Swipe right for Present • left for Absent",
                style =
                MaterialTheme.typography.bodySmall,
                color =
                MaterialTheme.colorScheme
                    .onSurface
                    .copy(alpha = 0.7f)
            )
        }

        /*
         * Tardiness remains a button because both
         * horizontal swipe directions are already used.
         */
        IconButton(
            onClick = onLate,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = R.drawable.late_icon
                ),
                contentDescription = "Mark Late",
                tint = Color(0xFFFBC02D),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun AttendanceStatusBadge(
    status: AttendanceStatus
) {
    val color =
        when (status) {
            AttendanceStatus.PRESENT ->
                Color(0xFF388E3C)

            AttendanceStatus.LATE ->
                Color(0xFFFBC02D)

            AttendanceStatus.ABSENT ->
                Color(0xFFD32F2F)
        }

    val label =
        when (status) {
            AttendanceStatus.PRESENT ->
                "Present"

            AttendanceStatus.LATE ->
                "Late"

            AttendanceStatus.ABSENT ->
                "Absent"
        }

    val icon =
        when (status) {
            AttendanceStatus.PRESENT ->
                R.drawable.present_icon

            AttendanceStatus.LATE ->
                R.drawable.late_icon

            AttendanceStatus.ABSENT ->
                R.drawable.absent_icon
        }

    Row(
        modifier = Modifier
            .clip(
                RoundedCornerShape(50)
            )
            .background(
                color.copy(alpha = 0.18f)
            )
            .border(
                width = 1.dp,
                color = color,
                shape =
                RoundedCornerShape(50)
            )
            .padding(
                horizontal = 9.dp,
                vertical = 5.dp
            ),

        verticalAlignment =
        Alignment.CenterVertically
    ) {

        Icon(
            painter =
            painterResource(id = icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(17.dp)
        )

        Spacer(
            modifier = Modifier.width(4.dp)
        )

        Text(
            text = label,
            style =
            MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun CompactStudentContent(
    student: String,
    photoBase64: String?,
    status: AttendanceStatus,
    onLate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),

        verticalAlignment =
        Alignment.CenterVertically
    ) {

        /*
         * Much smaller portrait once marked.
         */
        StudentPortrait(
            student = student,
            photoBase64 = photoBase64,
            compact = true
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Text(
            text = student,
            style =
            MaterialTheme.typography.bodyMedium,
            color =
            MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        AttendanceStatusBadge(
            status = status
        )

        /*
         * If it isn't already late, keep a small
         * one-tap way to mark tardiness.
         */
        if (
            status != AttendanceStatus.LATE
        ) {

            Spacer(
                modifier = Modifier.width(4.dp)
            )

            IconButton(
                onClick = onLate,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = R.drawable.late_icon
                    ),
                    contentDescription = "Mark Late",
                    tint = Color(0xFFFBC02D),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun StudentPortrait(
    student: String,
    photoBase64: String?,
    compact: Boolean
) {
    val imageBitmap =
        remember(photoBase64) {

            if (photoBase64.isNullOrBlank()) {
                null
            } else {

                try {

                    val bytes =
                        Base64.decode(
                            photoBase64,
                            Base64.DEFAULT
                        )

                    BitmapFactory
                        .decodeByteArray(
                            bytes,
                            0,
                            bytes.size
                        )
                        ?.asImageBitmap()

                } catch (e: Exception) {

                    Timber.tag(
                        "StudentPortrait"
                    ).w(
                        e,
                        "Unable to decode photo for $student"
                    )

                    null
                }
            }
        }

    val width =
        if (compact) {
            42.dp
        } else {
            68.dp
        }

    val height =
        if (compact) {
            52.dp
        } else {
            86.dp
        }

    if (imageBitmap != null) {

        Image(
            bitmap = imageBitmap,

            contentDescription =
            "Photo of $student",

            modifier = Modifier
                .width(width)
                .height(height)
                .clip(
                    RoundedCornerShape(
                        if (compact) {
                            8.dp
                        } else {
                            12.dp
                        }
                    )
                ),

            contentScale =
            ContentScale.Crop
        )

    } else {

        /*
         * Fallback for old/manual groups that don't
         * have photographs.
         */
        val initials =
            student
                .split(" ")
                .filter {
                    it.isNotBlank()
                }
                .take(2)
                .joinToString("") {
                    it.take(1).uppercase()
                }

        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(
                    RoundedCornerShape(
                        if (compact) {
                            8.dp
                        } else {
                            12.dp
                        }
                    )
                )
                .background(
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                ),

            contentAlignment =
            Alignment.Center
        ) {

            Text(
                text = initials,
                style =
                if (compact) {
                    MaterialTheme
                        .typography
                        .labelLarge
                } else {
                    MaterialTheme
                        .typography
                        .headlineSmall
                }
            )
        }
    }
}

@Composable
fun AttendanceOption(
    status: AttendanceStatus,
    color: Color,
    image: Painter,
    onClick: () -> Unit
) {
    var isClicked by remember { mutableStateOf(false) }

    HapticButton(
        onClick = {
            isClicked = !isClicked
            onClick()
        },
        modifier = Modifier
            .padding(4.dp)
            .size(width = 90.dp, height = 70.dp), // Increased height for image and text
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = image,
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

