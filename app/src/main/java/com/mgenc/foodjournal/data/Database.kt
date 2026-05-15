package com.mgenc.foodjournal.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK
}

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val foodName: String,
    val mealType: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val displayOrder: Int = 0,
)

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entries WHERE epochDay = :epochDay ORDER BY mealType, displayOrder")
    fun getByDate(epochDay: Long): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries ORDER BY epochDay DESC, mealType, displayOrder")
    fun getAll(): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries WHERE id = :id")
    suspend fun getById(id: Long): FoodEntry?

    @Insert
    suspend fun insert(entry: FoodEntry): Long

    @Update
    suspend fun update(entry: FoodEntry)

    @Delete
    suspend fun delete(entry: FoodEntry)

    @Transaction
    suspend fun updateDisplayOrders(entries: List<FoodEntry>) {
        for ((index, entry) in entries.withIndex()) {
            update(entry.copy(displayOrder = index))
        }
    }
}

@Database(entities = [FoodEntry::class], version = 2)
abstract class FoodJournalDatabase : RoomDatabase() {
    abstract fun foodEntryDao(): FoodEntryDao
}
