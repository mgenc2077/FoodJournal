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
    @PrimaryKey val id: String,
    val epochDay: Long,
    val foodName: String,
    val mealType: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val displayOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entries WHERE epochDay = :epochDay ORDER BY mealType, displayOrder")
    fun getByDate(epochDay: Long): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries ORDER BY epochDay DESC, mealType, displayOrder")
    fun getAll(): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries WHERE id = :id")
    suspend fun getById(id: String): FoodEntry?

    @Query("SELECT * FROM food_entries WHERE updatedAt > :since ORDER BY updatedAt")
    suspend fun getChangedSince(since: Long): List<FoodEntry>

    @Insert
    suspend fun insert(entry: FoodEntry): Long

    @Update
    suspend fun update(entry: FoodEntry)

    @Delete
    suspend fun delete(entry: FoodEntry)

    @Transaction
    suspend fun updateDisplayOrders(entries: List<FoodEntry>) {
        val now = System.currentTimeMillis()
        for ((index, entry) in entries.withIndex()) {
            update(entry.copy(displayOrder = index, updatedAt = now))
        }
    }

    @Query("SELECT COUNT(*) FROM food_entries")
    suspend fun count(): Int

    @Transaction
    suspend fun upsertAll(entries: List<FoodEntry>) {
        for (entry in entries) {
            val existing = getById(entry.id)
            if (existing == null) {
                insert(entry)
            } else if (entry.updatedAt > existing.updatedAt) {
                update(entry)
            }
        }
    }
}

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY name")
    fun getAll(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: String): Recipe?

    @Query("SELECT * FROM recipes WHERE updatedAt > :since ORDER BY updatedAt")
    suspend fun getChangedSince(since: Long): List<Recipe>

    @Insert
    suspend fun insert(recipe: Recipe): Long

    @Update
    suspend fun update(recipe: Recipe)

    @Delete
    suspend fun delete(recipe: Recipe)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    @Transaction
    suspend fun upsertAll(recipes: List<Recipe>) {
        for (recipe in recipes) {
            val existing = getById(recipe.id)
            if (existing == null) {
                insert(recipe)
            } else if (recipe.updatedAt > existing.updatedAt) {
                update(recipe)
            }
        }
    }
}

@Database(entities = [FoodEntry::class, Recipe::class], version = 4)
abstract class FoodJournalDatabase : RoomDatabase() {
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun recipeDao(): RecipeDao
}
