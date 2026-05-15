package com.mgenc.foodjournal

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mgenc.foodjournal.data.FoodJournalDatabase

class FoodJournalApp : Application() {
    val database: FoodJournalDatabase by lazy {
        Room.databaseBuilder(this, FoodJournalDatabase::class.java, "food_journal")
            .fallbackToDestructiveMigration(true)
            .build()
    }

    private val prefs by lazy {
        getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    }

    var serverUrl: String
        get() = prefs.getString("server_url", "") ?: ""
        set(value) = prefs.edit().putString("server_url", value).apply()

    var lastSyncAt: Long
        get() = prefs.getLong("last_sync_at", 0L)
        set(value) = prefs.edit().putLong("last_sync_at", value).apply()
}
