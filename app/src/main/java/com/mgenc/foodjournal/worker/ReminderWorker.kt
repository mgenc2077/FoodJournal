package com.mgenc.foodjournal.worker

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mgenc.foodjournal.data.FoodJournalDatabase
import com.mgenc.foodjournal.util.NotificationHelper

class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()
        val db = Room.databaseBuilder(
            applicationContext,
            FoodJournalDatabase::class.java,
            "food_journal",
        ).build()
        val reminder = db.reminderDao().getById(reminderId) ?: return Result.failure()
        if (!reminder.enabled) return Result.success()
        NotificationHelper.showReminderNotification(
            applicationContext,
            reminder.id,
            reminder.mealType,
        )
        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
        const val TAG_REMINDER = "food_reminder"
    }
}
