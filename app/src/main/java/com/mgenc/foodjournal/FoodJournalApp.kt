package com.mgenc.foodjournal

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mgenc.foodjournal.data.FoodJournalDatabase

class FoodJournalApp : Application() {
    val database: FoodJournalDatabase by lazy {
        Room.databaseBuilder(this, FoodJournalDatabase::class.java, "food_journal.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food_entries ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
    }
}
