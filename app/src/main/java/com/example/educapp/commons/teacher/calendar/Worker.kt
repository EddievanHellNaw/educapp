package com.example.educapp.commons.teacher.calendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.educapp.R
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Retrieve event data from inputData
        val eventTitle = inputData.getString("eventTitle") ?: "Event Reminder"
        val eventDescription = inputData.getString("eventDescription") ?: ""

        // Create notification channel if necessary
        createNotificationChannel()

        // Build notification
        val notification = NotificationCompat.Builder(applicationContext, "event_channel")
            .setSmallIcon(R.drawable.teacher_image) // Replace with your own icon
            .setContentTitle(eventTitle)
            .setContentText(eventDescription)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Show the notification
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((eventTitle.hashCode() and 0xffff), notification)

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "event_channel",
                "Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled events"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}



fun scheduleNotification(context: Context, event: Event) {
    val reminderMillis = event.reminderTime?.toDate()?.time ?: return
    val delayMillis = reminderMillis - System.currentTimeMillis()
    if (delayMillis <= 0) return  // Reminder time already passed

    val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(
            workDataOf(
                "eventTitle" to event.title,
                "eventDescription" to event.description
            )
        )
        .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}
