package com.mgenc.foodjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.data.Reminder
import com.mgenc.foodjournal.util.uuidV7
import com.mgenc.foodjournal.worker.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FoodJournalApp).database.reminderDao()
    private val scheduler = ReminderScheduler(app)

    val reminders: StateFlow<List<Reminder>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addReminder(mealType: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val reminder = Reminder(
                id = uuidV7(),
                mealType = mealType,
                hour = hour,
                minute = minute,
                enabled = true,
                createdAt = now,
                updatedAt = now,
            )
            dao.insert(reminder)
            scheduler.schedule(reminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            scheduler.cancel(reminder.id)
            dao.deleteById(reminder.id)
        }
    }

    fun toggleEnabled(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(
                enabled = !reminder.enabled,
                updatedAt = System.currentTimeMillis(),
            )
            dao.update(updated)
            if (updated.enabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated.id)
            }
        }
    }
}
