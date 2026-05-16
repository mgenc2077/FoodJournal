package com.mgenc.foodjournal.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.MainActivity
import com.mgenc.foodjournal.R

object NotificationHelper {

    fun showReminderNotification(
        context: Context,
        reminderId: String,
        mealType: String,
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val label = mealType.lowercase().replaceFirstChar { it.uppercase() }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, FoodJournalApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Food Journal")
            .setContentText("Time to log your $label!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context)
            .notify(reminderId.hashCode() and 0x7FFFFFFF, notification)
    }
}
