package com.mgenc.foodjournal

import android.app.Application
import androidx.room.Room
import com.mgenc.foodjournal.data.FoodJournalDatabase

class FoodJournalApp : Application() {
    val database: FoodJournalDatabase by lazy {
        Room.databaseBuilder(this, FoodJournalDatabase::class.java, "food_journal.db").build()
    }
}
