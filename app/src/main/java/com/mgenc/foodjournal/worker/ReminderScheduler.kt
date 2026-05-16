package com.mgenc.foodjournal.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mgenc.foodjournal.data.Reminder
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    fun schedule(reminder: Reminder) {
        val now = LocalDateTime.now()
        val targetTime = LocalTime.of(reminder.hour, reminder.minute)
        var next = now.with(targetTime)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        val initialDelay = Duration.between(now, next)

        val data = Data.Builder()
            .putString(ReminderWorker.KEY_REMINDER_ID, reminder.id)
            .build()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            24, TimeUnit.HOURS,
        )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(ReminderWorker.TAG_REMINDER)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "reminder_${reminder.id}",
                ExistingPeriodicWorkPolicy.REPLACE,
                request,
            )
    }

    fun cancel(reminderId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("reminder_$reminderId")
    }
}
