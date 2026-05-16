package com.mgenc.foodjournal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.worker.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as FoodJournalApp
        CoroutineScope(Dispatchers.IO).launch {
            val reminders = app.database.reminderDao().getEnabled()
            val scheduler = ReminderScheduler(context)
            for (reminder in reminders) {
                scheduler.schedule(reminder)
            }
        }
    }
}
