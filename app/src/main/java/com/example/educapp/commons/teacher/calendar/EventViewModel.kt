package com.example.educapp.commons.teacher.calendar

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    // The scheduled event time as stored in Firestore
    val dateTime: Timestamp = Timestamp.now(),
    // Reminder option: whether a reminder should be sent
    val remind: Boolean = false,
    // The time at which the reminder should fire (as a Timestamp), if any
    val reminderTime: Timestamp? = null
) {
    // Helper to convert Firestore Timestamp -> LocalDate
    fun toLocalDate(): LocalDate {
        val instant = dateTime.toDate().toInstant()
        return instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }

    // Helper to convert Firestore Timestamp -> LocalTime
    fun toLocalTime(): LocalTime {
        val instant = dateTime.toDate().toInstant()
        return instant.atZone(ZoneId.systemDefault()).toLocalTime()
    }
}
