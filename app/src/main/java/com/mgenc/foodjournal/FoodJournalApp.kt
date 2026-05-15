package com.mgenc.foodjournal

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mgenc.foodjournal.data.FoodJournalDatabase

class FoodJournalApp : Application() {
    val database: FoodJournalDatabase by lazy {
        Room.databaseBuilder(this, FoodJournalDatabase::class.java, "food_journal")
            .addMigrations(MIGRATION_4_5)
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

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food_entries ADD COLUMN deletedAt INTEGER")
        db.execSQL("ALTER TABLE recipes ADD COLUMN deletedAt INTEGER")
    }
}
