package com.example.educapp.commons.teacher.calendar

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class EventSource {
    MANUAL,
    UASLP_ACADEMIC_CALENDAR
}

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",

    // Start of the event. Existing calendar queries continue to use this field.
    val dateTime: Timestamp = Timestamp.now(),

    // Optional end for multi-day imported events.
    val endDateTime: Timestamp? = null,

    // Academic-calendar entries will normally be all-day events.
    val allDay: Boolean = false,

    // Reminder settings used by the existing Worker implementation.
    val remind: Boolean = false,
    val reminderTime: Timestamp? = null,

    // Identifies how the event entered the application.
    val source: String = EventSource.MANUAL.name,

    // Deterministic logical key used to avoid duplicate imported events.
    // Manual events leave this blank.
    val sourceKey: String = "",

    // Reserved for Stage 2/3 calendar recognition, e.g. WRITTEN_EXAM, HOLIDAY.
    val academicType: String = ""
) {
    fun toLocalDate(): LocalDate {
        val instant = dateTime.toDate().toInstant()
        return instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }

    fun toLocalTime(): LocalTime {
        val instant = dateTime.toDate().toInstant()
        return instant.atZone(ZoneId.systemDefault()).toLocalTime()
    }

    fun toEndLocalDate(): LocalDate? {
        val instant = endDateTime?.toDate()?.toInstant() ?: return null
        return instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
